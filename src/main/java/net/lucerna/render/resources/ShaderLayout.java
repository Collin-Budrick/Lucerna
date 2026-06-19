package net.lucerna.render.resources;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ShaderLayout(
        int schemaVersion,
        String layoutVersion,
        String namespace,
        List<ShaderDescriptorSet> descriptorSets,
        List<ShaderAttachment> attachments,
        List<ShaderPassDescriptor> passes
) {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;
    public static final String LUCERNA_NAMESPACE = "lucerna";

    public ShaderLayout {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
        layoutVersion = requireText(layoutVersion, "layoutVersion");
        namespace = requireText(namespace, "namespace");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        Objects.requireNonNull(attachments, "attachments");
        Objects.requireNonNull(passes, "passes");
        descriptorSets = List.copyOf(descriptorSets);
        attachments = List.copyOf(attachments);
        passes = List.copyOf(passes);
        for (ShaderDescriptorSet descriptorSet : descriptorSets) {
            Objects.requireNonNull(descriptorSet, "descriptorSets must not contain null entries");
        }
        for (ShaderAttachment attachment : attachments) {
            Objects.requireNonNull(attachment, "attachments must not contain null entries");
        }
        for (ShaderPassDescriptor pass : passes) {
            Objects.requireNonNull(pass, "passes must not contain null entries");
        }
    }

    public ShaderLayoutValidationReport validate() {
        return ShaderLayoutValidator.validate(this);
    }

    public Optional<ShaderPassDescriptor> pass(ShaderPassId passId) {
        Objects.requireNonNull(passId, "passId");
        return this.passes.stream()
                .filter(pass -> pass.id().equals(passId))
                .findFirst();
    }

    public Optional<ShaderDescriptorSet> descriptorSet(String name) {
        Objects.requireNonNull(name, "name");
        return this.descriptorSets.stream()
                .filter(descriptorSet -> descriptorSet.name().equals(name))
                .findFirst();
    }

    public Optional<ShaderAttachment> attachment(String name) {
        Objects.requireNonNull(name, "name");
        return this.attachments.stream()
                .filter(attachment -> attachment.name().equals(name))
                .findFirst();
    }

    public List<String> passIds() {
        return this.passes.stream()
                .map(pass -> pass.id().value())
                .toList();
    }

    public Set<String> descriptorBindingNames() {
        Set<String> names = new LinkedHashSet<>();
        for (ShaderDescriptorSet descriptorSet : this.descriptorSets) {
            names.addAll(descriptorSet.bindingNames());
        }
        return Set.copyOf(names);
    }

    public Set<String> resourceNames() {
        Set<String> names = new LinkedHashSet<>(this.descriptorBindingNames());
        for (ShaderAttachment attachment : this.attachments) {
            names.add(attachment.name());
        }
        return Set.copyOf(names);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
