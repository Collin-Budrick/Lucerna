package net.lucerna.render.cluster;

import net.lucerna.world.section.ChunkSectionGeneration;

import java.util.Objects;

public record ChunkClusterMetadata(
        ChunkClusterId id,
        ChunkClusterLocalBounds localBounds,
        ChunkClusterLodTier lodTier,
        ChunkSectionGeneration sectionGeneration,
        int occupiedVoxelCount,
        int opaqueVoxelCount,
        int translucentVoxelCount,
        int fluidVoxelCount,
        int emissiveVoxelCount,
        int surfaceSampleCount,
        int materialPaletteSize,
        long sourceGeneration,
        boolean visibilityPlaceholder,
        boolean uploadPlaceholder
) {
    public static final int ESTIMATED_UPLOAD_HEADER_BYTES = 96;

    public ChunkClusterMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(localBounds, "localBounds");
        Objects.requireNonNull(lodTier, "lodTier");
        Objects.requireNonNull(sectionGeneration, "sectionGeneration");
        occupiedVoxelCount = requireNonNegative(occupiedVoxelCount, "occupiedVoxelCount");
        opaqueVoxelCount = requireNonNegative(opaqueVoxelCount, "opaqueVoxelCount");
        translucentVoxelCount = requireNonNegative(translucentVoxelCount, "translucentVoxelCount");
        fluidVoxelCount = requireNonNegative(fluidVoxelCount, "fluidVoxelCount");
        emissiveVoxelCount = requireNonNegative(emissiveVoxelCount, "emissiveVoxelCount");
        surfaceSampleCount = requireNonNegative(surfaceSampleCount, "surfaceSampleCount");
        materialPaletteSize = requireNonNegative(materialPaletteSize, "materialPaletteSize");
        sourceGeneration = Math.max(0L, sourceGeneration);
        int capacity = localBounds.estimatedVoxelCapacity();
        if (occupiedVoxelCount > capacity) {
            throw new IllegalArgumentException("occupiedVoxelCount cannot exceed cluster voxel capacity");
        }
        if (surfaceSampleCount > capacity) {
            throw new IllegalArgumentException("surfaceSampleCount cannot exceed cluster voxel capacity");
        }
        if (opaqueVoxelCount + translucentVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("opaque and translucent counts cannot exceed occupiedVoxelCount");
        }
        if (fluidVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("fluidVoxelCount cannot exceed occupiedVoxelCount");
        }
        if (emissiveVoxelCount > occupiedVoxelCount) {
            throw new IllegalArgumentException("emissiveVoxelCount cannot exceed occupiedVoxelCount");
        }
    }

    public boolean hasGeometryPayload() {
        return this.occupiedVoxelCount > 0 || this.surfaceSampleCount > 0;
    }

    public boolean hasTranslucentOrFluid() {
        return this.translucentVoxelCount > 0 || this.fluidVoxelCount > 0;
    }

    public boolean boundsComplete() {
        return this.localBounds.spanX() == this.lodTier.edgeLength()
                && this.localBounds.spanY() == this.lodTier.edgeLength()
                && this.localBounds.spanZ() == this.lodTier.edgeLength();
    }

    public float occupiedDensity() {
        int capacity = this.localBounds.estimatedVoxelCapacity();
        if (capacity <= 0 || this.occupiedVoxelCount <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.occupiedVoxelCount / capacity);
    }

    public ChunkClusterDensityComplexityBucket densityComplexityBucket() {
        if (!this.hasGeometryPayload()) {
            return ChunkClusterDensityComplexityBucket.EMPTY;
        }

        float density = this.occupiedDensity();
        boolean mixedMaterialKinds = this.hasTranslucentOrFluid() || this.emissiveVoxelCount > 0;
        boolean highSurfaceDetail = this.surfaceSampleCount >= Math.max(8, this.localBounds.estimatedVoxelCapacity() / 16);
        boolean largePalette = this.materialPaletteSize >= 16;
        if (density >= 0.75F || (density >= 0.35F && mixedMaterialKinds && (highSurfaceDetail || largePalette))) {
            return ChunkClusterDensityComplexityBucket.COMPLEX;
        }
        if (density >= 0.35F || mixedMaterialKinds || highSurfaceDetail || largePalette) {
            return ChunkClusterDensityComplexityBucket.DENSE;
        }
        if (density >= 0.10F || this.surfaceSampleCount >= 4 || this.materialPaletteSize >= 8) {
            return ChunkClusterDensityComplexityBucket.MODERATE;
        }
        return ChunkClusterDensityComplexityBucket.SPARSE;
    }

    public boolean gpuUploadMetadataReady() {
        return this.hasGeometryPayload()
                && this.boundsComplete()
                && this.estimatedUploadBytes() >= ESTIMATED_UPLOAD_HEADER_BYTES
                && this.sourceGeneration >= this.sectionGeneration.combinedGeneration();
    }

    public boolean indirectPayloadMetadataReady() {
        return this.gpuUploadMetadataReady()
                && this.lodTier.clustersPerSection() > 0
                && this.densityComplexityBucket() != ChunkClusterDensityComplexityBucket.EMPTY;
    }

    public String generationAssociationKey() {
        return this.id.stableKey() + "@generation=" + this.sourceGeneration;
    }

    public int estimatedUploadBytes() {
        return ESTIMATED_UPLOAD_HEADER_BYTES
                + this.surfaceSampleCount * Integer.BYTES * 4
                + Math.min(this.materialPaletteSize, 256) * Integer.BYTES;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }
}
