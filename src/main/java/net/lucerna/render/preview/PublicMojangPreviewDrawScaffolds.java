package net.lucerna.render.preview;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

public final class PublicMojangPreviewDrawScaffolds {
    private static final String DIRECT_LIGHT_SOURCE_BINDING = "InSampler";
    private static final String NO_TEXTURE_BINDING = "none";
    private static final String TEXTURED_FULLSCREEN_MODE = "textured-fullscreen-direct-light";
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
                    RenderPipelines.ENTITY_OUTLINE_BLIT,
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
                RenderPipelines.ENTITY_OUTLINE_BLIT,
                DIRECT_LIGHT_SOURCE_BINDING,
                TEXTURED_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang fullscreen direct-light preview draw can bind a source texture and issue one bounded draw"
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

        renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, directLightSourceView, directLightSourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                RenderPipelines.ENTITY_OUTLINE_BLIT,
                DIRECT_LIGHT_SOURCE_BINDING,
                TEXTURED_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang fullscreen direct-light preview draw issued"
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
