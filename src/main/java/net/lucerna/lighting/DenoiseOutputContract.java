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
        String denoisedDiffuseResource,
        String directShadowResource,
        String rejectionMaskResource,
        String evidenceLabel,
        String readinessReason
) {
    public DenoiseOutputContract {
        outputGeneration = Math.max(0L, outputGeneration);
        width = Math.max(0, width);
        height = Math.max(0, height);
        denoisedDiffuseResource = normalizeText(denoisedDiffuseResource, "lucerna.denoise.diffuse");
        directShadowResource = normalizeText(directShadowResource, "lucerna.denoise.directShadows");
        rejectionMaskResource = normalizeText(rejectionMaskResource, "lucerna.denoise.rejectionMask");
        evidenceLabel = normalizeText(evidenceLabel, "denoised_output_intent");
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
                "lucerna.denoise.diffuse",
                "lucerna.denoise.directShadows",
                "lucerna.denoise.rejectionMask",
                "denoise_output_disabled",
                reason
        );
    }

    public static DenoiseOutputContract diffuseOutputIntent(
            boolean denoisedDiffuseOutput,
            boolean directShadowOutput,
            boolean rejectionMaskOutput,
            long outputGeneration,
            int width,
            int height,
            String readinessReason
    ) {
        return new DenoiseOutputContract(
                denoisedDiffuseOutput,
                directShadowOutput,
                rejectionMaskOutput,
                true,
                true,
                outputGeneration,
                width,
                height,
                "lucerna.denoise.diffuse",
                "lucerna.denoise.directShadows",
                "lucerna.denoise.rejectionMask",
                "denoised_diffuse_output_intent",
                readinessReason
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

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
