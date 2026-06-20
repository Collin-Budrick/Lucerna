package net.lucerna.render.lighting.restir.direct;

import java.util.Objects;

public record DirectRestirSelectedCandidate(
        DirectRestirLightSourceIdentity lightSource,
        int candidateIndex,
        int reservoirPixelX,
        int reservoirPixelY,
        float targetPdf,
        float candidatePdf,
        float contributionLuminance,
        long candidateGeneration,
        String selectionKey
) {
    public DirectRestirSelectedCandidate {
        Objects.requireNonNull(lightSource, "lightSource");
        if (candidateIndex < 0) {
            throw new IllegalArgumentException("candidateIndex must be non-negative");
        }
        if (reservoirPixelX < 0) {
            throw new IllegalArgumentException("reservoirPixelX must be non-negative");
        }
        if (reservoirPixelY < 0) {
            throw new IllegalArgumentException("reservoirPixelY must be non-negative");
        }
        requireNonNegativeFinite(targetPdf, "targetPdf");
        requireNonNegativeFinite(candidatePdf, "candidatePdf");
        requireNonNegativeFinite(contributionLuminance, "contributionLuminance");
        if (candidateGeneration < 0L) {
            throw new IllegalArgumentException("candidateGeneration must be non-negative");
        }
        selectionKey = requireText(selectionKey, "selectionKey");
    }

    public static DirectRestirSelectedCandidate metadataOnly(
            DirectRestirLightSourceIdentity lightSource,
            int candidateIndex,
            int reservoirPixelX,
            int reservoirPixelY,
            float contributionLuminance,
            long candidateGeneration
    ) {
        Objects.requireNonNull(lightSource, "lightSource");
        return new DirectRestirSelectedCandidate(
                lightSource,
                candidateIndex,
                reservoirPixelX,
                reservoirPixelY,
                0.0F,
                0.0F,
                contributionLuminance,
                candidateGeneration,
                lightSource.stableKey() + "#" + candidateIndex + "@" + reservoirPixelX + "," + reservoirPixelY
        );
    }

    public boolean hasExecutableSamplingPdf() {
        return this.targetPdf > 0.0F && this.candidatePdf > 0.0F;
    }

    public boolean hasContribution() {
        return this.contributionLuminance > 0.0F;
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
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
