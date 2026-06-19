package net.lucerna.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LucernaTelemetry {
    private final Map<String, Long> cpuScopeStartNanos = new LinkedHashMap<>();
    private final Map<String, Long> cpuScopeDurationsNanos = new LinkedHashMap<>();
    private final Map<String, Long> gpuScopeDurationsNanos = new LinkedHashMap<>();

    public synchronized void beginCpuScope(String name) {
        this.cpuScopeStartNanos.put(normalizeScopeName(name), System.nanoTime());
    }

    public synchronized void endCpuScope(String name) {
        String normalizedName = normalizeScopeName(name);
        Long start = this.cpuScopeStartNanos.remove(normalizedName);
        if (start != null) {
            this.cpuScopeDurationsNanos.put(normalizedName, System.nanoTime() - start);
        }
    }

    public synchronized void recordCpuScopeNanos(String name, long durationNanos) {
        this.cpuScopeDurationsNanos.put(normalizeScopeName(name), normalizeDuration(durationNanos));
    }

    public synchronized void recordGpuScopeNanos(String name, long durationNanos) {
        this.gpuScopeDurationsNanos.put(normalizeScopeName(name), normalizeDuration(durationNanos));
    }

    public synchronized void recordGpuScopeMillis(String name, double durationMillis) {
        this.recordGpuScopeNanos(name, millisToNanos(durationMillis));
    }

    public synchronized Map<String, Long> cpuScopeDurationsNanos() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.cpuScopeDurationsNanos));
    }

    public synchronized Map<String, Double> cpuScopeDurationsMillis() {
        return durationsMillis(this.cpuScopeDurationsNanos);
    }

    public synchronized Map<String, Long> gpuScopeDurationsNanos() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.gpuScopeDurationsNanos));
    }

    public synchronized Map<String, Double> gpuScopeDurationsMillis() {
        return durationsMillis(this.gpuScopeDurationsNanos);
    }

    public synchronized List<String> activeCpuScopeNames() {
        return Collections.unmodifiableList(new ArrayList<>(this.cpuScopeStartNanos.keySet()));
    }

    public synchronized int activeCpuScopeCount() {
        return this.cpuScopeStartNanos.size();
    }

    public synchronized void clear() {
        this.cpuScopeStartNanos.clear();
        this.cpuScopeDurationsNanos.clear();
        this.gpuScopeDurationsNanos.clear();
    }

    private static String normalizeScopeName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.trim();
    }

    private static long normalizeDuration(long durationNanos) {
        return Math.max(0L, durationNanos);
    }

    private static long millisToNanos(double durationMillis) {
        if (!Double.isFinite(durationMillis) || durationMillis <= 0.0D) {
            return 0L;
        }
        return Math.round(durationMillis * 1_000_000.0D);
    }

    private static Map<String, Double> durationsMillis(Map<String, Long> durationsNanos) {
        Map<String, Double> durationsMillis = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : durationsNanos.entrySet()) {
            durationsMillis.put(entry.getKey(), entry.getValue() / 1_000_000.0D);
        }
        return Collections.unmodifiableMap(durationsMillis);
    }
}
