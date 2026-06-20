package net.lucerna.render.voxel;

import net.lucerna.world.section.ChunkSectionLifecycleStatus;

import java.util.List;
import java.util.Objects;

public record VoxelSectionLifecycleStatus(
        int sectionSnapshotCount,
        ChunkSectionLifecycleStatus lifecycle,
        String source
) {
    public VoxelSectionLifecycleStatus {
        requireNonNegative(sectionSnapshotCount, "sectionSnapshotCount");
        Objects.requireNonNull(lifecycle, "lifecycle");
        source = requireText(source, "source");
    }

    public static VoxelSectionLifecycleStatus fromSectionSnapshots(List<VoxelSectionSnapshotReference> sections) {
        Objects.requireNonNull(sections, "sections");

        long observedGeneration = 0L;
        for (VoxelSectionSnapshotReference section : sections) {
            Objects.requireNonNull(section, "sections must not contain null entries");
            long generation = section.combinedGeneration();
            observedGeneration = Math.max(observedGeneration, generation);
        }

        return observed(
                sections.size(),
                observedGeneration,
                0,
                false,
                false,
                "voxel_section_snapshot_references"
        );
    }

    public static VoxelSectionLifecycleStatus fromObservedChurn(
            List<VoxelSectionSnapshotReference> sections,
            int changedSectionCount,
            boolean sectionLoadObserved,
            boolean sectionUnloadObserved
    ) {
        Objects.requireNonNull(sections, "sections");

        long observedGeneration = 0L;
        for (VoxelSectionSnapshotReference section : sections) {
            Objects.requireNonNull(section, "sections must not contain null entries");
            observedGeneration = Math.max(observedGeneration, section.combinedGeneration());
        }

        return observed(
                sections.size(),
                observedGeneration,
                changedSectionCount,
                sectionLoadObserved,
                sectionUnloadObserved,
                "voxel_section_lifecycle_churn_observer"
        );
    }

    public static VoxelSectionLifecycleStatus observed(
            int sectionSnapshotCount,
            long observedSnapshotGeneration,
            int changedSectionCount,
            boolean sectionLoadObserved,
            boolean sectionUnloadObserved,
            String source
    ) {
        return new VoxelSectionLifecycleStatus(
                sectionSnapshotCount,
                ChunkSectionLifecycleStatus.observed(
                        observedSnapshotGeneration,
                        changedSectionCount,
                        sectionLoadObserved,
                        sectionUnloadObserved
                ),
                source
        );
    }

    public long observedSnapshotGeneration() {
        return this.lifecycle.observedSnapshotGeneration();
    }

    public boolean sectionSnapshotGenerationObserved() {
        return this.lifecycle.sectionSnapshotGenerationObserved();
    }

    public int changedSectionCount() {
        return this.lifecycle.changedSectionCount();
    }

    public boolean chunkChurnObserved() {
        return this.lifecycle.chunkChurnObserved();
    }

    public boolean sectionLoadObserved() {
        return this.lifecycle.sectionLoadObserved();
    }

    public boolean sectionUnloadObserved() {
        return this.lifecycle.sectionUnloadObserved();
    }

    public boolean realGpuTraversalClaimed() {
        return this.lifecycle.realGpuTraversalClaimed();
    }

    public String marker() {
        return this.lifecycle.marker();
    }

    public String traversalBoundaryLabel() {
        return this.lifecycle.traversalBoundaryLabel();
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
