package net.lucerna.render.cache;

public record SparseVoxelRadianceCacheInvalidationSummary(
        long cacheGeneration,
        long latestDirtyGeneration,
        int sourceDirtyRegionCount,
        int coalescedDirtyRegionCount,
        int pendingDirtyRegionCountAfterDrain,
        int affectedRecordCount,
        int retainedRecordCount,
        int createdDirtyPlaceholderCount,
        boolean globalInvalidation,
        String reason
) {
    public SparseVoxelRadianceCacheInvalidationSummary {
        cacheGeneration = Math.max(0L, cacheGeneration);
        latestDirtyGeneration = Math.max(0L, latestDirtyGeneration);
        sourceDirtyRegionCount = Math.max(0, sourceDirtyRegionCount);
        coalescedDirtyRegionCount = Math.max(0, coalescedDirtyRegionCount);
        pendingDirtyRegionCountAfterDrain = Math.max(0, pendingDirtyRegionCountAfterDrain);
        affectedRecordCount = Math.max(0, affectedRecordCount);
        retainedRecordCount = Math.max(0, retainedRecordCount);
        createdDirtyPlaceholderCount = Math.max(0, createdDirtyPlaceholderCount);
        if (reason == null || reason.isBlank()) {
            reason = globalInvalidation ? "sparse voxel radiance cache globally invalidated" : "sparse voxel radiance cache unchanged";
        } else {
            reason = reason.trim();
        }
    }

    public static SparseVoxelRadianceCacheInvalidationSummary empty(long cacheGeneration) {
        return new SparseVoxelRadianceCacheInvalidationSummary(
                cacheGeneration,
                0L,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                "no dirty regions"
        );
    }

    public boolean invalidatedAnything() {
        return this.globalInvalidation || this.affectedRecordCount > 0 || this.createdDirtyPlaceholderCount > 0;
    }
}
