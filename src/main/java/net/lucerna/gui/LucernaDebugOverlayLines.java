package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.render.preview.FinalCompositeModeStatus;
import net.lucerna.render.preview.Round8AdaptiveDebugStatus;
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
        String directSummary = roundFiveDirectSummary(snapshot);
        if (!directSummary.isBlank()) {
            lines.add(Component.literal("Round 5 direct: " + directSummary));
        }
        String roundSixSummary = roundSixSummary(snapshot);
        if (!roundSixSummary.isBlank()) {
            lines.add(Component.literal("Round 6 GI/cache: " + roundSixSummary));
        }
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();
        lines.add(Component.literal("Round 7 mix: " + compositeStatus.compactSourceMixPolicy()));
        lines.add(Component.literal("Round 7 denoise: " + compositeStatus.denoiseSourcePolicy()));
        Round8AdaptiveDebugStatus round8 = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Round 8 adaptive debug: " + round8.summary()));
        lines.add(Component.literal("Round 8 heatmaps: " + round8.heatmapRolesLine()));
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
            case ADAPTIVE_SAMPLING -> addAdaptiveSamplingLines(lines, snapshot);
            case RAY_BUDGET_HEATMAP -> addRayBudgetHeatmapLines(lines, snapshot);
            case VARIANCE_MAP -> addVarianceMapLines(lines, snapshot);
            case HISTORY_CONFIDENCE -> addHistoryConfidenceLines(lines, snapshot);
            case DISOCCLUSION_MASK -> addDisocclusionMaskLines(lines, snapshot);
            case OFF -> lines.add(statusLine(snapshot));
        }
        addCompositeModeLines(lines);

        return lines;
    }

    public static List<Component> roundSixEvidenceOverlay(LucernaStatusSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Debug overlay: Round 6 GI/cache evidence"));
        addRoundSixEvidenceLines(lines, snapshot);
        return lines;
    }

    private static void addCompositeModeLines(List<Component> lines) {
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();
        lines.add(Component.literal("Round 7 mode: " + compositeStatus.statusKey()
                + " | " + compositeStatus.signalIsolationLabel()));
        lines.add(Component.literal("Round 7 mix: " + compositeStatus.compactSourceMixPolicy()));
        lines.add(Component.literal("Round 7 denoise: " + compositeStatus.denoiseSourcePolicy()));
        lines.add(Component.literal("Round 7 source guard: " + compositeStatus.compactAuthenticityPolicy()));
        lines.add(Component.literal("Round 7 proof gate: " + compositeStatus.firstLightingMilestoneGate()));
        lines.add(Component.literal("Round 7 evidence: " + compositeStatus.controllerEvidenceLine()));
    }

    public static List<Component> validationLines(LucernaStatusSnapshot snapshot) {
        List<Component> lines = new ArrayList<>();
        lines.add(statusLine(snapshot));
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();
        lines.add(Component.literal("round7.compositeMode=" + compositeStatus.statusKey()));
        lines.add(Component.literal("round7.compositeDisplayName=" + compositeStatus.displayName()));
        lines.add(Component.literal("round7.compositeEvidenceKey=" + compositeStatus.evidenceKey()));
        lines.add(Component.literal("round7.compositeDispatch=" + compositeStatus.dispatchLabel()));
        lines.add(Component.literal("round7.compositeSignals=baseWorldColor:"
                + yesNo(compositeStatus.baseWorldColorEnabled())
                + ",directLighting:"
                + yesNo(compositeStatus.directLightingEnabled())
                + ",diffuseGi:"
                + yesNo(compositeStatus.diffuseGiEnabled())));
        lines.add(Component.literal("round7.compositeIsolation=" + compositeStatus.signalIsolationLabel()));
        lines.add(Component.literal("round7.selectedSourcePolicy=" + compositeStatus.selectedSourcePolicy()));
        lines.add(Component.literal("round7.finalCompositeSubmissionPolicy="
                + compositeStatus.finalCompositeSubmissionPolicy()));
        lines.add(Component.literal("round7.selectedSourceAuthenticityPolicy="
                + compositeStatus.selectedSourceAuthenticityPolicy()));
        lines.add(Component.literal("round7.focusedRegionProofExpectation="
                + compositeStatus.focusedRegionProofExpectation()));
        lines.add(Component.literal("round7.visualProofBoundary=" + compositeStatus.visualProofBoundarySummary()));
        lines.add(Component.literal("round7.compositeReason=" + compositeStatus.modeReason()));
        lines.add(Component.literal("round7.compositeExpectedEvidence=" + compositeStatus.expectedEvidence()));
        lines.add(Component.literal("round7.compositeValidation=" + compositeStatus.validationSummary()));
        lines.add(Component.literal("round7.compactSourceMix=" + compositeStatus.compactSourceMixPolicy()));
        lines.add(Component.literal("round7.denoiseSourcePolicy=" + compositeStatus.denoiseSourcePolicy()));
        lines.add(Component.literal("round7.firstLightingMilestoneGate="
                + compositeStatus.firstLightingMilestoneGate()));
        Round8AdaptiveDebugStatus round8 = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("round8.adaptiveDebugSummary=" + round8.summary()));
        lines.add(Component.literal("round8.adaptiveSampling=" + round8.adaptiveSamplingLine()));
        lines.add(Component.literal("round8.sceneState=" + round8.sceneStateLine()));
        lines.add(Component.literal("round8.rayBudget=" + round8.rayBudgetLine()));
        lines.add(Component.literal("round8.rayBudgetBuckets=" + round8.rayBudgetBucketLine()));
        lines.add(Component.literal("round8.rayBudgetHeatmap=" + round8.rayBudgetHeatmapLine()));
        lines.add(Component.literal("round8.varianceMap=" + round8.varianceMapLine()));
        lines.add(Component.literal("round8.historyConfidence=" + round8.historyConfidenceLine()));
        lines.add(Component.literal("round8.historyConfidenceHeatmap=" + round8.historyConfidenceHeatmapLine()));
        lines.add(Component.literal("round8.historyCounts=" + round8.historyCountsLine()));
        lines.add(Component.literal("round8.disocclusionMask=" + round8.disocclusionMaskLine()));
        lines.add(Component.literal("round8.cacheConfidenceContribution=" + round8.cacheConfidenceContributionLine()));
        lines.add(Component.literal("round8.heatmapRoles=" + round8.heatmapRolesLine()));
        lines.add(Component.literal("round8.readiness=" + round8.readinessLine()));
        lines.add(Component.literal("round8.evidenceBoundary=" + round8.evidenceBoundaryLine()));
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

    private static void addAdaptiveSamplingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 8 adaptive sampling status."));
        lines.add(Component.literal(status.sceneStateLine()));
        lines.add(Component.literal(status.adaptiveSamplingLine()));
        lines.add(Component.literal(status.rayBudgetLine()));
        lines.add(Component.literal(status.rayBudgetBucketLine()));
        lines.add(Component.literal(status.varianceMapLine()));
        lines.add(Component.literal(status.historyConfidenceLine()));
        lines.add(Component.literal(status.historyCountsLine()));
        lines.add(Component.literal(status.cacheConfidenceContributionLine()));
        lines.add(Component.literal(status.disocclusionMaskLine()));
        lines.add(Component.literal(status.heatmapRolesLine()));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
    }

    private static void addRayBudgetHeatmapLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 8 ray-budget heatmap role."));
        lines.add(Component.literal(status.sceneStateLine()));
        lines.add(Component.literal(status.rayBudgetHeatmapLine()));
        lines.add(Component.literal(status.rayBudgetLine()));
        lines.add(Component.literal(status.rayBudgetBucketLine()));
        lines.add(Component.literal(status.cacheConfidenceContributionLine()));
        lines.add(Component.literal("Heatmap legend: reuse=blue low=green medium=yellow high=red missing=gray"));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
    }

    private static void addVarianceMapLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 8 variance map readiness."));
        lines.add(Component.literal(status.sceneStateLine()));
        lines.add(Component.literal(status.varianceMapLine()));
        lines.add(Component.literal(status.rayBudgetLine()));
        lines.add(Component.literal(status.cacheConfidenceContributionLine()));
        lines.add(Component.literal("Variance legend: stable=blue refresh=yellow high=red missing=gray"));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
    }

    private static void addHistoryConfidenceLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 8 history-confidence heatmap role."));
        lines.add(Component.literal(status.sceneStateLine()));
        lines.add(Component.literal(status.historyConfidenceHeatmapLine()));
        lines.add(Component.literal(status.historyConfidenceLine()));
        lines.add(Component.literal(status.historyCountsLine()));
        lines.add(Component.literal(status.varianceMapLine()));
        lines.add(Component.literal("History legend: reset=red low=yellow reusable=green missing=gray"));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
    }

    private static void addDisocclusionMaskLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 8 disocclusion mask readiness."));
        lines.add(Component.literal(status.sceneStateLine()));
        lines.add(Component.literal(status.disocclusionMaskLine()));
        lines.add(Component.literal(status.historyConfidenceLine()));
        lines.add(Component.literal(status.historyCountsLine()));
        lines.add(Component.literal("Disocclusion legend: stable=transparent moving=yellow reset=red missing=gray"));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
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
        addRoundSixStatusLines(lines, snapshot);
        lines.add(Component.literal("Frame pass status: " + snapshot.framePassStatusLabel()));
        lines.add(Component.literal("First-pass validation: " + snapshot.firstPassValidationSummary()));
    }

    private static void addDirectLightingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus directStage = lightingDispatch.stages().get("direct_lighting");

        lines.add(Component.literal("Overlay state: " + snapshot.debugOverlay().name()
                + " | Renderer: " + snapshot.rendererStateLabel()
                + " | Native: " + snapshot.nativeBridgeLabel()));
        lines.add(Component.literal("Overlay scope: R5 direct source; R6/R7 GI+denoise lines stay separate."));

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
        lines.add(Component.literal("Direct R5 payload accepted: " + yesNoUnknown(directStage.payloadAccepted())
                + " | validated=" + yesNoUnknown(directStage.payloadValidated())
                + " | hasWork=" + yesNoUnknown(directStage.payloadHasDirectWork())));
        lines.add(Component.literal("Direct R5 payload source: " + directPayloadSourceLabel(directStage)));
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
        lines.add(Component.literal("Direct R5 CPU output generated: " + yesNoUnknown(directStage.cpuOutputGenerated())
                + " | evidence=" + directOutputEvidenceLabel(directStage)));
        lines.add(Component.literal("Direct R5 proof: surface proof validated; remaining gate is GI/denoise/final quality."));
        lines.add(Component.literal("Direct native: attempts=" + detailOrUnknown(directStage, "attempts")
                + " submitted=" + detailOrUnknown(directStage, "submitted")
                + " skipped=" + detailOrUnknown(directStage, "skipped")
                + " marker=" + shorten(detailOrUnknown(directStage, "output_marker"), 48)));
        lines.add(Component.literal("Direct flags: " + directFlagLabel(directStage)));
        String reason = directStage.readinessReason().isBlank() ? lightingDispatch.message() : directStage.readinessReason();
        lines.add(Component.literal("Direct readiness: " + shorten(reason, 96)));
        lines.add(Component.literal("R6/R7 status: raw GI, denoise, cache, and final mix shown in evidence panel."));
    }

    private static void addRoundSixStatusLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        addRoundSixEvidenceLines(lines, snapshot);
    }

    private static void addRoundSixEvidenceLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus diffuseGiStage = firstStage(
                lightingDispatch,
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        );
        LightingDispatchStageTelemetryStatus cacheStage = firstStage(
                lightingDispatch,
                "cache",
                "radiance_cache",
                "sparse_radiance_cache",
                "sparse_voxel_radiance_cache"
        );
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                lightingDispatch,
                "denoise",
                "diffuse_gi_denoise",
                "denoised_gi",
                "round7_denoise"
        );
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();

        lines.add(Component.literal("Round 6 GI/cache status: " + roundSixSummary(snapshot)));
        lines.add(Component.literal("Round 7 mix policy: " + compositeStatus.compactSourceMixPolicy()));
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            lines.add(Component.literal("Round 6 source: lighting dispatch telemetry unavailable"));
            lines.add(Component.literal("Round 6 reason: " + shorten(lightingDispatch.message(), 96)));
            lines.add(Component.literal("First-lighting gate: " + compositeStatus.firstLightingMilestoneGate()));
            return;
        }

        if (diffuseGiStage == null) {
            lines.add(Component.literal("Diffuse GI: not reported by controller/native status yet"));
        } else {
            lines.add(Component.literal("Diffuse GI readiness: enabled=" + yesNoUnknown(diffuseGiStage.enabled())
                    + " native=" + nativeExecutionLabel(diffuseGiStage)
                    + " ready=" + yesNoUnknown(diffuseGiStage.readyForNativeExecution())
                    + " frame=" + dispatchFrameLabel(snapshot, diffuseGiStage)));
            lines.add(Component.literal("Low-res GI dispatch: gen=" + valueOrUnknown(diffuseGiStage.generation())
                    + " size=" + valueOrUnknown(diffuseGiStage.dimensions())
                    + " groups=" + valueOrUnknown(diffuseGiStage.dispatchGroups())
                    + " rays=" + valueOrUnknown(diffuseGiStage.rayCount())
                    + " samples=" + valueOrUnknown(diffuseGiStage.sampleCount())));
            lines.add(Component.literal("Low-res GI IO: " + valueOrUnknown(diffuseGiStage.ioCounts())
                    + " | inputs=" + firstDetailOrUnknown(diffuseGiStage, "inputs", "input_count", "last_input_count")
                    + " outputs=" + firstDetailOrUnknown(diffuseGiStage, "outputs", "output_count", "last_output_count")));
            lines.add(Component.literal("Low-res GI output: source=" + giOutputSourceLabel(diffuseGiStage)
                    + " generated=" + yesNoUnknown(diffuseGiStage.cpuOutputGenerated())
                    + " size=" + valueOrUnknown(diffuseGiStage.outputDimensions())
                    + " pixels=" + valueOrUnknown(diffuseGiStage.outputPixelCount())));
            lines.add(Component.literal("Low-res GI evidence: energy=" + evidenceValueLabel(diffuseGiStage.outputEnergy())
                    + " checksum=" + evidenceValueLabel(diffuseGiStage.outputChecksum())
                    + " temporaryDirectSource=" + giTemporaryDirectSourceLabel(diffuseGiStage)));
            lines.add(Component.literal("Diffuse GI readiness reason: " + readinessReason(diffuseGiStage)));
        }

        if (denoiseStage == null) {
            lines.add(Component.literal("Denoised GI source: not reported; CPU fallback/shader state unknown"));
        } else {
            lines.add(Component.literal("Denoised GI source: " + denoiseSourceLabel(denoiseStage)
                    + " realShader=" + realDenoiseShaderLabel(denoiseStage)
                    + " cpuFallback=" + cpuDenoiseFallbackLabel(denoiseStage)));
            lines.add(Component.literal("Denoised GI evidence: generated="
                    + yesNoUnknown(denoiseStage.cpuOutputGenerated())
                    + " size=" + valueOrUnknown(denoiseStage.outputDimensions())
                    + " energy=" + evidenceValueLabel(denoiseStage.outputEnergy())
                    + " checksum=" + evidenceValueLabel(denoiseStage.outputChecksum())));
        }

        if (cacheStage == null) {
            lines.add(Component.literal("Sparse radiance cache: not reported by controller/native status yet"));
            lines.add(Component.literal("Dirty-region fallback: pending=" + snapshot.pendingDirtyRegionCount()
                    + " worldGen=" + snapshot.worldGeneration()));
        } else {
            lines.add(Component.literal("Sparse radiance cache: records=" + firstDetailOrUnknown(
                    cacheStage,
                    "record_count",
                    "records",
                    "cache_records",
                    "radiance_records"
            ) + " confidence=" + firstDetailOrUnknown(
                    cacheStage,
                    "confidence",
                    "avg_confidence",
                    "average_confidence",
                    "cache_confidence"
            ) + " reads=" + valueOrUnknown(cacheStage.cacheReadCount())
                    + " writes=" + valueOrUnknown(cacheStage.cacheWriteCount())));
            lines.add(Component.literal("Cache dispatch: gen=" + valueOrUnknown(cacheStage.generation())
                    + " frame=" + dispatchFrameLabel(snapshot, cacheStage)
                    + " recorded=" + yesNoUnknown(cacheStage.recordedThisFrame())));
            lines.add(Component.literal("Cache invalidation: dirty=" + firstDetailOrUnknown(
                    cacheStage,
                    "dirty_regions",
                    "pending_dirty_regions",
                    "invalidation_dirty_regions"
            ) + " invalidated=" + firstDetailOrUnknown(
                    cacheStage,
                    "invalidated_records",
                    "invalidation_count",
                    "invalidations"
            ) + " reason=" + readinessReason(cacheStage)));
        }
        lines.add(Component.literal("First-lighting gate: " + compositeStatus.firstLightingMilestoneGate()));
    }

    private static String roundFiveDirectSummary(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            return "";
        }

        LightingDispatchStageTelemetryStatus directStage = lightingDispatch.stages().get("direct_lighting");
        if (directStage == null) {
            return "";
        }

        return "payloadAccepted=" + yesNoUnknown(directStage.payloadAccepted())
                + " cpuOutput=" + yesNoUnknown(directStage.cpuOutputGenerated())
                + " evidence=" + directOutputEvidenceLabel(directStage)
                + " directSurfaceProof=validated qualityStack=open";
    }

    private static String roundSixSummary(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            return "not_reported";
        }

        LightingDispatchStageTelemetryStatus diffuseGiStage = firstStage(
                lightingDispatch,
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        );
        LightingDispatchStageTelemetryStatus cacheStage = firstStage(
                lightingDispatch,
                "cache",
                "radiance_cache",
                "sparse_radiance_cache",
                "sparse_voxel_radiance_cache"
        );
        return "gi=" + stageSummary(diffuseGiStage)
                + " cache=" + stageSummary(cacheStage)
                + " dirtyPending=" + snapshot.pendingDirtyRegionCount();
    }

    private static String giOutputSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "output_source",
                "output_source_label",
                "source",
                "source_label",
                "preview_source",
                "native_output_source",
                "native_gi_output_source"
        );
    }

    private static String giTemporaryDirectSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "temporary_direct_source",
                "temporary_direct_light_source",
                "temporary_direct_light_source_ready",
                "temporary_source_ready",
                "uses_direct_light_payload",
                "using_direct_light_payload",
                "direct_light_payload_source"
        );
    }

    private static String denoiseSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "output_source",
                "output_source_label",
                "source",
                "source_label",
                "denoise_source",
                "denoised_output_source",
                "native_denoise_output_source"
        );
    }

    private static String realDenoiseShaderLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "real_denoise_shader_output",
                "realDenoiseShaderOutput",
                "shader_output",
                "shaderDenoiseOutput",
                "gpu_denoise_output"
        );
    }

    private static String cpuDenoiseFallbackLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "cpu_output_fallback",
                "cpuFallback",
                "cpu_denoise_fallback",
                "cpu_denoised_output",
                "cpu_denoised_diffuse_gi"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        String realShader = realDenoiseShaderLabel(stage);
        if ("false".equalsIgnoreCase(realShader) || "0".equals(realShader)) {
            return "true";
        }
        return "?";
    }

    private static String stageSummary(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "not_reported";
        }
        return "enabled=" + yesNoUnknown(stage.enabled())
                + ",ready=" + yesNoUnknown(stage.readyForNativeExecution())
                + ",frame=" + valueOrUnknown(stage.frameIndex());
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

    private static String directPayloadSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        if (Boolean.TRUE.equals(stage.placeholder())) {
            return "proof-placeholder";
        }
        if (Boolean.TRUE.equals(stage.metadataOnly())) {
            return "metadata-only";
        }
        if (isTruthy(firstDetailOrUnknown(
                stage,
                "proof_marker",
                "proof_marker_source",
                "proof_only",
                "proof_source"
        ))) {
            return "proof-only";
        }
        if (isTruthy(firstDetailOrUnknown(
                stage,
                "focus_window_only",
                "focus_window_source",
                "focus_only",
                "focus_window"
        ))) {
            return "focus-window-only";
        }
        boolean accepted = Boolean.TRUE.equals(stage.payloadAccepted());
        boolean validated = Boolean.TRUE.equals(stage.payloadValidated()) || Boolean.TRUE.equals(stage.validated());
        boolean hasWork = Boolean.TRUE.equals(stage.payloadHasDirectWork());
        boolean wroteOutput = isTruthy(stage.details().get("output_write_recorded"))
                || parsePositive(stage.details().get("output_writes"));
        boolean resolvedOutput = isTruthy(stage.details().get("resolve_recorded"))
                || parsePositive(stage.details().get("resolves"));
        if (accepted && validated && hasWork && wroteOutput && resolvedOutput) {
            return "real-direct-light-payload";
        }
        List<String> missing = new ArrayList<>();
        if (!accepted) {
            missing.add("accepted");
        }
        if (!validated) {
            missing.add("validated");
        }
        if (!hasWork) {
            missing.add("hasWork");
        }
        if (!wroteOutput) {
            missing.add("outputWrite");
        }
        if (!resolvedOutput) {
            missing.add("resolve");
        }
        return "not-real-yet:missing-" + String.join("/", missing);
    }

    private static String payloadReadinessLabel(LightingDispatchStageTelemetryStatus stage) {
        List<String> fields = new ArrayList<>();
        fields.add("metadata_only=" + yesNoUnknown(stage.metadataOnly()));
        fields.add("validated=" + yesNoUnknown(stage.payloadValidated()));
        fields.add("hasWork=" + yesNoUnknown(stage.payloadHasDirectWork()));
        fields.add("shadowReady=" + yesNoUnknown(stage.payloadReadyForShadowTracing()));
        return String.join(" ", fields);
    }

    private static String directOutputEvidenceLabel(LightingDispatchStageTelemetryStatus stage) {
        return "energy=" + evidenceValueLabel(stage.outputEnergy())
                + " checksum=" + evidenceValueLabel(stage.outputChecksum());
    }

    private static String evidenceValueLabel(String value) {
        if (value == null || value.isBlank()) {
            return "missing";
        }
        return "present(" + shorten(value, 24) + ")";
    }

    private static String evidenceValueLabel(Long value) {
        if (value == null) {
            return "missing";
        }
        return "present(" + value + ")";
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

    private static String dispatchFrameLabel(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        if (stage.frameIndex() != null) {
            return Long.toString(stage.frameIndex());
        }
        if (Boolean.TRUE.equals(stage.recordedThisFrame())) {
            return snapshot.frameLifecycle().frameIndex() + " current";
        }
        if (stage.payloadFrameIndex() != null) {
            return stage.payloadFrameIndex() + " payload";
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

    private static String firstDetailOrUnknown(LightingDispatchStageTelemetryStatus stage, String... keys) {
        if (stage == null || keys == null) {
            return "?";
        }
        for (String key : keys) {
            String value = stage.details().get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "?";
    }

    private static LightingDispatchStageTelemetryStatus firstStage(
            LightingDispatchTelemetryStatus lightingDispatch,
            String... stageIds
    ) {
        if (lightingDispatch == null || stageIds == null) {
            return null;
        }
        for (String stageId : stageIds) {
            LightingDispatchStageTelemetryStatus stage = lightingDispatch.stages().get(stageId);
            if (stage != null) {
                return stage;
            }
        }
        return null;
    }

    private static String readinessReason(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null || stage.readinessReason().isBlank()) {
            return "unreported";
        }
        return shorten(stage.readinessReason(), 96);
    }

    private static FinalCompositeModeStatus currentCompositeModeStatus() {
        return FinalCompositeModeStatus.fromConfigMode(
                LucernaController.getInstance().getConfig().compositeMode()
        );
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
