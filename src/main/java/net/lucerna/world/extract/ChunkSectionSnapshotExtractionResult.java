package net.lucerna.world.extract;

import net.lucerna.world.DirtyRegionSnapshot;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;

import java.util.List;
import java.util.Objects;

public record ChunkSectionSnapshotExtractionResult(
        DirtyRegionSnapshot dirtyRegionSnapshot,
        List<ChunkSectionSnapshotHandoff> sectionSnapshots,
        List<ChunkSectionSnapshotSkip> skippedSections,
        int cachedSectionCount
) {
    public ChunkSectionSnapshotExtractionResult {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        Objects.requireNonNull(skippedSections, "skippedSections");
        sectionSnapshots = List.copyOf(sectionSnapshots);
        skippedSections = List.copyOf(skippedSections);

        if (cachedSectionCount < 0) {
            throw new IllegalArgumentException("cachedSectionCount must be non-negative");
        }

        int handledRegionCount = sectionSnapshots.size() + skippedSections.size();
        if (handledRegionCount != dirtyRegionSnapshot.batch().dirtyRegionCount()) {
            throw new IllegalArgumentException("all coalesced dirty regions must be represented");
        }
    }

    public static ChunkSectionSnapshotExtractionResult empty(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            int cachedSectionCount
    ) {
        return new ChunkSectionSnapshotExtractionResult(
                dirtyRegionSnapshot,
                List.of(),
                List.of(),
                cachedSectionCount
        );
    }

    public List<ChunkSectionVoxelSnapshot> snapshots() {
        return this.sectionSnapshots.stream()
                .map(ChunkSectionSnapshotHandoff::snapshot)
                .toList();
    }

    public int extractedSectionCount() {
        return this.sectionSnapshots.size();
    }

    public int skippedSectionCount() {
        return this.skippedSections.size();
    }

    public boolean hasExtractedSections() {
        return !this.sectionSnapshots.isEmpty();
    }

    public boolean hasSkippedSections() {
        return !this.skippedSections.isEmpty();
    }
}
