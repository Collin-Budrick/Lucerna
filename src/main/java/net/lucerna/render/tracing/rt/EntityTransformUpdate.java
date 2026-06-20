package net.lucerna.render.tracing.rt;

public record EntityTransformUpdate(
        String entityKey,
        String entityType,
        int instanceIndex,
        double worldX,
        double worldY,
        double worldZ,
        float rotationX,
        float rotationY,
        float rotationZ,
        float rotationW,
        float scaleX,
        float scaleY,
        float scaleZ,
        long transformGeneration,
        boolean visible,
        boolean transformChanged
) {
    public EntityTransformUpdate {
        entityKey = clean(entityKey, "unknown-entity");
        entityType = clean(entityType, "unknown");
        instanceIndex = Math.max(0, instanceIndex);
        worldX = finite(worldX);
        worldY = finite(worldY);
        worldZ = finite(worldZ);
        rotationX = finite(rotationX);
        rotationY = finite(rotationY);
        rotationZ = finite(rotationZ);
        rotationW = finiteOr(rotationW, 1.0F);
        scaleX = positiveFinite(scaleX);
        scaleY = positiveFinite(scaleY);
        scaleZ = positiveFinite(scaleZ);
        transformGeneration = Math.max(0L, transformGeneration);
    }

    public static EntityTransformUpdate identity(
            String entityKey,
            String entityType,
            int instanceIndex,
            double worldX,
            double worldY,
            double worldZ,
            long transformGeneration
    ) {
        return new EntityTransformUpdate(
                entityKey,
                entityType,
                instanceIndex,
                worldX,
                worldY,
                worldZ,
                0.0F,
                0.0F,
                0.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F,
                transformGeneration,
                true,
                true
        );
    }

    public String compactLabel() {
        return this.entityType + "#" + this.instanceIndex + "@" + this.transformGeneration;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static float finite(float value) {
        return finiteOr(value, 0.0F);
    }

    private static float positiveFinite(float value) {
        value = finiteOr(value, 1.0F);
        return value <= 0.0F ? 1.0F : value;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
