package net.lucerna.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record LightingDispatchTelemetryStatus(
        boolean statusAvailable,
        Long generation,
        Integer advertisedDispatches,
        Integer payloadDispatches,
        Integer enabledDispatches,
        Integer disabledDispatches,
        String generationRange,
        Map<String, LightingDispatchStageTelemetryStatus> stages,
        String message
) {
    private static final Pattern LOOSE_STAGE_FIELD_PATTERN = Pattern.compile(
            "(?i)\\b(?:lighting[._-](?:dispatch[._-])?stage[._-])?"
                    + "([a-z0-9_-]+)[._-]"
                    + "(enabled|enabled_this_packet|active|generation|last_generation|dispatch_generation|"
                    + "stage_generation|dispatch|last_dispatch|dispatch_groups|groups|rays|last_rays|ray_count|"
                    + "cache|last_cache|cache_counts|cache_reads|cache_writes|cache_read_count|cache_write_count)"
                    + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;}\\]]+)"
    );

    public LightingDispatchTelemetryStatus {
        generationRange = blankToEmpty(stripQuotes(generationRange));
        stages = immutableStages(stages);
        message = clean(message, "Lighting dispatch status has not been reported.");
        statusAvailable = statusAvailable && hasAnyStatus(
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages
        );
    }

    public static LightingDispatchTelemetryStatus unavailable(String message) {
        return new LightingDispatchTelemetryStatus(
                false,
                null,
                null,
                null,
                null,
                null,
                "",
                Map.of(),
                message
        );
    }

    public static LightingDispatchTelemetryStatus fromNativeStatus(String nativeStatus) {
        String cleanedStatus = clean(nativeStatus, "");
        if (cleanedStatus.isBlank()) {
            return unavailable("Native status string is blank.");
        }

        Map<String, String> lightingFields = extractLightingAggregateFields(cleanedStatus);
        Map<String, LightingDispatchStageTelemetryStatus> stages = parseStages(cleanedStatus);
        Long generation = firstLong(
                findLooseValue(cleanedStatus, "lighting_dispatch_generation"),
                firstPresent(lightingFields, "last_packet_generation", "packet_generation", "generation")
        );
        Integer advertisedDispatches = firstInt(
                findLooseValue(cleanedStatus, "lighting_dispatches"),
                firstPresent(lightingFields, "advertised_dispatches", "dispatches", "advertised")
        );
        Integer payloadDispatches = firstInt(
                findLooseValue(cleanedStatus, "lighting_dispatch_payloads"),
                firstPresent(lightingFields, "payload_dispatches", "payloads", "payload")
        );
        Integer enabledDispatches = firstInt(firstPresent(lightingFields, "enabled_dispatches", "enabled"));
        Integer disabledDispatches = firstInt(firstPresent(lightingFields, "disabled_dispatches", "disabled"));
        String generationRange = firstNonBlank(
                findLooseValue(cleanedStatus, "lighting_dispatch_generation_range"),
                firstPresent(lightingFields, "last_generation_range", "generation_range", "range")
        );

        if (!hasAnyStatus(
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages
        )) {
            return unavailable("Lighting dispatch fields are not present in native status.");
        }

        return new LightingDispatchTelemetryStatus(
                true,
                generation,
                advertisedDispatches,
                payloadDispatches,
                enabledDispatches,
                disabledDispatches,
                generationRange,
                stages,
                "Lighting dispatch status reported by native status."
        );
    }

    public boolean hasLightingDispatchStatus() {
        return this.statusAvailable;
    }

    public boolean hasStageStatuses() {
        return this.statusAvailable && !this.stages.isEmpty();
    }

    public String compactLabel() {
        if (!this.hasLightingDispatchStatus()) {
            return this.message;
        }

        StringBuilder label = new StringBuilder();
        append(label, "gen", this.generation == null ? "" : Long.toString(this.generation));
        append(label, "range", this.generationRange);
        append(label, "dispatches", this.advertisedDispatches == null ? "" : Integer.toString(this.advertisedDispatches));
        append(label, "payloads", this.payloadDispatches == null ? "" : Integer.toString(this.payloadDispatches));
        if (this.enabledDispatches != null || this.disabledDispatches != null) {
            append(label, "enabled", valueOrUnknown(this.enabledDispatches) + "/" + valueOrUnknown(this.disabledDispatches));
        }
        append(label, "stages", Integer.toString(this.stages.size()));
        return label.length() == 0 ? this.message : label.toString();
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "lighting.dispatch");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".available", Boolean.toString(this.hasLightingDispatchStatus()));
        fields.put(normalizedPrefix + ".summary", this.compactLabel());
        if (this.generation != null) {
            fields.put(normalizedPrefix + ".generation", Long.toString(this.generation));
        }
        if (!this.generationRange.isBlank()) {
            fields.put(normalizedPrefix + ".generationRange", this.generationRange);
        }
        if (this.advertisedDispatches != null) {
            fields.put(normalizedPrefix + ".advertisedDispatches", Integer.toString(this.advertisedDispatches));
        }
        if (this.payloadDispatches != null) {
            fields.put(normalizedPrefix + ".payloadDispatches", Integer.toString(this.payloadDispatches));
        }
        if (this.enabledDispatches != null) {
            fields.put(normalizedPrefix + ".enabledDispatches", Integer.toString(this.enabledDispatches));
        }
        if (this.disabledDispatches != null) {
            fields.put(normalizedPrefix + ".disabledDispatches", Integer.toString(this.disabledDispatches));
        }
        fields.put(normalizedPrefix + ".stageCount", Integer.toString(this.stages.size()));
        for (LightingDispatchStageTelemetryStatus stage : this.stages.values()) {
            fields.putAll(stage.validationFields(normalizedPrefix + ".stage." + sanitizeKey(stage.stageId())));
        }
        return Collections.unmodifiableMap(fields);
    }

    private static Map<String, LightingDispatchStageTelemetryStatus> parseStages(String nativeStatus) {
        Map<String, Map<String, String>> stageFields = new LinkedHashMap<>();
        parseStageBlocks(nativeStatus, stageFields);
        parseLooseStageFields(nativeStatus, stageFields);

        if (stageFields.isEmpty()) {
            return Map.of();
        }

        Map<String, LightingDispatchStageTelemetryStatus> stages = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : stageFields.entrySet()) {
            LightingDispatchStageTelemetryStatus status = LightingDispatchStageTelemetryStatus.fromFields(entry.getValue());
            stages.put(status.stageId(), status);
        }
        return stages;
    }

    private static void parseStageBlocks(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        String stageList = extractStageList(nativeStatus);
        if (stageList.isBlank()) {
            return;
        }

        for (String block : extractBraceBlocks(stageList)) {
            Map<String, String> fields = parseDelimitedFields(block);
            String stageId = normalizeStageId(firstPresent(fields, "id", "stage", "stage_id", "stage_name", "name"));
            if (stageId.isBlank()) {
                continue;
            }

            Map<String, String> target = stageFields.computeIfAbsent(stageId, ignored -> new LinkedHashMap<>());
            target.putIfAbsent("id", stageId);
            putMissing(target, fields);
        }
    }

    private static void parseLooseStageFields(String nativeStatus, Map<String, Map<String, String>> stageFields) {
        Matcher matcher = LOOSE_STAGE_FIELD_PATTERN.matcher(nativeStatus);
        while (matcher.find()) {
            String stageId = normalizeStageId(matcher.group(1));
            if (!isKnownStageId(stageId) && !stageFields.containsKey(stageId)) {
                continue;
            }

            String fieldKey = normalizeLooseStageFieldKey(matcher.group(2));
            String fieldValue = stripQuotes(matcher.group(3));
            if (fieldKey.isBlank() || fieldValue.isBlank()) {
                continue;
            }

            Map<String, String> target = stageFields.computeIfAbsent(stageId, ignored -> new LinkedHashMap<>());
            target.putIfAbsent("id", stageId);
            target.putIfAbsent(fieldKey, fieldValue);
        }
    }

    private static Map<String, String> extractLightingAggregateFields(String nativeStatus) {
        int lightingStart = nativeStatus.indexOf("lighting={");
        if (lightingStart < 0) {
            return Map.of();
        }

        int contentStart = lightingStart + "lighting={".length();
        int stagesStart = nativeStatus.indexOf("stages=[", contentStart);
        int contentEnd = stagesStart >= 0 ? stagesStart : findMatching(nativeStatus, contentStart - 1, '{', '}');
        if (contentEnd <= contentStart) {
            return Map.of();
        }

        String content = nativeStatus.substring(contentStart, trimTrailingDelimiter(nativeStatus, contentStart, contentEnd));
        return parseDelimitedFields(content);
    }

    private static String extractStageList(String nativeStatus) {
        String stageList = extractBracketContentAfter(nativeStatus, "lighting={", "stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        stageList = extractBracketContent(nativeStatus, "lighting_dispatch_stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        stageList = extractBracketContent(nativeStatus, "lighting_stages=[");
        if (!stageList.isBlank()) {
            return stageList;
        }

        return extractBracketContent(nativeStatus, "stages=[");
    }

    private static String extractBracketContentAfter(String source, String sectionMarker, String listMarker) {
        int sectionStart = source.indexOf(sectionMarker);
        if (sectionStart < 0) {
            return "";
        }

        int listStart = source.indexOf(listMarker, sectionStart + sectionMarker.length());
        if (listStart < 0) {
            return "";
        }
        return extractBracketContentAt(source, listStart + listMarker.length() - 1);
    }

    private static String extractBracketContent(String source, String marker) {
        int markerStart = source.indexOf(marker);
        if (markerStart < 0) {
            return "";
        }
        return extractBracketContentAt(source, markerStart + marker.length() - 1);
    }

    private static String extractBracketContentAt(String source, int bracketIndex) {
        int end = findMatching(source, bracketIndex, '[', ']');
        if (end <= bracketIndex) {
            return "";
        }
        return source.substring(bracketIndex + 1, end);
    }

    private static List<String> extractBraceBlocks(String source) {
        List<String> blocks = new ArrayList<>();
        int blockStart = -1;
        int depth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == '{') {
                if (depth == 0) {
                    blockStart = index + 1;
                }
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0 && blockStart >= 0) {
                    blocks.add(source.substring(blockStart, index));
                    blockStart = -1;
                }
            }
        }
        return blocks;
    }

    private static Map<String, String> parseDelimitedFields(String block) {
        if (block == null || block.isBlank()) {
            return Map.of();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (String segment : splitTopLevel(block, ',')) {
            int delimiter = delimiterIndex(segment);
            if (delimiter <= 0) {
                continue;
            }

            String key = normalizeFieldKey(segment.substring(0, delimiter));
            String value = stripQuotes(segment.substring(delimiter + 1));
            if (!key.isBlank() && !value.isBlank()) {
                fields.putIfAbsent(key, value);
            }
        }
        return fields;
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> segments = new ArrayList<>();
        StringBuilder segment = new StringBuilder();
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quoted) {
                segment.append(character);
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                segment.append(character);
                continue;
            }
            if (character == '{') {
                braceDepth++;
            } else if (character == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (character == '[') {
                bracketDepth++;
            } else if (character == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }

            if (character == delimiter && braceDepth == 0 && bracketDepth == 0) {
                segments.add(segment.toString());
                segment.setLength(0);
            } else {
                segment.append(character);
            }
        }

        segments.add(segment.toString());
        return segments;
    }

    private static int delimiterIndex(String segment) {
        int braceDepth = 0;
        int bracketDepth = 0;
        boolean quoted = false;
        char quote = 0;

        for (int index = 0; index < segment.length(); index++) {
            char character = segment.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == '{') {
                braceDepth++;
            } else if (character == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
            } else if (character == '[') {
                bracketDepth++;
            } else if (character == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            } else if ((character == '=' || character == ':') && braceDepth == 0 && bracketDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static int findMatching(String source, int openIndex, char open, char close) {
        if (openIndex < 0 || openIndex >= source.length() || source.charAt(openIndex) != open) {
            return -1;
        }

        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        for (int index = openIndex; index < source.length(); index++) {
            char character = source.charAt(index);
            if (quoted) {
                if (character == quote) {
                    quoted = false;
                }
                continue;
            }
            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
                continue;
            }
            if (character == open) {
                depth++;
            } else if (character == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int trimTrailingDelimiter(String source, int start, int end) {
        int trimmedEnd = end;
        while (trimmedEnd > start) {
            char character = source.charAt(trimmedEnd - 1);
            if (character == ',' || Character.isWhitespace(character)) {
                trimmedEnd--;
            } else {
                break;
            }
        }
        return trimmedEnd;
    }

    private static String findLooseValue(String nativeStatus, String key) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:^|[\\s,{])" + Pattern.quote(key) + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;}\\]]+)"
        );
        Matcher matcher = pattern.matcher(nativeStatus);
        if (!matcher.find()) {
            return "";
        }
        return stripQuotes(matcher.group(1));
    }

    private static void putMissing(Map<String, String> target, Map<String, String> source) {
        for (Map.Entry<String, String> entry : source.entrySet()) {
            target.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static Long firstLong(String... values) {
        for (String value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Integer firstInt(String... values) {
        for (String value : values) {
            Long parsed = parseLong(value);
            if (parsed == null) {
                continue;
            }
            try {
                return Math.toIntExact(parsed);
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
        return null;
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

    private static boolean hasAnyStatus(
            Long generation,
            Integer advertisedDispatches,
            Integer payloadDispatches,
            Integer enabledDispatches,
            Integer disabledDispatches,
            String generationRange,
            Map<String, LightingDispatchStageTelemetryStatus> stages
    ) {
        return generation != null
                || advertisedDispatches != null
                || payloadDispatches != null
                || enabledDispatches != null
                || disabledDispatches != null
                || (generationRange != null && !generationRange.isBlank())
                || (stages != null && !stages.isEmpty());
    }

    private static void append(StringBuilder label, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (label.length() > 0) {
            label.append(' ');
        }
        label.append(key).append('=').append(value);
    }

    private static String valueOrUnknown(Integer value) {
        return value == null ? "?" : Integer.toString(value);
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

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String cleaned = stripQuotes(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private static boolean isKnownStageId(String stageId) {
        return switch (stageId) {
            case "direct_lighting", "diffuse_gi", "denoise", "composite", "cache" -> true;
            default -> false;
        };
    }

    private static String normalizeLooseStageFieldKey(String value) {
        return switch (normalizeFieldKey(value)) {
            case "active" -> "enabled";
            case "generation", "dispatch_generation", "stage_generation" -> "last_generation";
            case "dispatch", "dispatch_groups", "groups" -> "last_dispatch";
            case "rays", "ray_count" -> "last_rays";
            case "cache", "cache_counts" -> "last_cache";
            case "cache_reads", "cache_read_count" -> "cache_reads";
            case "cache_writes", "cache_write_count" -> "cache_writes";
            default -> normalizeFieldKey(value);
        };
    }

    private static String normalizeStageId(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalizeFieldKey(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    }

    private static Map<String, LightingDispatchStageTelemetryStatus> immutableStages(
            Map<String, LightingDispatchStageTelemetryStatus> source
    ) {
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
