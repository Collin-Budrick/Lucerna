package net.lucerna.upload;

import net.lucerna.render.lighting.post.HistoryRejectionPlan;
import net.lucerna.render.lighting.post.HistoryRejectionSettings;

import java.util.Objects;

public record NativePostProcessingRejectionUpload(
        boolean enabled,
        long frameIndex,
        long previousFrameIndex,
        int availableInputCount,
        int availableInputMask,
        boolean previousDepthAvailable,
        boolean previousNormalRoughnessAvailable,
        boolean previousLightingAvailable,
        boolean motionHistoryAvailable,
        boolean matrixHistoryReusable,
        boolean historyInputsComplete,
        boolean temporalReuseAllowed,
        boolean writesRejectionMask,
        float depthThreshold,
        float normalDotThreshold,
        float motionPixelThreshold,
        float luminanceThreshold,
        float minHistoryConfidence,
        float reactiveMaskThreshold,
        int historyRepairRadiusPixels,
        boolean rejectDirtyRegions,
        int flags
) {
    public static final int INPUT_PREVIOUS_DEPTH = 1;
    public static final int INPUT_PREVIOUS_NORMAL_ROUGHNESS = 1 << 1;
    public static final int INPUT_PREVIOUS_LIGHTING = 1 << 2;
    public static final int INPUT_MOTION_HISTORY = 1 << 3;
    public static final int INPUT_MATRIX_HISTORY = 1 << 4;
    public static final int INPUT_MASK_HISTORY_RESOURCES = INPUT_PREVIOUS_DEPTH
            | INPUT_PREVIOUS_NORMAL_ROUGHNESS
            | INPUT_PREVIOUS_LIGHTING
            | INPUT_MOTION_HISTORY;
    public static final int INPUT_MASK_ALL = INPUT_PREVIOUS_DEPTH
            | INPUT_PREVIOUS_NORMAL_ROUGHNESS
            | INPUT_PREVIOUS_LIGHTING
            | INPUT_MOTION_HISTORY
            | INPUT_MATRIX_HISTORY;

    public static final int FLAG_ENABLED = 1;
    public static final int FLAG_INPUTS_COMPLETE = 1 << 1;
    public static final int FLAG_TEMPORAL_REUSE_ALLOWED = 1 << 2;
    public static final int FLAG_WRITES_REJECTION_MASK = 1 << 3;
    public static final int FLAG_MATRIX_HISTORY_REUSABLE = 1 << 4;
    public static final int FLAG_REPAIRS_REJECTED_HISTORY = 1 << 5;
    public static final int FLAG_REJECTS_DIRTY_REGIONS = 1 << 6;

    public NativePostProcessingRejectionUpload {
        requireNonNegative(frameIndex, "frameIndex");
        requireNonNegative(previousFrameIndex, "previousFrameIndex");
        if ((availableInputMask & ~INPUT_MASK_ALL) != 0) {
            throw new IllegalArgumentException("availableInputMask contains unsupported bits");
        }
        int resolvedInputCount = Integer.bitCount(availableInputMask);
        if (availableInputCount != resolvedInputCount) {
            throw new IllegalArgumentException("availableInputCount must match availableInputMask");
        }
        requireFiniteNonNegative(depthThreshold, "depthThreshold");
        requireFiniteNonNegative(normalDotThreshold, "normalDotThreshold");
        requireFiniteNonNegative(motionPixelThreshold, "motionPixelThreshold");
        requireFiniteNonNegative(luminanceThreshold, "luminanceThreshold");
        requireFiniteNonNegative(minHistoryConfidence, "minHistoryConfidence");
        requireFiniteNonNegative(reactiveMaskThreshold, "reactiveMaskThreshold");
        requireNonNegative(historyRepairRadiusPixels, "historyRepairRadiusPixels");
        if (normalDotThreshold > 1.0F) {
            throw new IllegalArgumentException("normalDotThreshold must be between 0 and 1");
        }
        if (minHistoryConfidence > 1.0F) {
            throw new IllegalArgumentException("minHistoryConfidence must be between 0 and 1");
        }
        if (reactiveMaskThreshold > 1.0F) {
            throw new IllegalArgumentException("reactiveMaskThreshold must be between 0 and 1");
        }
        if (enabled && temporalReuseAllowed && !historyInputsComplete) {
            throw new IllegalArgumentException("temporal reuse requires complete history inputs");
        }
        if (enabled
                && historyInputsComplete
                && (availableInputMask & INPUT_MASK_HISTORY_RESOURCES) != INPUT_MASK_HISTORY_RESOURCES) {
            throw new IllegalArgumentException("complete history inputs require all previous resource input bits");
        }
        if (previousDepthAvailable != hasInput(availableInputMask, INPUT_PREVIOUS_DEPTH)
                || previousNormalRoughnessAvailable != hasInput(availableInputMask, INPUT_PREVIOUS_NORMAL_ROUGHNESS)
                || previousLightingAvailable != hasInput(availableInputMask, INPUT_PREVIOUS_LIGHTING)
                || motionHistoryAvailable != hasInput(availableInputMask, INPUT_MOTION_HISTORY)
                || matrixHistoryReusable != hasInput(availableInputMask, INPUT_MATRIX_HISTORY)) {
            throw new IllegalArgumentException("rejection input booleans must match availableInputMask");
        }
        int expectedFlags = flags(
                enabled,
                historyInputsComplete,
                temporalReuseAllowed,
                writesRejectionMask,
                matrixHistoryReusable,
                enabled && historyRepairRadiusPixels > 0,
                rejectDirtyRegions
        );
        if (flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match rejection state");
        }
    }

    public static NativePostProcessingRejectionUpload from(HistoryRejectionPlan plan) {
        Objects.requireNonNull(plan, "plan");
        HistoryRejectionSettings settings = plan.settings();
        int availableInputMask = availableInputMask(plan);
        boolean repairsRejectedHistory = settings.repairsRejectedHistory();
        return new NativePostProcessingRejectionUpload(
                plan.enabled(),
                plan.frameIndex(),
                plan.previousFrameIndex(),
                Integer.bitCount(availableInputMask),
                availableInputMask,
                plan.previousDepthAvailable(),
                plan.previousNormalRoughnessAvailable(),
                plan.previousLightingAvailable(),
                plan.motionHistoryAvailable(),
                plan.matrixHistoryReusable(),
                plan.historyInputsComplete(),
                plan.temporalReuseAllowed(),
                plan.writesRejectionMask(),
                settings.depthThreshold(),
                settings.normalDotThreshold(),
                settings.motionPixelThreshold(),
                settings.luminanceThreshold(),
                settings.minHistoryConfidence(),
                settings.reactiveMaskThreshold(),
                settings.historyRepairRadiusPixels(),
                settings.rejectDirtyRegions(),
                flags(
                        plan.enabled(),
                        plan.historyInputsComplete(),
                        plan.temporalReuseAllowed(),
                        plan.writesRejectionMask(),
                        plan.matrixHistoryReusable(),
                        repairsRejectedHistory,
                        settings.rejectDirtyRegions()
                )
        );
    }

    private static int availableInputMask(HistoryRejectionPlan plan) {
        int mask = 0;
        if (plan.previousDepthAvailable()) {
            mask |= INPUT_PREVIOUS_DEPTH;
        }
        if (plan.previousNormalRoughnessAvailable()) {
            mask |= INPUT_PREVIOUS_NORMAL_ROUGHNESS;
        }
        if (plan.previousLightingAvailable()) {
            mask |= INPUT_PREVIOUS_LIGHTING;
        }
        if (plan.motionHistoryAvailable()) {
            mask |= INPUT_MOTION_HISTORY;
        }
        if (plan.matrixHistoryReusable()) {
            mask |= INPUT_MATRIX_HISTORY;
        }
        return mask;
    }

    private static int flags(
            boolean enabled,
            boolean historyInputsComplete,
            boolean temporalReuseAllowed,
            boolean writesRejectionMask,
            boolean matrixHistoryReusable,
            boolean repairsRejectedHistory,
            boolean rejectDirtyRegions
    ) {
        int flags = 0;
        if (enabled) {
            flags |= FLAG_ENABLED;
        }
        if (historyInputsComplete) {
            flags |= FLAG_INPUTS_COMPLETE;
        }
        if (temporalReuseAllowed) {
            flags |= FLAG_TEMPORAL_REUSE_ALLOWED;
        }
        if (writesRejectionMask) {
            flags |= FLAG_WRITES_REJECTION_MASK;
        }
        if (matrixHistoryReusable) {
            flags |= FLAG_MATRIX_HISTORY_REUSABLE;
        }
        if (repairsRejectedHistory) {
            flags |= FLAG_REPAIRS_REJECTED_HISTORY;
        }
        if (rejectDirtyRegions) {
            flags |= FLAG_REJECTS_DIRTY_REGIONS;
        }
        return flags;
    }

    private static boolean hasInput(int mask, int input) {
        return (mask & input) == input;
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
}
