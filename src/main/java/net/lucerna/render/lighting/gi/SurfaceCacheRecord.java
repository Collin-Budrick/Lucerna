package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record SurfaceCacheRecord(
        SurfaceCacheKey key,
        long generation,
        int materialId,
        float normalX,
        float normalY,
        float normalZ,
        float albedoR,
        float albedoG,
        float albedoB,
        float roughness,
        CacheConfidence confidence
) {
    public SurfaceCacheRecord {
        Objects.requireNonNull(key, "key");
        generation = Math.max(0L, generation);
        materialId = Math.max(0, materialId);
        normalX = finiteOrZero(normalX);
        normalY = finiteOrZero(normalY);
        normalZ = finiteOrZero(normalZ);
        albedoR = clampUnit(albedoR);
        albedoG = clampUnit(albedoG);
        albedoB = clampUnit(albedoB);
        roughness = clampUnit(roughness);
        if (confidence == null) {
            confidence = CacheConfidence.empty("surface cache confidence unavailable");
        }
    }

    public boolean usable(float confidenceFloor) {
        return this.confidence.usable(confidenceFloor);
    }

    public boolean dirty() {
        return this.confidence.dirty();
    }

    public SurfaceCacheRecord withConfidence(CacheConfidence confidence) {
        return new SurfaceCacheRecord(
                this.key,
                this.generation,
                this.materialId,
                this.normalX,
                this.normalY,
                this.normalZ,
                this.albedoR,
                this.albedoG,
                this.albedoB,
                this.roughness,
                confidence
        );
    }

    private static float finiteOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return value;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
