package net.lucerna.render.lighting.gi;

import net.lucerna.config.QualityPreset;

public record DiffuseGiSettings(
        int internalScaleDivisor,
        int samplesPerCell,
        int maxTemporalFrames,
        float temporalBlendFactor,
        float historyConfidenceFloor,
        boolean surfaceCacheEnabled,
        boolean radianceCacheEnabled,
        boolean adaptiveRayBudgetEnabled
) {
    public DiffuseGiSettings {
        if (internalScaleDivisor <= 0) {
            throw new IllegalArgumentException("internalScaleDivisor must be positive");
        }
        requireNonNegative(samplesPerCell, "samplesPerCell");
        requireNonNegative(maxTemporalFrames, "maxTemporalFrames");
        temporalBlendFactor = clampUnit(temporalBlendFactor);
        historyConfidenceFloor = clampUnit(historyConfidenceFloor);
    }

    public static DiffuseGiSettings disabled() {
        return new DiffuseGiSettings(1, 0, 0, 0.0F, 1.0F, false, false, false);
    }

    public static DiffuseGiSettings firstMilestoneDefaults(int internalScaleDivisor) {
        return new DiffuseGiSettings(
                Math.max(1, internalScaleDivisor),
                2,
                16,
                0.10F,
                0.65F,
                true,
                true,
                true
        );
    }

    public static DiffuseGiSettings fromQuality(QualityPreset preset, int preferredScaleDivisor) {
        QualityPreset resolvedPreset = preset == null ? QualityPreset.BALANCED : preset;
        int scaleDivisor = Math.max(1, preferredScaleDivisor);
        return switch (resolvedPreset) {
            case PERFORMANCE -> new DiffuseGiSettings(Math.max(2, scaleDivisor), 1, 8, 0.14F, 0.72F, true, true, true);
            case BALANCED -> firstMilestoneDefaults(Math.max(2, scaleDivisor));
            case QUALITY -> new DiffuseGiSettings(Math.max(1, Math.min(2, scaleDivisor)), 3, 24, 0.08F, 0.58F, true, true, true);
            case EXPERIMENTAL -> new DiffuseGiSettings(1, 4, 32, 0.06F, 0.50F, true, true, true);
        };
    }

    public boolean enabled() {
        return this.samplesPerCell > 0;
    }

    public boolean temporalAccumulationEnabled() {
        return this.enabled() && this.maxTemporalFrames > 0 && this.temporalBlendFactor > 0.0F;
    }

    public boolean anyCacheEnabled() {
        return this.surfaceCacheEnabled || this.radianceCacheEnabled;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
