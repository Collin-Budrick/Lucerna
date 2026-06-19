package net.lucerna.render.cache;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionSnapshot;
import net.lucerna.world.DirtyRegionType;

import java.util.Objects;

public record SparseVoxelRadianceCacheInvalidationPolicy(
        int sectionPadding,
        int sectionsPerCell,
        int maxCascade,
        boolean invalidateOnResourcePackReload,
        boolean invalidateOnDimensionChange,
        boolean invalidateOnWorldBoundary,
        boolean invalidateOnWeatherChange,
        boolean invalidateOnTimeOfDayChange,
        boolean invalidateOnChunkUnload
) {
    public SparseVoxelRadianceCacheInvalidationPolicy {
        sectionPadding = Math.max(0, sectionPadding);
        sectionsPerCell = Math.max(1, sectionsPerCell);
        maxCascade = Math.max(0, maxCascade);
    }

    public static SparseVoxelRadianceCacheInvalidationPolicy conservative() {
        return new SparseVoxelRadianceCacheInvalidationPolicy(1, 1, 0, true, true, true, true, true, true);
    }

    public boolean invalidatesAll(DirtyRegionSnapshot dirtyRegionSnapshot) {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.invalidatesAll(dirtyRegionSnapshot.batch());
    }

    public boolean invalidatesAll(DirtyRegionBatch dirtyRegionBatch) {
        Objects.requireNonNull(dirtyRegionBatch, "dirtyRegionBatch");
        return dirtyRegionBatch.regions().stream().anyMatch(this::invalidatesAll);
    }

    public boolean invalidatesAll(DirtyRegion dirtyRegion) {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        return switch (dirtyRegion.type()) {
            case RESOURCE_PACK_RELOAD -> this.invalidateOnResourcePackReload;
            case DIMENSION_CHANGE -> this.invalidateOnDimensionChange;
            case WORLD_JOIN, WORLD_LEAVE -> this.invalidateOnWorldBoundary;
            case WEATHER_CHANGE -> this.invalidateOnWeatherChange;
            case TIME_OF_DAY_CHANGE -> this.invalidateOnTimeOfDayChange;
            default -> false;
        };
    }

    public boolean affects(SparseVoxelRadianceCacheRecord record, DirtyRegion dirtyRegion) {
        Objects.requireNonNull(record, "record");
        return this.affects(record.key(), dirtyRegion);
    }

    public boolean affects(SparseVoxelRadianceCacheKey key, DirtyRegion dirtyRegion) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        if (this.invalidatesAll(dirtyRegion)) {
            return true;
        }
        if (!dirtyRegion.sectionScoped()) {
            return false;
        }
        if (dirtyRegion.type() == DirtyRegionType.CHUNK_UNLOAD && !this.invalidateOnChunkUnload) {
            return false;
        }
        int dirtyCellX = Math.floorDiv(dirtyRegion.sectionX(), this.sectionsPerCell);
        int dirtyCellY = Math.floorDiv(dirtyRegion.sectionY(), this.sectionsPerCell);
        int dirtyCellZ = Math.floorDiv(dirtyRegion.sectionZ(), this.sectionsPerCell);
        return Objects.equals(key.dimension(), dirtyRegion.dimension())
                && key.cascade() <= this.maxCascade
                && withinPadding(key.cellX(), dirtyCellX)
                && withinPadding(key.cellY(), dirtyCellY)
                && withinPadding(key.cellZ(), dirtyCellZ);
    }

    public SparseVoxelRadianceCacheConfidence dirtyConfidence(DirtyRegion dirtyRegion, String reason) {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        return SparseVoxelRadianceCacheConfidence.dirty(dirtyRegion.generation(), reason);
    }

    private boolean withinPadding(int cacheCoordinate, int dirtyCoordinate) {
        return Math.abs(cacheCoordinate - dirtyCoordinate) <= this.sectionPadding;
    }
}
