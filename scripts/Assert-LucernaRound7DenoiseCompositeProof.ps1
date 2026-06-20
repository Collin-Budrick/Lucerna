<#
.SYNOPSIS
Controller-only Round 7 assertion helper for raw GI, denoised GI, and final composite evidence.

.DESCRIPTION
This script checks already captured screenshots and an optional controller launch log. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller has captured same-scene Round 7 artifacts.
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

    [string[]] $ShaderDenoiseDispatchPreparedPatterns = @(
        "(?:round7\.shaderDenoise\.dispatchPrepared|shaderDenoiseDispatchPrepared|shader_denoise_dispatch_prepared)=true"
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
        "Lucerna native shader denoise output image candidate: .*realOutput=false"
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

    [switch] $RequirePhysicalGiEvidence
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
        [bool] $DispatchPreparedPresent,
        [bool] $OutputImageReadyPresent,
        [bool] $OutputImageStateExplicitPresent,
        [bool] $OutputMaterialReadyPresent,
        [bool] $OutputMaterialStateExplicitPresent,
        [bool] $ShaderGeneratedOutputTruePresent,
        [bool] $ShaderGeneratedOutputExplicitPresent,
        [bool] $CpuReadbackFallbackActivePresent,
        [bool] $CpuReadbackFallbackExplicitPresent,
        [bool] $RealOutputReadyPresent,
        [bool] $RealOutputStateExplicitPresent,
        [bool] $CpuReadbackSourcePresent,
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
    } elseif ($CpuReadbackFallbackActivePresent -or $CpuReadbackSourcePresent) {
        "cpu-readback-denoised-output"
    } elseif ([bool]$ImageCandidateEvidence.boundaryOnly) {
        "shader-output-image-candidate-boundary"
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
        honestNonOverclaim = $RealOutputProven -or ($CpuReadbackFallbackActivePresent -or $CpuReadbackSourcePresent -or [bool]$ImageCandidateEvidence.boundaryOnly -or $blockerReason -ne "none")
        prerequisites = [ordered]@{
            intent = $IntentPresent
            inputReady = $InputReadyPresent
            dispatchPrepared = $DispatchPreparedPresent
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
        }
    }
}

function Measure-Round7LogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
    $shaderOutputImageCandidateEvidence = Get-Round7ShaderOutputImageCandidateEvidence $log
    $physicalGiEvidence = Get-Round7PhysicalGiEvidence $log
    $acceptedFinalCompositePresent = Test-Regex $log "sourceIdentity=native-direct-light-rgba8\+native-diffuse-gi-rgba8\+cpu-denoised-diffuse-gi-rgba8.*sourceAuthenticity=accepted:final-composite-direct-plus-raw-gi-plus-(?:cpu-)?denoised-gi.*evidence=round7\.composite\.final\.direct_raw_denoised.*finalBlendComplete=true.*metadataOnly=false"
    $directSourcePresent = Test-AnyRegex $log $DirectSourcePatterns
    $nativeGiSourcePresent = Test-AnyRegex $log $NativeGiSourcePatterns
    $rawGiSourcePresent = Test-AnyRegex $log $RawGiSourcePatterns
    $denoiseDispatchPresent = Test-AnyRegex $log $DenoiseDispatchPatterns
    $denoisedGiOutputPresent = Test-AnyRegex $log $DenoisedGiOutputPatterns
    $finalCompositePresent = (Test-AnyRegex $log $FinalCompositePatterns) -or $acceptedFinalCompositePresent
    $hudSafeFinalCompositePresent = Test-AnyRegex $log $HudSafeFinalCompositePatterns
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporaryDirectLightSubstitution=true|using the current direct-light RGBA payload as the temporary visible source"
    $metadataOnlyPreviewPresent = (Test-Regex $log "metadata-only|metadata scaffold|signal_separated_denoise_metadata_scaffold_no_render_output|no_render_output") -and -not $acceptedFinalCompositePresent
    $firstPracticalCpuOutputPresent = Test-Regex $log "first_practical_cpu_denoised_diffuse_gi_rgba8_generated|denoisedCpuOutputGenerated=true|denoised_cpu_output_generated=true"
    $realDenoiseShaderOutputPresent = Test-Regex $log "(?:^|[\s,;])(?:realShaderDenoiseOutputReady|real_shader_denoise_output_ready)=true(?:[,;]|$)"
    $realDenoiseShaderOutputFalsePresent = Test-Regex $log "realDenoiseShaderOutput=false|real_denoise_shader_output=false"
    $shaderDenoiseIntentPresent = Test-AnyRegex $log $ShaderDenoiseIntentPatterns
    $shaderDenoiseInputReadyPresent = Test-AnyRegex $log $ShaderDenoiseInputReadyPatterns
    $shaderDenoiseDispatchPreparedPresent = Test-AnyRegex $log $ShaderDenoiseDispatchPreparedPatterns
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
    $shaderDenoiseOutputReadyPresent = $realShaderDenoiseOutputReadyPresent
    $shaderDenoiseSourceClaimPresent = Test-AnyRegex $log $ShaderDenoiseSourceClaimPatterns
    $shaderDenoiseOutputOpenPresent = (Test-AnyRegex $log $ShaderDenoiseOutputOpenPatterns) -or $realShaderDenoiseOutputNotReadyPresent -or $shaderDenoiseOutputImageNotReadyPresent -or $shaderDenoiseOutputMaterialNotReadyPresent -or $shaderDenoiseShaderGeneratedOutputFalsePresent
    $shaderDenoiseOutputStateExplicitPresent = $realShaderDenoiseOutputStateExplicitPresent
    $realShaderDenoiseOutputProven = $shaderDenoiseDispatchPreparedPresent -and $shaderDenoiseOutputImageReadyPresent -and $shaderDenoiseOutputMaterialReadyPresent -and $shaderDenoiseShaderGeneratedOutputTruePresent -and $realShaderDenoiseOutputReadyPresent -and -not $shaderDenoiseCpuReadbackFallbackActivePresent
    $shaderDenoiseBoundaryEvidence = Get-Round7ShaderDenoiseBoundaryEvidence `
        -LogText $log `
        -ImageCandidateEvidence $shaderOutputImageCandidateEvidence `
        -IntentPresent $shaderDenoiseIntentPresent `
        -InputReadyPresent $shaderDenoiseInputReadyPresent `
        -DispatchPreparedPresent $shaderDenoiseDispatchPreparedPresent `
        -OutputImageReadyPresent $shaderDenoiseOutputImageReadyPresent `
        -OutputImageStateExplicitPresent $shaderDenoiseOutputImageStateExplicitPresent `
        -OutputMaterialReadyPresent $shaderDenoiseOutputMaterialReadyPresent `
        -OutputMaterialStateExplicitPresent $shaderDenoiseOutputMaterialStateExplicitPresent `
        -ShaderGeneratedOutputTruePresent $shaderDenoiseShaderGeneratedOutputTruePresent `
        -ShaderGeneratedOutputExplicitPresent $shaderDenoiseShaderGeneratedOutputExplicitPresent `
        -CpuReadbackFallbackActivePresent $shaderDenoiseCpuReadbackFallbackActivePresent `
        -CpuReadbackFallbackExplicitPresent $shaderDenoiseCpuReadbackFallbackExplicitPresent `
        -RealOutputReadyPresent $realShaderDenoiseOutputReadyPresent `
        -RealOutputStateExplicitPresent $realShaderDenoiseOutputStateExplicitPresent `
        -CpuReadbackSourcePresent $cpuReadbackDenoiseSourcePresent `
        -RealOutputProven $realShaderDenoiseOutputProven
    $shaderDenoiseOpenBoundaryPresent = $shaderDenoiseOutputOpenPresent -or $shaderDenoiseCpuReadbackFallbackActivePresent -or $cpuReadbackDenoiseSourcePresent -or ([bool]$shaderOutputImageCandidateEvidence.boundaryOnly)
    $shaderDenoiseOverclaimPresent = (Test-AnyRegex $log $ShaderDenoiseOverclaimPatterns) -or ($shaderDenoiseSourceClaimPresent -and -not $realShaderDenoiseOutputProven) -or ($realShaderDenoiseOutputReadyPresent -and ($shaderDenoiseCpuReadbackFallbackActivePresent -or -not $shaderDenoiseShaderGeneratedOutputTruePresent -or -not $shaderDenoiseOutputImageReadyPresent -or -not $shaderDenoiseOutputMaterialReadyPresent))
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
            shaderDenoiseDispatchPreparedPresent = $shaderDenoiseDispatchPreparedPresent
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
            shaderDenoiseCpuReadbackFallbackActivePresent = $shaderDenoiseCpuReadbackFallbackActivePresent
            shaderDenoiseCpuReadbackFallbackInactivePresent = $shaderDenoiseCpuReadbackFallbackInactivePresent
            shaderDenoiseCpuReadbackFallbackExplicitPresent = $shaderDenoiseCpuReadbackFallbackExplicitPresent
            realShaderDenoiseOutputReadyPresent = $realShaderDenoiseOutputReadyPresent
            realShaderDenoiseOutputNotReadyPresent = $realShaderDenoiseOutputNotReadyPresent
            realShaderDenoiseOutputStateExplicitPresent = $realShaderDenoiseOutputStateExplicitPresent
            realShaderDenoiseOutputProven = $realShaderDenoiseOutputProven
            shaderDenoiseOpenBoundaryPresent = $shaderDenoiseOpenBoundaryPresent
            cpuReadbackDenoiseSourcePresent = $cpuReadbackDenoiseSourcePresent
            shaderDenoiseOutputReadyPresent = $shaderDenoiseOutputReadyPresent
            shaderDenoiseSourceClaimPresent = $shaderDenoiseSourceClaimPresent
            shaderDenoiseOutputOpenPresent = $shaderDenoiseOutputOpenPresent
            shaderDenoiseOutputStateExplicitPresent = $shaderDenoiseOutputStateExplicitPresent
            shaderDenoiseOverclaimPresent = $shaderDenoiseOverclaimPresent
            shaderDenoiseHonestNonOverclaimPresent = [bool]$shaderDenoiseBoundaryEvidence.honestNonOverclaim -and -not $shaderDenoiseOverclaimPresent
            physicalGiEvidencePresent = [bool]$physicalGiEvidence.present
            physicalGiOverclaimPresent = $physicalGiOverclaimPresent
            proofMarkerPresent = $proofMarkerPresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            submittedFocusWindowOnlyPresent = $submittedFocusWindowOnlyPresent
            submittedRound7GiSourcePresent = $submittedRound7GiSourcePresent
            nativeErrorPresent = $nativeErrorPresent
        }
        shaderOutputImageCandidate = $shaderOutputImageCandidateEvidence
        shaderDenoiseBoundary = $shaderDenoiseBoundaryEvidence
        physicalGiEvidence = $physicalGiEvidence
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
            shaderDenoiseDispatchPreparedPatterns = @($ShaderDenoiseDispatchPreparedPatterns)
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
if ($RequireShaderDenoiseEvidence -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Shader-denoise evidence requires -LogPath so intent, input readiness, source identity, and output readiness can be checked.")
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
    if ($RequirePhysicalGiEvidence -and -not $logProof.markers.physicalGiEvidencePresent) {
        $failures.Add("Missing native physical GI sample/coupling evidence markers for Round 7 raw/final GI source.")
    }
    if ($RequireShaderDenoiseEvidence) {
        if (-not $logProof.markers.shaderDenoiseIntentPresent) {
            $failures.Add("Missing shader-denoise intent marker.")
        }
        if (-not $logProof.markers.shaderDenoiseInputReadyPresent) {
            $failures.Add("Missing shader-denoise input readiness marker.")
        }
        if (-not $logProof.markers.shaderDenoiseDispatchPreparedPresent) {
            $failures.Add("Missing explicit shader-denoise dispatch prepared marker.")
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
                required = [bool]$RequirePhysicalGiEvidence
                evidence = if ($logProof) { $logProof.physicalGiEvidence } else { $null }
                evidencePresent = if ($logProof) { [bool]$logProof.markers.physicalGiEvidencePresent } else { $null }
                overclaimPresent = if ($logProof) { [bool]$logProof.markers.physicalGiOverclaimPresent } else { $null }
                classification = if (-not $RequirePhysicalGiEvidence) {
                    "recorded_only"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif ([bool]$logProof.markers.physicalGiEvidencePresent) {
                    "native_physical_gi_sample_coupling_evidence_present"
                } else {
                    "native_physical_gi_sample_coupling_evidence_missing"
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
                required = [bool]$RequireShaderDenoiseEvidence
                intentLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseIntentPresent } else { $null }
                inputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseInputReadyPresent } else { $null }
                dispatchPreparedLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDispatchPreparedPresent } else { $null }
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
                cpuReadbackFallbackActiveLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent } else { $null }
                cpuReadbackFallbackInactiveLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent } else { $null }
                cpuReadbackFallbackExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseCpuReadbackFallbackExplicitPresent } else { $null }
                cpuReadbackSourceLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.cpuReadbackDenoiseSourcePresent } else { $null }
                realOutputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputReadyPresent } else { $null }
                realOutputNotReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputNotReadyPresent } else { $null }
                realOutputStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputStateExplicitPresent } else { $null }
                realOutputProven = if ($logProof) { [bool]$logProof.markers.realShaderDenoiseOutputProven } else { $null }
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
                classification = if (-not $RequireShaderDenoiseEvidence) {
                    "not_required"
                } elseif (-not $logProof) {
                    "missing_log"
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
                shaderDenoiseDispatchPreparedPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseDispatchPreparedPresent } else { $null }
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
    Write-Host "shaderDenoiseDispatchPreparedPresent=$($logProof.markers.shaderDenoiseDispatchPreparedPresent)"
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
    Write-Host "shaderDenoiseCpuReadbackFallbackActivePresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackActivePresent)"
    Write-Host "shaderDenoiseCpuReadbackFallbackInactivePresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackInactivePresent)"
    Write-Host "shaderDenoiseCpuReadbackFallbackExplicitPresent=$($logProof.markers.shaderDenoiseCpuReadbackFallbackExplicitPresent)"
    Write-Host "cpuReadbackDenoiseSourcePresent=$($logProof.markers.cpuReadbackDenoiseSourcePresent)"
    Write-Host "realShaderDenoiseOutputReadyPresent=$($logProof.markers.realShaderDenoiseOutputReadyPresent)"
    Write-Host "realShaderDenoiseOutputNotReadyPresent=$($logProof.markers.realShaderDenoiseOutputNotReadyPresent)"
    Write-Host "realShaderDenoiseOutputStateExplicitPresent=$($logProof.markers.realShaderDenoiseOutputStateExplicitPresent)"
    Write-Host "realShaderDenoiseOutputProven=$($logProof.markers.realShaderDenoiseOutputProven)"
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
    Write-Host "shaderDenoisePrereq.dispatchPrepared=$($logProof.shaderDenoiseBoundary.prerequisites.dispatchPrepared)"
    Write-Host "shaderDenoisePrereq.outputImageReady=$($logProof.shaderDenoiseBoundary.prerequisites.outputImageReady)"
    Write-Host "shaderDenoisePrereq.outputMaterialReady=$($logProof.shaderDenoiseBoundary.prerequisites.outputMaterialReady)"
    Write-Host "shaderDenoisePrereq.shaderGeneratedOutput=$($logProof.shaderDenoiseBoundary.prerequisites.shaderGeneratedOutput)"
    Write-Host "shaderDenoisePrereq.cpuReadbackFallbackActive=$($logProof.shaderDenoiseBoundary.prerequisites.cpuReadbackFallbackActive)"
    Write-Host "shaderDenoisePrereq.realOutputReady=$($logProof.shaderDenoiseBoundary.prerequisites.realOutputReady)"
    Write-Host "physicalGiEvidencePresent=$($logProof.markers.physicalGiEvidencePresent)"
    Write-Host "physicalGiOverclaimPresent=$($logProof.markers.physicalGiOverclaimPresent)"
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
