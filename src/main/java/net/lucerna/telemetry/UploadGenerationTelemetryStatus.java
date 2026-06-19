package net.lucerna.telemetry;

import net.lucerna.upload.NativeUploadStagingMetadata;
import net.lucerna.upload.NativeUploadQueue;

public record UploadGenerationTelemetryStatus(
        long lastGeneration,
        long lastWorldGeneration,
        long lastMaterialGeneration,
        long lastSectionGeneration,
        long lastSectionMaterialGeneration,
        long lastSectionOccupancyGeneration,
        long lastSectionEmissiveGeneration,
        long lastSectionDirtyRegionGeneration,
        long lastGBufferStagingGeneration,
        long stagingGeneration,
        long stagedFirstWorldGeneration,
        long stagedLastWorldGeneration,
        long stagedMaterialGeneration,
        long stagedFirstSectionSnapshotGeneration,
        long stagedLastSectionSnapshotGeneration,
        int stagedDirtyRegionCount,
        int stagedMaterialUpdateCount,
        int stagedSectionSnapshotCount,
        int stagedGBufferStagingCount
) {
    public UploadGenerationTelemetryStatus {
        lastWorldGeneration = nonNegative(lastWorldGeneration);
        lastMaterialGeneration = nonNegative(lastMaterialGeneration);
        lastSectionGeneration = nonNegative(lastSectionGeneration);
        lastSectionMaterialGeneration = nonNegative(lastSectionMaterialGeneration);
        lastSectionOccupancyGeneration = nonNegative(lastSectionOccupancyGeneration);
        lastSectionEmissiveGeneration = nonNegative(lastSectionEmissiveGeneration);
        lastSectionDirtyRegionGeneration = nonNegative(lastSectionDirtyRegionGeneration);
        lastGBufferStagingGeneration = nonNegative(lastGBufferStagingGeneration);
        stagingGeneration = nonNegative(stagingGeneration);
        stagedDirtyRegionCount = nonNegative(stagedDirtyRegionCount);
        stagedMaterialUpdateCount = nonNegative(stagedMaterialUpdateCount);
        stagedSectionSnapshotCount = nonNegative(stagedSectionSnapshotCount);
        stagedGBufferStagingCount = nonNegative(stagedGBufferStagingCount);

        stagedFirstWorldGeneration = nonNegative(stagedFirstWorldGeneration);
        stagedLastWorldGeneration = nonNegative(stagedLastWorldGeneration);
        if (stagedDirtyRegionCount == 0) {
            stagedFirstWorldGeneration = 0L;
            stagedLastWorldGeneration = 0L;
        } else {
            stagedLastWorldGeneration = Math.max(stagedFirstWorldGeneration, stagedLastWorldGeneration);
        }

        stagedMaterialGeneration = nonNegative(stagedMaterialGeneration);
        stagedFirstSectionSnapshotGeneration = nonNegative(stagedFirstSectionSnapshotGeneration);
        stagedLastSectionSnapshotGeneration = nonNegative(stagedLastSectionSnapshotGeneration);
        if (stagedSectionSnapshotCount == 0) {
            stagedFirstSectionSnapshotGeneration = 0L;
            stagedLastSectionSnapshotGeneration = 0L;
        } else {
            stagedLastSectionSnapshotGeneration = Math.max(
                    stagedFirstSectionSnapshotGeneration,
                    stagedLastSectionSnapshotGeneration
            );
        }

        lastGeneration = maxNonNegative(
                lastGeneration,
                lastWorldGeneration,
                lastMaterialGeneration,
                lastSectionGeneration,
                lastSectionMaterialGeneration,
                lastSectionOccupancyGeneration,
                lastSectionEmissiveGeneration,
                lastSectionDirtyRegionGeneration,
                lastGBufferStagingGeneration,
                stagingGeneration,
                stagedLastWorldGeneration,
                stagedMaterialGeneration,
                stagedLastSectionSnapshotGeneration
        );
    }

    public UploadGenerationTelemetryStatus(
            long lastGeneration,
            long lastWorldGeneration,
            long lastMaterialGeneration
    ) {
        this(
                lastGeneration,
                lastWorldGeneration,
                lastMaterialGeneration,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0,
                0,
                0,
                0
        );
    }

    public static UploadGenerationTelemetryStatus empty() {
        return new UploadGenerationTelemetryStatus(0L, 0L, 0L);
    }

    public static UploadGenerationTelemetryStatus from(NativeUploadQueue queue) {
        if (queue == null) {
            return empty();
        }
        NativeUploadStagingMetadata stagingMetadata = queue.lastStagingMetadata();
        if (stagingMetadata == null) {
            stagingMetadata = NativeUploadStagingMetadata.empty();
        }
        return new UploadGenerationTelemetryStatus(
                queue.lastGeneration(),
                queue.lastWorldGeneration(),
                queue.lastMaterialGeneration(),
                queue.lastSectionGeneration(),
                queue.lastSectionMaterialGeneration(),
                queue.lastSectionOccupancyGeneration(),
                queue.lastSectionEmissiveGeneration(),
                queue.lastSectionDirtyRegionGeneration(),
                queue.lastGBufferStagingGeneration(),
                stagingMetadata.generation(),
                stagingMetadata.firstWorldGeneration(),
                stagingMetadata.lastWorldGeneration(),
                stagingMetadata.materialGeneration(),
                stagingMetadata.firstSectionSnapshotGeneration(),
                stagingMetadata.lastSectionSnapshotGeneration(),
                stagingMetadata.dirtyRegionCount(),
                stagingMetadata.materialUpdateCount(),
                stagingMetadata.sectionSnapshotCount(),
                stagingMetadata.gBufferStagingCount()
        );
    }

    public boolean hasSectionSnapshotStaging() {
        return this.stagedSectionSnapshotCount > 0;
    }

    public boolean hasGBufferStaging() {
        return this.stagedGBufferStagingCount > 0;
    }

    public boolean hasGBufferStagingGeneration() {
        return this.lastGBufferStagingGeneration > 0L;
    }

    public String compactGenerationLabel() {
        return "combined=%d world=%d material=%d section=%d gbuffer=%d staging=%d".formatted(
                this.lastGeneration,
                this.lastWorldGeneration,
                this.lastMaterialGeneration,
                this.lastSectionGeneration,
                this.lastGBufferStagingGeneration,
                this.stagingGeneration
        );
    }

    public String compactSectionGenerationLabel() {
        return "section=%d material=%d occupancy=%d emissive=%d dirty=%d".formatted(
                this.lastSectionGeneration,
                this.lastSectionMaterialGeneration,
                this.lastSectionOccupancyGeneration,
                this.lastSectionEmissiveGeneration,
                this.lastSectionDirtyRegionGeneration
        );
    }

    public String compactSectionSnapshotLabel() {
        return "count=%d range=%s".formatted(
                this.stagedSectionSnapshotCount,
                rangeLabel(this.stagedFirstSectionSnapshotGeneration, this.stagedLastSectionSnapshotGeneration)
        );
    }

    public String compactGBufferStagingLabel() {
        return "count=%d gen=%d".formatted(
                this.stagedGBufferStagingCount,
                this.lastGBufferStagingGeneration
        );
    }

    public String explicitGBufferStagingLabel() {
        return "stagedCount=%d lastGBufferGen=%d stagingGen=%d combinedGen=%d payload=%s".formatted(
                this.stagedGBufferStagingCount,
                this.lastGBufferStagingGeneration,
                this.stagingGeneration,
                this.lastGeneration,
                Boolean.toString(this.hasGBufferStaging())
        );
    }

    public String compactStagingPayloadLabel() {
        return "dirty=%d material=%d sections=%d gbuffer=%d worldRange=%s materialGen=%d".formatted(
                this.stagedDirtyRegionCount,
                this.stagedMaterialUpdateCount,
                this.stagedSectionSnapshotCount,
                this.stagedGBufferStagingCount,
                rangeLabel(this.stagedFirstWorldGeneration, this.stagedLastWorldGeneration),
                this.stagedMaterialGeneration
        );
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }

    private static long maxNonNegative(long first, long... rest) {
        long max = nonNegative(first);
        for (long value : rest) {
            max = Math.max(max, nonNegative(value));
        }
        return max;
    }

    private static String rangeLabel(long firstGeneration, long lastGeneration) {
        if (firstGeneration == 0L && lastGeneration == 0L) {
            return "none";
        }
        if (firstGeneration == lastGeneration) {
            return Long.toString(firstGeneration);
        }
        return firstGeneration + "-" + lastGeneration;
    }
}
