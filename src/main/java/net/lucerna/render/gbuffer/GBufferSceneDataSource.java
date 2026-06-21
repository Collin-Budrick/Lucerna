package net.lucerna.render.gbuffer;

public record GBufferSceneDataSource(
        GBufferSceneDataSourceKind kind,
        String producer,
        String description
) {
    private static final String UNKNOWN_LABEL = "unknown";

    public GBufferSceneDataSource {
        if (kind == null) {
            kind = GBufferSceneDataSourceKind.UNKNOWN;
        }
        producer = normalizeLabel(producer);
        if (description == null || description.isBlank()) {
            description = kind.label();
        } else {
            description = description.trim();
        }
    }

    public static GBufferSceneDataSource unknown(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.UNKNOWN,
                UNKNOWN_LABEL,
                description
        );
    }

    public static GBufferSceneDataSource missing(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.MISSING,
                "missing",
                description
        );
    }

    public static GBufferSceneDataSource metadataOnly(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.METADATA_ONLY,
                "metadata",
                description
        );
    }

    public static GBufferSceneDataSource contractOnly(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.CONTRACT_ONLY,
                "contract",
                description
        );
    }

    public static GBufferSceneDataSource publicMojangOpaqueOnly(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.PUBLIC_MOJANG_OPAQUE_ONLY,
                "mojang-public-opaque",
                description
        );
    }

    public static GBufferSceneDataSource publicMojangDepthView(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.PUBLIC_MOJANG_DEPTH_VIEW,
                "mojang-public-depth-view",
                description
        );
    }

    public static GBufferSceneDataSource synthetic(String producer, String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.LUCERNA_SYNTHETIC,
                producer,
                description
        );
    }

    public static GBufferSceneDataSource mojangSampled(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.MINECRAFT_MOJANG_SAMPLED,
                "mojang",
                description
        );
    }

    public static GBufferSceneDataSource sodiumSampled(String description) {
        return new GBufferSceneDataSource(
                GBufferSceneDataSourceKind.MINECRAFT_SODIUM_SAMPLED,
                "sodium",
                description
        );
    }

    public boolean actualMinecraftSampled() {
        return this.kind.actualMinecraftSampled();
    }

    public boolean mojangSampled() {
        return this.kind.mojangSampled();
    }

    public boolean sodiumSampled() {
        return this.kind.sodiumSampled();
    }

    public boolean synthetic() {
        return this.kind.synthetic();
    }

    public boolean contractOnly() {
        return this.kind.contractOnly();
    }

    public boolean publicMojangOpaqueOnly() {
        return this.kind.publicMojangOpaqueOnly();
    }

    public boolean publicMojangDepthView() {
        return this.kind.publicMojangDepthView();
    }

    public boolean metadataOnly() {
        return this.kind.metadataOnly();
    }

    public boolean missing() {
        return this.kind.missing();
    }

    public String statusLabel() {
        return "kind=" + this.kind.label()
                + ", producer=" + this.producer
                + ", actualMinecraftSampled=" + actualMinecraftSampled()
                + ", mojangSampled=" + mojangSampled()
                + ", sodiumSampled=" + sodiumSampled()
                + ", synthetic=" + synthetic()
                + ", contractOnly=" + contractOnly()
                + ", publicMojangOpaqueOnly=" + publicMojangOpaqueOnly()
                + ", publicMojangDepthView=" + publicMojangDepthView()
                + ", metadataOnly=" + metadataOnly()
                + ", missing=" + missing()
                + ", description=" + this.description;
    }

    private static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_LABEL;
        }
        return value.trim();
    }
}
