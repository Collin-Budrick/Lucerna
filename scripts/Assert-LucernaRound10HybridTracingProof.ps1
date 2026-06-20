<#
.SYNOPSIS
Controller-only Round 10 assertion helper for voxel traversal and hybrid tracing overlay evidence.

.DESCRIPTION
This script checks already captured screenshots and optional controller launch logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller has captured voxel ray debug, RT entity debug, and hybrid hit
debug overlay artifacts.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $VoxelRayDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $RtEntityDebugImagePath,

    [Parameter(Mandatory = $true)]
    [string] $HybridHitDebugImagePath,

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

    [double] $MinRtEntityChangedPixelPercent = 0.25,

    [double] $MinHybridHitChangedPixelPercent = 0.25,

    [string[]] $Round10MarkerPatterns = @(
        "Lucerna Round 10",
        "round10\.",
        "Hybrid voxel traversal",
        "Vulkan RT"
    ),

    [string[]] $VoxelRayDebugOverlayPatterns = @(
        "voxelRayDebug(?:Visible|Submitted|Enabled)?=true",
        "round10\.voxelRayDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=voxel-ray-debug",
        "round10ArtifactRole=voxel-ray-debug"
    ),

    [string[]] $RtEntityDebugOverlayPatterns = @(
        "rtEntityDebug(?:Visible|Submitted|Enabled)?=true",
        "round10\.rtEntityDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=rt-entity-debug",
        "round10ArtifactRole=rt-entity-debug"
    ),

    [string[]] $HybridHitDebugOverlayPatterns = @(
        "hybridHitDebug(?:Visible|Submitted|Enabled)?=true",
        "round10\.hybridHitDebug(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=hybrid-hit-debug",
        "round10ArtifactRole=hybrid-hit-debug"
    ),

    [string[]] $RayCountPatterns = @(
        "voxelRay(?:Count|s)?=([1-9][0-9]*)",
        "voxel_ray_count=([1-9][0-9]*)",
        "round10\.voxelRays=([1-9][0-9]*)",
        "round10\.rayCount=([1-9][0-9]*)"
    ),

    [string[]] $HitCountPatterns = @(
        "voxelHit(?:Count|s)?=([0-9]+)",
        "voxel_hit_count=([0-9]+)",
        "round10\.voxelHits=([0-9]+)",
        "hit(?:Count|s)?=([0-9]+)"
    ),

    [string[]] $MissCountPatterns = @(
        "voxelMiss(?:Count|s)?=([0-9]+)",
        "voxel_miss_count=([0-9]+)",
        "round10\.voxelMisses=([0-9]+)",
        "miss(?:Count|s)?=([0-9]+)"
    ),

    [string[]] $TraversalStepPatterns = @(
        "averageTraversalSteps=([1-9][0-9]*(?:\.[0-9]+)?)",
        "avg_traversal_steps=([1-9][0-9]*(?:\.[0-9]+)?)",
        "traversal(?:Step|Steps|StepCount)=([1-9][0-9]*(?:\.[0-9]+)?)",
        "round10\.traversalSteps=([1-9][0-9]*(?:\.[0-9]+)?)"
    ),

    [string[]] $SkippedSectionPatterns = @(
        "skippedSection(?:Count|s)?=([0-9]+)",
        "skipped_sections=([0-9]+)",
        "round10\.skippedSections=([0-9]+)"
    ),

    [string[]] $BlasStatusPatterns = @(
        "BLAS(?:Status|Ready|Builds|BuildCount)?=",
        "blas(?:Status|Ready|Builds|BuildCount)?=",
        "round10\.blas"
    ),

    [string[]] $TlasStatusPatterns = @(
        "TLAS(?:Status|Ready|Builds|BuildCount)?=",
        "tlas(?:Status|Ready|Builds|BuildCount)?=",
        "round10\.tlas"
    ),

    [string[]] $FallbackStatusPatterns = @(
        "rtFallback(?:Status|Active)?=",
        "fallbackStatus=",
        "nonRtFallback=",
        "hardwareRtAvailable=",
        "round10\.rtFallback"
    ),

    [string[]] $HybridVoxelHitPatterns = @(
        "voxelHybridHit(?:Count|s)?=([0-9]+)",
        "hybridVoxelHits=([0-9]+)",
        "hybrid_source_voxel=([0-9]+)",
        "round10\.hybrid\.voxelHits=([0-9]+)"
    ),

    [string[]] $HybridRtHitPatterns = @(
        "rtHybridHit(?:Count|s)?=([0-9]+)",
        "hybridRtHits=([0-9]+)",
        "hybrid_source_rt=([0-9]+)",
        "round10\.hybrid\.rtHits=([0-9]+)"
    ),

    [string[]] $HybridScreenSpaceHitPatterns = @(
        "screenSpaceHybridHit(?:Count|s)?=([0-9]+)",
        "hybridScreenSpaceHits=([0-9]+)",
        "hybrid_source_screen(?:Space)?=([0-9]+)",
        "round10\.hybrid\.screenSpaceHits=([0-9]+)"
    ),

    [string[]] $BoundaryLabelPatterns = @(
        "round10\.boundary(?:Label)?=",
        "boundaryLabel=",
        "metadataOnly(?:Tracing|Rt|Traversal)?=",
        "hardwareRtExecution(?:Proven|Ready)?=",
        "tracingBoundary="
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round10-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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

function Measure-Round10LogProof {
    param([string[]] $ResolvedLogPaths)

    $combined = New-Object System.Text.StringBuilder
    foreach ($path in $ResolvedLogPaths) {
        [void]$combined.AppendLine("### LOG: $path")
        [void]$combined.AppendLine((Get-Content -Raw -LiteralPath $path))
    }
    $log = $combined.ToString()

    $rayCounts = Get-CapturedNumbers $log $RayCountPatterns
    $hitCounts = Get-CapturedNumbers $log $HitCountPatterns
    $missCounts = Get-CapturedNumbers $log $MissCountPatterns
    $traversalSteps = Get-CapturedNumbers $log $TraversalStepPatterns
    $skippedSections = Get-CapturedNumbers $log $SkippedSectionPatterns
    $hybridVoxelHits = Get-CapturedNumbers $log $HybridVoxelHitPatterns
    $hybridRtHits = Get-CapturedNumbers $log $HybridRtHitPatterns
    $hybridScreenSpaceHits = Get-CapturedNumbers $log $HybridScreenSpaceHitPatterns

    return [ordered]@{
        logPaths = @($ResolvedLogPaths)
        markers = [ordered]@{
            round10MarkerPresent = Test-AnyRegex $log $Round10MarkerPatterns
            voxelRayDebugOverlayPresent = Test-AnyRegex $log $VoxelRayDebugOverlayPatterns
            rtEntityDebugOverlayPresent = Test-AnyRegex $log $RtEntityDebugOverlayPatterns
            hybridHitDebugOverlayPresent = Test-AnyRegex $log $HybridHitDebugOverlayPatterns
            rayCountPresent = Test-AnyRegex $log $RayCountPatterns
            hitCountPresent = Test-AnyRegex $log $HitCountPatterns
            missCountPresent = Test-AnyRegex $log $MissCountPatterns
            traversalStepPresent = Test-AnyRegex $log $TraversalStepPatterns
            skippedSectionPresent = Test-AnyRegex $log $SkippedSectionPatterns
            blasStatusPresent = Test-AnyRegex $log $BlasStatusPatterns
            tlasStatusPresent = Test-AnyRegex $log $TlasStatusPatterns
            fallbackStatusPresent = Test-AnyRegex $log $FallbackStatusPatterns
            hybridVoxelHitPresent = Test-AnyRegex $log $HybridVoxelHitPatterns
            hybridRtHitPresent = Test-AnyRegex $log $HybridRtHitPatterns
            hybridScreenSpaceHitPresent = Test-AnyRegex $log $HybridScreenSpaceHitPatterns
            boundaryLabelPresent = Test-AnyRegex $log $BoundaryLabelPatterns
            invalidTracingValuesPresent = Test-Regex $log "invalid(?:VoxelRay|Traversal|HybridHit|RtEntity)(?:Count|s)?=true|negative (?:voxel ray|traversal|hybrid|BLAS|TLAS)|(?:voxelRay(?:Count|s)?|hybridHit(?:Count|s)?|traversal(?:Step|Steps|StepCount)).*(?:NaN|Infinity)"
            proofMarkerPresent = Test-Regex $log "round10\.(?:proofMarker|focusWindowOnly)=true|Round 10 .*proof marker|Round 10 .*focus-window-only"
            temporaryDirectLightSourcePresent = Test-Regex $log "round10\.temporaryDirectLightSource=true|Round 10 .*temporary direct-light|Round 10 .*current direct-light RGBA payload"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            rayCounts = @($rayCounts)
            hitCounts = @($hitCounts)
            missCounts = @($missCounts)
            traversalSteps = @($traversalSteps)
            skippedSections = @($skippedSections)
            hybridVoxelHits = @($hybridVoxelHits)
            hybridRtHits = @($hybridRtHits)
            hybridScreenSpaceHits = @($hybridScreenSpaceHits)
            maxRayCount = Get-MaxNumber $rayCounts
            maxHitCount = Get-MaxNumber $hitCounts
            maxMissCount = Get-MaxNumber $missCounts
            maxTraversalSteps = Get-MaxNumber $traversalSteps
            maxSkippedSections = Get-MaxNumber $skippedSections
            maxHybridVoxelHits = Get-MaxNumber $hybridVoxelHits
            maxHybridRtHits = Get-MaxNumber $hybridRtHits
            maxHybridScreenSpaceHits = Get-MaxNumber $hybridScreenSpaceHits
        }
        patterns = [ordered]@{
            round10MarkerPatterns = @($Round10MarkerPatterns)
            voxelRayDebugOverlayPatterns = @($VoxelRayDebugOverlayPatterns)
            rtEntityDebugOverlayPatterns = @($RtEntityDebugOverlayPatterns)
            hybridHitDebugOverlayPatterns = @($HybridHitDebugOverlayPatterns)
            rayCountPatterns = @($RayCountPatterns)
            hitCountPatterns = @($HitCountPatterns)
            missCountPatterns = @($MissCountPatterns)
            traversalStepPatterns = @($TraversalStepPatterns)
            skippedSectionPatterns = @($SkippedSectionPatterns)
            blasStatusPatterns = @($BlasStatusPatterns)
            tlasStatusPatterns = @($TlasStatusPatterns)
            fallbackStatusPatterns = @($FallbackStatusPatterns)
            hybridVoxelHitPatterns = @($HybridVoxelHitPatterns)
            hybridRtHitPatterns = @($HybridRtHitPatterns)
            hybridScreenSpaceHitPatterns = @($HybridScreenSpaceHitPatterns)
            boundaryLabelPatterns = @($BoundaryLabelPatterns)
        }
    }
}

$voxelResolved = Resolve-ExistingFile $VoxelRayDebugImagePath "Voxel ray debug image"
$rtResolved = Resolve-ExistingFile $RtEntityDebugImagePath "RT entity debug image"
$hybridResolved = Resolve-ExistingFile $HybridHitDebugImagePath "Hybrid hit debug image"
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$voxelDimensions = Get-ImageDimensions $voxelResolved
$rtDimensions = Get-ImageDimensions $rtResolved
$hybridDimensions = Get-ImageDimensions $hybridResolved

$rtDelta = Invoke-DeltaHelper $voxelResolved $rtResolved "rt-entity-debug"
$hybridDelta = Invoke-DeltaHelper $voxelResolved $hybridResolved "hybrid-hit-debug"

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round10LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
    @{ label = "RT entity debug overlay"; dimensions = $rtDimensions },
    @{ label = "hybrid hit debug overlay"; dimensions = $hybridDimensions }
)) {
    if (($entry.dimensions.width -ne $voxelDimensions.width) -or ($entry.dimensions.height -ne $voxelDimensions.height)) {
        $failures.Add("$($entry.label) image dimensions differ from voxel ray debug overlay. voxel=$($voxelDimensions.width)x$($voxelDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ([double]$rtDelta.focusRegionMetrics.changedPixelPercent -lt $MinRtEntityChangedPixelPercent) {
    $failures.Add("RT entity debug overlay changed pixels below threshold. actual=$($rtDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinRtEntityChangedPixelPercent")
}
if ([double]$hybridDelta.focusRegionMetrics.changedPixelPercent -lt $MinHybridHitChangedPixelPercent) {
    $failures.Add("Hybrid hit debug overlay changed pixels below threshold. actual=$($hybridDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinHybridHitChangedPixelPercent")
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.round10MarkerPresent) {
        $failures.Add("Missing Round 10 tracing log marker.")
    }
    if (-not $logProof.markers.voxelRayDebugOverlayPresent) {
        $failures.Add("Missing Round 10 voxel ray debug overlay marker.")
    }
    if (-not $logProof.markers.rtEntityDebugOverlayPresent) {
        $failures.Add("Missing Round 10 RT entity debug overlay marker.")
    }
    if (-not $logProof.markers.hybridHitDebugOverlayPresent) {
        $failures.Add("Missing Round 10 hybrid hit debug overlay marker.")
    }
    if (-not $logProof.markers.rayCountPresent -or $null -eq $logProof.counts.maxRayCount -or [double]$logProof.counts.maxRayCount -le 0) {
        $failures.Add("Missing nonzero Round 10 voxel ray count marker.")
    }
    if (-not $logProof.markers.hitCountPresent) {
        $failures.Add("Missing Round 10 voxel hit count marker.")
    }
    if (-not $logProof.markers.missCountPresent) {
        $failures.Add("Missing Round 10 voxel miss count marker.")
    }
    if (-not $logProof.markers.traversalStepPresent -or $null -eq $logProof.counts.maxTraversalSteps -or [double]$logProof.counts.maxTraversalSteps -le 0) {
        $failures.Add("Missing nonzero Round 10 traversal step marker.")
    }
    if (-not $logProof.markers.skippedSectionPresent) {
        $failures.Add("Missing Round 10 skipped section marker.")
    }
    if (-not $logProof.markers.blasStatusPresent) {
        $failures.Add("Missing Round 10 BLAS status marker.")
    }
    if (-not $logProof.markers.tlasStatusPresent) {
        $failures.Add("Missing Round 10 TLAS status marker.")
    }
    if (-not $logProof.markers.fallbackStatusPresent) {
        $failures.Add("Missing Round 10 RT fallback/capability status marker.")
    }
    if (-not $logProof.markers.hybridVoxelHitPresent) {
        $failures.Add("Missing Round 10 per-source hybrid voxel hit count marker.")
    }
    if (-not $logProof.markers.hybridRtHitPresent) {
        $failures.Add("Missing Round 10 per-source hybrid RT hit count marker.")
    }
    if (-not $logProof.markers.hybridScreenSpaceHitPresent) {
        $failures.Add("Missing Round 10 per-source hybrid screen-space hit count marker.")
    }
    if (($null -eq $logProof.counts.maxHybridVoxelHits -or [double]$logProof.counts.maxHybridVoxelHits -le 0) -and
            ($null -eq $logProof.counts.maxHybridRtHits -or [double]$logProof.counts.maxHybridRtHits -le 0) -and
            ($null -eq $logProof.counts.maxHybridScreenSpaceHits -or [double]$logProof.counts.maxHybridScreenSpaceHits -le 0)) {
        $failures.Add("Round 10 hybrid hit counts are present but all per-source counts are zero.")
    }
    if ($logProof.markers.invalidTracingValuesPresent) {
        $failures.Add("Log contains invalid Round 10 tracing value markers.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; Round 10 proof must use tracing overlay paths.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker or focus-window-only evidence; Round 10 proof must use requested overlay artifacts.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$result = [ordered]@{
    overlayArtifacts = [ordered]@{
        voxelRayDebug = [ordered]@{
            path = $voxelResolved
            dimensions = $voxelDimensions
            role = "voxel-ray-debug"
        }
        rtEntityDebug = [ordered]@{
            path = $rtResolved
            dimensions = $rtDimensions
            role = "rt-entity-debug"
        }
        hybridHitDebug = [ordered]@{
            path = $hybridResolved
            dimensions = $hybridDimensions
            role = "hybrid-hit-debug"
        }
    }
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minRtEntityChangedPixelPercent = $MinRtEntityChangedPixelPercent
        minHybridHitChangedPixelPercent = $MinHybridHitChangedPixelPercent
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
        voxelRayToRtEntity = $rtDelta
        voxelRayToHybridHit = $hybridDelta
    }
    selectedFocusRegions = [ordered]@{
        voxelRayToRtEntity = $rtDelta.focusRegion
        voxelRayToHybridHit = $hybridDelta.focusRegion
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round10_hybrid_tracing_overlay_evidence_passed" } else { "round10_hybrid_tracing_overlay_evidence_failed" }
        tracks = [ordered]@{
            voxelTraversal = [ordered]@{
                voxelRayDebugOverlayPresent = if ($logProof) { [bool]$logProof.markers.voxelRayDebugOverlayPresent } else { $null }
                rayCountPresent = if ($logProof) { [bool]$logProof.markers.rayCountPresent } else { $null }
                maxRayCount = if ($logProof) { $logProof.counts.maxRayCount } else { $null }
                hitCountPresent = if ($logProof) { [bool]$logProof.markers.hitCountPresent } else { $null }
                missCountPresent = if ($logProof) { [bool]$logProof.markers.missCountPresent } else { $null }
                traversalStepPresent = if ($logProof) { [bool]$logProof.markers.traversalStepPresent } else { $null }
                skippedSectionPresent = if ($logProof) { [bool]$logProof.markers.skippedSectionPresent } else { $null }
            }
            rtEntity = [ordered]@{
                rtEntityDebugOverlayPresent = if ($logProof) { [bool]$logProof.markers.rtEntityDebugOverlayPresent } else { $null }
                blasStatusPresent = if ($logProof) { [bool]$logProof.markers.blasStatusPresent } else { $null }
                tlasStatusPresent = if ($logProof) { [bool]$logProof.markers.tlasStatusPresent } else { $null }
                fallbackStatusPresent = if ($logProof) { [bool]$logProof.markers.fallbackStatusPresent } else { $null }
            }
            hybridHits = [ordered]@{
                hybridHitDebugOverlayPresent = if ($logProof) { [bool]$logProof.markers.hybridHitDebugOverlayPresent } else { $null }
                hybridVoxelHitPresent = if ($logProof) { [bool]$logProof.markers.hybridVoxelHitPresent } else { $null }
                hybridRtHitPresent = if ($logProof) { [bool]$logProof.markers.hybridRtHitPresent } else { $null }
                hybridScreenSpaceHitPresent = if ($logProof) { [bool]$logProof.markers.hybridScreenSpaceHitPresent } else { $null }
                maxHybridVoxelHits = if ($logProof) { $logProof.counts.maxHybridVoxelHits } else { $null }
                maxHybridRtHits = if ($logProof) { $logProof.counts.maxHybridRtHits } else { $null }
                maxHybridScreenSpaceHits = if ($logProof) { $logProof.counts.maxHybridScreenSpaceHits } else { $null }
            }
            proofBoundary = [ordered]@{
                boundaryLabelPresent = if ($logProof) { [bool]$logProof.markers.boundaryLabelPresent } else { $null }
                classification = "round10_overlay_and_telemetry_scaffold_not_physical_quality_claim"
            }
            rejectionMarkers = [ordered]@{
                invalidTracingValuesPresent = if ($logProof) { [bool]$logProof.markers.invalidTracingValuesPresent } else { $null }
                temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
                proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
                nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
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

Write-Host "voxelRayDebug=$($result.overlayArtifacts.voxelRayDebug.path)"
Write-Host "rtEntityDebug=$($result.overlayArtifacts.rtEntityDebug.path)"
Write-Host "hybridHitDebug=$($result.overlayArtifacts.hybridHitDebug.path)"
Write-Host "logPaths=$($result.logPaths -join ';')"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "rtEntity.focus.changedPixelPercent=$($rtDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "hybridHit.focus.changedPixelPercent=$($hybridDelta.focusRegionMetrics.changedPixelPercent)"
if ($logProof) {
    Write-Host "round10MarkerPresent=$($logProof.markers.round10MarkerPresent)"
    Write-Host "voxelRayDebugOverlayPresent=$($logProof.markers.voxelRayDebugOverlayPresent)"
    Write-Host "rtEntityDebugOverlayPresent=$($logProof.markers.rtEntityDebugOverlayPresent)"
    Write-Host "hybridHitDebugOverlayPresent=$($logProof.markers.hybridHitDebugOverlayPresent)"
    Write-Host "rayCountPresent=$($logProof.markers.rayCountPresent)"
    Write-Host "maxRayCount=$($logProof.counts.maxRayCount)"
    Write-Host "hitCountPresent=$($logProof.markers.hitCountPresent)"
    Write-Host "missCountPresent=$($logProof.markers.missCountPresent)"
    Write-Host "traversalStepPresent=$($logProof.markers.traversalStepPresent)"
    Write-Host "skippedSectionPresent=$($logProof.markers.skippedSectionPresent)"
    Write-Host "blasStatusPresent=$($logProof.markers.blasStatusPresent)"
    Write-Host "tlasStatusPresent=$($logProof.markers.tlasStatusPresent)"
    Write-Host "fallbackStatusPresent=$($logProof.markers.fallbackStatusPresent)"
    Write-Host "hybridVoxelHitPresent=$($logProof.markers.hybridVoxelHitPresent)"
    Write-Host "hybridRtHitPresent=$($logProof.markers.hybridRtHitPresent)"
    Write-Host "hybridScreenSpaceHitPresent=$($logProof.markers.hybridScreenSpaceHitPresent)"
    Write-Host "boundaryLabelPresent=$($logProof.markers.boundaryLabelPresent)"
    Write-Host "invalidTracingValuesPresent=$($logProof.markers.invalidTracingValuesPresent)"
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
    throw "Round 10 hybrid tracing proof failed: $($failures -join '; ')"
}
