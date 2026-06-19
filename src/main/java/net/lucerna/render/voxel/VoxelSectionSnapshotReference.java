package net.lucerna.render.voxel;

import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionMaterialPaletteReference;
import net.lucerna.world.section.VoxelOccupancyBitOrder;
import net.lucerna.world.section.VoxelOccupancyMaskMetadata;
import net.lucerna.world.section.VoxelOccupancySummary;

import java.util.Objects;

public record VoxelSectionSnapshotReference(
        ChunkSectionOrigin origin,
        ChunkSectionGeneration generation,
        int occupiedVoxelCount,
        int opaqueVoxelCount,
        int translucentVoxelCount,
        int fluidVoxelCount,
        int emissiveVoxelCount,
        VoxelOccupancyBitOrder occupancyBitOrder,
        int occupancyMaskWordOffset,
        int occupancyMaskWordCount,
        int occupancyMaskBitCount,
        long occupancyMaskGeneration,
        int materialPaletteOffset,
        int materialPaletteSize,
        long materialGeneration,
        boolean hasEmissivePayload
) {
    public VoxelSectionSnapshotReference {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(occupancyBitOrder, "occupancyBitOrder");
        requireVoxelCount(occupiedVoxelCount, "occupiedVoxelCount");
        requireVoxelCount(opaqueVoxelCount, "opaqueVoxelCount");
        requireVoxelCount(translucentVoxelCount, "translucentVoxelCount");
        requireVoxelCount(fluidVoxelCount, "fluidVoxelCount");
        requireVoxelCount(emissiveVoxelCount, "emissiveVoxelCount");
        if (opaqueVoxelCount + translucentVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("opaque and translucent counts cannot exceed occupiedVoxelCount");
        }
        if (fluidVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("fluidVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (emissiveVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("emissiveVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (occupancyMaskWordOffset < 0) {
            throw new IllegalArgumentException("occupancyMaskWordOffset must be non-negative");
        }
        if (occupancyMaskWordCount < 0 || occupancyMaskWordCount > VoxelOccupancyMaskMetadata.SECTION_MASK_WORD_COUNT) {
            throw new IllegalArgumentException(
                    "occupancyMaskWordCount must be between 0 and " + VoxelOccupancyMaskMetadata.SECTION_MASK_WORD_COUNT
            );
        }
        requireVoxelCount(occupancyMaskBitCount, "occupancyMaskBitCount");
        if (occupancyMaskBitCount > occupancyMaskWordCount * Long.SIZE) {
            throw new IllegalArgumentException("occupancyMaskBitCount cannot exceed the occupancy mask word capacity");
        }
        requireNonNegative(occupancyMaskGeneration, "occupancyMaskGeneration");
        if (generation.occupancyGeneration() < occupancyMaskGeneration) {
            throw new IllegalArgumentException("section occupancy generation must include occupancy mask generation");
        }
        if (materialPaletteOffset < 0) {
            throw new IllegalArgumentException("materialPaletteOffset must be non-negative");
        }
        if (materialPaletteSize < 0) {
            throw new IllegalArgumentException("materialPaletteSize must be non-negative");
        }
        requireNonNegative(materialGeneration, "materialGeneration");
        if (generation.materialGeneration() < materialGeneration) {
            throw new IllegalArgumentException("section material generation must include material palette generation");
        }
    }

    public static VoxelSectionSnapshotReference from(ChunkSectionVoxelSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        VoxelOccupancySummary summary = snapshot.occupancySummary();
        VoxelOccupancyMaskMetadata occupancyMask = snapshot.occupancyMask();
        SectionMaterialPaletteReference materialPalette = snapshot.materialPalette();
        return new VoxelSectionSnapshotReference(
                snapshot.origin(),
                snapshot.generation(),
                summary.occupiedVoxelCount(),
                summary.opaqueVoxelCount(),
                summary.translucentVoxelCount(),
                summary.fluidVoxelCount(),
                summary.emissiveVoxelCount(),
                occupancyMask.bitOrder(),
                occupancyMask.wordOffset(),
                occupancyMask.wordCount(),
                occupancyMask.bitCount(),
                occupancyMask.generation(),
                materialPalette.paletteOffset(),
                materialPalette.paletteSize(),
                materialPalette.materialGeneration(),
                !snapshot.emissiveEntries().isEmpty()
        );
    }

    public String stableKey() {
        return this.origin.stableKey();
    }

    public long combinedGeneration() {
        return Math.max(this.generation.combinedGeneration(), Math.max(this.occupancyMaskGeneration, this.materialGeneration));
    }

    public boolean hasOccupiedVoxels() {
        return this.occupiedVoxelCount > 0;
    }

    public boolean hasOccupancyMask() {
        return this.occupancyMaskWordCount > 0;
    }

    public boolean hasMaterialPalette() {
        return this.materialPaletteSize > 0;
    }

    public boolean hasTraversalPayload() {
        return this.hasOccupiedVoxels() || this.hasOccupancyMask() || this.hasMaterialPalette() || this.hasEmissivePayload;
    }

    private static void requireVoxelCount(int count, String name) {
        if (count < 0 || count > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException(name + " must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
