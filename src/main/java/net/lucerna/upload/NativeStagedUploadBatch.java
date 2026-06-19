package net.lucerna.upload;

import java.util.List;
import java.util.Objects;

public record NativeStagedUploadBatch(
        NativeUploadBatch worldAndMaterialBatch,
        NativeUploadStagingMetadata metadata,
        List<NativeSectionSnapshotUpload> sectionSnapshots,
        List<NativeGBufferStagingUpload> gBufferStaging
) {
    public NativeStagedUploadBatch {
        Objects.requireNonNull(worldAndMaterialBatch, "worldAndMaterialBatch");
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        Objects.requireNonNull(gBufferStaging, "gBufferStaging");
        sectionSnapshots = List.copyOf(sectionSnapshots);
        gBufferStaging = List.copyOf(gBufferStaging);

        if (metadata.dirtyRegionCount() != worldAndMaterialBatch.dirtyRegionCount()) {
            throw new IllegalArgumentException("metadata dirtyRegionCount must match worldAndMaterialBatch");
        }
        if (metadata.materialUpdateCount() != worldAndMaterialBatch.materialUpdateCount()) {
            throw new IllegalArgumentException("metadata materialUpdateCount must match worldAndMaterialBatch");
        }
        if (metadata.sectionSnapshotCount() != sectionSnapshots.size()) {
            throw new IllegalArgumentException("metadata sectionSnapshotCount must match sectionSnapshots");
        }
        if (metadata.gBufferStagingCount() != gBufferStaging.size()) {
            throw new IllegalArgumentException("metadata gBufferStagingCount must match gBufferStaging");
        }
    }

    public NativeStagedUploadBatch(
            NativeUploadBatch worldAndMaterialBatch,
            List<NativeSectionSnapshotUpload> sectionSnapshots,
            List<NativeGBufferStagingUpload> gBufferStaging
    ) {
        this(
                worldAndMaterialBatch,
                NativeUploadStagingMetadata.from(worldAndMaterialBatch, sectionSnapshots, gBufferStaging),
                sectionSnapshots,
                gBufferStaging
        );
    }

    public boolean isEmpty() {
        return this.metadata.isEmpty();
    }

    public boolean hasStagingPayloads() {
        return !this.sectionSnapshots.isEmpty() || !this.gBufferStaging.isEmpty();
    }

    public NativeUploadPacket toWorldMaterialPacket() {
        return this.worldAndMaterialBatch.toPacket();
    }

    public NativeSectionSnapshotUploadPacket toSectionSnapshotPacket() {
        return NativeSectionSnapshotUploadPacket.from(this);
    }
}
