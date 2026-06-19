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

function Measure-Round6LogProof {
    param([string] $ResolvedLogPath)

    $log = Get-Content -Raw -LiteralPath $ResolvedLogPath
    $roundSixDispatchPresent = Test-Regex $log "Lucerna Round 6 lighting dispatch prepared:"
    $diffuseGiEnabled = Test-Regex $log "diffuse_gi=\{\{?enabled=true,"
    $cacheStagePresent = Test-Regex $log "cache=\{\{?enabled=true,"
    $giSizePresent = Test-Regex $log "diffuse_gi=\{\{?enabled=true,size=\d+x\d+"
    $cacheConfidencePresent = Test-Regex $log "(cache_confidence|confidence)="
    $debugOverlayPresent = Test-Regex $log "(Round 6|GI|cache).*debug|debug.*(Round 6|GI|cache)"
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
            nativeErrorPresent = $nativeErrorPresent
        }
        maxima = [ordered]@{
            giRays = $maxGiRays
            giCacheReads = $maxGiCacheReads
            giSamples = $maxGiSamples
        }
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
        requireDebugScreenshot = [bool]$RequireDebugScreenshot
        requireLogProof = [bool]$RequireLogProof
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        enabledDimensions = $enabledDimensions
        debugDimensions = $debugDimensions
        debugScreenshotProvided = -not [string]::IsNullOrWhiteSpace($debugResolved)
    }
    imageDelta = $delta
    logProof = $logProof
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
if ($logProof) {
    Write-Host "roundSixDispatchPresent=$($logProof.markers.roundSixDispatchPresent)"
    Write-Host "diffuseGiEnabled=$($logProof.markers.diffuseGiEnabled)"
    Write-Host "giSizePresent=$($logProof.markers.giSizePresent)"
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
