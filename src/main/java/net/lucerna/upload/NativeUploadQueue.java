package net.lucerna.upload;

import net.lucerna.material.MaterialSnapshot;
import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class NativeUploadQueue {
    private final AtomicLong lastWorldGeneration = new AtomicLong();
    private final AtomicLong lastMaterialGeneration = new AtomicLong();
    private final AtomicLong lastSectionGeneration = new AtomicLong();
    private final AtomicLong lastSectionMaterialGeneration = new AtomicLong();
    private final AtomicLong lastSectionOccupancyGeneration = new AtomicLong();
    private final AtomicLong lastSectionEmissiveGeneration = new AtomicLong();
    private final AtomicLong lastSectionDirtyRegionGeneration = new AtomicLong();
    private final AtomicLong lastGBufferStagingGeneration = new AtomicLong();
    private volatile NativeUploadStagingMetadata lastStagingMetadata = NativeUploadStagingMetadata.empty();

    public NativeUploadBatch acceptWorldDeltas(Collection<DirtyRegion> dirtyRegions) {
        return this.acceptWorldDeltas(DirtyRegionBatch.from(dirtyRegions));
    }

    public synchronized NativeUploadBatch acceptWorldDeltas(DirtyRegionBatch dirtyRegionBatch) {
        return this.acceptWorldAndMaterialDeltas(dirtyRegionBatch, MaterialSnapshot.empty());
    }

    public synchronized NativeUploadBatch acceptWorldDeltas(DirtyRegionSnapshot dirtyRegionSnapshot) {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.acceptWorldDeltas(dirtyRegionSnapshot.batch());
    }

    public NativeUploadBatch acceptWorldAndMaterialDeltas(
            Collection<DirtyRegion> dirtyRegions,
            MaterialSnapshot materialSnapshot
    ) {
        return this.acceptWorldAndMaterialDeltas(DirtyRegionBatch.from(dirtyRegions), materialSnapshot);
    }

    public synchronized NativeUploadBatch acceptWorldAndMaterialDeltas(
            DirtyRegionBatch dirtyRegionBatch,
            MaterialSnapshot materialSnapshot
    ) {
        Objects.requireNonNull(dirtyRegionBatch, "dirtyRegionBatch");
        Objects.requireNonNull(materialSnapshot, "materialSnapshot");

        DirtyRegionBatch coalescedDirtyRegionBatch = dirtyRegionBatch.coalesced();
        long previousWorldGeneration = this.lastWorldGeneration.get();
        long previousMaterialGeneration = this.lastMaterialGeneration.get();

        List<NativeDirtyRegionUpload> dirtyRegionUploads = coalescedDirtyRegionBatch.regions().stream()
                .filter(region -> region.generation() > previousWorldGeneration)
                .map(NativeDirtyRegionUpload::from)
                .toList();
        List<NativeMaterialUpload> materialUploads = materialSnapshot.materials().stream()
                .filter(material -> material.generation() > previousMaterialGeneration)
                .map(NativeMaterialUpload::from)
                .toList();

        long nextWorldGeneration = dirtyRegionUploads.stream()
                .mapToLong(NativeDirtyRegionUpload::generation)
                .max()
                .orElse(previousWorldGeneration);
        long maxPayloadMaterialGeneration = materialUploads.stream()
                .mapToLong(NativeMaterialUpload::generation)
                .max()
                .orElse(previousMaterialGeneration);

        nextWorldGeneration = Math.max(previousWorldGeneration, nextWorldGeneration);
        long nextMaterialGeneration = Math.max(
                previousMaterialGeneration,
                Math.max(materialSnapshot.generation(), maxPayloadMaterialGeneration)
        );

        this.lastWorldGeneration.set(nextWorldGeneration);
        this.lastMaterialGeneration.set(nextMaterialGeneration);

        long firstWorldGeneration = dirtyRegionUploads.stream()
                .mapToLong(NativeDirtyRegionUpload::generation)
                .min()
                .orElse(0L);
        long lastWorldGeneration = dirtyRegionUploads.stream()
                .mapToLong(NativeDirtyRegionUpload::generation)
                .max()
                .orElse(0L);
        long batchGeneration = Math.max(nextWorldGeneration, nextMaterialGeneration);
        NativeUploadBatch batch = new NativeUploadBatch(
                batchGeneration,
                dirtyRegionUploads.size(),
                materialUploads.size(),
                firstWorldGeneration,
                lastWorldGeneration,
                nextMaterialGeneration,
                dirtyRegionUploads,
                materialUploads
        );
        this.lastStagingMetadata = NativeUploadStagingMetadata.from(batch, List.of(), List.of());
        return batch;
    }

    public synchronized NativeUploadBatch acceptWorldAndMaterialDeltas(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            MaterialSnapshot materialSnapshot
    ) {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.acceptWorldAndMaterialDeltas(dirtyRegionSnapshot.batch(), materialSnapshot);
    }

    public synchronized NativeStagedUploadBatch acceptStagingDeltas(
            Collection<NativeSectionSnapshotUpload> sectionSnapshots,
            Collection<NativeGBufferStagingUpload> gBufferStaging
    ) {
        return this.acceptStagingDeltas(new NativeUploadBatch(0, 0, 0), sectionSnapshots, gBufferStaging);
    }

    public synchronized NativeStagedUploadBatch acceptStagingDeltas(
            NativeUploadBatch worldAndMaterialBatch,
            Collection<NativeSectionSnapshotUpload> sectionSnapshots,
            Collection<NativeGBufferStagingUpload> gBufferStaging
    ) {
        Objects.requireNonNull(worldAndMaterialBatch, "worldAndMaterialBatch");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        Objects.requireNonNull(gBufferStaging, "gBufferStaging");

        long previousSectionGeneration = this.lastSectionGeneration.get();
        long previousSectionMaterialGeneration = this.lastSectionMaterialGeneration.get();
        long previousSectionOccupancyGeneration = this.lastSectionOccupancyGeneration.get();
        long previousSectionEmissiveGeneration = this.lastSectionEmissiveGeneration.get();
        long previousSectionDirtyRegionGeneration = this.lastSectionDirtyRegionGeneration.get();
        long previousGBufferStagingGeneration = this.lastGBufferStagingGeneration.get();

        List<NativeSectionSnapshotUpload> sectionSnapshotUploads = List.copyOf(sectionSnapshots).stream()
                .filter(upload -> hasNewSectionPayload(
                        upload,
                        previousSectionGeneration,
                        previousSectionMaterialGeneration,
                        previousSectionOccupancyGeneration,
                        previousSectionEmissiveGeneration,
                        previousSectionDirtyRegionGeneration
                ))
                .toList();
        List<NativeGBufferStagingUpload> gBufferUploads = List.copyOf(gBufferStaging).stream()
                .filter(upload -> upload.generation() > previousGBufferStagingGeneration)
                .toList();

        this.lastWorldGeneration.set(max(this.lastWorldGeneration.get(), worldAndMaterialBatch.lastWorldGeneration()));
        this.lastMaterialGeneration.set(max(this.lastMaterialGeneration.get(), worldAndMaterialBatch.materialGeneration()));
        this.lastSectionGeneration.set(max(
                previousSectionGeneration,
                sectionSnapshotUploads.stream()
                        .mapToLong(NativeSectionSnapshotUpload::sectionGeneration)
                        .max()
                        .orElse(previousSectionGeneration)
        ));
        this.lastSectionMaterialGeneration.set(max(
                previousSectionMaterialGeneration,
                sectionSnapshotUploads.stream()
                        .mapToLong(NativeSectionSnapshotUpload::materialGeneration)
                        .max()
                        .orElse(previousSectionMaterialGeneration)
        ));
        this.lastSectionOccupancyGeneration.set(max(
                previousSectionOccupancyGeneration,
                sectionSnapshotUploads.stream()
                        .mapToLong(NativeSectionSnapshotUpload::occupancyGeneration)
                        .max()
                        .orElse(previousSectionOccupancyGeneration)
        ));
        this.lastSectionEmissiveGeneration.set(max(
                previousSectionEmissiveGeneration,
                sectionSnapshotUploads.stream()
                        .mapToLong(NativeSectionSnapshotUpload::emissiveGeneration)
                        .max()
                        .orElse(previousSectionEmissiveGeneration)
        ));
        this.lastSectionDirtyRegionGeneration.set(max(
                previousSectionDirtyRegionGeneration,
                sectionSnapshotUploads.stream()
                        .mapToLong(NativeSectionSnapshotUpload::dirtyRegionGeneration)
                        .max()
                        .orElse(previousSectionDirtyRegionGeneration)
        ));
        this.lastGBufferStagingGeneration.set(max(
                previousGBufferStagingGeneration,
                gBufferUploads.stream()
                        .mapToLong(NativeGBufferStagingUpload::generation)
                        .max()
                        .orElse(previousGBufferStagingGeneration)
        ));

        NativeStagedUploadBatch batch = new NativeStagedUploadBatch(
                worldAndMaterialBatch,
                sectionSnapshotUploads,
                gBufferUploads
        );
        this.lastStagingMetadata = batch.metadata();
        return batch;
    }

    public synchronized NativeStagedUploadBatch acceptWorldMaterialAndStagingDeltas(
            Collection<DirtyRegion> dirtyRegions,
            MaterialSnapshot materialSnapshot,
            Collection<NativeSectionSnapshotUpload> sectionSnapshots,
            Collection<NativeGBufferStagingUpload> gBufferStaging
    ) {
        return this.acceptWorldMaterialAndStagingDeltas(
                DirtyRegionBatch.from(dirtyRegions),
                materialSnapshot,
                sectionSnapshots,
                gBufferStaging
        );
    }

    public synchronized NativeStagedUploadBatch acceptWorldMaterialAndStagingDeltas(
            DirtyRegionBatch dirtyRegionBatch,
            MaterialSnapshot materialSnapshot,
            Collection<NativeSectionSnapshotUpload> sectionSnapshots,
            Collection<NativeGBufferStagingUpload> gBufferStaging
    ) {
        NativeUploadBatch worldAndMaterialBatch = this.acceptWorldAndMaterialDeltas(dirtyRegionBatch, materialSnapshot);
        return this.acceptStagingDeltas(worldAndMaterialBatch, sectionSnapshots, gBufferStaging);
    }

    public synchronized NativeStagedUploadBatch acceptWorldMaterialAndStagingDeltas(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            MaterialSnapshot materialSnapshot,
            Collection<NativeSectionSnapshotUpload> sectionSnapshots,
            Collection<NativeGBufferStagingUpload> gBufferStaging
    ) {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.acceptWorldMaterialAndStagingDeltas(
                dirtyRegionSnapshot.batch(),
                materialSnapshot,
                sectionSnapshots,
                gBufferStaging
        );
    }

    public long lastGeneration() {
        return max(
                this.lastWorldGeneration.get(),
                this.lastMaterialGeneration.get(),
                this.lastSectionGeneration.get(),
                this.lastSectionMaterialGeneration.get(),
                this.lastSectionOccupancyGeneration.get(),
                this.lastSectionEmissiveGeneration.get(),
                this.lastSectionDirtyRegionGeneration.get(),
                this.lastGBufferStagingGeneration.get()
        );
    }

    public long lastWorldGeneration() {
        return this.lastWorldGeneration.get();
    }

    public long lastMaterialGeneration() {
        return this.lastMaterialGeneration.get();
    }

    public long lastSectionGeneration() {
        return this.lastSectionGeneration.get();
    }

    public long lastSectionMaterialGeneration() {
        return this.lastSectionMaterialGeneration.get();
    }

    public long lastSectionOccupancyGeneration() {
        return this.lastSectionOccupancyGeneration.get();
    }

    public long lastSectionEmissiveGeneration() {
        return this.lastSectionEmissiveGeneration.get();
    }

    public long lastSectionDirtyRegionGeneration() {
        return this.lastSectionDirtyRegionGeneration.get();
    }

    public long lastGBufferStagingGeneration() {
        return this.lastGBufferStagingGeneration.get();
    }

    public NativeUploadStagingMetadata lastStagingMetadata() {
        return this.lastStagingMetadata;
    }

    private static boolean hasNewSectionPayload(
            NativeSectionSnapshotUpload upload,
            long previousSectionGeneration,
            long previousSectionMaterialGeneration,
            long previousSectionOccupancyGeneration,
            long previousSectionEmissiveGeneration,
            long previousSectionDirtyRegionGeneration
    ) {
        return upload.sectionGeneration() > previousSectionGeneration
                || upload.materialGeneration() > previousSectionMaterialGeneration
                || upload.occupancyGeneration() > previousSectionOccupancyGeneration
                || upload.emissiveGeneration() > previousSectionEmissiveGeneration
                || upload.dirtyRegionGeneration() > previousSectionDirtyRegionGeneration;
    }

    private static long max(long first, long second) {
        return Math.max(first, second);
    }

    private static long max(
            long first,
            long second,
            long third,
            long fourth,
            long fifth,
            long sixth,
            long seventh,
            long eighth
    ) {
        return Math.max(
                Math.max(Math.max(first, second), Math.max(third, fourth)),
                Math.max(Math.max(fifth, sixth), Math.max(seventh, eighth))
        );
    }
}
