package net.lucerna.nativebridge;

public record DirectLightingPreviewCompositeSubmissionResult(
        boolean submitted,
        long frameIndex,
        boolean nativeOperational,
        boolean snapshotReady,
        boolean targetReady,
        boolean targetHudPreserving,
        boolean targetNativeWritable,
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
            boolean targetHudPreserving,
            boolean targetNativeWritable,
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
                targetHudPreserving,
                targetNativeWritable,
                strength,
                alpha,
                reason
        );
    }

    private static float normalizeUnit(float value, float fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
