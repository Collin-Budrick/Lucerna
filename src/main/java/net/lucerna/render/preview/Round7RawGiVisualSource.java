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
                "Round 7 RAW_GI visual mode can draw the native diffuse-GI RGBA8 payload as a raw source view"
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
                + ",ready=" + this.sourceReady
                + ",reason=\"" + this.reason + "\"";
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
