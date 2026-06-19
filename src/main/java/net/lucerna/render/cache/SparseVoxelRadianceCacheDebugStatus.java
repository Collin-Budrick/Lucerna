package net.lucerna.render.cache;

public record SparseVoxelRadianceCacheDebugStatus(
        long cacheGeneration,
        int recordCount,
        int dirtyRecordCount,
        int usableRecordCount,
        float combinedConfidence,
        float maxVariance,
        long latestSourceGeneration,
        SparseVoxelRadianceCacheInvalidationSummary lastInvalidation,
        String reason
) {
    public SparseVoxelRadianceCacheDebugStatus {
        cacheGeneration = Math.max(0L, cacheGeneration);
        recordCount = Math.max(0, recordCount);
        dirtyRecordCount = Math.max(0, dirtyRecordCount);
        usableRecordCount = Math.max(0, usableRecordCount);
        combinedConfidence = clampUnit(combinedConfidence);
        maxVariance = finiteNonNegative(maxVariance);
        latestSourceGeneration = Math.max(0L, latestSourceGeneration);
        if (lastInvalidation == null) {
            lastInvalidation = SparseVoxelRadianceCacheInvalidationSummary.empty(cacheGeneration);
        }
        if (reason == null || reason.isBlank()) {
            reason = "sparse voxel radiance cache status";
        } else {
            reason = reason.trim();
        }
    }

    public static SparseVoxelRadianceCacheDebugStatus empty() {
        return new SparseVoxelRadianceCacheDebugStatus(
                0L,
                0,
                0,
                0,
                0.0F,
                1.0F,
                0L,
                SparseVoxelRadianceCacheInvalidationSummary.empty(0L),
                "sparse voxel radiance cache has no records"
        );
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
