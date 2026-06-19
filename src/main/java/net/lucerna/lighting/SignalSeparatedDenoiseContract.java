package net.lucerna.lighting;

import java.util.List;
import java.util.Objects;

public record SignalSeparatedDenoiseContract(
        long frameIndex,
        long generation,
        int width,
        int height,
        boolean enabled,
        DenoiseSignalInputContract diffuseGi,
        DenoiseSignalInputContract directShadows,
        DenoiseSignalInputContract specular,
        DenoiseSignalInputContract ambientOcclusion,
        DenoiseEdgeRejectionInputs edgeRejectionInputs,
        DenoiseHistoryCounters historyCounters,
        DenoiseOutputContract output,
        String readinessReason
) {
    public SignalSeparatedDenoiseContract {
        frameIndex = Math.max(0L, frameIndex);
        generation = Math.max(0L, generation);
        width = Math.max(0, width);
        height = Math.max(0, height);
        diffuseGi = diffuseGi == null
                ? DenoiseSignalInputContract.diffuseGi(false, 0L, 0, 0, 0, 0, 0, "diffuse GI signal not supplied")
                : diffuseGi;
        directShadows = directShadows == null
                ? DenoiseSignalInputContract.directShadows(false, 0L, 0, 0, 0, 0, "direct shadow signal not supplied")
                : directShadows;
        specular = normalizeOptional(specular, DenoiseSignalKind.SPECULAR_PLACEHOLDER);
        ambientOcclusion = normalizeOptional(ambientOcclusion, DenoiseSignalKind.AMBIENT_OCCLUSION_PLACEHOLDER);
        edgeRejectionInputs = edgeRejectionInputs == null
                ? DenoiseEdgeRejectionInputs.unavailable("edge rejection inputs not supplied")
                : edgeRejectionInputs;
        historyCounters = historyCounters == null ? DenoiseHistoryCounters.none() : historyCounters;
        output = output == null
                ? DenoiseOutputContract.disabled("denoise output contract not supplied")
                : output;
        readinessReason = readinessReason == null || readinessReason.isBlank()
                ? defaultReason(enabled, diffuseGi, directShadows, edgeRejectionInputs)
                : readinessReason;
    }

    public static SignalSeparatedDenoiseContract disabled(String reason) {
        return new SignalSeparatedDenoiseContract(
                0L,
                0L,
                0,
                0,
                false,
                null,
                null,
                null,
                null,
                DenoiseEdgeRejectionInputs.unavailable(reason),
                DenoiseHistoryCounters.none(),
                DenoiseOutputContract.disabled(reason),
                reason
        );
    }

    public List<DenoiseSignalInputContract> signals() {
        return List.of(this.diffuseGi, this.directShadows, this.specular, this.ambientOcclusion);
    }

    public boolean requiredSignalsAvailable() {
        return this.diffuseGi.readyForDenoise() && this.directShadows.readyForDenoise();
    }

    public boolean readyForScheduling() {
        return this.enabled
                && this.width > 0
                && this.height > 0
                && this.requiredSignalsAvailable()
                && this.edgeRejectionInputs.readyForEdgeAwareDenoise();
    }

    public long maxInputGeneration() {
        return Math.max(
                Math.max(this.diffuseGi.generation(), this.directShadows.generation()),
                Math.max(this.edgeRejectionInputs.gBufferGeneration(), this.edgeRejectionInputs.historyGeneration())
        );
    }

    public String debugSummary() {
        return "signalSeparatedDenoise enabled=" + this.enabled
                + " ready=" + this.readyForScheduling()
                + " generation=" + this.generation
                + " size=" + this.width + "x" + this.height
                + " diffuseGi={" + this.diffuseGi.statusLabel() + "}"
                + " directShadows={" + this.directShadows.statusLabel() + "}"
                + " edgeInputs=" + this.edgeRejectionInputs.readyForEdgeAwareDenoise()
                + " historyAccepted=" + this.historyCounters.acceptedPixels()
                + " historyRejected=" + this.historyCounters.rejectedPixels()
                + " outputs=" + this.output.hasWritableOutput()
                + " reason=" + this.readinessReason;
    }

    private static DenoiseSignalInputContract normalizeOptional(
            DenoiseSignalInputContract signal,
            DenoiseSignalKind fallbackKind
    ) {
        if (signal == null) {
            return DenoiseSignalInputContract.optionalPlaceholder(
                    Objects.requireNonNull(fallbackKind, "fallbackKind"),
                    "optional signal placeholder not populated"
            );
        }
        return signal;
    }

    private static String defaultReason(
            boolean enabled,
            DenoiseSignalInputContract diffuseGi,
            DenoiseSignalInputContract directShadows,
            DenoiseEdgeRejectionInputs edgeInputs
    ) {
        if (!enabled) {
            return "signal-separated denoise disabled";
        }
        if (!diffuseGi.readyForDenoise()) {
            return "diffuse GI signal is not ready for denoise";
        }
        if (!directShadows.readyForDenoise()) {
            return "direct shadow signal is not ready for denoise";
        }
        if (!edgeInputs.readyForEdgeAwareDenoise()) {
            return edgeInputs.readinessReason();
        }
        return "signal-separated denoise contract ready for scheduling";
    }
}
