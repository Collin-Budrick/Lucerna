package net.lucerna.render.preview;

import java.util.Locale;

public record PublicMojangFinalCompositeSubmissionResult(
        boolean attempted,
        boolean submitted,
        boolean drawCallsIssued,
        boolean javaOpaqueRenderObjectsPresent,
        TargetStatus targetStatus,
        String reason
) {
    public PublicMojangFinalCompositeSubmissionResult {
        if (targetStatus == null) {
            targetStatus = TargetStatus.UNKNOWN;
        }
        if (reason == null || reason.isBlank()) {
            reason = submitted
                    ? "public Mojang final composite submission recorded"
                    : "public Mojang final composite submission was not submitted";
        } else {
            reason = reason.trim();
        }
    }

    public static PublicMojangFinalCompositeSubmissionResult notAttempted(String reason) {
        return new PublicMojangFinalCompositeSubmissionResult(
                false,
                false,
                false,
                false,
                TargetStatus.NOT_REQUESTED,
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult notSubmitted(
            boolean attempted,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return new PublicMojangFinalCompositeSubmissionResult(
                attempted,
                false,
                false,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitted(
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return new PublicMojangFinalCompositeSubmissionResult(
                true,
                true,
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                reason
        );
    }

    public boolean skipped() {
        return this.attempted && !this.submitted;
    }

    public String submissionStateLabel() {
        if (this.submitted && this.drawCallsIssued) {
            return "submitted-with-draw";
        }
        if (this.submitted) {
            return "submitted-without-draw";
        }
        if (this.skipped()) {
            return "skipped-after-attempt";
        }
        return "not-attempted";
    }

    public String submittedSourceIdentity() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        if (!this.submitted) {
            return "not-submitted";
        }
        boolean directLight = normalizedReason.contains("native direct-light emissive source is blended")
                || normalizedReason.contains("native direct-light surface-source");
        boolean denoisedGi = normalizedReason.contains("denoised diffuse-gi")
                || normalizedReason.contains("cpu-denoised-diffuse-gi-rgba8")
                || normalizedReason.contains("shader-denoised-diffuse-gi-rgba8");
        boolean cpuDenoiseExplicitlyTrue = normalizedReason.contains("cpudenoisedsourceready=true")
                || normalizedReason.contains("cpu_denoised_source_ready=true")
                || normalizedReason.contains("cpudenoisedgi=enabled-ready")
                || normalizedReason.contains("cpudenoisedgi=ready")
                || normalizedReason.contains("cpudenoisedreadback=ready")
                || normalizedReason.contains("sourcekind=cpu-denoised-readback")
                || normalizedReason.contains("sourcekind=cpu_denoised_readback");
        boolean shaderDenoiseExplicitlyFalse = normalizedReason.contains("realdenoiseshaderoutput=false")
                || normalizedReason.contains("real_denoise_shader_output=false")
                || normalizedReason.contains("shaderdenoiseoutput=false")
                || normalizedReason.contains("gpu_denoise_output=false")
                || normalizedReason.contains("shaderdenoisedgi=pending-realdenoiseshaderoutput")
                || normalizedReason.contains("shaderdenoisedgi=enabled-missing")
                || normalizedReason.contains("shadergenerateddenoisedgi=missing")
                || normalizedReason.contains("sourcekind=cpu-denoised-readback")
                || normalizedReason.contains("sourcekind=cpu_denoised_readback");
        boolean shaderDenoiseExplicitlyTrue = normalizedReason.contains("realdenoiseshaderoutput=true")
                || normalizedReason.contains("real_denoise_shader_output=true")
                || normalizedReason.contains("shaderdenoiseoutput=true")
                || normalizedReason.contains("gpu_denoise_output=true")
                || normalizedReason.contains("realshaderdenoiseoutputready=ready")
                || normalizedReason.contains("realshaderdenoiseoutputready=true")
                || normalizedReason.contains("shaderdenoisedgi=enabled-ready")
                || normalizedReason.contains("shaderdenoisedgi=ready")
                || normalizedReason.contains("shadergenerateddenoisedgi=ready")
                || normalizedReason.contains("sourcekind=shader-generated-denoised-gi")
                || normalizedReason.contains("sourcekind=shader_generated_denoised_gi");
        boolean shaderDenoisedGi = !shaderDenoiseExplicitlyFalse
                && (shaderDenoiseExplicitlyTrue
                || normalizedReason.contains("shader-denoised-diffuse-gi-rgba8"));
        boolean cpuDenoisedGi = !shaderDenoisedGi
                && (cpuDenoiseExplicitlyTrue
                || normalizedReason.contains("cpu-denoised-diffuse-gi-rgba8")
                || denoisedGi);
        boolean rawGi = normalizedReason.contains("raw native diffuse-gi source is blended")
                || normalizedReason.contains("round 7 raw_gi native diffuse-gi source additive draw issued")
                || normalizedReason.contains("rawdrawrepeats=1");
        StringBuilder identity = new StringBuilder();
        appendIdentity(identity, directLight, "native-direct-light-rgba8");
        appendIdentity(identity, rawGi, "native-diffuse-gi-rgba8");
        appendIdentity(identity, cpuDenoisedGi, "cpu-denoised-diffuse-gi-rgba8");
        appendIdentity(identity, shaderDenoisedGi, "shader-denoised-diffuse-gi-rgba8");
        if (identity.length() > 0) {
            return identity.toString();
        }
        return "unknown-submitted-source";
    }

    public boolean submittedFocusWindowOnly() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("focuswindowonly=true")
                || normalizedReason.contains("focus_window_only=true")
                || normalizedReason.contains("focus_window_source=true")
                || normalizedReason.contains("focus_only=true"));
    }

    public boolean submittedMetadataOnlyPreview() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (this.targetStatus == TargetStatus.METADATA_ONLY
                || normalizedReason.contains("metadataonly=true")
                || normalizedReason.contains("metadata_only=true")
                || normalizedReason.contains("metadata_preview=true")
                || normalizedReason.contains("metadata_source=true"));
    }

    public boolean submittedProofMarkerSource() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("proofmarker=true")
                || normalizedReason.contains("proof_marker=true")
                || normalizedReason.contains("proof_source=true")
                || normalizedReason.contains("proof_only=true"));
    }

    public boolean submittedTemporaryDirectLightSubstitution() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("temporarydirectlightsubstitution=true")
                || normalizedReason.contains("temporary_direct_light_substitution=true")
                || normalizedReason.contains("temporary_direct_light_source=true")
                || normalizedReason.contains("temporary_direct_source=true")
                || normalizedReason.contains("uses_direct_light_payload=true")
                || normalizedReason.contains("using_direct_light_payload=true")
                || normalizedReason.contains("direct_light_payload_substitute=true"));
    }

    public boolean submittedRectangularWashoutRisk() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("rectangularwashout=true")
                || normalizedReason.contains("rectangular_washout=true")
                || normalizedReason.contains("hard_rectangle=true")
                || normalizedReason.contains("full_screen_washout=true")
                || normalizedReason.contains("full_target_washout=true"));
    }

    public boolean submittedPreviewOnlyEvidence() {
        return this.submittedFocusWindowOnly()
                || this.submittedMetadataOnlyPreview()
                || this.submittedProofMarkerSource()
                || this.submittedTemporaryDirectLightSubstitution()
                || this.submittedRectangularWashoutRisk();
    }

    public boolean submittedSourceGatedSurfaceProjection() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("source-gated")
                || normalizedReason.contains("source_gated")
                || normalizedReason.contains("sourceboundary=full-target-source-gated")
                || normalizedReason.contains("surfaceprojection=source-gated"));
    }

    public boolean submittedDirectLightSource() {
        return this.submittedSourceIdentity().contains("native-direct-light-rgba8");
    }

    public boolean submittedRawGiSource() {
        return this.submittedSourceIdentity().contains("native-diffuse-gi-rgba8");
    }

    public boolean submittedDenoisedGiSource() {
        return this.submittedDenoisedGiSourceIdentity() != DenoisedGiSourceIdentity.NONE;
    }

    public boolean submittedCpuDenoisedGiSource() {
        DenoisedGiSourceIdentity identity = this.submittedDenoisedGiSourceIdentity();
        return identity == DenoisedGiSourceIdentity.CPU_DENOISED_READBACK
                || identity == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
    }

    public boolean submittedShaderDenoisedGiSource() {
        DenoisedGiSourceIdentity identity = this.submittedDenoisedGiSourceIdentity();
        return identity == DenoisedGiSourceIdentity.SHADER_GENERATED_DENOISED_GI
                || identity == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
    }

    public DenoisedGiSourceIdentity submittedDenoisedGiSourceIdentity() {
        String identity = this.submittedSourceIdentity();
        boolean cpu = identity.contains("cpu-denoised-diffuse-gi-rgba8");
        boolean shader = identity.contains("shader-denoised-diffuse-gi-rgba8");
        if (cpu && shader) {
            return DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
        }
        if (shader) {
            return DenoisedGiSourceIdentity.SHADER_GENERATED_DENOISED_GI;
        }
        if (cpu) {
            return DenoisedGiSourceIdentity.CPU_DENOISED_READBACK;
        }
        return DenoisedGiSourceIdentity.NONE;
    }

    public boolean submittedShaderDenoiseOverclaim() {
        return this.submittedShaderDenoiseVisualShaderIntent()
                && !this.submittedRealShaderDenoiseOutputReady();
    }

    public boolean submittedShaderDenoiseVisualShaderIntent() {
        String normalizedReason = this.reason.toLowerCase(Locale.ROOT);
        return this.submitted
                && (normalizedReason.contains("round 7 denoised_gi visual")
                || normalizedReason.contains("round 7 final_composite visual")
                || normalizedReason.contains("round7_denoised_gi_visual")
                || normalizedReason.contains("denoised-source additive draw")
                || normalizedReason.contains("source-gated-denoised-gi-visual")
                || normalizedReason.contains("shader=lucerna:core/round7_denoised_gi_visual"));
    }

    public boolean submittedRealShaderDenoiseDispatchReady() {
        return this.submittedShaderDenoisedGiSource();
    }

    public boolean submittedRealShaderDenoiseOutputReady() {
        return this.submittedShaderDenoisedGiSource();
    }

    public String denoiseEvidenceBoundarySummary() {
        return "cpuDenoisedOutput=" + readyState(this.submittedCpuDenoisedGiSource())
                + ",denoisedSourceIdentity=" + this.submittedDenoisedGiSourceIdentity().stableLabel()
                + ",shaderDenoiseVisualShaderIntent=" + readyState(this.submittedShaderDenoiseVisualShaderIntent())
                + ",realShaderDenoiseDispatchReady=" + readyState(this.submittedRealShaderDenoiseDispatchReady())
                + ",realShaderDenoiseOutputReady=" + readyState(this.submittedRealShaderDenoiseOutputReady())
                + ",shaderDenoiseOverclaimPresent=" + this.submittedShaderDenoiseOverclaim()
                + ",finalDenoiseSourceIdentity=" + this.denoiseSourceClassLabel()
                + ",boundary=\""
                + (this.submittedRealShaderDenoiseOutputReady()
                ? "submitted source reports shader-denoised GI output; controller still needs quality/stability proof"
                : "current submitted denoise evidence is CPU/readback or absent; visual shader intent is not real shader-side denoise output")
                + "\"";
    }

    public String authenticityGuardsSummary() {
        return "metadataOnlyPreview=" + this.submittedMetadataOnlyPreview()
                + ",focusWindowOnly=" + this.submittedFocusWindowOnly()
                + ",proofMarkerSource=" + this.submittedProofMarkerSource()
                + ",temporaryDirectLightSubstitution=" + this.submittedTemporaryDirectLightSubstitution()
                + ",rectangularWashoutRisk=" + this.submittedRectangularWashoutRisk()
                + ",previewEvidenceClean=" + !this.submittedPreviewOnlyEvidence();
    }

    public boolean submittedRound7GiSource() {
        return this.submittedRawGiSource() || this.submittedDenoisedGiSource();
    }

    public String sourceAuthenticityLabel() {
        if (!this.submitted) {
            return "no-submitted-source";
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "rejected:metadata-only-preview";
        }
        if (this.submittedProofMarkerSource()) {
            return "rejected:proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "rejected:focus-window-only";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "rejected:temporary-direct-light-substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "rejected:rectangular-washout-risk";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "accepted:final-composite-direct-plus-raw-gi-plus-"
                    + this.denoiseSourceClassLabel()
                    + "/source-separated/"
                    + this.surfaceProjectionEvidenceLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRound7GiSource()) {
            return "accepted:partial-final-composite-direct-plus-gi";
        }
        if (this.submittedDirectLightSource()) {
            return "accepted:native-direct-light-surface-source";
        }
        if (this.submittedDenoisedGiSource()) {
            return "accepted:" + this.denoiseSourceClassLabel() + "-output";
        }
        if (this.submittedRawGiSource()) {
            return "accepted:raw-gi-output";
        }
        return "unknown-source";
    }

    public String focusedRegionReadiness() {
        if (!this.submitted) {
            return "not-ready:not-submitted";
        }
        if (!this.drawCallsIssued) {
            return "not-ready:no-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "not-ready:metadata-only-preview";
        }
        if (this.submittedProofMarkerSource()) {
            return "not-ready:proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "not-ready:focus-window-only";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "not-ready:temporary-direct-light-substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "not-ready:rectangular-washout-risk";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "ready-for-controller-final-direct-raw-denoised-"
                    + this.surfaceProjectionEvidenceLabel()
                    + "-surface-delta";
        }
        if (this.submittedDirectLightSource()) {
            return "ready-for-controller-direct-light-surface-delta";
        }
        if (!this.submittedRound7GiSource()) {
            return "not-ready:unknown-source";
        }
        return "ready-for-controller-focused-region-delta";
    }

    public String visualProofExpectation() {
        if (!this.submitted) {
            return "visual proof should not pass until a selected Round 7 source draw is submitted";
        }
        if (!this.drawCallsIssued) {
            return "visual proof should not pass because the submission reported no draw calls";
        }
        if (this.submittedFocusWindowOnly()) {
            return "visual proof should not pass because Round 7 rejects focus-window-only brightness";
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "visual proof should not pass because metadata-only preview is not a drawable lighting source";
        }
        if (this.submittedProofMarkerSource()) {
            return "visual proof should not pass because proof markers are not surface lighting";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "visual proof should not pass because GI/denoise paths reject temporary direct-light substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "visual proof should not pass because rectangular/full-screen washout is not geometry/material-aware surface projection";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "final visual proof can only pass if controller screenshots show stable source-separated direct plus raw GI plus denoised GI contribution projected as source-gated scene/surface lighting with HUD safety; focus windows, proof markers, metadata-only previews, and rectangular washout must fail";
        }
        if (this.submittedDirectLightSource()) {
            return "direct-light visual proof can only pass if controller screenshots show an emissive native direct-light surface delta";
        }
        if (this.submittedRound7GiSource()) {
            return "visual proof can only pass if controller screenshots show focused-surface delta for this submitted source";
        }
        return "visual proof should remain inconclusive because the submitted source identity is unknown";
    }

    public String visualProofMissingReason() {
        if (!this.attempted) {
            return "final-composite-not-attempted";
        }
        if (!this.submitted) {
            return "final-composite-skipped";
        }
        if (!this.drawCallsIssued) {
            return "submitted-without-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "target-not-ready:" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "metadata-only-preview-source";
        }
        if (this.submittedProofMarkerSource()) {
            return "proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "focus-window-only-source";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "temporary-direct-light-substitution-source";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "rectangular-washout-risk";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "awaiting-controller-final-direct-raw-denoised-geometry-material-aware-surface-delta-and-quality-proof";
        }
        if (this.submittedDirectLightSource()) {
            return "awaiting-controller-direct-light-before-after-surface-delta";
        }
        if (!this.submittedRound7GiSource()) {
            return "unknown-submitted-source";
        }
        return "awaiting-controller-before-after-focused-surface-delta";
    }

    public String finalSurfaceProjectionQualityGate() {
        if (!this.submitted) {
            return "not-ready:not-submitted";
        }
        if (!this.drawCallsIssued) {
            return "not-ready:no-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "not-ready:rejected-preview-evidence/" + this.sourceAuthenticityLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "ready-for-controller-proof:source-separated-direct-raw-gi-"
                    + this.denoiseSourceClassLabel()
                    + ";surfaceProjection="
                    + this.surfaceProjectionEvidenceLabel()
                    + ";geometry-material-aware-quality=pending;temporal-stability=pending";
        }
        if (this.submittedRound7GiSource()) {
            return "partial-ready:gi-source-present;missing-direct-or-denoised-final-stack";
        }
        if (this.submittedDirectLightSource()) {
            return "partial-ready:direct-source-present;missing-gi-denoise-final-stack";
        }
        return "not-ready:unknown-source";
    }

    public String summary() {
        return "state=" + this.submissionStateLabel()
                + ",attempted=" + this.attempted
                + ",submitted=" + this.submitted
                + ",drawCallsIssued=" + this.drawCallsIssued
                + ",javaOpaqueRenderObjectsPresent=" + this.javaOpaqueRenderObjectsPresent
                + ",targetStatus=" + this.targetStatus
                + ",sourceIdentity=" + this.submittedSourceIdentity()
                + ",sourceReadinessMatrix=" + this.sourceReadinessMatrix()
                + ",denoiseEvidenceBoundary=" + this.denoiseEvidenceBoundarySummary()
                + ",sourceAuthenticity=" + this.sourceAuthenticityLabel()
                + ",authenticityGuards=" + this.authenticityGuardsSummary()
                + ",surfaceProjectionEvidence=" + this.surfaceProjectionEvidenceLabel()
                + ",geometryMaterialProjectionReadiness=" + this.geometryMaterialProjectionReadiness()
                + ",temporalStabilityReadiness=" + this.temporalStabilityReadiness()
                + ",focusWindowOnly=" + this.submittedFocusWindowOnly()
                + ",metadataOnlyPreview=" + this.submittedMetadataOnlyPreview()
                + ",proofMarkerSource=" + this.submittedProofMarkerSource()
                + ",temporaryDirectLightSubstitution=" + this.submittedTemporaryDirectLightSubstitution()
                + ",rectangularWashoutRisk=" + this.submittedRectangularWashoutRisk()
                + ",sourceGatedSurfaceProjection=" + this.submittedSourceGatedSurfaceProjection()
                + ",round7GiSource=" + this.submittedRound7GiSource()
                + ",focusedRegionReadiness=" + this.focusedRegionReadiness()
                + ",finalSurfaceProjectionQualityGate=" + this.finalSurfaceProjectionQualityGate()
                + ",visualProofMissingReason=" + this.visualProofMissingReason()
                + ",visualProofExpectation=\"" + this.visualProofExpectation() + "\""
                + ",reason=" + this.reason;
    }

    public String sourceReadinessMatrix() {
        return "direct=" + readyState(this.submittedDirectLightSource())
                + ",rawGI=" + readyState(this.submittedRawGiSource())
                + ",cpuDenoisedGI=" + readyState(this.submittedCpuDenoisedGiSource())
                + ",shaderDenoisedGI=" + readyState(this.submittedShaderDenoisedGiSource())
                + ",denoisedSourceIdentity=" + this.submittedDenoisedGiSourceIdentity().stableLabel()
                + ",shaderDenoiseVisualShaderIntent=" + readyState(this.submittedShaderDenoiseVisualShaderIntent())
                + ",realShaderDenoiseDispatchReady=" + readyState(this.submittedRealShaderDenoiseDispatchReady())
                + ",realShaderDenoiseOutputReady=" + readyState(this.submittedRealShaderDenoiseOutputReady())
                + ",shaderDenoiseOverclaimPresent=" + this.submittedShaderDenoiseOverclaim()
                + ",previewEvidenceClean=" + readyState(!this.submittedPreviewOnlyEvidence())
                + ",sourceGatedSurfaceProjection=" + readyState(this.submittedSourceGatedSurfaceProjection());
    }

    public String geometryMaterialProjectionReadiness() {
        if (!this.submitted || !this.drawCallsIssued) {
            return "not-ready:submission-missing";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "not-ready:" + this.sourceAuthenticityLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "candidate:source-separated-full-target;"
                    + this.surfaceProjectionEvidenceLabel()
                    + ";physical-geometry-material-quality=pending-controller-proof";
        }
        if (this.submittedRound7GiSource()) {
            return "partial:gi-source-submitted;final-direct-plus-denoise-stack-incomplete";
        }
        if (this.submittedDirectLightSource()) {
            return "partial:direct-source-submitted;gi-denoise-stack-incomplete";
        }
        return "not-ready:unknown-source";
    }

    public String temporalStabilityReadiness() {
        if (!this.submittedDenoisedGiSource()) {
            return "not-ready:no-denoised-source";
        }
        if (this.submittedShaderDenoisedGiSource()) {
            return "candidate:shader-denoised-source-present;requires stable/moved screenshot sequence proof";
        }
        return "partial:cpu-denoised-source-present;requires stable/moved screenshot sequence proof and must not claim shader denoise";
    }

    public String denoiseSourceClassLabel() {
        if (this.submittedDenoisedGiSourceIdentity() == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER) {
            return "mixed-cpu-and-shader-denoised-gi";
        }
        if (this.submittedShaderDenoisedGiSource()) {
            return "shader-denoised-gi";
        }
        if (this.submittedCpuDenoisedGiSource()) {
            return "cpu-denoised-gi";
        }
        return "denoised-gi-missing";
    }

    public String surfaceProjectionEvidenceLabel() {
        if (!this.submitted) {
            return "not-submitted";
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "rejected-preview-shortcut";
        }
        if (this.submittedSourceGatedSurfaceProjection()) {
            return "source-gated-scene-shaped-preview";
        }
        return "unlabeled-requires-controller-proof";
    }

    private static void appendIdentity(StringBuilder identity, boolean present, String label) {
        if (!present) {
            return;
        }
        if (identity.length() > 0) {
            identity.append("+");
        }
        identity.append(label);
    }

    private static String readyState(boolean ready) {
        return ready ? "ready" : "missing";
    }

    public enum TargetStatus {
        NOT_REQUESTED,
        TARGET_MISSING,
        METADATA_ONLY,
        JAVA_OPAQUE_OBJECTS_PRESENT,
        NATIVE_WRITABLE_UNAVAILABLE,
        READY,
        UNKNOWN
    }

    public enum DenoisedGiSourceIdentity {
        NONE("none"),
        CPU_DENOISED_READBACK("cpu-denoised-readback"),
        SHADER_GENERATED_DENOISED_GI("shader-generated-denoised-gi"),
        MIXED_CPU_AND_SHADER("mixed-cpu-and-shader-denoised-gi");

        private final String stableLabel;

        DenoisedGiSourceIdentity(String stableLabel) {
            this.stableLabel = stableLabel;
        }

        public String stableLabel() {
            return this.stableLabel;
        }
    }
}
