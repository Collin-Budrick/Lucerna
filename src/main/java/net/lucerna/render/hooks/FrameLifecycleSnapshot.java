package net.lucerna.render.hooks;

public record FrameLifecycleSnapshot(
        long frameIndex,
        FrameHookStage stage,
        FramePassIntent passIntent,
        int viewportWidth,
        int viewportHeight,
        boolean resizePending,
        boolean frameOpen,
        boolean lightingSubmitted,
        String lastMessage
) {
    public FrameLifecycleSnapshot {
        if (stage == null) {
            stage = FrameHookStage.IDLE;
        }
        if (passIntent == null) {
            passIntent = FramePassIntent.NONE;
        }
        if (lastMessage == null || lastMessage.isBlank()) {
            lastMessage = "Frame lifecycle has not started.";
        } else {
            lastMessage = lastMessage.trim();
        }
    }

    public boolean hasViewport() {
        return this.viewportWidth > 0 && this.viewportHeight > 0;
    }
}
