package net.lucerna.upload;

import java.util.List;
import java.util.Objects;

public record NativeUploadStagingMetadata(
        long generation,
        long firstWorldGeneration,
        long lastWorldGeneration,
        long materialGeneration,
        long firstSectionSnapshotGeneration,
        long lastSectionSnapshotGeneration,
        long sectionGeneration,
        long sectionMaterialGeneration,
        long sectionOccupancyGeneration,
        long sectionEmissiveGeneration,
        long sectionDirtyRegionGeneration,
        long gBufferStagingGeneration,
        int dirtyRegionCount,
        int materialUpdateCount,
        int sectionSnapshotCount,
        int gBufferStagingCount
) {
    public NativeUploadStagingMetadata {
        requireNonNegative(generation, "generation");
        requireNonNegative(firstWorldGeneration, "firstWorldGeneration");
        requireNonNegative(lastWorldGeneration, "lastWorldGeneration");
        requireNonNegative(materialGeneration, "materialGeneration");
        requireNonNegative(firstSectionSnapshotGeneration, "firstSectionSnapshotGeneration");
        requireNonNegative(lastSectionSnapshotGeneration, "lastSectionSnapshotGeneration");
        requireNonNegative(sectionGeneration, "sectionGeneration");
        requireNonNegative(sectionMaterialGeneration, "sectionMaterialGeneration");
        requireNonNegative(sectionOccupancyGeneration, "sectionOccupancyGeneration");
        requireNonNegative(sectionEmissiveGeneration, "sectionEmissiveGeneration");
        requireNonNegative(sectionDirtyRegionGeneration, "sectionDirtyRegionGeneration");
        requireNonNegative(gBufferStagingGeneration, "gBufferStagingGeneration");
        requireNonNegative(dirtyRegionCount, "dirtyRegionCount");
        requireNonNegative(materialUpdateCount, "materialUpdateCount");
        requireNonNegative(sectionSnapshotCount, "sectionSnapshotCount");
        requireNonNegative(gBufferStagingCount, "gBufferStagingCount");
        if (firstWorldGeneration > lastWorldGeneration) {
            throw new IllegalArgumentException("firstWorldGeneration must be less than or equal to lastWorldGeneration");
        }
        if (firstSectionSnapshotGeneration > lastSectionSnapshotGeneration) {
            throw new IllegalArgumentException(
                    "firstSectionSnapshotGeneration must be less than or equal to lastSectionSnapshotGeneration"
            );
        }
        if (sectionSnapshotCount == 0 && (firstSectionSnapshotGeneration != 0 || lastSectionSnapshotGeneration != 0)) {
            throw new IllegalArgumentException("empty section staging metadata must use zero section generation bounds");
        }
        if (dirtyRegionCount == 0 && (firstWorldGeneration != 0 || lastWorldGeneration != 0)) {
            throw new IllegalArgumentException("empty dirty-region metadata must use zero world generation bounds");
        }
    }

    public static NativeUploadStagingMetadata empty() {
        return new NativeUploadStagingMetadata(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static NativeUploadStagingMetadata from(
            NativeUploadBatch worldAndMaterialBatch,
            List<NativeSectionSnapshotUpload> sectionSnapshots,
            List<NativeGBufferStagingUpload> gBufferStaging
    ) {
        Objects.requireNonNull(worldAndMaterialBatch, "worldAndMaterialBatch");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        Objects.requireNonNull(gBufferStaging, "gBufferStaging");

        long firstSectionSnapshotGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::combinedGeneration)
                .min()
                .orElse(0L);
        long lastSectionSnapshotGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::combinedGeneration)
                .max()
                .orElse(0L);
        long sectionGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::sectionGeneration)
                .max()
                .orElse(0L);
        long sectionMaterialGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::materialGeneration)
                .max()
                .orElse(0L);
        long sectionOccupancyGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::occupancyGeneration)
                .max()
                .orElse(0L);
        long sectionEmissiveGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::emissiveGeneration)
                .max()
                .orElse(0L);
        long sectionDirtyRegionGeneration = sectionSnapshots.stream()
                .mapToLong(NativeSectionSnapshotUpload::dirtyRegionGeneration)
                .max()
                .orElse(0L);
        long gBufferStagingGeneration = gBufferStaging.stream()
                .mapToLong(NativeGBufferStagingUpload::generation)
                .max()
                .orElse(0L);
        long generation = max(
                worldAndMaterialBatch.generation(),
                sectionGeneration,
                sectionMaterialGeneration,
                sectionOccupancyGeneration,
                sectionEmissiveGeneration,
                sectionDirtyRegionGeneration,
                gBufferStagingGeneration
        );

        return new NativeUploadStagingMetadata(
                generation,
                worldAndMaterialBatch.firstWorldGeneration(),
                worldAndMaterialBatch.lastWorldGeneration(),
                worldAndMaterialBatch.materialGeneration(),
                firstSectionSnapshotGeneration,
                lastSectionSnapshotGeneration,
                sectionGeneration,
                sectionMaterialGeneration,
                sectionOccupancyGeneration,
                sectionEmissiveGeneration,
                sectionDirtyRegionGeneration,
                gBufferStagingGeneration,
                worldAndMaterialBatch.dirtyRegionCount(),
                worldAndMaterialBatch.materialUpdateCount(),
                sectionSnapshots.size(),
                gBufferStaging.size()
        );
    }

    public boolean isEmpty() {
        return this.dirtyRegionCount == 0
                && this.materialUpdateCount == 0
                && this.sectionSnapshotCount == 0
                && this.gBufferStagingCount == 0;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static long max(long first, long second, long third, long fourth, long fifth, long sixth, long seventh) {
        return Math.max(Math.max(Math.max(first, second), Math.max(third, fourth)), Math.max(Math.max(fifth, sixth), seventh));
    }
}
