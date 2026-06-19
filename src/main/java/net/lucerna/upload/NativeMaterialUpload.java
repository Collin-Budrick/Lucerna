package net.lucerna.upload;

import net.lucerna.material.LucernaMaterial;

import java.util.Objects;

public record NativeMaterialUpload(
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
    public NativeMaterialUpload {
        Objects.requireNonNull(blockId, "blockId");
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public static NativeMaterialUpload from(LucernaMaterial material) {
        Objects.requireNonNull(material, "material");
        return new NativeMaterialUpload(
                material.materialId(),
                material.generation(),
                material.blockId(),
                material.faceId(),
                material.albedoTextureIndex(),
                material.roughness(),
                material.metalness(),
                material.emissiveRed(),
                material.emissiveGreen(),
                material.emissiveBlue(),
                material.emissiveStrength(),
                material.flags()
        );
    }
}
