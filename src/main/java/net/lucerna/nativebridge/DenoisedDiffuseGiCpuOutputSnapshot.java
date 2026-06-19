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
        boolean edgeInputsAvailable,
        boolean historyConfidenceAvailable,
        String outputMarker,
        String rawInputMarker,
        String denoisedOutputMarker,
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
        historyAcceptedCount = Math.max(0, historyAcceptedCount);
        historyRejectedCount = Math.max(0, historyRejectedCount);
        edgePreservedCount = Math.max(0, edgePreservedCount);
        edgeRejectedCount = Math.max(0, edgeRejectedCount);
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
        rawInputMarker = rawInputMarker == null || rawInputMarker.isBlank() ? "unknown" : rawInputMarker;
        denoisedOutputMarker = denoisedOutputMarker == null || denoisedOutputMarker.isBlank()
                ? "unknown"
                : denoisedOutputMarker;
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? "unknown"
                : readinessReason;
    }

    public static DenoisedDiffuseGiCpuOutputSnapshot unavailable(String reason) {
        return new DenoisedDiffuseGiCpuOutputSnapshot(
                false,
                false,
                0L,
                0L,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0L,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "unknown",
                "unknown",
                "unknown",
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
                denoise.edgeInputsAvailable(),
                denoise.historyConfidenceAvailable(),
                denoise.outputMarker(),
                denoise.rawInputMarker(),
                denoise.denoisedOutputMarker(),
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
                + " historyAccepted=" + this.historyAcceptedCount
                + " historyRejected=" + this.historyRejectedCount
                + " edgePreserved=" + this.edgePreservedCount
                + " edgeRejected=" + this.edgeRejectedCount
                + " rawGi=" + this.rawGiInputAvailable
                + " denoisedIntent=" + this.denoisedOutputIntent
                + " denoisedCpuOutputGenerated=" + this.denoisedCpuOutputGenerated
                + " denoisedOutputDiffersFromRaw=" + this.denoisedOutputDiffersFromRaw
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " readinessBoundary=\"" + this.outputReadinessBoundary() + "\""
                + " outputMarker=" + this.outputMarker
                + " rawInputMarker=" + this.rawInputMarker
                + " denoisedOutputMarker=" + this.denoisedOutputMarker
                + " reason=" + this.previewReadinessReason();
    }

    private static int saturatedPixelCount(int width, int height) {
        long pixels = Math.max(0L, width) * Math.max(0L, height);
        return pixels > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pixels;
    }
}
