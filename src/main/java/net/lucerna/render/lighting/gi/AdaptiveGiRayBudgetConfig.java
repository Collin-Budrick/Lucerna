package net.lucerna.render.lighting.gi;

public record AdaptiveGiRayBudgetConfig(
        int reuseOnlyRaysPerCell,
        int lowRaysPerCell,
        int mediumRaysPerCell,
        int highRaysPerCell,
        int maxRaysPerFrame,
        float mediumVarianceThreshold,
        float highVarianceThreshold,
        float lowConfidenceThreshold,
        float reuseConfidenceThreshold,
        int dirtyRegionBoostThreshold,
        float dirtyRegionCellFraction,
        float emissiveCellFraction,
        float noisyCellFraction,
        float lowConfidenceCellFraction,
        int minBoostedCells
) {
    public AdaptiveGiRayBudgetConfig {
        requireNonNegative(reuseOnlyRaysPerCell, "reuseOnlyRaysPerCell");
        requirePositive(lowRaysPerCell, "lowRaysPerCell");
        requirePositive(mediumRaysPerCell, "mediumRaysPerCell");
        requirePositive(highRaysPerCell, "highRaysPerCell");
        requirePositive(maxRaysPerFrame, "maxRaysPerFrame");
        if (reuseOnlyRaysPerCell > lowRaysPerCell || lowRaysPerCell > mediumRaysPerCell
                || mediumRaysPerCell > highRaysPerCell) {
            throw new IllegalArgumentException("ray tiers must be monotonic");
        }
        mediumVarianceThreshold = finiteNonNegative(mediumVarianceThreshold);
        highVarianceThreshold = finiteNonNegative(highVarianceThreshold);
        if (highVarianceThreshold < mediumVarianceThreshold) {
            throw new IllegalArgumentException("highVarianceThreshold must be greater than or equal to mediumVarianceThreshold");
        }
        lowConfidenceThreshold = clampUnit(lowConfidenceThreshold);
        reuseConfidenceThreshold = clampUnit(reuseConfidenceThreshold);
        if (reuseConfidenceThreshold < lowConfidenceThreshold) {
            throw new IllegalArgumentException("reuseConfidenceThreshold must be greater than or equal to lowConfidenceThreshold");
        }
        dirtyRegionBoostThreshold = Math.max(0, dirtyRegionBoostThreshold);
        dirtyRegionCellFraction = clampUnit(dirtyRegionCellFraction);
        emissiveCellFraction = clampUnit(emissiveCellFraction);
        noisyCellFraction = clampUnit(noisyCellFraction);
        lowConfidenceCellFraction = clampUnit(lowConfidenceCellFraction);
        minBoostedCells = Math.max(0, minBoostedCells);
    }

    public static AdaptiveGiRayBudgetConfig firstMilestone() {
        return new AdaptiveGiRayBudgetConfig(
                0,
                1,
                2,
                4,
                1_000_000,
                0.20F,
                0.50F,
                0.45F,
                0.80F,
                1,
                0.20F,
                0.12F,
                0.30F,
                0.25F,
                64
        );
    }

    public int raysPerCell(GiRayBudgetTier tier) {
        return switch (tier) {
            case DISABLED -> 0;
            case REUSE_ONLY -> this.reuseOnlyRaysPerCell;
            case LOW -> this.lowRaysPerCell;
            case MEDIUM -> this.mediumRaysPerCell;
            case HIGH -> this.highRaysPerCell;
        };
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float finiteNonNegative(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }
}
