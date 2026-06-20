package net.lucerna.render.lighting.restir.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Round11RestirExecutionStatus(
        boolean statusPresent,
        Boolean realRestirDiExecution,
        Long directReservoirCount,
        Long restirDiCandidateCount,
        Long restirDiSelectedCount,
        String restirDiCandidateReductionRatio,
        Long restirDiTemporalReuseCount,
        Long restirDiSpatialReuseCount,
        String restirDiOutputEnergy,
        String restirDiOutputChecksum,
        Boolean realRestirGiReuseExecution,
        Long giReservoirCount,
        Long giCandidateCount,
        Long giPathReuseCount,
        Long invalidatedReservoirCount,
        Round11RestirConfidenceStats confidenceStats,
        Map<String, String> fields
) {
    private static final Pattern LOOSE_FIELD_PATTERN = Pattern.compile(
            "([A-Za-z0-9_.-]+)=((?:\"[^\"]*\")|(?:'[^']*')|[^\\s,}]+)"
    );

    public Round11RestirExecutionStatus {
        restirDiCandidateReductionRatio = clean(restirDiCandidateReductionRatio);
        restirDiOutputEnergy = clean(restirDiOutputEnergy);
        restirDiOutputChecksum = clean(restirDiOutputChecksum);
        confidenceStats = confidenceStats == null ? Round11RestirConfidenceStats.unavailable() : confidenceStats;
        fields = immutable(fields);
    }

    public static Round11RestirExecutionStatus unavailable(String reason) {
        Map<String, String> fields = new LinkedHashMap<>();
        String cleanedReason = clean(reason);
        if (!cleanedReason.isBlank()) {
            fields.put("reason", cleanedReason);
        }
        return fromFields(false, fields);
    }

    public static Round11RestirExecutionStatus fromNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return unavailable("native status string is blank");
        }

        Map<String, String> fields = parseFields(nativeStatus);
        return fromFields(!fields.isEmpty(), fields);
    }

    public String compactDirectExecutionLine() {
        return "Round 11 ReSTIR DI: realExecution=" + booleanOrUnknown(this.realRestirDiExecution)
                + " reservoirs=" + valueOrUnknown(this.directReservoirCount)
                + " candidates=" + valueOrUnknown(this.restirDiCandidateCount)
                + " selected=" + valueOrUnknown(this.restirDiSelectedCount)
                + " temporalReuse=" + valueOrUnknown(this.restirDiTemporalReuseCount)
                + " spatialReuse=" + valueOrUnknown(this.restirDiSpatialReuseCount)
                + " outputEnergy=" + valueOrUnknown(this.restirDiOutputEnergy)
                + " checksum=" + valueOrUnknown(this.restirDiOutputChecksum);
    }

    public String compactGiReuseLine() {
        return "Round 11 ReSTIR GI reuse: realExecution=" + booleanOrUnknown(this.realRestirGiReuseExecution)
                + " reservoirs=" + valueOrUnknown(this.giReservoirCount)
                + " candidates=" + valueOrUnknown(this.giCandidateCount)
                + " pathReuse=" + valueOrUnknown(this.giPathReuseCount)
                + " invalidated=" + valueOrUnknown(this.invalidatedReservoirCount)
                + " confidence=" + this.confidenceStats.compactLine();
    }

    public String compactReductionLine() {
        return "Round 11 ReSTIR reduction: candidates=" + valueOrUnknown(this.restirDiCandidateCount)
                + " selected=" + valueOrUnknown(this.restirDiSelectedCount)
                + " ratio=" + valueOrUnknown(this.restirDiCandidateReductionRatio)
                + " reduced=" + yesNo(this.hasCandidateReductionEvidence());
    }

    public String compactStabilityBoundaryLine() {
        return "Round 11 stability boundary: temporalReuse=" + valueOrUnknown(this.restirDiTemporalReuseCount)
                + " spatialReuse=" + valueOrUnknown(this.restirDiSpatialReuseCount)
                + " giPathReuse=" + valueOrUnknown(this.giPathReuseCount)
                + " invalidated=" + valueOrUnknown(this.invalidatedReservoirCount)
                + " confidence=" + this.confidenceStats.compactLine()
                + "; temporal/flicker stability proof remains controller-owned";
    }

    public String compactQualityBoundaryLine() {
        String executionLabel = hasAnyRealExecution()
                ? "native execution evidence reported"
                : "metadata/status evidence only";
        return "Round 11 quality boundary: " + executionLabel
                + "; no cheaper many-light, ReSTIR DI/PT quality, or flicker improvement claim without controller proof";
    }

    public boolean hasAnyRealExecution() {
        return Boolean.TRUE.equals(this.realRestirDiExecution) || Boolean.TRUE.equals(this.realRestirGiReuseExecution);
    }

    public boolean hasCandidateReductionEvidence() {
        if (this.restirDiCandidateReductionRatio != null && !this.restirDiCandidateReductionRatio.isBlank()) {
            try {
                double ratio = Double.parseDouble(this.restirDiCandidateReductionRatio);
                return Double.isFinite(ratio) && ratio < 0.999D;
            } catch (NumberFormatException ignored) {
                return !"1".equals(this.restirDiCandidateReductionRatio);
            }
        }
        if (this.restirDiCandidateCount == null || this.restirDiSelectedCount == null) {
            return false;
        }
        return this.restirDiCandidateCount > this.restirDiSelectedCount;
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix);
        if (normalizedPrefix.isBlank()) {
            normalizedPrefix = "round11.restir.execution";
        }

        Map<String, String> validationFields = new LinkedHashMap<>();
        validationFields.put(normalizedPrefix + ".present", Boolean.toString(this.statusPresent));
        validationFields.put(normalizedPrefix + ".directExecution", this.compactDirectExecutionLine());
        validationFields.put(normalizedPrefix + ".giReuse", this.compactGiReuseLine());
        validationFields.put(normalizedPrefix + ".reduction", this.compactReductionLine());
        validationFields.put(normalizedPrefix + ".stabilityBoundary", this.compactStabilityBoundaryLine());
        validationFields.put(normalizedPrefix + ".qualityBoundary", this.compactQualityBoundaryLine());
        for (Map.Entry<String, String> entry : this.fields.entrySet()) {
            validationFields.put(normalizedPrefix + ".raw." + entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(validationFields);
    }

    private static Round11RestirExecutionStatus fromFields(boolean statusPresent, Map<String, String> fields) {
        Round11RestirConfidenceStats confidenceStats = new Round11RestirConfidenceStats(
                first(fields, "restir_di_min_confidence", "gi_min_confidence", "min_confidence", "confidence_min", "reservoir_confidence_min"),
                first(fields, "restir_di_mean_confidence", "gi_mean_confidence", "mean_confidence", "avg_confidence", "average_confidence", "confidence", "combined_confidence"),
                first(fields, "restir_di_max_confidence", "gi_max_confidence", "max_confidence", "confidence_max", "reservoir_confidence_max")
        );

        return new Round11RestirExecutionStatus(
                statusPresent,
                parseBoolean(first(
                        fields,
                        "realRestirDiExecution",
                        "real_restir_di_execution",
                        "restir_di_real_execution",
                        "restir_di_execution",
                        "realRestirExecution",
                        "real_restir_execution"
                )),
                parseLong(first(fields, "direct_reservoir_count", "restir_di_reservoir_count", "reservoir_count", "direct_reservoirs")),
                parseLong(first(fields, "restir_di_candidate_count", "direct_candidate_count", "candidate_count", "candidates")),
                parseLong(first(fields, "restir_di_selected_count", "selected_light_count", "selected_count", "selected_candidates")),
                first(fields, "restir_di_candidate_reduction_ratio", "candidate_reduction_ratio", "reduction_ratio"),
                parseLong(first(fields, "restir_di_temporal_reuse_count", "temporal_reuse_count", "temporalReuseCount")),
                parseLong(first(fields, "restir_di_spatial_reuse_count", "spatial_reuse_count", "spatialReuseCount")),
                first(fields, "restir_di_output_energy", "direct_output_energy", "output_energy"),
                first(fields, "restir_di_output_checksum", "direct_output_checksum", "output_checksum"),
                parseBoolean(first(
                        fields,
                        "realRestirGiReuseExecution",
                        "real_restir_gi_reuse_execution",
                        "restir_gi_real_reuse_execution",
                        "gi_reuse_real_execution",
                        "real_gi_reuse_execution"
                )),
                parseLong(first(fields, "gi_reservoir_count", "path_reservoir_count", "gi_reservoirs")),
                parseLong(first(fields, "gi_candidate_count", "gi_path_candidate_count", "path_candidate_count")),
                parseLong(first(fields, "gi_path_reuse_count", "path_reuse_count", "gi_reuse_count", "pathReuseCount")),
                parseLong(first(fields, "invalidated_reservoir_count", "invalidated_reservoirs", "invalidation_count", "invalidated_count")),
                confidenceStats,
                fields
        );
    }

    private static Map<String, String> parseFields(String nativeStatus) {
        Map<String, String> fields = new LinkedHashMap<>();
        parseNativeBlock(nativeStatus, "round11_restir={", fields);
        parseNativeBlock(nativeStatus, "round11.restir={", fields);
        if (containsRound11RestirEvidence(nativeStatus)) {
            parseLooseFields(nativeStatus, fields);
        }
        return fields;
    }

    private static boolean containsRound11RestirEvidence(String nativeStatus) {
        String normalized = normalizeKey(nativeStatus);
        return normalized.contains("round11") || normalized.contains("restir");
    }

    private static void parseNativeBlock(String nativeStatus, String marker, Map<String, String> fields) {
        int start = nativeStatus.indexOf(marker);
        if (start < 0) {
            return;
        }
        int blockStart = start + marker.length();
        int blockEnd = nativeStatus.indexOf('}', blockStart);
        if (blockEnd <= blockStart) {
            return;
        }

        String block = nativeStatus.substring(blockStart, blockEnd);
        for (String segment : block.split(",")) {
            int delimiter = segment.indexOf('=');
            if (delimiter <= 0) {
                continue;
            }
            putField(fields, segment.substring(0, delimiter), segment.substring(delimiter + 1));
        }
    }

    private static void parseLooseFields(String nativeStatus, Map<String, String> fields) {
        Matcher matcher = LOOSE_FIELD_PATTERN.matcher(nativeStatus);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            if (value.startsWith("{")) {
                continue;
            }
            putField(fields, key, value);
        }
    }

    private static void putField(Map<String, String> fields, String key, String value) {
        String normalizedKey = normalizeKey(key);
        String cleanedValue = clean(value);
        if (!normalizedKey.isBlank() && !cleanedValue.isBlank()) {
            fields.putIfAbsent(normalizedKey, cleanedValue);
        }
    }

    private static String first(Map<String, String> fields, String... keys) {
        if (fields == null || fields.isEmpty() || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = fields.get(normalizeKey(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Boolean parseBoolean(String value) {
        String cleaned = normalizeValue(value);
        return switch (cleaned) {
            case "1", "true", "yes", "y", "on", "enabled", "active", "real" -> true;
            case "0", "false", "no", "n", "off", "disabled", "inactive", "metadataonly", "metadata" -> false;
            default -> null;
        };
    }

    private static Long parseLong(String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Map<String, String> immutable(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(value.length());
        String cleaned = Round11RestirConfidenceStats.stripQuotes(value).toLowerCase(Locale.ROOT);
        for (int index = 0; index < cleaned.length(); index++) {
            char character = cleaned.charAt(index);
            if ((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9')) {
                normalized.append(character);
            }
        }
        return normalized.toString();
    }

    private static String normalizeValue(String value) {
        return clean(value).toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private static String clean(String value) {
        String cleaned = Round11RestirConfidenceStats.stripQuotes(value);
        while (cleaned.endsWith(".") || cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String booleanOrUnknown(Boolean value) {
        return value == null ? "?" : Boolean.toString(value);
    }

    private static String valueOrUnknown(Long value) {
        return value == null ? "?" : Long.toString(value);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
