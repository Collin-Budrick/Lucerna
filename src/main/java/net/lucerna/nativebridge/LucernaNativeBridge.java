package net.lucerna.nativebridge;

import net.lucerna.Lucerna;
import net.lucerna.upload.NativeUploadBatch;

public final class LucernaNativeBridge {
    private static final String LIBRARY_NAME = "lucerna_renderer";

    private boolean loadAttempted;
    private boolean loaded;
    private boolean available;
    private boolean initialized;
    private String lastError = "Native library has not been loaded.";

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

    public synchronized void load() {
        if (this.loadAttempted) {
            return;
        }

        this.loadAttempted = true;

        try {
            System.loadLibrary(LIBRARY_NAME);
            this.loaded = true;
            this.available = true;
            this.initialized = false;
            this.lastError = "";
            Lucerna.LOGGER.info("Loaded native library {}.", LIBRARY_NAME);
        } catch (UnsatisfiedLinkError error) {
            this.loaded = false;
            this.available = false;
            this.initialized = false;
            this.lastError = "Could not load native library " + LIBRARY_NAME + ": " + error.getMessage();
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
            this.invokeNative("uploadWorldDeltas", () -> nativeUploadWorldDeltas(batch.generation(), batch.dirtyRegionCount(), batch.materialUpdateCount()), true);
        }
    }

    public synchronized void renderLighting() {
        if (this.isOperational()) {
            this.invokeNative("renderLighting", LucernaNativeBridge::nativeRenderLighting, true);
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

    private static native boolean nativeUploadWorldDeltas(long generation, int dirtyRegionCount, int materialUpdateCount);

    private static native boolean nativeRenderLighting();

    private static native boolean nativeEndFrame();

    private static native boolean nativeAdoptBorrowedVulkanContext(long instance, long physicalDevice, long device, long graphicsQueue, int graphicsQueueFamily);

    private static native boolean nativeReleaseBorrowedVulkanContext();

    private static native String nativeStatus();

    private static native String nativeLastError();
}
