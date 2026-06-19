package net.lucerna.telemetry;

import net.lucerna.upload.NativeUploadQueue;

public record UploadGenerationTelemetryStatus(
        long lastGeneration,
        long lastWorldGeneration,
        long lastMaterialGeneration
) {
    public UploadGenerationTelemetryStatus {
        lastGeneration = Math.max(0L, lastGeneration);
        lastWorldGeneration = Math.max(0L, lastWorldGeneration);
        lastMaterialGeneration = Math.max(0L, lastMaterialGeneration);
    }

    public static UploadGenerationTelemetryStatus from(NativeUploadQueue queue) {
        if (queue == null) {
            return new UploadGenerationTelemetryStatus(0L, 0L, 0L);
        }
        return new UploadGenerationTelemetryStatus(
                queue.lastGeneration(),
                queue.lastWorldGeneration(),
                queue.lastMaterialGeneration()
        );
    }
}
