package net.lucerna.render.voxel;

import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.Objects;

public record VoxelTraversalHit(
        ChunkSectionOrigin sectionOrigin,
        int voxelIndex,
        int blockX,
        int blockY,
        int blockZ,
        int materialId,
        float distance,
        int normalX,
        int normalY,
        int normalZ,
        long sectionGeneration,
        long materialGeneration
) {
    public VoxelTraversalHit {
        Objects.requireNonNull(sectionOrigin, "sectionOrigin");
        if (voxelIndex < 0 || voxelIndex >= ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException("voxelIndex must be a section-local voxel index");
        }
        requireBlockCoordinateInSection(blockX, sectionOrigin.minBlockX(), "blockX");
        requireBlockCoordinateInSection(blockY, sectionOrigin.minBlockY(), "blockY");
        requireBlockCoordinateInSection(blockZ, sectionOrigin.minBlockZ(), "blockZ");
        if (materialId < 0) {
            throw new IllegalArgumentException("materialId must be non-negative");
        }
        if (!Float.isFinite(distance) || distance < 0.0F) {
            throw new IllegalArgumentException("distance must be finite and non-negative");
        }
        requireAxisNormal(normalX, normalY, normalZ);
        requireNonNegative(sectionGeneration, "sectionGeneration");
        requireNonNegative(materialGeneration, "materialGeneration");
    }

    public boolean hasMaterial() {
        return this.materialId > 0;
    }

    public long combinedGeneration() {
        return Math.max(this.sectionGeneration, this.materialGeneration);
    }

    public String blockKey() {
        return this.sectionOrigin.dimension() + ":" + this.blockX + "," + this.blockY + "," + this.blockZ;
    }

    private static void requireBlockCoordinateInSection(int blockCoordinate, int sectionMin, String name) {
        int sectionMaxExclusive = sectionMin + ChunkSectionOrigin.SECTION_EDGE_LENGTH;
        if (blockCoordinate < sectionMin || blockCoordinate >= sectionMaxExclusive) {
            throw new IllegalArgumentException(name + " must be within the hit section");
        }
    }

    private static void requireAxisNormal(int normalX, int normalY, int normalZ) {
        requireNormalComponent(normalX, "normalX");
        requireNormalComponent(normalY, "normalY");
        requireNormalComponent(normalZ, "normalZ");
        int manhattanLength = Math.abs(normalX) + Math.abs(normalY) + Math.abs(normalZ);
        if (manhattanLength != 0 && manhattanLength != 1) {
            throw new IllegalArgumentException("hit normal must be zero or one axis-aligned unit direction");
        }
    }

    private static void requireNormalComponent(int value, String name) {
        if (value < -1 || value > 1) {
            throw new IllegalArgumentException(name + " must be between -1 and 1");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
