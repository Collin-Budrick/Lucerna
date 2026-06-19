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
        boolean enabled,
        boolean validated,
        boolean placeholder,
        boolean temporalHistory,
        boolean edgeInputsAvailable,
        boolean directShadowSignalAvailable,
        boolean diffuseGiSignalAvailable,
        boolean optionalSpecularPlaceholder,
        boolean optionalAoPlaceholder,
        boolean ready,
        boolean accepted,
        String outputMarker,
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
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
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
                parseBoolean(extractField(denoiseExecution, "enabled")),
                parseBoolean(extractField(denoiseExecution, "validated")),
                parseBoolean(extractField(denoiseExecution, "placeholder")),
                parseBoolean(extractField(denoiseExecution, "temporal_history")),
                parseBoolean(extractField(denoiseExecution, "edge_inputs_available")),
                parseBoolean(extractField(denoiseExecution, "direct_shadow_signal_available")),
                parseBoolean(extractField(denoiseExecution, "diffuse_gi_signal_available")),
                parseBoolean(extractField(denoiseExecution, "optional_specular_placeholder")),
                parseBoolean(extractField(denoiseExecution, "optional_ao_placeholder")),
                parseBoolean(extractField(denoiseExecution, "ready")),
                parseBoolean(extractField(denoiseExecution, "accepted_this_dispatch")),
                extractField(denoiseExecution, "output_marker"),
                extractField(denoiseExecution, "readiness_reason")
        );
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.denoiseExecutionAvailable;
    }

    public boolean hasHistoryCounters() {
        return this.historyAcceptedCount > 0 || this.historyRejectedCount > 0;
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
                + " outputMarker=" + this.outputMarker
                + " reason=" + this.readinessReason;
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
