package net.lucerna.render.lighting.restir.sampling;

import java.util.Objects;

public record RestirDiCandidateRegionBucket(
        String dimension,
        int regionX,
        int regionY,
        int regionZ,
        int radiusSections,
        int candidateCount,
        long generation
) {
    public RestirDiCandidateRegionBucket {
        dimension = cleanDimension(dimension);
        radiusSections = Math.max(0, radiusSections);
        candidateCount = Math.max(0, candidateCount);
        generation = Math.max(0L, generation);
    }

    public static RestirDiCandidateRegionBucket section(
            String dimension,
            int sectionX,
            int sectionY,
            int sectionZ,
            int candidateCount,
            long generation
    ) {
        return new RestirDiCandidateRegionBucket(dimension, sectionX, sectionY, sectionZ, 0, candidateCount, generation);
    }

    public boolean hasCandidates() {
        return this.candidateCount > 0;
    }

    public String stableKey() {
        return this.dimension + ":" + this.regionX + "," + this.regionY + "," + this.regionZ + ":r" + this.radiusSections;
    }

    public String compactLabel() {
        return this.stableKey() + " candidates=" + this.candidateCount + " gen=" + this.generation;
    }

    private static String cleanDimension(String value) {
        Objects.requireNonNull(value, "dimension");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        return value;
    }
}
