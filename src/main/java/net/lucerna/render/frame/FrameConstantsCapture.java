package net.lucerna.render.frame;

import java.util.List;

public record FrameConstantsCapture(
        LucernaFrameConstants constants,
        FrameMatrixHistory matrixHistory,
        List<String> capturedInputs,
        List<String> missingInputs,
        String source,
        String message
) {
    public FrameConstantsCapture {
        if (constants == null) {
            constants = LucernaFrameConstants.unavailable();
        }
        if (matrixHistory == null) {
            matrixHistory = FrameMatrixHistory.unavailable("Matrix history has not been captured.");
        }
        capturedInputs = capturedInputs == null ? List.of() : List.copyOf(capturedInputs);
        missingInputs = missingInputs == null ? List.of() : List.copyOf(missingInputs);
        source = clean(source, FrameConstantsCaptureRequest.SOURCE_MANUAL);
        message = clean(message, "Frame constants capture status has not been reported.");
    }

    public static FrameConstantsCapture unavailable(String message) {
        return new FrameConstantsCapture(
                LucernaFrameConstants.unavailable(),
                FrameMatrixHistory.unavailable(message),
                List.of(),
                List.of("frameIndex", "viewport", "worldState", "cameraMatrices", "renderFlags"),
                FrameConstantsCaptureRequest.SOURCE_MANUAL,
                message
        );
    }

    public boolean constantsAvailable() {
        return this.constants.hasFrameIndex()
                || this.constants.hasViewport()
                || this.constants.hasWorldState()
                || this.constants.hasCameraMatrices()
                || this.constants.hasRenderFlags();
    }

    public boolean requiredConstantsAvailable() {
        return this.constants.hasRequiredConstants();
    }

    public boolean freshForFrame(long expectedFrameIndex) {
        return this.constants.freshForFrame(expectedFrameIndex);
    }

    public boolean temporalReuseAllowed() {
        return this.matrixHistory.temporalReuseAllowed();
    }

    public String stateLabel() {
        if (!this.constantsAvailable()) {
            return "unavailable";
        }
        if (!this.requiredConstantsAvailable()) {
            return "partial";
        }
        if (this.temporalReuseAllowed()) {
            return "ready-with-history";
        }
        return "ready-current-only";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
