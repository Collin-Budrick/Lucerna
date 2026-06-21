#version 450

// Contract resource for the future shader-generated diffuse GI denoise output.
//
// Paired concrete resources:
// - denoise/shader_generated_diffuse_output.fsh is the runtime-loadable public
//   Mojang fragment pass. It preserves the existing single InSampler binding,
//   consumes lucerna.lighting.diffuseGi raw-diffuse-gi-rgba8 only, and writes
//   fragColor into the owned lucerna.denoise.diffuse color target. The
//   raw-diffuse-gi-rgba8 payload may include traced raw-GI lighting evidence,
//   but the fragment resource must keep that raw-GI source identity. Because
//   the runtime .fsh has only InSampler today, its edge preservation is a
//   raw-signal proxy using luminance, chroma, alpha/confidence, and local
//   discontinuities; it must report depth/material/history samplers as pending.
// - denoise/shader_generated_diffuse_output.frag.glsl declares the stricter
//   sampler/output interface for the pending depth/normal/history and
//   rejection-mask path. It is still not scheduled by the current placeholder
//   renderer and does not by itself prove real shader denoise execution.
//
// This file is intentionally side-effect free. It is not the executable
// placeholder for lucerna.denoise.diffuse and it must not make telemetry claim
// shaderDenoiseOutputImageReady, shaderDenoiseShaderGeneratedOutput,
// realDenoiseShaderOutput, or realShaderDenoiseOutputReady by itself.
//
// Source identity separation:
// - The strict accepted shader-generated denoise path consumes
//   raw-diffuse-gi-rgba8 input from lucerna.lighting.diffuseGi. Native
//   direct-light validation RGBA, lucerna.lighting.direct, and temporary
//   direct-light preview payloads are rejected substitutes. Required telemetry
//   identity markers include shaderDenoiseInputKind=raw-diffuse-gi-rgba8,
//   rawGiInputReady=true, directLightValidationInput=false, and
//   diagnosticDirectLightValidationFallback=false before this input can count.
// - tracedRawGiInputConsumed means this shader sampled a raw-GI payload whose
//   upstream producer exposed traced lighting evidence. It does not by itself
//   prove physically correct GI, voxel/ray-traced lighting consumption,
//   hardware RT, or production denoise quality.
// - CPU/readback visual denoise is a CPU-staged RGBA payload. It may feed
//   preview visuals or candidate-image readiness only.
// - Public-Mojang final visual shaping is core/round7_denoised_gi_visual.fsh
//   or composite/final_composite.frag.glsl drawing into the borrowed world
//   color path. It is not a denoise output-image producer.
// - Real shader-generated denoise output for the current slice may be produced
//   by a public Mojang fragment pass rendering into the owned
//   lucerna.denoise.diffuse color target. A later compute/storage-image variant
//   may write lucerna.denoise.diffuse plus lucerna.denoise.rejectionMask as
//   Lucerna-owned storage images. Runtime status must distinguish these:
//   shaderDenoiseColorAttachmentWrite=true for the fragment path, while
//   shaderDenoiseComputeDispatch=false and shaderDenoiseStorageImageWrite=false.
//
// Required readable inputs for the future dispatch:
// - Raw GI: lucerna.lighting.diffuseGi as raw-diffuse-gi-rgba8,
//   lucerna.lighting.cacheConfidence, lucerna.lighting.variance,
//   lucerna.lighting.rayBudget. Direct-light validation input may be upstream
//   evidence for earlier milestones, but it is not a valid raw-GI input for
//   this strict denoise-output contract.
// - Normal/depth/material: lucerna.gbuffer.depth,
//   lucerna.gbuffer.normalRoughness, lucerna.gbuffer.materialId.
// - Motion/history/confidence: lucerna.gbuffer.motionHistory, PreviousDepth,
//   PreviousNormalRoughness, PreviousMaterialId when available,
//   PreviousLighting, MotionHistory, RadianceHistory, VarianceConfidence.
//
// Future GLSL interface names, documented but not declared here until the
// scheduler binds concrete descriptor layouts:
// - RawDiffuseGiSampler: half-resolution linear diffuse GI radiance from
//   lucerna.lighting.diffuseGi raw-diffuse-gi-rgba8, never
//   lucerna.lighting.direct or a direct-light validation texture.
// - RawGiConfidenceSampler: half-resolution cache confidence and invalidation.
// - RawGiVarianceSampler: half-resolution current-frame variance.
// - CurrentDepthSampler, CurrentNormalRoughnessSampler,
//   CurrentMaterialIdSampler.
// - CurrentMotionHistorySampler, PreviousDepthSampler,
//   PreviousNormalRoughnessSampler, PreviousLightingSampler.
// - Runtime Mojang .fsh variant uses the public Mojang InSampler binding for
//   lucerna.lighting.diffuseGi raw-diffuse-gi-rgba8 and writes fragColor to
//   lucerna.denoise.diffuse. Its current material/depth preservation mode is
//   raw-signal-edge-fallback until scheduler bindings expose real
//   CurrentDepthSampler, CurrentNormalRoughnessSampler, CurrentMaterialIdSampler,
//   and history samplers to the public Mojang pass.
// - Future compute variant may promote these to storage-image names:
//   OutputDenoisedDiffuseImage and OutputRejectionMaskImage.
// - Current fragment resource outputs are DenoisedDiffuseOutput and
//   RejectionMaskOutput.
//
// Required write contract once implemented:
// - lucerna.denoise.diffuse must be a full-resolution R16G16B16A16_SFLOAT
//   Lucerna-owned output target written by shader execution: currently a public
//   Mojang fragment pass color attachment, or later a compute storage image. It
//   must not be CPU upload, a candidate image, or a final-composite draw into
//   the borrowed Minecraft/Sodium world color target.
// - lucerna.denoise.rejectionMask must be a full-resolution mask that encodes
//   at least disocclusion, depth/normal edge rejection, material mismatch,
//   variance clamp, low confidence, and history reset reasons.
// - The pass must publish a compute-write-to-shader-read barrier before
//   lucerna.debug.overlay or lucerna.composite.final samples either output.
// - The current public Mojang .fsh resource requires a color-attachment
//   write-to-shader-read transition before lucerna.composite.final or
//   lucerna.debug.overlay samples lucerna.denoise.diffuse.
// - The stricter .frag.glsl resource version requires an equivalent
//   color-output attachment transition before either output is sampled by
//   debug/composite.
//
// Readiness gates required before this can be execution evidence:
// - shaderDenoiseInputPrerequisitesReady=true: raw GI, normal/depth/material,
//   motion/history/confidence inputs are bound for the same frame.
// - shaderDenoiseOutputImageReadinessReady=true: writable output images are
//   allocated, owned by lucerna.denoise.diffuse, and not CPU candidate images.
// - shaderDenoiseTemporalHistoryReady=true: previous-frame resources are valid
//   or explicitly reset and confidence/history ages are known.
// - shaderDenoiseRealOutputPrerequisitesReady=true: owned render-target or
//   storage write, barrier, and final-composite consumption contracts are all
//   satisfied.
// - Public Mojang fragment readiness may be claimed separately when
//   shaderDenoiseFragmentPassPrepared=true,
//   shaderDenoiseFragmentInputsCompleteForCurrentMojangPass=true,
//   shaderDenoiseColorAttachmentWrite=true, and
//   shaderDenoiseOutputFinalCompositeConsumable=true. That status must keep
//   shaderDenoiseInputsCompleteForDispatch=false until the full
//   depth/normal/material/history sampler set is bound.
//
// Non-execution boundary:
// - Current placeholder resources may reference this contract, but no dispatch,
//   imageStore, atomics, memory barriers, or history writes are present in this
//   contract file.
// - denoise/shader_generated_diffuse_output.fsh has a color output, but that
//   resource being present is not runtime proof until the scheduler binds its
//   raw-diffuse-gi-rgba8 input and lucerna.denoise.diffuse as the owned draw
//   target. It remains a public Mojang fragment output path, not compute
//   denoise and not a storage-image write.
// - Until the scheduler binds and runs a real implementation, telemetry must
//   keep shaderGeneratedDenoisedGI=false, realDenoiseShaderOutput=false,
//   realShaderDenoiseOutputReady=false, and shaderDenoiseOverclaimPresent=false.
void main() {
}
