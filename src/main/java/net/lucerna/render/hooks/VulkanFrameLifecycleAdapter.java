package net.lucerna.render.hooks;

import net.lucerna.compat.BackendStatus;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.telemetry.LucernaTelemetry;

import java.util.Objects;

public final class VulkanFrameLifecycleAdapter {
    private static final String FRAME_SCOPE = "frame";
    private static final String LIGHTING_SCOPE = "lighting";

    private final LucernaNativeBridge nativeBridge;
    private final LucernaTelemetry telemetry;

    private long frameIndex;
    private int viewportWidth;
    private int viewportHeight;
    private boolean resizePending;
    private boolean frameOpen;
    private boolean lightingSubmitted;
    private FrameHookStage stage = FrameHookStage.IDLE;
    private FramePassIntent passIntent = FramePassIntent.NONE;
    private FrameHookResult lastResult = FrameHookResult.skipped(0L, FramePassIntent.NONE, "Frame lifecycle has not started.");

    public VulkanFrameLifecycleAdapter(LucernaNativeBridge nativeBridge, LucernaTelemetry telemetry) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    public synchronized boolean accepts(BackendStatus backendStatus) {
        return backendStatus != null && backendStatus.sodiumVulkan();
    }

    public synchronized FrameHookResult onResize(int width, int height) {
        this.viewportWidth = Math.max(0, width);
        this.viewportHeight = Math.max(0, height);

        if (width <= 0 || height <= 0) {
            this.resizePending = false;
            return this.skip("Resize ignored until the viewport has positive dimensions.");
        }

        if (this.frameOpen) {
            this.resizePending = true;
            return this.accept(
                    FrameHookEvent.RESIZE_QUEUED,
                    false,
                    FramePassIntent.NONE,
                    "Resize queued until the active frame ends."
            );
        }

        if (!this.isNativeOperational()) {
            this.resizePending = true;
            this.stage = FrameHookStage.RESIZE_PENDING;
            return this.accept(
                    FrameHookEvent.RESIZE_QUEUED,
                    false,
                    FramePassIntent.NONE,
                    "Resize queued until the native bridge is operational."
            );
        }

        this.nativeBridge.onResize(width, height);
        this.resizePending = false;
        if (!this.isNativeOperational()) {
            return this.failAfterNative("Native resize disabled the bridge: " + this.nativeBridge.lastError());
        }

        return this.accept(
                FrameHookEvent.RESIZE_SUBMITTED,
                true,
                FramePassIntent.NONE,
                "Resize submitted to the native bridge."
        );
    }

    public synchronized FrameHookResult beginFrame(BackendStatus backendStatus, float tickDelta) {
        if (!this.accepts(backendStatus)) {
            String backendMessage = backendStatus == null ? "missing backend status" : backendStatus.diagnosticSummary();
            return this.skip("Frame skipped because the backend is not Sodium Vulkan: " + backendMessage);
        }

        return this.beginFrame(tickDelta);
    }

    public synchronized FrameHookResult beginFrame(float tickDelta) {
        if (this.frameOpen) {
            return this.skip("beginFrame skipped because frame " + this.frameIndex + " is still open.");
        }

        if (!this.isNativeOperational()) {
            return this.skip("beginFrame skipped because the native bridge is not operational.");
        }

        long nextFrame = this.frameIndex + 1L;
        this.telemetry.beginCpuScope(FRAME_SCOPE);

        if (!this.submitPendingResize()) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
            return this.failAfterNative(nextFrame, "beginFrame skipped because pending resize disabled the native bridge.");
        }

        this.nativeBridge.beginFrame(nextFrame, tickDelta);
        if (!this.isNativeOperational()) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
            return this.failAfterNative(nextFrame, "Native beginFrame disabled the bridge: " + this.nativeBridge.lastError());
        }

        this.frameIndex = nextFrame;
        this.frameOpen = true;
        this.lightingSubmitted = false;
        this.stage = FrameHookStage.FRAME_ACTIVE;
        this.passIntent = FramePassIntent.NO_OP_LIGHTING_PASS;
        return this.accept(
                FrameHookEvent.BEGIN_FRAME,
                true,
                this.passIntent,
                "Frame " + this.frameIndex + " began; no-op lighting pass is expected."
        );
    }

    public synchronized FrameHookResult renderLighting() {
        if (!this.frameOpen) {
            return this.skip("renderLighting skipped because no frame is open.");
        }

        if (this.lightingSubmitted) {
            return this.skip("renderLighting skipped because frame " + this.frameIndex + " already submitted lighting.");
        }

        if (!this.isNativeOperational()) {
            this.closeLocalFrame();
            return this.skip("renderLighting skipped because the native bridge is not operational.");
        }

        this.telemetry.beginCpuScope(LIGHTING_SCOPE);
        try {
            this.nativeBridge.renderLighting();
        } finally {
            this.telemetry.endCpuScope(LIGHTING_SCOPE);
        }

        if (!this.isNativeOperational()) {
            this.closeLocalFrame();
            return this.failAfterNative("Native renderLighting disabled the bridge: " + this.nativeBridge.lastError());
        }

        this.lightingSubmitted = true;
        this.stage = FrameHookStage.LIGHTING_SUBMITTED;
        this.passIntent = FramePassIntent.NO_OP_LIGHTING_PASS;
        return this.accept(
                FrameHookEvent.RENDER_LIGHTING,
                true,
                this.passIntent,
                "No-op lighting pass submitted for frame " + this.frameIndex + "."
        );
    }

    public synchronized FrameHookResult endFrame() {
        if (!this.frameOpen) {
            return this.skip("endFrame skipped because no frame is open.");
        }

        if (!this.isNativeOperational()) {
            this.closeLocalFrame();
            return this.skip("endFrame skipped because the native bridge is not operational.");
        }

        this.nativeBridge.endFrame();
        this.telemetry.endCpuScope(FRAME_SCOPE);
        this.frameOpen = false;
        this.passIntent = FramePassIntent.NONE;
        this.stage = FrameHookStage.FRAME_COMPLETE;

        if (!this.isNativeOperational()) {
            return this.failAfterNative("Native endFrame disabled the bridge: " + this.nativeBridge.lastError());
        }

        return this.accept(
                FrameHookEvent.END_FRAME,
                true,
                FramePassIntent.NONE,
                "Frame " + this.frameIndex + " ended."
        );
    }

    public synchronized long frameIndex() {
        return this.frameIndex;
    }

    public synchronized FrameHookResult lastResult() {
        return this.lastResult;
    }

    public synchronized FrameLifecycleSnapshot snapshot() {
        return new FrameLifecycleSnapshot(
                this.frameIndex,
                this.stage,
                this.passIntent,
                this.viewportWidth,
                this.viewportHeight,
                this.resizePending,
                this.frameOpen,
                this.lightingSubmitted,
                this.lastResult.message()
        );
    }

    private boolean submitPendingResize() {
        if (!this.resizePending) {
            return true;
        }

        if (this.viewportWidth <= 0 || this.viewportHeight <= 0) {
            this.resizePending = false;
            return true;
        }

        this.nativeBridge.onResize(this.viewportWidth, this.viewportHeight);
        this.resizePending = false;
        return this.isNativeOperational();
    }

    private boolean isNativeOperational() {
        return this.nativeBridge.isLoaded()
                && this.nativeBridge.isAvailable()
                && this.nativeBridge.isInitialized();
    }

    private void closeLocalFrame() {
        if (this.frameOpen) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
        }
        this.frameOpen = false;
        this.lightingSubmitted = false;
        this.passIntent = FramePassIntent.NONE;
        this.stage = FrameHookStage.SKIPPED;
    }

    private FrameHookResult accept(FrameHookEvent event, boolean nativeCallIssued, FramePassIntent intent, String message) {
        this.lastResult = FrameHookResult.accepted(event, nativeCallIssued, this.frameIndex, intent, message);
        this.stage = this.stageForEvent(event);
        return this.lastResult;
    }

    private FrameHookResult skip(String message) {
        return this.skipForFrame(this.frameIndex, message);
    }

    private FrameHookResult skipForFrame(long skippedFrameIndex, String message) {
        this.stage = FrameHookStage.SKIPPED;
        this.lastResult = FrameHookResult.skipped(skippedFrameIndex, this.passIntent, message);
        return this.lastResult;
    }

    private FrameHookResult failAfterNative(String message) {
        return this.failAfterNative(this.frameIndex, message);
    }

    private FrameHookResult failAfterNative(long skippedFrameIndex, String message) {
        this.stage = FrameHookStage.SKIPPED;
        this.lastResult = FrameHookResult.failedNative(skippedFrameIndex, this.passIntent, message);
        return this.lastResult;
    }

    private FrameHookStage stageForEvent(FrameHookEvent event) {
        return switch (event) {
            case RESIZE_QUEUED -> FrameHookStage.RESIZE_PENDING;
            case RESIZE_SUBMITTED -> FrameHookStage.RESIZE_SUBMITTED;
            case BEGIN_FRAME -> FrameHookStage.FRAME_ACTIVE;
            case RENDER_LIGHTING -> FrameHookStage.LIGHTING_SUBMITTED;
            case END_FRAME -> FrameHookStage.FRAME_COMPLETE;
            case SKIPPED -> FrameHookStage.SKIPPED;
        };
    }
}
