package net.lucerna.render.lighting.gi;

import java.util.Objects;

public final class AdaptiveGiRayBudgetPolicy {
    private final AdaptiveGiRayBudgetConfig config;

    public AdaptiveGiRayBudgetPolicy(AdaptiveGiRayBudgetConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public static AdaptiveGiRayBudgetPolicy firstMilestone() {
        return new AdaptiveGiRayBudgetPolicy(AdaptiveGiRayBudgetConfig.firstMilestone());
    }

    public AdaptiveGiRayBudgetConfig config() {
        return this.config;
    }

    public GiRayBudgetAllocation allocate(
            DiffuseGiLowResolutionGrid grid,
            CacheConfidence cacheConfidence,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings
    ) {
        DiffuseGiSettings resolvedSettings = settings == null ? DiffuseGiSettings.disabled() : settings;
        DiffuseGiLowResolutionGrid resolvedGrid = grid == null ? DiffuseGiLowResolutionGrid.unavailable() : grid;
        CacheConfidence resolvedConfidence = cacheConfidence == null
                ? CacheConfidence.empty("cache confidence unavailable")
                : cacheConfidence;
        TemporalAccumulationInput resolvedTemporalInput = temporalInput == null
                ? TemporalAccumulationInput.unavailable("temporal input unavailable")
                : temporalInput;
        GiCacheSnapshot resolvedCacheSnapshot = cacheSnapshot == null ? GiCacheSnapshot.empty() : cacheSnapshot;

        if (!resolvedSettings.enabled()) {
            return GiRayBudgetAllocation.disabled(resolvedGrid, "diffuse GI disabled");
        }
        if (!resolvedGrid.available()) {
            return GiRayBudgetAllocation.disabled(resolvedGrid, "low-resolution GI grid unavailable");
        }

        GiRayBudgetTier tier = resolvedSettings.adaptiveRayBudgetEnabled()
                ? this.classify(resolvedConfidence, resolvedTemporalInput, resolvedCacheSnapshot)
                : GiRayBudgetTier.MEDIUM;
        String reason = reason(tier, resolvedConfidence, resolvedTemporalInput, resolvedCacheSnapshot, resolvedSettings);
        return GiRayBudgetAllocation.fromTier(tier, resolvedGrid, this.config, reason);
    }

    public GiRayBudgetTier classify(
            CacheConfidence cacheConfidence,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot
    ) {
        CacheConfidence resolvedConfidence = cacheConfidence == null
                ? CacheConfidence.empty("cache confidence unavailable")
                : cacheConfidence;
        TemporalAccumulationInput resolvedTemporalInput = temporalInput == null
                ? TemporalAccumulationInput.unavailable("temporal input unavailable")
                : temporalInput;
        GiCacheSnapshot resolvedCacheSnapshot = cacheSnapshot == null ? GiCacheSnapshot.empty() : cacheSnapshot;

        if (resolvedConfidence.dirty()) {
            return GiRayBudgetTier.HIGH;
        }
        if (resolvedCacheSnapshot.dirtyRegionCount() >= this.config.dirtyRegionBoostThreshold()
                && this.config.dirtyRegionBoostThreshold() > 0) {
            return GiRayBudgetTier.HIGH;
        }
        if (resolvedConfidence.variance() >= this.config.highVarianceThreshold()) {
            return GiRayBudgetTier.HIGH;
        }
        if (resolvedConfidence.confidence() < this.config.lowConfidenceThreshold()) {
            return GiRayBudgetTier.HIGH;
        }
        if (!resolvedTemporalInput.reuseAllowed()) {
            return GiRayBudgetTier.MEDIUM;
        }
        if (resolvedConfidence.variance() >= this.config.mediumVarianceThreshold()) {
            return GiRayBudgetTier.MEDIUM;
        }
        if (resolvedConfidence.confidence() >= this.config.reuseConfidenceThreshold()) {
            return GiRayBudgetTier.REUSE_ONLY;
        }
        return GiRayBudgetTier.LOW;
    }

    private static String reason(
            GiRayBudgetTier tier,
            CacheConfidence cacheConfidence,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings
    ) {
        if (!settings.adaptiveRayBudgetEnabled()) {
            return "fixed medium budget";
        }
        if (cacheConfidence.dirty()) {
            return "dirty GI cache";
        }
        if (cacheSnapshot.hasDirtyRegions()) {
            return "dirty regions pending";
        }
        if (!temporalInput.reuseAllowed()) {
            return "temporal reuse unavailable";
        }
        return switch (tier) {
            case DISABLED -> "disabled";
            case REUSE_ONLY -> "cache confidence high enough for reuse";
            case LOW -> "cache confidence moderate";
            case MEDIUM -> "variance or temporal state requires refresh";
            case HIGH -> "cache confidence low or variance high";
        };
    }
}
