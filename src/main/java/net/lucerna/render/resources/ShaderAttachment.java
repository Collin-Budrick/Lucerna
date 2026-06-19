package net.lucerna.render.resources;

import java.util.List;
import java.util.Objects;

public record ShaderAttachment(
        String name,
        ShaderPassId ownerPass,
        String format,
        String resolution,
        int samples,
        List<String> usage,
        List<String> consumers,
        List<String> fallbackFormats,
        String notes
) {
    public ShaderAttachment {
        name = requireText(name, "name");
        Objects.requireNonNull(ownerPass, "ownerPass");
        format = requireText(format, "format");
        resolution = requireText(resolution, "resolution");
        if (samples <= 0) {
            throw new IllegalArgumentException("samples must be positive");
        }
        usage = copyTextList(usage, "usage", true);
        consumers = copyTextList(consumers, "consumers", false);
        fallbackFormats = copyTextList(fallbackFormats, "fallbackFormats", false);
        notes = normalizeOptional(notes);
    }

    public boolean isOwnedBy(ShaderPassId passId) {
        return this.ownerPass.equals(passId);
    }

    public boolean hasConsumer(String passId) {
        Objects.requireNonNull(passId, "passId");
        return this.consumers.contains(passId);
    }

    public boolean fullResolution() {
        return "full".equals(this.resolution);
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
