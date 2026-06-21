package net.lucerna.render.gbuffer;

import java.util.Objects;

public record GBufferSceneDataSamplingEvidence(
        GBufferSceneDataSamplingSourceKind sourceKind,
        int sampleCount,
        int nonzeroDepthSampleCount,
        double minNormalizedDepth,
        double maxNormalizedDepth,
        int normalSampleCount,
        int materialIdSampleCount,
        int albedoSampleCount,
        int emissiveSampleCount,
        String blockerReason
) {
    private static final String READY = "ready";

    public GBufferSceneDataSamplingEvidence {
        if (sourceKind == null) {
            sourceKind = GBufferSceneDataSamplingSourceKind.UNKNOWN;
        }
        sampleCount = Math.max(0, sampleCount);
        nonzeroDepthSampleCount = Math.min(Math.max(0, nonzeroDepthSampleCount), sampleCount);
        normalSampleCount = Math.min(Math.max(0, normalSampleCount), sampleCount);
        materialIdSampleCount = Math.min(Math.max(0, materialIdSampleCount), sampleCount);
        albedoSampleCount = Math.min(Math.max(0, albedoSampleCount), sampleCount);
        emissiveSampleCount = Math.min(Math.max(0, emissiveSampleCount), sampleCount);
        if (nonzeroDepthSampleCount == 0) {
            minNormalizedDepth = 0.0D;
            maxNormalizedDepth = 0.0D;
        } else {
            minNormalizedDepth = clampNormalizedDepth(minNormalizedDepth);
            maxNormalizedDepth = clampNormalizedDepth(maxNormalizedDepth);
            if (minNormalizedDepth > maxNormalizedDepth) {
                double swapped = minNormalizedDepth;
                minNormalizedDepth = maxNormalizedDepth;
                maxNormalizedDepth = swapped;
            }
        }
        if (blockerReason == null || blockerReason.isBlank()) {
            blockerReason = defaultBlocker(
                    sourceKind,
                    sampleCount,
                    nonzeroDepthSampleCount,
                    normalSampleCount,
                    materialIdSampleCount,
                    albedoSampleCount,
                    emissiveSampleCount
            );
        } else {
            blockerReason = blockerReason.trim();
        }
    }

    public GBufferSceneDataSamplingEvidence(
            GBufferSceneDataSamplingSourceKind sourceKind,
            int sampleCount,
            int nonzeroDepthSampleCount,
            double minNormalizedDepth,
            double maxNormalizedDepth,
            int materialIdSampleCount,
            String blockerReason
    ) {
        this(
                sourceKind,
                sampleCount,
                nonzeroDepthSampleCount,
                minNormalizedDepth,
                maxNormalizedDepth,
                0,
                materialIdSampleCount,
                0,
                0,
                blockerReason
        );
    }

    public static GBufferSceneDataSamplingEvidence missing(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.MISSING,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence metadataOnly(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.METADATA_ONLY,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence contractOnly(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.CONTRACT_ONLY,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence publicMojangOpaqueOnly(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.PUBLIC_MOJANG_OPAQUE_ONLY,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence publicMojangDepthViewShaderProofRequired(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.PUBLIC_MOJANG_DEPTH_VIEW_SHADER_PROOF_REQUIRED,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence synthetic(String reason) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.LUCERNA_SYNTHETIC,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledDepth(
            int sampleCount,
            int nonzeroDepthSampleCount,
            double minNormalizedDepth,
            double maxNormalizedDepth,
            String reason
    ) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.ACTUAL_SAMPLED_DEPTH_ATTACHMENT,
                sampleCount,
                nonzeroDepthSampleCount,
                minNormalizedDepth,
                maxNormalizedDepth,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledNormal(
            int sampleCount,
            String reason
    ) {
        return sampledNormal(sampleCount, sampleCount, reason);
    }

    public static GBufferSceneDataSamplingEvidence sampledNormal(
            int sampleCount,
            int normalSampleCount,
            String reason
    ) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.SAMPLED_NORMAL_ATTACHMENT,
                sampleCount,
                0,
                0.0D,
                0.0D,
                normalSampleCount,
                0,
                0,
                0,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledMaterial(
            int sampleCount,
            int materialIdSampleCount,
            String reason
    ) {
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.SAMPLED_MATERIAL_ATTACHMENT,
                sampleCount,
                0,
                0.0D,
                0.0D,
                materialIdSampleCount,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledAlbedo(
            int sampleCount,
            int albedoSampleCount,
            String reason
    ) {
        String resolvedReason = reason;
        if ((resolvedReason == null || resolvedReason.isBlank()) && albedoSampleCount <= 0) {
            resolvedReason = "Albedo attachment has no reported albedo samples.";
        }
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.ACTUAL_SAMPLED_ATTACHMENT,
                sampleCount,
                0,
                0.0D,
                0.0D,
                0,
                0,
                albedoSampleCount,
                0,
                resolvedReason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledEmissive(
            int sampleCount,
            int emissiveSampleCount,
            String reason
    ) {
        String resolvedReason = reason;
        if ((resolvedReason == null || resolvedReason.isBlank()) && emissiveSampleCount <= 0) {
            resolvedReason = "Emissive attachment has no reported emissive samples.";
        }
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.ACTUAL_SAMPLED_ATTACHMENT,
                sampleCount,
                0,
                0.0D,
                0.0D,
                0,
                0,
                0,
                emissiveSampleCount,
                resolvedReason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledAttachment(
            GBufferSceneDataKind kind,
            int sampleCount,
            int nonzeroDepthSampleCount,
            double minNormalizedDepth,
            double maxNormalizedDepth,
            int materialIdSampleCount,
            String reason
    ) {
        return sampledAttachment(
                kind,
                sampleCount,
                nonzeroDepthSampleCount,
                minNormalizedDepth,
                maxNormalizedDepth,
                sampleCount,
                materialIdSampleCount,
                sampleCount,
                sampleCount,
                reason
        );
    }

    public static GBufferSceneDataSamplingEvidence sampledAttachment(
            GBufferSceneDataKind kind,
            int sampleCount,
            int nonzeroDepthSampleCount,
            double minNormalizedDepth,
            double maxNormalizedDepth,
            int normalSampleCount,
            int materialIdSampleCount,
            int albedoSampleCount,
            int emissiveSampleCount,
            String reason
    ) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case DEPTH -> sampledDepth(sampleCount, nonzeroDepthSampleCount, minNormalizedDepth, maxNormalizedDepth, reason);
            case NORMAL -> sampledNormal(sampleCount, normalSampleCount, reason);
            case MATERIAL -> sampledMaterial(sampleCount, materialIdSampleCount, reason);
            case ALBEDO -> sampledAlbedo(sampleCount, albedoSampleCount, reason);
            case EMISSIVE -> sampledEmissive(sampleCount, emissiveSampleCount, reason);
            default -> new GBufferSceneDataSamplingEvidence(
                    GBufferSceneDataSamplingSourceKind.ACTUAL_SAMPLED_ATTACHMENT,
                    sampleCount,
                    0,
                    0.0D,
                    0.0D,
                    0,
                    reason
            );
        };
    }

    public static GBufferSceneDataSamplingEvidence infer(
            GBufferSceneDataKind kind,
            boolean hasExtent,
            boolean hasNativeView,
            boolean sampled,
            boolean sceneDerived,
            GBufferSceneDataSource source,
            String reason
    ) {
        Objects.requireNonNull(kind, "kind");
        if (source == null || source.missing()) {
            return missing(reason);
        }
        if (source.publicMojangOpaqueOnly()) {
            return publicMojangOpaqueOnly(reason);
        }
        if (source.publicMojangDepthView()) {
            return publicMojangDepthViewShaderProofRequired(reason);
        }
        if (source.contractOnly()) {
            return contractOnly(reason);
        }
        if (source.metadataOnly()) {
            return metadataOnly(reason);
        }
        if (source.synthetic()) {
            return synthetic(reason);
        }
        if (!hasExtent || !hasNativeView || !sampled || !sceneDerived) {
            return missing(reason);
        }
        if (source.actualMinecraftSampled()) {
            return sampledAttachment(
                    kind,
                    0,
                    0,
                    0.0D,
                    0.0D,
                    0,
                    "Sampled Minecraft attachment was declared, but no sample evidence was reported."
            );
        }
        return new GBufferSceneDataSamplingEvidence(
                GBufferSceneDataSamplingSourceKind.UNKNOWN,
                0,
                0,
                0.0D,
                0.0D,
                0,
                reason
        );
    }

    public boolean provesDepthSampling() {
        return this.sourceKind.actualSampledDepthAttachment()
                && this.sampleCount > 0
                && this.nonzeroDepthSampleCount > 0;
    }

    public boolean provesNormalSampling() {
        return this.sourceKind.sampledNormalAttachment()
                && this.sampleCount > 0
                && this.normalSampleCount > 0;
    }

    public boolean provesMaterialSampling() {
        return this.sourceKind.sampledMaterialAttachment()
                && this.sampleCount > 0
                && this.materialIdSampleCount > 0;
    }

    public boolean provesAlbedoSampling() {
        return this.sourceKind.actualSampledAttachment()
                && this.sampleCount > 0
                && this.albedoSampleCount > 0;
    }

    public boolean provesEmissiveSampling() {
        return this.sourceKind.actualSampledAttachment()
                && this.sampleCount > 0
                && this.emissiveSampleCount > 0;
    }

    public boolean provesSampledAttachment(GBufferSceneDataKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case DEPTH -> provesDepthSampling();
            case NORMAL -> provesNormalSampling();
            case MATERIAL -> provesMaterialSampling();
            case ALBEDO -> provesAlbedoSampling();
            case EMISSIVE -> provesEmissiveSampling();
            default -> this.sourceKind.actualSampledAttachment() && this.sampleCount > 0;
        };
    }

    public int sceneSampleCount(GBufferSceneDataKind kind) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case DEPTH -> depthSampleCount();
            case NORMAL -> provesNormalSampling() ? this.normalSampleCount : 0;
            case MATERIAL -> provesMaterialSampling() ? this.materialIdSampleCount : 0;
            case ALBEDO -> provesAlbedoSampling() ? this.albedoSampleCount : 0;
            case EMISSIVE -> provesEmissiveSampling() ? this.emissiveSampleCount : 0;
            default -> this.sourceKind.actualSampledAttachment() ? this.sampleCount : 0;
        };
    }

    public boolean sampledNormalOrMaterialAttachment() {
        return this.sourceKind.sampledNormalOrMaterialAttachment();
    }

    public boolean publicMojangOpaqueOnly() {
        return this.sourceKind.publicMojangOpaqueOnly();
    }

    public boolean publicMojangDepthViewShaderProofRequired() {
        return this.sourceKind.publicMojangDepthViewShaderProofRequired();
    }

    public boolean depthViewPresent() {
        return publicMojangDepthViewShaderProofRequired() || provesDepthSampling();
    }

    public boolean depthTextureSampleBindingReady() {
        return publicMojangDepthViewShaderProofRequired() || provesDepthSampling();
    }

    public boolean depthSamplingEvidenceReady() {
        return provesDepthSampling();
    }

    public int depthSampleCount() {
        return provesDepthSampling() ? this.sampleCount : 0;
    }

    public String depthSamplingMarker() {
        return this.sourceKind.label();
    }

    public String depthSamplingBlocker() {
        return depthSamplingEvidenceReady() ? READY : this.blockerReason;
    }

    public boolean metadataOnly() {
        return this.sourceKind.metadataOnly();
    }

    public boolean missing() {
        return this.sourceKind.missing();
    }

    public boolean synthetic() {
        return this.sourceKind.synthetic();
    }

    public boolean contractOnly() {
        return this.sourceKind.contractOnly();
    }

    public String statusLabel() {
        return "sourceKind=" + this.sourceKind.label()
                + ", sampleCount=" + this.sampleCount
                + ", nonzeroDepthSampleCount=" + this.nonzeroDepthSampleCount
                + ", minNormalizedDepth=" + this.minNormalizedDepth
                + ", maxNormalizedDepth=" + this.maxNormalizedDepth
                + ", normalSampleCount=" + this.normalSampleCount
                + ", materialIdSampleCount=" + this.materialIdSampleCount
                + ", albedoSampleCount=" + this.albedoSampleCount
                + ", emissiveSampleCount=" + this.emissiveSampleCount
                + ", depthViewPresent=" + depthViewPresent()
                + ", depthTextureSampleBindingReady=" + depthTextureSampleBindingReady()
                + ", depthSamplingEvidenceReady=" + depthSamplingEvidenceReady()
                + ", depthSampleCount=" + depthSampleCount()
                + ", depthSamplingMarker=" + depthSamplingMarker()
                + ", depthSamplingBlocker=" + depthSamplingBlocker()
                + ", blockerReason=" + this.blockerReason;
    }

    private static double clampNormalizedDepth(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static String defaultBlocker(
            GBufferSceneDataSamplingSourceKind sourceKind,
            int sampleCount,
            int nonzeroDepthSampleCount,
            int normalSampleCount,
            int materialIdSampleCount,
            int albedoSampleCount,
            int emissiveSampleCount
    ) {
        if (sourceKind.missing()) {
            return "Scene-data attachment is missing.";
        }
        if (sourceKind.publicMojangOpaqueOnly()) {
            return "Only public Mojang Java-opaque framebuffer fallback is available.";
        }
        if (sourceKind.publicMojangDepthViewShaderProofRequired()) {
            return "Public Mojang depth view is present, but actual depth texel sampling requires controller proof from a shader pass.";
        }
        if (sourceKind.contractOnly()) {
            return "Contract-only placeholder is not sampled Minecraft G-buffer data.";
        }
        if (sourceKind.metadataOnly()) {
            return "Only metadata is available; no sampled scene-data attachment is bound.";
        }
        if (sourceKind.synthetic()) {
            return "Synthetic Lucerna data is not proof of sampled Minecraft G-buffer data.";
        }
        if (sourceKind.actualSampledDepthAttachment() && nonzeroDepthSampleCount == 0) {
            return "Depth attachment has no nonzero normalized depth samples.";
        }
        if (sourceKind.sampledNormalAttachment() && normalSampleCount == 0) {
            return "Normal attachment has no reported normal samples.";
        }
        if (sourceKind.sampledMaterialAttachment() && materialIdSampleCount == 0) {
            return "Material attachment has no material id samples.";
        }
        if (sourceKind.actualSampledAttachment() && sampleCount == 0) {
            return "Sampled attachment has no reported samples.";
        }
        return READY;
    }
}
