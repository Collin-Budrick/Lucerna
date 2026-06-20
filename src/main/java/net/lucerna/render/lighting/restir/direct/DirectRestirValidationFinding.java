package net.lucerna.render.lighting.restir.direct;

import java.util.Objects;

public record DirectRestirValidationFinding(
        DirectRestirValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public DirectRestirValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static DirectRestirValidationFinding error(String code, String location, String message) {
        return new DirectRestirValidationFinding(DirectRestirValidationSeverity.ERROR, code, location, message);
    }

    public static DirectRestirValidationFinding warning(String code, String location, String message) {
        return new DirectRestirValidationFinding(DirectRestirValidationSeverity.WARNING, code, location, message);
    }

    public static DirectRestirValidationFinding info(String code, String location, String message) {
        return new DirectRestirValidationFinding(DirectRestirValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == DirectRestirValidationSeverity.ERROR;
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
