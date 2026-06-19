package net.lucerna.config;

public enum CompositeMode {
    BASE_VANILLA_ONLY(
            "Base / vanilla only",
            "baseline",
            Round7VisualMode.BASELINE,
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
            Round7VisualMode.FINAL_COMPOSITE,
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
            Round7VisualMode.RAW_GI,
            false,
            false,
            true,
            "diffuse GI contribution only; direct light is excluded",
            "GI-only screenshot/debug frame showing diffuse GI without direct light",
            "Use this mode to isolate diffuse GI output for raw-vs-denoised comparison."
    ),
    RAW_GI(
            "Raw GI",
            "raw-gi",
            Round7VisualMode.RAW_GI,
            false,
            false,
            true,
            "raw native diffuse-GI contribution only; denoise and final blend are excluded",
            "same-scene raw GI screenshot/debug frame showing the native diffuse-GI RGBA8 source",
            "Use this mode to capture Round 7 raw GI evidence before denoise."
    ),
    DENOISED_GI(
            "Denoised GI",
            "denoised-gi",
            Round7VisualMode.DENOISED_GI,
            false,
            false,
            true,
            "CPU denoised diffuse-GI contribution only; final visual quality is not claimed",
            "same-scene denoised GI screenshot/debug frame showing the CPU denoised diffuse-GI output",
            "Use this mode to compare denoised GI against the raw GI source without claiming shader/final quality."
    ),
    FINAL_LUCERNA_COMPOSITE(
            "Final Lucerna composite",
            "final-composite",
            Round7VisualMode.FINAL_COMPOSITE,
            true,
            true,
            true,
            "base world color plus Lucerna direct and diffuse GI contributions",
            "final screenshot with base world color, direct light, and diffuse GI combined",
            "Use this mode to prove the final composite path preserves HUD/vanilla rendering while adding Lucerna lighting."
    );

    private final String displayName;
    private final String statusKey;
    private final Round7VisualMode visualMode;
    private final boolean baseWorldColorEnabled;
    private final boolean directLightingEnabled;
    private final boolean diffuseGiEnabled;
    private final String statusDescription;
    private final String expectedEvidence;
    private final String modeReason;

    CompositeMode(
            String displayName,
            String statusKey,
            Round7VisualMode visualMode,
            boolean baseWorldColorEnabled,
            boolean directLightingEnabled,
            boolean diffuseGiEnabled,
            String statusDescription,
            String expectedEvidence,
            String modeReason
    ) {
        this.displayName = displayName;
        this.statusKey = statusKey;
        this.visualMode = visualMode == null ? Round7VisualMode.FINAL_COMPOSITE : visualMode;
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
        return this.visualMode.evidenceKey();
    }

    public Round7VisualMode visualMode() {
        return this.visualMode;
    }

    public String visualModeId() {
        return this.visualMode.stableId();
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
                + ",visualMode=" + this.visualMode.stableId()
                + ",evidenceKey=" + this.evidenceKey()
                + ",baseWorldColor=" + this.baseWorldColorEnabled
                + ",directLighting=" + this.directLightingEnabled
                + ",diffuseGi=" + this.diffuseGiEnabled
                + ",lucernaLighting=" + this.lucernaLightingEnabled();
    }

    public CompositeMode next() {
        CompositeMode[] modes = controllerSelectableModes();
        for (int index = 0; index < modes.length; index++) {
            if (modes[index] == this) {
                return modes[(index + 1) % modes.length];
            }
        }
        return modes[0];
    }

    public static CompositeMode[] controllerSelectableModes() {
        return new CompositeMode[]{
                BASE_VANILLA_ONLY,
                RAW_GI,
                DENOISED_GI,
                FINAL_LUCERNA_COMPOSITE
        };
    }

    public static CompositeMode fromSerializedName(String name, CompositeMode fallback) {
        if (name == null) {
            return fallback;
        }
        for (CompositeMode mode : values()) {
            if (mode.matchesSerializedName(name)) {
                return mode;
            }
        }
        return fallback;
    }

    private boolean matchesSerializedName(String name) {
        if (this.name().equalsIgnoreCase(name) || this.statusKey.equalsIgnoreCase(name)) {
            return true;
        }
        return switch (this) {
            case BASE_VANILLA_ONLY -> "base-vanilla-only".equalsIgnoreCase(name);
            case RAW_GI -> "round7-raw-gi".equalsIgnoreCase(name);
            case DENOISED_GI -> "round7-denoised-gi".equalsIgnoreCase(name);
            case FINAL_LUCERNA_COMPOSITE -> "final-lucerna-composite".equalsIgnoreCase(name)
                    || "round7-final-composite".equalsIgnoreCase(name);
            case DIRECT_ONLY, GI_ONLY -> false;
        };
    }
}
