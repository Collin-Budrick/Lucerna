package net.lucerna.render.lighting.restir.gi;

public record GiReservoirConfidence(
        float selectionWeightSum,
        float selectedWeight,
        int candidateCount,
        int acceptedCandidateCount,
        float temporalConfidence,
        float spatialConfidence,
        GiBounceConfidence bounceConfidence,
        boolean metadataOnly,
        String reason
) {
    public GiReservoirConfidence {
        selectionWeightSum = GiBounceConfidence.finiteNonNegative(selectionWeightSum);
        selectedWeight = GiBounceConfidence.finiteNonNegative(selectedWeight);
        candidateCount = Math.max(0, candidateCount);
        acceptedCandidateCount = Math.max(0, Math.min(candidateCount, acceptedCandidateCount));
        temporalConfidence = GiBounceConfidence.clampUnit(temporalConfidence);
        spatialConfidence = GiBounceConfidence.clampUnit(spatialConfidence);
        bounceConfidence = bounceConfidence == null ? GiBounceConfidence.unavailable("bounce metadata unavailable") : bounceConfidence;
        reason = GiBounceConfidence.clean(reason, metadataOnly ? "GI reservoir metadata only" : "GI reservoir confidence metadata");
    }

    public static GiReservoirConfidence unavailable(String reason) {
        return new GiReservoirConfidence(
                0.0F,
                0.0F,
                0,
                0,
                0.0F,
                0.0F,
                GiBounceConfidence.unavailable(reason),
                true,
                reason
        );
    }

    public float selectedWeightRatio() {
        if (this.selectionWeightSum <= 0.0F) {
            return 0.0F;
        }
        return GiBounceConfidence.clampUnit(this.selectedWeight / this.selectionWeightSum);
    }

    public float acceptanceRatio() {
        if (this.candidateCount == 0) {
            return 0.0F;
        }
        return GiBounceConfidence.clampUnit((float) this.acceptedCandidateCount / (float) this.candidateCount);
    }

    public float combinedConfidence() {
        return GiBounceConfidence.clampUnit(
                this.selectedWeightRatio() * 0.20F
                        + this.acceptanceRatio() * 0.20F
                        + this.temporalConfidence * 0.20F
                        + this.spatialConfidence * 0.15F
                        + this.bounceConfidence.confidence() * 0.25F
        );
    }

    public boolean hasCandidateSupport() {
        return this.candidateCount > 0 && this.acceptedCandidateCount > 0 && this.selectedWeight > 0.0F;
    }

    public String boundaryLabel() {
        return "GI reservoir metadata; real path reuse execution not implied; combinedConfidence="
                + this.combinedConfidence()
                + " metadataOnly=" + this.metadataOnly;
    }
}
