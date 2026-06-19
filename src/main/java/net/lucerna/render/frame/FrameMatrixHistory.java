package net.lucerna.render.frame;

public record FrameMatrixHistory(
        long frameIndex,
        FrameCameraMatrices currentMatrices,
        long previousFrameIndex,
        FrameCameraMatrices previousMatrices,
        boolean historyReset,
        String resetReason
) {
    private static final float DEFAULT_STABLE_DELTA = 0.00075F;
    private static final float DEFAULT_MOVED_DELTA = 0.006F;

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

    public float matrixDeltaMagnitude() {
        if (!this.hasCurrentMatrices() || !this.hasPreviousMatrices()) {
            return 1.0F;
        }
        return Math.max(
                averageAbsDelta(this.currentMatrices.view(), this.previousMatrices.view()),
                averageAbsDelta(this.currentMatrices.viewProjection(), this.previousMatrices.viewProjection())
        );
    }

    public boolean stableForHistory() {
        return this.temporalReuseAllowed() && this.matrixDeltaMagnitude() <= DEFAULT_STABLE_DELTA;
    }

    public boolean movedForHistory() {
        return this.hasCurrentMatrices()
                && this.hasPreviousMatrices()
                && this.matrixDeltaMagnitude() >= DEFAULT_MOVED_DELTA;
    }

    public String motionStateLabel() {
        if (!this.hasCurrentMatrices()) {
            return "unavailable";
        }
        if (this.historyReset) {
            return "reset";
        }
        if (!this.hasPreviousMatrices()) {
            return "current-only";
        }
        if (this.stableForHistory()) {
            return "stable";
        }
        if (this.movedForHistory()) {
            return "moved";
        }
        return "minor-motion";
    }

    private static float averageAbsDelta(FrameMatrix4f current, FrameMatrix4f previous) {
        float[] currentValues = current.toRowMajorArray();
        float[] previousValues = previous.toRowMajorArray();
        float sum = 0.0F;
        for (int index = 0; index < currentValues.length; index++) {
            sum += Math.abs(currentValues[index] - previousValues[index]);
        }
        return sum / currentValues.length;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
