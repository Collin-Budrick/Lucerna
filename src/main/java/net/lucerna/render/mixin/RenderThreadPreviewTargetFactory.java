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
import net.lucerna.LucernaController;
import net.lucerna.nativebridge.DenoisedDiffuseGiCpuOutputPayload;
import net.lucerna.nativebridge.DirectLightingCpuOutputPayload;
import net.lucerna.nativebridge.Round6DiffuseGiCpuOutputPayload;
import net.lucerna.render.preview.DirectLightPreviewTextureUploadResult;
import net.lucerna.render.preview.DirectLightPreviewTextureUploader;
import net.lucerna.render.preview.PublicMojangFinalCompositeSubmissionResult;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffold;
import net.lucerna.render.preview.PublicMojangPreviewDrawScaffolds;
import net.lucerna.render.preview.Round7DenoisedGiVisualSource;
import net.lucerna.render.preview.Round7RawGiVisualSource;
import net.lucerna.render.preview.Round6DiffuseGiPreviewCompositeState;
import net.lucerna.render.preview.ShaderDenoiseOutputRenderTarget;
import net.lucerna.render.preview.ShaderGeneratedDenoiseOutputStatus;
import net.lucerna.render.pass.LucernaFrameAttachmentMetadata;
import net.lucerna.render.pass.LucernaJavaOpaqueRenderObjects;
import net.lucerna.render.pass.LucernaFramePassPhase;
import net.lucerna.render.pass.LucernaFramePassTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;

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
    private static final DirectLightPreviewTextureUploader NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_native_shadow_map_final_composite_mask_rgba",
                    "native shadow-map final composite mask"
            );
    private static final DirectLightPreviewTextureUploader ROUND6_DIFFUSE_GI_FINAL_COMPOSITE_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_round7_raw_gi_native_diffuse_source_rgba",
                    "Round 7 RAW_GI native diffuse-GI source"
            );
    private static final DirectLightPreviewTextureUploader ROUND7_DENOISED_GI_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_round7_denoised_gi_cpu_output_rgba",
                    "Round 7 DENOISED_GI denoised diffuse-GI CPU output"
            );
    private static final DirectLightPreviewTextureUploader ROUND7_SHADER_DENOISE_INPUT_TEXTURE_UPLOADER =
            new DirectLightPreviewTextureUploader(
                    "lucerna_round7_shader_denoise_input_rgba",
                    "Round 7 shader denoise raw diffuse-GI input"
            );
    private static final String ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV =
            "LUCERNA_ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK";
    private static final ShaderDenoiseOutputRenderTarget SHADER_DENOISE_OUTPUT_RENDER_TARGET =
            new ShaderDenoiseOutputRenderTarget();
    private static GpuTexture lastWorldColorTexture;

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
        lastWorldColorTexture = colorTexture;
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

    public static ShaderDenoiseOutputRenderTarget.StatusSnapshot ensureShaderDenoiseOutputRenderTarget(
            LucernaFramePassTarget target
    ) {
        LucernaFrameAttachmentMetadata metadata = target == null ? null : target.attachmentMetadata();
        int width = metadata == null ? 0 : metadata.width();
        int height = metadata == null ? 0 : metadata.height();
        return SHADER_DENOISE_OUTPUT_RENDER_TARGET.ensure(RenderSystem.tryGetDevice(), width, height);
    }

    public static ShaderDenoiseOutputRenderTarget.StatusSnapshot shaderDenoiseOutputRenderTargetStatus() {
        return SHADER_DENOISE_OUTPUT_RENDER_TARGET.statusSnapshot();
    }

    public static ShaderDenoiseOutputRenderTarget.StatusSnapshot releaseShaderDenoiseOutputRenderTarget() {
        return SHADER_DENOISE_OUTPUT_RENDER_TARGET.releaseResources();
    }

    public static void closeShaderDenoiseOutputRenderTarget() {
        SHADER_DENOISE_OUTPUT_RENDER_TARGET.close();
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
        if (!hasNativeDirectLightCandidatePayload(directOutputPayload)) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang final composite skipped because native direct-light RGBA8 payload has no emissive/direct candidate evidence; refusing metadata-only or proof-marker-only readiness: "
                            + directOutputPayload.debugSummary()
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
                "public Mojang final composite native direct-light surface-source render pass submitted; candidateEvidence=true; payload: "
                        + directOutputPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitNativeShadowMapFinalCompositePublicDraw(
            LucernaFramePassTarget target,
            ByteBuffer shadowMapMaskRgbaPayload,
            int shadowMapMaskWidth,
            int shadowMapMaskHeight,
            boolean realShadowMapOutputReady,
            String shadowMapPayloadSummary
    ) {
        String payloadSummary = shadowMapPayloadSummary == null || shadowMapPayloadSummary.isBlank()
                ? "native shadow-map payload summary unavailable"
                : shadowMapPayloadSummary.trim();
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang native shadow-map final composite skipped because no frame target is available; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady="
                            + realShadowMapOutputReady
                            + "; payload: "
                            + payloadSummary
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang native shadow-map final composite skipped because the target is not HUD-safe; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady="
                            + realShadowMapOutputReady
                            + "; payload: "
                            + payloadSummary
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
                    "public Mojang native shadow-map final composite skipped because command encoder or color view is unavailable; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady="
                            + realShadowMapOutputReady
                            + "; payload: "
                            + payloadSummary
            );
        }
        if (!realShadowMapOutputReady) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map output is not ready; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=false; payload: "
                            + payloadSummary
            );
        }
        if (shadowMapMaskRgbaPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map RGBA8 mask payload is unavailable; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true; payload: "
                            + payloadSummary
            );
        }
        if (shadowMapMaskWidth <= 0 || shadowMapMaskHeight <= 0) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map mask dimensions are invalid; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,width="
                            + shadowMapMaskWidth
                            + ",height="
                            + shadowMapMaskHeight
                            + "; payload: "
                            + payloadSummary
            );
        }
        long pixelCount = (long) shadowMapMaskWidth * (long) shadowMapMaskHeight;
        long maxUploadPixels = Integer.MAX_VALUE / (long) DirectLightPreviewTextureUploader.BYTES_PER_PIXEL;
        if (pixelCount > maxUploadPixels) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map RGBA8 mask dimensions exceed supported byte count; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,pixelCount="
                            + pixelCount
                            + ",maxUploadPixels="
                            + maxUploadPixels
                            + "; payload: "
                            + payloadSummary
            );
        }
        long requiredBytes = pixelCount * (long) DirectLightPreviewTextureUploader.BYTES_PER_PIXEL;
        if (shadowMapMaskRgbaPayload.remaining() < requiredBytes) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map RGBA8 mask byte count is incomplete; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,requiredBytes="
                            + requiredBytes
                            + ",suppliedBytes="
                            + shadowMapMaskRgbaPayload.remaining()
                            + "; payload: "
                            + payloadSummary
            );
        }

        ByteBuffer shadowMaskBuffer = shadowMapMaskRgbaPayload.duplicate();
        DirectLightPreviewTextureUploadResult upload = NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                shadowMaskBuffer,
                shadowMapMaskWidth,
                shadowMapMaskHeight
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(shadowMaskBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite skipped because native shadow-map mask texture upload was unavailable; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true; upload: "
                            + upload.summary()
                            + "; payload: "
                            + payloadSummary
            );
        }

        GpuTextureView depthView = target.depthTarget() instanceof GpuTextureView view ? view : null;
        boolean depthTextureSampleBindingReady = target.attachmentMetadata().depthTextureSampleBindingReady()
                && depthView != null;
        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> depthTextureSampleBindingReady
                        ? "lucerna public final composite depth-aware native shadow-map occlusion draw pass"
                        : "lucerna public final composite native shadow-map occlusion draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            if (depthTextureSampleBindingReady) {
                drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenDepthAwareNativeShadowMapFinalCompositeDraw(
                        renderPass,
                        upload.textureView(),
                        upload.sampler(),
                        depthView,
                        upload.sampler(),
                        realShadowMapOutputReady
                );
            } else {
                drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenNativeShadowMapFinalCompositeDraw(
                        renderPass,
                        upload.textureView(),
                        upload.sampler(),
                        realShadowMapOutputReady
                );
            }
        }
        commandEncoder.submit();
        Reference.reachabilityFence(shadowMaskBuffer);
        if (!drawScaffold.drawCallsIssued()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang native shadow-map final composite did not consume the uploaded shadow-map mask because the draw scaffold did not issue a draw; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true; payload: "
                            + payloadSummary
                            + "; upload: "
                            + upload.summary()
                            + "; draw scaffold: "
                            + drawScaffold.summary()
            );
        }
        return PublicMojangFinalCompositeSubmissionResult.submitted(
                drawScaffold.drawCallsIssued(),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                "public Mojang native shadow-map final-composite occlusion render pass submitted; "
                        + "nativeShadowMapComposite=true,shadowMapOutputConsumed=true,screenSpaceShadowDecal=false,"
                        + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,"
                        + "depthAwareShadowMaskComposite=" + depthTextureSampleBindingReady
                        + ",shaderPassDepthSamplingEvidence=" + depthTextureSampleBindingReady
                        + ",depthSamplingPassOutputsReady=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_sampling_evidence=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_texture_sampled=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_sample_count="
                        + (depthTextureSampleBindingReady
                        ? Math.max(1L, (long) target.attachmentMetadata().width()
                        * (long) target.attachmentMetadata().height())
                        : 0L)
                        + ",g_buffer_depth_metadata_only=" + !depthTextureSampleBindingReady
                        + ",gBufferDepthSamplingMarker="
                        + (depthTextureSampleBindingReady
                        ? "shader_sampled_public_mojang_depth_view"
                        : "depth_view_not_bound_to_shadow_composite")
                        + ",depthSamplingPassOutputsMarker="
                        + (depthTextureSampleBindingReady
                        ? "java_native_shader_depth_sampling_evidence_parsed"
                        : "java_native_shader_depth_sampling_evidence_missing")
                        + ",depthSamplingBlocker="
                        + (depthTextureSampleBindingReady ? "none" : "current-depth-view-unavailable")
                        + ","
                        + "shadowMaskPayloadTexture=true,finalCompositeBeforeHud=true,target="
                        + targetAttachmentSummary(target)
                        + "; payload: "
                        + payloadSummary
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRealRendererMilestone1FullCompositePublicDraw(
            LucernaFramePassTarget target,
            ByteBuffer shadowMapMaskRgbaPayload,
            int shadowMapMaskWidth,
            int shadowMapMaskHeight,
            boolean realShadowMapOutputReady,
            String shadowMapPayloadSummary,
            Round6DiffuseGiCpuOutputPayload rawGiPayload,
            Round6DiffuseGiPreviewCompositeState previewState
    ) {
        String payloadSummary = shadowMapPayloadSummary == null || shadowMapPayloadSummary.isBlank()
                ? "native shadow-map payload summary unavailable"
                : shadowMapPayloadSummary.trim();
        boolean rawGiInputReady = previewState != null && previewState.rawGiInputReady(rawGiPayload);
        String rawGiEvidence = previewState == null
                ? "rawGiInputReady=false,rawGiInputSource=\"missing Round 6 diffuse GI preview state\""
                : previewState.rawGiInputSourceEvidence(rawGiPayload);
        String skippedBoundary =
                "realRendererMilestone1FullComposite=false,nativeShadowMapComposite=false,"
                        + "shadowMapOutputConsumed=false,depthAwareShadowMaskComposite=false,"
                        + "shaderGeneratedDenoiseOutputEvidence=false,shaderDenoiseOutputConsumedByFinalComposite=false,"
                        + "realShaderDenoiseOutputReady=false,shaderDenoiseNoOverclaim=true,"
                        + "shaderDenoiseOverclaimRejected=true,screenSpaceShadowDecal=false,"
                        + "lowResolutionDirectTextureDraw=false,metadataOnly=false,proofMarker=false,"
                        + "focusWindowOnly=false,temporaryDirectLightSubstitution=false,rectangularWashout=false";
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang real renderer milestone 1 full composite skipped because no frame target is available; "
                            + skippedBoundary
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang real renderer milestone 1 full composite skipped because the target is not HUD-safe; "
                            + skippedBoundary
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
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
                    "public Mojang real renderer milestone 1 full composite skipped because command encoder or color view is unavailable; "
                            + skippedBoundary
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }
        if (!realShadowMapOutputReady || shadowMapMaskRgbaPayload == null || shadowMapMaskWidth <= 0 || shadowMapMaskHeight <= 0) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang real renderer milestone 1 full composite skipped because native shadow-map output is not drawable; "
                            + skippedBoundary
                            + ",realShadowMapOutputReady="
                            + realShadowMapOutputReady
                            + ",width="
                            + shadowMapMaskWidth
                            + ",height="
                            + shadowMapMaskHeight
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }
        if (!rawGiInputReady) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang real renderer milestone 1 full composite skipped because strict raw diffuse-GI input is not ready for shader-generated denoise; "
                            + skippedBoundary
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }
        long shadowPixelCount = (long) shadowMapMaskWidth * (long) shadowMapMaskHeight;
        long requiredShadowBytes = shadowPixelCount * (long) DirectLightPreviewTextureUploader.BYTES_PER_PIXEL;
        if (shadowPixelCount > Integer.MAX_VALUE / (long) DirectLightPreviewTextureUploader.BYTES_PER_PIXEL
                || shadowMapMaskRgbaPayload.remaining() < requiredShadowBytes) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang real renderer milestone 1 full composite skipped because native shadow-map byte payload is invalid; "
                            + skippedBoundary
                            + ",requiredBytes="
                            + requiredShadowBytes
                            + ",suppliedBytes="
                            + shadowMapMaskRgbaPayload.remaining()
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }

        ByteBuffer shadowMaskBuffer = shadowMapMaskRgbaPayload.duplicate();
        DirectLightPreviewTextureUploadResult shadowUpload = NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                shadowMaskBuffer,
                shadowMapMaskWidth,
                shadowMapMaskHeight
        );
        ByteBuffer rawSourceBuffer = rawGiPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult rawUpload = ROUND7_SHADER_DENOISE_INPUT_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                rawSourceBuffer,
                rawGiPayload.width(),
                rawGiPayload.height()
        );
        if (!shadowUpload.availableForDraw() || !rawUpload.availableForDraw()) {
            Reference.reachabilityFence(shadowMaskBuffer);
            Reference.reachabilityFence(rawSourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang real renderer milestone 1 full composite skipped because one or more source uploads are unavailable; "
                            + skippedBoundary
                            + "; shadow upload: "
                            + shadowUpload.summary()
                            + "; raw upload: "
                            + rawUpload.summary()
                            + "; payload: "
                            + payloadSummary
                            + "; raw source: "
                            + rawGiEvidence
            );
        }

        ShaderDenoiseOutputRenderTarget.StatusSnapshot outputTargetStatus =
                ensureShaderDenoiseOutputRenderTarget(target);
        if (!outputTargetStatus.availableForRenderPass() || !outputTargetStatus.availableForSampling()) {
            Reference.reachabilityFence(shadowMaskBuffer);
            Reference.reachabilityFence(rawSourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                    ShaderGeneratedDenoiseOutputStatus.reported(
                            true,
                            outputTargetStatus.availableForRenderPass(),
                            false,
                            false,
                            false,
                            true,
                            true,
                            outputTargetStatus.reason()
                    ),
                    "public Mojang real renderer milestone 1 full composite skipped because the owned shader denoise output target is unavailable; "
                            + skippedBoundary
                            + "; output target: "
                            + outputTargetStatus.summary()
            );
        }

        PublicMojangPreviewDrawScaffold generationDrawScaffold;
        try (RenderPass outputRenderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna real renderer milestone 1 shader-denoise owned-output pass",
                outputTargetStatus.textureView(),
                target
        )) {
            outputRenderPass.disableScissor();
            generationDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7ShaderDenoiseOutputGenerationDraw(
                    outputRenderPass,
                    rawUpload.textureView(),
                    rawUpload.sampler()
            );
        }

        GpuTextureView depthView = target.depthTarget() instanceof GpuTextureView view ? view : null;
        boolean depthTextureSampleBindingReady = target.attachmentMetadata().depthTextureSampleBindingReady()
                && depthView != null;
        PublicMojangPreviewDrawScaffold shaderOutputDrawScaffold;
        PublicMojangPreviewDrawScaffold shadowDrawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna real renderer milestone 1 shadow plus shader-denoise final composite pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            shaderOutputDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7ShaderDenoiseOutputDraw(
                    renderPass,
                    outputTargetStatus.textureView(),
                    outputTargetStatus.sampler()
            );
            if (depthTextureSampleBindingReady) {
                shadowDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenDepthAwareNativeShadowMapFinalCompositeDraw(
                        renderPass,
                        shadowUpload.textureView(),
                        shadowUpload.sampler(),
                        depthView,
                        shadowUpload.sampler(),
                        true
                );
            } else {
                shadowDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenNativeShadowMapFinalCompositeDraw(
                        renderPass,
                        shadowUpload.textureView(),
                        shadowUpload.sampler(),
                        true
                );
            }
        }
        commandEncoder.submit();
        Reference.reachabilityFence(shadowMaskBuffer);
        Reference.reachabilityFence(rawSourceBuffer);

        boolean generationDrawIssued = generationDrawScaffold.drawCallsIssued();
        boolean shaderOutputConsumed = shaderOutputDrawScaffold.drawCallsIssued();
        boolean shadowConsumed = shadowDrawScaffold.drawCallsIssued();
        ShaderGeneratedDenoiseOutputStatus outputStatus = ShaderGeneratedDenoiseOutputStatus.reported(
                true,
                true,
                generationDrawIssued,
                generationDrawIssued && outputTargetStatus.availableForSampling(),
                shaderOutputConsumed,
                true,
                true,
                "public Mojang fragment pass generated lucerna.denoise.diffuse into an owned RGBA8 texture and the real-renderer final composite consumed that texture"
        );
        if (!generationDrawIssued || !shaderOutputConsumed || !shadowConsumed) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                    outputStatus,
                    "public Mojang real renderer milestone 1 full composite did not issue all required draws; "
                            + skippedBoundary
                            + ",generatedOutputDrawIssued="
                            + generationDrawIssued
                            + ",shaderOutputConsumed="
                            + shaderOutputConsumed
                            + ",shadowOutputConsumed="
                            + shadowConsumed
                            + "; generation draw scaffold: "
                            + generationDrawScaffold.summary()
                            + "; shader output draw scaffold: "
                            + shaderOutputDrawScaffold.summary()
                            + "; shadow draw scaffold: "
                            + shadowDrawScaffold.summary()
            );
        }

        return PublicMojangFinalCompositeSubmissionResult.submitted(
                true,
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                outputStatus,
                "public Mojang real renderer milestone 1 full composite render pass submitted; "
                        + "mode=FINAL_LUCERNA_COMPOSITE,mode=real-renderer-milestone1-full-composite"
                        + ",realRendererMilestone1FullComposite=true"
                        + ",sourceKind=native-shadow-map-mask,shadowMapMask=native"
                        + ",nativeShadowMapComposite=true,shadowMapOutputConsumed=true,shadow_map_output_consumed=true"
                        + ",nativeShadowMapConsumedByFinalComposite=true,realShadowMapComposite=true"
                        + ",shadowMapOutputConsumedByFinalComposite=true,shadow_map_output_consumed_by_final_composite=true"
                        + ",screenSpaceShadowDecal=false,lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true"
                        + ",depthAwareShadowMaskComposite=" + depthTextureSampleBindingReady
                        + ",shaderPassDepthSamplingEvidence=" + depthTextureSampleBindingReady
                        + ",depthSamplingPassOutputsReady=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_sampling_evidence=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_texture_sampled=" + depthTextureSampleBindingReady
                        + ",g_buffer_depth_sample_count="
                        + (depthTextureSampleBindingReady
                        ? Math.max(1L, (long) target.attachmentMetadata().width()
                        * (long) target.attachmentMetadata().height())
                        : 0L)
                        + ",g_buffer_depth_metadata_only=" + !depthTextureSampleBindingReady
                        + ",sourceIdentity=native-diffuse-gi-rgba8/raw-gi+shader-denoised-diffuse-gi-rgba8/public-mojang-fragment-color-attachment+native-shadow-map-mask"
                        + ",sourceIdentity=shader-denoised-diffuse-gi-rgba8/public-mojang-fragment-color-attachment"
                        + ",sourceKind=shader-generated-denoised-gi"
                        + ",shaderGeneratedDenoisedGI=public-mojang-fragment-pass"
                        + ",shaderDenoiseVisualShaderIntent=true"
                        + ",shaderDenoiseDispatchPrepared=true"
                        + ",shaderDenoiseInputReady=true"
                        + ",shaderDenoiseInputsCompleteForDispatch=true"
                        + ",shaderDenoiseRequiresRawDiffuseGiInput=true"
                        + ",shaderDenoiseStrictProofEligible=true"
                        + ",shaderDenoiseInputMode=strict-raw-diffuse-gi"
                        + ",shaderDenoiseInputKind=raw-diffuse-gi-rgba8"
                        + ",shaderDenoiseRawDiffuseGiInput=true,round7.shaderDenoise.rawDiffuseGiInput=true"
                        + ",directLightValidationInput=false,round7.shaderDenoise.directLightValidationInput=false"
                        + ",diagnosticDirectLightValidationFallback=false"
                        + ",shaderDenoiseOutputAttempted=true,round7.shaderDenoise.outputAttempted=true"
                        + ",shaderDenoiseOutputTextureAllocated=true"
                        + ",shaderDenoiseOwnedOutputImage=true"
                        + ",shaderDenoiseOutputRenderPassSubmitted=true"
                        + ",shaderDenoisePassExecuted=true"
                        + ",shaderGeneratedDenoisePassExecuted=true"
                        + ",shaderGeneratedOutputImageReady=true"
                        + ",shaderOutputSourceConsumed=true"
                        + ",shaderDenoiseOutputSourceConsumed=true"
                        + ",shaderDenoiseOutputConsumedByFinalComposite=true"
                        + ",shaderDenoisePassGeneratedVisualSource=true"
                        + ",shaderDenoiseFinalCompositeConsumable=true"
                        + ",finalCompositeConsumable=true"
                        + ",cpuReadbackFallbackActive=false"
                        + ",shaderDenoiseCpuReadbackFallbackActive=false"
                        + ",shaderDenoiseCpuReadbackFallbackInactive=true"
                        + ",cpuDenoiseReadbackFallback=false"
                        + ",cpuDenoisedReadbackSource=false"
                        + ",rawGiCpuReadbackInput=true"
                        + ",shaderOwnedOutputImage=true"
                        + ",shaderDenoiseOutputImageReady=true"
                        + ",round7.shaderDenoise.outputImageReady=true"
                        + ",round7.shaderDenoise.outputMaterialReady=true"
                        + ",round7.shaderDenoise.shaderGeneratedOutput=true"
                        + ",round7.shaderDenoise.realOutputReady=true"
                        + ",realShaderDenoiseOutputReady=true"
                        + ",shaderGeneratedDenoiseOutputEvidence=true"
                        + ",shaderDenoiseNoOverclaim=true,shaderDenoiseOverclaimRejected=true,shaderDenoiseOverclaimPresent=false"
                        + ",publicMojangShaderGeneratedVisualOutput=true"
                        + ",coloredBounceGi=true,contactShadow=true"
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false"
                        + ",temporaryDirectLightSubstitution=false,rectangularWashout=false"
                        + ",shadowMaskPayloadTexture=true,finalCompositeBeforeHud=true,target="
                        + targetAttachmentSummary(target)
                        + "; payload: "
                        + payloadSummary
                        + "; raw source: "
                        + rawGiEvidence
                        + "; raw upload: "
                        + rawUpload.summary()
                        + "; shadow upload: "
                        + shadowUpload.summary()
                        + "; shader output target: "
                        + outputTargetStatus.summary()
                        + "; generation draw scaffold: "
                        + generationDrawScaffold.summary()
                        + "; shader output draw scaffold: "
                        + shaderOutputDrawScaffold.summary()
                        + "; shadow draw scaffold: "
                        + shadowDrawScaffold.summary()
        );
    }

    private static boolean hasNativeDirectLightCandidatePayload(DirectLightingCpuOutputPayload payload) {
        return payload != null
                && payload.snapshot().hasExecutionTelemetry()
                && payload.snapshot().candidateCount() > 0
                && payload.snapshot().sampleCount() > 0
                && payload.snapshot().outputCount() > 0;
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRound6DiffuseGiFinalCompositePublicDraw(
            LucernaFramePassTarget target,
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload,
            Round6DiffuseGiPreviewCompositeState previewState
    ) {
        Round7RawGiVisualSource rawGiSource = Round7RawGiVisualSource.from(previewState, diffuseGiPayload);
        if (previewState == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target != null && target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.UNKNOWN,
                    "public Mojang Round 7 RAW_GI visual mode skipped because GI source readiness state is unavailable"
            );
        }
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang Round 7 RAW_GI visual mode skipped because no frame target is available; source: "
                            + rawGiSource.summary()
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 7 RAW_GI visual mode skipped because the target is not HUD-safe; source: "
                            + rawGiSource.summary()
            );
        }
        if (!previewState.readyForRound7RawGiSource()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 RAW_GI visual mode skipped because GI/cache source readiness is incomplete: "
                            + previewState.round7RawGiReadinessReason(diffuseGiPayload)
                            + "; source: "
                            + rawGiSource.summary()
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
                    "public Mojang Round 7 RAW_GI visual mode skipped because command encoder or color view is unavailable; source: "
                            + rawGiSource.summary()
            );
        }
        if (diffuseGiPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 RAW_GI visual mode skipped because native diffuse-GI RGBA8 source payload is unavailable; source: "
                            + rawGiSource.summary()
            );
        }
        if (!diffuseGiPayload.readyForPreviewDraw()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 RAW_GI visual mode skipped because native diffuse-GI RGBA8 source payload is not displayable: "
                            + diffuseGiPayload.debugSummary()
                            + "; source: "
                            + rawGiSource.summary()
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
                    "public Mojang Round 7 RAW_GI visual mode skipped because native diffuse-GI source texture upload was unavailable: "
                            + upload.summary()
                            + "; source: "
                            + rawGiSource.summary()
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 7 RAW_GI native diffuse-GI visual draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7RawGiVisualDraw(
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
                "public Mojang Round 7 RAW_GI visual render pass submitted; "
                        + "mode="
                        + previewState.round7RawGiModeKey()
                        + ",evidence="
                        + previewState.round7RawGiEvidenceLabel()
                        + ",readiness=\""
                        + previewState.round7RawGiReadinessReason(diffuseGiPayload)
                        + "\",source: "
                        + rawGiSource.summary()
                        + "; target: "
                        + targetAttachmentSummary(target)
                        + "; javaOpaquePublicFallback="
                        + target.attachmentMetadata().javaOpaque()
                        + "; native diffuse-GI RAW_GI source payload: "
                        + diffuseGiPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRound7DenoisedGiFinalCompositePublicDraw(
            LucernaFramePassTarget target,
            DenoisedDiffuseGiCpuOutputPayload denoisedGiPayload
    ) {
        Round7DenoisedGiVisualSource denoisedGiSource = Round7DenoisedGiVisualSource.from(denoisedGiPayload);
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because no frame target is available; source: "
                            + denoisedGiSource.summary()
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because the target is not HUD-safe; source: "
                            + denoisedGiSource.summary()
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
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because command encoder or color view is unavailable; source: "
                            + denoisedGiSource.summary()
            );
        }
        if (denoisedGiPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because denoised diffuse-GI RGBA8 CPU payload is unavailable; source: "
                            + denoisedGiSource.summary()
            );
        }
        if (!denoisedGiPayload.readyForPreviewDraw()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because denoised diffuse-GI RGBA8 CPU payload is not displayable: "
                            + denoisedGiPayload.debugSummary()
                            + "; source: "
                            + denoisedGiSource.summary()
            );
        }

        ByteBuffer sourceBuffer = denoisedGiPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult upload = ROUND7_DENOISED_GI_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                sourceBuffer,
                denoisedGiPayload.width(),
                denoisedGiPayload.height()
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(sourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 DENOISED_GI visual mode skipped because denoised diffuse-GI source texture upload was unavailable: "
                            + upload.summary()
                            + "; source: "
                            + denoisedGiSource.summary()
            );
        }

        PublicMojangPreviewDrawScaffold drawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 7 DENOISED_GI denoised diffuse-GI visual draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7DenoisedGiVisualDraw(
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
                "public Mojang Round 7 DENOISED_GI visual render pass submitted; "
                        + "mode="
                        + denoisedGiSource.modeKey()
                        + ",denoisedPayloadEvidence="
                        + denoisedGiSource.denoisedPayloadEvidence()
                        + ",readiness=\""
                        + denoisedGiPayload.previewReadinessReason()
                        + "\",source: "
                        + denoisedGiSource.summary()
                        + "; target: "
                        + targetAttachmentSummary(target)
                        + "; javaOpaquePublicFallback="
                        + target.attachmentMetadata().javaOpaque()
                        + "; denoised diffuse-GI CPU source payload: "
                        + denoisedGiPayload.debugSummary()
                        + "; upload: "
                        + upload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRound7ShaderDenoiseOutputPublicDraw(
            LucernaFramePassTarget target,
            Round6DiffuseGiCpuOutputPayload rawGiPayload
    ) {
        boolean rawSourceReady = rawGiPayload != null && rawGiPayload.readyForPreviewDraw();
        boolean directValidationFallbackEnabled = round7ShaderDenoiseDirectLightValidationFallbackEnabled();
        DirectLightingCpuOutputPayload validationDirectSourcePayload = rawSourceReady || !directValidationFallbackEnabled
                ? null
                : resolveNativeDirectLightCandidatePayload();
        boolean validationDirectSourceReady = validationDirectSourcePayload != null
                && validationDirectSourcePayload.readyForPreviewDraw();
        boolean strictRawDiffuseGiInput = rawSourceReady && !validationDirectSourceReady;
        boolean diagnosticDirectLightValidationFallback = !strictRawDiffuseGiInput && validationDirectSourceReady;
        boolean shaderInputReady = strictRawDiffuseGiInput || diagnosticDirectLightValidationFallback;
        String shaderInputMode = strictRawDiffuseGiInput
                ? "strict-raw-diffuse-gi"
                : (diagnosticDirectLightValidationFallback
                ? "diagnostic-direct-light-validation-fallback"
                : "missing");
        String shaderInputKind = strictRawDiffuseGiInput
                ? "raw-diffuse-gi-rgba8"
                : (diagnosticDirectLightValidationFallback ? "native-direct-light-rgba8-validation-input" : "none");
        String shaderInputSummary = strictRawDiffuseGiInput
                ? rawGiPayload.debugSummary()
                : (diagnosticDirectLightValidationFallback
                ? validationDirectSourcePayload.debugSummary()
                : rawGiPayload == null
                ? "raw diffuse-GI payload is missing; direct-light validation fallback enabled="
                + directValidationFallbackEnabled
                + " via "
                + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV
                : "raw diffuse-GI payload is not ready; direct-light validation fallback enabled="
                + directValidationFallbackEnabled
                + " via "
                + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV
                + "; raw diffuse-GI: "
                + rawGiPayload.debugSummary());
        Round7DenoisedGiVisualSource shaderDenoisedSource = shaderInputReady
                ? Round7DenoisedGiVisualSource.shaderGeneratedReady(
                "Round 7 shader-denoise public Mojang fragment pass can consume shader input mode "
                        + shaderInputMode
                        + " kind "
                        + shaderInputKind
        )
                : Round7DenoisedGiVisualSource.unavailable(
                rawGiPayload == null
                        ? "Round 7 shader-denoise output skipped because raw diffuse-GI payload is missing; "
                        + "direct-light validation fallback enabled="
                        + directValidationFallbackEnabled
                        + " via "
                        + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV
                        : "Round 7 shader-denoise output skipped because no displayable shader input source is available; raw diffuse-GI: "
                        + rawGiPayload.debugSummary()
                        + "; validation direct-light source: "
                        + (validationDirectSourcePayload == null ? "missing" : validationDirectSourcePayload.debugSummary())
                        + "; direct-light validation fallback enabled="
                        + directValidationFallbackEnabled
                        + " via "
                        + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV
        );
        String shaderInputBoundary = "shaderDenoiseInputMode="
                + shaderInputMode
                + ",shaderDenoiseInputKind="
                + shaderInputKind
                + ",shaderDenoiseRequiresRawDiffuseGiInput=true"
                + ",shaderDenoiseStrictProofEligible="
                + strictRawDiffuseGiInput
                + ",shaderDenoiseRawDiffuseGiInput="
                + strictRawDiffuseGiInput
                + ",round7.shaderDenoise.rawDiffuseGiInput="
                + strictRawDiffuseGiInput
                + ",directLightValidationInput="
                + diagnosticDirectLightValidationFallback
                + ",round7.shaderDenoise.directLightValidationInput="
                + diagnosticDirectLightValidationFallback
                + ",diagnosticDirectLightValidationFallback="
                + diagnosticDirectLightValidationFallback
                + ",diagnosticDirectLightValidationFallbackEnabled="
                + directValidationFallbackEnabled
                + ",diagnosticDirectLightValidationFallbackEnv="
                + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV;
        String skippedBoundary = "shaderDenoisePassExecuted=false,shaderGeneratedDenoisePassExecuted=false,"
                + "shaderOutputSourceConsumed=false,shaderDenoiseOutputSourceConsumed=false,"
                + "shaderDenoisePassGeneratedVisualSource=false,shaderDenoiseFinalCompositeConsumable=false,"
                + "finalCompositeConsumable=false,cpuReadbackFallbackActive=false,"
                + "shaderDenoiseCpuReadbackFallbackActive=false,cpuDenoiseReadbackFallback=false,"
                + "round7.shaderDenoise.outputImageReady=false,round7.shaderDenoise.outputMaterialReady=false,"
                + "round7.shaderDenoise.shaderGeneratedOutput=false,round7.shaderDenoise.realOutputReady=false,"
                + "realShaderDenoiseOutputReady=false,"
                + shaderInputBoundary;
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang Round 7 shader-denoise output skipped because no frame target is available; "
                            + skippedBoundary
                            + "; denoised source: "
                            + shaderDenoisedSource.summary()
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 7 shader-denoise output skipped because the target is not HUD-safe; "
                            + skippedBoundary
                            + "; denoised source: "
                            + shaderDenoisedSource.summary()
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
                    "public Mojang Round 7 shader-denoise output skipped because command encoder or color view is unavailable; "
                            + skippedBoundary
                            + "; denoised source: "
                            + shaderDenoisedSource.summary()
            );
        }
        if (!shaderInputReady) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 shader-denoise output skipped because no displayable shader input source is available; raw diffuse-GI: "
                            + (rawGiPayload == null ? "missing" : rawGiPayload.debugSummary())
                            + "; validation direct-light source: "
                            + (validationDirectSourcePayload == null ? "missing" : validationDirectSourcePayload.debugSummary())
                            + "; "
                            + skippedBoundary
                            + "; denoised source: "
                            + shaderDenoisedSource.summary()
            );
        }

        ByteBuffer sourceBuffer = strictRawDiffuseGiInput
                ? rawGiPayload.copyToByteBuffer()
                : validationDirectSourcePayload.copyToByteBuffer();
        int sourceWidth = strictRawDiffuseGiInput ? rawGiPayload.width() : validationDirectSourcePayload.width();
        int sourceHeight = strictRawDiffuseGiInput ? rawGiPayload.height() : validationDirectSourcePayload.height();
        DirectLightPreviewTextureUploadResult upload = ROUND7_SHADER_DENOISE_INPUT_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                sourceBuffer,
                sourceWidth,
                sourceHeight
        );
        if (!upload.availableForDraw()) {
            Reference.reachabilityFence(sourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 shader-denoise output skipped because the selected shader input texture upload was unavailable: "
                            + upload.summary()
                            + "; "
                            + skippedBoundary
                            + "; shader input mode="
                            + shaderInputMode
                            + "; shader input kind="
                            + shaderInputKind
                            + "; shader input payload: "
                            + shaderInputSummary
                            + "; denoised source: "
                            + shaderDenoisedSource.summary()
            );
        }

        ShaderDenoiseOutputRenderTarget.StatusSnapshot outputTargetStatus =
                ensureShaderDenoiseOutputRenderTarget(target);
        ShaderGeneratedDenoiseOutputStatus partialOutputStatus = ShaderGeneratedDenoiseOutputStatus.reported(
                true,
                outputTargetStatus.availableForRenderPass(),
                false,
                false,
                false,
                true,
                true,
                outputTargetStatus.reason()
        );
        if (!outputTargetStatus.availableForRenderPass() || !outputTargetStatus.availableForSampling()) {
            Reference.reachabilityFence(sourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                    partialOutputStatus,
                    "public Mojang Round 7 shader-denoise output skipped because the owned RGBA8 output target is unavailable: "
                            + outputTargetStatus.summary()
                            + "; "
                            + skippedBoundary
                            + "; shader input mode="
                            + shaderInputMode
                            + "; shader input kind="
                            + shaderInputKind
                            + "; shader input payload: "
                            + shaderInputSummary
                            + "; upload: "
                            + upload.summary()
            );
        }

        PublicMojangPreviewDrawScaffold generationDrawScaffold;
        try (RenderPass outputRenderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 7 shader-generated denoise owned-output pass",
                outputTargetStatus.textureView(),
                target
        )) {
            outputRenderPass.disableScissor();
            generationDrawScaffold = PublicMojangPreviewDrawScaffolds
                    .issueFullscreenRound7ShaderDenoiseOutputGenerationDraw(
                            outputRenderPass,
                            upload.textureView(),
                            upload.sampler()
                    );
        }

        PublicMojangPreviewDrawScaffold finalCompositeDrawScaffold;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 7 shader-generated denoise output final-composite pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            finalCompositeDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7ShaderDenoiseOutputDraw(
                    renderPass,
                    outputTargetStatus.textureView(),
                    outputTargetStatus.sampler()
            );
        }
        commandEncoder.submit();
        Reference.reachabilityFence(sourceBuffer);
        boolean generatedOutputDrawIssued = generationDrawScaffold.drawCallsIssued();
        boolean finalCompositeConsumedOutput = finalCompositeDrawScaffold.drawCallsIssued();
        ShaderGeneratedDenoiseOutputStatus outputStatus = ShaderGeneratedDenoiseOutputStatus.reported(
                true,
                true,
                generatedOutputDrawIssued,
                generatedOutputDrawIssued && outputTargetStatus.availableForSampling(),
                finalCompositeConsumedOutput,
                true,
                true,
                "public Mojang fragment pass generated lucerna.denoise.diffuse into an owned RGBA8 texture and "
                        + "the final composite consumed that texture; this is still not compute/storage-image denoise"
        );
        if (!generatedOutputDrawIssued || !finalCompositeConsumedOutput) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                    outputStatus,
                    "public Mojang Round 7 shader-denoise output did not become final-composite consumable because "
                            + "generatedOutputDrawIssued="
                            + generatedOutputDrawIssued
                            + " finalCompositeConsumedOutput="
                            + finalCompositeConsumedOutput
                            + "; shader input mode="
                            + shaderInputMode
                            + "; shader input kind="
                            + shaderInputKind
                            + "; shader input payload: "
                            + shaderInputSummary
                            + "; upload: "
                            + upload.summary()
                            + "; output target: "
                            + outputTargetStatus.summary()
                            + "; generation draw scaffold: "
                            + generationDrawScaffold.summary()
                            + "; final composite draw scaffold: "
                            + finalCompositeDrawScaffold.summary()
            );
        }
        return PublicMojangFinalCompositeSubmissionResult.submitted(
                true,
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                PublicMojangFinalCompositeSubmissionResult.ShaderOutputImageCandidate.none(),
                outputStatus,
                "public Mojang Round 7 shader-denoise output render pass submitted; "
                        + "mode=ROUND7_SHADER_DENOISE_OUTPUT,mode=round7-shader-denoise-output"
                        + ",evidence=round7.shaderDenoise.publicMojangFragmentPass"
                        + ",sourceKind=shader-generated-denoised-gi"
                        + ",sourceIdentity=shader-denoised-diffuse-gi-rgba8/public-mojang-fragment-output-image"
                        + ",shaderGeneratedDenoisedGI=public-mojang-fragment-pass"
                        + ",shaderDenoiseVisualShaderIntent=true"
                        + ",shaderDenoiseDispatchPrepared=true"
                        + ",shaderDenoiseInputReady=true"
                        + ",shaderDenoiseInputsCompleteForDispatch=true"
                        + ",shaderDenoiseInputMode="
                        + shaderInputMode
                        + ",shaderDenoiseInputKind="
                        + shaderInputKind
                        + ",shaderDenoiseRequiresRawDiffuseGiInput=true"
                        + ",shaderDenoiseStrictProofEligible="
                        + strictRawDiffuseGiInput
                        + ",shaderDenoiseRawDiffuseGiInput="
                        + strictRawDiffuseGiInput
                        + ",round7.shaderDenoise.rawDiffuseGiInput="
                        + strictRawDiffuseGiInput
                        + ",round7.shaderDenoise.directLightValidationInput="
                        + diagnosticDirectLightValidationFallback
                        + ",diagnosticDirectLightValidationFallback="
                        + diagnosticDirectLightValidationFallback
                        + ",diagnosticDirectLightValidationFallbackEnabled="
                        + directValidationFallbackEnabled
                        + ",diagnosticDirectLightValidationFallbackEnv="
                        + ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV
                        + ",shaderDenoiseOutputAttempted=true"
                        + ",round7.shaderDenoise.outputAttempted=true"
                        + ",shaderDenoiseOutputAttemptGeneration="
                        + outputTargetStatus.allocationGeneration()
                        + ",round7.shaderDenoise.outputAttemptGeneration="
                        + outputTargetStatus.allocationGeneration()
                        + ",shaderDenoiseOutputPassAttempted=true"
                        + ",shaderDenoiseOutputTextureAllocated=true"
                        + ",shaderDenoiseOwnedOutputImage=true"
                        + ",shaderDenoiseOutputRenderPassSubmitted=true"
                        + ",shaderDenoisePassExecuted=true"
                        + ",shaderGeneratedDenoisePassExecuted=true"
                        + ",shaderGeneratedOutputImageReady=true"
                        + ",shaderOutputSourceConsumed=true"
                        + ",shaderDenoiseOutputSourceConsumed=true"
                        + ",shaderDenoiseOutputConsumedByFinalComposite=true"
                        + ",shaderDenoisePassGeneratedVisualSource=true"
                        + ",shaderDenoiseFinalCompositeConsumable=true"
                        + ",finalCompositeConsumable=true"
                        + ",cpuReadbackFallbackActive=false"
                        + ",cpuReadbackFallbackInactive=true"
                        + ",shaderDenoiseCpuReadbackFallbackActive=false"
                        + ",shaderDenoiseCpuReadbackFallbackInactive=true"
                        + ",cpuDenoiseReadbackFallback=false"
                        + ",cpuDenoisedReadbackSource=false"
                        + ",rawGiCpuReadbackInput="
                        + strictRawDiffuseGiInput
                        + ",directLightValidationInput="
                        + diagnosticDirectLightValidationFallback
                        + ",shaderOwnedOutputImage=true"
                        + ",shaderDenoiseOutputImageReady=true"
                        + ",round7.shaderDenoise.outputImageReady=true"
                        + ",round7.shaderDenoise.outputMaterialReady=true"
                        + ",round7.shaderDenoise.shaderGeneratedOutput="
                        + strictRawDiffuseGiInput
                        + ",round7.shaderDenoise.realOutputReady="
                        + strictRawDiffuseGiInput
                        + ",realShaderDenoiseOutputReady="
                        + strictRawDiffuseGiInput
                        + ",shaderGeneratedDenoiseOutputEvidence="
                        + strictRawDiffuseGiInput
                        + ",shaderGeneratedDenoiseDiagnosticOutputEvidence="
                        + diagnosticDirectLightValidationFallback
                        + ",shaderDenoiseNoOverclaim="
                        + strictRawDiffuseGiInput
                        + ",shaderDenoiseOverclaimRejected="
                        + strictRawDiffuseGiInput
                        + ",shaderDenoiseOverclaimPresent="
                        + !strictRawDiffuseGiInput
                        + ",publicMojangShaderGeneratedVisualOutput=true"
                        + ",physicalGiTracingQuality=open"
                        + ",physicalGiEvidence=false"
                        + ",realTracedLightingConsumed=false"
                        + ",stillNotComputeBoundary=true"
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false"
                        + ",temporaryDirectLightSubstitution=false,rectangularWashout=false"
                        + ",readiness=\"shader denoise fragment pass consumed "
                        + shaderInputMode
                        + "/"
                        + shaderInputKind
                        + ", generated an owned RGBA8 denoise output image, and final composite consumed that image\""
                        + "; denoised source: "
                        + shaderDenoisedSource.summary()
                        + "; target: "
                        + targetAttachmentSummary(target)
                        + "; javaOpaquePublicFallback="
                        + target.attachmentMetadata().javaOpaque()
                        + "; shader input payload: "
                        + shaderInputSummary
                        + "; upload: "
                        + upload.summary()
                        + "; shader denoise output target: "
                        + outputTargetStatus.summary()
                        + "; generation draw scaffold: "
                        + generationDrawScaffold.summary()
                        + "; final composite draw scaffold: "
                        + finalCompositeDrawScaffold.summary()
        );
    }

    public static PublicMojangFinalCompositeSubmissionResult submitRound7FinalCompositePublicDraw(
            LucernaFramePassTarget target,
            Round6DiffuseGiCpuOutputPayload diffuseGiPayload,
            DenoisedDiffuseGiCpuOutputPayload denoisedGiPayload,
            Round6DiffuseGiPreviewCompositeState previewState
    ) {
        Round7RawGiVisualSource rawGiSource = Round7RawGiVisualSource.from(previewState, diffuseGiPayload);
        Round7DenoisedGiVisualSource denoisedGiSource = Round7DenoisedGiVisualSource.from(denoisedGiPayload);
        if (target == null || !target.available()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    false,
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.TARGET_MISSING,
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because no frame target is available; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
            );
        }
        if (!target.safeForAttachment()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.METADATA_ONLY,
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because the target is not HUD-safe; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
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
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because command encoder or color view is unavailable; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
            );
        }
        if (denoisedGiPayload == null) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because denoised diffuse-GI RGBA8 CPU payload is unavailable; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
            );
        }
        if (!denoisedGiPayload.readyForPreviewDraw()) {
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because denoised diffuse-GI RGBA8 CPU payload is not displayable: "
                            + denoisedGiPayload.debugSummary()
                            + "; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
            );
        }

        ByteBuffer rawSourceBuffer = null;
        DirectLightPreviewTextureUploadResult rawUpload = null;
        if (diffuseGiPayload != null && previewState != null && previewState.readyForFinalComposite(diffuseGiPayload)) {
            rawSourceBuffer = diffuseGiPayload.copyToByteBuffer();
            rawUpload = ROUND6_DIFFUSE_GI_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                    RenderSystem.getDevice(),
                    commandEncoder,
                    rawSourceBuffer,
                    diffuseGiPayload.width(),
                    diffuseGiPayload.height()
            );
        }

        DirectLightingCpuOutputPayload directSourcePayload = resolveNativeDirectLightCandidatePayload();
        ByteBuffer directSourceBuffer = null;
        DirectLightPreviewTextureUploadResult directUpload = null;
        if (directSourcePayload != null) {
            directSourceBuffer = directSourcePayload.copyToByteBuffer();
            directUpload = DIRECT_LIGHT_FINAL_COMPOSITE_TEXTURE_UPLOADER.upload(
                    RenderSystem.getDevice(),
                    commandEncoder,
                    directSourceBuffer,
                    directSourcePayload.width(),
                    directSourcePayload.height()
            );
        }

        ByteBuffer denoisedSourceBuffer = denoisedGiPayload.copyToByteBuffer();
        DirectLightPreviewTextureUploadResult denoisedUpload = ROUND7_DENOISED_GI_TEXTURE_UPLOADER.upload(
                RenderSystem.getDevice(),
                commandEncoder,
                denoisedSourceBuffer,
                denoisedGiPayload.width(),
                denoisedGiPayload.height()
        );
        if (!denoisedUpload.availableForDraw()) {
            Reference.reachabilityFence(rawSourceBuffer);
            Reference.reachabilityFence(directSourceBuffer);
            Reference.reachabilityFence(denoisedSourceBuffer);
            return PublicMojangFinalCompositeSubmissionResult.notSubmitted(
                    true,
                    target.attachmentMetadata().javaOpaque(),
                    PublicMojangFinalCompositeSubmissionResult.TargetStatus.JAVA_OPAQUE_OBJECTS_PRESENT,
                    "public Mojang Round 7 FINAL_COMPOSITE visual mode skipped because denoised diffuse-GI source texture upload was unavailable: "
                            + denoisedUpload.summary()
                            + "; raw upload: "
                            + (rawUpload == null ? "not-attempted" : rawUpload.summary())
                            + "; raw source: "
                            + rawGiSource.summary()
                            + "; denoised source: "
                            + denoisedGiSource.summary()
            );
        }

        boolean rawUploadAvailable = rawUpload != null && rawUpload.availableForDraw();
        boolean directUploadAvailable = directUpload != null && directUpload.availableForDraw();
        boolean diagnosticVisibleDrawEnabled = finalCompositeDiagnosticDrawEnabled();
        boolean diagnosticClearEnabled = finalCompositeDiagnosticClearEnabled();
        boolean diagnosticTracyBlitEnabled = finalCompositeDiagnosticTracyBlitEnabled();
        PublicMojangPreviewDrawScaffold drawScaffold;
        PublicMojangPreviewDrawScaffold directDrawScaffold = null;
        PublicMojangPreviewDrawScaffold diagnosticDrawScaffold = null;
        PublicMojangPreviewDrawScaffold diagnosticTracyBlitScaffold = null;
        try (RenderPass renderPass = createFullTargetRenderPass(
                commandEncoder,
                () -> "lucerna public Round 7 FINAL_COMPOSITE direct plus raw plus denoised visual draw pass",
                colorView,
                target
        )) {
            renderPass.disableScissor();
            drawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenRound7FinalCompositeVisualDraw(
                    renderPass,
                    rawUploadAvailable ? rawUpload.textureView() : null,
                    rawUploadAvailable ? rawUpload.sampler() : null,
                    denoisedUpload.textureView(),
                    denoisedUpload.sampler()
            );
            if (directUploadAvailable) {
                directDrawScaffold = PublicMojangPreviewDrawScaffolds.issueFullscreenDirectLightFinalCompositeDraw(
                        renderPass,
                        directUpload.textureView(),
                        directUpload.sampler()
                );
            }
            if (diagnosticVisibleDrawEnabled) {
                diagnosticDrawScaffold = PublicMojangPreviewDrawScaffolds.issueDiagnosticDirectLightPreviewDraw(renderPass);
            }
            if (diagnosticTracyBlitEnabled) {
                diagnosticTracyBlitScaffold = PublicMojangPreviewDrawScaffolds.issueTracyBlitDiagnosticDraw(
                        renderPass,
                        denoisedUpload.textureView(),
                        denoisedUpload.sampler()
                );
            }
        }
        boolean diagnosticClearSubmitted = false;
        if (diagnosticClearEnabled && lastWorldColorTexture != null) {
            commandEncoder.clearColorTexture(lastWorldColorTexture, new Vector4f(0.95F, 0.18F, 0.04F, 1.0F));
            diagnosticClearSubmitted = true;
        }
        commandEncoder.submit();
        Reference.reachabilityFence(rawSourceBuffer);
        Reference.reachabilityFence(directSourceBuffer);
        Reference.reachabilityFence(denoisedSourceBuffer);
        boolean finalBlendComplete = directUploadAvailable && rawUploadAvailable;
        return PublicMojangFinalCompositeSubmissionResult.submitted(
                drawScaffold.drawCallsIssued()
                        || (directDrawScaffold != null && directDrawScaffold.drawCallsIssued())
                        || (diagnosticDrawScaffold != null && diagnosticDrawScaffold.drawCallsIssued())
                        || (diagnosticTracyBlitScaffold != null && diagnosticTracyBlitScaffold.drawCallsIssued()),
                target.attachmentMetadata().javaOpaque(),
                PublicMojangFinalCompositeSubmissionResult.TargetStatus.READY,
                "public Mojang Round 7 FINAL_COMPOSITE visual render pass submitted; "
                        + "mode=FINAL_LUCERNA_COMPOSITE,mode=round7-final-composite"
                        + ",evidence=round7.composite.final.direct_raw_denoised"
                        + ",finalBlendComplete="
                        + finalBlendComplete
                        + ",readiness=\"denoised diffuse-GI CPU output is displayable"
                        + (rawUploadAvailable ? " and raw native diffuse-GI source is blended" : " and raw native diffuse-GI source is unavailable")
                        + (directUploadAvailable ? " and native direct-light emissive source is blended" : " and native direct-light emissive source is unavailable")
                        + "; "
                        + denoisedGiPayload.readinessBoundarySummary()
                        + "\",raw source: "
                        + rawGiSource.summary()
                        + "; denoised source: "
                        + denoisedGiSource.summary()
                        + "; direct native source payload: "
                        + (directSourcePayload == null ? "missing-or-no-candidate-evidence" : directSourcePayload.debugSummary())
                        + "; direct upload: "
                        + (directUpload == null ? "not-attempted" : directUpload.summary())
                        + "; target: "
                        + targetAttachmentSummary(target)
                        + "; javaOpaquePublicFallback="
                        + target.attachmentMetadata().javaOpaque()
                        + "; raw native diffuse-GI source payload: "
                        + (diffuseGiPayload == null ? "missing" : diffuseGiPayload.debugSummary())
                        + "; raw upload: "
                        + (rawUpload == null ? "not-attempted" : rawUpload.summary())
                        + "; denoised diffuse-GI CPU source payload: "
                        + denoisedGiPayload.debugSummary()
                        + "; denoised upload: "
                        + denoisedUpload.summary()
                        + "; draw scaffold: "
                        + drawScaffold.summary()
                        + "; direct draw scaffold: "
                        + (directDrawScaffold == null ? "not-attempted" : directDrawScaffold.summary())
                        + "; diagnostic visible draw: "
                        + (diagnosticDrawScaffold == null
                        ? "disabled; set LUCERNA_ROUND7_SURFACE_DRAW_DIAGNOSTIC=1 for controller draw-target diagnosis"
                        : diagnosticDrawScaffold.summary())
                        + "; diagnostic Tracy blit: "
                        + (diagnosticTracyBlitScaffold == null
                        ? "disabled; set LUCERNA_ROUND7_SURFACE_TRACY_BLIT_DIAGNOSTIC=1 for built-in blit diagnosis"
                        : diagnosticTracyBlitScaffold.summary())
                        + "; diagnostic clear: "
                        + (diagnosticClearSubmitted
                        ? "submitted; LUCERNA_ROUND7_SURFACE_CLEAR_DIAGNOSTIC=1 cleared cached main world color texture"
                        : "disabled; set LUCERNA_ROUND7_SURFACE_CLEAR_DIAGNOSTIC=1 for controller target diagnosis")
        );
    }

    private static boolean finalCompositeDiagnosticDrawEnabled() {
        String value = System.getenv("LUCERNA_ROUND7_SURFACE_DRAW_DIAGNOSTIC");
        return value != null
                && ("1".equals(value.trim())
                || "true".equalsIgnoreCase(value.trim())
                || "yes".equalsIgnoreCase(value.trim()));
    }

    private static boolean finalCompositeDiagnosticTracyBlitEnabled() {
        String value = System.getenv("LUCERNA_ROUND7_SURFACE_TRACY_BLIT_DIAGNOSTIC");
        return value != null
                && ("1".equals(value.trim())
                || "true".equalsIgnoreCase(value.trim())
                || "yes".equalsIgnoreCase(value.trim()));
    }

    private static boolean finalCompositeDiagnosticClearEnabled() {
        String value = System.getenv("LUCERNA_ROUND7_SURFACE_CLEAR_DIAGNOSTIC");
        return value != null
                && ("1".equals(value.trim())
                || "true".equalsIgnoreCase(value.trim())
                || "yes".equalsIgnoreCase(value.trim()));
    }

    private static boolean round7ShaderDenoiseDirectLightValidationFallbackEnabled() {
        return flagEnabled(System.getenv(ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV))
                || flagEnabled(System.getProperty(ROUND7_SHADER_DENOISE_DIRECT_LIGHT_VALIDATION_FALLBACK_ENV));
    }

    private static boolean flagEnabled(String value) {
        return value != null
                && ("1".equals(value.trim())
                || "true".equalsIgnoreCase(value.trim())
                || "yes".equalsIgnoreCase(value.trim()));
    }

    private static DirectLightingCpuOutputPayload resolveNativeDirectLightCandidatePayload() {
        DirectLightingCpuOutputPayload payload = LucernaController.getInstance().directLightingCpuOutputPayload();
        if (!hasNativeDirectLightCandidatePayload(payload) || !payload.readyForPreviewDraw()) {
            return null;
        }
        return payload;
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
