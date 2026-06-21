package net.lucerna.render.tracing.voxel;

import net.lucerna.render.tracing.TracedLightingConsumptionEvidence;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;

import java.util.List;
import java.util.Objects;

public record Round10VoxelTraversalStatus(
        Round10VoxelTraversalInputSummary input,
        Round10VoxelTraversalOutputSummary output,
        boolean gpuTraversalAvailable,
        boolean cpuMetadataFallback,
        boolean realOccupancyMaskBitsAvailable,
        String marker
) {
    public Round10VoxelTraversalStatus {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        marker = requireText(marker, "marker");
        if (gpuTraversalAvailable && cpuMetadataFallback) {
            throw new IllegalArgumentException("gpuTraversalAvailable and cpuMetadataFallback cannot both be true");
        }
        if (realOccupancyMaskBitsAvailable && !input.occupancyMaskSummary().hasMaskPayload()) {
            throw new IllegalArgumentException("realOccupancyMaskBitsAvailable requires occupancy mask metadata");
        }
        if (output.realGpuTraversalExecuted() && !gpuTraversalAvailable) {
            throw new IllegalArgumentException("realGpuTraversalExecuted requires gpuTraversalAvailable");
        }
        if (output.realGpuTraversalExecuted() && cpuMetadataFallback) {
            throw new IllegalArgumentException("realGpuTraversalExecuted cannot use cpuMetadataFallback");
        }
        if (output.maskBitsReady() && !input.occupancyMaskSummary().hasMaskPayload()) {
            throw new IllegalArgumentException("maskBitsReady requires occupancy mask metadata");
        }
    }

    public static Round10VoxelTraversalStatus cpuMetadataFallback(
            Round10VoxelTraversalInputSummary input,
            Round10VoxelTraversalOutputSummary output
    ) {
        return new Round10VoxelTraversalStatus(
                input,
                output,
                false,
                true,
                false,
                markerFor(output)
        );
    }

    public static Round10VoxelTraversalStatus cpuBoundedSnapshotTraversal(
            Round10VoxelTraversalInputSummary input,
            List<VoxelSectionSnapshotReference> sections
    ) {
        Objects.requireNonNull(input, "input");
        Round10VoxelTraversalOutputSummary output = Round10VoxelTraversalOutputSummary.boundedCpuFromSectionSnapshots(
                input.sourceGeneration(),
                sections
        );
        return new Round10VoxelTraversalStatus(
                input,
                output,
                false,
                true,
                output.maskBitsReady() && !output.maskBitsSource().contains("metadata_only"),
                markerFor(output)
        );
    }

    public static Round10VoxelTraversalStatus metadataOnlySnapshotStatus(
            Round10VoxelTraversalInputSummary input,
            List<VoxelSectionSnapshotReference> sections
    ) {
        Objects.requireNonNull(input, "input");
        Round10VoxelTraversalOutputSummary output = Round10VoxelTraversalOutputSummary.metadataOnlyFromSectionSnapshots(
                input.sourceGeneration(),
                sections
        );
        return new Round10VoxelTraversalStatus(
                input,
                output,
                false,
                true,
                false,
                markerFor(output)
        );
    }

    public boolean hasControllerProofCounters() {
        return this.output.hasTraversalEvidence()
                && this.output.hitCount() > 0
                && this.output.missCount() > 0;
    }

    public boolean materialIdConsistency() {
        return "consistent".equalsIgnoreCase(this.output.materialIdConsistency())
                || "pass".equalsIgnoreCase(this.output.materialIdConsistency())
                || "matched".equalsIgnoreCase(this.output.materialIdConsistency());
    }

    public boolean emptySectionSkipSafe() {
        return this.output.emptySectionSkipSafe();
    }

    public boolean maskBitsReady() {
        return this.output.maskBitsReady();
    }

    public String maskBitsSource() {
        return this.output.maskBitsSource();
    }

    public String traversalBackend() {
        return this.output.traversalBackend();
    }

    public boolean realGpuTraversalExecuted() {
        return this.output.realGpuTraversalExecuted();
    }

    public long sourceCoupledBounceCount() {
        return this.output.sourceCoupledBounceCount();
    }

    public String sourceCoupledBounceSource() {
        return this.output.sourceCoupledBounceSource();
    }

    public long depthCoupledHitCount() {
        return this.output.depthCoupledHitCount();
    }

    public boolean hasMaterialDepthSourceCoupling() {
        return this.output.hasMaterialDepthSourceCoupling();
    }

    public String boundaryLabel() {
        return this.output.boundary();
    }

    public String correctnessLabel() {
        return this.output.correctnessLabel();
    }

    public TracedLightingConsumptionEvidence toTraceConsumptionEvidence(
            long cacheReadCount,
            long cacheWriteCount,
            boolean finalGiSourceConsumed,
            String finalGiSource
    ) {
        return this.output.toTraceConsumptionEvidence(
                cacheReadCount,
                cacheWriteCount,
                finalGiSourceConsumed,
                finalGiSource
        );
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String markerFor(Round10VoxelTraversalOutputSummary output) {
        Objects.requireNonNull(output, "output");
        if (output.traversalBackend().contains("metadata_only")) {
            return output.hasTraversalEvidence()
                    ? "round10_voxel_traversal_metadata_only_status_recorded"
                    : "round10_voxel_traversal_metadata_only_status_no_rays";
        }
        if (output.hasMaterialDepthSourceCoupling()) {
            return "round10_voxel_traversal_cpu_bounded_scene_tied_gi_evidence_recorded";
        }
        if (output.hasMaterialHitEvidence() && output.hasDepthCoupledHitEvidence()) {
            return "round10_voxel_traversal_cpu_bounded_material_depth_evidence_recorded";
        }
        if (output.hasTraversalEvidence()) {
            return "round10_voxel_traversal_cpu_metadata_status_recorded";
        }
        return "round10_voxel_traversal_cpu_metadata_status_no_rays";
    }
}
