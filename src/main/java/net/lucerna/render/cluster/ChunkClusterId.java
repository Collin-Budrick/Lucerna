package net.lucerna.render.cluster;

import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.Objects;

public record ChunkClusterId(
        ChunkSectionOrigin sectionOrigin,
        int clusterIndex
) {
    public ChunkClusterId {
        Objects.requireNonNull(sectionOrigin, "sectionOrigin");
        if (clusterIndex < 0) {
            throw new IllegalArgumentException("clusterIndex must be non-negative");
        }
    }

    public static ChunkClusterId sectionCluster(ChunkSectionOrigin sectionOrigin) {
        return new ChunkClusterId(sectionOrigin, 0);
    }

    public String stableKey() {
        return this.sectionOrigin.stableKey() + "#cluster=" + this.clusterIndex;
    }
}
