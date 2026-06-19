package net.lucerna.mixin.client;

import net.lucerna.LucernaController;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final Object LUCERNA_METADATA_ONLY_RENDER_PASS = new Object();
    private static final String LUCERNA_DIRECT_LIGHT_PREVIEW_TARGET =
            "GameRenderer.renderLevel world color target after level render and before hand/HUD composition.";

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

        LucernaFramePassTarget target = LucernaFramePassTarget.safeWorldColorBeforeHud(
                LUCERNA_METADATA_ONLY_RENDER_PASS,
                null,
                null,
                null,
                LUCERNA_DIRECT_LIGHT_PREVIEW_TARGET
        );
        controller.attachDirectLightPreviewTarget(target, 0.0F);
    }
}
