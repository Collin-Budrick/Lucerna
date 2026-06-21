package net.lucerna.render.gbuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record GBufferWriteIntent(
        String passId,
        int numericPassId,
        int width,
        int height,
        long generation,
        List<GBufferAttachmentWriteIntent> attachments
) {
    public GBufferWriteIntent {
        passId = requireText(passId, "passId");
        if (numericPassId <= 0) {
            throw new IllegalArgumentException("numericPassId must be positive");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("G-buffer write dimensions must be non-negative");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        Objects.requireNonNull(attachments, "attachments");
        attachments = List.copyOf(attachments);

        Set<String> attachmentNames = new HashSet<>();
        for (GBufferAttachmentWriteIntent attachment : attachments) {
            Objects.requireNonNull(attachment, "attachments must not contain null entries");
            if (!attachmentNames.add(attachment.attachmentName())) {
                throw new IllegalArgumentException("attachment write intents must be unique by attachmentName");
            }
        }
    }

    public static GBufferWriteIntent empty(long generation) {
        return new GBufferWriteIntent(
                GBufferTargetContract.MAIN_PASS_ID,
                GBufferTargetContract.MAIN_NUMERIC_PASS_ID,
                0,
                0,
                generation,
                List.of()
        );
    }

    public static GBufferWriteIntent lucernaMain(int width, int height, long generation) {
        return fromTarget(GBufferTargetContract.lucernaMain(), width, height, generation);
    }

    public static GBufferWriteIntent fromTarget(
            GBufferTargetContract targetContract,
            int width,
            int height,
            long generation
    ) {
        Objects.requireNonNull(targetContract, "targetContract");
        List<GBufferAttachmentWriteIntent> attachmentIntents = new ArrayList<>();
        for (GBufferAttachmentContract attachment : targetContract.attachments()) {
            GBufferWriteSemantic.fromAttachmentName(attachment.name())
                    .map(semantic -> GBufferAttachmentWriteIntent.fromContract(semantic, attachment))
                    .ifPresent(attachmentIntents::add);
        }
        return new GBufferWriteIntent(
                targetContract.passId(),
                targetContract.numericPassId(),
                width,
                height,
                generation,
                attachmentIntents
        );
    }

    public Optional<GBufferAttachmentWriteIntent> attachmentIntent(String attachmentName) {
        Objects.requireNonNull(attachmentName, "attachmentName");
        return this.attachments.stream()
                .filter(attachment -> attachment.writesAttachment(attachmentName))
                .findFirst();
    }

    public boolean writesAttachment(String attachmentName) {
        return this.attachmentIntent(attachmentName).isPresent();
    }

    public boolean hasWritableAttachments() {
        return !this.attachments.isEmpty();
    }

    public boolean requiresHistory() {
        return this.attachments.stream().anyMatch(GBufferAttachmentWriteIntent::historySensitive);
    }

    public List<String> attachmentNames() {
        return this.attachments.stream()
                .map(GBufferAttachmentWriteIntent::attachmentName)
                .toList();
    }

    public boolean writesRequiredSceneDataAttachments() {
        return this.missingRequiredSceneDataAttachmentNames().isEmpty();
    }

    public List<GBufferSceneDataKind> missingRequiredSceneDataKinds() {
        List<GBufferSceneDataKind> missing = new ArrayList<>();
        for (GBufferSceneDataKind kind : GBufferSceneDataKind.lightingRequired()) {
            if (!this.writesAttachment(kind.attachmentName())) {
                missing.add(kind);
            }
        }
        return List.copyOf(missing);
    }

    public List<String> missingRequiredSceneDataAttachmentNames() {
        return this.missingRequiredSceneDataKinds().stream()
                .map(GBufferSceneDataKind::attachmentName)
                .toList();
    }

    public boolean dimensionsAvailable() {
        return this.width > 0 && this.height > 0;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
