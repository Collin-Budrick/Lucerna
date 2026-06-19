package net.lucerna.render.lighting.direct;

import java.util.Objects;

public record DirectCelestialLight(
        DirectCelestialLightSource source,
        DirectLightDirection direction,
        DirectLightColor color,
        float illuminance,
        float angularRadiusRadians,
        boolean castsShadows
) {
    public static final float DEFAULT_SUN_ANGULAR_RADIUS_RADIANS = 0.00465F;
    public static final float DEFAULT_MOON_ANGULAR_RADIUS_RADIANS = 0.0045F;

    public DirectCelestialLight {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(color, "color");
        requireNonNegativeFinite(illuminance, "illuminance");
        requireNonNegativeFinite(angularRadiusRadians, "angularRadiusRadians");
        if (angularRadiusRadians > Math.PI) {
            throw new IllegalArgumentException("angularRadiusRadians must not exceed pi");
        }
    }

    public static DirectCelestialLight sun(
            DirectLightDirection direction,
            DirectLightColor color,
            float illuminance,
            boolean castsShadows
    ) {
        return new DirectCelestialLight(
                DirectCelestialLightSource.SUN,
                direction,
                color,
                illuminance,
                DEFAULT_SUN_ANGULAR_RADIUS_RADIANS,
                castsShadows
        );
    }

    public static DirectCelestialLight moon(
            DirectLightDirection direction,
            DirectLightColor color,
            float illuminance,
            boolean castsShadows
    ) {
        return new DirectCelestialLight(
                DirectCelestialLightSource.MOON,
                direction,
                color,
                illuminance,
                DEFAULT_MOON_ANGULAR_RADIUS_RADIANS,
                castsShadows
        );
    }

    public static DirectCelestialLight disabled(DirectCelestialLightSource source) {
        Objects.requireNonNull(source, "source");
        float angularRadius = source == DirectCelestialLightSource.SUN
                ? DEFAULT_SUN_ANGULAR_RADIUS_RADIANS
                : DEFAULT_MOON_ANGULAR_RADIUS_RADIANS;
        return new DirectCelestialLight(
                source,
                DirectLightDirection.up(),
                DirectLightColor.black(),
                0.0F,
                angularRadius,
                false
        );
    }

    public boolean enabled() {
        return this.illuminance > 0.0F && this.color.hasEnergy();
    }

    public boolean castsEffectiveShadows() {
        return this.enabled() && this.castsShadows;
    }

    public String sourceKey() {
        return this.source.wireName();
    }

    public float weightedEnergy() {
        return this.illuminance * this.color.luminance();
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
