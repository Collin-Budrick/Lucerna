package net.lucerna.render.resources;

import java.util.List;
import java.util.Objects;

public record ShaderPassDescriptor(
        ShaderPassId id,
        int numericId,
        String stage,
        String directory,
        ShaderPassType type,
        int executionOrder,
        String placeholderShader,
        List<String> descriptorSets,
        List<String> reads,
        List<String> writes,
        String pushConstants,
        boolean sideEffectFreePlaceholder,
        String handoff
) {
    public ShaderPassDescriptor {
        Objects.requireNonNull(id, "id");
        if (numericId <= 0) {
            throw new IllegalArgumentException("numericId must be positive");
        }
        stage = requireText(stage, "stage");
        directory = requireText(directory, "directory");
        if (type == null) {
            type = ShaderPassType.UNKNOWN;
        }
        if (executionOrder < 0) {
            throw new IllegalArgumentException("executionOrder must not be negative");
        }
        placeholderShader = requireText(placeholderShader, "placeholderShader");
        descriptorSets = copyTextList(descriptorSets, "descriptorSets", true);
        reads = copyTextList(reads, "reads", false);
        writes = copyTextList(writes, "writes", false);
        pushConstants = requireText(pushConstants, "pushConstants");
        handoff = normalizeOptional(handoff);
    }

    public boolean compute() {
        return this.type == ShaderPassType.COMPUTE;
    }

    public boolean graphics() {
        return this.type == ShaderPassType.GRAPHICS;
    }

    public boolean usesDescriptorSet(String descriptorSetName) {
        Objects.requireNonNull(descriptorSetName, "descriptorSetName");
        return this.descriptorSets.contains(descriptorSetName);
    }

    public boolean readsResource(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        return this.reads.contains(resourceName);
    }

    public boolean writesResource(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");
        return this.writes.contains(resourceName);
    }

    public boolean hasHandoff() {
        return !this.handoff.isBlank();
    }

    private static List<String> copyTextList(List<String> values, String name, boolean requireNonEmpty) {
        Objects.requireNonNull(values, name);
        values = List.copyOf(values);
        if (requireNonEmpty && values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        for (String value : values) {
            requireText(value, name + " entries");
        }
        return values;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
