package net.lucerna.world.section;

public record SectionSurfaceSampleMetadata(
        int localX,
        int localY,
        int localZ,
        int materialId,
        long generation
) {
    public SectionSurfaceSampleMetadata {
        VoxelOccupancyBitOrder.requireLocalCoordinate(localX, "localX");
        VoxelOccupancyBitOrder.requireLocalCoordinate(localY, "localY");
        VoxelOccupancyBitOrder.requireLocalCoordinate(localZ, "localZ");
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public int voxelIndex() {
        return VoxelOccupancyBitOrder.MINECRAFT_SECTION_YZX.toIndex(this.localX, this.localY, this.localZ);
    }
}
