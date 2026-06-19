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

    [switch] $AutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 10.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 80.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinFocusChangedPixelPercent = 1.0,

    [double] $MinFocusBrighterPixelPercent = 0.5,

    [double] $MinFocusMeanSignedLuma = 0.5,

    [long] $MinEmissiveCandidates = 1,

    [long] $MinShadowCandidates = 1,

    [long] $MinSurfaceSamples = 1,

    [double] $MinDirectOutputEnergy = 1.0,

    [long] $MinDirectOutputChecksum = 1,

    [string[]] $DirectPayloadPatterns = @(
        "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
        "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated"
    ),

    [string[]] $FinalSurfaceCompositePatterns = @(
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*mode=(?![^`r`n]*focus-window)[^`r`n]*(?:direct|emissive))(?=[^`r`n]*(?:surface|world|final|composite))"
    ),

    [switch] $RequireDebugScreenshot,

    [switch] $RequireLogProof
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
        [string] $EnabledPath
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round5-direct-surface-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    try {
        if ($AutoFocusRegion) {
            & $compareScript `
                -BaselineImagePath:$BaselinePath `
                -EnabledImagePath:$EnabledPath `
                -OutputJsonPath:$tempJson `
                -RegionLeftPercent:$RegionLeftPercent `
                -RegionTopPercent:$RegionTopPercent `
                -RegionWidthPercent:$RegionWidthPercent `
                -RegionHeightPercent:$RegionHeightPercent `
                -ChangedPixelThreshold:$ChangedPixelThreshold `
                -BrightPixelThreshold:$BrightPixelThreshold `
                -AutoFocusRegion `
                -AutoRegionSearchLeftPercent:$AutoRegionSearchLeftPercent `
                -AutoRegionSearchTopPercent:$AutoRegionSearchTopPercent `
                -AutoRegionSearchWidthPercent:$AutoRegionSearchWidthPercent `
                -AutoRegionSearchHeightPercent:$AutoRegionSearchHeightPercent `
                -AutoRegionColumns:$AutoRegionColumns `
                -AutoRegionRows:$AutoRegionRows `
                -AutoRegionPaddingCells:$AutoRegionPaddingCells | Out-Host
        } else {
            & $compareScript `
                -BaselineImagePath:$BaselinePath `
                -EnabledImagePath:$EnabledPath `
                -OutputJsonPath:$tempJson `
                -RegionLeftPercent:$RegionLeftPercent `
                -RegionTopPercent:$RegionTopPercent `
                -RegionWidthPercent:$RegionWidthPercent `
                -RegionHeightPercent:$RegionHeightPercent `
                -ChangedPixelThreshold:$ChangedPixelThreshold `
                -BrightPixelThreshold:$BrightPixelThreshold | Out-Host
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

function Test-AnyRegexInTexts {
    param(
        [string[]] $Texts,
        [string[]] $Patterns
    )

    foreach ($text in $Texts) {
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }
        if (Test-AnyRegex $text $Patterns) {
            return $true
        }
    }
    return $false
}

function Get-MaxRegexLong {
    param(
        [string] $Text,
        [string] $Pattern
    )

    $max = 0L
    foreach ($match in [regex]::Matches($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        if ($match.Groups.Count -lt 2) {
            continue
        }
        $value = 0L
        if ([long]::TryParse($match.Groups[1].Value, [ref]$value) -and $value -gt $max) {
            $max = $value
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
        $value = 0.0
        if ([double]::TryParse(
                $match.Groups[1].Value,
                [System.Globalization.NumberStyles]::Float,
                [System.Globalization.CultureInfo]::InvariantCulture,
                [ref]$value
            ) -and $value -gt $max) {
            $max = $value
        }
    }
    return $max
}

function Measure-Round5DirectSurfaceLogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
    $directPlanPresent = Test-Regex $log "Lucerna direct lighting plan:"
    $directPayloadPresent = Test-AnyRegex $log $DirectPayloadPatterns
    $nativeDirectExecutionPresent = Test-Regex $log "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true"
    $finalSurfaceCompositePresent = Test-AnyRegex $log $FinalSurfaceCompositePatterns
    $finalCompositeSubmittedPresent = Test-Regex $log "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true"
    $focusWindowOnlyPresent = Test-Regex $log "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|mode=final-composite-direct-light-focus-window-additive|focus-window-only"
    $temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporary direct-light|current direct-light RGBA payload|using the current direct-light RGBA payload as the temporary visible source"
    $proofMarkerPresent = Test-Regex $log "round5-direct-proof|R5 visual proof|round6-gi-proof|R6 GI proof|R7 proof|proof marker|CPU output proof"
    $nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"

    $maxEmissiveCandidates = Get-MaxRegexLong $log "emissive=(\d+)"
    $maxShadowCandidates = Get-MaxRegexLong $log "shadowCandidates=(\d+)"
    $maxSurfaceSampleSections = Get-MaxRegexLong $log "surfaceSampleSections=(\d+)"
    $maxSurfaceSamples = Get-MaxRegexLong $log "surfaceSamples=(\d+)"
    $maxDirectOutputEnergy = Get-MaxRegexDouble $log "cpuOutputEnergy=([0-9]+(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)"
    $maxDirectOutputChecksum = Get-MaxRegexLong $log "cpuOutputChecksum=(\d+)"

    return [ordered]@{
        markers = [ordered]@{
            directPlanPresent = $directPlanPresent
            directPayloadPresent = $directPayloadPresent
            nativeDirectExecutionPresent = $nativeDirectExecutionPresent
            finalCompositeSubmittedPresent = $finalCompositeSubmittedPresent
            finalSurfaceCompositePresent = $finalSurfaceCompositePresent
            focusWindowOnlyPresent = $focusWindowOnlyPresent
            temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
            proofMarkerPresent = $proofMarkerPresent
            nativeErrorPresent = $nativeErrorPresent
        }
        maxima = [ordered]@{
            emissiveCandidates = $maxEmissiveCandidates
            shadowCandidates = $maxShadowCandidates
            surfaceSampleSections = $maxSurfaceSampleSections
            surfaceSamples = $maxSurfaceSamples
            directOutputEnergy = $maxDirectOutputEnergy
            directOutputChecksum = $maxDirectOutputChecksum
        }
        patterns = [ordered]@{
            directPayloadPatterns = @($DirectPayloadPatterns)
            finalSurfaceCompositePatterns = @($FinalSurfaceCompositePatterns)
        }
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$enabledResolved = Resolve-ExistingFile $EnabledImagePath "Enabled image"
$debugResolved = Resolve-OptionalFile $DebugImagePath "Debug image"
$logResolved = Resolve-OptionalFile $LogPath "Log"

$delta = Invoke-DeltaHelper $baselineResolved $enabledResolved
$baselineDimensions = Get-ImageDimensions $baselineResolved
$enabledDimensions = Get-ImageDimensions $enabledResolved
$debugDimensions = if ([string]::IsNullOrWhiteSpace($debugResolved)) { $null } else { Get-ImageDimensions $debugResolved }
$logProof = if ([string]::IsNullOrWhiteSpace($logResolved)) { $null } else { Measure-Round5DirectSurfaceLogProof $logResolved }

$focusMetrics = $delta.focusRegionMetrics
$focusDeltaPassed = (
    ([double]$focusMetrics.changedPixelPercent -ge $MinFocusChangedPixelPercent) -and
    ([double]$focusMetrics.brighterPixelPercent -ge $MinFocusBrighterPixelPercent) -and
    ([double]$focusMetrics.meanSignedLuma -ge $MinFocusMeanSignedLuma)
)
$proofMarkerPathPatterns = @(
    "(?i)round5-direct-proof",
    "(?i)proof-overlay",
    "(?i)proof-marker",
    "(?i)visual-proof-marker"
)
$proofMarkerPathPresent = Test-AnyRegexInTexts `
    @($baselineResolved, $enabledResolved, $debugResolved, $logResolved, $OutputJsonPath) `
    $proofMarkerPathPatterns
$proofMarkerLogPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $false }
$proofMarkerContaminationPresent = $proofMarkerPathPresent -or $proofMarkerLogPresent
$focusWindowOnlyPresent = if ($logProof) { [bool]$logProof.markers.focusWindowOnlyPresent } else { $false }
$temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $false }
$drawPresentButSurfaceDeltaFailed = (
    $logProof -and
    $logProof.markers.finalSurfaceCompositePresent -and
    (-not $proofMarkerContaminationPresent) -and
    (-not $focusWindowOnlyPresent) -and
    (-not $focusDeltaPassed)
)
$proofClassification = if ($proofMarkerContaminationPresent) {
    "proof_marker_contaminated"
} elseif ($focusWindowOnlyPresent) {
    "focus_window_contaminated"
} elseif ($temporaryDirectLightSourcePresent) {
    "temporary_direct_source_contaminated"
} elseif ($drawPresentButSurfaceDeltaFailed) {
    "direct_surface_draw_present_but_screenshot_delta_failed"
} elseif ($focusDeltaPassed -and $logProof -and $logProof.markers.finalSurfaceCompositePresent) {
    "direct_emissive_surface_delta_passed"
} elseif ($focusDeltaPassed) {
    "screenshot_delta_passed"
} else {
    "screenshot_delta_failed"
}

$failures = New-Object System.Collections.Generic.List[string]
if (($enabledDimensions.width -ne $baselineDimensions.width) -or ($enabledDimensions.height -ne $baselineDimensions.height)) {
    $failures.Add("Enabled image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) enabled=$($enabledDimensions.width)x$($enabledDimensions.height)")
}
if ($debugDimensions -and (($debugDimensions.width -ne $baselineDimensions.width) -or ($debugDimensions.height -ne $baselineDimensions.height))) {
    $failures.Add("Debug image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) debug=$($debugDimensions.width)x$($debugDimensions.height)")
}
if ([double]$focusMetrics.changedPixelPercent -lt $MinFocusChangedPixelPercent) {
    $failures.Add("Focused surface changed-pixel percentage below threshold. actual=$($focusMetrics.changedPixelPercent) expected>=$MinFocusChangedPixelPercent")
}
if ([double]$focusMetrics.brighterPixelPercent -lt $MinFocusBrighterPixelPercent) {
    $failures.Add("Focused surface brighter-pixel percentage below threshold. actual=$($focusMetrics.brighterPixelPercent) expected>=$MinFocusBrighterPixelPercent")
}
if ([double]$focusMetrics.meanSignedLuma -lt $MinFocusMeanSignedLuma) {
    $failures.Add("Focused surface mean signed luma below threshold. actual=$($focusMetrics.meanSignedLuma) expected>=$MinFocusMeanSignedLuma")
}
if ($RequireDebugScreenshot -and [string]::IsNullOrWhiteSpace($debugResolved)) {
    $failures.Add("Debug screenshot was required but no -DebugImagePath was provided.")
}
if ($RequireLogProof -and [string]::IsNullOrWhiteSpace($logResolved)) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($proofMarkerContaminationPresent) {
    $failures.Add("Proof-marker contamination detected. pathMarker=$proofMarkerPathPresent logMarker=$proofMarkerLogPresent; rerun with no-marker direct surface capture artifacts.")
}
if ($focusWindowOnlyPresent) {
    $failures.Add("Log contains focus-window-only direct-light evidence; final direct/emissive surface proof must not use the focus-window path.")
}
if ($temporaryDirectLightSourcePresent) {
    $failures.Add("Log contains temporary direct-light source marker; final direct/emissive surface proof must use the real direct surface composite path.")
}
if ($logProof) {
    if (-not $logProof.markers.directPlanPresent) {
        $failures.Add("Missing direct lighting plan log marker.")
    }
    if (-not $logProof.markers.directPayloadPresent) {
        $failures.Add("Missing real native direct payload/output log markers.")
    }
    if (-not $logProof.markers.nativeDirectExecutionPresent) {
        $failures.Add("Missing native direct execution output-write/resolve log marker.")
    }
    if (-not $logProof.markers.finalSurfaceCompositePresent) {
        $failures.Add("Missing final direct/emissive surface composite submission marker.")
    }
    if ([long]$logProof.maxima.emissiveCandidates -lt $MinEmissiveCandidates) {
        $failures.Add("Emissive candidate count below threshold. actual=$($logProof.maxima.emissiveCandidates) expected>=$MinEmissiveCandidates")
    }
    if ([long]$logProof.maxima.shadowCandidates -lt $MinShadowCandidates) {
        $failures.Add("Direct shadow candidate count below threshold. actual=$($logProof.maxima.shadowCandidates) expected>=$MinShadowCandidates")
    }
    if ([long]$logProof.maxima.surfaceSamples -lt $MinSurfaceSamples) {
        $failures.Add("Surface sample count below threshold. actual=$($logProof.maxima.surfaceSamples) expected>=$MinSurfaceSamples")
    }
    if ([double]$logProof.maxima.directOutputEnergy -lt $MinDirectOutputEnergy) {
        $failures.Add("Direct output energy below threshold. actual=$($logProof.maxima.directOutputEnergy) expected>=$MinDirectOutputEnergy")
    }
    if ([long]$logProof.maxima.directOutputChecksum -lt $MinDirectOutputChecksum) {
        $failures.Add("Direct output checksum below threshold. actual=$($logProof.maxima.directOutputChecksum) expected>=$MinDirectOutputChecksum")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
    if ($drawPresentButSurfaceDeltaFailed) {
        $failures.Add("Final direct surface composite logs are present, but focused screenshot delta failed. changed=$($focusMetrics.changedPixelPercent) brighter=$($focusMetrics.brighterPixelPercent) meanSignedLuma=$($focusMetrics.meanSignedLuma)")
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
        minEmissiveCandidates = $MinEmissiveCandidates
        minShadowCandidates = $MinShadowCandidates
        minSurfaceSamples = $MinSurfaceSamples
        minDirectOutputEnergy = $MinDirectOutputEnergy
        minDirectOutputChecksum = $MinDirectOutputChecksum
        requireDebugScreenshot = [bool]$RequireDebugScreenshot
        requireLogProof = [bool]$RequireLogProof
        focusRegionSelection = if ($AutoFocusRegion) { "auto" } else { "fixed" }
        autoFocusRegion = [ordered]@{
            enabled = [bool]$AutoFocusRegion
            searchLeftPercent = $AutoRegionSearchLeftPercent
            searchTopPercent = $AutoRegionSearchTopPercent
            searchWidthPercent = $AutoRegionSearchWidthPercent
            searchHeightPercent = $AutoRegionSearchHeightPercent
            columns = $AutoRegionColumns
            rows = $AutoRegionRows
            paddingCells = $AutoRegionPaddingCells
        }
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        enabledDimensions = $enabledDimensions
        debugDimensions = $debugDimensions
        debugScreenshotProvided = -not [string]::IsNullOrWhiteSpace($debugResolved)
    }
    imageDelta = $delta
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = $proofClassification
        focusDeltaPassed = $focusDeltaPassed
        proofMarkerContaminationPresent = $proofMarkerContaminationPresent
        focusWindowOnlyPresent = $focusWindowOnlyPresent
        temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
        drawPresentButSurfaceDeltaFailed = $drawPresentButSurfaceDeltaFailed
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
Write-Host "focusRegion=$($delta.focusRegion.left),$($delta.focusRegion.top),$($delta.focusRegion.width),$($delta.focusRegion.height)"
Write-Host "focusRegion.selectionMode=$($delta.focusRegion.selectionMode)"
Write-Host "focus.changedPixelPercent=$($focusMetrics.changedPixelPercent)"
Write-Host "focus.brighterPixelPercent=$($focusMetrics.brighterPixelPercent)"
Write-Host "focus.meanSignedLuma=$($focusMetrics.meanSignedLuma)"
if ($logProof) {
    Write-Host "directPlanPresent=$($logProof.markers.directPlanPresent)"
    Write-Host "directPayloadPresent=$($logProof.markers.directPayloadPresent)"
    Write-Host "nativeDirectExecutionPresent=$($logProof.markers.nativeDirectExecutionPresent)"
    Write-Host "finalCompositeSubmittedPresent=$($logProof.markers.finalCompositeSubmittedPresent)"
    Write-Host "finalSurfaceCompositePresent=$($logProof.markers.finalSurfaceCompositePresent)"
    Write-Host "focusWindowOnlyPresent=$($logProof.markers.focusWindowOnlyPresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "max.emissiveCandidates=$($logProof.maxima.emissiveCandidates)"
    Write-Host "max.shadowCandidates=$($logProof.maxima.shadowCandidates)"
    Write-Host "max.surfaceSamples=$($logProof.maxima.surfaceSamples)"
    Write-Host "max.directOutputEnergy=$($logProof.maxima.directOutputEnergy)"
    Write-Host "max.directOutputChecksum=$($logProof.maxima.directOutputChecksum)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "proof.focusDeltaPassed=$($result.proofClarity.focusDeltaPassed)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 5 direct/emissive surface proof failed: $($failures -join '; ')"
}
