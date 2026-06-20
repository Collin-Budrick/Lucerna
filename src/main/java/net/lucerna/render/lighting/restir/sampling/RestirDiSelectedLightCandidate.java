package net.lucerna.render.lighting.restir.sampling;

import java.util.Objects;

public record RestirDiSelectedLightCandidate(
        RestirDiLightId lightId,
        String regionKey,
        int sampleIndex,
        float selectionWeight,
        long sourceGeneration
) {
    public RestirDiSelectedLightCandidate {
        Objects.requireNonNull(lightId, "lightId");
        regionKey = cleanRegionKey(regionKey);
        sampleIndex = Math.max(0, sampleIndex);
        selectionWeight = nonNegativeFinite(selectionWeight, "selectionWeight");
        sourceGeneration = Math.max(0L, sourceGeneration);
    }

    public static RestirDiSelectedLightCandidate fromRegion(
            RestirDiLightId lightId,
            RestirDiCandidateRegionBucket region,
            int sampleIndex,
            float selectionWeight
    ) {
        Objects.requireNonNull(lightId, "lightId");
        Objects.requireNonNull(region, "region");
        return new RestirDiSelectedLightCandidate(
                lightId,
                region.stableKey(),
                sampleIndex,
                selectionWeight,
                Math.max(lightId.generation(), region.generation())
        );
    }

    public boolean hasWeight() {
        return this.selectionWeight > 0.0F;
    }

    public String compactLabel() {
        return this.lightId.compactLabel()
                + " region=" + this.regionKey
                + " sample=" + this.sampleIndex
                + " weight=" + this.selectionWeight
                + " gen=" + this.sourceGeneration;
    }

    private static String cleanRegionKey(String value) {
        if (value == null || value.isBlank()) {
            return "unbucketed";
        }
        return value.trim();
    }

    private static float nonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return Math.max(0.0F, value);
    }
}
