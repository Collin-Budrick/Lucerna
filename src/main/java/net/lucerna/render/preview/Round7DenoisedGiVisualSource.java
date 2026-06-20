package net.lucerna.render.preview;

import net.lucerna.nativebridge.DenoisedDiffuseGiCpuOutputPayload;

public record Round7DenoisedGiVisualSource(
        String modeKey,
        String sourceLabel,
        String evidenceLabel,
        String shaderLabel,
        boolean sourceReady,
        String reason
) {
    private static final String MODE_KEY = "ROUND7_DENOISED_GI";
    private static final String SOURCE_LABEL = "Round 7 DENOISED_GI visual mode using CPU denoised diffuse-GI RGBA8 payload";
    private static final String EVIDENCE_LABEL = "round7.denoisedGi.cpuDenoisedDiffuseGiPayload";
    private static final String SHADER_LABEL = "lucerna:core/round7_denoised_gi_visual";

    public Round7DenoisedGiVisualSource {
        modeKey = normalize(modeKey, MODE_KEY);
        sourceLabel = normalize(sourceLabel, SOURCE_LABEL);
        evidenceLabel = normalize(evidenceLabel, EVIDENCE_LABEL);
        shaderLabel = normalize(shaderLabel, SHADER_LABEL);
        if (reason == null || reason.isBlank()) {
            reason = sourceReady
                    ? "Round 7 DENOISED_GI source is ready for visual draw"
                    : "Round 7 DENOISED_GI source is not ready for visual draw";
        } else {
            reason = reason.trim();
        }
    }

    public static Round7DenoisedGiVisualSource from(DenoisedDiffuseGiCpuOutputPayload payload) {
        if (payload == null) {
            return unavailable("Round 7 DENOISED_GI visual mode is unavailable because CPU denoised GI payload is missing");
        }
        if (!payload.readyForPreviewDraw()) {
            return unavailable("Round 7 DENOISED_GI visual mode is unavailable because CPU denoised GI payload is not displayable: "
                    + payload.debugSummary());
        }
        return new Round7DenoisedGiVisualSource(
                MODE_KEY,
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                SHADER_LABEL,
                true,
                "Round 7 DENOISED_GI visual mode can draw the CPU denoised diffuse-GI RGBA8 payload; "
                        + payload.readinessBoundarySummary()
        );
    }

    public static Round7DenoisedGiVisualSource unavailable(String reason) {
        return new Round7DenoisedGiVisualSource(
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
                + ",sourceAuthenticity=\"" + this.sourceAuthenticityBoundary() + "\""
                + ",projectionBoundary=\"" + this.surfaceProjectionBoundary() + "\""
                + ",qualityBoundary=\"" + this.qualityBoundary() + "\""
                + ",temporalReadiness=\"" + this.temporalReadinessBoundary() + "\""
                + ",focusedRegionProof=\"" + this.focusedRegionProofExpectation() + "\""
                + ",ready=" + this.sourceReady
                + ",reason=\"" + this.reason + "\"";
    }

    public String denoisedPayloadEvidence() {
        return this.evidenceLabel;
    }

    public String sourceIdentity() {
        return "cpu-denoised-diffuse-gi-rgba8/denoised-gi";
    }

    public String sourceClassLabel() {
        return "cpu-denoised-gi/readback-output;not-raw-gi;real-shader-denoise=false";
    }

    public String qualityBoundary() {
        return "CPU denoised RGBA8 source readiness is not real shader denoise quality unless realDenoiseShaderOutput=true and controller raw-vs-denoised proof improves";
    }

    public String sourceAuthenticityBoundary() {
        return "denoised GI source must be CPU denoised diffuse-GI RGBA8 output from the denoise stage; reject raw metadata, proof markers, focus-window-only sources, and direct-light substitution";
    }

    public String surfaceProjectionBoundary() {
        return "current public shader path is scene-shaped full-target projection from denoised payload cues; real geometry/material-aware shader denoise and physically correct GI projection remain pending";
    }

    public String temporalReadinessBoundary() {
        return this.sourceReady
                ? "CPU denoised source is drawable, but temporal stability still requires controller stable/moved screenshot sequence proof and history accept/reject telemetry"
                : "temporal proof should not pass because denoised source is not drawable";
    }

    public String focusedRegionProofExpectation() {
        return this.sourceReady
                ? "source-ready only; controller still needs focused-surface screenshot delta, anti-rectangular-washout proof, and raw-vs-denoised quality comparison"
                : "not ready; visual proof should not pass";
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
