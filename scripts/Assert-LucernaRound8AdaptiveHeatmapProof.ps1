<#
.SYNOPSIS
Controller-only Round 8 assertion helper for adaptive ray-budget and history-confidence heatmap evidence.

.DESCRIPTION
This script checks already captured screenshots and optional controller launch logs. It does not
launch Minecraft, run Gradle, compile shaders, build native code, or create validation evidence by
itself. Use it after the controller has captured stable, moved/noisy, emissive, and history-confidence
Round 8 heatmap artifacts.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $StableRayBudgetHeatmapImagePath,

    [Parameter(Mandatory = $true)]
    [string] $MovedRayBudgetHeatmapImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EmissiveRayBudgetHeatmapImagePath,

    [Parameter(Mandatory = $true)]
    [string] $StableHistoryConfidenceImagePath,

    [Parameter(Mandatory = $true)]
    [string] $MovedHistoryConfidenceImagePath,

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

    [double] $MinMovedRayBudgetChangedPixelPercent = 0.5,

    [double] $MinEmissiveRayBudgetChangedPixelPercent = 0.5,

    [double] $MinHistoryConfidenceChangedPixelPercent = 0.5,

    [double] $MinHistoryConfidenceMeanAbsLuma = 0.25,

    [string[]] $RayBudgetMarkerPatterns = @(
        "Lucerna Round 8 adaptive ray budget",
        "round8\.adaptive\.rayBudget",
        "round8\.adaptiveSampling=.*Adaptive sampling:",
        "adaptiveRayBudget(?:Enabled)?=true",
        "adaptive_ray_budget(?:_enabled)?=true"
    ),

    [string[]] $RayBudgetBucketPatterns = @(
        "Lucerna Round 8 adaptive ray budget buckets: .*reuse(?:Only)?=[0-9]+.*low=[0-9]+.*medium=[0-9]+.*high=[0-9]+",
        "rayBudgetBuckets=.*reuse(?:Only)?=[0-9]+.*low=[0-9]+.*medium=[0-9]+.*high=[0-9]+",
        "round8\.rayBudgetBuckets=.*reuseOnly=.*low=.*medium=.*high=",
        "ray_budget_buckets=.*reuse(?:Only)?=[0-9]+.*low=[0-9]+.*medium=[0-9]+.*high=[0-9]+",
        "budget bucket counts"
    ),

    [string[]] $SceneStatePatterns = @(
        "sceneState=(?:stable|moved|noisy|moved-noisy|emissive|disoccluded|moved-disoccluded)",
        "round8\.sceneState=.*sceneState: (?:stable|moved|noisy|moved-noisy|emissive|disoccluded|moved-disoccluded)",
        "Round 8 sceneState: (?:stable|moved|noisy|moved-noisy|emissive|disoccluded|moved-disoccluded)"
    ),

    [string[]] $StableLowBudgetPatterns = @(
        "sceneState=stable.*(?:reuse(?:Only)?|low)=[1-9][0-9]*",
        "stable.*(?:reuse(?:Only)?|low)(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*",
        "(?:reuse(?:Only)?|low)(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*.*sceneState=stable",
        "round8\.rayBudgetBuckets=.*(?:reuseOnly|low)=[1-9][0-9]*"
    ),

    [string[]] $MovedHighBudgetPatterns = @(
        "sceneState=(?:moved|noisy|moved-noisy|disoccluded).*high(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*",
        "(?:moved|noisy|disoccluded).*high(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*",
        "high(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*.*sceneState=(?:moved|noisy|moved-noisy|disoccluded)",
        "round8\.rayBudgetBuckets=.*high=[1-9][0-9]*"
    ),

    [string[]] $EmissiveHighBudgetPatterns = @(
        "sceneState=emissive.*high(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*",
        "emissive(?:Contribution|Proximity|Regions|Cells|Tiles)?=[1-9][0-9]*",
        "high(?:Regions|Tiles|Cells|Count|Buckets)?=[1-9][0-9]*.*emissive",
        "round8\.sceneState=.*sceneState: emissive"
    ),

    [string[]] $CacheConfidenceContributionPatterns = @(
        "cacheConfidenceContribution=",
        "cache_confidence_contribution=",
        "round8\.cacheConfidenceContribution=.*(?:value|cacheConfidence)=",
        "cache confidence contribution",
        "confidence=.*variance=.*rayBudget"
    ),

    [string[]] $DispatchCountPatterns = @(
        "round8\.dispatch(?:Count|Counts|Rays)?=([0-9]+)",
        "round8\.ray(?:Count|BudgetRays)=([0-9]+)",
        "sceneState=(?:stable|moved|noisy|moved-noisy|emissive|disoccluded|moved-disoccluded).*?(?:dispatch(?:Count|Counts|Rays)|cappedRays|rays)=([0-9]+)",
        "(?:dispatch(?:Count|Counts|Rays)|cappedRays|rays)=([0-9]+).*?sceneState=(?:stable|moved|noisy|moved-noisy|emissive|disoccluded|moved-disoccluded)"
    ),

    [string[]] $DispatchCountsChangedPatterns = @(
        "dispatchCountsChanged=true",
        "round8\.dispatchCountsChanged=true",
        "adaptive ray budget dispatch counts changed"
    ),

    [string[]] $RayBudgetHeatmapPatterns = @(
        "Lucerna Round 8 ray-budget heatmap: .*artifactRole=",
        "round8\.rayBudgetHeatmap=.*role=(?:ray-budget|ray-budget-[a-z-]+)",
        "round8\.heatmapRoles=.*rayBudget=(?:ray-budget|ray-budget-[a-z-]+)",
        "rayBudgetHeatmap(?:Submitted|Visible|Artifact)=true",
        "debug heatmap.*ray budget",
        "heatmapArtifact=.*ray-budget"
    ),

    [string[]] $HistoryConfidenceMarkerPatterns = @(
        "Lucerna Round 8 history confidence",
        "round8\.historyConfidence",
        "round8\.historyConfidenceHeatmap=.*History-confidence heatmap:",
        "historyConfidence(?:Available|Enabled)?=true",
        "history_confidence(?:_available|_enabled)?=true"
    ),

    [string[]] $HistoryAcceptedPatterns = @(
        "historyAccepted=[1-9][0-9]*",
        "history_accepted=[1-9][0-9]*",
        "round8\.historyCounts=.*historyAccepted=[1-9][0-9]*",
        "acceptedHistory(?:Pixels|Count)?=[1-9][0-9]*"
    ),

    [string[]] $HistoryRejectedPatterns = @(
        "historyRejected=[1-9][0-9]*",
        "history_rejected=[1-9][0-9]*",
        "round8\.historyCounts=.*historyRejected=[1-9][0-9]*",
        "rejectedHistory(?:Pixels|Count)?=[1-9][0-9]*",
        "disocclusion(?:Rejected|Rejects)=[1-9][0-9]*"
    ),

    [string[]] $HistoryStablePatterns = @(
        "sceneState=stable.*historyAccepted=[1-9][0-9]*",
        "stable.*history(?:Confidence|Accepted)",
        "stationary surfaces gain confidence"
    ),

    [string[]] $HistoryMovedPatterns = @(
        "sceneState=(?:moved|disoccluded|moved-disoccluded).*historyRejected=[1-9][0-9]*",
        "(?:moved|disoccluded).*history(?:Rejected|Confidence)",
        "newly visible surfaces lose confidence"
    ),

    [string[]] $HistoryConfidenceHeatmapPatterns = @(
        "Lucerna Round 8 history-confidence heatmap: .*artifactRole=",
        "round8\.historyConfidenceHeatmap=.*role=(?:history-confidence|history-confidence-[a-z-]+)",
        "round8\.heatmapRoles=.*historyConfidence=(?:history-confidence|history-confidence-[a-z-]+)",
        "historyConfidenceHeatmap(?:Submitted|Visible|Artifact)=true",
        "debug heatmap.*history confidence",
        "heatmapArtifact=.*history-confidence"
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-round8-$Label-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
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

function Measure-Round8LogProof {
    param([string[]] $ResolvedLogPaths)

    $combined = New-Object System.Text.StringBuilder
    foreach ($path in $ResolvedLogPaths) {
        [void]$combined.AppendLine("### LOG: $path")
        [void]$combined.AppendLine((Get-Content -Raw -LiteralPath $path))
    }
    $log = $combined.ToString()
    $dispatchNumbers = Get-CapturedNumbers $log $DispatchCountPatterns
    $historyAcceptedNumbers = Get-CapturedNumbers $log @("historyAccepted=([0-9]+)", "history_accepted=([0-9]+)", "acceptedHistory(?:Pixels|Count)?=([0-9]+)")
    $historyRejectedNumbers = Get-CapturedNumbers $log @("historyRejected=([0-9]+)", "history_rejected=([0-9]+)", "rejectedHistory(?:Pixels|Count)?=([0-9]+)", "disocclusion(?:Rejected|Rejects)=([0-9]+)")

    $explicitDispatchCountsChanged = Test-AnyRegex $log $DispatchCountsChangedPatterns
    $derivedDispatchCountsChanged = Test-DistinctPositiveNumbers $dispatchNumbers

    return [ordered]@{
        logPaths = @($ResolvedLogPaths)
        markers = [ordered]@{
            rayBudgetMarkerPresent = Test-AnyRegex $log $RayBudgetMarkerPatterns
            rayBudgetBucketCountsPresent = Test-AnyRegex $log $RayBudgetBucketPatterns
            sceneStatePresent = Test-AnyRegex $log $SceneStatePatterns
            stableLowBudgetPresent = Test-AnyRegex $log $StableLowBudgetPatterns
            movedHighBudgetPresent = Test-AnyRegex $log $MovedHighBudgetPatterns
            emissiveHighBudgetPresent = Test-AnyRegex $log $EmissiveHighBudgetPatterns
            cacheConfidenceContributionPresent = Test-AnyRegex $log $CacheConfidenceContributionPatterns
            rayBudgetHeatmapPresent = Test-AnyRegex $log $RayBudgetHeatmapPatterns
            explicitDispatchCountsChanged = $explicitDispatchCountsChanged
            derivedDispatchCountsChanged = $derivedDispatchCountsChanged
            dispatchCountsChanged = $explicitDispatchCountsChanged -or $derivedDispatchCountsChanged
            historyConfidenceMarkerPresent = Test-AnyRegex $log $HistoryConfidenceMarkerPatterns
            historyAcceptedPresent = Test-AnyRegex $log $HistoryAcceptedPatterns
            historyRejectedPresent = Test-AnyRegex $log $HistoryRejectedPatterns
            historyStableMarkerPresent = Test-AnyRegex $log $HistoryStablePatterns
            historyMovedMarkerPresent = Test-AnyRegex $log $HistoryMovedPatterns
            historyConfidenceHeatmapPresent = Test-AnyRegex $log $HistoryConfidenceHeatmapPatterns
            invalidBudgetValuesPresent = Test-Regex $log "invalidRayBudget=true|invalid_budget_values=true|negative ray budget|rayBudget=.*(?:NaN|Infinity)|RAY_BUDGET_.*(?:ERROR|INVALID)"
            proofMarkerPresent = Test-Regex $log "proof marker|R6 GI proof|R7 proof|CPU output proof|focus-window-only"
            temporaryDirectLightSourcePresent = Test-Regex $log "temporarySourceReady=true|temporary direct-light|current direct-light RGBA payload"
            nativeErrorPresent = Test-Regex $log "invalid descriptor|VK_ERROR|Lucerna native error|native error"
        }
        counts = [ordered]@{
            dispatchCounts = @($dispatchNumbers)
            maxDispatchCount = Get-MaxNumber $dispatchNumbers
            maxHistoryAccepted = Get-MaxNumber $historyAcceptedNumbers
            maxHistoryRejected = Get-MaxNumber $historyRejectedNumbers
        }
        patterns = [ordered]@{
            rayBudgetMarkerPatterns = @($RayBudgetMarkerPatterns)
            rayBudgetBucketPatterns = @($RayBudgetBucketPatterns)
            sceneStatePatterns = @($SceneStatePatterns)
            stableLowBudgetPatterns = @($StableLowBudgetPatterns)
            movedHighBudgetPatterns = @($MovedHighBudgetPatterns)
            emissiveHighBudgetPatterns = @($EmissiveHighBudgetPatterns)
            cacheConfidenceContributionPatterns = @($CacheConfidenceContributionPatterns)
            rayBudgetHeatmapPatterns = @($RayBudgetHeatmapPatterns)
            historyConfidenceMarkerPatterns = @($HistoryConfidenceMarkerPatterns)
            historyAcceptedPatterns = @($HistoryAcceptedPatterns)
            historyRejectedPatterns = @($HistoryRejectedPatterns)
            historyStablePatterns = @($HistoryStablePatterns)
            historyMovedPatterns = @($HistoryMovedPatterns)
            historyConfidenceHeatmapPatterns = @($HistoryConfidenceHeatmapPatterns)
        }
    }
}

$stableRayBudgetResolved = Resolve-ExistingFile $StableRayBudgetHeatmapImagePath "Stable ray-budget heatmap image"
$movedRayBudgetResolved = Resolve-ExistingFile $MovedRayBudgetHeatmapImagePath "Moved/noisy ray-budget heatmap image"
$emissiveRayBudgetResolved = Resolve-ExistingFile $EmissiveRayBudgetHeatmapImagePath "Emissive ray-budget heatmap image"
$stableHistoryResolved = Resolve-ExistingFile $StableHistoryConfidenceImagePath "Stable history-confidence heatmap image"
$movedHistoryResolved = Resolve-ExistingFile $MovedHistoryConfidenceImagePath "Moved history-confidence heatmap image"
$logResolved = Resolve-OptionalFiles $LogPath "Log"

$stableRayBudgetDimensions = Get-ImageDimensions $stableRayBudgetResolved
$movedRayBudgetDimensions = Get-ImageDimensions $movedRayBudgetResolved
$emissiveRayBudgetDimensions = Get-ImageDimensions $emissiveRayBudgetResolved
$stableHistoryDimensions = Get-ImageDimensions $stableHistoryResolved
$movedHistoryDimensions = Get-ImageDimensions $movedHistoryResolved

$movedRayBudgetDelta = Invoke-DeltaHelper $stableRayBudgetResolved $movedRayBudgetResolved "ray-budget-moved"
$emissiveRayBudgetDelta = Invoke-DeltaHelper $stableRayBudgetResolved $emissiveRayBudgetResolved "ray-budget-emissive"
$historyConfidenceDelta = Invoke-DeltaHelper $stableHistoryResolved $movedHistoryResolved "history-confidence-moved"

$logProof = if ($logResolved.Count -eq 0) { $null } else { Measure-Round8LogProof $logResolved }
$failures = New-Object System.Collections.Generic.List[string]

foreach ($entry in @(
    @{ label = "moved/noisy ray-budget heatmap"; dimensions = $movedRayBudgetDimensions },
    @{ label = "emissive ray-budget heatmap"; dimensions = $emissiveRayBudgetDimensions },
    @{ label = "stable history-confidence heatmap"; dimensions = $stableHistoryDimensions },
    @{ label = "moved history-confidence heatmap"; dimensions = $movedHistoryDimensions }
)) {
    if (($entry.dimensions.width -ne $stableRayBudgetDimensions.width) -or ($entry.dimensions.height -ne $stableRayBudgetDimensions.height)) {
        $failures.Add("$($entry.label) image dimensions differ from stable ray-budget heatmap. stable=$($stableRayBudgetDimensions.width)x$($stableRayBudgetDimensions.height) actual=$($entry.dimensions.width)x$($entry.dimensions.height)")
    }
}

if ([double]$movedRayBudgetDelta.focusRegionMetrics.changedPixelPercent -lt $MinMovedRayBudgetChangedPixelPercent) {
    $failures.Add("Moved/noisy ray-budget heatmap changed pixels below threshold. actual=$($movedRayBudgetDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinMovedRayBudgetChangedPixelPercent")
}
if ([double]$emissiveRayBudgetDelta.focusRegionMetrics.changedPixelPercent -lt $MinEmissiveRayBudgetChangedPixelPercent) {
    $failures.Add("Emissive ray-budget heatmap changed pixels below threshold. actual=$($emissiveRayBudgetDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinEmissiveRayBudgetChangedPixelPercent")
}
if ([double]$historyConfidenceDelta.focusRegionMetrics.changedPixelPercent -lt $MinHistoryConfidenceChangedPixelPercent) {
    $failures.Add("History-confidence heatmap changed pixels below threshold. actual=$($historyConfidenceDelta.focusRegionMetrics.changedPixelPercent) expected>=$MinHistoryConfidenceChangedPixelPercent")
}
if ([double]$historyConfidenceDelta.focusRegionMetrics.meanAbsLuma -lt $MinHistoryConfidenceMeanAbsLuma) {
    $failures.Add("History-confidence heatmap mean absolute luma below threshold. actual=$($historyConfidenceDelta.focusRegionMetrics.meanAbsLuma) expected>=$MinHistoryConfidenceMeanAbsLuma")
}
if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    if (-not $logProof.markers.rayBudgetMarkerPresent) {
        $failures.Add("Missing Round 8 adaptive ray-budget log marker.")
    }
    if (-not $logProof.markers.rayBudgetBucketCountsPresent) {
        $failures.Add("Missing Round 8 ray-budget bucket count marker.")
    }
    if (-not $logProof.markers.sceneStatePresent) {
        $failures.Add("Missing Round 8 sceneState marker.")
    }
    if (-not $logProof.markers.stableLowBudgetPresent) {
        $failures.Add("Missing Round 8 stable-view low/reuse budget marker.")
    }
    if (-not $logProof.markers.movedHighBudgetPresent) {
        $failures.Add("Missing Round 8 moved/noisy high-budget marker.")
    }
    if (-not $logProof.markers.emissiveHighBudgetPresent) {
        $failures.Add("Missing Round 8 emissive high-budget marker.")
    }
    if (-not $logProof.markers.cacheConfidenceContributionPresent) {
        $failures.Add("Missing Round 8 cache-confidence contribution marker.")
    }
    if (-not $logProof.markers.dispatchCountsChanged) {
        $failures.Add("Missing Round 8 dispatch-count change marker or distinct positive dispatch/ray counts.")
    }
    if (-not $logProof.markers.rayBudgetHeatmapPresent) {
        $failures.Add("Missing Round 8 ray-budget heatmap artifact/render marker.")
    }
    if (-not $logProof.markers.historyConfidenceMarkerPresent) {
        $failures.Add("Missing Round 8 history-confidence log marker.")
    }
    if (-not $logProof.markers.historyAcceptedPresent) {
        $failures.Add("Missing Round 8 history accept count marker.")
    }
    if (-not $logProof.markers.historyRejectedPresent) {
        $failures.Add("Missing Round 8 history reject count marker.")
    }
    if (-not $logProof.markers.historyStableMarkerPresent) {
        $failures.Add("Missing Round 8 stable history-confidence marker.")
    }
    if (-not $logProof.markers.historyMovedMarkerPresent) {
        $failures.Add("Missing Round 8 moved/disoccluded history-confidence marker.")
    }
    if (-not $logProof.markers.historyConfidenceHeatmapPresent) {
        $failures.Add("Missing Round 8 history-confidence heatmap artifact/render marker.")
    }
    if ($logProof.markers.invalidBudgetValuesPresent) {
        $failures.Add("Log contains invalid ray-budget value markers.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light source marker; Round 8 proof must use adaptive heatmap/debug paths.")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker or focus-window-only evidence; Round 8 proof must use requested heatmap artifacts.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$result = [ordered]@{
    heatmapArtifacts = [ordered]@{
        stableRayBudget = [ordered]@{
            path = $stableRayBudgetResolved
            dimensions = $stableRayBudgetDimensions
            role = "stable-ray-budget-heatmap"
        }
        movedRayBudget = [ordered]@{
            path = $movedRayBudgetResolved
            dimensions = $movedRayBudgetDimensions
            role = "moved-noisy-ray-budget-heatmap"
        }
        emissiveRayBudget = [ordered]@{
            path = $emissiveRayBudgetResolved
            dimensions = $emissiveRayBudgetDimensions
            role = "emissive-ray-budget-heatmap"
        }
        stableHistoryConfidence = [ordered]@{
            path = $stableHistoryResolved
            dimensions = $stableHistoryDimensions
            role = "stable-history-confidence-heatmap"
        }
        movedHistoryConfidence = [ordered]@{
            path = $movedHistoryResolved
            dimensions = $movedHistoryDimensions
            role = "moved-disoccluded-history-confidence-heatmap"
        }
    }
    logPaths = @($logResolved)
    thresholds = [ordered]@{
        minMovedRayBudgetChangedPixelPercent = $MinMovedRayBudgetChangedPixelPercent
        minEmissiveRayBudgetChangedPixelPercent = $MinEmissiveRayBudgetChangedPixelPercent
        minHistoryConfidenceChangedPixelPercent = $MinHistoryConfidenceChangedPixelPercent
        minHistoryConfidenceMeanAbsLuma = $MinHistoryConfidenceMeanAbsLuma
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
        stableToMovedRayBudget = $movedRayBudgetDelta
        stableToEmissiveRayBudget = $emissiveRayBudgetDelta
        stableToMovedHistoryConfidence = $historyConfidenceDelta
    }
    selectedFocusRegions = [ordered]@{
        stableToMovedRayBudget = $movedRayBudgetDelta.focusRegion
        stableToEmissiveRayBudget = $emissiveRayBudgetDelta.focusRegion
        stableToMovedHistoryConfidence = $historyConfidenceDelta.focusRegion
    }
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = if ($failures.Count -eq 0) { "round8_adaptive_heatmap_evidence_passed" } else { "round8_adaptive_heatmap_evidence_failed" }
        tracks = [ordered]@{
            rayBudgetStableVsMoved = [ordered]@{
                imageDeltaPresent = ([double]$movedRayBudgetDelta.focusRegionMetrics.changedPixelPercent -ge $MinMovedRayBudgetChangedPixelPercent)
                bucketMarkerPresent = if ($logProof) { [bool]$logProof.markers.rayBudgetBucketCountsPresent } else { $null }
                sceneStatePresent = if ($logProof) { [bool]$logProof.markers.sceneStatePresent } else { $null }
                stableLowBudgetMarkerPresent = if ($logProof) { [bool]$logProof.markers.stableLowBudgetPresent } else { $null }
                movedHighBudgetMarkerPresent = if ($logProof) { [bool]$logProof.markers.movedHighBudgetPresent } else { $null }
            }
            rayBudgetStableVsEmissive = [ordered]@{
                imageDeltaPresent = ([double]$emissiveRayBudgetDelta.focusRegionMetrics.changedPixelPercent -ge $MinEmissiveRayBudgetChangedPixelPercent)
                emissiveHighBudgetMarkerPresent = if ($logProof) { [bool]$logProof.markers.emissiveHighBudgetPresent } else { $null }
                cacheConfidenceContributionPresent = if ($logProof) { [bool]$logProof.markers.cacheConfidenceContributionPresent } else { $null }
            }
            historyConfidenceStableVsMoved = [ordered]@{
                imageDeltaPresent = (
                    ([double]$historyConfidenceDelta.focusRegionMetrics.changedPixelPercent -ge $MinHistoryConfidenceChangedPixelPercent) -and
                    ([double]$historyConfidenceDelta.focusRegionMetrics.meanAbsLuma -ge $MinHistoryConfidenceMeanAbsLuma)
                )
                historyAcceptedPresent = if ($logProof) { [bool]$logProof.markers.historyAcceptedPresent } else { $null }
                historyRejectedPresent = if ($logProof) { [bool]$logProof.markers.historyRejectedPresent } else { $null }
            }
            heatmapArtifacts = [ordered]@{
                rayBudgetHeatmapPresent = if ($logProof) { [bool]$logProof.markers.rayBudgetHeatmapPresent } else { $null }
                historyConfidenceHeatmapPresent = if ($logProof) { [bool]$logProof.markers.historyConfidenceHeatmapPresent } else { $null }
            }
            rejectionMarkers = [ordered]@{
                invalidBudgetValuesPresent = if ($logProof) { [bool]$logProof.markers.invalidBudgetValuesPresent } else { $null }
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

Write-Host "stableRayBudgetHeatmap=$($result.heatmapArtifacts.stableRayBudget.path)"
Write-Host "movedRayBudgetHeatmap=$($result.heatmapArtifacts.movedRayBudget.path)"
Write-Host "emissiveRayBudgetHeatmap=$($result.heatmapArtifacts.emissiveRayBudget.path)"
Write-Host "stableHistoryConfidenceHeatmap=$($result.heatmapArtifacts.stableHistoryConfidence.path)"
Write-Host "movedHistoryConfidenceHeatmap=$($result.heatmapArtifacts.movedHistoryConfidence.path)"
Write-Host "logPaths=$($result.logPaths -join ';')"
Write-Host "focusRegionSelection=$($result.thresholds.focusRegionSelection)"
Write-Host "movedRayBudget.focusRegion=$($movedRayBudgetDelta.focusRegion.left),$($movedRayBudgetDelta.focusRegion.top),$($movedRayBudgetDelta.focusRegion.width),$($movedRayBudgetDelta.focusRegion.height)"
Write-Host "movedRayBudget.focus.changedPixelPercent=$($movedRayBudgetDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "emissiveRayBudget.focusRegion=$($emissiveRayBudgetDelta.focusRegion.left),$($emissiveRayBudgetDelta.focusRegion.top),$($emissiveRayBudgetDelta.focusRegion.width),$($emissiveRayBudgetDelta.focusRegion.height)"
Write-Host "emissiveRayBudget.focus.changedPixelPercent=$($emissiveRayBudgetDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "historyConfidence.focusRegion=$($historyConfidenceDelta.focusRegion.left),$($historyConfidenceDelta.focusRegion.top),$($historyConfidenceDelta.focusRegion.width),$($historyConfidenceDelta.focusRegion.height)"
Write-Host "historyConfidence.focus.changedPixelPercent=$($historyConfidenceDelta.focusRegionMetrics.changedPixelPercent)"
Write-Host "historyConfidence.focus.meanAbsLuma=$($historyConfidenceDelta.focusRegionMetrics.meanAbsLuma)"
if ($logProof) {
    Write-Host "rayBudgetMarkerPresent=$($logProof.markers.rayBudgetMarkerPresent)"
    Write-Host "rayBudgetBucketCountsPresent=$($logProof.markers.rayBudgetBucketCountsPresent)"
    Write-Host "sceneStatePresent=$($logProof.markers.sceneStatePresent)"
    Write-Host "stableLowBudgetPresent=$($logProof.markers.stableLowBudgetPresent)"
    Write-Host "movedHighBudgetPresent=$($logProof.markers.movedHighBudgetPresent)"
    Write-Host "emissiveHighBudgetPresent=$($logProof.markers.emissiveHighBudgetPresent)"
    Write-Host "cacheConfidenceContributionPresent=$($logProof.markers.cacheConfidenceContributionPresent)"
    Write-Host "dispatchCountsChanged=$($logProof.markers.dispatchCountsChanged)"
    Write-Host "rayBudgetHeatmapPresent=$($logProof.markers.rayBudgetHeatmapPresent)"
    Write-Host "historyConfidenceMarkerPresent=$($logProof.markers.historyConfidenceMarkerPresent)"
    Write-Host "historyAcceptedPresent=$($logProof.markers.historyAcceptedPresent)"
    Write-Host "historyRejectedPresent=$($logProof.markers.historyRejectedPresent)"
    Write-Host "historyStableMarkerPresent=$($logProof.markers.historyStableMarkerPresent)"
    Write-Host "historyMovedMarkerPresent=$($logProof.markers.historyMovedMarkerPresent)"
    Write-Host "historyConfidenceHeatmapPresent=$($logProof.markers.historyConfidenceHeatmapPresent)"
    Write-Host "invalidBudgetValuesPresent=$($logProof.markers.invalidBudgetValuesPresent)"
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
    throw "Round 8 adaptive heatmap proof failed: $($failures -join '; ')"
}
