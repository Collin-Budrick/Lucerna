package net.lucerna.mixin.client;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.lucerna.Lucerna;
import net.lucerna.LucernaController;
import net.lucerna.render.preview.ProofVisualMode;
import net.lucerna.render.preview.WorldSpaceEmissiveSpillSubmitter;
import net.lucerna.render.preview.WorldSpaceShadowDecalSubmitter;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static boolean lucerna$loggedCloudSuppression;
    private static boolean lucerna$loggedWorldSpaceHook;

    @Inject(
            method = "addCloudsPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/CloudStatus;Lnet/minecraft/world/phys/Vec3;JFIFI)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void lucerna$suppressVanillaCloudPass(
            FrameGraphBuilder frameGraphBuilder,
            CloudStatus cloudStatus,
            Vec3 cameraPosition,
            long ticks,
            float partialTick,
            int color,
            float cloudHeight,
            int viewDistance,
            CallbackInfo callbackInfo
    ) {
        if (!LucernaController.getInstance().isRendererActive()
                || !ProofVisualMode.experimentalVisualStackAllowed()) {
            return;
        }

        if (!lucerna$loggedCloudSuppression) {
            lucerna$loggedCloudSuppression = true;
            Lucerna.LOGGER.info(
                    "Lucerna cinematic atmosphere: vanillaCloudPassSuppressed=true reason=controller_experimental_visual_stack cloudStatus={} viewDistance={}.",
                    cloudStatus,
                    viewDistance
            );
        }
        callbackInfo.cancel();
    }

    @Inject(
            method = "submitFeatures(Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;Z)V",
            at = @At("TAIL"),
            require = 0
    )
    private void lucerna$submitWorldSpaceShadowDecals(
            LevelRenderState levelRenderState,
            SubmitNodeCollector submitNodeCollector,
            boolean renderBlockOutline,
            CallbackInfo callbackInfo
    ) {
        if (!lucerna$loggedWorldSpaceHook) {
            lucerna$loggedWorldSpaceHook = true;
            Lucerna.LOGGER.info(
                    "Lucerna LevelRenderer submitFeatures hook reached: worldSpaceVisualPreviewActive={} javaWorldSpaceFallback={} experimentalVisualStack={}",
                    LucernaController.getInstance().isWorldSpaceVisualPreviewActive(),
                    ProofVisualMode.javaWorldSpaceVisualFallbackAllowed(),
                    ProofVisualMode.experimentalVisualStackAllowed()
            );
        }
        PoseStack poseStack = new PoseStack();
        WorldSpaceEmissiveSpillSubmitter.submit(poseStack, submitNodeCollector, levelRenderState);
        WorldSpaceShadowDecalSubmitter.submit(poseStack, submitNodeCollector, levelRenderState);
    }
}
