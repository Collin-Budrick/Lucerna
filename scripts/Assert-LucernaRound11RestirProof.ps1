<#
.SYNOPSIS
Controller-only Round 11 assertion helper for ReSTIR reservoir debug and execution evidence.

.DESCRIPTION
This script checks already captured screenshots and optional controller launch logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, validate shaders, or create render
evidence by itself. Existing overlay-only calls can use DirectReservoirDebug, GiReservoirDebug,
and ReservoirReuseDebug artifacts. Stricter execution calls can also provide DirectBruteBaseline,
RestirDirectEnabled, RestirTemporalStable, RestirTemporalMoved, and RestirExecutionDebug artifacts.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $DirectReservoirDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $GiReservoirDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $ReservoirReuseDebugImagePath,

    [string] $DirectBruteBaselineImagePath = "",

    [string] $RestirDirectEnabledImagePath = "",

    [string] $RestirTemporalStableImagePath = "",

    [string[]] $RestirTemporalStableRepeatImagePath = @(),

    [string] $RestirTemporalMovedImagePath = "",

    [string] $RestirExecutionDebugImagePath = "",

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

    [double] $MinRestirDirectChangedPixelPercent = 0.25,

    [double] $MinRestirMovedChangedPixelPercent = 0.25,

    [double] $MaxRestirStableChangedPixelPercent = 8.0,

    [double] $MinCandidateReductionRatio = 1.10,

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

    [string[]] $InputCandidateCountPatterns = @(
        "inputCandidate(?:Count|s)?=([1-9][0-9]*)",
        "candidateInput(?:Count|s)?=([1-9][0-9]*)",
        "rawCandidate(?:Count|s)?=([1-9][0-9]*)",
        "(?<!selected)candidate(?:Count|s)?=([1-9][0-9]*)",
        "candidate_count=([1-9][0-9]*)",
        "round11\.(?:input|raw)?Candidate(?:Count|s)?=([1-9][0-9]*)",
        "round11\.(?:restirDi|directReservoir).*(?<!selected)candidate(?:Count|s)?=([1-9][0-9]*)"
    ),

    [string[]] $SelectedCountPatterns = @(
        "selected(?:Candidate)?(?:Count|s)?=([1-9][0-9]*)",
        "selected_candidate_count=([1-9][0-9]*)",
        "selectedReservoir(?:Count|s)?=([1-9][0-9]*)",
        "round11\.selected(?:Candidate)?Count=([1-9][0-9]*)",
        "round11\.(?:restirDi|directReservoir).*selected(?:Candidate)?(?:Count|s)?=([1-9][0-9]*)"
    ),

    [string[]] $CandidateReductionRatioPatterns = @(
        "candidateReductionRatio=([1-9][0-9]*(?:\.[0-9]+)?|[0-9]+\.[0-9]*[1-9][0-9]*)",
        "candidate_reduction_ratio=([1-9][0-9]*(?:\.[0-9]+)?|[0-9]+\.[0-9]*[1-9][0-9]*)",
        "round11\.candidateReductionRatio=([1-9][0-9]*(?:\.[0-9]+)?|[0-9]+\.[0-9]*[1-9][0-9]*)"
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

    [string[]] $RestirExecutionMarkerPatterns = @(
        "Lucerna Round 11 ReSTIR DI execution",
        "round11\.(?:restirDi|restirDI|directExecution|execution).*?(?:executed|enabled|ready)=(?:true|1)",
        "restirDirectExecution(?:Ready|Enabled|Executed)?=(?:true|1)",
        "realRestirDiExecution=(?:true|1)",
        "restirDiExecutionPresent=(?:true|1)",
        "ReSTIR DI.*(?:executed|enabled|ready)"
    ),

    [string[]] $OutputEnergyPatterns = @(
        "restir(?:Direct|Di|DI)?OutputEnergy=([1-9][0-9.eE+-]*)",
        "outputEnergy=([1-9][0-9.eE+-]*)",
        "cpuOutputEnergy=([1-9][0-9.eE+-]*)",
        "round11\.(?:restirDi|directOutput).*energy=([1-9][0-9.eE+-]*)"
    ),

    [string[]] $OutputChecksumPatterns = @(
        "restir(?:Direct|Di|DI)?OutputChecksum=([1-9][0-9]*)",
        "outputChecksum=([1-9][0-9]*)",
        "cpuOutputChecksum=([1-9][0-9]*)",
        "round11\.(?:restirDi|directOutput).*checksum=([1-9][0-9]*)"
    ),

    [switch] $RequireLogProof,

    [switch] $RequireRestirExecutionProof,

    [switch] $RequireCandidateReduction,

    [switch] $RequireSelectedCount,

    [switch] $RequireTemporalReuse,

    [switch] $RequireSpatialReuse,

    [switch] $RequireOutputEnergy,

    [switch] $RequireOutputChecksum,

    [switch] $RequireSameSceneDelta,

    [switch] $RequireStabilityComparison
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

function Get-CandidateReductionRatio {
    param(
        [double[]] $InputCandidateCounts,
        [double[]] $SelectedCounts,
        [double[]] $ExplicitRatios
    )

    $explicitMax = Get-MaxNumber $ExplicitRatios
    if ($null -ne $explicitMax) {
        return [double]$explicitMax
    }

    $maxInput = Get-MaxNumber $InputCandidateCounts
    $maxSelected = Get-MaxNumber $SelectedCounts
    if ($null -eq $maxInput -or $null -eq $maxSelected -or [double]$maxSelected -le 0) {
        return $null
    }
    return [double]$maxInput / [double]$maxSelected
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
    $inputCandidateCounts = Get-CapturedNumbers $log $InputCandidateCountPatterns
    $selectedCounts = Get-CapturedNumbers $log $SelectedCountPatterns
    $candidateReductionRatios = Get-CapturedNumbers $log $CandidateReductionRatioPatterns
    $temporalReuseCounts = Get-CapturedNumbers $log $TemporalReuseCountPatterns
    $spatialReuseCounts = Get-CapturedNumbers $log $SpatialReuseCountPatterns
    $pathReuseCounts = Get-CapturedNumbers $log $PathReuseCountPatterns
    $invalidationCounts = Get-CapturedNumbers $log $InvalidationCountPatterns
    $outputEnergies = Get-CapturedNumbers $log $OutputEnergyPatterns
    $outputChecksums = Get-CapturedNumbers $log $OutputChecksumPatterns
    $candidateReductionRatio = Get-CandidateReductionRatio $inputCandidateCounts $selectedCounts $candidateReductionRatios

    return [ordered]@{
        logPaths = @($ResolvedLogPaths)
        markers = [ordered]@{
            round11MarkerPresent = Test-AnyRegex $log $Round11MarkerPatterns
            directReservoirOverlayPresent = Test-AnyRegex $log $DirectReservoirOverlayPatterns
            giReservoirOverlayPresent = Test-AnyRegex $log $GiReservoirOverlayPatterns
            reservoirReuseOverlayPresent = Test-AnyRegex $log $ReservoirReuseOverlayPatterns
            restirExecutionMarkerPresent = Test-AnyRegex $log $RestirExecutionMarkerPatterns
            reservoirCountPresent = Test-AnyRegex $log $ReservoirCountPatterns
            candidateCountPresent = Test-AnyRegex $log $CandidateCountPatterns
            inputCandidateCountPresent = Test-AnyRegex $log $InputCandidateCountPatterns
            selectedCountPresent = Test-AnyRegex $log $SelectedCountPatterns
            candidateReductionRatioPresent = Test-AnyRegex $log $CandidateReductionRatioPatterns
            temporalReuseCountPresent = Test-AnyRegex $log $TemporalReuseCountPatterns
            spatialReuseCountPresent = Test-AnyRegex $log $SpatialReuseCountPatterns
            pathReuseCountPresent = Test-AnyRegex $log $PathReuseCountPatterns
            invalidationCountPresent = Test-AnyRegex $log $InvalidationCountPatterns
            confidenceStatsPresent = Test-AnyRegex $log $ConfidenceStatsPatterns
            outputEnergyPresent = Test-AnyRegex $log $OutputEnergyPatterns
            outputChecksumPresent = Test-AnyRegex $log $OutputChecksumPatterns
            invalidReservoirValuesPresent = Test-Regex $log "invalid(?:Reservoir|Restir|ReSTIR|Reuse)(?:Count|s)?=true|negative (?:reservoir|candidate|temporal reuse|spatial reuse|path reuse|invalidation)|(?:reservoir(?:Count|s)?|candidate(?:Count|s)?|temporalReuse|spatialReuse|pathReuse|confidence).*(?:NaN|Infinity)"
            proofMarkerPresent = Test-Regex $log "round11\.(?:proofMarker|proof_marker|focusWindowOnly|focus_window_only)=true|round11.*(?:proof-marker|proof marker|focus-window-only)|round11ArtifactRole=(?:proof|focus-window)"
            temporaryDirectLightSourcePresent = Test-Regex $log "round11\.(?:temporarySourceReady|temporary_source_ready|temporaryDirectLightSource|temporary_direct_light_source)=true|round11.*(?:temporary direct-light|current direct-light RGBA payload|using the current direct-light RGBA payload)"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            reservoirCounts = @($reservoirCounts)
            candidateCounts = @($candidateCounts)
            inputCandidateCounts = @($inputCandidateCounts)
            selectedCounts = @($selectedCounts)
            candidateReductionRatios = @($candidateReductionRatios)
            temporalReuseCounts = @($temporalReuseCounts)
            spatialReuseCounts = @($spatialReuseCounts)
            pathReuseCounts = @($pathReuseCounts)
            invalidationCounts = @($invalidationCounts)
            outputEnergies = @($outputEnergies)
            outputChecksums = @($outputChecksums)
            maxReservoirCount = Get-MaxNumber $reservoirCounts
            maxCandidateCount = Get-MaxNumber $candidateCounts
            maxInputCandidateCount = Get-MaxNumber $inputCandidateCounts
            maxSelectedCount = Get-MaxNumber $selectedCounts
            candidateReductionRatio = $candidateReductionRatio
            maxTemporalReuseCount = Get-MaxNumber $temporalReuseCounts
            maxSpatialReuseCount = Get-MaxNumber $spatialReuseCounts
            maxPathReuseCount = Get-MaxNumber $pathReuseCounts
            maxInvalidationCount = Get-MaxNumber $invalidationCounts
            maxOutputEnergy = Get-MaxNumber $outputEnergies
            maxOutputChecksum = Get-MaxNumber $outputChecksums
        }
        patterns = [ordered]@{
            round11MarkerPatterns = @($Round11MarkerPatterns)
            directReservoirOverlayPatterns = @($DirectReservoirOverlayPatterns)
            giReservoirOverlayPatterns = @($GiReservoirOverlayPatterns)
            reservoirReuseOverlayPatterns = @($ReservoirReuseOverlayPatterns)
            restirExecutionMarkerPatterns = @($RestirExecutionMarkerPatterns)
            reservoirCountPatterns = @($ReservoirCountPatterns)
            candidateCountPatterns = @($CandidateCountPatterns)
            inputCandidateCountPatterns = @($InputCandidateCountPatterns)
            selectedCountPatterns = @($SelectedCountPatterns)
            candidateReductionRatioPatterns = @($CandidateReductionRatioPatterns)
            temporalReuseCountPatterns = @($TemporalReuseCountPatterns)
            spatialReuseCountPatterns = @($SpatialReuseCountPatterns)
            pathReuseCountPatterns = @($PathReuseCountPatterns)
            invalidationCountPatterns = @($InvalidationCountPatterns)
            confidenceStatsPatterns = @($ConfidenceStatsPatterns)
            outputEnergyPatterns = @($OutputEnergyPatterns)
            outputChecksumPatterns = @($OutputChecksumPatterns)
        }
    }
}

$directResolved = Resolve-ExistingFile $DirectReservoirDebugImagePath "Direct reservoir debug image"
$giResolved = Resolve-ExistingFile $GiReservoirDebugImagePath "GI reservoir debug image"
$reuseResolved = Resolve-ExistingFile $ReservoirReuseDebugImagePath "Reservoir reuse debug image"
$directBruteBaselineResolved = Resolve-OptionalFile $DirectBruteBaselineImagePath "Direct brute baseline image"
$restirDirectResolved = Resolve-OptionalFile $RestirDirectEnabledImagePath "ReSTIR direct enabled image"
$restirStableResolved = Resolve-OptionalFile $RestirTemporalStableImagePath "ReSTIR temporal stable image"
$restirStableRepeatResolved = Resolve-OptionalFiles $RestirTemporalStableRepeatImagePath "ReSTIR temporal stable repeat image"
$restirMovedResolved = Resolve-OptionalFile $RestirTemporalMovedImagePath "ReSTIR temporal moved image"
$restirExecutionDebugResolved = Resolve-OptionalFile $RestirExecutionDebugImagePath "ReSTIR execution debug image"
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$directDimensions = Get-ImageDimensions $directResolved
$giDimensions = Get-ImageDimensions $giResolved
$reuseDimensions = Get-ImageDimensions $reuseResolved
$directBruteBaselineDimensions = if (-not [string]::IsNullOrWhiteSpace($directBruteBaselineResolved)) { Get-ImageDimensions $directBruteBaselineResolved } else { $null }
$restirDirectDimensions = if (-not [string]::IsNullOrWhiteSpace($restirDirectResolved)) { Get-ImageDimensions $restirDirectResolved } else { $null }
$restirStableDimensions = if (-not [string]::IsNullOrWhiteSpace($restirStableResolved)) { Get-ImageDimensions $restirStableResolved } else { $null }
$restirMovedDimensions = if (-not [string]::IsNullOrWhiteSpace($restirMovedResolved)) { Get-ImageDimensions $restirMovedResolved } else { $null }
$restirExecutionDebugDimensions = if (-not [string]::IsNullOrWhiteSpace($restirExecutionDebugResolved)) { Get-ImageDimensions $restirExecutionDebugResolved } else { $null }

$giDelta = Invoke-DeltaHelper $directResolved $giResolved "gi-reservoir-debug"
$reuseDelta = Invoke-DeltaHelper $directResolved $reuseResolved "reservoir-reuse-debug"
$restirDirectDelta = if (-not [string]::IsNullOrWhiteSpace($directBruteBaselineResolved) -and -not [string]::IsNullOrWhiteSpace($restirDirectResolved)) {
    Invoke-DeltaHelper $directBruteBaselineResolved $restirDirectResolved "restir-direct-enabled"
} else { $null }
$restirMovedDelta = if (-not [string]::IsNullOrWhiteSpace($restirStableResolved) -and -not [string]::IsNullOrWhiteSpace($restirMovedResolved)) {
    Invoke-DeltaHelper $restirStableResolved $restirMovedResolved "restir-temporal-moved"
} else { $null }
$restirStableRepeatDeltas = New-Object System.Collections.Generic.List[object]
if (-not [string]::IsNullOrWhiteSpace($restirStableResolved)) {
    $repeatIndex = 1
    foreach ($repeatPath in @($restirStableRepeatResolved)) {
        $restirStableRepeatDeltas.Add((Invoke-DeltaHelper $restirStableResolved $repeatPath ("restir-temporal-stable-repeat-{0:D2}" -f $repeatIndex))) | Out-Null
        $repeatIndex++
    }
}

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
foreach ($entry in @(
    @{ label = "Direct brute baseline"; dimensions = $directBruteBaselineDimensions },
    @{ label = "ReSTIR direct enabled"; dimensions = $restirDirectDimensions },
    @{ label = "ReSTIR temporal stable"; dimensions = $restirStableDimensions },
    @{ label = "ReSTIR temporal moved"; dimensions = $restirMovedDimensions },
    @{ label = "ReSTIR execution debug"; dimensions = $restirExecutionDebugDimensions }
)) {
    if ($null -ne $entry.dimensions -and (($entry.dimensions.width -ne $directDimensions.width) -or ($entry.dimensions.height -ne $directDimensions.height))) {
        $failures.Add("$($entry.label) image dimensions differ from direct reservoir debug overlay. direct=$($directDimensions.width)x$($directDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ([double]$giDelta.focusRegionMetrics.changedPixelPercent -lt $MinGiReservoirChangedPixelPercent) {
    $failures.Add("GI reservoir debug overlay changed pixels below threshold. actual=$($giDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinGiReservoirChangedPixelPercent")
}
if ([double]$reuseDelta.focusRegionMetrics.changedPixelPercent -lt $MinReservoirReuseChangedPixelPercent) {
    $failures.Add("Reservoir reuse debug overlay changed pixels below threshold. actual=$($reuseDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinReservoirReuseChangedPixelPercent")
}
if ($RequireSameSceneDelta) {
    if ($null -eq $restirDirectDelta) {
        $failures.Add("Same-scene ReSTIR direct delta was required but -DirectBruteBaselineImagePath and -RestirDirectEnabledImagePath were not both provided.")
    } elseif ([double]$restirDirectDelta.focusRegionMetrics.changedPixelPercent -lt $MinRestirDirectChangedPixelPercent) {
        $failures.Add("ReSTIR direct enabled same-scene changed pixels below threshold. actual=$($restirDirectDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinRestirDirectChangedPixelPercent")
    }
}
if ($RequireStabilityComparison) {
    if ($restirStableRepeatResolved.Count -eq 0) {
        $failures.Add("Stability/flicker comparison was required but no -RestirTemporalStableRepeatImagePath was provided.")
    }
    foreach ($stableDelta in @($restirStableRepeatDeltas)) {
        if ([double]$stableDelta.focusRegionMetrics.changedPixelPercent -gt $MaxRestirStableChangedPixelPercent) {
            $failures.Add("ReSTIR temporal stable repeat changed pixels above flicker threshold. actual=$($stableDelta.focusRegionMetrics.changedPixelPercent) expected<=$MaxRestirStableChangedPixelPercent")
        }
    }
    if ($null -eq $restirMovedDelta) {
        $failures.Add("Temporal moved comparison was required but -RestirTemporalStableImagePath and -RestirTemporalMovedImagePath were not both provided.")
    } elseif ([double]$restirMovedDelta.focusRegionMetrics.changedPixelPercent -lt $MinRestirMovedChangedPixelPercent) {
        $failures.Add("ReSTIR temporal moved changed pixels below threshold. actual=$($restirMovedDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinRestirMovedChangedPixelPercent")
    }
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if (($RequireRestirExecutionProof -or $RequireCandidateReduction -or $RequireSelectedCount -or $RequireTemporalReuse -or $RequireSpatialReuse -or $RequireOutputEnergy -or $RequireOutputChecksum) -and $logResolved.Count -eq 0) {
    $failures.Add("Round 11 execution proof switches require at least one -LogPath.")
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
    if ($RequireRestirExecutionProof -and -not $logProof.markers.restirExecutionMarkerPresent) {
        $failures.Add("Missing real Round 11 ReSTIR DI execution marker.")
    }
    if ($RequireSelectedCount -and (-not $logProof.markers.selectedCountPresent -or $null -eq $logProof.counts.maxSelectedCount -or [double]$logProof.counts.maxSelectedCount -le 0)) {
        $failures.Add("Missing nonzero Round 11 selected candidate/reservoir count marker.")
    }
    if ($RequireCandidateReduction) {
        if ($null -eq $logProof.counts.candidateReductionRatio) {
            $failures.Add("Missing Round 11 candidate reduction ratio or enough input/selected counts to derive it.")
        } elseif ([double]$logProof.counts.candidateReductionRatio -lt $MinCandidateReductionRatio) {
            $failures.Add("Round 11 candidate reduction ratio below threshold. actual=$($logProof.counts.candidateReductionRatio) expected>=$MinCandidateReductionRatio")
        }
    }
    if (-not $logProof.markers.temporalReuseCountPresent) {
        $failures.Add("Missing Round 11 temporal reuse count marker.")
    }
    if ($RequireTemporalReuse -and ($null -eq $logProof.counts.maxTemporalReuseCount -or [double]$logProof.counts.maxTemporalReuseCount -le 0)) {
        $failures.Add("Round 11 temporal reuse was required but no nonzero temporal reuse count was found.")
    }
    if (-not $logProof.markers.spatialReuseCountPresent) {
        $failures.Add("Missing Round 11 spatial reuse count marker.")
    }
    if ($RequireSpatialReuse -and ($null -eq $logProof.counts.maxSpatialReuseCount -or [double]$logProof.counts.maxSpatialReuseCount -le 0)) {
        $failures.Add("Round 11 spatial reuse was required but no nonzero spatial reuse count was found.")
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
    if ($RequireOutputEnergy -and (-not $logProof.markers.outputEnergyPresent -or $null -eq $logProof.counts.maxOutputEnergy -or [double]$logProof.counts.maxOutputEnergy -le 0)) {
        $failures.Add("Round 11 output energy was required but no nonzero output energy marker was found.")
    }
    if ($RequireOutputChecksum -and (-not $logProof.markers.outputChecksumPresent -or $null -eq $logProof.counts.maxOutputChecksum -or [double]$logProof.counts.maxOutputChecksum -le 0)) {
        $failures.Add("Round 11 output checksum was required but no nonzero output checksum marker was found.")
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

$focusRegionSelection = "auto"
if ($DisableAutoFocusRegion) {
    $focusRegionSelection = "fixed"
}

$directBruteBaselineArtifact = $null
if (-not [string]::IsNullOrWhiteSpace($directBruteBaselineResolved)) {
    $directBruteBaselineArtifact = [ordered]@{
        path = $directBruteBaselineResolved
        dimensions = $directBruteBaselineDimensions
        role = "direct-brute-baseline"
    }
}

$restirDirectEnabledArtifact = $null
if (-not [string]::IsNullOrWhiteSpace($restirDirectResolved)) {
    $restirDirectEnabledArtifact = [ordered]@{
        path = $restirDirectResolved
        dimensions = $restirDirectDimensions
        role = "restir-direct-enabled"
    }
}

$restirTemporalStableArtifact = $null
if (-not [string]::IsNullOrWhiteSpace($restirStableResolved)) {
    $restirTemporalStableArtifact = [ordered]@{
        path = $restirStableResolved
        dimensions = $restirStableDimensions
        role = "restir-temporal-stable"
    }
}

$restirTemporalMovedArtifact = $null
if (-not [string]::IsNullOrWhiteSpace($restirMovedResolved)) {
    $restirTemporalMovedArtifact = [ordered]@{
        path = $restirMovedResolved
        dimensions = $restirMovedDimensions
        role = "restir-temporal-moved"
    }
}

$restirExecutionDebugArtifact = $null
if (-not [string]::IsNullOrWhiteSpace($restirExecutionDebugResolved)) {
    $restirExecutionDebugArtifact = [ordered]@{
        path = $restirExecutionDebugResolved
        dimensions = $restirExecutionDebugDimensions
        role = "restir-execution-debug"
    }
}

$restirTemporalStableRepeatArtifacts = New-Object System.Collections.Generic.List[object]
for ($index = 0; $index -lt $restirStableRepeatResolved.Count; $index++) {
    $restirTemporalStableRepeatArtifacts.Add([ordered]@{
        path = $restirStableRepeatResolved[$index]
        role = "restir-temporal-stable-repeat"
        index = $index
    })
}

$stableRepeatChangedPixelPercents = New-Object System.Collections.Generic.List[object]
for ($index = 0; $index -lt $restirStableRepeatDeltas.Count; $index++) {
    $stableDelta = $restirStableRepeatDeltas[$index]
    $stableRepeatChangedPixelPercents.Add($stableDelta.focusRegionMetrics.changedPixelPercent)
}

$directBruteBaselineToRestirDirectFocusRegion = $null
$directSameSceneChangedPixelPercent = $null
if ($restirDirectDelta) {
    $directBruteBaselineToRestirDirectFocusRegion = $restirDirectDelta.focusRegion
    $directSameSceneChangedPixelPercent = $restirDirectDelta.focusRegionMetrics.changedPixelPercent
}

$restirTemporalStableToMovedFocusRegion = $null
$temporalMovedChangedPixelPercent = $null
if ($restirMovedDelta) {
    $restirTemporalStableToMovedFocusRegion = $restirMovedDelta.focusRegion
    $temporalMovedChangedPixelPercent = $restirMovedDelta.focusRegionMetrics.changedPixelPercent
}

$strictExecutionProof = [bool]($RequireRestirExecutionProof -or $RequireSameSceneDelta -or $RequireStabilityComparison)
if ($failures.Count -eq 0) {
    if ($strictExecutionProof) {
        $proofClassification = "round11_restir_execution_stability_evidence_passed"
    } else {
        $proofClassification = "round11_restir_reservoir_overlay_evidence_passed"
    }
} else {
    if ($strictExecutionProof) {
        $proofClassification = "round11_restir_execution_stability_evidence_failed"
    } else {
        $proofClassification = "round11_restir_reservoir_overlay_evidence_failed"
    }
}

if ($strictExecutionProof) {
    $proofBoundaryClassification = "round11_restir_execution_and_stability_evidence_not_physical_quality_claim"
} else {
    $proofBoundaryClassification = "round11_reservoir_debug_overlay_and_telemetry_scaffold_not_physical_restir_quality_claim"
}

$round11MarkerPresent = $null
$reservoirCountPresent = $null
$maxReservoirCount = $null
$candidateCountPresent = $null
$maxCandidateCount = $null
$selectedCountPresent = $null
$maxSelectedCount = $null
$candidateReductionRatio = $null
$temporalReuseCountPresent = $null
$spatialReuseCountPresent = $null
$pathReuseCountPresent = $null
$invalidationCountPresent = $null
$confidenceStatsPresent = $null
$restirExecutionMarkerPresent = $null
$outputEnergyPresent = $null
$maxOutputEnergy = $null
$outputChecksumPresent = $null
$maxOutputChecksum = $null
$directReservoirOverlayPresent = $null
$giReservoirOverlayPresent = $null
$reservoirReuseOverlayPresent = $null
$invalidReservoirValuesPresent = $null
$temporaryDirectLightSourcePresent = $null
$proofMarkerPresent = $null
$nativeErrorPresent = $null
if ($logProof) {
    $round11MarkerPresent = [bool]$logProof.markers.round11MarkerPresent
    $reservoirCountPresent = [bool]$logProof.markers.reservoirCountPresent
    $maxReservoirCount = $logProof.counts.maxReservoirCount
    $candidateCountPresent = [bool]$logProof.markers.candidateCountPresent
    $maxCandidateCount = $logProof.counts.maxCandidateCount
    $selectedCountPresent = [bool]$logProof.markers.selectedCountPresent
    $maxSelectedCount = $logProof.counts.maxSelectedCount
    $candidateReductionRatio = $logProof.counts.candidateReductionRatio
    $temporalReuseCountPresent = [bool]$logProof.markers.temporalReuseCountPresent
    $spatialReuseCountPresent = [bool]$logProof.markers.spatialReuseCountPresent
    $pathReuseCountPresent = [bool]$logProof.markers.pathReuseCountPresent
    $invalidationCountPresent = [bool]$logProof.markers.invalidationCountPresent
    $confidenceStatsPresent = [bool]$logProof.markers.confidenceStatsPresent
    $restirExecutionMarkerPresent = [bool]$logProof.markers.restirExecutionMarkerPresent
    $outputEnergyPresent = [bool]$logProof.markers.outputEnergyPresent
    $maxOutputEnergy = $logProof.counts.maxOutputEnergy
    $outputChecksumPresent = [bool]$logProof.markers.outputChecksumPresent
    $maxOutputChecksum = $logProof.counts.maxOutputChecksum
    $directReservoirOverlayPresent = [bool]$logProof.markers.directReservoirOverlayPresent
    $giReservoirOverlayPresent = [bool]$logProof.markers.giReservoirOverlayPresent
    $reservoirReuseOverlayPresent = [bool]$logProof.markers.reservoirReuseOverlayPresent
    $invalidReservoirValuesPresent = [bool]$logProof.markers.invalidReservoirValuesPresent
    $temporaryDirectLightSourcePresent = [bool]$logProof.markers.temporaryDirectLightSourcePresent
    $proofMarkerPresent = [bool]$logProof.markers.proofMarkerPresent
    $nativeErrorPresent = [bool]$logProof.markers.nativeErrorPresent
}

$restirTemporalStableRepeatArtifactArray = @()
for ($index = 0; $index -lt $restirTemporalStableRepeatArtifacts.Count; $index++) {
    $restirTemporalStableRepeatArtifactArray += $restirTemporalStableRepeatArtifacts[$index]
}

$logResolvedArray = @()
foreach ($path in @($logResolved)) {
    $logResolvedArray += $path
}

$restirStableRepeatDeltaArray = @()
for ($index = 0; $index -lt $restirStableRepeatDeltas.Count; $index++) {
    $restirStableRepeatDeltaArray += $restirStableRepeatDeltas[$index]
}

$stableRepeatChangedPixelPercentArray = @()
for ($index = 0; $index -lt $stableRepeatChangedPixelPercents.Count; $index++) {
    $stableRepeatChangedPixelPercentArray += $stableRepeatChangedPixelPercents[$index]
}

$failuresArray = @()
for ($index = 0; $index -lt $failures.Count; $index++) {
    $failuresArray += $failures[$index]
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
        directBruteBaseline = $directBruteBaselineArtifact
        restirDirectEnabled = $restirDirectEnabledArtifact
        restirTemporalStable = $restirTemporalStableArtifact
        restirTemporalStableRepeats = $restirTemporalStableRepeatArtifactArray
        restirTemporalMoved = $restirTemporalMovedArtifact
        restirExecutionDebug = $restirExecutionDebugArtifact
    }
    logPaths = $logResolvedArray
    thresholds = [ordered]@{
        minGiReservoirChangedPixelPercent = $MinGiReservoirChangedPixelPercent
        minReservoirReuseChangedPixelPercent = $MinReservoirReuseChangedPixelPercent
        minRestirDirectChangedPixelPercent = $MinRestirDirectChangedPixelPercent
        minRestirMovedChangedPixelPercent = $MinRestirMovedChangedPixelPercent
        maxRestirStableChangedPixelPercent = $MaxRestirStableChangedPixelPercent
        minCandidateReductionRatio = $MinCandidateReductionRatio
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        focusRegionSelection = $focusRegionSelection
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
        requireRestirExecutionProof = [bool]$RequireRestirExecutionProof
        requireCandidateReduction = [bool]$RequireCandidateReduction
        requireSelectedCount = [bool]$RequireSelectedCount
        requireTemporalReuse = [bool]$RequireTemporalReuse
        requireSpatialReuse = [bool]$RequireSpatialReuse
        requireOutputEnergy = [bool]$RequireOutputEnergy
        requireOutputChecksum = [bool]$RequireOutputChecksum
        requireSameSceneDelta = [bool]$RequireSameSceneDelta
        requireStabilityComparison = [bool]$RequireStabilityComparison
    }
    imageDelta = [ordered]@{
        directToGiReservoir = $giDelta
        directToReservoirReuse = $reuseDelta
        directBruteBaselineToRestirDirect = $restirDirectDelta
        restirTemporalStableToMoved = $restirMovedDelta
        restirTemporalStableRepeats = $restirStableRepeatDeltaArray
    }
    selectedFocusRegions = [ordered]@{
        directToGiReservoir = $giDelta.focusRegion
        directToReservoirReuse = $reuseDelta.focusRegion
        directBruteBaselineToRestirDirect = $directBruteBaselineToRestirDirectFocusRegion
        restirTemporalStableToMoved = $restirTemporalStableToMovedFocusRegion
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = $proofClassification
        tracks = [ordered]@{
            reservoirInputs = [ordered]@{
                round11MarkerPresent = $round11MarkerPresent
                reservoirCountPresent = $reservoirCountPresent
                maxReservoirCount = $maxReservoirCount
                candidateCountPresent = $candidateCountPresent
                maxCandidateCount = $maxCandidateCount
                selectedCountPresent = $selectedCountPresent
                maxSelectedCount = $maxSelectedCount
                candidateReductionRatio = $candidateReductionRatio
            }
            reuse = [ordered]@{
                temporalReuseCountPresent = $temporalReuseCountPresent
                spatialReuseCountPresent = $spatialReuseCountPresent
                pathReuseCountPresent = $pathReuseCountPresent
                invalidationCountPresent = $invalidationCountPresent
                confidenceStatsPresent = $confidenceStatsPresent
            }
            execution = [ordered]@{
                restirExecutionMarkerPresent = $restirExecutionMarkerPresent
                outputEnergyPresent = $outputEnergyPresent
                maxOutputEnergy = $maxOutputEnergy
                outputChecksumPresent = $outputChecksumPresent
                maxOutputChecksum = $maxOutputChecksum
            }
            stability = [ordered]@{
                directSameSceneDeltaPresent = $null -ne $restirDirectDelta
                directSameSceneChangedPixelPercent = $directSameSceneChangedPixelPercent
                temporalMovedDeltaPresent = $null -ne $restirMovedDelta
                temporalMovedChangedPixelPercent = $temporalMovedChangedPixelPercent
                stableRepeatDeltaCount = $restirStableRepeatDeltas.Count
                stableRepeatChangedPixelPercents = $stableRepeatChangedPixelPercentArray
            }
            overlays = [ordered]@{
                directReservoirOverlayPresent = $directReservoirOverlayPresent
                giReservoirOverlayPresent = $giReservoirOverlayPresent
                reservoirReuseOverlayPresent = $reservoirReuseOverlayPresent
            }
            rejectionMarkers = [ordered]@{
                invalidReservoirValuesPresent = $invalidReservoirValuesPresent
                temporaryDirectLightSourcePresent = $temporaryDirectLightSourcePresent
                proofMarkerPresent = $proofMarkerPresent
                nativeErrorPresent = $nativeErrorPresent
            }
            proofBoundary = [ordered]@{
                classification = $proofBoundaryClassification
            }
        }
    }
    passed = $failures.Count -eq 0
    failures = $failuresArray
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
if ($result.overlayArtifacts.directBruteBaseline) {
    Write-Host "directBruteBaseline=$($result.overlayArtifacts.directBruteBaseline.path)"
}
if ($result.overlayArtifacts.restirDirectEnabled) {
    Write-Host "restirDirectEnabled=$($result.overlayArtifacts.restirDirectEnabled.path)"
}
if ($result.overlayArtifacts.restirTemporalStable) {
    Write-Host "restirTemporalStable=$($result.overlayArtifacts.restirTemporalStable.path)"
}
if ($result.overlayArtifacts.restirTemporalStableRepeats.Count -gt 0) {
    Write-Host "restirTemporalStableRepeats=$((@($result.overlayArtifacts.restirTemporalStableRepeats) | ForEach-Object { $_.path }) -join ';')"
}
if ($result.overlayArtifacts.restirTemporalMoved) {
    Write-Host "restirTemporalMoved=$($result.overlayArtifacts.restirTemporalMoved.path)"
}
if ($result.overlayArtifacts.restirExecutionDebug) {
    Write-Host "restirExecutionDebug=$($result.overlayArtifacts.restirExecutionDebug.path)"
}
Write-Host "logPaths=$($result.logPaths -join ';')"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "giReservoir.focus.changedPixelPercent=$($giDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "reservoirReuse.focus.changedPixelPercent=$($reuseDelta.focusRegionMetrics.changedPixelPercent)"
if ($restirDirectDelta) {
    Write-Host "restirDirect.focus.changedPixelPercent=$($restirDirectDelta.focusRegionMetrics.changedPixelPercent)"
}
if ($restirMovedDelta) {
    Write-Host "restirTemporalMoved.focus.changedPixelPercent=$($restirMovedDelta.focusRegionMetrics.changedPixelPercent)"
}
if ($restirStableRepeatDeltas.Count -gt 0) {
    Write-Host "restirTemporalStable.repeat.changedPixelPercents=$((@($restirStableRepeatDeltas) | ForEach-Object { $_.focusRegionMetrics.changedPixelPercent }) -join ';')"
}
if ($logProof) {
    Write-Host "round11MarkerPresent=$($logProof.markers.round11MarkerPresent)"
    Write-Host "directReservoirOverlayPresent=$($logProof.markers.directReservoirOverlayPresent)"
    Write-Host "giReservoirOverlayPresent=$($logProof.markers.giReservoirOverlayPresent)"
    Write-Host "reservoirReuseOverlayPresent=$($logProof.markers.reservoirReuseOverlayPresent)"
    Write-Host "restirExecutionMarkerPresent=$($logProof.markers.restirExecutionMarkerPresent)"
    Write-Host "reservoirCountPresent=$($logProof.markers.reservoirCountPresent)"
    Write-Host "maxReservoirCount=$($logProof.counts.maxReservoirCount)"
    Write-Host "candidateCountPresent=$($logProof.markers.candidateCountPresent)"
    Write-Host "maxCandidateCount=$($logProof.counts.maxCandidateCount)"
    Write-Host "selectedCountPresent=$($logProof.markers.selectedCountPresent)"
    Write-Host "maxSelectedCount=$($logProof.counts.maxSelectedCount)"
    Write-Host "candidateReductionRatio=$($logProof.counts.candidateReductionRatio)"
    Write-Host "temporalReuseCountPresent=$($logProof.markers.temporalReuseCountPresent)"
    Write-Host "maxTemporalReuseCount=$($logProof.counts.maxTemporalReuseCount)"
    Write-Host "spatialReuseCountPresent=$($logProof.markers.spatialReuseCountPresent)"
    Write-Host "maxSpatialReuseCount=$($logProof.counts.maxSpatialReuseCount)"
    Write-Host "pathReuseCountPresent=$($logProof.markers.pathReuseCountPresent)"
    Write-Host "invalidationCountPresent=$($logProof.markers.invalidationCountPresent)"
    Write-Host "confidenceStatsPresent=$($logProof.markers.confidenceStatsPresent)"
    Write-Host "outputEnergyPresent=$($logProof.markers.outputEnergyPresent)"
    Write-Host "maxOutputEnergy=$($logProof.counts.maxOutputEnergy)"
    Write-Host "outputChecksumPresent=$($logProof.markers.outputChecksumPresent)"
    Write-Host "maxOutputChecksum=$($logProof.counts.maxOutputChecksum)"
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
