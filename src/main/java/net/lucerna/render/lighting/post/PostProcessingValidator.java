package net.lucerna.render.lighting.post;

import net.lucerna.render.gbuffer.GBufferTargetContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PostProcessingValidator {
    private PostProcessingValidator() {
    }

    public static PostProcessingValidationReport validateDenoise(
            EdgeAwareDenoiseSettings settings,
            HistoryRejectionPlan historyRejection,
            DenoiseInputContract inputs,
            long outputGeneration
    ) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(historyRejection, "historyRejection");
        Objects.requireNonNull(inputs, "inputs");

        List<PostProcessingValidationFinding> findings = new ArrayList<>();
        if (!settings.enabled()) {
            findings.add(PostProcessingValidationFinding.info(
                    "DENOISE_DISABLED",
                    "$.denoise.enabled",
                    "Denoise pass is disabled by settings"
            ));
            return new PostProcessingValidationReport(findings);
        }

        if (inputs.frameIndex() <= 0L) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_FRAME_INDEX",
                    "$.inputs.frameIndex",
                    "Denoise planning requires a positive frame index"
            ));
        }
        if (!inputs.dimensionsAvailable()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_GBUFFER_DIMENSIONS",
                    "$.inputs.gBuffer",
                    "Denoise planning requires positive G-buffer dimensions"
            ));
        }
        if (!inputs.gBuffer().hasDepth()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_DEPTH_INPUT",
                    "$.inputs.gBuffer." + GBufferTargetContract.DEPTH,
                    "Edge-aware denoise requires the current depth attachment"
            ));
        }
        if (!inputs.gBuffer().hasNormals()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_NORMAL_INPUT",
                    "$.inputs.gBuffer." + GBufferTargetContract.NORMAL_ROUGHNESS,
                    "Edge-aware denoise requires the current normal/roughness attachment"
            ));
        }
        if (!inputs.gBuffer().hasMotionVectors()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_MOTION_HISTORY_INPUT",
                    "$.inputs.gBuffer." + GBufferTargetContract.MOTION_HISTORY,
                    "Denoise layout requires the current motion/history attachment"
            ));
        }
        if (!inputs.directLightingAvailable()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_DIRECT_LIGHTING",
                    "$.inputs." + PostProcessingResourceContract.DIRECT_LIGHTING,
                    "Denoise requires direct lighting input"
            ));
        }
        if (!inputs.diffuseGiAvailable()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_DIFFUSE_GI",
                    "$.inputs." + PostProcessingResourceContract.DIFFUSE_GI,
                    "Denoise requires diffuse GI input"
            ));
        }
        if (!inputs.cacheConfidenceAvailable()) {
            findings.add(PostProcessingValidationFinding.warning(
                    "MISSING_CACHE_CONFIDENCE",
                    "$.inputs." + PostProcessingResourceContract.CACHE_CONFIDENCE,
                    "Denoise will run without GI cache confidence weighting"
            ));
        }
        if (settings.edgeAware() && !inputs.hasEdgeAwareInputs()) {
            findings.add(PostProcessingValidationFinding.error(
                    "EDGE_AWARE_INPUTS_INCOMPLETE",
                    "$.denoise.edgeAware",
                    "Edge-aware filtering requires both depth and normal inputs"
            ));
        }
        if (settings.historyAware() && historyRejection.enabled()) {
            if (!historyRejection.historyInputsComplete()) {
                findings.add(PostProcessingValidationFinding.warning(
                        "HISTORY_INPUTS_INCOMPLETE",
                        "$.history",
                        "Temporal history rejection will fall back to current-frame filtering"
                ));
            }
            if (!historyRejection.temporalReuseAllowed()) {
                findings.add(PostProcessingValidationFinding.warning(
                        "TEMPORAL_REUSE_UNAVAILABLE",
                        "$.history.matrixHistory",
                        historyRejection.fallbackReason()
                ));
            }
        }
        if (outputGeneration < inputs.maxInputGeneration()) {
            findings.add(PostProcessingValidationFinding.warning(
                    "OUTPUT_GENERATION_STALE",
                    "$.outputGeneration",
                    "Denoise output generation is older than one or more lighting/history inputs"
            ));
        }

        return new PostProcessingValidationReport(findings);
    }

    public static PostProcessingValidationReport validateComposite(
            DenoisePassPlan denoisePlan,
            FinalCompositeHandoff handoff
    ) {
        Objects.requireNonNull(denoisePlan, "denoisePlan");
        Objects.requireNonNull(handoff, "handoff");

        List<PostProcessingValidationFinding> findings = new ArrayList<>();
        if (handoff.frameIndex() <= 0L) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_COMPOSITE_FRAME_INDEX",
                    "$.composite.frameIndex",
                    "Final composite handoff requires a positive frame index"
            ));
        }
        if (!handoff.dimensionsAvailable()) {
            findings.add(PostProcessingValidationFinding.error(
                    "MISSING_COMPOSITE_DIMENSIONS",
                    "$.composite",
                    "Final composite handoff requires positive target dimensions"
            ));
        }
        if (!handoff.borrowedWorldColorTarget()) {
            findings.add(PostProcessingValidationFinding.error(
                    "COMPOSITE_TARGET_NOT_BORROWED",
                    "$.composite.borrowedWorldColorTarget",
                    "Final composite must target the borrowed Minecraft/Sodium world color target"
            ));
        }
        if (!handoff.beforeHudAndLateTranslucency()) {
            findings.add(PostProcessingValidationFinding.error(
                    "COMPOSITE_ORDER_UNSAFE",
                    "$.composite.beforeHudAndLateTranslucency",
                    "Final composite must run before vanilla HUD and late translucency composition"
            ));
        }
        if (!handoff.writesWorldColorTarget()) {
            findings.add(PostProcessingValidationFinding.error(
                    "COMPOSITE_WORLD_COLOR_NOT_WRITTEN",
                    "$.composite.writeResources",
                    "Final composite handoff must write lucerna.composite.worldColor"
            ));
        }
        if (handoff.clearBeforeWrite()) {
            findings.add(PostProcessingValidationFinding.warning(
                    "COMPOSITE_CLEAR_REQUESTED",
                    "$.composite.clearBeforeWrite",
                    "Composite handoff is expected to preserve the borrowed target instead of clearing it"
            ));
        }
        if (denoisePlan.enabled() && !denoisePlan.validationReport().valid()) {
            findings.add(PostProcessingValidationFinding.error(
                    "DENOISE_PLAN_INVALID",
                    "$.denoise",
                    "Composite handoff cannot consume an invalid enabled denoise plan"
            ));
        }
        if (!handoff.usesDenoisedDiffuse()) {
            findings.add(PostProcessingValidationFinding.warning(
                    "COMPOSITE_DENOISED_DIFFUSE_UNAVAILABLE",
                    "$.composite.usesDenoisedDiffuse",
                    "Composite handoff will need to fall back to direct/GI lighting until denoise output is available"
            ));
        }

        return new PostProcessingValidationReport(findings);
    }
}
