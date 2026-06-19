package net.lucerna.render.pass;

public enum LucernaFramePassPhase {
    UNKNOWN(false, false, "Unknown frame-pass phase."),
    WORLD_COLOR_BEFORE_HUD(true, true, "World color target before vanilla HUD/translucent overlays."),
    POST_HUD(false, false, "Post-HUD target; not safe for Lucerna world lighting composite.");

    private final boolean worldColorTarget;
    private final boolean hudPreserving;
    private final String description;

    LucernaFramePassPhase(boolean worldColorTarget, boolean hudPreserving, String description) {
        this.worldColorTarget = worldColorTarget;
        this.hudPreserving = hudPreserving;
        this.description = description;
    }

    public boolean worldColorTarget() {
        return this.worldColorTarget;
    }

    public boolean hudPreserving() {
        return this.hudPreserving;
    }

    public boolean safeForLightingComposite() {
        return this.worldColorTarget && this.hudPreserving;
    }

    public String description() {
        return this.description;
    }
}
