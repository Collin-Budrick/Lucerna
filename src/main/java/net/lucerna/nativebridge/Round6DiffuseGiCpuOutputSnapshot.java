package net.lucerna.nativebridge;

public record Round6DiffuseGiCpuOutputSnapshot(
        boolean nativeStatusAvailable,
        boolean diffuseGiExecutionAvailable,
        long dispatchGeneration,
        int sampleCount,
        int rayCount,
        int cacheReadCount,
        int cacheWriteCount,
        int outputCount,
        boolean enabled,
        boolean ready,
        boolean accepted,
        boolean cpuOutputGenerated,
        boolean outputWriteRecorded,
        boolean resolveRecorded,
        int outputWidth,
        int outputHeight,
        int outputPixels,
        double outputEnergy,
        String outputChecksum,
        long visibleSignalPopulationCount,
        long visibleSignalSampledPixels,
        long visibleSignalNonzeroPixels,
        boolean visibleSignalGenerated,
        boolean visibleSignalCacheBacked,
        long physicalGiSamples,
        long physicalGiHitSamples,
        long surfaceMaterialHitCoupledSamples,
        long geometryHitCoupledSamples,
        long physicalSceneLinkScore,
        String physicalOutputChecksum,
        boolean physicalSceneLinked,
        boolean physicalSurfaceContribution,
        String physicalSampleMarker,
        String surfaceMaterialHitMarker,
        String proofBoundaryMarker,
        String outputMarker,
        String readinessReason
) {
    public Round6DiffuseGiCpuOutputSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        sampleCount = Math.max(0, sampleCount);
        rayCount = Math.max(0, rayCount);
        cacheReadCount = Math.max(0, cacheReadCount);
        cacheWriteCount = Math.max(0, cacheWriteCount);
        outputCount = Math.max(0, outputCount);
        outputWidth = Math.max(0, outputWidth);
        outputHeight = Math.max(0, outputHeight);
        outputPixels = Math.max(0, outputPixels);
        outputEnergy = Double.isFinite(outputEnergy) ? Math.max(0.0D, outputEnergy) : 0.0D;
        outputChecksum = outputChecksum == null || outputChecksum.isBlank() ? "0" : outputChecksum;
        visibleSignalPopulationCount = Math.max(0L, visibleSignalPopulationCount);
        visibleSignalSampledPixels = Math.max(0L, visibleSignalSampledPixels);
        visibleSignalNonzeroPixels = Math.max(0L, visibleSignalNonzeroPixels);
        physicalGiSamples = Math.max(0L, physicalGiSamples);
        physicalGiHitSamples = Math.max(0L, physicalGiHitSamples);
        surfaceMaterialHitCoupledSamples = Math.max(0L, surfaceMaterialHitCoupledSamples);
        geometryHitCoupledSamples = Math.max(0L, geometryHitCoupledSamples);
        physicalSceneLinkScore = Math.max(0L, physicalSceneLinkScore);
        physicalOutputChecksum = physicalOutputChecksum == null || physicalOutputChecksum.isBlank()
                ? "0"
                : physicalOutputChecksum;
        physicalSampleMarker = physicalSampleMarker == null || physicalSampleMarker.isBlank()
                ? "unknown"
                : physicalSampleMarker;
        surfaceMaterialHitMarker = surfaceMaterialHitMarker == null || surfaceMaterialHitMarker.isBlank()
                ? "unknown"
                : surfaceMaterialHitMarker;
        proofBoundaryMarker = proofBoundaryMarker == null || proofBoundaryMarker.isBlank()
                ? "unknown"
                : proofBoundaryMarker;
        outputMarker = outputMarker == null || outputMarker.isBlank() ? "unknown" : outputMarker;
        readinessReason = readinessReason == null || readinessReason.isBlank() ? "unknown" : readinessReason;
    }

    public static Round6DiffuseGiCpuOutputSnapshot unavailable(String reason) {
        return new Round6DiffuseGiCpuOutputSnapshot(
                false,
                false,
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
                0,
                0,
                0,
                0.0D,
                "0",
                0L,
                0L,
                0L,
                false,
                false,
                0L,
                0L,
                0L,
                0L,
                0L,
                "0",
                false,
                false,
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                reason
        );
    }

    public static Round6DiffuseGiCpuOutputSnapshot fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status unavailable");
        }

        String diffuseGiExecution = extractBlock(nativeStatus, "diffuse_gi_execution={");
        if (diffuseGiExecution.isBlank()) {
            return new Round6DiffuseGiCpuOutputSnapshot(
                    true,
                    false,
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
                    0,
                    0,
                    0,
                    0.0D,
                    "0",
                    0L,
                    0L,
                    0L,
                    false,
                    false,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    "0",
                    false,
                    false,
                    "unknown",
                    "unknown",
                    "unknown",
                    "unknown",
                    "native diffuse GI execution status unavailable"
            );
        }

        int outputWidth = firstPositiveInt(
                extractField(diffuseGiExecution, "output_width"),
                dimensionComponent(extractField(diffuseGiExecution, "cpu_output_size"), 0),
                dimensionComponent(extractField(diffuseGiExecution, "size"), 0)
        );
        int outputHeight = firstPositiveInt(
                extractField(diffuseGiExecution, "output_height"),
                dimensionComponent(extractField(diffuseGiExecution, "cpu_output_size"), 1),
                dimensionComponent(extractField(diffuseGiExecution, "size"), 1)
        );
        int outputPixels = parseInt(firstPresent(
                extractField(diffuseGiExecution, "output_pixels"),
                extractField(diffuseGiExecution, "cpu_output_pixels")
        ));
        if (outputPixels <= 0) {
            outputPixels = saturatedPixelCount(outputWidth, outputHeight);
        }

        boolean visibleSignalGenerated = parseBoolean(extractField(diffuseGiExecution, "visible_signal_generated"));
        boolean metadataBackedNativePreview = parseBoolean(extractField(diffuseGiExecution, "enabled"))
                && outputWidth > 0
                && outputHeight > 0
                && outputPixels > 0
                && parseInt(extractField(diffuseGiExecution, "outputs")) > 0
                && (parseInt(extractField(diffuseGiExecution, "samples")) > 0
                || parseInt(extractField(diffuseGiExecution, "rays")) > 0);
        boolean cpuOutputGenerated = parseBoolean(extractField(diffuseGiExecution, "cpu_output_generated"))
                || visibleSignalGenerated
                || metadataBackedNativePreview;
        String outputChecksum = firstPresent(
                extractField(diffuseGiExecution, "output_checksum"),
                extractField(diffuseGiExecution, "cpu_output_checksum"),
                extractField(diffuseGiExecution, "visible_signal_checksum")
        );
        double outputEnergy = parseDouble(firstPresent(
                extractField(diffuseGiExecution, "output_energy"),
                extractField(diffuseGiExecution, "cpu_output_energy"),
                extractField(diffuseGiExecution, "visible_signal_energy")
        ));

        return new Round6DiffuseGiCpuOutputSnapshot(
                true,
                true,
                parseLong(extractField(diffuseGiExecution, "dispatch_generation")),
                parseInt(extractField(diffuseGiExecution, "samples")),
                parseInt(extractField(diffuseGiExecution, "rays")),
                parseInt(extractField(diffuseGiExecution, "cache_reads")),
                parseInt(extractField(diffuseGiExecution, "cache_writes")),
                parseInt(extractField(diffuseGiExecution, "outputs")),
                parseBoolean(extractField(diffuseGiExecution, "enabled")),
                parseBoolean(extractField(diffuseGiExecution, "ready")),
                parseBoolean(extractField(diffuseGiExecution, "accepted_this_dispatch")),
                cpuOutputGenerated,
                parseBoolean(extractField(diffuseGiExecution, "output_write_recorded"))
                        || visibleSignalGenerated
                        || metadataBackedNativePreview,
                parseBoolean(extractField(diffuseGiExecution, "resolve_recorded"))
                        || visibleSignalGenerated
                        || metadataBackedNativePreview,
                outputWidth,
                outputHeight,
                outputPixels,
                outputEnergy,
                outputChecksum,
                parseLong(extractField(diffuseGiExecution, "visible_signal_population_count")),
                parseLong(extractField(diffuseGiExecution, "visible_signal_sampled_pixels")),
                parseLong(extractField(diffuseGiExecution, "visible_signal_nonzero_pixels")),
                visibleSignalGenerated,
                parseBoolean(extractField(diffuseGiExecution, "visible_signal_cache_backed")),
                parseLong(extractField(diffuseGiExecution, "physical_gi_samples")),
                parseLong(extractField(diffuseGiExecution, "physical_gi_hit_samples")),
                parseLong(extractField(diffuseGiExecution, "surface_material_hit_coupled_samples")),
                parseLong(extractField(diffuseGiExecution, "geometry_hit_coupled_samples")),
                parseLong(extractField(diffuseGiExecution, "physical_scene_link_score")),
                extractField(diffuseGiExecution, "physical_output_checksum"),
                parseBoolean(extractField(diffuseGiExecution, "physical_scene_linked")),
                parseBoolean(extractField(diffuseGiExecution, "physical_surface_contribution")),
                extractField(diffuseGiExecution, "physical_sample_marker"),
                extractField(diffuseGiExecution, "surface_material_hit_marker"),
                extractField(diffuseGiExecution, "proof_boundary_marker"),
                extractField(diffuseGiExecution, "output_marker"),
                extractField(diffuseGiExecution, "readiness_reason")
        );
    }

    public boolean hasExecutionTelemetry() {
        return this.nativeStatusAvailable && this.diffuseGiExecutionAvailable;
    }

    public boolean hasCpuOutputTelemetry() {
        return this.hasExecutionTelemetry()
                && this.cpuOutputGenerated
                && this.outputWidth > 0
                && this.outputHeight > 0
                && this.outputPixels > 0;
    }

    public boolean hasNonzeroEnergy() {
        return this.outputEnergy > 0.0D
                || !"0".equals(this.outputChecksum)
                || this.visibleSignalNonzeroPixels > 0L
                || (this.cpuOutputGenerated && this.outputPixels > 0 && (this.sampleCount > 0 || this.rayCount > 0));
    }

    public boolean readyForPreviewPayload() {
        return this.hasCpuOutputTelemetry()
                && this.hasNonzeroEnergy()
                && this.enabled
                && this.outputCount > 0
                && this.outputWriteRecorded
                && this.resolveRecorded;
    }

    public boolean physicalSceneTiedGiEvidenceReady() {
        return this.readyForPreviewPayload()
                && this.physicalSceneLinked
                && this.physicalSurfaceContribution
                && this.physicalGiSamples > 0L
                && this.physicalGiHitSamples > 0L
                && this.surfaceMaterialHitCoupledSamples > 0L
                && this.geometryHitCoupledSamples > 0L;
    }

    public boolean spatiallyVaryingGiPayloadReady() {
        return this.readyForPreviewPayload()
                && this.outputPixels > 1
                && (this.visibleSignalNonzeroPixels > 0L
                || this.physicalGiHitSamples > 0L
                || this.physicalSceneLinkScore > 0L)
                && !"0".equals(this.outputChecksum);
    }

    public boolean gpuTraversalExecuted() {
        return false;
    }

    public boolean nativeComputeGiExecuted() {
        return false;
    }

    public String physicalRendererNoOverclaimBlockers() {
        StringBuilder builder = new StringBuilder();
        appendBlocker(builder, !this.gpuTraversalExecuted(),
                "gpuTraversalExecuted=false: Round 6 GI payload is CPU/native readback telemetry, not GPU traversal");
        appendBlocker(builder, !this.nativeComputeGiExecuted(),
                "nativeComputeGiExecuted=false: no native Vulkan compute GI execution is reported by this DTO");
        appendBlocker(builder, !this.physicalSceneTiedGiEvidenceReady(),
                "physical scene/material coupled GI evidence incomplete");
        return builder.length() == 0 ? "none" : builder.toString();
    }

    public String spatialGiPayloadSummary() {
        return "spatiallyVaryingGiPayloadReady=" + this.spatiallyVaryingGiPayloadReady()
                + " physicalSceneTiedGiEvidenceReady=" + this.physicalSceneTiedGiEvidenceReady()
                + " sourceKind=raw-diffuse-gi-rgba8-cpu-readback"
                + " outputSize=" + this.outputWidth + "x" + this.outputHeight
                + " outputPixels=" + this.outputPixels
                + " outputChecksum=" + this.outputChecksum
                + " visibleSignalNonzeroPixels=" + this.visibleSignalNonzeroPixels
                + " physicalGiSamples=" + this.physicalGiSamples
                + " physicalGiHitSamples=" + this.physicalGiHitSamples
                + " surfaceMaterialHitCoupledSamples=" + this.surfaceMaterialHitCoupledSamples
                + " geometryHitCoupledSamples=" + this.geometryHitCoupledSamples
                + " physicalSceneLinkScore=" + this.physicalSceneLinkScore
                + " physicalSceneLinked=" + this.physicalSceneLinked
                + " physicalSurfaceContribution=" + this.physicalSurfaceContribution
                + " gpuTraversalExecuted=" + this.gpuTraversalExecuted()
                + " nativeComputeGiExecuted=" + this.nativeComputeGiExecuted()
                + " noOverclaimBlockers=\"" + this.physicalRendererNoOverclaimBlockers() + "\"";
    }

    public String pixelPayloadStatus() {
        if (!this.hasCpuOutputTelemetry()) {
            return "native Round 6 diffuse GI CPU output telemetry is not available";
        }
        return "native reports Round 6 diffuse GI CPU output dimensions, energy, and checksum; Java can request a bounded RGBA8 preview payload";
    }

    public String debugSummary() {
        if (!this.hasExecutionTelemetry()) {
            return this.readinessReason;
        }
        return "round6DiffuseGiCpuOutput ready=" + this.ready
                + " accepted=" + this.accepted
                + " generated=" + this.cpuOutputGenerated
                + " size=" + this.outputWidth + "x" + this.outputHeight
                + " pixels=" + this.outputPixels
                + " samples=" + this.sampleCount
                + " rays=" + this.rayCount
                + " cacheReads=" + this.cacheReadCount
                + " cacheWrites=" + this.cacheWriteCount
                + " energy=" + this.outputEnergy
                + " checksum=" + this.outputChecksum
                + " visibleSignalGenerated=" + this.visibleSignalGenerated
                + " visibleSignalNonzeroPixels=" + this.visibleSignalNonzeroPixels
                + " physicalGiSamples=" + this.physicalGiSamples
                + " physicalGiHitSamples=" + this.physicalGiHitSamples
                + " surfaceMaterialHitCoupledSamples=" + this.surfaceMaterialHitCoupledSamples
                + " geometryHitCoupledSamples=" + this.geometryHitCoupledSamples
                + " physicalSceneLinked=" + this.physicalSceneLinked
                + " physicalSurfaceContribution=" + this.physicalSurfaceContribution
                + " " + this.spatialGiPayloadSummary()
                + " outputMarker=" + this.outputMarker
                + " reason=" + this.readinessReason;
    }

    private static void appendBlocker(StringBuilder builder, boolean active, String message) {
        if (!active) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(message);
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

    private static String dimensionComponent(String dimensions, int component) {
        if (dimensions == null || dimensions.isBlank()) {
            return "";
        }
        String[] parts = dimensions.trim().split("x", 3);
        return component >= 0 && component < parts.length ? parts[component] : "";
    }

    private static String firstPresent(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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

    private static int firstPositiveInt(String... values) {
        if (values == null) {
            return 0;
        }
        for (String value : values) {
            int parsed = parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        }
        return 0;
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

    private static int saturatedPixelCount(int width, int height) {
        long pixels = Math.max(0L, width) * Math.max(0L, height);
        return pixels > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pixels;
    }
}
