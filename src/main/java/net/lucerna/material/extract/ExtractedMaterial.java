package net.lucerna.material.extract;

import net.lucerna.material.LucernaMaterial;
import net.lucerna.material.MaterialKey;
import net.lucerna.material.MaterialRegistry;

import java.util.Objects;

public record ExtractedMaterial(
        MaterialKey key,
        String blockId,
        String blockStateId,
        String fluidId,
        MaterialTextureMetadata albedoTexture,
        int minecraftBlockStateId,
        int minecraftModelFlags,
        int lucernaFlags,
        int lightEmission,
        float opacity,
        boolean modelBacked
) {
    public ExtractedMaterial {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockStateId, "blockStateId");
        fluidId = MaterialTextureIndex.normalize(fluidId);
        Objects.requireNonNull(albedoTexture, "albedoTexture");
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

    public LucernaMaterial register(MaterialRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return registry.getOrCreate(this.key);
    }

    public MaterialUploadMetadata toUploadMetadata(LucernaMaterial material) {
        return MaterialUploadMetadata.from(material, this);
    }
}
