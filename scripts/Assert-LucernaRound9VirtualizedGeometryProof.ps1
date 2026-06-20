<#
.SYNOPSIS
Controller-only Round 9 assertion helper for virtualized chunk geometry and culling evidence.

.DESCRIPTION
This script checks already captured screenshots and optional controller launch logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller has captured flat/open cluster overlay, interior/wall-facing
culling overlay, and high render-distance/open terrain culling overlay artifacts.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $FlatClusterOverlayImagePath,

    [Parameter(Mandatory = $true)]
    [string] $InteriorCullingOverlayImagePath,

    [Parameter(Mandatory = $true)]
    [string] $HighDistanceCullingOverlayImagePath,

    [string] $ForestComplexCullingOverlayImagePath = "",

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

    [double] $MinInteriorCullingChangedPixelPercent = 0.25,

    [double] $MinHighDistanceCullingChangedPixelPercent = 0.25,

    [double] $MinForestComplexChangedPixelPercent = 0.25,

    [string[]] $Round9MarkerPatterns = @(
        "Lucerna Round 9 virtualized chunk geometry",
        "round9\.virtualized(?:Chunk)?Geometry",
        "round9\.chunkClusters",
        "virtualized chunk geometry"
    ),

    [string[]] $ClusterCountPatterns = @(
        "cluster(?:Count|s)?=([1-9][0-9]*)",
        "clusters(?:Total)?=([1-9][0-9]*)",
        "cluster_count=([1-9][0-9]*)",
        "round9\.cluster(?:Count|s)=([1-9][0-9]*)",
        "meshlet(?:Count|s)?=([1-9][0-9]*)"
    ),

    [string[]] $VisibleClusterCountPatterns = @(
        "visibleCluster(?:Count|s)?=([0-9]+)",
        "visible_clusters=([0-9]+)",
        "visible_cluster_count=([0-9]+)",
        "round9\.visibleCluster(?:Count|s)=([0-9]+)"
    ),

    [string[]] $CulledClusterCountPatterns = @(
        "(?:culled|offscreen)Cluster(?:Count|s)?=([0-9]+)",
        "culled_clusters=([0-9]+)",
        "culled_cluster_count=([0-9]+)",
        "offscreen_clusters=([0-9]+)",
        "round9\.(?:culled|offscreen)Cluster(?:Count|s)=([0-9]+)"
    ),

    [string[]] $HiddenClusterCountPatterns = @(
        "hiddenCluster(?:Count|s)?=([0-9]+)",
        "hidden_clusters=([0-9]+)",
        "hidden_cluster_count=([0-9]+)",
        "round9\.hiddenCluster(?:Count|s)=([0-9]+)"
    ),

    [string[]] $UploadBytesPatterns = @(
        "upload(?:Bytes|_bytes)=([1-9][0-9]*)",
        "clusterUploadBytes=([1-9][0-9]*)",
        "upload_byte_estimate=([1-9][0-9]*)",
        "total_upload_byte_estimate=([1-9][0-9]*)",
        "round9\.uploadBytes=([1-9][0-9]*)",
        "meshletUploadBytes=([1-9][0-9]*)"
    ),

    [string[]] $GenerationCounterPatterns = @(
        "generation(?:Counter|s)?=([1-9][0-9]*)",
        "generation_counter=([1-9][0-9]*)",
        "clusterGeneration=([1-9][0-9]*)",
        "geometryGeneration=([1-9][0-9]*)",
        "round9\.generation(?:Counter|s)?=([1-9][0-9]*)"
    ),

    [string[]] $IndirectDrawPatterns = @(
        "indirectDraw(?:Count|s|Placeholder)?=([0-9]+)",
        "indirect_draw(?:_count)?=([0-9]+)",
        "indirect_draw_count_placeholder=([0-9]+)",
        "indirect_draw_candidate_count=([0-9]+)",
        "drawList(?:Count)?=([0-9]+)",
        "round9\.indirectDraw(?:Count|s)?=([0-9]+)"
    ),

    [string[]] $RealIndirectDrawPatterns = @(
        "indirectDraw(?:Count|s)=([1-9][0-9]*)",
        "indirect_draw_count=([1-9][0-9]*)",
        "realIndirectDraw(?:Count|s)=([1-9][0-9]*)",
        "real_indirect_draw_count=([1-9][0-9]*)",
        "drawList(?:Count)?=([1-9][0-9]*)",
        "round9\.indirectDraw(?:Count|s)=([1-9][0-9]*)"
    ),

    [string[]] $GpuCullingExecutedPatterns = @(
        "actualGpuCullingExecuted=(true|false)",
        "realGpuCullingExecuted=(true|false)",
        "gpu_culling_executed=(true|false)",
        "round9\.actualGpuCullingExecuted=(true|false)",
        "round9\.gpu_culling_executed=(true|false)"
    ),

    [string[]] $GpuCullingPrerequisitesReadyPatterns = @(
        "gpuCullingPrerequisitesReady=(true|false)",
        "gpuPrerequisitesReady=(true|false)",
        "gpu_prerequisites_ready=(true|false)",
        "gpu_culling_prerequisites_ready=(true|false)",
        "round9\.gpuCullingPrerequisitesReady=(true|false)",
        "round9\.gpu_culling_prerequisites_ready=(true|false)"
    ),

    [string[]] $GpuCullingBlockerReasonPatterns = @(
        "gpuCullingBlockerReason=([A-Za-z0-9_.:-]+)",
        "gpu_culling_blocker_reason=([A-Za-z0-9_.:-]+)",
        "round9\.gpuCullingBlockerReason=([A-Za-z0-9_.:-]+)"
    ),

    [string[]] $FrustumCandidatePatterns = @(
        "frustumCandidate(?:Count|s)?=([0-9]+)",
        "frustum_candidate_count=([0-9]+)",
        "frustum_culling_candidate_count=([0-9]+)",
        "frustum_candidates=([0-9]+)",
        "round9\.frustumCandidates=([0-9]+)",
        "round9\.frustum_candidate_count=([0-9]+)"
    ),

    [string[]] $OcclusionCandidatePatterns = @(
        "occlusion(?:Candidate|Placeholder)(?:Count|s)?=([0-9]+)",
        "occlusion_candidate_count=([0-9]+)",
        "occlusion_culling_candidate_count=([0-9]+)",
        "occlusion_culling_placeholder_count=([0-9]+)",
        "occlusion_placeholder_count=([0-9]+)",
        "occlusion_candidates=([0-9]+)",
        "round9\.occlusionCandidates=([0-9]+)",
        "round9\.occlusionPlaceholderCount=([0-9]+)",
        "round9\.occlusion_candidate_count=([0-9]+)",
        "round9\.occlusion_placeholder_count=([0-9]+)"
    ),

    [string[]] $IndirectDrawReadyPatterns = @(
        "indirectDrawReady=(true|false)",
        "indirect_draw_ready=(true|false)",
        "gpuIndirectDrawReady=(true|false)",
        "round9\.indirectDrawReady=(true|false)",
        "round9\.indirect_draw_ready=(true|false)"
    ),

    [string[]] $ForestComplexPatterns = @(
        "artifactRole=forest-complex-culling-overlay",
        "round9ArtifactRole=forest-complex-culling-overlay",
        "sceneKind=forest-complex-area",
        "round9SceneKind=forest-complex-area",
        "round9\.scene=forest-complex-area",
        "round9\.sceneKind=forest-complex-area",
        "clusterDensityBucket=(?:medium|high|complex|forest)",
        "complexityBucket=(?:medium|high|complex|forest)"
    ),

    [string[]] $FrameTimingPatterns = @(
        "cpuFrameTime(?:Ms)?=([0-9]+(?:\.[0-9]+)?)",
        "gpuFrameTime(?:Ms)?=([0-9]+(?:\.[0-9]+)?)",
        "cpuFrameTime(?:Ms)?Placeholder=([0-9]+(?:\.[0-9]+)?)",
        "gpuFrameTime(?:Ms)?Placeholder=([0-9]+(?:\.[0-9]+)?)",
        "cpu_frame_time_ms_placeholder=([0-9]+(?:\.[0-9]+)?)",
        "gpu_frame_time_ms_placeholder=([0-9]+(?:\.[0-9]+)?)",
        "frameTiming(?:Ready|Present|Marker)=true",
        "round9\.cpuFrameTimeMs=([0-9]+(?:\.[0-9]+)?)",
        "round9\.gpuFrameTimeMs=([0-9]+(?:\.[0-9]+)?)",
        "round9\.frameTiming(?:Ready|Present|Marker)=true"
    ),

    [string[]] $OverclaimPatterns = @(
        "actualGpuCullingExecuted=false[^`r`n]*(?:realGpuCulling(?:Proven|Ready)|gpuCullingOutputReady)=true",
        "gpu_culling_executed=false[^`r`n]*(?:real_gpu_culling_(?:proven|ready)|gpu_culling_output_ready)=true",
        "realGpuCulling(?:Proven|Ready)=true[^`r`n]*(?:actualGpuCullingExecuted|gpu_culling_executed)=false",
        "real_gpu_culling_(?:proven|ready)=true[^`r`n]*(?:actualGpuCullingExecuted|gpu_culling_executed)=false",
        "round9\.(?:realGpuCulling(?:Proven|Ready)|real_gpu_culling_(?:proven|ready))=true[^`r`n]*(?:actualGpuCullingExecuted|gpu_culling_executed)=false",
        "gpuCullingBlockerReason=(?!none|ready|executed|n/?a)[A-Za-z0-9_.:-]+[^`r`n]*(?:realGpuCulling(?:Proven|Ready)|gpuCullingOutputReady)=true",
        "gpu_culling_blocker_reason=(?!none|ready|executed|n/?a)[A-Za-z0-9_.:-]+[^`r`n]*(?:real_gpu_culling_(?:proven|ready)|gpu_culling_output_ready)=true"
    ),

    [string[]] $CpuConservativeCullingTelemetryPatterns = @(
        "cpu(?:Conservative)?Culling(?:Telemetry|Enabled|Active)?=true",
        "conservativeCulling(?:Telemetry|Enabled|Active)?=true",
        "round9\.cpuConservativeCulling(?:Telemetry|Enabled|Active)?=true",
        "round9\.cullingMode=(?:cpu|conservative|cpu-conservative)",
        "cullingTier=(?:cpu|conservative|cpu-conservative)"
    ),

    [string[]] $BoundaryLabelPatterns = @(
        "round9\.boundary(?:Label)?=",
        "boundaryLabel=",
        "metadataOnly(?:Culling|Preview)?=",
        "realGpuCulling(?:Proven|Ready)?=",
        "cullingBoundary="
    ),

    [string[]] $ClusterOverlayPatterns = @(
        "clusterOverlay(?:Visible|Submitted|Enabled)?=true",
        "chunkClusterOverlay(?:Visible|Submitted|Enabled)?=true",
        "round9\.clusterOverlay(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=flat-open-cluster-overlay"
    ),

    [string[]] $CullingOverlayPatterns = @(
        "cullingOverlay(?:Visible|Submitted|Enabled)?=true",
        "chunkCullingOverlay(?:Visible|Submitted|Enabled)?=true",
        "round9\.cullingOverlay(?:Visible|Submitted|Enabled)?=true",
        "artifactRole=(?:interior-wall-culling-overlay|high-distance-open-terrain-culling-overlay)"
    ),

    [string[]] $VisibleCountsChangedPatterns = @(
        "visibleClusterCountsChanged=true",
        "round9\.visibleClusterCountsChanged=true",
        "visible cluster count changed"
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round9-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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

    $numbers = New-Object System.Collections.Generic.List[long]
    foreach ($pattern in $Patterns) {
        $matches = [regex]::Matches($Text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        foreach ($match in $matches) {
            if ($match.Groups.Count -lt 2) {
                continue
            }
            $value = 0L
            if ([long]::TryParse($match.Groups[1].Value, [ref]$value)) {
                $numbers.Add($value) | Out-Null
            }
        }
    }
    return $numbers.ToArray()
}

function Get-MaxNumber {
    param([long[]] $Numbers)

    if ($null -eq $Numbers -or $Numbers.Count -eq 0) {
        return $null
    }
    return ($Numbers | Measure-Object -Maximum).Maximum
}

function Test-DistinctPositiveNumbers {
    param([long[]] $Numbers)

    if ($null -eq $Numbers -or $Numbers.Count -lt 2) {
        return $false
    }
    $positiveDistinct = @($Numbers | Where-Object { $_ -gt 0 } | Sort-Object -Unique)
    return $positiveDistinct.Count -ge 2
}

function Measure-Round9LogProof {
    param([string[]] $ResolvedLogPaths)

    $combined = New-Object System.Text.StringBuilder
    foreach ($path in $ResolvedLogPaths) {
        [void]$combined.AppendLine("### LOG: $path")
        [void]$combined.AppendLine((Get-Content -Raw -LiteralPath $path))
    }
    $log = $combined.ToString()
    $clusterCounts = Get-CapturedNumbers $log $ClusterCountPatterns
    $visibleClusterCounts = Get-CapturedNumbers $log $VisibleClusterCountPatterns
    $culledClusterCounts = Get-CapturedNumbers $log $CulledClusterCountPatterns
    $hiddenClusterCounts = Get-CapturedNumbers $log $HiddenClusterCountPatterns
    $uploadBytes = Get-CapturedNumbers $log $UploadBytesPatterns
    $generationCounters = Get-CapturedNumbers $log $GenerationCounterPatterns
    $indirectDrawCounts = Get-CapturedNumbers $log $IndirectDrawPatterns
    $realIndirectDrawCounts = Get-CapturedNumbers $log $RealIndirectDrawPatterns
    $frustumCandidateCounts = Get-CapturedNumbers $log $FrustumCandidatePatterns
    $occlusionCandidateCounts = Get-CapturedNumbers $log $OcclusionCandidatePatterns
    $combinedCulledHiddenCounts = @($culledClusterCounts) + @($hiddenClusterCounts)

    $explicitVisibleCountsChanged = Test-AnyRegex $log $VisibleCountsChangedPatterns
    $derivedVisibleCountsChanged = Test-DistinctPositiveNumbers $visibleClusterCounts

    return [ordered]@{
        logPaths = @($ResolvedLogPaths)
        markers = [ordered]@{
            round9MarkerPresent = Test-AnyRegex $log $Round9MarkerPatterns
            clusterCountPresent = Test-AnyRegex $log $ClusterCountPatterns
            visibleClusterCountPresent = Test-AnyRegex $log $VisibleClusterCountPatterns
            visibleClusterCountsChanged = $explicitVisibleCountsChanged -or $derivedVisibleCountsChanged
            culledOrOffscreenCountPresent = Test-AnyRegex $log $CulledClusterCountPatterns
            hiddenClusterCountPresent = Test-AnyRegex $log $HiddenClusterCountPatterns
            culledOffscreenOrHiddenCountPresent = (Test-AnyRegex $log $CulledClusterCountPatterns) -or (Test-AnyRegex $log $HiddenClusterCountPatterns)
            cpuConservativeCullingTelemetryPresent = Test-AnyRegex $log $CpuConservativeCullingTelemetryPatterns
            uploadBytesPresent = Test-AnyRegex $log $UploadBytesPatterns
            generationCounterPresent = Test-AnyRegex $log $GenerationCounterPatterns
            indirectDrawPresent = Test-AnyRegex $log $IndirectDrawPatterns
            realIndirectDrawPresent = Test-AnyRegex $log $RealIndirectDrawPatterns
            gpuCullingExecutedPresent = Test-AnyRegex $log $GpuCullingExecutedPatterns
            gpuCullingPrerequisitesReadyPresent = Test-AnyRegex $log $GpuCullingPrerequisitesReadyPatterns
            gpuCullingBlockerReasonPresent = Test-AnyRegex $log $GpuCullingBlockerReasonPatterns
            frustumCandidateCountPresent = Test-AnyRegex $log $FrustumCandidatePatterns
            occlusionCandidateCountPresent = Test-AnyRegex $log $OcclusionCandidatePatterns
            indirectDrawReadyPresent = Test-AnyRegex $log $IndirectDrawReadyPatterns
            forestComplexCapturePresent = Test-AnyRegex $log $ForestComplexPatterns
            frameTimingPresent = Test-AnyRegex $log $FrameTimingPatterns
            overclaimPresent = Test-AnyRegex $log $OverclaimPatterns
            boundaryLabelPresent = Test-AnyRegex $log $BoundaryLabelPatterns
            clusterOverlayPresent = Test-AnyRegex $log $ClusterOverlayPatterns
            cullingOverlayPresent = Test-AnyRegex $log $CullingOverlayPatterns
            invalidClusterValuesPresent = Test-Regex $log "invalidCluster(?:Count|s)?=true|negative cluster|cluster(?:Count|s)?=.*(?:NaN|Infinity)|visibleCluster(?:Count|s)?=.*(?:NaN|Infinity)"
            terrainCorruptionPresent = Test-Regex $log "terrain corruption|missing terrain|chunk hole|geometry corruption|invalid meshlet|cluster bounds invalid"
            proofMarkerPresent = Test-Regex $log "round9\.(?:proofMarker|focusWindowOnly)=true|Round 9 .*proof marker|Round 9 .*focus-window-only"
            temporaryDirectLightSourcePresent = Test-Regex $log "round9\.temporaryDirectLightSource=true|Round 9 .*temporary direct-light|Round 9 .*current direct-light RGBA payload"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            clusterCounts = @($clusterCounts)
            visibleClusterCounts = @($visibleClusterCounts)
            culledClusterCounts = @($culledClusterCounts)
            hiddenClusterCounts = @($hiddenClusterCounts)
            uploadBytes = @($uploadBytes)
            generationCounters = @($generationCounters)
            indirectDrawCounts = @($indirectDrawCounts)
            realIndirectDrawCounts = @($realIndirectDrawCounts)
            frustumCandidateCounts = @($frustumCandidateCounts)
            occlusionCandidateCounts = @($occlusionCandidateCounts)
            maxClusterCount = Get-MaxNumber $clusterCounts
            maxVisibleClusterCount = Get-MaxNumber $visibleClusterCounts
            maxCulledClusterCount = Get-MaxNumber $culledClusterCounts
            maxHiddenClusterCount = Get-MaxNumber $hiddenClusterCounts
            maxCulledOffscreenOrHiddenCount = Get-MaxNumber $combinedCulledHiddenCounts
            maxUploadBytes = Get-MaxNumber $uploadBytes
            maxGenerationCounter = Get-MaxNumber $generationCounters
            maxIndirectDrawCount = Get-MaxNumber $indirectDrawCounts
            maxRealIndirectDrawCount = Get-MaxNumber $realIndirectDrawCounts
            maxFrustumCandidateCount = Get-MaxNumber $frustumCandidateCounts
            maxOcclusionCandidateCount = Get-MaxNumber $occlusionCandidateCounts
        }
        patterns = [ordered]@{
            round9MarkerPatterns = @($Round9MarkerPatterns)
            clusterCountPatterns = @($ClusterCountPatterns)
            visibleClusterCountPatterns = @($VisibleClusterCountPatterns)
            culledClusterCountPatterns = @($CulledClusterCountPatterns)
            hiddenClusterCountPatterns = @($HiddenClusterCountPatterns)
            uploadBytesPatterns = @($UploadBytesPatterns)
            generationCounterPatterns = @($GenerationCounterPatterns)
            indirectDrawPatterns = @($IndirectDrawPatterns)
            realIndirectDrawPatterns = @($RealIndirectDrawPatterns)
            gpuCullingExecutedPatterns = @($GpuCullingExecutedPatterns)
            gpuCullingPrerequisitesReadyPatterns = @($GpuCullingPrerequisitesReadyPatterns)
            gpuCullingBlockerReasonPatterns = @($GpuCullingBlockerReasonPatterns)
            frustumCandidatePatterns = @($FrustumCandidatePatterns)
            occlusionCandidatePatterns = @($OcclusionCandidatePatterns)
            indirectDrawReadyPatterns = @($IndirectDrawReadyPatterns)
            forestComplexPatterns = @($ForestComplexPatterns)
            frameTimingPatterns = @($FrameTimingPatterns)
            overclaimPatterns = @($OverclaimPatterns)
            cpuConservativeCullingTelemetryPatterns = @($CpuConservativeCullingTelemetryPatterns)
            boundaryLabelPatterns = @($BoundaryLabelPatterns)
            clusterOverlayPatterns = @($ClusterOverlayPatterns)
            cullingOverlayPatterns = @($CullingOverlayPatterns)
        }
    }
}

$flatResolved = Resolve-ExistingFile $FlatClusterOverlayImagePath "Flat/open cluster overlay image"
$interiorResolved = Resolve-ExistingFile $InteriorCullingOverlayImagePath "Interior/wall-facing culling overlay image"
$highDistanceResolved = Resolve-ExistingFile $HighDistanceCullingOverlayImagePath "High render-distance/open terrain culling overlay image"
$forestComplexResolved = if ([string]::IsNullOrWhiteSpace($ForestComplexCullingOverlayImagePath)) { "" } else { Resolve-ExistingFile $ForestComplexCullingOverlayImagePath "Forest/complex culling overlay image" }
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$flatDimensions = Get-ImageDimensions $flatResolved
$interiorDimensions = Get-ImageDimensions $interiorResolved
$highDistanceDimensions = Get-ImageDimensions $highDistanceResolved
$forestComplexDimensions = if ([string]::IsNullOrWhiteSpace($forestComplexResolved)) { $null } else { Get-ImageDimensions $forestComplexResolved }

$interiorDelta = Invoke-DeltaHelper $flatResolved $interiorResolved "interior-culling"
$highDistanceDelta = Invoke-DeltaHelper $flatResolved $highDistanceResolved "high-distance-culling"
$forestComplexDelta = if ([string]::IsNullOrWhiteSpace($forestComplexResolved)) { $null } else { Invoke-DeltaHelper $flatResolved $forestComplexResolved "forest-complex-culling" }

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round9LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
    @{ label = "interior/wall-facing culling overlay"; dimensions = $interiorDimensions },
    @{ label = "high render-distance/open terrain culling overlay"; dimensions = $highDistanceDimensions },
    @{ label = "forest/complex culling overlay"; dimensions = $forestComplexDimensions }
)) {
    if ($null -eq $entry.dimensions) {
        continue
    }
    if (($entry.dimensions.width -ne $flatDimensions.width) -or ($entry.dimensions.height -ne $flatDimensions.height)) {
        $failures.Add("$($entry.label) image dimensions differ from flat/open cluster overlay. flat=$($flatDimensions.width)x$($flatDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ([double]$interiorDelta.focusRegionMetrics.changedPixelPercent -lt $MinInteriorCullingChangedPixelPercent) {
    $failures.Add("Interior/wall-facing culling overlay changed pixels below threshold. actual=$($interiorDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinInteriorCullingChangedPixelPercent")
}
if ([double]$highDistanceDelta.focusRegionMetrics.changedPixelPercent -lt $MinHighDistanceCullingChangedPixelPercent) {
    $failures.Add("High render-distance/open terrain culling overlay changed pixels below threshold. actual=$($highDistanceDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinHighDistanceCullingChangedPixelPercent")
}
if ($forestComplexDelta -and [double]$forestComplexDelta.focusRegionMetrics.changedPixelPercent -lt $MinForestComplexChangedPixelPercent) {
    $failures.Add("Forest/complex culling overlay changed pixels below threshold. actual=$($forestComplexDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinForestComplexChangedPixelPercent")
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.round9MarkerPresent) {
        $failures.Add("Missing Round 9 virtualized chunk geometry log marker.")
    }
    if (-not $logProof.markers.clusterCountPresent) {
        $failures.Add("Missing Round 9 cluster count marker.")
    }
    if (-not $logProof.markers.visibleClusterCountPresent) {
        $failures.Add("Missing Round 9 visible cluster count marker.")
    }
    if (-not $logProof.markers.visibleClusterCountsChanged) {
        $failures.Add("Missing Round 9 visible-cluster count change marker or distinct positive visible counts.")
    }
    if (-not $logProof.markers.culledOffscreenOrHiddenCountPresent) {
        $failures.Add("Missing Round 9 culled/offscreen/hidden cluster count marker.")
    }
    if ($logProof.markers.cpuConservativeCullingTelemetryPresent) {
        if (-not $logProof.markers.culledOffscreenOrHiddenCountPresent) {
            $failures.Add("CPU/conservative Round 9 culling telemetry is present but no culled/offscreen/hidden count marker was found.")
        } elseif ($null -eq $logProof.counts.maxCulledOffscreenOrHiddenCount -or [long]$logProof.counts.maxCulledOffscreenOrHiddenCount -le 0) {
            $failures.Add("CPU/conservative Round 9 culling telemetry is present but culled/offscreen/hidden count never becomes nonzero.")
        }
        if (-not $logProof.markers.realIndirectDrawPresent) {
            if (-not $logProof.markers.indirectDrawReadyPresent) {
                $failures.Add("CPU/conservative Round 9 culling telemetry is present but no real indirect draw or explicit indirect readiness boundary marker was found.")
            } elseif ($null -eq $logProof.counts.maxIndirectDrawCount -or [long]$logProof.counts.maxIndirectDrawCount -le 0) {
                $failures.Add("CPU/conservative Round 9 culling telemetry is present but indirect draw candidate/placeholder count never becomes nonzero.")
            }
        } elseif ($null -eq $logProof.counts.maxRealIndirectDrawCount -or [long]$logProof.counts.maxRealIndirectDrawCount -le 0) {
            $failures.Add("CPU/conservative Round 9 culling telemetry has a real indirect marker but real indirect_draw_count never becomes nonzero.")
        }
    }
    if (-not $logProof.markers.uploadBytesPresent) {
        $failures.Add("Missing Round 9 cluster upload byte marker.")
    }
    if (-not $logProof.markers.generationCounterPresent) {
        $failures.Add("Missing Round 9 generation counter marker.")
    }
    if (-not $logProof.markers.indirectDrawPresent) {
        $failures.Add("Missing Round 9 indirect draw placeholder/count marker.")
    }
    if (-not $logProof.markers.gpuCullingExecutedPresent) {
        $failures.Add("Missing Round 9 actual GPU-culling execution marker.")
    }
    if (-not $logProof.markers.gpuCullingPrerequisitesReadyPresent) {
        $failures.Add("Missing Round 9 GPU-culling prerequisite readiness marker.")
    }
    if (-not $logProof.markers.gpuCullingBlockerReasonPresent) {
        $failures.Add("Missing Round 9 GPU-culling blocker reason marker.")
    }
    if (-not $logProof.markers.frustumCandidateCountPresent) {
        $failures.Add("Missing Round 9 frustum candidate count marker.")
    }
    if (-not $logProof.markers.occlusionCandidateCountPresent) {
        $failures.Add("Missing Round 9 occlusion candidate/placeholder count marker.")
    }
    if (-not $logProof.markers.indirectDrawReadyPresent) {
        $failures.Add("Missing Round 9 indirect draw readiness marker.")
    }
    if (-not $logProof.markers.frameTimingPresent) {
        $failures.Add("Missing Round 9 CPU/GPU frame timing or timing-placeholder marker.")
    }
    if ($forestComplexDelta -and -not $logProof.markers.forestComplexCapturePresent) {
        $failures.Add("Forest/complex image was provided but logs do not contain a forest/complex capture or density marker.")
    }
    if (-not $logProof.markers.clusterOverlayPresent) {
        $failures.Add("Missing Round 9 cluster overlay artifact/render marker.")
    }
    if (-not $logProof.markers.cullingOverlayPresent) {
        $failures.Add("Missing Round 9 culling overlay artifact/render marker.")
    }
    if ($logProof.markers.invalidClusterValuesPresent) {
        $failures.Add("Log contains invalid cluster value markers.")
    }
    if ($logProof.markers.terrainCorruptionPresent) {
        $failures.Add("Log contains terrain/geometry corruption markers.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; Round 9 proof must use virtualized geometry/culling paths.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker or focus-window-only evidence; Round 9 proof must use requested overlay artifacts.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
    if ($logProof.markers.overclaimPresent) {
        $failures.Add("Log contains Round 9 GPU-culling overclaim markers.")
    }
}

$result = [ordered]@{
    overlayArtifacts = [ordered]@{
        flatClusterOverlay = [ordered]@{
            path = $flatResolved
            dimensions = $flatDimensions
            role = "flat-open-cluster-overlay"
        }
        interiorCullingOverlay = [ordered]@{
            path = $interiorResolved
            dimensions = $interiorDimensions
            role = "interior-wall-facing-culling-overlay"
        }
        highDistanceCullingOverlay = [ordered]@{
            path = $highDistanceResolved
            dimensions = $highDistanceDimensions
            role = "high-render-distance-open-terrain-culling-overlay"
        }
        forestComplexCullingOverlay = if ($forestComplexResolved) {
            [ordered]@{
                path = $forestComplexResolved
                dimensions = $forestComplexDimensions
                role = "forest-complex-culling-overlay"
            }
        } else {
            $null
        }
    }
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minInteriorCullingChangedPixelPercent = $MinInteriorCullingChangedPixelPercent
        minHighDistanceCullingChangedPixelPercent = $MinHighDistanceCullingChangedPixelPercent
        minForestComplexChangedPixelPercent = $MinForestComplexChangedPixelPercent
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
        flatToInteriorCulling = $interiorDelta
        flatToHighDistanceCulling = $highDistanceDelta
        flatToForestComplexCulling = $forestComplexDelta
    }
    selectedFocusRegions = [ordered]@{
        flatToInteriorCulling = $interiorDelta.focusRegion
        flatToHighDistanceCulling = $highDistanceDelta.focusRegion
        flatToForestComplexCulling = if ($forestComplexDelta) { $forestComplexDelta.focusRegion } else { $null }
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round9_virtualized_geometry_evidence_passed" } else { "round9_virtualized_geometry_evidence_failed" }
        tracks = [ordered]@{
            clusterMetadata = [ordered]@{
                imageArtifactPresent = $true
                clusterCountPresent = if ($logProof) { [bool]$logProof.markers.clusterCountPresent } else { $null }
                uploadBytesPresent = if ($logProof) { [bool]$logProof.markers.uploadBytesPresent } else { $null }
                generationCounterPresent = if ($logProof) { [bool]$logProof.markers.generationCounterPresent } else { $null }
            }
            culling = [ordered]@{
                interiorImageDeltaPresent = ([double]$interiorDelta.focusRegionMetrics.changedPixelPercent -ge $MinInteriorCullingChangedPixelPercent)
                highDistanceImageDeltaPresent = ([double]$highDistanceDelta.focusRegionMetrics.changedPixelPercent -ge $MinHighDistanceCullingChangedPixelPercent)
                visibleClusterCountPresent = if ($logProof) { [bool]$logProof.markers.visibleClusterCountPresent } else { $null }
                visibleClusterCountsChanged = if ($logProof) { [bool]$logProof.markers.visibleClusterCountsChanged } else { $null }
                culledOrOffscreenCountPresent = if ($logProof) { [bool]$logProof.markers.culledOrOffscreenCountPresent } else { $null }
                hiddenClusterCountPresent = if ($logProof) { [bool]$logProof.markers.hiddenClusterCountPresent } else { $null }
                culledOffscreenOrHiddenCountPresent = if ($logProof) { [bool]$logProof.markers.culledOffscreenOrHiddenCountPresent } else { $null }
                maxCulledOffscreenOrHiddenCount = if ($logProof) { $logProof.counts.maxCulledOffscreenOrHiddenCount } else { $null }
                cpuConservativeCullingTelemetryPresent = if ($logProof) { [bool]$logProof.markers.cpuConservativeCullingTelemetryPresent } else { $null }
                indirectDrawPresent = if ($logProof) { [bool]$logProof.markers.indirectDrawPresent } else { $null }
                realIndirectDrawPresent = if ($logProof) { [bool]$logProof.markers.realIndirectDrawPresent } else { $null }
                maxRealIndirectDrawCount = if ($logProof) { $logProof.counts.maxRealIndirectDrawCount } else { $null }
                gpuCullingExecutedPresent = if ($logProof) { [bool]$logProof.markers.gpuCullingExecutedPresent } else { $null }
                gpuCullingPrerequisitesReadyPresent = if ($logProof) { [bool]$logProof.markers.gpuCullingPrerequisitesReadyPresent } else { $null }
                gpuCullingBlockerReasonPresent = if ($logProof) { [bool]$logProof.markers.gpuCullingBlockerReasonPresent } else { $null }
                frustumCandidateCountPresent = if ($logProof) { [bool]$logProof.markers.frustumCandidateCountPresent } else { $null }
                maxFrustumCandidateCount = if ($logProof) { $logProof.counts.maxFrustumCandidateCount } else { $null }
                occlusionCandidateCountPresent = if ($logProof) { [bool]$logProof.markers.occlusionCandidateCountPresent } else { $null }
                maxOcclusionCandidateCount = if ($logProof) { $logProof.counts.maxOcclusionCandidateCount } else { $null }
                indirectDrawReadyPresent = if ($logProof) { [bool]$logProof.markers.indirectDrawReadyPresent } else { $null }
                frameTimingPresent = if ($logProof) { [bool]$logProof.markers.frameTimingPresent } else { $null }
                forestComplexImageDeltaPresent = if ($forestComplexDelta) { ([double]$forestComplexDelta.focusRegionMetrics.changedPixelPercent -ge $MinForestComplexChangedPixelPercent) } else { $null }
                forestComplexCapturePresent = if ($logProof) { [bool]$logProof.markers.forestComplexCapturePresent } else { $null }
            }
            proofBoundary = [ordered]@{
                boundaryLabelPresent = if ($logProof) { [bool]$logProof.markers.boundaryLabelPresent } else { $null }
                overclaimPresent = if ($logProof) { [bool]$logProof.markers.overclaimPresent } else { $null }
                classification = if ($logProof -and [bool]$logProof.markers.cpuConservativeCullingTelemetryPresent) { "cpu_conservative_culling_requires_nonzero_cull_and_real_indirect_draw" } else { "metadata_or_placeholder_culling_boundary_reported" }
            }
            overlays = [ordered]@{
                clusterOverlayPresent = if ($logProof) { [bool]$logProof.markers.clusterOverlayPresent } else { $null }
                cullingOverlayPresent = if ($logProof) { [bool]$logProof.markers.cullingOverlayPresent } else { $null }
            }
            rejectionMarkers = [ordered]@{
                invalidClusterValuesPresent = if ($logProof) { [bool]$logProof.markers.invalidClusterValuesPresent } else { $null }
                terrainCorruptionPresent = if ($logProof) { [bool]$logProof.markers.terrainCorruptionPresent } else { $null }
                temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
                proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
                nativeErrorPresent = if ($logProof) { [bool]$logProof.markers.nativeErrorPresent } else { $null }
                overclaimPresent = if ($logProof) { [bool]$logProof.markers.overclaimPresent } else { $null }
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

Write-Host "flatClusterOverlay=$($result.overlayArtifacts.flatClusterOverlay.path)"
Write-Host "interiorCullingOverlay=$($result.overlayArtifacts.interiorCullingOverlay.path)"
Write-Host "highDistanceCullingOverlay=$($result.overlayArtifacts.highDistanceCullingOverlay.path)"
if ($forestComplexResolved) {
    Write-Host "forestComplexCullingOverlay=$($result.overlayArtifacts.forestComplexCullingOverlay.path)"
}
Write-Host "logPaths=$($result.logPaths -join ';')"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "interiorCulling.focusRegion=$($interiorDelta.focusRegion.left),$($interiorDelta.focusRegion.top),$($interiorDelta.focusRegion.width),$($interiorDelta.focusRegion.height)"
Write-Host "interiorCulling.focus.changedPixelPercent=$($interiorDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "highDistanceCulling.focusRegion=$($highDistanceDelta.focusRegion.left),$($highDistanceDelta.focusRegion.top),$($highDistanceDelta.focusRegion.width),$($highDistanceDelta.focusRegion.height)"
Write-Host "highDistanceCulling.focus.changedPixelPercent=$($highDistanceDelta.focusRegionMetrics.changedPixelPercent)"
if ($forestComplexDelta) {
    Write-Host "forestComplexCulling.focusRegion=$($forestComplexDelta.focusRegion.left),$($forestComplexDelta.focusRegion.top),$($forestComplexDelta.focusRegion.width),$($forestComplexDelta.focusRegion.height)"
    Write-Host "forestComplexCulling.focus.changedPixelPercent=$($forestComplexDelta.focusRegionMetrics.changedPixelPercent)"
}
if ($logProof) {
    Write-Host "round9MarkerPresent=$($logProof.markers.round9MarkerPresent)"
    Write-Host "clusterCountPresent=$($logProof.markers.clusterCountPresent)"
    Write-Host "visibleClusterCountPresent=$($logProof.markers.visibleClusterCountPresent)"
    Write-Host "visibleClusterCountsChanged=$($logProof.markers.visibleClusterCountsChanged)"
    Write-Host "culledOrOffscreenCountPresent=$($logProof.markers.culledOrOffscreenCountPresent)"
    Write-Host "hiddenClusterCountPresent=$($logProof.markers.hiddenClusterCountPresent)"
    Write-Host "culledOffscreenOrHiddenCountPresent=$($logProof.markers.culledOffscreenOrHiddenCountPresent)"
    Write-Host "maxCulledOffscreenOrHiddenCount=$($logProof.counts.maxCulledOffscreenOrHiddenCount)"
    Write-Host "cpuConservativeCullingTelemetryPresent=$($logProof.markers.cpuConservativeCullingTelemetryPresent)"
    Write-Host "uploadBytesPresent=$($logProof.markers.uploadBytesPresent)"
    Write-Host "generationCounterPresent=$($logProof.markers.generationCounterPresent)"
    Write-Host "indirectDrawPresent=$($logProof.markers.indirectDrawPresent)"
    Write-Host "realIndirectDrawPresent=$($logProof.markers.realIndirectDrawPresent)"
    Write-Host "maxRealIndirectDrawCount=$($logProof.counts.maxRealIndirectDrawCount)"
    Write-Host "gpuCullingExecutedPresent=$($logProof.markers.gpuCullingExecutedPresent)"
    Write-Host "gpuCullingPrerequisitesReadyPresent=$($logProof.markers.gpuCullingPrerequisitesReadyPresent)"
    Write-Host "gpuCullingBlockerReasonPresent=$($logProof.markers.gpuCullingBlockerReasonPresent)"
    Write-Host "frustumCandidateCountPresent=$($logProof.markers.frustumCandidateCountPresent)"
    Write-Host "maxFrustumCandidateCount=$($logProof.counts.maxFrustumCandidateCount)"
    Write-Host "occlusionCandidateCountPresent=$($logProof.markers.occlusionCandidateCountPresent)"
    Write-Host "maxOcclusionCandidateCount=$($logProof.counts.maxOcclusionCandidateCount)"
    Write-Host "indirectDrawReadyPresent=$($logProof.markers.indirectDrawReadyPresent)"
    Write-Host "forestComplexCapturePresent=$($logProof.markers.forestComplexCapturePresent)"
    Write-Host "frameTimingPresent=$($logProof.markers.frameTimingPresent)"
    Write-Host "boundaryLabelPresent=$($logProof.markers.boundaryLabelPresent)"
    Write-Host "clusterOverlayPresent=$($logProof.markers.clusterOverlayPresent)"
    Write-Host "cullingOverlayPresent=$($logProof.markers.cullingOverlayPresent)"
    Write-Host "invalidClusterValuesPresent=$($logProof.markers.invalidClusterValuesPresent)"
    Write-Host "terrainCorruptionPresent=$($logProof.markers.terrainCorruptionPresent)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
    Write-Host "overclaimPresent=$($logProof.markers.overclaimPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Round 9 virtualized geometry proof failed: $($failures -join '; ')"
}
