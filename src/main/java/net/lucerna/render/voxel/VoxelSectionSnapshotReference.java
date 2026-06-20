package net.lucerna.render.voxel;

import net.lucerna.upload.NativeSectionSnapshotUpload;
import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionMaterialPaletteReference;
import net.lucerna.world.section.SectionSurfaceSampleMetadata;
import net.lucerna.world.section.VoxelOccupancyBitOrder;
import net.lucerna.world.section.VoxelOccupancyMaskMetadata;
import net.lucerna.world.section.VoxelOccupancyMaskSource;
import net.lucerna.world.section.VoxelOccupancySummary;

import java.util.List;
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
        boolean hasEmissivePayload,
        List<SectionSurfaceSampleMetadata> surfaceSamples,
        int solidWallHitEvidenceCount,
        int openSkyMissEvidenceCount,
        int glassVoxelCount,
        int waterVoxelCount,
        int opaqueMaterialFlagCount,
        boolean occupancyMaskBitsReady,
        VoxelOccupancyMaskSource occupancyMaskSource,
        boolean emptySectionSkipSafe,
        boolean materialLookupReady,
        boolean opaqueMaterialFlagsReady,
        boolean glassMaterialFlagsReady,
        boolean waterMaterialFlagsReady
) {
    public VoxelSectionSnapshotReference(
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
            boolean hasEmissivePayload,
            List<SectionSurfaceSampleMetadata> surfaceSamples
    ) {
        this(
                origin,
                generation,
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount,
                occupancyBitOrder,
                occupancyMaskWordOffset,
                occupancyMaskWordCount,
                occupancyMaskBitCount,
                occupancyMaskGeneration,
                materialPaletteOffset,
                materialPaletteSize,
                materialGeneration,
                hasEmissivePayload,
                surfaceSamples,
                0,
                0,
                0,
                0,
                opaqueVoxelCount,
                false,
                occupancyMaskWordCount > 0 && occupancyMaskBitCount > 0
                        ? VoxelOccupancyMaskSource.METADATA_ONLY
                        : VoxelOccupancyMaskSource.NONE,
                occupiedVoxelCount == 0
                        && occupancyMaskWordCount == 0
                        && materialPaletteSize == 0
                        && !hasEmissivePayload
                        && surfaceSamples.isEmpty(),
                materialPaletteSize > 0,
                false,
                false,
                false
        );
    }

    public VoxelSectionSnapshotReference {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(generation, "generation");
        Objects.requireNonNull(occupancyBitOrder, "occupancyBitOrder");
        Objects.requireNonNull(occupancyMaskSource, "occupancyMaskSource");
        Objects.requireNonNull(surfaceSamples, "surfaceSamples");
        surfaceSamples = List.copyOf(surfaceSamples);
        requireVoxelCount(occupiedVoxelCount, "occupiedVoxelCount");
        requireVoxelCount(opaqueVoxelCount, "opaqueVoxelCount");
        requireVoxelCount(translucentVoxelCount, "translucentVoxelCount");
        requireVoxelCount(fluidVoxelCount, "fluidVoxelCount");
        requireVoxelCount(emissiveVoxelCount, "emissiveVoxelCount");
        requireVoxelCount(solidWallHitEvidenceCount, "solidWallHitEvidenceCount");
        requireVoxelCount(openSkyMissEvidenceCount, "openSkyMissEvidenceCount");
        requireVoxelCount(glassVoxelCount, "glassVoxelCount");
        requireVoxelCount(waterVoxelCount, "waterVoxelCount");
        requireVoxelCount(opaqueMaterialFlagCount, "opaqueMaterialFlagCount");
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
        if (surfaceSamples.size() > occupiedVoxelCount) {
            throw new IllegalArgumentException("surface sample count cannot exceed occupiedVoxelCount");
        }
        if (occupancyMaskBitsReady && (occupancyMaskWordCount == 0 || occupancyMaskBitCount == 0)) {
            throw new IllegalArgumentException("occupancyMaskBitsReady requires non-empty mask storage metadata");
        }
        if (!occupancyMaskBitsReady
                && occupancyMaskSource != VoxelOccupancyMaskSource.NONE
                && occupancyMaskSource != VoxelOccupancyMaskSource.METADATA_ONLY) {
            throw new IllegalArgumentException("non-ready occupancy mask bits cannot claim a concrete source");
        }
        if (emptySectionSkipSafe && hasOccupiedSectionPayload(
                occupiedVoxelCount,
                occupancyMaskBitsReady,
                materialLookupReady,
                hasEmissivePayload,
                surfaceSamples
        )) {
            throw new IllegalArgumentException("emptySectionSkipSafe cannot be true while traversal payload is present");
        }
        if (materialLookupReady && materialPaletteSize == 0) {
            throw new IllegalArgumentException("materialLookupReady requires a material palette");
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
                !snapshot.emissiveEntries().isEmpty(),
                snapshot.surfaceSamples(),
                summary.solidWallHitEvidenceCount(),
                summary.openSkyMissEvidenceCount(),
                summary.glassVoxelCount(),
                summary.waterVoxelCount(),
                summary.opaqueMaterialFlagCount(),
                occupancyMask.readyForTraversal(),
                occupancyMask.source(),
                snapshot.emptySectionSkipSafe(),
                materialPalette.readyForMaterialLookup(),
                materialPalette.opaqueFlagsReady(),
                materialPalette.glassFlagsReady(),
                materialPalette.waterFlagsReady()
        );
    }

    public static VoxelSectionSnapshotReference from(NativeSectionSnapshotUpload upload) {
        Objects.requireNonNull(upload, "upload");
        VoxelOccupancyBitOrder occupancyBitOrder = occupancyBitOrder(upload);
        return new VoxelSectionSnapshotReference(
                new ChunkSectionOrigin(
                        upload.dimension(),
                        upload.sectionX(),
                        upload.sectionY(),
                        upload.sectionZ()
                ),
                new ChunkSectionGeneration(
                        upload.sectionGeneration(),
                        upload.materialGeneration(),
                        upload.occupancyGeneration(),
                        upload.emissiveGeneration()
                ),
                upload.occupiedVoxelCount(),
                upload.opaqueVoxelCount(),
                upload.translucentVoxelCount(),
                upload.fluidVoxelCount(),
                upload.emissiveVoxelCount(),
                occupancyBitOrder,
                upload.occupancyMaskWordOffset(),
                upload.occupancyMaskWordCount(),
                upload.occupancyMaskBitCount(),
                upload.occupancyMaskGeneration(),
                upload.materialPaletteOffset(),
                upload.materialPaletteSize(),
                upload.materialPaletteGeneration(),
                upload.emissiveEntryCount() > 0,
                List.of(),
                0,
                0,
                0,
                0,
                upload.opaqueVoxelCount(),
                upload.occupancyMaskWordCount() > 0 && upload.occupancyMaskBitCount() > 0,
                upload.occupancyMaskWordCount() > 0 && upload.occupancyMaskBitCount() > 0
                        ? VoxelOccupancyMaskSource.NATIVE_UPLOAD
                        : VoxelOccupancyMaskSource.NONE,
                upload.occupiedVoxelCount() == 0
                        && upload.occupancyMaskWordCount() == 0
                        && upload.materialPaletteSize() == 0
                        && upload.emissiveEntryCount() == 0,
                upload.materialPaletteSize() > 0,
                false,
                false,
                false
        );
    }

    public static List<VoxelSectionSnapshotReference> fromSnapshots(List<ChunkSectionVoxelSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots");
        return snapshots.stream()
                .map(VoxelSectionSnapshotReference::from)
                .toList();
    }

    public static List<VoxelSectionSnapshotReference> fromUploads(List<NativeSectionSnapshotUpload> uploads) {
        Objects.requireNonNull(uploads, "uploads");
        return uploads.stream()
                .map(VoxelSectionSnapshotReference::from)
                .toList();
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

    public boolean hasOccupancyMaskReadyForTraversal() {
        return this.occupancyMaskBitsReady;
    }

    public boolean occupancyMaskMetadataOnly() {
        return this.occupancyMaskSource.metadataOnly();
    }

    public boolean hasMaterialPalette() {
        return this.materialPaletteSize > 0;
    }

    public boolean hasMaterialLookupReady() {
        return this.materialLookupReady;
    }

    public boolean hasSolidWallHitEvidence() {
        return this.solidWallHitEvidenceCount > 0;
    }

    public boolean hasOpenSkyMissEvidence() {
        return this.openSkyMissEvidenceCount > 0;
    }

    public boolean hasOpaqueMaterialFlags() {
        return this.opaqueMaterialFlagsReady && this.opaqueMaterialFlagCount > 0;
    }

    public boolean hasGlassMaterialFlags() {
        return this.glassMaterialFlagsReady && this.glassVoxelCount > 0;
    }

    public boolean hasWaterMaterialFlags() {
        return this.waterMaterialFlagsReady && this.waterVoxelCount > 0;
    }

    public boolean hasSurfaceSamples() {
        return !this.surfaceSamples.isEmpty();
    }

    public VoxelSectionSnapshotReference withSurfaceSamples(List<SectionSurfaceSampleMetadata> surfaceSamples) {
        return new VoxelSectionSnapshotReference(
                this.origin,
                this.generation,
                this.occupiedVoxelCount,
                this.opaqueVoxelCount,
                this.translucentVoxelCount,
                this.fluidVoxelCount,
                this.emissiveVoxelCount,
                this.occupancyBitOrder,
                this.occupancyMaskWordOffset,
                this.occupancyMaskWordCount,
                this.occupancyMaskBitCount,
                this.occupancyMaskGeneration,
                this.materialPaletteOffset,
                this.materialPaletteSize,
                this.materialGeneration,
                this.hasEmissivePayload,
                surfaceSamples,
                this.solidWallHitEvidenceCount,
                this.openSkyMissEvidenceCount,
                this.glassVoxelCount,
                this.waterVoxelCount,
                this.opaqueMaterialFlagCount,
                this.occupancyMaskBitsReady,
                this.occupancyMaskSource,
                this.emptySectionSkipSafe,
                this.materialLookupReady,
                this.opaqueMaterialFlagsReady,
                this.glassMaterialFlagsReady,
                this.waterMaterialFlagsReady
        );
    }

    public boolean hasTraversalPayload() {
        return this.hasOccupiedVoxels()
                || this.hasOccupancyMask()
                || this.hasMaterialPalette()
                || this.hasEmissivePayload
                || this.hasSurfaceSamples();
    }

    public boolean hasKnownSceneValidationEvidence() {
        return this.hasSolidWallHitEvidence()
                || this.hasOpenSkyMissEvidence()
                || this.hasGlassMaterialFlags()
                || this.hasWaterMaterialFlags()
                || this.hasOpaqueMaterialFlags()
                || this.emptySectionSkipSafe;
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

    private static VoxelOccupancyBitOrder occupancyBitOrder(NativeSectionSnapshotUpload upload) {
        VoxelOccupancyBitOrder occupancyBitOrder;
        try {
            occupancyBitOrder = VoxelOccupancyBitOrder.valueOf(upload.occupancyBitOrderName());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("upload occupancyBitOrderName is not supported by voxel traversal", exception);
        }
        if (occupancyBitOrder.ordinal() + 1 != upload.occupancyBitOrderId()) {
            throw new IllegalArgumentException("upload occupancyBitOrderId does not match occupancyBitOrderName");
        }
        return occupancyBitOrder;
    }

    private static boolean hasOccupiedSectionPayload(
            int occupiedVoxelCount,
            boolean occupancyMaskBitsReady,
            boolean materialLookupReady,
            boolean hasEmissivePayload,
            List<SectionSurfaceSampleMetadata> surfaceSamples
    ) {
        return occupiedVoxelCount > 0
                || occupancyMaskBitsReady
                || materialLookupReady
                || hasEmissivePayload
                || !surfaceSamples.isEmpty();
    }
}
