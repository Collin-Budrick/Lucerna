package net.lucerna.render.lighting.gi;

import java.util.Collection;
import java.util.Objects;

public record CacheConfidence(
        float confidence,
        float variance,
        int sampleCount,
        long sourceGeneration,
        long lastTouchedFrame,
        boolean dirty,
        String reason
) {
    public CacheConfidence {
        confidence = clampUnit(confidence);
        variance = finiteNonNegative(variance);
        sampleCount = Math.max(0, sampleCount);
        sourceGeneration = Math.max(0L, sourceGeneration);
        lastTouchedFrame = Math.max(0L, lastTouchedFrame);
        reason = clean(reason, dirty ? "dirty" : "available");
    }

    public static CacheConfidence empty(String reason) {
        return new CacheConfidence(0.0F, 1.0F, 0, 0L, 0L, false, reason);
    }

    public static CacheConfidence dirty(long sourceGeneration, String reason) {
        return new CacheConfidence(0.0F, 1.0F, 0, sourceGeneration, 0L, true, reason);
    }

    public static CacheConfidence fromSamples(
            int sampleCount,
            float variance,
            long sourceGeneration,
            long lastTouchedFrame,
            boolean dirty
    ) {
        int safeSampleCount = Math.max(0, sampleCount);
        float safeVariance = finiteNonNegative(variance);
        float sampleConfidence = Math.min(1.0F, safeSampleCount / 32.0F);
        float varianceConfidence = 1.0F / (1.0F + safeVariance);
        float dirtyPenalty = dirty ? 0.25F : 1.0F;
        return new CacheConfidence(
                sampleConfidence * varianceConfidence * dirtyPenalty,
                safeVariance,
                safeSampleCount,
                sourceGeneration,
                lastTouchedFrame,
                dirty,
                dirty ? "dirty samples" : "sampled"
        );
    }

    public static CacheConfidence merge(Collection<CacheConfidence> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            return empty("no cache records");
        }

        float weightedConfidence = 0.0F;
        int weightTotal = 0;
        int sampleTotal = 0;
        float maxVariance = 0.0F;
        long maxSourceGeneration = 0L;
        long maxTouchedFrame = 0L;
        boolean dirty = false;

        for (CacheConfidence value : values) {
            Objects.requireNonNull(value, "values must not contain null entries");
            int weight = Math.max(1, value.sampleCount());
            weightedConfidence += value.confidence() * weight;
            weightTotal += weight;
            sampleTotal += value.sampleCount();
            maxVariance = Math.max(maxVariance, value.variance());
            maxSourceGeneration = Math.max(maxSourceGeneration, value.sourceGeneration());
            maxTouchedFrame = Math.max(maxTouchedFrame, value.lastTouchedFrame());
            dirty = dirty || value.dirty();
        }

        return new CacheConfidence(
                weightTotal == 0 ? 0.0F : weightedConfidence / weightTotal,
                maxVariance,
                sampleTotal,
                maxSourceGeneration,
                maxTouchedFrame,
                dirty,
                dirty ? "merged dirty cache records" : "merged cache records"
        );
    }

    public boolean usable(float confidenceFloor) {
        return !this.dirty && this.confidence >= clampUnit(confidenceFloor);
    }

    public boolean needsRefresh(float confidenceFloor, float maxVariance) {
        return this.dirty || this.confidence < clampUnit(confidenceFloor) || this.variance > finiteNonNegative(maxVariance);
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float finiteNonNegative(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
