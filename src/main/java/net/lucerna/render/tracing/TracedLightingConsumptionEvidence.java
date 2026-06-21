package net.lucerna.render.tracing;

import net.lucerna.render.tracing.voxel.Round10VoxelTraversalOutputSummary;
import net.lucerna.render.tracing.voxel.Round10VoxelTraversalStatus;

import java.util.Objects;

public record TracedLightingConsumptionEvidence(
        long generation,
        long rayCount,
        long hitCount,
        long missCount,
        long materialCoupledHitCount,
        long depthCoupledHitCount,
        long sourceCoupledBounceCount,
        long cacheReadCount,
        long cacheWriteCount,
        boolean finalGiSourceConsumed,
        boolean realGpuTraversalConsumed,
        String finalGiSource,
        String evidenceSource,
        String boundary
) {
    public TracedLightingConsumptionEvidence {
        requireNonNegative(generation, "generation");
        requireNonNegative(rayCount, "rayCount");
        requireNonNegative(hitCount, "hitCount");
        requireNonNegative(missCount, "missCount");
        requireNonNegative(materialCoupledHitCount, "materialCoupledHitCount");
        requireNonNegative(depthCoupledHitCount, "depthCoupledHitCount");
        requireNonNegative(sourceCoupledBounceCount, "sourceCoupledBounceCount");
        requireNonNegative(cacheReadCount, "cacheReadCount");
        requireNonNegative(cacheWriteCount, "cacheWriteCount");
        finalGiSource = clean(finalGiSource, finalGiSourceConsumed ? "final_gi_source" : "not_consumed");
        evidenceSource = clean(evidenceSource, "trace_consumption_evidence_unavailable");
        boundary = clean(boundary, "traced_lighting_consumption_not_proven");
        if (hitCount > rayCount || missCount > rayCount - hitCount) {
            throw new IllegalArgumentException("hitCount plus missCount cannot exceed rayCount");
        }
        if (materialCoupledHitCount > hitCount) {
            throw new IllegalArgumentException("materialCoupledHitCount cannot exceed hitCount");
        }
        if (depthCoupledHitCount > hitCount) {
            throw new IllegalArgumentException("depthCoupledHitCount cannot exceed hitCount");
        }
        if (finalGiSourceConsumed && !hasConsumptionEvidence(
                rayCount,
                hitCount,
                materialCoupledHitCount,
                depthCoupledHitCount,
                sourceCoupledBounceCount
        )) {
            throw new IllegalArgumentException(
                    "final GI trace consumption requires ray, hit, material, depth, and source-bounce evidence"
            );
        }
        if (realGpuTraversalConsumed && !finalGiSourceConsumed) {
            throw new IllegalArgumentException("realGpuTraversalConsumed requires finalGiSourceConsumed");
        }
        if (realGpuTraversalConsumed && !sourceAllowsRealGpuTraversal(evidenceSource)) {
            throw new IllegalArgumentException("realGpuTraversalConsumed requires a non-CPU GPU traversal evidence source");
        }
    }

    public TracedLightingConsumptionEvidence(
            long generation,
            long rayCount,
            long hitCount,
            long missCount,
            long materialCoupledHitCount,
            long sourceCoupledBounceCount,
            long cacheReadCount,
            long cacheWriteCount,
            boolean finalGiSourceConsumed,
            String finalGiSource,
            String evidenceSource,
            String boundary
    ) {
        this(
                generation,
                rayCount,
                hitCount,
                missCount,
                materialCoupledHitCount,
                0L,
                sourceCoupledBounceCount,
                cacheReadCount,
                cacheWriteCount,
                finalGiSourceConsumed,
                false,
                finalGiSource,
                evidenceSource,
                boundary
        );
    }

    public static TracedLightingConsumptionEvidence notConsumed(long generation, String reason) {
        return new TracedLightingConsumptionEvidence(
                generation,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                false,
                false,
                "not_consumed",
                "not_supplied",
                clean(reason, "traced_lighting_consumption_not_supplied")
        );
    }

    public static TracedLightingConsumptionEvidence fromVoxelTraversal(
            Round10VoxelTraversalOutputSummary output,
            long cacheReadCount,
            long cacheWriteCount,
            boolean finalGiSourceConsumed,
            String finalGiSource
    ) {
        Objects.requireNonNull(output, "output");
        return new TracedLightingConsumptionEvidence(
                output.generation(),
                output.rayCount(),
                output.hitCount(),
                output.missCount(),
                output.materialHitCount(),
                output.wallHitCount(),
                output.sourceCoupledBounceCount(),
                cacheReadCount,
                cacheWriteCount,
                finalGiSourceConsumed,
                finalGiSourceConsumed && output.realGpuTraversalExecuted(),
                finalGiSource,
                output.traversalBackend(),
                output.boundary()
        );
    }

    public static TracedLightingConsumptionEvidence fromVoxelTraversalStatus(
            Round10VoxelTraversalStatus status,
            long cacheReadCount,
            long cacheWriteCount,
            boolean finalGiSourceConsumed,
            String finalGiSource
    ) {
        Objects.requireNonNull(status, "status");
        return fromVoxelTraversal(
                status.output(),
                cacheReadCount,
                cacheWriteCount,
                finalGiSourceConsumed,
                finalGiSource
        );
    }

    public boolean hasTraceCounters() {
        return this.rayCount > 0L && (this.hitCount > 0L || this.missCount > 0L);
    }

    public boolean hasMaterialCoupledHits() {
        return this.materialCoupledHitCount > 0L;
    }

    public boolean hasDepthCoupledHits() {
        return this.depthCoupledHitCount > 0L;
    }

    public boolean hasSourceCoupledBounces() {
        return this.sourceCoupledBounceCount > 0L;
    }

    public boolean hasCacheEvidence() {
        return this.cacheReadCount > 0L || this.cacheWriteCount > 0L;
    }

    public boolean hasMaterialDepthSourceCoupling() {
        return this.hasMaterialCoupledHits()
                && this.hasDepthCoupledHits()
                && this.hasSourceCoupledBounces();
    }

    public boolean hasRequiredConsumptionCounters() {
        return hasConsumptionEvidence(
                this.rayCount,
                this.hitCount,
                this.materialCoupledHitCount,
                this.depthCoupledHitCount,
                this.sourceCoupledBounceCount
        );
    }

    public String blocker() {
        return this.boundary;
    }

    public String compactLabel() {
        return "rays=" + this.rayCount
                + "/hits=" + this.hitCount
                + "/misses=" + this.missCount
                + "/materialHits=" + this.materialCoupledHitCount
                + "/depthHits=" + this.depthCoupledHitCount
                + "/sourceBounces=" + this.sourceCoupledBounceCount
                + "/cacheReads=" + this.cacheReadCount
                + "/cacheWrites=" + this.cacheWriteCount
                + "/finalConsumed=" + this.finalGiSourceConsumed
                + "/realGpuConsumed=" + this.realGpuTraversalConsumed
                + "/source=" + this.evidenceSource;
    }

    private static boolean hasConsumptionEvidence(
            long rayCount,
            long hitCount,
            long materialCoupledHitCount,
            long depthCoupledHitCount,
            long sourceCoupledBounceCount
    ) {
        return rayCount > 0L
                && hitCount > 0L
                && materialCoupledHitCount > 0L
                && depthCoupledHitCount > 0L
                && sourceCoupledBounceCount > 0L;
    }

    private static boolean sourceAllowsRealGpuTraversal(String evidenceSource) {
        String source = clean(evidenceSource, "trace_consumption_evidence_unavailable")
                .toLowerCase(java.util.Locale.ROOT);
        return !source.contains("cpu")
                && !source.contains("metadata")
                && !source.contains("fallback")
                && !source.contains("not_")
                && !source.contains("unavailable");
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String clean(String value, String fallback) {
        String resolved = value == null || value.isBlank() ? fallback : value.trim();
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalArgumentException("text value must not be blank");
        }
        return resolved;
    }
}
