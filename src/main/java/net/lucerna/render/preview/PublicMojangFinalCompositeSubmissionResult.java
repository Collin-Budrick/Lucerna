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
        boolean directLight = normalizedReason.contains("candidateevidence=true")
                || normalizedReason.contains("native direct-light emissive source is blended")
                || normalizedReason.contains("native direct-light surface-source");
        boolean denoisedGi = normalizedReason.contains("denoised diffuse-gi")
                || normalizedReason.contains("cpu-denoised-diffuse-gi-rgba8");
        boolean rawGi = normalizedReason.contains("raw_gi")
                || normalizedReason.contains("native diffuse-gi")
                || normalizedReason.contains("native-diffuse-gi-rgba8/raw-gi");
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
        return this.submitted && this.reason.toLowerCase(Locale.ROOT).contains("focus-window");
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
        if (this.submittedFocusWindowOnly()) {
            return "rejected:focus-window-only";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "accepted:final-composite-direct-plus-raw-gi-plus-denoised-gi";
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
        if (this.submittedFocusWindowOnly()) {
            return "not-ready:focus-window-only";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "ready-for-controller-final-direct-raw-denoised-focused-region-delta";
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
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "final visual proof can only pass if controller screenshots show stable direct plus raw GI plus denoised GI surface contribution and HUD safety";
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
        if (this.submittedFocusWindowOnly()) {
            return "focus-window-only-source";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "awaiting-controller-final-direct-raw-denoised-focused-surface-delta-and-quality-proof";
        }
        if (this.submittedDirectLightSource()) {
            return "awaiting-controller-direct-light-before-after-surface-delta";
        }
        if (!this.submittedRound7GiSource()) {
            return "unknown-submitted-source";
        }
        return "awaiting-controller-before-after-focused-surface-delta";
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
                + ",round7GiSource=" + this.submittedRound7GiSource()
                + ",focusedRegionReadiness=" + this.focusedRegionReadiness()
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
