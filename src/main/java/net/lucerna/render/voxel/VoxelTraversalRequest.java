package net.lucerna.render.voxel;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record VoxelTraversalRequest(
        long requestGeneration,
        long frameIndex,
        VoxelTraversalPurpose purpose,
        VoxelRay ray,
        VoxelDdaConfig ddaConfig,
        VoxelRayBudgetConfig rayBudget,
        List<VoxelSectionSnapshotReference> sectionSnapshots,
        boolean requireOccupancyMasks,
        boolean requireMaterialPalette
) {
    public VoxelTraversalRequest {
        if (requestGeneration < 0) {
            throw new IllegalArgumentException("requestGeneration must be non-negative");
        }
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(ray, "ray");
        Objects.requireNonNull(ddaConfig, "ddaConfig");
        Objects.requireNonNull(rayBudget, "rayBudget");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        sectionSnapshots = List.copyOf(sectionSnapshots);

        Set<String> sectionKeys = new HashSet<>();
        for (VoxelSectionSnapshotReference sectionSnapshot : sectionSnapshots) {
            Objects.requireNonNull(sectionSnapshot, "sectionSnapshots must not contain null entries");
            if (!sectionKeys.add(sectionSnapshot.stableKey())) {
                throw new IllegalArgumentException("sectionSnapshots must be unique by section origin");
            }
        }
    }

    public static VoxelTraversalRequest primaryGBuffer(
            long frameIndex,
            VoxelRay ray,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        return new VoxelTraversalRequest(
                maxSnapshotGeneration(sectionSnapshots),
                frameIndex,
                VoxelTraversalPurpose.PRIMARY_GBUFFER,
                ray,
                VoxelDdaConfig.primaryGBuffer(),
                VoxelRayBudgetConfig.primaryGBuffer(1, 1),
                sectionSnapshots,
                true,
                true
        );
    }

    public int sectionCount() {
        return this.sectionSnapshots.size();
    }

    public boolean hasSectionSnapshots() {
        return !this.sectionSnapshots.isEmpty();
    }

    public long maxSnapshotGeneration() {
        return maxSnapshotGeneration(this.sectionSnapshots);
    }

    public boolean requiresSectionPayload() {
        return this.requireOccupancyMasks || this.requireMaterialPalette;
    }

    private static long maxSnapshotGeneration(List<VoxelSectionSnapshotReference> sectionSnapshots) {
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        return sectionSnapshots.stream()
                .filter(Objects::nonNull)
                .mapToLong(VoxelSectionSnapshotReference::combinedGeneration)
                .max()
                .orElse(0L);
    }
}
