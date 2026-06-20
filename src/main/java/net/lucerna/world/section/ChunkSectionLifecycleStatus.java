package net.lucerna.world.section;

import java.util.Objects;

public record ChunkSectionLifecycleStatus(
        long observedSnapshotGeneration,
        int changedSectionCount,
        boolean chunkChurnObserved,
        boolean sectionLoadObserved,
        boolean sectionUnloadObserved,
        boolean realGpuTraversalClaimed,
        String marker
) {
    public static final String NO_CHURN_MARKER = "round10_section_lifecycle_no_churn_observed";
    public static final String CHURN_MARKER = "round10_chunk_churn_section_lifecycle_observed";

    public ChunkSectionLifecycleStatus {
        requireNonNegative(observedSnapshotGeneration, "observedSnapshotGeneration");
        requireNonNegative(changedSectionCount, "changedSectionCount");
        marker = requireText(marker, "marker");
        if (realGpuTraversalClaimed) {
            throw new IllegalArgumentException("chunk section lifecycle status must not claim real GPU traversal");
        }
        if (changedSectionCount > 0 && !chunkChurnObserved) {
            throw new IllegalArgumentException("changedSectionCount requires chunkChurnObserved");
        }
        if ((sectionLoadObserved || sectionUnloadObserved) && !chunkChurnObserved) {
            throw new IllegalArgumentException("section load/unload markers require chunkChurnObserved");
        }
    }

    public static ChunkSectionLifecycleStatus notObserved() {
        return new ChunkSectionLifecycleStatus(
                0L,
                0,
                false,
                false,
                false,
                false,
                NO_CHURN_MARKER
        );
    }

    public static ChunkSectionLifecycleStatus observed(
            long observedSnapshotGeneration,
            int changedSectionCount,
            boolean sectionLoadObserved,
            boolean sectionUnloadObserved
    ) {
        boolean chunkChurnObserved = changedSectionCount > 0 || sectionLoadObserved || sectionUnloadObserved;
        return new ChunkSectionLifecycleStatus(
                observedSnapshotGeneration,
                changedSectionCount,
                chunkChurnObserved,
                sectionLoadObserved,
                sectionUnloadObserved,
                false,
                chunkChurnObserved ? CHURN_MARKER : NO_CHURN_MARKER
        );
    }

    public boolean sectionSnapshotGenerationObserved() {
        return this.observedSnapshotGeneration > 0L;
    }

    public boolean hasLifecycleEvidence() {
        return this.sectionSnapshotGenerationObserved()
                || this.changedSectionCount > 0
                || this.chunkChurnObserved
                || this.sectionLoadObserved
                || this.sectionUnloadObserved;
    }

    public String traversalBoundaryLabel() {
        return this.realGpuTraversalClaimed
                ? "invalid_real_gpu_traversal_claim"
                : "java_section_lifecycle_status_only_no_gpu_traversal_claim";
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
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
