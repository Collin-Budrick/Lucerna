package net.lucerna.render.voxel;

import java.util.Objects;

public record VoxelTraversalValidationFinding(
        VoxelTraversalValidationSeverity severity,
        String code,
        String location,
        String message
) {
    public VoxelTraversalValidationFinding {
        Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        location = normalizeLocation(location);
        message = requireText(message, "message");
    }

    public static VoxelTraversalValidationFinding error(String code, String location, String message) {
        return new VoxelTraversalValidationFinding(VoxelTraversalValidationSeverity.ERROR, code, location, message);
    }

    public static VoxelTraversalValidationFinding warning(String code, String location, String message) {
        return new VoxelTraversalValidationFinding(VoxelTraversalValidationSeverity.WARNING, code, location, message);
    }

    public static VoxelTraversalValidationFinding info(String code, String location, String message) {
        return new VoxelTraversalValidationFinding(VoxelTraversalValidationSeverity.INFO, code, location, message);
    }

    public boolean error() {
        return this.severity == VoxelTraversalValidationSeverity.ERROR;
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
