package net.lucerna.render.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffold;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffolds;
import net.lucerna.render.pass.LucernaFrameAttachmentMetadata;
import net.lucerna.render.pass.LucernaJavaOpaqueRenderObjects;
import net.lucerna.render.pass.LucernaFramePassPhase;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.renderer.GameRenderer;

import java.util.Optional;

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

    public static PublicMojangPreviewPassSubmissionResult submitNoDrawPublicPreviewPass(
            LucernaFramePassTarget target
    ) {
        if (target == null || !target.available()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang preview pass skipped because no frame target is available"
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang preview pass skipped because the target is not HUD-safe"
            );
        }
        if (!(target.commandEncoder() instanceof CommandEncoder commandEncoder)
                || !(target.colorTarget() instanceof GpuTextureView colorView)) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    target.attachmentMetadata().javaOpaque()
                            ? PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT
                            : PublicMojangPreviewPassSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang preview pass skipped because command encoder or color view is unavailable"
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "lucerna public direct-light preview no-draw pass",
                colorView,
                Optional.empty()
        )) {
            drawScaffold = PublicMojangPreviewDrawScaffolds.describeFullscreenDirectLightPreviewDraw(
                    renderPass,
                    null
            );
            // Intentionally no draw calls: validates attachment without changing the world color target.
        }
        commandEncoder.submit();
        return PublicMojangPreviewPassSubmissionResult.submitted(
                false,
                target.attachmentMetadata().javaOpaque(),
                PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                "public Mojang preview render pass submitted without draw calls; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangPreviewPassSubmissionResult submitDiagnosticPublicPreviewDraw(
            LucernaFramePassTarget target
    ) {
        if (target == null || !target.available()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang diagnostic preview draw skipped because no frame target is available"
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang diagnostic preview draw skipped because the target is not HUD-safe"
            );
        }
        if (!(target.commandEncoder() instanceof CommandEncoder commandEncoder)
                || !(target.colorTarget() instanceof GpuTextureView colorView)) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    target.attachmentMetadata().javaOpaque()
                            ? PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT
                            : PublicMojangPreviewPassSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang diagnostic preview draw skipped because command encoder or color view is unavailable"
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "lucerna public diagnostic direct-light preview draw pass",
                colorView,
                Optional.empty()
        )) {
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueDiagnosticDirectLightPreviewDraw(renderPass);
        }
        commandEncoder.submit();
        return PublicMojangPreviewPassSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                "public Mojang diagnostic preview render pass submitted; draw scaffold: "
                        + drawScaffold.summary()
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
