package net.lucerna.lighting;

public record DenoiseHistoryCounters(
        long acceptedPixels,
        long rejectedPixels,
        long resetPixels,
        long missingHistoryPixels,
        long disocclusionPixels,
        long materialMismatchPixels
) {
    public DenoiseHistoryCounters {
        acceptedPixels = Math.max(0L, acceptedPixels);
        rejectedPixels = Math.max(0L, rejectedPixels);
        resetPixels = Math.max(0L, resetPixels);
        missingHistoryPixels = Math.max(0L, missingHistoryPixels);
        disocclusionPixels = Math.max(0L, disocclusionPixels);
        materialMismatchPixels = Math.max(0L, materialMismatchPixels);
    }

    public static DenoiseHistoryCounters none() {
        return new DenoiseHistoryCounters(0L, 0L, 0L, 0L, 0L, 0L);
    }

    public long evaluatedPixels() {
        return this.acceptedPixels
                + this.rejectedPixels
                + this.resetPixels
                + this.missingHistoryPixels
                + this.disocclusionPixels
                + this.materialMismatchPixels;
    }

    public boolean hasHistoryEvaluation() {
        return this.evaluatedPixels() > 0L;
    }

    public long acceptedOrRejectedPixels() {
        return this.acceptedPixels + this.rejectedPixels;
    }

    public float rejectionRatio() {
        long evaluated = this.acceptedOrRejectedPixels();
        if (evaluated <= 0L) {
            return 0.0F;
        }
        return (float) this.rejectedPixels / (float) evaluated;
    }
}
