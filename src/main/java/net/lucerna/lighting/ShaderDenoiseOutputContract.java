package net.lucerna.lighting;

public record ShaderDenoiseOutputContract(
        boolean contractReady,
        boolean dispatchPathImplemented,
        boolean shaderWritableOutput,
        boolean shaderDispatchPrepared,
        boolean shaderOutputImageReady,
        boolean shaderOutputMaterialReady,
        boolean shaderGeneratedOutput,
        boolean cpuReadbackFallbackActive,
        boolean realDenoiseShaderOutput,
        boolean geometryAwareInputsBound,
        boolean edgePreservationInputsBound,
        boolean temporalInputsBound,
        boolean historyRejectionInputsBound,
        boolean varianceInputsBound,
        boolean confidenceInputsBound,
        long generation,
        int width,
        int height,
        String shaderResource,
        String denoisedDiffuseResource,
        String rejectionMaskResource,
        String historyRejectionResource,
        String varianceConfidenceResource,
        String evidenceLabel,
        String outputExecutionBoundary,
        String rawVsDenoisedQualityBoundary,
        String pendingReason,
        String readinessReason
) {
    public static final String DEFAULT_SHADER_RESOURCE = "lucerna:denoise/diffuse_edge_aware_contract";
    public static final String DEFAULT_HISTORY_VARIANCE_RESOURCE = "lucerna:denoise/history_variance_quality_contract";
    public static final String DEFAULT_DENOISED_DIFFUSE_RESOURCE = "lucerna.denoise.diffuse";
    public static final String DEFAULT_REJECTION_MASK_RESOURCE = "lucerna.denoise.rejectionMask";
    public static final String DEFAULT_VARIANCE_CONFIDENCE_RESOURCE = "VarianceConfidence";

    public ShaderDenoiseOutputContract {
        generation = Math.max(0L, generation);
        width = Math.max(0, width);
        height = Math.max(0, height);
        shaderResource = normalizeText(shaderResource, DEFAULT_SHADER_RESOURCE);
        denoisedDiffuseResource = normalizeText(denoisedDiffuseResource, DEFAULT_DENOISED_DIFFUSE_RESOURCE);
        rejectionMaskResource = normalizeText(rejectionMaskResource, DEFAULT_REJECTION_MASK_RESOURCE);
        historyRejectionResource = normalizeText(historyRejectionResource, DEFAULT_HISTORY_VARIANCE_RESOURCE);
        varianceConfidenceResource = normalizeText(varianceConfidenceResource, DEFAULT_VARIANCE_CONFIDENCE_RESOURCE);
        evidenceLabel = normalizeText(evidenceLabel, "shader_denoise_output_contract");
        outputExecutionBoundary = normalizeText(outputExecutionBoundary, defaultOutputExecutionBoundary(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                cpuReadbackFallbackActive,
                realDenoiseShaderOutput
        ));
        rawVsDenoisedQualityBoundary = normalizeText(rawVsDenoisedQualityBoundary, defaultQualityBoundary(
                realDenoiseShaderOutput,
                edgePreservationInputsBound,
                historyRejectionInputsBound,
                varianceInputsBound,
                confidenceInputsBound
        ));
        pendingReason = normalizeText(pendingReason, defaultPendingReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                cpuReadbackFallbackActive,
                realDenoiseShaderOutput
        ));
        readinessReason = normalizeText(readinessReason, defaultReadinessReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                cpuReadbackFallbackActive,
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
                false,
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
                DEFAULT_HISTORY_VARIANCE_RESOURCE,
                DEFAULT_VARIANCE_CONFIDENCE_RESOURCE,
                "shader_denoise_disabled",
                "shader denoise execution is disabled",
                "raw-vs-denoised quality cannot be compared while denoise is disabled",
                reason,
                reason
        );
    }

    public static ShaderDenoiseOutputContract contractOnly(
            boolean contractReady,
            boolean geometryAwareInputsBound,
            boolean edgePreservationInputsBound,
            boolean temporalInputsBound,
            boolean historyRejectionInputsBound,
            boolean varianceInputsBound,
            boolean confidenceInputsBound,
            long generation,
            int width,
            int height,
            String outputExecutionBoundary,
            String rawVsDenoisedQualityBoundary,
            String pendingReason
    ) {
        return new ShaderDenoiseOutputContract(
                contractReady,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                geometryAwareInputsBound,
                edgePreservationInputsBound,
                temporalInputsBound,
                historyRejectionInputsBound,
                varianceInputsBound,
                confidenceInputsBound,
                generation,
                width,
                height,
                DEFAULT_SHADER_RESOURCE,
                DEFAULT_DENOISED_DIFFUSE_RESOURCE,
                DEFAULT_REJECTION_MASK_RESOURCE,
                DEFAULT_HISTORY_VARIANCE_RESOURCE,
                DEFAULT_VARIANCE_CONFIDENCE_RESOURCE,
                "shader_denoise_contract_only",
                outputExecutionBoundary,
                rawVsDenoisedQualityBoundary,
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
                && this.shaderDispatchPrepared
                && this.shaderOutputImageReady
                && this.shaderOutputMaterialReady
                && this.shaderGeneratedOutput
                && !this.cpuReadbackFallbackActive
                && this.realDenoiseShaderOutput
                && this.width > 0
                && this.height > 0;
    }

    public String statusSummary() {
        return "shaderDenoise contractReady=" + this.contractReady
                + " dispatchPathImplemented=" + this.dispatchPathImplemented
                + " shaderWritableOutput=" + this.shaderWritableOutput
                + " shaderDispatchPrepared=" + this.shaderDispatchPrepared
                + " shaderOutputImageReady=" + this.shaderOutputImageReady
                + " shaderOutputMaterialReady=" + this.shaderOutputMaterialReady
                + " shaderGeneratedOutput=" + this.shaderGeneratedOutput
                + " cpuReadbackFallbackActive=" + this.cpuReadbackFallbackActive
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " geometryInputs=" + this.geometryAwareInputsBound
                + " edgePreservationInputs=" + this.edgePreservationInputsBound
                + " temporalInputs=" + this.temporalInputsBound
                + " historyRejectionInputs=" + this.historyRejectionInputsBound
                + " varianceInputs=" + this.varianceInputsBound
                + " confidenceInputs=" + this.confidenceInputsBound
                + " shader=" + this.shaderResource
                + " output=" + this.denoisedDiffuseResource
                + " rejectionMask=" + this.rejectionMaskResource
                + " historyRejection=" + this.historyRejectionResource
                + " varianceConfidence=" + this.varianceConfidenceResource
                + " executionBoundary=" + this.outputExecutionBoundary
                + " qualityBoundary=" + this.rawVsDenoisedQualityBoundary
                + " reason=" + this.readinessReason;
    }

    public boolean shaderDenoiseInputsCompleteForDispatch() {
        return this.contractReady
                && this.geometryAwareInputsBound
                && this.edgePreservationInputsBound
                && this.temporalInputsBound
                && this.historyRejectionInputsBound
                && this.varianceInputsBound
                && this.confidenceInputsBound
                && this.width > 0
                && this.height > 0;
    }

    public String pendingChecklist() {
        return "shaderDenoiseChecklist"
                + " inputsCompleteForDispatch=" + this.shaderDenoiseInputsCompleteForDispatch()
                + " dispatchPathImplemented=" + this.dispatchPathImplemented
                + " shaderWritableOutput=" + this.shaderWritableOutput
                + " shaderDispatchPrepared=" + this.shaderDispatchPrepared
                + " shaderOutputImageReady=" + this.shaderOutputImageReady
                + " shaderOutputMaterialReady=" + this.shaderOutputMaterialReady
                + " shaderGeneratedOutput=" + this.shaderGeneratedOutput
                + " cpuReadbackFallbackActive=" + this.cpuReadbackFallbackActive
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " pending=" + this.pendingReason;
    }

    public String outputReadinessSummary() {
        return "shaderDenoiseOutputReadiness"
                + " dispatchPrepared=" + this.shaderDispatchPrepared
                + " imageReady=" + this.shaderOutputImageReady
                + " materialReady=" + this.shaderOutputMaterialReady
                + " shaderGenerated=" + this.shaderGeneratedOutput
                + " cpuFallback=" + this.cpuReadbackFallbackActive
                + " realShaderOutput=" + this.realDenoiseShaderOutput
                + " readyForProof=" + this.readyForControllerShaderProof()
                + " reason=" + this.readinessReason;
    }

    public String qualityBoundarySummary() {
        return "rawVsDenoisedQualityBoundary=" + this.rawVsDenoisedQualityBoundary
                + "; executionBoundary=" + this.outputExecutionBoundary;
    }

    private static String defaultPendingReason(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean shaderDispatchPrepared,
            boolean shaderOutputImageReady,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean cpuReadbackFallbackActive,
            boolean realDenoiseShaderOutput
    ) {
        if (!contractReady) {
            return "shader denoise inputs/resources are incomplete";
        }
        if (!dispatchPathImplemented) {
            return "shader denoise dispatch path is not wired yet";
        }
        if (!shaderDispatchPrepared) {
            return "shader denoise dispatch is not prepared for the current frame";
        }
        if (!shaderWritableOutput) {
            return "shader denoise output attachment is not writable yet";
        }
        if (!shaderOutputImageReady) {
            return "shader denoise output image is not ready";
        }
        if (!shaderOutputMaterialReady) {
            return "shader denoise material/descriptors are not ready";
        }
        if (!shaderGeneratedOutput) {
            return cpuReadbackFallbackActive
                    ? "CPU/readback denoise fallback is active; shader output has not generated pixels"
                    : "shader denoise output has not generated pixels";
        }
        if (!realDenoiseShaderOutput) {
            return "shader denoise output has not been controller-validated";
        }
        return "shader denoise output has no pending reason";
    }

    private static String defaultOutputExecutionBoundary(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean shaderDispatchPrepared,
            boolean shaderOutputImageReady,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean cpuReadbackFallbackActive,
            boolean realDenoiseShaderOutput
    ) {
        if (!contractReady) {
            return "resource contract incomplete; no shader dispatch or output claim";
        }
        if (!dispatchPathImplemented) {
            return "contract-only resource; scheduler has not dispatched a denoise shader";
        }
        if (!shaderDispatchPrepared) {
            return "dispatch path exists but no current-frame shader dispatch has been prepared";
        }
        if (!shaderWritableOutput) {
            return "dispatch path exists but writable denoise output is not bound";
        }
        if (!shaderOutputImageReady || !shaderOutputMaterialReady) {
            return "shader output target or descriptor material is not ready for generated output";
        }
        if (!shaderGeneratedOutput) {
            return cpuReadbackFallbackActive
                    ? "CPU/readback fallback is active; shader output has no generated pixels"
                    : "shader output target is ready but no shader-generated pixels are proven";
        }
        if (!realDenoiseShaderOutput) {
            return "shader wrote an output candidate but controller proof has not validated it";
        }
        return "controller-validated shader output may be compared against raw GI";
    }

    private static String defaultQualityBoundary(
            boolean realDenoiseShaderOutput,
            boolean edgePreservationInputsBound,
            boolean historyRejectionInputsBound,
            boolean varianceInputsBound,
            boolean confidenceInputsBound
    ) {
        if (!realDenoiseShaderOutput) {
            return "raw-vs-denoised quality remains a contract boundary; current evidence must not be treated as real shader denoise output";
        }
        if (!edgePreservationInputsBound) {
            return "shader denoise output lacks edge-preservation inputs for quality validation";
        }
        if (!historyRejectionInputsBound) {
            return "shader denoise output lacks history rejection inputs for temporal quality validation";
        }
        if (!varianceInputsBound || !confidenceInputsBound) {
            return "shader denoise output lacks variance/confidence inputs for noise-quality validation";
        }
        return "raw and shader-denoised outputs can be compared for edge, temporal, and variance quality";
    }

    private static String defaultReadinessReason(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean shaderDispatchPrepared,
            boolean shaderOutputImageReady,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean cpuReadbackFallbackActive,
            boolean realDenoiseShaderOutput
    ) {
        if (contractReady
                && dispatchPathImplemented
                && shaderWritableOutput
                && shaderDispatchPrepared
                && shaderOutputImageReady
                && shaderOutputMaterialReady
                && shaderGeneratedOutput
                && !cpuReadbackFallbackActive
                && realDenoiseShaderOutput) {
            return "real shader denoise output is available for validation";
        }
        return defaultPendingReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                cpuReadbackFallbackActive,
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
