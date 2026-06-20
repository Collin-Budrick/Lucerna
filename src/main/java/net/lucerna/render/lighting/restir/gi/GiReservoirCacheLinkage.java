package net.lucerna.render.lighting.restir.gi;

public record GiReservoirCacheLinkage(
        long cacheGeneration,
        String surfaceCacheKey,
        String radianceCacheKey,
        long linkedRecordGeneration,
        boolean surfaceRecordLinked,
        boolean radianceRecordLinked,
        boolean dirty,
        String reason
) {
    public GiReservoirCacheLinkage {
        cacheGeneration = Math.max(0L, cacheGeneration);
        surfaceCacheKey = cleanKey(surfaceCacheKey);
        radianceCacheKey = cleanKey(radianceCacheKey);
        linkedRecordGeneration = Math.max(0L, linkedRecordGeneration);
        surfaceRecordLinked = surfaceRecordLinked && !surfaceCacheKey.isEmpty();
        radianceRecordLinked = radianceRecordLinked && !radianceCacheKey.isEmpty();
        reason = GiBounceConfidence.clean(reason, dirty ? "GI reservoir cache linkage dirty" : "GI reservoir cache linkage metadata");
    }

    public static GiReservoirCacheLinkage unlinked(String reason) {
        return new GiReservoirCacheLinkage(0L, "", "", 0L, false, false, false, reason);
    }

    public boolean linked() {
        return this.surfaceRecordLinked || this.radianceRecordLinked;
    }

    public boolean usable() {
        return this.linked() && !this.dirty;
    }

    public String stableLinkKey() {
        return this.cacheGeneration
                + ":" + this.surfaceCacheKey
                + ":" + this.radianceCacheKey
                + ":" + this.linkedRecordGeneration;
    }

    private static String cleanKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
