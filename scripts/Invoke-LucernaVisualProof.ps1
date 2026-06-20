param(
    [ValidateSet("Baseline", "Enabled", "Debug", "Direct", "RawGi", "DenoisedGi", "FinalComposite", "ShaderDenoisedGi", "ShaderDenoiseFinalComposite", "ShaderDenoiseDebug", "ParticleBaseline", "ParticleFinalComposite", "TranslucentBaseline", "TranslucentFinalComposite", "TemporalStable", "TemporalMoved", "StableHeatmap", "MovedHeatmap", "EmissiveHeatmap", "HistoryStable", "HistoryMoved", "FlatClusterOverlay", "InteriorCullingOverlay", "HighDistanceCullingOverlay", "ForestComplexCullingOverlay", "VoxelRayDebug", "RtEntityDebug", "HybridHitDebug", "DirectReservoirDebug", "GiReservoirDebug", "ReservoirReuseDebug", "DirectBruteBaseline", "RestirDirectEnabled", "RestirTemporalStable", "RestirTemporalMoved", "RestirExecutionDebug")]
    [string] $Mode,

    [ValidateSet("Round5Direct", "Round5DirectSurface", "Round6DiffuseGi", "Round6NativeDiffuseGi", "Round6NativeDiffuseGiNoMarker", "Round56PhysicalLighting", "Round7DenoiseComposite", "Round7CompositeStability", "Round7EmissiveGiSurface", "Round8AdaptiveHeatmaps", "Round9VirtualizedGeometry", "Round10HybridTracing", "Round11Restir")]
    [string] $ValidationProfile = "Round5Direct",

    [string] $WorldName = "New World",

    [string] $ScenarioName = "",

    [switch] $SetupScene,

    [string] $BaselineImagePath = "",

    [string] $EnabledImagePath = "",

    [string] $ImageDeltaJsonPath = "",

    [string] $ImageDiagnosticsJsonPath = "",

    [switch] $IncludeImageBandDiagnostics,

    [double] $ImageDeltaRegionLeftPercent = 30.0,

    [double] $ImageDeltaRegionTopPercent = 20.0,

    [double] $ImageDeltaRegionWidthPercent = 40.0,

    [double] $ImageDeltaRegionHeightPercent = 55.0,

    [switch] $AutoImageDeltaRegion,

    [double] $AutoImageDeltaSearchLeftPercent = 5.0,

    [double] $AutoImageDeltaSearchTopPercent = 10.0,

    [double] $AutoImageDeltaSearchWidthPercent = 90.0,

    [double] $AutoImageDeltaSearchHeightPercent = 80.0,

    [int] $AutoImageDeltaRegionColumns = 12,

    [int] $AutoImageDeltaRegionRows = 8,

    [int] $AutoImageDeltaRegionPaddingCells = 1,

    [int] $TimeoutSeconds = 240,

    [ValidateRange(1, 60)]
    [int] $TemporalCaptureCount = 1,

    [ValidateRange(0, 120)]
    [int] $TemporalCaptureIntervalSeconds = 0,

    [string] $TemporalCaptureLabel = "",

    [string] $CaptureManifestJsonPath = "",

    [switch] $RejectWindowScreenshotSource,

    [string[]] $PhysicalLightingRequiredLogPattern = @(),

    [string[]] $PhysicalLightingForbiddenLogPattern = @(),

    [ValidateSet("MinecraftF2", "Window", "InClient")]
    [string] $ScreenshotSource = "MinecraftF2"
)

$ErrorActionPreference = "Stop"

function Invoke-ImageDeltaComparison {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [string] $JsonPath
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $args = @(
        "-BaselineImagePath", $BaselinePath,
        "-EnabledImagePath", $EnabledPath,
        "-RegionLeftPercent", $ImageDeltaRegionLeftPercent,
        "-RegionTopPercent", $ImageDeltaRegionTopPercent,
        "-RegionWidthPercent", $ImageDeltaRegionWidthPercent,
        "-RegionHeightPercent", $ImageDeltaRegionHeightPercent
    )
    if ($AutoImageDeltaRegion) {
        $args += @(
            "-AutoFocusRegion",
            "-AutoRegionSearchLeftPercent", $AutoImageDeltaSearchLeftPercent,
            "-AutoRegionSearchTopPercent", $AutoImageDeltaSearchTopPercent,
            "-AutoRegionSearchWidthPercent", $AutoImageDeltaSearchWidthPercent,
            "-AutoRegionSearchHeightPercent", $AutoImageDeltaSearchHeightPercent,
            "-AutoRegionColumns", $AutoImageDeltaRegionColumns,
            "-AutoRegionRows", $AutoImageDeltaRegionRows,
            "-AutoRegionPaddingCells", $AutoImageDeltaRegionPaddingCells
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($JsonPath)) {
        $args += @("-OutputJsonPath", $JsonPath)
    }

    & $compareScript @args
}

function Invoke-ImageDiagnosticsComparison {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [string] $JsonPath
    )

    $diagnosticsScript = Join-Path $PSScriptRoot "Get-LucernaVisualProofImageDiagnostics.ps1"
    if (-not (Test-Path -LiteralPath $diagnosticsScript)) {
        throw "Missing Lucerna image diagnostics helper: $diagnosticsScript"
    }

    $args = @(
        "-BaselineImagePath", $BaselinePath,
        "-EnabledImagePath", $EnabledPath,
        "-FixedRegionLeftPercent", $ImageDeltaRegionLeftPercent,
        "-FixedRegionTopPercent", $ImageDeltaRegionTopPercent,
        "-FixedRegionWidthPercent", $ImageDeltaRegionWidthPercent,
        "-FixedRegionHeightPercent", $ImageDeltaRegionHeightPercent
    )
    if ($IncludeImageBandDiagnostics) {
        $args += @("-IncludeBands")
    }
    if (-not [string]::IsNullOrWhiteSpace($JsonPath)) {
        $args += @("-OutputJsonPath", $JsonPath)
    }

    & $diagnosticsScript @args
}

function Write-LucernaConfig {
    param(
        [string] $Root,
        [bool] $RendererEnabled,
        [string] $DebugOverlay,
        [string] $CompositeMode = "FINAL_LUCERNA_COMPOSITE"
    )

    $configDir = Join-Path $Root "run\config"
    New-Item -ItemType Directory -Force -Path $configDir | Out-Null
    $configPath = Join-Path $configDir "lucerna.json"
    $config = [ordered]@{
        schemaVersion = 2
        rendererEnabled = $RendererEnabled
        qualityPreset = "BALANCED"
        debugOverlay = $DebugOverlay
        compositeMode = $CompositeMode
        showIrisNotice = $true
    }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}

function Get-Round7CaptureIntent {
    param([string] $CaptureMode)

    switch ($CaptureMode) {
        "Baseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "baseline"
                requiredPatterns = @()
            }
        }
        "RawGi" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "RAW_GI"
                artifactRole = "raw-gi"
                requiredPatterns = @(
                    "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
                    "public Mojang Round 7 RAW_GI visual render pass submitted; .*mode=ROUND7_RAW_GI.*evidence=round7\.rawGi\.nativeDiffuseGiPayload",
                    "public Mojang Round 7 RAW_GI native diffuse-GI source additive draw issued"
                )
            }
        }
        "Direct" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DIRECT_ONLY"
                artifactRole = "direct-emissive"
                requiredPatterns = @(
                    "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
                    "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*(?:selected=direct-light:ready|sourceIdentity=native-direct-light-rgba8|sourceAuthenticity=accepted:native-direct-light-surface-source))(?=[^`r`n]*mode=final-composite-native-direct-light-surface-additive)"
                )
            }
        }
        "DenoisedGi" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DENOISED_GI"
                artifactRole = "denoised-gi"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedPayloadReady=true.*readyForPreviewDraw=true",
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true.*denoised_cpu_output_generated=true",
                    "Lucerna Round 7 denoised GI CPU output: .*realDenoiseShaderOutput=false",
                    "public Mojang Round 7 DENOISED_GI visual render pass submitted; .*mode=ROUND7_DENOISED_GI.*denoisedPayloadEvidence=round7\.denoisedGi\.cpuDenoisedDiffuseGiPayload"
                )
            }
        }
        "ShaderDenoisedGi" {
            $shaderDenoiseRequiredPatterns = @(
                "(?:round7\.shaderDenoise\.dispatchPrepared|shaderDenoiseDispatchPrepared|shader_denoise_dispatch_prepared)=true",
                "(?:round7\.shaderDenoise\.outputImageReady|shaderDenoiseOutputImageReady|shader_denoise_output_image_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputMaterialReady|shaderDenoiseOutputMaterialReady|shader_denoise_output_material_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.shaderGeneratedOutput|shaderDenoiseShaderGeneratedOutput|shader_denoise_shader_generated_output|shaderGeneratedDenoiseOutput)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputAttempted|shaderDenoiseOutputAttempted|shader_denoise_output_attempted)=(?:true|false)|shader_denoise_output_attempt=(?:cpu_candidate_staged|metadata_accepted_no_candidate|not_started)",
                "(?:round7\.shaderDenoise\.outputAttemptGeneration|shaderDenoiseOutputAttemptGeneration|shader_denoise_output_attempt_generation)=[0-9]+",
                "(?:round7\.shaderDenoise\.cpuReadbackFallbackActive|shaderDenoiseCpuReadbackFallbackActive|cpuReadbackDenoiseFallbackActive|cpu_readback_denoise_fallback_active)=(?:true|false)",
                "(?:round7\.shaderDenoise\.realOutputReady|realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=false|real_shader_output(?:_ready)?=false",
                "(?:round7\.shaderDenoise\.noOverclaim|shaderDenoiseNoOverclaim|shader_denoise_no_overclaim)=true|shader_denoise_no_overclaim=true;[^`r`n]*real_shader_output(?:_ready)?=false",
                "(?:shaderDenoiseBlockerReason|shader_denoise_blocker_reason|shaderDenoiseBlocker|shader_denoise_blocker)=[A-Za-z0-9_.-]+|Lucerna native shader denoise output (?:image candidate|readiness): .*blocker=[^.`r`n]+",
                "(?:sourceIdentity|source_identity)=(?:[^`r`n]*cpu-denoised-diffuse-gi-rgba8|[^`r`n]*shader-denoised-diffuse-gi-rgba8|[^`r`n]*shader-output-image-candidate)",
                "Lucerna native shader denoise output (?:image candidate: ready=(?:true|false) size=[0-9]+x[0-9]+ pixels=[0-9]+ bytes=[0-9]+ checksum=[0-9]+ cpuStaged=(?:true|false) nonGpu=(?:true|false) realShaderGenerated=(?:true|false) realOutput=(?:true|false)|readiness: label=[^ ]+ realOutputReady=(?:true|false) outputReady=(?:true|false) imageReady=(?:true|false) materialReady=(?:true|false) shaderGenerated=(?:true|false) cpuFallback=(?:true|false) candidateReady=(?:true|false) candidateSize=[0-9]+x[0-9]+ candidatePixels=[0-9]+ candidateBytes=[0-9]+ candidateChecksum=[0-9]+ candidateCpuStaged=(?:true|false) candidateNonGpu=(?:true|false)) .*blocker=[^.`r`n]+"
            )
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DENOISED_GI"
                artifactRole = "shader-denoised-gi"
                shaderDenoiseEvidence = $true
                requiredPatterns = @(
                    "(?:shaderDenoiseIntent|shader_denoise_intent|denoiseShaderIntent|denoise_shader_intent|shaderDenoiseVisualShaderIntent)=true",
                    "(?:shaderDenoiseInputReady|shader_denoise_input_ready|shaderDenoiseRawInputReady|shader_denoise_raw_input_ready|rawGiInputReady|raw_gi_input_ready)=true|(?:rawGI|cpuDenoisedGI)=enabled-ready|(?:rawGI|cpuDenoisedGI)=ready",
                    "public Mojang Round 7 DENOISED_GI visual render pass submitted; .*mode=ROUND7_DENOISED_GI"
                ) + $shaderDenoiseRequiredPatterns
            }
        }
        "ShaderDenoiseFinalComposite" {
            $shaderDenoiseRequiredPatterns = @(
                "(?:round7\.shaderDenoise\.dispatchPrepared|shaderDenoiseDispatchPrepared|shader_denoise_dispatch_prepared)=true",
                "(?:round7\.shaderDenoise\.outputImageReady|shaderDenoiseOutputImageReady|shader_denoise_output_image_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputMaterialReady|shaderDenoiseOutputMaterialReady|shader_denoise_output_material_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.shaderGeneratedOutput|shaderDenoiseShaderGeneratedOutput|shader_denoise_shader_generated_output|shaderGeneratedDenoiseOutput)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputAttempted|shaderDenoiseOutputAttempted|shader_denoise_output_attempted)=(?:true|false)|shader_denoise_output_attempt=(?:cpu_candidate_staged|metadata_accepted_no_candidate|not_started)",
                "(?:round7\.shaderDenoise\.outputAttemptGeneration|shaderDenoiseOutputAttemptGeneration|shader_denoise_output_attempt_generation)=[0-9]+",
                "(?:round7\.shaderDenoise\.cpuReadbackFallbackActive|shaderDenoiseCpuReadbackFallbackActive|cpuReadbackDenoiseFallbackActive|cpu_readback_denoise_fallback_active)=(?:true|false)",
                "(?:round7\.shaderDenoise\.realOutputReady|realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=false|real_shader_output(?:_ready)?=false",
                "(?:round7\.shaderDenoise\.noOverclaim|shaderDenoiseNoOverclaim|shader_denoise_no_overclaim)=true|shader_denoise_no_overclaim=true;[^`r`n]*real_shader_output(?:_ready)?=false",
                "(?:shaderDenoiseBlockerReason|shader_denoise_blocker_reason|shaderDenoiseBlocker|shader_denoise_blocker)=[A-Za-z0-9_.-]+|Lucerna native shader denoise output (?:image candidate|readiness): .*blocker=[^.`r`n]+",
                "(?:sourceIdentity|source_identity)=(?:[^`r`n]*cpu-denoised-diffuse-gi-rgba8|[^`r`n]*shader-denoised-diffuse-gi-rgba8|[^`r`n]*shader-output-image-candidate)",
                "Lucerna native shader denoise output (?:image candidate: ready=(?:true|false) size=[0-9]+x[0-9]+ pixels=[0-9]+ bytes=[0-9]+ checksum=[0-9]+ cpuStaged=(?:true|false) nonGpu=(?:true|false) realShaderGenerated=(?:true|false) realOutput=(?:true|false)|readiness: label=[^ ]+ realOutputReady=(?:true|false) outputReady=(?:true|false) imageReady=(?:true|false) materialReady=(?:true|false) shaderGenerated=(?:true|false) cpuFallback=(?:true|false) candidateReady=(?:true|false) candidateSize=[0-9]+x[0-9]+ candidatePixels=[0-9]+ candidateBytes=[0-9]+ candidateChecksum=[0-9]+ candidateCpuStaged=(?:true|false) candidateNonGpu=(?:true|false)) .*blocker=[^.`r`n]+"
            )
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "shader-denoise-final-composite"
                shaderDenoiseEvidence = $true
                requiredPatterns = @(
                    "(?:shaderDenoiseIntent|shader_denoise_intent|denoiseShaderIntent|denoise_shader_intent|shaderDenoiseVisualShaderIntent)=true",
                    "(?:shaderDenoiseInputReady|shader_denoise_input_ready|shaderDenoiseRawInputReady|shader_denoise_raw_input_ready|rawGiInputReady|raw_gi_input_ready)=true|(?:rawGI|cpuDenoisedGI)=enabled-ready|(?:rawGI|cpuDenoisedGI)=ready",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
                    "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE"
                ) + $shaderDenoiseRequiredPatterns
            }
        }
        "FinalComposite" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "final-composite"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
                    "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready",
                    "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*evidence=round7\.composite\.final\.direct_raw_denoised.*finalBlendComplete=true"
                )
            }
        }
        "Debug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "DIRECT_LIGHTING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "debug"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "(?:round7\.compositeMode|debug\.overlay=DIRECT_LIGHTING|Overlay state: DIRECT_LIGHTING|Direct Lighting)"
                )
            }
        }
        "ShaderDenoiseDebug" {
            $shaderDenoiseRequiredPatterns = @(
                "(?:round7\.shaderDenoise\.dispatchPrepared|shaderDenoiseDispatchPrepared|shader_denoise_dispatch_prepared)=true",
                "(?:round7\.shaderDenoise\.outputImageReady|shaderDenoiseOutputImageReady|shader_denoise_output_image_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputMaterialReady|shaderDenoiseOutputMaterialReady|shader_denoise_output_material_ready)=(?:true|false)",
                "(?:round7\.shaderDenoise\.shaderGeneratedOutput|shaderDenoiseShaderGeneratedOutput|shader_denoise_shader_generated_output|shaderGeneratedDenoiseOutput)=(?:true|false)",
                "(?:round7\.shaderDenoise\.outputAttempted|shaderDenoiseOutputAttempted|shader_denoise_output_attempted)=(?:true|false)|shader_denoise_output_attempt=(?:cpu_candidate_staged|metadata_accepted_no_candidate|not_started)",
                "(?:round7\.shaderDenoise\.outputAttemptGeneration|shaderDenoiseOutputAttemptGeneration|shader_denoise_output_attempt_generation)=[0-9]+",
                "(?:round7\.shaderDenoise\.cpuReadbackFallbackActive|shaderDenoiseCpuReadbackFallbackActive|cpuReadbackDenoiseFallbackActive|cpu_readback_denoise_fallback_active)=(?:true|false)",
                "(?:round7\.shaderDenoise\.realOutputReady|realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=false|real_shader_output(?:_ready)?=false",
                "(?:round7\.shaderDenoise\.noOverclaim|shaderDenoiseNoOverclaim|shader_denoise_no_overclaim)=true|shader_denoise_no_overclaim=true;[^`r`n]*real_shader_output(?:_ready)?=false",
                "(?:shaderDenoiseBlockerReason|shader_denoise_blocker_reason|shaderDenoiseBlocker|shader_denoise_blocker)=[A-Za-z0-9_.-]+|Lucerna native shader denoise output (?:image candidate|readiness): .*blocker=[^.`r`n]+",
                "(?:sourceIdentity|source_identity)=(?:[^`r`n]*cpu-denoised-diffuse-gi-rgba8|[^`r`n]*shader-denoised-diffuse-gi-rgba8|[^`r`n]*shader-output-image-candidate)",
                "Lucerna native shader denoise output (?:image candidate: ready=(?:true|false) size=[0-9]+x[0-9]+ pixels=[0-9]+ bytes=[0-9]+ checksum=[0-9]+ cpuStaged=(?:true|false) nonGpu=(?:true|false) realShaderGenerated=(?:true|false) realOutput=(?:true|false)|readiness: label=[^ ]+ realOutputReady=(?:true|false) outputReady=(?:true|false) imageReady=(?:true|false) materialReady=(?:true|false) shaderGenerated=(?:true|false) cpuFallback=(?:true|false) candidateReady=(?:true|false) candidateSize=[0-9]+x[0-9]+ candidatePixels=[0-9]+ candidateBytes=[0-9]+ candidateChecksum=[0-9]+ candidateCpuStaged=(?:true|false) candidateNonGpu=(?:true|false)) .*blocker=[^.`r`n]+"
            )
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "SHADER_DENOISE_TEMPORAL"
                compositeMode = "DENOISED_GI"
                artifactRole = "shader-denoise-debug"
                shaderDenoiseEvidence = $true
                requiredPatterns = @(
                    "(?:shaderDenoiseIntent|shader_denoise_intent|denoiseShaderIntent|denoise_shader_intent|shaderDenoiseVisualShaderIntent)=true",
                    "(?:shaderDenoiseInputReady|shader_denoise_input_ready|shaderDenoiseRawInputReady|shader_denoise_raw_input_ready|rawGiInputReady|raw_gi_input_ready)=true|(?:rawGI|cpuDenoisedGI)=enabled-ready|(?:rawGI|cpuDenoisedGI)=ready"
                ) + $shaderDenoiseRequiredPatterns
            }
        }
        "Enabled" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "final-composite"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true"
                )
            }
        }
        default {
            throw "Unsupported Round 7 capture mode: $CaptureMode"
        }
    }
}

function Get-Round56PhysicalLightingCaptureIntent {
    param([string] $CaptureMode)

    $enabledPatterns = @(
        "(?:Lucerna physical lighting|lucerna\.physicalLighting|physical(?:Source|Lighting).*ready=true|firstLighting|first-lighting|physicalSurface|physical-surface|surfaceLighting|PL-A|PL-C|physical-ish|physicalish)",
        "(?:Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*|Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*|Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi))",
        "physical_gi_samples=[1-9][0-9]*.*physical_gi_hit_samples=[1-9][0-9]*",
        "surface_material_hit_coupled_samples=[1-9][0-9]*.*geometry_hit_coupled_samples=[1-9][0-9]*",
        "physical_scene_linked=true.*physical_surface_contribution=true",
        "physical_sample_marker=`"?[^`"\r\n,}]+.*surface_material_hit_marker=`"?[^`"\r\n,}]+",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*mode=(?![^`r`n]*focus-window)[^`r`n]*(?:direct|emissive|physical|gi|surface|final|composite))(?=[^`r`n]*(?:surface|world|final|composite))"
    )

    switch ($CaptureMode) {
        "Baseline" {
            return [ordered]@{
                rendererEnabled = $false
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "physical-lighting-baseline-disabled"
                requiredPatterns = @(
                    "Using graphics backend Vulkan",
                    "Lucerna backend status: SODIUM_VULKAN"
                )
            }
        }
        "Enabled" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "physical-lighting-enabled"
                requiredPatterns = @($enabledPatterns)
            }
        }
        "Debug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "FIRST_LIGHTING_PHYSICAL_PROOF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "physical-lighting-debug"
                requiredPatterns = @($enabledPatterns)
            }
        }
        default {
            throw "Unsupported Round 5/6 physical-lighting capture mode: $CaptureMode"
        }
    }
}

function Get-Round7CompositeStabilityCaptureIntent {
    param([string] $CaptureMode)

    $finalCompositePatterns = @(
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
        "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*finalBlendComplete=true",
        "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready"
    )
    $baselinePatterns = @(
        "Using graphics backend Vulkan",
        "Lucerna backend status: SODIUM_VULKAN"
    )
    switch ($CaptureMode) {
        "ParticleBaseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "particles-baseline"
                sceneKind = "particles"
                sceneState = "baseline"
                sceneAction = "particles"
                preScreenshotAction = "particles"
                requiredPatterns = @($baselinePatterns)
            }
        }
        "ParticleFinalComposite" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "particles-final-composite"
                sceneKind = "particles"
                sceneState = "final-composite"
                sceneAction = "particles"
                preScreenshotAction = "particles"
                requiredPatterns = @($finalCompositePatterns)
            }
        }
        "TranslucentBaseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "translucency-baseline"
                sceneKind = "translucency"
                sceneState = "baseline"
                sceneAction = "translucency"
                preScreenshotAction = "none"
                requiredPatterns = @($baselinePatterns)
            }
        }
        "TranslucentFinalComposite" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "translucency-final-composite"
                sceneKind = "translucency"
                sceneState = "final-composite"
                sceneAction = "translucency"
                preScreenshotAction = "none"
                requiredPatterns = @($finalCompositePatterns)
            }
        }
        "TemporalStable" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "temporal-stable-final-composite"
                sceneKind = "temporal"
                sceneState = "stable"
                sceneAction = "temporal-stable"
                preScreenshotAction = "none"
                requiredPatterns = @($finalCompositePatterns)
            }
        }
        "TemporalMoved" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "temporal-moved-final-composite"
                sceneKind = "temporal"
                sceneState = "moved-disoccluded"
                sceneAction = "temporal-moved"
                preScreenshotAction = "none"
                requiredPatterns = @($finalCompositePatterns)
            }
        }
        default {
            throw "Unsupported Round 7 composite stability capture mode: $CaptureMode"
        }
    }
}

function Get-Round7EmissiveGiSurfaceCaptureIntent {
    param([string] $CaptureMode)

    $finalCompositePatterns = @(
        "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
        "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready",
        "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*finalBlendComplete=true"
    )
    $baselinePatterns = @(
        "Using graphics backend Vulkan",
        "Lucerna backend status: SODIUM_VULKAN"
    )

    switch ($CaptureMode) {
        "Baseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "emissive-gi-surface-baseline"
                sceneAction = "emissive-gi-surface"
                hideHudForScreenshot = $true
                requiredPatterns = @($baselinePatterns)
            }
        }
        "Enabled" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "emissive-gi-surface-final-composite"
                sceneAction = "emissive-gi-surface"
                hideHudForScreenshot = $true
                requiredPatterns = @($finalCompositePatterns)
            }
        }
        "Debug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "DIRECT_LIGHTING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "emissive-gi-surface-debug"
                sceneAction = "emissive-gi-surface"
                hideHudForScreenshot = $false
                requiredPatterns = @($finalCompositePatterns) + @(
                    "(?:round7\.compositeMode|debug\.overlay=DIRECT_LIGHTING|Overlay state: DIRECT_LIGHTING|Direct Lighting)"
                )
            }
        }
        default {
            throw "Unsupported Round 7 emissive/GI surface capture mode: $CaptureMode"
        }
    }
}

function Get-Round8CaptureIntent {
    param([string] $CaptureMode)

    $rayBudgetCommonPatterns = @(
        "(?:Lucerna Round 8 adaptive ray budget: .*adaptiveRayBudget(?:Enabled)?=true|round8\.adaptiveSampling=.*enabled=true)",
        "(?:Lucerna Round 8 adaptive ray budget buckets: .*reuse(?:Only)?=[0-9]+.*low=[0-9]+.*medium=[0-9]+.*high=[0-9]+|round8\.rayBudgetBuckets=.*reuseOnly=.*low=.*medium=.*high=)",
        "(?:Lucerna Round 8 adaptive ray budget: .*cacheConfidenceContribution=.*|round8\.cacheConfidenceContribution=.*(?:value|cacheConfidence)=)",
        "(?:Lucerna Round 8 ray-budget heatmap: .*artifactRole=|round8\.rayBudgetHeatmap=.*role=(?:ray-budget|ray-budget-[a-z-]+))"
    )
    $historyCommonPatterns = @(
        "(?:Lucerna Round 8 history confidence: .*historyAccepted=[0-9]+.*historyRejected=[0-9]+|round8\.historyCounts=.*historyAccepted=.*historyRejected=)",
        "(?:Lucerna Round 8 history confidence: .*confidence(?:Map)?=.*|round8\.historyConfidence=.*value=)",
        "(?:Lucerna Round 8 history-confidence heatmap: .*artifactRole=|round8\.historyConfidenceHeatmap=.*role=(?:history-confidence|history-confidence-[a-z-]+))"
    )

    switch ($CaptureMode) {
        "StableHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-stable"
                heatmapKind = "ray-budget"
                sceneState = "stable"
                sceneAction = "stationary"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*(?:reuse(?:Only)?|low)=[1-9][0-9]*|round8\.rayBudgetBuckets=.*(?:reuseOnly|low)=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*sceneState=stable|round8\.sceneState=.*sceneState: stable)"
                )
            }
        }
        "MovedHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-moved"
                heatmapKind = "ray-budget"
                sceneState = "moved-noisy"
                sceneAction = "moved"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*high=[1-9][0-9]*|round8\.rayBudgetBuckets=.*high=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*sceneState=(?:moved|noisy|moved-noisy)|round8\.sceneState=.*sceneState: (?:moved|noisy|moved-noisy))"
                )
            }
        }
        "EmissiveHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-emissive"
                heatmapKind = "ray-budget"
                sceneState = "emissive"
                sceneAction = "emissive"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*high=[1-9][0-9]*|round8\.rayBudgetBuckets=.*high=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*emissive(?:Contribution|Proximity|Regions)=[1-9][0-9]*|round8\.sceneState=.*sceneState: emissive)"
                )
            }
        }
        "HistoryStable" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "HISTORY_CONFIDENCE"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "history-confidence-stable"
                heatmapKind = "history-confidence"
                sceneState = "stable"
                sceneAction = "stationary"
                requiredPatterns = @($historyCommonPatterns) + @(
                    "(?:Lucerna Round 8 history confidence: .*historyAccepted=[1-9][0-9]*|round8\.historyCounts=.*historyAccepted=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 history confidence: .*sceneState=stable|round8\.sceneState=.*sceneState: stable)"
                )
            }
        }
        "HistoryMoved" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "HISTORY_CONFIDENCE"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "history-confidence-moved"
                heatmapKind = "history-confidence"
                sceneState = "moved-disoccluded"
                sceneAction = "moved"
                requiredPatterns = @($historyCommonPatterns) + @(
                    "(?:Lucerna Round 8 history confidence: .*historyRejected=[1-9][0-9]*|round8\.historyCounts=.*historyRejected=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 history confidence: .*sceneState=(?:moved|disoccluded|moved-disoccluded)|round8\.sceneState=.*sceneState: (?:moved|disoccluded|moved-disoccluded))"
                )
            }
        }
        default {
            throw "Unsupported Round 8 capture mode: $CaptureMode"
        }
    }
}

function Get-Round9CaptureIntent {
    param([string] $CaptureMode)

    $clusterCommonPatterns = @(
        "(?:Lucerna Round 9 virtualized chunk geometry|round9\.virtualized(?:Chunk)?Geometry|round9\.chunkClusters)",
        "(?:cluster(?:Count|s)?|clusters(?:Total)?|cluster_count)=[1-9][0-9]*",
        "(?:visibleCluster(?:Count|s)?|visible_clusters|visible_cluster_count)=[0-9]+",
        "(?:upload(?:Bytes|_bytes)|clusterUploadBytes|upload_byte_estimate|total_upload_byte_estimate)=[1-9][0-9]*",
        "(?:generation(?:Counter|s)?|generation_counter|clusterGeneration|geometryGeneration)=[1-9][0-9]*"
    )
    $cullingCommonPatterns = @(
        "(?:Lucerna Round 9 chunk culling|round9\.chunkCulling|virtualized culling)",
        "(?:visibleCluster(?:Count|s)?|visible_clusters|visible_cluster_count)=[0-9]+",
        "(?:(?:culled|offscreen)Cluster(?:Count|s)?|culled_clusters|offscreen_clusters|culled_cluster_count)=[0-9]+",
        "(?:indirectDraw(?:Count|s|Placeholder)?|indirect_draw(?:_count|_count_placeholder|_candidate_count)?|drawList(?:Count)?)=[0-9]+",
        "(?:actualGpuCullingExecuted|realGpuCullingExecuted|gpu_culling_executed|round9\.actualGpuCullingExecuted|round9\.gpu_culling_executed)=(?:true|false)",
        "(?:gpuCullingPrerequisitesReady|gpuPrerequisitesReady|gpu_prerequisites_ready|gpu_culling_prerequisites_ready|round9\.gpuCullingPrerequisitesReady|round9\.gpu_culling_prerequisites_ready)=(?:true|false)",
        "(?:gpuCullingBlockerReason|gpu_culling_blocker_reason|round9\.gpuCullingBlockerReason)=",
        "(?:frustumCandidate(?:Count|s)?|frustum_candidate_count|frustum_candidates|frustum_culling_candidate_count|round9\.frustumCandidates|round9\.frustum_candidate_count)=[0-9]+",
        "(?:occlusion(?:Candidate|Placeholder|Ready)(?:Count|s)?|occlusion_candidate_count|occlusion_placeholder_count|occlusion_candidates|occlusion_culling_candidate_count|occlusion_culling_placeholder_count|round9\.occlusionCandidates|round9\.occlusionPlaceholderCount|round9\.occlusion_candidate_count|round9\.occlusion_placeholder_count)=[0-9]+",
        "(?:indirectDrawReady|indirect_draw_ready|gpuIndirectDrawReady|round9\.indirectDrawReady|round9\.indirect_draw_ready)=(?:true|false)",
        "(?:cpuFrameTime(?:Ms)?(?:Placeholder)?|gpuFrameTime(?:Ms)?(?:Placeholder)?|cpu_frame_time_ms_placeholder|gpu_frame_time_ms_placeholder|frameTiming(?:Ready|Present|Marker)|round9\.cpuFrameTimeMs|round9\.gpuFrameTimeMs|round9\.frameTiming(?:Ready|Present|Marker))="
    )

    switch ($CaptureMode) {
        "FlatClusterOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "flat-open-cluster-overlay"
                sceneKind = "flat-open-terrain"
                sceneAction = "flat-open"
                requiredPatterns = @($clusterCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:flat-open-terrain|flat|open-terrain)",
                    "(?:clusterOverlay|chunkClusterOverlay|round9\.clusterOverlay)(?:Visible|Submitted|Enabled)?=true|round9ArtifactRole=flat-open-cluster-overlay"
                )
            }
        }
        "InteriorCullingOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "interior-wall-culling-overlay"
                sceneKind = "interior-wall-facing"
                sceneAction = "wall-facing"
                requiredPatterns = @($clusterCommonPatterns) + @($cullingCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:interior-wall-facing|wall-facing|interior)"
                )
            }
        }
        "HighDistanceCullingOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "high-distance-open-terrain-culling-overlay"
                sceneKind = "high-render-distance-open-terrain"
                sceneAction = "high-distance-open"
                requiredPatterns = @($clusterCommonPatterns) + @($cullingCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:high-render-distance-open-terrain|high-distance-open|open-terrain)"
                )
            }
        }
        "ForestComplexCullingOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "forest-complex-culling-overlay"
                sceneKind = "forest-complex-area"
                sceneAction = "forest-complex"
                requiredPatterns = @($clusterCommonPatterns) + @($cullingCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:forest-complex-area|forest-complex|complex-area)",
                    "(?:clusterDensityBucket|complexityBucket|round9\.clusterDensityBucket)=(?:medium|high|complex|forest)"
                )
            }
        }
        default {
            throw "Unsupported Round 9 capture mode: $CaptureMode"
        }
    }
}

function Get-Round10CaptureIntent {
    param([string] $CaptureMode)

    $voxelTraversalCommonPatterns = @(
        "(?:Lucerna Round 10 voxel traversal|round10\.voxelTraversal|round10\.voxelRayDebug)",
        "(?:voxelRay(?:Count|s)?|voxel_ray_count|round10\.voxelRays)=([1-9][0-9]*)",
        "(?:voxelHit(?:Count|s)?|voxel_hit_count|round10\.voxelHits)=([0-9]+)",
        "(?:voxelMiss(?:Count|s)?|voxel_miss_count|round10\.voxelMisses)=([0-9]+)",
        "(?:traversal(?:Step|Steps|StepCount)|averageTraversalSteps|avg_traversal_steps|round10\.traversalSteps)=([1-9][0-9]*(?:\.[0-9]+)?)",
        "(?:skippedSection(?:Count|s)?|skipped_sections|round10\.skippedSections)=([0-9]+)",
        "(?:wallHit(?:Count|s)?|wall_hit_count|round10\.wallHit(?:Count|s)?)=([1-9][0-9]*)",
        "(?:openSkyMiss(?:Count|s)?|open_sky_miss_count|round10\.openSkyMiss(?:Count|s)?)=([1-9][0-9]*)",
        "(?:glassWater|glassOrWater|transparentMaterial)(?:Hit(?:Count|s)?|Hits)=([1-9][0-9]*)",
        "(?:opaqueMaterialHit(?:Count|s)?|opaque_material_hit_count|round10\.opaqueMaterialHits)=([1-9][0-9]*)",
        "(?:materialIdConsistency(?:Ready|Passed)?|material_id_consistency(?:_ready|_passed)?|round10\.materialIdConsistency(?:Ready|Passed)?)=true",
        "(?:materialLookupReady|material_lookup_ready|round10\.materialLookupReady)=true",
        "(?:maskBitsReady|mask_bits_ready|round10\.maskBitsReady)=true",
        "(?:maskBitsSource|mask_bits_source|round10\.maskBitsSource)=",
        "(?:emptySectionSkipSafe|empty_section_skip_safe|round10\.emptySectionSkipSafe)=true",
        "(?:sectionLifecycle(?:Marker|Ready|Observed)?|section_lifecycle(?:_marker|_ready|_observed)?|round10\.sectionLifecycle(?:Marker|Ready|Observed)?)=true|(?:sectionLifecycleCount|section_lifecycle_count|section_lifecycle_marker_count|round10\.sectionLifecycleCount)=([1-9][0-9]*)",
        "(?:worldLeaveSeen|world_leave_seen|round10\.worldLeaveSeen|shutdownSafe|shutdown_safe|round10\.shutdownSafe)=(?:true|false)",
        "(?:traversalBackend|traversal_backend|round10\.traversalBackend)=",
        "(?:realGpuTraversalExecuted|real_gpu_traversal_executed|round10\.realGpuTraversalExecuted)=false|(?:gpuTraversalBoundary|gpu_traversal_boundary)="
    )
    $rtCommonPatterns = @(
        "(?:Lucerna Round 10 RT entity|round10\.rtEntityDebug|round10\.rtEntities|Vulkan RT)",
        "(?:BLAS|blas)(?:Status|Ready|Builds|BuildCount)?=",
        "(?:TLAS|tlas)(?:Status|Ready|Builds|BuildCount)?=",
        "(?:rtFallback(?:Status|Active)?|fallbackStatus|nonRtFallback)=",
        "(?:hardwareRtExecutionProven|hardware_rt_execution_proven|round10\.hardwareRtExecutionProven)=(?:true|false)"
    )
    $hybridCommonPatterns = @(
        "(?:Lucerna Round 10 hybrid hit|round10\.hybridHitDebug|round10\.hybridHits)",
        "(?:hybridHit(?:Count|s)?|hybrid_hit_count|round10\.hybridHits)=([1-9][0-9]*)",
        "(?:voxelHybridHit(?:Count|s)?|hybridVoxelHits|hybrid_source_voxel)=([0-9]+)",
        "(?:rtHybridHit(?:Count|s)?|hybridRtHits|hybrid_source_rt)=([0-9]+)",
        "(?:screenSpaceHybridHit(?:Count|s)?|hybridScreenSpaceHits|hybrid_source_screen)=([0-9]+)",
        "(?:entityMovement(?:Marker|Ready|Observed)?|entity_movement(?:_marker|_ready|_observed)?|round10\.entityMovement(?:Marker|Ready|Observed)?)=true",
        "(?:entityMovementCount|entity_movement_count|entity_movement_marker_count|round10\.entityMovementCount)=([1-9][0-9]*)",
        "(?:chunkChurn(?:Marker|Ready|Observed)?|chunk_churn(?:_marker|_ready|_observed)?|round10\.chunkChurn(?:Marker|Ready|Observed)?)=true",
        "(?:chunkChurnCount|chunk_churn_count|chunk_churn_marker_count|round10\.chunkChurnCount)=([1-9][0-9]*)",
        "(?:srcStable|sourceStable|selectedSourceStable|source_stable|selected_source_stability|round10\.sourceStability)=(?:true|stable|selected|consistent)",
        "(?:chunkChurnMaterialConsistent|chunk_churn_material_consistent|materialConsistentDuringChunkChurn|material_consistent_during_chunk_churn|round10\.chunkChurnMaterialConsistent)=true",
        "(?:entityMoveMaterialConsistent|entity_move_material_consistent|materialConsistentDuringEntityMovement|material_consistent_during_entity_movement|round10\.entityMoveMaterialConsistent)=true",
        "(?:realTracedLightingConsumed|real_traced_lighting_consumed|traced_lighting_consumed|round10\.realTracedLightingConsumed)=false",
        "(?:tracedLightingNoOverclaim|traced_lighting_no_overclaim|round10\.tracedLightingNoOverclaim)=true|realTracedLightingConsumed=false[^`r`n]*(?:open|boundary|not[-_ ]?consumed|no[-_ ]?overclaim)"
    )

    switch ($CaptureMode) {
        "VoxelRayDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "VOXEL_RAY_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "voxel-ray-debug"
                sceneKind = "round10-tracing-debug"
                sceneAction = "voxel-rays"
                requiredPatterns = @($voxelTraversalCommonPatterns) + @(
                    "(?:voxelRayDebug(?:Visible|Submitted|Enabled)?=true|round10ArtifactRole=voxel-ray-debug|artifactRole=voxel-ray-debug)",
                    "(?:round10\.scene=.*wall|wallSceneMarker=true)",
                    "(?:round10\.scene=.*open-sky|openSkySceneMarker=true)",
                    "(?:round10\.scene=.*glass-water|glassWaterSceneMarker=true)"
                )
            }
        }
        "RtEntityDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RT_ENTITY_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "rt-entity-debug"
                sceneKind = "round10-rt-entity-debug"
                sceneAction = "rt-entities"
                requiredPatterns = @($rtCommonPatterns) + @(
                    "(?:rtEntityDebug(?:Visible|Submitted|Enabled)?=true|round10ArtifactRole=rt-entity-debug|artifactRole=rt-entity-debug)",
                    "(?:entityMovement(?:Marker|Ready|Observed)?|round10\.entityMovement(?:Marker|Ready|Observed)?)=true"
                )
            }
        }
        "HybridHitDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "HYBRID_HIT_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "hybrid-hit-debug"
                sceneKind = "round10-hybrid-hit-debug"
                sceneAction = "hybrid-hits"
                requiredPatterns = @($voxelTraversalCommonPatterns) + @($rtCommonPatterns) + @($hybridCommonPatterns) + @(
                    "(?:hybridHitDebug(?:Visible|Submitted|Enabled)?=true|round10ArtifactRole=hybrid-hit-debug|artifactRole=hybrid-hit-debug)",
                    "(?:chunkChurn(?:Marker|Ready|Observed)?|round10\.chunkChurn(?:Marker|Ready|Observed)?)=true"
                )
            }
        }
        default {
            throw "Unsupported Round 10 capture mode: $CaptureMode"
        }
    }
}

function Get-Round11CaptureIntent {
    param([string] $CaptureMode)

    $round11CommonPatterns = @(
        "(?:Lucerna Round 11|round11\.|ReSTIR|RESTIR|reservoir)"
    )
    $reservoirCountPatterns = @(
        "(?:reservoir(?:Count|s)?|reservoir_count|round11\.reservoirCount|round11\.reservoirs)=([1-9][0-9]*)"
    )
    $candidateCountPatterns = @(
        "(?:candidate(?:Count|s)?|candidate_count|selectedCandidateCount|selected_candidate_count|round11\.candidateCount)=([1-9][0-9]*)"
    )
    $temporalReusePatterns = @(
        "(?:temporalReuse(?:Count|Accepted)?|temporal_reuse(?:_count|_accepted)?|round11\.temporalReuse)=([0-9]+)"
    )
    $spatialReusePatterns = @(
        "(?:spatialReuse(?:Count|Accepted)?|spatial_reuse(?:_count|_accepted)?|round11\.spatialReuse)=([0-9]+)"
    )
    $pathReusePatterns = @(
        "(?:pathReuse(?:Count|Accepted)?|path_reuse(?:_count|_accepted)?|giPathReuseCount|round11\.pathReuse)=([0-9]+)"
    )
    $invalidationPatterns = @(
        "(?:invalidation(?:Count|s)?|invalidation_count|invalidatedReservoirs|invalidated_reservoirs|round11\.invalidation)=([0-9]+)"
    )
    $confidencePatterns = @(
        "(?:confidence|minConfidence|maxConfidence|meanConfidence|combinedConfidence|reservoir_confidence|round11\.confidence)="
    )
    $restirDirectExecutionPatterns = @(
        "(?:Lucerna Round 11 ReSTIR DI execution|round11\.(?:restirDi|restirDI|directExecution|execution).*?(?:executed|enabled|ready)=(?:true|1)|restirDirectExecution(?:Ready|Enabled|Executed)?=(?:true|1)|realRestirDiExecution=(?:true|1)|restirDiExecutionPresent=(?:true|1))"
    )
    $selectedCountPatterns = @(
        "(?:selected(?:Candidate)?(?:Count|s)?|selected_candidate_count|round11\.selected(?:Candidate)?Count|round11\.(?:restirDi|directReservoir).*selected)=([1-9][0-9]*)"
    )
    $candidateReductionPatterns = @(
        "(?:candidateReductionRatio|candidate_reduction_ratio|round11\.candidateReductionRatio)=([1-9][0-9]*(?:\.[0-9]+)?|[0-9]+\.[0-9]*[1-9][0-9]*)"
    )
    $outputEnergyPatterns = @(
        "(?:restir(?:Direct|Di|DI)?OutputEnergy|outputEnergy|cpuOutputEnergy|round11\.(?:restirDi|directOutput).*energy)=([1-9][0-9.eE+-]*)"
    )
    $outputChecksumPatterns = @(
        "(?:restir(?:Direct|Di|DI)?OutputChecksum|outputChecksum|cpuOutputChecksum|round11\.(?:restirDi|directOutput).*checksum)=([1-9][0-9]*)"
    )

    switch ($CaptureMode) {
        "DirectReservoirDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "DIRECT_RESERVOIR_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "direct-reservoir-debug"
                sceneKind = "round11-restir-direct"
                requiredPatterns = @($round11CommonPatterns) + @($reservoirCountPatterns) + @($candidateCountPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($confidencePatterns) + @(
                    "(?:directReservoirDebug(?:Visible|Submitted|Enabled)?=true|round11ArtifactRole=direct-reservoir-debug|artifactRole=direct-reservoir-debug|debug\.overlay=DIRECT_RESERVOIR_DEBUG|Overlay state: DIRECT_RESERVOIR_DEBUG)"
                )
            }
        }
        "GiReservoirDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "GI_RESERVOIR_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "gi-reservoir-debug"
                sceneKind = "round11-restir-gi"
                requiredPatterns = @($round11CommonPatterns) + @($reservoirCountPatterns) + @($candidateCountPatterns) + @($pathReusePatterns) + @($invalidationPatterns) + @($confidencePatterns) + @(
                    "(?:giReservoirDebug(?:Visible|Submitted|Enabled)?=true|round11ArtifactRole=gi-reservoir-debug|artifactRole=gi-reservoir-debug|debug\.overlay=GI_RESERVOIR_DEBUG|Overlay state: GI_RESERVOIR_DEBUG)"
                )
            }
        }
        "ReservoirReuseDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RESERVOIR_REUSE_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "reservoir-reuse-debug"
                sceneKind = "round11-restir-reuse"
                requiredPatterns = @($round11CommonPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($pathReusePatterns) + @($invalidationPatterns) + @($confidencePatterns) + @(
                    "(?:reservoirReuseDebug(?:Visible|Submitted|Enabled)?=true|round11ArtifactRole=reservoir-reuse-debug|artifactRole=reservoir-reuse-debug|debug\.overlay=RESERVOIR_REUSE_DEBUG|Overlay state: RESERVOIR_REUSE_DEBUG)"
                )
            }
        }
        "DirectBruteBaseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DIRECT_ONLY"
                artifactRole = "direct-brute-baseline"
                sceneKind = "round11-restir-direct"
                sceneState = "direct-brute-baseline"
                sceneAction = "direct-baseline"
                requiredPatterns = @()
            }
        }
        "RestirDirectEnabled" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "restir-direct-enabled"
                sceneKind = "round11-restir-direct"
                sceneState = "restir-direct-enabled"
                sceneAction = "direct-enabled"
                requiredPatterns = @($round11CommonPatterns) + @($restirDirectExecutionPatterns) + @($reservoirCountPatterns) + @($candidateCountPatterns) + @($selectedCountPatterns) + @($candidateReductionPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($outputEnergyPatterns) + @($outputChecksumPatterns)
            }
        }
        "RestirTemporalStable" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "restir-temporal-stable"
                sceneKind = "round11-restir-temporal"
                sceneState = "stable"
                sceneAction = "temporal-stable"
                requiredPatterns = @($round11CommonPatterns) + @($restirDirectExecutionPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($outputEnergyPatterns) + @($outputChecksumPatterns)
            }
        }
        "RestirTemporalMoved" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "restir-temporal-moved"
                sceneKind = "round11-restir-temporal"
                sceneState = "moved-disoccluded"
                sceneAction = "temporal-moved"
                requiredPatterns = @($round11CommonPatterns) + @($restirDirectExecutionPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($invalidationPatterns) + @($outputEnergyPatterns) + @($outputChecksumPatterns)
            }
        }
        "RestirExecutionDebug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RESERVOIR_REUSE_DEBUG"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "restir-execution-debug"
                sceneKind = "round11-restir-execution"
                sceneState = "execution-debug"
                sceneAction = "execution-debug"
                requiredPatterns = @($round11CommonPatterns) + @($restirDirectExecutionPatterns) + @($reservoirCountPatterns) + @($candidateCountPatterns) + @($selectedCountPatterns) + @($candidateReductionPatterns) + @($temporalReusePatterns) + @($spatialReusePatterns) + @($outputEnergyPatterns) + @($outputChecksumPatterns) + @(
                    "(?:restirExecutionDebug(?:Visible|Submitted|Enabled)?=true|round11ArtifactRole=restir-execution-debug|artifactRole=restir-execution-debug|debug\.overlay=RESERVOIR_REUSE_DEBUG|Overlay state: RESERVOIR_REUSE_DEBUG)"
                )
            }
        }
        default {
            throw "Unsupported Round 11 capture mode: $CaptureMode"
        }
    }
}

function Wait-LatestLogPattern {
    param(
        [string] $LogPath,
        [string[]] $RequiredPatterns,
        [datetime] $Deadline,
        [string[]] $EarlyFailureLogPaths = @(),
        [string[]] $ForbiddenPatterns = @()
    )

    $earlyFailurePatterns = @(
        "Lucerna native library is not available yet",
        "Application Control policy has blocked this file"
    )
    $pathsToScan = @($LogPath) + @($EarlyFailureLogPaths)

    while ((Get-Date) -lt $Deadline) {
        foreach ($path in $pathsToScan) {
            if ([string]::IsNullOrWhiteSpace($path) -or -not (Test-Path -LiteralPath $path)) {
                continue
            }
            try {
                $candidateLog = Get-Content -Raw -LiteralPath $path
            } catch {
                continue
            }
            foreach ($pattern in $earlyFailurePatterns) {
                if ($candidateLog -match $pattern) {
                    throw "Lucerna visual proof is blocked before required markers were observed. Matched native-load failure marker '$pattern' in $path. For Round6NativeDiffuseGi, this means Windows Application Control/native DLL loading must be resolved before the controller can validate native diffuse-GI output-source replacement; do not count the temporary direct-light RGBA preview path as this proof."
                }
            }
            foreach ($pattern in $ForbiddenPatterns) {
                if ($candidateLog -match $pattern) {
                    throw "Lucerna visual proof is contaminated before required markers were observed. Matched forbidden marker '$pattern' in $path. Capture the requested validation profile without proof-marker overlays, temporary direct-light payload sources, focus-window-only preview modes, or invalid validation markers."
                }
            }
        }

        if (Test-Path -LiteralPath $LogPath) {
            try {
                $log = Get-Content -Raw -LiteralPath $LogPath
            } catch {
                Start-Sleep -Milliseconds 500
                continue
            }
            $allPresent = $true
            foreach ($pattern in $RequiredPatterns) {
                if ($log -notmatch $pattern) {
                    $allPresent = $false
                    break
                }
            }
            foreach ($pattern in $ForbiddenPatterns) {
                if ($log -match $pattern) {
                    throw "Lucerna visual proof is contaminated. Matched forbidden marker '$pattern' in $LogPath. Capture the requested validation profile without proof-marker overlays, temporary direct-light payload sources, focus-window-only preview modes, or invalid validation markers."
                }
            }
            if ($allPresent) {
                return
            }
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for required log markers: $($RequiredPatterns -join '; ')"
}

function Get-JavaProcessCommandLine {
    param([int] $ProcessId)

    try {
        $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
        if ($processInfo -and $processInfo.CommandLine) {
            return [string]$processInfo.CommandLine
        }
    } catch {
        return ""
    }
    return ""
}

function Test-MinecraftClientWindowProcess {
    param([System.Diagnostics.Process] $Process)

    if ($null -eq $Process -or $Process.MainWindowHandle -eq 0) {
        return $false
    }
    if ($script:LucernaMinecraftLaunchStart) {
        try {
            if ($Process.StartTime -lt $script:LucernaMinecraftLaunchStart.AddSeconds(-5)) {
                return $false
            }
        } catch {
            return $false
        }
    }

    $title = [string]$Process.MainWindowTitle
    $commandLine = Get-JavaProcessCommandLine $Process.Id
    $titleLooksMinecraft = $title -like "*Minecraft*" -and $title -notmatch "(?i)(serena|codex)"
    $commandLooksMinecraft = $commandLine -match "(?i)(net\.fabricmc|devlaunchinjector|fabric-loader|com\.mojang)"

    return $titleLooksMinecraft -and $commandLooksMinecraft
}

function Get-MinecraftWindowProcess {
    Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object {
        Test-MinecraftClientWindowProcess $_
    } | Sort-Object @{ Expression = {
                try {
                    $_.StartTime
                } catch {
                    [datetime]::MinValue
                }
            }; Descending = $true } | Select-Object -First 1
}

function Focus-MinecraftWindow {
    $windowProcess = Get-MinecraftWindowProcess
    if ($null -eq $windowProcess) {
        throw "Could not find a Minecraft/java window to focus."
    }

    if (-not ("LucernaVisualProof.WindowFocus" -as [type])) {
        Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

namespace LucernaVisualProof {
    public static class WindowFocus {
        [DllImport("user32.dll")]
        public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        public static extern bool SetForegroundWindow(IntPtr hWnd);
    }
}
"@
    }

    [void] [LucernaVisualProof.WindowFocus]::ShowWindow($windowProcess.MainWindowHandle, 9)
    [void] [LucernaVisualProof.WindowFocus]::SetForegroundWindow($windowProcess.MainWindowHandle)
    $shell = New-Object -ComObject WScript.Shell
    [void] $shell.AppActivate($windowProcess.Id)
    Start-Sleep -Milliseconds 500
    return $windowProcess
}

function Send-MinecraftKeys {
    param([string] $Keys)

    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.SendKeys]::SendWait($Keys)
    Start-Sleep -Milliseconds 250
}

function Send-MinecraftChatCommand {
    param([string] $Command)

    Focus-MinecraftWindow | Out-Null
    Set-Clipboard -Value $Command
    Send-MinecraftKeys "t"
    Send-MinecraftKeys "^v"
    Send-MinecraftKeys "{ENTER}"
    Start-Sleep -Milliseconds 750
}

function Clear-MinecraftChat {
    Focus-MinecraftWindow | Out-Null
    Send-MinecraftKeys "{ESC}"
    if (-not ("LucernaValidationKeyboard" -as [type])) {
        Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class LucernaValidationKeyboard {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, UIntPtr dwExtraInfo);
}
"@
    }

    $keyUp = 0x0002
    [LucernaValidationKeyboard]::keybd_event(0x72, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x44, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x44, 0, $keyUp, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x72, 0, $keyUp, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    Send-MinecraftKeys "{ESC}"
    Start-Sleep -Milliseconds 500
}

function Invoke-OptionalSceneSetup {
    if (-not $SetupScene) {
        return
    }

    $commands = @(
        "/gamerule sendCommandFeedback false",
        "/gamerule doDaylightCycle false",
        "/gamemode creative",
        "/time set 18000",
        "/weather clear",
        "/kill @e[type=!player,distance=..32]",
        "/fill ~4 ~-1 ~-3 ~4 ~3 ~3 minecraft:smooth_stone",
        "/setblock ~3 ~ ~ minecraft:glowstone",
        "/time set 18000",
        "/tp @s ~ ~ ~ -90 0"
    )
    foreach ($command in $commands) {
        Send-MinecraftChatCommand $command
    }
}

function Add-LucernaControllerMarker {
    param(
        [string] $Path,
        [string] $Message
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }
    Add-Content -LiteralPath $Path -Value ("[Lucerna controller proof] " + $Message)
}

function Invoke-Round7CompositeStabilitySceneAction {
    param(
        [string] $SceneAction,
        [string] $MarkerPath
    )

    Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
    Send-MinecraftChatCommand "/gamemode creative"
    Send-MinecraftChatCommand "/weather clear"
    Send-MinecraftChatCommand "/time set 18000"

    switch ($SceneAction) {
        "particles" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~2 ~-1 ~-4 ~6 ~4 ~4 minecraft:smooth_stone"
                Send-MinecraftChatCommand "/setblock ~3 ~ ~ minecraft:glowstone"
                Send-MinecraftChatCommand "/setblock ~3 ~-1 ~1 minecraft:campfire[lit=true]"
                Send-MinecraftChatCommand "/setblock ~3 ~ ~2 minecraft:soul_campfire[lit=true]"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round7.stability.scene=particles particleSceneMarker=true finalCompositeStabilityScene=true"
        }
        "translucency" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~2 ~-1 ~-4 ~7 ~4 ~4 minecraft:smooth_stone"
                Send-MinecraftChatCommand "/fill ~4 ~ ~-2 ~4 ~2 ~2 minecraft:glass"
                Send-MinecraftChatCommand "/fill ~5 ~ ~-1 ~5 ~1 ~1 minecraft:water"
                Send-MinecraftChatCommand "/setblock ~3 ~ ~ minecraft:glowstone"
                Send-MinecraftChatCommand "/setblock ~6 ~ ~ minecraft:tinted_glass"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round7.stability.scene=translucency translucentSceneMarker=true glassWaterSceneMarker=true finalCompositeStabilityScene=true"
        }
        "temporal-stable" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~2 ~-1 ~-5 ~8 ~4 ~5 minecraft:smooth_stone"
                Send-MinecraftChatCommand "/fill ~5 ~ ~-4 ~5 ~3 ~4 minecraft:air"
                Send-MinecraftChatCommand "/fill ~6 ~ ~-4 ~6 ~3 ~-2 minecraft:blue_concrete"
                Send-MinecraftChatCommand "/fill ~6 ~ ~2 ~6 ~3 ~4 minecraft:red_concrete"
                Send-MinecraftChatCommand "/fill ~7 ~ ~-1 ~7 ~2 ~1 minecraft:lime_concrete"
                Send-MinecraftChatCommand "/setblock ~3 ~1 ~ minecraft:glowstone"
                Send-MinecraftChatCommand "/setblock ~4 ~ ~3 minecraft:sea_lantern"
                Send-MinecraftChatCommand "/setblock ~4 ~ ~-3 minecraft:shroomlight"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Start-Sleep -Seconds 5
            Add-LucernaControllerMarker $MarkerPath "round7.stability.scene=temporal sceneState=stable temporalSceneMarker=true historyStableSceneMarker=true finalCompositeStabilityScene=true temporalAsymmetricScene=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        "temporal-moved" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~2 ~-1 ~-5 ~8 ~4 ~5 minecraft:smooth_stone"
                Send-MinecraftChatCommand "/fill ~5 ~ ~-4 ~5 ~3 ~4 minecraft:air"
                Send-MinecraftChatCommand "/fill ~6 ~ ~-4 ~6 ~3 ~-2 minecraft:blue_concrete"
                Send-MinecraftChatCommand "/fill ~6 ~ ~2 ~6 ~3 ~4 minecraft:red_concrete"
                Send-MinecraftChatCommand "/fill ~7 ~ ~-1 ~7 ~2 ~1 minecraft:lime_concrete"
                Send-MinecraftChatCommand "/setblock ~3 ~1 ~ minecraft:glowstone"
                Send-MinecraftChatCommand "/setblock ~4 ~ ~3 minecraft:sea_lantern"
                Send-MinecraftChatCommand "/setblock ~4 ~ ~-3 minecraft:shroomlight"
            }
            Send-MinecraftChatCommand "/tp @s ~-1 ~ ~1 -70 1"
            Start-Sleep -Milliseconds 750
            Send-MinecraftChatCommand "/tp @s ~2 ~ ~-2 -125 7"
            Start-Sleep -Seconds 3
            Add-LucernaControllerMarker $MarkerPath "round7.stability.scene=temporal sceneState=moved-disoccluded temporalSceneMarker=true historyMovedSceneMarker=true movedCameraTemporalPair=true finalCompositeStabilityScene=true temporalAsymmetricScene=true temporalCameraTranslation=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        default {
            throw "Unsupported Round 7 composite stability scene action: $SceneAction"
        }
    }
}

function Invoke-Round7CompositeStabilityPreScreenshotAction {
    param(
        [string] $PreScreenshotAction,
        [string] $MarkerPath
    )

    if ($PreScreenshotAction -ne "particles") {
        return
    }

    Send-MinecraftChatCommand "/particle minecraft:flame ~3 ~1 ~ 0.55 0.45 0.55 0.02 160 force @s"
    Send-MinecraftChatCommand "/particle minecraft:smoke ~3 ~1 ~1 0.75 0.7 0.75 0.01 140 force @s"
    Add-LucernaControllerMarker $MarkerPath "round7.stability.particleBurst=true particleSceneMarker=true"
}

function Invoke-Round7EmissiveGiSurfaceSceneAction {
    param([string] $MarkerPath)

    Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
    Send-MinecraftChatCommand "/gamemode creative"
    Send-MinecraftChatCommand "/weather clear"
    Send-MinecraftChatCommand "/time set 18000"

    if ($SetupScene) {
        Send-MinecraftChatCommand "/kill @e[type=!player,distance=..32]"
        Send-MinecraftChatCommand "/fill ~4 ~-1 ~-4 ~4 ~4 ~4 minecraft:smooth_stone"
        Send-MinecraftChatCommand "/fill ~3 ~-1 ~-4 ~7 ~-1 ~4 minecraft:smooth_stone"
        Send-MinecraftChatCommand "/setblock ~4 ~1 ~ minecraft:glowstone"
        Send-MinecraftChatCommand "/setblock ~4 ~0 ~1 minecraft:orange_concrete"
        Send-MinecraftChatCommand "/setblock ~4 ~0 ~-1 minecraft:blue_concrete"
        Send-MinecraftChatCommand "/setblock ~3 ~0 ~2 minecraft:redstone_lamp[lit=true]"
    }

    Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
    Add-LucernaControllerMarker $MarkerPath "round7.emissiveGiSurface.scene=locked-wall surfaceProofScene=true handHudExcludedRegion=true fixedWorldSurfaceRegion=true"
}

function Invoke-Round9SceneAction {
    param(
        [string] $SceneAction,
        [string] $MarkerPath = ""
    )

    switch ($SceneAction) {
        "flat-open" {
            Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
            Send-MinecraftChatCommand "/gamemode creative"
            Send-MinecraftChatCommand "/time set 6000"
            Send-MinecraftChatCommand "/weather clear"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ 0 18"
            if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) {
                Add-LucernaControllerMarker $MarkerPath "round9.scene=flat-open-terrain round9SceneKind=flat-open-terrain sceneKind=flat-open-terrain clusterDensityBucket=open complexityBucket=open"
            }
        }
        "wall-facing" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~4 ~-1 ~-4 ~4 ~4 ~4 minecraft:smooth_stone"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) {
                Add-LucernaControllerMarker $MarkerPath "round9.scene=interior-wall-facing round9SceneKind=interior-wall-facing sceneKind=interior-wall-facing clusterDensityBucket=interior complexityBucket=medium"
            }
        }
        "high-distance-open" {
            Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
            Send-MinecraftChatCommand "/gamemode creative"
            Send-MinecraftChatCommand "/time set 6000"
            Send-MinecraftChatCommand "/weather clear"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ 35 10"
            if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) {
                Add-LucernaControllerMarker $MarkerPath "round9.scene=high-render-distance-open-terrain round9SceneKind=high-render-distance-open-terrain sceneKind=high-render-distance-open-terrain clusterDensityBucket=open complexityBucket=medium"
            }
        }
        "forest-complex" {
            Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
            Send-MinecraftChatCommand "/gamemode creative"
            Send-MinecraftChatCommand "/time set 6000"
            Send-MinecraftChatCommand "/weather clear"
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 minecraft:grass_block"
                Send-MinecraftChatCommand "/fill ~-8 ~ ~-8 ~8 ~8 ~8 minecraft:air"
                Send-MinecraftChatCommand "/fill ~-7 ~ ~-7 ~-5 ~5 ~-5 minecraft:oak_leaves"
                Send-MinecraftChatCommand "/fill ~5 ~ ~-7 ~7 ~6 ~-5 minecraft:spruce_leaves"
                Send-MinecraftChatCommand "/fill ~-7 ~ ~5 ~-5 ~4 ~7 minecraft:birch_leaves"
                Send-MinecraftChatCommand "/fill ~5 ~ ~5 ~7 ~5 ~7 minecraft:jungle_leaves"
                Send-MinecraftChatCommand "/fill ~-6 ~ ~-6 ~-6 ~4 ~-6 minecraft:oak_log"
                Send-MinecraftChatCommand "/fill ~6 ~ ~-6 ~6 ~5 ~-6 minecraft:spruce_log"
                Send-MinecraftChatCommand "/fill ~-6 ~ ~6 ~-6 ~3 ~6 minecraft:birch_log"
                Send-MinecraftChatCommand "/fill ~6 ~ ~6 ~6 ~4 ~6 minecraft:jungle_log"
                Send-MinecraftChatCommand "/fill ~-2 ~ ~-2 ~2 ~2 ~2 minecraft:mossy_cobblestone"
                Send-MinecraftChatCommand "/setblock ~ ~1 ~ minecraft:glowstone"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~2 ~ -35 12"
            if (-not [string]::IsNullOrWhiteSpace($MarkerPath)) {
                Add-LucernaControllerMarker $MarkerPath "round9.scene=forest-complex-area round9SceneKind=forest-complex-area sceneKind=forest-complex-area clusterDensityBucket=forest complexityBucket=complex"
            }
        }
        default {
            throw "Unsupported Round 9 scene action: $SceneAction"
        }
    }
}

function Invoke-Round10SceneAction {
    param(
        [string] $SceneAction,
        [string] $MarkerPath
    )

    Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
    Send-MinecraftChatCommand "/gamemode creative"
    Send-MinecraftChatCommand "/time set 6000"
    Send-MinecraftChatCommand "/weather clear"

    if ($SetupScene) {
        Send-MinecraftChatCommand "/kill @e[type=!player,distance=..48]"
        Send-MinecraftChatCommand "/fill ~2 ~-1 ~-6 ~10 ~4 ~6 minecraft:smooth_stone"
        Send-MinecraftChatCommand "/fill ~4 ~ ~-5 ~9 ~3 ~5 minecraft:air"
        Send-MinecraftChatCommand "/fill ~6 ~ ~-4 ~6 ~3 ~-1 minecraft:deepslate"
        Send-MinecraftChatCommand "/fill ~6 ~ ~1 ~6 ~3 ~4 minecraft:copper_block"
        Send-MinecraftChatCommand "/fill ~7 ~ ~-5 ~7 ~3 ~-3 minecraft:glass"
        Send-MinecraftChatCommand "/fill ~8 ~ ~3 ~8 ~2 ~5 minecraft:water"
        Send-MinecraftChatCommand "/fill ~4 ~4 ~-5 ~9 ~8 ~5 minecraft:air"
        Send-MinecraftChatCommand "/fill ~9 ~ ~-1 ~10 ~2 ~1 minecraft:air"
        Send-MinecraftChatCommand "/setblock ~8 ~0 ~4 minecraft:water"
        Send-MinecraftChatCommand "/setblock ~7 ~1 ~-4 minecraft:tinted_glass"
        Send-MinecraftChatCommand "/setblock ~4 ~1 ~ minecraft:glowstone"
        Send-MinecraftChatCommand "/summon minecraft:armor_stand ~5 ~ ~2 {NoGravity:1b,Invisible:0b,Tags:[`"LucernaR10A`"]}"
        Send-MinecraftChatCommand "/summon minecraft:armor_stand ~8 ~ ~-2 {NoGravity:1b,Invisible:0b,Tags:[`"LucernaR10B`"]}"
        Send-MinecraftChatCommand "/fill ~12 ~-1 ~-2 ~13 ~1 ~2 minecraft:smooth_stone"
        Send-MinecraftChatCommand "/fill ~12 ~-1 ~-2 ~13 ~1 ~2 minecraft:air"
    }

    switch ($SceneAction) {
        "voxel-rays" {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round10.scene=voxel-rays-wall-open-sky-glass-water voxelRayDebugScene=true traversalDebugScene=true wallSceneMarker=true tunnelSceneMarker=true openSkySceneMarker=true glassWaterSceneMarker=true opaqueMaterialSceneMarker=true materialIdConsistencyScene=true maskBitsSceneSource=controller-known-scene emptySectionSkipScene=true sectionLifecycleMarker=true sectionLifecycleCount=1 section_lifecycle_count=1"
        }
        "rt-entities" {
            Send-MinecraftChatCommand "/tp @e[type=minecraft:armor_stand,tag=LucernaR10A,distance=..48,limit=1] ~ ~ ~1"
            Send-MinecraftChatCommand "/tp @e[type=minecraft:armor_stand,tag=LucernaR10B,distance=..48,limit=1] ~ ~ ~-1"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -80 0"
            Add-LucernaControllerMarker $MarkerPath "round10.scene=rt-entities-moving rtEntityDebugScene=true blasTlasDebugScene=true entityMovementMarker=true entityMovementCount=2 entity_movement_count=2 rtEntityMovementScene=true hardwareRtFallbackAccepted=true sectionLifecycleMarker=true sectionLifecycleCount=1 section_lifecycle_count=1"
        }
        "hybrid-hits" {
            Send-MinecraftChatCommand "/fill ~11 ~-1 ~-2 ~12 ~2 ~2 minecraft:smooth_stone"
            Send-MinecraftChatCommand "/fill ~11 ~ ~-1 ~12 ~1 ~1 minecraft:air"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -100 2"
            Add-LucernaControllerMarker $MarkerPath "round10.scene=hybrid-hits-wall-glass-water-churn hybridHitDebugScene=true voxelRtScreenSpaceScene=true wallSceneMarker=true glassWaterSceneMarker=true opaqueMaterialSceneMarker=true entityMovementMarker=true entityMovementCount=2 entity_movement_count=2 chunkChurnMarker=true chunkChurnCount=1 chunk_churn_count=1 chunkChurnScene=true sectionLifecycleMarker=true sectionLifecycleCount=2 section_lifecycle_count=2 sourceStable=true selected_source_stability=stable chunkChurnMaterialConsistent=true chunk_churn_material_consistent=true entityMoveMaterialConsistent=true entity_move_material_consistent=true realTracedLightingConsumed=false real_traced_lighting_consumed=false tracedLightingNoOverclaim=true round10.tracedLightingNoOverclaim=true"
        }
        default {
            throw "Unsupported Round 10 scene action: $SceneAction"
        }
    }
}

function Invoke-Round11SceneAction {
    param(
        [string] $SceneAction,
        [string] $MarkerPath
    )

    Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
    Send-MinecraftChatCommand "/gamemode creative"
    Send-MinecraftChatCommand "/weather clear"
    Send-MinecraftChatCommand "/time set 18000"

    if ($SetupScene) {
        Send-MinecraftChatCommand "/kill @e[type=!player,distance=..48]"
        Send-MinecraftChatCommand "/fill ~3 ~-1 ~-5 ~9 ~4 ~5 minecraft:smooth_stone"
        Send-MinecraftChatCommand "/fill ~5 ~ ~-4 ~5 ~3 ~4 minecraft:air"
        Send-MinecraftChatCommand "/fill ~6 ~ ~-4 ~6 ~3 ~-2 minecraft:blue_concrete"
        Send-MinecraftChatCommand "/fill ~6 ~ ~2 ~6 ~3 ~4 minecraft:red_concrete"
        Send-MinecraftChatCommand "/fill ~7 ~ ~-1 ~7 ~2 ~1 minecraft:lime_concrete"
        Send-MinecraftChatCommand "/setblock ~3 ~1 ~ minecraft:glowstone"
        Send-MinecraftChatCommand "/setblock ~4 ~ ~3 minecraft:sea_lantern"
        Send-MinecraftChatCommand "/setblock ~4 ~ ~-3 minecraft:shroomlight"
    }

    switch ($SceneAction) {
        "direct-baseline" {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round11.execution.scene=direct-brute-baseline directBruteBaseline=true sameSceneRestirPair=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        "direct-enabled" {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round11.execution.scene=restir-direct-enabled restirDirectEnabled=true sameSceneRestirPair=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        "temporal-stable" {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Start-Sleep -Seconds 5
            Add-LucernaControllerMarker $MarkerPath "round11.stability.scene=temporal sceneState=stable restirTemporalStable=true sameSceneStablePair=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        "temporal-moved" {
            Send-MinecraftChatCommand "/tp @s ~-1 ~ ~1 -70 1"
            Start-Sleep -Milliseconds 750
            Send-MinecraftChatCommand "/tp @s ~2 ~ ~-2 -125 7"
            Start-Sleep -Seconds 3
            Add-LucernaControllerMarker $MarkerPath "round11.stability.scene=temporal sceneState=moved-disoccluded restirTemporalMoved=true movedCameraTemporalPair=true temporalCameraTranslation=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        "execution-debug" {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
            Add-LucernaControllerMarker $MarkerPath "round11.execution.scene=execution-debug restirExecutionDebug=true executionTelemetryDebugScene=true temporalWorldSurfaceRegion=upper-mid-no-hud"
        }
        default {
            throw "Unsupported Round 11 ReSTIR scene action: $SceneAction"
        }
    }
}

function Wait-NewScreenshot {
    param(
        [string] $ScreenshotDir,
        [string[]] $ExistingNames,
        [datetime] $After,
        [datetime] $Deadline,
        [switch] $RequireNewAfter
    )

    while ((Get-Date) -lt $Deadline) {
        $afterWithTolerance = $After.AddSeconds(-1)
        $candidate = Get-ChildItem -LiteralPath $ScreenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
                Where-Object { $_.LastWriteTime -gt $afterWithTolerance -and $ExistingNames -notcontains $_.Name } |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
        if ($null -ne $candidate) {
            return $candidate
        }
        $timestampCandidate = Get-ChildItem -LiteralPath $ScreenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
                Where-Object { $_.LastWriteTime -gt $afterWithTolerance } |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
        if ($null -ne $timestampCandidate) {
            return $timestampCandidate
        }
        Start-Sleep -Milliseconds 500
    }
    $launchWindowStart = if ($script:LucernaMinecraftLaunchStart) {
        $script:LucernaMinecraftLaunchStart.AddSeconds(-5)
    } else {
        $After.AddMinutes(-2)
    }
    $launchCandidate = Get-ChildItem -LiteralPath $ScreenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTime -gt $launchWindowStart } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    if ($null -ne $launchCandidate) {
        if ($RequireNewAfter) {
            throw "Timed out waiting for a screenshot created after the controller capture request. Refusing stale launch-window screenshot: $($launchCandidate.FullName)"
        }
        return $launchCandidate
    }
    throw "Timed out waiting for a new Minecraft screenshot."
}

function Save-MinecraftWindowScreenshot {
    param(
        [string] $DestinationPath
    )

    $windowProcess = Focus-MinecraftWindow
    if ($null -eq $windowProcess) {
        throw "Could not find a Minecraft/java window for fallback screenshot capture."
    }

    if (-not ("LucernaVisualProof.NativeWindow" -as [type])) {
        Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

namespace LucernaVisualProof {
    public static class NativeWindow {
        [StructLayout(LayoutKind.Sequential)]
        public struct RECT {
            public int Left;
            public int Top;
            public int Right;
            public int Bottom;
        }

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    }
}
"@
    }
    Add-Type -AssemblyName System.Drawing

    $rect = New-Object LucernaVisualProof.NativeWindow+RECT
    if (-not [LucernaVisualProof.NativeWindow]::GetWindowRect($windowProcess.MainWindowHandle, [ref]$rect)) {
        throw "Could not get Minecraft/java window bounds for fallback screenshot capture."
    }

    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)
    $bitmap = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
        $bitmap.Save($DestinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
    return Get-Item -LiteralPath $DestinationPath
}

function Copy-FreshLatestLog {
    param(
        [string] $Root,
        [string] $ValidationDir,
        [string] $Scenario,
        [string] $Stamp,
        [string] $SourceLog = ""
    )

    $latestLog = if ([string]::IsNullOrWhiteSpace($SourceLog)) {
        Join-Path $Root "run\logs\latest.log"
    } else {
        $SourceLog
    }
    if (-not (Test-Path -LiteralPath $latestLog)) {
        return ""
    }

    $safeScenario = ($Scenario -replace "[^A-Za-z0-9_.-]", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($safeScenario)) {
        $safeScenario = "lucerna-visual-proof"
    }

    $target = Join-Path $ValidationDir "latest-$safeScenario-$Stamp.log"
    Copy-Item -LiteralPath $latestLog -Destination $target -Force
    return $target
}

if (-not [string]::IsNullOrWhiteSpace($BaselineImagePath) -or -not [string]::IsNullOrWhiteSpace($EnabledImagePath)) {
    if ([string]::IsNullOrWhiteSpace($BaselineImagePath) -or [string]::IsNullOrWhiteSpace($EnabledImagePath)) {
        throw "Both -BaselineImagePath and -EnabledImagePath are required for image-delta-only mode."
    }
    Invoke-ImageDeltaComparison $BaselineImagePath $EnabledImagePath $ImageDeltaJsonPath
    if (-not [string]::IsNullOrWhiteSpace($ImageDiagnosticsJsonPath) -or $IncludeImageBandDiagnostics) {
        Invoke-ImageDiagnosticsComparison $BaselineImagePath $EnabledImagePath $ImageDiagnosticsJsonPath
    }
    return
}

$root = (Resolve-Path ".").Path
$gradlew = Join-Path $root "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Run this script from a Minecraft mod workspace containing gradlew.bat."
}

$scenario = if ([string]::IsNullOrWhiteSpace($ScenarioName)) {
    if ($ValidationProfile -eq "Round56PhysicalLighting") {
        "round56-physical-lighting-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round7DenoiseComposite") {
        "round7-denoise-composite-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round7CompositeStability") {
        "round7-composite-stability-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        "round7-emissive-gi-surface-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        "round8-adaptive-heatmap-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        "round9-virtualized-geometry-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round10HybridTracing") {
        "round10-hybrid-tracing-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round11Restir") {
        "round11-restir-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        "round5-direct-surface-$($Mode.ToLowerInvariant())"
    } else {
        "round5-visual-proof-$($Mode.ToLowerInvariant())"
    }
} else {
    $ScenarioName
}
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$validationDir = Join-Path $root "run\validation-logs"
$screenshotArchiveDir = Join-Path $root "run\validation-screenshots"
$screenshotDir = Join-Path $root "run\screenshots"
New-Item -ItemType Directory -Force -Path $validationDir | Out-Null
New-Item -ItemType Directory -Force -Path $screenshotArchiveDir | Out-Null
New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null

$configPath = Join-Path $root "run\config\lucerna.json"
$backupConfig = $null
$configExisted = Test-Path -LiteralPath $configPath
if ($configExisted) {
    $backupConfig = Get-Content -Raw -LiteralPath $configPath
}

$aliasPath = $null
$createdAlias = $false
$process = $null
try {
    $round7CaptureIntent = $null
    $round7StabilityCaptureIntent = $null
    $round7SurfaceCaptureIntent = $null
    $round8CaptureIntent = $null
    $round9CaptureIntent = $null
    $round10CaptureIntent = $null
    $round11CaptureIntent = $null
    $round56PhysicalLightingCaptureIntent = $null
    if ($ValidationProfile -eq "Round56PhysicalLighting") {
        if ($ScreenshotSource -ne "InClient") {
            throw "Round56PhysicalLighting requires -ScreenshotSource InClient so capture provenance comes from the Minecraft client screenshot hook."
        }
        $RejectWindowScreenshotSource = $true
        $round56PhysicalLightingCaptureIntent = Get-Round56PhysicalLightingCaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round56PhysicalLightingCaptureIntent.rendererEnabled) `
            ([string]$round56PhysicalLightingCaptureIntent.debugOverlay) `
            ([string]$round56PhysicalLightingCaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round7DenoiseComposite") {
        $round7CaptureIntent = Get-Round7CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round7CaptureIntent.rendererEnabled) `
            ([string]$round7CaptureIntent.debugOverlay) `
            ([string]$round7CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round7CompositeStability") {
        $round7StabilityCaptureIntent = Get-Round7CompositeStabilityCaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round7StabilityCaptureIntent.rendererEnabled) `
            ([string]$round7StabilityCaptureIntent.debugOverlay) `
            ([string]$round7StabilityCaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        $round7SurfaceCaptureIntent = Get-Round7EmissiveGiSurfaceCaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round7SurfaceCaptureIntent.rendererEnabled) `
            ([string]$round7SurfaceCaptureIntent.debugOverlay) `
            ([string]$round7SurfaceCaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        $round8CaptureIntent = Get-Round8CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round8CaptureIntent.rendererEnabled) `
            ([string]$round8CaptureIntent.debugOverlay) `
            ([string]$round8CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        $round9CaptureIntent = Get-Round9CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round9CaptureIntent.rendererEnabled) `
            ([string]$round9CaptureIntent.debugOverlay) `
            ([string]$round9CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round10HybridTracing") {
        $round10CaptureIntent = Get-Round10CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round10CaptureIntent.rendererEnabled) `
            ([string]$round10CaptureIntent.debugOverlay) `
            ([string]$round10CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round11Restir") {
        $round11CaptureIntent = Get-Round11CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round11CaptureIntent.rendererEnabled) `
            ([string]$round11CaptureIntent.debugOverlay) `
            ([string]$round11CaptureIntent.compositeMode)
    } else {
        switch ($Mode) {
            "Baseline" { Write-LucernaConfig $root $false "OFF" }
            "Enabled" { Write-LucernaConfig $root $true "OFF" }
            "Debug" { Write-LucernaConfig $root $true "DIRECT_LIGHTING" }
            default { throw "Mode '$Mode' is only supported with -ValidationProfile Round7DenoiseComposite." }
        }
    }

    $latestLog = Join-Path $root "run\logs\latest.log"
    if (Test-Path -LiteralPath $latestLog) {
        Remove-Item -LiteralPath $latestLog -Force
    }

    $quickPlayWorld = $WorldName
    if ($WorldName -match "\s") {
        $safeWorldName = "CodexVisualProofWorld"
        $worldTarget = Join-Path $root ("run\saves\" + $WorldName)
        $aliasPath = Join-Path $root ("run\saves\" + $safeWorldName)
        if (Test-Path -LiteralPath $aliasPath) {
            $existing = Get-Item -LiteralPath $aliasPath
            $existingTarget = if ($existing.Target -is [array]) { $existing.Target -join "" } else { [string]$existing.Target }
            if ($existing.LinkType -ne "Junction" -or [string]::IsNullOrWhiteSpace($existingTarget)) {
                Remove-Item -LiteralPath $aliasPath -Recurse -Force
                New-Item -ItemType Junction -Path $aliasPath -Target $worldTarget | Out-Null
                $createdAlias = $true
            } elseif ($existingTarget -ne $worldTarget) {
                throw "Quick-play alias already exists with a different target: $aliasPath"
            }
        } else {
            New-Item -ItemType Junction -Path $aliasPath -Target $worldTarget | Out-Null
            $createdAlias = $true
        }
        $quickPlayWorld = $safeWorldName
    }

    $gradleOut = Join-Path $validationDir "gradle-$scenario-$stamp.out.log"
    $gradleErr = Join-Path $validationDir "gradle-$scenario-$stamp.err.log"
    $loomArgs = "runClient"
    if (-not [string]::IsNullOrWhiteSpace($quickPlayWorld)) {
        $loomArgs += " `"--args=--quickPlaySingleplayer $quickPlayWorld`""
    }

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $gradlew
    $psi.WorkingDirectory = $root
    $psi.Arguments = $loomArgs
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    if ($ValidationProfile -eq "Round5DirectSurface" -or $ValidationProfile -eq "Round6NativeDiffuseGiNoMarker" -or $ValidationProfile -eq "Round56PhysicalLighting" -or $ValidationProfile -eq "Round7DenoiseComposite" -or $ValidationProfile -eq "Round7CompositeStability" -or $ValidationProfile -eq "Round7EmissiveGiSurface" -or $ValidationProfile -eq "Round8AdaptiveHeatmaps" -or $ValidationProfile -eq "Round9VirtualizedGeometry" -or $ValidationProfile -eq "Round10HybridTracing" -or $ValidationProfile -eq "Round11Restir") {
        $psi.Environment["LUCERNA_HIDE_PROOF_OVERLAYS"] = "true"
    }
    if ($ValidationProfile -eq "Round56PhysicalLighting") {
        $psi.Environment["LUCERNA_PHYSICAL_LIGHTING_CAPTURE_MODE"] = [string]$round56PhysicalLightingCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_PHYSICAL_LIGHTING_ARTIFACT_ROLE"] = [string]$round56PhysicalLightingCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_PHYSICAL_LIGHTING_VISUAL_PROOF_OWNER"] = "controller"
        $psi.Environment["LUCERNA_PHYSICAL_LIGHTING_STRICT_PROOF"] = "true"
    }
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        $psi.Environment["LUCERNA_ROUND7_CAPTURE_MODE"] = [string]$round7CaptureIntent.artifactRole
        if ($round7CaptureIntent.Contains("shaderDenoiseEvidence") -and [bool]$round7CaptureIntent.shaderDenoiseEvidence) {
            $psi.Environment["LUCERNA_ROUND7_SHADER_DENOISE_PROOF"] = "true"
            $psi.Environment["LUCERNA_ROUND7_SHADER_DENOISE_ARTIFACT_ROLE"] = [string]$round7CaptureIntent.artifactRole
            $psi.Environment["LUCERNA_ROUND7_SHADER_DENOISE_INTENT"] = "true"
            $psi.Environment["LUCERNA_ROUND7_SHADER_DENOISE_BOUNDARY_EVIDENCE"] = "true"
        }
    }
    if ($ValidationProfile -eq "Round7CompositeStability") {
        $psi.Environment["LUCERNA_ROUND7_CAPTURE_MODE"] = [string]$round7StabilityCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND7_STABILITY_CAPTURE_MODE"] = [string]$round7StabilityCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND7_STABILITY_SCENE"] = [string]$round7StabilityCaptureIntent.sceneKind
        $psi.Environment["LUCERNA_ROUND7_STABILITY_SCENE_STATE"] = [string]$round7StabilityCaptureIntent.sceneState
        $psi.Environment["LUCERNA_ROUND7_STABILITY_PROOF_OWNER"] = "controller"
        $psi.Environment["LUCERNA_ROUND7_STABILITY_TEMPORAL_CAPTURE_COUNT"] = [string]$TemporalCaptureCount
        $psi.Environment["LUCERNA_ROUND7_STABILITY_TEMPORAL_CAPTURE_INTERVAL_SECONDS"] = [string]$TemporalCaptureIntervalSeconds
        if (-not [string]::IsNullOrWhiteSpace($TemporalCaptureLabel)) {
            $psi.Environment["LUCERNA_ROUND7_STABILITY_TEMPORAL_CAPTURE_LABEL"] = $TemporalCaptureLabel
        }
    }
    if ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        $psi.Environment["LUCERNA_ROUND7_CAPTURE_MODE"] = [string]$round7SurfaceCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND7_SURFACE_CAPTURE_MODE"] = [string]$round7SurfaceCaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND7_SURFACE_PROOF_OWNER"] = "controller"
        $psi.Environment["LUCERNA_ROUND7_SURFACE_REGION"] = "fixed-upper-mid-world-surface"
    }
    if ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        $psi.Environment["LUCERNA_ROUND8_CAPTURE_MODE"] = [string]$round8CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND8_ARTIFACT_ROLE"] = [string]$round8CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND8_HEATMAP"] = [string]$round8CaptureIntent.heatmapKind
        $psi.Environment["LUCERNA_ROUND8_SCENE_STATE"] = [string]$round8CaptureIntent.sceneState
        $psi.Environment["LUCERNA_ROUND8_VISUAL_PROOF_OWNER"] = "controller"
    }
    if ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        $psi.Environment["LUCERNA_ROUND9_CAPTURE_MODE"] = [string]$round9CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND9_ARTIFACT_ROLE"] = [string]$round9CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND9_SCENE_KIND"] = [string]$round9CaptureIntent.sceneKind
        $psi.Environment["LUCERNA_ROUND9_VISUAL_PROOF_OWNER"] = "controller"
    }
    if ($ValidationProfile -eq "Round10HybridTracing") {
        $psi.Environment["LUCERNA_ROUND10_CAPTURE_MODE"] = [string]$round10CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND10_ARTIFACT_ROLE"] = [string]$round10CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND10_SCENE_KIND"] = [string]$round10CaptureIntent.sceneKind
        $psi.Environment["LUCERNA_ROUND10_SCENE_ACTION"] = [string]$round10CaptureIntent.sceneAction
        $psi.Environment["LUCERNA_ROUND10_KNOWN_SCENE_MARKERS"] = "wall,tunnel,open-sky,glass,water,opaque,empty-section"
        $psi.Environment["LUCERNA_ROUND10_REQUIRE_MATERIAL_PROOF"] = "true"
        $psi.Environment["LUCERNA_ROUND10_ALLOW_HARDWARE_RT_FALLBACK"] = "true"
        $psi.Environment["LUCERNA_ROUND10_VISUAL_PROOF_OWNER"] = "controller"
    }
    if ($ValidationProfile -eq "Round11Restir") {
        $psi.Environment["LUCERNA_ROUND11_CAPTURE_MODE"] = [string]$round11CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND11_ARTIFACT_ROLE"] = [string]$round11CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND11_SCENE_KIND"] = [string]$round11CaptureIntent.sceneKind
        if ($round11CaptureIntent.Contains("sceneState")) {
            $psi.Environment["LUCERNA_ROUND11_SCENE_STATE"] = [string]$round11CaptureIntent.sceneState
        }
        if ($round11CaptureIntent.Contains("sceneAction")) {
            $psi.Environment["LUCERNA_ROUND11_SCENE_ACTION"] = [string]$round11CaptureIntent.sceneAction
        }
        $psi.Environment["LUCERNA_ROUND11_EXECUTION_MODE"] = [string]$round11CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND11_VISUAL_PROOF_OWNER"] = "controller"
        $psi.Environment["LUCERNA_ROUND11_TEMPORAL_CAPTURE_COUNT"] = [string]$TemporalCaptureCount
        $psi.Environment["LUCERNA_ROUND11_TEMPORAL_CAPTURE_INTERVAL_SECONDS"] = [string]$TemporalCaptureIntervalSeconds
        if (-not [string]::IsNullOrWhiteSpace($TemporalCaptureLabel)) {
            $psi.Environment["LUCERNA_ROUND11_TEMPORAL_CAPTURE_LABEL"] = $TemporalCaptureLabel
        }
    }
    if ($ScreenshotSource -eq "InClient") {
        $psi.Environment["LUCERNA_CONTROLLER_SCREENSHOT_REQUEST"] = "true"
        $screenshotDelayTicks = if ($ValidationProfile -eq "Round7CompositeStability" -or ($ValidationProfile -eq "Round11Restir" -and $round11CaptureIntent -and [string]$round11CaptureIntent.sceneKind -eq "round11-restir-temporal")) { "1400" } else { "180" }
        $psi.Environment["LUCERNA_CONTROLLER_SCREENSHOT_DELAY_TICKS"] = $screenshotDelayTicks
    }
    $script:LucernaMinecraftLaunchStart = Get-Date
    $process = [System.Diagnostics.Process]::Start($psi)
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()
    Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
        if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData.Out -Value $EventArgs.Data }
    } -MessageData @{ Out = $gradleOut } | Out-Null
    Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
        if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData.Err -Value $EventArgs.Data }
    } -MessageData @{ Err = $gradleErr } | Out-Null

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $commonPatterns = @(
        "Using graphics backend Vulkan",
        "Lucerna backend status: SODIUM_VULKAN",
        "joined the game"
    )
    $enabledPatterns = if ($ValidationProfile -eq "Round56PhysicalLighting") {
        if ($PhysicalLightingRequiredLogPattern.Count -gt 0) {
            @($PhysicalLightingRequiredLogPattern)
        } else {
            @($round56PhysicalLightingCaptureIntent.requiredPatterns)
        }
    } elseif ($ValidationProfile -eq "Round7DenoiseComposite") {
        @($round7CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round7CompositeStability") {
        @($round7StabilityCaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        @($round7SurfaceCaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        @($round8CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        @($round9CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round10HybridTracing") {
        @($round10CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round11Restir") {
        @($round11CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGiNoMarker") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:nativeGiOutputReady|nativeDiffuseGiOutputReady|sourceNativeGiReady)=true",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi)",
            "physical_gi_samples=[1-9][0-9]*.*physical_gi_hit_samples=[1-9][0-9]*",
            "surface_material_hit_coupled_samples=[1-9][0-9]*.*geometry_hit_coupled_samples=[1-9][0-9]*",
            "physical_scene_linked=true.*physical_surface_contribution=true",
            "physical_sample_marker=`"?[^`"\r\n,}]+.*surface_material_hit_marker=`"?[^`"\r\n,}]+",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-native-diffuse-gi-surface-additive"
        )
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGi") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:nativeGiOutputReady|nativeDiffuseGiOutputReady|sourceNativeGiReady)=true",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi)",
            "physical_gi_samples=[1-9][0-9]*.*physical_gi_hit_samples=[1-9][0-9]*",
            "surface_material_hit_coupled_samples=[1-9][0-9]*.*geometry_hit_coupled_samples=[1-9][0-9]*",
            "physical_scene_linked=true.*physical_surface_contribution=true",
            "physical_sample_marker=`"?[^`"\r\n,}]+.*surface_material_hit_marker=`"?[^`"\r\n,}]+",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=(?:round6-diffuse-gi-|round6-native-diffuse-gi-)"
        )
    } elseif ($ValidationProfile -eq "Round6DiffuseGi") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: ready=true .*temporarySourceReady=true",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-diffuse-gi-focus-window-additive"
        )
    } elseif ($ValidationProfile -eq "Round5DirectSurface" -and $Mode -ne "Baseline") {
        @(
            "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
            "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*mode=(?![^`r`n]*focus-window)[^`r`n]*(?:direct|emissive))(?=[^`r`n]*(?:surface|world|final|composite))"
        )
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        @()
    } else {
        @(
        "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
        "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_surface_sample_cpu_output_generated",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=final-composite-direct-light-focus-window-additive"
        )
    }
    $forbiddenPatterns = if ($ValidationProfile -eq "Round56PhysicalLighting") {
        @(
            "temporarySourceReady=true",
            "temporaryDirectLightSubstitution=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "Lucerna public Mojang final composite: .*metadataOnlyPreview=true",
            "Lucerna Round 6 diffuse GI preview composite: .*metadata-only",
            "physicalLighting.*metadata scaffold",
            "physicalLighting.*no_render_output",
            "physicalGiTracingQuality=(?!open)",
            "physical GI .*production-quality",
            "physicallyCorrectGi=true",
            "realPhysicalGiTracing=true",
            "realGpuGiTracing=true",
            "metadata_only_proof_rejected=false",
            "focus_window_capture_rejected=false",
            "proof_marker_evidence_rejected=false",
            "temporary_direct_substitution_rejected=false",
            "Lucerna public Mojang final composite: .*metadata scaffold",
            "Lucerna public Mojang final composite: .*no_render_output",
            "round6-diffuse-gi-focus-window-additive",
            "final-composite-direct-light-focus-window-additive",
            "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true",
            "focusWindowOnly(?:Submitted)?=true",
            "focus_window_only=true",
            "round5-direct-proof",
            "R5 visual proof",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "invalid descriptor",
            "VK_ERROR",
            "VK_[A-Z_]*ERROR",
            "Lucerna native error",
            "native error",
            "Vulkan error"
        ) + @($PhysicalLightingForbiddenLogPattern)
    } elseif ($ValidationProfile -eq "Round7DenoiseComposite" -or $ValidationProfile -eq "Round7CompositeStability" -or $ValidationProfile -eq "Round7EmissiveGiSurface") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "final-composite-direct-light-focus-window-additive",
            "focusWindowOnly(?:Submitted)?=true",
            "focus_window_only=true",
            "round6-gi-proof",
            "R6 GI proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "invalid descriptor",
            "VK_ERROR",
            "VK_[A-Z_]*ERROR",
            "Lucerna native error",
            "native error",
            "Vulkan error"
        )
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "invalidRayBudget=true",
            "invalid_budget_values=true",
            "negative ray budget",
            "rayBudget=.*(?:NaN|Infinity)"
        )
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "R8 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "invalidCluster(?:Count|s)?=true",
            "negative cluster",
            "cluster(?:Count|s)?=.*(?:NaN|Infinity)",
            "visibleCluster(?:Count|s)?=.*(?:NaN|Infinity)",
            "terrain corruption",
            "missing terrain",
            "chunk hole",
            "geometry corruption",
            "invalid meshlet",
            "cluster bounds invalid",
            "actualGpuCullingExecuted=false[^`r`n]*(?:realGpuCulling(?:Proven|Ready)|gpuCullingOutputReady)=true",
            "gpu_culling_executed=false[^`r`n]*(?:real_gpu_culling_(?:proven|ready)|gpu_culling_output_ready)=true",
            "realGpuCulling(?:Proven|Ready)=true[^`r`n]*(?:actualGpuCullingExecuted|gpu_culling_executed)=false",
            "real_gpu_culling_(?:proven|ready)=true[^`r`n]*(?:actualGpuCullingExecuted|gpu_culling_executed)=false",
            "gpuCullingBlockerReason=(?!none|ready|executed|n/?a)[A-Za-z0-9_.:-]+[^`r`n]*(?:realGpuCulling(?:Proven|Ready)|gpuCullingOutputReady)=true",
            "gpu_culling_blocker_reason=(?!none|ready|executed|n/?a)[A-Za-z0-9_.:-]+[^`r`n]*(?:real_gpu_culling_(?:proven|ready)|gpu_culling_output_ready)=true",
            "invalid descriptor",
            "VK_ERROR",
            "VK_[A-Z_]*ERROR",
            "Lucerna native error",
            "native error",
            "Vulkan error"
        )
    } elseif ($ValidationProfile -eq "Round10HybridTracing") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "R8 proof",
            "R9 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "invalid(?:VoxelRay|Traversal|HybridHit|RtEntity)(?:Count|s)?=true",
            "invalid(?:Material|MaskBits)(?:Count|s)?=true",
            "negative (?:voxel ray|traversal|hybrid|BLAS|TLAS)",
            "negative (?:material|wall|open sky)",
            "(?:voxelRay(?:Count|s)?|hybridHit(?:Count|s)?|traversal(?:Step|Steps|StepCount)|wallHit(?:Count|s)?|openSkyMiss(?:Count|s)?|materialHit(?:Count|s)?).*(?:NaN|Infinity)",
            "realGpuTraversalExecuted=false[^`r`n]*(?:realGpuTraversal(?:Proven|Ready)|gpuTraversalOutputReady)=true",
            "real_gpu_traversal_executed=false[^`r`n]*(?:real_gpu_traversal_(?:proven|ready)|gpu_traversal_output_ready)=true",
            "hardwareRtExecutionProven=false[^`r`n]*(?:realHardwareRt(?:Proven|Ready)|rtOutputReady)=true",
            "hardware_rt_execution_proven=false[^`r`n]*(?:real_hardware_rt_(?:proven|ready)|rt_output_ready)=true"
        )
    } elseif ($ValidationProfile -eq "Round11Restir") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "R8 proof",
            "R9 proof",
            "R10 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true",
            "focusWindowOnly(?:Submitted)?=true",
            "focus_window_only=true",
            "invalid(?:Reservoir|Restir|ReSTIR|Reuse)(?:Count|s)?=true",
            "negative (?:reservoir|candidate|temporal reuse|spatial reuse|path reuse|invalidation)",
            "(?:reservoir(?:Count|s)?|candidate(?:Count|s)?|temporalReuse|spatialReuse|pathReuse|confidence).*(?:NaN|Infinity)"
        )
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGiNoMarker") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "physicalGiTracingQuality=(?!open)",
            "physicallyCorrectGi=true",
            "realPhysicalGiTracing=true",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true"
        )
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "temporary direct-light",
            "current direct-light RGBA payload",
            "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true",
            "final-composite-direct-light-focus-window-additive",
            "focusWindowOnly(?:Submitted)?=true",
            "focus_window_only=true",
            "round5-direct-proof",
            "R5 visual proof",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "proofMarkerSource=true",
            "cpuOutputProofMarker=true"
        )
    } else {
        @()
    }
    $markerLog = $gradleOut
    $earlyFailureLogPaths = @($gradleOut, $gradleErr)
    Wait-LatestLogPattern $markerLog $commonPatterns $deadline $earlyFailureLogPaths $forbiddenPatterns

    Invoke-OptionalSceneSetup
    if ($SetupScene) {
        Start-Sleep -Seconds 8
    }

    if ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        if ($round8CaptureIntent.sceneAction -eq "moved" -and $SetupScene) {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -35 0"
            Start-Sleep -Seconds 1
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -140 0"
        } elseif ($round8CaptureIntent.sceneAction -eq "stationary") {
            Start-Sleep -Seconds 5
        }
    }
    if ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        Invoke-Round9SceneAction ([string]$round9CaptureIntent.sceneAction) $markerLog
        Start-Sleep -Seconds 5
    }
    if ($ValidationProfile -eq "Round10HybridTracing") {
        Invoke-Round10SceneAction ([string]$round10CaptureIntent.sceneAction) $markerLog
        Start-Sleep -Seconds 5
    }
    if ($ValidationProfile -eq "Round11Restir" -and $round11CaptureIntent.Contains("sceneAction")) {
        Invoke-Round11SceneAction ([string]$round11CaptureIntent.sceneAction) $markerLog
        Start-Sleep -Seconds 5
    }
    if ($ValidationProfile -eq "Round7CompositeStability") {
        Invoke-Round7CompositeStabilitySceneAction ([string]$round7StabilityCaptureIntent.sceneAction) $markerLog
        Start-Sleep -Seconds 3
    }
    if ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        Invoke-Round7EmissiveGiSurfaceSceneAction $markerLog
        Start-Sleep -Seconds 5
    }

    if ($enabledPatterns.Count -gt 0) {
        Wait-LatestLogPattern $markerLog $enabledPatterns $deadline $earlyFailureLogPaths $forbiddenPatterns
    }
    if (($ValidationProfile -eq "Round56PhysicalLighting" -or $ValidationProfile -eq "Round7DenoiseComposite" -or $ValidationProfile -eq "Round7CompositeStability" -or $ValidationProfile -eq "Round7EmissiveGiSurface" -or $ValidationProfile -eq "Round8AdaptiveHeatmaps" -or $ValidationProfile -eq "Round9VirtualizedGeometry" -or $ValidationProfile -eq "Round10HybridTracing" -or $ValidationProfile -eq "Round11Restir") -and -not $SetupScene) {
        Start-Sleep -Seconds 8
    }

    $archiveName = "$scenario-$stamp-$Mode.png"
    $archivePath = Join-Path $screenshotArchiveDir $archiveName
    if ($ValidationProfile -eq "Round7CompositeStability") {
        Invoke-Round7CompositeStabilityPreScreenshotAction ([string]$round7StabilityCaptureIntent.preScreenshotAction) $markerLog
    }
    if ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
        Add-LucernaControllerMarker $markerLog "round7.emissiveGiSurface.cameraLockedBeforeScreenshot=true yaw=-90 pitch=0"
    }
    Clear-MinecraftChat
    $hudHiddenForScreenshot = $false
    if ($ValidationProfile -eq "Round7EmissiveGiSurface" -and [bool]$round7SurfaceCaptureIntent.hideHudForScreenshot) {
        Add-LucernaControllerMarker $markerLog "round7.emissiveGiSurface.captureRole=$($round7SurfaceCaptureIntent.artifactRole) hideGuiBeforeScreenshot=true fixedWorldSurfaceRegion=true commandFeedback=false chatCleared=true"
        Send-MinecraftKeys "{F1}"
        $hudHiddenForScreenshot = $true
        Start-Sleep -Seconds 3
    } elseif ($ValidationProfile -eq "Round7EmissiveGiSurface") {
        Add-LucernaControllerMarker $markerLog "round7.emissiveGiSurface.captureRole=$($round7SurfaceCaptureIntent.artifactRole) hideGuiBeforeScreenshot=false fixedWorldSurfaceRegion=true commandFeedback=false chatCleared=true"
    }
    $temporalRepeatEnabled = (
        (
            $ValidationProfile -eq "Round7CompositeStability" -and
            $round7StabilityCaptureIntent -and
            [string]$round7StabilityCaptureIntent.sceneKind -eq "temporal"
        ) -or (
            $ValidationProfile -eq "Round11Restir" -and
            $round11CaptureIntent -and
            [string]$round11CaptureIntent.sceneKind -eq "round11-restir-temporal"
        )
    ) -and (
        $TemporalCaptureCount -gt 1
    )
    $effectiveCaptureCount = if ($temporalRepeatEnabled) { $TemporalCaptureCount } else { 1 }
    if ($TemporalCaptureCount -gt 1 -and -not $temporalRepeatEnabled) {
        Add-LucernaControllerMarker $markerLog "$($ValidationProfile).temporal.repeatIgnored=true requestedCount=$TemporalCaptureCount reason=non-temporal-capture"
    }
    $captureLabelBase = if ([string]::IsNullOrWhiteSpace($TemporalCaptureLabel)) {
        [string]$Mode
    } else {
        $TemporalCaptureLabel
    }
    $captureLabelSafe = [regex]::Replace($captureLabelBase, "[^A-Za-z0-9_.-]+", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($captureLabelSafe)) {
        $captureLabelSafe = [string]$Mode
    }
    $capturedScreenshotPaths = New-Object System.Collections.Generic.List[string]
    $capturedScreenshotSources = New-Object System.Collections.Generic.List[string]
    $capturedScreenshotStartedAt = New-Object System.Collections.Generic.List[string]
    $capturedScreenshotCompletedAt = New-Object System.Collections.Generic.List[string]
    $capturedScreenshotElapsedMs = New-Object System.Collections.Generic.List[object]
    try {
        $previousCaptureStartedAt = $null
        for ($captureIndex = 0; $captureIndex -lt $effectiveCaptureCount; $captureIndex++) {
            if ($captureIndex -gt 0 -and $TemporalCaptureIntervalSeconds -gt 0) {
                Start-Sleep -Seconds $TemporalCaptureIntervalSeconds
            }
            $captureArchivePath = if ($captureIndex -eq 0) {
                $archivePath
            } else {
                Join-Path $screenshotArchiveDir ("$scenario-$stamp-$Mode-$captureLabelSafe-repeat{0:D2}.png" -f ($captureIndex + 1))
            }
            if ($temporalRepeatEnabled) {
                $repeatSceneState = if ($ValidationProfile -eq "Round11Restir") { [string]$round11CaptureIntent.sceneState } else { [string]$round7StabilityCaptureIntent.sceneState }
                $repeatMarkerPrefix = if ($ValidationProfile -eq "Round11Restir") { "round11.stability.temporal" } else { "round7.stability.temporal" }
                Add-LucernaControllerMarker $markerLog "$repeatMarkerPrefix.repeatCapture index=$($captureIndex + 1) count=$effectiveCaptureCount label=$captureLabelSafe intervalSeconds=$TemporalCaptureIntervalSeconds sceneState=$repeatSceneState"
            }
            Focus-MinecraftWindow | Out-Null
            Send-MinecraftKeys "{ESC}"
            Send-MinecraftKeys "{ESC}"
            Start-Sleep -Milliseconds 500
            $captureStartedAt = Get-Date
            $elapsedFromPreviousStartMs = if ($previousCaptureStartedAt) {
                [Math]::Round(($captureStartedAt - $previousCaptureStartedAt).TotalMilliseconds, 3)
            } else {
                $null
            }
            $existingScreenshotNames = @(Get-ChildItem -LiteralPath $screenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
                    Select-Object -ExpandProperty Name)
            $beforeScreenshot = Get-Date
            if ($ScreenshotSource -eq "Window") {
                $screenshot = Save-MinecraftWindowScreenshot $captureArchivePath
                $capturedScreenshotSource = "window"
            } elseif ($ScreenshotSource -eq "InClient") {
                $screenshotDeadline = (Get-Date).AddSeconds(100)
                if ($temporalRepeatEnabled -and $captureIndex -gt 0) {
                    Send-MinecraftKeys "{F2}"
                    $screenshot = Wait-NewScreenshot $screenshotDir $existingScreenshotNames $beforeScreenshot $screenshotDeadline -RequireNewAfter
                    $capturedScreenshotSource = "minecraft-in-client-f2-repeat"
                } else {
                    Send-MinecraftKeys "{F2}"
                    $screenshot = Wait-NewScreenshot $screenshotDir $existingScreenshotNames $beforeScreenshot $screenshotDeadline -RequireNewAfter
                    $capturedScreenshotSource = "minecraft-in-client"
                }
                Copy-Item -LiteralPath $screenshot.FullName -Destination $captureArchivePath -Force
            } else {
                Send-MinecraftKeys "{F2}"
                $screenshotDeadline = (Get-Date).AddSeconds(45)
                try {
                    $screenshot = Wait-NewScreenshot $screenshotDir $existingScreenshotNames $beforeScreenshot $screenshotDeadline -RequireNewAfter
                    Copy-Item -LiteralPath $screenshot.FullName -Destination $captureArchivePath -Force
                    $capturedScreenshotSource = "minecraft-f2"
                } catch {
                    $screenshot = Save-MinecraftWindowScreenshot $captureArchivePath
                    $capturedScreenshotSource = "window-fallback"
                }
            }
            if ($RejectWindowScreenshotSource -and $capturedScreenshotSource -match "^(?:window|window-fallback)$") {
                throw "Screenshot source '$capturedScreenshotSource' is rejected for visual proof. Use MinecraftF2 or InClient capture, preferably InClient for temporal/flicker evidence."
            }
            $captureCompletedAt = Get-Date
            $capturedScreenshotPaths.Add($captureArchivePath) | Out-Null
            $capturedScreenshotSources.Add($capturedScreenshotSource) | Out-Null
            $capturedScreenshotStartedAt.Add($captureStartedAt.ToString("o")) | Out-Null
            $capturedScreenshotCompletedAt.Add($captureCompletedAt.ToString("o")) | Out-Null
            $capturedScreenshotElapsedMs.Add($elapsedFromPreviousStartMs) | Out-Null
            $previousCaptureStartedAt = $captureStartedAt
        }
    } finally {
        if ($hudHiddenForScreenshot) {
            Send-MinecraftKeys "{F1}"
            Start-Sleep -Milliseconds 250
        }
    }

    $logPath = Copy-FreshLatestLog $root $validationDir $scenario $stamp $markerLog
    $captureManifest = [ordered]@{
        validationProfile = $ValidationProfile
        mode = $Mode
        scenario = $scenario
        stamp = $stamp
        screenshotSourceRequested = $ScreenshotSource
        rejectWindowScreenshotSource = [bool]$RejectWindowScreenshotSource
        temporalCaptureRequestedCount = $TemporalCaptureCount
        temporalCaptureEffectiveCount = $effectiveCaptureCount
        temporalCaptureIntervalSeconds = $TemporalCaptureIntervalSeconds
        temporalCaptureLabel = $captureLabelSafe
        temporalSeries = [ordered]@{
            enabled = [bool]$temporalRepeatEnabled
            fixedIntervalRequested = [bool]($temporalRepeatEnabled -and $TemporalCaptureIntervalSeconds -gt 0)
            requestedCount = $TemporalCaptureCount
            effectiveCount = $effectiveCaptureCount
            intervalSeconds = $TemporalCaptureIntervalSeconds
            label = $captureLabelSafe
            sceneState = if ($ValidationProfile -eq "Round11Restir" -and $round11CaptureIntent) { [string]$round11CaptureIntent.sceneState } elseif ($ValidationProfile -eq "Round7CompositeStability" -and $round7StabilityCaptureIntent) { [string]$round7StabilityCaptureIntent.sceneState } else { "" }
            artifactRole = if ($ValidationProfile -eq "Round11Restir" -and $round11CaptureIntent) { [string]$round11CaptureIntent.artifactRole } elseif ($ValidationProfile -eq "Round7CompositeStability" -and $round7StabilityCaptureIntent) { [string]$round7StabilityCaptureIntent.artifactRole } else { "" }
        }
        screenshots = @(for ($index = 0; $index -lt $capturedScreenshotPaths.Count; $index++) {
            [ordered]@{
                index = $index
                plannedOffsetSeconds = if ($temporalRepeatEnabled) { $index * $TemporalCaptureIntervalSeconds } else { 0 }
                elapsedMsSincePreviousCaptureStart = $capturedScreenshotElapsedMs[$index]
                startedAt = $capturedScreenshotStartedAt[$index]
                completedAt = $capturedScreenshotCompletedAt[$index]
                path = $capturedScreenshotPaths[$index]
                source = $capturedScreenshotSources[$index]
            }
        })
        latestLog = $logPath
    }
    if (-not [string]::IsNullOrWhiteSpace($CaptureManifestJsonPath)) {
        $manifestParent = Split-Path -Parent $CaptureManifestJsonPath
        if (-not [string]::IsNullOrWhiteSpace($manifestParent)) {
            New-Item -ItemType Directory -Force -Path $manifestParent | Out-Null
        }
        $captureManifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $CaptureManifestJsonPath -Encoding UTF8
    }
    Write-Host "screenshot=$archivePath"
    Write-Host "screenshotSource=$($capturedScreenshotSources[0])"
    Write-Host "captureManifestJson=$CaptureManifestJsonPath"
    if ($temporalRepeatEnabled) {
        Write-Host "temporalCaptureCount=$effectiveCaptureCount"
        Write-Host "temporalCaptureIntervalSeconds=$TemporalCaptureIntervalSeconds"
        Write-Host "temporalCaptureLabel=$captureLabelSafe"
        Write-Host "temporalCaptureScreenshots=$($capturedScreenshotPaths -join ';')"
        Write-Host "temporalCaptureSources=$($capturedScreenshotSources -join ';')"
    }
    if ($round7CaptureIntent) {
        Write-Host "round7ArtifactRole=$($round7CaptureIntent.artifactRole)"
        Write-Host "round7CompositeMode=$($round7CaptureIntent.compositeMode)"
        if ($round7CaptureIntent.Contains("shaderDenoiseEvidence")) {
            Write-Host "round7ShaderDenoiseEvidence=$($round7CaptureIntent.shaderDenoiseEvidence)"
        }
    }
    if ($round7StabilityCaptureIntent) {
        Write-Host "round7StabilityArtifactRole=$($round7StabilityCaptureIntent.artifactRole)"
        Write-Host "round7StabilityScene=$($round7StabilityCaptureIntent.sceneKind)"
        Write-Host "round7StabilitySceneState=$($round7StabilityCaptureIntent.sceneState)"
        Write-Host "round7StabilityCompositeMode=$($round7StabilityCaptureIntent.compositeMode)"
    }
    if ($round7SurfaceCaptureIntent) {
        Write-Host "round7SurfaceArtifactRole=$($round7SurfaceCaptureIntent.artifactRole)"
        Write-Host "round7SurfaceCompositeMode=$($round7SurfaceCaptureIntent.compositeMode)"
        Write-Host "round7SurfaceHudHiddenForScreenshot=$hudHiddenForScreenshot"
        Write-Host "round7SurfaceMeasuredRegion=fixed-upper-mid-world-surface"
    }
    if ($round56PhysicalLightingCaptureIntent) {
        Write-Host "physicalLightingArtifactRole=$($round56PhysicalLightingCaptureIntent.artifactRole)"
        Write-Host "physicalLightingCompositeMode=$($round56PhysicalLightingCaptureIntent.compositeMode)"
        Write-Host "physicalLightingDebugOverlay=$($round56PhysicalLightingCaptureIntent.debugOverlay)"
        Write-Host "physicalLightingStrictProof=true"
        Write-Host "physicalLightingRequiredPatternCount=$($enabledPatterns.Count)"
        Write-Host "physicalLightingForbiddenPatternCount=$($forbiddenPatterns.Count)"
    }
    if ($round8CaptureIntent) {
        Write-Host "round8ArtifactRole=$($round8CaptureIntent.artifactRole)"
        Write-Host "round8Heatmap=$($round8CaptureIntent.heatmapKind)"
        Write-Host "round8SceneState=$($round8CaptureIntent.sceneState)"
        Write-Host "round8CompositeMode=$($round8CaptureIntent.compositeMode)"
    }
    if ($round9CaptureIntent) {
        Write-Host "round9ArtifactRole=$($round9CaptureIntent.artifactRole)"
        Write-Host "round9SceneKind=$($round9CaptureIntent.sceneKind)"
        Write-Host "round9DebugOverlay=$($round9CaptureIntent.debugOverlay)"
        Write-Host "round9CompositeMode=$($round9CaptureIntent.compositeMode)"
    }
    if ($round10CaptureIntent) {
        Write-Host "round10ArtifactRole=$($round10CaptureIntent.artifactRole)"
        Write-Host "round10SceneKind=$($round10CaptureIntent.sceneKind)"
        Write-Host "round10DebugOverlay=$($round10CaptureIntent.debugOverlay)"
        Write-Host "round10CompositeMode=$($round10CaptureIntent.compositeMode)"
    }
    if ($round11CaptureIntent) {
        Write-Host "round11ArtifactRole=$($round11CaptureIntent.artifactRole)"
        Write-Host "round11SceneKind=$($round11CaptureIntent.sceneKind)"
        if ($round11CaptureIntent.Contains("sceneState")) {
            Write-Host "round11SceneState=$($round11CaptureIntent.sceneState)"
        }
        if ($round11CaptureIntent.Contains("sceneAction")) {
            Write-Host "round11SceneAction=$($round11CaptureIntent.sceneAction)"
        }
        Write-Host "round11DebugOverlay=$($round11CaptureIntent.debugOverlay)"
        Write-Host "round11CompositeMode=$($round11CaptureIntent.compositeMode)"
    }
    Write-Host "latestLog=$logPath"
    Write-Host "gradleOut=$gradleOut"
    Write-Host "gradleErr=$gradleErr"
} finally {
    if ($process -and -not $process.HasExited) {
        Get-Process | Where-Object { $_.MainWindowTitle -like "*Minecraft*" } | ForEach-Object { [void] $_.CloseMainWindow() }
        if (-not $process.WaitForExit(20000)) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    if ($configExisted) {
        Set-Content -LiteralPath $configPath -Value $backupConfig -Encoding UTF8
    } elseif (Test-Path -LiteralPath $configPath) {
        Remove-Item -LiteralPath $configPath -Force
    }
    if ($createdAlias -and $aliasPath -and (Test-Path -LiteralPath $aliasPath)) {
        try {
            [System.IO.Directory]::Delete($aliasPath)
        } catch {
            Write-Warning "Could not remove quick-play alias ${aliasPath}: $($_.Exception.Message)"
        }
    }
}
