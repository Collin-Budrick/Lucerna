<#
.SYNOPSIS
Controller-only image diagnostics for Lucerna visual proof screenshots.

.DESCRIPTION
Compares already captured screenshots. This helper does not launch Minecraft, run builds,
or create validation evidence. It reports exact SHA-256 identity and pixel-delta metrics for
the full image, a fixed world-surface crop, and optional top/middle/bottom bands.
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EnabledImagePath,

    [string] $OutputJsonPath = "",

    [double] $FixedRegionLeftPercent = 50.0,

    [double] $FixedRegionTopPercent = 22.0,

    [double] $FixedRegionWidthPercent = 36.0,

    [double] $FixedRegionHeightPercent = 38.0,

    [switch] $IncludeBands,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingImage {
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

function Get-RegionRectangle {
    param(
        [int] $Width,
        [int] $Height,
        [double] $LeftPercent,
        [double] $TopPercent,
        [double] $WidthPercent,
        [double] $HeightPercent
    )

    $left = [Math]::Max(0, [Math]::Min($Width - 1, [int][Math]::Round($Width * $LeftPercent / 100.0)))
    $top = [Math]::Max(0, [Math]::Min($Height - 1, [int][Math]::Round($Height * $TopPercent / 100.0)))
    $right = [Math]::Max($left + 1, [Math]::Min($Width, [int][Math]::Round($Width * ($LeftPercent + $WidthPercent) / 100.0)))
    $bottom = [Math]::Max($top + 1, [Math]::Min($Height, [int][Math]::Round($Height * ($TopPercent + $HeightPercent) / 100.0)))

    return [ordered]@{
        left = $left
        top = $top
        width = $right - $left
        height = $bottom - $top
        rightExclusive = $right
        bottomExclusive = $bottom
        leftPercent = [Math]::Round(100.0 * [double]$left / [double]$Width, 4)
        topPercent = [Math]::Round(100.0 * [double]$top / [double]$Height, 4)
        widthPercent = [Math]::Round(100.0 * [double]($right - $left) / [double]$Width, 4)
        heightPercent = [Math]::Round(100.0 * [double]($bottom - $top) / [double]$Height, 4)
    }
}

function New-Accumulator {
    [ordered]@{
        pixelCount = 0L
        changedPixels = 0L
        brighterPixels = 0L
        darkerPixels = 0L
        sumAbsR = 0.0
        sumAbsG = 0.0
        sumAbsB = 0.0
        sumAbsLuma = 0.0
        sumSignedLuma = 0.0
        sumSquaredLuma = 0.0
        maxAbsLuma = 0.0
        maxAbsChannel = 0
    }
}

function Add-PixelDelta {
    param(
        [System.Collections.IDictionary] $Accumulator,
        [System.Drawing.Color] $Baseline,
        [System.Drawing.Color] $Enabled
    )

    $deltaR = [int]$Enabled.R - [int]$Baseline.R
    $deltaG = [int]$Enabled.G - [int]$Baseline.G
    $deltaB = [int]$Enabled.B - [int]$Baseline.B
    $absR = [Math]::Abs($deltaR)
    $absG = [Math]::Abs($deltaG)
    $absB = [Math]::Abs($deltaB)
    $baselineLuma = (0.2126 * [int]$Baseline.R) + (0.7152 * [int]$Baseline.G) + (0.0722 * [int]$Baseline.B)
    $enabledLuma = (0.2126 * [int]$Enabled.R) + (0.7152 * [int]$Enabled.G) + (0.0722 * [int]$Enabled.B)
    $deltaLuma = $enabledLuma - $baselineLuma
    $absLuma = [Math]::Abs($deltaLuma)
    $maxChannel = [Math]::Max($absR, [Math]::Max($absG, $absB))

    $Accumulator.pixelCount++
    $Accumulator.sumAbsR += $absR
    $Accumulator.sumAbsG += $absG
    $Accumulator.sumAbsB += $absB
    $Accumulator.sumAbsLuma += $absLuma
    $Accumulator.sumSignedLuma += $deltaLuma
    $Accumulator.sumSquaredLuma += ($deltaLuma * $deltaLuma)
    $Accumulator.maxAbsLuma = [Math]::Max($Accumulator.maxAbsLuma, $absLuma)
    $Accumulator.maxAbsChannel = [Math]::Max($Accumulator.maxAbsChannel, $maxChannel)

    if ($maxChannel -ge $ChangedPixelThreshold) {
        $Accumulator.changedPixels++
    }
    if ($deltaLuma -ge $BrightPixelThreshold) {
        $Accumulator.brighterPixels++
    } elseif ($deltaLuma -le -$BrightPixelThreshold) {
        $Accumulator.darkerPixels++
    }
}

function Complete-Metrics {
    param([System.Collections.IDictionary] $Accumulator)

    if ($Accumulator.pixelCount -le 0) {
        throw "Cannot complete diagnostics for an empty image region."
    }

    $count = [double]$Accumulator.pixelCount
    return [ordered]@{
        pixelCount = $Accumulator.pixelCount
        meanAbsRgb = [ordered]@{
            r = [Math]::Round($Accumulator.sumAbsR / $count, 4)
            g = [Math]::Round($Accumulator.sumAbsG / $count, 4)
            b = [Math]::Round($Accumulator.sumAbsB / $count, 4)
        }
        meanAbsLuma = [Math]::Round($Accumulator.sumAbsLuma / $count, 4)
        meanSignedLuma = [Math]::Round($Accumulator.sumSignedLuma / $count, 4)
        rmseLuma = [Math]::Round([Math]::Sqrt($Accumulator.sumSquaredLuma / $count), 4)
        maxAbsLuma = [Math]::Round($Accumulator.maxAbsLuma, 4)
        maxAbsChannel = $Accumulator.maxAbsChannel
        changedPixels = $Accumulator.changedPixels
        changedPixelPercent = [Math]::Round(100.0 * $Accumulator.changedPixels / $count, 4)
        brighterPixels = $Accumulator.brighterPixels
        brighterPixelPercent = [Math]::Round(100.0 * $Accumulator.brighterPixels / $count, 4)
        darkerPixels = $Accumulator.darkerPixels
        darkerPixelPercent = [Math]::Round(100.0 * $Accumulator.darkerPixels / $count, 4)
    }
}

function Measure-RegionDelta {
    param(
        [System.Drawing.Bitmap] $BaselineImage,
        [System.Drawing.Bitmap] $EnabledImage,
        [System.Collections.IDictionary] $Region
    )

    $accumulator = New-Accumulator
    for ($y = [int]$Region.top; $y -lt [int]$Region.bottomExclusive; $y++) {
        for ($x = [int]$Region.left; $x -lt [int]$Region.rightExclusive; $x++) {
            Add-PixelDelta $accumulator $BaselineImage.GetPixel($x, $y) $EnabledImage.GetPixel($x, $y)
        }
    }

    return [ordered]@{
        region = $Region
        metrics = Complete-Metrics $accumulator
    }
}

function Get-ImageFileIdentity {
    param([string] $Path)

    $item = Get-Item -LiteralPath $Path
    return [ordered]@{
        path = $Path
        byteLength = $item.Length
        sha256 = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

function Get-LucernaVisualProofImageDiagnostics {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath
    )

    Add-Type -AssemblyName System.Drawing

    $baselineResolved = Resolve-ExistingImage $BaselinePath "Baseline image"
    $enabledResolved = Resolve-ExistingImage $EnabledPath "Enabled image"
    $baselineIdentity = Get-ImageFileIdentity $baselineResolved
    $enabledIdentity = Get-ImageFileIdentity $enabledResolved

    $baselineImage = [System.Drawing.Bitmap]::new($baselineResolved)
    $enabledImage = [System.Drawing.Bitmap]::new($enabledResolved)
    try {
        if ($baselineImage.Width -ne $enabledImage.Width -or $baselineImage.Height -ne $enabledImage.Height) {
            throw "Image dimensions differ. baseline=$($baselineImage.Width)x$($baselineImage.Height) enabled=$($enabledImage.Width)x$($enabledImage.Height)"
        }

        $width = $baselineImage.Width
        $height = $baselineImage.Height
        $regions = [ordered]@{
            fullImage = Get-RegionRectangle $width $height 0.0 0.0 100.0 100.0
            fixedWorldSurfaceCrop = Get-RegionRectangle $width $height $FixedRegionLeftPercent $FixedRegionTopPercent $FixedRegionWidthPercent $FixedRegionHeightPercent
        }
        if ($IncludeBands) {
            $regions["topBand"] = Get-RegionRectangle $width $height 0.0 0.0 100.0 33.3333
            $regions["middleBand"] = Get-RegionRectangle $width $height 0.0 33.3333 100.0 33.3334
            $regions["bottomBand"] = Get-RegionRectangle $width $height 0.0 66.6667 100.0 33.3333
        }

        $regionResults = [ordered]@{}
        foreach ($entry in $regions.GetEnumerator()) {
            $regionResults[$entry.Key] = Measure-RegionDelta $baselineImage $enabledImage $entry.Value
        }

        $hashIdentical = ([string]$baselineIdentity.sha256 -eq [string]$enabledIdentity.sha256)
        $fullChangedPixels = [int64]$regionResults["fullImage"].metrics.changedPixels
        $fixedChangedPixels = [int64]$regionResults["fixedWorldSurfaceCrop"].metrics.changedPixels

        return [ordered]@{
            baselineImage = $baselineResolved
            enabledImage = $enabledResolved
            dimensions = [ordered]@{
                width = $width
                height = $height
            }
            thresholds = [ordered]@{
                changedPixelThreshold = $ChangedPixelThreshold
                brightPixelThreshold = $BrightPixelThreshold
            }
            fileIdentity = [ordered]@{
                baseline = $baselineIdentity
                enabled = $enabledIdentity
                identicalBySha256 = $hashIdentical
                sameByteLength = ([int64]$baselineIdentity.byteLength -eq [int64]$enabledIdentity.byteLength)
            }
            regions = $regionResults
            classification = [ordered]@{
                screenshotsIdenticalByHash = $hashIdentical
                anyScreenRegionChangedAboveThreshold = ($fullChangedPixels -gt 0)
                fixedWorldSurfaceCropChangedAboveThreshold = ($fixedChangedPixels -gt 0)
                changedOutsideFixedWorldSurfaceCrop = (($fullChangedPixels -gt 0) -and ($fixedChangedPixels -eq 0))
                onlyFileEncodingOrMetadataChanged = ((-not $hashIdentical) -and ($fullChangedPixels -eq 0))
                bandDiagnosticsIncluded = [bool]$IncludeBands
            }
        }
    } finally {
        $baselineImage.Dispose()
        $enabledImage.Dispose()
    }
}

$result = Get-LucernaVisualProofImageDiagnostics $BaselineImagePath $EnabledImagePath
$json = $result | ConvertTo-Json -Depth 10
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    $parent = Split-Path -Parent $OutputJsonPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    Set-Content -LiteralPath $OutputJsonPath -Value $json -Encoding UTF8
}

Write-Host "baselineImage=$($result.baselineImage)"
Write-Host "enabledImage=$($result.enabledImage)"
Write-Host "dimensions=$($result.dimensions.width)x$($result.dimensions.height)"
Write-Host "hash.identicalBySha256=$($result.fileIdentity.identicalBySha256)"
Write-Host "hash.baselineSha256=$($result.fileIdentity.baseline.sha256)"
Write-Host "hash.enabledSha256=$($result.fileIdentity.enabled.sha256)"
Write-Host "full.changedPixelPercent=$($result.regions["fullImage"].metrics.changedPixelPercent)"
Write-Host "full.brighterPixelPercent=$($result.regions["fullImage"].metrics.brighterPixelPercent)"
Write-Host "fixed.changedPixelPercent=$($result.regions["fixedWorldSurfaceCrop"].metrics.changedPixelPercent)"
Write-Host "fixed.brighterPixelPercent=$($result.regions["fixedWorldSurfaceCrop"].metrics.brighterPixelPercent)"
if ($IncludeBands) {
    Write-Host "top.changedPixelPercent=$($result.regions["topBand"].metrics.changedPixelPercent)"
    Write-Host "middle.changedPixelPercent=$($result.regions["middleBand"].metrics.changedPixelPercent)"
    Write-Host "bottom.changedPixelPercent=$($result.regions["bottomBand"].metrics.changedPixelPercent)"
}
Write-Host "classification.anyScreenRegionChangedAboveThreshold=$($result.classification.anyScreenRegionChangedAboveThreshold)"
Write-Host "classification.changedOutsideFixedWorldSurfaceCrop=$($result.classification.changedOutsideFixedWorldSurfaceCrop)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
