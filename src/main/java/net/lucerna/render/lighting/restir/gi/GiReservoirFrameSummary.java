package net.lucerna.render.lighting.restir.gi;

import java.util.List;
import java.util.Objects;

public record GiReservoirFrameSummary(
        long frameIndex,
        long sourceGeneration,
        List<GiPathReservoirRecord> reservoirs,
        GiReservoirInvalidationSummary invalidationSummary,
        int candidateCount,
        int acceptedCandidateCount,
        int temporalReuseCandidateCount,
        int spatialReuseCandidateCount,
        boolean metadataOnly,
        String readinessReason
) {
    public GiReservoirFrameSummary {
        frameIndex = Math.max(0L, frameIndex);
        sourceGeneration = Math.max(0L, sourceGeneration);
        Objects.requireNonNull(reservoirs, "reservoirs");
        reservoirs = List.copyOf(reservoirs);
        for (GiPathReservoirRecord reservoir : reservoirs) {
            Objects.requireNonNull(reservoir, "reservoirs must not contain null entries");
        }
        invalidationSummary = invalidationSummary == null
                ? GiReservoirInvalidationSummary.empty(frameIndex, 0L)
                : invalidationSummary;
        candidateCount = Math.max(0, candidateCount);
        acceptedCandidateCount = Math.max(0, Math.min(candidateCount, acceptedCandidateCount));
        temporalReuseCandidateCount = Math.max(0, Math.min(candidateCount, temporalReuseCandidateCount));
        spatialReuseCandidateCount = Math.max(0, Math.min(candidateCount, spatialReuseCandidateCount));
        readinessReason = GiBounceConfidence.clean(
                readinessReason,
                metadataOnly ? "GI path reservoir metadata only" : "GI path reservoir frame metadata"
        );
    }

    public static GiReservoirFrameSummary unavailable(String reason) {
        return new GiReservoirFrameSummary(
                0L,
                0L,
                List.of(),
                GiReservoirInvalidationSummary.empty(0L, 0L),
                0,
                0,
                0,
                0,
                true,
                reason
        );
    }

    public int reservoirCount() {
        return this.reservoirs.size();
    }

    public boolean hasReservoirMetadata() {
        return !this.reservoirs.isEmpty() || this.candidateCount > 0;
    }

    public float acceptedCandidateRatio() {
        if (this.candidateCount == 0) {
            return 0.0F;
        }
        return GiBounceConfidence.clampUnit((float) this.acceptedCandidateCount / (float) this.candidateCount);
    }

    public float averageReservoirConfidence() {
        if (this.reservoirs.isEmpty()) {
            return 0.0F;
        }
        float total = 0.0F;
        for (GiPathReservoirRecord reservoir : this.reservoirs) {
            total += reservoir.confidence().combinedConfidence();
        }
        return GiBounceConfidence.clampUnit(total / this.reservoirs.size());
    }

    public String compactSummary() {
        return "frame=" + this.frameIndex
                + " reservoirs=" + this.reservoirCount()
                + " candidates=" + this.acceptedCandidateCount + "/" + this.candidateCount
                + " temporalReuseCandidates=" + this.temporalReuseCandidateCount
                + " spatialReuseCandidates=" + this.spatialReuseCandidateCount
                + " averageConfidence=" + this.averageReservoirConfidence()
                + " metadataOnly=" + this.metadataOnly
                + " invalidated=" + this.invalidationSummary.invalidatedAnything();
    }

    public String executionBoundaryLabel() {
        return "Round 11 GI/PT reservoir contracts only; metadata does not prove real path reuse execution";
    }
}
