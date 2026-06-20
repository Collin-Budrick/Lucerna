package net.lucerna.render.cluster;

public enum ChunkClusterDensityComplexityBucket {
    EMPTY(0, "empty"),
    SPARSE(1, "sparse"),
    MODERATE(2, "moderate"),
    DENSE(3, "dense"),
    COMPLEX(4, "complex");

    private final int telemetryRank;
    private final String telemetryLabel;

    ChunkClusterDensityComplexityBucket(int telemetryRank, String telemetryLabel) {
        this.telemetryRank = telemetryRank;
        this.telemetryLabel = telemetryLabel;
    }

    public int telemetryRank() {
        return this.telemetryRank;
    }

    public String telemetryLabel() {
        return this.telemetryLabel;
    }

    public boolean atLeast(ChunkClusterDensityComplexityBucket other) {
        return this.telemetryRank >= other.telemetryRank;
    }
}
