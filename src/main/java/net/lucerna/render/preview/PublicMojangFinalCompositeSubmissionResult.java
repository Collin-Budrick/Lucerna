package net.lucerna.render.preview;

import java.util.Locale;

public record PublicMojangFinalCompositeSubmissionResult(
        boolean attempted,
        boolean submitted,
        boolean drawCallsIssued,
        boolean javaOpaqueRenderObjectsPresent,
        TargetStatus targetStatus,
        ShaderOutputImageCandidate shaderOutputImageCandidate,
        ShaderGeneratedDenoiseOutputStatus shaderGeneratedDenoiseOutputStatus,
        String reason
) {
    public PublicMojangFinalCompositeSubmissionResult(
            boolean attempted,
            boolean submitted,
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        this(
                attempted,
                submitted,
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                ShaderOutputImageCandidate.none(),
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public PublicMojangFinalCompositeSubmissionResult(
            boolean attempted,
            boolean submitted,
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            ShaderOutputImageCandidate shaderOutputImageCandidate,
            String reason
    ) {
        this(
                attempted,
                submitted,
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                shaderOutputImageCandidate,
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public PublicMojangFinalCompositeSubmissionResult {
        if (targetStatus == null) {
            targetStatus = TargetStatus.UNKNOWN;
        }
        if (shaderOutputImageCandidate == null) {
            shaderOutputImageCandidate = ShaderOutputImageCandidate.none();
        }
        if (shaderGeneratedDenoiseOutputStatus == null) {
            shaderGeneratedDenoiseOutputStatus = ShaderGeneratedDenoiseOutputStatus.none();
        }
        if (reason == null || reason.isBlank()) {
            reason = submitted
                    ? "public Mojang final composite submission recorded"
                    : "public Mojang final composite submission was not submitted";
        } else {
            reason = reason.trim();
        }
    }

    public static PublicMojangFinalCompositeSubmissionResult notAttempted(String reason) {
        return new PublicMojangFinalCompositeSubmissionResult(
                false,
                false,
                false,
                false,
                TargetStatus.NOT_REQUESTED,
                ShaderOutputImageCandidate.none(),
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult notSubmitted(
            boolean attempted,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return notSubmitted(attempted, javaOpaqueRenderObjectsPresent, targetStatus, ShaderOutputImageCandidate.none(), reason);
    }

    public static PublicMojangFinalCompositeSubmissionResult notSubmitted(
            boolean attempted,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            ShaderOutputImageCandidate shaderOutputImageCandidate,
            String reason
    ) {
        return notSubmitted(
                attempted,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                shaderOutputImageCandidate,
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult notSubmitted(
            boolean attempted,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            ShaderOutputImageCandidate shaderOutputImageCandidate,
            ShaderGeneratedDenoiseOutputStatus shaderGeneratedDenoiseOutputStatus,
            String reason
    ) {
        return new PublicMojangFinalCompositeSubmissionResult(
                attempted,
                false,
                false,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                shaderOutputImageCandidate,
                shaderGeneratedDenoiseOutputStatus,
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitted(
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            String reason
    ) {
        return new PublicMojangFinalCompositeSubmissionResult(
                true,
                true,
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                ShaderOutputImageCandidate.none(),
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitted(
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            ShaderOutputImageCandidate shaderOutputImageCandidate,
            String reason
    ) {
        return submitted(
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                shaderOutputImageCandidate,
                ShaderGeneratedDenoiseOutputStatus.none(),
                reason
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitted(
            boolean drawCallsIssued,
            boolean javaOpaqueRenderObjectsPresent,
            TargetStatus targetStatus,
            ShaderOutputImageCandidate shaderOutputImageCandidate,
            ShaderGeneratedDenoiseOutputStatus shaderGeneratedDenoiseOutputStatus,
            String reason
    ) {
        return new PublicMojangFinalCompositeSubmissionResult(
                true,
                true,
                drawCallsIssued,
                javaOpaqueRenderObjectsPresent,
                targetStatus,
                shaderOutputImageCandidate,
                shaderGeneratedDenoiseOutputStatus,
                reason
        );
    }

    public boolean skipped() {
        return this.attempted && !this.submitted;
    }

    public String submissionStateLabel() {
        if (this.submitted && this.drawCallsIssued) {
            return "submitted-with-draw";
        }
        if (this.submitted) {
            return "submitted-without-draw";
        }
        if (this.skipped()) {
            return "skipped-after-attempt";
        }
        return "not-attempted";
    }

    public String submittedSourceIdentity() {
        String normalizedReason = this.normalizedReason();
        if (!this.submitted) {
            return "not-submitted";
        }
        boolean directLight = normalizedReason.contains("native direct-light emissive source is blended")
                || normalizedReason.contains("native direct-light surface-source")
                || normalizedReason.contains("directspill")
                || normalizedReason.contains("direct_spill");
        boolean genericDenoisedGi = normalizedReason.contains("denoised diffuse-gi")
                && !normalizedReason.contains("shader-denoised-diffuse-gi-rgba8");
        boolean cpuDenoiseExplicitlyTrue = normalizedReason.contains("cpudenoisedsourceready=true")
                || normalizedReason.contains("cpu_denoised_source_ready=true")
                || normalizedReason.contains("cpudenoisedgi=enabled-ready")
                || normalizedReason.contains("cpudenoisedgi=ready")
                || normalizedReason.contains("cpudenoisedreadback=ready")
                || normalizedReason.contains("sourcekind=cpu-denoised-readback")
                || normalizedReason.contains("sourcekind=cpu_denoised_readback");
        boolean shaderDenoisedGi = this.submittedRealShaderDenoiseOutputReady();
        boolean cpuDenoisedGi = !shaderDenoisedGi
                && (cpuDenoiseExplicitlyTrue
                || normalizedReason.contains("cpu-denoised-diffuse-gi-rgba8")
                || genericDenoisedGi);
        boolean rawGi = normalizedReason.contains("raw native diffuse-gi source is blended")
                || normalizedReason.contains("round 7 raw_gi native diffuse-gi source additive draw issued")
                || normalizedReason.contains("rawdrawrepeats=1")
                || normalizedReason.contains("coloredbouncegi")
                || normalizedReason.contains("colored_bounce_gi");
        boolean contactShadow = normalizedReason.contains("contactshadow")
                || normalizedReason.contains("contact_shadow");
        boolean publicMojangShaderVisualOutput = this.submittedPublicMojangShaderGeneratedVisualOutput();
        StringBuilder identity = new StringBuilder();
        appendIdentity(identity, directLight, "native-direct-light-rgba8");
        appendIdentity(identity, directLight, "directSpill");
        appendIdentity(identity, rawGi, "native-diffuse-gi-rgba8");
        appendIdentity(identity, rawGi, "coloredBounceGi");
        appendIdentity(identity, contactShadow, "contactShadow");
        appendIdentity(identity, this.submittedNativeShadowMapMask(), "native-shadow-map-mask");
        appendIdentity(identity, this.submittedShadowMapOutputConsumed(), "shadowMapOutputConsumed");
        appendIdentity(identity, this.submittedRealShadowMapComposite(), "realShadowMapComposite");
        appendIdentity(identity, cpuDenoisedGi, "cpu-denoised-diffuse-gi-rgba8");
        appendIdentity(identity, cpuDenoisedGi, "cpuFallback");
        appendIdentity(identity, publicMojangShaderVisualOutput, "public-mojang-visual-shaping-output");
        appendIdentity(identity, this.submittedShaderDenoisePassGeneratedVisualSource(),
                "shader-denoise-pass-generated-final-color-source");
        appendIdentity(identity, this.submittedShaderDenoiseOutputSourceConsumed(),
                "shaderDenoiseOutputSourceConsumed");
        appendIdentity(identity, shaderDenoisedGi, "shaderDenoisedGi");
        appendIdentity(identity, shaderDenoisedGi, "shader-denoised-diffuse-gi-rgba8");
        if (identity.length() > 0) {
            return identity.toString();
        }
        return "unknown-submitted-source";
    }

    public boolean submittedFocusWindowOnly() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("focuswindowonly=true")
                || normalizedReason.contains("focus_window_only=true")
                || normalizedReason.contains("focus_window_source=true")
                || normalizedReason.contains("focus_only=true"));
    }

    public boolean submittedMetadataOnlyPreview() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.targetStatus == TargetStatus.METADATA_ONLY
                || normalizedReason.contains("metadataonly=true")
                || normalizedReason.contains("metadata_only=true")
                || normalizedReason.contains("metadata_preview=true")
                || normalizedReason.contains("metadata_source=true"));
    }

    public boolean submittedProofMarkerSource() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("proofmarker=true")
                || normalizedReason.contains("proof_marker=true")
                || normalizedReason.contains("proof_source=true")
                || normalizedReason.contains("proof_only=true"));
    }

    public boolean submittedTemporaryDirectLightSubstitution() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("temporarydirectlightsubstitution=true")
                || normalizedReason.contains("temporary_direct_light_substitution=true")
                || normalizedReason.contains("temporary_direct_light_source=true")
                || normalizedReason.contains("temporary_direct_source=true")
                || normalizedReason.contains("uses_direct_light_payload=true")
                || normalizedReason.contains("using_direct_light_payload=true")
                || normalizedReason.contains("direct_light_payload_substitute=true"));
    }

    public boolean submittedRectangularWashoutRisk() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("rectangularwashout=true")
                || normalizedReason.contains("rectangular_washout=true")
                || normalizedReason.contains("hard_rectangle=true")
                || normalizedReason.contains("full_screen_washout=true")
                || normalizedReason.contains("full_target_washout=true"));
    }

    public boolean submittedNativeShadowMapMask() {
        if (!this.submitted) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("native-shadow-map-mask")
                || normalizedReason.contains("native_shadow_map_mask")
                || normalizedReason.contains("sourcekind=native-shadow-map-mask")
                || normalizedReason.contains("sourcekind=native_shadow_map_mask")
                || normalizedReason.contains("shadowmapsource=native-shadow-map-mask")
                || normalizedReason.contains("shadow_map_source=native_shadow_map_mask")
                || normalizedReason.contains("shadowmapmask=native")
                || normalizedReason.contains("shadow_map_mask=native");
    }

    public boolean submittedShadowMapOutputConsumed() {
        if (!this.submitted) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("shadowmapoutputconsumed=true")
                || normalizedReason.contains("shadow_map_output_consumed=true")
                || normalizedReason.contains("shadowmapconsumed=true")
                || normalizedReason.contains("shadow_map_consumed=true")
                || normalizedReason.contains("native_shadow_map_consumed=true")
                || normalizedReason.contains("native-shadow-map-output-consumed")
                || normalizedReason.contains("native_shadow_map_output_consumed")
                || normalizedReason.contains("realshadowmapcomposite=true")
                || normalizedReason.contains("real_shadow_map_composite=true");
    }

    public boolean submittedDepthAwareShadowMaskComposite() {
        if (!this.submitted) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("depthawareshadowmaskcomposite=true")
                || normalizedReason.contains("depth_aware_shadow_mask_composite=true")
                || normalizedReason.contains("shaderpassdepthsamplingevidence=true")
                || normalizedReason.contains("shader_pass_depth_sampling_evidence=true")
                || normalizedReason.contains("g_buffer_depth_texture_sampled=true")
                || normalizedReason.contains("gbuffer_depth_texture_sampled=true");
    }

    public boolean submittedScreenSpaceShadowDecalSource() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("screen_space_shadow_decal=true")
                || normalizedReason.contains("screenspaceshadowdecal=true")
                || normalizedReason.contains("screen_space_shadow=true")
                || normalizedReason.contains("shadow_decal_screen_space=true")
                || normalizedReason.contains("sourcekind=screen-space-shadow-decal")
                || normalizedReason.contains("sourcekind=screen_space_shadow_decal")
                || normalizedReason.contains("screen-space-shadow-decal-source")
                || normalizedReason.contains("screen_space_shadow_decal_source"));
    }

    public boolean submittedLowResDirectTextureShadowProof() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("low_res_direct_texture_shadow_proof=true")
                || normalizedReason.contains("lowresdirecttextureshadowproof=true")
                || normalizedReason.contains("low_res_direct_texture=true")
                || normalizedReason.contains("cpu_direct_texture_composite=true")
                || normalizedReason.contains("direct_light_low_res_texture=true")
                || normalizedReason.contains("sourcekind=low-res-direct-texture")
                || normalizedReason.contains("sourcekind=low_res_direct_texture")
                || normalizedReason.contains("low-res-direct-texture-shadow-source")
                || normalizedReason.contains("low_res_direct_texture_shadow_source"));
    }

    public boolean submittedShadowMapCompositeNoOverclaimBoundary() {
        return !this.submittedScreenSpaceShadowDecalSource()
                && !this.submittedLowResDirectTextureShadowProof()
                && !this.submittedMetadataOnlyPreview()
                && !this.submittedProofMarkerSource()
                && !this.submittedFocusWindowOnly()
                && !this.submittedTemporaryDirectLightSubstitution()
                && !this.submittedShaderDenoiseOverclaim();
    }

    public boolean submittedRealShadowMapComposite() {
        return this.submitted
                && this.drawCallsIssued
                && this.targetStatus == TargetStatus.READY
                && this.submittedNativeShadowMapMask()
                && this.submittedShadowMapOutputConsumed()
                && this.submittedShadowMapCompositeNoOverclaimBoundary();
    }

    public boolean submittedPreviewOnlyEvidence() {
        return this.submittedFocusWindowOnly()
                || this.submittedMetadataOnlyPreview()
                || this.submittedProofMarkerSource()
                || this.submittedTemporaryDirectLightSubstitution()
                || this.submittedRectangularWashoutRisk()
                || this.submittedScreenSpaceShadowDecalSource()
                || this.submittedLowResDirectTextureShadowProof()
                || this.submittedShaderDenoiseDirectLightValidationInput();
    }

    public boolean submittedSourceGatedSurfaceProjection() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("source-gated")
                || normalizedReason.contains("source_gated")
                || normalizedReason.contains("geometry-aware")
                || normalizedReason.contains("geometry_aware")
                || normalizedReason.contains("sourceboundary=full-target-source-gated")
                || normalizedReason.contains("surfaceprojection=source-gated"));
    }

    public boolean submittedGeometryAwareFinalComposite() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && this.submittedSourceGatedSurfaceProjection()
                && !this.submittedPreviewOnlyEvidence()
                && (normalizedReason.contains("geometry-aware")
                || normalizedReason.contains("geometry_aware")
                || normalizedReason.contains("directspill")
                || normalizedReason.contains("coloredbouncegi")
                || normalizedReason.contains("contactshadow")
                || normalizedReason.contains("shaderdenoisedgi"));
    }

    public boolean submittedDirectLightSource() {
        return this.submittedSourceIdentity().contains("native-direct-light-rgba8");
    }

    public boolean submittedRawGiSource() {
        return this.submittedSourceIdentity().contains("native-diffuse-gi-rgba8");
    }

    public boolean submittedDenoisedGiSource() {
        return this.submittedDenoisedGiSourceIdentity() != DenoisedGiSourceIdentity.NONE;
    }

    public boolean submittedCpuDenoisedGiSource() {
        DenoisedGiSourceIdentity identity = this.submittedDenoisedGiSourceIdentity();
        return identity == DenoisedGiSourceIdentity.CPU_DENOISED_READBACK
                || identity == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
    }

    public boolean submittedShaderDenoisedGiSource() {
        DenoisedGiSourceIdentity identity = this.submittedDenoisedGiSourceIdentity();
        return identity == DenoisedGiSourceIdentity.SHADER_GENERATED_DENOISED_GI
                || identity == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
    }

    public DenoisedGiSourceIdentity submittedDenoisedGiSourceIdentity() {
        String identity = this.submittedSourceIdentity();
        boolean cpu = identity.contains("cpu-denoised-diffuse-gi-rgba8");
        boolean shader = identity.contains("shader-denoised-diffuse-gi-rgba8");
        if (cpu && shader) {
            return DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER;
        }
        if (shader) {
            return DenoisedGiSourceIdentity.SHADER_GENERATED_DENOISED_GI;
        }
        if (cpu) {
            return DenoisedGiSourceIdentity.CPU_DENOISED_READBACK;
        }
        return DenoisedGiSourceIdentity.NONE;
    }

    public boolean submittedShaderDenoiseOverclaim() {
        return this.submittedShaderDenoiseOutputClaimPresent()
                && !this.submittedRealShaderDenoiseOutputReady();
    }

    public boolean submittedShaderDenoiseVisualShaderIntent() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("round 7 denoised_gi visual")
                || normalizedReason.contains("round 7 final_composite visual")
                || normalizedReason.contains("round7_denoised_gi_visual")
                || normalizedReason.contains("denoised-source additive draw")
                || normalizedReason.contains("source-gated-denoised-gi-visual")
                || normalizedReason.contains("shader=lucerna:core/round7_denoised_gi_visual"));
    }

    public boolean submittedPublicMojangShaderGeneratedVisualOutput() {
        return this.submitted
                && this.drawCallsIssued
                && this.targetStatus == TargetStatus.READY
                && this.submittedShaderDenoiseVisualShaderIntent()
                && !this.submittedPreviewOnlyEvidence();
    }

    public boolean submittedRealShaderDenoiseDispatchReady() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("shaderdenoisedispatchprepared=true")
                || normalizedReason.contains("shader_denoise_dispatch_prepared=true")
                || normalizedReason.contains("round7.shaderdenoise.dispatchprepared=true"))
                && (normalizedReason.contains("shaderdenoiseinputready=true")
                || normalizedReason.contains("shader_denoise_input_ready=true")
                || normalizedReason.contains("shaderdenoiseinputscompletefordispatch=true"));
    }

    public boolean submittedShaderDenoiseOutputPassAttempted() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderGeneratedDenoiseOutputStatus.outputPassAttempted()
                || this.submittedShaderDenoiseOutputRenderPassSubmitted()
                || normalizedReason.contains("shaderdenoiseoutputpassattempted=true")
                || normalizedReason.contains("shader_denoise_output_pass_attempted=true")
                || normalizedReason.contains("shaderdenoisepassattempted=true")
                || normalizedReason.contains("shader_denoise_pass_attempted=true")
                || normalizedReason.contains("round7.shaderdenoise.outputpassattempted=true"));
    }

    public boolean submittedShaderDenoiseOutputTextureAllocated() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderGeneratedDenoiseOutputStatus.outputTextureAllocated()
                || normalizedReason.contains("shaderdenoiseoutputtextureallocated=true")
                || normalizedReason.contains("shader_denoise_output_texture_allocated=true")
                || normalizedReason.contains("shaderdenoiseownedoutputimage=true")
                || normalizedReason.contains("shader_denoise_owned_output_image=true")
                || normalizedReason.contains("round7.shaderdenoise.outputtextureallocated=true"));
    }

    public boolean submittedShaderDenoiseOwnedOutputImage() {
        return this.submittedShaderDenoiseOutputTextureAllocated()
                && this.submittedShaderDenoiseOutputImageReady();
    }

    public boolean submittedShaderDenoiseOutputRenderPassSubmitted() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderGeneratedDenoiseOutputStatus.outputRenderPassSubmitted()
                || normalizedReason.contains("shaderdenoiseoutputrenderpasssubmitted=true")
                || normalizedReason.contains("shader_denoise_output_render_pass_submitted=true")
                || normalizedReason.contains("shaderdenoisepasssubmitted=true")
                || normalizedReason.contains("shader_denoise_pass_submitted=true")
                || normalizedReason.contains("shaderdenoisepassexecuted=true")
                || normalizedReason.contains("shader_generated_denoise_pass_executed=true")
                || normalizedReason.contains("shadergenerateddenoisepassexecuted=true")
                || normalizedReason.contains("round7.shaderdenoise.publicmojangfragmentpass")
                || normalizedReason.contains("round7-shader-denoise-output"));
    }

    public boolean submittedShaderDenoiseGeneratedOutputPassSubmitted() {
        return this.submittedShaderDenoiseOutputPassAttempted()
                && this.submittedShaderDenoiseOutputRenderPassSubmitted();
    }

    public boolean submittedShaderDenoiseOutputImageReady() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderGeneratedDenoiseOutputStatus.shaderGeneratedOutputImageReady()
                || normalizedReason.contains("shaderdenoiseoutputimageready=true")
                || normalizedReason.contains("shader_denoise_output_image_ready=true")
                || normalizedReason.contains("shadergeneratedoutputimageready=true")
                || normalizedReason.contains("shader_generated_output_image_ready=true")
                || normalizedReason.contains("round7.shaderdenoise.outputimageready=true"));
    }

    public boolean submittedShaderDenoisePassExecuted() {
        return this.submittedShaderDenoiseOutputRenderPassSubmitted();
    }

    public boolean submittedShaderDenoiseOutputSourceConsumed() {
        String normalizedReason = this.normalizedReason();
        return this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                || (this.submittedShaderDenoisePassExecuted()
                && (normalizedReason.contains("shaderoutputsourceconsumed=true")
                || normalizedReason.contains("shader_output_source_consumed=true")
                || normalizedReason.contains("shaderdenoiseoutputsourceconsumed=true")
                || normalizedReason.contains("shader_denoise_output_source_consumed=true")));
    }

    public boolean submittedShaderDenoiseOutputConsumedByFinalComposite() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderGeneratedDenoiseOutputStatus.outputConsumedByFinalComposite()
                || normalizedReason.contains("shaderdenoiseoutputconsumedbyfinalcomposite=true")
                || normalizedReason.contains("shader_denoise_output_consumed_by_final_composite=true")
                || normalizedReason.contains("shaderoutputconsumedbyfinalcomposite=true")
                || normalizedReason.contains("shader_output_consumed_by_final_composite=true")
                || normalizedReason.contains("outputconsumedbyfinalcomposite=true")
                || normalizedReason.contains("output_consumed_by_final_composite=true")
                || normalizedReason.contains("round7.shaderdenoise.outputconsumedbyfinalcomposite=true"));
    }

    public boolean submittedShaderDenoiseFinalCompositeConsumable() {
        String normalizedReason = this.normalizedReason();
        return this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                || (this.submittedShaderDenoisePassExecuted()
                && (normalizedReason.contains("finalcompositeconsumable=true")
                || normalizedReason.contains("final_composite_consumable=true")
                || normalizedReason.contains("shaderdenoisefinalcompositeconsumable=true")
                || normalizedReason.contains("shader_denoise_final_composite_consumable=true")));
    }

    public boolean submittedShaderDenoiseCpuReadbackFallbackActive() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (normalizedReason.contains("cpureadbackfallbackactive=true")
                || normalizedReason.contains("cpu_readback_fallback_active=true")
                || normalizedReason.contains("shaderdenoisecpureadbackfallbackactive=true")
                || normalizedReason.contains("shader_denoise_cpu_readback_fallback_active=true")
                || normalizedReason.contains("cpudenoisereadbackfallback=true")
                || normalizedReason.contains("cpu_denoise_readback_fallback=true"));
    }

    public boolean submittedShaderDenoiseCpuReadbackFallbackInactive() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && !this.submittedShaderDenoiseCpuReadbackFallbackActive()
                && (this.shaderGeneratedDenoiseOutputStatus.cpuReadbackFallbackInactive()
                || normalizedReason.contains("cpureadbackfallbackinactive=true")
                || normalizedReason.contains("cpu_readback_fallback_inactive=true")
                || normalizedReason.contains("shaderdenoisecpureadbackfallbackinactive=true")
                || normalizedReason.contains("shader_denoise_cpu_readback_fallback_inactive=true")
                || normalizedReason.contains("cpureadbackfallbackactive=false")
                || normalizedReason.contains("cpu_readback_fallback_active=false")
                || normalizedReason.contains("shaderdenoisecpureadbackfallbackactive=false")
                || normalizedReason.contains("shader_denoise_cpu_readback_fallback_active=false"));
    }

    public boolean submittedShaderDenoiseStillNotComputeBoundary() {
        String normalizedReason = this.normalizedReason();
        if (normalizedReason.contains("stillnotcomputeboundary=false")
                || normalizedReason.contains("still_not_compute_boundary=false")
                || normalizedReason.contains("shaderdenoisestillnotcomputeboundary=false")
                || normalizedReason.contains("shader_denoise_still_not_compute_boundary=false")) {
            return false;
        }
        return this.shaderGeneratedDenoiseOutputStatus.stillNotComputeBoundary()
                || normalizedReason.contains("stillnotcomputeboundary=true")
                || normalizedReason.contains("still_not_compute_boundary=true")
                || normalizedReason.contains("shaderdenoisestillnotcomputeboundary=true")
                || normalizedReason.contains("shader_denoise_still_not_compute_boundary=true");
    }

    public boolean submittedShaderDenoiseRawDiffuseGiInput() {
        if (!this.submitted) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("shaderdenoiseinputkind=raw-diffuse-gi-rgba8")
                || normalizedReason.contains("shader_denoise_input_kind=raw-diffuse-gi-rgba8")
                || normalizedReason.contains("shaderdenoiseinputmode=strict-raw-diffuse-gi")
                || normalizedReason.contains("shader_denoise_input_mode=strict-raw-diffuse-gi")
                || normalizedReason.contains("shaderdenoiserawdiffusegiinput=true")
                || normalizedReason.contains("shader_denoise_raw_diffuse_gi_input=true")
                || normalizedReason.contains("round7.shaderdenoise.rawdiffusegiinput=true")
                || normalizedReason.contains("rawgicpureadbackinput=true")
                || normalizedReason.contains("raw_gi_cpu_readback_input=true");
    }

    public boolean submittedShaderDenoiseDirectLightValidationInput() {
        if (!this.submitted) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("shaderdenoiseinputkind=native-direct-light-rgba8-validation-input")
                || normalizedReason.contains("shader_denoise_input_kind=native-direct-light-rgba8-validation-input")
                || normalizedReason.contains("shaderdenoiseinputmode=diagnostic-direct-light-validation-fallback")
                || normalizedReason.contains("shader_denoise_input_mode=diagnostic-direct-light-validation-fallback")
                || normalizedReason.contains("directlightvalidationinput=true")
                || normalizedReason.contains("direct_light_validation_input=true")
                || normalizedReason.contains("round7.shaderdenoise.directlightvalidationinput=true")
                || normalizedReason.contains("diagnosticdirectlightvalidationfallback=true")
                || normalizedReason.contains("diagnostic_direct_light_validation_fallback=true");
    }

    public boolean submittedShaderDenoiseStrictRawDiffuseGiInput() {
        if (!this.submittedShaderDenoiseRawDiffuseGiInput()
                || this.submittedShaderDenoiseDirectLightValidationInput()) {
            return false;
        }
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("shaderdenoisestrictproofeligible=true")
                || normalizedReason.contains("shader_denoise_strict_proof_eligible=true")
                || normalizedReason.contains("shaderdenoiserequiresrawdiffusegiinput=true")
                || normalizedReason.contains("shader_denoise_requires_raw_diffuse_gi_input=true")
                || normalizedReason.contains("shaderdenoiseinputmode=strict-raw-diffuse-gi")
                || normalizedReason.contains("shader_denoise_input_mode=strict-raw-diffuse-gi");
    }

    public boolean submittedShaderDenoisePassGeneratedVisualSource() {
        String normalizedReason = this.normalizedReason();
        return this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                && this.submittedShaderDenoiseOutputImageReady()
                && (normalizedReason.contains("shaderdenoisepassgeneratedvisualsource=true")
                || normalizedReason.contains("shader_denoise_pass_generated_visual_source=true")
                || normalizedReason.contains("shadergenerateddenoisedgi=public-mojang-fragment-pass")
                || normalizedReason.contains("sourcekind=shader-generated-denoised-gi")
                || normalizedReason.contains("sourcekind=shader_generated_denoised_gi"))
                && !this.submittedExplicitShaderDenoiseOutputFalse();
    }

    public boolean submittedRealShaderDenoiseOutputReady() {
        return this.submitted
                && this.submittedShaderDenoiseStrictRawDiffuseGiInput()
                && this.submittedShaderDenoiseOwnedOutputImage()
                && this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                && this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                && this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                && !this.submittedShaderOutputImageCandidate()
                && !this.submittedExplicitShaderDenoiseOutputFalse();
    }

    public boolean submittedShaderOutputImageCandidate() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.shaderOutputImageCandidate.candidatePresent()
                || normalizedReason.contains("shaderoutputimagecandidate=true")
                || normalizedReason.contains("shader_output_image_candidate=true")
                || normalizedReason.contains("shaderdenoiseoutputcandidate=true")
                || normalizedReason.contains("shader_denoise_output_candidate=true")
                || normalizedReason.contains("shaderdenoisedgi=candidate")
                || normalizedReason.contains("shadergenerateddenoisedgi=candidate")
                || normalizedReason.contains("sourcekind=shader-output-image-candidate")
                || normalizedReason.contains("sourcekind=shader_output_image_candidate"));
    }

    public String denoiseEvidenceBoundarySummary() {
        return "cpuDenoisedOutput=" + readyState(this.submittedCpuDenoisedGiSource())
                + ",denoisedSourceIdentity=" + this.submittedDenoisedGiSourceIdentity().stableLabel()
                + ",shaderDenoiseSourceClassification=" + this.shaderDenoiseSourceClassification()
                + ",shaderDenoiseOutputClaimPresent=" + this.submittedShaderDenoiseOutputClaimPresent()
                + ",shaderOutputImageCandidate=" + readyState(this.submittedShaderOutputImageCandidate())
                + ",shaderOutputImageCandidateBoundary=" + this.shaderOutputImageCandidate.boundarySummary()
                + ",shaderGeneratedDenoiseOutputStatus="
                + this.shaderGeneratedDenoiseOutputStatus.boundarySummary()
                + ",shaderOutputImageCandidateOnly="
                + (this.submittedShaderOutputImageCandidate() && !this.submittedRealShaderDenoiseOutputReady())
                + ",shaderDenoiseVisualShaderIntent=" + readyState(this.submittedShaderDenoiseVisualShaderIntent())
                + ",publicMojangShaderGeneratedVisualOutput="
                + readyState(this.submittedPublicMojangShaderGeneratedVisualOutput())
                + ",realShaderDenoiseDispatchReady=" + readyState(this.submittedRealShaderDenoiseDispatchReady())
                + ",shaderDenoiseRawDiffuseGiInput=" + this.submittedShaderDenoiseRawDiffuseGiInput()
                + ",shaderDenoiseDirectLightValidationInput="
                + this.submittedShaderDenoiseDirectLightValidationInput()
                + ",shaderDenoiseStrictRawDiffuseGiInput="
                + this.submittedShaderDenoiseStrictRawDiffuseGiInput()
                + ",shaderDenoiseOutputPassAttempted=" + this.submittedShaderDenoiseOutputPassAttempted()
                + ",shaderDenoiseOutputTextureAllocated=" + this.submittedShaderDenoiseOutputTextureAllocated()
                + ",shaderDenoiseOwnedOutputImage=" + this.submittedShaderDenoiseOwnedOutputImage()
                + ",shaderDenoiseOutputRenderPassSubmitted="
                + this.submittedShaderDenoiseOutputRenderPassSubmitted()
                + ",shaderDenoiseGeneratedOutputPassSubmitted="
                + this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                + ",shaderDenoisePassExecuted=" + this.submittedShaderDenoisePassExecuted()
                + ",shaderGeneratedDenoisePassExecuted=" + this.submittedShaderDenoisePassExecuted()
                + ",shaderOutputSourceConsumed=" + this.submittedShaderDenoiseOutputSourceConsumed()
                + ",shaderDenoiseOutputSourceConsumed=" + this.submittedShaderDenoiseOutputSourceConsumed()
                + ",shaderDenoiseOutputConsumedByFinalComposite="
                + this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                + ",shaderDenoisePassGeneratedVisualSource="
                + this.submittedShaderDenoisePassGeneratedVisualSource()
                + ",shaderDenoiseFinalCompositeConsumable="
                + this.submittedShaderDenoiseFinalCompositeConsumable()
                + ",finalCompositeConsumable=" + this.submittedShaderDenoiseFinalCompositeConsumable()
                + ",cpuReadbackFallbackActive=" + this.submittedShaderDenoiseCpuReadbackFallbackActive()
                + ",shaderDenoiseCpuReadbackFallbackActive="
                + this.submittedShaderDenoiseCpuReadbackFallbackActive()
                + ",cpuReadbackFallbackInactive=" + this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                + ",shaderDenoiseCpuReadbackFallbackInactive="
                + this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                + ",shaderDenoiseStillNotComputeBoundary="
                + this.submittedShaderDenoiseStillNotComputeBoundary()
                + ",realShaderDenoiseOutputReady=" + readyState(this.submittedRealShaderDenoiseOutputReady())
                + ",round7.shaderDenoise.outputImageReady=" + this.submittedShaderDenoiseOutputImageReady()
                + ",round7.shaderDenoise.passGeneratedFinalColorOutput="
                + this.submittedShaderDenoisePassGeneratedVisualSource()
                + ",round7.shaderDenoise.outputMaterialReady="
                + (this.submittedShaderDenoiseOutputImageReady()
                || this.submittedShaderDenoiseFinalCompositeConsumable())
                + ",round7.shaderDenoise.shaderGeneratedOutput=" + this.submittedRealShaderDenoiseOutputReady()
                + ",round7.shaderDenoise.realOutputReady=" + this.submittedRealShaderDenoiseOutputReady()
                + ",shaderDenoiseOverclaimPresent=" + this.submittedShaderDenoiseOverclaim()
                + ",finalDenoiseSourceIdentity=" + this.denoiseSourceClassLabel()
                + ",boundary=\""
                + (this.submittedRealShaderDenoiseOutputReady()
                ? "submitted source reports shader-denoised GI output; controller still needs quality/stability proof"
                : "current submitted denoise evidence is CPU/readback or absent; visual shader intent is not real shader-side denoise output")
                + "\"";
    }

    public String finalSourceIdentitySummary() {
        String identity = this.submittedSourceIdentity();
        return "directSpill=" + readyState(identity.contains("directSpill"))
                + ",coloredBounceGi=" + readyState(identity.contains("coloredBounceGi"))
                + ",contactShadow=" + readyState(identity.contains("contactShadow"))
                + ",nativeShadowMapMask=" + readyState(this.submittedNativeShadowMapMask())
                + ",shadowMapOutputConsumed=" + readyState(this.submittedShadowMapOutputConsumed())
                + ",realShadowMapComposite=" + readyState(this.submittedRealShadowMapComposite())
                + ",shaderDenoisedGi=" + readyState(identity.contains("shaderDenoisedGi"))
                + ",cpuFallback=" + readyState(identity.contains("cpuFallback"))
                + ",geometryAwareFinalComposite=" + readyState(this.submittedGeometryAwareFinalComposite())
                + ",focusWindowOnlyRejected=" + !this.submittedFocusWindowOnly()
                + ",rectangularWashoutRejected=" + !this.submittedRectangularWashoutRisk();
    }

    public String finalCompositeDenoisedSourceIdentityBoundary() {
        return "submittedDenoisedIdentity=" + this.submittedDenoisedGiSourceIdentity().stableLabel()
                + ",cpuReadbackVisualShaping=" + this.submittedCpuDenoisedGiSource()
                + ",visualShaderIntent=" + this.submittedShaderDenoiseVisualShaderIntent()
                + ",publicMojangShaderGeneratedVisualOutput="
                + this.submittedPublicMojangShaderGeneratedVisualOutput()
                + ",shaderDenoisePassExecuted=" + this.submittedShaderDenoisePassExecuted()
                + ",shaderOutputSourceConsumed=" + this.submittedShaderDenoiseOutputSourceConsumed()
                + ",shaderOutputConsumedByFinalComposite="
                + this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                + ",shaderDenoisePassGeneratedVisualSource="
                + this.submittedShaderDenoisePassGeneratedVisualSource()
                + ",shaderDenoiseFinalCompositeConsumable="
                + this.submittedShaderDenoiseFinalCompositeConsumable()
                + ",cpuReadbackFallbackActive=" + this.submittedShaderDenoiseCpuReadbackFallbackActive()
                + ",cpuReadbackFallbackInactive=" + this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                + ",stillNotComputeBoundary=" + this.submittedShaderDenoiseStillNotComputeBoundary()
                + ",shaderDenoiseSourceClassification=" + this.shaderDenoiseSourceClassification()
                + ",shaderGeneratedDenoisedOutput=" + this.submittedShaderDenoisedGiSource()
                + ",realShaderDenoiseOutputReady=" + this.submittedRealShaderDenoiseOutputReady()
                + ",shaderOutputPassAttempted=" + this.submittedShaderDenoiseOutputPassAttempted()
                + ",shaderOutputTextureAllocated=" + this.submittedShaderDenoiseOutputTextureAllocated()
                + ",shaderOwnedOutputImage=" + this.submittedShaderDenoiseOwnedOutputImage()
                + ",shaderOutputRenderPassSubmitted="
                + this.submittedShaderDenoiseOutputRenderPassSubmitted()
                + ",shaderGeneratedOutputPassSubmitted="
                + this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                + ",shaderOutputImageReady=" + this.submittedShaderDenoiseOutputImageReady()
                + ",shaderOutputImageCandidate=" + this.submittedShaderOutputImageCandidate()
                + ",shaderOutputBlocker=\"" + this.shaderOutputImageCandidate.blocker() + "\""
                + ",shaderOutputBlockerSource=\"" + this.shaderDenoiseOutputBlockerSource() + "\""
                + ",boundary=\""
                + (this.submittedRealShaderDenoiseOutputReady()
                ? "final composite source identity reports shader-generated denoised GI output"
                : "final composite source identity is CPU/readback visual shaping or missing shader output; do not count it as real shader-generated denoise")
                + "\"";
    }

    public String authenticityGuardsSummary() {
        return "metadataOnlyPreview=" + this.submittedMetadataOnlyPreview()
                + ",focusWindowOnly=" + this.submittedFocusWindowOnly()
                + ",proofMarkerSource=" + this.submittedProofMarkerSource()
                + ",temporaryDirectLightSubstitution=" + this.submittedTemporaryDirectLightSubstitution()
                + ",rectangularWashoutRisk=" + this.submittedRectangularWashoutRisk()
                + ",screenSpaceShadowDecal=" + this.submittedScreenSpaceShadowDecalSource()
                + ",lowResDirectTextureShadowProof=" + this.submittedLowResDirectTextureShadowProof()
                + ",shadowMapCompositeNoOverclaim="
                + this.submittedShadowMapCompositeNoOverclaimBoundary()
                + ",shaderDenoiseOverclaim=" + this.submittedShaderDenoiseOverclaim()
                + ",shaderDenoiseNoOverclaim=" + !this.submittedShaderDenoiseOverclaim()
                + ",previewEvidenceClean=" + !this.submittedPreviewOnlyEvidence();
    }

    public boolean submittedRound7GiSource() {
        return this.submittedRawGiSource() || this.submittedDenoisedGiSource();
    }

    public String sourceAuthenticityLabel() {
        if (!this.submitted) {
            return "no-submitted-source";
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "rejected:metadata-only-preview";
        }
        if (this.submittedProofMarkerSource()) {
            return "rejected:proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "rejected:focus-window-only";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "rejected:temporary-direct-light-substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "rejected:rectangular-washout-risk";
        }
        if (this.submittedScreenSpaceShadowDecalSource()) {
            return "rejected:screen-space-shadow-decal";
        }
        if (this.submittedLowResDirectTextureShadowProof()) {
            return "rejected:low-res-direct-texture-shadow-proof";
        }
        if (this.submittedShaderDenoiseDirectLightValidationInput()) {
            return "rejected:shader-denoise-direct-light-validation-input";
        }
        if (this.submittedRealShadowMapComposite()) {
            return "accepted:native-shadow-map-mask/final-composite-consumed";
        }
        if (this.submittedRealShaderDenoiseOutputReady()) {
            return "accepted:shader-generated-denoise-output/final-composite-consumed";
        }
        if (this.submittedShaderDenoisePassGeneratedVisualSource()) {
            return "candidate:shader-denoise-fragment-pass-output;"
                    + "requires-owned-image-final-consumption-strict-raw-diffuse-gi-input-and-fallback-inactive";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "accepted:final-composite-direct-plus-raw-gi-plus-"
                    + this.denoiseSourceClassLabel()
                    + "/source-separated/"
                    + this.surfaceProjectionEvidenceLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRound7GiSource()) {
            return "accepted:partial-final-composite-direct-plus-gi";
        }
        if (this.submittedDirectLightSource()) {
            return "accepted:native-direct-light-surface-source";
        }
        if (this.submittedDenoisedGiSource()) {
            return "accepted:" + this.denoiseSourceClassLabel() + "-output";
        }
        if (this.submittedRawGiSource()) {
            return "accepted:raw-gi-output";
        }
        return "unknown-source";
    }

    public String focusedRegionReadiness() {
        if (!this.submitted) {
            return "not-ready:not-submitted";
        }
        if (!this.drawCallsIssued) {
            return "not-ready:no-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "not-ready:metadata-only-preview";
        }
        if (this.submittedProofMarkerSource()) {
            return "not-ready:proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "not-ready:focus-window-only";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "not-ready:temporary-direct-light-substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "not-ready:rectangular-washout-risk";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "ready-for-controller-final-direct-raw-denoised-"
                    + this.surfaceProjectionEvidenceLabel()
                    + "-surface-delta";
        }
        if (this.submittedDirectLightSource()) {
            return "ready-for-controller-direct-light-surface-delta";
        }
        if (!this.submittedRound7GiSource()) {
            return "not-ready:unknown-source";
        }
        return "ready-for-controller-focused-region-delta";
    }

    public String visualProofExpectation() {
        if (!this.submitted) {
            return "visual proof should not pass until a selected Round 7 source draw is submitted";
        }
        if (!this.drawCallsIssued) {
            return "visual proof should not pass because the submission reported no draw calls";
        }
        if (this.submittedFocusWindowOnly()) {
            return "visual proof should not pass because Round 7 rejects focus-window-only brightness";
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "visual proof should not pass because metadata-only preview is not a drawable lighting source";
        }
        if (this.submittedProofMarkerSource()) {
            return "visual proof should not pass because proof markers are not surface lighting";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "visual proof should not pass because GI/denoise paths reject temporary direct-light substitution";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "visual proof should not pass because rectangular/full-screen washout is not geometry/material-aware surface projection";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "final visual proof can only pass if controller screenshots show stable source-separated direct plus raw GI plus denoised GI contribution projected as source-gated scene/surface lighting with HUD safety; focus windows, proof markers, metadata-only previews, and rectangular washout must fail";
        }
        if (this.submittedDirectLightSource()) {
            return "direct-light visual proof can only pass if controller screenshots show an emissive native direct-light surface delta";
        }
        if (this.submittedRound7GiSource()) {
            return "visual proof can only pass if controller screenshots show focused-surface delta for this submitted source";
        }
        return "visual proof should remain inconclusive because the submitted source identity is unknown";
    }

    public String visualProofMissingReason() {
        if (!this.attempted) {
            return "final-composite-not-attempted";
        }
        if (!this.submitted) {
            return "final-composite-skipped";
        }
        if (!this.drawCallsIssued) {
            return "submitted-without-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "target-not-ready:" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedMetadataOnlyPreview()) {
            return "metadata-only-preview-source";
        }
        if (this.submittedProofMarkerSource()) {
            return "proof-marker-source";
        }
        if (this.submittedFocusWindowOnly()) {
            return "focus-window-only-source";
        }
        if (this.submittedTemporaryDirectLightSubstitution()) {
            return "temporary-direct-light-substitution-source";
        }
        if (this.submittedRectangularWashoutRisk()) {
            return "rectangular-washout-risk";
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "awaiting-controller-final-direct-raw-denoised-geometry-material-aware-surface-delta-and-quality-proof";
        }
        if (this.submittedDirectLightSource()) {
            return "awaiting-controller-direct-light-before-after-surface-delta";
        }
        if (!this.submittedRound7GiSource()) {
            return "unknown-submitted-source";
        }
        return "awaiting-controller-before-after-focused-surface-delta";
    }

    public String finalSurfaceProjectionQualityGate() {
        if (!this.submitted) {
            return "not-ready:not-submitted";
        }
        if (!this.drawCallsIssued) {
            return "not-ready:no-draw-calls";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "not-ready:rejected-preview-evidence/" + this.sourceAuthenticityLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "ready-for-controller-proof:source-separated-direct-raw-gi-"
                    + this.denoiseSourceClassLabel()
                    + ";surfaceProjection="
                    + this.surfaceProjectionEvidenceLabel()
                    + ";geometry-material-aware-quality=pending;temporal-stability=pending";
        }
        if (this.submittedRound7GiSource()) {
            return "partial-ready:gi-source-present;missing-direct-or-denoised-final-stack";
        }
        if (this.submittedDirectLightSource()) {
            return "partial-ready:direct-source-present;missing-gi-denoise-final-stack";
        }
        return "not-ready:unknown-source";
    }

    public String summary() {
        boolean finalPhysicalCompositeReady = this.submitted
                && this.drawCallsIssued
                && this.targetStatus == TargetStatus.READY
                && this.submittedSourceGatedSurfaceProjection()
                && !this.submittedPreviewOnlyEvidence();
        return "state=" + this.submissionStateLabel()
                + ",attempted=" + this.attempted
                + ",submitted=" + this.submitted
                + ",drawCallsIssued=" + this.drawCallsIssued
                + ",javaOpaqueRenderObjectsPresent=" + this.javaOpaqueRenderObjectsPresent
                + ",targetStatus=" + this.targetStatus
                + ",sourceIdentity=" + this.submittedSourceIdentity()
                + ",sourceReadinessMatrix=" + this.sourceReadinessMatrix()
                + ",finalSourceIdentity=" + this.finalSourceIdentitySummary()
                + ",shaderOutputImageCandidate=\"" + this.shaderOutputImageCandidate.boundarySummary() + "\""
                + ",denoiseEvidenceBoundary=" + this.denoiseEvidenceBoundarySummary()
                + ",finalCompositeDenoisedSourceIdentityBoundary="
                + this.finalCompositeDenoisedSourceIdentityBoundary()
                + ",shaderGeneratedDenoiseOutputStatus=\""
                + this.shaderGeneratedDenoiseOutputStatus.boundarySummary() + "\""
                + ",shaderDenoiseOutputPassAttempted=" + this.submittedShaderDenoiseOutputPassAttempted()
                + ",shaderDenoiseOutputTextureAllocated=" + this.submittedShaderDenoiseOutputTextureAllocated()
                + ",shaderDenoiseOwnedOutputImage=" + this.submittedShaderDenoiseOwnedOutputImage()
                + ",shaderDenoiseOutputRenderPassSubmitted="
                + this.submittedShaderDenoiseOutputRenderPassSubmitted()
                + ",shaderDenoiseGeneratedOutputPassSubmitted="
                + this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                + ",shaderDenoisePassExecuted=" + this.submittedShaderDenoisePassExecuted()
                + ",shaderGeneratedDenoisePassExecuted=" + this.submittedShaderDenoisePassExecuted()
                + ",shaderOutputSourceConsumed=" + this.submittedShaderDenoiseOutputSourceConsumed()
                + ",shaderDenoiseOutputSourceConsumed=" + this.submittedShaderDenoiseOutputSourceConsumed()
                + ",shaderDenoiseOutputConsumedByFinalComposite="
                + this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                + ",shaderDenoisePassGeneratedVisualSource="
                + this.submittedShaderDenoisePassGeneratedVisualSource()
                + ",shaderDenoiseFinalCompositeConsumable="
                + this.submittedShaderDenoiseFinalCompositeConsumable()
                + ",finalCompositeConsumable=" + this.submittedShaderDenoiseFinalCompositeConsumable()
                + ",cpuReadbackFallbackActive=" + this.submittedShaderDenoiseCpuReadbackFallbackActive()
                + ",shaderDenoiseCpuReadbackFallbackActive="
                + this.submittedShaderDenoiseCpuReadbackFallbackActive()
                + ",cpuReadbackFallbackInactive=" + this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                + ",shaderDenoiseCpuReadbackFallbackInactive="
                + this.submittedShaderDenoiseCpuReadbackFallbackInactive()
                + ",stillNotComputeBoundary=" + this.submittedShaderDenoiseStillNotComputeBoundary()
                + ",sourceAuthenticity=" + this.sourceAuthenticityLabel()
                + ",authenticityGuards=" + this.authenticityGuardsSummary()
                + ",surfaceProjectionEvidence=" + this.surfaceProjectionEvidenceLabel()
                + ",geometryMaterialProjectionReadiness=" + this.geometryMaterialProjectionReadiness()
                + ",temporalStabilityReadiness=" + this.temporalStabilityReadiness()
                + ",focusWindowOnly=" + this.submittedFocusWindowOnly()
                + ",metadataOnlyPreview=" + this.submittedMetadataOnlyPreview()
                + ",proofMarkerSource=" + this.submittedProofMarkerSource()
                + ",temporaryDirectLightSubstitution=" + this.submittedTemporaryDirectLightSubstitution()
                + ",rectangularWashoutRisk=" + this.submittedRectangularWashoutRisk()
                + ",nativeShadowMapMask=" + this.submittedNativeShadowMapMask()
                + ",shadowMapOutputConsumed=" + this.submittedShadowMapOutputConsumed()
                + ",realShadowMapComposite=" + this.submittedRealShadowMapComposite()
                + ",screenSpaceShadowDecal=" + this.submittedScreenSpaceShadowDecalSource()
                + ",lowResDirectTextureShadowProof=" + this.submittedLowResDirectTextureShadowProof()
                + ",shadowMapCompositeNoOverclaim="
                + this.submittedShadowMapCompositeNoOverclaimBoundary()
                + ",sourceGatedSurfaceProjection=" + this.submittedSourceGatedSurfaceProjection()
                + ",localized_emissive_spill=" + finalPhysicalCompositeReady
                + ",emissive_spill_marker=\"localized_emissive_spill_source_surface_distance_normal_opacity_energy_recorded\""
                + ",hue_shifted_bounce=" + finalPhysicalCompositeReady
                + ",colored_bounce=" + finalPhysicalCompositeReady
                + ",colored_bounce_marker=\"colored_bounce_material_albedo_hit_coupled_samples_recorded\""
                + ",contact_shadow_darkening=" + finalPhysicalCompositeReady
                + ",contact_shadow_marker=\"contact_shadow_local_occlusion_surface_material_darkening_recorded\""
                + ",final_physical_composite_ready=" + finalPhysicalCompositeReady
                + ",final_physical_composite_marker=\"final_physical_composite_direct_spill_colored_bounce_contact_shadow_shader_denoise_submitted\""
                + ",metadata_only_proof_rejected=" + !this.submittedMetadataOnlyPreview()
                + ",focus_window_capture_rejected=" + !this.submittedFocusWindowOnly()
                + ",proof_marker_evidence_rejected=" + !this.submittedProofMarkerSource()
                + ",temporary_direct_substitution_rejected=" + !this.submittedTemporaryDirectLightSubstitution()
                + ",rectangular_washout_rejected=" + !this.submittedRectangularWashoutRisk()
                + ",wrong_window_screenshot_rejected=true"
                + ",blank_screenshot_rejected=true"
                + ",round7GiSource=" + this.submittedRound7GiSource()
                + ",focusedRegionReadiness=" + this.focusedRegionReadiness()
                + ",finalSurfaceProjectionQualityGate=" + this.finalSurfaceProjectionQualityGate()
                + ",visualProofMissingReason=" + this.visualProofMissingReason()
                + ",visualProofExpectation=\"" + this.visualProofExpectation() + "\""
                + ",reason=" + this.reason;
    }

    public String sourceReadinessMatrix() {
        return "direct=" + readyState(this.submittedDirectLightSource())
                + ",rawGI=" + readyState(this.submittedRawGiSource())
                + ",cpuDenoisedGI=" + readyState(this.submittedCpuDenoisedGiSource())
                + ",shaderDenoisedGI=" + readyState(this.submittedShaderDenoisedGiSource())
                + ",shaderOutputImageCandidate=" + readyState(this.submittedShaderOutputImageCandidate())
                + ",denoisedSourceIdentity=" + this.submittedDenoisedGiSourceIdentity().stableLabel()
                + ",shaderDenoiseSourceClassification=" + this.shaderDenoiseSourceClassification()
                + ",shaderDenoiseVisualShaderIntent=" + readyState(this.submittedShaderDenoiseVisualShaderIntent())
                + ",publicMojangShaderGeneratedVisualOutput="
                + readyState(this.submittedPublicMojangShaderGeneratedVisualOutput())
                + ",realShaderDenoiseDispatchReady=" + readyState(this.submittedRealShaderDenoiseDispatchReady())
                + ",shaderDenoiseRawDiffuseGiInput=" + readyState(this.submittedShaderDenoiseRawDiffuseGiInput())
                + ",shaderDenoiseDirectLightValidationInput="
                + readyState(this.submittedShaderDenoiseDirectLightValidationInput())
                + ",shaderDenoiseStrictRawDiffuseGiInput="
                + readyState(this.submittedShaderDenoiseStrictRawDiffuseGiInput())
                + ",shaderDenoiseOutputPassAttempted="
                + readyState(this.submittedShaderDenoiseOutputPassAttempted())
                + ",shaderDenoiseOutputTextureAllocated="
                + readyState(this.submittedShaderDenoiseOutputTextureAllocated())
                + ",shaderDenoiseOwnedOutputImage=" + readyState(this.submittedShaderDenoiseOwnedOutputImage())
                + ",shaderDenoiseOutputRenderPassSubmitted="
                + readyState(this.submittedShaderDenoiseOutputRenderPassSubmitted())
                + ",shaderDenoiseGeneratedOutputPassSubmitted="
                + readyState(this.submittedShaderDenoiseGeneratedOutputPassSubmitted())
                + ",shaderDenoisePassExecuted=" + readyState(this.submittedShaderDenoisePassExecuted())
                + ",shaderOutputSourceConsumed=" + readyState(this.submittedShaderDenoiseOutputSourceConsumed())
                + ",shaderOutputConsumedByFinalComposite="
                + readyState(this.submittedShaderDenoiseOutputConsumedByFinalComposite())
                + ",shaderDenoisePassGeneratedVisualSource="
                + readyState(this.submittedShaderDenoisePassGeneratedVisualSource())
                + ",shaderDenoiseFinalCompositeConsumable="
                + readyState(this.submittedShaderDenoiseFinalCompositeConsumable())
                + ",cpuReadbackFallbackActive="
                + readyState(this.submittedShaderDenoiseCpuReadbackFallbackActive())
                + ",cpuReadbackFallbackInactive="
                + readyState(this.submittedShaderDenoiseCpuReadbackFallbackInactive())
                + ",stillNotComputeBoundary=" + readyState(this.submittedShaderDenoiseStillNotComputeBoundary())
                + ",realShaderDenoiseOutputReady=" + readyState(this.submittedRealShaderDenoiseOutputReady())
                + ",shaderDenoiseOverclaimPresent=" + this.submittedShaderDenoiseOverclaim()
                + ",previewEvidenceClean=" + readyState(!this.submittedPreviewOnlyEvidence())
                + ",sourceGatedSurfaceProjection=" + readyState(this.submittedSourceGatedSurfaceProjection());
    }

    public String geometryMaterialProjectionReadiness() {
        if (!this.submitted || !this.drawCallsIssued) {
            return "not-ready:submission-missing";
        }
        if (this.targetStatus != TargetStatus.READY) {
            return "not-ready:target-" + this.targetStatus.name().toLowerCase(Locale.ROOT);
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "not-ready:" + this.sourceAuthenticityLabel();
        }
        if (this.submittedDirectLightSource() && this.submittedRawGiSource() && this.submittedDenoisedGiSource()) {
            return "candidate:source-separated-full-target;"
                    + this.surfaceProjectionEvidenceLabel()
                    + ";physical-geometry-material-quality=pending-controller-proof";
        }
        if (this.submittedRound7GiSource()) {
            return "partial:gi-source-submitted;final-direct-plus-denoise-stack-incomplete";
        }
        if (this.submittedDirectLightSource()) {
            return "partial:direct-source-submitted;gi-denoise-stack-incomplete";
        }
        return "not-ready:unknown-source";
    }

    public String temporalStabilityReadiness() {
        if (!this.submittedDenoisedGiSource()) {
            return "not-ready:no-denoised-source";
        }
        if (this.submittedShaderDenoisedGiSource()) {
            return "candidate:shader-denoised-source-present;requires stable/moved screenshot sequence proof";
        }
        return "partial:cpu-denoised-source-present;requires stable/moved screenshot sequence proof and must not claim shader denoise";
    }

    public String denoiseSourceClassLabel() {
        if (this.submittedDenoisedGiSourceIdentity() == DenoisedGiSourceIdentity.MIXED_CPU_AND_SHADER) {
            return "mixed-cpu-and-shader-denoised-gi";
        }
        if (this.submittedShaderDenoisedGiSource()) {
            return "shader-denoised-gi";
        }
        if (this.submittedCpuDenoisedGiSource()) {
            return "cpu-denoised-gi";
        }
        return "denoised-gi-missing";
    }

    public boolean submittedShaderDenoiseOutputClaimPresent() {
        String normalizedReason = this.normalizedReason();
        return this.submitted
                && (this.submittedRealShaderDenoiseOutputReady()
                || normalizedReason.contains("shaderdenoiseshadergeneratedoutput=true")
                || normalizedReason.contains("shader_denoise_shader_generated_output=true")
                || normalizedReason.contains("round7.shaderdenoise.shadergeneratedoutput=true")
                || normalizedReason.contains("shadergenerateddenoisedgi=public-mojang-fragment-pass")
                || normalizedReason.contains("shadergenerateddenoisedgi=ready")
                || normalizedReason.contains("sourcekind=shader-generated-denoised-gi")
                || normalizedReason.contains("sourcekind=shader_generated_denoised_gi"));
    }

    public String shaderDenoiseSourceClassification() {
        if (this.submittedRealShaderDenoiseOutputReady()) {
            return "true-shader-generated-output";
        }
        if (this.submittedShaderDenoiseDirectLightValidationInput()) {
            return "diagnostic-direct-light-validation-fallback-not-strict-proof";
        }
        if (this.submittedShaderDenoisePassGeneratedVisualSource()) {
            return "partial-shader-generated-pass-output-contract-incomplete";
        }
        if (this.submittedShaderOutputImageCandidate()) {
            return "shader-output-image-candidate-only";
        }
        if (this.submittedPublicMojangShaderGeneratedVisualOutput()) {
            return this.submittedCpuDenoisedGiSource()
                    ? "public-mojang-shader-generated-visual-output-over-cpu-readback"
                    : "public-mojang-shader-generated-visual-output";
        }
        if (this.submittedShaderDenoiseVisualShaderIntent()) {
            return this.submittedCpuDenoisedGiSource()
                    ? "visual-shader-over-cpu-readback-denoised-gi"
                    : "visual-shader-intent-without-denoised-source";
        }
        if (this.submittedCpuDenoisedGiSource()) {
            return "cpu-readback-denoised-gi";
        }
        return "denoised-source-missing";
    }

    public String shaderDenoiseOutputBlockerSource() {
        if (this.submittedRealShaderDenoiseOutputReady()) {
            return "none:true-shader-generated-output-ready";
        }
        if (this.submittedShaderOutputImageCandidate()) {
            return this.shaderOutputImageCandidate.blocker();
        }
        if (this.submittedShaderDenoiseDirectLightValidationInput()) {
            return "rejected:direct-light-validation-input;strict-proof-requires-raw-diffuse-gi";
        }
        if (this.submittedExplicitShaderDenoiseOutputFalse()) {
            return "explicit-status:false-real-shader-denoise-output";
        }
        if (this.submittedShaderDenoiseOwnedOutputImage()
                && this.submittedShaderDenoiseGeneratedOutputPassSubmitted()
                && !this.submittedShaderDenoiseOutputConsumedByFinalComposite()) {
            return "partial:owned-output-and-generated-pass-present;missing-final-composite-consumption";
        }
        if (this.submittedShaderDenoiseOutputConsumedByFinalComposite()
                && !this.submittedShaderDenoiseCpuReadbackFallbackInactive()) {
            return "partial:final-composite-consumption-present;cpu-readback-fallback-inactive-not-proven";
        }
        if (this.shaderGeneratedDenoiseOutputStatus.partialEvidencePresent()
                || this.submittedShaderDenoiseOutputPassAttempted()
                || this.submittedShaderDenoiseOutputTextureAllocated()
                || this.submittedShaderDenoiseOutputRenderPassSubmitted()
                || this.submittedShaderDenoiseOutputImageReady()
                || this.submittedShaderDenoiseOutputConsumedByFinalComposite()) {
            return "partial:shader-generated-denoise-output-contract-incomplete";
        }
        if (this.submittedPublicMojangShaderGeneratedVisualOutput()) {
            return "none:public-mojang-visual-shader-draw-submitted;real-denoise-output-still-requires-explicit-shader-output-markers";
        }
        if (this.submittedShaderDenoiseVisualShaderIntent()) {
            return "visual-shader-intent-only:uses-cpu-readback-or-unproven-source";
        }
        return "missing:true-shader-output-readiness-markers";
    }

    public String surfaceProjectionEvidenceLabel() {
        if (!this.submitted) {
            return "not-submitted";
        }
        if (this.submittedPreviewOnlyEvidence()) {
            return "rejected-preview-shortcut";
        }
        if (this.submittedSourceGatedSurfaceProjection()) {
            return "source-gated-scene-shaped-preview";
        }
        return "unlabeled-requires-controller-proof";
    }

    private static void appendIdentity(StringBuilder identity, boolean present, String label) {
        if (!present) {
            return;
        }
        if (identity.length() > 0) {
            identity.append("+");
        }
        identity.append(label);
    }

    private static String readyState(boolean ready) {
        return ready ? "ready" : "missing";
    }

    private String normalizedReason() {
        return this.reason.toLowerCase(Locale.ROOT);
    }

    public boolean submittedExplicitShaderDenoiseOutputFalse() {
        String normalizedReason = this.normalizedReason();
        return normalizedReason.contains("realdenoiseshaderoutput=false")
                || normalizedReason.contains("real_denoise_shader_output=false")
                || normalizedReason.contains("shaderdenoiseoutput=false")
                || normalizedReason.contains("gpu_denoise_output=false")
                || normalizedReason.contains("realshaderdenoiseoutputready=false")
                || normalizedReason.contains("shaderdenoisedgi=pending-realdenoiseshaderoutput")
                || normalizedReason.contains("shaderdenoisedgi=enabled-missing")
                || normalizedReason.contains("shaderdenoisedgi=candidate")
                || normalizedReason.contains("shadergenerateddenoisedgi=missing")
                || normalizedReason.contains("shadergenerateddenoisedgi=candidate")
                || normalizedReason.contains("sourcekind=shader-output-image-candidate")
                || normalizedReason.contains("sourcekind=shader_output_image_candidate")
                || normalizedReason.contains("sourcekind=cpu-denoised-readback")
                || normalizedReason.contains("sourcekind=cpu_denoised_readback")
                || normalizedReason.contains("shaderdenoiseinputkind=native-direct-light-rgba8-validation-input")
                || normalizedReason.contains("shader_denoise_input_kind=native-direct-light-rgba8-validation-input")
                || normalizedReason.contains("shaderdenoiseinputmode=diagnostic-direct-light-validation-fallback")
                || normalizedReason.contains("shader_denoise_input_mode=diagnostic-direct-light-validation-fallback")
                || normalizedReason.contains("directlightvalidationinput=true")
                || normalizedReason.contains("direct_light_validation_input=true");
    }

    public enum TargetStatus {
        NOT_REQUESTED,
        TARGET_MISSING,
        METADATA_ONLY,
        JAVA_OPAQUE_OBJECTS_PRESENT,
        NATIVE_WRITABLE_UNAVAILABLE,
        READY,
        UNKNOWN
    }

    public record ShaderOutputImageCandidate(
            boolean candidatePresent,
            int width,
            int height,
            long checksum,
            String sourceLabel,
            String blocker
    ) {
        private static final String SOURCE_NONE = "none";
        private static final String BLOCKER_NONE = "not-provided";

        public ShaderOutputImageCandidate {
            if (!candidatePresent) {
                width = 0;
                height = 0;
                checksum = 0L;
            }
            if (width < 0) {
                width = 0;
            }
            if (height < 0) {
                height = 0;
            }
            if (sourceLabel == null || sourceLabel.isBlank()) {
                sourceLabel = candidatePresent ? "shader-output-image-candidate" : SOURCE_NONE;
            } else {
                sourceLabel = sourceLabel.trim();
            }
            if (blocker == null || blocker.isBlank()) {
                blocker = candidatePresent
                        ? "candidate-only:not-real-shader-generated-denoised-output"
                        : BLOCKER_NONE;
            } else {
                blocker = blocker.trim();
            }
        }

        public static ShaderOutputImageCandidate none() {
            return new ShaderOutputImageCandidate(false, 0, 0, 0L, SOURCE_NONE, BLOCKER_NONE);
        }

        public static ShaderOutputImageCandidate candidate(
                int width,
                int height,
                long checksum,
                String sourceLabel,
                String blocker
        ) {
            return new ShaderOutputImageCandidate(true, width, height, checksum, sourceLabel, blocker);
        }

        public String dimensionsLabel() {
            return this.candidatePresent ? this.width + "x" + this.height : "none";
        }

        public String checksumLabel() {
            return this.candidatePresent ? Long.toUnsignedString(this.checksum) : "none";
        }

        public String boundarySummary() {
            if (!this.candidatePresent) {
                return "present=false,dims=none,checksum=none,source=none,blocker=not-provided,"
                        + "realShaderDenoiseOutputReady=not-applicable,sourceKind=none";
            }
            return "present=" + this.candidatePresent
                    + ",dims=" + this.dimensionsLabel()
                    + ",checksum=" + this.checksumLabel()
                    + ",source=" + this.sourceLabel
                    + ",blocker=" + this.blocker
                    + ",realShaderDenoiseOutputReady=false"
                    + ",sourceKind=shader-output-image-candidate";
        }
    }

    public enum DenoisedGiSourceIdentity {
        NONE("none"),
        CPU_DENOISED_READBACK("cpu-denoised-readback"),
        SHADER_GENERATED_DENOISED_GI("shader-generated-denoised-gi"),
        MIXED_CPU_AND_SHADER("mixed-cpu-and-shader-denoised-gi");

        private final String stableLabel;

        DenoisedGiSourceIdentity(String stableLabel) {
            this.stableLabel = stableLabel;
        }

        public String stableLabel() {
            return this.stableLabel;
        }
    }
}
