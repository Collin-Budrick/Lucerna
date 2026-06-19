package net.lucerna.nativebridge;

public record DenoiseExecutionSnapshot(
        boolean nativeStatusAvailable,
        boolean denoiseExecutionAvailable,
        long dispatchGeneration,
        long packetGeneration,
        int width,
        int height,
        int inputCount,
        int outputCount,
        int sampleCount,
        int historyAcceptedCount,
        int historyRejectedCount,
        int edgeRejectedCount,
        int edgePreservedCount,
        int rawGiPixels,
        int rawGiSamples,
        int rawGiRays,
        int rawGiCacheReads,
        int denoisedOutputPixels,
        long denoisedOutputChecksum,
        int denoisedOutputChangedPixels,
        int denoisedOutputMeanAbsDelta,
        int compositeWidth,
        int compositeHeight,
        int compositeOutputCount,
        boolean enabled,
        boolean validated,
        boolean placeholder,
        boolean temporalHistory,
        boolean edgeInputsAvailable,
        boolean directShadowSignalAvailable,
        boolean diffuseGiSignalAvailable,
        boolean optionalSpecularPlaceholder,
        boolean optionalAoPlaceholder,
        boolean rawGiInputAvailable,
        boolean rawDirectInputAvailable,
        boolean denoisedOutputIntent,
        boolean denoisedCpuOutputGenerated,
        boolean denoisedOutputDiffersFromRaw,
        boolean realDenoiseShaderOutput,
        boolean compositeStageRecorded,
        boolean compositeEnabled,
        boolean compositeReady,
        boolean compositePlaceholder,
        boolean edgeDepthAvailable,
        boolean edgeNormalAvailable,
        boolean edgeMaterialAvailable,
        boolean historyConfidenceAvailable,
        boolean ready,
        boolean accepted,
        String outputMarker,
        String rawInputMarker,
        String denoisedOutputMarker,
        String compositeMarker,
        String readinessReason
) {
    public DenoiseExecutionSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        packetGeneration = Math.max(0L, packetGeneration);
        width = Math.max(0, width);
        height = Math.max(0, height);
        inputCount = Math.max(0, inputCount);
        outputCount = Math.max(0, outputCount);
        sampleCount = Math.max(0, sampleCount);
        historyAcceptedCount = Math.max(0, historyAcceptedCount);
        historyRejectedCount = Math.max(0, historyRejectedCount);
        edgeRejectedCount = Math.max(0, edgeRejectedCount);
        edgePreservedCount = Math.max(0, edgePreservedCount);
        rawGiPixels = Math.max(0, rawGiPixels);
        rawGiSamples = Math.max(0, rawGiSamples);
        rawGiRays = Math.max(0, rawGiRays);
        rawGiCacheReads = Math.max(0, rawGiCacheReads);
        denoisedOutputPixels = Math.max(0, denoisedOutputPixels);
        denoisedOutputChecksum = Math.max(0L, denoisedOutputChecksum);
        denoisedOutputChangedPixels = Math.max(0, denoisedOutputChangedPixels);
        denoisedOutputMeanAbsDelta = Math.max(0, denoisedOutputMeanAbsDelta);
        compositeWidth = Math.max(0, compositeWidth);
        compositeHeight = Math.max(0, compositeHeight);
        compositeOutputCount = Math.max(0, compositeOutputCount);
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
        rawInputMarker = rawInputMarker == null || rawInputMarker.isBlank() ? "unknown" : rawInputMarker;
        denoisedOutputMarker = denoisedOutputMarker == null || denoisedOutputMarker.isBlank() ? "unknown" : denoisedOutputMarker;
        compositeMarker = compositeMarker == null || compositeMarker.isBlank() ? "unknown" : compositeMarker;
        readinessReason = readinessReason == null || readinessReason.isBlank() ? "unknown" : readinessReason;
    }

    public static DenoiseExecutionSnapshot unavailable(String reason) {
        return new DenoiseExecutionSnapshot(
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
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
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
                false,
                false,
                false,
                false,
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                reason
        );
    }

    public static DenoiseExecutionSnapshot fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status unavailable");
        }

        String denoiseExecution = extractBlock(nativeStatus, "denoise_execution={");
        if (denoiseExecution.isBlank()) {
            return new DenoiseExecutionSnapshot(
                    true,
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
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
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
                    false,
                    false,
                    false,
                    false,
                    "unknown",
                    "unknown",
                    "unknown",
                    "unknown",
                    "native denoise execution status unavailable"
            );
        }

        return new DenoiseExecutionSnapshot(
                true,
                true,
                parseLong(extractField(denoiseExecution, "dispatch_generation")),
                parseLong(extractField(denoiseExecution, "packet_generation")),
                dimensionComponentInt(extractField(denoiseExecution, "size"), 0),
                dimensionComponentInt(extractField(denoiseExecution, "size"), 1),
                parseInt(extractField(denoiseExecution, "inputs")),
                parseInt(extractField(denoiseExecution, "outputs")),
                parseInt(extractField(denoiseExecution, "samples")),
                parseInt(extractField(denoiseExecution, "history_accepted")),
                parseInt(extractField(denoiseExecution, "history_rejected")),
                parseInt(extractField(denoiseExecution, "edge_rejected")),
                parseInt(extractField(denoiseExecution, "edge_preserved")),
                parseInt(extractField(denoiseExecution, "raw_gi_pixels")),
                parseInt(extractField(denoiseExecution, "raw_gi_samples")),
                parseInt(extractField(denoiseExecution, "raw_gi_rays")),
                parseInt(extractField(denoiseExecution, "raw_gi_cache_reads")),
                parseInt(extractField(denoiseExecution, "denoised_output_pixels")),
                parseLong(extractField(denoiseExecution, "denoised_output_checksum")),
                parseInt(extractField(denoiseExecution, "denoised_output_changed_pixels")),
                parseInt(extractField(denoiseExecution, "denoised_output_mean_abs_delta")),
                dimensionComponentInt(extractField(denoiseExecution, "composite_size"), 0),
                dimensionComponentInt(extractField(denoiseExecution, "composite_size"), 1),
                parseInt(extractField(denoiseExecution, "composite_outputs")),
                parseBoolean(extractField(denoiseExecution, "enabled")),
                parseBoolean(extractField(denoiseExecution, "validated")),
                parseBoolean(extractField(denoiseExecution, "placeholder")),
                parseBoolean(extractField(denoiseExecution, "temporal_history")),
                parseBoolean(extractField(denoiseExecution, "edge_inputs_available")),
                parseBoolean(extractField(denoiseExecution, "direct_shadow_signal_available")),
                parseBoolean(extractField(denoiseExecution, "diffuse_gi_signal_available")),
                parseBoolean(extractField(denoiseExecution, "optional_specular_placeholder")),
                parseBoolean(extractField(denoiseExecution, "optional_ao_placeholder")),
                parseBoolean(extractField(denoiseExecution, "raw_gi_input_available")),
                parseBoolean(extractField(denoiseExecution, "raw_direct_input_available")),
                parseBoolean(extractField(denoiseExecution, "denoised_output_intent")),
                parseBoolean(extractField(denoiseExecution, "denoised_cpu_output_generated")),
                parseBoolean(extractField(denoiseExecution, "denoised_output_differs_from_raw")),
                parseBoolean(extractField(denoiseExecution, "real_denoise_shader_output")),
                parseBoolean(extractField(denoiseExecution, "composite_stage_recorded")),
                parseBoolean(extractField(denoiseExecution, "composite_enabled")),
                parseBoolean(extractField(denoiseExecution, "composite_ready")),
                parseBoolean(extractField(denoiseExecution, "composite_placeholder")),
                parseBoolean(extractField(denoiseExecution, "edge_depth_available")),
                parseBoolean(extractField(denoiseExecution, "edge_normal_available")),
                parseBoolean(extractField(denoiseExecution, "edge_material_available")),
                parseBoolean(extractField(denoiseExecution, "history_confidence_available")),
                parseBoolean(extractField(denoiseExecution, "ready")),
                parseBoolean(extractField(denoiseExecution, "accepted_this_dispatch")),
                extractField(denoiseExecution, "output_marker"),
                extractField(denoiseExecution, "raw_input_marker"),
                extractField(denoiseExecution, "denoised_output_marker"),
                extractField(denoiseExecution, "composite_marker"),
                extractField(denoiseExecution, "readiness_reason")
        );
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.denoiseExecutionAvailable;
    }

    public boolean hasHistoryCounters() {
        return this.historyAcceptedCount > 0 || this.historyRejectedCount > 0;
    }

    public boolean cpuDenoisedOutputReadbackReady() {
        return this.hasExecutionTelemetry()
                && this.enabled
                && this.accepted
                && this.denoisedOutputIntent
                && this.denoisedCpuOutputGenerated
                && this.denoisedOutputPixels > 0
                && this.denoisedOutputChecksum > 0L;
    }

    public boolean denoiseQualityEvidenceReady() {
        return this.cpuDenoisedOutputReadbackReady()
                && this.denoisedOutputDiffersFromRaw
                && this.edgeInputsAvailable
                && (this.hasHistoryCounters() || this.edgePreservedCount > 0 || this.edgeRejectedCount > 0);
    }

    public String denoiseReadinessBoundary() {
        if (!this.hasExecutionTelemetry()) {
            return "no-denoise-execution-telemetry";
        }
        if (!this.cpuDenoisedOutputReadbackReady()) {
            return "not-ready:missing-accepted-cpu-output-readback";
        }
        if (this.realDenoiseShaderOutput) {
            return this.denoiseQualityEvidenceReady()
                    ? "real-shader-denoise-output-with-quality-evidence"
                    : "real-shader-denoise-output-without-quality-evidence";
        }
        if (this.denoiseQualityEvidenceReady()) {
            return "cpu-output-readback-ready; quality-evidence-present; real-shader-output=false";
        }
        return "cpu-output-readback-ready; denoise-quality-not-proven; real-shader-output=false";
    }

    public String debugSummary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        return "denoiseExecution ready=" + this.ready
                + " accepted=" + this.accepted
                + " size=" + this.width + "x" + this.height
                + " inputs=" + this.inputCount
                + " outputs=" + this.outputCount
                + " samples=" + this.sampleCount
                + " edgeInputs=" + this.edgeInputsAvailable
                + " diffuseGiSignal=" + this.diffuseGiSignalAvailable
                + " directShadowSignal=" + this.directShadowSignalAvailable
                + " historyAccepted=" + this.historyAcceptedCount
                + " historyRejected=" + this.historyRejectedCount
                + " edgePreserved=" + this.edgePreservedCount
                + " edgeRejected=" + this.edgeRejectedCount
                + " rawGi=" + this.rawGiInputAvailable
                + " rawGiPixels=" + this.rawGiPixels
                + " rawGiSamples=" + this.rawGiSamples
                + " rawGiRays=" + this.rawGiRays
                + " rawGiCacheReads=" + this.rawGiCacheReads
                + " denoisedIntent=" + this.denoisedOutputIntent
                + " denoisedCpuOutputGenerated=" + this.denoisedCpuOutputGenerated
                + " denoisedOutputPixels=" + this.denoisedOutputPixels
                + " denoisedOutputChangedPixels=" + this.denoisedOutputChangedPixels
                + " denoisedOutputMeanAbsDelta=" + this.denoisedOutputMeanAbsDelta
                + " denoisedOutputDiffersFromRaw=" + this.denoisedOutputDiffersFromRaw
                + " realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + " cpuReadbackReady=" + this.cpuDenoisedOutputReadbackReady()
                + " denoiseQualityEvidenceReady=" + this.denoiseQualityEvidenceReady()
                + " readinessBoundary=" + this.denoiseReadinessBoundary()
                + " composite=" + this.compositeSignalLabel()
                + " compositeSize=" + this.compositeWidth + "x" + this.compositeHeight
                + " outputMarker=" + this.outputMarker
                + " rawInputMarker=" + this.rawInputMarker
                + " denoisedOutputMarker=" + this.denoisedOutputMarker
                + " compositeMarker=" + this.compositeMarker
                + " reason=" + this.readinessReason;
    }

    public String compositeSignalLabel() {
        if (!this.compositeStageRecorded) {
            return "missing";
        }
        if (this.compositePlaceholder) {
            return "placeholder";
        }
        return this.compositeReady ? "ready" : "metadata";
    }

    private static String extractBlock(String source, String marker) {
        int markerStart = source.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }
        int contentStart = markerStart + marker.length();
        int depth = 1;
        boolean quoted = false;
        for (int index = contentStart; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == '"') {
                    quoted = false;
                }
                continue;
            }
            if (character == '"') {
                quoted = true;
                continue;
            }
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(contentStart, index);
                }
            }
        }
        return "";
    }

    private static String extractField(String block, String fieldName) {
        String marker = fieldName + "=";
        int markerStart = findFieldMarker(block, marker);
        if (markerStart < 0) {
            return "";
        }
        int valueStart = markerStart + marker.length();
        boolean quoted = valueStart < block.length() && block.charAt(valueStart) == '"';
        int contentStart = quoted ? valueStart + 1 : valueStart;
        for (int index = contentStart; index < block.length(); index++) {
            char character = block.charAt(index);
            if (quoted && character == '"') {
                return block.substring(contentStart, index);
            }
            if (!quoted && (character == ',' || character == '}')) {
                return block.substring(contentStart, index).trim();
            }
        }
        return block.substring(contentStart).trim();
    }

    private static int findFieldMarker(String block, String marker) {
        if (block == null || block.isBlank() || marker == null || marker.isBlank()) {
            return -1;
        }

        int searchFrom = 0;
        while (searchFrom < block.length()) {
            int markerStart = block.indexOf(marker, searchFrom);
            if (markerStart < 0) {
                return -1;
            }
            if (markerStart == 0 || block.charAt(markerStart - 1) == ',') {
                return markerStart;
            }
            searchFrom = markerStart + marker.length();
        }
        return -1;
    }

    private static boolean parseBoolean(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static int parseInt(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? 0L : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static int dimensionComponentInt(String dimensions, int component) {
        if (dimensions == null || dimensions.isBlank()) {
            return 0;
        }
        String[] parts = dimensions.trim().split("x", 3);
        if (component < 0 || component >= parts.length) {
            return 0;
        }
        return parseInt(parts[component]);
    }
}
