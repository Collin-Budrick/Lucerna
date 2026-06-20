package net.lucerna.world.section;

public record VoxelOccupancySummary(
        int occupiedVoxelCount,
        int opaqueVoxelCount,
        int translucentVoxelCount,
        int fluidVoxelCount,
        int emissiveVoxelCount,
        int solidWallHitEvidenceCount,
        int openSkyMissEvidenceCount,
        int glassVoxelCount,
        int waterVoxelCount,
        int opaqueMaterialFlagCount
) {
    public VoxelOccupancySummary(
            int occupiedVoxelCount,
            int opaqueVoxelCount,
            int translucentVoxelCount,
            int fluidVoxelCount,
            int emissiveVoxelCount
    ) {
        this(
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount,
                0,
                0,
                0,
                0,
                0
        );
    }

    public VoxelOccupancySummary {
        requireRange(occupiedVoxelCount, "occupiedVoxelCount");
        requireRange(opaqueVoxelCount, "opaqueVoxelCount");
        requireRange(translucentVoxelCount, "translucentVoxelCount");
        requireRange(fluidVoxelCount, "fluidVoxelCount");
        requireRange(emissiveVoxelCount, "emissiveVoxelCount");
        requireRange(solidWallHitEvidenceCount, "solidWallHitEvidenceCount");
        requireRange(openSkyMissEvidenceCount, "openSkyMissEvidenceCount");
        requireRange(glassVoxelCount, "glassVoxelCount");
        requireRange(waterVoxelCount, "waterVoxelCount");
        requireRange(opaqueMaterialFlagCount, "opaqueMaterialFlagCount");
        if (opaqueVoxelCount + translucentVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("opaque and translucent counts cannot exceed occupiedVoxelCount");
        }
        if (fluidVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("fluidVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (emissiveVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("emissiveVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (solidWallHitEvidenceCount > opaqueVoxelCount) {
            throw new IllegalArgumentException("solidWallHitEvidenceCount cannot exceed opaqueVoxelCount");
        }
        if (glassVoxelCount > translucentVoxelCount) {
            throw new IllegalArgumentException("glassVoxelCount cannot exceed translucentVoxelCount");
        }
        if (waterVoxelCount > fluidVoxelCount) {
            throw new IllegalArgumentException("waterVoxelCount cannot exceed fluidVoxelCount");
        }
        if (opaqueMaterialFlagCount > opaqueVoxelCount) {
            throw new IllegalArgumentException("opaqueMaterialFlagCount cannot exceed opaqueVoxelCount");
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

    public boolean hasSolidWallHitEvidence() {
        return this.solidWallHitEvidenceCount > 0;
    }

    public boolean hasOpenSkyMissEvidence() {
        return this.openSkyMissEvidenceCount > 0;
    }

    public boolean hasGlassMaterialFlags() {
        return this.glassVoxelCount > 0;
    }

    public boolean hasWaterMaterialFlags() {
        return this.waterVoxelCount > 0;
    }

    public boolean hasOpaqueMaterialFlags() {
        return this.opaqueMaterialFlagCount > 0;
    }

    private static void requireRange(int count, String name) {
        if (count < 0 || count > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException(name + " must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
    }
}
