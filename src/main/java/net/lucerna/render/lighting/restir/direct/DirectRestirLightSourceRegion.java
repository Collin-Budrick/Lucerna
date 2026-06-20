package net.lucerna.render.lighting.restir.direct;

import java.util.Objects;

public record DirectRestirLightSourceRegion(
        String dimension,
        int minBlockX,
        int minBlockY,
        int minBlockZ,
        int maxBlockX,
        int maxBlockY,
        int maxBlockZ,
        long generation
) {
    public DirectRestirLightSourceRegion {
        dimension = requireText(dimension, "dimension");
        if (maxBlockX < minBlockX) {
            throw new IllegalArgumentException("maxBlockX must be greater than or equal to minBlockX");
        }
        if (maxBlockY < minBlockY) {
            throw new IllegalArgumentException("maxBlockY must be greater than or equal to minBlockY");
        }
        if (maxBlockZ < minBlockZ) {
            throw new IllegalArgumentException("maxBlockZ must be greater than or equal to minBlockZ");
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public static DirectRestirLightSourceRegion block(
            String dimension,
            int blockX,
            int blockY,
            int blockZ,
            long generation
    ) {
        return new DirectRestirLightSourceRegion(
                dimension,
                blockX,
                blockY,
                blockZ,
                blockX,
                blockY,
                blockZ,
                generation
        );
    }

    public int blockVolume() {
        long x = (long) this.maxBlockX - this.minBlockX + 1L;
        long y = (long) this.maxBlockY - this.minBlockY + 1L;
        long z = (long) this.maxBlockZ - this.minBlockZ + 1L;
        long volume = x * y * z;
        return volume > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) volume;
    }

    public String compactKey() {
        return this.dimension + ":" + this.minBlockX + "," + this.minBlockY + "," + this.minBlockZ
                + "->" + this.maxBlockX + "," + this.maxBlockY + "," + this.maxBlockZ
                + ":g" + this.generation;
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
