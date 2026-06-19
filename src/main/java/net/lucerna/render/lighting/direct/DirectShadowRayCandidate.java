package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelRay;

import java.util.Objects;

public record DirectShadowRayCandidate(
        DirectShadowRaySource source,
        String sourceKey,
        VoxelRay ray,
        float contributionWeight,
        long sourceGeneration,
        int sampleIndex
) {
    private static final float DEFAULT_MIN_SHADOW_DISTANCE = 0.01F;

    public DirectShadowRayCandidate {
        Objects.requireNonNull(source, "source");
        sourceKey = requireText(sourceKey, "sourceKey");
        Objects.requireNonNull(ray, "ray");
        requireNonNegativeFinite(contributionWeight, "contributionWeight");
        if (sourceGeneration < 0L) {
            throw new IllegalArgumentException("sourceGeneration must be non-negative");
        }
        if (sampleIndex < 0) {
            throw new IllegalArgumentException("sampleIndex must be non-negative");
        }
    }

    public static DirectShadowRayCandidate forCelestialLight(
            DirectCelestialLight light,
            float originX,
            float originY,
            float originZ,
            float maxDistance,
            long sourceGeneration,
            int sampleIndex
    ) {
        Objects.requireNonNull(light, "light");
        requireShadowDistance(maxDistance);
        DirectLightDirection direction = light.direction().normalized();
        return new DirectShadowRayCandidate(
                sourceFrom(light.source()),
                light.sourceKey(),
                new VoxelRay(
                        originX,
                        originY,
                        originZ,
                        direction.x(),
                        direction.y(),
                        direction.z(),
                        DEFAULT_MIN_SHADOW_DISTANCE,
                        maxDistance
                ),
                light.weightedEnergy(),
                sourceGeneration,
                sampleIndex
        );
    }

    public static DirectShadowRayCandidate forEmissiveBlock(
            DirectEmissiveBlockLight light,
            float originX,
            float originY,
            float originZ,
            int sampleIndex
    ) {
        Objects.requireNonNull(light, "light");
        float targetX = light.blockX() + 0.5F;
        float targetY = light.blockY() + 0.5F;
        float targetZ = light.blockZ() + 0.5F;
        float deltaX = targetX - originX;
        float deltaY = targetY - originY;
        float deltaZ = targetZ - originZ;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance <= DEFAULT_MIN_SHADOW_DISTANCE) {
            throw new IllegalArgumentException("origin must not be at the emissive block sample center");
        }
        DirectLightDirection direction = new DirectLightDirection(deltaX, deltaY, deltaZ).normalized();
        return new DirectShadowRayCandidate(
                DirectShadowRaySource.EMISSIVE_BLOCK,
                light.stableKey(),
                new VoxelRay(
                        originX,
                        originY,
                        originZ,
                        direction.x(),
                        direction.y(),
                        direction.z(),
                        DEFAULT_MIN_SHADOW_DISTANCE,
                        distance
                ),
                light.priority(),
                light.generation(),
                sampleIndex
        );
    }

    public boolean contributesLighting() {
        return this.contributionWeight > 0.0F;
    }

    private static DirectShadowRaySource sourceFrom(DirectCelestialLightSource source) {
        return switch (source) {
            case SUN -> DirectShadowRaySource.SUN;
            case MOON -> DirectShadowRaySource.MOON;
        };
    }

    private static void requireShadowDistance(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("maxDistance must be finite");
        }
        if (value <= DEFAULT_MIN_SHADOW_DISTANCE) {
            throw new IllegalArgumentException("maxDistance must exceed the minimum shadow distance");
        }
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
