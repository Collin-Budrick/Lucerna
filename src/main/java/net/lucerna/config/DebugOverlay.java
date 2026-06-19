package net.lucerna.config;

public enum DebugOverlay {
    OFF("Off"),
    BACKEND("Backend"),
    DIRTY_REGIONS("Dirty regions"),
    MATERIAL_IDS("Material ids"),
    FRAME_TIMINGS("Frame timings"),
    DIRECT_LIGHTING("Direct lighting"),
    NATIVE_QUEUE("Native queue");

    private final String displayName;

    DebugOverlay(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    public DebugOverlay next() {
        DebugOverlay[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static DebugOverlay fromSerializedName(String name, DebugOverlay fallback) {
        if (name == null) {
            return fallback;
        }
        for (DebugOverlay overlay : values()) {
            if (overlay.name().equalsIgnoreCase(name)) {
                return overlay;
            }
        }
        return fallback;
    }
}
