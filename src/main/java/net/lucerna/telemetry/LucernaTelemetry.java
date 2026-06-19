package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LucernaTelemetry {
    private final Map<String, Long> cpuScopeStartNanos = new LinkedHashMap<>();
    private final Map<String, Long> cpuScopeDurationsNanos = new LinkedHashMap<>();

    public synchronized void beginCpuScope(String name) {
        this.cpuScopeStartNanos.put(name, System.nanoTime());
    }

    public synchronized void endCpuScope(String name) {
        Long start = this.cpuScopeStartNanos.remove(name);
        if (start != null) {
            this.cpuScopeDurationsNanos.put(name, System.nanoTime() - start);
        }
    }

    public synchronized Map<String, Long> cpuScopeDurationsNanos() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.cpuScopeDurationsNanos));
    }

    public synchronized Map<String, Double> cpuScopeDurationsMillis() {
        Map<String, Double> durationsMillis = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : this.cpuScopeDurationsNanos.entrySet()) {
            durationsMillis.put(entry.getKey(), entry.getValue() / 1_000_000.0D);
        }
        return Collections.unmodifiableMap(durationsMillis);
    }

    public synchronized int activeCpuScopeCount() {
        return this.cpuScopeStartNanos.size();
    }

    public synchronized void clear() {
        this.cpuScopeStartNanos.clear();
        this.cpuScopeDurationsNanos.clear();
    }
}
