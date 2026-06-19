package net.lucerna.config;

public enum QualityPreset {
    PERFORMANCE("Performance"),
    BALANCED("Balanced"),
    QUALITY("Quality"),
    EXPERIMENTAL("Experimental");

    private final String displayName;

    QualityPreset(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    public QualityPreset next() {
        QualityPreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static QualityPreset fromSerializedName(String name, QualityPreset fallback) {
        if (name == null) {
            return fallback;
        }
        for (QualityPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return fallback;
    }
}
