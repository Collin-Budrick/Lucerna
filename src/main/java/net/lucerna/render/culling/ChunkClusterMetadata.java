package net.lucerna.render.culling;

public record ChunkClusterMetadata(
        long clusterId,
        int chunkX,
        int sectionY,
        int chunkZ,
        int localClusterIndex,
        ClusterBounds bounds,
        int primitiveCount,
        int materialSpanCount,
        long generation,
        int uploadBytes,
        boolean occlusionCandidate
) {
    public ChunkClusterMetadata {
        if (clusterId < 0L) {
            throw new IllegalArgumentException("clusterId must be non-negative");
        }
        if (localClusterIndex < 0) {
            throw new IllegalArgumentException("localClusterIndex must be non-negative");
        }
        if (primitiveCount < 0 || materialSpanCount < 0 || generation < 0L || uploadBytes < 0) {
            throw new IllegalArgumentException("cluster metadata counters must be non-negative");
        }
    }

    public String sectionKey() {
        return this.chunkX + "," + this.sectionY + "," + this.chunkZ;
    }

    public boolean hasBounds() {
        return this.bounds != null;
    }
}
