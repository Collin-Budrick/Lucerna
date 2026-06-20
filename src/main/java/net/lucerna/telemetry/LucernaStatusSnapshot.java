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
        FrameConstantsTelemetryStatus frameConstants,
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
            uploads = UploadGenerationTelemetryStatus.empty();
        }
        if (frameLifecycle == null) {
            frameLifecycle = emptyFrameLifecycle();
        }
        if (frameConstants == null) {
            frameConstants = FrameConstantsTelemetryStatus.unavailable(frameLifecycle, capturedNanos);
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
                FrameConstantsTelemetryStatus.unavailable(emptyFrameLifecycle(), capturedNanos),
                new FrameTimingTelemetryStatus(cpuScopeDurationsMillis, Map.of(), List.of(), activeCpuScopeCount),
                capturedNanos
        );
    }

    public static LucernaStatusSnapshot capture(LucernaController controller) {
        BackendStatus backendStatus = controller.backendStatus();
        FrameLifecycleSnapshot frameLifecycle = controller.frameHooks().snapshot();
        long capturedNanos = System.nanoTime();
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
                frameLifecycle,
                FrameConstantsTelemetryStatus.from(controller.frameConstants(), frameLifecycle, capturedNanos),
                FrameTimingTelemetryStatus.from(controller.telemetry()),
                capturedNanos
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

    public long uploadSectionGeneration() {
        return this.uploads.lastSectionGeneration();
    }

    public long uploadSectionMaterialGeneration() {
        return this.uploads.lastSectionMaterialGeneration();
    }

    public long uploadSectionOccupancyGeneration() {
        return this.uploads.lastSectionOccupancyGeneration();
    }

    public long uploadSectionEmissiveGeneration() {
        return this.uploads.lastSectionEmissiveGeneration();
    }

    public long uploadSectionDirtyRegionGeneration() {
        return this.uploads.lastSectionDirtyRegionGeneration();
    }

    public long uploadGBufferStagingGeneration() {
        return this.uploads.lastGBufferStagingGeneration();
    }

    public long uploadStagingGeneration() {
        return this.uploads.stagingGeneration();
    }

    public int stagedSectionSnapshotCount() {
        return this.uploads.stagedSectionSnapshotCount();
    }

    public int stagedGBufferStagingCount() {
        return this.uploads.stagedGBufferStagingCount();
    }

    public String uploadGenerationLabel() {
        return this.uploads.compactGenerationLabel();
    }

    public String sectionGenerationLabel() {
        return this.uploads.compactSectionGenerationLabel();
    }

    public String sectionSnapshotStagingLabel() {
        return this.uploads.compactSectionSnapshotLabel();
    }

    public String gBufferStagingLabel() {
        return this.uploads.compactGBufferStagingLabel();
    }

    public String explicitGBufferStagingLabel() {
        return this.uploads.explicitGBufferStagingLabel();
    }

    public String stagingPayloadLabel() {
        return this.uploads.compactStagingPayloadLabel();
    }

    public NativePassTelemetryStatus nativePassStates() {
        return this.nativeBridge.nativePassStates();
    }

    public String nativePassStateLabel() {
        return this.nativePassStates().compactLabel();
    }

    public LightingDispatchTelemetryStatus lightingDispatchStatus() {
        return this.nativeBridge.lightingDispatchStatus();
    }

    public String lightingDispatchStatusLabel() {
        return this.lightingDispatchStatus().compactLabel();
    }

    public FirstPassValidationTelemetryStatus firstPassValidation() {
        return FirstPassValidationTelemetryStatus.placeholder(this);
    }

    public String firstPassValidationSummary() {
        return this.firstPassValidation().summary();
    }

    public String framePassStatusLabel() {
        return "%s/%s attachable=%s".formatted(
                this.frameLifecycle.framePassStatusCode().name(),
                this.frameLifecycle.framePassStatus().kind().name(),
                Boolean.toString(this.frameLifecycle.framePassAttachable())
        );
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

    public String frameConstantsLabel() {
        return this.frameConstants.stateLabel();
    }

    public boolean frameConstantsRequiredAvailable() {
        return this.frameConstants.requiredConstantsAvailable();
    }

    public boolean frameConstantsFresh() {
        return this.frameConstants.freshForFrame();
    }

    public String compactStatusLine() {
        return "Lucerna renderer=%s backend=%s native=%s iris=%s context=%s frameStage=%s constants=%s dirty=%d worldGen=%d uploadWorldGen=%d uploadMatGen=%d sectionGen=%d gbuffer=%d/%d lighting=%s firstPass=%s cpuScopes=%d gpuScopes=%d"
                .formatted(
                        this.rendererStateLabel(),
                        this.backend.kind().name(),
                        this.nativeBridge.stateLabel(),
                        this.iris.stateLabel(),
                        this.frameLifecycle.contextStatus().name(),
                        this.frameLifecycle.stage().name(),
                        this.frameConstants.stateLabel(),
                        this.pendingDirtyRegionCount(),
                        this.worldGeneration(),
                        this.uploadWorldGeneration(),
                        this.uploadMaterialGeneration(),
                        this.uploadSectionGeneration(),
                        this.stagedGBufferStagingCount(),
                        this.uploadGBufferStagingGeneration(),
                        this.lightingDispatchStatusLabel(),
                        this.firstPassValidation().placeholder() ? "placeholder" : "reported",
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
        fields.put("native.diagnostic", this.nativeBridge.diagnosticMessage());
        fields.put("native.passStates", this.nativePassStateLabel());
        fields.put("native.passTimingBoundary", this.nativePassStates().compactTimingBoundaryLabel());
        fields.putAll(this.nativePassStates().validationFields("native.pass"));
        fields.putAll(this.lightingDispatchStatus().validationFields("lighting.dispatch"));
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
        fields.put("upload.lastSectionGeneration", Long.toString(this.uploadSectionGeneration()));
        fields.put("upload.lastSectionMaterialGeneration", Long.toString(this.uploadSectionMaterialGeneration()));
        fields.put("upload.lastSectionOccupancyGeneration", Long.toString(this.uploadSectionOccupancyGeneration()));
        fields.put("upload.lastSectionEmissiveGeneration", Long.toString(this.uploadSectionEmissiveGeneration()));
        fields.put("upload.lastSectionDirtyRegionGeneration", Long.toString(this.uploadSectionDirtyRegionGeneration()));
        fields.put("upload.lastGBufferStagingGeneration", Long.toString(this.uploadGBufferStagingGeneration()));
        fields.put("upload.gbuffer.stagedCount", Integer.toString(this.stagedGBufferStagingCount()));
        fields.put("upload.gbuffer.lastGeneration", Long.toString(this.uploadGBufferStagingGeneration()));
        fields.put("upload.gbuffer.stagingGeneration", Long.toString(this.uploadStagingGeneration()));
        fields.put("upload.gbuffer.combinedGeneration", Long.toString(this.uploadGeneration()));
        fields.put("upload.gbuffer.hasPayload", Boolean.toString(this.uploads.hasGBufferStaging()));
        fields.put("upload.gbuffer.hasGeneration", Boolean.toString(this.uploads.hasGBufferStagingGeneration()));
        fields.put("upload.stagingGeneration", Long.toString(this.uploadStagingGeneration()));
        fields.put("upload.generations", this.uploadGenerationLabel());
        fields.put("upload.sectionGenerations", this.sectionGenerationLabel());
        fields.put("upload.sectionSnapshots", this.sectionSnapshotStagingLabel());
        fields.put("upload.gbufferStaging", this.gBufferStagingLabel());
        fields.put("upload.gbufferStagingExplicit", this.explicitGBufferStagingLabel());
        fields.put("upload.stagingPayloads", this.stagingPayloadLabel());
        fields.put("frame.index", Long.toString(this.frameLifecycle.frameIndex()));
        fields.put("frame.stage", this.frameLifecycle.stage().name());
        fields.put("frame.passIntent", this.frameLifecycle.passIntent().name());
        fields.put("frame.passStatus", this.framePassStatusLabel());
        fields.put("frame.passStatusCode", this.frameLifecycle.framePassStatusCode().name());
        fields.put("frame.passKind", this.frameLifecycle.framePassStatus().kind().name());
        fields.put("frame.passAttachable", Boolean.toString(this.frameLifecycle.framePassAttachable()));
        fields.put("frame.passMessage", this.frameLifecycle.framePassMessage());
        fields.put("frame.viewport", this.frameLifecycle.viewportWidth() + "x" + this.frameLifecycle.viewportHeight());
        fields.put("frame.resizePending", Boolean.toString(this.frameLifecycle.resizePending()));
        fields.put("frame.open", Boolean.toString(this.frameLifecycle.frameOpen()));
        fields.put("frame.lightingSubmitted", Boolean.toString(this.frameLifecycle.lightingSubmitted()));
        fields.put("frame.context.status", this.frameLifecycle.contextStatus().name());
        fields.put("frame.context.ready", Boolean.toString(this.frameLifecycle.contextReady()));
        fields.put("frame.context.source", this.frameLifecycle.contextAcquisition().source());
        fields.put("frame.context.message", this.frameLifecycle.contextMessage());
        fields.put("frame.constants.available", Boolean.toString(this.frameConstants.constantsAvailable()));
        fields.put("frame.constants.requiredAvailable", Boolean.toString(this.frameConstants.requiredConstantsAvailable()));
        fields.put("frame.constants.fresh", Boolean.toString(this.frameConstants.freshForFrame()));
        fields.put("frame.constants.state", this.frameConstants.stateLabel());
        fields.put("frame.constants.frameIndex", Long.toString(this.frameConstants.constantsFrameIndex()));
        fields.put("frame.constants.lifecycleFrameIndex", Long.toString(this.frameConstants.lifecycleFrameIndex()));
        fields.put("frame.constants.ageNanos", Long.toString(this.frameConstants.ageNanos()));
        fields.put("frame.constants.missingRequired", this.frameConstants.missingRequiredLabel());
        fields.put("frame.constants.message", this.frameConstants.message());
        fields.put("frame.cpuScopeCount", Integer.toString(this.cpuScopeDurationsMillis().size()));
        fields.put("frame.gpuScopeCount", Integer.toString(this.gpuScopeDurationsMillis().size()));
        fields.put("frame.activeCpuScopeCount", Integer.toString(this.activeCpuScopeCount()));
        fields.put("frame.timingAvailability", this.frameTimings.compactAvailabilityLine());
        fields.put("frame.timingBoundary", this.frameTimings.measurementBoundaryLabel());
        fields.put("frame.activeCpuScopes", String.join(",", this.frameTimings.activeCpuScopeNames()));
        fields.put("frame.totalCpuMillis", Double.toString(this.frameTimings.totalCpuMillis()));
        fields.put("frame.totalGpuMillis", Double.toString(this.frameTimings.totalGpuMillis()));
        fields.put("firstPass.validationSummary", this.firstPassValidationSummary());
        fields.putAll(this.firstPassValidation().validationFields("firstPass"));
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
