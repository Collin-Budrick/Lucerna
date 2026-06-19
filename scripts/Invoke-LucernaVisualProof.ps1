param(
    [ValidateSet("Baseline", "Enabled", "Debug", "RawGi", "DenoisedGi", "FinalComposite")]
    [string] $Mode,

    [ValidateSet("Round5Direct", "Round6DiffuseGi", "Round6NativeDiffuseGi", "Round6NativeDiffuseGiNoMarker", "Round7DenoiseComposite")]
    [string] $ValidationProfile = "Round5Direct",

    [string] $WorldName = "New World",

    [string] $ScenarioName = "",

    [switch] $SetupScene,

    [string] $BaselineImagePath = "",

    [string] $EnabledImagePath = "",

    [string] $ImageDeltaJsonPath = "",

    [double] $ImageDeltaRegionLeftPercent = 30.0,

    [double] $ImageDeltaRegionTopPercent = 20.0,

    [double] $ImageDeltaRegionWidthPercent = 40.0,

    [double] $ImageDeltaRegionHeightPercent = 55.0,

    [switch] $AutoImageDeltaRegion,

    [double] $AutoImageDeltaSearchLeftPercent = 5.0,

    [double] $AutoImageDeltaSearchTopPercent = 10.0,

    [double] $AutoImageDeltaSearchWidthPercent = 90.0,

    [double] $AutoImageDeltaSearchHeightPercent = 80.0,

    [int] $AutoImageDeltaRegionColumns = 12,

    [int] $AutoImageDeltaRegionRows = 8,

    [int] $AutoImageDeltaRegionPaddingCells = 1,

    [int] $TimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"

function Invoke-ImageDeltaComparison {
    param(
        [string] $BaselinePath,
        [string] $EnabledPath,
        [string] $JsonPath
    )

    $compareScript = Join-Path $PSScriptRoot "Compare-LucernaVisualProofImages.ps1"
    if (-not (Test-Path -LiteralPath $compareScript)) {
        throw "Missing Lucerna image comparison helper: $compareScript"
    }

    $args = @(
        "-BaselineImagePath", $BaselinePath,
        "-EnabledImagePath", $EnabledPath,
        "-RegionLeftPercent", $ImageDeltaRegionLeftPercent,
        "-RegionTopPercent", $ImageDeltaRegionTopPercent,
        "-RegionWidthPercent", $ImageDeltaRegionWidthPercent,
        "-RegionHeightPercent", $ImageDeltaRegionHeightPercent
    )
    if ($AutoImageDeltaRegion) {
        $args += @(
            "-AutoFocusRegion",
            "-AutoRegionSearchLeftPercent", $AutoImageDeltaSearchLeftPercent,
            "-AutoRegionSearchTopPercent", $AutoImageDeltaSearchTopPercent,
            "-AutoRegionSearchWidthPercent", $AutoImageDeltaSearchWidthPercent,
            "-AutoRegionSearchHeightPercent", $AutoImageDeltaSearchHeightPercent,
            "-AutoRegionColumns", $AutoImageDeltaRegionColumns,
            "-AutoRegionRows", $AutoImageDeltaRegionRows,
            "-AutoRegionPaddingCells", $AutoImageDeltaRegionPaddingCells
        )
    }
    if (-not [string]::IsNullOrWhiteSpace($JsonPath)) {
        $args += @("-OutputJsonPath", $JsonPath)
    }

    & $compareScript @args
}

function Write-LucernaConfig {
    param(
        [string] $Root,
        [bool] $RendererEnabled,
        [string] $DebugOverlay,
        [string] $CompositeMode = "FINAL_LUCERNA_COMPOSITE"
    )

    $configDir = Join-Path $Root "run\config"
    New-Item -ItemType Directory -Force -Path $configDir | Out-Null
    $configPath = Join-Path $configDir "lucerna.json"
    $config = [ordered]@{
        schemaVersion = 2
        rendererEnabled = $RendererEnabled
        qualityPreset = "BALANCED"
        debugOverlay = $DebugOverlay
        compositeMode = $CompositeMode
        showIrisNotice = $true
    }
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}

function Get-Round7CaptureIntent {
    param([string] $CaptureMode)

    switch ($CaptureMode) {
        "Baseline" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "BASE_VANILLA_ONLY"
                artifactRole = "baseline"
                requiredPatterns = @()
            }
        }
        "RawGi" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "RAW_GI"
                artifactRole = "raw-gi"
                requiredPatterns = @(
                    "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
                    "public Mojang Round 7 RAW_GI visual render pass submitted; .*mode=ROUND7_RAW_GI.*evidence=round7\.rawGi\.nativeDiffuseGiPayload",
                    "public Mojang Round 7 RAW_GI native diffuse-GI source additive draw issued"
                )
            }
        }
        "DenoisedGi" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DENOISED_GI"
                artifactRole = "denoised-gi"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedPayloadReady=true.*readyForPreviewDraw=true",
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true.*denoised_cpu_output_generated=true",
                    "Lucerna Round 7 denoised GI CPU output: .*realDenoiseShaderOutput=false",
                    "public Mojang Round 7 DENOISED_GI visual render pass submitted; .*mode=ROUND7_DENOISED_GI.*denoisedPayloadEvidence=round7\.denoisedGi\.cpuDenoisedDiffuseGiPayload"
                )
            }
        }
        "FinalComposite" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "final-composite"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true",
                    "round7\.finalCompositeMode=final-composite.*round7\.finalCompositeSourceMix=base=true,direct=enabled-ready,gi=enabled-ready,denoised=enabled-ready",
                    "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*evidence=round7\.composite\.final\.base_direct_gi"
                )
            }
        }
        "Debug" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "DIRECT_LIGHTING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "debug"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "(?:round7\.compositeMode|debug\.overlay=DIRECT_LIGHTING|Overlay state: DIRECT_LIGHTING|Direct Lighting)"
                )
            }
        }
        "Enabled" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "final-composite"
                requiredPatterns = @(
                    "Lucerna Round 7 denoised GI CPU output: .*denoisedCpuOutputGenerated=true",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true"
                )
            }
        }
        default {
            throw "Unsupported Round 7 capture mode: $CaptureMode"
        }
    }
}

function Wait-LatestLogPattern {
    param(
        [string] $LogPath,
        [string[]] $RequiredPatterns,
        [datetime] $Deadline,
        [string[]] $EarlyFailureLogPaths = @(),
        [string[]] $ForbiddenPatterns = @()
    )

    $earlyFailurePatterns = @(
        "Lucerna native library is not available yet",
        "Application Control policy has blocked this file"
    )
    $pathsToScan = @($LogPath) + @($EarlyFailureLogPaths)

    while ((Get-Date) -lt $Deadline) {
        foreach ($path in $pathsToScan) {
            if ([string]::IsNullOrWhiteSpace($path) -or -not (Test-Path -LiteralPath $path)) {
                continue
            }
            try {
                $candidateLog = Get-Content -Raw -LiteralPath $path
            } catch {
                continue
            }
            foreach ($pattern in $earlyFailurePatterns) {
                if ($candidateLog -match $pattern) {
                    throw "Lucerna visual proof is blocked before required markers were observed. Matched native-load failure marker '$pattern' in $path. For Round6NativeDiffuseGi, this means Windows Application Control/native DLL loading must be resolved before the controller can validate native diffuse-GI output-source replacement; do not count the temporary direct-light RGBA preview path as this proof."
                }
            }
            foreach ($pattern in $ForbiddenPatterns) {
                if ($candidateLog -match $pattern) {
                    throw "Lucerna visual proof is contaminated before required markers were observed. Matched forbidden marker '$pattern' in $path. For no-marker Round 6 validation, capture a surface-composite run without proof-marker overlays, temporary direct-light payload sources, or focus-window-only preview modes."
                }
            }
        }

        if (Test-Path -LiteralPath $LogPath) {
            try {
                $log = Get-Content -Raw -LiteralPath $LogPath
            } catch {
                Start-Sleep -Milliseconds 500
                continue
            }
            $allPresent = $true
            foreach ($pattern in $RequiredPatterns) {
                if ($log -notmatch $pattern) {
                    $allPresent = $false
                    break
                }
            }
            foreach ($pattern in $ForbiddenPatterns) {
                if ($log -match $pattern) {
                    throw "Lucerna visual proof is contaminated. Matched forbidden marker '$pattern' in $LogPath. For no-marker Round 6 validation, capture a surface-composite run without proof-marker overlays, temporary direct-light payload sources, or focus-window-only preview modes."
                }
            }
            if ($allPresent) {
                return
            }
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for required log markers: $($RequiredPatterns -join '; ')"
}

function Get-MinecraftWindowProcess {
    Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object {
        $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -like "*Minecraft*"
    } | Sort-Object @{ Expression = {
                try {
                    $_.StartTime
                } catch {
                    [datetime]::MinValue
                }
            }; Descending = $true } | Select-Object -First 1
}

function Focus-MinecraftWindow {
    $windowProcess = Get-MinecraftWindowProcess
    if ($null -eq $windowProcess) {
        throw "Could not find a Minecraft/java window to focus."
    }

    $shell = New-Object -ComObject WScript.Shell
    [void] $shell.AppActivate($windowProcess.Id)
    Start-Sleep -Milliseconds 500
    return $windowProcess
}

function Send-MinecraftKeys {
    param([string] $Keys)

    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.SendKeys]::SendWait($Keys)
    Start-Sleep -Milliseconds 250
}

function Send-MinecraftChatCommand {
    param([string] $Command)

    Focus-MinecraftWindow | Out-Null
    Set-Clipboard -Value $Command
    Send-MinecraftKeys "t"
    Send-MinecraftKeys "^v"
    Send-MinecraftKeys "{ENTER}"
    Start-Sleep -Milliseconds 750
}

function Invoke-OptionalSceneSetup {
    if (-not $SetupScene) {
        return
    }

    $commands = @(
        "/gamerule sendCommandFeedback false",
        "/gamerule doDaylightCycle false",
        "/gamemode creative",
        "/time set 18000",
        "/weather clear",
        "/kill @e[type=!player,distance=..32]",
        "/fill ~4 ~-1 ~-3 ~4 ~3 ~3 minecraft:smooth_stone",
        "/setblock ~3 ~ ~ minecraft:glowstone",
        "/time set 18000",
        "/tp @s ~ ~ ~ -90 0"
    )
    foreach ($command in $commands) {
        Send-MinecraftChatCommand $command
    }
}

function Wait-NewScreenshot {
    param(
        [string] $ScreenshotDir,
        [string[]] $ExistingNames,
        [datetime] $After,
        [datetime] $Deadline
    )

    while ((Get-Date) -lt $Deadline) {
        $afterWithTolerance = $After.AddSeconds(-1)
        $candidate = Get-ChildItem -LiteralPath $ScreenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
                Where-Object { $_.LastWriteTime -gt $afterWithTolerance -and $ExistingNames -notcontains $_.Name } |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
        if ($null -ne $candidate) {
            return $candidate
        }
        Start-Sleep -Milliseconds 500
    }
    throw "Timed out waiting for a new Minecraft screenshot."
}

function Save-MinecraftWindowScreenshot {
    param(
        [string] $DestinationPath
    )

    $windowProcess = Get-MinecraftWindowProcess
    if ($null -eq $windowProcess) {
        throw "Could not find a Minecraft/java window for fallback screenshot capture."
    }

    if (-not ("LucernaVisualProof.NativeWindow" -as [type])) {
        Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

namespace LucernaVisualProof {
    public static class NativeWindow {
        [StructLayout(LayoutKind.Sequential)]
        public struct RECT {
            public int Left;
            public int Top;
            public int Right;
            public int Bottom;
        }

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    }
}
"@
    }
    Add-Type -AssemblyName System.Drawing

    $rect = New-Object LucernaVisualProof.NativeWindow+RECT
    if (-not [LucernaVisualProof.NativeWindow]::GetWindowRect($windowProcess.MainWindowHandle, [ref]$rect)) {
        throw "Could not get Minecraft/java window bounds for fallback screenshot capture."
    }

    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)
    $bitmap = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
        $bitmap.Save($DestinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
    return Get-Item -LiteralPath $DestinationPath
}

function Copy-FreshLatestLog {
    param(
        [string] $Root,
        [string] $ValidationDir,
        [string] $Scenario,
        [string] $Stamp,
        [string] $SourceLog = ""
    )

    $latestLog = if ([string]::IsNullOrWhiteSpace($SourceLog)) {
        Join-Path $Root "run\logs\latest.log"
    } else {
        $SourceLog
    }
    if (-not (Test-Path -LiteralPath $latestLog)) {
        return ""
    }

    $safeScenario = ($Scenario -replace "[^A-Za-z0-9_.-]", "-").Trim("-")
    if ([string]::IsNullOrWhiteSpace($safeScenario)) {
        $safeScenario = "lucerna-visual-proof"
    }

    $target = Join-Path $ValidationDir "latest-$safeScenario-$Stamp.log"
    Copy-Item -LiteralPath $latestLog -Destination $target -Force
    return $target
}

if (-not [string]::IsNullOrWhiteSpace($BaselineImagePath) -or -not [string]::IsNullOrWhiteSpace($EnabledImagePath)) {
    if ([string]::IsNullOrWhiteSpace($BaselineImagePath) -or [string]::IsNullOrWhiteSpace($EnabledImagePath)) {
        throw "Both -BaselineImagePath and -EnabledImagePath are required for image-delta-only mode."
    }
    Invoke-ImageDeltaComparison $BaselineImagePath $EnabledImagePath $ImageDeltaJsonPath
    return
}

$root = (Resolve-Path ".").Path
$gradlew = Join-Path $root "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradlew)) {
    throw "Run this script from a Minecraft mod workspace containing gradlew.bat."
}

$scenario = if ([string]::IsNullOrWhiteSpace($ScenarioName)) {
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        "round7-denoise-composite-$($Mode.ToLowerInvariant())"
    } else {
        "round5-visual-proof-$($Mode.ToLowerInvariant())"
    }
} else {
    $ScenarioName
}
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$validationDir = Join-Path $root "run\validation-logs"
$screenshotArchiveDir = Join-Path $root "run\validation-screenshots"
$screenshotDir = Join-Path $root "run\screenshots"
New-Item -ItemType Directory -Force -Path $validationDir | Out-Null
New-Item -ItemType Directory -Force -Path $screenshotArchiveDir | Out-Null
New-Item -ItemType Directory -Force -Path $screenshotDir | Out-Null

$configPath = Join-Path $root "run\config\lucerna.json"
$backupConfig = $null
$configExisted = Test-Path -LiteralPath $configPath
if ($configExisted) {
    $backupConfig = Get-Content -Raw -LiteralPath $configPath
}

$aliasPath = $null
$createdAlias = $false
$process = $null
try {
    $round7CaptureIntent = $null
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        $round7CaptureIntent = Get-Round7CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round7CaptureIntent.rendererEnabled) `
            ([string]$round7CaptureIntent.debugOverlay) `
            ([string]$round7CaptureIntent.compositeMode)
    } else {
        switch ($Mode) {
            "Baseline" { Write-LucernaConfig $root $false "OFF" }
            "Enabled" { Write-LucernaConfig $root $true "OFF" }
            "Debug" { Write-LucernaConfig $root $true "DIRECT_LIGHTING" }
            default { throw "Mode '$Mode' is only supported with -ValidationProfile Round7DenoiseComposite." }
        }
    }

    $latestLog = Join-Path $root "run\logs\latest.log"
    if (Test-Path -LiteralPath $latestLog) {
        Remove-Item -LiteralPath $latestLog -Force
    }

    $quickPlayWorld = $WorldName
    if ($WorldName -match "\s") {
        $safeWorldName = "CodexVisualProofWorld"
        $worldTarget = Join-Path $root ("run\saves\" + $WorldName)
        $aliasPath = Join-Path $root ("run\saves\" + $safeWorldName)
        if (Test-Path -LiteralPath $aliasPath) {
            $existing = Get-Item -LiteralPath $aliasPath
            $existingTarget = if ($existing.Target -is [array]) { $existing.Target -join "" } else { [string]$existing.Target }
            if ($existing.LinkType -ne "Junction" -or [string]::IsNullOrWhiteSpace($existingTarget)) {
                Remove-Item -LiteralPath $aliasPath -Recurse -Force
                New-Item -ItemType Junction -Path $aliasPath -Target $worldTarget | Out-Null
                $createdAlias = $true
            } elseif ($existingTarget -ne $worldTarget) {
                throw "Quick-play alias already exists with a different target: $aliasPath"
            }
        } else {
            New-Item -ItemType Junction -Path $aliasPath -Target $worldTarget | Out-Null
            $createdAlias = $true
        }
        $quickPlayWorld = $safeWorldName
    }

    $gradleOut = Join-Path $validationDir "gradle-$scenario-$stamp.out.log"
    $gradleErr = Join-Path $validationDir "gradle-$scenario-$stamp.err.log"
    $loomArgs = "runClient"
    if (-not [string]::IsNullOrWhiteSpace($quickPlayWorld)) {
        $loomArgs += " `"--args=--quickPlaySingleplayer $quickPlayWorld`""
    }

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $gradlew
    $psi.WorkingDirectory = $root
    $psi.Arguments = $loomArgs
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    if ($ValidationProfile -eq "Round6NativeDiffuseGiNoMarker" -or $ValidationProfile -eq "Round7DenoiseComposite") {
        $psi.Environment["LUCERNA_HIDE_PROOF_OVERLAYS"] = "true"
    }
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        $psi.Environment["LUCERNA_ROUND7_CAPTURE_MODE"] = [string]$round7CaptureIntent.artifactRole
    }
    $process = [System.Diagnostics.Process]::Start($psi)
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()
    Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
        if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData.Out -Value $EventArgs.Data }
    } -MessageData @{ Out = $gradleOut } | Out-Null
    Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
        if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData.Err -Value $EventArgs.Data }
    } -MessageData @{ Err = $gradleErr } | Out-Null

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $commonPatterns = @(
        "Using graphics backend Vulkan",
        "Lucerna backend status: SODIUM_VULKAN",
        "joined the game"
    )
    $enabledPatterns = if ($ValidationProfile -eq "Round7DenoiseComposite") {
        @($round7CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGiNoMarker") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:nativeGiOutputReady|nativeDiffuseGiOutputReady|sourceNativeGiReady)=true",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi)",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-native-diffuse-gi-surface-additive"
        )
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGi") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:nativeGiOutputReady|nativeDiffuseGiOutputReady|sourceNativeGiReady)=true",
            "Lucerna Round 6 diffuse GI preview composite: .*ready=true .*(?:temporarySourceReady=false|(?:visibleSource|outputSource|source|sourceType)=`"?native[-_ ]?diffuse[-_ ]?gi)",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=(?:round6-diffuse-gi-|round6-native-diffuse-gi-)"
        )
    } elseif ($ValidationProfile -eq "Round6DiffuseGi") {
        @(
            "Lucerna Round 6 lighting dispatch prepared: .*diffuse_gi=\{\{enabled=true,.*rays=[1-9][0-9]*,cache_reads=[1-9][0-9]*",
            "Lucerna Round 6 diffuse GI preview composite: ready=true .*temporarySourceReady=true",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=round6-diffuse-gi-focus-window-additive"
        )
    } else {
        @(
        "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
        "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_surface_sample_cpu_output_generated",
        "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true.*mode=final-composite-direct-light-focus-window-additive"
        )
    }
    $forbiddenPatterns = if ($ValidationProfile -eq "Round7DenoiseComposite") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "proof marker",
            "CPU output proof"
        )
    } elseif ($ValidationProfile -eq "Round6NativeDiffuseGiNoMarker") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "proof marker",
            "CPU output proof"
        )
    } else {
        @()
    }
    $markerLog = $gradleOut
    $earlyFailureLogPaths = @($gradleOut, $gradleErr)
    Wait-LatestLogPattern $markerLog $commonPatterns $deadline $earlyFailureLogPaths $forbiddenPatterns

    Invoke-OptionalSceneSetup
    if ($SetupScene) {
        Start-Sleep -Seconds 8
    }

    if ($enabledPatterns.Count -gt 0) {
        Wait-LatestLogPattern $markerLog $enabledPatterns $deadline $earlyFailureLogPaths $forbiddenPatterns
    }
    if ($ValidationProfile -eq "Round7DenoiseComposite" -and -not $SetupScene) {
        Start-Sleep -Seconds 8
    }

    $archiveName = "$scenario-$stamp-$Mode.png"
    $archivePath = Join-Path $screenshotArchiveDir $archiveName
    $existingScreenshotNames = @(Get-ChildItem -LiteralPath $screenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty Name)
    $beforeScreenshot = Get-Date
    Focus-MinecraftWindow | Out-Null
    Send-MinecraftKeys "{F2}"
    $screenshotDeadline = (Get-Date).AddSeconds(45)
    try {
        $screenshot = Wait-NewScreenshot $screenshotDir $existingScreenshotNames $beforeScreenshot $screenshotDeadline
        Copy-Item -LiteralPath $screenshot.FullName -Destination $archivePath -Force
        $screenshotSource = "minecraft-f2"
    } catch {
        $screenshot = Save-MinecraftWindowScreenshot $archivePath
        $screenshotSource = "window-fallback"
    }

    $logPath = Copy-FreshLatestLog $root $validationDir $scenario $stamp $markerLog
    Write-Host "screenshot=$archivePath"
    Write-Host "screenshotSource=$screenshotSource"
    if ($round7CaptureIntent) {
        Write-Host "round7ArtifactRole=$($round7CaptureIntent.artifactRole)"
        Write-Host "round7CompositeMode=$($round7CaptureIntent.compositeMode)"
    }
    Write-Host "latestLog=$logPath"
    Write-Host "gradleOut=$gradleOut"
    Write-Host "gradleErr=$gradleErr"
} finally {
    if ($process -and -not $process.HasExited) {
        Get-Process | Where-Object { $_.MainWindowTitle -like "*Minecraft*" } | ForEach-Object { [void] $_.CloseMainWindow() }
        if (-not $process.WaitForExit(20000)) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    if ($configExisted) {
        Set-Content -LiteralPath $configPath -Value $backupConfig -Encoding UTF8
    } elseif (Test-Path -LiteralPath $configPath) {
        Remove-Item -LiteralPath $configPath -Force
    }
    if ($createdAlias -and $aliasPath -and (Test-Path -LiteralPath $aliasPath)) {
        try {
            [System.IO.Directory]::Delete($aliasPath)
        } catch {
            Write-Warning "Could not remove quick-play alias ${aliasPath}: $($_.Exception.Message)"
        }
    }
}
