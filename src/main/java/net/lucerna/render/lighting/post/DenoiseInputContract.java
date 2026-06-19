package net.lucerna.render.lighting.post;

import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.frame.FrameMatrixHistory;
import net.lucerna.render.frame.LucernaFrameConstants;

import java.util.Objects;

public record DenoiseInputContract(
        long frameIndex,
        GBufferDescriptor gBuffer,
        boolean directLightingAvailable,
        boolean diffuseGiAvailable,
        boolean cacheConfidenceAvailable,
        boolean previousDepthAvailable,
        boolean previousNormalRoughnessAvailable,
        boolean previousLightingAvailable,
        boolean motionHistoryAvailable,
        FrameMatrixHistory matrixHistory,
        long directLightingGeneration,
        long diffuseGiGeneration,
        long historyGeneration
) {
    public DenoiseInputContract {
        frameIndex = Math.max(0L, frameIndex);
        if (gBuffer == null) {
            gBuffer = GBufferDescriptor.empty();
        }
        if (matrixHistory == null) {
            matrixHistory = FrameMatrixHistory.unavailable("Denoise matrix history was not supplied.");
        }
        directLightingGeneration = Math.max(0L, directLightingGeneration);
        diffuseGiGeneration = Math.max(0L, diffuseGiGeneration);
        historyGeneration = Math.max(0L, historyGeneration);
    }

    public static DenoiseInputContract empty() {
        return new DenoiseInputContract(
                0L,
                GBufferDescriptor.empty(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                FrameMatrixHistory.unavailable("Denoise inputs have not been populated."),
                0L,
                0L,
                0L
        );
    }

    public static DenoiseInputContract fromFrame(
            LucernaFrameConstants constants,
            FrameMatrixHistory matrixHistory,
            GBufferDescriptor gBuffer,
            boolean directLightingAvailable,
            boolean diffuseGiAvailable,
            boolean cacheConfidenceAvailable,
            long directLightingGeneration,
            long diffuseGiGeneration,
            long historyGeneration
    ) {
        Objects.requireNonNull(constants, "constants");
        FrameMatrixHistory resolvedHistory = matrixHistory == null
                ? FrameMatrixHistory.unavailable("Denoise matrix history was not supplied.")
                : matrixHistory;
        boolean temporalHistoryAvailable = resolvedHistory.temporalReuseAllowed();
        GBufferDescriptor resolvedGBuffer = gBuffer == null ? GBufferDescriptor.empty() : gBuffer;
        return new DenoiseInputContract(
                constants.frameIndex(),
                resolvedGBuffer,
                directLightingAvailable,
                diffuseGiAvailable,
                cacheConfidenceAvailable,
                temporalHistoryAvailable,
                temporalHistoryAvailable,
                temporalHistoryAvailable,
                resolvedGBuffer.hasMotionVectors(),
                resolvedHistory,
                directLightingGeneration,
                diffuseGiGeneration,
                historyGeneration
        );
    }

    public boolean dimensionsAvailable() {
        return this.gBuffer.width() > 0 && this.gBuffer.height() > 0;
    }

    public boolean hasEdgeAwareInputs() {
        return this.gBuffer.hasDepth() && this.gBuffer.hasNormals();
    }

    public boolean hasLightingInputs() {
        return this.directLightingAvailable && this.diffuseGiAvailable;
    }

    public boolean hasHistoryInputs() {
        return this.previousDepthAvailable
                && this.previousNormalRoughnessAvailable
                && this.previousLightingAvailable
                && this.motionHistoryAvailable
                && this.matrixHistory.temporalReuseAllowed();
    }

    public boolean hasRequiredDenoiseInputs() {
        return this.dimensionsAvailable()
                && this.hasEdgeAwareInputs()
                && this.gBuffer.hasMotionVectors()
                && this.hasLightingInputs();
    }

    public long pixelCount() {
        return (long) this.gBuffer.width() * (long) this.gBuffer.height();
    }

    public boolean historyConfidenceMapInputsAvailable() {
        return this.dimensionsAvailable()
                && this.gBuffer.hasMotionVectors()
                && this.matrixHistory.hasCurrentMatrices();
    }

    public boolean varianceMapInputsAvailable() {
        return this.dimensionsAvailable()
                && (this.directLightingAvailable || this.diffuseGiAvailable)
                && (this.cacheConfidenceAvailable || this.previousLightingAvailable || this.diffuseGiAvailable);
    }

    public boolean disocclusionMaskInputsAvailable() {
        return this.dimensionsAvailable()
                && this.gBuffer.hasMotionVectors()
                && this.matrixHistory.hasCurrentMatrices();
    }

    public String round8InputSummary() {
        return "frame=" + this.frameIndex
                + " size=" + this.gBuffer.width() + "x" + this.gBuffer.height()
                + " direct=" + this.directLightingAvailable
                + " diffuseGi=" + this.diffuseGiAvailable
                + " cacheConfidence=" + this.cacheConfidenceAvailable
                + " historyInputs=" + this.hasHistoryInputs()
                + " matrix=" + this.matrixHistory.motionStateLabel();
    }

    public long maxInputGeneration() {
        return Math.max(
                this.directLightingGeneration,
                Math.max(this.diffuseGiGeneration, this.historyGeneration)
        );
    }
}
