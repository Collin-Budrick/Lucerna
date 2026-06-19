package net.lucerna.telemetry;

import net.lucerna.compat.BackendKind;
import net.lucerna.compat.BackendStatus;

public record BackendTelemetryStatus(
        BackendKind kind,
        boolean active,
        String backendName,
        String message
) {
    public BackendTelemetryStatus {
        if (kind == null) {
            kind = BackendKind.UNKNOWN;
        }
        backendName = clean(backendName, "unknown");
        message = clean(message, "Backend status has not been reported.");
    }

    public static BackendTelemetryStatus from(BackendStatus status) {
        if (status == null) {
            return new BackendTelemetryStatus(
                    BackendKind.UNKNOWN,
                    false,
                    "unknown",
                    "Backend status has not been reported."
            );
        }
        return new BackendTelemetryStatus(
                status.kind(),
                status.active(),
                status.backendName(),
                status.userMessage()
        );
    }

    public String label() {
        return this.backendName + " (" + this.kind.name() + ")";
    }

    public String activeLabel() {
        return this.active ? "active" : "inactive";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
