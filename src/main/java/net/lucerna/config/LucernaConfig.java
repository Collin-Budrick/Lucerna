package net.lucerna.config;

public record LucernaConfig(
        int schemaVersion,
        boolean rendererEnabled,
        QualityPreset qualityPreset,
        DebugOverlay debugOverlay,
        boolean showIrisNotice
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public LucernaConfig {
        if (schemaVersion <= 0) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
        if (qualityPreset == null) {
            qualityPreset = QualityPreset.BALANCED;
        }
        if (debugOverlay == null) {
            debugOverlay = DebugOverlay.OFF;
        }
    }

    public LucernaConfig(
            boolean rendererEnabled,
            QualityPreset qualityPreset,
            DebugOverlay debugOverlay,
            boolean showIrisNotice
    ) {
        this(CURRENT_SCHEMA_VERSION, rendererEnabled, qualityPreset, debugOverlay, showIrisNotice);
    }

    public static LucernaConfig defaults() {
        return new LucernaConfig(CURRENT_SCHEMA_VERSION, true, QualityPreset.BALANCED, DebugOverlay.OFF, true);
    }

    public LucernaConfig normalized() {
        return new LucernaConfig(
                CURRENT_SCHEMA_VERSION,
                this.rendererEnabled,
                this.qualityPreset,
                this.debugOverlay,
                this.showIrisNotice
        );
    }

    public LucernaConfig withRendererEnabled(boolean enabled) {
        return new LucernaConfig(CURRENT_SCHEMA_VERSION, enabled, this.qualityPreset, this.debugOverlay, this.showIrisNotice);
    }

    public LucernaConfig withQualityPreset(QualityPreset preset) {
        return new LucernaConfig(CURRENT_SCHEMA_VERSION, this.rendererEnabled, preset, this.debugOverlay, this.showIrisNotice);
    }

    public LucernaConfig withDebugOverlay(DebugOverlay overlay) {
        return new LucernaConfig(CURRENT_SCHEMA_VERSION, this.rendererEnabled, this.qualityPreset, overlay, this.showIrisNotice);
    }

    public LucernaConfig withShowIrisNotice(boolean show) {
        return new LucernaConfig(CURRENT_SCHEMA_VERSION, this.rendererEnabled, this.qualityPreset, this.debugOverlay, show);
    }
}
