package net.lucerna.nativebridge;

public record DenoiseExecutionSnapshot(
        boolean nativeStatusAvailable,
        boolean denoiseExecutionAvailable,
        long dispatchGeneration,
        long packetGeneration,
        int width,
        int height,
        int inputCount,
        int outputCount,
        int sampleCount,
        int historyAcceptedCount,
        int historyRejectedCount,
        int edgeRejectedCount,
        int edgePreservedCount,
        int rawGiPixels,
        int rawGiSamples,
        int rawGiRays,
        int rawGiCacheReads,
        int denoisedOutputPixels,
        long denoisedOutputChecksum,
        int denoisedOutputChangedPixels,
        int denoisedOutputMeanAbsDelta,
        long previousDenoisedOutputChecksum,
        long currentDenoisedOutputChecksum,
        int frameToFrameChangedPixels,
        int frameToFrameMeanAbsDelta,
        int temporalStablePixels,
        int temporalUnstablePixels,
        double temporalHistoryConfidence,
        double temporalFlickerScore,
        int shaderDenoiseOutputImageCandidateWidth,
        int shaderDenoiseOutputImageCandidateHeight,
        int shaderDenoiseOutputImageCandidatePixels,
        long shaderDenoiseOutputImageCandidateBytes,
        long shaderDenoiseOutputImageCandidateChecksum,
        int shaderGeneratedDenoiseOutputWidth,
        int shaderGeneratedDenoiseOutputHeight,
        long shaderGeneratedDenoiseOutputTexelCount,
        long shaderGeneratedDenoiseOutputSampleCount,
        long shaderGeneratedDenoiseOutputChecksum,
        int compositeWidth,
        int compositeHeight,
        int compositeOutputCount,
        boolean enabled,
        boolean validated,
        boolean placeholder,
        boolean temporalHistory,
        boolean edgeInputsAvailable,
        boolean directShadowSignalAvailable,
        boolean diffuseGiSignalAvailable,
        boolean optionalSpecularPlaceholder,
        boolean optionalAoPlaceholder,
        boolean rawGiInputAvailable,
        boolean rawDirectInputAvailable,
        boolean shaderDenoiseRawDiffuseGiInputReady,
        boolean shaderDenoiseDirectLightValidationInputReady,
        boolean shaderDenoiseDirectLightValidationInputActive,
        boolean shaderDenoisePhysicalGiInputEvidence,
        boolean shaderDenoiseRealTracedInputReady,
        boolean denoisedOutputIntent,
        boolean denoisedCpuOutputGenerated,
        boolean denoisedOutputDiffersFromRaw,
        boolean realDenoiseShaderOutput,
        boolean rawGiInputReady,
        boolean cpuDenoisedReadbackReady,
        boolean shaderDenoiseDispatchPrepared,
        boolean shaderDenoiseInputReady,
        boolean publicMojangShaderVisualOutputAttempted,
        boolean publicMojangShaderVisualOutputSubmitted,
        boolean publicMojangShaderVisualOutputReady,
        boolean shaderDenoiseOutputReady,
        boolean shaderDenoiseOutputImageReady,
        boolean shaderDenoiseOutputImageOwnedByShaderPass,
        boolean shaderDenoiseOutputStorageWritable,
        boolean shaderDenoiseOutputBarrierReady,
        boolean shaderDenoiseOutputFinalCompositeConsumable,
        boolean shaderDenoiseOutputImageCandidateReady,
        boolean shaderDenoiseOutputImageCandidateCpuStaged,
        boolean shaderDenoiseOutputImageCandidateNonGpu,
        boolean shaderDenoiseOutputMaterialReady,
        boolean shaderDenoiseShaderGeneratedOutput,
        boolean shaderGeneratedDenoiseOutputEvidence,
        boolean shaderGeneratedDenoiseOutputReported,
        boolean shaderGeneratedDenoiseOutputImageReady,
        boolean shaderGeneratedDenoiseOutputGenerated,
        boolean shaderGeneratedDenoiseOutputFinalCompositeConsumed,
        boolean shaderGeneratedDenoiseOutputReportReady,
        boolean shaderDenoiseCpuReadbackFallbackReported,
        boolean temporalReady,
        boolean temporalGhostingRisk,
        boolean cpuFallbackQualityMetrics,
        boolean compositeStageRecorded,
        boolean compositeEnabled,
        boolean compositeReady,
        boolean compositePlaceholder,
        boolean edgeDepthAvailable,
        boolean edgeNormalAvailable,
        boolean edgeMaterialAvailable,
        boolean historyConfidenceAvailable,
        boolean ready,
        boolean accepted,
        String outputMarker,
        String rawInputMarker,
        String shaderDenoiseInputKind,
        String shaderDenoiseInputStatus,
        String shaderDenoiseInputBlocker,
        String shaderDenoisePhysicalGiBlocker,
        String shaderDenoiseTracingBlocker,
        String denoisedOutputMarker,
        String temporalReadinessMarker,
        String temporalGhostingRiskMarker,
        String shaderDenoiseOutputImageCandidateMarker,
        String shaderDenoiseOutputImageBlocker,
        String shaderGeneratedDenoiseOutputMarker,
        String shaderGeneratedDenoiseOutputIdentity,
        String shaderGeneratedDenoiseOutputBlocker,
        String compositeMarker,
        String readinessReason
) {
    public DenoiseExecutionSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        packetGeneration = Math.max(0L, packetGeneration);
        width = Math.max(0, width);
        height = Math.max(0, height);
        inputCount = Math.max(0, inputCount);
        outputCount = Math.max(0, outputCount);
        sampleCount = Math.max(0, sampleCount);
        historyAcceptedCount = Math.max(0, historyAcceptedCount);
        historyRejectedCount = Math.max(0, historyRejectedCount);
        edgeRejectedCount = Math.max(0, edgeRejectedCount);
        edgePreservedCount = Math.max(0, edgePreservedCount);
        rawGiPixels = Math.max(0, rawGiPixels);
        rawGiSamples = Math.max(0, rawGiSamples);
        rawGiRays = Math.max(0, rawGiRays);
        rawGiCacheReads = Math.max(0, rawGiCacheReads);
        denoisedOutputPixels = Math.max(0, denoisedOutputPixels);
        denoisedOutputChecksum = Math.max(0L, denoisedOutputChecksum);
        denoisedOutputChangedPixels = Math.max(0, denoisedOutputChangedPixels);
        denoisedOutputMeanAbsDelta = Math.max(0, denoisedOutputMeanAbsDelta);
        previousDenoisedOutputChecksum = Math.max(0L, previousDenoisedOutputChecksum);
        currentDenoisedOutputChecksum = Math.max(0L, currentDenoisedOutputChecksum);
        frameToFrameChangedPixels = Math.max(0, frameToFrameChangedPixels);
        frameToFrameMeanAbsDelta = Math.max(0, frameToFrameMeanAbsDelta);
        temporalStablePixels = Math.max(0, temporalStablePixels);
        temporalUnstablePixels = Math.max(0, temporalUnstablePixels);
        temporalHistoryConfidence = Math.max(0.0D, temporalHistoryConfidence);
        temporalFlickerScore = Math.max(0.0D, temporalFlickerScore);
        shaderDenoiseOutputImageCandidateWidth = Math.max(0, shaderDenoiseOutputImageCandidateWidth);
        shaderDenoiseOutputImageCandidateHeight = Math.max(0, shaderDenoiseOutputImageCandidateHeight);
        shaderDenoiseOutputImageCandidatePixels = Math.max(0, shaderDenoiseOutputImageCandidatePixels);
        shaderDenoiseOutputImageCandidateBytes = Math.max(0L, shaderDenoiseOutputImageCandidateBytes);
        shaderDenoiseOutputImageCandidateChecksum = Math.max(0L, shaderDenoiseOutputImageCandidateChecksum);
        shaderGeneratedDenoiseOutputWidth = Math.max(0, shaderGeneratedDenoiseOutputWidth);
        shaderGeneratedDenoiseOutputHeight = Math.max(0, shaderGeneratedDenoiseOutputHeight);
        shaderGeneratedDenoiseOutputTexelCount = Math.max(0L, shaderGeneratedDenoiseOutputTexelCount);
        shaderGeneratedDenoiseOutputSampleCount = Math.max(0L, shaderGeneratedDenoiseOutputSampleCount);
        shaderGeneratedDenoiseOutputChecksum = Math.max(0L, shaderGeneratedDenoiseOutputChecksum);
        compositeWidth = Math.max(0, compositeWidth);
        compositeHeight = Math.max(0, compositeHeight);
        compositeOutputCount = Math.max(0, compositeOutputCount);
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
        rawInputMarker = rawInputMarker == null || rawInputMarker.isBlank() ? "unknown" : rawInputMarker;
        shaderDenoiseInputKind = shaderDenoiseInputKind == null || shaderDenoiseInputKind.isBlank()
                ? "unknown"
                : shaderDenoiseInputKind;
        shaderDenoiseInputStatus = shaderDenoiseInputStatus == null || shaderDenoiseInputStatus.isBlank()
                ? "unknown"
                : shaderDenoiseInputStatus;
        shaderDenoiseInputBlocker = shaderDenoiseInputBlocker == null || shaderDenoiseInputBlocker.isBlank()
                ? "unknown"
                : shaderDenoiseInputBlocker;
        shaderDenoisePhysicalGiBlocker = shaderDenoisePhysicalGiBlocker == null
                || shaderDenoisePhysicalGiBlocker.isBlank()
                ? "unknown"
                : shaderDenoisePhysicalGiBlocker;
        shaderDenoiseTracingBlocker = shaderDenoiseTracingBlocker == null || shaderDenoiseTracingBlocker.isBlank()
                ? "unknown"
                : shaderDenoiseTracingBlocker;
        denoisedOutputMarker = denoisedOutputMarker == null || denoisedOutputMarker.isBlank() ? "unknown" : denoisedOutputMarker;
        temporalReadinessMarker = temporalReadinessMarker == null || temporalReadinessMarker.isBlank()
                ? "unknown"
                : temporalReadinessMarker;
        temporalGhostingRiskMarker = temporalGhostingRiskMarker == null || temporalGhostingRiskMarker.isBlank()
                ? "unknown"
                : temporalGhostingRiskMarker;
        shaderDenoiseOutputImageCandidateMarker = shaderDenoiseOutputImageCandidateMarker == null
                || shaderDenoiseOutputImageCandidateMarker.isBlank()
                ? "unknown"
                : shaderDenoiseOutputImageCandidateMarker;
        shaderDenoiseOutputImageBlocker = shaderDenoiseOutputImageBlocker == null
                || shaderDenoiseOutputImageBlocker.isBlank()
                ? "unknown"
                : shaderDenoiseOutputImageBlocker;
        shaderGeneratedDenoiseOutputMarker = shaderGeneratedDenoiseOutputMarker == null
                || shaderGeneratedDenoiseOutputMarker.isBlank()
                ? "unknown"
                : shaderGeneratedDenoiseOutputMarker;
        shaderGeneratedDenoiseOutputIdentity = shaderGeneratedDenoiseOutputIdentity == null
                || shaderGeneratedDenoiseOutputIdentity.isBlank()
                ? "unknown"
                : shaderGeneratedDenoiseOutputIdentity;
        shaderGeneratedDenoiseOutputBlocker = shaderGeneratedDenoiseOutputBlocker == null
                || shaderGeneratedDenoiseOutputBlocker.isBlank()
                ? "unknown"
                : shaderGeneratedDenoiseOutputBlocker;
        compositeMarker = compositeMarker == null || compositeMarker.isBlank() ? "unknown" : compositeMarker;
        readinessReason = readinessReason == null || readinessReason.isBlank() ? "unknown" : readinessReason;
    }

    public static DenoiseExecutionSnapshot unavailable(String reason) {
        return unavailableStatus(false, false, reason);
    }

    private static DenoiseExecutionSnapshot unavailableStatus(
            boolean nativeStatusAvailable,
            boolean denoiseExecutionAvailable,
            String reason
    ) {
        return new DenoiseExecutionSnapshot(
                nativeStatusAvailable,
                denoiseExecutionAvailable,
                0L, // dispatchGeneration
                0L, // packetGeneration
                0, // width
                0, // height
                0, // inputCount
                0, // outputCount
                0, // sampleCount
                0, // historyAcceptedCount
                0, // historyRejectedCount
                0, // edgeRejectedCount
                0, // edgePreservedCount
                0, // rawGiPixels
                0, // rawGiSamples
                0, // rawGiRays
                0, // rawGiCacheReads
                0, // denoisedOutputPixels
                0L, // denoisedOutputChecksum
                0, // denoisedOutputChangedPixels
                0, // denoisedOutputMeanAbsDelta
                0L, // previousDenoisedOutputChecksum
                0L, // currentDenoisedOutputChecksum
                0, // frameToFrameChangedPixels
                0, // frameToFrameMeanAbsDelta
                0, // temporalStablePixels
                0, // temporalUnstablePixels
                0.0D, // temporalHistoryConfidence
                0.0D, // temporalFlickerScore
                0, // shaderDenoiseOutputImageCandidateWidth
                0, // shaderDenoiseOutputImageCandidateHeight
                0, // shaderDenoiseOutputImageCandidatePixels
                0L, // shaderDenoiseOutputImageCandidateBytes
                0L, // shaderDenoiseOutputImageCandidateChecksum
                0, // shaderGeneratedDenoiseOutputWidth
                0, // shaderGeneratedDenoiseOutputHeight
                0L, // shaderGeneratedDenoiseOutputTexelCount
                0L, // shaderGeneratedDenoiseOutputSampleCount
                0L, // shaderGeneratedDenoiseOutputChecksum
                0, // compositeWidth
                0, // compositeHeight
                0, // compositeOutputCount
                false, // enabled
                false, // validated
                false, // placeholder
                false, // temporalHistory
                false, // edgeInputsAvailable
                false, // directShadowSignalAvailable
                false, // diffuseGiSignalAvailable
                false, // optionalSpecularPlaceholder
                false, // optionalAoPlaceholder
                false, // rawGiInputAvailable
                false, // rawDirectInputAvailable
                false, // shaderDenoiseRawDiffuseGiInputReady
                false, // shaderDenoiseDirectLightValidationInputReady
                false, // shaderDenoiseDirectLightValidationInputActive
                false, // shaderDenoisePhysicalGiInputEvidence
                false, // shaderDenoiseRealTracedInputReady
                false, // denoisedOutputIntent
                false, // denoisedCpuOutputGenerated
                false, // denoisedOutputDiffersFromRaw
                false, // realDenoiseShaderOutput
                false, // rawGiInputReady
                false, // cpuDenoisedReadbackReady
                true, // shaderDenoiseDispatchPrepared
                true, // shaderDenoiseInputReady
                false, // publicMojangShaderVisualOutputAttempted
                false, // publicMojangShaderVisualOutputSubmitted
                false, // publicMojangShaderVisualOutputReady
                false, // shaderDenoiseOutputReady
                false, // shaderDenoiseOutputImageReady
                false, // shaderDenoiseOutputImageOwnedByShaderPass
                false, // shaderDenoiseOutputStorageWritable
                false, // shaderDenoiseOutputBarrierReady
                false, // shaderDenoiseOutputFinalCompositeConsumable
                false, // shaderDenoiseOutputImageCandidateReady
                false, // shaderDenoiseOutputImageCandidateCpuStaged
                true, // shaderDenoiseOutputImageCandidateNonGpu
                false, // shaderDenoiseOutputMaterialReady
                false, // shaderDenoiseShaderGeneratedOutput
                false, // shaderGeneratedDenoiseOutputEvidence
                false, // shaderGeneratedDenoiseOutputReported
                false, // shaderGeneratedDenoiseOutputImageReady
                false, // shaderGeneratedDenoiseOutputGenerated
                false, // shaderGeneratedDenoiseOutputFinalCompositeConsumed
                false, // shaderGeneratedDenoiseOutputReportReady
                false, // shaderDenoiseCpuReadbackFallbackReported
                false, // temporalReady
                false, // temporalGhostingRisk
                false, // cpuFallbackQualityMetrics
                false, // compositeStageRecorded
                false, // compositeEnabled
                false, // compositeReady
                false, // compositePlaceholder
                false, // edgeDepthAvailable
                false, // edgeNormalAvailable
                false, // edgeMaterialAvailable
                false, // historyConfidenceAvailable
                false, // ready
                false, // accepted
                "unknown", // outputMarker
                "unknown", // rawInputMarker
                "unknown", // shaderDenoiseInputKind
                "unknown", // shaderDenoiseInputStatus
                "unknown", // shaderDenoiseInputBlocker
                "unknown", // shaderDenoisePhysicalGiBlocker
                "unknown", // shaderDenoiseTracingBlocker
                "unknown", // denoisedOutputMarker
                "unknown", // temporalReadinessMarker
                "unknown", // temporalGhostingRiskMarker
                "unknown", // shaderDenoiseOutputImageCandidateMarker
                "unknown", // shaderDenoiseOutputImageBlocker
                "unknown", // shaderGeneratedDenoiseOutputMarker
                "unknown", // shaderGeneratedDenoiseOutputIdentity
                reason, // shaderGeneratedDenoiseOutputBlocker
                "unknown", // compositeMarker
                reason
        );
    }

    public static DenoiseExecutionSnapshot fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status unavailable");
        }

        String denoiseExecution = extractBlock(nativeStatus, "denoise_execution={");
        if (denoiseExecution.isBlank()) {
            return unavailableStatus(true, false, "native denoise execution status unavailable");
        }

        return new DenoiseExecutionSnapshot(
                true,
                true,
                parseLong(extractField(denoiseExecution, "dispatch_generation")),
                parseLong(extractField(denoiseExecution, "packet_generation")),
                dimensionComponentInt(extractField(denoiseExecution, "size"), 0),
                dimensionComponentInt(extractField(denoiseExecution, "size"), 1),
                parseInt(extractField(denoiseExecution, "inputs")),
                parseInt(extractField(denoiseExecution, "outputs")),
                parseInt(extractField(denoiseExecution, "samples")),
                parseInt(extractField(denoiseExecution, "history_accepted")),
                parseInt(extractField(denoiseExecution, "history_rejected")),
                parseInt(extractField(denoiseExecution, "edge_rejected")),
                parseInt(extractField(denoiseExecution, "edge_preserved")),
                parseInt(extractField(denoiseExecution, "raw_gi_pixels")),
                parseInt(extractField(denoiseExecution, "raw_gi_samples")),
                parseInt(extractField(denoiseExecution, "raw_gi_rays")),
                parseInt(extractField(denoiseExecution, "raw_gi_cache_reads")),
                parseInt(extractField(denoiseExecution, "denoised_output_pixels")),
                parseLong(extractField(denoiseExecution, "denoised_output_checksum")),
                parseInt(extractField(denoiseExecution, "denoised_output_changed_pixels")),
                parseInt(extractField(denoiseExecution, "denoised_output_mean_abs_delta")),
                parseLongField(
                        denoiseExecution,
                        "previous_denoised_output_checksum",
                        "denoised_output_previous_checksum"
                ),
                parseLongField(
                        denoiseExecution,
                        "current_denoised_output_checksum",
                        "denoised_output_current_checksum",
                        "denoised_output_checksum"
                ),
                parseIntField(
                        denoiseExecution,
                        "frame_to_frame_changed_pixels",
                        "denoised_frame_to_frame_changed_pixels",
                        "temporal_changed_pixels",
                        "temporal_unstable_pixels"
                ),
                parseIntField(
                        denoiseExecution,
                        "frame_to_frame_mean_abs_delta",
                        "denoised_frame_to_frame_mean_abs_delta",
                        "temporal_mean_abs_delta"
                ),
                parseInt(extractField(denoiseExecution, "temporal_stable_pixels")),
                parseInt(extractField(denoiseExecution, "temporal_unstable_pixels")),
                parseDoubleField(
                        denoiseExecution,
                        "history_confidence",
                        "temporal_history_confidence",
                        "denoise_history_confidence"
                ),
                parseDoubleField(
                        denoiseExecution,
                        "flicker_score",
                        "temporal_flicker_score",
                        "denoise_flicker_score"
                ),
                dimensionOrFieldInt(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_size",
                        "shader_denoise_output_image_candidate_width",
                        0
                ),
                dimensionOrFieldInt(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_size",
                        "shader_denoise_output_image_candidate_height",
                        1
                ),
                parseInt(extractField(denoiseExecution, "shader_denoise_output_image_candidate_pixels")),
                parseLong(extractField(denoiseExecution, "shader_denoise_output_image_candidate_bytes")),
                parseLong(extractField(denoiseExecution, "shader_denoise_output_image_candidate_checksum")),
                dimensionOrFieldInt(
                        denoiseExecution,
                        "shader_generated_denoise_output_size",
                        "shader_generated_denoise_output_width",
                        0
                ),
                dimensionOrFieldInt(
                        denoiseExecution,
                        "shader_generated_denoise_output_size",
                        "shader_generated_denoise_output_height",
                        1
                ),
                parseLongField(
                        denoiseExecution,
                        "shader_generated_denoise_output_texel_count",
                        "shader_generated_denoise_output_pixels",
                        "shader_generated_denoise_output_texels"
                ),
                parseLongField(
                        denoiseExecution,
                        "shader_generated_denoise_output_sample_count",
                        "shader_generated_denoise_output_samples"
                ),
                parseLong(extractField(denoiseExecution, "shader_generated_denoise_output_checksum")),
                dimensionComponentInt(extractField(denoiseExecution, "composite_size"), 0),
                dimensionComponentInt(extractField(denoiseExecution, "composite_size"), 1),
                parseInt(extractField(denoiseExecution, "composite_outputs")),
                parseBoolean(extractField(denoiseExecution, "enabled")),
                parseBoolean(extractField(denoiseExecution, "validated")),
                parseBoolean(extractField(denoiseExecution, "placeholder")),
                parseBoolean(extractField(denoiseExecution, "temporal_history")),
                parseBoolean(extractField(denoiseExecution, "edge_inputs_available")),
                parseBoolean(extractField(denoiseExecution, "direct_shadow_signal_available")),
                parseBoolean(extractField(denoiseExecution, "diffuse_gi_signal_available")),
                parseBoolean(extractField(denoiseExecution, "optional_specular_placeholder")),
                parseBoolean(extractField(denoiseExecution, "optional_ao_placeholder")),
                parseBoolean(extractField(denoiseExecution, "raw_gi_input_available")),
                parseBoolean(extractField(denoiseExecution, "raw_direct_input_available")),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_raw_diffuse_gi_input_ready",
                        "shader_denoise_raw_gi_input_ready",
                        "raw_diffuse_gi_shader_denoise_input_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_direct_light_validation_input_ready",
                        "direct_light_validation_shader_denoise_input_ready",
                        "shader_denoise_validation_direct_input_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_direct_light_validation_input_active",
                        "shader_denoise_uses_direct_light_validation_input",
                        "direct_light_validation_shader_denoise_input_active"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_physical_gi_input_evidence",
                        "physical_gi_evidence",
                        "shader_denoise_physical_gi_evidence"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_real_traced_input_ready",
                        "real_traced_lighting_input_ready",
                        "real_traced_lighting_consumed"
                ),
                parseBoolean(extractField(denoiseExecution, "denoised_output_intent")),
                parseBoolean(extractField(denoiseExecution, "denoised_cpu_output_generated")),
                parseBoolean(extractField(denoiseExecution, "denoised_output_differs_from_raw")),
                parseBooleanField(
                        denoiseExecution,
                        "real_denoise_shader_output",
                        "real_shader_denoise_output",
                        "real_shader_denoise_output_proven"
                ),
                parseBoolean(extractField(denoiseExecution, "raw_gi_input_ready")),
                parseBoolean(extractField(denoiseExecution, "cpu_denoised_readback_ready")),
                parseBoolean(extractField(denoiseExecution, "shader_denoise_dispatch_prepared")),
                parseBoolean(extractField(denoiseExecution, "shader_denoise_input_ready")),
                parseBooleanField(
                        denoiseExecution,
                        "public_mojang_shader_visual_output_attempted",
                        "public_mojang_shader_output_attempted",
                        "public_mojang_visual_output_attempted",
                        "public_mojang_denoise_visual_output_attempted",
                        "shader_visual_output_attempted"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "public_mojang_shader_visual_output_submitted",
                        "public_mojang_shader_output_submitted",
                        "public_mojang_visual_output_submitted",
                        "public_mojang_denoise_visual_output_submitted",
                        "shader_visual_output_submitted"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "public_mojang_shader_visual_output_ready",
                        "public_mojang_shader_output_ready",
                        "public_mojang_visual_output_ready",
                        "public_mojang_denoise_visual_output_ready",
                        "shader_visual_output_ready"
                ),
                parseBoolean(extractField(denoiseExecution, "shader_denoise_output_ready")),
                parseBoolean(extractField(denoiseExecution, "shader_denoise_output_image_ready")),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_image_owned_by_shader_pass",
                        "shader_output_image_owned_by_shader_pass",
                        "real_shader_output_image_owned",
                        "shader_denoise_output_image_owned"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_storage_writable",
                        "shader_output_storage_writable",
                        "shader_denoise_storage_image_writable",
                        "shader_denoise_output_writable"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_barrier_ready",
                        "shader_output_barrier_ready",
                        "shader_denoise_output_layout_transition_ready",
                        "shader_denoise_output_memory_barrier_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_final_composite_consumable",
                        "shader_output_final_composite_consumable",
                        "real_shader_output_consumed_by_final_composite",
                        "shader_denoise_output_composite_consumable"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_ready",
                        "shader_output_image_candidate_ready",
                        "shader_output_image_candidate_present"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_cpu_staged",
                        "shader_output_image_candidate_cpu_staged"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_non_gpu",
                        "shader_output_image_candidate_non_gpu"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_material_ready",
                        "shader_output_material_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_output_shader_generated",
                        "shader_generated_output",
                        "shader_denoise_generated_output",
                        "real_shader_generated_output",
                        "real_shader_denoise_generated_output",
                        "real_denoise_shader_generated_output",
                        "gpu_denoise_generated_output"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_evidence",
                        "shader_denoise_generated_output_evidence",
                        "real_shader_denoise_output_evidence",
                        "shader_denoise_output_evidence",
                        "shader_generated_denoise_output_proven"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_reported",
                        "shader_generated_denoise_output_report"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_image_ready",
                        "shader_generated_output_image_ready",
                        "real_shader_generated_output_image_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_generated",
                        "shader_generated_denoise_output_true",
                        "real_shader_generated_output"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_final_composite_consumed",
                        "shader_generated_output_final_composite_consumed",
                        "real_shader_generated_output_consumed_by_final_composite"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_generated_denoise_output_report_ready",
                        "shader_generated_denoise_output_evidence_ready",
                        "shader_generated_denoise_output_evidence"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "shader_denoise_cpu_readback_fallback",
                        "cpu_readback_fallback",
                        "cpu_readback_fallback_used",
                        "denoise_cpu_readback_fallback"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "temporal_ready",
                        "temporal_history_ready",
                        "temporal_proof_ready",
                        "temporal_acceptance_ready"
                ),
                parseBooleanField(
                        denoiseExecution,
                        "ghosting_risk",
                        "temporal_ghosting_risk",
                        "denoise_ghosting_risk"
                ),
                parseBoolean(extractField(denoiseExecution, "cpu_fallback_quality_metrics")),
                parseBoolean(extractField(denoiseExecution, "composite_stage_recorded")),
                parseBoolean(extractField(denoiseExecution, "composite_enabled")),
                parseBoolean(extractField(denoiseExecution, "composite_ready")),
                parseBoolean(extractField(denoiseExecution, "composite_placeholder")),
                parseBoolean(extractField(denoiseExecution, "edge_depth_available")),
                parseBoolean(extractField(denoiseExecution, "edge_normal_available")),
                parseBoolean(extractField(denoiseExecution, "edge_material_available")),
                parseBoolean(extractField(denoiseExecution, "history_confidence_available")),
                parseBoolean(extractField(denoiseExecution, "ready")),
                parseBoolean(extractField(denoiseExecution, "accepted_this_dispatch")),
                extractField(denoiseExecution, "output_marker"),
                extractField(denoiseExecution, "raw_input_marker"),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_input_kind",
                        "shaderDenoiseInputKind",
                        "shader_denoise_source_input_kind"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_input_status",
                        "shaderDenoiseInputStatus",
                        "shader_denoise_source_input_status"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_input_blocker",
                        "shaderDenoiseInputBlocker",
                        "shader_denoise_source_input_blocker"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_physical_gi_blocker",
                        "physical_gi_blocker",
                        "shaderDenoisePhysicalGiBlocker"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_tracing_blocker",
                        "real_traced_lighting_blocker",
                        "shaderDenoiseTracingBlocker"
                ),
                extractField(denoiseExecution, "denoised_output_marker"),
                extractFieldOrDefault(
                        denoiseExecution,
                        "temporal_readiness_marker",
                        "temporal_marker",
                        "temporal_ready_marker"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "ghosting_risk_marker",
                        "temporal_ghosting_risk_marker",
                        "denoise_ghosting_risk_marker"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_output_image_candidate_marker",
                        "shader_output_image_candidate_marker",
                        "shader_output_image_candidate_source"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_denoise_output_image_blocker",
                        "shader_output_blocker_reason",
                        "shader_output_image_candidate_blocker",
                        "shader_denoise_output_blocker_reason"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_generated_denoise_output_marker",
                        "shader_denoise_generated_output_marker",
                        "real_shader_denoise_output_marker",
                        "shader_denoise_output_evidence_marker"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_generated_denoise_output_identity",
                        "shader_generated_output_identity",
                        "real_shader_generated_output_identity"
                ),
                extractFieldOrDefault(
                        denoiseExecution,
                        "shader_generated_denoise_output_blocker",
                        "shader_generated_output_blocker",
                        "real_shader_generated_output_blocker"
                ),
                extractField(denoiseExecution, "composite_marker"),
                extractField(denoiseExecution, "readiness_reason")
        );
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.denoiseExecutionAvailable;
    }

    public boolean hasHistoryCounters() {
        return this.historyAcceptedCount > 0 || this.historyRejectedCount > 0;
    }

    public boolean cpuDenoisedOutputReadbackReady() {
        return this.hasExecutionTelemetry()
                && (this.cpuDenoisedReadbackReady || (
                this.enabled
                        && this.accepted
                        && this.denoisedOutputIntent
                        && this.denoisedCpuOutputGenerated
                        && this.denoisedOutputPixels > 0
                        && this.denoisedOutputChecksum > 0L
        ));
    }

    public boolean denoiseQualityEvidenceReady() {
        return this.cpuDenoisedOutputReadbackReady()
                && this.denoisedOutputDiffersFromRaw
                && this.edgeInputsAvailable
                && (this.hasHistoryCounters() || this.edgePreservedCount > 0 || this.edgeRejectedCount > 0);
    }

    public boolean shaderDenoiseOutputImageCandidatePresent() {
        return this.hasExecutionTelemetry()
                && this.shaderDenoiseOutputImageCandidateReady
                && this.shaderDenoiseOutputImageCandidateWidth > 0
                && this.shaderDenoiseOutputImageCandidateHeight > 0
                && this.shaderDenoiseOutputImageCandidatePixels > 0
                && this.shaderDenoiseOutputImageCandidateBytes > 0L
                && this.shaderDenoiseOutputImageCandidateChecksum > 0L;
    }

    public boolean shaderDenoiseCpuReadbackFallbackActive() {
        return this.shaderDenoiseCpuReadbackFallbackReported || this.shaderDenoiseCpuReadbackFallbackDerived();
    }

    public boolean shaderDenoiseCpuReadbackFallbackDerived() {
        return this.hasExecutionTelemetry()
                && this.cpuDenoisedOutputReadbackReady()
                && !this.realShaderDenoiseOutputReady();
    }

    public boolean shaderDenoiseNoOverclaimStatus() {
        return !this.realShaderDenoiseOutputReady();
    }

    public boolean publicMojangShaderVisualOutputActive() {
        return this.publicMojangShaderVisualOutputAttempted
                || this.publicMojangShaderVisualOutputSubmitted
                || this.publicMojangShaderVisualOutputReady;
    }

    public String publicMojangShaderVisualOutputBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (!this.publicMojangShaderVisualOutputActive()) {
            return "public Mojang shader visual output not reported";
        }
        if (this.publicMojangShaderVisualOutputReady) {
            return "public Mojang shader visual output ready; not real compute/native shader denoise output";
        }
        if (this.publicMojangShaderVisualOutputSubmitted) {
            return "public Mojang shader visual output submitted; readiness not proven";
        }
        return "public Mojang shader visual output attempted; submission/readiness not proven";
    }

    public boolean shaderDenoiseUsesRawDiffuseGiInput() {
        return this.shaderDenoiseRawDiffuseGiInputReady
                && "raw-diffuse-gi-rgba8".equals(this.shaderDenoiseInputKind);
    }

    public boolean shaderDenoiseUsesDirectLightValidationInput() {
        return this.shaderDenoiseDirectLightValidationInputActive
                || "native-direct-light-rgba8-validation-input".equals(this.shaderDenoiseInputKind);
    }

    public String shaderDenoiseInputBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (this.shaderDenoiseUsesRawDiffuseGiInput()) {
            return "raw-diffuse-gi-rgba8 input ready; physical GI/tracing still requires separate proof";
        }
        if (this.shaderDenoiseUsesDirectLightValidationInput()) {
            return "direct-light validation RGBA8 input active; raw diffuse GI, physical GI, and tracing remain unproven";
        }
        if (this.rawGiInputAvailable || this.diffuseGiSignalAvailable) {
            return "diffuse GI metadata present but raw diffuse-GI RGBA8 input is not ready";
        }
        return this.shaderDenoiseInputBlocker;
    }

    public boolean shaderDenoiseInputPrerequisitesReady() {
        return this.hasExecutionTelemetry()
                && this.shaderDenoiseInputReady
                && this.rawGiInputReady
                && this.edgeInputsAvailable
                && (this.rawGiInputAvailable || this.diffuseGiSignalAvailable);
    }

    public boolean shaderDenoiseOutputImageReadinessReady() {
        return this.hasExecutionTelemetry()
                && this.shaderDenoiseOutputReady
                && this.shaderDenoiseOutputImageReady
                && this.shaderDenoiseOutputImageOwnedByShaderPass
                && this.shaderDenoiseOutputStorageWritable
                && this.shaderDenoiseOutputBarrierReady
                && this.shaderDenoiseOutputFinalCompositeConsumable
                && this.shaderDenoiseOutputMaterialReady
                && !this.shaderDenoiseOutputImageCandidatePresent()
                && !this.shaderDenoiseOutputImageCandidateCpuStaged
                && !this.shaderDenoiseOutputImageCandidateNonGpu;
    }

    public boolean shaderDenoiseTemporalHistoryReady() {
        return this.hasExecutionTelemetry()
                && this.temporalReady
                && this.temporalHistory
                && this.hasHistoryCounters()
                && this.historyConfidenceAvailable
                && !this.temporalGhostingRisk;
    }

    public boolean shaderDenoiseRealOutputPrerequisitesReady() {
        return this.hasExecutionTelemetry()
                && this.shaderDenoiseDispatchPrepared
                && this.shaderDenoiseInputPrerequisitesReady()
                && this.shaderDenoiseOutputImageReadinessReady()
                && this.shaderDenoiseTemporalHistoryReady()
                && !this.publicMojangShaderVisualOutputReady
                && !this.shaderDenoiseCpuReadbackFallbackReported;
    }

    public boolean cpuReadbackGuidedVisualDenoiseActive() {
        return this.shaderDenoiseCpuReadbackFallbackActive()
                || this.publicMojangShaderVisualOutputReady
                || this.shaderDenoiseOutputImageCandidatePresent()
                || this.shaderDenoiseOutputImageCandidateCpuStaged
                || this.shaderDenoiseOutputImageCandidateNonGpu;
    }

    public String shaderDenoiseRealOutputBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        return "realShaderDenoiseOutputBoundary"
                + " inputPrerequisitesReady=" + this.shaderDenoiseInputPrerequisitesReady()
                + " inputKind=" + this.shaderDenoiseInputKind
                + " inputStatus=" + this.shaderDenoiseInputStatus
                + " inputBoundary=\"" + this.shaderDenoiseInputBoundary() + "\""
                + " rawDiffuseGiInputReady=" + this.shaderDenoiseRawDiffuseGiInputReady
                + " directLightValidationInputActive=" + this.shaderDenoiseDirectLightValidationInputActive
                + " physicalGiEvidence=" + this.shaderDenoisePhysicalGiInputEvidence
                + " realTracedInputReady=" + this.shaderDenoiseRealTracedInputReady
                + " outputImageReadinessReady=" + this.shaderDenoiseOutputImageReadinessReady()
                + " temporalHistoryReady=" + this.shaderDenoiseTemporalHistoryReady()
                + " realOutputPrerequisitesReady=" + this.shaderDenoiseRealOutputPrerequisitesReady()
                + " cpuReadbackGuidedVisualActive=" + this.cpuReadbackGuidedVisualDenoiseActive()
                + " realShaderDenoiseOutputReady=" + this.realShaderDenoiseOutputReady()
                + " shaderGeneratedDenoiseOutputEvidence=" + this.shaderGeneratedDenoiseOutputEvidence
                + " shaderGeneratedDenoiseOutputEvidenceReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " shaderGeneratedDenoiseOutputReportReady=" + this.shaderGeneratedDenoiseOutputReportReady()
                + " shaderGeneratedDenoiseOutputFinalCompositeConsumed="
                + this.shaderGeneratedDenoiseOutputFinalCompositeConsumed
                + " shaderGeneratedDenoiseOutputMarker=" + this.shaderGeneratedDenoiseOutputMarker
                + " blocker=" + this.shaderDenoiseOutputBlockerReason();
    }

    public String shaderDenoiseOutputPrerequisitesSummary() {
        return "shaderDenoiseOutputPrerequisites"
                + " dispatchPrepared=" + this.shaderDenoiseDispatchPrepared
                + " inputReady=" + this.shaderDenoiseInputReady
                + " inputPrerequisitesReady=" + this.shaderDenoiseInputPrerequisitesReady()
                + " rawGiInputReady=" + this.rawGiInputReady
                + " rawGiInputAvailable=" + this.rawGiInputAvailable
                + " inputKind=" + this.shaderDenoiseInputKind
                + " rawDiffuseGiInputReady=" + this.shaderDenoiseRawDiffuseGiInputReady
                + " directLightValidationInputReady=" + this.shaderDenoiseDirectLightValidationInputReady
                + " directLightValidationInputActive=" + this.shaderDenoiseDirectLightValidationInputActive
                + " physicalGiEvidence=" + this.shaderDenoisePhysicalGiInputEvidence
                + " physicalGiBlocker=" + this.shaderDenoisePhysicalGiBlocker
                + " realTracedInputReady=" + this.shaderDenoiseRealTracedInputReady
                + " tracingBlocker=" + this.shaderDenoiseTracingBlocker
                + " inputBoundary=\"" + this.shaderDenoiseInputBoundary() + "\""
                + " edgeInputsAvailable=" + this.edgeInputsAvailable
                + " diffuseGiSignalAvailable=" + this.diffuseGiSignalAvailable
                + " publicMojangVisualAttempted=" + this.publicMojangShaderVisualOutputAttempted
                + " publicMojangVisualSubmitted=" + this.publicMojangShaderVisualOutputSubmitted
                + " publicMojangVisualReady=" + this.publicMojangShaderVisualOutputReady
                + " outputReady=" + this.shaderDenoiseOutputReady
                + " imageReady=" + this.shaderDenoiseOutputImageReady
                + " imageOwnedByShaderPass=" + this.shaderDenoiseOutputImageOwnedByShaderPass
                + " storageWritable=" + this.shaderDenoiseOutputStorageWritable
                + " barrierReady=" + this.shaderDenoiseOutputBarrierReady
                + " finalCompositeConsumable=" + this.shaderDenoiseOutputFinalCompositeConsumable
                + " materialReady=" + this.shaderDenoiseOutputMaterialReady
                + " outputImageReadinessReady=" + this.shaderDenoiseOutputImageReadinessReady()
                + " temporalReady=" + this.temporalReady
                + " temporalHistory=" + this.temporalHistory
                + " historyCounters=" + this.hasHistoryCounters()
                + " historyConfidenceAvailable=" + this.historyConfidenceAvailable
                + " temporalGhostingRisk=" + this.temporalGhostingRisk
                + " temporalHistoryReady=" + this.shaderDenoiseTemporalHistoryReady()
                + " shaderGenerated=" + this.shaderDenoiseShaderGeneratedOutput
                + " shaderGeneratedDenoiseOutputEvidence=" + this.shaderGeneratedDenoiseOutputEvidence
                + " shaderGeneratedDenoiseOutputEvidenceReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " shaderGeneratedDenoiseOutputReportReady=" + this.shaderGeneratedDenoiseOutputReportReady()
                + " shaderGeneratedDenoiseOutputImageReady=" + this.shaderGeneratedDenoiseOutputImageReady
                + " shaderGeneratedDenoiseOutputGenerated=" + this.shaderGeneratedDenoiseOutputGenerated
                + " shaderGeneratedDenoiseOutputFinalCompositeConsumed="
                + this.shaderGeneratedDenoiseOutputFinalCompositeConsumed
                + " shaderGeneratedDenoiseOutputMarker=" + this.shaderGeneratedDenoiseOutputMarker
                + " realShaderFlag=" + this.realDenoiseShaderOutput
                + " realOutputPrerequisitesReady=" + this.shaderDenoiseRealOutputPrerequisitesReady()
                + " realOutputPathReady=" + this.shaderDenoiseRealOutputPathReady()
                + " realShaderDenoiseOutputReady=" + this.realShaderDenoiseOutputReady()
                + " cpuReadbackFallback=" + this.shaderDenoiseCpuReadbackFallbackActive()
                + " cpuReadbackFallbackReported=" + this.shaderDenoiseCpuReadbackFallbackReported
                + " cpuReadbackFallbackDerived=" + this.shaderDenoiseCpuReadbackFallbackDerived()
                + " cpuReadbackGuidedVisualActive=" + this.cpuReadbackGuidedVisualDenoiseActive()
                + " candidateOnly=" + this.shaderDenoiseOutputImageCandidatePresent()
                + " candidateCpuStaged=" + this.shaderDenoiseOutputImageCandidateCpuStaged
                + " candidateNonGpu=" + this.shaderDenoiseOutputImageCandidateNonGpu
                + " blocker=" + this.shaderDenoiseOutputBlockerReason()
                + " noOverclaim=" + this.shaderDenoiseNoOverclaimStatus();
    }

    public boolean realShaderDenoiseOutputReady() {
        return this.shaderDenoiseRealOutputPathReady()
                && this.realDenoiseShaderOutput
                && this.shaderDenoiseShaderGeneratedOutput
                && this.shaderGeneratedDenoiseOutputReportReady();
    }

    public boolean shaderGeneratedDenoiseOutputEvidenceReady() {
        return this.shaderGeneratedDenoiseOutputEvidence
                && this.shaderGeneratedDenoiseOutputReportReady();
    }

    public boolean shaderGeneratedDenoiseOutputReportReady() {
        if (!this.hasExecutionTelemetry()) {
            return false;
        }
        long expectedTexels = (long) this.shaderGeneratedDenoiseOutputWidth
                * (long) this.shaderGeneratedDenoiseOutputHeight;
        return this.shaderGeneratedDenoiseOutputReported
                && this.shaderGeneratedDenoiseOutputReportReady
                && this.shaderGeneratedDenoiseOutputImageReady
                && this.shaderGeneratedDenoiseOutputGenerated
                && this.shaderGeneratedDenoiseOutputFinalCompositeConsumed
                && this.shaderGeneratedDenoiseOutputWidth > 0
                && this.shaderGeneratedDenoiseOutputHeight > 0
                && this.shaderGeneratedDenoiseOutputTexelCount > 0L
                && this.shaderGeneratedDenoiseOutputTexelCount == expectedTexels
                && this.shaderGeneratedDenoiseOutputSampleCount > 0L
                && this.shaderGeneratedDenoiseOutputChecksum > 0L
                && !"unknown".equals(this.shaderGeneratedDenoiseOutputIdentity)
                && !"shader_generated_denoise_output_identity_missing".equals(this.shaderGeneratedDenoiseOutputIdentity);
    }

    public boolean shaderDenoiseRealOutputPathReady() {
        return this.shaderDenoiseRealOutputPrerequisitesReady();
    }

    public String shaderDenoiseOutputBlockerReason() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (this.realShaderDenoiseOutputReady()) {
            return "none";
        }
        if (!this.shaderDenoiseDispatchPrepared) {
            return "shader-denoise-dispatch-not-prepared";
        }
        if (!this.shaderDenoiseInputReady) {
            return "shader-denoise-input-not-ready";
        }
        if (!this.shaderDenoiseUsesRawDiffuseGiInput() && this.shaderDenoiseUsesDirectLightValidationInput()) {
            return "shader-denoise-input-is-direct-light-validation-not-raw-diffuse-gi";
        }
        if (!this.rawGiInputReady || (!this.rawGiInputAvailable && !this.diffuseGiSignalAvailable)) {
            return "shader-denoise-raw-gi-input-not-ready";
        }
        if (!this.edgeInputsAvailable) {
            return "shader-denoise-edge-inputs-not-ready";
        }
        if (!this.shaderDenoiseOutputReady) {
            return "shader-denoise-output-readiness-not-reported";
        }
        if (!this.shaderDenoiseOutputImageReady) {
            return this.normalizedShaderDenoiseOutputImageBlocker();
        }
        if (!this.shaderDenoiseOutputImageOwnedByShaderPass) {
            return "shader-denoise-output-image-not-owned-by-shader-pass";
        }
        if (!this.shaderDenoiseOutputStorageWritable) {
            return "shader-denoise-output-storage-not-writable";
        }
        if (!this.shaderDenoiseOutputBarrierReady) {
            return "shader-denoise-output-barrier-not-ready";
        }
        if (!this.shaderDenoiseOutputFinalCompositeConsumable) {
            return "shader-denoise-output-not-consumable-by-final-composite";
        }
        if (!this.shaderDenoiseOutputMaterialReady) {
            return "shader-denoise-output-material-handoff-not-ready";
        }
        if (this.shaderDenoiseOutputImageCandidateCpuStaged || this.shaderDenoiseOutputImageCandidateNonGpu) {
            return "shader-denoise-output-candidate-is-cpu-staged-or-non-gpu";
        }
        if (this.shaderDenoiseOutputImageCandidatePresent()) {
            return "shader-denoise-output-candidate-is-boundary-only";
        }
        if (!this.temporalReady) {
            return "shader-denoise-temporal-history-not-ready";
        }
        if (!this.temporalHistory) {
            return "shader-denoise-temporal-history-inputs-not-bound";
        }
        if (!this.hasHistoryCounters()) {
            return "shader-denoise-history-counters-not-ready";
        }
        if (!this.historyConfidenceAvailable) {
            return "shader-denoise-history-confidence-not-ready";
        }
        if (this.temporalGhostingRisk) {
            return "shader-denoise-temporal-ghosting-risk";
        }
        if (this.publicMojangShaderVisualOutputReady) {
            return "public-mojang-visual-output-is-not-real-shader-denoise-output";
        }
        if (this.shaderDenoiseCpuReadbackFallbackReported) {
            return "cpu-readback-guided-visual-denoise-active";
        }
        if (!this.shaderDenoiseShaderGeneratedOutput) {
            return "shader-denoise-output-is-not-shader-generated";
        }
        if (!this.realDenoiseShaderOutput) {
            return "real-denoise-shader-output-flag-false";
        }
        if (!this.shaderGeneratedDenoiseOutputReportReady()) {
            return this.normalizedShaderGeneratedDenoiseOutputBlocker();
        }
        return this.normalizedShaderDenoiseOutputImageBlocker();
    }

    public String shaderDenoiseOutputReadinessLabel() {
        if (this.realShaderDenoiseOutputReady()) {
            return "real-shader-output-ready";
        }
        if (this.shaderGeneratedDenoiseOutputReportReady()) {
            return "shader-generated-output-report-ready";
        }
        if (this.shaderDenoiseRealOutputPrerequisitesReady()) {
            return "real-shader-output-prerequisites-ready";
        }
        if (this.shaderDenoiseOutputImageReadinessReady() && !this.shaderDenoiseTemporalHistoryReady()) {
            return "shader-output-image-ready-temporal-history-blocked";
        }
        if (this.shaderDenoiseCpuReadbackFallbackActive()) {
            return "cpu-readback-fallback-active";
        }
        if (this.shaderDenoiseOutputImageCandidatePresent()) {
            return "candidate-image-only";
        }
        if (this.publicMojangShaderVisualOutputReady) {
            return "public-mojang-visual-ready-noncompute";
        }
        if (this.publicMojangShaderVisualOutputSubmitted) {
            return "public-mojang-visual-submitted";
        }
        if (this.publicMojangShaderVisualOutputAttempted) {
            return "public-mojang-visual-attempted";
        }
        if (this.shaderDenoiseDispatchPrepared || this.shaderDenoiseInputReady) {
            return "shader-output-blocked";
        }
        return "shader-output-unavailable";
    }

    private String normalizedShaderDenoiseOutputImageBlocker() {
        if (this.shaderDenoiseOutputImageBlocker == null
                || this.shaderDenoiseOutputImageBlocker.isBlank()
                || "unknown".equals(this.shaderDenoiseOutputImageBlocker)) {
            return "shader-denoise-output-image-not-ready";
        }
        return this.shaderDenoiseOutputImageBlocker;
    }

    private String normalizedShaderGeneratedDenoiseOutputBlocker() {
        if (this.shaderGeneratedDenoiseOutputBlocker == null
                || this.shaderGeneratedDenoiseOutputBlocker.isBlank()
                || "unknown".equals(this.shaderGeneratedDenoiseOutputBlocker)) {
            return "shader-generated-denoise-output-report-not-ready";
        }
        return this.shaderGeneratedDenoiseOutputBlocker;
    }

    public String shaderDenoiseOutputImageCandidateBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (!this.shaderDenoiseOutputImageCandidateReady) {
            return this.shaderDenoiseOutputImageBlocker;
        }
        if (this.shaderDenoiseOutputImageCandidateNonGpu || this.shaderDenoiseOutputImageCandidateCpuStaged) {
            return "candidate image is CPU-staged/non-GPU and is not real shader-generated output";
        }
        if (!this.shaderDenoiseShaderGeneratedOutput || !this.realDenoiseShaderOutput) {
            return "candidate image exists, but real shader-generated output remains false";
        }
        return "candidate image is real shader-generated denoise output";
    }

    public String denoiseReadinessBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (this.realShaderDenoiseOutputReady()) {
            return this.denoiseQualityEvidenceReady()
                    ? "real-shader-denoise-output-with-quality-evidence"
                    : "real-shader-denoise-output-without-quality-evidence";
        }
        if (this.shaderGeneratedDenoiseOutputReportReady()) {
            return "shader-generated-denoise-output-report-ready; broader-real-output-prerequisites-still-open";
        }
        if (this.shaderDenoiseRealOutputPrerequisitesReady()) {
            return "real-shader-output-prerequisites-ready; shader-generated-pixels-or-controller-proof-missing";
        }
        if (this.shaderDenoiseOutputImageReadinessReady() && !this.shaderDenoiseTemporalHistoryReady()) {
            return "shader-output-image-ready; temporal-history-readiness-missing; real-shader-output=false";
        }
        if (!this.cpuDenoisedOutputReadbackReady()) {
            return "not-ready:missing-accepted-cpu-output-readback";
        }
        if (this.denoiseQualityEvidenceReady()) {
            return "cpu-output-readback-ready; guided-visual-denoise-only; quality-evidence-present; real-shader-output=false";
        }
        return "cpu-output-readback-ready; guided-visual-denoise-only; denoise-quality-not-proven; real-shader-output=false";
    }

    public String debugSummary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        return "denoiseExecution ready=" + this.ready
                + " accepted=" + this.accepted
                + " size=" + this.width + "x" + this.height
                + " inputs=" + this.inputCount
                + " outputs=" + this.outputCount
                + " samples=" + this.sampleCount
                + " edgeInputs=" + this.edgeInputsAvailable
                + " diffuseGiSignal=" + this.diffuseGiSignalAvailable
                + " directShadowSignal=" + this.directShadowSignalAvailable
                + " historyAccepted=" + this.historyAcceptedCount
                + " historyRejected=" + this.historyRejectedCount
                + " edgePreserved=" + this.edgePreservedCount
                + " edgeRejected=" + this.edgeRejectedCount
                + " rawGi=" + this.rawGiInputAvailable
                + " rawGiPixels=" + this.rawGiPixels
                + " rawGiSamples=" + this.rawGiSamples
                + " rawGiRays=" + this.rawGiRays
                + " rawGiCacheReads=" + this.rawGiCacheReads
                + " shaderDenoiseInputKind=" + this.shaderDenoiseInputKind
                + " shaderDenoiseInputStatus=" + this.shaderDenoiseInputStatus
                + " shaderDenoiseInputBlocker=" + this.shaderDenoiseInputBlocker
                + " shaderDenoiseRawDiffuseGiInputReady=" + this.shaderDenoiseRawDiffuseGiInputReady
                + " shaderDenoiseDirectLightValidationInputReady="
                + this.shaderDenoiseDirectLightValidationInputReady
                + " shaderDenoiseDirectLightValidationInputActive="
                + this.shaderDenoiseDirectLightValidationInputActive
                + " physicalGiEvidence=" + this.shaderDenoisePhysicalGiInputEvidence
                + " shaderDenoisePhysicalGiBlocker=" + this.shaderDenoisePhysicalGiBlocker
                + " shaderDenoiseRealTracedInputReady=" + this.shaderDenoiseRealTracedInputReady
                + " shaderDenoiseTracingBlocker=" + this.shaderDenoiseTracingBlocker
                + " shaderDenoiseInputBoundary=\"" + this.shaderDenoiseInputBoundary() + "\""
                + " denoisedIntent=" + this.denoisedOutputIntent
                + " denoisedCpuOutputGenerated=" + this.denoisedCpuOutputGenerated
                + " denoisedOutputPixels=" + this.denoisedOutputPixels
                + " denoisedOutputChangedPixels=" + this.denoisedOutputChangedPixels
                + " denoisedOutputMeanAbsDelta=" + this.denoisedOutputMeanAbsDelta
                + " previousDenoisedOutputChecksum=" + this.previousDenoisedOutputChecksum
                + " currentDenoisedOutputChecksum=" + this.currentDenoisedOutputChecksum
                + " frameToFrameChangedPixels=" + this.frameToFrameChangedPixels
                + " frameToFrameMeanAbsDelta=" + this.frameToFrameMeanAbsDelta
                + " temporalStablePixels=" + this.temporalStablePixels
                + " temporalUnstablePixels=" + this.temporalUnstablePixels
                + " temporalHistoryConfidence=" + this.temporalHistoryConfidence
                + " temporalFlickerScore=" + this.temporalFlickerScore
                + " temporalReady=" + this.temporalReady
                + " temporalGhostingRisk=" + this.temporalGhostingRisk
                + " temporalReadinessMarker=" + this.temporalReadinessMarker
                + " temporalGhostingRiskMarker=" + this.temporalGhostingRiskMarker
                + " denoisedOutputDiffersFromRaw=" + this.denoisedOutputDiffersFromRaw
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " rawGiInputReady=" + this.rawGiInputReady
                + " shaderDenoiseInputPrerequisitesReady=" + this.shaderDenoiseInputPrerequisitesReady()
                + " cpuDenoisedReadbackReady=" + this.cpuDenoisedReadbackReady
                + " shaderDenoiseDispatchPrepared=" + this.shaderDenoiseDispatchPrepared
                + " shaderDenoiseInputReady=" + this.shaderDenoiseInputReady
                + " publicMojangShaderVisualOutputAttempted=" + this.publicMojangShaderVisualOutputAttempted
                + " publicMojangShaderVisualOutputSubmitted=" + this.publicMojangShaderVisualOutputSubmitted
                + " publicMojangShaderVisualOutputReady=" + this.publicMojangShaderVisualOutputReady
                + " publicMojangShaderVisualOutputBoundary=" + this.publicMojangShaderVisualOutputBoundary()
                + " shaderDenoiseOutputReady=" + this.shaderDenoiseOutputReady
                + " shaderDenoiseOutputImageReady=" + this.shaderDenoiseOutputImageReady
                + " shaderDenoiseOutputImageOwnedByShaderPass=" + this.shaderDenoiseOutputImageOwnedByShaderPass
                + " shaderDenoiseOutputStorageWritable=" + this.shaderDenoiseOutputStorageWritable
                + " shaderDenoiseOutputBarrierReady=" + this.shaderDenoiseOutputBarrierReady
                + " shaderDenoiseOutputFinalCompositeConsumable=" + this.shaderDenoiseOutputFinalCompositeConsumable
                + " shaderDenoiseRealOutputPathReady=" + this.shaderDenoiseRealOutputPathReady()
                + " shaderDenoiseOutputImageReadinessReady=" + this.shaderDenoiseOutputImageReadinessReady()
                + " shaderDenoiseTemporalHistoryReady=" + this.shaderDenoiseTemporalHistoryReady()
                + " shaderDenoiseRealOutputPrerequisitesReady=" + this.shaderDenoiseRealOutputPrerequisitesReady()
                + " shaderDenoiseOutputImageCandidateReady=" + this.shaderDenoiseOutputImageCandidateReady
                + " shaderDenoiseOutputImageCandidateCpuStaged=" + this.shaderDenoiseOutputImageCandidateCpuStaged
                + " shaderDenoiseOutputImageCandidateNonGpu=" + this.shaderDenoiseOutputImageCandidateNonGpu
                + " realShaderDenoiseOutputReady=" + this.realShaderDenoiseOutputReady()
                + " shaderDenoiseCpuReadbackFallbackActive=" + this.shaderDenoiseCpuReadbackFallbackActive()
                + " shaderDenoiseCpuReadbackFallbackReported=" + this.shaderDenoiseCpuReadbackFallbackReported
                + " shaderDenoiseCpuReadbackFallbackDerived=" + this.shaderDenoiseCpuReadbackFallbackDerived()
                + " cpuReadbackGuidedVisualDenoiseActive=" + this.cpuReadbackGuidedVisualDenoiseActive()
                + " shaderDenoiseOutputReadinessLabel=" + this.shaderDenoiseOutputReadinessLabel()
                + " shaderDenoiseOutputBlockerReason=" + this.shaderDenoiseOutputBlockerReason()
                + " shaderDenoiseRealOutputBoundary=\"" + this.shaderDenoiseRealOutputBoundary() + "\""
                + " shaderDenoiseNoOverclaimStatus=" + this.shaderDenoiseNoOverclaimStatus()
                + " shaderDenoiseOutputPrerequisites=\"" + this.shaderDenoiseOutputPrerequisitesSummary() + "\""
                + " shaderDenoiseOutputImageCandidateSize=" + this.shaderDenoiseOutputImageCandidateWidth
                + "x" + this.shaderDenoiseOutputImageCandidateHeight
                + " shaderDenoiseOutputImageCandidatePixels=" + this.shaderDenoiseOutputImageCandidatePixels
                + " shaderDenoiseOutputImageCandidateBytes=" + this.shaderDenoiseOutputImageCandidateBytes
                + " shaderDenoiseOutputImageCandidateChecksum=" + this.shaderDenoiseOutputImageCandidateChecksum
                + " shaderGeneratedDenoiseOutputReported=" + this.shaderGeneratedDenoiseOutputReported
                + " shaderGeneratedDenoiseOutputImageReady=" + this.shaderGeneratedDenoiseOutputImageReady
                + " shaderGeneratedDenoiseOutputGenerated=" + this.shaderGeneratedDenoiseOutputGenerated
                + " shaderGeneratedDenoiseOutputFinalCompositeConsumed="
                + this.shaderGeneratedDenoiseOutputFinalCompositeConsumed
                + " shaderGeneratedDenoiseOutputReportReady=" + this.shaderGeneratedDenoiseOutputReportReady()
                + " shaderGeneratedDenoiseOutputSize=" + this.shaderGeneratedDenoiseOutputWidth
                + "x" + this.shaderGeneratedDenoiseOutputHeight
                + " shaderGeneratedDenoiseOutputTexelCount=" + this.shaderGeneratedDenoiseOutputTexelCount
                + " shaderGeneratedDenoiseOutputSampleCount=" + this.shaderGeneratedDenoiseOutputSampleCount
                + " shaderGeneratedDenoiseOutputChecksum=" + this.shaderGeneratedDenoiseOutputChecksum
                + " shaderGeneratedDenoiseOutputIdentity=" + this.shaderGeneratedDenoiseOutputIdentity
                + " shaderGeneratedDenoiseOutputBlocker=" + this.shaderGeneratedDenoiseOutputBlocker
                + " shaderDenoiseOutputImageCandidateMarker=" + this.shaderDenoiseOutputImageCandidateMarker
                + " shaderDenoiseOutputImageBlocker=" + this.shaderDenoiseOutputImageBlocker
                + " shaderDenoiseOutputImageCandidateBoundary=" + this.shaderDenoiseOutputImageCandidateBoundary()
                + " shaderDenoiseOutputMaterialReady=" + this.shaderDenoiseOutputMaterialReady
                + " shaderDenoiseShaderGeneratedOutput=" + this.shaderDenoiseShaderGeneratedOutput
                + " shaderGeneratedDenoiseOutputEvidence=" + this.shaderGeneratedDenoiseOutputEvidence
                + " shaderGeneratedDenoiseOutputEvidenceReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " shaderGeneratedDenoiseOutputMarker=" + this.shaderGeneratedDenoiseOutputMarker
                + " cpuFallbackQualityMetrics=" + this.cpuFallbackQualityMetrics
                + " cpuReadbackReady=" + this.cpuDenoisedOutputReadbackReady()
                + " denoiseQualityEvidenceReady=" + this.denoiseQualityEvidenceReady()
                + " readinessBoundary=" + this.denoiseReadinessBoundary()
                + " composite=" + this.compositeSignalLabel()
                + " compositeSize=" + this.compositeWidth + "x" + this.compositeHeight
                + " outputMarker=" + this.outputMarker
                + " rawInputMarker=" + this.rawInputMarker
                + " denoisedOutputMarker=" + this.denoisedOutputMarker
                + " compositeMarker=" + this.compositeMarker
                + " reason=" + this.readinessReason;
    }

    public String compositeSignalLabel() {
        if (!this.compositeStageRecorded) {
            return "missing";
        }
        if (this.compositePlaceholder) {
            return "placeholder";
        }
        return this.compositeReady ? "ready" : "metadata";
    }

    private static String extractBlock(String source, String marker) {
        int markerStart = source.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }
        int contentStart = markerStart + marker.length();
        int depth = 1;
        boolean quoted = false;
        for (int index = contentStart; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == '"') {
                    quoted = false;
                }
                continue;
            }
            if (character == '"') {
                quoted = true;
                continue;
            }
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(contentStart, index);
                }
            }
        }
        return "";
    }

    private static String extractField(String block, String fieldName) {
        String marker = fieldName + "=";
        int markerStart = findFieldMarker(block, marker);
        if (markerStart < 0) {
            return "";
        }
        int valueStart = markerStart + marker.length();
        boolean quoted = valueStart < block.length() && block.charAt(valueStart) == '"';
        int contentStart = quoted ? valueStart + 1 : valueStart;
        for (int index = contentStart; index < block.length(); index++) {
            char character = block.charAt(index);
            if (quoted && character == '"') {
                return block.substring(contentStart, index);
            }
            if (!quoted && (character == ',' || character == '}')) {
                return block.substring(contentStart, index).trim();
            }
        }
        return block.substring(contentStart).trim();
    }

    private static int findFieldMarker(String block, String marker) {
        if (block == null || block.isBlank() || marker == null || marker.isBlank()) {
            return -1;
        }

        int searchFrom = 0;
        while (searchFrom < block.length()) {
            int markerStart = block.indexOf(marker, searchFrom);
            if (markerStart < 0) {
                return -1;
            }
            if (markerStart == 0 || block.charAt(markerStart - 1) == ',') {
                return markerStart;
            }
            searchFrom = markerStart + marker.length();
        }
        return -1;
    }

    private static boolean parseBoolean(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? 0L : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? 0.0D : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    private static int parseIntField(String block, String... fieldNames) {
        return parseInt(extractFieldOrDefault(block, fieldNames));
    }

    private static long parseLongField(String block, String... fieldNames) {
        return parseLong(extractFieldOrDefault(block, fieldNames));
    }

    private static double parseDoubleField(String block, String... fieldNames) {
        return parseDouble(extractFieldOrDefault(block, fieldNames));
    }

    private static boolean parseBooleanField(String block, String... fieldNames) {
        return parseBoolean(extractFieldOrDefault(block, fieldNames));
    }

    private static String extractFieldOrDefault(String block, String... fieldNames) {
        if (fieldNames == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            String value = extractField(block, fieldName);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static int dimensionComponentInt(String dimensions, int component) {
        if (dimensions == null || dimensions.isBlank()) {
            return 0;
        }
        String[] parts = dimensions.trim().split("x", 3);
        if (component < 0 || component >= parts.length) {
            return 0;
        }
        return parseInt(parts[component]);
    }

    private static int dimensionOrFieldInt(String block, String dimensionsField, String scalarField, int component) {
        int dimensionValue = dimensionComponentInt(extractField(block, dimensionsField), component);
        return dimensionValue > 0 ? dimensionValue : parseInt(extractField(block, scalarField));
    }
}
