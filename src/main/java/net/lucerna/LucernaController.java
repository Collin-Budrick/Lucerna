package net.lucerna;

import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;
import net.lucerna.compat.iris.IrisCompat;
import net.lucerna.compat.sodium.LucernaBackendDetector;
import net.lucerna.config.DebugOverlay;
import net.lucerna.config.LucernaConfig;
import net.lucerna.config.LucernaConfigManager;
import net.lucerna.material.MaterialRegistry;
import net.lucerna.material.extract.LucernaMaterialExtractionService;
import net.lucerna.material.extract.MaterialTableRefreshResult;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.render.gbuffer.GBufferWriteIntent;
import net.lucerna.render.gbuffer.PrimaryVoxelGBufferPassPlan;
import net.lucerna.render.LucernaFrameHooks;
import net.lucerna.render.context.MojangVulkanBorrowedContextProbe;
import net.lucerna.render.frame.FrameConstantsCapture;
import net.lucerna.render.frame.FrameRenderFlags;
import net.lucerna.render.frame.LucernaFrameConstantsCollector;
import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.voxel.VoxelRay;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.render.voxel.VoxelTraversalRequest;
import net.lucerna.upload.NativeGBufferStagingUpload;
import net.lucerna.telemetry.LucernaTelemetry;
import net.lucerna.upload.NativeSectionSnapshotUpload;
import net.lucerna.upload.NativeStagedUploadBatch;
import net.lucerna.upload.NativeUploadQueue;
import net.lucerna.world.LucernaWorldFeed;
import net.lucerna.world.extract.ChunkSectionSnapshotExtractionResult;
import net.lucerna.world.extract.LucernaSectionSnapshotExtractionCoordinator;
import net.lucerna.world.extract.MinecraftChunkSectionSnapshotExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;

import java.util.LinkedHashMap;
import java.util.List;

public final class LucernaController {
    private static final LucernaController INSTANCE = new LucernaController();

    private final LucernaConfigManager configManager = new LucernaConfigManager();
    private final LucernaBackendDetector backendDetector = new LucernaBackendDetector();
    private final IrisCompat irisCompat = new IrisCompat();
    private final LucernaNativeBridge nativeBridge = new LucernaNativeBridge();
    private final LucernaWorldFeed worldFeed = new LucernaWorldFeed();
    private final MaterialRegistry materialRegistry = new MaterialRegistry();
    private final LucernaMaterialExtractionService materialExtractionService = new LucernaMaterialExtractionService(this.materialRegistry);
    private final LucernaSectionSnapshotExtractionCoordinator sectionSnapshotCoordinator =
            new LucernaSectionSnapshotExtractionCoordinator(new MinecraftChunkSectionSnapshotExtractor(this.materialExtractionService));
    private final NativeUploadQueue uploadQueue = new NativeUploadQueue();
    private final LucernaTelemetry telemetry = new LucernaTelemetry();
    private final LucernaFrameHooks frameHooks = new LucernaFrameHooks(
            nativeBridge,
            telemetry,
            MojangVulkanBorrowedContextProbe.instance()
    );
    private final LucernaFrameConstantsCollector frameConstantsCollector = new LucernaFrameConstantsCollector();

    private BackendStatus backendStatus = BackendStatus.disabled(BackendKind.UNKNOWN, "Lucerna has not checked the renderer backend yet.");
    private boolean initialized;
    private boolean nativeInitialized;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private long frameConstantsFrameIndex;
    private String lastLoggedBackendStatusKey = "";
    private String lastLoggedFrameContextKey = "";
    private String lastLoggedFrameConstantsKey = "";
    private String lastLoggedSectionExtractionKey = "";
    private String lastPreparedGBufferStagingKey = "";
    private String lastLoggedGBufferStagingKey = "";
    private String lastLoggedFirstPassPlanKey = "";
    private long gBufferStagingGeneration;
    private FrameConstantsCapture frameConstantsCapture = FrameConstantsCapture.unavailable(
            "Frame constants have not been captured by Fabric level extraction yet."
    );

    private LucernaController() {
    }

    public static LucernaController getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (this.initialized) {
            return;
        }

        this.initialized = true;
        this.configManager.load();
        this.refreshBackendStatus();
        Lucerna.LOGGER.info("Lucerna initialized: {}", this.backendStatus.userMessage());
    }

    public void tick() {
        if (!this.initialized) {
            return;
        }

        this.refreshBackendStatus();
        this.activateOrDeactivateNative();

        if (this.isRendererActive()) {
            this.irisCompat.disableIrisShadersForLucerna();
            this.ensureMaterialTablePrepared();
            var client = Minecraft.getInstance();
            var sectionExtraction = this.sectionSnapshotCoordinator.drainAndExtract(client, this.worldFeed);
            var materialUpdates = this.materialRegistry.snapshotUpdatesAfter(this.uploadQueue.lastMaterialGeneration());
            var sectionUploads = sectionExtraction.sectionSnapshots().stream()
                    .map(handoff -> NativeSectionSnapshotUpload.from(handoff.snapshot(), handoff.dirtyRegion()))
                    .toList();
            var gBufferUploads = this.prepareGBufferStagingUploads(client, sectionUploads, materialUpdates.generation());
            var stagedBatch = this.uploadQueue.acceptWorldMaterialAndStagingDeltas(
                    sectionExtraction.dirtyRegionSnapshot(),
                    materialUpdates,
                    sectionUploads,
                    gBufferUploads
            );
            var firstPassPlan = this.buildPrimaryGBufferPassPlan(stagedBatch);
            this.nativeBridge.uploadWorldDeltas(stagedBatch.worldAndMaterialBatch());
            this.nativeBridge.uploadSectionSnapshots(stagedBatch);
            this.nativeBridge.uploadGBufferStaging(stagedBatch);
            this.logSectionExtractionStatusIfChanged(sectionExtraction);
            this.logGBufferStagingStatusIfChanged(stagedBatch);
            this.logFirstPassPlanIfChanged(firstPassPlan);
            this.submitNoOpFrame(0.0F);
        }
    }

    public void shutdown() {
        if (this.nativeInitialized) {
            this.nativeBridge.shutdown();
            this.nativeInitialized = false;
        }
        this.telemetry.clear();
        this.sectionSnapshotCoordinator.clearCache();
        this.frameConstantsCollector.reset();
        this.frameConstantsFrameIndex = 0L;
        this.frameConstantsCapture = FrameConstantsCapture.unavailable("Lucerna is shutting down.");
        this.resetGBufferPlanning();
        Lucerna.LOGGER.info("Lucerna shutdown complete.");
    }

    public boolean isRendererActive() {
        return this.getConfig().rendererEnabled()
                && this.backendStatus.active()
                && this.backendStatus.kind() == BackendKind.SODIUM_VULKAN
                && this.nativeBridge.isAvailable();
    }

    public LucernaConfig getConfig() {
        return this.configManager.config();
    }

    public LucernaConfigManager configManager() {
        return this.configManager;
    }

    public BackendStatus backendStatus() {
        return this.backendStatus;
    }

    public IrisCompat irisCompat() {
        return this.irisCompat;
    }

    public LucernaWorldFeed worldFeed() {
        return this.worldFeed;
    }

    public MaterialRegistry materialRegistry() {
        return this.materialRegistry;
    }

    public LucernaMaterialExtractionService materialExtractionService() {
        return this.materialExtractionService;
    }

    public NativeUploadQueue uploadQueue() {
        return this.uploadQueue;
    }

    public LucernaTelemetry telemetry() {
        return this.telemetry;
    }

    public LucernaFrameHooks frameHooks() {
        return this.frameHooks;
    }

    public FrameConstantsCapture frameConstantsCapture() {
        return this.frameConstantsCapture;
    }

    public LucernaFrameConstants frameConstants() {
        return this.frameConstantsCapture.constants();
    }

    public LucernaNativeBridge.NativeBridgeStatus nativeBridgeStatus() {
        return this.nativeBridge.status();
    }

    public void onViewportChanged(int width, int height) {
        if (width == this.viewportWidth && height == this.viewportHeight) {
            return;
        }

        this.viewportWidth = width;
        this.viewportHeight = height;
        this.frameConstantsCollector.requestHistoryReset("Viewport changed to " + width + "x" + height + ".");
        this.frameHooks.onResize(width, height);
    }

    public void captureFrameConstants(Object renderContext, float tickDelta) {
        if (!this.initialized || renderContext == null) {
            return;
        }

        long frameIndex = ++this.frameConstantsFrameIndex;
        this.frameConstantsCapture = this.frameConstantsCollector.captureMinecraftContext(
                Minecraft.getInstance(),
                renderContext,
                this.currentFrameRenderFlags(),
                frameIndex,
                tickDelta
        );
        this.logFrameConstantsStatusIfChanged();
    }

    public void requestFrameHistoryReset(String reason) {
        this.frameConstantsCollector.requestHistoryReset(reason);
    }

    public MaterialTableRefreshResult refreshMaterials(ModelManager modelManager) {
        MaterialTableRefreshResult result = this.materialExtractionService.refreshKnownBlockStateMaterials(modelManager);
        if (result.failedStateCount() > 0) {
            Lucerna.LOGGER.warn(
                    "Lucerna material refresh completed with {} failures out of {} planned states; materialGeneration={} materialCount={}.",
                    result.failedStateCount(),
                    result.plannedStateCount(),
                    result.generationAfter(),
                    result.materialCountAfter()
            );
        } else {
            Lucerna.LOGGER.info(
                    "Lucerna material refresh complete: states={} materials={} generation={} created={}.",
                    result.registeredStateCount(),
                    result.materialCountAfter(),
                    result.generationAfter(),
                    result.createdMaterialCount()
            );
        }
        return result;
    }

    private void refreshBackendStatus() {
        this.backendStatus = this.backendDetector.detect();
        this.logBackendStatusIfChanged();
    }

    private void activateOrDeactivateNative() {
        if (!this.getConfig().rendererEnabled() || !this.backendStatus.active()) {
            if (this.nativeInitialized) {
                this.nativeBridge.shutdown();
                this.nativeInitialized = false;
                this.sectionSnapshotCoordinator.clearCache();
                this.resetGBufferPlanning();
            }
            return;
        }

        if (this.backendStatus.kind() != BackendKind.SODIUM_VULKAN) {
            return;
        }

        if (!this.nativeBridge.isLoaded()) {
            this.nativeBridge.load();
        }

        if (!this.nativeBridge.isAvailable() || this.nativeInitialized) {
            return;
        }

        this.nativeInitialized = this.nativeBridge.init();
        if (!this.nativeInitialized) {
            Lucerna.LOGGER.warn("Lucerna native renderer did not initialize: {}", this.nativeBridge.lastError());
        } else {
            Lucerna.LOGGER.info("Lucerna native renderer initialized.");
        }
    }

    private void ensureMaterialTablePrepared() {
        if (this.materialExtractionService.lastRefreshResult().isPresent()) {
            return;
        }

        this.refreshMaterials(null);
    }

    private List<NativeGBufferStagingUpload> prepareGBufferStagingUploads(
            Minecraft client,
            List<NativeSectionSnapshotUpload> sectionUploads,
            long materialGeneration
    ) {
        int[] dimensions = this.currentViewportDimensions(client);
        int width = dimensions[0];
        int height = dimensions[1];
        if (width <= 0 || height <= 0) {
            return List.of();
        }

        long sourceGeneration = Math.max(materialGeneration, this.maxSectionUploadGeneration(sectionUploads));
        String stagingKey = width
                + "x"
                + height
                + "|"
                + sourceGeneration;
        if (stagingKey.equals(this.lastPreparedGBufferStagingKey)) {
            return List.of();
        }

        this.lastPreparedGBufferStagingKey = stagingKey;
        this.gBufferStagingGeneration = Math.max(this.gBufferStagingGeneration + 1L, Math.max(1L, sourceGeneration));
        return List.of(NativeGBufferStagingUpload.lucernaMain(width, height, this.gBufferStagingGeneration));
    }

    private PrimaryVoxelGBufferPassPlan buildPrimaryGBufferPassPlan(NativeStagedUploadBatch stagedBatch) {
        if (stagedBatch == null || stagedBatch.gBufferStaging().isEmpty()) {
            return null;
        }

        NativeGBufferStagingUpload upload = stagedBatch.gBufferStaging().get(stagedBatch.gBufferStaging().size() - 1);
        var sectionReferences = this.uniqueSectionReferences(stagedBatch.sectionSnapshots());
        var writeIntent = GBufferWriteIntent.lucernaMain(upload.width(), upload.height(), upload.generation());
        var traversalRequest = VoxelTraversalRequest.primaryGBuffer(
                this.frameHooks.frameIndex(),
                new VoxelRay(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 512.0F),
                sectionReferences
        );
        return PrimaryVoxelGBufferPassPlan.from(writeIntent, traversalRequest, sectionReferences);
    }

    private List<VoxelSectionSnapshotReference> uniqueSectionReferences(List<NativeSectionSnapshotUpload> sectionUploads) {
        var referencesByKey = new LinkedHashMap<String, VoxelSectionSnapshotReference>();
        for (NativeSectionSnapshotUpload sectionUpload : sectionUploads) {
            VoxelSectionSnapshotReference reference = VoxelSectionSnapshotReference.from(sectionUpload);
            VoxelSectionSnapshotReference existing = referencesByKey.get(reference.stableKey());
            if (existing == null || reference.combinedGeneration() >= existing.combinedGeneration()) {
                referencesByKey.put(reference.stableKey(), reference);
            }
        }
        return List.copyOf(referencesByKey.values());
    }

    private int[] currentViewportDimensions(Minecraft client) {
        int width = this.viewportWidth;
        int height = this.viewportHeight;
        if ((width <= 0 || height <= 0) && client != null && client.getWindow() != null) {
            width = client.getWindow().getWidth();
            height = client.getWindow().getHeight();
        }
        return new int[]{Math.max(0, width), Math.max(0, height)};
    }

    private long maxSectionUploadGeneration(List<NativeSectionSnapshotUpload> sectionUploads) {
        return sectionUploads.stream()
                .mapToLong(NativeSectionSnapshotUpload::combinedGeneration)
                .max()
                .orElse(0L);
    }

    private void resetGBufferPlanning() {
        this.lastPreparedGBufferStagingKey = "";
        this.lastLoggedGBufferStagingKey = "";
        this.lastLoggedFirstPassPlanKey = "";
        this.gBufferStagingGeneration = 0L;
    }

    private FrameRenderFlags currentFrameRenderFlags() {
        boolean active = this.isRendererActive();
        return new FrameRenderFlags(
                this.getConfig().qualityPreset(),
                this.getConfig().debugOverlay(),
                this.getConfig().rendererEnabled(),
                active,
                active,
                active,
                active,
                active,
                this.getConfig().debugOverlay() != DebugOverlay.OFF
        );
    }

    private void submitNoOpFrame(float tickDelta) {
        var beginResult = this.frameHooks.beginFrame(this.backendStatus, tickDelta);
        this.logFrameContextStatusIfChanged();
        if (!beginResult.accepted()) {
            return;
        }

        this.frameHooks.renderLighting();
        this.frameHooks.endFrame();
        this.logFrameContextStatusIfChanged();
    }

    private void logFrameContextStatusIfChanged() {
        var snapshot = this.frameHooks.snapshot();
        var acquisition = snapshot.contextAcquisition();
        String logKey = acquisition.status().name()
                + "|"
                + acquisition.source()
                + "|"
                + acquisition.message();
        if (logKey.equals(this.lastLoggedFrameContextKey)) {
            return;
        }

        this.lastLoggedFrameContextKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna frame context {} via {}: {}",
                acquisition.status(),
                acquisition.source(),
                acquisition.message()
        );
    }

    private void logBackendStatusIfChanged() {
        String logKey = this.backendStatus.diagnosticSummary();
        if (logKey.equals(this.lastLoggedBackendStatusKey)) {
            return;
        }

        this.lastLoggedBackendStatusKey = logKey;
        Lucerna.LOGGER.info("Lucerna backend status: {}", this.backendStatus.diagnosticSummary());
    }

    private void logFrameConstantsStatusIfChanged() {
        String logKey = this.frameConstantsCapture.stateLabel()
                + "|"
                + this.frameConstantsCapture.source()
                + "|"
                + this.frameConstantsCapture.message();
        if (logKey.equals(this.lastLoggedFrameConstantsKey)) {
            return;
        }

        this.lastLoggedFrameConstantsKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna frame constants {} via {}: {}",
                this.frameConstantsCapture.stateLabel(),
                this.frameConstantsCapture.source(),
                this.frameConstantsCapture.message()
        );
    }

    private void logSectionExtractionStatusIfChanged(ChunkSectionSnapshotExtractionResult result) {
        if (result.dirtyRegionSnapshot().isEmpty() && result.cachedSectionCount() == 0) {
            return;
        }

        String logKey = result.extractedSectionCount()
                + "|"
                + result.skippedSectionCount()
                + "|"
                + result.cachedSectionCount()
                + "|"
                + this.uploadQueue.lastSectionGeneration()
                + "|"
                + this.uploadQueue.lastSectionDirtyRegionGeneration();
        if (logKey.equals(this.lastLoggedSectionExtractionKey)) {
            return;
        }

        this.lastLoggedSectionExtractionKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna section staging: extracted={} skipped={} cached={} sectionGeneration={} dirtyRegionGeneration={}.",
                result.extractedSectionCount(),
                result.skippedSectionCount(),
                result.cachedSectionCount(),
                this.uploadQueue.lastSectionGeneration(),
                this.uploadQueue.lastSectionDirtyRegionGeneration()
        );
    }

    private void logGBufferStagingStatusIfChanged(NativeStagedUploadBatch stagedBatch) {
        if (stagedBatch == null || stagedBatch.gBufferStaging().isEmpty()) {
            return;
        }

        NativeGBufferStagingUpload upload = stagedBatch.gBufferStaging().get(stagedBatch.gBufferStaging().size() - 1);
        String logKey = upload.passId()
                + "|"
                + upload.generation()
                + "|"
                + upload.width()
                + "x"
                + upload.height()
                + "|"
                + upload.attachmentCount();
        if (logKey.equals(this.lastLoggedGBufferStagingKey)) {
            return;
        }

        this.lastLoggedGBufferStagingKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna G-buffer staging prepared: pass={} generation={} size={}x{} attachments={}.",
                upload.passId(),
                upload.generation(),
                upload.width(),
                upload.height(),
                upload.attachmentCount()
        );
    }

    private void logFirstPassPlanIfChanged(PrimaryVoxelGBufferPassPlan plan) {
        if (plan == null) {
            return;
        }

        var metadata = plan.outputMetadata();
        String logKey = plan.writeIntent().generation()
                + "|"
                + plan.readyForCpuPlanning()
                + "|"
                + metadata.sectionCount()
                + "|"
                + metadata.occupiedVoxelCount()
                + "|"
                + plan.findings().size();
        if (logKey.equals(this.lastLoggedFirstPassPlanKey)) {
            return;
        }

        this.lastLoggedFirstPassPlanKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna primary G-buffer first-pass plan: ready={} valid={} sections={} occupiedVoxels={} materialSections={} emissiveSections={} writes={} findings={}.",
                plan.readyForCpuPlanning(),
                plan.valid(),
                metadata.sectionCount(),
                metadata.occupiedVoxelCount(),
                metadata.materialPayloadSectionCount(),
                metadata.emissivePayloadSectionCount(),
                metadata.expectedAttachmentWriteCount(),
                plan.findings().size()
        );
    }
}
