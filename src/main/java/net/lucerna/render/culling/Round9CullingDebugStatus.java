package net.lucerna.render.culling;

import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.lucerna.telemetry.NativePassTelemetryStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public record Round9CullingDebugStatus(
        boolean telemetryPresent,
        String summary,
        String clusterMetadataLine,
        String visibilityCountsLine,
        String cullingLine,
        String indirectDrawLine,
        String uploadLine,
        String generationLine,
        String readinessLine,
        String gpuCullingReadinessLine,
        String frustumCandidateLine,
        String occlusionReadinessLine,
        String executionBoundaryLine,
        String evidenceBoundaryLine
) {
    public Round9CullingDebugStatus {
        summary = clean(summary, "round9 culling telemetry unavailable");
        clusterMetadataLine = clean(clusterMetadataLine, "Cluster metadata: unavailable");
        visibilityCountsLine = clean(visibilityCountsLine, "Visibility counts: unavailable");
        cullingLine = clean(cullingLine, "Culling: unavailable");
        indirectDrawLine = clean(indirectDrawLine, "Indirect draw list: unavailable");
        uploadLine = clean(uploadLine, "Cluster upload: unavailable");
        generationLine = clean(generationLine, "Cluster generation: unavailable");
        readinessLine = clean(readinessLine, "Round 9 readiness: missing");
        gpuCullingReadinessLine = clean(gpuCullingReadinessLine, "Round 9 GPU culling readiness: missing");
        frustumCandidateLine = clean(frustumCandidateLine, "Round 9 frustum candidates: unavailable");
        occlusionReadinessLine = clean(occlusionReadinessLine, "Round 9 occlusion readiness: unavailable");
        executionBoundaryLine = clean(executionBoundaryLine, "Round 9 execution boundary: actualGpuExecuted=no");
        evidenceBoundaryLine = clean(
                evidenceBoundaryLine,
                "Round 9 evidence boundary: metadata/status only; controller owns visual no-corruption proof"
        );
    }

    public static Round9CullingDebugStatus fromSnapshot(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus dispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus cullingStage = firstStage(
                dispatch,
                "chunk_culling",
                "cluster_culling",
                "gpu_culling",
                "round9_culling",
                "visibility_culling"
        );
        LightingDispatchStageTelemetryStatus clusterStage = firstStage(
                dispatch,
                "chunk_clusters",
                "cluster_metadata",
                "virtualized_chunk_geometry",
                "round9_clusters"
        );
        NativePassTelemetryStatus nativePasses = snapshot.nativePassStates();
        boolean nativePassTelemetryPresent = nativePasses != null && nativePasses.hasPassStates();
        Map<String, String> nativeRound9Details = parseRound9NativeDetails(snapshot.nativeBridge().nativeStatus());

        String clusterCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "cluster_count",
                "clusters",
                "chunk_clusters",
                "meshlet_count"
        );
        String sectionCount = firstValue(
                clusterStage,
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "payload_sections",
                "section_count",
                "sections",
                "cluster_sections",
                "chunk_section_count"
        );
        String visibleCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "visible_cluster_count",
                "visible_clusters",
                "visible",
                "visibility_visible"
        );
        String culledCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "culled_cluster_count",
                "culled_clusters",
                "culled",
                "hidden_clusters"
        );
        String offscreenCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "offscreen_cluster_count",
                "offscreen_clusters",
                "offscreen"
        );
        String frustumCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "frustum_culled_count",
                "frustum_culled",
                "frustum"
        );
        String frustumCandidateCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "frustum_candidate_count",
                "frustum_candidates",
                "candidate_clusters",
                "culling_candidates"
        );
        String occlusionPlaceholderCount = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "occlusion_placeholder_count",
                "occlusion_placeholders",
                "occlusion_placeholder",
                "occlusion_culled"
        );
        String occlusionReady = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "occlusion_ready",
                "occlusion_culling_ready",
                "occlusion_query_ready",
                "hiz_ready"
        );
        String drawCount = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "indirect_draw_count",
                "indirect_draw_count_placeholder",
                "draw_command_count",
                "draws",
                "indirect_draws"
        );
        String indirectReady = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "indirect_draw_ready",
                "indirect_ready",
                "draw_list_ready",
                "indirect_dispatch_ready"
        );
        String indirectBytes = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "indirect_draw_bytes",
                "indirect_bytes",
                "draw_list_bytes",
                "command_bytes"
        );
        String uploadBytes = firstValue(
                clusterStage,
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "upload_byte_estimate",
                "cluster_upload_bytes",
                "upload_bytes",
                "visibility_upload_bytes",
                "metadata_upload_bytes"
        );
        String generation = firstValue(
                cullingStage,
                clusterStage,
                nativeRound9Details,
                nativePasses,
                "generation_counter",
                "generation",
                "last_generation",
                "cluster_generation",
                "visibility_generation"
        );
        String generationRange = firstValue(
                clusterStage,
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "generation_range",
                "cluster_generation_range",
                "visibility_generation_range"
        );
        String mode = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "mode",
                "culling_mode",
                "visibility_mode"
        );
        String cullingReady = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "ready",
                "culling_ready",
                "visibility_ready"
        );
        String actualGpuExecuted = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "actual_gpu_culling_executed",
                "gpu_culling_executed",
                "executed_on_gpu",
                "gpu_execution"
        );
        String gpuPrerequisitesReady = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "gpu_culling_prerequisites_ready",
                "gpu_prerequisites_ready",
                "prerequisites_ready"
        );
        String missingPrerequisites = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "gpu_culling_missing_prerequisites",
                "missing_prerequisites",
                "missing_prereqs"
        );
        String blockerReason = firstValue(
                cullingStage,
                nativeRound9Details,
                nativePasses,
                "gpu_culling_blocker_reason",
                "blocker_reason",
                "culling_blocker"
        );

        boolean hasTelemetry = (dispatch != null && dispatch.hasLightingDispatchStatus())
                || hasAny(clusterCount, visibleCount, culledCount, drawCount, uploadBytes, generation)
                || nativePassTelemetryPresent
                || !nativeRound9Details.isEmpty();
        String actualGpuExecutedLabel = yesNo(truthy(actualGpuExecuted));
        String prerequisitesReadyLabel = yesNo(truthy(gpuPrerequisitesReady));
        String occlusionReadyLabel = yesNo(truthy(occlusionReady));
        String indirectReadyLabel = yesNo(truthy(indirectReady) && truthy(actualGpuExecuted));
        String missingPrerequisitesLabel = tokenOrFallback(
                missingPrerequisites,
                truthy(actualGpuExecuted) ? "none" : "gpu-dispatch-visibility-buffer-occlusion-query"
        );
        String blockerReasonLabel = tokenOrFallback(
                blockerReason,
                truthy(actualGpuExecuted) ? "none" : "actual-gpu-culling-not-proven"
        );
        String modeLabel = truthy(actualGpuExecuted) ? "actual-gpu-culling" : "conservative-cpu-status";
        String summary = "clusters=" + valueOrUnknown(clusterCount)
                + ",visible=" + valueOrUnknown(visibleCount)
                + ",culled=" + valueOrUnknown(culledCount)
                + ",offscreen=" + valueOrUnknown(offscreenCount)
                + ",gpuExecuted=" + actualGpuExecutedLabel
                + ",gpuPrereqsReady=" + prerequisitesReadyLabel
                + ",frustumCandidates=" + valueOrUnknown(frustumCandidateCount)
                + ",occlusionPlaceholder=" + valueOrUnknown(occlusionPlaceholderCount)
                + ",draws=" + valueOrUnknown(drawCount)
                + ",uploadBytes=" + valueOrUnknown(uploadBytes)
                + ",gen=" + valueOrUnknown(generation);
        String clusterLine = "Cluster metadata: clusters=" + valueOrUnknown(clusterCount)
                + " sections=" + valueOrUnknown(sectionCount)
                + " source=" + sourceLabel(clusterStage, cullingStage);
        String visibilityLine = "Visibility counts: visible=" + valueOrUnknown(visibleCount)
                + " culled=" + valueOrUnknown(culledCount)
                + " offscreen=" + valueOrUnknown(offscreenCount)
                + " frustumCandidates=" + valueOrUnknown(frustumCandidateCount)
                + " occlusionPlaceholder=" + valueOrUnknown(occlusionPlaceholderCount);
        String cullingLine = "Culling: actualGpuExecuted=" + actualGpuExecutedLabel
                + " gpuPrereqsReady=" + prerequisitesReadyLabel
                + " missingPrereqs=" + missingPrerequisitesLabel
                + " blocker=" + blockerReasonLabel
                + " frustumCandidates=" + valueOrUnknown(frustumCandidateCount)
                + " frustum=" + valueOrUnknown(frustumCount)
                + " occlusionReady=" + occlusionReadyLabel
                + " occlusionPlaceholder=" + valueOrUnknown(occlusionPlaceholderCount)
                + " mode=" + valueOrUnknown(mode, "metadata-only-placeholder")
                + " modeLabel=" + modeLabel
                + " terrainRenderingChanged=no";
        String indirectLine = "Indirect draw list: draws=" + valueOrUnknown(drawCount)
                + " visibleClusters=" + valueOrUnknown(visibleCount)
                + " bytes=" + valueOrUnknown(indirectBytes)
                + " ready=" + indirectReadyLabel
                + " metadataOnly=yes";
        String uploadLine = "Cluster upload: bytes=" + valueOrUnknown(uploadBytes)
                + " uploadWorldGen=" + snapshot.uploadWorldGeneration()
                + " sectionGen=" + snapshot.uploadSectionGeneration();
        String generationLine = "Cluster generation: gen=" + valueOrUnknown(generation)
                + " range=" + valueOrUnknown(generationRange)
                + " worldGen=" + snapshot.worldGeneration();
        String readinessLine = "Round 9 readiness: telemetry=" + yesNo(hasTelemetry)
                + " clusters=" + readinessFrom(clusterCount, sectionCount)
                + " counts=" + readinessFrom(visibleCount, culledCount, offscreenCount, occlusionPlaceholderCount)
                + " indirect=" + readinessFrom(drawCount, indirectBytes)
                + " upload=" + readinessFrom(uploadBytes)
                + " gpuPrereqs=" + prerequisitesReadyLabel
                + " gpuExecuted=" + actualGpuExecutedLabel
                + " blocker=" + blockerReasonLabel
                + " ready=" + valueOrUnknown(cullingReady);
        String gpuReadinessLine = "Round 9 GPU culling readiness: actualGpuExecuted=" + actualGpuExecutedLabel
                + " prerequisitesReady=" + prerequisitesReadyLabel
                + " missingPrereqs=" + missingPrerequisitesLabel
                + " blocker=" + blockerReasonLabel
                + " modeLabel=" + modeLabel;
        String frustumLine = "Round 9 frustum candidates: candidates=" + valueOrUnknown(frustumCandidateCount)
                + " culled=" + valueOrUnknown(frustumCount)
                + " visible=" + valueOrUnknown(visibleCount)
                + " offscreen=" + valueOrUnknown(offscreenCount);
        String occlusionLine = "Round 9 occlusion readiness: ready=" + occlusionReadyLabel
                + " placeholders=" + valueOrUnknown(occlusionPlaceholderCount)
                + " blocker=" + (truthy(occlusionReady) ? "none" : "occlusion-query-or-hiz-not-proven");
        String executionLine = "Round 9 execution boundary: actualGpuExecuted=" + actualGpuExecutedLabel
                + " indirectReady=" + indirectReadyLabel
                + " modeLabel=" + modeLabel
                + " conservativeCpuFallback=" + yesNo(!truthy(actualGpuExecuted));
        String boundaryLine = "Round 9 evidence boundary: culling/status only; no rendering hooks or terrain visibility changes";

        return new Round9CullingDebugStatus(
                hasTelemetry,
                summary,
                clusterLine,
                visibilityLine,
                cullingLine,
                indirectLine,
                uploadLine,
                generationLine,
                readinessLine,
                gpuReadinessLine,
                frustumLine,
                occlusionLine,
                executionLine,
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
            Map<String, String> nativeRound9Details,
            NativePassTelemetryStatus nativePasses,
            String... keys
    ) {
        String value = firstDetail(primary, keys);
        if (!value.isBlank()) {
            return value;
        }
        value = firstDetail(secondary, keys);
        if (!value.isBlank()) {
            return value;
        }
        value = firstMapDetail(nativeRound9Details, keys);
        if (!value.isBlank()) {
            return value;
        }
        return firstNativeDetail(nativePasses, keys);
    }

    private static String firstValue(
            LightingDispatchStageTelemetryStatus primary,
            Map<String, String> nativeRound9Details,
            NativePassTelemetryStatus nativePasses,
            String... keys
    ) {
        String value = firstDetail(primary, keys);
        if (!value.isBlank()) {
            return value;
        }
        value = firstMapDetail(nativeRound9Details, keys);
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

    private static Map<String, String> parseRound9NativeDetails(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return Map.of();
        }
        int start = nativeStatus.indexOf("round9_virtual_geometry={");
        if (start < 0) {
            return Map.of();
        }
        int blockStart = start + "round9_virtual_geometry={".length();
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

    private static String sourceLabel(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary
    ) {
        if (primary != null) {
            return primary.stageId();
        }
        if (secondary != null) {
            return secondary.stageId();
        }
        return "awaiting Agent Y/controller telemetry";
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

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String valueOrUnknown(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String tokenOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replace(' ', '-');
    }

    private static boolean truthy(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        return "true".equals(normalized)
                || "yes".equals(normalized)
                || "ready".equals(normalized)
                || "1".equals(normalized)
                || "executed".equals(normalized);
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
