# Lucerna Parallel Implementation Plan

## Progress Audit

Current controller estimate: **about 79% complete against this file**.

This percentage is conservative:
- Rounds 0-3 are mostly implemented and controller-validated.
- Round 4 has Java/native/shader scaffolding, dirty-region GI inputs, direct shadow candidate planning, native direct/GI/post handoff DTOs, payload-category telemetry, and native dispatch validation, but the first real lighting milestone is not complete yet.
- The remaining work is the hardest part: turning Phase 5 metadata/planning into visible direct light, low-res GI, denoise, composite behavior, and richer debug telemetry.

Legend:
- ~~Struck through~~ = implemented and validated by the main controller.
- **Partial** = useful scaffolding exists, but the deliverable is not complete or not fully validated.
- **Open** = not started or not enough evidence to call complete.

## Summary

- ~~Lucerna is a Fabric 26.x + Sodium Vulkan renderer mod with a C++ native core.~~ **DONE/VALIDATED**
- ~~Iris is compatibility-visible only: Lucerna appears in Iris-facing settings/status paths, but Iris shader-pack rendering is disabled while Lucerna owns rendering.~~ **DONE/VALIDATED**
- ~~Implementation should be coordinated by a **main controller** with multiple sub-agents working on disjoint modules.~~ **DONE/ONGOING**
- ~~**Sub-agents must not run tests, Gradle checks, `runClient`, builds used as verification, render smoke tests, or Minecraft launches.** They only edit their assigned scope and report changed files. The main controller runs all tests and sends feedback.~~ **DONE/ONGOING**

## Controller/Sub-Agent Rules

- ~~The main controller owns repo scaffolding, task sequencing, final integration, conflict resolution, all test execution, all Minecraft launches, and all feedback loops.~~ **DONE/ONGOING**
- ~~Sub-agents get explicit write ownership. They must not edit outside their assigned paths unless they ask first.~~ **DONE/ONGOING**
- ~~Sub-agents must assume others are editing the repo in parallel. They must not revert unrelated changes.~~ **DONE/ONGOING**
- ~~Sub-agents may read/search files and make code changes in their scope. They must not run verification commands.~~ **DONE/ONGOING**
- ~~Verification includes Gradle checks, test tasks, compiler-only checks, native builds, shader compilation, `runClient`, Minecraft launches, and render smoke tests. Reading files and searching source are allowed.~~ **DONE/ONGOING**
- ~~Each sub-agent final report must include: changed files, public interfaces touched, assumptions made, and any untested risk.~~ **DONE/ONGOING**
- ~~Controller reviews each patch, runs the relevant checks, then either integrates it or sends focused feedback back to that sub-agent.~~ **DONE/ONGOING**

## Parallel Work Breakdown

### ~~Round 0: Controller-Owned Bootstrap~~ **DONE/VALIDATED**

- ~~Create the initial Fabric/Loom project, package layout, CMake/native layout, shared naming, config directory, and build metadata.~~ **DONE/VALIDATED**
- ~~Establish stable module boundaries:~~ **DONE/VALIDATED**
  - ~~Java mod core: `net.lucerna`~~
  - ~~Compatibility: `net.lucerna.compat`~~
  - ~~World extraction: `net.lucerna.world`~~
  - ~~Native bridge: `net.lucerna.nativebridge`~~
  - ~~Native C++: `native/lucerna_renderer`~~
  - ~~Shaders/resources: `src/main/resources/assets/lucerna`~~
- ~~Add placeholder contracts/interfaces before spawning workers so write scopes do not collide.~~ **DONE/VALIDATED**

### ~~Round 1: Parallel Foundation Agents~~ **DONE/VALIDATED**

- ~~**Agent A: Sodium/Vulkan Compatibility**~~ **DONE/VALIDATED**
  - ~~Owns backend detection, Sodium presence checks, Vulkan activation gates, and Lucerna enable/disable state.~~
  - ~~Does not implement rendering.~~
  - ~~Deliverable: `LucernaBackendDetector`, backend enum/status model, logs and user-facing disable reasons.~~

- ~~**Agent B: Iris Settings Compatibility**~~ **DONE/VALIDATED**
  - ~~Owns Iris detection and settings/status integration.~~
  - ~~Implements "Iris installed, Lucerna active, Iris shader packs disabled" behavior.~~
  - ~~Deliverable: Iris-facing placeholder/status hooks and Lucerna settings entry point.~~
  - ~~Must not attempt Iris shader-pack rendering.~~

- ~~**Agent C: Native Bridge Skeleton**~~ **DONE/VALIDATED**
  - ~~Owns JNI declarations, Java loader, native library lifecycle, and C++ exported ABI stubs.~~
  - ~~Deliverable: no-op native lifecycle with `init`, `shutdown`, `beginFrame`, `endFrame`, `onResize`, `uploadWorldDeltas`, `renderLighting`.~~
  - ~~Must not touch Sodium/Iris compatibility logic.~~

- ~~**Agent D: Config And Debug UI**~~ **DONE/VALIDATED**
  - ~~Owns Lucerna config model, config persistence, keybind/debug overlay toggles, and status presentation.~~
  - ~~Deliverable: config schema with renderer enable flag, quality preset, debug overlay selection, and backend status display.~~

### ~~Round 2: Parallel Renderer Data Agents~~ **DONE/VALIDATED**

- ~~**Agent E: World Event Feed**~~ **DONE/VALIDATED**
  - ~~Owns chunk load/unload, section rebuild, block update, fluid update, emissive update, resource-pack reload, dimension/weather/time tracking.~~
  - ~~Deliverable: `LucernaWorldFeed` and dirty-region queue.~~
  - ~~Must not implement GPU upload.~~

- ~~**Agent F: Material Extraction**~~ **DONE/VALIDATED**
  - ~~Owns block/material table generation from Minecraft block states, models, textures, emissive flags, opacity, water/glass/leaves flags.~~
  - ~~Deliverable: stable Java-side material records and native upload DTOs.~~

- ~~**Agent G: Native Resource Manager**~~ **DONE/VALIDATED**
  - ~~Owns C++ Vulkan resource lifetime abstractions, frame-resource rings, buffer/image wrappers, and safe shutdown.~~
  - ~~Deliverable: native resource manager that can accept borrowed Vulkan context handles later.~~
  - ~~Must not wire into Minecraft frame rendering.~~

- ~~**Agent H: Shader/Resource Layout**~~ **DONE/VALIDATED**
  - ~~Owns Lucerna shader asset organization, shader compile conventions, descriptor layout docs, and placeholder fullscreen/no-op shaders.~~
  - ~~Deliverable: resource structure for G-buffer, lighting, denoise, composite, and debug passes.~~

### Round 3: Integration-Critical Agents

- ~~**Agent I: Vulkan Frame Hook**~~ **DONE/VALIDATED**
  - ~~Owns attaching Lucerna passes to Mojang/Sodium Vulkan frame/render-pass flow.~~
  - ~~Deliverable: no-op Vulkan pass that runs only when backend state is `SODIUM_VULKAN`.~~
  - ~~High-risk scope; controller tests immediately after integration.~~

- ~~**Agent J: Chunk Data Upload**~~ **DONE/VALIDATED**
  - ~~Owns converting world/material deltas into native upload packets and staging them into GPU buffers.~~
  - ~~Deliverable: upload queue, generation counters, dirty-region invalidation handoff.~~

- ~~**Agent K: G-buffer/Voxel First Pass**~~ **DONE/VALIDATED**
  - ~~Owns first Lucerna-controlled world buffers: depth-compatible data, normals, albedo/material id, emissive, motion/history metadata.~~
  - ~~Deliverable: minimal G-buffer and voxel occupancy traversal scaffolding.~~

- **Agent L: Debug Telemetry** **PARTIAL**
  - Owns GPU/CPU timing labels, backend status overlay, dirty chunk overlay, material id overlay, and native error reporting.
  - Deliverable: debug views and structured logs for controller-led testing.
  - ~~Status: backend/native/upload/frame telemetry, Phase 5 lighting dispatch telemetry, native payload-category summaries, and readiness reasons are implemented and compile/launch validated.~~ **DONE/VALIDATED**
  - Status: full GPU timing labels and visual overlay validation are still open.

### Round 4: Lighting Feature Agents

- **Agent M: Direct Lighting** **PARTIAL**
  - Owns sun/moon lighting, emissive block list sampling, voxel shadow ray scaffolding.
  - ~~Status: Java planning contracts, emissive sampling, bounded direct shadow candidate generation, native direct-light handoff DTOs, resource-category stage counts, and native dispatch metadata exist and have been validated.~~ **DONE/VALIDATED**
  - Status: real native direct-light execution is still open.

- **Agent N: First GI** **PARTIAL**
  - Owns low-res single-bounce diffuse GI, temporal accumulation inputs, and cache confidence output.
  - ~~Status: Java planning contracts, ray budget model, dirty-region cache inputs, cache records, native diffuse-GI plan/cache handoff DTOs, and dispatch metadata exist and have been validated.~~ **DONE/VALIDATED**
  - Status: real GI tracing/accumulation output is still open.

- **Agent O: Denoise/Composite** **PARTIAL**
  - Owns edge-aware diffuse denoise, history rejection inputs, and final composite into the world color target.
  - ~~Status: Java planning contracts, post-processing handoff DTOs, shader/resource contracts, and native dispatch metadata exist and have been validated.~~ **DONE/VALIDATED**
  - Status: real denoise shader/composite output is still open.

- **Agent P: Cache/Adaptive Sampling** **PARTIAL**
  - Owns surface/radiance cache records, variance map, ray-budget classification, and dirty-region invalidation policy.
  - ~~Status: cache/ray-budget scaffolding, dirty-region cache inputs, native cache handoff DTOs, and native cache payload telemetry exist and have been validated through launch.~~ **DONE/VALIDATED**
  - Status: visible adaptive debug output is still open.

Append Policy

This section intentionally does not edit, rename, reorder, or reinterpret Rounds 0-4.

Rounds 0-4 remain the historical and current implementation record. The sections below extend the plan from the current Round 4 status toward the full Lucerna cracked-stack target:

Virtualized Chunk Geometry
Voxel Surface Cache
Sparse Voxel Radiance Cache
ReSTIR DI / ReSTIR PT Enhanced-style path reuse
Neural Incident Radiance Cache
Neural Visibility Cache
Hybrid voxel traversal + Vulkan RT tracing
NRD-style denoising and later neural reconstruction
Adaptive sampling
Temporal upscaling
Volumetrics, transparency handling, and final visual polish

All new validation steps follow the existing controller/sub-agent rule:

Sub-agents edit only their assigned scope.
Sub-agents do not run tests, builds, Minecraft launches, shader validation, Gradle checks, or render smoke tests.
The main controller runs all validation.
Every visual milestone must be validated in-world with screenshots and logs.
Round 5: First Visible Lighting Milestone
Goal

Turn the existing Phase 5 planning/dispatch scaffolding into the first visible Lucerna lighting output.

This is the "Lucerna is real" milestone.

The target is not beautiful GI yet. The target is a simple, obvious, testable lighting result:

emissive/direct light affects visible surfaces,
sun/moon direct light path exists,
lighting dispatch produces visible output,
composite path does not corrupt vanilla HUD,
logs prove the native path executed.
Agent Q: Native Direct Lighting Execution

Owns:

Native direct light execution path.
First bounded emissive candidate evaluation.
First simple sun/moon directional light calculation.
Basic voxel shadow-ray hook or placeholder ray result path.

Deliverable:

Native lighting output buffer changes visibly when emissive blocks or sun/moon state are present.
Lighting dispatch count, candidate count, and output-buffer status are logged.

Must not own:

Java material extraction.
Full GI.
Denoising.
UI/HUD composition beyond providing the lighting buffer.

Validation by controller:

Run static compile/build checks.
Launch Minecraft with Sodium Vulkan active.
Join a flat test world.
Place a visible emissive block such as glowstone, redstone lamp, lava, or torch near a flat wall.
Take screenshots with Lucerna lighting disabled and enabled.
Confirm screenshot difference:
nearby surfaces visibly brighten,
light color roughly matches emissive source,
HUD remains readable and uncorrupted.
Validate logs contain:
native direct lighting dispatch,
candidate count greater than zero near emissive blocks,
output buffer write/resolve marker,
no native errors.
Agent R: Lighting Debug Overlay

Owns:

Direct lighting debug overlay modes.
Candidate count overlay.
Direct lighting timing/status display.
Visible indicator that lighting dispatch is active.

Deliverable:

Debug overlay can show:
lighting enabled/disabled,
emissive candidate count,
direct shadow candidate count,
last direct lighting dispatch frame,
CPU/native/GPU timing placeholder or timing if available.

Validation by controller:

Launch Minecraft with Lucerna active.
Join world with emissive blocks nearby.
Toggle debug overlay.
Take screenshot of direct lighting overlay.
Validate overlay is readable and does not overlap catastrophically with vanilla HUD.
Validate logs contain overlay mode changes and direct lighting telemetry.
Round 5 Acceptance Criteria
Direct/emissive lighting has a visible effect in-world.
Controller screenshots show clear before/after difference.
Native logs prove lighting dispatch executed.
HUD/translucency are not corrupted.
Disabling Lucerna restores vanilla/no-op behavior.
No crash on world join, resize, world leave, or shutdown.
Round 6: Low-Resolution Diffuse GI and First Radiance Cache
Goal

Add the first low-resolution diffuse global illumination path.

This should be deliberately simple:

one-bounce diffuse GI,
low-resolution buffer,
temporal accumulation,
cache confidence output,
debug visualization.

This round should not attempt ReSTIR PT, neural radiance caching, or high-quality denoising yet.

Agent S: Low-Resolution GI Pass

Owns:

Low-resolution GI buffer allocation.
Simple diffuse GI ray dispatch.
Basic one-bounce voxel/world sampling path.
GI output buffer population.

Deliverable:

A low-res GI buffer that reacts to nearby emissive sources, sunlight, and simple scene geometry.

Validation by controller:

Build and launch with Sodium Vulkan active.
Join a controlled test world with:
dark cave/interior,
one torch or glowstone,
one colored block near the light source,
one open skylight area.
Take screenshots of:
emissive-only area,
colored bounce test,
skylight test,
debug GI buffer view.
Validate visual expectations:
emissive source creates visible indirect brightness,
colored surfaces influence bounce color at least weakly,
skylight areas are brighter than sealed interiors.
Validate logs contain:
GI dispatch,
low-res target dimensions,
ray budget summary,
cache confidence summary,
no invalid descriptor/native errors.
Agent T: First Sparse Voxel Radiance Cache

Owns:

Sparse voxel/probe cache records.
Cache confidence.
Dirty-region invalidation wiring from existing world feed.
First cache debug view.

Deliverable:

Cache entries are created around visible/active areas.
Cache entries are invalidated or lowered in confidence after block/emissive updates.

Validation by controller:

Join a test world with visible debug overlay.
Place an emissive block.
Take screenshot of cache occupancy/confidence overlay.
Break or move the emissive block.
Take second screenshot showing affected cache region changes.
Validate logs contain:
cache entries allocated,
dirty region received,
confidence reset or invalidation,
cache update count.
Round 6 Acceptance Criteria
Low-res GI visibly affects the scene.
Radiance cache exists and updates over time.
Dirty block/emissive changes affect cache state.
Debug overlay can show GI/cache state.
Screenshots demonstrate visible GI difference.
Logs validate dispatch, update, and invalidation behavior.
Round 7: Denoise and Composite Path
Goal

Turn noisy direct/GI buffers into a stable final image.

This round focuses on a practical NRD-style foundation, not the final bleeding-edge denoiser.

Agent U: Signal-Separated Denoise

Owns:

Separate denoise inputs for:
diffuse GI,
direct shadows,
specular/reflection placeholder if available,
AO placeholder if available.
Edge-aware denoise using depth/normal/material rejection.
History validation inputs.

Deliverable:

Denoised diffuse GI output is visibly more stable than raw GI.
Denoise does not smear across hard block edges.

Validation by controller:

Launch and join world.
Enable raw GI debug view.
Take screenshot.
Enable denoised GI debug view.
Take screenshot from the same location.
Validate visual expectations:
denoised GI has less noise/flicker,
hard block edges remain reasonably sharp,
light does not bleed heavily through walls.
Validate logs contain:
denoise dispatch,
history accepted/rejected counts,
depth/normal rejection stats if implemented,
no GPU/native errors.
Agent V: Final Composite

Owns:

Compositing direct lighting, GI, and base world color.
Native-res HUD preservation.
Toggleable composite modes:
vanilla/base only,
direct only,
GI only,
final Lucerna composite.

Deliverable:

Final image shows Lucerna lighting without corrupting HUD, hand, particles, or basic translucent objects.

Validation by controller:

Join a survival or creative test world.
Capture screenshots for:
base/vanilla mode,
direct-only mode,
GI-only mode,
final composite mode.
Validate:
HUD remains native and readable,
lighting overlays are visually plausible,
no full-screen black/white corruption,
no inverted colors,
no obvious double-composite.
Validate logs show composite mode changes and final composite dispatch.
Round 7 Acceptance Criteria
Denoised GI is visibly better than raw GI.
Composite path produces stable visible lighting.
HUD is preserved.
Debug composite modes work.
Screenshots prove raw vs denoised vs final image.
Logs prove denoise and composite dispatches run cleanly.
Round 8: Adaptive Sampling Controller and Debug Heatmaps
Goal

Use existing cache/ray-budget scaffolding to make ray allocation visible and useful.

The renderer should begin spending more work where the image is unstable and less work where cache/history is strong.

Agent W: Adaptive Sampling Controller

Owns:

Ray budget classification.
Per-tile or per-pixel budget map.
Inputs from:
variance,
disocclusion,
cache confidence,
emissive proximity,
material type,
motion.
Output ray budget used by direct/GI passes.

Deliverable:

Adaptive sampling affects actual dispatch counts and visible debug heatmap.

Validation by controller:

Join test world.
View stable flat terrain.
Screenshot ray budget heatmap.
Move quickly into a dark/emissive cave or newly visible region.
Screenshot ray budget heatmap again.
Validate:
stable terrain receives lower ray budget,
new/dark/emissive areas receive higher ray budget,
dispatch counts change in logs.
Validate logs contain:
budget bucket counts,
high/medium/low ray region counts,
cache confidence contribution,
no invalid budget values.
Agent X: Variance and History Confidence

Owns:

Variance map.
History confidence map.
Disocclusion mask.
Debug views for variance/confidence.

Deliverable:

Debug views show where history is trusted or rejected.

Validation by controller:

Join world with camera stationary.
Take screenshot of history confidence overlay.
Move camera rapidly.
Take screenshot after disocclusion.
Validate:
stationary surfaces gain confidence,
newly visible surfaces lose confidence,
variance map highlights unstable lighting regions.
Validate logs contain history accept/reject counts.
Round 8 Acceptance Criteria
Adaptive ray budgets are visible.
Dispatch counts change based on scene conditions.
Stable areas get cheaper.
New/noisy/emissive areas get more sampling.
Debug heatmaps are screenshot-validated.
Logs include ray budget and history confidence telemetry.
Round 9: Virtualized Chunk Geometry
Goal

Begin the Nanite-like part of the stack, but Minecraft-native.

This is not literal Nanite. This is Virtualized Chunk Geometry:

chunk section clustering,
meshlet or cluster metadata,
GPU-friendly culling,
LOD-ready structure,
distant terrain simplification path.
Agent Y: Chunk Cluster/Meshlet Metadata

Owns:

Chunk section cluster records.
Meshlet/cluster metadata layout.
LOD metadata contracts.
GPU upload format for cluster visibility data.

Deliverable:

Chunk geometry can be described as clusters/meshlets without changing visual output yet.

Validation by controller:

Run compile/build checks.
Launch and join world.
Enable chunk cluster debug overlay.
Take screenshots in:
flat terrain,
cave/interior,
forest/complex area,
high render-distance view.
Validate:
clusters align with chunk/section boundaries or expected subdivisions,
no missing terrain,
no visual corruption.
Validate logs contain:
cluster count,
visible cluster count,
upload size,
generation counters.
Agent Z: GPU-Driven Chunk Culling

Owns:

Frustum culling.
Occlusion culling placeholder or first implementation.
Indirect draw list generation.
Debug culling statistics.

Deliverable:

Hidden/offscreen cluster counts are reduced and visible in logs/overlay.

Validation by controller:

Join world with high render distance.
Face open terrain and screenshot culling overlay.
Face a wall/cave interior and screenshot culling overlay.
Validate:
visible cluster count changes with camera orientation,
terrain does not disappear incorrectly,
CPU/GPU timing does not regress catastrophically.
Validate logs contain culling stats and indirect draw counts.
Round 9 Acceptance Criteria
Virtualized chunk metadata exists.
Debug overlay shows clusters/culling.
No terrain corruption.
Visible/culled counts react to camera.
Logs validate cluster upload and culling behavior.
Round 10: Hybrid Voxel Traversal + Vulkan RT
Goal

Add the serious tracing backend:

custom voxel traversal for block terrain,
Vulkan RT acceleration structures for complex geometry/entities,
unified hit result resolver.
Agent AA: Voxel Traversal Backend

Owns:

DDA-style voxel traversal.
Chunk occupancy masks.
Empty-section skipping.
Block/material hit lookup.
Debug ray/hit visualization.

Deliverable:

Voxel rays can hit solid blocks and miss empty space correctly.

Validation by controller:

Join a test world with simple known geometry:
wall,
tunnel,
open sky,
glass/water test area.
Enable voxel ray debug view.
Take screenshots showing hit/miss visualization.
Validate:
rays hit walls,
rays miss open sky,
empty space skipping does not skip visible geometry,
block material IDs match expected hit surfaces.
Validate logs contain:
ray count,
hit/miss count,
average traversal steps,
skipped section count.
Agent AB: Vulkan RT Path For Complex Geometry

Owns:

BLAS/TLAS setup for entities and complex/modded models.
Entity transform updates.
Hardware RT hit path.
Fallback when RT extension is unavailable.

Deliverable:

Entities/complex models can participate in tracing separate from voxel terrain.

Validation by controller:

Launch on RTX/Vulkan-capable machine.
Join world with entities and complex models.
Take screenshots with RT entity debug overlay.
Validate:
entities appear in RT debug mask,
entity movement updates acceleration structure,
no crash on entity spawn/despawn.
Validate logs contain:
BLAS/TLAS build/update counts,
RT extension availability,
fallback path if applicable.
Agent AC: Hybrid Hit Resolver

Owns:

Combining screen-space, voxel, and Vulkan RT hit results.
Hit priority rules.
Material consistency across tracing paths.

Deliverable:

Lighting/reflections can use the best available hit result.

Validation by controller:

Join world with blocks, entities, glass/water, and emissives.
Enable hybrid hit debug view.
Take screenshots showing hit source classification:
screen-space,
voxel,
hardware RT,
miss/sky.
Validate logs show per-source hit counts.
Round 10 Acceptance Criteria
Voxel traversal works for terrain.
Vulkan RT works for entities/complex geometry where supported.
Hybrid resolver selects correct hit sources.
Debug views and logs prove hit behavior.
No crash on entity movement, chunk load/unload, or world leave.
Round 11: ReSTIR DI and ReSTIR PT-Style Path Reuse
Goal

Add practical sample reuse.

Start with direct-light ReSTIR for many emissive blocks, then move into GI/path reuse.

Agent AD: ReSTIR DI For Many Emissives

Owns:

Direct light reservoirs.
Emissive candidate sampling.
Temporal and spatial reuse for direct light.
Reservoir debug overlay.

Deliverable:

Many-light scenes are cheaper and more stable than brute-force sampling.

Validation by controller:

Create a test scene with many torches/glowstone/redstone lamps.
Capture screenshot with brute/direct baseline.
Capture screenshot with ReSTIR DI enabled.
Validate:
lighting remains plausible,
fewer shadow/light candidates are needed,
no obvious flicker when camera moves slowly.
Validate logs contain:
reservoir count,
candidate count,
selected light IDs/regions,
temporal reuse count,
spatial reuse count.
Agent AE: ReSTIR GI / PT-Style Path Reuse

Owns:

GI reservoirs.
Path candidate reuse.
Path length metadata.
Reservoir confidence for denoiser/adaptive sampling.

Deliverable:

GI converges faster and flickers less in indirect lighting scenes.

Validation by controller:

Join a test cave/interior with indirect lighting.
Capture raw GI without path reuse.
Capture GI with path reuse enabled.
Move camera slowly and record screenshots at fixed intervals.
Validate:
faster apparent convergence,
less flicker/noise,
no severe ghosting from invalid reuse.
Validate logs contain:
GI reservoir count,
path reuse count,
invalidated reservoir count,
confidence statistics.
Round 11 Acceptance Criteria
Direct many-light sampling is reservoir-based.
GI path reuse exists.
Reservoir metadata feeds denoising/adaptive sampling.
Screenshots show stability/quality improvement.
Logs prove candidate reuse and reservoir invalidation behavior.

## Controller Test Responsibilities

- After each sub-agent batch, the controller runs:
  - ~~Static compile/build checks.~~ **DONE/VALIDATED for current worktree**
  - ~~Fabric client launch checks.~~ **DONE/VALIDATED for current worktree**
  - ~~Minecraft world join/leave checks.~~ **DONE/VALIDATED for current worktree**
    - ~~Sodium + Iris Vulkan active launch.~~ **DONE/VALIDATED for current worktree**
  - Resize, resource reload, dimension switch, chunk streaming, and shutdown leak checks. **PARTIAL**
- ~~Controller feedback format to sub-agents:~~ **DONE/ONGOING**
  - ~~Failing command or scenario.~~
  - ~~Relevant logs/stack traces.~~
  - ~~Exact expected behavior.~~
  - ~~Files the sub-agent may edit for the fix.~~
- ~~No sub-agent is allowed to "verify" fixes by running tests or build-like checks. They patch, explain, and wait for controller feedback.~~ **DONE/ONGOING**

Latest strong validation evidence:
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts native section/G-buffer/Phase 5 handoff dispatches from the direct/GI/post DTO integration, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-phase5-handoff-sodium-iris-vulkan-20260619-032114.log`**
- ~~Sodium + Iris + Vulkan launches, joins a world, accepts native section/G-buffer/Phase 5 lighting dispatches, and shuts down cleanly.~~
- ~~Sodium-only Vulkan launches, joins a world, accepts native section/G-buffer/Phase 5 lighting dispatches, and shuts down cleanly.~~
- ~~OpenGL-forced launch disables Lucerna cleanly without native/material/G-buffer/lighting upload work.~~
- ~~No-Sodium launch disables Lucerna cleanly without native/material/G-buffer/lighting upload work.~~
- Vulkan-unavailable remains unvalidated on this hardware because Vulkan is available and no test-only backend override exists.
- Dimension switching and explicit resource-pack reload still need targeted validation.

## Acceptance Criteria

- ~~Lucerna activates only on Sodium Vulkan and disables cleanly elsewhere.~~ **DONE/VALIDATED for tested matrix**
- ~~Iris can be installed without crashing; Lucerna is visible/status-aware, but Iris shader packs do not render while Lucerna is active.~~ **DONE/VALIDATED**
- ~~Native lifecycle survives startup, world join, resize, resource reload, world leave, and shutdown.~~ **DONE/VALIDATED for startup/world/resize/resource reload/world leave/shutdown**
- ~~World/material updates reach native upload queues with generation tracking.~~ **DONE/VALIDATED**
- ~~First visual milestone: no-op or flat composite pass renders without corrupting vanilla HUD/translucency.~~ **DONE/VALIDATED as no-op/flat placeholder path**
- First lighting milestone: basic emissive/direct light + low-res diffuse GI + denoise/composite path works under controller-run testing. **OPEN**

## Assumptions

- ~~The workspace starts empty, so the controller creates the initial scaffold before parallel work.~~ **DONE/HISTORICAL**
- ~~Fabric 26.x, Sodium 26.2 Vulkan path, and C++ native core remain the target.~~ **DONE/ONGOING**
- ~~Sub-agent work is parallelized only after stable interfaces exist to avoid overlapping edits.~~ **DONE/ONGOING**


Long-Term Acceptance Criteria For Full Lucerna
Functional
Lucerna activates only on supported Sodium Vulkan configurations.
Lucerna disables cleanly on unsupported configurations.
Native lifecycle is stable.
Chunk/material/world updates reach native systems.
Direct lighting is visible.
Low-res GI is visible.
Denoise/composite is stable.
Radiance cache updates and invalidates correctly.
Adaptive sampling changes real work distribution.
Hybrid voxel/Vulkan RT tracing works.
ReSTIR-style reuse improves stability/performance.
Neural systems are optional and fallback-safe.
HUD/transparency are preserved.
Debug overlays make every major system inspectable.
Visual

Controller screenshots must validate:

Direct light from sun/moon/emissives.
Torch/lava/glowstone lighting.
One-bounce diffuse GI.
Colored bounce.
Cave/interior indirect light.
Denoised vs raw lighting.
Water/reflections.
Glass/transparency.
Volumetric fog.
Material-aware GI.
Debug overlays:
G-buffer,
material ID,
ray budget,
cache confidence,
reservoir confidence,
variance/history,
pass timing.
Logging

Logs must validate:

backend selection,
enable/disable reason,
native lifecycle,
chunk/material uploads,
direct lighting dispatch,
GI dispatch,
denoise dispatch,
composite dispatch,
cache update/invalidation,
adaptive ray budget stats,
ReSTIR reservoir stats,
neural inference/fallback stats if enabled,
GPU/CPU timings,
shutdown cleanup.
Controller Screenshot Rule

Every visual milestone requires at least:

One screenshot with the feature disabled or baseline mode.
One screenshot with the feature enabled.
One debug overlay screenshot.
Log excerpt showing the relevant dispatch/path executed.
Confirmation that HUD and basic vanilla rendering remain intact.
Final Summary

The remaining course should proceed in this order:

First visible direct lighting.
Low-res GI.
Denoise/composite.
Adaptive sampling.
Virtualized chunk geometry.
Hybrid voxel/Vulkan RT.
ReSTIR DI/PT-style reuse.
Neural visibility/radiance caches.
Latest-gen denoise/reconstruction.
Transparency/water/volumetrics/material polish.
Performance hardening.
Cracked-stack integration.

Do not jump to neural/ReSTIR/Nanite-like systems before the first lighting milestone is visually proven.

The next immediate milestone remains:

Make one emissive block visibly light one surface, screenshot it, and prove the logs match.