package net.lucerna.telemetry;

public record DirtyRegionTelemetryStatus(
        long worldGeneration,
        int pendingDirtyRegionCount
) {
    public DirtyRegionTelemetryStatus {
        worldGeneration = Math.max(0L, worldGeneration);
        pendingDirtyRegionCount = Math.max(0, pendingDirtyRegionCount);
    }

    public long pendingUploadLag(UploadGenerationTelemetryStatus uploads) {
        if (uploads == null) {
            return this.worldGeneration;
        }
        return Math.max(0L, this.worldGeneration - uploads.lastWorldGeneration());
    }
}
