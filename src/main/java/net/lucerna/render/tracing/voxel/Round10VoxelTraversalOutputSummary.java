package net.lucerna.render.tracing.voxel;

import java.util.Objects;

public record Round10VoxelTraversalOutputSummary(
        long generation,
        long rayCount,
        long hitCount,
        long missCount,
        long wallHitCount,
        long openSkyMissCount,
        long stepCount,
        double averageStepsPerRay,
        long skippedSectionCount,
        long materialHitCount,
        long glassWaterMaterialHitCount,
        long opaqueMaterialHitCount,
        String materialHitSource,
        String materialIdConsistency,
        boolean emptySectionSkipSafe,
        boolean maskBitsReady,
        String maskBitsSource,
        String traversalBackend,
        boolean realGpuTraversalExecuted,
        String boundary
) {
    public Round10VoxelTraversalOutputSummary {
        requireNonNegative(generation, "generation");
        requireNonNegative(rayCount, "rayCount");
        requireNonNegative(hitCount, "hitCount");
        requireNonNegative(missCount, "missCount");
        requireNonNegative(wallHitCount, "wallHitCount");
        requireNonNegative(openSkyMissCount, "openSkyMissCount");
        requireNonNegative(stepCount, "stepCount");
        if (!Double.isFinite(averageStepsPerRay) || averageStepsPerRay < 0.0D) {
            throw new IllegalArgumentException("averageStepsPerRay must be finite and non-negative");
        }
        requireNonNegative(skippedSectionCount, "skippedSectionCount");
        requireNonNegative(materialHitCount, "materialHitCount");
        requireNonNegative(glassWaterMaterialHitCount, "glassWaterMaterialHitCount");
        requireNonNegative(opaqueMaterialHitCount, "opaqueMaterialHitCount");
        materialHitSource = requireText(materialHitSource, "materialHitSource");
        materialIdConsistency = requireText(materialIdConsistency, "materialIdConsistency");
        maskBitsSource = requireText(maskBitsSource, "maskBitsSource");
        traversalBackend = requireText(traversalBackend, "traversalBackend");
        boundary = requireText(boundary, "boundary");
        if (hitCount + missCount > rayCount) {
            throw new IllegalArgumentException("hitCount plus missCount cannot exceed rayCount");
        }
        if (wallHitCount > hitCount) {
            throw new IllegalArgumentException("wallHitCount cannot exceed hitCount");
        }
        if (openSkyMissCount > missCount) {
            throw new IllegalArgumentException("openSkyMissCount cannot exceed missCount");
        }
        if (rayCount == 0 && stepCount != 0) {
            throw new IllegalArgumentException("stepCount requires at least one ray");
        }
        if (materialHitCount > hitCount) {
            throw new IllegalArgumentException("materialHitCount cannot exceed hitCount");
        }
        if (glassWaterMaterialHitCount + opaqueMaterialHitCount > materialHitCount) {
            throw new IllegalArgumentException(
                    "glassWaterMaterialHitCount plus opaqueMaterialHitCount cannot exceed materialHitCount"
            );
        }
        if (realGpuTraversalExecuted && traversalBackend.toLowerCase(java.util.Locale.ROOT).contains("cpu")) {
            throw new IllegalArgumentException("realGpuTraversalExecuted cannot use a CPU traversal backend label");
        }
    }

    public Round10VoxelTraversalOutputSummary(
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
        this(
                generation,
                rayCount,
                hitCount,
                missCount,
                0L,
                0L,
                stepCount,
                averageStepsPerRay,
                skippedSectionCount,
                materialHitCount,
                0L,
                0L,
                materialHitSource,
                "not_checked",
                true,
                false,
                "not_recorded",
                backend,
                false,
                boundary
        );
    }

    public static Round10VoxelTraversalOutputSummary notRun(long generation) {
        return new Round10VoxelTraversalOutputSummary(
                generation,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0.0D,
                0L,
                0L,
                0L,
                0L,
                "not_run",
                "not_checked",
                true,
                false,
                "not_uploaded",
                "not_run",
                false,
                "round10_voxel_traversal_not_evaluated"
        );
    }

    public boolean hasTraversalEvidence() {
        return this.rayCount > 0 && this.stepCount > 0 && (this.hitCount > 0 || this.missCount > 0);
    }

    public boolean hasMaterialHitEvidence() {
        return this.materialHitCount > 0 && !"not_run".equals(this.materialHitSource);
    }

    public boolean hasTerrainMaterialCorrectnessEvidence() {
        return this.wallHitCount > 0
                || this.openSkyMissCount > 0
                || this.glassWaterMaterialHitCount > 0
                || this.opaqueMaterialHitCount > 0
                || !"not_checked".equals(this.materialIdConsistency);
    }

    public String backend() {
        return this.traversalBackend;
    }

    public String correctnessLabel() {
        return "wallHitCount=" + this.wallHitCount
                + ",openSkyMissCount=" + this.openSkyMissCount
                + ",glassWaterMaterialHitCount=" + this.glassWaterMaterialHitCount
                + ",opaqueMaterialHitCount=" + this.opaqueMaterialHitCount
                + ",materialIdConsistency=" + this.materialIdConsistency
                + ",emptySectionSkipSafe=" + this.emptySectionSkipSafe
                + ",maskBitsReady=" + this.maskBitsReady
                + ",maskBitsSource=" + this.maskBitsSource
                + ",traversalBackend=" + this.traversalBackend
                + ",realGpuTraversalExecuted=" + this.realGpuTraversalExecuted
                + ",boundary=" + this.boundary;
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
