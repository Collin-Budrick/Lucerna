package net.lucerna.world;

import java.util.Objects;

public record DirtyRegion(
        DirtyRegionType type,
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ,
        long generation
) {
    public DirtyRegion {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dimension, "dimension");
        if (generation <= 0) {
            throw new IllegalArgumentException("generation must be positive");
        }
    }

    public DirtyRegionKey key() {
        return new DirtyRegionKey(this.dimension, this.sectionX, this.sectionY, this.sectionZ);
    }

    public DirtyRegionCoalesceKey coalesceKey() {
        return DirtyRegionCoalesceKey.from(this);
    }

    public boolean sectionScoped() {
        return this.type.sectionScoped();
    }
}
