package net.lucerna.render.gbuffer;

public enum GBufferSceneDataSourceKind {
    UNKNOWN(false, false, false, false, false, "unknown"),
    CONTRACT_ONLY(false, false, false, false, true, "contract-only"),
    LUCERNA_SYNTHETIC(false, false, false, true, false, "lucerna-synthetic"),
    MINECRAFT_MOJANG_SAMPLED(true, true, false, false, false, "minecraft-mojang-sampled"),
    MINECRAFT_SODIUM_SAMPLED(true, false, true, false, false, "minecraft-sodium-sampled");

    private final boolean actualMinecraftSampled;
    private final boolean mojangSampled;
    private final boolean sodiumSampled;
    private final boolean synthetic;
    private final boolean contractOnly;
    private final String label;

    GBufferSceneDataSourceKind(
            boolean actualMinecraftSampled,
            boolean mojangSampled,
            boolean sodiumSampled,
            boolean synthetic,
            boolean contractOnly,
            String label
    ) {
        this.actualMinecraftSampled = actualMinecraftSampled;
        this.mojangSampled = mojangSampled;
        this.sodiumSampled = sodiumSampled;
        this.synthetic = synthetic;
        this.contractOnly = contractOnly;
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

    public String label() {
        return this.label;
    }
}
