package net.lucerna.render.lighting.gi;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionType;

import java.util.Objects;

public record GiCacheInvalidationPolicy(
        int sectionPadding,
        boolean invalidateOnResourcePackReload,
        boolean invalidateOnDimensionChange,
        boolean invalidateOnWorldBoundary,
        boolean invalidateOnWeatherChange,
        boolean invalidateOnTimeOfDayChange,
        boolean invalidateOnChunkUnload
) {
    public GiCacheInvalidationPolicy {
        sectionPadding = Math.max(0, sectionPadding);
    }

    public static GiCacheInvalidationPolicy conservative() {
        return new GiCacheInvalidationPolicy(true);
    }

    public GiCacheInvalidationPolicy(boolean globalOnEnvironmentChanges) {
        this(
                1,
                true,
                true,
                true,
                globalOnEnvironmentChanges,
                globalOnEnvironmentChanges,
                false
        );
    }

    public boolean invalidatesAll(DirtyRegionBatch dirtyRegions) {
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        return dirtyRegions.regions().stream().anyMatch(this::invalidatesAll);
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

    public boolean affects(SurfaceCacheRecord record, DirtyRegionBatch dirtyRegions) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        return dirtyRegions.regions().stream().anyMatch(region -> this.affects(record.key(), region));
    }

    public boolean affects(RadianceCacheRecord record, DirtyRegionBatch dirtyRegions) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        return dirtyRegions.regions().stream().anyMatch(region -> this.affects(record.key(), region));
    }

    public boolean affects(SurfaceCacheKey key, DirtyRegion dirtyRegion) {
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
        return sameDimension(key.dimension(), dirtyRegion.dimension())
                && withinPadding(key.sectionX(), dirtyRegion.sectionX())
                && withinPadding(key.sectionY(), dirtyRegion.sectionY())
                && withinPadding(key.sectionZ(), dirtyRegion.sectionZ());
    }

    public boolean affects(RadianceCacheKey key, DirtyRegion dirtyRegion) {
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
        return sameDimension(key.dimension(), dirtyRegion.dimension())
                && withinPadding(key.cellX(), dirtyRegion.sectionX())
                && withinPadding(key.cellY(), dirtyRegion.sectionY())
                && withinPadding(key.cellZ(), dirtyRegion.sectionZ());
    }

    private boolean withinPadding(int cacheCoordinate, int dirtyCoordinate) {
        return Math.abs(cacheCoordinate - dirtyCoordinate) <= this.sectionPadding;
    }

    private static boolean sameDimension(String left, String right) {
        return Objects.equals(left, right);
    }
}
