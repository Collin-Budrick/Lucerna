package net.lucerna.render.gbuffer;

import java.util.Objects;

public record GBufferAttachmentWriteIntent(
        GBufferWriteSemantic semantic,
        String attachmentName,
        String format,
        boolean required,
        boolean clearBeforeWrite,
        boolean historySensitive,
        String producer
) {
    public static final String FIRST_PASS_PRODUCER = "lucerna.voxel.first_pass";

    public GBufferAttachmentWriteIntent {
        Objects.requireNonNull(semantic, "semantic");
        attachmentName = requireText(attachmentName, "attachmentName");
        format = requireText(format, "format");
        producer = requireText(producer, "producer");
        if (!semantic.attachmentName().equals(attachmentName)) {
            throw new IllegalArgumentException("attachmentName must match the write semantic attachment");
        }
    }

    public static GBufferAttachmentWriteIntent fromContract(
            GBufferWriteSemantic semantic,
            GBufferAttachmentContract attachment
    ) {
        Objects.requireNonNull(semantic, "semantic");
        Objects.requireNonNull(attachment, "attachment");
        if (!semantic.attachmentName().equals(attachment.name())) {
            throw new IllegalArgumentException("attachment contract does not match the requested semantic");
        }
        return new GBufferAttachmentWriteIntent(
                semantic,
                attachment.name(),
                attachment.format(),
                true,
                true,
                semantic == GBufferWriteSemantic.MOTION_HISTORY,
                FIRST_PASS_PRODUCER
        );
    }

    public boolean writesAttachment(String candidateAttachmentName) {
        return this.attachmentName.equals(candidateAttachmentName);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
