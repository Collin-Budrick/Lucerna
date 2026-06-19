package net.lucerna.render.cluster;

import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionEmissiveEntryMetadata;
import net.lucerna.world.section.SectionSurfaceSampleMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChunkClusterMetadataBuilder {
    private static final int COARSE_CLUSTER_EDGE = ChunkClusterLodTier.HALF_SECTION.edgeLength();
    private static final int CLUSTER_AXIS_COUNT = ChunkSectionOrigin.SECTION_EDGE_LENGTH / COARSE_CLUSTER_EDGE;

    private ChunkClusterMetadataBuilder() {
    }

    public static ChunkClusterUploadPacketMetadata fromSnapshot(ChunkSectionVoxelSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return build(
                snapshot.origin(),
                snapshot.generation(),
                snapshot.occupancySummary().occupiedVoxelCount(),
                snapshot.occupancySummary().opaqueVoxelCount(),
                snapshot.occupancySummary().translucentVoxelCount(),
                snapshot.occupancySummary().fluidVoxelCount(),
                snapshot.occupancySummary().emissiveVoxelCount(),
                snapshot.materialPalette().paletteSize(),
                snapshot.surfaceSamples(),
                snapshot.emissiveEntries()
        );
    }

    public static ChunkClusterUploadPacketMetadata fromReference(VoxelSectionSnapshotReference reference) {
        Objects.requireNonNull(reference, "reference");
        return build(
                reference.origin(),
                reference.generation(),
                reference.occupiedVoxelCount(),
                reference.opaqueVoxelCount(),
                reference.translucentVoxelCount(),
                reference.fluidVoxelCount(),
                reference.emissiveVoxelCount(),
                reference.materialPaletteSize(),
                reference.surfaceSamples(),
                List.of()
        );
    }

    public static List<ChunkClusterUploadPacketMetadata> fromSnapshots(List<ChunkSectionVoxelSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots");
        return snapshots.stream()
                .map(ChunkClusterMetadataBuilder::fromSnapshot)
                .toList();
    }

    public static List<ChunkClusterUploadPacketMetadata> fromReferences(List<VoxelSectionSnapshotReference> references) {
        Objects.requireNonNull(references, "references");
        return references.stream()
                .map(ChunkClusterMetadataBuilder::fromReference)
                .toList();
    }

    private static ChunkClusterUploadPacketMetadata build(
            ChunkSectionOrigin origin,
            ChunkSectionGeneration generation,
            int occupiedVoxelCount,
            int opaqueVoxelCount,
            int translucentVoxelCount,
            int fluidVoxelCount,
            int emissiveVoxelCount,
            int materialPaletteSize,
            List<SectionSurfaceSampleMetadata> surfaceSamples,
            List<SectionEmissiveEntryMetadata> emissiveEntries
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(surfaceSamples, "surfaceSamples");
        Objects.requireNonNull(emissiveEntries, "emissiveEntries");
        if (occupiedVoxelCount == 0 && surfaceSamples.isEmpty()) {
            return ChunkClusterUploadPacketMetadata.empty(origin);
        }

        List<ChunkClusterMetadata> clusters = surfaceSamples.isEmpty()
                ? List.of(sectionLevelCluster(
                origin,
                generation,
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount,
                materialPaletteSize
        ))
                : sampleClusters(
                origin,
                generation,
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                materialPaletteSize,
                surfaceSamples,
                emissiveEntries
        );

        List<ChunkClusterVisibilityMetadata> visibility = clusters.stream()
                .map(ChunkClusterVisibilityMetadata::from)
                .toList();
        ChunkClusterSummary summary = summarize(origin, generation, clusters);
        return new ChunkClusterUploadPacketMetadata(origin, clusters, visibility, summary);
    }

    private static ChunkClusterMetadata sectionLevelCluster(
            ChunkSectionOrigin origin,
            ChunkSectionGeneration generation,
            int occupiedVoxelCount,
            int opaqueVoxelCount,
            int translucentVoxelCount,
            int fluidVoxelCount,
            int emissiveVoxelCount,
            int materialPaletteSize
    ) {
        return new ChunkClusterMetadata(
                ChunkClusterId.sectionCluster(origin),
                ChunkClusterLocalBounds.wholeSection(),
                ChunkClusterLodTier.SECTION_COARSE,
                generation,
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount,
                0,
                materialPaletteSize,
                generation.combinedGeneration(),
                true,
                true
        );
    }

    private static List<ChunkClusterMetadata> sampleClusters(
            ChunkSectionOrigin origin,
            ChunkSectionGeneration generation,
            int occupiedVoxelCount,
            int opaqueVoxelCount,
            int translucentVoxelCount,
            int fluidVoxelCount,
            int materialPaletteSize,
            List<SectionSurfaceSampleMetadata> surfaceSamples,
            List<SectionEmissiveEntryMetadata> emissiveEntries
    ) {
        Map<Integer, MutableClusterStats> statsByIndex = new HashMap<>();
        for (SectionSurfaceSampleMetadata sample : surfaceSamples) {
            Objects.requireNonNull(sample, "surfaceSamples must not contain null entries");
            int clusterIndex = clusterIndex(sample.localX(), sample.localY(), sample.localZ());
            MutableClusterStats stats = statsByIndex.computeIfAbsent(clusterIndex, MutableClusterStats::new);
            stats.surfaceSampleCount++;
            stats.sourceGeneration = Math.max(stats.sourceGeneration, sample.generation());
        }
        for (SectionEmissiveEntryMetadata emissiveEntry : emissiveEntries) {
            Objects.requireNonNull(emissiveEntry, "emissiveEntries must not contain null entries");
            int clusterIndex = clusterIndex(emissiveEntry.localX(), emissiveEntry.localY(), emissiveEntry.localZ());
            MutableClusterStats stats = statsByIndex.computeIfAbsent(clusterIndex, MutableClusterStats::new);
            stats.emissiveVoxelCount++;
            stats.sourceGeneration = Math.max(stats.sourceGeneration, emissiveEntry.generation());
        }

        List<ChunkClusterMetadata> clusters = new ArrayList<>();
        int totalSamples = Math.max(1, surfaceSamples.size());
        for (MutableClusterStats stats : statsByIndex.values()) {
            ChunkClusterLocalBounds bounds = localBounds(stats.clusterIndex);
            int clusterCapacity = bounds.estimatedVoxelCapacity();
            int representativeOccupiedCount = Math.max(
                    proportionalCount(occupiedVoxelCount, stats.surfaceSampleCount, totalSamples),
                    stats.emissiveVoxelCount
            );
            representativeOccupiedCount = Math.min(clusterCapacity, representativeOccupiedCount);
            int representativeOpaqueCount = Math.min(
                    representativeOccupiedCount,
                    proportionalCount(opaqueVoxelCount, stats.surfaceSampleCount, totalSamples)
            );
            int representativeTranslucentCount = Math.min(
                    representativeOccupiedCount - representativeOpaqueCount,
                    proportionalCount(translucentVoxelCount, stats.surfaceSampleCount, totalSamples)
            );
            int representativeFluidCount = Math.min(
                    representativeOccupiedCount,
                    proportionalCount(fluidVoxelCount, stats.surfaceSampleCount, totalSamples)
            );
            clusters.add(new ChunkClusterMetadata(
                    new ChunkClusterId(origin, stats.clusterIndex),
                    bounds,
                    ChunkClusterLodTier.HALF_SECTION,
                    generation,
                    representativeOccupiedCount,
                    representativeOpaqueCount,
                    representativeTranslucentCount,
                    representativeFluidCount,
                    Math.min(representativeOccupiedCount, stats.emissiveVoxelCount),
                    stats.surfaceSampleCount,
                    materialPaletteSize,
                    Math.max(generation.combinedGeneration(), stats.sourceGeneration),
                    true,
                    true
            ));
        }
        clusters.sort(Comparator.comparingInt(cluster -> cluster.id().clusterIndex()));
        return List.copyOf(clusters);
    }

    private static ChunkClusterSummary summarize(
            ChunkSectionOrigin origin,
            ChunkSectionGeneration generation,
            List<ChunkClusterMetadata> clusters
    ) {
        int visiblePlaceholderCount = 0;
        int totalPlaceholderCount = 0;
        int uploadByteEstimate = 0;
        int occupiedVoxelCount = 0;
        int surfaceSampleCount = 0;
        long combinedGeneration = generation.combinedGeneration();
        for (ChunkClusterMetadata cluster : clusters) {
            if (cluster.visibilityPlaceholder()) {
                visiblePlaceholderCount++;
            }
            if (cluster.visibilityPlaceholder() || cluster.uploadPlaceholder()) {
                totalPlaceholderCount++;
            }
            uploadByteEstimate += cluster.estimatedUploadBytes();
            occupiedVoxelCount += cluster.occupiedVoxelCount();
            surfaceSampleCount += cluster.surfaceSampleCount();
            combinedGeneration = Math.max(combinedGeneration, cluster.sourceGeneration());
        }
        return new ChunkClusterSummary(
                origin,
                generation,
                clusters.size(),
                visiblePlaceholderCount,
                totalPlaceholderCount,
                uploadByteEstimate,
                occupiedVoxelCount,
                surfaceSampleCount,
                combinedGeneration
        );
    }

    private static int clusterIndex(int localX, int localY, int localZ) {
        int x = localX / COARSE_CLUSTER_EDGE;
        int y = localY / COARSE_CLUSTER_EDGE;
        int z = localZ / COARSE_CLUSTER_EDGE;
        return (y * CLUSTER_AXIS_COUNT + z) * CLUSTER_AXIS_COUNT + x;
    }

    private static ChunkClusterLocalBounds localBounds(int clusterIndex) {
        int x = clusterIndex % CLUSTER_AXIS_COUNT;
        int z = (clusterIndex / CLUSTER_AXIS_COUNT) % CLUSTER_AXIS_COUNT;
        int y = clusterIndex / (CLUSTER_AXIS_COUNT * CLUSTER_AXIS_COUNT);
        int minX = x * COARSE_CLUSTER_EDGE;
        int minY = y * COARSE_CLUSTER_EDGE;
        int minZ = z * COARSE_CLUSTER_EDGE;
        return new ChunkClusterLocalBounds(
                minX,
                minY,
                minZ,
                minX + COARSE_CLUSTER_EDGE - 1,
                minY + COARSE_CLUSTER_EDGE - 1,
                minZ + COARSE_CLUSTER_EDGE - 1
        );
    }

    private static int proportionalCount(int totalCount, int sampleCount, int totalSamples) {
        if (totalCount <= 0 || sampleCount <= 0 || totalSamples <= 0) {
            return 0;
        }
        return Math.max(1, Math.round((float) totalCount * sampleCount / totalSamples));
    }

    private static final class MutableClusterStats {
        private final int clusterIndex;
        private int surfaceSampleCount;
        private int emissiveVoxelCount;
        private long sourceGeneration;

        private MutableClusterStats(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }
    }
}
