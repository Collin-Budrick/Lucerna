package net.lucerna.render.lighting.post;

import net.lucerna.render.resources.ShaderPassId;

import java.util.List;
import java.util.Objects;

public record DenoisePassPlan(
        EdgeAwareDenoiseSettings settings,
        HistoryRejectionPlan historyRejection,
        DenoiseInputContract inputs,
        long outputGeneration,
        PostProcessingValidationReport validationReport
) {
    public DenoisePassPlan {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(historyRejection, "historyRejection");
        Objects.requireNonNull(inputs, "inputs");
        outputGeneration = Math.max(0L, outputGeneration);
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
        return new DenoisePassPlan(
                resolvedSettings,
                historyRejection,
                resolvedInputs,
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

    public boolean temporalReuseAllowed() {
        return this.historyRejection.temporalReuseAllowed();
    }

    public List<String> readResources() {
        return PostProcessingResourceContract.DENOISE_READS;
    }

    public List<String> writeResources() {
        return this.enabled() ? PostProcessingResourceContract.DENOISE_WRITES : List.of();
    }
}
