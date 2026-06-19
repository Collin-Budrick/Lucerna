package net.lucerna.render.hooks;

import net.lucerna.render.context.BorrowedVulkanContextAcquisition;
import net.lucerna.render.context.BorrowedVulkanContextAcquisitionStatus;

public record FrameLifecycleSnapshot(
        long frameIndex,
        FrameHookStage stage,
        FramePassIntent passIntent,
        int viewportWidth,
        int viewportHeight,
        boolean resizePending,
        boolean frameOpen,
        boolean lightingSubmitted,
        BorrowedVulkanContextAcquisition contextAcquisition,
        String lastMessage
) {
    public FrameLifecycleSnapshot(
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
        this(
                frameIndex,
                stage,
                passIntent,
                viewportWidth,
                viewportHeight,
                resizePending,
                frameOpen,
                lightingSubmitted,
                BorrowedVulkanContextAcquisition.absent("Frame context acquisition has not been reported."),
                lastMessage
        );
    }

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
        if (contextAcquisition == null) {
            contextAcquisition = BorrowedVulkanContextAcquisition.absent(
                    "Frame context acquisition has not been reported."
            );
        }
    }

    public boolean hasViewport() {
        return this.viewportWidth > 0 && this.viewportHeight > 0;
    }

    public BorrowedVulkanContextAcquisitionStatus contextStatus() {
        return this.contextAcquisition.status();
    }

    public boolean contextReady() {
        return this.contextAcquisition.ready();
    }

    public String contextMessage() {
        return this.contextAcquisition.message();
    }
}
