package net.lucerna.render.culling;

public record CullingCameraMetadata(
        double positionX,
        double positionY,
        double positionZ,
        double forwardX,
        double forwardY,
        double forwardZ,
        double horizontalFovDegrees,
        double maxDistanceBlocks
) {
    public CullingCameraMetadata {
        double length = Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
        if (length <= 0.000001D) {
            throw new IllegalArgumentException("camera forward vector must be non-zero");
        }
        forwardX /= length;
        forwardY /= length;
        forwardZ /= length;
        if (horizontalFovDegrees <= 0.0D || horizontalFovDegrees > 180.0D) {
            throw new IllegalArgumentException("horizontalFovDegrees must be in (0, 180]");
        }
        if (maxDistanceBlocks < 0.0D) {
            throw new IllegalArgumentException("maxDistanceBlocks must be non-negative");
        }
    }

    public double halfFovCosine(double paddingDegrees) {
        double paddedHalfFov = Math.min(179.0D, this.horizontalFovDegrees + paddingDegrees) * 0.5D;
        return Math.cos(Math.toRadians(paddedHalfFov));
    }
}
