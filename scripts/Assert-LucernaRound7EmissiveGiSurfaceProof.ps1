<#
.SYNOPSIS
Controller-only Round 7 assertion helper for emissive/GI surface visual proof.

.DESCRIPTION
This script checks already captured screenshots and optional controller logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. The default measured region is a fixed upper/mid world-surface crop so hand, skin, chat,
hotbar, and lower-HUD changes cannot satisfy the image-delta thresholds.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EnabledImagePath,

    [string] $DebugImagePath = "",

    [string[]] $LogPath = @(),

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 50.0,

    [double] $RegionTopPercent = 22.0,

    [double] $RegionWidthPercent = 36.0,

    [double] $RegionHeightPercent = 38.0,

    [switch] $AutoFocusRegion,

    [switch] $IncludeBandDiagnostics,

    [double] $AutoRegionSearchLeftPercent = 8.0,

    [double] $AutoRegionSearchTopPercent = 12.0,

    [double] $AutoRegionSearchWidthPercent = 84.0,

    [double] $AutoRegionSearchHeightPercent = 56.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 6,

    [int] $AutoRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinChangedPixelPercent = 1.0,

    [double] $MinBrighterPixelPercent = 0.25,

    [double] $MinMeanSignedLuma = 0.25,

    [double] $MinMeanAbsLuma = 0.5,

    [double] $MinSceneColorVariance = 4.0,

    [double] $MaxRegionBottomPercent = 72.0,

    [ValidateRange(1, 256)]
    [int] $TileColumns = 16,

    [ValidateRange(1, 256)]
    [int] $TileRows = 9,

    [ValidateRange(0.0, 100.0)]
    [double] $ActiveTileChangedPercentThreshold = 0.25,

    [ValidateRange(0.0, 100.0)]
    [double] $MaxFullImageChangedPixelPercent = 85.0,

    [ValidateRange(0.0, 100.0)]
    [double] $MaxFullImageChangedBoundingBoxAreaPercent = 92.0,

    [ValidateRange(0.0, 100.0)]
    [double] $MaxFullImageActiveTilePercent = 85.0,

    [ValidateRange(0.0, 100.0)]
    [double] $MaxFullImageEdgeActiveTilePercent = 35.0,

    [ValidateRange(0.0, 100.0)]
    [double] $MinFixedChangedPixelShareOfFull = 4.0,

    [switch] $RequireLogProof,

    [switch] $RequireHiddenGuiLogProof,

    [string[]] $FinalCompositePatterns = @(
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
        "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*finalBlendComplete=true",
        "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready",
        "sourceAuthenticity=accepted:final-composite-direct-plus-raw-gi-plus-denoised-gi"
    ),

    [string[]] $SurfaceScenePatterns = @(
        "round7\.emissiveGiSurface\.scene=locked-wall",
        "surfaceProofScene=true",
        "fixedWorldSurfaceRegion=true"
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
        [string] $EnabledPath
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round7-emissive-gi-surface-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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
    if ($AutoFocusRegion) {
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

function Invoke-ImageDiagnosticsHelper {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [object] $FocusRegion = $null
    )

    $diagnosticsScript = Join-Path $PSScriptRoot "Get-LucernaVisualProofImageDiagnostics.ps1"
    if (-not (Test-Path -LiteralPath $diagnosticsScript)) {
        throw "Missing Lucerna image diagnostics helper: $diagnosticsScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round7-emissive-gi-surface-diagnostics-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    $diagnosticRegionLeftPercent = $RegionLeftPercent
    $diagnosticRegionTopPercent = $RegionTopPercent
    $diagnosticRegionWidthPercent = $RegionWidthPercent
    $diagnosticRegionHeightPercent = $RegionHeightPercent
    if ($AutoFocusRegion -and $FocusRegion) {
        $diagnosticRegionLeftPercent = [double]$FocusRegion.leftPercent
        $diagnosticRegionTopPercent = [double]$FocusRegion.topPercent
        $diagnosticRegionWidthPercent = [double]$FocusRegion.widthPercent
        $diagnosticRegionHeightPercent = [double]$FocusRegion.heightPercent
    }
    $diagnosticsArgs = @{
        BaselineImagePath = $BaselinePath
        EnabledImagePath = $EnabledPath
        FixedRegionLeftPercent = $diagnosticRegionLeftPercent
        FixedRegionTopPercent = $diagnosticRegionTopPercent
        FixedRegionWidthPercent = $diagnosticRegionWidthPercent
        FixedRegionHeightPercent = $diagnosticRegionHeightPercent
        ChangedPixelThreshold = $ChangedPixelThreshold
        BrightPixelThreshold = $BrightPixelThreshold
        TileColumns = $TileColumns
        TileRows = $TileRows
        ActiveTileChangedPercentThreshold = $ActiveTileChangedPercentThreshold
        WashoutBoundingBoxAreaPercentThreshold = $MaxFullImageChangedBoundingBoxAreaPercent
        WashoutActiveTilePercentThreshold = $MaxFullImageActiveTilePercent
        WashoutEdgeActiveTilePercentThreshold = $MaxFullImageEdgeActiveTilePercent
        OutputJsonPath = $tempJson
    }
    if ($IncludeBandDiagnostics) {
        $diagnosticsArgs.IncludeBands = $true
    }

    & $diagnosticsScript @diagnosticsArgs | Out-Null
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

function Measure-LogProof {
    param([string[]] $ResolvedLogPaths)

    $logParts = New-Object System.Collections.Generic.List[string]
    foreach ($path in $ResolvedLogPaths) {
        $logParts.Add((Get-Content -Raw -LiteralPath $path)) | Out-Null
    }
    $log = $logParts -join "`n"

    return [ordered]@{
        markers = [ordered]@{
            finalCompositePresent = Test-AnyRegex $log $FinalCompositePatterns
            surfaceScenePresent = Test-AnyRegex $log $SurfaceScenePatterns
            hiddenGuiCapturePresent = Test-Regex $log "round7\.emissiveGiSurface\.captureRole=.*hideGuiBeforeScreenshot=true"
            commandFeedbackDisabledPresent = Test-Regex $log "commandFeedback=false|/gamerule sendCommandFeedback false"
            chatClearedPresent = Test-Regex $log "chatCleared=true"
            temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporary direct-light|current direct-light RGBA payload|using the current direct-light RGBA payload as the temporary visible source"
            proofMarkerPresent = Test-Regex $log "proof marker|round5-direct-proof|R5 visual proof|round6-gi-proof|R6 GI proof|R7 proof|CPU output proof"
            focusWindowOnlyPresent = Test-Regex $log "focus-window-only|sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|mode=final-composite-direct-light-focus-window-additive|round6-diffuse-gi-focus-window-additive"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|VK_[A-Z_]*ERROR|Lucerna native error|native error|Vulkan error"
        }
        patterns = [ordered]@{
            finalCompositePatterns = @($FinalCompositePatterns)
            surfaceScenePatterns = @($SurfaceScenePatterns)
        }
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$enabledResolved = Resolve-ExistingFile $EnabledImagePath "Enabled image"
$debugResolved = if ([string]::IsNullOrWhiteSpace($DebugImagePath)) { "" } else { Resolve-ExistingFile $DebugImagePath "Debug image" }
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$baselineDimensions = Get-ImageDimensions $baselineResolved
$enabledDimensions = Get-ImageDimensions $enabledResolved
$debugDimensions = if ([string]::IsNullOrWhiteSpace($debugResolved)) { $null } else { Get-ImageDimensions $debugResolved }
$delta = Invoke-DeltaHelper $baselineResolved $enabledResolved
$imageDiagnostics = Invoke-ImageDiagnosticsHelper $baselineResolved $enabledResolved $delta.focusRegion
$sceneVariance = [ordered]@{
    baseline = Measure-SceneColorVariance $baselineResolved
    enabled = Measure-SceneColorVariance $enabledResolved
}
if ($debugDimensions) {
    $sceneVariance["debug"] = Measure-SceneColorVariance $debugResolved
}

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

if (($enabledDimensions.width -ne $baselineDimensions.width) -or ($enabledDimensions.height -ne $baselineDimensions.height)) {
    $failures.Add("Enabled image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) actual=$($enabledDimensions.width)x$($enabledDimensions.height)")
}
if ($debugDimensions -and (($debugDimensions.width -ne $baselineDimensions.width) -or ($debugDimensions.height -ne $baselineDimensions.height))) {
    $failures.Add("Debug image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) actual=$($debugDimensions.width)x$($debugDimensions.height)")
}

$regionBottomPercent = $RegionTopPercent + $RegionHeightPercent
$autoSearchBottomPercent = $AutoRegionSearchTopPercent + $AutoRegionSearchHeightPercent
if (-not $AutoFocusRegion -and $regionBottomPercent -gt $MaxRegionBottomPercent) {
    $failures.Add("Fixed proof region extends into lower HUD/hand area. bottomPercent=$regionBottomPercent max=$MaxRegionBottomPercent")
}
if ($AutoFocusRegion -and $autoSearchBottomPercent -gt $MaxRegionBottomPercent) {
    $failures.Add("Auto-focus search region extends into lower HUD/hand area. bottomPercent=$autoSearchBottomPercent max=$MaxRegionBottomPercent")
}

foreach ($entry in $sceneVariance.GetEnumerator()) {
    if ([double]$entry.Value.stdDevLuma -lt $MinSceneColorVariance) {
        $failures.Add("$($entry.Key) image appears too flat or blank. stdDevLuma=$($entry.Value.stdDevLuma) expected>=$MinSceneColorVariance")
    }
}

if ([double]$delta.focusRegionMetrics.changedPixelPercent -lt $MinChangedPixelPercent) {
    $failures.Add("Focused surface-region changed pixels below threshold. actual=$($delta.focusRegionMetrics.changedPixelPercent) expected>=$MinChangedPixelPercent")
}
if ([double]$delta.focusRegionMetrics.brighterPixelPercent -lt $MinBrighterPixelPercent) {
    $failures.Add("Focused surface-region brighter pixels below threshold. actual=$($delta.focusRegionMetrics.brighterPixelPercent) expected>=$MinBrighterPixelPercent")
}
if ([double]$delta.focusRegionMetrics.meanSignedLuma -lt $MinMeanSignedLuma) {
    $failures.Add("Focused surface-region mean signed luma below threshold. actual=$($delta.focusRegionMetrics.meanSignedLuma) expected>=$MinMeanSignedLuma")
}
if ([double]$delta.focusRegionMetrics.meanAbsLuma -lt $MinMeanAbsLuma) {
    $failures.Add("Focused surface-region mean absolute luma below threshold. actual=$($delta.focusRegionMetrics.meanAbsLuma) expected>=$MinMeanAbsLuma")
}
if ($imageDiagnostics.fileIdentity.identicalBySha256) {
    $failures.Add("Baseline and enabled screenshots are identical by SHA-256; no captured visual proof can be accepted.")
}
if (-not $imageDiagnostics.classification.anyScreenRegionChangedAboveThreshold) {
    $failures.Add("Full-image diagnostics found no pixels changed above threshold; no draw affected any measured screen region.")
}
$fullMetrics = $imageDiagnostics.regions.fullImage.metrics
$fixedMetrics = $imageDiagnostics.regions.fixedWorldSurfaceCrop.metrics
$fullShape = $imageDiagnostics.regions.fullImage.shape
$fixedChangedShareOfFull = if ([double]$fullMetrics.changedPixels -le 0.0) {
    0.0
} else {
    [Math]::Round(100.0 * [double]$fixedMetrics.changedPixels / [double]$fullMetrics.changedPixels, 4)
}
if ([double]$fullMetrics.changedPixelPercent -gt $MaxFullImageChangedPixelPercent) {
    $failures.Add("Full-screen changed-pixel coverage is too high for localized scene-tied surface evidence. actual=$($fullMetrics.changedPixelPercent) max=$MaxFullImageChangedPixelPercent")
}
if ([double]$fullShape.changedBoundingBoxAreaPercent -gt $MaxFullImageChangedBoundingBoxAreaPercent) {
    $failures.Add("Changed-pixel bounding box covers too much of the screen; reject rectangular/full-screen washout. actual=$($fullShape.changedBoundingBoxAreaPercent) max=$MaxFullImageChangedBoundingBoxAreaPercent")
}
if ([double]$fullShape.tileMetrics.activeTilePercent -gt $MaxFullImageActiveTilePercent) {
    $failures.Add("Changed pixels are spread across too many screen tiles; reject uniform washout. actual=$($fullShape.tileMetrics.activeTilePercent) max=$MaxFullImageActiveTilePercent")
}
if ([double]$fullShape.tileMetrics.edgeActiveTilePercent -gt $MaxFullImageEdgeActiveTilePercent) {
    $failures.Add("Changed pixels reach too many screen-edge tiles; reject frame-wide/rectangular proof-like lighting. actual=$($fullShape.tileMetrics.edgeActiveTilePercent) max=$MaxFullImageEdgeActiveTilePercent")
}
if ([double]$fixedChangedShareOfFull -lt $MinFixedChangedPixelShareOfFull) {
    $failures.Add("Fixed scene-surface crop owns too little of the total changed-pixel evidence. actual=$fixedChangedShareOfFull expected>=$MinFixedChangedPixelShareOfFull")
}
if ($imageDiagnostics.classification.fullScreenOrRectangularWashoutSuspect) {
    $failures.Add("Image diagnostics classify the delta as full-screen or rectangular washout; Round 7 surface proof requires localized scene-shaped evidence.")
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($RequireHiddenGuiLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Hidden-GUI log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.finalCompositePresent) {
        $failures.Add("Missing Round 7 final composite dispatch/submission log marker.")
    }
    if (-not $logProof.markers.surfaceScenePresent) {
        $failures.Add("Missing locked emissive/GI surface scene log marker.")
    }
    if ($RequireHiddenGuiLogProof -and -not $logProof.markers.hiddenGuiCapturePresent) {
        $failures.Add("Missing hidden-GUI screenshot capture marker for baseline/enabled surface proof.")
    }
    if (-not $logProof.markers.commandFeedbackDisabledPresent) {
        $failures.Add("Missing sendCommandFeedback false marker.")
    }
    if (-not $logProof.markers.chatClearedPresent) {
        $failures.Add("Missing chat-cleared capture marker.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; surface proof must not use temporary direct-light payload substitution.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker evidence; surface proof must use real world-surface pixels without marker overlays.")
    }
    if ($logProof.markers.focusWindowOnlyPresent) {
        $failures.Add("Log contains focus-window marker; surface proof must not rely on focus-window-only brightness.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$result = [ordered]@{
    baselineImage = $baselineResolved
    enabledImage = $enabledResolved
    debugImage = $debugResolved
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minChangedPixelPercent = $MinChangedPixelPercent
        minBrighterPixelPercent = $MinBrighterPixelPercent
        minMeanSignedLuma = $MinMeanSignedLuma
        minMeanAbsLuma = $MinMeanAbsLuma
        minSceneColorVariance = $MinSceneColorVariance
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        maxRegionBottomPercent = $MaxRegionBottomPercent
        tileColumns = $TileColumns
        tileRows = $TileRows
        activeTileChangedPercentThreshold = $ActiveTileChangedPercentThreshold
        maxFullImageChangedPixelPercent = $MaxFullImageChangedPixelPercent
        maxFullImageChangedBoundingBoxAreaPercent = $MaxFullImageChangedBoundingBoxAreaPercent
        maxFullImageActiveTilePercent = $MaxFullImageActiveTilePercent
        maxFullImageEdgeActiveTilePercent = $MaxFullImageEdgeActiveTilePercent
        minFixedChangedPixelShareOfFull = $MinFixedChangedPixelShareOfFull
        focusRegionSelection = if ($AutoFocusRegion) { "auto-upper-mid-world-surface" } else { "fixed-upper-mid-world-surface" }
        bandDiagnosticsIncluded = [bool]$IncludeBandDiagnostics
        requireLogProof = [bool]$RequireLogProof
        requireHiddenGuiLogProof = [bool]$RequireHiddenGuiLogProof
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        enabledDimensions = $enabledDimensions
        debugDimensions = $debugDimensions
        sceneVariance = $sceneVariance
    }
    imageDelta = $delta
    imageDiagnostics = $imageDiagnostics
    selectedFocusRegion = $delta.focusRegion
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round7_emissive_gi_surface_evidence_passed" } else { "round7_emissive_gi_surface_failed" }
        handHudExcludedByRegion = ($regionBottomPercent -le $MaxRegionBottomPercent) -and (-not $AutoFocusRegion)
        surfaceCompositeEvidencePresent = (
            ([double]$delta.focusRegionMetrics.changedPixelPercent -ge $MinChangedPixelPercent) -and
            ([double]$delta.focusRegionMetrics.brighterPixelPercent -ge $MinBrighterPixelPercent) -and
            ([double]$delta.focusRegionMetrics.meanSignedLuma -ge $MinMeanSignedLuma)
        )
        screenshotIdentity = [ordered]@{
            identicalBySha256 = [bool]$imageDiagnostics.fileIdentity.identicalBySha256
            baselineSha256 = [string]$imageDiagnostics.fileIdentity.baseline.sha256
            enabledSha256 = [string]$imageDiagnostics.fileIdentity.enabled.sha256
            sameByteLength = [bool]$imageDiagnostics.fileIdentity.sameByteLength
        }
        screenRegionDiagnostics = [ordered]@{
            anyScreenRegionChangedAboveThreshold = [bool]$imageDiagnostics.classification.anyScreenRegionChangedAboveThreshold
            fixedWorldSurfaceCropChangedAboveThreshold = [bool]$imageDiagnostics.classification.fixedWorldSurfaceCropChangedAboveThreshold
            changedOutsideFixedWorldSurfaceCrop = [bool]$imageDiagnostics.classification.changedOutsideFixedWorldSurfaceCrop
            onlyFileEncodingOrMetadataChanged = [bool]$imageDiagnostics.classification.onlyFileEncodingOrMetadataChanged
            fullScreenOrRectangularWashoutSuspect = [bool]$imageDiagnostics.classification.fullScreenOrRectangularWashoutSuspect
            localizedSceneShapedDeltaPresent = [bool]$imageDiagnostics.classification.localizedSceneShapedDeltaPresent
            fixedChangedPixelShareOfFull = $fixedChangedShareOfFull
            fullImage = $imageDiagnostics.regions.fullImage.metrics
            fullImageShape = $imageDiagnostics.regions.fullImage.shape
            fixedWorldSurfaceCrop = $imageDiagnostics.regions.fixedWorldSurfaceCrop.metrics
            fixedWorldSurfaceCropShape = $imageDiagnostics.regions.fixedWorldSurfaceCrop.shape
            bands = if ($IncludeBandDiagnostics) {
                [ordered]@{
                    top = $imageDiagnostics.regions.topBand.metrics
                    middle = $imageDiagnostics.regions.middleBand.metrics
                    bottom = $imageDiagnostics.regions.bottomBand.metrics
                }
            } else {
                $null
            }
        }
        logTracks = [ordered]@{
            finalCompositePresent = if ($logProof) { [bool]$logProof.markers.finalCompositePresent } else { $null }
            surfaceScenePresent = if ($logProof) { [bool]$logProof.markers.surfaceScenePresent } else { $null }
            hiddenGuiCapturePresent = if ($logProof) { [bool]$logProof.markers.hiddenGuiCapturePresent } else { $null }
            commandFeedbackDisabledPresent = if ($logProof) { [bool]$logProof.markers.commandFeedbackDisabledPresent } else { $null }
            chatClearedPresent = if ($logProof) { [bool]$logProof.markers.chatClearedPresent } else { $null }
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

Write-Host "baselineImage=$($result.baselineImage)"
Write-Host "enabledImage=$($result.enabledImage)"
Write-Host "debugImage=$($result.debugImage)"
Write-Host "logPaths=$(@($result.logPaths) -join ';')"
Write-Host "focus.changedPixelPercent=$($delta.focusRegionMetrics.changedPixelPercent)"
Write-Host "focus.brighterPixelPercent=$($delta.focusRegionMetrics.brighterPixelPercent)"
Write-Host "focus.meanSignedLuma=$($delta.focusRegionMetrics.meanSignedLuma)"
Write-Host "focus.meanAbsLuma=$($delta.focusRegionMetrics.meanAbsLuma)"
Write-Host "focus.regionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "hash.identicalBySha256=$($imageDiagnostics.fileIdentity.identicalBySha256)"
Write-Host "hash.baselineSha256=$($imageDiagnostics.fileIdentity.baseline.sha256)"
Write-Host "hash.enabledSha256=$($imageDiagnostics.fileIdentity.enabled.sha256)"
Write-Host "full.changedPixelPercent=$($imageDiagnostics.regions.fullImage.metrics.changedPixelPercent)"
Write-Host "full.brighterPixelPercent=$($imageDiagnostics.regions.fullImage.metrics.brighterPixelPercent)"
Write-Host "full.changedBoundingBoxAreaPercent=$($imageDiagnostics.regions.fullImage.shape.changedBoundingBoxAreaPercent)"
Write-Host "full.activeTilePercent=$($imageDiagnostics.regions.fullImage.shape.tileMetrics.activeTilePercent)"
Write-Host "full.edgeActiveTilePercent=$($imageDiagnostics.regions.fullImage.shape.tileMetrics.edgeActiveTilePercent)"
Write-Host "fixed.changedPixelPercent=$($imageDiagnostics.regions.fixedWorldSurfaceCrop.metrics.changedPixelPercent)"
Write-Host "fixed.brighterPixelPercent=$($imageDiagnostics.regions.fixedWorldSurfaceCrop.metrics.brighterPixelPercent)"
Write-Host "fixed.changedPixelShareOfFull=$fixedChangedShareOfFull"
Write-Host "classification.fullScreenOrRectangularWashoutSuspect=$($imageDiagnostics.classification.fullScreenOrRectangularWashoutSuspect)"
Write-Host "classification.localizedSceneShapedDeltaPresent=$($imageDiagnostics.classification.localizedSceneShapedDeltaPresent)"
if ($IncludeBandDiagnostics) {
    Write-Host "top.changedPixelPercent=$($imageDiagnostics.regions.topBand.metrics.changedPixelPercent)"
    Write-Host "middle.changedPixelPercent=$($imageDiagnostics.regions.middleBand.metrics.changedPixelPercent)"
    Write-Host "bottom.changedPixelPercent=$($imageDiagnostics.regions.bottomBand.metrics.changedPixelPercent)"
}
if ($logProof) {
    Write-Host "finalCompositePresent=$($logProof.markers.finalCompositePresent)"
    Write-Host "surfaceScenePresent=$($logProof.markers.surfaceScenePresent)"
    Write-Host "hiddenGuiCapturePresent=$($logProof.markers.hiddenGuiCapturePresent)"
    Write-Host "commandFeedbackDisabledPresent=$($logProof.markers.commandFeedbackDisabledPresent)"
    Write-Host "chatClearedPresent=$($logProof.markers.chatClearedPresent)"
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
    throw "Round 7 emissive/GI surface proof failed: $($failures -join '; ')"
}
