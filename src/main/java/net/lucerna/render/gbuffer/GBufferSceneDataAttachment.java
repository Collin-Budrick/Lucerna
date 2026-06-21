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
        String sourceDescription,
        GBufferSceneDataSamplingEvidence samplingEvidence
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public GBufferSceneDataAttachment {
        Objects.requireNonNull(kind, "kind");
        width = Math.max(0, width);
        height = Math.max(0, height);
        format = normalizeLabel(format);
        layout = normalizeLabel(layout);
        if (source == null) {
            source = GBufferSceneDataSource.metadataOnly(sourceDescription);
        }
        if (sourceDescription == null || sourceDescription.isBlank()) {
            sourceDescription = "No scene-derived frame-data attachment is bound.";
        } else {
            sourceDescription = sourceDescription.trim();
        }
        if (samplingEvidence == null) {
            samplingEvidence = GBufferSceneDataSamplingEvidence.infer(
                    kind,
                    width > 0 && height > 0,
                    imageHandle != 0L && imageViewHandle != 0L,
                    sampled,
                    sceneDerived,
                    source,
                    sourceDescription
            );
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
                GBufferSceneDataSource.missing(reason),
                reason,
                GBufferSceneDataSamplingEvidence.missing(reason)
        );
    }

    public static GBufferSceneDataAttachment publicMojangOpaqueOnly(
            GBufferSceneDataKind kind,
            int width,
            int height,
            String format,
            String sourceDescription
    ) {
        return new GBufferSceneDataAttachment(
                kind,
                width,
                height,
                format,
                UNKNOWN_LABEL,
                0L,
                0L,
                false,
                true,
                GBufferSceneDataSource.publicMojangOpaqueOnly(sourceDescription),
                sourceDescription,
                GBufferSceneDataSamplingEvidence.publicMojangOpaqueOnly(sourceDescription)
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
                GBufferSceneDataSource.metadataOnly(sourceDescription),
                sourceDescription,
                null
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
                source,
                sourceDescription,
                null
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
            String sourceDescription,
            GBufferSceneDataSamplingEvidence samplingEvidence
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
                sourceDescription,
                samplingEvidence
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
        return mojangSampled(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                sourceDescription,
                null
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
            String sourceDescription,
            GBufferSceneDataSamplingEvidence samplingEvidence
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
                sourceDescription,
                samplingEvidence
        );
    }

    public static GBufferSceneDataAttachment publicMojangDepthView(
            int width,
            int height,
            String format,
            String layout,
            long imageHandle,
            long imageViewHandle,
            String sourceDescription
    ) {
        return new GBufferSceneDataAttachment(
                GBufferSceneDataKind.DEPTH,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                imageHandle != 0L && imageViewHandle != 0L,
                true,
                GBufferSceneDataSource.publicMojangDepthView(sourceDescription),
                sourceDescription,
                GBufferSceneDataSamplingEvidence.publicMojangDepthViewShaderProofRequired(sourceDescription)
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
        return sodiumSampled(
                kind,
                width,
                height,
                format,
                layout,
                imageHandle,
                imageViewHandle,
                sourceDescription,
                null
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
            String sourceDescription,
            GBufferSceneDataSamplingEvidence samplingEvidence
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
                sourceDescription,
                samplingEvidence
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
                sourceDescription,
                GBufferSceneDataSamplingEvidence.synthetic(sourceDescription)
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

    public boolean publicMojangOpaqueOnlySource() {
        return this.source.publicMojangOpaqueOnly();
    }

    public boolean publicMojangDepthViewSource() {
        return this.source.publicMojangDepthView();
    }

    public boolean metadataOnlySource() {
        return this.source.metadataOnly();
    }

    public boolean missingSource() {
        return this.source.missing();
    }

    public boolean actualSampledDepthAttachment() {
        return this.kind == GBufferSceneDataKind.DEPTH
                && actualMinecraftSampled()
                && this.samplingEvidence.provesDepthSampling();
    }

    public boolean sampledNormalMaterialAttachment() {
        return (this.kind == GBufferSceneDataKind.NORMAL || this.kind == GBufferSceneDataKind.MATERIAL)
                && actualMinecraftSampled()
                && this.samplingEvidence.provesSampledAttachment(this.kind);
    }

    public boolean sampledAlbedoAttachment() {
        return this.kind == GBufferSceneDataKind.ALBEDO
                && actualMinecraftSampled()
                && this.samplingEvidence.provesAlbedoSampling();
    }

    public boolean sampledEmissiveAttachment() {
        return this.kind == GBufferSceneDataKind.EMISSIVE
                && actualMinecraftSampled()
                && this.samplingEvidence.provesEmissiveSampling();
    }

    public boolean sampledLightingAttachment() {
        return this.kind.requiredForLighting()
                && actualMinecraftSampled()
                && this.samplingEvidence.provesSampledAttachment(this.kind);
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

    public boolean depthViewPresent() {
        return this.kind == GBufferSceneDataKind.DEPTH && hasNativeView();
    }

    public boolean depthTextureSampleBindingReady() {
        return this.kind == GBufferSceneDataKind.DEPTH
                && hasNativeView()
                && this.sampled
                && (this.source.publicMojangDepthView() || actualMinecraftSampled());
    }

    public boolean depthSamplingEvidenceReady() {
        return actualSampledDepthAttachment();
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
        if (this.source.publicMojangDepthView()) {
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

    public boolean readyForLighting() {
        return this.hasExtent()
                && this.hasNativeView()
                && this.sampled
                && this.sceneDerived
                && actualMinecraftSampled()
                && this.samplingEvidence.provesSampledAttachment(this.kind);
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
                + ", source={" + this.source.statusLabel() + "}"
                + ", samplingSourceKind=" + this.samplingEvidence.sourceKind().label()
                + ", sampleCount=" + this.samplingEvidence.sampleCount()
                + ", sceneSampleCount=" + sceneSampleCount()
                + ", nonzeroDepthSampleCount=" + this.samplingEvidence.nonzeroDepthSampleCount()
                + ", minNormalizedDepth=" + this.samplingEvidence.minNormalizedDepth()
                + ", maxNormalizedDepth=" + this.samplingEvidence.maxNormalizedDepth()
                + ", normalSampleCount=" + this.samplingEvidence.normalSampleCount()
                + ", materialIdSampleCount=" + this.samplingEvidence.materialIdSampleCount()
                + ", albedoSampleCount=" + this.samplingEvidence.albedoSampleCount()
                + ", emissiveSampleCount=" + this.samplingEvidence.emissiveSampleCount()
                + ", publicMojangDepthViewSource=" + publicMojangDepthViewSource()
                + ", depthViewPresent=" + depthViewPresent()
                + ", depthTextureSampleBindingReady=" + depthTextureSampleBindingReady()
                + ", depthSamplingEvidenceReady=" + depthSamplingEvidenceReady()
                + ", depthSampleCount=" + depthSampleCount()
                + ", depthSamplingMarker=" + depthSamplingMarker()
                + ", depthSamplingBlocker=" + depthSamplingBlocker()
                + ", samplingBlockerReason=" + this.samplingEvidence.blockerReason()
                + ", samplingEvidence={" + this.samplingEvidence.statusLabel() + "}"
                + ", actualSampledDepthAttachment=" + actualSampledDepthAttachment()
                + ", sampledNormalMaterialAttachment=" + sampledNormalMaterialAttachment()
                + ", sampledAlbedoAttachment=" + sampledAlbedoAttachment()
                + ", sampledEmissiveAttachment=" + sampledEmissiveAttachment()
                + ", sampledLightingAttachment=" + sampledLightingAttachment();
    }

    private static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_LABEL;
        }
        return value.trim();
    }
}
