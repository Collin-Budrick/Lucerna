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

    public static List<Component> settingsSummary(LucernaStatusSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Backend: " + snapshot.backendLabel()));
        lines.add(Component.literal("Renderer: " + snapshot.rendererStateLabel()
                + " | Quality: " + snapshot.qualityPreset().displayName()));
        lines.add(Component.literal("Iris: " + snapshot.irisMessage()));
        return lines;
    }

    public static List<Component> selectedOverlay(LucernaStatusSnapshot snapshot) {
        DebugOverlay overlay = snapshot.debugOverlay();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Debug overlay: " + overlay.displayName()));

        switch (overlay) {
            case BACKEND -> addBackendLines(lines, snapshot);
            case DIRTY_REGIONS -> addDirtyRegionLines(lines, snapshot);
            case MATERIAL_IDS -> lines.add(Component.literal("Material id overlay awaiting extraction data."));
            case FRAME_TIMINGS -> addTimingLines(lines, snapshot);
            case NATIVE_QUEUE -> addNativeQueueLines(lines, snapshot);
            case OFF -> lines.add(Component.literal("Overlay disabled."));
        }

        return lines;
    }

    private static void addBackendLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal(snapshot.backendMessage()));
        lines.add(Component.literal("Renderer state: " + snapshot.rendererStateLabel()));
    }

    private static void addDirtyRegionLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("World generation: " + snapshot.worldGeneration()));
        lines.add(Component.literal("Last uploaded generation: " + snapshot.uploadGeneration()));
    }

    private static void addTimingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        if (!snapshot.hasCpuTimings()) {
            lines.add(Component.literal("No completed frame timings yet."));
            if (snapshot.activeCpuScopeCount() > 0) {
                lines.add(Component.literal("Active CPU scopes: " + snapshot.activeCpuScopeCount()));
            }
            return;
        }

        for (Map.Entry<String, Double> timing : snapshot.cpuScopeDurationsMillis().entrySet()) {
            lines.add(Component.literal(timing.getKey() + ": " + formatMillis(timing.getValue())));
        }
    }

    private static void addNativeQueueLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Last upload generation: " + snapshot.uploadGeneration()));
        lines.add(Component.literal("Native renderer: " + snapshot.rendererStateLabel()));
    }

    private static String formatMillis(double millis) {
        return String.format(Locale.ROOT, "%.3f ms", millis);
    }
}
