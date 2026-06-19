package net.lucerna.render.frame;

public record FrameConstantsCaptureRequest(
        long frameIndex,
        float tickDelta,
        FrameViewport viewport,
        WorldRenderState worldState,
        FrameCameraMatrices cameraMatrices,
        FrameJitter jitter,
        FrameRenderFlags flags,
        boolean historyResetRequested,
        String historyResetReason,
        String source
) {
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_MINECRAFT_REFLECTION = "minecraft-reflection";

    public FrameConstantsCaptureRequest {
        frameIndex = Math.max(0L, frameIndex);
        tickDelta = clampTickDelta(tickDelta);
        if (viewport == null) {
            viewport = FrameViewport.UNAVAILABLE;
        }
        if (worldState == null) {
            worldState = WorldRenderState.unavailable();
        }
        if (cameraMatrices == null) {
            cameraMatrices = FrameCameraMatrices.unavailable();
        }
        if (jitter == null) {
            jitter = FrameJitter.disabled();
        }
        if (flags == null) {
            flags = FrameRenderFlags.unavailable();
        }
        historyResetReason = clean(historyResetReason, historyResetRequested ? "History reset requested." : "");
        source = clean(source, SOURCE_MANUAL);
    }

    public static Builder builder(long frameIndex, float tickDelta) {
        return new Builder(frameIndex, tickDelta);
    }

    public static FrameConstantsCaptureRequest unavailable(long frameIndex, float tickDelta, String source) {
        return builder(frameIndex, tickDelta).source(source).build();
    }

    public static FrameConstantsCaptureRequest fromConstants(LucernaFrameConstants constants, String source) {
        if (constants == null) {
            return unavailable(0L, 0.0F, source);
        }
        return builder(constants.frameIndex(), constants.tickDelta())
                .viewport(constants.viewport())
                .worldState(constants.worldState())
                .cameraMatrices(constants.cameraMatrices())
                .jitter(constants.jitter())
                .flags(constants.flags())
                .source(source)
                .build();
    }

    public LucernaFrameConstants toFrameConstants(long capturedNanos) {
        return new LucernaFrameConstants(
                this.frameIndex,
                this.tickDelta,
                this.viewport,
                this.worldState,
                this.cameraMatrices,
                this.jitter,
                this.flags,
                capturedNanos
        );
    }

    public boolean hasCameraMatrices() {
        return this.cameraMatrices.hasRequiredMatrices();
    }

    public boolean hasViewport() {
        return this.viewport.available();
    }

    public boolean hasWorldState() {
        return this.worldState.available();
    }

    public boolean hasRenderFlags() {
        return this.flags.available();
    }

    public Builder toBuilder() {
        return builder(this.frameIndex, this.tickDelta)
                .viewport(this.viewport)
                .worldState(this.worldState)
                .cameraMatrices(this.cameraMatrices)
                .jitter(this.jitter)
                .flags(this.flags)
                .historyReset(this.historyResetRequested, this.historyResetReason)
                .source(this.source);
    }

    private static float clampTickDelta(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public static final class Builder {
        private final long frameIndex;
        private final float tickDelta;
        private FrameViewport viewport = FrameViewport.UNAVAILABLE;
        private WorldRenderState worldState = WorldRenderState.unavailable();
        private FrameCameraMatrices cameraMatrices = FrameCameraMatrices.unavailable();
        private FrameJitter jitter = FrameJitter.disabled();
        private FrameRenderFlags flags = FrameRenderFlags.unavailable();
        private boolean historyResetRequested;
        private String historyResetReason = "";
        private String source = SOURCE_MANUAL;

        private Builder(long frameIndex, float tickDelta) {
            this.frameIndex = frameIndex;
            this.tickDelta = tickDelta;
        }

        public Builder viewport(FrameViewport viewport) {
            this.viewport = viewport;
            return this;
        }

        public Builder worldState(WorldRenderState worldState) {
            this.worldState = worldState;
            return this;
        }

        public Builder cameraMatrices(FrameCameraMatrices cameraMatrices) {
            this.cameraMatrices = cameraMatrices;
            return this;
        }

        public Builder jitter(FrameJitter jitter) {
            this.jitter = jitter;
            return this;
        }

        public Builder flags(FrameRenderFlags flags) {
            this.flags = flags;
            return this;
        }

        public Builder requestHistoryReset(String reason) {
            return this.historyReset(true, reason);
        }

        public Builder historyReset(boolean requested, String reason) {
            this.historyResetRequested = requested;
            this.historyResetReason = reason;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public FrameConstantsCaptureRequest build() {
            return new FrameConstantsCaptureRequest(
                    this.frameIndex,
                    this.tickDelta,
                    this.viewport,
                    this.worldState,
                    this.cameraMatrices,
                    this.jitter,
                    this.flags,
                    this.historyResetRequested,
                    this.historyResetReason,
                    this.source
            );
        }
    }
}
