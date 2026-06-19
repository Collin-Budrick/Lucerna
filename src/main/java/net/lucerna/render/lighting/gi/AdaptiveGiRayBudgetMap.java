package net.lucerna.render.lighting.gi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AdaptiveGiRayBudgetMap(
        int lowResolutionCellCount,
        List<AdaptiveGiRayBudgetClassAllocation> classes,
        int requestedRays,
        int cappedRays,
        boolean capped,
        String reason
) {
    public AdaptiveGiRayBudgetMap {
        requireNonNegative(lowResolutionCellCount, "lowResolutionCellCount");
        Objects.requireNonNull(classes, "classes");
        classes = List.copyOf(classes);
        requestedRays = classes.stream().mapToInt(AdaptiveGiRayBudgetClassAllocation::requestedRays).sum();
        cappedRays = classes.stream().mapToInt(AdaptiveGiRayBudgetClassAllocation::cappedRays).sum();
        capped = capped || cappedRays < requestedRays;
        reason = clean(reason, "adaptive GI ray budget map");
    }

    public static AdaptiveGiRayBudgetMap empty(String reason) {
        return new AdaptiveGiRayBudgetMap(0, List.of(), 0, 0, false, reason);
    }

    public static AdaptiveGiRayBudgetMap fromRequestedClasses(
            int lowResolutionCellCount,
            List<AdaptiveGiRayBudgetClassAllocation> requestedClasses,
            int maxRaysPerFrame,
            String reason
    ) {
        int safeMaxRays = Math.max(0, maxRaysPerFrame);
        List<AdaptiveGiRayBudgetClassAllocation> requested = requestedClasses == null
                ? List.of()
                : requestedClasses.stream()
                .filter(Objects::nonNull)
                .filter(allocation -> allocation.cellCount() > 0)
                .toList();
        int requestedRays = requested.stream().mapToInt(AdaptiveGiRayBudgetClassAllocation::requestedRays).sum();
        if (requestedRays <= safeMaxRays) {
            return new AdaptiveGiRayBudgetMap(lowResolutionCellCount, requested, requestedRays, requestedRays, false, reason);
        }

        List<AdaptiveGiRayBudgetClassAllocation> cappedClasses = new ArrayList<>(requested.size());
        int remainingCap = safeMaxRays;
        int remainingRequested = requestedRays;
        for (AdaptiveGiRayBudgetClassAllocation allocation : requested) {
            int cappedRays = remainingRequested <= 0
                    ? 0
                    : clampToInt((long) allocation.requestedRays() * (long) remainingCap / (long) remainingRequested);
            cappedRays = Math.min(allocation.requestedRays(), Math.max(0, cappedRays));
            cappedClasses.add(allocation.withCappedRays(cappedRays));
            remainingCap -= cappedRays;
            remainingRequested -= allocation.requestedRays();
        }
        return new AdaptiveGiRayBudgetMap(lowResolutionCellCount, cappedClasses, requestedRays, safeMaxRays, true, reason);
    }

    public static AdaptiveGiRayBudgetMap singleClass(
            DiffuseGiLowResolutionGrid grid,
            AdaptiveGiRayBudgetClass budgetClass,
            AdaptiveGiRayBudgetConfig config,
            String reason
    ) {
        int cellCount = grid == null ? 0 : grid.cellCount();
        if (cellCount == 0) {
            return empty(reason);
        }
        return fromRequestedClasses(
                cellCount,
                List.of(AdaptiveGiRayBudgetClassAllocation.requested(budgetClass, cellCount, config, reason)),
                config.maxRaysPerFrame(),
                reason
        );
    }

    public GiRayBudgetTier highestTier() {
        GiRayBudgetTier tier = GiRayBudgetTier.DISABLED;
        for (AdaptiveGiRayBudgetClassAllocation allocation : this.classes) {
            if (allocation.cellCount() > 0 && allocation.tier().ordinal() > tier.ordinal()) {
                tier = allocation.tier();
            }
        }
        return tier;
    }

    public int raysPerCellForTier(GiRayBudgetTier tier) {
        return this.classes.stream()
                .filter(allocation -> allocation.tier() == tier)
                .mapToInt(AdaptiveGiRayBudgetClassAllocation::raysPerCell)
                .max()
                .orElse(0);
    }

    public String compactLabel() {
        if (this.classes.isEmpty()) {
            return "empty";
        }
        StringBuilder builder = new StringBuilder();
        for (AdaptiveGiRayBudgetClassAllocation allocation : this.classes) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(allocation.compactLabel());
        }
        return builder.toString();
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
