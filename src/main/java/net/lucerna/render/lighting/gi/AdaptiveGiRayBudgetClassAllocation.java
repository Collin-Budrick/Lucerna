package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record AdaptiveGiRayBudgetClassAllocation(
        AdaptiveGiRayBudgetClass budgetClass,
        GiRayBudgetTier tier,
        int cellCount,
        int raysPerCell,
        int requestedRays,
        int cappedRays,
        boolean capped,
        String reason
) {
    public AdaptiveGiRayBudgetClassAllocation {
        Objects.requireNonNull(budgetClass, "budgetClass");
        if (tier == null) {
            tier = budgetClass.tier();
        }
        requireNonNegative(cellCount, "cellCount");
        requireNonNegative(raysPerCell, "raysPerCell");
        requireNonNegative(requestedRays, "requestedRays");
        requireNonNegative(cappedRays, "cappedRays");
        if (cappedRays > requestedRays) {
            throw new IllegalArgumentException("cappedRays cannot exceed requestedRays");
        }
        if (requestedRays != clampToInt((long) cellCount * (long) raysPerCell)) {
            throw new IllegalArgumentException("requestedRays must match cellCount * raysPerCell");
        }
        capped = capped || cappedRays < requestedRays;
        reason = clean(reason, budgetClass.label());
    }

    public static AdaptiveGiRayBudgetClassAllocation requested(
            AdaptiveGiRayBudgetClass budgetClass,
            int cellCount,
            AdaptiveGiRayBudgetConfig config,
            String reason
    ) {
        Objects.requireNonNull(budgetClass, "budgetClass");
        Objects.requireNonNull(config, "config");
        GiRayBudgetTier tier = budgetClass.tier();
        int raysPerCell = config.raysPerCell(tier);
        int requestedRays = clampToInt((long) Math.max(0, cellCount) * (long) raysPerCell);
        return new AdaptiveGiRayBudgetClassAllocation(
                budgetClass,
                tier,
                cellCount,
                raysPerCell,
                requestedRays,
                requestedRays,
                false,
                reason
        );
    }

    public AdaptiveGiRayBudgetClassAllocation withCappedRays(int cappedRays) {
        return new AdaptiveGiRayBudgetClassAllocation(
                this.budgetClass,
                this.tier,
                this.cellCount,
                this.raysPerCell,
                this.requestedRays,
                Math.max(0, cappedRays),
                Math.max(0, cappedRays) < this.requestedRays,
                this.reason
        );
    }

    public String compactLabel() {
        return this.budgetClass.label()
                + ":" + this.cellCount
                + "@" + this.raysPerCell
                + "=" + this.cappedRays + "/" + this.requestedRays;
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
