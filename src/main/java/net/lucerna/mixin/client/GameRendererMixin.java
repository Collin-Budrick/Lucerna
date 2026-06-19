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
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V",
                    shift = At.Shift.BEFORE
            ),
            require = 0
    )
    private void lucerna$attachPresentedWorldColorComposite(
            DeltaTracker deltaTracker,
            boolean renderLevel,
            CallbackInfo callbackInfo
    ) {
        LucernaController controller = LucernaController.getInstance();
        if (!renderLevel || !controller.isRendererActive()) {
            return;
        }

        LucernaFramePassTarget target = RenderThreadPreviewTargetFactory.worldColorTarget((GameRenderer) (Object) this);
        controller.attachFinalWorldColorCompositeTarget(target, 0.0F);
    }
}
