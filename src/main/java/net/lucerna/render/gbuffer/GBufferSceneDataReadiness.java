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
        boolean frameDataPublicMojangOpaqueOnly,
        boolean frameDataMetadataOnly,
        boolean frameDataMissing,
        GBufferSceneDataSamplingEvidence samplingEvidence,
        boolean actualSampledDepthAttachment,
        boolean sampledNormalMaterialAttachment,
        boolean sampledAlbedoAttachment,
        boolean sampledEmissiveAttachment,
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
            frameDataSource = GBufferSceneDataSource.missing(blocker);
        }
        if (samplingEvidence == null) {
            samplingEvidence = fallbackSamplingEvidence(
                    kind,
                    false,
                    frameTargetAvailable,
                    frameAttachmentMetadataOnly,
                    frameAttachmentJavaOpaque,
                    blocker
            );
        }
        frameDataActualMinecraftSampled = frameDataSource.actualMinecraftSampled() && frameDataActualMinecraftSampled;
        frameDataMojangSampled = frameDataSource.mojangSampled() && frameDataMojangSampled;
        frameDataSodiumSampled = frameDataSource.sodiumSampled() && frameDataSodiumSampled;
        frameDataSynthetic = frameDataSource.synthetic() || frameDataSynthetic;
        frameDataContractOnly = frameDataSource.contractOnly() || frameDataContractOnly;
        frameDataPublicMojangOpaqueOnly = frameDataSource.publicMojangOpaqueOnly()
                || samplingEvidence.publicMojangOpaqueOnly()
                || frameDataPublicMojangOpaqueOnly;
        frameDataMetadataOnly = frameDataSource.metadataOnly()
                || samplingEvidence.metadataOnly()
                || frameDataMetadataOnly;
        frameDataMissing = frameDataSource.missing() || samplingEvidence.missing() || frameDataMissing;
        actualSampledDepthAttachment = kind == GBufferSceneDataKind.DEPTH
                && frameDataActualMinecraftSampled
                && samplingEvidence.provesDepthSampling();
        sampledNormalMaterialAttachment = (kind == GBufferSceneDataKind.NORMAL || kind == GBufferSceneDataKind.MATERIAL)
                && frameDataActualMinecraftSampled
                && samplingEvidence.provesSampledAttachment(kind);
        sampledAlbedoAttachment = kind == GBufferSceneDataKind.ALBEDO
                && frameDataActualMinecraftSampled
                && samplingEvidence.provesAlbedoSampling();
        sampledEmissiveAttachment = kind == GBufferSceneDataKind.EMISSIVE
                && frameDataActualMinecraftSampled
                && samplingEvidence.provesEmissiveSampling();
        if (statusCode == null) {
            statusCode = GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
        }
        if (statusCode == GBufferSceneDataStatusCode.READY) {
            if (frameDataPublicMojangOpaqueOnly) {
                statusCode = GBufferSceneDataStatusCode.FRAME_DATA_PUBLIC_MOJANG_OPAQUE_ONLY;
            } else if (frameDataMetadataOnly) {
                statusCode = GBufferSceneDataStatusCode.FRAME_DATA_METADATA_ONLY;
            } else if (frameDataMissing) {
                statusCode = GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
            } else if (!samplingEvidence.provesSampledAttachment(kind)) {
                statusCode = sampleEvidenceStatus(kind);
            }
        }
        if (blocker == null || blocker.isBlank()
                || (statusCode != GBufferSceneDataStatusCode.READY && "ready".equals(blocker))) {
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
                : fallbackFrameDataSource(kind, frameTargetAvailable, frameAttachmentMetadataOnly, frameAttachmentJavaOpaque);
        GBufferSceneDataSamplingEvidence samplingEvidence = frameDataBound
                ? frameDataAttachment.samplingEvidence()
                : fallbackSamplingEvidence(
                        kind,
                        false,
                        frameTargetAvailable,
                        frameAttachmentMetadataOnly,
                        frameAttachmentJavaOpaque,
                        "No scene-data attachment bound for " + kind.label() + "."
                );
        boolean frameDataActualMinecraftSampled = frameDataBound && frameDataAttachment.actualMinecraftSampled();
        boolean frameDataMojangSampled = frameDataBound && frameDataAttachment.mojangSampled();
        boolean frameDataSodiumSampled = frameDataBound && frameDataAttachment.sodiumSampled();
        boolean frameDataSynthetic = frameDataBound && frameDataAttachment.syntheticSource();
        boolean frameDataContractOnly = !frameDataBound || frameDataAttachment.contractOnlySource();
        boolean frameDataPublicMojangOpaqueOnly = (!frameDataBound && frameTargetAvailable && frameAttachmentJavaOpaque)
                || (frameDataBound && frameDataAttachment.publicMojangOpaqueOnlySource())
                || samplingEvidence.publicMojangOpaqueOnly();
        boolean frameDataMetadataOnly = (!frameDataBound && frameTargetAvailable && frameAttachmentMetadataOnly)
                || (frameDataBound && frameDataAttachment.metadataOnlySource())
                || samplingEvidence.metadataOnly();
        boolean frameDataMissing = !frameDataBound
                || (frameDataBound && frameDataAttachment.missingSource())
                || samplingEvidence.missing();
        GBufferSceneDataStatusCode statusCode = resolveStatus(
                kind,
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
                frameDataContractOnly,
                frameDataPublicMojangOpaqueOnly,
                frameDataMetadataOnly,
                samplingEvidence
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
                frameDataPublicMojangOpaqueOnly,
                frameDataMetadataOnly,
                frameDataMissing,
                samplingEvidence,
                frameDataBound && frameDataAttachment.actualSampledDepthAttachment(),
                frameDataBound && frameDataAttachment.sampledNormalMaterialAttachment(),
                frameDataBound && frameDataAttachment.sampledAlbedoAttachment(),
                frameDataBound && frameDataAttachment.sampledEmissiveAttachment(),
                statusCode,
                statusCode == GBufferSceneDataStatusCode.READY ? "ready" : evidenceBlocker(statusCode, samplingEvidence)
        );
    }

    public boolean ready() {
        return this.statusCode == GBufferSceneDataStatusCode.READY;
    }

    public boolean trueSampledSceneDataReady() {
        return ready()
                && this.frameDataActualMinecraftSampled
                && !this.frameDataSynthetic
                && !this.frameDataContractOnly
                && !this.frameDataPublicMojangOpaqueOnly
                && !this.frameDataMetadataOnly
                && !this.frameDataMissing
                && this.samplingEvidence.provesSampledAttachment(this.kind);
    }

    public boolean publicMojangOpaqueOnlyFallback() {
        return this.frameDataPublicMojangOpaqueOnly
                || this.statusCode == GBufferSceneDataStatusCode.FRAME_DATA_PUBLIC_MOJANG_OPAQUE_ONLY
                || this.statusCode == GBufferSceneDataStatusCode.FRAME_TARGET_JAVA_OPAQUE;
    }

    public boolean metadataOnly() {
        return this.frameDataMetadataOnly
                || this.statusCode == GBufferSceneDataStatusCode.FRAME_DATA_METADATA_ONLY
                || this.statusCode == GBufferSceneDataStatusCode.FRAME_TARGET_METADATA_ONLY;
    }

    public boolean missing() {
        return this.frameDataMissing
                || this.statusCode == GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
    }

    public GBufferSceneDataSamplingSourceKind samplingSourceKind() {
        return this.samplingEvidence.sourceKind();
    }

    public int sampleCount() {
        return this.samplingEvidence.sampleCount();
    }

    public int sceneSampleCount() {
        return this.samplingEvidence.sceneSampleCount(this.kind);
    }

    public int nonzeroDepthSampleCount() {
        return this.samplingEvidence.nonzeroDepthSampleCount();
    }

    public double minNormalizedDepth() {
        return this.samplingEvidence.minNormalizedDepth();
    }

    public double maxNormalizedDepth() {
        return this.samplingEvidence.maxNormalizedDepth();
    }

    public int normalSampleCount() {
        return this.samplingEvidence.normalSampleCount();
    }

    public int materialIdSampleCount() {
        return this.samplingEvidence.materialIdSampleCount();
    }

    public int albedoSampleCount() {
        return this.samplingEvidence.albedoSampleCount();
    }

    public int emissiveSampleCount() {
        return this.samplingEvidence.emissiveSampleCount();
    }

    public String samplingBlockerReason() {
        return this.samplingEvidence.blockerReason();
    }

    public boolean publicMojangDepthView() {
        return this.frameDataSource.publicMojangDepthView()
                || this.samplingEvidence.publicMojangDepthViewShaderProofRequired();
    }

    public boolean depthViewPresent() {
        return this.kind == GBufferSceneDataKind.DEPTH
                && this.frameDataBound
                && this.frameDataNativeReadable;
    }

    public boolean depthTextureSampleBindingReady() {
        return this.kind == GBufferSceneDataKind.DEPTH
                && this.frameDataBound
                && this.frameDataNativeReadable
                && this.frameDataSampled
                && (publicMojangDepthView() || this.frameDataActualMinecraftSampled);
    }

    public boolean depthSamplingEvidenceReady() {
        return this.kind == GBufferSceneDataKind.DEPTH
                && this.actualSampledDepthAttachment
                && this.samplingEvidence.provesDepthSampling();
    }

    public int depthSampleCount() {
        return depthSamplingEvidenceReady() ? this.samplingEvidence.sampleCount() : 0;
    }

    public String depthSamplingMarker() {
        if (this.kind != GBufferSceneDataKind.DEPTH) {
            return "not-depth";
        }
        if (!depthViewPresent()) {
            return "depth-view-missing";
        }
        if (depthSamplingEvidenceReady()) {
            return "actual-sampled-depth-attachment";
        }
        if (publicMojangDepthView()) {
            return "public-mojang-depth-view-shader-proof-required";
        }
        return this.samplingEvidence.depthSamplingMarker();
    }

    public String depthSamplingBlocker() {
        if (depthSamplingEvidenceReady()) {
            return "ready";
        }
        return this.samplingEvidence.depthSamplingBlocker();
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
                + ", publicMojangOpaqueOnlyFallback=" + publicMojangOpaqueOnlyFallback()
                + ", publicMojangDepthView=" + publicMojangDepthView()
                + ", metadataOnly=" + metadataOnly()
                + ", missing=" + missing()
                + ", actualSampledDepthAttachment=" + this.actualSampledDepthAttachment
                + ", sampledNormalMaterialAttachment=" + this.sampledNormalMaterialAttachment
                + ", sampledAlbedoAttachment=" + this.sampledAlbedoAttachment
                + ", sampledEmissiveAttachment=" + this.sampledEmissiveAttachment
                + ", samplingSourceKind=" + samplingSourceKind().label()
                + ", sampleCount=" + sampleCount()
                + ", sceneSampleCount=" + sceneSampleCount()
                + ", nonzeroDepthSampleCount=" + nonzeroDepthSampleCount()
                + ", minNormalizedDepth=" + minNormalizedDepth()
                + ", maxNormalizedDepth=" + maxNormalizedDepth()
                + ", normalSampleCount=" + normalSampleCount()
                + ", materialIdSampleCount=" + materialIdSampleCount()
                + ", albedoSampleCount=" + albedoSampleCount()
                + ", emissiveSampleCount=" + emissiveSampleCount()
                + ", depthViewPresent=" + depthViewPresent()
                + ", depthTextureSampleBindingReady=" + depthTextureSampleBindingReady()
                + ", depthSamplingEvidenceReady=" + depthSamplingEvidenceReady()
                + ", depthSampleCount=" + depthSampleCount()
                + ", depthSamplingMarker=" + depthSamplingMarker()
                + ", depthSamplingBlocker=" + depthSamplingBlocker()
                + ", samplingBlockerReason=" + samplingBlockerReason()
                + ", samplingEvidence={" + this.samplingEvidence.statusLabel() + "}"
                + ", blocker=" + this.blocker
                + "}";
    }

    private static GBufferSceneDataStatusCode resolveStatus(
            GBufferSceneDataKind kind,
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
            boolean frameDataContractOnly,
            boolean frameDataPublicMojangOpaqueOnly,
            boolean frameDataMetadataOnly,
            GBufferSceneDataSamplingEvidence samplingEvidence
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
            if (frameTargetAvailable && (frameDataPublicMojangOpaqueOnly || frameAttachmentJavaOpaque)) {
                return GBufferSceneDataStatusCode.FRAME_DATA_PUBLIC_MOJANG_OPAQUE_ONLY;
            }
            if (frameTargetAvailable && (frameDataMetadataOnly || frameAttachmentMetadataOnly)) {
                return GBufferSceneDataStatusCode.FRAME_DATA_METADATA_ONLY;
            }
            return GBufferSceneDataStatusCode.FRAME_DATA_ATTACHMENT_MISSING;
        }
        if (frameDataPublicMojangOpaqueOnly) {
            return GBufferSceneDataStatusCode.FRAME_DATA_PUBLIC_MOJANG_OPAQUE_ONLY;
        }
        if (frameDataMetadataOnly) {
            return GBufferSceneDataStatusCode.FRAME_DATA_METADATA_ONLY;
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
        if (!samplingEvidence.provesSampledAttachment(kind)) {
            return sampleEvidenceStatus(kind);
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

    private static GBufferSceneDataStatusCode sampleEvidenceStatus(GBufferSceneDataKind kind) {
        return switch (kind) {
            case DEPTH -> GBufferSceneDataStatusCode.FRAME_DATA_DEPTH_SAMPLE_EVIDENCE_MISSING;
            case NORMAL -> GBufferSceneDataStatusCode.FRAME_DATA_NORMAL_SAMPLE_EVIDENCE_MISSING;
            case MATERIAL -> GBufferSceneDataStatusCode.FRAME_DATA_MATERIAL_ID_SAMPLE_EVIDENCE_MISSING;
            case ALBEDO -> GBufferSceneDataStatusCode.FRAME_DATA_ALBEDO_SAMPLE_EVIDENCE_MISSING;
            case EMISSIVE -> GBufferSceneDataStatusCode.FRAME_DATA_EMISSIVE_SAMPLE_EVIDENCE_MISSING;
            default -> GBufferSceneDataStatusCode.FRAME_DATA_SAMPLE_EVIDENCE_MISSING;
        };
    }

    private static String evidenceBlocker(
            GBufferSceneDataStatusCode statusCode,
            GBufferSceneDataSamplingEvidence samplingEvidence
    ) {
        if (samplingEvidence != null && samplingEvidence.blockerReason() != null
                && !samplingEvidence.blockerReason().isBlank()
                && !"ready".equals(samplingEvidence.blockerReason())) {
            return statusCode.description() + " evidence=" + samplingEvidence.blockerReason();
        }
        return statusCode.description();
    }

    private static GBufferSceneDataSource fallbackFrameDataSource(
            GBufferSceneDataKind kind,
            boolean frameTargetAvailable,
            boolean frameAttachmentMetadataOnly,
            boolean frameAttachmentJavaOpaque
    ) {
        String reason = "No scene-data attachment bound for " + kind.label() + ".";
        if (frameTargetAvailable && frameAttachmentJavaOpaque) {
            return GBufferSceneDataSource.publicMojangOpaqueOnly(reason);
        }
        if (frameTargetAvailable && frameAttachmentMetadataOnly) {
            return GBufferSceneDataSource.metadataOnly(reason);
        }
        return GBufferSceneDataSource.missing(reason);
    }

    private static GBufferSceneDataSamplingEvidence fallbackSamplingEvidence(
            GBufferSceneDataKind kind,
            boolean frameDataBound,
            boolean frameTargetAvailable,
            boolean frameAttachmentMetadataOnly,
            boolean frameAttachmentJavaOpaque,
            String reason
    ) {
        Objects.requireNonNull(kind, "kind");
        if (frameTargetAvailable && frameAttachmentJavaOpaque) {
            return GBufferSceneDataSamplingEvidence.publicMojangOpaqueOnly(reason);
        }
        if (frameTargetAvailable && frameAttachmentMetadataOnly) {
            return GBufferSceneDataSamplingEvidence.metadataOnly(reason);
        }
        if (!frameDataBound) {
            return GBufferSceneDataSamplingEvidence.missing(reason);
        }
        return GBufferSceneDataSamplingEvidence.missing(reason);
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
