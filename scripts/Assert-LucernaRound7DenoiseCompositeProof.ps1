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
        "native[-_ ]?diffuse[-_ ]?gi.*(?:source|output|payload).*ready"
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

    [switch] $RequireShaderDenoiseEvidence
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

    return [ordered]@{
        raw = $RawRoughness
        denoised = $DenoisedRoughness
        meanAbsNeighborLumaReductionPercent = [Math]::Round($meanReduction, 4)
        rmsNeighborLumaReductionPercent = [Math]::Round($rmsReduction, 4)
        roughnessImproved = $meanReduction -ge $MinDenoiseRoughnessReductionPercent
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

function Measure-Round7LogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
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
    $cpuReadbackDenoiseSourcePresent = Test-AnyRegex $log $CpuReadbackDenoiseSourcePatterns
    $shaderDenoiseOutputReadyPresent = (Test-AnyRegex $log $ShaderDenoiseOutputReadyPatterns) -or $realDenoiseShaderOutputPresent
    $shaderDenoiseSourceClaimPresent = Test-AnyRegex $log $ShaderDenoiseSourceClaimPatterns
    $shaderDenoiseOutputOpenPresent = (Test-AnyRegex $log $ShaderDenoiseOutputOpenPatterns) -or $realDenoiseShaderOutputFalsePresent
    $shaderDenoiseOutputStateExplicitPresent = $shaderDenoiseOutputReadyPresent -or $shaderDenoiseOutputOpenPresent
    $shaderDenoiseOverclaimPresent = (Test-AnyRegex $log $ShaderDenoiseOverclaimPatterns) -or ($shaderDenoiseSourceClaimPresent -and $shaderDenoiseOutputOpenPresent -and -not $shaderDenoiseOutputReadyPresent)
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
            cpuReadbackDenoiseSourcePresent = $cpuReadbackDenoiseSourcePresent
            shaderDenoiseOutputReadyPresent = $shaderDenoiseOutputReadyPresent
            shaderDenoiseSourceClaimPresent = $shaderDenoiseSourceClaimPresent
            shaderDenoiseOutputOpenPresent = $shaderDenoiseOutputOpenPresent
            shaderDenoiseOutputStateExplicitPresent = $shaderDenoiseOutputStateExplicitPresent
            shaderDenoiseOverclaimPresent = $shaderDenoiseOverclaimPresent
            proofMarkerPresent = $proofMarkerPresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            submittedFocusWindowOnlyPresent = $submittedFocusWindowOnlyPresent
            submittedRound7GiSourcePresent = $submittedRound7GiSourcePresent
            nativeErrorPresent = $nativeErrorPresent
        }
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
    if ($RequireShaderDenoiseEvidence) {
        if (-not $logProof.markers.shaderDenoiseIntentPresent) {
            $failures.Add("Missing shader-denoise intent marker.")
        }
        if (-not $logProof.markers.shaderDenoiseInputReadyPresent) {
            $failures.Add("Missing shader-denoise input readiness marker.")
        }
        if (-not $logProof.markers.cpuReadbackDenoiseSourcePresent) {
            $failures.Add("Missing CPU/readback denoise source marker; shader-denoise proof must distinguish CPU/readback from shader output.")
        }
        if (-not $logProof.markers.shaderDenoiseOutputStateExplicitPresent) {
            $failures.Add("Missing explicit shader-denoise output readiness marker; proof must show shader output ready or explicitly false/open.")
        }
        if ($logProof.markers.shaderDenoiseOverclaimPresent) {
            $failures.Add("Log over-claims shader-denoise output while shader output readiness is false/open.")
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
            ([double]$denoiseQuality.meanAbsNeighborLumaReductionPercent -ge $MinDenoiseRoughnessReductionPercent)
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
            denoisedGi = [ordered]@{
                imageDeltaPresent = (
                    ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -ge $MinDenoiseChangedPixelPercent) -and
                    ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -ge $MinDenoiseMeanAbsLuma) -and
                    ([double]$denoiseQuality.meanAbsNeighborLumaReductionPercent -ge $MinDenoiseRoughnessReductionPercent)
                )
                dispatchLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoiseDispatchPresent } else { $null }
                outputLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoisedGiOutputPresent } else { $null }
                roughnessReductionPercent = $denoiseQuality.meanAbsNeighborLumaReductionPercent
            }
            shaderDenoise = [ordered]@{
                required = [bool]$RequireShaderDenoiseEvidence
                intentLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseIntentPresent } else { $null }
                inputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseInputReadyPresent } else { $null }
                cpuReadbackSourceLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.cpuReadbackDenoiseSourcePresent } else { $null }
                shaderOutputReadyLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputReadyPresent } else { $null }
                shaderSourceClaimLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseSourceClaimPresent } else { $null }
                shaderOutputOpenLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputOpenPresent } else { $null }
                shaderOutputStateExplicitLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputStateExplicitPresent } else { $null }
                shaderOutputOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOverclaimPresent } else { $null }
                classification = if (-not $RequireShaderDenoiseEvidence) {
                    "not_required"
                } elseif (-not $logProof) {
                    "missing_log"
                } elseif ([bool]$logProof.markers.shaderDenoiseOutputReadyPresent) {
                    "shader_denoise_output_proven"
                } elseif ([bool]$logProof.markers.shaderDenoiseOutputOpenPresent) {
                    "shader_denoise_output_explicitly_open"
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
                cpuReadbackDenoiseSourcePresent = if ($logProof) { [bool]$logProof.markers.cpuReadbackDenoiseSourcePresent } else { $null }
                shaderDenoiseOutputReadyPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputReadyPresent } else { $null }
                shaderDenoiseSourceClaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseSourceClaimPresent } else { $null }
                shaderDenoiseOutputOpenPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOutputOpenPresent } else { $null }
                shaderDenoiseOverclaimPresent = if ($logProof) { [bool]$logProof.markers.shaderDenoiseOverclaimPresent } else { $null }
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
    Write-Host "cpuReadbackDenoiseSourcePresent=$($logProof.markers.cpuReadbackDenoiseSourcePresent)"
    Write-Host "shaderDenoiseOutputReadyPresent=$($logProof.markers.shaderDenoiseOutputReadyPresent)"
    Write-Host "shaderDenoiseSourceClaimPresent=$($logProof.markers.shaderDenoiseSourceClaimPresent)"
    Write-Host "shaderDenoiseOutputOpenPresent=$($logProof.markers.shaderDenoiseOutputOpenPresent)"
    Write-Host "shaderDenoiseOutputStateExplicitPresent=$($logProof.markers.shaderDenoiseOutputStateExplicitPresent)"
    Write-Host "shaderDenoiseOverclaimPresent=$($logProof.markers.shaderDenoiseOverclaimPresent)"
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
