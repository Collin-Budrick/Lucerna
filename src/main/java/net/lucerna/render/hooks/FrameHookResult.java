package net.lucerna.render.hooks;

public record FrameHookResult(
        FrameHookEvent event,
        boolean accepted,
        boolean nativeCallIssued,
        long frameIndex,
        FramePassIntent passIntent,
        String message
) {
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
    }

    public static FrameHookResult accepted(
            FrameHookEvent event,
            boolean nativeCallIssued,
            long frameIndex,
            FramePassIntent passIntent,
            String message
    ) {
        return new FrameHookResult(event, true, nativeCallIssued, frameIndex, passIntent, message);
    }

    public static FrameHookResult skipped(long frameIndex, FramePassIntent passIntent, String message) {
        return new FrameHookResult(FrameHookEvent.SKIPPED, false, false, frameIndex, passIntent, message);
    }

    public static FrameHookResult failedNative(long frameIndex, FramePassIntent passIntent, String message) {
        return new FrameHookResult(FrameHookEvent.SKIPPED, false, true, frameIndex, passIntent, message);
    }
}
