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

    [string] $DebugImagePath = "",

    [string] $LogPath = "",

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 30.0,

    [double] $RegionTopPercent = 20.0,

    [double] $RegionWidthPercent = 40.0,

    [double] $RegionHeightPercent = 55.0,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinRawChangedPixelPercent = 0.5,

    [double] $MinDenoiseChangedPixelPercent = 0.1,

    [double] $MinDenoiseMeanAbsLuma = 0.1,

    [double] $MinFinalChangedPixelPercent = 0.5,

    [double] $MinFinalMeanAbsLuma = 0.5,

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

    [switch] $RequireLogProof,

    [switch] $RequireDebugScreenshot
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

function Invoke-DeltaHelper {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [string] $Label
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round7-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    try {
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
    $rawGiSourcePresent = Test-AnyRegex $log $RawGiSourcePatterns
    $denoiseDispatchPresent = Test-AnyRegex $log $DenoiseDispatchPatterns
    $denoisedGiOutputPresent = Test-AnyRegex $log $DenoisedGiOutputPatterns
    $finalCompositePresent = Test-AnyRegex $log $FinalCompositePatterns
    $hudSafeFinalCompositePresent = Test-AnyRegex $log $HudSafeFinalCompositePatterns
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporary direct-light|current direct-light RGBA payload"
    $metadataOnlyPreviewPresent = Test-Regex $log "metadata-only|metadata scaffold|signal_separated_denoise_metadata_scaffold_no_render_output|no_render_output"
    $firstPracticalCpuOutputPresent = Test-Regex $log "first_practical_cpu_denoised_diffuse_gi_rgba8_generated|denoisedCpuOutputGenerated=true|denoised_cpu_output_generated=true"
    $realDenoiseShaderOutputPresent = Test-Regex $log "realDenoiseShaderOutput=true|real_denoise_shader_output=true"
    $realDenoiseShaderOutputFalsePresent = Test-Regex $log "realDenoiseShaderOutput=false|real_denoise_shader_output=false"
    $proofMarkerPresent = Test-Regex $log "proof marker|R6 GI proof|R7 proof|CPU output proof"
    $focusWindowOnlyPresent = Test-Regex $log "focus-window"
    $nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"

    return [ordered]@{
        markers = [ordered]@{
            rawGiSourcePresent = $rawGiSourcePresent
            denoiseDispatchPresent = $denoiseDispatchPresent
            denoisedGiOutputPresent = $denoisedGiOutputPresent
            finalCompositePresent = $finalCompositePresent
            hudSafeFinalCompositePresent = $hudSafeFinalCompositePresent
            temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
            metadataOnlyPreviewPresent = $metadataOnlyPreviewPresent
            firstPracticalCpuOutputPresent = $firstPracticalCpuOutputPresent
            realDenoiseShaderOutputPresent = $realDenoiseShaderOutputPresent
            realDenoiseShaderOutputFalsePresent = $realDenoiseShaderOutputFalsePresent
            proofMarkerPresent = $proofMarkerPresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            nativeErrorPresent = $nativeErrorPresent
        }
        patterns = [ordered]@{
            rawGiSourcePatterns = @($RawGiSourcePatterns)
            denoiseDispatchPatterns = @($DenoiseDispatchPatterns)
            denoisedGiOutputPatterns = @($DenoisedGiOutputPatterns)
            finalCompositePatterns = @($FinalCompositePatterns)
            hudSafeFinalCompositePatterns = @($HudSafeFinalCompositePatterns)
        }
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$rawResolved = Resolve-ExistingFile $RawGiImagePath "Raw GI image"
$denoisedResolved = Resolve-ExistingFile $DenoisedGiImagePath "Denoised GI image"
$finalResolved = Resolve-ExistingFile $FinalCompositeImagePath "Final composite image"
$debugResolved = Resolve-OptionalFile $DebugImagePath "Debug image"
$logResolved = Resolve-OptionalFile $LogPath "Log"

$baselineDimensions = Get-ImageDimensions $baselineResolved
$rawDimensions = Get-ImageDimensions $rawResolved
$denoisedDimensions = Get-ImageDimensions $denoisedResolved
$finalDimensions = Get-ImageDimensions $finalResolved
$debugDimensions = if ([string]::IsNullOrWhiteSpace($debugResolved)) { $null } else { Get-ImageDimensions $debugResolved }

$rawDelta = Invoke-DeltaHelper $baselineResolved $rawResolved "raw"
$denoiseDelta = Invoke-DeltaHelper $rawResolved $denoisedResolved "denoised"
$finalDelta = Invoke-DeltaHelper $baselineResolved $finalResolved "final"

$logProof = if ([string]::IsNullOrWhiteSpace($logResolved)) { $null } else { Measure-Round7LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
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

if ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -lt $MinRawChangedPixelPercent) {
    $failures.Add("Raw GI focused-region changed pixels below threshold. actual=$($rawDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinRawChangedPixelPercent")
}
if ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -lt $MinDenoiseChangedPixelPercent) {
    $failures.Add("Denoised GI focused-region changed pixels below threshold when compared with raw GI. actual=$($denoiseDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinDenoiseChangedPixelPercent")
}
if ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -lt $MinDenoiseMeanAbsLuma) {
    $failures.Add("Denoised GI focused-region mean absolute luma below threshold when compared with raw GI. actual=$($denoiseDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinDenoiseMeanAbsLuma")
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
if ($logProof) {
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
    if ($logProof.markers.metadataOnlyPreviewPresent -and -not $logProof.markers.firstPracticalCpuOutputPresent) {
        $failures.Add("Log contains metadata-only/scaffold marker without first practical CPU denoised output; Round 7 visual proof must use real raw GI, denoised GI, and final composite outputs.")
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
}

$result = [ordered]@{
    baselineImage = $baselineResolved
    rawGiImage = $rawResolved
    denoisedGiImage = $denoisedResolved
    finalCompositeImage = $finalResolved
    debugImage = $debugResolved
    logPath = $logResolved
    thresholds = [ordered]@{
        minRawChangedPixelPercent = $MinRawChangedPixelPercent
        minDenoiseChangedPixelPercent = $MinDenoiseChangedPixelPercent
        minDenoiseMeanAbsLuma = $MinDenoiseMeanAbsLuma
        minFinalChangedPixelPercent = $MinFinalChangedPixelPercent
        minFinalMeanAbsLuma = $MinFinalMeanAbsLuma
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        requireLogProof = [bool]$RequireLogProof
        requireDebugScreenshot = [bool]$RequireDebugScreenshot
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        rawDimensions = $rawDimensions
        denoisedDimensions = $denoisedDimensions
        finalDimensions = $finalDimensions
        debugDimensions = $debugDimensions
    }
    imageDelta = [ordered]@{
        baselineToRawGi = $rawDelta
        rawGiToDenoisedGi = $denoiseDelta
        baselineToFinalComposite = $finalDelta
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round7_raw_denoised_final_evidence_passed" } else { "round7_evidence_failed" }
        rawGiEvidencePresent = ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -ge $MinRawChangedPixelPercent)
        denoiseComparisonPresent = (
            ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -ge $MinDenoiseChangedPixelPercent) -and
            ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -ge $MinDenoiseMeanAbsLuma)
        )
        finalCompositeEvidencePresent = (
            ([double]$finalDelta.focusRegionMetrics.changedPixelPercent -ge $MinFinalChangedPixelPercent) -and
            ([double]$finalDelta.focusRegionMetrics.meanAbsLuma -ge $MinFinalMeanAbsLuma)
        )
        tracks = [ordered]@{
            rawGi = [ordered]@{
                imageDeltaPresent = ([double]$rawDelta.focusRegionMetrics.changedPixelPercent -ge $MinRawChangedPixelPercent)
                logMarkerPresent = if ($logProof) { [bool]$logProof.markers.rawGiSourcePresent } else { $null }
            }
            denoisedGi = [ordered]@{
                imageDeltaPresent = (
                    ([double]$denoiseDelta.focusRegionMetrics.changedPixelPercent -ge $MinDenoiseChangedPixelPercent) -and
                    ([double]$denoiseDelta.focusRegionMetrics.meanAbsLuma -ge $MinDenoiseMeanAbsLuma)
                )
                dispatchLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoiseDispatchPresent } else { $null }
                outputLogMarkerPresent = if ($logProof) { [bool]$logProof.markers.denoisedGiOutputPresent } else { $null }
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
Write-Host "rawGiImage=$($result.rawGiImage)"
Write-Host "denoisedGiImage=$($result.denoisedGiImage)"
Write-Host "finalCompositeImage=$($result.finalCompositeImage)"
Write-Host "debugImage=$($result.debugImage)"
Write-Host "logPath=$($result.logPath)"
Write-Host "raw.focus.changedPixelPercent=$($rawDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "denoise.focus.changedPixelPercent=$($denoiseDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "denoise.focus.meanAbsLuma=$($denoiseDelta.focusRegionMetrics.meanAbsLuma)"
Write-Host "final.focus.changedPixelPercent=$($finalDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "final.focus.meanAbsLuma=$($finalDelta.focusRegionMetrics.meanAbsLuma)"
if ($logProof) {
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
