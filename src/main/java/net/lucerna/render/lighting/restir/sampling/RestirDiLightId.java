package net.lucerna.render.lighting.restir.sampling;

import java.util.Objects;

public record RestirDiLightId(
        String namespace,
        String stableKey,
        long generation
) {
    public RestirDiLightId {
        namespace = clean(namespace, "namespace");
        stableKey = clean(stableKey, "stableKey");
        generation = Math.max(0L, generation);
    }

    public static RestirDiLightId emissiveBlock(String dimension, int blockX, int blockY, int blockZ, int materialId, long generation) {
        String key = clean(dimension, "dimension") + ":" + blockX + "," + blockY + "," + blockZ + ":" + Math.max(0, materialId);
        return new RestirDiLightId("emissive_block", key, generation);
    }

    public static RestirDiLightId celestial(String dimension, String sourceName, long generation) {
        return new RestirDiLightId("celestial", clean(dimension, "dimension") + ":" + clean(sourceName, "sourceName"), generation);
    }

    public String compactLabel() {
        return this.namespace + ":" + this.stableKey + "@gen=" + this.generation;
    }

    private static String clean(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
