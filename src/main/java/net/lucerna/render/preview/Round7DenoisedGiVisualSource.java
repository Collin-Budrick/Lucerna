package net.lucerna.render.preview;

import net.lucerna.nativebridge.DenoisedDiffuseGiCpuOutputPayload;

public record Round7DenoisedGiVisualSource(
        String modeKey,
        String sourceLabel,
        String evidenceLabel,
        String shaderLabel,
        DenoisedSourceKind sourceKind,
        boolean sourceReady,
        String reason
) {
    private static final String MODE_KEY = "ROUND7_DENOISED_GI";
    private static final String SOURCE_LABEL = "Round 7 DENOISED_GI visual mode using CPU denoised diffuse-GI RGBA8 payload";
    private static final String EVIDENCE_LABEL = "round7.denoisedGi.cpuDenoisedDiffuseGiPayload";
    private static final String SHADER_LABEL = "lucerna:core/round7_denoised_gi_visual";

    public Round7DenoisedGiVisualSource {
        if (sourceKind == null) {
            sourceKind = DenoisedSourceKind.CPU_DENOISED_READBACK;
        }
        modeKey = normalize(modeKey, MODE_KEY);
        sourceLabel = normalize(sourceLabel, sourceKind.defaultSourceLabel());
        evidenceLabel = normalize(evidenceLabel, sourceKind.defaultEvidenceLabel());
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
                DenoisedSourceKind.CPU_DENOISED_READBACK,
                true,
                "Round 7 DENOISED_GI visual mode can draw the CPU denoised diffuse-GI RGBA8 payload; "
                        + payload.readinessBoundarySummary()
        );
    }

    public static Round7DenoisedGiVisualSource shaderGeneratedReady(String reason) {
        return new Round7DenoisedGiVisualSource(
                MODE_KEY,
                DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI.defaultSourceLabel(),
                DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI.defaultEvidenceLabel(),
                SHADER_LABEL,
                DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI,
                true,
                reason == null || reason.isBlank()
                        ? "Round 7 DENOISED_GI visual mode can draw a shader-generated denoised GI output"
                        : reason
        );
    }

    public static Round7DenoisedGiVisualSource unavailable(String reason) {
        return new Round7DenoisedGiVisualSource(
                MODE_KEY,
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                SHADER_LABEL,
                DenoisedSourceKind.UNAVAILABLE,
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
                + ",readinessIdentity=" + this.readinessIdentitySummary()
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
        return this.sourceKind.sourceIdentityLabel();
    }

    public String sourceClassLabel() {
        return this.sourceKind.sourceClassLabel();
    }

    public boolean cpuDenoisedReadbackSourceReady() {
        return this.sourceReady && this.sourceKind == DenoisedSourceKind.CPU_DENOISED_READBACK;
    }

    public boolean shaderGeneratedDenoisedSourceReady() {
        return this.sourceReady && this.sourceKind == DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI;
    }

    public boolean realShaderDenoiseOutputReady() {
        return this.shaderGeneratedDenoisedSourceReady();
    }

    public String readinessIdentitySummary() {
        return "cpuDenoisedReadback=" + readyState(this.cpuDenoisedReadbackSourceReady())
                + ",shaderGeneratedDenoisedGI=" + readyState(this.shaderGeneratedDenoisedSourceReady())
                + ",realShaderDenoiseOutputReady=" + readyState(this.realShaderDenoiseOutputReady())
                + ",overclaimPrevented=" + (this.sourceKind != DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI
                || this.realShaderDenoiseOutputReady());
    }

    public String qualityBoundary() {
        if (this.sourceKind == DenoisedSourceKind.SHADER_GENERATED_DENOISED_GI) {
            return "shader-generated denoised GI source readiness is only a candidate; controller raw-vs-denoised and temporal proof must still pass";
        }
        return "CPU denoised RGBA8 source readiness is not real shader denoise quality unless sourceKind=shader-generated-denoised-gi and controller raw-vs-denoised proof improves";
    }

    public String sourceAuthenticityBoundary() {
        return "denoised GI source must declare CPU-readback versus shader-generated identity explicitly; reject raw metadata, proof markers, focus-window-only sources, direct-light substitution, and shader-denoise claims without shader-generated output readiness";
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

    private static String readyState(boolean ready) {
        return ready ? "ready" : "missing";
    }

    public enum DenoisedSourceKind {
        CPU_DENOISED_READBACK(
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                "cpu-denoised-diffuse-gi-rgba8/denoised-gi",
                "cpu-denoised-gi/readback-output;not-raw-gi;real-shader-denoise=false"
        ),
        SHADER_GENERATED_DENOISED_GI(
                "Round 7 DENOISED_GI visual mode using shader-generated denoised diffuse-GI output",
                "round7.denoisedGi.shaderGeneratedDenoisedDiffuseGiOutput",
                "shader-denoised-diffuse-gi-rgba8/denoised-gi",
                "shader-denoised-gi/generated-output;not-cpu-readback;real-shader-denoise=true"
        ),
        UNAVAILABLE(
                SOURCE_LABEL,
                EVIDENCE_LABEL,
                "denoised-gi-unavailable",
                "denoised-gi/unavailable;real-shader-denoise=false"
        );

        private final String defaultSourceLabel;
        private final String defaultEvidenceLabel;
        private final String sourceIdentityLabel;
        private final String sourceClassLabel;

        DenoisedSourceKind(
                String defaultSourceLabel,
                String defaultEvidenceLabel,
                String sourceIdentityLabel,
                String sourceClassLabel
        ) {
            this.defaultSourceLabel = defaultSourceLabel;
            this.defaultEvidenceLabel = defaultEvidenceLabel;
            this.sourceIdentityLabel = sourceIdentityLabel;
            this.sourceClassLabel = sourceClassLabel;
        }

        public String defaultSourceLabel() {
            return this.defaultSourceLabel;
        }

        public String defaultEvidenceLabel() {
            return this.defaultEvidenceLabel;
        }

        public String sourceIdentityLabel() {
            return this.sourceIdentityLabel;
        }

        public String sourceClassLabel() {
            return this.sourceClassLabel;
        }
    }
}
