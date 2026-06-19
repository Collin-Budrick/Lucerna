package net.lucerna.material.extract;

import net.lucerna.material.LucernaMaterial;

import java.util.Objects;

public record MaterialUploadMetadata(
        int materialId,
        long generation,
        String blockId,
        String blockStateId,
        String fluidId,
        int faceId,
        int albedoTextureIndex,
        String albedoTextureId,
        String albedoAtlasId,
        float roughness,
        float metalness,
        float emissiveRed,
        float emissiveGreen,
        float emissiveBlue,
        float emissiveStrength,
        int flags,
        int minecraftBlockStateId,
        int minecraftModelFlags,
        int lightEmission,
        float opacity,
        boolean modelBacked,
        boolean textureTransparent,
        boolean textureTranslucent,
        boolean textureAnimated
) {
    public MaterialUploadMetadata {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockStateId, "blockStateId");
        fluidId = MaterialTextureIndex.normalize(fluidId);
        albedoTextureId = MaterialTextureIndex.normalize(albedoTextureId);
        albedoAtlasId = MaterialTextureIndex.normalize(albedoAtlasId);
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        if (minecraftBlockStateId < 0) {
            throw new IllegalArgumentException("minecraftBlockStateId must be non-negative");
        }
        if (lightEmission < 0) {
            throw new IllegalArgumentException("lightEmission must be non-negative");
        }
        if (opacity < 0.0f || opacity > 1.0f) {
            throw new IllegalArgumentException("opacity must be in [0, 1]");
        }
    }

    public static MaterialUploadMetadata from(LucernaMaterial material, ExtractedMaterial extracted) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(extracted, "extracted");

        MaterialTextureMetadata texture = extracted.albedoTexture();
        return new MaterialUploadMetadata(
                material.materialId(),
                material.generation(),
                material.blockId(),
                extracted.blockStateId(),
                extracted.fluidId(),
                material.faceId(),
                material.albedoTextureIndex(),
                texture.textureId(),
                texture.atlasId(),
                material.roughness(),
                material.metalness(),
                material.emissiveRed(),
                material.emissiveGreen(),
                material.emissiveBlue(),
                material.emissiveStrength(),
                material.flags(),
                extracted.minecraftBlockStateId(),
                extracted.minecraftModelFlags(),
                extracted.lightEmission(),
                extracted.opacity(),
                extracted.modelBacked(),
                texture.transparent(),
                texture.translucent(),
                texture.animated()
        );
    }
}
