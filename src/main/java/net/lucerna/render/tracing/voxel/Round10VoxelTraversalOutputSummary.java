package net.lucerna.render.tracing.voxel;

import java.util.Objects;

public record Round10VoxelTraversalOutputSummary(
        long generation,
        long rayCount,
        long hitCount,
        long missCount,
        long stepCount,
        double averageStepsPerRay,
        long skippedSectionCount,
        long materialHitCount,
        String materialHitSource,
        String backend,
        String boundary
) {
    public Round10VoxelTraversalOutputSummary {
        requireNonNegative(generation, "generation");
        requireNonNegative(rayCount, "rayCount");
        requireNonNegative(hitCount, "hitCount");
        requireNonNegative(missCount, "missCount");
        requireNonNegative(stepCount, "stepCount");
        if (!Double.isFinite(averageStepsPerRay) || averageStepsPerRay < 0.0D) {
            throw new IllegalArgumentException("averageStepsPerRay must be finite and non-negative");
        }
        requireNonNegative(skippedSectionCount, "skippedSectionCount");
        requireNonNegative(materialHitCount, "materialHitCount");
        materialHitSource = requireText(materialHitSource, "materialHitSource");
        backend = requireText(backend, "backend");
        boundary = requireText(boundary, "boundary");
        if (hitCount + missCount > rayCount) {
            throw new IllegalArgumentException("hitCount plus missCount cannot exceed rayCount");
        }
        if (rayCount == 0 && stepCount != 0) {
            throw new IllegalArgumentException("stepCount requires at least one ray");
        }
        if (materialHitCount > hitCount) {
            throw new IllegalArgumentException("materialHitCount cannot exceed hitCount");
        }
    }

    public static Round10VoxelTraversalOutputSummary notRun(long generation) {
        return new Round10VoxelTraversalOutputSummary(
                generation,
                0L,
                0L,
                0L,
                0L,
                0.0D,
                0L,
                0L,
                "not_run",
                "not_run",
                "round10_voxel_traversal_not_evaluated"
        );
    }

    public boolean hasTraversalEvidence() {
        return this.rayCount > 0 && this.stepCount > 0 && (this.hitCount > 0 || this.missCount > 0);
    }

    public boolean hasMaterialHitEvidence() {
        return this.materialHitCount > 0 && !"not_run".equals(this.materialHitSource);
    }

    private static void requireNonNegative(long value, String name) {
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
