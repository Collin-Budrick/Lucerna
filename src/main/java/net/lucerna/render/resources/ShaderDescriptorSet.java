package net.lucerna.render.resources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ShaderDescriptorSet(
        int set,
        String name,
        String scope,
        List<ShaderDescriptorBinding> bindings
) {
    public ShaderDescriptorSet {
        if (set < 0) {
            throw new IllegalArgumentException("set must not be negative");
        }
        name = requireText(name, "name");
        scope = requireText(scope, "scope");
        Objects.requireNonNull(bindings, "bindings");
        bindings = List.copyOf(bindings);
        if (bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings must not be empty");
        }
        for (ShaderDescriptorBinding binding : bindings) {
            Objects.requireNonNull(binding, "bindings must not contain null entries");
        }
    }

    public Optional<ShaderDescriptorBinding> binding(int bindingIndex) {
        return this.bindings.stream()
                .filter(binding -> binding.binding() == bindingIndex)
                .findFirst();
    }

    public Optional<ShaderDescriptorBinding> binding(String bindingName) {
        Objects.requireNonNull(bindingName, "bindingName");
        return this.bindings.stream()
                .filter(binding -> binding.name().equals(bindingName))
                .findFirst();
    }

    public List<String> bindingNames() {
        return this.bindings.stream()
                .map(ShaderDescriptorBinding::name)
                .toList();
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
