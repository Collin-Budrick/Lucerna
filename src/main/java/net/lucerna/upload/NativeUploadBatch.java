package net.lucerna.upload;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record NativeUploadBatch(
        long generation,
        int dirtyRegionCount,
        int materialUpdateCount,
        long firstWorldGeneration,
        long lastWorldGeneration,
        long materialGeneration,
        List<NativeDirtyRegionUpload> dirtyRegions,
        List<NativeMaterialUpload> materialUpdates
) {
    public NativeUploadBatch {
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        Objects.requireNonNull(materialUpdates, "materialUpdates");
        dirtyRegions = List.copyOf(dirtyRegions);
        materialUpdates = List.copyOf(materialUpdates);

        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        if (dirtyRegionCount < 0) {
            throw new IllegalArgumentException("dirtyRegionCount must be non-negative");
        }
        if (materialUpdateCount < 0) {
            throw new IllegalArgumentException("materialUpdateCount must be non-negative");
        }
        if (firstWorldGeneration < 0 || lastWorldGeneration < 0 || materialGeneration < 0) {
            throw new IllegalArgumentException("generation bounds must be non-negative");
        }
        if (firstWorldGeneration > lastWorldGeneration) {
            throw new IllegalArgumentException("firstWorldGeneration must be less than or equal to lastWorldGeneration");
        }
        if (dirtyRegionCount < dirtyRegions.size()) {
            throw new IllegalArgumentException("dirtyRegionCount cannot be smaller than dirty region payload size");
        }
        if (materialUpdateCount < materialUpdates.size()) {
            throw new IllegalArgumentException("materialUpdateCount cannot be smaller than material payload size");
        }
        if (!dirtyRegions.isEmpty()) {
            long actualFirstWorldGeneration = firstWorldGeneration(dirtyRegions);
            long actualLastWorldGeneration = lastWorldGeneration(dirtyRegions);
            if (firstWorldGeneration != actualFirstWorldGeneration || lastWorldGeneration != actualLastWorldGeneration) {
                throw new IllegalArgumentException("world generation bounds must match dirty region payloads");
            }
        }
    }

    public NativeUploadBatch(long generation, int dirtyRegionCount, int materialUpdateCount) {
        this(
                generation,
                dirtyRegionCount,
                materialUpdateCount,
                dirtyRegionCount > 0 ? generation : 0,
                dirtyRegionCount > 0 ? generation : 0,
                0,
                List.of(),
                List.of()
        );
    }

    public NativeUploadBatch(
            long materialGeneration,
            List<NativeDirtyRegionUpload> dirtyRegions,
            List<NativeMaterialUpload> materialUpdates
    ) {
        this(
                Math.max(lastWorldGeneration(dirtyRegions), materialGeneration),
                dirtyRegions.size(),
                materialUpdates.size(),
                firstWorldGeneration(dirtyRegions),
                lastWorldGeneration(dirtyRegions),
                materialGeneration,
                dirtyRegions,
                materialUpdates
        );
    }

    public boolean isEmpty() {
        return this.dirtyRegionCount == 0 && this.materialUpdateCount == 0;
    }

    public boolean hasPayloads() {
        return !this.dirtyRegions.isEmpty() || !this.materialUpdates.isEmpty();
    }

    private static long firstWorldGeneration(List<NativeDirtyRegionUpload> dirtyRegions) {
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        return dirtyRegions.stream()
                .min(Comparator.comparingLong(NativeDirtyRegionUpload::generation))
                .map(NativeDirtyRegionUpload::generation)
                .orElse(0L);
    }

    private static long lastWorldGeneration(List<NativeDirtyRegionUpload> dirtyRegions) {
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
        return dirtyRegions.stream()
                .max(Comparator.comparingLong(NativeDirtyRegionUpload::generation))
                .map(NativeDirtyRegionUpload::generation)
                .orElse(0L);
    }
}
