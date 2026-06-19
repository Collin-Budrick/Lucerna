package net.lucerna.render.resources;

import java.util.Objects;

public record ShaderAttachmentWriteSemantic(
        String semantic,
        String attachmentName,
        ShaderPassId producerPass,
        boolean required,
        boolean clearBeforeWrite,
        boolean historySensitive,
        String notes
) {
    public ShaderAttachmentWriteSemantic {
        semantic = requireText(semantic, "semantic");
        attachmentName = requireText(attachmentName, "attachmentName");
        Objects.requireNonNull(producerPass, "producerPass");
        notes = normalizeOptional(notes);
    }

    public boolean producedBy(ShaderPassId passId) {
        Objects.requireNonNull(passId, "passId");
        return this.producerPass.equals(passId);
    }

    public boolean hasNotes() {
        return !this.notes.isBlank();
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
