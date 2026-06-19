param(
    [ValidateSet("Baseline", "Enabled", "Debug", "Direct", "RawGi", "DenoisedGi", "FinalComposite", "StableHeatmap", "MovedHeatmap", "EmissiveHeatmap", "HistoryStable", "HistoryMoved", "FlatClusterOverlay", "InteriorCullingOverlay", "HighDistanceCullingOverlay")]
    [string] $Mode,

    [ValidateSet("Round5Direct", "Round5DirectSurface", "Round6DiffuseGi", "Round6NativeDiffuseGi", "Round6NativeDiffuseGiNoMarker", "Round7DenoiseComposite", "Round8AdaptiveHeatmaps", "Round9VirtualizedGeometry")]
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
        "Direct" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "OFF"
                compositeMode = "DIRECT_ONLY"
                artifactRole = "direct-emissive"
                requiredPatterns = @(
                    "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
                    "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated",
                    "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*(?:selected=direct-light:ready|sourceIdentity=native-direct-light-rgba8|sourceAuthenticity=accepted:native-direct-light-surface-source))(?=[^`r`n]*mode=final-composite-native-direct-light-surface-additive)"
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
                    "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; .*mode=FINAL_LUCERNA_COMPOSITE.*evidence=round7\.composite\.final\.direct_raw_denoised.*finalBlendComplete=true"
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

function Get-Round8CaptureIntent {
    param([string] $CaptureMode)

    $rayBudgetCommonPatterns = @(
        "(?:Lucerna Round 8 adaptive ray budget: .*adaptiveRayBudget(?:Enabled)?=true|round8\.adaptiveSampling=.*enabled=true)",
        "(?:Lucerna Round 8 adaptive ray budget buckets: .*reuse(?:Only)?=[0-9]+.*low=[0-9]+.*medium=[0-9]+.*high=[0-9]+|round8\.rayBudgetBuckets=.*reuseOnly=.*low=.*medium=.*high=)",
        "(?:Lucerna Round 8 adaptive ray budget: .*cacheConfidenceContribution=.*|round8\.cacheConfidenceContribution=.*(?:value|cacheConfidence)=)",
        "(?:Lucerna Round 8 ray-budget heatmap: .*artifactRole=|round8\.rayBudgetHeatmap=.*role=(?:ray-budget|ray-budget-[a-z-]+))"
    )
    $historyCommonPatterns = @(
        "(?:Lucerna Round 8 history confidence: .*historyAccepted=[0-9]+.*historyRejected=[0-9]+|round8\.historyCounts=.*historyAccepted=.*historyRejected=)",
        "(?:Lucerna Round 8 history confidence: .*confidence(?:Map)?=.*|round8\.historyConfidence=.*value=)",
        "(?:Lucerna Round 8 history-confidence heatmap: .*artifactRole=|round8\.historyConfidenceHeatmap=.*role=(?:history-confidence|history-confidence-[a-z-]+))"
    )

    switch ($CaptureMode) {
        "StableHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-stable"
                heatmapKind = "ray-budget"
                sceneState = "stable"
                sceneAction = "stationary"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*(?:reuse(?:Only)?|low)=[1-9][0-9]*|round8\.rayBudgetBuckets=.*(?:reuseOnly|low)=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*sceneState=stable|round8\.sceneState=.*sceneState: stable)"
                )
            }
        }
        "MovedHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-moved"
                heatmapKind = "ray-budget"
                sceneState = "moved-noisy"
                sceneAction = "moved"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*high=[1-9][0-9]*|round8\.rayBudgetBuckets=.*high=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*sceneState=(?:moved|noisy|moved-noisy)|round8\.sceneState=.*sceneState: (?:moved|noisy|moved-noisy))"
                )
            }
        }
        "EmissiveHeatmap" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "RAY_BUDGET_HEATMAP"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "ray-budget-emissive"
                heatmapKind = "ray-budget"
                sceneState = "emissive"
                sceneAction = "emissive"
                requiredPatterns = @($rayBudgetCommonPatterns) + @(
                    "(?:Lucerna Round 8 adaptive ray budget buckets: .*high=[1-9][0-9]*|round8\.rayBudgetBuckets=.*high=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 adaptive ray budget: .*emissive(?:Contribution|Proximity|Regions)=[1-9][0-9]*|round8\.sceneState=.*sceneState: emissive)"
                )
            }
        }
        "HistoryStable" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "HISTORY_CONFIDENCE"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "history-confidence-stable"
                heatmapKind = "history-confidence"
                sceneState = "stable"
                sceneAction = "stationary"
                requiredPatterns = @($historyCommonPatterns) + @(
                    "(?:Lucerna Round 8 history confidence: .*historyAccepted=[1-9][0-9]*|round8\.historyCounts=.*historyAccepted=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 history confidence: .*sceneState=stable|round8\.sceneState=.*sceneState: stable)"
                )
            }
        }
        "HistoryMoved" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "HISTORY_CONFIDENCE"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "history-confidence-moved"
                heatmapKind = "history-confidence"
                sceneState = "moved-disoccluded"
                sceneAction = "moved"
                requiredPatterns = @($historyCommonPatterns) + @(
                    "(?:Lucerna Round 8 history confidence: .*historyRejected=[1-9][0-9]*|round8\.historyCounts=.*historyRejected=[1-9][0-9]*)",
                    "(?:Lucerna Round 8 history confidence: .*sceneState=(?:moved|disoccluded|moved-disoccluded)|round8\.sceneState=.*sceneState: (?:moved|disoccluded|moved-disoccluded))"
                )
            }
        }
        default {
            throw "Unsupported Round 8 capture mode: $CaptureMode"
        }
    }
}

function Get-Round9CaptureIntent {
    param([string] $CaptureMode)

    $clusterCommonPatterns = @(
        "(?:Lucerna Round 9 virtualized chunk geometry|round9\.virtualized(?:Chunk)?Geometry|round9\.chunkClusters)",
        "(?:cluster(?:Count|s)?|clusters(?:Total)?|cluster_count)=[1-9][0-9]*",
        "(?:visibleCluster(?:Count|s)?|visible_clusters|visible_cluster_count)=[0-9]+",
        "(?:upload(?:Bytes|_bytes)|clusterUploadBytes|upload_byte_estimate|total_upload_byte_estimate)=[1-9][0-9]*",
        "(?:generation(?:Counter|s)?|generation_counter|clusterGeneration|geometryGeneration)=[1-9][0-9]*"
    )
    $cullingCommonPatterns = @(
        "(?:Lucerna Round 9 chunk culling|round9\.chunkCulling|virtualized culling)",
        "(?:visibleCluster(?:Count|s)?|visible_clusters|visible_cluster_count)=[0-9]+",
        "(?:(?:culled|offscreen)Cluster(?:Count|s)?|culled_clusters|offscreen_clusters|culled_cluster_count)=[0-9]+",
        "(?:indirectDraw(?:Count|s|Placeholder)?|indirect_draw(?:_count|_count_placeholder)?|drawList(?:Count)?)=[0-9]+"
    )

    switch ($CaptureMode) {
        "FlatClusterOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "flat-open-cluster-overlay"
                sceneKind = "flat-open-terrain"
                sceneAction = "flat-open"
                requiredPatterns = @($clusterCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:flat-open-terrain|flat|open-terrain)",
                    "(?:clusterOverlay|chunkClusterOverlay|round9\.clusterOverlay)(?:Visible|Submitted|Enabled)?=true|round9ArtifactRole=flat-open-cluster-overlay"
                )
            }
        }
        "InteriorCullingOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "interior-wall-culling-overlay"
                sceneKind = "interior-wall-facing"
                sceneAction = "wall-facing"
                requiredPatterns = @($clusterCommonPatterns) + @($cullingCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:interior-wall-facing|wall-facing|interior)"
                )
            }
        }
        "HighDistanceCullingOverlay" {
            return [ordered]@{
                rendererEnabled = $true
                debugOverlay = "CHUNK_CULLING"
                compositeMode = "FINAL_LUCERNA_COMPOSITE"
                artifactRole = "high-distance-open-terrain-culling-overlay"
                sceneKind = "high-render-distance-open-terrain"
                sceneAction = "high-distance-open"
                requiredPatterns = @($clusterCommonPatterns) + @($cullingCommonPatterns) + @(
                    "(?:sceneKind|round9\.scene)=(?:high-render-distance-open-terrain|high-distance-open|open-terrain)"
                )
            }
        }
        default {
            throw "Unsupported Round 9 capture mode: $CaptureMode"
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
                    throw "Lucerna visual proof is contaminated before required markers were observed. Matched forbidden marker '$pattern' in $path. Capture the requested validation profile without proof-marker overlays, temporary direct-light payload sources, focus-window-only preview modes, or invalid validation markers."
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
                    throw "Lucerna visual proof is contaminated. Matched forbidden marker '$pattern' in $LogPath. Capture the requested validation profile without proof-marker overlays, temporary direct-light payload sources, focus-window-only preview modes, or invalid validation markers."
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

function Clear-MinecraftChat {
    Focus-MinecraftWindow | Out-Null
    if (-not ("LucernaValidationKeyboard" -as [type])) {
        Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class LucernaValidationKeyboard {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, int dwFlags, UIntPtr dwExtraInfo);
}
"@
    }

    $keyUp = 0x0002
    [LucernaValidationKeyboard]::keybd_event(0x72, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x44, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x44, 0, $keyUp, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [LucernaValidationKeyboard]::keybd_event(0x72, 0, $keyUp, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 500
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

function Invoke-Round9SceneAction {
    param([string] $SceneAction)

    switch ($SceneAction) {
        "flat-open" {
            Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
            Send-MinecraftChatCommand "/gamemode creative"
            Send-MinecraftChatCommand "/time set 6000"
            Send-MinecraftChatCommand "/weather clear"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ 0 18"
        }
        "wall-facing" {
            if ($SetupScene) {
                Send-MinecraftChatCommand "/fill ~4 ~-1 ~-4 ~4 ~4 ~4 minecraft:smooth_stone"
            }
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -90 0"
        }
        "high-distance-open" {
            Send-MinecraftChatCommand "/gamerule sendCommandFeedback false"
            Send-MinecraftChatCommand "/gamemode creative"
            Send-MinecraftChatCommand "/time set 6000"
            Send-MinecraftChatCommand "/weather clear"
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ 35 10"
        }
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
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        "round8-adaptive-heatmap-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        "round9-virtualized-geometry-$($Mode.ToLowerInvariant())"
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        "round5-direct-surface-$($Mode.ToLowerInvariant())"
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
    $round8CaptureIntent = $null
    $round9CaptureIntent = $null
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        $round7CaptureIntent = Get-Round7CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round7CaptureIntent.rendererEnabled) `
            ([string]$round7CaptureIntent.debugOverlay) `
            ([string]$round7CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        $round8CaptureIntent = Get-Round8CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round8CaptureIntent.rendererEnabled) `
            ([string]$round8CaptureIntent.debugOverlay) `
            ([string]$round8CaptureIntent.compositeMode)
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        $round9CaptureIntent = Get-Round9CaptureIntent $Mode
        Write-LucernaConfig `
            $root `
            ([bool]$round9CaptureIntent.rendererEnabled) `
            ([string]$round9CaptureIntent.debugOverlay) `
            ([string]$round9CaptureIntent.compositeMode)
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
    if ($ValidationProfile -eq "Round5DirectSurface" -or $ValidationProfile -eq "Round6NativeDiffuseGiNoMarker" -or $ValidationProfile -eq "Round7DenoiseComposite" -or $ValidationProfile -eq "Round8AdaptiveHeatmaps" -or $ValidationProfile -eq "Round9VirtualizedGeometry") {
        $psi.Environment["LUCERNA_HIDE_PROOF_OVERLAYS"] = "true"
    }
    if ($ValidationProfile -eq "Round7DenoiseComposite") {
        $psi.Environment["LUCERNA_ROUND7_CAPTURE_MODE"] = [string]$round7CaptureIntent.artifactRole
    }
    if ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        $psi.Environment["LUCERNA_ROUND8_CAPTURE_MODE"] = [string]$round8CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND8_ARTIFACT_ROLE"] = [string]$round8CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND8_HEATMAP"] = [string]$round8CaptureIntent.heatmapKind
        $psi.Environment["LUCERNA_ROUND8_SCENE_STATE"] = [string]$round8CaptureIntent.sceneState
        $psi.Environment["LUCERNA_ROUND8_VISUAL_PROOF_OWNER"] = "controller"
    }
    if ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        $psi.Environment["LUCERNA_ROUND9_CAPTURE_MODE"] = [string]$round9CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND9_ARTIFACT_ROLE"] = [string]$round9CaptureIntent.artifactRole
        $psi.Environment["LUCERNA_ROUND9_SCENE_KIND"] = [string]$round9CaptureIntent.sceneKind
        $psi.Environment["LUCERNA_ROUND9_VISUAL_PROOF_OWNER"] = "controller"
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
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        @($round8CaptureIntent.requiredPatterns)
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        @($round9CaptureIntent.requiredPatterns)
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
    } elseif ($ValidationProfile -eq "Round5DirectSurface" -and $Mode -ne "Baseline") {
        @(
            "Lucerna direct lighting plan: .*emissive=[1-9][0-9]*.*shadowCandidates=[1-9][0-9]*.*surfaceSampleSections=[1-9][0-9]*.*surfaceSamples=[1-9][0-9]*\.",
            "Lucerna native direct lighting execution: .*outputWriteRecorded=true.*resolveRecorded=true.*ready=true.*cpuOutput=true.*cpuOutputEnergy=[1-9][0-9.eE+-]*.*cpuOutputChecksum=[1-9][0-9]*.*reason=direct_lighting_(?:surface_sample|emissive_candidate)_cpu_output_generated",
            "Lucerna public Mojang final composite: attempted=true submitted=true drawCalls=true(?=[^`r`n]*mode=(?![^`r`n]*focus-window)[^`r`n]*(?:direct|emissive))(?=[^`r`n]*(?:surface|world|final|composite))"
        )
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        @()
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
    } elseif ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "proof marker",
            "CPU output proof",
            "invalidRayBudget=true",
            "invalid_budget_values=true",
            "negative ray budget",
            "rayBudget=.*(?:NaN|Infinity)"
        )
    } elseif ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "round6-diffuse-gi-focus-window-additive",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
            "R8 proof",
            "proof marker",
            "CPU output proof",
            "invalidCluster(?:Count|s)?=true",
            "negative cluster",
            "cluster(?:Count|s)?=.*(?:NaN|Infinity)",
            "visibleCluster(?:Count|s)?=.*(?:NaN|Infinity)"
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
    } elseif ($ValidationProfile -eq "Round5DirectSurface") {
        @(
            "temporarySourceReady=true",
            "using the current direct-light RGBA payload as the temporary visible source",
            "temporary direct-light",
            "current direct-light RGBA payload",
            "sourceIdentity=native-direct-light-rgba8,focusWindowOnly=true",
            "final-composite-direct-light-focus-window-additive",
            "focus-window-only",
            "round5-direct-proof",
            "R5 visual proof",
            "round6-gi-proof",
            "R6 GI proof",
            "R7 proof",
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

    if ($ValidationProfile -eq "Round8AdaptiveHeatmaps") {
        if ($round8CaptureIntent.sceneAction -eq "moved" -and $SetupScene) {
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -35 0"
            Start-Sleep -Seconds 1
            Send-MinecraftChatCommand "/tp @s ~ ~ ~ -140 0"
        } elseif ($round8CaptureIntent.sceneAction -eq "stationary") {
            Start-Sleep -Seconds 5
        }
    }
    if ($ValidationProfile -eq "Round9VirtualizedGeometry") {
        Invoke-Round9SceneAction ([string]$round9CaptureIntent.sceneAction)
        Start-Sleep -Seconds 5
    }

    if ($enabledPatterns.Count -gt 0) {
        Wait-LatestLogPattern $markerLog $enabledPatterns $deadline $earlyFailureLogPaths $forbiddenPatterns
    }
    if (($ValidationProfile -eq "Round7DenoiseComposite" -or $ValidationProfile -eq "Round8AdaptiveHeatmaps" -or $ValidationProfile -eq "Round9VirtualizedGeometry") -and -not $SetupScene) {
        Start-Sleep -Seconds 8
    }

    $archiveName = "$scenario-$stamp-$Mode.png"
    $archivePath = Join-Path $screenshotArchiveDir $archiveName
    $existingScreenshotNames = @(Get-ChildItem -LiteralPath $screenshotDir -Filter "*.png" -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty Name)
    $beforeScreenshot = Get-Date
    Clear-MinecraftChat
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
    if ($round8CaptureIntent) {
        Write-Host "round8ArtifactRole=$($round8CaptureIntent.artifactRole)"
        Write-Host "round8Heatmap=$($round8CaptureIntent.heatmapKind)"
        Write-Host "round8SceneState=$($round8CaptureIntent.sceneState)"
        Write-Host "round8CompositeMode=$($round8CaptureIntent.compositeMode)"
    }
    if ($round9CaptureIntent) {
        Write-Host "round9ArtifactRole=$($round9CaptureIntent.artifactRole)"
        Write-Host "round9SceneKind=$($round9CaptureIntent.sceneKind)"
        Write-Host "round9DebugOverlay=$($round9CaptureIntent.debugOverlay)"
        Write-Host "round9CompositeMode=$($round9CaptureIntent.compositeMode)"
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
