package net.lucerna.gui;

import net.lucerna.config.DebugOverlay;
import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LightingDispatchTelemetryStatus;
import net.lucerna.telemetry.NativePassTelemetryStatus;
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
        lines.add(Component.literal("Staging: sections=" + snapshot.stagedSectionSnapshotCount()
                + " sectionGen=" + snapshot.uploadSectionGeneration()
                + " | G-buffer: " + snapshot.gBufferStagingLabel()));
        lines.add(Component.literal("Frame: stage=" + snapshot.frameStage()
                + " context=" + snapshot.frameLifecycle().contextStatus()
                + " source=" + snapshot.frameContextAcquisition().source()));
        lines.add(Component.literal("Frame constants: " + snapshot.frameConstantsLabel()
                + " | required=" + yesNo(snapshot.frameConstantsRequiredAvailable())
                + " | fresh=" + yesNo(snapshot.frameConstantsFresh())));
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
            case DIRECT_LIGHTING -> addDirectLightingLines(lines, snapshot);
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
        lines.add(Component.literal("Frame constants: " + snapshot.frameConstants().stateLabel()
                + " | " + snapshot.frameConstants().message()));
        lines.add(Component.literal("Native status: " + snapshot.nativeBridge().nativeStatus()));
        lines.add(Component.literal("Native pass states: " + snapshot.nativePassStateLabel()));
        lines.add(Component.literal("Frame pass status: " + snapshot.framePassStatusLabel()));
        lines.add(Component.literal("First-pass validation: " + snapshot.firstPassValidationSummary()));
        lines.add(Component.literal("Iris: " + snapshot.irisLabel() + " (" + snapshot.iris().shaderPackState() + ")"));
    }

    private static void addDirtyRegionLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Pending dirty regions: " + snapshot.pendingDirtyRegionCount()));
        lines.add(Component.literal("World generation: " + snapshot.worldGeneration()));
        lines.add(Component.literal("Last uploaded world generation: " + snapshot.uploadWorldGeneration()));
        lines.add(Component.literal("Pending world upload lag: " + snapshot.pendingWorldUploadLag()));
        lines.add(Component.literal("Last uploaded material generation: " + snapshot.uploadMaterialGeneration()));
        lines.add(Component.literal("Section snapshots: " + snapshot.sectionSnapshotStagingLabel()));
        lines.add(Component.literal("G-buffer staging: " + snapshot.gBufferStagingLabel()));
        lines.add(Component.literal("G-buffer staging explicit: " + snapshot.explicitGBufferStagingLabel()));
        lines.add(Component.literal("First-pass validation: " + snapshot.firstPassValidationSummary()));
    }

    private static void addMaterialLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("Material id overlay awaiting extraction data."));
        lines.add(Component.literal("Last uploaded material generation: " + snapshot.uploadMaterialGeneration()));
        lines.add(Component.literal("Section material generation: " + snapshot.uploadSectionMaterialGeneration()));
        lines.add(Component.literal("G-buffer staging: " + snapshot.explicitGBufferStagingLabel()));
        lines.add(Component.literal("First-pass validation: " + snapshot.firstPassValidationSummary()));
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
        lines.add(Component.literal("Frame pass status: " + snapshot.framePassStatusLabel()));
        lines.add(Component.literal("Frame constants: " + snapshot.frameConstants().stateLabel()
                + " | age=" + formatOptionalMillis(snapshot.frameConstants().ageMillis())));
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
        lines.add(Component.literal("Native diagnostic: " + snapshot.nativeBridge().diagnosticMessage()));
        lines.add(Component.literal("Frame context: " + snapshot.frameLifecycle().contextStatus()
                + " | " + snapshot.frameLifecycle().contextMessage()));
        lines.add(Component.literal("Upload generation: " + snapshot.uploadGeneration()));
        lines.add(Component.literal("Upload world=" + snapshot.uploadWorldGeneration()
                + " material=" + snapshot.uploadMaterialGeneration()
                + " pendingDirty=" + snapshot.pendingDirtyRegionCount()));
        lines.add(Component.literal("Upload generations: " + snapshot.uploadGenerationLabel()));
        lines.add(Component.literal("Section generations: " + snapshot.sectionGenerationLabel()));
        lines.add(Component.literal("Section snapshots: " + snapshot.sectionSnapshotStagingLabel()));
        lines.add(Component.literal("G-buffer staging: " + snapshot.gBufferStagingLabel()));
        lines.add(Component.literal("G-buffer staging explicit: " + snapshot.explicitGBufferStagingLabel()));
        lines.add(Component.literal("Staging payloads: " + snapshot.stagingPayloadLabel()));
        addNativePassStateLines(lines, snapshot);
        addLightingDispatchLines(lines, snapshot);
        lines.add(Component.literal("Frame pass status: " + snapshot.framePassStatusLabel()));
        lines.add(Component.literal("First-pass validation: " + snapshot.firstPassValidationSummary()));
    }

    private static void addDirectLightingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus directStage = lightingDispatch.stages().get("direct_lighting");

        lines.add(Component.literal("Overlay state: " + snapshot.debugOverlay().name()
                + " | Renderer: " + snapshot.rendererStateLabel()
                + " | Native: " + snapshot.nativeBridgeLabel()));

        if (!lightingDispatch.hasLightingDispatchStatus()) {
            lines.add(Component.literal("Lighting dispatch: unavailable"));
            lines.add(Component.literal("Reason: " + shorten(lightingDispatch.message(), 96)));
            lines.add(Component.literal("Frame: " + snapshot.frameLifecycle().frameIndex()
                    + " | Context: " + snapshot.frameLifecycle().contextStatus()));
            return;
        }

        lines.add(Component.literal("Lighting dispatch: " + lightingDispatch.compactLabel()));
        if (directStage == null) {
            lines.add(Component.literal("Direct stage: not reported"));
            lines.add(Component.literal("Frame: " + snapshot.frameLifecycle().frameIndex()
                    + " | submitted=" + yesNo(snapshot.frameLifecycle().lightingSubmitted())));
            return;
        }

        lines.add(Component.literal("Direct stage: enabled=" + yesNoUnknown(directStage.enabled())
                + " native=" + nativeExecutionLabel(directStage)
                + " debug=" + yesNoUnknown(directStage.debugOverlay())));
        lines.add(Component.literal("Direct counts: candidates=" + countOrFallback(directStage.candidateCount(), directStage.sampleCount())
                + " samples=" + valueOrUnknown(directStage.sampleCount())
                + " rays=" + valueOrUnknown(directStage.rayCount())));
        lines.add(Component.literal("Direct dispatch: frame=" + directDispatchFrameLabel(snapshot, directStage)
                + " gen=" + valueOrUnknown(directStage.generation())
                + " groups=" + valueOrUnknown(directStage.dispatchGroups())));
        lines.add(Component.literal("Direct payload: accepted=" + yesNoUnknown(directStage.payloadAccepted())
                + " gen=" + valueOrUnknown(directStage.payloadGeneration())
                + " frame=" + valueOrUnknown(directStage.payloadFrameIndex())
                + " range=" + valueOrUnknown(directStage.payloadGenerationRange())));
        lines.add(Component.literal("Direct payload counts: celestial=" + valueOrUnknown(directStage.celestialCount())
                + " emissive=" + valueOrUnknown(directStage.emissiveCount())
                + " shadow=" + valueOrUnknown(directStage.shadowCandidateCount())
                + " budgetedShadow=" + valueOrUnknown(directStage.budgetedShadowCandidateCount())
                + " sections=" + valueOrUnknown(directStage.sectionSnapshotCount())));
        lines.add(Component.literal("Direct payload readiness: " + payloadReadinessLabel(directStage)));
        lines.add(Component.literal("Direct output: writes=" + detailOrUnknown(directStage, "output_writes")
                + " resolves=" + detailOrUnknown(directStage, "resolves")
                + " writeRecorded=" + detailOrUnknown(directStage, "output_write_recorded")
                + " resolveRecorded=" + detailOrUnknown(directStage, "resolve_recorded")));
        lines.add(Component.literal("Direct CPU output: generated=" + yesNoUnknown(directStage.cpuOutputGenerated())
                + " size=" + valueOrUnknown(directStage.outputDimensions())
                + " pixels=" + valueOrUnknown(directStage.outputPixelCount())
                + " energy=" + valueOrUnknown(directStage.outputEnergy())
                + " checksum=" + valueOrUnknown(directStage.outputChecksum())));
        lines.add(Component.literal("Direct native: attempts=" + detailOrUnknown(directStage, "attempts")
                + " submitted=" + detailOrUnknown(directStage, "submitted")
                + " skipped=" + detailOrUnknown(directStage, "skipped")
                + " marker=" + shorten(detailOrUnknown(directStage, "output_marker"), 48)));
        lines.add(Component.literal("Direct flags: " + directFlagLabel(directStage)));
        String reason = directStage.readinessReason().isBlank() ? lightingDispatch.message() : directStage.readinessReason();
        lines.add(Component.literal("Direct readiness: " + shorten(reason, 96)));
    }

    private static void addNativePassStateLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        NativePassTelemetryStatus nativePassStates = snapshot.nativePassStates();
        lines.add(Component.literal("Native pass states: " + nativePassStates.compactLabel()));
        if (!nativePassStates.hasPassStates()) {
            return;
        }

        for (Map.Entry<String, String> entry : nativePassStates.passStates().entrySet()) {
            lines.add(Component.literal("Native pass " + entry.getKey() + ": " + entry.getValue()));
        }
    }

    private static void addLightingDispatchLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        lines.add(Component.literal("Lighting dispatch: " + lightingDispatch.compactLabel()));
        if (!lightingDispatch.hasStageStatuses()) {
            return;
        }

        for (LightingDispatchStageTelemetryStatus stage : lightingDispatch.stages().values()) {
            lines.add(Component.literal("Lighting stage: " + stage.compactLabel()));
        }
    }

    private static String nativeExecutionLabel(LightingDispatchStageTelemetryStatus stage) {
        if (isTruthy(stage.details().get("output_write_recorded"))
                && isTruthy(stage.details().get("resolve_recorded"))) {
            return "executed";
        }
        if (parsePositive(stage.details().get("submitted"))) {
            return "submitted";
        }
        if (Boolean.TRUE.equals(stage.readyForNativeExecution())) {
            return Boolean.TRUE.equals(stage.placeholder()) ? "placeholder" : "ready";
        }
        if (Boolean.FALSE.equals(stage.readyForNativeExecution())) {
            return "blocked";
        }
        if (Boolean.TRUE.equals(stage.placeholder())) {
            return "placeholder";
        }
        return "unknown";
    }

    private static String directFlagLabel(LightingDispatchStageTelemetryStatus stage) {
        List<String> flags = new ArrayList<>();
        if (Boolean.TRUE.equals(stage.placeholder())) {
            flags.add("placeholder");
        }
        if (Boolean.TRUE.equals(stage.metadataOnly())) {
            flags.add("metadata_only");
        }
        if (Boolean.TRUE.equals(stage.validated())) {
            flags.add("validated");
        }
        if (Boolean.TRUE.equals(stage.recordedThisFrame())) {
            flags.add("recorded");
        }
        if (stage.flags() != null) {
            flags.add("raw=" + stage.flags());
        }
        return flags.isEmpty() ? "unreported" : String.join(",", flags);
    }

    private static String payloadReadinessLabel(LightingDispatchStageTelemetryStatus stage) {
        List<String> fields = new ArrayList<>();
        fields.add("metadata_only=" + yesNoUnknown(stage.metadataOnly()));
        fields.add("validated=" + yesNoUnknown(stage.payloadValidated()));
        fields.add("hasWork=" + yesNoUnknown(stage.payloadHasDirectWork()));
        fields.add("shadowReady=" + yesNoUnknown(stage.payloadReadyForShadowTracing()));
        return String.join(" ", fields);
    }

    private static String countOrFallback(Long count, Long fallback) {
        if (count != null) {
            return Long.toString(count);
        }
        if (fallback != null) {
            return Long.toString(fallback) + " inferred";
        }
        return "?";
    }

    private static String directDispatchFrameLabel(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        if (stage.frameIndex() != null) {
            return Long.toString(stage.frameIndex());
        }
        if (Boolean.TRUE.equals(stage.recordedThisFrame())) {
            return snapshot.frameLifecycle().frameIndex() + " current";
        }
        return "?";
    }

    private static String valueOrUnknown(Long value) {
        return value == null ? "?" : Long.toString(value);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String detailOrUnknown(LightingDispatchStageTelemetryStatus stage, String key) {
        return valueOrUnknown(stage.details().get(key));
    }

    private static boolean isTruthy(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private static boolean parsePositive(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(value) > 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String yesNoUnknown(Boolean value) {
        if (value == null) {
            return "?";
        }
        return yesNo(value);
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() <= maxLength) {
            return value == null || value.isBlank() ? "unreported" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String formatMillis(double millis) {
        return String.format(Locale.ROOT, "%.3f ms", millis);
    }

    private static String formatOptionalMillis(double millis) {
        if (millis < 0.0D) {
            return "unavailable";
        }
        return formatMillis(millis);
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
