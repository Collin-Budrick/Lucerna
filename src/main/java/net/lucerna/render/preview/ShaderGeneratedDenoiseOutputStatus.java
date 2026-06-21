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
    private static final int MAX_BOUNDARY_REASON_CHARS = 180;
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
            boundaryReason = compactReason(boundaryReason.trim());
        }
        if (passIdentity == null || passIdentity.isBlank()) {
            passIdentity = DEFAULT_PASS_IDENTITY;
        } else {
            passIdentity = passIdentity.trim();
        }
        if (inputPreservationMode == null || inputPreservationMode.isBlank()) {
            inputPreservationMode = DEFAULT_INPUT_PRESERVATION;
        } else {
            inputPreservationMode = compactReason(inputPreservationMode.trim());
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

    public static ShaderGeneratedDenoiseOutputStatus ownedFragmentOutput(
            boolean outputRenderPassSubmitted,
            boolean shaderGeneratedOutputImageReady,
            boolean outputConsumedByFinalComposite,
            boolean strictRawDiffuseGiInput,
            boolean depthMaterialInputsBound,
            String boundaryReason
    ) {
        return new ShaderGeneratedDenoiseOutputStatus(
                true,
                true,
                outputRenderPassSubmitted,
                shaderGeneratedOutputImageReady,
                outputConsumedByFinalComposite,
                true,
                true,
                boundaryReason,
                DEFAULT_PASS_IDENTITY,
                true,
                outputRenderPassSubmitted,
                false,
                false,
                strictRawDiffuseGiInput,
                depthMaterialInputsBound,
                strictRawDiffuseGiInput
                        ? "raw-diffuse-gi-rgba8 sampler preserved into owned fragment output"
                        : DEFAULT_INPUT_PRESERVATION
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

    public boolean ownedOutputValidForPlayableProof() {
        return this.realShaderDenoiseOutputReady()
                && this.rawDiffuseGiOnlySampler
                && !this.computeDispatch
                && !this.storageImageWrite;
    }

    public boolean nativeComputeDenoiseExecuted() {
        return this.computeDispatch && this.storageImageWrite && !this.publicMojangFragmentOutput;
    }

    public boolean gpuTraversalInputExecuted() {
        return false;
    }

    public boolean noNativeComputeOverclaim() {
        return !this.nativeComputeDenoiseExecuted();
    }

    public boolean noGpuTraversalOverclaim() {
        return !this.gpuTraversalInputExecuted();
    }

    public boolean visualQualityStillBlocked() {
        return !this.depthMaterialInputsBound
                || this.stillNotComputeBoundary
                || !this.ownedOutputValidForPlayableProof();
    }

    public boolean partialEvidencePresent() {
        return this.outputPassAttempted
                || this.outputTextureAllocated
                || this.outputRenderPassSubmitted
                || this.shaderGeneratedOutputImageReady
                || this.outputConsumedByFinalComposite
                || this.cpuReadbackFallbackInactive;
    }

    public String ownedOutputValiditySummary() {
        return "ownedOutputValidForPlayableProof=" + this.ownedOutputValidForPlayableProof()
                + ",ownedOutputImageReady=" + this.ownedOutputImageReady()
                + ",generatedOutputPassSubmitted=" + this.generatedOutputPassSubmitted()
                + ",finalCompositeConsumptionReady=" + this.finalCompositeConsumptionReady()
                + ",fragmentOutputReady=" + this.fragmentOutputReady()
                + ",rawDiffuseGiOnlySampler=" + this.rawDiffuseGiOnlySampler
                + ",depthMaterialInputsBound=" + this.depthMaterialInputsBound
                + ",cpuReadbackFallbackActive=" + !this.cpuReadbackFallbackInactive
                + ",nativeComputeDenoiseExecuted=" + this.nativeComputeDenoiseExecuted()
                + ",gpuTraversalExecuted=" + this.gpuTraversalInputExecuted();
    }

    public String noOverclaimBlockerSummary() {
        StringBuilder builder = new StringBuilder();
        appendBlocker(builder, this.nativeComputeDenoiseExecuted(),
                "native compute denoise is claimed by compute/storage flags");
        appendBlocker(builder, this.gpuTraversalInputExecuted(),
                "GPU traversal input is claimed by this status");
        appendBlocker(builder, this.computeDispatch && this.publicMojangFragmentOutput,
                "compute dispatch conflicts with public Mojang fragment output identity");
        appendBlocker(builder, this.storageImageWrite && !this.computeDispatch,
                "storage image write cannot be claimed without compute dispatch");
        appendBlocker(builder, !this.rawDiffuseGiOnlySampler, "raw diffuse-GI sampler identity not preserved");
        return builder.length() == 0
                ? "none; fragment output status does not claim GPU traversal or native compute denoise"
                : builder.toString();
    }

    public String inputIdentitySummary() {
        return "shaderDenoiseReadyInputIdentity=raw-diffuse-gi-rgba8"
                + ",rawDiffuseGiOnlySampler=" + this.rawDiffuseGiOnlySampler
                + ",depthMaterialInputsBound=" + this.depthMaterialInputsBound
                + ",inputPreservationMode=\"" + this.inputPreservationMode + "\""
                + ",nativeComputeDenoiseExecuted=" + this.nativeComputeDenoiseExecuted()
                + ",gpuTraversalExecuted=" + this.gpuTraversalInputExecuted()
                + ",noNativeComputeOverclaim=" + this.noNativeComputeOverclaim()
                + ",noGpuTraversalOverclaim=" + this.noGpuTraversalOverclaim()
                + ",blockers=\"" + this.noOverclaimBlockerSummary() + "\"";
    }

    public String blockerSummary() {
        if (this.ownedOutputValidForPlayableProof() && this.depthMaterialInputsBound) {
            return "none; owned public Mojang fragment denoise output is valid for playable proof";
        }
        StringBuilder builder = new StringBuilder();
        appendBlocker(builder, !this.outputPassAttempted, "output pass not attempted");
        appendBlocker(builder, !this.outputTextureAllocated, "owned output texture not allocated");
        appendBlocker(builder, !this.outputRenderPassSubmitted, "output render pass not submitted");
        appendBlocker(builder, !this.shaderGeneratedOutputImageReady, "shader-generated output image not ready");
        appendBlocker(builder, !this.outputConsumedByFinalComposite, "output not consumed by final composite");
        appendBlocker(builder, !this.cpuReadbackFallbackInactive, "CPU readback fallback still active");
        appendBlocker(builder, !this.rawDiffuseGiOnlySampler, "strict raw diffuse-GI input not preserved");
        appendBlocker(builder, !this.depthMaterialInputsBound, "depth/material inputs not bound");
        appendBlocker(builder, this.computeDispatch || this.storageImageWrite, "status claims compute/storage-image path");
        appendBlocker(builder, this.stillNotComputeBoundary, "still fragment boundary, not compute denoise");
        return builder.length() == 0 ? "unknown shader denoise blocker" : builder.toString();
    }

    public String playableProofSummary() {
        return this.ownedOutputValiditySummary()
                + ",visualQualityStillBlocked=" + this.visualQualityStillBlocked()
                + ",passIdentity=\"" + this.passIdentity + "\""
                + ",inputIdentity=\"" + this.inputIdentitySummary() + "\""
                + ",blockers=\"" + this.blockerSummary() + "\"";
    }

    public String compactBoundarySummary() {
        return "shaderDenoiseOutputPassAttempted=" + this.outputPassAttempted
                + ",shaderDenoiseOutputTextureAllocated=" + this.outputTextureAllocated
                + ",shaderDenoiseOutputRenderPassSubmitted=" + this.outputRenderPassSubmitted
                + ",shaderGeneratedOutputImageReady=" + this.shaderGeneratedOutputImageReady
                + ",shaderDenoiseOutputConsumedByFinalComposite=" + this.outputConsumedByFinalComposite
                + ",realShaderDenoiseOutputReady=" + this.realShaderDenoiseOutputReady()
                + ",shaderDenoiseCpuReadbackFallbackActive=" + !this.cpuReadbackFallbackInactive
                + ",publicMojangFragmentOutput=" + this.publicMojangFragmentOutput
                + ",colorAttachmentWrite=" + this.colorAttachmentWrite
                + ",computeDispatch=" + this.computeDispatch
                + ",storageImageWrite=" + this.storageImageWrite
                + ",nativeComputeDenoiseExecuted=" + this.nativeComputeDenoiseExecuted()
                + ",gpuTraversalExecuted=" + this.gpuTraversalInputExecuted()
                + ",rawDiffuseGiOnlySampler=" + this.rawDiffuseGiOnlySampler
                + ",depthMaterialInputsBound=" + this.depthMaterialInputsBound
                + ",stillNotComputeBoundary=" + this.stillNotComputeBoundary
                + ",noOverclaimBlockers=\"" + this.noOverclaimBlockerSummary() + "\""
                + ",passIdentity=\"" + this.passIdentity + "\""
                + ",boundary=\"" + this.boundaryReason + "\"";
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

    private static String compactReason(String value) {
        if (value.length() <= MAX_BOUNDARY_REASON_CHARS) {
            return value;
        }
        return value.substring(0, MAX_BOUNDARY_REASON_CHARS - 3) + "...";
    }

    private static void appendBlocker(StringBuilder builder, boolean active, String message) {
        if (!active) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(message);
    }
}
