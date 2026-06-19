package net.lucerna.render.lighting.post;

import net.lucerna.lighting.DenoiseEdgeRejectionInputs;
import net.lucerna.lighting.DenoiseHistoryCounters;
import net.lucerna.lighting.DenoiseOutputContract;
import net.lucerna.lighting.DenoiseSignalInputContract;
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
                    historyRejection.writesRejectionMask()
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
        SignalSeparatedDenoiseContract signalContract = buildSignalContract(
                resolvedSettings.enabled(),
                resolvedInputs,
                outputGeneration,
                resolvedSettings.enabled(),
                historyRejection.writesRejectionMask()
        );
        return new DenoisePassPlan(
                resolvedSettings,
                historyRejection,
                resolvedInputs,
                signalContract,
                outputGeneration,
                PostProcessingValidator.validateDenoise(
                        resolvedSettings,
                        historyRejection,
                        resolvedInputs,
                        outputGeneration
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

    public boolean edgeRejectionMetadataAvailable() {
        return this.signalContract.edgeRejectionMetadataAvailable();
    }

    public boolean temporalReuseAllowed() {
        return this.historyRejection.temporalReuseAllowed();
    }

    public List<String> readResources() {
        return PostProcessingResourceContract.DENOISE_READS;
    }

    public List<String> writeResources() {
        return this.enabled() ? PostProcessingResourceContract.DENOISE_WRITES : List.of();
    }

    private static SignalSeparatedDenoiseContract buildSignalContract(
            boolean enabled,
            DenoiseInputContract inputs,
            long outputGeneration,
            boolean writesDiffuseOutput,
            boolean writesRejectionMask
    ) {
        DenoiseInputContract resolvedInputs = inputs == null ? DenoiseInputContract.empty() : inputs;
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
                DenoiseHistoryCounters.none(),
                output,
                null
        );
    }
}
