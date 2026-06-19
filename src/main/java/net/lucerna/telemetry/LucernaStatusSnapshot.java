package net.lucerna.telemetry;

import net.lucerna.LucernaController;
import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;
import net.lucerna.config.DebugOverlay;
import net.lucerna.config.QualityPreset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record LucernaStatusSnapshot(
        boolean rendererEnabled,
        boolean rendererActive,
        QualityPreset qualityPreset,
        DebugOverlay debugOverlay,
        BackendKind backendKind,
        String backendName,
        String backendMessage,
        String irisMessage,
        long worldGeneration,
        long uploadGeneration,
        Map<String, Double> cpuScopeDurationsMillis,
        int activeCpuScopeCount,
        long capturedNanos
) {
    public LucernaStatusSnapshot {
        if (qualityPreset == null) {
            qualityPreset = QualityPreset.BALANCED;
        }
        if (debugOverlay == null) {
            debugOverlay = DebugOverlay.OFF;
        }
        if (backendKind == null) {
            backendKind = BackendKind.UNKNOWN;
        }
        backendName = clean(backendName, "unknown");
        backendMessage = clean(backendMessage, "Backend status has not been reported.");
        irisMessage = clean(irisMessage, "Iris status has not been reported.");
        cpuScopeDurationsMillis = immutableCopy(cpuScopeDurationsMillis);
    }

    public static LucernaStatusSnapshot capture(LucernaController controller) {
        BackendStatus backendStatus = controller.backendStatus();
        return new LucernaStatusSnapshot(
                controller.getConfig().rendererEnabled(),
                controller.isRendererActive(),
                controller.getConfig().qualityPreset(),
                controller.getConfig().debugOverlay(),
                backendStatus.kind(),
                backendStatus.backendName(),
                backendStatus.userMessage(),
                controller.irisCompat().statusMessage(),
                controller.worldFeed().currentGeneration(),
                controller.uploadQueue().lastGeneration(),
                controller.telemetry().cpuScopeDurationsMillis(),
                controller.telemetry().activeCpuScopeCount(),
                System.nanoTime()
        );
    }

    public String rendererStateLabel() {
        if (!this.rendererEnabled) {
            return "disabled by config";
        }
        return this.rendererActive ? "active" : "inactive";
    }

    public String backendLabel() {
        return this.backendName + " (" + this.backendKind.name() + ")";
    }

    public boolean hasCpuTimings() {
        return !this.cpuScopeDurationsMillis.isEmpty();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static Map<String, Double> immutableCopy(Map<String, Double> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
