package net.lucerna.material;

import java.util.Objects;

public record MaterialKey(
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
    public MaterialKey {
        Objects.requireNonNull(blockId, "blockId");
    }

    public String cacheKey() {
        return this.blockId
                + "|" + this.faceId
                + "|" + this.albedoTextureIndex
                + "|" + this.roughness
                + "|" + this.metalness
                + "|" + this.emissiveRed
                + "|" + this.emissiveGreen
                + "|" + this.emissiveBlue
                + "|" + this.emissiveStrength
                + "|" + this.flags;
    }
}
