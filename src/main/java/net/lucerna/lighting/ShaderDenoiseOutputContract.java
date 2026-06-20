package net.lucerna.lighting;

public record ShaderDenoiseOutputContract(
        boolean contractReady,
        boolean dispatchPathImplemented,
        boolean shaderWritableOutput,
        boolean realDenoiseShaderOutput,
        boolean geometryAwareInputsBound,
        boolean temporalInputsBound,
        boolean varianceInputsBound,
        long generation,
        int width,
        int height,
        String shaderResource,
        String denoisedDiffuseResource,
        String rejectionMaskResource,
        String evidenceLabel,
        String pendingReason,
        String readinessReason
) {
    public static final String DEFAULT_SHADER_RESOURCE = "lucerna:denoise/diffuse_edge_aware_contract";
    public static final String DEFAULT_DENOISED_DIFFUSE_RESOURCE = "lucerna.denoise.diffuse";
    public static final String DEFAULT_REJECTION_MASK_RESOURCE = "lucerna.denoise.rejectionMask";

    public ShaderDenoiseOutputContract {
        generation = Math.max(0L, generation);
        width = Math.max(0, width);
        height = Math.max(0, height);
        shaderResource = normalizeText(shaderResource, DEFAULT_SHADER_RESOURCE);
        denoisedDiffuseResource = normalizeText(denoisedDiffuseResource, DEFAULT_DENOISED_DIFFUSE_RESOURCE);
        rejectionMaskResource = normalizeText(rejectionMaskResource, DEFAULT_REJECTION_MASK_RESOURCE);
        evidenceLabel = normalizeText(evidenceLabel, "shader_denoise_output_contract");
        pendingReason = normalizeText(pendingReason, defaultPendingReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                realDenoiseShaderOutput
        ));
        readinessReason = normalizeText(readinessReason, defaultReadinessReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                realDenoiseShaderOutput
        ));
    }

    public static ShaderDenoiseOutputContract disabled(String reason) {
        return new ShaderDenoiseOutputContract(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0L,
                0,
                0,
                DEFAULT_SHADER_RESOURCE,
                DEFAULT_DENOISED_DIFFUSE_RESOURCE,
                DEFAULT_REJECTION_MASK_RESOURCE,
                "shader_denoise_disabled",
                reason,
                reason
        );
    }

    public static ShaderDenoiseOutputContract contractOnly(
            boolean contractReady,
            boolean geometryAwareInputsBound,
            boolean temporalInputsBound,
            boolean varianceInputsBound,
            long generation,
            int width,
            int height,
            String pendingReason
    ) {
        return new ShaderDenoiseOutputContract(
                contractReady,
                false,
                false,
                false,
                geometryAwareInputsBound,
                temporalInputsBound,
                varianceInputsBound,
                generation,
                width,
                height,
                DEFAULT_SHADER_RESOURCE,
                DEFAULT_DENOISED_DIFFUSE_RESOURCE,
                DEFAULT_REJECTION_MASK_RESOURCE,
                "shader_denoise_contract_only",
                pendingReason,
                contractReady
                        ? "shader denoise resource contract is ready; dispatch/output path remains pending"
                        : "shader denoise resource contract is not ready"
        );
    }

    public boolean readyForControllerShaderProof() {
        return this.contractReady
                && this.dispatchPathImplemented
                && this.shaderWritableOutput
                && this.realDenoiseShaderOutput
                && this.width > 0
                && this.height > 0;
    }

    public String statusSummary() {
        return "shaderDenoise contractReady=" + this.contractReady
                + " dispatchPathImplemented=" + this.dispatchPathImplemented
                + " shaderWritableOutput=" + this.shaderWritableOutput
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " geometryInputs=" + this.geometryAwareInputsBound
                + " temporalInputs=" + this.temporalInputsBound
                + " varianceInputs=" + this.varianceInputsBound
                + " shader=" + this.shaderResource
                + " output=" + this.denoisedDiffuseResource
                + " rejectionMask=" + this.rejectionMaskResource
                + " reason=" + this.readinessReason;
    }

    private static String defaultPendingReason(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean realDenoiseShaderOutput
    ) {
        if (!contractReady) {
            return "shader denoise inputs/resources are incomplete";
        }
        if (!dispatchPathImplemented) {
            return "shader denoise dispatch path is not wired yet";
        }
        if (!shaderWritableOutput) {
            return "shader denoise output attachment is not writable yet";
        }
        if (!realDenoiseShaderOutput) {
            return "shader denoise output has not been controller-validated";
        }
        return "shader denoise output has no pending reason";
    }

    private static String defaultReadinessReason(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean realDenoiseShaderOutput
    ) {
        if (contractReady && dispatchPathImplemented && shaderWritableOutput && realDenoiseShaderOutput) {
            return "real shader denoise output is available for validation";
        }
        return defaultPendingReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                realDenoiseShaderOutput
        );
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
