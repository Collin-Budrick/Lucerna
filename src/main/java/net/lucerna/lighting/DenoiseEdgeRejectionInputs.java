package net.lucerna.lighting;

public record DenoiseEdgeRejectionInputs(
        boolean depthAvailable,
        boolean normalAvailable,
        boolean materialAvailable,
        boolean motionHistoryAvailable,
        boolean previousDepthAvailable,
        boolean previousNormalAvailable,
        boolean previousMaterialAvailable,
        boolean previousLightingAvailable,
        long gBufferGeneration,
        long historyGeneration,
        String readinessReason
) {
    public DenoiseEdgeRejectionInputs {
        gBufferGeneration = Math.max(0L, gBufferGeneration);
        historyGeneration = Math.max(0L, historyGeneration);
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? defaultReason(depthAvailable, normalAvailable, materialAvailable, motionHistoryAvailable)
                : readinessReason;
    }

    public static DenoiseEdgeRejectionInputs unavailable(String reason) {
        return new DenoiseEdgeRejectionInputs(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0L,
                0L,
                reason
        );
    }

    public boolean currentEdgeInputsAvailable() {
        return this.depthAvailable && this.normalAvailable && this.materialAvailable;
    }

    public boolean temporalHistoryInputsAvailable() {
        return this.motionHistoryAvailable
                && this.previousDepthAvailable
                && this.previousNormalAvailable
                && this.previousLightingAvailable;
    }

    public boolean readyForEdgeAwareDenoise() {
        return this.currentEdgeInputsAvailable();
    }

    public boolean readyForHistoryRejection() {
        return this.currentEdgeInputsAvailable() && this.temporalHistoryInputsAvailable();
    }

    private static String defaultReason(
            boolean depthAvailable,
            boolean normalAvailable,
            boolean materialAvailable,
            boolean motionHistoryAvailable
    ) {
        if (!depthAvailable) {
            return "current depth edge input unavailable";
        }
        if (!normalAvailable) {
            return "current normal edge input unavailable";
        }
        if (!materialAvailable) {
            return "current material edge input unavailable";
        }
        if (!motionHistoryAvailable) {
            return "edge inputs available without motion/history rejection";
        }
        return "edge and motion inputs available for denoise contract";
    }
}
