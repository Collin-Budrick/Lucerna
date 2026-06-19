package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record LowResDiffuseGiPlan(
        DiffuseGiFrameInput frameInput,
        TemporalAccumulationInput temporalInput,
        GiCacheSnapshot cacheSnapshot,
        CacheConfidence cacheConfidence,
        GiRayBudgetAllocation rayBudget,
        DiffuseGiSourceSummary sourceSummary,
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
                this.validationReport
        );
    }

    public boolean readyForScheduling() {
        return this.validationReport.valid()
                && this.frameInput.hasRequiredInputs()
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
                + " reason=" + this.rayBudget.reason();
    }

    public String cacheConfidenceDebugLabel() {
        return this.cacheContributionLabel()
                + " confidence=" + this.cacheConfidence.confidence()
                + " variance=" + this.cacheConfidence.variance()
                + " samples=" + this.cacheConfidence.sampleCount()
                + " dirty=" + this.cacheConfidence.dirty()
                + " " + this.varianceContributionLabel()
                + " reason=" + this.cacheConfidence.reason();
    }

    public String sourceDebugLabel() {
        return this.sourceSummary.debugLabel()
                + " sceneState=" + this.sceneState()
                + " " + this.emissiveContributionLabel();
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
}
