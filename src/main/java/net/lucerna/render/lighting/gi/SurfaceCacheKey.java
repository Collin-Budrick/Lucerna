package net.lucerna.render.lighting.gi;

import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.Objects;

public record SurfaceCacheKey(
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ,
        int localX,
        int localY,
        int localZ,
        int faceOrdinal
) {
    public SurfaceCacheKey {
        dimension = requireText(dimension, "dimension");
        requireLocal(localX, "localX");
        requireLocal(localY, "localY");
        requireLocal(localZ, "localZ");
        if (faceOrdinal < 0) {
            throw new IllegalArgumentException("faceOrdinal must be non-negative");
        }
    }

    public static SurfaceCacheKey fromSection(
            ChunkSectionOrigin origin,
            int localX,
            int localY,
            int localZ,
            int faceOrdinal
    ) {
        Objects.requireNonNull(origin, "origin");
        return new SurfaceCacheKey(
                origin.dimension(),
                origin.sectionX(),
                origin.sectionY(),
                origin.sectionZ(),
                localX,
                localY,
                localZ,
                faceOrdinal
        );
    }

    public String stableKey() {
        return this.dimension + ":" + this.sectionX + "," + this.sectionY + "," + this.sectionZ
                + "/" + this.localX + "," + this.localY + "," + this.localZ + ":" + this.faceOrdinal;
    }

    private static void requireLocal(int value, String name) {
        if (value < 0 || value >= ChunkSectionOrigin.SECTION_EDGE_LENGTH) {
            throw new IllegalArgumentException(name + " must be in section-local voxel range");
        }
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
