package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record LightingDispatchStageTelemetryStatus(
        String stageId,
        Boolean enabled,
        Long generation,
        String dispatchGroups,
        String dimensions,
        String ioCounts,
        Long sampleCount,
        Long candidateCount,
        Long rayCount,
        Long cacheReadCount,
        Long cacheWriteCount,
        Long flags,
        Boolean placeholder,
        Boolean validated,
        Boolean debugOverlay,
        Boolean readyForNativeExecution,
        String readinessReason,
        Long frameIndex,
        Boolean recordedThisFrame,
        Boolean payloadAccepted,
        Long payloadGeneration,
        String payloadGenerationRange,
        Long payloadFrameIndex,
        Long celestialCount,
        Long emissiveCount,
        Long shadowCandidateCount,
        Long budgetedShadowCandidateCount,
        Long sectionSnapshotCount,
        Boolean metadataOnly,
        Boolean cpuOutputGenerated,
        String outputDimensions,
        Long outputPixelCount,
        String outputEnergy,
        Long outputChecksum,
        Boolean payloadValidated,
        Boolean payloadHasDirectWork,
        Boolean payloadReadyForShadowTracing,
        Map<String, String> details
) {
    public LightingDispatchStageTelemetryStatus {
        stageId = cleanStageId(stageId);
        dispatchGroups = blankToEmpty(stripQuotes(dispatchGroups));
        dimensions = blankToEmpty(stripQuotes(dimensions));
        ioCounts = blankToEmpty(stripQuotes(ioCounts));
        readinessReason = blankToEmpty(stripQuotes(readinessReason));
        payloadGenerationRange = blankToEmpty(stripQuotes(payloadGenerationRange));
        outputDimensions = blankToEmpty(stripQuotes(outputDimensions));
        outputEnergy = blankToEmpty(stripQuotes(outputEnergy));
        details = immutable(details);
    }

    public static LightingDispatchStageTelemetryStatus fromFields(Map<String, String> fields) {
        Map<String, String> normalizedFields = normalizeFields(fields);
        String stageId = firstPresent(normalizedFields, "id", "stage", "stage_id", "stage_name", "name");
        Boolean enabled = parseBoolean(firstPresent(normalizedFields, "enabled_this_packet", "enabled", "active"));
        Long generation = parseLong(firstPresent(
                normalizedFields,
                "last_generation",
                "generation",
                "dispatch_generation",
                "stage_generation"
        ));
        String dispatchGroups = firstPresent(
                normalizedFields,
                "last_dispatch",
                "dispatch",
                "dispatch_groups",
                "groups",
                "group_count"
        );
        if (dispatchGroups.isBlank()) {
            dispatchGroups = xyzLabel(
                    firstPresent(normalizedFields, "dispatch_x", "groups_x", "group_x"),
                    firstPresent(normalizedFields, "dispatch_y", "groups_y", "group_y"),
                    firstPresent(normalizedFields, "dispatch_z", "groups_z", "group_z")
            );
        }

        String dimensions = firstPresent(normalizedFields, "last_size", "size", "dimensions", "resolution");
        if (dimensions.isBlank()) {
            dimensions = xyLabel(
                    firstPresent(normalizedFields, "width", "last_width"),
                    firstPresent(normalizedFields, "height", "last_height")
            );
        }

        String ioCounts = firstPresent(normalizedFields, "last_io", "io", "io_counts");
        if (ioCounts.isBlank()) {
            ioCounts = pairLabel(
                    firstPresent(normalizedFields, "inputs", "input_count", "last_input_count"),
                    firstPresent(normalizedFields, "outputs", "output_count", "last_output_count")
            );
        }

        Long sampleCount = parseLong(firstPresent(
                normalizedFields,
                "last_samples",
                "samples",
                "sample_count",
                "last_sample_count"
        ));
        Long candidateCount = parseLong(firstPresent(
                normalizedFields,
                "last_candidates",
                "candidates",
                "candidate_count",
                "last_candidate_count",
                "shadow_candidates",
                "shadow_candidate_count",
                "direct_shadow_candidates"
        ));
        Long rayCount = parseLong(firstPresent(normalizedFields, "last_rays", "rays", "ray_count"));
        Long cacheReadCount = parseLong(firstPresent(
                normalizedFields,
                "cache_reads",
                "cache_read_count",
                "last_cache_reads"
        ));
        Long cacheWriteCount = parseLong(firstPresent(
                normalizedFields,
                "cache_writes",
                "cache_write_count",
                "last_cache_writes"
        ));

        String cachePair = firstPresent(normalizedFields, "last_cache", "cache", "cache_counts");
        Long[] parsedCachePair = parseLongPair(cachePair);
        if (cacheReadCount == null) {
            cacheReadCount = parsedCachePair[0];
        }
        if (cacheWriteCount == null) {
            cacheWriteCount = parsedCachePair[1];
        }
        Long flags = parseLong(firstPresent(normalizedFields, "last_flags", "flags", "stage_flags"));
        Boolean placeholder = parseBoolean(firstPresent(normalizedFields, "placeholder", "metadata_only"));
        Boolean validated = parseBoolean(firstPresent(normalizedFields, "validated", "valid"));
        Boolean debugOverlay = parseBoolean(firstPresent(normalizedFields, "debug_overlay", "debug"));
        Boolean readyForNativeExecution = parseBoolean(firstPresent(
                normalizedFields,
                "ready_for_native_execution",
                "native_ready",
                "ready",
                "executable"
        ));
        String readinessReason = firstPresent(
                normalizedFields,
                "readiness_reason",
                "ready_reason",
                "native_readiness_reason",
                "reason"
        );
        Long frameIndex = parseLong(firstPresent(
                normalizedFields,
                "last_frame",
                "frame",
                "frame_index",
                "last_frame_index",
                "dispatch_frame"
        ));
        Boolean recordedThisFrame = parseBoolean(firstPresent(
                normalizedFields,
                "recorded_this_frame",
                "recorded",
                "submitted_this_frame"
        ));
        Boolean payloadAccepted = parseBoolean(firstPresent(
                normalizedFields,
                "payload_accepted",
                "direct_payload_accepted",
                "accepted"
        ));
        Long payloadGeneration = parseLong(firstPresent(
                normalizedFields,
                "payload_generation",
                "last_payload_generation",
                "direct_lighting_payload_generation"
        ));
        String payloadGenerationRange = firstPresent(
                normalizedFields,
                "payload_generation_range",
                "last_payload_generation_range",
                "direct_lighting_payload_generation_range"
        );
        Long payloadFrameIndex = parseLong(firstPresent(
                normalizedFields,
                "payload_frame",
                "payload_frame_index",
                "last_payload_frame",
                "last_payload_frame_index"
        ));
        Long celestialCount = parseLong(firstPresent(
                normalizedFields,
                "celestial_count",
                "celestial",
                "celestial_lights",
                "celestial_light_count"
        ));
        Long emissiveCount = parseLong(firstPresent(
                normalizedFields,
                "emissive_count",
                "emissive",
                "emissive_lights",
                "emissive_light_count"
        ));
        Long shadowCandidateCount = parseLong(firstPresent(
                normalizedFields,
                "shadow_candidate_count",
                "shadow_candidates",
                "shadow",
                "direct_shadow_candidates"
        ));
        Long budgetedShadowCandidateCount = parseLong(firstPresent(
                normalizedFields,
                "budgeted_shadow_candidate_count",
                "budgeted_shadow_candidates",
                "budgeted_shadow"
        ));
        Long sectionSnapshotCount = parseLong(firstPresent(
                normalizedFields,
                "section_snapshot_count",
                "section_snapshots",
                "sections",
                "section_count"
        ));
        Boolean metadataOnly = parseBoolean(firstPresent(
                normalizedFields,
                "metadata_only",
                "payload_metadata_only"
        ));
        Boolean cpuOutputGenerated = parseBoolean(firstPresent(
                normalizedFields,
                "cpu_output_generated",
                "direct_cpu_output_generated"
        ));
        String outputDimensions = xyLabel(
                firstPresent(normalizedFields, "output_width", "direct_output_width"),
                firstPresent(normalizedFields, "output_height", "direct_output_height")
        );
        Long outputPixelCount = parseLong(firstPresent(
                normalizedFields,
                "output_pixels",
                "output_pixel_count",
                "direct_output_pixels"
        ));
        String outputEnergy = firstPresent(
                normalizedFields,
                "output_energy",
                "direct_output_energy"
        );
        Long outputChecksum = parseLong(firstPresent(
                normalizedFields,
                "output_checksum",
                "direct_output_checksum"
        ));
        Boolean payloadValidated = parseBoolean(firstPresent(
                normalizedFields,
                "payload_validated",
                "direct_payload_validated"
        ));
        Boolean payloadHasDirectWork = parseBoolean(firstPresent(
                normalizedFields,
                "payload_has_direct_work",
                "has_direct_work"
        ));
        Boolean payloadReadyForShadowTracing = parseBoolean(firstPresent(
                normalizedFields,
                "payload_ready_for_shadow_tracing",
                "ready_for_shadow_tracing"
        ));

        return new LightingDispatchStageTelemetryStatus(
                stageId,
                enabled,
                generation,
                dispatchGroups,
                dimensions,
                ioCounts,
                sampleCount,
                candidateCount,
                rayCount,
                cacheReadCount,
                cacheWriteCount,
                flags,
                placeholder,
                validated,
                debugOverlay,
                readyForNativeExecution,
                readinessReason,
                frameIndex,
                recordedThisFrame,
                payloadAccepted,
                payloadGeneration,
                payloadGenerationRange,
                payloadFrameIndex,
                celestialCount,
                emissiveCount,
                shadowCandidateCount,
                budgetedShadowCandidateCount,
                sectionSnapshotCount,
                metadataOnly,
                cpuOutputGenerated,
                outputDimensions,
                outputPixelCount,
                outputEnergy,
                outputChecksum,
                payloadValidated,
                payloadHasDirectWork,
                payloadReadyForShadowTracing,
                normalizedFields
        );
    }

    public String compactLabel() {
        StringBuilder label = new StringBuilder(this.stageId);
        int fieldCount = 0;
        fieldCount += append(label, "enabled", this.enabled == null ? "" : Boolean.toString(this.enabled));
        fieldCount += append(label, "gen", this.generation == null ? "" : Long.toString(this.generation));
        fieldCount += append(label, "groups", this.dispatchGroups);
        fieldCount += append(label, "samples", this.sampleCount == null ? "" : Long.toString(this.sampleCount));
        fieldCount += append(label, "candidates", this.candidateCount == null ? "" : Long.toString(this.candidateCount));
        fieldCount += append(label, "rays", this.rayCount == null ? "" : Long.toString(this.rayCount));
        String cacheLabel = cacheLabel();
        fieldCount += append(label, "cache", cacheLabel);
        fieldCount += append(label, "ready", this.readyForNativeExecution == null ? "" : Boolean.toString(this.readyForNativeExecution));
        fieldCount += append(label, "payload", this.payloadAccepted == null ? "" : Boolean.toString(this.payloadAccepted));
        fieldCount += append(label, "payloadGen", this.payloadGeneration == null ? "" : Long.toString(this.payloadGeneration));
        fieldCount += append(label, "frame", this.frameIndex == null ? "" : Long.toString(this.frameIndex));
        if (fieldCount == 0) {
            label.append(" reported");
        }
        return label.toString();
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "lighting.dispatch.stage." + sanitizeKey(this.stageId));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".present", "true");
        fields.put(normalizedPrefix + ".summary", this.compactLabel());
        if (this.enabled != null) {
            fields.put(normalizedPrefix + ".enabled", Boolean.toString(this.enabled));
        }
        if (this.generation != null) {
            fields.put(normalizedPrefix + ".generation", Long.toString(this.generation));
        }
        if (!this.dispatchGroups.isBlank()) {
            fields.put(normalizedPrefix + ".dispatchGroups", this.dispatchGroups);
        }
        if (!this.dimensions.isBlank()) {
            fields.put(normalizedPrefix + ".dimensions", this.dimensions);
        }
        if (!this.ioCounts.isBlank()) {
            fields.put(normalizedPrefix + ".ioCounts", this.ioCounts);
        }
        if (this.sampleCount != null) {
            fields.put(normalizedPrefix + ".samples", Long.toString(this.sampleCount));
        }
        if (this.candidateCount != null) {
            fields.put(normalizedPrefix + ".candidates", Long.toString(this.candidateCount));
        }
        if (this.rayCount != null) {
            fields.put(normalizedPrefix + ".rays", Long.toString(this.rayCount));
        }
        if (this.cacheReadCount != null || this.cacheWriteCount != null) {
            fields.put(normalizedPrefix + ".cacheCounts", cacheLabel());
        }
        if (this.cacheReadCount != null) {
            fields.put(normalizedPrefix + ".cacheReads", Long.toString(this.cacheReadCount));
        }
        if (this.cacheWriteCount != null) {
            fields.put(normalizedPrefix + ".cacheWrites", Long.toString(this.cacheWriteCount));
        }
        if (this.flags != null) {
            fields.put(normalizedPrefix + ".flags", Long.toString(this.flags));
        }
        if (this.placeholder != null) {
            fields.put(normalizedPrefix + ".placeholder", Boolean.toString(this.placeholder));
        }
        if (this.validated != null) {
            fields.put(normalizedPrefix + ".validated", Boolean.toString(this.validated));
        }
        if (this.debugOverlay != null) {
            fields.put(normalizedPrefix + ".debugOverlay", Boolean.toString(this.debugOverlay));
        }
        if (this.readyForNativeExecution != null) {
            fields.put(normalizedPrefix + ".readyForNativeExecution", Boolean.toString(this.readyForNativeExecution));
        }
        if (!this.readinessReason.isBlank()) {
            fields.put(normalizedPrefix + ".readinessReason", this.readinessReason);
        }
        if (this.frameIndex != null) {
            fields.put(normalizedPrefix + ".frameIndex", Long.toString(this.frameIndex));
        }
        if (this.recordedThisFrame != null) {
            fields.put(normalizedPrefix + ".recordedThisFrame", Boolean.toString(this.recordedThisFrame));
        }
        if (this.payloadAccepted != null) {
            fields.put(normalizedPrefix + ".payloadAccepted", Boolean.toString(this.payloadAccepted));
        }
        if (this.payloadGeneration != null) {
            fields.put(normalizedPrefix + ".payloadGeneration", Long.toString(this.payloadGeneration));
        }
        if (!this.payloadGenerationRange.isBlank()) {
            fields.put(normalizedPrefix + ".payloadGenerationRange", this.payloadGenerationRange);
        }
        if (this.payloadFrameIndex != null) {
            fields.put(normalizedPrefix + ".payloadFrameIndex", Long.toString(this.payloadFrameIndex));
        }
        if (this.celestialCount != null) {
            fields.put(normalizedPrefix + ".celestialCount", Long.toString(this.celestialCount));
        }
        if (this.emissiveCount != null) {
            fields.put(normalizedPrefix + ".emissiveCount", Long.toString(this.emissiveCount));
        }
        if (this.shadowCandidateCount != null) {
            fields.put(normalizedPrefix + ".shadowCandidateCount", Long.toString(this.shadowCandidateCount));
        }
        if (this.budgetedShadowCandidateCount != null) {
            fields.put(normalizedPrefix + ".budgetedShadowCandidateCount", Long.toString(this.budgetedShadowCandidateCount));
        }
        if (this.sectionSnapshotCount != null) {
            fields.put(normalizedPrefix + ".sectionSnapshotCount", Long.toString(this.sectionSnapshotCount));
        }
        if (this.metadataOnly != null) {
            fields.put(normalizedPrefix + ".metadataOnly", Boolean.toString(this.metadataOnly));
        }
        if (this.cpuOutputGenerated != null) {
            fields.put(normalizedPrefix + ".cpuOutputGenerated", Boolean.toString(this.cpuOutputGenerated));
        }
        if (!this.outputDimensions.isBlank()) {
            fields.put(normalizedPrefix + ".outputDimensions", this.outputDimensions);
        }
        if (this.outputPixelCount != null) {
            fields.put(normalizedPrefix + ".outputPixelCount", Long.toString(this.outputPixelCount));
        }
        if (!this.outputEnergy.isBlank()) {
            fields.put(normalizedPrefix + ".outputEnergy", this.outputEnergy);
        }
        if (this.outputChecksum != null) {
            fields.put(normalizedPrefix + ".outputChecksum", Long.toString(this.outputChecksum));
        }
        if (this.payloadValidated != null) {
            fields.put(normalizedPrefix + ".payloadValidated", Boolean.toString(this.payloadValidated));
        }
        if (this.payloadHasDirectWork != null) {
            fields.put(normalizedPrefix + ".payloadHasDirectWork", Boolean.toString(this.payloadHasDirectWork));
        }
        if (this.payloadReadyForShadowTracing != null) {
            fields.put(normalizedPrefix + ".payloadReadyForShadowTracing", Boolean.toString(this.payloadReadyForShadowTracing));
        }
        for (Map.Entry<String, String> entry : this.details.entrySet()) {
            fields.put(normalizedPrefix + ".raw." + sanitizeKey(entry.getKey()), entry.getValue());
        }
        return Collections.unmodifiableMap(fields);
    }

    private String cacheLabel() {
        if (this.cacheReadCount == null && this.cacheWriteCount == null) {
            return "";
        }
        return valueOrUnknown(this.cacheReadCount) + "/" + valueOrUnknown(this.cacheWriteCount);
    }

    private static int append(StringBuilder label, String key, String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        label.append(' ').append(key).append('=').append(value);
        return 1;
    }

    private static String valueOrUnknown(Long value) {
        return value == null ? "?" : Long.toString(value);
    }

    private static Map<String, String> normalizeFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = normalizeFieldKey(entry.getKey());
            String value = stripQuotes(entry.getValue());
            if (!key.isBlank() && !value.isBlank()) {
                normalized.putIfAbsent(key, value);
            }
        }
        return normalized;
    }

    private static String firstPresent(Map<String, String> fields, String... keys) {
        if (fields == null || fields.isEmpty() || keys == null) {
            return "";
        }

        for (String key : keys) {
            String value = fields.get(normalizeFieldKey(key));
            if (value != null && !value.isBlank()) {
                return stripQuotes(value);
            }
        }
        return "";
    }

    private static Boolean parseBoolean(String value) {
        String cleaned = stripQuotes(value).toLowerCase(Locale.ROOT);
        return switch (cleaned) {
            case "1", "true", "yes", "y", "on", "enabled", "active" -> true;
            case "0", "false", "no", "n", "off", "disabled", "inactive" -> false;
            default -> null;
        };
    }

    private static Long parseLong(String value) {
        String cleaned = stripQuotes(value);
        if (cleaned.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long[] parseLongPair(String value) {
        String cleaned = stripQuotes(value);
        if (cleaned.isBlank()) {
            return new Long[]{null, null};
        }

        String[] parts = cleaned.split("[/x:,]");
        if (parts.length < 2) {
            return new Long[]{null, null};
        }
        return new Long[]{parseLong(parts[0]), parseLong(parts[1])};
    }

    private static String xyzLabel(String x, String y, String z) {
        if (x.isBlank() || y.isBlank() || z.isBlank()) {
            return "";
        }
        return x + "x" + y + "x" + z;
    }

    private static String xyLabel(String x, String y) {
        if (x.isBlank() || y.isBlank()) {
            return "";
        }
        return x + "x" + y;
    }

    private static String pairLabel(String first, String second) {
        if (first.isBlank() || second.isBlank()) {
            return "";
        }
        return first + "/" + second;
    }

    private static Map<String, String> immutable(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String cleanStageId(String value) {
        String cleaned = stripQuotes(value).toLowerCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return "unknown";
        }
        return cleaned.replace('-', '_').replace(' ', '_');
    }

    private static String normalizeFieldKey(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    }

    private static String sanitizeKey(String value) {
        String cleaned = clean(value, "unknown").toLowerCase(Locale.ROOT);
        StringBuilder sanitized = new StringBuilder(cleaned.length());
        for (int index = 0; index < cleaned.length(); index++) {
            char character = cleaned.charAt(index);
            if ((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9')) {
                sanitized.append(character);
            } else {
                sanitized.append('.');
            }
        }
        return sanitized.toString().replaceAll("\\.+", ".");
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String blankToEmpty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
