package net.lucerna.render.lighting.restir.direct;

public record DirectRestirSpatialReuseSummary(
        DirectRestirReuseMode mode,
        int neighborRadiusPixels,
        int neighborReservoirCount,
        int acceptedNeighborCount,
        int rejectedNeighborCount,
        float neighborhoodConfidence,
        String rejectionReason
) {
    public DirectRestirSpatialReuseSummary {
        mode = mode == null ? DirectRestirReuseMode.DISABLED : mode;
        if (neighborRadiusPixels < 0) {
            throw new IllegalArgumentException("neighborRadiusPixels must be non-negative");
        }
        if (neighborReservoirCount < 0) {
            throw new IllegalArgumentException("neighborReservoirCount must be non-negative");
        }
        if (acceptedNeighborCount < 0) {
            throw new IllegalArgumentException("acceptedNeighborCount must be non-negative");
        }
        if (rejectedNeighborCount < 0) {
            throw new IllegalArgumentException("rejectedNeighborCount must be non-negative");
        }
        requireUnitFinite(neighborhoodConfidence, "neighborhoodConfidence");
        rejectionReason = clean(rejectionReason, "none");
        if (acceptedNeighborCount + rejectedNeighborCount > neighborReservoirCount) {
            throw new IllegalArgumentException("accepted and rejected neighbors cannot exceed neighborReservoirCount");
        }
    }

    public static DirectRestirSpatialReuseSummary disabled() {
        return new DirectRestirSpatialReuseSummary(
                DirectRestirReuseMode.DISABLED,
                0,
                0,
                0,
                0,
                0.0F,
                "spatial reuse disabled"
        );
    }

    public boolean hasReuseEvidence() {
        return this.mode != DirectRestirReuseMode.DISABLED && this.acceptedNeighborCount > 0;
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
