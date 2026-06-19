package net.lucerna.config;

public enum CompositeMode {
    BASE_VANILLA_ONLY(
            "Base / vanilla only",
            "base-vanilla-only",
            true,
            false,
            false,
            "vanilla/base world color only; Lucerna lighting composite is bypassed"
    ),
    DIRECT_ONLY(
            "Direct only",
            "direct-only",
            false,
            true,
            false,
            "direct-light contribution only; diffuse GI is excluded"
    ),
    GI_ONLY(
            "GI only",
            "gi-only",
            false,
            false,
            true,
            "diffuse GI contribution only; direct light is excluded"
    ),
    FINAL_LUCERNA_COMPOSITE(
            "Final Lucerna composite",
            "final-lucerna-composite",
            true,
            true,
            true,
            "base world color plus Lucerna direct and diffuse GI contributions"
    );

    private final String displayName;
    private final String statusKey;
    private final boolean baseWorldColorEnabled;
    private final boolean directLightingEnabled;
    private final boolean diffuseGiEnabled;
    private final String statusDescription;

    CompositeMode(
            String displayName,
            String statusKey,
            boolean baseWorldColorEnabled,
            boolean directLightingEnabled,
            boolean diffuseGiEnabled,
            String statusDescription
    ) {
        this.displayName = displayName;
        this.statusKey = statusKey;
        this.baseWorldColorEnabled = baseWorldColorEnabled;
        this.directLightingEnabled = directLightingEnabled;
        this.diffuseGiEnabled = diffuseGiEnabled;
        this.statusDescription = statusDescription;
    }

    public String displayName() {
        return this.displayName;
    }

    public String statusKey() {
        return this.statusKey;
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

    public String summary() {
        return "mode=" + this.statusKey
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
