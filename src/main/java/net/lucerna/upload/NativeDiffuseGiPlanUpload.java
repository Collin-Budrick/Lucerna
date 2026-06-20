package net.lucerna.upload;

import net.lucerna.render.frame.FrameJitter;
import net.lucerna.render.lighting.gi.CacheConfidence;
import net.lucerna.render.lighting.gi.DiffuseGiLowResolutionGrid;
import net.lucerna.render.lighting.gi.DiffuseGiSceneInputSummary;
import net.lucerna.render.lighting.gi.DiffuseGiSettings;
import net.lucerna.render.lighting.gi.DiffuseGiSourceSummary;
import net.lucerna.render.lighting.gi.DiffuseGiValidationReport;
import net.lucerna.render.lighting.gi.GiCacheSnapshot;
import net.lucerna.render.lighting.gi.GiRayBudgetAllocation;
import net.lucerna.render.lighting.gi.GiRayBudgetTier;
import net.lucerna.render.lighting.gi.LowResDiffuseGiPlan;
import net.lucerna.render.lighting.gi.TemporalAccumulationInput;

import java.util.Objects;

public record NativeDiffuseGiPlanUpload(
        long generation,
        boolean readyForScheduling,
        boolean requiresTracing,
        boolean reusesTemporalHistory,
        boolean cacheUsable,
        int sourceWidth,
        int sourceHeight,
        int scaleDivisor,
        int gridWidth,
        int gridHeight,
        int gridCellCount,
        int settingsScaleDivisor,
        int samplesPerCell,
        int maxTemporalFrames,
        float settingsTemporalBlendFactor,
        float settingsHistoryConfidenceFloor,
        boolean surfaceCacheEnabled,
        boolean radianceCacheEnabled,
        boolean adaptiveRayBudgetEnabled,
        int rayBudgetTierId,
        String rayBudgetTierName,
        boolean rayBudgetTierActive,
        boolean rayBudgetTierRequiresTracing,
        boolean rayBudgetReuseOnly,
        int raysPerCell,
        int lowResolutionCellCount,
        int requestedRays,
        int cappedRays,
        boolean rayBudgetCapped,
        String rayBudgetReason,
        long frameIndex,
        long previousFrameIndex,
        long temporalCacheGeneration,
        boolean temporalReuseAllowed,
        boolean temporalHistoryReset,
        boolean temporalAccumulates,
        String temporalResetReason,
        float jitterX,
        float jitterY,
        int jitterSequenceIndex,
        int jitterSequenceLength,
        boolean jitterEnabled,
        float temporalBlendFactor,
        float temporalConfidenceFloor,
        int temporalMaxHistoryFrames,
        float cacheConfidence,
        float cacheVariance,
        int cacheSampleCount,
        long cacheConfidenceSourceGeneration,
        long cacheConfidenceLastTouchedFrame,
        boolean cacheDirty,
        String cacheConfidenceReason,
        long cacheGeneration,
        int dirtyRegionCount,
        long firstDirtyRegionGeneration,
        long lastDirtyRegionGeneration,
        int surfaceRecordCount,
        int radianceRecordCount,
        long sourceSummaryGeneration,
        long sourceDirectLightingGeneration,
        long sourceWorldGeneration,
        long sourceMaterialGeneration,
        long sourceSectionGeneration,
        long sourceDirtyRegionGeneration,
        boolean sourceDirectLightingReady,
        boolean sourceWorldInputAvailable,
        boolean sourceMaterialInputAvailable,
        boolean sourceSectionInputAvailable,
        int sourceCelestialLightCount,
        int sourceEmissiveLightCount,
        int sourceShadowCandidateCount,
        int sourceBudgetedShadowCandidateCount,
        int sourceSectionSnapshotCount,
        int sourceDirtyRegionCount,
        int sourceMaterialUpdateCount,
        int sceneSurfaceSampleCount,
        int sceneColoredSurfaceSampleCount,
        int sceneDistinctMaterialCount,
        int sceneSkylitSurfaceCount,
        int sceneSealedInteriorSurfaceCount,
        int sceneDownwardFacingSurfaceCount,
        int sceneVerticalSurfaceCount,
        int sceneDirtySurfaceSampleCount,
        int sceneEmissiveProximitySignals,
        int sceneCacheSampleCountInput,
        boolean sceneCacheDirtyInput,
        int sceneRadianceSampleCount,
        float sceneMaterialDiversityRatio,
        float sceneAverageAlbedoR,
        float sceneAverageAlbedoG,
        float sceneAverageAlbedoB,
        float sceneAverageAlbedoSaturation,
        float sceneColoredBounceInfluence,
        float sceneMaterialColorInfluence,
        float sceneSkylightExposureRatio,
        float sceneSealedInteriorRatio,
        float sceneDownwardFacingRatio,
        float sceneVerticalSurfaceRatio,
        float sceneOrientationBalance,
        float sceneAverageSurfaceNormalX,
        float sceneAverageSurfaceNormalY,
        float sceneAverageSurfaceNormalZ,
        float sceneAverageNormalLength,
        float sceneSurfaceOrientationConfidence,
        float sceneAverageSurfaceRoughness,
        float sceneAverageSurfaceConfidence,
        float sceneUsableSurfaceConfidenceRatio,
        float sceneDirtySurfaceSampleRatio,
        float sceneMaterialOpacityHint,
        float sceneEmissiveProximityScore,
        float sceneEmissiveSourceCoupling,
        float sceneCelestialSourceCoupling,
        float sceneLightSourceSceneCoupling,
        float sceneDirtyRegionInfluence,
        float sceneOcclusionDirtyRegionInfluence,
        float sceneCacheConfidenceInput,
        float sceneCacheVarianceInput,
        float sceneCachePhysicalConfidence,
        float sceneAverageRadianceR,
        float sceneAverageRadianceG,
        float sceneAverageRadianceB,
        float sceneRadianceEnergy,
        float sceneRadianceDirectionConfidence,
        float sceneMaterialGeometryCoupling,
        float scenePhysicalGiInputScore,
        boolean sceneEmissiveProximityAvailable,
        boolean sceneAffectedSurfaceRegionAvailable,
        int sceneAffectedSurfaceMinBlockX,
        int sceneAffectedSurfaceMinBlockY,
        int sceneAffectedSurfaceMinBlockZ,
        int sceneAffectedSurfaceMaxBlockX,
        int sceneAffectedSurfaceMaxBlockY,
        int sceneAffectedSurfaceMaxBlockZ,
        boolean proofHandHudExcluded,
        boolean proofSurfaceOnlyEligible,
        boolean giOutputAuthenticNativeCpu,
        boolean giOutputRealShader,
        boolean denoiseOutputCpuScaffold,
        boolean denoiseOutputRealShader,
        int validationFindingCount,
        int validationErrorCount,
        int validationWarningCount,
        String gridDebugLabel,
        String rayBudgetDebugLabel,
        String cacheConfidenceDebugLabel,
        String sourceDebugLabel,
        String sceneAffectedSurfaceRegionLabel,
        String proofAuthenticityLabel,
        String compactLabel
) {
    public static final int GRID_DIMENSION_STRIDE = 5;
    public static final int GRID_SOURCE_WIDTH_OFFSET = 0;
    public static final int GRID_SOURCE_HEIGHT_OFFSET = 1;
    public static final int GRID_SCALE_DIVISOR_OFFSET = 2;
    public static final int GRID_WIDTH_OFFSET = 3;
    public static final int GRID_HEIGHT_OFFSET = 4;

    public static final int SETTINGS_INTEGER_STRIDE = 6;
    public static final int SETTINGS_SCALE_DIVISOR_OFFSET = 0;
    public static final int SETTINGS_SAMPLES_PER_CELL_OFFSET = 1;
    public static final int SETTINGS_MAX_TEMPORAL_FRAMES_OFFSET = 2;
    public static final int SETTINGS_SURFACE_CACHE_ENABLED_OFFSET = 3;
    public static final int SETTINGS_RADIANCE_CACHE_ENABLED_OFFSET = 4;
    public static final int SETTINGS_ADAPTIVE_RAY_BUDGET_ENABLED_OFFSET = 5;
    public static final int SETTINGS_FLOAT_STRIDE = 2;
    public static final int SETTINGS_TEMPORAL_BLEND_FACTOR_OFFSET = 0;
    public static final int SETTINGS_HISTORY_CONFIDENCE_FLOOR_OFFSET = 1;

    public static final int RAY_BUDGET_STRIDE = 9;
    public static final int RAY_BUDGET_TIER_ID_OFFSET = 0;
    public static final int RAY_BUDGET_TIER_ACTIVE_OFFSET = 1;
    public static final int RAY_BUDGET_TIER_REQUIRES_TRACING_OFFSET = 2;
    public static final int RAY_BUDGET_REUSE_ONLY_OFFSET = 3;
    public static final int RAY_BUDGET_RAYS_PER_CELL_OFFSET = 4;
    public static final int RAY_BUDGET_LOW_RES_CELL_COUNT_OFFSET = 5;
    public static final int RAY_BUDGET_REQUESTED_RAYS_OFFSET = 6;
    public static final int RAY_BUDGET_CAPPED_RAYS_OFFSET = 7;
    public static final int RAY_BUDGET_CAPPED_OFFSET = 8;

    public static final int TEMPORAL_FRAME_STRIDE = 3;
    public static final int TEMPORAL_FRAME_INDEX_OFFSET = 0;
    public static final int TEMPORAL_PREVIOUS_FRAME_INDEX_OFFSET = 1;
    public static final int TEMPORAL_CACHE_GENERATION_OFFSET = 2;
    public static final int TEMPORAL_STATE_STRIDE = 5;
    public static final int TEMPORAL_REUSE_ALLOWED_OFFSET = 0;
    public static final int TEMPORAL_HISTORY_RESET_OFFSET = 1;
    public static final int TEMPORAL_ACCUMULATES_OFFSET = 2;
    public static final int TEMPORAL_JITTER_ENABLED_OFFSET = 3;
    public static final int TEMPORAL_MAX_HISTORY_FRAMES_OFFSET = 4;
    public static final int TEMPORAL_FLOAT_STRIDE = 4;
    public static final int TEMPORAL_JITTER_X_OFFSET = 0;
    public static final int TEMPORAL_JITTER_Y_OFFSET = 1;
    public static final int TEMPORAL_BLEND_FACTOR_OFFSET = 2;
    public static final int TEMPORAL_CONFIDENCE_FLOOR_OFFSET = 3;

    public static final int CACHE_CONFIDENCE_FLOAT_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_VALUE_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_VARIANCE_OFFSET = 1;
    public static final int CACHE_CONFIDENCE_INTEGER_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_SAMPLE_COUNT_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_DIRTY_OFFSET = 1;
    public static final int CACHE_CONFIDENCE_GENERATION_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_SOURCE_GENERATION_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_LAST_TOUCHED_FRAME_OFFSET = 1;

    public static final int CACHE_COUNT_STRIDE = 3;
    public static final int CACHE_DIRTY_REGION_COUNT_OFFSET = 0;
    public static final int CACHE_SURFACE_RECORD_COUNT_OFFSET = 1;
    public static final int CACHE_RADIANCE_RECORD_COUNT_OFFSET = 2;
    public static final int CACHE_GENERATION_STRIDE = 3;
    public static final int CACHE_GENERATION_OFFSET = 0;
    public static final int CACHE_FIRST_DIRTY_GENERATION_OFFSET = 1;
    public static final int CACHE_LAST_DIRTY_GENERATION_OFFSET = 2;

    public static final int SOURCE_GENERATION_STRIDE = 6;
    public static final int SOURCE_SUMMARY_GENERATION_OFFSET = 0;
    public static final int SOURCE_DIRECT_LIGHTING_GENERATION_OFFSET = 1;
    public static final int SOURCE_WORLD_GENERATION_OFFSET = 2;
    public static final int SOURCE_MATERIAL_GENERATION_OFFSET = 3;
    public static final int SOURCE_SECTION_GENERATION_OFFSET = 4;
    public static final int SOURCE_DIRTY_REGION_GENERATION_OFFSET = 5;
    public static final int SOURCE_FLAG_STRIDE = 4;
    public static final int SOURCE_DIRECT_LIGHTING_READY_OFFSET = 0;
    public static final int SOURCE_WORLD_INPUT_AVAILABLE_OFFSET = 1;
    public static final int SOURCE_MATERIAL_INPUT_AVAILABLE_OFFSET = 2;
    public static final int SOURCE_SECTION_INPUT_AVAILABLE_OFFSET = 3;
    public static final int SOURCE_COUNT_STRIDE = 7;
    public static final int SOURCE_CELESTIAL_LIGHT_COUNT_OFFSET = 0;
    public static final int SOURCE_EMISSIVE_LIGHT_COUNT_OFFSET = 1;
    public static final int SOURCE_SHADOW_CANDIDATE_COUNT_OFFSET = 2;
    public static final int SOURCE_BUDGETED_SHADOW_CANDIDATE_COUNT_OFFSET = 3;
    public static final int SOURCE_SECTION_SNAPSHOT_COUNT_OFFSET = 4;
    public static final int SOURCE_DIRTY_REGION_COUNT_OFFSET = 5;
    public static final int SOURCE_MATERIAL_UPDATE_COUNT_OFFSET = 6;
    public static final int SCENE_INPUT_INTEGER_STRIDE = 8;
    public static final int SCENE_SURFACE_SAMPLE_COUNT_OFFSET = 0;
    public static final int SCENE_COLORED_SURFACE_SAMPLE_COUNT_OFFSET = 1;
    public static final int SCENE_SKYLIT_SURFACE_COUNT_OFFSET = 2;
    public static final int SCENE_SEALED_INTERIOR_SURFACE_COUNT_OFFSET = 3;
    public static final int SCENE_EMISSIVE_PROXIMITY_SIGNAL_COUNT_OFFSET = 4;
    public static final int SCENE_CACHE_SAMPLE_COUNT_INPUT_OFFSET = 5;
    public static final int SCENE_CACHE_DIRTY_INPUT_OFFSET = 6;
    public static final int SCENE_RADIANCE_SAMPLE_COUNT_OFFSET = 7;
    public static final int SCENE_INPUT_FLOAT_STRIDE = 14;
    public static final int SCENE_AVERAGE_ALBEDO_R_OFFSET = 0;
    public static final int SCENE_AVERAGE_ALBEDO_G_OFFSET = 1;
    public static final int SCENE_AVERAGE_ALBEDO_B_OFFSET = 2;
    public static final int SCENE_AVERAGE_ALBEDO_SATURATION_OFFSET = 3;
    public static final int SCENE_COLORED_BOUNCE_INFLUENCE_OFFSET = 4;
    public static final int SCENE_SKYLIGHT_EXPOSURE_RATIO_OFFSET = 5;
    public static final int SCENE_SEALED_INTERIOR_RATIO_OFFSET = 6;
    public static final int SCENE_EMISSIVE_PROXIMITY_SCORE_OFFSET = 7;
    public static final int SCENE_CACHE_CONFIDENCE_INPUT_OFFSET = 8;
    public static final int SCENE_CACHE_VARIANCE_INPUT_OFFSET = 9;
    public static final int SCENE_AVERAGE_RADIANCE_R_OFFSET = 10;
    public static final int SCENE_AVERAGE_RADIANCE_G_OFFSET = 11;
    public static final int SCENE_AVERAGE_RADIANCE_B_OFFSET = 12;
    public static final int SCENE_RADIANCE_ENERGY_OFFSET = 13;
    public static final int SCENE_SURFACE_MATERIAL_INTEGER_STRIDE = 4;
    public static final int SCENE_DISTINCT_MATERIAL_COUNT_OFFSET = 0;
    public static final int SCENE_DOWNWARD_FACING_SURFACE_COUNT_OFFSET = 1;
    public static final int SCENE_VERTICAL_SURFACE_COUNT_OFFSET = 2;
    public static final int SCENE_DIRTY_SURFACE_SAMPLE_COUNT_OFFSET = 3;
    public static final int SCENE_SURFACE_MATERIAL_FLOAT_STRIDE = 24;
    public static final int SCENE_MATERIAL_DIVERSITY_RATIO_OFFSET = 0;
    public static final int SCENE_MATERIAL_COLOR_INFLUENCE_OFFSET = 1;
    public static final int SCENE_DOWNWARD_FACING_RATIO_OFFSET = 2;
    public static final int SCENE_VERTICAL_SURFACE_RATIO_OFFSET = 3;
    public static final int SCENE_ORIENTATION_BALANCE_OFFSET = 4;
    public static final int SCENE_AVERAGE_SURFACE_NORMAL_X_OFFSET = 5;
    public static final int SCENE_AVERAGE_SURFACE_NORMAL_Y_OFFSET = 6;
    public static final int SCENE_AVERAGE_SURFACE_NORMAL_Z_OFFSET = 7;
    public static final int SCENE_AVERAGE_NORMAL_LENGTH_OFFSET = 8;
    public static final int SCENE_SURFACE_ORIENTATION_CONFIDENCE_OFFSET = 9;
    public static final int SCENE_AVERAGE_SURFACE_ROUGHNESS_OFFSET = 10;
    public static final int SCENE_AVERAGE_SURFACE_CONFIDENCE_OFFSET = 11;
    public static final int SCENE_USABLE_SURFACE_CONFIDENCE_RATIO_OFFSET = 12;
    public static final int SCENE_DIRTY_SURFACE_SAMPLE_RATIO_OFFSET = 13;
    public static final int SCENE_MATERIAL_OPACITY_HINT_OFFSET = 14;
    public static final int SCENE_EMISSIVE_SOURCE_COUPLING_OFFSET = 15;
    public static final int SCENE_CELESTIAL_SOURCE_COUPLING_OFFSET = 16;
    public static final int SCENE_LIGHT_SOURCE_SCENE_COUPLING_OFFSET = 17;
    public static final int SCENE_DIRTY_REGION_INFLUENCE_OFFSET = 18;
    public static final int SCENE_OCCLUSION_DIRTY_REGION_INFLUENCE_OFFSET = 19;
    public static final int SCENE_CACHE_PHYSICAL_CONFIDENCE_OFFSET = 20;
    public static final int SCENE_RADIANCE_DIRECTION_CONFIDENCE_OFFSET = 21;
    public static final int SCENE_MATERIAL_GEOMETRY_COUPLING_OFFSET = 22;
    public static final int SCENE_PHYSICAL_GI_INPUT_SCORE_OFFSET = 23;
    public static final int SCENE_SURFACE_REGION_BOUNDS_STRIDE = 6;
    public static final int SCENE_SURFACE_REGION_MIN_BLOCK_X_OFFSET = 0;
    public static final int SCENE_SURFACE_REGION_MIN_BLOCK_Y_OFFSET = 1;
    public static final int SCENE_SURFACE_REGION_MIN_BLOCK_Z_OFFSET = 2;
    public static final int SCENE_SURFACE_REGION_MAX_BLOCK_X_OFFSET = 3;
    public static final int SCENE_SURFACE_REGION_MAX_BLOCK_Y_OFFSET = 4;
    public static final int SCENE_SURFACE_REGION_MAX_BLOCK_Z_OFFSET = 5;
    public static final int PROOF_FLAG_STRIDE = 6;
    public static final int PROOF_EMISSIVE_PROXIMITY_AVAILABLE_OFFSET = 0;
    public static final int PROOF_AFFECTED_SURFACE_REGION_AVAILABLE_OFFSET = 1;
    public static final int PROOF_HAND_HUD_EXCLUDED_OFFSET = 2;
    public static final int PROOF_SURFACE_ONLY_ELIGIBLE_OFFSET = 3;
    public static final int PROOF_GI_OUTPUT_AUTHENTIC_NATIVE_CPU_OFFSET = 4;
    public static final int PROOF_GI_OUTPUT_REAL_SHADER_OFFSET = 5;
    public static final int DENOISE_OUTPUT_FLAG_STRIDE = 2;
    public static final int DENOISE_OUTPUT_CPU_SCAFFOLD_OFFSET = 0;
    public static final int DENOISE_OUTPUT_REAL_SHADER_OFFSET = 1;

    public NativeDiffuseGiPlanUpload {
        requireNonNegative(generation, "generation");
        requireNonNegative(sourceWidth, "sourceWidth");
        requireNonNegative(sourceHeight, "sourceHeight");
        requirePositive(scaleDivisor, "scaleDivisor");
        requireNonNegative(gridWidth, "gridWidth");
        requireNonNegative(gridHeight, "gridHeight");
        requireNonNegative(gridCellCount, "gridCellCount");
        if ((sourceWidth == 0 || sourceHeight == 0) && (gridWidth != 0 || gridHeight != 0 || gridCellCount != 0)) {
            throw new IllegalArgumentException("unavailable source dimensions require an unavailable GI grid");
        }
        if (sourceWidth > 0 && sourceHeight > 0 && (gridWidth == 0 || gridHeight == 0)) {
            throw new IllegalArgumentException("available source dimensions require a positive GI grid");
        }

        requirePositive(settingsScaleDivisor, "settingsScaleDivisor");
        requireNonNegative(samplesPerCell, "samplesPerCell");
        requireNonNegative(maxTemporalFrames, "maxTemporalFrames");
        requireUnit(settingsTemporalBlendFactor, "settingsTemporalBlendFactor");
        requireUnit(settingsHistoryConfidenceFloor, "settingsHistoryConfidenceFloor");

        requireNonNegative(rayBudgetTierId, "rayBudgetTierId");
        rayBudgetTierName = requireText(rayBudgetTierName, "rayBudgetTierName");
        requireNonNegative(raysPerCell, "raysPerCell");
        requireNonNegative(lowResolutionCellCount, "lowResolutionCellCount");
        requireNonNegative(requestedRays, "requestedRays");
        requireNonNegative(cappedRays, "cappedRays");
        if (cappedRays > requestedRays) {
            throw new IllegalArgumentException("cappedRays cannot exceed requestedRays");
        }
        if (rayBudgetCapped && cappedRays >= requestedRays) {
            throw new IllegalArgumentException("rayBudgetCapped requires cappedRays to be below requestedRays");
        }
        rayBudgetReason = clean(rayBudgetReason, "GI ray budget");

        requireNonNegative(frameIndex, "frameIndex");
        requireNonNegative(previousFrameIndex, "previousFrameIndex");
        requireNonNegative(temporalCacheGeneration, "temporalCacheGeneration");
        if (temporalReuseAllowed && (frameIndex == 0L || previousFrameIndex == 0L || previousFrameIndex >= frameIndex)) {
            throw new IllegalArgumentException("temporal reuse requires ordered frame indices");
        }
        if (temporalHistoryReset && temporalReuseAllowed) {
            throw new IllegalArgumentException("temporal history reset cannot also allow reuse");
        }
        if (temporalAccumulates && !temporalReuseAllowed) {
            throw new IllegalArgumentException("temporal accumulation requires temporal reuse");
        }
        temporalResetReason = clean(temporalResetReason, "");
        requireFinite(jitterX, "jitterX");
        requireFinite(jitterY, "jitterY");
        requireNonNegative(jitterSequenceIndex, "jitterSequenceIndex");
        requireNonNegative(jitterSequenceLength, "jitterSequenceLength");
        if (jitterSequenceIndex > 0 && jitterSequenceLength == 0) {
            throw new IllegalArgumentException("jitterSequenceIndex requires a non-zero jitterSequenceLength");
        }
        requireUnit(temporalBlendFactor, "temporalBlendFactor");
        requireUnit(temporalConfidenceFloor, "temporalConfidenceFloor");
        requireNonNegative(temporalMaxHistoryFrames, "temporalMaxHistoryFrames");

        requireUnit(cacheConfidence, "cacheConfidence");
        requireFiniteNonNegative(cacheVariance, "cacheVariance");
        requireNonNegative(cacheSampleCount, "cacheSampleCount");
        requireNonNegative(cacheConfidenceSourceGeneration, "cacheConfidenceSourceGeneration");
        requireNonNegative(cacheConfidenceLastTouchedFrame, "cacheConfidenceLastTouchedFrame");
        cacheConfidenceReason = clean(cacheConfidenceReason, cacheDirty ? "dirty" : "available");

        requireNonNegative(cacheGeneration, "cacheGeneration");
        requireNonNegative(dirtyRegionCount, "dirtyRegionCount");
        requireNonNegative(firstDirtyRegionGeneration, "firstDirtyRegionGeneration");
        requireNonNegative(lastDirtyRegionGeneration, "lastDirtyRegionGeneration");
        if (dirtyRegionCount == 0 && (firstDirtyRegionGeneration != 0L || lastDirtyRegionGeneration != 0L)) {
            throw new IllegalArgumentException("empty dirty region handoff must use zero generation bounds");
        }
        if (dirtyRegionCount > 0) {
            if (firstDirtyRegionGeneration == 0L || lastDirtyRegionGeneration == 0L) {
                throw new IllegalArgumentException("dirty region handoff requires positive generation bounds");
            }
            if (firstDirtyRegionGeneration > lastDirtyRegionGeneration) {
                throw new IllegalArgumentException("firstDirtyRegionGeneration must be <= lastDirtyRegionGeneration");
            }
        }
        requireNonNegative(surfaceRecordCount, "surfaceRecordCount");
        requireNonNegative(radianceRecordCount, "radianceRecordCount");
        requireNonNegative(sourceSummaryGeneration, "sourceSummaryGeneration");
        requireNonNegative(sourceDirectLightingGeneration, "sourceDirectLightingGeneration");
        requireNonNegative(sourceWorldGeneration, "sourceWorldGeneration");
        requireNonNegative(sourceMaterialGeneration, "sourceMaterialGeneration");
        requireNonNegative(sourceSectionGeneration, "sourceSectionGeneration");
        requireNonNegative(sourceDirtyRegionGeneration, "sourceDirtyRegionGeneration");
        requireNonNegative(sourceCelestialLightCount, "sourceCelestialLightCount");
        requireNonNegative(sourceEmissiveLightCount, "sourceEmissiveLightCount");
        requireNonNegative(sourceShadowCandidateCount, "sourceShadowCandidateCount");
        requireNonNegative(sourceBudgetedShadowCandidateCount, "sourceBudgetedShadowCandidateCount");
        requireNonNegative(sourceSectionSnapshotCount, "sourceSectionSnapshotCount");
        requireNonNegative(sourceDirtyRegionCount, "sourceDirtyRegionCount");
        requireNonNegative(sourceMaterialUpdateCount, "sourceMaterialUpdateCount");
        requireNonNegative(sceneSurfaceSampleCount, "sceneSurfaceSampleCount");
        requireNonNegative(sceneColoredSurfaceSampleCount, "sceneColoredSurfaceSampleCount");
        requireNonNegative(sceneDistinctMaterialCount, "sceneDistinctMaterialCount");
        requireNonNegative(sceneSkylitSurfaceCount, "sceneSkylitSurfaceCount");
        requireNonNegative(sceneSealedInteriorSurfaceCount, "sceneSealedInteriorSurfaceCount");
        requireNonNegative(sceneDownwardFacingSurfaceCount, "sceneDownwardFacingSurfaceCount");
        requireNonNegative(sceneVerticalSurfaceCount, "sceneVerticalSurfaceCount");
        requireNonNegative(sceneDirtySurfaceSampleCount, "sceneDirtySurfaceSampleCount");
        requireNonNegative(sceneEmissiveProximitySignals, "sceneEmissiveProximitySignals");
        requireNonNegative(sceneCacheSampleCountInput, "sceneCacheSampleCountInput");
        requireNonNegative(sceneRadianceSampleCount, "sceneRadianceSampleCount");
        requireUnit(sceneMaterialDiversityRatio, "sceneMaterialDiversityRatio");
        requireUnit(sceneAverageAlbedoR, "sceneAverageAlbedoR");
        requireUnit(sceneAverageAlbedoG, "sceneAverageAlbedoG");
        requireUnit(sceneAverageAlbedoB, "sceneAverageAlbedoB");
        requireUnit(sceneAverageAlbedoSaturation, "sceneAverageAlbedoSaturation");
        requireUnit(sceneColoredBounceInfluence, "sceneColoredBounceInfluence");
        requireUnit(sceneMaterialColorInfluence, "sceneMaterialColorInfluence");
        requireUnit(sceneSkylightExposureRatio, "sceneSkylightExposureRatio");
        requireUnit(sceneSealedInteriorRatio, "sceneSealedInteriorRatio");
        requireUnit(sceneDownwardFacingRatio, "sceneDownwardFacingRatio");
        requireUnit(sceneVerticalSurfaceRatio, "sceneVerticalSurfaceRatio");
        requireUnit(sceneOrientationBalance, "sceneOrientationBalance");
        requireFinite(sceneAverageSurfaceNormalX, "sceneAverageSurfaceNormalX");
        requireFinite(sceneAverageSurfaceNormalY, "sceneAverageSurfaceNormalY");
        requireFinite(sceneAverageSurfaceNormalZ, "sceneAverageSurfaceNormalZ");
        requireUnit(sceneAverageNormalLength, "sceneAverageNormalLength");
        requireUnit(sceneSurfaceOrientationConfidence, "sceneSurfaceOrientationConfidence");
        requireUnit(sceneAverageSurfaceRoughness, "sceneAverageSurfaceRoughness");
        requireUnit(sceneAverageSurfaceConfidence, "sceneAverageSurfaceConfidence");
        requireUnit(sceneUsableSurfaceConfidenceRatio, "sceneUsableSurfaceConfidenceRatio");
        requireUnit(sceneDirtySurfaceSampleRatio, "sceneDirtySurfaceSampleRatio");
        requireUnit(sceneMaterialOpacityHint, "sceneMaterialOpacityHint");
        requireUnit(sceneEmissiveProximityScore, "sceneEmissiveProximityScore");
        requireUnit(sceneEmissiveSourceCoupling, "sceneEmissiveSourceCoupling");
        requireUnit(sceneCelestialSourceCoupling, "sceneCelestialSourceCoupling");
        requireUnit(sceneLightSourceSceneCoupling, "sceneLightSourceSceneCoupling");
        requireUnit(sceneDirtyRegionInfluence, "sceneDirtyRegionInfluence");
        requireUnit(sceneOcclusionDirtyRegionInfluence, "sceneOcclusionDirtyRegionInfluence");
        requireUnit(sceneCacheConfidenceInput, "sceneCacheConfidenceInput");
        requireFiniteNonNegative(sceneCacheVarianceInput, "sceneCacheVarianceInput");
        requireUnit(sceneCachePhysicalConfidence, "sceneCachePhysicalConfidence");
        requireFiniteNonNegative(sceneAverageRadianceR, "sceneAverageRadianceR");
        requireFiniteNonNegative(sceneAverageRadianceG, "sceneAverageRadianceG");
        requireFiniteNonNegative(sceneAverageRadianceB, "sceneAverageRadianceB");
        requireFiniteNonNegative(sceneRadianceEnergy, "sceneRadianceEnergy");
        requireUnit(sceneRadianceDirectionConfidence, "sceneRadianceDirectionConfidence");
        requireUnit(sceneMaterialGeometryCoupling, "sceneMaterialGeometryCoupling");
        requireUnit(scenePhysicalGiInputScore, "scenePhysicalGiInputScore");
        if (!sceneAffectedSurfaceRegionAvailable) {
            requireZero(sceneAffectedSurfaceMinBlockX, "sceneAffectedSurfaceMinBlockX");
            requireZero(sceneAffectedSurfaceMinBlockY, "sceneAffectedSurfaceMinBlockY");
            requireZero(sceneAffectedSurfaceMinBlockZ, "sceneAffectedSurfaceMinBlockZ");
            requireZero(sceneAffectedSurfaceMaxBlockX, "sceneAffectedSurfaceMaxBlockX");
            requireZero(sceneAffectedSurfaceMaxBlockY, "sceneAffectedSurfaceMaxBlockY");
            requireZero(sceneAffectedSurfaceMaxBlockZ, "sceneAffectedSurfaceMaxBlockZ");
        } else if (sceneAffectedSurfaceMinBlockX > sceneAffectedSurfaceMaxBlockX
                || sceneAffectedSurfaceMinBlockY > sceneAffectedSurfaceMaxBlockY
                || sceneAffectedSurfaceMinBlockZ > sceneAffectedSurfaceMaxBlockZ) {
            throw new IllegalArgumentException("affected surface min bounds must be <= max bounds");
        }
        if (proofSurfaceOnlyEligible && !sceneAffectedSurfaceRegionAvailable) {
            throw new IllegalArgumentException("surface-only proof eligibility requires an affected surface region");
        }
        if (proofSurfaceOnlyEligible && !proofHandHudExcluded) {
            throw new IllegalArgumentException("surface-only proof eligibility requires hand/HUD-excluded capture hints");
        }
        if (giOutputRealShader && giOutputAuthenticNativeCpu) {
            throw new IllegalArgumentException("GI output cannot be both real shader and CPU scaffold");
        }
        if (denoiseOutputRealShader && denoiseOutputCpuScaffold) {
            throw new IllegalArgumentException("denoise output cannot be both real shader and CPU scaffold");
        }
        requireNonNegative(validationFindingCount, "validationFindingCount");
        requireNonNegative(validationErrorCount, "validationErrorCount");
        requireNonNegative(validationWarningCount, "validationWarningCount");
        if (validationErrorCount > validationFindingCount || validationWarningCount > validationFindingCount) {
            throw new IllegalArgumentException("validation counts cannot exceed validationFindingCount");
        }
        gridDebugLabel = requireText(gridDebugLabel, "gridDebugLabel");
        rayBudgetDebugLabel = requireText(rayBudgetDebugLabel, "rayBudgetDebugLabel");
        cacheConfidenceDebugLabel = requireText(cacheConfidenceDebugLabel, "cacheConfidenceDebugLabel");
        sourceDebugLabel = requireText(sourceDebugLabel, "sourceDebugLabel");
        sceneAffectedSurfaceRegionLabel = requireText(sceneAffectedSurfaceRegionLabel, "sceneAffectedSurfaceRegionLabel");
        proofAuthenticityLabel = requireText(proofAuthenticityLabel, "proofAuthenticityLabel");
        compactLabel = requireText(compactLabel, "compactLabel");
    }

    public static NativeDiffuseGiPlanUpload from(LowResDiffuseGiPlan plan) {
        Objects.requireNonNull(plan, "plan");

        DiffuseGiLowResolutionGrid grid = plan.frameInput().lowResolutionGrid();
        DiffuseGiSettings settings = plan.frameInput().settings();
        GiRayBudgetAllocation rayBudget = plan.rayBudget();
        GiRayBudgetTier tier = rayBudget.tier();
        TemporalAccumulationInput temporalInput = plan.temporalInput();
        FrameJitter jitter = temporalInput.jitter();
        CacheConfidence confidence = plan.cacheConfidence();
        GiCacheSnapshot cacheSnapshot = plan.cacheSnapshot();
        DiffuseGiSourceSummary sourceSummary = plan.sourceSummary();
        DiffuseGiSceneInputSummary sceneInputs = plan.sceneInputSummary();
        DiffuseGiValidationReport validationReport = plan.validationReport();

        long generation = max(
                cacheSnapshot.cacheGeneration(),
                cacheSnapshot.latestDirtyGeneration(),
                confidence.sourceGeneration(),
                confidence.lastTouchedFrame(),
                temporalInput.frameIndex(),
                sourceSummary.generation()
        );

        return new NativeDiffuseGiPlanUpload(
                generation,
                plan.readyForScheduling(),
                plan.requiresTracing(),
                plan.reusesTemporalHistory(),
                plan.cacheUsable(),
                grid.sourceWidth(),
                grid.sourceHeight(),
                grid.scaleDivisor(),
                grid.width(),
                grid.height(),
                grid.cellCount(),
                settings.internalScaleDivisor(),
                settings.samplesPerCell(),
                settings.maxTemporalFrames(),
                settings.temporalBlendFactor(),
                settings.historyConfidenceFloor(),
                settings.surfaceCacheEnabled(),
                settings.radianceCacheEnabled(),
                settings.adaptiveRayBudgetEnabled(),
                tierId(tier),
                tier.name(),
                tier.active(),
                tier.requiresTracing(),
                rayBudget.reuseOnly(),
                rayBudget.raysPerCell(),
                rayBudget.lowResolutionCellCount(),
                rayBudget.requestedRays(),
                rayBudget.cappedRays(),
                rayBudget.capped(),
                rayBudget.reason(),
                temporalInput.frameIndex(),
                temporalInput.previousFrameIndex(),
                temporalInput.cacheGeneration(),
                temporalInput.reuseAllowed(),
                temporalInput.historyReset(),
                temporalInput.accumulates(),
                temporalInput.resetReason(),
                jitter.x(),
                jitter.y(),
                jitter.sequenceIndex(),
                jitter.sequenceLength(),
                jitter.enabled(),
                temporalInput.blendFactor(),
                temporalInput.confidenceFloor(),
                temporalInput.maxHistoryFrames(),
                confidence.confidence(),
                confidence.variance(),
                confidence.sampleCount(),
                confidence.sourceGeneration(),
                confidence.lastTouchedFrame(),
                confidence.dirty(),
                confidence.reason(),
                cacheSnapshot.cacheGeneration(),
                cacheSnapshot.dirtyRegionCount(),
                cacheSnapshot.dirtyRegions().firstGeneration(),
                cacheSnapshot.dirtyRegions().lastGeneration(),
                cacheSnapshot.surfaceRecordCount(),
                cacheSnapshot.radianceRecordCount(),
                sourceSummary.generation(),
                sourceSummary.directLightingGeneration(),
                sourceSummary.worldGeneration(),
                sourceSummary.materialGeneration(),
                sourceSummary.sectionGeneration(),
                sourceSummary.dirtyRegionGeneration(),
                sourceSummary.directLightingReady(),
                sourceSummary.worldInputAvailable(),
                sourceSummary.materialInputAvailable(),
                sourceSummary.sectionInputAvailable(),
                sourceSummary.celestialLightCount(),
                sourceSummary.emissiveLightCount(),
                sourceSummary.shadowCandidateCount(),
                sourceSummary.budgetedShadowCandidateCount(),
                sourceSummary.sectionSnapshotCount(),
                sourceSummary.dirtyRegionCount(),
                sourceSummary.materialUpdateCount(),
                sceneInputs.surfaceSampleCount(),
                sceneInputs.coloredSurfaceSampleCount(),
                sceneInputs.distinctMaterialCount(),
                sceneInputs.skylitSurfaceCount(),
                sceneInputs.sealedInteriorSurfaceCount(),
                sceneInputs.downwardFacingSurfaceCount(),
                sceneInputs.verticalSurfaceCount(),
                sceneInputs.dirtySurfaceSampleCount(),
                sceneInputs.emissiveProximitySignals(),
                sceneInputs.cacheSampleCountInput(),
                sceneInputs.cacheDirtyInput(),
                sceneInputs.radianceSampleCount(),
                sceneInputs.materialDiversityRatio(),
                sceneInputs.averageAlbedoR(),
                sceneInputs.averageAlbedoG(),
                sceneInputs.averageAlbedoB(),
                sceneInputs.averageAlbedoSaturation(),
                sceneInputs.coloredBounceInfluence(),
                sceneInputs.materialColorInfluence(),
                sceneInputs.skylightExposureRatio(),
                sceneInputs.sealedInteriorRatio(),
                sceneInputs.downwardFacingRatio(),
                sceneInputs.verticalSurfaceRatio(),
                sceneInputs.orientationBalance(),
                sceneInputs.averageSurfaceNormalX(),
                sceneInputs.averageSurfaceNormalY(),
                sceneInputs.averageSurfaceNormalZ(),
                sceneInputs.averageNormalLength(),
                sceneInputs.surfaceOrientationConfidence(),
                sceneInputs.averageSurfaceRoughness(),
                sceneInputs.averageSurfaceConfidence(),
                sceneInputs.usableSurfaceConfidenceRatio(),
                sceneInputs.dirtySurfaceSampleRatio(),
                sceneInputs.materialOpacityHint(),
                sceneInputs.emissiveProximityScore(),
                sceneInputs.emissiveSourceCoupling(),
                sceneInputs.celestialSourceCoupling(),
                sceneInputs.lightSourceSceneCoupling(),
                sceneInputs.dirtyRegionInfluence(),
                sceneInputs.occlusionDirtyRegionInfluence(),
                sceneInputs.cacheConfidenceInput(),
                sceneInputs.cacheVarianceInput(),
                sceneInputs.cachePhysicalConfidence(),
                sceneInputs.averageRadianceR(),
                sceneInputs.averageRadianceG(),
                sceneInputs.averageRadianceB(),
                sceneInputs.radianceEnergy(),
                sceneInputs.radianceDirectionConfidence(),
                sceneInputs.materialGeometryCoupling(),
                sceneInputs.physicalGiInputScore(),
                sceneInputs.emissiveProximityAvailable(),
                sceneInputs.affectedSurfaceRegionAvailable(),
                sceneInputs.affectedSurfaceMinBlockX(),
                sceneInputs.affectedSurfaceMinBlockY(),
                sceneInputs.affectedSurfaceMinBlockZ(),
                sceneInputs.affectedSurfaceMaxBlockX(),
                sceneInputs.affectedSurfaceMaxBlockY(),
                sceneInputs.affectedSurfaceMaxBlockZ(),
                true,
                sceneInputs.readyForSurfaceOnlyProof(),
                plan.readyForScheduling() && plan.requiresTracing(),
                false,
                true,
                false,
                validationReport.findings().size(),
                validationReport.errorCount(),
                validationReport.warningCount(),
                plan.gridDebugLabel(),
                plan.rayBudgetDebugLabel(),
                plan.cacheConfidenceDebugLabel(),
                plan.sourceDebugLabel(),
                sceneInputs.affectedSurfaceRegionLabel(),
                proofAuthenticityLabel(sceneInputs, plan),
                plan.compactLabel()
        );
    }

    public int[] gridDimensions() {
        return new int[]{
                this.sourceWidth,
                this.sourceHeight,
                this.scaleDivisor,
                this.gridWidth,
                this.gridHeight
        };
    }

    public int[] settingsIntegers() {
        return new int[]{
                this.settingsScaleDivisor,
                this.samplesPerCell,
                this.maxTemporalFrames,
                this.surfaceCacheEnabled ? 1 : 0,
                this.radianceCacheEnabled ? 1 : 0,
                this.adaptiveRayBudgetEnabled ? 1 : 0
        };
    }

    public float[] settingsFloats() {
        return new float[]{
                this.settingsTemporalBlendFactor,
                this.settingsHistoryConfidenceFloor
        };
    }

    public int[] rayBudgetData() {
        return new int[]{
                this.rayBudgetTierId,
                this.rayBudgetTierActive ? 1 : 0,
                this.rayBudgetTierRequiresTracing ? 1 : 0,
                this.rayBudgetReuseOnly ? 1 : 0,
                this.raysPerCell,
                this.lowResolutionCellCount,
                this.requestedRays,
                this.cappedRays,
                this.rayBudgetCapped ? 1 : 0
        };
    }

    public long[] temporalFrames() {
        return new long[]{
                this.frameIndex,
                this.previousFrameIndex,
                this.temporalCacheGeneration
        };
    }

    public int[] temporalState() {
        return new int[]{
                this.temporalReuseAllowed ? 1 : 0,
                this.temporalHistoryReset ? 1 : 0,
                this.temporalAccumulates ? 1 : 0,
                this.jitterEnabled ? 1 : 0,
                this.temporalMaxHistoryFrames
        };
    }

    public float[] temporalFloats() {
        return new float[]{
                this.jitterX,
                this.jitterY,
                this.temporalBlendFactor,
                this.temporalConfidenceFloor
        };
    }

    public float[] cacheConfidenceFloats() {
        return new float[]{
                this.cacheConfidence,
                this.cacheVariance
        };
    }

    public int[] cacheConfidenceIntegers() {
        return new int[]{
                this.cacheSampleCount,
                this.cacheDirty ? 1 : 0
        };
    }

    public long[] cacheConfidenceGenerations() {
        return new long[]{
                this.cacheConfidenceSourceGeneration,
                this.cacheConfidenceLastTouchedFrame
        };
    }

    public int[] cacheCounts() {
        return new int[]{
                this.dirtyRegionCount,
                this.surfaceRecordCount,
                this.radianceRecordCount
        };
    }

    public long[] cacheGenerations() {
        return new long[]{
                this.cacheGeneration,
                this.firstDirtyRegionGeneration,
                this.lastDirtyRegionGeneration
        };
    }

    public long[] sourceGenerations() {
        return new long[]{
                this.sourceSummaryGeneration,
                this.sourceDirectLightingGeneration,
                this.sourceWorldGeneration,
                this.sourceMaterialGeneration,
                this.sourceSectionGeneration,
                this.sourceDirtyRegionGeneration
        };
    }

    public int[] sourceFlags() {
        return new int[]{
                this.sourceDirectLightingReady ? 1 : 0,
                this.sourceWorldInputAvailable ? 1 : 0,
                this.sourceMaterialInputAvailable ? 1 : 0,
                this.sourceSectionInputAvailable ? 1 : 0
        };
    }

    public int[] sourceCounts() {
        return new int[]{
                this.sourceCelestialLightCount,
                this.sourceEmissiveLightCount,
                this.sourceShadowCandidateCount,
                this.sourceBudgetedShadowCandidateCount,
                this.sourceSectionSnapshotCount,
                this.sourceDirtyRegionCount,
                this.sourceMaterialUpdateCount
        };
    }

    public int[] sceneInputIntegers() {
        return new int[]{
                this.sceneSurfaceSampleCount,
                this.sceneColoredSurfaceSampleCount,
                this.sceneSkylitSurfaceCount,
                this.sceneSealedInteriorSurfaceCount,
                this.sceneEmissiveProximitySignals,
                this.sceneCacheSampleCountInput,
                this.sceneCacheDirtyInput ? 1 : 0,
                this.sceneRadianceSampleCount
        };
    }

    public float[] sceneInputFloats() {
        return new float[]{
                this.sceneAverageAlbedoR,
                this.sceneAverageAlbedoG,
                this.sceneAverageAlbedoB,
                this.sceneAverageAlbedoSaturation,
                this.sceneColoredBounceInfluence,
                this.sceneSkylightExposureRatio,
                this.sceneSealedInteriorRatio,
                this.sceneEmissiveProximityScore,
                this.sceneCacheConfidenceInput,
                this.sceneCacheVarianceInput,
                this.sceneAverageRadianceR,
                this.sceneAverageRadianceG,
                this.sceneAverageRadianceB,
                this.sceneRadianceEnergy
        };
    }

    public int[] sceneSurfaceMaterialIntegers() {
        return new int[]{
                this.sceneDistinctMaterialCount,
                this.sceneDownwardFacingSurfaceCount,
                this.sceneVerticalSurfaceCount,
                this.sceneDirtySurfaceSampleCount
        };
    }

    public float[] sceneSurfaceMaterialFloats() {
        return new float[]{
                this.sceneMaterialDiversityRatio,
                this.sceneMaterialColorInfluence,
                this.sceneDownwardFacingRatio,
                this.sceneVerticalSurfaceRatio,
                this.sceneOrientationBalance,
                this.sceneAverageSurfaceNormalX,
                this.sceneAverageSurfaceNormalY,
                this.sceneAverageSurfaceNormalZ,
                this.sceneAverageNormalLength,
                this.sceneSurfaceOrientationConfidence,
                this.sceneAverageSurfaceRoughness,
                this.sceneAverageSurfaceConfidence,
                this.sceneUsableSurfaceConfidenceRatio,
                this.sceneDirtySurfaceSampleRatio,
                this.sceneMaterialOpacityHint,
                this.sceneEmissiveSourceCoupling,
                this.sceneCelestialSourceCoupling,
                this.sceneLightSourceSceneCoupling,
                this.sceneDirtyRegionInfluence,
                this.sceneOcclusionDirtyRegionInfluence,
                this.sceneCachePhysicalConfidence,
                this.sceneRadianceDirectionConfidence,
                this.sceneMaterialGeometryCoupling,
                this.scenePhysicalGiInputScore
        };
    }

    public int[] sceneSurfaceRegionBounds() {
        return new int[]{
                this.sceneAffectedSurfaceMinBlockX,
                this.sceneAffectedSurfaceMinBlockY,
                this.sceneAffectedSurfaceMinBlockZ,
                this.sceneAffectedSurfaceMaxBlockX,
                this.sceneAffectedSurfaceMaxBlockY,
                this.sceneAffectedSurfaceMaxBlockZ
        };
    }

    public int[] proofFlags() {
        return new int[]{
                this.sceneEmissiveProximityAvailable ? 1 : 0,
                this.sceneAffectedSurfaceRegionAvailable ? 1 : 0,
                this.proofHandHudExcluded ? 1 : 0,
                this.proofSurfaceOnlyEligible ? 1 : 0,
                this.giOutputAuthenticNativeCpu ? 1 : 0,
                this.giOutputRealShader ? 1 : 0
        };
    }

    public int[] denoiseOutputFlags() {
        return new int[]{
                this.denoiseOutputCpuScaffold ? 1 : 0,
                this.denoiseOutputRealShader ? 1 : 0
        };
    }

    public String[] debugLabels() {
        return new String[]{
                this.gridDebugLabel,
                this.rayBudgetDebugLabel,
                this.cacheConfidenceDebugLabel,
                this.sourceDebugLabel,
                this.sceneAffectedSurfaceRegionLabel,
                this.proofAuthenticityLabel,
                this.compactLabel
        };
    }

    private static int tierId(GiRayBudgetTier tier) {
        return switch (Objects.requireNonNull(tier, "tier")) {
            case DISABLED -> 0;
            case REUSE_ONLY -> 1;
            case LOW -> 2;
            case MEDIUM -> 3;
            case HIGH -> 4;
        };
    }

    private static long max(long first, long second, long third, long fourth, long fifth, long sixth) {
        return Math.max(Math.max(Math.max(first, second), Math.max(third, fourth)), Math.max(fifth, sixth));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireZero(int value, String name) {
        if (value != 0) {
            throw new IllegalArgumentException(name + " must be zero when affected surface region is unavailable");
        }
    }

    private static void requireUnit(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private static void requireFiniteNonNegative(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static String proofAuthenticityLabel(DiffuseGiSceneInputSummary sceneInputs, LowResDiffuseGiPlan plan) {
        return "surfaceOnlyEligible=" + sceneInputs.readyForSurfaceOnlyProof()
                + " handHudExcludedHint=true"
                + " affectedSurfaceRegion=\"" + sceneInputs.affectedSurfaceRegionLabel() + "\""
                + " emissiveProximityAvailable=" + sceneInputs.emissiveProximityAvailable()
                + " surfaceNormal=" + sceneInputs.averageSurfaceNormalX()
                + "," + sceneInputs.averageSurfaceNormalY()
                + "," + sceneInputs.averageSurfaceNormalZ()
                + " roughness=" + sceneInputs.averageSurfaceRoughness()
                + " surfaceCacheConfidence=" + sceneInputs.averageSurfaceConfidence()
                + " materialOpacityHint=" + sceneInputs.materialOpacityHint()
                + " physicalGiInputScore=" + sceneInputs.physicalGiInputScore()
                + " giOutputSource=native-cpu-scaffold"
                + " realShaderGiOutput=false"
                + " denoiseOutput=cpu-scaffold"
                + " realDenoiseShaderOutput=false"
                + " requiresTracing=" + plan.requiresTracing()
                + " readyForScheduling=" + plan.readyForScheduling();
    }
}
