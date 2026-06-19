package net.lucerna.render.mixin;

public record PublicMojangPreviewPassSubmissionResult(
        boolean attempted,
        boolean submitted,
        boolean drawCallsIssued,
        boolean javaOpaqueRenderObjectsPresent,
        TargetStatus targetStatus,
        String reason
) {
    public PublicMojangPreviewPassSubmissionResult {
        if (targetStatus == null) {
            targetStatus = TargetStatus.UNKNOWN;
        }
        if (reason == null || reason.isBlank()) {
            reason = submitted
                    ? "public Mojang preview pass submission recorded"
                    : "public Mojang preview pass submission was not submitted";
        } else {
            reason = reason.trim();
        }
    }

    public static PublicMojangPreviewPassSubmissionResult notAttempted(String reason) {
        return new PublicMojangPreviewPassSubmissionResult(
                false,
                false,
                false,
                false,
                TargetStatus.NOT_REQUESTED,
                reason
        );
    }

    public static PublicMojangPreviewPassSubmissionResult notSubmitted(
            boolean attempted,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return new PublicMojangPreviewPassSubmissionResult(
                attempted,
                false,
                false,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                reason
        );
    }

    public static PublicMojangPreviewPassSubmissionResult submitted(
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return new PublicMojangPreviewPassSubmissionResult(
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
