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
                "Round 7 DENOISED_GI visual mode can draw the CPU denoised diffuse-GI RGBA8 payload"
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
                + ",ready=" + this.sourceReady
                + ",reason=\"" + this.reason + "\"";
    }

    public String denoisedPayloadEvidence() {
        return this.evidenceLabel;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
