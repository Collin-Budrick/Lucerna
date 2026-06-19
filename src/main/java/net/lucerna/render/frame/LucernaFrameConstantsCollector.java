package net.lucerna.render.frame;

import java.util.ArrayList;
import java.util.List;

public final class LucernaFrameConstantsCollector {
    private FrameCameraMatrices previousCameraMatrices = FrameCameraMatrices.unavailable();
    private FrameViewport previousViewport = FrameViewport.UNAVAILABLE;
    private String previousDimensionId = WorldRenderState.UNKNOWN_DIMENSION;
    private long previousFrameIndex;
    private boolean pendingHistoryReset;
    private String pendingHistoryResetReason = "";
    private FrameConstantsCapture lastCapture = FrameConstantsCapture.unavailable(
            "Frame constants have not been captured."
    );

    public synchronized FrameConstantsCapture capture(FrameConstantsCaptureRequest request) {
        FrameConstantsCaptureRequest normalizedRequest = request == null
                ? FrameConstantsCaptureRequest.unavailable(0L, 0.0F, "null-request")
                : request;
        long capturedNanos = System.nanoTime();
        LucernaFrameConstants constants = normalizedRequest.toFrameConstants(capturedNanos);
        String resetReason = this.historyResetReason(normalizedRequest, constants);
        boolean historyReset = !resetReason.isBlank();
        FrameCameraMatrices previousMatricesForFrame = historyReset
                ? FrameCameraMatrices.unavailable()
                : this.previousCameraMatrices;
        long previousIndexForFrame = historyReset ? 0L : this.previousFrameIndex;

        FrameMatrixHistory matrixHistory = new FrameMatrixHistory(
                constants.frameIndex(),
                constants.cameraMatrices(),
                previousIndexForFrame,
                previousMatricesForFrame,
                historyReset,
                resetReason
        );

        FrameConstantsCapture capture = new FrameConstantsCapture(
                constants,
                matrixHistory,
                capturedInputs(constants, matrixHistory),
                missingInputs(constants, matrixHistory),
                normalizedRequest.source(),
                message(constants, matrixHistory)
        );

        this.updateHistory(constants);
        this.pendingHistoryReset = false;
        this.pendingHistoryResetReason = "";
        this.lastCapture = capture;
        return capture;
    }

    public synchronized FrameConstantsCapture capture(
            long frameIndex,
            float tickDelta,
            FrameViewport viewport,
            WorldRenderState worldState,
            FrameRenderFlags flags
    ) {
        return this.capture(FrameConstantsCaptureRequest.builder(frameIndex, tickDelta)
                .viewport(viewport)
                .worldState(worldState)
                .flags(flags)
                .build());
    }

    public synchronized FrameConstantsCapture captureMinecraftContext(
            Object minecraftClient,
            Object renderContext,
            FrameRenderFlags flags,
            long frameIndex,
            float tickDelta
    ) {
        return this.captureMinecraftContext(
                minecraftClient,
                renderContext,
                flags,
                frameIndex,
                tickDelta,
                FrameJitter.disabled()
        );
    }

    public synchronized FrameConstantsCapture captureMinecraftContext(
            Object minecraftClient,
            Object renderContext,
            FrameRenderFlags flags,
            long frameIndex,
            float tickDelta,
            FrameJitter jitter
    ) {
        return this.capture(ReflectiveFrameContextExtractor.extract(
                minecraftClient,
                renderContext,
                flags,
                frameIndex,
                tickDelta,
                jitter
        ));
    }

    public synchronized void requestHistoryReset(String reason) {
        this.pendingHistoryReset = true;
        this.pendingHistoryResetReason = clean(reason, "History reset requested by controller.");
    }

    public synchronized void reset() {
        this.previousCameraMatrices = FrameCameraMatrices.unavailable();
        this.previousViewport = FrameViewport.UNAVAILABLE;
        this.previousDimensionId = WorldRenderState.UNKNOWN_DIMENSION;
        this.previousFrameIndex = 0L;
        this.pendingHistoryReset = false;
        this.pendingHistoryResetReason = "";
        this.lastCapture = FrameConstantsCapture.unavailable("Frame constants collector was reset.");
    }

    public synchronized FrameConstantsCapture lastCapture() {
        return this.lastCapture;
    }

    public synchronized FrameMatrixHistory matrixHistory() {
        return this.lastCapture.matrixHistory();
    }

    private String historyResetReason(FrameConstantsCaptureRequest request, LucernaFrameConstants constants) {
        List<String> reasons = new ArrayList<>();
        if (this.pendingHistoryReset) {
            reasons.add(clean(this.pendingHistoryResetReason, "History reset requested by controller."));
        }
        if (request.historyResetRequested()) {
            reasons.add(clean(request.historyResetReason(), "History reset requested by frame context."));
        }
        if (this.previousFrameIndex > 0L && constants.hasFrameIndex() && constants.frameIndex() <= this.previousFrameIndex) {
            reasons.add("Frame index did not advance.");
        }
        if (this.previousViewport.available()
                && constants.hasViewport()
                && !this.previousViewport.equals(constants.viewport())) {
            reasons.add("Viewport changed from " + this.previousViewport.label() + " to " + constants.viewport().label() + ".");
        }
        if (!WorldRenderState.UNKNOWN_DIMENSION.equals(this.previousDimensionId)
                && constants.worldState().hasDimension()
                && !this.previousDimensionId.equals(constants.worldState().dimensionId())) {
            reasons.add("Dimension changed from " + this.previousDimensionId + " to "
                    + constants.worldState().dimensionId() + ".");
        }
        if (constants.hasCameraMatrices() && !this.previousCameraMatrices.hasRequiredMatrices()) {
            reasons.add("No previous camera matrices are available.");
        }
        if (!constants.hasCameraMatrices() && this.previousCameraMatrices.hasRequiredMatrices()) {
            reasons.add("Current camera matrices are unavailable.");
        }
        return String.join(" ", reasons);
    }

    private void updateHistory(LucernaFrameConstants constants) {
        if (constants.hasCameraMatrices()) {
            this.previousCameraMatrices = constants.cameraMatrices();
        } else {
            this.previousCameraMatrices = FrameCameraMatrices.unavailable();
        }
        if (constants.hasFrameIndex()) {
            this.previousFrameIndex = constants.frameIndex();
        }
        if (constants.hasViewport()) {
            this.previousViewport = constants.viewport();
        }
        if (constants.worldState().hasDimension()) {
            this.previousDimensionId = constants.worldState().dimensionId();
        }
    }

    private static List<String> capturedInputs(LucernaFrameConstants constants, FrameMatrixHistory matrixHistory) {
        List<String> captured = new ArrayList<>();
        captured.add("tickDelta");
        if (constants.hasFrameIndex()) {
            captured.add("frameIndex");
        }
        if (constants.hasViewport()) {
            captured.add("viewport");
        }
        if (constants.hasWorldState()) {
            captured.add("worldState");
        }
        if (constants.hasCameraMatrices()) {
            captured.add("cameraMatrices");
        }
        if (constants.jitter().available()) {
            captured.add("jitter");
        }
        if (constants.hasRenderFlags()) {
            captured.add("renderFlags");
        }
        if (matrixHistory.hasPreviousMatrices()) {
            captured.add("previousCameraMatrices");
        }
        if (matrixHistory.historyReset()) {
            captured.add("historyReset");
        }
        return List.copyOf(captured);
    }

    private static List<String> missingInputs(LucernaFrameConstants constants, FrameMatrixHistory matrixHistory) {
        List<String> missing = new ArrayList<>(constants.missingRequiredConstants());
        if (!constants.jitter().available()) {
            missing.add("jitter");
        }
        if (constants.hasCameraMatrices() && !matrixHistory.hasPreviousMatrices()) {
            missing.add("previousCameraMatrices");
        }
        return List.copyOf(missing);
    }

    private static String message(LucernaFrameConstants constants, FrameMatrixHistory matrixHistory) {
        if (!constants.hasRequiredConstants()) {
            return "Frame constants captured with missing required inputs: "
                    + String.join(", ", constants.missingRequiredConstants())
                    + ".";
        }
        if (matrixHistory.temporalReuseAllowed()) {
            return "Frame constants captured with reusable previous camera matrices.";
        }
        if (matrixHistory.historyReset()) {
            return "Frame constants captured; temporal history reset: " + matrixHistory.resetReason();
        }
        return "Frame constants captured without previous camera matrix history.";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
