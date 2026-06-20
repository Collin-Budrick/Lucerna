package net.lucerna.render.lighting.restir.direct;

public record DirectRestirReservoirWeight(
        float sumWeights,
        float selectedWeight,
        float normalizedWeight,
        int candidateCount,
        float confidence,
        boolean unbiasedWeightReady
) {
    public DirectRestirReservoirWeight {
        requireNonNegativeFinite(sumWeights, "sumWeights");
        requireNonNegativeFinite(selectedWeight, "selectedWeight");
        requireUnitFinite(normalizedWeight, "normalizedWeight");
        if (candidateCount < 0) {
            throw new IllegalArgumentException("candidateCount must be non-negative");
        }
        requireUnitFinite(confidence, "confidence");
        if (candidateCount == 0 && (sumWeights > 0.0F || selectedWeight > 0.0F || normalizedWeight > 0.0F)) {
            throw new IllegalArgumentException("weights require at least one candidate");
        }
        if (unbiasedWeightReady && (candidateCount == 0 || normalizedWeight <= 0.0F)) {
            throw new IllegalArgumentException("unbiasedWeightReady requires a selected weighted candidate");
        }
    }

    public static DirectRestirReservoirWeight empty() {
        return new DirectRestirReservoirWeight(0.0F, 0.0F, 0.0F, 0, 0.0F, false);
    }

    public boolean hasSelectedCandidateWeight() {
        return this.candidateCount > 0 && this.selectedWeight > 0.0F && this.normalizedWeight > 0.0F;
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireUnitFinite(float value, String name) {
        requireNonNegativeFinite(value, name);
        if (value > 1.0F) {
            throw new IllegalArgumentException(name + " must be no greater than one");
        }
    }
}
