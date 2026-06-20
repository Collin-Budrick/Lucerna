package net.lucerna.render.preview;

import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.upload.NativeDiffuseGiPlanUpload;
import net.lucerna.upload.NativeDiffuseGiUploadPacket;

public record Round6DiffuseGiPreviewCompositeState(
        boolean diffuseGiEnabled,
        boolean cacheEnabled,
        long generation,
        int gridWidth,
        int gridHeight,
        int samplesPerCell,
        int rayCount,
        int cacheReadCount,
        int cacheWriteCount,
        int cacheRecordCount,
        int dirtyRegionCount,
        int surfaceRecordCount,
        int radianceRecordCount,
        boolean emissiveProximityAvailable,
        boolean affectedSurfaceRegionAvailable,
        String affectedSurfaceRegionLabel,
        boolean handHudExcludedProofHint,
        boolean surfaceOnlyProofEligible,
        boolean giOutputAuthenticNativeCpu,
        boolean realShaderGiOutput,
        boolean cpuDenoiseScaffoldOutput,
        boolean realDenoiseShaderOutput,
        boolean cacheUsable,
        boolean sourceDirectLightingReady,
        String sourceDebugLabel,
        String reason
) {
    public Round6DiffuseGiPreviewCompositeState {
        generation = Math.max(0L, generation);
        gridWidth = Math.max(0, gridWidth);
        gridHeight = Math.max(0, gridHeight);
        samplesPerCell = Math.max(0, samplesPerCell);
        rayCount = Math.max(0, rayCount);
        cacheReadCount = Math.max(0, cacheReadCount);
        cacheWriteCount = Math.max(0, cacheWriteCount);
        cacheRecordCount = Math.max(0, cacheRecordCount);
        dirtyRegionCount = Math.max(0, dirtyRegionCount);
        surfaceRecordCount = Math.max(0, surfaceRecordCount);
        radianceRecordCount = Math.max(0, radianceRecordCount);
        if (affectedSurfaceRegionLabel == null || affectedSurfaceRegionLabel.isBlank()) {
            affectedSurfaceRegionLabel = affectedSurfaceRegionAvailable
                    ? "affected surface region available"
                    : "affected surface region unavailable";
        } else {
            affectedSurfaceRegionLabel = affectedSurfaceRegionLabel.trim();
        }
        if (sourceDebugLabel == null || sourceDebugLabel.isBlank()) {
            sourceDebugLabel = "Round 6 GI source summary unavailable";
        } else {
            sourceDebugLabel = sourceDebugLabel.trim();
        }
        if (reason == null || reason.isBlank()) {
            reason = diffuseGiEnabled
                    && cacheEnabled
                    && gridWidth > 0
                    && gridHeight > 0
                    && samplesPerCell > 0
                    && rayCount > 0
                    && cacheReadCount > 0
                    && cacheRecordCount > 0
                    && sourceDirectLightingReady
                    ? "Round 6 diffuse GI scene-tied CPU/readback source is ready for final-composite preview"
                    : "Round 6 diffuse GI scene-tied CPU/readback source is not ready for final-composite preview";
        } else {
            reason = reason.trim();
        }
    }

    public static Round6DiffuseGiPreviewCompositeState unavailable(String reason) {
        return new Round6DiffuseGiPreviewCompositeState(
                false,
                false,
                0L,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                "affected surface region unavailable",
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                "Round 6 GI source summary unavailable",
                reason
        );
    }

    public static Round6DiffuseGiPreviewCompositeState from(
            boolean diffuseGiEnabled,
            boolean cacheEnabled,
            int cacheRecordCount,
            NativeDiffuseGiUploadPacket upload
    ) {
        if (upload == null) {
            return unavailable("Round 6 diffuse GI upload packet is unavailable");
        }

        NativeDiffuseGiPlanUpload plan = upload.planUpload();
        int cacheReads = upload.dirtyRegionCount() + upload.surfaceRecordCount() + upload.radianceRecordCount();
        int cacheWrites = upload.surfaceRecordCount() + upload.radianceRecordCount();
        boolean resolvedDiffuseGiEnabled = diffuseGiEnabled && upload.readyForScheduling();
        boolean resolvedCacheEnabled = cacheEnabled
                && cacheRecordCount > 0
                && (upload.cacheUsable() || upload.surfaceRecordCount() > 0 || upload.radianceRecordCount() > 0);
        boolean[] proofFlags = proofFlags(plan);
        boolean[] denoiseFlags = denoiseFlags(plan);
        String reason = previewReason(
                resolvedDiffuseGiEnabled,
                resolvedCacheEnabled,
                plan.gridWidth(),
                plan.gridHeight(),
                plan.cappedRays(),
                cacheReads,
                plan.sourceDirectLightingReady(),
                plan.sourceDebugLabel()
        );

        return new Round6DiffuseGiPreviewCompositeState(
                resolvedDiffuseGiEnabled,
                resolvedCacheEnabled,
                upload.generation(),
                plan.gridWidth(),
                plan.gridHeight(),
                plan.samplesPerCell(),
                plan.cappedRays(),
                cacheReads,
                cacheWrites,
                cacheRecordCount,
                upload.dirtyRegionCount(),
                upload.surfaceRecordCount(),
                upload.radianceRecordCount(),
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_EMISSIVE_PROXIMITY_AVAILABLE_OFFSET],
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_AFFECTED_SURFACE_REGION_AVAILABLE_OFFSET],
                plan.sceneAffectedSurfaceRegionLabel(),
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_HAND_HUD_EXCLUDED_OFFSET],
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_SURFACE_ONLY_ELIGIBLE_OFFSET],
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_GI_OUTPUT_AUTHENTIC_NATIVE_CPU_OFFSET],
                proofFlags[NativeDiffuseGiPlanUpload.PROOF_GI_OUTPUT_REAL_SHADER_OFFSET],
                denoiseFlags[NativeDiffuseGiPlanUpload.DENOISE_OUTPUT_CPU_SCAFFOLD_OFFSET],
                denoiseFlags[NativeDiffuseGiPlanUpload.DENOISE_OUTPUT_REAL_SHADER_OFFSET],
                upload.cacheUsable(),
                plan.sourceDirectLightingReady(),
                plan.sourceDebugLabel(),
                reason
        );
    }

    public boolean readyForRound6PreviewSource() {
        return this.diffuseGiEnabled
                && this.gridWidth > 0
                && this.gridHeight > 0
                && this.samplesPerCell > 0
                && this.rayCount > 0
                && this.sourceDirectLightingReady;
    }

    public boolean readyForRealSurfaceOnlyProof() {
        return readyForRound6PreviewSource()
                && this.emissiveProximityAvailable
                && this.affectedSurfaceRegionAvailable
                && this.handHudExcludedProofHint
                && this.surfaceOnlyProofEligible
                && this.giOutputAuthenticNativeCpu
                && !this.realShaderGiOutput;
    }

    public boolean readyForRound7RawGiSource() {
        return readyForRound6PreviewSource();
    }

    public boolean readyForFinalComposite(Round6DiffuseGiCpuOutputPayload sourcePayload) {
        return readyForRound6PreviewSource()
                && sourcePayload != null
                && sourcePayload.readyForPreviewDraw();
    }

    public String round7RawGiModeKey() {
        return "ROUND7_RAW_GI";
    }

    public String round7RawGiEvidenceLabel() {
        return "round7.rawGi.nativeDiffuseGiPayload";
    }

    public String round7RawGiSourceLabel() {
        return "Round 7 RAW_GI visual source: native scene-tied diffuse-GI RGBA8 CPU/readback payload";
    }

    public String round7RawGiReadinessReason(Round6DiffuseGiCpuOutputPayload sourcePayload) {
        if (!readyForRound7RawGiSource()) {
            return "Round 7 RAW_GI visual source is not ready: " + this.reason;
        }
        if (sourcePayload == null) {
            return "Round 7 RAW_GI visual source metadata is ready, but no native scene-tied diffuse-GI RGBA8 CPU/readback payload is available";
        }
        if (!sourcePayload.readyForPreviewDraw()) {
            return "Round 7 RAW_GI visual source metadata is ready, but the native scene-tied diffuse-GI RGBA8 CPU/readback payload is not displayable: "
                    + sourcePayload.previewReadinessReason();
        }
        return "Round 7 RAW_GI visual source can draw the native scene-tied diffuse-GI RGBA8 CPU/readback payload as a raw source view";
    }

    public String finalCompositeReadinessReason(Round6DiffuseGiCpuOutputPayload sourcePayload) {
        if (!readyForRound6PreviewSource()) {
            return this.reason;
        }
        if (sourcePayload == null) {
            return "Round 6 diffuse GI output-source metadata is ready, but no native scene-tied diffuse GI RGBA8 CPU/readback payload is available";
        }
        if (!sourcePayload.readyForPreviewDraw()) {
            return "Round 6 diffuse GI output-source metadata is ready, but the native scene-tied diffuse GI RGBA8 CPU/readback payload is not displayable: "
                    + sourcePayload.previewReadinessReason();
        }
        return "Round 6 diffuse GI output-source metadata and native scene-tied diffuse GI RGBA8 CPU/readback payload are ready for diffuse-GI final-composite preview";
    }

    public String summary() {
        return "diffuseGiEnabled=" + this.diffuseGiEnabled
                + ",cacheEnabled=" + this.cacheEnabled
                + ",generation=" + this.generation
                + ",grid=" + this.gridWidth + "x" + this.gridHeight
                + ",samplesPerCell=" + this.samplesPerCell
                + ",rays=" + this.rayCount
                + ",cacheReads=" + this.cacheReadCount
                + ",cacheWrites=" + this.cacheWriteCount
                + ",cacheRecords=" + this.cacheRecordCount
                + ",dirtyRegions=" + this.dirtyRegionCount
                + ",surfaceRecords=" + this.surfaceRecordCount
                + ",radianceRecords=" + this.radianceRecordCount
                + ",emissiveProximityAvailable=" + this.emissiveProximityAvailable
                + ",affectedSurfaceRegion=\"" + this.affectedSurfaceRegionLabel + "\""
                + ",handHudExcludedProofHint=" + this.handHudExcludedProofHint
                + ",surfaceOnlyProofEligible=" + this.surfaceOnlyProofEligible
                + ",realSurfaceOnlyProofReady=" + this.readyForRealSurfaceOnlyProof()
                + ",giOutputAuthenticNativeCpu=" + this.giOutputAuthenticNativeCpu
                + ",realShaderGiOutput=" + this.realShaderGiOutput
                + ",cpuDenoiseScaffoldOutput=" + this.cpuDenoiseScaffoldOutput
                + ",realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + ",cacheUsable=" + this.cacheUsable
                + ",sourceDirectInputReady=" + this.sourceDirectLightingReady
                + ",source=\"" + this.sourceDebugLabel + "\""
                + ",reason=\"" + this.reason + "\"";
    }

    public String surfaceProofSummary() {
        return "emissiveProximityAvailable=" + this.emissiveProximityAvailable
                + ",affectedSurfaceRegionAvailable=" + this.affectedSurfaceRegionAvailable
                + ",affectedSurfaceRegion=\"" + this.affectedSurfaceRegionLabel + "\""
                + ",handHudExcludedProofHint=" + this.handHudExcludedProofHint
                + ",surfaceOnlyProofEligible=" + this.surfaceOnlyProofEligible
                + ",realSurfaceOnlyProofReady=" + this.readyForRealSurfaceOnlyProof();
    }

    public String outputAuthenticitySummary() {
        return "giOutputAuthenticNativeCpu=" + this.giOutputAuthenticNativeCpu
                + ",realShaderGiOutput=" + this.realShaderGiOutput
                + ",cpuDenoiseScaffoldOutput=" + this.cpuDenoiseScaffoldOutput
                + ",realDenoiseShaderOutput=" + this.realDenoiseShaderOutput
                + "," + this.sourceBoundarySummary();
    }

    public String sourceBoundarySummary() {
        return "nativeSceneTiedGiCpuReadback=" + this.giOutputAuthenticNativeCpu
                + ",physicalGiSceneLinkedCpuMetrics=" + this.readyForRealSurfaceOnlyProof()
                + ",physicalGiTracingQuality=open"
                + ",shaderGiOutput=" + this.realShaderGiOutput
                + ",cpuReadbackDenoise=" + this.cpuDenoiseScaffoldOutput
                + ",shaderDenoiseOutput=" + this.realDenoiseShaderOutput
                + ",previewBoundary=scene-linked CPU/readback physical metrics require controller screenshot proof and are not production-quality final lighting";
    }

    private static String previewReason(
            boolean diffuseGiEnabled,
            boolean cacheEnabled,
            int gridWidth,
            int gridHeight,
            int rayCount,
            int cacheReadCount,
            boolean sourceDirectLightingReady,
            String sourceDebugLabel
    ) {
        if (!diffuseGiEnabled) {
            return "Round 6 diffuse GI dispatch metadata is not enabled or not schedule-ready";
        }
        if (gridWidth <= 0 || gridHeight <= 0) {
            return "Round 6 diffuse GI grid dimensions are unavailable";
        }
        if (rayCount <= 0) {
            return "Round 6 diffuse GI dispatch has no rays";
        }
        if (!sourceDirectLightingReady) {
            return "Round 6 diffuse GI source summary does not yet report diffuse-GI source-input readiness: "
                    + (sourceDebugLabel == null || sourceDebugLabel.isBlank() ? "unavailable" : sourceDebugLabel.trim());
        }
        if (!cacheEnabled) {
            return "Round 6 diffuse GI scene-tied CPU/readback source is preview-ready with nonzero grid, rays, and source-input readiness; sparse cache record/write proof remains separate";
        }
        return "Round 6 diffuse GI/cache scene-tied CPU/readback source is enabled with nonzero grid, rays, sparse cache records, and source-input readiness";
    }

    private static boolean[] proofFlags(NativeDiffuseGiPlanUpload plan) {
        int[] flags = plan.proofFlags();
        boolean[] result = new boolean[NativeDiffuseGiPlanUpload.PROOF_FLAG_STRIDE];
        for (int index = 0; index < result.length && index < flags.length; index++) {
            result[index] = flags[index] != 0;
        }
        return result;
    }

    private static boolean[] denoiseFlags(NativeDiffuseGiPlanUpload plan) {
        int[] flags = plan.denoiseOutputFlags();
        boolean[] result = new boolean[NativeDiffuseGiPlanUpload.DENOISE_OUTPUT_FLAG_STRIDE];
        for (int index = 0; index < result.length && index < flags.length; index++) {
            result[index] = flags[index] != 0;
        }
        return result;
    }
}
