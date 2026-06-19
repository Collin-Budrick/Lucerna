package net.lucerna.render.hooks;

import net.lucerna.render.context.BorrowedVulkanContextAcquisition;
import net.lucerna.render.context.BorrowedVulkanContextAcquisitionStatus;

public record FrameHookResult(
        FrameHookEvent event,
        boolean accepted,
        boolean nativeCallIssued,
        long frameIndex,
        FramePassIntent passIntent,
        String message,
        BorrowedVulkanContextAcquisition contextAcquisition
) {
    public FrameHookResult(
            FrameHookEvent event,
            boolean accepted,
            boolean nativeCallIssued,
            long frameIndex,
            FramePassIntent passIntent,
            String message
    ) {
        this(
                event,
                accepted,
                nativeCallIssued,
                frameIndex,
                passIntent,
                message,
                BorrowedVulkanContextAcquisition.absent("Frame context acquisition has not been reported.")
        );
    }

    public FrameHookResult {
        if (event == null) {
            event = FrameHookEvent.SKIPPED;
        }
        if (passIntent == null) {
            passIntent = FramePassIntent.NONE;
        }
        if (message == null || message.isBlank()) {
            message = "No frame hook message was reported.";
        } else {
            message = message.trim();
        }
        if (contextAcquisition == null) {
            contextAcquisition = BorrowedVulkanContextAcquisition.absent(
                    "Frame context acquisition has not been reported."
            );
        }
    }

    public static FrameHookResult accepted(
            FrameHookEvent event,
            boolean nativeCallIssued,
            long frameIndex,
            FramePassIntent passIntent,
            String message
    ) {
        return accepted(event, nativeCallIssued, frameIndex, passIntent, message, null);
    }

    public static FrameHookResult accepted(
            FrameHookEvent event,
            boolean nativeCallIssued,
            long frameIndex,
            FramePassIntent passIntent,
            String message,
            BorrowedVulkanContextAcquisition contextAcquisition
    ) {
        return new FrameHookResult(event, true, nativeCallIssued, frameIndex, passIntent, message, contextAcquisition);
    }

    public static FrameHookResult skipped(long frameIndex, FramePassIntent passIntent, String message) {
        return skipped(frameIndex, passIntent, message, null);
    }

    public static FrameHookResult skipped(
            long frameIndex,
            FramePassIntent passIntent,
            String message,
            BorrowedVulkanContextAcquisition contextAcquisition
    ) {
        return new FrameHookResult(
                FrameHookEvent.SKIPPED,
                false,
                false,
                frameIndex,
                passIntent,
                message,
                contextAcquisition
        );
    }

    public static FrameHookResult failedNative(long frameIndex, FramePassIntent passIntent, String message) {
        return failedNative(frameIndex, passIntent, message, null);
    }

    public static FrameHookResult failedNative(
            long frameIndex,
            FramePassIntent passIntent,
            String message,
            BorrowedVulkanContextAcquisition contextAcquisition
    ) {
        return new FrameHookResult(
                FrameHookEvent.SKIPPED,
                false,
                true,
                frameIndex,
                passIntent,
                message,
                contextAcquisition
        );
    }

    public BorrowedVulkanContextAcquisitionStatus contextStatus() {
        return this.contextAcquisition.status();
    }

    public boolean contextReady() {
        return this.contextAcquisition.ready();
    }
}
