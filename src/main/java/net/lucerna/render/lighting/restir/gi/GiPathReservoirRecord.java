package net.lucerna.render.lighting.restir.gi;

public record GiPathReservoirRecord(
        GiPathCandidateId selectedCandidateId,
        GiPathLengthBucket pathLengthBucket,
        GiReservoirConfidence confidence,
        GiReservoirCacheLinkage cacheLinkage,
        long sourceGeneration,
        long lastTouchedFrame,
        boolean temporalReuseEligible,
        boolean spatialReuseEligible,
        String debugLabel
) {
    public GiPathReservoirRecord {
        selectedCandidateId = selectedCandidateId == null
                ? GiPathCandidateId.unavailable("selected GI path candidate unavailable")
                : selectedCandidateId;
        pathLengthBucket = pathLengthBucket == null ? GiPathLengthBucket.NONE : pathLengthBucket;
        confidence = confidence == null ? GiReservoirConfidence.unavailable("reservoir confidence unavailable") : confidence;
        cacheLinkage = cacheLinkage == null ? GiReservoirCacheLinkage.unlinked("cache linkage unavailable") : cacheLinkage;
        sourceGeneration = Math.max(0L, sourceGeneration);
        lastTouchedFrame = Math.max(0L, lastTouchedFrame);
        debugLabel = GiBounceConfidence.clean(debugLabel, "GI path reservoir metadata");
    }

    public static GiPathReservoirRecord unavailable(String reason) {
        return new GiPathReservoirRecord(
                GiPathCandidateId.unavailable(reason),
                GiPathLengthBucket.NONE,
                GiReservoirConfidence.unavailable(reason),
                GiReservoirCacheLinkage.unlinked(reason),
                0L,
                0L,
                false,
                false,
                reason
        );
    }

    public boolean reuseMetadataEligible(float confidenceFloor) {
        return !this.confidence.metadataOnly()
                && this.confidence.combinedConfidence() >= GiBounceConfidence.clampUnit(confidenceFloor)
                && this.cacheLinkage.usable()
                && (this.temporalReuseEligible || this.spatialReuseEligible);
    }

    public String compactLabel() {
        return "candidate=" + this.selectedCandidateId.stableKey()
                + " bucket=" + this.pathLengthBucket
                + " confidence=" + this.confidence.combinedConfidence()
                + " cacheLinked=" + this.cacheLinkage.linked()
                + " temporalEligible=" + this.temporalReuseEligible
                + " spatialEligible=" + this.spatialReuseEligible
                + " metadataOnly=" + this.confidence.metadataOnly();
    }
}
