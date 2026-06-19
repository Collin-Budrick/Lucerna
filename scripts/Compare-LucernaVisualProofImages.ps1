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

    [switch] $AutoFocusRegion,

    [double] $AutoRegionSearchLeftPercent = 5.0,

    [double] $AutoRegionSearchTopPercent = 10.0,

    [double] $AutoRegionSearchWidthPercent = 90.0,

    [double] $AutoRegionSearchHeightPercent = 80.0,

    [int] $AutoRegionColumns = 12,

    [int] $AutoRegionRows = 8,

    [int] $AutoRegionPaddingCells = 1,

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

function Add-AccumulatorValues {
    param(
        [System.Collections.IDictionary] $Target,
        [System.Collections.IDictionary] $Source
    )

    $Target.pixelCount += $Source.pixelCount
    $Target.changedPixels += $Source.changedPixels
    $Target.brighterPixels += $Source.brighterPixels
    $Target.darkerPixels += $Source.darkerPixels
    $Target.sumAbsR += $Source.sumAbsR
    $Target.sumAbsG += $Source.sumAbsG
    $Target.sumAbsB += $Source.sumAbsB
    $Target.sumAbsLuma += $Source.sumAbsLuma
    $Target.sumSignedLuma += $Source.sumSignedLuma
    $Target.sumSquaredLuma += $Source.sumSquaredLuma
    $Target.maxAbsLuma = [Math]::Max($Target.maxAbsLuma, $Source.maxAbsLuma)
    $Target.maxAbsChannel = [Math]::Max($Target.maxAbsChannel, $Source.maxAbsChannel)
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

function New-AutoRegionCells {
    param(
        [System.Collections.IDictionary] $SearchRegion,
        [int] $Columns,
        [int] $Rows
    )

    if ($Columns -lt 1 -or $Rows -lt 1) {
        throw "Auto focus grid dimensions must be positive. columns=$Columns rows=$Rows"
    }

    $cells = New-Object System.Collections.Generic.List[object]
    for ($row = 0; $row -lt $Rows; $row++) {
        $top = $SearchRegion.top + [int][Math]::Floor($SearchRegion.height * $row / [double]$Rows)
        $bottom = $SearchRegion.top + [int][Math]::Floor($SearchRegion.height * ($row + 1) / [double]$Rows)
        for ($col = 0; $col -lt $Columns; $col++) {
            $left = $SearchRegion.left + [int][Math]::Floor($SearchRegion.width * $col / [double]$Columns)
            $right = $SearchRegion.left + [int][Math]::Floor($SearchRegion.width * ($col + 1) / [double]$Columns)
            if ($right -le $left -or $bottom -le $top) {
                continue
            }
            $cells.Add([ordered]@{
                row = $row
                column = $col
                left = $left
                top = $top
                rightExclusive = $right
                bottomExclusive = $bottom
                width = $right - $left
                height = $bottom - $top
                accumulator = New-Accumulator
                metrics = $null
                score = 0.0
            }) | Out-Null
        }
    }
    return $cells.ToArray()
}

function Get-RegionOutput {
    param(
        [System.Collections.IDictionary] $Region,
        [int] $ImageWidth,
        [int] $ImageHeight,
        [string] $SelectionMode,
        [object] $AutoSelection
    )

    $output = [ordered]@{
        selectionMode = $SelectionMode
        leftPercent = [Math]::Round(100.0 * [double]$Region.left / [double]$ImageWidth, 4)
        topPercent = [Math]::Round(100.0 * [double]$Region.top / [double]$ImageHeight, 4)
        widthPercent = [Math]::Round(100.0 * [double]$Region.width / [double]$ImageWidth, 4)
        heightPercent = [Math]::Round(100.0 * [double]$Region.height / [double]$ImageHeight, 4)
        requestedLeftPercent = $RegionLeftPercent
        requestedTopPercent = $RegionTopPercent
        requestedWidthPercent = $RegionWidthPercent
        requestedHeightPercent = $RegionHeightPercent
        left = $Region.left
        top = $Region.top
        width = $Region.width
        height = $Region.height
    }
    if ($null -ne $AutoSelection) {
        $output.autoSelection = $AutoSelection
    }
    return $output
}

function Select-AutoFocusRegion {
    param(
        [object[]] $Cells,
        [System.Collections.IDictionary] $SearchRegion,
        [System.Collections.IDictionary] $FallbackRegion,
        [System.Collections.IDictionary] $FallbackAccumulator,
        [int] $Columns,
        [int] $Rows,
        [int] $PaddingCells
    )

    $ranked = New-Object System.Collections.Generic.List[object]
    foreach ($cell in $Cells) {
        if ($cell.accumulator.pixelCount -le 0) {
            continue
        }
        $metrics = Complete-Metrics $cell.accumulator
        $signedBias = [Math]::Max(0.0, [double]$metrics.meanSignedLuma)
        $score = ([double]$metrics.changedPixelPercent * 4.0) +
                ([double]$metrics.brighterPixelPercent * 2.0) +
                ([double]$metrics.meanAbsLuma * 2.0) +
                ($signedBias * 0.5) +
                ([double]$metrics.maxAbsLuma * 0.1)
        $cell.metrics = $metrics
        $cell.score = [Math]::Round($score, 6)
        if ([double]$cell.score -gt 0.0 -and ([int64]$metrics.changedPixels -gt 0 -or [int64]$metrics.brighterPixels -gt 0)) {
            $ranked.Add($cell) | Out-Null
        }
    }

    if ($ranked.Count -eq 0) {
        return [ordered]@{
            region = $FallbackRegion
            accumulator = $FallbackAccumulator
            diagnostics = [ordered]@{
                reason = "no_changed_or_bright_pixels_in_auto_search_region"
                searchRegion = $SearchRegion
                grid = [ordered]@{
                    columns = $Columns
                    rows = $Rows
                    paddingCells = $PaddingCells
                }
                topCandidateCells = @()
            }
        }
    }

    $topCells = @($ranked | Sort-Object -Property @{ Expression = { [double]$_.score }; Descending = $true } | Select-Object -First 5)
    $best = $topCells[0]
    $pad = [Math]::Max(0, $PaddingCells)
    $minColumn = [Math]::Max(0, [int]$best.column - $pad)
    $maxColumn = [Math]::Min($Columns - 1, [int]$best.column + $pad)
    $minRow = [Math]::Max(0, [int]$best.row - $pad)
    $maxRow = [Math]::Min($Rows - 1, [int]$best.row + $pad)

    $selectedCells = @($Cells | Where-Object {
        [int]$_.column -ge $minColumn -and [int]$_.column -le $maxColumn -and
        [int]$_.row -ge $minRow -and [int]$_.row -le $maxRow
    })

    $left = ($selectedCells | ForEach-Object { [int]$_.left } | Measure-Object -Minimum).Minimum
    $top = ($selectedCells | ForEach-Object { [int]$_.top } | Measure-Object -Minimum).Minimum
    $right = ($selectedCells | ForEach-Object { [int]$_.rightExclusive } | Measure-Object -Maximum).Maximum
    $bottom = ($selectedCells | ForEach-Object { [int]$_.bottomExclusive } | Measure-Object -Maximum).Maximum
    $accumulator = New-Accumulator
    foreach ($cell in $selectedCells) {
        Add-AccumulatorValues $accumulator $cell.accumulator
    }

    $candidateOutput = @($topCells | ForEach-Object {
        [ordered]@{
            row = $_.row
            column = $_.column
            score = $_.score
            left = $_.left
            top = $_.top
            width = $_.width
            height = $_.height
            metrics = $_.metrics
        }
    })

    return [ordered]@{
        region = [ordered]@{
            left = [int]$left
            top = [int]$top
            width = [int]($right - $left)
            height = [int]($bottom - $top)
            rightExclusive = [int]$right
            bottomExclusive = [int]$bottom
        }
        accumulator = $accumulator
        diagnostics = [ordered]@{
            reason = "selected_highest_delta_surface_cell_with_padding"
            searchRegion = $SearchRegion
            grid = [ordered]@{
                columns = $Columns
                rows = $Rows
                paddingCells = $PaddingCells
                selectedMinRow = $minRow
                selectedMaxRow = $maxRow
                selectedMinColumn = $minColumn
                selectedMaxColumn = $maxColumn
            }
            topCandidateCells = $candidateOutput
        }
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
        $autoSearchRegion = $null
        $autoCells = @()
        if ($AutoFocusRegion) {
            $autoSearchRegion = Get-RegionRectangle $width $height $AutoRegionSearchLeftPercent $AutoRegionSearchTopPercent $AutoRegionSearchWidthPercent $AutoRegionSearchHeightPercent
            $autoCells = New-AutoRegionCells $autoSearchRegion $AutoRegionColumns $AutoRegionRows
        }
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
                if ($AutoFocusRegion -and $x -ge $autoSearchRegion.left -and $x -lt $autoSearchRegion.rightExclusive -and $y -ge $autoSearchRegion.top -and $y -lt $autoSearchRegion.bottomExclusive) {
                    $cellColumn = [Math]::Min($AutoRegionColumns - 1, [Math]::Max(0, [int][Math]::Floor(($x - $autoSearchRegion.left) * $AutoRegionColumns / [double]$autoSearchRegion.width)))
                    $cellRow = [Math]::Min($AutoRegionRows - 1, [Math]::Max(0, [int][Math]::Floor(($y - $autoSearchRegion.top) * $AutoRegionRows / [double]$autoSearchRegion.height)))
                    $cellIndex = ($cellRow * $AutoRegionColumns) + $cellColumn
                    if ($cellIndex -ge 0 -and $cellIndex -lt $autoCells.Count) {
                        Add-PixelDelta $autoCells[$cellIndex].accumulator $baselinePixel $enabledPixel $ChangedPixelThreshold $BrightPixelThreshold
                    }
                }
            }
        }

        $selectionMode = "fixed"
        $autoSelection = $null
        if ($AutoFocusRegion) {
            $selected = Select-AutoFocusRegion $autoCells $autoSearchRegion $region $focus $AutoRegionColumns $AutoRegionRows $AutoRegionPaddingCells
            $region = $selected.region
            $focus = $selected.accumulator
            $selectionMode = "auto"
            $autoSelection = $selected.diagnostics
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
            focusRegion = Get-RegionOutput $region $width $height $selectionMode $autoSelection
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
Write-Host "focusRegion.selectionMode=$($result.focusRegion.selectionMode)"
if ($result.focusRegion.autoSelection) {
    Write-Host "focusRegion.autoSelection.reason=$($result.focusRegion.autoSelection.reason)"
}
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
