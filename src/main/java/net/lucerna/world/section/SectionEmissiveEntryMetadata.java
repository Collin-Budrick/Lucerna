package net.lucerna.world.section;

public record SectionEmissiveEntryMetadata(
        int localX,
        int localY,
        int localZ,
        int materialId,
        int blockLightLevel,
        long generation
) {
    public SectionEmissiveEntryMetadata {
        VoxelOccupancyBitOrder.requireLocalCoordinate(localX, "localX");
        VoxelOccupancyBitOrder.requireLocalCoordinate(localY, "localY");
        VoxelOccupancyBitOrder.requireLocalCoordinate(localZ, "localZ");
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (blockLightLevel < 0 || blockLightLevel > 15) {
            throw new IllegalArgumentException("blockLightLevel must be between 0 and 15");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public int voxelIndex() {
        return VoxelOccupancyBitOrder.MINECRAFT_SECTION_YZX.toIndex(this.localX, this.localY, this.localZ);
    }
}
