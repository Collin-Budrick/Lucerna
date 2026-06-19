package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record RadianceCacheRecord(
        RadianceCacheKey key,
        long generation,
        float radianceR,
        float radianceG,
        float radianceB,
        float directionX,
        float directionY,
        float directionZ,
        float directionalVariance,
        int sampleCount,
        long lastFrameIndex,
        CacheConfidence confidence
) {
    public RadianceCacheRecord {
        Objects.requireNonNull(key, "key");
        generation = Math.max(0L, generation);
        radianceR = finiteNonNegative(radianceR);
        radianceG = finiteNonNegative(radianceG);
        radianceB = finiteNonNegative(radianceB);
        directionX = finiteOrZero(directionX);
        directionY = finiteOrZero(directionY);
        directionZ = finiteOrZero(directionZ);
        directionalVariance = finiteNonNegative(directionalVariance);
        sampleCount = Math.max(0, sampleCount);
        lastFrameIndex = Math.max(0L, lastFrameIndex);
        if (confidence == null) {
            confidence = CacheConfidence.fromSamples(sampleCount, directionalVariance, generation, lastFrameIndex, false);
        }
    }

    public boolean usable(float confidenceFloor) {
        return this.confidence.usable(confidenceFloor);
    }

    public boolean directionLooksNormalized() {
        float lengthSquared = this.directionX * this.directionX + this.directionY * this.directionY + this.directionZ * this.directionZ;
        return lengthSquared > 0.9F && lengthSquared < 1.1F;
    }

    public RadianceCacheRecord withConfidence(CacheConfidence confidence) {
        return new RadianceCacheRecord(
                this.key,
                this.generation,
                this.radianceR,
                this.radianceG,
                this.radianceB,
                this.directionX,
                this.directionY,
                this.directionZ,
                this.directionalVariance,
                this.sampleCount,
                this.lastFrameIndex,
                confidence
        );
    }

    private static float finiteOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return value;
    }

    private static float finiteNonNegative(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }
}
