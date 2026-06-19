package net.lucerna.lighting;

public record DenoiseOutputContract(
        boolean denoisedDiffuseOutput,
        boolean directShadowOutput,
        boolean rejectionMaskOutput,
        boolean specularOutputPlaceholder,
        boolean ambientOcclusionOutputPlaceholder,
        long outputGeneration,
        int width,
        int height,
        String readinessReason
) {
    public DenoiseOutputContract {
        outputGeneration = Math.max(0L, outputGeneration);
        width = Math.max(0, width);
        height = Math.max(0, height);
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? defaultReason(denoisedDiffuseOutput, directShadowOutput, rejectionMaskOutput)
                : readinessReason;
    }

    public static DenoiseOutputContract disabled(String reason) {
        return new DenoiseOutputContract(
                false,
                false,
                false,
                true,
                true,
                0L,
                0,
                0,
                reason
        );
    }

    public boolean hasWritableOutput() {
        return this.width > 0
                && this.height > 0
                && (this.denoisedDiffuseOutput || this.directShadowOutput || this.rejectionMaskOutput);
    }

    private static String defaultReason(
            boolean denoisedDiffuseOutput,
            boolean directShadowOutput,
            boolean rejectionMaskOutput
    ) {
        if (denoisedDiffuseOutput || directShadowOutput || rejectionMaskOutput) {
            return "denoise output contract advertises writable outputs";
        }
        return "denoise output contract is metadata-only";
    }
}
