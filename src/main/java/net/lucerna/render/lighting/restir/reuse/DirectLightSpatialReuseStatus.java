package net.lucerna.render.lighting.restir.reuse;

import java.util.List;
import java.util.Objects;

public record DirectLightSpatialReuseStatus(
        int reservoirCount,
        int candidateCount,
        List<Integer> selectedLightIds,
        int spatialReuseCount,
        DirectLightNeighborTapCounts neighborTapCounts,
        DirectLightReuseAcceptanceCounts acceptanceCounts,
        List<DirectLightReuseInvalidationReason> invalidationReasons,
        boolean neighborReservoirsAvailable,
        boolean statusOnly,
        String readinessReason
) {
    public DirectLightSpatialReuseStatus {
        reservoirCount = Math.max(0, reservoirCount);
        candidateCount = Math.max(0, candidateCount);
        selectedLightIds = DirectLightTemporalReuseStatus.normalizeLightIds(selectedLightIds);
        spatialReuseCount = Math.max(0, spatialReuseCount);
        neighborTapCounts = neighborTapCounts == null ? DirectLightNeighborTapCounts.empty() : neighborTapCounts;
        acceptanceCounts = acceptanceCounts == null ? DirectLightReuseAcceptanceCounts.empty() : acceptanceCounts;
        invalidationReasons = normalizeReasons(invalidationReasons);
        statusOnly = true;
        readinessReason = DirectLightTemporalReuseStatus.clean(
                readinessReason,
                defaultReadinessReason(neighborReservoirsAvailable, spatialReuseCount, neighborTapCounts, invalidationReasons)
        );
    }

    public static DirectLightSpatialReuseStatus unavailable(DirectLightReuseInvalidationReason reason) {
        return new DirectLightSpatialReuseStatus(
                0,
                0,
                List.of(),
                0,
                DirectLightNeighborTapCounts.empty(),
                DirectLightReuseAcceptanceCounts.empty(),
                List.of(reason == null ? DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION : reason),
                false,
                true,
                "spatial reuse status unavailable"
        );
    }

    public boolean readyForReusePlanning() {
        return this.neighborReservoirsAvailable
                && this.reservoirCount > 0
                && this.candidateCount > 0
                && this.neighborTapCounts.hasAcceptedNeighborTap()
                && !this.invalidated();
    }

    public boolean invalidated() {
        return this.invalidationReasons.stream().anyMatch(reason -> reason != DirectLightReuseInvalidationReason.NONE);
    }

    public String summary() {
        return "reservoirCount=" + this.reservoirCount
                + ",candidateCount=" + this.candidateCount
                + ",selectedLightIds=" + this.selectedLightIds
                + ",spatialReuseCount=" + this.spatialReuseCount
                + ",neighborReservoirsAvailable=" + this.neighborReservoirsAvailable
                + ",spatialReady=" + this.readyForReusePlanning()
                + ",spatialInvalidationReasons=" + DirectLightTemporalReuseStatus.stableReasonIds(this.invalidationReasons)
                + "," + this.neighborTapCounts.summary("spatial")
                + "," + this.acceptanceCounts.summary("spatial")
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

    private static String defaultReadinessReason(
            boolean neighborReservoirsAvailable,
            int spatialReuseCount,
            DirectLightNeighborTapCounts neighborTapCounts,
            List<DirectLightReuseInvalidationReason> reasons
    ) {
        if (!neighborReservoirsAvailable) {
            return "missing neighboring direct-light reservoirs";
        }
        if (reasons.stream().anyMatch(reason -> reason != DirectLightReuseInvalidationReason.NONE)) {
            return "spatial reuse invalidated by " + DirectLightTemporalReuseStatus.stableReasonIds(reasons);
        }
        if (!neighborTapCounts.hasAcceptedNeighborTap()) {
            return "spatial reuse has no accepted neighbor taps";
        }
        if (spatialReuseCount <= 0) {
            return "spatial reuse counters not populated yet";
        }
        return "spatial reuse status counters present";
    }
}
