# Lucerna Shader Layout

These shader files are placeholders for the Sodium Vulkan integration milestones. The authoritative resource contract is `layout.json`; `contracts.md` explains how later renderer agents should extend it.

## Pass Directories

- `gbuffer/`: Lucerna-controlled visibility outputs: depth-compatible data, normals, albedo, material id, emissive, and motion/history metadata.
- `voxel/`: first-pass voxel occupancy staging from chunk/material dirty-region inputs into G-buffer traversal inputs.
- `lighting/`: direct sun/moon, emissive sampling, first diffuse GI, and cache confidence passes.
- `denoise/`: temporal rejection, spatial denoise, variance-aware filtering, and history repair passes.
- `composite/`: flat-composite staging into the active Minecraft target without corrupting vanilla HUD or late translucency.
- `debug/`: overlays used by controller-run verification: backend state, dirty regions, material ids, timings, native queues, and cache views.

## Naming

Stable pass ids use `lucerna.<stage>.<name>` and must remain stable once a native or Java caller references them. Use lowercase shader file names with the stage suffix when real shaders land:

- `<pass>.vert.glsl`
- `<pass>.frag.glsl`
- `<pass>.comp.glsl`

Keep placeholder files side-effect free. Sub-agents may add or refine shader assets in their owned scope, but they must not run shader compilation, Gradle checks, Minecraft launches, or render smoke tests.

`lucerna.gbuffer.main` must keep its attachment names aligned with `GBufferTargetContract`: `lucerna.gbuffer.depth`, `lucerna.gbuffer.normalRoughness`, `lucerna.gbuffer.albedoOpacity`, `lucerna.gbuffer.materialId`, `lucerna.gbuffer.emissive`, and `lucerna.gbuffer.motionHistory`.

## Descriptor Contract

`layout.json` reserves four descriptor sets for future native integration:

- `set 0`: frame constants, camera history, quality constants, samplers, blue-noise texture.
- `set 1`: material table, chunk/section metadata, read/write voxel occupancy, dirty-region queue, emissive block list, upload scratch.
- `set 2`: previous-frame resources, temporal radiance, variance/confidence, motion history.
- `set 3`: debug constants, timing readback, native queue telemetry, debug overlay target, debug labels.

Resource workers should update `layout.json` when adding a pass, dependency, target, descriptor binding, push constant use, attachment write semantic, attachment format, barrier, or expected controller-run validation scenario. Additive changes are preferred; renaming existing ids or bindings requires controller coordination.
