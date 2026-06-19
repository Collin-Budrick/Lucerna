package net.lucerna.render.lighting.post;

import net.lucerna.config.QualityPreset;

public record HistoryRejectionSettings(
        boolean enabled,
        float depthThreshold,
        float normalDotThreshold,
        float motionPixelThreshold,
        float luminanceThreshold,
        float minHistoryConfidence,
        float reactiveMaskThreshold,
        int historyRepairRadiusPixels,
        boolean rejectDirtyRegions
) {
    private static final int MAX_REPAIR_RADIUS_PIXELS = 4;

    public HistoryRejectionSettings {
        depthThreshold = positiveFinite(depthThreshold, 0.02F);
        normalDotThreshold = clamp01(finiteOr(normalDotThreshold, 0.88F));
        motionPixelThreshold = positiveFinite(motionPixelThreshold, 1.0F);
        luminanceThreshold = positiveFinite(luminanceThreshold, 0.25F);
        minHistoryConfidence = clamp01(finiteOr(minHistoryConfidence, 0.35F));
        reactiveMaskThreshold = clamp01(finiteOr(reactiveMaskThreshold, 0.60F));
        historyRepairRadiusPixels = Math.max(0, Math.min(MAX_REPAIR_RADIUS_PIXELS, historyRepairRadiusPixels));
    }

    public static HistoryRejectionSettings disabled() {
        return new HistoryRejectionSettings(
                false,
                0.02F,
                0.88F,
                1.0F,
                0.25F,
                0.0F,
                1.0F,
                0,
                false
        );
    }

    public static HistoryRejectionSettings forPreset(QualityPreset preset) {
        if (preset == null) {
            preset = QualityPreset.BALANCED;
        }
        return switch (preset) {
            case PERFORMANCE -> new HistoryRejectionSettings(
                    true, 0.030F, 0.82F, 1.50F, 0.32F, 0.30F, 0.70F, 1, true);
            case BALANCED -> new HistoryRejectionSettings(
                    true, 0.020F, 0.88F, 1.00F, 0.25F, 0.35F, 0.60F, 1, true);
            case QUALITY -> new HistoryRejectionSettings(
                    true, 0.015F, 0.91F, 0.75F, 0.20F, 0.40F, 0.55F, 2, true);
            case EXPERIMENTAL -> new HistoryRejectionSettings(
                    true, 0.012F, 0.94F, 0.50F, 0.16F, 0.45F, 0.50F, 2, true);
        };
    }

    public boolean requiresPreviousFrame() {
        return this.enabled;
    }

    public boolean repairsRejectedHistory() {
        return this.enabled && this.historyRepairRadiusPixels > 0;
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
