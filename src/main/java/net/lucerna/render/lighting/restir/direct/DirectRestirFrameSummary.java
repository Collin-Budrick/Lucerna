package net.lucerna.render.lighting.restir.direct;

import java.util.Objects;

public record DirectRestirFrameSummary(
        long frameIndex,
        int reservoirWidth,
        int reservoirHeight,
        int reservoirCount,
        int sourceCount,
        int selectedCandidateCount,
        DirectRestirSelectedCandidate selectedCandidate,
        DirectRestirReservoirWeight reservoirWeight,
        DirectRestirTemporalReuseSummary temporalReuse,
        DirectRestirSpatialReuseSummary spatialReuse,
        DirectRestirValidationStatus validationStatus,
        String statusMarker
) {
    public DirectRestirFrameSummary {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        if (reservoirWidth < 0) {
            throw new IllegalArgumentException("reservoirWidth must be non-negative");
        }
        if (reservoirHeight < 0) {
            throw new IllegalArgumentException("reservoirHeight must be non-negative");
        }
        if (reservoirCount < 0) {
            throw new IllegalArgumentException("reservoirCount must be non-negative");
        }
        if (sourceCount < 0) {
            throw new IllegalArgumentException("sourceCount must be non-negative");
        }
        if (selectedCandidateCount < 0) {
            throw new IllegalArgumentException("selectedCandidateCount must be non-negative");
        }
        Objects.requireNonNull(reservoirWeight, "reservoirWeight");
        Objects.requireNonNull(temporalReuse, "temporalReuse");
        Objects.requireNonNull(spatialReuse, "spatialReuse");
        Objects.requireNonNull(validationStatus, "validationStatus");
        statusMarker = requireText(statusMarker, "statusMarker");
        if ((long) reservoirWidth * reservoirHeight < reservoirCount) {
            throw new IllegalArgumentException("reservoirCount cannot exceed reservoir dimensions");
        }
        if (selectedCandidateCount > reservoirCount) {
            throw new IllegalArgumentException("selectedCandidateCount cannot exceed reservoirCount");
        }
        if (selectedCandidate == null && selectedCandidateCount > 0) {
            throw new IllegalArgumentException("selectedCandidate is required when selectedCandidateCount is positive");
        }
    }

    public static DirectRestirFrameSummary metadataOnly(long frameIndex) {
        return new DirectRestirFrameSummary(
                frameIndex,
                0,
                0,
                0,
                0,
                0,
                null,
                DirectRestirReservoirWeight.empty(),
                DirectRestirTemporalReuseSummary.disabled(frameIndex),
                DirectRestirSpatialReuseSummary.disabled(),
                DirectRestirValidationStatus.metadataScaffold(),
                "direct_restir_metadata_scaffold_no_execution"
        );
    }

    public boolean hasReservoirMetadata() {
        return this.reservoirCount > 0 && this.reservoirWidth > 0 && this.reservoirHeight > 0;
    }

    public boolean hasSelectedCandidate() {
        return this.selectedCandidate != null && this.selectedCandidateCount > 0;
    }

    public boolean hasReuseEvidence() {
        return this.temporalReuse.hasReuseEvidence() || this.spatialReuse.hasReuseEvidence();
    }

    public String compactStatusLine() {
        return "directRestir reservoirs=" + this.reservoirCount
                + " sources=" + this.sourceCount
                + " selected=" + this.selectedCandidateCount
                + " temporal=" + this.temporalReuse.mode()
                + " spatial=" + this.spatialReuse.mode()
                + " confidence=" + this.reservoirWeight.confidence()
                + " boundary=\"" + this.validationStatus.boundaryLabel() + "\"";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
