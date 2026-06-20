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

    [string[]] $WallHitPatterns = @(
        "wallHit(?:Count|s)?=([1-9][0-9]*)",
        "wall_hit_count=([1-9][0-9]*)",
        "round10\.wallHit(?:Count|s)?=([1-9][0-9]*)"
    ),

    [string[]] $OpenSkyMissPatterns = @(
        "openSkyMiss(?:Count|s)?=([1-9][0-9]*)",
        "open_sky_miss_count=([1-9][0-9]*)",
        "round10\.openSkyMiss(?:Count|s)?=([1-9][0-9]*)"
    ),

    [string[]] $GlassWaterMaterialHitPatterns = @(
        "(?:glassWater|glassOrWater|transparentMaterial)Hit(?:Count|s)?=([1-9][0-9]*)",
        "(?:glass_water|glass_or_water|transparent_material)_hit_count=([1-9][0-9]*)",
        "round10\.(?:glassWater|glassOrWater|transparentMaterial)Hits=([1-9][0-9]*)"
    ),

    [string[]] $OpaqueMaterialHitPatterns = @(
        "opaqueMaterialHit(?:Count|s)?=([1-9][0-9]*)",
        "opaque_material_hit_count=([1-9][0-9]*)",
        "round10\.opaqueMaterialHits=([1-9][0-9]*)"
    ),

    [string[]] $MaterialIdConsistencyPatterns = @(
        "materialIdConsistency(?:Ready|Passed)?=true",
        "material_id_consistency(?:_ready|_passed)?=true",
        "round10\.materialIdConsistency(?:Ready|Passed)?=true"
    ),

    [string[]] $MaterialLookupReadyPatterns = @(
        "materialLookupReady=true",
        "material_lookup_ready=true",
        "round10\.materialLookupReady=true"
    ),

    [string[]] $MaskBitsReadyPatterns = @(
        "maskBitsReady=true",
        "mask_bits_ready=true",
        "round10\.maskBitsReady=true"
    ),

    [string[]] $MaskBitsSourcePatterns = @(
        "maskBitsSource=(?:material-table|voxel-material|native-voxel|world-extraction|block-state)",
        "mask_bits_source=(?:material-table|voxel-material|native-voxel|world-extraction|block-state)",
        "round10\.maskBitsSource=(?:material-table|voxel-material|native-voxel|world-extraction|block-state)"
    ),

    [string[]] $EmptySectionSkipSafePatterns = @(
        "emptySectionSkipSafe=true",
        "empty_section_skip_safe=true",
        "round10\.emptySectionSkipSafe=true"
    ),

    [string[]] $SectionLifecyclePatterns = @(
        "sectionLifecycle(?:Marker|Ready|Observed)?=true",
        "section_lifecycle(?:_marker|_ready|_observed)?=true",
        "round10\.sectionLifecycle(?:Marker|Ready|Observed)?=true",
        "sectionLifecycleCount=([1-9][0-9]*)",
        "section_lifecycle_count=([1-9][0-9]*)",
        "section_lifecycle_marker_count=([1-9][0-9]*)",
        "round10\.sectionLifecycleCount=([1-9][0-9]*)"
    ),

    [string[]] $LifecycleShutdownPatterns = @(
        "worldLeaveSeen=(?:true|false)",
        "world_leave_seen=(?:true|false)",
        "round10\.worldLeaveSeen=(?:true|false)",
        "shutdownSafe=(?:true|false)",
        "shutdown_safe=(?:true|false)",
        "round10\.shutdownSafe=(?:true|false)"
    ),

    [string[]] $TraversalBackendPatterns = @(
        "traversalBackend=(?:cpu|native-cpu|gpu-boundary|fallback|voxel-cpu)",
        "traversal_backend=(?:cpu|native-cpu|gpu-boundary|fallback|voxel-cpu)",
        "round10\.traversalBackend=(?:cpu|native-cpu|gpu-boundary|fallback|voxel-cpu)"
    ),

    [string[]] $RealGpuTraversalBoundaryPatterns = @(
        "realGpuTraversalExecuted=false",
        "real_gpu_traversal_executed=false",
        "round10\.realGpuTraversalExecuted=false",
        "gpuTraversalBoundary=(?:open|not-proven|fallback|cpu-status)",
        "gpu_traversal_boundary=(?:open|not-proven|fallback|cpu-status)"
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

    [string[]] $HardwareRtExecutionPatterns = @(
        "hardwareRtExecutionProven=(?:true|false)",
        "hardware_rt_execution_proven=(?:true|false)",
        "round10\.hardwareRtExecutionProven=(?:true|false)"
    ),

    [string[]] $HardwareRtFallbackPatterns = @(
        "hardwareRtExecutionProven=false[^`r`n]*(?:rtFallback(?:Status|Active)?|fallbackStatus|nonRtFallback|hardwareRtFallback)=([^`r`n ]+)",
        "hardware_rt_execution_proven=false[^`r`n]*(?:rt_fallback(?:_status|_active)?|fallback_status|non_rt_fallback|hardware_rt_fallback)=([^`r`n ]+)",
        "(?:rtFallback(?:Status|Active)?|fallbackStatus|nonRtFallback|hardwareRtFallback)=(?:active|true|cpu|native-cpu|unsupported|unavailable|fallback)",
        "hardwareRtFallbackAccepted=true"
    ),

    [string[]] $EntityMovementPatterns = @(
        "entityMovement(?:Marker|Ready|Observed)?=true",
        "entity_movement(?:_marker|_ready|_observed)?=true",
        "round10\.entityMovement(?:Marker|Ready|Observed)?=true"
    ),

    [string[]] $EntityMovementCountPatterns = @(
        "entityMovementCount=([1-9][0-9]*)",
        "entity_movement_count=([1-9][0-9]*)",
        "entity_movement_marker_count=([1-9][0-9]*)",
        "round10\.entityMovementCount=([1-9][0-9]*)"
    ),

    [string[]] $ChunkChurnPatterns = @(
        "chunkChurn(?:Marker|Ready|Observed)?=true",
        "chunk_churn(?:_marker|_ready|_observed)?=true",
        "round10\.chunkChurn(?:Marker|Ready|Observed)?=true"
    ),

    [string[]] $ChunkChurnCountPatterns = @(
        "chunkChurnCount=([1-9][0-9]*)",
        "chunk_churn_count=([1-9][0-9]*)",
        "chunk_churn_marker_count=([1-9][0-9]*)",
        "round10\.chunkChurnCount=([1-9][0-9]*)"
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

    [string[]] $SourceStabilityPatterns = @(
        "srcStable=(?:true|stable|selected|consistent)",
        "sourceStable=(?:true|stable|selected|consistent)",
        "selectedSourceStable=(?:true|stable|selected|consistent)",
        "source_stable=(?:true|stable|selected|consistent)",
        "selected_source_stability=(?:true|stable|selected|consistent)",
        "round10\.sourceStability=(?:true|stable|selected|consistent)"
    ),

    [string[]] $ChunkChurnMaterialConsistencyPatterns = @(
        "chunkChurnMaterialConsistent=true",
        "chunk_churn_material_consistent=true",
        "materialConsistentDuringChunkChurn=true",
        "material_consistent_during_chunk_churn=true",
        "chunk_churn_material_stable=true",
        "round10\.chunkChurnMaterialConsistent=true"
    ),

    [string[]] $EntityMovementMaterialConsistencyPatterns = @(
        "entityMoveMaterialConsistent=true",
        "entity_move_material_consistent=true",
        "materialConsistentDuringEntityMovement=true",
        "material_consistent_during_entity_movement=true",
        "entity_movement_material_stable=true",
        "round10\.entityMoveMaterialConsistent=true"
    ),

    [string[]] $RealTracedLightingConsumedFalsePatterns = @(
        "realTracedLightingConsumed=false",
        "real_traced_lighting_consumed=false",
        "traced_lighting_consumed=false",
        "round10\.realTracedLightingConsumed=false"
    ),

    [string[]] $TracedLightingNoOverclaimPatterns = @(
        "tracedLightingNoOverclaim=true",
        "traced_lighting_no_overclaim=true",
        "round10\.tracedLightingNoOverclaim=true",
        "realTracedLightingConsumed=false[^`r`n]*(?:open|boundary|not[-_ ]?consumed|no[-_ ]?overclaim)",
        "real_traced_lighting_consumed=false[^`r`n]*(?:open|boundary|not[-_ ]?consumed|no[-_ ]?overclaim)"
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
    $wallHits = Get-CapturedNumbers $log $WallHitPatterns
    $openSkyMisses = Get-CapturedNumbers $log $OpenSkyMissPatterns
    $glassWaterMaterialHits = Get-CapturedNumbers $log $GlassWaterMaterialHitPatterns
    $opaqueMaterialHits = Get-CapturedNumbers $log $OpaqueMaterialHitPatterns
    $sectionLifecycleCounts = Get-CapturedNumbers $log $SectionLifecyclePatterns
    $hybridVoxelHits = Get-CapturedNumbers $log $HybridVoxelHitPatterns
    $hybridRtHits = Get-CapturedNumbers $log $HybridRtHitPatterns
    $hybridScreenSpaceHits = Get-CapturedNumbers $log $HybridScreenSpaceHitPatterns
    $entityMovementCounts = Get-CapturedNumbers $log $EntityMovementCountPatterns
    $chunkChurnCounts = Get-CapturedNumbers $log $ChunkChurnCountPatterns

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
            wallHitPresent = Test-AnyRegex $log $WallHitPatterns
            openSkyMissPresent = Test-AnyRegex $log $OpenSkyMissPatterns
            glassWaterMaterialHitPresent = Test-AnyRegex $log $GlassWaterMaterialHitPatterns
            opaqueMaterialHitPresent = Test-AnyRegex $log $OpaqueMaterialHitPatterns
            materialIdConsistencyPresent = Test-AnyRegex $log $MaterialIdConsistencyPatterns
            materialLookupReadyPresent = Test-AnyRegex $log $MaterialLookupReadyPatterns
            maskBitsReadyPresent = Test-AnyRegex $log $MaskBitsReadyPatterns
            maskBitsSourcePresent = Test-AnyRegex $log $MaskBitsSourcePatterns
            emptySectionSkipSafePresent = Test-AnyRegex $log $EmptySectionSkipSafePatterns
            sectionLifecyclePresent = Test-AnyRegex $log $SectionLifecyclePatterns
            lifecycleShutdownPresent = Test-AnyRegex $log $LifecycleShutdownPatterns
            traversalBackendPresent = Test-AnyRegex $log $TraversalBackendPatterns
            realGpuTraversalBoundaryPresent = Test-AnyRegex $log $RealGpuTraversalBoundaryPatterns
            blasStatusPresent = Test-AnyRegex $log $BlasStatusPatterns
            tlasStatusPresent = Test-AnyRegex $log $TlasStatusPatterns
            fallbackStatusPresent = Test-AnyRegex $log $FallbackStatusPatterns
            hardwareRtExecutionPresent = Test-AnyRegex $log $HardwareRtExecutionPatterns
            hardwareRtExecutionProvenTrue = Test-Regex $log "(?:hardwareRtExecutionProven|hardware_rt_execution_proven|round10\.hardwareRtExecutionProven)=true"
            hardwareRtExecutionProvenFalse = Test-Regex $log "(?:hardwareRtExecutionProven|hardware_rt_execution_proven|round10\.hardwareRtExecutionProven)=false"
            hardwareRtFallbackPresent = Test-AnyRegex $log $HardwareRtFallbackPatterns
            hybridVoxelHitPresent = Test-AnyRegex $log $HybridVoxelHitPatterns
            hybridRtHitPresent = Test-AnyRegex $log $HybridRtHitPatterns
            hybridScreenSpaceHitPresent = Test-AnyRegex $log $HybridScreenSpaceHitPatterns
            entityMovementPresent = Test-AnyRegex $log $EntityMovementPatterns
            entityMovementCountPresent = Test-AnyRegex $log $EntityMovementCountPatterns
            chunkChurnPresent = Test-AnyRegex $log $ChunkChurnPatterns
            chunkChurnCountPresent = Test-AnyRegex $log $ChunkChurnCountPatterns
            boundaryLabelPresent = Test-AnyRegex $log $BoundaryLabelPatterns
            sourceStabilityPresent = Test-AnyRegex $log $SourceStabilityPatterns
            chunkChurnMaterialConsistencyPresent = Test-AnyRegex $log $ChunkChurnMaterialConsistencyPatterns
            entityMovementMaterialConsistencyPresent = Test-AnyRegex $log $EntityMovementMaterialConsistencyPatterns
            realTracedLightingConsumedFalsePresent = Test-AnyRegex $log $RealTracedLightingConsumedFalsePatterns
            tracedLightingNoOverclaimPresent = Test-AnyRegex $log $TracedLightingNoOverclaimPatterns
            invalidTracingValuesPresent = Test-Regex $log "invalid(?:VoxelRay|Traversal|HybridHit|RtEntity|Material|MaskBits)(?:Count|s)?=true|negative (?:voxel ray|traversal|hybrid|BLAS|TLAS|material|wall|open sky)|(?:voxelRay(?:Count|s)?|hybridHit(?:Count|s)?|traversal(?:Step|Steps|StepCount)|wallHit(?:Count|s)?|openSkyMiss(?:Count|s)?|materialHit(?:Count|s)?).*(?:NaN|Infinity)"
            proofMarkerPresent = Test-Regex $log "round10\.(?:proofMarker|focusWindowOnly)=true|Round 10 .*proof marker|Round 10 .*focus-window-only"
            temporaryDirectLightSourcePresent = Test-Regex $log "round10\.temporaryDirectLightSource=true|Round 10 .*temporary direct-light|Round 10 .*current direct-light RGBA payload"
            gpuTraversalOverclaimPresent = Test-Regex $log "realGpuTraversalExecuted=false[^`r`n]*(?:realGpuTraversal(?:Proven|Ready)|gpuTraversalOutputReady)=true|real_gpu_traversal_executed=false[^`r`n]*(?:real_gpu_traversal_(?:proven|ready)|gpu_traversal_output_ready)=true"
            hardwareRtOverclaimPresent = Test-Regex $log "hardwareRtExecutionProven=false[^`r`n]*(?:realHardwareRt(?:Proven|Ready)|rtOutputReady)=true|hardware_rt_execution_proven=false[^`r`n]*(?:real_hardware_rt_(?:proven|ready)|rt_output_ready)=true"
            tracedLightingConsumptionOverclaimPresent = Test-Regex $log "(?:realTracedLightingConsumed|real_traced_lighting_consumed|traced_lighting_consumed|round10\.realTracedLightingConsumed)=true"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            rayCounts = @($rayCounts)
            hitCounts = @($hitCounts)
            missCounts = @($missCounts)
            traversalSteps = @($traversalSteps)
            skippedSections = @($skippedSections)
            wallHits = @($wallHits)
            openSkyMisses = @($openSkyMisses)
            glassWaterMaterialHits = @($glassWaterMaterialHits)
            opaqueMaterialHits = @($opaqueMaterialHits)
            sectionLifecycleCounts = @($sectionLifecycleCounts)
            hybridVoxelHits = @($hybridVoxelHits)
            hybridRtHits = @($hybridRtHits)
            hybridScreenSpaceHits = @($hybridScreenSpaceHits)
            entityMovementCounts = @($entityMovementCounts)
            chunkChurnCounts = @($chunkChurnCounts)
            maxRayCount = Get-MaxNumber $rayCounts
            maxHitCount = Get-MaxNumber $hitCounts
            maxMissCount = Get-MaxNumber $missCounts
            maxTraversalSteps = Get-MaxNumber $traversalSteps
            maxSkippedSections = Get-MaxNumber $skippedSections
            maxWallHits = Get-MaxNumber $wallHits
            maxOpenSkyMisses = Get-MaxNumber $openSkyMisses
            maxGlassWaterMaterialHits = Get-MaxNumber $glassWaterMaterialHits
            maxOpaqueMaterialHits = Get-MaxNumber $opaqueMaterialHits
            maxSectionLifecycleCount = Get-MaxNumber $sectionLifecycleCounts
            maxHybridVoxelHits = Get-MaxNumber $hybridVoxelHits
            maxHybridRtHits = Get-MaxNumber $hybridRtHits
            maxHybridScreenSpaceHits = Get-MaxNumber $hybridScreenSpaceHits
            maxEntityMovementCount = Get-MaxNumber $entityMovementCounts
            maxChunkChurnCount = Get-MaxNumber $chunkChurnCounts
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
            wallHitPatterns = @($WallHitPatterns)
            openSkyMissPatterns = @($OpenSkyMissPatterns)
            glassWaterMaterialHitPatterns = @($GlassWaterMaterialHitPatterns)
            opaqueMaterialHitPatterns = @($OpaqueMaterialHitPatterns)
            materialIdConsistencyPatterns = @($MaterialIdConsistencyPatterns)
            materialLookupReadyPatterns = @($MaterialLookupReadyPatterns)
            maskBitsReadyPatterns = @($MaskBitsReadyPatterns)
            maskBitsSourcePatterns = @($MaskBitsSourcePatterns)
            emptySectionSkipSafePatterns = @($EmptySectionSkipSafePatterns)
            sectionLifecyclePatterns = @($SectionLifecyclePatterns)
            lifecycleShutdownPatterns = @($LifecycleShutdownPatterns)
            traversalBackendPatterns = @($TraversalBackendPatterns)
            realGpuTraversalBoundaryPatterns = @($RealGpuTraversalBoundaryPatterns)
            blasStatusPatterns = @($BlasStatusPatterns)
            tlasStatusPatterns = @($TlasStatusPatterns)
            fallbackStatusPatterns = @($FallbackStatusPatterns)
            hardwareRtExecutionPatterns = @($HardwareRtExecutionPatterns)
            hardwareRtFallbackPatterns = @($HardwareRtFallbackPatterns)
            hybridVoxelHitPatterns = @($HybridVoxelHitPatterns)
            hybridRtHitPatterns = @($HybridRtHitPatterns)
            hybridScreenSpaceHitPatterns = @($HybridScreenSpaceHitPatterns)
            entityMovementPatterns = @($EntityMovementPatterns)
            entityMovementCountPatterns = @($EntityMovementCountPatterns)
            chunkChurnPatterns = @($ChunkChurnPatterns)
            chunkChurnCountPatterns = @($ChunkChurnCountPatterns)
            boundaryLabelPatterns = @($BoundaryLabelPatterns)
            sourceStabilityPatterns = @($SourceStabilityPatterns)
            chunkChurnMaterialConsistencyPatterns = @($ChunkChurnMaterialConsistencyPatterns)
            entityMovementMaterialConsistencyPatterns = @($EntityMovementMaterialConsistencyPatterns)
            realTracedLightingConsumedFalsePatterns = @($RealTracedLightingConsumedFalsePatterns)
            tracedLightingNoOverclaimPatterns = @($TracedLightingNoOverclaimPatterns)
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
    if (-not $logProof.markers.wallHitPresent -or $null -eq $logProof.counts.maxWallHits -or [double]$logProof.counts.maxWallHits -le 0) {
        $failures.Add("Missing nonzero Round 10 known-scene wall hit count marker.")
    }
    if (-not $logProof.markers.openSkyMissPresent -or $null -eq $logProof.counts.maxOpenSkyMisses -or [double]$logProof.counts.maxOpenSkyMisses -le 0) {
        $failures.Add("Missing nonzero Round 10 known-scene open-sky miss count marker.")
    }
    if (-not $logProof.markers.glassWaterMaterialHitPresent -or $null -eq $logProof.counts.maxGlassWaterMaterialHits -or [double]$logProof.counts.maxGlassWaterMaterialHits -le 0) {
        $failures.Add("Missing nonzero Round 10 glass/water material hit count marker.")
    }
    if (-not $logProof.markers.opaqueMaterialHitPresent -or $null -eq $logProof.counts.maxOpaqueMaterialHits -or [double]$logProof.counts.maxOpaqueMaterialHits -le 0) {
        $failures.Add("Missing nonzero Round 10 opaque material hit count marker.")
    }
    if (-not $logProof.markers.materialIdConsistencyPresent) {
        $failures.Add("Missing Round 10 material ID consistency marker.")
    }
    if (-not $logProof.markers.materialLookupReadyPresent) {
        $failures.Add("Missing Round 10 material lookup readiness marker.")
    }
    if (-not $logProof.markers.maskBitsReadyPresent -or -not $logProof.markers.maskBitsSourcePresent) {
        $failures.Add("Missing Round 10 mask-bits readiness/source marker.")
    }
    if (-not $logProof.markers.emptySectionSkipSafePresent) {
        $failures.Add("Missing Round 10 empty-section skip safety marker.")
    }
    if (-not $logProof.markers.sectionLifecyclePresent -or $null -eq $logProof.counts.maxSectionLifecycleCount -or [double]$logProof.counts.maxSectionLifecycleCount -le 0) {
        $failures.Add("Missing nonzero Round 10 section lifecycle stress marker/count.")
    }
    if (-not $logProof.markers.lifecycleShutdownPresent) {
        $failures.Add("Missing Round 10 world-leave or shutdown-safe lifecycle exposure marker.")
    }
    if (-not $logProof.markers.traversalBackendPresent) {
        $failures.Add("Missing Round 10 traversal backend marker.")
    }
    if (-not $logProof.markers.realGpuTraversalBoundaryPresent) {
        $failures.Add("Missing honest Round 10 real GPU traversal false/boundary marker.")
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
    if (-not $logProof.markers.hardwareRtExecutionPresent) {
        $failures.Add("Missing Round 10 hardware RT execution-proven marker.")
    }
    if ($logProof.markers.hardwareRtExecutionProvenFalse -and -not $logProof.markers.hardwareRtFallbackPresent) {
        $failures.Add("Missing Round 10 hardware RT fallback marker for unproven hardware RT execution.")
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
    if (-not $logProof.markers.entityMovementPresent) {
        $failures.Add("Missing Round 10 entity movement scene/control marker.")
    }
    if (-not $logProof.markers.entityMovementCountPresent -or $null -eq $logProof.counts.maxEntityMovementCount -or [double]$logProof.counts.maxEntityMovementCount -le 0) {
        $failures.Add("Missing nonzero Round 10 entity movement stress count marker.")
    }
    if (-not $logProof.markers.chunkChurnPresent) {
        $failures.Add("Missing Round 10 chunk churn scene/control marker.")
    }
    if (-not $logProof.markers.chunkChurnCountPresent -or $null -eq $logProof.counts.maxChunkChurnCount -or [double]$logProof.counts.maxChunkChurnCount -le 0) {
        $failures.Add("Missing nonzero Round 10 chunk churn stress count marker.")
    }
    if (-not $logProof.markers.sourceStabilityPresent) {
        $failures.Add("Missing Round 10 source stability stress marker.")
    }
    if (-not $logProof.markers.chunkChurnMaterialConsistencyPresent) {
        $failures.Add("Missing Round 10 chunk-churn material consistency stress marker.")
    }
    if (-not $logProof.markers.entityMovementMaterialConsistencyPresent) {
        $failures.Add("Missing Round 10 entity-movement material consistency stress marker.")
    }
    if (-not $logProof.markers.realTracedLightingConsumedFalsePresent) {
        $failures.Add("Missing explicit Round 10 realTracedLightingConsumed=false boundary marker.")
    }
    if (-not $logProof.markers.tracedLightingNoOverclaimPresent) {
        $failures.Add("Missing Round 10 traced-lighting no-overclaim marker.")
    }
    if ($logProof.markers.gpuTraversalOverclaimPresent) {
        $failures.Add("Log overclaims real GPU traversal despite realGpuTraversalExecuted=false.")
    }
    if ($logProof.markers.hardwareRtOverclaimPresent) {
        $failures.Add("Log overclaims hardware RT output despite hardwareRtExecutionProven=false.")
    }
    if ($logProof.markers.tracedLightingConsumptionOverclaimPresent) {
        $failures.Add("Log overclaims real traced lighting consumption; Round 10 stress proof requires realTracedLightingConsumed=false.")
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
                wallHitPresent = if ($logProof) { [bool]$logProof.markers.wallHitPresent } else { $null }
                maxWallHits = if ($logProof) { $logProof.counts.maxWallHits } else { $null }
                openSkyMissPresent = if ($logProof) { [bool]$logProof.markers.openSkyMissPresent } else { $null }
                maxOpenSkyMisses = if ($logProof) { $logProof.counts.maxOpenSkyMisses } else { $null }
                emptySectionSkipSafePresent = if ($logProof) { [bool]$logProof.markers.emptySectionSkipSafePresent } else { $null }
                traversalBackendPresent = if ($logProof) { [bool]$logProof.markers.traversalBackendPresent } else { $null }
                realGpuTraversalBoundaryPresent = if ($logProof) { [bool]$logProof.markers.realGpuTraversalBoundaryPresent } else { $null }
            }
            materialCorrectness = [ordered]@{
                glassWaterMaterialHitPresent = if ($logProof) { [bool]$logProof.markers.glassWaterMaterialHitPresent } else { $null }
                maxGlassWaterMaterialHits = if ($logProof) { $logProof.counts.maxGlassWaterMaterialHits } else { $null }
                opaqueMaterialHitPresent = if ($logProof) { [bool]$logProof.markers.opaqueMaterialHitPresent } else { $null }
                maxOpaqueMaterialHits = if ($logProof) { $logProof.counts.maxOpaqueMaterialHits } else { $null }
                materialIdConsistencyPresent = if ($logProof) { [bool]$logProof.markers.materialIdConsistencyPresent } else { $null }
                materialLookupReadyPresent = if ($logProof) { [bool]$logProof.markers.materialLookupReadyPresent } else { $null }
                maskBitsReadyPresent = if ($logProof) { [bool]$logProof.markers.maskBitsReadyPresent } else { $null }
                maskBitsSourcePresent = if ($logProof) { [bool]$logProof.markers.maskBitsSourcePresent } else { $null }
            }
            stressLifecycle = [ordered]@{
                sectionLifecyclePresent = if ($logProof) { [bool]$logProof.markers.sectionLifecyclePresent } else { $null }
                maxSectionLifecycleCount = if ($logProof) { $logProof.counts.maxSectionLifecycleCount } else { $null }
                lifecycleShutdownPresent = if ($logProof) { [bool]$logProof.markers.lifecycleShutdownPresent } else { $null }
                entityMovementPresent = if ($logProof) { [bool]$logProof.markers.entityMovementPresent } else { $null }
                entityMovementCountPresent = if ($logProof) { [bool]$logProof.markers.entityMovementCountPresent } else { $null }
                maxEntityMovementCount = if ($logProof) { $logProof.counts.maxEntityMovementCount } else { $null }
                chunkChurnPresent = if ($logProof) { [bool]$logProof.markers.chunkChurnPresent } else { $null }
                chunkChurnCountPresent = if ($logProof) { [bool]$logProof.markers.chunkChurnCountPresent } else { $null }
                maxChunkChurnCount = if ($logProof) { $logProof.counts.maxChunkChurnCount } else { $null }
            }
            rtEntity = [ordered]@{
                rtEntityDebugOverlayPresent = if ($logProof) { [bool]$logProof.markers.rtEntityDebugOverlayPresent } else { $null }
                blasStatusPresent = if ($logProof) { [bool]$logProof.markers.blasStatusPresent } else { $null }
                tlasStatusPresent = if ($logProof) { [bool]$logProof.markers.tlasStatusPresent } else { $null }
                fallbackStatusPresent = if ($logProof) { [bool]$logProof.markers.fallbackStatusPresent } else { $null }
                hardwareRtExecutionPresent = if ($logProof) { [bool]$logProof.markers.hardwareRtExecutionPresent } else { $null }
                hardwareRtExecutionProvenTrue = if ($logProof) { [bool]$logProof.markers.hardwareRtExecutionProvenTrue } else { $null }
                hardwareRtExecutionProvenFalse = if ($logProof) { [bool]$logProof.markers.hardwareRtExecutionProvenFalse } else { $null }
                hardwareRtFallbackPresent = if ($logProof) { [bool]$logProof.markers.hardwareRtFallbackPresent } else { $null }
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
                sourceStabilityPresent = if ($logProof) { [bool]$logProof.markers.sourceStabilityPresent } else { $null }
                chunkChurnMaterialConsistencyPresent = if ($logProof) { [bool]$logProof.markers.chunkChurnMaterialConsistencyPresent } else { $null }
                entityMovementMaterialConsistencyPresent = if ($logProof) { [bool]$logProof.markers.entityMovementMaterialConsistencyPresent } else { $null }
                realTracedLightingConsumedFalsePresent = if ($logProof) { [bool]$logProof.markers.realTracedLightingConsumedFalsePresent } else { $null }
                tracedLightingNoOverclaimPresent = if ($logProof) { [bool]$logProof.markers.tracedLightingNoOverclaimPresent } else { $null }
                classification = "round10_overlay_and_telemetry_scaffold_not_physical_quality_claim"
            }
            rejectionMarkers = [ordered]@{
                invalidTracingValuesPresent = if ($logProof) { [bool]$logProof.markers.invalidTracingValuesPresent } else { $null }
                temporaryDirectLightSourcePresent = if ($logProof) { [bool]$logProof.markers.temporaryDirectLightSourcePresent } else { $null }
                proofMarkerPresent = if ($logProof) { [bool]$logProof.markers.proofMarkerPresent } else { $null }
                gpuTraversalOverclaimPresent = if ($logProof) { [bool]$logProof.markers.gpuTraversalOverclaimPresent } else { $null }
                hardwareRtOverclaimPresent = if ($logProof) { [bool]$logProof.markers.hardwareRtOverclaimPresent } else { $null }
                tracedLightingConsumptionOverclaimPresent = if ($logProof) { [bool]$logProof.markers.tracedLightingConsumptionOverclaimPresent } else { $null }
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
    Write-Host "wallHitPresent=$($logProof.markers.wallHitPresent)"
    Write-Host "maxWallHits=$($logProof.counts.maxWallHits)"
    Write-Host "openSkyMissPresent=$($logProof.markers.openSkyMissPresent)"
    Write-Host "maxOpenSkyMisses=$($logProof.counts.maxOpenSkyMisses)"
    Write-Host "glassWaterMaterialHitPresent=$($logProof.markers.glassWaterMaterialHitPresent)"
    Write-Host "maxGlassWaterMaterialHits=$($logProof.counts.maxGlassWaterMaterialHits)"
    Write-Host "opaqueMaterialHitPresent=$($logProof.markers.opaqueMaterialHitPresent)"
    Write-Host "maxOpaqueMaterialHits=$($logProof.counts.maxOpaqueMaterialHits)"
    Write-Host "materialIdConsistencyPresent=$($logProof.markers.materialIdConsistencyPresent)"
    Write-Host "materialLookupReadyPresent=$($logProof.markers.materialLookupReadyPresent)"
    Write-Host "maskBitsReadyPresent=$($logProof.markers.maskBitsReadyPresent)"
    Write-Host "maskBitsSourcePresent=$($logProof.markers.maskBitsSourcePresent)"
    Write-Host "emptySectionSkipSafePresent=$($logProof.markers.emptySectionSkipSafePresent)"
    Write-Host "sectionLifecyclePresent=$($logProof.markers.sectionLifecyclePresent)"
    Write-Host "maxSectionLifecycleCount=$($logProof.counts.maxSectionLifecycleCount)"
    Write-Host "lifecycleShutdownPresent=$($logProof.markers.lifecycleShutdownPresent)"
    Write-Host "traversalBackendPresent=$($logProof.markers.traversalBackendPresent)"
    Write-Host "realGpuTraversalBoundaryPresent=$($logProof.markers.realGpuTraversalBoundaryPresent)"
    Write-Host "blasStatusPresent=$($logProof.markers.blasStatusPresent)"
    Write-Host "tlasStatusPresent=$($logProof.markers.tlasStatusPresent)"
    Write-Host "fallbackStatusPresent=$($logProof.markers.fallbackStatusPresent)"
    Write-Host "hardwareRtExecutionPresent=$($logProof.markers.hardwareRtExecutionPresent)"
    Write-Host "hardwareRtExecutionProvenTrue=$($logProof.markers.hardwareRtExecutionProvenTrue)"
    Write-Host "hardwareRtExecutionProvenFalse=$($logProof.markers.hardwareRtExecutionProvenFalse)"
    Write-Host "hardwareRtFallbackPresent=$($logProof.markers.hardwareRtFallbackPresent)"
    Write-Host "hybridVoxelHitPresent=$($logProof.markers.hybridVoxelHitPresent)"
    Write-Host "hybridRtHitPresent=$($logProof.markers.hybridRtHitPresent)"
    Write-Host "hybridScreenSpaceHitPresent=$($logProof.markers.hybridScreenSpaceHitPresent)"
    Write-Host "entityMovementPresent=$($logProof.markers.entityMovementPresent)"
    Write-Host "entityMovementCountPresent=$($logProof.markers.entityMovementCountPresent)"
    Write-Host "maxEntityMovementCount=$($logProof.counts.maxEntityMovementCount)"
    Write-Host "chunkChurnPresent=$($logProof.markers.chunkChurnPresent)"
    Write-Host "chunkChurnCountPresent=$($logProof.markers.chunkChurnCountPresent)"
    Write-Host "maxChunkChurnCount=$($logProof.counts.maxChunkChurnCount)"
    Write-Host "boundaryLabelPresent=$($logProof.markers.boundaryLabelPresent)"
    Write-Host "sourceStabilityPresent=$($logProof.markers.sourceStabilityPresent)"
    Write-Host "chunkChurnMaterialConsistencyPresent=$($logProof.markers.chunkChurnMaterialConsistencyPresent)"
    Write-Host "entityMovementMaterialConsistencyPresent=$($logProof.markers.entityMovementMaterialConsistencyPresent)"
    Write-Host "realTracedLightingConsumedFalsePresent=$($logProof.markers.realTracedLightingConsumedFalsePresent)"
    Write-Host "tracedLightingNoOverclaimPresent=$($logProof.markers.tracedLightingNoOverclaimPresent)"
    Write-Host "invalidTracingValuesPresent=$($logProof.markers.invalidTracingValuesPresent)"
    Write-Host "gpuTraversalOverclaimPresent=$($logProof.markers.gpuTraversalOverclaimPresent)"
    Write-Host "hardwareRtOverclaimPresent=$($logProof.markers.hardwareRtOverclaimPresent)"
    Write-Host "tracedLightingConsumptionOverclaimPresent=$($logProof.markers.tracedLightingConsumptionOverclaimPresent)"
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
