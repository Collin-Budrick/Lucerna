package net.lucerna.render.frame;

public record WorldRenderState(
        String dimensionId,
        long timeOfDay,
        float celestialAngle,
        boolean raining,
        boolean thundering,
        float rainIntensity,
        float thunderIntensity
) {
    public static final String UNKNOWN_DIMENSION = "unknown";

    public WorldRenderState {
        dimensionId = cleanDimensionId(dimensionId);
        timeOfDay = Math.max(-1L, timeOfDay);
        celestialAngle = normalizeCelestialAngle(celestialAngle);
        rainIntensity = clampUnit(rainIntensity);
        thunderIntensity = clampUnit(thunderIntensity);
    }

    public static WorldRenderState unavailable() {
        return new WorldRenderState(UNKNOWN_DIMENSION, -1L, -1.0F, false, false, 0.0F, 0.0F);
    }

    public boolean hasDimension() {
        return !UNKNOWN_DIMENSION.equals(this.dimensionId);
    }

    public boolean hasTimeOfDay() {
        return this.timeOfDay >= 0L;
    }

    public boolean hasCelestialAngle() {
        return this.celestialAngle >= 0.0F;
    }

    public boolean available() {
        return this.hasDimension() && this.hasTimeOfDay();
    }

    public String weatherLabel() {
        if (this.thundering) {
            return "thunder " + this.thunderIntensity;
        }
        if (this.raining) {
            return "rain " + this.rainIntensity;
        }
        return "clear";
    }

    private static String cleanDimensionId(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_DIMENSION;
        }
        return value.trim();
    }

    private static float normalizeCelestialAngle(float value) {
        if (!Float.isFinite(value)) {
            return -1.0F;
        }
        if (value < 0.0F) {
            return -1.0F;
        }
        if (value > 1.0F) {
            return value % 1.0F;
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
