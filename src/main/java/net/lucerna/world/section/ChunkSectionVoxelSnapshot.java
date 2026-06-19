package net.lucerna.world.section;

import java.util.List;
import java.util.Objects;

public record ChunkSectionVoxelSnapshot(
        ChunkSectionOrigin origin,
        ChunkSectionGeneration generation,
        VoxelOccupancySummary occupancySummary,
        VoxelOccupancyMaskMetadata occupancyMask,
        SectionMaterialPaletteReference materialPalette,
        List<SectionEmissiveEntryMetadata> emissiveEntries,
        List<SectionSurfaceSampleMetadata> surfaceSamples
) {
    public ChunkSectionVoxelSnapshot {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(occupancySummary, "occupancySummary");
        Objects.requireNonNull(occupancyMask, "occupancyMask");
        Objects.requireNonNull(materialPalette, "materialPalette");
        Objects.requireNonNull(emissiveEntries, "emissiveEntries");
        Objects.requireNonNull(surfaceSamples, "surfaceSamples");
        emissiveEntries = List.copyOf(emissiveEntries);
        surfaceSamples = List.copyOf(surfaceSamples);

        long maxEmissiveGeneration = emissiveEntries.stream()
                .mapToLong(SectionEmissiveEntryMetadata::generation)
                .max()
                .orElse(0L);
        long maxSurfaceGeneration = surfaceSamples.stream()
                .mapToLong(SectionSurfaceSampleMetadata::generation)
                .max()
                .orElse(0L);
        if (generation.emissiveGeneration() < maxEmissiveGeneration) {
            throw new IllegalArgumentException("emissive generation must include all emissive entries");
        }
        if (generation.occupancyGeneration() < maxSurfaceGeneration || generation.materialGeneration() < maxSurfaceGeneration) {
            throw new IllegalArgumentException("section generation must include all surface samples");
        }
        if (generation.occupancyGeneration() < occupancyMask.generation()) {
            throw new IllegalArgumentException("occupancy generation must include the occupancy mask generation");
        }
        if (generation.materialGeneration() < materialPalette.materialGeneration()) {
            throw new IllegalArgumentException("material generation must include the palette generation");
        }
        if (emissiveEntries.size() > occupancySummary.emissiveVoxelCount()) {
            throw new IllegalArgumentException("emissive entry count cannot exceed the emissive voxel count");
        }
        if (surfaceSamples.size() > occupancySummary.occupiedVoxelCount()) {
            throw new IllegalArgumentException("surface sample count cannot exceed the occupied voxel count");
        }
    }

    public static ChunkSectionVoxelSnapshot empty(ChunkSectionOrigin origin) {
        return new ChunkSectionVoxelSnapshot(
                origin,
                ChunkSectionGeneration.empty(),
                VoxelOccupancySummary.empty(),
                VoxelOccupancyMaskMetadata.empty(),
                SectionMaterialPaletteReference.empty(),
                List.of(),
                List.of()
        );
    }

    public int emissiveEntryCount() {
        return this.emissiveEntries.size();
    }

    public boolean hasVoxelPayload() {
        return this.occupancySummary.hasOccupiedVoxels()
                || this.occupancyMask.hasMask()
                || this.materialPalette.hasPalette()
                || !this.emissiveEntries.isEmpty()
                || !this.surfaceSamples.isEmpty();
    }
}
