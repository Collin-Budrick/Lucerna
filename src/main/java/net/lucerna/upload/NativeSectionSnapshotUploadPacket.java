package net.lucerna.upload;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativeSectionSnapshotUploadPacket {
    private final long generation;
    private final int sectionSnapshotCount;
    private final long firstSectionSnapshotGeneration;
    private final long lastSectionSnapshotGeneration;
    private final long sectionGeneration;
    private final long sectionMaterialGeneration;
    private final long sectionOccupancyGeneration;
    private final long sectionEmissiveGeneration;
    private final long sectionDirtyRegionGeneration;
    private final int[] dirtyRegionTypeIds;
    private final String[] dirtyRegionTypeNames;
    private final String[] dirtyRegionDimensions;
    private final int[] dirtyRegionSectionXs;
    private final int[] dirtyRegionSectionYs;
    private final int[] dirtyRegionSectionZs;
    private final int[] dirtyRegionSectionScoped;
    private final long[] dirtyRegionGenerations;
    private final String[] sectionDimensions;
    private final int[] sectionXs;
    private final int[] sectionYs;
    private final int[] sectionZs;
    private final long[] sectionCombinedGenerations;
    private final long[] sectionGenerations;
    private final long[] sectionMaterialGenerations;
    private final long[] sectionOccupancyGenerations;
    private final long[] sectionEmissiveGenerations;
    private final long[] sectionDirtyRegionGenerations;
    private final int[] occupiedVoxelCounts;
    private final int[] opaqueVoxelCounts;
    private final int[] translucentVoxelCounts;
    private final int[] fluidVoxelCounts;
    private final int[] emissiveVoxelCounts;
    private final int[] voxelCounts;
    private final int[] occupancyBitOrderIds;
    private final String[] occupancyBitOrderNames;
    private final int[] occupancyMaskWordOffsets;
    private final int[] occupancyMaskWordCounts;
    private final int[] occupancyMaskBitCounts;
    private final long[] occupancyMaskGenerations;
    private final int[] materialPaletteOffsets;
    private final long[] materialPaletteGenerations;
    private final int[] materialPalettePayloadOffsets;
    private final int[] materialPalettePayloadCounts;
    private final int[] materialPaletteIds;
    private final int[] emissivePayloadOffsets;
    private final int[] emissivePayloadCounts;
    private final int[] emissiveVoxelIndices;
    private final int[] emissiveMaterialIds;
    private final int[] emissiveBlockLightLevels;
    private final long[] emissiveEntryGenerations;

    private NativeSectionSnapshotUploadPacket(
            long generation,
            int sectionSnapshotCount,
            long firstSectionSnapshotGeneration,
            long lastSectionSnapshotGeneration,
            long sectionGeneration,
            long sectionMaterialGeneration,
            long sectionOccupancyGeneration,
            long sectionEmissiveGeneration,
            long sectionDirtyRegionGeneration,
            int[] dirtyRegionTypeIds,
            String[] dirtyRegionTypeNames,
            String[] dirtyRegionDimensions,
            int[] dirtyRegionSectionXs,
            int[] dirtyRegionSectionYs,
            int[] dirtyRegionSectionZs,
            int[] dirtyRegionSectionScoped,
            long[] dirtyRegionGenerations,
            String[] sectionDimensions,
            int[] sectionXs,
            int[] sectionYs,
            int[] sectionZs,
            long[] sectionCombinedGenerations,
            long[] sectionGenerations,
            long[] sectionMaterialGenerations,
            long[] sectionOccupancyGenerations,
            long[] sectionEmissiveGenerations,
            long[] sectionDirtyRegionGenerations,
            int[] occupiedVoxelCounts,
            int[] opaqueVoxelCounts,
            int[] translucentVoxelCounts,
            int[] fluidVoxelCounts,
            int[] emissiveVoxelCounts,
            int[] voxelCounts,
            int[] occupancyBitOrderIds,
            String[] occupancyBitOrderNames,
            int[] occupancyMaskWordOffsets,
            int[] occupancyMaskWordCounts,
            int[] occupancyMaskBitCounts,
            long[] occupancyMaskGenerations,
            int[] materialPaletteOffsets,
            long[] materialPaletteGenerations,
            int[] materialPalettePayloadOffsets,
            int[] materialPalettePayloadCounts,
            int[] materialPaletteIds,
            int[] emissivePayloadOffsets,
            int[] emissivePayloadCounts,
            int[] emissiveVoxelIndices,
            int[] emissiveMaterialIds,
            int[] emissiveBlockLightLevels,
            long[] emissiveEntryGenerations
    ) {
        this.generation = generation;
        this.sectionSnapshotCount = sectionSnapshotCount;
        this.firstSectionSnapshotGeneration = firstSectionSnapshotGeneration;
        this.lastSectionSnapshotGeneration = lastSectionSnapshotGeneration;
        this.sectionGeneration = sectionGeneration;
        this.sectionMaterialGeneration = sectionMaterialGeneration;
        this.sectionOccupancyGeneration = sectionOccupancyGeneration;
        this.sectionEmissiveGeneration = sectionEmissiveGeneration;
        this.sectionDirtyRegionGeneration = sectionDirtyRegionGeneration;
        this.dirtyRegionTypeIds = copy(dirtyRegionTypeIds, "dirtyRegionTypeIds");
        this.dirtyRegionTypeNames = copy(dirtyRegionTypeNames, "dirtyRegionTypeNames");
        this.dirtyRegionDimensions = copy(dirtyRegionDimensions, "dirtyRegionDimensions");
        this.dirtyRegionSectionXs = copy(dirtyRegionSectionXs, "dirtyRegionSectionXs");
        this.dirtyRegionSectionYs = copy(dirtyRegionSectionYs, "dirtyRegionSectionYs");
        this.dirtyRegionSectionZs = copy(dirtyRegionSectionZs, "dirtyRegionSectionZs");
        this.dirtyRegionSectionScoped = copy(dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
        this.dirtyRegionGenerations = copy(dirtyRegionGenerations, "dirtyRegionGenerations");
        this.sectionDimensions = copy(sectionDimensions, "sectionDimensions");
        this.sectionXs = copy(sectionXs, "sectionXs");
        this.sectionYs = copy(sectionYs, "sectionYs");
        this.sectionZs = copy(sectionZs, "sectionZs");
        this.sectionCombinedGenerations = copy(sectionCombinedGenerations, "sectionCombinedGenerations");
        this.sectionGenerations = copy(sectionGenerations, "sectionGenerations");
        this.sectionMaterialGenerations = copy(sectionMaterialGenerations, "sectionMaterialGenerations");
        this.sectionOccupancyGenerations = copy(sectionOccupancyGenerations, "sectionOccupancyGenerations");
        this.sectionEmissiveGenerations = copy(sectionEmissiveGenerations, "sectionEmissiveGenerations");
        this.sectionDirtyRegionGenerations = copy(sectionDirtyRegionGenerations, "sectionDirtyRegionGenerations");
        this.occupiedVoxelCounts = copy(occupiedVoxelCounts, "occupiedVoxelCounts");
        this.opaqueVoxelCounts = copy(opaqueVoxelCounts, "opaqueVoxelCounts");
        this.translucentVoxelCounts = copy(translucentVoxelCounts, "translucentVoxelCounts");
        this.fluidVoxelCounts = copy(fluidVoxelCounts, "fluidVoxelCounts");
        this.emissiveVoxelCounts = copy(emissiveVoxelCounts, "emissiveVoxelCounts");
        this.voxelCounts = copy(voxelCounts, "voxelCounts");
        this.occupancyBitOrderIds = copy(occupancyBitOrderIds, "occupancyBitOrderIds");
        this.occupancyBitOrderNames = copy(occupancyBitOrderNames, "occupancyBitOrderNames");
        this.occupancyMaskWordOffsets = copy(occupancyMaskWordOffsets, "occupancyMaskWordOffsets");
        this.occupancyMaskWordCounts = copy(occupancyMaskWordCounts, "occupancyMaskWordCounts");
        this.occupancyMaskBitCounts = copy(occupancyMaskBitCounts, "occupancyMaskBitCounts");
        this.occupancyMaskGenerations = copy(occupancyMaskGenerations, "occupancyMaskGenerations");
        this.materialPaletteOffsets = copy(materialPaletteOffsets, "materialPaletteOffsets");
        this.materialPaletteGenerations = copy(materialPaletteGenerations, "materialPaletteGenerations");
        this.materialPalettePayloadOffsets = copy(materialPalettePayloadOffsets, "materialPalettePayloadOffsets");
        this.materialPalettePayloadCounts = copy(materialPalettePayloadCounts, "materialPalettePayloadCounts");
        this.materialPaletteIds = copy(materialPaletteIds, "materialPaletteIds");
        this.emissivePayloadOffsets = copy(emissivePayloadOffsets, "emissivePayloadOffsets");
        this.emissivePayloadCounts = copy(emissivePayloadCounts, "emissivePayloadCounts");
        this.emissiveVoxelIndices = copy(emissiveVoxelIndices, "emissiveVoxelIndices");
        this.emissiveMaterialIds = copy(emissiveMaterialIds, "emissiveMaterialIds");
        this.emissiveBlockLightLevels = copy(emissiveBlockLightLevels, "emissiveBlockLightLevels");
        this.emissiveEntryGenerations = copy(emissiveEntryGenerations, "emissiveEntryGenerations");

        this.validate();
    }

    public static NativeSectionSnapshotUploadPacket from(NativeStagedUploadBatch batch) {
        Objects.requireNonNull(batch, "batch");

        NativeUploadStagingMetadata metadata = batch.metadata();
        List<NativeSectionSnapshotUpload> sectionSnapshots = batch.sectionSnapshots();
        int sectionCount = sectionSnapshots.size();
        int materialPalettePayloadCount = 0;
        int emissivePayloadCount = 0;

        for (NativeSectionSnapshotUpload upload : sectionSnapshots) {
            materialPalettePayloadCount = checkedPayloadCount(
                    materialPalettePayloadCount,
                    upload.materialPaletteSize(),
                    "material palette payload"
            );
            emissivePayloadCount = checkedPayloadCount(
                    emissivePayloadCount,
                    upload.emissiveEntryCount(),
                    "emissive payload"
            );
        }

        String[] sectionDimensions = new String[sectionCount];
        int[] sectionXs = new int[sectionCount];
        int[] sectionYs = new int[sectionCount];
        int[] sectionZs = new int[sectionCount];
        long[] sectionCombinedGenerations = new long[sectionCount];
        long[] sectionGenerations = new long[sectionCount];
        long[] sectionMaterialGenerations = new long[sectionCount];
        long[] sectionOccupancyGenerations = new long[sectionCount];
        long[] sectionEmissiveGenerations = new long[sectionCount];
        long[] sectionDirtyRegionGenerations = new long[sectionCount];
        int[] occupiedVoxelCounts = new int[sectionCount];
        int[] opaqueVoxelCounts = new int[sectionCount];
        int[] translucentVoxelCounts = new int[sectionCount];
        int[] fluidVoxelCounts = new int[sectionCount];
        int[] emissiveVoxelCounts = new int[sectionCount];
        int[] occupancyBitOrderIds = new int[sectionCount];
        String[] occupancyBitOrderNames = new String[sectionCount];
        int[] occupancyMaskWordOffsets = new int[sectionCount];
        int[] occupancyMaskWordCounts = new int[sectionCount];
        int[] occupancyMaskBitCounts = new int[sectionCount];
        long[] occupancyMaskGenerations = new long[sectionCount];
        int[] materialPaletteOffsets = new int[sectionCount];
        long[] materialPaletteGenerations = new long[sectionCount];
        int[] materialPalettePayloadOffsets = new int[sectionCount];
        int[] materialPalettePayloadCounts = new int[sectionCount];
        int[] materialPaletteIds = new int[materialPalettePayloadCount];
        int[] emissivePayloadOffsets = new int[sectionCount];
        int[] emissivePayloadCounts = new int[sectionCount];
        int[] emissiveVoxelIndices = new int[emissivePayloadCount];
        int[] emissiveMaterialIds = new int[emissivePayloadCount];
        int[] emissiveBlockLightLevels = new int[emissivePayloadCount];
        long[] emissiveEntryGenerations = new long[emissivePayloadCount];
        int[] dirtyRegionTypeIds = new int[sectionCount];
        String[] dirtyRegionTypeNames = new String[sectionCount];
        String[] dirtyRegionDimensions = new String[sectionCount];
        int[] dirtyRegionSectionXs = new int[sectionCount];
        int[] dirtyRegionSectionYs = new int[sectionCount];
        int[] dirtyRegionSectionZs = new int[sectionCount];
        int[] dirtyRegionSectionScoped = new int[sectionCount];
        long[] dirtyRegionGenerations = new long[sectionCount];
        int[] voxelCounts = new int[sectionCount * 5];

        int materialPalettePayloadOffset = 0;
        int emissivePayloadOffset = 0;
        for (int sectionIndex = 0; sectionIndex < sectionSnapshots.size(); sectionIndex++) {
            NativeSectionSnapshotUpload upload = sectionSnapshots.get(sectionIndex);
            NativeDirtyRegionHandoff dirtyRegion = upload.dirtyRegionHandoff();
            dirtyRegionTypeIds[sectionIndex] = dirtyRegion.typeId();
            dirtyRegionTypeNames[sectionIndex] = dirtyRegion.typeName();
            dirtyRegionDimensions[sectionIndex] = dirtyRegion.dimension();
            dirtyRegionSectionXs[sectionIndex] = dirtyRegion.sectionX();
            dirtyRegionSectionYs[sectionIndex] = dirtyRegion.sectionY();
            dirtyRegionSectionZs[sectionIndex] = dirtyRegion.sectionZ();
            dirtyRegionSectionScoped[sectionIndex] = dirtyRegion.sectionScoped() ? 1 : 0;
            dirtyRegionGenerations[sectionIndex] = dirtyRegion.dirtyRegionGeneration();
            sectionDimensions[sectionIndex] = upload.dimension();
            sectionXs[sectionIndex] = upload.sectionX();
            sectionYs[sectionIndex] = upload.sectionY();
            sectionZs[sectionIndex] = upload.sectionZ();
            sectionCombinedGenerations[sectionIndex] = upload.combinedGeneration();
            sectionGenerations[sectionIndex] = upload.sectionGeneration();
            sectionMaterialGenerations[sectionIndex] = upload.materialGeneration();
            sectionOccupancyGenerations[sectionIndex] = upload.occupancyGeneration();
            sectionEmissiveGenerations[sectionIndex] = upload.emissiveGeneration();
            sectionDirtyRegionGenerations[sectionIndex] = upload.dirtyRegionGeneration();
            occupiedVoxelCounts[sectionIndex] = upload.occupiedVoxelCount();
            opaqueVoxelCounts[sectionIndex] = upload.opaqueVoxelCount();
            translucentVoxelCounts[sectionIndex] = upload.translucentVoxelCount();
            fluidVoxelCounts[sectionIndex] = upload.fluidVoxelCount();
            emissiveVoxelCounts[sectionIndex] = upload.emissiveVoxelCount();
            int voxelCountOffset = sectionIndex * 5;
            voxelCounts[voxelCountOffset] = upload.occupiedVoxelCount();
            voxelCounts[voxelCountOffset + 1] = upload.opaqueVoxelCount();
            voxelCounts[voxelCountOffset + 2] = upload.translucentVoxelCount();
            voxelCounts[voxelCountOffset + 3] = upload.fluidVoxelCount();
            voxelCounts[voxelCountOffset + 4] = upload.emissiveVoxelCount();
            occupancyBitOrderIds[sectionIndex] = upload.occupancyBitOrderId();
            occupancyBitOrderNames[sectionIndex] = upload.occupancyBitOrderName();
            occupancyMaskWordOffsets[sectionIndex] = upload.occupancyMaskWordOffset();
            occupancyMaskWordCounts[sectionIndex] = upload.occupancyMaskWordCount();
            occupancyMaskBitCounts[sectionIndex] = upload.occupancyMaskBitCount();
            occupancyMaskGenerations[sectionIndex] = upload.occupancyMaskGeneration();
            materialPaletteOffsets[sectionIndex] = upload.materialPaletteOffset();
            materialPaletteGenerations[sectionIndex] = upload.materialPaletteGeneration();

            int[] sectionMaterialPaletteIds = upload.materialPaletteIds();
            materialPalettePayloadOffsets[sectionIndex] = materialPalettePayloadOffset;
            materialPalettePayloadCounts[sectionIndex] = sectionMaterialPaletteIds.length;
            System.arraycopy(
                    sectionMaterialPaletteIds,
                    0,
                    materialPaletteIds,
                    materialPalettePayloadOffset,
                    sectionMaterialPaletteIds.length
            );
            materialPalettePayloadOffset += sectionMaterialPaletteIds.length;

            int[] sectionEmissiveVoxelIndices = upload.emissiveVoxelIndices();
            int[] sectionEmissiveMaterialIds = upload.emissiveMaterialIds();
            int[] sectionEmissiveBlockLightLevels = upload.emissiveBlockLightLevels();
            long[] sectionEmissiveEntryGenerations = upload.emissiveGenerations();
            emissivePayloadOffsets[sectionIndex] = emissivePayloadOffset;
            emissivePayloadCounts[sectionIndex] = sectionEmissiveVoxelIndices.length;
            System.arraycopy(
                    sectionEmissiveVoxelIndices,
                    0,
                    emissiveVoxelIndices,
                    emissivePayloadOffset,
                    sectionEmissiveVoxelIndices.length
            );
            System.arraycopy(
                    sectionEmissiveMaterialIds,
                    0,
                    emissiveMaterialIds,
                    emissivePayloadOffset,
                    sectionEmissiveMaterialIds.length
            );
            System.arraycopy(
                    sectionEmissiveBlockLightLevels,
                    0,
                    emissiveBlockLightLevels,
                    emissivePayloadOffset,
                    sectionEmissiveBlockLightLevels.length
            );
            System.arraycopy(
                    sectionEmissiveEntryGenerations,
                    0,
                    emissiveEntryGenerations,
                    emissivePayloadOffset,
                    sectionEmissiveEntryGenerations.length
            );
            emissivePayloadOffset += sectionEmissiveVoxelIndices.length;
        }

        return new NativeSectionSnapshotUploadPacket(
                metadata.generation(),
                metadata.sectionSnapshotCount(),
                metadata.firstSectionSnapshotGeneration(),
                metadata.lastSectionSnapshotGeneration(),
                metadata.sectionGeneration(),
                metadata.sectionMaterialGeneration(),
                metadata.sectionOccupancyGeneration(),
                metadata.sectionEmissiveGeneration(),
                metadata.sectionDirtyRegionGeneration(),
                dirtyRegionTypeIds,
                dirtyRegionTypeNames,
                dirtyRegionDimensions,
                dirtyRegionSectionXs,
                dirtyRegionSectionYs,
                dirtyRegionSectionZs,
                dirtyRegionSectionScoped,
                dirtyRegionGenerations,
                sectionDimensions,
                sectionXs,
                sectionYs,
                sectionZs,
                sectionCombinedGenerations,
                sectionGenerations,
                sectionMaterialGenerations,
                sectionOccupancyGenerations,
                sectionEmissiveGenerations,
                sectionDirtyRegionGenerations,
                occupiedVoxelCounts,
                opaqueVoxelCounts,
                translucentVoxelCounts,
                fluidVoxelCounts,
                emissiveVoxelCounts,
                voxelCounts,
                occupancyBitOrderIds,
                occupancyBitOrderNames,
                occupancyMaskWordOffsets,
                occupancyMaskWordCounts,
                occupancyMaskBitCounts,
                occupancyMaskGenerations,
                materialPaletteOffsets,
                materialPaletteGenerations,
                materialPalettePayloadOffsets,
                materialPalettePayloadCounts,
                materialPaletteIds,
                emissivePayloadOffsets,
                emissivePayloadCounts,
                emissiveVoxelIndices,
                emissiveMaterialIds,
                emissiveBlockLightLevels,
                emissiveEntryGenerations
        );
    }

    public long generation() {
        return this.generation;
    }

    public int sectionSnapshotCount() {
        return this.sectionSnapshotCount;
    }

    public long firstSectionSnapshotGeneration() {
        return this.firstSectionSnapshotGeneration;
    }

    public long lastSectionSnapshotGeneration() {
        return this.lastSectionSnapshotGeneration;
    }

    public long sectionGeneration() {
        return this.sectionGeneration;
    }

    public long sectionMaterialGeneration() {
        return this.sectionMaterialGeneration;
    }

    public long sectionOccupancyGeneration() {
        return this.sectionOccupancyGeneration;
    }

    public long sectionEmissiveGeneration() {
        return this.sectionEmissiveGeneration;
    }

    public long sectionDirtyRegionGeneration() {
        return this.sectionDirtyRegionGeneration;
    }

    public int sectionPayloadCount() {
        return this.sectionDimensions.length;
    }

    public int materialPalettePayloadCount() {
        return this.materialPaletteIds.length;
    }

    public int emissivePayloadCount() {
        return this.emissiveVoxelIndices.length;
    }

    public boolean isEmpty() {
        return this.sectionSnapshotCount == 0;
    }

    public boolean hasPayloads() {
        return this.sectionDimensions.length > 0;
    }

    public int[] dirtyRegionTypeIds() {
        return copy(this.dirtyRegionTypeIds, "dirtyRegionTypeIds");
    }

    public String[] dirtyRegionTypeNames() {
        return copy(this.dirtyRegionTypeNames, "dirtyRegionTypeNames");
    }

    public String[] dirtyRegionDimensions() {
        return copy(this.dirtyRegionDimensions, "dirtyRegionDimensions");
    }

    public int[] dirtyRegionSectionXs() {
        return copy(this.dirtyRegionSectionXs, "dirtyRegionSectionXs");
    }

    public int[] dirtyRegionSectionYs() {
        return copy(this.dirtyRegionSectionYs, "dirtyRegionSectionYs");
    }

    public int[] dirtyRegionSectionZs() {
        return copy(this.dirtyRegionSectionZs, "dirtyRegionSectionZs");
    }

    public int[] dirtyRegionSectionScoped() {
        return copy(this.dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
    }

    public long[] dirtyRegionGenerations() {
        return copy(this.dirtyRegionGenerations, "dirtyRegionGenerations");
    }

    public String[] sectionDimensions() {
        return copy(this.sectionDimensions, "sectionDimensions");
    }

    public int[] sectionXs() {
        return copy(this.sectionXs, "sectionXs");
    }

    public int[] sectionYs() {
        return copy(this.sectionYs, "sectionYs");
    }

    public int[] sectionZs() {
        return copy(this.sectionZs, "sectionZs");
    }

    public long[] sectionCombinedGenerations() {
        return copy(this.sectionCombinedGenerations, "sectionCombinedGenerations");
    }

    public long[] sectionGenerations() {
        return copy(this.sectionGenerations, "sectionGenerations");
    }

    public long[] sectionMaterialGenerations() {
        return copy(this.sectionMaterialGenerations, "sectionMaterialGenerations");
    }

    public long[] sectionOccupancyGenerations() {
        return copy(this.sectionOccupancyGenerations, "sectionOccupancyGenerations");
    }

    public long[] sectionEmissiveGenerations() {
        return copy(this.sectionEmissiveGenerations, "sectionEmissiveGenerations");
    }

    public long[] sectionDirtyRegionGenerations() {
        return copy(this.sectionDirtyRegionGenerations, "sectionDirtyRegionGenerations");
    }

    public int[] occupiedVoxelCounts() {
        return copy(this.occupiedVoxelCounts, "occupiedVoxelCounts");
    }

    public int[] opaqueVoxelCounts() {
        return copy(this.opaqueVoxelCounts, "opaqueVoxelCounts");
    }

    public int[] translucentVoxelCounts() {
        return copy(this.translucentVoxelCounts, "translucentVoxelCounts");
    }

    public int[] fluidVoxelCounts() {
        return copy(this.fluidVoxelCounts, "fluidVoxelCounts");
    }

    public int[] emissiveVoxelCounts() {
        return copy(this.emissiveVoxelCounts, "emissiveVoxelCounts");
    }

    public int[] voxelCounts() {
        return copy(this.voxelCounts, "voxelCounts");
    }

    public int[] occupancyBitOrderIds() {
        return copy(this.occupancyBitOrderIds, "occupancyBitOrderIds");
    }

    public String[] occupancyBitOrderNames() {
        return copy(this.occupancyBitOrderNames, "occupancyBitOrderNames");
    }

    public int[] occupancyMaskWordOffsets() {
        return copy(this.occupancyMaskWordOffsets, "occupancyMaskWordOffsets");
    }

    public int[] occupancyMaskWordCounts() {
        return copy(this.occupancyMaskWordCounts, "occupancyMaskWordCounts");
    }

    public int[] occupancyMaskBitCounts() {
        return copy(this.occupancyMaskBitCounts, "occupancyMaskBitCounts");
    }

    public long[] occupancyMaskGenerations() {
        return copy(this.occupancyMaskGenerations, "occupancyMaskGenerations");
    }

    public int[] materialPaletteOffsets() {
        return copy(this.materialPaletteOffsets, "materialPaletteOffsets");
    }

    public long[] materialPaletteGenerations() {
        return copy(this.materialPaletteGenerations, "materialPaletteGenerations");
    }

    public int[] materialPalettePayloadOffsets() {
        return copy(this.materialPalettePayloadOffsets, "materialPalettePayloadOffsets");
    }

    public int[] materialPalettePayloadCounts() {
        return copy(this.materialPalettePayloadCounts, "materialPalettePayloadCounts");
    }

    public int[] materialPaletteIds() {
        return copy(this.materialPaletteIds, "materialPaletteIds");
    }

    public int[] emissivePayloadOffsets() {
        return copy(this.emissivePayloadOffsets, "emissivePayloadOffsets");
    }

    public int[] emissivePayloadCounts() {
        return copy(this.emissivePayloadCounts, "emissivePayloadCounts");
    }

    public int[] emissiveVoxelIndices() {
        return copy(this.emissiveVoxelIndices, "emissiveVoxelIndices");
    }

    public int[] emissiveMaterialIds() {
        return copy(this.emissiveMaterialIds, "emissiveMaterialIds");
    }

    public int[] emissiveBlockLightLevels() {
        return copy(this.emissiveBlockLightLevels, "emissiveBlockLightLevels");
    }

    public long[] emissiveEntryGenerations() {
        return copy(this.emissiveEntryGenerations, "emissiveEntryGenerations");
    }

    private void validate() {
        requireNonNegative(this.generation, "generation");
        requireNonNegative(this.sectionSnapshotCount, "sectionSnapshotCount");
        requireNonNegative(this.firstSectionSnapshotGeneration, "firstSectionSnapshotGeneration");
        requireNonNegative(this.lastSectionSnapshotGeneration, "lastSectionSnapshotGeneration");
        requireNonNegative(this.sectionGeneration, "sectionGeneration");
        requireNonNegative(this.sectionMaterialGeneration, "sectionMaterialGeneration");
        requireNonNegative(this.sectionOccupancyGeneration, "sectionOccupancyGeneration");
        requireNonNegative(this.sectionEmissiveGeneration, "sectionEmissiveGeneration");
        requireNonNegative(this.sectionDirtyRegionGeneration, "sectionDirtyRegionGeneration");
        if (this.firstSectionSnapshotGeneration > this.lastSectionSnapshotGeneration) {
            throw new IllegalArgumentException(
                    "firstSectionSnapshotGeneration must be less than or equal to lastSectionSnapshotGeneration"
            );
        }
        if (this.sectionSnapshotCount == 0
                && (this.firstSectionSnapshotGeneration != 0 || this.lastSectionSnapshotGeneration != 0)) {
            throw new IllegalArgumentException("empty section packet must use zero section generation bounds");
        }

        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionTypeIds", this.dirtyRegionTypeIds.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionTypeNames", this.dirtyRegionTypeNames.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionDimensions", this.dirtyRegionDimensions.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionSectionXs", this.dirtyRegionSectionXs.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionSectionYs", this.dirtyRegionSectionYs.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionSectionZs", this.dirtyRegionSectionZs.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionSectionScoped", this.dirtyRegionSectionScoped.length);
        requireMatchingLength(this.sectionSnapshotCount, "dirtyRegionGenerations", this.dirtyRegionGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionDimensions", this.sectionDimensions.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionXs", this.sectionXs.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionYs", this.sectionYs.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionZs", this.sectionZs.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionCombinedGenerations", this.sectionCombinedGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionGenerations", this.sectionGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionMaterialGenerations", this.sectionMaterialGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionOccupancyGenerations", this.sectionOccupancyGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionEmissiveGenerations", this.sectionEmissiveGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "sectionDirtyRegionGenerations", this.sectionDirtyRegionGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupiedVoxelCounts", this.occupiedVoxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "opaqueVoxelCounts", this.opaqueVoxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "translucentVoxelCounts", this.translucentVoxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "fluidVoxelCounts", this.fluidVoxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "emissiveVoxelCounts", this.emissiveVoxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount * 5, "voxelCounts", this.voxelCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyBitOrderIds", this.occupancyBitOrderIds.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyBitOrderNames", this.occupancyBitOrderNames.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyMaskWordOffsets", this.occupancyMaskWordOffsets.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyMaskWordCounts", this.occupancyMaskWordCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyMaskBitCounts", this.occupancyMaskBitCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "occupancyMaskGenerations", this.occupancyMaskGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "materialPaletteOffsets", this.materialPaletteOffsets.length);
        requireMatchingLength(this.sectionSnapshotCount, "materialPaletteGenerations", this.materialPaletteGenerations.length);
        requireMatchingLength(this.sectionSnapshotCount, "materialPalettePayloadOffsets", this.materialPalettePayloadOffsets.length);
        requireMatchingLength(this.sectionSnapshotCount, "materialPalettePayloadCounts", this.materialPalettePayloadCounts.length);
        requireMatchingLength(this.sectionSnapshotCount, "emissivePayloadOffsets", this.emissivePayloadOffsets.length);
        requireMatchingLength(this.sectionSnapshotCount, "emissivePayloadCounts", this.emissivePayloadCounts.length);
        requireMatchingLength(this.emissiveVoxelIndices.length, "emissiveMaterialIds", this.emissiveMaterialIds.length);
        requireMatchingLength(this.emissiveVoxelIndices.length, "emissiveBlockLightLevels", this.emissiveBlockLightLevels.length);
        requireMatchingLength(this.emissiveVoxelIndices.length, "emissiveEntryGenerations", this.emissiveEntryGenerations.length);

        requirePayloadWindowCoverage(
                this.materialPalettePayloadOffsets,
                this.materialPalettePayloadCounts,
                this.materialPaletteIds.length,
                "material palette"
        );
        requirePayloadWindowCoverage(
                this.emissivePayloadOffsets,
                this.emissivePayloadCounts,
                this.emissiveVoxelIndices.length,
                "emissive"
        );

        for (int index = 0; index < this.sectionSnapshotCount; index++) {
            if (this.dirtyRegionTypeIds[index] <= 0) {
                throw new IllegalArgumentException("dirtyRegionTypeIds entries must be positive");
            }
            requireText(this.dirtyRegionTypeNames[index], "dirtyRegionTypeNames entries");
            requireText(this.dirtyRegionDimensions[index], "dirtyRegionDimensions entries");
            int sectionScoped = this.dirtyRegionSectionScoped[index];
            if (sectionScoped != 0 && sectionScoped != 1) {
                throw new IllegalArgumentException("dirtyRegionSectionScoped entries must be 0 or 1");
            }
            requireNonNegative(this.dirtyRegionGenerations[index], "dirtyRegionGenerations entries");

            requireText(this.sectionDimensions[index], "sectionDimensions entries");
            if (!this.dirtyRegionDimensions[index].equals(this.sectionDimensions[index])
                    || this.dirtyRegionSectionXs[index] != this.sectionXs[index]
                    || this.dirtyRegionSectionYs[index] != this.sectionYs[index]
                    || this.dirtyRegionSectionZs[index] != this.sectionZs[index]) {
                throw new IllegalArgumentException("dirty region handoff entries must match section origins");
            }

            int voxelCountOffset = index * 5;
            requireMatchingValue(this.occupiedVoxelCounts[index], "occupiedVoxelCounts", this.voxelCounts[voxelCountOffset]);
            requireMatchingValue(this.opaqueVoxelCounts[index], "opaqueVoxelCounts", this.voxelCounts[voxelCountOffset + 1]);
            requireMatchingValue(this.translucentVoxelCounts[index], "translucentVoxelCounts", this.voxelCounts[voxelCountOffset + 2]);
            requireMatchingValue(this.fluidVoxelCounts[index], "fluidVoxelCounts", this.voxelCounts[voxelCountOffset + 3]);
            requireMatchingValue(this.emissiveVoxelCounts[index], "emissiveVoxelCounts", this.voxelCounts[voxelCountOffset + 4]);
        }
        for (String bitOrderName : this.occupancyBitOrderNames) {
            requireText(bitOrderName, "occupancyBitOrderNames entries");
        }
        for (int materialPaletteId : this.materialPaletteIds) {
            if (materialPaletteId <= 0) {
                throw new IllegalArgumentException("materialPaletteIds must contain positive ids");
            }
        }
        for (int index = 0; index < this.emissiveVoxelIndices.length; index++) {
            if (this.emissiveMaterialIds[index] <= 0) {
                throw new IllegalArgumentException("emissiveMaterialIds must contain positive ids");
            }
            int blockLightLevel = this.emissiveBlockLightLevels[index];
            if (blockLightLevel < 0 || blockLightLevel > 15) {
                throw new IllegalArgumentException("emissiveBlockLightLevels entries must be between 0 and 15");
            }
            requireNonNegative(this.emissiveEntryGenerations[index], "emissiveEntryGenerations entries");
        }
    }

    private static void requirePayloadWindowCoverage(int[] offsets, int[] counts, int payloadLength, String name) {
        int cursor = 0;
        for (int index = 0; index < offsets.length; index++) {
            int offset = offsets[index];
            int count = counts[index];
            if (offset < 0) {
                throw new IllegalArgumentException(name + " payload offsets must be non-negative");
            }
            if (count < 0) {
                throw new IllegalArgumentException(name + " payload counts must be non-negative");
            }
            if (offset != cursor) {
                throw new IllegalArgumentException(name + " payload offsets must be contiguous");
            }
            cursor = checkedPayloadCount(cursor, count, name);
            if (cursor > payloadLength) {
                throw new IllegalArgumentException(name + " payload windows exceed flattened payload length");
            }
        }
        if (cursor != payloadLength) {
            throw new IllegalArgumentException(name + " payload windows must cover flattened payload length");
        }
    }

    private static int checkedPayloadCount(int current, int increment, String name) {
        if (increment < 0) {
            throw new IllegalArgumentException(name + " count must be non-negative");
        }
        try {
            return Math.addExact(current, increment);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " count exceeds supported packet array length", exception);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
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

    private static void requireMatchingValue(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " packed voxel count mismatch");
        }
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
