package net.lucerna.render.preview;

import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;

import java.util.Map;

public record Round8AdaptiveDebugStatus(
        boolean telemetryPresent,
        String summary,
        String adaptiveSamplingLine,
        String rayBudgetLine,
        String rayBudgetHeatmapLine,
        String varianceMapLine,
        String historyConfidenceLine,
        String disocclusionMaskLine,
        String readinessLine,
        String evidenceBoundaryLine
) {
    public Round8AdaptiveDebugStatus {
        if (summary == null || summary.isBlank()) {
            summary = "adaptive debug telemetry unavailable";
        }
        adaptiveSamplingLine = cleanLine(adaptiveSamplingLine);
        rayBudgetLine = cleanLine(rayBudgetLine);
        rayBudgetHeatmapLine = cleanLine(rayBudgetHeatmapLine);
        varianceMapLine = cleanLine(varianceMapLine);
        historyConfidenceLine = cleanLine(historyConfidenceLine);
        disocclusionMaskLine = cleanLine(disocclusionMaskLine);
        readinessLine = cleanLine(readinessLine);
        evidenceBoundaryLine = cleanLine(evidenceBoundaryLine);
    }

    public static Round8AdaptiveDebugStatus fromSnapshot(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus dispatch = snapshot.lightingDispatchStatus();
        boolean hasTelemetry = dispatch.hasLightingDispatchStatus();
        LightingDispatchStageTelemetryStatus giStage = firstStage(
                dispatch,
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        );
        LightingDispatchStageTelemetryStatus cacheStage = firstStage(
                dispatch,
                "cache",
                "radiance_cache",
                "sparse_radiance_cache",
                "sparse_voxel_radiance_cache"
        );
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                dispatch,
                "denoise",
                "post_processing",
                "post_process",
                "history_rejection",
                "rejection"
        );

        String adaptiveEnabled = firstDetail(
                giStage,
                cacheStage,
                "adaptive_ray_budget_enabled",
                "adaptive_budget_enabled",
                "adaptiveRayBudgetEnabled",
                "adaptive_sampling_enabled",
                "adaptive_enabled"
        );
        String tier = firstDetail(
                giStage,
                cacheStage,
                "ray_budget_tier_name",
                "ray_budget_tier",
                "budget_tier",
                "tier"
        );
        String raysPerCell = firstDetail(
                giStage,
                cacheStage,
                "rays_per_cell",
                "ray_budget_rays_per_cell",
                "raysPerCell"
        );
        String requestedRays = firstDetail(
                giStage,
                cacheStage,
                "requested_rays",
                "ray_budget_requested_rays",
                "requestedRays"
        );
        String cappedRays = firstDetail(
                giStage,
                cacheStage,
                "capped_rays",
                "ray_budget_capped_rays",
                "cappedRays"
        );
        String cells = firstDetail(
                giStage,
                cacheStage,
                "low_res_cells",
                "low_resolution_cell_count",
                "ray_budget_low_res_cell_count",
                "grid_cells",
                "cell_count"
        );
        String confidence = firstDetail(
                cacheStage,
                giStage,
                "confidence",
                "avg_confidence",
                "average_confidence",
                "cache_confidence",
                "history_confidence"
        );
        String variance = firstDetail(
                cacheStage,
                giStage,
                "variance",
                "max_variance",
                "cache_variance",
                "confidence_variance"
        );
        String sampleCount = firstDetail(
                cacheStage,
                giStage,
                "sample_count",
                "samples",
                "cache_samples",
                "confidence_samples"
        );
        String historyConfidence = firstDetail(
                denoiseStage,
                giStage,
                cacheStage,
                "history_confidence",
                "history_confidence_floor",
                "min_history_confidence",
                "temporal_confidence_floor",
                "temporal_history_confidence"
        );
        String historyAvailable = firstDetail(
                denoiseStage,
                giStage,
                "history_confidence_available",
                "history_inputs_complete",
                "temporal_reuse_allowed",
                "history_available"
        );
        String disocclusionPixels = firstDetail(
                denoiseStage,
                giStage,
                "disocclusion_pixels",
                "disoccluded_pixels",
                "disocclusion_count"
        );
        String disocclusionMaskReady = firstDetail(
                denoiseStage,
                giStage,
                "disocclusion_mask_ready",
                "writes_rejection_mask",
                "rejection_mask_ready",
                "motion_history_available"
        );

        String summary = "adaptive=" + valueOrUnknown(adaptiveEnabled)
                + ",tier=" + valueOrUnknown(tier)
                + ",confidence=" + valueOrUnknown(confidence)
                + ",variance=" + valueOrUnknown(variance)
                + ",history=" + valueOrUnknown(historyAvailable)
                + ",disocclusion=" + valueOrUnknown(disocclusionMaskReady);
        String adaptiveLine = "Adaptive sampling: enabled=" + valueOrUnknown(adaptiveEnabled)
                + " tier=" + valueOrUnknown(tier)
                + " reason=" + readinessReason(giStage, cacheStage);
        String budgetLine = "Ray budget: tier=" + valueOrUnknown(tier)
                + " raysPerCell=" + valueOrUnknown(raysPerCell)
                + " requested=" + valueOrUnknown(requestedRays)
                + " capped=" + valueOrUnknown(cappedRays)
                + " cells=" + valueOrUnknown(cells);
        String heatmapLine = "Ray-budget heatmap: source=GI budget tier"
                + " mode=" + heatmapMode(tier)
                + " ready=" + readinessFrom(tier, requestedRays, cappedRays);
        String varianceLine = "Variance map: variance=" + valueOrUnknown(variance)
                + " confidence=" + valueOrUnknown(confidence)
                + " samples=" + valueOrUnknown(sampleCount)
                + " ready=" + readinessFrom(variance, confidence);
        String historyLine = "History confidence: available=" + valueOrUnknown(historyAvailable)
                + " value=" + valueOrUnknown(historyConfidence)
                + " frame=" + valueOrUnknown(stageFrame(denoiseStage, giStage));
        String disocclusionLine = "Disocclusion mask: ready=" + valueOrUnknown(disocclusionMaskReady)
                + " pixels=" + valueOrUnknown(disocclusionPixels)
                + " source=history rejection counters";
        String readinessLine = "Round 8 readiness: telemetry=" + yesNo(hasTelemetry)
                + " budget=" + readinessFrom(tier, requestedRays, cappedRays)
                + " variance=" + readinessFrom(variance, confidence)
                + " history=" + readinessFrom(historyAvailable, historyConfidence)
                + " disocclusion=" + readinessFrom(disocclusionMaskReady, disocclusionPixels);
        String boundaryLine = "Round 8 evidence boundary: overlay/status only; controller must prove real heatmap draw and work redistribution";

        return new Round8AdaptiveDebugStatus(
                hasTelemetry,
                summary,
                adaptiveLine,
                budgetLine,
                heatmapLine,
                varianceLine,
                historyLine,
                disocclusionLine,
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

    private static String firstDetail(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            String... keys
    ) {
        String value = firstDetail(primary, keys);
        if (!value.isBlank()) {
            return value;
        }
        return firstDetail(secondary, keys);
    }

    private static String firstDetail(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary,
            LightingDispatchStageTelemetryStatus tertiary,
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
        return firstDetail(tertiary, keys);
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

    private static String readinessReason(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary
    ) {
        if (primary != null && !primary.readinessReason().isBlank()) {
            return primary.readinessReason();
        }
        if (secondary != null && !secondary.readinessReason().isBlank()) {
            return secondary.readinessReason();
        }
        return "unreported";
    }

    private static String stageFrame(
            LightingDispatchStageTelemetryStatus primary,
            LightingDispatchStageTelemetryStatus secondary
    ) {
        if (primary != null && primary.frameIndex() != null) {
            return Long.toString(primary.frameIndex());
        }
        if (secondary != null && secondary.frameIndex() != null) {
            return Long.toString(secondary.frameIndex());
        }
        return "";
    }

    private static String heatmapMode(String tier) {
        if (tier == null || tier.isBlank()) {
            return "awaiting-budget";
        }
        return switch (tier.toLowerCase(java.util.Locale.ROOT)) {
            case "reuse_only", "reuse-only", "reuse" -> "reuse-only";
            case "low" -> "low-ray";
            case "medium" -> "medium-ray";
            case "high" -> "high-ray";
            default -> "tier-" + tier;
        };
    }

    private static String readinessFrom(String primary, String secondary) {
        return (!valueOrUnknown(primary).equals("?") || !valueOrUnknown(secondary).equals("?")) ? "ready" : "missing";
    }

    private static String readinessFrom(String primary, String secondary, String tertiary) {
        return (!valueOrUnknown(primary).equals("?")
                || !valueOrUnknown(secondary).equals("?")
                || !valueOrUnknown(tertiary).equals("?")) ? "ready" : "missing";
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String cleanLine(String value) {
        return value == null || value.isBlank() ? "unreported" : value.trim();
    }
}
