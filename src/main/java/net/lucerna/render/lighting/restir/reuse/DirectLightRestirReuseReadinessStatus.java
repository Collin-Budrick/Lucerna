package net.lucerna.render.lighting.restir.reuse;

import java.util.LinkedHashSet;
import java.util.List;

public record DirectLightRestirReuseReadinessStatus(
        int reservoirCount,
        int candidateCount,
        List<Integer> selectedLightIds,
        int temporalReuseCount,
        int spatialReuseCount,
        DirectLightTemporalReuseStatus temporalReuse,
        DirectLightSpatialReuseStatus spatialReuse,
        boolean directLightReservoirsAvailable,
        boolean statusOnly,
        String readinessReason
) {
    public DirectLightRestirReuseReadinessStatus {
        temporalReuse = temporalReuse == null
                ? DirectLightTemporalReuseStatus.unavailable(DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION)
                : temporalReuse;
        spatialReuse = spatialReuse == null
                ? DirectLightSpatialReuseStatus.unavailable(DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION)
                : spatialReuse;
        reservoirCount = Math.max(0, Math.max(reservoirCount, Math.max(
                temporalReuse.reservoirCount(),
                spatialReuse.reservoirCount()
        )));
        candidateCount = Math.max(0, Math.max(candidateCount, Math.max(
                temporalReuse.candidateCount(),
                spatialReuse.candidateCount()
        )));
        selectedLightIds = mergedLightIds(selectedLightIds, temporalReuse.selectedLightIds(), spatialReuse.selectedLightIds());
        temporalReuseCount = Math.max(0, Math.max(temporalReuseCount, temporalReuse.temporalReuseCount()));
        spatialReuseCount = Math.max(0, Math.max(spatialReuseCount, spatialReuse.spatialReuseCount()));
        statusOnly = true;
        readinessReason = DirectLightTemporalReuseStatus.clean(readinessReason, defaultReadinessReason(
                directLightReservoirsAvailable,
                temporalReuse,
                spatialReuse
        ));
    }

    public static DirectLightRestirReuseReadinessStatus statusOnlyUnavailable(String readinessReason) {
        return new DirectLightRestirReuseReadinessStatus(
                0,
                0,
                List.of(),
                0,
                0,
                DirectLightTemporalReuseStatus.unavailable(DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION),
                DirectLightSpatialReuseStatus.unavailable(DirectLightReuseInvalidationReason.STATUS_ONLY_NO_EXECUTION),
                false,
                true,
                readinessReason
        );
    }

    public boolean temporalReuseReady() {
        return this.temporalReuse.readyForReusePlanning();
    }

    public boolean spatialReuseReady() {
        return this.spatialReuse.readyForReusePlanning();
    }

    public boolean readyForReservoirReusePlanning() {
        return this.directLightReservoirsAvailable
                && this.reservoirCount > 0
                && this.candidateCount > 0
                && (this.temporalReuseReady() || this.spatialReuseReady());
    }

    public String compactSummary() {
        return "reservoirCount=" + this.reservoirCount
                + ",candidateCount=" + this.candidateCount
                + ",selectedLightIds=" + this.selectedLightIds
                + ",temporalReuseCount=" + this.temporalReuseCount
                + ",spatialReuseCount=" + this.spatialReuseCount
                + ",directLightReservoirsAvailable=" + this.directLightReservoirsAvailable
                + ",temporalReuseReady=" + this.temporalReuseReady()
                + ",spatialReuseReady=" + this.spatialReuseReady()
                + ",restirReuseReady=" + this.readyForReservoirReusePlanning()
                + ",statusOnly=" + this.statusOnly
                + ",executionClaim=none"
                + ",readinessReason=\"" + this.readinessReason + "\"";
    }

    public String detailSummary() {
        return this.compactSummary()
                + ",temporal={" + this.temporalReuse.summary() + "}"
                + ",spatial={" + this.spatialReuse.summary() + "}";
    }

    public String evidenceBoundary() {
        return "Direct-light ReSTIR reuse status only; records reservoir/candidate/reuse counters and invalidation reasons, "
                + "but does not claim temporal reuse, spatial reuse, GPU execution, or visible rendering output.";
    }

    private static String defaultReadinessReason(
            boolean directLightReservoirsAvailable,
            DirectLightTemporalReuseStatus temporalReuse,
            DirectLightSpatialReuseStatus spatialReuse
    ) {
        if (!directLightReservoirsAvailable) {
            return "direct-light reservoirs unavailable";
        }
        if (temporalReuse.readyForReusePlanning() || spatialReuse.readyForReusePlanning()) {
            return "direct-light ReSTIR reuse status has planning-ready counters";
        }
        return "direct-light ReSTIR reuse counters present but not planning-ready";
    }

    private static List<Integer> mergedLightIds(
            List<Integer> selectedLightIds,
            List<Integer> temporalSelectedLightIds,
            List<Integer> spatialSelectedLightIds
    ) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        if (selectedLightIds != null) {
            selectedLightIds.stream().filter(id -> id != null && id >= 0).forEach(ids::add);
        }
        if (temporalSelectedLightIds != null) {
            temporalSelectedLightIds.stream().filter(id -> id != null && id >= 0).forEach(ids::add);
        }
        if (spatialSelectedLightIds != null) {
            spatialSelectedLightIds.stream().filter(id -> id != null && id >= 0).forEach(ids::add);
        }
        return List.copyOf(ids);
    }
}
