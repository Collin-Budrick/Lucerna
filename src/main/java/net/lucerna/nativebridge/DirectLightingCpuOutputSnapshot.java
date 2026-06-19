package net.lucerna.nativebridge;

public record DirectLightingCpuOutputSnapshot(
        boolean nativeStatusAvailable,
        boolean directExecutionAvailable,
        long dispatchGeneration,
        int candidateCount,
        int sampleCount,
        int rayCount,
        int outputCount,
        long outputWrites,
        long resolves,
        boolean outputWriteRecorded,
        boolean resolveRecorded,
        boolean ready,
        boolean cpuOutputGenerated,
        int outputWidth,
        int outputHeight,
        int outputPixels,
        double outputEnergy,
        String outputChecksum,
        String readinessReason
) {
    public DirectLightingCpuOutputSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        candidateCount = Math.max(0, candidateCount);
        sampleCount = Math.max(0, sampleCount);
        rayCount = Math.max(0, rayCount);
        outputCount = Math.max(0, outputCount);
        outputWrites = Math.max(0L, outputWrites);
        resolves = Math.max(0L, resolves);
        outputWidth = Math.max(0, outputWidth);
        outputHeight = Math.max(0, outputHeight);
        outputPixels = Math.max(0, outputPixels);
        outputEnergy = Double.isFinite(outputEnergy) ? Math.max(0.0D, outputEnergy) : 0.0D;
        outputChecksum = outputChecksum == null || outputChecksum.isBlank() ? "0" : outputChecksum;
        readinessReason = readinessReason == null || readinessReason.isBlank() ? "unknown" : readinessReason;
    }

    public static DirectLightingCpuOutputSnapshot unavailable(String reason) {
        return new DirectLightingCpuOutputSnapshot(
                false,
                false,
                0L,
                0,
                0,
                0,
                0,
                0L,
                0L,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                0.0D,
                "0",
                reason
        );
    }

    public static DirectLightingCpuOutputSnapshot fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status unavailable");
        }

        String directExecution = extractBlock(nativeStatus, "direct_execution={");
        if (directExecution.isBlank()) {
            return new DirectLightingCpuOutputSnapshot(
                    true,
                    false,
                    0L,
                    0,
                    0,
                    0,
                    0,
                    0L,
                    0L,
                    false,
                    false,
                    false,
                    false,
                    0,
                    0,
                    0,
                    0.0D,
                    "0",
                    "native direct execution status unavailable"
            );
        }

        return new DirectLightingCpuOutputSnapshot(
                true,
                true,
                parseLong(extractField(directExecution, "dispatch_generation")),
                parseInt(extractField(directExecution, "candidate_count")),
                parseInt(extractField(directExecution, "sample_count")),
                parseInt(extractField(directExecution, "ray_count")),
                parseInt(extractField(directExecution, "output_count")),
                parseLong(extractField(directExecution, "output_writes")),
                parseLong(extractField(directExecution, "resolves")),
                parseBoolean(extractField(directExecution, "output_write_recorded")),
                parseBoolean(extractField(directExecution, "resolve_recorded")),
                parseBoolean(extractField(directExecution, "ready")),
                parseBoolean(extractField(directExecution, "cpu_output_generated")),
                parseInt(extractField(directExecution, "output_width")),
                parseInt(extractField(directExecution, "output_height")),
                parseInt(extractField(directExecution, "output_pixels")),
                parseDouble(extractField(directExecution, "output_energy")),
                extractField(directExecution, "output_checksum"),
                extractField(directExecution, "readiness_reason")
        );
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.directExecutionAvailable;
    }

    public boolean hasCpuOutputTelemetry() {
        return this.hasExecutionTelemetry()
                && this.cpuOutputGenerated
                && this.outputWidth > 0
                && this.outputHeight > 0
                && this.outputPixels > 0;
    }

    public boolean hasNonzeroEnergy() {
        return this.outputEnergy > 0.0D || !"0".equals(this.outputChecksum);
    }

    public boolean hasPixelPayload() {
        return false;
    }

    public String pixelPayloadStatus() {
        if (!this.hasCpuOutputTelemetry()) {
            return "native CPU direct-light output telemetry is not available";
        }
        return "native reports CPU direct-light output dimensions, energy, and checksum; Java pixel payload access is not exposed yet";
    }

    public String debugSummary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        return "directCpuOutput ready=" + this.ready
                + " generated=" + this.cpuOutputGenerated
                + " size=" + this.outputWidth + "x" + this.outputHeight
                + " pixels=" + this.outputPixels
                + " energy=" + this.outputEnergy
                + " checksum=" + this.outputChecksum
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
        int markerStart = block.indexOf(marker);
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

    private static double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? 0.0D : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
