package net.lucerna.render.voxel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VoxelTraversalResult(
        long requestGeneration,
        VoxelTraversalStatus status,
        List<VoxelTraversalHit> hits,
        int visitedSectionCount,
        int visitedVoxelCount,
        int consumedRayCount,
        long sourceGeneration,
        List<String> diagnostics
) {
    public VoxelTraversalResult {
        requireNonNegative(requestGeneration, "requestGeneration");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(hits, "hits");
        hits = List.copyOf(hits);
        for (VoxelTraversalHit hit : hits) {
            Objects.requireNonNull(hit, "hits must not contain null entries");
        }
        requireNonNegative(visitedSectionCount, "visitedSectionCount");
        requireNonNegative(visitedVoxelCount, "visitedVoxelCount");
        requireNonNegative(consumedRayCount, "consumedRayCount");
        requireNonNegative(sourceGeneration, "sourceGeneration");
        Objects.requireNonNull(diagnostics, "diagnostics");
        diagnostics = diagnostics.stream()
                .map(diagnostic -> requireText(diagnostic, "diagnostics entries"))
                .toList();
        if (status == VoxelTraversalStatus.HIT && hits.isEmpty()) {
            throw new IllegalArgumentException("HIT traversal result requires at least one hit");
        }
    }

    public static VoxelTraversalResult notRun(long requestGeneration) {
        return new VoxelTraversalResult(
                requestGeneration,
                VoxelTraversalStatus.NOT_RUN,
                List.of(),
                0,
                0,
                0,
                0L,
                List.of()
        );
    }

    public static VoxelTraversalResult miss(
            long requestGeneration,
            int visitedSectionCount,
            int visitedVoxelCount,
            int consumedRayCount,
            long sourceGeneration
    ) {
        return new VoxelTraversalResult(
                requestGeneration,
                VoxelTraversalStatus.MISS,
                List.of(),
                visitedSectionCount,
                visitedVoxelCount,
                consumedRayCount,
                sourceGeneration,
                List.of()
        );
    }

    public static VoxelTraversalResult hit(
            long requestGeneration,
            VoxelTraversalHit hit,
            int visitedSectionCount,
            int visitedVoxelCount,
            int consumedRayCount
    ) {
        Objects.requireNonNull(hit, "hit");
        return new VoxelTraversalResult(
                requestGeneration,
                VoxelTraversalStatus.HIT,
                List.of(hit),
                visitedSectionCount,
                visitedVoxelCount,
                consumedRayCount,
                hit.combinedGeneration(),
                List.of()
        );
    }

    public boolean hasHit() {
        return !this.hits.isEmpty();
    }

    public Optional<VoxelTraversalHit> primaryHit() {
        return this.hits.stream().findFirst();
    }

    public boolean completed() {
        return this.status == VoxelTraversalStatus.HIT || this.status == VoxelTraversalStatus.MISS;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
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
