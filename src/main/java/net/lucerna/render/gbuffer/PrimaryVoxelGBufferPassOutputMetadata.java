package net.lucerna.render.gbuffer;

import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.render.voxel.VoxelTraversalRequest;

import java.util.List;
import java.util.Objects;

public record PrimaryVoxelGBufferPassOutputMetadata(
        List<GBufferAttachmentWriteIntent> expectedAttachmentWrites,
        long requestGeneration,
        int sectionCount,
        long occupiedVoxelCount,
        long opaqueVoxelCount,
        long translucentVoxelCount,
        long fluidVoxelCount,
        long emissiveVoxelCount,
        int materialPayloadSectionCount,
        long materialPaletteEntryCount,
        int emissivePayloadSectionCount,
        boolean hasMaterialPayload,
        boolean hasEmissivePayload,
        boolean requiresHistory
) {
    public PrimaryVoxelGBufferPassOutputMetadata {
        Objects.requireNonNull(expectedAttachmentWrites, "expectedAttachmentWrites");
        expectedAttachmentWrites = List.copyOf(expectedAttachmentWrites);
        for (GBufferAttachmentWriteIntent expectedAttachmentWrite : expectedAttachmentWrites) {
            Objects.requireNonNull(expectedAttachmentWrite, "expectedAttachmentWrites must not contain null entries");
        }
        requireNonNegative(requestGeneration, "requestGeneration");
        requireNonNegative(sectionCount, "sectionCount");
        requireNonNegative(occupiedVoxelCount, "occupiedVoxelCount");
        requireNonNegative(opaqueVoxelCount, "opaqueVoxelCount");
        requireNonNegative(translucentVoxelCount, "translucentVoxelCount");
        requireNonNegative(fluidVoxelCount, "fluidVoxelCount");
        requireNonNegative(emissiveVoxelCount, "emissiveVoxelCount");
        requireNonNegative(materialPayloadSectionCount, "materialPayloadSectionCount");
        requireNonNegative(materialPaletteEntryCount, "materialPaletteEntryCount");
        requireNonNegative(emissivePayloadSectionCount, "emissivePayloadSectionCount");
        if (materialPayloadSectionCount > sectionCount) {
            throw new IllegalArgumentException("materialPayloadSectionCount cannot exceed sectionCount");
        }
        if (emissivePayloadSectionCount > sectionCount) {
            throw new IllegalArgumentException("emissivePayloadSectionCount cannot exceed sectionCount");
        }
        if (opaqueVoxelCount + translucentVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("opaque and translucent counts cannot exceed occupiedVoxelCount");
        }
        if (fluidVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("fluidVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (emissiveVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("emissiveVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (hasMaterialPayload != (materialPayloadSectionCount > 0 || materialPaletteEntryCount > 0)) {
            throw new IllegalArgumentException("hasMaterialPayload must match the material payload counts");
        }
        if (hasEmissivePayload != (emissivePayloadSectionCount > 0)) {
            throw new IllegalArgumentException("hasEmissivePayload must match emissivePayloadSectionCount");
        }
    }

    public static PrimaryVoxelGBufferPassOutputMetadata from(
            GBufferWriteIntent writeIntent,
            VoxelTraversalRequest traversalRequest,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        Objects.requireNonNull(writeIntent, "writeIntent");
        Objects.requireNonNull(traversalRequest, "traversalRequest");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");

        long occupiedVoxelCount = 0L;
        long opaqueVoxelCount = 0L;
        long translucentVoxelCount = 0L;
        long fluidVoxelCount = 0L;
        long emissiveVoxelCount = 0L;
        int materialPayloadSectionCount = 0;
        long materialPaletteEntryCount = 0L;
        int emissivePayloadSectionCount = 0;

        for (VoxelSectionSnapshotReference sectionSnapshot : sectionSnapshots) {
            Objects.requireNonNull(sectionSnapshot, "sectionSnapshots must not contain null entries");
            occupiedVoxelCount += sectionSnapshot.occupiedVoxelCount();
            opaqueVoxelCount += sectionSnapshot.opaqueVoxelCount();
            translucentVoxelCount += sectionSnapshot.translucentVoxelCount();
            fluidVoxelCount += sectionSnapshot.fluidVoxelCount();
            emissiveVoxelCount += sectionSnapshot.emissiveVoxelCount();
            if (sectionSnapshot.hasMaterialPalette()) {
                materialPayloadSectionCount++;
                materialPaletteEntryCount += sectionSnapshot.materialPaletteSize();
            }
            if (sectionSnapshot.hasEmissivePayload()) {
                emissivePayloadSectionCount++;
            }
        }

        return new PrimaryVoxelGBufferPassOutputMetadata(
                writeIntent.attachments(),
                traversalRequest.requestGeneration(),
                sectionSnapshots.size(),
                occupiedVoxelCount,
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount,
                materialPayloadSectionCount,
                materialPaletteEntryCount,
                emissivePayloadSectionCount,
                materialPayloadSectionCount > 0 || materialPaletteEntryCount > 0,
                emissivePayloadSectionCount > 0,
                writeIntent.requiresHistory()
        );
    }

    public int expectedAttachmentWriteCount() {
        return this.expectedAttachmentWrites.size();
    }

    public boolean hasExpectedAttachmentWrites() {
        return !this.expectedAttachmentWrites.isEmpty();
    }

    public List<String> expectedAttachmentNames() {
        return this.expectedAttachmentWrites.stream()
                .map(GBufferAttachmentWriteIntent::attachmentName)
                .toList();
    }

    public boolean hasOccupiedVoxels() {
        return this.occupiedVoxelCount > 0L;
    }

    public boolean hasPayloadHints() {
        return this.hasMaterialPayload || this.hasEmissivePayload;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
