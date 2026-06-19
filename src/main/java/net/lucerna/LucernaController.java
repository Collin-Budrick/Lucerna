package net.lucerna;

import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;
import net.lucerna.compat.iris.IrisCompat;
import net.lucerna.compat.sodium.LucernaBackendDetector;
import net.lucerna.config.LucernaConfig;
import net.lucerna.config.LucernaConfigManager;
import net.lucerna.material.MaterialRegistry;
import net.lucerna.material.extract.LucernaMaterialExtractionService;
import net.lucerna.material.extract.MaterialTableRefreshResult;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.render.LucernaFrameHooks;
import net.lucerna.telemetry.LucernaTelemetry;
import net.lucerna.upload.NativeUploadQueue;
import net.lucerna.world.LucernaWorldFeed;
import net.minecraft.client.resources.model.ModelManager;

public final class LucernaController {
    private static final LucernaController INSTANCE = new LucernaController();

    private final LucernaConfigManager configManager = new LucernaConfigManager();
    private final LucernaBackendDetector backendDetector = new LucernaBackendDetector();
    private final IrisCompat irisCompat = new IrisCompat();
    private final LucernaNativeBridge nativeBridge = new LucernaNativeBridge();
    private final LucernaWorldFeed worldFeed = new LucernaWorldFeed();
    private final MaterialRegistry materialRegistry = new MaterialRegistry();
    private final LucernaMaterialExtractionService materialExtractionService = new LucernaMaterialExtractionService(this.materialRegistry);
    private final NativeUploadQueue uploadQueue = new NativeUploadQueue();
    private final LucernaTelemetry telemetry = new LucernaTelemetry();
    private final LucernaFrameHooks frameHooks = new LucernaFrameHooks(nativeBridge, telemetry);

    private BackendStatus backendStatus = BackendStatus.disabled(BackendKind.UNKNOWN, "Lucerna has not checked the renderer backend yet.");
    private boolean initialized;
    private boolean nativeInitialized;
    private int viewportWidth = -1;
    private int viewportHeight = -1;

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
            var dirtyRegions = this.worldFeed.drainDirtyRegionSnapshot();
            var materialUpdates = this.materialRegistry.snapshotUpdatesAfter(this.uploadQueue.lastMaterialGeneration());
            var batch = this.uploadQueue.acceptWorldAndMaterialDeltas(dirtyRegions, materialUpdates);
            this.nativeBridge.uploadWorldDeltas(batch);
            this.submitNoOpFrame(0.0F);
        }
    }

    public void shutdown() {
        if (this.nativeInitialized) {
            this.nativeBridge.shutdown();
            this.nativeInitialized = false;
        }
        this.telemetry.clear();
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

    public LucernaNativeBridge.NativeBridgeStatus nativeBridgeStatus() {
        return this.nativeBridge.status();
    }

    public void onViewportChanged(int width, int height) {
        if (width == this.viewportWidth && height == this.viewportHeight) {
            return;
        }

        this.viewportWidth = width;
        this.viewportHeight = height;
        this.frameHooks.onResize(width, height);
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
    }

    private void activateOrDeactivateNative() {
        if (!this.getConfig().rendererEnabled() || !this.backendStatus.active()) {
            if (this.nativeInitialized) {
                this.nativeBridge.shutdown();
                this.nativeInitialized = false;
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
        }
    }

    private void ensureMaterialTablePrepared() {
        if (this.materialExtractionService.lastRefreshResult().isPresent()) {
            return;
        }

        this.refreshMaterials(null);
    }

    private void submitNoOpFrame(float tickDelta) {
        var beginResult = this.frameHooks.beginFrame(this.backendStatus, tickDelta);
        if (!beginResult.accepted()) {
            return;
        }

        this.frameHooks.renderLighting();
        this.frameHooks.endFrame();
    }
}
