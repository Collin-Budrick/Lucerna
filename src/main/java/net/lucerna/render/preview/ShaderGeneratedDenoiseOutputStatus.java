package net.lucerna.render.preview;

public record ShaderGeneratedDenoiseOutputStatus(
        boolean outputPassAttempted,
        boolean outputTextureAllocated,
        boolean outputRenderPassSubmitted,
        boolean shaderGeneratedOutputImageReady,
        boolean outputConsumedByFinalComposite,
        boolean cpuReadbackFallbackInactive,
        boolean stillNotComputeBoundary,
        String boundaryReason,
        String passIdentity,
        boolean publicMojangFragmentOutput,
        boolean colorAttachmentWrite,
        boolean computeDispatch,
        boolean storageImageWrite,
        boolean rawDiffuseGiOnlySampler,
        boolean depthMaterialInputsBound,
        String inputPreservationMode
) {
    private static final String DEFAULT_BOUNDARY =
            "shader-generated denoise output is a public Mojang fragment color-attachment path, not compute/storage-image denoise";
    private static final String DEFAULT_PASS_IDENTITY =
            "public-mojang-fragment-denoise-output";
    private static final String DEFAULT_INPUT_PRESERVATION =
            "single InSampler raw-diffuse edge-aware fallback; depth/normal/material/history sampler preservation pending";

    public ShaderGeneratedDenoiseOutputStatus {
        if (boundaryReason == null || boundaryReason.isBlank()) {
            boundaryReason = stillNotComputeBoundary
                    ? DEFAULT_BOUNDARY
                    : "compute boundary not asserted by this status";
        } else {
            boundaryReason = boundaryReason.trim();
        }
        if (passIdentity == null || passIdentity.isBlank()) {
            passIdentity = DEFAULT_PASS_IDENTITY;
        } else {
            passIdentity = passIdentity.trim();
        }
        if (inputPreservationMode == null || inputPreservationMode.isBlank()) {
            inputPreservationMode = DEFAULT_INPUT_PRESERVATION;
        } else {
            inputPreservationMode = inputPreservationMode.trim();
        }
        colorAttachmentWrite = colorAttachmentWrite && outputRenderPassSubmitted;
        computeDispatch = computeDispatch && !publicMojangFragmentOutput;
        storageImageWrite = storageImageWrite && computeDispatch;
        depthMaterialInputsBound = depthMaterialInputsBound && rawDiffuseGiOnlySampler;
    }

    public static ShaderGeneratedDenoiseOutputStatus none() {
        return new ShaderGeneratedDenoiseOutputStatus(
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                "shader-generated denoise output pass has not produced owned output",
                DEFAULT_PASS_IDENTITY,
                false,
                false,
                false,
                false,
                false,
                false,
                DEFAULT_INPUT_PRESERVATION
        );
    }

    public static ShaderGeneratedDenoiseOutputStatus reported(
            boolean outputPassAttempted,
            boolean outputTextureAllocated,
            boolean outputRenderPassSubmitted,
            boolean shaderGeneratedOutputImageReady,
            boolean outputConsumedByFinalComposite,
            boolean cpuReadbackFallbackInactive,
            boolean stillNotComputeBoundary,
            String boundaryReason
    ) {
        boolean fragmentOutput = outputPassAttempted || outputRenderPassSubmitted || shaderGeneratedOutputImageReady;
        return new ShaderGeneratedDenoiseOutputStatus(
                outputPassAttempted,
                outputTextureAllocated,
                outputRenderPassSubmitted,
                shaderGeneratedOutputImageReady,
                outputConsumedByFinalComposite,
                cpuReadbackFallbackInactive,
                stillNotComputeBoundary,
                boundaryReason,
                DEFAULT_PASS_IDENTITY,
                fragmentOutput,
                outputRenderPassSubmitted,
                false,
                false,
                fragmentOutput,
                false,
                DEFAULT_INPUT_PRESERVATION
        );
    }

    public static ShaderGeneratedDenoiseOutputStatus reported(
            boolean outputPassAttempted,
            boolean outputTextureAllocated,
            boolean outputRenderPassSubmitted,
            boolean shaderGeneratedOutputImageReady,
            boolean outputConsumedByFinalComposite,
            boolean cpuReadbackFallbackInactive,
            boolean stillNotComputeBoundary,
            String boundaryReason,
            String passIdentity,
            boolean publicMojangFragmentOutput,
            boolean colorAttachmentWrite,
            boolean computeDispatch,
            boolean storageImageWrite,
            boolean rawDiffuseGiOnlySampler,
            boolean depthMaterialInputsBound,
            String inputPreservationMode
    ) {
        return new ShaderGeneratedDenoiseOutputStatus(
                outputPassAttempted,
                outputTextureAllocated,
                outputRenderPassSubmitted,
                shaderGeneratedOutputImageReady,
                outputConsumedByFinalComposite,
                cpuReadbackFallbackInactive,
                stillNotComputeBoundary,
                boundaryReason,
                passIdentity,
                publicMojangFragmentOutput,
                colorAttachmentWrite,
                computeDispatch,
                storageImageWrite,
                rawDiffuseGiOnlySampler,
                depthMaterialInputsBound,
                inputPreservationMode
        );
    }

    public boolean ownedOutputImageReady() {
        return this.outputTextureAllocated && this.shaderGeneratedOutputImageReady;
    }

    public boolean generatedOutputPassSubmitted() {
        return this.outputPassAttempted && this.outputRenderPassSubmitted;
    }

    public boolean finalCompositeConsumptionReady() {
        return this.outputConsumedByFinalComposite && this.cpuReadbackFallbackInactive;
    }

    public boolean fragmentOutputReady() {
        return this.publicMojangFragmentOutput
                && this.colorAttachmentWrite
                && !this.computeDispatch
                && !this.storageImageWrite;
    }

    public boolean realShaderDenoiseOutputReady() {
        return this.ownedOutputImageReady()
                && this.generatedOutputPassSubmitted()
                && this.finalCompositeConsumptionReady()
                && this.fragmentOutputReady();
    }

    public boolean partialEvidencePresent() {
        return this.outputPassAttempted
                || this.outputTextureAllocated
                || this.outputRenderPassSubmitted
                || this.shaderGeneratedOutputImageReady
                || this.outputConsumedByFinalComposite
                || this.cpuReadbackFallbackInactive;
    }

    public String boundarySummary() {
        return "outputPassAttempted=" + this.outputPassAttempted
                + ",outputTextureAllocated=" + this.outputTextureAllocated
                + ",outputRenderPassSubmitted=" + this.outputRenderPassSubmitted
                + ",shaderGeneratedOutputImageReady=" + this.shaderGeneratedOutputImageReady
                + ",ownedOutputImageReady=" + this.ownedOutputImageReady()
                + ",outputConsumedByFinalComposite=" + this.outputConsumedByFinalComposite
                + ",cpuReadbackFallbackInactive=" + this.cpuReadbackFallbackInactive
                + ",realShaderDenoiseOutputReady=" + this.realShaderDenoiseOutputReady()
                + ",stillNotComputeBoundary=" + this.stillNotComputeBoundary
                + ",passIdentity=\"" + this.passIdentity + "\""
                + ",publicMojangFragmentOutput=" + this.publicMojangFragmentOutput
                + ",colorAttachmentWrite=" + this.colorAttachmentWrite
                + ",computeDispatch=" + this.computeDispatch
                + ",storageImageWrite=" + this.storageImageWrite
                + ",rawDiffuseGiOnlySampler=" + this.rawDiffuseGiOnlySampler
                + ",depthMaterialInputsBound=" + this.depthMaterialInputsBound
                + ",inputPreservationMode=\"" + this.inputPreservationMode + "\""
                + ",boundary=\"" + this.boundaryReason + "\"";
    }
}
