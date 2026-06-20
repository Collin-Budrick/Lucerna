package net.lucerna.render.tracing.hybrid;

import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.lucerna.telemetry.NativePassTelemetryStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public record Round10HybridHitDebugStatus(
        boolean telemetryPresent,
        String summary,
        String wallHitCount,
        String openSkyMissCount,
        String glassWaterMaterialHitCount,
        String opaqueMaterialHitCount,
        String materialIdConsistency,
        String emptySectionSkipSafe,
        String maskBitsReady,
        String maskBitsSource,
        String traversalBackend,
        boolean realGpuTraversalExecuted,
        String srcStable,
        String srcStableReason,
        String chunkChurnMaterialConsistent,
        String entityMoveMaterialConsistent,
        String fallbackSourceReason,
        boolean realTracedLightingConsumed,
        String stressLine,
        String tracedLightingBoundaryLine,
        String sourceCountsLine,
        String priorityLine,
        String materialConsistencyLine,
        String fallbackLine,
        String readinessLine,
        String evidenceBoundaryLine
) {
    public Round10HybridHitDebugStatus {
        summary = clean(summary, "hybrid hit telemetry unavailable");
        wallHitCount = clean(wallHitCount, "unknown");
        openSkyMissCount = clean(openSkyMissCount, "unknown");
        glassWaterMaterialHitCount = clean(glassWaterMaterialHitCount, "unknown");
        opaqueMaterialHitCount = clean(opaqueMaterialHitCount, "unknown");
        materialIdConsistency = clean(materialIdConsistency, "unknown");
        emptySectionSkipSafe = clean(emptySectionSkipSafe, "false");
        maskBitsReady = clean(maskBitsReady, "false");
        maskBitsSource = clean(maskBitsSource, "unknown");
        traversalBackend = clean(traversalBackend, "unknown");
        srcStable = clean(srcStable, "unknown");
        srcStableReason = clean(srcStableReason, "awaiting stress telemetry");
        chunkChurnMaterialConsistent = clean(chunkChurnMaterialConsistent, "unknown");
        entityMoveMaterialConsistent = clean(entityMoveMaterialConsistent, "unknown");
        fallbackSourceReason = clean(fallbackSourceReason, "awaiting fallback-source telemetry");
        stressLine = clean(stressLine, "Hybrid stress: unavailable");
        tracedLightingBoundaryLine = clean(
                tracedLightingBoundaryLine,
                "Hybrid traced lighting boundary: realTracedLightingConsumed=false; open until native traced-light output is consumed"
        );
        sourceCountsLine = clean(sourceCountsLine, "Hybrid source counts: unavailable");
        priorityLine = clean(priorityLine, "Hybrid priority: unavailable");
        materialConsistencyLine = clean(materialConsistencyLine, "Hybrid material consistency: unavailable");
        fallbackLine = clean(fallbackLine, "Hybrid fallback: unavailable");
        readinessLine = clean(readinessLine, "Round 10 hybrid readiness: missing");
        evidenceBoundaryLine = clean(
                evidenceBoundaryLine,
                "Round 10 evidence boundary: hit resolver/status only; native tracing and screenshot proof are controller-owned"
        );
    }

    public Round10HybridHitDebugStatus(
            boolean telemetryPresent,
            String summary,
            String sourceCountsLine,
            String priorityLine,
            String materialConsistencyLine,
            String fallbackLine,
            String readinessLine,
            String evidenceBoundaryLine
    ) {
        this(
                telemetryPresent,
                summary,
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                "false",
                "false",
                "unknown",
                "unknown",
                false,
                "unknown",
                "awaiting stress telemetry",
                "unknown",
                "unknown",
                "awaiting fallback-source telemetry",
                false,
                "Hybrid stress: unavailable",
                "Hybrid traced lighting boundary: realTracedLightingConsumed=false; open until native traced-light output is consumed",
                sourceCountsLine,
                priorityLine,
                materialConsistencyLine,
                fallbackLine,
                readinessLine,
                evidenceBoundaryLine
        );
    }

    public static Round10HybridHitDebugStatus fromSnapshot(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus dispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus hybridStage = firstStage(
                dispatch,
                "hybrid_hit",
                "hybrid_hit_resolver",
                "hybrid_resolver",
                "round10_hybrid_hit"
        );
        LightingDispatchStageTelemetryStatus rtStage = firstStage(
                dispatch,
                "hardware_rt",
                "vulkan_rt",
                "ray_tracing",
                "round10_rt"
        );
        LightingDispatchStageTelemetryStatus voxelStage = firstStage(
                dispatch,
                "voxel_trace",
                "voxel_hit",
                "voxel_tracing",
                "voxel"
        );
        NativePassTelemetryStatus nativePasses = snapshot.nativePassStates();
        Map<String, String> nativeHybridDetails = parseNativeBlock(
                snapshot.nativeBridge().nativeStatus(),
                "round10_hybrid_hit={"
        );
        Map<String, String> nativeVoxelDetails = parseNativeBlock(
                snapshot.nativeBridge().nativeStatus(),
                "round10_voxel_traversal={"
        );

        String screenCount = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "screen_space_hits",
                "screen_hits",
                "screenspace_hits",
                "screen"
        );
        String voxelCount = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativePasses,
                "voxel_hits",
                "voxel_hit_count",
                "voxel"
        );
        String rtCount = firstValue(
                hybridStage,
                rtStage,
                nativeHybridDetails,
                nativePasses,
                "hardware_rt_hits",
                "rt_hits",
                "raytraced_hits",
                "hardware_rt"
        );
        String skyCount = firstValue(hybridStage, nativeHybridDetails, nativePasses, "sky_hits", "sky");
        String missCount = firstValue(hybridStage, nativeHybridDetails, nativePasses, "misses", "miss_count", "miss");
        String wallHitCount = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "wall_hit_count",
                "known_scene_wall_hit_count",
                "terrain_wall_hit_count"
        );
        String openSkyMissCount = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "open_sky_miss_count",
                "open_sky_misses",
                "sky_miss_count"
        );
        String glassWaterMaterialHitCount = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "glass_water_material_hit_count",
                "transparent_material_hit_count",
                "fluid_glass_hit_count"
        );
        String opaqueMaterialHitCount = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "opaque_material_hit_count",
                "opaque_hits",
                "solid_material_hit_count"
        );
        String selectedSource = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "selected_source",
                "priority_source",
                "winner",
                "resolved_source"
        );
        String materialConsistent = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "material_consistent",
                "material_match",
                "consistent_material"
        );
        String materialId = firstValue(hybridStage, nativeHybridDetails, nativePasses, "material_id", "material");
        String materialIdConsistency = firstNonBlank(
                materialConsistent,
                firstValue(
                        hybridStage,
                        voxelStage,
                        nativeHybridDetails,
                        nativeVoxelDetails,
                        nativePasses,
                        "material_id_consistency",
                        "material_lookup_ready",
                        "material_consistency"
                )
        );
        String expectedMaterialId = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "expected_material_id",
                "expected_material"
        );
        String fallbackActive = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "fallback_active",
                "fallback",
                "using_fallback"
        );
        String voxelAvailable = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativePasses,
                "voxel_available",
                "voxel_path_available",
                "voxel_ready"
        );
        String rtAvailable = firstValue(
                hybridStage,
                rtStage,
                nativeHybridDetails,
                nativePasses,
                "hardware_rt_available",
                "rt_available",
                "rt_ready",
                "hardware_rt_ready"
        );
        String fallbackReason = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "fallback_reason",
                "reason",
                "readiness_reason"
        );
        String emptySectionSkipSafe = emptySectionSkipSafetyValue(
                firstValue(
                        hybridStage,
                        voxelStage,
                        nativeHybridDetails,
                        nativeVoxelDetails,
                        nativePasses,
                        "empty_section_skip_safe",
                        "empty_section_skip_safety_count",
                        "empty_section_skips"
                )
        );
        String maskBitsReady = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "mask_bits_ready",
                "occupancy_mask_bits_ready",
                "real_mask_bits_ready"
        );
        String maskBitsSource = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "mask_bits_source",
                "mask_bit_source",
                "occupancy_mask_source"
        );
        String traversalBackend = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "traversal_backend",
                "backend",
                "voxel_traversal_backend"
        );
        String realGpuTraversalExecutedValue = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "real_gpu_traversal_executed",
                "gpu_traversal_executed",
                "real_gpu_voxel_traversal"
        );
        boolean realGpuTraversalExecuted = truthy(realGpuTraversalExecutedValue);
        String srcStable = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "src_stable",
                "source_stable",
                "selected_source_stable",
                "selected_source_stability"
        );
        String srcStableReason = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "src_stable_reason",
                "source_stability_reason",
                "selected_source_stability_reason",
                "selected_source_churn_reason"
        );
        String chunkChurnMaterialConsistent = firstValue(
                hybridStage,
                voxelStage,
                nativeHybridDetails,
                nativeVoxelDetails,
                nativePasses,
                "chunk_churn_material_consistent",
                "material_consistent_during_chunk_churn",
                "chunk_churn_material_match",
                "chunk_churn_material_stable"
        );
        String entityMoveMaterialConsistent = firstValue(
                hybridStage,
                rtStage,
                nativeHybridDetails,
                nativePasses,
                "entity_move_material_consistent",
                "material_consistent_during_entity_movement",
                "entity_movement_material_match",
                "entity_move_material_stable"
        );
        String fallbackSourceReason = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "fallback_source_reason",
                "fallback_src_reason",
                "fallback_selected_source_reason",
                "source_fallback_reason"
        );
        String realTracedLightingConsumedValue = firstValue(
                hybridStage,
                nativeHybridDetails,
                nativePasses,
                "real_traced_lighting_consumed",
                "traced_lighting_consumed",
                "real_lighting_consumed",
                "rt_lighting_consumed"
        );
        boolean realTracedLightingConsumed = truthy(realTracedLightingConsumedValue);

        boolean hasTelemetry = hasAny(
                screenCount,
                voxelCount,
                rtCount,
                skyCount,
                missCount,
                selectedSource,
                materialConsistent,
                wallHitCount,
                openSkyMissCount,
                glassWaterMaterialHitCount,
                opaqueMaterialHitCount,
                fallbackActive,
                fallbackReason
        );
        String fallbackSourceReasonResolved = firstNonBlank(fallbackSourceReason, fallbackReason);
        String summary = "selected=" + valueOrUnknown(selectedSource)
                + ",screen=" + valueOrUnknown(screenCount)
                + ",voxel=" + valueOrUnknown(voxelCount)
                + ",rt=" + valueOrUnknown(rtCount)
                + ",sky=" + valueOrUnknown(skyCount)
                + ",miss=" + valueOrUnknown(missCount)
                + ",wallHitCount=" + valueOrUnknown(wallHitCount)
                + ",openSkyMissCount=" + valueOrUnknown(openSkyMissCount)
                + ",materialConsistent=" + valueOrUnknown(materialConsistent)
                + ",materialIdConsistency=" + valueOrUnknown(materialIdConsistency)
                + ",traversalBackend=" + valueOrUnknown(traversalBackend)
                + ",realGpuTraversalExecuted=" + yesNo(realGpuTraversalExecuted)
                + ",srcStable=" + valueOrUnknown(srcStable)
                + ",chunkChurnMaterialConsistent=" + valueOrUnknown(chunkChurnMaterialConsistent)
                + ",entityMoveMaterialConsistent=" + valueOrUnknown(entityMoveMaterialConsistent)
                + ",realTracedLightingConsumed=" + yesNo(realTracedLightingConsumed)
                + ",fallback=" + valueOrUnknown(fallbackActive);
        String countsLine = "Hybrid source counts: screen=" + valueOrUnknown(screenCount)
                + " voxel=" + valueOrUnknown(voxelCount)
                + " hardwareRt=" + valueOrUnknown(rtCount)
                + " sky=" + valueOrUnknown(skyCount)
                + " miss=" + valueOrUnknown(missCount)
                + " wallHitCount=" + valueOrUnknown(wallHitCount)
                + " openSkyMissCount=" + valueOrUnknown(openSkyMissCount);
        String priorityLine = "Hybrid priority: selected=" + valueOrUnknown(selectedSource)
                + " rule=hardwareRt>voxel>screenSpace>sky>miss"
                + " fallback=" + valueOrUnknown(fallbackActive);
        String materialLine = "Hybrid material consistency: consistent=" + valueOrUnknown(materialConsistent)
                + " material=" + valueOrUnknown(materialId)
                + " expected=" + valueOrUnknown(expectedMaterialId)
                + " materialIdConsistency=" + valueOrUnknown(materialIdConsistency)
                + " glassWaterMaterialHitCount=" + valueOrUnknown(glassWaterMaterialHitCount)
                + " opaqueMaterialHitCount=" + valueOrUnknown(opaqueMaterialHitCount);
        String fallbackLine = "Hybrid fallback: active=" + valueOrUnknown(fallbackActive)
                + " voxelAvailable=" + valueOrUnknown(voxelAvailable)
                + " hardwareRtAvailable=" + valueOrUnknown(rtAvailable)
                + " reason=" + valueOrUnknown(fallbackReason, "awaiting native/controller telemetry");
        String stressLine = "Hybrid stress: srcStable=" + valueOrUnknown(srcStable)
                + " srcReason=" + valueOrUnknown(srcStableReason, "awaiting stress telemetry")
                + " chunkChurnMaterial=" + valueOrUnknown(chunkChurnMaterialConsistent)
                + " entityMoveMaterial=" + valueOrUnknown(entityMoveMaterialConsistent)
                + " fallbackSourceReason=" + valueOrUnknown(
                        fallbackSourceReasonResolved,
                        "awaiting fallback-source telemetry"
                );
        String tracedLightingBoundaryLine = realTracedLightingConsumed
                ? "Hybrid traced lighting boundary: realTracedLightingConsumed=true from telemetry; controller proof still required"
                : "Hybrid traced lighting boundary: realTracedLightingConsumed=false; open until native traced-light output is consumed";
        String readinessLine = "Round 10 hybrid readiness: telemetry=" + yesNo(hasTelemetry)
                + " sources=" + readinessFrom(screenCount, voxelCount, rtCount, skyCount, missCount)
                + " priority=" + readinessFrom(selectedSource)
                + " material=" + readinessFrom(materialConsistent, materialId, expectedMaterialId)
                + " terrainMaterial=" + readinessFrom(
                        wallHitCount,
                        openSkyMissCount,
                        glassWaterMaterialHitCount,
                        opaqueMaterialHitCount,
                        materialIdConsistency
                )
                + " maskBitsReady=" + valueOrUnknown(maskBitsReady, "false")
                + " maskBitsSource=" + valueOrUnknown(maskBitsSource)
                + " traversalBackend=" + valueOrUnknown(traversalBackend)
                + " realGpuTraversalExecuted=" + yesNo(realGpuTraversalExecuted)
                + " emptySectionSkipSafe=" + valueOrUnknown(emptySectionSkipSafe, "false")
                + " stress=" + readinessFrom(
                        srcStable,
                        srcStableReason,
                        chunkChurnMaterialConsistent,
                        entityMoveMaterialConsistent,
                        fallbackSourceReasonResolved
                )
                + " realTracedLightingConsumed=" + yesNo(realTracedLightingConsumed)
                + " fallback=" + readinessFrom(fallbackActive, voxelAvailable, rtAvailable, fallbackReason);
        String boundaryLine = realGpuTraversalExecuted
                ? "Round 10 evidence boundary: Java/status resolver reports GPU traversal execution from native telemetry; controller proof remains required"
                : "Round 10 evidence boundary: Java/status resolver only; realGpuTraversalExecuted=false, voxel traversal remains CPU/status boundary unless native telemetry proves otherwise";
        if (!realTracedLightingConsumed) {
            boundaryLine += "; realTracedLightingConsumed=false, traced lighting consumption remains open";
        }

        return new Round10HybridHitDebugStatus(
                hasTelemetry,
                summary,
                wallHitCount,
                openSkyMissCount,
                glassWaterMaterialHitCount,
                opaqueMaterialHitCount,
                materialIdConsistency,
                emptySectionSkipSafe,
                maskBitsReady,
                maskBitsSource,
                traversalBackend,
                realGpuTraversalExecuted,
                srcStable,
                srcStableReason,
                chunkChurnMaterialConsistent,
                entityMoveMaterialConsistent,
                fallbackSourceReasonResolved,
                realTracedLightingConsumed,
                stressLine,
                tracedLightingBoundaryLine,
                countsLine,
                priorityLine,
                materialLine,
                fallbackLine,
                readinessLine,
                boundaryLine
        );
    }

    private static LightingDispatchStageTelemetryStatus firstStage(
            LightingDispatchTelemetryStatus dispatch,
            String... stageIds
    ) {
        if (dispatch == null || stageIds == null) {
            return null;
        }
        for (String stageId : stageIds) {
            LightingDispatchStageTelemetryStatus stage = dispatch.stages().get(stageId);
            if (stage != null) {
                return stage;
            }
        }
        return null;
    }

    private static String firstValue(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            Map<String, String> nativeDetails,
            NativePassTelemetryStatus nativePasses,
            String... keys
    ) {
        String value = firstValue(primary, nativeDetails, nativePasses, keys);
        if (!value.isBlank()) {
            return value;
        }
        return firstValue(secondary, nativeDetails, nativePasses, keys);
    }

    private static String firstValue(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            Map<String, String> primaryNativeDetails,
            Map<String, String> secondaryNativeDetails,
            NativePassTelemetryStatus nativePasses,
            String... keys
    ) {
        String value = firstValue(primary, primaryNativeDetails, nativePasses, keys);
        if (!value.isBlank()) {
            return value;
        }
        value = firstValue(secondary, secondaryNativeDetails, nativePasses, keys);
        if (!value.isBlank()) {
            return value;
        }
        return firstMapDetail(primaryNativeDetails, keys);
    }

    private static String firstValue(
            LightingDispatchStageTelemetryStatus stage,
            Map<String, String> nativeDetails,
            NativePassTelemetryStatus nativePasses,
            String... keys
    ) {
        String value = firstDetail(stage, keys);
        if (!value.isBlank()) {
            return value;
        }
        value = firstMapDetail(nativeDetails, keys);
        if (!value.isBlank()) {
            return value;
        }
        return firstNativeDetail(nativePasses, keys);
    }

    private static String firstDetail(LightingDispatchStageTelemetryStatus stage, String... keys) {
        if (stage == null || keys == null) {
            return "";
        }
        Map<String, String> details = stage.details();
        for (String key : keys) {
            String value = details.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String firstMapDetail(Map<String, String> details, String... keys) {
        if (details == null || details.isEmpty() || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = details.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String firstNativeDetail(NativePassTelemetryStatus nativePasses, String... keys) {
        if (nativePasses == null || keys == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : nativePasses.passDetails().entrySet()) {
            String normalizedKey = normalize(entry.getKey());
            for (String key : keys) {
                if (normalizedKey.endsWith("." + normalize(key)) || normalizedKey.equals(normalize(key))) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    private static Map<String, String> parseNativeBlock(String nativeStatus, String marker) {
        if (nativeStatus == null || nativeStatus.isBlank() || marker == null || marker.isBlank()) {
            return Map.of();
        }
        int start = nativeStatus.indexOf(marker);
        if (start < 0) {
            return Map.of();
        }
        int blockStart = start + marker.length();
        int blockEnd = nativeStatus.indexOf('}', blockStart);
        if (blockEnd <= blockStart) {
            return Map.of();
        }
        Map<String, String> details = new LinkedHashMap<>();
        String block = nativeStatus.substring(blockStart, blockEnd);
        for (String segment : block.split(",")) {
            int delimiter = segment.indexOf('=');
            if (delimiter <= 0) {
                continue;
            }
            String key = segment.substring(0, delimiter).trim();
            String value = segment.substring(delimiter + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                details.putIfAbsent(key, value.replace("\"", ""));
            }
        }
        return Map.copyOf(details);
    }

    private static String readinessFrom(String... values) {
        return hasAny(values) ? "ready" : "missing";
    }

    private static boolean hasAny(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static String emptySectionSkipSafetyValue(String value) {
        if (value == null || value.isBlank()) {
            return "false";
        }
        if (truthy(value)) {
            return "true";
        }
        try {
            return Long.parseLong(value.trim()) >= 0L ? "true" : "false";
        } catch (NumberFormatException ignored) {
            return value.trim();
        }
    }

    private static boolean truthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        return "true".equals(normalized)
                || "yes".equals(normalized)
                || "1".equals(normalized)
                || "ready".equals(normalized)
                || "executed".equals(normalized);
    }

    private static String valueOrUnknown(String value) {
        return valueOrUnknown(value, "?");
    }

    private static String valueOrUnknown(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
