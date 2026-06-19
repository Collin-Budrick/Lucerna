package net.lucerna.nativebridge;

public record DirectLightingPreviewCompositeSubmissionResult(
        boolean submitted,
        long frameIndex,
        boolean nativeOperational,
        boolean snapshotReady,
        boolean targetReady,
        boolean targetMissing,
        boolean targetHudPreserving,
        boolean targetMetadataOnly,
        boolean targetJavaOpaqueRenderObjectsPresent,
        boolean targetNativeWritable,
        boolean targetNativeWritableHandlesPresent,
        boolean nativeJniSubmissionWired,
        float strength,
        float alpha,
        String reason
) {
    public DirectLightingPreviewCompositeSubmissionResult {
        frameIndex = Math.max(0L, frameIndex);
        strength = normalizeUnit(strength, 0.0F);
        alpha = normalizeUnit(alpha, 0.0F);
        if (reason == null || reason.isBlank()) {
            reason = submitted
                    ? "direct-light preview composite submission accepted"
                    : "direct-light preview composite submission was not submitted";
        } else {
            reason = reason.trim();
        }
    }

    public static DirectLightingPreviewCompositeSubmissionResult notSubmitted(
            long frameIndex,
            boolean nativeOperational,
            boolean snapshotReady,
            boolean targetReady,
            boolean targetMissing,
            boolean targetHudPreserving,
            boolean targetMetadataOnly,
            boolean targetJavaOpaqueRenderObjectsPresent,
            boolean targetNativeWritable,
            boolean targetNativeWritableHandlesPresent,
            boolean nativeJniSubmissionWired,
            float strength,
            float alpha,
            String reason
    ) {
        return new DirectLightingPreviewCompositeSubmissionResult(
                false,
                frameIndex,
                nativeOperational,
                snapshotReady,
                targetReady,
                targetMissing,
                targetHudPreserving,
                targetMetadataOnly,
                targetJavaOpaqueRenderObjectsPresent,
                targetNativeWritable,
                targetNativeWritableHandlesPresent,
                nativeJniSubmissionWired,
                strength,
                alpha,
                reason
        );
    }

    public String targetStatusLabel() {
        if (this.targetMissing) {
            return "target_missing";
        }
        if (this.targetNativeWritableHandlesPresent) {
            return "native_writable_handles_present";
        }
        if (this.targetJavaOpaqueRenderObjectsPresent) {
            return "java_opaque_render_objects_present";
        }
        if (this.targetMetadataOnly) {
            return "metadata_only";
        }
        return this.targetReady ? "target_ready_without_native_handles" : "target_not_ready";
    }

    public String nativeSubmissionStatusLabel() {
        if (this.submitted) {
            return "submitted";
        }
        return this.nativeJniSubmissionWired ? "not_submitted" : "native_jni_submission_not_wired";
    }

    private static float normalizeUnit(float value, float fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
