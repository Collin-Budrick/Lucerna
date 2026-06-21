package net.lucerna.render.gbuffer;

public enum GBufferSceneDataSamplingSourceKind {
    UNKNOWN(false, false, false, false, false, false, false, false, false, "unknown"),
    MISSING(false, false, false, false, false, false, true, false, false, "missing"),
    METADATA_ONLY(false, false, false, false, false, true, false, false, false, "metadata-only"),
    CONTRACT_ONLY(false, false, false, false, false, false, false, false, true, "contract-only"),
    PUBLIC_MOJANG_OPAQUE_ONLY(false, false, false, false, true, false, false, false, false, "public-mojang-opaque-only"),
    PUBLIC_MOJANG_DEPTH_VIEW_SHADER_PROOF_REQUIRED(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            "public-mojang-depth-view-shader-proof-required"
    ),
    LUCERNA_SYNTHETIC(false, false, false, false, false, false, false, true, false, "lucerna-synthetic"),
    ACTUAL_SAMPLED_DEPTH_ATTACHMENT(true, true, false, false, false, false, false, false, false, "actual-sampled-depth-attachment"),
    SAMPLED_NORMAL_ATTACHMENT(true, false, true, false, false, false, false, false, false, "sampled-normal-attachment"),
    SAMPLED_MATERIAL_ATTACHMENT(true, false, false, true, false, false, false, false, false, "sampled-material-attachment"),
    ACTUAL_SAMPLED_ATTACHMENT(true, false, false, false, false, false, false, false, false, "actual-sampled-attachment");

    private final boolean actualSampledAttachment;
    private final boolean actualSampledDepthAttachment;
    private final boolean sampledNormalAttachment;
    private final boolean sampledMaterialAttachment;
    private final boolean publicMojangOpaqueOnly;
    private final boolean metadataOnly;
    private final boolean missing;
    private final boolean synthetic;
    private final boolean contractOnly;
    private final String label;

    GBufferSceneDataSamplingSourceKind(
            boolean actualSampledAttachment,
            boolean actualSampledDepthAttachment,
            boolean sampledNormalAttachment,
            boolean sampledMaterialAttachment,
            boolean publicMojangOpaqueOnly,
            boolean metadataOnly,
            boolean missing,
            boolean synthetic,
            boolean contractOnly,
            String label
    ) {
        this.actualSampledAttachment = actualSampledAttachment;
        this.actualSampledDepthAttachment = actualSampledDepthAttachment;
        this.sampledNormalAttachment = sampledNormalAttachment;
        this.sampledMaterialAttachment = sampledMaterialAttachment;
        this.publicMojangOpaqueOnly = publicMojangOpaqueOnly;
        this.metadataOnly = metadataOnly;
        this.missing = missing;
        this.synthetic = synthetic;
        this.contractOnly = contractOnly;
        this.label = label;
    }

    public boolean actualSampledAttachment() {
        return this.actualSampledAttachment;
    }

    public boolean actualSampledDepthAttachment() {
        return this.actualSampledDepthAttachment;
    }

    public boolean sampledNormalAttachment() {
        return this.sampledNormalAttachment;
    }

    public boolean sampledMaterialAttachment() {
        return this.sampledMaterialAttachment;
    }

    public boolean sampledNormalOrMaterialAttachment() {
        return this.sampledNormalAttachment || this.sampledMaterialAttachment;
    }

    public boolean publicMojangOpaqueOnly() {
        return this.publicMojangOpaqueOnly;
    }

    public boolean publicMojangDepthViewShaderProofRequired() {
        return this == PUBLIC_MOJANG_DEPTH_VIEW_SHADER_PROOF_REQUIRED;
    }

    public boolean metadataOnly() {
        return this.metadataOnly;
    }

    public boolean missing() {
        return this.missing;
    }

    public boolean synthetic() {
        return this.synthetic;
    }

    public boolean contractOnly() {
        return this.contractOnly;
    }

    public String label() {
        return this.label;
    }
}
