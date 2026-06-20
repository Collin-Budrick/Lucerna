# Lucerna Shader Layout

These shader files are placeholders for the Sodium Vulkan integration milestones. The authoritative resource contract is `layout.json`; `contracts.md` explains how later renderer agents should extend it.

## Pass Directories

- `gbuffer/`: Lucerna-controlled visibility outputs: depth-compatible data, normals, albedo, material id, emissive, and motion/history metadata.
- `voxel/`: first-pass voxel occupancy staging from chunk/material dirty-region inputs into G-buffer traversal inputs.
- `lighting/`: direct sun/moon, emissive sampling, first diffuse GI, cache confidence, variance, and reserved adaptive sampling metadata.
- `denoise/`: temporal rejection, spatial denoise, variance-aware filtering, and history repair passes.
- `composite/`: final composite staging into the active Minecraft target without corrupting vanilla HUD or late translucency.
- `debug/`: overlays used by controller-run verification: backend state, dirty regions, material ids, timings, native queues, direct-light candidate counts, output/resolve state, cache confidence, variance, adaptive sampling, and label views.
- `core/`: public Mojang `RenderPipeline` shaders used only for Java-side preview/diagnostic draw plumbing before the native/direct-light texture path is available.
- `restir/`: Round 11 ReSTIR DI/GI/PT-style reuse resource contracts for direct reservoir resolve, temporal reuse, spatial reuse, and GI path-reuse debug outputs. These are contract-only or CPU/native preview boundaries until a controller-validated GPU shader path exists.

`core/direct_light_preview_diagnostic.fsh` is a temporary diagnostic shader. It proves that Lucerna can set a public Mojang pipeline and issue a bounded draw call against the world color target before HUD composition. It is not the real direct-light resolve, does not sample Lucerna direct-light output, and must not be used as screenshot proof that one emissive block lights one surface.

`core/direct_light_preview_additive.fsh` samples the Java-uploaded native CPU direct-light preview texture through the public Mojang `InSampler` binding and additively blends source-gated RGB into the world color target. Coverage is derived from payload alpha/luminance plus local signal gradients so controller proof can reject metadata-only, proof-marker, focus-window-only, and hard rectangular paths. This is still preview-readback based and not the final native GPU direct-light resolve.

`core/direct_light_final_composite_focus.fsh` is a legacy-named direct-light public-Mojang composite resource. It no longer defines proof-marker or focus-window brightness as acceptable evidence; the shader gates contribution by native direct-light payload signal and local signal structure. Keep any controller evidence from this resource labeled as source-gated direct-light preview evidence, not physical GI, shader denoise, or final quality.

`core/round6_native_diffuse_gi_surface.fsh` samples the native diffuse-GI RGBA8 CPU/readback payload through the same public Mojang `InSampler` binding and projects only source-gated signal into the borrowed world color target. The projection is shaped by local payload luminance/chroma, cardinal and diagonal signal gradients, material-contrast cues, and conservative flat-field damping rather than a hard proof rectangle, focus window, or full-screen wash. It is still CPU/readback preview output. It must not be reported as physical GI tracing, real shader denoise, or production final-composite quality.

`core/round7_denoised_gi_visual.fsh` is the first shader-side Round 7 denoise visual-shaping resource for the existing public Mojang draw contract. It preserves the single `InSampler` binding and uses only local luminance, chroma, alpha/confidence, deterministic small-radius neighborhood samples, guided neighbor rejection, cardinal/diagonal signal gradients, material-structure cues, signal-support weighting, source-surface support cues, edge-aware output damping, and unsupported-halo suppression to reduce speckle while avoiding obvious smearing across signal edges. The current visual filter is intentionally conservative: center samples dominate at detected payload gradients, wide-radius samples are weak, directional support is reduced across stronger local signal gradients, local detail is restored from the nearest cardinal neighborhood, center energy is guarded against dimming, and flat high-confidence regions are damped unless local structure or chroma supports a source-shaped contribution. This is not a full denoiser yet: the current public resource path does not provide depth, normals, material id, motion vectors, variance, history radiance, history validity, disocclusion masks, a rejection-mask output, or a writable shader denoise target, so it cannot perform geometry-aware rejection, temporal accumulation, variance clipping, history repair, or NRD-style disocclusion handling. It also must not be reported as real native shader-generated denoise output while `realDenoiseShaderOutput=false`, `realShaderDenoiseOutputReady=false`, or the shader-output image remains unavailable. Candidate-only CPU-staged output images are readiness evidence only and must stay separate from `lucerna.denoise.diffuse` until a controller-validated shader dispatch writes that attachment. It requires controller build/render validation before any AGENTS strike-through or milestone claim.

`restir/direct_reservoir_resolve_contract.glsl`, `restir/direct_temporal_reuse_contract.glsl`, `restir/direct_spatial_reuse_contract.glsl`, and `restir/gi_path_reuse_debug_contract.glsl` document the Round 11 first bounded ReSTIR DI preview path plus future GI/PT-style reuse debug boundaries. They intentionally declare no concrete descriptor bindings or side-effecting shader writes. Current evidence must remain labeled as CPU/native preview or contract-only unless a later controller-validated GPU shader implementation writes the documented reservoir/debug outputs.

`composite/final_composite.frag.glsl` is the distinct final composite shader resource for `lucerna.composite.final`. It is not a preview shader and does not replace either `core/direct_light_preview_*.fsh` resource. Its semantics are bounded: negative radiance is clamped away, direct and diffuse contributions are capped before composition, and final RGB is clamped to the borrowed world-color target range.

## Phase 5 Order

The canonical Phase 5 dependency order is `lucerna.lighting.direct`, `lucerna.lighting.gi`, `lucerna.denoise.diffuse`, `lucerna.debug.overlay`, then `lucerna.composite.final`. Debug overlay is scheduled before composite because composite may consume `lucerna.debug.overlay` when overlay mode is active.

Round 5 uses the same IDs but narrows the immediate target to visible direct light: `lucerna.lighting.direct` writes a Lucerna-owned direct-light target, and `lucerna.composite.final` resolves it into `lucerna.composite.worldColor` before vanilla HUD and late translucency.

The smallest direct-only resource path is:

1. `lucerna.lighting.direct` produces `lucerna.lighting.direct`.
2. `lucerna.composite.final` reads `lucerna.gbuffer.albedoOpacity` and `lucerna.lighting.direct`.
3. `lucerna.composite.final` writes `lucerna.composite.worldColor`.

Baseline/disabled mode skips the direct-light contribution and preserves the validated no-op or flat-composite result. Enabled direct-only mode resolves direct radiance into the borrowed Minecraft/Sodium world color target before vanilla HUD and late translucency. Neither mode may sample Iris shader-pack color, depth, shadow, or lighting resources.

The final composite resource id is `lucerna:shaders/composite/final_composite.frag.glsl`. Preview resources under `core/` remain limited to public Mojang preview/diagnostic plumbing and are not final composite proof.

## Round 4 Payload Handoffs

`layout.json` now separates the first-lighting payload contract from real shader implementation:

- Direct lighting: full-resolution direct radiance in `lucerna.lighting.direct`, sourced from frame sky data, emissive candidates, blue noise, material flags, voxel occupancy, and G-buffer attachments.
- GI/cache: half-resolution diffuse GI, cache confidence, variance, ray-budget classification, `RadianceHistory`, and `VarianceConfidence`, with dirty-region generations defining cache invalidation.
- Denoise/composite: full-resolution denoised diffuse and rejection mask feed the final composite, which writes only the borrowed Minecraft/Sodium world color target before HUD and late translucency.
- Debug readiness: `DebugLabelTable` reserves labels for contract-ready versus algorithm-complete state and for first-lighting entry/exit boundaries.

The first lighting milestone is reached only when the controller can observe visible direct light, low-resolution GI, denoise, and composite behavior without corrupting vanilla HUD or late translucency. Placeholder shaders remain side-effect free until that work lands.

For the first visible direct-light proof, the controller needs three screenshot markers: baseline/disabled or no-op, enabled direct-light output, and direct-light debug overlay. The enabled shot should show one emissive block, sun, or moon path visibly brightening a surface; the overlay should expose candidate counts, shadow candidate count, dispatch frame, output-buffer status, resolve status, and HUD readability. Metadata in this folder only defines that contract; real visible output remains controller-validated.

Lucerna must not consume Iris shader-pack outputs for this milestone. Iris can remain status/settings-visible, but shader-pack color, depth, shadow, and lighting resources are outside the Lucerna direct output and composite resolve contract.

## Naming

Stable pass ids use `lucerna.<stage>.<name>` and must remain stable once a native or Java caller references them. Use lowercase shader file names with the stage suffix when real shaders land:

- `<pass>.vert.glsl`
- `<pass>.frag.glsl`
- `<pass>.comp.glsl`

Keep placeholder files side-effect free. Sub-agents may add or refine shader assets in their owned scope, but they must not run shader compilation, Gradle checks, Minecraft launches, or render smoke tests.

`lucerna.gbuffer.main` must keep its attachment names aligned with `GBufferTargetContract`: `lucerna.gbuffer.depth`, `lucerna.gbuffer.normalRoughness`, `lucerna.gbuffer.albedoOpacity`, `lucerna.gbuffer.materialId`, `lucerna.gbuffer.emissive`, and `lucerna.gbuffer.motionHistory`.

Phase 5 lighting metadata adds these public handoff targets: `lucerna.lighting.direct`, `lucerna.lighting.diffuseGi`, `lucerna.lighting.cacheConfidence`, `lucerna.lighting.variance`, `lucerna.lighting.rayBudget`, `lucerna.denoise.diffuse`, `lucerna.denoise.rejectionMask`, `lucerna.debug.overlay`, and `lucerna.composite.worldColor`. Keep their write semantics aligned with `layout.json`.

Phase 5 debug target keys are `overlay.direct_lighting`, `overlay.diffuse_gi`, `overlay.cache_confidence`, `overlay.variance`, `overlay.ray_budget`, `overlay.denoised_diffuse`, `overlay.denoise_rejection`, `overlay.debug_overlay`, and `overlay.final_composite`. `overlay.adaptive_sampling` is retained only as an alias for `overlay.ray_budget`.

Stage readiness and first-lighting milestone keys are `readiness.lucerna.lighting.direct`, `readiness.lucerna.lighting.gi`, `readiness.lucerna.denoise.diffuse`, `readiness.lucerna.composite.final`, `milestone.round4.first_lighting.entry`, and `milestone.round4.first_lighting.exit`.

## Descriptor Contract

`layout.json` reserves four descriptor sets for future native integration:

- `set 0`: frame constants, camera history, quality constants, samplers, blue-noise texture.
- `set 1`: material table, chunk/section metadata, read/write voxel occupancy, dirty-region queue, emissive block list, upload scratch.
- `set 2`: previous-frame resources, temporal radiance, variance/confidence, and motion history.
- `set 3`: debug constants, timing readback, native queue telemetry, debug overlay target, debug labels.

Resource workers should update `layout.json` when adding a pass, dependency, target, descriptor binding, push constant use, attachment write semantic, attachment format, barrier, debug label, or expected controller-run validation scenario. Additive changes are preferred; renaming existing ids or bindings requires controller coordination.
