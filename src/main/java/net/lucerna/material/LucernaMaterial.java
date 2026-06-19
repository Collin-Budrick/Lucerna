package net.lucerna.material;

import java.util.Objects;

public record LucernaMaterial(
        int materialId,
        long generation,
        String blockId,
        int faceId,
        int albedoTextureIndex,
        float roughness,
        float metalness,
        float emissiveRed,
        float emissiveGreen,
        float emissiveBlue,
        float emissiveStrength,
        int flags
) {
    public LucernaMaterial(
            int materialId,
            String blockId,
            int faceId,
            int albedoTextureIndex,
            float roughness,
            float metalness,
            float emissiveRed,
            float emissiveGreen,
            float emissiveBlue,
            float emissiveStrength,
            int flags
    ) {
        this(
                materialId,
                0,
                blockId,
                faceId,
                albedoTextureIndex,
                roughness,
                metalness,
                emissiveRed,
                emissiveGreen,
                emissiveBlue,
                emissiveStrength,
                flags
        );
    }

    public LucernaMaterial {
        Objects.requireNonNull(blockId, "blockId");
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public boolean emissive() {
        return (this.flags & MaterialFlags.EMISSIVE) != 0;
    }
}
