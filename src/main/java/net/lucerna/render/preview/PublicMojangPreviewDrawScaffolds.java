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
    private static final String FINAL_COMPOSITE_FULLSCREEN_MODE = "final-composite-native-direct-light-surface-additive";
    private static final String ROUND7_RAW_GI_FULLSCREEN_MODE =
            "round7-raw-gi-native-diffuse-source-additive";
    private static final String ROUND7_DENOISED_GI_FULLSCREEN_MODE =
            "round7-denoised-gi-first-practical-cpu-output-additive";
    private static final String ROUND7_FINAL_COMPOSITE_FULLSCREEN_MODE =
            "round7-final-composite-raw-plus-denoised-surface-additive";
    private static final String RAW_GI_SOURCE_IDENTITY =
            "sourceIdentity=native-diffuse-gi-rgba8/raw-gi";
    private static final String DENOISED_GI_SOURCE_IDENTITY =
            "sourceIdentity=cpu-denoised-diffuse-gi-rgba8/denoised-gi";
    private static final String FINAL_COMPOSITE_SOURCE_BOUNDARY =
            "sourceBoundary=full-target-scene-shaped-surface-projection,"
                    + "metadataOnly=false,proofMarker=false,focusWindowOnly=false,"
                    + "temporaryDirectLightSubstitution=false,rectangularWashoutRejected=true,"
                    + "geometryMaterialAwareProjection=pending-shader/native-quality";
    private static final String DIRECT_LIGHT_FINAL_COMPOSITE_SHADER =
            "lucerna:core/round6_native_diffuse_gi_surface";
    private static final String ROUND6_DIFFUSE_GI_SURFACE_SHADER =
            "lucerna:core/round6_native_diffuse_gi_surface";
    private static final String ROUND7_DENOISED_GI_VISUAL_SHADER =
            "lucerna:core/round7_denoised_gi_visual";
    private static final String ADDITIVE_RGBA8_COLOR_TARGET_STATE =
            "blend=ADDITIVE,colorTargetFormat=RGBA8_UNORM,colorWriteMask=WRITE_COLOR";
    private static final String DIAGNOSTIC_FULLSCREEN_MODE = "diagnostic-fullscreen-warm-additive";
    private static final int FULLSCREEN_TRIANGLE_FIRST_VERTEX = 0;
    private static final int FULLSCREEN_TRIANGLE_VERTEX_COUNT = 3;
    private static final int SINGLE_INSTANCE_COUNT = 1;
    private static final int FIRST_INSTANCE = 0;
    private static final int ROUND6_PUBLIC_FALLBACK_DRAW_REPEATS = 1;
    private static final int ROUND7_RAW_GI_DRAW_REPEATS = 1;
    private static final int ROUND7_DENOISED_GI_DRAW_REPEATS = 1;
    private static final int ROUND7_FINAL_RAW_GI_DRAW_REPEATS = 1;
    private static final int ROUND7_FINAL_DENOISED_GI_DRAW_REPEATS = 1;
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
                            "core/round7_denoised_gi_visual"
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
    private static final RenderPipeline ROUND7_RAW_GI_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/round7_raw_gi_visual_additive"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/round6_native_diffuse_gi_surface"
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
    private static final RenderPipeline ROUND7_DENOISED_GI_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/round7_denoised_gi_visual_additive"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/round6_native_diffuse_gi_surface"
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
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
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
                "public Mojang final composite can bind the native direct-light CPU payload texture and issue one full-target surface-source additive draw; shader="
                        + DIRECT_LIGHT_FINAL_COMPOSITE_SHADER
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
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
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
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
                "public Mojang final composite native direct-light surface-source additive draw issued; shader="
                        + DIRECT_LIGHT_FINAL_COMPOSITE_SHADER
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenRound7RawGiVisualDraw(
            RenderPass renderPass,
            GpuTextureView sourceView
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 RAW_GI visual draw scaffold is unavailable because no render pass is open"
            );
        }
        if (sourceView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    ROUND7_RAW_GI_ADDITIVE_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    ROUND7_RAW_GI_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang Round 7 RAW_GI visual APIs are present, but no native diffuse-GI source texture view is available"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                ROUND7_RAW_GI_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_RAW_GI_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 RAW_GI visual mode can bind the native diffuse-GI RGBA8 source texture and issue a full-target raw-source additive draw; shader="
                        + ROUND6_DIFFUSE_GI_SURFACE_SHADER
                        + "," + RAW_GI_SOURCE_IDENTITY
                        + ",surfaceProjection=scene-shaped-full-target"
                        + ",metadataOnly=false,temporaryDirectLightSubstitution=false"
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound7RawGiVisualDraw(
            RenderPass renderPass,
            GpuTextureView sourceView,
            GpuSampler sourceSampler
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenRound7RawGiVisualDraw(
                renderPass,
                sourceView
        );
        if (!scaffold.drawPrepared()) {
            return scaffold;
        }
        if (sourceView == null) {
            return scaffold;
        }
        if (sourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 RAW_GI visual draw skipped because no native diffuse-GI source sampler is available"
            );
        }

        renderPass.setPipeline(ROUND7_RAW_GI_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, sourceView, sourceSampler);
        for (int drawIndex = 0; drawIndex < ROUND7_RAW_GI_DRAW_REPEATS; drawIndex++) {
            renderPass.draw(
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FIRST_INSTANCE
            );
        }
        return PublicMojangPreviewDrawScaffold.issued(
                ROUND7_RAW_GI_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_RAW_GI_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 RAW_GI native diffuse-GI source additive draw issued; shader="
                        + ROUND6_DIFFUSE_GI_SURFACE_SHADER
                        + "," + RAW_GI_SOURCE_IDENTITY
                        + ",surfaceProjection=scene-shaped-full-target"
                        + ",metadataOnly=false,temporaryDirectLightSubstitution=false"
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
                        + ",javaOpaqueFallbackDrawRepeats=" + ROUND7_RAW_GI_DRAW_REPEATS
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenRound7DenoisedGiVisualDraw(
            RenderPass renderPass,
            GpuTextureView sourceView
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 DENOISED_GI visual draw scaffold is unavailable because no render pass is open"
            );
        }
        if (sourceView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    ROUND7_DENOISED_GI_ADDITIVE_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    ROUND7_DENOISED_GI_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang Round 7 DENOISED_GI visual APIs are present, but no denoised diffuse-GI source texture view is available"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                ROUND7_DENOISED_GI_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_DENOISED_GI_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 DENOISED_GI visual mode can bind the denoised diffuse-GI RGBA8 CPU source texture and issue a stronger full-target denoised-source additive draw; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + "," + DENOISED_GI_SOURCE_IDENTITY
                        + ",surfaceProjection=scene-shaped-full-target"
                        + ",metadataOnly=false,temporaryDirectLightSubstitution=false"
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound7DenoisedGiVisualDraw(
            RenderPass renderPass,
            GpuTextureView sourceView,
            GpuSampler sourceSampler
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenRound7DenoisedGiVisualDraw(
                renderPass,
                sourceView
        );
        if (!scaffold.drawPrepared()) {
            return scaffold;
        }
        if (sourceView == null) {
            return scaffold;
        }
        if (sourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 DENOISED_GI visual draw skipped because no denoised diffuse-GI source sampler is available"
            );
        }

        renderPass.setPipeline(ROUND7_DENOISED_GI_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, sourceView, sourceSampler);
        for (int drawIndex = 0; drawIndex < ROUND7_DENOISED_GI_DRAW_REPEATS; drawIndex++) {
            renderPass.draw(
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FIRST_INSTANCE
            );
        }
        return PublicMojangPreviewDrawScaffold.issued(
                ROUND7_DENOISED_GI_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_DENOISED_GI_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 DENOISED_GI denoised diffuse-GI CPU output additive draw issued; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + "," + DENOISED_GI_SOURCE_IDENTITY
                        + ",surfaceProjection=scene-shaped-full-target"
                        + ",metadataOnly=false,temporaryDirectLightSubstitution=false"
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
                        + ",javaOpaqueFallbackDrawRepeats=" + ROUND7_DENOISED_GI_DRAW_REPEATS
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound7FinalCompositeVisualDraw(
            RenderPass renderPass,
            GpuTextureView rawGiSourceView,
            GpuSampler rawGiSourceSampler,
            GpuTextureView denoisedGiSourceView,
            GpuSampler denoisedGiSourceSampler
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 FINAL_COMPOSITE visual draw skipped because no render pass is open"
            );
        }
        if (denoisedGiSourceView == null || denoisedGiSourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 FINAL_COMPOSITE visual draw skipped because no denoised diffuse-GI source texture is available"
            );
        }

        boolean rawSourceBound = rawGiSourceView != null && rawGiSourceSampler != null;
        if (rawSourceBound) {
            renderPass.setPipeline(ROUND7_RAW_GI_ADDITIVE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, rawGiSourceView, rawGiSourceSampler);
            for (int drawIndex = 0; drawIndex < ROUND7_FINAL_RAW_GI_DRAW_REPEATS; drawIndex++) {
                renderPass.draw(
                        FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                        SINGLE_INSTANCE_COUNT,
                        FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                        FIRST_INSTANCE
                );
            }
        }

        renderPass.setPipeline(ROUND7_DENOISED_GI_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, denoisedGiSourceView, denoisedGiSourceSampler);
        for (int drawIndex = 0; drawIndex < ROUND7_FINAL_DENOISED_GI_DRAW_REPEATS; drawIndex++) {
            renderPass.draw(
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FIRST_INSTANCE
            );
        }

        return PublicMojangPreviewDrawScaffold.issued(
                ROUND7_DENOISED_GI_ADDITIVE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_FINAL_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 FINAL_COMPOSITE full-target additive draw issued from "
                        + (rawSourceBound ? "raw native diffuse-GI plus " : "")
                        + "denoised diffuse-GI sources; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + "," + (rawSourceBound ? RAW_GI_SOURCE_IDENTITY + "," : "")
                        + DENOISED_GI_SOURCE_IDENTITY
                        + "," + FINAL_COMPOSITE_SOURCE_BOUNDARY
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
                        + ",rawDrawRepeats=" + (rawSourceBound ? ROUND7_FINAL_RAW_GI_DRAW_REPEATS : 0)
                        + ",denoisedDrawRepeats=" + ROUND7_FINAL_DENOISED_GI_DRAW_REPEATS
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenRound6DiffuseGiFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView sourceView
    ) {
        return describeFullscreenRound7RawGiVisualDraw(renderPass, sourceView);
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound6DiffuseGiFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView sourceView,
            GpuSampler sourceSampler
    ) {
        return issueFullscreenRound7RawGiVisualDraw(renderPass, sourceView, sourceSampler);
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
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
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

    public static PublicMojangPreviewDrawScaffold issueTracyBlitDiagnosticDraw(
            RenderPass renderPass,
            GpuTextureView sourceView,
            GpuSampler sourceSampler
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Tracy blit diagnostic skipped because no render pass is open"
            );
        }
        if (sourceView == null || sourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Tracy blit diagnostic skipped because no source texture is available"
            );
        }

        renderPass.setPipeline(RenderPipelines.TRACY_BLIT);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, sourceView, sourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                RenderPipelines.TRACY_BLIT,
                DIRECT_LIGHT_SOURCE_BINDING,
                "diagnostic-tracy-blit-fullscreen-replace",
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Tracy blit diagnostic draw issued against final composite target"
        );
    }
}
