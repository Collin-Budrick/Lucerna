package net.lucerna.render.hooks;

public enum FramePassIntent {
    NONE("No Lucerna Vulkan pass is requested."),
    NO_OP_LIGHTING_PASS("Issue Lucerna's native lighting hook as a no-op pass placeholder."),
    NO_OP_FRAME_ATTACHMENT_PASS("Attach Lucerna's Java-side no-op pass placeholder to a borrowed frame target."),
    FLAT_COMPOSITE_PASS("Attach Lucerna's Java-side flat composite placeholder to a borrowed frame target.");

    private final String description;

    FramePassIntent(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
