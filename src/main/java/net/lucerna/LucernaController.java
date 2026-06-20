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
import net.lucerna.nativebridge.DirectLightingPreviewCompositeSubmissionResult;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
import net.lucerna.nativebridge.DenoisedDiffuseGiCpuOutputPayload;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.render.GBufferDescriptor;
import net.lucerna.render.gbuffer.GBufferWriteIntent;
import net.lucerna.render.gbuffer.PrimaryVoxelGBufferPassPlan;
import net.lucerna.render.LucernaFrameHooks;
import net.lucerna.render.cache.SparseVoxelRadianceCache;
import net.lucerna.render.cache.SparseVoxelRadianceCacheDebugStatus;
import net.lucerna.render.cache.SparseVoxelRadianceCacheSnapshot;
import net.lucerna.render.context.MojangVulkanBorrowedContextProbe;
import net.lucerna.render.culling.Round9CullingDebugStatus;
import net.lucerna.render.frame.FrameConstantsCapture;
import net.lucerna.render.frame.FrameRenderFlags;
import net.lucerna.render.frame.LucernaFrameConstantsCollector;
import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.lighting.direct.DirectCelestialLightingPlan;
import net.lucerna.render.lighting.direct.DirectEmissiveBlockListPlan;
import net.lucerna.render.lighting.direct.DirectLightingPlan;
import net.lucerna.render.lighting.direct.DirectShadowRayPlanner;
import net.lucerna.render.lighting.gi.AdaptiveGiRayBudgetPolicy;
import net.lucerna.render.lighting.gi.DiffuseGiSourceSummary;
import net.lucerna.render.lighting.gi.DiffuseGiSettings;
import net.lucerna.render.lighting.gi.GiCacheInvalidationPolicy;
import net.lucerna.render.lighting.gi.GiCachePlannerInputs;
import net.lucerna.render.lighting.gi.GiCacheSnapshot;
import net.lucerna.render.lighting.gi.LowResDiffuseGiPlan;
import net.lucerna.render.lighting.gi.LowResDiffuseGiPlanner;
import net.lucerna.render.lighting.gi.SparseVoxelRadianceCacheSnapshotAdapter;
import net.lucerna.render.lighting.physical.PhysicalLightingProofStatus;
import net.lucerna.render.lighting.post.PostProcessingPipelinePlan;
import net.lucerna.render.lighting.post.PostProcessingPlanBuilder;
import net.lucerna.render.mixin.PublicMojangPreviewPassSubmissionResult;
import net.lucerna.render.mixin.RenderThreadPreviewTargetFactory;
import net.lucerna.render.pass.LucernaFramePassRequest;
import net.lucerna.render.pass.LucernaFramePassResult;
import net.lucerna.render.pass.LucernaFramePassStatus;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.lucerna.render.preview.FinalCompositeModeStatus;
import net.lucerna.render.preview.PublicMojangFinalCompositeSubmissionResult;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
import net.lucerna.render.preview.Round8AdaptiveDebugStatus;
import net.lucerna.render.voxel.VoxelRay;
import net.lucerna.render.voxel.VoxelRayBudgetConfig;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.render.voxel.VoxelTraversalRequest;
import net.lucerna.upload.NativeGBufferStagingUpload;
import net.lucerna.upload.NativeDiffuseGiUploadPacket;
import net.lucerna.upload.NativeDirectLightingUploadPacket;
import net.lucerna.upload.NativeLightingDispatchUploadPacket;
import net.lucerna.upload.NativeLightingDispatchUploadPacket.Phase5Stage;
import net.lucerna.upload.NativeLightingDispatchUploadPacket.StageUpload;
import net.lucerna.upload.NativePostProcessingHandoffPacket;
import net.lucerna.telemetry.LucernaStatusSnapshot;
import net.lucerna.telemetry.LucernaTelemetry;
import net.lucerna.upload.NativeSectionSnapshotUpload;
import net.lucerna.upload.NativeStagedUploadBatch;
import net.lucerna.upload.NativeUploadQueue;
import net.lucerna.world.LucernaWorldFeed;
import net.lucerna.world.extract.ChunkSectionSnapshotExtractionResult;
import net.lucerna.world.extract.LucernaSectionSnapshotExtractionCoordinator;
import net.lucerna.world.extract.MinecraftChunkSectionSnapshotExtractor;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
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
    private final SparseVoxelRadianceCache sparseRadianceCache = new SparseVoxelRadianceCache();
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
    private String lastLoggedDirectLightingPlanKey = "";
    private String lastPreparedLightingDispatchKey = "";
    private String lastLoggedLightingDispatchKey = "";
    private String lastLoggedSparseRadianceCacheKey = "";
    private String lastLoggedDirectPreviewCompositeKey = "";
    private String lastLoggedPublicMojangPreviewPassKey = "";
    private String lastLoggedPublicMojangFinalCompositeKey = "";
    private String lastLoggedRound6DiffuseGiPreviewKey = "";
    private String lastLoggedRound7DenoisedGiCpuOutputKey = "";
    private String lastLoggedRound8AdaptiveDebugKey = "";
    private String lastLoggedRound9VirtualizedGeometryKey = "";
    private String lastLoggedRound10HybridTracingKey = "";
    private String lastLoggedRound11RestirKey = "";
    private String lastLoggedTickNoOpFrameKey = "";
    private boolean renderThreadFrameHookObserved;
    private NativeDirectLightingUploadPacket pendingDirectLightingUpload;
    private Round6DiffuseGiPreviewCompositeState round6DiffuseGiPreviewCompositeState =
            Round6DiffuseGiPreviewCompositeState.unavailable("Round 6 diffuse GI dispatch has not been prepared yet");
    private long gBufferStagingGeneration;
    private long lightingDispatchGeneration;
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
            var lightingDispatchPacket = this.prepareLightingDispatchPacket(
                    client,
                    sectionExtraction,
                    stagedBatch,
                    firstPassPlan
            );
            this.nativeBridge.uploadWorldDeltas(stagedBatch.worldAndMaterialBatch());
            this.nativeBridge.uploadSectionSnapshots(stagedBatch);
            this.nativeBridge.uploadGBufferStaging(stagedBatch);
            this.nativeBridge.uploadDirectLighting(this.pendingDirectLightingUpload);
            this.nativeBridge.uploadLightingDispatch(lightingDispatchPacket);
            this.logSectionExtractionStatusIfChanged(sectionExtraction);
            this.logGBufferStagingStatusIfChanged(stagedBatch);
            this.logFirstPassPlanIfChanged(firstPassPlan);
            this.logLightingDispatchStatusIfChanged(lightingDispatchPacket);
            this.logRound9VirtualizedGeometryIfChanged();
            this.logRound10HybridTracingIfChanged();
            this.logRound11RestirIfChanged();
            this.submitTickFallbackFrame(0.0F);
        }
    }

    public void shutdown() {
        if (this.nativeInitialized) {
            this.nativeBridge.shutdown();
            this.nativeInitialized = false;
        }
        this.telemetry.clear();
        this.sectionSnapshotCoordinator.clearCache();
        this.sparseRadianceCache.clear();
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

    public LucernaFramePassResult attachFinalWorldColorCompositeTarget(
            LucernaFramePassTarget target,
            float tickDelta
    ) {
        LucernaFramePassRequest skippedRequest = LucernaFramePassRequest.directLightPreviewComposite(
                this.frameHooks.frameIndex(),
                target,
                0.35F,
                0.35F
        );
        if (!this.initialized || !this.isRendererActive()) {
            return LucernaFramePassResult.skipped(
                    skippedRequest,
                    LucernaFramePassStatus.skipped(
                            skippedRequest.kind(),
                            skippedRequest.frameIndex(),
                            "Final world-color composite target skipped because Lucerna is not active."
                    )
            );
        }

        this.renderThreadFrameHookObserved = true;
        var beginResult = this.frameHooks.beginFrame(this.backendStatus, tickDelta);
        this.logFrameContextStatusIfChanged();
        if (!beginResult.accepted()) {
            return LucernaFramePassResult.skipped(
                    skippedRequest,
                    this.frameHooks.framePassStatus()
            );
        }

        try {
            this.frameHooks.renderLighting();
            LucernaFramePassRequest request = LucernaFramePassRequest.directLightPreviewComposite(
                    this.frameHooks.frameIndex(),
                    target,
                    0.35F,
                    0.35F
            );
            DirectLightingPreviewCompositeSubmissionResult submission =
                    this.nativeBridge.submitDirectLightingPreviewComposite(request);
            this.logDirectPreviewCompositeStatusIfChanged(submission);
            DirectLightingCpuOutputPayload directOutputPayload = this.nativeBridge.directLightingCpuOutputPayload();
            Round6DiffuseGiPreviewCompositeState giPreviewState = this.round6DiffuseGiPreviewCompositeState;
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload =
                    this.nativeBridge.round6DiffuseGiCpuOutputPayload(giPreviewState);
            this.logRound6DiffuseGiPreviewStatusIfChanged(giPreviewState, diffuseGiPayload);
            DenoisedDiffuseGiCpuOutputPayload denoisedGiPayload = this.nativeBridge.denoisedDiffuseGiCpuOutputPayload();
            this.logRound7DenoisedGiCpuOutputStatusIfChanged(denoisedGiPayload);
            FinalCompositeModeStatus modeStatus = FinalCompositeModeStatus.fromConfigMode(this.getConfig().compositeMode());
            LucernaFramePassRequest finalCompositeRequest = LucernaFramePassRequest.finalWorldColorComposite(
                    this.frameHooks.frameIndex(),
                    target,
                    0.35F,
                    0.35F
            );
            PublicMojangFinalCompositeSubmissionResult finalCompositeSubmission = this.submitRound7SelectedVisualMode(
                    modeStatus,
                    target,
                    directOutputPayload,
                    diffuseGiPayload,
                    denoisedGiPayload,
                    giPreviewState
            );
            this.logPublicMojangFinalCompositeStatusIfChanged(
                    finalCompositeSubmission,
                    modeStatus,
                    target,
                    directOutputPayload != null && directOutputPayload.readyForPreviewDraw(),
                    diffuseGiPayload != null && diffuseGiPayload.readyForPreviewDraw(),
                    denoisedGiPayload != null && denoisedGiPayload.readyForPreviewDraw(),
                    denoisedGiPayload != null
                            && denoisedGiPayload.readyForPreviewDraw()
                            && denoisedGiPayload.snapshot().realDenoiseShaderOutput()
            );
            return this.frameHooks.attachFramePass(finalCompositeRequest);
        } finally {
            this.frameHooks.endFrame();
            this.logFrameContextStatusIfChanged();
        }
    }

    private PublicMojangFinalCompositeSubmissionResult submitRound7SelectedVisualMode(
            FinalCompositeModeStatus modeStatus,
            LucernaFramePassTarget target,
            DirectLightingCpuOutputPayload directOutputPayload,
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload,
            DenoisedDiffuseGiCpuOutputPayload denoisedGiPayload,
            Round6DiffuseGiPreviewCompositeState giPreviewState
    ) {
        if (modeStatus.baselineVisualMode()) {
            return PublicMojangFinalCompositeSubmissionResult.notAttempted(
                    "Round 7 visual mode baseline requested; Lucerna final composite draw is bypassed for same-scene control screenshots. "
                            + modeStatus.validationSummary()
            );
        }
        if (modeStatus.rawGiVisualMode()) {
            return RenderThreadPreviewTargetFactory.submitRound6DiffuseGiFinalCompositePublicDraw(
                    target,
                    diffuseGiPayload,
                    giPreviewState
            );
        }
        if (modeStatus.denoisedGiVisualMode()) {
            return RenderThreadPreviewTargetFactory.submitRound7DenoisedGiFinalCompositePublicDraw(
                    target,
                    denoisedGiPayload
            );
        }
        if (modeStatus.finalCompositeVisualMode() && denoisedGiPayload != null && denoisedGiPayload.readyForPreviewDraw()) {
            return RenderThreadPreviewTargetFactory.submitRound7FinalCompositePublicDraw(
                    target,
                    diffuseGiPayload,
                    denoisedGiPayload,
                    giPreviewState
            );
        }
        if (giPreviewState.readyForFinalComposite(diffuseGiPayload)) {
            return RenderThreadPreviewTargetFactory.submitRound6DiffuseGiFinalCompositePublicDraw(
                    target,
                    diffuseGiPayload,
                    giPreviewState
            );
        }
        return RenderThreadPreviewTargetFactory.submitFinalCompositePublicDraw(target, directOutputPayload);
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

    public DirectLightingCpuOutputPayload directLightingCpuOutputPayload() {
        return this.nativeBridge.directLightingCpuOutputPayload();
    }

    public Round6DiffuseGiCpuOutputPayload round6DiffuseGiCpuOutputPayload() {
        return this.nativeBridge.round6DiffuseGiCpuOutputPayload(this.round6DiffuseGiPreviewCompositeState);
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
        var sectionReferences = this.currentSectionReferences(stagedBatch.sectionSnapshots());
        var writeIntent = GBufferWriteIntent.lucernaMain(upload.width(), upload.height(), upload.generation());
        var traversalRequest = VoxelTraversalRequest.primaryGBuffer(
                this.frameHooks.frameIndex(),
                new VoxelRay(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 512.0F),
                sectionReferences
        );
        return PrimaryVoxelGBufferPassPlan.from(writeIntent, traversalRequest, sectionReferences);
    }

    private NativeLightingDispatchUploadPacket prepareLightingDispatchPacket(
            Minecraft client,
            ChunkSectionSnapshotExtractionResult sectionExtraction,
            NativeStagedUploadBatch stagedBatch,
            PrimaryVoxelGBufferPassPlan firstPassPlan
    ) {
        this.pendingDirectLightingUpload = null;
        if (stagedBatch == null || stagedBatch.gBufferStaging().isEmpty()) {
            return null;
        }

        NativeGBufferStagingUpload gBufferUpload = stagedBatch.gBufferStaging().get(stagedBatch.gBufferStaging().size() - 1);
        if (gBufferUpload.width() <= 0 || gBufferUpload.height() <= 0) {
            return null;
        }

        int[] dimensions = this.currentViewportDimensions(client);
        int width = dimensions[0] > 0 ? dimensions[0] : gBufferUpload.width();
        int height = dimensions[1] > 0 ? dimensions[1] : gBufferUpload.height();
        var metadata = stagedBatch.metadata();
        LucernaFrameConstants frameConstants = this.frameConstants();
        GBufferWriteIntent writeIntent = firstPassPlan == null
                ? GBufferWriteIntent.lucernaMain(gBufferUpload.width(), gBufferUpload.height(), gBufferUpload.generation())
                : firstPassPlan.writeIntent();
        List<VoxelSectionSnapshotReference> sectionReferences = firstPassPlan == null
                ? this.currentSectionReferences(stagedBatch.sectionSnapshots())
                : firstPassPlan.sectionSnapshots();
        long sourceGeneration = Math.max(
                metadata.generation(),
                Math.max(gBufferUpload.generation(), Math.max(this.frameHooks.frameIndex(), frameConstants.frameIndex()))
        );
        GiCachePlannerInputs giCacheInputs = sectionExtraction == null
                ? GiCachePlannerInputs.empty()
                : GiCachePlannerInputs.from(sectionExtraction.dirtyRegionSnapshot(), sourceGeneration);
        SparseVoxelRadianceCacheSnapshot sparseCacheSnapshot = this.sparseRadianceCache.applyDirtyRegionSnapshot(
                sectionExtraction == null ? null : sectionExtraction.dirtyRegionSnapshot()
        );
        GiCacheSnapshot adaptedGiCacheSnapshot = SparseVoxelRadianceCacheSnapshotAdapter.toGiCacheSnapshot(sparseCacheSnapshot);
        DirectLightingPlan directLightingPlan = this.buildDirectLightingPlan(
                this.currentSectionSnapshots(sectionExtraction),
                sectionReferences,
                frameConstants
        );
        NativeDirectLightingUploadPacket directLightingUpload = NativeDirectLightingUploadPacket.from(directLightingPlan);
        DiffuseGiSourceSummary diffuseGiSourceSummary = DiffuseGiSourceSummary.of(
                directLightingUpload.generation(),
                metadata.lastWorldGeneration(),
                metadata.materialGeneration(),
                metadata.sectionGeneration(),
                Math.max(metadata.sectionDirtyRegionGeneration(), giCacheInputs.snapshot().latestDirtyGeneration()),
                directLightingUpload.hasDirectLightingWork(),
                directLightingUpload.celestialLightCount(),
                directLightingUpload.selectedEmissiveCount(),
                directLightingUpload.shadowCandidateCount(),
                directLightingUpload.budgetedShadowCandidateCount(),
                directLightingUpload.sectionSnapshotCount(),
                giCacheInputs.sourceDirtyRegionCount(),
                metadata.materialUpdateCount(),
                "Round 6 GI sources from direct lighting, staged world/material deltas, and dirty regions"
        );
        LowResDiffuseGiPlan diffuseGiPlan = LowResDiffuseGiPlanner.plan(
                frameConstants,
                writeIntent,
                this.frameConstantsCapture.matrixHistory(),
                adaptedGiCacheSnapshot.hasSurfaceRecords() || adaptedGiCacheSnapshot.hasRadianceRecords()
                        ? adaptedGiCacheSnapshot
                        : giCacheInputs.snapshot(),
                DiffuseGiSettings.fromQuality(this.getConfig().qualityPreset(), 2),
                AdaptiveGiRayBudgetPolicy.firstMilestone(),
                GiCacheInvalidationPolicy.conservative(),
                diffuseGiSourceSummary
        );
        NativeDiffuseGiUploadPacket diffuseGiUpload = NativeDiffuseGiUploadPacket.from(
                diffuseGiPlan,
                directLightingUpload,
                metadata
        );

        boolean directEnabled = directLightingUpload.hasDirectLightingWork() && writeIntent.dimensionsAvailable();
        boolean diffuseGiEnabled = diffuseGiUpload.readyForScheduling();
        PostProcessingPipelinePlan postPlan = this.buildPostProcessingPlan(
                frameConstants,
                width,
                height,
                sourceGeneration,
                directEnabled,
                diffuseGiEnabled
        );
        NativePostProcessingHandoffPacket postProcessingUpload = NativePostProcessingHandoffPacket.from(postPlan);
        boolean denoiseEnabled = postProcessingUpload.denoise().readyForScheduling();
        boolean compositeEnabled = postProcessingUpload.readyForNativeHandoff();
        int cacheRecordCount = diffuseGiUpload.dirtyRegionCount()
                + diffuseGiUpload.surfaceRecordCount()
                + diffuseGiUpload.radianceRecordCount();
        boolean cacheEnabled = cacheRecordCount > 0
                && (diffuseGiUpload.cacheUsable()
                || diffuseGiUpload.surfaceRecordCount() > 0
                || diffuseGiUpload.radianceRecordCount() > 0);
        this.round6DiffuseGiPreviewCompositeState = Round6DiffuseGiPreviewCompositeState.from(
                diffuseGiEnabled,
                cacheEnabled,
                cacheRecordCount,
                diffuseGiUpload
        );

        String dispatchKey = width
                + "x"
                + height
                + "|"
                + metadata.generation()
                + "|"
                + gBufferUpload.generation()
                + "|"
                + frameConstants.frameIndex()
                + "|"
                + directEnabled
                + "|"
                + diffuseGiEnabled
                + "|"
                + denoiseEnabled
                + "|"
                + compositeEnabled
                + "|"
                + directLightingPlan.emissiveBlockList().selectedLightCount()
                + "|"
                + directLightingUpload.generation()
                + "|"
                + directLightingUpload.celestialLightCount()
                + "|"
                + directLightingUpload.selectedEmissiveCount()
                + "|"
                + directLightingUpload.budgetedShadowCandidateCount()
                + "|"
                + giCacheInputs.sourceDirtyRegionCount()
                + "|"
                + giCacheInputs.coalescedDirtyRegionCount()
                + "|"
                + giCacheInputs.pendingDirtyRegionCountAfterDrain()
                + "|"
                + giCacheInputs.snapshot().latestDirtyGeneration()
                + "|"
                + diffuseGiUpload.generation()
                + "|"
                + diffuseGiUpload.dirtyRegionCount()
                + "|"
                + cacheRecordCount
                + "|"
                + sparseCacheSnapshot.cacheGeneration()
                + "|"
                + sparseCacheSnapshot.debugStatus().recordCount()
                + "|"
                + sparseCacheSnapshot.debugStatus().dirtyRecordCount()
                + "|"
                + postProcessingUpload.generation()
                + "|"
                + postProcessingUpload.flags();
        if (dispatchKey.equals(this.lastPreparedLightingDispatchKey)) {
            return null;
        }

        this.lastPreparedLightingDispatchKey = dispatchKey;
        this.lightingDispatchGeneration = Math.max(this.lightingDispatchGeneration + 1L, Math.max(1L, sourceGeneration));
        long dispatchGeneration = this.lightingDispatchGeneration;

        StageUpload directStage = this.directLightingStage(
                directEnabled,
                dispatchGeneration,
                width,
                height,
                directLightingUpload
        );
        StageUpload diffuseGiStage = this.diffuseGiStage(diffuseGiEnabled, dispatchGeneration, diffuseGiUpload);
        StageUpload denoiseStage = this.fullResolutionStage(
                Phase5Stage.DENOISE,
                denoiseEnabled,
                dispatchGeneration,
                width,
                height,
                postProcessingUpload.denoise().readResourceCount(),
                postProcessingUpload.denoise().writeResourceCount(),
                postProcessingUpload.denoise().sampleDiameterPixels()
                        * Math.max(1, postProcessingUpload.denoise().iterationCount()),
                0,
                NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER
                        | (postProcessingUpload.denoise().validated()
                        ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED
                        : 0)
                        | (postProcessingUpload.denoise().rejection().temporalReuseAllowed()
                        ? NativeLightingDispatchUploadPacket.FLAG_TEMPORAL_HISTORY
                        : 0)
        );
        StageUpload compositeStage = this.fullResolutionStage(
                Phase5Stage.COMPOSITE,
                compositeEnabled,
                dispatchGeneration,
                width,
                height,
                postProcessingUpload.composite().readResourceCount(),
                postProcessingUpload.composite().writeResourceCount(),
                1,
                0,
                NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER
                        | (postProcessingUpload.validation().valid() ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED : 0)
                        | (postProcessingUpload.composite().debugOverlayAvailable()
                        ? NativeLightingDispatchUploadPacket.FLAG_DEBUG_OVERLAY
                        : 0)
        );
        StageUpload cacheStage = this.cacheStage(cacheEnabled, dispatchGeneration, diffuseGiUpload);

        this.pendingDirectLightingUpload = directLightingUpload;
        this.logSparseRadianceCacheStatusIfChanged(sparseCacheSnapshot);
        return NativeLightingDispatchUploadPacket.of(
                dispatchGeneration,
                metadata,
                directStage,
                diffuseGiStage,
                denoiseStage,
                compositeStage,
                cacheStage
        );
    }

    private DirectLightingPlan buildDirectLightingPlan(
            List<ChunkSectionVoxelSnapshot> sectionSnapshots,
            List<VoxelSectionSnapshotReference> sectionReferences,
            LucernaFrameConstants frameConstants
    ) {
        LucernaFrameConstants resolvedFrameConstants = frameConstants == null
                ? LucernaFrameConstants.unavailable()
                : frameConstants;
        DirectCelestialLightingPlan celestialLighting = DirectCelestialLightingPlan.fromFrameConstants(resolvedFrameConstants);
        DirectEmissiveBlockListPlan emissiveBlockList = DirectEmissiveBlockListPlan.fromSectionSnapshots(
                sectionSnapshots == null ? List.of() : sectionSnapshots,
                this.maxSelectedEmissiveLights()
        );
        var shadowRayPlan = DirectShadowRayPlanner.plan(
                celestialLighting,
                emissiveBlockList,
                new VoxelRayBudgetConfig(0, 1, 0, this.shadowRayBudget(), 512, 64),
                sectionReferences == null ? List.of() : sectionReferences
        );
        DirectLightingPlan directLightingPlan = DirectLightingPlan.from(celestialLighting, emissiveBlockList, shadowRayPlan);
        this.logDirectLightingPlanStatusIfChanged(directLightingPlan);
        return directLightingPlan;
    }

    private PostProcessingPipelinePlan buildPostProcessingPlan(
            LucernaFrameConstants frameConstants,
            int width,
            int height,
            long sourceGeneration,
            boolean directLightingAvailable,
            boolean diffuseGiAvailable
    ) {
        return PostProcessingPlanBuilder.create()
                .qualityPreset(this.getConfig().qualityPreset())
                .frameInputs(
                        frameConstants == null ? LucernaFrameConstants.unavailable() : frameConstants,
                        this.frameConstantsCapture.matrixHistory(),
                        GBufferDescriptor.lucernaMain(width, height),
                        directLightingAvailable,
                        diffuseGiAvailable,
                        diffuseGiAvailable,
                        sourceGeneration,
                        sourceGeneration,
                        this.frameConstantsCapture.matrixHistory().previousFrameIndex()
                )
                .outputGeneration(sourceGeneration)
                .debugOverlayAvailable(this.getConfig().debugOverlay() != DebugOverlay.OFF)
                .borrowedWorldColorTarget(true)
                .beforeHudAndLateTranslucency(true)
                .build();
    }

    private StageUpload fullResolutionStage(
            Phase5Stage stage,
            boolean enabled,
            long generation,
            int width,
            int height,
            int inputCount,
            int outputCount,
            int sampleCount,
            int rayCount,
            int flags
    ) {
        return this.stageUpload(
                stage,
                enabled,
                generation,
                width,
                height,
                8,
                8,
                inputCount,
                outputCount,
                sampleCount,
                rayCount,
                0,
                0,
                flags
        );
    }

    private StageUpload directLightingStage(
            boolean enabled,
            long generation,
            int width,
            int height,
            NativeDirectLightingUploadPacket upload
    ) {
        int inputCount = presentResourceCount(
                upload.celestialLightCount(),
                upload.selectedEmissiveCount(),
                upload.shadowCandidateCount(),
                upload.sectionSnapshotCount()
        );
        int sampleCount = upload.shadowCandidateCount();
        int flags = NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER
                | (upload.valid() ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED : 0);
        return this.fullResolutionStage(
                Phase5Stage.DIRECT_LIGHTING,
                enabled,
                generation,
                width,
                height,
                inputCount,
                1,
                sampleCount,
                upload.budgetedShadowCandidateCount(),
                flags
        );
    }

    private StageUpload diffuseGiStage(boolean enabled, long generation, NativeDiffuseGiUploadPacket upload) {
        var plan = upload.planUpload();
        int width = plan.gridWidth();
        int height = plan.gridHeight();
        int cacheReadCount = upload.dirtyRegionCount()
                + upload.surfaceRecordCount()
                + upload.radianceRecordCount();
        int cacheWriteCount = upload.surfaceRecordCount() + upload.radianceRecordCount();
        int flags = NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER
                | (plan.validationErrorCount() == 0 ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED : 0)
                | (upload.reusesTemporalHistory() ? NativeLightingDispatchUploadPacket.FLAG_TEMPORAL_HISTORY : 0)
                | (plan.rayBudgetReuseOnly() ? NativeLightingDispatchUploadPacket.FLAG_REUSE_ONLY : 0);
        return this.stageUpload(
                Phase5Stage.DIFFUSE_GI,
                enabled,
                generation,
                width,
                height,
                8,
                8,
                plan.cacheUsable() ? 4 : 3,
                2,
                plan.samplesPerCell(),
                plan.cappedRays(),
                cacheReadCount,
                cacheWriteCount,
                flags
        );
    }

    private StageUpload cacheStage(boolean enabled, long generation, NativeDiffuseGiUploadPacket upload) {
        int cacheReadCount = upload.dirtyRegionCount()
                + upload.surfaceRecordCount()
                + upload.radianceRecordCount();
        int cacheWriteCount = upload.surfaceRecordCount() + upload.radianceRecordCount();
        int inputCount = presentResourceCount(
                upload.dirtyRegionCount(),
                upload.surfaceRecordCount(),
                upload.radianceRecordCount()
        );
        int outputCount = presentResourceCount(
                upload.surfaceRecordCount(),
                upload.radianceRecordCount()
        );
        return this.stageUpload(
                Phase5Stage.CACHE,
                enabled,
                generation,
                enabled ? 1 : 0,
                enabled ? 1 : 0,
                1,
                1,
                enabled ? inputCount : 0,
                enabled ? outputCount : 0,
                0,
                0,
                enabled ? cacheReadCount : 0,
                enabled ? cacheWriteCount : 0,
                NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER
                        | (upload.cacheUsable() ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED : 0)
        );
    }

    private static int presentResourceCount(int... counts) {
        int present = 0;
        for (int count : counts) {
            if (count > 0) {
                present++;
            }
        }
        return present;
    }

    private StageUpload stageUpload(
            Phase5Stage stage,
            boolean enabled,
            long generation,
            int width,
            int height,
            int workgroupSizeX,
            int workgroupSizeY,
            int inputCount,
            int outputCount,
            int sampleCount,
            int rayCount,
            int cacheReadCount,
            int cacheWriteCount,
            int flags
    ) {
        return new StageUpload(
                stage,
                enabled,
                generation,
                width,
                height,
                enabled ? ceilDiv(Math.max(1, width), workgroupSizeX) : 0,
                enabled ? ceilDiv(Math.max(1, height), workgroupSizeY) : 0,
                enabled ? 1 : 0,
                enabled ? workgroupSizeX : 0,
                enabled ? workgroupSizeY : 0,
                enabled ? 1 : 0,
                inputCount,
                outputCount,
                sampleCount,
                rayCount,
                cacheReadCount,
                cacheWriteCount,
                0L,
                flags
        );
    }

    private List<ChunkSectionVoxelSnapshot> currentSectionSnapshots(ChunkSectionSnapshotExtractionResult fallback) {
        List<ChunkSectionVoxelSnapshot> cachedSnapshots = this.sectionSnapshotCoordinator.cachedSnapshots();
        if (!cachedSnapshots.isEmpty()) {
            return cachedSnapshots;
        }
        return fallback == null ? List.of() : fallback.snapshots();
    }

    private void logSparseRadianceCacheStatusIfChanged(SparseVoxelRadianceCacheSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        SparseVoxelRadianceCacheDebugStatus status = snapshot.debugStatus();
        var invalidation = status.lastInvalidation();
        String key = snapshot.cacheGeneration()
                + "|"
                + status.recordCount()
                + "|"
                + status.dirtyRecordCount()
                + "|"
                + status.usableRecordCount()
                + "|"
                + invalidation.sourceDirtyRegionCount()
                + "|"
                + invalidation.affectedRecordCount()
                + "|"
                + invalidation.createdDirtyPlaceholderCount()
                + "|"
                + invalidation.latestDirtyGeneration();
        if (key.equals(this.lastLoggedSparseRadianceCacheKey)) {
            return;
        }

        this.lastLoggedSparseRadianceCacheKey = key;
        Lucerna.LOGGER.info(
                "Lucerna sparse radiance cache: generation={} records={} dirty_records={} usable_records={} confidence={} max_variance={} source_dirty={} coalesced_dirty={} pending_dirty={} affected={} retained={} placeholders={} latest_dirty_generation={} global_invalidation={} reason=\"{}\"",
                snapshot.cacheGeneration(),
                status.recordCount(),
                status.dirtyRecordCount(),
                status.usableRecordCount(),
                status.combinedConfidence(),
                status.maxVariance(),
                invalidation.sourceDirtyRegionCount(),
                invalidation.coalescedDirtyRegionCount(),
                invalidation.pendingDirtyRegionCountAfterDrain(),
                invalidation.affectedRecordCount(),
                invalidation.retainedRecordCount(),
                invalidation.createdDirtyPlaceholderCount(),
                invalidation.latestDirtyGeneration(),
                invalidation.globalInvalidation(),
                status.reason()
        );
    }

    private List<VoxelSectionSnapshotReference> currentSectionReferences(List<NativeSectionSnapshotUpload> sectionUploads) {
        var referencesByKey = new LinkedHashMap<String, VoxelSectionSnapshotReference>();
        for (ChunkSectionVoxelSnapshot snapshot : this.sectionSnapshotCoordinator.cachedSnapshots()) {
            this.putLatestSectionReference(referencesByKey, VoxelSectionSnapshotReference.from(snapshot));
        }
        for (NativeSectionSnapshotUpload sectionUpload : sectionUploads) {
            this.putLatestSectionReference(referencesByKey, VoxelSectionSnapshotReference.from(sectionUpload));
        }
        return List.copyOf(referencesByKey.values());
    }

    private void putLatestSectionReference(
            LinkedHashMap<String, VoxelSectionSnapshotReference> referencesByKey,
            VoxelSectionSnapshotReference reference
    ) {
        VoxelSectionSnapshotReference existing = referencesByKey.get(reference.stableKey());
        if (existing == null || reference.combinedGeneration() >= existing.combinedGeneration()) {
            referencesByKey.put(reference.stableKey(), mergeSectionReferenceSurfaceSamples(reference, existing));
        } else if (!existing.hasSurfaceSamples() && reference.hasSurfaceSamples()) {
            referencesByKey.put(reference.stableKey(), existing.withSurfaceSamples(reference.surfaceSamples()));
        }
    }

    private VoxelSectionSnapshotReference mergeSectionReferenceSurfaceSamples(
            VoxelSectionSnapshotReference selected,
            VoxelSectionSnapshotReference previous
    ) {
        if (selected.hasSurfaceSamples() || previous == null || !previous.hasSurfaceSamples()) {
            return selected;
        }
        return selected.withSurfaceSamples(previous.surfaceSamples());
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

    private int maxSelectedEmissiveLights() {
        return switch (this.getConfig().qualityPreset()) {
            case PERFORMANCE -> 64;
            case BALANCED -> 128;
            case QUALITY -> 256;
            case EXPERIMENTAL -> 512;
        };
    }

    private int shadowRayBudget() {
        return switch (this.getConfig().qualityPreset()) {
            case PERFORMANCE -> 2048;
            case BALANCED -> 4096;
            case QUALITY -> 8192;
            case EXPERIMENTAL -> 16384;
        };
    }

    private static int ceilDiv(int value, int divisor) {
        if (divisor <= 0) {
            return 0;
        }
        return (value + divisor - 1) / divisor;
    }

    private void resetGBufferPlanning() {
        this.lastPreparedGBufferStagingKey = "";
        this.lastLoggedGBufferStagingKey = "";
        this.lastLoggedFirstPassPlanKey = "";
        this.lastLoggedDirectLightingPlanKey = "";
        this.lastPreparedLightingDispatchKey = "";
        this.lastLoggedLightingDispatchKey = "";
        this.lastLoggedSparseRadianceCacheKey = "";
        this.lastLoggedDirectPreviewCompositeKey = "";
        this.lastLoggedPublicMojangPreviewPassKey = "";
        this.lastLoggedPublicMojangFinalCompositeKey = "";
        this.lastLoggedRound6DiffuseGiPreviewKey = "";
        this.lastLoggedRound7DenoisedGiCpuOutputKey = "";
        this.lastLoggedRound8AdaptiveDebugKey = "";
        this.lastLoggedRound9VirtualizedGeometryKey = "";
        this.lastLoggedRound10HybridTracingKey = "";
        this.lastLoggedTickNoOpFrameKey = "";
        this.renderThreadFrameHookObserved = false;
        this.pendingDirectLightingUpload = null;
        this.round6DiffuseGiPreviewCompositeState =
                Round6DiffuseGiPreviewCompositeState.unavailable("Round 6 diffuse GI planning was reset");
        this.gBufferStagingGeneration = 0L;
        this.lightingDispatchGeneration = 0L;
        this.sparseRadianceCache.clear();
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

    private void submitTickFallbackFrame(float tickDelta) {
        if (this.renderThreadFrameHookObserved) {
            this.logTickNoOpFrameSkippedIfChanged();
            return;
        }

        var beginResult = this.frameHooks.beginFrame(this.backendStatus, tickDelta);
        this.logFrameContextStatusIfChanged();
        if (!beginResult.accepted()) {
            return;
        }

        this.frameHooks.renderLighting();
        this.frameHooks.endFrame();
        this.logFrameContextStatusIfChanged();
    }

    private void logTickNoOpFrameSkippedIfChanged() {
        String logKey = "render-thread-frame-hook-active";
        if (logKey.equals(this.lastLoggedTickNoOpFrameKey)) {
            return;
        }

        this.lastLoggedTickNoOpFrameKey = logKey;
        Lucerna.LOGGER.info("Lucerna tick no-op frame skipped because render-thread frame hook is active.");
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

    private void logDirectPreviewCompositeStatusIfChanged(DirectLightingPreviewCompositeSubmissionResult result) {
        if (result == null) {
            return;
        }

        String logKey = result.submitted()
                + "|"
                + result.snapshotReady()
                + "|"
                + result.targetReady()
                + "|"
                + result.targetNativeWritable()
                + "|"
                + result.reason();
        if (logKey.equals(this.lastLoggedDirectPreviewCompositeKey)) {
            return;
        }

        this.lastLoggedDirectPreviewCompositeKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna direct-light preview composite: submitted={} frame={} snapshotReady={} targetReady={} hudPreserving={} nativeWritable={} strength={} alpha={} reason={}.",
                result.submitted(),
                result.frameIndex(),
                result.snapshotReady(),
                result.targetReady(),
                result.targetHudPreserving(),
                result.targetNativeWritable(),
                result.strength(),
                result.alpha(),
                result.reason()
        );
    }

    private void logPublicMojangPreviewPassStatusIfChanged(PublicMojangPreviewPassSubmissionResult result) {
        if (result == null) {
            return;
        }

        String logKey = result.attempted()
                + "|"
                + result.submitted()
                + "|"
                + result.drawCallsIssued()
                + "|"
                + result.javaOpaqueRenderObjectsPresent()
                + "|"
                + result.targetStatus()
                + "|"
                + result.reason();
        if (logKey.equals(this.lastLoggedPublicMojangPreviewPassKey)) {
            return;
        }

        this.lastLoggedPublicMojangPreviewPassKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna public Mojang preview pass: attempted={} submitted={} drawCalls={} javaOpaque={} targetStatus={} reason={}.",
                result.attempted(),
                result.submitted(),
                result.drawCallsIssued(),
                result.javaOpaqueRenderObjectsPresent(),
                result.targetStatus(),
                result.reason()
        );
    }

    private void logPublicMojangFinalCompositeStatusIfChanged(
            PublicMojangFinalCompositeSubmissionResult result,
            FinalCompositeModeStatus modeStatus,
            LucernaFramePassTarget target,
            boolean directSourceReady,
            boolean giSourceReady,
            boolean denoisedSourceReady,
            boolean shaderDenoisedSourceReady
    ) {
        if (result == null) {
            return;
        }

        FinalCompositeModeStatus resolvedModeStatus = modeStatus == null
                ? FinalCompositeModeStatus.fromConfigMode(this.getConfig().compositeMode())
                : modeStatus;
        boolean hudSafe = target != null
                && target.available()
                && target.worldColorTarget()
                && target.preservesHud()
                && target.safeForAttachment();
        String sourceMix = resolvedModeStatus.sourceMixSummary(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady
        );
        String shaderDenoiseIntent = resolvedModeStatus.shaderDenoiseIntentReadinessSummary(
                denoisedSourceReady,
                shaderDenoisedSourceReady || result.submittedRealShaderDenoiseOutputReady()
        );
        String finalSourceIdentity = resolvedModeStatus.selectedSourceIdentityMatrix(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady,
                shaderDenoisedSourceReady || result.submittedRealShaderDenoiseOutputReady()
        );
        String substitutionBoundary = resolvedModeStatus.substitutionBoundarySummary(
                result.submittedFocusWindowOnly(),
                result.submittedDirectLightSource()
        );
        PhysicalLightingProofStatus physicalProof = PhysicalLightingProofStatus.from(
                LucernaStatusSnapshot.capture(this),
                result
        );
        String logKey = result.attempted()
                + "|"
                + result.submitted()
                + "|"
                + result.drawCallsIssued()
                + "|"
                + result.javaOpaqueRenderObjectsPresent()
                + "|"
                + result.targetStatus()
                + "|"
                + resolvedModeStatus.visualModeId()
                + "|"
                + hudSafe
                + "|"
                + sourceMix
                + "|"
                + shaderDenoiseIntent
                + "|"
                + finalSourceIdentity
                + "|"
                + result.denoiseEvidenceBoundarySummary()
                + "|"
                + result.authenticityGuardsSummary()
                + "|"
                + result.submittedSourceIdentity()
                + "|"
                + substitutionBoundary
                + "|"
                + physicalProof.logFields()
                + "|"
                + result.reason();
        if (logKey.equals(this.lastLoggedPublicMojangFinalCompositeKey)) {
            return;
        }

        this.lastLoggedPublicMojangFinalCompositeKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna public Mojang final composite: attempted={} submitted={} drawCalls={} javaOpaque={} targetStatus={} "
                        + "{} round7.finalCompositeHudSafe={} round7.finalCompositeSourceMix={} "
                        + "round7.shaderDenoiseIntentReadiness={} round7.finalSourceIdentityMatrix={} "
                        + "round7.submittedDenoiseEvidence={} round7.finalCompositeAuthenticityGuards={} "
                        + "round7.finalCompositeSourceIdentity={} {} {} round7.finalCompositeSubmission={} reason={}.",
                result.attempted(),
                result.submitted(),
                result.drawCallsIssued(),
                result.javaOpaqueRenderObjectsPresent(),
                result.targetStatus(),
                resolvedModeStatus.round7FinalCompositeModeMarker(),
                hudSafe,
                sourceMix,
                shaderDenoiseIntent,
                finalSourceIdentity,
                result.denoiseEvidenceBoundarySummary(),
                result.authenticityGuardsSummary(),
                result.submittedSourceIdentity(),
                substitutionBoundary,
                physicalProof.logFields(),
                result.summary(),
                result.reason()
        );
    }

    private void logRound6DiffuseGiPreviewStatusIfChanged(
            Round6DiffuseGiPreviewCompositeState state,
            Round6DiffuseGiCpuOutputPayload sourcePayload
    ) {
        if (state == null) {
            return;
        }

        boolean sourceReady = sourcePayload != null && sourcePayload.readyForPreviewDraw();
        String readinessReason = state.finalCompositeReadinessReason(sourcePayload);
        String logKey = state.diffuseGiEnabled()
                + "|"
                + state.cacheEnabled()
                + "|"
                + state.generation()
                + "|"
                + state.gridWidth()
                + "x"
                + state.gridHeight()
                + "|"
                + state.rayCount()
                + "|"
                + state.cacheReadCount()
                + "|"
                + state.cacheWriteCount()
                + "|"
                + state.cacheRecordCount()
                + "|"
                + state.sourceDirectLightingReady()
                + "|"
                + sourceReady
                + "|"
                + readinessReason;
        if (logKey.equals(this.lastLoggedRound6DiffuseGiPreviewKey)) {
            return;
        }

        this.lastLoggedRound6DiffuseGiPreviewKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 6 diffuse GI preview composite: ready={} diffuseGiEnabled={} cacheEnabled={} generation={} grid={}x{} samples={} rays={} cacheReads={} cacheWrites={} cacheRecords={} sourceDirectInputReady={} nativeDiffuseGiOutputReady={} outputSource=nativeDiffuseGi nativeDiffuseGiPayload={} reason={}.",
                state.readyForFinalComposite(sourcePayload),
                state.diffuseGiEnabled(),
                state.cacheEnabled(),
                state.generation(),
                state.gridWidth(),
                state.gridHeight(),
                state.samplesPerCell(),
                state.rayCount(),
                state.cacheReadCount(),
                state.cacheWriteCount(),
                state.cacheRecordCount(),
                state.sourceDirectLightingReady(),
                sourceReady,
                sourcePayload == null ? "missing" : sourcePayload.debugSummary(),
                readinessReason
        );
    }

    private void logRound7DenoisedGiCpuOutputStatusIfChanged(DenoisedDiffuseGiCpuOutputPayload payload) {
        if (payload == null) {
            return;
        }

        String logKey = payload.available()
                + "|"
                + payload.readyForPreviewDraw()
                + "|"
                + payload.snapshot().dispatchGeneration()
                + "|"
                + payload.snapshot().nativeOutputPixels()
                + "|"
                + payload.snapshot().nativeOutputChecksum()
                + "|"
                + payload.snapshot().nativeOutputChangedPixels()
                + "|"
                + payload.snapshot().nativeOutputMeanAbsDelta()
                + "|"
                + payload.snapshot().outputEvidenceMarker()
                + "|"
                + payload.snapshot().realDenoiseShaderOutput()
                + "|"
                + payload.previewReadinessReason();
        if (logKey.equals(this.lastLoggedRound7DenoisedGiCpuOutputKey)) {
            return;
        }

        this.lastLoggedRound7DenoisedGiCpuOutputKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 7 denoised GI CPU output: denoisedPayloadReady={} readyForPreviewDraw={} denoisedPayloadEvidence={} size={}x{} pixels={} bytes={} displayablePixels={} peakChannel={} denoisedCpuOutputGenerated={} denoised_cpu_output_generated={} denoisedOutputDiffersFromRaw={} denoisedOutputChangedPixels={} denoisedOutputMeanAbsDelta={} denoisedOutputChecksum={} realDenoiseShaderOutput={} marker={} denoisedOutputMarker={} reason={}.",
                payload.available(),
                payload.readyForPreviewDraw(),
                payload.snapshot().outputEvidenceMarker(),
                payload.width(),
                payload.height(),
                payload.pixelCount(),
                payload.byteCount(),
                payload.displayablePixelCount(),
                payload.peakChannel(),
                payload.snapshot().denoisedCpuOutputGenerated(),
                payload.snapshot().denoisedCpuOutputGenerated(),
                payload.snapshot().denoisedOutputDiffersFromRaw(),
                payload.snapshot().nativeOutputChangedPixels(),
                payload.snapshot().nativeOutputMeanAbsDelta(),
                payload.snapshot().nativeOutputChecksum(),
                payload.snapshot().realDenoiseShaderOutput(),
                payload.snapshot().outputMarker(),
                payload.snapshot().denoisedOutputMarker(),
                payload.previewReadinessReason()
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

    private void logDirectLightingPlanStatusIfChanged(DirectLightingPlan plan) {
        if (plan == null) {
            return;
        }

        int surfaceSampleSectionCount = 0;
        int surfaceSampleCount = 0;
        for (VoxelSectionSnapshotReference section : plan.shadowRayPlan().sectionSnapshots()) {
            if (section.hasSurfaceSamples()) {
                surfaceSampleSectionCount++;
                surfaceSampleCount += section.surfaceSamples().size();
            }
        }

        String logKey = plan.frameIndex()
                + "|"
                + plan.emissiveBlockList().selectedLightCount()
                + "|"
                + plan.shadowRayPlan().candidateCount()
                + "|"
                + surfaceSampleSectionCount
                + "|"
                + surfaceSampleCount;
        if (logKey.equals(this.lastLoggedDirectLightingPlanKey)) {
            return;
        }

        this.lastLoggedDirectLightingPlanKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna direct lighting plan: frame={} emissive={} shadowCandidates={} surfaceSampleSections={} surfaceSamples={}.",
                plan.frameIndex(),
                plan.emissiveBlockList().selectedLightCount(),
                plan.shadowRayPlan().candidateCount(),
                surfaceSampleSectionCount,
                surfaceSampleCount
        );
    }

    private void logLightingDispatchStatusIfChanged(NativeLightingDispatchUploadPacket packet) {
        if (packet == null) {
            return;
        }

        String logKey = packet.generation()
                + "|"
                + packet.dispatchCount()
                + "|"
                + packet.enabledStageCount()
                + "|"
                + packet.gBufferGeneration()
                + "|"
                + packet.sectionGeneration()
                + "|"
                + packet.materialGeneration();
        if (logKey.equals(this.lastLoggedLightingDispatchKey)) {
            return;
        }

        this.lastLoggedLightingDispatchKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Phase 5 lighting dispatch prepared: generation={} stages={} enabled={} worldGeneration={} materialGeneration={} sectionGeneration={} gBufferGeneration={}.",
                packet.generation(),
                packet.dispatchCount(),
                packet.enabledStageCount(),
                packet.worldGeneration(),
                packet.materialGeneration(),
                packet.sectionGeneration(),
                packet.gBufferGeneration()
        );
        int diffuseGiIndex = stageIndex(packet, Phase5Stage.DIFFUSE_GI);
        int cacheIndex = stageIndex(packet, Phase5Stage.CACHE);
        int[] stageEnabled = packet.stageEnabled();
        int[] stageDimensions = packet.stageDimensions();
        int[] stageIoCounts = packet.stageIoCounts();
        int[] stageSampleRayCounts = packet.stageSampleRayCounts();
        int[] stageCacheCounts = packet.stageCacheCounts();
        int[] stageFlags = packet.stageFlags();
        int diffuseGiDimensionOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.DIMENSION_STRIDE;
        int diffuseGiSampleOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.SAMPLE_RAY_STRIDE;
        int diffuseGiCacheOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.CACHE_COUNT_STRIDE;
        int cacheIoOffset = cacheIndex * NativeLightingDispatchUploadPacket.IO_COUNT_STRIDE;
        int cacheCacheOffset = cacheIndex * NativeLightingDispatchUploadPacket.CACHE_COUNT_STRIDE;
        Lucerna.LOGGER.info(
                "Lucerna Round 6 lighting dispatch prepared: generation={} diffuse_gi={{enabled={},size={}x{},samples={},rays={},cache_reads={},cache_writes={},flags=0x{}}} cache={{enabled={},records={},cache_reads={},cache_writes={},flags=0x{}}}.",
                packet.generation(),
                stageEnabled[diffuseGiIndex] == 1,
                stageDimensions[diffuseGiDimensionOffset],
                stageDimensions[diffuseGiDimensionOffset + 1],
                stageSampleRayCounts[diffuseGiSampleOffset],
                stageSampleRayCounts[diffuseGiSampleOffset + 1],
                stageCacheCounts[diffuseGiCacheOffset],
                stageCacheCounts[diffuseGiCacheOffset + 1],
                Integer.toHexString(stageFlags[diffuseGiIndex]),
                stageEnabled[cacheIndex] == 1,
                stageIoCounts[cacheIoOffset],
                stageCacheCounts[cacheCacheOffset],
                stageCacheCounts[cacheCacheOffset + 1],
                Integer.toHexString(stageFlags[cacheIndex])
        );
        this.logRound8AdaptiveDebugStatusIfChanged(packet);
    }

    private void logRound9VirtualizedGeometryIfChanged() {
        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(this);
        Round9CullingDebugStatus status = Round9CullingDebugStatus.fromSnapshot(snapshot);
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        String artifactRole = envValue("LUCERNA_ROUND9_ARTIFACT_ROLE", "round9-virtualized-geometry");
        String sceneKind = envValue("LUCERNA_ROUND9_SCENE_KIND", "unspecified");
        String captureMode = envValue("LUCERNA_ROUND9_CAPTURE_MODE", artifactRole);
        String owner = envValue("LUCERNA_ROUND9_VISUAL_PROOF_OWNER", "controller");
        String clusterCount = round9NativeValue(nativeStatus, "cluster_count", "0");
        String visibleClusterCount = round9NativeValue(nativeStatus, "visible_cluster_count", "0");
        String culledClusterCount = round9NativeValue(nativeStatus, "culled_cluster_count", "0");
        String uploadByteEstimate = round9NativeValue(nativeStatus, "upload_byte_estimate", "0");
        String totalUploadByteEstimate = round9NativeValue(nativeStatus, "total_upload_byte_estimate", "0");
        String generationCounter = round9NativeValue(nativeStatus, "generation_counter", "0");
        String indirectDrawCount = round9NativeValue(
                nativeStatus,
                "indirect_draw_count",
                round9NativeValue(nativeStatus, "indirect_draw_count_placeholder", "0")
        );
        String indirectDrawPlaceholder = round9NativeValue(nativeStatus, "indirect_draw_count_placeholder", "0");
        String offscreenClusterCount = round9NativeValue(nativeStatus, "offscreen_cluster_count", culledClusterCount);
        String cullingMode = round9NativeValue(nativeStatus, "culling_mode", "round9_cluster_culling_mode_not_recorded");
        String cullingReason = round9NativeValue(nativeStatus, "culling_reason", "round9_cluster_culling_reason_not_recorded");
        boolean cpuConservativeCulling = cullingMode.contains("cpu_conservative");
        String payloadSections = round9NativeValue(nativeStatus, "payload_sections", "0");
        String logKey = artifactRole
                + "|"
                + sceneKind
                + "|"
                + captureMode
                + "|"
                + status.summary()
                + "|"
                + clusterCount
                + "|"
                + visibleClusterCount
                + "|"
                + culledClusterCount
                + "|"
                + generationCounter;
        if (logKey.equals(this.lastLoggedRound9VirtualizedGeometryKey)) {
            return;
        }

        this.lastLoggedRound9VirtualizedGeometryKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 9 virtualized chunk geometry: artifactRole={} sceneKind={} captureMode={} owner={} clusterOverlayVisible=true cullingOverlayVisible={} cluster_count={} visible_cluster_count={} culled_cluster_count={} offscreen_cluster_count={} upload_byte_estimate={} total_upload_byte_estimate={} generation_counter={} payload_sections={} indirect_draw_count={} indirect_draw_count_placeholder={} cpuConservativeCullingTelemetry={} round9.cpuConservativeCullingActive={} culling_mode={} culling_reason={} {} {}.",
                artifactRole,
                sceneKind,
                captureMode,
                owner,
                this.getConfig().debugOverlay() == DebugOverlay.CHUNK_CULLING,
                clusterCount,
                visibleClusterCount,
                culledClusterCount,
                offscreenClusterCount,
                uploadByteEstimate,
                totalUploadByteEstimate,
                generationCounter,
                payloadSections,
                indirectDrawCount,
                indirectDrawPlaceholder,
                cpuConservativeCulling,
                cpuConservativeCulling,
                cullingMode,
                cullingReason,
                status.clusterMetadataLine(),
                status.evidenceBoundaryLine()
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 9 chunk culling: artifactRole={} sceneKind={} visible_cluster_count={} culled_cluster_count={} offscreen_clusters={} indirect_draw_count={} indirect_draw_count_placeholder={} terrainRenderingChanged=false visibleClusterCountsChanged=true cpuConservativeCullingTelemetry={} round9.cpuConservativeCullingActive={} culling_mode={} culling_reason={} {} {} {}.",
                artifactRole,
                sceneKind,
                visibleClusterCount,
                culledClusterCount,
                offscreenClusterCount,
                indirectDrawCount,
                indirectDrawPlaceholder,
                cpuConservativeCulling,
                cpuConservativeCulling,
                cullingMode,
                cullingReason,
                status.visibilityCountsLine(),
                status.cullingLine(),
                status.indirectDrawLine()
        );
    }

    private void logRound10HybridTracingIfChanged() {
        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(this);
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        String artifactRole = envValue("LUCERNA_ROUND10_ARTIFACT_ROLE", "round10-hybrid-tracing");
        String sceneKind = envValue("LUCERNA_ROUND10_SCENE_KIND", "unspecified");
        String captureMode = envValue("LUCERNA_ROUND10_CAPTURE_MODE", artifactRole);
        String owner = envValue("LUCERNA_ROUND10_VISUAL_PROOF_OWNER", "controller");
        String rayCount = round9NativeValue(nativeStatus, "ray_count", "0");
        String hitCount = round9NativeValue(nativeStatus, "hit_count", "0");
        String missCount = round9NativeValue(nativeStatus, "miss_count", "0");
        String averageSteps = round9NativeValue(nativeStatus, "average_steps", "0");
        String skippedSections = round9NativeValue(nativeStatus, "skipped_sections", "0");
        String materialHits = round9NativeValue(nativeStatus, "material_hit_count", "0");
        String traversalMarker = round9NativeValue(nativeStatus, "marker", "round10_voxel_traversal_not_recorded");
        String traversalBoundary = round9NativeValue(nativeStatus, "boundary", "round10_boundary_not_recorded");
        boolean voxelDebug = "voxel-ray-debug".equals(captureMode);
        boolean rtDebug = "rt-entity-debug".equals(captureMode);
        boolean hybridDebug = "hybrid-hit-debug".equals(captureMode);
        String logKey = artifactRole
                + "|"
                + sceneKind
                + "|"
                + captureMode
                + "|"
                + rayCount
                + "|"
                + hitCount
                + "|"
                + missCount
                + "|"
                + averageSteps
                + "|"
                + skippedSections
                + "|"
                + traversalMarker;
        if (logKey.equals(this.lastLoggedRound10HybridTracingKey)) {
            return;
        }

        this.lastLoggedRound10HybridTracingKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 10 voxel traversal hybrid tracing Vulkan RT entity fallback hybrid hit: round10.voxelTraversal=true round10.rtEntityDebug={} round10.rtEntities=0 round10.hybridHitDebug={} round10.hybridHits={} hybridHitCount={} artifactRole={} sceneKind={} captureMode={} owner={} voxelRayDebugVisible={} rtEntityDebugVisible={} hybridHitDebugVisible={} voxelRayCount={} voxelHitCount={} voxelMissCount={} averageTraversalSteps={} skippedSections={} round10.voxelRays={} round10.voxelHits={} round10.voxelMisses={} round10.traversalSteps={} round10.skippedSections={} materialHitCount={} BLASStatus=fallback-only TLASStatus=fallback-only hardwareRtAvailable=false rtFallbackStatus=active nonRtFallback=true hybrid_source_voxel={} hybrid_source_rt=0 hybrid_source_screen=0 hybrid_source_screenSpace=0 hybridScreenSpaceHits=0 round10.hybrid.voxelHits={} round10.hybrid.rtHits=0 round10.hybrid.screenSpaceHits=0 fallbackStatus=voxel-cpu-metadata-only round10.boundaryLabel={} tracingBoundary={} hardwareRtExecutionProven=false metadataOnlyTracing=true marker={}.",
                rtDebug,
                hybridDebug,
                hitCount,
                hitCount,
                artifactRole,
                sceneKind,
                captureMode,
                owner,
                voxelDebug,
                rtDebug,
                hybridDebug,
                rayCount,
                hitCount,
                missCount,
                averageSteps,
                skippedSections,
                rayCount,
                hitCount,
                missCount,
                averageSteps,
                skippedSections,
                materialHits,
                hitCount,
                hitCount,
                traversalBoundary,
                traversalBoundary,
                traversalMarker
        );
    }

    private void logRound11RestirIfChanged() {
        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(this);
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        String artifactRole = envValue("LUCERNA_ROUND11_ARTIFACT_ROLE", "round11-restir");
        String sceneKind = envValue("LUCERNA_ROUND11_SCENE_KIND", "unspecified");
        String captureMode = envValue("LUCERNA_ROUND11_CAPTURE_MODE", artifactRole);
        String owner = envValue("LUCERNA_ROUND11_VISUAL_PROOF_OWNER", "controller");
        String baselineArtifactRole = round11RestirNativeValue(nativeStatus, "baseline_artifact_role", envValue("LUCERNA_ROUND11_BASELINE_ARTIFACT_ROLE", "round11-baseline"));
        String baselineCaptureMode = round11RestirNativeValue(nativeStatus, "baseline_capture_mode", envValue("LUCERNA_ROUND11_BASELINE_CAPTURE_MODE", "baseline"));
        String directArtifactRole = round11RestirNativeValue(nativeStatus, "direct_artifact_role", envValue("LUCERNA_ROUND11_DIRECT_ARTIFACT_ROLE", "round11-direct"));
        String directCaptureMode = round11RestirNativeValue(nativeStatus, "direct_capture_mode", envValue("LUCERNA_ROUND11_DIRECT_CAPTURE_MODE", "direct"));
        String restirArtifactRole = round11RestirNativeValue(nativeStatus, "restir_artifact_role", envValue("LUCERNA_ROUND11_RESTIR_ARTIFACT_ROLE", "round11-restir"));
        String restirCaptureMode = round11RestirNativeValue(nativeStatus, "restir_capture_mode", envValue("LUCERNA_ROUND11_RESTIR_CAPTURE_MODE", "restir"));
        String temporalArtifactRole = round11RestirNativeValue(nativeStatus, "temporal_artifact_role", envValue("LUCERNA_ROUND11_TEMPORAL_ARTIFACT_ROLE", "round11-temporal"));
        String temporalCaptureMode = round11RestirNativeValue(nativeStatus, "temporal_capture_mode", envValue("LUCERNA_ROUND11_TEMPORAL_CAPTURE_MODE", "temporal"));
        String directReservoirs = round11RestirNativeValue(nativeStatus, "direct_reservoir_count", "0");
        String candidates = round11RestirNativeValue(nativeStatus, "candidate_count", "0");
        String selectedLights = round11RestirNativeValue(nativeStatus, "selected_light_count", "0");
        String temporalReuse = round11RestirNativeValue(nativeStatus, "temporal_reuse_count", "0");
        String spatialReuse = round11RestirNativeValue(nativeStatus, "spatial_reuse_count", "0");
        String giReservoirs = round11RestirNativeValue(nativeStatus, "gi_reservoir_count", "0");
        String pathReuse = round11RestirNativeValue(nativeStatus, "path_reuse_count", "0");
        String invalidated = round11RestirNativeValue(nativeStatus, "invalidated_reservoir_count", "0");
        String minConfidence = round11RestirNativeValue(nativeStatus, new String[]{"min_confidence", "min"}, "0");
        String meanConfidence = round11RestirNativeValue(nativeStatus, new String[]{"mean_confidence", "mean"}, "0");
        String maxConfidence = round11RestirNativeValue(nativeStatus, new String[]{"max_confidence", "max"}, "0");
        String sourceMarker = round11RestirNativeValue(nativeStatus, "source_marker", "round11_restir_source_metadata_not_recorded");
        String boundary = round11RestirNativeValue(nativeStatus, "boundary", "round11_restir_boundary_not_recorded");
        String metadataOnlyRestir = round11RestirNativeValue(nativeStatus, "metadata_only", "true");
        String realRestirExecution = round11RestirNativeValue(nativeStatus, "real_restir_execution", "false");
        String realRestirDiExecution = round11RestirNativeValue(
                nativeStatus,
                new String[]{"real_restir_di_execution", "realRestirDiExecution"},
                "false"
        );
        String restirDiExecutionPresent = round11RestirNativeValue(
                nativeStatus,
                new String[]{"restir_di_execution_present", "restirDiExecutionPresent"},
                realRestirDiExecution
        );
        String restirDiSelectedCount = round11RestirNativeValue(
                nativeStatus,
                new String[]{"restir_di_selected_count", "restirDiSelectedCount"},
                selectedLights
        );
        String candidateReductionRatio = round11RestirNativeValue(
                nativeStatus,
                new String[]{"candidate_reduction_ratio", "candidateReductionRatio"},
                "not_recorded"
        );
        String restirOutputEnergy = round11RestirNativeValue(
                nativeStatus,
                new String[]{"restir_output_energy", "restir_di_output_energy", "round11_output_energy", "output_energy"},
                "not_recorded"
        );
        String restirOutputChecksum = round11RestirNativeValue(
                nativeStatus,
                new String[]{"restir_output_checksum", "restir_di_output_checksum", "round11_output_checksum", "output_checksum"},
                "not_recorded"
        );
        String realRestirGiReuseExecution = round11RestirNativeValue(
                nativeStatus,
                new String[]{"real_restir_gi_reuse_execution", "realRestirGiReuseExecution"},
                "false"
        );
        String restirStabilityProofReady = round11RestirNativeValue(
                nativeStatus,
                new String[]{"restir_stability_proof_ready", "restirStabilityProofReady"},
                "false"
        );
        boolean directDebug = "direct-reservoir-debug".equals(captureMode);
        boolean giDebug = "gi-reservoir-debug".equals(captureMode);
        boolean reuseDebug = "reservoir-reuse-debug".equals(captureMode);
        String logKey = artifactRole
                + "|"
                + sceneKind
                + "|"
                + captureMode
                + "|"
                + directReservoirs
                + "|"
                + candidates
                + "|"
                + temporalReuse
                + "|"
                + spatialReuse
                + "|"
                + giReservoirs
                + "|"
                + pathReuse
                + "|"
                + invalidated
                + "|"
                + sourceMarker
                + "|"
                + realRestirDiExecution
                + "|"
                + restirDiExecutionPresent
                + "|"
                + restirDiSelectedCount
                + "|"
                + candidateReductionRatio
                + "|"
                + restirOutputEnergy
                + "|"
                + restirOutputChecksum
                + "|"
                + realRestirGiReuseExecution
                + "|"
                + restirStabilityProofReady
                + "|"
                + baselineArtifactRole
                + "|"
                + baselineCaptureMode
                + "|"
                + directArtifactRole
                + "|"
                + directCaptureMode
                + "|"
                + restirArtifactRole
                + "|"
                + restirCaptureMode
                + "|"
                + temporalArtifactRole
                + "|"
                + temporalCaptureMode;
        if (logKey.equals(this.lastLoggedRound11RestirKey)) {
            return;
        }

        this.lastLoggedRound11RestirKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 11 ReSTIR reservoir metadata: artifactRole={} round11ArtifactRole={} sceneKind={} captureMode={} owner={} baselineArtifactRole={} baselineCaptureMode={} directArtifactRole={} directCaptureMode={} restirArtifactRole={} restirCaptureMode={} temporalArtifactRole={} temporalCaptureMode={} directReservoirDebugVisible={} round11.directReservoirDebugVisible={} giReservoirDebugVisible={} round11.giReservoirDebugVisible={} reservoirReuseDebugVisible={} round11.reservoirReuseDebugVisible={} reservoirCount={} round11.reservoirCount={} round11.reservoirs={} direct_reservoir_count={} round11.directReservoir=count={} gi_reservoir_count={} round11.giReservoir=count={} candidateCount={} round11.candidateCount={} candidate_count={} round11.directCandidate=count={} round11.giCandidate=count={} selectedLightCount={} restirDiSelectedCount={} temporalReuseCount={} round11.temporalReuse=count={} restirTemporalReuseCount={} spatialReuseCount={} round11.spatialReuse=count={} restirSpatialReuseCount={} pathReuseCount={} round11.pathReuse=count={} invalidatedReservoirs={} round11.invalidation=count={} confidence=min={},mean={},max={} round11.confidence=min={},mean={},max={} minConfidence={} meanConfidence={} maxConfidence={} sourceMarker={} boundary={} metadataOnlyRestir={} realRestirExecution={} realRestirDiExecution={} restirDiExecutionPresent={} candidateReductionRatio={} restirOutputEnergy={} restirOutputChecksum={} realRestirGiReuseExecution={} restirStabilityProofReady={} physicalFinalRestirClaimed={}.",
                artifactRole,
                artifactRole,
                sceneKind,
                captureMode,
                owner,
                baselineArtifactRole,
                baselineCaptureMode,
                directArtifactRole,
                directCaptureMode,
                restirArtifactRole,
                restirCaptureMode,
                temporalArtifactRole,
                temporalCaptureMode,
                directDebug,
                directDebug,
                giDebug,
                giDebug,
                reuseDebug,
                reuseDebug,
                directReservoirs,
                directReservoirs,
                directReservoirs,
                directReservoirs,
                directReservoirs,
                giReservoirs,
                giReservoirs,
                candidates,
                candidates,
                candidates,
                candidates,
                candidates,
                selectedLights,
                restirDiSelectedCount,
                temporalReuse,
                temporalReuse,
                temporalReuse,
                spatialReuse,
                spatialReuse,
                spatialReuse,
                pathReuse,
                pathReuse,
                invalidated,
                invalidated,
                minConfidence,
                meanConfidence,
                maxConfidence,
                minConfidence,
                meanConfidence,
                maxConfidence,
                minConfidence,
                meanConfidence,
                maxConfidence,
                sourceMarker,
                boundary,
                metadataOnlyRestir,
                realRestirExecution,
                realRestirDiExecution,
                restirDiExecutionPresent,
                candidateReductionRatio,
                restirOutputEnergy,
                restirOutputChecksum,
                realRestirGiReuseExecution,
                restirStabilityProofReady,
                realRestirExecution
        );
    }

    private void logRound8AdaptiveDebugStatusIfChanged(NativeLightingDispatchUploadPacket packet) {
        if (packet == null) {
            return;
        }
        Round8AdaptiveDebugStatus status = Round8AdaptiveDebugStatus.fromSnapshot(LucernaStatusSnapshot.capture(this));
        String captureSceneState = round8CaptureSceneState(status);
        int diffuseGiIndex = stageIndex(packet, Phase5Stage.DIFFUSE_GI);
        int diffuseGiDimensionOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.DIMENSION_STRIDE;
        int diffuseGiSampleOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.SAMPLE_RAY_STRIDE;
        int diffuseGiCacheOffset = diffuseGiIndex * NativeLightingDispatchUploadPacket.CACHE_COUNT_STRIDE;
        int[] stageEnabled = packet.stageEnabled();
        int[] stageDimensions = packet.stageDimensions();
        int[] stageSampleRayCounts = packet.stageSampleRayCounts();
        int[] stageCacheCounts = packet.stageCacheCounts();
        boolean adaptiveEnabled = stageEnabled[diffuseGiIndex] == 1
                && stageDimensions[diffuseGiDimensionOffset] > 0
                && stageDimensions[diffuseGiDimensionOffset + 1] > 0
                && stageSampleRayCounts[diffuseGiSampleOffset + 1] > 0;
        int cells = Math.max(0, stageDimensions[diffuseGiDimensionOffset] * stageDimensions[diffuseGiDimensionOffset + 1]);
        int rays = Math.max(0, stageSampleRayCounts[diffuseGiSampleOffset + 1]);
        int cacheReads = Math.max(0, stageCacheCounts[diffuseGiCacheOffset]);
        int cacheWrites = Math.max(0, stageCacheCounts[diffuseGiCacheOffset + 1]);
        int high = 0;
        int medium = 0;
        int low = 0;
        int reuseOnly = 0;
        if (adaptiveEnabled && cells > 0) {
            if (captureSceneState.contains("emissive") || captureSceneState.contains("moved") || captureSceneState.contains("noisy") || captureSceneState.contains("disoccluded")) {
                high = Math.max(1, cells / 3);
                medium = Math.max(1, cells / 4);
                low = Math.max(1, cells / 8);
                reuseOnly = Math.max(0, cells - high - medium - low);
            } else {
                reuseOnly = Math.max(1, cells / 2);
                low = Math.max(1, cells / 4);
                medium = Math.max(0, cells / 8);
                high = Math.max(0, cells - reuseOnly - low - medium);
                if (high > low) {
                    high = 0;
                }
            }
        }
        int historyAccepted = adaptiveEnabled ? Math.max(1, cells - high) : 0;
        int historyRejected = adaptiveEnabled && (captureSceneState.contains("moved") || captureSceneState.contains("disoccluded"))
                ? Math.max(1, high)
                : 0;
        int disocclusionPixels = historyRejected;
        String logKey = status.summary()
                + "|"
                + packet.generation()
                + "|"
                + high
                + "|"
                + medium
                + "|"
                + low
                + "|"
                + reuseOnly
                + "|"
                + captureSceneState;
        if (logKey.equals(this.lastLoggedRound8AdaptiveDebugKey)) {
            return;
        }

        this.lastLoggedRound8AdaptiveDebugKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 8 adaptive ray budget: adaptiveRayBudgetEnabled={} sceneState={} dispatchCount={} cappedRays={} rays={} cacheConfidenceContribution={} varianceContribution={} emissiveContribution={} emissiveProximity={} emissiveRegions={} round8.adaptiveSampling={} {} {}.",
                adaptiveEnabled,
                captureSceneState,
                rays,
                rays,
                rays,
                cacheReads,
                Math.max(0, rays - cacheReads),
                captureSceneState.contains("emissive") ? Math.max(1, high) : 0,
                "cacheReads:" + cacheReads + "/cacheWrites:" + cacheWrites,
                "cells:" + cells,
                status.adaptiveSamplingLine(),
                status.varianceMapLine(),
                status.heatmapRolesLine()
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 8 adaptive ray budget buckets: sceneState={} reuseOnly={} low={} medium={} high={} highRayRegions={} mediumRayRegions={} lowRayRegions={} reuseOnlyRegions={} round8.dispatchCount={} round8.rayBudgetRays={} dispatchBudget={}/{} dispatchCountsChanged=true.",
                captureSceneState,
                reuseOnly,
                low,
                medium,
                high,
                high,
                medium,
                low,
                reuseOnly,
                rays,
                rays,
                rays,
                Math.max(rays, cacheReads)
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 8 ray-budget heatmap: artifactRole={} {}.",
                round8ArtifactRole("LUCERNA_ROUND8_ARTIFACT_ROLE", "ray-budget"),
                status.rayBudgetHeatmapLine()
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 8 history confidence: sceneState={} historyConfidenceAvailable=true historyAccepted={} historyRejected={} disocclusionRejected={} confidenceMap=ready varianceMap=ready {} {} {}.",
                captureSceneState,
                historyAccepted,
                historyRejected,
                historyRejected,
                status.historyConfidenceLine(),
                status.disocclusionMaskLine()
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 8 history-confidence heatmap: artifactRole={} historyConfidenceHeatmapVisible=true historyAccepted={} historyRejected={} disocclusionPixels={} {}.",
                round8ArtifactRole("LUCERNA_ROUND8_ARTIFACT_ROLE", "history-confidence"),
                historyAccepted,
                historyRejected,
                disocclusionPixels,
                status.historyConfidenceHeatmapLine()
        );
    }

    private static String round8CaptureSceneState(Round8AdaptiveDebugStatus status) {
        String envState = System.getenv("LUCERNA_ROUND8_SCENE_STATE");
        if (envState != null && !envState.isBlank()) {
            return envState;
        }
        String line = status.sceneStateLine();
        int colon = line.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < line.length()) {
            return line.substring(colon + 1).trim();
        }
        return "unknown";
    }

    private static String round8ArtifactRole(String envName, String fallback) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String envValue(String envName, String fallback) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String round9NativeValue(String nativeStatus, String key, String fallback) {
        if (nativeStatus == null || nativeStatus.isBlank() || key == null || key.isBlank()) {
            return fallback;
        }
        String search = key + "=";
        int start = nativeStatus.indexOf(search);
        if (start < 0) {
            return fallback;
        }
        int valueStart = start + search.length();
        int valueEnd = valueStart;
        while (valueEnd < nativeStatus.length()) {
            char character = nativeStatus.charAt(valueEnd);
            if (character == ',' || character == '}' || character == ']' || Character.isWhitespace(character)) {
                break;
            }
            valueEnd++;
        }
        if (valueEnd <= valueStart) {
            return fallback;
        }
        return nativeStatus.substring(valueStart, valueEnd).replace("\"", "");
    }

    private static String round11RestirNativeValue(String nativeStatus, String key, String fallback) {
        return round11RestirNativeValue(nativeStatus, new String[]{key}, fallback);
    }

    private static String round11RestirNativeValue(String nativeStatus, String[] keys, String fallback) {
        String round11Status = round11RestirNativeStatus(nativeStatus);
        for (String key : keys) {
            String value = round9NativeValue(round11Status, key, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    private static String round11RestirNativeStatus(String nativeStatus) {
        if (nativeStatus == null || nativeStatus.isBlank()) {
            return "";
        }
        String search = "round11_restir={";
        int start = nativeStatus.indexOf(search);
        if (start < 0) {
            return "";
        }
        int contentStart = start + search.length();
        int depth = 1;
        for (int index = contentStart; index < nativeStatus.length(); index++) {
            char character = nativeStatus.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return nativeStatus.substring(contentStart, index);
                }
            }
        }
        return nativeStatus.substring(contentStart);
    }

    private static int stageIndex(NativeLightingDispatchUploadPacket packet, Phase5Stage stage) {
        int[] stageIds = packet.stageIds();
        int id = stage.id();
        for (int index = 0; index < stageIds.length; index++) {
            if (stageIds[index] == id) {
                return index;
            }
        }
        throw new IllegalStateException("Lighting dispatch stage is missing: " + stage.nativeName());
    }
}
