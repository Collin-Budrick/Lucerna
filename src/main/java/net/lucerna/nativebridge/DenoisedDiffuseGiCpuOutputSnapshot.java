package net.lucerna.nativebridge;

public record DenoisedDiffuseGiCpuOutputSnapshot(
        boolean nativeStatusAvailable,
        boolean denoiseExecutionAvailable,
        long dispatchGeneration,
        long packetGeneration,
        int outputWidth,
        int outputHeight,
        int outputPixels,
        int inputCount,
        int outputCount,
        int sampleCount,
        int nativeOutputPixels,
        long nativeOutputChecksum,
        int nativeOutputChangedPixels,
        int nativeOutputMeanAbsDelta,
        long previousDenoisedOutputChecksum,
        long currentDenoisedOutputChecksum,
        int frameToFrameChangedPixels,
        int frameToFrameMeanAbsDelta,
        int temporalStablePixels,
        int temporalUnstablePixels,
        double temporalHistoryConfidence,
        double temporalFlickerScore,
        int shaderOutputImageCandidateWidth,
        int shaderOutputImageCandidateHeight,
        int shaderOutputImageCandidatePixels,
        long shaderOutputImageCandidateBytes,
        long shaderOutputImageCandidateChecksum,
        int historyAcceptedCount,
        int historyRejectedCount,
        int edgePreservedCount,
        int edgeRejectedCount,
        boolean enabled,
        boolean ready,
        boolean accepted,
        boolean rawGiInputAvailable,
        boolean denoisedOutputIntent,
        boolean denoisedCpuOutputGenerated,
        boolean denoisedOutputDiffersFromRaw,
        boolean realDenoiseShaderOutput,
        boolean shaderOutputImageCandidateReady,
        boolean shaderOutputImageCandidateCpuStaged,
        boolean shaderOutputImageCandidateNonGpu,
        boolean shaderDenoiseShaderGeneratedOutput,
        boolean temporalReady,
        boolean temporalGhostingRisk,
        boolean edgeInputsAvailable,
        boolean historyConfidenceAvailable,
        String outputMarker,
        String rawInputMarker,
        String denoisedOutputMarker,
        String temporalReadinessMarker,
        String temporalGhostingRiskMarker,
        String shaderOutputImageCandidateMarker,
        String shaderOutputImageBlocker,
        String readinessReason
) {
    public DenoisedDiffuseGiCpuOutputSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        packetGeneration = Math.max(0L, packetGeneration);
        outputWidth = Math.max(0, outputWidth);
        outputHeight = Math.max(0, outputHeight);
        outputPixels = Math.max(0, outputPixels);
        inputCount = Math.max(0, inputCount);
        outputCount = Math.max(0, outputCount);
        sampleCount = Math.max(0, sampleCount);
        nativeOutputPixels = Math.max(0, nativeOutputPixels);
        nativeOutputChecksum = Math.max(0L, nativeOutputChecksum);
        nativeOutputChangedPixels = Math.max(0, nativeOutputChangedPixels);
        nativeOutputMeanAbsDelta = Math.max(0, nativeOutputMeanAbsDelta);
        previousDenoisedOutputChecksum = Math.max(0L, previousDenoisedOutputChecksum);
        currentDenoisedOutputChecksum = Math.max(0L, currentDenoisedOutputChecksum);
        frameToFrameChangedPixels = Math.max(0, frameToFrameChangedPixels);
        frameToFrameMeanAbsDelta = Math.max(0, frameToFrameMeanAbsDelta);
        temporalStablePixels = Math.max(0, temporalStablePixels);
        temporalUnstablePixels = Math.max(0, temporalUnstablePixels);
        temporalHistoryConfidence = Math.max(0.0D, temporalHistoryConfidence);
        temporalFlickerScore = Math.max(0.0D, temporalFlickerScore);
        shaderOutputImageCandidateWidth = Math.max(0, shaderOutputImageCandidateWidth);
        shaderOutputImageCandidateHeight = Math.max(0, shaderOutputImageCandidateHeight);
        shaderOutputImageCandidatePixels = Math.max(0, shaderOutputImageCandidatePixels);
        shaderOutputImageCandidateBytes = Math.max(0L, shaderOutputImageCandidateBytes);
        shaderOutputImageCandidateChecksum = Math.max(0L, shaderOutputImageCandidateChecksum);
        historyAcceptedCount = Math.max(0, historyAcceptedCount);
        historyRejectedCount = Math.max(0, historyRejectedCount);
        edgePreservedCount = Math.max(0, edgePreservedCount);
        edgeRejectedCount = Math.max(0, edgeRejectedCount);
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
        rawInputMarker = rawInputMarker == null || rawInputMarker.isBlank() ? "unknown" : rawInputMarker;
        denoisedOutputMarker = denoisedOutputMarker == null || denoisedOutputMarker.isBlank()
                ? "unknown"
                : denoisedOutputMarker;
        temporalReadinessMarker = temporalReadinessMarker == null || temporalReadinessMarker.isBlank()
                ? "unknown"
                : temporalReadinessMarker;
        temporalGhostingRiskMarker = temporalGhostingRiskMarker == null || temporalGhostingRiskMarker.isBlank()
                ? "unknown"
                : temporalGhostingRiskMarker;
        shaderOutputImageCandidateMarker = shaderOutputImageCandidateMarker == null
                || shaderOutputImageCandidateMarker.isBlank()
                ? "unknown"
                : shaderOutputImageCandidateMarker;
        shaderOutputImageBlocker = shaderOutputImageBlocker == null || shaderOutputImageBlocker.isBlank()
                ? "unknown"
                : shaderOutputImageBlocker;
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? "unknown"
                : readinessReason;
    }

    public static DenoisedDiffuseGiCpuOutputSnapshot unavailable(String reason) {
        return new DenoisedDiffuseGiCpuOutputSnapshot(
                false, // nativeStatusAvailable
                false, // denoiseExecutionAvailable
                0L, // dispatchGeneration
                0L, // packetGeneration
                0, // outputWidth
                0, // outputHeight
                0, // outputPixels
                0, // inputCount
                0, // outputCount
                0, // sampleCount
                0, // nativeOutputPixels
                0L, // nativeOutputChecksum
                0, // nativeOutputChangedPixels
                0, // nativeOutputMeanAbsDelta
                0L, // previousDenoisedOutputChecksum
                0L, // currentDenoisedOutputChecksum
                0, // frameToFrameChangedPixels
                0, // frameToFrameMeanAbsDelta
                0, // temporalStablePixels
                0, // temporalUnstablePixels
                0.0D, // temporalHistoryConfidence
                0.0D, // temporalFlickerScore
                0, // shaderOutputImageCandidateWidth
                0, // shaderOutputImageCandidateHeight
                0, // shaderOutputImageCandidatePixels
                0L, // shaderOutputImageCandidateBytes
                0L, // shaderOutputImageCandidateChecksum
                0, // historyAcceptedCount
                0, // historyRejectedCount
                0, // edgePreservedCount
                0, // edgeRejectedCount
                false, // enabled
                false, // ready
                false, // accepted
                false, // rawGiInputAvailable
                false, // denoisedOutputIntent
                false, // denoisedCpuOutputGenerated
                false, // denoisedOutputDiffersFromRaw
                false, // realDenoiseShaderOutput
                false, // shaderOutputImageCandidateReady
                false, // shaderOutputImageCandidateCpuStaged
                false, // shaderOutputImageCandidateNonGpu
                false, // shaderDenoiseShaderGeneratedOutput
                false, // temporalReady
                false, // temporalGhostingRisk
                false, // edgeInputsAvailable
                false, // historyConfidenceAvailable
                "unknown", // outputMarker
                "unknown", // rawInputMarker
                "unknown", // denoisedOutputMarker
                "unknown", // temporalReadinessMarker
                "unknown", // temporalGhostingRiskMarker
                "unknown", // shaderOutputImageCandidateMarker
                reason, // shaderOutputImageBlocker
                reason
        );
    }

    public static DenoisedDiffuseGiCpuOutputSnapshot fromDenoiseExecution(DenoiseExecutionSnapshot denoise) {
        if (denoise == null) {
            return unavailable("native denoise execution snapshot was not supplied");
        }
        int outputPixels = denoise.denoisedOutputPixels() > 0
                ? denoise.denoisedOutputPixels()
                : saturatedPixelCount(denoise.width(), denoise.height());
        int outputWidth = denoise.width();
        int outputHeight = denoise.height();
        int halfWidth = Math.max(1, denoise.width() / 2);
        int halfHeight = Math.max(1, denoise.height() / 2);
        if (outputPixels > 0 && saturatedPixelCount(halfWidth, halfHeight) == outputPixels) {
            outputWidth = halfWidth;
            outputHeight = halfHeight;
        }
        return new DenoisedDiffuseGiCpuOutputSnapshot(
                denoise.nativeStatusAvailable(),
                denoise.denoiseExecutionAvailable(),
                denoise.dispatchGeneration(),
                denoise.packetGeneration(),
                outputWidth,
                outputHeight,
                outputPixels,
                denoise.inputCount(),
                denoise.outputCount(),
                denoise.sampleCount(),
                denoise.denoisedOutputPixels(),
                denoise.denoisedOutputChecksum(),
                denoise.denoisedOutputChangedPixels(),
                denoise.denoisedOutputMeanAbsDelta(),
                denoise.previousDenoisedOutputChecksum(),
                denoise.currentDenoisedOutputChecksum(),
                denoise.frameToFrameChangedPixels(),
                denoise.frameToFrameMeanAbsDelta(),
                denoise.temporalStablePixels(),
                denoise.temporalUnstablePixels(),
                denoise.temporalHistoryConfidence(),
                denoise.temporalFlickerScore(),
                denoise.shaderDenoiseOutputImageCandidateWidth(),
                denoise.shaderDenoiseOutputImageCandidateHeight(),
                denoise.shaderDenoiseOutputImageCandidatePixels(),
                denoise.shaderDenoiseOutputImageCandidateBytes(),
                denoise.shaderDenoiseOutputImageCandidateChecksum(),
                denoise.historyAcceptedCount(),
                denoise.historyRejectedCount(),
                denoise.edgePreservedCount(),
                denoise.edgeRejectedCount(),
                denoise.enabled(),
                denoise.ready(),
                denoise.accepted(),
                denoise.rawGiInputAvailable(),
                denoise.denoisedOutputIntent(),
                denoise.denoisedCpuOutputGenerated(),
                denoise.denoisedOutputDiffersFromRaw(),
                denoise.realDenoiseShaderOutput(),
                denoise.shaderDenoiseOutputImageCandidateReady(),
                denoise.shaderDenoiseOutputImageCandidateCpuStaged(),
                denoise.shaderDenoiseOutputImageCandidateNonGpu(),
                denoise.shaderDenoiseShaderGeneratedOutput(),
                denoise.temporalReady(),
                denoise.temporalGhostingRisk(),
                denoise.edgeInputsAvailable(),
                denoise.historyConfidenceAvailable(),
                denoise.outputMarker(),
                denoise.rawInputMarker(),
                denoise.denoisedOutputMarker(),
                denoise.temporalReadinessMarker(),
                denoise.temporalGhostingRiskMarker(),
                denoise.shaderDenoiseOutputImageCandidateMarker(),
                denoise.shaderDenoiseOutputImageBlocker(),
                denoise.readinessReason()
        );
    }

    public static DenoisedDiffuseGiCpuOutputSnapshot fromNativeStatus(String nativeStatus) {
        return fromDenoiseExecution(DenoiseExecutionSnapshot.fromNativeStatus(nativeStatus));
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.denoiseExecutionAvailable;
    }

    public boolean hasDenoisedOutputTelemetry() {
        return this.hasExecutionTelemetry()
                && this.denoisedOutputIntent
                && this.outputWidth > 0
                && this.outputHeight > 0
                && this.outputPixels > 0;
    }

    public boolean readyForPreviewPayload() {
        return this.hasDenoisedOutputTelemetry()
                && this.enabled
                && this.accepted
                && this.outputCount > 0
                && this.nativeOutputPixels == this.outputPixels
                && this.rawGiInputAvailable
                && this.denoisedCpuOutputGenerated;
    }

    public boolean cpuOutputReadbackReady() {
        return this.readyForPreviewPayload()
                && this.nativeOutputChecksum > 0L;
    }

    public boolean denoiseQualityEvidenceReady() {
        return this.cpuOutputReadbackReady()
                && this.denoisedOutputDiffersFromRaw
                && this.edgeInputsAvailable
                && (this.historyAcceptedCount > 0
                || this.historyRejectedCount > 0
                || this.edgePreservedCount > 0
                || this.edgeRejectedCount > 0);
    }

    public boolean shaderOutputImageCandidatePresent() {
        return this.hasExecutionTelemetry()
                && this.shaderOutputImageCandidateReady
                && this.shaderOutputImageCandidateWidth > 0
                && this.shaderOutputImageCandidateHeight > 0
                && this.shaderOutputImageCandidatePixels > 0
                && this.shaderOutputImageCandidateBytes > 0L
                && this.shaderOutputImageCandidateChecksum > 0L;
    }

    public String shaderOutputImageCandidateBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        if (!this.shaderOutputImageCandidateReady) {
            return this.shaderOutputImageBlocker;
        }
        if (this.shaderOutputImageCandidateNonGpu || this.shaderOutputImageCandidateCpuStaged) {
            return "shader output image candidate is CPU-staged/non-GPU and distinct from real shader-generated output";
        }
        if (!this.shaderDenoiseShaderGeneratedOutput || !this.realDenoiseShaderOutput) {
            return "shader output image candidate exists, but real shader-generated output remains false";
        }
        return "shader output image candidate is real shader-generated denoised diffuse GI";
    }

    public String previewReadinessReason() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        if (!this.hasDenoisedOutputTelemetry()) {
            return "native denoised diffuse GI output telemetry is not available";
        }
        if (!this.rawGiInputAvailable) {
            return "native denoise execution did not report a raw diffuse GI input";
        }
        if (!this.denoisedCpuOutputGenerated) {
            return "native denoise execution did not generate the first practical CPU denoised diffuse GI output";
        }
        if (!this.accepted || this.outputCount <= 0) {
            return "native denoise execution has not accepted a denoised diffuse GI output for this dispatch";
        }
        if (!this.denoisedOutputDiffersFromRaw) {
            return "native denoised diffuse GI RGBA8 output is available but currently matches the raw GI input";
        }
        return this.outputReadinessBoundary();
    }

    public String outputEvidenceMarker() {
        if (this.realDenoiseShaderOutput) {
            return "denoised_diffuse_gi_rgba8_real_shader_output";
        }
        if (this.denoisedCpuOutputGenerated) {
            return "denoised_diffuse_gi_rgba8_first_practical_cpu_output";
        }
        if (this.denoisedOutputIntent) {
            return "denoised_diffuse_gi_rgba8_intent_only";
        }
        return "denoised_diffuse_gi_rgba8_unavailable";
    }

    public String outputReadinessBoundary() {
        if (!this.cpuOutputReadbackReady()) {
            return "denoised diffuse GI CPU output/readback is not ready";
        }
        if (this.realDenoiseShaderOutput) {
            return this.denoiseQualityEvidenceReady()
                    ? "real shader denoise output is ready with edge/history quality evidence"
                    : "real shader denoise output is present but quality evidence is incomplete";
        }
        if (this.denoiseQualityEvidenceReady()) {
            return "CPU denoised diffuse GI RGBA8 readback is ready and differs from raw GI; real shader denoise remains false";
        }
        return "CPU denoised diffuse GI RGBA8 readback is ready, but denoise quality is not proven; real shader denoise remains false";
    }

    public String debugSummary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        return "denoisedDiffuseGiOutput readyForPayload=" + this.readyForPreviewPayload()
                + " cpuOutputReadbackReady=" + this.cpuOutputReadbackReady()
                + " denoiseQualityEvidenceReady=" + this.denoiseQualityEvidenceReady()
                + " evidence=" + this.outputEvidenceMarker()
                + " size=" + this.outputWidth + "x" + this.outputHeight
                + " pixels=" + this.outputPixels
                + " inputs=" + this.inputCount
                + " outputs=" + this.outputCount
                + " samples=" + this.sampleCount
                + " nativeOutputPixels=" + this.nativeOutputPixels
                + " nativeOutputChecksum=" + this.nativeOutputChecksum
                + " nativeOutputChangedPixels=" + this.nativeOutputChangedPixels
                + " nativeOutputMeanAbsDelta=" + this.nativeOutputMeanAbsDelta
                + " previousDenoisedOutputChecksum=" + this.previousDenoisedOutputChecksum
                + " currentDenoisedOutputChecksum=" + this.currentDenoisedOutputChecksum
                + " frameToFrameChangedPixels=" + this.frameToFrameChangedPixels
                + " frameToFrameMeanAbsDelta=" + this.frameToFrameMeanAbsDelta
                + " temporalStablePixels=" + this.temporalStablePixels
                + " temporalUnstablePixels=" + this.temporalUnstablePixels
                + " temporalHistoryConfidence=" + this.temporalHistoryConfidence
                + " temporalFlickerScore=" + this.temporalFlickerScore
                + " temporalReady=" + this.temporalReady
                + " temporalGhostingRisk=" + this.temporalGhostingRisk
                + " temporalReadinessMarker=" + this.temporalReadinessMarker
                + " temporalGhostingRiskMarker=" + this.temporalGhostingRiskMarker
                + " shaderOutputImageCandidateReady=" + this.shaderOutputImageCandidateReady
                + " shaderOutputImageCandidateCpuStaged=" + this.shaderOutputImageCandidateCpuStaged
                + " shaderOutputImageCandidateNonGpu=" + this.shaderOutputImageCandidateNonGpu
                + " shaderOutputImageCandidateSize="
                + this.shaderOutputImageCandidateWidth + "x" + this.shaderOutputImageCandidateHeight
                + " shaderOutputImageCandidatePixels=" + this.shaderOutputImageCandidatePixels
                + " shaderOutputImageCandidateBytes=" + this.shaderOutputImageCandidateBytes
                + " shaderOutputImageCandidateChecksum=" + this.shaderOutputImageCandidateChecksum
                + " shaderOutputImageCandidateBoundary=\"" + this.shaderOutputImageCandidateBoundary() + "\""
                + " historyAccepted=" + this.historyAcceptedCount
                + " historyRejected=" + this.historyRejectedCount
                + " edgePreserved=" + this.edgePreservedCount
                + " edgeRejected=" + this.edgeRejectedCount
                + " rawGi=" + this.rawGiInputAvailable
                + " denoisedIntent=" + this.denoisedOutputIntent
                + " denoisedCpuOutputGenerated=" + this.denoisedCpuOutputGenerated
                + " denoisedOutputDiffersFromRaw=" + this.denoisedOutputDiffersFromRaw
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " shaderDenoiseShaderGeneratedOutput=" + this.shaderDenoiseShaderGeneratedOutput
                + " readinessBoundary=\"" + this.outputReadinessBoundary() + "\""
                + " outputMarker=" + this.outputMarker
                + " rawInputMarker=" + this.rawInputMarker
                + " denoisedOutputMarker=" + this.denoisedOutputMarker
                + " shaderOutputImageCandidateMarker=" + this.shaderOutputImageCandidateMarker
                + " shaderOutputImageBlocker=" + this.shaderOutputImageBlocker
                + " reason=" + this.previewReadinessReason();
    }

    private static int saturatedPixelCount(int width, int height) {
        long pixels = Math.max(0L, width) * Math.max(0L, height);
        return pixels > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pixels;
    }
}
