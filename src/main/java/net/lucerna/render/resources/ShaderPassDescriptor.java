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
        String handoff,
        List<ShaderPassId> dependsOn,
        List<ShaderAttachmentWriteSemantic> attachmentWriteSemantics
) {
    public ShaderPassDescriptor(
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
        this(
                id,
                numericId,
                stage,
                directory,
                type,
                executionOrder,
                placeholderShader,
                descriptorSets,
                reads,
                writes,
                pushConstants,
                sideEffectFreePlaceholder,
                handoff,
                List.of(),
                List.of()
        );
    }

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
        dependsOn = copyPassIdList(dependsOn, "dependsOn");
        attachmentWriteSemantics = copyWriteSemanticList(attachmentWriteSemantics, "attachmentWriteSemantics");
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

    public boolean dependsOn(ShaderPassId passId) {
        Objects.requireNonNull(passId, "passId");
        return this.dependsOn.contains(passId);
    }

    public boolean hasAttachmentWriteSemantics() {
        return !this.attachmentWriteSemantics.isEmpty();
    }

    public List<String> dependencyIds() {
        return this.dependsOn.stream()
                .map(ShaderPassId::value)
                .toList();
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

    private static List<ShaderPassId> copyPassIdList(List<ShaderPassId> values, String name) {
        if (values == null) {
            return List.of();
        }
        values = List.copyOf(values);
        for (ShaderPassId value : values) {
            Objects.requireNonNull(value, name + " entries");
        }
        return values;
    }

    private static List<ShaderAttachmentWriteSemantic> copyWriteSemanticList(
            List<ShaderAttachmentWriteSemantic> values,
            String name
    ) {
        if (values == null) {
            return List.of();
        }
        values = List.copyOf(values);
        for (ShaderAttachmentWriteSemantic value : values) {
            Objects.requireNonNull(value, name + " entries");
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
