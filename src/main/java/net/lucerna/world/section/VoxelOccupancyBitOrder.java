package net.lucerna.world.section;

public enum VoxelOccupancyBitOrder {
    MINECRAFT_SECTION_YZX;

    public int toIndex(int localX, int localY, int localZ) {
        requireLocalCoordinate(localX, "localX");
        requireLocalCoordinate(localY, "localY");
        requireLocalCoordinate(localZ, "localZ");
        return (localY * ChunkSectionOrigin.SECTION_EDGE_LENGTH + localZ)
                * ChunkSectionOrigin.SECTION_EDGE_LENGTH
                + localX;
    }

    public static void requireLocalCoordinate(int coordinate, String name) {
        if (coordinate < 0 || coordinate >= ChunkSectionOrigin.SECTION_EDGE_LENGTH) {
            throw new IllegalArgumentException(name + " must be between 0 and 15");
        }
    }
}
