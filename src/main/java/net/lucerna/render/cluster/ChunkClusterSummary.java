package net.lucerna.render.cluster;

import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.Objects;

public record ChunkClusterSummary(
        ChunkSectionOrigin sectionOrigin,
        ChunkSectionGeneration sectionGeneration,
        int clusterCount,
        int visibleClusterPlaceholderCount,
        int totalClusterPlaceholderCount,
        int uploadByteEstimate,
        int occupiedVoxelCount,
        int surfaceSampleCount,
        long combinedGeneration
) {
    public ChunkClusterSummary {
        Objects.requireNonNull(sectionOrigin, "sectionOrigin");
        Objects.requireNonNull(sectionGeneration, "sectionGeneration");
        clusterCount = requireNonNegative(clusterCount, "clusterCount");
        visibleClusterPlaceholderCount = requireNonNegative(visibleClusterPlaceholderCount, "visibleClusterPlaceholderCount");
        totalClusterPlaceholderCount = requireNonNegative(totalClusterPlaceholderCount, "totalClusterPlaceholderCount");
        uploadByteEstimate = requireNonNegative(uploadByteEstimate, "uploadByteEstimate");
        occupiedVoxelCount = requireNonNegative(occupiedVoxelCount, "occupiedVoxelCount");
        surfaceSampleCount = requireNonNegative(surfaceSampleCount, "surfaceSampleCount");
        combinedGeneration = Math.max(0L, combinedGeneration);
        if (visibleClusterPlaceholderCount > totalClusterPlaceholderCount) {
            throw new IllegalArgumentException("visible placeholder count cannot exceed total placeholder count");
        }
        if (totalClusterPlaceholderCount > clusterCount) {
            throw new IllegalArgumentException("total placeholder count cannot exceed clusterCount");
        }
    }

    public static ChunkClusterSummary empty(ChunkSectionOrigin sectionOrigin) {
        return new ChunkClusterSummary(
                sectionOrigin,
                ChunkSectionGeneration.empty(),
                0,
                0,
                0,
                0,
                0,
                0,
                0L
        );
    }

    public String stableSectionKey() {
        return this.sectionOrigin.stableKey();
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }
}
