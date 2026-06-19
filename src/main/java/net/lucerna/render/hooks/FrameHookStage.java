package net.lucerna.render.hooks;

public enum FrameHookStage {
    IDLE,
    RESIZE_PENDING,
    RESIZE_SUBMITTED,
    FRAME_ACTIVE,
    LIGHTING_SUBMITTED,
    FRAME_COMPLETE,
    SKIPPED
}
