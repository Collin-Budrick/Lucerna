package net.lucerna.render.lighting.post;

import net.lucerna.config.QualityPreset;

public record EdgeAwareDenoiseSettings(
        boolean enabled,
        int radiusPixels,
        int iterationCount,
        float spatialSigma,
        float depthSigma,
        float normalSigma,
        float luminanceSigma,
        float historyBlend,
        float historyClampSigma,
        boolean bilateralUpsampleGi
) {
    private static final int MAX_RADIUS_PIXELS = 8;
    private static final int MAX_ITERATIONS = 4;

    public EdgeAwareDenoiseSettings {
        radiusPixels = clamp(radiusPixels, 0, MAX_RADIUS_PIXELS);
        iterationCount = clamp(iterationCount, 0, MAX_ITERATIONS);
        spatialSigma = positiveFinite(spatialSigma, 1.0F);
        depthSigma = positiveFinite(depthSigma, 0.02F);
        normalSigma = clamp01(finiteOr(normalSigma, 0.85F));
        luminanceSigma = positiveFinite(luminanceSigma, 0.2F);
        historyBlend = clamp01(finiteOr(historyBlend, 0.75F));
        historyClampSigma = positiveFinite(historyClampSigma, 1.5F);
    }

    public static EdgeAwareDenoiseSettings disabled() {
        return new EdgeAwareDenoiseSettings(
                false,
                0,
                0,
                1.0F,
                0.02F,
                0.85F,
                0.2F,
                0.0F,
                1.0F,
                false
        );
    }

    public static EdgeAwareDenoiseSettings forPreset(QualityPreset preset) {
        if (preset == null) {
            preset = QualityPreset.BALANCED;
        }
        return switch (preset) {
            case PERFORMANCE -> new EdgeAwareDenoiseSettings(
                    true, 1, 1, 0.85F, 0.025F, 0.82F, 0.28F, 0.65F, 1.25F, true);
            case BALANCED -> new EdgeAwareDenoiseSettings(
                    true, 2, 1, 1.10F, 0.020F, 0.88F, 0.22F, 0.75F, 1.50F, true);
            case QUALITY -> new EdgeAwareDenoiseSettings(
                    true, 2, 2, 1.35F, 0.015F, 0.92F, 0.18F, 0.82F, 1.75F, true);
            case EXPERIMENTAL -> new EdgeAwareDenoiseSettings(
                    true, 3, 2, 1.60F, 0.012F, 0.94F, 0.14F, 0.88F, 2.00F, true);
        };
    }

    public boolean edgeAware() {
        return this.enabled && this.radiusPixels > 0 && this.iterationCount > 0;
    }

    public boolean historyAware() {
        return this.enabled && this.historyBlend > 0.0F;
    }

    public int sampleDiameterPixels() {
        return this.radiusPixels * 2 + 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveFinite(float value, float fallback) {
        value = finiteOr(value, fallback);
        return value <= 0.0F ? fallback : value;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
