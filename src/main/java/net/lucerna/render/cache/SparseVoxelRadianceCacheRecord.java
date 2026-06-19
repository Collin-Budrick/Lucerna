package net.lucerna.render.cache;

import net.lucerna.world.DirtyRegion;

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

    public static SparseVoxelRadianceCacheRecord allocatedFromDirtyRegion(
            SparseVoxelRadianceCacheKey key,
            DirtyRegion dirtyRegion,
            long cacheGeneration
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        int hash = stableHash(key, dirtyRegion);
        float baseRadiance = 0.05F + ((hash >>> 4) & 0x7) * 0.01F;
        float redBias = channel(hash, 8);
        float greenBias = channel(hash, 12);
        float blueBias = channel(hash, 16);
        float variance = 0.5F + ((hash >>> 20) & 0x3) * 0.125F;
        int sampleCount = 4 + ((hash >>> 24) & 0x7);
        long generation = Math.max(Math.max(cacheGeneration, dirtyRegion.generation()), 0L);
        return new SparseVoxelRadianceCacheRecord(
                key,
                generation,
                baseRadiance + redBias,
                baseRadiance + greenBias,
                baseRadiance + blueBias,
                0.0F,
                1.0F,
                0.0F,
                variance,
                sampleCount,
                generation,
                SparseVoxelRadianceCacheConfidence.fromSamples(
                        sampleCount,
                        variance,
                        generation,
                        generation,
                        true
                )
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

    private static float channel(int hash, int shift) {
        return ((hash >>> shift) & 0xF) / 512.0F;
    }

    private static int stableHash(SparseVoxelRadianceCacheKey key, DirtyRegion dirtyRegion) {
        int hash = 17;
        hash = 31 * hash + key.stableKey().hashCode();
        hash = 31 * hash + dirtyRegion.type().nativeTypeId();
        hash = 31 * hash + Long.hashCode(dirtyRegion.generation());
        return hash;
    }
}
