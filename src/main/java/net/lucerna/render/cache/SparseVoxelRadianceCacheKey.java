package net.lucerna.render.cache;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionKey;

import java.util.Objects;

public record SparseVoxelRadianceCacheKey(
        String dimension,
        int cellX,
        int cellY,
        int cellZ,
        int cascade
) {
    public SparseVoxelRadianceCacheKey {
        dimension = requireText(dimension, "dimension");
        if (cascade < 0) {
            throw new IllegalArgumentException("cascade must be non-negative");
        }
    }

    public static SparseVoxelRadianceCacheKey fromDirtyRegion(DirtyRegion dirtyRegion, int cascade, int sectionsPerCell) {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        return fromDirtyRegionKey(dirtyRegion.key(), cascade, sectionsPerCell);
    }

    public static SparseVoxelRadianceCacheKey fromDirtyRegionKey(DirtyRegionKey dirtyRegionKey, int cascade, int sectionsPerCell) {
        Objects.requireNonNull(dirtyRegionKey, "dirtyRegionKey");
        int resolvedSectionsPerCell = Math.max(1, sectionsPerCell);
        return new SparseVoxelRadianceCacheKey(
                dirtyRegionKey.dimension(),
                Math.floorDiv(dirtyRegionKey.sectionX(), resolvedSectionsPerCell),
                Math.floorDiv(dirtyRegionKey.sectionY(), resolvedSectionsPerCell),
                Math.floorDiv(dirtyRegionKey.sectionZ(), resolvedSectionsPerCell),
                cascade
        );
    }

    public String stableKey() {
        return this.dimension + ":" + this.cellX + "," + this.cellY + "," + this.cellZ + "#" + this.cascade;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
