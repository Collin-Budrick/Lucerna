package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record RadianceCacheKey(
        String dimension,
        int cellX,
        int cellY,
        int cellZ,
        int cascade
) {
    public RadianceCacheKey {
        dimension = requireText(dimension, "dimension");
        if (cascade < 0) {
            throw new IllegalArgumentException("cascade must be non-negative");
        }
    }

    public static RadianceCacheKey fromSurface(SurfaceCacheKey surfaceKey, int cascade, int sectionCellSize) {
        Objects.requireNonNull(surfaceKey, "surfaceKey");
        int resolvedSectionCellSize = Math.max(1, sectionCellSize);
        return new RadianceCacheKey(
                surfaceKey.dimension(),
                floorDiv(surfaceKey.sectionX(), resolvedSectionCellSize),
                floorDiv(surfaceKey.sectionY(), resolvedSectionCellSize),
                floorDiv(surfaceKey.sectionZ(), resolvedSectionCellSize),
                cascade
        );
    }

    public String stableKey() {
        return this.dimension + ":" + this.cellX + "," + this.cellY + "," + this.cellZ + "#" + this.cascade;
    }

    private static int floorDiv(int value, int divisor) {
        return Math.floorDiv(value, divisor);
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
