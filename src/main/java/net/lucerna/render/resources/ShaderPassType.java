package net.lucerna.render.resources;

import java.util.Locale;

public enum ShaderPassType {
    GRAPHICS,
    COMPUTE,
    UNKNOWN;

    public static ShaderPassType fromLayoutValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "graphics" -> GRAPHICS;
            case "compute" -> COMPUTE;
            default -> UNKNOWN;
        };
    }

    public String layoutValue() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
