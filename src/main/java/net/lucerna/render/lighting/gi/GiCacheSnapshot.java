package net.lucerna.render.lighting.gi;

import net.lucerna.world.DirtyRegionBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record GiCacheSnapshot(
        long cacheGeneration,
        DirtyRegionBatch dirtyRegions,
        List<SurfaceCacheRecord> surfaceRecords,
        List<RadianceCacheRecord> radianceRecords
) {
    public GiCacheSnapshot {
        cacheGeneration = Math.max(0L, cacheGeneration);
        if (dirtyRegions == null) {
            dirtyRegions = DirtyRegionBatch.empty();
        }
        Objects.requireNonNull(surfaceRecords, "surfaceRecords");
        Objects.requireNonNull(radianceRecords, "radianceRecords");
        surfaceRecords = List.copyOf(surfaceRecords);
        radianceRecords = List.copyOf(radianceRecords);
        validateSurfaceRecords(surfaceRecords);
        validateRadianceRecords(radianceRecords);
        long maxRecordGeneration = Math.max(
                surfaceRecords.stream().mapToLong(SurfaceCacheRecord::generation).max().orElse(0L),
                radianceRecords.stream().mapToLong(RadianceCacheRecord::generation).max().orElse(0L)
        );
        if (cacheGeneration < maxRecordGeneration) {
            throw new IllegalArgumentException("cacheGeneration must cover all cache record generations");
        }
    }

    public static GiCacheSnapshot empty() {
        return new GiCacheSnapshot(0L, DirtyRegionBatch.empty(), List.of(), List.of());
    }

    public static GiCacheSnapshot from(
            long cacheGeneration,
            DirtyRegionBatch dirtyRegions,
            List<SurfaceCacheRecord> surfaceRecords,
            List<RadianceCacheRecord> radianceRecords
    ) {
        return new GiCacheSnapshot(cacheGeneration, dirtyRegions, surfaceRecords, radianceRecords);
    }

    public int surfaceRecordCount() {
        return this.surfaceRecords.size();
    }

    public int radianceRecordCount() {
        return this.radianceRecords.size();
    }

    public boolean isEmpty() {
        return this.surfaceRecords.isEmpty() && this.radianceRecords.isEmpty();
    }

    public boolean hasSurfaceRecords() {
        return !this.surfaceRecords.isEmpty();
    }

    public boolean hasRadianceRecords() {
        return !this.radianceRecords.isEmpty();
    }

    public boolean hasDirtyRegions() {
        return !this.dirtyRegions.isEmpty();
    }

    public int dirtyRegionCount() {
        return this.dirtyRegions.dirtyRegionCount();
    }

    public long latestDirtyGeneration() {
        return this.dirtyRegions.lastGeneration();
    }

    public boolean globallyInvalidatedBy(GiCacheInvalidationPolicy invalidationPolicy) {
        GiCacheInvalidationPolicy resolvedPolicy = invalidationPolicy == null
                ? GiCacheInvalidationPolicy.conservative()
                : invalidationPolicy;
        return resolvedPolicy.invalidatesAll(this.dirtyRegions);
    }

    public CacheConfidence combinedConfidence() {
        List<CacheConfidence> confidences = new ArrayList<>();
        this.surfaceRecords.stream()
                .map(SurfaceCacheRecord::confidence)
                .forEach(confidences::add);
        this.radianceRecords.stream()
                .map(RadianceCacheRecord::confidence)
                .forEach(confidences::add);
        return CacheConfidence.merge(confidences);
    }

    private static void validateSurfaceRecords(List<SurfaceCacheRecord> records) {
        Set<String> keys = new HashSet<>();
        for (SurfaceCacheRecord record : records) {
            Objects.requireNonNull(record, "surfaceRecords must not contain null entries");
            if (!keys.add(record.key().stableKey())) {
                throw new IllegalArgumentException("surfaceRecords must be unique by cache key");
            }
        }
    }

    private static void validateRadianceRecords(List<RadianceCacheRecord> records) {
        Set<String> keys = new HashSet<>();
        for (RadianceCacheRecord record : records) {
            Objects.requireNonNull(record, "radianceRecords must not contain null entries");
            if (!keys.add(record.key().stableKey())) {
                throw new IllegalArgumentException("radianceRecords must be unique by cache key");
            }
        }
    }
}
