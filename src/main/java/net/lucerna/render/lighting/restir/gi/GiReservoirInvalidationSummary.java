package net.lucerna.render.lighting.restir.gi;

public record GiReservoirInvalidationSummary(
        long frameIndex,
        long previousFrameIndex,
        long cacheGeneration,
        long latestDirtyGeneration,
        int dirtyRegionCount,
        int invalidatedReservoirCount,
        int retainedReservoirCount,
        int createdPlaceholderCount,
        boolean cameraReset,
        boolean globalInvalidation,
        String reason
) {
    public GiReservoirInvalidationSummary {
        frameIndex = Math.max(0L, frameIndex);
        previousFrameIndex = Math.max(0L, previousFrameIndex);
        cacheGeneration = Math.max(0L, cacheGeneration);
        latestDirtyGeneration = Math.max(0L, latestDirtyGeneration);
        dirtyRegionCount = Math.max(0, dirtyRegionCount);
        invalidatedReservoirCount = Math.max(0, invalidatedReservoirCount);
        retainedReservoirCount = Math.max(0, retainedReservoirCount);
        createdPlaceholderCount = Math.max(0, createdPlaceholderCount);
        reason = GiBounceConfidence.clean(reason, defaultReason(cameraReset, globalInvalidation, dirtyRegionCount));
    }

    public static GiReservoirInvalidationSummary empty(long frameIndex, long cacheGeneration) {
        return new GiReservoirInvalidationSummary(
                frameIndex,
                0L,
                cacheGeneration,
                0L,
                0,
                0,
                0,
                0,
                false,
                false,
                "no GI path reservoir invalidation"
        );
    }

    public boolean invalidatedAnything() {
        return this.globalInvalidation
                || this.cameraReset
                || this.invalidatedReservoirCount > 0
                || this.createdPlaceholderCount > 0;
    }

    public int totalKnownReservoirs() {
        return this.invalidatedReservoirCount + this.retainedReservoirCount + this.createdPlaceholderCount;
    }

    private static String defaultReason(boolean cameraReset, boolean globalInvalidation, int dirtyRegionCount) {
        if (globalInvalidation) {
            return "GI path reservoirs globally invalidated";
        }
        if (cameraReset) {
            return "GI path reservoirs invalidated by camera reset";
        }
        if (dirtyRegionCount > 0) {
            return "GI path reservoirs touched by dirty regions";
        }
        return "GI path reservoir metadata unchanged";
    }
}
