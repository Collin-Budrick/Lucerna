package net.lucerna.render.pass;

public record LucernaFramePassResult(
        LucernaFramePassRequest request,
        LucernaFramePassStatus status,
        boolean accepted,
        boolean drawCallsIssued,
        long frameIndex,
        String message
) {
    public LucernaFramePassResult {
        frameIndex = Math.max(0L, frameIndex);
        if (request == null) {
            request = LucernaFramePassRequest.noOp(frameIndex);
        }
        if (status == null) {
            status = LucernaFramePassStatus.skipped(request.kind(), frameIndex, message);
        }
        accepted = accepted && status.accepted();
        drawCallsIssued = drawCallsIssued && status.drawCallsIssued();
        if (message == null || message.isBlank()) {
            message = status.message();
        } else {
            message = message.trim();
        }
    }

    public static LucernaFramePassResult acceptedNoOp(
            LucernaFramePassRequest request,
            LucernaFramePassStatus status
    ) {
        long fallbackFrameIndex = status == null ? frameIndexFrom(request) : status.frameIndex();
        LucernaFramePassRequest normalizedRequest = request == null
                ? LucernaFramePassRequest.noOp(fallbackFrameIndex)
                : request;
        LucernaFramePassStatus normalizedStatus = status == null
                ? LucernaFramePassStatus.attachedNoOp(
                        normalizedRequest.kind(),
                        fallbackFrameIndex,
                        "Lucerna accepted a safe frame pass target; attachment remains a guarded no-op."
                )
                : status;
        return new LucernaFramePassResult(
                normalizedRequest,
                normalizedStatus,
                true,
                false,
                normalizedStatus.frameIndex(),
                normalizedStatus.message()
        );
    }

    public static LucernaFramePassResult acceptedDirectLightPreview(
            LucernaFramePassRequest request,
            LucernaFramePassStatus status,
            boolean drawCallsIssued
    ) {
        long fallbackFrameIndex = status == null ? frameIndexFrom(request) : status.frameIndex();
        LucernaFramePassRequest normalizedRequest = request == null
                ? LucernaFramePassRequest.directLightPreviewComposite(fallbackFrameIndex, null, 0.0F, 0.0F)
                : request;
        LucernaFramePassStatus normalizedStatus = status == null
                ? LucernaFramePassStatus.submittedDirectLightPreview(
                        fallbackFrameIndex,
                        drawCallsIssued,
                        "Lucerna accepted a direct-light preview frame target."
                )
                : status;
        boolean canReportDrawCalls = drawCallsIssued
                && canReportDrawCalls(normalizedRequest.kind(), normalizedStatus.kind());
        return new LucernaFramePassResult(
                normalizedRequest,
                normalizedStatus,
                true,
                canReportDrawCalls,
                normalizedStatus.frameIndex(),
                normalizedStatus.message()
        );
    }

    public static LucernaFramePassResult skipped(
            LucernaFramePassRequest request,
            LucernaFramePassStatus status
    ) {
        long fallbackFrameIndex = status == null ? frameIndexFrom(request) : status.frameIndex();
        LucernaFramePassRequest normalizedRequest = request == null
                ? LucernaFramePassRequest.noOp(fallbackFrameIndex)
                : request;
        LucernaFramePassStatus normalizedStatus = status == null
                ? LucernaFramePassStatus.skipped(
                        normalizedRequest.kind(),
                        fallbackFrameIndex,
                        "Lucerna frame pass attachment was skipped."
                )
                : status;
        return new LucernaFramePassResult(
                normalizedRequest,
                normalizedStatus,
                false,
                false,
                normalizedStatus.frameIndex(),
                normalizedStatus.message()
        );
    }

    private static long frameIndexFrom(LucernaFramePassRequest request) {
        return request == null ? 0L : request.frameIndex();
    }

    private static boolean canReportDrawCalls(LucernaFramePassKind requestKind, LucernaFramePassKind statusKind) {
        return requestKind == statusKind
                && (requestKind == LucernaFramePassKind.DIRECT_LIGHT_PREVIEW_COMPOSITE
                || requestKind == LucernaFramePassKind.FINAL_WORLD_COLOR_COMPOSITE);
    }
}
