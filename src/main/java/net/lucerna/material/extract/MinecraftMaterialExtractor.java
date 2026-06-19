package net.lucerna.material.extract;

import net.lucerna.material.MaterialFlags;
import net.lucerna.material.MaterialKey;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MinecraftMaterialExtractor {
    private static final float DEFAULT_ROUGHNESS = 0.9f;
    private static final float DEFAULT_METALNESS = 0.0f;
    private static final String NO_FLUID_ID = "";

    private final MaterialTextureIndex textureIndex;

    public MinecraftMaterialExtractor() {
        this(new MaterialTextureIndex());
    }

    public MinecraftMaterialExtractor(MaterialTextureIndex textureIndex) {
        this.textureIndex = Objects.requireNonNull(textureIndex, "textureIndex");
    }

    public ExtractedMaterial extract(BlockState state) {
        return this.extract(state, null);
    }

    public ExtractedMaterial extract(BlockState state, ModelManager modelManager) {
        Objects.requireNonNull(state, "state");

        ModelMetadata modelMetadata = this.readModelMetadata(state, modelManager);
        MaterialTextureMetadata texture = modelMetadata.albedoTexture();
        FluidState fluidState = state.getFluidState();
        Block block = state.getBlock();
        String blockId = idToString(BuiltInRegistries.BLOCK.getKey(block));
        String blockStateId = blockStateId(state, blockId);
        String fluidId = fluidState.isEmpty() ? NO_FLUID_ID : idToString(BuiltInRegistries.FLUID.getKey(fluidState.getType()));
        MaterialTraits traits = this.classify(state, block, blockId, fluidState, texture);
        EmissiveColor emissiveColor = emissiveColor(blockId, fluidId, traits.lightEmission());
        int flags = traits.flags();

        MaterialKey key = new MaterialKey(
                blockId,
                MaterialFace.WHOLE_BLOCK.id(),
                texture.textureIndex(),
                roughnessFor(flags),
                DEFAULT_METALNESS,
                emissiveColor.red(),
                emissiveColor.green(),
                emissiveColor.blue(),
                emissiveStrength(traits.lightEmission(), flags),
                flags
        );

        return new ExtractedMaterial(
                key,
                blockId,
                blockStateId,
                fluidId,
                texture,
                Block.getId(state),
                modelMetadata.minecraftModelFlags(),
                flags,
                traits.lightEmission(),
                traits.opacity(),
                modelMetadata.modelBacked()
        );
    }

    public MaterialTextureIndex textureIndex() {
        return this.textureIndex;
    }

    private ModelMetadata readModelMetadata(BlockState state, ModelManager modelManager) {
        if (modelManager == null || state.getRenderShape() != RenderShape.MODEL) {
            return ModelMetadata.missing();
        }

        var model = modelManager.getBlockStateModelSet().get(state);
        if (model == null) {
            return ModelMetadata.missing();
        }

        Material.Baked particleMaterial = model.particleMaterial();
        MaterialTextureMetadata texture = MaterialTextureMetadata.from(particleMaterial, this.textureIndex);
        return new ModelMetadata(texture, model.materialFlags(), true);
    }

    private MaterialTraits classify(
            BlockState state,
            Block block,
            String blockId,
            FluidState fluidState,
            MaterialTextureMetadata texture
    ) {
        boolean fluid = !fluidState.isEmpty() || state.liquid();
        boolean water = !fluidState.isEmpty() && fluidState.is(FluidTags.WATER);
        boolean lava = !fluidState.isEmpty() && fluidState.is(FluidTags.LAVA);
        boolean leaves = block instanceof LeavesBlock || state.is(BlockTags.LEAVES);
        boolean glass = block instanceof TransparentBlock || blockId.contains("glass");
        boolean textureCutout = texture.transparent() && !texture.translucent();
        boolean cutout = textureCutout || leaves || looksLikeCutout(blockId);
        boolean translucent = water || glass || texture.translucent() || looksLikeTranslucent(blockId);
        int lightEmission = Math.max(0, state.getLightEmission());
        boolean emissive = lightEmission > 0 || lava;
        boolean opaque = !state.isAir()
                && !fluid
                && !cutout
                && !translucent
                && state.canOcclude()
                && state.isSolidRender();

        int flags = 0;
        if (opaque) {
            flags |= MaterialFlags.OPAQUE;
        }
        if (emissive) {
            flags |= MaterialFlags.EMISSIVE;
        }
        if (water) {
            flags |= MaterialFlags.WATER;
        }
        if (glass) {
            flags |= MaterialFlags.GLASS;
        }
        if (leaves) {
            flags |= MaterialFlags.LEAVES;
        }
        if (translucent) {
            flags |= MaterialFlags.TRANSLUCENT;
        }
        if (fluid) {
            flags |= MaterialFlags.FLUID;
        }
        if (cutout) {
            flags |= MaterialFlags.CUTOUT;
        }

        return new MaterialTraits(flags, lightEmission, opacityFor(flags, state.isAir()));
    }

    private static boolean looksLikeCutout(String blockId) {
        String path = path(blockId);
        return path.contains("sapling")
                || path.contains("flower")
                || path.contains("grass")
                || path.contains("fern")
                || path.contains("bush")
                || path.contains("mushroom")
                || path.contains("vine")
                || path.contains("roots")
                || path.contains("rail")
                || path.contains("bars")
                || path.contains("chain")
                || path.contains("pane")
                || path.contains("door")
                || path.contains("trapdoor")
                || path.contains("sign")
                || path.contains("lantern")
                || path.contains("torch");
    }

    private static boolean looksLikeTranslucent(String blockId) {
        String path = path(blockId);
        return path.contains("ice")
                || path.contains("slime")
                || path.contains("honey")
                || path.contains("portal");
    }

    private static float roughnessFor(int flags) {
        if (MaterialFlags.has(flags, MaterialFlags.WATER) || MaterialFlags.has(flags, MaterialFlags.GLASS)) {
            return 0.05f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.TRANSLUCENT)) {
            return 0.2f;
        }
        return DEFAULT_ROUGHNESS;
    }

    private static float opacityFor(int flags, boolean air) {
        if (air) {
            return 0.0f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.OPAQUE)) {
            return 1.0f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.WATER)) {
            return 0.55f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.GLASS)) {
            return 0.35f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.CUTOUT)) {
            return 0.8f;
        }
        if (MaterialFlags.has(flags, MaterialFlags.TRANSLUCENT)) {
            return 0.5f;
        }
        return 0.75f;
    }

    private static float emissiveStrength(int lightEmission, int flags) {
        if (!MaterialFlags.has(flags, MaterialFlags.EMISSIVE)) {
            return 0.0f;
        }
        return Math.max(1.0f / 15.0f, Math.min(1.0f, lightEmission / 15.0f));
    }

    private static EmissiveColor emissiveColor(String blockId, String fluidId, int lightEmission) {
        if (lightEmission <= 0 && !fluidId.contains("lava")) {
            return EmissiveColor.NONE;
        }

        String id = blockId + "|" + fluidId;
        if (id.contains("soul")) {
            return new EmissiveColor(0.35f, 0.65f, 1.0f);
        }
        if (id.contains("redstone")) {
            return new EmissiveColor(1.0f, 0.1f, 0.05f);
        }
        if (id.contains("sculk")) {
            return new EmissiveColor(0.0f, 0.55f, 0.85f);
        }
        return new EmissiveColor(1.0f, 0.65f, 0.25f);
    }

    private static String blockStateId(BlockState state, String blockId) {
        String properties = state.getProperties().stream()
                .sorted(Comparator.comparing(Property::getName))
                .map(property -> property.getName() + "=" + propertyValueName(state, property))
                .collect(Collectors.joining(","));
        if (properties.isEmpty()) {
            return blockId;
        }
        return blockId + "[" + properties + "]";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(BlockState state, Property<?> property) {
        Property typedProperty = property;
        Comparable value = state.getValue(typedProperty);
        return typedProperty.getName(value);
    }

    private static String idToString(Identifier identifier) {
        return identifier == null ? "" : identifier.toString();
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private record ModelMetadata(
            MaterialTextureMetadata albedoTexture,
            int minecraftModelFlags,
            boolean modelBacked
    ) {
        static ModelMetadata missing() {
            return new ModelMetadata(MaterialTextureMetadata.missing(), 0, false);
        }
    }

    private record MaterialTraits(
            int flags,
            int lightEmission,
            float opacity
    ) {
    }

    private record EmissiveColor(
            float red,
            float green,
            float blue
    ) {
        static final EmissiveColor NONE = new EmissiveColor(0.0f, 0.0f, 0.0f);
    }
}
