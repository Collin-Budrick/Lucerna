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

    [int] $BrightPixelThreshold = 6,

    [ValidateRange(1, 256)]
    [int] $TileColumns = 16,

    [ValidateRange(1, 256)]
    [int] $TileRows = 9,

    [ValidateRange(0.0, 100.0)]
    [double] $ActiveTileChangedPercentThreshold = 0.25,

    [ValidateRange(0.0, 100.0)]
    [double] $WashoutBoundingBoxAreaPercentThreshold = 85.0,

    [ValidateRange(0.0, 100.0)]
    [double] $WashoutActiveTilePercentThreshold = 75.0,

    [ValidateRange(0.0, 100.0)]
    [double] $WashoutEdgeActiveTilePercentThreshold = 20.0
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

    return @{
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
        minChangedX = [int]::MaxValue
        minChangedY = [int]::MaxValue
        maxChangedX = -1
        maxChangedY = -1
    }
}

function Get-PixelDeltaFacts {
    param(
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

    return [ordered]@{
        absR = $absR
        absG = $absG
        absB = $absB
        deltaLuma = $deltaLuma
        absLuma = $absLuma
        maxChannel = $maxChannel
        changed = ($maxChannel -ge $ChangedPixelThreshold)
        brighter = ($deltaLuma -ge $BrightPixelThreshold)
        darker = ($deltaLuma -le -$BrightPixelThreshold)
    }
}

function Add-PixelDelta {
    param(
        [System.Collections.IDictionary] $Accumulator,
        [System.Collections.IDictionary] $Facts,
        [int] $X,
        [int] $Y
    )

    $Accumulator.pixelCount++
    $Accumulator.sumAbsR += [double]$Facts.absR
    $Accumulator.sumAbsG += [double]$Facts.absG
    $Accumulator.sumAbsB += [double]$Facts.absB
    $Accumulator.sumAbsLuma += [double]$Facts.absLuma
    $Accumulator.sumSignedLuma += [double]$Facts.deltaLuma
    $Accumulator.sumSquaredLuma += ([double]$Facts.deltaLuma * [double]$Facts.deltaLuma)
    $Accumulator.maxAbsLuma = [Math]::Max($Accumulator.maxAbsLuma, [double]$Facts.absLuma)
    $Accumulator.maxAbsChannel = [Math]::Max($Accumulator.maxAbsChannel, [int]$Facts.maxChannel)

    if ([bool]$Facts.changed) {
        $Accumulator.changedPixels++
        $Accumulator.minChangedX = [Math]::Min([int]$Accumulator.minChangedX, $X)
        $Accumulator.minChangedY = [Math]::Min([int]$Accumulator.minChangedY, $Y)
        $Accumulator.maxChangedX = [Math]::Max([int]$Accumulator.maxChangedX, $X)
        $Accumulator.maxChangedY = [Math]::Max([int]$Accumulator.maxChangedY, $Y)
    }
    if ([bool]$Facts.brighter) {
        $Accumulator.brighterPixels++
    } elseif ([bool]$Facts.darker) {
        $Accumulator.darkerPixels++
    }
}

function Complete-Metrics {
    param([System.Collections.IDictionary] $Accumulator)

    if ($Accumulator.pixelCount -le 0) {
        throw "Cannot complete diagnostics for an empty image region."
    }

    $count = [double]$Accumulator.pixelCount
    $changedBoundingBox = $null
    if ($Accumulator.changedPixels -gt 0) {
        $changedBoundingBox = [ordered]@{
            left = [int]$Accumulator.minChangedX
            top = [int]$Accumulator.minChangedY
            rightInclusive = [int]$Accumulator.maxChangedX
            bottomInclusive = [int]$Accumulator.maxChangedY
            width = ([int]$Accumulator.maxChangedX - [int]$Accumulator.minChangedX + 1)
            height = ([int]$Accumulator.maxChangedY - [int]$Accumulator.minChangedY + 1)
        }
    }

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
        changedBoundingBox = $changedBoundingBox
    }
}

function Complete-TileMetrics {
    param(
        [object[]] $TileAccumulators,
        [System.Collections.IDictionary] $Region,
        [int] $EffectiveTileColumns,
        [int] $EffectiveTileRows
    )

    $activeTiles = 0
    $sumChangedPercent = 0.0
    $sumSquaredChangedPercent = 0.0
    $maxChangedPercent = 0.0
    $edgeActiveTiles = 0
    $tileResults = New-Object System.Collections.Generic.List[object]

    for ($index = 0; $index -lt $TileAccumulators.Count; $index++) {
        $tile = $TileAccumulators[$index]
        $metrics = Complete-Metrics $tile.accumulator
        $changedPercent = [double]$metrics.changedPixelPercent
        $active = $changedPercent -ge $ActiveTileChangedPercentThreshold
        if ($active) {
            $activeTiles++
            if ($tile.column -eq 0 -or $tile.row -eq 0 -or $tile.column -eq ($EffectiveTileColumns - 1) -or $tile.row -eq ($EffectiveTileRows - 1)) {
                $edgeActiveTiles++
            }
        }
        $sumChangedPercent += $changedPercent
        $sumSquaredChangedPercent += ($changedPercent * $changedPercent)
        $maxChangedPercent = [Math]::Max($maxChangedPercent, $changedPercent)
        $tileResults.Add([ordered]@{
            row = $tile.row
            column = $tile.column
            region = $tile.region
            active = $active
            metrics = $metrics
        }) | Out-Null
    }

    $tileCount = [double]$TileAccumulators.Count
    $mean = $sumChangedPercent / $tileCount
    $variance = [Math]::Max(0.0, ($sumSquaredChangedPercent / $tileCount) - ($mean * $mean))
    $stdDev = [Math]::Sqrt($variance)
    $activeTilePercent = 100.0 * [double]$activeTiles / $tileCount
    $edgeActiveTilePercent = 100.0 * [double]$edgeActiveTiles / $tileCount
    $coefficientOfVariation = $null
    if ($mean -gt 0.000001) {
        $coefficientOfVariation = [Math]::Round($stdDev / $mean, 4)
    }

    return [ordered]@{
        tileColumns = $EffectiveTileColumns
        tileRows = $EffectiveTileRows
        tileCount = [int]$TileAccumulators.Count
        activeTileChangedPercentThreshold = $ActiveTileChangedPercentThreshold
        activeTiles = $activeTiles
        activeTilePercent = [Math]::Round($activeTilePercent, 4)
        edgeActiveTiles = $edgeActiveTiles
        edgeActiveTilePercent = [Math]::Round($edgeActiveTilePercent, 4)
        meanTileChangedPixelPercent = [Math]::Round($mean, 4)
        stdDevTileChangedPixelPercent = [Math]::Round($stdDev, 4)
        coefficientOfVariation = $coefficientOfVariation
        maxTileChangedPixelPercent = [Math]::Round($maxChangedPercent, 4)
        tiles = $tileResults.ToArray()
    }
}

function Measure-RegionDelta {
    param(
        [System.Drawing.Bitmap] $BaselineImage,
        [System.Drawing.Bitmap] $EnabledImage,
        [System.Collections.IDictionary] $Region
    )

    $accumulator = New-Accumulator
    $tileAccumulators = New-Object System.Collections.Generic.List[object]
    $effectiveTileColumns = [Math]::Max(1, [Math]::Min($TileColumns, [int]$Region.width))
    $effectiveTileRows = [Math]::Max(1, [Math]::Min($TileRows, [int]$Region.height))
    for ($row = 0; $row -lt $effectiveTileRows; $row++) {
        for ($column = 0; $column -lt $effectiveTileColumns; $column++) {
            $tileLeft = [int]($Region.left + [Math]::Floor([double]$Region.width * [double]$column / [double]$effectiveTileColumns))
            $tileTop = [int]($Region.top + [Math]::Floor([double]$Region.height * [double]$row / [double]$effectiveTileRows))
            $tileRight = [int]($Region.left + [Math]::Floor([double]$Region.width * [double]($column + 1) / [double]$effectiveTileColumns))
            $tileBottom = [int]($Region.top + [Math]::Floor([double]$Region.height * [double]($row + 1) / [double]$effectiveTileRows))
            $tileAccumulators.Add([ordered]@{
                row = $row
                column = $column
                region = [ordered]@{
                    left = $tileLeft
                    top = $tileTop
                    width = [Math]::Max(1, $tileRight - $tileLeft)
                    height = [Math]::Max(1, $tileBottom - $tileTop)
                    rightExclusive = [Math]::Max($tileLeft + 1, $tileRight)
                    bottomExclusive = [Math]::Max($tileTop + 1, $tileBottom)
                }
                accumulator = New-Accumulator
            }) | Out-Null
        }
    }

    for ($y = [int]$Region.top; $y -lt [int]$Region.bottomExclusive; $y++) {
        for ($x = [int]$Region.left; $x -lt [int]$Region.rightExclusive; $x++) {
            $facts = Get-PixelDeltaFacts $BaselineImage.GetPixel($x, $y) $EnabledImage.GetPixel($x, $y)
            Add-PixelDelta $accumulator $facts $x $y
            $relativeX = [Math]::Max(0, [Math]::Min([int]$Region.width - 1, $x - [int]$Region.left))
            $relativeY = [Math]::Max(0, [Math]::Min([int]$Region.height - 1, $y - [int]$Region.top))
            $tileColumn = [Math]::Min($effectiveTileColumns - 1, [int][Math]::Floor([double]$relativeX * [double]$effectiveTileColumns / [double]$Region.width))
            $tileRow = [Math]::Min($effectiveTileRows - 1, [int][Math]::Floor([double]$relativeY * [double]$effectiveTileRows / [double]$Region.height))
            $tileIndex = ($tileRow * $effectiveTileColumns) + $tileColumn
            $tileAccumulator = $tileAccumulators[$tileIndex].accumulator
            Add-PixelDelta $tileAccumulator $facts $x $y
        }
    }

    $metrics = Complete-Metrics $accumulator
    $boundingBoxAreaPercent = 0.0
    $changedDensityInsideBoundingBox = 0.0
    if ($metrics.changedBoundingBox) {
        $boundingBoxArea = [double]$metrics.changedBoundingBox.width * [double]$metrics.changedBoundingBox.height
        $boundingBoxAreaPercent = 100.0 * $boundingBoxArea / ([double]$Region.width * [double]$Region.height)
        $changedDensityInsideBoundingBox = 100.0 * [double]$metrics.changedPixels / $boundingBoxArea
    }

    return [ordered]@{
        region = $Region
        metrics = $metrics
        shape = [ordered]@{
            changedBoundingBoxAreaPercent = [Math]::Round($boundingBoxAreaPercent, 4)
            changedDensityInsideBoundingBoxPercent = [Math]::Round($changedDensityInsideBoundingBox, 4)
            tileMetrics = Complete-TileMetrics $tileAccumulators.ToArray() $Region $effectiveTileColumns $effectiveTileRows
        }
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
                tileColumns = $TileColumns
                tileRows = $TileRows
                activeTileChangedPercentThreshold = $ActiveTileChangedPercentThreshold
                washoutBoundingBoxAreaPercentThreshold = $WashoutBoundingBoxAreaPercentThreshold
                washoutActiveTilePercentThreshold = $WashoutActiveTilePercentThreshold
                washoutEdgeActiveTilePercentThreshold = $WashoutEdgeActiveTilePercentThreshold
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
                fullScreenOrRectangularWashoutSuspect = (
                    ([double]$regionResults["fullImage"].shape.changedBoundingBoxAreaPercent -ge $WashoutBoundingBoxAreaPercentThreshold) -and
                    ([double]$regionResults["fullImage"].shape.tileMetrics.activeTilePercent -ge $WashoutActiveTilePercentThreshold) -and
                    ([double]$regionResults["fullImage"].shape.tileMetrics.edgeActiveTilePercent -ge $WashoutEdgeActiveTilePercentThreshold)
                )
                localizedSceneShapedDeltaPresent = (
                    ($fixedChangedPixels -gt 0) -and
                    ([double]$regionResults["fullImage"].shape.changedBoundingBoxAreaPercent -lt $WashoutBoundingBoxAreaPercentThreshold) -and
                    ([double]$regionResults["fullImage"].shape.tileMetrics.activeTilePercent -lt $WashoutActiveTilePercentThreshold)
                )
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
Write-Host "full.changedBoundingBoxAreaPercent=$($result.regions["fullImage"].shape.changedBoundingBoxAreaPercent)"
Write-Host "full.activeTilePercent=$($result.regions["fullImage"].shape.tileMetrics.activeTilePercent)"
Write-Host "full.edgeActiveTilePercent=$($result.regions["fullImage"].shape.tileMetrics.edgeActiveTilePercent)"
Write-Host "fixed.changedPixelPercent=$($result.regions["fixedWorldSurfaceCrop"].metrics.changedPixelPercent)"
Write-Host "fixed.brighterPixelPercent=$($result.regions["fixedWorldSurfaceCrop"].metrics.brighterPixelPercent)"
Write-Host "fixed.changedBoundingBoxAreaPercent=$($result.regions["fixedWorldSurfaceCrop"].shape.changedBoundingBoxAreaPercent)"
Write-Host "fixed.activeTilePercent=$($result.regions["fixedWorldSurfaceCrop"].shape.tileMetrics.activeTilePercent)"
if ($IncludeBands) {
    Write-Host "top.changedPixelPercent=$($result.regions["topBand"].metrics.changedPixelPercent)"
    Write-Host "middle.changedPixelPercent=$($result.regions["middleBand"].metrics.changedPixelPercent)"
    Write-Host "bottom.changedPixelPercent=$($result.regions["bottomBand"].metrics.changedPixelPercent)"
}
Write-Host "classification.anyScreenRegionChangedAboveThreshold=$($result.classification.anyScreenRegionChangedAboveThreshold)"
Write-Host "classification.changedOutsideFixedWorldSurfaceCrop=$($result.classification.changedOutsideFixedWorldSurfaceCrop)"
Write-Host "classification.fullScreenOrRectangularWashoutSuspect=$($result.classification.fullScreenOrRectangularWashoutSuspect)"
Write-Host "classification.localizedSceneShapedDeltaPresent=$($result.classification.localizedSceneShapedDeltaPresent)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
