package net.lucerna.render.gbuffer;

public enum GBufferSceneDataSourceKind {
    UNKNOWN(false, false, false, false, false, false, false, false, "unknown"),
    MISSING(false, false, false, false, false, false, false, true, "missing"),
    METADATA_ONLY(false, false, false, false, false, false, true, false, "metadata-only"),
    CONTRACT_ONLY(false, false, false, false, true, false, false, false, "contract-only"),
    PUBLIC_MOJANG_OPAQUE_ONLY(false, false, false, false, false, true, false, false, "public-mojang-opaque-only"),
    PUBLIC_MOJANG_DEPTH_VIEW(false, false, false, false, false, false, false, false, "public-mojang-depth-view"),
    LUCERNA_SYNTHETIC(false, false, false, true, false, false, false, false, "lucerna-synthetic"),
    MINECRAFT_MOJANG_SAMPLED(true, true, false, false, false, false, false, false, "minecraft-mojang-sampled"),
    MINECRAFT_SODIUM_SAMPLED(true, false, true, false, false, false, false, false, "minecraft-sodium-sampled");

    private final boolean actualMinecraftSampled;
    private final boolean mojangSampled;
    private final boolean sodiumSampled;
    private final boolean synthetic;
    private final boolean contractOnly;
    private final boolean publicMojangOpaqueOnly;
    private final boolean metadataOnly;
    private final boolean missing;
    private final String label;

    GBufferSceneDataSourceKind(
            boolean actualMinecraftSampled,
            boolean mojangSampled,
            boolean sodiumSampled,
            boolean synthetic,
            boolean contractOnly,
            boolean publicMojangOpaqueOnly,
            boolean metadataOnly,
            boolean missing,
            String label
    ) {
        this.actualMinecraftSampled = actualMinecraftSampled;
        this.mojangSampled = mojangSampled;
        this.sodiumSampled = sodiumSampled;
        this.synthetic = synthetic;
        this.contractOnly = contractOnly;
        this.publicMojangOpaqueOnly = publicMojangOpaqueOnly;
        this.metadataOnly = metadataOnly;
        this.missing = missing;
        this.label = label;
    }

    public boolean actualMinecraftSampled() {
        return this.actualMinecraftSampled;
    }

    public boolean mojangSampled() {
        return this.mojangSampled;
    }

    public boolean sodiumSampled() {
        return this.sodiumSampled;
    }

    public boolean synthetic() {
        return this.synthetic;
    }

    public boolean contractOnly() {
        return this.contractOnly;
    }

    public boolean publicMojangOpaqueOnly() {
        return this.publicMojangOpaqueOnly;
    }

    public boolean publicMojangDepthView() {
        return this == PUBLIC_MOJANG_DEPTH_VIEW;
    }

    public boolean metadataOnly() {
        return this.metadataOnly;
    }

    public boolean missing() {
        return this.missing;
    }

    public String label() {
        return this.label;
    }
}
