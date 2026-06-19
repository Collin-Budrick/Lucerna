package net.lucerna.render.lighting.gi;

import net.lucerna.render.frame.FrameJitter;
import net.lucerna.render.frame.FrameMatrixHistory;
import net.lucerna.render.frame.LucernaFrameConstants;

import java.util.Objects;

public record TemporalAccumulationInput(
        long frameIndex,
        long previousFrameIndex,
        long cacheGeneration,
        boolean reuseAllowed,
        boolean historyReset,
        String resetReason,
        FrameJitter jitter,
        float blendFactor,
        float confidenceFloor,
        int maxHistoryFrames
) {
    public TemporalAccumulationInput {
        frameIndex = Math.max(0L, frameIndex);
        previousFrameIndex = Math.max(0L, previousFrameIndex);
        cacheGeneration = Math.max(0L, cacheGeneration);
        if (reuseAllowed && (frameIndex == 0L || previousFrameIndex == 0L || previousFrameIndex >= frameIndex)) {
            reuseAllowed = false;
        }
        if (jitter == null) {
            jitter = FrameJitter.disabled();
        }
        blendFactor = clampUnit(blendFactor);
        confidenceFloor = clampUnit(confidenceFloor);
        maxHistoryFrames = Math.max(0, maxHistoryFrames);
        if (historyReset) {
            resetReason = clean(resetReason, "Temporal GI history reset.");
            reuseAllowed = false;
        } else {
            resetReason = clean(resetReason, "");
        }
    }

    public static TemporalAccumulationInput unavailable(String reason) {
        return new TemporalAccumulationInput(
                0L,
                0L,
                0L,
                false,
                true,
                reason,
                FrameJitter.disabled(),
                0.0F,
                1.0F,
                0
        );
    }

    public static TemporalAccumulationInput from(
            LucernaFrameConstants constants,
            FrameMatrixHistory matrixHistory,
            DiffuseGiSettings settings,
            GiCacheSnapshot cacheSnapshot
    ) {
        LucernaFrameConstants resolvedConstants = constants == null ? LucernaFrameConstants.unavailable() : constants;
        DiffuseGiSettings resolvedSettings = settings == null ? DiffuseGiSettings.disabled() : settings;
        GiCacheSnapshot resolvedCacheSnapshot = cacheSnapshot == null ? GiCacheSnapshot.empty() : cacheSnapshot;
        FrameMatrixHistory resolvedMatrixHistory = matrixHistory == null
                ? FrameMatrixHistory.unavailable("No matrix history supplied for temporal GI.")
                : matrixHistory;

        boolean hasCachedHistory = resolvedCacheSnapshot.hasRadianceRecords() || resolvedCacheSnapshot.hasSurfaceRecords();
        boolean reuseAllowed = resolvedSettings.temporalAccumulationEnabled()
                && hasCachedHistory
                && resolvedConstants.hasFrameIndex()
                && resolvedMatrixHistory.temporalReuseAllowed();
        boolean historyReset = !reuseAllowed;
        String resetReason = resetReason(resolvedMatrixHistory, resolvedSettings, hasCachedHistory, resolvedConstants);

        return new TemporalAccumulationInput(
                resolvedConstants.frameIndex(),
                resolvedMatrixHistory.previousFrameIndex(),
                resolvedCacheSnapshot.cacheGeneration(),
                reuseAllowed,
                historyReset,
                resetReason,
                resolvedConstants.jitter(),
                resolvedSettings.temporalBlendFactor(),
                resolvedSettings.historyConfidenceFloor(),
                resolvedSettings.maxTemporalFrames()
        );
    }

    public boolean hasHistoryFrame() {
        return this.previousFrameIndex > 0L && this.cacheGeneration > 0L;
    }

    public boolean accumulates() {
        return this.reuseAllowed && this.hasHistoryFrame() && this.blendFactor > 0.0F;
    }

    public String stateLabel() {
        if (this.accumulates()) {
            return "accumulating";
        }
        if (this.historyReset) {
            return "reset";
        }
        return "current-only";
    }

    private static String resetReason(
            FrameMatrixHistory matrixHistory,
            DiffuseGiSettings settings,
            boolean hasCachedHistory,
            LucernaFrameConstants constants
    ) {
        Objects.requireNonNull(matrixHistory, "matrixHistory");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(constants, "constants");
        if (!settings.temporalAccumulationEnabled()) {
            return "Temporal GI accumulation is disabled by settings.";
        }
        if (!constants.hasFrameIndex()) {
            return "Frame index is unavailable for temporal GI.";
        }
        if (!hasCachedHistory) {
            return "No GI cache history is available.";
        }
        if (matrixHistory.historyReset()) {
            return matrixHistory.resetReason();
        }
        if (!matrixHistory.temporalReuseAllowed()) {
            return "Matrix history is not reusable for temporal GI.";
        }
        return "";
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
