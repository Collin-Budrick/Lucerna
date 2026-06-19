package net.lucerna.render.lighting.post;

import java.util.Objects;

public record PostProcessingValidationFinding(
        PostProcessingValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public PostProcessingValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static PostProcessingValidationFinding error(String code, String location, String message) {
        return new PostProcessingValidationFinding(PostProcessingValidationSeverity.ERROR, code, location, message);
    }

    public static PostProcessingValidationFinding warning(String code, String location, String message) {
        return new PostProcessingValidationFinding(PostProcessingValidationSeverity.WARNING, code, location, message);
    }

    public static PostProcessingValidationFinding info(String code, String location, String message) {
        return new PostProcessingValidationFinding(PostProcessingValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == PostProcessingValidationSeverity.ERROR;
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
