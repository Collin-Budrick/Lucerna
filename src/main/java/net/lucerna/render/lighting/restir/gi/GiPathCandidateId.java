package net.lucerna.render.lighting.restir.gi;

import java.util.Objects;

public record GiPathCandidateId(
        long frameIndex,
        long reservoirIndex,
        int pixelX,
        int pixelY,
        int sampleIndex,
        long stablePathHash,
        String debugLabel
) {
    public GiPathCandidateId {
        frameIndex = Math.max(0L, frameIndex);
        reservoirIndex = Math.max(0L, reservoirIndex);
        pixelX = Math.max(0, pixelX);
        pixelY = Math.max(0, pixelY);
        sampleIndex = Math.max(0, sampleIndex);
        stablePathHash = Math.max(0L, stablePathHash);
        debugLabel = clean(debugLabel, "GI path candidate metadata");
    }

    public static GiPathCandidateId unavailable(String reason) {
        return new GiPathCandidateId(0L, 0L, 0, 0, 0, 0L, reason);
    }

    public String stableKey() {
        return this.frameIndex
                + ":" + this.reservoirIndex
                + ":" + this.pixelX
                + ":" + this.pixelY
                + ":" + this.sampleIndex
                + ":" + this.stablePathHash;
    }

    public boolean hasStablePathHash() {
        return this.stablePathHash > 0L;
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI path candidate metadata");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
