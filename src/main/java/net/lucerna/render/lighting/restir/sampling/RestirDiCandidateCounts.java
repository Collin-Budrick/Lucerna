package net.lucerna.render.lighting.restir.sampling;

public record RestirDiCandidateCounts(
        int stableLightCount,
        int regionBucketCount,
        int bruteForceBaselineCount,
        int sampledCount,
        int selectedLightCount
) {
    public RestirDiCandidateCounts {
        stableLightCount = Math.max(0, stableLightCount);
        regionBucketCount = Math.max(0, regionBucketCount);
        bruteForceBaselineCount = Math.max(0, bruteForceBaselineCount);
        sampledCount = Math.max(0, sampledCount);
        selectedLightCount = Math.max(0, Math.min(selectedLightCount, sampledCount));
    }

    public static RestirDiCandidateCounts unavailable() {
        return new RestirDiCandidateCounts(0, 0, 0, 0, 0);
    }

    public boolean hasCandidates() {
        return this.bruteForceBaselineCount > 0 || this.stableLightCount > 0;
    }

    public int reducedCandidateCount() {
        return Math.max(0, this.bruteForceBaselineCount - this.sampledCount);
    }

    public float reductionRatio() {
        if (this.bruteForceBaselineCount <= 0) {
            return 0.0F;
        }
        return clampUnit(this.reducedCandidateCount() / (float) this.bruteForceBaselineCount);
    }

    public RestirDiCandidateReductionStatus reductionStatus() {
        if (!this.hasCandidates()) {
            return RestirDiCandidateReductionStatus.EMPTY;
        }
        if (this.sampledCount <= 0) {
            return RestirDiCandidateReductionStatus.UNAVAILABLE;
        }
        return this.sampledCount < this.bruteForceBaselineCount
                ? RestirDiCandidateReductionStatus.REDUCED
                : RestirDiCandidateReductionStatus.PASSTHROUGH;
    }

    public String compactLabel() {
        return "lights=" + this.stableLightCount
                + " regions=" + this.regionBucketCount
                + " bruteForce=" + this.bruteForceBaselineCount
                + " sampled=" + this.sampledCount
                + " selected=" + this.selectedLightCount
                + " reductionRatio=" + this.reductionRatio()
                + " status=" + this.reductionStatus().name().toLowerCase();
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
