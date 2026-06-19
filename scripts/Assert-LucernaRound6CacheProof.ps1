param(
    [Parameter(Mandatory = $true)]
    [string] $LogPath,

    [string] $OutputJsonPath = "",

    [int] $MinCacheRecords = 1,

    [int] $MinCacheWrites = 1,

    [switch] $RequireDirtyRegionTelemetry
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingLog {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Log path does not exist: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).Path
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

$resolvedLog = Resolve-ExistingLog $LogPath
$log = Get-Content -Raw -LiteralPath $resolvedLog

$roundSixDispatchPresent = Test-Regex $log "Lucerna Round 6 lighting dispatch prepared:"
$diffuseGiEnabled = Test-Regex $log "diffuse_gi=\{\{?enabled=true,"
$cacheStageEnabled = Test-Regex $log "cache=\{\{?enabled=true,"
$sparseCacheStatusPresent = Test-Regex $log "Lucerna sparse radiance cache:"

$maxSparseRecords = Get-MaxRegexNumber $log "Lucerna sparse radiance cache: generation=\d+ records=(\d+)"
$maxDispatchCacheRecords = Get-MaxRegexNumber $log "cache=\{\{?enabled=true,records=(\d+),"
$maxCacheRecords = [Math]::Max($maxSparseRecords, $maxDispatchCacheRecords)

$maxSnakeCacheWrites = Get-MaxRegexNumber $log "cache_writes=(\d+)"
$maxCamelCacheWrites = Get-MaxRegexNumber $log "cacheWrites=(\d+)"
$maxCacheWrites = [Math]::Max($maxSnakeCacheWrites, $maxCamelCacheWrites)

$maxCacheReads = [Math]::Max(
    (Get-MaxRegexNumber $log "cache_reads=(\d+)"),
    (Get-MaxRegexNumber $log "cacheReads=(\d+)")
)
$maxDirtyRecords = Get-MaxRegexNumber $log "dirty_records=(\d+)"
$maxSourceDirty = Get-MaxRegexNumber $log "source_dirty=(\d+)"
$maxPendingDirty = Get-MaxRegexNumber $log "pending_dirty=(\d+)"
$dirtyRegionTelemetryPresent = $maxDirtyRecords -gt 0 -or $maxSourceDirty -gt 0 -or $maxPendingDirty -gt 0

$result = [ordered]@{
    logPath = $resolvedLog
    thresholds = [ordered]@{
        minCacheRecords = $MinCacheRecords
        minCacheWrites = $MinCacheWrites
        requireDirtyRegionTelemetry = [bool]$RequireDirtyRegionTelemetry
    }
    markers = [ordered]@{
        roundSixDispatchPresent = $roundSixDispatchPresent
        diffuseGiEnabled = $diffuseGiEnabled
        cacheStageEnabled = $cacheStageEnabled
        sparseCacheStatusPresent = $sparseCacheStatusPresent
        dirtyRegionTelemetryPresent = $dirtyRegionTelemetryPresent
    }
    maxima = [ordered]@{
        cacheRecords = $maxCacheRecords
        sparseRecords = $maxSparseRecords
        dispatchCacheRecords = $maxDispatchCacheRecords
        cacheWrites = $maxCacheWrites
        cacheReads = $maxCacheReads
        dirtyRecords = $maxDirtyRecords
        sourceDirty = $maxSourceDirty
        pendingDirty = $maxPendingDirty
    }
    passed = $false
}

$failures = New-Object System.Collections.Generic.List[string]
if (-not $roundSixDispatchPresent) {
    $failures.Add("Missing Round 6 lighting dispatch prepared log marker.")
}
if (-not $diffuseGiEnabled) {
    $failures.Add("Missing enabled diffuse GI dispatch marker.")
}
if (-not $cacheStageEnabled) {
    $failures.Add("Missing enabled cache stage marker.")
}
if (-not $sparseCacheStatusPresent) {
    $failures.Add("Missing sparse radiance cache status marker.")
}
if ($maxCacheRecords -lt $MinCacheRecords) {
    $failures.Add("Cache records below threshold. actual=$maxCacheRecords expected>=$MinCacheRecords")
}
if ($maxCacheWrites -lt $MinCacheWrites) {
    $failures.Add("Cache writes below threshold. actual=$maxCacheWrites expected>=$MinCacheWrites")
}
if ($RequireDirtyRegionTelemetry -and -not $dirtyRegionTelemetryPresent) {
    $failures.Add("Dirty-region cache telemetry was required but no nonzero dirty-region marker was found.")
}

$result.passed = $failures.Count -eq 0
$result.failures = @($failures)

$json = $result | ConvertTo-Json -Depth 8
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    $parent = Split-Path -Parent $OutputJsonPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Set-Content -LiteralPath $OutputJsonPath -Value $json -Encoding UTF8
}

Write-Host "logPath=$($result.logPath)"
Write-Host "roundSixDispatchPresent=$($result.markers.roundSixDispatchPresent)"
Write-Host "diffuseGiEnabled=$($result.markers.diffuseGiEnabled)"
Write-Host "cacheStageEnabled=$($result.markers.cacheStageEnabled)"
Write-Host "sparseCacheStatusPresent=$($result.markers.sparseCacheStatusPresent)"
Write-Host "dirtyRegionTelemetryPresent=$($result.markers.dirtyRegionTelemetryPresent)"
Write-Host "max.cacheRecords=$($result.maxima.cacheRecords)"
Write-Host "max.cacheWrites=$($result.maxima.cacheWrites)"
Write-Host "max.cacheReads=$($result.maxima.cacheReads)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 6 cache proof failed: $($failures -join '; ')"
}
