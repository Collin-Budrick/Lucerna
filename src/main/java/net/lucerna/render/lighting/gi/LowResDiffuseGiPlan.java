package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record LowResDiffuseGiPlan(
        DiffuseGiFrameInput frameInput,
        TemporalAccumulationInput temporalInput,
        GiCacheSnapshot cacheSnapshot,
        CacheConfidence cacheConfidence,
        GiRayBudgetAllocation rayBudget,
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
        if (validationReport == null) {
            validationReport = DiffuseGiValidationReport.empty();
        }
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
                + " confidence=" + this.cacheConfidence.confidence();
    }
}
