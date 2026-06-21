package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record NativePassTelemetryStatus(
        boolean statusAvailable,
        Map<String, String> passStates,
        Map<String, String> passDetails,
        String message
) {
    private static final int MAX_COMPACT_PASS_STATES = 4;
    private static final Pattern PASS_BLOCK_PATTERN = Pattern.compile("\\{([^}]*)}");
    private static final Pattern PASS_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b([a-z0-9_.-]*(?:pass|gbuffer|g-buffer|depth|shadow|voxel|ray|trac|lighting|composite|denoise)[a-z0-9_.-]*)\\s*[:=]\\s*([^,;|}\\]]+)"
    );

    public NativePassTelemetryStatus {
        passStates = immutable(passStates);
        passDetails = immutable(passDetails);
        message = clean(message, "Native pass states have not been reported.");
        statusAvailable = statusAvailable && !passStates.isEmpty();
    }

    public static NativePassTelemetryStatus unavailable(String message) {
        return new NativePassTelemetryStatus(false, Map.of(), Map.of(), message);
    }

    public static NativePassTelemetryStatus fromNativeStatus(String nativeStatus) {
        String cleanedStatus = clean(nativeStatus, "");
        if (cleanedStatus.isBlank()) {
            return unavailable("Native status string is blank.");
        }

        Map<String, String> passStates = new LinkedHashMap<>();
        Map<String, String> passDetails = new LinkedHashMap<>();
        parsePassBlocks(cleanedStatus, passStates, passDetails);
        if (passStates.isEmpty()) {
            parseLoosePassStatePairs(cleanedStatus, passStates);
        }

        if (passStates.isEmpty()) {
            return unavailable("Native pass states are not present in native status.");
        }

        return new NativePassTelemetryStatus(
                true,
                passStates,
                passDetails,
                "Native pass states reported by native status."
        );
    }

    public boolean hasPassStates() {
        return this.statusAvailable && !this.passStates.isEmpty();
    }

    public String stateFor(String passId) {
        if (passId == null || passId.isBlank()) {
            return "";
        }
        return this.passStates.getOrDefault(passId.trim(), "");
    }

    public String compactLabel() {
        if (!this.hasPassStates()) {
            return this.message;
        }

        StringBuilder label = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, String> entry : this.passStates.entrySet()) {
            if (index >= MAX_COMPACT_PASS_STATES) {
                label.append(", +").append(this.passStates.size() - index).append(" more");
                break;
            }
            if (index > 0) {
                label.append(", ");
            }
            label.append(entry.getKey()).append("=").append(entry.getValue());
            index++;
        }
        return label.toString();
    }

    public String compactTimingBoundaryLabel() {
        if (!this.hasPassStates()) {
            return "native pass timing unavailable(" + this.message + ")";
        }

        boolean cpuTimingReported = hasAnyDetailFragment("cpu_ms", "cpu_millis", "cpu_time_ms", "native_cpu_ms");
        boolean gpuTimingReported = hasAnyDetailFragment("gpu_ms", "gpu_millis", "gpu_time_ms", "native_gpu_ms");
        String cpu = cpuTimingReported ? "CPU=reported" : "CPU=pending";
        String gpu = gpuTimingReported ? "GPU=reported(real timestamps)" : "GPU=unavailable/pending";
        return cpu + " | " + gpu + " | states=" + this.passStates.size();
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "native.pass");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".available", Boolean.toString(this.hasPassStates()));
        fields.put(normalizedPrefix + ".summary", this.compactLabel());
        fields.put(normalizedPrefix + ".timingBoundary", this.compactTimingBoundaryLabel());
        for (Map.Entry<String, String> entry : this.passStates.entrySet()) {
            fields.put(normalizedPrefix + "." + sanitizeKey(entry.getKey()) + ".state", entry.getValue());
        }
        for (Map.Entry<String, String> entry : this.passDetails.entrySet()) {
            fields.put(normalizedPrefix + "." + sanitizeKey(entry.getKey()), entry.getValue());
        }
        return Collections.unmodifiableMap(fields);
    }

    private boolean hasAnyDetailFragment(String... fragments) {
        if (fragments == null || this.passDetails.isEmpty()) {
            return false;
        }
        for (String key : this.passDetails.keySet()) {
            String normalizedKey = key.toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
            for (String fragment : fragments) {
                if (normalizedKey.contains(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void parsePassBlocks(
            String nativeStatus,
            Map<String, String> passStates,
            Map<String, String> passDetails
    ) {
        int passesStart = nativeStatus.indexOf("passes=[");
        if (passesStart < 0) {
            return;
        }

        int blockStart = passesStart + "passes=[".length();
        int blockEnd = nativeStatus.indexOf(']', blockStart);
        if (blockEnd <= blockStart) {
            return;
        }

        Matcher matcher = PASS_BLOCK_PATTERN.matcher(nativeStatus.substring(blockStart, blockEnd));
        while (matcher.find()) {
            Map<String, String> blockFields = parseDelimitedFields(matcher.group(1));
            String passId = blockFields.get("id");
            String state = blockFields.get("state");
            if (passId == null || passId.isBlank() || state == null || state.isBlank()) {
                continue;
            }

            passStates.putIfAbsent(passId, state);
            for (Map.Entry<String, String> entry : blockFields.entrySet()) {
                if ("id".equals(entry.getKey()) || "state".equals(entry.getKey())) {
                    continue;
                }
                passDetails.putIfAbsent(passId + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    private static Map<String, String> parseDelimitedFields(String block) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String segment : block.split(",")) {
            int delimiter = delimiterIndex(segment);
            if (delimiter <= 0) {
                continue;
            }

            String key = segment.substring(0, delimiter).trim();
            String value = segment.substring(delimiter + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                fields.putIfAbsent(key, value);
            }
        }
        return fields;
    }

    private static void parseLoosePassStatePairs(String nativeStatus, Map<String, String> passStates) {
        Matcher matcher = PASS_KEY_VALUE_PATTERN.matcher(nativeStatus);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            if (!key.isBlank() && isLikelyPassState(value)) {
                passStates.putIfAbsent(key, value);
            }
        }
    }

    private static boolean isLikelyPassState(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "inactive",
                    "waiting_for_frame",
                    "waiting_for_context",
                    "ready",
                    "submitted",
                    "skipped",
                    "skipped_invalid_order",
                    "skipped_no_context",
                    "not_wired",
                    "active",
                    "idle",
                    "enabled",
                    "disabled",
                    "unavailable" -> true;
            default -> false;
        };
    }

    private static int delimiterIndex(String segment) {
        int equalsIndex = segment.indexOf('=');
        int colonIndex = segment.indexOf(':');
        if (equalsIndex < 0) {
            return colonIndex;
        }
        if (colonIndex < 0) {
            return equalsIndex;
        }
        return Math.min(equalsIndex, colonIndex);
    }

    private static Map<String, String> immutable(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
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

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
