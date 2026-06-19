package net.lucerna.telemetry;

import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.render.hooks.FrameLifecycleSnapshot;

import java.util.List;

public record FrameConstantsTelemetryStatus(
        boolean constantsAvailable,
        boolean requiredConstantsAvailable,
        boolean freshForFrame,
        long constantsFrameIndex,
        long lifecycleFrameIndex,
        long constantsCapturedNanos,
        long snapshotCapturedNanos,
        long ageNanos,
        List<String> missingRequiredConstants,
        String message
) {
    public FrameConstantsTelemetryStatus {
        constantsFrameIndex = Math.max(0L, constantsFrameIndex);
        lifecycleFrameIndex = Math.max(0L, lifecycleFrameIndex);
        constantsCapturedNanos = Math.max(0L, constantsCapturedNanos);
        snapshotCapturedNanos = Math.max(0L, snapshotCapturedNanos);
        ageNanos = ageNanos < 0L ? -1L : ageNanos;
        missingRequiredConstants = missingRequiredConstants == null ? List.of() : List.copyOf(missingRequiredConstants);
        message = clean(message, "Frame constants status has not been reported.");
        if (!constantsAvailable) {
            requiredConstantsAvailable = false;
            freshForFrame = false;
        }
        if (!requiredConstantsAvailable) {
            freshForFrame = false;
        }
    }

    public static FrameConstantsTelemetryStatus unavailable(
            FrameLifecycleSnapshot lifecycle,
            long snapshotCapturedNanos
    ) {
        return unavailable(lifecycle, snapshotCapturedNanos, "Frame constants have not been wired by the controller.");
    }

    public static FrameConstantsTelemetryStatus unavailable(
            FrameLifecycleSnapshot lifecycle,
            long snapshotCapturedNanos,
            String message
    ) {
        long lifecycleFrameIndex = lifecycle == null ? 0L : lifecycle.frameIndex();
        return new FrameConstantsTelemetryStatus(
                false,
                false,
                false,
                0L,
                lifecycleFrameIndex,
                0L,
                snapshotCapturedNanos,
                -1L,
                List.of("frameConstants"),
                message
        );
    }

    public static FrameConstantsTelemetryStatus from(
            LucernaFrameConstants constants,
            FrameLifecycleSnapshot lifecycle,
            long snapshotCapturedNanos
    ) {
        if (constants == null) {
            return unavailable(lifecycle, snapshotCapturedNanos);
        }

        long lifecycleFrameIndex = lifecycle == null ? 0L : lifecycle.frameIndex();
        long ageNanos = ageNanos(constants.capturedNanos(), snapshotCapturedNanos);
        boolean requiredConstantsAvailable = constants.hasRequiredConstants();
        boolean freshForFrame = constants.freshForFrame(lifecycleFrameIndex);
        List<String> missingRequiredConstants = constants.missingRequiredConstants();

        return new FrameConstantsTelemetryStatus(
                true,
                requiredConstantsAvailable,
                freshForFrame,
                constants.frameIndex(),
                lifecycleFrameIndex,
                constants.capturedNanos(),
                snapshotCapturedNanos,
                ageNanos,
                missingRequiredConstants,
                message(constants.frameIndex(), lifecycleFrameIndex, requiredConstantsAvailable, freshForFrame, missingRequiredConstants)
        );
    }

    public String stateLabel() {
        if (!this.constantsAvailable) {
            return "unwired";
        }
        if (!this.requiredConstantsAvailable) {
            return "missing";
        }
        return this.freshForFrame ? "fresh" : "stale";
    }

    public double ageMillis() {
        if (this.ageNanos < 0L) {
            return -1.0D;
        }
        return this.ageNanos / 1_000_000.0D;
    }

    public String missingRequiredLabel() {
        if (this.missingRequiredConstants.isEmpty()) {
            return "none";
        }
        return String.join(",", this.missingRequiredConstants);
    }

    private static long ageNanos(long constantsCapturedNanos, long snapshotCapturedNanos) {
        if (constantsCapturedNanos <= 0L || snapshotCapturedNanos <= 0L || snapshotCapturedNanos < constantsCapturedNanos) {
            return -1L;
        }
        return snapshotCapturedNanos - constantsCapturedNanos;
    }

    private static String message(
            long constantsFrameIndex,
            long lifecycleFrameIndex,
            boolean requiredConstantsAvailable,
            boolean freshForFrame,
            List<String> missingRequiredConstants
    ) {
        if (!requiredConstantsAvailable) {
            return "Frame constants missing required fields: " + String.join(", ", missingRequiredConstants);
        }
        if (!freshForFrame) {
            return "Frame constants are for frame "
                    + constantsFrameIndex
                    + " while lifecycle reports frame "
                    + lifecycleFrameIndex
                    + ".";
        }
        return "Frame constants are fresh for frame " + constantsFrameIndex + ".";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
