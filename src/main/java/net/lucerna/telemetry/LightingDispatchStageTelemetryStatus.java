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
        Boolean cpuReadbackFallback,
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
        Boolean previewFallbackContribution,
        Boolean metadataOnlyProofRejected,
        Boolean focusWindowCaptureRejected,
        Boolean proofMarkerEvidenceRejected,
        Boolean temporaryDirectSubstitutionRejected,
        Boolean rectangularWashoutRejected,
        String physicalSceneMarker,
        String physicalOutputMarker,
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
        proofBoundaryMarker = blankToEmpty(stripQuotes(proofBoundaryMarker));
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
        Boolean cpuReadbackFallback = parseBoolean(firstPresent(
                normalizedFields,
                "cpu_readback_fallback",
                "cpu_readback_fallback_used",
                "cpu_denoise_readback_fallback",
                "shader_denoise_cpu_readback_fallback",
                "denoise_cpu_readback_fallback"
        ));
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
                cpuReadbackFallback,
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
                previewFallbackContribution,
                metadataOnlyProofRejected,
                focusWindowCaptureRejected,
                proofMarkerEvidenceRejected,
                temporaryDirectSubstitutionRejected,
                rectangularWashoutRejected,
                physicalSceneMarker,
                physicalOutputMarker,
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
            fieldCount += append(label, "cpuFallback", this.cpuReadbackFallback == null ? "" : Boolean.toString(this.cpuReadbackFallback));
            fieldCount += append(label, "temporalReady", this.temporalReady == null ? "" : Boolean.toString(this.temporalReady));
            fieldCount += append(label, "flicker", this.flickerScore);
        }
        if (isPhysicalGiLikeStage() || hasAnyPhysicalGiEvidence()) {
            fieldCount += append(label, "physicalScene", this.physicalSceneLinked == null ? "" : Boolean.toString(this.physicalSceneLinked));
            fieldCount += append(label, "physicalSurface", this.physicalSurfaceContribution == null ? "" : Boolean.toString(this.physicalSurfaceContribution));
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

    public String denoiseReadinessStatusLine() {
        return this.stageDisplayName()
                + " denoise rawSource=" + booleanOrUnknown(this.rawSourceReady)
                + " cpuReady=" + booleanOrUnknown(this.cpuDenoiseReady)
                + " shaderIntent=" + booleanOrUnknown(this.shaderDenoiseIntended)
                + " shaderOutput=" + booleanOrUnknown(this.shaderOutputReady)
                + " shaderGenerated=" + booleanOrUnknown(this.shaderGeneratedOutput)
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
                + " cpuReadbackFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " blockers=" + shorten(this.shaderDenoiseBlockers, 56);
    }

    public String shaderDenoiseOutputBoundaryLine() {
        boolean realShaderOutputReady = Boolean.TRUE.equals(this.shaderOutputReady)
                && Boolean.TRUE.equals(this.shaderGeneratedOutput)
                && Boolean.TRUE.equals(this.shaderOutputImageReady)
                && Boolean.TRUE.equals(this.shaderOutputMaterialReady);
        String blocker = this.shaderDenoiseBlockers.isBlank()
                ? "unreported"
                : shorten(this.shaderDenoiseBlockers, 64);
        String source = this.sourceIdentity.isBlank()
                ? "unreported"
                : shorten(this.sourceIdentity, 48);
        return this.stageDisplayName()
                + " shader denoise output realShaderOutputReady=" + realShaderOutputReady
                + " outputReady=" + booleanOrUnknown(this.shaderOutputReady)
                + " generatedOutput=" + booleanOrUnknown(this.shaderGeneratedOutput)
                + " outputImage=" + booleanOrUnknown(this.shaderOutputImageReady)
                + " outputMaterial=" + booleanOrUnknown(this.shaderOutputMaterialReady)
                + " cpuReadbackFallback=" + booleanOrUnknown(this.cpuReadbackFallback)
                + " sourceIdentity=" + source
                + " blockers=" + blocker
                + " boundary=\"CPU/readback visual shaping is not real shader-generated denoise output\"";
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
        if (this.cpuReadbackFallback != null) {
            fields.put(normalizedPrefix + ".cpuReadbackFallback", Boolean.toString(this.cpuReadbackFallback));
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
                || this.cpuReadbackFallback != null
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
                || !this.evidenceBoundary.isBlank();
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
