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
        return new NativeUploadBatch(
                batchGeneration,
                dirtyRegionUploads.size(),
                materialUploads.size(),
                firstWorldGeneration,
                lastWorldGeneration,
                nextMaterialGeneration,
                dirtyRegionUploads,
                materialUploads
        );
    }

    public synchronized NativeUploadBatch acceptWorldAndMaterialDeltas(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            MaterialSnapshot materialSnapshot
    ) {
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.acceptWorldAndMaterialDeltas(dirtyRegionSnapshot.batch(), materialSnapshot);
    }

    public long lastGeneration() {
        return Math.max(this.lastWorldGeneration.get(), this.lastMaterialGeneration.get());
    }

    public long lastWorldGeneration() {
        return this.lastWorldGeneration.get();
    }

    public long lastMaterialGeneration() {
        return this.lastMaterialGeneration.get();
    }
}
