package net.lucerna.config;

public enum DebugOverlay {
    OFF("Off"),
    BACKEND("Backend"),
    DIRTY_REGIONS("Dirty regions"),
    MATERIAL_IDS("Material ids"),
    FRAME_TIMINGS("Frame timings"),
    DIRECT_LIGHTING("Direct lighting"),
    FIRST_LIGHTING_QUALITY("First-lighting quality"),
    FIRST_LIGHTING_PHYSICAL_PROOF("First-lighting physical proof"),
    SHADER_DENOISE_TEMPORAL("Shader denoise temporal proof"),
    NATIVE_QUEUE("Native queue"),
    ADAPTIVE_SAMPLING("Round 8 adaptive sampling"),
    RAY_BUDGET_HEATMAP("Round 8 ray budget heatmap"),
    VARIANCE_MAP("Round 8 variance map"),
    HISTORY_CONFIDENCE("Round 8 history confidence heatmap"),
    DISOCCLUSION_MASK("Round 8 disocclusion mask"),
    CHUNK_CULLING("Round 9 chunk culling"),
    VOXEL_RAY_DEBUG("Round 10 voxel ray debug"),
    RT_ENTITY_DEBUG("Round 10 RT entity debug"),
    HYBRID_HIT_DEBUG("Round 10 hybrid hit debug"),
    RESTIR_EXECUTION_DEBUG("Round 11 ReSTIR execution debug"),
    DIRECT_RESERVOIR_DEBUG("Round 11 direct reservoir debug"),
    GI_RESERVOIR_DEBUG("Round 11 GI reservoir debug"),
    RESERVOIR_REUSE_DEBUG("Round 11 reservoir reuse debug");

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
