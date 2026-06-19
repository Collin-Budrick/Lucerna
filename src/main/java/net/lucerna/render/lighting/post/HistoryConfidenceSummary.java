package net.lucerna.render.lighting.post;

import net.lucerna.lighting.DenoiseHistoryCounters;

public record HistoryConfidenceSummary(
        boolean enabled,
        long frameIndex,
        long previousFrameIndex,
        int width,
        int height,
        long pixelCount,
        boolean confidenceMapReady,
        boolean varianceMapReady,
        boolean disocclusionMaskReady,
        long historyAccepted,
        long historyRejected,
        long disocclusionPixels,
        float historyConfidence,
        float variance,
        String sceneState,
        String readinessReason
) {
    private static final float STABLE_CONFIDENCE = 0.92F;
    private static final float MOVED_CONFIDENCE = 0.36F;
    private static final float NOISY_CONFIDENCE = 0.48F;

    public HistoryConfidenceSummary {
        frameIndex = Math.max(0L, frameIndex);
        previousFrameIndex = Math.max(0L, previousFrameIndex);
        width = Math.max(0, width);
        height = Math.max(0, height);
        pixelCount = Math.max(0L, pixelCount);
        historyAccepted = Math.max(0L, historyAccepted);
        historyRejected = Math.max(0L, historyRejected);
        disocclusionPixels = Math.max(0L, disocclusionPixels);
        historyConfidence = clamp01(historyConfidence);
        variance = clamp01(variance);
        sceneState = clean(sceneState, "unavailable");
        readinessReason = clean(readinessReason, "history confidence telemetry unavailable");
    }

    public static HistoryConfidenceSummary unavailable(String reason) {
        return new HistoryConfidenceSummary(
                false,
                0L,
                0L,
                0,
                0,
                0L,
                false,
                false,
                false,
                0L,
                0L,
                0L,
                0.0F,
                0.0F,
                "unavailable",
                reason
        );
    }

    public static HistoryConfidenceSummary from(HistoryRejectionPlan plan, DenoiseInputContract inputs) {
        if (inputs == null) {
            return unavailable("denoise inputs unavailable");
        }
        if (plan == null) {
            return unavailable("history rejection plan unavailable");
        }

        long pixels = inputs.pixelCount();
        boolean dimensionsAvailable = inputs.dimensionsAvailable();
        boolean enabled = plan.enabled();
        boolean confidenceReady = enabled
                && dimensionsAvailable
                && inputs.historyConfidenceMapInputsAvailable();
        boolean varianceReady = enabled
                && dimensionsAvailable
                && inputs.varianceMapInputsAvailable();
        boolean disocclusionReady = enabled
                && dimensionsAvailable
                && inputs.disocclusionMaskInputsAvailable()
                && plan.writesRejectionMask();

        if (!enabled) {
            return new HistoryConfidenceSummary(
                    false,
                    inputs.frameIndex(),
                    plan.previousFrameIndex(),
                    inputs.gBuffer().width(),
                    inputs.gBuffer().height(),
                    pixels,
                    false,
                    varianceReady,
                    false,
                    0L,
                    0L,
                    0L,
                    0.0F,
                    0.0F,
                    "disabled",
                    "history rejection disabled"
            );
        }
        if (!dimensionsAvailable) {
            return new HistoryConfidenceSummary(
                    true,
                    inputs.frameIndex(),
                    plan.previousFrameIndex(),
                    inputs.gBuffer().width(),
                    inputs.gBuffer().height(),
                    0L,
                    false,
                    false,
                    false,
                    0L,
                    0L,
                    0L,
                    0.0F,
                    0.0F,
                    "unavailable",
                    "history confidence telemetry has no frame dimensions"
            );
        }

        String state = classifySceneState(plan, inputs);
        long accepted = acceptedPixelsForState(state, pixels);
        long rejected = Math.max(0L, pixels - accepted);
        long disoccluded = disocclusionPixelsForState(state, rejected, pixels);
        float confidence = confidenceForState(state, plan.settings().minHistoryConfidence());
        float variance = varianceForState(state, rejected, pixels);
        String reason = "sceneState=" + state
                + "; matrix=" + inputs.matrixHistory().motionStateLabel()
                + "; historyInputs=" + plan.historyInputsComplete()
                + "; temporalReuse=" + plan.temporalReuseAllowed();

        return new HistoryConfidenceSummary(
                true,
                inputs.frameIndex(),
                plan.previousFrameIndex(),
                inputs.gBuffer().width(),
                inputs.gBuffer().height(),
                pixels,
                confidenceReady,
                varianceReady,
                disocclusionReady,
                accepted,
                rejected,
                disoccluded,
                confidence,
                variance,
                state,
                reason
        );
    }

    public DenoiseHistoryCounters historyCounters() {
        long reset = "reset".equals(this.sceneState) ? this.historyRejected : 0L;
        long missing = "disoccluded".equals(this.sceneState) && this.previousFrameIndex == 0L
                ? this.historyRejected
                : 0L;
        long materialMismatch = "moved".equals(this.sceneState) ? this.historyRejected / 4L : 0L;
        return new DenoiseHistoryCounters(
                this.historyAccepted,
                this.historyRejected,
                reset,
                missing,
                this.disocclusionPixels,
                materialMismatch
        );
    }

    public String compactSummary() {
        return "sceneState=" + this.sceneState
                + " confidenceMapReady=" + this.confidenceMapReady
                + " varianceMapReady=" + this.varianceMapReady
                + " disocclusionMaskReady=" + this.disocclusionMaskReady
                + " historyAccepted=" + this.historyAccepted
                + " historyRejected=" + this.historyRejected
                + " disocclusionPixels=" + this.disocclusionPixels
                + " confidence=" + this.historyConfidence
                + " variance=" + this.variance;
    }

    public boolean hasHistoryCounters() {
        return this.historyAccepted > 0L || this.historyRejected > 0L;
    }

    private static String classifySceneState(HistoryRejectionPlan plan, DenoiseInputContract inputs) {
        if (inputs.matrixHistory().historyReset()) {
            return "reset";
        }
        if (!plan.historyInputsComplete() || !inputs.matrixHistory().hasPreviousMatrices()) {
            return "disoccluded";
        }
        if (plan.temporalReuseAllowed() && inputs.matrixHistory().stableForHistory()) {
            return inputs.cacheConfidenceAvailable() ? "stable" : "noisy";
        }
        if (inputs.matrixHistory().movedForHistory()) {
            return "moved";
        }
        if (!inputs.cacheConfidenceAvailable()) {
            return "noisy";
        }
        return plan.temporalReuseAllowed() ? "stable" : "disoccluded";
    }

    private static long acceptedPixelsForState(String state, long pixels) {
        return switch (state) {
            case "stable" -> pixels * 7L / 8L;
            case "noisy" -> pixels / 2L;
            case "moved" -> pixels / 3L;
            default -> 0L;
        };
    }

    private static long disocclusionPixelsForState(String state, long rejected, long pixels) {
        return switch (state) {
            case "disoccluded", "reset" -> pixels;
            case "moved" -> rejected / 2L;
            default -> 0L;
        };
    }

    private static float confidenceForState(String state, float floor) {
        float value = switch (state) {
            case "stable" -> STABLE_CONFIDENCE;
            case "moved" -> MOVED_CONFIDENCE;
            case "noisy" -> NOISY_CONFIDENCE;
            default -> 0.0F;
        };
        return clamp01(Math.max(value, state.equals("stable") ? floor : 0.0F));
    }

    private static float varianceForState(String state, long rejected, long pixels) {
        if (pixels <= 0L) {
            return 0.0F;
        }
        float rejectionRatio = (float) rejected / (float) pixels;
        return switch (state) {
            case "stable" -> Math.max(0.03F, rejectionRatio * 0.25F);
            case "noisy" -> Math.max(0.55F, rejectionRatio);
            case "moved" -> Math.max(0.70F, rejectionRatio);
            default -> 1.0F;
        };
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
