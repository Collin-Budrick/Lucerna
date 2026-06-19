package net.lucerna.render.preview;

import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;

import java.util.Map;

public record Round8AdaptiveDebugStatus(
        boolean telemetryPresent,
        String summary,
        String adaptiveSamplingLine,
        String sceneStateLine,
        String rayBudgetLine,
        String rayBudgetBucketLine,
        String rayBudgetHeatmapLine,
        String varianceMapLine,
        String historyConfidenceLine,
        String historyConfidenceHeatmapLine,
        String historyCountsLine,
        String disocclusionMaskLine,
        String cacheConfidenceContributionLine,
        String heatmapRolesLine,
        String readinessLine,
        String evidenceBoundaryLine
) {
    public Round8AdaptiveDebugStatus {
        if (summary == null || summary.isBlank()) {
            summary = "adaptive debug telemetry unavailable";
        }
        adaptiveSamplingLine = cleanLine(adaptiveSamplingLine);
        sceneStateLine = cleanLine(sceneStateLine);
        rayBudgetLine = cleanLine(rayBudgetLine);
        rayBudgetBucketLine = cleanLine(rayBudgetBucketLine);
        rayBudgetHeatmapLine = cleanLine(rayBudgetHeatmapLine);
        varianceMapLine = cleanLine(varianceMapLine);
        historyConfidenceLine = cleanLine(historyConfidenceLine);
        historyConfidenceHeatmapLine = cleanLine(historyConfidenceHeatmapLine);
        historyCountsLine = cleanLine(historyCountsLine);
        disocclusionMaskLine = cleanLine(disocclusionMaskLine);
        cacheConfidenceContributionLine = cleanLine(cacheConfidenceContributionLine);
        heatmapRolesLine = cleanLine(heatmapRolesLine);
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
        String sceneState = firstDetail(
                giStage,
                cacheStage,
                denoiseStage,
                "scene_state",
                "scene",
                "round8_scene_state",
                "capture_scene_state",
                "lucerna_round8_scene_state"
        );
        String artifactRole = firstDetail(
                giStage,
                cacheStage,
                denoiseStage,
                "artifact_role",
                "heatmap_artifact_role",
                "round8_artifact_role",
                "capture_mode",
                "lucerna_round8_capture_mode"
        );
        String rayBudgetHeatmapRole = firstDetail(
                giStage,
                cacheStage,
                "ray_budget_heatmap_role",
                "ray_budget_artifact_role",
                "heatmap_ray_budget_role",
                "heatmap_artifact",
                "artifact_role"
        );
        String historyConfidenceHeatmapRole = firstDetail(
                denoiseStage,
                giStage,
                cacheStage,
                "history_confidence_heatmap_role",
                "history_confidence_artifact_role",
                "heatmap_history_confidence_role",
                "history_heatmap_artifact",
                "artifact_role"
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
        String reuseOnlyCount = firstDetail(
                giStage,
                cacheStage,
                "reuse_only",
                "reuseonly",
                "reuse",
                "reuse_count",
                "reuse_cells",
                "reuse_regions",
                "stable_reuse",
                "stable_reuse_cells"
        );
        String lowCount = firstDetail(
                giStage,
                cacheStage,
                "low",
                "low_count",
                "low_cells",
                "low_regions",
                "stable_refresh",
                "stable_refresh_cells"
        );
        String mediumCount = firstDetail(
                giStage,
                cacheStage,
                "medium",
                "medium_count",
                "medium_cells",
                "medium_regions",
                "fixed_medium",
                "fixed_medium_cells"
        );
        String highCount = firstDetail(
                giStage,
                cacheStage,
                "high",
                "high_count",
                "high_cells",
                "high_regions",
                "noisy",
                "noisy_cells",
                "emissive_high",
                "emissive_high_cells"
        );
        String bucketSummary = firstDetail(
                giStage,
                cacheStage,
                "ray_budget_buckets",
                "raybudgetbuckets",
                "budget_buckets",
                "budgetbucketcounts",
                "budget_bucket_counts",
                "adaptive_ray_budget_buckets",
                "adaptive_budget_buckets"
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
        String cacheConfidenceContribution = firstDetail(
                cacheStage,
                giStage,
                "cache_confidence_contribution",
                "cacheconfidencecontribution",
                "cache_confidence_weight",
                "cache_confidence_factor",
                "confidence_contribution"
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
        String historyAccepted = firstDetail(
                denoiseStage,
                giStage,
                "history_accepted",
                "historyaccepted",
                "history_accepted_pixels",
                "accepted_history",
                "acceptedhistory",
                "accepted_history_pixels"
        );
        String historyRejected = firstDetail(
                denoiseStage,
                giStage,
                "history_rejected",
                "historyrejected",
                "history_rejected_pixels",
                "rejected_history",
                "rejectedhistory",
                "rejected_history_pixels",
                "disocclusion_rejected",
                "disocclusionrejects"
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
                + ",sceneState=" + valueOrUnknown(sceneState)
                + ",buckets=" + bucketCountsLabel(reuseOnlyCount, lowCount, mediumCount, highCount, bucketSummary)
                + ",confidence=" + valueOrUnknown(confidence)
                + ",cacheConfidenceContribution=" + valueOrUnknown(cacheConfidenceContribution)
                + ",variance=" + valueOrUnknown(variance)
                + ",history=" + valueOrUnknown(historyAvailable)
                + ",historyAccepted=" + valueOrUnknown(historyAccepted)
                + ",historyRejected=" + valueOrUnknown(historyRejected)
                + ",disocclusion=" + valueOrUnknown(disocclusionMaskReady);
        String adaptiveLine = "Adaptive sampling: enabled=" + valueOrUnknown(adaptiveEnabled)
                + " tier=" + valueOrUnknown(tier)
                + " sceneState=" + valueOrUnknown(sceneState)
                + " reason=" + readinessReason(giStage, cacheStage);
        String sceneStateLine = "Round 8 sceneState: " + valueOrUnknown(sceneState)
                + " artifactRole=" + valueOrUnknown(artifactRole)
                + " captureHint=controller-supplied";
        String budgetLine = "Ray budget: tier=" + valueOrUnknown(tier)
                + " raysPerCell=" + valueOrUnknown(raysPerCell)
                + " requested=" + valueOrUnknown(requestedRays)
                + " capped=" + valueOrUnknown(cappedRays)
                + " cells=" + valueOrUnknown(cells);
        String bucketLine = "Ray-budget buckets: reuseOnly=" + valueOrUnknown(reuseOnlyCount)
                + " low=" + valueOrUnknown(lowCount)
                + " medium=" + valueOrUnknown(mediumCount)
                + " high=" + valueOrUnknown(highCount)
                + " summary=" + valueOrUnknown(bucketSummary);
        String heatmapLine = "Ray-budget heatmap: role=" + valueOrUnknown(rayBudgetHeatmapRole, artifactRole, "ray-budget")
                + " source=GI budget tier"
                + " mode=" + heatmapMode(tier)
                + " sceneState=" + valueOrUnknown(sceneState)
                + " buckets=" + bucketCountsLabel(reuseOnlyCount, lowCount, mediumCount, highCount, bucketSummary)
                + " ready=" + readinessFrom(tier, requestedRays, cappedRays);
        String varianceLine = "Variance map: variance=" + valueOrUnknown(variance)
                + " confidence=" + valueOrUnknown(confidence)
                + " samples=" + valueOrUnknown(sampleCount)
                + " ready=" + readinessFrom(variance, confidence);
        String historyLine = "History confidence: available=" + valueOrUnknown(historyAvailable)
                + " value=" + valueOrUnknown(historyConfidence)
                + " accepted=" + valueOrUnknown(historyAccepted)
                + " rejected=" + valueOrUnknown(historyRejected)
                + " frame=" + valueOrUnknown(stageFrame(denoiseStage, giStage));
        String historyHeatmapLine = "History-confidence heatmap: role="
                + valueOrUnknown(historyConfidenceHeatmapRole, artifactRole, "history-confidence")
                + " sceneState=" + valueOrUnknown(sceneState)
                + " accepted=" + valueOrUnknown(historyAccepted)
                + " rejected=" + valueOrUnknown(historyRejected)
                + " ready=" + readinessFrom(historyAvailable, historyConfidence, historyAccepted, historyRejected);
        String historyCountsLine = "History counts: historyAccepted=" + valueOrUnknown(historyAccepted)
                + " historyRejected=" + valueOrUnknown(historyRejected)
                + " disocclusionPixels=" + valueOrUnknown(disocclusionPixels);
        String disocclusionLine = "Disocclusion mask: ready=" + valueOrUnknown(disocclusionMaskReady)
                + " pixels=" + valueOrUnknown(disocclusionPixels)
                + " source=history rejection counters";
        String cacheContributionLine = "Cache confidence contribution: value="
                + valueOrUnknown(cacheConfidenceContribution)
                + " cacheConfidence=" + valueOrUnknown(confidence)
                + " cacheReads=" + valueOrUnknown(cacheStage == null ? null : cacheStage.cacheReadCount())
                + " cacheWrites=" + valueOrUnknown(cacheStage == null ? null : cacheStage.cacheWriteCount());
        String heatmapRolesLine = "Heatmap roles: rayBudget="
                + valueOrUnknown(rayBudgetHeatmapRole, artifactRole, "ray-budget")
                + " historyConfidence=" + valueOrUnknown(historyConfidenceHeatmapRole, artifactRole, "history-confidence")
                + " visualProof=controller-owned";
        String readinessLine = "Round 8 readiness: telemetry=" + yesNo(hasTelemetry)
                + " sceneState=" + readinessFrom(sceneState, artifactRole)
                + " budget=" + readinessFrom(tier, requestedRays, cappedRays)
                + " buckets=" + readinessFrom(reuseOnlyCount, lowCount, mediumCount, highCount, bucketSummary)
                + " variance=" + readinessFrom(variance, confidence)
                + " history=" + readinessFrom(historyAvailable, historyConfidence, historyAccepted, historyRejected)
                + " cacheConfidenceContribution=" + readinessFrom(cacheConfidenceContribution, confidence)
                + " disocclusion=" + readinessFrom(disocclusionMaskReady, disocclusionPixels);
        String boundaryLine = "Round 8 evidence boundary: overlay/status only; visual heatmap proof is controller-owned and must reject proof markers/focus-window shortcuts";

        return new Round8AdaptiveDebugStatus(
                hasTelemetry,
                summary,
                adaptiveLine,
                sceneStateLine,
                budgetLine,
                bucketLine,
                heatmapLine,
                varianceLine,
                historyLine,
                historyHeatmapLine,
                historyCountsLine,
                disocclusionLine,
                cacheContributionLine,
                heatmapRolesLine,
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

    private static String readinessFrom(
            String primary,
            String secondary,
            String tertiary,
            String quaternary
    ) {
        return (!valueOrUnknown(primary).equals("?")
                || !valueOrUnknown(secondary).equals("?")
                || !valueOrUnknown(tertiary).equals("?")
                || !valueOrUnknown(quaternary).equals("?")) ? "ready" : "missing";
    }

    private static String readinessFrom(
            String primary,
            String secondary,
            String tertiary,
            String quaternary,
            String fallbackSummary
    ) {
        return (!valueOrUnknown(primary).equals("?")
                || !valueOrUnknown(secondary).equals("?")
                || !valueOrUnknown(tertiary).equals("?")
                || !valueOrUnknown(quaternary).equals("?")
                || !valueOrUnknown(fallbackSummary).equals("?")) ? "ready" : "missing";
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String valueOrUnknown(Long value) {
        return value == null ? "?" : Long.toString(value);
    }

    private static String valueOrUnknown(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return fallback;
    }

    private static String bucketCountsLabel(
            String reuseOnlyCount,
            String lowCount,
            String mediumCount,
            String highCount,
            String bucketSummary
    ) {
        if (bucketSummary != null && !bucketSummary.isBlank()) {
            return bucketSummary;
        }
        return "reuseOnly=" + valueOrUnknown(reuseOnlyCount)
                + "/low=" + valueOrUnknown(lowCount)
                + "/medium=" + valueOrUnknown(mediumCount)
                + "/high=" + valueOrUnknown(highCount);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String cleanLine(String value) {
        return value == null || value.isBlank() ? "unreported" : value.trim();
    }
}
