package net.lucerna.render.gbuffer;

import java.util.Objects;

public record GBufferSceneDataAttachment(
        GBufferSceneDataKind kind,
        int width,
        int height,
        String format,
        String layout,
        long imageHandle,
        long imageViewHandle,
        boolean sampled,
        boolean sceneDerived,
        GBufferSceneDataSource source,
        String sourceDescription
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public GBufferSceneDataAttachment {
        Objects.requireNonNull(kind, "kind");
        width = Math.max(0, width);
        height = Math.max(0, height);
        format = normalizeLabel(format);
        layout = normalizeLabel(layout);
        if (source == null) {
            source = GBufferSceneDataSource.contractOnly(sourceDescription);
        }
        if (sourceDescription == null || sourceDescription.isBlank()) {
            sourceDescription = "No scene-derived frame-data attachment is bound.";
        } else {
            sourceDescription = sourceDescription.trim();
        }
    }

    public static GBufferSceneDataAttachment unavailable(
            GBufferSceneDataKind kind,
            String reason
    ) {
        return new GBufferSceneDataAttachment(
                kind,
                0,
                0,
                UNKNOWN_LABEL,
                UNKNOWN_LABEL,
                0L,
                0L,
                false,
                false,
                GBufferSceneDataSource.contractOnly(reason),
                reason
        );
    }

    public static GBufferSceneDataAttachment nativeReadable(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            boolean sampled,
            boolean sceneDerived,
            String sourceDescription
    ) {
        return nativeReadable(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                sampled,
                sceneDerived,
                GBufferSceneDataSource.contractOnly(sourceDescription),
                sourceDescription
        );
    }

    public static GBufferSceneDataAttachment nativeReadable(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            boolean sampled,
            boolean sceneDerived,
            GBufferSceneDataSource source,
            String sourceDescription
    ) {
        return new GBufferSceneDataAttachment(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                sampled,
                sceneDerived,
                source,
                sourceDescription
        );
    }

    public static GBufferSceneDataAttachment mojangSampled(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            String sourceDescription
    ) {
        return nativeReadable(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                true,
                true,
                GBufferSceneDataSource.mojangSampled(sourceDescription),
                sourceDescription
        );
    }

    public static GBufferSceneDataAttachment sodiumSampled(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            String sourceDescription
    ) {
        return nativeReadable(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                true,
                true,
                GBufferSceneDataSource.sodiumSampled(sourceDescription),
                sourceDescription
        );
    }

    public static GBufferSceneDataAttachment synthetic(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            String producer,
            String sourceDescription
    ) {
        return nativeReadable(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                true,
                true,
                GBufferSceneDataSource.synthetic(producer, sourceDescription),
                sourceDescription
        );
    }

    public String attachmentName() {
        return this.kind.attachmentName();
    }

    public boolean hasExtent() {
        return this.width > 0 && this.height > 0;
    }

    public boolean hasNativeView() {
        return this.imageHandle != 0L && this.imageViewHandle != 0L;
    }

    public boolean actualMinecraftSampled() {
        return this.source.actualMinecraftSampled();
    }

    public boolean mojangSampled() {
        return this.source.mojangSampled();
    }

    public boolean sodiumSampled() {
        return this.source.sodiumSampled();
    }

    public boolean syntheticSource() {
        return this.source.synthetic();
    }

    public boolean contractOnlySource() {
        return this.source.contractOnly();
    }

    public boolean readyForLighting() {
        return this.hasExtent()
                && this.hasNativeView()
                && this.sampled
                && this.sceneDerived
                && actualMinecraftSampled();
    }

    public String statusLabel() {
        return "kind=" + this.kind.label()
                + ", attachment=" + this.attachmentName()
                + ", extent=" + this.width + "x" + this.height
                + ", format=" + this.format
                + ", layout=" + this.layout
                + ", nativeView=" + hasNativeView()
                + ", sampled=" + this.sampled
                + ", sceneDerived=" + this.sceneDerived
                + ", source={" + this.source.statusLabel() + "}";
    }

    private static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_LABEL;
        }
        return value.trim();
    }
}
