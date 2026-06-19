package net.lucerna.material.extract;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;

import java.util.Objects;

public record MaterialTextureMetadata(
        String textureId,
        String atlasId,
        int textureIndex,
        boolean transparent,
        boolean translucent,
        boolean animated
) {
    public MaterialTextureMetadata {
        textureId = MaterialTextureIndex.normalize(textureId);
        atlasId = MaterialTextureIndex.normalize(atlasId);
        if (textureIndex < MaterialTextureIndex.UNKNOWN_TEXTURE_INDEX) {
            throw new IllegalArgumentException("textureIndex must be non-negative or UNKNOWN_TEXTURE_INDEX");
        }
    }

    public static MaterialTextureMetadata missing() {
        return new MaterialTextureMetadata(
                MaterialTextureIndex.UNKNOWN_TEXTURE_ID,
                MaterialTextureIndex.UNKNOWN_TEXTURE_ID,
                MaterialTextureIndex.UNKNOWN_TEXTURE_INDEX,
                false,
                false,
                false
        );
    }

    public static MaterialTextureMetadata from(Material.Baked material, MaterialTextureIndex textureIndex) {
        Objects.requireNonNull(textureIndex, "textureIndex");
        if (material == null) {
            return missing();
        }

        TextureAtlasSprite sprite = material.sprite();
        if (sprite == null) {
            return missing();
        }

        String textureId = sprite.contents().name().toString();
        String atlasId = sprite.atlasLocation().toString();
        Transparency transparency = sprite.contents().transparency();
        return new MaterialTextureMetadata(
                textureId,
                atlasId,
                textureIndex.getOrCreate(textureId),
                transparency.hasTransparent(),
                material.forceTranslucent() || transparency.hasTranslucent(),
                sprite.isAnimated()
        );
    }

    public boolean known() {
        return this.textureIndex != MaterialTextureIndex.UNKNOWN_TEXTURE_INDEX;
    }
}
