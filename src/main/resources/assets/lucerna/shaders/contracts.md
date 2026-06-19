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
7. `lucerna.composite.final`: currently represents flat-composite staging into the borrowed Minecraft/Sodium world color target before vanilla HUD and late translucency.

Debug overlay runs before final composite so `lucerna.composite.final` can blend or ignore `lucerna.debug.overlay` according to the selected overlay mode. `numericId` values remain stable native/debug identifiers and are not used to sort this pipeline.

## Phase 5 Write Semantics

- Direct lighting writes `lucerna.lighting.direct` after clearing it; it never blends into Minecraft/Sodium color targets.
- Round 5 direct-light resolve is the first visible use of that target: composite may sample `lucerna.lighting.direct` and current albedo/opacity to brighten the borrowed world color target, but only after the direct output buffer write has completed.
- GI writes `lucerna.lighting.diffuseGi`, `lucerna.lighting.cacheConfidence`, `lucerna.lighting.variance`, and `lucerna.lighting.rayBudget` after clearing them. `lucerna.lighting.rayBudget` is the canonical ray-budget debug target and reserved adaptive sampling metadata with values `0` reuse-only, `1` low, `2` medium, and `3` high.
- Denoise writes `lucerna.denoise.diffuse` and `lucerna.denoise.rejectionMask` after clearing them. Both are history-sensitive and depend on cache confidence plus variance metadata.
- Debug writes `lucerna.debug.overlay` only when overlay mode needs it. It is optional, clear-before-write, and consumes `DebugLabelTable` for stable display names.
- Composite writes only `lucerna.composite.worldColor`, the borrowed Minecraft/Sodium world color target. It must not own presentation or the swapchain.

## Round 4 Payload Contracts

`phase5Payloads` documents the richer first-lighting handoffs without adding descriptor bindings or implementing shader algorithms.

- Direct lighting reads `LucernaFrameConstants`, `BlueNoiseTexture`, `MaterialTable`, `VoxelOccupancy`, `EmissiveBlockList`, and current G-buffer attachments. Its payload is a full-resolution `R16G16B16A16_SFLOAT` target where RGB is linear direct radiance and alpha is reserved for visibility or confidence.
- Direct candidates are directional sun/moon candidates plus bounded emissive candidates. The contract records direction or world position, radiance/color, source kind, material id, generation, candidate weight, and shadow-ray budget fields.
- GI/cache reads direct lighting, voxel occupancy, dirty-region generation, blue noise, `RadianceHistory`, and `VarianceConfidence`. It writes half-resolution diffuse radiance, cache confidence, current-frame variance, and ray-budget classification before updating history resources.
- `lucerna.lighting.cacheConfidence` uses x for current confidence and reserves y for cache age or invalidation reason. `VarianceConfidence` is the temporal aggregate paired with current-frame confidence and variance attachments.
- Denoise owns upsampling, variance-aware clamps, temporal rejection, and history repair inputs. Its composite handoff is `lucerna.denoise.diffuse` plus `lucerna.denoise.rejectionMask`.
- Composite consumes albedo/opacity, direct lighting, diffuse GI, denoised diffuse, and optional debug overlay. It writes only `lucerna.composite.worldColor` before vanilla HUD and late translucency, and never consumes Iris shader-pack outputs.

## First Lighting Milestone

`phase5Telemetry.firstLightingMilestone` defines the boundary for the next visible milestone.

Entry requires active Sodium Vulkan, native world/material upload generations, allocated current-frame G-buffer targets, and the already validated no-op or flat-composite path. Exit requires controller-observable visible direct lighting, low-resolution diffuse GI plus confidence/variance/ray-budget output, denoise/composite behavior, and no HUD or late-translucency corruption.

The milestone is limited to basic sun/moon, bounded emissive sampling, voxel shadow-ray visibility, low-resolution single-bounce diffuse GI, cache confidence/variance/ray-budget metadata, edge-aware denoise, and final composite. Iris shader-pack rendering, swapchain ownership, hardware RT, volumetrics, water/transparency handling, temporal upscaling, and ReSTIR reservoirs stay out of scope.

## Round 5 Direct Output And Resolve

Round 5 narrows the immediate visible milestone to one direct-lighting proof before later GI quality work. The public output remains `lucerna.lighting.direct`: full-resolution linear RGB direct radiance, with alpha reserved for direct visibility or confidence. The direct pass may use Lucerna-owned frame constants, G-buffer attachments, `MaterialTable`, `VoxelOccupancy`, `BlueNoiseTexture`, and `EmissiveBlockList`.

The minimal resource path for Q3 is direct-only:

1. `lucerna.lighting.direct` writes the Lucerna-owned direct output target.
2. `lucerna.composite.final` reads `lucerna.gbuffer.albedoOpacity` plus `lucerna.lighting.direct`.
3. `lucerna.composite.final` resolves into `lucerna.composite.worldColor`, the borrowed Minecraft/Sodium world color target.

Baseline or disabled mode must skip the direct-radiance contribution and preserve the validated no-op or flat-composite behavior. Enabled direct-only mode may add direct radiance from `lucerna.lighting.direct` before vanilla HUD and late translucency. Both modes keep Minecraft/Sodium ownership of the target.

The Round 5 path must not consume Iris shader-pack color, depth, shadow, or lighting resources. Iris remains status/settings-visible only; it is not an input to direct output or composite resolve.

Visible direct output is not considered complete from metadata alone. It remains controller-validated only after screenshots show an emissive, sun, or moon candidate brightening a visible surface and logs show native direct dispatch, candidate count, direct output write, direct resolve, and no native errors.

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

## Phase 5 Telemetry Names

`phase5Telemetry.debugTargetNames` in `layout.json` is the canonical list for overlay labels and controller validation. The stable keys are:

- `overlay.direct_lighting`: `lucerna.lighting.direct`
- `overlay.diffuse_gi`: `lucerna.lighting.diffuseGi`
- `overlay.cache_confidence`: `lucerna.lighting.cacheConfidence`
- `overlay.variance`: `lucerna.lighting.variance`
- `overlay.ray_budget`: `lucerna.lighting.rayBudget`
- `overlay.denoised_diffuse`: `lucerna.denoise.diffuse`
- `overlay.denoise_rejection`: `lucerna.denoise.rejectionMask`
- `overlay.debug_overlay`: `lucerna.debug.overlay`
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
