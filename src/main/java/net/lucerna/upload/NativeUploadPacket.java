package net.lucerna.upload;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativeUploadPacket {
    public static final int MATERIAL_PROPERTY_STRIDE = 6;
    public static final int MATERIAL_PROPERTY_ROUGHNESS_OFFSET = 0;
    public static final int MATERIAL_PROPERTY_METALNESS_OFFSET = 1;
    public static final int MATERIAL_PROPERTY_EMISSIVE_RED_OFFSET = 2;
    public static final int MATERIAL_PROPERTY_EMISSIVE_GREEN_OFFSET = 3;
    public static final int MATERIAL_PROPERTY_EMISSIVE_BLUE_OFFSET = 4;
    public static final int MATERIAL_PROPERTY_EMISSIVE_STRENGTH_OFFSET = 5;

    private final long generation;
    private final int dirtyRegionCount;
    private final int materialUpdateCount;
    private final long firstWorldGeneration;
    private final long lastWorldGeneration;
    private final long materialGeneration;
    private final int[] dirtyRegionTypeIds;
    private final String[] dirtyRegionDimensions;
    private final int[] dirtyRegionSectionXs;
    private final int[] dirtyRegionSectionYs;
    private final int[] dirtyRegionSectionZs;
    private final int[] dirtyRegionSectionScoped;
    private final long[] dirtyRegionGenerations;
    private final int[] materialIds;
    private final long[] materialGenerations;
    private final String[] materialBlockIds;
    private final int[] materialFaceIds;
    private final int[] materialAlbedoTextureIndices;
    private final float[] materialProperties;
    private final int[] materialFlags;

    private NativeUploadPacket(
            long generation,
            int dirtyRegionCount,
            int materialUpdateCount,
            long firstWorldGeneration,
            long lastWorldGeneration,
            long materialGeneration,
            int[] dirtyRegionTypeIds,
            String[] dirtyRegionDimensions,
            int[] dirtyRegionSectionXs,
            int[] dirtyRegionSectionYs,
            int[] dirtyRegionSectionZs,
            int[] dirtyRegionSectionScoped,
            long[] dirtyRegionGenerations,
            int[] materialIds,
            long[] materialGenerations,
            String[] materialBlockIds,
            int[] materialFaceIds,
            int[] materialAlbedoTextureIndices,
            float[] materialProperties,
            int[] materialFlags
    ) {
        this.generation = generation;
        this.dirtyRegionCount = dirtyRegionCount;
        this.materialUpdateCount = materialUpdateCount;
        this.firstWorldGeneration = firstWorldGeneration;
        this.lastWorldGeneration = lastWorldGeneration;
        this.materialGeneration = materialGeneration;
        this.dirtyRegionTypeIds = copy(dirtyRegionTypeIds, "dirtyRegionTypeIds");
        this.dirtyRegionDimensions = copy(dirtyRegionDimensions, "dirtyRegionDimensions");
        this.dirtyRegionSectionXs = copy(dirtyRegionSectionXs, "dirtyRegionSectionXs");
        this.dirtyRegionSectionYs = copy(dirtyRegionSectionYs, "dirtyRegionSectionYs");
        this.dirtyRegionSectionZs = copy(dirtyRegionSectionZs, "dirtyRegionSectionZs");
        this.dirtyRegionSectionScoped = copy(dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
        this.dirtyRegionGenerations = copy(dirtyRegionGenerations, "dirtyRegionGenerations");
        this.materialIds = copy(materialIds, "materialIds");
        this.materialGenerations = copy(materialGenerations, "materialGenerations");
        this.materialBlockIds = copy(materialBlockIds, "materialBlockIds");
        this.materialFaceIds = copy(materialFaceIds, "materialFaceIds");
        this.materialAlbedoTextureIndices = copy(materialAlbedoTextureIndices, "materialAlbedoTextureIndices");
        this.materialProperties = copy(materialProperties, "materialProperties");
        this.materialFlags = copy(materialFlags, "materialFlags");

        this.validate();
    }

    public static NativeUploadPacket from(NativeUploadBatch batch) {
        Objects.requireNonNull(batch, "batch");

        List<NativeDirtyRegionUpload> dirtyRegions = batch.dirtyRegions();
        int[] dirtyRegionTypeIds = new int[dirtyRegions.size()];
        String[] dirtyRegionDimensions = new String[dirtyRegions.size()];
        int[] dirtyRegionSectionXs = new int[dirtyRegions.size()];
        int[] dirtyRegionSectionYs = new int[dirtyRegions.size()];
        int[] dirtyRegionSectionZs = new int[dirtyRegions.size()];
        int[] dirtyRegionSectionScoped = new int[dirtyRegions.size()];
        long[] dirtyRegionGenerations = new long[dirtyRegions.size()];

        for (int index = 0; index < dirtyRegions.size(); index++) {
            NativeDirtyRegionUpload upload = dirtyRegions.get(index);
            dirtyRegionTypeIds[index] = upload.typeId();
            dirtyRegionDimensions[index] = upload.dimension();
            dirtyRegionSectionXs[index] = upload.sectionX();
            dirtyRegionSectionYs[index] = upload.sectionY();
            dirtyRegionSectionZs[index] = upload.sectionZ();
            dirtyRegionSectionScoped[index] = upload.sectionScoped() ? 1 : 0;
            dirtyRegionGenerations[index] = upload.generation();
        }

        List<NativeMaterialUpload> materialUpdates = batch.materialUpdates();
        int[] materialIds = new int[materialUpdates.size()];
        long[] materialGenerations = new long[materialUpdates.size()];
        String[] materialBlockIds = new String[materialUpdates.size()];
        int[] materialFaceIds = new int[materialUpdates.size()];
        int[] materialAlbedoTextureIndices = new int[materialUpdates.size()];
        float[] materialProperties = new float[materialUpdates.size() * MATERIAL_PROPERTY_STRIDE];
        int[] materialFlags = new int[materialUpdates.size()];

        for (int index = 0; index < materialUpdates.size(); index++) {
            NativeMaterialUpload upload = materialUpdates.get(index);
            int propertyOffset = index * MATERIAL_PROPERTY_STRIDE;
            materialIds[index] = upload.materialId();
            materialGenerations[index] = upload.generation();
            materialBlockIds[index] = upload.blockId();
            materialFaceIds[index] = upload.faceId();
            materialAlbedoTextureIndices[index] = upload.albedoTextureIndex();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_ROUGHNESS_OFFSET] = upload.roughness();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_METALNESS_OFFSET] = upload.metalness();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_EMISSIVE_RED_OFFSET] = upload.emissiveRed();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_EMISSIVE_GREEN_OFFSET] = upload.emissiveGreen();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_EMISSIVE_BLUE_OFFSET] = upload.emissiveBlue();
            materialProperties[propertyOffset + MATERIAL_PROPERTY_EMISSIVE_STRENGTH_OFFSET] = upload.emissiveStrength();
            materialFlags[index] = upload.flags();
        }

        return new NativeUploadPacket(
                batch.generation(),
                batch.dirtyRegionCount(),
                batch.materialUpdateCount(),
                batch.firstWorldGeneration(),
                batch.lastWorldGeneration(),
                batch.materialGeneration(),
                dirtyRegionTypeIds,
                dirtyRegionDimensions,
                dirtyRegionSectionXs,
                dirtyRegionSectionYs,
                dirtyRegionSectionZs,
                dirtyRegionSectionScoped,
                dirtyRegionGenerations,
                materialIds,
                materialGenerations,
                materialBlockIds,
                materialFaceIds,
                materialAlbedoTextureIndices,
                materialProperties,
                materialFlags
        );
    }

    public long generation() {
        return this.generation;
    }

    public int dirtyRegionCount() {
        return this.dirtyRegionCount;
    }

    public int materialUpdateCount() {
        return this.materialUpdateCount;
    }

    public long firstWorldGeneration() {
        return this.firstWorldGeneration;
    }

    public long lastWorldGeneration() {
        return this.lastWorldGeneration;
    }

    public long materialGeneration() {
        return this.materialGeneration;
    }

    public int dirtyRegionPayloadCount() {
        return this.dirtyRegionTypeIds.length;
    }

    public int materialPayloadCount() {
        return this.materialIds.length;
    }

    public boolean isEmpty() {
        return this.dirtyRegionCount == 0 && this.materialUpdateCount == 0;
    }

    public boolean hasPayloads() {
        return this.dirtyRegionTypeIds.length > 0 || this.materialIds.length > 0;
    }

    public int[] dirtyRegionTypeIds() {
        return copy(this.dirtyRegionTypeIds, "dirtyRegionTypeIds");
    }

    public String[] dirtyRegionDimensions() {
        return copy(this.dirtyRegionDimensions, "dirtyRegionDimensions");
    }

    public int[] dirtyRegionSectionXs() {
        return copy(this.dirtyRegionSectionXs, "dirtyRegionSectionXs");
    }

    public int[] dirtyRegionSectionYs() {
        return copy(this.dirtyRegionSectionYs, "dirtyRegionSectionYs");
    }

    public int[] dirtyRegionSectionZs() {
        return copy(this.dirtyRegionSectionZs, "dirtyRegionSectionZs");
    }

    public int[] dirtyRegionSectionScoped() {
        return copy(this.dirtyRegionSectionScoped, "dirtyRegionSectionScoped");
    }

    public long[] dirtyRegionGenerations() {
        return copy(this.dirtyRegionGenerations, "dirtyRegionGenerations");
    }

    public int[] materialIds() {
        return copy(this.materialIds, "materialIds");
    }

    public long[] materialGenerations() {
        return copy(this.materialGenerations, "materialGenerations");
    }

    public String[] materialBlockIds() {
        return copy(this.materialBlockIds, "materialBlockIds");
    }

    public int[] materialFaceIds() {
        return copy(this.materialFaceIds, "materialFaceIds");
    }

    public int[] materialAlbedoTextureIndices() {
        return copy(this.materialAlbedoTextureIndices, "materialAlbedoTextureIndices");
    }

    public float[] materialProperties() {
        return copy(this.materialProperties, "materialProperties");
    }

    public int[] materialFlags() {
        return copy(this.materialFlags, "materialFlags");
    }

    private void validate() {
        if (this.generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        if (this.dirtyRegionCount < 0) {
            throw new IllegalArgumentException("dirtyRegionCount must be non-negative");
        }
        if (this.materialUpdateCount < 0) {
            throw new IllegalArgumentException("materialUpdateCount must be non-negative");
        }
        if (this.firstWorldGeneration < 0 || this.lastWorldGeneration < 0 || this.materialGeneration < 0) {
            throw new IllegalArgumentException("generation bounds must be non-negative");
        }
        if (this.firstWorldGeneration > this.lastWorldGeneration) {
            throw new IllegalArgumentException("firstWorldGeneration must be less than or equal to lastWorldGeneration");
        }

        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionDimensions", this.dirtyRegionDimensions.length);
        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionSectionXs", this.dirtyRegionSectionXs.length);
        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionSectionYs", this.dirtyRegionSectionYs.length);
        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionSectionZs", this.dirtyRegionSectionZs.length);
        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionSectionScoped", this.dirtyRegionSectionScoped.length);
        requireMatchingLength(this.dirtyRegionTypeIds.length, "dirtyRegionGenerations", this.dirtyRegionGenerations.length);
        if (this.dirtyRegionTypeIds.length > this.dirtyRegionCount) {
            throw new IllegalArgumentException("dirty region payload count cannot exceed dirtyRegionCount");
        }

        requireMatchingLength(this.materialIds.length, "materialGenerations", this.materialGenerations.length);
        requireMatchingLength(this.materialIds.length, "materialBlockIds", this.materialBlockIds.length);
        requireMatchingLength(this.materialIds.length, "materialFaceIds", this.materialFaceIds.length);
        requireMatchingLength(this.materialIds.length, "materialAlbedoTextureIndices", this.materialAlbedoTextureIndices.length);
        requireMatchingLength(this.materialIds.length, "materialFlags", this.materialFlags.length);
        requireMatchingLength(this.materialIds.length * MATERIAL_PROPERTY_STRIDE, "materialProperties", this.materialProperties.length);
        if (this.materialIds.length > this.materialUpdateCount) {
            throw new IllegalArgumentException("material payload count cannot exceed materialUpdateCount");
        }

        for (String dimension : this.dirtyRegionDimensions) {
            Objects.requireNonNull(dimension, "dirtyRegionDimensions must not contain null entries");
        }
        for (String blockId : this.materialBlockIds) {
            Objects.requireNonNull(blockId, "materialBlockIds must not contain null entries");
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
