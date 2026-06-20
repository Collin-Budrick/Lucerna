package net.lucerna.render.preview;

import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record FirstLightingQualityProofStatus(
        String selectedMode,
        String finalCompositeStability,
        String particleTranslucencyPreservation,
        String temporalHistoryState,
        String denoiseSourceIdentity,
        String physicalGiSourceIdentity,
        String rejectedEvidenceTypes,
        String geometryMaterialProjectionState,
        String readinessGate,
        boolean finalCompositeStableReady,
        boolean particleTranslucencyProofReady,
        boolean temporalHistoryReady,
        boolean realShaderDenoiseOutput,
        boolean cpuDenoiseFallback,
        boolean rejectedEvidenceClean,
        boolean geometryMaterialProjectionCandidateReady
) {
    public FirstLightingQualityProofStatus {
        selectedMode = normalize(selectedMode, "unknown");
        finalCompositeStability = normalize(finalCompositeStability, "unreported");
        particleTranslucencyPreservation = normalize(particleTranslucencyPreservation, "unreported");
        temporalHistoryState = normalize(temporalHistoryState, "unreported");
        denoiseSourceIdentity = normalize(denoiseSourceIdentity, "unreported");
        physicalGiSourceIdentity = normalize(physicalGiSourceIdentity, "unreported");
        rejectedEvidenceTypes = normalize(rejectedEvidenceTypes, "unreported");
        geometryMaterialProjectionState = normalize(geometryMaterialProjectionState, "unreported");
        readinessGate = normalize(readinessGate, "unreported");
    }

    public static FirstLightingQualityProofStatus fromSnapshot(
            LucernaStatusSnapshot snapshot,
            FinalCompositeModeStatus compositeStatus
    ) {
        LightingDispatchTelemetryStatus lighting = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus directStage = firstStage(lighting, "direct_lighting");
        LightingDispatchStageTelemetryStatus giStage = firstStage(
                lighting,
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        );
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                lighting,
                "denoise",
                "diffuse_gi_denoise",
                "denoised_gi",
                "round7_denoise"
        );
        LightingDispatchStageTelemetryStatus compositeStage = firstStage(
                lighting,
                "composite",
                "final_composite",
                "round7_composite"
        );

        boolean finalCompositeMode = compositeStatus.finalCompositeVisualMode();
        boolean finalCompositeStableReady = finalCompositeMode
                && snapshot.frameLifecycle().framePassAttachable()
                && snapshot.frameLifecycle().lightingSubmitted()
                && !snapshot.frameLifecycle().resizePending()
                && snapshot.frameConstantsFresh()
                && sourceReady(directStage)
                && sourceReady(giStage)
                && sourceReady(denoiseStage);
        boolean particleTranslucencyProofReady = explicitTruthy(
                compositeStage,
                "particle_translucency_preserved",
                "particles_translucency_preserved",
                "translucency_preserved",
                "particles_preserved",
                "particle_translucency_proof_ready"
        );
        boolean temporalHistoryReady = explicitTruthy(
                denoiseStage,
                giStage,
                "temporal_history_ready",
                "history_inputs_complete",
                "temporal_reuse_allowed",
                "history_available"
        );
        boolean realShaderDenoiseOutput = explicitTruthy(
                denoiseStage,
                "real_denoise_shader_output",
                "realDenoiseShaderOutput",
                "shader_output",
                "shaderDenoiseOutput",
                "gpu_denoise_output"
        );
        boolean cpuDenoiseFallback = explicitTruthy(
                denoiseStage,
                "cpu_output_fallback",
                "cpuFallback",
                "cpu_denoise_fallback",
                "cpu_denoised_output",
                "cpu_denoised_diffuse_gi"
        ) || explicitFalse(
                denoiseStage,
                "real_denoise_shader_output",
                "realDenoiseShaderOutput",
                "shader_output",
                "shaderDenoiseOutput",
                "gpu_denoise_output"
        );

        String focusWindowEvidence = firstDetail(
                compositeStage,
                giStage,
                directStage,
                "focus_window_only",
                "focus_window_source",
                "focus_only",
                "focus_window"
        );
        String proofMarkerEvidence = firstDetail(
                compositeStage,
                giStage,
                directStage,
                "proof_marker",
                "proof_marker_source",
                "proof_only",
                "proof_source"
        );
        String metadataPreviewEvidence = firstDetail(
                compositeStage,
                giStage,
                directStage,
                "metadata_only",
                "metadata_preview",
                "preview_metadata_only",
                "metadata_source"
        );
        String directSubstitutionEvidence = firstDetail(
                giStage,
                denoiseStage,
                "temporary_direct_source",
                "temporary_direct_light_source",
                "uses_direct_light_payload",
                "using_direct_light_payload",
                "direct_light_payload_source"
        );
        String rectangularWashoutEvidence = firstDetail(
                compositeStage,
                giStage,
                denoiseStage,
                "rectangular_washout",
                "rectangularWashout",
                "hard_rectangle",
                "full_screen_washout"
        );
        String antiRectangularWashoutPassed = firstDetail(
                compositeStage,
                giStage,
                denoiseStage,
                "washout_rejected",
                "anti_rectangular_washout_passed"
        );
        boolean rectangularWashoutClean = !truthyOrUnknown(rectangularWashoutEvidence)
                || truthy(antiRectangularWashoutPassed);
        boolean rejectedEvidenceClean = !truthyOrUnknown(focusWindowEvidence)
                && !truthyOrUnknown(proofMarkerEvidence)
                && !truthyOrUnknown(metadataPreviewEvidence)
                && !truthyOrUnknown(directSubstitutionEvidence)
                && rectangularWashoutClean;
        boolean geometryMaterialProjectionCandidateReady = finalCompositeStableReady
                && rejectedEvidenceClean
                && sourceReady(directStage)
                && sourceReady(giStage)
                && sourceReady(denoiseStage);

        String finalCompositeStability = "mode=" + compositeStatus.statusKey()
                + " framePassAttachable=" + yesNo(snapshot.frameLifecycle().framePassAttachable())
                + " lightingSubmitted=" + yesNo(snapshot.frameLifecycle().lightingSubmitted())
                + " resizePending=" + yesNo(snapshot.frameLifecycle().resizePending())
                + " frameConstantsFresh=" + yesNo(snapshot.frameConstantsFresh())
                + " direct=" + readyLabel(directStage)
                + " rawGI=" + readyLabel(giStage)
                + " denoisedGI=" + readyLabel(denoiseStage);
        String particleTranslucencyPreservation = "required=true telemetry="
                + valueOrUnknown(firstDetail(
                compositeStage,
                "particle_translucency_preserved",
                "particles_translucency_preserved",
                "translucency_preserved",
                "particles_preserved",
                "particle_translucency_proof_ready"
        )) + " boundary=must compare particles/translucency with HUD-safe final composite";
        String temporalHistoryState = "required=true ready=" + yesNo(temporalHistoryReady)
                + " historyAvailable=" + valueOrUnknown(firstDetail(
                denoiseStage,
                giStage,
                "history_inputs_complete",
                "temporal_reuse_allowed",
                "history_available"
        )) + " accepted=" + valueOrUnknown(firstDetail(
                denoiseStage,
                giStage,
                "history_accepted",
                "historyAccepted",
                "history_accepted_pixels"
        )) + " rejected=" + valueOrUnknown(firstDetail(
                denoiseStage,
                giStage,
                "history_rejected",
                "historyRejected",
                "history_rejected_pixels",
                "disocclusion_rejected"
        ));
        String denoiseSourceIdentity = "realShaderDenoiseOutput=" + yesNo(realShaderDenoiseOutput)
                + " cpuFallback=" + yesNo(cpuDenoiseFallback)
                + " source=" + valueOrUnknown(firstDetail(
                denoiseStage,
                "output_source",
                "output_source_label",
                "denoise_source",
                "denoised_output_source",
                "native_denoise_output_source"
        )) + " boundary=CPU fallback is proof source only, not shader denoise quality";
        String physicalGiSourceIdentity = "sceneLinked=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.SCENE_LINKED))
                + " surfaceContribution=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.SURFACE_CONTRIBUTION))
                + " sceneLinkScore=" + valueOrUnknown(longDetail(giStage, PhysicalGiLongField.SCENE_LINK_SCORE))
                + " physicalOutputChecksum=" + valueOrUnknown(longDetail(giStage, PhysicalGiLongField.OUTPUT_CHECKSUM))
                + " previewFallback=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.PREVIEW_FALLBACK))
                + " physicalSceneMarker=" + valueOrUnknown(stringDetail(giStage, PhysicalGiStringField.SCENE_MARKER))
                + " physicalOutputMarker=" + valueOrUnknown(stringDetail(giStage, PhysicalGiStringField.OUTPUT_MARKER))
                + " proofBoundaryMarker=" + valueOrUnknown(stringDetail(giStage, PhysicalGiStringField.PROOF_BOUNDARY_MARKER))
                + " boundary=scene-linked CPU/readback physical metrics are evidence fields only; real physical GI tracing quality remains open";
        String rejectedEvidenceTypes = "metadataPreview=" + valueOrUnknown(metadataPreviewEvidence)
                + " focusWindowOnly=" + valueOrUnknown(focusWindowEvidence)
                + " proofMarker=" + valueOrUnknown(proofMarkerEvidence)
                + " directSubstitution=" + valueOrUnknown(directSubstitutionEvidence)
                + " rectangularWashout=" + valueOrUnknown(rectangularWashoutEvidence)
                + " antiRectangularWashoutPassed=" + valueOrUnknown(antiRectangularWashoutPassed)
                + " metadataOnlyProofRejected=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.METADATA_ONLY_PROOF_REJECTED))
                + " focusWindowCaptureRejected=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.FOCUS_WINDOW_CAPTURE_REJECTED))
                + " proofMarkerEvidenceRejected=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.PROOF_MARKER_EVIDENCE_REJECTED))
                + " temporaryDirectSubstitutionRejected=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.TEMPORARY_DIRECT_SUBSTITUTION_REJECTED))
                + " rectangularWashoutRejected=" + valueOrUnknown(boolDetail(giStage, PhysicalGiBooleanField.RECTANGULAR_WASHOUT_REJECTED))
                + " clean=" + yesNo(rejectedEvidenceClean);
        String geometryMaterialProjectionState = "candidateReady=" + yesNo(geometryMaterialProjectionCandidateReady)
                + " boundary=\"" + compositeStatus.geometryMaterialProjectionBoundary() + "\""
                + " surfaceProjection=" + valueOrUnknown(firstDetail(
                compositeStage,
                giStage,
                denoiseStage,
                "surface_projection",
                "surfaceProjection",
                "geometry_material_projection",
                "geometryMaterialProjection"
        )) + " materialAware=" + valueOrUnknown(firstDetail(
                compositeStage,
                giStage,
                denoiseStage,
                "material_aware",
                "materialAware",
                "material_response",
                "materialResponse"
        )) + " geometryAware=" + valueOrUnknown(firstDetail(
                compositeStage,
                giStage,
                denoiseStage,
                "geometry_aware",
                "geometryAware",
                "surface_samples",
                "surfaceSamples"
        )) + " quality=pending-controller-proof";
        String readinessGate = "ready=" + yesNo(finalCompositeStableReady
                && particleTranslucencyProofReady
                && temporalHistoryReady
                && rejectedEvidenceClean)
                + " requires=stable-final-composite,particle-translucency-preservation,temporal-history,"
                + "explicit-shader-vs-cpu-denoise-source,rejected-preview-evidence,"
                + "scene-shaped-geometry-material-aware-surface-proof";

        return new FirstLightingQualityProofStatus(
                compositeStatus.statusKey(),
                finalCompositeStability,
                particleTranslucencyPreservation,
                temporalHistoryState,
                denoiseSourceIdentity,
                physicalGiSourceIdentity,
                rejectedEvidenceTypes,
                geometryMaterialProjectionState,
                readinessGate,
                finalCompositeStableReady,
                particleTranslucencyProofReady,
                temporalHistoryReady,
                realShaderDenoiseOutput,
                cpuDenoiseFallback,
                rejectedEvidenceClean,
                geometryMaterialProjectionCandidateReady
        );
    }

    public String summaryLine() {
        return "mode=" + this.selectedMode
                + " finalStable=" + yesNo(this.finalCompositeStableReady)
                + " particlesTranslucency=" + yesNo(this.particleTranslucencyProofReady)
                + " temporalHistory=" + yesNo(this.temporalHistoryReady)
                + " realShaderDenoise=" + yesNo(this.realShaderDenoiseOutput)
                + " cpuFallback=" + yesNo(this.cpuDenoiseFallback)
                + " denoiseClass=" + this.denoiseSourceClass()
                + " rejectedEvidenceClean=" + yesNo(this.rejectedEvidenceClean)
                + " geometryMaterialProjectionCandidate=" + yesNo(this.geometryMaterialProjectionCandidateReady);
    }

    public String denoiseSourceClass() {
        if (this.realShaderDenoiseOutput) {
            return "shader-denoised-gi";
        }
        if (this.cpuDenoiseFallback) {
            return "cpu-denoised-gi";
        }
        return "denoised-gi-missing";
    }

    public String finalCompositeQualityBoundary() {
        if (!this.finalCompositeStableReady) {
            return "blocked:final-composite-not-stable";
        }
        if (!this.rejectedEvidenceClean) {
            return "blocked:metadata-focus-proof-marker-direct-substitution-or-washout-evidence";
        }
        if (!this.geometryMaterialProjectionCandidateReady) {
            return "candidate:source-readiness-present;geometry-material-projection-proof-pending";
        }
        if (!this.temporalHistoryReady) {
            return "candidate:geometry-material-projection-present;temporal-stability-proof-pending";
        }
        if (!this.realShaderDenoiseOutput) {
            return "partial:cpu-denoised-preview-quality;real-shader-denoise-pending";
        }
        return "ready:shader-denoised-final-composite-candidate";
    }

    public String sourceAuthenticityGate() {
        return "denoiseClass=" + this.denoiseSourceClass()
                + ",rejectedEvidenceClean=" + yesNo(this.rejectedEvidenceClean)
                + ",geometryMaterialProjectionCandidate=" + yesNo(this.geometryMaterialProjectionCandidateReady)
                + ",temporalHistoryReady=" + yesNo(this.temporalHistoryReady)
                + ",qualityBoundary=" + this.finalCompositeQualityBoundary();
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = normalize(prefix, "round7.firstLightingQuality");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".summary", this.summaryLine());
        fields.put(normalizedPrefix + ".sourceAuthenticityGate", this.sourceAuthenticityGate());
        fields.put(normalizedPrefix + ".finalCompositeQualityBoundary", this.finalCompositeQualityBoundary());
        fields.put(normalizedPrefix + ".denoiseSourceClass", this.denoiseSourceClass());
        fields.put(normalizedPrefix + ".selectedMode", this.selectedMode);
        fields.put(normalizedPrefix + ".finalCompositeStability", this.finalCompositeStability);
        fields.put(normalizedPrefix + ".particleTranslucencyPreservation", this.particleTranslucencyPreservation);
        fields.put(normalizedPrefix + ".temporalHistoryState", this.temporalHistoryState);
        fields.put(normalizedPrefix + ".denoiseSourceIdentity", this.denoiseSourceIdentity);
        fields.put(normalizedPrefix + ".physicalGiSourceIdentity", this.physicalGiSourceIdentity);
        fields.put(normalizedPrefix + ".rejectedEvidenceTypes", this.rejectedEvidenceTypes);
        fields.put(normalizedPrefix + ".geometryMaterialProjectionState", this.geometryMaterialProjectionState);
        fields.put(normalizedPrefix + ".readinessGate", this.readinessGate);
        fields.put(normalizedPrefix + ".finalCompositeStableReady", Boolean.toString(this.finalCompositeStableReady));
        fields.put(normalizedPrefix + ".particleTranslucencyProofReady", Boolean.toString(this.particleTranslucencyProofReady));
        fields.put(normalizedPrefix + ".temporalHistoryReady", Boolean.toString(this.temporalHistoryReady));
        fields.put(normalizedPrefix + ".realShaderDenoiseOutput", Boolean.toString(this.realShaderDenoiseOutput));
        fields.put(normalizedPrefix + ".cpuDenoiseFallback", Boolean.toString(this.cpuDenoiseFallback));
        fields.put(normalizedPrefix + ".rejectedEvidenceClean", Boolean.toString(this.rejectedEvidenceClean));
        fields.put(normalizedPrefix + ".geometryMaterialProjectionCandidateReady", Boolean.toString(this.geometryMaterialProjectionCandidateReady));
        return Collections.unmodifiableMap(fields);
    }

    private static boolean sourceReady(LightingDispatchStageTelemetryStatus stage) {
        return stage != null
                && (Boolean.TRUE.equals(stage.cpuOutputGenerated())
                || Boolean.TRUE.equals(stage.readyForNativeExecution())
                || truthy(stage.details().get("source_ready"))
                || truthy(stage.details().get("texture_ready"))
                || truthy(stage.details().get("output_ready")));
    }

    private static String readyLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "not_reported";
        }
        return sourceReady(stage) ? "ready" : "missing";
    }

    private static LightingDispatchStageTelemetryStatus firstStage(
            LightingDispatchTelemetryStatus lighting,
            String... stageIds
    ) {
        if (lighting == null || stageIds == null) {
            return null;
        }
        for (String stageId : stageIds) {
            LightingDispatchStageTelemetryStatus stage = lighting.stages().get(stageId);
            if (stage != null) {
                return stage;
            }
        }
        return null;
    }

    private static boolean explicitTruthy(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            String... keys
    ) {
        return truthy(firstDetail(primary, secondary, keys));
    }

    private static boolean explicitTruthy(LightingDispatchStageTelemetryStatus stage, String... keys) {
        return truthy(firstDetail(stage, keys));
    }

    private static boolean explicitFalse(LightingDispatchStageTelemetryStatus stage, String... keys) {
        return falsey(firstDetail(stage, keys));
    }

    private static String firstDetail(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            String... keys
    ) {
        String value = firstDetail(primary, keys);
        return value.isBlank() ? firstDetail(secondary, keys) : value;
    }

    private static String firstDetail(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            LightingDispatchStageTelemetryStatus tertiary,
            String... keys
    ) {
        String value = firstDetail(primary, secondary, keys);
        return value.isBlank() ? firstDetail(tertiary, keys) : value;
    }

    private static String firstDetail(LightingDispatchStageTelemetryStatus stage, String... keys) {
        if (stage == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = stage.details().get(normalizeKey(key));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean truthyOrUnknown(String value) {
        if (value == null || value.isBlank() || "?".equals(value)) {
            return false;
        }
        return truthy(value) || !falsey(value);
    }

    private static boolean truthy(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static boolean falsey(String value) {
        return "0".equals(value) || "false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String boolDetail(LightingDispatchStageTelemetryStatus stage, PhysicalGiBooleanField field) {
        if (stage == null || field == null) {
            return "";
        }
        Boolean value = switch (field) {
            case SCENE_LINKED -> stage.physicalSceneLinked();
            case SURFACE_CONTRIBUTION -> stage.physicalSurfaceContribution();
            case PREVIEW_FALLBACK -> stage.previewFallbackContribution();
            case METADATA_ONLY_PROOF_REJECTED -> stage.metadataOnlyProofRejected();
            case FOCUS_WINDOW_CAPTURE_REJECTED -> stage.focusWindowCaptureRejected();
            case PROOF_MARKER_EVIDENCE_REJECTED -> stage.proofMarkerEvidenceRejected();
            case TEMPORARY_DIRECT_SUBSTITUTION_REJECTED -> stage.temporaryDirectSubstitutionRejected();
            case RECTANGULAR_WASHOUT_REJECTED -> stage.rectangularWashoutRejected();
        };
        return value == null ? "" : Boolean.toString(value);
    }

    private static String longDetail(LightingDispatchStageTelemetryStatus stage, PhysicalGiLongField field) {
        if (stage == null || field == null) {
            return "";
        }
        Long value = switch (field) {
            case SCENE_LINK_SCORE -> stage.physicalSceneLinkScore();
            case OUTPUT_CHECKSUM -> stage.physicalOutputChecksum();
        };
        return value == null ? "" : Long.toString(value);
    }

    private static String stringDetail(LightingDispatchStageTelemetryStatus stage, PhysicalGiStringField field) {
        if (stage == null || field == null) {
            return "";
        }
        return switch (field) {
            case SCENE_MARKER -> stage.physicalSceneMarker();
            case OUTPUT_MARKER -> stage.physicalOutputMarker();
            case PROOF_BOUNDARY_MARKER -> stage.proofBoundaryMarker();
        };
    }

    private static String normalizeKey(String value) {
        return normalize(value, "").toLowerCase().replace('-', '_').replace('.', '_');
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private enum PhysicalGiBooleanField {
        SCENE_LINKED,
        SURFACE_CONTRIBUTION,
        PREVIEW_FALLBACK,
        METADATA_ONLY_PROOF_REJECTED,
        FOCUS_WINDOW_CAPTURE_REJECTED,
        PROOF_MARKER_EVIDENCE_REJECTED,
        TEMPORARY_DIRECT_SUBSTITUTION_REJECTED,
        RECTANGULAR_WASHOUT_REJECTED
    }

    private enum PhysicalGiLongField {
        SCENE_LINK_SCORE,
        OUTPUT_CHECKSUM
    }

    private enum PhysicalGiStringField {
        SCENE_MARKER,
        OUTPUT_MARKER,
        PROOF_BOUNDARY_MARKER
    }
}
