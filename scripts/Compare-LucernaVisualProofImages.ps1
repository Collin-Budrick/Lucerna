param(
    [Parameter(Mandatory = $true)]
    [string] $BaselineImagePath,

    [Parameter(Mandatory = $true)]
    [string] $EnabledImagePath,

    [string] $OutputJsonPath = "",

    [double] $RegionLeftPercent = 30.0,

    [double] $RegionTopPercent = 20.0,

    [double] $RegionWidthPercent = 40.0,

    [double] $RegionHeightPercent = 55.0,

    [int] $ChangedPixelThreshold = 8,

    [int] $BrightPixelThreshold = 6
)

$ErrorActionPreference = "Stop"

function Resolve-ExistingPath {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Image path does not exist: $Path"
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
        [System.Drawing.Color] $Enabled,
        [int] $ChangedThreshold,
        [int] $BrightThreshold
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

    if ($maxChannel -ge $ChangedThreshold) {
        $Accumulator.changedPixels++
    }
    if ($deltaLuma -ge $BrightThreshold) {
        $Accumulator.brighterPixels++
    } elseif ($deltaLuma -le -$BrightThreshold) {
        $Accumulator.darkerPixels++
    }
}

function Complete-Metrics {
    param([System.Collections.IDictionary] $Accumulator)

    if ($Accumulator.pixelCount -le 0) {
        throw "Cannot complete image metrics for an empty region."
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

function Measure-LucernaImageDelta {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath
    )

    Add-Type -AssemblyName System.Drawing

    $baselineResolved = Resolve-ExistingPath $BaselinePath
    $enabledResolved = Resolve-ExistingPath $EnabledPath
    $baselineImage = [System.Drawing.Bitmap]::new($baselineResolved)
    $enabledImage = [System.Drawing.Bitmap]::new($enabledResolved)

    try {
        if ($baselineImage.Width -ne $enabledImage.Width -or $baselineImage.Height -ne $enabledImage.Height) {
            throw "Image dimensions differ. baseline=$($baselineImage.Width)x$($baselineImage.Height) enabled=$($enabledImage.Width)x$($enabledImage.Height)"
        }

        $width = $baselineImage.Width
        $height = $baselineImage.Height
        $region = Get-RegionRectangle $width $height $RegionLeftPercent $RegionTopPercent $RegionWidthPercent $RegionHeightPercent
        $full = New-Accumulator
        $focus = New-Accumulator

        for ($y = 0; $y -lt $height; $y++) {
            for ($x = 0; $x -lt $width; $x++) {
                $baselinePixel = $baselineImage.GetPixel($x, $y)
                $enabledPixel = $enabledImage.GetPixel($x, $y)
                Add-PixelDelta $full $baselinePixel $enabledPixel $ChangedPixelThreshold $BrightPixelThreshold

                if ($x -ge $region.left -and $x -lt $region.rightExclusive -and $y -ge $region.top -and $y -lt $region.bottomExclusive) {
                    Add-PixelDelta $focus $baselinePixel $enabledPixel $ChangedPixelThreshold $BrightPixelThreshold
                }
            }
        }

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
            focusRegion = [ordered]@{
                leftPercent = $RegionLeftPercent
                topPercent = $RegionTopPercent
                widthPercent = $RegionWidthPercent
                heightPercent = $RegionHeightPercent
                left = $region.left
                top = $region.top
                width = $region.width
                height = $region.height
            }
            fullImage = Complete-Metrics $full
            focusRegionMetrics = Complete-Metrics $focus
        }
    } finally {
        $baselineImage.Dispose()
        $enabledImage.Dispose()
    }
}

$result = Measure-LucernaImageDelta $BaselineImagePath $EnabledImagePath
$json = $result | ConvertTo-Json -Depth 8
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
Write-Host "focusRegion=$($result.focusRegion.left),$($result.focusRegion.top),$($result.focusRegion.width),$($result.focusRegion.height)"
Write-Host "full.meanAbsLuma=$($result.fullImage.meanAbsLuma)"
Write-Host "full.meanSignedLuma=$($result.fullImage.meanSignedLuma)"
Write-Host "full.changedPixelPercent=$($result.fullImage.changedPixelPercent)"
Write-Host "full.brighterPixelPercent=$($result.fullImage.brighterPixelPercent)"
Write-Host "focus.meanAbsLuma=$($result.focusRegionMetrics.meanAbsLuma)"
Write-Host "focus.meanSignedLuma=$($result.focusRegionMetrics.meanSignedLuma)"
Write-Host "focus.changedPixelPercent=$($result.focusRegionMetrics.changedPixelPercent)"
Write-Host "focus.brighterPixelPercent=$($result.focusRegionMetrics.brighterPixelPercent)"
if (-not [string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    Write-Host "json=$OutputJsonPath"
}
