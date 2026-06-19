package net.lucerna.render.voxel;

public record VoxelRayBudgetConfig(
        int primaryRaysPerPixel,
        int shadowRaysPerHit,
        int giRaysPerHit,
        int maxRaysPerFrame,
        int maxVisitedVoxelsPerRay,
        int maxVisitedSectionsPerRay
) {
    public VoxelRayBudgetConfig {
        requireNonNegative(primaryRaysPerPixel, "primaryRaysPerPixel");
        requireNonNegative(shadowRaysPerHit, "shadowRaysPerHit");
        requireNonNegative(giRaysPerHit, "giRaysPerHit");
        if (primaryRaysPerPixel + shadowRaysPerHit + giRaysPerHit <= 0) {
            throw new IllegalArgumentException("at least one ray class must have a positive budget");
        }
        if (maxRaysPerFrame <= 0) {
            throw new IllegalArgumentException("maxRaysPerFrame must be positive");
        }
        if (maxVisitedVoxelsPerRay <= 0) {
            throw new IllegalArgumentException("maxVisitedVoxelsPerRay must be positive");
        }
        if (maxVisitedSectionsPerRay <= 0) {
            throw new IllegalArgumentException("maxVisitedSectionsPerRay must be positive");
        }
    }

    public static VoxelRayBudgetConfig primaryGBuffer(int viewportWidth, int viewportHeight) {
        return new VoxelRayBudgetConfig(
                1,
                0,
                0,
                positivePixelBudget(viewportWidth, viewportHeight),
                512,
                64
        );
    }

    public int primaryRayBudget(int viewportWidth, int viewportHeight) {
        long pixelCount = positivePixelBudget(viewportWidth, viewportHeight);
        long requested = pixelCount * (long) this.primaryRaysPerPixel;
        return clampToInt(Math.min(requested, this.maxRaysPerFrame));
    }

    public int totalSecondaryRaysPerHit() {
        return this.shadowRaysPerHit + this.giRaysPerHit;
    }

    public boolean hasSecondaryBudget() {
        return this.totalSecondaryRaysPerHit() > 0;
    }

    private static int positivePixelBudget(int viewportWidth, int viewportHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return 1;
        }
        return clampToInt((long) viewportWidth * (long) viewportHeight);
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(1L, value);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
