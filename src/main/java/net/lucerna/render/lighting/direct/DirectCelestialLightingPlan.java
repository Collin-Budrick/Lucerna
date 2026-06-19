package net.lucerna.render.lighting.direct;

import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.frame.WorldRenderState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record DirectCelestialLightingPlan(
        long frameIndex,
        String dimensionId,
        DirectCelestialLight sun,
        DirectCelestialLight moon,
        float weatherVisibility,
        boolean worldTimeAvailable
) {
    public static final String UNKNOWN_DIMENSION = "unknown";

    public DirectCelestialLightingPlan {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        dimensionId = cleanDimensionId(dimensionId);
        Objects.requireNonNull(sun, "sun");
        Objects.requireNonNull(moon, "moon");
        weatherVisibility = clampUnit(weatherVisibility);
    }

    public static DirectCelestialLightingPlan unavailable(long frameIndex) {
        return new DirectCelestialLightingPlan(
                frameIndex,
                UNKNOWN_DIMENSION,
                DirectCelestialLight.disabled(DirectCelestialLightSource.SUN),
                DirectCelestialLight.disabled(DirectCelestialLightSource.MOON),
                0.0F,
                false
        );
    }

    public static DirectCelestialLightingPlan fromFrameConstants(LucernaFrameConstants constants) {
        Objects.requireNonNull(constants, "constants");
        WorldRenderState worldState = constants.worldState();
        if (worldState == null || !worldState.available()) {
            return unavailable(constants.frameIndex());
        }

        float weatherVisibility = weatherVisibility(worldState);
        float celestialAngle = resolveCelestialAngle(worldState);
        DirectLightDirection sunDirection = celestialDirection(celestialAngle);
        DirectLightDirection moonDirection = celestialDirection(celestialAngle + 0.5F);
        float sunIlluminance = weatherVisibility * horizonVisibility(sunDirection);
        float moonIlluminance = weatherVisibility * horizonVisibility(moonDirection) * 0.08F;

        return new DirectCelestialLightingPlan(
                constants.frameIndex(),
                worldState.dimensionId(),
                DirectCelestialLight.sun(sunDirection, DirectLightColor.white(), sunIlluminance, true),
                DirectCelestialLight.moon(moonDirection, DirectLightColor.moonTint(), moonIlluminance, true),
                weatherVisibility,
                worldState.hasTimeOfDay()
        );
    }

    public boolean hasDimension() {
        return !UNKNOWN_DIMENSION.equals(this.dimensionId);
    }

    public List<DirectCelestialLight> activeLights() {
        List<DirectCelestialLight> activeLights = new ArrayList<>(2);
        if (this.sun.enabled()) {
            activeLights.add(this.sun);
        }
        if (this.moon.enabled()) {
            activeLights.add(this.moon);
        }
        return List.copyOf(activeLights);
    }

    public int activeLightCount() {
        return this.activeLights().size();
    }

    public boolean hasActiveLight() {
        return this.sun.enabled() || this.moon.enabled();
    }

    public boolean hasActiveShadowCaster() {
        return this.sun.castsEffectiveShadows() || this.moon.castsEffectiveShadows();
    }

    private static DirectLightDirection celestialDirection(float celestialAngle) {
        float normalizedAngle = normalizeAngle(celestialAngle);
        double radians = (normalizedAngle - 0.25D) * 2.0D * Math.PI;
        float x = (float) -Math.sin(radians);
        float y = (float) Math.cos(radians);
        return new DirectLightDirection(x, y, 0.0F).normalized();
    }

    private static float horizonVisibility(DirectLightDirection direction) {
        return clampUnit((direction.y() + 0.05F) / 0.35F);
    }

    private static float weatherVisibility(WorldRenderState worldState) {
        float rainAttenuation = worldState.raining() ? worldState.rainIntensity() * 0.45F : 0.0F;
        float thunderAttenuation = worldState.thundering() ? worldState.thunderIntensity() * 0.75F : 0.0F;
        return clampUnit(1.0F - Math.max(rainAttenuation, thunderAttenuation));
    }

    private static float resolveCelestialAngle(WorldRenderState worldState) {
        if (worldState.hasCelestialAngle()) {
            return worldState.celestialAngle();
        }
        if (worldState.hasTimeOfDay()) {
            return (worldState.timeOfDay() % 24000L) / 24000.0F;
        }
        return 0.0F;
    }

    private static float normalizeAngle(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        float normalized = value % 1.0F;
        if (normalized < 0.0F) {
            normalized += 1.0F;
        }
        return normalized;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String cleanDimensionId(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_DIMENSION;
        }
        return value.trim();
    }
}
