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
                + ",sourceIdentityBoundary=" + this.selectedSourceIdentityBoundary()
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
                + ",identityMatrix=" + this.selectedSourceIdentityMatrix(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady,
                false
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

    public String shaderDenoiseIntentReadinessSummary(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        return this.shaderDenoiseIntentReadinessSummary(
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none()
        );
    }

    public String shaderDenoiseIntentReadinessSummary(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate shaderOutputImageCandidate
    ) {
        return this.shaderDenoiseIntentReadinessSummary(
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                shaderOutputImageCandidate,
                false,
                "real shader-generated denoise output has not been proven"
        );
    }

    public String shaderDenoiseIntentReadinessSummary(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate shaderOutputImageCandidate,
            boolean realShaderDenoiseOutputReady,
            String shaderOutputBlocker
    ) {
        boolean denoiseVisualIntent = this.denoisedGiVisualMode() || this.finalCompositeVisualMode();
        PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate resolvedCandidate =
                shaderOutputImageCandidate == null
                        ? PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none()
                        : shaderOutputImageCandidate;
        boolean provenRealShaderOutput = denoiseVisualIntent
                && shaderDenoisedSourceReady
                && realShaderDenoiseOutputReady
                && !resolvedCandidate.candidatePresent();
        String blocker = shaderOutputBlocker == null || shaderOutputBlocker.isBlank()
                ? (resolvedCandidate.candidatePresent()
                ? resolvedCandidate.blocker()
                : "real shader-generated denoise output has not been proven")
                : shaderOutputBlocker.trim();
        return "round7.cpuDenoisedOutputReady=" + cpuDenoisedSourceReady
                + ",round7.denoisedGiSourceIdentity="
                + denoisedSourceIdentity(cpuDenoisedSourceReady, provenRealShaderOutput).stableLabel()
                + ",round7.shaderOutputImageCandidate=" + resolvedCandidate.boundarySummary()
                + ",round7.shaderDenoiseVisualShaderIntent=" + denoiseVisualIntent
                + ",round7.shaderDenoiseVisualShaderSource="
                + (denoiseVisualIntent ? "public-mojang-source-gated-denoised-gi-visual" : "excluded-by-mode")
                + ",round7.shaderDenoiseVisualSourceReady=" + shaderDenoisedSourceReady
                + ",round7.shaderDenoiseSourceClassification="
                + shaderDenoiseSourceClassification(
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                resolvedCandidate.candidatePresent(),
                provenRealShaderOutput
        )
                + ",round7.realShaderDenoiseDispatchReady=" + provenRealShaderOutput
                + ",round7.realShaderDenoiseOutputReady=" + provenRealShaderOutput
                + ",round7.realShaderDenoiseClaimAllowed="
                + provenRealShaderOutput
                + ",round7.shaderOutputImageCandidateOnly="
                + (resolvedCandidate.candidatePresent() && !provenRealShaderOutput)
                + ",round7.shaderOutputBlocker=\"" + blocker + "\""
                + ",round7.shaderDenoiseOverclaimPresent="
                + (denoiseVisualIntent && shaderDenoisedSourceReady && !provenRealShaderOutput)
                + ",round7.shaderDenoiseBoundary=\""
                + (provenRealShaderOutput
                ? "real shader-denoised output was reported by explicit true shader-output markers"
                : resolvedCandidate.candidatePresent()
                ? "shader output image candidate is reported with blocker metadata, but real shader-side denoise output is not proven"
                : "visual shader may draw CPU/readback denoised payload cues, but real shader-side denoise output is not proven")
                + "\"";
    }

    public String compactSourceMixPolicy() {
        return "direct=" + sourcePolicyState(this.directLightingEnabled, "native-direct")
                + ",rawGI=" + sourcePolicyState(
                this.diffuseGiEnabled || this.rawGiVisualMode(),
                "native-scene-tied-cpu/readback"
        )
                + ",denoisedGI=" + denoisedPolicyState()
                + ",final=" + (this.finalLucernaComposite
                ? "direct+rawGI+cpuDenoisedGI/source-gated-surface-preview"
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
            return "current=CPU/readback denoised RGBA8 plus optional visual-shader shaping; shader output candidate is telemetry-only; real shader denoise must report explicit realShaderDenoiseOutputReady=true with shader-generated output";
        }
        return "mode-specific; require explicit shader-vs-CPU source label";
    }

    public String shaderOutputStatusSummary() {
        return this.shaderOutputStatusSummary(
                PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none()
        );
    }

    public String shaderOutputStatusSummary(
            PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate shaderOutputImageCandidate
    ) {
        PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate resolvedCandidate =
                shaderOutputImageCandidate == null
                        ? PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none()
                        : shaderOutputImageCandidate;
        return "nativeSceneTiedGiOutput=CPU/readback,physicalGiSceneLinkedMetrics=telemetry-only,physicalGiTracingQuality=open,realShaderGiOutput=false"
                + ",cpuGiScaffoldOutput=" + (!this.baselineVisualMode() && (this.diffuseGiEnabled || this.rawGiVisualMode()))
                + ",realDenoiseShaderOutput=false"
                + ",cpuDenoiseScaffoldOutput=" + (this.denoisedGiVisualMode() || this.finalCompositeVisualMode())
                + ",shaderDenoiseVisualShaderIntent=" + (this.denoisedGiVisualMode() || this.finalCompositeVisualMode())
                + ",shaderOutputImageCandidate=" + resolvedCandidate.boundarySummary()
                + ",realShaderDenoiseDispatchReady=false"
                + ",realShaderDenoiseOutputReady=false"
                + ",shaderQualityGate=open";
    }

    public String lightingStackBoundary() {
        return "nativeSceneTiedGI=CPU/readback signal,physicalGiSceneLinkedMetrics=telemetry-only,shaderGI=false,cpuDenoise="
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
            return "final composite preview mixes direct, raw native GI, and CPU/readback denoise through source-gated scene/surface projection; focus-window/proof-marker paths are rejected, scene-linked physical GI metrics remain evidence-only, and physical GI tracing quality, geometry/material-aware quality, and shader denoise remain open";
        }
        return "custom mode; require explicit source and proof boundary";
    }

    public String compactAuthenticityPolicy() {
        if (this.baselineVisualMode()) {
            return "baseline control; no Lucerna source";
        }
        if (this.finalLucernaComposite) {
            return "reject proof-marker/focus-window/metadata/substitution; direct/raw/denoised must be distinct ready sources";
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
            return "denoised-gi/cpu-readback-denoised-diffuse-gi-rgba8;visual-shader-shaping=allowed;shader-output-candidate=telemetry-only;true-shader-output=not-current";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light/native-direct-light-rgba8";
        }
        if (this.finalCompositeVisualMode()) {
            return "final-composite/direct-emissive-plus-raw-native-gi-plus-cpu-denoised-gi/source-gated-surface-preview;visual-shader-denoise=may-shape-cpu-readback;shader-output-candidate=telemetry-only;true-shader-generated-denoise=not-current";
        }
        return "custom/unspecified";
    }

    public String selectedSourceIdentityBoundary() {
        if (this.baselineVisualMode()) {
            return "baseline=no-lucerna-source";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct=native-direct-light-rgba8;rawGI=excluded;cpuDenoisedGI=excluded;shaderDenoisedGI=excluded;shaderOutputImageCandidate=excluded";
        }
        if (this.rawGiVisualMode()) {
            return "direct=excluded;rawGI=native-diffuse-gi-rgba8;cpuDenoisedGI=excluded;shaderDenoisedGI=excluded;shaderOutputImageCandidate=excluded";
        }
        if (this.denoisedGiVisualMode()) {
            return "direct=excluded;rawGI=excluded;cpuDenoisedGI=cpu-denoised-diffuse-gi-rgba8;shaderVisualDenoise=visual-shader-over-cpu-readback;shaderOutputImageCandidate=telemetry-only-if-present;shaderGeneratedDenoisedGI=requires-realShaderDenoiseOutputReady";
        }
        if (this.finalCompositeVisualMode()) {
            return "direct=native-direct-light-rgba8;rawGI=native-diffuse-gi-rgba8;cpuDenoisedGI=cpu-denoised-diffuse-gi-rgba8;shaderVisualDenoise=visual-shader-over-cpu-readback;shaderOutputImageCandidate=telemetry-only-if-present;shaderGeneratedDenoisedGI=requires-realShaderDenoiseOutputReady";
        }
        return "custom=requires-explicit-direct/raw/cpu-denoised/shader-output-candidate/shader-denoised-source-identity";
    }

    public String finalCompositeSubmissionPolicy() {
        if (this.baselineVisualMode()) {
            return "not-attempted:baseline-control";
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "diagnostic-direct-light-only; not final emissive surface proof";
        }
        if (this.finalCompositeVisualMode()) {
            return "submit full-target source-gated scene/surface final composite preview; blend native direct emissive, scene-tied native raw GI, and CPU/readback-denoised GI when ready; visual shader denoise may shape CPU/readback payloads, shader output image candidates are blocker telemetry only, true shader-generated denoise requires explicit realShaderDenoiseOutputReady=true; focus-window/proof-marker evidence is rejected, raw-only fallback is degraded, rectangular washout is rejected by controller proof";
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
        return this.selectedSourceReadinessSummary(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady,
                false
        );
    }

    public String selectedSourceReadinessSummary(
            boolean directSourceReady,
            boolean giSourceReady,
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        boolean denoisedSourceReady = cpuDenoisedSourceReady || shaderDenoisedSourceReady;
        PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity denoisedIdentity =
                denoisedSourceIdentity(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
        if (this.baselineVisualMode()) {
            return "baseline-control:ready";
        }
        if (this.rawGiVisualMode()) {
            return "raw-gi:" + readyState(giSourceReady);
        }
        if (this.denoisedGiVisualMode()) {
            return "denoised-gi:" + readyState(denoisedSourceReady)
                    + ",sourceIdentity=" + denoisedIdentity.stableLabel()
                    + ",sourceBoundary=" + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
        }
        if (this.directLightingEnabled && !this.diffuseGiEnabled) {
            return "direct-light:" + readyState(directSourceReady);
        }
        if (this.finalCompositeVisualMode()) {
            if (directSourceReady && giSourceReady && denoisedSourceReady) {
                return "final-selected=direct+raw-gi+denoised-gi:ready-for-controller-surface-quality-proof"
                        + ",denoisedSourceIdentity=" + denoisedIdentity.stableLabel()
                        + ",denoisedSourceBoundary="
                        + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
            }
            if (denoisedSourceReady && giSourceReady) {
                return "final-selected=raw-gi+denoised-gi:ready,direct-blend=" + readyState(directSourceReady)
                        + ",denoisedSourceIdentity=" + denoisedIdentity.stableLabel()
                        + ",denoisedSourceBoundary="
                        + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
            }
            if (denoisedSourceReady) {
                return "final-selected=denoised-gi-only:degraded,raw-gi=missing,direct-blend="
                        + readyState(directSourceReady)
                        + ",denoisedSourceIdentity=" + denoisedIdentity.stableLabel()
                        + ",denoisedSourceBoundary="
                        + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
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
                + ",denoised-gi=" + readyState(denoisedSourceReady)
                + ",denoisedSourceIdentity=" + denoisedIdentity.stableLabel()
                + ",denoisedSourceBoundary="
                + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, shaderDenoisedSourceReady);
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
            return "requires distinct direct/raw-GI/denoised-GI source identities, submitted full-target source-gated scene/surface draw, HUD-safe target, focused-surface delta, anti-rectangular-washout proof, proof-marker/focus-window rejection, and raw-vs-denoised quality comparison";
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
                return "candidate:direct+raw-gi+cpu-denoised-gi-ready;source-gated-surface-preview;requires-controller-geometry-material-aware-quality-proof";
            }
            return "blocked:final-source-missing direct=" + readyState(directSourceReady)
                    + ",raw-gi=" + readyState(giSourceReady)
                    + ",denoised-gi=" + readyState(denoisedSourceReady);
        }
        return "custom:requires-explicit-quality-gate";
    }

    public String selectedSourceIdentityMatrix(
            boolean directSourceReady,
            boolean rawGiSourceReady,
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        return this.selectedSourceIdentityMatrix(
                directSourceReady,
                rawGiSourceReady,
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                false
        );
    }

    public String selectedSourceIdentityMatrix(
            boolean directSourceReady,
            boolean rawGiSourceReady,
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            boolean shaderOutputImageCandidatePresent
    ) {
        return this.selectedSourceIdentityMatrix(
                directSourceReady,
                rawGiSourceReady,
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                shaderOutputImageCandidatePresent,
                false
        );
    }

    public String selectedSourceIdentityMatrix(
            boolean directSourceReady,
            boolean rawGiSourceReady,
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            boolean shaderOutputImageCandidatePresent,
            boolean realShaderDenoiseOutputReady
    ) {
        boolean provenRealShaderOutput = shaderDenoisedSourceReady
                && realShaderDenoiseOutputReady
                && !shaderOutputImageCandidatePresent;
        return "direct=" + sourceState(this.directLightingEnabled, directSourceReady)
                + ",rawGI=" + sourceState(this.diffuseGiEnabled || this.rawGiVisualMode(), rawGiSourceReady)
                + ",cpuDenoisedGI=" + sourceState(
                (this.denoisedGiVisualMode() || this.finalCompositeVisualMode()) && this.diffuseGiEnabled,
                cpuDenoisedSourceReady
        )
                + ",shaderOutputImageCandidate=" + sourceState(
                this.denoisedGiVisualMode() || this.finalCompositeVisualMode(),
                shaderOutputImageCandidatePresent
        )
                + ",shaderVisualDenoise=" + sourceState(
                this.denoisedGiVisualMode() || this.finalCompositeVisualMode(),
                shaderDenoisedSourceReady && !provenRealShaderOutput
        )
                + ",shaderDenoisedGI=" + sourceState(
                (this.denoisedGiVisualMode() || this.finalCompositeVisualMode()) && this.diffuseGiEnabled,
                provenRealShaderOutput
        )
                + ",selectedDenoisedIdentity="
                + denoisedSourceIdentity(cpuDenoisedSourceReady, provenRealShaderOutput).stableLabel()
                + ",selectedDenoisedBoundary="
                + denoisedSourceOutputBoundary(cpuDenoisedSourceReady, provenRealShaderOutput)
                + ",shaderDenoiseSourceClassification="
                + shaderDenoiseSourceClassification(
                cpuDenoisedSourceReady,
                shaderDenoisedSourceReady,
                shaderOutputImageCandidatePresent,
                provenRealShaderOutput
        )
                + ",shaderDenoiseRequiredForQualityMilestone="
                + (this.denoisedGiVisualMode() || this.finalCompositeVisualMode());
    }

    public String finalCompositeReadinessGate(
            boolean directSourceReady,
            boolean rawGiSourceReady,
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            boolean temporalStabilityReady,
            boolean geometryMaterialProjectionReady,
            boolean rejectedEvidenceClean,
            boolean antiWashoutClean
    ) {
        if (!this.finalCompositeVisualMode()) {
            return "not-final-composite:" + this.statusKey;
        }
        if (!rejectedEvidenceClean) {
            return "blocked:rejected-evidence-present";
        }
        if (!antiWashoutClean) {
            return "blocked:rectangular-washout-or-focus-window-risk";
        }
        boolean denoisedSourceReady = cpuDenoisedSourceReady || shaderDenoisedSourceReady;
        if (!(directSourceReady && rawGiSourceReady && denoisedSourceReady)) {
            return "blocked:missing-source "
                    + this.selectedSourceIdentityMatrix(
                    directSourceReady,
                    rawGiSourceReady,
                    cpuDenoisedSourceReady,
                    shaderDenoisedSourceReady
            );
        }
        if (!geometryMaterialProjectionReady) {
            return "candidate:source-separated-source-gated-surface-preview-ready;geometry-material-projection-quality=pending";
        }
        if (!temporalStabilityReady) {
            return "candidate:geometry-material-projection-ready;temporal-stability=pending";
        }
        if (!shaderDenoisedSourceReady) {
            return "partial:first-lighting-preview-ready;real-shader-denoise=pending";
        }
        return "candidate:source-separated-final-composite-with-shader-denoise-visual-source;real-shader-generated-output-still-requires-explicit-proof";
    }

    public String geometryMaterialProjectionBoundary() {
        if (this.finalCompositeVisualMode()) {
            return "current=source-gated scene/surface public Mojang full-target projection from CPU/readback payloads with physical GI metrics surfaced as telemetry-only evidence; rejected=focus-window/proof-marker/metadata-only/rectangular-washout; pending=real geometry/material-aware shader/native projection plus physical GI tracing quality";
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
                + ",sourceIdentityBoundary=\"" + this.selectedSourceIdentityBoundary() + "\""
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
                + "\",sourceIdentityBoundary=\"" + this.selectedSourceIdentityBoundary()
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

    private static PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity denoisedSourceIdentity(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        if (cpuDenoisedSourceReady && shaderDenoisedSourceReady) {
            return PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
        }
        if (shaderDenoisedSourceReady) {
            return PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity.SHADER_GENERATED_DENOISED_GI;
        }
        if (cpuDenoisedSourceReady) {
            return PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity.CPU_DENOISED_READBACK;
        }
        return PublicMojangFinalCompositeSubmissionResult.DenoisedGiSourceIdentity.NONE;
    }

    private static String denoisedSourceOutputBoundary(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        if (shaderDenoisedSourceReady) {
            return cpuDenoisedSourceReady
                    ? "mixed:shader-generated-ready-plus-cpu-readback-present"
                    : "shader-generated-denoise-output-ready";
        }
        if (cpuDenoisedSourceReady) {
            return "cpu-readback-visual-shaping;real-shader-denoise-output=false";
        }
        return "missing;real-shader-denoise-output=false";
    }

    private static String shaderDenoiseSourceClassification(
            boolean cpuDenoisedSourceReady,
            boolean shaderDenoisedSourceReady,
            boolean shaderOutputImageCandidatePresent,
            boolean realShaderDenoiseOutputReady
    ) {
        if (realShaderDenoiseOutputReady) {
            return cpuDenoisedSourceReady
                    ? "mixed-cpu-readback-plus-true-shader-generated-output"
                    : "true-shader-generated-output";
        }
        if (shaderOutputImageCandidatePresent) {
            return "shader-output-image-candidate-only";
        }
        if (shaderDenoisedSourceReady) {
            return "visual-shader-denoise-intent-only";
        }
        if (cpuDenoisedSourceReady) {
            return "cpu-readback-denoised-gi";
        }
        return "denoised-source-missing";
    }
}
