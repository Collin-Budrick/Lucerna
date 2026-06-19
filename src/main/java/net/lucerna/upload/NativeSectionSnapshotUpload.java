package net.lucerna.upload;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionEmissiveEntryMetadata;
import net.lucerna.world.section.VoxelOccupancyMaskMetadata;
import net.lucerna.world.section.VoxelOccupancySummary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record NativeSectionSnapshotUpload(
        NativeDirtyRegionHandoff dirtyRegionHandoff,
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ,
        long sectionGeneration,
        long materialGeneration,
        long occupancyGeneration,
        long emissiveGeneration,
        int occupiedVoxelCount,
        int opaqueVoxelCount,
        int translucentVoxelCount,
        int fluidVoxelCount,
        int emissiveVoxelCount,
        int occupancyBitOrderId,
        String occupancyBitOrderName,
        int occupancyMaskWordOffset,
        int occupancyMaskWordCount,
        int occupancyMaskBitCount,
        long occupancyMaskGeneration,
        int materialPaletteOffset,
        long materialPaletteGeneration,
        int[] materialPaletteIds,
        int[] emissiveVoxelIndices,
        int[] emissiveMaterialIds,
        int[] emissiveBlockLightLevels,
        long[] emissiveGenerations
) {
    public NativeSectionSnapshotUpload {
        Objects.requireNonNull(dirtyRegionHandoff, "dirtyRegionHandoff");
        requireText(dimension, "dimension");
        if (!dirtyRegionHandoff.matchesSection(dimension, sectionX, sectionY, sectionZ)) {
            throw new IllegalArgumentException("dirtyRegionHandoff must match the section origin");
        }
        requireNonNegative(sectionGeneration, "sectionGeneration");
        requireNonNegative(materialGeneration, "materialGeneration");
        requireNonNegative(occupancyGeneration, "occupancyGeneration");
        requireNonNegative(emissiveGeneration, "emissiveGeneration");
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
        if (occupancyBitOrderId <= 0) {
            throw new IllegalArgumentException("occupancyBitOrderId must be positive");
        }
        requireText(occupancyBitOrderName, "occupancyBitOrderName");
        if (occupancyMaskWordOffset < 0) {
            throw new IllegalArgumentException("occupancyMaskWordOffset must be non-negative");
        }
        if (occupancyMaskWordCount < 0 || occupancyMaskWordCount > VoxelOccupancyMaskMetadata.SECTION_MASK_WORD_COUNT) {
            throw new IllegalArgumentException(
                    "occupancyMaskWordCount must be between 0 and " + VoxelOccupancyMaskMetadata.SECTION_MASK_WORD_COUNT
            );
        }
        if (occupancyMaskBitCount < 0 || occupancyMaskBitCount > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException("occupancyMaskBitCount must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
        if (occupancyMaskBitCount > occupancyMaskWordCount * Long.SIZE) {
            throw new IllegalArgumentException("occupancyMaskBitCount cannot exceed the occupancy mask word capacity");
        }
        requireNonNegative(occupancyMaskGeneration, "occupancyMaskGeneration");
        if (materialPaletteOffset < 0) {
            throw new IllegalArgumentException("materialPaletteOffset must be non-negative");
        }
        requireNonNegative(materialPaletteGeneration, "materialPaletteGeneration");

        materialPaletteIds = copy(materialPaletteIds, "materialPaletteIds");
        emissiveVoxelIndices = copy(emissiveVoxelIndices, "emissiveVoxelIndices");
        emissiveMaterialIds = copy(emissiveMaterialIds, "emissiveMaterialIds");
        emissiveBlockLightLevels = copy(emissiveBlockLightLevels, "emissiveBlockLightLevels");
        emissiveGenerations = copy(emissiveGenerations, "emissiveGenerations");

        for (int materialId : materialPaletteIds) {
            if (materialId <= 0) {
                throw new IllegalArgumentException("materialPaletteIds must contain positive ids");
            }
        }

        requireMatchingLength(emissiveVoxelIndices.length, "emissiveMaterialIds", emissiveMaterialIds.length);
        requireMatchingLength(emissiveVoxelIndices.length, "emissiveBlockLightLevels", emissiveBlockLightLevels.length);
        requireMatchingLength(emissiveVoxelIndices.length, "emissiveGenerations", emissiveGenerations.length);
        if (emissiveVoxelIndices.length > emissiveVoxelCount) {
            throw new IllegalArgumentException("emissive payload count cannot exceed emissiveVoxelCount");
        }
        for (int index = 0; index < emissiveVoxelIndices.length; index++) {
            int voxelIndex = emissiveVoxelIndices[index];
            if (voxelIndex < 0 || voxelIndex >= ChunkSectionOrigin.SECTION_VOLUME) {
                throw new IllegalArgumentException("emissiveVoxelIndices entries must be section voxel indices");
            }
            if (emissiveMaterialIds[index] <= 0) {
                throw new IllegalArgumentException("emissiveMaterialIds must contain positive ids");
            }
            int blockLightLevel = emissiveBlockLightLevels[index];
            if (blockLightLevel < 0 || blockLightLevel > 15) {
                throw new IllegalArgumentException("emissiveBlockLightLevels entries must be between 0 and 15");
            }
            requireNonNegative(emissiveGenerations[index], "emissiveGenerations entries");
        }
    }

    public static NativeSectionSnapshotUpload from(ChunkSectionVoxelSnapshot snapshot, DirtyRegion dirtyRegion) {
        return from(snapshot, NativeDirtyRegionHandoff.from(dirtyRegion));
    }

    public static NativeSectionSnapshotUpload from(
            ChunkSectionVoxelSnapshot snapshot,
            NativeDirtyRegionHandoff dirtyRegionHandoff
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(dirtyRegionHandoff, "dirtyRegionHandoff");

        ChunkSectionOrigin origin = snapshot.origin();
        VoxelOccupancySummary summary = snapshot.occupancySummary();
        VoxelOccupancyMaskMetadata occupancyMask = snapshot.occupancyMask();
        List<SectionEmissiveEntryMetadata> emissiveEntries = snapshot.emissiveEntries();
        int[] emissiveVoxelIndices = new int[emissiveEntries.size()];
        int[] emissiveMaterialIds = new int[emissiveEntries.size()];
        int[] emissiveBlockLightLevels = new int[emissiveEntries.size()];
        long[] emissiveGenerations = new long[emissiveEntries.size()];

        for (int index = 0; index < emissiveEntries.size(); index++) {
            SectionEmissiveEntryMetadata entry = emissiveEntries.get(index);
            emissiveVoxelIndices[index] = entry.voxelIndex();
            emissiveMaterialIds[index] = entry.materialId();
            emissiveBlockLightLevels[index] = entry.blockLightLevel();
            emissiveGenerations[index] = entry.generation();
        }

        return new NativeSectionSnapshotUpload(
                dirtyRegionHandoff,
                origin.dimension(),
                origin.sectionX(),
                origin.sectionY(),
                origin.sectionZ(),
                snapshot.generation().sectionGeneration(),
                snapshot.generation().materialGeneration(),
                snapshot.generation().occupancyGeneration(),
                snapshot.generation().emissiveGeneration(),
                summary.occupiedVoxelCount(),
                summary.opaqueVoxelCount(),
                summary.translucentVoxelCount(),
                summary.fluidVoxelCount(),
                summary.emissiveVoxelCount(),
                occupancyMask.bitOrder().ordinal() + 1,
                occupancyMask.bitOrder().name(),
                occupancyMask.wordOffset(),
                occupancyMask.wordCount(),
                occupancyMask.bitCount(),
                occupancyMask.generation(),
                snapshot.materialPalette().paletteOffset(),
                snapshot.materialPalette().materialGeneration(),
                snapshot.materialPalette().materialIdArray(),
                emissiveVoxelIndices,
                emissiveMaterialIds,
                emissiveBlockLightLevels,
                emissiveGenerations
        );
    }

    public long dirtyRegionGeneration() {
        return this.dirtyRegionHandoff.dirtyRegionGeneration();
    }

    public long combinedGeneration() {
        return max(
                this.sectionGeneration,
                this.materialGeneration,
                this.occupancyGeneration,
                this.emissiveGeneration,
                this.dirtyRegionGeneration()
        );
    }

    public int materialPaletteSize() {
        return this.materialPaletteIds.length;
    }

    public int emissiveEntryCount() {
        return this.emissiveVoxelIndices.length;
    }

    public boolean hasSectionPayload() {
        return this.occupiedVoxelCount > 0
                || this.occupancyMaskWordCount > 0
                || this.materialPaletteIds.length > 0
                || this.emissiveVoxelIndices.length > 0;
    }

    @Override
    public int[] materialPaletteIds() {
        return copy(this.materialPaletteIds, "materialPaletteIds");
    }

    @Override
    public int[] emissiveVoxelIndices() {
        return copy(this.emissiveVoxelIndices, "emissiveVoxelIndices");
    }

    @Override
    public int[] emissiveMaterialIds() {
        return copy(this.emissiveMaterialIds, "emissiveMaterialIds");
    }

    @Override
    public int[] emissiveBlockLightLevels() {
        return copy(this.emissiveBlockLightLevels, "emissiveBlockLightLevels");
    }

    @Override
    public long[] emissiveGenerations() {
        return copy(this.emissiveGenerations, "emissiveGenerations");
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
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

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static long max(long first, long second, long third, long fourth, long fifth) {
        return Math.max(Math.max(first, second), Math.max(Math.max(third, fourth), fifth));
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
