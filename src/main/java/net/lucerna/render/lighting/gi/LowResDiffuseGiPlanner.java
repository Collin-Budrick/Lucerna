package net.lucerna.render.lighting.gi;

import net.lucerna.render.frame.FrameMatrixHistory;
import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.gbuffer.GBufferWriteIntent;

import java.util.ArrayList;
import java.util.List;

public final class LowResDiffuseGiPlanner {
    private LowResDiffuseGiPlanner() {
    }

    public static LowResDiffuseGiPlan plan(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent gBufferWriteIntent,
            FrameMatrixHistory matrixHistory,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings
    ) {
        return plan(
                frameConstants,
                gBufferWriteIntent,
                matrixHistory,
                cacheSnapshot,
                settings,
                AdaptiveGiRayBudgetPolicy.firstMilestone(),
                GiCacheInvalidationPolicy.conservative()
        );
    }

    public static LowResDiffuseGiPlan plan(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent gBufferWriteIntent,
            FrameMatrixHistory matrixHistory,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings,
            AdaptiveGiRayBudgetPolicy rayBudgetPolicy,
            GiCacheInvalidationPolicy invalidationPolicy
    ) {
        return plan(
                frameConstants,
                gBufferWriteIntent,
                matrixHistory,
                cacheSnapshot,
                settings,
                rayBudgetPolicy,
                invalidationPolicy,
                DiffuseGiSourceSummary.unavailable()
        );
    }

    public static LowResDiffuseGiPlan plan(
            LucernaFrameConstants frameConstants,
            GBufferWriteIntent gBufferWriteIntent,
            FrameMatrixHistory matrixHistory,
            GiCacheSnapshot cacheSnapshot,
            DiffuseGiSettings settings,
            AdaptiveGiRayBudgetPolicy rayBudgetPolicy,
            GiCacheInvalidationPolicy invalidationPolicy,
            DiffuseGiSourceSummary sourceSummary
    ) {
        LucernaFrameConstants resolvedConstants = frameConstants == null
                ? LucernaFrameConstants.unavailable()
                : frameConstants;
        DiffuseGiSettings resolvedSettings = settings == null
                ? DiffuseGiSettings.fromQuality(resolvedConstants.flags().qualityPreset(), 2)
                : settings;
        GiCacheSnapshot resolvedCacheSnapshot = cacheSnapshot == null ? GiCacheSnapshot.empty() : cacheSnapshot;
        AdaptiveGiRayBudgetPolicy resolvedRayBudgetPolicy = rayBudgetPolicy == null
                ? AdaptiveGiRayBudgetPolicy.firstMilestone()
                : rayBudgetPolicy;
        GiCacheInvalidationPolicy resolvedInvalidationPolicy = invalidationPolicy == null
                ? GiCacheInvalidationPolicy.conservative()
                : invalidationPolicy;
        DiffuseGiSourceSummary resolvedSourceSummary = sourceSummary == null
                ? DiffuseGiSourceSummary.unavailable()
                : sourceSummary;

        DiffuseGiFrameInput frameInput = DiffuseGiFrameInput.from(
                resolvedConstants,
                gBufferWriteIntent,
                resolvedSettings
        );
        TemporalAccumulationInput temporalInput = TemporalAccumulationInput.from(
                resolvedConstants,
                matrixHistory,
                resolvedSettings,
                resolvedCacheSnapshot
        );
        CacheConfidence cacheConfidence = resolveCacheConfidence(resolvedCacheSnapshot, resolvedInvalidationPolicy);
        GiRayBudgetAllocation rayBudget = resolvedRayBudgetPolicy.allocate(
                frameInput.lowResolutionGrid(),
                cacheConfidence,
                temporalInput,
                resolvedCacheSnapshot,
                resolvedSettings
        );
        DiffuseGiValidationReport validationReport = validate(
                frameInput,
                temporalInput,
                resolvedCacheSnapshot,
                cacheConfidence,
                resolvedSourceSummary,
                rayBudget
        );
        return new LowResDiffuseGiPlan(
                frameInput,
                temporalInput,
                resolvedCacheSnapshot,
                cacheConfidence,
                rayBudget,
                resolvedSourceSummary,
                validationReport
        );
    }

    private static CacheConfidence resolveCacheConfidence(
            GiCacheSnapshot cacheSnapshot,
            GiCacheInvalidationPolicy invalidationPolicy
    ) {
        if (cacheSnapshot.globallyInvalidatedBy(invalidationPolicy)) {
            return CacheConfidence.dirty(
                    Math.max(cacheSnapshot.cacheGeneration(), cacheSnapshot.latestDirtyGeneration()),
                    "GI cache globally invalidated by dirty regions"
            );
        }
        CacheConfidence confidence = cacheSnapshot.combinedConfidence();
        if (!cacheSnapshot.hasDirtyRegions()) {
            return confidence;
        }
        return new CacheConfidence(
                confidence.confidence() * 0.5F,
                Math.max(confidence.variance(), 0.5F),
                confidence.sampleCount(),
                Math.max(confidence.sourceGeneration(), cacheSnapshot.latestDirtyGeneration()),
                confidence.lastTouchedFrame(),
                true,
                "GI cache intersects dirty regions"
        );
    }

    private static DiffuseGiValidationReport validate(
            DiffuseGiFrameInput frameInput,
            TemporalAccumulationInput temporalInput,
            GiCacheSnapshot cacheSnapshot,
            CacheConfidence cacheConfidence,
            DiffuseGiSourceSummary sourceSummary,
            GiRayBudgetAllocation rayBudget
    ) {
        List<DiffuseGiValidationFinding> findings = new ArrayList<>();
        validateFrameInput(findings, frameInput);
        validateTemporalInput(findings, temporalInput);
        validateCache(findings, cacheSnapshot, cacheConfidence);
        validateSourceSummary(findings, sourceSummary);
        validateRayBudget(findings, rayBudget);
        return new DiffuseGiValidationReport(findings);
    }

    private static void validateFrameInput(List<DiffuseGiValidationFinding> findings, DiffuseGiFrameInput frameInput) {
        if (!frameInput.settings().enabled()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "DIFFUSE_GI_DISABLED",
                    "$.settings",
                    "Diffuse GI settings do not request any samples"
            ));
        }
        for (String missingConstant : frameInput.frameConstants().missingRequiredConstants()) {
            findings.add(DiffuseGiValidationFinding.error(
                    "MISSING_FRAME_CONSTANT",
                    "$.frameConstants." + missingConstant,
                    "Diffuse GI requires complete frame constants"
            ));
        }
        if (!frameInput.frameConstants().flags().diffuseGiEnabled()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "FRAME_FLAG_DIFFUSE_GI_DISABLED",
                    "$.frameConstants.flags.diffuseGiEnabled",
                    "Current frame flags indicate diffuse GI is disabled"
            ));
        }
        if (!frameInput.lowResolutionGrid().available()) {
            findings.add(DiffuseGiValidationFinding.error(
                    "LOW_RES_GRID_UNAVAILABLE",
                    "$.lowResolutionGrid",
                    "Diffuse GI requires a low-resolution dispatch grid"
            ));
        }
        if (!frameInput.gBufferWriteIntent().dimensionsAvailable()) {
            findings.add(DiffuseGiValidationFinding.error(
                    "GBUFFER_DIMENSIONS_UNAVAILABLE",
                    "$.gBuffer",
                    "Diffuse GI requires G-buffer dimensions"
            ));
        } else if (!frameInput.dimensionsMatchViewport()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GBUFFER_VIEWPORT_DIMENSION_MISMATCH",
                    "$.gBuffer",
                    "G-buffer dimensions do not match the captured viewport"
            ));
        }
        for (String missingAttachment : frameInput.missingRequiredGBufferAttachments()) {
            findings.add(DiffuseGiValidationFinding.error(
                    "MISSING_GBUFFER_ATTACHMENT",
                    "$.gBuffer.attachments",
                    "Diffuse GI requires G-buffer attachment " + missingAttachment
            ));
        }
        for (String missingAttachment : frameInput.missingTemporalGBufferAttachments()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "MISSING_GBUFFER_HISTORY_ATTACHMENT",
                    "$.gBuffer.attachments",
                    "Temporal GI accumulation cannot use motion history without attachment " + missingAttachment
            ));
        }
    }

    private static void validateTemporalInput(
            List<DiffuseGiValidationFinding> findings,
            TemporalAccumulationInput temporalInput
    ) {
        if (!temporalInput.reuseAllowed()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "TEMPORAL_REUSE_UNAVAILABLE",
                    "$.temporal",
                    temporalInput.resetReason().isBlank()
                            ? "Temporal GI will shade the current frame without history reuse"
                            : temporalInput.resetReason()
            ));
        }
    }

    private static void validateCache(
            List<DiffuseGiValidationFinding> findings,
            GiCacheSnapshot cacheSnapshot,
            CacheConfidence cacheConfidence
    ) {
        if (cacheSnapshot.isEmpty()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_CACHE_EMPTY",
                    "$.cache",
                    "No surface or radiance cache records are available for diffuse GI"
            ));
        }
        if (cacheConfidence.dirty()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GI_CACHE_DIRTY",
                    "$.cache.confidence",
                    cacheConfidence.reason()
            ));
        }
    }

    private static void validateSourceSummary(
            List<DiffuseGiValidationFinding> findings,
            DiffuseGiSourceSummary sourceSummary
    ) {
        if (!sourceSummary.hasWorldMaterialInputs()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SOURCE_WORLD_MATERIAL_PENDING",
                    "$.sourceSummary",
                    sourceSummary.debugLabel()
            ));
        }
        if (!sourceSummary.hasDirectLightingWork()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SOURCE_DIRECT_LIGHT_PENDING",
                    "$.sourceSummary.directLighting",
                    sourceSummary.compactLabel()
            ));
        }
    }

    private static void validateRayBudget(List<DiffuseGiValidationFinding> findings, GiRayBudgetAllocation rayBudget) {
        if (!rayBudget.tier().active()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GI_RAY_BUDGET_DISABLED",
                    "$.rayBudget",
                    rayBudget.reason()
            ));
            return;
        }
        if (rayBudget.tier().requiresTracing() && rayBudget.cappedRays() == 0) {
            findings.add(DiffuseGiValidationFinding.error(
                    "GI_RAY_BUDGET_EMPTY",
                    "$.rayBudget.cappedRays",
                    "Diffuse GI tracing tier selected but no rays are available"
            ));
        }
        if (rayBudget.capped()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_RAY_BUDGET_CAPPED",
                    "$.rayBudget.cappedRays",
                    "Diffuse GI ray budget was capped by the per-frame limit"
            ));
        }
    }
}
