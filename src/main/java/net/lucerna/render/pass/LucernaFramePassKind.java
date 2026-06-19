package net.lucerna.render.pass;

public enum LucernaFramePassKind {
    NO_OP("No-op frame attachment placeholder."),
    FLAT_COMPOSITE("Flat-color composite placeholder."),
    DIRECT_LIGHT_PREVIEW_COMPOSITE("Direct-light preview composite request.");

    private final String description;

    LucernaFramePassKind(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
