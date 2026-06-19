package net.lucerna.upload;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public final class NativeLightingDispatchUploadPacket {
    public static final int STAGE_COUNT = 5;
    public static final int DIMENSION_STRIDE = 2;
    public static final int DISPATCH_GROUP_STRIDE = 3;
    public static final int WORKGROUP_SIZE_STRIDE = 3;
    public static final int IO_COUNT_STRIDE = 2;
    public static final int SAMPLE_RAY_STRIDE = 2;
    public static final int CACHE_COUNT_STRIDE = 2;

    public static final int FLAG_VALIDATED = 1;
    public static final int FLAG_PLACEHOLDER = 1 << 1;
    public static final int FLAG_TEMPORAL_HISTORY = 1 << 2;
    public static final int FLAG_DEBUG_OVERLAY = 1 << 3;
    public static final int FLAG_REUSE_ONLY = 1 << 4;

    private final long generation;
    private final int dispatchCount;
    private final long firstDispatchGeneration;
    private final long lastDispatchGeneration;
    private final long worldGeneration;
    private final long materialGeneration;
    private final long sectionGeneration;
    private final long gBufferGeneration;
    private final int[] stageIds;
    private final String[] stageNames;
    private final int[] stageEnabled;
    private final long[] stageGenerations;
    private final int[] stageDimensions;
    private final int[] stageDispatchGroups;
    private final int[] stageWorkgroupSizes;
    private final int[] stageIoCounts;
    private final int[] stageSampleRayCounts;
    private final int[] stageCacheCounts;
    private final long[] stageEstimatedBytes;
    private final int[] stageFlags;

    private NativeLightingDispatchUploadPacket(
            long generation,
            long worldGeneration,
            long materialGeneration,
            long sectionGeneration,
            long gBufferGeneration,
            StageUpload[] stages
    ) {
        Objects.requireNonNull(stages, "stages");
        if (stages.length != STAGE_COUNT) {
            throw new IllegalArgumentException("lighting dispatch packet must include all Phase 5 stages");
        }

        EnumSet<Phase5Stage> seenStages = EnumSet.noneOf(Phase5Stage.class);
        long firstGeneration = Long.MAX_VALUE;
        long lastGeneration = 0L;

        this.dispatchCount = stages.length;
        this.worldGeneration = requireNonNegative(worldGeneration, "worldGeneration");
        this.materialGeneration = requireNonNegative(materialGeneration, "materialGeneration");
        this.sectionGeneration = requireNonNegative(sectionGeneration, "sectionGeneration");
        this.gBufferGeneration = requireNonNegative(gBufferGeneration, "gBufferGeneration");
        this.stageIds = new int[stages.length];
        this.stageNames = new String[stages.length];
        this.stageEnabled = new int[stages.length];
        this.stageGenerations = new long[stages.length];
        this.stageDimensions = new int[stages.length * DIMENSION_STRIDE];
        this.stageDispatchGroups = new int[stages.length * DISPATCH_GROUP_STRIDE];
        this.stageWorkgroupSizes = new int[stages.length * WORKGROUP_SIZE_STRIDE];
        this.stageIoCounts = new int[stages.length * IO_COUNT_STRIDE];
        this.stageSampleRayCounts = new int[stages.length * SAMPLE_RAY_STRIDE];
        this.stageCacheCounts = new int[stages.length * CACHE_COUNT_STRIDE];
        this.stageEstimatedBytes = new long[stages.length];
        this.stageFlags = new int[stages.length];

        for (int index = 0; index < stages.length; index++) {
            StageUpload stage = Objects.requireNonNull(stages[index], "stages must not contain null entries");
            if (!seenStages.add(stage.stage())) {
                throw new IllegalArgumentException("lighting dispatch stages must be unique");
            }

            this.stageIds[index] = stage.stage().id();
            this.stageNames[index] = stage.stage().nativeName();
            this.stageEnabled[index] = stage.enabled() ? 1 : 0;
            this.stageGenerations[index] = stage.generation();
            int dimensionOffset = index * DIMENSION_STRIDE;
            this.stageDimensions[dimensionOffset] = stage.width();
            this.stageDimensions[dimensionOffset + 1] = stage.height();
            int dispatchOffset = index * DISPATCH_GROUP_STRIDE;
            this.stageDispatchGroups[dispatchOffset] = stage.dispatchX();
            this.stageDispatchGroups[dispatchOffset + 1] = stage.dispatchY();
            this.stageDispatchGroups[dispatchOffset + 2] = stage.dispatchZ();
            int workgroupOffset = index * WORKGROUP_SIZE_STRIDE;
            this.stageWorkgroupSizes[workgroupOffset] = stage.workgroupSizeX();
            this.stageWorkgroupSizes[workgroupOffset + 1] = stage.workgroupSizeY();
            this.stageWorkgroupSizes[workgroupOffset + 2] = stage.workgroupSizeZ();
            int ioOffset = index * IO_COUNT_STRIDE;
            this.stageIoCounts[ioOffset] = stage.inputCount();
            this.stageIoCounts[ioOffset + 1] = stage.outputCount();
            int sampleOffset = index * SAMPLE_RAY_STRIDE;
            this.stageSampleRayCounts[sampleOffset] = stage.sampleCount();
            this.stageSampleRayCounts[sampleOffset + 1] = stage.rayCount();
            int cacheOffset = index * CACHE_COUNT_STRIDE;
            this.stageCacheCounts[cacheOffset] = stage.cacheReadCount();
            this.stageCacheCounts[cacheOffset + 1] = stage.cacheWriteCount();
            this.stageEstimatedBytes[index] = stage.estimatedBytes();
            this.stageFlags[index] = stage.flags();

            firstGeneration = Math.min(firstGeneration, stage.generation());
            lastGeneration = Math.max(lastGeneration, stage.generation());
        }

        if (seenStages.size() != STAGE_COUNT) {
            throw new IllegalArgumentException("lighting dispatch packet must include every Phase 5 stage");
        }

        this.firstDispatchGeneration = firstGeneration == Long.MAX_VALUE ? 0L : firstGeneration;
        this.lastDispatchGeneration = lastGeneration;
        this.generation = Math.max(requireNonNegative(generation, "generation"), this.lastDispatchGeneration);
        if (this.generation == 0L) {
            throw new IllegalArgumentException("lighting dispatch packet generation must be positive");
        }
    }

    public static NativeLightingDispatchUploadPacket of(
            long generation,
            NativeUploadStagingMetadata metadata,
            StageUpload directLighting,
            StageUpload diffuseGi,
            StageUpload denoise,
            StageUpload composite,
            StageUpload cache
    ) {
        NativeUploadStagingMetadata resolvedMetadata = metadata == null
                ? NativeUploadStagingMetadata.empty()
                : metadata;
        return new NativeLightingDispatchUploadPacket(
                generation,
                resolvedMetadata.lastWorldGeneration(),
                resolvedMetadata.materialGeneration(),
                resolvedMetadata.sectionGeneration(),
                resolvedMetadata.gBufferStagingGeneration(),
                new StageUpload[]{directLighting, diffuseGi, denoise, composite, cache}
        );
    }

    public long generation() {
        return this.generation;
    }

    public int dispatchCount() {
        return this.dispatchCount;
    }

    public long firstDispatchGeneration() {
        return this.firstDispatchGeneration;
    }

    public long lastDispatchGeneration() {
        return this.lastDispatchGeneration;
    }

    public long worldGeneration() {
        return this.worldGeneration;
    }

    public long materialGeneration() {
        return this.materialGeneration;
    }

    public long sectionGeneration() {
        return this.sectionGeneration;
    }

    public long gBufferGeneration() {
        return this.gBufferGeneration;
    }

    public int enabledStageCount() {
        int enabledCount = 0;
        for (int enabled : this.stageEnabled) {
            if (enabled == 1) {
                enabledCount++;
            }
        }
        return enabledCount;
    }

    public boolean directLightingStageEnabled() {
        return this.stageEnabled(this.stageIndex(Phase5Stage.DIRECT_LIGHTING));
    }

    public int directLightingInputCount() {
        return this.stageInputCount(this.stageIndex(Phase5Stage.DIRECT_LIGHTING));
    }

    public int directLightingOutputCount() {
        return this.stageOutputCount(this.stageIndex(Phase5Stage.DIRECT_LIGHTING));
    }

    public int directLightingCandidateCount() {
        return this.directLightingSampleCount();
    }

    public int directLightingSampleCount() {
        return this.stageSampleCount(this.stageIndex(Phase5Stage.DIRECT_LIGHTING));
    }

    public int directLightingRayCount() {
        return this.stageRayCount(this.stageIndex(Phase5Stage.DIRECT_LIGHTING));
    }

    public int directLightingFlags() {
        return this.stageFlags[this.stageIndex(Phase5Stage.DIRECT_LIGHTING)];
    }

    public int[] stageIds() {
        return copy(this.stageIds, "stageIds");
    }

    public String[] stageNames() {
        return copy(this.stageNames, "stageNames");
    }

    public int[] stageEnabled() {
        return copy(this.stageEnabled, "stageEnabled");
    }

    public long[] stageGenerations() {
        return copy(this.stageGenerations, "stageGenerations");
    }

    public int[] stageDimensions() {
        return copy(this.stageDimensions, "stageDimensions");
    }

    public int[] stageDispatchGroups() {
        return copy(this.stageDispatchGroups, "stageDispatchGroups");
    }

    public int[] stageWorkgroupSizes() {
        return copy(this.stageWorkgroupSizes, "stageWorkgroupSizes");
    }

    public int[] stageIoCounts() {
        return copy(this.stageIoCounts, "stageIoCounts");
    }

    public int[] stageSampleRayCounts() {
        return copy(this.stageSampleRayCounts, "stageSampleRayCounts");
    }

    public int[] stageCacheCounts() {
        return copy(this.stageCacheCounts, "stageCacheCounts");
    }

    public long[] stageEstimatedBytes() {
        return copy(this.stageEstimatedBytes, "stageEstimatedBytes");
    }

    public int[] stageFlags() {
        return copy(this.stageFlags, "stageFlags");
    }

    private int stageIndex(Phase5Stage stage) {
        int id = stage.id();
        for (int index = 0; index < this.stageIds.length; index++) {
            if (this.stageIds[index] == id) {
                return index;
            }
        }
        throw new IllegalStateException("lighting dispatch stage is missing: " + stage.nativeName());
    }

    private boolean stageEnabled(int stageIndex) {
        return this.stageEnabled[stageIndex] == 1;
    }

    private int stageInputCount(int stageIndex) {
        return this.stageIoCounts[stageIndex * IO_COUNT_STRIDE];
    }

    private int stageOutputCount(int stageIndex) {
        return this.stageIoCounts[stageIndex * IO_COUNT_STRIDE + 1];
    }

    private int stageSampleCount(int stageIndex) {
        return this.stageSampleRayCounts[stageIndex * SAMPLE_RAY_STRIDE];
    }

    private int stageRayCount(int stageIndex) {
        return this.stageSampleRayCounts[stageIndex * SAMPLE_RAY_STRIDE + 1];
    }

    private static long requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }

    private static int requirePositiveWhenEnabled(boolean enabled, int value, String name) {
        if (enabled && value <= 0) {
            throw new IllegalArgumentException(name + " must be positive when enabled");
        }
        if (!enabled && value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    public enum Phase5Stage {
        DIRECT_LIGHTING(0, "direct_lighting"),
        DIFFUSE_GI(1, "diffuse_gi"),
        DENOISE(2, "denoise"),
        COMPOSITE(3, "composite"),
        CACHE(4, "cache");

        private final int id;
        private final String nativeName;

        Phase5Stage(int id, String nativeName) {
            this.id = id;
            this.nativeName = nativeName;
        }

        public int id() {
            return this.id;
        }

        public String nativeName() {
            return this.nativeName;
        }
    }

    public record StageUpload(
            Phase5Stage stage,
            boolean enabled,
            long generation,
            int width,
            int height,
            int dispatchX,
            int dispatchY,
            int dispatchZ,
            int workgroupSizeX,
            int workgroupSizeY,
            int workgroupSizeZ,
            int inputCount,
            int outputCount,
            int sampleCount,
            int rayCount,
            int cacheReadCount,
            int cacheWriteCount,
            long estimatedBytes,
            int flags
    ) {
        public StageUpload {
            Objects.requireNonNull(stage, "stage");
            if (generation <= 0L) {
                throw new IllegalArgumentException("lighting dispatch stage generation must be positive");
            }
            int dimensionMinimum = enabled && stage != Phase5Stage.CACHE ? 1 : 0;
            if (width < dimensionMinimum || height < dimensionMinimum) {
                throw new IllegalArgumentException("lighting dispatch dimensions are too small for stage " + stage.nativeName());
            }
            dispatchX = requirePositiveWhenEnabled(enabled, dispatchX, "dispatchX");
            dispatchY = requirePositiveWhenEnabled(enabled, dispatchY, "dispatchY");
            dispatchZ = requirePositiveWhenEnabled(enabled, dispatchZ, "dispatchZ");
            workgroupSizeX = requirePositiveWhenEnabled(enabled, workgroupSizeX, "workgroupSizeX");
            workgroupSizeY = requirePositiveWhenEnabled(enabled, workgroupSizeY, "workgroupSizeY");
            workgroupSizeZ = requirePositiveWhenEnabled(enabled, workgroupSizeZ, "workgroupSizeZ");
            inputCount = requireNonNegative(inputCount, "inputCount");
            outputCount = requireNonNegative(outputCount, "outputCount");
            sampleCount = requireNonNegative(sampleCount, "sampleCount");
            rayCount = requireNonNegative(rayCount, "rayCount");
            cacheReadCount = requireNonNegative(cacheReadCount, "cacheReadCount");
            cacheWriteCount = requireNonNegative(cacheWriteCount, "cacheWriteCount");
            estimatedBytes = requireNonNegative(estimatedBytes, "estimatedBytes");
            if (enabled && stage == Phase5Stage.COMPOSITE && outputCount == 0) {
                throw new IllegalArgumentException("enabled composite lighting dispatch must advertise an output");
            }
            if (enabled && stage == Phase5Stage.CACHE && cacheReadCount == 0 && cacheWriteCount == 0) {
                throw new IllegalArgumentException("enabled cache dispatch must advertise cache reads or writes");
            }
        }
    }
}
