package net.lucerna.render.pass;

public enum LucernaFramePassStatusCode {
    NOT_REQUESTED("No Lucerna frame pass attachment has been requested."),
    WAITING_FOR_FRAME("A Lucerna frame pass is waiting for an active frame."),
    WAITING_FOR_CONTEXT("A Lucerna frame pass is waiting for a ready borrowed Vulkan context."),
    WAITING_FOR_TARGET("A Lucerna frame pass is waiting for a safe frame target."),
    TARGET_UNSAFE("A Lucerna frame pass target was supplied but was not marked safe."),
    READY_TO_ATTACH("A Lucerna frame pass has the required frame, context, and target state."),
    ATTACHED_NO_OP("A Lucerna frame pass target was accepted; no draw calls were issued."),
    FRAME_CLOSED("The active frame closed before a Lucerna frame pass issued draw calls."),
    SKIPPED("A Lucerna frame pass attachment was skipped.");

    private final String description;

    LucernaFramePassStatusCode(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
