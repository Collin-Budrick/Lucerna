package net.lucerna.render.tracing.voxel;

import net.lucerna.world.section.VoxelOccupancyMaskMetadata;

public record Round10VoxelOccupancyMaskSummary(
        int sectionCount,
        int occupiedSectionCount,
        int maskBackedSectionCount,
        int readyMaskBackedSectionCount,
        int metadataOnlyMaskSectionCount,
        int missingMaskSectionCount,
        long maskWordCount,
        long maskBitCount,
        long generation
) {
    public Round10VoxelOccupancyMaskSummary {
        requireNonNegative(sectionCount, "sectionCount");
        requireNonNegative(occupiedSectionCount, "occupiedSectionCount");
        requireNonNegative(maskBackedSectionCount, "maskBackedSectionCount");
        requireNonNegative(readyMaskBackedSectionCount, "readyMaskBackedSectionCount");
        requireNonNegative(metadataOnlyMaskSectionCount, "metadataOnlyMaskSectionCount");
        requireNonNegative(missingMaskSectionCount, "missingMaskSectionCount");
        requireNonNegative(maskWordCount, "maskWordCount");
        requireNonNegative(maskBitCount, "maskBitCount");
        requireNonNegative(generation, "generation");
        if (occupiedSectionCount > sectionCount) {
            throw new IllegalArgumentException("occupiedSectionCount cannot exceed sectionCount");
        }
        if (maskBackedSectionCount + missingMaskSectionCount > occupiedSectionCount) {
            throw new IllegalArgumentException("mask section counts cannot exceed occupiedSectionCount");
        }
        if (readyMaskBackedSectionCount > maskBackedSectionCount) {
            throw new IllegalArgumentException("readyMaskBackedSectionCount cannot exceed maskBackedSectionCount");
        }
        if (metadataOnlyMaskSectionCount > maskBackedSectionCount) {
            throw new IllegalArgumentException("metadataOnlyMaskSectionCount cannot exceed maskBackedSectionCount");
        }
        if (maskBitCount > maskWordCount * VoxelOccupancyMaskMetadata.BITS_PER_WORD) {
            throw new IllegalArgumentException("maskBitCount cannot exceed maskWordCount capacity");
        }
    }

    public static Round10VoxelOccupancyMaskSummary empty() {
        return new Round10VoxelOccupancyMaskSummary(0, 0, 0, 0, 0, 0, 0L, 0L, 0L);
    }

    public boolean hasMaskPayload() {
        return this.maskBackedSectionCount > 0 && this.maskWordCount > 0;
    }

    public boolean maskBitsReady() {
        return this.occupiedSectionCount > 0
                && this.readyMaskBackedSectionCount == this.occupiedSectionCount
                && this.maskBitCount > 0;
    }

    public boolean hasPartialConcreteMaskPayload() {
        return this.readyMaskBackedSectionCount > 0 && this.maskBitCount > 0;
    }

    public String maskBitsSource() {
        if (this.maskBitsReady()) {
            return "section_snapshot_concrete_occupancy_masks";
        }
        if (this.hasPartialConcreteMaskPayload()) {
            return "partial_section_snapshot_concrete_occupancy_masks";
        }
        if (this.metadataOnlyMaskSectionCount > 0) {
            return "metadata_only_section_snapshot_occupancy_masks";
        }
        if (this.hasMaskPayload()) {
            return "section_snapshot_occupancy_masks_not_ready";
        }
        return "not_uploaded";
    }

    public boolean requiresFallbackTraversal() {
        return this.missingMaskSectionCount > 0
                || this.metadataOnlyMaskSectionCount > 0
                || this.readyMaskBackedSectionCount < this.occupiedSectionCount;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
