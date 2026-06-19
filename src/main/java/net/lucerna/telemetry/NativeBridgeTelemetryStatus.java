package net.lucerna.telemetry;

import net.lucerna.nativebridge.LucernaNativeBridge.NativeBridgeStatus;

public record NativeBridgeTelemetryStatus(
        boolean statusAvailable,
        boolean loadAttempted,
        boolean loaded,
        boolean available,
        boolean initialized,
        String lastError,
        String nativeStatus
) {
    public NativeBridgeTelemetryStatus {
        lastError = blankToEmpty(lastError);
        nativeStatus = clean(nativeStatus, "native status unavailable");
    }

    public static NativeBridgeTelemetryStatus from(NativeBridgeStatus status) {
        if (status == null) {
            return unavailable("Native bridge status has not been reported.");
        }
        return new NativeBridgeTelemetryStatus(
                true,
                status.loadAttempted(),
                status.loaded(),
                status.available(),
                status.initialized(),
                status.lastError(),
                status.nativeStatus()
        );
    }

    public static NativeBridgeTelemetryStatus unavailable(String reason) {
        return new NativeBridgeTelemetryStatus(
                false,
                false,
                false,
                false,
                false,
                reason,
                "native status unavailable"
        );
    }

    public boolean operational() {
        return this.loaded && this.available && this.initialized;
    }

    public String stateLabel() {
        if (!this.statusAvailable) {
            return "unreported";
        }
        if (this.operational()) {
            return "operational";
        }
        if (!this.loadAttempted) {
            return "not loaded";
        }
        if (!this.loaded) {
            return "load failed";
        }
        if (!this.available) {
            return "unavailable";
        }
        return this.initialized ? "initialized" : "loaded";
    }

    public String diagnosticMessage() {
        if (!this.lastError.isBlank()) {
            return this.lastError;
        }
        return this.nativeStatus;
    }

    public NativePassTelemetryStatus nativePassStates() {
        if (!this.statusAvailable) {
            return NativePassTelemetryStatus.unavailable("Native bridge status has not been reported.");
        }
        return NativePassTelemetryStatus.fromNativeStatus(this.nativeStatus);
    }

    private static String blankToEmpty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
