package net.lucerna.render.preview;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class PublicMojangPreviewDrawScaffolds {
    private static final String DIRECT_LIGHT_SOURCE_BINDING = "InSampler";
    private static final String NO_TEXTURE_BINDING = "none";
    private static final String TEXTURED_FULLSCREEN_MODE = "surface-sample-masked-direct-light-additive";
    private static final String FINAL_COMPOSITE_FULLSCREEN_MODE = "final-composite-direct-light-additive";
    private static final String DIAGNOSTIC_FULLSCREEN_MODE = "diagnostic-fullscreen-warm-additive";
    private static final int FULLSCREEN_TRIANGLE_FIRST_VERTEX = 0;
    private static final int FULLSCREEN_TRIANGLE_VERTEX_COUNT = 3;
    private static final int SINGLE_INSTANCE_COUNT = 1;
    private static final int FIRST_INSTANCE = 0;
    private static final RenderPipeline DIAGNOSTIC_DIRECT_LIGHT_PREVIEW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/direct_light_preview_diagnostic"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/direct_light_preview_diagnostic"
                    ))
                    .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline DIRECT_LIGHT_PREVIEW_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/direct_light_preview_additive"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/direct_light_preview_additive"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.ADDITIVE),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline DIRECT_LIGHT_FINAL_COMPOSITE_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/direct_light_final_composite_additive"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/direct_light_preview_additive"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.ADDITIVE),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );

    private PublicMojangPreviewDrawScaffolds() {
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenDirectLightPreviewDraw(
            RenderPass renderPass,
            GpuTextureView directLightSourceView
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang preview draw scaffold is unavailable because no render pass is open"
            );
        }
        if (directLightSourceView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    DIRECT_LIGHT_PREVIEW_ADDITIVE_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    TEXTURED_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang fullscreen draw APIs are present, but no Lucerna direct-light source texture view is available yet"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                DIRECT_LIGHT_PREVIEW_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                TEXTURED_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang surface-sample direct-light preview draw can bind a masked source texture and issue one bounded draw"
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenDirectLightPreviewDraw(
            RenderPass renderPass,
            GpuTextureView directLightSourceView,
            GpuSampler directLightSourceSampler
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenDirectLightPreviewDraw(
                renderPass,
                directLightSourceView
        );
        if (!scaffold.drawPrepared()) {
            return scaffold;
        }
        if (directLightSourceView == null) {
            return scaffold;
        }
        if (directLightSourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang preview draw skipped because no direct-light source sampler is available"
            );
        }

        renderPass.setPipeline(DIRECT_LIGHT_PREVIEW_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, directLightSourceView, directLightSourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                DIRECT_LIGHT_PREVIEW_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                TEXTURED_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang surface-sample masked direct-light preview draw issued"
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenDirectLightFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView directLightSourceView
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang final composite draw scaffold is unavailable because no render pass is open"
            );
        }
        if (directLightSourceView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    DIRECT_LIGHT_FINAL_COMPOSITE_ADDITIVE_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    FINAL_COMPOSITE_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang final composite draw APIs are present, but no Lucerna direct-light source texture view is available"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                DIRECT_LIGHT_FINAL_COMPOSITE_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                FINAL_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang final composite can bind the native direct-light CPU payload texture and issue one bounded draw"
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenDirectLightFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView directLightSourceView,
            GpuSampler directLightSourceSampler
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenDirectLightFinalCompositeDraw(
                renderPass,
                directLightSourceView
        );
        if (!scaffold.drawPrepared()) {
            return scaffold;
        }
        if (directLightSourceView == null) {
            return scaffold;
        }
        if (directLightSourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang final composite draw skipped because no direct-light source sampler is available"
            );
        }

        renderPass.setPipeline(DIRECT_LIGHT_FINAL_COMPOSITE_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, directLightSourceView, directLightSourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                DIRECT_LIGHT_FINAL_COMPOSITE_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                FINAL_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang final composite direct-light draw issued"
        );
    }

    public static PublicMojangPreviewDrawScaffold issueDiagnosticDirectLightPreviewDraw(
            RenderPass renderPass
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang diagnostic preview draw skipped because no render pass is open"
            );
        }

        renderPass.setPipeline(DIAGNOSTIC_DIRECT_LIGHT_PREVIEW_PIPELINE);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                DIAGNOSTIC_DIRECT_LIGHT_PREVIEW_PIPELINE,
                NO_TEXTURE_BINDING,
                DIAGNOSTIC_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang diagnostic direct-light preview draw issued"
        );
    }
}
