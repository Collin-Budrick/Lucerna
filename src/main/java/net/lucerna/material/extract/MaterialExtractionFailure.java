package net.lucerna.material.extract;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

public record MaterialExtractionFailure(
        String blockId,
        String blockStateId,
        int minecraftBlockStateId,
        String exceptionClass,
        String message
) {
    public MaterialExtractionFailure {
        blockId = MaterialTextureIndex.normalize(blockId);
        blockStateId = MaterialTextureIndex.normalize(blockStateId);
        exceptionClass = MaterialTextureIndex.normalize(exceptionClass);
        message = MaterialTextureIndex.normalize(message);
        if (minecraftBlockStateId < 0) {
            throw new IllegalArgumentException("minecraftBlockStateId must be non-negative");
        }
    }

    public static MaterialExtractionFailure from(BlockState state, RuntimeException exception) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(exception, "exception");

        String blockId = blockId(state);
        return new MaterialExtractionFailure(
                blockId,
                blockStateId(state, blockId),
                minecraftBlockStateId(state),
                exception.getClass().getName(),
                exception.getMessage()
        );
    }

    private static String blockId(BlockState state) {
        try {
            return idToString(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        } catch (RuntimeException ignored) {
            return MaterialTextureIndex.UNKNOWN_TEXTURE_ID;
        }
    }

    private static String blockStateId(BlockState state, String blockId) {
        try {
            String properties = state.getProperties().stream()
                    .sorted(Comparator.comparing(Property::getName))
                    .map(property -> property.getName() + "=" + propertyValueName(state, property))
                    .collect(Collectors.joining(","));
            if (properties.isEmpty()) {
                return blockId;
            }
            return blockId + "[" + properties + "]";
        } catch (RuntimeException ignored) {
            return state.toString();
        }
    }

    private static int minecraftBlockStateId(BlockState state) {
        try {
            return Block.getId(state);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(BlockState state, Property<?> property) {
        Property typedProperty = property;
        Comparable value = state.getValue(typedProperty);
        return typedProperty.getName(value);
    }

    private static String idToString(Identifier identifier) {
        return identifier == null ? MaterialTextureIndex.UNKNOWN_TEXTURE_ID : identifier.toString();
    }
}
