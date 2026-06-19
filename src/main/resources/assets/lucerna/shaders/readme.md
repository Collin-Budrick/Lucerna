# Lucerna Shader Layout

These shader files are placeholders for the Sodium Vulkan integration milestones. The authoritative resource contract is `layout.json`; `contracts.md` explains how later renderer agents should extend it.

## Pass Directories

- `gbuffer/`: Lucerna-controlled visibility outputs: depth-compatible data, normals, albedo, material id, emissive, and motion/history metadata.
- `voxel/`: first-pass voxel occupancy staging from chunk/material dirty-region inputs into G-buffer traversal inputs.
- `lighting/`: direct sun/moon, emissive sampling, first diffuse GI, cache confidence, variance, and reserved adaptive sampling metadata.
- `denoise/`: temporal rejection, spatial denoise, variance-aware filtering, and history repair passes.
- `composite/`: flat-composite staging into the active Minecraft target without corrupting vanilla HUD or late translucency.
- `debug/`: overlays used by controller-run verification: backend state, dirty regions, material ids, timings, native queues, cache confidence, variance, adaptive sampling, and label views.

## Phase 5 Order

The canonical Phase 5 dependency order is `lucerna.lighting.direct`, `lucerna.lighting.gi`, `lucerna.denoise.diffuse`, `lucerna.debug.overlay`, then `lucerna.composite.final`. Debug overlay is scheduled before composite because composite may consume `lucerna.debug.overlay` when overlay mode is active.

## Naming

Stable pass ids use `lucerna.<stage>.<name>` and must remain stable once a native or Java caller references them. Use lowercase shader file names with the stage suffix when real shaders land:

- `<pass>.vert.glsl`
- `<pass>.frag.glsl`
- `<pass>.comp.glsl`

Keep placeholder files side-effect free. Sub-agents may add or refine shader assets in their owned scope, but they must not run shader compilation, Gradle checks, Minecraft launches, or render smoke tests.

`lucerna.gbuffer.main` must keep its attachment names aligned with `GBufferTargetContract`: `lucerna.gbuffer.depth`, `lucerna.gbuffer.normalRoughness`, `lucerna.gbuffer.albedoOpacity`, `lucerna.gbuffer.materialId`, `lucerna.gbuffer.emissive`, and `lucerna.gbuffer.motionHistory`.

Phase 5 lighting metadata adds these public handoff targets: `lucerna.lighting.direct`, `lucerna.lighting.diffuseGi`, `lucerna.lighting.cacheConfidence`, `lucerna.lighting.variance`, `lucerna.lighting.rayBudget`, `lucerna.denoise.diffuse`, `lucerna.denoise.rejectionMask`, `lucerna.debug.overlay`, and `lucerna.composite.worldColor`. Keep their write semantics aligned with `layout.json`.

Phase 5 debug target keys are `overlay.direct_lighting`, `overlay.diffuse_gi`, `overlay.cache_confidence`, `overlay.variance`, `overlay.ray_budget`, `overlay.denoised_diffuse`, `overlay.denoise_rejection`, `overlay.debug_overlay`, and `overlay.final_composite`. `overlay.adaptive_sampling` is retained only as an alias for `overlay.ray_budget`.

## Descriptor Contract

`layout.json` reserves four descriptor sets for future native integration:

- `set 0`: frame constants, camera history, quality constants, samplers, blue-noise texture.
- `set 1`: material table, chunk/section metadata, read/write voxel occupancy, dirty-region queue, emissive block list, upload scratch.
- `set 2`: previous-frame resources, temporal radiance, variance/confidence, and motion history.
- `set 3`: debug constants, timing readback, native queue telemetry, debug overlay target, debug labels.

Resource workers should update `layout.json` when adding a pass, dependency, target, descriptor binding, push constant use, attachment write semantic, attachment format, barrier, debug label, or expected controller-run validation scenario. Additive changes are preferred; renaming existing ids or bindings requires controller coordination.
