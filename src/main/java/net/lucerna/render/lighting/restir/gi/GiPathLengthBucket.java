package net.lucerna.render.lighting.restir.gi;

public enum GiPathLengthBucket {
    NONE(0, 0),
    ONE_BOUNCE(1, 1),
    TWO_BOUNCE(2, 2),
    THREE_BOUNCE(3, 3),
    FOUR_OR_MORE_BOUNCES(4, Integer.MAX_VALUE);

    private final int minInclusive;
    private final int maxInclusive;

    GiPathLengthBucket(int minInclusive, int maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public static GiPathLengthBucket fromPathLength(int pathLength) {
        int safePathLength = Math.max(0, pathLength);
        for (GiPathLengthBucket bucket : values()) {
            if (safePathLength >= bucket.minInclusive && safePathLength <= bucket.maxInclusive) {
                return bucket;
            }
        }
        return FOUR_OR_MORE_BOUNCES;
    }

    public int minInclusive() {
        return this.minInclusive;
    }

    public int maxInclusive() {
        return this.maxInclusive;
    }

    public boolean hasIndirectBounce() {
        return this.minInclusive > 1;
    }

    public boolean active() {
        return this != NONE;
    }
}
