package net.lucerna.render.preview;

import net.lucerna.config.CompositeMode;

public record FinalCompositeModeStatus(
        CompositeMode mode,
        String statusKey,
        boolean baseWorldColorEnabled,
        boolean directLightingEnabled,
        boolean diffuseGiEnabled,
        boolean finalLucernaComposite,
        String dispatchLabel,
        String statusText
) {
    public FinalCompositeModeStatus {
        if (mode == null) {
            mode = CompositeMode.FINAL_LUCERNA_COMPOSITE;
        }
        if (statusKey == null || statusKey.isBlank()) {
            statusKey = mode.statusKey();
        } else {
            statusKey = statusKey.trim();
        }
        baseWorldColorEnabled = baseWorldColorEnabled && mode.baseWorldColorEnabled();
        directLightingEnabled = directLightingEnabled && mode.directLightingEnabled();
        diffuseGiEnabled = diffuseGiEnabled && mode.diffuseGiEnabled();
        finalLucernaComposite = finalLucernaComposite
                && baseWorldColorEnabled
                && directLightingEnabled
                && diffuseGiEnabled;
        if (dispatchLabel == null || dispatchLabel.isBlank()) {
            dispatchLabel = defaultDispatchLabel(mode);
        } else {
            dispatchLabel = dispatchLabel.trim();
        }
        if (statusText == null || statusText.isBlank()) {
            statusText = mode.statusDescription();
        } else {
            statusText = statusText.trim();
        }
    }

    public static FinalCompositeModeStatus fromConfigMode(CompositeMode mode) {
        CompositeMode resolvedMode = mode == null ? CompositeMode.FINAL_LUCERNA_COMPOSITE : mode;
        return new FinalCompositeModeStatus(
                resolvedMode,
                resolvedMode.statusKey(),
                resolvedMode.baseWorldColorEnabled(),
                resolvedMode.directLightingEnabled(),
                resolvedMode.diffuseGiEnabled(),
                resolvedMode == CompositeMode.FINAL_LUCERNA_COMPOSITE,
                defaultDispatchLabel(resolvedMode),
                resolvedMode.statusDescription()
        );
    }

    public boolean lucernaLightingEnabled() {
        return this.directLightingEnabled || this.diffuseGiEnabled;
    }

    public String summary() {
        return "mode=" + this.statusKey
                + ",baseWorldColor=" + this.baseWorldColorEnabled
                + ",directLighting=" + this.directLightingEnabled
                + ",diffuseGi=" + this.diffuseGiEnabled
                + ",finalLucernaComposite=" + this.finalLucernaComposite
                + ",dispatch=" + this.dispatchLabel;
    }

    public String debugLine() {
        return this.mode.displayName() + " | " + this.summary();
    }

    public String foundationBoundary() {
        return "Round 7 composite mode/status foundation only; denoise and final visual quality remain controller-validated work";
    }

    private static String defaultDispatchLabel(CompositeMode mode) {
        return switch (mode) {
            case BASE_VANILLA_ONLY -> "composite-mode/base-vanilla-only-no-lucerna-lighting";
            case DIRECT_ONLY -> "composite-mode/direct-only";
            case GI_ONLY -> "composite-mode/gi-only";
            case FINAL_LUCERNA_COMPOSITE -> "composite-mode/final-lucerna-composite";
        };
    }
}
