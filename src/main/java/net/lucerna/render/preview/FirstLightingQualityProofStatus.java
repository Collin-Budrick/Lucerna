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
        String rejectedEvidenceTypes,
        String readinessGate,
        boolean finalCompositeStableReady,
        boolean particleTranslucencyProofReady,
        boolean temporalHistoryReady,
        boolean realShaderDenoiseOutput,
        boolean cpuDenoiseFallback,
        boolean rejectedEvidenceClean
) {
    public FirstLightingQualityProofStatus {
        selectedMode = normalize(selectedMode, "unknown");
        finalCompositeStability = normalize(finalCompositeStability, "unreported");
        particleTranslucencyPreservation = normalize(particleTranslucencyPreservation, "unreported");
        temporalHistoryState = normalize(temporalHistoryState, "unreported");
        denoiseSourceIdentity = normalize(denoiseSourceIdentity, "unreported");
        rejectedEvidenceTypes = normalize(rejectedEvidenceTypes, "unreported");
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
        boolean rejectedEvidenceClean = !truthyOrUnknown(focusWindowEvidence)
                && !truthyOrUnknown(proofMarkerEvidence)
                && !truthyOrUnknown(metadataPreviewEvidence)
                && !truthyOrUnknown(directSubstitutionEvidence);

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
        String rejectedEvidenceTypes = "metadataPreview=" + valueOrUnknown(metadataPreviewEvidence)
                + " focusWindowOnly=" + valueOrUnknown(focusWindowEvidence)
                + " proofMarker=" + valueOrUnknown(proofMarkerEvidence)
                + " directSubstitution=" + valueOrUnknown(directSubstitutionEvidence)
                + " clean=" + yesNo(rejectedEvidenceClean);
        String readinessGate = "ready=" + yesNo(finalCompositeStableReady
                && particleTranslucencyProofReady
                && temporalHistoryReady
                && rejectedEvidenceClean)
                + " requires=stable-final-composite,particle-translucency-preservation,temporal-history,"
                + "explicit-shader-vs-cpu-denoise-source,rejected-preview-evidence";

        return new FirstLightingQualityProofStatus(
                compositeStatus.statusKey(),
                finalCompositeStability,
                particleTranslucencyPreservation,
                temporalHistoryState,
                denoiseSourceIdentity,
                rejectedEvidenceTypes,
                readinessGate,
                finalCompositeStableReady,
                particleTranslucencyProofReady,
                temporalHistoryReady,
                realShaderDenoiseOutput,
                cpuDenoiseFallback,
                rejectedEvidenceClean
        );
    }

    public String summaryLine() {
        return "mode=" + this.selectedMode
                + " finalStable=" + yesNo(this.finalCompositeStableReady)
                + " particlesTranslucency=" + yesNo(this.particleTranslucencyProofReady)
                + " temporalHistory=" + yesNo(this.temporalHistoryReady)
                + " realShaderDenoise=" + yesNo(this.realShaderDenoiseOutput)
                + " cpuFallback=" + yesNo(this.cpuDenoiseFallback)
                + " rejectedEvidenceClean=" + yesNo(this.rejectedEvidenceClean);
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = normalize(prefix, "round7.firstLightingQuality");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".summary", this.summaryLine());
        fields.put(normalizedPrefix + ".selectedMode", this.selectedMode);
        fields.put(normalizedPrefix + ".finalCompositeStability", this.finalCompositeStability);
        fields.put(normalizedPrefix + ".particleTranslucencyPreservation", this.particleTranslucencyPreservation);
        fields.put(normalizedPrefix + ".temporalHistoryState", this.temporalHistoryState);
        fields.put(normalizedPrefix + ".denoiseSourceIdentity", this.denoiseSourceIdentity);
        fields.put(normalizedPrefix + ".rejectedEvidenceTypes", this.rejectedEvidenceTypes);
        fields.put(normalizedPrefix + ".readinessGate", this.readinessGate);
        fields.put(normalizedPrefix + ".finalCompositeStableReady", Boolean.toString(this.finalCompositeStableReady));
        fields.put(normalizedPrefix + ".particleTranslucencyProofReady", Boolean.toString(this.particleTranslucencyProofReady));
        fields.put(normalizedPrefix + ".temporalHistoryReady", Boolean.toString(this.temporalHistoryReady));
        fields.put(normalizedPrefix + ".realShaderDenoiseOutput", Boolean.toString(this.realShaderDenoiseOutput));
        fields.put(normalizedPrefix + ".cpuDenoiseFallback", Boolean.toString(this.cpuDenoiseFallback));
        fields.put(normalizedPrefix + ".rejectedEvidenceClean", Boolean.toString(this.rejectedEvidenceClean));
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
}
