package net.lucerna.render.cluster;

import java.util.Objects;

public record ChunkClusterVisibilityMetadata(
        ChunkClusterId id,
        boolean visiblePlaceholder,
        boolean uploadQueuedPlaceholder,
        int estimatedUploadBytes,
        long generation
) {
    public ChunkClusterVisibilityMetadata {
        Objects.requireNonNull(id, "id");
        if (estimatedUploadBytes < 0) {
            throw new IllegalArgumentException("estimatedUploadBytes must be non-negative");
        }
        generation = Math.max(0L, generation);
    }

    public static ChunkClusterVisibilityMetadata from(ChunkClusterMetadata cluster) {
        Objects.requireNonNull(cluster, "cluster");
        return new ChunkClusterVisibilityMetadata(
                cluster.id(),
                cluster.visibilityPlaceholder(),
                cluster.uploadPlaceholder(),
                cluster.estimatedUploadBytes(),
                cluster.sourceGeneration()
        );
    }
}
