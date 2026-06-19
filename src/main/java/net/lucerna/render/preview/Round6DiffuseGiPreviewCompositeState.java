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
                    ? "Round 6 diffuse GI/cache metadata is ready for final-composite preview"
                    : "Round 6 diffuse GI/cache metadata is not ready for final-composite preview";
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

    public boolean readyForFinalComposite(Round6DiffuseGiCpuOutputPayload sourcePayload) {
        return readyForRound6PreviewSource()
                && sourcePayload != null
                && sourcePayload.readyForPreviewDraw();
    }

    public String finalCompositeReadinessReason(Round6DiffuseGiCpuOutputPayload sourcePayload) {
        if (!readyForRound6PreviewSource()) {
            return this.reason;
        }
        if (sourcePayload == null) {
            return "Round 6 diffuse GI/cache metadata is ready, but no native GI preview payload is available";
        }
        if (!sourcePayload.readyForPreviewDraw()) {
            return "Round 6 diffuse GI/cache metadata is ready, but the native GI preview payload is not displayable: "
                    + sourcePayload.previewReadinessReason();
        }
        return "Round 6 diffuse GI/cache metadata and native GI RGBA8 payload are ready for GI-specific final-composite preview";
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
                + ",cacheUsable=" + this.cacheUsable
                + ",sourceDirectLightingReady=" + this.sourceDirectLightingReady
                + ",source=\"" + this.sourceDebugLabel + "\""
                + ",reason=\"" + this.reason + "\"";
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
            return "Round 6 diffuse GI source summary does not yet report direct-light readiness: "
                    + (sourceDebugLabel == null || sourceDebugLabel.isBlank() ? "unavailable" : sourceDebugLabel.trim());
        }
        if (!cacheEnabled) {
            return "Round 6 diffuse GI metadata is preview-ready with nonzero grid, rays, and direct-light source readiness; sparse cache record/write proof remains separate";
        }
        return "Round 6 diffuse GI/cache metadata is enabled with nonzero grid, rays, sparse cache records, and direct-light source readiness";
    }
}
