package net.lucerna.render.frame;

import net.lucerna.config.DebugOverlay;
import net.lucerna.config.QualityPreset;

public record FrameRenderFlags(
        QualityPreset qualityPreset,
        DebugOverlay debugOverlay,
        boolean rendererEnabled,
        boolean rendererActive,
        boolean gBufferEnabled,
        boolean directLightingEnabled,
        boolean diffuseGiEnabled,
        boolean denoiseEnabled,
        boolean debugViewsEnabled,
        boolean available
) {
    public FrameRenderFlags(
            QualityPreset qualityPreset,
            DebugOverlay debugOverlay,
            boolean rendererEnabled,
            boolean rendererActive,
            boolean gBufferEnabled,
            boolean directLightingEnabled,
            boolean diffuseGiEnabled,
            boolean denoiseEnabled,
            boolean debugViewsEnabled
    ) {
        this(
                qualityPreset,
                debugOverlay,
                rendererEnabled,
                rendererActive,
                gBufferEnabled,
                directLightingEnabled,
                diffuseGiEnabled,
                denoiseEnabled,
                debugViewsEnabled,
                true
        );
    }

    public FrameRenderFlags {
        if (qualityPreset == null) {
            qualityPreset = QualityPreset.BALANCED;
        }
        if (debugOverlay == null) {
            debugOverlay = DebugOverlay.OFF;
        }
        debugViewsEnabled = debugViewsEnabled || debugOverlay != DebugOverlay.OFF;
    }

    public static FrameRenderFlags unavailable() {
        return new FrameRenderFlags(
                QualityPreset.BALANCED,
                DebugOverlay.OFF,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
    }

    public boolean anyLightingEnabled() {
        return this.directLightingEnabled || this.diffuseGiEnabled;
    }
}
