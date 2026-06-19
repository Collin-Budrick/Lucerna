package net.lucerna.upload;

import net.lucerna.render.lighting.gi.CacheConfidence;
import net.lucerna.render.lighting.gi.GiCacheSnapshot;
import net.lucerna.render.lighting.gi.RadianceCacheKey;
import net.lucerna.render.lighting.gi.RadianceCacheRecord;
import net.lucerna.render.lighting.gi.SurfaceCacheKey;
import net.lucerna.render.lighting.gi.SurfaceCacheRecord;
import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativeDiffuseGiCacheUpload {
    public static final int DIRTY_REGION_SECTION_STRIDE = 3;
    public static final int DIRTY_REGION_SECTION_X_OFFSET = 0;
    public static final int DIRTY_REGION_SECTION_Y_OFFSET = 1;
    public static final int DIRTY_REGION_SECTION_Z_OFFSET = 2;

    public static final int SURFACE_KEY_STRIDE = 7;
    public static final int SURFACE_KEY_SECTION_X_OFFSET = 0;
    public static final int SURFACE_KEY_SECTION_Y_OFFSET = 1;
    public static final int SURFACE_KEY_SECTION_Z_OFFSET = 2;
    public static final int SURFACE_KEY_LOCAL_X_OFFSET = 3;
    public static final int SURFACE_KEY_LOCAL_Y_OFFSET = 4;
    public static final int SURFACE_KEY_LOCAL_Z_OFFSET = 5;
    public static final int SURFACE_KEY_FACE_ORDINAL_OFFSET = 6;
    public static final int SURFACE_PROPERTY_STRIDE = 7;
    public static final int SURFACE_PROPERTY_NORMAL_X_OFFSET = 0;
    public static final int SURFACE_PROPERTY_NORMAL_Y_OFFSET = 1;
    public static final int SURFACE_PROPERTY_NORMAL_Z_OFFSET = 2;
    public static final int SURFACE_PROPERTY_ALBEDO_R_OFFSET = 3;
    public static final int SURFACE_PROPERTY_ALBEDO_G_OFFSET = 4;
    public static final int SURFACE_PROPERTY_ALBEDO_B_OFFSET = 5;
    public static final int SURFACE_PROPERTY_ROUGHNESS_OFFSET = 6;

    public static final int RADIANCE_KEY_STRIDE = 4;
    public static final int RADIANCE_KEY_CELL_X_OFFSET = 0;
    public static final int RADIANCE_KEY_CELL_Y_OFFSET = 1;
    public static final int RADIANCE_KEY_CELL_Z_OFFSET = 2;
    public static final int RADIANCE_KEY_CASCADE_OFFSET = 3;
    public static final int RADIANCE_PROPERTY_STRIDE = 7;
    public static final int RADIANCE_PROPERTY_R_OFFSET = 0;
    public static final int RADIANCE_PROPERTY_G_OFFSET = 1;
    public static final int RADIANCE_PROPERTY_B_OFFSET = 2;
    public static final int RADIANCE_PROPERTY_DIRECTION_X_OFFSET = 3;
    public static final int RADIANCE_PROPERTY_DIRECTION_Y_OFFSET = 4;
    public static final int RADIANCE_PROPERTY_DIRECTION_Z_OFFSET = 5;
    public static final int RADIANCE_PROPERTY_DIRECTIONAL_VARIANCE_OFFSET = 6;

    public static final int CACHE_CONFIDENCE_FLOAT_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_VALUE_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_VARIANCE_OFFSET = 1;
    public static final int CACHE_CONFIDENCE_INTEGER_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_SAMPLE_COUNT_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_DIRTY_OFFSET = 1;
    public static final int CACHE_CONFIDENCE_GENERATION_STRIDE = 2;
    public static final int CACHE_CONFIDENCE_SOURCE_GENERATION_OFFSET = 0;
    public static final int CACHE_CONFIDENCE_LAST_TOUCHED_FRAME_OFFSET = 1;

    private final long cacheGeneration;
    private final int dirtyRegionCount;
    private final long firstDirtyRegionGeneration;
    private final long lastDirtyRegionGeneration;
    private final int surfaceRecordCount;
    private final int radianceRecordCount;
    private final int[] dirtyRegionTypeIds;
    private final String[] dirtyRegionTypeNames;
    private final String[] dirtyRegionDimensions;
    private final int[] dirtyRegionSections;
    private final int[] dirtyRegionSectionScoped;
    private final long[] dirtyRegionGenerations;
    private final String[] surfaceDimensions;
    private final int[] surfaceKeys;
    private final long[] surfaceGenerations;
    private final int[] surfaceMaterialIds;
    private final float[] surfaceProperties;
    private final float[] surfaceConfidenceFloats;
    private final int[] surfaceConfidenceIntegers;
    private final long[] surfaceConfidenceGenerations;
    private final String[] radianceDimensions;
    private final int[] radianceKeys;
    private final long[] radianceGenerations;
    private final float[] radianceProperties;
    private final int[] radianceSampleCounts;
    private final long[] radianceLastFrameIndices;
    private final float[] radianceConfidenceFloats;
    private final int[] radianceConfidenceIntegers;
    private final long[] radianceConfidenceGenerations;

    private NativeDiffuseGiCacheUpload(
            long cacheGeneration,
            int dirtyRegionCount,
            long firstDirtyRegionGeneration,
            long lastDirtyRegionGeneration,
            int surfaceRecordCount,
            int radianceRecordCount,
            int[] dirtyRegionTypeIds,
            String[] dirtyRegionTypeNames,
            String[] dirtyRegionDimensions,
            int[] dirtyRegionSections,
            int[] dirtyRegionSectionScoped,
            long[] dirtyRegionGenerations,
            String[] surfaceDimensions,
            int[] surfaceKeys,
            long[] surfaceGenerations,
            int[] surfaceMaterialIds,
            float[] surfaceProperties,
            float[] surfaceConfidenceFloats,
            int[] surfaceConfidenceIntegers,
            long[] surfaceConfidenceGenerations,
            String[] radianceDimensions,
            int[] radianceKeys,
            long[] radianceGenerations,
            float[] radianceProperties,
            int[] radianceSampleCounts,
            long[] radianceLastFrameIndices,
            float[] radianceConfidenceFloats,
            int[] radianceConfidenceIntegers,
            long[] radianceConfidenceGenerations
    ) {
        this.cacheGeneration = cacheGeneration;
        this.dirtyRegionCount = dirtyRegionCount;
        this.firstDirtyRegionGeneration = firstDirtyRegionGeneration;
        this.lastDirtyRegionGeneration = lastDirtyRegionGeneration;
        this.surfaceRecordCount = surfaceRecordCount;
        this.radianceRecordCount = radianceRecordCount;
        this.dirtyRegionTypeIds = copy(dirtyRegionTypeIds, "dirtyRegionTypeIds");
        this.dirtyRegionTypeNames = copy(dirtyRegionTypeNames, "dirtyRegionTypeNames");
        this.dirtyRegionDimensions = copy(dirtyRegionDimensions, "dirtyRegionDimensions");
        this.dirtyRegionSections = copy(dirtyRegionSections, "dirtyRegionSections");
        this.dirtyRegionSectionScoped = copy(dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
        this.dirtyRegionGenerations = copy(dirtyRegionGenerations, "dirtyRegionGenerations");
        this.surfaceDimensions = copy(surfaceDimensions, "surfaceDimensions");
        this.surfaceKeys = copy(surfaceKeys, "surfaceKeys");
        this.surfaceGenerations = copy(surfaceGenerations, "surfaceGenerations");
        this.surfaceMaterialIds = copy(surfaceMaterialIds, "surfaceMaterialIds");
        this.surfaceProperties = copy(surfaceProperties, "surfaceProperties");
        this.surfaceConfidenceFloats = copy(surfaceConfidenceFloats, "surfaceConfidenceFloats");
        this.surfaceConfidenceIntegers = copy(surfaceConfidenceIntegers, "surfaceConfidenceIntegers");
        this.surfaceConfidenceGenerations = copy(surfaceConfidenceGenerations, "surfaceConfidenceGenerations");
        this.radianceDimensions = copy(radianceDimensions, "radianceDimensions");
        this.radianceKeys = copy(radianceKeys, "radianceKeys");
        this.radianceGenerations = copy(radianceGenerations, "radianceGenerations");
        this.radianceProperties = copy(radianceProperties, "radianceProperties");
        this.radianceSampleCounts = copy(radianceSampleCounts, "radianceSampleCounts");
        this.radianceLastFrameIndices = copy(radianceLastFrameIndices, "radianceLastFrameIndices");
        this.radianceConfidenceFloats = copy(radianceConfidenceFloats, "radianceConfidenceFloats");
        this.radianceConfidenceIntegers = copy(radianceConfidenceIntegers, "radianceConfidenceIntegers");
        this.radianceConfidenceGenerations = copy(radianceConfidenceGenerations, "radianceConfidenceGenerations");

        this.validate();
    }

    public static NativeDiffuseGiCacheUpload empty() {
        return from(GiCacheSnapshot.empty());
    }

    public static NativeDiffuseGiCacheUpload from(GiCacheSnapshot snapshot) {
        GiCacheSnapshot resolvedSnapshot = snapshot == null ? GiCacheSnapshot.empty() : snapshot;
        DirtyRegionBatch dirtyRegionBatch = resolvedSnapshot.dirtyRegions();
        List<DirtyRegion> dirtyRegions = dirtyRegionBatch.regions();
        List<SurfaceCacheRecord> surfaceRecords = resolvedSnapshot.surfaceRecords();
        List<RadianceCacheRecord> radianceRecords = resolvedSnapshot.radianceRecords();

        int[] dirtyRegionTypeIds = new int[dirtyRegions.size()];
        String[] dirtyRegionTypeNames = new String[dirtyRegions.size()];
        String[] dirtyRegionDimensions = new String[dirtyRegions.size()];
        int[] dirtyRegionSections = new int[dirtyRegions.size() * DIRTY_REGION_SECTION_STRIDE];
        int[] dirtyRegionSectionScoped = new int[dirtyRegions.size()];
        long[] dirtyRegionGenerations = new long[dirtyRegions.size()];

        for (int index = 0; index < dirtyRegions.size(); index++) {
            DirtyRegion dirtyRegion = dirtyRegions.get(index);
            dirtyRegionTypeIds[index] = dirtyRegion.type().nativeTypeId();
            dirtyRegionTypeNames[index] = dirtyRegion.type().name();
            dirtyRegionDimensions[index] = dirtyRegion.dimension();
            int sectionOffset = index * DIRTY_REGION_SECTION_STRIDE;
            dirtyRegionSections[sectionOffset + DIRTY_REGION_SECTION_X_OFFSET] = dirtyRegion.sectionX();
            dirtyRegionSections[sectionOffset + DIRTY_REGION_SECTION_Y_OFFSET] = dirtyRegion.sectionY();
            dirtyRegionSections[sectionOffset + DIRTY_REGION_SECTION_Z_OFFSET] = dirtyRegion.sectionZ();
            dirtyRegionSectionScoped[index] = dirtyRegion.sectionScoped() ? 1 : 0;
            dirtyRegionGenerations[index] = dirtyRegion.generation();
        }

        String[] surfaceDimensions = new String[surfaceRecords.size()];
        int[] surfaceKeys = new int[surfaceRecords.size() * SURFACE_KEY_STRIDE];
        long[] surfaceGenerations = new long[surfaceRecords.size()];
        int[] surfaceMaterialIds = new int[surfaceRecords.size()];
        float[] surfaceProperties = new float[surfaceRecords.size() * SURFACE_PROPERTY_STRIDE];
        float[] surfaceConfidenceFloats = new float[surfaceRecords.size() * CACHE_CONFIDENCE_FLOAT_STRIDE];
        int[] surfaceConfidenceIntegers = new int[surfaceRecords.size() * CACHE_CONFIDENCE_INTEGER_STRIDE];
        long[] surfaceConfidenceGenerations = new long[surfaceRecords.size() * CACHE_CONFIDENCE_GENERATION_STRIDE];

        for (int index = 0; index < surfaceRecords.size(); index++) {
            SurfaceCacheRecord record = surfaceRecords.get(index);
            SurfaceCacheKey key = record.key();
            CacheConfidence confidence = record.confidence();
            surfaceDimensions[index] = key.dimension();
            int keyOffset = index * SURFACE_KEY_STRIDE;
            surfaceKeys[keyOffset + SURFACE_KEY_SECTION_X_OFFSET] = key.sectionX();
            surfaceKeys[keyOffset + SURFACE_KEY_SECTION_Y_OFFSET] = key.sectionY();
            surfaceKeys[keyOffset + SURFACE_KEY_SECTION_Z_OFFSET] = key.sectionZ();
            surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_X_OFFSET] = key.localX();
            surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_Y_OFFSET] = key.localY();
            surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_Z_OFFSET] = key.localZ();
            surfaceKeys[keyOffset + SURFACE_KEY_FACE_ORDINAL_OFFSET] = key.faceOrdinal();
            surfaceGenerations[index] = record.generation();
            surfaceMaterialIds[index] = record.materialId();
            int propertyOffset = index * SURFACE_PROPERTY_STRIDE;
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_X_OFFSET] = record.normalX();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_Y_OFFSET] = record.normalY();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_Z_OFFSET] = record.normalZ();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_R_OFFSET] = record.albedoR();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_G_OFFSET] = record.albedoG();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_B_OFFSET] = record.albedoB();
            surfaceProperties[propertyOffset + SURFACE_PROPERTY_ROUGHNESS_OFFSET] = record.roughness();
            fillConfidence(
                    confidence,
                    surfaceConfidenceFloats,
                    surfaceConfidenceIntegers,
                    surfaceConfidenceGenerations,
                    index
            );
        }

        String[] radianceDimensions = new String[radianceRecords.size()];
        int[] radianceKeys = new int[radianceRecords.size() * RADIANCE_KEY_STRIDE];
        long[] radianceGenerations = new long[radianceRecords.size()];
        float[] radianceProperties = new float[radianceRecords.size() * RADIANCE_PROPERTY_STRIDE];
        int[] radianceSampleCounts = new int[radianceRecords.size()];
        long[] radianceLastFrameIndices = new long[radianceRecords.size()];
        float[] radianceConfidenceFloats = new float[radianceRecords.size() * CACHE_CONFIDENCE_FLOAT_STRIDE];
        int[] radianceConfidenceIntegers = new int[radianceRecords.size() * CACHE_CONFIDENCE_INTEGER_STRIDE];
        long[] radianceConfidenceGenerations = new long[radianceRecords.size() * CACHE_CONFIDENCE_GENERATION_STRIDE];

        for (int index = 0; index < radianceRecords.size(); index++) {
            RadianceCacheRecord record = radianceRecords.get(index);
            RadianceCacheKey key = record.key();
            CacheConfidence confidence = record.confidence();
            radianceDimensions[index] = key.dimension();
            int keyOffset = index * RADIANCE_KEY_STRIDE;
            radianceKeys[keyOffset + RADIANCE_KEY_CELL_X_OFFSET] = key.cellX();
            radianceKeys[keyOffset + RADIANCE_KEY_CELL_Y_OFFSET] = key.cellY();
            radianceKeys[keyOffset + RADIANCE_KEY_CELL_Z_OFFSET] = key.cellZ();
            radianceKeys[keyOffset + RADIANCE_KEY_CASCADE_OFFSET] = key.cascade();
            radianceGenerations[index] = record.generation();
            int propertyOffset = index * RADIANCE_PROPERTY_STRIDE;
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_R_OFFSET] = record.radianceR();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_G_OFFSET] = record.radianceG();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_B_OFFSET] = record.radianceB();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_X_OFFSET] = record.directionX();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_Y_OFFSET] = record.directionY();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_Z_OFFSET] = record.directionZ();
            radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTIONAL_VARIANCE_OFFSET] = record.directionalVariance();
            radianceSampleCounts[index] = record.sampleCount();
            radianceLastFrameIndices[index] = record.lastFrameIndex();
            fillConfidence(
                    confidence,
                    radianceConfidenceFloats,
                    radianceConfidenceIntegers,
                    radianceConfidenceGenerations,
                    index
            );
        }

        return new NativeDiffuseGiCacheUpload(
                resolvedSnapshot.cacheGeneration(),
                resolvedSnapshot.dirtyRegionCount(),
                dirtyRegionBatch.firstGeneration(),
                dirtyRegionBatch.lastGeneration(),
                resolvedSnapshot.surfaceRecordCount(),
                resolvedSnapshot.radianceRecordCount(),
                dirtyRegionTypeIds,
                dirtyRegionTypeNames,
                dirtyRegionDimensions,
                dirtyRegionSections,
                dirtyRegionSectionScoped,
                dirtyRegionGenerations,
                surfaceDimensions,
                surfaceKeys,
                surfaceGenerations,
                surfaceMaterialIds,
                surfaceProperties,
                surfaceConfidenceFloats,
                surfaceConfidenceIntegers,
                surfaceConfidenceGenerations,
                radianceDimensions,
                radianceKeys,
                radianceGenerations,
                radianceProperties,
                radianceSampleCounts,
                radianceLastFrameIndices,
                radianceConfidenceFloats,
                radianceConfidenceIntegers,
                radianceConfidenceGenerations
        );
    }

    public long cacheGeneration() {
        return this.cacheGeneration;
    }

    public int dirtyRegionCount() {
        return this.dirtyRegionCount;
    }

    public long firstDirtyRegionGeneration() {
        return this.firstDirtyRegionGeneration;
    }

    public long lastDirtyRegionGeneration() {
        return this.lastDirtyRegionGeneration;
    }

    public int surfaceRecordCount() {
        return this.surfaceRecordCount;
    }

    public int radianceRecordCount() {
        return this.radianceRecordCount;
    }

    public boolean isEmpty() {
        return this.dirtyRegionCount == 0 && this.surfaceRecordCount == 0 && this.radianceRecordCount == 0;
    }

    public boolean hasDirtyRegions() {
        return this.dirtyRegionCount > 0;
    }

    public boolean hasSurfaceRecords() {
        return this.surfaceRecordCount > 0;
    }

    public boolean hasRadianceRecords() {
        return this.radianceRecordCount > 0;
    }

    public int[] dirtyRegionTypeIds() {
        return copy(this.dirtyRegionTypeIds, "dirtyRegionTypeIds");
    }

    public String[] dirtyRegionTypeNames() {
        return copy(this.dirtyRegionTypeNames, "dirtyRegionTypeNames");
    }

    public String[] dirtyRegionDimensions() {
        return copy(this.dirtyRegionDimensions, "dirtyRegionDimensions");
    }

    public int[] dirtyRegionSections() {
        return copy(this.dirtyRegionSections, "dirtyRegionSections");
    }

    public int[] dirtyRegionSectionScoped() {
        return copy(this.dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
    }

    public long[] dirtyRegionGenerations() {
        return copy(this.dirtyRegionGenerations, "dirtyRegionGenerations");
    }

    public String[] surfaceDimensions() {
        return copy(this.surfaceDimensions, "surfaceDimensions");
    }

    public int[] surfaceKeys() {
        return copy(this.surfaceKeys, "surfaceKeys");
    }

    public long[] surfaceGenerations() {
        return copy(this.surfaceGenerations, "surfaceGenerations");
    }

    public int[] surfaceMaterialIds() {
        return copy(this.surfaceMaterialIds, "surfaceMaterialIds");
    }

    public float[] surfaceProperties() {
        return copy(this.surfaceProperties, "surfaceProperties");
    }

    public float[] surfaceConfidenceFloats() {
        return copy(this.surfaceConfidenceFloats, "surfaceConfidenceFloats");
    }

    public int[] surfaceConfidenceIntegers() {
        return copy(this.surfaceConfidenceIntegers, "surfaceConfidenceIntegers");
    }

    public long[] surfaceConfidenceGenerations() {
        return copy(this.surfaceConfidenceGenerations, "surfaceConfidenceGenerations");
    }

    public String[] radianceDimensions() {
        return copy(this.radianceDimensions, "radianceDimensions");
    }

    public int[] radianceKeys() {
        return copy(this.radianceKeys, "radianceKeys");
    }

    public long[] radianceGenerations() {
        return copy(this.radianceGenerations, "radianceGenerations");
    }

    public float[] radianceProperties() {
        return copy(this.radianceProperties, "radianceProperties");
    }

    public int[] radianceSampleCounts() {
        return copy(this.radianceSampleCounts, "radianceSampleCounts");
    }

    public long[] radianceLastFrameIndices() {
        return copy(this.radianceLastFrameIndices, "radianceLastFrameIndices");
    }

    public float[] radianceConfidenceFloats() {
        return copy(this.radianceConfidenceFloats, "radianceConfidenceFloats");
    }

    public int[] radianceConfidenceIntegers() {
        return copy(this.radianceConfidenceIntegers, "radianceConfidenceIntegers");
    }

    public long[] radianceConfidenceGenerations() {
        return copy(this.radianceConfidenceGenerations, "radianceConfidenceGenerations");
    }

    private void validate() {
        requireNonNegative(this.cacheGeneration, "cacheGeneration");
        requireNonNegative(this.dirtyRegionCount, "dirtyRegionCount");
        requireNonNegative(this.firstDirtyRegionGeneration, "firstDirtyRegionGeneration");
        requireNonNegative(this.lastDirtyRegionGeneration, "lastDirtyRegionGeneration");
        if (this.dirtyRegionCount == 0 && (this.firstDirtyRegionGeneration != 0L || this.lastDirtyRegionGeneration != 0L)) {
            throw new IllegalArgumentException("empty dirty region cache upload must use zero generation bounds");
        }
        if (this.dirtyRegionCount > 0) {
            if (this.firstDirtyRegionGeneration == 0L || this.lastDirtyRegionGeneration == 0L) {
                throw new IllegalArgumentException("dirty region cache upload requires positive generation bounds");
            }
            if (this.firstDirtyRegionGeneration > this.lastDirtyRegionGeneration) {
                throw new IllegalArgumentException("firstDirtyRegionGeneration must be <= lastDirtyRegionGeneration");
            }
        }
        requireNonNegative(this.surfaceRecordCount, "surfaceRecordCount");
        requireNonNegative(this.radianceRecordCount, "radianceRecordCount");

        requireMatchingLength(this.dirtyRegionCount, "dirtyRegionTypeIds", this.dirtyRegionTypeIds.length);
        requireMatchingLength(this.dirtyRegionCount, "dirtyRegionTypeNames", this.dirtyRegionTypeNames.length);
        requireMatchingLength(this.dirtyRegionCount, "dirtyRegionDimensions", this.dirtyRegionDimensions.length);
        requireMatchingLength(
                this.dirtyRegionCount * DIRTY_REGION_SECTION_STRIDE,
                "dirtyRegionSections",
                this.dirtyRegionSections.length
        );
        requireMatchingLength(this.dirtyRegionCount, "dirtyRegionSectionScoped", this.dirtyRegionSectionScoped.length);
        requireMatchingLength(this.dirtyRegionCount, "dirtyRegionGenerations", this.dirtyRegionGenerations.length);

        requireMatchingLength(this.surfaceRecordCount, "surfaceDimensions", this.surfaceDimensions.length);
        requireMatchingLength(this.surfaceRecordCount * SURFACE_KEY_STRIDE, "surfaceKeys", this.surfaceKeys.length);
        requireMatchingLength(this.surfaceRecordCount, "surfaceGenerations", this.surfaceGenerations.length);
        requireMatchingLength(this.surfaceRecordCount, "surfaceMaterialIds", this.surfaceMaterialIds.length);
        requireMatchingLength(
                this.surfaceRecordCount * SURFACE_PROPERTY_STRIDE,
                "surfaceProperties",
                this.surfaceProperties.length
        );
        requireMatchingLength(
                this.surfaceRecordCount * CACHE_CONFIDENCE_FLOAT_STRIDE,
                "surfaceConfidenceFloats",
                this.surfaceConfidenceFloats.length
        );
        requireMatchingLength(
                this.surfaceRecordCount * CACHE_CONFIDENCE_INTEGER_STRIDE,
                "surfaceConfidenceIntegers",
                this.surfaceConfidenceIntegers.length
        );
        requireMatchingLength(
                this.surfaceRecordCount * CACHE_CONFIDENCE_GENERATION_STRIDE,
                "surfaceConfidenceGenerations",
                this.surfaceConfidenceGenerations.length
        );

        requireMatchingLength(this.radianceRecordCount, "radianceDimensions", this.radianceDimensions.length);
        requireMatchingLength(this.radianceRecordCount * RADIANCE_KEY_STRIDE, "radianceKeys", this.radianceKeys.length);
        requireMatchingLength(this.radianceRecordCount, "radianceGenerations", this.radianceGenerations.length);
        requireMatchingLength(
                this.radianceRecordCount * RADIANCE_PROPERTY_STRIDE,
                "radianceProperties",
                this.radianceProperties.length
        );
        requireMatchingLength(this.radianceRecordCount, "radianceSampleCounts", this.radianceSampleCounts.length);
        requireMatchingLength(this.radianceRecordCount, "radianceLastFrameIndices", this.radianceLastFrameIndices.length);
        requireMatchingLength(
                this.radianceRecordCount * CACHE_CONFIDENCE_FLOAT_STRIDE,
                "radianceConfidenceFloats",
                this.radianceConfidenceFloats.length
        );
        requireMatchingLength(
                this.radianceRecordCount * CACHE_CONFIDENCE_INTEGER_STRIDE,
                "radianceConfidenceIntegers",
                this.radianceConfidenceIntegers.length
        );
        requireMatchingLength(
                this.radianceRecordCount * CACHE_CONFIDENCE_GENERATION_STRIDE,
                "radianceConfidenceGenerations",
                this.radianceConfidenceGenerations.length
        );

        validateDirtyRegions();
        validateSurfaceRecords();
        validateRadianceRecords();
    }

    private void validateDirtyRegions() {
        long actualFirstGeneration = Long.MAX_VALUE;
        long actualLastGeneration = 0L;
        for (int index = 0; index < this.dirtyRegionCount; index++) {
            if (this.dirtyRegionTypeIds[index] <= 0) {
                throw new IllegalArgumentException("dirtyRegionTypeIds entries must be positive");
            }
            requireText(this.dirtyRegionTypeNames[index], "dirtyRegionTypeNames entries");
            requireText(this.dirtyRegionDimensions[index], "dirtyRegionDimensions entries");
            int sectionScoped = this.dirtyRegionSectionScoped[index];
            if (sectionScoped != 0 && sectionScoped != 1) {
                throw new IllegalArgumentException("dirtyRegionSectionScoped entries must be 0 or 1");
            }
            long generation = this.dirtyRegionGenerations[index];
            if (generation <= 0L) {
                throw new IllegalArgumentException("dirtyRegionGenerations entries must be positive");
            }
            actualFirstGeneration = Math.min(actualFirstGeneration, generation);
            actualLastGeneration = Math.max(actualLastGeneration, generation);
        }
        if (this.dirtyRegionCount > 0
                && (this.firstDirtyRegionGeneration != actualFirstGeneration
                || this.lastDirtyRegionGeneration != actualLastGeneration)) {
            throw new IllegalArgumentException("dirty region generation bounds must match dirty region payloads");
        }
    }

    private void validateSurfaceRecords() {
        for (int index = 0; index < this.surfaceRecordCount; index++) {
            requireText(this.surfaceDimensions[index], "surfaceDimensions entries");
            requireNonNegative(this.surfaceGenerations[index], "surfaceGenerations entries");
            requireNonNegative(this.surfaceMaterialIds[index], "surfaceMaterialIds entries");
            int keyOffset = index * SURFACE_KEY_STRIDE;
            requireNonNegative(this.surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_X_OFFSET], "surface localX entries");
            requireNonNegative(this.surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_Y_OFFSET], "surface localY entries");
            requireNonNegative(this.surfaceKeys[keyOffset + SURFACE_KEY_LOCAL_Z_OFFSET], "surface localZ entries");
            requireNonNegative(this.surfaceKeys[keyOffset + SURFACE_KEY_FACE_ORDINAL_OFFSET], "surface faceOrdinal entries");

            int propertyOffset = index * SURFACE_PROPERTY_STRIDE;
            requireFinite(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_X_OFFSET], "surface normalX entries");
            requireFinite(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_Y_OFFSET], "surface normalY entries");
            requireFinite(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_NORMAL_Z_OFFSET], "surface normalZ entries");
            requireUnit(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_R_OFFSET], "surface albedoR entries");
            requireUnit(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_G_OFFSET], "surface albedoG entries");
            requireUnit(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_ALBEDO_B_OFFSET], "surface albedoB entries");
            requireUnit(this.surfaceProperties[propertyOffset + SURFACE_PROPERTY_ROUGHNESS_OFFSET], "surface roughness entries");
            validateConfidenceArrays(
                    this.surfaceConfidenceFloats,
                    this.surfaceConfidenceIntegers,
                    this.surfaceConfidenceGenerations,
                    index,
                    "surface"
            );
        }
    }

    private void validateRadianceRecords() {
        for (int index = 0; index < this.radianceRecordCount; index++) {
            requireText(this.radianceDimensions[index], "radianceDimensions entries");
            requireNonNegative(this.radianceGenerations[index], "radianceGenerations entries");
            int keyOffset = index * RADIANCE_KEY_STRIDE;
            requireNonNegative(this.radianceKeys[keyOffset + RADIANCE_KEY_CASCADE_OFFSET], "radiance cascade entries");

            int propertyOffset = index * RADIANCE_PROPERTY_STRIDE;
            requireFiniteNonNegative(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_R_OFFSET], "radianceR entries");
            requireFiniteNonNegative(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_G_OFFSET], "radianceG entries");
            requireFiniteNonNegative(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_B_OFFSET], "radianceB entries");
            requireFinite(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_X_OFFSET], "radiance directionX entries");
            requireFinite(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_Y_OFFSET], "radiance directionY entries");
            requireFinite(this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTION_Z_OFFSET], "radiance directionZ entries");
            requireFiniteNonNegative(
                    this.radianceProperties[propertyOffset + RADIANCE_PROPERTY_DIRECTIONAL_VARIANCE_OFFSET],
                    "radiance directionalVariance entries"
            );
            requireNonNegative(this.radianceSampleCounts[index], "radianceSampleCounts entries");
            requireNonNegative(this.radianceLastFrameIndices[index], "radianceLastFrameIndices entries");
            validateConfidenceArrays(
                    this.radianceConfidenceFloats,
                    this.radianceConfidenceIntegers,
                    this.radianceConfidenceGenerations,
                    index,
                    "radiance"
            );
        }
    }

    private static void fillConfidence(
            CacheConfidence confidence,
            float[] confidenceFloats,
            int[] confidenceIntegers,
            long[] confidenceGenerations,
            int index
    ) {
        Objects.requireNonNull(confidence, "confidence");
        int floatOffset = index * CACHE_CONFIDENCE_FLOAT_STRIDE;
        confidenceFloats[floatOffset + CACHE_CONFIDENCE_VALUE_OFFSET] = confidence.confidence();
        confidenceFloats[floatOffset + CACHE_CONFIDENCE_VARIANCE_OFFSET] = confidence.variance();
        int integerOffset = index * CACHE_CONFIDENCE_INTEGER_STRIDE;
        confidenceIntegers[integerOffset + CACHE_CONFIDENCE_SAMPLE_COUNT_OFFSET] = confidence.sampleCount();
        confidenceIntegers[integerOffset + CACHE_CONFIDENCE_DIRTY_OFFSET] = confidence.dirty() ? 1 : 0;
        int generationOffset = index * CACHE_CONFIDENCE_GENERATION_STRIDE;
        confidenceGenerations[generationOffset + CACHE_CONFIDENCE_SOURCE_GENERATION_OFFSET] = confidence.sourceGeneration();
        confidenceGenerations[generationOffset + CACHE_CONFIDENCE_LAST_TOUCHED_FRAME_OFFSET] = confidence.lastTouchedFrame();
    }

    private static void validateConfidenceArrays(
            float[] confidenceFloats,
            int[] confidenceIntegers,
            long[] confidenceGenerations,
            int index,
            String name
    ) {
        int floatOffset = index * CACHE_CONFIDENCE_FLOAT_STRIDE;
        requireUnit(confidenceFloats[floatOffset + CACHE_CONFIDENCE_VALUE_OFFSET], name + " confidence entries");
        requireFiniteNonNegative(confidenceFloats[floatOffset + CACHE_CONFIDENCE_VARIANCE_OFFSET], name + " variance entries");
        int integerOffset = index * CACHE_CONFIDENCE_INTEGER_STRIDE;
        requireNonNegative(confidenceIntegers[integerOffset + CACHE_CONFIDENCE_SAMPLE_COUNT_OFFSET], name + " sample count entries");
        int dirty = confidenceIntegers[integerOffset + CACHE_CONFIDENCE_DIRTY_OFFSET];
        if (dirty != 0 && dirty != 1) {
            throw new IllegalArgumentException(name + " dirty confidence entries must be 0 or 1");
        }
        int generationOffset = index * CACHE_CONFIDENCE_GENERATION_STRIDE;
        requireNonNegative(
                confidenceGenerations[generationOffset + CACHE_CONFIDENCE_SOURCE_GENERATION_OFFSET],
                name + " source generation entries"
        );
        requireNonNegative(
                confidenceGenerations[generationOffset + CACHE_CONFIDENCE_LAST_TOUCHED_FRAME_OFFSET],
                name + " last touched frame entries"
        );
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireUnit(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F || value > 1.0F) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private static void requireFiniteNonNegative(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static float[] copy(float[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
