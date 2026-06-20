package net.lucerna.render.lighting.restir.reuse;

public record DirectLightNeighborTapCounts(
        int requestedTapCount,
        int attemptedTapCount,
        int acceptedTapCount,
        int rejectedTapCount,
        int missingTapCount,
        int outOfBoundsTapCount
) {
    public DirectLightNeighborTapCounts {
        requestedTapCount = Math.max(0, requestedTapCount);
        attemptedTapCount = Math.max(0, attemptedTapCount);
        acceptedTapCount = Math.max(0, acceptedTapCount);
        rejectedTapCount = Math.max(0, rejectedTapCount);
        missingTapCount = Math.max(0, missingTapCount);
        outOfBoundsTapCount = Math.max(0, outOfBoundsTapCount);
        int accounted = acceptedTapCount + rejectedTapCount + missingTapCount + outOfBoundsTapCount;
        if (attemptedTapCount < accounted) {
            attemptedTapCount = accounted;
        }
        if (requestedTapCount < attemptedTapCount) {
            requestedTapCount = attemptedTapCount;
        }
    }

    public static DirectLightNeighborTapCounts empty() {
        return new DirectLightNeighborTapCounts(0, 0, 0, 0, 0, 0);
    }

    public boolean hasAcceptedNeighborTap() {
        return this.acceptedTapCount > 0;
    }

    public String summary(String prefix) {
        String label = prefix == null || prefix.isBlank() ? "spatial" : prefix.trim();
        return label + ".requestedTapCount=" + this.requestedTapCount
                + "," + label + ".attemptedTapCount=" + this.attemptedTapCount
                + "," + label + ".acceptedTapCount=" + this.acceptedTapCount
                + "," + label + ".rejectedTapCount=" + this.rejectedTapCount
                + "," + label + ".missingTapCount=" + this.missingTapCount
                + "," + label + ".outOfBoundsTapCount=" + this.outOfBoundsTapCount;
    }
}
