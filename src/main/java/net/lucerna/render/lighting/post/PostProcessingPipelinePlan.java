package net.lucerna.render.lighting.post;

import java.util.Objects;

public record PostProcessingPipelinePlan(
        DenoisePassPlan denoisePlan,
        FinalCompositeHandoff compositeHandoff,
        PostProcessingValidationReport validationReport
) {
    public PostProcessingPipelinePlan {
        Objects.requireNonNull(denoisePlan, "denoisePlan");
        Objects.requireNonNull(compositeHandoff, "compositeHandoff");
        if (validationReport == null) {
            validationReport = PostProcessingValidationReport.merge(
                    denoisePlan.validationReport(),
                    PostProcessingValidator.validateComposite(denoisePlan, compositeHandoff)
            );
        }
    }

    public static PostProcessingPipelinePlan from(
            DenoisePassPlan denoisePlan,
            FinalCompositeHandoff compositeHandoff
    ) {
        Objects.requireNonNull(denoisePlan, "denoisePlan");
        Objects.requireNonNull(compositeHandoff, "compositeHandoff");
        return new PostProcessingPipelinePlan(
                denoisePlan,
                compositeHandoff,
                PostProcessingValidationReport.merge(
                        denoisePlan.validationReport(),
                        PostProcessingValidator.validateComposite(denoisePlan, compositeHandoff)
                )
        );
    }

    public boolean valid() {
        return this.validationReport.valid();
    }

    public boolean readyForNativeHandoff() {
        return this.valid() && this.compositeHandoff.readyForWorldColorHandoff();
    }

    public boolean denoiseScheduled() {
        return this.denoisePlan.readyForScheduling();
    }

    public boolean shaderDenoiseContractReady() {
        return this.denoisePlan.shaderDenoiseContractReady();
    }

    public boolean realDenoiseShaderOutput() {
        return this.denoisePlan.realDenoiseShaderOutput();
    }

    public String shaderDenoiseStatusSummary() {
        return this.denoisePlan.shaderDenoiseStatusSummary();
    }
}
