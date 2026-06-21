package net.lucerna.render.preview;

import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;

public record Round7RawGiVisualSource(
        String modeKey,
        String sourceLabel,
        String evidenceLabel,
        String shaderLabel,
        boolean sourceReady,
        String reason
) {
    private static final String MODE_KEY = "ROUND7_RAW_GI";
    private static final String SOURCE_LABEL = "Round 7 RAW_GI visual mode using native diffuse-GI RGBA8 payload";
    private static final String EVIDENCE_LABEL = "round7.rawGi.nativeDiffuseGiPayload";
    private static final String SHADER_LABEL = "lucerna:core/round6_native_diffuse_gi_surface";

    public Round7RawGiVisualSource {
        modeKey = normalize(modeKey, MODE_KEY);
        sourceLabel = normalize(sourceLabel, SOURCE_LABEL);
        evidenceLabel = normalize(evidenceLabel, EVIDENCE_LABEL);
        shaderLabel = normalize(shaderLabel, SHADER_LABEL);
        if (reason == null || reason.isBlank()) {
            reason = sourceReady
                    ? "Round 7 RAW_GI source is ready for visual draw"
                    : "Round 7 RAW_GI source is not ready for visual draw";
        } else {
            reason = reason.trim();
        }
    }

    public static Round7RawGiVisualSource from(
            Round6DiffuseGiPreviewCompositeState previewState,
            Round6DiffuseGiCpuOutputPayload payload
    ) {
        if (previewState == null) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable because diffuse-GI readiness state is missing");
        }
        if (!previewState.readyForRound7RawGiSource()) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable because diffuse-GI source readiness is incomplete: "
                    + previewState.summary());
        }
        if (payload == null) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable because native diffuse-GI RGBA8 payload is missing");
        }
        if (!payload.readyForPreviewDraw()) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable because native diffuse-GI RGBA8 payload is not displayable: "
                    + payload.debugSummary());
        }
        return new Round7RawGiVisualSource(
                MODE_KEY,
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                SHADER_LABEL,
                true,
                "Round 7 RAW_GI visual mode can draw the native diffuse-GI RGBA8 payload as a raw source view; "
                        + payload.spatialGiPayloadSummary()
        );
    }

    public static Round7RawGiVisualSource unavailable(String reason) {
        return new Round7RawGiVisualSource(
                MODE_KEY,
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                SHADER_LABEL,
                false,
                reason
        );
    }

    public String summary() {
        return "mode=" + this.modeKey
                + ",source=\"" + this.sourceLabel + "\""
                + ",evidence=" + this.evidenceLabel
                + ",shader=" + this.shaderLabel
                + ",sourceIdentity=" + this.sourceIdentity()
                + ",sourceClass=" + this.sourceClassLabel()
                + ",spatialPayloadIdentity=" + this.spatialPayloadIdentitySummary()
                + ",sourceAuthenticity=\"" + this.sourceAuthenticityBoundary() + "\""
                + ",projectionBoundary=\"" + this.surfaceProjectionBoundary() + "\""
                + ",qualityReadiness=\"" + this.qualityReadinessBoundary() + "\""
                + ",focusedRegionProof=\"" + this.focusedRegionProofExpectation() + "\""
                + ",ready=" + this.sourceReady
                + ",reason=\"" + this.reason + "\"";
    }

    public String sourceIdentity() {
        return "native-diffuse-gi-rgba8/raw-gi;physical-scene-linked-cpu-metrics-only";
    }

    public String sourceClassLabel() {
        return "raw-gi-cpu/readback-native-output;not-denoised;not-real-shader-denoise";
    }

    public String spatialPayloadIdentitySummary() {
        return "sourceKind=raw-diffuse-gi-rgba8-cpu-readback"
                + ",rawGiSourceReady=" + this.sourceReady
                + ",spatiallyVaryingGiPayloadReady=defer-to-payload-summary"
                + ",physicalSceneTiedGiEvidenceReady=defer-to-payload-summary"
                + ",gpuTraversalExecuted=false"
                + ",nativeComputeGiExecuted=false"
                + ",nativeComputeDenoiseExecuted=false";
    }

    public String sourceAuthenticityBoundary() {
        return "raw GI source must be native diffuse-GI RGBA8 CPU/readback output; reject metadata-only, proof-marker, focus-window-only, and temporary direct-light substitution";
    }

    public String surfaceProjectionBoundary() {
        return "current public shader path is scene-shaped full-target projection from GI payload cues; scene-linked physical GI metrics may be reported separately, but geometry/material-aware physical GI projection remains pending controller/native-shader work";
    }

    public String qualityReadinessBoundary() {
        return this.sourceReady
                ? "raw GI source identity is ready; physical GI tracing quality, temporal stability, denoise quality, and shader output remain pending"
                : "raw GI source identity is not ready";
    }

    public String focusedRegionProofExpectation() {
        return this.sourceReady
                ? "source-ready only; controller still needs focused-surface screenshot delta and anti-rectangular-washout proof"
                : "not ready; visual proof should not pass";
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
