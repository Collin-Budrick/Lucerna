package net.lucerna.render.gbuffer;

import java.util.List;
import java.util.Objects;

public record GBufferAttachmentContract(
        String name,
        String ownerPass,
        String format,
        String resolution,
        int samples,
        List<String> usage,
        List<String> consumers
) {
    public GBufferAttachmentContract {
        requireText(name, "name");
        requireText(ownerPass, "ownerPass");
        requireText(format, "format");
        requireText(resolution, "resolution");
        if (samples <= 0) {
            throw new IllegalArgumentException("samples must be positive");
        }
        Objects.requireNonNull(usage, "usage");
        Objects.requireNonNull(consumers, "consumers");
        usage = List.copyOf(usage);
        consumers = List.copyOf(consumers);
        if (usage.isEmpty()) {
            throw new IllegalArgumentException("usage must not be empty");
        }
        for (String usageEntry : usage) {
            requireText(usageEntry, "usage entries");
        }
        for (String consumer : consumers) {
            requireText(consumer, "consumers entries");
        }
    }

    public boolean isNamed(String attachmentName) {
        return this.name.equals(attachmentName);
    }

    public boolean fullResolution() {
        return "full".equals(this.resolution);
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
