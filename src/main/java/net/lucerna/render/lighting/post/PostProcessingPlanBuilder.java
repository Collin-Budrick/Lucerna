package net.lucerna.render.lighting.post;

import net.lucerna.config.QualityPreset;
import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.frame.FrameMatrixHistory;
import net.lucerna.render.frame.LucernaFrameConstants;

import java.util.Objects;

public final class PostProcessingPlanBuilder {
    private EdgeAwareDenoiseSettings denoiseSettings = EdgeAwareDenoiseSettings.forPreset(QualityPreset.BALANCED);
    private HistoryRejectionSettings historyRejectionSettings = HistoryRejectionSettings.forPreset(QualityPreset.BALANCED);
    private DenoiseInputContract denoiseInputs = DenoiseInputContract.empty();
    private long outputGeneration;
    private boolean debugOverlayAvailable;
    private boolean borrowedWorldColorTarget = true;
    private boolean beforeHudAndLateTranslucency = true;

    private PostProcessingPlanBuilder() {
    }

    public static PostProcessingPlanBuilder create() {
        return new PostProcessingPlanBuilder();
    }

    public PostProcessingPlanBuilder qualityPreset(QualityPreset preset) {
        this.denoiseSettings = EdgeAwareDenoiseSettings.forPreset(preset);
        this.historyRejectionSettings = HistoryRejectionSettings.forPreset(preset);
        return this;
    }

    public PostProcessingPlanBuilder denoiseSettings(EdgeAwareDenoiseSettings denoiseSettings) {
        this.denoiseSettings = Objects.requireNonNull(denoiseSettings, "denoiseSettings");
        return this;
    }

    public PostProcessingPlanBuilder historyRejectionSettings(HistoryRejectionSettings historyRejectionSettings) {
        this.historyRejectionSettings = Objects.requireNonNull(historyRejectionSettings, "historyRejectionSettings");
        return this;
    }

    public PostProcessingPlanBuilder denoiseInputs(DenoiseInputContract denoiseInputs) {
        this.denoiseInputs = Objects.requireNonNull(denoiseInputs, "denoiseInputs");
        return this;
    }

    public PostProcessingPlanBuilder frameInputs(
            LucernaFrameConstants constants,
            FrameMatrixHistory matrixHistory,
            GBufferDescriptor gBuffer,
            boolean directLightingAvailable,
            boolean diffuseGiAvailable,
            boolean cacheConfidenceAvailable,
            long directLightingGeneration,
            long diffuseGiGeneration,
            long historyGeneration
    ) {
        this.denoiseInputs = DenoiseInputContract.fromFrame(
                constants,
                matrixHistory,
                gBuffer,
                directLightingAvailable,
                diffuseGiAvailable,
                cacheConfidenceAvailable,
                directLightingGeneration,
                diffuseGiGeneration,
                historyGeneration
        );
        return this;
    }

    public PostProcessingPlanBuilder outputGeneration(long outputGeneration) {
        this.outputGeneration = Math.max(0L, outputGeneration);
        return this;
    }

    public PostProcessingPlanBuilder debugOverlayAvailable(boolean debugOverlayAvailable) {
        this.debugOverlayAvailable = debugOverlayAvailable;
        return this;
    }

    public PostProcessingPlanBuilder borrowedWorldColorTarget(boolean borrowedWorldColorTarget) {
        this.borrowedWorldColorTarget = borrowedWorldColorTarget;
        return this;
    }

    public PostProcessingPlanBuilder beforeHudAndLateTranslucency(boolean beforeHudAndLateTranslucency) {
        this.beforeHudAndLateTranslucency = beforeHudAndLateTranslucency;
        return this;
    }

    public PostProcessingPipelinePlan build() {
        long resolvedOutputGeneration = Math.max(this.outputGeneration, this.denoiseInputs.maxInputGeneration());
        DenoisePassPlan denoisePlan = DenoisePassPlan.create(
                this.denoiseSettings,
                this.historyRejectionSettings,
                this.denoiseInputs,
                resolvedOutputGeneration
        );
        FinalCompositeHandoff compositeHandoff = FinalCompositeHandoff.fromDenoisePlan(
                denoisePlan,
                this.debugOverlayAvailable,
                this.borrowedWorldColorTarget,
                this.beforeHudAndLateTranslucency
        );
        return PostProcessingPipelinePlan.from(denoisePlan, compositeHandoff);
    }
}
