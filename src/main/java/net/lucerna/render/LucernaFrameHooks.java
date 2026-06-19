package net.lucerna.render;

import net.lucerna.compat.BackendStatus;
import net.lucerna.nativebridge.LucernaNativeBridge;
import net.lucerna.render.hooks.FrameHookResult;
import net.lucerna.render.hooks.FrameLifecycleSnapshot;
import net.lucerna.render.hooks.VulkanFrameLifecycleAdapter;
import net.lucerna.telemetry.LucernaTelemetry;

public final class LucernaFrameHooks {
    private final VulkanFrameLifecycleAdapter lifecycleAdapter;

    public LucernaFrameHooks(LucernaNativeBridge nativeBridge, LucernaTelemetry telemetry) {
        this.lifecycleAdapter = new VulkanFrameLifecycleAdapter(nativeBridge, telemetry);
    }

    public FrameHookResult onResize(int width, int height) {
        return this.lifecycleAdapter.onResize(width, height);
    }

    public FrameHookResult beginFrame(BackendStatus backendStatus, float tickDelta) {
        return this.lifecycleAdapter.beginFrame(backendStatus, tickDelta);
    }

    public FrameHookResult beginFrame(float tickDelta) {
        return this.lifecycleAdapter.beginFrame(tickDelta);
    }

    public FrameHookResult renderLighting() {
        return this.lifecycleAdapter.renderLighting();
    }

    public FrameHookResult endFrame() {
        return this.lifecycleAdapter.endFrame();
    }

    public long frameIndex() {
        return this.lifecycleAdapter.frameIndex();
    }

    public FrameHookResult lastResult() {
        return this.lifecycleAdapter.lastResult();
    }

    public FrameLifecycleSnapshot snapshot() {
        return this.lifecycleAdapter.snapshot();
    }
}
