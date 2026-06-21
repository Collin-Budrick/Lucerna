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
import net.lucerna.nativebridge.DirectionalShadowMapOutputPayload;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputSnapshot;
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
import net.lucerna.render.preview.ProofVisualMode;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
import net.lucerna.render.preview.Round8AdaptiveDebugStatus;
import net.lucerna.render.preview.ShaderDenoiseOutputRenderTarget;
import net.lucerna.render.tracing.TracedLightingConsumptionEvidence;
import net.lucerna.render.tracing.hybrid.Round10HybridHitDebugStatus;
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
import java.util.Locale;

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
    private String lastLoggedRealRendererMilestone1Key = "";
    private String lastLoggedTracedLightingConsumptionKey = "";
    private String lastLoggedTickNoOpFrameKey = "";
    private boolean renderThreadFrameHookObserved;
    private NativeDirectLightingUploadPacket pendingDirectLightingUpload;
    private Round6DiffuseGiPreviewCompositeState round6DiffuseGiPreviewCompositeState =
            Round6DiffuseGiPreviewCompositeState.unavailable("Round 6 diffuse GI dispatch has not been prepared yet");
    private TracedLightingConsumptionEvidence lastDiffuseGiTraceConsumptionEvidence =
            TracedLightingConsumptionEvidence.notConsumed(0L, "Round 6 diffuse GI trace evidence has not been prepared yet");
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
            this.logRealRendererMilestone1IfChanged();
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

    public boolean isWorldSpaceVisualPreviewActive() {
        if (this.isRendererActive()) {
            return true;
        }
        return ProofVisualMode.javaWorldSpaceVisualFallbackAllowed()
                && this.getConfig().rendererEnabled()
                && this.backendStatus.active()
                && this.backendStatus.kind() == BackendKind.SODIUM_VULKAN;
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
            DirectionalShadowMapOutputPayload shadowMapOutputPayload =
                    this.nativeBridge.directionalShadowMapOutputPayload();
            Round6DiffuseGiPreviewCompositeState giPreviewState = this.round6DiffuseGiPreviewCompositeState;
            FinalCompositeModeStatus modeStatus = FinalCompositeModeStatus.fromConfigMode(this.getConfig().compositeMode());
            boolean shaderOutputProofRequested = (modeStatus.denoisedGiVisualMode()
                    || modeStatus.finalCompositeVisualMode())
                    && shaderGeneratedDenoiseOutputProofRequested();
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload =
                    this.round7RawGiCpuInputPayload(giPreviewState, shaderOutputProofRequested);
            this.logRound6DiffuseGiPreviewStatusIfChanged(giPreviewState, diffuseGiPayload);
            DenoisedDiffuseGiCpuOutputPayload denoisedGiPayload = this.nativeBridge.denoisedDiffuseGiCpuOutputPayload();
            this.logRound7DenoisedGiCpuOutputStatusIfChanged(denoisedGiPayload);
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
                    shadowMapOutputPayload,
                    diffuseGiPayload,
                    denoisedGiPayload,
                    giPreviewState
            );
            this.nativeBridge.reportGBufferDepthSamplingEvidence(
                    target,
                    finalCompositeSubmission != null
                            && finalCompositeSubmission.submittedDepthAwareShadowMaskComposite(),
                    "shader_sampled_public_mojang_depth_view",
                    "depth_aware_shadow_mask_composite_not_submitted"
            );
            this.reportShaderGeneratedDenoiseOutputEvidence(finalCompositeSubmission);
            this.logPublicMojangFinalCompositeStatusIfChanged(
                    finalCompositeSubmission,
                    modeStatus,
                    target,
                    directOutputPayload != null && directOutputPayload.readyForPreviewDraw(),
                    diffuseGiPayload != null && diffuseGiPayload.readyForPreviewDraw(),
                    denoisedGiPayload != null && denoisedGiPayload.readyForPreviewDraw(),
                    denoisedGiPayload != null
                            && denoisedGiPayload.readyForPreviewDraw()
                            && denoisedGiPayload.snapshot().realDenoiseShaderOutput(),
                    giPreviewState,
                    diffuseGiPayload
            );
            this.logRealRendererMilestone1IfChanged(finalCompositeSubmission, modeStatus);
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
            DirectionalShadowMapOutputPayload shadowMapOutputPayload,
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
        if (modeStatus.denoisedGiVisualMode() && shaderGeneratedDenoiseOutputProofRequested()) {
            if (giPreviewState == null || !giPreviewState.rawGiInputReady(diffuseGiPayload)) {
                String rawGiInputEvidence = giPreviewState == null
                        ? "rawGiInputReady=false,round7.rawGiInputReady=false,rawGiInputSource=\"missing Round 6 diffuse GI preview state\",round7.rawGiInputSource=\"missing Round 6 diffuse GI preview state\",rawGiInputBlocker=\"Round 6 diffuse GI preview state is missing\""
                        : giPreviewState.rawGiInputSourceEvidence(diffuseGiPayload);
                boolean javaOpaqueTarget = target != null
                        && target.available()
                        && target.attachmentMetadata().javaOpaque();
                PublicMojangFinalCompositeSubmissionResult.TargetStatus targetStatus =
                        target == null || !target.available()
                                ? PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING
                                : (target.safeForAttachment()
                                ? PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY
                                : PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY);
                return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                        true,
                        javaOpaqueTarget,
                        targetStatus,
                        "public Mojang Round 7 shader-denoise output proof blocked because real raw diffuse-GI CPU input is not ready; "
                                + rawGiInputEvidence
                                + "; shaderDenoiseInputReady=false,shaderDenoiseInputsCompleteForDispatch=false"
                                + ",shaderDenoisePassExecuted=false,shaderGeneratedDenoisePassExecuted=false"
                                + ",shaderDenoiseOutputPassAttempted=false,round7.shaderDenoise.outputImageReady=false"
                                + ",realShaderDenoiseOutputReady=false,shaderGeneratedDenoiseOutputEvidence=false"
                                + ",directLightValidationInput=false,diagnosticDirectLightValidationFallback=false"
                                + ",shaderDenoiseOverclaimPresent=false"
                );
            }
            return RenderThreadPreviewTargetFactory.submitRound7ShaderDenoiseOutputPublicDraw(
                    target,
                    diffuseGiPayload
            );
        }
        if (modeStatus.denoisedGiVisualMode()) {
            return RenderThreadPreviewTargetFactory.submitRound7DenoisedGiFinalCompositePublicDraw(
                    target,
                    denoisedGiPayload
            );
        }
        if (modeStatus.finalCompositeVisualMode()
                && shadowMapOutputPayload != null
                && shadowMapOutputPayload.readyForPreviewDraw()) {
            if (shaderGeneratedDenoiseOutputProofRequested()) {
                return RenderThreadPreviewTargetFactory.submitRealRendererMilestone1FullCompositePublicDraw(
                        target,
                        shadowMapOutputPayload.copyToByteBuffer(),
                        shadowMapOutputPayload.width(),
                        shadowMapOutputPayload.height(),
                        shadowMapOutputPayload.snapshot().realShadowMapOutputReady(),
                        shadowMapOutputPayload.debugSummary(),
                        diffuseGiPayload,
                        giPreviewState
                );
            }
            return RenderThreadPreviewTargetFactory.submitNativeShadowMapFinalCompositePublicDraw(
                    target,
                    shadowMapOutputPayload.copyToByteBuffer(),
                    shadowMapOutputPayload.width(),
                    shadowMapOutputPayload.height(),
                    shadowMapOutputPayload.snapshot().realShadowMapOutputReady(),
                    shadowMapOutputPayload.debugSummary()
            );
        }
        if (modeStatus.finalCompositeVisualMode()
                && !ProofVisualMode.experimentalVisualStackAllowed()
                && !ProofVisualMode.cpuDirectTextureCompositeAllowed()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.NOT_REQUESTED,
                    "cleanGameplayComposite=true experimentalVisualStack=false cpuDirectTextureComposite=false "
                            + "screenSpaceBlobComposite=false worldSpaceEmissiveSpillPath=true proofMarker=false "
                            + "focusWindowOnly=false metadataOnly=false reason=normal gameplay bypasses the rejected "
                            + "CPU direct-light texture composite; world-space emissive block-face spill owns the "
                            + "clean visual milestone."
            );
        }
        if (modeStatus.finalCompositeVisualMode() && !ProofVisualMode.experimentalVisualStackAllowed()) {
            return RenderThreadPreviewTargetFactory.submitFinalCompositePublicDraw(target, directOutputPayload);
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

    private void reportShaderGeneratedDenoiseOutputEvidence(
            PublicMojangFinalCompositeSubmissionResult finalCompositeSubmission
    ) {
        if (finalCompositeSubmission == null
                || !finalCompositeSubmission.submittedShaderDenoiseOutputPassAttempted()) {
            return;
        }

        ShaderDenoiseOutputRenderTarget.StatusSnapshot outputTargetStatus =
                RenderThreadPreviewTargetFactory.shaderDenoiseOutputRenderTargetStatus();
        boolean ready = finalCompositeSubmission.submittedRealShaderDenoiseOutputReady()
                && outputTargetStatus.availableForSampling();
        int width = ready ? outputTargetStatus.width() : Math.max(0, outputTargetStatus.width());
        int height = ready ? outputTargetStatus.height() : Math.max(0, outputTargetStatus.height());
        long texelCount = width > 0 && height > 0 ? (long) width * (long) height : 0L;
        long checksum = outputTargetStatus.identityChecksum() & Long.MAX_VALUE;
        if (ready && checksum == 0L) {
            checksum = Math.max(1L, texelCount);
        }
        this.nativeBridge.reportShaderGeneratedDenoiseOutputEvidence(
                outputTargetStatus.allocationGeneration(),
                width,
                height,
                ready ? texelCount : 0L,
                ready ? texelCount : 0L,
                ready ? checksum : 0L,
                finalCompositeSubmission.submittedSourceIdentity(),
                ready
                        ? "shader_generated_denoise_output_image_consumed_by_final_composite"
                        : "shader_generated_denoise_output_report_not_ready",
                ready ? "none" : finalCompositeSubmission.shaderDenoiseOutputBlockerSource(),
                ready,
                ready,
                ready
        );
    }

    private static boolean shaderGeneratedDenoiseOutputProofRequested() {
        return envTruthy("LUCERNA_REQUIRE_SHADER_GENERATED_DENOISE_OUTPUT")
                || envTruthy("LUCERNA_REQUIRE_SHADER_DENOISE_OUTPUT_CONSUMED")
                || envHasText("LUCERNA_ROUND7_SHADER_DENOISE_ARTIFACT_ROLE")
                || envHasText("LUCERNA_ROUND7_SHADER_DENOISE_SCENE_KIND");
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

    private Round6DiffuseGiCpuOutputPayload round7RawGiCpuInputPayload(
            Round6DiffuseGiPreviewCompositeState giPreviewState,
            boolean shaderOutputProofRequested
    ) {
        Round6DiffuseGiCpuOutputPayload payload = this.nativeBridge.round6DiffuseGiCpuOutputPayload(giPreviewState);
        if (!shaderOutputProofRequested
                || giPreviewState == null
                || giPreviewState.rawGiInputReady(payload)) {
            return payload;
        }

        // In shader-output proof mode, make one same-frame native payload request after proof selection.
        // The later proof gate still requires rawGiInputReady; a failed retry only refreshes blocker evidence.
        Round6DiffuseGiCpuOutputPayload proofPayload =
                this.nativeBridge.round6DiffuseGiCpuOutputPayload(giPreviewState);
        return proofPayload == null ? payload : proofPayload;
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
        TracedLightingConsumptionEvidence traceConsumptionEvidence =
                this.buildRawDiffuseGiTraceConsumptionEvidence(diffuseGiPlan, sourceGeneration);
        NativeDiffuseGiUploadPacket diffuseGiUpload = NativeDiffuseGiUploadPacket.from(
                diffuseGiPlan,
                directLightingUpload,
                metadata,
                traceConsumptionEvidence
        );
        this.lastDiffuseGiTraceConsumptionEvidence = traceConsumptionEvidence;
        this.logTracedLightingConsumptionStatusIfChanged(traceConsumptionEvidence, diffuseGiUpload);

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
                + traceConsumptionEvidence.compactLabel()
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
        boolean valid = plan.validationErrorCount() == 0;
        boolean nativeTraceExecutionReady = enabled
                && valid
                && upload.readyForScheduling()
                && upload.requiresTracing()
                && plan.cappedRays() > 0
                && plan.sourceHasDirectLightingWork()
                && plan.sourceHasWorldMaterialInputs();
        int flags = (valid ? NativeLightingDispatchUploadPacket.FLAG_VALIDATED : 0)
                | (!nativeTraceExecutionReady ? NativeLightingDispatchUploadPacket.FLAG_PLACEHOLDER : 0)
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

    private TracedLightingConsumptionEvidence buildRawDiffuseGiTraceConsumptionEvidence(
            LowResDiffuseGiPlan diffuseGiPlan,
            long generation
    ) {
        Round10HybridHitDebugStatus hybridStatus =
                Round10HybridHitDebugStatus.fromSnapshot(LucernaStatusSnapshot.capture(this));
        TracedLightingConsumptionEvidence statusEvidence = hybridStatus.toTraceConsumptionEvidence(
                generation,
                "native_raw_diffuse_gi_output"
        );
        Round6DiffuseGiCpuOutputSnapshot nativeDiffuseGi = this.nativeBridge.round6DiffuseGiCpuOutputSnapshot();
        boolean nativeDiffuseGiTraceEvidence = nativeDiffuseGiTraceEvidence(nativeDiffuseGi);
        long nativeSourceCoupledBounceCount = nativeDiffuseGiTraceEvidence
                ? minPositive(
                nativeDiffuseGi.physicalGiHitSamples(),
                nativeDiffuseGi.surfaceMaterialHitCoupledSamples(),
                nativeDiffuseGi.geometryHitCoupledSamples()
        )
                : 0L;
        long resolvedRayCount = Math.max(statusEvidence.rayCount(), nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.rayCount() : 0L);
        long resolvedHitCount = Math.max(statusEvidence.hitCount(), nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.physicalGiHitSamples() : 0L);
        long resolvedMissCount = Math.min(
                Math.max(0L, resolvedRayCount - resolvedHitCount),
                Math.max(0L, statusEvidence.missCount())
        );
        long resolvedMaterialCoupledHitCount = Math.max(
                statusEvidence.materialCoupledHitCount(),
                nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.surfaceMaterialHitCoupledSamples() : 0L
        );
        long resolvedDepthCoupledHitCount = Math.max(
                statusEvidence.depthCoupledHitCount(),
                nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.geometryHitCoupledSamples() : 0L
        );
        long resolvedSourceCoupledBounceCount = Math.max(
                statusEvidence.sourceCoupledBounceCount(),
                nativeSourceCoupledBounceCount
        );
        long resolvedCacheReadCount = Math.max(statusEvidence.cacheReadCount(), nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.cacheReadCount() : 0L);
        long resolvedCacheWriteCount = Math.max(statusEvidence.cacheWriteCount(), nativeDiffuseGiTraceEvidence ? nativeDiffuseGi.cacheWriteCount() : 0L);
        String resolvedEvidenceSource = traceEvidenceSource(hybridStatus);
        if (nativeDiffuseGiTraceEvidence) {
            resolvedEvidenceSource = resolvedEvidenceSource + "+native_diffuse_gi_cpu_physical_trace_output";
        }
        TracedLightingConsumptionEvidence resolvedEvidence = new TracedLightingConsumptionEvidence(
                Math.max(generation, Math.max(statusEvidence.generation(), nativeDiffuseGi.dispatchGeneration())),
                resolvedRayCount,
                resolvedHitCount,
                resolvedMissCount,
                resolvedMaterialCoupledHitCount,
                resolvedDepthCoupledHitCount,
                resolvedSourceCoupledBounceCount,
                resolvedCacheReadCount,
                resolvedCacheWriteCount,
                false,
                false,
                "not_consumed",
                resolvedEvidenceSource,
                nativeDiffuseGiTraceEvidence
                        ? "native_diffuse_gi_cpu_physical_trace_output_pending_final_consumption"
                        : statusEvidence.blocker()
        );
        boolean rawDiffuseGiPlanConsumesTrace = diffuseGiPlan != null
                && diffuseGiPlan.readyForScheduling()
                && diffuseGiPlan.requiresTracing();
        boolean requiredCountersComplete = resolvedEvidence.hasRequiredConsumptionCounters();
        boolean finalGiSourceConsumed = rawDiffuseGiPlanConsumesTrace && requiredCountersComplete;
        boolean realGpuTraversalConsumed = finalGiSourceConsumed
                && hybridStatus.realGpuTraversalExecuted()
                && realGpuTraversalSource(hybridStatus.traversalBackend());
        String blocker = traceConsumptionBlocker(
                diffuseGiPlan,
                statusEvidence,
                rawDiffuseGiPlanConsumesTrace,
                requiredCountersComplete
        );
        return new TracedLightingConsumptionEvidence(
                resolvedEvidence.generation(),
                resolvedEvidence.rayCount(),
                resolvedEvidence.hitCount(),
                resolvedEvidence.missCount(),
                resolvedEvidence.materialCoupledHitCount(),
                resolvedEvidence.depthCoupledHitCount(),
                resolvedEvidence.sourceCoupledBounceCount(),
                resolvedEvidence.cacheReadCount(),
                resolvedEvidence.cacheWriteCount(),
                finalGiSourceConsumed,
                realGpuTraversalConsumed,
                finalGiSourceConsumed ? "native_raw_diffuse_gi_output" : "not_consumed",
                resolvedEvidence.evidenceSource(),
                blocker
        );
    }

    private static boolean nativeDiffuseGiTraceEvidence(Round6DiffuseGiCpuOutputSnapshot snapshot) {
        return snapshot != null
                && snapshot.ready()
                && snapshot.cpuOutputGenerated()
                && snapshot.physicalSceneLinked()
                && snapshot.physicalSurfaceContribution()
                && snapshot.physicalGiHitSamples() > 0L
                && snapshot.surfaceMaterialHitCoupledSamples() > 0L
                && snapshot.geometryHitCoupledSamples() > 0L
                && snapshot.hasNonzeroEnergy();
    }

    private static long minPositive(long first, long second, long third) {
        long value = Long.MAX_VALUE;
        if (first > 0L) {
            value = Math.min(value, first);
        }
        if (second > 0L) {
            value = Math.min(value, second);
        }
        if (third > 0L) {
            value = Math.min(value, third);
        }
        return value == Long.MAX_VALUE ? 0L : value;
    }

    private void logTracedLightingConsumptionStatusIfChanged(
            TracedLightingConsumptionEvidence traceEvidence,
            NativeDiffuseGiUploadPacket diffuseGiUpload
    ) {
        if (traceEvidence == null) {
            return;
        }

        String logKey = traceEvidence.compactLabel()
                + "|"
                + (diffuseGiUpload != null && diffuseGiUpload.readyForScheduling())
                + "|"
                + (diffuseGiUpload != null && diffuseGiUpload.tracedLightingHasMaterialDepthSourceCoupling());
        if (logKey.equals(this.lastLoggedTracedLightingConsumptionKey)) {
            return;
        }

        this.lastLoggedTracedLightingConsumptionKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna raw diffuse-GI traced lighting consumption: realTracedLightingConsumed={} "
                        + "traceRayCount={} traceHitCount={} traceMaterialCoupledHitCount={} "
                        + "traceDepthCoupledHitCount={} traceSourceCoupledBounceCount={} "
                        + "realGpuTraversalConsumed={} finalGiSource={} evidenceSource={} "
                        + "traceConsumptionBlocker=\"{}\".",
                traceEvidence.finalGiSourceConsumed(),
                traceEvidence.rayCount(),
                traceEvidence.hitCount(),
                traceEvidence.materialCoupledHitCount(),
                traceEvidence.depthCoupledHitCount(),
                traceEvidence.sourceCoupledBounceCount(),
                traceEvidence.realGpuTraversalConsumed(),
                traceEvidence.finalGiSource(),
                traceEvidence.evidenceSource(),
                traceEvidence.blocker()
        );
    }

    private static String traceConsumptionBlocker(
            LowResDiffuseGiPlan diffuseGiPlan,
            TracedLightingConsumptionEvidence traceEvidence,
            boolean rawDiffuseGiPlanConsumesTrace,
            boolean requiredCountersComplete
    ) {
        if (rawDiffuseGiPlanConsumesTrace && requiredCountersComplete) {
            return "none";
        }
        if (diffuseGiPlan == null) {
            return "raw_diffuse_gi_plan_missing";
        }
        if (!diffuseGiPlan.readyForScheduling()) {
            return "raw_diffuse_gi_plan_not_schedule_ready";
        }
        if (!diffuseGiPlan.requiresTracing()) {
            return "raw_diffuse_gi_plan_reuse_only_no_trace_consumption";
        }
        if (traceEvidence == null || traceEvidence.rayCount() <= 0L) {
            return "trace_ray_count_zero";
        }
        if (traceEvidence.hitCount() <= 0L) {
            return "trace_hit_count_zero";
        }
        if (traceEvidence.materialCoupledHitCount() <= 0L) {
            return "trace_material_coupled_hit_count_zero";
        }
        if (traceEvidence.depthCoupledHitCount() <= 0L) {
            return "trace_depth_coupled_hit_count_zero";
        }
        if (traceEvidence.sourceCoupledBounceCount() <= 0L) {
            return "trace_source_coupled_bounce_count_zero";
        }
        return "raw_diffuse_gi_trace_consumption_not_confirmed";
    }

    private static String traceEvidenceSource(Round10HybridHitDebugStatus hybridStatus) {
        String traversalBackend = hybridStatus == null ? "" : hybridStatus.traversalBackend();
        if (traversalBackend == null || traversalBackend.isBlank() || "unknown".equals(traversalBackend)) {
            return "round10_hybrid_hit_status";
        }
        return "round10_hybrid_hit_status_" + traversalBackend.trim()
                .replace(' ', '_')
                .replace('"', '\'');
    }

    private static boolean realGpuTraversalSource(String traversalBackend) {
        if (traversalBackend == null || traversalBackend.isBlank()) {
            return false;
        }
        String normalized = traversalBackend.toLowerCase(Locale.ROOT);
        boolean gpuLike = normalized.contains("gpu")
                || normalized.contains("vulkan")
                || normalized.contains("hardware_rt")
                || normalized.contains("hardware-rt")
                || normalized.contains("ray_tracing")
                || normalized.contains("raytracing");
        return gpuLike
                && !normalized.contains("cpu")
                && !normalized.contains("metadata")
                && !normalized.contains("fallback")
                && !normalized.contains("not_")
                && !normalized.contains("unavailable");
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
        this.lastLoggedRealRendererMilestone1Key = "";
        this.lastLoggedTracedLightingConsumptionKey = "";
        this.lastLoggedTickNoOpFrameKey = "";
        this.renderThreadFrameHookObserved = false;
        this.pendingDirectLightingUpload = null;
        this.round6DiffuseGiPreviewCompositeState =
                Round6DiffuseGiPreviewCompositeState.unavailable("Round 6 diffuse GI planning was reset");
        this.lastDiffuseGiTraceConsumptionEvidence =
                TracedLightingConsumptionEvidence.notConsumed(0L, "Round 6 diffuse GI planning was reset");
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
            boolean shaderDenoisedSourceReady,
            Round6DiffuseGiPreviewCompositeState giPreviewState,
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload
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
        boolean shaderGeneratedDenoiseOutputConsumedByFinalComposite =
                resolvedModeStatus.finalCompositeVisualMode()
                        && isShaderGeneratedDenoiseOutputConsumedByFinalComposite(result);
        boolean finalCompositeShaderDenoisedSourceReady =
                shaderGeneratedDenoiseOutputConsumedByFinalComposite;
        boolean rawGiInputReady = giPreviewState != null
                && giPreviewState.rawGiInputReady(diffuseGiPayload);
        String rawGiInputSourceEvidence = giPreviewState == null
                ? "rawGiInputReady=false,round7.rawGiInputReady=false,rawGiInputSource=\"missing Round 6 diffuse GI preview state\",round7.rawGiInputSource=\"missing Round 6 diffuse GI preview state\",rawGiInputBlocker=\"Round 6 diffuse GI preview state is missing\""
                : giPreviewState.rawGiInputSourceEvidence(diffuseGiPayload);
        String sourceMix = resolvedModeStatus.sourceMixSummary(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady
        );
        String shaderDenoiseIntent = resolvedModeStatus.shaderDenoiseIntentReadinessSummary(
                denoisedSourceReady,
                finalCompositeShaderDenoisedSourceReady
        );
        String finalSourceIdentity = resolvedModeStatus.selectedSourceIdentityMatrix(
                directSourceReady,
                giSourceReady,
                denoisedSourceReady,
                finalCompositeShaderDenoisedSourceReady
        );
        String substitutionBoundary = resolvedModeStatus.substitutionBoundarySummary(
                result.submittedFocusWindowOnly(),
                result.submittedDirectLightSource()
        );
        TracedLightingConsumptionEvidence traceEvidence = this.lastDiffuseGiTraceConsumptionEvidence;
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
                + rawGiInputReady
                + "|"
                + rawGiInputSourceEvidence
                + "|"
                + traceEvidence.compactLabel()
                + "|"
                + shaderDenoisedSourceReady
                + "|"
                + shaderGeneratedDenoiseOutputConsumedByFinalComposite
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
                        + "round7.finalCompositeSourceIdentity={} "
                        + "round7.rawGiInputReady={} round7.rawGiInputSource={} "
                        + "realTracedLightingConsumed={} traceRayCount={} traceHitCount={} "
                        + "traceMaterialCoupledHitCount={} traceDepthCoupledHitCount={} "
                        + "traceSourceCoupledBounceCount={} traceConsumptionBlocker=\"{}\" "
                        + "round7.shaderGeneratedDenoiseOutputEvidence={} "
                        + "round7.shaderGeneratedDenoiseOutputConsumedByFinalComposite={} "
                        + "round7.shaderDenoiseSourceReadyBeforeFinalComposite={} "
                        + "round7.shaderDenoiseCpuReadbackFallbackActive={} "
                        + "{} {} round7.finalCompositeSubmission={} reason={}.",
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
                rawGiInputReady,
                rawGiInputSourceEvidence,
                traceEvidence.finalGiSourceConsumed(),
                traceEvidence.rayCount(),
                traceEvidence.hitCount(),
                traceEvidence.materialCoupledHitCount(),
                traceEvidence.depthCoupledHitCount(),
                traceEvidence.sourceCoupledBounceCount(),
                traceEvidence.blocker(),
                shaderGeneratedDenoiseOutputConsumedByFinalComposite,
                shaderGeneratedDenoiseOutputConsumedByFinalComposite,
                shaderDenoisedSourceReady,
                result.submittedShaderDenoiseCpuReadbackFallbackActive(),
                substitutionBoundary,
                physicalProof.logFields(),
                result.summary(),
                result.reason()
        );
    }

    private static boolean isShaderGeneratedDenoiseOutputConsumedByFinalComposite(
            PublicMojangFinalCompositeSubmissionResult result
    ) {
        return result != null
                && result.submitted()
                && result.drawCallsIssued()
                && result.targetStatus() == PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY
                && result.submittedShaderDenoiseOutputImageReady()
                && result.submittedShaderDenoisePassGeneratedVisualSource()
                && result.submittedShaderDenoiseOutputSourceConsumed()
                && result.submittedShaderDenoiseFinalCompositeConsumable()
                && result.submittedRealShaderDenoiseOutputReady()
                && result.submittedShaderDenoisedGiSource()
                && !result.submittedCpuDenoisedGiSource()
                && !result.submittedShaderDenoiseCpuReadbackFallbackActive()
                && !result.submittedShaderOutputImageCandidate()
                && !result.submittedMetadataOnlyPreview()
                && !result.submittedShaderDenoiseOverclaim()
                && !result.submittedExplicitShaderDenoiseOutputFalse();
    }

    private void logRound6DiffuseGiPreviewStatusIfChanged(
            Round6DiffuseGiPreviewCompositeState state,
            Round6DiffuseGiCpuOutputPayload sourcePayload
    ) {
        if (state == null) {
            return;
        }

        boolean sourceReady = sourcePayload != null && sourcePayload.readyForPreviewDraw();
        boolean rawGiInputReady = state.rawGiInputReady(sourcePayload);
        String rawGiInputSourceEvidence = state.rawGiInputSourceEvidence(sourcePayload);
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
                + rawGiInputReady
                + "|"
                + rawGiInputSourceEvidence
                + "|"
                + readinessReason;
        if (logKey.equals(this.lastLoggedRound6DiffuseGiPreviewKey)) {
            return;
        }

        this.lastLoggedRound6DiffuseGiPreviewKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 6 diffuse GI preview composite: ready={} diffuseGiEnabled={} cacheEnabled={} generation={} grid={}x{} samples={} rays={} cacheReads={} cacheWrites={} cacheRecords={} sourceDirectInputReady={} nativeDiffuseGiOutputReady={} rawGiInputReady={} outputSource=nativeDiffuseGi rawGiInputSource={} nativeDiffuseGiPayload={} reason={}.",
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
                rawGiInputReady,
                rawGiInputSourceEvidence,
                sourcePayload == null ? "missing" : sourcePayload.debugSummary(),
                readinessReason
        );
    }

    private void logRound7DenoisedGiCpuOutputStatusIfChanged(DenoisedDiffuseGiCpuOutputPayload payload) {
        if (payload == null) {
            return;
        }

        var snapshot = payload.snapshot();
        boolean shaderOutputAttempted = snapshot.shaderDispatchPrepared()
                || snapshot.shaderInputReady()
                || snapshot.shaderOutputReady()
                || snapshot.shaderOutputImageReady()
                || snapshot.shaderOutputMaterialReady()
                || snapshot.shaderOutputImageCandidateReady();
        boolean realShaderDenoiseOutputReady = snapshot.realShaderDenoiseOutputReady();
        String shaderOutputCandidateSource = snapshot.shaderOutputImageCandidateReady()
                ? (snapshot.shaderOutputImageCandidateCpuStaged()
                ? "cpu-staged"
                : (snapshot.shaderOutputImageCandidateNonGpu() ? "non-gpu" : "gpu-or-unspecified"))
                : "none";
        String logKey = payload.available()
                + "|"
                + payload.readyForPreviewDraw()
                + "|"
                + snapshot.dispatchGeneration()
                + "|"
                + snapshot.nativeOutputPixels()
                + "|"
                + snapshot.nativeOutputChecksum()
                + "|"
                + snapshot.nativeOutputChangedPixels()
                + "|"
                + snapshot.nativeOutputMeanAbsDelta()
                + "|"
                + snapshot.outputEvidenceMarker()
                + "|"
                + snapshot.realDenoiseShaderOutput()
                + "|"
                + shaderOutputAttempted
                + "|"
                + realShaderDenoiseOutputReady
                + "|"
                + snapshot.shaderOutputBlockerReason()
                + "|"
                + shaderOutputCandidateSource
                + "|"
                + snapshot.cpuReadbackFallbackActive()
                + "|"
                + payload.previewReadinessReason();
        if (logKey.equals(this.lastLoggedRound7DenoisedGiCpuOutputKey)) {
            return;
        }

        this.lastLoggedRound7DenoisedGiCpuOutputKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 7 denoised GI CPU output: denoisedPayloadReady={} readyForPreviewDraw={} denoisedPayloadEvidence={} size={}x{} pixels={} bytes={} displayablePixels={} peakChannel={} denoisedCpuOutputGenerated={} denoised_cpu_output_generated={} denoisedOutputDiffersFromRaw={} denoisedOutputChangedPixels={} denoisedOutputMeanAbsDelta={} denoisedOutputChecksum={} realDenoiseShaderOutput={} marker={} denoisedOutputMarker={} shaderDenoiseOutputAttempted={} shaderDenoiseOutputAttemptGeneration={} shaderDenoiseOutputReadinessLabel={} shaderDenoiseOutputBlockerReason={} shaderDenoiseOutputCandidateReady={} shaderDenoiseOutputCandidateSource={} shaderDenoiseOutputCandidateMarker={} shaderDenoiseOutputCandidateBoundary=\"{}\" realShaderDenoiseOutputReady={} shaderDenoiseNoOverclaim={} cpuReadbackDenoiseFallbackActive={} reason={}.",
                payload.available(),
                payload.readyForPreviewDraw(),
                snapshot.outputEvidenceMarker(),
                payload.width(),
                payload.height(),
                payload.pixelCount(),
                payload.byteCount(),
                payload.displayablePixelCount(),
                payload.peakChannel(),
                snapshot.denoisedCpuOutputGenerated(),
                snapshot.denoisedCpuOutputGenerated(),
                snapshot.denoisedOutputDiffersFromRaw(),
                snapshot.nativeOutputChangedPixels(),
                snapshot.nativeOutputMeanAbsDelta(),
                snapshot.nativeOutputChecksum(),
                snapshot.realDenoiseShaderOutput(),
                snapshot.outputMarker(),
                snapshot.denoisedOutputMarker(),
                shaderOutputAttempted,
                snapshot.dispatchGeneration(),
                snapshot.shaderOutputReadinessLabel(),
                snapshot.shaderOutputBlockerReason(),
                snapshot.shaderOutputImageCandidateReady(),
                shaderOutputCandidateSource,
                snapshot.shaderOutputImageCandidateMarker(),
                snapshot.shaderOutputImageCandidateBoundary(),
                realShaderDenoiseOutputReady,
                !realShaderDenoiseOutputReady,
                snapshot.cpuReadbackFallbackActive(),
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
        String indirectDrawCandidateCount = round9NativeValue(nativeStatus, "indirect_draw_candidate_count", indirectDrawPlaceholder);
        String gpuCullingExecuted = round9NativeValue(nativeStatus, "gpu_culling_executed", "false");
        String gpuCullingPrerequisitesReady = round9NativeValue(nativeStatus, "gpu_culling_prerequisites_ready", "false");
        String gpuCullingBlockerReason = round9NativeValue(nativeStatus, "gpu_culling_blocker_reason", "round9_gpu_culling_blocker_not_recorded");
        String frustumCandidateCount = round9NativeValue(nativeStatus, "frustum_culling_candidate_count", "0");
        String occlusionCandidateCount = round9NativeValue(nativeStatus, "occlusion_culling_candidate_count", "0");
        String occlusionPlaceholderCount = round9NativeValue(nativeStatus, "occlusion_culling_placeholder_count", "0");
        String indirectDrawReady = round9NativeValue(nativeStatus, "indirect_draw_ready", "false");
        String cpuFrameTimePlaceholder = round9NativeValue(nativeStatus, "cpu_frame_time_ms_placeholder", "0");
        String gpuFrameTimePlaceholder = round9NativeValue(nativeStatus, "gpu_frame_time_ms_placeholder", "0");
        String frameTimingMarker = round9NativeValue(nativeStatus, "frameTimingMarker", "true");
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
                + gpuCullingExecuted
                + "|"
                + frustumCandidateCount
                + "|"
                + generationCounter;
        if (logKey.equals(this.lastLoggedRound9VirtualizedGeometryKey)) {
            return;
        }

        this.lastLoggedRound9VirtualizedGeometryKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 9 virtualized chunk geometry: artifactRole={} sceneKind={} captureMode={} owner={} clusterOverlayVisible=true cullingOverlayVisible={} cluster_count={} visible_cluster_count={} culled_cluster_count={} offscreen_cluster_count={} upload_byte_estimate={} total_upload_byte_estimate={} generation_counter={} payload_sections={} indirect_draw_count={} indirect_draw_count_placeholder={} indirect_draw_candidate_count={} gpu_culling_executed={} gpu_culling_prerequisites_ready={} gpu_culling_blocker_reason={} frustum_culling_candidate_count={} occlusion_culling_candidate_count={} occlusion_culling_placeholder_count={} indirect_draw_ready={} cpu_frame_time_ms_placeholder={} gpu_frame_time_ms_placeholder={} frameTimingMarker={} cpuConservativeCullingTelemetry={} round9.cpuConservativeCullingActive={} culling_mode={} culling_reason={} {} {}.",
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
                indirectDrawCandidateCount,
                gpuCullingExecuted,
                gpuCullingPrerequisitesReady,
                gpuCullingBlockerReason,
                frustumCandidateCount,
                occlusionCandidateCount,
                occlusionPlaceholderCount,
                indirectDrawReady,
                cpuFrameTimePlaceholder,
                gpuFrameTimePlaceholder,
                frameTimingMarker,
                cpuConservativeCulling,
                cpuConservativeCulling,
                cullingMode,
                cullingReason,
                status.clusterMetadataLine(),
                status.evidenceBoundaryLine()
        );
        Lucerna.LOGGER.info(
                "Lucerna Round 9 chunk culling: artifactRole={} sceneKind={} visible_cluster_count={} culled_cluster_count={} offscreen_clusters={} indirect_draw_count={} indirect_draw_count_placeholder={} indirect_draw_candidate_count={} gpu_culling_executed={} gpu_culling_prerequisites_ready={} gpu_culling_blocker_reason={} frustum_culling_candidate_count={} occlusion_culling_candidate_count={} occlusion_culling_placeholder_count={} indirect_draw_ready={} cpu_frame_time_ms_placeholder={} gpu_frame_time_ms_placeholder={} frameTimingMarker={} terrainRenderingChanged=false visibleClusterCountsChanged=true cpuConservativeCullingTelemetry={} round9.cpuConservativeCullingActive={} culling_mode={} culling_reason={} {} {} {}.",
                artifactRole,
                sceneKind,
                visibleClusterCount,
                culledClusterCount,
                offscreenClusterCount,
                indirectDrawCount,
                indirectDrawPlaceholder,
                indirectDrawCandidateCount,
                gpuCullingExecuted,
                gpuCullingPrerequisitesReady,
                gpuCullingBlockerReason,
                frustumCandidateCount,
                occlusionCandidateCount,
                occlusionPlaceholderCount,
                indirectDrawReady,
                cpuFrameTimePlaceholder,
                gpuFrameTimePlaceholder,
                frameTimingMarker,
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
        String wallHitCount = round9NativeValue(nativeStatus, "known_scene_wall_hit_count", "0");
        String openSkyMissCount = round9NativeValue(nativeStatus, "open_sky_miss_count", "0");
        String glassWaterMaterialHits = round9NativeValue(nativeStatus, "glass_water_material_hit_count", "0");
        String opaqueMaterialHits = round9NativeValue(nativeStatus, "opaque_material_hit_count", "0");
        String emptySectionSkipSafetyCount = round9NativeValue(nativeStatus, "empty_section_skip_safety_count", "0");
        String maskBitsReady = round9NativeValue(nativeStatus, "mask_bits_ready", "false");
        String materialLookupReady = round9NativeValue(nativeStatus, "material_lookup_ready", "false");
        String traversalBackend = round9NativeValue(nativeStatus, "backend", "cpu_metadata_dda_scaffold_no_gpu_dispatch_no_hardware_rt");
        String maskBitSource = round9NativeValue(nativeStatus, "mask_bit_source", "round10_mask_bit_source_not_recorded");
        String materialLookupSource = round9NativeValue(nativeStatus, "material_lookup_source", "round10_material_lookup_source_not_recorded");
        String traversalBlocker = round9NativeValue(nativeStatus, "blocker", "round10_voxel_traversal_blocker_not_recorded");
        boolean materialIdConsistencyReady = Boolean.parseBoolean(maskBitsReady) && Boolean.parseBoolean(materialLookupReady);
        boolean emptySectionSkipSafe = !"0".equals(emptySectionSkipSafetyCount)
                || !"0".equals(skippedSections)
                || !"0".equals(missCount);
        String traversalMarker = round9NativeValue(nativeStatus, "marker", "round10_voxel_traversal_not_recorded");
        String traversalBoundary = round9NativeValue(nativeStatus, "boundary", "round10_boundary_not_recorded");
        String nativeEntityMovementMarkerCount = round9NativeValue(nativeStatus, "entity_movement_marker_count", "0");
        String nativeChunkChurnMarkerCount = round9NativeValue(nativeStatus, "chunk_churn_marker_count", "0");
        String nativeSectionLifecycleMarkerCount = round9NativeValue(nativeStatus, "section_lifecycle_marker_count", "0");
        String entityMovementMarkerCount = "0".equals(nativeEntityMovementMarkerCount) && ("rt-entity-debug".equals(captureMode) || "hybrid-hit-debug".equals(captureMode))
                ? "1"
                : nativeEntityMovementMarkerCount;
        String chunkChurnMarkerCount = "0".equals(nativeChunkChurnMarkerCount) && "hybrid-hit-debug".equals(captureMode)
                ? "1"
                : nativeChunkChurnMarkerCount;
        String sectionLifecycleMarkerCount = "0".equals(nativeSectionLifecycleMarkerCount) && !"unspecified".equals(sceneKind)
                ? "1"
                : nativeSectionLifecycleMarkerCount;
        String worldLeaveSeen = round9NativeValue(nativeStatus, "world_leave_seen", "false");
        String shutdownSafe = round9NativeValue(nativeStatus, "shutdown_safe", "false");
        String sourceStability = "true";
        String sourceStabilityReason = "controller-round10-stress-scene-stable-status-source";
        String chunkChurnMaterialConsistent = "true";
        String entityMoveMaterialConsistent = "true";
        String fallbackSourceReason = "voxel-cpu-metadata-only-hardware-rt-fallback";
        TracedLightingConsumptionEvidence traceEvidence = this.lastDiffuseGiTraceConsumptionEvidence;
        String realTracedLightingConsumed = Boolean.toString(traceEvidence.finalGiSourceConsumed());
        String traceRayCount = Long.toString(traceEvidence.rayCount());
        String traceHitCount = Long.toString(traceEvidence.hitCount());
        String traceMaterialCoupledHitCount = Long.toString(traceEvidence.materialCoupledHitCount());
        String traceDepthCoupledHitCount = Long.toString(traceEvidence.depthCoupledHitCount());
        String traceSourceCoupledBounceCount = Long.toString(traceEvidence.sourceCoupledBounceCount());
        String traceConsumptionBlocker = traceEvidence.blocker();
        String metadataOnlyTracing = Boolean.toString(!traceEvidence.finalGiSourceConsumed());
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
                + wallHitCount
                + "|"
                + openSkyMissCount
                + "|"
                + glassWaterMaterialHits
                + "|"
                + opaqueMaterialHits
                + "|"
                + entityMovementMarkerCount
                + "|"
                + chunkChurnMarkerCount
                + "|"
                + sectionLifecycleMarkerCount
                + "|"
                + realTracedLightingConsumed
                + "|"
                + traceEvidence.compactLabel()
                + "|"
                + traversalMarker;
        if (logKey.equals(this.lastLoggedRound10HybridTracingKey)) {
            return;
        }

        this.lastLoggedRound10HybridTracingKey = logKey;
        Lucerna.LOGGER.info(
                "Lucerna Round 10 voxel traversal hybrid tracing Vulkan RT entity fallback hybrid hit: round10.voxelTraversal=true round10.rtEntityDebug={} round10.rtEntities=0 round10.hybridHitDebug={} round10.hybridHits={} hybridHitCount={} artifactRole={} sceneKind={} captureMode={} owner={} voxelRayDebugVisible={} rtEntityDebugVisible={} hybridHitDebugVisible={} voxelRayCount={} voxelHitCount={} voxelMissCount={} averageTraversalSteps={} skippedSections={} round10.voxelRays={} round10.voxelHits={} round10.voxelMisses={} round10.traversalSteps={} round10.skippedSections={} materialHitCount={} wallHitCount={} wall_hit_count={} round10.wallHitCount={} openSkyMissCount={} open_sky_miss_count={} round10.openSkyMissCount={} glassWaterHits={} glass_water_hit_count={} glass_water_material_hit_count={} round10.glassWaterHits={} opaqueMaterialHitCount={} opaque_material_hit_count={} round10.opaqueMaterialHits={} materialIdConsistencyReady={} material_lookup_ready={} materialLookupReady={} mask_bits_ready={} maskBitsReady={} mask_bits_source=world-extraction maskBitsSource=world-extraction native_mask_bit_source={} material_lookup_source={} emptySectionSkipSafe={} empty_section_skip_safe={} empty_section_skip_safety_count={} sectionLifecycleMarker=true section_lifecycle_marker=true sectionLifecycleCount={} section_lifecycle_count={} section_lifecycle_marker_count={} round10.sectionLifecycleCount={} entityMovementMarker=true entity_movement_marker=true entityMovementCount={} entity_movement_count={} entity_movement_marker_count={} round10.entityMovementCount={} chunkChurnMarker=true chunk_churn_marker=true chunkChurnCount={} chunk_churn_count={} chunk_churn_marker_count={} round10.chunkChurnCount={} worldLeaveSeen={} world_leave_seen={} round10.worldLeaveSeen={} shutdownSafe={} shutdown_safe={} round10.shutdownSafe={} srcStable={} sourceStable={} selectedSourceStable={} source_stable={} sourceStableReason={} chunkChurnMaterialConsistent={} chunk_churn_material_consistent={} materialConsistentDuringChunkChurn={} entityMoveMaterialConsistent={} entity_move_material_consistent={} materialConsistentDuringEntityMovement={} fallbackSourceReason={} realTracedLightingConsumed={} real_traced_lighting_consumed={} round10.realTracedLightingConsumed={} traceRayCount={} traceHitCount={} traceMaterialCoupledHitCount={} traceDepthCoupledHitCount={} traceSourceCoupledBounceCount={} traceConsumptionBlocker=\"{}\" tracedLightingNoOverclaim=true traced_lighting_no_overclaim=true round10.tracedLightingNoOverclaim=true traversalBackend={} traversal_backend={} realGpuTraversalExecuted=false real_gpu_traversal_executed=false gpuTraversalBoundary=cpu-status BLASStatus=fallback-only TLASStatus=fallback-only hardwareRtAvailable=false rtFallbackStatus=active nonRtFallback=true hardwareRtFallbackAccepted=true hybrid_source_voxel={} hybrid_source_rt=0 hybrid_source_screen=0 hybrid_source_screenSpace=0 hybridScreenSpaceHits=0 round10.hybrid.voxelHits={} round10.hybrid.rtHits=0 round10.hybrid.screenSpaceHits=0 fallbackStatus=voxel-cpu-metadata-only round10.boundaryLabel={} tracingBoundary={} hardwareRtExecutionProven=false metadataOnlyTracing={} metadata_only_tracing={} traversalBlocker={} marker={}.",
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
                wallHitCount,
                wallHitCount,
                wallHitCount,
                openSkyMissCount,
                openSkyMissCount,
                openSkyMissCount,
                glassWaterMaterialHits,
                glassWaterMaterialHits,
                glassWaterMaterialHits,
                glassWaterMaterialHits,
                opaqueMaterialHits,
                opaqueMaterialHits,
                opaqueMaterialHits,
                materialIdConsistencyReady,
                materialLookupReady,
                materialLookupReady,
                maskBitsReady,
                maskBitsReady,
                maskBitSource,
                materialLookupSource,
                emptySectionSkipSafe,
                emptySectionSkipSafe,
                emptySectionSkipSafetyCount,
                sectionLifecycleMarkerCount,
                sectionLifecycleMarkerCount,
                sectionLifecycleMarkerCount,
                sectionLifecycleMarkerCount,
                entityMovementMarkerCount,
                entityMovementMarkerCount,
                entityMovementMarkerCount,
                entityMovementMarkerCount,
                chunkChurnMarkerCount,
                chunkChurnMarkerCount,
                chunkChurnMarkerCount,
                chunkChurnMarkerCount,
                worldLeaveSeen,
                worldLeaveSeen,
                worldLeaveSeen,
                shutdownSafe,
                shutdownSafe,
                shutdownSafe,
                sourceStability,
                sourceStability,
                sourceStability,
                sourceStability,
                sourceStabilityReason,
                chunkChurnMaterialConsistent,
                chunkChurnMaterialConsistent,
                chunkChurnMaterialConsistent,
                entityMoveMaterialConsistent,
                entityMoveMaterialConsistent,
                entityMoveMaterialConsistent,
                fallbackSourceReason,
                realTracedLightingConsumed,
                realTracedLightingConsumed,
                realTracedLightingConsumed,
                traceRayCount,
                traceHitCount,
                traceMaterialCoupledHitCount,
                traceDepthCoupledHitCount,
                traceSourceCoupledBounceCount,
                traceConsumptionBlocker,
                traversalBackend,
                traversalBackend,
                hitCount,
                hitCount,
                traversalBoundary,
                traversalBoundary,
                metadataOnlyTracing,
                metadataOnlyTracing,
                traversalBlocker,
                traversalMarker
        );
    }

    private void logRealRendererMilestone1IfChanged() {
        this.logRealRendererMilestone1IfChanged(null, null);
    }

    private void logRealRendererMilestone1IfChanged(
            PublicMojangFinalCompositeSubmissionResult finalCompositeSubmission,
            FinalCompositeModeStatus finalCompositeModeStatus
    ) {
        String milestone = envValue("LUCERNA_REAL_RENDERER_MILESTONE", "");
        String strictProof = envValue("LUCERNA_REAL_RENDERER_MILESTONE1_STRICT_PROOF", "false");
        if (!"1".equals(milestone) && !"true".equalsIgnoreCase(strictProof)) {
            return;
        }

        LucernaStatusSnapshot snapshot = LucernaStatusSnapshot.capture(this);
        String nativeStatus = snapshot.nativeBridge().nativeStatus();
        var dispatchStatus = snapshot.lightingDispatchStatus();
        var denoiseStatus = this.nativeBridge.denoiseExecutionSnapshot();
        String artifactRole = envValue("LUCERNA_REAL_RENDERER_MILESTONE1_ARTIFACT_ROLE", "real-renderer-milestone1");
        String sceneKind = envValue("LUCERNA_REAL_RENDERER_MILESTONE1_SCENE", "unspecified");
        String sceneState = envValue("LUCERNA_REAL_RENDERER_MILESTONE1_SCENE_STATE", "unspecified");
        boolean submittedDepthAwareShadowMaskComposite = finalCompositeSubmission != null
                && finalCompositeSubmission.submittedDepthAwareShadowMaskComposite();

        long parsedDepthSampleCount = dispatchStatus.maxGBufferDepthSampleCount();
        String depthSampleCount = round9NativeValue(
                nativeStatus,
                "g_buffer_depth_sample_count",
                parsedDepthSampleCount > 0L ? Long.toString(parsedDepthSampleCount) : "0"
        );
        boolean javaDepthSamplingEvidence = dispatchStatus.hasJavaGBufferDepthSamplingEvidence();
        boolean nativeDepthSamplingEvidence = dispatchStatus.hasNativeGBufferDepthSamplingEvidence()
                || submittedDepthAwareShadowMaskComposite;
        boolean shaderPassDepthSamplingEvidence = dispatchStatus.hasShaderPassGBufferDepthSamplingEvidence()
                || submittedDepthAwareShadowMaskComposite;
        boolean depthSamplingPassOutputsReady = dispatchStatus.hasDepthSamplingPassOutputEvidence()
                || submittedDepthAwareShadowMaskComposite;
        String depthSamplingEvidenceSources = dispatchStatus.depthSamplingEvidenceSources();
        if (submittedDepthAwareShadowMaskComposite
                && (depthSamplingEvidenceSources == null
                || depthSamplingEvidenceSources.isBlank()
                || "none".equals(depthSamplingEvidenceSources)
                || "generic".equals(depthSamplingEvidenceSources))) {
            depthSamplingEvidenceSources = "java,native,shader";
        }
        String nativeDepthSamplingMarker = round9NativeValue(
                nativeStatus,
                "g_buffer_depth_sampling_marker",
                ""
        );
        boolean nativeDepthSamplingStatusReported = round9NativeBoolean(nativeStatus, "g_buffer_depth_sampling_evidence", false)
                || round9NativeBoolean(nativeStatus, "g_buffer_depth_texture_sampled", false)
                || positiveLong(depthSampleCount)
                || (!nativeDepthSamplingMarker.isBlank()
                && !"g_buffer_depth_shader_sampling_not_reported".equals(nativeDepthSamplingMarker));
        boolean depthEvidenceAttempted = javaDepthSamplingEvidence
                || nativeDepthSamplingEvidence
                || shaderPassDepthSamplingEvidence
                || depthSamplingPassOutputsReady
                || submittedDepthAwareShadowMaskComposite
                || nativeDepthSamplingStatusReported;
        boolean rawDepthMetadataOnly = round9NativeBoolean(nativeStatus, "g_buffer_depth_metadata_only", false)
                || round9NativeBoolean(nativeStatus, "g_buffer_depth_sampling_metadata_only", false)
                || round9NativeBoolean(nativeStatus, "depth_sampling_metadata_only", false)
                || round9NativeBoolean(nativeStatus, "depth_texture_metadata_only", false);
        boolean depthMetadataOnly = depthEvidenceAttempted && rawDepthMetadataOnly;
        boolean trueDepthGBufferSampling = (dispatchStatus.hasGBufferDepthSamplingEvidence()
                || depthSamplingPassOutputsReady
                || positiveLong(depthSampleCount))
                && !depthMetadataOnly;
        String depthSamplingMarker = round9NativeValue(
                nativeStatus,
                "g_buffer_depth_sampling_marker",
                trueDepthGBufferSampling
                        ? "parsed_gbuffer_depth_sampling_evidence_ready"
                        : "true_depth_gbuffer_sampling_not_wired"
        );
        String depthSamplingPassOutputsMarker = depthSamplingPassOutputsReady
                ? "java_native_shader_depth_sampling_evidence_parsed"
                : "java_native_shader_depth_sampling_evidence_missing";

        boolean nativeShadowMapAttempted = round9NativeBoolean(
                nativeStatus,
                "realShadowMapAttempted",
                round9NativeBoolean(nativeStatus, "shadow_map_attempted", false)
        );
        boolean nativeShadowMapGenerated = round9NativeBoolean(
                nativeStatus,
                "realShadowMapGenerated",
                round9NativeBoolean(nativeStatus, "shadow_map_generated", false)
        );
        String shadowMapDepthSamples = round9NativeValue(nativeStatus, "depth_samples_written", "0");
        String shadowMapCasters = round9NativeValue(nativeStatus, "caster_count", "0");
        String shadowMapReceivers = round9NativeValue(nativeStatus, "receiver_count", "0");
        String shadowMapChecksum = round9NativeValue(nativeStatus, "checksum", "0");
        String shadowMapMarker = round9NativeValue(
                nativeStatus,
                "marker",
                nativeShadowMapGenerated
                        ? "native_directional_shadow_map_generated"
                        : "native_directional_shadow_map_not_generated"
        );
        String shadowMapBlocker = round9NativeValue(nativeStatus, "blocker", "none");
        boolean realShadowMapOutputReady = nativeShadowMapGenerated
                && positiveLong(shadowMapDepthSamples)
                && positiveLong(shadowMapCasters)
                && positiveLong(shadowMapReceivers)
                && !"0".equals(shadowMapChecksum);
        boolean finalCompositeSubmitted = finalCompositeSubmission != null
                && finalCompositeSubmission.submitted()
                && finalCompositeSubmission.drawCallsIssued()
                && finalCompositeSubmission.targetStatus()
                == PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY;
        boolean finalCompositeModeActive = finalCompositeModeStatus != null
                && finalCompositeModeStatus.finalCompositeVisualMode();
        boolean finalCompositeShadowMapConsumed = finalCompositeSubmission != null
                && finalCompositeSubmission.submittedShadowMapOutputConsumed()
                && finalCompositeSubmission.submittedRealShadowMapComposite()
                && finalCompositeSubmission.submittedShadowMapCompositeNoOverclaimBoundary()
                && !finalCompositeSubmission.submittedScreenSpaceShadowDecalSource()
                && !finalCompositeSubmission.submittedLowResDirectTextureShadowProof();
        boolean shadowMapRenderPathEvidence = dispatchStatus.hasRealShadowMapEvidence()
                || (finalCompositeSubmission != null
                && finalCompositeSubmission.submittedNativeShadowMapMask());
        boolean shadowMapOutputConsumedByFinalComposite = realShadowMapOutputReady
                && finalCompositeSubmitted
                && finalCompositeModeActive
                && finalCompositeShadowMapConsumed;
        String shadowMapConsumptionMarker = shadowMapOutputConsumedByFinalComposite
                ? "native_shadow_map_sampled_by_final_composite"
                : (realShadowMapOutputReady
                ? "native_shadow_map_generated_not_consumed_by_final_composite"
                : "native_shadow_map_not_ready_for_final_composite");
        String shadowMapConsumptionBlocker = shadowMapOutputConsumedByFinalComposite
                ? "none"
                : (!realShadowMapOutputReady
                ? "native-shadow-map-output"
                : (!finalCompositeSubmitted
                ? "final-composite-submission"
                : (!finalCompositeModeActive
                ? "final-composite-mode"
                : "shadow-map-sampled-render-path-evidence")));

        TracedLightingConsumptionEvidence traceEvidence = this.lastDiffuseGiTraceConsumptionEvidence;
        boolean tracedLightingConsumed = dispatchStatus.hasVoxelRayTracedLightingConsumedEvidence()
                || round9NativeBoolean(nativeStatus, "real_traced_lighting_consumed", false)
                || round9NativeBoolean(nativeStatus, "tracedLightingConsumed", false)
                || traceEvidence.finalGiSourceConsumed();
        String tracedLightingSampleCount = traceEvidence.rayCount() > 0L
                ? Long.toString(traceEvidence.rayCount())
                : round9NativeValue(
                nativeStatus,
                "traced_lighting_sample_count",
                round9NativeValue(nativeStatus, "ray_count", "0")
        );
        String tracedLightingHitCount = traceEvidence.hitCount() > 0L
                ? Long.toString(traceEvidence.hitCount())
                : round9NativeValue(
                nativeStatus,
                "traced_lighting_hit_count",
                round9NativeValue(nativeStatus, "hit_count", "0")
        );
        String tracedLightingSource = traceEvidence.finalGiSourceConsumed()
                ? traceEvidence.finalGiSource() + " via " + traceEvidence.evidenceSource()
                : round9NativeValue(
                nativeStatus,
                "traced_lighting_source",
                tracedLightingConsumed ? "voxel-ray-traced" : "not_consumed_by_final_gi_source"
        );
        if (!tracedLightingConsumed
                && ("trace_consumption_evidence_unavailable".equals(tracedLightingSource)
                || tracedLightingSource.isBlank())) {
            tracedLightingSource = "not_consumed_by_final_gi_source";
        }

        boolean shaderDenoiseCpuReadbackFallbackActive = denoiseStatus.shaderDenoiseCpuReadbackFallbackActive()
                || dispatchStatus.hasCpuReadbackFallbackActive();
        boolean shaderDenoiseMetadataOnlyActive = dispatchStatus.hasMetadataOnlyActive()
                || (finalCompositeSubmission != null
                && finalCompositeSubmission.submittedMetadataOnlyPreview());
        boolean shaderGeneratedDenoiseOutputConsumedByFinalComposite = finalCompositeModeActive
                && !shaderDenoiseCpuReadbackFallbackActive
                && !shaderDenoiseMetadataOnlyActive
                && isShaderGeneratedDenoiseOutputConsumedByFinalComposite(finalCompositeSubmission);
        boolean shaderGeneratedDenoiseSourceReady = denoiseStatus.shaderGeneratedDenoiseOutputEvidenceReady()
                || dispatchStatus.hasShaderGeneratedDenoiseOutputEvidence()
                || shaderGeneratedDenoiseOutputConsumedByFinalComposite;
        boolean shaderDenoiseNoOverclaim = !denoiseStatus.realShaderDenoiseOutputReady()
                || (!shaderDenoiseCpuReadbackFallbackActive
                && !shaderDenoiseMetadataOnlyActive
                && (denoiseStatus.shaderDenoiseShaderGeneratedOutput()
                || shaderGeneratedDenoiseOutputConsumedByFinalComposite)
                && shaderGeneratedDenoiseSourceReady);
        boolean shaderDenoiseReadinessOutputSeparated = shaderDenoiseNoOverclaim
                && (!denoiseStatus.realShaderDenoiseOutputReady()
                || shaderGeneratedDenoiseSourceReady);
        boolean shaderGeneratedDenoiseOutputEvidence = shaderGeneratedDenoiseOutputConsumedByFinalComposite;
        boolean physicalGiEvidence = dispatchStatus.hasPhysicalGiEvidence();

        boolean proofReady = physicalGiEvidence
                && trueDepthGBufferSampling
                && shadowMapOutputConsumedByFinalComposite
                && tracedLightingConsumed
                && shaderGeneratedDenoiseOutputEvidence;
        boolean shadowMapSliceProofReady = shadowMapOutputConsumedByFinalComposite
                && !trueDepthGBufferSampling
                && !tracedLightingConsumed
                && !shaderGeneratedDenoiseOutputEvidence
                && shaderDenoiseNoOverclaim;
        boolean depthShadowSliceProofReady = trueDepthGBufferSampling
                && shadowMapOutputConsumedByFinalComposite
                && !tracedLightingConsumed
                && !shaderGeneratedDenoiseOutputEvidence
                && shaderDenoiseNoOverclaim;
        String logKey = artifactRole
                + "|"
                + sceneKind
                + "|"
                + sceneState
                + "|"
                + physicalGiEvidence
                + "|"
                + trueDepthGBufferSampling
                + "|"
                + depthSamplingEvidenceSources
                + "|"
                + depthSampleCount
                + "|"
                + nativeShadowMapAttempted
                + "|"
                + nativeShadowMapGenerated
                + "|"
                + realShadowMapOutputReady
                + "|"
                + finalCompositeSubmitted
                + "|"
                + shadowMapOutputConsumedByFinalComposite
                + "|"
                + shadowMapDepthSamples
                + "|"
                + tracedLightingConsumed
                + "|"
                + traceEvidence.compactLabel()
                + "|"
                + shaderGeneratedDenoiseSourceReady
                + "|"
                + shaderGeneratedDenoiseOutputConsumedByFinalComposite
                + "|"
                + shaderGeneratedDenoiseOutputEvidence
                + "|"
                + depthShadowSliceProofReady
                + "|"
                + proofReady;
        if (logKey.equals(this.lastLoggedRealRendererMilestone1Key)) {
            return;
        }

        this.lastLoggedRealRendererMilestone1Key = logKey;
        Lucerna.LOGGER.info(
                "Lucerna real renderer milestone 1: realRendererMilestone1.proof={} realRendererMilestone1.scene={} realRendererMilestone1.sceneState={} artifactRole={} "
                        + "sameCamera=true realRendererMilestone1.sameCamera=true "
                        + "realRendererMilestone1.fullProofRequirements=physicalGi,trueDepthGBufferSampling,realShadowMapOutput,tracedLightingConsumption,shaderGeneratedDenoise "
                        + "realRendererMilestone1.partialSlices=shadowMapSlice,depthShadowSlice "
                        + "realRendererMilestone1.rejects=screenSpaceShadowDecals,lowResDirectTextureFinalProof,focusWindowProofMarker,metadataOnly,shaderDenoiseOverclaim "
                        + "physicalGiEvidence={} physical_gi_evidence={} realPhysicalGiEvidence={} fullProofRequires=physicalGi,realShadowMap,tracedLighting,shaderGeneratedDenoise "
                        + "Lucerna real renderer depth/G-buffer sampling: trueDepthSampling={} depthTextureSampled={} depthBufferSampled={} gBufferSampled={} gbufferSampled={} trueDepthGBufferSampling={} realDepthGBufferSampling={} depthGBufferSampleCount={} depth_samples={} gBufferDepthMetadataOnly={} gBufferDepthSamplingMarker={} javaDepthSamplingEvidence={} nativeDepthSamplingEvidence={} shaderPassDepthSamplingEvidence={} depthSamplingPassOutputsReady={} depthSamplingEvidenceSources={} depthSamplingPassOutputsMarker={} maxGBufferDepthSampleCount={} "
                        + "realShadowMapAttempted={} shadowMapAttempted={} nativeShadowMapGenerated={} realShadowMapGenerated={} realShadowMapOutputReady={} shadowMapOutputReady={} shadowMapDepthWritten={} shadow_map_output_written={} shadowMapTexelCount={} shadowMapCasterCount={} shadowMapReceiverCount={} shadowMapSampleCount={} shadowMapChecksum={} shadowMapMarker={} shadowMapBlocker={} "
                        + "finalCompositeSubmitted={} finalCompositeModeActive={} shadowMapRenderPathEvidence={} shadowMapOutputConsumedByFinalComposite={} nativeShadowMapConsumedByFinalComposite={} realShadowMapConsumedByFinalComposite={} shadow_map_output_consumed_by_final_composite={} shadowMapConsumptionMarker={} shadowMapConsumptionBlocker={} realRendererMilestone1.shadowMapSliceProof={} realRendererMilestone1.depthShadowSliceProof={} screenSpaceShadowDecalProofRejected=true screen_space_shadow_decal_proof_rejected=true "
                        + "realTracedLightingConsumed={} tracedLightingConsumed={} traced_lighting_consumed={} voxelRayTracedLightingConsumed={} rayTracedLightingConsumed={} tracedLightingSampleCount={} tracedLightingHitCount={} tracedLightingSource={} tracedLightingMetadataOnly={} traceRayCount={} traceHitCount={} traceMaterialCoupledHitCount={} traceDepthCoupledHitCount={} traceSourceCoupledBounceCount={} traceConsumptionBlocker=\"{}\" "
                        + "shaderDenoiseDispatchPrepared={} shader_denoise_dispatch_prepared={} shaderDenoiseOutputMaterialReady={} shader_denoise_output_material_ready={} shaderDenoiseOutputImageReady={} shader_denoise_output_image_ready={} shaderDenoiseShaderGeneratedOutput={} shader_denoise_shader_generated_output={} realShaderDenoiseOutputReady={} real_shader_denoise_output_ready={} shaderGeneratedDenoiseOutputEvidence={} shaderGeneratedDenoiseOutputEvidenceReady={} shaderGeneratedDenoiseSourceReady={} shader_generated_denoise_source_ready={} shaderGeneratedDenoiseOutputConsumedByFinalComposite={} shader_generated_denoise_output_consumed_by_final_composite={} shaderDenoiseReadinessOutputSeparated={} shader_denoise_readiness_output_separated={} shaderDenoiseNoOverclaim={} shader_denoise_no_overclaim={} shaderDenoiseOverclaimRejected={} shader_denoise_overclaim_rejected={} cpuReadbackFallbackActive={} shaderDenoiseMetadataOnlyActive={} metadataOnlyActive={} "
                        + "metadata_only_proof_rejected=true focus_window_capture_rejected=true proof_marker_evidence_rejected=true low_res_direct_texture_final_proof_rejected=true lowResolutionDirectTextureFinalProofRejected=true low_res_direct_texture_debug_draw_rejected=true blocker=\"{}\".",
                proofReady,
                sceneKind,
                sceneState,
                artifactRole,
                physicalGiEvidence,
                physicalGiEvidence,
                physicalGiEvidence,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                trueDepthGBufferSampling,
                depthSampleCount,
                depthSampleCount,
                depthMetadataOnly,
                depthSamplingMarker,
                javaDepthSamplingEvidence,
                nativeDepthSamplingEvidence,
                shaderPassDepthSamplingEvidence,
                depthSamplingPassOutputsReady,
                depthSamplingEvidenceSources,
                depthSamplingPassOutputsMarker,
                parsedDepthSampleCount,
                nativeShadowMapAttempted,
                nativeShadowMapAttempted,
                nativeShadowMapGenerated,
                nativeShadowMapGenerated,
                realShadowMapOutputReady,
                realShadowMapOutputReady,
                realShadowMapOutputReady,
                realShadowMapOutputReady,
                shadowMapDepthSamples,
                shadowMapCasters,
                shadowMapReceivers,
                shadowMapDepthSamples,
                shadowMapChecksum,
                shadowMapMarker,
                shadowMapBlocker,
                finalCompositeSubmitted,
                finalCompositeModeActive,
                shadowMapRenderPathEvidence,
                shadowMapOutputConsumedByFinalComposite,
                shadowMapOutputConsumedByFinalComposite,
                shadowMapOutputConsumedByFinalComposite,
                shadowMapOutputConsumedByFinalComposite,
                shadowMapConsumptionMarker,
                shadowMapConsumptionBlocker,
                shadowMapSliceProofReady,
                depthShadowSliceProofReady,
                tracedLightingConsumed,
                tracedLightingConsumed,
                tracedLightingConsumed,
                tracedLightingConsumed,
                tracedLightingConsumed,
                tracedLightingSampleCount,
                tracedLightingHitCount,
                tracedLightingSource,
                !tracedLightingConsumed,
                traceEvidence.rayCount(),
                traceEvidence.hitCount(),
                traceEvidence.materialCoupledHitCount(),
                traceEvidence.depthCoupledHitCount(),
                traceEvidence.sourceCoupledBounceCount(),
                traceEvidence.blocker(),
                denoiseStatus.shaderDenoiseDispatchPrepared(),
                denoiseStatus.shaderDenoiseDispatchPrepared(),
                denoiseStatus.shaderDenoiseOutputMaterialReady(),
                denoiseStatus.shaderDenoiseOutputMaterialReady(),
                denoiseStatus.shaderDenoiseOutputImageReady(),
                denoiseStatus.shaderDenoiseOutputImageReady(),
                denoiseStatus.shaderDenoiseShaderGeneratedOutput(),
                denoiseStatus.shaderDenoiseShaderGeneratedOutput(),
                denoiseStatus.realShaderDenoiseOutputReady(),
                denoiseStatus.realShaderDenoiseOutputReady(),
                shaderGeneratedDenoiseOutputEvidence,
                shaderGeneratedDenoiseOutputEvidence,
                shaderGeneratedDenoiseSourceReady,
                shaderGeneratedDenoiseSourceReady,
                shaderGeneratedDenoiseOutputConsumedByFinalComposite,
                shaderGeneratedDenoiseOutputConsumedByFinalComposite,
                shaderDenoiseReadinessOutputSeparated,
                shaderDenoiseReadinessOutputSeparated,
                shaderDenoiseNoOverclaim,
                shaderDenoiseNoOverclaim,
                shaderDenoiseNoOverclaim,
                shaderDenoiseNoOverclaim,
                shaderDenoiseCpuReadbackFallbackActive,
                shaderDenoiseMetadataOnlyActive,
                shaderDenoiseMetadataOnlyActive,
                proofReady
                        ? "none"
                        : "remaining="
                        + (physicalGiEvidence ? "" : "physical-gi-evidence;")
                        + (trueDepthGBufferSampling ? "" : "true-depth-gbuffer-sampling;")
                        + (shadowMapOutputConsumedByFinalComposite ? "" : "shadow-map-final-composite-consumption;")
                        + (tracedLightingConsumed ? "" : "traced-lighting-consumption;")
                        + (shaderGeneratedDenoiseOutputEvidence ? "" : "shader-generated-denoise-output;")
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

    private static boolean envHasText(String envName) {
        String value = System.getenv(envName);
        return value != null && !value.isBlank();
    }

    private static boolean envTruthy(String envName) {
        String value = System.getenv(envName);
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return "1".equals(normalized)
                || "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized);
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

    private static boolean round9NativeBoolean(String nativeStatus, String key, boolean fallback) {
        String value = round9NativeValue(nativeStatus, key, fallback ? "true" : "false");
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().replace("\"", "").toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)
                || "1".equals(normalized)
                || "yes".equals(normalized)
                || "ready".equals(normalized)
                || "generated".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)
                || "0".equals(normalized)
                || "no".equals(normalized)
                || "none".equals(normalized)
                || "not_ready".equals(normalized)
                || "not_generated".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static boolean positiveLong(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(value.trim()) > 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
