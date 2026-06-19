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
}
