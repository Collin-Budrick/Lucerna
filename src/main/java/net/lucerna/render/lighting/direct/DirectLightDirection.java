package net.lucerna.render.lighting.direct;

public record DirectLightDirection(
        float x,
        float y,
        float z
) {
    private static final float NORMALIZED_EPSILON = 0.001F;

    public DirectLightDirection {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        if (lengthSquared(x, y, z) <= 0.0F) {
            throw new IllegalArgumentException("direction must be non-zero");
        }
    }

    public static DirectLightDirection of(float x, float y, float z) {
        return new DirectLightDirection(x, y, z);
    }

    public static DirectLightDirection up() {
        return new DirectLightDirection(0.0F, 1.0F, 0.0F);
    }

    public float lengthSquared() {
        return lengthSquared(this.x, this.y, this.z);
    }

    public boolean looksNormalized() {
        return Math.abs(this.lengthSquared() - 1.0F) <= NORMALIZED_EPSILON;
    }

    public DirectLightDirection normalized() {
        float length = (float) Math.sqrt(this.lengthSquared());
        return new DirectLightDirection(this.x / length, this.y / length, this.z / length);
    }

    private static float lengthSquared(float x, float y, float z) {
        return x * x + y * y + z * z;
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
