<#
.SYNOPSIS
Controller-only Round 11 assertion helper for ReSTIR reservoir debug evidence.

.DESCRIPTION
This script checks already captured screenshots and optional controller launch logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, validate shaders, or create render
evidence by itself. Use it after the controller has captured DirectReservoirDebug, GiReservoirDebug,
and ReservoirReuseDebug overlay artifacts.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $DirectReservoirDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $GiReservoirDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $ReservoirReuseDebugImagePath,

    [string[]] $LogPath = @(),

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 5.0,

    [double] $RegionTopPercent = 10.0,

    [double] $RegionWidthPercent = 90.0,

    [double] $RegionHeightPercent = 80.0,

    [switch] $DisableAutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 10.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 80.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6,

    [double] $MinGiReservoirChangedPixelPercent = 0.25,

    [double] $MinReservoirReuseChangedPixelPercent = 0.25,

    [string[]] $Round11MarkerPatterns = @(
        "Lucerna Round 11",
        "round11\.",
        "ReSTIR",
        "RESTIR",
        "reservoir"
    ),

    [string[]] $DirectReservoirOverlayPatterns = @(
        "directReservoirDebug(?:Visible|Submitted|Enabled)?=true",
        "round11\.directReservoirDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=direct-reservoir-debug",
        "round11ArtifactRole=direct-reservoir-debug",
        "debug\.overlay=DIRECT_RESERVOIR_DEBUG",
        "Overlay state: DIRECT_RESERVOIR_DEBUG"
    ),

    [string[]] $GiReservoirOverlayPatterns = @(
        "giReservoirDebug(?:Visible|Submitted|Enabled)?=true",
        "round11\.giReservoirDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=gi-reservoir-debug",
        "round11ArtifactRole=gi-reservoir-debug",
        "debug\.overlay=GI_RESERVOIR_DEBUG",
        "Overlay state: GI_RESERVOIR_DEBUG"
    ),

    [string[]] $ReservoirReuseOverlayPatterns = @(
        "reservoirReuseDebug(?:Visible|Submitted|Enabled)?=true",
        "round11\.reservoirReuseDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=reservoir-reuse-debug",
        "round11ArtifactRole=reservoir-reuse-debug",
        "debug\.overlay=RESERVOIR_REUSE_DEBUG",
        "Overlay state: RESERVOIR_REUSE_DEBUG"
    ),

    [string[]] $ReservoirCountPatterns = @(
        "reservoir(?:Count|s)?=([1-9][0-9]*)",
        "reservoir_count=([1-9][0-9]*)",
        "round11\.reservoir(?:Count|s)?=([1-9][0-9]*)",
        "round11\.directReservoir=.*count=([1-9][0-9]*)",
        "round11\.giReservoir=.*count=([1-9][0-9]*)"
    ),

    [string[]] $CandidateCountPatterns = @(
        "candidate(?:Count|s)?=([1-9][0-9]*)",
        "candidate_count=([1-9][0-9]*)",
        "selectedCandidateCount=([1-9][0-9]*)",
        "selected_candidate_count=([1-9][0-9]*)",
        "round11\.(?:direct|gi)?Candidate=.*count=([1-9][0-9]*)"
    ),

    [string[]] $TemporalReuseCountPatterns = @(
        "temporalReuse(?:Count|Accepted)?=([0-9]+)",
        "temporal_reuse(?:_count|_accepted)?=([0-9]+)",
        "round11\.temporalReuse=.*count=([0-9]+)"
    ),

    [string[]] $SpatialReuseCountPatterns = @(
        "spatialReuse(?:Count|Accepted)?=([0-9]+)",
        "spatial_reuse(?:_count|_accepted)?=([0-9]+)",
        "round11\.spatialReuse=.*count=([0-9]+)"
    ),

    [string[]] $PathReuseCountPatterns = @(
        "pathReuse(?:Count|Accepted)?=([0-9]+)",
        "path_reuse(?:_count|_accepted)?=([0-9]+)",
        "giPathReuseCount=([0-9]+)",
        "round11\.pathReuse=.*count=([0-9]+)"
    ),

    [string[]] $InvalidationCountPatterns = @(
        "invalidation(?:Count|s)?=([0-9]+)",
        "invalidation_count=([0-9]+)",
        "invalidatedReservoirs=([0-9]+)",
        "invalidated_reservoirs=([0-9]+)",
        "round11\.invalidation=.*count=([0-9]+)"
    ),

    [string[]] $ConfidenceStatsPatterns = @(
        "confidence=",
        "minConfidence=",
        "meanConfidence=",
        "maxConfidence=",
        "combinedConfidence=",
        "reservoir_confidence",
        "round11\.confidence="
    ),

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

function Resolve-OptionalFiles {
    param(
        [string[]] $Paths,
        [string] $Label
    )

    $resolved = New-Object System.Collections.Generic.List[string]
    foreach ($path in @($Paths)) {
        if ([string]::IsNullOrWhiteSpace($path)) {
            continue
        }
        $resolved.Add((Resolve-ExistingFile $path $Label)) | Out-Null
    }
    return $resolved.ToArray()
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round11-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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
                    -AutoRegionSearchHeightPercent $AutoRegionSearchHeightPercent `
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

function Get-CapturedNumbers {
    param(
        [string] $Text,
        [string[]] $Patterns
    )

    $numbers = New-Object System.Collections.Generic.List[double]
    foreach ($pattern in $Patterns) {
        $matches = [regex]::Matches($Text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        foreach ($match in $matches) {
            if ($match.Groups.Count -lt 2) {
                continue
            }
            $value = 0.0
            if ([double]::TryParse($match.Groups[1].Value, [ref]$value)) {
                $numbers.Add($value) | Out-Null
            }
        }
    }
    return $numbers.ToArray()
}

function Get-MaxNumber {
    param([double[]] $Numbers)

    if ($null -eq $Numbers -or $Numbers.Count -eq 0) {
        return $null
    }
    return ($Numbers | Measure-Object -Maximum).Maximum
}

function Measure-Round11LogProof {
    param([string[]] $ResolvedLogPaths)

    $combined = New-Object System.Text.StringBuilder
    foreach ($path in $ResolvedLogPaths) {
        [void]$combined.AppendLine("### LOG: $path")
        [void]$combined.AppendLine((Get-Content -Raw -LiteralPath $path))
    }
    $log = $combined.ToString()

    $reservoirCounts = Get-CapturedNumbers $log $ReservoirCountPatterns
    $candidateCounts = Get-CapturedNumbers $log $CandidateCountPatterns
    $temporalReuseCounts = Get-CapturedNumbers $log $TemporalReuseCountPatterns
    $spatialReuseCounts = Get-CapturedNumbers $log $SpatialReuseCountPatterns
    $pathReuseCounts = Get-CapturedNumbers $log $PathReuseCountPatterns
    $invalidationCounts = Get-CapturedNumbers $log $InvalidationCountPatterns

    return [ordered]@{
        logPaths = @($ResolvedLogPaths)
        markers = [ordered]@{
            round11MarkerPresent = Test-AnyRegex $log $Round11MarkerPatterns
            directReservoirOverlayPresent = Test-AnyRegex $log $DirectReservoirOverlayPatterns
            giReservoirOverlayPresent = Test-AnyRegex $log $GiReservoirOverlayPatterns
            reservoirReuseOverlayPresent = Test-AnyRegex $log $ReservoirReuseOverlayPatterns
            reservoirCountPresent = Test-AnyRegex $log $ReservoirCountPatterns
            candidateCountPresent = Test-AnyRegex $log $CandidateCountPatterns
            temporalReuseCountPresent = Test-AnyRegex $log $TemporalReuseCountPatterns
            spatialReuseCountPresent = Test-AnyRegex $log $SpatialReuseCountPatterns
            pathReuseCountPresent = Test-AnyRegex $log $PathReuseCountPatterns
            invalidationCountPresent = Test-AnyRegex $log $InvalidationCountPatterns
            confidenceStatsPresent = Test-AnyRegex $log $ConfidenceStatsPatterns
            invalidReservoirValuesPresent = Test-Regex $log "invalid(?:Reservoir|Restir|ReSTIR|Reuse)(?:Count|s)?=true|negative (?:reservoir|candidate|temporal reuse|spatial reuse|path reuse|invalidation)|(?:reservoir(?:Count|s)?|candidate(?:Count|s)?|temporalReuse|spatialReuse|pathReuse|confidence).*(?:NaN|Infinity)"
            proofMarkerPresent = Test-Regex $log "round11\.(?:proofMarker|proof_marker|focusWindowOnly|focus_window_only)=true|round11.*(?:proof-marker|proof marker|focus-window-only)|round11ArtifactRole=(?:proof|focus-window)"
            temporaryDirectLightSourcePresent = Test-Regex $log "round11\.(?:temporarySourceReady|temporary_source_ready|temporaryDirectLightSource|temporary_direct_light_source)=true|round11.*(?:temporary direct-light|current direct-light RGBA payload|using the current direct-light RGBA payload)"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            reservoirCounts = @($reservoirCounts)
            candidateCounts = @($candidateCounts)
            temporalReuseCounts = @($temporalReuseCounts)
            spatialReuseCounts = @($spatialReuseCounts)
            pathReuseCounts = @($pathReuseCounts)
            invalidationCounts = @($invalidationCounts)
            maxReservoirCount = Get-MaxNumber $reservoirCounts
            maxCandidateCount = Get-MaxNumber $candidateCounts
            maxTemporalReuseCount = Get-MaxNumber $temporalReuseCounts
            maxSpatialReuseCount = Get-MaxNumber $spatialReuseCounts
            maxPathReuseCount = Get-MaxNumber $pathReuseCounts
            maxInvalidationCount = Get-MaxNumber $invalidationCounts
        }
        patterns = [ordered]@{
            round11MarkerPatterns = @($Round11MarkerPatterns)
            directReservoirOverlayPatterns = @($DirectReservoirOverlayPatterns)
            giReservoirOverlayPatterns = @($GiReservoirOverlayPatterns)
            reservoirReuseOverlayPatterns = @($ReservoirReuseOverlayPatterns)
            reservoirCountPatterns = @($ReservoirCountPatterns)
            candidateCountPatterns = @($CandidateCountPatterns)
            temporalReuseCountPatterns = @($TemporalReuseCountPatterns)
            spatialReuseCountPatterns = @($SpatialReuseCountPatterns)
            pathReuseCountPatterns = @($PathReuseCountPatterns)
            invalidationCountPatterns = @($InvalidationCountPatterns)
            confidenceStatsPatterns = @($ConfidenceStatsPatterns)
        }
    }
}

$directResolved = Resolve-ExistingFile $DirectReservoirDebugImagePath "Direct reservoir debug image"
$giResolved = Resolve-ExistingFile $GiReservoirDebugImagePath "GI reservoir debug image"
$reuseResolved = Resolve-ExistingFile $ReservoirReuseDebugImagePath "Reservoir reuse debug image"
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$directDimensions = Get-ImageDimensions $directResolved
$giDimensions = Get-ImageDimensions $giResolved
$reuseDimensions = Get-ImageDimensions $reuseResolved

$giDelta = Invoke-DeltaHelper $directResolved $giResolved "gi-reservoir-debug"
$reuseDelta = Invoke-DeltaHelper $directResolved $reuseResolved "reservoir-reuse-debug"

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round11LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
    @{ label = "GI reservoir debug overlay"; dimensions = $giDimensions },
    @{ label = "reservoir reuse debug overlay"; dimensions = $reuseDimensions }
)) {
    if (($entry.dimensions.width -ne $directDimensions.width) -or ($entry.dimensions.height -ne $directDimensions.height)) {
        $failures.Add("$($entry.label) image dimensions differ from direct reservoir debug overlay. direct=$($directDimensions.width)x$($directDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ([double]$giDelta.focusRegionMetrics.changedPixelPercent -lt $MinGiReservoirChangedPixelPercent) {
    $failures.Add("GI reservoir debug overlay changed pixels below threshold. actual=$($giDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinGiReservoirChangedPixelPercent")
}
if ([double]$reuseDelta.focusRegionMetrics.changedPixelPercent -lt $MinReservoirReuseChangedPixelPercent) {
    $failures.Add("Reservoir reuse debug overlay changed pixels below threshold. actual=$($reuseDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinReservoirReuseChangedPixelPercent")
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.round11MarkerPresent) {
        $failures.Add("Missing Round 11/ReSTIR reservoir log marker.")
    }
    if (-not $logProof.markers.directReservoirOverlayPresent) {
        $failures.Add("Missing Round 11 direct reservoir debug overlay marker.")
    }
    if (-not $logProof.markers.giReservoirOverlayPresent) {
        $failures.Add("Missing Round 11 GI reservoir debug overlay marker.")
    }
    if (-not $logProof.markers.reservoirReuseOverlayPresent) {
        $failures.Add("Missing Round 11 reservoir reuse debug overlay marker.")
    }
    if (-not $logProof.markers.reservoirCountPresent -or $null -eq $logProof.counts.maxReservoirCount -or [double]$logProof.counts.maxReservoirCount -le 0) {
        $failures.Add("Missing nonzero Round 11 reservoir count marker.")
    }
    if (-not $logProof.markers.candidateCountPresent -or $null -eq $logProof.counts.maxCandidateCount -or [double]$logProof.counts.maxCandidateCount -le 0) {
        $failures.Add("Missing nonzero Round 11 candidate count marker.")
    }
    if (-not $logProof.markers.temporalReuseCountPresent) {
        $failures.Add("Missing Round 11 temporal reuse count marker.")
    }
    if (-not $logProof.markers.spatialReuseCountPresent) {
        $failures.Add("Missing Round 11 spatial reuse count marker.")
    }
    if (-not $logProof.markers.pathReuseCountPresent) {
        $failures.Add("Missing Round 11 path reuse count marker.")
    }
    if (-not $logProof.markers.invalidationCountPresent) {
        $failures.Add("Missing Round 11 invalidation count marker.")
    }
    if (-not $logProof.markers.confidenceStatsPresent) {
        $failures.Add("Missing Round 11 confidence stats marker.")
    }
    if ($logProof.markers.invalidReservoirValuesPresent) {
        $failures.Add("Log contains invalid Round 11 reservoir/reuse value markers.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; Round 11 proof must use reservoir debug paths.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker or focus-window-only evidence; Round 11 proof must use requested overlay artifacts.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$result = [ordered]@{
    overlayArtifacts = [ordered]@{
        directReservoirDebug = [ordered]@{
            path = $directResolved
            dimensions = $directDimensions
            role = "direct-reservoir-debug"
        }
        giReservoirDebug = [ordered]@{
            path = $giResolved
            dimensions = $giDimensions
            role = "gi-reservoir-debug"
        }
        reservoirReuseDebug = [ordered]@{
            path = $reuseResolved
            dimensions = $reuseDimensions
            role = "reservoir-reuse-debug"
        }
    }
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minGiReservoirChangedPixelPercent = $MinGiReservoirChangedPixelPercent
        minReservoirReuseChangedPixelPercent = $MinReservoirReuseChangedPixelPercent
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        focusRegionSelection = if ($DisableAutoFocusRegion) { "fixed" } else { "auto" }
        autoFocusRegion = [ordered]@{
            enabled = -not [bool]$DisableAutoFocusRegion
            searchLeftPercent = $AutoRegionSearchLeftPercent
            searchTopPercent = $AutoRegionSearchTopPercent
            searchWidthPercent = $AutoRegionSearchWidthPercent
            searchHeightPercent = $AutoRegionSearchHeightPercent
            columns = $AutoRegionColumns
            rows = $AutoRegionRows
            paddingCells = $AutoRegionPaddingCells
        }
        requireLogProof = [bool]$RequireLogProof
    }
    imageDelta = [ordered]@{
        directToGiReservoir = $giDelta
        directToReservoirReuse = $reuseDelta
    }
    selectedFocusRegions = [ordered]@{
        directToGiReservoir = $giDelta.focusRegion
        directToReservoirReuse = $reuseDelta.focusRegion
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round11_restir_reservoir_overlay_evidence_passed" } else { "round11_restir_reservoir_overlay_evidence_failed" }
        tracks = [ordered]@{
            reservoirInputs = [ordered]@{
                round11MarkerPresent = if ($logProof) { [bool]$logProof.markers.round11MarkerPresent } else { $null }
                reservoirCountPresent = if ($logProof) { [bool]$logProof.markers.reservoirCountPresent } else { $null }
                maxReservoirCount = if ($logProof) { $logProof.counts.maxReservoirCount } else { $null }
                candidateCountPresent = if ($logProof) { [bool]$logProof.markers.candidateCountPresent } else { $null }
                maxCandidateCount = if ($logProof) { $logProof.counts.maxCandidateCount } else { $null }
            }
            reuse = [ordered]@{
                temporalReuseCountPresent = if ($logProof) { [bool]$logProof.markers.temporalReuseCountPresent } else { $null }
                spatialReuseCountPresent = if ($logProof) { [bool]$logProof.markers.spatialReuseCountPresent } else { $null }
                pathReuseCountPresent = if ($logProof) { [bool]$logProof.markers.pathReuseCountPresent } else { $null }
                invalidationCountPresent = if ($logProof) { [bool]$logProof.markers.invalidationCountPresent } else { $null }
                confidenceStatsPresent = if ($logProof) { [bool]$logProof.markers.confidenceStatsPresent } else { $null }
            }
            overlays = [ordered]@{
                directReservoirOverlayPresent = if ($logProof) { [bool]$logProof.markers.directReservoirOverlayPresent } else { $null }
                giReservoirOverlayPresent = if ($logProof) { [bool]$logProof.markers.giReservoirOverlayPresent } else { $null }
                reservoirReuseOverlayPresent = if ($logProof) { [bool]$logProof.markers.reservoirReuseOverlayPresent } else { $null }
            }
            rejectionMarkers = [ordered]@{
                invalidReservoirValuesPresent = if ($logProof) { [bool]$logProof.markers.invalidReservoirValuesPresent } else { $null }
                temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
                proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
                nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
            }
            proofBoundary = [ordered]@{
                classification = "round11_reservoir_debug_overlay_and_telemetry_scaffold_not_physical_restir_quality_claim"
            }
        }
    }
    passed = $failures.Count -eq 0
    failures = @($failures)
}

$json = $result | ConvertTo-Json -Depth 14
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    $parent = Split-Path -Parent $OutputJsonPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Set-Content -LiteralPath $OutputJsonPath -Value $json -Encoding UTF8
}

Write-Host "directReservoirDebug=$($result.overlayArtifacts.directReservoirDebug.path)"
Write-Host "giReservoirDebug=$($result.overlayArtifacts.giReservoirDebug.path)"
Write-Host "reservoirReuseDebug=$($result.overlayArtifacts.reservoirReuseDebug.path)"
Write-Host "logPaths=$($result.logPaths -join ';')"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "giReservoir.focus.changedPixelPercent=$($giDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "reservoirReuse.focus.changedPixelPercent=$($reuseDelta.focusRegionMetrics.changedPixelPercent)"
if ($logProof) {
    Write-Host "round11MarkerPresent=$($logProof.markers.round11MarkerPresent)"
    Write-Host "directReservoirOverlayPresent=$($logProof.markers.directReservoirOverlayPresent)"
    Write-Host "giReservoirOverlayPresent=$($logProof.markers.giReservoirOverlayPresent)"
    Write-Host "reservoirReuseOverlayPresent=$($logProof.markers.reservoirReuseOverlayPresent)"
    Write-Host "reservoirCountPresent=$($logProof.markers.reservoirCountPresent)"
    Write-Host "maxReservoirCount=$($logProof.counts.maxReservoirCount)"
    Write-Host "candidateCountPresent=$($logProof.markers.candidateCountPresent)"
    Write-Host "maxCandidateCount=$($logProof.counts.maxCandidateCount)"
    Write-Host "temporalReuseCountPresent=$($logProof.markers.temporalReuseCountPresent)"
    Write-Host "spatialReuseCountPresent=$($logProof.markers.spatialReuseCountPresent)"
    Write-Host "pathReuseCountPresent=$($logProof.markers.pathReuseCountPresent)"
    Write-Host "invalidationCountPresent=$($logProof.markers.invalidationCountPresent)"
    Write-Host "confidenceStatsPresent=$($logProof.markers.confidenceStatsPresent)"
    Write-Host "invalidReservoirValuesPresent=$($logProof.markers.invalidReservoirValuesPresent)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 11 ReSTIR reservoir proof failed: $($failures -join '; ')"
}
