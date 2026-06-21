package net.lucerna.render.gbuffer;

import java.util.Objects;

public record GBufferSceneDataReadiness(
        GBufferSceneDataKind kind,
        String attachmentName,
        boolean requiredForLighting,
        boolean targetDeclared,
        boolean writeIntentDeclared,
        boolean writeDimensionsAvailable,
        boolean frameTargetAvailable,
        boolean frameTargetSafe,
        boolean frameAttachmentMetadataOnly,
        boolean frameAttachmentJavaOpaque,
        boolean frameAttachmentNativeWritable,
        boolean frameDataBound,
        boolean frameDataSceneDerived,
        boolean frameDataNativeReadable,
        boolean frameDataSampled,
        boolean frameDataExtentAvailable,
        boolean frameDataExtentMatches,
        int expectedWidth,
        int expectedHeight,
        int frameDataWidth,
        int frameDataHeight,
        String expectedFormat,
        String frameDataFormat,
        GBufferSceneDataSource frameDataSource,
        boolean frameDataActualMinecraftSampled,
        boolean frameDataMojangSampled,
        boolean frameDataSodiumSampled,
        boolean frameDataSynthetic,
        boolean frameDataContractOnly,
        GBufferSceneDataStatusCode statusCode,
        String blocker
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public GBufferSceneDataReadiness {
        Objects.requireNonNull(kind, "kind");
        attachmentName = requireText(attachmentName, "attachmentName");
        expectedWidth = Math.max(0, expectedWidth);
        expectedHeight = Math.max(0, expectedHeight);
        frameDataWidth = Math.max(0, frameDataWidth);
        frameDataHeight = Math.max(0, frameDataHeight);
        expectedFormat = normalizeLabel(expectedFormat);
        frameDataFormat = normalizeLabel(frameDataFormat);
        if (frameDataSource == null) {
            frameDataSource = GBufferSceneDataSource.contractOnly(blocker);
        }
        frameDataActualMinecraftSampled = frameDataSource.actualMinecraftSampled() && frameDataActualMinecraftSampled;
        frameDataMojangSampled = frameDataSource.mojangSampled() && frameDataMojangSampled;
        frameDataSodiumSampled = frameDataSource.sodiumSampled() && frameDataSodiumSampled;
        frameDataSynthetic = frameDataSource.synthetic() || frameDataSynthetic;
        frameDataContractOnly = frameDataSource.contractOnly() || frameDataContractOnly;
        if (statusCode == null) {
            statusCode = GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
        }
        if (blocker == null || blocker.isBlank()) {
            blocker = statusCode.description();
        } else {
            blocker = blocker.trim();
        }
    }

    public static GBufferSceneDataReadiness from(
            GBufferSceneDataKind kind,
            GBufferTargetContract targetContract,
            GBufferWriteIntent writeIntent,
            boolean frameTargetAvailable,
            boolean frameTargetSafe,
            boolean frameAttachmentMetadataOnly,
            boolean frameAttachmentJavaOpaque,
            boolean frameAttachmentNativeWritable,
            GBufferSceneDataAttachment frameDataAttachment
    ) {
        Objects.requireNonNull(kind, "kind");
        GBufferTargetContract resolvedTarget = targetContract == null
                ? GBufferTargetContract.lucernaMain()
                : targetContract;
        GBufferWriteIntent resolvedWriteIntent = writeIntent == null
                ? GBufferWriteIntent.empty(0L)
                : writeIntent;
        var targetAttachment = resolvedTarget.attachment(kind.attachmentName());
        var writeAttachment = resolvedWriteIntent.attachmentIntent(kind.attachmentName());
        boolean targetDeclared = targetAttachment.isPresent();
        boolean writeDeclared = writeAttachment.isPresent();
        boolean dimensionsAvailable = resolvedWriteIntent.dimensionsAvailable();
        boolean frameDataBound = frameDataAttachment != null;
        boolean frameDataExtentAvailable = frameDataBound && frameDataAttachment.hasExtent();
        boolean frameDataExtentMatches = frameDataExtentAvailable
                && dimensionsAvailable
                && frameDataAttachment.width() == resolvedWriteIntent.width()
                && frameDataAttachment.height() == resolvedWriteIntent.height();
        boolean frameDataNativeReadable = frameDataBound && frameDataAttachment.hasNativeView();
        boolean frameDataSampled = frameDataBound && frameDataAttachment.sampled();
        boolean frameDataSceneDerived = frameDataBound && frameDataAttachment.sceneDerived();
        GBufferSceneDataSource frameDataSource = frameDataBound
                ? frameDataAttachment.source()
                : GBufferSceneDataSource.contractOnly("No scene-data attachment bound for " + kind.label() + ".");
        boolean frameDataActualMinecraftSampled = frameDataBound && frameDataAttachment.actualMinecraftSampled();
        boolean frameDataMojangSampled = frameDataBound && frameDataAttachment.mojangSampled();
        boolean frameDataSodiumSampled = frameDataBound && frameDataAttachment.sodiumSampled();
        boolean frameDataSynthetic = frameDataBound && frameDataAttachment.syntheticSource();
        boolean frameDataContractOnly = !frameDataBound || frameDataAttachment.contractOnlySource();
        GBufferSceneDataStatusCode statusCode = resolveStatus(
                targetDeclared,
                writeDeclared,
                dimensionsAvailable,
                frameTargetAvailable,
                frameTargetSafe,
                frameAttachmentMetadataOnly,
                frameAttachmentJavaOpaque,
                frameAttachmentNativeWritable,
                frameDataBound,
                frameDataSceneDerived,
                frameDataExtentAvailable,
                frameDataExtentMatches,
                frameDataNativeReadable,
                frameDataSampled,
                frameDataActualMinecraftSampled,
                frameDataSynthetic,
                frameDataContractOnly
        );
        String expectedFormat = writeAttachment
                .map(GBufferAttachmentWriteIntent::format)
                .or(() -> targetAttachment.map(GBufferAttachmentContract::format))
                .orElse(UNKNOWN_LABEL);
        String frameDataFormat = frameDataBound ? frameDataAttachment.format() : UNKNOWN_LABEL;

        return new GBufferSceneDataReadiness(
                kind,
                kind.attachmentName(),
                kind.requiredForLighting(),
                targetDeclared,
                writeDeclared,
                dimensionsAvailable,
                frameTargetAvailable,
                frameTargetSafe,
                frameAttachmentMetadataOnly,
                frameAttachmentJavaOpaque,
                frameAttachmentNativeWritable,
                frameDataBound,
                frameDataSceneDerived,
                frameDataNativeReadable,
                frameDataSampled,
                frameDataExtentAvailable,
                frameDataExtentMatches,
                resolvedWriteIntent.width(),
                resolvedWriteIntent.height(),
                frameDataBound ? frameDataAttachment.width() : 0,
                frameDataBound ? frameDataAttachment.height() : 0,
                expectedFormat,
                frameDataFormat,
                frameDataSource,
                frameDataActualMinecraftSampled,
                frameDataMojangSampled,
                frameDataSodiumSampled,
                frameDataSynthetic,
                frameDataContractOnly,
                statusCode,
                statusCode == GBufferSceneDataStatusCode.READY ? "ready" : statusCode.description()
        );
    }

    public boolean ready() {
        return this.statusCode == GBufferSceneDataStatusCode.READY;
    }

    public boolean trueSampledSceneDataReady() {
        return ready() && this.frameDataActualMinecraftSampled && !this.frameDataSynthetic && !this.frameDataContractOnly;
    }

    public String statusLabel() {
        return this.kind.label()
                + "{ready=" + ready()
                + ", code=" + this.statusCode
                + ", attachment=" + this.attachmentName
                + ", expectedExtent=" + this.expectedWidth + "x" + this.expectedHeight
                + ", frameDataExtent=" + this.frameDataWidth + "x" + this.frameDataHeight
                + ", expectedFormat=" + this.expectedFormat
                + ", frameDataFormat=" + this.frameDataFormat
                + ", sourceKind=" + this.frameDataSource.kind().label()
                + ", actualMinecraftSampled=" + this.frameDataActualMinecraftSampled
                + ", mojangSampled=" + this.frameDataMojangSampled
                + ", sodiumSampled=" + this.frameDataSodiumSampled
                + ", synthetic=" + this.frameDataSynthetic
                + ", contractOnly=" + this.frameDataContractOnly
                + ", blocker=" + this.blocker
                + "}";
    }

    private static GBufferSceneDataStatusCode resolveStatus(
            boolean targetDeclared,
            boolean writeIntentDeclared,
            boolean writeDimensionsAvailable,
            boolean frameTargetAvailable,
            boolean frameTargetSafe,
            boolean frameAttachmentMetadataOnly,
            boolean frameAttachmentJavaOpaque,
            boolean frameAttachmentNativeWritable,
            boolean frameDataBound,
            boolean frameDataSceneDerived,
            boolean frameDataExtentAvailable,
            boolean frameDataExtentMatches,
            boolean frameDataNativeReadable,
            boolean frameDataSampled,
            boolean frameDataActualMinecraftSampled,
            boolean frameDataSynthetic,
            boolean frameDataContractOnly
    ) {
        if (!targetDeclared) {
            return GBufferSceneDataStatusCode.TARGET_ATTACHMENT_MISSING;
        }
        if (!writeIntentDeclared) {
            return GBufferSceneDataStatusCode.WRITE_INTENT_MISSING;
        }
        if (!writeDimensionsAvailable) {
            return GBufferSceneDataStatusCode.WRITE_DIMENSIONS_MISSING;
        }
        if (!frameDataBound) {
            return GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
        }
        if (!frameDataSceneDerived) {
            return GBufferSceneDataStatusCode.FRAME_DATA_NOT_SCENE_DERIVED;
        }
        if (!frameDataExtentAvailable) {
            return GBufferSceneDataStatusCode.FRAME_DATA_EXTENT_MISSING;
        }
        if (!frameDataExtentMatches) {
            return GBufferSceneDataStatusCode.FRAME_DATA_EXTENT_MISMATCH;
        }
        if (!frameDataNativeReadable) {
            return GBufferSceneDataStatusCode.FRAME_DATA_NOT_NATIVE_READABLE;
        }
        if (!frameDataSampled) {
            return GBufferSceneDataStatusCode.FRAME_DATA_NOT_SAMPLED;
        }
        if (frameDataContractOnly) {
            return GBufferSceneDataStatusCode.FRAME_DATA_CONTRACT_ONLY;
        }
        if (frameDataSynthetic) {
            return GBufferSceneDataStatusCode.FRAME_DATA_SYNTHETIC;
        }
        if (!frameDataActualMinecraftSampled) {
            return GBufferSceneDataStatusCode.FRAME_DATA_NOT_ACTUAL_GAME_SAMPLED;
        }
        if (!frameTargetAvailable) {
            return GBufferSceneDataStatusCode.FRAME_TARGET_UNAVAILABLE;
        }
        if (!frameTargetSafe) {
            return GBufferSceneDataStatusCode.FRAME_TARGET_UNSAFE;
        }
        if (frameAttachmentMetadataOnly) {
            return GBufferSceneDataStatusCode.FRAME_TARGET_METADATA_ONLY;
        }
        if (frameAttachmentJavaOpaque) {
            return GBufferSceneDataStatusCode.FRAME_TARGET_JAVA_OPAQUE;
        }
        if (!frameAttachmentNativeWritable) {
            return GBufferSceneDataStatusCode.FRAME_TARGET_NOT_NATIVE_WRITABLE;
        }
        return GBufferSceneDataStatusCode.READY;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_LABEL;
        }
        return value.trim();
    }
}
