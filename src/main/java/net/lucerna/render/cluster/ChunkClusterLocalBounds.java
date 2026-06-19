package net.lucerna.render.cluster;

import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.VoxelOccupancyBitOrder;

public record ChunkClusterLocalBounds(
        int minLocalX,
        int minLocalY,
        int minLocalZ,
        int maxLocalX,
        int maxLocalY,
        int maxLocalZ
) {
    public ChunkClusterLocalBounds {
        VoxelOccupancyBitOrder.requireLocalCoordinate(minLocalX, "minLocalX");
        VoxelOccupancyBitOrder.requireLocalCoordinate(minLocalY, "minLocalY");
        VoxelOccupancyBitOrder.requireLocalCoordinate(minLocalZ, "minLocalZ");
        VoxelOccupancyBitOrder.requireLocalCoordinate(maxLocalX, "maxLocalX");
        VoxelOccupancyBitOrder.requireLocalCoordinate(maxLocalY, "maxLocalY");
        VoxelOccupancyBitOrder.requireLocalCoordinate(maxLocalZ, "maxLocalZ");
        if (maxLocalX < minLocalX || maxLocalY < minLocalY || maxLocalZ < minLocalZ) {
            throw new IllegalArgumentException("cluster local bounds must be ordered");
        }
    }

    public static ChunkClusterLocalBounds wholeSection() {
        int max = ChunkSectionOrigin.SECTION_EDGE_LENGTH - 1;
        return new ChunkClusterLocalBounds(0, 0, 0, max, max, max);
    }

    public int spanX() {
        return this.maxLocalX - this.minLocalX + 1;
    }

    public int spanY() {
        return this.maxLocalY - this.minLocalY + 1;
    }

    public int spanZ() {
        return this.maxLocalZ - this.minLocalZ + 1;
    }

    public int estimatedVoxelCapacity() {
        return this.spanX() * this.spanY() * this.spanZ();
    }

    public int minBlockX(ChunkSectionOrigin origin) {
        return origin.minBlockX() + this.minLocalX;
    }

    public int minBlockY(ChunkSectionOrigin origin) {
        return origin.minBlockY() + this.minLocalY;
    }

    public int minBlockZ(ChunkSectionOrigin origin) {
        return origin.minBlockZ() + this.minLocalZ;
    }
}
