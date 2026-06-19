package net.lucerna.upload;

import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.gbuffer.GBufferAttachmentContract;
import net.lucerna.render.gbuffer.GBufferTargetContract;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record NativeGBufferStagingUpload(
        long generation,
        String passId,
        int numericPassId,
        int width,
        int height,
        String[] attachmentNames,
        String[] attachmentFormats,
        String[] attachmentResolutions,
        int[] attachmentSamples,
        int[] attachmentEnabled
) {
    public NativeGBufferStagingUpload {
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        requireText(passId, "passId");
        if (numericPassId <= 0) {
            throw new IllegalArgumentException("numericPassId must be positive");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("G-buffer dimensions must be non-negative");
        }

        attachmentNames = copy(attachmentNames, "attachmentNames");
        attachmentFormats = copy(attachmentFormats, "attachmentFormats");
        attachmentResolutions = copy(attachmentResolutions, "attachmentResolutions");
        attachmentSamples = copy(attachmentSamples, "attachmentSamples");
        attachmentEnabled = copy(attachmentEnabled, "attachmentEnabled");

        requireMatchingLength(attachmentNames.length, "attachmentFormats", attachmentFormats.length);
        requireMatchingLength(attachmentNames.length, "attachmentResolutions", attachmentResolutions.length);
        requireMatchingLength(attachmentNames.length, "attachmentSamples", attachmentSamples.length);
        requireMatchingLength(attachmentNames.length, "attachmentEnabled", attachmentEnabled.length);

        for (int index = 0; index < attachmentNames.length; index++) {
            requireText(attachmentNames[index], "attachmentNames entries");
            requireText(attachmentFormats[index], "attachmentFormats entries");
            requireText(attachmentResolutions[index], "attachmentResolutions entries");
            if (attachmentSamples[index] <= 0) {
                throw new IllegalArgumentException("attachmentSamples entries must be positive");
            }
            int enabled = attachmentEnabled[index];
            if (enabled != 0 && enabled != 1) {
                throw new IllegalArgumentException("attachmentEnabled entries must be 0 or 1");
            }
        }
    }

    public static NativeGBufferStagingUpload empty(long generation) {
        return new NativeGBufferStagingUpload(
                generation,
                GBufferTargetContract.MAIN_PASS_ID,
                GBufferTargetContract.MAIN_NUMERIC_PASS_ID,
                0,
                0,
                new String[0],
                new String[0],
                new String[0],
                new int[0],
                new int[0]
        );
    }

    public static NativeGBufferStagingUpload lucernaMain(int width, int height, long generation) {
        return from(GBufferDescriptor.lucernaMain(width, height), generation);
    }

    public static NativeGBufferStagingUpload from(GBufferDescriptor descriptor, long generation) {
        Objects.requireNonNull(descriptor, "descriptor");
        GBufferTargetContract targetContract = descriptor.targetContract();
        List<GBufferAttachmentContract> attachments = targetContract.attachments();
        String[] attachmentNames = new String[attachments.size()];
        String[] attachmentFormats = new String[attachments.size()];
        String[] attachmentResolutions = new String[attachments.size()];
        int[] attachmentSamples = new int[attachments.size()];
        int[] attachmentEnabled = new int[attachments.size()];

        for (int index = 0; index < attachments.size(); index++) {
            GBufferAttachmentContract attachment = attachments.get(index);
            attachmentNames[index] = attachment.name();
            attachmentFormats[index] = attachment.format();
            attachmentResolutions[index] = attachment.resolution();
            attachmentSamples[index] = attachment.samples();
            attachmentEnabled[index] = attachmentEnabled(descriptor, attachment.name());
        }

        return new NativeGBufferStagingUpload(
                generation,
                targetContract.passId(),
                targetContract.numericPassId(),
                descriptor.width(),
                descriptor.height(),
                attachmentNames,
                attachmentFormats,
                attachmentResolutions,
                attachmentSamples,
                attachmentEnabled
        );
    }

    public int attachmentCount() {
        return this.attachmentNames.length;
    }

    public boolean hasEnabledAttachments() {
        for (int enabled : this.attachmentEnabled) {
            if (enabled == 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] attachmentNames() {
        return copy(this.attachmentNames, "attachmentNames");
    }

    @Override
    public String[] attachmentFormats() {
        return copy(this.attachmentFormats, "attachmentFormats");
    }

    @Override
    public String[] attachmentResolutions() {
        return copy(this.attachmentResolutions, "attachmentResolutions");
    }

    @Override
    public int[] attachmentSamples() {
        return copy(this.attachmentSamples, "attachmentSamples");
    }

    @Override
    public int[] attachmentEnabled() {
        return copy(this.attachmentEnabled, "attachmentEnabled");
    }

    private static int attachmentEnabled(GBufferDescriptor descriptor, String attachmentName) {
        return switch (attachmentName) {
            case GBufferTargetContract.DEPTH -> descriptor.hasDepth() ? 1 : 0;
            case GBufferTargetContract.NORMAL_ROUGHNESS -> descriptor.hasNormals() ? 1 : 0;
            case GBufferTargetContract.ALBEDO_OPACITY -> descriptor.hasAlbedo() ? 1 : 0;
            case GBufferTargetContract.MATERIAL_ID -> descriptor.hasMaterialIds() ? 1 : 0;
            case GBufferTargetContract.EMISSIVE -> descriptor.hasEmissive() ? 1 : 0;
            case GBufferTargetContract.MOTION_HISTORY -> descriptor.hasMotionVectors() ? 1 : 0;
            default -> 0;
        };
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
