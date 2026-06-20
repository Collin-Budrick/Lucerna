package net.lucerna.render.lighting.restir.direct;

public record DirectRestirTemporalReuseSummary(
        DirectRestirReuseMode mode,
        long currentFrameIndex,
        long previousFrameIndex,
        int previousReservoirCount,
        int acceptedReservoirCount,
        int rejectedReservoirCount,
        float historyConfidence,
        String rejectionReason
) {
    public DirectRestirTemporalReuseSummary {
        mode = mode == null ? DirectRestirReuseMode.DISABLED : mode;
        if (currentFrameIndex < 0L) {
            throw new IllegalArgumentException("currentFrameIndex must be non-negative");
        }
        if (previousFrameIndex < 0L) {
            throw new IllegalArgumentException("previousFrameIndex must be non-negative");
        }
        if (previousReservoirCount < 0) {
            throw new IllegalArgumentException("previousReservoirCount must be non-negative");
        }
        if (acceptedReservoirCount < 0) {
            throw new IllegalArgumentException("acceptedReservoirCount must be non-negative");
        }
        if (rejectedReservoirCount < 0) {
            throw new IllegalArgumentException("rejectedReservoirCount must be non-negative");
        }
        requireUnitFinite(historyConfidence, "historyConfidence");
        rejectionReason = clean(rejectionReason, "none");
        if (acceptedReservoirCount + rejectedReservoirCount > previousReservoirCount) {
            throw new IllegalArgumentException("accepted and rejected reservoirs cannot exceed previousReservoirCount");
        }
    }

    public static DirectRestirTemporalReuseSummary disabled(long currentFrameIndex) {
        return new DirectRestirTemporalReuseSummary(
                DirectRestirReuseMode.DISABLED,
                currentFrameIndex,
                0L,
                0,
                0,
                0,
                0.0F,
                "temporal reuse disabled"
        );
    }

    public boolean hasReuseEvidence() {
        return this.mode != DirectRestirReuseMode.DISABLED && this.acceptedReservoirCount > 0;
    }

    private static void requireUnitFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be between zero and one");
        }
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
