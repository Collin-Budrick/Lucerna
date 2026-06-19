package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record GiRayBudgetAllocation(
        GiRayBudgetTier tier,
        int raysPerCell,
        int lowResolutionCellCount,
        int requestedRays,
        int cappedRays,
        boolean capped,
        String reason
) {
    public GiRayBudgetAllocation {
        Objects.requireNonNull(tier, "tier");
        requireNonNegative(raysPerCell, "raysPerCell");
        requireNonNegative(lowResolutionCellCount, "lowResolutionCellCount");
        requireNonNegative(requestedRays, "requestedRays");
        requireNonNegative(cappedRays, "cappedRays");
        if (cappedRays > requestedRays) {
            throw new IllegalArgumentException("cappedRays cannot exceed requestedRays");
        }
        if (tier == GiRayBudgetTier.DISABLED && (raysPerCell != 0 || requestedRays != 0 || cappedRays != 0)) {
            throw new IllegalArgumentException("disabled GI ray budgets must not request rays");
        }
        reason = clean(reason, tier.name().toLowerCase());
    }

    public static GiRayBudgetAllocation disabled(DiffuseGiLowResolutionGrid grid, String reason) {
        int cellCount = grid == null ? 0 : grid.cellCount();
        return new GiRayBudgetAllocation(GiRayBudgetTier.DISABLED, 0, cellCount, 0, 0, false, reason);
    }

    public static GiRayBudgetAllocation fromTier(
            GiRayBudgetTier tier,
            DiffuseGiLowResolutionGrid grid,
            AdaptiveGiRayBudgetConfig config,
            String reason
    ) {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(config, "config");
        int cellCount = grid == null ? 0 : grid.cellCount();
        int raysPerCell = config.raysPerCell(tier);
        int requestedRays = clampToInt((long) cellCount * (long) raysPerCell);
        int cappedRays = Math.min(requestedRays, config.maxRaysPerFrame());
        return new GiRayBudgetAllocation(
                tier,
                raysPerCell,
                cellCount,
                requestedRays,
                cappedRays,
                cappedRays < requestedRays,
                reason
        );
    }

    public boolean hasTraceBudget() {
        return this.tier.requiresTracing() && this.cappedRays > 0;
    }

    public boolean reuseOnly() {
        return this.tier == GiRayBudgetTier.REUSE_ONLY;
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(0L, value);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
