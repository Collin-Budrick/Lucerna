package net.lucerna.render.tracing.voxel;

import net.lucerna.render.voxel.VoxelDdaConfig;
import net.lucerna.render.voxel.VoxelRayBudgetConfig;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;

import java.util.List;
import java.util.Objects;

public record Round10VoxelTraversalInputSummary(
        long frameIndex,
        long sourceGeneration,
        int sectionCount,
        int occupiedSectionCount,
        int emptySectionSkipCandidates,
        long occupiedVoxelCount,
        long opaqueVoxelCount,
        long translucentVoxelCount,
        long emissiveVoxelCount,
        int materialPaletteSectionCount,
        long materialPaletteEntryCount,
        Round10VoxelOccupancyMaskSummary occupancyMaskSummary,
        VoxelDdaConfig ddaConfig,
        VoxelRayBudgetConfig rayBudget,
        String source
) {
    public Round10VoxelTraversalInputSummary {
        requireNonNegative(frameIndex, "frameIndex");
        requireNonNegative(sourceGeneration, "sourceGeneration");
        requireNonNegative(sectionCount, "sectionCount");
        requireNonNegative(occupiedSectionCount, "occupiedSectionCount");
        requireNonNegative(emptySectionSkipCandidates, "emptySectionSkipCandidates");
        requireNonNegative(occupiedVoxelCount, "occupiedVoxelCount");
        requireNonNegative(opaqueVoxelCount, "opaqueVoxelCount");
        requireNonNegative(translucentVoxelCount, "translucentVoxelCount");
        requireNonNegative(emissiveVoxelCount, "emissiveVoxelCount");
        requireNonNegative(materialPaletteSectionCount, "materialPaletteSectionCount");
        requireNonNegative(materialPaletteEntryCount, "materialPaletteEntryCount");
        Objects.requireNonNull(occupancyMaskSummary, "occupancyMaskSummary");
        Objects.requireNonNull(ddaConfig, "ddaConfig");
        Objects.requireNonNull(rayBudget, "rayBudget");
        source = requireText(source, "source");
        if (occupiedSectionCount > sectionCount) {
            throw new IllegalArgumentException("occupiedSectionCount cannot exceed sectionCount");
        }
        if (emptySectionSkipCandidates > sectionCount) {
            throw new IllegalArgumentException("emptySectionSkipCandidates cannot exceed sectionCount");
        }
        if (materialPaletteSectionCount > occupiedSectionCount) {
            throw new IllegalArgumentException("materialPaletteSectionCount cannot exceed occupiedSectionCount");
        }
    }

    public static Round10VoxelTraversalInputSummary fromSectionSnapshots(
            long frameIndex,
            List<VoxelSectionSnapshotReference> sections,
            VoxelDdaConfig ddaConfig,
            VoxelRayBudgetConfig rayBudget
    ) {
        Objects.requireNonNull(sections, "sections");
        Objects.requireNonNull(ddaConfig, "ddaConfig");
        Objects.requireNonNull(rayBudget, "rayBudget");

        int occupiedSections = 0;
        int emptySections = 0;
        int maskSections = 0;
        int readyMaskSections = 0;
        int metadataOnlyMaskSections = 0;
        int missingMaskSections = 0;
        int materialPaletteSections = 0;
        long occupiedVoxels = 0L;
        long opaqueVoxels = 0L;
        long translucentVoxels = 0L;
        long emissiveVoxels = 0L;
        long maskWords = 0L;
        long maskBits = 0L;
        long paletteEntries = 0L;
        long generation = 0L;

        for (VoxelSectionSnapshotReference section : sections) {
            Objects.requireNonNull(section, "sections must not contain null entries");
            generation = Math.max(generation, section.combinedGeneration());
            if (!section.hasOccupiedVoxels()) {
                emptySections++;
                continue;
            }
            occupiedSections++;
            occupiedVoxels += section.occupiedVoxelCount();
            opaqueVoxels += section.opaqueVoxelCount();
            translucentVoxels += section.translucentVoxelCount();
            emissiveVoxels += section.emissiveVoxelCount();
            if (section.hasOccupancyMask()) {
                maskSections++;
                maskWords += section.occupancyMaskWordCount();
                maskBits += section.occupancyMaskBitCount();
                if (section.hasConcreteOccupancyMaskPayload()) {
                    readyMaskSections++;
                }
                if (section.occupancyMaskMetadataOnly()) {
                    metadataOnlyMaskSections++;
                }
            } else {
                missingMaskSections++;
            }
            if (section.hasMaterialPalette()) {
                materialPaletteSections++;
                paletteEntries += section.materialPaletteSize();
            }
        }

        return new Round10VoxelTraversalInputSummary(
                frameIndex,
                generation,
                sections.size(),
                occupiedSections,
                emptySections,
                occupiedVoxels,
                opaqueVoxels,
                translucentVoxels,
                emissiveVoxels,
                materialPaletteSections,
                paletteEntries,
                new Round10VoxelOccupancyMaskSummary(
                        sections.size(),
                        occupiedSections,
                        maskSections,
                        readyMaskSections,
                        metadataOnlyMaskSections,
                        missingMaskSections,
                        maskWords,
                        maskBits,
                        generation
                ),
                ddaConfig,
                rayBudget,
                "section_snapshot_metadata"
        );
    }

    public boolean hasWorldData() {
        return this.occupiedSectionCount > 0 && this.occupiedVoxelCount > 0;
    }

    public boolean hasMaterialMetadata() {
        return this.materialPaletteSectionCount > 0 && this.materialPaletteEntryCount > 0;
    }

    public boolean maskBitsReady() {
        return this.occupancyMaskSummary.maskBitsReady();
    }

    public String maskBitsSource() {
        return this.occupancyMaskSummary.maskBitsSource();
    }

    public boolean emptySectionSkipSafe() {
        return this.emptySectionSkipCandidates <= this.sectionCount
                && this.emptySectionSkipCandidates + this.occupiedSectionCount <= this.sectionCount;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
