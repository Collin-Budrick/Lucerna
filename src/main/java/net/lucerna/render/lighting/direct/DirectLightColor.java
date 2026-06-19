package net.lucerna.render.lighting.direct;

public record DirectLightColor(
        float red,
        float green,
        float blue
) {
    public DirectLightColor {
        requireNonNegativeFinite(red, "red");
        requireNonNegativeFinite(green, "green");
        requireNonNegativeFinite(blue, "blue");
    }

    public static DirectLightColor black() {
        return new DirectLightColor(0.0F, 0.0F, 0.0F);
    }

    public static DirectLightColor white() {
        return new DirectLightColor(1.0F, 1.0F, 1.0F);
    }

    public static DirectLightColor moonTint() {
        return new DirectLightColor(0.58F, 0.68F, 1.0F);
    }

    public static DirectLightColor warmEmissive() {
        return new DirectLightColor(1.0F, 0.72F, 0.42F);
    }

    public boolean hasEnergy() {
        return this.red > 0.0F || this.green > 0.0F || this.blue > 0.0F;
    }

    public float luminance() {
        return this.red * 0.2126F + this.green * 0.7152F + this.blue * 0.0722F;
    }

    public DirectLightColor scaled(float scale) {
        requireNonNegativeFinite(scale, "scale");
        return new DirectLightColor(this.red * scale, this.green * scale, this.blue * scale);
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
