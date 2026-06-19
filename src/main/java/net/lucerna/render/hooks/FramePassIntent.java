package net.lucerna.render.hooks;

public enum FramePassIntent {
    NONE("No Lucerna Vulkan pass is requested."),
    NO_OP_LIGHTING_PASS("Issue Lucerna's native lighting hook as a no-op pass placeholder.");

    private final String description;

    FramePassIntent(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
