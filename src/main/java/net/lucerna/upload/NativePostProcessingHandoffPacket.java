package net.lucerna.upload;

import net.lucerna.render.lighting.post.PostProcessingPipelinePlan;

import java.util.Objects;

public final class NativePostProcessingHandoffPacket {
    public static final int FLAG_VALIDATED = 1;
    public static final int FLAG_DENOISE_SCHEDULED = 1 << 1;
    public static final int FLAG_COMPOSITE_READY = 1 << 2;
    public static final int FLAG_TEMPORAL_HISTORY = 1 << 3;
    public static final int FLAG_DEBUG_OVERLAY = 1 << 4;
    public static final int FLAG_HAS_WARNINGS = 1 << 5;
    public static final int FLAG_HAS_ERRORS = 1 << 6;

    private final long generation;
    private final long frameIndex;
    private final long outputGeneration;
    private final NativePostProcessingDenoiseUpload denoise;
    private final NativePostProcessingCompositeUpload composite;
    private final NativePostProcessingValidationUpload validation;
    private final int flags;

    private NativePostProcessingHandoffPacket(
            long generation,
            long frameIndex,
            long outputGeneration,
            NativePostProcessingDenoiseUpload denoise,
            NativePostProcessingCompositeUpload composite,
            NativePostProcessingValidationUpload validation,
            int flags
    ) {
        this.generation = generation;
        this.frameIndex = frameIndex;
        this.outputGeneration = outputGeneration;
        this.denoise = Objects.requireNonNull(denoise, "denoise");
        this.composite = Objects.requireNonNull(composite, "composite");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.flags = flags;

        this.validate();
    }

    public static NativePostProcessingHandoffPacket from(PostProcessingPipelinePlan plan) {
        Objects.requireNonNull(plan, "plan");
        NativePostProcessingDenoiseUpload denoise = NativePostProcessingDenoiseUpload.from(plan.denoisePlan());
        NativePostProcessingCompositeUpload composite = NativePostProcessingCompositeUpload.from(plan.compositeHandoff());
        NativePostProcessingValidationUpload validation = NativePostProcessingValidationUpload.from(plan);
        long frameIndex = Math.max(denoise.frameIndex(), composite.frameIndex());
        long outputGeneration = Math.max(denoise.outputGeneration(), composite.sourceGeneration());
        long generation = max(
                frameIndex,
                outputGeneration,
                denoise.maxInputGeneration()
        );
        return new NativePostProcessingHandoffPacket(
                generation,
                frameIndex,
                outputGeneration,
                denoise,
                composite,
                validation,
                flags(denoise, composite, validation)
        );
    }

    public long generation() {
        return this.generation;
    }

    public long frameIndex() {
        return this.frameIndex;
    }

    public long outputGeneration() {
        return this.outputGeneration;
    }

    public NativePostProcessingDenoiseUpload denoise() {
        return this.denoise;
    }

    public NativePostProcessingCompositeUpload composite() {
        return this.composite;
    }

    public NativePostProcessingValidationUpload validation() {
        return this.validation;
    }

    public int flags() {
        return this.flags;
    }

    public boolean readyForNativeHandoff() {
        return this.validation.readyForNativeHandoff();
    }

    private void validate() {
        requireNonNegative(this.generation, "generation");
        requireNonNegative(this.frameIndex, "frameIndex");
        requireNonNegative(this.outputGeneration, "outputGeneration");
        if (this.generation < this.frameIndex || this.generation < this.outputGeneration) {
            throw new IllegalArgumentException("generation must cover frame and output generations");
        }
        if (this.frameIndex < this.denoise.frameIndex() || this.frameIndex < this.composite.frameIndex()) {
            throw new IllegalArgumentException("frameIndex must cover denoise and composite frames");
        }
        if (this.outputGeneration < this.denoise.outputGeneration()
                || this.outputGeneration < this.composite.sourceGeneration()) {
            throw new IllegalArgumentException("outputGeneration must cover denoise and composite outputs");
        }
        int expectedFlags = flags(this.denoise, this.composite, this.validation);
        if (this.flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match post-processing handoff state");
        }
    }

    private static int flags(
            NativePostProcessingDenoiseUpload denoise,
            NativePostProcessingCompositeUpload composite,
            NativePostProcessingValidationUpload validation
    ) {
        int flags = 0;
        if (validation.valid()) {
            flags |= FLAG_VALIDATED;
        }
        if (denoise.readyForScheduling()) {
            flags |= FLAG_DENOISE_SCHEDULED;
        }
        if (composite.readyForWorldColorHandoff()) {
            flags |= FLAG_COMPOSITE_READY;
        }
        if (denoise.rejection().temporalReuseAllowed()) {
            flags |= FLAG_TEMPORAL_HISTORY;
        }
        if (composite.debugOverlayAvailable()) {
            flags |= FLAG_DEBUG_OVERLAY;
        }
        if (validation.warningCount() > 0) {
            flags |= FLAG_HAS_WARNINGS;
        }
        if (validation.errorCount() > 0) {
            flags |= FLAG_HAS_ERRORS;
        }
        return flags;
    }

    private static long max(long first, long second, long third) {
        return Math.max(first, Math.max(second, third));
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
