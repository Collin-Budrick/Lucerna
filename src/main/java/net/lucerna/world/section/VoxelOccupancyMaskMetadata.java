package net.lucerna.world.section;

import java.util.Objects;

public record VoxelOccupancyMaskMetadata(
        VoxelOccupancyBitOrder bitOrder,
        int wordOffset,
        int wordCount,
        int bitCount,
        long generation
) {
    public static final int BITS_PER_WORD = Long.SIZE;
    public static final int SECTION_MASK_WORD_COUNT = ChunkSectionOrigin.SECTION_VOLUME / BITS_PER_WORD;

    public VoxelOccupancyMaskMetadata {
        Objects.requireNonNull(bitOrder, "bitOrder");
        if (wordOffset < 0) {
            throw new IllegalArgumentException("wordOffset must be non-negative");
        }
        if (wordCount < 0 || wordCount > SECTION_MASK_WORD_COUNT) {
            throw new IllegalArgumentException("wordCount must be between 0 and " + SECTION_MASK_WORD_COUNT);
        }
        if (bitCount < 0 || bitCount > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException("bitCount must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
        if (bitCount > wordCount * BITS_PER_WORD) {
            throw new IllegalArgumentException("bitCount cannot exceed the capacity described by wordCount");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public static VoxelOccupancyMaskMetadata empty() {
        return new VoxelOccupancyMaskMetadata(VoxelOccupancyBitOrder.MINECRAFT_SECTION_YZX, 0, 0, 0, 0);
    }

    public static VoxelOccupancyMaskMetadata sectionMask(int wordOffset, long generation) {
        return new VoxelOccupancyMaskMetadata(
                VoxelOccupancyBitOrder.MINECRAFT_SECTION_YZX,
                wordOffset,
                SECTION_MASK_WORD_COUNT,
                ChunkSectionOrigin.SECTION_VOLUME,
                generation
        );
    }

    public int bitOffset() {
        return this.wordOffset * BITS_PER_WORD;
    }

    public boolean hasMask() {
        return this.wordCount > 0;
    }
}
