package net.lucerna.render.lighting.restir.reuse;

import java.util.List;
import java.util.Objects;

public record DirectLightTemporalReuseStatus(
        int reservoirCount,
        int candidateCount,
        List<Integer> selectedLightIds,
        int temporalReuseCount,
        DirectLightReuseAcceptanceCounts acceptanceCounts,
        List<DirectLightReuseInvalidationReason> invalidationReasons,
        boolean previousFrameAvailable,
        boolean statusOnly,
        String readinessReason
) {
    public DirectLightTemporalReuseStatus {
        reservoirCount = Math.max(0, reservoirCount);
        candidateCount = Math.max(0, candidateCount);
        selectedLightIds = normalizeLightIds(selectedLightIds);
        temporalReuseCount = Math.max(0, temporalReuseCount);
        acceptanceCounts = acceptanceCounts == null ? DirectLightReuseAcceptanceCounts.empty() : acceptanceCounts;
        invalidationReasons = normalizeReasons(invalidationReasons);
        statusOnly = true;
        readinessReason = clean(readinessReason, defaultReadinessReason(previousFrameAvailable, temporalReuseCount, invalidationReasons));
    }

    public static DirectLightTemporalReuseStatus unavailable(DirectLightReuseInvalidationReason reason) {
        return new DirectLightTemporalReuseStatus(
                0,
                0,
                List.of(),
                0,
                DirectLightReuseAcceptanceCounts.empty(),
                List.of(reason == null ? DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION : reason),
                false,
                true,
                "temporal reuse status unavailable"
        );
    }

    public boolean readyForReusePlanning() {
        return this.previousFrameAvailable
                && this.reservoirCount > 0
                && this.candidateCount > 0
                && !this.invalidated();
    }

    public boolean invalidated() {
        return this.invalidationReasons.stream().anyMatch(reason -> reason != DirectLightReuseInvalidationReason.NONE);
    }

    public String summary() {
        return "reservoirCount=" + this.reservoirCount
                + ",candidateCount=" + this.candidateCount
                + ",selectedLightIds=" + this.selectedLightIds
                + ",temporalReuseCount=" + this.temporalReuseCount
                + ",previousFrameAvailable=" + this.previousFrameAvailable
                + ",temporalReady=" + this.readyForReusePlanning()
                + ",temporalInvalidationReasons=" + stableReasonIds(this.invalidationReasons)
                + "," + this.acceptanceCounts.summary("temporal")
                + ",statusOnly=" + this.statusOnly
                + ",executionClaim=none"
                + ",readinessReason=\"" + this.readinessReason + "\"";
    }

    private static List<DirectLightReuseInvalidationReason> normalizeReasons(
            List<DirectLightReuseInvalidationReason> reasons
    ) {
        if (reasons == null || reasons.isEmpty()) {
            return List.of(DirectLightReuseInvalidationReason.NONE);
        }
        List<DirectLightReuseInvalidationReason> normalized = reasons.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of(DirectLightReuseInvalidationReason.NONE) : normalized;
    }

    static List<Integer> normalizeLightIds(List<Integer> selectedLightIds) {
        if (selectedLightIds == null || selectedLightIds.isEmpty()) {
            return List.of();
        }
        return selectedLightIds.stream()
                .filter(id -> id != null && id >= 0)
                .distinct()
                .toList();
    }

    private static String defaultReadinessReason(
            boolean previousFrameAvailable,
            int temporalReuseCount,
            List<DirectLightReuseInvalidationReason> reasons
    ) {
        if (!previousFrameAvailable) {
            return "missing previous direct-light reservoir frame";
        }
        if (reasons.stream().anyMatch(reason -> reason != DirectLightReuseInvalidationReason.NONE)) {
            return "temporal reuse invalidated by " + stableReasonIds(reasons);
        }
        if (temporalReuseCount <= 0) {
            return "temporal reuse counters not populated yet";
        }
        return "temporal reuse status counters present";
    }

    static String stableReasonIds(List<DirectLightReuseInvalidationReason> reasons) {
        return reasons.stream()
                .map(DirectLightReuseInvalidationReason::stableId)
                .toList()
                .toString();
    }

    static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
