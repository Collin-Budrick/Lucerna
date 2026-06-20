package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FrameTimingTelemetryStatus(
        Map<String, Double> cpuScopeDurationsMillis,
        Map<String, Double> gpuScopeDurationsMillis,
        List<String> activeCpuScopeNames,
        int activeCpuScopeCount
) {
    public FrameTimingTelemetryStatus {
        cpuScopeDurationsMillis = immutableMap(cpuScopeDurationsMillis);
        gpuScopeDurationsMillis = immutableMap(gpuScopeDurationsMillis);
        activeCpuScopeNames = activeCpuScopeNames == null ? List.of() : List.copyOf(activeCpuScopeNames);
        activeCpuScopeCount = Math.max(Math.max(0, activeCpuScopeCount), activeCpuScopeNames.size());
    }

    public static FrameTimingTelemetryStatus from(LucernaTelemetry telemetry) {
        if (telemetry == null) {
            return empty();
        }
        return new FrameTimingTelemetryStatus(
                telemetry.cpuScopeDurationsMillis(),
                telemetry.gpuScopeDurationsMillis(),
                telemetry.activeCpuScopeNames(),
                telemetry.activeCpuScopeCount()
        );
    }

    public static FrameTimingTelemetryStatus empty() {
        return new FrameTimingTelemetryStatus(Map.of(), Map.of(), List.of(), 0);
    }

    public boolean hasCpuTimings() {
        return !this.cpuScopeDurationsMillis.isEmpty();
    }

    public boolean hasGpuTimings() {
        return !this.gpuScopeDurationsMillis.isEmpty();
    }

    public boolean hasAnyTimings() {
        return this.hasCpuTimings() || this.hasGpuTimings();
    }

    public String cpuTimingAvailabilityLabel() {
        if (this.hasCpuTimings()) {
            return "reported(" + this.cpuScopeDurationsMillis.size() + " scopes)";
        }
        if (this.activeCpuScopeCount > 0) {
            return "pending(" + this.activeCpuScopeCount + " active)";
        }
        return "pending(no completed CPU scopes)";
    }

    public String gpuTimingAvailabilityLabel() {
        if (this.hasGpuTimings()) {
            return "reported(" + this.gpuScopeDurationsMillis.size() + " scopes)";
        }
        return "unavailable(native/Vulkan GPU timestamps not reported)";
    }

    public String compactAvailabilityLine() {
        return "CPU=" + this.cpuTimingAvailabilityLabel()
                + " | GPU=" + this.gpuTimingAvailabilityLabel();
    }

    public String measurementBoundaryLabel() {
        String cpuBoundary = this.hasCpuTimings()
                ? "CPU frame scopes measured"
                : this.activeCpuScopeCount > 0
                ? "CPU scopes active, completed timings pending"
                : "CPU timings pending";
        String gpuBoundary = this.hasGpuTimings()
                ? "real GPU timestamp scopes measured"
                : "GPU timestamps unavailable until native/Vulkan timing scopes are wired";
        return cpuBoundary + "; " + gpuBoundary;
    }

    public List<String> compactLightingStageTimingLines() {
        return List.of(
                this.compactStageTimingLine("GI", "diffuse_gi", "low_res_gi", "low_resolution_gi", "gi"),
                this.compactStageTimingLine("Denoise", "denoise", "diffuse_denoise", "edge_aware_denoise"),
                this.compactStageTimingLine("Composite", "composite", "final_composite"),
                this.compactStageTimingLine("Adaptive", "adaptive_sampling", "ray_budget", "variance", "history_confidence"),
                this.compactStageTimingLine("Final", "final_composite", "present", "submit")
        );
    }

    public String compactStageTimingLine(String stageLabel, String... scopeAliases) {
        String label = cleanLabel(stageLabel, "stage");
        Double cpuMillis = firstMatchingDuration(this.cpuScopeDurationsMillis, scopeAliases);
        Double gpuMillis = firstMatchingDuration(this.gpuScopeDurationsMillis, scopeAliases);
        return label
                + " CPU=" + durationOrPending(cpuMillis, this.cpuTimingAvailabilityLabel())
                + " GPU=" + durationOrUnavailable(gpuMillis, this.gpuTimingAvailabilityLabel());
    }

    public double totalCpuMillis() {
        return totalMillis(this.cpuScopeDurationsMillis);
    }

    public double totalGpuMillis() {
        return totalMillis(this.gpuScopeDurationsMillis);
    }

    private static double totalMillis(Map<String, Double> timings) {
        double total = 0.0D;
        for (double durationMillis : timings.values()) {
            total += durationMillis;
        }
        return total;
    }

    private static Map<String, Double> immutableMap(Map<String, Double> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static Double firstMatchingDuration(Map<String, Double> timings, String... aliases) {
        if (timings == null || timings.isEmpty() || aliases == null) {
            return null;
        }

        for (String alias : aliases) {
            String normalizedAlias = normalize(alias);
            if (normalizedAlias.isBlank()) {
                continue;
            }
            for (Map.Entry<String, Double> entry : timings.entrySet()) {
                String normalizedKey = normalize(entry.getKey());
                if (normalizedKey.equals(normalizedAlias)
                        || normalizedKey.contains(normalizedAlias)
                        || normalizedAlias.contains(normalizedKey)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static String durationOrPending(Double durationMillis, String fallback) {
        if (durationMillis != null) {
            return formatMillis(durationMillis);
        }
        return fallback.startsWith("reported(") ? "pending(no matching scope)" : fallback;
    }

    private static String durationOrUnavailable(Double durationMillis, String fallback) {
        if (durationMillis != null) {
            return formatMillis(durationMillis);
        }
        return fallback.startsWith("reported(") ? "pending(no matching scope)" : fallback;
    }

    private static String formatMillis(double millis) {
        return String.format(java.util.Locale.ROOT, "%.3fms", millis);
    }

    private static String cleanLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_')
                .replace(' ', '_');
    }
}
