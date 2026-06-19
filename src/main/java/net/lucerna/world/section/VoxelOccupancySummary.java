package net.lucerna.world.section;

public record VoxelOccupancySummary(
        int occupiedVoxelCount,
        int opaqueVoxelCount,
        int translucentVoxelCount,
        int fluidVoxelCount,
        int emissiveVoxelCount
) {
    public VoxelOccupancySummary {
        requireRange(occupiedVoxelCount, "occupiedVoxelCount");
        requireRange(opaqueVoxelCount, "opaqueVoxelCount");
        requireRange(translucentVoxelCount, "translucentVoxelCount");
        requireRange(fluidVoxelCount, "fluidVoxelCount");
        requireRange(emissiveVoxelCount, "emissiveVoxelCount");
        if (opaqueVoxelCount + translucentVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("opaque and translucent counts cannot exceed occupiedVoxelCount");
        }
        if (fluidVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("fluidVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (emissiveVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("emissiveVoxelCount cannot exceed occupiedVoxelCount");
        }
    }

    public static VoxelOccupancySummary empty() {
        return new VoxelOccupancySummary(0, 0, 0, 0, 0);
    }

    public int emptyVoxelCount() {
        return ChunkSectionOrigin.SECTION_VOLUME - this.occupiedVoxelCount;
    }

    public boolean hasOccupiedVoxels() {
        return this.occupiedVoxelCount > 0;
    }

    private static void requireRange(int count, String name) {
        if (count < 0 || count > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException(name + " must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
    }
}
