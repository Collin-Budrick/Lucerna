package net.lucerna.lighting;

public record ShaderDenoiseOutputContract(
        boolean contractReady,
        boolean dispatchPathImplemented,
        boolean shaderWritableOutput,
        boolean shaderDispatchPrepared,
        boolean shaderOutputImageReady,
        boolean shaderOutputImageOwnedByShaderPass,
        boolean shaderOutputStorageWritable,
        boolean shaderOutputBarrierReady,
        boolean shaderOutputFinalCompositeConsumable,
        boolean shaderOutputMaterialReady,
        boolean shaderGeneratedOutput,
        boolean publicMojangShaderVisualOutputAttempted,
        boolean publicMojangShaderVisualOutputSubmitted,
        boolean publicMojangShaderVisualOutputReady,
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
        String readinessReason,
        boolean shaderOutputImageCandidateReady,
        boolean shaderOutputImageCandidateCpuStaged,
        boolean shaderOutputImageCandidateNonGpu,
        String shaderOutputBlockerReason
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
                shaderOutputImageOwnedByShaderPass,
                shaderOutputStorageWritable,
                shaderOutputBarrierReady,
                shaderOutputFinalCompositeConsumable,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                publicMojangShaderVisualOutputReady,
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
                shaderOutputImageOwnedByShaderPass,
                shaderOutputStorageWritable,
                shaderOutputBarrierReady,
                shaderOutputFinalCompositeConsumable,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                publicMojangShaderVisualOutputReady,
                cpuReadbackFallbackActive,
                realDenoiseShaderOutput
        ));
        readinessReason = normalizeText(readinessReason, defaultReadinessReason(
                contractReady,
                dispatchPathImplemented,
                shaderWritableOutput,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputImageOwnedByShaderPass,
                shaderOutputStorageWritable,
                shaderOutputBarrierReady,
                shaderOutputFinalCompositeConsumable,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                publicMojangShaderVisualOutputReady,
                cpuReadbackFallbackActive,
                realDenoiseShaderOutput
        ));
        shaderOutputBlockerReason = normalizeText(shaderOutputBlockerReason, pendingReason);
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
                reason,
                false,
                false,
                false,
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
                false,
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
                        : "shader denoise resource contract is not ready",
                false,
                false,
                false,
                pendingReason
        );
    }

    public static ShaderDenoiseOutputContract fromDenoiseExecutionSnapshot(
            net.lucerna.nativebridge.DenoiseExecutionSnapshot snapshot
    ) {
        if (snapshot == null || !snapshot.hasExecutionTelemetry()) {
            return disabled("native denoise execution telemetry is unavailable");
        }
        boolean candidateOnly = snapshot.shaderDenoiseOutputImageCandidatePresent();
        return new ShaderDenoiseOutputContract(
                snapshot.hasExecutionTelemetry(),
                snapshot.shaderDenoiseDispatchPrepared(),
                snapshot.shaderDenoiseRealOutputPathReady(),
                snapshot.shaderDenoiseDispatchPrepared(),
                snapshot.shaderDenoiseOutputImageReady(),
                snapshot.shaderDenoiseOutputImageOwnedByShaderPass(),
                snapshot.shaderDenoiseOutputStorageWritable(),
                snapshot.shaderDenoiseOutputBarrierReady(),
                snapshot.shaderDenoiseOutputFinalCompositeConsumable(),
                snapshot.shaderDenoiseOutputMaterialReady(),
                snapshot.shaderDenoiseShaderGeneratedOutput(),
                snapshot.publicMojangShaderVisualOutputAttempted(),
                snapshot.publicMojangShaderVisualOutputSubmitted(),
                snapshot.publicMojangShaderVisualOutputReady(),
                snapshot.shaderDenoiseCpuReadbackFallbackActive(),
                snapshot.realShaderDenoiseOutputReady(),
                snapshot.rawGiInputReady(),
                snapshot.edgeInputsAvailable(),
                snapshot.temporalHistory(),
                snapshot.hasHistoryCounters(),
                snapshot.historyConfidenceAvailable(),
                snapshot.historyConfidenceAvailable(),
                snapshot.dispatchGeneration(),
                snapshot.width(),
                snapshot.height(),
                DEFAULT_SHADER_RESOURCE,
                DEFAULT_DENOISED_DIFFUSE_RESOURCE,
                DEFAULT_REJECTION_MASK_RESOURCE,
                DEFAULT_HISTORY_VARIANCE_RESOURCE,
                DEFAULT_VARIANCE_CONFIDENCE_RESOURCE,
                snapshot.realShaderDenoiseOutputReady()
                        ? "shader_denoise_real_shader_output"
                        : candidateOnly
                                ? "shader_denoise_candidate_image_only"
                                : "shader_denoise_cpu_readback_boundary",
                snapshot.shaderDenoiseOutputImageCandidateBoundary(),
                snapshot.denoiseReadinessBoundary(),
                snapshot.shaderDenoiseOutputBlockerReason(),
                snapshot.shaderDenoiseOutputReadinessLabel(),
                candidateOnly,
                snapshot.shaderDenoiseOutputImageCandidateCpuStaged(),
                snapshot.shaderDenoiseOutputImageCandidateNonGpu(),
                snapshot.shaderDenoiseOutputBlockerReason()
        );
    }

    public boolean readyForControllerShaderProof() {
        return this.contractReady
                && this.dispatchPathImplemented
                && this.shaderWritableOutput
                && this.shaderDispatchPrepared
                && this.shaderOutputImageReady
                && this.shaderOutputImageOwnedByShaderPass
                && this.shaderOutputStorageWritable
                && this.shaderOutputBarrierReady
                && this.shaderOutputFinalCompositeConsumable
                && this.shaderOutputMaterialReady
                && this.shaderGeneratedOutput
                && !this.publicMojangShaderVisualOutputReady
                && !this.cpuReadbackFallbackActive
                && this.realDenoiseShaderOutput
                && !this.shaderOutputImageCandidateReady
                && !this.shaderOutputImageCandidateCpuStaged
                && !this.shaderOutputImageCandidateNonGpu
                && this.width > 0
                && this.height > 0;
    }

    public String statusSummary() {
        return "shaderDenoise contractReady=" + this.contractReady
                + " dispatchPathImplemented=" + this.dispatchPathImplemented
                + " shaderWritableOutput=" + this.shaderWritableOutput
                + " shaderDispatchPrepared=" + this.shaderDispatchPrepared
                + " shaderOutputImageReady=" + this.shaderOutputImageReady
                + " shaderOutputImageOwnedByShaderPass=" + this.shaderOutputImageOwnedByShaderPass
                + " shaderOutputStorageWritable=" + this.shaderOutputStorageWritable
                + " shaderOutputBarrierReady=" + this.shaderOutputBarrierReady
                + " shaderOutputFinalCompositeConsumable=" + this.shaderOutputFinalCompositeConsumable
                + " shaderOutputMaterialReady=" + this.shaderOutputMaterialReady
                + " shaderGeneratedOutput=" + this.shaderGeneratedOutput
                + " publicMojangVisualAttempted=" + this.publicMojangShaderVisualOutputAttempted
                + " publicMojangVisualSubmitted=" + this.publicMojangShaderVisualOutputSubmitted
                + " publicMojangVisualReady=" + this.publicMojangShaderVisualOutputReady
                + " cpuReadbackFallbackActive=" + this.cpuReadbackFallbackActive
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " candidateOnly=" + this.shaderOutputImageCandidateReady
                + " candidateCpuStaged=" + this.shaderOutputImageCandidateCpuStaged
                + " candidateNonGpu=" + this.shaderOutputImageCandidateNonGpu
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
                + " publicMojangVisualBoundary=" + this.publicMojangShaderVisualOutputBoundary()
                + " qualityBoundary=" + this.rawVsDenoisedQualityBoundary
                + " blocker=" + this.shaderOutputBlockerReason
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
                + " shaderOutputImageOwnedByShaderPass=" + this.shaderOutputImageOwnedByShaderPass
                + " shaderOutputStorageWritable=" + this.shaderOutputStorageWritable
                + " shaderOutputBarrierReady=" + this.shaderOutputBarrierReady
                + " shaderOutputFinalCompositeConsumable=" + this.shaderOutputFinalCompositeConsumable
                + " shaderOutputMaterialReady=" + this.shaderOutputMaterialReady
                + " shaderGeneratedOutput=" + this.shaderGeneratedOutput
                + " publicMojangVisualAttempted=" + this.publicMojangShaderVisualOutputAttempted
                + " publicMojangVisualSubmitted=" + this.publicMojangShaderVisualOutputSubmitted
                + " publicMojangVisualReady=" + this.publicMojangShaderVisualOutputReady
                + " cpuReadbackFallbackActive=" + this.cpuReadbackFallbackActive
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " candidateOnly=" + this.shaderOutputImageCandidateReady
                + " candidateCpuStaged=" + this.shaderOutputImageCandidateCpuStaged
                + " candidateNonGpu=" + this.shaderOutputImageCandidateNonGpu
                + " pending=" + this.pendingReason;
    }

    public String outputReadinessSummary() {
        return "shaderDenoiseOutputReadiness"
                + " dispatchPrepared=" + this.shaderDispatchPrepared
                + " imageReady=" + this.shaderOutputImageReady
                + " imageOwnedByShaderPass=" + this.shaderOutputImageOwnedByShaderPass
                + " storageWritable=" + this.shaderOutputStorageWritable
                + " barrierReady=" + this.shaderOutputBarrierReady
                + " finalCompositeConsumable=" + this.shaderOutputFinalCompositeConsumable
                + " materialReady=" + this.shaderOutputMaterialReady
                + " shaderGenerated=" + this.shaderGeneratedOutput
                + " publicMojangVisualAttempted=" + this.publicMojangShaderVisualOutputAttempted
                + " publicMojangVisualSubmitted=" + this.publicMojangShaderVisualOutputSubmitted
                + " publicMojangVisualReady=" + this.publicMojangShaderVisualOutputReady
                + " cpuFallback=" + this.cpuReadbackFallbackActive
                + " realShaderOutput=" + this.realDenoiseShaderOutput
                + " candidateOnly=" + this.shaderOutputImageCandidateReady
                + " candidateCpuStaged=" + this.shaderOutputImageCandidateCpuStaged
                + " candidateNonGpu=" + this.shaderOutputImageCandidateNonGpu
                + " readyForProof=" + this.readyForControllerShaderProof()
                + " publicMojangVisualBoundary=" + this.publicMojangShaderVisualOutputBoundary()
                + " blocker=" + this.shaderOutputBlockerReason
                + " reason=" + this.readinessReason;
    }

    public String qualityBoundarySummary() {
        return "rawVsDenoisedQualityBoundary=" + this.rawVsDenoisedQualityBoundary
                + "; executionBoundary=" + this.outputExecutionBoundary
                + "; publicMojangVisualBoundary=" + this.publicMojangShaderVisualOutputBoundary();
    }

    public String publicMojangShaderVisualOutputBoundary() {
        if (this.publicMojangShaderVisualOutputReady) {
            return "public Mojang shader visual output ready; not real compute/native shader denoise output";
        }
        if (this.publicMojangShaderVisualOutputSubmitted) {
            return "public Mojang shader visual output submitted; readiness not proven";
        }
        if (this.publicMojangShaderVisualOutputAttempted) {
            return "public Mojang shader visual output attempted; submission/readiness not proven";
        }
        return "public Mojang shader visual output not reported";
    }

    private static String defaultPendingReason(
            boolean contractReady,
            boolean dispatchPathImplemented,
            boolean shaderWritableOutput,
            boolean shaderDispatchPrepared,
            boolean shaderOutputImageReady,
            boolean shaderOutputImageOwnedByShaderPass,
            boolean shaderOutputStorageWritable,
            boolean shaderOutputBarrierReady,
            boolean shaderOutputFinalCompositeConsumable,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean publicMojangShaderVisualOutputReady,
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
        if (!shaderOutputImageOwnedByShaderPass) {
            return "shader denoise output image is not owned by the shader denoise pass";
        }
        if (!shaderOutputStorageWritable) {
            return "shader denoise output storage/image binding is not writable";
        }
        if (!shaderOutputBarrierReady) {
            return "shader denoise output memory/layout barrier is not ready";
        }
        if (!shaderOutputFinalCompositeConsumable) {
            return "shader denoise output is not consumable by final composite";
        }
        if (!shaderOutputMaterialReady) {
            return "shader denoise material/descriptors are not ready";
        }
        if (publicMojangShaderVisualOutputReady) {
            return "public Mojang visual shader output is ready, but it is not a real shader denoise output target";
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
            boolean shaderOutputImageOwnedByShaderPass,
            boolean shaderOutputStorageWritable,
            boolean shaderOutputBarrierReady,
            boolean shaderOutputFinalCompositeConsumable,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean publicMojangShaderVisualOutputReady,
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
        if (!shaderOutputImageOwnedByShaderPass
                || !shaderOutputStorageWritable
                || !shaderOutputBarrierReady
                || !shaderOutputFinalCompositeConsumable) {
            return "shader output target exists but lacks owned writable storage, barriers, or final-composite handoff";
        }
        if (publicMojangShaderVisualOutputReady) {
            return "public Mojang visual shader output is separate from real shader-generated denoise output";
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
            boolean shaderOutputImageOwnedByShaderPass,
            boolean shaderOutputStorageWritable,
            boolean shaderOutputBarrierReady,
            boolean shaderOutputFinalCompositeConsumable,
            boolean shaderOutputMaterialReady,
            boolean shaderGeneratedOutput,
            boolean publicMojangShaderVisualOutputReady,
            boolean cpuReadbackFallbackActive,
            boolean realDenoiseShaderOutput
    ) {
        if (contractReady
                && dispatchPathImplemented
                && shaderWritableOutput
                && shaderDispatchPrepared
                && shaderOutputImageReady
                && shaderOutputImageOwnedByShaderPass
                && shaderOutputStorageWritable
                && shaderOutputBarrierReady
                && shaderOutputFinalCompositeConsumable
                && shaderOutputMaterialReady
                && shaderGeneratedOutput
                && !publicMojangShaderVisualOutputReady
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
                shaderOutputImageOwnedByShaderPass,
                shaderOutputStorageWritable,
                shaderOutputBarrierReady,
                shaderOutputFinalCompositeConsumable,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                publicMojangShaderVisualOutputReady,
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
