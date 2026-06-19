package net.lucerna.render.cache;

import net.lucerna.world.DirtyRegionSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SparseVoxelRadianceCacheSnapshot(
        long cacheGeneration,
        DirtyRegionSnapshot dirtyRegionSnapshot,
        List<SparseVoxelRadianceCacheRecord> records,
        SparseVoxelRadianceCacheInvalidationSummary invalidationSummary,
        SparseVoxelRadianceCacheDebugStatus debugStatus
) {
    public SparseVoxelRadianceCacheSnapshot {
        cacheGeneration = Math.max(0L, cacheGeneration);
        if (dirtyRegionSnapshot == null) {
            dirtyRegionSnapshot = DirtyRegionSnapshot.empty();
        }
        Objects.requireNonNull(records, "records");
        records = List.copyOf(records);
        validateRecords(records);
        if (invalidationSummary == null) {
            invalidationSummary = SparseVoxelRadianceCacheInvalidationSummary.empty(cacheGeneration);
        }
        if (debugStatus == null) {
            debugStatus = SparseVoxelRadianceCacheDebugStatus.empty();
        }
    }

    public static SparseVoxelRadianceCacheSnapshot empty() {
        return new SparseVoxelRadianceCacheSnapshot(
                0L,
                DirtyRegionSnapshot.empty(),
                List.of(),
                SparseVoxelRadianceCacheInvalidationSummary.empty(0L),
                SparseVoxelRadianceCacheDebugStatus.empty()
        );
    }

    public int recordCount() {
        return this.records.size();
    }

    public boolean hasRecords() {
        return !this.records.isEmpty();
    }

    public boolean hasDirtyRegions() {
        return !this.dirtyRegionSnapshot.isEmpty();
    }

    public SparseVoxelRadianceCacheConfidence combinedConfidence() {
        return SparseVoxelRadianceCacheConfidence.merge(this.records.stream()
                .map(SparseVoxelRadianceCacheRecord::confidence)
                .toList());
    }

    private static void validateRecords(List<SparseVoxelRadianceCacheRecord> records) {
        Set<String> keys = new HashSet<>();
        for (SparseVoxelRadianceCacheRecord record : records) {
            Objects.requireNonNull(record, "records must not contain null entries");
            if (!keys.add(record.key().stableKey())) {
                throw new IllegalArgumentException("records must be unique by sparse voxel radiance cache key");
            }
        }
    }
}
