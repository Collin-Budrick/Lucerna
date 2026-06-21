package net.lucerna.nativebridge;

public record DirectionalShadowMapOutputSnapshot(
        boolean nativeStatusAvailable,
        boolean shadowMapStatusAvailable,
        boolean attempted,
        boolean generated,
        boolean realShadowMapAttempted,
        boolean realShadowMapGenerated,
        boolean conservativeCpuOutput,
        boolean gpuShadowMapGenerated,
        boolean hardwareRtShadowMapGenerated,
        boolean consumedByLighting,
        long dispatchGeneration,
        int texelWidth,
        int texelHeight,
        int texelCount,
        int casterCount,
        int receiverCount,
        int depthSamplesWritten,
        int outputSampleCount,
        int outputCasterCount,
        int outputReceiverCount,
        int receiverCandidateCount,
        int casterCandidateCount,
        int receiverRejectedCount,
        int casterRejectedCount,
        int depthTexelCount,
        int depthCoveredTexelCount,
        int depthUncoveredTexelCount,
        String checksum,
        String marker,
        String blocker,
        String receiverBlocker,
        String casterBlocker,
        String depthBlocker,
        String hardwareRtBlocker,
        String readinessReason
) {
    public DirectionalShadowMapOutputSnapshot {
        dispatchGeneration = Math.max(0L, dispatchGeneration);
        texelWidth = Math.max(0, texelWidth);
        texelHeight = Math.max(0, texelHeight);
        int derivedTexelCount = saturatingPixelCount(texelWidth, texelHeight);
        texelCount = texelCount > 0 ? texelCount : derivedTexelCount;
        casterCount = Math.max(0, casterCount);
        receiverCount = Math.max(0, receiverCount);
        depthSamplesWritten = Math.max(0, depthSamplesWritten);
        outputSampleCount = outputSampleCount > 0 ? outputSampleCount : depthSamplesWritten;
        outputCasterCount = outputCasterCount > 0 ? outputCasterCount : casterCount;
        outputReceiverCount = outputReceiverCount > 0 ? outputReceiverCount : receiverCount;
        receiverCandidateCount = Math.max(receiverCount, receiverCandidateCount);
        casterCandidateCount = Math.max(casterCount, casterCandidateCount);
        receiverRejectedCount = Math.max(0, receiverRejectedCount);
        casterRejectedCount = Math.max(0, casterRejectedCount);
        depthTexelCount = depthTexelCount > 0 ? depthTexelCount : texelCount;
        depthCoveredTexelCount = depthCoveredTexelCount > 0
                ? (depthTexelCount > 0 ? Math.min(depthCoveredTexelCount, depthTexelCount) : depthCoveredTexelCount)
                : Math.min(depthSamplesWritten, depthTexelCount);
        int derivedUncoveredTexels = Math.max(0, depthTexelCount - depthCoveredTexelCount);
        depthUncoveredTexelCount = Math.max(0, depthUncoveredTexelCount);
        if (depthUncoveredTexelCount == 0 && derivedUncoveredTexels > 0) {
            depthUncoveredTexelCount = derivedUncoveredTexels;
        }
        checksum = checksum == null || checksum.isBlank() ? "0" : checksum.trim();
        marker = marker == null || marker.isBlank() ? "missing" : marker.trim();
        blocker = blocker == null || blocker.isBlank() ? "none" : blocker.trim();
        receiverBlocker = receiverBlocker == null || receiverBlocker.isBlank() ? "none" : receiverBlocker.trim();
        casterBlocker = casterBlocker == null || casterBlocker.isBlank() ? "none" : casterBlocker.trim();
        depthBlocker = depthBlocker == null || depthBlocker.isBlank() ? "none" : depthBlocker.trim();
        hardwareRtBlocker = hardwareRtBlocker == null || hardwareRtBlocker.isBlank()
                ? "native conservative shadow-map output is CPU generated, not hardware RT"
                : hardwareRtBlocker.trim();
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? "native directional shadow-map readiness is unknown"
                : readinessReason.trim();
    }

    public static DirectionalShadowMapOutputSnapshot unavailable(String reason) {
        return new DirectionalShadowMapOutputSnapshot(
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
                0,
                0,
                "0",
                "missing",
                "unavailable",
                "unavailable",
                "unavailable",
                "unavailable",
                "native conservative shadow-map output is CPU generated, not hardware RT",
                reason
        );
    }

    public static DirectionalShadowMapOutputSnapshot fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status unavailable");
        }

        String shadowMap = extractBlock(nativeStatus, "directional_shadow_map={");
        if (shadowMap.isBlank()) {
            return new DirectionalShadowMapOutputSnapshot(
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
                    0,
                    0,
                    "0",
                    "missing",
                    "directional_shadow_map_status_missing",
                    "directional_shadow_map_status_missing",
                    "directional_shadow_map_status_missing",
                    "directional_shadow_map_status_missing",
                    "native conservative shadow-map output is CPU generated, not hardware RT",
                    "native directional shadow-map status unavailable"
            );
        }

        int texelWidth = firstPositiveInt(
                extractField(shadowMap, "texel_width"),
                extractField(shadowMap, "width"),
                dimensionComponent(extractField(shadowMap, "dimensions"), 0)
        );
        int texelHeight = firstPositiveInt(
                extractField(shadowMap, "texel_height"),
                extractField(shadowMap, "height"),
                dimensionComponent(extractField(shadowMap, "dimensions"), 1)
        );
        int texelCount = firstPositiveInt(
                extractField(shadowMap, "texel_count"),
                extractField(shadowMap, "pixel_count"),
                extractField(shadowMap, "output_pixels")
        );
        boolean generated = parseBoolean(extractField(shadowMap, "generated"));
        boolean realShadowMapGenerated = parseBoolean(extractField(shadowMap, "realShadowMapGenerated"));
        boolean conservativeCpuOutput = parseBoolean(firstPresent(
                extractField(shadowMap, "conservativeCpuOutput"),
                extractField(shadowMap, "conservative_cpu_output"),
                generated || realShadowMapGenerated ? "true" : "false"
        ));
        boolean gpuShadowMapGenerated = parseBoolean(firstPresent(
                extractField(shadowMap, "gpuShadowMapGenerated"),
                extractField(shadowMap, "gpu_shadow_map_generated")
        ));
        boolean hardwareRtShadowMapGenerated = parseBoolean(firstPresent(
                extractField(shadowMap, "hardwareRtShadowMapGenerated"),
                extractField(shadowMap, "hardwareRtShadowMap"),
                extractField(shadowMap, "hardware_rt_shadow_map_generated"),
                extractField(shadowMap, "hardware_rt_shadow_map")
        ));
        boolean consumedByLighting = parseBoolean(firstPresent(
                extractField(shadowMap, "consumedByLighting"),
                extractField(shadowMap, "consumed_by_lighting"),
                extractField(shadowMap, "shadowMapConsumedByLighting"),
                extractField(shadowMap, "shadow_map_consumed_by_lighting")
        ));
        int casterCount = parseInt(extractField(shadowMap, "caster_count"));
        int receiverCount = parseInt(extractField(shadowMap, "receiver_count"));
        int depthSamplesWritten = parseInt(firstPresent(
                extractField(shadowMap, "depth_samples_written"),
                extractField(shadowMap, "depth_sample_count"),
                extractField(shadowMap, "sample_count")
        ));
        int outputSampleCount = firstPositiveInt(
                extractField(shadowMap, "shadow_map_output_sample_count"),
                extractField(shadowMap, "output_sample_count"),
                extractField(shadowMap, "sample_count")
        );
        int outputCasterCount = firstPositiveInt(
                extractField(shadowMap, "shadow_map_output_caster_count"),
                extractField(shadowMap, "output_caster_count"),
                extractField(shadowMap, "caster_count")
        );
        int outputReceiverCount = firstPositiveInt(
                extractField(shadowMap, "shadow_map_output_receiver_count"),
                extractField(shadowMap, "output_receiver_count"),
                extractField(shadowMap, "receiver_count")
        );
        int receiverCandidateCount = firstPositiveInt(
                extractField(shadowMap, "receiver_candidate_count"),
                extractField(shadowMap, "candidate_receiver_count")
        );
        int casterCandidateCount = firstPositiveInt(
                extractField(shadowMap, "caster_candidate_count"),
                extractField(shadowMap, "candidate_caster_count")
        );
        int receiverRejectedCount = parseInt(firstPresent(
                extractField(shadowMap, "receiver_rejected_count"),
                extractField(shadowMap, "rejected_receiver_count")
        ));
        int casterRejectedCount = parseInt(firstPresent(
                extractField(shadowMap, "caster_rejected_count"),
                extractField(shadowMap, "rejected_caster_count")
        ));
        int depthTexelCount = firstPositiveInt(
                extractField(shadowMap, "depth_texel_count"),
                extractField(shadowMap, "depth_map_texel_count"),
                Integer.toString(texelCount)
        );
        int depthCoveredTexelCount = firstPositiveInt(
                extractField(shadowMap, "depth_covered_texel_count"),
                extractField(shadowMap, "depth_map_covered_texel_count")
        );
        int depthUncoveredTexelCount = parseInt(firstPresent(
                extractField(shadowMap, "depth_uncovered_texel_count"),
                extractField(shadowMap, "depth_map_uncovered_texel_count")
        ));
        String checksum = firstPresent(
                extractField(shadowMap, "checksum"),
                extractField(shadowMap, "output_checksum"),
                "0"
        );
        String blocker = firstPresent(extractField(shadowMap, "blocker"), "none");
        String receiverBlocker = firstPresent(extractField(shadowMap, "receiver_blocker"), "none");
        String casterBlocker = firstPresent(extractField(shadowMap, "caster_blocker"), "none");
        String depthBlocker = firstPresent(extractField(shadowMap, "depth_blocker"), "none");
        String hardwareRtBlocker = firstPresent(
                extractField(shadowMap, "hardware_rt_blocker"),
                extractField(shadowMap, "hardwareRtBlocker"),
                "native conservative shadow-map output is CPU generated, not hardware RT"
        );

        return new DirectionalShadowMapOutputSnapshot(
                true,
                true,
                parseBoolean(extractField(shadowMap, "attempted")),
                generated,
                parseBoolean(extractField(shadowMap, "realShadowMapAttempted")),
                realShadowMapGenerated,
                conservativeCpuOutput,
                gpuShadowMapGenerated,
                hardwareRtShadowMapGenerated,
                consumedByLighting,
                parseLong(firstPresent(
                        extractField(shadowMap, "dispatch_generation"),
                        extractField(shadowMap, "generation")
                )),
                texelWidth,
                texelHeight,
                texelCount,
                casterCount,
                receiverCount,
                depthSamplesWritten,
                outputSampleCount,
                outputCasterCount,
                outputReceiverCount,
                receiverCandidateCount,
                casterCandidateCount,
                receiverRejectedCount,
                casterRejectedCount,
                depthTexelCount,
                depthCoveredTexelCount,
                depthUncoveredTexelCount,
                checksum,
                firstPresent(extractField(shadowMap, "marker"), "missing"),
                blocker,
                receiverBlocker,
                casterBlocker,
                depthBlocker,
                hardwareRtBlocker,
                readinessReasonFor(
                        generated,
                        realShadowMapGenerated,
                        texelWidth,
                        texelHeight,
                        texelCount,
                        casterCount,
                        receiverCount,
                        depthSamplesWritten,
                        outputSampleCount,
                        outputCasterCount,
                        outputReceiverCount,
                        depthTexelCount,
                        depthCoveredTexelCount,
                        checksum,
                        blocker,
                        receiverBlocker,
                        casterBlocker,
                        depthBlocker
                )
        );
    }

    public boolean hasShadowMapTelemetry() {
        return this.nativeStatusAvailable && this.shadowMapStatusAvailable;
    }

    public boolean hasOutputDimensions() {
        return this.texelWidth > 0
                && this.texelHeight > 0
                && this.texelCount > 0
                && (long) this.texelWidth * (long) this.texelHeight == this.texelCount;
    }

    public boolean hasCasterReceiverEvidence() {
        return this.casterCount > 0 && this.receiverCount > 0;
    }

    public boolean hasDepthSamples() {
        return this.depthSamplesWritten > 0 && this.outputSampleCount > 0;
    }

    public boolean hasOutputCounts() {
        return this.outputSampleCount > 0
                && this.outputCasterCount > 0
                && this.outputReceiverCount > 0;
    }

    public boolean hasDepthMapCoverage() {
        return this.depthTexelCount > 0
                && this.depthCoveredTexelCount > 0
                && this.depthCoveredTexelCount <= this.depthTexelCount;
    }

    public boolean hasNonzeroChecksum() {
        return this.checksum != null && !this.checksum.isBlank() && !"0".equals(this.checksum);
    }

    public boolean realShadowMapOutputReady() {
        return this.hasShadowMapTelemetry()
                && this.generated
                && this.realShadowMapGenerated
                && this.hasOutputDimensions()
                && this.hasCasterReceiverEvidence()
                && this.hasDepthSamples()
                && this.hasOutputCounts()
                && this.hasDepthMapCoverage()
                && this.hasNonzeroChecksum();
    }

    public boolean readyForPreviewPayload() {
        return this.realShadowMapOutputReady();
    }

    public boolean honestGpuBoundary() {
        return !this.gpuShadowMapGenerated && !this.hardwareRtShadowMapGenerated && !this.consumedByLighting;
    }

    public int expectedByteCount() {
        long bytes = Math.max(0L, this.texelCount) * 4L;
        return bytes > Integer.MAX_VALUE ? -1 : (int) bytes;
    }

    public String previewReadinessReason() {
        if (this.realShadowMapOutputReady()) {
            return "native directional shadow-map output has dimensions, caster/receiver/output counts, depth-map coverage, and checksum";
        }
        return this.readinessReason;
    }

    public String boundarySummary() {
        return "conservativeCpuOutput=" + this.conservativeCpuOutput
                + " gpuShadowMapGenerated=" + this.gpuShadowMapGenerated
                + " hardwareRtShadowMapGenerated=" + this.hardwareRtShadowMapGenerated
                + " consumedByLighting=" + this.consumedByLighting
                + " hardwareRtBlocker=" + this.hardwareRtBlocker
                + " honestGpuBoundary=" + this.honestGpuBoundary();
    }

    public String debugSummary() {
        if (!this.hasShadowMapTelemetry()) {
            return this.readinessReason;
        }
        return "directionalShadowMap ready=" + this.realShadowMapOutputReady()
                + " attempted=" + this.attempted
                + " generated=" + this.generated
                + " realAttempted=" + this.realShadowMapAttempted
                + " realGenerated=" + this.realShadowMapGenerated
                + " size=" + this.texelWidth + "x" + this.texelHeight
                + " texels=" + this.texelCount
                + " casters=" + this.casterCount
                + " receivers=" + this.receiverCount
                + " depthSamples=" + this.depthSamplesWritten
                + " outputSamples=" + this.outputSampleCount
                + " outputCasters=" + this.outputCasterCount
                + " outputReceivers=" + this.outputReceiverCount
                + " receiverCandidates=" + this.receiverCandidateCount
                + " casterCandidates=" + this.casterCandidateCount
                + " receiverRejected=" + this.receiverRejectedCount
                + " casterRejected=" + this.casterRejectedCount
                + " depthTexels=" + this.depthTexelCount
                + " depthCoveredTexels=" + this.depthCoveredTexelCount
                + " depthUncoveredTexels=" + this.depthUncoveredTexelCount
                + " checksum=" + this.checksum
                + " marker=" + this.marker
                + " blocker=" + this.blocker
                + " receiverBlocker=" + this.receiverBlocker
                + " casterBlocker=" + this.casterBlocker
                + " depthBlocker=" + this.depthBlocker
                + " " + this.boundarySummary()
                + " reason=" + this.previewReadinessReason();
    }

    private static String readinessReasonFor(
            boolean generated,
            boolean realShadowMapGenerated,
            int texelWidth,
            int texelHeight,
            int texelCount,
            int casterCount,
            int receiverCount,
            int depthSamplesWritten,
            int outputSampleCount,
            int outputCasterCount,
            int outputReceiverCount,
            int depthTexelCount,
            int depthCoveredTexelCount,
            String checksum,
            String blocker,
            String receiverBlocker,
            String casterBlocker,
            String depthBlocker
    ) {
        if (!generated || !realShadowMapGenerated) {
            return "native directional shadow-map output has not been generated; blocker=" + blocker;
        }
        int expectedTexels = saturatingPixelCount(texelWidth, texelHeight);
        int effectiveTexelCount = texelCount > 0 ? texelCount : expectedTexels;
        if (texelWidth <= 0 || texelHeight <= 0 || effectiveTexelCount <= 0 || expectedTexels != effectiveTexelCount) {
            return "native directional shadow-map dimensions are incomplete";
        }
        if (casterCount <= 0 || receiverCount <= 0) {
            return "native directional shadow-map caster/receiver evidence is incomplete";
        }
        if (outputCasterCount <= 0 || outputReceiverCount <= 0) {
            return "native directional shadow-map output caster/receiver counts are incomplete"
                    + "; casterBlocker=" + casterBlocker
                    + "; receiverBlocker=" + receiverBlocker;
        }
        if (depthSamplesWritten <= 0) {
            return "native directional shadow-map has no written depth samples";
        }
        if (outputSampleCount <= 0) {
            return "native directional shadow-map output sample count is empty";
        }
        if (depthTexelCount <= 0 || depthCoveredTexelCount <= 0) {
            return "native directional shadow-map depth map coverage is incomplete; depthBlocker=" + depthBlocker;
        }
        if (checksum == null || checksum.isBlank() || "0".equals(checksum)) {
            return "native directional shadow-map checksum is empty";
        }
        return "native directional shadow-map output is ready for conservative CPU RGBA8 preview payload; not hardware RT";
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

    private static String firstPresent(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String dimensionComponent(String dimensions, int component) {
        if (dimensions == null || dimensions.isBlank()) {
            return "";
        }
        String[] parts = dimensions.toLowerCase().split("x", 2);
        if (component < 0 || component >= parts.length) {
            return "";
        }
        return parts[component].trim();
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

    private static int saturatingPixelCount(int width, int height) {
        long pixels = Math.max(0L, width) * Math.max(0L, height);
        return pixels > Integer.MAX_VALUE ? 0 : (int) pixels;
    }
}
