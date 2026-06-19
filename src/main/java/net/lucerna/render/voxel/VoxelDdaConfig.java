package net.lucerna.render.voxel;

public record VoxelDdaConfig(
        int maxStepsPerRay,
        int maxSectionsPerRay,
        float hitEpsilon,
        float normalEpsilon,
        boolean stopAtFirstOpaque,
        boolean includeTranslucent,
        boolean includeFluids
) {
    public VoxelDdaConfig {
        if (maxStepsPerRay <= 0) {
            throw new IllegalArgumentException("maxStepsPerRay must be positive");
        }
        if (maxSectionsPerRay <= 0) {
            throw new IllegalArgumentException("maxSectionsPerRay must be positive");
        }
        requirePositiveFinite(hitEpsilon, "hitEpsilon");
        requirePositiveFinite(normalEpsilon, "normalEpsilon");
    }

    public static VoxelDdaConfig primaryGBuffer() {
        return new VoxelDdaConfig(
                512,
                64,
                0.0001F,
                0.001F,
                true,
                true,
                true
        );
    }

    public static VoxelDdaConfig shadowRay() {
        return new VoxelDdaConfig(
                384,
                48,
                0.0001F,
                0.001F,
                true,
                false,
                false
        );
    }

    public boolean mayVisitNonOpaqueVoxels() {
        return this.includeTranslucent || this.includeFluids;
    }

    private static void requirePositiveFinite(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
