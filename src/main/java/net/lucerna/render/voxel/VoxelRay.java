package net.lucerna.render.voxel;

public record VoxelRay(
        float originX,
        float originY,
        float originZ,
        float directionX,
        float directionY,
        float directionZ,
        float minDistance,
        float maxDistance
) {
    private static final float NORMALIZED_EPSILON = 0.001F;

    public VoxelRay {
        requireFinite(originX, "originX");
        requireFinite(originY, "originY");
        requireFinite(originZ, "originZ");
        requireFinite(directionX, "directionX");
        requireFinite(directionY, "directionY");
        requireFinite(directionZ, "directionZ");
        requireFinite(minDistance, "minDistance");
        requireFinite(maxDistance, "maxDistance");
        if (directionLengthSquared(directionX, directionY, directionZ) <= 0.0F) {
            throw new IllegalArgumentException("ray direction must be non-zero");
        }
        if (minDistance < 0.0F) {
            throw new IllegalArgumentException("minDistance must be non-negative");
        }
        if (maxDistance <= minDistance) {
            throw new IllegalArgumentException("maxDistance must be greater than minDistance");
        }
    }

    public float directionLengthSquared() {
        return directionLengthSquared(this.directionX, this.directionY, this.directionZ);
    }

    public boolean directionLooksNormalized() {
        return Math.abs(this.directionLengthSquared() - 1.0F) <= NORMALIZED_EPSILON;
    }

    public float distanceRange() {
        return this.maxDistance - this.minDistance;
    }

    public VoxelRay withDistanceRange(float newMinDistance, float newMaxDistance) {
        return new VoxelRay(
                this.originX,
                this.originY,
                this.originZ,
                this.directionX,
                this.directionY,
                this.directionZ,
                newMinDistance,
                newMaxDistance
        );
    }

    private static float directionLengthSquared(float directionX, float directionY, float directionZ) {
        return directionX * directionX + directionY * directionY + directionZ * directionZ;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
