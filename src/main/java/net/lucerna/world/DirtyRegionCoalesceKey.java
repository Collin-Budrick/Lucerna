package net.lucerna.world;

import java.util.Objects;

public record DirtyRegionCoalesceKey(
        DirtyRegionType type,
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ
) {
    public DirtyRegionCoalesceKey {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dimension, "dimension");
    }

    public static DirtyRegionCoalesceKey from(DirtyRegion region) {
        Objects.requireNonNull(region, "region");
        return new DirtyRegionCoalesceKey(
                region.type(),
                region.dimension(),
                region.sectionX(),
                region.sectionY(),
                region.sectionZ()
        );
    }

    public DirtyRegionKey regionKey() {
        return new DirtyRegionKey(this.dimension, this.sectionX, this.sectionY, this.sectionZ);
    }
}
