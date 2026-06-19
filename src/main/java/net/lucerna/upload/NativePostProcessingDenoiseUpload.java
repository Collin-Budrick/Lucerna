package net.lucerna.upload;

import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.lighting.post.DenoisePassPlan;
import net.lucerna.render.lighting.post.EdgeAwareDenoiseSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativePostProcessingDenoiseUpload {
    public static final int FLAG_ENABLED = 1;
    public static final int FLAG_READY_FOR_SCHEDULING = 1 << 1;
    public static final int FLAG_EDGE_AWARE = 1 << 2;
    public static final int FLAG_HISTORY_AWARE = 1 << 3;
    public static final int FLAG_TEMPORAL_REUSE_ALLOWED = 1 << 4;
    public static final int FLAG_WRITES_DIFFUSE_OUTPUT = 1 << 5;
    public static final int FLAG_WRITES_REJECTION_MASK = 1 << 6;
    public static final int FLAG_BILATERAL_UPSAMPLE_GI = 1 << 7;
    public static final int FLAG_VALIDATED = 1 << 8;

    private final boolean enabled;
    private final boolean readyForScheduling;
    private final long frameIndex;
    private final long outputGeneration;
    private final long directLightingGeneration;
    private final long diffuseGiGeneration;
    private final long historyGeneration;
    private final int width;
    private final int height;
    private final boolean depthAvailable;
    private final boolean normalsAvailable;
    private final boolean albedoAvailable;
    private final boolean materialIdsAvailable;
    private final boolean emissiveAvailable;
    private final boolean motionVectorsAvailable;
    private final boolean directLightingAvailable;
    private final boolean diffuseGiAvailable;
    private final boolean cacheConfidenceAvailable;
    private final int readResourceCount;
    private final int writeResourceCount;
    private final String[] readResources;
    private final String[] writeResources;
    private final int radiusPixels;
    private final int sampleDiameterPixels;
    private final int iterationCount;
    private final float spatialSigma;
    private final float depthSigma;
    private final float normalSigma;
    private final float luminanceSigma;
    private final float historyBlend;
    private final float historyClampSigma;
    private final boolean bilateralUpsampleGi;
    private final NativePostProcessingRejectionUpload rejection;
    private final boolean validated;
    private final int flags;

    private NativePostProcessingDenoiseUpload(
            boolean enabled,
            boolean readyForScheduling,
            long frameIndex,
            long outputGeneration,
            long directLightingGeneration,
            long diffuseGiGeneration,
            long historyGeneration,
            int width,
            int height,
            boolean depthAvailable,
            boolean normalsAvailable,
            boolean albedoAvailable,
            boolean materialIdsAvailable,
            boolean emissiveAvailable,
            boolean motionVectorsAvailable,
            boolean directLightingAvailable,
            boolean diffuseGiAvailable,
            boolean cacheConfidenceAvailable,
            String[] readResources,
            String[] writeResources,
            int radiusPixels,
            int sampleDiameterPixels,
            int iterationCount,
            float spatialSigma,
            float depthSigma,
            float normalSigma,
            float luminanceSigma,
            float historyBlend,
            float historyClampSigma,
            boolean bilateralUpsampleGi,
            NativePostProcessingRejectionUpload rejection,
            boolean validated,
            int flags
    ) {
        this.enabled = enabled;
        this.readyForScheduling = readyForScheduling;
        this.frameIndex = frameIndex;
        this.outputGeneration = outputGeneration;
        this.directLightingGeneration = directLightingGeneration;
        this.diffuseGiGeneration = diffuseGiGeneration;
        this.historyGeneration = historyGeneration;
        this.width = width;
        this.height = height;
        this.depthAvailable = depthAvailable;
        this.normalsAvailable = normalsAvailable;
        this.albedoAvailable = albedoAvailable;
        this.materialIdsAvailable = materialIdsAvailable;
        this.emissiveAvailable = emissiveAvailable;
        this.motionVectorsAvailable = motionVectorsAvailable;
        this.directLightingAvailable = directLightingAvailable;
        this.diffuseGiAvailable = diffuseGiAvailable;
        this.cacheConfidenceAvailable = cacheConfidenceAvailable;
        this.readResources = copy(readResources, "readResources");
        this.writeResources = copy(writeResources, "writeResources");
        this.readResourceCount = this.readResources.length;
        this.writeResourceCount = this.writeResources.length;
        this.radiusPixels = radiusPixels;
        this.sampleDiameterPixels = sampleDiameterPixels;
        this.iterationCount = iterationCount;
        this.spatialSigma = spatialSigma;
        this.depthSigma = depthSigma;
        this.normalSigma = normalSigma;
        this.luminanceSigma = luminanceSigma;
        this.historyBlend = historyBlend;
        this.historyClampSigma = historyClampSigma;
        this.bilateralUpsampleGi = bilateralUpsampleGi;
        this.rejection = Objects.requireNonNull(rejection, "rejection");
        this.validated = validated;
        this.flags = flags;

        this.validate();
    }

    public static NativePostProcessingDenoiseUpload from(DenoisePassPlan plan) {
        Objects.requireNonNull(plan, "plan");
        EdgeAwareDenoiseSettings settings = plan.settings();
        GBufferDescriptor gBuffer = plan.inputs().gBuffer();
        NativePostProcessingRejectionUpload rejection = NativePostProcessingRejectionUpload.from(plan.historyRejection());
        return new NativePostProcessingDenoiseUpload(
                plan.enabled(),
                plan.readyForScheduling(),
                plan.inputs().frameIndex(),
                plan.outputGeneration(),
                plan.inputs().directLightingGeneration(),
                plan.inputs().diffuseGiGeneration(),
                plan.inputs().historyGeneration(),
                gBuffer.width(),
                gBuffer.height(),
                gBuffer.hasDepth(),
                gBuffer.hasNormals(),
                gBuffer.hasAlbedo(),
                gBuffer.hasMaterialIds(),
                gBuffer.hasEmissive(),
                gBuffer.hasMotionVectors(),
                plan.inputs().directLightingAvailable(),
                plan.inputs().diffuseGiAvailable(),
                plan.inputs().cacheConfidenceAvailable(),
                toArray(plan.readResources()),
                toArray(plan.writeResources()),
                settings.radiusPixels(),
                settings.sampleDiameterPixels(),
                settings.iterationCount(),
                settings.spatialSigma(),
                settings.depthSigma(),
                settings.normalSigma(),
                settings.luminanceSigma(),
                settings.historyBlend(),
                settings.historyClampSigma(),
                settings.bilateralUpsampleGi(),
                rejection,
                plan.validationReport().valid(),
                flags(plan, settings)
        );
    }

    public boolean enabled() {
        return this.enabled;
    }

    public boolean readyForScheduling() {
        return this.readyForScheduling;
    }

    public long frameIndex() {
        return this.frameIndex;
    }

    public long outputGeneration() {
        return this.outputGeneration;
    }

    public long directLightingGeneration() {
        return this.directLightingGeneration;
    }

    public long diffuseGiGeneration() {
        return this.diffuseGiGeneration;
    }

    public long historyGeneration() {
        return this.historyGeneration;
    }

    public long maxInputGeneration() {
        return Math.max(this.directLightingGeneration, Math.max(this.diffuseGiGeneration, this.historyGeneration));
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean depthAvailable() {
        return this.depthAvailable;
    }

    public boolean normalsAvailable() {
        return this.normalsAvailable;
    }

    public boolean albedoAvailable() {
        return this.albedoAvailable;
    }

    public boolean materialIdsAvailable() {
        return this.materialIdsAvailable;
    }

    public boolean emissiveAvailable() {
        return this.emissiveAvailable;
    }

    public boolean motionVectorsAvailable() {
        return this.motionVectorsAvailable;
    }

    public boolean directLightingAvailable() {
        return this.directLightingAvailable;
    }

    public boolean diffuseGiAvailable() {
        return this.diffuseGiAvailable;
    }

    public boolean cacheConfidenceAvailable() {
        return this.cacheConfidenceAvailable;
    }

    public int readResourceCount() {
        return this.readResourceCount;
    }

    public int writeResourceCount() {
        return this.writeResourceCount;
    }

    public String[] readResources() {
        return copy(this.readResources, "readResources");
    }

    public String[] writeResources() {
        return copy(this.writeResources, "writeResources");
    }

    public int radiusPixels() {
        return this.radiusPixels;
    }

    public int sampleDiameterPixels() {
        return this.sampleDiameterPixels;
    }

    public int iterationCount() {
        return this.iterationCount;
    }

    public float spatialSigma() {
        return this.spatialSigma;
    }

    public float depthSigma() {
        return this.depthSigma;
    }

    public float normalSigma() {
        return this.normalSigma;
    }

    public float luminanceSigma() {
        return this.luminanceSigma;
    }

    public float historyBlend() {
        return this.historyBlend;
    }

    public float historyClampSigma() {
        return this.historyClampSigma;
    }

    public boolean bilateralUpsampleGi() {
        return this.bilateralUpsampleGi;
    }

    public NativePostProcessingRejectionUpload rejection() {
        return this.rejection;
    }

    public boolean validated() {
        return this.validated;
    }

    public int flags() {
        return this.flags;
    }

    private void validate() {
        requireNonNegative(this.frameIndex, "frameIndex");
        requireNonNegative(this.outputGeneration, "outputGeneration");
        requireNonNegative(this.directLightingGeneration, "directLightingGeneration");
        requireNonNegative(this.diffuseGiGeneration, "diffuseGiGeneration");
        requireNonNegative(this.historyGeneration, "historyGeneration");
        requireNonNegative(this.width, "width");
        requireNonNegative(this.height, "height");
        requireNonNegative(this.radiusPixels, "radiusPixels");
        requireNonNegative(this.sampleDiameterPixels, "sampleDiameterPixels");
        requireNonNegative(this.iterationCount, "iterationCount");
        if (this.sampleDiameterPixels != this.radiusPixels * 2 + 1) {
            throw new IllegalArgumentException("sampleDiameterPixels must match radiusPixels");
        }
        requireFiniteNonNegative(this.spatialSigma, "spatialSigma");
        requireFiniteNonNegative(this.depthSigma, "depthSigma");
        requireFiniteNonNegative(this.normalSigma, "normalSigma");
        requireFiniteNonNegative(this.luminanceSigma, "luminanceSigma");
        requireFiniteNonNegative(this.historyBlend, "historyBlend");
        requireFiniteNonNegative(this.historyClampSigma, "historyClampSigma");
        if (this.normalSigma > 1.0F) {
            throw new IllegalArgumentException("normalSigma must be between 0 and 1");
        }
        if (this.historyBlend > 1.0F) {
            throw new IllegalArgumentException("historyBlend must be between 0 and 1");
        }
        requireTextEntries(this.readResources, "readResources");
        requireTextEntries(this.writeResources, "writeResources");
        if (!this.enabled && this.writeResourceCount != 0) {
            throw new IllegalArgumentException("disabled denoise upload must not advertise writes");
        }
        if (this.readyForScheduling && (!this.enabled || this.width == 0 || this.height == 0)) {
            throw new IllegalArgumentException("ready denoise upload requires enabled non-zero dimensions");
        }
        int expectedFlags = flags(
                this.enabled,
                this.readyForScheduling,
                this.radiusPixels > 0 && this.iterationCount > 0 && this.enabled,
                this.enabled && this.historyBlend > 0.0F,
                this.rejection.temporalReuseAllowed(),
                this.enabled,
                this.rejection.writesRejectionMask(),
                this.bilateralUpsampleGi,
                this.validated
        );
        if (this.flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match denoise upload state");
        }
    }

    private static int flags(DenoisePassPlan plan, EdgeAwareDenoiseSettings settings) {
        return flags(
                plan.enabled(),
                plan.readyForScheduling(),
                settings.edgeAware(),
                settings.historyAware(),
                plan.temporalReuseAllowed(),
                plan.writesDiffuseOutput(),
                plan.writesRejectionMask(),
                settings.bilateralUpsampleGi(),
                plan.validationReport().valid()
        );
    }

    private static int flags(
            boolean enabled,
            boolean readyForScheduling,
            boolean edgeAware,
            boolean historyAware,
            boolean temporalReuseAllowed,
            boolean writesDiffuseOutput,
            boolean writesRejectionMask,
            boolean bilateralUpsampleGi,
            boolean validated
    ) {
        int flags = 0;
        if (enabled) {
            flags |= FLAG_ENABLED;
        }
        if (readyForScheduling) {
            flags |= FLAG_READY_FOR_SCHEDULING;
        }
        if (edgeAware) {
            flags |= FLAG_EDGE_AWARE;
        }
        if (historyAware) {
            flags |= FLAG_HISTORY_AWARE;
        }
        if (temporalReuseAllowed) {
            flags |= FLAG_TEMPORAL_REUSE_ALLOWED;
        }
        if (writesDiffuseOutput) {
            flags |= FLAG_WRITES_DIFFUSE_OUTPUT;
        }
        if (writesRejectionMask) {
            flags |= FLAG_WRITES_REJECTION_MASK;
        }
        if (bilateralUpsampleGi) {
            flags |= FLAG_BILATERAL_UPSAMPLE_GI;
        }
        if (validated) {
            flags |= FLAG_VALIDATED;
        }
        return flags;
    }

    private static String[] toArray(List<String> values) {
        Objects.requireNonNull(values, "values");
        return values.toArray(String[]::new);
    }

    private static void requireTextEntries(String[] values, String name) {
        for (String value : values) {
            Objects.requireNonNull(value, name + " must not contain null entries");
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not contain blank entries");
            }
        }
    }

    private static void requireFiniteNonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
