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
    private static final String SAMPLER0_BINDING = "Sampler0";
    private static final String SAMPLER1_BINDING = "Sampler1";
    private static final String NO_TEXTURE_BINDING = "none";
    private static final String TEXTURED_FULLSCREEN_MODE = "source-gated-direct-light-surface-additive";
    private static final String FINAL_COMPOSITE_FULLSCREEN_MODE = "final-composite-native-direct-light-source-gated-additive";
    private static final String NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE =
            "final-composite-native-shadow-map-mask-occlusion";
    private static final String ROUND7_RAW_GI_FULLSCREEN_MODE =
            "round7-raw-gi-native-diffuse-source-additive";
    private static final String ROUND7_DENOISED_GI_FULLSCREEN_MODE =
            "round7-denoised-gi-first-practical-cpu-output-additive";
    private static final String ROUND7_SHADER_DENOISE_OUTPUT_FULLSCREEN_MODE =
            "round7-shader-generated-denoise-output-fragment-color-attachment";
    private static final String ROUND7_FINAL_COMPOSITE_FULLSCREEN_MODE =
            "round7-final-composite-source-separated-scene-surface-additive";
    private static final String DAYTIME_WORLD_SHADOW_FULLSCREEN_MODE =
            "daytime-world-shadow-translucent";
    private static final String CINEMATIC_DAYLIGHT_BLOOM_FULLSCREEN_MODE =
            "cinematic-daylight-bloom-additive";
    private static final String CINEMATIC_GRADE_VIGNETTE_FULLSCREEN_MODE =
            "cinematic-grade-vignette-translucent";
    private static final String CINEMATIC_ATMOSPHERE_CLOUDS_FULLSCREEN_MODE =
            "cinematic-atmosphere-clouds-translucent";
    private static final String CINEMATIC_SURFACE_BOUNCE_FULLSCREEN_MODE =
            "cinematic-surface-bounce-translucent";
    private static final String RAW_GI_SOURCE_IDENTITY =
            "sourceIdentity=native-diffuse-gi-rgba8/raw-gi";
    private static final String DENOISED_GI_SOURCE_IDENTITY =
            "sourceIdentity=cpu-denoised-diffuse-gi-rgba8+public-mojang-visual-filter/denoised-gi";
    private static final String SHADER_DENOISE_OUTPUT_SOURCE_IDENTITY =
            "sourceIdentity=shader-denoised-diffuse-gi-rgba8/public-mojang-fragment-color-attachment";
    private static final String SHADER_DENOISE_OUTPUT_SUBMISSION_BOUNDARY =
            "sourceKind=shader-generated-denoised-gi,"
                    + "shaderGeneratedDenoisedGI=public-mojang-fragment-pass,"
                    + "shaderGeneratedDenoiseOutputKind=fragment-color-attachment,"
                    + "shaderDenoisePassIdentity=lucerna.denoise.diffuse.public-mojang-fragment,"
                    + "shaderDenoiseVisualShaderIntent=true,"
                    + "shaderDenoiseDispatchPrepared=false,"
                    + "shaderDenoiseFragmentPassPrepared=true,"
                    + "shaderDenoiseInputReady=true,"
                    + "shaderDenoiseInputsCompleteForDispatch=false,"
                    + "shaderDenoiseFragmentInputsCompleteForCurrentMojangPass=true,"
                    + "shaderDenoiseDepthMaterialInputsBound=false,"
                    + "shaderDenoiseMaterialDepthPreservation=raw-signal-edge-fallback,"
                    + "shaderDenoiseFullGeometryGuidanceReady=false,"
                    + "shaderDenoiseRequiredDepthMaterialInputsPending=true,"
                    + "shaderDenoiseOutputPassAttempted=true,"
                    + "shaderDenoiseOutputTextureAllocated=true,"
                    + "shaderDenoiseOwnedOutputImage=true,"
                    + "shaderDenoiseOutputRenderPassSubmitted=true,"
                    + "shaderDenoiseColorAttachmentWrite=true,"
                    + "shaderDenoiseStorageImageWrite=false,"
                    + "shaderDenoiseComputeDispatch=false,"
                    + "shaderDenoiseComputeBarrierReady=false,"
                    + "shaderDenoiseColorAttachmentTransitionRequired=true,"
                    + "shaderDenoisePassExecuted=true,"
                    + "shaderGeneratedDenoisePassExecuted=true,"
                    + "shaderOutputSourceConsumed=true,"
                    + "shaderDenoiseOutputSourceConsumed=true,"
                    + "shaderDenoiseOutputConsumedByFinalComposite=true,"
                    + "shaderDenoisePassGeneratedVisualSource=true,"
                    + "shaderDenoiseFinalCompositeConsumable=true,"
                    + "finalCompositeConsumable=true,"
                    + "cpuReadbackFallbackActive=false,"
                    + "cpuReadbackFallbackInactive=true,"
                    + "shaderDenoiseCpuReadbackFallbackActive=false,"
                    + "shaderDenoiseCpuReadbackFallbackInactive=true,"
                    + "cpuDenoiseReadbackFallback=false,"
                    + "cpuDenoisedReadbackSource=false,"
                    + "rawGiCpuReadbackInput=true,"
                    + "shaderOwnedOutputImage=true,"
                    + "shaderDenoiseOutputImageReady=true,"
                    + "round7.shaderDenoise.outputImageReady=true,"
                    + "round7.shaderDenoise.outputMaterialReady=true,"
                    + "round7.shaderDenoise.shaderGeneratedOutput=true,"
                    + "round7.shaderDenoise.realOutputReady=true,"
                    + "realShaderDenoiseOutputReady=true,"
                    + "shaderGeneratedDenoiseOutputEvidence=true,"
                    + "publicMojangShaderGeneratedVisualOutput=true,"
                    + "stillNotComputeBoundary=true,"
                    + "notStorageImageBoundary=true,"
                    + "metadataOnly=false,proofMarker=false,focusWindowOnly=false,"
                    + "temporaryDirectLightSubstitution=false,rectangularWashout=false";
    private static final String FINAL_COMPOSITE_SOURCE_BOUNDARY =
            "sourceBoundary=full-target-source-gated-scene-surface-projection,"
                    + "metadataOnly=false,proofMarker=false,focusWindowOnly=false,"
                    + "temporaryDirectLightSubstitution=false,rectangularWashoutRejected=true,"
                    + "physicalLightingEvidence=source-gated-surface-shaped-preview,"
                    + "cpuDenoisedSource=true,"
                    + "publicMojangShaderVisualOutput=true,"
                    + "shaderGeneratedDenoisedGI=false-public-mojang-visual-only,"
                    + "round7.shaderDenoise.outputImageReady=false,"
                    + "round7.shaderDenoise.outputMaterialReady=true,"
                    + "round7.shaderDenoise.shaderGeneratedOutput=false,"
                    + "round7.shaderDenoise.realOutputReady=false,"
                    + "realShaderDenoiseOutputReady=false,"
                    + "geometryMaterialAwareProjection=pending-shader/native-quality";
    private static final String DIRECT_LIGHT_FINAL_COMPOSITE_SHADER =
            "lucerna:core/direct_light_final_composite_focus";
    private static final String NATIVE_SHADOW_MAP_FINAL_COMPOSITE_SHADER =
            "lucerna:composite/native_shadow_mask_composite";
    private static final String NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_SHADER =
            "lucerna:composite/depth_aware_shadow_mask_composite";
    private static final String ROUND6_DIFFUSE_GI_SURFACE_SHADER =
            "lucerna:core/round6_native_diffuse_gi_surface";
    private static final String ROUND7_DENOISED_GI_VISUAL_SHADER =
            "lucerna:core/round7_denoised_gi_visual";
    private static final String ROUND7_SHADER_GENERATED_DENOISE_OUTPUT_SHADER =
            "lucerna:denoise/shader_generated_diffuse_output";
    private static final String ADDITIVE_RGBA8_COLOR_TARGET_STATE =
            "blend=ADDITIVE,colorTargetFormat=RGBA8_UNORM,colorWriteMask=WRITE_COLOR";
    private static final String REPLACE_RGBA8_COLOR_TARGET_STATE =
            "blend=NONE,colorTargetFormat=RGBA8_UNORM,colorWriteMask=WRITE_COLOR";
    private static final String TRANSLUCENT_RGBA8_COLOR_TARGET_STATE =
            "blend=TRANSLUCENT,colorTargetFormat=RGBA8_UNORM,colorWriteMask=WRITE_COLOR";
    private static final String DIAGNOSTIC_FULLSCREEN_MODE = "diagnostic-fullscreen-warm-additive";
    private static final int FULLSCREEN_TRIANGLE_FIRST_VERTEX = 0;
    private static final int FULLSCREEN_TRIANGLE_VERTEX_COUNT = 3;
    private static final int SINGLE_INSTANCE_COUNT = 1;
    private static final int FIRST_INSTANCE = 0;
    private static final int ROUND6_PUBLIC_FALLBACK_DRAW_REPEATS = 1;
    private static final int ROUND7_RAW_GI_DRAW_REPEATS = 1;
    private static final int ROUND7_DENOISED_GI_DRAW_REPEATS = 1;
    private static final int ROUND7_FINAL_RAW_GI_DRAW_REPEATS = 0;
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
                            "composite/native_shadow_mask_composite"
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
    private static final RenderPipeline NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/native_shadow_map_final_composite_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "composite/native_shadow_mask_composite"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline NATIVE_SHADOW_MAP_FINAL_COMPOSITE_SPLIT_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/native_shadow_map_final_composite_split_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "composite/native_shadow_mask_composite_split"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/native_depth_aware_shadow_map_final_composite_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "composite/depth_aware_shadow_mask_composite"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_SPLIT_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/native_depth_aware_shadow_map_final_composite_split_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "composite/depth_aware_shadow_mask_composite_split"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1)
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline DAYTIME_WORLD_SHADOW_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/daytime_world_shadow_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/daytime_world_shadow_overlay"
                    ))
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline CINEMATIC_DAYLIGHT_BLOOM_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/cinematic_daylight_bloom_additive"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/cinematic_daylight_bloom"
                    ))
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.ADDITIVE),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline CINEMATIC_GRADE_VIGNETTE_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/cinematic_grade_vignette_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/cinematic_grade_vignette"
                    ))
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline CINEMATIC_ATMOSPHERE_CLOUDS_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/cinematic_atmosphere_clouds_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/cinematic_atmosphere_clouds"
                    ))
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline CINEMATIC_SURFACE_BOUNCE_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/cinematic_surface_bounce_translucent"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/cinematic_surface_bounce"
                    ))
                    .withColorTargetState(new ColorTargetState(
                            Optional.of(BlendFunction.TRANSLUCENT),
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
    private static final RenderPipeline ROUND7_SHADER_DENOISE_OUTPUT_GENERATE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/round7_shader_denoise_output_generate"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "denoise/shader_generated_diffuse_output"
                    ))
                    .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
                    .withColorTargetState(new ColorTargetState(
                            Optional.empty(),
                            GpuFormat.RGBA8_UNORM,
                            ColorTargetState.WRITE_COLOR
                    ))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );
    private static final RenderPipeline ROUND7_SHADER_DENOISE_OUTPUT_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/round7_shader_denoise_output_final_composite"
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
    private static final RenderPipeline ROUND7_SHADER_DENOISE_OUTPUT_SPLIT_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "pipeline/round7_shader_denoise_output_split_additive_final_composite"
                    ))
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(
                            "lucerna",
                            "core/round7_denoised_gi_visual_split"
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

    private static boolean splitScreenVisualProofEnabled() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("LUCERNA_SPLIT_SCREEN_VISUAL_PROOF", "false"));
    }

    private static RenderPipeline nativeShadowMapFinalCompositePipeline() {
        return splitScreenVisualProofEnabled()
                ? NATIVE_SHADOW_MAP_FINAL_COMPOSITE_SPLIT_TRANSLUCENT_PIPELINE
                : NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE;
    }

    private static RenderPipeline nativeDepthAwareShadowMapFinalCompositePipeline() {
        return splitScreenVisualProofEnabled()
                ? NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_SPLIT_TRANSLUCENT_PIPELINE
                : NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE;
    }

    private static RenderPipeline shaderDenoiseOutputFinalCompositePipeline() {
        return splitScreenVisualProofEnabled()
                ? ROUND7_SHADER_DENOISE_OUTPUT_SPLIT_ADDITIVE_PIPELINE
                : ROUND7_SHADER_DENOISE_OUTPUT_ADDITIVE_PIPELINE;
    }

    private static String shaderDenoiseOutputFinalCompositeColorTargetState() {
        return splitScreenVisualProofEnabled()
                ? ADDITIVE_RGBA8_COLOR_TARGET_STATE
                : ADDITIVE_RGBA8_COLOR_TARGET_STATE;
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
                "public Mojang direct-light preview draw can bind a source-gated surface texture and issue one bounded HUD-safe draw"
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
                "public Mojang source-gated direct-light surface preview draw issued; proofMarker=false,focusWindowOnly=false,metadataOnly=false"
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
                "public Mojang final composite can bind the native direct-light CPU payload texture and issue one full-target source-gated surface additive draw; shader="
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
                "public Mojang final composite native direct-light sampled additive draw issued; "
                        + "cleanGameplayComposite=true,experimentalVisualStack=false,"
                        + "lowResolutionDirectTextureDraw=true,cinematicCompositeUsesShadowAndBloomPasses=false,"
                        + "proofMarker=false,focusWindowOnly=false,metadataOnly=false,shader="
                        + DIRECT_LIGHT_FINAL_COMPOSITE_SHADER
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenNativeShadowMapFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView shadowMapMaskView,
            boolean realShadowMapOutputReady
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang native shadow-map composite draw scaffold is unavailable because no render pass is open"
            );
        }
        if (!realShadowMapOutputReady) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang native shadow-map composite draw APIs are present, but the native shadow-map output is not ready; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=false"
            );
        }
        if (shadowMapMaskView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang native shadow-map composite draw APIs are present, but no native shadow-map mask texture view is available; "
                            + "nativeShadowMapComposite=false,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                            + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang native shadow-map composite can bind the native shadow-mask payload texture and issue one translucent full-target occlusion draw; "
                        + "nativeShadowMapComposite=true,shadowMapOutputConsumed=false,screenSpaceShadowDecal=false,"
                        + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,shader="
                        + NATIVE_SHADOW_MAP_FINAL_COMPOSITE_SHADER
                        + "," + TRANSLUCENT_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenNativeShadowMapFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView shadowMapMaskView,
            GpuSampler shadowMapMaskSampler,
            boolean realShadowMapOutputReady
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenNativeShadowMapFinalCompositeDraw(
                renderPass,
                shadowMapMaskView,
                realShadowMapOutputReady
        );
        if (!scaffold.drawPrepared()) {
            return scaffold;
        }
        if (!realShadowMapOutputReady || shadowMapMaskView == null) {
            return scaffold;
        }
        if (shadowMapMaskSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang native shadow-map composite draw skipped because no shadow-map mask sampler is available"
            );
        }

        renderPass.setPipeline(nativeShadowMapFinalCompositePipeline());
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, shadowMapMaskView, shadowMapMaskSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );

        return PublicMojangPreviewDrawScaffold.issued(
                NATIVE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang native shadow-map mask final-composite occlusion draw issued before HUD; "
                        + "nativeShadowMapComposite=true,shadowMapOutputConsumed=true,screenSpaceShadowDecal=false,"
                        + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,"
                        + "shadowMaskPayloadTexture=true,shadowMaskExpectedRgbZeroAlphaSignal=true,shader="
                        + NATIVE_SHADOW_MAP_FINAL_COMPOSITE_SHADER
                        + "," + TRANSLUCENT_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenDepthAwareNativeShadowMapFinalCompositeDraw(
            RenderPass renderPass,
            GpuTextureView shadowMapMaskView,
            GpuSampler shadowMapMaskSampler,
            GpuTextureView currentDepthView,
            GpuSampler currentDepthSampler,
            boolean realShadowMapOutputReady
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang depth-aware native shadow-map composite draw scaffold is unavailable because no render pass is open"
            );
        }
        if (!realShadowMapOutputReady || shadowMapMaskView == null || currentDepthView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                    SAMPLER0_BINDING + "+" + SAMPLER1_BINDING,
                    NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang depth-aware native shadow-map composite draw APIs are present, but required source/depth bindings are not ready; "
                            + "depthAwareShadowMaskComposite=false,shaderPassDepthSamplingEvidence=false,"
                            + "depthSamplingPassOutputsReady=false,nativeShadowMapComposite=false,shadowMapOutputConsumed=false,"
                            + "screenSpaceShadowDecal=false,lowResolutionDirectTextureDraw=false,realShadowMapOutputReady="
                            + realShadowMapOutputReady
            );
        }
        if (shadowMapMaskSampler == null || currentDepthSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang depth-aware native shadow-map composite draw skipped because one or more samplers are unavailable"
            );
        }

        renderPass.setPipeline(nativeDepthAwareShadowMapFinalCompositePipeline());
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(SAMPLER0_BINDING, shadowMapMaskView, shadowMapMaskSampler);
        renderPass.bindTexture(SAMPLER1_BINDING, currentDepthView, currentDepthSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );

        return PublicMojangPreviewDrawScaffold.issued(
                NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_TRANSLUCENT_PIPELINE,
                SAMPLER0_BINDING + "+" + SAMPLER1_BINDING,
                NATIVE_SHADOW_MAP_COMPOSITE_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang depth-aware native shadow-map final-composite draw issued; "
                        + "depthAwareShadowMaskComposite=true,shaderPassDepthSamplingEvidence=true,"
                        + "depthSamplingPassOutputsReady=true,g_buffer_depth_sampling_evidence=true,"
                        + "g_buffer_depth_texture_sampled=true,g_buffer_depth_metadata_only=false,"
                        + "depthSamplingPassOutputsMarker=java_native_shader_depth_sampling_evidence_parsed,"
                        + "nativeShadowMapComposite=true,shadowMapOutputConsumed=true,screenSpaceShadowDecal=false,"
                        + "lowResolutionDirectTextureDraw=false,realShadowMapOutputReady=true,shader="
                        + NATIVE_DEPTH_AWARE_SHADOW_MAP_FINAL_COMPOSITE_SHADER
                        + "," + TRANSLUCENT_RGBA8_COLOR_TARGET_STATE
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
                        + ",surfaceProjection=source-gated-scene-shaped-full-target"
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false,temporaryDirectLightSubstitution=false"
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
                        + ",surfaceProjection=source-gated-scene-shaped-full-target"
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false,temporaryDirectLightSubstitution=false"
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
                        + ",surfaceProjection=source-gated-scene-shaped-full-target"
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false,temporaryDirectLightSubstitution=false"
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
                        + ",metadataOnly=false,proofMarker=false,focusWindowOnly=false"
                        + ",temporaryDirectLightSubstitution=false,rectangularWashout=false"
                        + ",cpuDenoisedSource=true"
                        + ",publicMojangShaderVisualOutput=true"
                        + ",shaderGeneratedDenoisedGI=false-public-mojang-visual-only"
                        + ",round7.shaderDenoise.outputImageReady=false"
                        + ",round7.shaderDenoise.outputMaterialReady=true"
                        + ",round7.shaderDenoise.shaderGeneratedOutput=false"
                        + ",round7.shaderDenoise.realOutputReady=false"
                        + ",realShaderDenoiseOutputReady=false"
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
                        + ",javaOpaqueFallbackDrawRepeats=" + ROUND7_DENOISED_GI_DRAW_REPEATS
        );
    }

    public static PublicMojangPreviewDrawScaffold describeFullscreenRound7ShaderDenoiseOutputDraw(
            RenderPass renderPass,
            GpuTextureView sourceView
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 shader-denoise output draw scaffold is unavailable because no render pass is open"
            );
        }
        if (sourceView == null) {
            return PublicMojangPreviewDrawScaffold.prepared(
                    ROUND7_SHADER_DENOISE_OUTPUT_ADDITIVE_PIPELINE,
                    DIRECT_LIGHT_SOURCE_BINDING,
                    ROUND7_SHADER_DENOISE_OUTPUT_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang Round 7 shader-denoise fragment-output APIs are present, but no raw diffuse-GI source texture view is available; "
                            + "shaderDenoisePassExecuted=false,shaderOutputSourceConsumed=false,"
                            + "shaderDenoiseFragmentPassPrepared=false,shaderDenoiseColorAttachmentWrite=false,"
                            + "shaderDenoiseComputeDispatch=false,shaderDenoiseStorageImageWrite=false,"
                            + "shaderDenoiseFinalCompositeConsumable=false,finalCompositeConsumable=false,"
                            + "cpuReadbackFallbackActive=false,shaderDenoiseCpuReadbackFallbackActive=false"
            );
        }

        return PublicMojangPreviewDrawScaffold.prepared(
                shaderDenoiseOutputFinalCompositePipeline(),
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_SHADER_DENOISE_OUTPUT_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 shader-denoise output mode can bind the shader-owned denoise output texture for final-color sampling; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + "," + SHADER_DENOISE_OUTPUT_SOURCE_IDENTITY
                        + "," + SHADER_DENOISE_OUTPUT_SUBMISSION_BOUNDARY
                        + "," + shaderDenoiseOutputFinalCompositeColorTargetState()
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound7ShaderDenoiseOutputGenerationDraw(
            RenderPass renderPass,
            GpuTextureView rawSourceView,
            GpuSampler rawSourceSampler
    ) {
        if (renderPass == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 shader-generated denoise output draw skipped because no output render pass is open"
            );
        }
        if (rawSourceView == null || rawSourceSampler == null) {
            return PublicMojangPreviewDrawScaffold.unavailable(
                    "public Mojang Round 7 shader-generated denoise output draw skipped because no raw diffuse-GI source sampler is available"
            );
        }

        renderPass.setPipeline(ROUND7_SHADER_DENOISE_OUTPUT_GENERATE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, rawSourceView, rawSourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                ROUND7_SHADER_DENOISE_OUTPUT_GENERATE_PIPELINE,
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_SHADER_DENOISE_OUTPUT_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 shader-generated denoise output fragment color-attachment pass issued into lucerna_shader_denoise_output_rgba; shader="
                        + ROUND7_SHADER_GENERATED_DENOISE_OUTPUT_SHADER
                        + "," + SHADER_DENOISE_OUTPUT_SOURCE_IDENTITY
                        + "," + SHADER_DENOISE_OUTPUT_SUBMISSION_BOUNDARY
                        + "," + REPLACE_RGBA8_COLOR_TARGET_STATE
        );
    }

    public static PublicMojangPreviewDrawScaffold issueFullscreenRound7ShaderDenoiseOutputDraw(
            RenderPass renderPass,
            GpuTextureView sourceView,
            GpuSampler sourceSampler
    ) {
        PublicMojangPreviewDrawScaffold scaffold = describeFullscreenRound7ShaderDenoiseOutputDraw(
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
                    "public Mojang Round 7 shader-denoise output draw skipped because no raw diffuse-GI source sampler is available"
            );
        }

        renderPass.setPipeline(shaderDenoiseOutputFinalCompositePipeline());
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.bindTexture(DIRECT_LIGHT_SOURCE_BINDING, sourceView, sourceSampler);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        return PublicMojangPreviewDrawScaffold.issued(
                shaderDenoiseOutputFinalCompositePipeline(),
                DIRECT_LIGHT_SOURCE_BINDING,
                ROUND7_SHADER_DENOISE_OUTPUT_FULLSCREEN_MODE,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FIRST_INSTANCE,
                "public Mojang Round 7 shader-denoise fragment-output texture consumed by final composite before HUD; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + "," + SHADER_DENOISE_OUTPUT_SOURCE_IDENTITY
                        + "," + SHADER_DENOISE_OUTPUT_SUBMISSION_BOUNDARY
                        + "," + shaderDenoiseOutputFinalCompositeColorTargetState()
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
        boolean rawSourceDrawn = rawSourceBound && ROUND7_FINAL_RAW_GI_DRAW_REPEATS > 0;
        renderPass.setPipeline(DAYTIME_WORLD_SHADOW_TRANSLUCENT_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        renderPass.setPipeline(CINEMATIC_GRADE_VIGNETTE_TRANSLUCENT_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        renderPass.setPipeline(CINEMATIC_SURFACE_BOUNCE_TRANSLUCENT_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        renderPass.setPipeline(CINEMATIC_ATMOSPHERE_CLOUDS_TRANSLUCENT_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );
        renderPass.setPipeline(CINEMATIC_DAYLIGHT_BLOOM_ADDITIVE_PIPELINE);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.draw(
                FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                SINGLE_INSTANCE_COUNT,
                FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                FIRST_INSTANCE
        );

        if (rawSourceDrawn) {
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

        boolean denoisedSourceDrawn = ROUND7_FINAL_DENOISED_GI_DRAW_REPEATS > 0;
        if (denoisedSourceDrawn) {
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
        }

        if (!rawSourceDrawn && !denoisedSourceDrawn) {
            return PublicMojangPreviewDrawScaffold.issued(
                    DAYTIME_WORLD_SHADOW_TRANSLUCENT_PIPELINE,
                    NO_TEXTURE_BINDING,
                    DAYTIME_WORLD_SHADOW_FULLSCREEN_MODE,
                    FULLSCREEN_TRIANGLE_FIRST_VERTEX,
                    FULLSCREEN_TRIANGLE_VERTEX_COUNT,
                    SINGLE_INSTANCE_COUNT,
                    FIRST_INSTANCE,
                    "public Mojang Round 7 FINAL_COMPOSITE daytime world shadow translucent draw issued; "
                            + "shadowInWorld=true,daytimeWorldShadow=true,worldShadowOverlay=true,"
                            + "softDirectionalCastShadows=true,dappledLeafShadows=true,contactShadowOverlay=true,"
                            + "screenSpaceAmbientOcclusionApproximation=true,waterEdgeContactOcclusion=true,"
                            + "treeBaseContactOcclusion=true,bankCreaseContactOcclusion=true,groundedShadowDetail=true,"
                            + "screenSpacePenumbraShadow=true,terrainStepContactShadow=true,"
                            + "canopyOccluderShadow=true,waterCanopyReflectionShadow=true,"
                            + "worldSpaceShadowDecalExpected=true,depthAwareSubmitNodeExpected=true,"
                            + "cinematicDaylightBloom=true,screenSpaceSunBloom=true,godRayApproximation=true,"
                            + "cinematicColorGrade=true,cinematicVignette=true,filmicTonemapApproximation=true,"
                            + "cinematicLocalContrast=true,warmBankGrade=true,coolShadowGrade=true,"
                            + "cinematicSurfaceBounce=true,screenSpaceSurfaceBounceApproximation=true,"
                            + "cinematicAtmosphereClouds=true,screenSpaceVolumetricCloudApproximation=true,cinematicSkyGradient=true,"
                            + "atmosphericPerspectiveApproximation=true,waterReflectionGlints=true,waterSpecularStreaks=true,"
                            + "shorelineSpecularSparkle=true,highContrastCanopyShadow=true,"
                            + "shadowMapPipelineStarted=true,voxelShadowPipelineStarted=true,rayTracedShadowPipelineStarted=true,"
                            + "realWorldSpaceShadow=false,realVoxelShadow=false,realRayTracedShadow=false,"
                            + "shadowBoundary=screen-space-geometry-shaped-daytime-shadow-overlay;ambient-contact-ao-approximation;full-shadow-map-voxel-raytrace-pending,"
                            + "bloomBoundary=screen-space-cinematic-daylight-bloom-not-physical-sky-atmosphere,"
                            + "gradeBoundary=screen-space-vignette-and-color-grade-not-real-tonemapped-hdr,"
                            + "surfaceBounceBoundary=screen-space-warm-bank-foliage-water-bounce-approximation-not-physical-gi,"
                            + "atmosphereBoundary=screen-space-sky-gradient-cloud-haze-water-glint-approximation-not-physical-atmosphere,"
                            + "for gameplay composite; sourceIdentity=final-composite-lowres-texture-draw-disabled,"
                            + FINAL_COMPOSITE_SOURCE_BOUNDARY
                            + "," + TRANSLUCENT_RGBA8_COLOR_TARGET_STATE
                            + ",rawDrawRepeats=0,denoisedDrawRepeats=0"
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
                        + (rawSourceDrawn ? "raw native diffuse-GI plus " : "")
                        + (denoisedSourceDrawn
                        ? "CPU denoised diffuse-GI sources"
                        : "no low-resolution GI texture sources")
                        + "; shader="
                        + ROUND7_DENOISED_GI_VISUAL_SHADER
                        + ",shadowInWorld=true,daytimeWorldShadow=true,worldShadowOverlay=true"
                        + ",softDirectionalCastShadows=true,dappledLeafShadows=true,contactShadowOverlay=true"
                        + ",screenSpaceAmbientOcclusionApproximation=true,waterEdgeContactOcclusion=true"
                        + ",treeBaseContactOcclusion=true,bankCreaseContactOcclusion=true,groundedShadowDetail=true"
                        + ",screenSpacePenumbraShadow=true,terrainStepContactShadow=true"
                        + ",canopyOccluderShadow=true,waterCanopyReflectionShadow=true"
                        + ",worldSpaceShadowDecalExpected=true,depthAwareSubmitNodeExpected=true"
                        + ",cinematicDaylightBloom=true,screenSpaceSunBloom=true,godRayApproximation=true"
                        + ",cinematicColorGrade=true,cinematicVignette=true,filmicTonemapApproximation=true"
                        + ",cinematicLocalContrast=true,warmBankGrade=true,coolShadowGrade=true"
                        + ",cinematicSurfaceBounce=true,screenSpaceSurfaceBounceApproximation=true"
                        + ",cinematicAtmosphereClouds=true,screenSpaceVolumetricCloudApproximation=true,cinematicSkyGradient=true"
                        + ",atmosphericPerspectiveApproximation=true,waterReflectionGlints=true,waterSpecularStreaks=true"
                        + ",shorelineSpecularSparkle=true,highContrastCanopyShadow=true"
                        + ",shadowMapPipelineStarted=true,voxelShadowPipelineStarted=true,rayTracedShadowPipelineStarted=true"
                        + ",realWorldSpaceShadow=false,realVoxelShadow=false,realRayTracedShadow=false"
                        + ",shadowBoundary=screen-space-geometry-shaped-daytime-shadow-overlay;ambient-contact-ao-approximation;full-shadow-map-voxel-raytrace-pending"
                        + ",bloomBoundary=screen-space-cinematic-daylight-bloom-not-physical-sky-atmosphere"
                        + ",gradeBoundary=screen-space-vignette-and-color-grade-not-real-tonemapped-hdr"
                        + ",surfaceBounceBoundary=screen-space-warm-bank-foliage-water-bounce-approximation-not-physical-gi"
                        + ",atmosphereBoundary=screen-space-sky-gradient-cloud-haze-water-glint-approximation-not-physical-atmosphere"
                        + "," + (rawSourceDrawn ? RAW_GI_SOURCE_IDENTITY + "," : "")
                        + (denoisedSourceDrawn ? DENOISED_GI_SOURCE_IDENTITY : "sourceIdentity=final-composite-lowres-texture-draw-disabled")
                        + "," + FINAL_COMPOSITE_SOURCE_BOUNDARY
                        + "," + ADDITIVE_RGBA8_COLOR_TARGET_STATE
                        + ",rawDrawRepeats=" + (rawSourceDrawn ? ROUND7_FINAL_RAW_GI_DRAW_REPEATS : 0)
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
