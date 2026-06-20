package net.lucerna.render.tracing.voxel;

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
                output.hasTraversalEvidence()
                        ? "round10_voxel_traversal_cpu_metadata_status_recorded"
                        : "round10_voxel_traversal_cpu_metadata_status_no_rays"
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

    public String correctnessLabel() {
        return this.output.correctnessLabel();
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
