package net.lucerna.config;

public enum Round7VisualMode {
    BASELINE("baseline", "round7.visual.baseline.base_world_color_only"),
    RAW_GI("raw-gi", "round7.visual.raw_gi.native_diffuse_gi"),
    DENOISED_GI("denoised-gi", "round7.visual.denoised_gi.cpu_denoised_diffuse_gi"),
    FINAL_COMPOSITE("final-composite", "round7.visual.final_composite.existing_final_path");

    private final String stableId;
    private final String evidenceKey;

    Round7VisualMode(String stableId, String evidenceKey) {
        this.stableId = stableId;
        this.evidenceKey = evidenceKey;
    }

    public String stableId() {
        return this.stableId;
    }

    public String evidenceKey() {
        return this.evidenceKey;
    }

    public boolean baseline() {
        return this == BASELINE;
    }

    public boolean rawGi() {
        return this == RAW_GI;
    }

    public boolean denoisedGi() {
        return this == DENOISED_GI;
    }

    public boolean finalComposite() {
        return this == FINAL_COMPOSITE;
    }
}
