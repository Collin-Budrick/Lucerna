package net.lucerna.render.frame;

public record FrameMatrixHistory(
        long frameIndex,
        FrameCameraMatrices currentMatrices,
        long previousFrameIndex,
        FrameCameraMatrices previousMatrices,
        boolean historyReset,
        String resetReason
) {
    public FrameMatrixHistory {
        frameIndex = Math.max(0L, frameIndex);
        if (currentMatrices == null) {
            currentMatrices = FrameCameraMatrices.unavailable();
        }
        previousFrameIndex = Math.max(0L, previousFrameIndex);
        if (previousMatrices == null) {
            previousMatrices = FrameCameraMatrices.unavailable();
        }
        if (historyReset) {
            resetReason = clean(resetReason, "Temporal matrix history was reset.");
            previousFrameIndex = 0L;
            previousMatrices = FrameCameraMatrices.unavailable();
        } else {
            resetReason = clean(resetReason, "");
        }
    }

    public static FrameMatrixHistory unavailable(String reason) {
        return new FrameMatrixHistory(
                0L,
                FrameCameraMatrices.unavailable(),
                0L,
                FrameCameraMatrices.unavailable(),
                true,
                reason
        );
    }

    public boolean hasCurrentMatrices() {
        return this.currentMatrices.hasRequiredMatrices();
    }

    public boolean hasPreviousMatrices() {
        return this.previousFrameIndex > 0L && this.previousMatrices.hasRequiredMatrices();
    }

    public boolean temporalReuseAllowed() {
        return this.hasCurrentMatrices()
                && this.hasPreviousMatrices()
                && !this.historyReset
                && this.frameIndex > this.previousFrameIndex;
    }

    public String stateLabel() {
        if (!this.hasCurrentMatrices()) {
            return "unavailable";
        }
        if (this.historyReset) {
            return "reset";
        }
        if (this.temporalReuseAllowed()) {
            return "reusable";
        }
        return "current-only";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
