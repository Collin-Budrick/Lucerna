package net.lucerna.render.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.lucerna.render.pass.LucernaFrameAttachmentMetadata;
import net.lucerna.render.pass.LucernaJavaOpaqueRenderObjects;
import net.lucerna.render.pass.LucernaFramePassPhase;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.renderer.GameRenderer;

public final class RenderThreadPreviewTargetFactory {
    private static final Object METADATA_ONLY_RENDER_PASS = new Object();
    private static final String DIRECT_LIGHT_PREVIEW_TARGET =
            "GameRenderer.renderLevel world color target after level render and before hand/HUD composition.";

    private RenderThreadPreviewTargetFactory() {
    }

    public static LucernaFramePassTarget metadataOnlyWorldColorTarget() {
        return LucernaFramePassTarget.safeWorldColorBeforeHud(
                METADATA_ONLY_RENDER_PASS,
                null,
                null,
                null,
                DIRECT_LIGHT_PREVIEW_TARGET,
                LucernaFrameAttachmentMetadata.metadataOnly(
                        LucernaFramePassPhase.WORLD_COLOR_BEFORE_HUD,
                        DIRECT_LIGHT_PREVIEW_TARGET
                )
        );
    }

    public static LucernaFramePassTarget worldColorTarget(GameRenderer renderer) {
        if (renderer == null) {
            return metadataOnlyWorldColorTarget();
        }

        RenderTarget renderTarget = renderer.mainRenderTarget();
        if (renderTarget == null) {
            return metadataOnlyWorldColorTarget();
        }

        GpuTextureView colorView = renderTarget.getColorTextureView();
        GpuTexture colorTexture = renderTarget.getColorTexture();
        GpuTextureView depthView = renderTarget.getDepthTextureView();
        GpuTexture depthTexture = renderTarget.getDepthTexture();
        GpuDevice device = RenderSystem.tryGetDevice();
        CommandEncoder commandEncoder = device == null ? null : device.createCommandEncoder();
        int width = positiveDimension(renderTarget.width, colorView == null ? 0 : colorView.getWidth(0));
        int height = positiveDimension(renderTarget.height, colorView == null ? 0 : colorView.getHeight(0));
        String colorFormat = colorTexture == null || colorTexture.getFormat() == null
                ? "unknown"
                : colorTexture.getFormat().toString();
        String depthFormat = depthTexture == null || depthTexture.getFormat() == null
                ? "unknown"
                : depthTexture.getFormat().toString();
        String colorLayout = colorTexture == null
                ? "unknown"
                : "usage=" + colorTexture.usage() + ",label=" + label(colorTexture.getLabel());
        String depthLayout = depthTexture == null
                ? "unknown"
                : "usage=" + depthTexture.usage() + ",label=" + label(depthTexture.getLabel());
        LucernaJavaOpaqueRenderObjects opaqueObjects = LucernaJavaOpaqueRenderObjects.of(
                renderTarget,
                commandEncoder,
                colorView,
                depthView,
                DIRECT_LIGHT_PREVIEW_TARGET
        );
        LucernaFrameAttachmentMetadata metadata = LucernaFrameAttachmentMetadata.javaOpaque(
                LucernaFramePassPhase.WORLD_COLOR_BEFORE_HUD,
                width,
                height,
                colorFormat,
                colorLayout,
                depthFormat,
                depthLayout,
                opaqueObjects,
                DIRECT_LIGHT_PREVIEW_TARGET
        );
        return LucernaFramePassTarget.safeWorldColorBeforeHud(
                renderTarget,
                commandEncoder,
                colorView,
                depthView,
                DIRECT_LIGHT_PREVIEW_TARGET,
                metadata
        );
    }

    private static int positiveDimension(int primary, int fallback) {
        if (primary > 0) {
            return primary;
        }
        return Math.max(0, fallback);
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
