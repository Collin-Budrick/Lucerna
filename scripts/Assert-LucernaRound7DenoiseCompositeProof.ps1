<#
.SYNOPSIS
Controller-only Round 7 assertion helper for raw GI, denoised GI, and final composite evidence.

.DESCRIPTION
This script checks already captured screenshots and an optional controller launch log. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller has captured same-scene Round 7 artifacts. Use
-RequireShaderGeneratedDenoiseOutput only when the log should prove a consumed, shader-generated
denoise output image rather than the older open-boundary shader-denoise evidence. Use
-RequireTracedGiConsumption when the same proof must also show traced raw diffuse-GI consumption
by the final in-game composite.
Use -RequireFullRendererMilestoneProof when the log should prove the next full-renderer
milestone in one in-world run: physical GI evidence, real shadow-map output and final
composite consumption, true depth/G-buffer sampling, traced lighting consumption, and
shader-generated denoise output, with screenshot/proof-overlay/metadata shortcuts rejected.
Use -RequirePlayablePhysicalRendererMilestoneProof when the same evidence must be present
inside a playable-budget capture: Vulkan, renderer enabled, low-cost renderer budget markers,
shadow-map consumption, true depth/G-buffer sampling, traced lighting consumption, and
shader-generated denoise output, while proof overlays, fullscreen blobs, low-res substitutions,
and focus-window shortcuts are rejected.
Use -RequireRealWorldVisualQualityComparisonProof after the controller captures comparable
baseline, normal-enabled, and playable-physical screenshots from the same scene. This rejects
the old low-resolution overlay/fixed-blob/fullscreen-wash failure mode and requires the
log to prove normal heavy-workload bypass plus playable physical renderer evidence.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $RawGiImagePath,

    [Parameter(Mandatory = $true)]
    [string] $DenoisedGiImagePath,

    [Parameter(Mandatory = $true)]
    [string] $FinalCompositeImagePath,

    [string] $DirectImagePath = "",

    [string] $DebugImagePath = "",

    [string] $LogPath = "",

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 30.0,

    [double] $RegionTopPercent = 20.0,

    [double] $RegionWidthPercent = 40.0,

    [double] $RegionHeightPercent = 55.0,

    [switch] $DisableAutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 10.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 80.0,

    [double] $DenoiseAutoRegionSearchHeightPercent = 55.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinRawChangedPixelPercent = 0.5,

    [double] $MinDirectChangedPixelPercent = 0.5,

    [double] $MinDirectMeanAbsLuma = 0.5,

    [double] $MinDenoiseChangedPixelPercent = 0.1,

    [double] $MinDenoiseMeanAbsLuma = 0.1,

    [double] $MinDenoiseRoughnessReductionPercent = 0.0,

    [double] $MinDenoiseEdgePreservationPercent = 0.0,

    [double] $MinFinalChangedPixelPercent = 0.5,

    [double] $MinFinalMeanAbsLuma = 0.5,

    [string[]] $DirectSourcePatterns = @(
        "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*",
        "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true",
        "direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated",
        "round7\.finalCompositeSourceMix=base=true,direct=enabled-ready",
        "direct=enabled-ready",
        "native direct.*(?:source|output|payload).*ready=true",
        "mode=(?:DIRECT_ONLY|direct-only)",
        "mode=(?:ROUND7_DIRECT|round7-direct|final-lucerna-composite).*direct"
    ),

    [string[]] $NativeGiSourcePatterns = @(
        "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
        "nativeGiOutputSourcePresent=True",
        "nativeGiOutputSourcePresent=true",
        "sourceIdentity=native-diffuse-gi-rgba8",
        "sourceIdentity=native-denoised-diffuse-gi-rgba8",
        "round7\.rawGi\.nativeDiffuseGiPayload",
        "nativeDiffuseGiPayload",
        "native[-_ ]?diffuse[-_ ]?gi.*(?:source|output|payload).*ready",
        "physical_scene_linked=true.*physical_surface_contribution=true",
        "physicalGI sceneLinked=true surfaceContribution=true"
    ),

    [string[]] $RawGiSourcePatterns = @(
        "round7\.rawGiSource=lucerna\.lighting\.diffuseGi",
        "rawGiSource=lucerna\.lighting\.diffuseGi",
        "rawGiOutput=lucerna\.lighting\.diffuseGi",
        "rawGi=true",
        "raw_gi_input_available=true",
        "raw_input_marker=",
        "Lucerna Round 7 raw GI",
        "public Mojang Round 7 RAW_GI visual render pass submitted",
        "mode=ROUND7_RAW_GI",
        "round7\.rawGi\.nativeDiffuseGiPayload",
        "round7-raw-gi-native-diffuse-source-additive",
        "mode=round7-raw-gi",
        "raw(?:_| )?gi.*(?:source|output).*native"
    ),

    [string[]] $DenoiseDispatchPatterns = @(
        "Lucerna Round 7 denoise",
        "Lucerna Round 7 denoised GI CPU output",
        "denoise dispatch",
        "denoise_execution=\{",
        "first_practical_cpu_denoised_diffuse_gi_rgba8_generated",
        "denoisedCpuOutputGenerated=true",
        "denoised_cpu_output_generated=true",
        "mode=round7-denoised-gi"
    ),

    [string[]] $DenoisedGiOutputPatterns = @(
        "round7\.denoisedGiOutput=lucerna\.denoise\.diffuse",
        "denoisedGiOutput=lucerna\.denoise\.diffuse",
        "denoisedOutputResource=lucerna\.denoise\.diffuse",
        "denoisedPayloadReady=true",
        "denoisedPayloadEvidence=denoised_diffuse_gi_rgba8_first_practical_cpu_output",
        "denoised_diffuse_gi_cpu_rgba8_output_generated_from_raw_gi",
        "denoised_output_marker=",
        "denoisedOutputMarker=",
        "denoisedPayloadReady=true",
        "readyForPreviewDraw=true",
        "denoisedOutputDiffersFromRaw=true",
        "denoised(?:_| )?(?:gi|diffuse).*output.*(?:lucerna\.denoise\.diffuse|native|texture)",
        "mode=round7-denoised-gi"
    ),

    [string[]] $FinalCompositePatterns = @(
        "round7\.finalCompositeMode=round7\.composite\.final\.base_direct_gi",
        "compositeEvidenceKey=round7\.composite\.final\.base_direct_gi",
        "evidenceKey=round7\.composite\.final\.base_direct_gi",
        "mode=final-lucerna-composite",
        "mode=FINAL_LUCERNA_COMPOSITE",
        "Lucerna Round 7 final composite",
        "mode=round7-final-composite",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
        "final composite.*(?:submitted=true|dispatch)",
        "composite mode.*(?:final|lucerna)",
        "mode=final-lucerna-composite"
    ),

    [string[]] $HudSafeFinalCompositePatterns = @(
        "round7\.finalCompositeHudSafe=true",
        "hudSafeFinalComposite=true",
        "hud_preserved=true",
        "HUD-safe final composite",
        "before hand/HUD composition",
        "HUD remains readable"
    ),

    [string[]] $ShaderDenoiseIntentPatterns = @(
        "(?:shaderDenoiseIntent|shader_denoise_intent|denoiseShaderIntent|denoise_shader_intent)=true",
        "shaderDenoiseVisualShaderIntent=true",
        "(?:round7\.shaderDenoise|shader_denoise|denoiseShader).*intent(?:=|:)(?:true|enabled|requested)",
        "LUCERNA_ROUND7_SHADER_DENOISE_INTENT=true",
        "round7ShaderDenoiseEvidence=True"
    ),

    [string[]] $ShaderDenoiseInputReadyPatterns = @(
        "(?:shaderDenoiseInputReady|shader_denoise_input_ready|shaderDenoiseRawInputReady|shader_denoise_raw_input_ready)=true",
        "(?:rawGiInputReady|raw_gi_input_ready|rawInputReady|raw_input_ready)=true",
        "(?:rawGI|cpuDenoisedGI)=(?:enabled-)?ready",
        "(?:shaderDenoiseInput|shader_denoise_input).*?(?:raw|diffuse|gi).*?(?:ready|available)=true",
        "(?:denoiseShaderInput|denoise_shader_input).*?(?:ready|available)=true"
    ),

    [string[]] $ShaderDenoiseRawGiCpuReadbackInputPatterns = @(
        "(?:round7\.shaderDenoise\.rawGiCpuReadbackInput|rawGiCpuReadbackInput|raw_gi_cpu_readback_input)=true"
    ),

    [string[]] $ShaderDenoiseRawDiffuseGiInputSourcePatterns = @(
        "(?:round7\.shaderDenoise\.inputKind|shaderDenoiseInputKind|shader_denoise_input_kind|shaderInputKind|shader_input_kind)=raw-diffuse-gi-rgba8",
        "shader input kind=raw-diffuse-gi-rgba8",
        "public Mojang Round 7 shader-denoise output mode can bind the raw diffuse-GI input texture"
    ),

    [string[]] $ShaderDenoiseDirectLightValidationInputPatterns = @(
        "(?:round7\.shaderDenoise\.directLightValidationInput|directLightValidationInput|direct_light_validation_input)=true",
        "(?:round7\.shaderDenoise\.inputKind|shaderDenoiseInputKind|shader_denoise_input_kind|shaderInputKind|shader_input_kind)=native-direct-light-rgba8-validation-input",
        "native-direct-light-rgba8-validation-input"
    ),

    [string[]] $ShaderDenoiseDispatchPreparedPatterns = @(
        "(?:round7\.shaderDenoise\.dispatchPrepared|shaderDenoiseDispatchPrepared|shader_denoise_dispatch_prepared)=true"
    ),

    [string[]] $ShaderDenoiseOutputAttemptedPatterns = @(
        "(?:round7\.shaderDenoise\.outputAttempted|shaderDenoiseOutputAttempted|shader_denoise_output_attempted)=(?:true|false)",
        "shader_denoise_output_attempt=(?:cpu_candidate_staged|metadata_accepted_no_candidate|not_started)"
    ),

    [string[]] $ShaderDenoiseOutputAttemptGenerationPatterns = @(
        "(?:round7\.shaderDenoise\.outputAttemptGeneration|shaderDenoiseOutputAttemptGeneration|shader_denoise_output_attempt_generation)=[0-9]+"
    ),

    [string[]] $ShaderDenoiseOutputImageReadyPatterns = @(
        "(?:round7\.shaderDenoise\.outputImageReady|shaderDenoiseOutputImageReady|shader_denoise_output_image_ready)=true",
        "Lucerna native shader denoise output image candidate: ready=true"
    ),

    [string[]] $ShaderDenoiseOutputImageNotReadyPatterns = @(
        "(?:round7\.shaderDenoise\.outputImageReady|shaderDenoiseOutputImageReady|shader_denoise_output_image_ready)=false",
        "Lucerna native shader denoise output image candidate: ready=false"
    ),

    [string[]] $ShaderDenoiseOutputMaterialReadyPatterns = @(
        "(?:round7\.shaderDenoise\.outputMaterialReady|shaderDenoiseOutputMaterialReady|shader_denoise_output_material_ready)=true"
    ),

    [string[]] $ShaderDenoiseOutputMaterialNotReadyPatterns = @(
        "(?:round7\.shaderDenoise\.outputMaterialReady|shaderDenoiseOutputMaterialReady|shader_denoise_output_material_ready)=false"
    ),

    [string[]] $ShaderDenoiseShaderGeneratedOutputTruePatterns = @(
        "(?:round7\.shaderDenoise\.shaderGeneratedOutput|shaderDenoiseShaderGeneratedOutput|shader_denoise_shader_generated_output|shaderGeneratedDenoiseOutput)=true",
        "Lucerna native shader denoise output image candidate: .*realShaderGenerated=true"
    ),

    [string[]] $ShaderDenoiseShaderGeneratedOutputFalsePatterns = @(
        "(?:round7\.shaderDenoise\.shaderGeneratedOutput|shaderDenoiseShaderGeneratedOutput|shader_denoise_shader_generated_output|shaderGeneratedDenoiseOutput)=false",
        "Lucerna native shader denoise output image candidate: .*realShaderGenerated=false"
    ),

    [string[]] $ShaderDenoiseCpuReadbackFallbackActivePatterns = @(
        "(?:round7\.shaderDenoise\.cpuReadbackFallbackActive|shaderDenoiseCpuReadbackFallbackActive|cpuReadbackDenoiseFallbackActive|cpu_readback_denoise_fallback_active)=true",
        "Lucerna native shader denoise output image candidate: .*(?:cpuStaged|nonGpu)=true"
    ),

    [string[]] $ShaderDenoiseCpuReadbackFallbackInactivePatterns = @(
        "(?:round7\.shaderDenoise\.cpuReadbackFallbackActive|shaderDenoiseCpuReadbackFallbackActive|cpuReadbackDenoiseFallbackActive|cpu_readback_denoise_fallback_active)=false"
    ),

    [string[]] $RealShaderDenoiseOutputReadyPatterns = @(
        "(?:round7\.shaderDenoise\.realOutputReady|realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=true",
        "Lucerna native shader denoise output image candidate: .*realOutput=true"
    ),

    [string[]] $RealShaderDenoiseOutputNotReadyPatterns = @(
        "(?:round7\.shaderDenoise\.realOutputReady|realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=false",
        "real_shader_output(?:_ready)?=false",
        "Lucerna native shader denoise output image candidate: .*realOutput=false"
    ),

    [string[]] $ShaderGeneratedDenoiseOutputEvidenceReadyPatterns = @(
        "(?:shaderGeneratedDenoiseOutputEvidence|shaderGeneratedDenoiseOutputEvidenceReady|shader_generated_denoise_output_evidence|shader_generated_denoise_output_evidence_ready)=true"
    ),

    [string[]] $ShaderDenoiseNoOverclaimPatterns = @(
        "(?:round7\.shaderDenoise\.noOverclaim|shaderDenoiseNoOverclaim|shader_denoise_no_overclaim)=true",
        "shader_denoise_no_overclaim=true;[^`r`n]*(?:real_shader_output_ready|real_shader_output)=false"
    ),

    [string[]] $ShaderDenoiseOutputConsumedPatterns = @(
        "(?:shaderOutputSourceConsumed|shaderDenoiseOutputSourceConsumed|shader_denoise_output_source_consumed)=true"
    ),

    [string[]] $ShaderDenoiseFinalCompositeConsumablePatterns = @(
        "(?:shaderDenoiseFinalCompositeConsumable|shader_denoise_final_composite_consumable|finalCompositeConsumable)=true"
    ),

    [string[]] $TracedGiConsumedPatterns = @(
        "(?:realTracedLightingConsumed|tracedLightingConsumed|traced_lighting_consumed|voxelRayTracedLightingConsumed|rayTracedLightingConsumed)=true",
        "(?:tracedLightingConsumedByFinalComposite|traced_lighting_consumed_by_final_composite|traceFinalGiSourceConsumed|trace_final_gi_source_consumed|finalGiSourceConsumed|final_gi_source_consumed)=true"
    ),

    [string[]] $TracedGiFinalCompositeConsumptionPatterns = @(
        "(?:tracedLightingConsumedByFinalComposite|traced_lighting_consumed_by_final_composite|traceFinalGiSourceConsumed|trace_final_gi_source_consumed|finalGiSourceConsumed|final_gi_source_consumed)=true",
        "(?:Lucerna real renderer milestone 1: |finalCompositeSubmitted=true|final_composite_submitted=true)[^`r`n]*(?:realTracedLightingConsumed|tracedLightingConsumed|traced_lighting_consumed|voxelRayTracedLightingConsumed|rayTracedLightingConsumed)=true"
    ),

    [string[]] $TracedGiMetadataOnlyPatterns = @(
        "(?:metadataOnlyTracing|metadata_only_tracing)=true",
        "(?:tracedLightingMetadataOnly|traced_lighting_metadata_only)=true"
    ),

    [string[]] $RealGpuTraversalExecutedPatterns = @(
        "(?:realGpuTraversalExecuted|real_gpu_traversal_executed|round10\.realGpuTraversalExecuted)=true",
        "(?:realGpuTracedLightingConsumed|real_gpu_traced_lighting_consumed|traceRealGpuTraversalConsumed|trace_real_gpu_traversal_consumed)=true"
    ),

    [string[]] $RealGpuTraversalAllowedEvidencePatterns = @(
        "(?:realGpuTraversalEvidence|real_gpu_traversal_evidence|gpuTraversalEvidence|gpu_traversal_evidence)=true",
        "(?:gpuTraversalOutputReady|gpu_traversal_output_ready|hardwareRtExecutionProven|hardware_rt_execution_proven)=true",
        "(?:traversalBackend|traversal_backend|traceEvidenceSource|trace_evidence_source)=(?:gpu|vulkan-gpu|hardware-rt|hardware_rt|rt-hardware|compute)"
    ),

    [string[]] $FullRendererProofProfilePatterns = @(
        "realRendererMilestone1\.fullRendererProofProfile=true.*sameInWorldRunRequired=true",
        "realRendererMilestone1\.proofScope=full-renderer-proof",
        "realRendererMilestone1ProofScope=full-renderer-proof"
    ),

    [string[]] $PlayablePhysicalRendererProfilePatterns = @(
        "realRendererMilestone1\.playablePhysicalProfile=true",
        "realRendererMilestone1\.proofScope=playable-physical-renderer-proof",
        "realRendererMilestone1ProofScope=playable-physical-renderer-proof"
    ),

    [string[]] $PlayablePhysicalRendererBudgetPatterns = @(
        "(?:realRendererMilestone1\.playableBudgetRequired|playablePhysicalBudgetRequired|playable_physical_budget_required)=true",
        "(?:playablePhysicalRendererBudget|playable_physical_renderer_budget|lowCostPhysicalRendererBudget|low_cost_physical_renderer_budget)=(?:true|active|enabled)",
        "(?:heavyProofWorkload|heavy_proof_workload|fullProofWorkload|full_proof_workload)=false",
        "(?:proofTelemetryBudget|proof_telemetry_budget|rendererTelemetryBudget|renderer_telemetry_budget)=(?:playable|low-cost|budgeted)"
    ),

    [string[]] $RealWorldVisualQualityComparisonProfilePatterns = @(
        "realRendererMilestone1\.visualQualityComparisonProfile=true.*sameSceneBaselineEnabledPlayableRequired=true",
        "realRendererMilestone1\.visualQualityComparisonSequence=.*PhysicalBaseline.*VisualQualityEnabled.*VisualQualityPlayablePhysical",
        "realRendererMilestone1\.visualQualityRejects=.*fullscreenWash.*fixedLightBlob.*proofOverlays.*focusWindowOnly.*lowResDebugSubstitution.*screenSpaceDecalOnly.*wrongWindow.*menuChatScreenshots"
    ),

    [string[]] $NormalHeavyWorkloadBypassPatterns = @(
        "realRendererMilestone1\.visualQualityEnabled=true.*heavyProofWorkload=false.*fullProofWorkload=false.*normalGameplayPathRequired=true.*nativeLightingDispatchBypassed=true.*nativeReadbackBypassed=true.*round9Round10Round11TelemetryBypassed=true",
        "Lucerna playable renderer path active: .*heavyProofWorkload=false.*nativeLightingDispatchBypassed=true.*nativeReadbackBypassed=true.*round9Round10Round11TelemetryBypassed=true"
    ),

    [string[]] $SoftReceiverTiedShadowMaskPatterns = @(
        "(?:softReceiverTiedShadowMask|soft_receiver_tied_shadow_mask|receiverTiedSoftShadowMask|receiver_tied_soft_shadow_mask)=true",
        "(?:shadowMaskReceiverTied|shadow_mask_receiver_tied|shadowMaskReceiverWorldSpace|shadow_mask_receiver_world_space)=true",
        "(?:shadowMaskSoftened|shadow_mask_softened|softShadowMask|soft_shadow_mask)=true"
    ),

    [string[]] $CleanInWorldCaptureContractPatterns = @(
        "realRendererMilestone1\.cleanCaptureContract=.*inWorldOnly.*menuClosedRequired=true.*chatClosedRequired=true",
        "realRendererMilestone1CleanInWorldScreenshotRequired=True",
        "realRendererMilestone1CleanInWorldScreenshotRequired=true"
    ),

    [string[]] $MenuChatScreenshotContaminationPatterns = @(
        "(?:pauseMenuOpen|pause_menu_open|menuOpen|menu_open|screenOpen|screen_open)=true",
        "(?:chatOpen|chat_open|chatScreenOpen|chat_screen_open)=true",
        "(?:screenshotContainsMenu|screenshot_contains_menu|menuScreenshot|menu_screenshot)=true",
        "(?:screenshotContainsChat|screenshot_contains_chat|chatScreenshot|chat_screenshot)=true"
    ),

    [string[]] $ProofOverlayEvidencePatterns = @(
        "(?:proofOverlayVisible|proof_overlay_visible)=true",
        "(?:proofOverlayForbidden|proof_overlay_forbidden)=false",
        "(?:debugOverlay|debug\.overlay)=SHADER_DENOISE_OUTPUT_PROOF",
        "Overlay state: SHADER_DENOISE_OUTPUT_PROOF"
    ),

    [string[]] $LowResDebugSubstitutionPatterns = @(
        "(?:lowResolutionDirectTextureDraw|low_resolution_direct_texture_draw)=true",
        "(?:lowResDebugMarker|low_res_debug_marker|lowResolutionDebugMarker|low_resolution_debug_marker)=true",
        "(?:debugMarkerOnly|debug_marker_only)=true",
        "(?:cpuDirectTextureComposite|cpu_direct_texture_composite)=true",
        "diagnostic-fullscreen",
        "fullscreen-warm-additive",
        "fullscreenWash=true",
        "fullscreen_wash=true",
        "fullscreen-wash",
        "(?:fullscreen_blob|fullscreenBlob|fullscreen_blob_visual|fixed_light_blob|fixedLightBlob|fixedLightBlobVisual|fixed_light_blob_visual)=true",
        "(?:screenSpaceDecalOnly|screen_space_decal_only)=true",
        "(?:lowResDebugSubstitution|low_res_debug_substitution)=true",
        "(?:heavyProofWorkload|heavy_proof_workload|fullProofWorkload|full_proof_workload)=true",
        "(?:playablePhysicalRendererBudgeted|playable_physical_renderer_budgeted|playablePhysicalRendererBudget|playable_physical_renderer_budget|lowCostPhysicalRendererBudget|low_cost_physical_renderer_budget)=(?:false|inactive|disabled)",
        "(?:proofTelemetryBudget|proof_telemetry_budget|rendererTelemetryBudget|renderer_telemetry_budget)=(?:full|unbounded|heavy)",
        "(?:minecraftFps|minecraft_fps|currentFps|current_fps|proofFps|proof_fps|fps)=0(?:\.0+)?(?:\D|$)",
        "(?:minecraftFps|minecraft_fps|currentFps|current_fps|proofFps|proof_fps|fps)=1(?:\.0+)?(?:\D|$)"
    ),

    [string[]] $TrueDepthGBufferSamplingPatterns = @(
        "(?:trueDepthSampling|trueDepthGBufferSampling|realDepthGBufferSampling|real_depth_gbuffer_sampling|g_buffer_depth_texture_sampled|gBufferDepthTextureSampled)=true",
        "(?:depthSamplingPassOutputsReady|depth_sampling_pass_outputs_ready)=true",
        "(?:g_buffer_depth_sampling_evidence|gBufferDepthSamplingEvidence|shaderPassDepthSamplingEvidence|shader_pass_depth_sampling_evidence)=true"
    ),

    [string[]] $DepthGBufferSourcePatterns = @(
        "(?:depthSamplingEvidenceSources|depth_sampling_evidence_sources)=[^`r`n]*(?:java)[^`r`n]*(?:native)[^`r`n]*(?:shader)",
        "(?:depthSamplingPassOutputsMarker|depth_sampling_pass_outputs_marker)=java_native_shader_depth_sampling_evidence_parsed"
    ),

    [string[]] $DepthGBufferMetadataOnlyPatterns = @(
        "(?:gBufferDepthMetadataOnly|g_buffer_depth_metadata_only|depth_sampling_metadata_only)=true"
    ),

    [string[]] $RealShadowMapOutputPatterns = @(
        "(?:realShadowMapAttempted|shadowMapAttempted|shadow_map_attempted|shadowMapPassSubmitted|shadow_map_pass_submitted|realShadowMapPassSubmitted)=true",
        "(?:nativeShadowMapGenerated|realShadowMapGenerated|native_shadow_map_generated)=true",
        "(?:realShadowMapOutputReady|shadowMapOutputReady|shadow_map_output_ready|shadowMapDepthWritten|shadow_map_depth_written|shadowMapOutputWritten|shadow_map_output_written)=true"
    ),

    [string[]] $ShadowMapConsumptionPatterns = @(
        "(?:shadowMapOutputConsumedByFinalComposite|nativeShadowMapConsumedByFinalComposite|realShadowMapConsumedByFinalComposite|shadow_map_output_consumed_by_final_composite|shadowMapOutputConsumed|shadow_map_output_consumed)=true",
        "(?:shadowMapConsumptionMarker|shadow_map_consumption_marker)=native_shadow_map_sampled_by_final_composite",
        "(?:finalCompositeSubmitted|final_composite_submitted)=true[^`r`n]*(?:shadowMapOutputConsumed|shadow_map_output_consumed|shadowMapOutputConsumedByFinalComposite|shadow_map_output_consumed_by_final_composite)=true"
    ),

    [string[]] $FullRendererOverclaimPatterns = @(
        "(?:fullRendererProofOverclaimPresent|full_renderer_proof_overclaim_present|realRendererMilestone1OverclaimPresent|real_renderer_milestone1_overclaim_present)=true",
        "(?:metadataOnlyProofAccepted|metadata_only_proof_accepted)=true",
        "(?:focusWindowProofAccepted|focus_window_proof_accepted)=true",
        "(?:lowResDebugSubstitutionAccepted|low_res_debug_substitution_accepted)=true"
    ),

    [string[]] $ShaderDenoiseOutputCandidateOnlySourcePatterns = @(
        "(?:sourceIdentity|source_identity)=[^`r`n]*shader-output-image-candidate",
        "(?:sourceKind|source_kind)=(?:shader-output-image-candidate|shader-output-image-candidate-boundary)",
        "sourceKind=shader-output-image-candidate"
    ),

    [string[]] $CpuReadbackDenoiseSourcePatterns = @(
        "(?:cpuReadbackDenoiseSource|cpu_readback_denoise_source|cpuDenoisedSource|cpu_denoised_source)=true",
        "sourceIdentity=.*cpu-denoised-diffuse-gi-rgba8",
        "denoisedPayloadEvidence=.*(?:cpu|readback)",
        "denoised_diffuse_gi_cpu_rgba8_output_generated_from_raw_gi",
        "first_practical_cpu_denoised_diffuse_gi_rgba8_generated"
    ),

    [string[]] $ShaderDenoiseOutputReadyPatterns = @(
        "realShaderDenoiseOutputReady=true",
        "(?:shaderDenoiseOutputState|shader_denoise_output_state|shaderDenoiseReadiness|shader_denoise_readiness)=(?:ready|proven)",
        "(?:shaderDenoiseOutputReady|shader_denoise_output_ready|denoiseShaderOutputReady|denoise_shader_output_ready)=true"
    ),

    [string[]] $ShaderDenoiseSourceClaimPatterns = @(
        "sourceIdentity=.*shader-denoised-diffuse-gi-rgba8",
        "(?:accepted:|sourceAuthenticity=accepted:).*shader-denoised",
        "(?:shaderDenoiseSource|shader_denoise_source|denoiseShaderSource|denoise_shader_source)=shader"
    ),

    [string[]] $ShaderDenoiseOutputOpenPatterns = @(
        "(?:realDenoiseShaderOutput|real_denoise_shader_output|shaderDenoiseOutputReady|shader_denoise_output_ready)=false",
        "realShaderDenoiseOutputReady=false",
        "(?:shaderDenoiseBlocker|shader_denoise_blocker|shaderDenoiseBlockerReason|shader_denoise_blocker_reason)=[A-Za-z0-9_.-]+",
        "(?:shaderDenoiseOutputState|shader_denoise_output_state|shaderDenoiseReadiness|shader_denoise_readiness)=(?:open|false|not-ready|not_ready|pending|missing)",
        "(?:shaderDenoiseOutput|shader_denoise_output|denoiseShaderOutput|denoise_shader_output).*?(?:open|not-ready|not_ready|pending|unproven)"
    ),

    [string[]] $ShaderDenoiseOverclaimPatterns = @(
        "sourceIdentity=.*shader-denoised-diffuse-gi-rgba8.*(?:realDenoiseShaderOutput|real_denoise_shader_output|shaderDenoiseOutputReady|shader_denoise_output_ready)=false",
        "(?:realDenoiseShaderOutput|real_denoise_shader_output|shaderDenoiseOutputReady|shader_denoise_output_ready)=false.*sourceIdentity=.*shader-denoised-diffuse-gi-rgba8",
        "(?:accepted:|sourceAuthenticity=accepted:).*shader-denoised.*(?:realDenoiseShaderOutput|real_denoise_shader_output|shaderDenoiseOutputReady|shader_denoise_output_ready)=false"
    ),

    [switch] $RequireLogProof,

    [switch] $RequireDirectImage,

    [switch] $RequireDebugScreenshot,

    [switch] $RequireShaderDenoiseEvidence,

    [switch] $RequireShaderGeneratedDenoiseOutput,

    [switch] $RequireTracedGiConsumption,

    [switch] $RequirePhysicalGiEvidence,

    [switch] $RequireFullRendererMilestoneProof,

    [switch] $RequirePlayablePhysicalRendererMilestoneProof,

    [switch] $RequireRealWorldVisualQualityComparisonProof
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingFile {
    param(
        [string] $Path,
        [string] $Label
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        throw "$Label path is required."
    }
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Label path does not exist: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Resolve-OptionalFile {
    param(
        [string] $Path,
        [string] $Label
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return ""
    }
    return Resolve-ExistingFile $Path $Label
}

function Get-ImageDimensions {
    param([string] $Path)

    Add-Type -AssemblyName System.Drawing
    $image = [System.Drawing.Bitmap]::new($Path)
    try {
        return [ordered]@{
            width = $image.Width
            height = $image.Height
        }
    } finally {
        $image.Dispose()
    }
}

function Get-Luma {
    param([System.Drawing.Color] $Color)

    return (0.2126 * [int]$Color.R) + (0.7152 * [int]$Color.G) + (0.0722 * [int]$Color.B)
}

function Measure-ImageRoughness {
    param(
        [string] $Path,
        [object] $Region
    )

    Add-Type -AssemblyName System.Drawing
    $image = [System.Drawing.Bitmap]::new($Path)
    try {
        $left = [Math]::Max(0, [int]$Region.left)
        $top = [Math]::Max(0, [int]$Region.top)
        $right = [Math]::Min($image.Width, $left + [int]$Region.width)
        $bottom = [Math]::Min($image.Height, $top + [int]$Region.height)
        $edgeCount = 0L
        $sumAbsLuma = 0.0
        $sumSquaredLuma = 0.0
        $maxAbsLuma = 0.0

        for ($y = $top; $y -lt $bottom; $y++) {
            for ($x = $left; $x -lt $right; $x++) {
                $center = Get-Luma $image.GetPixel($x, $y)
                if ($x + 1 -lt $right) {
                    $delta = [Math]::Abs((Get-Luma $image.GetPixel($x + 1, $y)) - $center)
                    $edgeCount++
                    $sumAbsLuma += $delta
                    $sumSquaredLuma += ($delta * $delta)
                    $maxAbsLuma = [Math]::Max($maxAbsLuma, $delta)
                }
                if ($y + 1 -lt $bottom) {
                    $delta = [Math]::Abs((Get-Luma $image.GetPixel($x, $y + 1)) - $center)
                    $edgeCount++
                    $sumAbsLuma += $delta
                    $sumSquaredLuma += ($delta * $delta)
                    $maxAbsLuma = [Math]::Max($maxAbsLuma, $delta)
                }
            }
        }

        if ($edgeCount -le 0) {
            throw "Cannot measure roughness for an empty or one-pixel focus region in $Path."
        }

        $count = [double]$edgeCount
        return [ordered]@{
            edgeCount = $edgeCount
            meanAbsNeighborLuma = [Math]::Round($sumAbsLuma / $count, 4)
            rmsNeighborLuma = [Math]::Round([Math]::Sqrt($sumSquaredLuma / $count), 4)
            maxAbsNeighborLuma = [Math]::Round($maxAbsLuma, 4)
        }
    } finally {
        $image.Dispose()
    }
}

function Compare-Roughness {
    param(
        [object] $RawRoughness,
        [object] $DenoisedRoughness
    )

    $rawMean = [double]$RawRoughness.meanAbsNeighborLuma
    $denoisedMean = [double]$DenoisedRoughness.meanAbsNeighborLuma
    $meanReduction = if ($rawMean -gt 0.0) { 100.0 * ($rawMean - $denoisedMean) / $rawMean } else { 0.0 }
    $rawRms = [double]$RawRoughness.rmsNeighborLuma
    $denoisedRms = [double]$DenoisedRoughness.rmsNeighborLuma
    $rmsReduction = if ($rawRms -gt 0.0) { 100.0 * ($rawRms - $denoisedRms) / $rawRms } else { 0.0 }
    $rawMax = [double]$RawRoughness.maxAbsNeighborLuma
    $denoisedMax = [double]$DenoisedRoughness.maxAbsNeighborLuma
    $edgePreservation = if ($rawMax -gt 0.0) { 100.0 * $denoisedMax / $rawMax } else { 100.0 }

    return [ordered]@{
        raw = $RawRoughness
        denoised = $DenoisedRoughness
        meanAbsNeighborLumaReductionPercent = [Math]::Round($meanReduction, 4)
        rmsNeighborLumaReductionPercent = [Math]::Round($rmsReduction, 4)
        maxEdgeLumaPreservationPercent = [Math]::Round($edgePreservation, 4)
        roughnessImproved = $meanReduction -ge $MinDenoiseRoughnessReductionPercent
        edgeDetailPreserved = $edgePreservation -ge $MinDenoiseEdgePreservationPercent
    }
}

function Invoke-DeltaHelper {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [string] $Label,
        [double] $SearchHeightPercent = $AutoRegionSearchHeightPercent
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round7-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    try {
        if (-not $DisableAutoFocusRegion) {
            & $compareScript `
                    -BaselineImagePath $BaselinePath `
                    -EnabledImagePath $EnabledPath `
                    -OutputJsonPath $tempJson `
                    -RegionLeftPercent $RegionLeftPercent `
                    -RegionTopPercent $RegionTopPercent `
                    -RegionWidthPercent $RegionWidthPercent `
                    -RegionHeightPercent $RegionHeightPercent `
                    -ChangedPixelThreshold $ChangedPixelThreshold `
                    -BrightPixelThreshold $BrightPixelThreshold `
                    -AutoFocusRegion `
                    -AutoRegionSearchLeftPercent $AutoRegionSearchLeftPercent `
                    -AutoRegionSearchTopPercent $AutoRegionSearchTopPercent `
                    -AutoRegionSearchWidthPercent $AutoRegionSearchWidthPercent `
                    -AutoRegionSearchHeightPercent $SearchHeightPercent `
                    -AutoRegionColumns $AutoRegionColumns `
                    -AutoRegionRows $AutoRegionRows `
                    -AutoRegionPaddingCells $AutoRegionPaddingCells | Out-Host
        } else {
            & $compareScript `
                    -BaselineImagePath $BaselinePath `
                    -EnabledImagePath $EnabledPath `
                    -OutputJsonPath $tempJson `
                    -RegionLeftPercent $RegionLeftPercent `
                    -RegionTopPercent $RegionTopPercent `
                    -RegionWidthPercent $RegionWidthPercent `
                    -RegionHeightPercent $RegionHeightPercent `
                    -ChangedPixelThreshold $ChangedPixelThreshold `
                    -BrightPixelThreshold $BrightPixelThreshold | Out-Host
        }

        if (-not (Test-Path -LiteralPath $tempJson)) {
            throw "Image comparison helper did not write expected JSON: $tempJson"
        }
        return Get-Content -Raw -LiteralPath $tempJson | ConvertFrom-Json
    } finally {
        if (Test-Path -LiteralPath $tempJson) {
            Remove-Item -LiteralPath $tempJson -Force
        }
    }
}

function Test-Regex {
    param(
        [string] $Text,
        [string] $Pattern
    )

    return [regex]::IsMatch($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

function Test-AnyRegex {
    param(
        [string] $Text,
        [string[]] $Patterns
    )

    foreach ($pattern in $Patterns) {
        if (Test-Regex $Text $pattern) {
            return $true
        }
    }
    return $false
}

function Get-LastRegexMatch {
    param(
        [string] $Text,
        [string] $Pattern
    )

    $matches = [regex]::Matches(
        $Text,
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if ($matches.Count -le 0) {
        return $null
    }
    return $matches[$matches.Count - 1]
}

function Convert-ToNullableBool {
    param([string] $Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }
    $normalized = $Value.Trim().ToLowerInvariant()
    if (@("true", "ready", "present", "yes", "1") -contains $normalized) {
        return $true
    }
    if (@("false", "missing", "absent", "no", "0", "none") -contains $normalized) {
        return $false
    }
    return $null
}

function Convert-ToNullableInt64 {
    param([string] $Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "none") {
        return $null
    }
    try {
        return [long]$Value
    } catch {
        return $null
    }
}

function Convert-ToNullableDouble {
    param([string] $Value)

    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "none") {
        return $null
    }
    $parsed = 0.0
    if ([double]::TryParse(
            $Value,
            [System.Globalization.NumberStyles]::Float,
            [System.Globalization.CultureInfo]::InvariantCulture,
            [ref]$parsed)) {
        return $parsed
    }
    return $null
}

function Get-MaxRegexNumber {
    param(
        [string] $Text,
        [string] $Pattern
    )

    $max = 0L
    foreach ($match in [regex]::Matches($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        if ($match.Groups.Count -lt 2) {
            continue
        }
        $value = Convert-ToNullableInt64 $match.Groups[1].Value
        if ($null -ne $value) {
            $max = [Math]::Max($max, [long]$value)
        }
    }
    return $max
}

function Get-MaxRegexDouble {
    param(
        [string] $Text,
        [string] $Pattern
    )

    $max = 0.0
    foreach ($match in [regex]::Matches($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        if ($match.Groups.Count -lt 2) {
            continue
        }
        $value = Convert-ToNullableDouble $match.Groups[1].Value
        if ($null -ne $value) {
            $max = [Math]::Max($max, [double]$value)
        }
    }
    return $max
}

function Get-Round7PhysicalGiEvidence {
    param([string] $LogText)

    $physicalGiSamples = Get-MaxRegexNumber $LogText "(?:physical_gi_samples|physicalGiSamples)=(\d+)"
    $physicalGiHitSamples = Get-MaxRegexNumber $LogText "(?:physical_gi_hit_samples|physicalGiHitSamples)=(\d+)"
    $surfaceMaterialHitCoupledSamples = Get-MaxRegexNumber $LogText "(?:surface_material_hit_coupled_samples|surfaceMaterialHitCoupledSamples)=(\d+)"
    $geometryHitCoupledSamples = Get-MaxRegexNumber $LogText "(?:geometry_hit_coupled_samples|geometryHitCoupledSamples)=(\d+)"
    $surfaceMaterialHitCoupling = Get-MaxRegexDouble $LogText "(?:surface_material_hit_coupling|surfaceMaterialHitCoupling)=([0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)"
    $geometryHitCoupling = Get-MaxRegexDouble $LogText "(?:geometry_hit_coupling|geometryHitCoupling)=([0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)"
    $physicalSceneLinkScore = Get-MaxRegexNumber $LogText "(?:physical_scene_link_score|physicalSceneLinkScore|sceneScore)=(\d+)"
    $physicalOutputChecksum = Get-MaxRegexNumber $LogText "(?:physical_output_checksum|physicalOutputChecksum|physicalChecksum)=(\d+)"
    $physicalGiSampleMarkerPresent = Test-Regex $LogText "(?:physical_sample_marker|physicalSampleMarker)=`"?[^`"\r\n,}]+|physicalGI .*marker=(?!unknown)[^ `r`n]+"
    $surfaceMaterialHitMarkerPresent = Test-Regex $LogText "(?:surface_material_hit_marker|surfaceMaterialHitMarker)=`"?[^`"\r\n,}]+"
    $physicalSceneMarkerPresent = Test-Regex $LogText "(?:physical_scene_marker|physicalSceneMarker)=`"?[^`"\r\n,}]+|physicalGI .*marker=(?!unknown)[^ `r`n]+"
    $physicalOutputMarkerPresent = Test-Regex $LogText "(?:physical_output_marker|physicalOutputMarker)=`"?[^`"\r\n,}]+|physicalGI .*outputMarker=(?!unknown)[^ `r`n]+"
    $physicalSceneLinkedPresent = Test-Regex $LogText "(?:physical_scene_linked|physicalSceneLinked|physicalGI sceneLinked)=true"
    $physicalSurfaceContributionPresent = Test-Regex $LogText "(?:physical_surface_contribution|physicalSurfaceContribution|physicalGI .*surfaceContribution)=true"
    $previewFallbackContributionPresent = Test-Regex $LogText "(?:preview_fallback_contribution|previewFallback)=true"
    $overclaimPresent = Test-Regex $LogText "physicalGiTracingQuality=(?!open)|physical GI .*production-quality|physicallyCorrectGi=true|realPhysicalGiTracing=true|realGpuGiTracing=true"
    $present = $physicalSceneLinkedPresent `
        -and $physicalSurfaceContributionPresent `
        -and ($physicalGiSamples -ge 1) `
        -and ($physicalGiHitSamples -ge 1) `
        -and ($surfaceMaterialHitCoupledSamples -ge 1) `
        -and ($geometryHitCoupledSamples -ge 1) `
        -and ($physicalOutputChecksum -ge 1) `
        -and ($physicalGiSampleMarkerPresent -or $physicalSceneMarkerPresent -or $physicalOutputMarkerPresent)

    return [ordered]@{
        present = $present
        physicalGiSamples = $physicalGiSamples
        physicalGiHitSamples = $physicalGiHitSamples
        surfaceMaterialHitCoupledSamples = $surfaceMaterialHitCoupledSamples
        geometryHitCoupledSamples = $geometryHitCoupledSamples
        surfaceMaterialHitCoupling = $surfaceMaterialHitCoupling
        geometryHitCoupling = $geometryHitCoupling
        physicalSceneLinkScore = $physicalSceneLinkScore
        physicalOutputChecksum = $physicalOutputChecksum
        physicalGiSampleMarkerPresent = $physicalGiSampleMarkerPresent
        surfaceMaterialHitMarkerPresent = $surfaceMaterialHitMarkerPresent
        physicalSceneMarkerPresent = $physicalSceneMarkerPresent
        physicalOutputMarkerPresent = $physicalOutputMarkerPresent
        physicalSceneLinkedPresent = $physicalSceneLinkedPresent
        physicalSurfaceContributionPresent = $physicalSurfaceContributionPresent
        previewFallbackContributionPresent = $previewFallbackContributionPresent
        overclaimPresent = $overclaimPresent
    }
}

function Get-Round7TracedGiConsumptionEvidence {
    param([string] $LogText)

    $rayCount = Get-MaxRegexNumber $LogText "(?:tracedLighting(?:Ray|Sample)Count|traced_lighting_(?:ray|sample)_count|traceRayCount|trace_ray_count|ray_count|rays)=(\d+)"
    $hitCount = Get-MaxRegexNumber $LogText "(?:tracedLightingHitCount|traced_lighting_hit_count|traceHitCount|trace_hit_count|hit_count|hits)=(\d+)"
    $materialCoupledHitCount = Get-MaxRegexNumber $LogText "(?:tracedLightingMaterialCoupledHitCount|traced_lighting_material_coupled_hit_count|traceMaterialCoupledHitCount|trace_material_coupled_hit_count|materialCoupledHitCount|material_coupled_hit_count|materialHits)=(\d+)"
    $depthCoupledHitCount = Get-MaxRegexNumber $LogText "(?:tracedLightingDepthCoupledHitCount|traced_lighting_depth_coupled_hit_count|traceDepthCoupledHitCount|trace_depth_coupled_hit_count|depthCoupledHitCount|depth_coupled_hit_count|depthHits)=(\d+)"
    $sourceCoupledBounceCount = Get-MaxRegexNumber $LogText "(?:tracedLightingSourceCoupledBounceCount|traced_lighting_source_coupled_bounce_count|traceSourceCoupledBounceCount|trace_source_coupled_bounce_count|sourceCoupledBounceCount|source_coupled_bounce_count|sourceBounce(?:s)?|source_bounce_count)=(\d+)"
    $consumedPresent = Test-AnyRegex $LogText $TracedGiConsumedPatterns
    $finalCompositeConsumptionPresent = Test-AnyRegex $LogText $TracedGiFinalCompositeConsumptionPatterns
    $sourcePresent = Test-Regex $LogText "(?:tracedLightingSource|consumedLightingSource|lightingConsumptionSource|lighting_source|traceEvidenceSource|trace_evidence_source|finalGiSource|final_gi_source)=[^`r`n]*(?:voxel|ray|rt|trace|traced|hybrid|final_gi|diffuse)"
    $metadataOnlyTracingPresent = Test-AnyRegex $LogText $TracedGiMetadataOnlyPatterns
    $realGpuTraversalExecutedPresent = Test-AnyRegex $LogText $RealGpuTraversalExecutedPatterns
    $realGpuTraversalAllowedEvidencePresent = Test-AnyRegex $LogText $RealGpuTraversalAllowedEvidencePatterns
    $realGpuTraversalOverclaimPresent = $realGpuTraversalExecutedPresent -and -not $realGpuTraversalAllowedEvidencePresent
    $materialDepthSourceCoupled = $materialCoupledHitCount -ge 1 -and $depthCoupledHitCount -ge 1 -and $sourceCoupledBounceCount -ge 1
    $present = $consumedPresent `
        -and $finalCompositeConsumptionPresent `
        -and ($rayCount -ge 1) `
        -and ($hitCount -ge 1) `
        -and $materialDepthSourceCoupled `
        -and $sourcePresent `
        -and -not $metadataOnlyTracingPresent `
        -and -not $realGpuTraversalOverclaimPresent

    return [ordered]@{
        present = $present
        consumedPresent = $consumedPresent
        finalCompositeConsumptionPresent = $finalCompositeConsumptionPresent
        sourcePresent = $sourcePresent
        rayCount = $rayCount
        hitCount = $hitCount
        materialCoupledHitCount = $materialCoupledHitCount
        depthCoupledHitCount = $depthCoupledHitCount
        sourceCoupledBounceCount = $sourceCoupledBounceCount
        materialDepthSourceCoupled = $materialDepthSourceCoupled
        metadataOnlyTracingPresent = $metadataOnlyTracingPresent
        realGpuTraversalExecutedPresent = $realGpuTraversalExecutedPresent
        realGpuTraversalAllowedEvidencePresent = $realGpuTraversalAllowedEvidencePresent
        realGpuTraversalOverclaimPresent = $realGpuTraversalOverclaimPresent
    }
}

function Get-Round7ShaderOutputImageCandidateEvidence {
    param([string] $LogText)

    $nativeMatch = Get-LastRegexMatch $LogText "Lucerna native shader denoise output image candidate: ready=(?<ready>true|false) size=(?<width>[0-9]+)x(?<height>[0-9]+) pixels=(?<pixels>[0-9]+) bytes=(?<bytes>[0-9]+) checksum=(?<checksum>[0-9]+) cpuStaged=(?<cpuStaged>true|false) nonGpu=(?<nonGpu>true|false) realShaderGenerated=(?<shaderGenerated>true|false) realOutput=(?<realOutput>true|false) marker=(?<marker>\S+) blocker=(?<blocker>[^.`r`n]+)"
    $boundaryMatch = Get-LastRegexMatch $LogText "(?:shaderOutputImageCandidateBoundary|round7\.shaderOutputImageCandidate|shaderOutputImageCandidate|shaderOutputCandidate)=`"?(?<summary>present=(?<present>true|false),dims=(?<dims>none|[0-9]+x[0-9]+),checksum=(?<summaryChecksum>none|[0-9]+),source=(?<source>[^,`"`r`n]+),blocker=(?<summaryBlocker>[^,`"`r`n]+),realShaderDenoiseOutputReady=(?<summaryRealOutput>true|false),sourceKind=shader-output-image-candidate)"

    $width = $null
    $height = $null
    if ($nativeMatch) {
        $width = Convert-ToNullableInt64 $nativeMatch.Groups["width"].Value
        $height = Convert-ToNullableInt64 $nativeMatch.Groups["height"].Value
    } elseif ($boundaryMatch -and $boundaryMatch.Groups["dims"].Value -match "^(?<w>[0-9]+)x(?<h>[0-9]+)$") {
        $width = Convert-ToNullableInt64 $Matches["w"]
        $height = Convert-ToNullableInt64 $Matches["h"]
    }

    $ready = if ($nativeMatch) {
        Convert-ToNullableBool $nativeMatch.Groups["ready"].Value
    } elseif ($boundaryMatch) {
        Convert-ToNullableBool $boundaryMatch.Groups["present"].Value
    } else {
        $null
    }
    $realOutput = if ($nativeMatch) {
        Convert-ToNullableBool $nativeMatch.Groups["realOutput"].Value
    } elseif ($boundaryMatch) {
        Convert-ToNullableBool $boundaryMatch.Groups["summaryRealOutput"].Value
    } else {
        $null
    }
    $shaderGenerated = if ($nativeMatch) {
        Convert-ToNullableBool $nativeMatch.Groups["shaderGenerated"].Value
    } else {
        $null
    }

    $checksum = if ($nativeMatch) {
        Convert-ToNullableInt64 $nativeMatch.Groups["checksum"].Value
    } elseif ($boundaryMatch) {
        Convert-ToNullableInt64 $boundaryMatch.Groups["summaryChecksum"].Value
    } else {
        $null
    }

    $candidatePresent = [bool]($nativeMatch -or $boundaryMatch)
    $cpuStaged = if ($nativeMatch) { Convert-ToNullableBool $nativeMatch.Groups["cpuStaged"].Value } else { $null }
    $nonGpu = if ($nativeMatch) { Convert-ToNullableBool $nativeMatch.Groups["nonGpu"].Value } else { $null }

    return [ordered]@{
        present = $candidatePresent
        candidateReady = $ready
        cpuStaged = $cpuStaged
        nonGpu = $nonGpu
        width = $width
        height = $height
        pixels = if ($nativeMatch) { Convert-ToNullableInt64 $nativeMatch.Groups["pixels"].Value } else { $null }
        bytes = if ($nativeMatch) { Convert-ToNullableInt64 $nativeMatch.Groups["bytes"].Value } else { $null }
        checksum = $checksum
        marker = if ($nativeMatch) { $nativeMatch.Groups["marker"].Value } else { $null }
        source = if ($boundaryMatch) { $boundaryMatch.Groups["source"].Value } else { $null }
        blocker = if ($nativeMatch) { $nativeMatch.Groups["blocker"].Value.Trim() } elseif ($boundaryMatch) { $boundaryMatch.Groups["summaryBlocker"].Value } else { $null }
        shaderGeneratedOutput = $shaderGenerated
        realShaderDenoiseOutputReady = $realOutput
        boundaryOnly = $candidatePresent -and -not ([bool]$shaderGenerated -and [bool]$realOutput -and -not [bool]$cpuStaged -and -not [bool]$nonGpu)
        evidenceLabel = "boundary evidence only; shader-output-image candidates do not prove real shader-generated denoise output"
    }
}

function Get-Round7ShaderDenoiseBoundaryEvidence {
    param(
        [string] $LogText,
        [object] $ImageCandidateEvidence,
        [bool] $IntentPresent,
        [bool] $InputReadyPresent,
        [bool] $RawGiCpuReadbackInputPresent,
        [bool] $RawDiffuseGiInputSourcePresent,
        [bool] $DirectLightValidationInputPresent,
        [bool] $DispatchPreparedPresent,
        [bool] $OutputImageReadyPresent,
        [bool] $OutputImageStateExplicitPresent,
        [bool] $OutputMaterialReadyPresent,
        [bool] $OutputMaterialStateExplicitPresent,
        [bool] $ShaderGeneratedOutputTruePresent,
        [bool] $ShaderGeneratedOutputExplicitPresent,
        [bool] $OutputAttemptedMarkerPresent,
        [object] $OutputAttemptGeneration,
        [bool] $CpuReadbackFallbackActivePresent,
        [bool] $CpuReadbackFallbackExplicitPresent,
        [bool] $RealOutputReadyPresent,
        [bool] $RealOutputStateExplicitPresent,
        [bool] $CpuReadbackSourcePresent,
        [bool] $OutputCandidateOnlySourcePresent,
        [bool] $NoOverclaimMarkerPresent,
        [bool] $RealOutputProven
    )

    $sourceIdentityMatch = Get-LastRegexMatch $LogText "(?:sourceIdentity|source_identity)=`"?(?<source>[^,;`" `r`n]+)"
    $sourceAuthenticityMatch = Get-LastRegexMatch $LogText "(?:sourceAuthenticity|source_authenticity)=`"?(?<source>[^,;`" `r`n]+)"
    $blockerMatch = Get-LastRegexMatch $LogText "(?:shaderDenoiseBlockerReason|shader_denoise_blocker_reason|shaderDenoiseBlocker|shader_denoise_blocker)=`"?(?<blocker>[A-Za-z0-9_.-]+)"

    $sourceIdentity = if ($sourceIdentityMatch) { $sourceIdentityMatch.Groups["source"].Value } elseif ($ImageCandidateEvidence.source) { [string]$ImageCandidateEvidence.source } else { $null }
    $sourceAuthenticity = if ($sourceAuthenticityMatch) { $sourceAuthenticityMatch.Groups["source"].Value } else { $null }

    $blockerReason = if ($RealOutputProven) {
        "none"
    } elseif ($blockerMatch) {
        $blockerMatch.Groups["blocker"].Value
    } elseif (-not $IntentPresent) {
        "shader_denoise_intent_missing"
    } elseif (-not $InputReadyPresent) {
        "shader_denoise_input_not_ready"
    } elseif (-not $RawGiCpuReadbackInputPresent) {
        "raw_gi_cpu_readback_input_missing"
    } elseif (-not $RawDiffuseGiInputSourcePresent) {
        "raw_diffuse_gi_input_source_missing"
    } elseif ($DirectLightValidationInputPresent) {
        "direct_light_validation_input_present"
    } elseif (-not $DispatchPreparedPresent) {
        "shader_denoise_dispatch_not_prepared"
    } elseif (-not $OutputImageStateExplicitPresent) {
        "shader_output_image_state_missing"
    } elseif (-not $OutputImageReadyPresent) {
        "shader_output_image_not_ready"
    } elseif (-not $OutputMaterialStateExplicitPresent) {
        "shader_output_material_state_missing"
    } elseif (-not $OutputMaterialReadyPresent) {
        "shader_output_material_not_ready"
    } elseif (-not $ShaderGeneratedOutputExplicitPresent) {
        "shader_generated_output_state_missing"
    } elseif (-not $ShaderGeneratedOutputTruePresent) {
        "shader_generated_output_false"
    } elseif (-not $OutputAttemptedMarkerPresent) {
        "shader_output_attempt_marker_missing"
    } elseif ($null -eq $OutputAttemptGeneration) {
        "shader_output_attempt_generation_missing"
    } elseif (-not $CpuReadbackFallbackExplicitPresent) {
        "cpu_readback_fallback_state_missing"
    } elseif ($CpuReadbackFallbackActivePresent) {
        "cpu_readback_fallback_active"
    } elseif (-not $RealOutputStateExplicitPresent) {
        "real_shader_output_state_missing"
    } elseif (-not $RealOutputReadyPresent) {
        "real_shader_output_not_ready"
    } else {
        "real_shader_output_unproven"
    }

    $sourceKind = if ($RealOutputProven) {
        "real-shader-denoised-output"
    } elseif ($OutputCandidateOnlySourcePresent -or [bool]$ImageCandidateEvidence.boundaryOnly) {
        "shader-output-image-candidate-boundary"
    } elseif ($CpuReadbackFallbackActivePresent -or $CpuReadbackSourcePresent) {
        "cpu-readback-denoised-output"
    } elseif (-not [string]::IsNullOrWhiteSpace($sourceIdentity)) {
        "declared-source-identity"
    } else {
        "unknown"
    }

    return [ordered]@{
        sourceIdentity = $sourceIdentity
        sourceAuthenticity = $sourceAuthenticity
        sourceKind = $sourceKind
        blockerReason = $blockerReason
        outputAttemptedMarkerPresent = $OutputAttemptedMarkerPresent
        outputAttemptGeneration = $OutputAttemptGeneration
        candidateOnlySourcePresent = $OutputCandidateOnlySourcePresent
        noOverclaimMarkerPresent = $NoOverclaimMarkerPresent
        honestNonOverclaim = $RealOutputProven -or ($NoOverclaimMarkerPresent -and -not $RealOutputReadyPresent -and ($CpuReadbackFallbackActivePresent -or $CpuReadbackSourcePresent -or $OutputCandidateOnlySourcePresent -or [bool]$ImageCandidateEvidence.boundaryOnly -or $blockerReason -ne "none"))
        prerequisites = [ordered]@{
            intent = $IntentPresent
            inputReady = $InputReadyPresent
            rawGiCpuReadbackInput = $RawGiCpuReadbackInputPresent
            rawDiffuseGiInputSource = $RawDiffuseGiInputSourcePresent
            directLightValidationInputRejected = -not $DirectLightValidationInputPresent
            dispatchPrepared = $DispatchPreparedPresent
            outputAttempted = $OutputAttemptedMarkerPresent
            outputAttemptGeneration = $OutputAttemptGeneration
            outputImageReady = $OutputImageReadyPresent
            outputImageStateExplicit = $OutputImageStateExplicitPresent
            outputMaterialReady = $OutputMaterialReadyPresent
            outputMaterialStateExplicit = $OutputMaterialStateExplicitPresent
            shaderGeneratedOutput = $ShaderGeneratedOutputTruePresent
            shaderGeneratedOutputStateExplicit = $ShaderGeneratedOutputExplicitPresent
            cpuReadbackFallbackActive = $CpuReadbackFallbackActivePresent
            cpuReadbackFallbackStateExplicit = $CpuReadbackFallbackExplicitPresent
            realOutputReady = $RealOutputReadyPresent
            realOutputStateExplicit = $RealOutputStateExplicitPresent
            noOverclaim = $NoOverclaimMarkerPresent
            candidateOnlySource = $OutputCandidateOnlySourcePresent
        }
    }
}

function Measure-Round7LogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
    $shaderOutputImageCandidateEvidence = Get-Round7ShaderOutputImageCandidateEvidence $log
    $physicalGiEvidence = Get-Round7PhysicalGiEvidence $log
    $tracedGiConsumptionEvidence = Get-Round7TracedGiConsumptionEvidence $log
    $fullRendererProofProfilePresent = Test-AnyRegex $log $FullRendererProofProfilePatterns
    $playablePhysicalRendererProfilePresent = Test-AnyRegex $log $PlayablePhysicalRendererProfilePatterns
    $playablePhysicalRendererBudgetPresent = Test-AnyRegex $log $PlayablePhysicalRendererBudgetPatterns
    $realWorldVisualQualityComparisonProfilePresent = Test-AnyRegex $log $RealWorldVisualQualityComparisonProfilePatterns
    $normalHeavyWorkloadBypassPresent = Test-AnyRegex $log $NormalHeavyWorkloadBypassPatterns
    $softReceiverTiedShadowMaskPresent = Test-AnyRegex $log $SoftReceiverTiedShadowMaskPatterns
    $cleanInWorldCaptureContractPresent = Test-AnyRegex $log $CleanInWorldCaptureContractPatterns
    $menuChatScreenshotContaminationPresent = Test-AnyRegex $log $MenuChatScreenshotContaminationPatterns
    $proofOverlayEvidencePresent = Test-AnyRegex $log $ProofOverlayEvidencePatterns
    $lowResDebugSubstitutionPresent = Test-AnyRegex $log $LowResDebugSubstitutionPatterns
    $trueDepthGBufferSamplingMarkerPresent = Test-AnyRegex $log $TrueDepthGBufferSamplingPatterns
    $depthGBufferSourcePresent = Test-AnyRegex $log $DepthGBufferSourcePatterns
    $depthGBufferSampleCount = Get-MaxRegexNumber $log "(?:depthGBufferSampleCount|depth_gbuffer_sample_count|gBufferSampleCount|gbuffer_sample_count|g_buffer_depth_sample_count|depthSampleCount|depth_samples)=(\d+)"
    $depthGBufferMetadataOnlyPresent = Test-AnyRegex $log $DepthGBufferMetadataOnlyPatterns
    $trueDepthGBufferSamplingProven = $trueDepthGBufferSamplingMarkerPresent -and $depthGBufferSourcePresent -and ($depthGBufferSampleCount -ge 1) -and -not $depthGBufferMetadataOnlyPresent
    $realShadowMapOutputMarkerPresent = Test-AnyRegex $log $RealShadowMapOutputPatterns
    $shadowMapSampleCount = Get-MaxRegexNumber $log "(?:shadowMap(?:Texel|Sample|Receiver|Caster|Output)(?:Count|s)?|shadow_map_(?:texel|sample|receiver|caster|output)_count)=(\d+)"
    $realShadowMapOutputProven = $realShadowMapOutputMarkerPresent -and ($shadowMapSampleCount -ge 1)
    $shadowMapOutputConsumedPresent = Test-AnyRegex $log $ShadowMapConsumptionPatterns
    $fullRendererMilestoneProofPresent = Test-Regex $log "(?:realRendererMilestone1\.proof|realRendererMilestone1Proof|real_renderer_milestone1_proof)=true"
    $fullRendererOverclaimPresent = Test-AnyRegex $log $FullRendererOverclaimPatterns
    $acceptedFinalCompositePresent = Test-Regex $log "sourceIdentity=native-direct-light-rgba8\+native-diffuse-gi-rgba8\+cpu-denoised-diffuse-gi-rgba8.*sourceAuthenticity=accepted:final-composite-direct-plus-raw-gi-plus-(?:cpu-)?denoised-gi.*evidence=round7\.composite\.final\.direct_raw_denoised.*finalBlendComplete=true.*metadataOnly=false"
    $acceptedRound7DenoiseDrawPresent = Test-Regex $log "(?:Lucerna public Mojang final composite: .*round7\.finalCompositeSubmission=state=submitted-with-draw.*metadataOnly=false|public Mojang Round 7 DENOISED_GI visual render pass submitted.*metadataOnly=false|denoised diffuse GI RGBA8 payload is ready for preview draw.*metadataOnly=false)"
    $directSourcePresent = Test-AnyRegex $log $DirectSourcePatterns
    $nativeGiSourcePresent = Test-AnyRegex $log $NativeGiSourcePatterns
    $rawGiSourcePresent = Test-AnyRegex $log $RawGiSourcePatterns
    $denoiseDispatchPresent = Test-AnyRegex $log $DenoiseDispatchPatterns
    $denoisedGiOutputPresent = Test-AnyRegex $log $DenoisedGiOutputPatterns
    $finalCompositePresent = (Test-AnyRegex $log $FinalCompositePatterns) -or $acceptedFinalCompositePresent
    $hudSafeFinalCompositePresent = Test-AnyRegex $log $HudSafeFinalCompositePatterns
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporaryDirectLightSubstitution=true|using the current direct-light RGBA payload as the temporary visible source"
    $metadataOnlyPreviewPresent = (Test-Regex $log "metadata-only|metadata scaffold|signal_separated_denoise_metadata_scaffold_no_render_output|no_render_output") -and -not ($acceptedFinalCompositePresent -or $acceptedRound7DenoiseDrawPresent)
    $firstPracticalCpuOutputPresent = Test-Regex $log "first_practical_cpu_denoised_diffuse_gi_rgba8_generated|denoisedCpuOutputGenerated=true|denoised_cpu_output_generated=true"
    $realDenoiseShaderOutputPresent = Test-Regex $log "(?:^|[\s,;])(?:realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=true(?:[,;]|$)"
    $realDenoiseShaderOutputFalsePresent = Test-Regex $log "realDenoiseShaderOutput=false|real_denoise_shader_output=false"
    $shaderDenoiseIntentPresent = Test-AnyRegex $log $ShaderDenoiseIntentPatterns
    $shaderDenoiseInputReadyPresent = Test-AnyRegex $log $ShaderDenoiseInputReadyPatterns
    $shaderDenoiseRawGiCpuReadbackInputPresent = Test-AnyRegex $log $ShaderDenoiseRawGiCpuReadbackInputPatterns
    $shaderDenoiseRawDiffuseGiInputSourcePresent = Test-AnyRegex $log $ShaderDenoiseRawDiffuseGiInputSourcePatterns
    $shaderDenoiseDirectLightValidationInputPresent = Test-AnyRegex $log $ShaderDenoiseDirectLightValidationInputPatterns
    $shaderDenoiseRawGiInputProven = $shaderDenoiseRawGiCpuReadbackInputPresent -and $shaderDenoiseRawDiffuseGiInputSourcePresent -and -not $shaderDenoiseDirectLightValidationInputPresent
    $shaderDenoiseDispatchPreparedPresent = Test-AnyRegex $log $ShaderDenoiseDispatchPreparedPatterns
    $shaderDenoiseOutputAttemptedPresent = Test-AnyRegex $log $ShaderDenoiseOutputAttemptedPatterns
    $shaderDenoiseOutputAttemptGenerationMatch = Get-LastRegexMatch $log "(?:round7\.shaderDenoise\.outputAttemptGeneration|shaderDenoiseOutputAttemptGeneration|shader_denoise_output_attempt_generation)=(?<generation>[0-9]+)"
    $shaderDenoiseOutputAttemptGeneration = if ($shaderDenoiseOutputAttemptGenerationMatch) { Convert-ToNullableInt64 $shaderDenoiseOutputAttemptGenerationMatch.Groups["generation"].Value } else { $null }
    $shaderDenoiseOutputAttemptGenerationPresent = $null -ne $shaderDenoiseOutputAttemptGeneration
    $shaderDenoiseOutputImageReadyPresent = (Test-AnyRegex $log $ShaderDenoiseOutputImageReadyPatterns) -or ([bool]$shaderOutputImageCandidateEvidence.candidateReady)
    $shaderDenoiseOutputImageNotReadyPresent = (Test-AnyRegex $log $ShaderDenoiseOutputImageNotReadyPatterns) -or ($shaderOutputImageCandidateEvidence.candidateReady -eq $false)
    $shaderDenoiseOutputImageStateExplicitPresent = $shaderDenoiseOutputImageReadyPresent -or $shaderDenoiseOutputImageNotReadyPresent
    $shaderDenoiseOutputMaterialReadyPresent = Test-AnyRegex $log $ShaderDenoiseOutputMaterialReadyPatterns
    $shaderDenoiseOutputMaterialNotReadyPresent = Test-AnyRegex $log $ShaderDenoiseOutputMaterialNotReadyPatterns
    $shaderDenoiseOutputMaterialStateExplicitPresent = $shaderDenoiseOutputMaterialReadyPresent -or $shaderDenoiseOutputMaterialNotReadyPresent
    $shaderDenoiseShaderGeneratedOutputTruePresent = (Test-AnyRegex $log $ShaderDenoiseShaderGeneratedOutputTruePatterns) -or ([bool]$shaderOutputImageCandidateEvidence.shaderGeneratedOutput)
    $shaderDenoiseShaderGeneratedOutputFalsePresent = (Test-AnyRegex $log $ShaderDenoiseShaderGeneratedOutputFalsePatterns) -or ($shaderOutputImageCandidateEvidence.shaderGeneratedOutput -eq $false)
    $shaderDenoiseShaderGeneratedOutputExplicitPresent = $shaderDenoiseShaderGeneratedOutputTruePresent -or $shaderDenoiseShaderGeneratedOutputFalsePresent
    $shaderDenoiseCpuReadbackFallbackActivePresent = Test-AnyRegex $log $ShaderDenoiseCpuReadbackFallbackActivePatterns
    $shaderDenoiseCpuReadbackFallbackInactivePresent = Test-AnyRegex $log $ShaderDenoiseCpuReadbackFallbackInactivePatterns
    $shaderDenoiseCpuReadbackFallbackExplicitPresent = $shaderDenoiseCpuReadbackFallbackActivePresent -or $shaderDenoiseCpuReadbackFallbackInactivePresent
    $realShaderDenoiseOutputReadyPresent = (Test-AnyRegex $log $RealShaderDenoiseOutputReadyPatterns) -or ([bool]$shaderOutputImageCandidateEvidence.realShaderDenoiseOutputReady)
    $realShaderDenoiseOutputNotReadyPresent = (Test-AnyRegex $log $RealShaderDenoiseOutputNotReadyPatterns) -or $realDenoiseShaderOutputFalsePresent
    if ($shaderOutputImageCandidateEvidence.realShaderDenoiseOutputReady -eq $false) {
        $realShaderDenoiseOutputNotReadyPresent = $true
    }
    $realShaderDenoiseOutputStateExplicitPresent = $realShaderDenoiseOutputReadyPresent -or $realShaderDenoiseOutputNotReadyPresent
    $cpuReadbackDenoiseSourcePresent = Test-AnyRegex $log $CpuReadbackDenoiseSourcePatterns
    $shaderDenoiseOutputCandidateOnlySourcePresent = (Test-AnyRegex $log $ShaderDenoiseOutputCandidateOnlySourcePatterns) -or ([bool]$shaderOutputImageCandidateEvidence.boundaryOnly)
    $shaderGeneratedDenoiseOutputEvidenceReadyPresent = Test-AnyRegex $log $ShaderGeneratedDenoiseOutputEvidenceReadyPatterns
    $shaderDenoiseOutputConsumedPresent = Test-AnyRegex $log $ShaderDenoiseOutputConsumedPatterns
    $shaderDenoiseFinalCompositeConsumablePresent = Test-AnyRegex $log $ShaderDenoiseFinalCompositeConsumablePatterns
    $shaderDenoiseNoOverclaimPresent = Test-AnyRegex $log $ShaderDenoiseNoOverclaimPatterns
    $shaderDenoiseOutputReadyPresent = $realShaderDenoiseOutputReadyPresent
    $shaderDenoiseSourceClaimPresent = Test-AnyRegex $log $ShaderDenoiseSourceClaimPatterns
    $shaderDenoiseOutputOpenPresent = (Test-AnyRegex $log $ShaderDenoiseOutputOpenPatterns) -or $realShaderDenoiseOutputNotReadyPresent -or $shaderDenoiseOutputImageNotReadyPresent -or $shaderDenoiseOutputMaterialNotReadyPresent -or $shaderDenoiseShaderGeneratedOutputFalsePresent
    $shaderDenoiseOutputStateExplicitPresent = $realShaderDenoiseOutputStateExplicitPresent
    $realShaderDenoiseOutputProven = $shaderDenoiseRawGiInputProven -and $shaderDenoiseDispatchPreparedPresent -and $shaderDenoiseOutputImageReadyPresent -and $shaderDenoiseOutputMaterialReadyPresent -and $shaderDenoiseShaderGeneratedOutputTruePresent -and $shaderGeneratedDenoiseOutputEvidenceReadyPresent -and $realShaderDenoiseOutputReadyPresent -and -not $shaderDenoiseCpuReadbackFallbackActivePresent
    $shaderGeneratedDenoiseOutputImageSliceProven = $realShaderDenoiseOutputProven -and $shaderDenoiseOutputConsumedPresent -and $shaderDenoiseFinalCompositeConsumablePresent
    $shaderDenoiseBoundaryEvidence = Get-Round7ShaderDenoiseBoundaryEvidence `
        -LogText $log `
        -ImageCandidateEvidence $shaderOutputImageCandidateEvidence `
        -IntentPresent $shaderDenoiseIntentPresent `
        -InputReadyPresent $shaderDenoiseInputReadyPresent `
        -RawGiCpuReadbackInputPresent $shaderDenoiseRawGiCpuReadbackInputPresent `
        -RawDiffuseGiInputSourcePresent $shaderDenoiseRawDiffuseGiInputSourcePresent `
        -DirectLightValidationInputPresent $shaderDenoiseDirectLightValidationInputPresent `
        -DispatchPreparedPresent $shaderDenoiseDispatchPreparedPresent `
        -OutputImageReadyPresent $shaderDenoiseOutputImageReadyPresent `
        -OutputImageStateExplicitPresent $shaderDenoiseOutputImageStateExplicitPresent `
        -OutputMaterialReadyPresent $shaderDenoiseOutputMaterialReadyPresent `
        -OutputMaterialStateExplicitPresent $shaderDenoiseOutputMaterialStateExplicitPresent `
        -ShaderGeneratedOutputTruePresent $shaderDenoiseShaderGeneratedOutputTruePresent `
        -ShaderGeneratedOutputExplicitPresent $shaderDenoiseShaderGeneratedOutputExplicitPresent `
        -OutputAttemptedMarkerPresent $shaderDenoiseOutputAttemptedPresent `
        -OutputAttemptGeneration $shaderDenoiseOutputAttemptGeneration `
        -CpuReadbackFallbackActivePresent $shaderDenoiseCpuReadbackFallbackActivePresent `
        -CpuReadbackFallbackExplicitPresent $shaderDenoiseCpuReadbackFallbackExplicitPresent `
        -RealOutputReadyPresent $realShaderDenoiseOutputReadyPresent `
        -RealOutputStateExplicitPresent $realShaderDenoiseOutputStateExplicitPresent `
        -CpuReadbackSourcePresent $cpuReadbackDenoiseSourcePresent `
        -OutputCandidateOnlySourcePresent $shaderDenoiseOutputCandidateOnlySourcePresent `
        -NoOverclaimMarkerPresent $shaderDenoiseNoOverclaimPresent `
        -RealOutputProven $realShaderDenoiseOutputProven
    $shaderDenoiseOpenBoundaryPresent = $shaderDenoiseOutputOpenPresent -or $shaderDenoiseCpuReadbackFallbackActivePresent -or $cpuReadbackDenoiseSourcePresent -or ([bool]$shaderOutputImageCandidateEvidence.boundaryOnly)
    $shaderDenoiseOverclaimPresent = (Test-AnyRegex $log $ShaderDenoiseOverclaimPatterns) -or ($shaderDenoiseSourceClaimPresent -and -not $realShaderDenoiseOutputProven) -or ($realShaderDenoiseOutputReadyPresent -and ($shaderDenoiseCpuReadbackFallbackActivePresent -or -not $shaderDenoiseShaderGeneratedOutputTruePresent -or -not $shaderGeneratedDenoiseOutputEvidenceReadyPresent -or -not $shaderDenoiseOutputImageReadyPresent -or -not $shaderDenoiseOutputMaterialReadyPresent))
    $physicalGiOverclaimPresent = [bool]$physicalGiEvidence.overclaimPresent
    $proofMarkerPresent = Test-Regex $log "proofMarkerSource=true|cpuOutputProofMarker=true|round6-gi-proof|round7-proof-marker|R6 GI proof|R7 proof|CPU output proof"
    $submittedFocusWindowOnlyPresent = Test-Regex $log "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|mode=final-composite-direct-light-focus-window-additive"
    $submittedRound7GiSourcePresent = Test-Regex $log "sourceIdentity=native-denoised-diffuse-gi-rgba8,focusWindowOnly=false,round7GiSource=true|mode=round7-final-composite"
    $focusWindowOnlyPresent = $submittedFocusWindowOnlyPresent -and -not $submittedRound7GiSourcePresent
    $nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"

    return [ordered]@{
        markers = [ordered]@{
            rawGiSourcePresent = $rawGiSourcePresent
            directSourcePresent = $directSourcePresent
            nativeGiSourcePresent = $nativeGiSourcePresent
            denoiseDispatchPresent = $denoiseDispatchPresent
            denoisedGiOutputPresent = $denoisedGiOutputPresent
            finalCompositePresent = $finalCompositePresent
            acceptedFinalCompositePresent = $acceptedFinalCompositePresent
            hudSafeFinalCompositePresent = $hudSafeFinalCompositePresent
            temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
            metadataOnlyPreviewPresent = $metadataOnlyPreviewPresent
            firstPracticalCpuOutputPresent = $firstPracticalCpuOutputPresent
            realDenoiseShaderOutputPresent = $realDenoiseShaderOutputPresent
            realDenoiseShaderOutputFalsePresent = $realDenoiseShaderOutputFalsePresent
            shaderDenoiseIntentPresent = $shaderDenoiseIntentPresent
            shaderDenoiseInputReadyPresent = $shaderDenoiseInputReadyPresent
            shaderDenoiseRawGiCpuReadbackInputPresent = $shaderDenoiseRawGiCpuReadbackInputPresent
            shaderDenoiseRawDiffuseGiInputSourcePresent = $shaderDenoiseRawDiffuseGiInputSourcePresent
            shaderDenoiseDirectLightValidationInputPresent = $shaderDenoiseDirectLightValidationInputPresent
            shaderDenoiseRawGiInputProven = $shaderDenoiseRawGiInputProven
            shaderDenoiseDispatchPreparedPresent = $shaderDenoiseDispatchPreparedPresent
            shaderDenoiseOutputAttemptedPresent = $shaderDenoiseOutputAttemptedPresent
            shaderDenoiseOutputAttemptGenerationPresent = $shaderDenoiseOutputAttemptGenerationPresent
            shaderDenoiseOutputAttemptGeneration = $shaderDenoiseOutputAttemptGeneration
            shaderDenoiseOutputImageReadyPresent = $shaderDenoiseOutputImageReadyPresent
            shaderDenoiseOutputImageNotReadyPresent = $shaderDenoiseOutputImageNotReadyPresent
            shaderDenoiseOutputImageStateExplicitPresent = $shaderDenoiseOutputImageStateExplicitPresent
            shaderDenoiseOutputImageCandidatePresent = [bool]$shaderOutputImageCandidateEvidence.present
            shaderDenoiseOutputImageCandidateReadyPresent = [bool]$shaderOutputImageCandidateEvidence.candidateReady
            shaderDenoiseOutputImageCandidateCpuStagedPresent = [bool]$shaderOutputImageCandidateEvidence.cpuStaged
            shaderDenoiseOutputImageCandidateNonGpuPresent = [bool]$shaderOutputImageCandidateEvidence.nonGpu
            shaderDenoiseOutputImageCandidateBoundaryOnly = [bool]$shaderOutputImageCandidateEvidence.boundaryOnly
            shaderDenoiseOutputMaterialReadyPresent = $shaderDenoiseOutputMaterialReadyPresent
            shaderDenoiseOutputMaterialNotReadyPresent = $shaderDenoiseOutputMaterialNotReadyPresent
            shaderDenoiseOutputMaterialStateExplicitPresent = $shaderDenoiseOutputMaterialStateExplicitPresent
            shaderDenoiseShaderGeneratedOutputTruePresent = $shaderDenoiseShaderGeneratedOutputTruePresent
            shaderDenoiseShaderGeneratedOutputFalsePresent = $shaderDenoiseShaderGeneratedOutputFalsePresent
            shaderDenoiseShaderGeneratedOutputExplicitPresent = $shaderDenoiseShaderGeneratedOutputExplicitPresent
            shaderGeneratedDenoiseOutputEvidenceReadyPresent = $shaderGeneratedDenoiseOutputEvidenceReadyPresent
            shaderDenoiseCpuReadbackFallbackActivePresent = $shaderDenoiseCpuReadbackFallbackActivePresent
            shaderDenoiseCpuReadbackFallbackInactivePresent = $shaderDenoiseCpuReadbackFallbackInactivePresent
            shaderDenoiseCpuReadbackFallbackExplicitPresent = $shaderDenoiseCpuReadbackFallbackExplicitPresent
            realShaderDenoiseOutputReadyPresent = $realShaderDenoiseOutputReadyPresent
            realShaderDenoiseOutputNotReadyPresent = $realShaderDenoiseOutputNotReadyPresent
            realShaderDenoiseOutputStateExplicitPresent = $realShaderDenoiseOutputStateExplicitPresent
            realShaderDenoiseOutputProven = $realShaderDenoiseOutputProven
            shaderDenoiseOutputConsumedPresent = $shaderDenoiseOutputConsumedPresent
            shaderDenoiseFinalCompositeConsumablePresent = $shaderDenoiseFinalCompositeConsumablePresent
            shaderGeneratedDenoiseOutputImageSliceProven = $shaderGeneratedDenoiseOutputImageSliceProven
            shaderDenoiseOpenBoundaryPresent = $shaderDenoiseOpenBoundaryPresent
            cpuReadbackDenoiseSourcePresent = $cpuReadbackDenoiseSourcePresent
            shaderDenoiseOutputCandidateOnlySourcePresent = $shaderDenoiseOutputCandidateOnlySourcePresent
            shaderDenoiseNoOverclaimPresent = $shaderDenoiseNoOverclaimPresent
            shaderDenoiseOutputReadyPresent = $shaderDenoiseOutputReadyPresent
            shaderDenoiseSourceClaimPresent = $shaderDenoiseSourceClaimPresent
            shaderDenoiseOutputOpenPresent = $shaderDenoiseOutputOpenPresent
            shaderDenoiseOutputStateExplicitPresent = $shaderDenoiseOutputStateExplicitPresent
            shaderDenoiseOverclaimPresent = $shaderDenoiseOverclaimPresent
            shaderDenoiseHonestNonOverclaimPresent = [bool]$shaderDenoiseBoundaryEvidence.honestNonOverclaim -and -not $shaderDenoiseOverclaimPresent
            physicalGiEvidencePresent = [bool]$physicalGiEvidence.present
            physicalGiOverclaimPresent = $physicalGiOverclaimPresent
            fullRendererProofProfilePresent = $fullRendererProofProfilePresent
            playablePhysicalRendererProfilePresent = $playablePhysicalRendererProfilePresent
            playablePhysicalRendererBudgetPresent = $playablePhysicalRendererBudgetPresent
            realWorldVisualQualityComparisonProfilePresent = $realWorldVisualQualityComparisonProfilePresent
            normalHeavyWorkloadBypassPresent = $normalHeavyWorkloadBypassPresent
            softReceiverTiedShadowMaskPresent = $softReceiverTiedShadowMaskPresent
            cleanInWorldCaptureContractPresent = $cleanInWorldCaptureContractPresent
            menuChatScreenshotContaminationPresent = $menuChatScreenshotContaminationPresent
            proofOverlayEvidencePresent = $proofOverlayEvidencePresent
            lowResDebugSubstitutionPresent = $lowResDebugSubstitutionPresent
            trueDepthGBufferSamplingMarkerPresent = $trueDepthGBufferSamplingMarkerPresent
            depthGBufferSourcePresent = $depthGBufferSourcePresent
            depthGBufferSampleCount = $depthGBufferSampleCount
            depthGBufferMetadataOnlyPresent = $depthGBufferMetadataOnlyPresent
            trueDepthGBufferSamplingProven = $trueDepthGBufferSamplingProven
            realShadowMapOutputMarkerPresent = $realShadowMapOutputMarkerPresent
            shadowMapSampleCount = $shadowMapSampleCount
            realShadowMapOutputProven = $realShadowMapOutputProven
            shadowMapOutputConsumedPresent = $shadowMapOutputConsumedPresent
            fullRendererMilestoneProofPresent = $fullRendererMilestoneProofPresent
            fullRendererOverclaimPresent = $fullRendererOverclaimPresent
            tracedGiConsumptionPresent = [bool]$tracedGiConsumptionEvidence.present
            tracedGiConsumedPresent = [bool]$tracedGiConsumptionEvidence.consumedPresent
            tracedGiFinalCompositeConsumptionPresent = [bool]$tracedGiConsumptionEvidence.finalCompositeConsumptionPresent
            tracedGiSourcePresent = [bool]$tracedGiConsumptionEvidence.sourcePresent
            tracedGiTraceCountersPresent = [bool]($tracedGiConsumptionEvidence.rayCount -ge 1 -and $tracedGiConsumptionEvidence.hitCount -ge 1)
            tracedGiMaterialDepthSourceCoupled = [bool]$tracedGiConsumptionEvidence.materialDepthSourceCoupled
            metadataOnlyTracingPresent = [bool]$tracedGiConsumptionEvidence.metadataOnlyTracingPresent
            realGpuTraversalExecutedPresent = [bool]$tracedGiConsumptionEvidence.realGpuTraversalExecutedPresent
            realGpuTraversalAllowedEvidencePresent = [bool]$tracedGiConsumptionEvidence.realGpuTraversalAllowedEvidencePresent
            realGpuTraversalOverclaimPresent = [bool]$tracedGiConsumptionEvidence.realGpuTraversalOverclaimPresent
            proofMarkerPresent = $proofMarkerPresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            submittedFocusWindowOnlyPresent = $submittedFocusWindowOnlyPresent
            submittedRound7GiSourcePresent = $submittedRound7GiSourcePresent
            nativeErrorPresent = $nativeErrorPresent
        }
        shaderOutputImageCandidate = $shaderOutputImageCandidateEvidence
        shaderDenoiseBoundary = $shaderDenoiseBoundaryEvidence
        physicalGiEvidence = $physicalGiEvidence
        tracedGiConsumptionEvidence = $tracedGiConsumptionEvidence
        patterns = [ordered]@{
            rawGiSourcePatterns = @($RawGiSourcePatterns)
            directSourcePatterns = @($DirectSourcePatterns)
            nativeGiSourcePatterns = @($NativeGiSourcePatterns)
            denoiseDispatchPatterns = @($DenoiseDispatchPatterns)
            denoisedGiOutputPatterns = @($DenoisedGiOutputPatterns)
            finalCompositePatterns = @($FinalCompositePatterns)
            hudSafeFinalCompositePatterns = @($HudSafeFinalCompositePatterns)
            shaderDenoiseIntentPatterns = @($ShaderDenoiseIntentPatterns)
            shaderDenoiseInputReadyPatterns = @($ShaderDenoiseInputReadyPatterns)
            shaderDenoiseRawGiCpuReadbackInputPatterns = @($ShaderDenoiseRawGiCpuReadbackInputPatterns)
            shaderDenoiseRawDiffuseGiInputSourcePatterns = @($ShaderDenoiseRawDiffuseGiInputSourcePatterns)
            shaderDenoiseDirectLightValidationInputPatterns = @($ShaderDenoiseDirectLightValidationInputPatterns)
            shaderDenoiseDispatchPreparedPatterns = @($ShaderDenoiseDispatchPreparedPatterns)
            shaderDenoiseOutputAttemptedPatterns = @($ShaderDenoiseOutputAttemptedPatterns)
            shaderDenoiseOutputAttemptGenerationPatterns = @($ShaderDenoiseOutputAttemptGenerationPatterns)
            shaderDenoiseOutputImageReadyPatterns = @($ShaderDenoiseOutputImageReadyPatterns)
            shaderDenoiseOutputImageNotReadyPatterns = @($ShaderDenoiseOutputImageNotReadyPatterns)
            shaderDenoiseOutputMaterialReadyPatterns = @($ShaderDenoiseOutputMaterialReadyPatterns)
            shaderDenoiseOutputMaterialNotReadyPatterns = @($ShaderDenoiseOutputMaterialNotReadyPatterns)
            shaderDenoiseShaderGeneratedOutputTruePatterns = @($ShaderDenoiseShaderGeneratedOutputTruePatterns)
            shaderDenoiseShaderGeneratedOutputFalsePatterns = @($ShaderDenoiseShaderGeneratedOutputFalsePatterns)
            shaderDenoiseCpuReadbackFallbackActivePatterns = @($ShaderDenoiseCpuReadbackFallbackActivePatterns)
            shaderDenoiseCpuReadbackFallbackInactivePatterns = @($ShaderDenoiseCpuReadbackFallbackInactivePatterns)
            realShaderDenoiseOutputReadyPatterns = @($RealShaderDenoiseOutputReadyPatterns)
            realShaderDenoiseOutputNotReadyPatterns = @($RealShaderDenoiseOutputNotReadyPatterns)
            shaderGeneratedDenoiseOutputEvidenceReadyPatterns = @($ShaderGeneratedDenoiseOutputEvidenceReadyPatterns)
            shaderDenoiseNoOverclaimPatterns = @($ShaderDenoiseNoOverclaimPatterns)
            shaderDenoiseOutputConsumedPatterns = @($ShaderDenoiseOutputConsumedPatterns)
            shaderDenoiseFinalCompositeConsumablePatterns = @($ShaderDenoiseFinalCompositeConsumablePatterns)
            tracedGiConsumedPatterns = @($TracedGiConsumedPatterns)
            tracedGiFinalCompositeConsumptionPatterns = @($TracedGiFinalCompositeConsumptionPatterns)
            tracedGiMetadataOnlyPatterns = @($TracedGiMetadataOnlyPatterns)
            realGpuTraversalExecutedPatterns = @($RealGpuTraversalExecutedPatterns)
            realGpuTraversalAllowedEvidencePatterns = @($RealGpuTraversalAllowedEvidencePatterns)
            shaderDenoiseOutputCandidateOnlySourcePatterns = @($ShaderDenoiseOutputCandidateOnlySourcePatterns)
            cpuReadbackDenoiseSourcePatterns = @($CpuReadbackDenoiseSourcePatterns)
            shaderDenoiseOutputReadyPatterns = @($ShaderDenoiseOutputReadyPatterns)
            shaderDenoiseSourceClaimPatterns = @($ShaderDenoiseSourceClaimPatterns)
            shaderDenoiseOutputOpenPatterns = @($ShaderDenoiseOutputOpenPatterns)
            shaderDenoiseOverclaimPatterns = @($ShaderDenoiseOverclaimPatterns)
        }
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$directResolved = Resolve-OptionalFile $DirectImagePath "Direct/emissive image"
$rawResolved = Resolve-ExistingFile $RawGiImagePath "Raw GI image"
$denoisedResolved = Resolve-ExistingFile $DenoisedGiImagePath "Denoised GI image"
$finalResolved = Resolve-ExistingFile $FinalCompositeImagePath "Final composite image"
$debugResolved = Resolve-OptionalFile $DebugImagePath "Debug image"
$logResolved = Resolve-OptionalFile $LogPath "Log"

$baselineDimensions = Get-ImageDimensions $baselineResolved
$directDimensions = if ([string]::IsNullOrWhiteSpace($directResolved)) { $null } else { Get-ImageDimensions $directResolved }
$rawDimensions = Get-ImageDimensions $rawResolved
$denoisedDimensions = Get-ImageDimensions $denoisedResolved
$finalDimensions = Get-ImageDimensions $finalResolved
$debugDimensions = if ([string]::IsNullOrWhiteSpace($debugResolved)) { $null } else { Get-ImageDimensions $debugResolved }

$directDelta = if ([string]::IsNullOrWhiteSpace($directResolved)) { $null } else { Invoke-DeltaHelper $baselineResolved $directResolved "direct" }
$rawDelta = Invoke-DeltaHelper $baselineResolved $rawResolved "raw"
$denoiseDelta = Invoke-DeltaHelper $rawResolved $denoisedResolved "denoised" $DenoiseAutoRegionSearchHeightPercent
$finalDelta = Invoke-DeltaHelper $baselineResolved $finalResolved "final"
$rawRoughnessInDenoiseRegion = Measure-ImageRoughness $rawResolved $denoiseDelta.focusRegion
$denoisedRoughnessInDenoiseRegion = Measure-ImageRoughness $denoisedResolved $denoiseDelta.focusRegion
$denoiseQuality = Compare-Roughness $rawRoughnessInDenoiseRegion $denoisedRoughnessInDenoiseRegion

$logProof = if ([string]::IsNullOrWhiteSpace($logResolved)) { $null } else { Measure-Round7LogProof $logResolved }
$fullRendererMilestoneProofRequired = [bool]$RequireFullRendererMilestoneProof
$playablePhysicalRendererMilestoneProofRequired = [bool]$RequirePlayablePhysicalRendererMilestoneProof
$realWorldVisualQualityComparisonProofRequired = [bool]$RequireRealWorldVisualQualityComparisonProof
$physicalRendererMilestoneProofRequired = [bool]($fullRendererMilestoneProofRequired -or $playablePhysicalRendererMilestoneProofRequired -or $realWorldVisualQualityComparisonProofRequired)
$physicalGiEvidenceRequired = [bool]($RequirePhysicalGiEvidence -or $physicalRendererMilestoneProofRequired)
$tracedGiConsumptionRequired = [bool]($RequireTracedGiConsumption -or $physicalRendererMilestoneProofRequired)
$shaderDenoiseEvidenceRequired = [bool]($RequireShaderDenoiseEvidence -or $RequireShaderGeneratedDenoiseOutput -or $tracedGiConsumptionRequired -or $physicalRendererMilestoneProofRequired)
$shaderGeneratedOutputRequiredForProof = [bool]($RequireShaderGeneratedDenoiseOutput -or $tracedGiConsumptionRequired -or $physicalRendererMilestoneProofRequired)
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
    @{ label = "direct/emissive"; dimensions = $directDimensions },
    @{ label = "raw GI"; dimensions = $rawDimensions },
    @{ label = "denoised GI"; dimensions = $denoisedDimensions },
    @{ label = "final composite"; dimensions = $finalDimensions },
    @{ label = "debug"; dimensions = $debugDimensions }
)) {
    if ($null -eq $entry.dimensions) {
        continue
    }
    if (($entry.dimensions.width -ne $baselineDimensions.width) -or ($entry.dimensions.height -ne $baselineDimensions.height)) {
        $failures.Add("$($entry.label) image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ($RequireDirectImage -and [string]::IsNullOrWhiteSpace($directResolved)) {
    $failures.Add("Direct/emissive screenshot was required but no -DirectImagePath was provided.")
}
if ($directDelta) {
    if ([double]$directDelta.focusRegionMetrics.changedPixelPercent -lt $MinDirectChangedPixelPercent) {
        $failures.Add("Direct/emissive focused-region changed pixels below threshold. actual=$($directDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinDirectChangedPixelPercent")
    }
    if ([double]$directDelta.focusRegionMetrics.meanAbsLuma -lt $MinDirectMeanAbsLuma) {
        $failures.Add("Direct/emissive focused-region mean absolute luma below threshold. actual=$($directDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinDirectMeanAbsLuma")
    }
}
if ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -lt $MinRawChangedPixelPercent) {
    $failures.Add("Raw GI focused-region changed pixels below threshold. actual=$($rawDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinRawChangedPixelPercent")
}
if ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -lt $MinDenoiseChangedPixelPercent) {
    $failures.Add("Denoised GI focused-region changed pixels below threshold when compared with raw GI. actual=$($denoiseDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinDenoiseChangedPixelPercent")
}
if ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -lt $MinDenoiseMeanAbsLuma) {
    $failures.Add("Denoised GI focused-region mean absolute luma below threshold when compared with raw GI. actual=$($denoiseDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinDenoiseMeanAbsLuma")
}
if ([double]$denoiseQuality.meanAbsNeighborLumaReductionPercent -lt $MinDenoiseRoughnessReductionPercent) {
    $failures.Add("Denoised GI roughness did not improve enough over raw GI in the denoise focus region. actualReductionPercent=$($denoiseQuality.meanAbsNeighborLumaReductionPercent) expected>=$MinDenoiseRoughnessReductionPercent rawMeanAbsNeighborLuma=$($denoiseQuality.raw.meanAbsNeighborLuma) denoisedMeanAbsNeighborLuma=$($denoiseQuality.denoised.meanAbsNeighborLuma)")
}
if ([double]$denoiseQuality.maxEdgeLumaPreservationPercent -lt $MinDenoiseEdgePreservationPercent) {
    $failures.Add("Denoised GI edge/detail preservation below threshold. actualPreservationPercent=$($denoiseQuality.maxEdgeLumaPreservationPercent) expected>=$MinDenoiseEdgePreservationPercent rawMaxAbsNeighborLuma=$($denoiseQuality.raw.maxAbsNeighborLuma) denoisedMaxAbsNeighborLuma=$($denoiseQuality.denoised.maxAbsNeighborLuma)")
}
if ([double]$finalDelta.focusRegionMetrics.changedPixelPercent -lt $MinFinalChangedPixelPercent) {
    $failures.Add("Final composite focused-region changed pixels below threshold. actual=$($finalDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinFinalChangedPixelPercent")
}
if ([double]$finalDelta.focusRegionMetrics.meanAbsLuma -lt $MinFinalMeanAbsLuma) {
    $failures.Add("Final composite focused-region mean absolute luma below threshold. actual=$($finalDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinFinalMeanAbsLuma")
}
if ($RequireDebugScreenshot -and [string]::IsNullOrWhiteSpace($debugResolved)) {
    $failures.Add("Debug screenshot was required but no -DebugImagePath was provided.")
}
if ($RequireLogProof -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($shaderDenoiseEvidenceRequired -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Shader-denoise evidence requires -LogPath so intent, input readiness, source identity, and output readiness can be checked.")
}
if ($tracedGiConsumptionRequired -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Traced GI consumption proof requires -LogPath so traced consumption and coupling counters can be checked.")
}
if ($physicalRendererMilestoneProofRequired -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Physical renderer milestone proof requires -LogPath so physical GI, depth/G-buffer, shadow-map, traced lighting, shader-denoise, playable-budget, and contamination rejection markers can be checked.")
}
if ($logProof) {
    if (-not $logProof.markers.directSourcePresent) {
        $failures.Add("Missing native direct/emissive source/output log marker.")
    }
    if (-not $logProof.markers.nativeGiSourcePresent) {
        $failures.Add("Missing native GI source/output log marker.")
    }
    if (-not $logProof.markers.rawGiSourcePresent) {
        $failures.Add("Missing Round 7 raw GI source/output log marker.")
    }
    if (-not $logProof.markers.denoiseDispatchPresent) {
        $failures.Add("Missing Round 7 denoise dispatch log marker.")
    }
    if (-not $logProof.markers.denoisedGiOutputPresent) {
        $failures.Add("Missing Round 7 denoised GI output log marker.")
    }
    if (-not $logProof.markers.finalCompositePresent) {
        $failures.Add("Missing Round 7 final composite log marker.")
    }
    if (-not $logProof.markers.hudSafeFinalCompositePresent) {
        $failures.Add("Missing Round 7 HUD-safe final composite log marker.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; Round 7 proof must use raw GI/denoised/final paths.")
    }
    if ($logProof.markers.metadataOnlyPreviewPresent) {
        $failures.Add("Log contains metadata-only/scaffold marker; Round 7 proof must use real direct, raw GI, denoised GI, and final composite outputs.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker evidence; Round 7 proof must use requested debug/composite modes.")
    }
    if ($logProof.markers.focusWindowOnlyPresent) {
        $failures.Add("Log contains focus-window marker; Round 7 proof must not rely on focus-window-only brightness.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
    if ($logProof.markers.physicalGiOverclaimPresent) {
        $failures.Add("Log overclaims physical GI/tracing quality; Round 7 proof must preserve the open physicalGiTracingQuality boundary.")
    }
    if ($physicalGiEvidenceRequired -and -not $logProof.markers.physicalGiEvidencePresent) {
        $failures.Add("Missing native physical GI sample/coupling evidence markers for Round 7 raw/final GI source.")
    }
    if ($shaderDenoiseEvidenceRequired) {
        if (-not $logProof.markers.shaderDenoiseIntentPresent) {
            $failures.Add("Missing shader-denoise intent marker.")
        }
        if (-not $logProof.markers.shaderDenoiseInputReadyPresent) {
            $failures.Add("Missing shader-denoise input readiness marker.")
        }
        if ($shaderGeneratedOutputRequiredForProof -and -not $logProof.markers.shaderDenoiseRawGiCpuReadbackInputPresent) {
            $failures.Add("Shader-generated denoise output proof requires rawGiCpuReadbackInput=true.")
        }
        if ($shaderGeneratedOutputRequiredForProof -and -not $logProof.markers.shaderDenoiseRawDiffuseGiInputSourcePresent) {
            $failures.Add("Shader-generated denoise output proof requires shaderDenoiseInputKind=raw-diffuse-gi-rgba8 or equivalent raw diffuse-GI source evidence.")
        }
        if ($shaderGeneratedOutputRequiredForProof -and $logProof.markers.shaderDenoiseDirectLightValidationInputPresent) {
            $failures.Add("Shader-generated denoise output proof rejects directLightValidationInput=true/native-direct-light-rgba8-validation-input; raw diffuse-GI input is required.")
        }
        if (-not $logProof.markers.shaderDenoiseDispatchPreparedPresent) {
            $failures.Add("Missing explicit shader-denoise dispatch prepared marker.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputAttemptedPresent) {
            $failures.Add("Missing explicit shader-denoise output attempted true/false marker.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputAttemptGenerationPresent) {
            $failures.Add("Missing explicit shader-denoise output attempt generation marker.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputImageStateExplicitPresent) {
            $failures.Add("Missing explicit shader-denoise output image readiness marker.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputMaterialStateExplicitPresent) {
            $failures.Add("Missing explicit shader-denoise output material readiness marker.")
        }
        if (-not $logProof.markers.shaderDenoiseShaderGeneratedOutputExplicitPresent) {
            $failures.Add("Missing explicit shader-generated denoise output true/false marker.")
        }
        if (-not $logProof.markers.shaderDenoiseCpuReadbackFallbackExplicitPresent) {
            $failures.Add("Missing explicit CPU/readback fallback active true/false marker.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputStateExplicitPresent) {
            $failures.Add("Missing explicit real shader-denoise output readiness marker.")
        }
        if (-not $shaderGeneratedOutputRequiredForProof -and -not $logProof.markers.realShaderDenoiseOutputNotReadyPresent) {
            $failures.Add("Missing explicit realShaderDenoiseOutputReady=false marker for current shader-output attempt/no-overclaim proof.")
        }
        if (-not $logProof.markers.shaderDenoiseNoOverclaimPresent) {
            $failures.Add("Missing explicit shader_denoise_no_overclaim=true marker.")
        }
        if ($logProof.markers.shaderDenoiseOverclaimPresent) {
            $failures.Add("Log over-claims real shader-denoise output without the full explicit marker set: dispatch prepared, output image ready, output material ready, shader-generated output true, CPU/readback fallback inactive, and real output ready.")
        }
        if ([string]::IsNullOrWhiteSpace([string]$logProof.shaderDenoiseBoundary.sourceKind) -or [string]$logProof.shaderDenoiseBoundary.sourceKind -eq "unknown") {
            $failures.Add("Missing shader-denoise source identity/source kind marker for boundary evidence.")
        }
        if ([string]::IsNullOrWhiteSpace([string]$logProof.shaderDenoiseBoundary.blockerReason)) {
            $failures.Add("Missing shader-denoise blocker reason for non-proven shader output boundary evidence.")
        }
        if (-not $logProof.markers.shaderDenoiseHonestNonOverclaimPresent) {
            $failures.Add("Missing honest shader-denoise non-overclaim boundary marker; proof must either prove real shader output or explicitly report CPU fallback/candidate/open blocker state.")
        }
        if ($shaderGeneratedOutputRequiredForProof) {
            if (-not $logProof.markers.shaderDenoiseOutputImageReadyPresent) {
                $failures.Add("Shader-generated denoise output proof requires shader-denoise output image ready=true.")
            }
            if (-not $logProof.markers.shaderDenoiseOutputMaterialReadyPresent) {
                $failures.Add("Shader-generated denoise output proof requires shader-denoise output material ready=true.")
            }
            if (-not $logProof.markers.shaderDenoiseShaderGeneratedOutputTruePresent) {
                $failures.Add("Shader-generated denoise output proof requires shaderGeneratedOutput=true.")
            }
            if (-not $logProof.markers.shaderGeneratedDenoiseOutputEvidenceReadyPresent) {
                $failures.Add("Shader-generated denoise output proof requires shaderGeneratedDenoiseOutputEvidenceReady=true.")
            }
            if (-not $logProof.markers.realShaderDenoiseOutputReadyPresent) {
                $failures.Add("Shader-generated denoise output proof requires realShaderDenoiseOutputReady=true.")
            }
            if ($logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent) {
                $failures.Add("Shader-generated denoise output proof requires CPU/readback fallback inactive; active fallback marker is present.")
            }
            if (-not $logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent) {
                $failures.Add("Shader-generated denoise output proof requires explicit CPU/readback fallback active=false marker.")
            }
            if (-not $logProof.markers.shaderDenoiseOutputConsumedPresent) {
                $failures.Add("Shader-generated denoise output proof requires shaderDenoiseOutputSourceConsumed=true.")
            }
            if (-not $logProof.markers.shaderDenoiseFinalCompositeConsumablePresent) {
                $failures.Add("Shader-generated denoise output proof requires shaderDenoiseFinalCompositeConsumable/finalCompositeConsumable=true.")
            }
            if (-not $logProof.markers.shaderDenoiseRawGiInputProven) {
                $failures.Add("Shader-generated denoise output proof requires raw GI CPU/readback input provenance and must not use the direct-light validation input.")
            }
            if (-not $logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven) {
                $failures.Add("Shader-generated denoise output image slice is not fully proven: raw GI CPU/readback input, raw diffuse-GI source kind, dispatch, output image, material, shader-generated evidence, real output, consumed source, final composite consumable, CPU fallback false, and no direct-light validation input must all be present.")
            }
        }
    }
    if ($tracedGiConsumptionRequired) {
        if (-not $logProof.markers.tracedGiConsumedPresent) {
            $failures.Add("Traced GI consumption proof requires realTracedLightingConsumed=true or tracedLightingConsumedByFinalComposite=true.")
        }
        if (-not $logProof.markers.tracedGiFinalCompositeConsumptionPresent) {
            $failures.Add("Traced GI consumption proof requires traced lighting consumed by the final in-game composite.")
        }
        if (-not $logProof.markers.tracedGiTraceCountersPresent) {
            $failures.Add("Traced GI consumption proof requires positive trace ray and hit counters.")
        }
        if (-not $logProof.markers.tracedGiMaterialDepthSourceCoupled) {
            $failures.Add("Traced GI consumption proof requires positive material-coupled, depth-coupled, and source-bounce counters.")
        }
        if (-not $logProof.markers.tracedGiSourcePresent) {
            $failures.Add("Traced GI consumption proof requires a traced/voxel/ray/hybrid final GI source marker.")
        }
        if (-not $logProof.markers.tracedGiConsumptionPresent) {
            $failures.Add("Traced GI consumption proof is not fully proven from log markers.")
        }
        if ($logProof.markers.metadataOnlyTracingPresent) {
            $failures.Add("Traced GI consumption proof rejects metadataOnlyTracing=true/tracedLightingMetadataOnly=true.")
        }
        if ($logProof.markers.realGpuTraversalOverclaimPresent) {
            $failures.Add("Traced GI consumption proof rejects realGpuTraversalExecuted=true unless explicit GPU traversal evidence is present.")
        }
    }
    if ($fullRendererMilestoneProofRequired) {
        if (-not $logProof.markers.fullRendererProofProfilePresent) {
            $failures.Add("Full renderer milestone proof requires a RealRendererMilestone1 full-renderer profile marker from the controller harness.")
        }
        if (-not $logProof.markers.cleanInWorldCaptureContractPresent) {
            $failures.Add("Full renderer milestone proof requires a clean in-world capture contract marker: menu closed, chat closed, proof overlays forbidden, wrong-window/blank capture forbidden.")
        }
        if ($logProof.markers.menuChatScreenshotContaminationPresent) {
            $failures.Add("Full renderer milestone proof rejects menu/chat screenshot contamination markers.")
        }
        if ($logProof.markers.proofOverlayEvidencePresent -or $logProof.markers.proofMarkerPresent) {
            $failures.Add("Full renderer milestone proof rejects proof overlays and proof-marker evidence.")
        }
        if ($logProof.markers.focusWindowOnlyPresent) {
            $failures.Add("Full renderer milestone proof rejects focus-window-only final composite paths.")
        }
        if ($logProof.markers.lowResDebugSubstitutionPresent) {
            $failures.Add("Full renderer milestone proof rejects low-resolution debug substitutions and CPU direct texture composites.")
        }
        if ($logProof.markers.metadataOnlyPreviewPresent -or $logProof.markers.metadataOnlyTracingPresent -or $logProof.markers.depthGBufferMetadataOnlyPresent) {
            $failures.Add("Full renderer milestone proof rejects metadata-only preview, tracing, or depth/G-buffer evidence.")
        }
        if (-not $logProof.markers.trueDepthGBufferSamplingProven) {
            $failures.Add("Full renderer milestone proof requires true depth/G-buffer sampling: positive sample count, Java/native/shader evidence source, and metadata-only=false.")
        }
        if (-not $logProof.markers.realShadowMapOutputProven) {
            $failures.Add("Full renderer milestone proof requires real/native shadow-map output with positive shadow-map sample/caster/receiver/output counters.")
        }
        if (-not $logProof.markers.shadowMapOutputConsumedPresent) {
            $failures.Add("Full renderer milestone proof requires shadow-map output consumed by the final in-world composite.")
        }
        if (-not $logProof.markers.fullRendererMilestoneProofPresent) {
            $failures.Add("Full renderer milestone proof requires realRendererMilestone1.proof=true or equivalent final proof marker.")
        }
        if ($logProof.markers.fullRendererOverclaimPresent -or $logProof.markers.physicalGiOverclaimPresent -or $logProof.markers.shaderDenoiseOverclaimPresent -or $logProof.markers.realGpuTraversalOverclaimPresent) {
            $failures.Add("Full renderer milestone proof rejects physical GI, shader-denoise, GPU traversal, or full-renderer overclaim markers.")
        }
    }
    if ($playablePhysicalRendererMilestoneProofRequired) {
        if (-not $logProof.markers.playablePhysicalRendererProfilePresent) {
            $failures.Add("Playable physical renderer milestone proof requires a RealRendererMilestone1 playable-physical profile marker from the controller harness.")
        }
        if (-not $logProof.markers.playablePhysicalRendererBudgetPresent) {
            $failures.Add("Playable physical renderer milestone proof requires low-cost/playable budget markers, including heavyProofWorkload=false/fullProofWorkload=false.")
        }
        if (-not $logProof.markers.cleanInWorldCaptureContractPresent) {
            $failures.Add("Playable physical renderer milestone proof requires a clean in-world capture contract marker: menu closed, chat closed, proof overlays forbidden, wrong-window/blank capture forbidden.")
        }
        if ($logProof.markers.menuChatScreenshotContaminationPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects menu/chat screenshot contamination markers.")
        }
        if ($logProof.markers.proofOverlayEvidencePresent -or $logProof.markers.proofMarkerPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects proof overlays and proof-marker evidence.")
        }
        if ($logProof.markers.focusWindowOnlyPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects focus-window-only final composite paths.")
        }
        if ($logProof.markers.lowResDebugSubstitutionPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects fullscreen blobs, low-resolution debug substitutions, and CPU direct texture composites.")
        }
        if ($logProof.markers.metadataOnlyPreviewPresent -or $logProof.markers.metadataOnlyTracingPresent -or $logProof.markers.depthGBufferMetadataOnlyPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects metadata-only preview, tracing, or depth/G-buffer evidence.")
        }
        if (-not $logProof.markers.trueDepthGBufferSamplingProven) {
            $failures.Add("Playable physical renderer milestone proof requires true depth/G-buffer sampling: positive sample count, Java/native/shader evidence source, and metadata-only=false.")
        }
        if (-not $logProof.markers.realShadowMapOutputProven) {
            $failures.Add("Playable physical renderer milestone proof requires real/native shadow-map output with positive shadow-map sample/caster/receiver/output counters.")
        }
        if (-not $logProof.markers.shadowMapOutputConsumedPresent) {
            $failures.Add("Playable physical renderer milestone proof requires shadow-map output consumed by the final in-world composite.")
        }
        if ($logProof.markers.fullRendererOverclaimPresent -or $logProof.markers.physicalGiOverclaimPresent -or $logProof.markers.shaderDenoiseOverclaimPresent -or $logProof.markers.realGpuTraversalOverclaimPresent) {
            $failures.Add("Playable physical renderer milestone proof rejects physical GI, shader-denoise, GPU traversal, or full-renderer overclaim markers.")
        }
    }
    if ($realWorldVisualQualityComparisonProofRequired) {
        if ([string]::IsNullOrWhiteSpace($DebugImagePath) -or -not $debugResolved) {
            $failures.Add("Real-world visual quality comparison proof requires baseline, enabled/final, and playable-physical screenshots; pass the playable-physical screenshot as -DebugImagePath.")
        }
        if (-not $logProof.markers.realWorldVisualQualityComparisonProfilePresent) {
            $failures.Add("Real-world visual quality comparison proof requires controller markers for the baseline/enabled/playable-physical comparison sequence.")
        }
        if (-not $logProof.markers.normalHeavyWorkloadBypassPresent) {
            $failures.Add("Real-world visual quality comparison proof requires normal non-proof gameplay bypass evidence: heavyProofWorkload=false plus native lighting/readback/telemetry bypass markers.")
        }
        if (-not $logProof.markers.playablePhysicalRendererProfilePresent) {
            $failures.Add("Real-world visual quality comparison proof requires the playable-physical renderer profile marker.")
        }
        if (-not $logProof.markers.playablePhysicalRendererBudgetPresent) {
            $failures.Add("Real-world visual quality comparison proof requires playable low-cost budget markers, including heavyProofWorkload=false/fullProofWorkload=false.")
        }
        if (-not $logProof.markers.cleanInWorldCaptureContractPresent) {
            $failures.Add("Real-world visual quality comparison proof requires a clean in-world screenshot contract: no menu/chat, no wrong-window fallback, no blank screenshot.")
        }
        if ($logProof.markers.menuChatScreenshotContaminationPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects menu/chat screenshot contamination markers.")
        }
        if ($logProof.markers.proofOverlayEvidencePresent -or $logProof.markers.proofMarkerPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects proof overlays and proof-marker evidence.")
        }
        if ($logProof.markers.focusWindowOnlyPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects focus-window-only composite paths.")
        }
        if ($logProof.markers.lowResDebugSubstitutionPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects fullscreen wash, fixed light blobs, screen-space decal-only proof, low-resolution debug substitution, CPU direct texture composites, and 0-1 FPS/full proof workload markers.")
        }
        if ($logProof.markers.metadataOnlyPreviewPresent -or $logProof.markers.metadataOnlyTracingPresent -or $logProof.markers.depthGBufferMetadataOnlyPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects metadata-only preview, tracing, or depth/G-buffer evidence.")
        }
        if ($logProof.markers.fullRendererOverclaimPresent -or $logProof.markers.physicalGiOverclaimPresent -or $logProof.markers.shaderDenoiseOverclaimPresent -or $logProof.markers.realGpuTraversalOverclaimPresent) {
            $failures.Add("Real-world visual quality comparison proof rejects physical GI, shader-denoise, GPU traversal, or full-renderer overclaim markers.")
        }
        if (-not $logProof.markers.softReceiverTiedShadowMaskPresent) {
            $failures.Add("Real-world visual quality comparison proof requires a soft, receiver-tied shadow-mask marker rather than a detached screen-space blob/decal.")
        }
        if (-not $logProof.markers.trueDepthGBufferSamplingProven) {
            $failures.Add("Real-world visual quality comparison proof requires true depth/G-buffer sampling with positive sample count and Java/native/shader evidence sources.")
        }
        if (-not $logProof.markers.realShadowMapOutputProven -or -not $logProof.markers.shadowMapOutputConsumedPresent) {
            $failures.Add("Real-world visual quality comparison proof requires real shadow-map output consumed by the final in-world composite.")
        }
        if (-not $logProof.markers.tracedGiConsumptionPresent) {
            $failures.Add("Real-world visual quality comparison proof requires traced/raw GI lighting consumed by the final in-game composite.")
        }
        if (-not $logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven) {
            $failures.Add("Real-world visual quality comparison proof requires shader-generated denoise output consumed by the final composite with CPU/readback fallback inactive.")
        }
    }
}

$result = [ordered]@{
    baselineImage = $baselineResolved
    directImage = $directResolved
    rawGiImage = $rawResolved
    denoisedGiImage = $denoisedResolved
    finalCompositeImage = $finalResolved
    debugImage = $debugResolved
    logPath = $logResolved
    thresholds = [ordered]@{
        minRawChangedPixelPercent = $MinRawChangedPixelPercent
        minDirectChangedPixelPercent = $MinDirectChangedPixelPercent
        minDirectMeanAbsLuma = $MinDirectMeanAbsLuma
        minDenoiseChangedPixelPercent = $MinDenoiseChangedPixelPercent
        minDenoiseMeanAbsLuma = $MinDenoiseMeanAbsLuma
        minDenoiseRoughnessReductionPercent = $MinDenoiseRoughnessReductionPercent
        minDenoiseEdgePreservationPercent = $MinDenoiseEdgePreservationPercent
        minFinalChangedPixelPercent = $MinFinalChangedPixelPercent
        minFinalMeanAbsLuma = $MinFinalMeanAbsLuma
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        focusRegionSelection = if ($DisableAutoFocusRegion) { "fixed" } else { "auto" }
        autoFocusRegion = [ordered]@{
            enabled = -not [bool]$DisableAutoFocusRegion
            searchLeftPercent = $AutoRegionSearchLeftPercent
            searchTopPercent = $AutoRegionSearchTopPercent
            searchWidthPercent = $AutoRegionSearchWidthPercent
            searchHeightPercent = $AutoRegionSearchHeightPercent
            denoiseSearchHeightPercent = $DenoiseAutoRegionSearchHeightPercent
            columns = $AutoRegionColumns
            rows = $AutoRegionRows
            paddingCells = $AutoRegionPaddingCells
        }
        requireLogProof = [bool]$RequireLogProof
        requireDirectImage = [bool]$RequireDirectImage
        requireDebugScreenshot = [bool]$RequireDebugScreenshot
        requireShaderDenoiseEvidence = [bool]$RequireShaderDenoiseEvidence
        requireShaderGeneratedDenoiseOutput = [bool]$RequireShaderGeneratedDenoiseOutput
        requireTracedGiConsumption = [bool]$RequireTracedGiConsumption
        requireFullRendererMilestoneProof = [bool]$RequireFullRendererMilestoneProof
        requirePlayablePhysicalRendererMilestoneProof = [bool]$RequirePlayablePhysicalRendererMilestoneProof
        requireRealWorldVisualQualityComparisonProof = $realWorldVisualQualityComparisonProofRequired
        shaderDenoiseEvidenceRequired = $shaderDenoiseEvidenceRequired
        shaderGeneratedOutputRequiredForProof = $shaderGeneratedOutputRequiredForProof
        physicalGiEvidenceRequired = $physicalGiEvidenceRequired
        tracedGiConsumptionRequired = $tracedGiConsumptionRequired
        requirePhysicalGiEvidence = [bool]$RequirePhysicalGiEvidence
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        directDimensions = $directDimensions
        rawDimensions = $rawDimensions
        denoisedDimensions = $denoisedDimensions
        finalDimensions = $finalDimensions
        debugDimensions = $debugDimensions
    }
    imageDelta = [ordered]@{
        baselineToDirect = $directDelta
        baselineToRawGi = $rawDelta
        rawGiToDenoisedGi = $denoiseDelta
        baselineToFinalComposite = $finalDelta
    }
    selectedFocusRegions = [ordered]@{
        baselineToDirect = if ($directDelta) { $directDelta.focusRegion } else { $null }
        baselineToRawGi = $rawDelta.focusRegion
        rawGiToDenoisedGi = $denoiseDelta.focusRegion
        baselineToFinalComposite = $finalDelta.focusRegion
    }
    logProof = $logProof
    denoiseQuality = $denoiseQuality
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round7_raw_denoised_final_evidence_passed" } else { "round7_evidence_failed" }
        directEvidencePresent = if ($directDelta) {
            ([double]$directDelta.focusRegionMetrics.changedPixelPercent -ge $MinDirectChangedPixelPercent) -and
            ([double]$directDelta.focusRegionMetrics.meanAbsLuma -ge $MinDirectMeanAbsLuma)
        } else {
            $false
        }
        rawGiEvidencePresent = ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -ge $MinRawChangedPixelPercent)
        denoiseComparisonPresent = (
            ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -ge $MinDenoiseChangedPixelPercent) -and
            ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -ge $MinDenoiseMeanAbsLuma) -and
            ([double]$denoiseQuality.meanAbsNeighborLumaReductionPercent -ge $MinDenoiseRoughnessReductionPercent) -and
            ([double]$denoiseQuality.maxEdgeLumaPreservationPercent -ge $MinDenoiseEdgePreservationPercent)
        )
        finalCompositeEvidencePresent = (
            ([double]$finalDelta.focusRegionMetrics.changedPixelPercent -ge $MinFinalChangedPixelPercent) -and
            ([double]$finalDelta.focusRegionMetrics.meanAbsLuma -ge $MinFinalMeanAbsLuma)
        )
        tracks = [ordered]@{
            direct = [ordered]@{
                imageProvided = -not [string]::IsNullOrWhiteSpace($directResolved)
                imageDeltaPresent = if ($directDelta) {
                    ([double]$directDelta.focusRegionMetrics.changedPixelPercent -ge $MinDirectChangedPixelPercent) -and
                    ([double]$directDelta.focusRegionMetrics.meanAbsLuma -ge $MinDirectMeanAbsLuma)
                } else {
                    $null
                }
                logMarkerPresent = if ($logProof) { [bool]$logProof.markers.directSourcePresent } else { $null }
            }
            rawGi = [ordered]@{
                imageDeltaPresent = ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -ge $MinRawChangedPixelPercent)
                logMarkerPresent = if ($logProof) { [bool]$logProof.markers.rawGiSourcePresent } else { $null }
                nativeGiLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.nativeGiSourcePresent } else { $null }
            }
            physicalGi = [ordered]@{
                required = $physicalGiEvidenceRequired
                evidence = if ($logProof) { $logProof.physicalGiEvidence } else { $null }
                evidencePresent = if ($logProof) { [bool]$logProof.markers.physicalGiEvidencePresent } else { $null }
                overclaimPresent = if ($logProof) { [bool]$logProof.markers.physicalGiOverclaimPresent } else { $null }
                classification = if (-not $physicalGiEvidenceRequired) {
                    "recorded_only"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif ([bool]$logProof.markers.physicalGiEvidencePresent) {
                    "native_physical_gi_sample_coupling_evidence_present"
                } else {
                    "native_physical_gi_sample_coupling_evidence_missing"
                }
            }
            tracedGiConsumption = [ordered]@{
                required = $tracedGiConsumptionRequired
                evidence = if ($logProof) { $logProof.tracedGiConsumptionEvidence } else { $null }
                evidencePresent = if ($logProof) { [bool]$logProof.markers.tracedGiConsumptionPresent } else { $null }
                consumedPresent = if ($logProof) { [bool]$logProof.markers.tracedGiConsumedPresent } else { $null }
                finalCompositeConsumptionPresent = if ($logProof) { [bool]$logProof.markers.tracedGiFinalCompositeConsumptionPresent } else { $null }
                traceCountersPresent = if ($logProof) { [bool]$logProof.markers.tracedGiTraceCountersPresent } else { $null }
                materialDepthSourceCoupled = if ($logProof) { [bool]$logProof.markers.tracedGiMaterialDepthSourceCoupled } else { $null }
                metadataOnlyTracingPresent = if ($logProof) { [bool]$logProof.markers.metadataOnlyTracingPresent } else { $null }
                realGpuTraversalExecutedPresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalExecutedPresent } else { $null }
                realGpuTraversalAllowedEvidencePresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalAllowedEvidencePresent } else { $null }
                realGpuTraversalOverclaimPresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalOverclaimPresent } else { $null }
                classification = if (-not $tracedGiConsumptionRequired) {
                    "recorded_only"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif ([bool]$logProof.markers.tracedGiConsumptionPresent) {
                    "traced_raw_diffuse_gi_consumption_present"
                } else {
                    "traced_raw_diffuse_gi_consumption_missing"
                }
            }
            fullRendererMilestone = [ordered]@{
                required = $fullRendererMilestoneProofRequired
                profileMarkerPresent = if ($logProof) { [bool]$logProof.markers.fullRendererProofProfilePresent } else { $null }
                playablePhysicalRequired = $playablePhysicalRendererMilestoneProofRequired
                playablePhysicalProfileMarkerPresent = if ($logProof) { [bool]$logProof.markers.playablePhysicalRendererProfilePresent } else { $null }
                playablePhysicalBudgetPresent = if ($logProof) { [bool]$logProof.markers.playablePhysicalRendererBudgetPresent } else { $null }
                cleanInWorldCaptureContractPresent = if ($logProof) { [bool]$logProof.markers.cleanInWorldCaptureContractPresent } else { $null }
                menuChatScreenshotContaminationPresent = if ($logProof) { [bool]$logProof.markers.menuChatScreenshotContaminationPresent } else { $null }
                proofOverlayEvidencePresent = if ($logProof) { [bool]$logProof.markers.proofOverlayEvidencePresent } else { $null }
                lowResDebugSubstitutionPresent = if ($logProof) { [bool]$logProof.markers.lowResDebugSubstitutionPresent } else { $null }
                trueDepthGBufferSamplingProven = if ($logProof) { [bool]$logProof.markers.trueDepthGBufferSamplingProven } else { $null }
                trueDepthGBufferSamplingMarkerPresent = if ($logProof) { [bool]$logProof.markers.trueDepthGBufferSamplingMarkerPresent } else { $null }
                depthGBufferSourcePresent = if ($logProof) { [bool]$logProof.markers.depthGBufferSourcePresent } else { $null }
                depthGBufferSampleCount = if ($logProof) { $logProof.markers.depthGBufferSampleCount } else { $null }
                depthGBufferMetadataOnlyPresent = if ($logProof) { [bool]$logProof.markers.depthGBufferMetadataOnlyPresent } else { $null }
                realShadowMapOutputProven = if ($logProof) { [bool]$logProof.markers.realShadowMapOutputProven } else { $null }
                realShadowMapOutputMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShadowMapOutputMarkerPresent } else { $null }
                shadowMapSampleCount = if ($logProof) { $logProof.markers.shadowMapSampleCount } else { $null }
                shadowMapOutputConsumedPresent = if ($logProof) { [bool]$logProof.markers.shadowMapOutputConsumedPresent } else { $null }
                finalProofMarkerPresent = if ($logProof) { [bool]$logProof.markers.fullRendererMilestoneProofPresent } else { $null }
                overclaimPresent = if ($logProof) { [bool]$logProof.markers.fullRendererOverclaimPresent } else { $null }
                classification = if (-not $fullRendererMilestoneProofRequired) {
                    "not_required"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif (
                    [bool]$logProof.markers.fullRendererProofProfilePresent -and
                    [bool]$logProof.markers.cleanInWorldCaptureContractPresent -and
                    [bool]$logProof.markers.trueDepthGBufferSamplingProven -and
                    [bool]$logProof.markers.realShadowMapOutputProven -and
                    [bool]$logProof.markers.shadowMapOutputConsumedPresent -and
                    [bool]$logProof.markers.fullRendererMilestoneProofPresent -and
                    -not [bool]$logProof.markers.menuChatScreenshotContaminationPresent -and
                    -not [bool]$logProof.markers.proofOverlayEvidencePresent -and
                    -not [bool]$logProof.markers.lowResDebugSubstitutionPresent -and
                    -not [bool]$logProof.markers.fullRendererOverclaimPresent
                ) {
                    "full_renderer_milestone_log_proof_present"
                } else {
                    "full_renderer_milestone_log_proof_missing_or_contaminated"
                }
                playablePhysicalClassification = if (-not $playablePhysicalRendererMilestoneProofRequired) {
                    "not_required"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif (
                    [bool]$logProof.markers.playablePhysicalRendererProfilePresent -and
                    [bool]$logProof.markers.playablePhysicalRendererBudgetPresent -and
                    [bool]$logProof.markers.cleanInWorldCaptureContractPresent -and
                    [bool]$logProof.markers.trueDepthGBufferSamplingProven -and
                    [bool]$logProof.markers.realShadowMapOutputProven -and
                    [bool]$logProof.markers.shadowMapOutputConsumedPresent -and
                    -not [bool]$logProof.markers.menuChatScreenshotContaminationPresent -and
                    -not [bool]$logProof.markers.proofOverlayEvidencePresent -and
                    -not [bool]$logProof.markers.lowResDebugSubstitutionPresent -and
                    -not [bool]$logProof.markers.fullRendererOverclaimPresent
                ) {
                    "playable_physical_renderer_log_proof_present"
                } else {
                    "playable_physical_renderer_log_proof_missing_or_contaminated"
                }
                realWorldVisualQualityComparison = [ordered]@{
                    required = $realWorldVisualQualityComparisonProofRequired
                    profileMarkerPresent = if ($logProof) { [bool]$logProof.markers.realWorldVisualQualityComparisonProfilePresent } else { $null }
                    normalHeavyWorkloadBypassPresent = if ($logProof) { [bool]$logProof.markers.normalHeavyWorkloadBypassPresent } else { $null }
                    softReceiverTiedShadowMaskPresent = if ($logProof) { [bool]$logProof.markers.softReceiverTiedShadowMaskPresent } else { $null }
                    baselineScreenshotPresent = [bool]$baselineResolved
                    enabledScreenshotPresent = [bool]$finalResolved
                    playablePhysicalScreenshotPresent = [bool]$debugResolved
                    classification = if (-not $realWorldVisualQualityComparisonProofRequired) {
                        "not_required"
                    } elseif (-not $logProof) {
                        "missing_log"
                    } elseif (
                        [bool]$baselineResolved -and
                        [bool]$finalResolved -and
                        [bool]$debugResolved -and
                        [bool]$logProof.markers.realWorldVisualQualityComparisonProfilePresent -and
                        [bool]$logProof.markers.normalHeavyWorkloadBypassPresent -and
                        [bool]$logProof.markers.playablePhysicalRendererProfilePresent -and
                        [bool]$logProof.markers.playablePhysicalRendererBudgetPresent -and
                        [bool]$logProof.markers.softReceiverTiedShadowMaskPresent -and
                        [bool]$logProof.markers.trueDepthGBufferSamplingProven -and
                        [bool]$logProof.markers.realShadowMapOutputProven -and
                        [bool]$logProof.markers.shadowMapOutputConsumedPresent -and
                        [bool]$logProof.markers.tracedGiConsumptionPresent -and
                        [bool]$logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven -and
                        -not [bool]$logProof.markers.menuChatScreenshotContaminationPresent -and
                        -not [bool]$logProof.markers.proofOverlayEvidencePresent -and
                        -not [bool]$logProof.markers.focusWindowOnlyPresent -and
                        -not [bool]$logProof.markers.lowResDebugSubstitutionPresent
                    ) {
                        "real_world_visual_quality_comparison_present"
                    } else {
                        "real_world_visual_quality_comparison_missing_or_contaminated"
                    }
                }
            }
            denoisedGi = [ordered]@{
                imageDeltaPresent = (
                    ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -ge $MinDenoiseChangedPixelPercent) -and
                    ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -ge $MinDenoiseMeanAbsLuma) -and
                    ([double]$denoiseQuality.meanAbsNeighborLumaReductionPercent -ge $MinDenoiseRoughnessReductionPercent) -and
                    ([double]$denoiseQuality.maxEdgeLumaPreservationPercent -ge $MinDenoiseEdgePreservationPercent)
                )
                dispatchLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoiseDispatchPresent } else { $null }
                outputLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoisedGiOutputPresent } else { $null }
                roughnessReductionPercent = $denoiseQuality.meanAbsNeighborLumaReductionPercent
                edgeDetailPreservationPercent = $denoiseQuality.maxEdgeLumaPreservationPercent
                edgeDetailPreserved = $denoiseQuality.edgeDetailPreserved
            }
            shaderDenoise = [ordered]@{
                required = $shaderDenoiseEvidenceRequired
                boundaryEvidenceRequired = [bool]$RequireShaderDenoiseEvidence
                shaderGeneratedOutputRequired = [bool]$RequireShaderGeneratedDenoiseOutput
                shaderGeneratedOutputRequiredForProof = $shaderGeneratedOutputRequiredForProof
                intentLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseIntentPresent } else { $null }
                inputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseInputReadyPresent } else { $null }
                rawGiCpuReadbackInputLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawGiCpuReadbackInputPresent } else { $null }
                rawDiffuseGiInputSourceLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawDiffuseGiInputSourcePresent } else { $null }
                directLightValidationInputLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDirectLightValidationInputPresent } else { $null }
                rawGiInputProven = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawGiInputProven } else { $null }
                dispatchPreparedLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDispatchPreparedPresent } else { $null }
                outputAttemptedLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputAttemptedPresent } else { $null }
                outputAttemptGenerationLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputAttemptGenerationPresent } else { $null }
                outputAttemptGeneration = if ($logProof) { $logProof.markers.shaderDenoiseOutputAttemptGeneration } else { $null }
                outputImageReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageReadyPresent } else { $null }
                outputImageNotReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageNotReadyPresent } else { $null }
                outputImageStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageStateExplicitPresent } else { $null }
                outputImageCandidate = if ($logProof) { $logProof.shaderOutputImageCandidate } else { $null }
                outputImageCandidatePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidatePresent } else { $null }
                outputImageCandidateReady = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateReadyPresent } else { $null }
                outputImageCandidateCpuStaged = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateCpuStagedPresent } else { $null }
                outputImageCandidateNonGpu = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateNonGpuPresent } else { $null }
                outputImageCandidateBoundaryOnly = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateBoundaryOnly } else { $null }
                outputImageCandidateEvidenceLabel = if ($logProof) { $logProof.shaderOutputImageCandidate.evidenceLabel } else { $null }
                outputMaterialReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputMaterialReadyPresent } else { $null }
                outputMaterialNotReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputMaterialNotReadyPresent } else { $null }
                outputMaterialStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputMaterialStateExplicitPresent } else { $null }
                shaderGeneratedOutputTrueLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseShaderGeneratedOutputTruePresent } else { $null }
                shaderGeneratedOutputFalseLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseShaderGeneratedOutputFalsePresent } else { $null }
                shaderGeneratedOutputExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseShaderGeneratedOutputExplicitPresent } else { $null }
                shaderGeneratedOutputEvidenceReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderGeneratedDenoiseOutputEvidenceReadyPresent } else { $null }
                cpuReadbackFallbackActiveLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent } else { $null }
                cpuReadbackFallbackInactiveLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent } else { $null }
                cpuReadbackFallbackExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackExplicitPresent } else { $null }
                cpuReadbackSourceLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.cpuReadbackDenoiseSourcePresent } else { $null }
                candidateOnlySourceLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputCandidateOnlySourcePresent } else { $null }
                noOverclaimLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseNoOverclaimPresent } else { $null }
                realOutputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputReadyPresent } else { $null }
                realOutputNotReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputNotReadyPresent } else { $null }
                realOutputStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputStateExplicitPresent } else { $null }
                realOutputProven = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputProven } else { $null }
                outputConsumedLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputConsumedPresent } else { $null }
                finalCompositeConsumableLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseFinalCompositeConsumablePresent } else { $null }
                shaderGeneratedOutputImageSliceProven = if ($logProof) { [bool]$logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven } else { $null }
                openBoundaryPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOpenBoundaryPresent } else { $null }
                shaderOutputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputReadyPresent } else { $null }
                shaderSourceClaimLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseSourceClaimPresent } else { $null }
                shaderOutputOpenLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputOpenPresent } else { $null }
                shaderOutputStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputStateExplicitPresent } else { $null }
                shaderOutputOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOverclaimPresent } else { $null }
                honestNonOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseHonestNonOverclaimPresent } else { $null }
                boundary = if ($logProof) { $logProof.shaderDenoiseBoundary } else { $null }
                sourceIdentity = if ($logProof) { $logProof.shaderDenoiseBoundary.sourceIdentity } else { $null }
                sourceKind = if ($logProof) { $logProof.shaderDenoiseBoundary.sourceKind } else { $null }
                blockerReason = if ($logProof) { $logProof.shaderDenoiseBoundary.blockerReason } else { $null }
                classification = if (-not $shaderDenoiseEvidenceRequired) {
                    "not_required"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif ($shaderGeneratedOutputRequiredForProof -and [bool]$logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven) {
                    "shader_generated_denoise_output_image_slice_proven"
                } elseif ($shaderGeneratedOutputRequiredForProof) {
                    "shader_generated_denoise_output_image_slice_missing"
                } elseif ([bool]$logProof.markers.realShaderDenoiseOutputProven) {
                    "real_shader_denoise_output_proven"
                } elseif ([bool]$logProof.markers.shaderDenoiseOutputImageCandidateBoundaryOnly) {
                    "shader_output_image_candidate_boundary_only_real_shader_output_open"
                } elseif ([bool]$logProof.markers.shaderDenoiseOpenBoundaryPresent) {
                    "shader_denoise_open_boundary_or_cpu_readback_fallback"
                } else {
                    "shader_denoise_output_state_missing"
                }
            }
            finalComposite = [ordered]@{
                imageDeltaPresent = (
                    ([double]$finalDelta.focusRegionMetrics.changedPixelPercent -ge $MinFinalChangedPixelPercent) -and
                    ([double]$finalDelta.focusRegionMetrics.meanAbsLuma -ge $MinFinalMeanAbsLuma)
                )
                logMarkerPresent = if ($logProof) { [bool]$logProof.markers.finalCompositePresent } else { $null }
                hudSafeLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.hudSafeFinalCompositePresent } else { $null }
            }
            rejectionMarkers = [ordered]@{
                temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
                metadataOnlyPreviewPresent = if ($logProof) { [bool]$logProof.markers.metadataOnlyPreviewPresent } else { $null }
                firstPracticalCpuOutputPresent = if ($logProof) { [bool]$logProof.markers.firstPracticalCpuOutputPresent } else { $null }
                realDenoiseShaderOutputPresent = if ($logProof) { [bool]$logProof.markers.realDenoiseShaderOutputPresent } else { $null }
                realDenoiseShaderOutputFalsePresent = if ($logProof) { [bool]$logProof.markers.realDenoiseShaderOutputFalsePresent } else { $null }
                shaderDenoiseIntentPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseIntentPresent } else { $null }
                shaderDenoiseInputReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseInputReadyPresent } else { $null }
                shaderDenoiseRawGiCpuReadbackInputPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawGiCpuReadbackInputPresent } else { $null }
                shaderDenoiseRawDiffuseGiInputSourcePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawDiffuseGiInputSourcePresent } else { $null }
                shaderDenoiseDirectLightValidationInputPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDirectLightValidationInputPresent } else { $null }
                shaderDenoiseRawGiInputProven = if ($logProof) { [bool]$logProof.markers.shaderDenoiseRawGiInputProven } else { $null }
                shaderDenoiseDispatchPreparedPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDispatchPreparedPresent } else { $null }
                shaderDenoiseOutputAttemptedPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputAttemptedPresent } else { $null }
                shaderDenoiseOutputAttemptGenerationPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputAttemptGenerationPresent } else { $null }
                shaderDenoiseOutputAttemptGeneration = if ($logProof) { $logProof.markers.shaderDenoiseOutputAttemptGeneration } else { $null }
                shaderDenoiseOutputImageReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageReadyPresent } else { $null }
                shaderDenoiseOutputImageNotReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageNotReadyPresent } else { $null }
                shaderDenoiseOutputImageCandidatePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidatePresent } else { $null }
                shaderDenoiseOutputImageCandidateReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateReadyPresent } else { $null }
                shaderDenoiseOutputImageCandidateCpuStagedPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateCpuStagedPresent } else { $null }
                shaderDenoiseOutputImageCandidateNonGpuPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateNonGpuPresent } else { $null }
                shaderDenoiseOutputImageCandidateBoundaryOnly = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputImageCandidateBoundaryOnly } else { $null }
                shaderDenoiseOutputMaterialReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputMaterialReadyPresent } else { $null }
                shaderDenoiseOutputMaterialNotReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputMaterialNotReadyPresent } else { $null }
                shaderDenoiseShaderGeneratedOutputTruePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseShaderGeneratedOutputTruePresent } else { $null }
                shaderDenoiseShaderGeneratedOutputFalsePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseShaderGeneratedOutputFalsePresent } else { $null }
                shaderDenoiseCpuReadbackFallbackActivePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent } else { $null }
                shaderDenoiseCpuReadbackFallbackInactivePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent } else { $null }
                realShaderDenoiseOutputReadyPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputReadyPresent } else { $null }
                realShaderDenoiseOutputNotReadyPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputNotReadyPresent } else { $null }
                realShaderDenoiseOutputProven = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputProven } else { $null }
                shaderDenoiseOpenBoundaryPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOpenBoundaryPresent } else { $null }
                cpuReadbackDenoiseSourcePresent = if ($logProof) { [bool]$logProof.markers.cpuReadbackDenoiseSourcePresent } else { $null }
                shaderDenoiseOutputCandidateOnlySourcePresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputCandidateOnlySourcePresent } else { $null }
                shaderDenoiseNoOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseNoOverclaimPresent } else { $null }
                shaderDenoiseOutputReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputReadyPresent } else { $null }
                shaderDenoiseSourceClaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseSourceClaimPresent } else { $null }
                shaderDenoiseOutputOpenPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputOpenPresent } else { $null }
                shaderDenoiseOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOverclaimPresent } else { $null }
                shaderDenoiseHonestNonOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseHonestNonOverclaimPresent } else { $null }
                shaderDenoiseSourceIdentity = if ($logProof) { $logProof.shaderDenoiseBoundary.sourceIdentity } else { $null }
                shaderDenoiseSourceKind = if ($logProof) { $logProof.shaderDenoiseBoundary.sourceKind } else { $null }
                shaderDenoiseBlockerReason = if ($logProof) { $logProof.shaderDenoiseBoundary.blockerReason } else { $null }
                physicalGiEvidencePresent = if ($logProof) { [bool]$logProof.markers.physicalGiEvidencePresent } else { $null }
                physicalGiOverclaimPresent = if ($logProof) { [bool]$logProof.markers.physicalGiOverclaimPresent } else { $null }
                tracedGiConsumptionPresent = if ($logProof) { [bool]$logProof.markers.tracedGiConsumptionPresent } else { $null }
                tracedGiConsumedPresent = if ($logProof) { [bool]$logProof.markers.tracedGiConsumedPresent } else { $null }
                tracedGiFinalCompositeConsumptionPresent = if ($logProof) { [bool]$logProof.markers.tracedGiFinalCompositeConsumptionPresent } else { $null }
                tracedGiTraceCountersPresent = if ($logProof) { [bool]$logProof.markers.tracedGiTraceCountersPresent } else { $null }
                tracedGiMaterialDepthSourceCoupled = if ($logProof) { [bool]$logProof.markers.tracedGiMaterialDepthSourceCoupled } else { $null }
                metadataOnlyTracingPresent = if ($logProof) { [bool]$logProof.markers.metadataOnlyTracingPresent } else { $null }
                realGpuTraversalExecutedPresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalExecutedPresent } else { $null }
                realGpuTraversalAllowedEvidencePresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalAllowedEvidencePresent } else { $null }
                realGpuTraversalOverclaimPresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalOverclaimPresent } else { $null }
                fullRendererProofProfilePresent = if ($logProof) { [bool]$logProof.markers.fullRendererProofProfilePresent } else { $null }
                cleanInWorldCaptureContractPresent = if ($logProof) { [bool]$logProof.markers.cleanInWorldCaptureContractPresent } else { $null }
                menuChatScreenshotContaminationPresent = if ($logProof) { [bool]$logProof.markers.menuChatScreenshotContaminationPresent } else { $null }
                proofOverlayEvidencePresent = if ($logProof) { [bool]$logProof.markers.proofOverlayEvidencePresent } else { $null }
                lowResDebugSubstitutionPresent = if ($logProof) { [bool]$logProof.markers.lowResDebugSubstitutionPresent } else { $null }
                depthGBufferMetadataOnlyPresent = if ($logProof) { [bool]$logProof.markers.depthGBufferMetadataOnlyPresent } else { $null }
                fullRendererOverclaimPresent = if ($logProof) { [bool]$logProof.markers.fullRendererOverclaimPresent } else { $null }
                proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
                focusWindowOnlyPresent = if ($logProof) { [bool]$logProof.markers.focusWindowOnlyPresent } else { $null }
                nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
            }
        }
    }
    passed = $failures.Count -eq 0
    failures = @($failures)
}

$json = $result | ConvertTo-Json -Depth 12
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    $parent = Split-Path -Parent $OutputJsonPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Set-Content -LiteralPath $OutputJsonPath -Value $json -Encoding UTF8
}

Write-Host "baselineImage=$($result.baselineImage)"
Write-Host "directImage=$($result.directImage)"
Write-Host "rawGiImage=$($result.rawGiImage)"
Write-Host "denoisedGiImage=$($result.denoisedGiImage)"
Write-Host "finalCompositeImage=$($result.finalCompositeImage)"
Write-Host "debugImage=$($result.debugImage)"
Write-Host "logPath=$($result.logPath)"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
if ($directDelta) {
    Write-Host "direct.focusRegion=$($directDelta.focusRegion.left),$($directDelta.focusRegion.top),$($directDelta.focusRegion.width),$($directDelta.focusRegion.height)"
    Write-Host "direct.focus.changedPixelPercent=$($directDelta.focusRegionMetrics.changedPixelPercent)"
    Write-Host "direct.focus.meanAbsLuma=$($directDelta.focusRegionMetrics.meanAbsLuma)"
}
Write-Host "raw.focusRegion=$($rawDelta.focusRegion.left),$($rawDelta.focusRegion.top),$($rawDelta.focusRegion.width),$($rawDelta.focusRegion.height)"
Write-Host "raw.focus.changedPixelPercent=$($rawDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "denoise.focusRegion=$($denoiseDelta.focusRegion.left),$($denoiseDelta.focusRegion.top),$($denoiseDelta.focusRegion.width),$($denoiseDelta.focusRegion.height)"
Write-Host "denoise.focus.changedPixelPercent=$($denoiseDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "denoise.focus.meanAbsLuma=$($denoiseDelta.focusRegionMetrics.meanAbsLuma)"
Write-Host "denoise.roughness.raw.meanAbsNeighborLuma=$($denoiseQuality.raw.meanAbsNeighborLuma)"
Write-Host "denoise.roughness.denoised.meanAbsNeighborLuma=$($denoiseQuality.denoised.meanAbsNeighborLuma)"
Write-Host "denoise.roughness.meanAbsNeighborLumaReductionPercent=$($denoiseQuality.meanAbsNeighborLumaReductionPercent)"
Write-Host "denoise.roughness.rmsNeighborLumaReductionPercent=$($denoiseQuality.rmsNeighborLumaReductionPercent)"
Write-Host "denoise.detail.maxEdgeLumaPreservationPercent=$($denoiseQuality.maxEdgeLumaPreservationPercent)"
Write-Host "denoise.detail.edgeDetailPreserved=$($denoiseQuality.edgeDetailPreserved)"
Write-Host "final.focusRegion=$($finalDelta.focusRegion.left),$($finalDelta.focusRegion.top),$($finalDelta.focusRegion.width),$($finalDelta.focusRegion.height)"
Write-Host "final.focus.changedPixelPercent=$($finalDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "final.focus.meanAbsLuma=$($finalDelta.focusRegionMetrics.meanAbsLuma)"
if ($logProof) {
    Write-Host "directSourcePresent=$($logProof.markers.directSourcePresent)"
    Write-Host "nativeGiSourcePresent=$($logProof.markers.nativeGiSourcePresent)"
    Write-Host "rawGiSourcePresent=$($logProof.markers.rawGiSourcePresent)"
    Write-Host "denoiseDispatchPresent=$($logProof.markers.denoiseDispatchPresent)"
    Write-Host "denoisedGiOutputPresent=$($logProof.markers.denoisedGiOutputPresent)"
    Write-Host "finalCompositePresent=$($logProof.markers.finalCompositePresent)"
    Write-Host "hudSafeFinalCompositePresent=$($logProof.markers.hudSafeFinalCompositePresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "metadataOnlyPreviewPresent=$($logProof.markers.metadataOnlyPreviewPresent)"
    Write-Host "firstPracticalCpuOutputPresent=$($logProof.markers.firstPracticalCpuOutputPresent)"
    Write-Host "realDenoiseShaderOutputPresent=$($logProof.markers.realDenoiseShaderOutputPresent)"
    Write-Host "realDenoiseShaderOutputFalsePresent=$($logProof.markers.realDenoiseShaderOutputFalsePresent)"
    Write-Host "shaderDenoiseIntentPresent=$($logProof.markers.shaderDenoiseIntentPresent)"
    Write-Host "shaderDenoiseInputReadyPresent=$($logProof.markers.shaderDenoiseInputReadyPresent)"
    Write-Host "shaderDenoiseRawGiCpuReadbackInputPresent=$($logProof.markers.shaderDenoiseRawGiCpuReadbackInputPresent)"
    Write-Host "shaderDenoiseRawDiffuseGiInputSourcePresent=$($logProof.markers.shaderDenoiseRawDiffuseGiInputSourcePresent)"
    Write-Host "shaderDenoiseDirectLightValidationInputPresent=$($logProof.markers.shaderDenoiseDirectLightValidationInputPresent)"
    Write-Host "shaderDenoiseRawGiInputProven=$($logProof.markers.shaderDenoiseRawGiInputProven)"
    Write-Host "shaderDenoiseDispatchPreparedPresent=$($logProof.markers.shaderDenoiseDispatchPreparedPresent)"
    Write-Host "shaderDenoiseOutputAttemptedPresent=$($logProof.markers.shaderDenoiseOutputAttemptedPresent)"
    Write-Host "shaderDenoiseOutputAttemptGenerationPresent=$($logProof.markers.shaderDenoiseOutputAttemptGenerationPresent)"
    Write-Host "shaderDenoiseOutputAttemptGeneration=$($logProof.markers.shaderDenoiseOutputAttemptGeneration)"
    Write-Host "shaderDenoiseOutputImageReadyPresent=$($logProof.markers.shaderDenoiseOutputImageReadyPresent)"
    Write-Host "shaderDenoiseOutputImageNotReadyPresent=$($logProof.markers.shaderDenoiseOutputImageNotReadyPresent)"
    Write-Host "shaderDenoiseOutputImageStateExplicitPresent=$($logProof.markers.shaderDenoiseOutputImageStateExplicitPresent)"
    Write-Host "shaderDenoiseOutputImageCandidatePresent=$($logProof.markers.shaderDenoiseOutputImageCandidatePresent)"
    Write-Host "shaderDenoiseOutputImageCandidateReadyPresent=$($logProof.markers.shaderDenoiseOutputImageCandidateReadyPresent)"
    Write-Host "shaderDenoiseOutputImageCandidateCpuStagedPresent=$($logProof.markers.shaderDenoiseOutputImageCandidateCpuStagedPresent)"
    Write-Host "shaderDenoiseOutputImageCandidateNonGpuPresent=$($logProof.markers.shaderDenoiseOutputImageCandidateNonGpuPresent)"
    Write-Host "shaderDenoiseOutputImageCandidateBoundaryOnly=$($logProof.markers.shaderDenoiseOutputImageCandidateBoundaryOnly)"
    Write-Host "shaderDenoiseOutputImageCandidateDims=$($logProof.shaderOutputImageCandidate.width)x$($logProof.shaderOutputImageCandidate.height)"
    Write-Host "shaderDenoiseOutputImageCandidatePixels=$($logProof.shaderOutputImageCandidate.pixels)"
    Write-Host "shaderDenoiseOutputImageCandidateBytes=$($logProof.shaderOutputImageCandidate.bytes)"
    Write-Host "shaderDenoiseOutputImageCandidateChecksum=$($logProof.shaderOutputImageCandidate.checksum)"
    Write-Host "shaderDenoiseOutputImageCandidateMarker=$($logProof.shaderOutputImageCandidate.marker)"
    Write-Host "shaderDenoiseOutputImageCandidateBlocker=$($logProof.shaderOutputImageCandidate.blocker)"
    Write-Host "shaderDenoiseOutputImageCandidateEvidence=$($logProof.shaderOutputImageCandidate.evidenceLabel)"
    Write-Host "shaderDenoiseOutputMaterialReadyPresent=$($logProof.markers.shaderDenoiseOutputMaterialReadyPresent)"
    Write-Host "shaderDenoiseOutputMaterialNotReadyPresent=$($logProof.markers.shaderDenoiseOutputMaterialNotReadyPresent)"
    Write-Host "shaderDenoiseOutputMaterialStateExplicitPresent=$($logProof.markers.shaderDenoiseOutputMaterialStateExplicitPresent)"
    Write-Host "shaderDenoiseShaderGeneratedOutputTruePresent=$($logProof.markers.shaderDenoiseShaderGeneratedOutputTruePresent)"
    Write-Host "shaderDenoiseShaderGeneratedOutputFalsePresent=$($logProof.markers.shaderDenoiseShaderGeneratedOutputFalsePresent)"
    Write-Host "shaderDenoiseShaderGeneratedOutputExplicitPresent=$($logProof.markers.shaderDenoiseShaderGeneratedOutputExplicitPresent)"
    Write-Host "shaderGeneratedDenoiseOutputEvidenceReadyPresent=$($logProof.markers.shaderGeneratedDenoiseOutputEvidenceReadyPresent)"
    Write-Host "shaderDenoiseCpuReadbackFallbackActivePresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent)"
    Write-Host "shaderDenoiseCpuReadbackFallbackInactivePresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent)"
    Write-Host "shaderDenoiseCpuReadbackFallbackExplicitPresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackExplicitPresent)"
    Write-Host "cpuReadbackDenoiseSourcePresent=$($logProof.markers.cpuReadbackDenoiseSourcePresent)"
    Write-Host "shaderDenoiseOutputCandidateOnlySourcePresent=$($logProof.markers.shaderDenoiseOutputCandidateOnlySourcePresent)"
    Write-Host "shaderDenoiseNoOverclaimPresent=$($logProof.markers.shaderDenoiseNoOverclaimPresent)"
    Write-Host "realShaderDenoiseOutputReadyPresent=$($logProof.markers.realShaderDenoiseOutputReadyPresent)"
    Write-Host "realShaderDenoiseOutputNotReadyPresent=$($logProof.markers.realShaderDenoiseOutputNotReadyPresent)"
    Write-Host "realShaderDenoiseOutputStateExplicitPresent=$($logProof.markers.realShaderDenoiseOutputStateExplicitPresent)"
    Write-Host "realShaderDenoiseOutputProven=$($logProof.markers.realShaderDenoiseOutputProven)"
    Write-Host "shaderDenoiseOutputConsumedPresent=$($logProof.markers.shaderDenoiseOutputConsumedPresent)"
    Write-Host "shaderDenoiseFinalCompositeConsumablePresent=$($logProof.markers.shaderDenoiseFinalCompositeConsumablePresent)"
    Write-Host "shaderGeneratedDenoiseOutputImageSliceProven=$($logProof.markers.shaderGeneratedDenoiseOutputImageSliceProven)"
    Write-Host "shaderDenoiseOpenBoundaryPresent=$($logProof.markers.shaderDenoiseOpenBoundaryPresent)"
    Write-Host "shaderDenoiseOutputReadyPresent=$($logProof.markers.shaderDenoiseOutputReadyPresent)"
    Write-Host "shaderDenoiseSourceClaimPresent=$($logProof.markers.shaderDenoiseSourceClaimPresent)"
    Write-Host "shaderDenoiseOutputOpenPresent=$($logProof.markers.shaderDenoiseOutputOpenPresent)"
    Write-Host "shaderDenoiseOutputStateExplicitPresent=$($logProof.markers.shaderDenoiseOutputStateExplicitPresent)"
    Write-Host "shaderDenoiseOverclaimPresent=$($logProof.markers.shaderDenoiseOverclaimPresent)"
    Write-Host "shaderDenoiseHonestNonOverclaimPresent=$($logProof.markers.shaderDenoiseHonestNonOverclaimPresent)"
    Write-Host "shaderDenoiseSourceIdentity=$($logProof.shaderDenoiseBoundary.sourceIdentity)"
    Write-Host "shaderDenoiseSourceAuthenticity=$($logProof.shaderDenoiseBoundary.sourceAuthenticity)"
    Write-Host "shaderDenoiseSourceKind=$($logProof.shaderDenoiseBoundary.sourceKind)"
    Write-Host "shaderDenoiseBlockerReason=$($logProof.shaderDenoiseBoundary.blockerReason)"
    Write-Host "shaderDenoisePrereq.intent=$($logProof.shaderDenoiseBoundary.prerequisites.intent)"
    Write-Host "shaderDenoisePrereq.inputReady=$($logProof.shaderDenoiseBoundary.prerequisites.inputReady)"
    Write-Host "shaderDenoisePrereq.rawGiCpuReadbackInput=$($logProof.shaderDenoiseBoundary.prerequisites.rawGiCpuReadbackInput)"
    Write-Host "shaderDenoisePrereq.rawDiffuseGiInputSource=$($logProof.shaderDenoiseBoundary.prerequisites.rawDiffuseGiInputSource)"
    Write-Host "shaderDenoisePrereq.directLightValidationInputRejected=$($logProof.shaderDenoiseBoundary.prerequisites.directLightValidationInputRejected)"
    Write-Host "shaderDenoisePrereq.dispatchPrepared=$($logProof.shaderDenoiseBoundary.prerequisites.dispatchPrepared)"
    Write-Host "shaderDenoisePrereq.outputAttempted=$($logProof.shaderDenoiseBoundary.prerequisites.outputAttempted)"
    Write-Host "shaderDenoisePrereq.outputAttemptGeneration=$($logProof.shaderDenoiseBoundary.prerequisites.outputAttemptGeneration)"
    Write-Host "shaderDenoisePrereq.outputImageReady=$($logProof.shaderDenoiseBoundary.prerequisites.outputImageReady)"
    Write-Host "shaderDenoisePrereq.outputMaterialReady=$($logProof.shaderDenoiseBoundary.prerequisites.outputMaterialReady)"
    Write-Host "shaderDenoisePrereq.shaderGeneratedOutput=$($logProof.shaderDenoiseBoundary.prerequisites.shaderGeneratedOutput)"
    Write-Host "shaderDenoisePrereq.cpuReadbackFallbackActive=$($logProof.shaderDenoiseBoundary.prerequisites.cpuReadbackFallbackActive)"
    Write-Host "shaderDenoisePrereq.realOutputReady=$($logProof.shaderDenoiseBoundary.prerequisites.realOutputReady)"
    Write-Host "shaderDenoisePrereq.noOverclaim=$($logProof.shaderDenoiseBoundary.prerequisites.noOverclaim)"
    Write-Host "shaderDenoisePrereq.candidateOnlySource=$($logProof.shaderDenoiseBoundary.prerequisites.candidateOnlySource)"
    Write-Host "physicalGiEvidencePresent=$($logProof.markers.physicalGiEvidencePresent)"
    Write-Host "physicalGiOverclaimPresent=$($logProof.markers.physicalGiOverclaimPresent)"
    Write-Host "fullRendererProofProfilePresent=$($logProof.markers.fullRendererProofProfilePresent)"
    Write-Host "playablePhysicalRendererProfilePresent=$($logProof.markers.playablePhysicalRendererProfilePresent)"
    Write-Host "playablePhysicalRendererBudgetPresent=$($logProof.markers.playablePhysicalRendererBudgetPresent)"
    Write-Host "cleanInWorldCaptureContractPresent=$($logProof.markers.cleanInWorldCaptureContractPresent)"
    Write-Host "menuChatScreenshotContaminationPresent=$($logProof.markers.menuChatScreenshotContaminationPresent)"
    Write-Host "proofOverlayEvidencePresent=$($logProof.markers.proofOverlayEvidencePresent)"
    Write-Host "lowResDebugSubstitutionPresent=$($logProof.markers.lowResDebugSubstitutionPresent)"
    Write-Host "trueDepthGBufferSamplingProven=$($logProof.markers.trueDepthGBufferSamplingProven)"
    Write-Host "trueDepthGBufferSamplingMarkerPresent=$($logProof.markers.trueDepthGBufferSamplingMarkerPresent)"
    Write-Host "depthGBufferSourcePresent=$($logProof.markers.depthGBufferSourcePresent)"
    Write-Host "depthGBufferSampleCount=$($logProof.markers.depthGBufferSampleCount)"
    Write-Host "depthGBufferMetadataOnlyPresent=$($logProof.markers.depthGBufferMetadataOnlyPresent)"
    Write-Host "realShadowMapOutputProven=$($logProof.markers.realShadowMapOutputProven)"
    Write-Host "realShadowMapOutputMarkerPresent=$($logProof.markers.realShadowMapOutputMarkerPresent)"
    Write-Host "shadowMapSampleCount=$($logProof.markers.shadowMapSampleCount)"
    Write-Host "shadowMapOutputConsumedPresent=$($logProof.markers.shadowMapOutputConsumedPresent)"
    Write-Host "fullRendererMilestoneProofPresent=$($logProof.markers.fullRendererMilestoneProofPresent)"
    Write-Host "fullRendererOverclaimPresent=$($logProof.markers.fullRendererOverclaimPresent)"
    Write-Host "physicalSceneLinkedPresent=$($logProof.physicalGiEvidence.physicalSceneLinkedPresent)"
    Write-Host "physicalSurfaceContributionPresent=$($logProof.physicalGiEvidence.physicalSurfaceContributionPresent)"
    Write-Host "physicalGiSampleMarkerPresent=$($logProof.physicalGiEvidence.physicalGiSampleMarkerPresent)"
    Write-Host "surfaceMaterialHitMarkerPresent=$($logProof.physicalGiEvidence.surfaceMaterialHitMarkerPresent)"
    Write-Host "max.physicalGiSamples=$($logProof.physicalGiEvidence.physicalGiSamples)"
    Write-Host "max.physicalGiHitSamples=$($logProof.physicalGiEvidence.physicalGiHitSamples)"
    Write-Host "max.surfaceMaterialHitCoupledSamples=$($logProof.physicalGiEvidence.surfaceMaterialHitCoupledSamples)"
    Write-Host "max.geometryHitCoupledSamples=$($logProof.physicalGiEvidence.geometryHitCoupledSamples)"
    Write-Host "max.surfaceMaterialHitCoupling=$($logProof.physicalGiEvidence.surfaceMaterialHitCoupling)"
    Write-Host "max.geometryHitCoupling=$($logProof.physicalGiEvidence.geometryHitCoupling)"
    Write-Host "max.physicalSceneLinkScore=$($logProof.physicalGiEvidence.physicalSceneLinkScore)"
    Write-Host "max.physicalOutputChecksum=$($logProof.physicalGiEvidence.physicalOutputChecksum)"
    Write-Host "tracedGiConsumptionPresent=$($logProof.markers.tracedGiConsumptionPresent)"
    Write-Host "tracedGiConsumedPresent=$($logProof.markers.tracedGiConsumedPresent)"
    Write-Host "tracedGiFinalCompositeConsumptionPresent=$($logProof.markers.tracedGiFinalCompositeConsumptionPresent)"
    Write-Host "tracedGiTraceCountersPresent=$($logProof.markers.tracedGiTraceCountersPresent)"
    Write-Host "tracedGiSourcePresent=$($logProof.markers.tracedGiSourcePresent)"
    Write-Host "tracedGiMaterialDepthSourceCoupled=$($logProof.markers.tracedGiMaterialDepthSourceCoupled)"
    Write-Host "max.tracedGiRayCount=$($logProof.tracedGiConsumptionEvidence.rayCount)"
    Write-Host "max.tracedGiHitCount=$($logProof.tracedGiConsumptionEvidence.hitCount)"
    Write-Host "max.tracedGiMaterialCoupledHitCount=$($logProof.tracedGiConsumptionEvidence.materialCoupledHitCount)"
    Write-Host "max.tracedGiDepthCoupledHitCount=$($logProof.tracedGiConsumptionEvidence.depthCoupledHitCount)"
    Write-Host "max.tracedGiSourceCoupledBounceCount=$($logProof.tracedGiConsumptionEvidence.sourceCoupledBounceCount)"
    Write-Host "metadataOnlyTracingPresent=$($logProof.markers.metadataOnlyTracingPresent)"
    Write-Host "realGpuTraversalExecutedPresent=$($logProof.markers.realGpuTraversalExecutedPresent)"
    Write-Host "realGpuTraversalAllowedEvidencePresent=$($logProof.markers.realGpuTraversalAllowedEvidencePresent)"
    Write-Host "realGpuTraversalOverclaimPresent=$($logProof.markers.realGpuTraversalOverclaimPresent)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "focusWindowOnlyPresent=$($logProof.markers.focusWindowOnlyPresent)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 7 denoise/composite proof failed: $($failures -join '; ')"
}
