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
                || normalizedReason.contains("cpu-denoised-diffuse-gi-rgba8");
        boolean rawGi = normalizedReason.contains("raw native diffuse-gi source is blended")
                || normalizedReason.contains("round 7 raw_gi native diffuse-gi source additive draw issued")
                || normalizedReason.contains("rawdrawrepeats=1");
        StringBuilder identity = new StringBuilder();
        appendIdentity(identity, directLight, "native-direct-light-rgba8");
        appendIdentity(identity, rawGi, "native-diffuse-gi-rgba8");
        appendIdentity(identity, denoisedGi, "cpu-denoised-diffuse-gi-rgba8");
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

    public boolean submittedDirectLightSource() {
        return this.submittedSourceIdentity().contains("native-direct-light-rgba8");
    }

    public boolean submittedRawGiSource() {
        return this.submittedSourceIdentity().contains("native-diffuse-gi-rgba8");
    }

    public boolean submittedDenoisedGiSource() {
        return this.submittedSourceIdentity().contains("denoised-diffuse-gi-rgba8");
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
            return "accepted:final-composite-direct-plus-raw-gi-plus-denoised-gi/source-separated";
        }
        if (this.submittedDirectLightSource() && this.submittedRound7GiSource()) {
            return "accepted:partial-final-composite-direct-plus-gi";
        }
        if (this.submittedDirectLightSource()) {
            return "accepted:native-direct-light-surface-source";
        }
        if (this.submittedDenoisedGiSource()) {
            return "accepted:denoised-gi-output";
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
            return "ready-for-controller-final-direct-raw-denoised-geometry-material-aware-surface-delta";
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
            return "final visual proof can only pass if controller screenshots show stable source-separated direct plus raw GI plus denoised GI contribution projected onto scene geometry/material surfaces with HUD safety";
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
            return "ready-for-controller-proof:source-separated-direct-raw-gi-denoised-gi;geometry-material-aware-quality=pending";
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
                + ",sourceAuthenticity=" + this.sourceAuthenticityLabel()
                + ",focusWindowOnly=" + this.submittedFocusWindowOnly()
                + ",metadataOnlyPreview=" + this.submittedMetadataOnlyPreview()
                + ",proofMarkerSource=" + this.submittedProofMarkerSource()
                + ",temporaryDirectLightSubstitution=" + this.submittedTemporaryDirectLightSubstitution()
                + ",rectangularWashoutRisk=" + this.submittedRectangularWashoutRisk()
                + ",round7GiSource=" + this.submittedRound7GiSource()
                + ",focusedRegionReadiness=" + this.focusedRegionReadiness()
                + ",finalSurfaceProjectionQualityGate=" + this.finalSurfaceProjectionQualityGate()
                + ",visualProofMissingReason=" + this.visualProofMissingReason()
                + ",visualProofExpectation=\"" + this.visualProofExpectation() + "\""
                + ",reason=" + this.reason;
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

    public enum TargetStatus {
        NOT_REQUESTED,
        TARGET_MISSING,
        METADATA_ONLY,
        JAVA_OPAQUE_OBJECTS_PRESENT,
        NATIVE_WRITABLE_UNAVAILABLE,
        READY,
        UNKNOWN
    }
}
