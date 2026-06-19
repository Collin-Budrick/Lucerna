package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record DiffuseGiValidationFinding(
        DiffuseGiValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public DiffuseGiValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static DiffuseGiValidationFinding error(String code, String location, String message) {
        return new DiffuseGiValidationFinding(DiffuseGiValidationSeverity.ERROR, code, location, message);
    }

    public static DiffuseGiValidationFinding warning(String code, String location, String message) {
        return new DiffuseGiValidationFinding(DiffuseGiValidationSeverity.WARNING, code, location, message);
    }

    public static DiffuseGiValidationFinding info(String code, String location, String message) {
        return new DiffuseGiValidationFinding(DiffuseGiValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == DiffuseGiValidationSeverity.ERROR;
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
