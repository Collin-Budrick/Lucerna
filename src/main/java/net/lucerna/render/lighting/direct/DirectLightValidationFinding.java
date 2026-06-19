package net.lucerna.render.lighting.direct;

import java.util.Objects;

public record DirectLightValidationFinding(
        DirectLightValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public DirectLightValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static DirectLightValidationFinding error(String code, String location, String message) {
        return new DirectLightValidationFinding(DirectLightValidationSeverity.ERROR, code, location, message);
    }

    public static DirectLightValidationFinding warning(String code, String location, String message) {
        return new DirectLightValidationFinding(DirectLightValidationSeverity.WARNING, code, location, message);
    }

    public static DirectLightValidationFinding info(String code, String location, String message) {
        return new DirectLightValidationFinding(DirectLightValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == DirectLightValidationSeverity.ERROR;
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
