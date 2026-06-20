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
        )
                + ",qualityGate=" + this.selectedCompositeQualityGate(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady
        );
    }

    public String compactSourceMixPolicy() {
        return "direct=" + sourcePolicyState(this.directLightingEnabled, "native-direct")
                + ",rawGI=" + sourcePolicyState(
                this.diffuseGiEnabled || this.rawGiVisualMode(),
                "native-scene-tied-cpu/readback"
        )
                + ",denoisedGI=" + denoisedPolicyState()
                + ",final=" + (this.finalLucernaComposite
                ? "direct+rawGI+cpuDenoisedGI/composite-proof"
                : "isolated")
                + "," + this.shaderOutputStatusSummary();
    }

    public String denoiseSourcePolicy() {
        if (this.baselineVisualMode()) {
            return "excluded:baseline";
        }
        if (this.rawGiVisualMode()) {
            return "excluded:raw-GI-control";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "excluded:direct-only-control";
        }
        if (this.denoisedGiVisualMode() || this.finalLucernaComposite) {
            return "current=CPU/readback denoised RGBA8; real shader denoise must report realShader=true";
        }
        return "mode-specific; require explicit shader-vs-CPU source label";
    }

    public String shaderOutputStatusSummary() {
        return "nativeSceneTiedGiOutput=CPU/readback,physicalGiQuality=open,realShaderGiOutput=false"
                + ",cpuGiScaffoldOutput=" + (!this.baselineVisualMode() && (this.diffuseGiEnabled || this.rawGiVisualMode()))
                + ",realDenoiseShaderOutput=false"
                + ",cpuDenoiseScaffoldOutput=" + (this.denoisedGiVisualMode() || this.finalCompositeVisualMode())
                + ",shaderQualityGate=open";
    }

    public String lightingStackBoundary() {
        return "nativeSceneTiedGI=CPU/readback signal,shaderGI=false,cpuDenoise="
                + (this.denoisedGiVisualMode() || this.finalCompositeVisualMode())
                + ",shaderDenoise=false,finalComposite="
                + (this.finalCompositeVisualMode() ? "preview/proof mix,quality-open" : "not-final");
    }

    public String finalCompositeBoundary() {
        if (this.baselineVisualMode()) {
            return "baseline control; no Lucerna lighting composite";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-only diagnostic; does not prove GI, denoise, or final quality";
        }
        if (this.rawGiVisualMode()) {
            return "raw native GI view; proves source/display path only, not denoise or final quality";
        }
        if (this.denoisedGiVisualMode()) {
            return "CPU/readback denoised GI view; shader denoise quality remains open";
        }
        if (this.finalCompositeVisualMode()) {
            return "final composite preview mixes direct, raw native GI, and CPU/readback denoise through scene-shaped full-target projection; physical GI, geometry/material-aware quality, and shader denoise remain open";
        }
        return "custom mode; require explicit source and proof boundary";
    }

    public String compactAuthenticityPolicy() {
        if (this.baselineVisualMode()) {
            return "baseline control; no Lucerna source";
        }
        if (this.finalLucernaComposite) {
            return "reject marker/focus/metadata/substitution; direct/raw/denoised must be distinct ready sources";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct source must be accepted, candidate-backed, written, and resolved";
        }
        return "reject marker/focus/metadata/direct-substitution";
    }

    public boolean rejectsFocusWindowOnlyComposite() {
        return this.rawGiVisualMode() || this.denoisedGiVisualMode() || this.finalCompositeVisualMode();
    }

    public boolean rejectsDirectLightSubstitution() {
        return this.rawGiVisualMode() || this.denoisedGiVisualMode();
    }

    public String substitutionBoundarySummary(
            boolean submittedFocusWindowOnly,
            boolean submittedDirectLightSource
    ) {
        boolean focusWindowEvidenceClean = !(this.rejectsFocusWindowOnlyComposite() && submittedFocusWindowOnly);
        boolean directSubstitutionEvidenceClean = !(this.rejectsDirectLightSubstitution() && submittedDirectLightSource);
        return "round7.finalCompositeFocusWindowEvidenceClean=" + focusWindowEvidenceClean
                + ",round7.finalCompositeDirectSubstitutionEvidenceClean=" + directSubstitutionEvidenceClean
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
            return "raw-gi/native-scene-tied-diffuse-gi-rgba8-cpu/readback";
        }
        if (this.denoisedGiVisualMode()) {
            return "denoised-gi/cpu/readback-denoised-diffuse-gi-rgba8";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light/native-direct-light-rgba8";
        }
        if (this.finalCompositeVisualMode()) {
            return "final-composite/direct-emissive-plus-raw-native-gi-plus-cpu-denoised-gi/source-separated-preview";
        }
        return "custom/unspecified";
    }

    public String finalCompositeSubmissionPolicy() {
        if (this.baselineVisualMode()) {
            return "not-attempted:baseline-control";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "diagnostic-direct-light-only; not final emissive surface proof";
        }
        if (this.finalCompositeVisualMode()) {
            return "submit full-target scene-shaped final composite preview; blend native direct emissive, scene-tied native raw GI, and CPU/readback-denoised GI when ready; raw-only fallback is degraded, rectangular washout is rejected by controller proof, and shader denoise remains open";
        }
        if (this.rawGiVisualMode() || this.denoisedGiVisualMode()) {
            return "submit isolated full-target " + this.selectedSourcePolicy();
        }
        return "mode-specific submission policy; controller log carries submitted/skipped result";
    }

    public String selectedSourceAuthenticityPolicy() {
        if (this.baselineVisualMode()) {
            return "baseline has no Lucerna payload";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light payload is real only when accepted, non-metadata, has work, and writes/resolves output";
        }
        if (this.rawGiVisualMode() || this.denoisedGiVisualMode()) {
            return "reject metadata-only, proof-marker, focus-window-only, and direct-light substitution sources";
        }
        if (this.finalCompositeVisualMode()) {
            return "reject metadata-only, proof-marker, focus-window-only, and direct-light-substitution sources; accept direct, raw GI, and denoised GI only as distinct ready payloads";
        }
        return "require mode-specific source identity before screenshot proof";
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
            if (directSourceReady && giSourceReady && denoisedSourceReady) {
                return "final-selected=direct+raw-gi+denoised-gi:ready-for-controller-surface-quality-proof";
            }
            if (denoisedSourceReady && giSourceReady) {
                return "final-selected=raw-gi+denoised-gi:ready,direct-blend=" + readyState(directSourceReady);
            }
            if (denoisedSourceReady) {
                return "final-selected=denoised-gi-only:degraded,raw-gi=missing,direct-blend="
                        + readyState(directSourceReady);
            }
            if (giSourceReady) {
                return "final-selected=raw-gi-fallback:degraded,denoised-gi=missing,direct-blend="
                        + readyState(directSourceReady);
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
            return "requires distinct direct/raw-GI/denoised-GI source identities, submitted full-target scene-shaped draw, HUD-safe target, focused-surface delta, anti-rectangular-washout proof, and raw-vs-denoised quality comparison";
        }
        return "requires mode-specific controller screenshot delta; status alone is not visual proof";
    }

    public String firstLightingMilestoneGate() {
        if (this.baselineVisualMode()) {
            return "control only; first-lighting proof must fail here";
        }
        if (this.rawGiVisualMode()) {
            return "open until raw GI has full-target draw, focused delta, and debug/source identity";
        }
        if (this.denoisedGiVisualMode()) {
            return "open until denoised beats raw visually and labels shader vs CPU fallback";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct proof is validated; full milestone still waits on GI/denoise/final quality";
        }
        if (this.finalLucernaComposite) {
            return "open until direct+GI+denoised mix is stable, HUD-safe, source-separated, geometry/material-aware, and quality-proven";
        }
        return "open until controller screenshot/log quality proof passes";
    }

    public String selectedCompositeQualityGate(
            boolean directSourceReady,
            boolean giSourceReady,
            boolean denoisedSourceReady
    ) {
        if (this.baselineVisualMode()) {
            return "not-applicable:baseline-control";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return directSourceReady
                    ? "partial:direct-only-diagnostic;missing-raw-gi-denoised-gi-final-quality"
                    : "blocked:direct-source-missing";
        }
        if (this.rawGiVisualMode()) {
            return giSourceReady
                    ? "partial:raw-gi-source-ready;requires-scene-shaped-focused-delta"
                    : "blocked:raw-gi-source-missing";
        }
        if (this.denoisedGiVisualMode()) {
            return denoisedSourceReady
                    ? "partial:cpu-denoised-source-ready;requires-raw-vs-denoised-quality-proof"
                    : "blocked:denoised-gi-source-missing";
        }
        if (this.finalCompositeVisualMode()) {
            if (directSourceReady && giSourceReady && denoisedSourceReady) {
                return "candidate:direct+raw-gi+cpu-denoised-gi-ready;requires-controller-geometry-material-aware-quality-proof";
            }
            return "blocked:final-source-missing direct=" + readyState(directSourceReady)
                    + ",raw-gi=" + readyState(giSourceReady)
                    + ",denoised-gi=" + readyState(denoisedSourceReady);
        }
        return "custom:requires-explicit-quality-gate";
    }

    public String geometryMaterialProjectionBoundary() {
        if (this.finalCompositeVisualMode()) {
            return "current=scene-shaped public Mojang full-target projection from CPU/readback payloads; pending=real geometry/material-aware shader/native projection plus physical GI quality";
        }
        if (this.rawGiVisualMode() || this.denoisedGiVisualMode()) {
            return "current=isolated source visualization; pending=final geometry/material-aware composite quality";
        }
        return "mode-specific; controller proof must state whether geometry/material-aware projection is actually implemented";
    }

    public String visualProofBoundarySummary() {
        return "selectedSourcePolicy=" + this.selectedSourcePolicy()
                + ",submissionPolicy=\"" + this.finalCompositeSubmissionPolicy() + "\""
                + ",sourceAuthenticityPolicy=\"" + this.selectedSourceAuthenticityPolicy() + "\""
                + ",focusedRegionProof=\"" + this.focusedRegionProofExpectation() + "\""
                + ",geometryMaterialProjectionBoundary=\"" + this.geometryMaterialProjectionBoundary() + "\""
                + ",rejectsFocusWindowOnly=" + this.rejectsFocusWindowOnlyComposite()
                + ",rejectsDirectLightSubstitution=" + this.rejectsDirectLightSubstitution();
    }

    public String validationSummary() {
        return "displayName=\"" + this.displayName
                + "\",evidenceKey=\"" + this.evidenceKey
                + "\",visualMode=\"" + this.visualModeId
                + "\",isolation=\"" + this.signalIsolationLabel()
                + "\",selectedSourcePolicy=\"" + this.selectedSourcePolicy()
                + "\",submissionPolicy=\"" + this.finalCompositeSubmissionPolicy()
                + "\",sourceAuthenticityPolicy=\"" + this.selectedSourceAuthenticityPolicy()
                + "\",shaderOutputStatus=\"" + this.shaderOutputStatusSummary()
                + "\",geometryMaterialProjectionBoundary=\"" + this.geometryMaterialProjectionBoundary()
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

    private static String sourcePolicyState(boolean enabledByMode, String sourceName) {
        return enabledByMode ? sourceName : "excluded";
    }

    private String denoisedPolicyState() {
        if (this.denoisedGiVisualMode() || this.finalLucernaComposite) {
            return "cpu-fallback-now/real-shader-required";
        }
        return "excluded";
    }

    private static String readyState(boolean sourceReady) {
        return sourceReady ? "ready" : "missing";
    }
}
