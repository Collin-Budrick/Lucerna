package net.lucerna.render.preview;

import net.lucerna.config.CompositeMode;
import net.lucerna.config.Round7VisualMode;

public record FinalCompositeModeStatus(
        CompositeMode mode,
        Round7VisualMode visualMode,
        String visualModeId,
        String statusKey,
        String displayName,
        String evidenceKey,
        boolean baseWorldColorEnabled,
        boolean directLightingEnabled,
        boolean diffuseGiEnabled,
        boolean finalLucernaComposite,
        boolean baselineOnly,
        boolean isolatedLucernaSignal,
        String dispatchLabel,
        String statusText,
        String modeReason,
        String expectedEvidence
) {
    public FinalCompositeModeStatus {
        if (mode == null) {
            mode = CompositeMode.FINAL_LUCERNA_COMPOSITE;
        }
        if (visualMode == null) {
            visualMode = mode.visualMode();
        }
        if (visualModeId == null || visualModeId.isBlank()) {
            visualModeId = visualMode.stableId();
        } else {
            visualModeId = visualModeId.trim();
        }
        if (statusKey == null || statusKey.isBlank()) {
            statusKey = mode.statusKey();
        } else {
            statusKey = statusKey.trim();
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = mode.displayName();
        } else {
            displayName = displayName.trim();
        }
        if (evidenceKey == null || evidenceKey.isBlank()) {
            evidenceKey = mode.evidenceKey();
        } else {
            evidenceKey = evidenceKey.trim();
        }
        baseWorldColorEnabled = baseWorldColorEnabled && mode.baseWorldColorEnabled();
        directLightingEnabled = directLightingEnabled && mode.directLightingEnabled();
        diffuseGiEnabled = diffuseGiEnabled && mode.diffuseGiEnabled();
        finalLucernaComposite = finalLucernaComposite
                && baseWorldColorEnabled
                && directLightingEnabled
                && diffuseGiEnabled;
        baselineOnly = baselineOnly && baseWorldColorEnabled && !directLightingEnabled && !diffuseGiEnabled;
        isolatedLucernaSignal = isolatedLucernaSignal && !baseWorldColorEnabled && directLightingEnabled != diffuseGiEnabled;
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
        if (modeReason == null || modeReason.isBlank()) {
            modeReason = mode.modeReason();
        } else {
            modeReason = modeReason.trim();
        }
        if (expectedEvidence == null || expectedEvidence.isBlank()) {
            expectedEvidence = mode.expectedEvidence();
        } else {
            expectedEvidence = expectedEvidence.trim();
        }
    }

    public static FinalCompositeModeStatus fromConfigMode(CompositeMode mode) {
        CompositeMode resolvedMode = mode == null ? CompositeMode.FINAL_LUCERNA_COMPOSITE : mode;
        return new FinalCompositeModeStatus(
                resolvedMode,
                resolvedMode.visualMode(),
                resolvedMode.visualModeId(),
                resolvedMode.statusKey(),
                resolvedMode.displayName(),
                resolvedMode.evidenceKey(),
                resolvedMode.baseWorldColorEnabled(),
                resolvedMode.directLightingEnabled(),
                resolvedMode.diffuseGiEnabled(),
                resolvedMode == CompositeMode.FINAL_LUCERNA_COMPOSITE,
                resolvedMode.baselineOnly(),
                resolvedMode.isolatedLucernaSignal(),
                defaultDispatchLabel(resolvedMode),
                resolvedMode.statusDescription(),
                resolvedMode.modeReason(),
                resolvedMode.expectedEvidence()
        );
    }

    public boolean lucernaLightingEnabled() {
        return this.directLightingEnabled || this.diffuseGiEnabled;
    }

    public String summary() {
        return "mode=" + this.statusKey
                + ",evidenceKey=" + this.evidenceKey
                + ",visualMode=" + this.visualModeId
                + ",round7.finalCompositeMode=" + this.visualModeId
                + ",selectedSourcePolicy=" + this.selectedSourcePolicy()
                + ",baseWorldColor=" + this.baseWorldColorEnabled
                + ",directLighting=" + this.directLightingEnabled
                + ",diffuseGi=" + this.diffuseGiEnabled
                + ",finalLucernaComposite=" + this.finalLucernaComposite
                + ",dispatch=" + this.dispatchLabel;
    }

    public String round7FinalCompositeModeMarker() {
        return "round7.finalCompositeMode=" + this.visualModeId;
    }

    public String sourceMixSummary(
            boolean directSourceReady,
            boolean giSourceReady,
            boolean denoisedSourceReady
    ) {
        return "base=" + this.baseWorldColorEnabled
                + ",direct=" + sourceState(this.directLightingEnabled, directSourceReady)
                + ",gi=" + sourceState(this.diffuseGiEnabled || this.rawGiVisualMode(), giSourceReady)
                + ",denoised=" + sourceState(
                (this.denoisedGiVisualMode() || this.finalCompositeVisualMode()) && this.diffuseGiEnabled,
                denoisedSourceReady
        )
                + ",selected=" + this.selectedSourceReadinessSummary(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady
        );
    }

    public boolean rejectsFocusWindowOnlyComposite() {
        return this.rawGiVisualMode() || this.denoisedGiVisualMode() || this.finalCompositeVisualMode();
    }

    public boolean rejectsDirectLightSubstitution() {
        return this.rawGiVisualMode() || this.denoisedGiVisualMode() || this.finalCompositeVisualMode();
    }

    public String substitutionBoundarySummary(
            boolean submittedFocusWindowOnly,
            boolean submittedDirectLightSource
    ) {
        boolean focusWindowRejected = this.rejectsFocusWindowOnlyComposite() && !submittedFocusWindowOnly;
        boolean directSubstitutionRejected = this.rejectsDirectLightSubstitution() && !submittedDirectLightSource;
        return "round7.finalCompositeRejectFocusWindowOnly=" + focusWindowRejected
                + ",round7.finalCompositeRejectDirectLightSubstitution=" + directSubstitutionRejected
                + ",focusWindowOnlySubmitted=" + submittedFocusWindowOnly
                + ",directLightSourceSubmitted=" + submittedDirectLightSource
                + ",round7.focusedRegionProofPolicy=" + this.focusedRegionProofExpectation();
    }

    public String debugLine() {
        return this.displayName + " | " + this.summary();
    }

    public String controllerEvidenceLine() {
        return this.evidenceKey + " | " + this.expectedEvidence;
    }

    public boolean baselineVisualMode() {
        return this.visualMode.baseline();
    }

    public boolean rawGiVisualMode() {
        return this.visualMode.rawGi();
    }

    public boolean denoisedGiVisualMode() {
        return this.visualMode.denoisedGi();
    }

    public boolean finalCompositeVisualMode() {
        return this.visualMode.finalComposite();
    }

    public String signalIsolationLabel() {
        if (this.baselineOnly) {
            return "baseline/base-only";
        }
        if (this.isolatedLucernaSignal) {
            return this.directLightingEnabled ? "isolated/direct-only" : "isolated/gi-only";
        }
        if (this.finalLucernaComposite) {
            return "combined/final";
        }
        return "custom/partial";
    }

    public String selectedSourcePolicy() {
        if (this.baselineVisualMode()) {
            return "baseline-control/no-lucerna-source";
        }
        if (this.rawGiVisualMode()) {
            return "raw-gi/native-diffuse-gi-rgba8";
        }
        if (this.denoisedGiVisualMode()) {
            return "denoised-gi/cpu-denoised-diffuse-gi-rgba8";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light/native-direct-light-rgba8";
        }
        if (this.finalCompositeVisualMode()) {
            return "final-composite/denoised-gi-preferred/raw-gi-fallback";
        }
        return "custom/unspecified";
    }

    public String selectedSourceReadinessSummary(
            boolean directSourceReady,
            boolean giSourceReady,
            boolean denoisedSourceReady
    ) {
        if (this.baselineVisualMode()) {
            return "baseline-control:ready";
        }
        if (this.rawGiVisualMode()) {
            return "raw-gi:" + readyState(giSourceReady);
        }
        if (this.denoisedGiVisualMode()) {
            return "denoised-gi:" + readyState(denoisedSourceReady);
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light:" + readyState(directSourceReady);
        }
        if (this.finalCompositeVisualMode()) {
            if (denoisedSourceReady) {
                return "final-selected=denoised-gi:ready";
            }
            if (giSourceReady) {
                return "final-selected=raw-gi-fallback:ready,denoised-gi=missing";
            }
            return "final-selected=missing,denoised-gi=missing,raw-gi=missing,direct="
                    + readyState(directSourceReady);
        }
        return "custom:direct=" + readyState(directSourceReady)
                + ",raw-gi=" + readyState(giSourceReady)
                + ",denoised-gi=" + readyState(denoisedSourceReady);
    }

    public String focusedRegionProofExpectation() {
        if (this.baselineVisualMode()) {
            return "control-frame; visual proof should not pass as Lucerna lighting";
        }
        if (this.rawGiVisualMode()) {
            return "requires submitted full-target raw-GI draw plus controller focused-surface screenshot delta";
        }
        if (this.denoisedGiVisualMode()) {
            return "requires submitted full-target denoised-GI draw plus controller focused-surface screenshot delta";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct diagnostic mode; Round 7 raw/denoised/final visual proof should not pass from this source";
        }
        if (this.finalCompositeVisualMode()) {
            return "requires final mode source identity, submitted full-target draw, HUD-safe target, and controller focused-surface screenshot delta";
        }
        return "requires mode-specific controller screenshot delta; status alone is not visual proof";
    }

    public String visualProofBoundarySummary() {
        return "selectedSourcePolicy=" + this.selectedSourcePolicy()
                + ",focusedRegionProof=\"" + this.focusedRegionProofExpectation() + "\""
                + ",rejectsFocusWindowOnly=" + this.rejectsFocusWindowOnlyComposite()
                + ",rejectsDirectLightSubstitution=" + this.rejectsDirectLightSubstitution();
    }

    public String validationSummary() {
        return "displayName=\"" + this.displayName
                + "\",evidenceKey=\"" + this.evidenceKey
                + "\",visualMode=\"" + this.visualModeId
                + "\",isolation=\"" + this.signalIsolationLabel()
                + "\",selectedSourcePolicy=\"" + this.selectedSourcePolicy()
                + "\",focusedRegionProof=\"" + this.focusedRegionProofExpectation()
                + "\",reason=\"" + this.modeReason
                + "\",expectedEvidence=\"" + this.expectedEvidence + "\"";
    }

    public String foundationBoundary() {
        return "Round 7 composite mode/status foundation only; denoise and final visual quality remain controller-validated work";
    }

    private static String defaultDispatchLabel(CompositeMode mode) {
        return switch (mode) {
            case BASE_VANILLA_ONLY -> "round7-visual-mode/baseline-no-lucerna-composite";
            case DIRECT_ONLY -> "composite-mode/direct-only";
            case GI_ONLY, RAW_GI -> "round7-visual-mode/raw-gi";
            case DENOISED_GI -> "round7-visual-mode/denoised-gi";
            case FINAL_LUCERNA_COMPOSITE -> "round7-visual-mode/final-composite";
        };
    }

    private static String sourceState(boolean enabledByMode, boolean sourceReady) {
        if (!enabledByMode) {
            return "excluded";
        }
        return sourceReady ? "enabled-ready" : "enabled-missing";
    }

    private static String readyState(boolean sourceReady) {
        return sourceReady ? "ready" : "missing";
    }
}
