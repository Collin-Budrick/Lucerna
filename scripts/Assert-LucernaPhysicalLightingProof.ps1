param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EnabledImagePath,

    [Parameter(Mandatory = $true)]
    [string] $DebugImagePath,

    [string[]] $LogPath = @(),

    [string[]] $CaptureManifestJsonPath = @(),

    [string[]] $ScreenshotSource = @(),

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

    [bool] $RequireInClientScreenshotProvenance = $true,

    [switch] $RequireLogProof,

    [ValidateSet("Any", "All")]
    [string] $PhysicalSourcePatternRequirement = "Any",

    [string[]] $PhysicalSourcePatterns = @(
        "(?:Lucerna physical lighting|lucerna\.physicalLighting|physical(?:Source|Lighting).*ready=true)",
        "(?:firstLighting|first-lighting|physicalSurface|physical-surface|surfaceLighting).*?(?:ready|enabled|source|marker)=",
        "(?:PL-A|PL-C|physical-ish|physicalish).*?(?:source|lighting|surface)"
    ),

    [string[]] $RequiredExecutionPatterns = @(
        "(?:Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*|Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*|Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi))",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*mode=(?![^`r`n]*focus-window)[^`r`n]*(?:direct|emissive|physical|gi))(?=[^`r`n]*(?:surface|world|final|composite))"
    ),

    [string[]] $AdditionalRequiredLogPattern = @(),

    [string[]] $OverrideRequiredLogPattern = @(),

    [string[]] $ForbiddenLogPattern = @(
        "temporarySourceReady=true",
        "temporaryDirectLightSubstitution=true",
        "using the current direct-light RGBA payload as the temporary visible source",
        "Lucerna public Mojang final composite: .*metadataOnlyPreview=true",
        "Lucerna Round 6 diffuse GI preview composite: .*metadata-only",
        "physicalLighting.*metadata scaffold",
        "physicalLighting.*no_render_output",
        "Lucerna public Mojang final composite: .*metadata scaffold",
        "Lucerna public Mojang final composite: .*no_render_output",
        "round6-diffuse-gi-focus-window-additive",
        "final-composite-direct-light-focus-window-additive",
        "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true",
        "focusWindowOnly(?:Submitted)?=true",
        "focus_window_only=true",
        "round5-direct-proof",
        "R5 visual proof",
        "round6-gi-proof",
        "R6 GI proof",
        "R7 proof",
        "proofMarkerSource=true",
        "cpuOutputProofMarker=true",
        "invalid descriptor",
        "VK_ERROR",
        "VK_[A-Z_]*ERROR",
        "Lucerna native error",
        "native error",
        "Vulkan error"
    )
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

function Resolve-ExistingFiles {
    param(
        [string[]] $Paths,
        [string] $Label
    )

    $resolved = New-Object System.Collections.Generic.List[string]
    foreach ($path in $Paths) {
        if ([string]::IsNullOrWhiteSpace($path)) {
            continue
        }
        $resolved.Add((Resolve-ExistingFile $path $Label)) | Out-Null
    }
    return @($resolved)
}

function Test-Regex {
    param(
        [string] $Text,
        [string] $Pattern
    )

    return [regex]::IsMatch($Text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
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

    $tempJson = Join-Path ([System.IO.Path]::GetTempPath()) ("lucerna-physical-lighting-delta-{0}.json" -f ([guid]::NewGuid().ToString("N")))
    try {
        $compareArgs = @{
            BaselineImagePath = $BaselinePath
            EnabledImagePath = $EnabledPath
            OutputJsonPath = $tempJson
            RegionLeftPercent = $RegionLeftPercent
            RegionTopPercent = $RegionTopPercent
            RegionWidthPercent = $RegionWidthPercent
            RegionHeightPercent = $RegionHeightPercent
            ChangedPixelThreshold = $ChangedPixelThreshold
            BrightPixelThreshold = $BrightPixelThreshold
        }
        if ($AutoFocusRegion) {
            $compareArgs.AutoFocusRegion = $true
            $compareArgs.AutoRegionSearchLeftPercent = $AutoRegionSearchLeftPercent
            $compareArgs.AutoRegionSearchTopPercent = $AutoRegionSearchTopPercent
            $compareArgs.AutoRegionSearchWidthPercent = $AutoRegionSearchWidthPercent
            $compareArgs.AutoRegionSearchHeightPercent = $AutoRegionSearchHeightPercent
            $compareArgs.AutoRegionColumns = $AutoRegionColumns
            $compareArgs.AutoRegionRows = $AutoRegionRows
            $compareArgs.AutoRegionPaddingCells = $AutoRegionPaddingCells
        }

        & $compareScript @compareArgs | Out-Host
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

function Read-CaptureManifestSources {
    param([string[]] $ManifestPaths)

    $sources = New-Object System.Collections.Generic.List[string]
    foreach ($manifestPath in $ManifestPaths) {
        if ([string]::IsNullOrWhiteSpace($manifestPath)) {
            continue
        }
        $manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
        foreach ($screenshot in @($manifest.screenshots)) {
            if ($screenshot -and -not [string]::IsNullOrWhiteSpace([string]$screenshot.source)) {
                $sources.Add([string]$screenshot.source) | Out-Null
            }
        }
    }
    return @($sources)
}

function Measure-LogProof {
    param([string] $LogText)

    $requiredPatterns = if ($OverrideRequiredLogPattern.Count -gt 0) {
        @($OverrideRequiredLogPattern)
    } else {
        @($RequiredExecutionPatterns) + @($AdditionalRequiredLogPattern)
    }

    $requiredMatches = @()
    foreach ($pattern in $requiredPatterns) {
        $requiredMatches += [ordered]@{
            pattern = $pattern
            present = Test-Regex $LogText $pattern
        }
    }

    $sourceMatches = @()
    foreach ($pattern in $PhysicalSourcePatterns) {
        $sourceMatches += [ordered]@{
            pattern = $pattern
            present = Test-Regex $LogText $pattern
        }
    }

    $forbiddenMatches = @()
    foreach ($pattern in $ForbiddenLogPattern) {
        $present = Test-Regex $LogText $pattern
        if ($present) {
            $forbiddenMatches += [ordered]@{
                pattern = $pattern
                present = $true
            }
        }
    }

    $requiredMissing = @($requiredMatches | Where-Object { -not $_.present })
    $sourcePresentCount = @($sourceMatches | Where-Object { $_.present }).Count
    $sourcePassed = if ($PhysicalSourcePatternRequirement -eq "All") {
        $sourcePresentCount -eq $sourceMatches.Count
    } else {
        $sourceMatches.Count -eq 0 -or $sourcePresentCount -gt 0
    }

    return [ordered]@{
        requiredPatterns = @($requiredMatches)
        missingRequiredPatterns = @($requiredMissing)
        physicalSourcePatterns = @($sourceMatches)
        physicalSourcePatternRequirement = $PhysicalSourcePatternRequirement
        physicalSourcePassed = $sourcePassed
        forbiddenMatches = @($forbiddenMatches)
        markers = [ordered]@{
            proofMarkerPresent = Test-Regex $LogText "round5-direct-proof|R5 visual proof|round6-gi-proof|R6 GI proof|R7 proof|proofMarkerSource=true|cpuOutputProofMarker=true"
            focusWindowOnlyPresent = Test-Regex $LogText "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true|final-composite-direct-light-focus-window-additive|round6-diffuse-gi-focus-window-additive|focusWindowOnly(?:Submitted)?=true|focus_window_only=true"
            temporaryDirectLightSourcePresent = Test-Regex $LogText "temporarySourceReady=true|temporaryDirectLightSubstitution=true|using the current direct-light RGBA payload as the temporary visible source"
            metadataOnlyPresent = Test-Regex $LogText "Lucerna public Mojang final composite: .*metadataOnlyPreview=true|Lucerna Round 6 diffuse GI preview composite: .*metadata-only|physicalLighting.*metadata scaffold|physicalLighting.*no_render_output|Lucerna public Mojang final composite: .*metadata scaffold|Lucerna public Mojang final composite: .*no_render_output"
            nativeErrorPresent = Test-Regex $LogText "invalid descriptor|VK_ERROR|VK_[A-Z_]*ERROR|Lucerna native error|native error|Vulkan error"
        }
    }
}

$baselineResolved = Resolve-ExistingFile $BaselineImagePath "Baseline image"
$enabledResolved = Resolve-ExistingFile $EnabledImagePath "Enabled image"
$debugResolved = Resolve-ExistingFile $DebugImagePath "Debug image"
$logResolved = Resolve-ExistingFiles $LogPath "Log"
$manifestResolved = Resolve-ExistingFiles $CaptureManifestJsonPath "Capture manifest"

$delta = Invoke-DeltaHelper $baselineResolved $enabledResolved
$baselineDimensions = Get-ImageDimensions $baselineResolved
$enabledDimensions = Get-ImageDimensions $enabledResolved
$debugDimensions = Get-ImageDimensions $debugResolved
$manifestSources = Read-CaptureManifestSources $manifestResolved
$allScreenshotSources = @($ScreenshotSource) + @($manifestSources)
$logText = ""
foreach ($path in $logResolved) {
    $logText += "`n" + (Get-Content -Raw -LiteralPath $path)
}
$logProof = if (-not [string]::IsNullOrWhiteSpace($logText)) { Measure-LogProof $logText } else { $null }

$focusMetrics = $delta.focusRegionMetrics
$focusDeltaPassed = (
    ([double]$focusMetrics.changedPixelPercent -ge $MinFocusChangedPixelPercent) -and
    ([double]$focusMetrics.brighterPixelPercent -ge $MinFocusBrighterPixelPercent) -and
    ([double]$focusMetrics.meanSignedLuma -ge $MinFocusMeanSignedLuma)
)

$failures = New-Object System.Collections.Generic.List[string]
if (($enabledDimensions.width -ne $baselineDimensions.width) -or ($enabledDimensions.height -ne $baselineDimensions.height)) {
    $failures.Add("Enabled image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) enabled=$($enabledDimensions.width)x$($enabledDimensions.height)")
}
if (($debugDimensions.width -ne $baselineDimensions.width) -or ($debugDimensions.height -ne $baselineDimensions.height)) {
    $failures.Add("Debug image dimensions differ from baseline. baseline=$($baselineDimensions.width)x$($baselineDimensions.height) debug=$($debugDimensions.width)x$($debugDimensions.height)")
}
if ([double]$focusMetrics.changedPixelPercent -lt $MinFocusChangedPixelPercent) {
    $failures.Add("Focused physical surface changed-pixel percentage below threshold. actual=$($focusMetrics.changedPixelPercent) expected>=$MinFocusChangedPixelPercent")
}
if ([double]$focusMetrics.brighterPixelPercent -lt $MinFocusBrighterPixelPercent) {
    $failures.Add("Focused physical surface brighter-pixel percentage below threshold. actual=$($focusMetrics.brighterPixelPercent) expected>=$MinFocusBrighterPixelPercent")
}
if ([double]$focusMetrics.meanSignedLuma -lt $MinFocusMeanSignedLuma) {
    $failures.Add("Focused physical surface mean signed luma below threshold. actual=$($focusMetrics.meanSignedLuma) expected>=$MinFocusMeanSignedLuma")
}

if ($RequireInClientScreenshotProvenance) {
    if ($allScreenshotSources.Count -eq 0) {
        $failures.Add("In-client screenshot provenance is required, but no -ScreenshotSource or capture manifest source was provided.")
    }
    foreach ($source in $allScreenshotSources) {
        if ([string]::IsNullOrWhiteSpace($source)) {
            continue
        }
        if ($source -ne "minecraft-in-client" -and $source -ne "minecraft-in-client-f2-repeat") {
            $failures.Add("Screenshot source '$source' is rejected for strict physical-lighting proof. Use -ScreenshotSource InClient and pass capture manifests from Invoke-LucernaVisualProof.")
        }
    }
}

if ($RequireLogProof -and $logResolved.Count -eq 0) {
    $failures.Add("Log proof was required but no -LogPath was provided.")
}
if ($logProof) {
    foreach ($missing in @($logProof.missingRequiredPatterns)) {
        $failures.Add("Missing required physical-lighting log pattern: $($missing.pattern)")
    }
    if (-not $logProof.physicalSourcePassed) {
        $failures.Add("Missing physical-ish source marker. requirement=$PhysicalSourcePatternRequirement patternCount=$($PhysicalSourcePatterns.Count)")
    }
    foreach ($forbidden in @($logProof.forbiddenMatches)) {
        $failures.Add("Forbidden physical-lighting log pattern matched: $($forbidden.pattern)")
    }
    if ($logProof.markers.proofMarkerPresent) {
        $failures.Add("Log contains proof-marker evidence; strict physical-lighting proof must use real world-surface pixels.")
    }
    if ($logProof.markers.focusWindowOnlyPresent) {
        $failures.Add("Log contains focus-window-only evidence; strict physical-lighting proof must not use focus-window brightness.")
    }
    if ($logProof.markers.temporaryDirectLightSourcePresent) {
        $failures.Add("Log contains temporary direct-light substitution evidence.")
    }
    if ($logProof.markers.metadataOnlyPresent) {
        $failures.Add("Log contains metadata-only/scaffold evidence.")
    }
    if ($logProof.markers.nativeErrorPresent) {
        $failures.Add("Log contains native/Vulkan error markers.")
    }
}

$classification = if ($logProof -and $logProof.markers.proofMarkerPresent) {
    "proof_marker_contaminated"
} elseif ($logProof -and $logProof.markers.focusWindowOnlyPresent) {
    "focus_window_contaminated"
} elseif ($logProof -and $logProof.markers.temporaryDirectLightSourcePresent) {
    "temporary_direct_source_contaminated"
} elseif ($logProof -and $logProof.markers.metadataOnlyPresent) {
    "metadata_only_contaminated"
} elseif ($focusDeltaPassed -and $logProof -and $logProof.physicalSourcePassed -and $logProof.missingRequiredPatterns.Count -eq 0) {
    "strict_physical_surface_delta_passed"
} elseif ($focusDeltaPassed) {
    "screenshot_delta_passed_without_complete_log_proof"
} else {
    "screenshot_delta_failed"
}

$result = [ordered]@{
    baselineImage = $baselineResolved
    enabledImage = $enabledResolved
    debugImage = $debugResolved
    logPaths = @($logResolved)
    captureManifests = @($manifestResolved)
    screenshotSources = @($allScreenshotSources)
    thresholds = [ordered]@{
        minFocusChangedPixelPercent = $MinFocusChangedPixelPercent
        minFocusBrighterPixelPercent = $MinFocusBrighterPixelPercent
        minFocusMeanSignedLuma = $MinFocusMeanSignedLuma
        changedPixelThreshold = $ChangedPixelThreshold
        brightPixelThreshold = $BrightPixelThreshold
        requireInClientScreenshotProvenance = $RequireInClientScreenshotProvenance
        requireLogProof = [bool]$RequireLogProof
        physicalSourcePatternRequirement = $PhysicalSourcePatternRequirement
    }
    screenshots = [ordered]@{
        baselineDimensions = $baselineDimensions
        enabledDimensions = $enabledDimensions
        debugDimensions = $debugDimensions
    }
    imageDelta = $delta
    logProof = $logProof
    proofClarity = [ordered]@{
        classification = $classification
        focusDeltaPassed = $focusDeltaPassed
        requireInClientScreenshotProvenance = $RequireInClientScreenshotProvenance
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
Write-Host "screenshotSources=$($allScreenshotSources -join ';')"
Write-Host "focusRegion=$($delta.focusRegion.left),$($delta.focusRegion.top),$($delta.focusRegion.width),$($delta.focusRegion.height)"
Write-Host "focus.changedPixelPercent=$($focusMetrics.changedPixelPercent)"
Write-Host "focus.brighterPixelPercent=$($focusMetrics.brighterPixelPercent)"
Write-Host "focus.meanSignedLuma=$($focusMetrics.meanSignedLuma)"
if ($logProof) {
    Write-Host "physicalSourcePassed=$($logProof.physicalSourcePassed)"
    Write-Host "missingRequiredPatternCount=$($logProof.missingRequiredPatterns.Count)"
    Write-Host "forbiddenMatchCount=$($logProof.forbiddenMatches.Count)"
    Write-Host "proofMarkerPresent=$($logProof.markers.proofMarkerPresent)"
    Write-Host "focusWindowOnlyPresent=$($logProof.markers.focusWindowOnlyPresent)"
    Write-Host "temporaryDirectLightSourcePresent=$($logProof.markers.temporaryDirectLightSourcePresent)"
    Write-Host "metadataOnlyPresent=$($logProof.markers.metadataOnlyPresent)"
    Write-Host "nativeErrorPresent=$($logProof.markers.nativeErrorPresent)"
}
Write-Host "proof.classification=$($result.proofClarity.classification)"
Write-Host "passed=$($result.passed)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
if ($failures.Count -gt 0) {
    throw "Strict physical-lighting proof failed: $($failures -join '; ')"
}
