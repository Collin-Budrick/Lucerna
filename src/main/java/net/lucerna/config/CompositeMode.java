package net.lucerna.config;

public enum CompositeMode {
    BASE_VANILLA_ONLY(
            "Base / vanilla only",
            "base-vanilla-only",
            "round7.composite.baseline.base_world_color_only",
            true,
            false,
            false,
            "vanilla/base world color only; Lucerna lighting composite is bypassed",
            "baseline screenshot/control frame with Lucerna direct and diffuse GI excluded",
            "Use this mode to prove the vanilla/base target is preserved before Lucerna lighting is blended."
    ),
    DIRECT_ONLY(
            "Direct only",
            "direct-only",
            "round7.composite.isolated.direct_only",
            false,
            true,
            false,
            "direct-light contribution only; diffuse GI is excluded",
            "direct-only screenshot/debug frame showing direct lighting without diffuse GI",
            "Use this mode to isolate direct-light output for before/after evidence."
    ),
    GI_ONLY(
            "GI only",
            "gi-only",
            "round7.composite.isolated.diffuse_gi_only",
            false,
            false,
            true,
            "diffuse GI contribution only; direct light is excluded",
            "GI-only screenshot/debug frame showing diffuse GI without direct light",
            "Use this mode to isolate diffuse GI output for raw-vs-denoised comparison."
    ),
    FINAL_LUCERNA_COMPOSITE(
            "Final Lucerna composite",
            "final-lucerna-composite",
            "round7.composite.final.base_direct_gi",
            true,
            true,
            true,
            "base world color plus Lucerna direct and diffuse GI contributions",
            "final screenshot with base world color, direct light, and diffuse GI combined",
            "Use this mode to prove the final composite path preserves HUD/vanilla rendering while adding Lucerna lighting."
    );

    private final String displayName;
    private final String statusKey;
    private final String evidenceKey;
    private final boolean baseWorldColorEnabled;
    private final boolean directLightingEnabled;
    private final boolean diffuseGiEnabled;
    private final String statusDescription;
    private final String expectedEvidence;
    private final String modeReason;

    CompositeMode(
            String displayName,
            String statusKey,
            String evidenceKey,
            boolean baseWorldColorEnabled,
            boolean directLightingEnabled,
            boolean diffuseGiEnabled,
            String statusDescription,
            String expectedEvidence,
            String modeReason
    ) {
        this.displayName = displayName;
        this.statusKey = statusKey;
        this.evidenceKey = evidenceKey;
        this.baseWorldColorEnabled = baseWorldColorEnabled;
        this.directLightingEnabled = directLightingEnabled;
        this.diffuseGiEnabled = diffuseGiEnabled;
        this.statusDescription = statusDescription;
        this.expectedEvidence = expectedEvidence;
        this.modeReason = modeReason;
    }

    public String displayName() {
        return this.displayName;
    }

    public String statusKey() {
        return this.statusKey;
    }

    public String evidenceKey() {
        return this.evidenceKey;
    }

    public boolean baseWorldColorEnabled() {
        return this.baseWorldColorEnabled;
    }

    public boolean directLightingEnabled() {
        return this.directLightingEnabled;
    }

    public boolean diffuseGiEnabled() {
        return this.diffuseGiEnabled;
    }

    public boolean lucernaLightingEnabled() {
        return this.directLightingEnabled || this.diffuseGiEnabled;
    }

    public String statusDescription() {
        return this.statusDescription;
    }

    public String expectedEvidence() {
        return this.expectedEvidence;
    }

    public String modeReason() {
        return this.modeReason;
    }

    public boolean baselineOnly() {
        return this.baseWorldColorEnabled && !this.directLightingEnabled && !this.diffuseGiEnabled;
    }

    public boolean isolatedLucernaSignal() {
        return !this.baseWorldColorEnabled && this.directLightingEnabled != this.diffuseGiEnabled;
    }

    public String summary() {
        return "mode=" + this.statusKey
                + ",evidenceKey=" + this.evidenceKey
                + ",baseWorldColor=" + this.baseWorldColorEnabled
                + ",directLighting=" + this.directLightingEnabled
                + ",diffuseGi=" + this.diffuseGiEnabled
                + ",lucernaLighting=" + this.lucernaLightingEnabled();
    }

    public CompositeMode next() {
        CompositeMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static CompositeMode fromSerializedName(String name, CompositeMode fallback) {
        if (name == null) {
            return fallback;
        }
        for (CompositeMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name) || mode.statusKey.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return fallback;
    }
}
