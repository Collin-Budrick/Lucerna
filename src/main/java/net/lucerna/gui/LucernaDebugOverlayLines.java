package net.lucerna.gui;

import net.lucerna.config.DebugOverlay;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LucernaDebugOverlayLines {
    private LucernaDebugOverlayLines() {
    }

    public static Component statusLine(LucernaStatusSnapshot snapshot) {
        return Component.literal(snapshot.compactStatusLine());
    }

    public static List<Component> settingsSummary(LucernaStatusSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(snapshot));
        lines.add(Component.literal("Backend: " + snapshot.backendLabel() + " | Native: " + snapshot.nativeBridgeLabel()));
        lines.add(Component.literal("Renderer: " + snapshot.rendererStateLabel()
                + " | Quality: " + snapshot.qualityPreset().displayName()
                + " | Iris: " + snapshot.irisLabel()));
        lines.add(Component.literal("Dirty: pending=" + snapshot.pendingDirtyRegionCount()
                + " worldGen=" + snapshot.worldGeneration()
                + " | Upload: worldGen=" + snapshot.uploadWorldGeneration()
                + " materialGen=" + snapshot.uploadMaterialGeneration()));
        lines.add(Component.literal("Frame: stage=" + snapshot.frameStage()
                + " context=" + snapshot.frameLifecycle().contextStatus()
                + " source=" + snapshot.frameContextAcquisition().source()));
        return lines;
    }

    public static List<Component> selectedOverlay(LucernaStatusSnapshot snapshot) {
        DebugOverlay overlay = snapshot.debugOverlay();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Debug overlay: " + overlay.displayName()));

        switch (overlay) {
            case BACKEND -> addBackendLines(lines, snapshot);
            case DIRTY_REGIONS -> addDirtyRegionLines(lines, snapshot);
            case MATERIAL_IDS -> addMaterialLines(lines, snapshot);
            case FRAME_TIMINGS -> addTimingLines(lines, snapshot);
            case NATIVE_QUEUE -> addNativeQueueLines(lines, snapshot);
            case OFF -> lines.add(statusLine(snapshot));
        }

        return lines;
    }

    public static List<Component> validationLines(LucernaStatusSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(snapshot));
        snapshot.validationFields().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Component.literal(entry.getKey() + "=" + entry.getValue()))
                .forEach(lines::add);
        return lines;
    }

    private static void addBackendLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Backend kind: " + snapshot.backendKind()));
        lines.add(Component.literal("Backend active: " + yesNo(snapshot.backend().active())));
        lines.add(Component.literal("Backend name: " + snapshot.backendName()));
        lines.add(Component.literal("Backend message: " + snapshot.backendMessage()));
        lines.add(Component.literal("Renderer state: " + snapshot.rendererStateLabel()));
        lines.add(Component.literal("Native bridge: " + snapshot.nativeBridgeLabel()));
        lines.add(Component.literal("Frame context: " + snapshot.frameLifecycle().contextStatus()
                + " | ready=" + yesNo(snapshot.frameLifecycle().contextReady())));
        lines.add(Component.literal("Frame context source: " + snapshot.frameContextAcquisition().source()));
        lines.add(Component.literal("Frame context message: " + snapshot.frameLifecycle().contextMessage()));
        lines.add(Component.literal("Iris: " + snapshot.irisLabel() + " (" + snapshot.iris().shaderPackState() + ")"));
    }

    private static void addDirtyRegionLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Pending dirty regions: " + snapshot.pendingDirtyRegionCount()));
        lines.add(Component.literal("World generation: " + snapshot.worldGeneration()));
        lines.add(Component.literal("Last uploaded world generation: " + snapshot.uploadWorldGeneration()));
        lines.add(Component.literal("Pending world upload lag: " + snapshot.pendingWorldUploadLag()));
        lines.add(Component.literal("Last uploaded material generation: " + snapshot.uploadMaterialGeneration()));
    }

    private static void addMaterialLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Material id overlay awaiting extraction data."));
        lines.add(Component.literal("Last uploaded material generation: " + snapshot.uploadMaterialGeneration()));
        lines.add(Component.literal("Combined upload generation: " + snapshot.uploadGeneration()));
    }

    private static void addTimingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        if (!snapshot.frameTimings().hasAnyTimings()) {
            lines.add(Component.literal("No completed frame timings yet."));
            if (snapshot.activeCpuScopeCount() > 0) {
                lines.add(Component.literal("Active CPU scopes: " + snapshot.activeCpuScopeCount()));
            }
            return;
        }

        lines.add(Component.literal("CPU total: " + formatMillis(snapshot.frameTimings().totalCpuMillis())));
        lines.add(Component.literal("Frame stage: " + snapshot.frameStage()
                + " | pass=" + snapshot.framePassIntent()
                + " | context=" + snapshot.frameLifecycle().contextStatus()));
        for (Map.Entry<String, Double> timing : snapshot.cpuScopeDurationsMillis().entrySet()) {
            lines.add(Component.literal("CPU " + timing.getKey() + ": " + formatMillis(timing.getValue())));
        }
        if (snapshot.hasGpuTimings()) {
            lines.add(Component.literal("GPU total: " + formatMillis(snapshot.frameTimings().totalGpuMillis())));
            for (Map.Entry<String, Double> timing : snapshot.gpuScopeDurationsMillis().entrySet()) {
                lines.add(Component.literal("GPU " + timing.getKey() + ": " + formatMillis(timing.getValue())));
            }
        }
        if (snapshot.activeCpuScopeCount() > 0) {
            lines.add(Component.literal("Active CPU scopes: " + String.join(", ", snapshot.frameTimings().activeCpuScopeNames())));
        }
    }

    private static void addNativeQueueLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Native state: " + snapshot.nativeBridgeLabel()));
        lines.add(Component.literal("Native loadAttempted=" + yesNo(snapshot.nativeBridge().loadAttempted())
                + " loaded=" + yesNo(snapshot.nativeBridge().loaded())
                + " available=" + yesNo(snapshot.nativeBridge().available())
                + " initialized=" + yesNo(snapshot.nativeBridge().initialized())));
        lines.add(Component.literal("Native status: " + snapshot.nativeBridge().nativeStatus()));
        lines.add(Component.literal("Native last error: " + snapshot.nativeBridge().diagnosticMessage()));
        lines.add(Component.literal("Frame context: " + snapshot.frameLifecycle().contextStatus()
                + " | " + snapshot.frameLifecycle().contextMessage()));
        lines.add(Component.literal("Upload generation: " + snapshot.uploadGeneration()));
        lines.add(Component.literal("Upload world=" + snapshot.uploadWorldGeneration()
                + " material=" + snapshot.uploadMaterialGeneration()
                + " pendingDirty=" + snapshot.pendingDirtyRegionCount()));
    }

    private static String formatMillis(double millis) {
        return String.format(Locale.ROOT, "%.3f ms", millis);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
