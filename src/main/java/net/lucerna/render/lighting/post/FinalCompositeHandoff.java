package net.lucerna.render.lighting.post;

import net.lucerna.render.resources.ShaderPassId;

import java.util.List;
import java.util.Objects;

public record FinalCompositeHandoff(
        long frameIndex,
        String targetName,
        String targetDescription,
        int width,
        int height,
        long sourceGeneration,
        boolean borrowedWorldColorTarget,
        boolean beforeHudAndLateTranslucency,
        boolean clearBeforeWrite,
        boolean usesDirectLighting,
        boolean usesRawDiffuseGi,
        boolean usesDenoisedDiffuse,
        boolean debugOverlayAvailable,
        List<String> readResources,
        List<String> writeResources
) {
    public FinalCompositeHandoff {
        frameIndex = Math.max(0L, frameIndex);
        targetName = requireText(targetName, "targetName");
        targetDescription = normalizeDescription(targetDescription);
        width = Math.max(0, width);
        height = Math.max(0, height);
        sourceGeneration = Math.max(0L, sourceGeneration);
        Objects.requireNonNull(readResources, "readResources");
        Objects.requireNonNull(writeResources, "writeResources");
        readResources = List.copyOf(readResources);
        writeResources = List.copyOf(writeResources);
        for (String readResource : readResources) {
            requireText(readResource, "readResources entries");
        }
        for (String writeResource : writeResources) {
            requireText(writeResource, "writeResources entries");
        }
    }

    public static FinalCompositeHandoff fromDenoisePlan(
            DenoisePassPlan denoisePlan,
            boolean debugOverlayAvailable,
            boolean borrowedWorldColorTarget,
            boolean beforeHudAndLateTranslucency
    ) {
        Objects.requireNonNull(denoisePlan, "denoisePlan");
        return new FinalCompositeHandoff(
                denoisePlan.inputs().frameIndex(),
                PostProcessingResourceContract.WORLD_COLOR,
                "borrowed Minecraft/Sodium world color target; sourceMix="
                        + sourceMixDescription(denoisePlan),
                denoisePlan.inputs().gBuffer().width(),
                denoisePlan.inputs().gBuffer().height(),
                denoisePlan.outputGeneration(),
                borrowedWorldColorTarget,
                beforeHudAndLateTranslucency,
                false,
                denoisePlan.readyForScheduling() && denoisePlan.inputs().directLightingAvailable(),
                denoisePlan.readyForScheduling() && denoisePlan.rawDiffuseGiInputAvailable(),
                denoisePlan.readyForScheduling() && denoisePlan.writesDiffuseOutput(),
                debugOverlayAvailable,
                debugOverlayAvailable
                        ? PostProcessingResourceContract.COMPOSITE_READS
                        : PostProcessingResourceContract.COMPOSITE_READS_WITHOUT_DEBUG,
                PostProcessingResourceContract.COMPOSITE_WRITES
        );
    }

    public ShaderPassId passId() {
        return PostProcessingResourceContract.compositePassId();
    }

    public int numericPassId() {
        return PostProcessingResourceContract.COMPOSITE_NUMERIC_PASS_ID;
    }

    public boolean dimensionsAvailable() {
        return this.width > 0 && this.height > 0;
    }

    public boolean writesWorldColorTarget() {
        return this.writeResources.contains(PostProcessingResourceContract.WORLD_COLOR);
    }

    public boolean blendsDirectRawAndDenoisedSources() {
        return this.usesDirectLighting && this.usesRawDiffuseGi && this.usesDenoisedDiffuse;
    }

    public String sourceMixSummary() {
        return "direct=" + sourceState(this.usesDirectLighting)
                + ",rawGi=" + sourceState(this.usesRawDiffuseGi)
                + ",denoisedGi=" + sourceState(this.usesDenoisedDiffuse)
                + ",finalBlend=" + sourceState(this.blendsDirectRawAndDenoisedSources())
                + ",boundary=planning-only; runtime CPU/readback and visual-quality evidence must come from native status and controller screenshots";
    }

    public boolean readyForWorldColorHandoff() {
        return this.frameIndex > 0L
                && this.dimensionsAvailable()
                && this.borrowedWorldColorTarget
                && this.beforeHudAndLateTranslucency
                && this.writesWorldColorTarget();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return "world color handoff";
        }
        return value.trim();
    }

    private static String sourceMixDescription(DenoisePassPlan denoisePlan) {
        boolean ready = denoisePlan.readyForScheduling();
        return "direct=" + sourceState(ready && denoisePlan.inputs().directLightingAvailable())
                + ",rawGi=" + sourceState(ready && denoisePlan.rawDiffuseGiInputAvailable())
                + ",denoisedGi=" + sourceState(ready && denoisePlan.writesDiffuseOutput())
                + ",qualityBoundary=denoise-plan-ready-is-not-proof-of-CPU-output-readback-or-denoise-quality";
    }

    private static String sourceState(boolean available) {
        return available ? "intended-ready" : "excluded-or-not-ready";
    }
}
