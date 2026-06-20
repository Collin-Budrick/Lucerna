package net.lucerna.render.lighting.restir.gi;

import java.util.Objects;

public record GiBounceConfidence(
        int bounceIndex,
        GiPathLengthBucket pathLengthBucket,
        float confidence,
        float variance,
        int sampleCount,
        boolean cacheBacked,
        boolean invalidated,
        String reason
) {
    public GiBounceConfidence {
        bounceIndex = Math.max(0, bounceIndex);
        pathLengthBucket = pathLengthBucket == null ? GiPathLengthBucket.NONE : pathLengthBucket;
        confidence = clampUnit(confidence);
        variance = finiteNonNegative(variance);
        sampleCount = Math.max(0, sampleCount);
        reason = clean(reason, invalidated ? "GI bounce metadata invalidated" : "GI bounce metadata");
    }

    public static GiBounceConfidence unavailable(String reason) {
        return new GiBounceConfidence(0, GiPathLengthBucket.NONE, 0.0F, 1.0F, 0, false, false, reason);
    }

    public static GiBounceConfidence fromSamples(
            int bounceIndex,
            int pathLength,
            int sampleCount,
            float variance,
            boolean cacheBacked,
            boolean invalidated,
            String reason
    ) {
        int safeSampleCount = Math.max(0, sampleCount);
        float safeVariance = finiteNonNegative(variance);
        float sampleConfidence = Math.min(1.0F, safeSampleCount / 24.0F);
        float varianceConfidence = 1.0F / (1.0F + safeVariance);
        float cacheMultiplier = cacheBacked ? 1.0F : 0.65F;
        float invalidationMultiplier = invalidated ? 0.20F : 1.0F;
        return new GiBounceConfidence(
                bounceIndex,
                GiPathLengthBucket.fromPathLength(pathLength),
                sampleConfidence * varianceConfidence * cacheMultiplier * invalidationMultiplier,
                safeVariance,
                safeSampleCount,
                cacheBacked,
                invalidated,
                reason
        );
    }

    public boolean usable(float confidenceFloor) {
        return !this.invalidated && this.confidence >= clampUnit(confidenceFloor);
    }

    public boolean hasPathMetadata() {
        return this.pathLengthBucket.active() && this.sampleCount > 0;
    }

    public String compactLabel() {
        return "bounce=" + this.bounceIndex
                + " bucket=" + this.pathLengthBucket
                + " confidence=" + this.confidence
                + " variance=" + this.variance
                + " samples=" + this.sampleCount
                + " cacheBacked=" + this.cacheBacked
                + " invalidated=" + this.invalidated;
    }

    static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    static float finiteNonNegative(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }

    static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI path reservoir metadata");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
