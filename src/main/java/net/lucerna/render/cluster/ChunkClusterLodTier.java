package net.lucerna.render.cluster;

public enum ChunkClusterLodTier {
    SECTION_COARSE(16, 1, "section-coarse"),
    HALF_SECTION(8, 8, "half-section-meshlet"),
    QUARTER_SECTION(4, 64, "quarter-section-meshlet");

    private final int edgeLength;
    private final int clustersPerSection;
    private final String telemetryLabel;

    ChunkClusterLodTier(int edgeLength, int clustersPerSection, String telemetryLabel) {
        this.edgeLength = edgeLength;
        this.clustersPerSection = clustersPerSection;
        this.telemetryLabel = telemetryLabel;
    }

    public int edgeLength() {
        return this.edgeLength;
    }

    public int clustersPerSection() {
        return this.clustersPerSection;
    }

    public String telemetryLabel() {
        return this.telemetryLabel;
    }
}
