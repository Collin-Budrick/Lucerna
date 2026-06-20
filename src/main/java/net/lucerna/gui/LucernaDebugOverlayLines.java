package net.lucerna.gui;

import net.lucerna.LucernaController;
import net.lucerna.config.DebugOverlay;
import net.lucerna.render.preview.FirstLightingQualityProofStatus;
import net.lucerna.render.preview.FinalCompositeModeStatus;
import net.lucerna.render.preview.Round8AdaptiveDebugStatus;
import net.lucerna.render.tracing.hybrid.Round10HybridHitDebugStatus;
import net.lucerna.render.virtualization.Round9CullingRuntimeStatus;
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
        FirstLightingQualityProofStatus qualityStatus = FirstLightingQualityProofStatus.fromSnapshot(
                snapshot,
                compositeStatus
        );
        lines.add(Component.literal("Round 7 mix: " + compositeStatus.compactSourceMixPolicy()));
        lines.add(Component.literal("Round 7 denoise: " + compositeStatus.denoiseSourcePolicy()));
        lines.add(Component.literal("Round 7 boundary: " + compositeStatus.lightingStackBoundary()));
        lines.add(Component.literal("First-light quality: " + qualityStatus.summaryLine()));
        lines.add(Component.literal("Timing boundary: " + snapshot.frameTimings().compactAvailabilityLine()));
        Round8AdaptiveDebugStatus round8 = Round8AdaptiveDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Round 8 adaptive debug: " + round8.summary()));
        lines.add(Component.literal("Round 8 heatmaps: " + round8.heatmapRolesLine()));
        Round9CullingRuntimeStatus round9 = Round9CullingRuntimeStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Round 9 culling: " + round9.summary()));
        Round10HybridHitDebugStatus round10 = Round10HybridHitDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Round 10 hybrid hits: " + round10.summary()));
        lines.add(Component.literal("Round 11 ReSTIR reservoirs: " + roundElevenSummary(snapshot)));
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
            case FIRST_LIGHTING_QUALITY -> addFirstLightingQualityLines(lines, snapshot);
            case FIRST_LIGHTING_PHYSICAL_PROOF -> addFirstLightingPhysicalProofLines(lines, snapshot);
            case SHADER_DENOISE_TEMPORAL -> addShaderDenoiseTemporalLines(lines, snapshot);
            case NATIVE_QUEUE -> addNativeQueueLines(lines, snapshot);
            case ADAPTIVE_SAMPLING -> addAdaptiveSamplingLines(lines, snapshot);
            case RAY_BUDGET_HEATMAP -> addRayBudgetHeatmapLines(lines, snapshot);
            case VARIANCE_MAP -> addVarianceMapLines(lines, snapshot);
            case HISTORY_CONFIDENCE -> addHistoryConfidenceLines(lines, snapshot);
            case DISOCCLUSION_MASK -> addDisocclusionMaskLines(lines, snapshot);
            case CHUNK_CULLING -> addChunkCullingLines(lines, snapshot);
            case VOXEL_RAY_DEBUG -> addRoundTenVoxelRayLines(lines, snapshot);
            case RT_ENTITY_DEBUG -> addRoundTenRtEntityLines(lines, snapshot);
            case HYBRID_HIT_DEBUG -> addRoundTenHybridHitLines(lines, snapshot);
            case RESTIR_EXECUTION_DEBUG -> addRoundElevenRestirExecutionLines(lines, snapshot);
            case DIRECT_RESERVOIR_DEBUG -> addRoundElevenDirectReservoirLines(lines, snapshot);
            case GI_RESERVOIR_DEBUG -> addRoundElevenGiReservoirLines(lines, snapshot);
            case RESERVOIR_REUSE_DEBUG -> addRoundElevenReservoirReuseLines(lines, snapshot);
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
        lines.add(Component.literal("Round 7 stack boundary: " + compositeStatus.lightingStackBoundary()));
        lines.add(Component.literal("Round 7 final boundary: " + compositeStatus.finalCompositeBoundary()));
        lines.add(Component.literal("Round 7 source guard: " + compositeStatus.compactAuthenticityPolicy()));
        lines.add(Component.literal("Round 7 proof gate: " + compositeStatus.firstLightingMilestoneGate()));
        lines.add(Component.literal("Round 7 evidence: " + compositeStatus.controllerEvidenceLine()));
    }

    private static void addFirstLightingQualityLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();
        FirstLightingQualityProofStatus qualityStatus = FirstLightingQualityProofStatus.fromSnapshot(
                snapshot,
                compositeStatus
        );
        lines.add(Component.literal("Overlay scope: final composite stability, particles/translucency, temporal history, and denoise source identity."));
        lines.add(Component.literal("First-lighting quality: " + qualityStatus.summaryLine()));
        lines.add(Component.literal("Final composite stability: " + qualityStatus.finalCompositeStability()));
        lines.add(Component.literal("Particles/translucency: " + qualityStatus.particleTranslucencyPreservation()));
        lines.add(Component.literal("Temporal/history: " + qualityStatus.temporalHistoryState()));
        lines.add(Component.literal("Denoise source identity: " + qualityStatus.denoiseSourceIdentity()));
        lines.add(Component.literal("Rejected evidence: " + qualityStatus.rejectedEvidenceTypes()));
        lines.add(Component.literal("Quality proof gate: " + qualityStatus.readinessGate()));
    }

    private static void addFirstLightingPhysicalProofLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus directStage = lightingDispatch.stages().get("direct_lighting");
        LightingDispatchStageTelemetryStatus diffuseGiStage = firstStage(
                lightingDispatch,
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        );
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                lightingDispatch,
                "denoise",
                "diffuse_gi_denoise",
                "denoised_gi",
                "round7_denoise"
        );
        FinalCompositeModeStatus compositeStatus = currentCompositeModeStatus();

        lines.add(Component.literal("Overlay scope: first-lighting source/status proof, not final image quality."));
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            lines.add(Component.literal("Lighting dispatch: unavailable"));
            lines.add(Component.literal("Reason: " + shorten(lightingDispatch.message(), 96)));
            lines.add(Component.literal("Proof boundary: controller must validate screenshots/logs before any physical-lighting claim."));
            return;
        }

        lines.add(Component.literal("Physical source: active=" + yesNoUnknown(physicalSourceActive(directStage))
                + " directNative=" + stageNativeExecutionLabel(directStage)
                + " giNative=" + stageNativeExecutionLabel(diffuseGiStage)));
        lines.add(Component.literal("Scene/surface samples: directCandidates="
                + countOrFallback(stageCandidateCount(directStage), stageSampleCount(directStage))
                + " surface=" + stageSurfaceSampleLabel(directStage)
                + " sections=" + stageSectionCountLabel(directStage)
                + " giSamples=" + valueOrUnknown(stageSampleCount(diffuseGiStage))
                + " giRays=" + valueOrUnknown(stageRayCount(diffuseGiStage))));
        lines.add(Component.literal("Direct output: energy=" + stageOutputEnergyLabel(directStage)
                + " checksum=" + stageOutputChecksumLabel(directStage)
                + " cpu=" + yesNoUnknown(stageCpuOutputGenerated(directStage))));
        lines.add(Component.literal("GI output: energy=" + stageOutputEnergyLabel(diffuseGiStage)
                + " checksum=" + stageOutputChecksumLabel(diffuseGiStage)
                + " denoiseEnergy=" + stageOutputEnergyLabel(denoiseStage)));
        lines.add(Component.literal("Proof guards: metadataOnly(direct/gi/denoise)="
                + stageMetadataOnlyLabel(directStage)
                + "/"
                + stageMetadataOnlyLabel(diffuseGiStage)
                + "/"
                + stageMetadataOnlyLabel(denoiseStage)
                + " focusWindow=" + proofFlagLabel(directStage, diffuseGiStage, denoiseStage, "focus_window_only", "focus_window_source", "focus_only", "focus_window")
                + " temporarySource=" + proofFlagLabel(directStage, diffuseGiStage, denoiseStage, "temporary_direct_source", "temporary_direct_light_source", "temporary_source_ready", "uses_direct_light_payload")));
        lines.add(Component.literal("Surface proof hints: emissiveProximity="
                + giEmissiveProximityLabel(diffuseGiStage)
                + " region=" + giAffectedSurfaceRegionLabel(diffuseGiStage)
                + " hudExcluded=" + giHandHudExcludedLabel(diffuseGiStage)));
        lines.add(Component.literal("Physical GI coupling: samples="
                + giPhysicalSampleLabel(diffuseGiStage)
                + " material=" + giMaterialCouplingLabel(diffuseGiStage)
                + " geometry=" + giGeometryCouplingLabel(diffuseGiStage)));
        lines.add(Component.literal("Physical GI tracing: bounceSource="
                + giBounceSourceLabel(diffuseGiStage)
                + " trace=" + giTracingEvidenceLabel(diffuseGiStage)
                + " controllerProof=pending"));
        lines.add(Component.literal("Proof boundary: " + firstLightingPhysicalProofBoundary(compositeStatus)));
    }

    private static void addShaderDenoiseTemporalLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                lightingDispatch,
                "shader_denoise",
                "edge_aware_denoise",
                "denoise",
                "diffuse_gi_denoise",
                "denoised_gi",
                "round7_denoise"
        );
        LightingDispatchStageTelemetryStatus adaptiveStage = firstStage(
                lightingDispatch,
                "adaptive_sampling",
                "history_confidence",
                "variance",
                "ray_budget"
        );
        lines.add(Component.literal("Overlay scope: shader denoise + temporal proof readiness; no quality claim."));
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            lines.add(Component.literal("Denoise telemetry: unavailable(" + shorten(lightingDispatch.message(), 64) + ")"));
            lines.add(Component.literal("Sources: raw=? cpu=? shaderIntent=? shaderOut=?"));
            lines.add(Component.literal("Shader output attempt: attempted=? generation=? realReady=false noOverclaim=true"));
            lines.add(Component.literal("Shader output prerequisites: dispatch=? image=? material=? generated=? realReady=?"));
            lines.add(Component.literal("Shader output candidate: present=? dims=missing checksum=missing"));
            lines.add(Component.literal("Candidate source: source=? marker=? boundary=telemetry-unavailable"));
            lines.add(Component.literal("Candidate boundary: blocker=telemetry-unavailable notRealShaderOut=true"));
            lines.add(Component.literal("CPU fallback boundary: active=? cpuReady=? cpuGenerated=? candidateOnly=? source=?"));
            lines.add(Component.literal("Denoise output: source=? generated=? energy=missing checksum=missing"));
            lines.add(Component.literal("Edge/history: edgeReject=? edgeKeep=? histReject=?"));
            lines.add(Component.literal("Temporal pixels: stable=? unstable=? f2fDelta=?"));
            lines.add(Component.literal("Temporal quality: historyConf=? flicker=? ghostRisk=? ready=?"));
            lines.add(Component.literal("Temporal proof: accept=? reject=? reset=? missing=? ready=?"));
            lines.add(Component.literal("Proof boundary: controller screenshots/logs required before temporal or shader-quality claim."));
            return;
        }

        lines.add(Component.literal("Sources: raw=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.rawSourceReady())
                + " cpu=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.cpuDenoiseReady())
                + " shaderIntent=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.shaderDenoiseIntended())
                + " shaderOut=" + shaderOutputReadinessLabel(denoiseStage)));
        lines.add(Component.literal("Shader state: dispatch=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.shaderDispatchPrepared())
                + " image=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.shaderOutputImageReady())
                + " material=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.shaderOutputMaterialReady())
                + " generated=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.shaderGeneratedOutput())));
        lines.add(Component.literal("Shader output attempt: attempted=" + shaderDenoiseOutputAttemptedLabel(denoiseStage)
                + " generation=" + valueOrUnknown(denoiseStage == null ? null : denoiseStage.generation())
                + " realReady=" + realShaderDenoiseOutputReadyLabel(denoiseStage)
                + " noOverclaim=" + shaderDenoiseNoOverclaimLabel(denoiseStage)));
        lines.add(Component.literal("Shader output prerequisites: " + shaderOutputPrerequisitesLabel(denoiseStage)));
        lines.add(Component.literal("Shader output candidate: present=" + shaderOutputImageCandidateReadinessLabel(denoiseStage)
                + " dims=" + shaderOutputImageCandidateDimensionsLabel(denoiseStage)
                + " checksum=" + shaderOutputImageCandidateChecksumLabel(denoiseStage)));
        lines.add(Component.literal("Candidate source: source=" + shaderOutputImageCandidateSourceLabel(denoiseStage)
                + " marker=" + shaderOutputImageCandidateMarkerLabel(denoiseStage)
                + " boundary=" + shaderOutputImageCandidateBlockerLabel(denoiseStage)));
        lines.add(Component.literal("Candidate boundary: blocker=" + shaderOutputImageCandidateBlockerLabel(denoiseStage)
                + " notRealShaderOut=" + yesNo(!Boolean.TRUE.equals(denoiseStage == null ? null : denoiseStage.shaderGeneratedOutput()))));
        lines.add(Component.literal("Fallback/blockers: cpuFallback=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.cpuReadbackFallback())
                + " blockers=" + shorten(denoiseStage == null ? "" : denoiseStage.shaderDenoiseBlockers(), 78)));
        lines.add(Component.literal("CPU fallback boundary: " + shaderDenoiseCpuFallbackBoundaryLabel(denoiseStage)));
        lines.add(Component.literal("Shader no-overclaim boundary: " + shaderDenoiseOutputBoundaryLine(denoiseStage)));
        lines.add(Component.literal("Denoise output: source=" + denoiseSourceIdentityLabel(denoiseStage)
                + " generated=" + yesNoUnknown(denoiseStage == null ? null : denoiseStage.cpuOutputGenerated())
                + " energy=" + stageOutputEnergyLabel(denoiseStage)
                + " checksum=" + stageOutputChecksumLabel(denoiseStage)));
        lines.add(Component.literal("Edge/history: edgeReject=" + valueOrUnknown(denoiseStage == null ? null : denoiseStage.edgeRejectionCount())
                + " edgeKeep=" + firstDetailOrUnknown(denoiseStage, "edge_preserved_count", "edge_preserved", "edge_kept")
                + " histReject=" + valueOrUnknown(denoiseStage == null ? null : denoiseStage.historyRejectionCount())));
        lines.add(Component.literal("Temporal pixels: stable=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "stable_pixel_count", "stable_pixels", "temporal_stable_pixels", "history_stable_pixels")
                + " unstable=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "unstable_pixel_count", "unstable_pixels", "temporal_unstable_pixels", "flicker_unstable_pixels")
                + " f2fDelta=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "frame_to_frame_delta", "frame_delta", "f2f_delta", "mean_frame_delta", "temporal_delta")));
        lines.add(Component.literal("Temporal quality: historyConf=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "history_confidence", "avg_history_confidence", "average_history_confidence", "temporal_history_confidence")
                + " flicker=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "flicker_score", "temporal_flicker_score", "frame_flicker_score", "flicker_risk")
                + " ghostRisk=" + temporalDetailOrUnknown(denoiseStage, adaptiveStage, "ghosting_risk", "temporal_ghosting_risk", "ghost_risk", "history_ghosting_risk")
                + " ready=" + temporalReadinessLabel(denoiseStage, adaptiveStage)));
        lines.add(Component.literal("Temporal proof: accept=" + firstDetailOrUnknown(denoiseStage, "history_accepted_count", "history_accepted", "temporal_history_accepted")
                + " reject=" + valueOrUnknown(denoiseStage == null ? null : denoiseStage.historyRejectionCount())
                + " reset=" + firstDetailOrUnknown(denoiseStage, "history_reset_count", "history_reset", "temporal_history_reset")
                + " missing=" + firstDetailOrUnknown(denoiseStage, "missing_history_pixels", "history_missing", "temporal_history_missing")
                + " ready=" + temporalReadinessLabel(denoiseStage, adaptiveStage)));
        lines.add(Component.literal("Temporal line: " + denoiseTemporalStatusLabel(denoiseStage, adaptiveStage)));
        lines.add(Component.literal("Proof boundary: " + denoiseProofBoundaryLabel(denoiseStage)));
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
        lines.add(Component.literal("round7.lightingStackBoundary=" + compositeStatus.lightingStackBoundary()));
        lines.add(Component.literal("round7.finalCompositeBoundary=" + compositeStatus.finalCompositeBoundary()));
        lines.add(Component.literal("round7.firstLightingMilestoneGate="
                + compositeStatus.firstLightingMilestoneGate()));
        FirstLightingQualityProofStatus qualityStatus = FirstLightingQualityProofStatus.fromSnapshot(
                snapshot,
                compositeStatus
        );
        lines.add(Component.literal("round7.firstLightingQualitySummary=" + qualityStatus.summaryLine()));
        qualityStatus.validationFields("round7.firstLightingQuality").entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Component.literal(entry.getKey() + "=" + entry.getValue()))
                .forEach(lines::add);
        lines.add(Component.literal("round7.timing.cpuAvailability="
                + snapshot.frameTimings().cpuTimingAvailabilityLabel()));
        lines.add(Component.literal("round7.timing.gpuAvailability="
                + snapshot.frameTimings().gpuTimingAvailabilityLabel()));
        lines.add(Component.literal("round7.timing.gi=" + snapshot.frameTimings().compactStageTimingLine(
                "GI",
                "diffuse_gi",
                "low_res_gi",
                "low_resolution_gi",
                "gi"
        )));
        lines.add(Component.literal("round7.timing.denoise=" + snapshot.frameTimings().compactStageTimingLine(
                "Denoise",
                "denoise",
                "diffuse_denoise",
                "edge_aware_denoise"
        )));
        lines.add(Component.literal("round7.timing.composite=" + snapshot.frameTimings().compactStageTimingLine(
                "Composite",
                "composite",
                "final_composite"
        )));
        lines.add(Component.literal("round8.timing.adaptive=" + snapshot.frameTimings().compactStageTimingLine(
                "Adaptive",
                "adaptive_sampling",
                "ray_budget",
                "variance",
                "history_confidence"
        )));
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
        Round9CullingRuntimeStatus round9 = Round9CullingRuntimeStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("round9.cullingSummary=" + round9.summary()));
        lines.add(Component.literal("round9.clusterMetadata=" + round9.clusterCountLine()));
        lines.add(Component.literal("round9.visibilityCounts=" + round9.visibilityCountLine()));
        lines.add(Component.literal("round9.culling=" + round9.cullingModeLine()));
        lines.add(Component.literal("round9.indirectDrawList=" + round9.indirectDrawCountLine()));
        lines.add(Component.literal("round9.upload=" + round9.uploadLine()));
        lines.add(Component.literal("round9.generation=" + round9.generationLine()));
        lines.add(Component.literal("round9.terrainRendering=" + round9.terrainRenderingLine()));
        lines.add(Component.literal("round9.valueGuard=" + round9.invalidOrZeroLine()));
        lines.add(Component.literal("round9.readiness=" + round9.readinessLine()));
        lines.add(Component.literal("round9.evidenceBoundary=" + round9.evidenceBoundaryLine()));
        Round10HybridHitDebugStatus round10 = Round10HybridHitDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("round10.hybridHitSummary=" + round10.summary()));
        lines.add(Component.literal("round10.sourceCounts=" + round10.sourceCountsLine()));
        lines.add(Component.literal("round10.priority=" + round10.priorityLine()));
        lines.add(Component.literal("round10.materialConsistency=" + round10.materialConsistencyLine()));
        lines.add(Component.literal("round10.fallback=" + round10.fallbackLine()));
        lines.add(Component.literal("round10.readiness=" + round10.readinessLine()));
        lines.add(Component.literal("round10.evidenceBoundary=" + round10.evidenceBoundaryLine()));
        lines.add(Component.literal("round11.restirSummary=" + roundElevenSummary(snapshot)));
        addRoundElevenValidationLines(lines, snapshot);
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
        lines.add(Component.literal("Timing availability: " + snapshot.frameTimings().compactAvailabilityLine()));
        lines.add(Component.literal("Timing proof boundary: " + snapshot.frameTimings().measurementBoundaryLabel()));
        lines.add(Component.literal("Native pass timing: " + snapshot.nativePassStates().compactTimingBoundaryLabel()));
        addLightingStageTimingSummaryLines(lines, snapshot);
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
        } else {
            lines.add(Component.literal("GPU total: unavailable (native/Vulkan timestamp queries not wired)."));
        }
        if (snapshot.activeCpuScopeCount() > 0) {
            lines.add(Component.literal("Active CPU scopes: " + String.join(", ", snapshot.frameTimings().activeCpuScopeNames())));
        }
    }

    private static void addLightingStageTimingSummaryLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        for (String timingLine : snapshot.frameTimings().compactLightingStageTimingLines()) {
            lines.add(Component.literal("Stage timing: " + timingLine));
        }
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        if (!lightingDispatch.hasStageStatuses()) {
            lines.add(Component.literal("Native stage timing: unavailable(" + shorten(lightingDispatch.message(), 64) + ")"));
            return;
        }
        for (LightingDispatchStageTelemetryStatus stage : lightingDispatch.stages().values()) {
            lines.add(Component.literal("Native stage timing: " + stage.compactTimingBoundaryLine()));
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
        addTemporalStageBoundaryLines(lines, snapshot);
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

    private static void addChunkCullingLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round9CullingRuntimeStatus status = Round9CullingRuntimeStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 9 culling status; GPU fields only count when reported true."));
        lines.add(Component.literal("GPU culling: executed=" + round9GpuCullingExecuted(snapshot)
                + " prereq=" + round9GpuPrerequisitesReady(snapshot)
                + " blocker=" + round9GpuBlockerReason(snapshot)));
        lines.add(Component.literal("Frustum: candidates=" + round9FrustumCandidates(snapshot)
                + " | clusters=" + status.clusterCountLine()));
        lines.add(Component.literal("Occlusion: placeholder=" + round9OcclusionPlaceholder(snapshot)
                + " candidates=" + round9OcclusionCandidates(snapshot)));
        lines.add(Component.literal("Indirect draws: ready=" + round9IndirectDrawReady(snapshot)
                + " count=" + round9IndirectDrawCount(snapshot)
                + " | " + status.indirectDrawCountLine()));
        lines.add(Component.literal("Visibility: visible=" + round9VisibleCount(snapshot)
                + " culled=" + round9CulledCount(snapshot)
                + " offscreen=" + round9OffscreenCount(snapshot)
                + " | " + status.visibilityCountLine()));
        lines.add(Component.literal("Frame timing: cpu=" + round9CpuTiming(snapshot)
                + " gpu=" + round9GpuTiming(snapshot)
                + " queue=" + round9FrameQueueTiming(snapshot)));
        lines.add(Component.literal("Density: " + round9DensityLabel(snapshot)));
        lines.add(Component.literal(status.cullingModeLine()));
        lines.add(Component.literal(status.terrainRenderingLine()));
        lines.add(Component.literal(status.invalidOrZeroLine()));
        lines.add(Component.literal(status.readinessLine()));
        lines.add(Component.literal(status.evidenceBoundaryLine()));
    }

    private static String round9GpuCullingExecuted(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "false",
                "round9_gpu_culling_executed",
                "gpu_culling_executed",
                "gpuCullingExecuted",
                "gpu_culling_real",
                "gpuCullingReal"
        );
    }

    private static String round9GpuPrerequisitesReady(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_gpu_prerequisites_ready",
                "gpu_prerequisites_ready",
                "gpuPrerequisitesReady",
                "gpu_culling_prerequisites_ready",
                "gpuCullingPrerequisitesReady"
        );
    }

    private static String round9GpuBlockerReason(LucernaStatusSnapshot snapshot) {
        return shorten(round9NativeValue(
                snapshot,
                "unreported",
                "round9_gpu_blocker_reason",
                "gpu_blocker_reason",
                "gpuCullingBlockerReason",
                "gpu_culling_blocker",
                "culling_blocker_reason",
                "blocker_reason"
        ), 72);
    }

    private static String round9FrustumCandidates(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_frustum_candidate_count",
                "frustum_candidate_count",
                "frustumCandidates",
                "frustum_candidates",
                "frustum_visible_candidates"
        );
    }

    private static String round9OcclusionPlaceholder(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_occlusion_placeholder",
                "occlusion_placeholder",
                "occlusionPlaceholder",
                "occlusion_culling_placeholder",
                "gpu_occlusion_placeholder"
        );
    }

    private static String round9OcclusionCandidates(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_occlusion_candidate_count",
                "occlusion_candidate_count",
                "occlusionCandidates",
                "occlusion_candidates",
                "occlusion_test_candidates"
        );
    }

    private static String round9IndirectDrawReady(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_indirect_draw_ready",
                "indirect_draw_ready",
                "indirectDrawReady",
                "indirect_ready",
                "real_indirect_draw_ready"
        );
    }

    private static String round9IndirectDrawCount(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_indirect_draw_count",
                "indirect_draw_count",
                "indirectDrawCount",
                "real_indirect_draw_count",
                "draw_count"
        );
    }

    private static String round9VisibleCount(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_visible_count",
                "visible_count",
                "visibleCount",
                "visible_cluster_count",
                "visible_chunk_count"
        );
    }

    private static String round9CulledCount(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_culled_count",
                "culled_count",
                "culledCount",
                "culled_cluster_count",
                "culled_chunk_count"
        );
    }

    private static String round9OffscreenCount(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "?",
                "round9_offscreen_count",
                "offscreen_count",
                "offscreenCount",
                "offscreen_cluster_count",
                "offscreen_chunk_count"
        );
    }

    private static String round9CpuTiming(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "pending",
                "round9_cpu_ms",
                "round9_cpu_millis",
                "culling_cpu_ms",
                "cpu_ms",
                "cpuMillis"
        );
    }

    private static String round9GpuTiming(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "pending",
                "round9_gpu_ms",
                "round9_gpu_millis",
                "culling_gpu_ms",
                "gpu_ms",
                "gpuMillis"
        );
    }

    private static String round9FrameQueueTiming(LucernaStatusSnapshot snapshot) {
        return round9NativeValue(
                snapshot,
                "pending",
                "round9_frame_queue_ms",
                "frame_queue_ms",
                "queue_ms",
                "queueMillis",
                "latency_ms"
        );
    }

    private static String round9DensityLabel(LucernaStatusSnapshot snapshot) {
        return shorten(round9NativeValue(
                snapshot,
                "unreported",
                "round9_density_label",
                "density_label",
                "forest_density_label",
                "complex_density_label",
                "terrain_density_label",
                "scene_density_label"
        ), 72);
    }

    private static String round9NativeValue(LucernaStatusSnapshot snapshot, String fallback, String... keys) {
        if (snapshot == null || keys == null) {
            return fallback;
        }
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        for (String key : keys) {
            String value = nativeStatusValue(nativeStatus, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    private static void addRoundTenVoxelRayLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round10HybridHitDebugStatus status = Round10HybridHitDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 10 voxel traversal CPU metadata/debug status."));
        lines.add(Component.literal("Voxel ray debug visible: yes | source=native round10_voxel_traversal status"));
        addRoundTenTraversalValidationLines(lines, snapshot);
        addRoundTenStressLines(lines, snapshot);
        lines.add(Component.literal("Round 10 hybrid hits: " + status.summary()));
        lines.add(Component.literal("Round 10 source counts: " + status.sourceCountsLine()));
        lines.add(Component.literal("Round 10 readiness: " + status.readinessLine()));
        lines.add(Component.literal("Round 10 boundary: " + status.evidenceBoundaryLine()));
    }

    private static void addRoundTenRtEntityLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round10HybridHitDebugStatus status = Round10HybridHitDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 10 Vulkan RT entity path status."));
        lines.add(Component.literal("RT entity debug visible: yes | BLAS/TLAS status is fallback-safe until native RT telemetry exists."));
        addRoundTenTraversalValidationLines(lines, snapshot);
        addRoundTenStressLines(lines, snapshot);
        lines.add(Component.literal("Round 10 fallback: " + status.fallbackLine()));
        lines.add(Component.literal("Round 10 priority: " + status.priorityLine()));
        lines.add(Component.literal("Round 10 material: " + status.materialConsistencyLine()));
        lines.add(Component.literal("Round 10 boundary: " + status.evidenceBoundaryLine()));
    }

    private static void addRoundTenHybridHitLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        Round10HybridHitDebugStatus status = Round10HybridHitDebugStatus.fromSnapshot(snapshot);
        lines.add(Component.literal("Overlay scope: Round 10 hybrid hit resolver status."));
        lines.add(Component.literal("Hybrid hit debug visible: yes | " + status.summary()));
        addRoundTenTraversalValidationLines(lines, snapshot);
        addRoundTenStressLines(lines, snapshot);
        lines.add(Component.literal("Hybrid source counts: " + status.sourceCountsLine()));
        lines.add(Component.literal("Hybrid priority: " + status.priorityLine()));
        lines.add(Component.literal("Hybrid material consistency: " + status.materialConsistencyLine()));
        lines.add(Component.literal("Hybrid fallback: " + status.fallbackLine()));
        lines.add(Component.literal("Round 10 evidence boundary: " + status.evidenceBoundaryLine()));
    }

    private static void addRoundTenTraversalValidationLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("R10 hits: wall=" + roundTenTraversalValue(
                snapshot,
                "known_scene_wall_hit_count",
                "known_scene_wall_hits",
                "traversal_known_scene_wall_hit_count",
                "wall_hit_count",
                "wall_hits"
        ) + " openSkyMiss=" + roundTenTraversalValue(
                snapshot,
                "open_sky_miss_count",
                "open_sky_misses",
                "traversal_open_sky_miss_count",
                "sky_miss_count",
                "sky_misses"
        ) + " glassWater=" + roundTenTraversalValue(
                snapshot,
                "glass_water_material_hit_count",
                "glass_water_material_hits",
                "traversal_glass_water_material_hit_count",
                "water_glass_material_hits",
                "translucent_material_hits"
        ) + " opaque=" + roundTenTraversalValue(
                snapshot,
                "opaque_material_hit_count",
                "opaque_material_hits",
                "traversal_opaque_material_hit_count",
                "opaque_hits",
                "material_hit_count"
        )));
        lines.add(Component.literal("R10 readiness: maskBits=" + roundTenTraversalReadyValue(
                snapshot,
                "mask_bits_ready",
                "traversal_mask_bits_ready",
                "occupancy_mask_bits_ready"
        ) + " source=" + shorten(roundTenTraversalValue(
                snapshot,
                "mask_bit_source",
                "traversal_mask_bit_source",
                "occupancy_mask_source",
                "mask_source"
        ), 44) + " materialLookup=" + roundTenTraversalReadyValue(
                snapshot,
                "material_lookup_ready",
                "traversal_material_lookup_ready",
                "palette_lookup_ready"
        ) + " emptySkipSafe=" + roundTenTraversalValue(
                snapshot,
                "empty_section_skip_safety_count",
                "traversal_empty_section_skip_safety_count",
                "empty_section_skips",
                "skipped_sections"
        )));
        lines.add(Component.literal("R10 backend: traversal=" + shorten(roundTenTraversalValue(
                snapshot,
                "backend",
                "traversal_backend",
                "voxel_traversal_backend"
        ), 54) + " realGpuTraversalExecuted=" + roundTenRealGpuTraversalExecuted(snapshot)));
    }

    private static void addRoundTenStressLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        lines.add(Component.literal("R10 stress: entityMove=" + roundTenTraversalReadyValue(
                snapshot,
                "entity_movement_marker",
                "entityMovementMarker",
                "entity_movement_seen",
                "entityMovementSeen",
                "moving_entity_marker",
                "movingEntityMarker",
                "entity_churn_marker",
                "entityChurnMarker"
        ) + " chunkChurn=" + roundTenTraversalReadyValue(
                snapshot,
                "chunk_churn_marker",
                "chunkChurnMarker",
                "chunk_churn_seen",
                "chunkChurnSeen",
                "chunk_load_unload_marker",
                "chunkLoadUnloadMarker"
        ) + " sectionLife=" + roundTenTraversalReadyValue(
                snapshot,
                "section_lifecycle_marker",
                "sectionLifecycleMarker",
                "section_lifecycle_seen",
                "sectionLifecycleSeen",
                "section_load_unload_marker",
                "sectionLoadUnloadMarker"
        ) + " worldLeave=" + roundTenTraversalReadyValue(
                snapshot,
                "world_leave_shutdown_marker",
                "worldLeaveShutdownMarker",
                "world_leave_marker",
                "worldLeaveMarker",
                "shutdown_marker",
                "shutdownMarker",
                "world_shutdown_marker",
                "worldShutdownMarker"
        )));
        lines.add(Component.literal("R10 stress detail: entity=" + shorten(roundTenTraversalValue(
                snapshot,
                "entity_movement_detail",
                "entityMovementDetail",
                "entity_movement_status",
                "entityMovementStatus",
                "entity_movement_reason",
                "entityMovementReason"
        ), 32) + " chunk=" + shorten(roundTenTraversalValue(
                snapshot,
                "chunk_churn_detail",
                "chunkChurnDetail",
                "chunk_churn_status",
                "chunkChurnStatus",
                "chunk_churn_reason",
                "chunkChurnReason"
        ), 32) + " section=" + shorten(roundTenTraversalValue(
                snapshot,
                "section_lifecycle_detail",
                "sectionLifecycleDetail",
                "section_lifecycle_status",
                "sectionLifecycleStatus",
                "section_lifecycle_reason",
                "sectionLifecycleReason"
        ), 32)));
        lines.add(Component.literal("R10 GPU/RT boundary: realGpuTraversal="
                + roundTenRealGpuTraversalExecuted(snapshot)
                + " realHardwareRt=" + roundTenTraversalReadyValue(
                snapshot,
                "real_hardware_rt_executed",
                "realHardwareRtExecuted",
                "hardware_rt_executed",
                "hardwareRtExecuted",
                "rt_hardware_acceleration_executed",
                "rtHardwareAccelerationExecuted"
        ) + " rtBackend=" + shorten(roundTenTraversalValue(
                snapshot,
                "rt_backend",
                "rtBackend",
                "rt_entity_backend",
                "rtEntityBackend",
                "hardware_rt_backend",
                "hardwareRtBackend",
                "blas_tlas_backend",
                "blasTlasBackend"
        ), 30) + " blocker=" + shorten(roundTenTraversalValue(
                snapshot,
                "rt_blocker_reason",
                "rtBlockerReason",
                "hardware_rt_blocker_reason",
                "hardwareRtBlockerReason",
                "gpu_rt_blocker",
                "gpuRtBlocker",
                "rt_boundary",
                "rtBoundary"
        ), 42)));
    }

    private static void addRoundElevenDirectReservoirLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchStageTelemetryStatus stage = roundElevenDirectStage(snapshot);
        lines.add(Component.literal("Overlay scope: Round 11 ReSTIR DI reservoir metadata/status."));
        lines.add(Component.literal("Direct reservoir debug visible: yes | source=native round11_restir metadata/status."));
        lines.add(Component.literal("Direct reservoirs: count=" + roundElevenNativeValue(snapshot, "direct_reservoir_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "direct_reservoir_count", "reservoir_count"))));
        lines.add(Component.literal("Direct candidates: count=" + roundElevenNativeValue(snapshot, "candidate_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "candidate_count", "selected_candidate_count"))));
        lines.add(Component.literal("Direct reuse: temporal=" + roundElevenNativeValue(snapshot, "temporal_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "temporal_reuse_count"))
                + " spatial=" + roundElevenNativeValue(snapshot, "spatial_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "spatial_reuse_count"))));
        lines.add(Component.literal("Direct confidence: " + roundElevenNativeConfidenceLine(snapshot, stage)));
        lines.add(Component.literal("Direct readiness: " + roundElevenReadinessLine(stage)));
        lines.add(Component.literal("Round 11 boundary: reservoir metadata/debug status only; no ReSTIR lighting quality claim."));
    }

    private static void addRoundElevenGiReservoirLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchStageTelemetryStatus stage = roundElevenGiStage(snapshot);
        lines.add(Component.literal("Overlay scope: Round 11 ReSTIR GI/path reservoir metadata/status."));
        lines.add(Component.literal("GI reservoir debug visible: yes | source=native round11_restir metadata/status."));
        lines.add(Component.literal("GI reservoirs: count=" + roundElevenNativeValue(snapshot, "gi_reservoir_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "gi_reservoir_count", "reservoir_count"))));
        lines.add(Component.literal("GI path candidates: count=" + roundElevenNativeValue(snapshot, "candidate_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "candidate_count", "path_candidate_count"))));
        lines.add(Component.literal("GI path reuse: count=" + roundElevenNativeValue(snapshot, "path_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "path_reuse_count"))));
        lines.add(Component.literal("GI invalidation: count=" + roundElevenNativeValue(snapshot, "invalidated_reservoir_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "invalidated_reservoir_count", "invalidation_count"))));
        lines.add(Component.literal("GI confidence: " + roundElevenNativeConfidenceLine(snapshot, stage)));
        lines.add(Component.literal("Round 11 boundary: GI/PT reservoir contracts only; physical path reuse execution not implied."));
    }

    private static void addRoundElevenReservoirReuseLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchStageTelemetryStatus directStage = roundElevenDirectStage(snapshot);
        LightingDispatchStageTelemetryStatus giStage = roundElevenGiStage(snapshot);
        LightingDispatchStageTelemetryStatus reuseStage = roundElevenReuseStage(snapshot);
        lines.add(Component.literal("Overlay scope: Round 11 temporal/spatial/path reuse counters."));
        lines.add(Component.literal("Reservoir reuse debug visible: yes | native round11_restir status plus direct+GI details."));
        lines.add(Component.literal("Temporal reuse: count=" + roundElevenNativeValue(snapshot, "temporal_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage, directStage, giStage}, "temporal_reuse_count"))));
        lines.add(Component.literal("Spatial reuse: count=" + roundElevenNativeValue(snapshot, "spatial_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage, directStage, giStage}, "spatial_reuse_count"))));
        lines.add(Component.literal("Path reuse: count=" + roundElevenNativeValue(snapshot, "path_reuse_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage, giStage}, "path_reuse_count"))));
        lines.add(Component.literal("Invalidation: count=" + roundElevenNativeValue(snapshot, "invalidated_reservoir_count", firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage, giStage}, "invalidated_reservoir_count", "invalidation_count"))));
        lines.add(Component.literal("Confidence: " + roundElevenNativeConfidenceLine(snapshot, reuseStage, directStage, giStage)));
        lines.add(Component.literal("Round 11 boundary: reuse telemetry/status only; shader/native reservoir execution still requires controller proof."));
    }

    private static void addRoundElevenRestirExecutionLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchStageTelemetryStatus directStage = roundElevenDirectStage(snapshot);
        LightingDispatchStageTelemetryStatus giStage = roundElevenGiStage(snapshot);
        LightingDispatchStageTelemetryStatus reuseStage = roundElevenReuseStage(snapshot);
        lines.add(Component.literal("Overlay scope: Round 11 ReSTIR execution proof, not reservoir metadata alone."));
        lines.add(Component.literal("ReSTIR DI execution: " + roundElevenRealRestirDiExecutionLine(snapshot, directStage)));
        lines.add(Component.literal("Selected reservoirs/candidates: " + roundElevenSelectedReservoirCandidateLine(
                snapshot,
                directStage,
                giStage
        )));
        lines.add(Component.literal("Candidate reduction: " + roundElevenReductionRatioLine(snapshot, directStage)));
        lines.add(Component.literal("Temporal/spatial reuse: " + roundElevenReuseExecutionLine(
                snapshot,
                reuseStage,
                directStage,
                giStage
        )));
        lines.add(Component.literal("Output evidence: " + roundElevenOutputEvidenceLine(
                snapshot,
                directStage,
                giStage,
                reuseStage
        )));
        lines.add(Component.literal("GI path reuse execution boundary: " + roundElevenGiPathReuseBoundaryLine(
                snapshot,
                giStage
        )));
        lines.add(Component.literal("Stability boundary: " + roundElevenStabilityBoundaryLine(snapshot, reuseStage)));
    }

    private static void addRoundElevenValidationLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchStageTelemetryStatus directStage = roundElevenDirectStage(snapshot);
        LightingDispatchStageTelemetryStatus giStage = roundElevenGiStage(snapshot);
        LightingDispatchStageTelemetryStatus reuseStage = roundElevenReuseStage(snapshot);
        lines.add(Component.literal("round11.directReservoir=" + roundElevenReservoirCountLine(directStage)));
        lines.add(Component.literal("round11.directCandidate=" + roundElevenCandidateCountLine(directStage)));
        lines.add(Component.literal("round11.giReservoir=" + roundElevenReservoirCountLine(giStage)));
        lines.add(Component.literal("round11.giCandidate=" + roundElevenCandidateCountLine(giStage)));
        lines.add(Component.literal("round11.temporalReuse=" + roundElevenTemporalReuseCountLine(reuseStage, directStage, giStage)));
        lines.add(Component.literal("round11.spatialReuse=" + roundElevenSpatialReuseCountLine(reuseStage, directStage, giStage)));
        lines.add(Component.literal("round11.pathReuse=" + roundElevenPathReuseCountLine(reuseStage, giStage)));
        lines.add(Component.literal("round11.invalidation=" + roundElevenInvalidationLine(reuseStage, giStage)));
        lines.add(Component.literal("round11.confidence=" + roundElevenConfidenceLine(reuseStage, directStage, giStage)));
        lines.add(Component.literal("round11.realRestirDiExecution="
                + roundElevenBooleanLabel(roundElevenRealRestirDiExecutionValue(snapshot, directStage))));
        lines.add(Component.literal("round11.selectedReservoirsCandidates="
                + roundElevenSelectedReservoirCandidateLine(snapshot, directStage, giStage)));
        lines.add(Component.literal("round11.reductionRatio=" + roundElevenReductionRatioLine(snapshot, directStage)));
        lines.add(Component.literal("round11.reuseExecution="
                + roundElevenReuseExecutionLine(snapshot, reuseStage, directStage, giStage)));
        lines.add(Component.literal("round11.outputEvidence="
                + roundElevenOutputEvidenceLine(snapshot, directStage, giStage, reuseStage)));
        lines.add(Component.literal("round11.giPathReuseExecutionBoundary="
                + roundElevenGiPathReuseBoundaryLine(snapshot, giStage)));
        lines.add(Component.literal("round11.stabilityBoundary="
                + roundElevenStabilityBoundaryLine(snapshot, reuseStage)));
        lines.add(Component.literal("round11.evidenceBoundary="
                + roundElevenExecutionEvidenceBoundaryLine(snapshot, directStage)));
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
        lines.add(Component.literal("Native pass timing: " + snapshot.nativePassStates().compactTimingBoundaryLabel()));
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
        lines.add(Component.literal("Round 7 stack boundary: " + compositeStatus.lightingStackBoundary()));
        lines.add(Component.literal("Round 7 final boundary: " + compositeStatus.finalCompositeBoundary()));
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
            lines.add(Component.literal("Native scene-tied GI CPU/readback: source=" + giOutputSourceLabel(diffuseGiStage)
                    + " generated=" + yesNoUnknown(diffuseGiStage.cpuOutputGenerated())
                    + " size=" + valueOrUnknown(diffuseGiStage.outputDimensions())
                    + " pixels=" + valueOrUnknown(diffuseGiStage.outputPixelCount())));
            lines.add(Component.literal("Native scene-tied GI evidence: energy=" + evidenceValueLabel(diffuseGiStage.outputEnergy())
                    + " checksum=" + evidenceValueLabel(diffuseGiStage.outputChecksum())
                    + " temporaryDirectSource=" + giTemporaryDirectSourceLabel(diffuseGiStage)));
            lines.add(Component.literal("Low-res GI proof hints: emissiveProximity="
                    + giEmissiveProximityLabel(diffuseGiStage)
                    + " surfaceRegion=" + giAffectedSurfaceRegionLabel(diffuseGiStage)
                    + " handHudExcluded=" + giHandHudExcludedLabel(diffuseGiStage)
                    + " surfaceOnlyEligible=" + giSurfaceOnlyProofEligibleLabel(diffuseGiStage)));
            lines.add(Component.literal("Native GI boundary: sceneTiedCpuReadback="
                    + giAuthenticOutputLabel(diffuseGiStage)
                    + " shaderGI=" + realGiShaderLabel(diffuseGiStage)
                    + " physicalTracingQuality=open"));
            lines.add(Component.literal("Low-res GI authenticity: cpuScaffold="
                    + giCpuScaffoldOutputLabel(diffuseGiStage)
                    + " realShaderGI=" + realGiShaderLabel(diffuseGiStage)
                    + " sourceAuthentic=" + giAuthenticOutputLabel(diffuseGiStage)));
            lines.add(Component.literal("Diffuse GI readiness reason: " + readinessReason(diffuseGiStage)));
        }

        if (denoiseStage == null) {
            lines.add(Component.literal("CPU/readback denoise: not reported; shader denoise state unknown"));
        } else {
            lines.add(Component.literal("CPU/readback denoise source: " + denoiseSourceLabel(denoiseStage)
                    + " cpuFallback=" + cpuDenoiseFallbackLabel(denoiseStage)
                    + " shaderDenoise=" + realDenoiseShaderLabel(denoiseStage)));
            lines.add(Component.literal("CPU/readback denoise evidence: generated="
                    + yesNoUnknown(denoiseStage.cpuOutputGenerated())
                    + " size=" + valueOrUnknown(denoiseStage.outputDimensions())
                    + " energy=" + evidenceValueLabel(denoiseStage.outputEnergy())
                    + " checksum=" + evidenceValueLabel(denoiseStage.outputChecksum())));
            lines.add(Component.literal("Denoise boundary: cpuReadback="
                    + cpuDenoiseFallbackLabel(denoiseStage)
                    + " shaderDenoise=" + realDenoiseShaderLabel(denoiseStage)
                    + " shaderQualityGate=open"));
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

    private static String roundElevenSummary(LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        if (!lightingDispatch.hasLightingDispatchStatus()) {
            return "not_reported";
        }
        return "direct=" + stageSummary(roundElevenDirectStage(snapshot))
                + " gi=" + stageSummary(roundElevenGiStage(snapshot))
                + " reuse=" + stageSummary(roundElevenReuseStage(snapshot));
    }

    private static LightingDispatchStageTelemetryStatus roundElevenDirectStage(LucernaStatusSnapshot snapshot) {
        return firstStage(
                snapshot.lightingDispatchStatus(),
                "restir_direct",
                "direct_restir",
                "restir_di",
                "direct_reservoir",
                "direct_reservoir_debug",
                "round11_direct_reservoir",
                "round11_restir_direct"
        );
    }

    private static LightingDispatchStageTelemetryStatus roundElevenGiStage(LucernaStatusSnapshot snapshot) {
        return firstStage(
                snapshot.lightingDispatchStatus(),
                "restir_gi",
                "gi_restir",
                "gi_reservoir",
                "path_reservoir",
                "gi_reservoir_debug",
                "round11_gi_reservoir",
                "round11_restir_gi"
        );
    }

    private static LightingDispatchStageTelemetryStatus roundElevenReuseStage(LucernaStatusSnapshot snapshot) {
        return firstStage(
                snapshot.lightingDispatchStatus(),
                "restir_reuse",
                "reservoir_reuse",
                "reservoir_reuse_debug",
                "round11_reservoir_reuse",
                "round11_restir_reuse"
        );
    }

    private static String roundElevenReservoirCountLine(LightingDispatchStageTelemetryStatus... stages) {
        return "count=" + firstRoundElevenDetail(
                stages,
                "reservoir_count",
                "reservoirs",
                "direct_reservoir_count",
                "gi_reservoir_count",
                "path_reservoir_count",
                "round11_reservoir_count"
        );
    }

    private static String roundElevenCandidateCountLine(LightingDispatchStageTelemetryStatus... stages) {
        String candidateCount = firstRoundElevenDetail(
                stages,
                "candidate_count",
                "candidates",
                "selected_candidate_count",
                "selected_candidates",
                "direct_candidate_count",
                "gi_candidate_count",
                "path_candidate_count",
                "round11_candidate_count"
        );
        if (!"?".equals(candidateCount)) {
            return "count=" + candidateCount;
        }
        for (LightingDispatchStageTelemetryStatus stage : stages) {
            if (stage != null && stage.candidateCount() != null) {
                return "count=" + stage.candidateCount();
            }
        }
        return "count=?";
    }

    private static String roundElevenReuseCountLine(LightingDispatchStageTelemetryStatus... stages) {
        return "temporal=" + firstRoundElevenDetail(stages, "temporal_reuse_count", "temporal_reuse", "temporal_reused", "accepted_temporal_reuse")
                + " spatial=" + firstRoundElevenDetail(stages, "spatial_reuse_count", "spatial_reuse", "spatial_reused", "accepted_spatial_reuse")
                + " rejected=" + firstRoundElevenDetail(stages, "reuse_rejected_count", "reuse_rejected", "rejected_reuse");
    }

    private static String roundElevenTemporalReuseCountLine(LightingDispatchStageTelemetryStatus... stages) {
        return "count=" + firstRoundElevenDetail(
                stages,
                "temporal_reuse_count",
                "temporal_reuse",
                "temporal_reused",
                "accepted_temporal_reuse",
                "temporal_reuse_accepted"
        );
    }

    private static String roundElevenSpatialReuseCountLine(LightingDispatchStageTelemetryStatus... stages) {
        return "count=" + firstRoundElevenDetail(
                stages,
                "spatial_reuse_count",
                "spatial_reuse",
                "spatial_reused",
                "accepted_spatial_reuse",
                "spatial_reuse_accepted"
        );
    }

    private static String roundElevenPathReuseCountLine(LightingDispatchStageTelemetryStatus... stages) {
        return "count=" + firstRoundElevenDetail(
                stages,
                "path_reuse_count",
                "path_reuse",
                "path_reused",
                "accepted_path_reuse",
                "gi_path_reuse_count",
                "restir_pt_path_reuse_count"
        );
    }

    private static String roundElevenInvalidationLine(LightingDispatchStageTelemetryStatus... stages) {
        return "count=" + firstRoundElevenDetail(
                stages,
                "invalidation_count",
                "invalidations",
                "invalidated_reservoirs",
                "invalidated_reservoir_count",
                "dirty_invalidations",
                "global_invalidations"
        ) + " reason=" + firstRoundElevenDetail(
                stages,
                "invalidation_reason",
                "invalidation",
                "dirty_reason",
                "reuse_invalidation_reason"
        );
    }

    private static String roundElevenConfidenceLine(LightingDispatchStageTelemetryStatus... stages) {
        return "min=" + firstRoundElevenDetail(stages, "min_confidence", "confidence_min", "reservoir_confidence_min")
                + " mean=" + firstRoundElevenDetail(stages, "mean_confidence", "avg_confidence", "average_confidence", "confidence", "combined_confidence")
                + " max=" + firstRoundElevenDetail(stages, "max_confidence", "confidence_max", "reservoir_confidence_max");
    }

    private static String roundElevenRealRestirDiExecutionLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        String execution = roundElevenRealRestirDiExecutionValue(snapshot, stage);
        String metadataOnly = roundElevenBooleanLabel(roundElevenMetadataOnlyValue(snapshot, stage));
        String source = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "source_marker", "source"),
                "source_marker",
                "source",
                "source_label"
        );
        String boundary = roundElevenExecutionEvidenceBoundaryLine(snapshot, stage);
        if (isTruthy(execution)) {
            return "realRestirDiExecution=true | first bounded CPU/native preview | source="
                    + source
                    + " | boundary="
                    + boundary;
        }
        return "realRestirDiExecution=false | metadataOnly="
                + metadataOnly
                + " | source="
                + source
                + " | boundary="
                + boundary;
    }

    private static String roundElevenRealRestirDiExecutionValue(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        return roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{stage},
                        "real_restir_di_execution",
                        "real_restir_execution",
                        "realRestirDiExecution",
                        "realRestirExecution"
                ),
                "real_restir_di_execution",
                "real_restir_execution",
                "realRestirDiExecution",
                "realRestirExecution"
        );
    }

    private static String roundElevenSelectedReservoirCandidateLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus directStage,
            LightingDispatchStageTelemetryStatus giStage
    ) {
        String directReservoirs = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{directStage},
                        "direct_reservoir_count",
                        "reservoir_count"
                ),
                "direct_reservoir_count",
                "reservoir_count"
        );
        String giReservoirs = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{giStage},
                        "gi_reservoir_count",
                        "path_reservoir_count",
                        "reservoir_count"
                ),
                "gi_reservoir_count",
                "path_reservoir_count"
        );
        String selected = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{directStage, giStage},
                        "selected_candidate_count",
                        "selected_candidates",
                        "selected_light_count"
                ),
                "selected_candidate_count",
                "selected_candidates",
                "selected_light_count"
        );
        String candidates = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{directStage, giStage},
                        "candidate_count",
                        "candidates",
                        "round11_candidate_count"
                ),
                "candidate_count",
                "candidates",
                "round11_candidate_count"
        );
        return "directReservoirs="
                + directReservoirs
                + " giReservoirs="
                + giReservoirs
                + " selected="
                + selected
                + " candidates="
                + candidates;
    }

    private static String roundElevenReductionRatioLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        String reportedRatio = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{stage},
                        "reduction_ratio",
                        "candidate_reduction_ratio",
                        "selected_candidate_ratio"
                ),
                "reduction_ratio",
                "candidate_reduction_ratio",
                "selected_candidate_ratio"
        );
        if (!"?".equals(reportedRatio)) {
            return "reported=" + reportedRatio;
        }
        String candidates = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "candidate_count", "candidates"),
                "candidate_count",
                "candidates"
        );
        String selected = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{stage},
                        "selected_candidate_count",
                        "selected_candidates",
                        "selected_light_count"
                ),
                "selected_candidate_count",
                "selected_candidates",
                "selected_light_count"
        );
        Double candidateValue = parseDoubleOrNull(candidates);
        Double selectedValue = parseDoubleOrNull(selected);
        if (candidateValue != null && selectedValue != null && candidateValue > 0.0D) {
            double ratio = selectedValue / candidateValue;
            double reductionPercent = Math.max(0.0D, 100.0D - (ratio * 100.0D));
            return "selected/candidates="
                    + formatRatio(ratio)
                    + " candidateReduction="
                    + formatPercent(reductionPercent)
                    + " selected="
                    + selected
                    + " candidates="
                    + candidates;
        }
        return "selected/candidates=? selected=" + selected + " candidates=" + candidates;
    }

    private static String roundElevenReuseExecutionLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus reuseStage,
            LightingDispatchStageTelemetryStatus directStage,
            LightingDispatchStageTelemetryStatus giStage
    ) {
        String temporal = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{reuseStage, directStage, giStage},
                        "temporal_reuse_count",
                        "temporal_reuse"
                ),
                "temporal_reuse_count",
                "temporal_reuse"
        );
        String spatial = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{reuseStage, directStage, giStage},
                        "spatial_reuse_count",
                        "spatial_reuse"
                ),
                "spatial_reuse_count",
                "spatial_reuse"
        );
        String path = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{reuseStage, giStage},
                        "path_reuse_count",
                        "path_reuse"
                ),
                "path_reuse_count",
                "path_reuse"
        );
        String invalidated = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{reuseStage, giStage},
                        "invalidated_reservoir_count",
                        "invalidation_count"
                ),
                "invalidated_reservoir_count",
                "invalidation_count"
        );
        String execution = isTruthy(roundElevenRealRestirDiExecutionValue(snapshot, directStage))
                ? "first-bounded-native-preview"
                : "metadata-only";
        return "temporal="
                + temporal
                + " spatial="
                + spatial
                + " path="
                + path
                + " invalidated="
                + invalidated
                + " execution="
                + execution;
    }

    private static String roundElevenOutputEvidenceLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus directStage,
            LightingDispatchStageTelemetryStatus giStage,
            LightingDispatchStageTelemetryStatus reuseStage
    ) {
        String energy = roundElevenNativeRestirValue(
                snapshot,
                roundElevenStageOutputEnergy(directStage, giStage, reuseStage),
                "restir_output_energy",
                "round11_output_energy",
                "output_energy"
        );
        String checksum = roundElevenNativeRestirValue(
                snapshot,
                roundElevenStageOutputChecksum(directStage, giStage, reuseStage),
                "restir_output_checksum",
                "round11_output_checksum",
                "output_checksum"
        );
        String metadataOnly = roundElevenMetadataOnlyValue(snapshot, directStage);
        return "energy="
                + roundElevenEvidenceValue(energy, metadataOnly)
                + " checksum="
                + roundElevenEvidenceValue(checksum, metadataOnly);
    }

    private static String roundElevenGiPathReuseBoundaryLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus giStage
    ) {
        String pathReuse = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{giStage}, "path_reuse_count", "path_reuse"),
                "path_reuse_count",
                "path_reuse"
        );
        String boundary = roundElevenExecutionEvidenceBoundaryLine(snapshot, giStage);
        if (isTruthy(roundElevenRealRestirDiExecutionValue(snapshot, giStage))) {
            return "pathReuse="
                    + pathReuse
                    + " | first bounded CPU/native preview; physical GI/PT reuse quality still pending | "
                    + boundary;
        }
        return "pathReuse="
                + pathReuse
                + " | metadata-only; GI/PT path reuse execution not proven | "
                + boundary;
    }

    private static String roundElevenStabilityBoundaryLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus reuseStage
    ) {
        String temporal = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage}, "temporal_reuse_count"),
                "temporal_reuse_count"
        );
        String spatial = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{reuseStage}, "spatial_reuse_count"),
                "spatial_reuse_count"
        );
        String invalidated = roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        new LightingDispatchStageTelemetryStatus[]{reuseStage},
                        "invalidated_reservoir_count",
                        "invalidation_count"
                ),
                "invalidated_reservoir_count",
                "invalidation_count"
        );
        String execution = isTruthy(roundElevenRealRestirDiExecutionValue(snapshot, reuseStage))
                ? "bounded native counters"
                : "metadata counters";
        return execution
                + "; temporal="
                + temporal
                + " spatial="
                + spatial
                + " invalidated="
                + invalidated
                + "; no flicker/stability improvement claim without controller sequence proof";
    }

    private static String roundElevenExecutionEvidenceBoundaryLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        return roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "boundary", "proof_boundary"),
                "boundary",
                "proof_boundary",
                "evidence_boundary"
        );
    }

    private static String roundElevenMetadataOnlyValue(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus stage
    ) {
        return roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(new LightingDispatchStageTelemetryStatus[]{stage}, "metadata_only", "metadataOnly"),
                "metadata_only",
                "metadataOnly"
        );
    }

    private static String roundElevenStageOutputEnergy(LightingDispatchStageTelemetryStatus... stages) {
        String detail = firstRoundElevenDetail(
                stages,
                "restir_output_energy",
                "round11_output_energy",
                "output_energy"
        );
        if (!"?".equals(detail)) {
            return detail;
        }
        for (LightingDispatchStageTelemetryStatus stage : stages) {
            if (stage != null && stage.outputEnergy() != null && !stage.outputEnergy().isBlank()) {
                return stage.outputEnergy();
            }
        }
        return "?";
    }

    private static String roundElevenStageOutputChecksum(LightingDispatchStageTelemetryStatus... stages) {
        String detail = firstRoundElevenDetail(
                stages,
                "restir_output_checksum",
                "round11_output_checksum",
                "output_checksum"
        );
        if (!"?".equals(detail)) {
            return detail;
        }
        for (LightingDispatchStageTelemetryStatus stage : stages) {
            if (stage != null && stage.outputChecksum() != null) {
                return Long.toString(stage.outputChecksum());
            }
        }
        return "?";
    }

    private static String roundElevenEvidenceValue(String value, String metadataOnly) {
        if (value == null || value.isBlank() || "?".equals(value)) {
            return isTruthy(metadataOnly) ? "missing(metadata-only)" : "missing";
        }
        return value;
    }

    private static String roundElevenBooleanLabel(String value) {
        if (isTruthy(value)) {
            return "true";
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return "false";
        }
        return value == null || value.isBlank() ? "?" : value;
    }

    private static String roundElevenNativeRestirValue(
            LucernaStatusSnapshot snapshot,
            String fallback,
            String... keys
    ) {
        String block = roundElevenNativeRestirBlock(snapshot);
        if (block.isBlank() || keys == null) {
            return fallback == null || fallback.isBlank() ? "?" : fallback;
        }
        for (String key : keys) {
            String value = nativeStatusValue(block, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return fallback == null || fallback.isBlank() ? "?" : fallback;
    }

    private static String roundElevenNativeRestirBlock(LucernaStatusSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return "";
        }
        String prefix = "round11_restir={";
        int start = nativeStatus.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        int contentStart = start + prefix.length();
        int depth = 1;
        for (int index = contentStart; index < nativeStatus.length(); index++) {
            char character = nativeStatus.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return nativeStatus.substring(contentStart, index);
                }
            }
        }
        return nativeStatus.substring(contentStart);
    }

    private static String nativeStatusValue(String text, String key) {
        if (text == null || text.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String search = key + "=";
        int start = text.indexOf(search);
        if (start < 0) {
            return "";
        }
        int valueStart = start + search.length();
        if (valueStart >= text.length()) {
            return "";
        }
        if (text.charAt(valueStart) == '"') {
            int valueEnd = text.indexOf('"', valueStart + 1);
            if (valueEnd > valueStart) {
                return text.substring(valueStart + 1, valueEnd);
            }
            return "";
        }
        int valueEnd = valueStart;
        while (valueEnd < text.length()) {
            char character = text.charAt(valueEnd);
            if (character == ',' || character == '}' || character == ']' || Character.isWhitespace(character)) {
                break;
            }
            valueEnd++;
        }
        if (valueEnd <= valueStart) {
            return "";
        }
        return text.substring(valueStart, valueEnd).replace("\"", "");
    }

    private static String roundTenTraversalValue(LucernaStatusSnapshot snapshot, String... keys) {
        if (snapshot == null || keys == null) {
            return "?";
        }
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        String traversalBlock = nativeStatusBlock(nativeStatus, "round10_voxel_traversal={");
        for (String key : keys) {
            String value = nativeStatusValue(traversalBlock, key);
            if (hasReportedValue(value)) {
                return value;
            }
        }
        for (String key : keys) {
            String value = nativeStatusValue(nativeStatus, key);
            if (hasReportedValue(value)) {
                return value;
            }
        }
        return "?";
    }

    private static String roundTenTraversalReadyValue(LucernaStatusSnapshot snapshot, String... keys) {
        String value = roundTenTraversalValue(snapshot, keys);
        if ("?".equals(value)) {
            return "?";
        }
        if (isTruthy(value)) {
            return "yes";
        }
        if (isFalsy(value)) {
            return "no";
        }
        return value;
    }

    private static String roundTenRealGpuTraversalExecuted(LucernaStatusSnapshot snapshot) {
        String explicit = roundTenTraversalValue(
                snapshot,
                "real_gpu_traversal_executed",
                "realGpuTraversalExecuted",
                "gpu_traversal_executed"
        );
        if (isTruthy(explicit)) {
            return "yes";
        }
        if (isFalsy(explicit)) {
            return "no";
        }

        String backend = roundTenTraversalValue(snapshot, "backend", "traversal_backend", "voxel_traversal_backend");
        String boundary = roundTenTraversalValue(snapshot, "boundary", "traversal_boundary", "evidence_boundary");
        String combined = (backend + " " + boundary).toLowerCase(Locale.ROOT);
        if (combined.contains("no_gpu")
                || combined.contains("not_real_gpu")
                || combined.contains("not_hardware_rt")
                || combined.contains("cpu_metadata")
                || combined.contains("scaffold")) {
            return "no";
        }
        return "no";
    }

    private static String nativeStatusBlock(String text, String prefix) {
        if (text == null || text.isBlank() || prefix == null || prefix.isBlank()) {
            return "";
        }
        int start = text.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        int contentStart = start + prefix.length();
        int depth = 1;
        for (int index = contentStart; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(contentStart, index);
                }
            }
        }
        return text.substring(contentStart);
    }

    private static boolean hasReportedValue(String value) {
        return value != null && !value.isBlank() && !"?".equals(value);
    }

    private static boolean isFalsy(String value) {
        return "0".equals(value)
                || "false".equalsIgnoreCase(value)
                || "no".equalsIgnoreCase(value);
    }

    private static Double parseDoubleOrNull(String value) {
        if (value == null || value.isBlank() || "?".equals(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatRatio(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static String roundElevenNativeConfidenceLine(
            LucernaStatusSnapshot snapshot,
            LightingDispatchStageTelemetryStatus... stages
    ) {
        return "min=" + roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(stages, "min_confidence", "confidence_min", "reservoir_confidence_min"),
                "min_confidence",
                "confidence_min",
                "min"
        ) + " mean=" + roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(
                        stages,
                        "mean_confidence",
                        "avg_confidence",
                        "average_confidence",
                        "confidence",
                        "combined_confidence"
                ),
                "mean_confidence",
                "avg_confidence",
                "average_confidence",
                "mean"
        ) + " max=" + roundElevenNativeRestirValue(
                snapshot,
                firstRoundElevenDetail(stages, "max_confidence", "confidence_max", "reservoir_confidence_max"),
                "max_confidence",
                "confidence_max",
                "max"
        );
    }

    private static String roundElevenNativeValue(LucernaStatusSnapshot snapshot, String key, String fallback) {
        if (snapshot == null || key == null || key.isBlank()) {
            return fallback == null || fallback.isBlank() ? "?" : fallback;
        }
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return fallback == null || fallback.isBlank() ? "?" : fallback;
        }
        String search = key + "=";
        int start = nativeStatus.indexOf(search);
        if (start < 0) {
            return fallback == null || fallback.isBlank() ? "?" : fallback;
        }
        int valueStart = start + search.length();
        int valueEnd = valueStart;
        while (valueEnd < nativeStatus.length()) {
            char character = nativeStatus.charAt(valueEnd);
            if (character == ',' || character == '}' || character == ']' || Character.isWhitespace(character)) {
                break;
            }
            valueEnd++;
        }
        if (valueEnd <= valueStart) {
            return fallback == null || fallback.isBlank() ? "?" : fallback;
        }
        return nativeStatus.substring(valueStart, valueEnd).replace("\"", "");
    }

    private static String roundElevenReadinessLine(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "stage=not_reported";
        }
        return "enabled=" + yesNoUnknown(stage.enabled())
                + " ready=" + yesNoUnknown(stage.readyForNativeExecution())
                + " recorded=" + yesNoUnknown(stage.recordedThisFrame())
                + " reason=" + readinessReason(stage);
    }

    private static String firstRoundElevenDetail(LightingDispatchStageTelemetryStatus[] stages, String... keys) {
        if (stages == null || keys == null) {
            return "?";
        }
        for (LightingDispatchStageTelemetryStatus stage : stages) {
            String value = firstDetailOrUnknown(stage, keys);
            if (!"?".equals(value)) {
                return value;
            }
        }
        return "?";
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

    private static String giEmissiveProximityLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "emissive_proximity_available",
                "scene_emissive_proximity_available",
                "emissiveProximityAvailable",
                "sceneEmissiveProximityAvailable"
        );
    }

    private static String giAffectedSurfaceRegionLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "affected_surface_region",
                "affected_surface_region_label",
                "scene_affected_surface_region",
                "scene_affected_surface_region_label",
                "surface_proof_region",
                "surfaceProofRegion"
        );
    }

    private static String giHandHudExcludedLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "hand_hud_excluded",
                "handHudExcluded",
                "proof_hand_hud_excluded",
                "proofHandHudExcluded",
                "before_hud_and_hand",
                "beforeHudAndHand"
        );
    }

    private static String giPhysicalSampleLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "physical_gi_sample_count",
                "physicalGiSampleCount",
                "gi_physical_sample_count",
                "giPhysicalSampleCount",
                "physical_sample_count",
                "physicalSampleCount",
                "bounce_sample_count",
                "bounceSampleCount",
                "scene_sample_count",
                "sceneSampleCount"
        );
    }

    private static String giMaterialCouplingLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "physical_gi_material_coupling",
                "physicalGiMaterialCoupling",
                "gi_material_coupling",
                "giMaterialCoupling",
                "material_coupling_score",
                "materialCouplingScore",
                "material_response_coupling",
                "materialResponseCoupling",
                "albedo_normal_coupling",
                "albedoNormalCoupling"
        );
    }

    private static String giGeometryCouplingLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "physical_gi_geometry_coupling",
                "physicalGiGeometryCoupling",
                "gi_geometry_coupling",
                "giGeometryCoupling",
                "geometry_coupling_score",
                "geometryCouplingScore",
                "surface_normal_coupling",
                "surfaceNormalCoupling",
                "occlusion_coupling",
                "occlusionCoupling"
        );
    }

    private static String giBounceSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "physical_gi_bounce_source",
                "physicalGiBounceSource",
                "gi_bounce_source",
                "giBounceSource",
                "bounce_source",
                "bounceSource",
                "source_coupling",
                "sourceCoupling",
                "emissive_sun_moon_coupling",
                "emissiveSunMoonCoupling"
        );
    }

    private static String giTracingEvidenceLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "physical_gi_tracing_evidence",
                "physicalGiTracingEvidence",
                "gi_tracing_evidence",
                "giTracingEvidence",
                "tracing_evidence",
                "tracingEvidence",
                "voxel_trace_evidence",
                "voxelTraceEvidence",
                "ray_hit_evidence",
                "rayHitEvidence"
        );
    }

    private static String giSurfaceOnlyProofEligibleLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "surface_only_proof_eligible",
                "surfaceOnlyProofEligible",
                "real_surface_only_proof_ready",
                "realSurfaceOnlyProofReady"
        );
    }

    private static String giAuthenticOutputLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "gi_output_authentic_native_cpu",
                "giOutputAuthenticNativeCpu",
                "native_gi_output_authentic",
                "nativeGiOutputAuthentic",
                "authentic_native_gi_output"
        );
    }

    private static String giCpuScaffoldOutputLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "cpu_gi_scaffold_output",
                "cpuGiScaffoldOutput",
                "gi_output_cpu_scaffold",
                "giOutputCpuScaffold",
                "native_cpu_scaffold_output"
        );
    }

    private static String realGiShaderLabel(LightingDispatchStageTelemetryStatus stage) {
        return firstDetailOrUnknown(
                stage,
                "real_shader_gi_output",
                "realShaderGiOutput",
                "shader_gi_output",
                "shaderGiOutput",
                "gpu_gi_output"
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

    private static String denoiseSourceIdentityLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "?";
        }
        if (!stage.sourceIdentity().isBlank()) {
            return shorten(stage.sourceIdentity(), 32);
        }
        return shorten(denoiseSourceLabel(stage), 32);
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

    private static String shaderOutputReadinessLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "?";
        }
        if (stage.shaderOutputReady() != null) {
            return yesNoUnknown(stage.shaderOutputReady());
        }
        return realDenoiseShaderLabel(stage);
    }

    private static String shaderOutputImageCandidateReadinessLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate",
                "shaderOutputImageCandidate",
                "shader_output_image_candidate_present",
                "shaderOutputImageCandidatePresent",
                "shader_denoise_output_candidate",
                "shaderDenoiseOutputCandidate",
                "shader_output_candidate",
                "shaderOutputCandidate"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        String sourceKind = firstDetailOrUnknown(stage, "source_kind", "sourceKind", "denoised_source_kind");
        if (containsShaderOutputImageCandidate(sourceKind)
                || (stage != null && containsShaderOutputImageCandidate(stage.sourceIdentity()))) {
            return "true";
        }
        return "?";
    }

    private static String shaderOutputImageCandidateDimensionsLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_dimensions",
                "shaderOutputImageCandidateDimensions",
                "shader_output_candidate_dimensions",
                "shaderOutputCandidateDimensions",
                "shader_output_image_candidate_size",
                "shaderOutputImageCandidateSize",
                "shader_output_candidate_size",
                "shaderOutputCandidateSize",
                "shader_output_candidate_dims",
                "shaderOutputCandidateDims"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        String width = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_width",
                "shaderOutputImageCandidateWidth",
                "shader_output_candidate_width",
                "shaderOutputCandidateWidth",
                "shader_output_width",
                "shaderOutputWidth"
        );
        String height = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_height",
                "shaderOutputImageCandidateHeight",
                "shader_output_candidate_height",
                "shaderOutputCandidateHeight",
                "shader_output_height",
                "shaderOutputHeight"
        );
        if (!"?".equals(width) && !"?".equals(height)) {
            return width + "x" + height;
        }
        if (stage != null
                && "true".equalsIgnoreCase(shaderOutputImageCandidateReadinessLabel(stage))
                && !stage.outputDimensions().isBlank()) {
            return stage.outputDimensions();
        }
        return "?";
    }

    private static String shaderOutputImageCandidateChecksumLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_checksum",
                "shaderOutputImageCandidateChecksum",
                "shader_output_candidate_checksum",
                "shaderOutputCandidateChecksum",
                "shader_denoise_output_candidate_checksum",
                "shaderDenoiseOutputCandidateChecksum",
                "shader_output_checksum",
                "shaderOutputChecksum"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        if (stage != null
                && "true".equalsIgnoreCase(shaderOutputImageCandidateReadinessLabel(stage))
                && stage.outputChecksum() != null) {
            return Long.toUnsignedString(stage.outputChecksum());
        }
        return "?";
    }

    private static String shaderOutputImageCandidateSourceLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_source",
                "shaderOutputImageCandidateSource",
                "shader_output_candidate_source",
                "shaderOutputCandidateSource",
                "shader_output_image_candidate_source_label",
                "shaderOutputImageCandidateSourceLabel",
                "source_kind",
                "sourceKind"
        );
        if (!"?".equals(explicit)) {
            return shorten(explicit, 72);
        }
        if (stage != null && containsShaderOutputImageCandidate(stage.sourceIdentity())) {
            return shorten(stage.sourceIdentity(), 72);
        }
        return "?";
    }

    private static String shaderOutputImageCandidateMarkerLabel(LightingDispatchStageTelemetryStatus stage) {
        return shorten(firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_marker",
                "shaderOutputImageCandidateMarker",
                "shader_output_candidate_marker",
                "shaderOutputCandidateMarker",
                "shader_denoise_output_candidate_marker",
                "shaderDenoiseOutputCandidateMarker"
        ), 48);
    }

    private static String shaderOutputImageCandidateBlockerLabel(LightingDispatchStageTelemetryStatus stage) {
        String explicit = firstDetailOrUnknown(
                stage,
                "shader_output_image_candidate_blocker",
                "shaderOutputImageCandidateBlocker",
                "shader_output_candidate_blocker",
                "shaderOutputCandidateBlocker",
                "shader_output_candidate_boundary",
                "shaderOutputCandidateBoundary",
                "shader_output_image_candidate_boundary",
                "shaderOutputImageCandidateBoundary"
        );
        if (!"?".equals(explicit)) {
            return shorten(explicit, 72);
        }
        if (stage != null && !stage.shaderDenoiseBlockers().isBlank()) {
            return shorten(stage.shaderDenoiseBlockers(), 72);
        }
        if (stage != null && containsShaderOutputImageCandidate(stage.sourceIdentity())) {
            return "candidate-only:not-real-shader-generated-denoised-output";
        }
        return "?";
    }

    private static String shaderDenoiseOutputAttemptedLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "?";
        }
        boolean attempted = Boolean.TRUE.equals(stage.shaderDispatchPrepared())
                || Boolean.TRUE.equals(stage.shaderOutputReady())
                || Boolean.TRUE.equals(stage.shaderOutputImageReady())
                || Boolean.TRUE.equals(stage.shaderOutputImageCandidateReady())
                || Boolean.TRUE.equals(stage.shaderOutputMaterialReady());
        return yesNo(attempted);
    }

    private static String realShaderDenoiseOutputReadyLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null || stage.realShaderDenoiseOutputReady() == null) {
            return "false";
        }
        return yesNo(stage.realShaderDenoiseOutputReady());
    }

    private static String shaderDenoiseNoOverclaimLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null || stage.realShaderDenoiseOutputReady() == null) {
            return "true";
        }
        return yesNo(!Boolean.TRUE.equals(stage.realShaderDenoiseOutputReady()));
    }

    private static String shaderDenoiseOutputBoundaryLine(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "realShaderDenoiseOutputReady=false noOverclaim=true cpuReadbackFallback=? blocker=stage-not-reported";
        }
        return shorten(stage.shaderDenoiseOutputBoundaryLine(), 160);
    }

    private static String shaderOutputPrerequisitesLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "dispatch=? image=? material=? generated=? realReady=?";
        }
        return "dispatch=" + yesNoUnknown(stage.shaderDispatchPrepared())
                + " image=" + yesNoUnknown(stage.shaderOutputImageReady())
                + " material=" + yesNoUnknown(stage.shaderOutputMaterialReady())
                + " generated=" + yesNoUnknown(stage.shaderGeneratedOutput())
                + " realReady=" + shaderOutputReadinessLabel(stage);
    }

    private static String shaderDenoiseCpuFallbackBoundaryLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "active=? cpuReady=? cpuGenerated=? candidateOnly=? source=?";
        }
        boolean candidateOnly = "true".equalsIgnoreCase(shaderOutputImageCandidateReadinessLabel(stage))
                && !Boolean.TRUE.equals(stage.shaderGeneratedOutput());
        return "active=" + yesNoUnknown(stage.cpuReadbackFallback())
                + " cpuReady=" + yesNoUnknown(stage.cpuDenoiseReady())
                + " cpuGenerated=" + yesNoUnknown(stage.cpuOutputGenerated())
                + " candidateOnly=" + yesNo(candidateOnly)
                + " source=" + shorten(denoiseSourceIdentityLabel(stage), 42);
    }

    private static boolean containsShaderOutputImageCandidate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace("_", "-");
        return normalized.contains("shader-output-image-candidate")
                || normalized.contains("shader-denoise-output-candidate")
                || normalized.contains("shader-output-candidate");
    }

    private static String temporalReadinessLabel(
            LightingDispatchStageTelemetryStatus denoiseStage,
            LightingDispatchStageTelemetryStatus adaptiveStage
    ) {
        String explicit = firstDetailOrUnknown(
                denoiseStage,
                "temporal_ready",
                "temporal_history_ready",
                "history_ready",
                "temporal_proof_ready",
                "temporal_acceptance_ready"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        explicit = firstDetailOrUnknown(
                adaptiveStage,
                "temporal_ready",
                "temporal_history_ready",
                "history_ready",
                "temporal_proof_ready",
                "temporal_acceptance_ready"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        if (denoiseStage != null) {
            String acceptedCount = firstDetailOrUnknown(
                    denoiseStage,
                    "history_accepted_count",
                    "history_accepted"
            );
            if (denoiseStage.historyRejectionCount() != null || !"?".equals(acceptedCount)) {
                return "counters_present";
            }
        }
        return "?";
    }

    private static String temporalDetailOrUnknown(
            LightingDispatchStageTelemetryStatus denoiseStage,
            LightingDispatchStageTelemetryStatus adaptiveStage,
            String... keys
    ) {
        String denoiseValue = firstDetailOrUnknown(denoiseStage, keys);
        if (!"?".equals(denoiseValue)) {
            return shorten(denoiseValue, 32);
        }
        return shorten(firstDetailOrUnknown(adaptiveStage, keys), 32);
    }

    private static String denoiseTemporalStatusLabel(
            LightingDispatchStageTelemetryStatus denoiseStage,
            LightingDispatchStageTelemetryStatus adaptiveStage
    ) {
        if (denoiseStage != null) {
            return shorten(denoiseStage.temporalHistoryStatusLine(), 96);
        }
        if (adaptiveStage != null) {
            return shorten(adaptiveStage.temporalHistoryStatusLine(), 96);
        }
        return "pending(no denoise/adaptive temporal stage details)";
    }

    private static String denoiseProofBoundaryLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "shader denoise and temporal proof telemetry not reported yet";
        }
        if (!stage.evidenceBoundary().isBlank()) {
            return shorten(stage.evidenceBoundary(), 96);
        }
        return "CPU/readback preview evidence only until real shader output and temporal screenshots pass";
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
            lines.add(Component.literal("Stage status: " + stage.compactStageStatusLine()));
            lines.add(Component.literal("Stage work: " + stage.stageWorkStatusLine()));
            lines.add(Component.literal("Timing boundary: " + stage.explicitMeasurementBoundaryLine()));
            lines.add(Component.literal("Temporal history: " + stage.temporalHistoryStatusLine()));
            lines.add(Component.literal("Proof boundary: " + stage.proofBoundaryLine()));
        }
    }

    private static void addTemporalStageBoundaryLines(List<Component> lines, LucernaStatusSnapshot snapshot) {
        LightingDispatchTelemetryStatus lightingDispatch = snapshot.lightingDispatchStatus();
        if (!lightingDispatch.hasStageStatuses()) {
            lines.add(Component.literal("Native temporal history: unavailable(" + shorten(lightingDispatch.message(), 64) + ")"));
            return;
        }
        LightingDispatchStageTelemetryStatus denoiseStage = firstStage(
                lightingDispatch,
                "denoise",
                "diffuse_gi_denoise",
                "denoised_gi",
                "round7_denoise"
        );
        LightingDispatchStageTelemetryStatus adaptiveStage = firstStage(
                lightingDispatch,
                "adaptive_sampling",
                "history_confidence",
                "variance",
                "ray_budget"
        );
        if (denoiseStage != null) {
            lines.add(Component.literal("Denoise temporal: " + denoiseStage.temporalHistoryStatusLine()));
        }
        if (adaptiveStage != null) {
            lines.add(Component.literal("Adaptive temporal: " + adaptiveStage.temporalHistoryStatusLine()));
        }
        if (denoiseStage == null && adaptiveStage == null) {
            lines.add(Component.literal("Native temporal history: pending(no denoise/adaptive stage details)"));
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

    private static Boolean physicalSourceActive(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return null;
        }
        if (Boolean.TRUE.equals(stage.payloadHasDirectWork())) {
            return true;
        }
        if (parsePositive(stage.details().get("physical_source_active"))
                || parsePositive(stage.details().get("physical_source_ready"))
                || parsePositive(stage.details().get("source_active"))
                || parsePositive(stage.details().get("source_ready"))
                || hasPositive(stage.emissiveCount())
                || hasPositive(stage.celestialCount())
                || hasPositive(stage.candidateCount())
                || hasPositive(stage.sampleCount())) {
            return true;
        }
        if (Boolean.FALSE.equals(stage.enabled()) || Boolean.FALSE.equals(stage.payloadAccepted())) {
            return false;
        }
        return null;
    }

    private static Long stageCandidateCount(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? null : stage.candidateCount();
    }

    private static Long stageSampleCount(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? null : stage.sampleCount();
    }

    private static Long stageRayCount(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? null : stage.rayCount();
    }

    private static Boolean stageCpuOutputGenerated(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? null : stage.cpuOutputGenerated();
    }

    private static String stageOutputEnergyLabel(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "missing" : evidenceValueLabel(stage.outputEnergy());
    }

    private static String stageOutputChecksumLabel(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "missing" : evidenceValueLabel(stage.outputChecksum());
    }

    private static String stageNativeExecutionLabel(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "not_reported" : nativeExecutionLabel(stage);
    }

    private static String stageMetadataOnlyLabel(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "?" : yesNoUnknown(stage.metadataOnly());
    }

    private static String stageSectionCountLabel(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "?" : valueOrUnknown(stage.sectionSnapshotCount());
    }

    private static String stageSurfaceSampleLabel(LightingDispatchStageTelemetryStatus stage) {
        if (stage == null) {
            return "?";
        }
        String explicit = firstDetailOrUnknown(
                stage,
                "surface_sample_count",
                "surface_samples",
                "opaque_surface_samples",
                "extracted_surface_samples",
                "scene_surface_samples"
        );
        if (!"?".equals(explicit)) {
            return explicit;
        }
        return valueOrUnknown(stage.sampleCount());
    }

    private static String proofFlagLabel(LightingDispatchStageTelemetryStatus first,
                                         LightingDispatchStageTelemetryStatus second,
                                         LightingDispatchStageTelemetryStatus third,
                                         String... keys) {
        String value = firstNonUnknownDetail(first, second, third, keys);
        if ("?".equals(value)) {
            return "?";
        }
        return isTruthy(value) || parsePositive(value) ? "yes" : value;
    }

    private static String firstNonUnknownDetail(LightingDispatchStageTelemetryStatus first,
                                                LightingDispatchStageTelemetryStatus second,
                                                LightingDispatchStageTelemetryStatus third,
                                                String... keys) {
        String firstValue = firstDetailOrUnknown(first, keys);
        if (!"?".equals(firstValue)) {
            return firstValue;
        }
        String secondValue = firstDetailOrUnknown(second, keys);
        if (!"?".equals(secondValue)) {
            return secondValue;
        }
        return firstDetailOrUnknown(third, keys);
    }

    private static boolean hasPositive(Long value) {
        return value != null && value > 0L;
    }

    private static String firstLightingPhysicalProofBoundary(FinalCompositeModeStatus compositeStatus) {
        return "requires controller screenshot delta and logs; "
                + shorten(compositeStatus.visualProofBoundarySummary(), 96);
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
            if ((value == null || value.isBlank()) && key != null) {
                value = stage.details().get(key.toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_'));
            }
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
