package net.lucerna.render.lighting.post;

import net.lucerna.lighting.DenoiseEdgeRejectionInputs;
import net.lucerna.lighting.DenoiseOutputContract;
import net.lucerna.lighting.DenoiseSignalInputContract;
import net.lucerna.lighting.ShaderDenoiseOutputContract;
import net.lucerna.lighting.SignalSeparatedDenoiseContract;
import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.resources.ShaderPassId;

import java.util.List;
import java.util.Objects;

public record DenoisePassPlan(
        EdgeAwareDenoiseSettings settings,
        HistoryRejectionPlan historyRejection,
        DenoiseInputContract inputs,
        SignalSeparatedDenoiseContract signalContract,
        ShaderDenoiseOutputContract shaderOutputContract,
        long outputGeneration,
        PostProcessingValidationReport validationReport
) {
    public DenoisePassPlan {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(historyRejection, "historyRejection");
        Objects.requireNonNull(inputs, "inputs");
        outputGeneration = Math.max(0L, outputGeneration);
        if (signalContract == null) {
            signalContract = buildSignalContract(
                    settings.enabled(),
                    inputs,
                    outputGeneration,
                    settings.enabled(),
                    historyRejection.writesRejectionMask(),
                    historyRejection
            );
        }
        if (shaderOutputContract == null) {
            shaderOutputContract = buildShaderOutputContract(
                    settings.enabled(),
                    inputs,
                    outputGeneration,
                    historyRejection
            );
        }
        if (historyRejection.frameIndex() != inputs.frameIndex()) {
            throw new IllegalArgumentException("historyRejection frameIndex must match denoise inputs");
        }
        if (validationReport == null) {
            validationReport = PostProcessingValidator.validateDenoise(
                    settings,
                    historyRejection,
                    inputs,
                    outputGeneration
            );
        }
    }

    public static DenoisePassPlan create(
            EdgeAwareDenoiseSettings settings,
            HistoryRejectionSettings historyRejectionSettings,
            DenoiseInputContract inputs,
            long outputGeneration
    ) {
        EdgeAwareDenoiseSettings resolvedSettings = settings == null
                ? EdgeAwareDenoiseSettings.disabled()
                : settings;
        DenoiseInputContract resolvedInputs = inputs == null ? DenoiseInputContract.empty() : inputs;
        HistoryRejectionPlan historyRejection = HistoryRejectionPlan.from(
                historyRejectionSettings,
                resolvedInputs
        );
        long resolvedOutputGeneration = Math.max(0L, outputGeneration);
        SignalSeparatedDenoiseContract signalContract = buildSignalContract(
                resolvedSettings.enabled(),
                resolvedInputs,
                resolvedOutputGeneration,
                resolvedSettings.enabled(),
                historyRejection.writesRejectionMask(),
                historyRejection
        );
        return new DenoisePassPlan(
                resolvedSettings,
                historyRejection,
                resolvedInputs,
                signalContract,
                buildShaderOutputContract(
                        resolvedSettings.enabled(),
                        resolvedInputs,
                        resolvedOutputGeneration,
                        historyRejection
                ),
                resolvedOutputGeneration,
                PostProcessingValidator.validateDenoise(
                        resolvedSettings,
                        historyRejection,
                        resolvedInputs,
                        resolvedOutputGeneration
                )
        );
    }

    public ShaderPassId passId() {
        return PostProcessingResourceContract.denoisePassId();
    }

    public int numericPassId() {
        return PostProcessingResourceContract.DENOISE_NUMERIC_PASS_ID;
    }

    public boolean enabled() {
        return this.settings.enabled();
    }

    public boolean readyForScheduling() {
        return this.enabled()
                && this.validationReport.valid()
                && this.inputs.hasRequiredDenoiseInputs();
    }

    public boolean writesDiffuseOutput() {
        return this.enabled();
    }

    public boolean writesRejectionMask() {
        return this.historyRejection.writesRejectionMask();
    }

    public boolean rawDiffuseGiInputAvailable() {
        return this.signalContract.rawDiffuseGiInputAvailable();
    }

    public boolean denoisedDiffuseOutputIntended() {
        return this.signalContract.denoisedDiffuseOutputIntended();
    }

    public boolean shaderDenoiseContractReady() {
        return this.shaderOutputContract.contractReady();
    }

    public boolean shaderDenoiseDispatchPathImplemented() {
        return this.shaderOutputContract.dispatchPathImplemented();
    }

    public boolean realDenoiseShaderOutput() {
        return this.shaderOutputContract.realDenoiseShaderOutput();
    }

    public boolean readyForControllerShaderDenoiseProof() {
        return this.shaderOutputContract.readyForControllerShaderProof();
    }

    public boolean shaderDenoiseInputsCompleteForDispatch() {
        return this.shaderOutputContract.shaderDenoiseInputsCompleteForDispatch();
    }

    public String shaderDenoiseReadinessReason() {
        return this.shaderOutputContract.readinessReason();
    }

    public String shaderDenoiseStatusSummary() {
        return this.shaderOutputContract.statusSummary();
    }

    public String shaderDenoisePendingChecklist() {
        return this.shaderOutputContract.pendingChecklist();
    }

    public String shaderDenoiseQualityBoundarySummary() {
        return this.shaderOutputContract.qualityBoundarySummary();
    }

    public boolean edgeRejectionMetadataAvailable() {
        return this.signalContract.edgeRejectionMetadataAvailable();
    }

    public boolean temporalReuseAllowed() {
        return this.historyRejection.temporalReuseAllowed();
    }

    public HistoryConfidenceSummary historyConfidenceSummary() {
        return this.historyRejection.confidenceSummary(this.inputs);
    }

    public String round8HistoryTelemetrySummary() {
        return this.historyConfidenceSummary().compactSummary();
    }

    public List<String> readResources() {
        return PostProcessingResourceContract.DENOISE_READS;
    }

    public List<String> writeResources() {
        return this.enabled() ? PostProcessingResourceContract.DENOISE_WRITES : List.of();
    }

    public List<String> contractResources() {
        return PostProcessingResourceContract.DENOISE_CONTRACT_RESOURCES;
    }

    private static ShaderDenoiseOutputContract buildShaderOutputContract(
            boolean enabled,
            DenoiseInputContract inputs,
            long outputGeneration,
            HistoryRejectionPlan historyRejection
    ) {
        DenoiseInputContract resolvedInputs = inputs == null ? DenoiseInputContract.empty() : inputs;
        HistoryRejectionPlan resolvedHistoryRejection = historyRejection == null
                ? HistoryRejectionPlan.from(HistoryRejectionSettings.disabled(), resolvedInputs)
                : historyRejection;
        GBufferDescriptor gBuffer = resolvedInputs.gBuffer();
        boolean contractReady = enabled
                && resolvedInputs.hasRequiredDenoiseInputs()
                && outputGeneration >= resolvedInputs.maxInputGeneration();
        boolean temporalInputsBound = resolvedInputs.hasHistoryInputs()
                && resolvedHistoryRejection.temporalReuseAllowed();
        boolean historyRejectionInputsBound = resolvedHistoryRejection.enabled()
                && resolvedInputs.disocclusionMaskInputsAvailable()
                && resolvedInputs.previousDepthAvailable()
                && resolvedInputs.previousNormalRoughnessAvailable()
                && resolvedInputs.previousLightingAvailable();
        boolean varianceInputsBound = resolvedInputs.cacheConfidenceAvailable()
                || resolvedInputs.varianceMapInputsAvailable();
        boolean confidenceInputsBound = resolvedInputs.cacheConfidenceAvailable()
                && resolvedInputs.historyConfidenceMapInputsAvailable();
        String executionBoundary = contractReady
                ? "contract-only shader resource is declared; scheduler dispatch, descriptor binding, and writable output are still pending"
                : "shader denoise resource contract is not schedulable yet";
        String qualityBoundary = "raw GI, denoised GI, rejection mask, variance, and history confidence must be captured from shader-written resources before quality can be claimed";
        String pendingReason = contractReady
                ? "shader-side denoise dispatch/output path is pending; CPU/readback denoise evidence remains separate"
                : "shader-side denoise contract awaits enabled settings, required inputs, and fresh output generation";
        return ShaderDenoiseOutputContract.contractOnly(
                contractReady,
                resolvedInputs.hasEdgeAwareInputs(),
                resolvedInputs.hasEdgeAwareInputs() && gBuffer.hasMaterialIds(),
                temporalInputsBound,
                historyRejectionInputsBound,
                varianceInputsBound,
                confidenceInputsBound,
                outputGeneration,
                gBuffer.width(),
                gBuffer.height(),
                executionBoundary,
                qualityBoundary,
                pendingReason
        );
    }

    private static SignalSeparatedDenoiseContract buildSignalContract(
            boolean enabled,
            DenoiseInputContract inputs,
            long outputGeneration,
            boolean writesDiffuseOutput,
            boolean writesRejectionMask,
            HistoryRejectionPlan historyRejection
    ) {
        DenoiseInputContract resolvedInputs = inputs == null ? DenoiseInputContract.empty() : inputs;
        HistoryRejectionPlan resolvedHistoryRejection = historyRejection == null
                ? HistoryRejectionPlan.from(HistoryRejectionSettings.disabled(), resolvedInputs)
                : historyRejection;
        HistoryConfidenceSummary historySummary = resolvedHistoryRejection.confidenceSummary(resolvedInputs);
        GBufferDescriptor gBuffer = resolvedInputs.gBuffer();
        int width = gBuffer.width();
        int height = gBuffer.height();
        DenoiseSignalInputContract diffuseGi = DenoiseSignalInputContract.diffuseGi(
                resolvedInputs.diffuseGiAvailable(),
                resolvedInputs.diffuseGiGeneration(),
                width,
                height,
                0,
                0,
                0,
                resolvedInputs.diffuseGiAvailable()
                        ? "raw diffuse GI input is available for denoise"
                        : "raw diffuse GI input is unavailable"
        );
        DenoiseSignalInputContract directShadows = DenoiseSignalInputContract.directShadows(
                resolvedInputs.directLightingAvailable(),
                resolvedInputs.directLightingGeneration(),
                width,
                height,
                0,
                0,
                resolvedInputs.directLightingAvailable()
                        ? "raw direct lighting/shadow input is available for denoise"
                        : "raw direct lighting/shadow input is unavailable"
        );
        DenoiseEdgeRejectionInputs edgeInputs = new DenoiseEdgeRejectionInputs(
                gBuffer.hasDepth(),
                gBuffer.hasNormals(),
                gBuffer.hasMaterialIds(),
                resolvedInputs.motionHistoryAvailable(),
                resolvedInputs.previousDepthAvailable(),
                resolvedInputs.previousNormalRoughnessAvailable(),
                gBuffer.hasMaterialIds() && resolvedInputs.previousNormalRoughnessAvailable(),
                resolvedInputs.previousLightingAvailable(),
                resolvedInputs.maxInputGeneration(),
                resolvedInputs.historyGeneration(),
                null
        );
        DenoiseOutputContract output = DenoiseOutputContract.diffuseOutputIntent(
                writesDiffuseOutput,
                false,
                writesRejectionMask,
                outputGeneration,
                width,
                height,
                writesDiffuseOutput
                        ? "denoised diffuse output is planned but not visually rendered by this Java contract"
                        : "denoised diffuse output is not planned"
        );
        return new SignalSeparatedDenoiseContract(
                resolvedInputs.frameIndex(),
                Math.max(outputGeneration, resolvedInputs.maxInputGeneration()),
                width,
                height,
                enabled,
                diffuseGi,
                directShadows,
                null,
                null,
                edgeInputs,
                historySummary.historyCounters(),
                output,
                "Round 8 variance/history confidence contract; "
                        + historySummary.compactSummary()
                        + "; realDenoiseShaderOutput=false; shaderDenoiseDispatchPath=pending"
        );
    }
}
