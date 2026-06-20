package net.lucerna.render.culling;

import java.util.Collections;
import java.util.List;

public record ClusterCullingSummary(
        int clusterCount,
        int visibleCount,
        int culledCount,
        int offscreenCount,
        int frustumCulledCount,
        int occlusionPlaceholderCount,
        int missingMetadataCount,
        int uploadBytes,
        boolean actualGpuCullingExecuted,
        boolean gpuCullingPrerequisitesReady,
        String gpuCullingMissingPrerequisites,
        String gpuCullingBlockerReason,
        int frustumCandidateCount,
        boolean occlusionReady,
        boolean indirectDrawReady,
        String cullingModeLabel,
        String generationSummary,
        IndirectDrawListStats indirectDrawStats,
        List<ClusterCullingDecision> decisions
) {
    public ClusterCullingSummary {
        if (clusterCount < 0
                || visibleCount < 0
                || culledCount < 0
                || offscreenCount < 0
                || frustumCulledCount < 0
                || occlusionPlaceholderCount < 0
                || missingMetadataCount < 0
                || uploadBytes < 0
                || frustumCandidateCount < 0) {
            throw new IllegalArgumentException("culling summary counters must be non-negative");
        }
        actualGpuCullingExecuted = actualGpuCullingExecuted && gpuCullingPrerequisitesReady;
        if (gpuCullingMissingPrerequisites == null || gpuCullingMissingPrerequisites.isBlank()) {
            gpuCullingMissingPrerequisites = actualGpuCullingExecuted ? "none" : "gpu-dispatch-and-visibility-buffer";
        }
        if (gpuCullingBlockerReason == null || gpuCullingBlockerReason.isBlank()) {
            gpuCullingBlockerReason = actualGpuCullingExecuted ? "none" : "actual-gpu-culling-not-proven";
        }
        if (cullingModeLabel == null || cullingModeLabel.isBlank()) {
            cullingModeLabel = actualGpuCullingExecuted ? "actual-gpu-culling" : "conservative-cpu-status";
        }
        if (generationSummary == null || generationSummary.isBlank()) {
            generationSummary = "unreported";
        }
        if (indirectDrawStats == null) {
            indirectDrawStats = IndirectDrawListStats.metadataOnly(0, 0, 0L);
        }
        indirectDrawReady = indirectDrawReady && indirectDrawStats.actualGpuIndirectReady();
        decisions = immutable(decisions);
    }

    public static ClusterCullingSummary unavailable(String reason) {
        return new ClusterCullingSummary(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                "cluster-metadata",
                "cluster metadata unavailable",
                0,
                false,
                false,
                "unavailable",
                reason == null || reason.isBlank() ? "cluster metadata unavailable" : reason,
                IndirectDrawListStats.metadataOnly(0, 0, 0L),
                List.of()
        );
    }

    public String compactLabel() {
        return "clusters=" + this.clusterCount
                + " visible=" + this.visibleCount
                + " culled=" + this.culledCount
                + " offscreen=" + this.offscreenCount
                + " occlusionPlaceholder=" + this.occlusionPlaceholderCount
                + " uploadBytes=" + this.uploadBytes
                + " gpuExecuted=" + this.actualGpuCullingExecuted
                + " gpuPrereqs=" + this.gpuCullingPrerequisitesReady
                + " frustumCandidates=" + this.frustumCandidateCount
                + " occlusionReady=" + this.occlusionReady
                + " indirectReady=" + this.indirectDrawReady
                + " mode=" + this.cullingModeLabel
                + " blocker=" + this.gpuCullingBlockerReason
                + " " + this.generationSummary;
    }

    private static List<ClusterCullingDecision> immutable(List<ClusterCullingDecision> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(List.copyOf(source));
    }
}
