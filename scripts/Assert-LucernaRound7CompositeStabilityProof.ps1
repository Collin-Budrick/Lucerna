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

    [string[]] $LogPath = @(),

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 10.0,

    [double] $RegionTopPercent = 10.0,

    [double] $RegionWidthPercent = 80.0,

    [double] $RegionHeightPercent = 75.0,

    [switch] $DisableAutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 8.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 80.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinParticleChangedPixelPercent = 0.1,

    [double] $MinTranslucentChangedPixelPercent = 0.1,

    [double] $MinTemporalChangedPixelPercent = 0.5,

    [double] $MinTemporalMeanAbsLuma = 0.5,

    [double] $MinSceneColorVariance = 8.0,

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
    if (-not $DisableAutoFocusRegion) {
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
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporary direct-light|current direct-light RGBA payload|using the current direct-light RGBA payload as the temporary visible source"
    $proofMarkerPresent = Test-Regex $log "proof marker|round5-direct-proof|R5 visual proof|round6-gi-proof|R6 GI proof|R7 proof|CPU output proof"
    $focusWindowOnlyPresent = Test-Regex $log "focus-window-only|sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|mode=final-composite-direct-light-focus-window-additive|round6-diffuse-gi-focus-window-additive"
    $nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|VK_[A-Z_]*ERROR|Lucerna native error|native error|Vulkan error"

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
            nativeErrorPresent = $nativeErrorPresent
        }
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

$sceneVariance = [ordered]@{
    particleBaseline = Measure-SceneColorVariance $particleBaselineResolved
    particleFinalComposite = Measure-SceneColorVariance $particleFinalResolved
    translucentBaseline = Measure-SceneColorVariance $translucentBaselineResolved
    translucentFinalComposite = Measure-SceneColorVariance $translucentFinalResolved
    temporalStable = Measure-SceneColorVariance $temporalStableResolved
    temporalMoved = Measure-SceneColorVariance $temporalMovedResolved
}

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round7CompositeStabilityLogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

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

foreach ($entry in $sceneVariance.GetEnumerator()) {
    if ([double]$entry.Value.stdDevLuma -lt $MinSceneColorVariance) {
        $failures.Add("$($entry.Key) image appears too flat or blank. stdDevLuma=$($entry.Value.stdDevLuma) expected>=$MinSceneColorVariance")
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
}
if ([double]$temporalDelta.focusRegionMetrics.meanAbsLuma -lt $MinTemporalMeanAbsLuma) {
    $failures.Add("Temporal moved-camera focused-region mean absolute luma below threshold. actual=$($temporalDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinTemporalMeanAbsLuma")
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
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minParticleChangedPixelPercent = $MinParticleChangedPixelPercent
        minTranslucentChangedPixelPercent = $MinTranslucentChangedPixelPercent
        minTemporalChangedPixelPercent = $MinTemporalChangedPixelPercent
        minTemporalMeanAbsLuma = $MinTemporalMeanAbsLuma
        minSceneColorVariance = $MinSceneColorVariance
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        focusRegionSelection = if ($DisableAutoFocusRegion) { "fixed" } else { "auto" }
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
    }
    imageDelta = [ordered]@{
        particleBaselineToFinalComposite = $particleDelta
        translucentBaselineToFinalComposite = $translucentDelta
        temporalStableToMoved = $temporalDelta
    }
    selectedFocusRegions = [ordered]@{
        particleBaselineToFinalComposite = $particleDelta.focusRegion
        translucentBaselineToFinalComposite = $translucentDelta.focusRegion
        temporalStableToMoved = $temporalDelta.focusRegion
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round7_composite_stability_evidence_passed" } else { "round7_composite_stability_failed" }
        particleCompositeEvidencePresent = ([double]$particleDelta.focusRegionMetrics.changedPixelPercent -ge $MinParticleChangedPixelPercent)
        translucencyCompositeEvidencePresent = ([double]$translucentDelta.focusRegionMetrics.changedPixelPercent -ge $MinTranslucentChangedPixelPercent)
        temporalMotionEvidencePresent = (
            ([double]$temporalDelta.focusRegionMetrics.changedPixelPercent -ge $MinTemporalChangedPixelPercent) -and
            ([double]$temporalDelta.focusRegionMetrics.meanAbsLuma -ge $MinTemporalMeanAbsLuma)
        )
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
            nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
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

Write-Host "particleBaselineImage=$($result.particleBaselineImage)"
Write-Host "particleFinalCompositeImage=$($result.particleFinalCompositeImage)"
Write-Host "translucentBaselineImage=$($result.translucentBaselineImage)"
Write-Host "translucentFinalCompositeImage=$($result.translucentFinalCompositeImage)"
Write-Host "temporalStableImage=$($result.temporalStableImage)"
Write-Host "temporalMovedImage=$($result.temporalMovedImage)"
Write-Host "logPaths=$(@($result.logPaths) -join ';')"
Write-Host "particle.focus.changedPixelPercent=$($particleDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "translucency.focus.changedPixelPercent=$($translucentDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "temporal.focus.changedPixelPercent=$($temporalDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "temporal.focus.meanAbsLuma=$($temporalDelta.focusRegionMetrics.meanAbsLuma)"
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
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 7 composite stability proof failed: $($failures -join '; ')"
}
