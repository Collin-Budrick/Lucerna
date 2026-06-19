package net.lucerna.render.context;

import net.lucerna.compat.BackendStatus;

public record VulkanFrameContextRequest(
        long frameIndex,
        float tickDelta,
        int viewportWidth,
        int viewportHeight,
        BackendStatus backendStatus
) {
    public VulkanFrameContextRequest {
        frameIndex = Math.max(0L, frameIndex);
        if (!Float.isFinite(tickDelta)) {
            tickDelta = 0.0F;
        }
        viewportWidth = Math.max(0, viewportWidth);
        viewportHeight = Math.max(0, viewportHeight);
    }

    public boolean hasViewport() {
        return this.viewportWidth > 0 && this.viewportHeight > 0;
    }
}
