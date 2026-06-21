# Lucerna Shader Resource Contracts

`layout.json` is a metadata contract for upcoming Lucerna Vulkan work. It is not a compiled shader manifest yet; it gives the native renderer, frame hook, world upload, lighting, denoise, composite, and debug workers stable names to coordinate against.

## Stability Rules

- Pass ids are public once referenced outside this folder. Use `lucerna.<stage>.<name>` and keep `numericId` stable.
- `numericId` is a stable native/debug identifier, not the scheduler key. Use `executionOrder` for pass sequencing.
- Descriptor set and binding numbers are public once consumed by native code. Add new bindings at the end of the owning set unless the controller explicitly coordinates a breaking change.
- Attachment names are public handoff points between passes. Prefer adding a new attachment over changing the meaning or format of an existing one.
- `dependsOn` edges document scheduler order and resource handoff requirements. They must point at declared passes that execute earlier.
- `attachmentWriteSemantics` documents the intended producer and clear/history behavior for attachments written by a pass.
- `debugLabels` is descriptive overlay metadata. Keep keys stable once exposed through `DebugLabelTable`.
- Placeholder shaders must stay side-effect free: no storage writes, image writes, atomics, barriers, discards, color outputs, or depth outputs.

## Pass Pipeline

The current contract reserves this order:

1. `lucerna.voxel.first_pass`: reads material/chunk dirty-region inputs and stages `VoxelOccupancy` for the main G-buffer traversal path.
2. `lucerna.gbuffer.main`: writes full-resolution depth, normal/roughness, albedo/opacity, material id, emissive, and motion/history buffers using attachment names from `GBufferTargetContract`.
3. `lucerna.lighting.direct`: reads G-buffer, emissive data, and blue noise, writes full-resolution direct lighting.
4. `lucerna.lighting.gi`: reads G-buffer, direct lighting, voxel occupancy, blue noise, and history, writes half-resolution diffuse GI, cache confidence, current-frame variance, adaptive ray-budget classification, and temporal radiance/variance resources.
5. `lucerna.denoise.diffuse`: reads lighting, cache confidence, variance, adaptive ray budget, G-buffer, and history, writes full-resolution diffuse lighting plus a rejection mask.
6. `lucerna.debug.overlay`: reads diagnostic resources, debug labels, cache confidence, variance, and adaptive ray budget before writing the debug overlay target.
7. `lucerna.composite.final`: represents final composite staging into the borrowed Minecraft/Sodium world color target before vanilla HUD and late translucency.

Debug overlay runs before final composite so `lucerna.composite.final` can blend or ignore `lucerna.debug.overlay` according to the selected overlay mode. `numericId` values remain stable native/debug identifiers and are not used to sort this pipeline.

## Phase 5 Write Semantics

- Direct lighting writes `lucerna.lighting.direct` after clearing it; it never blends into Minecraft/Sodium color targets.
- Round 5 direct-light resolve is the first visible use of that target: composite may sample `lucerna.lighting.direct` and current albedo/opacity to brighten the borrowed world color target, but only after the direct output buffer write has completed.
- GI writes `lucerna.lighting.diffuseGi`, `lucerna.lighting.cacheConfidence`, `lucerna.lighting.variance`, and `lucerna.lighting.rayBudget` after clearing them. `lucerna.lighting.rayBudget` is the canonical ray-budget debug target and reserved adaptive sampling metadata with values `0` reuse-only, `1` low, `2` medium, and `3` high.
- Denoise writes `lucerna.denoise.diffuse` and `lucerna.denoise.rejectionMask` after clearing them. Both are history-sensitive and depend on cache confidence plus variance metadata.
- Debug writes `lucerna.debug.overlay` only when overlay mode needs it. It is optional, clear-before-write, and consumes `DebugLabelTable` for stable display names.
- Native conservative shadow-map/mask output may provide `lucerna.lighting.nativeConservativeShadowMask` as a full-resolution mask payload for composite consumption. Composite may darken only receiver-supported regions from that payload; it must not synthesize shadows from screen coordinates or claim path-traced/shader-generated shadow output.
- Composite writes only `lucerna.composite.worldColor`, the borrowed Minecraft/Sodium world color target. It must not own presentation or the swapchain.
- `composite/final_composite.frag.glsl` is the distinct final composite shader resource. Preview shaders in `core/direct_light_preview_*.fsh` remain preview/diagnostic plumbing only and must not be treated as the final composite path.
- `composite/native_shadow_mask_composite.fsh` is a public Mojang composite consumer for the native conservative mask. It emits neutral darkening alpha from the mask payload only and must not add bloom, fog, proof markers, fixed blobs, focus-window behavior, or shader-denoise claims.
- `composite/depth_aware_shadow_mask_composite.frag.glsl` is the pending depth-aware consumer for the same native mask. It requires `ShadowMaskSampler`, `CurrentDepthSampler`, `CurrentNormalRoughnessSampler`, and `SourceLightingSampler`; without depth binding it must output transparent pixels and cannot be used as visual proof.

## Round 4 Payload Contracts

`phase5Payloads` documents the richer first-lighting handoffs without adding descriptor bindings or implementing shader algorithms.

- Direct lighting reads `LucernaFrameConstants`, `BlueNoiseTexture`, `MaterialTable`, `VoxelOccupancy`, `EmissiveBlockList`, and current G-buffer attachments. Its payload is a full-resolution `R16G16B16A16_SFLOAT` target where RGB is linear direct radiance and alpha is reserved for visibility or confidence.
- Direct candidates are directional sun/moon candidates plus bounded emissive candidates. The contract records direction or world position, radiance/color, source kind, material id, generation, candidate weight, and shadow-ray budget fields.
- GI/cache reads direct lighting, voxel occupancy, dirty-region generation, blue noise, `RadianceHistory`, and `VarianceConfidence`. It writes half-resolution diffuse radiance, cache confidence, current-frame variance, and ray-budget classification before updating history resources.
- `lucerna.lighting.cacheConfidence` uses x for current confidence and reserves y for cache age or invalidation reason. `VarianceConfidence` is the temporal aggregate paired with current-frame confidence and variance attachments.
- Denoise owns upsampling, variance-aware clamps, temporal rejection, and history repair inputs. Its composite handoff is `lucerna.denoise.diffuse` plus `lucerna.denoise.rejectionMask`.
- Composite consumes albedo/opacity, direct lighting, optional native conservative shadow mask, diffuse GI, denoised diffuse, and optional debug overlay. It writes only `lucerna.composite.worldColor` before vanilla HUD and late translucency, and never consumes Iris shader-pack outputs.

## First Lighting Milestone

`phase5Telemetry.firstLightingMilestone` defines the boundary for the next visible milestone.

Entry requires active Sodium Vulkan, native world/material upload generations, allocated current-frame G-buffer targets, and the already validated no-op or flat-composite path. Exit requires controller-observable visible direct lighting, low-resolution diffuse GI plus confidence/variance/ray-budget output, denoise/composite behavior, and no HUD or late-translucency corruption.

The milestone is limited to basic sun/moon, bounded emissive sampling, voxel shadow-ray visibility, low-resolution single-bounce diffuse GI, cache confidence/variance/ray-budget metadata, edge-aware denoise, and final composite. Iris shader-pack rendering, swapchain ownership, hardware RT, volumetrics, water/transparency handling, temporal upscaling, and ReSTIR reservoirs stay out of scope.

## Next Physical-Renderer Slice Boundary

This section documents the intended proof target only. It is not validation evidence, and it does not require the normal gameplay path to run heavy proof workloads.

The next physical-renderer visual-quality slice should prove four things together under controller-run same-scene evidence:

- Receiver-tied soft shadows: native conservative mask output may be softened only where receiver/depth/contact evidence supports it, with fixed screen-space blobs, fullscreen darkening, focus-window paths, and proof overlays rejected.
- Spatially varying GI: diffuse GI should vary with emissive source position, receiver geometry, material/color, depth/G-buffer evidence, traced-lighting counters, and source/receiver distance instead of appearing as a uniform tint or detached panel wash.
- Shader denoise visual output: the denoise visual path should preserve `raw-diffuse-gi-rgba8` source identity, produce a shader-owned output image consumed by the final composite, and keep CPU/readback candidate or visual-shaping fallbacks labeled as boundaries rather than shader denoise proof.
- Playable normal-path bypass: default gameplay must keep expensive proof/readback/native telemetry workloads bypassed unless an explicit proof mode enables them, so the visual-quality proof does not regress everyday playability.

Required controller evidence remains fresh Sodium + Iris + Vulkan in-client baseline/enabled/playable captures plus logs that show receiver-tied shadow markers, spatial GI variation markers, shader-denoise output/final-composite consumption markers, and clean no-overclaim markers. Hardware RT, GPU voxel traversal, native Vulkan compute or storage-image denoise, physically correct production GI, and polished shader-pack-quality final visuals remain open unless separate controller evidence explicitly proves them.

## Round 5 Direct Output And Resolve

Round 5 narrows the immediate visible milestone to one direct-lighting proof before later GI quality work. The public output remains `lucerna.lighting.direct`: full-resolution linear RGB direct radiance, with alpha reserved for direct visibility or confidence. The direct pass may use Lucerna-owned frame constants, G-buffer attachments, `MaterialTable`, `VoxelOccupancy`, `BlueNoiseTexture`, and `EmissiveBlockList`.

The minimal resource path for Q3 is direct-only:

1. `lucerna.lighting.direct` writes the Lucerna-owned direct output target.
2. `lucerna.composite.final` reads `lucerna.gbuffer.albedoOpacity` plus `lucerna.lighting.direct`.
3. `lucerna.composite.final` resolves into `lucerna.composite.worldColor`, the borrowed Minecraft/Sodium world color target.

Baseline or disabled mode must skip the direct-radiance contribution and preserve the validated no-op or flat-composite behavior. Enabled direct-only mode may add direct radiance from `lucerna.lighting.direct` before vanilla HUD and late translucency. Both modes keep Minecraft/Sodium ownership of the target.

The final composite shader resource uses bounded output semantics: direct and diffuse radiance are clamped to non-negative capped ranges before being multiplied by albedo/opacity, and the resolved RGB is clamped before writing `lucerna.composite.worldColor`. The shader resource intentionally uses semantic sampler/uniform names until the controller/native descriptor tables assign concrete final-composite bindings.

The Round 5 path must not consume Iris shader-pack color, depth, shadow, or lighting resources. Iris remains status/settings-visible only; it is not an input to direct output or composite resolve.

Visible direct output is not considered complete from metadata alone. It remains controller-validated only after screenshots show an emissive, sun, or moon candidate brightening a visible surface and logs show native direct dispatch, candidate count, direct output write, direct resolve, and no native errors.

## Native Conservative Shadow-Mask Composite

`composite/native_shadow_mask_composite.fsh` reserves the public resource id `lucerna:shaders/composite/native_shadow_mask_composite.fsh` for final-composite consumption of a native conservative shadow-map/mask payload. The expected payload is `lucerna.lighting.nativeConservativeShadowMask` with R coverage, G receiver support, B optional contact/edge confidence, and A validity/confidence. The shader samples this payload through public Mojang `InSampler` and emits black alpha for conservative source-over darkening before HUD composition.

Java readiness for this path is `DirectionalShadowMapOutputPayload.readyForFinalCompositeConsumption()`. It is intentionally budget-friendly proof metadata: dimensions, byte count, nonzero displayable RGBA8 mask data, native sample/caster/receiver counts, depth coverage, checksum, and an explicit `sourceKind=native-conservative-shadow-map-rgba8`. It does not claim GPU shadow-map generation, hardware RT, path tracing, physical GI, or shader-generated denoise.

The public Mojang consumer now applies a compact receiver-tied mask filter before alpha output. Parser-facing proof language may use `softShadowMaskComposite=true`, `edgeAwareShadowMask=true`, `receiverTiedShadow=true`, and `screenSpaceDecalRejected=true` only for this final-composite consumer behavior: these markers mean the mask was locally softened with receiver/contact confidence and flat/fullscreen/fixed-blob fallback paths remain rejected. They do not mean true GPU shadow-map generation, ray-traced shadows, hardware RT, or physically complete shadowing.

This resource is a consumer only. It is not a path-traced shadow system, not a screen-space shadow decal proof, not a shader-generated shadow-map output, and not shader-generated denoise. Controller proof must compare same-scene baseline/enabled screenshots and logs that show native mask output readiness, nonzero receiver-supported mask coverage, and absence of fixed screen-space blob, proof-marker, focus-window, bloom, fog, or denoise-overclaim fallbacks.

`composite/depth_aware_shadow_mask_composite.frag.glsl` reserves the resource id `lucerna:shaders/composite/depth_aware_shadow_mask_composite.frag.glsl` for a later scheduler path that can bind Lucerna-owned same-frame depth and normal inputs. Its sampler contract is:

- `ShadowMaskSampler`: `lucerna.lighting.nativeConservativeShadowMask`
- `CurrentDepthSampler`: `lucerna.gbuffer.depth`
- `CurrentNormalRoughnessSampler`: `lucerna.gbuffer.normalRoughness`
- `SourceLightingSampler`: `lucerna.lighting.direct`

The shader attenuates the native mask by depth continuity, optional normal agreement, receiver support, and source-lighting support. It also applies a depth-aware bilateral mask filter so softening stays tied to the receiver surface and does not smear across depth or normal discontinuities. This is deliberately not a decorative screen-space darkener: `LucernaDepthBindingReady=false` makes the resource transparent, and missing depth binding must be logged as a pending binding rather than a fallback visual path. The same documentation markers, `softShadowMaskComposite`, `edgeAwareShadowMask`, `receiverTiedShadow`, and `screenSpaceDecalRejected`, are valid for this resource only when the scheduler binds the declared mask/depth inputs and controller validation confirms same-frame depth consumption.

## Round 5 Debug Overlay Inputs

The debug overlay contract reserves Lucerna-owned inputs for direct-light validation:

- lighting enabled/disabled state
- emissive candidate count
- direct shadow candidate count
- last direct lighting dispatch frame
- direct output buffer write status
- direct resolve status
- CPU/native/GPU timing placeholder or timing value when available
- controller screenshot marker state

Overlay text or visualization must remain readable with the vanilla HUD and must identify when visible direct output is still pending controller validation.

## Round 7 Shader Denoise Output Boundary

Round 7 currently has three separate shader-resource concepts that must not be collapsed:

- `denoise/diffuse_edge_aware_contract.glsl`, `denoise/history_variance_quality_contract.glsl`, and `denoise/shader_generated_diffuse_output_contract.glsl` describe the real shader denoise output contracts. That path must read `raw-diffuse-gi-rgba8` from `lucerna.lighting.diffuseGi`, cache confidence, variance, ray-budget, current depth/normal/material G-buffer data, previous-frame history, motion, and confidence resources, then write `lucerna.denoise.diffuse` plus `lucerna.denoise.rejectionMask` from shader execution.
- `denoise/shader_generated_diffuse_output.frag.glsl` reserves the resource id `lucerna:shaders/denoise/shader_generated_diffuse_output.frag.glsl` for the shader-generated output pass. It names the concrete sampler interface: `RawDiffuseGiSampler`, `RawGiConfidenceSampler`, `RawGiVarianceSampler`, `CurrentDepthSampler`, `CurrentNormalRoughnessSampler`, `CurrentMaterialIdSampler`, `CurrentMotionHistorySampler`, `PreviousDepthSampler`, `PreviousNormalRoughnessSampler`, and `PreviousLightingSampler`; it writes `DenoisedDiffuseOutput` and `RejectionMaskOutput`. `RawDiffuseGiSampler` must be `raw-diffuse-gi-rgba8`, never direct-light validation input.
- `denoise/shader_generated_diffuse_output.fsh` reserves the runtime-loadable public Mojang fragment resource id `lucerna:shaders/denoise/shader_generated_diffuse_output.fsh` for the first separate denoise-output pass. It keeps the existing public Mojang one-sampler contract, binds `InSampler` to `lucerna.lighting.diffuseGi` `raw-diffuse-gi-rgba8`, then writes `fragColor` into the owned `lucerna.denoise.diffuse` render target. Depth/albedo/history inputs remain in the stricter `.frag.glsl` contract until those bindings are scheduled.
- CPU/readback visual denoise or candidate images may prove only a staged payload or readiness boundary. They are not shader output-image producers and must keep CPU/readback source identity.
- `core/round7_denoised_gi_visual.fsh` is only the public Mojang visual-shaping draw path for an already supplied payload. It has one `InSampler`, no depth/normal/material/motion/history/variance bindings, no storage-image writes, and no rejection-mask output.

Candidate output images are boundary evidence only. `shaderDenoiseOutputImageCandidateReady` may report a CPU-staged or non-GPU candidate image, but `shaderDenoiseOutputImageReady`, `shaderDenoiseShaderGeneratedOutput`, `realDenoiseShaderOutput`, and `realShaderDenoiseOutputReady` must remain false until the public Mojang fragment pass or a future compute path writes the declared denoise output image and the controller validates raw-GI, shader-denoised, final-composite, rejection/debug, and no-overclaim evidence. Direct-light validation input, `lucerna.lighting.direct`, and temporary direct-light RGBA payloads are not accepted substitutes for `raw-diffuse-gi-rgba8` on this strict path. Parser-facing evidence should include `shaderDenoiseInputKind=raw-diffuse-gi-rgba8`, `rawGiInputReady=true`, `directLightValidationInput=false`, and `diagnosticDirectLightValidationFallback=false`.

`tracedRawGiInputConsumed` is the documentation marker for the stricter next boundary: the shader consumed `raw-diffuse-gi-rgba8` whose upstream GI producer exposed traced lighting evidence. This remains a public Mojang fragment output path into `lucerna.denoise.diffuse`, not compute denoise and not a storage-image write. Physical GI quality still requires traced evidence from the GI producer plus controller validation; the shader/resource contract alone must not claim physically correct GI, voxel/ray-traced lighting consumption, hardware RT, or production denoise quality.

`composite/final_composite.frag.glsl` may sample `LucernaDenoisedDiffuse`, but final visual shaping is a consumer, not proof that the input was shader-generated. Runtime/source labels must distinguish `cpu-readback-visual-denoise`, `public-mojang-final-visual-shaping`, and `shader-generated-denoise-output-image`.

Required no-overclaim markers for the current visual path are: CPU/readback or candidate source identity preserved, `shaderDenoiseOutputImageCandidateBoundaryOnly=true` when a candidate image is present, `realShaderDenoiseOutputProven=false`, no proof marker, no focus-window-only fallback, no rectangular full-screen wash, and no substitution of temporary direct-light payloads for denoised GI. The `.fsh` resource is explicitly a public Mojang fragment pass into an owned output texture that preserves local raw-GI contrast from `raw-diffuse-gi-rgba8`, not compute denoise, not a storage-image write, and not proof that the pass ran until scheduler bindings and controller evidence exist.

## Phase 5 Telemetry Names

`phase5Telemetry.debugTargetNames` in `layout.json` is the canonical list for overlay labels and controller validation. The stable keys are:

- `overlay.direct_lighting`: `lucerna.lighting.direct`
- `overlay.diffuse_gi`: `lucerna.lighting.diffuseGi`
- `overlay.cache_confidence`: `lucerna.lighting.cacheConfidence`
- `overlay.variance`: `lucerna.lighting.variance`
- `overlay.ray_budget`: `lucerna.lighting.rayBudget`
- `overlay.denoised_diffuse`: `lucerna.denoise.diffuse`
- `overlay.denoise_rejection`: `lucerna.denoise.rejectionMask`
- `overlay.shader_generated_denoise_output`: `lucerna.denoise.diffuse` contract-only shader output-image readiness
- `overlay.debug_overlay`: `lucerna.debug.overlay`
- `overlay.native_shadow_mask`: `lucerna.lighting.nativeConservativeShadowMask` native conservative mask consumer evidence
- `overlay.final_composite`: `lucerna.composite.worldColor`

`overlay.adaptive_sampling` remains a debug-label alias for `lucerna.lighting.rayBudget`; new code should prefer `overlay.ray_budget`.

## Stage Readiness Labels

`DebugLabelTable` also reserves readiness and milestone boundary keys:

- `readiness.lucerna.lighting.direct`
- `readiness.lucerna.lighting.gi`
- `readiness.lucerna.denoise.diffuse`
- `readiness.lucerna.composite.final`
- `milestone.round4.first_lighting.entry`
- `milestone.round4.first_lighting.exit`

Readiness labels report contract-ready versus algorithm-complete state. The direct, GI, denoise, and composite stages are contract-ready once their inputs and outputs match `phase5Payloads`; they are not algorithm-complete until non-placeholder shaders produce visible lighting and post-processing output under controller-run validation.

## Descriptor Ownership

- `set 0 frame`: renderer/frame-hook owned. Updated per frame or when quality/debug config changes.
- `set 1 world`: world, material, voxel staging, and native upload workers own the backing data and generation counters.
- `set 2 history`: lighting, denoise, and frame lifecycle workers own invalidation, temporal radiance, variance/confidence, and ping-pong history.
- `set 3 debug`: debug telemetry owns timing, native queue, label, and overlay data.

## Validation Notes

The validation scenarios in `layout.json` are controller-run only. Sub-agents may edit this metadata and placeholder files, but must not run shader compilation, Gradle checks, compiler checks, native builds, Minecraft launches, or render smoke tests.

For Round 5, the controller screenshot set is baseline/disabled or no-op, enabled direct-light output, and direct-light debug overlay. The validation markers are one emissive block such as glowstone, a redstone lamp, lava, or torch near a visible wall; clear surface brightening in enabled mode; unchanged readable HUD; dispatch/candidate/output/resolve log markers; and no Iris shader-pack output consumption.

## Java Metadata Scaffold

`net.lucerna.render.resources` mirrors the descriptive parts of `layout.json` for Java-side validation and future native handoff planning. It parses from a caller-provided reader, models pass ids, dependencies, descriptor sets, descriptor bindings, attachments, attachment write semantics, and validation findings, and does not load renderer state or compile shader sources.

The `debugLabels` block is intentionally JSON-only for now. A later debug telemetry agent can model it when `DebugLabelTable` is backed by runtime data.
