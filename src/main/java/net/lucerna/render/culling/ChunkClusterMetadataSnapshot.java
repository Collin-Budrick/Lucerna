package net.lucerna.render.culling;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public record ChunkClusterMetadataSnapshot(
        long generation,
        long minClusterGeneration,
        long maxClusterGeneration,
        int clusterCount,
        int sectionCount,
        int uploadBytes,
        List<ChunkClusterMetadata> clusters
) {
    public ChunkClusterMetadataSnapshot {
        clusters = immutable(clusters);
        if (generation < 0L || minClusterGeneration < 0L || maxClusterGeneration < 0L) {
            throw new IllegalArgumentException("cluster generations must be non-negative");
        }
        if (clusterCount < 0 || sectionCount < 0 || uploadBytes < 0) {
            throw new IllegalArgumentException("cluster snapshot counters must be non-negative");
        }
        if (!clusters.isEmpty() && clusterCount != clusters.size()) {
            throw new IllegalArgumentException("clusterCount must match clusters size when clusters are supplied");
        }
    }

    public static ChunkClusterMetadataSnapshot empty(long generation) {
        return new ChunkClusterMetadataSnapshot(generation, 0L, 0L, 0, 0, 0, List.of());
    }

    public static ChunkClusterMetadataSnapshot fromClusters(long generation, List<ChunkClusterMetadata> clusters) {
        List<ChunkClusterMetadata> copy = immutable(clusters);
        long minGeneration = copy.stream()
                .min(Comparator.comparingLong(ChunkClusterMetadata::generation))
                .map(ChunkClusterMetadata::generation)
                .orElse(0L);
        long maxGeneration = copy.stream()
                .max(Comparator.comparingLong(ChunkClusterMetadata::generation))
                .map(ChunkClusterMetadata::generation)
                .orElse(0L);
        int uploadBytes = copy.stream().mapToInt(ChunkClusterMetadata::uploadBytes).sum();
        int sectionCount = (int) copy.stream().map(ChunkClusterMetadata::sectionKey).distinct().count();
        return new ChunkClusterMetadataSnapshot(
                generation,
                minGeneration,
                maxGeneration,
                copy.size(),
                sectionCount,
                uploadBytes,
                copy
        );
    }

    public String generationSummary() {
        if (this.clusterCount == 0) {
            return "snapshotGen=" + this.generation + " clusters=0";
        }
        return "snapshotGen=" + this.generation
                + " clusterGen=" + this.minClusterGeneration + ".." + this.maxClusterGeneration;
    }

    private static List<ChunkClusterMetadata> immutable(List<ChunkClusterMetadata> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(List.copyOf(source));
    }
}
