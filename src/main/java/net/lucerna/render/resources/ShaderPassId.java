package net.lucerna.render.resources;

import java.util.Objects;
import java.util.regex.Pattern;

public record ShaderPassId(String value) {
    private static final Pattern LAYOUT_FORMAT =
            Pattern.compile("lucerna\\.[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*");

    public ShaderPassId {
        value = requireText(value, "value");
    }

    public static ShaderPassId of(String value) {
        return new ShaderPassId(value);
    }

    public boolean matchesLayoutFormat() {
        return LAYOUT_FORMAT.matcher(this.value).matches();
    }

    public boolean matchesStage(String stage) {
        Objects.requireNonNull(stage, "stage");
        return this.stage().equals(stage);
    }

    public String stage() {
        String[] parts = this.value.split("\\.", 3);
        return parts.length >= 3 ? parts[1] : "";
    }

    public String name() {
        String[] parts = this.value.split("\\.", 3);
        return parts.length >= 3 ? parts[2] : this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
