package net.lucerna.render.tracing.hybrid;

public enum HybridHitSource {
    SCREEN_SPACE("screen-space", "screenSpace", 30, true),
    VOXEL("voxel", "voxel", 40, true),
    HARDWARE_RT("hardware-rt", "hardwareRt", 50, true),
    SKY("sky", "sky", 10, false),
    MISS("miss", "miss", 0, false);

    private final String displayName;
    private final String telemetryKey;
    private final int priority;
    private final boolean surfaceSource;

    HybridHitSource(String displayName, String telemetryKey, int priority, boolean surfaceSource) {
        this.displayName = displayName;
        this.telemetryKey = telemetryKey;
        this.priority = priority;
        this.surfaceSource = surfaceSource;
    }

    public String displayName() {
        return this.displayName;
    }

    public String telemetryKey() {
        return this.telemetryKey;
    }

    public int priority() {
        return this.priority;
    }

    public boolean surfaceSource() {
        return this.surfaceSource;
    }

    public static HybridHitSource fromTelemetry(String value) {
        if (value == null || value.isBlank()) {
            return MISS;
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(java.util.Locale.ROOT);
        for (HybridHitSource source : values()) {
            if (source.name().equals(normalized)
                    || source.telemetryKey.equalsIgnoreCase(value)
                    || source.displayName.equalsIgnoreCase(value)) {
                return source;
            }
        }
        return MISS;
    }
}
