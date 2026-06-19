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
        Long rayCount,
        Long cacheReadCount,
        Long cacheWriteCount,
        Map<String, String> details
) {
    public LightingDispatchStageTelemetryStatus {
        stageId = cleanStageId(stageId);
        dispatchGroups = blankToEmpty(stripQuotes(dispatchGroups));
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

        return new LightingDispatchStageTelemetryStatus(
                stageId,
                enabled,
                generation,
                dispatchGroups,
                rayCount,
                cacheReadCount,
                cacheWriteCount,
                normalizedFields
        );
    }

    public String compactLabel() {
        StringBuilder label = new StringBuilder(this.stageId);
        int fieldCount = 0;
        fieldCount += append(label, "enabled", this.enabled == null ? "" : Boolean.toString(this.enabled));
        fieldCount += append(label, "gen", this.generation == null ? "" : Long.toString(this.generation));
        fieldCount += append(label, "groups", this.dispatchGroups);
        fieldCount += append(label, "rays", this.rayCount == null ? "" : Long.toString(this.rayCount));
        String cacheLabel = cacheLabel();
        fieldCount += append(label, "cache", cacheLabel);
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
