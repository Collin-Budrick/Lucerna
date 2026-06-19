package net.lucerna.nativebridge;

import net.lucerna.Lucerna;
import net.lucerna.render.gbuffer.GBufferTargetContract;
import net.lucerna.render.pass.LucernaFramePassKind;
import net.lucerna.render.pass.LucernaFramePassRequest;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
import net.lucerna.upload.NativeDirectLightingUploadPacket;
import net.lucerna.upload.NativeGBufferStagingUploadPacket;
import net.lucerna.upload.NativeLightingDispatchUploadPacket;
import net.lucerna.upload.NativeSectionSnapshotUploadPacket;
import net.lucerna.upload.NativeStagedUploadBatch;
import net.lucerna.upload.NativeUploadBatch;
import net.lucerna.upload.NativeUploadPacket;

public final class LucernaNativeBridge {
    private static final String LIBRARY_NAME = "lucerna_renderer";
    private static final String LIBRARY_PATH_PROPERTY = "lucerna.native.path";
    private static final int FORMAT_TAG_GBUFFER_DEPTH = 10;
    private static final int FORMAT_TAG_GBUFFER_NORMAL_MATERIAL = 11;
    private static final int FORMAT_TAG_GBUFFER_ALBEDO_EMISSIVE = 12;
    private static final int FORMAT_TAG_GBUFFER_MOTION_HISTORY = 13;
    private static final int FORMAT_TAG_GBUFFER_MATERIAL_ID = 15;

    private boolean loadAttempted;
    private boolean loaded;
    private boolean available;
    private boolean initialized;
    private String lastError = "Native library has not been loaded.";
    private long lastLoggedSectionSnapshotGeneration;
    private long lastLoggedGBufferStagingGeneration;
    private long lastLoggedDirectLightingUploadGeneration;
    private long lastLoggedLightingDispatchGeneration;
    private String lastLoggedDirectLightingExecutionKey = "";
    private String lastLoggedDenoiseExecutionKey = "";
    private boolean directLightingUploadUnavailableLogged;
    private boolean diffuseGiPreviewRgba8ExportUnavailable;

    public synchronized boolean hasLoadAttempted() {
        return this.loadAttempted;
    }

    public synchronized boolean isLoaded() {
        return this.loaded;
    }

    public synchronized boolean isAvailable() {
        return this.available;
    }

    public synchronized boolean isInitialized() {
        return this.initialized;
    }

    public synchronized String lastError() {
        return this.lastError;
    }

    public synchronized NativeBridgeStatus status() {
        return new NativeBridgeStatus(
                this.loadAttempted,
                this.loaded,
                this.available,
                this.initialized,
                this.lastError,
                this.loaded ? this.queryNativeStatus() : "native library not loaded"
        );
    }

    public synchronized DirectLightingCpuOutputSnapshot directLightingCpuOutputSnapshot() {
        if (!this.loaded) {
            return DirectLightingCpuOutputSnapshot.unavailable("native library not loaded");
        }
        return DirectLightingCpuOutputSnapshot.fromNativeStatus(this.queryNativeStatus());
    }

    public synchronized DirectLightingCpuOutputPayload directLightingCpuOutputPayload() {
        if (!this.loaded) {
            return DirectLightingCpuOutputPayload.unavailable("native library not loaded");
        }

        DirectLightingCpuOutputSnapshot snapshot = this.directLightingCpuOutputSnapshot();
        if (!snapshot.hasCpuOutputTelemetry() || !snapshot.hasNonzeroEnergy()) {
            return new DirectLightingCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "direct-light CPU output telemetry is not ready for Java preview upload"
            );
        }

        byte[] rgba8 = nativeDirectLightingCpuOutputPreviewRgba8();
        if (rgba8 == null) {
            return new DirectLightingCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native direct-light CPU output preview payload returned null"
            );
        }
        long expectedBytes = Math.max(0L, snapshot.outputPixels()) * 4L;
        if (expectedBytes > Integer.MAX_VALUE || rgba8.length != (int) expectedBytes) {
            return new DirectLightingCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native direct-light CPU output preview RGBA8 payload size did not match telemetry"
            );
        }
        return new DirectLightingCpuOutputPayload(
                snapshot,
                rgba8,
                "native direct-light CPU output preview RGBA8 payload copied"
        );
    }

    public synchronized Round6DiffuseGiCpuOutputSnapshot round6DiffuseGiCpuOutputSnapshot() {
        if (!this.loaded) {
            return Round6DiffuseGiCpuOutputSnapshot.unavailable("native library not loaded");
        }
        return Round6DiffuseGiCpuOutputSnapshot.fromNativeStatus(this.queryNativeStatus());
    }

    public synchronized DenoiseExecutionSnapshot denoiseExecutionSnapshot() {
        if (!this.loaded) {
            return DenoiseExecutionSnapshot.unavailable("native library not loaded");
        }
        return DenoiseExecutionSnapshot.fromNativeStatus(this.queryNativeStatus());
    }

    public synchronized Round6DiffuseGiCpuOutputPayload round6DiffuseGiCpuOutputPayload(
            Round6DiffuseGiPreviewCompositeState previewState
    ) {
        if (!this.loaded) {
            return Round6DiffuseGiCpuOutputPayload.unavailable("native library not loaded");
        }

        Round6DiffuseGiCpuOutputSnapshot snapshot = this.round6DiffuseGiCpuOutputSnapshot();
        if (previewState != null && !previewState.readyForRound6PreviewSource()) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "Round 6 diffuse GI/cache metadata is not ready for native diffuse GI output preview payload: "
                            + previewState.summary()
            );
        }
        if (!snapshot.readyForPreviewPayload()) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "Round 6 diffuse GI CPU output telemetry is not ready for Java preview upload"
            );
        }

        if (this.diffuseGiPreviewRgba8ExportUnavailable) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native Round 6 diffuse GI preview RGBA8 JNI export is not available yet"
            );
        }

        byte[] rgba8;
        try {
            rgba8 = nativeDiffuseGiCpuOutputPreviewRgba8();
        } catch (UnsatisfiedLinkError error) {
            this.diffuseGiPreviewRgba8ExportUnavailable = true;
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native Round 6 diffuse GI preview RGBA8 JNI export is not available yet"
            );
        } catch (Throwable throwable) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native Round 6 diffuse GI preview RGBA8 call failed: " + throwable.getMessage()
            );
        }
        if (rgba8 == null) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native Round 6 diffuse GI preview payload returned null"
            );
        }
        long expectedBytes = Math.max(0L, snapshot.outputPixels()) * 4L;
        if (expectedBytes > Integer.MAX_VALUE || rgba8.length != (int) expectedBytes) {
            return new Round6DiffuseGiCpuOutputPayload(
                    snapshot,
                    new byte[0],
                    "native Round 6 diffuse GI preview RGBA8 payload size did not match telemetry"
            );
        }
        return new Round6DiffuseGiCpuOutputPayload(
                snapshot,
                rgba8,
                "native Round 6 diffuse GI preview RGBA8 payload copied"
        );
    }

    public synchronized DirectLightingPreviewCompositeSubmissionResult submitDirectLightingPreviewComposite(
            LucernaFramePassRequest request
    ) {
        return this.submitDirectLightingPreviewComposite(this.directLightingCpuOutputSnapshot(), request);
    }

    public synchronized DirectLightingPreviewCompositeSubmissionResult submitDirectLightingPreviewComposite(
            DirectLightingCpuOutputSnapshot snapshot,
            LucernaFramePassRequest request
    ) {
        DirectLightingCpuOutputSnapshot directOutput = snapshot == null
                ? DirectLightingCpuOutputSnapshot.unavailable("direct-light CPU output snapshot was not supplied")
                : snapshot;
        LucernaFramePassRequest previewRequest = request == null
                ? LucernaFramePassRequest.noOp(0L)
                : request;
        boolean nativeOperational = this.isOperational();
        boolean snapshotReady = directOutput.ready()
                && directOutput.hasCpuOutputTelemetry()
                && directOutput.hasNonzeroEnergy();
        boolean directPreviewRequest = previewRequest.kind() == LucernaFramePassKind.DIRECT_LIGHT_PREVIEW_COMPOSITE;
        var target = previewRequest.target();
        boolean targetPresent = directPreviewRequest && target != null && target.available();
        boolean targetMissing = directPreviewRequest && !targetPresent;
        boolean targetReady = directPreviewRequest && previewRequest.targetSafeForAttachment();
        boolean targetHudPreserving = directPreviewRequest
                && target != null
                && target.preservesHud();
        boolean targetMetadataOnly = directPreviewRequest
                && target != null
                && target.metadataOnlyAttachment();
        boolean targetJavaOpaqueRenderObjectsPresent = directPreviewRequest
                && target != null
                && targetJavaOpaqueRenderObjectsPresent(target);
        boolean targetNativeWritable = directPreviewRequest
                && target != null
                && target.nativeWritableAttachment();
        boolean targetNativeWritableHandlesPresent = targetNativeWritable;
        boolean nativeJniSubmissionWired = false;
        float strength = previewRequest.red();
        float alpha = previewRequest.alpha();

        if (!nativeOperational) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    false,
                    snapshotReady,
                    targetReady,
                    targetMissing,
                    targetHudPreserving,
                    targetMetadataOnly,
                    targetJavaOpaqueRenderObjectsPresent,
                    targetNativeWritable,
                    targetNativeWritableHandlesPresent,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    "native bridge is not initialized for direct-light preview composite submission"
            );
        }
        if (!directPreviewRequest) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    true,
                    snapshotReady,
                    false,
                    false,
                    targetHudPreserving,
                    false,
                    false,
                    targetNativeWritable,
                    targetNativeWritableHandlesPresent,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    "frame pass request is not a direct-light preview composite request"
            );
        }
        if (!snapshotReady) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    true,
                    false,
                    targetReady,
                    targetMissing,
                    targetHudPreserving,
                    targetMetadataOnly,
                    targetJavaOpaqueRenderObjectsPresent,
                    targetNativeWritable,
                    targetNativeWritableHandlesPresent,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    "direct-light CPU output snapshot is not ready for preview composite submission: "
                            + directOutput.debugSummary()
            );
        }
        if (targetMissing) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    true,
                    true,
                    false,
                    true,
                    targetHudPreserving,
                    false,
                    false,
                    false,
                    false,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    "direct-light preview composite target is missing"
            );
        }
        if (!targetReady) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    true,
                    true,
                    false,
                    false,
                    targetHudPreserving,
                    targetMetadataOnly,
                    targetJavaOpaqueRenderObjectsPresent,
                    targetNativeWritable,
                    targetNativeWritableHandlesPresent,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    "frame target is not ready or safe for HUD-preserving direct-light preview composite submission"
            );
        }
        if (!targetNativeWritable) {
            return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                    previewRequest.frameIndex(),
                    true,
                    true,
                    true,
                    false,
                    targetHudPreserving,
                    targetMetadataOnly,
                    targetJavaOpaqueRenderObjectsPresent,
                    false,
                    false,
                    nativeJniSubmissionWired,
                    strength,
                    alpha,
                    directLightingPreviewTargetNotWritableReason(
                            targetMetadataOnly,
                            targetJavaOpaqueRenderObjectsPresent
                    )
            );
        }

        return DirectLightingPreviewCompositeSubmissionResult.notSubmitted(
                previewRequest.frameIndex(),
                true,
                true,
                true,
                false,
                targetHudPreserving,
                targetMetadataOnly,
                targetJavaOpaqueRenderObjectsPresent,
                true,
                true,
                nativeJniSubmissionWired,
                strength,
                alpha,
                "native-writable direct-light preview target handles are present, but JNI submission is not wired yet"
        );
    }

    public synchronized void load() {
        if (this.loadAttempted) {
            return;
        }

        this.loadAttempted = true;

        String explicitLibraryPath = System.getProperty(LIBRARY_PATH_PROPERTY, "").trim();
        try {
            if (explicitLibraryPath.isEmpty()) {
                System.loadLibrary(LIBRARY_NAME);
                Lucerna.LOGGER.info("Loaded native library {} from java.library.path.", LIBRARY_NAME);
            } else {
                System.load(explicitLibraryPath);
                Lucerna.LOGGER.info("Loaded native library {} from {}.", LIBRARY_NAME, explicitLibraryPath);
            }
            this.loaded = true;
            this.available = true;
            this.initialized = false;
            this.diffuseGiPreviewRgba8ExportUnavailable = false;
            this.lastError = "";
        } catch (UnsatisfiedLinkError error) {
            this.loaded = false;
            this.available = false;
            this.initialized = false;
            String loadTarget = explicitLibraryPath.isEmpty() ? "java.library.path" : explicitLibraryPath;
            this.lastError = "Could not load native library " + LIBRARY_NAME + " from " + loadTarget + ": " + error.getMessage();
            Lucerna.LOGGER.warn("Lucerna native library is not available yet. Rendering will stay disabled.", error);
        }
    }

    public synchronized boolean init() {
        if (!this.available) {
            return false;
        }

        if (this.initialized) {
            return true;
        }

        if (!this.invokeNative("initialization", LucernaNativeBridge::nativeInit, false)) {
            return false;
        }

        this.lastLoggedSectionSnapshotGeneration = 0L;
        this.lastLoggedGBufferStagingGeneration = 0L;
        this.lastLoggedDirectLightingUploadGeneration = 0L;
        this.lastLoggedLightingDispatchGeneration = 0L;
        this.lastLoggedDirectLightingExecutionKey = "";
        this.lastLoggedDenoiseExecutionKey = "";
        this.directLightingUploadUnavailableLogged = false;
        this.diffuseGiPreviewRgba8ExportUnavailable = false;
        this.initialized = true;
        return true;
    }

    public synchronized void shutdown() {
        if (!this.loaded || !this.initialized) {
            this.initialized = false;
            return;
        }

        if (this.invokeNative("shutdown", LucernaNativeBridge::nativeShutdown, false)) {
            this.available = this.loaded;
            this.lastError = "";
        }
        this.lastLoggedSectionSnapshotGeneration = 0L;
        this.lastLoggedGBufferStagingGeneration = 0L;
        this.lastLoggedDirectLightingUploadGeneration = 0L;
        this.lastLoggedLightingDispatchGeneration = 0L;
        this.lastLoggedDirectLightingExecutionKey = "";
        this.lastLoggedDenoiseExecutionKey = "";
        this.directLightingUploadUnavailableLogged = false;
        this.initialized = false;
    }

    public synchronized void onResize(int width, int height) {
        if (this.isOperational()) {
            this.invokeNative("resize", () -> nativeOnResize(width, height), true);
        }
    }

    public synchronized void beginFrame(long frameIndex, float tickDelta) {
        if (this.isOperational()) {
            this.invokeNative("beginFrame", () -> nativeBeginFrame(frameIndex, tickDelta), true);
        }
    }

    public synchronized void uploadWorldDeltas(NativeUploadBatch batch) {
        if (this.isOperational() && batch != null && !batch.isEmpty()) {
            NativeUploadPacket packet = batch.toPacket();
            this.invokeNative("uploadWorldDeltas", () -> nativeUploadWorldDeltas(
                    packet.generation(),
                    packet.dirtyRegionCount(),
                    packet.materialUpdateCount(),
                    packet.firstWorldGeneration(),
                    packet.lastWorldGeneration(),
                    packet.materialGeneration(),
                    packet.dirtyRegionTypeIds(),
                    packet.dirtyRegionDimensions(),
                    packet.dirtyRegionSectionXs(),
                    packet.dirtyRegionSectionYs(),
                    packet.dirtyRegionSectionZs(),
                    packet.dirtyRegionSectionScoped(),
                    packet.dirtyRegionGenerations(),
                    packet.materialIds(),
                    packet.materialGenerations(),
                    packet.materialBlockIds(),
                    packet.materialFaceIds(),
                    packet.materialAlbedoTextureIndices(),
                    packet.materialProperties(),
                    packet.materialFlags()
            ), true);
        }
    }

    public synchronized void uploadSectionSnapshots(NativeStagedUploadBatch batch) {
        if (this.isOperational() && batch != null && batch.hasStagingPayloads()) {
            NativeSectionSnapshotUploadPacket packet = batch.toSectionSnapshotPacket();
            if (!packet.hasPayloads()) {
                return;
            }

            boolean accepted = this.invokeNative("uploadSectionSnapshots", () -> nativeUploadSectionSnapshots(
                    packet.generation(),
                    packet.sectionSnapshotCount(),
                    packet.firstSectionSnapshotGeneration(),
                    packet.lastSectionSnapshotGeneration(),
                    packet.sectionGeneration(),
                    packet.sectionMaterialGeneration(),
                    packet.sectionOccupancyGeneration(),
                    packet.sectionEmissiveGeneration(),
                    packet.sectionDirtyRegionGeneration(),
                    packet.dirtyRegionTypeIds(),
                    packet.dirtyRegionTypeNames(),
                    packet.dirtyRegionDimensions(),
                    packet.dirtyRegionSectionXs(),
                    packet.dirtyRegionSectionYs(),
                    packet.dirtyRegionSectionZs(),
                    packet.dirtyRegionSectionScoped(),
                    packet.dirtyRegionGenerations(),
                    packet.sectionDimensions(),
                    packet.sectionXs(),
                    packet.sectionYs(),
                    packet.sectionZs(),
                    packet.sectionGenerations(),
                    packet.sectionMaterialGenerations(),
                    packet.sectionOccupancyGenerations(),
                    packet.sectionEmissiveGenerations(),
                    packet.voxelCounts(),
                    packet.occupancyBitOrderIds(),
                    packet.occupancyBitOrderNames(),
                    packet.occupancyMaskWordOffsets(),
                    packet.occupancyMaskWordCounts(),
                    packet.occupancyMaskBitCounts(),
                    packet.occupancyMaskGenerations(),
                    packet.materialPaletteOffsets(),
                    packet.materialPaletteGenerations(),
                    packet.materialPalettePayloadCounts(),
                    packet.materialPaletteIds(),
                    packet.emissivePayloadCounts(),
                    packet.emissiveVoxelIndices(),
                    packet.emissiveMaterialIds(),
                    packet.emissiveBlockLightLevels(),
                    packet.emissiveEntryGenerations()
            ), true);
            if (accepted && packet.lastSectionSnapshotGeneration() != this.lastLoggedSectionSnapshotGeneration) {
                this.lastLoggedSectionSnapshotGeneration = packet.lastSectionSnapshotGeneration();
                Lucerna.LOGGER.info(
                        "Lucerna native section upload accepted: sections={} sectionGeneration={} dirtyRegionGeneration={} palettePayloads={} emissivePayloads={}.",
                        packet.sectionSnapshotCount(),
                        packet.sectionGeneration(),
                        packet.sectionDirtyRegionGeneration(),
                        packet.materialPalettePayloadCount(),
                        packet.emissivePayloadCount()
                );
            }
        }
    }

    public synchronized void uploadGBufferStaging(NativeStagedUploadBatch batch) {
        if (this.isOperational() && batch != null && batch.hasStagingPayloads()) {
            NativeGBufferStagingUploadPacket packet = batch.toGBufferStagingPacket();
            if (!packet.hasPayloads()) {
                return;
            }

            boolean accepted = this.invokeNative("uploadGBufferStaging", () -> nativeUploadGBufferStaging(
                    packet.generation(),
                    packet.gBufferStagingCount(),
                    packet.firstGBufferStagingGeneration(),
                    packet.lastGBufferStagingGeneration(),
                    packet.passIds(),
                    packet.numericPassIds(),
                    packet.widths(),
                    packet.heights(),
                    packet.attachmentPayloadCounts(),
                    packet.attachmentNames(),
                    nativeAttachmentFormatTags(packet),
                    nativeAttachmentWidths(packet),
                    nativeAttachmentHeights(packet),
                    packet.attachmentSamples(),
                    packet.attachmentEnabled()
            ), true);
            if (accepted && packet.lastGBufferStagingGeneration() != this.lastLoggedGBufferStagingGeneration) {
                this.lastLoggedGBufferStagingGeneration = packet.lastGBufferStagingGeneration();
                Lucerna.LOGGER.info(
                        "Lucerna native G-buffer staging accepted: targets={} generation={} attachments={} lastSize={}x{}.",
                        packet.gBufferStagingCount(),
                        packet.lastGBufferStagingGeneration(),
                        packet.attachmentPayloadCount(),
                        lastValue(packet.widths()),
                        lastValue(packet.heights())
                );
            }
        }
    }

    public synchronized void uploadDirectLighting(NativeDirectLightingUploadPacket packet) {
        if (!this.isOperational() || packet == null || !packet.hasPayloads()) {
            return;
        }

        try {
            boolean accepted = nativeUploadDirectLighting(
                    packet.frameIndex(),
                    packet.generation(),
                    packet.firstGeneration(),
                    packet.lastGeneration(),
                    packet.celestialGeneration(),
                    packet.emissiveGeneration(),
                    packet.shadowGeneration(),
                    packet.shadowCandidateGeneration(),
                    packet.sectionSnapshotGeneration(),
                    packet.dimensionId(),
                    packet.flags(),
                    packet.celestialLightCount(),
                    packet.celestialLightEnergy(),
                    packet.selectedEmissiveCount(),
                    packet.selectedEmissiveEnergy(),
                    packet.shadowCandidateCount(),
                    packet.budgetedShadowCandidateCount(),
                    packet.sectionSnapshotCount(),
                    packet.rayBudget(),
                    packet.celestialLightSources(),
                    packet.celestialLightFlags(),
                    packet.celestialLightData(),
                    packet.emissiveLightDimensions(),
                    packet.emissiveLightMetadata(),
                    packet.emissiveLightData(),
                    packet.emissiveLightGenerations(),
                    packet.shadowCandidateMetadata(),
                    packet.shadowCandidateRays(),
                    packet.shadowCandidateGenerations(),
                    packet.sectionSnapshotDimensions(),
                    packet.sectionSnapshotMetadata(),
                    packet.sectionSnapshotGenerations()
            );
            if (!accepted) {
                this.disableFromNativeFailure("uploadDirectLighting", true);
                return;
            }

            this.lastError = "";
            if (packet.generation() != this.lastLoggedDirectLightingUploadGeneration) {
                this.lastLoggedDirectLightingUploadGeneration = packet.generation();
                Lucerna.LOGGER.info(
                        "Lucerna native direct lighting payload accepted: generation={} frame={} celestial={} emissive={} shadowCandidates={} budgetedShadowCandidates={} sections={} flags=0x{}.",
                        packet.generation(),
                        packet.frameIndex(),
                        packet.celestialLightCount(),
                        packet.selectedEmissiveCount(),
                        packet.shadowCandidateCount(),
                        packet.budgetedShadowCandidateCount(),
                        packet.sectionSnapshotCount(),
                        Integer.toHexString(packet.flags())
                );
            }
        } catch (UnsatisfiedLinkError error) {
            if (!this.directLightingUploadUnavailableLogged) {
                this.directLightingUploadUnavailableLogged = true;
                Lucerna.LOGGER.warn(
                        "Lucerna native direct lighting payload upload is not implemented by the loaded native library yet; continuing with dispatch metadata only."
                );
            }
        } catch (Throwable throwable) {
            this.disableFromThrowable("uploadDirectLighting", true, throwable);
        }
    }

    public synchronized void uploadLightingDispatch(NativeLightingDispatchUploadPacket packet) {
        if (this.isOperational() && packet != null) {
            boolean accepted = this.invokeNative("uploadLightingDispatch", () -> nativeUploadLightingDispatch(
                    packet.generation(),
                    packet.dispatchCount(),
                    packet.firstDispatchGeneration(),
                    packet.lastDispatchGeneration(),
                    packet.worldGeneration(),
                    packet.materialGeneration(),
                    packet.sectionGeneration(),
                    packet.gBufferGeneration(),
                    packet.stageIds(),
                    packet.stageNames(),
                    packet.stageEnabled(),
                    packet.stageGenerations(),
                    packet.stageDimensions(),
                    packet.stageDispatchGroups(),
                    packet.stageWorkgroupSizes(),
                    packet.stageIoCounts(),
                    packet.stageSampleRayCounts(),
                    packet.stageCacheCounts(),
                    packet.stageEstimatedBytes(),
                    packet.stageFlags()
            ), true);
            if (accepted && packet.lastDispatchGeneration() != this.lastLoggedLightingDispatchGeneration) {
                this.lastLoggedLightingDispatchGeneration = packet.lastDispatchGeneration();
                Lucerna.LOGGER.info(
                        "Lucerna native lighting dispatch accepted: generation={} stages={} enabled={} gBufferGeneration={} sectionGeneration={} directEnabled={} directInputs={} directOutputs={} directCandidates={} directSamples={} directRays={} directFlags=0x{}.",
                        packet.generation(),
                        packet.dispatchCount(),
                        packet.enabledStageCount(),
                        packet.gBufferGeneration(),
                        packet.sectionGeneration(),
                        packet.directLightingStageEnabled(),
                        packet.directLightingInputCount(),
                        packet.directLightingOutputCount(),
                        packet.directLightingCandidateCount(),
                        packet.directLightingSampleCount(),
                        packet.directLightingRayCount(),
                        Integer.toHexString(packet.directLightingFlags())
                );
            }
        }
    }

    public synchronized void renderLighting() {
        if (this.isOperational()) {
            boolean accepted = this.invokeNative("renderLighting", LucernaNativeBridge::nativeRenderLighting, true);
            if (accepted) {
                this.logDirectLightingExecutionStatus();
                this.logDenoiseExecutionStatus();
            }
        }
    }

    public synchronized void endFrame() {
        if (this.isOperational()) {
            this.invokeNative("endFrame", LucernaNativeBridge::nativeEndFrame, true);
        }
    }

    public synchronized boolean adoptBorrowedVulkanContext(BorrowedVulkanContext context) {
        if (!this.isOperational()) {
            return false;
        }

        if (context == null || !context.hasRequiredHandles()) {
            this.lastError = "Borrowed Vulkan context is incomplete.";
            return false;
        }

        return this.invokeNative("adoptBorrowedVulkanContext", () -> nativeAdoptBorrowedVulkanContext(
                context.instance(),
                context.physicalDevice(),
                context.device(),
                context.graphicsQueue(),
                context.graphicsQueueFamily()
        ), true);
    }

    public synchronized void releaseBorrowedVulkanContext() {
        if (this.isOperational()) {
            this.invokeNative("releaseBorrowedVulkanContext", LucernaNativeBridge::nativeReleaseBorrowedVulkanContext, true);
        }
    }

    private boolean isOperational() {
        return this.loaded && this.available && this.initialized;
    }

    private static String directLightingPreviewTargetNotWritableReason(
            boolean targetMetadataOnly,
            boolean targetJavaOpaqueRenderObjectsPresent
    ) {
        if (targetJavaOpaqueRenderObjectsPresent) {
            return "frame target exposes Java-opaque render objects, but native-writable command/color handles are not available";
        }
        if (targetMetadataOnly) {
            return "frame target is HUD-safe but metadata-only; native-writable command/color handles are not available";
        }
        return "frame target is HUD-safe, but native-writable command/color handles are not present";
    }

    private static boolean targetJavaOpaqueRenderObjectsPresent(LucernaFramePassTarget target) {
        if (target == null || target.attachmentMetadata() == null) {
            return false;
        }
        return target.attachmentMetadata().javaOpaque();
    }

    private static int[] nativeAttachmentFormatTags(NativeGBufferStagingUploadPacket packet) {
        String[] attachmentNames = packet.attachmentNames();
        String[] attachmentFormats = packet.attachmentFormats();
        int[] formatTags = new int[attachmentNames.length];
        for (int index = 0; index < attachmentNames.length; index++) {
            formatTags[index] = nativeAttachmentFormatTag(attachmentNames[index], attachmentFormats[index]);
        }
        return formatTags;
    }

    private static int nativeAttachmentFormatTag(String attachmentName, String attachmentFormat) {
        return switch (attachmentName) {
            case GBufferTargetContract.DEPTH -> FORMAT_TAG_GBUFFER_DEPTH;
            case GBufferTargetContract.NORMAL_ROUGHNESS -> FORMAT_TAG_GBUFFER_NORMAL_MATERIAL;
            case GBufferTargetContract.ALBEDO_OPACITY, GBufferTargetContract.EMISSIVE -> FORMAT_TAG_GBUFFER_ALBEDO_EMISSIVE;
            case GBufferTargetContract.MATERIAL_ID -> FORMAT_TAG_GBUFFER_MATERIAL_ID;
            case GBufferTargetContract.MOTION_HISTORY -> FORMAT_TAG_GBUFFER_MOTION_HISTORY;
            default -> fallbackFormatTag(attachmentFormat);
        };
    }

    private static int fallbackFormatTag(String attachmentFormat) {
        if (attachmentFormat == null) {
            return 0;
        }
        return switch (attachmentFormat) {
            case "D32_SFLOAT" -> FORMAT_TAG_GBUFFER_DEPTH;
            case "R16G16B16A16_SFLOAT" -> FORMAT_TAG_GBUFFER_ALBEDO_EMISSIVE;
            case "R32_UINT" -> FORMAT_TAG_GBUFFER_MATERIAL_ID;
            default -> 1;
        };
    }

    private static int[] nativeAttachmentWidths(NativeGBufferStagingUploadPacket packet) {
        return nativeAttachmentDimensions(packet, true);
    }

    private static int[] nativeAttachmentHeights(NativeGBufferStagingUploadPacket packet) {
        return nativeAttachmentDimensions(packet, false);
    }

    private static int[] nativeAttachmentDimensions(NativeGBufferStagingUploadPacket packet, boolean width) {
        int[] dimensions = new int[packet.attachmentPayloadCount()];
        int[] offsets = packet.attachmentPayloadOffsets();
        int[] counts = packet.attachmentPayloadCounts();
        int[] targetWidths = packet.widths();
        int[] targetHeights = packet.heights();
        String[] resolutions = packet.attachmentResolutions();

        for (int targetIndex = 0; targetIndex < offsets.length; targetIndex++) {
            int baseDimension = width ? targetWidths[targetIndex] : targetHeights[targetIndex];
            int offset = offsets[targetIndex];
            int count = counts[targetIndex];
            for (int attachmentIndex = offset; attachmentIndex < offset + count; attachmentIndex++) {
                dimensions[attachmentIndex] = scaledDimension(baseDimension, resolutions[attachmentIndex]);
            }
        }
        return dimensions;
    }

    private static int scaledDimension(int baseDimension, String resolution) {
        if (baseDimension <= 0) {
            return 0;
        }
        if ("half".equals(resolution)) {
            return Math.max(1, (baseDimension + 1) / 2);
        }
        return baseDimension;
    }

    private static int lastValue(int[] values) {
        return values.length == 0 ? 0 : values[values.length - 1];
    }

    private boolean invokeNative(String operation, NativeCall call, boolean preserveInitializedAfterFailure) {
        try {
            if (call.invoke()) {
                this.lastError = "";
                return true;
            }
            return this.disableFromNativeFailure(operation, preserveInitializedAfterFailure);
        } catch (Throwable throwable) {
            return this.disableFromThrowable(operation, preserveInitializedAfterFailure, throwable);
        }
    }

    private boolean disableFromNativeFailure(String operation, boolean preserveInitializedAfterFailure) {
        String nativeError = this.queryNativeLastError();
        this.lastError = "Native " + operation + " failed" + (nativeError.isBlank() ? "." : ": " + nativeError);
        this.available = false;
        if (!preserveInitializedAfterFailure) {
            this.initialized = false;
        }
        Lucerna.LOGGER.error("Lucerna native {} failed; disabling native renderer. {}", operation, nativeError);
        return false;
    }

    private boolean disableFromThrowable(String operation, boolean preserveInitializedAfterFailure, Throwable throwable) {
        this.lastError = "Native " + operation + " threw " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        this.available = false;
        if (!preserveInitializedAfterFailure) {
            this.initialized = false;
        }
        Lucerna.LOGGER.error("Lucerna native {} threw; disabling native renderer.", operation, throwable);
        return false;
    }

    private String queryNativeStatus() {
        try {
            return nativeStatus();
        } catch (Throwable throwable) {
            return "native status unavailable: " + throwable.getMessage();
        }
    }

    private String queryNativeLastError() {
        try {
            String nativeError = nativeLastError();
            return nativeError == null ? "" : nativeError;
        } catch (Throwable throwable) {
            return "native error unavailable: " + throwable.getMessage();
        }
    }

    private void logDirectLightingExecutionStatus() {
        DirectLightingCpuOutputSnapshot directOutput = this.directLightingCpuOutputSnapshot();
        if (!directOutput.hasExecutionTelemetry()) {
            return;
        }

        String key = directOutput.dispatchGeneration()
                + "|" + directOutput.candidateCount()
                + "|" + directOutput.outputWriteRecorded()
                + "|" + directOutput.resolveRecorded()
                + "|" + directOutput.ready()
                + "|" + directOutput.cpuOutputGenerated()
                + "|" + directOutput.outputChecksum();
        if (key.equals(this.lastLoggedDirectLightingExecutionKey)) {
            return;
        }

        this.lastLoggedDirectLightingExecutionKey = key;
        Lucerna.LOGGER.info(
                "Lucerna native direct lighting execution: dispatchGeneration={} candidates={} samples={} rays={} outputs={} outputWrites={} resolves={} outputWriteRecorded={} resolveRecorded={} ready={} cpuOutput={} cpuOutputSize={}x{} cpuOutputPixels={} cpuOutputEnergy={} cpuOutputChecksum={} reason={}.",
                directOutput.dispatchGeneration(),
                directOutput.candidateCount(),
                directOutput.sampleCount(),
                directOutput.rayCount(),
                directOutput.outputCount(),
                directOutput.outputWrites(),
                directOutput.resolves(),
                directOutput.outputWriteRecorded(),
                directOutput.resolveRecorded(),
                directOutput.ready(),
                directOutput.cpuOutputGenerated(),
                directOutput.outputWidth(),
                directOutput.outputHeight(),
                directOutput.outputPixels(),
                directOutput.outputEnergy(),
                directOutput.outputChecksum(),
                directOutput.readinessReason()
        );
    }

    private void logDenoiseExecutionStatus() {
        DenoiseExecutionSnapshot denoise = this.denoiseExecutionSnapshot();
        if (!denoise.hasExecutionTelemetry()) {
            return;
        }

        String key = denoise.dispatchGeneration()
                + "|" + denoise.inputCount()
                + "|" + denoise.outputCount()
                + "|" + denoise.historyAcceptedCount()
                + "|" + denoise.historyRejectedCount()
                + "|" + denoise.ready()
                + "|" + denoise.accepted()
                + "|" + denoise.outputMarker()
                + "|" + denoise.rawInputMarker()
                + "|" + denoise.denoisedOutputMarker()
                + "|" + denoise.compositeMarker();
        if (key.equals(this.lastLoggedDenoiseExecutionKey)) {
            return;
        }

        this.lastLoggedDenoiseExecutionKey = key;
        Lucerna.LOGGER.info(
                "Lucerna native signal-separated denoise execution scaffold: dispatchGeneration={} size={}x{} inputs={} outputs={} samples={} enabled={} ready={} accepted={} diffuseGiSignal={} directShadowSignal={} edgeInputs={} temporalHistory={} historyAccepted={} historyRejected={} edgePreserved={} edgeRejected={} rawGi={} rawGiPixels={} rawGiSamples={} rawGiRays={} rawGiCacheReads={} rawDirect={} denoisedIntent={} realDenoiseShaderOutput={} composite={} compositeSize={}x{} compositeOutputs={} specularPlaceholder={} aoPlaceholder={} marker={} rawInputMarker={} denoisedOutputMarker={} compositeMarker={} reason={}.",
                denoise.dispatchGeneration(),
                denoise.width(),
                denoise.height(),
                denoise.inputCount(),
                denoise.outputCount(),
                denoise.sampleCount(),
                denoise.enabled(),
                denoise.ready(),
                denoise.accepted(),
                denoise.diffuseGiSignalAvailable(),
                denoise.directShadowSignalAvailable(),
                denoise.edgeInputsAvailable(),
                denoise.temporalHistory(),
                denoise.historyAcceptedCount(),
                denoise.historyRejectedCount(),
                denoise.edgePreservedCount(),
                denoise.edgeRejectedCount(),
                denoise.rawGiInputAvailable(),
                denoise.rawGiPixels(),
                denoise.rawGiSamples(),
                denoise.rawGiRays(),
                denoise.rawGiCacheReads(),
                denoise.rawDirectInputAvailable(),
                denoise.denoisedOutputIntent(),
                denoise.realDenoiseShaderOutput(),
                denoise.compositeSignalLabel(),
                denoise.compositeWidth(),
                denoise.compositeHeight(),
                denoise.compositeOutputCount(),
                denoise.optionalSpecularPlaceholder(),
                denoise.optionalAoPlaceholder(),
                denoise.outputMarker(),
                denoise.rawInputMarker(),
                denoise.denoisedOutputMarker(),
                denoise.compositeMarker(),
                denoise.readinessReason()
        );
    }

    @FunctionalInterface
    private interface NativeCall {
        boolean invoke();
    }

    public record BorrowedVulkanContext(
            long instance,
            long physicalDevice,
            long device,
            long graphicsQueue,
            int graphicsQueueFamily
    ) {
        public boolean hasRequiredHandles() {
            return this.instance != 0L
                    && this.physicalDevice != 0L
                    && this.device != 0L
                    && this.graphicsQueue != 0L
                    && this.graphicsQueueFamily >= 0;
        }
    }

    public record NativeBridgeStatus(
            boolean loadAttempted,
            boolean loaded,
            boolean available,
            boolean initialized,
            String lastError,
            String nativeStatus
    ) {
    }

    private static native boolean nativeInit();

    private static native boolean nativeShutdown();

    private static native boolean nativeOnResize(int width, int height);

    private static native boolean nativeBeginFrame(long frameIndex, float tickDelta);

    private static native boolean nativeUploadWorldDeltas(
            long generation,
            int dirtyRegionCount,
            int materialUpdateCount,
            long firstWorldGeneration,
            long lastWorldGeneration,
            long materialGeneration,
            int[] dirtyRegionTypeIds,
            String[] dirtyRegionDimensions,
            int[] dirtyRegionSectionXs,
            int[] dirtyRegionSectionYs,
            int[] dirtyRegionSectionZs,
            int[] dirtyRegionSectionScoped,
            long[] dirtyRegionGenerations,
            int[] materialIds,
            long[] materialGenerations,
            String[] materialBlockIds,
            int[] materialFaceIds,
            int[] materialAlbedoTextureIndices,
            float[] materialProperties,
            int[] materialFlags
    );

    private static native boolean nativeUploadSectionSnapshots(
            long generation,
            int sectionSnapshotCount,
            long firstSectionSnapshotGeneration,
            long lastSectionSnapshotGeneration,
            long sectionGeneration,
            long sectionMaterialGeneration,
            long sectionOccupancyGeneration,
            long sectionEmissiveGeneration,
            long sectionDirtyRegionGeneration,
            int[] dirtyRegionTypeIds,
            String[] dirtyRegionTypeNames,
            String[] dirtyRegionDimensions,
            int[] dirtyRegionSectionXs,
            int[] dirtyRegionSectionYs,
            int[] dirtyRegionSectionZs,
            int[] dirtyRegionSectionScoped,
            long[] dirtyRegionGenerations,
            String[] sectionDimensions,
            int[] sectionXs,
            int[] sectionYs,
            int[] sectionZs,
            long[] sectionGenerations,
            long[] materialGenerations,
            long[] occupancyGenerations,
            long[] sectionEmissiveGenerations,
            int[] voxelCounts,
            int[] occupancyBitOrderIds,
            String[] occupancyBitOrderNames,
            int[] occupancyMaskWordOffsets,
            int[] occupancyMaskWordCounts,
            int[] occupancyMaskBitCounts,
            long[] occupancyMaskGenerations,
            int[] materialPaletteOffsets,
            long[] materialPaletteGenerations,
            int[] materialPaletteCounts,
            int[] materialPaletteIds,
            int[] emissiveEntryCounts,
            int[] emissiveVoxelIndices,
            int[] emissiveMaterialIds,
            int[] emissiveBlockLightLevels,
            long[] emissiveEntryGenerations
    );

    private static native boolean nativeUploadGBufferStaging(
            long generation,
            int gBufferStagingCount,
            long firstGBufferStagingGeneration,
            long lastGBufferStagingGeneration,
            String[] passIds,
            int[] numericPassIds,
            int[] widths,
            int[] heights,
            int[] attachmentCounts,
            String[] attachmentNames,
            int[] attachmentFormats,
            int[] attachmentWidths,
            int[] attachmentHeights,
            int[] attachmentSamples,
            int[] attachmentEnabled
    );

    private static native boolean nativeUploadDirectLighting(
            long frameIndex,
            long generation,
            long firstGeneration,
            long lastGeneration,
            long celestialGeneration,
            long emissiveGeneration,
            long shadowGeneration,
            long shadowCandidateGeneration,
            long sectionSnapshotGeneration,
            String dimensionId,
            int flags,
            int celestialLightCount,
            float celestialLightEnergy,
            int selectedEmissiveCount,
            float selectedEmissiveEnergy,
            int shadowCandidateCount,
            int budgetedShadowCandidateCount,
            int sectionSnapshotCount,
            int[] rayBudget,
            int[] celestialLightSources,
            int[] celestialLightFlags,
            float[] celestialLightData,
            String[] emissiveLightDimensions,
            int[] emissiveLightMetadata,
            float[] emissiveLightData,
            long[] emissiveLightGenerations,
            int[] shadowCandidateMetadata,
            float[] shadowCandidateRays,
            long[] shadowCandidateGenerations,
            String[] sectionSnapshotDimensions,
            int[] sectionSnapshotMetadata,
            long[] sectionSnapshotGenerations
    );

    private static native boolean nativeUploadLightingDispatch(
            long generation,
            int dispatchCount,
            long firstDispatchGeneration,
            long lastDispatchGeneration,
            long worldGeneration,
            long materialGeneration,
            long sectionGeneration,
            long gBufferGeneration,
            int[] stageIds,
            String[] stageNames,
            int[] stageEnabled,
            long[] stageGenerations,
            int[] stageDimensions,
            int[] stageDispatchGroups,
            int[] stageWorkgroupSizes,
            int[] stageIoCounts,
            int[] stageSampleRayCounts,
            int[] stageCacheCounts,
            long[] stageEstimatedBytes,
            int[] stageFlags
    );

    private static native boolean nativeRenderLighting();

    private static native boolean nativeEndFrame();

    private static native boolean nativeAdoptBorrowedVulkanContext(long instance, long physicalDevice, long device, long graphicsQueue, int graphicsQueueFamily);

    private static native boolean nativeReleaseBorrowedVulkanContext();

    private static native String nativeStatus();

    private static native byte[] nativeDirectLightingCpuOutputPreviewRgba8();

    private static native byte[] nativeDiffuseGiCpuOutputPreviewRgba8();

    private static native String nativeLastError();
}
