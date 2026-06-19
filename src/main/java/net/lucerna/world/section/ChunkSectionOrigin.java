package net.lucerna.world.section;

import java.util.Objects;

public record ChunkSectionOrigin(
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ
) {
    public static final int SECTION_EDGE_LENGTH = 16;
    public static final int SECTION_VOLUME = SECTION_EDGE_LENGTH * SECTION_EDGE_LENGTH * SECTION_EDGE_LENGTH;

    public ChunkSectionOrigin {
        Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
    }

    public int minBlockX() {
        return this.sectionX * SECTION_EDGE_LENGTH;
    }

    public int minBlockY() {
        return this.sectionY * SECTION_EDGE_LENGTH;
    }

    public int minBlockZ() {
        return this.sectionZ * SECTION_EDGE_LENGTH;
    }

    public String stableKey() {
        return this.dimension + ":" + this.sectionX + "," + this.sectionY + "," + this.sectionZ;
    }
}
