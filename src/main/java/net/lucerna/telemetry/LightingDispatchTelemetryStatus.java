package net.lucerna.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record LightingDispatchTelemetryStatus(
        boolean statusAvailable,
        Long generation,
        Integer advertisedDispatches,
        Integer payloadDispatches,
        Integer enabledDispatches,
        Integer disabledDispatches,
        String generationRange,
        Map<String, LightingDispatchStageTelemetryStatus> stages,
        String message
) {
    private static final Pattern LOOSE_STAGE_FIELD_PATTERN = Pattern.compile(
            "(?i)\\b(?:lighting[._-](?:dispatch[._-])?stage[._-])?"
                    + "([a-z0-9_-]+)[._-]"
                    + "(enabled|enabled_this_packet|active|generation|last_generation|dispatch_generation|"
                    + "stage_generation|dispatch|last_dispatch|dispatch_groups|groups|rays|last_rays|ray_count|"
                    + "last_samples|samples|sample_count|last_sample_count|last_candidates|candidates|candidate_count|"
                    + "last_candidate_count|shadow_candidates|shadow_candidate_count|direct_shadow_candidates|"
                    + "budgeted_shadow_candidate_count|budgeted_shadow_candidates|section_snapshot_count|section_snapshots|"
                    + "celestial_count|emissive_count|payload_accepted|payload_generation|payload_generation_range|"
                    + "payload_frame|payload_frame_index|payload_validated|payload_has_direct_work|"
                    + "payload_ready_for_shadow_tracing|payload_metadata_only|cpu_output_generated|"
                    + "gi_cpu_output_generated|native_gi_output_generated|"
                    + "output_width|output_height|gi_output_width|gi_output_height|native_gi_output_width|"
                    + "native_gi_output_height|output_pixels|output_pixel_count|gi_output_pixels|"
                    + "gi_output_pixel_count|native_gi_output_pixels|native_gi_output_pixel_count|"
                    + "output_energy|gi_output_energy|native_gi_output_energy|output_checksum|"
                    + "gi_output_checksum|native_gi_output_checksum|"
                    + "output_source|output_source_label|source|source_label|preview_source|native_output_source|"
                    + "native_gi_output_source|temporary_direct_source|temporary_direct_light_source|"
                    + "temporary_direct_light_source_ready|temporary_source_ready|uses_direct_light_payload|"
                    + "using_direct_light_payload|direct_light_payload_source|"
                    + "raw_source_ready|raw_input_ready|input_source_ready|raw_gi_ready|raw_diffuse_gi_ready|"
                    + "raw_diffuse_gi_input_ready|denoise_raw_source_ready|denoise_raw_input_ready|"
                    + "cpu_denoise_ready|cpu_denoise_output_ready|cpu_denoised_output_ready|"
                    + "cpu_readback_denoise_ready|cpu_readback_denoised_output_ready|"
                    + "denoise_cpu_ready|denoise_cpu_output_ready|"
                    + "shader_denoise_intent|shader_denoise_intended|shader_denoise_planned|"
                    + "shader_denoise_enabled|shader_denoise_contract_ready|denoise_shader_intended|"
                    + "denoise_shader_planned|shader_output_ready|shader_denoise_output_ready|"
                    + "real_denoise_shader_output|real_shader_denoise_output|real_shader_gi_output|"
                    + "gpu_denoise_output_ready|gpu_denoise_output|shader_denoise_ready|"
                    + "shader_dispatch_prepared|shader_denoise_dispatch_prepared|"
                    + "denoise_shader_dispatch_prepared|real_shader_denoise_dispatch_prepared|"
                    + "gpu_denoise_dispatch_prepared|shader_output_image_ready|"
                    + "shader_denoise_output_image_ready|real_shader_output_image_ready|"
                    + "real_shader_denoise_output_image_ready|gpu_denoise_output_image_ready|"
                    + "shader_output_material_ready|shader_denoise_output_material_ready|"
                    + "real_shader_output_material_ready|real_shader_denoise_output_material_ready|"
                    + "gpu_denoise_output_material_ready|shader_generated_output|"
                    + "shader_denoise_generated_output|real_shader_generated_output|"
                    + "real_shader_denoise_generated_output|real_denoise_shader_generated_output|"
                    + "gpu_denoise_generated_output|shader_generated_denoise_output_evidence|"
                    + "shader_denoise_generated_output_evidence|real_shader_denoise_output_evidence|"
                    + "shader_denoise_output_evidence|shader_generated_denoise_output_proven|"
                    + "shader_generated_denoise_output_marker|shader_denoise_generated_output_marker|"
                    + "real_shader_denoise_output_marker|shader_denoise_output_evidence_marker|"
                    + "public_mojang_shader_visual_output_attempted|"
                    + "public_mojang_shader_output_attempted|public_mojang_visual_output_attempted|"
                    + "public_mojang_denoise_visual_output_attempted|shader_visual_output_attempted|"
                    + "public_mojang_shader_visual_output_submitted|public_mojang_shader_output_submitted|"
                    + "public_mojang_visual_output_submitted|public_mojang_denoise_visual_output_submitted|"
                    + "shader_visual_output_submitted|public_mojang_shader_visual_output_ready|"
                    + "public_mojang_shader_output_ready|public_mojang_visual_output_ready|"
                    + "public_mojang_denoise_visual_output_ready|shader_visual_output_ready|"
                    + "cpu_readback_fallback|cpu_readback_fallback_used|"
                    + "cpu_denoise_readback_fallback|shader_denoise_cpu_readback_fallback|"
                    + "denoise_cpu_readback_fallback|shader_denoise_blockers|shader_denoise_blocker|"
                    + "shader_blockers|shader_blocker|denoise_blockers|output_blockers|blockers|blocker|"
                    + "edge_rejection_count|edge_reject_count|edge_rejections|edge_rejected|"
                    + "edge_rejected_count|denoise_edge_rejections|history_rejection_count|"
                    + "history_reject_count|history_rejections|history_rejected|history_rejected_count|"
                    + "temporal_history_rejected|temporal_history_rejection_count|last_history_rejected|"
                    + "temporal_stable_pixels|temporal_stable_pixel_count|stable_pixels|stable_pixel_count|"
                    + "temporal_pixels_stable|temporal_unstable_pixels|temporal_unstable_pixel_count|"
                    + "unstable_pixels|unstable_pixel_count|temporal_pixels_unstable|frame_delta_pixels|"
                    + "frame_delta_pixel_count|temporal_frame_delta_pixels|temporal_changed_pixels|"
                    + "changed_pixels|denoised_output_changed_pixels|frame_delta_mean_delta|"
                    + "frame_delta_mean_abs_delta|temporal_frame_delta_mean_delta|temporal_mean_abs_delta|"
                    + "mean_abs_delta|denoised_output_mean_abs_delta|previous_output_checksum|"
                    + "previous_denoised_output_checksum|previous_frame_checksum|previous_checksum|"
                    + "temporal_previous_checksum|current_output_checksum|current_denoised_output_checksum|"
                    + "current_frame_checksum|current_checksum|temporal_current_checksum|history_confidence|"
                    + "avg_history_confidence|temporal_confidence|last_history_confidence|"
                    + "temporal_history_confidence|flicker_score|temporal_flicker_score|"
                    + "denoise_flicker_score|temporal_instability_score|ghosting_risk|"
                    + "ghosting_risk_marker|temporal_ghosting_risk|temporal_ghosting_risk_marker|"
                    + "temporal_ghosting_marker|temporal_ready|temporal_stability_ready|"
                    + "temporal_history_ready|history_ready|temporal_proof_ready|temporal_acceptance_ready|"
                    + "temporal_readiness_marker|temporal_stability_readiness_marker|"
                    + "temporal_history_marker|last_temporal_history_marker|history_marker|temporal_marker|"
                    + "source_identity|source_id|denoise_source|denoise_source_identity|"
                    + "evidence_boundary|proof_boundary|boundary|quality_boundary|"
                    + "shader_denoise_evidence_boundary|shader_denoise_boundary|"
                    + "denoise_evidence_boundary|denoise_quality_boundary|"
                    + "physical_scene_link_score|physical_gi_scene_link_score|gi_physical_scene_link_score|"
                    + "scene_link_score|scene_linked_score|physical_output_checksum|physical_gi_output_checksum|"
                    + "gi_physical_output_checksum|native_physical_output_checksum|physical_scene_linked|"
                    + "physical_gi_scene_linked|scene_linked_physical|gi_scene_linked|"
                    + "physical_surface_contribution|physical_gi_surface_contribution|"
                    + "surface_physical_contribution|physical_contribution|localized_emissive_spill|"
                    + "localized_spill|emissive_spill_localized|emissive_spill|hue_shifted_bounce|"
                    + "colored_bounce_hue_shift|colored_bounce|bounce_hue_shifted|"
                    + "contact_shadow_darkening|contact_shadows|local_occlusion_darkening|local_occlusion|"
                    + "final_physical_composite_ready|physical_final_composite_ready|"
                    + "final_composite_physical_ready|final_physical_composite|preview_fallback_contribution|"
                    + "physical_preview_fallback_contribution|cpu_preview_fallback_contribution|"
                    + "g_buffer_depth_sampling_evidence|gbuffer_depth_sampling_evidence|depth_sampling_evidence|"
                    + "real_gbuffer_depth_sampling|real_depth_texture_sampled|g_buffer_depth_sampled|"
                    + "gbuffer_depth_sampled|g_buffer_depth_texture_sampled|gbuffer_depth_texture_sampled|"
                    + "depth_texture_sampled|g_buffer_depth_metadata_only|gbuffer_depth_metadata_only|"
                    + "depth_sampling_metadata_only|g_buffer_depth_sampling_metadata_only|"
                    + "depth_texture_metadata_only|g_buffer_depth_sample_count|gbuffer_depth_sample_count|"
                    + "depth_sample_count|g_buffer_depth_samples|gbuffer_depth_samples|depth_samples|"
                    + "java_g_buffer_depth_sampling_evidence|java_gbuffer_depth_sampling_evidence|"
                    + "java_depth_sampling_evidence|java_true_depth_sampling|java_depth_texture_sampled|"
                    + "java_depth_buffer_sampled|java_g_buffer_depth_sampled|java_gbuffer_depth_sampled|"
                    + "java_gbuffer_sampled|java_g_buffer_depth_metadata_only|java_gbuffer_depth_metadata_only|"
                    + "java_depth_sampling_metadata_only|java_depth_texture_metadata_only|"
                    + "java_g_buffer_depth_sample_count|java_gbuffer_depth_sample_count|"
                    + "java_depth_sample_count|java_depth_samples|native_g_buffer_depth_sampling_evidence|"
                    + "native_gbuffer_depth_sampling_evidence|native_depth_sampling_evidence|"
                    + "native_true_depth_sampling|native_depth_texture_sampled|native_depth_buffer_sampled|"
                    + "native_g_buffer_depth_sampled|native_gbuffer_depth_sampled|native_gbuffer_sampled|"
                    + "native_g_buffer_depth_metadata_only|native_gbuffer_depth_metadata_only|"
                    + "native_depth_sampling_metadata_only|native_depth_texture_metadata_only|"
                    + "native_g_buffer_depth_sample_count|native_gbuffer_depth_sample_count|"
                    + "native_depth_sample_count|native_depth_samples|"
                    + "shader_pass_g_buffer_depth_sampling_evidence|"
                    + "shader_pass_gbuffer_depth_sampling_evidence|shader_pass_depth_sampling_evidence|"
                    + "shader_depth_sampling_evidence|shader_true_depth_sampling|"
                    + "shader_pass_depth_texture_sampled|shader_depth_texture_sampled|"
                    + "shader_pass_depth_buffer_sampled|shader_depth_buffer_sampled|"
                    + "shader_pass_g_buffer_depth_sampled|shader_g_buffer_depth_sampled|"
                    + "shader_pass_gbuffer_depth_sampled|shader_gbuffer_depth_sampled|"
                    + "shader_pass_gbuffer_sampled|shader_gbuffer_sampled|"
                    + "shader_pass_g_buffer_depth_metadata_only|shader_pass_gbuffer_depth_metadata_only|"
                    + "shader_pass_depth_sampling_metadata_only|shader_depth_sampling_metadata_only|"
                    + "shader_pass_depth_texture_metadata_only|shader_depth_texture_metadata_only|"
                    + "shader_pass_g_buffer_depth_sample_count|shader_pass_gbuffer_depth_sample_count|"
                    + "shader_g_buffer_depth_sample_count|shader_gbuffer_depth_sample_count|"
                    + "shader_pass_depth_sample_count|shader_depth_sample_count|"
                    + "shader_pass_depth_samples|shader_depth_samples|"
                    + "g_buffer_depth_sampling_marker|gbuffer_depth_sampling_marker|depth_sampling_marker|"
                    + "depth_texture_sample_marker|real_shadow_map_evidence|real_shadow_map|"
                    + "shadow_map_evidence|shadowmap_evidence|real_shadow_map_ready|shadow_map_ready|"
                    + "shadow_map_rendered|shadowmap_rendered|real_shadow_map_rendered|"
                    + "shadow_map_output_rendered|shadow_map_sampled|shadowmap_sampled|"
                    + "real_shadow_map_sampled|shadow_map_depth_sampled|shadow_map_metadata_only|"
                    + "shadowmap_metadata_only|real_shadow_map_metadata_only|shadow_map_evidence_metadata_only|"
                    + "shadow_map_evidence_marker|shadowmap_evidence_marker|real_shadow_map_marker|shadow_map_marker|"
                    + "voxel_ray_traced_lighting_consumed_evidence|voxel_traced_lighting_consumed_evidence|"
                    + "ray_traced_lighting_consumed_evidence|traced_lighting_consumed_evidence|"
                    + "voxel_ray_traced_lighting_consumed|ray_traced_lighting_consumed|"
                    + "real_traced_lighting_consumed|traced_lighting_consumed|"
                    + "real_ray_traced_lighting_consumed|real_voxel_traced_lighting_consumed|"
                    + "real_gpu_traversal_executed|gpu_voxel_traversal_executed|"
                    + "real_gpu_voxel_traversal_executed|real_ray_tracing_executed|"
                    + "hardware_ray_tracing_executed|traced_lighting_metadata_only|"
                    + "ray_traced_lighting_metadata_only|voxel_ray_traced_lighting_metadata_only|"
                    + "traced_lighting_consumed_metadata_only|voxel_ray_traced_lighting_marker|"
                    + "ray_traced_lighting_marker|traced_lighting_consumed_marker|voxel_traced_lighting_marker|"
                    + "metadata_only_proof_rejected|metadata_preview_rejected|metadata_only_rejected|"
                    + "focus_window_capture_rejected|focus_window_rejected|focus_window_only_rejected|"
                    + "proof_marker_evidence_rejected|proof_marker_rejected|proof_marker_source_rejected|"
                    + "temporary_direct_substitution_rejected|temporary_direct_light_substitution_rejected|"
                    + "temporary_direct_source_rejected|direct_light_substitution_rejected|"
                    + "rectangular_washout_rejected|anti_rectangular_washout_passed|washout_rejected|"
                    + "wrong_window_screenshot_rejected|wrong_window_capture_rejected|"
                    + "window_screenshot_rejected|wrong_provenance_rejected|blank_screenshot_rejected|"
                    + "blank_surface_rejected|blank_capture_rejected|blank_frame_rejected|"
                    + "physical_scene_marker|physical_gi_scene_marker|scene_link_marker|"
                    + "physical_scene_evidence_marker|physical_output_marker|physical_gi_output_marker|"
                    + "physical_output_evidence_marker|emissive_spill_marker|localized_emissive_spill_marker|"
                    + "spill_marker|colored_bounce_marker|hue_shifted_bounce_marker|bounce_marker|"
                    + "contact_shadow_marker|contact_shadow_darkening_marker|local_occlusion_marker|"
                    + "final_physical_composite_marker|physical_final_composite_marker|"
                    + "physical_composite_marker|proof_boundary_marker|physical_proof_boundary_marker|"
                    + "physical_gi_proof_boundary|gi_proof_boundary_marker|"
                    + "cache|last_cache|cache_counts|cache_reads|cache_writes|cache_read_count|cache_write_count|"
                    + "last_flags|flags|stage_flags|placeholder|metadata_only|validated|valid|debug_overlay|debug|"
                    + "ready_for_native_execution|native_ready|ready|executable|readiness_reason|ready_reason|"
                    + "native_readiness_reason|reason|recorded_this_frame|recorded|submitted_this_frame|"
                    + "last_frame|frame|frame_index|last_frame_index|dispatch_frame|"
                    + "last_size|size|dimensions|resolution|last_io|io|io_counts|inputs|input_count|last_input_count|"
                    + "outputs|output_count|last_output_count)"
                    + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;}\\]]+)"
    );

    public LightingDispatchTelemetryStatus {
        generationRange = blankToEmpty(stripQuotes(generationRange));
        stages = immutableStages(stages);
        message = clean(message, "Lighting dispatch status has not been reported.");
        statusAvailable = statusAvailable && hasAnyStatus(
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages
        );
    }

    public static LightingDispatchTelemetryStatus unavailable(String message) {
        return new LightingDispatchTelemetryStatus(
                false,
                null,
                null,
                null,
                null,
                null,
                "",
                Map.of(),
                message
        );
    }

    public static LightingDispatchTelemetryStatus fromNativeStatus(String nativeStatus) {
        String cleanedStatus = clean(nativeStatus, "");
        if (cleanedStatus.isBlank()) {
            return unavailable("Native status string is blank.");
        }

        Map<String, String> lightingFields = extractLightingAggregateFields(cleanedStatus);
        Map<String, LightingDispatchStageTelemetryStatus> stages = parseStages(cleanedStatus);
        Long generation = firstLong(
                findLooseValue(cleanedStatus, "lighting_dispatch_generation"),
                firstPresent(lightingFields, "last_packet_generation", "packet_generation", "generation")
        );
        Integer advertisedDispatches = firstInt(
                findLooseValue(cleanedStatus, "lighting_dispatches"),
                firstPresent(lightingFields, "advertised_dispatches", "dispatches", "advertised")
        );
        Integer payloadDispatches = firstInt(
                findLooseValue(cleanedStatus, "lighting_dispatch_payloads"),
                firstPresent(lightingFields, "payload_dispatches", "payloads", "payload")
        );
        Integer enabledDispatches = firstInt(firstPresent(lightingFields, "enabled_dispatches", "enabled"));
        Integer disabledDispatches = firstInt(firstPresent(lightingFields, "disabled_dispatches", "disabled"));
        String generationRange = firstNonBlank(
                findLooseValue(cleanedStatus, "lighting_dispatch_generation_range"),
                firstPresent(lightingFields, "last_generation_range", "generation_range", "range")
        );

        if (!hasAnyStatus(
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages
        )) {
            return unavailable("Lighting dispatch fields are not present in native status.");
        }

        return new LightingDispatchTelemetryStatus(
                true,
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages,
                "Lighting dispatch status reported by native status."
        );
    }

    public boolean hasLightingDispatchStatus() {
        return this.statusAvailable;
    }

    public boolean hasStageStatuses() {
        return this.statusAvailable && !this.stages.isEmpty();
    }

    public String compactLabel() {
        if (!this.hasLightingDispatchStatus()) {
            return this.message;
        }

        StringBuilder label = new StringBuilder();
        append(label, "gen", this.generation == null ? "" : Long.toString(this.generation));
        append(label, "range", this.generationRange);
        append(label, "dispatches", this.advertisedDispatches == null ? "" : Integer.toString(this.advertisedDispatches));
        append(label, "payloads", this.payloadDispatches == null ? "" : Integer.toString(this.payloadDispatches));
        if (this.enabledDispatches != null || this.disabledDispatches != null) {
            append(label, "enabled", valueOrUnknown(this.enabledDispatches) + "/" + valueOrUnknown(this.disabledDispatches));
        }
        append(label, "stages", Integer.toString(this.stages.size()));
        return label.length() == 0 ? this.message : label.toString();
    }

    public String shaderDenoiseOutputBoundarySummary() {
        if (!this.hasLightingDispatchStatus()) {
            return "unavailable:" + this.message;
        }

        LightingDispatchStageTelemetryStatus denoiseStage = this.stages.get("denoise");
        if (denoiseStage == null) {
            denoiseStage = firstDenoiseLikeStage();
        }
        if (denoiseStage == null) {
            return "unreported:no-denoise-stage";
        }

        return denoiseStage.shaderDenoiseOutputBoundaryLine();
    }

    public boolean hasGBufferDepthSamplingEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.gBufferDepthSamplingEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasJavaGBufferDepthSamplingEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.javaDepthSamplingEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNativeGBufferDepthSamplingEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.nativeDepthSamplingEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasShaderPassGBufferDepthSamplingEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.shaderPassDepthSamplingEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDepthSamplingPassOutputEvidence() {
        return this.hasJavaGBufferDepthSamplingEvidence()
                || this.hasNativeGBufferDepthSamplingEvidence()
                || this.hasShaderPassGBufferDepthSamplingEvidence();
    }

    public long maxGBufferDepthSampleCount() {
        long max = 0L;
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            max = Math.max(max, stage.maxGBufferDepthSampleCount());
        }
        return max;
    }

    public String depthSamplingEvidenceSources() {
        boolean javaDepth = this.hasJavaGBufferDepthSamplingEvidence();
        boolean nativeDepth = this.hasNativeGBufferDepthSamplingEvidence();
        boolean shaderDepth = this.hasShaderPassGBufferDepthSamplingEvidence();
        StringBuilder sources = new StringBuilder();
        appendSource(sources, "java", javaDepth);
        appendSource(sources, "native", nativeDepth);
        appendSource(sources, "shader", shaderDepth);
        appendSource(sources, "generic", this.hasGBufferDepthSamplingEvidence()
                && !javaDepth
                && !nativeDepth
                && !shaderDepth);
        return sources.length() == 0 ? "none" : sources.toString();
    }

    public boolean hasPhysicalGiEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.physicalGiEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRealShadowMapEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.realShadowMapEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNativeShadowMapMask() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.nativeShadowMapMaskReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasShadowMapOutputConsumed() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.shadowMapOutputConsumedReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRealShadowMapComposite() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.realShadowMapCompositeReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasShadowMapCompositeNoOverclaimBoundary() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (!stage.shadowMapCompositeNoOverclaimBoundary()
                    && (stage.nativeShadowMapMask() != null
                    || stage.shadowMapOutputConsumed() != null
                    || stage.realShadowMapComposite() != null
                    || stage.screenSpaceShadowDecal() != null
                    || stage.lowResDirectTextureShadowProof() != null)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasVoxelRayTracedLightingConsumedEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.voxelRayTracedLightingConsumedEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasShaderGeneratedDenoiseOutputEvidence() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (stage.shaderGeneratedDenoiseOutputEvidenceReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCpuReadbackFallbackActive() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (Boolean.TRUE.equals(stage.cpuReadbackFallback())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMetadataOnlyActive() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            if (Boolean.TRUE.equals(stage.metadataOnly())
                    || Boolean.TRUE.equals(stage.gBufferDepthMetadataOnly())
                    || Boolean.TRUE.equals(stage.shadowMapMetadataOnly())
                    || Boolean.TRUE.equals(stage.tracedLightingMetadataOnly())) {
                return true;
            }
        }
        return false;
    }

    public String advancedLightingEvidenceSummary() {
        return "gBufferDepthSamplingEvidence=" + this.hasGBufferDepthSamplingEvidence()
                + " javaDepthSamplingEvidence=" + this.hasJavaGBufferDepthSamplingEvidence()
                + " nativeDepthSamplingEvidence=" + this.hasNativeGBufferDepthSamplingEvidence()
                + " shaderPassDepthSamplingEvidence=" + this.hasShaderPassGBufferDepthSamplingEvidence()
                + " depthSamplingEvidenceSources=" + this.depthSamplingEvidenceSources()
                + " maxGBufferDepthSampleCount=" + this.maxGBufferDepthSampleCount()
                + " physicalGiEvidence=" + this.hasPhysicalGiEvidence()
                + " realShadowMapEvidence=" + this.hasRealShadowMapEvidence()
                + " nativeShadowMapMask=" + this.hasNativeShadowMapMask()
                + " shadowMapOutputConsumed=" + this.hasShadowMapOutputConsumed()
                + " realShadowMapComposite=" + this.hasRealShadowMapComposite()
                + " shadowMapCompositeNoOverclaim=" + this.hasShadowMapCompositeNoOverclaimBoundary()
                + " voxelRayTracedLightingConsumedEvidence=" + this.hasVoxelRayTracedLightingConsumedEvidence()
                + " shaderGeneratedDenoiseOutputEvidence=" + this.hasShaderGeneratedDenoiseOutputEvidence()
                + " cpuReadbackFallbackActive=" + this.hasCpuReadbackFallbackActive()
                + " metadataOnlyActive=" + this.hasMetadataOnlyActive();
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "lighting.dispatch");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".available", Boolean.toString(this.hasLightingDispatchStatus()));
        fields.put(normalizedPrefix + ".summary", this.compactLabel());
        fields.put(normalizedPrefix + ".shaderDenoiseOutputBoundary", this.shaderDenoiseOutputBoundarySummary());
        fields.put(normalizedPrefix + ".advancedLightingEvidence", this.advancedLightingEvidenceSummary());
        fields.put(
                normalizedPrefix + ".gBufferDepthSamplingEvidence",
                Boolean.toString(this.hasGBufferDepthSamplingEvidence())
        );
        fields.put(
                normalizedPrefix + ".javaDepthSamplingEvidence",
                Boolean.toString(this.hasJavaGBufferDepthSamplingEvidence())
        );
        fields.put(
                normalizedPrefix + ".nativeDepthSamplingEvidence",
                Boolean.toString(this.hasNativeGBufferDepthSamplingEvidence())
        );
        fields.put(
                normalizedPrefix + ".shaderPassDepthSamplingEvidence",
                Boolean.toString(this.hasShaderPassGBufferDepthSamplingEvidence())
        );
        fields.put(
                normalizedPrefix + ".depthSamplingPassOutputsReady",
                Boolean.toString(this.hasDepthSamplingPassOutputEvidence())
        );
        fields.put(normalizedPrefix + ".depthSamplingEvidenceSources", this.depthSamplingEvidenceSources());
        fields.put(normalizedPrefix + ".maxGBufferDepthSampleCount", Long.toString(this.maxGBufferDepthSampleCount()));
        fields.put(normalizedPrefix + ".physicalGiEvidence", Boolean.toString(this.hasPhysicalGiEvidence()));
        fields.put(normalizedPrefix + ".realShadowMapEvidence", Boolean.toString(this.hasRealShadowMapEvidence()));
        fields.put(normalizedPrefix + ".nativeShadowMapMask", Boolean.toString(this.hasNativeShadowMapMask()));
        fields.put(normalizedPrefix + ".shadowMapOutputConsumed", Boolean.toString(this.hasShadowMapOutputConsumed()));
        fields.put(normalizedPrefix + ".realShadowMapComposite", Boolean.toString(this.hasRealShadowMapComposite()));
        fields.put(
                normalizedPrefix + ".shadowMapCompositeNoOverclaim",
                Boolean.toString(this.hasShadowMapCompositeNoOverclaimBoundary())
        );
        fields.put(
                normalizedPrefix + ".voxelRayTracedLightingConsumedEvidence",
                Boolean.toString(this.hasVoxelRayTracedLightingConsumedEvidence())
        );
        fields.put(
                normalizedPrefix + ".shaderGeneratedDenoiseOutputEvidence",
                Boolean.toString(this.hasShaderGeneratedDenoiseOutputEvidence())
        );
        fields.put(normalizedPrefix + ".cpuReadbackFallbackActive", Boolean.toString(this.hasCpuReadbackFallbackActive()));
        fields.put(normalizedPrefix + ".metadataOnlyActive", Boolean.toString(this.hasMetadataOnlyActive()));
        if (this.generation != null) {
            fields.put(normalizedPrefix + ".generation", Long.toString(this.generation));
        }
        if (!this.generationRange.isBlank()) {
            fields.put(normalizedPrefix + ".generationRange", this.generationRange);
        }
        if (this.advertisedDispatches != null) {
            fields.put(normalizedPrefix + ".advertisedDispatches", Integer.toString(this.advertisedDispatches));
        }
        if (this.payloadDispatches != null) {
            fields.put(normalizedPrefix + ".payloadDispatches", Integer.toString(this.payloadDispatches));
        }
        if (this.enabledDispatches != null) {
            fields.put(normalizedPrefix + ".enabledDispatches", Integer.toString(this.enabledDispatches));
        }
        if (this.disabledDispatches != null) {
            fields.put(normalizedPrefix + ".disabledDispatches", Integer.toString(this.disabledDispatches));
        }
        fields.put(normalizedPrefix + ".stageCount", Integer.toString(this.stages.size()));
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            fields.putAll(stage.validationFields(normalizedPrefix + ".stage." + sanitizeKey(stage.stageId())));
        }
        return Collections.unmodifiableMap(fields);
    }

    private LightingDispatchStageTelemetryStatus firstDenoiseLikeStage() {
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            String stageId = stage.stageId();
            if ("shader_denoise".equals(stageId)
                    || "edge_aware_denoise".equals(stageId)
                    || "diffuse_gi_denoise".equals(stageId)) {
                return stage;
            }
        }
        return null;
    }

    private static Map<String, LightingDispatchStageTelemetryStatus> parseStages(String nativeStatus) {
        Map<String, Map<String, String>> stageFields = new LinkedHashMap<>();
        parseStageBlocks(nativeStatus, stageFields);
        parseLooseStageFields(nativeStatus, stageFields);
        parseLooseAdvancedLightingEvidenceFields(nativeStatus, stageFields);
        mergeDenoiseExecutionFields(nativeStatus, stageFields);
        mergeDiffuseGiExecutionFields(nativeStatus, stageFields);
        mergeDirectExecutionFields(nativeStatus, stageFields);
        mergeDirectPayloadSummaryFields(nativeStatus, stageFields);

        if (stageFields.isEmpty()) {
            return Map.of();
        }

        Map<String, LightingDispatchStageTelemetryStatus> stages = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : stageFields.entrySet()) {
            LightingDispatchStageTelemetryStatus status = LightingDispatchStageTelemetryStatus.fromFields(entry.getValue());
            stages.put(status.stageId(), status);
        }
        return stages;
    }

    private static void parseStageBlocks(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String stageList = extractStageList(nativeStatus);
        if (stageList.isBlank()) {
            return;
        }

        for (String block : extractBraceBlocks(stageList)) {
            Map<String, String> fields = parseDelimitedFields(block);
            String stageId = normalizeStageId(firstPresent(fields, "id", "stage", "stage_id", "stage_name", "name"));
            if (stageId.isBlank()) {
                continue;
            }

            Map<String, String> target = stageFields.computeIfAbsent(stageId, ignored -> new LinkedHashMap<>());
            target.putIfAbsent("id", stageId);
            putMissing(target, fields);
        }
    }

    private static void parseLooseStageFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        Matcher matcher = LOOSE_STAGE_FIELD_PATTERN.matcher(nativeStatus);
        while (matcher.find()) {
            String stageId = normalizeStageId(matcher.group(1));
            if (!isKnownStageId(stageId) && !stageFields.containsKey(stageId)) {
                continue;
            }

            String fieldKey = normalizeLooseStageFieldKey(matcher.group(2));
            String fieldValue = stripQuotes(matcher.group(3));
            if (fieldKey.isBlank() || fieldValue.isBlank()) {
                continue;
            }

            Map<String, String> target = stageFields.computeIfAbsent(stageId, ignored -> new LinkedHashMap<>());
            target.putIfAbsent("id", stageId);
            target.putIfAbsent(fieldKey, fieldValue);
        }
    }

    private static void parseLooseAdvancedLightingEvidenceFields(
            String nativeStatus,
            Map<String, Map<String, String>> stageFields
    ) {
        Map<String, String> target = new LinkedHashMap<>();
        target.put("id", "advanced_lighting");
        putIfPresent(target, "g_buffer_depth_sampling_evidence", firstLooseValue(
                nativeStatus,
                "g_buffer_depth_sampling_evidence",
                "gbuffer_depth_sampling_evidence",
                "depth_sampling_evidence",
                "real_gbuffer_depth_sampling",
                "real_depth_texture_sampled"
        ));
        putIfPresent(target, "g_buffer_depth_texture_sampled", firstLooseValue(
                nativeStatus,
                "g_buffer_depth_texture_sampled",
                "gbuffer_depth_texture_sampled",
                "depth_texture_sampled"
        ));
        putIfPresent(target, "g_buffer_depth_metadata_only", firstLooseValue(
                nativeStatus,
                "g_buffer_depth_metadata_only",
                "gbuffer_depth_metadata_only",
                "depth_sampling_metadata_only",
                "depth_texture_metadata_only"
        ));
        putIfPresent(target, "g_buffer_depth_sample_count", firstLooseValue(
                nativeStatus,
                "g_buffer_depth_sample_count",
                "gbuffer_depth_sample_count",
                "depth_sample_count",
                "depth_samples"
        ));
        putIfPresent(target, "java_g_buffer_depth_sampling_evidence", firstLooseValue(
                nativeStatus,
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
        ));
        putIfPresent(target, "java_g_buffer_depth_metadata_only", firstLooseValue(
                nativeStatus,
                "java_g_buffer_depth_metadata_only",
                "java_gbuffer_depth_metadata_only",
                "javaDepthMetadataOnly",
                "javaGBufferDepthMetadataOnly",
                "java_depth_sampling_metadata_only",
                "java_depth_texture_metadata_only"
        ));
        putIfPresent(target, "java_g_buffer_depth_sample_count", firstLooseValue(
                nativeStatus,
                "java_g_buffer_depth_sample_count",
                "java_gbuffer_depth_sample_count",
                "javaDepthSampleCount",
                "javaGBufferDepthSampleCount",
                "java_depth_sample_count",
                "java_depth_samples"
        ));
        putIfPresent(target, "native_g_buffer_depth_sampling_evidence", firstLooseValue(
                nativeStatus,
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
        ));
        putIfPresent(target, "native_g_buffer_depth_metadata_only", firstLooseValue(
                nativeStatus,
                "native_g_buffer_depth_metadata_only",
                "native_gbuffer_depth_metadata_only",
                "nativeDepthMetadataOnly",
                "nativeGBufferDepthMetadataOnly",
                "native_depth_sampling_metadata_only",
                "native_depth_texture_metadata_only"
        ));
        putIfPresent(target, "native_g_buffer_depth_sample_count", firstLooseValue(
                nativeStatus,
                "native_g_buffer_depth_sample_count",
                "native_gbuffer_depth_sample_count",
                "nativeDepthSampleCount",
                "nativeGBufferDepthSampleCount",
                "native_depth_sample_count",
                "native_depth_samples"
        ));
        putIfPresent(target, "shader_pass_g_buffer_depth_sampling_evidence", firstLooseValue(
                nativeStatus,
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
        ));
        putIfPresent(target, "shader_pass_g_buffer_depth_metadata_only", firstLooseValue(
                nativeStatus,
                "shader_pass_g_buffer_depth_metadata_only",
                "shader_pass_gbuffer_depth_metadata_only",
                "shaderPassDepthMetadataOnly",
                "shaderPassGBufferDepthMetadataOnly",
                "shaderDepthMetadataOnly",
                "shader_pass_depth_sampling_metadata_only",
                "shader_depth_sampling_metadata_only",
                "shader_pass_depth_texture_metadata_only",
                "shader_depth_texture_metadata_only"
        ));
        putIfPresent(target, "shader_pass_g_buffer_depth_sample_count", firstLooseValue(
                nativeStatus,
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
        putIfPresent(target, "g_buffer_depth_sampling_marker", firstLooseValue(
                nativeStatus,
                "g_buffer_depth_sampling_marker",
                "gbuffer_depth_sampling_marker",
                "depth_sampling_marker"
        ));
        putIfPresent(target, "real_shadow_map_evidence", firstLooseValue(
                nativeStatus,
                "real_shadow_map_evidence",
                "real_shadow_map",
                "shadow_map_evidence",
                "shadowmap_evidence",
                "real_shadow_map_ready"
        ));
        putIfPresent(target, "shadow_map_rendered", firstLooseValue(
                nativeStatus,
                "shadow_map_rendered",
                "shadowmap_rendered",
                "real_shadow_map_rendered"
        ));
        putIfPresent(target, "shadow_map_sampled", firstLooseValue(
                nativeStatus,
                "shadow_map_sampled",
                "shadowmap_sampled",
                "real_shadow_map_sampled"
        ));
        putIfPresent(target, "shadow_map_metadata_only", firstLooseValue(
                nativeStatus,
                "shadow_map_metadata_only",
                "shadowmap_metadata_only",
                "real_shadow_map_metadata_only"
        ));
        putIfPresent(target, "native_shadow_map_mask", firstLooseValue(
                nativeStatus,
                "native_shadow_map_mask",
                "nativeShadowMapMask",
                "native_shadowmap_mask",
                "native_shadow_map_output_mask",
                "nativeShadowMapOutputMask",
                "shadow_map_mask_native",
                "shadowmap_mask_native",
                "shadow_map_source_native"
        ));
        putIfPresent(target, "shadow_map_output_consumed", firstLooseValue(
                nativeStatus,
                "shadow_map_output_consumed",
                "shadowMapOutputConsumed",
                "shadowmap_output_consumed",
                "native_shadow_map_consumed",
                "nativeShadowMapConsumed",
                "native_shadowmap_consumed",
                "shadow_map_consumed",
                "shadowMapConsumed",
                "shadowmap_consumed"
        ));
        putIfPresent(target, "real_shadow_map_composite", firstLooseValue(
                nativeStatus,
                "real_shadow_map_composite",
                "realShadowMapComposite",
                "shadow_map_final_composite",
                "shadowMapFinalComposite",
                "shadowmap_final_composite",
                "native_shadow_map_composited",
                "nativeShadowMapComposited",
                "native_shadowmap_composited"
        ));
        putIfPresent(target, "screen_space_shadow_decal", firstLooseValue(
                nativeStatus,
                "screen_space_shadow_decal",
                "screenSpaceShadowDecal",
                "screenspace_shadow_decal",
                "screen_space_shadow",
                "screenSpaceShadow",
                "shadow_decal_screen_space"
        ));
        putIfPresent(target, "low_res_direct_texture_shadow_proof", firstLooseValue(
                nativeStatus,
                "low_res_direct_texture_shadow_proof",
                "lowResDirectTextureShadowProof",
                "lowres_direct_texture_shadow_proof",
                "low_res_direct_texture",
                "cpu_direct_texture_composite",
                "cpuDirectTextureComposite",
                "directLightLowResTexture",
                "direct_light_low_res_texture"
        ));
        putIfPresent(target, "shadow_map_evidence_marker", firstLooseValue(
                nativeStatus,
                "shadow_map_evidence_marker",
                "shadowmap_evidence_marker",
                "real_shadow_map_marker"
        ));
        putIfPresent(target, "voxel_ray_traced_lighting_consumed_evidence", firstLooseValue(
                nativeStatus,
                "voxel_ray_traced_lighting_consumed_evidence",
                "voxel_traced_lighting_consumed_evidence",
                "ray_traced_lighting_consumed_evidence",
                "traced_lighting_consumed_evidence"
        ));
        putIfPresent(target, "real_traced_lighting_consumed", firstLooseValue(
                nativeStatus,
                "real_traced_lighting_consumed",
                "traced_lighting_consumed",
                "real_ray_traced_lighting_consumed"
        ));
        putIfPresent(target, "real_gpu_traversal_executed", firstLooseValue(
                nativeStatus,
                "real_gpu_traversal_executed",
                "gpu_voxel_traversal_executed",
                "real_gpu_voxel_traversal_executed",
                "real_ray_tracing_executed"
        ));
        putIfPresent(target, "traced_lighting_metadata_only", firstLooseValue(
                nativeStatus,
                "traced_lighting_metadata_only",
                "ray_traced_lighting_metadata_only",
                "voxel_ray_traced_lighting_metadata_only"
        ));
        putIfPresent(target, "voxel_ray_traced_lighting_marker", firstLooseValue(
                nativeStatus,
                "voxel_ray_traced_lighting_marker",
                "ray_traced_lighting_marker",
                "traced_lighting_consumed_marker"
        ));
        putIfPresent(target, "shader_generated_denoise_output_evidence", firstLooseValue(
                nativeStatus,
                "shader_generated_denoise_output_evidence",
                "shader_denoise_generated_output_evidence",
                "real_shader_denoise_output_evidence",
                "shader_denoise_output_evidence"
        ));
        putIfPresent(target, "shader_generated_denoise_output_marker", firstLooseValue(
                nativeStatus,
                "shader_generated_denoise_output_marker",
                "shader_denoise_generated_output_marker",
                "real_shader_denoise_output_marker"
        ));
        putIfPresent(target, "cpu_readback_fallback", firstLooseValue(
                nativeStatus,
                "cpu_readback_fallback",
                "cpu_readback_fallback_used",
                "shader_denoise_cpu_readback_fallback"
        ));
        putIfPresent(target, "metadata_only", firstLooseValue(
                nativeStatus,
                "metadata_only",
                "payload_metadata_only"
        ));
        if (target.size() > 1) {
            Map<String, String> existing = stageFields.computeIfAbsent("advanced_lighting", ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, String> entry : target.entrySet()) {
                existing.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void mergeDirectExecutionFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String directExecution = extractBraceContent(nativeStatus, "direct_execution={");
        if (directExecution.isBlank()) {
            return;
        }

        Map<String, String> executionFields = parseDelimitedFields(directExecution);
        if (executionFields.isEmpty()) {
            return;
        }

        Map<String, String> target = stageFields.computeIfAbsent("direct_lighting", ignored -> new LinkedHashMap<>());
        target.putIfAbsent("id", "direct_lighting");
        putMissing(target, executionFields);
        copyMissing(target, executionFields, "dispatch_generation", "generation");
        copyMissing(target, executionFields, "candidate_count", "candidates");
        copyMissing(target, executionFields, "sample_count", "samples");
        copyMissing(target, executionFields, "ray_count", "rays");
        copyMissing(target, executionFields, "ready", "ready_for_native_execution");
        copyMissing(target, executionFields, "last_frame", "frame_index");
    }

    private static void mergeDiffuseGiExecutionFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String diffuseGiExecution = extractBraceContent(nativeStatus, "diffuse_gi_execution={");
        if (diffuseGiExecution.isBlank()) {
            return;
        }

        Map<String, String> executionFields = parseDelimitedFields(diffuseGiExecution);
        if (executionFields.isEmpty()) {
            return;
        }

        Map<String, String> target = stageFields.computeIfAbsent("diffuse_gi", ignored -> new LinkedHashMap<>());
        target.putIfAbsent("id", "diffuse_gi");
        putMissing(target, executionFields);
        copyMissing(target, executionFields, "dispatch_generation", "generation");
        copyMissing(target, executionFields, "sample_count", "samples");
        copyMissing(target, executionFields, "ray_count", "rays");
        copyMissing(target, executionFields, "cache_read_count", "cache_reads");
        copyMissing(target, executionFields, "cache_write_count", "cache_writes");
        copyMissing(target, executionFields, "ready", "ready_for_native_execution");
        copyMissing(target, executionFields, "last_frame", "frame_index");
        copyMissing(target, executionFields, "cpu_output_generated", "native_gi_output_generated");
        copyMissing(target, executionFields, "cpu_output_width", "native_gi_output_width");
        copyMissing(target, executionFields, "cpu_output_height", "native_gi_output_height");
        copyMissing(target, executionFields, "cpu_output_pixel_count", "native_gi_output_pixel_count");
        copyMissing(target, executionFields, "cpu_output_energy", "native_gi_output_energy");
        copyMissing(target, executionFields, "cpu_output_checksum", "native_gi_output_checksum");
        copyMissing(target, executionFields, "physical_scene_marker", "source_identity");
        copyMissing(target, executionFields, "proof_boundary_marker", "evidence_boundary");
    }

    private static void mergeDenoiseExecutionFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String denoiseExecution = extractBraceContent(nativeStatus, "denoise_execution={");
        if (denoiseExecution.isBlank()) {
            return;
        }

        Map<String, String> executionFields = parseDelimitedFields(denoiseExecution);
        if (executionFields.isEmpty()) {
            return;
        }

        Map<String, String> target = stageFields.computeIfAbsent("denoise", ignored -> new LinkedHashMap<>());
        target.putIfAbsent("id", "denoise");
        putMissing(target, executionFields);
        copyMissing(target, executionFields, "dispatch_generation", "generation");
        copyMissing(target, executionFields, "raw_gi_input_ready", "raw_source_ready");
        copyMissing(target, executionFields, "cpu_denoised_readback_ready", "cpu_denoise_ready");
        copyMissing(target, executionFields, "shader_denoise_dispatch_intent", "shader_denoise_intended");
        copyMissing(target, executionFields, "shader_denoise_dispatch_prepared", "shader_dispatch_prepared");
        copyMissing(target, executionFields, "shader_denoise_input_ready", "shader_denoise_input_ready");
        copyMissing(target, executionFields, "shader_denoise_output_ready", "shader_output_ready");
        copyMissing(target, executionFields, "shader_denoise_output_image_ready", "shader_output_image_ready");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_ready", "shader_output_image_candidate");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_ready", "shader_output_image_candidate_present");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_ready", "shader_output_image_candidate_ready");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_cpu_staged", "shader_output_image_candidate_cpu_staged");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_non_gpu", "shader_output_image_candidate_non_gpu");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_size", "shader_output_image_candidate_dimensions");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_width", "shader_output_image_candidate_width");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_height", "shader_output_image_candidate_height");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_pixels", "shader_output_image_candidate_pixels");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_bytes", "shader_output_image_candidate_bytes");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_checksum", "shader_output_image_candidate_checksum");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_marker", "shader_output_image_candidate_marker");
        copyMissing(target, executionFields, "shader_denoise_output_image_candidate_marker", "shader_output_image_candidate_source");
        copyMissing(target, executionFields, "shader_denoise_output_image_blocker", "shader_output_image_candidate_blocker");
        copyMissing(target, executionFields, "shader_denoise_output_image_blocker", "shader_output_image_candidate_boundary");
        copyMissing(target, executionFields, "shader_denoise_output_image_blocker", "shader_output_blocker_reason");
        copyMissing(target, executionFields, "shader_denoise_output_material_ready", "shader_output_material_ready");
        copyMissing(target, executionFields, "shader_denoise_output_shader_generated", "shader_generated_output");
        copyMissing(target, executionFields, "public_mojang_shader_visual_output_attempted", "public_mojang_shader_visual_output_attempted");
        copyMissing(target, executionFields, "public_mojang_shader_visual_output_submitted", "public_mojang_shader_visual_output_submitted");
        copyMissing(target, executionFields, "public_mojang_shader_visual_output_ready", "public_mojang_shader_visual_output_ready");
        copyMissing(target, executionFields, "real_denoise_shader_output", "real_shader_denoise_output");
        copyMissing(target, executionFields, "real_shader_denoise_output_ready", "real_shader_denoise_output_ready");
        copyMissing(target, executionFields, "cpu_fallback_quality_metrics", "cpu_readback_fallback");
        copyMissing(target, executionFields, "shader_denoise_cpu_readback_fallback", "cpu_readback_fallback");
        copyMissing(target, executionFields, "denoised_cpu_output_generated", "cpu_output_generated");
        copyMissing(target, executionFields, "denoised_output_pixels", "output_pixels");
        copyMissing(target, executionFields, "denoised_output_checksum", "output_checksum");
        copyMissing(target, executionFields, "denoised_output_changed_pixels", "changed_pixels");
        copyMissing(target, executionFields, "denoised_output_mean_abs_delta", "mean_abs_delta");
        copyMissing(target, executionFields, "edge_rejected", "edge_rejection_count");
        copyMissing(target, executionFields, "history_rejected", "history_rejection_count");
        copyMissing(target, executionFields, "temporal_stable_pixels", "stable_pixels");
        copyMissing(target, executionFields, "temporal_unstable_pixels", "unstable_pixels");
        copyMissing(target, executionFields, "denoised_output_changed_pixels", "frame_delta_pixels");
        copyMissing(target, executionFields, "temporal_mean_abs_delta", "frame_delta_mean_delta");
        copyMissing(target, executionFields, "denoised_output_mean_abs_delta", "frame_delta_mean_delta");
        copyMissing(target, executionFields, "previous_denoised_output_checksum", "previous_output_checksum");
        copyMissing(target, executionFields, "current_denoised_output_checksum", "current_output_checksum");
        copyMissing(target, executionFields, "temporal_history_confidence", "history_confidence");
        copyMissing(target, executionFields, "temporal_flicker_score", "flicker_score");
        copyMissing(target, executionFields, "temporal_ghosting_risk_marker", "ghosting_risk");
        copyMissing(target, executionFields, "temporal_stability_ready", "temporal_ready");
        copyMissing(target, executionFields, "temporal_stability_readiness_marker", "temporal_readiness_marker");
        copyMissing(target, executionFields, "shader_denoise_output_readiness_marker", "shader_denoise_blockers");
        copyMissing(target, executionFields, "shader_denoise_output_readiness_marker", "shader_output_readiness_label");
        copyMissing(target, executionFields, "shader_denoise_output_readiness_label", "shader_output_readiness_label");
        copyMissing(target, executionFields, "shader_denoise_output_blocker_reason", "shader_output_blocker_reason");
    }

    private static void mergeDirectPayloadSummaryFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String payloadGeneration = findLooseValue(nativeStatus, "direct_lighting_payload_generation");
        String payloadPackets = findLooseValue(nativeStatus, "direct_lighting_payloads");
        String payloadCounts = extractBraceContent(nativeStatus, "direct_lighting_payload_counts={");
        if (payloadGeneration.isBlank() && payloadPackets.isBlank() && payloadCounts.isBlank()) {
            return;
        }

        Map<String, String> target = stageFields.computeIfAbsent("direct_lighting", ignored -> new LinkedHashMap<>());
        target.putIfAbsent("id", "direct_lighting");
        putIfPresent(target, "payload_generation", payloadGeneration);
        putIfPresent(target, "payload_packets", payloadPackets);

        Map<String, String> countFields = parseDelimitedFields(payloadCounts);
        copyMissing(target, countFields, "celestial", "celestial_count");
        copyMissing(target, countFields, "emissive", "emissive_count");
        copyMissing(target, countFields, "shadow", "shadow_candidate_count");
        copyMissing(target, countFields, "budgeted_shadow", "budgeted_shadow_candidate_count");
        copyMissing(target, countFields, "sections", "section_snapshot_count");
    }

    private static Map<String, String> extractLightingAggregateFields(String nativeStatus) {
        int lightingStart = nativeStatus.indexOf("lighting={");
        if (lightingStart < 0) {
            return Map.of();
        }

        int contentStart = lightingStart + "lighting={".length();
        int stagesStart = nativeStatus.indexOf("stages=[", contentStart);
        int contentEnd = stagesStart >= 0 ? stagesStart : findMatching(nativeStatus, contentStart - 1, '{', '}');
        if (contentEnd <= contentStart) {
            return Map.of();
        }

        String content = nativeStatus.substring(contentStart, trimTrailingDelimiter(nativeStatus, contentStart, contentEnd));
        return parseDelimitedFields(content);
    }

    private static String extractStageList(String nativeStatus) {
        String stageList = extractBracketContentAfter(nativeStatus, "lighting={", "stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        stageList = extractBracketContent(nativeStatus, "lighting_dispatch_stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        stageList = extractBracketContent(nativeStatus, "lighting_stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        return extractBracketContent(nativeStatus, "stages=[");
    }

    private static String extractBracketContentAfter(String source, String sectionMarker, String listMarker) {
        int sectionStart = source.indexOf(sectionMarker);
        if (sectionStart < 0) {
            return "";
        }

        int listStart = source.indexOf(listMarker, sectionStart + sectionMarker.length());
        if (listStart < 0) {
            return "";
        }
        return extractBracketContentAt(source, listStart + listMarker.length() - 1);
    }

    private static String extractBracketContent(String source, String marker) {
        int markerStart = source.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }
        return extractBracketContentAt(source, markerStart + marker.length() - 1);
    }

    private static String extractBraceContent(String source, String marker) {
        int markerStart = source.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }
        int braceIndex = markerStart + marker.length() - 1;
        int end = findMatching(source, braceIndex, '{', '}');
        if (end <= braceIndex) {
            return "";
        }
        return source.substring(braceIndex + 1, end);
    }

    private static String extractBracketContentAt(String source, int bracketIndex) {
        int end = findMatching(source, bracketIndex, '[', ']');
        if (end <= bracketIndex) {
            return "";
        }
        return source.substring(bracketIndex + 1, end);
    }

    private static List<String> extractBraceBlocks(String source) {
        List<String> blocks = new ArrayList<>();
        int blockStart = -1;
        int depth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == '{') {
                if (depth == 0) {
                    blockStart = index + 1;
                }
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0 && blockStart >= 0) {
                    blocks.add(source.substring(blockStart, index));
                    blockStart = -1;
                }
            }
        }
        return blocks;
    }

    private static Map<String, String> parseDelimitedFields(String block) {
        if (block == null || block.isBlank()) {
            return Map.of();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (String segment : splitTopLevel(block, ',')) {
            int delimiter = delimiterIndex(segment);
            if (delimiter <= 0) {
                continue;
            }

            String key = normalizeFieldKey(segment.substring(0, delimiter));
            String value = stripQuotes(segment.substring(delimiter + 1));
            if (!key.isBlank() && !value.isBlank()) {
                fields.putIfAbsent(key, value);
            }
        }
        return fields;
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> segments = new ArrayList<>();
        StringBuilder segment = new StringBuilder();
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quoted) {
                segment.append(character);
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                segment.append(character);
                continue;
            }
            if (character == '{') {
                braceDepth++;
            } else if (character == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (character == '[') {
                bracketDepth++;
            } else if (character == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }

            if (character == delimiter && braceDepth == 0 && bracketDepth == 0) {
                segments.add(segment.toString());
                segment.setLength(0);
            } else {
                segment.append(character);
            }
        }

        segments.add(segment.toString());
        return segments;
    }

    private static int delimiterIndex(String segment) {
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == '{') {
                braceDepth++;
            } else if (character == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (character == '[') {
                bracketDepth++;
            } else if (character == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            } else if ((character == '=' || character == ':') && braceDepth == 0 && bracketDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int findMatching(String source, int openIndex, char open, char close) {
        if (openIndex < 0 || openIndex >= source.length() || source.charAt(openIndex) != open) {
            return -1;
        }

        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        for (int index = openIndex; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == open) {
                depth++;
            } else if (character == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int trimTrailingDelimiter(String source, int start, int end) {
        int trimmedEnd = end;
        while (trimmedEnd > start) {
            char character = source.charAt(trimmedEnd - 1);
            if (character == ',' || Character.isWhitespace(character)) {
                trimmedEnd--;
            } else {
                break;
            }
        }
        return trimmedEnd;
    }

    private static String findLooseValue(String nativeStatus, String key) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:^|[\\s,{])" + Pattern.quote(key) + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;}\\]]+)"
        );
        Matcher matcher = pattern.matcher(nativeStatus);
        if (!matcher.find()) {
            return "";
        }
        return stripQuotes(matcher.group(1));
    }

    private static String firstLooseValue(String nativeStatus, String... keys) {
        if (keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = findLooseValue(nativeStatus, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static void putMissing(Map<String, String> target, Map<String, String> source) {
        for (Map.Entry<String, String> entry : source.entrySet()) {
            target.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static void copyMissing(Map<String, String> target, Map<String, String> source, String sourceKey, String targetKey) {
        String value = source.get(sourceKey);
        if (value != null && !value.isBlank()) {
            target.putIfAbsent(targetKey, value);
        }
    }

    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.putIfAbsent(key, value);
        }
    }

    private static Long firstLong(String... values) {
        for (String value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Integer firstInt(String... values) {
        for (String value : values) {
            Long parsed = parseLong(value);
            if (parsed == null) {
                continue;
            }
            try {
                return Math.toIntExact(parsed);
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
        return null;
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

    private static boolean hasAnyStatus(
            Long generation,
            Integer advertisedDispatches,
            Integer payloadDispatches,
            Integer enabledDispatches,
            Integer disabledDispatches,
            String generationRange,
            Map<String, LightingDispatchStageTelemetryStatus> stages
    ) {
        return generation != null
                || advertisedDispatches != null
                || payloadDispatches != null
                || enabledDispatches != null
                || disabledDispatches != null
                || (generationRange != null && !generationRange.isBlank())
                || (stages != null && !stages.isEmpty());
    }

    private static void append(StringBuilder label, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (label.length() > 0) {
            label.append(' ');
        }
        label.append(key).append('=').append(value);
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

    private static String valueOrUnknown(Integer value) {
        return value == null ? "?" : Integer.toString(value);
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String cleaned = stripQuotes(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private static boolean isKnownStageId(String stageId) {
        return switch (stageId) {
            case "direct_lighting", "diffuse_gi", "low_res_gi", "low_resolution_gi", "gi",
                    "denoise", "shader_denoise", "edge_aware_denoise", "diffuse_gi_denoise",
                    "composite", "final_composite", "cache", "radiance_cache",
                    "sparse_radiance_cache", "sparse_voxel_radiance_cache",
                    "gbuffer", "g_buffer", "gbuffer_depth", "g_buffer_depth", "depth_sampling",
                    "shadow_map", "shadowmap", "voxel_tracing", "voxel_ray_tracing",
                    "ray_traced_lighting", "traced_lighting", "hybrid_tracing",
                    "advanced_lighting" -> true;
            default -> false;
        };
    }

    private static String normalizeLooseStageFieldKey(String value) {
        return switch (normalizeFieldKey(value)) {
            case "active" -> "enabled";
            case "generation", "dispatch_generation", "stage_generation" -> "last_generation";
            case "dispatch", "dispatch_groups", "groups" -> "last_dispatch";
            case "rays", "ray_count" -> "last_rays";
            case "samples", "sample_count", "last_sample_count" -> "last_samples";
            case "candidates", "candidate_count", "last_candidate_count", "shadow_candidates",
                    "shadow_candidate_count", "direct_shadow_candidates" -> "last_candidates";
            case "budgeted_shadow_candidates" -> "budgeted_shadow_candidate_count";
            case "section_snapshots" -> "section_snapshot_count";
            case "payload_frame_index" -> "payload_frame";
            case "payload_metadata_only" -> "metadata_only";
            case "gi_cpu_output_generated", "native_gi_output_generated" -> "cpu_output_generated";
            case "gi_output_width", "native_gi_output_width" -> "output_width";
            case "gi_output_height", "native_gi_output_height" -> "output_height";
            case "gi_output_pixels", "gi_output_pixel_count", "native_gi_output_pixels",
                    "native_gi_output_pixel_count" -> "output_pixels";
            case "gi_output_energy", "native_gi_output_energy" -> "output_energy";
            case "gi_output_checksum", "native_gi_output_checksum" -> "output_checksum";
            case "raw_input_ready", "input_source_ready", "raw_gi_ready", "raw_diffuse_gi_ready",
                    "raw_diffuse_gi_input_ready", "denoise_raw_source_ready", "denoise_raw_input_ready" -> "raw_source_ready";
            case "cpu_denoise_output_ready", "cpu_denoised_output_ready", "cpu_readback_denoise_ready",
                    "cpu_readback_denoised_output_ready", "denoise_cpu_ready", "denoise_cpu_output_ready" -> "cpu_denoise_ready";
            case "shader_denoise_intent", "shader_denoise_planned", "shader_denoise_enabled",
                    "shader_denoise_contract_ready", "denoise_shader_intended",
                    "denoise_shader_planned" -> "shader_denoise_intended";
            case "shader_denoise_output_ready", "real_shader_gi_output", "gpu_denoise_output_ready", "gpu_denoise_output",
                    "shader_denoise_ready" -> "shader_output_ready";
            case "real_denoise_shader_output", "real_shader_denoise_output",
                    "real_shader_denoise_output_proven" -> "real_shader_denoise_output";
            case "shader_denoise_output_image_candidate_ready", "shader_output_image_candidate",
                    "shader_output_image_candidate_present" -> "shader_output_image_candidate_ready";
            case "shader_denoise_output_image_candidate_cpu_staged" -> "shader_output_image_candidate_cpu_staged";
            case "shader_denoise_output_image_candidate_non_gpu" -> "shader_output_image_candidate_non_gpu";
            case "shader_denoise_output_image_candidate_size",
                    "shader_output_image_candidate_size" -> "shader_output_image_candidate_dimensions";
            case "shader_denoise_output_image_candidate_pixels" -> "shader_output_image_candidate_pixels";
            case "shader_denoise_output_image_candidate_bytes" -> "shader_output_image_candidate_bytes";
            case "shader_denoise_output_image_candidate_checksum" -> "shader_output_image_candidate_checksum";
            case "shader_denoise_output_image_candidate_marker",
                    "shader_output_image_candidate_source" -> "shader_output_image_candidate_marker";
            case "shader_denoise_output_image_blocker", "shader_output_image_candidate_blocker",
                    "shader_output_image_candidate_boundary",
                    "shader_denoise_output_blocker_reason" -> "shader_output_blocker_reason";
            case "shader_denoise_output_readiness_marker",
                    "shader_denoise_output_readiness_label" -> "shader_output_readiness_label";
            case "shader_denoise_dispatch_prepared", "denoise_shader_dispatch_prepared",
                    "real_shader_denoise_dispatch_prepared", "gpu_denoise_dispatch_prepared" -> "shader_dispatch_prepared";
            case "shader_denoise_output_image_ready", "real_shader_output_image_ready",
                    "real_shader_denoise_output_image_ready", "gpu_denoise_output_image_ready" -> "shader_output_image_ready";
            case "shader_denoise_output_material_ready", "real_shader_output_material_ready",
                    "real_shader_denoise_output_material_ready",
                    "gpu_denoise_output_material_ready" -> "shader_output_material_ready";
            case "shader_denoise_generated_output", "real_shader_generated_output",
                    "real_shader_denoise_generated_output", "real_denoise_shader_generated_output",
                    "gpu_denoise_generated_output" -> "shader_generated_output";
            case "shader_denoise_generated_output_evidence", "real_shader_denoise_output_evidence",
                    "shader_denoise_output_evidence", "shader_generated_denoise_output_proven" ->
                    "shader_generated_denoise_output_evidence";
            case "shader_denoise_generated_output_marker", "real_shader_denoise_output_marker",
                    "shader_denoise_output_evidence_marker" -> "shader_generated_denoise_output_marker";
            case "public_mojang_shader_output_attempted", "public_mojang_visual_output_attempted",
                    "public_mojang_denoise_visual_output_attempted",
                    "shader_visual_output_attempted" -> "public_mojang_shader_visual_output_attempted";
            case "public_mojang_shader_output_submitted", "public_mojang_visual_output_submitted",
                    "public_mojang_denoise_visual_output_submitted",
                    "shader_visual_output_submitted" -> "public_mojang_shader_visual_output_submitted";
            case "public_mojang_shader_output_ready", "public_mojang_visual_output_ready",
                    "public_mojang_denoise_visual_output_ready",
                    "shader_visual_output_ready" -> "public_mojang_shader_visual_output_ready";
            case "cpu_readback_fallback_used", "cpu_denoise_readback_fallback",
                    "shader_denoise_cpu_readback_fallback",
                    "denoise_cpu_readback_fallback" -> "cpu_readback_fallback";
            case "shader_denoise_blocker", "shader_blockers", "shader_blocker", "denoise_blockers",
                    "output_blockers", "blockers", "blocker" -> "shader_denoise_blockers";
            case "edge_reject_count", "edge_rejections", "edge_rejected", "edge_rejected_count",
                    "denoise_edge_rejections" -> "edge_rejection_count";
            case "history_reject_count", "history_rejections", "history_rejected",
                    "history_rejected_count", "temporal_history_rejected",
                    "temporal_history_rejection_count", "last_history_rejected" -> "history_rejection_count";
            case "temporal_stable_pixel_count", "stable_pixels", "stable_pixel_count",
                    "temporal_pixels_stable" -> "temporal_stable_pixels";
            case "temporal_unstable_pixel_count", "unstable_pixels", "unstable_pixel_count",
                    "temporal_pixels_unstable" -> "temporal_unstable_pixels";
            case "frame_delta_pixel_count", "temporal_frame_delta_pixels", "temporal_changed_pixels",
                    "changed_pixels", "denoised_output_changed_pixels" -> "frame_delta_pixels";
            case "frame_delta_mean_abs_delta", "temporal_frame_delta_mean_delta", "temporal_mean_abs_delta",
                    "mean_abs_delta", "denoised_output_mean_abs_delta" -> "frame_delta_mean_delta";
            case "previous_denoised_output_checksum", "previous_frame_checksum", "previous_checksum",
                    "temporal_previous_checksum" -> "previous_output_checksum";
            case "current_denoised_output_checksum", "current_frame_checksum", "current_checksum",
                    "temporal_current_checksum" -> "current_output_checksum";
            case "avg_history_confidence", "temporal_confidence", "last_history_confidence",
                    "temporal_history_confidence" -> "history_confidence";
            case "temporal_flicker_score", "denoise_flicker_score",
                    "temporal_instability_score" -> "flicker_score";
            case "ghosting_risk_marker", "temporal_ghosting_risk", "temporal_ghosting_risk_marker",
                    "temporal_ghosting_marker" -> "ghosting_risk";
            case "temporal_stability_ready", "temporal_history_ready", "history_ready",
                    "temporal_proof_ready", "temporal_acceptance_ready" -> "temporal_ready";
            case "temporal_stability_readiness_marker", "temporal_history_marker",
                    "last_temporal_history_marker", "history_marker",
                    "temporal_marker" -> "temporal_readiness_marker";
            case "source_id", "denoise_source", "denoise_source_identity" -> "source_identity";
            case "proof_boundary", "boundary", "quality_boundary", "shader_denoise_evidence_boundary",
                    "shader_denoise_boundary", "denoise_evidence_boundary",
                    "denoise_quality_boundary" -> "evidence_boundary";
            case "physical_gi_scene_link_score", "gi_physical_scene_link_score",
                    "scene_link_score", "scene_linked_score" -> "physical_scene_link_score";
            case "physical_gi_output_checksum", "gi_physical_output_checksum",
                    "native_physical_output_checksum" -> "physical_output_checksum";
            case "physical_gi_scene_linked", "scene_linked_physical",
                    "gi_scene_linked" -> "physical_scene_linked";
            case "physical_gi_surface_contribution", "surface_physical_contribution",
                    "physical_contribution" -> "physical_surface_contribution";
            case "localized_spill", "emissive_spill_localized",
                    "emissive_spill" -> "localized_emissive_spill";
            case "colored_bounce_hue_shift", "colored_bounce",
                    "bounce_hue_shifted" -> "hue_shifted_bounce";
            case "contact_shadows", "local_occlusion_darkening",
                    "local_occlusion" -> "contact_shadow_darkening";
            case "physical_final_composite_ready", "final_composite_physical_ready",
                    "final_physical_composite" -> "final_physical_composite_ready";
            case "physical_preview_fallback_contribution",
                    "cpu_preview_fallback_contribution" -> "preview_fallback_contribution";
            case "gbuffer_depth_sampling_evidence", "depth_sampling_evidence", "real_gbuffer_depth_sampling",
                    "real_depth_texture_sampled", "g_buffer_depth_sampled",
                    "gbuffer_depth_sampled" -> "g_buffer_depth_sampling_evidence";
            case "gbuffer_depth_texture_sampled", "depth_texture_sampled" -> "g_buffer_depth_texture_sampled";
            case "gbuffer_depth_metadata_only", "depth_sampling_metadata_only",
                    "g_buffer_depth_sampling_metadata_only",
                    "depth_texture_metadata_only" -> "g_buffer_depth_metadata_only";
            case "gbuffer_depth_sample_count", "depth_sample_count", "g_buffer_depth_samples",
                    "gbuffer_depth_samples", "depth_samples" -> "g_buffer_depth_sample_count";
            case "gbuffer_depth_sampling_marker", "depth_sampling_marker",
                    "depth_texture_sample_marker" -> "g_buffer_depth_sampling_marker";
            case "real_shadow_map", "shadow_map_evidence", "shadowmap_evidence",
                    "real_shadow_map_ready", "shadow_map_ready" -> "real_shadow_map_evidence";
            case "shadowmap_rendered", "real_shadow_map_rendered",
                    "shadow_map_output_rendered" -> "shadow_map_rendered";
            case "shadowmap_sampled", "real_shadow_map_sampled",
                    "shadow_map_depth_sampled" -> "shadow_map_sampled";
            case "shadowmap_metadata_only", "real_shadow_map_metadata_only",
                    "shadow_map_evidence_metadata_only" -> "shadow_map_metadata_only";
            case "shadowmap_evidence_marker", "real_shadow_map_marker",
                    "shadow_map_marker" -> "shadow_map_evidence_marker";
            case "nativeshadowmapmask", "native_shadowmap_mask", "native_shadow_map_output_mask",
                    "nativeshadowmapoutputmask", "shadow_map_mask_native", "shadowmap_mask_native",
                    "shadow_map_source_native" -> "native_shadow_map_mask";
            case "shadowmapoutputconsumed", "shadowmap_output_consumed", "native_shadow_map_consumed",
                    "nativeshadowmapconsumed", "native_shadowmap_consumed", "shadow_map_consumed",
                    "shadowmapconsumed", "shadowmap_consumed" -> "shadow_map_output_consumed";
            case "realshadowmapcomposite", "shadow_map_final_composite", "shadowmapfinalcomposite",
                    "shadowmap_final_composite", "native_shadow_map_composited",
                    "nativeshadowmapcomposited", "native_shadowmap_composited" -> "real_shadow_map_composite";
            case "screenspaceshadowdecal", "screenspace_shadow_decal", "screen_space_shadow",
                    "screenspaceshadow", "shadow_decal_screen_space" -> "screen_space_shadow_decal";
            case "lowresdirecttextureshadowproof", "lowres_direct_texture_shadow_proof",
                    "low_res_direct_texture", "cpu_direct_texture_composite", "cpudirecttexturecomposite",
                    "directlightlowrestexture", "direct_light_low_res_texture" ->
                    "low_res_direct_texture_shadow_proof";
            case "voxel_traced_lighting_consumed_evidence", "ray_traced_lighting_consumed_evidence",
                    "traced_lighting_consumed_evidence", "voxel_ray_traced_lighting_consumed",
                    "ray_traced_lighting_consumed" -> "voxel_ray_traced_lighting_consumed_evidence";
            case "traced_lighting_consumed", "real_ray_traced_lighting_consumed",
                    "real_voxel_traced_lighting_consumed" -> "real_traced_lighting_consumed";
            case "gpu_voxel_traversal_executed", "real_gpu_voxel_traversal_executed",
                    "real_ray_tracing_executed", "hardware_ray_tracing_executed" -> "real_gpu_traversal_executed";
            case "ray_traced_lighting_metadata_only", "voxel_ray_traced_lighting_metadata_only",
                    "traced_lighting_consumed_metadata_only" -> "traced_lighting_metadata_only";
            case "ray_traced_lighting_marker", "traced_lighting_consumed_marker",
                    "voxel_traced_lighting_marker" -> "voxel_ray_traced_lighting_marker";
            case "metadata_preview_rejected", "metadata_only_rejected" -> "metadata_only_proof_rejected";
            case "focus_window_rejected", "focus_window_only_rejected" -> "focus_window_capture_rejected";
            case "proof_marker_rejected", "proof_marker_source_rejected" -> "proof_marker_evidence_rejected";
            case "temporary_direct_light_substitution_rejected", "temporary_direct_source_rejected",
                    "direct_light_substitution_rejected" -> "temporary_direct_substitution_rejected";
            case "anti_rectangular_washout_passed", "washout_rejected" -> "rectangular_washout_rejected";
            case "wrong_window_capture_rejected", "window_screenshot_rejected",
                    "wrong_provenance_rejected" -> "wrong_window_screenshot_rejected";
            case "blank_surface_rejected", "blank_capture_rejected",
                    "blank_frame_rejected" -> "blank_screenshot_rejected";
            case "physical_gi_scene_marker", "scene_link_marker",
                    "physical_scene_evidence_marker" -> "physical_scene_marker";
            case "physical_gi_output_marker", "physical_output_evidence_marker" -> "physical_output_marker";
            case "localized_emissive_spill_marker", "spill_marker" -> "emissive_spill_marker";
            case "hue_shifted_bounce_marker", "bounce_marker" -> "colored_bounce_marker";
            case "contact_shadow_darkening_marker", "local_occlusion_marker" -> "contact_shadow_marker";
            case "physical_final_composite_marker",
                    "physical_composite_marker" -> "final_physical_composite_marker";
            case "physical_proof_boundary_marker", "physical_gi_proof_boundary",
                    "gi_proof_boundary_marker" -> "proof_boundary_marker";
            case "cache", "cache_counts" -> "last_cache";
            case "cache_reads", "cache_read_count" -> "cache_reads";
            case "cache_writes", "cache_write_count" -> "cache_writes";
            case "flags", "stage_flags" -> "last_flags";
            case "metadata_only" -> "metadata_only";
            case "valid" -> "validated";
            case "debug" -> "debug_overlay";
            case "native_ready", "ready", "executable" -> "ready_for_native_execution";
            case "ready_reason", "native_readiness_reason", "reason" -> "readiness_reason";
            case "recorded", "submitted_this_frame" -> "recorded_this_frame";
            case "frame", "frame_index", "last_frame_index", "dispatch_frame" -> "last_frame";
            case "size", "dimensions", "resolution" -> "last_size";
            case "io", "io_counts" -> "last_io";
            case "input_count", "last_input_count" -> "inputs";
            case "output_count", "last_output_count" -> "outputs";
            default -> normalizeFieldKey(value);
        };
    }

    private static String normalizeStageId(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalizeFieldKey(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    }

    private static Map<String, LightingDispatchStageTelemetryStatus> immutableStages(
            Map<String, LightingDispatchStageTelemetryStatus> source
    ) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
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
