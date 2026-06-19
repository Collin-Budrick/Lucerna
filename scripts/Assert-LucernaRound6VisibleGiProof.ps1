param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EnabledImagePath,

    [string] $DebugImagePath = "",

    [string] $LogPath = "",

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 30.0,

    [double] $RegionTopPercent = 20.0,

    [double] $RegionWidthPercent = 40.0,

    [double] $RegionHeightPercent = 55.0,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinFocusChangedPixelPercent = 1.0,

    [double] $MinFocusBrighterPixelPercent = 0.5,

    [double] $MinFocusMeanSignedLuma = 0.5,

    [long] $MinGiRays = 1,

    [long] $MinGiCacheReads = 1,

    [string[]] $NativeGiOutputSourcePatterns = @(
        "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:nativeGiOutputReady|nativeDiffuseGiOutputReady|sourceNativeGiReady)=true",
        "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi",
        "Lucerna public Mojang final composite: .*mode=round6-diffuse-gi-.*(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi"
    ),

    [switch] $RequireDebugScreenshot,

    [switch] $RequireLogProof,

    [switch] $RequireNativeGiOutputSource,

    [switch] $AllowProofMarkerEvidence
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
        [string] $EnabledPath
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round6-visible-gi-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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

function Get-MaxRegexNumber {
    param(
        [string] $Text,
        [string] $Pattern
    )

    $max = 0L
    foreach ($match in [regex]::Matches($Text, $Pattern)) {
        if ($match.Groups.Count -lt 2) {
            continue
        }
        $value = [long]$match.Groups[1].Value
        if ($value -gt $max) {
            $max = $value
        }
    }
    return $max
}

function Test-Regex {
    param(
        [string] $Text,
        [string] $Pattern
    )

    return [regex]::IsMatch($Text, $Pattern)
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

function Test-AnyRegexInTexts {
    param(
        [string[]] $Texts,
        [string[]] $Patterns
    )

    foreach ($text in $Texts) {
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }
        foreach ($pattern in $Patterns) {
            if (Test-Regex $text $pattern) {
                return $true
            }
        }
    }
    return $false
}

function Measure-Round6LogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
    $roundSixDispatchPresent = Test-Regex $log "Lucerna Round 6 lighting dispatch prepared:"
    $diffuseGiEnabled = Test-Regex $log "diffuse_gi=\{\{?enabled=true,"
    $cacheStagePresent = Test-Regex $log "cache=\{\{?enabled=true,"
    $giSizePresent = Test-Regex $log "diffuse_gi=\{\{?enabled=true,size=\d+x\d+"
    $cacheConfidencePresent = Test-Regex $log "(cache_confidence|confidence)="
    $debugOverlayPresent = Test-Regex $log "(Round 6|GI|cache).*debug|debug.*(Round 6|GI|cache)"
    $nativeGiOutputSourcePresent = Test-AnyRegex $log $NativeGiOutputSourcePatterns
    $temporaryDirectLightSourcePresent = Test-Regex $log "Lucerna Round 6 diffuse GI preview composite: .*temporarySourceReady=true|using the current direct-light RGBA payload as the temporary visible source until native GI output is exposed"
    $roundSixDrawSubmittedPresent = Test-Regex $log "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=(?:round6-diffuse-gi-|round6-native-diffuse-gi-)"
    $roundSixNoMarkerSurfaceDrawPresent = Test-Regex $log "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-native-diffuse-gi-surface-additive"
    $focusWindowPreviewDrawPresent = Test-Regex $log "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-diffuse-gi-focus-window-additive"
    $proofMarkerLogPresent = Test-Regex $log "(?i)(round6-gi-proof|R6 GI proof|proof marker|CPU output proof)"
    $nativeErrorPresent = Test-Regex $log "(?i)(invalid descriptor|VK_ERROR|Lucerna native error|native error)"

    $maxGiRays = Get-MaxRegexNumber $log "(?:diffuse_gi=\{\{?enabled=true,[^`r`n]*?rays=|rays=)(\d+)"
    $maxGiCacheReads = [Math]::Max(
        (Get-MaxRegexNumber $log "cache_reads=(\d+)"),
        (Get-MaxRegexNumber $log "cacheReads=(\d+)")
    )
    $maxGiSamples = Get-MaxRegexNumber $log "(?:diffuse_gi=\{\{?enabled=true,[^`r`n]*?samples=|samples=)(\d+)"

    return [ordered]@{
        markers = [ordered]@{
            roundSixDispatchPresent = $roundSixDispatchPresent
            diffuseGiEnabled = $diffuseGiEnabled
            giSizePresent = $giSizePresent
            cacheStagePresent = $cacheStagePresent
            cacheConfidencePresent = $cacheConfidencePresent
            debugOverlayPresent = $debugOverlayPresent
            nativeGiOutputSourcePresent = $nativeGiOutputSourcePresent
            temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
            roundSixDrawSubmittedPresent = $roundSixDrawSubmittedPresent
            roundSixNoMarkerSurfaceDrawPresent = $roundSixNoMarkerSurfaceDrawPresent
            focusWindowPreviewDrawPresent = $focusWindowPreviewDrawPresent
            proofMarkerLogPresent = $proofMarkerLogPresent
            nativeErrorPresent = $nativeErrorPresent
        }
        maxima = [ordered]@{
            giRays = $maxGiRays
            giCacheReads = $maxGiCacheReads
            giSamples = $maxGiSamples
        }
    }
}

function New-DeltaAccumulator {
    return [ordered]@{
        pixelCount = 0
        changedPixels = 0
        brighterPixels = 0
        darkerPixels = 0
        nearChangedPixels = 0
        nearBrightPixels = 0
        sumAbsLuma = 0.0
        sumSignedLuma = 0.0
        maxAbsLuma = 0.0
        maxAbsChannel = 0
        maxPositiveLumaBelowBrightThreshold = 0.0
        maxChannelBelowChangedThreshold = 0
    }
}

function Add-DeltaSample {
    param(
        [System.Collections.IDictionary] $Accumulator,
        [System.Drawing.Color] $Baseline,
        [System.Drawing.Color] $Enabled
    )

    $deltaR = [int]$Enabled.R - [int]$Baseline.R
    $deltaG = [int]$Enabled.G - [int]$Baseline.G
    $deltaB = [int]$Enabled.B - [int]$Baseline.B
    $baselineLuma = (0.2126 * [int]$Baseline.R) + (0.7152 * [int]$Baseline.G) + (0.0722 * [int]$Baseline.B)
    $enabledLuma = (0.2126 * [int]$Enabled.R) + (0.7152 * [int]$Enabled.G) + (0.0722 * [int]$Enabled.B)
    $deltaLuma = $enabledLuma - $baselineLuma
    $absLuma = [Math]::Abs($deltaLuma)
    $maxChannel = [Math]::Max([Math]::Abs($deltaR), [Math]::Max([Math]::Abs($deltaG), [Math]::Abs($deltaB)))

    $Accumulator.pixelCount++
    $Accumulator.sumAbsLuma += $absLuma
    $Accumulator.sumSignedLuma += $deltaLuma
    $Accumulator.maxAbsLuma = [Math]::Max($Accumulator.maxAbsLuma, $absLuma)
    $Accumulator.maxAbsChannel = [Math]::Max($Accumulator.maxAbsChannel, $maxChannel)

    if ($maxChannel -ge $ChangedPixelThreshold) {
        $Accumulator.changedPixels++
    } elseif ($maxChannel -gt 0) {
        $Accumulator.nearChangedPixels++
        $Accumulator.maxChannelBelowChangedThreshold = [Math]::Max($Accumulator.maxChannelBelowChangedThreshold, $maxChannel)
    }

    if ($deltaLuma -ge $BrightPixelThreshold) {
        $Accumulator.brighterPixels++
    } elseif ($deltaLuma -le -$BrightPixelThreshold) {
        $Accumulator.darkerPixels++
    } elseif ($deltaLuma -gt 0) {
        $Accumulator.nearBrightPixels++
        $Accumulator.maxPositiveLumaBelowBrightThreshold = [Math]::Max($Accumulator.maxPositiveLumaBelowBrightThreshold, $deltaLuma)
    }
}

function Complete-DeltaAccumulator {
    param([System.Collections.IDictionary] $Accumulator)

    $count = [Math]::Max(1, [int]$Accumulator.pixelCount)
    return [ordered]@{
        pixelCount = $Accumulator.pixelCount
        meanAbsLuma = [Math]::Round($Accumulator.sumAbsLuma / $count, 4)
        meanSignedLuma = [Math]::Round($Accumulator.sumSignedLuma / $count, 4)
        maxAbsLuma = [Math]::Round($Accumulator.maxAbsLuma, 4)
        maxAbsChannel = $Accumulator.maxAbsChannel
        changedPixels = $Accumulator.changedPixels
        changedPixelPercent = [Math]::Round(100.0 * $Accumulator.changedPixels / $count, 4)
        brighterPixels = $Accumulator.brighterPixels
        brighterPixelPercent = [Math]::Round(100.0 * $Accumulator.brighterPixels / $count, 4)
        darkerPixels = $Accumulator.darkerPixels
        darkerPixelPercent = [Math]::Round(100.0 * $Accumulator.darkerPixels / $count, 4)
        nearChangedPixels = $Accumulator.nearChangedPixels
        nearChangedPixelPercent = [Math]::Round(100.0 * $Accumulator.nearChangedPixels / $count, 4)
        nearBrightPixels = $Accumulator.nearBrightPixels
        nearBrightPixelPercent = [Math]::Round(100.0 * $Accumulator.nearBrightPixels / $count, 4)
        maxChannelBelowChangedThreshold = $Accumulator.maxChannelBelowChangedThreshold
        maxPositiveLumaBelowBrightThreshold = [Math]::Round($Accumulator.maxPositiveLumaBelowBrightThreshold, 4)
    }
}

function Get-ThresholdGap {
    param(
        [double] $Actual,
        [double] $Expected
    )

    $missing = [Math]::Max(0.0, $Expected - $Actual)
    $ratio = if ($Expected -eq 0.0) {
        if ($Actual -ge $Expected) { 1.0 } else { 0.0 }
    } else {
        $Actual / $Expected
    }
    return [ordered]@{
        actual = [Math]::Round($Actual, 4)
        expected = [Math]::Round($Expected, 4)
        missing = [Math]::Round($missing, 4)
        ratio = [Math]::Round($ratio, 4)
    }
}

function Test-RectIntersects {
    param(
        [int] $LeftA,
        [int] $TopA,
        [int] $RightA,
        [int] $BottomA,
        [int] $LeftB,
        [int] $TopB,
        [int] $RightB,
        [int] $BottomB
    )

    return ($LeftA -lt $RightB) -and ($RightA -gt $LeftB) -and ($TopA -lt $BottomB) -and ($BottomA -gt $TopB)
}

function Measure-ImageDeltaDiagnostics {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [object] $FocusRegion,
        [object] $FocusMetrics
    )

    Add-Type -AssemblyName System.Drawing
    $baselineImage = [System.Drawing.Bitmap]::new($BaselinePath)
    $enabledImage = [System.Drawing.Bitmap]::new($EnabledPath)
    try {
        if (($baselineImage.Width -ne $enabledImage.Width) -or ($baselineImage.Height -ne $enabledImage.Height)) {
            throw "Image dimensions differ. baseline=$($baselineImage.Width)x$($baselineImage.Height) enabled=$($enabledImage.Width)x$($enabledImage.Height)"
        }

        $columns = 8
        $rows = 6
        $focusLeft = [int]$FocusRegion.left
        $focusTop = [int]$FocusRegion.top
        $focusRight = $focusLeft + [int]$FocusRegion.width
        $focusBottom = $focusTop + [int]$FocusRegion.height
        $cells = New-Object System.Collections.Generic.List[object]
        $focusAccumulator = New-DeltaAccumulator

        for ($row = 0; $row -lt $rows; $row++) {
            $top = [int][Math]::Floor($row * $baselineImage.Height / $rows)
            $bottom = [int][Math]::Floor(($row + 1) * $baselineImage.Height / $rows)
            for ($column = 0; $column -lt $columns; $column++) {
                $left = [int][Math]::Floor($column * $baselineImage.Width / $columns)
                $right = [int][Math]::Floor(($column + 1) * $baselineImage.Width / $columns)
                $accumulator = New-DeltaAccumulator

                for ($y = $top; $y -lt $bottom; $y++) {
                    for ($x = $left; $x -lt $right; $x++) {
                        Add-DeltaSample $accumulator ($baselineImage.GetPixel($x, $y)) ($enabledImage.GetPixel($x, $y))
                    }
                }

                $metrics = Complete-DeltaAccumulator $accumulator
                $cells.Add([object]([ordered]@{
                    row = $row
                    column = $column
                    bounds = [ordered]@{
                        left = $left
                        top = $top
                        width = $right - $left
                        height = $bottom - $top
                    }
                    intersectsFocus = Test-RectIntersects $left $top $right $bottom $focusLeft $focusTop $focusRight $focusBottom
                    metrics = $metrics
                }))
            }
        }

        for ($y = [Math]::Max(0, $focusTop); $y -lt [Math]::Min($baselineImage.Height, $focusBottom); $y++) {
            for ($x = [Math]::Max(0, $focusLeft); $x -lt [Math]::Min($baselineImage.Width, $focusRight); $x++) {
                Add-DeltaSample $focusAccumulator ($baselineImage.GetPixel($x, $y)) ($enabledImage.GetPixel($x, $y))
            }
        }
        $diagnosticFocusMetrics = Complete-DeltaAccumulator $focusAccumulator

        $outsideFocusCells = @($cells | Where-Object { -not $_.intersectsFocus })
        $topOutsideByAbsLuma = @(
            $outsideFocusCells |
                Sort-Object `
                    @{ Expression = { [double]$_.metrics.meanAbsLuma }; Descending = $true },
                    @{ Expression = { [double]$_.metrics.changedPixelPercent }; Descending = $true } |
                Select-Object -First 6
        )
        $topOutsideByBrightPercent = @(
            $outsideFocusCells |
                Sort-Object `
                    @{ Expression = { [double]$_.metrics.brighterPixelPercent }; Descending = $true },
                    @{ Expression = { [double]$_.metrics.meanSignedLuma }; Descending = $true } |
                Select-Object -First 6
        )

        return [ordered]@{
            grid = [ordered]@{
                columns = $columns
                rows = $rows
                cells = @($cells.ToArray())
                topOutsideFocusByMeanAbsLuma = @($topOutsideByAbsLuma)
                topOutsideFocusByBrighterPixelPercent = @($topOutsideByBrightPercent)
            }
            focusThresholdNearMiss = [ordered]@{
                changedPixelPercent = Get-ThresholdGap ([double]$FocusMetrics.changedPixelPercent) $MinFocusChangedPixelPercent
                brighterPixelPercent = Get-ThresholdGap ([double]$FocusMetrics.brighterPixelPercent) $MinFocusBrighterPixelPercent
                meanSignedLuma = Get-ThresholdGap ([double]$FocusMetrics.meanSignedLuma) $MinFocusMeanSignedLuma
                measuredFocusMetrics = $diagnosticFocusMetrics
                nearChangedPixels = $diagnosticFocusMetrics.nearChangedPixels
                nearChangedPixelPercent = $diagnosticFocusMetrics.nearChangedPixelPercent
                nearBrightPixels = $diagnosticFocusMetrics.nearBrightPixels
                nearBrightPixelPercent = $diagnosticFocusMetrics.nearBrightPixelPercent
                maxChannelBelowChangedThreshold = $diagnosticFocusMetrics.maxChannelBelowChangedThreshold
                maxPositiveLumaBelowBrightThreshold = $diagnosticFocusMetrics.maxPositiveLumaBelowBrightThreshold
            }
        }
    } finally {
        $enabledImage.Dispose()
        $baselineImage.Dispose()
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$enabledResolved = Resolve-ExistingFile $EnabledImagePath "Enabled image"
$debugResolved = ""
if (-not [string]::IsNullOrWhiteSpace($DebugImagePath)) {
    $debugResolved = Resolve-ExistingFile $DebugImagePath "Debug image"
}

$delta = Invoke-DeltaHelper $baselineResolved $enabledResolved
$baselineDimensions = Get-ImageDimensions $baselineResolved
$enabledDimensions = Get-ImageDimensions $enabledResolved
$debugDimensions = $null
if (-not [string]::IsNullOrWhiteSpace($debugResolved)) {
    $debugDimensions = Get-ImageDimensions $debugResolved
}

$logResolved = ""
$logProof = $null
if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
    $logResolved = Resolve-ExistingFile $LogPath "Log"
    $logProof = Measure-Round6LogProof $logResolved
}

$focusMetrics = $delta.focusRegionMetrics
$focusDeltaPassed = (
    ([double]$focusMetrics.changedPixelPercent -ge $MinFocusChangedPixelPercent) -and
    ([double]$focusMetrics.brighterPixelPercent -ge $MinFocusBrighterPixelPercent) -and
    ([double]$focusMetrics.meanSignedLuma -ge $MinFocusMeanSignedLuma)
)
$imageDiagnostics = Measure-ImageDeltaDiagnostics $baselineResolved $enabledResolved $delta.focusRegion $focusMetrics
$proofMarkerPathPatterns = @(
    "(?i)round6-gi-proof",
    "(?i)proof-overlay",
    "(?i)proof-marker",
    "(?i)visual-proof-marker"
)
$proofMarkerPathPresent = Test-AnyRegexInTexts `
    @($baselineResolved, $enabledResolved, $debugResolved, $logResolved, $OutputJsonPath) `
    $proofMarkerPathPatterns
$proofMarkerLogPresent = $false
if ($logProof) {
    $proofMarkerLogPresent = [bool]$logProof.markers.proofMarkerLogPresent
}
$proofMarkerContaminationPresent = $proofMarkerPathPresent -or $proofMarkerLogPresent
$roundSixDrawSubmittedPresent = $false
$roundSixNoMarkerSurfaceDrawPresent = $false
if ($logProof) {
    $roundSixDrawSubmittedPresent = [bool]$logProof.markers.roundSixDrawSubmittedPresent
    $roundSixNoMarkerSurfaceDrawPresent = [bool]$logProof.markers.roundSixNoMarkerSurfaceDrawPresent
}
$drawPresentButNoMarkerScreenshotDeltaFailed = (
    $roundSixNoMarkerSurfaceDrawPresent -and
    (-not $proofMarkerContaminationPresent) -and
    (-not $focusDeltaPassed)
)
$proofClassification = if ($proofMarkerContaminationPresent -and -not $AllowProofMarkerEvidence) {
    "proof_marker_contaminated"
} elseif ($drawPresentButNoMarkerScreenshotDeltaFailed) {
    "round6_draw_present_but_no_marker_screenshot_delta_failed"
} elseif ($focusDeltaPassed -and $roundSixNoMarkerSurfaceDrawPresent -and -not $proofMarkerContaminationPresent) {
    "no_marker_visible_gi_delta_passed"
} elseif ($focusDeltaPassed) {
    "screenshot_delta_passed"
} else {
    "screenshot_delta_failed"
}
$failures = New-Object System.Collections.Generic.List[string]
if ([double]$focusMetrics.changedPixelPercent -lt $MinFocusChangedPixelPercent) {
    $failures.Add("Focused region changed-pixel percentage below threshold. actual=$($focusMetrics.changedPixelPercent) expected>=$MinFocusChangedPixelPercent")
}
if ([double]$focusMetrics.brighterPixelPercent -lt $MinFocusBrighterPixelPercent) {
    $failures.Add("Focused region brighter-pixel percentage below threshold. actual=$($focusMetrics.brighterPixelPercent) expected>=$MinFocusBrighterPixelPercent")
}
if ([double]$focusMetrics.meanSignedLuma -lt $MinFocusMeanSignedLuma) {
    $failures.Add("Focused region mean signed luma below threshold. actual=$($focusMetrics.meanSignedLuma) expected>=$MinFocusMeanSignedLuma")
}
if ($RequireDebugScreenshot -and [string]::IsNullOrWhiteSpace($debugResolved)) {
    $failures.Add("Debug screenshot was required but no -DebugImagePath was provided.")
}
if ($debugDimensions -and (($debugDimensions.width -ne $baselineDimensions.width) -or ($debugDimensions.height -ne $baselineDimensions.height))) {
    $failures.Add("Debug image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) debug=$($debugDimensions.width)x$($debugDimensions.height)")
}
if ($RequireLogProof -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($proofMarkerContaminationPresent -and -not $AllowProofMarkerEvidence) {
    $failures.Add("Proof-marker contamination detected. pathMarker=$proofMarkerPathPresent logMarker=$proofMarkerLogPresent; rerun with no-marker capture artifacts for real Round 6 visible-GI proof.")
}
if ($logProof) {
    if (-not $logProof.markers.roundSixDispatchPresent) {
        $failures.Add("Missing Round 6 lighting dispatch prepared log marker.")
    }
    if (-not $logProof.markers.diffuseGiEnabled) {
        $failures.Add("Missing enabled diffuse GI marker.")
    }
    if (-not $logProof.markers.giSizePresent) {
        $failures.Add("Missing low-resolution diffuse GI size marker.")
    }
    if ([long]$logProof.maxima.giRays -lt $MinGiRays) {
        $failures.Add("GI rays below threshold. actual=$($logProof.maxima.giRays) expected>=$MinGiRays")
    }
    if ([long]$logProof.maxima.giCacheReads -lt $MinGiCacheReads) {
        $failures.Add("GI cache reads below threshold. actual=$($logProof.maxima.giCacheReads) expected>=$MinGiCacheReads")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
    if ($RequireNativeGiOutputSource -and -not $logProof.markers.nativeGiOutputSourcePresent) {
        $failures.Add("Missing native diffuse-GI output source marker distinct from the temporary direct-light payload.")
    }
    if ($RequireNativeGiOutputSource -and $logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log still contains the temporary direct-light payload source marker.")
    }
    if ($drawPresentButNoMarkerScreenshotDeltaFailed) {
        $failures.Add("Round 6 no-marker surface draw logs are present, but focused screenshot delta failed. drawPresent=$roundSixNoMarkerSurfaceDrawPresent changed=$($focusMetrics.changedPixelPercent) brighter=$($focusMetrics.brighterPixelPercent) meanSignedLuma=$($focusMetrics.meanSignedLuma)")
    }
}

$result = [ordered]@{
    baselineImage = $baselineResolved
    enabledImage = $enabledResolved
    debugImage = $debugResolved
    logPath = $logResolved
    thresholds = [ordered]@{
        minFocusChangedPixelPercent = $MinFocusChangedPixelPercent
        minFocusBrighterPixelPercent = $MinFocusBrighterPixelPercent
        minFocusMeanSignedLuma = $MinFocusMeanSignedLuma
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        minGiRays = $MinGiRays
        minGiCacheReads = $MinGiCacheReads
        requireNativeGiOutputSource = [bool]$RequireNativeGiOutputSource
        nativeGiOutputSourcePatterns = @($NativeGiOutputSourcePatterns)
        requireDebugScreenshot = [bool]$RequireDebugScreenshot
        requireLogProof = [bool]$RequireLogProof
        allowProofMarkerEvidence = [bool]$AllowProofMarkerEvidence
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        enabledDimensions = $enabledDimensions
        debugDimensions = $debugDimensions
        debugScreenshotProvided = -not [string]::IsNullOrWhiteSpace($debugResolved)
    }
    imageDelta = $delta
    imageDiagnostics = $imageDiagnostics
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = $proofClassification
        focusDeltaPassed = $focusDeltaPassed
        roundSixDrawSubmittedPresent = $roundSixDrawSubmittedPresent
        roundSixNoMarkerSurfaceDrawPresent = $roundSixNoMarkerSurfaceDrawPresent
        proofMarkerContaminationPresent = $proofMarkerContaminationPresent
        proofMarkerPathPresent = $proofMarkerPathPresent
        proofMarkerLogPresent = $proofMarkerLogPresent
        drawPresentButNoMarkerScreenshotDeltaFailed = $drawPresentButNoMarkerScreenshotDeltaFailed
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
Write-Host "logPath=$($result.logPath)"
Write-Host "focus.changedPixelPercent=$($focusMetrics.changedPixelPercent)"
Write-Host "focus.brighterPixelPercent=$($focusMetrics.brighterPixelPercent)"
Write-Host "focus.meanSignedLuma=$($focusMetrics.meanSignedLuma)"
Write-Host "diagnostics.focus.changedPixelPercent.missing=$($imageDiagnostics.focusThresholdNearMiss.changedPixelPercent.missing)"
Write-Host "diagnostics.focus.brighterPixelPercent.missing=$($imageDiagnostics.focusThresholdNearMiss.brighterPixelPercent.missing)"
Write-Host "diagnostics.focus.meanSignedLuma.missing=$($imageDiagnostics.focusThresholdNearMiss.meanSignedLuma.missing)"
Write-Host "diagnostics.focus.nearChangedPixelPercent=$($imageDiagnostics.focusThresholdNearMiss.nearChangedPixelPercent)"
Write-Host "diagnostics.focus.nearBrightPixelPercent=$($imageDiagnostics.focusThresholdNearMiss.nearBrightPixelPercent)"
Write-Host "diagnostics.grid.columns=$($imageDiagnostics.grid.columns)"
Write-Host "diagnostics.grid.rows=$($imageDiagnostics.grid.rows)"
foreach ($cell in @($imageDiagnostics.grid.topOutsideFocusByMeanAbsLuma | Select-Object -First 3)) {
    Write-Host "diagnostics.grid.topOutsideAbsLuma[$($cell.row),$($cell.column)]=meanAbsLuma=$($cell.metrics.meanAbsLuma),changed=$($cell.metrics.changedPixelPercent),brighter=$($cell.metrics.brighterPixelPercent),bounds=$($cell.bounds.left),$($cell.bounds.top),$($cell.bounds.width),$($cell.bounds.height)"
}
foreach ($cell in @($imageDiagnostics.grid.topOutsideFocusByBrighterPixelPercent | Select-Object -First 3)) {
    Write-Host "diagnostics.grid.topOutsideBright[$($cell.row),$($cell.column)]=meanSignedLuma=$($cell.metrics.meanSignedLuma),changed=$($cell.metrics.changedPixelPercent),brighter=$($cell.metrics.brighterPixelPercent),bounds=$($cell.bounds.left),$($cell.bounds.top),$($cell.bounds.width),$($cell.bounds.height)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "proof.focusDeltaPassed=$($result.proofClarity.focusDeltaPassed)"
Write-Host "proof.proofMarkerContaminationPresent=$($result.proofClarity.proofMarkerContaminationPresent)"
Write-Host "proof.drawPresentButNoMarkerScreenshotDeltaFailed=$($result.proofClarity.drawPresentButNoMarkerScreenshotDeltaFailed)"
if ($logProof) {
    Write-Host "roundSixDispatchPresent=$($logProof.markers.roundSixDispatchPresent)"
    Write-Host "diffuseGiEnabled=$($logProof.markers.diffuseGiEnabled)"
    Write-Host "giSizePresent=$($logProof.markers.giSizePresent)"
    Write-Host "nativeGiOutputSourcePresent=$($logProof.markers.nativeGiOutputSourcePresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "roundSixDrawSubmittedPresent=$($logProof.markers.roundSixDrawSubmittedPresent)"
    Write-Host "roundSixNoMarkerSurfaceDrawPresent=$($logProof.markers.roundSixNoMarkerSurfaceDrawPresent)"
    Write-Host "focusWindowPreviewDrawPresent=$($logProof.markers.focusWindowPreviewDrawPresent)"
    Write-Host "proofMarkerLogPresent=$($logProof.markers.proofMarkerLogPresent)"
    Write-Host "max.giRays=$($logProof.maxima.giRays)"
    Write-Host "max.giCacheReads=$($logProof.maxima.giCacheReads)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 6 visible-GI proof failed: $($failures -join '; ')"
}
