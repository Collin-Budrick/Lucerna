package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record LightingDispatchStageTelemetryStatus(
        String stageId,
        Boolean enabled,
        Long generation,
        String dispatchGroups,
        String dimensions,
        String ioCounts,
        Long sampleCount,
        Long candidateCount,
        Long rayCount,
        Long cacheReadCount,
        Long cacheWriteCount,
        Long flags,
        Boolean placeholder,
        Boolean validated,
        Boolean debugOverlay,
        Boolean readyForNativeExecution,
        String readinessReason,
        Long frameIndex,
        Boolean recordedThisFrame,
        Boolean payloadAccepted,
        Long payloadGeneration,
        String payloadGenerationRange,
        Long payloadFrameIndex,
        Long celestialCount,
        Long emissiveCount,
        Long shadowCandidateCount,
        Long budgetedShadowCandidateCount,
        Long sectionSnapshotCount,
        Boolean metadataOnly,
        Boolean cpuOutputGenerated,
        String outputDimensions,
        Long outputPixelCount,
        String outputEnergy,
        Long outputChecksum,
        Boolean payloadValidated,
        Boolean payloadHasDirectWork,
        Boolean payloadReadyForShadowTracing,
        Boolean rawSourceReady,
        Boolean cpuDenoiseReady,
        Boolean shaderDenoiseIntended,
        Boolean shaderOutputReady,
        Boolean shaderDispatchPrepared,
        Boolean shaderOutputImageReady,
        Boolean shaderOutputMaterialReady,
        Boolean shaderGeneratedOutput,
        Boolean shaderGeneratedDenoiseOutputEvidence,
        Boolean publicMojangShaderVisualOutputAttempted,
        Boolean publicMojangShaderVisualOutputSubmitted,
        Boolean publicMojangShaderVisualOutputReady,
        Boolean cpuReadbackFallback,
        Boolean realShaderDenoiseOutput,
        Boolean realShaderDenoiseOutputReady,
        Boolean shaderOutputImageCandidateReady,
        Boolean shaderOutputImageCandidateCpuStaged,
        Boolean shaderOutputImageCandidateNonGpu,
        String shaderOutputImageCandidateDimensions,
        Long shaderOutputImageCandidatePixels,
        Long shaderOutputImageCandidateBytes,
        Long shaderOutputImageCandidateChecksum,
        String shaderOutputImageCandidateMarker,
        String shaderOutputReadinessLabel,
        String shaderOutputBlockerReason,
        String shaderDenoiseBlockers,
        Long edgeRejectionCount,
        Long historyRejectionCount,
        Long temporalStablePixelCount,
        Long temporalUnstablePixelCount,
        Long frameDeltaPixelCount,
        String frameDeltaMeanDelta,
        Long previousOutputChecksum,
        Long currentOutputChecksum,
        String historyConfidence,
        String flickerScore,
        String ghostingRisk,
        Boolean temporalReady,
        String temporalReadinessMarker,
        String sourceIdentity,
        String evidenceBoundary,
        Long physicalSceneLinkScore,
        Long physicalOutputChecksum,
        Boolean physicalSceneLinked,
        Boolean physicalSurfaceContribution,
        Boolean localizedEmissiveSpill,
        Boolean hueShiftedBounce,
        Boolean contactShadowDarkening,
        Boolean finalPhysicalCompositeReady,
        Boolean gBufferDepthSamplingEvidence,
        Boolean gBufferDepthTextureSampled,
        Boolean gBufferDepthMetadataOnly,
        Long gBufferDepthSampleCount,
        Boolean realShadowMapEvidence,
        Boolean shadowMapRendered,
        Boolean shadowMapSampled,
        Boolean shadowMapMetadataOnly,
        Boolean nativeShadowMapMask,
        Boolean shadowMapOutputConsumed,
        Boolean realShadowMapComposite,
        Boolean screenSpaceShadowDecal,
        Boolean lowResDirectTextureShadowProof,
        Boolean voxelRayTracedLightingConsumedEvidence,
        Boolean realTracedLightingConsumed,
        Boolean realGpuTraversalExecuted,
        Boolean tracedLightingMetadataOnly,
        Boolean previewFallbackContribution,
        Boolean metadataOnlyProofRejected,
        Boolean focusWindowCaptureRejected,
        Boolean proofMarkerEvidenceRejected,
        Boolean temporaryDirectSubstitutionRejected,
        Boolean rectangularWashoutRejected,
        Boolean wrongWindowScreenshotRejected,
        Boolean blankScreenshotRejected,
        String physicalSceneMarker,
        String physicalOutputMarker,
        String emissiveSpillMarker,
        String coloredBounceMarker,
        String contactShadowMarker,
        String finalPhysicalCompositeMarker,
        String gBufferDepthSamplingMarker,
        String shadowMapEvidenceMarker,
        String voxelRayTracedLightingMarker,
        String shaderGeneratedDenoiseOutputMarker,
        String proofBoundaryMarker,
        Map<String, String> details
) {
    public LightingDispatchStageTelemetryStatus {
        stageId = cleanStageId(stageId);
        dispatchGroups = blankToEmpty(stripQuotes(dispatchGroups));
        dimensions = blankToEmpty(stripQuotes(dimensions));
        ioCounts = blankToEmpty(stripQuotes(ioCounts));
        readinessReason = blankToEmpty(stripQuotes(readinessReason));
        payloadGenerationRange = blankToEmpty(stripQuotes(payloadGenerationRange));
        outputDimensions = blankToEmpty(stripQuotes(outputDimensions));
        outputEnergy = blankToEmpty(stripQuotes(outputEnergy));
        shaderDenoiseBlockers = blankToEmpty(stripQuotes(shaderDenoiseBlockers));
        frameDeltaMeanDelta = blankToEmpty(stripQuotes(frameDeltaMeanDelta));
        historyConfidence = blankToEmpty(stripQuotes(historyConfidence));
        flickerScore = blankToEmpty(stripQuotes(flickerScore));
        ghostingRisk = blankToEmpty(stripQuotes(ghostingRisk));
        temporalReadinessMarker = blankToEmpty(stripQuotes(temporalReadinessMarker));
        sourceIdentity = blankToEmpty(stripQuotes(sourceIdentity));
        evidenceBoundary = blankToEmpty(stripQuotes(evidenceBoundary));
        physicalSceneMarker = blankToEmpty(stripQuotes(physicalSceneMarker));
        physicalOutputMarker = blankToEmpty(stripQuotes(physicalOutputMarker));
        emissiveSpillMarker = blankToEmpty(stripQuotes(emissiveSpillMarker));
        coloredBounceMarker = blankToEmpty(stripQuotes(coloredBounceMarker));
        contactShadowMarker = blankToEmpty(stripQuotes(contactShadowMarker));
        finalPhysicalCompositeMarker = blankToEmpty(stripQuotes(finalPhysicalCompositeMarker));
        gBufferDepthSamplingMarker = blankToEmpty(stripQuotes(gBufferDepthSamplingMarker));
        shadowMapEvidenceMarker = blankToEmpty(stripQuotes(shadowMapEvidenceMarker));
        voxelRayTracedLightingMarker = blankToEmpty(stripQuotes(voxelRayTracedLightingMarker));
        shaderGeneratedDenoiseOutputMarker = blankToEmpty(stripQuotes(shaderGeneratedDenoiseOutputMarker));
        proofBoundaryMarker = blankToEmpty(stripQuotes(proofBoundaryMarker));
        shaderOutputImageCandidateDimensions = blankToEmpty(stripQuotes(shaderOutputImageCandidateDimensions));
        shaderOutputImageCandidateMarker = blankToEmpty(stripQuotes(shaderOutputImageCandidateMarker));
        shaderOutputReadinessLabel = blankToEmpty(stripQuotes(shaderOutputReadinessLabel));
        shaderOutputBlockerReason = blankToEmpty(stripQuotes(shaderOutputBlockerReason));
        details = immutable(details);
    }

    public static LightingDispatchStageTelemetryStatus fromFields(Map<String, String> fields) {
        Map<String, String> normalizedFields = normalizeFields(fields);
        String stageId = firstPresent(normalizedFields, "id", "stage", "stage_id", "stage_name", "name");
        Boolean enabled = parseBoolean(firstPresent(normalizedFields, "enabled_this_packet", "enabled", "active"));
        Long generation = parseLong(firstPresent(
                normalizedFields,
                "last_generation",
                "generation",
                "dispatch_generation",
                "stage_generation"
        ));
        String dispatchGroups = firstPresent(
                normalizedFields,
                "last_dispatch",
                "dispatch",
                "dispatch_groups",
                "groups",
                "group_count"
        );
        if (dispatchGroups.isBlank()) {
            dispatchGroups = xyzLabel(
                    firstPresent(normalizedFields, "dispatch_x", "groups_x", "group_x"),
                    firstPresent(normalizedFields, "dispatch_y", "groups_y", "group_y"),
                    firstPresent(normalizedFields, "dispatch_z", "groups_z", "group_z")
            );
        }

        String dimensions = firstPresent(normalizedFields, "last_size", "size", "dimensions", "resolution");
        if (dimensions.isBlank()) {
            dimensions = xyLabel(
                    firstPresent(normalizedFields, "width", "last_width"),
                    firstPresent(normalizedFields, "height", "last_height")
            );
        }

        String ioCounts = firstPresent(normalizedFields, "last_io", "io", "io_counts");
        if (ioCounts.isBlank()) {
            ioCounts = pairLabel(
                    firstPresent(normalizedFields, "inputs", "input_count", "last_input_count"),
                    firstPresent(normalizedFields, "outputs", "output_count", "last_output_count")
            );
        }

        Long sampleCount = parseLong(firstPresent(
                normalizedFields,
                "last_samples",
                "samples",
                "sample_count",
                "last_sample_count"
        ));
        Long candidateCount = parseLong(firstPresent(
                normalizedFields,
                "last_candidates",
                "candidates",
                "candidate_count",
                "last_candidate_count",
                "shadow_candidates",
                "shadow_candidate_count",
                "direct_shadow_candidates"
        ));
        Long rayCount = parseLong(firstPresent(normalizedFields, "last_rays", "rays", "ray_count"));
        Long cacheReadCount = parseLong(firstPresent(
                normalizedFields,
                "cache_reads",
                "cache_read_count",
                "last_cache_reads"
        ));
        Long cacheWriteCount = parseLong(firstPresent(
                normalizedFields,
                "cache_writes",
                "cache_write_count",
                "last_cache_writes"
        ));

        String cachePair = firstPresent(normalizedFields, "last_cache", "cache", "cache_counts");
        Long[] parsedCachePair = parseLongPair(cachePair);
        if (cacheReadCount == null) {
            cacheReadCount = parsedCachePair[0];
        }
        if (cacheWriteCount == null) {
            cacheWriteCount = parsedCachePair[1];
        }
        Long flags = parseLong(firstPresent(normalizedFields, "last_flags", "flags", "stage_flags"));
        Boolean placeholder = parseBoolean(firstPresent(normalizedFields, "placeholder", "metadata_only"));
        Boolean validated = parseBoolean(firstPresent(normalizedFields, "validated", "valid"));
        Boolean debugOverlay = parseBoolean(firstPresent(normalizedFields, "debug_overlay", "debug"));
        Boolean readyForNativeExecution = parseBoolean(firstPresent(
                normalizedFields,
                "ready_for_native_execution",
                "native_ready",
                "ready",
                "executable"
        ));
        String readinessReason = firstPresent(
                normalizedFields,
                "readiness_reason",
                "ready_reason",
                "native_readiness_reason",
                "reason"
        );
        Long frameIndex = parseLong(firstPresent(
                normalizedFields,
                "last_frame",
                "frame",
                "frame_index",
                "last_frame_index",
                "dispatch_frame"
        ));
        Boolean recordedThisFrame = parseBoolean(firstPresent(
                normalizedFields,
                "recorded_this_frame",
                "recorded",
                "submitted_this_frame"
        ));
        Boolean payloadAccepted = parseBoolean(firstPresent(
                normalizedFields,
                "payload_accepted",
                "direct_payload_accepted",
                "accepted"
        ));
        Long payloadGeneration = parseLong(firstPresent(
                normalizedFields,
                "payload_generation",
                "last_payload_generation",
                "direct_lighting_payload_generation"
        ));
        String payloadGenerationRange = firstPresent(
                normalizedFields,
                "payload_generation_range",
                "last_payload_generation_range",
                "direct_lighting_payload_generation_range"
        );
        Long payloadFrameIndex = parseLong(firstPresent(
                normalizedFields,
                "payload_frame",
                "payload_frame_index",
                "last_payload_frame",
                "last_payload_frame_index"
        ));
        Long celestialCount = parseLong(firstPresent(
                normalizedFields,
                "celestial_count",
                "celestial",
                "celestial_lights",
                "celestial_light_count"
        ));
        Long emissiveCount = parseLong(firstPresent(
                normalizedFields,
                "emissive_count",
                "emissive",
                "emissive_lights",
                "emissive_light_count"
        ));
        Long shadowCandidateCount = parseLong(firstPresent(
                normalizedFields,
                "shadow_candidate_count",
                "shadow_candidates",
                "shadow",
                "direct_shadow_candidates"
        ));
        Long budgetedShadowCandidateCount = parseLong(firstPresent(
                normalizedFields,
                "budgeted_shadow_candidate_count",
                "budgeted_shadow_candidates",
                "budgeted_shadow"
        ));
        Long sectionSnapshotCount = parseLong(firstPresent(
                normalizedFields,
                "section_snapshot_count",
                "section_snapshots",
                "sections",
                "section_count"
        ));
        Boolean metadataOnly = parseBoolean(firstPresent(
                normalizedFields,
                "metadata_only",
                "payload_metadata_only"
        ));
        Boolean cpuOutputGenerated = parseBoolean(firstPresent(
                normalizedFields,
                "cpu_output_generated",
                "direct_cpu_output_generated",
                "gi_cpu_output_generated",
                "native_gi_output_generated"
        ));
        String outputDimensions = xyLabel(
                firstPresent(normalizedFields, "output_width", "direct_output_width", "gi_output_width", "native_gi_output_width"),
                firstPresent(normalizedFields, "output_height", "direct_output_height", "gi_output_height", "native_gi_output_height")
        );
        Long outputPixelCount = parseLong(firstPresent(
                normalizedFields,
                "output_pixels",
                "output_pixel_count",
                "direct_output_pixels",
                "gi_output_pixels",
                "gi_output_pixel_count",
                "native_gi_output_pixels",
                "native_gi_output_pixel_count"
        ));
        String outputEnergy = firstPresent(
                normalizedFields,
                "output_energy",
                "direct_output_energy",
                "gi_output_energy",
                "native_gi_output_energy"
        );
        Long outputChecksum = parseLong(firstPresent(
                normalizedFields,
                "output_checksum",
                "direct_output_checksum",
                "gi_output_checksum",
                "native_gi_output_checksum"
        ));
        Boolean payloadValidated = parseBoolean(firstPresent(
                normalizedFields,
                "payload_validated",
                "direct_payload_validated"
        ));
        Boolean payloadHasDirectWork = parseBoolean(firstPresent(
                normalizedFields,
                "payload_has_direct_work",
                "has_direct_work"
        ));
        Boolean payloadReadyForShadowTracing = parseBoolean(firstPresent(
                normalizedFields,
                "payload_ready_for_shadow_tracing",
                "ready_for_shadow_tracing"
        ));
        Boolean rawSourceReady = parseBoolean(firstPresent(
                normalizedFields,
                "raw_source_ready",
                "raw_input_ready",
                "input_source_ready",
                "raw_gi_ready",
                "raw_diffuse_gi_ready",
                "raw_diffuse_gi_input_ready",
                "denoise_raw_source_ready",
                "denoise_raw_input_ready"
        ));
        Boolean cpuDenoiseReady = parseBoolean(firstPresent(
                normalizedFields,
                "cpu_denoise_ready",
                "cpu_denoise_output_ready",
                "cpu_denoised_output_ready",
                "cpu_readback_denoise_ready",
                "cpu_readback_denoised_output_ready",
                "denoise_cpu_ready",
                "denoise_cpu_output_ready"
        ));
        Boolean shaderDenoiseIntended = parseBoolean(firstPresent(
                normalizedFields,
                "shader_denoise_intent",
                "shader_denoise_intended",
                "shader_denoise_planned",
                "shader_denoise_enabled",
                "denoise_shader_intended",
                "denoise_shader_planned",
                "shader_denoise_contract_ready"
        ));
        Boolean shaderOutputReady = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_ready",
                "shader_denoise_output_ready",
                "real_denoise_shader_output",
                "real_shader_denoise_output",
                "real_shader_gi_output",
                "gpu_denoise_output_ready",
                "gpu_denoise_output",
                "shader_denoise_ready"
        ));
        Boolean shaderDispatchPrepared = parseBoolean(firstPresent(
                normalizedFields,
                "shader_dispatch_prepared",
                "shader_denoise_dispatch_prepared",
                "denoise_shader_dispatch_prepared",
                "real_shader_denoise_dispatch_prepared",
                "gpu_denoise_dispatch_prepared"
        ));
        Boolean shaderOutputImageReady = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_image_ready",
                "shader_denoise_output_image_ready",
                "real_shader_output_image_ready",
                "real_shader_denoise_output_image_ready",
                "gpu_denoise_output_image_ready"
        ));
        Boolean shaderOutputMaterialReady = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_material_ready",
                "shader_denoise_output_material_ready",
                "real_shader_output_material_ready",
                "real_shader_denoise_output_material_ready",
                "gpu_denoise_output_material_ready"
        ));
        Boolean shaderGeneratedOutput = parseBoolean(firstPresent(
                normalizedFields,
                "shader_generated_output",
                "shader_denoise_generated_output",
                "real_shader_generated_output",
                "real_shader_denoise_generated_output",
                "real_denoise_shader_generated_output",
                "gpu_denoise_generated_output"
        ));
        Boolean shaderGeneratedDenoiseOutputEvidence = parseBoolean(firstPresent(
                normalizedFields,
                "shader_generated_denoise_output_evidence",
                "shader_denoise_generated_output_evidence",
                "real_shader_denoise_output_evidence",
                "shader_denoise_output_evidence",
                "shader_generated_denoise_output_proven"
        ));
        Boolean publicMojangShaderVisualOutputAttempted = parseBoolean(firstPresent(
                normalizedFields,
                "public_mojang_shader_visual_output_attempted",
                "public_mojang_shader_output_attempted",
                "public_mojang_visual_output_attempted",
                "public_mojang_denoise_visual_output_attempted",
                "shader_visual_output_attempted"
        ));
        Boolean publicMojangShaderVisualOutputSubmitted = parseBoolean(firstPresent(
                normalizedFields,
                "public_mojang_shader_visual_output_submitted",
                "public_mojang_shader_output_submitted",
                "public_mojang_visual_output_submitted",
                "public_mojang_denoise_visual_output_submitted",
                "shader_visual_output_submitted"
        ));
        Boolean publicMojangShaderVisualOutputReady = parseBoolean(firstPresent(
                normalizedFields,
                "public_mojang_shader_visual_output_ready",
                "public_mojang_shader_output_ready",
                "public_mojang_visual_output_ready",
                "public_mojang_denoise_visual_output_ready",
                "shader_visual_output_ready"
        ));
        Boolean cpuReadbackFallback = parseBoolean(firstPresent(
                normalizedFields,
                "cpu_readback_fallback",
                "cpu_readback_fallback_used",
                "cpu_denoise_readback_fallback",
                "shader_denoise_cpu_readback_fallback",
                "denoise_cpu_readback_fallback"
        ));
        Boolean realShaderDenoiseOutput = parseBoolean(firstPresent(
                normalizedFields,
                "real_denoise_shader_output",
                "real_shader_denoise_output",
                "real_shader_denoise_output_proven"
        ));
        Boolean shaderOutputImageCandidateReady = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_ready",
                "shader_output_image_candidate_present",
                "shader_denoise_output_image_candidate_ready"
        ));
        Boolean shaderOutputImageCandidateCpuStaged = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_cpu_staged",
                "shader_denoise_output_image_candidate_cpu_staged"
        ));
        Boolean shaderOutputImageCandidateNonGpu = parseBoolean(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_non_gpu",
                "shader_denoise_output_image_candidate_non_gpu"
        ));
        String shaderOutputImageCandidateDimensions = firstPresent(
                normalizedFields,
                "shader_output_image_candidate_dimensions",
                "shader_output_image_candidate_size",
                "shader_denoise_output_image_candidate_size"
        );
        if (shaderOutputImageCandidateDimensions.isBlank()) {
            shaderOutputImageCandidateDimensions = xyLabel(
                    firstPresent(
                            normalizedFields,
                            "shader_output_image_candidate_width",
                            "shader_denoise_output_image_candidate_width"
                    ),
                    firstPresent(
                            normalizedFields,
                            "shader_output_image_candidate_height",
                            "shader_denoise_output_image_candidate_height"
                    )
            );
        }
        Long shaderOutputImageCandidatePixels = parseLong(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_pixels",
                "shader_denoise_output_image_candidate_pixels"
        ));
        Long shaderOutputImageCandidateBytes = parseLong(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_bytes",
                "shader_denoise_output_image_candidate_bytes"
        ));
        Long shaderOutputImageCandidateChecksum = parseLong(firstPresent(
                normalizedFields,
                "shader_output_image_candidate_checksum",
                "shader_denoise_output_image_candidate_checksum"
        ));
        String shaderOutputImageCandidateMarker = firstPresent(
                normalizedFields,
                "shader_output_image_candidate_marker",
                "shader_output_image_candidate_source",
                "shader_denoise_output_image_candidate_marker"
        );
        String shaderOutputReadinessLabel = firstPresent(
                normalizedFields,
                "shader_output_readiness_label",
                "shader_denoise_output_readiness_label",
                "shader_denoise_output_readiness_marker"
        );
        String shaderOutputBlockerReason = firstPresent(
                normalizedFields,
                "shader_output_blocker_reason",
                "shader_output_image_candidate_blocker",
                "shader_output_image_blocker",
                "shader_denoise_output_blocker_reason",
                "shader_denoise_output_image_blocker"
        );
        Boolean realShaderDenoiseOutputReady = Boolean.TRUE.equals(realShaderDenoiseOutput)
                && Boolean.TRUE.equals(shaderOutputReady)
                && Boolean.TRUE.equals(shaderOutputImageReady)
                && Boolean.TRUE.equals(shaderOutputMaterialReady)
                && Boolean.TRUE.equals(shaderGeneratedOutput)
                && !Boolean.TRUE.equals(cpuReadbackFallback)
                && !Boolean.TRUE.equals(shaderOutputImageCandidateCpuStaged)
                && !Boolean.TRUE.equals(shaderOutputImageCandidateNonGpu);
        String shaderDenoiseBlockers = firstPresent(
                normalizedFields,
                "shader_denoise_blockers",
                "shader_denoise_blocker",
                "shader_blockers",
                "shader_blocker",
                "denoise_blockers",
                "output_blockers",
                "blockers",
                "blocker"
        );
        Long edgeRejectionCount = parseLong(firstPresent(
                normalizedFields,
                "edge_rejection_count",
                "edge_reject_count",
                "edge_rejections",
                "edge_rejected",
                "edge_rejected_count",
                "denoise_edge_rejections"
        ));
        Long historyRejectionCount = parseLong(firstPresent(
                normalizedFields,
                "history_rejection_count",
                "history_reject_count",
                "history_rejections",
                "history_rejected",
                "history_rejected_count",
                "temporal_history_rejected",
                "temporal_history_rejection_count",
                "last_history_rejected"
        ));
        Long temporalStablePixelCount = parseLong(firstPresent(
                normalizedFields,
                "temporal_stable_pixels",
                "temporal_stable_pixel_count",
                "stable_pixels",
                "stable_pixel_count",
                "temporal_pixels_stable"
        ));
        Long temporalUnstablePixelCount = parseLong(firstPresent(
                normalizedFields,
                "temporal_unstable_pixels",
                "temporal_unstable_pixel_count",
                "unstable_pixels",
                "unstable_pixel_count",
                "temporal_pixels_unstable"
        ));
        Long frameDeltaPixelCount = parseLong(firstPresent(
                normalizedFields,
                "frame_delta_pixels",
                "frame_delta_pixel_count",
                "temporal_frame_delta_pixels",
                "temporal_changed_pixels",
                "changed_pixels",
                "denoised_output_changed_pixels"
        ));
        String frameDeltaMeanDelta = firstPresent(
                normalizedFields,
                "frame_delta_mean_delta",
                "frame_delta_mean_abs_delta",
                "temporal_frame_delta_mean_delta",
                "temporal_mean_abs_delta",
                "mean_abs_delta",
                "denoised_output_mean_abs_delta"
        );
        Long previousOutputChecksum = parseLong(firstPresent(
                normalizedFields,
                "previous_output_checksum",
                "previous_denoised_output_checksum",
                "previous_frame_checksum",
                "previous_checksum",
                "temporal_previous_checksum"
        ));
        Long currentOutputChecksum = parseLong(firstPresent(
                normalizedFields,
                "current_output_checksum",
                "current_denoised_output_checksum",
                "current_frame_checksum",
                "current_checksum",
                "temporal_current_checksum"
        ));
        String historyConfidence = firstPresent(
                normalizedFields,
                "history_confidence",
                "avg_history_confidence",
                "temporal_confidence",
                "last_history_confidence",
                "temporal_history_confidence"
        );
        String flickerScore = firstPresent(
                normalizedFields,
                "flicker_score",
                "temporal_flicker_score",
                "denoise_flicker_score",
                "temporal_instability_score"
        );
        String ghostingRisk = firstPresent(
                normalizedFields,
                "ghosting_risk",
                "ghosting_risk_marker",
                "temporal_ghosting_risk",
                "temporal_ghosting_risk_marker",
                "temporal_ghosting_marker"
        );
        Boolean temporalReady = parseBoolean(firstPresent(
                normalizedFields,
                "temporal_ready",
                "temporal_stability_ready",
                "temporal_history_ready",
                "history_ready",
                "temporal_proof_ready",
                "temporal_acceptance_ready"
        ));
        String temporalReadinessMarker = firstPresent(
                normalizedFields,
                "temporal_readiness_marker",
                "temporal_stability_readiness_marker",
                "temporal_history_marker",
                "last_temporal_history_marker",
                "history_marker",
                "temporal_marker"
        );
        String sourceIdentity = firstPresent(
                normalizedFields,
                "source_identity",
                "source_id",
                "source",
                "source_label",
                "output_source",
                "output_source_label",
                "preview_source",
                "native_output_source",
                "native_gi_output_source",
                "denoise_source",
                "denoise_source_identity"
        );
        String evidenceBoundary = firstPresent(
                normalizedFields,
                "evidence_boundary",
                "proof_boundary",
                "boundary",
                "quality_boundary",
                "shader_denoise_evidence_boundary",
                "shader_denoise_boundary",
                "denoise_evidence_boundary",
                "denoise_quality_boundary"
        );
        Long physicalSceneLinkScore = parseLong(firstPresent(
                normalizedFields,
                "physical_scene_link_score",
                "physical_gi_scene_link_score",
                "gi_physical_scene_link_score",
                "scene_link_score",
                "scene_linked_score"
        ));
        Long physicalOutputChecksum = parseLong(firstPresent(
                normalizedFields,
                "physical_output_checksum",
                "physical_gi_output_checksum",
                "gi_physical_output_checksum",
                "native_physical_output_checksum"
        ));
        Boolean physicalSceneLinked = parseBoolean(firstPresent(
                normalizedFields,
                "physical_scene_linked",
                "physical_gi_scene_linked",
                "scene_linked_physical",
                "gi_scene_linked"
        ));
        Boolean physicalSurfaceContribution = parseBoolean(firstPresent(
                normalizedFields,
                "physical_surface_contribution",
                "physical_gi_surface_contribution",
                "surface_physical_contribution",
                "physical_contribution"
        ));
        Boolean localizedEmissiveSpill = parseBoolean(firstPresent(
                normalizedFields,
                "localized_emissive_spill",
                "localized_spill",
                "emissive_spill_localized",
                "emissive_spill"
        ));
        Boolean hueShiftedBounce = parseBoolean(firstPresent(
                normalizedFields,
                "hue_shifted_bounce",
                "colored_bounce_hue_shift",
                "colored_bounce",
                "bounce_hue_shifted"
        ));
        Boolean contactShadowDarkening = parseBoolean(firstPresent(
                normalizedFields,
                "contact_shadow_darkening",
                "contact_shadows",
                "local_occlusion_darkening",
                "local_occlusion"
        ));
        Boolean finalPhysicalCompositeReady = parseBoolean(firstPresent(
                normalizedFields,
                "final_physical_composite_ready",
                "physical_final_composite_ready",
                "final_composite_physical_ready",
                "final_physical_composite"
        ));
        Boolean gBufferDepthSamplingEvidence = parseBoolean(firstPresent(
                normalizedFields,
                "g_buffer_depth_sampling_evidence",
                "gbuffer_depth_sampling_evidence",
                "depth_sampling_evidence",
                "real_gbuffer_depth_sampling",
                "real_depth_texture_sampled",
                "g_buffer_depth_sampled",
                "gbuffer_depth_sampled"
        ));
        Boolean gBufferDepthTextureSampled = parseBoolean(firstPresent(
                normalizedFields,
                "g_buffer_depth_texture_sampled",
                "gbuffer_depth_texture_sampled",
                "depth_texture_sampled",
                "real_depth_texture_sampled",
                "g_buffer_depth_sampled",
                "gbuffer_depth_sampled"
        ));
        Boolean gBufferDepthMetadataOnly = parseBoolean(firstPresent(
                normalizedFields,
                "g_buffer_depth_metadata_only",
                "gbuffer_depth_metadata_only",
                "depth_sampling_metadata_only",
                "g_buffer_depth_sampling_metadata_only",
                "depth_texture_metadata_only"
        ));
        Long gBufferDepthSampleCount = parseLong(firstPresent(
                normalizedFields,
                "g_buffer_depth_sample_count",
                "gbuffer_depth_sample_count",
                "depth_sample_count",
                "g_buffer_depth_samples",
                "gbuffer_depth_samples",
                "depth_samples"
        ));
        Boolean realShadowMapEvidence = parseBoolean(firstPresent(
                normalizedFields,
                "real_shadow_map_evidence",
                "real_shadow_map",
                "shadow_map_evidence",
                "shadowmap_evidence",
                "real_shadow_map_ready",
                "shadow_map_ready"
        ));
        Boolean shadowMapRendered = parseBoolean(firstPresent(
                normalizedFields,
                "shadow_map_rendered",
                "shadowmap_rendered",
                "real_shadow_map_rendered",
                "shadow_map_output_rendered"
        ));
        Boolean shadowMapSampled = parseBoolean(firstPresent(
                normalizedFields,
                "shadow_map_sampled",
                "shadowmap_sampled",
                "real_shadow_map_sampled",
                "shadow_map_depth_sampled"
        ));
        Boolean shadowMapMetadataOnly = parseBoolean(firstPresent(
                normalizedFields,
                "shadow_map_metadata_only",
                "shadowmap_metadata_only",
                "real_shadow_map_metadata_only",
                "shadow_map_evidence_metadata_only"
        ));
        Boolean nativeShadowMapMask = parseBoolean(firstPresent(
                normalizedFields,
                "native_shadow_map_mask",
                "nativeshadowmapmask",
                "native_shadowmap_mask",
                "native_shadow_map_output_mask",
                "nativeshadowmapoutputmask",
                "shadow_map_mask_native",
                "shadowmap_mask_native",
                "shadow_map_source_native"
        ));
        Boolean shadowMapOutputConsumed = parseBoolean(firstPresent(
                normalizedFields,
                "shadow_map_output_consumed",
                "shadowmapoutputconsumed",
                "shadowmap_output_consumed",
                "native_shadow_map_consumed",
                "nativeshadowmapconsumed",
                "native_shadowmap_consumed",
                "shadow_map_consumed",
                "shadowmapconsumed",
                "shadowmap_consumed"
        ));
        Boolean realShadowMapComposite = parseBoolean(firstPresent(
                normalizedFields,
                "real_shadow_map_composite",
                "realshadowmapcomposite",
                "shadow_map_final_composite",
                "shadowmapfinalcomposite",
                "shadowmap_final_composite",
                "native_shadow_map_composited",
                "nativeshadowmapcomposited",
                "native_shadowmap_composited"
        ));
        Boolean screenSpaceShadowDecal = parseBoolean(firstPresent(
                normalizedFields,
                "screen_space_shadow_decal",
                "screenspaceshadowdecal",
                "screenspace_shadow_decal",
                "screen_space_shadow",
                "screenspaceshadow",
                "shadow_decal_screen_space"
        ));
        Boolean lowResDirectTextureShadowProof = parseBoolean(firstPresent(
                normalizedFields,
                "low_res_direct_texture_shadow_proof",
                "lowresdirecttextureshadowproof",
                "lowres_direct_texture_shadow_proof",
                "low_res_direct_texture",
                "cpu_direct_texture_composite",
                "cpudirecttexturecomposite",
                "directlightlowrestexture",
                "direct_light_low_res_texture"
        ));
        Boolean voxelRayTracedLightingConsumedEvidence = parseBoolean(firstPresent(
                normalizedFields,
                "voxel_ray_traced_lighting_consumed_evidence",
                "voxel_traced_lighting_consumed_evidence",
                "ray_traced_lighting_consumed_evidence",
                "traced_lighting_consumed_evidence",
                "voxel_ray_traced_lighting_consumed",
                "ray_traced_lighting_consumed"
        ));
        Boolean realTracedLightingConsumed = parseBoolean(firstPresent(
                normalizedFields,
                "real_traced_lighting_consumed",
                "traced_lighting_consumed",
                "real_ray_traced_lighting_consumed",
                "real_voxel_traced_lighting_consumed"
        ));
        Boolean realGpuTraversalExecuted = parseBoolean(firstPresent(
                normalizedFields,
                "real_gpu_traversal_executed",
                "gpu_voxel_traversal_executed",
                "real_gpu_voxel_traversal_executed",
                "real_ray_tracing_executed",
                "hardware_ray_tracing_executed"
        ));
        Boolean tracedLightingMetadataOnly = parseBoolean(firstPresent(
                normalizedFields,
                "traced_lighting_metadata_only",
                "ray_traced_lighting_metadata_only",
                "voxel_ray_traced_lighting_metadata_only",
                "traced_lighting_consumed_metadata_only"
        ));
        Boolean previewFallbackContribution = parseBoolean(firstPresent(
                normalizedFields,
                "preview_fallback_contribution",
                "physical_preview_fallback_contribution",
                "cpu_preview_fallback_contribution"
        ));
        Boolean metadataOnlyProofRejected = parseBoolean(firstPresent(
                normalizedFields,
                "metadata_only_proof_rejected",
                "metadata_preview_rejected",
                "metadata_only_rejected"
        ));
        Boolean focusWindowCaptureRejected = parseBoolean(firstPresent(
                normalizedFields,
                "focus_window_capture_rejected",
                "focus_window_rejected",
                "focus_window_only_rejected"
        ));
        Boolean proofMarkerEvidenceRejected = parseBoolean(firstPresent(
                normalizedFields,
                "proof_marker_evidence_rejected",
                "proof_marker_rejected",
                "proof_marker_source_rejected"
        ));
        Boolean temporaryDirectSubstitutionRejected = parseBoolean(firstPresent(
                normalizedFields,
                "temporary_direct_substitution_rejected",
                "temporary_direct_light_substitution_rejected",
                "temporary_direct_source_rejected",
                "direct_light_substitution_rejected"
        ));
        Boolean rectangularWashoutRejected = parseBoolean(firstPresent(
                normalizedFields,
                "rectangular_washout_rejected",
                "anti_rectangular_washout_passed",
                "washout_rejected"
        ));
        Boolean wrongWindowScreenshotRejected = parseBoolean(firstPresent(
                normalizedFields,
                "wrong_window_screenshot_rejected",
                "wrong_window_capture_rejected",
                "window_screenshot_rejected",
                "wrong_provenance_rejected"
        ));
        Boolean blankScreenshotRejected = parseBoolean(firstPresent(
                normalizedFields,
                "blank_screenshot_rejected",
                "blank_surface_rejected",
                "blank_capture_rejected",
                "blank_frame_rejected"
        ));
        String physicalSceneMarker = firstPresent(
                normalizedFields,
                "physical_scene_marker",
                "physical_gi_scene_marker",
                "scene_link_marker",
                "physical_scene_evidence_marker"
        );
        String physicalOutputMarker = firstPresent(
                normalizedFields,
                "physical_output_marker",
                "physical_gi_output_marker",
                "physical_output_evidence_marker"
        );
        String emissiveSpillMarker = firstPresent(
                normalizedFields,
                "emissive_spill_marker",
                "localized_emissive_spill_marker",
                "spill_marker"
        );
        String coloredBounceMarker = firstPresent(
                normalizedFields,
                "colored_bounce_marker",
                "hue_shifted_bounce_marker",
                "bounce_marker"
        );
        String contactShadowMarker = firstPresent(
                normalizedFields,
                "contact_shadow_marker",
                "contact_shadow_darkening_marker",
                "local_occlusion_marker"
        );
        String finalPhysicalCompositeMarker = firstPresent(
                normalizedFields,
                "final_physical_composite_marker",
                "physical_final_composite_marker",
                "physical_composite_marker"
        );
        String gBufferDepthSamplingMarker = firstPresent(
                normalizedFields,
                "g_buffer_depth_sampling_marker",
                "gbuffer_depth_sampling_marker",
                "depth_sampling_marker",
                "depth_texture_sample_marker"
        );
        String shadowMapEvidenceMarker = firstPresent(
                normalizedFields,
                "shadow_map_evidence_marker",
                "shadowmap_evidence_marker",
                "real_shadow_map_marker",
                "shadow_map_marker"
        );
        String voxelRayTracedLightingMarker = firstPresent(
                normalizedFields,
                "voxel_ray_traced_lighting_marker",
                "ray_traced_lighting_marker",
                "traced_lighting_consumed_marker",
                "voxel_traced_lighting_marker"
        );
        String shaderGeneratedDenoiseOutputMarker = firstPresent(
                normalizedFields,
                "shader_generated_denoise_output_marker",
                "shader_denoise_generated_output_marker",
                "real_shader_denoise_output_marker",
                "shader_denoise_output_evidence_marker"
        );
        String proofBoundaryMarker = firstPresent(
                normalizedFields,
                "proof_boundary_marker",
                "physical_proof_boundary_marker",
                "physical_gi_proof_boundary",
                "gi_proof_boundary_marker"
        );

        return new LightingDispatchStageTelemetryStatus(
                stageId,
                enabled,
                generation,
                dispatchGroups,
                dimensions,
                ioCounts,
                sampleCount,
                candidateCount,
                rayCount,
                cacheReadCount,
                cacheWriteCount,
                flags,
                placeholder,
                validated,
                debugOverlay,
                readyForNativeExecution,
                readinessReason,
                frameIndex,
                recordedThisFrame,
                payloadAccepted,
                payloadGeneration,
                payloadGenerationRange,
                payloadFrameIndex,
                celestialCount,
                emissiveCount,
                shadowCandidateCount,
                budgetedShadowCandidateCount,
                sectionSnapshotCount,
                metadataOnly,
                cpuOutputGenerated,
                outputDimensions,
                outputPixelCount,
                outputEnergy,
                outputChecksum,
                payloadValidated,
                payloadHasDirectWork,
                payloadReadyForShadowTracing,
                rawSourceReady,
                cpuDenoiseReady,
                shaderDenoiseIntended,
                shaderOutputReady,
                shaderDispatchPrepared,
                shaderOutputImageReady,
                shaderOutputMaterialReady,
                shaderGeneratedOutput,
                shaderGeneratedDenoiseOutputEvidence,
                publicMojangShaderVisualOutputAttempted,
                publicMojangShaderVisualOutputSubmitted,
                publicMojangShaderVisualOutputReady,
                cpuReadbackFallback,
                realShaderDenoiseOutput,
                realShaderDenoiseOutputReady,
                shaderOutputImageCandidateReady,
                shaderOutputImageCandidateCpuStaged,
                shaderOutputImageCandidateNonGpu,
                shaderOutputImageCandidateDimensions,
                shaderOutputImageCandidatePixels,
                shaderOutputImageCandidateBytes,
                shaderOutputImageCandidateChecksum,
                shaderOutputImageCandidateMarker,
                shaderOutputReadinessLabel,
                shaderOutputBlockerReason,
                shaderDenoiseBlockers,
                edgeRejectionCount,
                historyRejectionCount,
                temporalStablePixelCount,
                temporalUnstablePixelCount,
                frameDeltaPixelCount,
                frameDeltaMeanDelta,
                previousOutputChecksum,
                currentOutputChecksum,
                historyConfidence,
                flickerScore,
                ghostingRisk,
                temporalReady,
                temporalReadinessMarker,
                sourceIdentity,
                evidenceBoundary,
                physicalSceneLinkScore,
                physicalOutputChecksum,
                physicalSceneLinked,
                physicalSurfaceContribution,
                localizedEmissiveSpill,
                hueShiftedBounce,
                contactShadowDarkening,
                finalPhysicalCompositeReady,
                gBufferDepthSamplingEvidence,
                gBufferDepthTextureSampled,
                gBufferDepthMetadataOnly,
                gBufferDepthSampleCount,
                realShadowMapEvidence,
                shadowMapRendered,
                shadowMapSampled,
                shadowMapMetadataOnly,
                nativeShadowMapMask,
                shadowMapOutputConsumed,
                realShadowMapComposite,
                screenSpaceShadowDecal,
                lowResDirectTextureShadowProof,
                voxelRayTracedLightingConsumedEvidence,
                realTracedLightingConsumed,
                realGpuTraversalExecuted,
                tracedLightingMetadataOnly,
                previewFallbackContribution,
                metadataOnlyProofRejected,
                focusWindowCaptureRejected,
                proofMarkerEvidenceRejected,
                temporaryDirectSubstitutionRejected,
                rectangularWashoutRejected,
                wrongWindowScreenshotRejected,
                blankScreenshotRejected,
                physicalSceneMarker,
                physicalOutputMarker,
                emissiveSpillMarker,
                coloredBounceMarker,
                contactShadowMarker,
                finalPhysicalCompositeMarker,
                gBufferDepthSamplingMarker,
                shadowMapEvidenceMarker,
                voxelRayTracedLightingMarker,
                shaderGeneratedDenoiseOutputMarker,
                proofBoundaryMarker,
                normalizedFields
        );
    }

    public String compactLabel() {
        StringBuilder label = new StringBuilder(this.stageId);
        int fieldCount = 0;
        fieldCount += append(label, "enabled", this.enabled == null ? "" : Boolean.toString(this.enabled));
        fieldCount += append(label, "gen", this.generation == null ? "" : Long.toString(this.generation));
        fieldCount += append(label, "groups", this.dispatchGroups);
        fieldCount += append(label, "samples", this.sampleCount == null ? "" : Long.toString(this.sampleCount));
        fieldCount += append(label, "candidates", this.candidateCount == null ? "" : Long.toString(this.candidateCount));
        fieldCount += append(label, "rays", this.rayCount == null ? "" : Long.toString(this.rayCount));
        String cacheLabel = cacheLabel();
        fieldCount += append(label, "cache", cacheLabel);
        fieldCount += append(label, "ready", this.readyForNativeExecution == null ? "" : Boolean.toString(this.readyForNativeExecution));
        fieldCount += append(label, "payload", this.payloadAccepted == null ? "" : Boolean.toString(this.payloadAccepted));
        fieldCount += append(label, "payloadGen", this.payloadGeneration == null ? "" : Long.toString(this.payloadGeneration));
        fieldCount += append(label, "frame", this.frameIndex == null ? "" : Long.toString(this.frameIndex));
        if (isDenoiseLikeStage()) {
            fieldCount += append(label, "raw", this.rawSourceReady == null ? "" : Boolean.toString(this.rawSourceReady));
            fieldCount += append(label, "cpuDenoise", this.cpuDenoiseReady == null ? "" : Boolean.toString(this.cpuDenoiseReady));
            fieldCount += append(label, "shaderIntent", this.shaderDenoiseIntended == null ? "" : Boolean.toString(this.shaderDenoiseIntended));
            fieldCount += append(label, "shaderOutput", this.shaderOutputReady == null ? "" : Boolean.toString(this.shaderOutputReady));
            fieldCount += append(label, "shaderDispatch", this.shaderDispatchPrepared == null ? "" : Boolean.toString(this.shaderDispatchPrepared));
            fieldCount += append(label, "shaderImage", this.shaderOutputImageReady == null ? "" : Boolean.toString(this.shaderOutputImageReady));
            fieldCount += append(label, "shaderMaterial", this.shaderOutputMaterialReady == null ? "" : Boolean.toString(this.shaderOutputMaterialReady));
            fieldCount += append(label, "shaderGenerated", this.shaderGeneratedOutput == null ? "" : Boolean.toString(this.shaderGeneratedOutput));
            fieldCount += append(label, "publicMojangVisual", publicMojangShaderVisualOutputLabel());
            fieldCount += append(label, "realShaderOutput", this.realShaderDenoiseOutputReady == null ? "" : Boolean.toString(this.realShaderDenoiseOutputReady));
            fieldCount += append(label, "cpuFallback", this.cpuReadbackFallback == null ? "" : Boolean.toString(this.cpuReadbackFallback));
            fieldCount += append(label, "candidateOnly", this.shaderOutputImageCandidateReady == null ? "" : Boolean.toString(this.shaderOutputImageCandidateReady));
            fieldCount += append(label, "temporalReady", this.temporalReady == null ? "" : Boolean.toString(this.temporalReady));
            fieldCount += append(label, "flicker", this.flickerScore);
        }
        if (isPhysicalGiLikeStage() || hasAnyPhysicalGiEvidence()) {
            fieldCount += append(label, "physicalScene", this.physicalSceneLinked == null ? "" : Boolean.toString(this.physicalSceneLinked));
            fieldCount += append(label, "physicalSurface", this.physicalSurfaceContribution == null ? "" : Boolean.toString(this.physicalSurfaceContribution));
            fieldCount += append(label, "spill", this.localizedEmissiveSpill == null ? "" : Boolean.toString(this.localizedEmissiveSpill));
            fieldCount += append(label, "coloredBounce", this.hueShiftedBounce == null ? "" : Boolean.toString(this.hueShiftedBounce));
            fieldCount += append(label, "contactShadow", this.contactShadowDarkening == null ? "" : Boolean.toString(this.contactShadowDarkening));
            fieldCount += append(label, "finalPhysicalComposite", this.finalPhysicalCompositeReady == null ? "" : Boolean.toString(this.finalPhysicalCompositeReady));
            fieldCount += append(label, "sceneScore", this.physicalSceneLinkScore == null ? "" : Long.toString(this.physicalSceneLinkScore));
        }
        if (fieldCount == 0) {
            label.append(" reported");
        }
        return label.toString();
    }

    public String compactStageStatusLine() {
        return this.stageDisplayName()
                + " enabled=" + booleanOrUnknown(this.enabled)
                + " ready=" + booleanOrUnknown(this.readyForNativeExecution)
                + " recorded=" + booleanOrUnknown(this.recordedThisFrame)
                + " frame=" + valueOrUnknown(this.frameIndex)
                + " " + this.stageTimingStatusLine();
    }

    public String stageTimingStatusLine() {
        return "CPU=" + cpuTimingValueLabel()
                + " GPU=" + gpuTimingValueLabel();
    }

    public String stageWorkStatusLine() {
        return "samples=" + valueOrUnknown(this.sampleCount)
                + " candidates=" + valueOrUnknown(this.candidateCount)
                + " rays=" + valueOrUnknown(this.rayCount)
                + " cache=" + this.cacheLabel()
                + " output=" + outputStatusLabel();
    }

    public String explicitMeasurementBoundaryLine() {
        String cpuBoundary = this.hasMeasuredCpuTiming() ? "stage CPU timing reported" : "CPU timing pending";
        String gpuBoundary = this.hasMeasuredGpuTiming()
                ? "real GPU timing reported"
                : "real GPU timestamp unavailable/pending";
        return this.stageDisplayName() + ": " + cpuBoundary + ", " + gpuBoundary;
    }

    public boolean hasMeasuredCpuTiming() {
        return hasAnyDetail("cpu_ms", "cpu_millis", "cpu_time_ms", "native_cpu_ms", "stage_cpu_ms");
    }

    public boolean hasMeasuredGpuTiming() {
        return hasAnyDetail("gpu_ms", "gpu_millis", "gpu_time_ms", "native_gpu_ms", "stage_gpu_ms");
    }

    public String compactTimingBoundaryLine() {
        return this.stageDisplayName() + " timing " + this.stageTimingStatusLine();
    }

    public String temporalHistoryStatusLine() {
        String accepted = firstDetailOrFallback(
                "?",
                "history_accepted",
                "history_accept_count",
                "temporal_history_accepted",
                "last_history_accepted"
        );
        String rejected = firstDetailOrFallback(
                "?",
                "history_rejected",
                "history_reject_count",
                "temporal_history_rejected",
                "last_history_rejected"
        );
        String confidence = firstDetailOrFallback(
                "?",
                "history_confidence",
                "avg_history_confidence",
                "temporal_confidence",
                "last_history_confidence"
        );
        String marker = firstDetailOrFallback(
                "pending",
                "temporal_history_marker",
                "last_temporal_history_marker",
                "history_marker",
                "temporal_marker"
        );
        return this.stageDisplayName()
                + " history accepted=" + accepted
                + " rejected=" + rejected
                + " confidence=" + confidence
                + " marker=" + shorten(marker, 32);
    }

    public String proofBoundaryLine() {
        return this.stageDisplayName()
                + " proof placeholder=" + booleanOrUnknown(this.placeholder)
                + " metadataOnly=" + booleanOrUnknown(this.metadataOnly)
                + " cpuOutput=" + booleanOrUnknown(this.cpuOutputGenerated)
                + " realGpuOutput=" + realGpuOutputBoundaryLabel()
                + " reason=" + shorten(this.readinessReason, 48);
    }

    public String physicalGiTracingEvidenceLine() {
        return this.stageDisplayName()
                + " physicalGI sceneLinked=" + booleanOrUnknown(this.physicalSceneLinked)
                + " surfaceContribution=" + booleanOrUnknown(this.physicalSurfaceContribution)
                + " sceneScore=" + valueOrUnknown(this.physicalSceneLinkScore)
                + " physicalChecksum=" + valueOrUnknown(this.physicalOutputChecksum)
                + " previewFallback=" + booleanOrUnknown(this.previewFallbackContribution)
                + " rejected(metadata/focus/proofMarker/directSubstitution/washout)="
                + booleanOrUnknown(this.metadataOnlyProofRejected)
                + "/" + booleanOrUnknown(this.focusWindowCaptureRejected)
                + "/" + booleanOrUnknown(this.proofMarkerEvidenceRejected)
                + "/" + booleanOrUnknown(this.temporaryDirectSubstitutionRejected)
                + "/" + booleanOrUnknown(this.rectangularWashoutRejected)
                + " marker=" + shorten(valueOrUnknown(this.physicalSceneMarker), 48)
                + " outputMarker=" + shorten(valueOrUnknown(this.physicalOutputMarker), 48)
                + " boundary=" + shorten(valueOrUnknown(this.proofBoundaryMarker), 64);
    }

    public boolean gBufferDepthSamplingEvidenceReady() {
        return genericGBufferDepthSamplingEvidenceReady() || depthSamplingPassOutputsReady();
    }

    public boolean javaDepthSamplingEvidenceReady() {
        return depthSamplingEvidenceReady(
                new String[]{
                        "java_g_buffer_depth_sampling_evidence",
                        "java_gbuffer_depth_sampling_evidence",
                        "javaDepthSamplingEvidence",
                        "javaGBufferDepthSamplingEvidence",
                        "java_depth_sampling_evidence",
                        "java_true_depth_sampling",
                        "java_depth_texture_sampled",
                        "java_depth_buffer_sampled",
                        "java_g_buffer_depth_sampled",
                        "java_gbuffer_depth_sampled",
                        "java_gbuffer_sampled"
                },
                new String[]{
                        "java_g_buffer_depth_metadata_only",
                        "java_gbuffer_depth_metadata_only",
                        "javaDepthMetadataOnly",
                        "javaGBufferDepthMetadataOnly",
                        "java_depth_sampling_metadata_only",
                        "java_depth_texture_metadata_only"
                },
                new String[]{
                        "java_g_buffer_depth_sample_count",
                        "java_gbuffer_depth_sample_count",
                        "javaDepthSampleCount",
                        "javaGBufferDepthSampleCount",
                        "java_depth_sample_count",
                        "java_depth_samples"
                }
        );
    }

    public boolean nativeDepthSamplingEvidenceReady() {
        return depthSamplingEvidenceReady(
                new String[]{
                        "native_g_buffer_depth_sampling_evidence",
                        "native_gbuffer_depth_sampling_evidence",
                        "nativeDepthSamplingEvidence",
                        "nativeGBufferDepthSamplingEvidence",
                        "native_depth_sampling_evidence",
                        "native_true_depth_sampling",
                        "native_depth_texture_sampled",
                        "native_depth_buffer_sampled",
                        "native_g_buffer_depth_sampled",
                        "native_gbuffer_depth_sampled",
                        "native_gbuffer_sampled"
                },
                new String[]{
                        "native_g_buffer_depth_metadata_only",
                        "native_gbuffer_depth_metadata_only",
                        "nativeDepthMetadataOnly",
                        "nativeGBufferDepthMetadataOnly",
                        "native_depth_sampling_metadata_only",
                        "native_depth_texture_metadata_only"
                },
                new String[]{
                        "native_g_buffer_depth_sample_count",
                        "native_gbuffer_depth_sample_count",
                        "nativeDepthSampleCount",
                        "nativeGBufferDepthSampleCount",
                        "native_depth_sample_count",
                        "native_depth_samples"
                }
        );
    }

    public boolean shaderPassDepthSamplingEvidenceReady() {
        return depthSamplingEvidenceReady(
                new String[]{
                        "shader_pass_g_buffer_depth_sampling_evidence",
                        "shader_pass_gbuffer_depth_sampling_evidence",
                        "shaderPassDepthSamplingEvidence",
                        "shaderPassGBufferDepthSamplingEvidence",
                        "shaderDepthSamplingEvidence",
                        "shader_pass_depth_sampling_evidence",
                        "shader_depth_sampling_evidence",
                        "shader_true_depth_sampling",
                        "shader_pass_depth_texture_sampled",
                        "shader_depth_texture_sampled",
                        "shader_pass_depth_buffer_sampled",
                        "shader_depth_buffer_sampled",
                        "shader_pass_g_buffer_depth_sampled",
                        "shader_g_buffer_depth_sampled",
                        "shader_pass_gbuffer_depth_sampled",
                        "shader_gbuffer_depth_sampled",
                        "shader_pass_gbuffer_sampled",
                        "shader_gbuffer_sampled"
                },
                new String[]{
                        "shader_pass_g_buffer_depth_metadata_only",
                        "shader_pass_gbuffer_depth_metadata_only",
                        "shaderPassDepthMetadataOnly",
                        "shaderPassGBufferDepthMetadataOnly",
                        "shaderDepthMetadataOnly",
                        "shader_pass_depth_sampling_metadata_only",
                        "shader_depth_sampling_metadata_only",
                        "shader_pass_depth_texture_metadata_only",
                        "shader_depth_texture_metadata_only"
                },
                new String[]{
                        "shader_pass_g_buffer_depth_sample_count",
                        "shader_pass_gbuffer_depth_sample_count",
                        "shaderPassDepthSampleCount",
                        "shaderPassGBufferDepthSampleCount",
                        "shaderDepthSampleCount",
                        "shader_g_buffer_depth_sample_count",
                        "shader_gbuffer_depth_sample_count",
                        "shader_pass_depth_sample_count",
                        "shader_depth_sample_count",
                        "shader_pass_depth_samples",
                        "shader_depth_samples"
                }
        );
    }

    public boolean depthSamplingPassOutputsReady() {
        return javaDepthSamplingEvidenceReady()
                || nativeDepthSamplingEvidenceReady()
                || shaderPassDepthSamplingEvidenceReady();
    }

    public long maxGBufferDepthSampleCount() {
        long max = this.gBufferDepthSampleCount == null ? 0L : Math.max(0L, this.gBufferDepthSampleCount);
        max = Math.max(max, positiveDetailLong(
                "java_g_buffer_depth_sample_count",
                "java_gbuffer_depth_sample_count",
                "javaDepthSampleCount",
                "javaGBufferDepthSampleCount",
                "java_depth_sample_count",
                "java_depth_samples"
        ));
        max = Math.max(max, positiveDetailLong(
                "native_g_buffer_depth_sample_count",
                "native_gbuffer_depth_sample_count",
                "nativeDepthSampleCount",
                "nativeGBufferDepthSampleCount",
                "native_depth_sample_count",
                "native_depth_samples"
        ));
        max = Math.max(max, positiveDetailLong(
                "shader_pass_g_buffer_depth_sample_count",
                "shader_pass_gbuffer_depth_sample_count",
                "shaderPassDepthSampleCount",
                "shaderPassGBufferDepthSampleCount",
                "shaderDepthSampleCount",
                "shader_g_buffer_depth_sample_count",
                "shader_gbuffer_depth_sample_count",
                "shader_pass_depth_sample_count",
                "shader_depth_sample_count",
                "shader_pass_depth_samples",
                "shader_depth_samples"
        ));
        return max;
    }

    public String depthSamplingEvidenceSources() {
        StringBuilder sources = new StringBuilder();
        appendSource(sources, "java", javaDepthSamplingEvidenceReady());
        appendSource(sources, "native", nativeDepthSamplingEvidenceReady());
        appendSource(sources, "shader", shaderPassDepthSamplingEvidenceReady());
        appendSource(sources, "generic", genericGBufferDepthSamplingEvidenceReady() && sources.length() == 0);
        return sources.length() == 0 ? "none" : sources.toString();
    }

    public boolean physicalGiEvidenceReady() {
        boolean sceneLinked = Boolean.TRUE.equals(this.physicalSceneLinked)
                || (this.physicalSceneLinkScore != null && this.physicalSceneLinkScore > 0L);
        boolean surfaceContribution = Boolean.TRUE.equals(this.physicalSurfaceContribution);
        boolean outputReady = Boolean.TRUE.equals(this.finalPhysicalCompositeReady)
                || (this.physicalOutputChecksum != null && this.physicalOutputChecksum > 0L);
        return sceneLinked
                && surfaceContribution
                && outputReady
                && !Boolean.TRUE.equals(this.previewFallbackContribution)
                && !Boolean.TRUE.equals(this.metadataOnly);
    }

    private boolean genericGBufferDepthSamplingEvidenceReady() {
        return (Boolean.TRUE.equals(this.gBufferDepthSamplingEvidence)
                || Boolean.TRUE.equals(this.gBufferDepthTextureSampled)
                || (this.gBufferDepthSampleCount != null && this.gBufferDepthSampleCount > 0L))
                && !Boolean.TRUE.equals(this.gBufferDepthMetadataOnly)
                && !Boolean.TRUE.equals(this.metadataOnly);
    }

    public boolean realShadowMapEvidenceReady() {
        return (Boolean.TRUE.equals(this.realShadowMapEvidence)
                || (Boolean.TRUE.equals(this.shadowMapRendered) && Boolean.TRUE.equals(this.shadowMapSampled)))
                && !Boolean.TRUE.equals(this.shadowMapMetadataOnly);
    }

    public boolean nativeShadowMapMaskReady() {
        return Boolean.TRUE.equals(this.nativeShadowMapMask)
                && !Boolean.TRUE.equals(this.shadowMapMetadataOnly);
    }

    public boolean shadowMapOutputConsumedReady() {
        return (Boolean.TRUE.equals(this.shadowMapOutputConsumed)
                || Boolean.TRUE.equals(this.realShadowMapComposite))
                && this.nativeShadowMapMaskReady()
                && this.shadowMapCompositeNoOverclaimBoundary();
    }

    public boolean realShadowMapCompositeReady() {
        return Boolean.TRUE.equals(this.realShadowMapComposite)
                && this.realShadowMapEvidenceReady()
                && this.shadowMapOutputConsumedReady();
    }

    public boolean shadowMapCompositeNoOverclaimBoundary() {
        return !Boolean.TRUE.equals(this.screenSpaceShadowDecal)
                && !Boolean.TRUE.equals(this.lowResDirectTextureShadowProof)
                && !Boolean.TRUE.equals(this.shadowMapMetadataOnly)
                && !Boolean.TRUE.equals(this.metadataOnly);
    }

    public boolean voxelRayTracedLightingConsumedEvidenceReady() {
        return (Boolean.TRUE.equals(this.voxelRayTracedLightingConsumedEvidence)
                || Boolean.TRUE.equals(this.realTracedLightingConsumed)
                || (Boolean.TRUE.equals(this.realGpuTraversalExecuted) && Boolean.TRUE.equals(this.realTracedLightingConsumed)))
                && !Boolean.TRUE.equals(this.tracedLightingMetadataOnly);
    }

    public boolean shaderGeneratedDenoiseOutputEvidenceReady() {
        return (Boolean.TRUE.equals(this.shaderGeneratedDenoiseOutputEvidence)
                || Boolean.TRUE.equals(this.shaderGeneratedOutput))
                && Boolean.TRUE.equals(this.realShaderDenoiseOutputReady)
                && !Boolean.TRUE.equals(this.cpuReadbackFallback)
                && !Boolean.TRUE.equals(this.shaderOutputImageCandidateCpuStaged)
                && !Boolean.TRUE.equals(this.shaderOutputImageCandidateNonGpu);
    }

    public String advancedLightingEvidenceLine() {
        return this.stageDisplayName()
                + " advancedLighting gBufferDepthEvidence=" + booleanOrUnknown(this.gBufferDepthSamplingEvidence)
                + " gBufferDepthTextureSampled=" + booleanOrUnknown(this.gBufferDepthTextureSampled)
                + " gBufferDepthSamples=" + valueOrUnknown(this.gBufferDepthSampleCount)
                + " gBufferDepthMetadataOnly=" + booleanOrUnknown(this.gBufferDepthMetadataOnly)
                + " javaDepthSamplingEvidence=" + javaDepthSamplingEvidenceReady()
                + " nativeDepthSamplingEvidence=" + nativeDepthSamplingEvidenceReady()
                + " shaderPassDepthSamplingEvidence=" + shaderPassDepthSamplingEvidenceReady()
                + " depthSamplingEvidenceSources=" + depthSamplingEvidenceSources()
                + " gBufferDepthReady=" + this.gBufferDepthSamplingEvidenceReady()
                + " shadowMapEvidence=" + booleanOrUnknown(this.realShadowMapEvidence)
                + " shadowMapRendered=" + booleanOrUnknown(this.shadowMapRendered)
                + " shadowMapSampled=" + booleanOrUnknown(this.shadowMapSampled)
                + " shadowMapMetadataOnly=" + booleanOrUnknown(this.shadowMapMetadataOnly)
                + " shadowMapReady=" + this.realShadowMapEvidenceReady()
                + " nativeShadowMapMask=" + booleanOrUnknown(this.nativeShadowMapMask)
                + " shadowMapOutputConsumed=" + booleanOrUnknown(this.shadowMapOutputConsumed)
                + " realShadowMapComposite=" + booleanOrUnknown(this.realShadowMapComposite)
                + " shadowMapCompositeNoOverclaim=" + this.shadowMapCompositeNoOverclaimBoundary()
                + " screenSpaceShadowDecal=" + booleanOrUnknown(this.screenSpaceShadowDecal)
                + " lowResDirectTextureShadowProof=" + booleanOrUnknown(this.lowResDirectTextureShadowProof)
                + " tracedLightingEvidence=" + booleanOrUnknown(this.voxelRayTracedLightingConsumedEvidence)
                + " realTracedLightingConsumed=" + booleanOrUnknown(this.realTracedLightingConsumed)
                + " realGpuTraversalExecuted=" + booleanOrUnknown(this.realGpuTraversalExecuted)
                + " tracedLightingMetadataOnly=" + booleanOrUnknown(this.tracedLightingMetadataOnly)
                + " tracedLightingReady=" + this.voxelRayTracedLightingConsumedEvidenceReady()
                + " shaderGeneratedDenoiseEvidence=" + booleanOrUnknown(this.shaderGeneratedDenoiseOutputEvidence)
                + " shaderGeneratedDenoiseReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " cpuReadbackFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " metadataOnly=" + booleanOrUnknown(this.metadataOnly)
                + " markers=" + shorten(valueOrUnknown(this.gBufferDepthSamplingMarker), 32)
                + "/" + shorten(valueOrUnknown(this.shadowMapEvidenceMarker), 32)
                + "/" + shorten(valueOrUnknown(this.voxelRayTracedLightingMarker), 32)
                + "/" + shorten(valueOrUnknown(this.shaderGeneratedDenoiseOutputMarker), 32);
    }

    public String denoiseReadinessStatusLine() {
        return this.stageDisplayName()
                + " denoise rawSource=" + booleanOrUnknown(this.rawSourceReady)
                + " cpuReady=" + booleanOrUnknown(this.cpuDenoiseReady)
                + " shaderIntent=" + booleanOrUnknown(this.shaderDenoiseIntended)
                + " shaderOutput=" + booleanOrUnknown(this.shaderOutputReady)
                + " shaderGenerated=" + booleanOrUnknown(this.shaderGeneratedOutput)
                + " shaderGeneratedEvidence=" + booleanOrUnknown(this.shaderGeneratedDenoiseOutputEvidence)
                + " shaderGeneratedEvidenceReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " publicMojangVisual=" + publicMojangShaderVisualOutputLabel()
                + " realShaderOutput=" + booleanOrUnknown(this.realShaderDenoiseOutputReady)
                + " cpuFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " edgeRejects=" + valueOrUnknown(this.edgeRejectionCount)
                + " historyRejects=" + valueOrUnknown(this.historyRejectionCount)
                + " source=" + shorten(this.sourceIdentity, 40);
    }

    public String shaderDenoiseStateStatusLine() {
        return this.stageDisplayName()
                + " shader denoise dispatchPrepared=" + booleanOrUnknown(this.shaderDispatchPrepared)
                + " outputImage=" + booleanOrUnknown(this.shaderOutputImageReady)
                + " outputMaterial=" + booleanOrUnknown(this.shaderOutputMaterialReady)
                + " generatedOutput=" + booleanOrUnknown(this.shaderGeneratedOutput)
                + " publicMojangVisual=" + publicMojangShaderVisualOutputLabel()
                + " realShaderOutput=" + booleanOrUnknown(this.realShaderDenoiseOutputReady)
                + " cpuReadbackFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " readiness=" + valueOrUnknown(this.shaderOutputReadinessLabel)
                + " blocker=" + shorten(this.shaderOutputBlockerReason, 56)
                + " blockers=" + shorten(this.shaderDenoiseBlockers, 56);
    }

    public String shaderDenoiseOutputBoundaryLine() {
        boolean realShaderOutputReady = Boolean.TRUE.equals(this.realShaderDenoiseOutputReady);
        String blocker = this.shaderDenoiseBlockers.isBlank()
                ? "unreported"
                : shorten(this.shaderDenoiseBlockers, 64);
        if (!this.shaderOutputBlockerReason.isBlank()) {
            blocker = shorten(this.shaderOutputBlockerReason, 64);
        }
        String source = this.sourceIdentity.isBlank()
                ? "unreported"
                : shorten(this.sourceIdentity, 48);
        return this.stageDisplayName()
                + " shader denoise output realShaderOutputReady=" + realShaderOutputReady
                + " outputReady=" + booleanOrUnknown(this.shaderOutputReady)
                + " generatedOutput=" + booleanOrUnknown(this.shaderGeneratedOutput)
                + " generatedOutputEvidence=" + booleanOrUnknown(this.shaderGeneratedDenoiseOutputEvidence)
                + " generatedOutputEvidenceReady=" + this.shaderGeneratedDenoiseOutputEvidenceReady()
                + " generatedOutputMarker=" + valueOrUnknown(this.shaderGeneratedDenoiseOutputMarker)
                + " outputImage=" + booleanOrUnknown(this.shaderOutputImageReady)
                + " outputMaterial=" + booleanOrUnknown(this.shaderOutputMaterialReady)
                + " publicMojangVisualAttempted=" + booleanOrUnknown(this.publicMojangShaderVisualOutputAttempted)
                + " publicMojangVisualSubmitted=" + booleanOrUnknown(this.publicMojangShaderVisualOutputSubmitted)
                + " publicMojangVisualReady=" + booleanOrUnknown(this.publicMojangShaderVisualOutputReady)
                + " cpuReadbackFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " candidateOnly=" + booleanOrUnknown(this.shaderOutputImageCandidateReady)
                + " candidateCpuStaged=" + booleanOrUnknown(this.shaderOutputImageCandidateCpuStaged)
                + " candidateNonGpu=" + booleanOrUnknown(this.shaderOutputImageCandidateNonGpu)
                + " candidateSize=" + valueOrUnknown(this.shaderOutputImageCandidateDimensions)
                + " candidateChecksum=" + valueOrUnknown(this.shaderOutputImageCandidateChecksum)
                + " readiness=" + valueOrUnknown(this.shaderOutputReadinessLabel)
                + " sourceIdentity=" + source
                + " blockers=" + blocker
                + " noOverclaim=" + Boolean.toString(!realShaderOutputReady)
                + " boundary=\"" + shaderDenoiseOutputBoundaryText(realShaderOutputReady) + "\"";
    }

    public String denoiseEvidenceBoundaryLine() {
        String boundary = this.evidenceBoundary.isBlank()
                ? "unreported"
                : shorten(this.evidenceBoundary, 72);
        return this.stageDisplayName()
                + " denoise evidence metadataOnly=" + booleanOrUnknown(this.metadataOnly)
                + " source=" + shorten(this.sourceIdentity, 40)
                + " boundary=" + boundary;
    }

    public String temporalFlickerEvidenceLine() {
        return this.stageDisplayName()
                + " temporal ready=" + booleanOrUnknown(this.temporalReady)
                + " stable=" + valueOrUnknown(this.temporalStablePixelCount)
                + " unstable=" + valueOrUnknown(this.temporalUnstablePixelCount)
                + " frameDeltaPixels=" + valueOrUnknown(this.frameDeltaPixelCount)
                + " meanDelta=" + valueOrUnknown(this.frameDeltaMeanDelta)
                + " prevChecksum=" + valueOrUnknown(this.previousOutputChecksum)
                + " currentChecksum=" + valueOrUnknown(this.currentOutputChecksum)
                + " historyConfidence=" + valueOrUnknown(this.historyConfidence)
                + " flickerScore=" + valueOrUnknown(this.flickerScore)
                + " ghostingRisk=" + shorten(valueOrUnknown(this.ghostingRisk), 40)
                + " marker=" + shorten(valueOrUnknown(this.temporalReadinessMarker), 40);
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "lighting.dispatch.stage." + sanitizeKey(this.stageId));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".present", "true");
        fields.put(normalizedPrefix + ".summary", this.compactLabel());
        fields.put(normalizedPrefix + ".statusLine", this.compactStageStatusLine());
        fields.put(normalizedPrefix + ".workStatus", this.stageWorkStatusLine());
        fields.put(normalizedPrefix + ".timingBoundary", this.explicitMeasurementBoundaryLine());
        fields.put(normalizedPrefix + ".compactTimingBoundary", this.compactTimingBoundaryLine());
        fields.put(normalizedPrefix + ".temporalHistory", this.temporalHistoryStatusLine());
        fields.put(normalizedPrefix + ".proofBoundary", this.proofBoundaryLine());
        if (isDenoiseLikeStage() || hasAnyDenoiseEvidence()) {
            fields.put(normalizedPrefix + ".denoiseReadiness", this.denoiseReadinessStatusLine());
            fields.put(normalizedPrefix + ".shaderDenoiseState", this.shaderDenoiseStateStatusLine());
            fields.put(normalizedPrefix + ".shaderDenoiseOutputBoundary", this.shaderDenoiseOutputBoundaryLine());
            fields.put(normalizedPrefix + ".denoiseEvidenceBoundary", this.denoiseEvidenceBoundaryLine());
            fields.put(normalizedPrefix + ".temporalFlickerEvidence", this.temporalFlickerEvidenceLine());
        }
        if (isPhysicalGiLikeStage() || hasAnyPhysicalGiEvidence()) {
            fields.put(normalizedPrefix + ".physicalGiTracingEvidence", this.physicalGiTracingEvidenceLine());
        }
        if (hasAnyAdvancedLightingEvidence()) {
            fields.put(normalizedPrefix + ".advancedLightingEvidence", this.advancedLightingEvidenceLine());
        }
        if (this.enabled != null) {
            fields.put(normalizedPrefix + ".enabled", Boolean.toString(this.enabled));
        }
        if (this.generation != null) {
            fields.put(normalizedPrefix + ".generation", Long.toString(this.generation));
        }
        if (!this.dispatchGroups.isBlank()) {
            fields.put(normalizedPrefix + ".dispatchGroups", this.dispatchGroups);
        }
        if (!this.dimensions.isBlank()) {
            fields.put(normalizedPrefix + ".dimensions", this.dimensions);
        }
        if (!this.ioCounts.isBlank()) {
            fields.put(normalizedPrefix + ".ioCounts", this.ioCounts);
        }
        if (this.sampleCount != null) {
            fields.put(normalizedPrefix + ".samples", Long.toString(this.sampleCount));
        }
        if (this.candidateCount != null) {
            fields.put(normalizedPrefix + ".candidates", Long.toString(this.candidateCount));
        }
        if (this.rayCount != null) {
            fields.put(normalizedPrefix + ".rays", Long.toString(this.rayCount));
        }
        if (this.cacheReadCount != null || this.cacheWriteCount != null) {
            fields.put(normalizedPrefix + ".cacheCounts", cacheLabel());
        }
        if (this.cacheReadCount != null) {
            fields.put(normalizedPrefix + ".cacheReads", Long.toString(this.cacheReadCount));
        }
        if (this.cacheWriteCount != null) {
            fields.put(normalizedPrefix + ".cacheWrites", Long.toString(this.cacheWriteCount));
        }
        if (this.flags != null) {
            fields.put(normalizedPrefix + ".flags", Long.toString(this.flags));
        }
        if (this.placeholder != null) {
            fields.put(normalizedPrefix + ".placeholder", Boolean.toString(this.placeholder));
        }
        if (this.validated != null) {
            fields.put(normalizedPrefix + ".validated", Boolean.toString(this.validated));
        }
        if (this.debugOverlay != null) {
            fields.put(normalizedPrefix + ".debugOverlay", Boolean.toString(this.debugOverlay));
        }
        if (this.readyForNativeExecution != null) {
            fields.put(normalizedPrefix + ".readyForNativeExecution", Boolean.toString(this.readyForNativeExecution));
        }
        if (!this.readinessReason.isBlank()) {
            fields.put(normalizedPrefix + ".readinessReason", this.readinessReason);
        }
        if (this.frameIndex != null) {
            fields.put(normalizedPrefix + ".frameIndex", Long.toString(this.frameIndex));
        }
        if (this.recordedThisFrame != null) {
            fields.put(normalizedPrefix + ".recordedThisFrame", Boolean.toString(this.recordedThisFrame));
        }
        if (this.payloadAccepted != null) {
            fields.put(normalizedPrefix + ".payloadAccepted", Boolean.toString(this.payloadAccepted));
        }
        if (this.payloadGeneration != null) {
            fields.put(normalizedPrefix + ".payloadGeneration", Long.toString(this.payloadGeneration));
        }
        if (!this.payloadGenerationRange.isBlank()) {
            fields.put(normalizedPrefix + ".payloadGenerationRange", this.payloadGenerationRange);
        }
        if (this.payloadFrameIndex != null) {
            fields.put(normalizedPrefix + ".payloadFrameIndex", Long.toString(this.payloadFrameIndex));
        }
        if (this.celestialCount != null) {
            fields.put(normalizedPrefix + ".celestialCount", Long.toString(this.celestialCount));
        }
        if (this.emissiveCount != null) {
            fields.put(normalizedPrefix + ".emissiveCount", Long.toString(this.emissiveCount));
        }
        if (this.shadowCandidateCount != null) {
            fields.put(normalizedPrefix + ".shadowCandidateCount", Long.toString(this.shadowCandidateCount));
        }
        if (this.budgetedShadowCandidateCount != null) {
            fields.put(normalizedPrefix + ".budgetedShadowCandidateCount", Long.toString(this.budgetedShadowCandidateCount));
        }
        if (this.sectionSnapshotCount != null) {
            fields.put(normalizedPrefix + ".sectionSnapshotCount", Long.toString(this.sectionSnapshotCount));
        }
        if (this.metadataOnly != null) {
            fields.put(normalizedPrefix + ".metadataOnly", Boolean.toString(this.metadataOnly));
        }
        if (this.cpuOutputGenerated != null) {
            fields.put(normalizedPrefix + ".cpuOutputGenerated", Boolean.toString(this.cpuOutputGenerated));
        }
        if (!this.outputDimensions.isBlank()) {
            fields.put(normalizedPrefix + ".outputDimensions", this.outputDimensions);
        }
        if (this.outputPixelCount != null) {
            fields.put(normalizedPrefix + ".outputPixelCount", Long.toString(this.outputPixelCount));
        }
        if (!this.outputEnergy.isBlank()) {
            fields.put(normalizedPrefix + ".outputEnergy", this.outputEnergy);
        }
        if (this.outputChecksum != null) {
            fields.put(normalizedPrefix + ".outputChecksum", Long.toString(this.outputChecksum));
        }
        if (this.payloadValidated != null) {
            fields.put(normalizedPrefix + ".payloadValidated", Boolean.toString(this.payloadValidated));
        }
        if (this.payloadHasDirectWork != null) {
            fields.put(normalizedPrefix + ".payloadHasDirectWork", Boolean.toString(this.payloadHasDirectWork));
        }
        if (this.payloadReadyForShadowTracing != null) {
            fields.put(normalizedPrefix + ".payloadReadyForShadowTracing", Boolean.toString(this.payloadReadyForShadowTracing));
        }
        if (this.rawSourceReady != null) {
            fields.put(normalizedPrefix + ".rawSourceReady", Boolean.toString(this.rawSourceReady));
        }
        if (this.cpuDenoiseReady != null) {
            fields.put(normalizedPrefix + ".cpuDenoiseReady", Boolean.toString(this.cpuDenoiseReady));
        }
        if (this.shaderDenoiseIntended != null) {
            fields.put(normalizedPrefix + ".shaderDenoiseIntended", Boolean.toString(this.shaderDenoiseIntended));
        }
        if (this.shaderOutputReady != null) {
            fields.put(normalizedPrefix + ".shaderOutputReady", Boolean.toString(this.shaderOutputReady));
        }
        if (this.shaderDispatchPrepared != null) {
            fields.put(normalizedPrefix + ".shaderDispatchPrepared", Boolean.toString(this.shaderDispatchPrepared));
        }
        if (this.shaderOutputImageReady != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageReady", Boolean.toString(this.shaderOutputImageReady));
        }
        if (this.shaderOutputMaterialReady != null) {
            fields.put(normalizedPrefix + ".shaderOutputMaterialReady", Boolean.toString(this.shaderOutputMaterialReady));
        }
        if (this.shaderGeneratedOutput != null) {
            fields.put(normalizedPrefix + ".shaderGeneratedOutput", Boolean.toString(this.shaderGeneratedOutput));
        }
        if (this.shaderGeneratedDenoiseOutputEvidence != null) {
            fields.put(
                    normalizedPrefix + ".shaderGeneratedDenoiseOutputEvidence",
                    Boolean.toString(this.shaderGeneratedDenoiseOutputEvidence)
            );
            fields.put(
                    normalizedPrefix + ".shaderGeneratedDenoiseOutputEvidenceReady",
                    Boolean.toString(this.shaderGeneratedDenoiseOutputEvidenceReady())
            );
        }
        if (this.publicMojangShaderVisualOutputAttempted != null) {
            fields.put(
                    normalizedPrefix + ".publicMojangShaderVisualOutputAttempted",
                    Boolean.toString(this.publicMojangShaderVisualOutputAttempted)
            );
        }
        if (this.publicMojangShaderVisualOutputSubmitted != null) {
            fields.put(
                    normalizedPrefix + ".publicMojangShaderVisualOutputSubmitted",
                    Boolean.toString(this.publicMojangShaderVisualOutputSubmitted)
            );
        }
        if (this.publicMojangShaderVisualOutputReady != null) {
            fields.put(
                    normalizedPrefix + ".publicMojangShaderVisualOutputReady",
                    Boolean.toString(this.publicMojangShaderVisualOutputReady)
            );
        }
        if (hasPublicMojangShaderVisualOutputState()) {
            fields.put(normalizedPrefix + ".publicMojangShaderVisualOutputBoundary", publicMojangShaderVisualOutputBoundaryText());
        }
        if (this.cpuReadbackFallback != null) {
            fields.put(normalizedPrefix + ".cpuReadbackFallback", Boolean.toString(this.cpuReadbackFallback));
        }
        if (this.realShaderDenoiseOutput != null) {
            fields.put(normalizedPrefix + ".realShaderDenoiseOutput", Boolean.toString(this.realShaderDenoiseOutput));
        }
        if (this.realShaderDenoiseOutputReady != null) {
            fields.put(normalizedPrefix + ".realShaderDenoiseOutputReady", Boolean.toString(this.realShaderDenoiseOutputReady));
            fields.put(normalizedPrefix + ".shaderDenoiseNoOverclaim", Boolean.toString(!this.realShaderDenoiseOutputReady));
        }
        if (this.shaderOutputImageCandidateReady != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateReady", Boolean.toString(this.shaderOutputImageCandidateReady));
        }
        if (this.shaderOutputImageCandidateCpuStaged != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateCpuStaged", Boolean.toString(this.shaderOutputImageCandidateCpuStaged));
        }
        if (this.shaderOutputImageCandidateNonGpu != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateNonGpu", Boolean.toString(this.shaderOutputImageCandidateNonGpu));
        }
        if (!this.shaderOutputImageCandidateDimensions.isBlank()) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateDimensions", this.shaderOutputImageCandidateDimensions);
        }
        if (this.shaderOutputImageCandidatePixels != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidatePixels", Long.toString(this.shaderOutputImageCandidatePixels));
        }
        if (this.shaderOutputImageCandidateBytes != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateBytes", Long.toString(this.shaderOutputImageCandidateBytes));
        }
        if (this.shaderOutputImageCandidateChecksum != null) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateChecksum", Long.toString(this.shaderOutputImageCandidateChecksum));
        }
        if (!this.shaderOutputImageCandidateMarker.isBlank()) {
            fields.put(normalizedPrefix + ".shaderOutputImageCandidateMarker", this.shaderOutputImageCandidateMarker);
        }
        if (!this.shaderOutputReadinessLabel.isBlank()) {
            fields.put(normalizedPrefix + ".shaderOutputReadinessLabel", this.shaderOutputReadinessLabel);
        }
        if (!this.shaderOutputBlockerReason.isBlank()) {
            fields.put(normalizedPrefix + ".shaderOutputBlockerReason", this.shaderOutputBlockerReason);
        }
        if (!this.shaderDenoiseBlockers.isBlank()) {
            fields.put(normalizedPrefix + ".shaderDenoiseBlockers", this.shaderDenoiseBlockers);
        }
        if (this.edgeRejectionCount != null) {
            fields.put(normalizedPrefix + ".edgeRejectionCount", Long.toString(this.edgeRejectionCount));
        }
        if (this.historyRejectionCount != null) {
            fields.put(normalizedPrefix + ".historyRejectionCount", Long.toString(this.historyRejectionCount));
        }
        if (this.temporalStablePixelCount != null) {
            fields.put(normalizedPrefix + ".temporalStablePixels", Long.toString(this.temporalStablePixelCount));
        }
        if (this.temporalUnstablePixelCount != null) {
            fields.put(normalizedPrefix + ".temporalUnstablePixels", Long.toString(this.temporalUnstablePixelCount));
        }
        if (this.frameDeltaPixelCount != null) {
            fields.put(normalizedPrefix + ".frameDeltaPixels", Long.toString(this.frameDeltaPixelCount));
        }
        if (!this.frameDeltaMeanDelta.isBlank()) {
            fields.put(normalizedPrefix + ".frameDeltaMeanDelta", this.frameDeltaMeanDelta);
        }
        if (this.previousOutputChecksum != null) {
            fields.put(normalizedPrefix + ".previousOutputChecksum", Long.toString(this.previousOutputChecksum));
        }
        if (this.currentOutputChecksum != null) {
            fields.put(normalizedPrefix + ".currentOutputChecksum", Long.toString(this.currentOutputChecksum));
        }
        if (!this.historyConfidence.isBlank()) {
            fields.put(normalizedPrefix + ".historyConfidence", this.historyConfidence);
        }
        if (!this.flickerScore.isBlank()) {
            fields.put(normalizedPrefix + ".flickerScore", this.flickerScore);
        }
        if (!this.ghostingRisk.isBlank()) {
            fields.put(normalizedPrefix + ".ghostingRisk", this.ghostingRisk);
        }
        if (this.temporalReady != null) {
            fields.put(normalizedPrefix + ".temporalReady", Boolean.toString(this.temporalReady));
        }
        if (!this.temporalReadinessMarker.isBlank()) {
            fields.put(normalizedPrefix + ".temporalReadinessMarker", this.temporalReadinessMarker);
        }
        if (!this.sourceIdentity.isBlank()) {
            fields.put(normalizedPrefix + ".sourceIdentity", this.sourceIdentity);
        }
        if (!this.evidenceBoundary.isBlank()) {
            fields.put(normalizedPrefix + ".evidenceBoundary", this.evidenceBoundary);
        }
        if (this.physicalSceneLinkScore != null) {
            fields.put(normalizedPrefix + ".physicalSceneLinkScore", Long.toString(this.physicalSceneLinkScore));
        }
        if (this.physicalOutputChecksum != null) {
            fields.put(normalizedPrefix + ".physicalOutputChecksum", Long.toString(this.physicalOutputChecksum));
        }
        if (this.physicalSceneLinked != null) {
            fields.put(normalizedPrefix + ".physicalSceneLinked", Boolean.toString(this.physicalSceneLinked));
        }
        if (this.physicalSurfaceContribution != null) {
            fields.put(normalizedPrefix + ".physicalSurfaceContribution", Boolean.toString(this.physicalSurfaceContribution));
        }
        if (this.gBufferDepthSamplingEvidence != null) {
            fields.put(normalizedPrefix + ".gBufferDepthSamplingEvidence", Boolean.toString(this.gBufferDepthSamplingEvidence));
            fields.put(
                    normalizedPrefix + ".gBufferDepthSamplingEvidenceReady",
                    Boolean.toString(this.gBufferDepthSamplingEvidenceReady())
            );
        }
        if (this.gBufferDepthTextureSampled != null) {
            fields.put(normalizedPrefix + ".gBufferDepthTextureSampled", Boolean.toString(this.gBufferDepthTextureSampled));
        }
        if (this.gBufferDepthMetadataOnly != null) {
            fields.put(normalizedPrefix + ".gBufferDepthMetadataOnly", Boolean.toString(this.gBufferDepthMetadataOnly));
        }
        if (this.gBufferDepthSampleCount != null) {
            fields.put(normalizedPrefix + ".gBufferDepthSampleCount", Long.toString(this.gBufferDepthSampleCount));
        }
        if (hasAnyDepthSamplingSourceDetail() || depthSamplingPassOutputsReady()) {
            fields.put(normalizedPrefix + ".javaDepthSamplingEvidence", Boolean.toString(javaDepthSamplingEvidenceReady()));
            fields.put(normalizedPrefix + ".nativeDepthSamplingEvidence", Boolean.toString(nativeDepthSamplingEvidenceReady()));
            fields.put(
                    normalizedPrefix + ".shaderPassDepthSamplingEvidence",
                    Boolean.toString(shaderPassDepthSamplingEvidenceReady())
            );
            fields.put(normalizedPrefix + ".depthSamplingPassOutputsReady", Boolean.toString(depthSamplingPassOutputsReady()));
            fields.put(normalizedPrefix + ".depthSamplingEvidenceSources", depthSamplingEvidenceSources());
            fields.put(normalizedPrefix + ".maxGBufferDepthSampleCount", Long.toString(maxGBufferDepthSampleCount()));
        }
        if (hasAnyPhysicalGiEvidence()) {
            fields.put(normalizedPrefix + ".physicalGiEvidenceReady", Boolean.toString(physicalGiEvidenceReady()));
        }
        if (this.realShadowMapEvidence != null) {
            fields.put(normalizedPrefix + ".realShadowMapEvidence", Boolean.toString(this.realShadowMapEvidence));
            fields.put(normalizedPrefix + ".realShadowMapEvidenceReady", Boolean.toString(this.realShadowMapEvidenceReady()));
        }
        if (this.shadowMapRendered != null) {
            fields.put(normalizedPrefix + ".shadowMapRendered", Boolean.toString(this.shadowMapRendered));
        }
        if (this.shadowMapSampled != null) {
            fields.put(normalizedPrefix + ".shadowMapSampled", Boolean.toString(this.shadowMapSampled));
        }
        if (this.shadowMapMetadataOnly != null) {
            fields.put(normalizedPrefix + ".shadowMapMetadataOnly", Boolean.toString(this.shadowMapMetadataOnly));
        }
        if (this.nativeShadowMapMask != null) {
            fields.put(normalizedPrefix + ".nativeShadowMapMask", Boolean.toString(this.nativeShadowMapMask));
            fields.put(normalizedPrefix + ".nativeShadowMapMaskReady", Boolean.toString(this.nativeShadowMapMaskReady()));
        }
        if (this.shadowMapOutputConsumed != null) {
            fields.put(normalizedPrefix + ".shadowMapOutputConsumed", Boolean.toString(this.shadowMapOutputConsumed));
            fields.put(
                    normalizedPrefix + ".shadowMapOutputConsumedReady",
                    Boolean.toString(this.shadowMapOutputConsumedReady())
            );
        }
        if (this.realShadowMapComposite != null) {
            fields.put(normalizedPrefix + ".realShadowMapComposite", Boolean.toString(this.realShadowMapComposite));
            fields.put(normalizedPrefix + ".realShadowMapCompositeReady", Boolean.toString(this.realShadowMapCompositeReady()));
        }
        if (this.screenSpaceShadowDecal != null) {
            fields.put(normalizedPrefix + ".screenSpaceShadowDecal", Boolean.toString(this.screenSpaceShadowDecal));
        }
        if (this.lowResDirectTextureShadowProof != null) {
            fields.put(
                    normalizedPrefix + ".lowResDirectTextureShadowProof",
                    Boolean.toString(this.lowResDirectTextureShadowProof)
            );
        }
        if (this.nativeShadowMapMask != null
                || this.shadowMapOutputConsumed != null
                || this.realShadowMapComposite != null
                || this.screenSpaceShadowDecal != null
                || this.lowResDirectTextureShadowProof != null) {
            fields.put(
                    normalizedPrefix + ".shadowMapCompositeNoOverclaim",
                    Boolean.toString(this.shadowMapCompositeNoOverclaimBoundary())
            );
        }
        if (this.voxelRayTracedLightingConsumedEvidence != null) {
            fields.put(
                    normalizedPrefix + ".voxelRayTracedLightingConsumedEvidence",
                    Boolean.toString(this.voxelRayTracedLightingConsumedEvidence)
            );
            fields.put(
                    normalizedPrefix + ".voxelRayTracedLightingConsumedEvidenceReady",
                    Boolean.toString(this.voxelRayTracedLightingConsumedEvidenceReady())
            );
        }
        if (this.realTracedLightingConsumed != null) {
            fields.put(normalizedPrefix + ".realTracedLightingConsumed", Boolean.toString(this.realTracedLightingConsumed));
        }
        if (this.realGpuTraversalExecuted != null) {
            fields.put(normalizedPrefix + ".realGpuTraversalExecuted", Boolean.toString(this.realGpuTraversalExecuted));
        }
        if (this.tracedLightingMetadataOnly != null) {
            fields.put(normalizedPrefix + ".tracedLightingMetadataOnly", Boolean.toString(this.tracedLightingMetadataOnly));
        }
        if (this.previewFallbackContribution != null) {
            fields.put(normalizedPrefix + ".previewFallbackContribution", Boolean.toString(this.previewFallbackContribution));
        }
        if (this.metadataOnlyProofRejected != null) {
            fields.put(normalizedPrefix + ".metadataOnlyProofRejected", Boolean.toString(this.metadataOnlyProofRejected));
        }
        if (this.focusWindowCaptureRejected != null) {
            fields.put(normalizedPrefix + ".focusWindowCaptureRejected", Boolean.toString(this.focusWindowCaptureRejected));
        }
        if (this.proofMarkerEvidenceRejected != null) {
            fields.put(normalizedPrefix + ".proofMarkerEvidenceRejected", Boolean.toString(this.proofMarkerEvidenceRejected));
        }
        if (this.temporaryDirectSubstitutionRejected != null) {
            fields.put(normalizedPrefix + ".temporaryDirectSubstitutionRejected", Boolean.toString(this.temporaryDirectSubstitutionRejected));
        }
        if (this.rectangularWashoutRejected != null) {
            fields.put(normalizedPrefix + ".rectangularWashoutRejected", Boolean.toString(this.rectangularWashoutRejected));
        }
        if (!this.physicalSceneMarker.isBlank()) {
            fields.put(normalizedPrefix + ".physicalSceneMarker", this.physicalSceneMarker);
        }
        if (!this.physicalOutputMarker.isBlank()) {
            fields.put(normalizedPrefix + ".physicalOutputMarker", this.physicalOutputMarker);
        }
        if (!this.gBufferDepthSamplingMarker.isBlank()) {
            fields.put(normalizedPrefix + ".gBufferDepthSamplingMarker", this.gBufferDepthSamplingMarker);
        }
        if (!this.shadowMapEvidenceMarker.isBlank()) {
            fields.put(normalizedPrefix + ".shadowMapEvidenceMarker", this.shadowMapEvidenceMarker);
        }
        if (!this.voxelRayTracedLightingMarker.isBlank()) {
            fields.put(normalizedPrefix + ".voxelRayTracedLightingMarker", this.voxelRayTracedLightingMarker);
        }
        if (!this.shaderGeneratedDenoiseOutputMarker.isBlank()) {
            fields.put(normalizedPrefix + ".shaderGeneratedDenoiseOutputMarker", this.shaderGeneratedDenoiseOutputMarker);
        }
        if (!this.proofBoundaryMarker.isBlank()) {
            fields.put(normalizedPrefix + ".proofBoundaryMarker", this.proofBoundaryMarker);
        }
        for (Map.Entry<String, String> entry : this.details.entrySet()) {
            fields.put(normalizedPrefix + ".raw." + sanitizeKey(entry.getKey()), entry.getValue());
        }
        return Collections.unmodifiableMap(fields);
    }

    private String stageDisplayName() {
        return switch (this.stageId) {
            case "direct_lighting" -> "Direct";
            case "diffuse_gi", "low_res_gi", "low_resolution_gi", "gi" -> "GI";
            case "denoise", "shader_denoise", "edge_aware_denoise", "diffuse_gi_denoise" -> "Denoise";
            case "composite", "final_composite" -> "Composite";
            case "cache", "radiance_cache", "sparse_radiance_cache", "sparse_voxel_radiance_cache" -> "Cache";
            case "gbuffer", "g_buffer", "gbuffer_depth", "g_buffer_depth", "depth_sampling" -> "GBufferDepth";
            case "shadow_map", "shadowmap" -> "ShadowMap";
            case "voxel_tracing", "voxel_ray_tracing", "ray_traced_lighting",
                    "traced_lighting", "hybrid_tracing" -> "TracedLighting";
            case "advanced_lighting" -> "AdvancedLighting";
            case "adaptive_sampling", "ray_budget", "variance", "history_confidence" -> "Adaptive";
            default -> this.stageId;
        };
    }

    private String outputStatusLabel() {
        if (Boolean.TRUE.equals(this.cpuOutputGenerated)) {
            return "CPU=" + outputEvidenceLabel();
        }
        if (Boolean.FALSE.equals(this.cpuOutputGenerated)) {
            return "CPU=not_generated";
        }
        return "CPU=unreported";
    }

    private String outputEvidenceLabel() {
        StringBuilder label = new StringBuilder("generated");
        if (!this.outputDimensions.isBlank()) {
            label.append(" size=").append(this.outputDimensions);
        }
        if (this.outputPixelCount != null) {
            label.append(" pixels=").append(this.outputPixelCount);
        }
        if (!this.outputEnergy.isBlank()) {
            label.append(" energy=").append(shorten(this.outputEnergy, 24));
        }
        if (this.outputChecksum != null) {
            label.append(" checksum=").append(this.outputChecksum);
        }
        return label.toString();
    }

    private String cpuTimingValueLabel() {
        return firstDetailOrFallback(
                "pending(no CPU stage scope)",
                "cpu_ms",
                "cpu_millis",
                "cpu_time_ms",
                "native_cpu_ms",
                "stage_cpu_ms"
        );
    }

    private String gpuTimingValueLabel() {
        return firstDetailOrFallback(
                "unavailable(native/Vulkan GPU timestamp not reported)",
                "gpu_ms",
                "gpu_millis",
                "gpu_time_ms",
                "native_gpu_ms",
                "stage_gpu_ms"
        );
    }

    private String realGpuOutputBoundaryLabel() {
        if (this.realShaderDenoiseOutputReady != null) {
            return Boolean.toString(this.realShaderDenoiseOutputReady);
        }
        if (this.shaderGeneratedOutput != null) {
            return Boolean.toString(this.shaderGeneratedOutput);
        }
        if (this.shaderOutputReady != null) {
            return Boolean.toString(this.shaderOutputReady);
        }
        String shaderOutput = firstDetailOrFallback(
                "",
                "real_shader_gi_output",
                "real_denoise_shader_output",
                "shader_output",
                "shader_gi_output",
                "shader_denoise_output",
                "gpu_output",
                "gpu_gi_output",
                "gpu_denoise_output"
        );
        if (!shaderOutput.isBlank()) {
            return shaderOutput;
        }
        return this.hasMeasuredGpuTiming() ? "timed_no_output_flag" : "unavailable";
    }

    private boolean hasPublicMojangShaderVisualOutputState() {
        return this.publicMojangShaderVisualOutputAttempted != null
                || this.publicMojangShaderVisualOutputSubmitted != null
                || this.publicMojangShaderVisualOutputReady != null;
    }

    private String publicMojangShaderVisualOutputLabel() {
        if (!hasPublicMojangShaderVisualOutputState()) {
            return "";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputReady)) {
            return "ready";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputSubmitted)) {
            return "submitted";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputAttempted)) {
            return "attempted";
        }
        return "reported_false";
    }

    private String publicMojangShaderVisualOutputBoundaryText() {
        if (!hasPublicMojangShaderVisualOutputState()) {
            return "public Mojang shader visual output not reported";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputReady)) {
            return "public Mojang shader visual output ready; not real compute/native shader denoise output";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputSubmitted)) {
            return "public Mojang shader visual output submitted; readiness not proven";
        }
        if (Boolean.TRUE.equals(this.publicMojangShaderVisualOutputAttempted)) {
            return "public Mojang shader visual output attempted; submission/readiness not proven";
        }
        return "public Mojang shader visual output explicitly false";
    }

    private boolean isDenoiseLikeStage() {
        return switch (this.stageId) {
            case "denoise", "shader_denoise", "edge_aware_denoise", "diffuse_gi_denoise" -> true;
            default -> false;
        };
    }

    private boolean isPhysicalGiLikeStage() {
        return switch (this.stageId) {
            case "diffuse_gi", "low_res_gi", "low_resolution_gi", "gi" -> true;
            default -> false;
        };
    }

    private boolean hasAnyDenoiseEvidence() {
        return this.rawSourceReady != null
                || this.cpuDenoiseReady != null
                || this.shaderDenoiseIntended != null
                || this.shaderOutputReady != null
                || this.shaderDispatchPrepared != null
                || this.shaderOutputImageReady != null
                || this.shaderOutputMaterialReady != null
                || this.shaderGeneratedOutput != null
                || this.shaderGeneratedDenoiseOutputEvidence != null
                || this.publicMojangShaderVisualOutputAttempted != null
                || this.publicMojangShaderVisualOutputSubmitted != null
                || this.publicMojangShaderVisualOutputReady != null
                || this.cpuReadbackFallback != null
                || this.realShaderDenoiseOutput != null
                || this.realShaderDenoiseOutputReady != null
                || this.shaderOutputImageCandidateReady != null
                || this.shaderOutputImageCandidateCpuStaged != null
                || this.shaderOutputImageCandidateNonGpu != null
                || !this.shaderOutputImageCandidateDimensions.isBlank()
                || this.shaderOutputImageCandidatePixels != null
                || this.shaderOutputImageCandidateBytes != null
                || this.shaderOutputImageCandidateChecksum != null
                || !this.shaderOutputImageCandidateMarker.isBlank()
                || !this.shaderOutputReadinessLabel.isBlank()
                || !this.shaderOutputBlockerReason.isBlank()
                || !this.shaderDenoiseBlockers.isBlank()
                || this.edgeRejectionCount != null
                || this.historyRejectionCount != null
                || this.temporalStablePixelCount != null
                || this.temporalUnstablePixelCount != null
                || this.frameDeltaPixelCount != null
                || !this.frameDeltaMeanDelta.isBlank()
                || this.previousOutputChecksum != null
                || this.currentOutputChecksum != null
                || !this.historyConfidence.isBlank()
                || !this.flickerScore.isBlank()
                || !this.ghostingRisk.isBlank()
                || this.temporalReady != null
                || !this.temporalReadinessMarker.isBlank()
                || !this.sourceIdentity.isBlank()
                || !this.evidenceBoundary.isBlank()
                || !this.shaderGeneratedDenoiseOutputMarker.isBlank();
    }

    private boolean hasAnyPhysicalGiEvidence() {
        return this.physicalSceneLinkScore != null
                || this.physicalOutputChecksum != null
                || this.physicalSceneLinked != null
                || this.physicalSurfaceContribution != null
                || this.previewFallbackContribution != null
                || this.metadataOnlyProofRejected != null
                || this.focusWindowCaptureRejected != null
                || this.proofMarkerEvidenceRejected != null
                || this.temporaryDirectSubstitutionRejected != null
                || this.rectangularWashoutRejected != null
                || !this.physicalSceneMarker.isBlank()
                || !this.physicalOutputMarker.isBlank()
                || !this.proofBoundaryMarker.isBlank();
    }

    private boolean hasAnyAdvancedLightingEvidence() {
        return this.gBufferDepthSamplingEvidence != null
                || this.gBufferDepthTextureSampled != null
                || this.gBufferDepthMetadataOnly != null
                || this.gBufferDepthSampleCount != null
                || hasAnyDepthSamplingSourceDetail()
                || this.realShadowMapEvidence != null
                || this.shadowMapRendered != null
                || this.shadowMapSampled != null
                || this.shadowMapMetadataOnly != null
                || this.nativeShadowMapMask != null
                || this.shadowMapOutputConsumed != null
                || this.realShadowMapComposite != null
                || this.screenSpaceShadowDecal != null
                || this.lowResDirectTextureShadowProof != null
                || this.voxelRayTracedLightingConsumedEvidence != null
                || this.realTracedLightingConsumed != null
                || this.realGpuTraversalExecuted != null
                || this.tracedLightingMetadataOnly != null
                || this.shaderGeneratedDenoiseOutputEvidence != null
                || !this.gBufferDepthSamplingMarker.isBlank()
                || !this.shadowMapEvidenceMarker.isBlank()
                || !this.voxelRayTracedLightingMarker.isBlank()
                || !this.shaderGeneratedDenoiseOutputMarker.isBlank();
    }

    private String firstDetailOrFallback(String fallback, String... keys) {
        if (keys != null) {
            for (String key : keys) {
                String value = this.details.get(normalizeFieldKey(key));
                if (value != null && !value.isBlank()) {
                    return stripQuotes(value);
                }
            }
        }
        return fallback;
    }

    private boolean hasAnyDetail(String... keys) {
        if (keys == null) {
            return false;
        }
        for (String key : keys) {
            String value = this.details.get(normalizeFieldKey(key));
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyDepthSamplingSourceDetail() {
        return hasAnyDetail(
                "java_g_buffer_depth_sampling_evidence",
                "java_gbuffer_depth_sampling_evidence",
                "java_depth_sampling_evidence",
                "java_true_depth_sampling",
                "java_depth_texture_sampled",
                "java_depth_buffer_sampled",
                "java_g_buffer_depth_sampled",
                "java_gbuffer_depth_sampled",
                "java_gbuffer_sampled",
                "java_g_buffer_depth_metadata_only",
                "java_gbuffer_depth_metadata_only",
                "java_depth_sampling_metadata_only",
                "java_depth_texture_metadata_only",
                "java_g_buffer_depth_sample_count",
                "java_gbuffer_depth_sample_count",
                "java_depth_sample_count",
                "java_depth_samples",
                "native_g_buffer_depth_sampling_evidence",
                "native_gbuffer_depth_sampling_evidence",
                "native_depth_sampling_evidence",
                "native_true_depth_sampling",
                "native_depth_texture_sampled",
                "native_depth_buffer_sampled",
                "native_g_buffer_depth_sampled",
                "native_gbuffer_depth_sampled",
                "native_gbuffer_sampled",
                "native_g_buffer_depth_metadata_only",
                "native_gbuffer_depth_metadata_only",
                "native_depth_sampling_metadata_only",
                "native_depth_texture_metadata_only",
                "native_g_buffer_depth_sample_count",
                "native_gbuffer_depth_sample_count",
                "native_depth_sample_count",
                "native_depth_samples",
                "shader_pass_g_buffer_depth_sampling_evidence",
                "shader_pass_gbuffer_depth_sampling_evidence",
                "shader_pass_depth_sampling_evidence",
                "shader_depth_sampling_evidence",
                "shader_true_depth_sampling",
                "shader_pass_depth_texture_sampled",
                "shader_depth_texture_sampled",
                "shader_pass_depth_buffer_sampled",
                "shader_depth_buffer_sampled",
                "shader_pass_g_buffer_depth_sampled",
                "shader_g_buffer_depth_sampled",
                "shader_pass_gbuffer_depth_sampled",
                "shader_gbuffer_depth_sampled",
                "shader_pass_gbuffer_sampled",
                "shader_gbuffer_sampled",
                "shader_pass_g_buffer_depth_metadata_only",
                "shader_pass_gbuffer_depth_metadata_only",
                "shader_pass_depth_sampling_metadata_only",
                "shader_depth_sampling_metadata_only",
                "shader_pass_depth_texture_metadata_only",
                "shader_depth_texture_metadata_only",
                "shader_pass_g_buffer_depth_sample_count",
                "shader_pass_gbuffer_depth_sample_count",
                "shader_g_buffer_depth_sample_count",
                "shader_gbuffer_depth_sample_count",
                "shader_pass_depth_sample_count",
                "shader_depth_sample_count",
                "shader_pass_depth_samples",
                "shader_depth_samples"
        );
    }

    private boolean depthSamplingEvidenceReady(String[] evidenceKeys, String[] metadataOnlyKeys, String[] sampleCountKeys) {
        Boolean evidence = detailBoolean(evidenceKeys);
        Boolean metadataOnly = detailBoolean(metadataOnlyKeys);
        return (Boolean.TRUE.equals(evidence) || positiveDetailLong(sampleCountKeys) > 0L)
                && !Boolean.TRUE.equals(metadataOnly)
                && !Boolean.TRUE.equals(this.gBufferDepthMetadataOnly)
                && !Boolean.TRUE.equals(this.metadataOnly);
    }

    private Boolean detailBoolean(String... keys) {
        return parseBoolean(firstDetailOrFallback("", keys));
    }

    private long positiveDetailLong(String... keys) {
        Long value = parseLong(firstDetailOrFallback("", keys));
        return value == null ? 0L : Math.max(0L, value);
    }

    private static void appendSource(StringBuilder sources, String source, boolean ready) {
        if (!ready) {
            return;
        }
        if (sources.length() > 0) {
            sources.append(',');
        }
        sources.append(source);
    }

    private String cacheLabel() {
        if (this.cacheReadCount == null && this.cacheWriteCount == null) {
            return "";
        }
        return valueOrUnknown(this.cacheReadCount) + "/" + valueOrUnknown(this.cacheWriteCount);
    }

    private static int append(StringBuilder label, String key, String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        label.append(' ').append(key).append('=').append(value);
        return 1;
    }

    private static String valueOrUnknown(Long value) {
        return value == null ? "?" : Long.toString(value);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String booleanOrUnknown(Boolean value) {
        return value == null ? "?" : Boolean.toString(value);
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() <= maxLength) {
            return value == null || value.isBlank() ? "unreported" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String shaderDenoiseOutputBoundaryText(boolean realShaderOutputReady) {
        return realShaderOutputReady
                ? "real shader-generated denoise output is explicitly reported ready"
                : "CPU/readback fallback or candidate-only image must not be treated as real shader-generated denoise output";
    }

    private static Map<String, String> normalizeFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = normalizeFieldKey(entry.getKey());
            String value = stripQuotes(entry.getValue());
            if (!key.isBlank() && !value.isBlank()) {
                normalized.putIfAbsent(key, value);
            }
        }
        return normalized;
    }

    private static String firstPresent(Map<String, String> fields, String... keys) {
        if (fields == null || fields.isEmpty() || keys == null) {
            return "";
        }

        for (String key : keys) {
            String value = fields.get(normalizeFieldKey(key));
            if (value != null && !value.isBlank()) {
                return stripQuotes(value);
            }
        }
        return "";
    }

    private static Boolean parseBoolean(String value) {
        String cleaned = stripQuotes(value).toLowerCase(Locale.ROOT);
        return switch (cleaned) {
            case "1", "true", "yes", "y", "on", "enabled", "active" -> true;
            case "0", "false", "no", "n", "off", "disabled", "inactive" -> false;
            default -> null;
        };
    }

    private static Long parseLong(String value) {
        String cleaned = stripQuotes(value);
        if (cleaned.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long[] parseLongPair(String value) {
        String cleaned = stripQuotes(value);
        if (cleaned.isBlank()) {
            return new Long[]{null, null};
        }

        String[] parts = cleaned.split("[/x:,]");
        if (parts.length < 2) {
            return new Long[]{null, null};
        }
        return new Long[]{parseLong(parts[0]), parseLong(parts[1])};
    }

    private static String xyzLabel(String x, String y, String z) {
        if (x.isBlank() || y.isBlank() || z.isBlank()) {
            return "";
        }
        return x + "x" + y + "x" + z;
    }

    private static String xyLabel(String x, String y) {
        if (x.isBlank() || y.isBlank()) {
            return "";
        }
        return x + "x" + y;
    }

    private static String pairLabel(String first, String second) {
        if (first.isBlank() || second.isBlank()) {
            return "";
        }
        return first + "/" + second;
    }

    private static Map<String, String> immutable(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String cleanStageId(String value) {
        String cleaned = stripQuotes(value).toLowerCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return "unknown";
        }
        return cleaned.replace('-', '_').replace(' ', '_');
    }

    private static String normalizeFieldKey(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    }

    private static String sanitizeKey(String value) {
        String cleaned = clean(value, "unknown").toLowerCase(Locale.ROOT);
        StringBuilder sanitized = new StringBuilder(cleaned.length());
        for (int index = 0; index < cleaned.length(); index++) {
            char character = cleaned.charAt(index);
            if ((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9')) {
                sanitized.append(character);
            } else {
                sanitized.append('.');
            }
        }
        return sanitized.toString().replaceAll("\\.+", ".");
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String blankToEmpty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
