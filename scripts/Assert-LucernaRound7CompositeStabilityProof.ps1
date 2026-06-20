<#
.SYNOPSIS
Controller-only Round 7 assertion helper for final-composite stability with particles, translucency, and temporal motion.

.DESCRIPTION
This script checks already captured screenshots and an optional controller launch log. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller captures Round7CompositeStability artifacts.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory = $true)]
    [string] $ParticleBaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $ParticleFinalCompositeImagePath,

    [Parameter(Mandatory = $true)]
    [string] $TranslucentBaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $TranslucentFinalCompositeImagePath,

    [Parameter(Mandatory = $true)]
    [string] $TemporalStableImagePath,

    [Parameter(Mandatory = $true)]
    [string] $TemporalMovedImagePath,

    [string[]] $TemporalStableSequenceImagePath = @(),

    [string[]] $TemporalMovedSequenceImagePath = @(),

    [string[]] $TemporalCaptureLabels = @(),

    [string[]] $TemporalCaptureManifestJsonPath = @(),

    [string[]] $LogPath = @(),

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 10.0,

    [double] $RegionTopPercent = 12.0,

    [double] $RegionWidthPercent = 80.0,

    [double] $RegionHeightPercent = 58.0,

    [switch] $DisableAutoFocusRegion,

    [switch] $AutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 10.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 60.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

    [switch] $DisableTemporalMotionAutoFocusRegion,

    [double] $TemporalMotionSearchLeftPercent = 4.0,

    [double] $TemporalMotionSearchTopPercent = 8.0,

    [double] $TemporalMotionSearchWidthPercent = 92.0,

    [double] $TemporalMotionSearchHeightPercent = 56.0,

    [int] $TemporalMotionRegionColumns = 16,

    [int] $TemporalMotionRegionRows = 8,

    [int] $TemporalMotionRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinParticleChangedPixelPercent = 0.1,

    [double] $MinTranslucentChangedPixelPercent = 0.1,

    [double] $MinTemporalChangedPixelPercent = 0.5,

    [double] $MinTemporalMeanAbsLuma = 0.5,

    [double] $MaxStableTemporalDriftChangedPixelPercent = 5.0,

    [double] $MaxStableTemporalDriftMeanAbsLuma = 1.5,

    [double] $MaxMovedTemporalFlickerChangedPixelPercent = 35.0,

    [double] $MaxMovedTemporalFlickerMeanAbsLuma = 8.0,

    [double] $MaxStableTemporalRoughnessScore = 2.0,

    [double] $MaxMovedTemporalRoughnessScore = 9.0,

    [double] $MinSceneColorVariance = 8.0,

    [double] $MinSequenceSceneColorVariance = 8.0,

    [double] $MaxRegionBottomPercent = 72.0,

    [switch] $RejectWindowScreenshotSources,

    [string[]] $ScreenshotSource = @(),

    [switch] $RequireLogProof,

    [string[]] $FinalCompositePatterns = @(
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
        "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*finalBlendComplete=true",
        "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready",
        "sourceAuthenticity=accepted:final-composite-direct-plus-raw-gi-plus-denoised-gi"
    ),

    [string[]] $HudPreservationPatterns = @(
        "round7\.finalCompositeHudSafe=true",
        "hudSafeFinalComposite=true",
        "HUD-safe final composite",
        "before hand/HUD composition",
        "HUD remains readable"
    ),

    [string[]] $TemporalHistoryPatterns = @(
        "round7\.stability\.scene=temporal",
        "temporalSceneMarker=true",
        "movedCameraTemporalPair=true",
        "historyStableSceneMarker=true",
        "historyMovedSceneMarker=true",
        "Lucerna Round 8 history confidence",
        "round8\.historyConfidence",
        "historyAccepted=[0-9]+.*historyRejected=[0-9]+",
        "historyRejected=[1-9][0-9]*",
        "stablePixels=[0-9]+",
        "unstablePixels=[0-9]+",
        "flickerScore=[0-9]+(?:\.[0-9]+)?",
        "ghostingRisk=(?:low|medium|high|[0-9]+(?:\.[0-9]+)?)",
        "temporalReadiness=(?:ready|not-ready|pending|open|true|false)",
        "sceneState=(?:stable|moved|disoccluded|moved-disoccluded)"
    ),

    [string[]] $ParticleScenePatterns = @(
        "round7\.stability\.scene=particles",
        "particleSceneMarker=true",
        "round7\.stability\.particleBurst=true",
        "particles-final-composite"
    ),

    [string[]] $TranslucencyScenePatterns = @(
        "round7\.stability\.scene=translucency",
        "translucentSceneMarker=true",
        "glassWaterSceneMarker=true",
        "translucency-final-composite"
    )
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

function Resolve-OptionalFiles {
    param(
        [string[]] $Paths,
        [string] $Label
    )

    $resolved = New-Object System.Collections.Generic.List[string]
    foreach ($path in $Paths) {
        if ([string]::IsNullOrWhiteSpace($path)) {
            continue
        }
        $resolved.Add((Resolve-ExistingFile $path $Label)) | Out-Null
    }
    return $resolved.ToArray()
}

function Read-OptionalJsonFiles {
    param(
        [string[]] $Paths,
        [string] $Label
    )

    $resolved = Resolve-OptionalFiles $Paths $Label
    $items = New-Object System.Collections.Generic.List[object]
    foreach ($path in $resolved) {
        $items.Add([ordered]@{
            path = $path
            manifest = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
        }) | Out-Null
    }
    return $items.ToArray()
}

function Resolve-TemporalSequenceFiles {
    param(
        [string[]] $Paths,
        [string] $FirstPath,
        [string] $Label
    )

    $resolved = New-Object System.Collections.Generic.List[string]
    if ($Paths.Count -gt 0) {
        foreach ($path in $Paths) {
            if ([string]::IsNullOrWhiteSpace($path)) {
                continue
            }
            $resolved.Add((Resolve-ExistingFile $path $Label)) | Out-Null
        }
    }
    if ($resolved.Count -eq 0) {
        $resolved.Add($FirstPath) | Out-Null
    } elseif ($resolved[0] -ne $FirstPath) {
        $withFirst = New-Object System.Collections.Generic.List[string]
        $withFirst.Add($FirstPath) | Out-Null
        foreach ($path in $resolved) {
            if ($path -ne $FirstPath) {
                $withFirst.Add($path) | Out-Null
            }
        }
        $resolved = $withFirst
    }
    return $resolved.ToArray()
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round7-stability-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    $compareArgs = @{
        BaselineImagePath = $BaselinePath
        EnabledImagePath = $EnabledPath
        RegionLeftPercent = $RegionLeftPercent
        RegionTopPercent = $RegionTopPercent
        RegionWidthPercent = $RegionWidthPercent
        RegionHeightPercent = $RegionHeightPercent
        ChangedPixelThreshold = $ChangedPixelThreshold
        BrightPixelThreshold = $BrightPixelThreshold
        OutputJsonPath = $tempJson
    }
    $useTemporalMotionRegion = $Label -eq "temporal-motion" -and -not $DisableTemporalMotionAutoFocusRegion
    if ($useTemporalMotionRegion) {
        $compareArgs.AutoFocusRegion = $true
        $compareArgs.AutoRegionSearchLeftPercent = $TemporalMotionSearchLeftPercent
        $compareArgs.AutoRegionSearchTopPercent = $TemporalMotionSearchTopPercent
        $compareArgs.AutoRegionSearchWidthPercent = $TemporalMotionSearchWidthPercent
        $compareArgs.AutoRegionSearchHeightPercent = $TemporalMotionSearchHeightPercent
        $compareArgs.AutoRegionColumns = $TemporalMotionRegionColumns
        $compareArgs.AutoRegionRows = $TemporalMotionRegionRows
        $compareArgs.AutoRegionPaddingCells = $TemporalMotionRegionPaddingCells
        $compareArgs.TileColumns = $TemporalMotionRegionColumns
        $compareArgs.TileRows = $TemporalMotionRegionRows
    } elseif ($AutoFocusRegion -and -not $DisableAutoFocusRegion) {
        $compareArgs.AutoFocusRegion = $true
        $compareArgs.AutoRegionSearchLeftPercent = $AutoRegionSearchLeftPercent
        $compareArgs.AutoRegionSearchTopPercent = $AutoRegionSearchTopPercent
        $compareArgs.AutoRegionSearchWidthPercent = $AutoRegionSearchWidthPercent
        $compareArgs.AutoRegionSearchHeightPercent = $AutoRegionSearchHeightPercent
        $compareArgs.AutoRegionColumns = $AutoRegionColumns
        $compareArgs.AutoRegionRows = $AutoRegionRows
        $compareArgs.AutoRegionPaddingCells = $AutoRegionPaddingCells
    }

    & $compareScript @compareArgs | Out-Null
    try {
        return Get-Content -Raw -LiteralPath $tempJson | ConvertFrom-Json
    } finally {
        Remove-Item -LiteralPath $tempJson -Force -ErrorAction SilentlyContinue
    }
}

function Get-TemporalCaptureLabel {
    param(
        [int] $Index,
        [string] $FallbackPrefix
    )

    if ($Index -lt $TemporalCaptureLabels.Count -and -not [string]::IsNullOrWhiteSpace($TemporalCaptureLabels[$Index])) {
        return $TemporalCaptureLabels[$Index]
    }
    return ("{0}-{1:D2}" -f $FallbackPrefix, ($Index + 1))
}

function Measure-TemporalSequenceDrift {
    param(
        [string[]] $Paths,
        [string] $LabelPrefix
    )

    $comparisons = New-Object System.Collections.Generic.List[object]
    $maxChangedPixelPercent = 0.0
    $maxMeanAbsLuma = 0.0
    $maxRmseLuma = 0.0
    $sumChangedPixelPercent = 0.0
    $sumMeanAbsLuma = 0.0
    $sumRmseLuma = 0.0
    $sumTileStdDev = 0.0
    $maxTileStdDev = 0.0

    for ($index = 1; $index -lt $Paths.Count; $index++) {
        $previousLabel = Get-TemporalCaptureLabel ($index - 1) $LabelPrefix
        $currentLabel = Get-TemporalCaptureLabel $index $LabelPrefix
        $delta = Invoke-DeltaHelper $Paths[$index - 1] $Paths[$index] ("$LabelPrefix-$index")
        $metrics = $delta.focusRegionMetrics
        $maxChangedPixelPercent = [Math]::Max($maxChangedPixelPercent, [double]$metrics.changedPixelPercent)
        $maxMeanAbsLuma = [Math]::Max($maxMeanAbsLuma, [double]$metrics.meanAbsLuma)
        $maxRmseLuma = [Math]::Max($maxRmseLuma, [double]$metrics.rmseLuma)
        $tileStdDev = if ($delta.focusRegionShape -and $delta.focusRegionShape.tileMetrics) {
            [double]$delta.focusRegionShape.tileMetrics.stdDevTileChangedPixelPercent
        } else {
            0.0
        }
        $sumChangedPixelPercent += [double]$metrics.changedPixelPercent
        $sumMeanAbsLuma += [double]$metrics.meanAbsLuma
        $sumRmseLuma += [double]$metrics.rmseLuma
        $sumTileStdDev += $tileStdDev
        $maxTileStdDev = [Math]::Max($maxTileStdDev, $tileStdDev)
        $comparisons.Add([ordered]@{
            fromLabel = $previousLabel
            toLabel = $currentLabel
            fromImage = $Paths[$index - 1]
            toImage = $Paths[$index]
            delta = $delta
        }) | Out-Null
    }

    $pairCount = [Math]::Max(1.0, [double]$comparisons.Count)
    $averageChangedPixelPercent = $sumChangedPixelPercent / $pairCount
    $averageMeanAbsLuma = $sumMeanAbsLuma / $pairCount
    $averageRmseLuma = $sumRmseLuma / $pairCount
    $averageTileStdDev = $sumTileStdDev / $pairCount
    $roughnessScore = $averageRmseLuma + ($maxTileStdDev * 0.1) + ($averageChangedPixelPercent * 0.05)
    $labels = @(for ($index = 0; $index -lt $Paths.Count; $index++) {
        Get-TemporalCaptureLabel $index $LabelPrefix
    })
    $comparisonArray = @($comparisons.ToArray())

    return [ordered]@{
        captureCount = $Paths.Count
        pairCount = $comparisons.Count
        maxChangedPixelPercent = [Math]::Round($maxChangedPixelPercent, 4)
        maxMeanAbsLuma = [Math]::Round($maxMeanAbsLuma, 4)
        maxRmseLuma = [Math]::Round($maxRmseLuma, 4)
        averageChangedPixelPercent = [Math]::Round($averageChangedPixelPercent, 4)
        averageMeanAbsLuma = [Math]::Round($averageMeanAbsLuma, 4)
        averageRmseLuma = [Math]::Round($averageRmseLuma, 4)
        averageTileChangedPixelStdDev = [Math]::Round($averageTileStdDev, 4)
        maxTileChangedPixelStdDev = [Math]::Round($maxTileStdDev, 4)
        roughnessScore = [Math]::Round($roughnessScore, 4)
        labels = @($labels)
        consecutiveComparisons = $comparisonArray
    }
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

function Measure-SceneColorVariance {
    param([string] $Path)

    Add-Type -AssemblyName System.Drawing
    $image = [System.Drawing.Bitmap]::new($Path)
    try {
        $sampleCount = 0
        $sum = 0.0
        $sumSq = 0.0
        $stepX = [Math]::Max(1, [int][Math]::Floor($image.Width / 32))
        $stepY = [Math]::Max(1, [int][Math]::Floor($image.Height / 18))
        for ($y = 0; $y -lt $image.Height; $y += $stepY) {
            for ($x = 0; $x -lt $image.Width; $x += $stepX) {
                $pixel = $image.GetPixel($x, $y)
                $luma = (0.2126 * [int]$pixel.R) + (0.7152 * [int]$pixel.G) + (0.0722 * [int]$pixel.B)
                $sampleCount++
                $sum += $luma
                $sumSq += ($luma * $luma)
            }
        }
        if ($sampleCount -le 0) {
            throw "Cannot measure scene variance for an empty image: $Path"
        }
        $mean = $sum / [double]$sampleCount
        $variance = [Math]::Max(0.0, ($sumSq / [double]$sampleCount) - ($mean * $mean))
        return [ordered]@{
            sampleCount = $sampleCount
            meanLuma = [Math]::Round($mean, 4)
            stdDevLuma = [Math]::Round([Math]::Sqrt($variance), 4)
        }
    } finally {
        $image.Dispose()
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

function Get-FirstRegexValue {
    param(
        [string] $Text,
        [string[]] $Patterns
    )

    foreach ($pattern in $Patterns) {
        $match = [regex]::Match($Text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        if ($match.Success -and $match.Groups.Count -gt 1) {
            return $match.Groups[1].Value
        }
    }
    return $null
}

function Get-TemporalTelemetryFields {
    param([string] $Log)

    $stablePixels = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.stablePixels|temporalStablePixels|stablePixels)=([0-9]+)",
        "(?:stablePixelCount|stable_pixels)=([0-9]+)"
    )
    $unstablePixels = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.unstablePixels|temporalUnstablePixels|unstablePixels)=([0-9]+)",
        "(?:unstablePixelCount|unstable_pixels)=([0-9]+)"
    )
    $frameDelta = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.frameDeltaMs|temporalFrameDeltaMs|frameDeltaMs)=([0-9]+(?:\.[0-9]+)?)",
        "(?:round7\.temporal\.frameDelta|temporalFrameDelta|frameDelta)=([0-9]+(?:\.[0-9]+)?)"
    )
    $historyConfidence = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.historyConfidence|temporalHistoryConfidence|historyConfidence)=([0-9]+(?:\.[0-9]+)?)",
        "(?:Lucerna Round 8 history confidence|round8\.historyConfidence)[^`r`n]*(?:value|confidence(?:Map)?)=([0-9]+(?:\.[0-9]+)?)"
    )
    $flickerScore = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.flickerScore|temporalFlickerScore|flickerScore)=([0-9]+(?:\.[0-9]+)?)",
        "(?:flicker_score)=([0-9]+(?:\.[0-9]+)?)"
    )
    $ghostingRisk = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.ghostingRisk|temporalGhostingRisk|ghostingRisk)=([A-Za-z0-9_.-]+)",
        "(?:ghosting_risk)=([A-Za-z0-9_.-]+)"
    )
    $temporalReadiness = Get-FirstRegexValue $Log @(
        "(?:round7\.temporal\.readiness|temporalReadiness|temporalReadyState)=([A-Za-z0-9_.-]+)",
        "(?:temporalReady|round7\.temporal\.ready)=(true|false)"
    )

    return [ordered]@{
        stablePixels = if ($null -ne $stablePixels) { [int64]$stablePixels } else { $null }
        unstablePixels = if ($null -ne $unstablePixels) { [int64]$unstablePixels } else { $null }
        frameDelta = if ($null -ne $frameDelta) { [double]$frameDelta } else { $null }
        historyConfidence = if ($null -ne $historyConfidence) { [double]$historyConfidence } else { $null }
        flickerScore = if ($null -ne $flickerScore) { [double]$flickerScore } else { $null }
        ghostingRisk = $ghostingRisk
        temporalReadiness = $temporalReadiness
        present = [ordered]@{
            stablePixels = $null -ne $stablePixels
            unstablePixels = $null -ne $unstablePixels
            frameDelta = $null -ne $frameDelta
            historyConfidence = $null -ne $historyConfidence
            flickerScore = $null -ne $flickerScore
            ghostingRisk = $null -ne $ghostingRisk
            temporalReadiness = $null -ne $temporalReadiness
        }
    }
}

function Measure-Round7CompositeStabilityLogProof {
    param([string[]] $ResolvedLogPaths)

    $logParts = New-Object System.Collections.Generic.List[string]
    foreach ($path in $ResolvedLogPaths) {
        $logParts.Add((Get-Content -Raw -LiteralPath $path)) | Out-Null
    }
    $log = $logParts -join "`n"
    $finalCompositePresent = Test-AnyRegex $log $FinalCompositePatterns
    $hudPreservationPresent = Test-AnyRegex $log $HudPreservationPatterns
    $temporalHistoryPresent = Test-AnyRegex $log $TemporalHistoryPatterns
    $temporalStableScenePresent = Test-Regex $log "historyStableSceneMarker=true|sceneState=stable"
    $temporalMovedScenePresent = Test-Regex $log "historyMovedSceneMarker=true|movedCameraTemporalPair=true|sceneState=(?:moved|disoccluded|moved-disoccluded)"
    $particleScenePresent = Test-AnyRegex $log $ParticleScenePatterns
    $translucencyScenePresent = Test-AnyRegex $log $TranslucencyScenePatterns
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporaryDirectLightSubstitution=true|using the current direct-light RGBA payload as the temporary visible source"
    $proofMarkerPresent = Test-Regex $log "proofMarkerSource=true|cpuOutputProofMarker=true|round5-direct-proof|round6-gi-proof|round7-proof-marker|R5 visual proof|R6 GI proof|R7 proof|CPU output proof"
    $focusWindowOnlyPresent = Test-Regex $log "focusWindowOnly(?:Submitted)?=true|focus_window_only=true|sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|mode=final-composite-direct-light-focus-window-additive|round6-diffuse-gi-focus-window-additive"
    $wrongWindowScreenshotPresent = Test-Regex $log "screenshotSource=(?:window|window-fallback)|temporalCaptureSources=[^`r`n]*(?:window|window-fallback)"
    $nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|VK_[A-Z_]*ERROR|Lucerna native error|native error|Vulkan error"
    $temporalTelemetry = Get-TemporalTelemetryFields $log

    return [ordered]@{
        markers = [ordered]@{
            finalCompositePresent = $finalCompositePresent
            hudPreservationPresent = $hudPreservationPresent
            temporalHistoryPresent = $temporalHistoryPresent
            temporalStableScenePresent = $temporalStableScenePresent
            temporalMovedScenePresent = $temporalMovedScenePresent
            particleScenePresent = $particleScenePresent
            translucencyScenePresent = $translucencyScenePresent
            temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
            proofMarkerPresent = $proofMarkerPresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            wrongWindowScreenshotPresent = $wrongWindowScreenshotPresent
            nativeErrorPresent = $nativeErrorPresent
        }
        temporalTelemetry = $temporalTelemetry
        patterns = [ordered]@{
            finalCompositePatterns = @($FinalCompositePatterns)
            hudPreservationPatterns = @($HudPreservationPatterns)
            temporalHistoryPatterns = @($TemporalHistoryPatterns)
            particleScenePatterns = @($ParticleScenePatterns)
            translucencyScenePatterns = @($TranslucencyScenePatterns)
        }
    }
}

$particleBaselineResolved = Resolve-ExistingFile $ParticleBaselineImagePath "Particle baseline image"
$particleFinalResolved = Resolve-ExistingFile $ParticleFinalCompositeImagePath "Particle final-composite image"
$translucentBaselineResolved = Resolve-ExistingFile $TranslucentBaselineImagePath "Translucent baseline image"
$translucentFinalResolved = Resolve-ExistingFile $TranslucentFinalCompositeImagePath "Translucent final-composite image"
$temporalStableResolved = Resolve-ExistingFile $TemporalStableImagePath "Temporal stable image"
$temporalMovedResolved = Resolve-ExistingFile $TemporalMovedImagePath "Temporal moved image"
$temporalStableSequenceResolved = Resolve-TemporalSequenceFiles `
    -Paths $TemporalStableSequenceImagePath `
    -FirstPath $temporalStableResolved `
    -Label "Temporal stable sequence image"
$temporalMovedSequenceResolved = Resolve-TemporalSequenceFiles `
    -Paths $TemporalMovedSequenceImagePath `
    -FirstPath $temporalMovedResolved `
    -Label "Temporal moved sequence image"
$temporalCaptureManifests = Read-OptionalJsonFiles $TemporalCaptureManifestJsonPath "Temporal capture manifest"
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$particleBaselineDimensions = Get-ImageDimensions $particleBaselineResolved
$particleFinalDimensions = Get-ImageDimensions $particleFinalResolved
$translucentBaselineDimensions = Get-ImageDimensions $translucentBaselineResolved
$translucentFinalDimensions = Get-ImageDimensions $translucentFinalResolved
$temporalStableDimensions = Get-ImageDimensions $temporalStableResolved
$temporalMovedDimensions = Get-ImageDimensions $temporalMovedResolved

$particleDelta = Invoke-DeltaHelper $particleBaselineResolved $particleFinalResolved "particles"
$translucentDelta = Invoke-DeltaHelper $translucentBaselineResolved $translucentFinalResolved "translucency"
$temporalDelta = Invoke-DeltaHelper $temporalStableResolved $temporalMovedResolved "temporal-motion"
$stableTemporalDrift = if ($temporalStableSequenceResolved.Count -ge 2) {
    Measure-TemporalSequenceDrift -Paths $temporalStableSequenceResolved -LabelPrefix "temporal-stable"
} else {
    $null
}
$movedTemporalFlicker = if ($temporalMovedSequenceResolved.Count -ge 2) {
    Measure-TemporalSequenceDrift -Paths $temporalMovedSequenceResolved -LabelPrefix "temporal-moved"
} else {
    $null
}

$sceneVariance = [ordered]@{
    particleBaseline = Measure-SceneColorVariance $particleBaselineResolved
    particleFinalComposite = Measure-SceneColorVariance $particleFinalResolved
    translucentBaseline = Measure-SceneColorVariance $translucentBaselineResolved
    translucentFinalComposite = Measure-SceneColorVariance $translucentFinalResolved
    temporalStable = Measure-SceneColorVariance $temporalStableResolved
    temporalMoved = Measure-SceneColorVariance $temporalMovedResolved
}
$sequenceSceneVariance = [ordered]@{
    temporalStableSequence = @(for ($index = 0; $index -lt $temporalStableSequenceResolved.Count; $index++) {
        [ordered]@{
            index = $index
            image = $temporalStableSequenceResolved[$index]
            variance = Measure-SceneColorVariance $temporalStableSequenceResolved[$index]
        }
    })
    temporalMovedSequence = @(for ($index = 0; $index -lt $temporalMovedSequenceResolved.Count; $index++) {
        [ordered]@{
            index = $index
            image = $temporalMovedSequenceResolved[$index]
            variance = Measure-SceneColorVariance $temporalMovedSequenceResolved[$index]
        }
    })
}

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round7CompositeStabilityLogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

$regionBottomPercent = $RegionTopPercent + $RegionHeightPercent
$autoSearchBottomPercent = $AutoRegionSearchTopPercent + $AutoRegionSearchHeightPercent
$temporalMotionSearchBottomPercent = $TemporalMotionSearchTopPercent + $TemporalMotionSearchHeightPercent
if ((-not $AutoFocusRegion -or $DisableAutoFocusRegion) -and $regionBottomPercent -gt $MaxRegionBottomPercent) {
    $failures.Add("Fixed proof region extends into lower HUD/hand area. bottomPercent=$regionBottomPercent max=$MaxRegionBottomPercent")
}
if ($AutoFocusRegion -and -not $DisableAutoFocusRegion -and $autoSearchBottomPercent -gt $MaxRegionBottomPercent) {
    $failures.Add("Auto-focus search region extends into lower HUD/hand area. bottomPercent=$autoSearchBottomPercent max=$MaxRegionBottomPercent")
}
if (-not $DisableTemporalMotionAutoFocusRegion -and $temporalMotionSearchBottomPercent -gt $MaxRegionBottomPercent) {
    $failures.Add("Temporal motion auto-focus search region extends into lower HUD/hand area. bottomPercent=$temporalMotionSearchBottomPercent max=$MaxRegionBottomPercent")
}

$baselineWidth = $particleBaselineDimensions.width
$baselineHeight = $particleBaselineDimensions.height
foreach ($entry in @(
    @{ label = "particle final composite"; dimensions = $particleFinalDimensions },
    @{ label = "translucent baseline"; dimensions = $translucentBaselineDimensions },
    @{ label = "translucent final composite"; dimensions = $translucentFinalDimensions },
    @{ label = "temporal stable"; dimensions = $temporalStableDimensions },
    @{ label = "temporal moved"; dimensions = $temporalMovedDimensions }
)) {
    if (($entry.dimensions.width -ne $baselineWidth) -or ($entry.dimensions.height -ne $baselineHeight)) {
        $failures.Add("$($entry.label) image dimensions differ from particle baseline. baseline=${baselineWidth}x${baselineHeight} actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}
foreach ($path in @($temporalStableSequenceResolved + $temporalMovedSequenceResolved)) {
    $dimensions = Get-ImageDimensions $path
    if (($dimensions.width -ne $baselineWidth) -or ($dimensions.height -ne $baselineHeight)) {
        $failures.Add("Temporal sequence image dimensions differ from particle baseline. image=$path baseline=${baselineWidth}x${baselineHeight} actual=$($dimensions.width)x$($dimensions.height)")
    }
}

foreach ($entry in $sceneVariance.GetEnumerator()) {
    if ([double]$entry.Value.stdDevLuma -lt $MinSceneColorVariance) {
        $failures.Add("$($entry.Key) image appears too flat or blank. stdDevLuma=$($entry.Value.stdDevLuma) expected>=$MinSceneColorVariance")
    }
}
foreach ($entry in @($sequenceSceneVariance.temporalStableSequence + $sequenceSceneVariance.temporalMovedSequence)) {
    if ([double]$entry.variance.stdDevLuma -lt $MinSequenceSceneColorVariance) {
        $failures.Add("Temporal sequence image appears too flat or blank/wrong-surface. image=$($entry.image) stdDevLuma=$($entry.variance.stdDevLuma) expected>=$MinSequenceSceneColorVariance")
    }
}
foreach ($source in $ScreenshotSource) {
    if ([string]::IsNullOrWhiteSpace($source)) {
        continue
    }
    if ($RejectWindowScreenshotSources -and $source -match "^(?:window|window-fallback)$") {
        $failures.Add("Screenshot source '$source' is rejected for temporal/flicker proof. Use Minecraft F2 or in-client capture, preferably in-client.")
    }
}

if ([double]$particleDelta.focusRegionMetrics.changedPixelPercent -lt $MinParticleChangedPixelPercent) {
    $failures.Add("Particle final-composite focused-region changed pixels below threshold. actual=$($particleDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinParticleChangedPixelPercent")
}
if ([double]$translucentDelta.focusRegionMetrics.changedPixelPercent -lt $MinTranslucentChangedPixelPercent) {
    $failures.Add("Translucency final-composite focused-region changed pixels below threshold. actual=$($translucentDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinTranslucentChangedPixelPercent")
}
if ([double]$temporalDelta.focusRegionMetrics.changedPixelPercent -lt $MinTemporalChangedPixelPercent) {
    $failures.Add("Temporal moved-camera focused-region changed pixels below threshold. actual=$($temporalDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinTemporalChangedPixelPercent")
    $autoReason = if ($temporalDelta.focusRegion.autoSelection) { [string]$temporalDelta.focusRegion.autoSelection.reason } else { "no-auto-selection-diagnostics" }
    $temporalSelectionLabel = if (-not $DisableTemporalMotionAutoFocusRegion) { "auto-temporal-motion-upper-world-surface" } else { "generic-round7-focus-region" }
    $failures.Add("Temporal moved-camera region did not show enough motion/disocclusion. selectionMode=$($temporalDelta.focusRegion.selectionMode) autoReason=$autoReason sourceRegion=$temporalSelectionLabel hint=check stale screenshots, static camera, blank surface, or HUD/hand-contaminated capture.")
}
if ([double]$temporalDelta.focusRegionMetrics.meanAbsLuma -lt $MinTemporalMeanAbsLuma) {
    $failures.Add("Temporal moved-camera focused-region mean absolute luma below threshold. actual=$($temporalDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinTemporalMeanAbsLuma")
}
if ($stableTemporalDrift) {
    if ([double]$stableTemporalDrift.maxChangedPixelPercent -gt $MaxStableTemporalDriftChangedPixelPercent) {
        $failures.Add("Stable temporal repeated-capture changed pixels exceed flicker threshold. actual=$($stableTemporalDrift.maxChangedPixelPercent) expected<=$MaxStableTemporalDriftChangedPixelPercent")
    }
    if ([double]$stableTemporalDrift.maxMeanAbsLuma -gt $MaxStableTemporalDriftMeanAbsLuma) {
        $failures.Add("Stable temporal repeated-capture mean absolute luma exceeds flicker threshold. actual=$($stableTemporalDrift.maxMeanAbsLuma) expected<=$MaxStableTemporalDriftMeanAbsLuma")
    }
    if ([double]$stableTemporalDrift.roughnessScore -gt $MaxStableTemporalRoughnessScore) {
        $failures.Add("Stable temporal repeated-capture roughness score exceeds threshold. actual=$($stableTemporalDrift.roughnessScore) expected<=$MaxStableTemporalRoughnessScore")
    }
}
if ($movedTemporalFlicker) {
    if ([double]$movedTemporalFlicker.maxChangedPixelPercent -gt $MaxMovedTemporalFlickerChangedPixelPercent) {
        $failures.Add("Moved temporal repeated-capture changed pixels exceed flicker threshold. actual=$($movedTemporalFlicker.maxChangedPixelPercent) expected<=$MaxMovedTemporalFlickerChangedPixelPercent")
    }
    if ([double]$movedTemporalFlicker.maxMeanAbsLuma -gt $MaxMovedTemporalFlickerMeanAbsLuma) {
        $failures.Add("Moved temporal repeated-capture mean absolute luma exceeds flicker threshold. actual=$($movedTemporalFlicker.maxMeanAbsLuma) expected<=$MaxMovedTemporalFlickerMeanAbsLuma")
    }
    if ([double]$movedTemporalFlicker.roughnessScore -gt $MaxMovedTemporalRoughnessScore) {
        $failures.Add("Moved temporal repeated-capture roughness score exceeds threshold. actual=$($movedTemporalFlicker.roughnessScore) expected<=$MaxMovedTemporalRoughnessScore")
    }
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.finalCompositePresent) {
        $failures.Add("Missing Round 7 final composite dispatch/submission log marker.")
    }
    if (-not $logProof.markers.hudPreservationPresent) {
        $failures.Add("Missing HUD preservation/final-composite-before-HUD log marker.")
    }
    if (-not $logProof.markers.temporalHistoryPresent) {
        $failures.Add("Missing temporal/history log marker for stable or moved camera capture.")
    }
    if (-not $logProof.markers.temporalStableScenePresent) {
        $failures.Add("Missing temporal stable-camera scene marker.")
    }
    if (-not $logProof.markers.temporalMovedScenePresent) {
        $failures.Add("Missing temporal moved-camera scene marker.")
    }
    if (-not $logProof.markers.particleScenePresent) {
        $failures.Add("Missing particle scene marker.")
    }
    if (-not $logProof.markers.translucencyScenePresent) {
        $failures.Add("Missing translucency/glass-water scene marker.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; composite stability proof must not use temporary direct-light payload substitution.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker evidence; composite stability proof must use real capture scenes without marker overlays.")
    }
    if ($logProof.markers.focusWindowOnlyPresent) {
        $failures.Add("Log contains focus-window marker; composite stability proof must not rely on focus-window-only brightness.")
    }
    if ($RejectWindowScreenshotSources -and $logProof.markers.wrongWindowScreenshotPresent) {
        $failures.Add("Log contains window or window-fallback screenshot source marker; temporal/flicker proof must use Minecraft-owned capture.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$result = [ordered]@{
    particleBaselineImage = $particleBaselineResolved
    particleFinalCompositeImage = $particleFinalResolved
    translucentBaselineImage = $translucentBaselineResolved
    translucentFinalCompositeImage = $translucentFinalResolved
    temporalStableImage = $temporalStableResolved
    temporalMovedImage = $temporalMovedResolved
    temporalStableSequenceImages = @($temporalStableSequenceResolved)
    temporalMovedSequenceImages = @($temporalMovedSequenceResolved)
    temporalCaptureManifests = @($temporalCaptureManifests)
    screenshotSources = @($ScreenshotSource)
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minParticleChangedPixelPercent = $MinParticleChangedPixelPercent
        minTranslucentChangedPixelPercent = $MinTranslucentChangedPixelPercent
        minTemporalChangedPixelPercent = $MinTemporalChangedPixelPercent
        minTemporalMeanAbsLuma = $MinTemporalMeanAbsLuma
        maxStableTemporalDriftChangedPixelPercent = $MaxStableTemporalDriftChangedPixelPercent
        maxStableTemporalDriftMeanAbsLuma = $MaxStableTemporalDriftMeanAbsLuma
        maxMovedTemporalFlickerChangedPixelPercent = $MaxMovedTemporalFlickerChangedPixelPercent
        maxMovedTemporalFlickerMeanAbsLuma = $MaxMovedTemporalFlickerMeanAbsLuma
        maxStableTemporalRoughnessScore = $MaxStableTemporalRoughnessScore
        maxMovedTemporalRoughnessScore = $MaxMovedTemporalRoughnessScore
        minSceneColorVariance = $MinSceneColorVariance
        minSequenceSceneColorVariance = $MinSequenceSceneColorVariance
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        maxRegionBottomPercent = $MaxRegionBottomPercent
        focusRegionSelection = if ($AutoFocusRegion -and -not $DisableAutoFocusRegion) { "auto-upper-mid-world-surface" } else { "fixed-upper-mid-world-surface" }
        temporalMotionFocusRegionSelection = if (-not $DisableTemporalMotionAutoFocusRegion) { "auto-temporal-motion-upper-world-surface" } else { "generic-round7-focus-region" }
        temporalMotionSearchRegion = [ordered]@{
            leftPercent = $TemporalMotionSearchLeftPercent
            topPercent = $TemporalMotionSearchTopPercent
            widthPercent = $TemporalMotionSearchWidthPercent
            heightPercent = $TemporalMotionSearchHeightPercent
            bottomPercent = $temporalMotionSearchBottomPercent
            columns = $TemporalMotionRegionColumns
            rows = $TemporalMotionRegionRows
            paddingCells = $TemporalMotionRegionPaddingCells
            hudHandExcluded = $temporalMotionSearchBottomPercent -le $MaxRegionBottomPercent
        }
        rejectWindowScreenshotSources = [bool]$RejectWindowScreenshotSources
        requireLogProof = [bool]$RequireLogProof
    }
    screenshots = [ordered]@{
        particleBaselineDimensions = $particleBaselineDimensions
        particleFinalCompositeDimensions = $particleFinalDimensions
        translucentBaselineDimensions = $translucentBaselineDimensions
        translucentFinalCompositeDimensions = $translucentFinalDimensions
        temporalStableDimensions = $temporalStableDimensions
        temporalMovedDimensions = $temporalMovedDimensions
        sceneVariance = $sceneVariance
        sequenceSceneVariance = $sequenceSceneVariance
    }
    imageDelta = [ordered]@{
        particleBaselineToFinalComposite = $particleDelta
        translucentBaselineToFinalComposite = $translucentDelta
        temporalStableToMoved = $temporalDelta
        temporalStableRepeatedDrift = $stableTemporalDrift
        temporalMovedRepeatedFlicker = $movedTemporalFlicker
    }
    selectedFocusRegions = [ordered]@{
        particleBaselineToFinalComposite = $particleDelta.focusRegion
        translucentBaselineToFinalComposite = $translucentDelta.focusRegion
        temporalStableToMoved = $temporalDelta.focusRegion
        temporalStableRepeatedDrift = if ($stableTemporalDrift -and $stableTemporalDrift.consecutiveComparisons.Count -gt 0) { $stableTemporalDrift.consecutiveComparisons[0].delta.focusRegion } else { $null }
        temporalMovedRepeatedFlicker = if ($movedTemporalFlicker -and $movedTemporalFlicker.consecutiveComparisons.Count -gt 0) { $movedTemporalFlicker.consecutiveComparisons[0].delta.focusRegion } else { $null }
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round7_composite_stability_evidence_passed" } else { "round7_composite_stability_failed" }
        handHudExcludedByRegion = if ($AutoFocusRegion -and -not $DisableAutoFocusRegion) { $autoSearchBottomPercent -le $MaxRegionBottomPercent } else { $regionBottomPercent -le $MaxRegionBottomPercent }
        temporalMotionHudHandExcludedByRegion = if (-not $DisableTemporalMotionAutoFocusRegion) { $temporalMotionSearchBottomPercent -le $MaxRegionBottomPercent } else { if ($AutoFocusRegion -and -not $DisableAutoFocusRegion) { $autoSearchBottomPercent -le $MaxRegionBottomPercent } else { $regionBottomPercent -le $MaxRegionBottomPercent } }
        particleCompositeEvidencePresent = ([double]$particleDelta.focusRegionMetrics.changedPixelPercent -ge $MinParticleChangedPixelPercent)
        translucencyCompositeEvidencePresent = ([double]$translucentDelta.focusRegionMetrics.changedPixelPercent -ge $MinTranslucentChangedPixelPercent)
        temporalMotionEvidencePresent = (
            ([double]$temporalDelta.focusRegionMetrics.changedPixelPercent -ge $MinTemporalChangedPixelPercent) -and
            ([double]$temporalDelta.focusRegionMetrics.meanAbsLuma -ge $MinTemporalMeanAbsLuma)
        )
        stableTemporalDriftEvidencePresent = if ($stableTemporalDrift) {
            ([double]$stableTemporalDrift.maxChangedPixelPercent -le $MaxStableTemporalDriftChangedPixelPercent) -and
            ([double]$stableTemporalDrift.maxMeanAbsLuma -le $MaxStableTemporalDriftMeanAbsLuma) -and
            ([double]$stableTemporalDrift.roughnessScore -le $MaxStableTemporalRoughnessScore)
        } else {
            $null
        }
        movedTemporalFlickerEvidencePresent = if ($movedTemporalFlicker) {
            ([double]$movedTemporalFlicker.maxChangedPixelPercent -le $MaxMovedTemporalFlickerChangedPixelPercent) -and
            ([double]$movedTemporalFlicker.maxMeanAbsLuma -le $MaxMovedTemporalFlickerMeanAbsLuma) -and
            ([double]$movedTemporalFlicker.roughnessScore -le $MaxMovedTemporalRoughnessScore)
        } else {
            $null
        }
        screenshotSourceEvidenceAccepted = if ($ScreenshotSource.Count -gt 0) {
            -not (@($ScreenshotSource | Where-Object { $_ -match "^(?:window|window-fallback)$" }).Count -gt 0 -and $RejectWindowScreenshotSources)
        } else {
            $null
        }
        logTracks = [ordered]@{
            finalCompositePresent = if ($logProof) { [bool]$logProof.markers.finalCompositePresent } else { $null }
            hudPreservationPresent = if ($logProof) { [bool]$logProof.markers.hudPreservationPresent } else { $null }
            temporalHistoryPresent = if ($logProof) { [bool]$logProof.markers.temporalHistoryPresent } else { $null }
            temporalStableScenePresent = if ($logProof) { [bool]$logProof.markers.temporalStableScenePresent } else { $null }
            temporalMovedScenePresent = if ($logProof) { [bool]$logProof.markers.temporalMovedScenePresent } else { $null }
            particleScenePresent = if ($logProof) { [bool]$logProof.markers.particleScenePresent } else { $null }
            translucencyScenePresent = if ($logProof) { [bool]$logProof.markers.translucencyScenePresent } else { $null }
            temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
            proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
            focusWindowOnlyPresent = if ($logProof) { [bool]$logProof.markers.focusWindowOnlyPresent } else { $null }
            wrongWindowScreenshotPresent = if ($logProof) { [bool]$logProof.markers.wrongWindowScreenshotPresent } else { $null }
            nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
        }
        temporalTelemetry = if ($logProof) { $logProof.temporalTelemetry } else { $null }
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

Write-Host "particleBaselineImage=$($result.particleBaselineImage)"
Write-Host "particleFinalCompositeImage=$($result.particleFinalCompositeImage)"
Write-Host "translucentBaselineImage=$($result.translucentBaselineImage)"
Write-Host "translucentFinalCompositeImage=$($result.translucentFinalCompositeImage)"
Write-Host "temporalStableImage=$($result.temporalStableImage)"
Write-Host "temporalMovedImage=$($result.temporalMovedImage)"
Write-Host "temporalStableSequenceImages=$(@($result.temporalStableSequenceImages) -join ';')"
Write-Host "temporalMovedSequenceImages=$(@($result.temporalMovedSequenceImages) -join ';')"
Write-Host "temporalCaptureManifestJson=$(@($TemporalCaptureManifestJsonPath) -join ';')"
Write-Host "screenshotSources=$(@($result.screenshotSources) -join ';')"
Write-Host "logPaths=$(@($result.logPaths) -join ';')"
Write-Host "particle.focus.changedPixelPercent=$($particleDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "translucency.focus.changedPixelPercent=$($translucentDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "temporal.focus.changedPixelPercent=$($temporalDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "temporal.focus.meanAbsLuma=$($temporalDelta.focusRegionMetrics.meanAbsLuma)"
if ($stableTemporalDrift) {
    Write-Host "temporal.stable.sequence.captureCount=$($stableTemporalDrift.captureCount)"
    Write-Host "temporal.stable.sequence.maxChangedPixelPercent=$($stableTemporalDrift.maxChangedPixelPercent)"
    Write-Host "temporal.stable.sequence.maxMeanAbsLuma=$($stableTemporalDrift.maxMeanAbsLuma)"
    Write-Host "temporal.stable.sequence.maxRmseLuma=$($stableTemporalDrift.maxRmseLuma)"
    Write-Host "temporal.stable.sequence.roughnessScore=$($stableTemporalDrift.roughnessScore)"
}
if ($movedTemporalFlicker) {
    Write-Host "temporal.moved.sequence.captureCount=$($movedTemporalFlicker.captureCount)"
    Write-Host "temporal.moved.sequence.maxChangedPixelPercent=$($movedTemporalFlicker.maxChangedPixelPercent)"
    Write-Host "temporal.moved.sequence.maxMeanAbsLuma=$($movedTemporalFlicker.maxMeanAbsLuma)"
    Write-Host "temporal.moved.sequence.maxRmseLuma=$($movedTemporalFlicker.maxRmseLuma)"
    Write-Host "temporal.moved.sequence.roughnessScore=$($movedTemporalFlicker.roughnessScore)"
}
Write-Host "focus.regionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "temporal.focus.regionSelection=$($result.thresholds.temporalMotionFocusRegionSelection)"
Write-Host "temporal.focus.searchBottomPercent=$($result.thresholds.temporalMotionSearchRegion.bottomPercent)"
if ($logProof) {
    Write-Host "finalCompositePresent=$($logProof.markers.finalCompositePresent)"
    Write-Host "hudPreservationPresent=$($logProof.markers.hudPreservationPresent)"
    Write-Host "temporalHistoryPresent=$($logProof.markers.temporalHistoryPresent)"
    Write-Host "temporalStableScenePresent=$($logProof.markers.temporalStableScenePresent)"
    Write-Host "temporalMovedScenePresent=$($logProof.markers.temporalMovedScenePresent)"
    Write-Host "particleScenePresent=$($logProof.markers.particleScenePresent)"
    Write-Host "translucencyScenePresent=$($logProof.markers.translucencyScenePresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "focusWindowOnlyPresent=$($logProof.markers.focusWindowOnlyPresent)"
    Write-Host "wrongWindowScreenshotPresent=$($logProof.markers.wrongWindowScreenshotPresent)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
    Write-Host "temporal.telemetry.stablePixels=$($logProof.temporalTelemetry.stablePixels)"
    Write-Host "temporal.telemetry.unstablePixels=$($logProof.temporalTelemetry.unstablePixels)"
    Write-Host "temporal.telemetry.frameDelta=$($logProof.temporalTelemetry.frameDelta)"
    Write-Host "temporal.telemetry.historyConfidence=$($logProof.temporalTelemetry.historyConfidence)"
    Write-Host "temporal.telemetry.flickerScore=$($logProof.temporalTelemetry.flickerScore)"
    Write-Host "temporal.telemetry.ghostingRisk=$($logProof.temporalTelemetry.ghostingRisk)"
    Write-Host "temporal.telemetry.temporalReadiness=$($logProof.temporalTelemetry.temporalReadiness)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 7 composite stability proof failed: $($failures -join '; ')"
}
