package net.lucerna.render.pass;

public record LucernaFrameAttachmentMetadata(
        LucernaFramePassPhase phase,
        int width,
        int height,
        String colorFormat,
        String colorLayout,
        String depthFormat,
        String depthLayout,
        long commandBufferHandle,
        long colorImageHandle,
        long colorImageViewHandle,
        long depthImageHandle,
        long depthImageViewHandle,
        LucernaJavaOpaqueRenderObjects javaOpaqueObjects,
        boolean nativeWritable,
        String description
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public LucernaFrameAttachmentMetadata {
        if (phase == null) {
            phase = LucernaFramePassPhase.UNKNOWN;
        }
        width = Math.max(0, width);
        height = Math.max(0, height);
        colorFormat = normalizeLabel(colorFormat);
        colorLayout = normalizeLabel(colorLayout);
        depthFormat = normalizeLabel(depthFormat);
        depthLayout = normalizeLabel(depthLayout);
        commandBufferHandle = Math.max(0L, commandBufferHandle);
        colorImageHandle = Math.max(0L, colorImageHandle);
        colorImageViewHandle = Math.max(0L, colorImageViewHandle);
        depthImageHandle = Math.max(0L, depthImageHandle);
        depthImageViewHandle = Math.max(0L, depthImageViewHandle);
        if (javaOpaqueObjects == null) {
            javaOpaqueObjects = LucernaJavaOpaqueRenderObjects.none();
        }
        nativeWritable = nativeWritable
                && phase.safeForLightingComposite()
                && width > 0
                && height > 0
                && commandBufferHandle != 0L
                && colorImageHandle != 0L
                && colorImageViewHandle != 0L;
        if (description == null || description.isBlank()) {
            description = nativeWritable
                    ? "Native-writable frame attachment metadata."
                    : javaOpaqueObjects.present()
                    ? "Java opaque frame attachment metadata without native handles."
                    : "Metadata-only frame attachment target.";
        } else {
            description = description.trim();
        }
    }

    public static LucernaFrameAttachmentMetadata metadataOnly(
            LucernaFramePassPhase phase,
            String description
    ) {
        return new LucernaFrameAttachmentMetadata(
                phase,
                0,
                0,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                0L,
                0L,
                0L,
                0L,
                0L,
                LucernaJavaOpaqueRenderObjects.none(),
                false,
                description
        );
    }

    public static LucernaFrameAttachmentMetadata metadataOnly(
            LucernaFramePassPhase phase,
            int width,
            int height,
            String colorFormat,
            String colorLayout,
            String depthFormat,
            String depthLayout,
            String description
    ) {
        return new LucernaFrameAttachmentMetadata(
                phase,
                width,
                height,
                colorFormat,
                colorLayout,
                depthFormat,
                depthLayout,
                0L,
                0L,
                0L,
                0L,
                0L,
                LucernaJavaOpaqueRenderObjects.none(),
                false,
                description
        );
    }

    public static LucernaFrameAttachmentMetadata javaOpaque(
            LucernaFramePassPhase phase,
            LucernaJavaOpaqueRenderObjects javaOpaqueObjects,
            String description
    ) {
        return javaOpaque(
                phase,
                0,
                0,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                javaOpaqueObjects,
                description
        );
    }

    public static LucernaFrameAttachmentMetadata javaOpaque(
            LucernaFramePassPhase phase,
            int width,
            int height,
            String colorFormat,
            String colorLayout,
            String depthFormat,
            String depthLayout,
            LucernaJavaOpaqueRenderObjects javaOpaqueObjects,
            String description
    ) {
        return new LucernaFrameAttachmentMetadata(
                phase,
                width,
                height,
                colorFormat,
                colorLayout,
                depthFormat,
                depthLayout,
                0L,
                0L,
                0L,
                0L,
                0L,
                javaOpaqueObjects,
                false,
                description
        );
    }

    public static LucernaFrameAttachmentMetadata nativeWritable(
            LucernaFramePassPhase phase,
            int width,
            int height,
            String colorFormat,
            String colorLayout,
            String depthFormat,
            String depthLayout,
            long commandBufferHandle,
            long colorImageHandle,
            long colorImageViewHandle,
            long depthImageHandle,
            long depthImageViewHandle,
            String description
    ) {
        return new LucernaFrameAttachmentMetadata(
                phase,
                width,
                height,
                colorFormat,
                colorLayout,
                depthFormat,
                depthLayout,
                commandBufferHandle,
                colorImageHandle,
                colorImageViewHandle,
                depthImageHandle,
                depthImageViewHandle,
                LucernaJavaOpaqueRenderObjects.none(),
                true,
                description
        );
    }

    public boolean metadataOnly() {
        return !this.nativeWritable;
    }

    public boolean javaOpaque() {
        return this.javaOpaqueObjects.present();
    }

    public boolean hasExtent() {
        return this.width > 0 && this.height > 0;
    }

    public boolean hasColorHandles() {
        return this.colorImageHandle != 0L && this.colorImageViewHandle != 0L;
    }

    public boolean hasCommandHandle() {
        return this.commandBufferHandle != 0L;
    }

    public String nativeWritableStatusLabel() {
        return "nativeWritable=" + this.nativeWritable
                + ", javaOpaque=" + javaOpaque()
                + ", hasExtent=" + hasExtent()
                + ", hasCommandHandle=" + hasCommandHandle()
                + ", hasColorHandles=" + hasColorHandles()
                + ", safeForLightingComposite=" + this.phase.safeForLightingComposite();
    }

    public String attachmentStatusLabel() {
        return nativeWritableStatusLabel()
                + ", metadataOnly=" + metadataOnly()
                + ", " + this.javaOpaqueObjects.statusLabel();
    }

    private static String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return UNKNOWN_LABEL;
        }
        return label.trim();
    }
}
