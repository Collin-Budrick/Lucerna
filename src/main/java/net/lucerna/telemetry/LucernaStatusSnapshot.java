package net.lucerna.telemetry;

import net.lucerna.LucernaController;
import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;
import net.lucerna.config.DebugOverlay;
import net.lucerna.config.QualityPreset;
import net.lucerna.render.context.BorrowedVulkanContextAcquisition;
import net.lucerna.render.hooks.FrameHookStage;
import net.lucerna.render.hooks.FrameLifecycleSnapshot;
import net.lucerna.render.hooks.FramePassIntent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record LucernaStatusSnapshot(
        boolean rendererEnabled,
        boolean rendererActive,
        QualityPreset qualityPreset,
        DebugOverlay debugOverlay,
        BackendTelemetryStatus backend,
        NativeBridgeTelemetryStatus nativeBridge,
        IrisTelemetryStatus iris,
        DirtyRegionTelemetryStatus dirtyRegions,
        UploadGenerationTelemetryStatus uploads,
        FrameLifecycleSnapshot frameLifecycle,
        FrameTimingTelemetryStatus frameTimings,
        long capturedNanos
) {
    public LucernaStatusSnapshot {
        if (qualityPreset == null) {
            qualityPreset = QualityPreset.BALANCED;
        }
        if (debugOverlay == null) {
            debugOverlay = DebugOverlay.OFF;
        }
        if (backend == null) {
            backend = BackendTelemetryStatus.from(null);
        }
        if (nativeBridge == null) {
            nativeBridge = NativeBridgeTelemetryStatus.unavailable("Native bridge status has not been reported.");
        }
        if (iris == null) {
            iris = IrisTelemetryStatus.unavailable("Iris status has not been reported.");
        }
        if (dirtyRegions == null) {
            dirtyRegions = new DirtyRegionTelemetryStatus(0L, 0);
        }
        if (uploads == null) {
            uploads = new UploadGenerationTelemetryStatus(0L, 0L, 0L);
        }
        if (frameLifecycle == null) {
            frameLifecycle = emptyFrameLifecycle();
        }
        if (frameTimings == null) {
            frameTimings = FrameTimingTelemetryStatus.empty();
        }
    }

    public LucernaStatusSnapshot(
            boolean rendererEnabled,
            boolean rendererActive,
            QualityPreset qualityPreset,
            DebugOverlay debugOverlay,
            BackendKind backendKind,
            String backendName,
            String backendMessage,
            String irisMessage,
            long worldGeneration,
            long uploadGeneration,
            Map<String, Double> cpuScopeDurationsMillis,
            int activeCpuScopeCount,
            long capturedNanos
    ) {
        this(
                rendererEnabled,
                rendererActive,
                qualityPreset,
                debugOverlay,
                new BackendTelemetryStatus(backendKind, rendererActive, backendName, backendMessage),
                NativeBridgeTelemetryStatus.unavailable("Native bridge status was not supplied."),
                IrisTelemetryStatus.unavailable(irisMessage),
                new DirtyRegionTelemetryStatus(worldGeneration, 0),
                new UploadGenerationTelemetryStatus(uploadGeneration, uploadGeneration, 0L),
                emptyFrameLifecycle(),
                new FrameTimingTelemetryStatus(cpuScopeDurationsMillis, Map.of(), List.of(), activeCpuScopeCount),
                capturedNanos
        );
    }

    public static LucernaStatusSnapshot capture(LucernaController controller) {
        BackendStatus backendStatus = controller.backendStatus();
        return new LucernaStatusSnapshot(
                controller.getConfig().rendererEnabled(),
                controller.isRendererActive(),
                controller.getConfig().qualityPreset(),
                controller.getConfig().debugOverlay(),
                BackendTelemetryStatus.from(backendStatus),
                NativeBridgeTelemetryStatus.from(controller.nativeBridgeStatus()),
                IrisTelemetryStatus.from(controller.irisCompat().status()),
                new DirtyRegionTelemetryStatus(
                        controller.worldFeed().currentGeneration(),
                        controller.worldFeed().pendingDirtyRegionCount()
                ),
                UploadGenerationTelemetryStatus.from(controller.uploadQueue()),
                controller.frameHooks().snapshot(),
                FrameTimingTelemetryStatus.from(controller.telemetry()),
                System.nanoTime()
        );
    }

    public String rendererStateLabel() {
        if (!this.rendererEnabled) {
            return "disabled by config";
        }
        return this.rendererActive ? "active" : "inactive";
    }

    public String backendLabel() {
        return this.backend.label();
    }

    public String nativeBridgeLabel() {
        return this.nativeBridge.stateLabel();
    }

    public String irisLabel() {
        return this.iris.stateLabel();
    }

    public BackendKind backendKind() {
        return this.backend.kind();
    }

    public String backendName() {
        return this.backend.backendName();
    }

    public String backendMessage() {
        return this.backend.message();
    }

    public String irisMessage() {
        return this.iris.message();
    }

    public long worldGeneration() {
        return this.dirtyRegions.worldGeneration();
    }

    public int pendingDirtyRegionCount() {
        return this.dirtyRegions.pendingDirtyRegionCount();
    }

    public long uploadGeneration() {
        return this.uploads.lastGeneration();
    }

    public long uploadWorldGeneration() {
        return this.uploads.lastWorldGeneration();
    }

    public long uploadMaterialGeneration() {
        return this.uploads.lastMaterialGeneration();
    }

    public long pendingWorldUploadLag() {
        return this.dirtyRegions.pendingUploadLag(this.uploads);
    }

    public Map<String, Double> cpuScopeDurationsMillis() {
        return this.frameTimings.cpuScopeDurationsMillis();
    }

    public Map<String, Double> gpuScopeDurationsMillis() {
        return this.frameTimings.gpuScopeDurationsMillis();
    }

    public int activeCpuScopeCount() {
        return this.frameTimings.activeCpuScopeCount();
    }

    public boolean hasCpuTimings() {
        return this.frameTimings.hasCpuTimings();
    }

    public boolean hasGpuTimings() {
        return this.frameTimings.hasGpuTimings();
    }

    public FrameHookStage frameStage() {
        return this.frameLifecycle.stage();
    }

    public FramePassIntent framePassIntent() {
        return this.frameLifecycle.passIntent();
    }

    public BorrowedVulkanContextAcquisition frameContextAcquisition() {
        return this.frameLifecycle.contextAcquisition();
    }

    public String compactStatusLine() {
        return "Lucerna renderer=%s backend=%s native=%s iris=%s context=%s frameStage=%s dirty=%d worldGen=%d uploadWorldGen=%d uploadMatGen=%d cpuScopes=%d gpuScopes=%d"
                .formatted(
                        this.rendererStateLabel(),
                        this.backend.kind().name(),
                        this.nativeBridge.stateLabel(),
                        this.iris.stateLabel(),
                        this.frameLifecycle.contextStatus().name(),
                        this.frameLifecycle.stage().name(),
                        this.pendingDirtyRegionCount(),
                        this.worldGeneration(),
                        this.uploadWorldGeneration(),
                        this.uploadMaterialGeneration(),
                        this.cpuScopeDurationsMillis().size(),
                        this.gpuScopeDurationsMillis().size()
                );
    }

    public Map<String, String> validationFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("renderer.enabled", Boolean.toString(this.rendererEnabled));
        fields.put("renderer.active", Boolean.toString(this.rendererActive));
        fields.put("renderer.state", this.rendererStateLabel());
        fields.put("quality.preset", this.qualityPreset.name());
        fields.put("debug.overlay", this.debugOverlay.name());
        fields.put("backend.kind", this.backend.kind().name());
        fields.put("backend.active", Boolean.toString(this.backend.active()));
        fields.put("backend.name", this.backend.backendName());
        fields.put("backend.message", this.backend.message());
        fields.put("native.statusAvailable", Boolean.toString(this.nativeBridge.statusAvailable()));
        fields.put("native.loadAttempted", Boolean.toString(this.nativeBridge.loadAttempted()));
        fields.put("native.loaded", Boolean.toString(this.nativeBridge.loaded()));
        fields.put("native.available", Boolean.toString(this.nativeBridge.available()));
        fields.put("native.initialized", Boolean.toString(this.nativeBridge.initialized()));
        fields.put("native.state", this.nativeBridge.stateLabel());
        fields.put("native.status", this.nativeBridge.nativeStatus());
        fields.put("native.lastError", this.nativeBridge.lastError());
        fields.put("iris.statusAvailable", Boolean.toString(this.iris.statusAvailable()));
        fields.put("iris.installed", Boolean.toString(this.iris.installed()));
        fields.put("iris.disableAttempted", Boolean.toString(this.iris.disableAttempted()));
        fields.put("iris.shadersDisabledForLucerna", Boolean.toString(this.iris.shadersDisabledForLucerna()));
        fields.put("iris.shaderPackState", this.iris.shaderPackState().name());
        fields.put("iris.message", this.iris.message());
        fields.put("dirty.pendingRegions", Integer.toString(this.pendingDirtyRegionCount()));
        fields.put("dirty.worldGeneration", Long.toString(this.worldGeneration()));
        fields.put("dirty.pendingWorldUploadLag", Long.toString(this.pendingWorldUploadLag()));
        fields.put("upload.lastGeneration", Long.toString(this.uploadGeneration()));
        fields.put("upload.lastWorldGeneration", Long.toString(this.uploadWorldGeneration()));
        fields.put("upload.lastMaterialGeneration", Long.toString(this.uploadMaterialGeneration()));
        fields.put("frame.index", Long.toString(this.frameLifecycle.frameIndex()));
        fields.put("frame.stage", this.frameLifecycle.stage().name());
        fields.put("frame.passIntent", this.frameLifecycle.passIntent().name());
        fields.put("frame.viewport", this.frameLifecycle.viewportWidth() + "x" + this.frameLifecycle.viewportHeight());
        fields.put("frame.resizePending", Boolean.toString(this.frameLifecycle.resizePending()));
        fields.put("frame.open", Boolean.toString(this.frameLifecycle.frameOpen()));
        fields.put("frame.lightingSubmitted", Boolean.toString(this.frameLifecycle.lightingSubmitted()));
        fields.put("frame.context.status", this.frameLifecycle.contextStatus().name());
        fields.put("frame.context.ready", Boolean.toString(this.frameLifecycle.contextReady()));
        fields.put("frame.context.source", this.frameLifecycle.contextAcquisition().source());
        fields.put("frame.context.message", this.frameLifecycle.contextMessage());
        fields.put("frame.cpuScopeCount", Integer.toString(this.cpuScopeDurationsMillis().size()));
        fields.put("frame.gpuScopeCount", Integer.toString(this.gpuScopeDurationsMillis().size()));
        fields.put("frame.activeCpuScopeCount", Integer.toString(this.activeCpuScopeCount()));
        fields.put("frame.activeCpuScopes", String.join(",", this.frameTimings.activeCpuScopeNames()));
        fields.put("frame.totalCpuMillis", Double.toString(this.frameTimings.totalCpuMillis()));
        fields.put("frame.totalGpuMillis", Double.toString(this.frameTimings.totalGpuMillis()));
        fields.put("captured.nanos", Long.toString(this.capturedNanos));
        return Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    private static FrameLifecycleSnapshot emptyFrameLifecycle() {
        return new FrameLifecycleSnapshot(
                0L,
                FrameHookStage.IDLE,
                FramePassIntent.NONE,
                0,
                0,
                false,
                false,
                false,
                BorrowedVulkanContextAcquisition.absent("Frame lifecycle has not been reported."),
                "Frame lifecycle has not been reported."
        );
    }
}
