package net.lucerna.render.tracing.rt;

public record BlasBuildStatus(
        AccelerationStructureUpdateState state,
        int geometryCount,
        int primitiveCount,
        long sourceGeneration,
        long buildGeneration,
        long estimatedInputBytes,
        long estimatedScratchBytes,
        long estimatedResultBytes,
        boolean nativeBuildSubmitted,
        boolean hardwareRtExecutionClaimed,
        String message
) {
    public BlasBuildStatus {
        if (state == null) {
            state = AccelerationStructureUpdateState.NOT_REQUESTED;
        }
        geometryCount = Math.max(0, geometryCount);
        primitiveCount = Math.max(0, primitiveCount);
        sourceGeneration = Math.max(0L, sourceGeneration);
        buildGeneration = Math.max(0L, buildGeneration);
        estimatedInputBytes = Math.max(0L, estimatedInputBytes);
        estimatedScratchBytes = Math.max(0L, estimatedScratchBytes);
        estimatedResultBytes = Math.max(0L, estimatedResultBytes);
        if (state != AccelerationStructureUpdateState.BUILT_ON_DEVICE) {
            hardwareRtExecutionClaimed = false;
        }
        message = clean(message, state.description());
    }

    public static BlasBuildStatus fallback(String reason) {
        return new BlasBuildStatus(
                AccelerationStructureUpdateState.FALLBACK_UNAVAILABLE,
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

    public static BlasBuildStatus metadataReady(
            int geometryCount,
            int primitiveCount,
            long sourceGeneration,
            long estimatedInputBytes
    ) {
        return new BlasBuildStatus(
                AccelerationStructureUpdateState.METADATA_READY,
                geometryCount,
                primitiveCount,
                sourceGeneration,
                0L,
                estimatedInputBytes,
                0L,
                0L,
                false,
                false,
                "BLAS metadata is ready; no native hardware RT build is proven."
        );
    }

    public boolean readyForNativeBuild() {
        return this.state == AccelerationStructureUpdateState.METADATA_READY
                || this.state == AccelerationStructureUpdateState.BUILD_QUEUED;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
