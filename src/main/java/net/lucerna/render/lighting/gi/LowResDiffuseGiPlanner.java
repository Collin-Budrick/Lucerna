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
                resolvedSettings,
                resolvedSourceSummary
        );
        DiffuseGiSceneInputSummary sceneInputSummary = DiffuseGiSceneInputSummary.from(
                resolvedSourceSummary,
                resolvedCacheSnapshot,
                cacheConfidence
        );
        DiffuseGiValidationReport validationReport = validate(
                frameInput,
                temporalInput,
                resolvedCacheSnapshot,
                cacheConfidence,
                resolvedSourceSummary,
                sceneInputSummary,
                rayBudget
        );
        return new LowResDiffuseGiPlan(
                frameInput,
                temporalInput,
                resolvedCacheSnapshot,
                cacheConfidence,
                rayBudget,
                resolvedSourceSummary,
                sceneInputSummary,
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
            DiffuseGiSceneInputSummary sceneInputSummary,
            GiRayBudgetAllocation rayBudget
    ) {
        List<DiffuseGiValidationFinding> findings = new ArrayList<>();
        validateFrameInput(findings, frameInput);
        validateTemporalInput(findings, temporalInput);
        validateCache(findings, cacheSnapshot, cacheConfidence);
        validateSourceSummary(findings, sourceSummary, sceneInputSummary);
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
            DiffuseGiSourceSummary sourceSummary,
            DiffuseGiSceneInputSummary sceneInputSummary
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
        DiffuseGiSceneInputSummary sceneInputs = sceneInputSummary == null
                ? DiffuseGiSceneInputSummary.from(sourceSummary, null, null)
                : sceneInputSummary;
        if (!sceneInputs.hasSceneTiedInputs()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SCENE_INPUTS_PENDING",
                    "$.sceneInputs",
                    "Scene-tied GI metadata will be populated from cache surface/radiance records when available"
            ));
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_PHYSICAL_EVIDENCE_PENDING",
                    "$.sceneInputs.physicalReadiness",
                    sceneInputs.physicalEvidenceRejectionLabel()
            ));
            return;
        }
        if (sceneInputs.surfaceSampleCount() == 0) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SURFACE_INPUTS_PENDING",
                    "$.sceneInputs.surfaceSampleCount",
                    "Diffuse GI cannot claim geometry/material-aware behavior until surface cache samples are available"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && sceneInputs.distinctMaterialCount() <= 1) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_MATERIAL_DIVERSITY_LIMITED",
                    "$.sceneInputs.materialDiversityRatio",
                    "Surface inputs currently represent one material; treat GI response as weakly material-tied"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && sceneInputs.materialColorInfluence() < 0.08F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_MATERIAL_COLOR_INFLUENCE_LOW",
                    "$.sceneInputs.materialColorInfluence",
                    "Diffuse GI has weak material/color input; do not claim colored bounce response from proof-only evidence"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && !sceneInputs.hasColoredBounceEvidence()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_COLORED_BOUNCE_EVIDENCE_PENDING",
                    "$.sceneInputs.coloredBounceEvidence",
                    "Colored bounce needs saturated surface albedo plus source/direct light, normals, nearby geometry, hit/cache confidence, and material coupling; reject global wash or metadata-only color"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && sceneInputs.materialGeometryCoupling() < 0.12F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_MATERIAL_GEOMETRY_COUPLING_LOW",
                    "$.sceneInputs.materialGeometryCoupling",
                    "Material color response is not yet tied strongly enough to surface normals/skylight/interior geometry"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && sceneInputs.averageNormalLength() < 0.5F) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GI_SURFACE_NORMAL_CONFIDENCE_LOW",
                    "$.sceneInputs.averageNormalLength",
                    "Surface normal vectors are weak; physical GI direction/orientation claims should be withheld"
            ));
        }
        if (sceneInputs.surfaceSampleCount() > 0 && sceneInputs.surfaceOrientationConfidence() < 0.35F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SURFACE_ORIENTATION_CONFIDENCE_LOW",
                    "$.sceneInputs.surfaceOrientationConfidence",
                    "Surface orientation input is too weak to distinguish physically linked bounce lighting from screen-space proof shaping"
            ));
        }
        if (sceneInputs.surfaceSampleCount() >= 4 && sceneInputs.skylightInteriorContrast() < 0.08F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SKYLIGHT_INTERIOR_CONTRAST_LOW",
                    "$.sceneInputs.skylightInteriorContrast",
                    "Scene inputs do not yet distinguish skylit surfaces from sealed/interior surfaces strongly enough for low-res GI proof"
            ));
        }
        if (sceneInputs.surfaceSampleCount() >= 4 && sceneInputs.orientationBalance() < 0.12F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_ORIENTATION_DISTRIBUTION_NARROW",
                    "$.sceneInputs.orientationBalance",
                    "Surface samples are dominated by one orientation, so bounce-light evidence may be scene-specific"
            ));
        }
        if (sourceSummary.directLightingReady() && sceneInputs.lightSourceSceneCoupling() < 0.05F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_LIGHT_SOURCE_SCENE_COUPLING_LOW",
                    "$.sceneInputs.lightSourceSceneCoupling",
                    "Direct light metadata exists, but emissive/sun/moon sources are not yet coupled to sampled scene geometry; " + sourceSummary.lightSourceCouplingLabel()
            ));
        }
        if (sourceSummary.emissiveLightCount() > 0 && sceneInputs.emissiveSourceCoupling() < 0.04F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_EMISSIVE_SOURCE_COUPLING_LOW",
                    "$.sceneInputs.emissiveSourceCoupling",
                    "Emissive source data exists but has weak proximity/material/normal linkage to affected surfaces"
            ));
        }
        if (sourceSummary.celestialLightCount() > 0 && sceneInputs.celestialSourceCoupling() < 0.04F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_SUN_MOON_SOURCE_COUPLING_LOW",
                    "$.sceneInputs.celestialSourceCoupling",
                    "Sun/moon source data exists but has weak skylight/interior/surface-normal linkage"
            ));
        }
        if (sceneInputs.radianceSampleCount() > 0 && sceneInputs.radianceDirectionConfidence() < 0.35F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_RADIANCE_DIRECTION_CONFIDENCE_LOW",
                    "$.sceneInputs.radianceDirectionConfidence",
                    "Radiance cache direction data is noisy or underdefined; do not overclaim directional GI yet"
            ));
        }
        if (sceneInputs.dirtyRegionInfluence() > 0.35F) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GI_DIRTY_REGION_INFLUENCE_HIGH",
                    "$.sceneInputs.dirtyRegionInfluence",
                    "Dirty regions dominate current GI inputs; cache-backed physical claims need fresh controller proof"
            ));
        }
        if (sceneInputs.cacheSampleCountInput() > 0 && sceneInputs.cachePhysicalConfidence() < 0.08F) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_CACHE_PHYSICAL_CONFIDENCE_LOW",
                    "$.sceneInputs.cachePhysicalConfidence",
                    "Cache confidence, variance, dirty state, or sample count is too weak for physical GI evidence"
            ));
        }
        if (sceneInputs.readyForSurfaceOnlyProof() && !sceneInputs.hasPhysicalGiEvidence()) {
            findings.add(DiffuseGiValidationFinding.warning(
                    "GI_SURFACE_PROOF_NOT_PHYSICAL",
                    "$.sceneInputs.physicalGiInputScore",
                    "Surface-only or screenshot-visible proof is not physical GI until emissive, material/color, orientation, occlusion/dirty, and cache signals are all scene-linked"
            ));
        }
        if (!sceneInputs.hasPhysicalGiEvidence()) {
            findings.add(DiffuseGiValidationFinding.info(
                    "GI_PHYSICAL_EVIDENCE_PENDING",
                    "$.sceneInputs.physicalReadiness",
                    sceneInputs.physicalEvidenceRejectionLabel()
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
