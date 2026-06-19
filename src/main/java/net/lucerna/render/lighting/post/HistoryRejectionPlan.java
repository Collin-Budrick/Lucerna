package net.lucerna.render.lighting.post;

import java.util.Objects;

public record HistoryRejectionPlan(
        HistoryRejectionSettings settings,
        long frameIndex,
        long previousFrameIndex,
        boolean previousDepthAvailable,
        boolean previousNormalRoughnessAvailable,
        boolean previousLightingAvailable,
        boolean motionHistoryAvailable,
        boolean matrixHistoryReusable
) {
    public HistoryRejectionPlan {
        Objects.requireNonNull(settings, "settings");
        frameIndex = Math.max(0L, frameIndex);
        previousFrameIndex = Math.max(0L, previousFrameIndex);
        matrixHistoryReusable = settings.enabled() && matrixHistoryReusable;
    }

    public static HistoryRejectionPlan from(HistoryRejectionSettings settings, DenoiseInputContract inputs) {
        Objects.requireNonNull(inputs, "inputs");
        HistoryRejectionSettings resolvedSettings = settings == null
                ? HistoryRejectionSettings.disabled()
                : settings;
        return new HistoryRejectionPlan(
                resolvedSettings,
                inputs.frameIndex(),
                inputs.matrixHistory().previousFrameIndex(),
                inputs.previousDepthAvailable(),
                inputs.previousNormalRoughnessAvailable(),
                inputs.previousLightingAvailable(),
                inputs.motionHistoryAvailable(),
                inputs.matrixHistory().temporalReuseAllowed()
        );
    }

    public boolean enabled() {
        return this.settings.enabled();
    }

    public boolean historyInputsComplete() {
        return this.previousDepthAvailable
                && this.previousNormalRoughnessAvailable
                && this.previousLightingAvailable
                && this.motionHistoryAvailable;
    }

    public boolean temporalReuseAllowed() {
        return this.enabled()
                && this.frameIndex > 0L
                && this.previousFrameIndex > 0L
                && this.frameIndex > this.previousFrameIndex
                && this.matrixHistoryReusable
                && this.historyInputsComplete();
    }

    public boolean writesRejectionMask() {
        return this.enabled() && this.frameIndex > 0L;
    }

    public HistoryConfidenceSummary confidenceSummary(DenoiseInputContract inputs) {
        return HistoryConfidenceSummary.from(this, inputs);
    }

    public boolean confidenceMapReady(DenoiseInputContract inputs) {
        return this.confidenceSummary(inputs).confidenceMapReady();
    }

    public boolean varianceMapReady(DenoiseInputContract inputs) {
        return this.confidenceSummary(inputs).varianceMapReady();
    }

    public boolean disocclusionMaskReady(DenoiseInputContract inputs) {
        return this.confidenceSummary(inputs).disocclusionMaskReady();
    }

    public String fallbackReason() {
        if (!this.enabled()) {
            return "history rejection disabled";
        }
        if (this.frameIndex <= 0L) {
            return "current frame index unavailable";
        }
        if (this.previousFrameIndex <= 0L) {
            return "previous frame unavailable";
        }
        if (!this.matrixHistoryReusable) {
            return "matrix history is reset or not reusable";
        }
        if (!this.historyInputsComplete()) {
            return "previous history resources are incomplete";
        }
        return "";
    }
}
