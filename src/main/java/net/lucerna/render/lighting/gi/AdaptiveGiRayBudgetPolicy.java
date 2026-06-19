package net.lucerna.render.lighting.gi;

import java.util.ArrayList;
import java.util.List;
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
        return this.allocate(
                grid,
                cacheConfidence,
                temporalInput,
                cacheSnapshot,
                settings,
                DiffuseGiSourceSummary.unavailable()
        );
    }

    public GiRayBudgetAllocation allocate(
            DiffuseGiLowResolutionGrid grid,
            CacheConfidence cacheConfidence,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings,
            DiffuseGiSourceSummary sourceSummary
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
        DiffuseGiSourceSummary resolvedSourceSummary = sourceSummary == null
                ? DiffuseGiSourceSummary.unavailable()
                : sourceSummary;

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
        if (!resolvedSettings.adaptiveRayBudgetEnabled()) {
            return GiRayBudgetAllocation.fromTier(tier, resolvedGrid, this.config, reason);
        }
        AdaptiveGiRayBudgetMap adaptiveMap = this.buildAdaptiveMap(
                resolvedGrid,
                resolvedConfidence,
                resolvedTemporalInput,
                resolvedCacheSnapshot,
                resolvedSourceSummary,
                reason
        );
        return GiRayBudgetAllocation.fromAdaptiveMap(adaptiveMap, reason);
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

    private AdaptiveGiRayBudgetMap buildAdaptiveMap(
            DiffuseGiLowResolutionGrid grid,
            CacheConfidence cacheConfidence,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSourceSummary sourceSummary,
            String reason
    ) {
        int totalCells = grid.cellCount();
        int remainingCells = totalCells;
        List<AdaptiveGiRayBudgetClassAllocation> classes = new ArrayList<>();

        int dirtySignalCount = Math.max(cacheSnapshot.dirtyRegionCount(), sourceSummary.dirtyRegionCount());
        if (cacheConfidence.dirty() || dirtySignalCount >= this.config.dirtyRegionBoostThreshold()
                && this.config.dirtyRegionBoostThreshold() > 0) {
            int dirtyCells = takeCells(remainingCells, boostedCellCount(
                    totalCells,
                    cacheConfidence.dirty() ? Math.max(1, dirtySignalCount) : dirtySignalCount,
                    this.config.dirtyRegionCellFraction()
            ));
            remainingCells -= dirtyCells;
            addClass(classes, AdaptiveGiRayBudgetClass.DIRTY, dirtyCells, "dirty regions or cache invalidation");
        }

        if (sourceSummary.directLightingReady() && sourceSummary.emissiveLightCount() > 0) {
            int emissiveCells = takeCells(remainingCells, boostedCellCount(
                    totalCells,
                    sourceSummary.emissiveLightCount(),
                    this.config.emissiveCellFraction()
            ));
            remainingCells -= emissiveCells;
            addClass(classes, AdaptiveGiRayBudgetClass.EMISSIVE, emissiveCells, "near emissive/direct GI sources");
        }

        if (cacheConfidence.variance() >= this.config.highVarianceThreshold()) {
            int noisyCells = takeCells(remainingCells, boostedCellCount(
                    totalCells,
                    Math.max(1, cacheConfidence.sampleCount()),
                    this.config.noisyCellFraction()
            ));
            remainingCells -= noisyCells;
            addClass(classes, AdaptiveGiRayBudgetClass.NOISY, noisyCells, "variance above adaptive threshold");
        } else if (cacheConfidence.variance() >= this.config.mediumVarianceThreshold()) {
            int varianceCells = takeCells(remainingCells, boostedCellCount(
                    totalCells,
                    Math.max(1, cacheConfidence.sampleCount()),
                    this.config.noisyCellFraction()
            ));
            remainingCells -= varianceCells;
            addClass(classes, AdaptiveGiRayBudgetClass.VARIANCE_REFRESH, varianceCells, "variance requires refresh");
        }

        if (!cacheConfidence.dirty() && cacheConfidence.confidence() < this.config.reuseConfidenceThreshold()) {
            int lowConfidenceCells = takeCells(remainingCells, boostedCellCount(
                    totalCells,
                    Math.max(1, cacheConfidence.sampleCount()),
                    this.config.lowConfidenceCellFraction()
            ));
            remainingCells -= lowConfidenceCells;
            AdaptiveGiRayBudgetClass confidenceClass = cacheConfidence.confidence() < this.config.lowConfidenceThreshold()
                    ? AdaptiveGiRayBudgetClass.LOW_CONFIDENCE
                    : AdaptiveGiRayBudgetClass.CACHE_CONFIDENCE;
            addClass(classes, confidenceClass, lowConfidenceCells, "cache confidence below reuse floor");
        }

        AdaptiveGiRayBudgetClass stableClass = temporalInput.reuseAllowed()
                && !cacheConfidence.dirty()
                && cacheConfidence.confidence() >= this.config.reuseConfidenceThreshold()
                ? AdaptiveGiRayBudgetClass.STABLE_REUSE
                : AdaptiveGiRayBudgetClass.STABLE_REFRESH;
        addClass(classes, stableClass, remainingCells, "stable residual cells");

        return AdaptiveGiRayBudgetMap.fromRequestedClasses(
                totalCells,
                classes,
                this.config.maxRaysPerFrame(),
                reason
        );
    }

    private void addClass(
            List<AdaptiveGiRayBudgetClassAllocation> classes,
            AdaptiveGiRayBudgetClass budgetClass,
            int cellCount,
            String reason
    ) {
        if (cellCount <= 0) {
            return;
        }
        classes.add(AdaptiveGiRayBudgetClassAllocation.requested(budgetClass, cellCount, this.config, reason));
    }

    private int boostedCellCount(int totalCells, int signalCount, float fraction) {
        if (totalCells <= 0 || signalCount <= 0) {
            return 0;
        }
        int fractionCells = (int) Math.ceil(totalCells * fraction);
        int signalCells = Math.min(totalCells, signalCount * Math.max(1, this.config.minBoostedCells()));
        return Math.min(totalCells, Math.max(Math.max(1, this.config.minBoostedCells()), Math.max(fractionCells, signalCells)));
    }

    private static int takeCells(int remainingCells, int requestedCells) {
        return Math.min(Math.max(0, remainingCells), Math.max(0, requestedCells));
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
