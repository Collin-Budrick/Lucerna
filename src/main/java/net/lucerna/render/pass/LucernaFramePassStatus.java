package net.lucerna.render.pass;

public record LucernaFramePassStatus(
        LucernaFramePassStatusCode code,
        LucernaFramePassKind kind,
        long frameIndex,
        boolean frameActive,
        boolean contextReady,
        boolean targetAvailable,
        boolean targetSafe,
        boolean drawCallsIssued,
        String message
) {
    public LucernaFramePassStatus {
        if (code == null) {
            code = LucernaFramePassStatusCode.NOT_REQUESTED;
        }
        if (kind == null) {
            kind = LucernaFramePassKind.NO_OP;
        }
        frameIndex = Math.max(0L, frameIndex);
        if (message == null || message.isBlank()) {
            message = code.description();
        } else {
            message = message.trim();
        }
        drawCallsIssued = drawCallsIssued && canReportDrawCalls(code, kind);
    }

    public static LucernaFramePassStatus notRequested() {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.NOT_REQUESTED,
                LucernaFramePassKind.NO_OP,
                0L,
                false,
                false,
                false,
                false,
                false,
                "No Lucerna frame pass attachment has been requested."
        );
    }

    public static LucernaFramePassStatus waitingForFrame(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.WAITING_FOR_FRAME,
                kind,
                frameIndex,
                false,
                false,
                false,
                false,
                false,
                message
        );
    }

    public static LucernaFramePassStatus waitingForContext(
            LucernaFramePassKind kind,
            long frameIndex,
            boolean frameActive,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.WAITING_FOR_CONTEXT,
                kind,
                frameIndex,
                frameActive,
                false,
                false,
                false,
                false,
                message
        );
    }

    public static LucernaFramePassStatus waitingForTarget(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.WAITING_FOR_TARGET,
                kind,
                frameIndex,
                true,
                true,
                false,
                false,
                false,
                message
        );
    }

    public static LucernaFramePassStatus targetUnsafe(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.TARGET_UNSAFE,
                kind,
                frameIndex,
                true,
                true,
                true,
                false,
                false,
                message
        );
    }

    public static LucernaFramePassStatus readyToAttach(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.READY_TO_ATTACH,
                kind,
                frameIndex,
                true,
                true,
                true,
                true,
                false,
                message
        );
    }

    public static LucernaFramePassStatus attachedNoOp(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.ATTACHED_NO_OP,
                kind,
                frameIndex,
                true,
                true,
                true,
                true,
                false,
                message
        );
    }

    public static LucernaFramePassStatus submittedDirectLightPreview(
            long frameIndex,
            boolean drawCallsIssued,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.ATTACHED_NO_OP,
                LucernaFramePassKind.DIRECT_LIGHT_PREVIEW_COMPOSITE,
                frameIndex,
                true,
                true,
                true,
                true,
                drawCallsIssued,
                message
        );
    }

    public static LucernaFramePassStatus frameClosed(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.FRAME_CLOSED,
                kind,
                frameIndex,
                false,
                false,
                false,
                false,
                false,
                message
        );
    }

    public static LucernaFramePassStatus skipped(
            LucernaFramePassKind kind,
            long frameIndex,
            String message
    ) {
        return new LucernaFramePassStatus(
                LucernaFramePassStatusCode.SKIPPED,
                kind,
                frameIndex,
                false,
                false,
                false,
                false,
                false,
                message
        );
    }

    public boolean attachable() {
        return this.code == LucernaFramePassStatusCode.READY_TO_ATTACH
                || this.code == LucernaFramePassStatusCode.ATTACHED_NO_OP;
    }

    public boolean accepted() {
        return this.code == LucernaFramePassStatusCode.ATTACHED_NO_OP;
    }

    private static boolean canReportDrawCalls(LucernaFramePassStatusCode code, LucernaFramePassKind kind) {
        return code == LucernaFramePassStatusCode.ATTACHED_NO_OP
                && kind == LucernaFramePassKind.DIRECT_LIGHT_PREVIEW_COMPOSITE;
    }
}
