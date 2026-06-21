package net.lucerna.render.frame;

import net.lucerna.render.gbuffer.GBufferSceneDataAttachment;
import net.lucerna.render.gbuffer.GBufferSceneDataSamplingEvidence;
import net.lucerna.render.gbuffer.GBufferWriteIntent;
import net.lucerna.render.pass.LucernaFramePassTarget;

import java.util.ArrayList;
import java.util.List;

public record LucernaFrameConstants(
        long frameIndex,
        float tickDelta,
        FrameViewport viewport,
        WorldRenderState worldState,
        FrameCameraMatrices cameraMatrices,
        FrameJitter jitter,
        FrameRenderFlags flags,
        long capturedNanos
) {
    public LucernaFrameConstants {
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
        capturedNanos = Math.max(0L, capturedNanos);
    }

    public static LucernaFrameConstants unavailable() {
        return new LucernaFrameConstants(
                0L,
                0.0F,
                FrameViewport.UNAVAILABLE,
                WorldRenderState.unavailable(),
                FrameCameraMatrices.unavailable(),
                FrameJitter.disabled(),
                FrameRenderFlags.unavailable(),
                0L
        );
    }

    public boolean hasFrameIndex() {
        return this.frameIndex > 0L;
    }

    public boolean hasViewport() {
        return this.viewport.available();
    }

    public boolean hasWorldState() {
        return this.worldState.available();
    }

    public boolean hasCameraMatrices() {
        return this.cameraMatrices.hasRequiredMatrices();
    }

    public boolean hasRenderFlags() {
        return this.flags.available();
    }

    public boolean hasRequiredConstants() {
        return this.hasFrameIndex()
                && this.hasViewport()
                && this.hasWorldState()
                && this.hasCameraMatrices()
                && this.hasRenderFlags();
    }

    public boolean freshForFrame(long expectedFrameIndex) {
        return this.hasRequiredConstants() && expectedFrameIndex > 0L && this.frameIndex == expectedFrameIndex;
    }

    public List<String> missingRequiredConstants() {
        List<String> missing = new ArrayList<>();
        if (!this.hasFrameIndex()) {
            missing.add("frameIndex");
        }
        if (!this.hasViewport()) {
            missing.add("viewport");
        }
        if (!this.hasWorldState()) {
            missing.add("worldState");
        }
        if (!this.hasCameraMatrices()) {
            missing.add("cameraMatrices");
        }
        if (!this.hasRenderFlags()) {
            missing.add("renderFlags");
        }
        return List.copyOf(missing);
    }

    public SceneFrameDataReadiness sceneFrameDataReadiness(
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            List<GBufferSceneDataAttachment> frameDataAttachments
    ) {
        return SceneFrameDataReadiness.from(this, writeIntent, frameTarget, frameDataAttachments);
    }

    public SceneFrameDataReadiness liveFrameTargetSceneFrameDataReadiness(
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            String depthSamplingMarker
    ) {
        return SceneFrameDataReadiness.fromLiveFrameTarget(this, writeIntent, frameTarget, depthSamplingMarker);
    }

    public SceneFrameDataReadiness liveFrameTargetSceneFrameDataReadiness(
            GBufferWriteIntent writeIntent,
            LucernaFramePassTarget frameTarget,
            String depthSamplingMarker,
            GBufferSceneDataSamplingEvidence depthSamplingEvidence
    ) {
        return SceneFrameDataReadiness.fromLiveFrameTarget(
                this,
                writeIntent,
                frameTarget,
                depthSamplingMarker,
                depthSamplingEvidence
        );
    }

    private static float clampTickDelta(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
