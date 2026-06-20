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

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
