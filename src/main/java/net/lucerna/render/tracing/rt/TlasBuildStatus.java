package net.lucerna.render.tracing.rt;

public record TlasBuildStatus(
        AccelerationStructureUpdateState state,
        int instanceCount,
        int changedInstanceCount,
        int visibleInstanceCount,
        long transformGeneration,
        long buildGeneration,
        long estimatedInstanceBytes,
        long estimatedScratchBytes,
        long estimatedResultBytes,
        boolean nativeBuildSubmitted,
        boolean hardwareRtExecutionClaimed,
        String message
) {
    public TlasBuildStatus {
        if (state == null) {
            state = AccelerationStructureUpdateState.NOT_REQUESTED;
        }
        instanceCount = Math.max(0, instanceCount);
        changedInstanceCount = clamp(changedInstanceCount, instanceCount);
        visibleInstanceCount = clamp(visibleInstanceCount, instanceCount);
        transformGeneration = Math.max(0L, transformGeneration);
        buildGeneration = Math.max(0L, buildGeneration);
        estimatedInstanceBytes = Math.max(0L, estimatedInstanceBytes);
        estimatedScratchBytes = Math.max(0L, estimatedScratchBytes);
        estimatedResultBytes = Math.max(0L, estimatedResultBytes);
        if (state != AccelerationStructureUpdateState.BUILT_ON_DEVICE) {
            hardwareRtExecutionClaimed = false;
        }
        message = clean(message, state.description());
    }

    public static TlasBuildStatus fallback(String reason) {
        return new TlasBuildStatus(
                AccelerationStructureUpdateState.FALLBACK_UNAVAILABLE,
                0,
                0,
                0,
                0L,
                0L,
                0L,
                0L,
                0L,
                false,
                false,
                reason
        );
    }

    public static TlasBuildStatus metadataReady(
            int instanceCount,
            int changedInstanceCount,
            int visibleInstanceCount,
            long transformGeneration,
            long estimatedInstanceBytes
    ) {
        return new TlasBuildStatus(
                AccelerationStructureUpdateState.METADATA_READY,
                instanceCount,
                changedInstanceCount,
                visibleInstanceCount,
                transformGeneration,
                0L,
                estimatedInstanceBytes,
                0L,
                0L,
                false,
                false,
                "TLAS metadata is ready; no native hardware RT build is proven."
        );
    }

    public boolean readyForNativeBuild() {
        return this.state == AccelerationStructureUpdateState.METADATA_READY
                || this.state == AccelerationStructureUpdateState.BUILD_QUEUED;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
