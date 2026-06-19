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
                + " reason=" + this.rayBudget.reason();
    }

    public String cacheConfidenceDebugLabel() {
        return "confidence=" + this.cacheConfidence.confidence()
                + " variance=" + this.cacheConfidence.variance()
                + " samples=" + this.cacheConfidence.sampleCount()
                + " dirty=" + this.cacheConfidence.dirty()
                + " reason=" + this.cacheConfidence.reason();
    }

    public String sourceDebugLabel() {
        return this.sourceSummary.debugLabel();
    }
}
