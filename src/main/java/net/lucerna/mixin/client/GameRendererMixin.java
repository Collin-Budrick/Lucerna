package net.lucerna.mixin.client;

import net.lucerna.LucernaController;
import net.lucerna.render.mixin.RenderThreadPreviewTargetFactory;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void lucerna$attachDirectLightPreviewComposite(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        LucernaController controller = LucernaController.getInstance();
        if (!controller.isRendererActive()) {
            return;
        }

        LucernaFramePassTarget target = RenderThreadPreviewTargetFactory.worldColorTarget((GameRenderer) (Object) this);
        controller.attachDirectLightPreviewTarget(target, 0.0F);
    }
}
