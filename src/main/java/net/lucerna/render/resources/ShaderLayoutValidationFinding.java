package net.lucerna.render.resources;

import java.util.Objects;

public record ShaderLayoutValidationFinding(
        ShaderLayoutValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public ShaderLayoutValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static ShaderLayoutValidationFinding error(String code, String location, String message) {
        return new ShaderLayoutValidationFinding(ShaderLayoutValidationSeverity.ERROR, code, location, message);
    }

    public static ShaderLayoutValidationFinding warning(String code, String location, String message) {
        return new ShaderLayoutValidationFinding(ShaderLayoutValidationSeverity.WARNING, code, location, message);
    }

    public static ShaderLayoutValidationFinding info(String code, String location, String message) {
        return new ShaderLayoutValidationFinding(ShaderLayoutValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == ShaderLayoutValidationSeverity.ERROR;
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
