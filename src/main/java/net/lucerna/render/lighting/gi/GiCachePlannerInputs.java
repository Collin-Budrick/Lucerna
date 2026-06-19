package net.lucerna.render.lighting.gi;

import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionSnapshot;

import java.util.Objects;

public record GiCachePlannerInputs(
        GiCacheSnapshot cacheSnapshot,
        CacheConfidence cacheConfidence,
        GiRayBudgetTier rayBudgetPressure,
        int sourceDirtyRegionCount,
        int coalescedDirtyRegionCount,
        int pendingDirtyRegionCountAfterDrain,
        String reason
) {
    public GiCachePlannerInputs {
        if (cacheSnapshot == null) {
            cacheSnapshot = GiCacheSnapshot.empty();
        }
        if (cacheConfidence == null) {
            cacheConfidence = CacheConfidence.empty("GI cache planner confidence unavailable");
        }
        if (rayBudgetPressure == null) {
            rayBudgetPressure = GiRayBudgetTier.HIGH;
        }
        sourceDirtyRegionCount = Math.max(0, sourceDirtyRegionCount);
        coalescedDirtyRegionCount = Math.max(0, coalescedDirtyRegionCount);
        pendingDirtyRegionCountAfterDrain = Math.max(0, pendingDirtyRegionCountAfterDrain);
        reason = clean(reason, cacheConfidence.reason());
    }

    public static GiCachePlannerInputs empty() {
        return GiCacheSnapshotBuilder.create().buildPlannerInputs();
    }

    public static GiCachePlannerInputs from(DirtyRegionSnapshot dirtyRegionSnapshot) {
        return GiCacheSnapshotBuilder.from(dirtyRegionSnapshot).buildPlannerInputs();
    }

    public static GiCachePlannerInputs from(DirtyRegionSnapshot dirtyRegionSnapshot, long cacheGenerationHint) {
        return GiCacheSnapshotBuilder.from(dirtyRegionSnapshot)
                .cacheGenerationHint(cacheGenerationHint)
                .buildPlannerInputs();
    }

    public static GiCachePlannerInputs from(DirtyRegionBatch dirtyRegionBatch) {
        return GiCacheSnapshotBuilder.from(dirtyRegionBatch).buildPlannerInputs();
    }

    public static GiCachePlannerInputs from(DirtyRegionBatch dirtyRegionBatch, long cacheGenerationHint) {
        return GiCacheSnapshotBuilder.from(dirtyRegionBatch)
                .cacheGenerationHint(cacheGenerationHint)
                .buildPlannerInputs();
    }

    public GiCacheSnapshot snapshot() {
        return this.cacheSnapshot;
    }

    public boolean hasDirtyRegions() {
        return this.cacheSnapshot.hasDirtyRegions();
    }

    public boolean emptyCache() {
        return this.cacheSnapshot.isEmpty();
    }

    public boolean appliesRayBudgetPressure() {
        return this.rayBudgetPressure.requiresTracing() || this.rayBudgetPressure == GiRayBudgetTier.HIGH;
    }

    public DirtyRegionBatch dirtyRegions() {
        return this.cacheSnapshot.dirtyRegions();
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI cache planner inputs");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
