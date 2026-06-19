package net.lucerna.render.gbuffer;

import java.util.Objects;

public record GBufferWriteIntentValidationFinding(
        GBufferWriteIntentValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public GBufferWriteIntentValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static GBufferWriteIntentValidationFinding error(String code, String location, String message) {
        return new GBufferWriteIntentValidationFinding(GBufferWriteIntentValidationSeverity.ERROR, code, location, message);
    }

    public static GBufferWriteIntentValidationFinding warning(String code, String location, String message) {
        return new GBufferWriteIntentValidationFinding(GBufferWriteIntentValidationSeverity.WARNING, code, location, message);
    }

    public static GBufferWriteIntentValidationFinding info(String code, String location, String message) {
        return new GBufferWriteIntentValidationFinding(GBufferWriteIntentValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == GBufferWriteIntentValidationSeverity.ERROR;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalizeLocation(String value) {
        if (value == null || value.isBlank()) {
            return "$";
        }
        return value.trim();
    }
}
