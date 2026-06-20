package net.lucerna.render.tracing.voxel;

import net.lucerna.world.section.VoxelOccupancyMaskMetadata;

public record Round10VoxelOccupancyMaskSummary(
        int sectionCount,
        int maskBackedSectionCount,
        int missingMaskSectionCount,
        long maskWordCount,
        long maskBitCount,
        long generation
) {
    public Round10VoxelOccupancyMaskSummary {
        requireNonNegative(sectionCount, "sectionCount");
        requireNonNegative(maskBackedSectionCount, "maskBackedSectionCount");
        requireNonNegative(missingMaskSectionCount, "missingMaskSectionCount");
        requireNonNegative(maskWordCount, "maskWordCount");
        requireNonNegative(maskBitCount, "maskBitCount");
        requireNonNegative(generation, "generation");
        if (maskBackedSectionCount + missingMaskSectionCount > sectionCount) {
            throw new IllegalArgumentException("mask section counts cannot exceed sectionCount");
        }
        if (maskBitCount > maskWordCount * VoxelOccupancyMaskMetadata.BITS_PER_WORD) {
            throw new IllegalArgumentException("maskBitCount cannot exceed maskWordCount capacity");
        }
    }

    public static Round10VoxelOccupancyMaskSummary empty() {
        return new Round10VoxelOccupancyMaskSummary(0, 0, 0, 0L, 0L, 0L);
    }

    public boolean hasMaskPayload() {
        return this.maskBackedSectionCount > 0 && this.maskWordCount > 0;
    }

    public boolean requiresFallbackTraversal() {
        return this.missingMaskSectionCount > 0;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
