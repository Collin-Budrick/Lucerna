package net.lucerna.render.lighting.restir.sampling;

import java.util.List;
import java.util.Objects;

public record RestirDiManyLightCandidateSamplingPlan(
        long generation,
        List<RestirDiLightId> stableLightIds,
        List<RestirDiCandidateRegionBucket> regionBuckets,
        List<RestirDiSelectedLightCandidate> selectedLights,
        RestirDiCandidateCounts counts,
        String debugLabel
) {
    public RestirDiManyLightCandidateSamplingPlan {
        generation = Math.max(0L, generation);
        Objects.requireNonNull(stableLightIds, "stableLightIds");
        Objects.requireNonNull(regionBuckets, "regionBuckets");
        Objects.requireNonNull(selectedLights, "selectedLights");
        stableLightIds = List.copyOf(stableLightIds);
        regionBuckets = List.copyOf(regionBuckets);
        selectedLights = List.copyOf(selectedLights);
        for (RestirDiLightId lightId : stableLightIds) {
            Objects.requireNonNull(lightId, "stableLightIds must not contain null entries");
            generation = Math.max(generation, lightId.generation());
        }
        for (RestirDiCandidateRegionBucket bucket : regionBuckets) {
            Objects.requireNonNull(bucket, "regionBuckets must not contain null entries");
            generation = Math.max(generation, bucket.generation());
        }
        for (RestirDiSelectedLightCandidate selectedLight : selectedLights) {
            Objects.requireNonNull(selectedLight, "selectedLights must not contain null entries");
            generation = Math.max(generation, selectedLight.sourceGeneration());
        }
        if (counts == null) {
            counts = new RestirDiCandidateCounts(
                    stableLightIds.size(),
                    regionBuckets.size(),
                    regionBuckets.stream().mapToInt(RestirDiCandidateRegionBucket::candidateCount).sum(),
                    selectedLights.size(),
                    selectedLights.size()
            );
        }
        debugLabel = clean(debugLabel, "ReSTIR DI many-light candidate sampling metadata only");
    }

    public static RestirDiManyLightCandidateSamplingPlan empty() {
        return new RestirDiManyLightCandidateSamplingPlan(
                0L,
                List.of(),
                List.of(),
                List.of(),
                RestirDiCandidateCounts.unavailable(),
                "ReSTIR DI many-light candidate sampling inputs unavailable"
        );
    }

    public RestirDiCandidateReductionStatus reductionStatus() {
        return this.counts.reductionStatus();
    }

    public float candidateReductionRatio() {
        return this.counts.reductionRatio();
    }

    public boolean metadataOnly() {
        return true;
    }

    public String compactLabel() {
        return "restirDiSamplingMetadataOnly=true"
                + " gen=" + this.generation
                + " " + this.counts.compactLabel()
                + " statusLabel=\"" + this.reductionStatus().label() + "\""
                + " debug=\"" + this.debugLabel + "\"";
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "ReSTIR DI candidate sampling metadata");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
