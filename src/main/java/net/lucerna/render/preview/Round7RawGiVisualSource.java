package net.lucerna.render.preview;

import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;

public record Round7RawGiVisualSource(
        String modeKey,
        String sourceLabel,
        String evidenceLabel,
        String shaderLabel,
        boolean sourceReady,
        boolean receiverLocalPayloadReady,
        boolean chromaRichPayloadCandidate,
        boolean smoothLobeFallbackRisk,
        String receiverPayloadIdentity,
        String visualPayloadClassLabel,
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
        receiverPayloadIdentity = normalize(receiverPayloadIdentity, "raw-gi-payload-unavailable");
        visualPayloadClassLabel = normalize(visualPayloadClassLabel, "raw-gi-visual-payload-unclassified");
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
                receiverLocalPayloadReady(payload),
                chromaRichPayloadCandidate(payload),
                smoothLobeFallbackRisk(payload),
                receiverPayloadIdentity(payload),
                visualPayloadClassLabel(payload),
                "Round 7 RAW_GI visual mode can draw the native diffuse-GI RGBA8 payload as a raw source view; "
                        + receiverPayloadStatusSummary(payload)
                        + "; "
                        + payload.spatialGiPayloadSummary()
        );
    }

    public static Round7RawGiVisualSource fromPayloadOnly(
            Round6DiffuseGiCpuOutputPayload payload,
            String selectionContext
    ) {
        String context = normalize(selectionContext, "final-composite source selection");
        if (payload == null) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable for " + context
                    + " because native diffuse-GI RGBA8 payload is missing");
        }
        if (!payload.rawGiInputReady()) {
            return unavailable("Round 7 RAW_GI visual mode is unavailable for " + context
                    + " because native diffuse-GI RGBA8 payload is not displayable: "
                    + payload.debugSummary());
        }
        return new Round7RawGiVisualSource(
                MODE_KEY,
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                SHADER_LABEL,
                true,
                receiverLocalPayloadReady(payload),
                chromaRichPayloadCandidate(payload),
                smoothLobeFallbackRisk(payload),
                receiverPayloadIdentity(payload),
                visualPayloadClassLabel(payload),
                "Round 7 RAW_GI visual mode selected a native scene-tied diffuse-GI RGBA8 payload for "
                        + context
                        + " without claiming preview-state or shader-denoise readiness; "
                        + receiverPayloadStatusSummary(payload)
                        + "; "
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
                false,
                false,
                false,
                "raw-gi-payload-unavailable",
                "raw-gi-visual-payload-unavailable",
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
                + ",receiverPayloadIdentity=" + this.receiverPayloadIdentity
                + ",visualPayloadClass=" + this.visualPayloadClassLabel
                + ",receiverLocalPayloadReady=" + this.receiverLocalPayloadReady
                + ",chromaRichPayloadCandidate=" + this.chromaRichPayloadCandidate
                + ",smoothLobeFallbackRisk=" + this.smoothLobeFallbackRisk
                + ",receiverPayloadStatus=\"" + this.receiverPayloadStatusBoundary() + "\""
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

    public String receiverPayloadStatusBoundary() {
        return this.visualPayloadClassLabel
                + ";receiver-local=" + this.receiverLocalPayloadReady
                + ";chroma-rich-candidate=" + this.chromaRichPayloadCandidate
                + ";smooth-lobe-fallback-risk=" + this.smoothLobeFallbackRisk
                + ";gpuTraversalExecuted=false"
                + ";nativeComputeGiExecuted=false"
                + ";physical-correctness=not-claimed";
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

    private static boolean receiverLocalPayloadReady(Round6DiffuseGiCpuOutputPayload payload) {
        return payload != null
                && payload.rawGiInputReady()
                && payload.spatiallyVaryingGiPayloadReady()
                && payload.physicalSceneTiedGiEvidenceReady();
    }

    private static boolean chromaRichPayloadCandidate(Round6DiffuseGiCpuOutputPayload payload) {
        return payload != null
                && payload.rawGiInputReady()
                && payload.spatiallyVaryingGiPayloadReady()
                && payload.peakChannel() > 0;
    }

    private static boolean smoothLobeFallbackRisk(Round6DiffuseGiCpuOutputPayload payload) {
        return payload == null
                || !payload.rawGiInputReady()
                || !payload.spatiallyVaryingGiPayloadReady()
                || !payload.physicalSceneTiedGiEvidenceReady();
    }

    private static String receiverPayloadIdentity(Round6DiffuseGiCpuOutputPayload payload) {
        if (payload == null || !payload.rawGiInputReady()) {
            return "raw-gi-payload-unavailable";
        }
        if (receiverLocalPayloadReady(payload) && chromaRichPayloadCandidate(payload)) {
            return "receiver-local/chroma-candidate/native-diffuse-gi-rgba8";
        }
        if (smoothLobeFallbackRisk(payload)) {
            return "smooth-lobe-fallback-risk/native-diffuse-gi-rgba8";
        }
        return "raw-gi-chroma-unproven/native-diffuse-gi-rgba8";
    }

    private static String visualPayloadClassLabel(Round6DiffuseGiCpuOutputPayload payload) {
        if (payload == null || !payload.rawGiInputReady()) {
            return "raw-gi-visual-payload-unavailable";
        }
        if (receiverLocalPayloadReady(payload) && chromaRichPayloadCandidate(payload)) {
            return "receiver-local-chroma-candidate-payload";
        }
        if (smoothLobeFallbackRisk(payload)) {
            return "smooth-lobe-fallback-risk-payload";
        }
        return "raw-gi-payload-chroma-unproven";
    }

    private static String receiverPayloadStatusSummary(Round6DiffuseGiCpuOutputPayload payload) {
        return "receiverPayloadIdentity=" + receiverPayloadIdentity(payload)
                + ",visualPayloadClass=" + visualPayloadClassLabel(payload)
                + ",receiverLocalPayloadReady=" + receiverLocalPayloadReady(payload)
                + ",chromaRichPayloadCandidate=" + chromaRichPayloadCandidate(payload)
                + ",smoothLobeFallbackRisk=" + smoothLobeFallbackRisk(payload)
                + ",gpuTraversalExecuted=false,nativeComputeGiExecuted=false,physicalCorrectnessClaimed=false";
    }
}
