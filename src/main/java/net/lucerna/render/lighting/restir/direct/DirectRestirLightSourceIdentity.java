package net.lucerna.render.lighting.restir.direct;

import java.util.Objects;

public record DirectRestirLightSourceIdentity(
        DirectRestirLightSourceKind kind,
        String stableKey,
        DirectRestirLightSourceRegion region,
        int materialId,
        long sourceGeneration,
        String label
) {
    public DirectRestirLightSourceIdentity {
        Objects.requireNonNull(kind, "kind");
        stableKey = requireText(stableKey, "stableKey");
        Objects.requireNonNull(region, "region");
        if (materialId < 0) {
            throw new IllegalArgumentException("materialId must be non-negative");
        }
        if (sourceGeneration < 0L) {
            throw new IllegalArgumentException("sourceGeneration must be non-negative");
        }
        label = cleanLabel(label, kind.name().toLowerCase() + ":" + stableKey);
    }

    public static DirectRestirLightSourceIdentity celestial(
            DirectRestirLightSourceKind kind,
            String dimension,
            long sourceGeneration
    ) {
        if (kind != DirectRestirLightSourceKind.SUN && kind != DirectRestirLightSourceKind.MOON) {
            throw new IllegalArgumentException("celestial source kind must be SUN or MOON");
        }
        dimension = requireText(dimension, "dimension");
        String key = dimension + ":" + kind.name().toLowerCase();
        return new DirectRestirLightSourceIdentity(
                kind,
                key,
                new DirectRestirLightSourceRegion(dimension, 0, 0, 0, 0, 0, 0, sourceGeneration),
                0,
                sourceGeneration,
                key
        );
    }

    public static DirectRestirLightSourceIdentity emissiveBlock(
            String dimension,
            int blockX,
            int blockY,
            int blockZ,
            int materialId,
            long sourceGeneration
    ) {
        dimension = requireText(dimension, "dimension");
        String key = dimension + ":" + blockX + "," + blockY + "," + blockZ + ":" + materialId;
        return new DirectRestirLightSourceIdentity(
                DirectRestirLightSourceKind.EMISSIVE_BLOCK,
                key,
                DirectRestirLightSourceRegion.block(dimension, blockX, blockY, blockZ, sourceGeneration),
                materialId,
                sourceGeneration,
                "emissive_block:" + key
        );
    }

    public boolean hasMaterialIdentity() {
        return this.materialId > 0;
    }

    public String compactLabel() {
        return this.kind + " key=" + this.stableKey + " material=" + this.materialId + " region=" + this.region.compactKey();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String cleanLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
