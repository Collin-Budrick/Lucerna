package net.lucerna.render;

import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.telemetry.LucernaTelemetry;

public final class LucernaFrameHooks {
    private final LucernaNativeBridge nativeBridge;
    private final LucernaTelemetry telemetry;
    private long frameIndex;

    public LucernaFrameHooks(LucernaNativeBridge nativeBridge, LucernaTelemetry telemetry) {
        this.nativeBridge = nativeBridge;
        this.telemetry = telemetry;
    }

    public void beginFrame(float tickDelta) {
        long frame = ++this.frameIndex;
        this.telemetry.beginCpuScope("frame");
        this.nativeBridge.beginFrame(frame, tickDelta);
    }

    public void renderLighting() {
        this.telemetry.beginCpuScope("lighting");
        this.nativeBridge.renderLighting();
        this.telemetry.endCpuScope("lighting");
    }

    public void endFrame() {
        this.nativeBridge.endFrame();
        this.telemetry.endCpuScope("frame");
    }

    public long frameIndex() {
        return this.frameIndex;
    }
}
