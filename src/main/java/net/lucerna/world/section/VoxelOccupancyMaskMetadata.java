package net.lucerna.world.section;

import java.util.Objects;

public record VoxelOccupancyMaskMetadata(
        VoxelOccupancyBitOrder bitOrder,
        int wordOffset,
        int wordCount,
        int bitCount,
        long generation,
        boolean maskBitsReady,
        VoxelOccupancyMaskSource source
) {
    public static final int BITS_PER_WORD = Long.SIZE;
    public static final int SECTION_MASK_WORD_COUNT = ChunkSectionOrigin.SECTION_VOLUME / BITS_PER_WORD;

    public VoxelOccupancyMaskMetadata(
            VoxelOccupancyBitOrder bitOrder,
            int wordOffset,
            int wordCount,
            int bitCount,
            long generation
    ) {
        this(
                bitOrder,
                wordOffset,
                wordCount,
                bitCount,
                generation,
                false,
                wordCount > 0 && bitCount > 0 ? VoxelOccupancyMaskSource.METADATA_ONLY : VoxelOccupancyMaskSource.NONE
        );
    }

    public VoxelOccupancyMaskMetadata {
        Objects.requireNonNull(bitOrder, "bitOrder");
        Objects.requireNonNull(source, "source");
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
        if (maskBitsReady && (wordCount == 0 || bitCount == 0)) {
            throw new IllegalArgumentException("maskBitsReady requires non-empty mask storage metadata");
        }
        if (!maskBitsReady && source != VoxelOccupancyMaskSource.NONE && source != VoxelOccupancyMaskSource.METADATA_ONLY) {
            throw new IllegalArgumentException("non-ready mask bits cannot claim a concrete scan/upload source");
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
                generation,
                true,
                VoxelOccupancyMaskSource.JAVA_SECTION_SCAN
        );
    }

    public int bitOffset() {
        return this.wordOffset * BITS_PER_WORD;
    }

    public boolean hasMask() {
        return this.wordCount > 0;
    }

    public boolean readyForTraversal() {
        return this.maskBitsReady;
    }

    public boolean metadataOnly() {
        return this.source.metadataOnly();
    }
}
