package net.lucerna.render.lighting.gi;

import net.lucerna.render.tracing.TracedLightingConsumptionEvidence;

import java.util.Objects;

public record LowResDiffuseGiPlan(
        DiffuseGiFrameInput frameInput,
        TemporalAccumulationInput temporalInput,
        GiCacheSnapshot cacheSnapshot,
        CacheConfidence cacheConfidence,
        GiRayBudgetAllocation rayBudget,
        DiffuseGiSourceSummary sourceSummary,
        DiffuseGiSceneInputSummary sceneInputSummary,
        TracedLightingConsumptionEvidence traceConsumptionEvidence,
        DiffuseGiValidationReport validationReport
) {
    private static final float ROUND8_HIGH_VARIANCE_THRESHOLD = 0.50F;

    public LowResDiffuseGiPlan {
        Objects.requireNonNull(frameInput, "frameInput");
        Objects.requireNonNull(temporalInput, "temporalInput");
        if (cacheSnapshot == null) {
            cacheSnapshot = GiCacheSnapshot.empty();
        }
        if (cacheConfidence == null) {
            cacheConfidence = CacheConfidence.empty("GI cache confidence unavailable");
        }
        Objects.requireNonNull(rayBudget, "rayBudget");
        if (sourceSummary == null) {
            sourceSummary = DiffuseGiSourceSummary.unavailable();
        }
        if (sceneInputSummary == null) {
            sceneInputSummary = DiffuseGiSceneInputSummary.from(sourceSummary, cacheSnapshot, cacheConfidence);
        }
        if (traceConsumptionEvidence == null) {
            traceConsumptionEvidence = TracedLightingConsumptionEvidence.notConsumed(
                    0L,
                    "gi_plan_trace_consumption_evidence_not_supplied"
            );
        }
        if (validationReport == null) {
            validationReport = DiffuseGiValidationReport.empty();
        }
    }

    public LowResDiffuseGiPlan(
            DiffuseGiFrameInput frameInput,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            CacheConfidence cacheConfidence,
            GiRayBudgetAllocation rayBudget,
            DiffuseGiValidationReport validationReport
    ) {
        this(
                frameInput,
                temporalInput,
                cacheSnapshot,
                cacheConfidence,
                rayBudget,
                DiffuseGiSourceSummary.unavailable(),
                null,
                null,
                validationReport
        );
    }

    public LowResDiffuseGiPlan withSourceSummary(DiffuseGiSourceSummary sourceSummary) {
        return new LowResDiffuseGiPlan(
                this.frameInput,
                this.temporalInput,
                this.cacheSnapshot,
                this.cacheConfidence,
                this.rayBudget,
                sourceSummary,
                DiffuseGiSceneInputSummary.from(sourceSummary, this.cacheSnapshot, this.cacheConfidence),
                this.traceConsumptionEvidence,
                this.validationReport
        );
    }

    public LowResDiffuseGiPlan withTraceConsumptionEvidence(
            TracedLightingConsumptionEvidence traceConsumptionEvidence
    ) {
        return new LowResDiffuseGiPlan(
                this.frameInput,
                this.temporalInput,
                this.cacheSnapshot,
                this.cacheConfidence,
                this.rayBudget,
                this.sourceSummary,
                this.sceneInputSummary,
                traceConsumptionEvidence,
                this.validationReport
        );
    }

    public boolean readyForScheduling() {
        return this.validationReport.valid()
                && this.frameInput.hasSchedulingInputs()
                && this.rayBudget.tier().active();
    }

    public boolean requiresTracing() {
        return this.readyForScheduling() && this.rayBudget.hasTraceBudget();
    }

    public boolean reusesTemporalHistory() {
        return this.readyForScheduling() && this.temporalInput.accumulates();
    }

    public boolean cacheUsable() {
        return this.cacheConfidence.usable(this.frameInput.settings().historyConfidenceFloor());
    }

    public boolean tracedLightingConsumedByFinalGiSource() {
        return this.traceConsumptionEvidence.finalGiSourceConsumed();
    }

    public boolean tracedLightingHasMaterialDepthSourceCoupling() {
        return this.traceConsumptionEvidence.hasMaterialDepthSourceCoupling();
    }

    public boolean realGpuTracedLightingConsumedByFinalGiSource() {
        return this.traceConsumptionEvidence.realGpuTraversalConsumed();
    }

    public String compactLabel() {
        return "grid=" + this.frameInput.lowResolutionGrid().label()
                + " temporal=" + this.temporalInput.stateLabel()
                + " budget=" + this.rayBudget.tier().name().toLowerCase()
                + " confidence=" + this.cacheConfidence.confidence()
                + " sceneState=" + this.sceneState()
                + " " + this.rayBudget.adaptiveMap().bucketCountsLabel()
                + " " + this.rayBudget.adaptiveMap().dispatchBudgetLabel()
                + " " + this.cacheContributionLabel()
                + " " + this.varianceContributionLabel()
                + " " + this.emissiveContributionLabel()
                + " " + this.sceneInputContributionLabel()
                + " " + this.traceConsumptionContributionLabel()
                + " sources={" + this.sourceSummary.compactLabel() + "}";
    }

    public String gridDebugLabel() {
        return this.frameInput.lowResolutionGrid().label();
    }

    public String rayBudgetDebugLabel() {
        return this.rayBudget.tier().name().toLowerCase()
                + " rays=" + this.rayBudget.cappedRays() + "/" + this.rayBudget.requestedRays()
                + " cells=" + this.rayBudget.lowResolutionCellCount()
                + " classes=" + this.rayBudget.adaptiveMap().compactLabel()
                + " " + this.rayBudget.adaptiveMap().bucketCountsLabel()
                + " " + this.rayBudget.adaptiveMap().bucketRaysLabel()
                + " " + this.rayBudget.adaptiveMap().regionCountsLabel()
                + " " + this.rayBudget.adaptiveMap().dispatchBudgetLabel()
                + " sceneState=" + this.sceneState()
                + " " + this.cacheContributionLabel()
                + " " + this.varianceContributionLabel()
                + " " + this.emissiveContributionLabel()
                + " " + this.sceneInputContributionLabel()
                + " " + this.traceConsumptionContributionLabel()
                + " reason=" + this.rayBudget.reason();
    }

    public String cacheConfidenceDebugLabel() {
        return this.cacheContributionLabel()
                + " confidence=" + this.cacheConfidence.confidence()
                + " variance=" + this.cacheConfidence.variance()
                + " samples=" + this.cacheConfidence.sampleCount()
                + " dirty=" + this.cacheConfidence.dirty()
                + " " + this.varianceContributionLabel()
                + " " + this.sceneInputContributionLabel()
                + " reason=" + this.cacheConfidence.reason();
    }

    public String sourceDebugLabel() {
        return this.sourceSummary.debugLabel()
                + " sceneState=" + this.sceneState()
                + " " + this.emissiveContributionLabel()
                + " " + this.sceneInputContributionLabel()
                + " " + this.traceConsumptionContributionLabel();
    }

    private String sceneState() {
        if (this.cacheConfidence.dirty() || this.cacheSnapshot.hasDirtyRegions() || this.sourceSummary.dirtyRegionCount() > 0) {
            return "dirty";
        }
        if (!this.temporalInput.reuseAllowed()) {
            return "temporal-reset";
        }
        if (this.cacheConfidence.variance() >= ROUND8_HIGH_VARIANCE_THRESHOLD) {
            return "high-variance";
        }
        if (this.sourceSummary.directLightingReady() && this.sourceSummary.emissiveLightCount() > 0) {
            return "emissive-active";
        }
        if (this.cacheUsable()) {
            return "stable-reuse";
        }
        return "stable-refresh";
    }

    private String cacheContributionLabel() {
        return "cacheConfidenceContribution=confidence:" + this.cacheConfidence.confidence()
                + "/floor:" + this.frameInput.settings().historyConfidenceFloor()
                + "/usable:" + this.cacheUsable()
                + "/samples:" + this.cacheConfidence.sampleCount()
                + "/dirty:" + this.cacheConfidence.dirty();
    }

    private String varianceContributionLabel() {
        return "varianceContribution=variance:" + this.cacheConfidence.variance()
                + "/high:" + ROUND8_HIGH_VARIANCE_THRESHOLD
                + "/samples:" + this.cacheConfidence.sampleCount();
    }

    private String emissiveContributionLabel() {
        return "emissiveContribution=count:" + this.sourceSummary.emissiveLightCount()
                + "/directReady:" + this.sourceSummary.directLightingReady()
                + " emissiveProximity=shadowCandidates:" + this.sourceSummary.shadowCandidateCount()
                + "/budgeted:" + this.sourceSummary.budgetedShadowCandidateCount()
                + " emissiveRegions=sections:" + this.sourceSummary.sectionSnapshotCount()
                + "/dirty:" + this.sourceSummary.dirtyRegionCount();
    }

    private String sceneInputContributionLabel() {
        return "sceneInputs={" + this.sceneInputSummary.compactLabel() + "}";
    }

    private String traceConsumptionContributionLabel() {
        return "traceConsumption={" + this.traceConsumptionEvidence.compactLabel() + "}";
    }
}
