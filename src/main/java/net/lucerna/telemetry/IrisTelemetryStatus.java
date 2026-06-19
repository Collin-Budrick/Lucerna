package net.lucerna.telemetry;

import net.lucerna.compat.iris.IrisCompatStatus;
import net.lucerna.compat.iris.IrisCompatStatus.ShaderPackState;

public record IrisTelemetryStatus(
        boolean statusAvailable,
        boolean installed,
        boolean disableAttempted,
        boolean shadersDisabledForLucerna,
        ShaderPackState shaderPackState,
        String message
) {
    public IrisTelemetryStatus {
        if (shaderPackState == null) {
            shaderPackState = installed ? ShaderPackState.UNKNOWN : ShaderPackState.NOT_INSTALLED;
        }
        message = clean(message, "Iris status has not been reported.");
    }

    public static IrisTelemetryStatus from(IrisCompatStatus status) {
        if (status == null) {
            return unavailable("Iris status has not been reported.");
        }
        return new IrisTelemetryStatus(
                true,
                status.installed(),
                status.disableAttempted(),
                status.shadersDisabledForLucerna(),
                status.shaderPackState(),
                status.userMessage()
        );
    }

    public static IrisTelemetryStatus unavailable(String reason) {
        return new IrisTelemetryStatus(
                false,
                false,
                false,
                false,
                ShaderPackState.UNKNOWN,
                reason
        );
    }

    public String stateLabel() {
        if (!this.statusAvailable) {
            return "unreported";
        }
        if (!this.installed) {
            return "not installed";
        }
        if (this.shadersDisabledForLucerna) {
            return "disabled for Lucerna";
        }
        if (this.disableAttempted) {
            return "disable attempted";
        }
        return "pending disable";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
