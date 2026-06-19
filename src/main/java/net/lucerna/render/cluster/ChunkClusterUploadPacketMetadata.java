package net.lucerna.render.cluster;

import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ChunkClusterUploadPacketMetadata(
        ChunkSectionOrigin sectionOrigin,
        List<ChunkClusterMetadata> clusters,
        List<ChunkClusterVisibilityMetadata> visibility,
        ChunkClusterSummary summary
) {
    public ChunkClusterUploadPacketMetadata {
        Objects.requireNonNull(sectionOrigin, "sectionOrigin");
        Objects.requireNonNull(clusters, "clusters");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(summary, "summary");
        clusters = List.copyOf(clusters);
        visibility = List.copyOf(visibility);
        validateClusters(sectionOrigin, clusters);
        validateVisibility(clusters, visibility);
        if (!summary.sectionOrigin().equals(sectionOrigin)) {
            throw new IllegalArgumentException("summary section origin must match upload packet section origin");
        }
        if (summary.clusterCount() != clusters.size()) {
            throw new IllegalArgumentException("summary clusterCount must match upload packet clusters");
        }
    }

    public static ChunkClusterUploadPacketMetadata empty(ChunkSectionOrigin sectionOrigin) {
        return new ChunkClusterUploadPacketMetadata(
                sectionOrigin,
                List.of(),
                List.of(),
                ChunkClusterSummary.empty(sectionOrigin)
        );
    }

    public int clusterCount() {
        return this.clusters.size();
    }

    public int uploadByteEstimate() {
        return this.summary.uploadByteEstimate();
    }

    public boolean hasClusterPayload() {
        return !this.clusters.isEmpty();
    }

    private static void validateClusters(ChunkSectionOrigin sectionOrigin, List<ChunkClusterMetadata> clusters) {
        Set<String> keys = new HashSet<>();
        for (ChunkClusterMetadata cluster : clusters) {
            Objects.requireNonNull(cluster, "clusters must not contain null entries");
            if (!cluster.id().sectionOrigin().equals(sectionOrigin)) {
                throw new IllegalArgumentException("cluster section origin must match upload packet section origin");
            }
            if (!keys.add(cluster.id().stableKey())) {
                throw new IllegalArgumentException("cluster ids must be unique within a section upload packet");
            }
        }
    }

    private static void validateVisibility(
            List<ChunkClusterMetadata> clusters,
            List<ChunkClusterVisibilityMetadata> visibility
    ) {
        if (visibility.size() != clusters.size()) {
            throw new IllegalArgumentException("visibility metadata count must match cluster count");
        }
        Set<String> clusterKeys = new HashSet<>();
        for (ChunkClusterMetadata cluster : clusters) {
            clusterKeys.add(cluster.id().stableKey());
        }
        for (ChunkClusterVisibilityMetadata visibleCluster : visibility) {
            Objects.requireNonNull(visibleCluster, "visibility must not contain null entries");
            if (!clusterKeys.contains(visibleCluster.id().stableKey())) {
                throw new IllegalArgumentException("visibility metadata must reference a cluster in this packet");
            }
        }
    }
}
