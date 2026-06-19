package net.lucerna.world;

import java.util.Objects;

public record DirtyRegionKey(
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ
) {
    public DirtyRegionKey {
        Objects.requireNonNull(dimension, "dimension");
    }
}
