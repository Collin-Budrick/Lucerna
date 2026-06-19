package net.lucerna.render.hooks;

import net.lucerna.compat.BackendStatus;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.render.context.BorrowedVulkanContextAcquisition;
import net.lucerna.render.context.BorrowedVulkanContextHandles;
import net.lucerna.render.context.BorrowedVulkanContextProbe;
import net.lucerna.render.context.GuardedBorrowedVulkanContextAdapter;
import net.lucerna.render.context.VulkanFrameContextRequest;
import net.lucerna.render.pass.LucernaFramePassKind;
import net.lucerna.render.pass.LucernaFramePassRequest;
import net.lucerna.render.pass.LucernaFramePassResult;
import net.lucerna.render.pass.LucernaFramePassStatus;
import net.lucerna.telemetry.LucernaTelemetry;

import java.util.Locale;
import java.util.Objects;

public final class VulkanFrameLifecycleAdapter {
    private static final String FRAME_SCOPE = "frame";
    private static final String LIGHTING_SCOPE = "lighting";

    private final LucernaNativeBridge nativeBridge;
    private final LucernaTelemetry telemetry;
    private final GuardedBorrowedVulkanContextAdapter contextAdapter;

    private long frameIndex;
    private int viewportWidth;
    private int viewportHeight;
    private boolean resizePending;
    private boolean frameOpen;
    private boolean lightingSubmitted;
    private boolean borrowedContextAdopted;
    private FrameHookStage stage = FrameHookStage.IDLE;
    private FramePassIntent passIntent = FramePassIntent.NONE;
    private LucernaFramePassStatus framePassStatus = LucernaFramePassStatus.notRequested();
    private BorrowedVulkanContextAcquisition lastContextAcquisition = BorrowedVulkanContextAcquisition.absent(
            "Frame context acquisition has not started."
    );
    private FrameHookResult lastResult = FrameHookResult.skipped(
            0L,
            FramePassIntent.NONE,
            "Frame lifecycle has not started.",
            this.lastContextAcquisition
    );

    public VulkanFrameLifecycleAdapter(LucernaNativeBridge nativeBridge, LucernaTelemetry telemetry) {
        this(nativeBridge, telemetry, BorrowedVulkanContextProbe.unwired());
    }

    public VulkanFrameLifecycleAdapter(
            LucernaNativeBridge nativeBridge,
            LucernaTelemetry telemetry,
            BorrowedVulkanContextProbe contextProbe
    ) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.contextAdapter = new GuardedBorrowedVulkanContextAdapter(contextProbe);
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
                    "Resize queued until the native bridge is operational and a borrowed Vulkan context is ready."
            );
        }

        this.resizePending = true;
        this.stage = FrameHookStage.RESIZE_PENDING;
        return this.accept(
                FrameHookEvent.RESIZE_QUEUED,
                false,
                FramePassIntent.NONE,
                "Resize queued until the next adopted borrowed Vulkan context."
        );
    }

    public synchronized FrameHookResult beginFrame(BackendStatus backendStatus, float tickDelta) {
        if (!this.accepts(backendStatus)) {
            String backendMessage = backendStatus == null ? "missing backend status" : backendStatus.diagnosticSummary();
            this.lastContextAcquisition = BorrowedVulkanContextAcquisition.absent(
                    "frame-hook",
                    "Borrowed Vulkan context was not requested because the backend is not Sodium Vulkan."
            );
            this.framePassStatus = LucernaFramePassStatus.waitingForContext(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    this.frameIndex + 1L,
                    false,
                    "Frame pass attachment skipped because the backend is not Sodium Vulkan."
            );
            return this.skip("Frame skipped because the backend is not Sodium Vulkan: " + backendMessage);
        }

        return this.beginFrame(backendStatus, tickDelta, this.frameIndex + 1L);
    }

    public synchronized FrameHookResult beginFrame(float tickDelta) {
        return this.beginFrame(null, tickDelta, this.frameIndex + 1L);
    }

    private FrameHookResult beginFrame(BackendStatus backendStatus, float tickDelta, long nextFrame) {
        if (this.frameOpen) {
            this.framePassStatus = LucernaFramePassStatus.waitingForFrame(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    this.frameIndex,
                    "Frame pass attachment is waiting for the current active frame to close."
            );
            return this.skip("beginFrame skipped because frame " + this.frameIndex + " is still open.");
        }

        BorrowedVulkanContextAcquisition contextAcquisition = this.acquireFrameContext(
                backendStatus,
                tickDelta,
                nextFrame
        );
        if (!contextAcquisition.ready()) {
            this.framePassStatus = LucernaFramePassStatus.waitingForContext(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    nextFrame,
                    false,
                    "Frame pass attachment is waiting for a READY borrowed Vulkan context."
            );
            return this.skipForFrame(
                    nextFrame,
                    "beginFrame skipped because borrowed Vulkan context is "
                            + contextAcquisition.status().name().toLowerCase(Locale.ROOT)
                            + ": "
                            + contextAcquisition.message()
            );
        }

        if (!this.isNativeOperational()) {
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    nextFrame,
                    "Frame pass attachment skipped because the native bridge is not operational."
            );
            return this.skip("beginFrame skipped because the native bridge is not operational.");
        }

        this.telemetry.beginCpuScope(FRAME_SCOPE);

        if (!this.adoptBorrowedContext(contextAcquisition)) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    nextFrame,
                    "Frame pass attachment skipped because the borrowed Vulkan context could not be adopted."
            );
            return this.failAfterNative(nextFrame, "beginFrame skipped because the borrowed Vulkan context could not be adopted.");
        }

        if (!this.submitPendingResize()) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
            this.releaseBorrowedContext();
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    nextFrame,
                    "Frame pass attachment skipped because pending resize disabled the native bridge."
            );
            return this.failAfterNative(nextFrame, "beginFrame skipped because pending resize disabled the native bridge.");
        }

        this.nativeBridge.beginFrame(nextFrame, tickDelta);
        if (!this.isNativeOperational()) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
            this.releaseBorrowedContext();
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    LucernaFramePassKind.FLAT_COMPOSITE,
                    nextFrame,
                    "Frame pass attachment skipped because native beginFrame disabled the bridge."
            );
            return this.failAfterNative(nextFrame, "Native beginFrame disabled the bridge: " + this.nativeBridge.lastError());
        }

        this.frameIndex = nextFrame;
        this.frameOpen = true;
        this.lightingSubmitted = false;
        this.stage = FrameHookStage.FRAME_ACTIVE;
        this.passIntent = FramePassIntent.NO_OP_LIGHTING_PASS;
        this.framePassStatus = LucernaFramePassStatus.waitingForTarget(
                LucernaFramePassKind.FLAT_COMPOSITE,
                this.frameIndex,
                "Borrowed Vulkan context is READY and frame is active; no safe pass target has been supplied."
        );
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

    public synchronized LucernaFramePassResult attachFramePass(LucernaFramePassRequest request) {
        LucernaFramePassRequest normalizedRequest = request == null
                ? LucernaFramePassRequest.noOp(this.frameIndex)
                : request;

        if (!this.frameOpen) {
            this.framePassStatus = LucernaFramePassStatus.waitingForFrame(
                    normalizedRequest.kind(),
                    normalizedRequest.hasExplicitFrameIndex() ? normalizedRequest.frameIndex() : this.frameIndex,
                    "Frame pass attachment skipped because no frame is active."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        if (!normalizedRequest.matchesFrame(this.frameIndex)) {
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    normalizedRequest.kind(),
                    this.frameIndex,
                    "Frame pass attachment skipped because request frame "
                            + normalizedRequest.frameIndex()
                            + " does not match active frame "
                            + this.frameIndex
                            + "."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        if (!this.lastContextAcquisition.ready()) {
            this.framePassStatus = LucernaFramePassStatus.waitingForContext(
                    normalizedRequest.kind(),
                    this.frameIndex,
                    true,
                    "Frame pass attachment skipped because the borrowed Vulkan context is not READY."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        if (!this.isNativeOperational()) {
            this.closeLocalFrame();
            this.framePassStatus = LucernaFramePassStatus.skipped(
                    normalizedRequest.kind(),
                    this.frameIndex,
                    "Frame pass attachment skipped because the native bridge is not operational."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        if (!normalizedRequest.hasTarget()) {
            this.framePassStatus = LucernaFramePassStatus.waitingForTarget(
                    normalizedRequest.kind(),
                    this.frameIndex,
                    "Frame pass attachment skipped because no safe Mojang render-pass target was supplied."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        if (!normalizedRequest.targetSafeForAttachment()) {
            this.framePassStatus = LucernaFramePassStatus.targetUnsafe(
                    normalizedRequest.kind(),
                    this.frameIndex,
                    "Frame pass attachment skipped because the target was not marked safe for Lucerna."
            );
            return LucernaFramePassResult.skipped(normalizedRequest, this.framePassStatus);
        }

        this.passIntent = this.intentFor(normalizedRequest.kind());
        this.framePassStatus = LucernaFramePassStatus.attachedNoOp(
                normalizedRequest.kind(),
                this.frameIndex,
                "Lucerna accepted a safe frame pass target; attachment remains a guarded no-op."
        );
        return LucernaFramePassResult.acceptedNoOp(normalizedRequest, this.framePassStatus);
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
        this.releaseBorrowedContext();
        this.telemetry.endCpuScope(FRAME_SCOPE);
        this.frameOpen = false;
        this.passIntent = FramePassIntent.NONE;
        this.stage = FrameHookStage.FRAME_COMPLETE;
        this.framePassStatus = LucernaFramePassStatus.frameClosed(
                this.framePassStatus.kind(),
                this.frameIndex,
                "Frame " + this.frameIndex + " ended; Lucerna frame pass attachment is closed."
        );

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

    public synchronized LucernaFramePassStatus framePassStatus() {
        return this.framePassStatus;
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
                this.lastContextAcquisition,
                this.framePassStatus,
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

        if (!this.borrowedContextAdopted) {
            return false;
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

    private BorrowedVulkanContextAcquisition acquireFrameContext(
            BackendStatus backendStatus,
            float tickDelta,
            long nextFrame
    ) {
        this.lastContextAcquisition = this.contextAdapter.acquire(new VulkanFrameContextRequest(
                nextFrame,
                tickDelta,
                this.viewportWidth,
                this.viewportHeight,
                backendStatus
        ));
        return this.lastContextAcquisition;
    }

    private boolean adoptBorrowedContext(BorrowedVulkanContextAcquisition acquisition) {
        if (this.borrowedContextAdopted) {
            return true;
        }

        BorrowedVulkanContextHandles handles = acquisition.handles();
        if (!acquisition.ready() || handles == null || !handles.hasRequiredHandles()) {
            return false;
        }

        boolean adopted = this.nativeBridge.adoptBorrowedVulkanContext(new LucernaNativeBridge.BorrowedVulkanContext(
                handles.instance(),
                handles.physicalDevice(),
                handles.device(),
                handles.graphicsQueue(),
                handles.graphicsQueueFamily()
        ));
        this.borrowedContextAdopted = adopted && this.isNativeOperational();
        return this.borrowedContextAdopted;
    }

    private void releaseBorrowedContext() {
        if (!this.borrowedContextAdopted) {
            return;
        }

        try {
            this.nativeBridge.releaseBorrowedVulkanContext();
        } finally {
            this.borrowedContextAdopted = false;
        }
    }

    private void closeLocalFrame() {
        this.releaseBorrowedContext();
        if (this.frameOpen) {
            this.telemetry.endCpuScope(FRAME_SCOPE);
        }
        this.frameOpen = false;
        this.lightingSubmitted = false;
        this.passIntent = FramePassIntent.NONE;
        this.stage = FrameHookStage.SKIPPED;
        this.framePassStatus = LucernaFramePassStatus.frameClosed(
                this.framePassStatus.kind(),
                this.frameIndex,
                "Frame was closed locally before Lucerna could attach a frame pass."
        );
    }

    private FrameHookResult accept(FrameHookEvent event, boolean nativeCallIssued, FramePassIntent intent, String message) {
        this.lastResult = FrameHookResult.accepted(
                event,
                nativeCallIssued,
                this.frameIndex,
                intent,
                message,
                this.lastContextAcquisition
        );
        this.stage = this.stageForEvent(event);
        return this.lastResult;
    }

    private FrameHookResult skip(String message) {
        return this.skipForFrame(this.frameIndex, message);
    }

    private FrameHookResult skipForFrame(long skippedFrameIndex, String message) {
        this.stage = FrameHookStage.SKIPPED;
        this.lastResult = FrameHookResult.skipped(
                skippedFrameIndex,
                this.passIntent,
                message,
                this.lastContextAcquisition
        );
        return this.lastResult;
    }

    private FrameHookResult failAfterNative(String message) {
        return this.failAfterNative(this.frameIndex, message);
    }

    private FrameHookResult failAfterNative(long skippedFrameIndex, String message) {
        this.stage = FrameHookStage.SKIPPED;
        this.lastResult = FrameHookResult.failedNative(
                skippedFrameIndex,
                this.passIntent,
                message,
                this.lastContextAcquisition
        );
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

    private FramePassIntent intentFor(LucernaFramePassKind kind) {
        return switch (kind) {
            case FLAT_COMPOSITE -> FramePassIntent.FLAT_COMPOSITE_PASS;
            case NO_OP -> FramePassIntent.NO_OP_FRAME_ATTACHMENT_PASS;
        };
    }
}
