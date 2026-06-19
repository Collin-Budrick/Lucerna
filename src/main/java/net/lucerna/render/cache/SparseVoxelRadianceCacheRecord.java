package net.lucerna.render.cache;

import java.util.Objects;

public record SparseVoxelRadianceCacheRecord(
        SparseVoxelRadianceCacheKey key,
        long generation,
        float radianceR,
        float radianceG,
        float radianceB,
        float directionX,
        float directionY,
        float directionZ,
        float variance,
        int sampleCount,
        long lastFrameIndex,
        SparseVoxelRadianceCacheConfidence confidence
) {
    public SparseVoxelRadianceCacheRecord {
        Objects.requireNonNull(key, "key");
        generation = Math.max(0L, generation);
        radianceR = finiteNonNegative(radianceR);
        radianceG = finiteNonNegative(radianceG);
        radianceB = finiteNonNegative(radianceB);
        directionX = finiteOrZero(directionX);
        directionY = finiteOrZero(directionY);
        directionZ = finiteOrZero(directionZ);
        variance = finiteNonNegative(variance);
        sampleCount = Math.max(0, sampleCount);
        lastFrameIndex = Math.max(0L, lastFrameIndex);
        if (confidence == null) {
            confidence = SparseVoxelRadianceCacheConfidence.fromSamples(sampleCount, variance, generation, lastFrameIndex, false);
        }
    }

    public static SparseVoxelRadianceCacheRecord dirtyPlaceholder(
            SparseVoxelRadianceCacheKey key,
            long generation,
            String reason
    ) {
        return new SparseVoxelRadianceCacheRecord(
                key,
                generation,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                1.0F,
                0.0F,
                1.0F,
                0,
                0L,
                SparseVoxelRadianceCacheConfidence.dirty(generation, reason)
        );
    }

    public boolean usable(float confidenceFloor) {
        return this.confidence.usable(confidenceFloor);
    }

    public boolean dirty() {
        return this.confidence.dirty();
    }

    public boolean directionLooksNormalized() {
        float lengthSquared = this.directionX * this.directionX + this.directionY * this.directionY + this.directionZ * this.directionZ;
        return lengthSquared > 0.9F && lengthSquared < 1.1F;
    }

    public SparseVoxelRadianceCacheRecord withConfidence(SparseVoxelRadianceCacheConfidence confidence) {
        return new SparseVoxelRadianceCacheRecord(
                this.key,
                this.generation,
                this.radianceR,
                this.radianceG,
                this.radianceB,
                this.directionX,
                this.directionY,
                this.directionZ,
                this.variance,
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
