package net.lucerna.render.resources;

import java.util.List;
import java.util.Objects;

public record ShaderDescriptorBinding(
        int binding,
        String name,
        String descriptorType,
        List<String> stages,
        String access,
        String updateFrequency,
        String format,
        String notes
) {
    public ShaderDescriptorBinding {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        name = requireText(name, "name");
        descriptorType = requireText(descriptorType, "descriptorType");
        stages = copyTextList(stages, "stages", true);
        access = requireText(access, "access");
        updateFrequency = requireText(updateFrequency, "updateFrequency");
        format = normalizeOptional(format);
        notes = normalizeOptional(notes);
    }

    public boolean readable() {
        return this.access.contains("read");
    }

    public boolean writable() {
        return this.access.contains("write");
    }

    public boolean hasFormat() {
        return !this.format.isBlank();
    }

    public boolean hasNotes() {
        return !this.notes.isBlank();
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
