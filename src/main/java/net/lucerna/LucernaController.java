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
import net.lucerna.render.lighting.post.PostProcessingPipelinePlan;
import net.lucerna.render.lighting.post.PostProcessingPlanBuilder;
import net.lucerna.render.mixin.PublicMojangPreviewPassSubmissionResult;
import net.lucerna.render.mixin.RenderThreadPreviewTargetFactory;
import net.lucerna.render.pass.LucernaFramePassRequest;
import net.lucerna.render.pass.LucernaFramePassResult;
import net.lucerna.render.pass.LucernaFramePassStatus;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.lucerna.render.preview.PublicMojangFinalCompositeSubmissionResult;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
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
            LucernaFramePassRequest finalCompositeRequest = LucernaFramePassRequest.finalWorldColorComposite(
                    this.frameHooks.frameIndex(),
                    target,
                    0.35F,
                    0.35F
            );
            PublicMojangFinalCompositeSubmissionResult finalCompositeSubmission = giPreviewState.readyForFinalComposite(
                    diffuseGiPayload
            )
                    ? RenderThreadPreviewTargetFactory.submitRound6DiffuseGiFinalCompositePublicDraw(
                            target,
                            diffuseGiPayload,
                            giPreviewState
                    )
                    : RenderThreadPreviewTargetFactory.submitFinalCompositePublicDraw(target, directOutputPayload);
            this.logPublicMojangFinalCompositeStatusIfChanged(finalCompositeSubmission);
            return this.frameHooks.attachFramePass(finalCompositeRequest);
        } finally {
            this.frameHooks.endFrame();
            this.logFrameContextStatusIfChanged();
        }
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

    private void logPublicMojangFinalCompositeStatusIfChanged(PublicMojangFinalCompositeSubmissionResult result) {
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
        if (logKey.equals(this.lastLoggedPublicMojangFinalCompositeKey)) {
            return;
        }

        this.lastLoggedPublicMojangFinalCompositeKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna public Mojang final composite: attempted={} submitted={} drawCalls={} javaOpaque={} targetStatus={} reason={}.",
                result.attempted(),
                result.submitted(),
                result.drawCallsIssued(),
                result.javaOpaqueRenderObjectsPresent(),
                result.targetStatus(),
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
                "Lucerna Round 6 diffuse GI preview composite: ready={} diffuseGiEnabled={} cacheEnabled={} generation={} grid={}x{} samples={} rays={} cacheReads={} cacheWrites={} cacheRecords={} sourceDirectReady={} nativeDiffuseGiOutputReady={} sourceType=nativeDiffuseGi nativeGiPayload={} reason={}.",
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
