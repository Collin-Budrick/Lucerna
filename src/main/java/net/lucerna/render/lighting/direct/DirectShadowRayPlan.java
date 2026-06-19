package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelRayBudgetConfig;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;

import java.util.List;
import java.util.Objects;

public record DirectShadowRayPlan(
        long frameIndex,
        long generation,
        List<DirectShadowRayCandidate> rayCandidates,
        VoxelRayBudgetConfig rayBudget,
        List<VoxelSectionSnapshotReference> sectionSnapshots,
        boolean requireOccupancyMasks,
        boolean allowTranslucentOccluders
) {
    public DirectShadowRayPlan {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        Objects.requireNonNull(rayCandidates, "rayCandidates");
        rayCandidates = List.copyOf(rayCandidates);
        for (DirectShadowRayCandidate rayCandidate : rayCandidates) {
            Objects.requireNonNull(rayCandidate, "rayCandidates must not contain null entries");
        }
        Objects.requireNonNull(rayBudget, "rayBudget");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        sectionSnapshots = List.copyOf(sectionSnapshots);
        for (VoxelSectionSnapshotReference sectionSnapshot : sectionSnapshots) {
            Objects.requireNonNull(sectionSnapshot, "sectionSnapshots must not contain null entries");
        }
    }

    public static DirectShadowRayPlan empty(long frameIndex) {
        return new DirectShadowRayPlan(
                frameIndex,
                0L,
                List.of(),
                new VoxelRayBudgetConfig(0, 1, 0, 1, 512, 64),
                List.of(),
                true,
                false
        );
    }

    public static DirectShadowRayPlan fromCandidates(
            long frameIndex,
            List<DirectShadowRayCandidate> rayCandidates,
            VoxelRayBudgetConfig rayBudget,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        Objects.requireNonNull(rayCandidates, "rayCandidates");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        long generation = Math.max(maxCandidateGeneration(rayCandidates), maxSectionGeneration(sectionSnapshots));
        return new DirectShadowRayPlan(
                frameIndex,
                generation,
                rayCandidates,
                rayBudget,
                sectionSnapshots,
                true,
                false
        );
    }

    public int candidateCount() {
        return this.rayCandidates.size();
    }

    public boolean hasRayCandidates() {
        return !this.rayCandidates.isEmpty();
    }

    public boolean hasSectionSnapshots() {
        return !this.sectionSnapshots.isEmpty();
    }

    public long maxCandidateGeneration() {
        return maxCandidateGeneration(this.rayCandidates);
    }

    public long maxSectionGeneration() {
        return maxSectionGeneration(this.sectionSnapshots);
    }

    public int budgetedCandidateCount() {
        return Math.min(this.candidateCount(), this.rayBudget.maxRaysPerFrame());
    }

    private static long maxCandidateGeneration(List<DirectShadowRayCandidate> rayCandidates) {
        Objects.requireNonNull(rayCandidates, "rayCandidates");
        return rayCandidates.stream()
                .filter(Objects::nonNull)
                .mapToLong(DirectShadowRayCandidate::sourceGeneration)
                .max()
                .orElse(0L);
    }

    private static long maxSectionGeneration(List<VoxelSectionSnapshotReference> sectionSnapshots) {
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        return sectionSnapshots.stream()
                .filter(Objects::nonNull)
                .mapToLong(VoxelSectionSnapshotReference::combinedGeneration)
                .max()
                .orElse(0L);
    }
}
