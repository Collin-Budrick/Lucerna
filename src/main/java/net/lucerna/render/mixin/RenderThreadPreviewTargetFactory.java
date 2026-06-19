package net.lucerna.render.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.render.preview.DirectLightPreviewTextureUploadResult;
import net.lucerna.render.preview.DirectLightPreviewTextureUploader;
import net.lucerna.render.preview.PublicMojangFinalCompositeSubmissionResult;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffold;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffolds;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
import net.lucerna.render.pass.LucernaFrameAttachmentMetadata;
import net.lucerna.render.pass.LucernaJavaOpaqueRenderObjects;
import net.lucerna.render.pass.LucernaFramePassPhase;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.renderer.GameRenderer;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

public final class RenderThreadPreviewTargetFactory {
    private static final Object METADATA_ONLY_RENDER_PASS = new Object();
    private static final String DIRECT_LIGHT_PREVIEW_TARGET =
            "GameRenderer.renderLevel world color target after level render and before hand/HUD composition.";
    private static final DirectLightPreviewTextureUploader DIRECT_LIGHT_PREVIEW_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader();
    private static final DirectLightPreviewTextureUploader DIRECT_LIGHT_FINAL_COMPOSITE_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_direct_light_final_composite_rgba",
                    "direct-light final composite"
            );
    private static final DirectLightPreviewTextureUploader ROUND6_DIFFUSE_GI_FINAL_COMPOSITE_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_round6_diffuse_gi_final_composite_rgba",
                    "Round 6 native diffuse GI final composite"
            );

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
        long colorImageHandle = nativeTextureHandle(colorTexture);
        long colorImageViewHandle = nativeTextureViewHandle(colorView);
        long depthImageHandle = nativeTextureHandle(depthTexture);
        long depthImageViewHandle = nativeTextureViewHandle(depthView);
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
                colorImageHandle,
                colorImageViewHandle,
                depthImageHandle,
                depthImageViewHandle,
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

    public static PublicMojangPreviewPassSubmissionResult submitSampledPublicPreviewDraw(
            LucernaFramePassTarget target,
            DirectLightingCpuOutputPayload directOutputPayload
    ) {
        if (target == null || !target.available()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang sampled preview draw skipped because no frame target is available"
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang sampled preview draw skipped because the target is not HUD-safe"
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
                    "public Mojang sampled preview draw skipped because command encoder or color view is unavailable"
            );
        }
        if (directOutputPayload == null) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang sampled preview draw skipped because direct-light RGBA8 payload is unavailable"
            );
        }
        if (!directOutputPayload.readyForPreviewDraw()) {
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang sampled preview draw skipped because direct-light RGBA8 payload is not preview-ready: "
                            + directOutputPayload.debugSummary()
            );
        }

        ByteBuffer directOutputBuffer = directOutputPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult upload = DIRECT_LIGHT_PREVIEW_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                directOutputBuffer,
                directOutputPayload.width(),
                directOutputPayload.height()
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(directOutputBuffer);
            return PublicMojangPreviewPassSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang sampled preview draw skipped because direct-light texture upload was unavailable: "
                            + upload.summary()
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public sampled direct-light preview draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenDirectLightPreviewDraw(
                    renderPass,
                    upload.textureView(),
                    upload.sampler()
            );
        }
        commandEncoder.submit();
        Reference.reachabilityFence(directOutputBuffer);
        return PublicMojangPreviewPassSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                "public Mojang sampled direct-light preview render pass submitted; payload: "
                        + directOutputPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitFinalCompositePublicDraw(
            LucernaFramePassTarget target,
            DirectLightingCpuOutputPayload directOutputPayload
    ) {
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang final composite skipped because no frame target is available"
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang final composite skipped because the target is not HUD-safe"
            );
        }
        if (!(target.commandEncoder() instanceof CommandEncoder commandEncoder)
                || !(target.colorTarget() instanceof GpuTextureView colorView)) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    target.attachmentMetadata().javaOpaque()
                            ? PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT
                            : PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang final composite skipped because command encoder or color view is unavailable"
            );
        }
        if (directOutputPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang final composite skipped because native direct-light RGBA8 payload is unavailable"
            );
        }
        if (!directOutputPayload.readyForPreviewDraw()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang final composite skipped because native direct-light RGBA8 payload is not ready for final composite draw: "
                            + directOutputPayload.debugSummary()
            );
        }

        ByteBuffer directOutputBuffer = directOutputPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult upload = DIRECT_LIGHT_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                directOutputBuffer,
                directOutputPayload.width(),
                directOutputPayload.height()
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(directOutputBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang final composite skipped because direct-light texture upload was unavailable: "
                            + upload.summary()
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public final composite direct-light draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenDirectLightFinalCompositeDraw(
                    renderPass,
                    upload.textureView(),
                    upload.sampler()
            );
        }
        commandEncoder.submit();
        Reference.reachabilityFence(directOutputBuffer);
        return PublicMojangFinalCompositeSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                "public Mojang final composite direct-light render pass submitted; payload: "
                        + directOutputPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRound6DiffuseGiFinalCompositePublicDraw(
            LucernaFramePassTarget target,
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload,
            Round6DiffuseGiPreviewCompositeState previewState
    ) {
        if (previewState == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target != null && target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.UNKNOWN,
                    "public Mojang Round 6 diffuse GI final composite skipped because GI preview readiness state is unavailable"
            );
        }
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang Round 6 diffuse GI final composite skipped because no frame target is available"
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 6 diffuse GI final composite skipped because the target is not HUD-safe"
            );
        }
        if (!previewState.readyForRound6PreviewSource()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 6 diffuse GI final composite skipped because GI/cache readiness is incomplete: "
                            + previewState.summary()
            );
        }
        if (!(target.commandEncoder() instanceof CommandEncoder commandEncoder)
                || !(target.colorTarget() instanceof GpuTextureView colorView)) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    target.attachmentMetadata().javaOpaque()
                            ? PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT
                            : PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 6 diffuse GI final composite skipped because command encoder or color view is unavailable"
            );
        }
        if (diffuseGiPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 6 diffuse GI final composite skipped because native diffuse GI RGBA8 output payload is unavailable"
            );
        }
        if (!diffuseGiPayload.readyForPreviewDraw()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 6 diffuse GI final composite skipped because native diffuse GI RGBA8 output payload is not displayable: "
                            + diffuseGiPayload.debugSummary()
            );
        }

        ByteBuffer sourceBuffer = diffuseGiPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult upload = ROUND6_DIFFUSE_GI_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                sourceBuffer,
                diffuseGiPayload.width(),
                diffuseGiPayload.height()
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(sourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 6 diffuse GI final composite skipped because native GI texture upload was unavailable: "
                            + upload.summary()
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 6 native diffuse GI final composite draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound6DiffuseGiFinalCompositeDraw(
                    renderPass,
                    upload.textureView(),
                    upload.sampler()
            );
        }
        commandEncoder.submit();
        Reference.reachabilityFence(sourceBuffer);
        return PublicMojangFinalCompositeSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                "public Mojang Round 6 native diffuse GI output final composite render pass submitted; readiness: "
                        + previewState.summary()
                        + "; target: "
                        + targetAttachmentSummary(target)
                        + "; javaOpaquePublicFallback="
                        + target.attachmentMetadata().javaOpaque()
                        + "; native diffuse GI output payload: "
                        + diffuseGiPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    private static String targetAttachmentSummary(LucernaFramePassTarget target) {
        if (target == null || target.attachmentMetadata() == null) {
            return "missing";
        }
        LucernaFrameAttachmentMetadata metadata = target.attachmentMetadata();
        return "phase=" + metadata.phase()
                + ",extent=" + metadata.width() + "x" + metadata.height()
                + ",colorFormat=" + metadata.colorFormat()
                + ",colorLayout=" + metadata.colorLayout()
                + ",depthFormat=" + metadata.depthFormat()
                + ",depthLayout=" + metadata.depthLayout()
                + "," + metadata.attachmentStatusLabel();
    }

    private static PublicMojangPreviewPassSubmissionResult submitDiagnosticFallback(
            CommandEncoder commandEncoder,
            GpuTextureView colorView,
            LucernaFramePassTarget target,
            String fallbackReason
    ) {
        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public diagnostic direct-light preview fallback draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueDiagnosticDirectLightPreviewDraw(renderPass);
        }
        commandEncoder.submit();
        return PublicMojangPreviewPassSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangPreviewPassSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                fallbackReason + "; diagnostic fallback: " + drawScaffold.summary()
        );
    }

    private static int positiveDimension(int primary, int fallback) {
        if (primary > 0) {
            return primary;
        }
        return Math.max(0, fallback);
    }

    private static RenderPass createFullTargetRenderPass(
            CommandEncoder commandEncoder,
            Supplier<String> label,
            GpuTextureView colorView,
            LucernaFramePassTarget target
    ) {
        LucernaFrameAttachmentMetadata metadata = target == null ? null : target.attachmentMetadata();
        Object depthTarget = target == null ? null : target.depthTarget();
        if (depthTarget instanceof GpuTextureView depthView
                && metadata != null
                && metadata.width() > 0
                && metadata.height() > 0) {
            return commandEncoder.createRenderPass(
                    label,
                    colorView,
                    Optional.empty(),
                    depthView,
                    OptionalDouble.empty(),
                    new RenderPass.RenderArea(0, 0, metadata.width(), metadata.height())
            );
        }
        return commandEncoder.createRenderPass(label, colorView, Optional.empty());
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private static long nativeTextureHandle(GpuTexture texture) {
        if (texture instanceof VulkanGpuTexture vulkanTexture) {
            return vulkanTexture.vkImage();
        }
        return 0L;
    }

    private static long nativeTextureViewHandle(GpuTextureView textureView) {
        if (textureView instanceof VulkanGpuTextureView vulkanTextureView) {
            return vulkanTextureView.vkImageView();
        }
        return 0L;
    }
}
