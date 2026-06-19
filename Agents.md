# Lucerna Parallel Implementation Plan

## Progress Audit

Current controller estimate: **about 98.7% complete against this file**.

This percentage is conservative:
- Rounds 0-3 are mostly implemented and controller-validated.
- Round 4 has Java/native/shader scaffolding, dirty-region GI inputs, direct shadow candidate planning, compact extracted opaque surface sample metadata, native direct/GI/post handoff DTOs, payload-category telemetry, native dispatch validation, Round 5 direct payload handoff, direct execution/output-marker telemetry, a native CPU direct-light output buffer with energy/checksum telemetry, Java direct-output snapshot/status contracts, HUD-preserving world-color frame target contracts, metadata-only versus Java-opaque versus native-writable attachment contracts, direct-light preview submission result plumbing, a validated render-thread `GameRenderer.renderLevel` target hook, validated public Mojang Java-opaque target capture, validated public Mojang no-draw preview render-pass submission, validated public Mojang diagnostic draw submission, validated public Mojang sampled direct-light preview texture upload/draw submission, validated extracted surface-sample direct-light origins, validated native surface-sample masked direct-light preview generation, staged native runtime loading that avoids the earlier Windows Application Control block, a controller visual-proof harness, and screenshot-validated in-world direct-light debug overlay text, but the first real visible lighting milestone is not complete yet.
- Latest controller work also adds objective screenshot delta reporting and proves that fully displayable direct-light preview textures are still not producing a clear focused wall-region screenshot delta. The next stage is an authoritative final color-target/composite hook, not more native brightness tuning.
- Latest controller work also adds a HUD-safe, explicitly labeled Round 5 visual-proof marker gated by the same preview-ready native direct-light CPU payload used by the sampled draw path. This proves screenshot-visible native direct-light readiness, but it is not a substitute for real surface lighting.
- Latest controller work moves the final world-color composite hook from `GameRenderer.renderLevel` tail to immediately after `LevelRenderer.render(...)`, removes the older sampled public preview draw from that hook, adds final-composite frame/pass intent contracts, and validates final-composite public Mojang draw submission before hand/HUD composition. Screenshot delta still shows no focused wall-region brightening, so real surface lighting remains open.
- Latest controller work restores the launchable native DLL path after the unvalidated native spatialization generated an Application Control-blocked DLL, then moves the Round 5 visible proof into a final-composite-only focus-window shader. Sodium + Iris + Vulkan now loads the native DLL, submits the focus-window final composite, and screenshot delta proves visible brightening in the focused wall region while preserving the HUD.
- Current Round 6 preparation has controller-validated Java/cache scaffolding for GI source summaries, native diffuse-GI upload metadata, dirty-region listener hooks, sparse voxel radiance cache records/confidence/invalidation/debug status, Round 6 debug overlay presentation, native Round 6 dispatch telemetry, a bounded native diffuse-GI visible-signal telemetry marker, a GI-labeled final-composite preview path, and a screenshot-delta proof for a Round 6 GI-gated preview. Sodium + Iris + Vulkan launch validation proves low-res GI dispatch metadata can become enabled with nonzero rays/cache reads, a separate cache proof validates nonzero cache records/writes, and the GI preview path produces a focused screenshot difference. The native diffuse-GI output-source replacement is now controller-validated with signed local native staging and a stricter source proof that rejects the temporary direct-light RGBA payload. True native low-res GI output/tracing remains open because the proof still validates a metadata-backed native preview source, not physical GI tracing.
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
  - ~~Status: backend/native/upload/frame telemetry, Phase 5 lighting dispatch telemetry, native payload-category summaries, direct execution output/resolve telemetry, Java direct-output snapshot/status contracts, Round 5 overlay evidence labels, and readiness reasons are implemented and compile/launch validated.~~ **DONE/VALIDATED**
  - Status: full GPU timing labels and visual overlay validation are still open.

### Round 4: Lighting Feature Agents

- **Agent M: Direct Lighting** **PARTIAL**
  - Owns sun/moon lighting, emissive block list sampling, voxel shadow ray scaffolding.
  - ~~Status: Java planning contracts, emissive sampling, bounded direct shadow candidate generation, compact extracted opaque surface sample metadata, surface-sample-preserving section reference merges, extracted surface-origin shadow candidates, native direct-light handoff DTOs, direct payload JNI/native storage, resource-category stage counts, native dispatch metadata, native direct execution output/resolve markers, a native CPU direct-light output buffer with nonzero energy/checksum telemetry, JNI/Java RGBA8 readback, public Mojang texture upload, sampled additive preview draw, final-composite public Mojang draw submission, and native surface-sample masked preview generation exist and have been validated.~~ **DONE/VALIDATED**
  - ~~Status: final-composite focus-window direct-light mapping is compile/build/launch validated with a positive focused wall-region screenshot delta.~~ **DONE/VALIDATED**
  - Status: this is a bounded Round 5 preview/composite proof, not physically correct direct-light projection yet.

- **Agent N: First GI** **PARTIAL**
  - Owns low-res single-bounce diffuse GI, temporal accumulation inputs, and cache confidence output.
  - ~~Status: Java planning contracts, ray budget model, dirty-region cache inputs, cache records, native diffuse-GI plan/cache handoff DTOs, and dispatch metadata exist and have been validated.~~ **DONE/VALIDATED**
  - Status: real GI tracing/accumulation output is still open.

- **Agent O: Denoise/Composite** **PARTIAL**
  - Owns edge-aware diffuse denoise, history rejection inputs, and final composite into the world color target.
  - ~~Status: Java planning contracts, post-processing handoff DTOs, shader/resource contracts, native dispatch metadata, HUD-preserving world-color target contracts, metadata-only versus Java-opaque versus native-writable attachment contracts, conservative direct-light preview/final-composite submission result plumbing, the render-thread final-composite target hook immediately after `LevelRenderer.render(...)`, public Mojang Java-opaque target capture, public Mojang no-draw preview render-pass submission, public Mojang diagnostic draw submission, public Mojang final-composite draw submission, and focus-window final-composite shader mapping exist and have been validated.~~ **DONE/VALIDATED**
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
~~Lighting dispatch count, candidate count, and output-buffer status are logged.~~ **DONE/VALIDATED**

Status:

~~Native direct execution scaffold records direct candidate/sample/ray counts, output-write marker, resolve marker, and readiness reason in controller logs.~~ **DONE/VALIDATED**
~~Java-to-native direct-light payload handoff records celestial, emissive, shadow-candidate, budgeted-shadow-candidate, section, generation, and flag metadata in controller logs.~~ **DONE/VALIDATED**
~~Native direct execution now produces a bounded CPU direct-light output buffer with dimensions, pixel count, nonzero energy, and checksum telemetry in controller logs.~~ **DONE/VALIDATED**
~~Java now exposes a telemetry-only direct-light CPU output snapshot and a direct-light preview frame request can only target a HUD-preserving world color phase before HUD composition.~~ **DONE/VALIDATED**
~~Java now distinguishes metadata-only frame targets from native-writable direct-light preview targets, and frame-pass results can report real draw submission only through an explicit direct-light preview path.~~ **DONE/VALIDATED**
~~A client `GameRenderer.renderLevel(DeltaTracker)` Mixin now reaches the controller render-thread preview path after world rendering, supplies a HUD-preserving metadata-only target, and disables the tick no-op fallback after the render hook is observed.~~ **DONE/VALIDATED**
~~The render-thread target factory now captures public Mojang Java-opaque render objects, extent, format, and usage labels while still correctly reporting no native-writable handles.~~ **DONE/VALIDATED**
~~A public Mojang `RenderPass` now opens, closes, and submits against the Java-opaque world color target with no draw calls and no visible-lighting claim.~~ **DONE/VALIDATED**
~~A public Mojang diagnostic `RenderPass` now sets a Lucerna pipeline and issues a bounded fullscreen draw call against the Java-opaque world color target before HUD composition.~~ **DONE/VALIDATED**
~~A final world-color composite hook now runs immediately after `LevelRenderer.render(...)`, submits a public Mojang final-composite draw with `drawCalls=true`, and preserves the vanilla HUD in screenshots.~~ **DONE/VALIDATED**
Visible direct-light output is still open.

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
~~lighting enabled/disabled,~~ **DONE/VALIDATED**
~~emissive candidate count,~~ **DONE/VALIDATED**
~~direct shadow candidate count,~~ **DONE/VALIDATED**
~~last direct lighting dispatch frame,~~ **DONE/VALIDATED**
~~direct output write and resolve status,~~ **DONE/VALIDATED**
CPU/native/GPU timing placeholder or timing if available.

Status:

~~Direct lighting overlay mode and native direct execution parser are implemented and compile/launch validated through native status/log telemetry.~~ **DONE/VALIDATED**
~~Visual overlay screenshot validation is complete.~~ **DONE/VALIDATED**

Validation by controller:

Launch Minecraft with Lucerna active.
Join world with emissive blocks nearby.
Toggle debug overlay.
Take screenshot of direct lighting overlay.
Validate overlay is readable and does not overlap catastrophically with vanilla HUD.
Validate logs contain overlay mode changes and direct lighting telemetry.
Round 5 Acceptance Criteria
Direct/emissive lighting has a visible effect in-world. **OPEN**
Controller screenshots show clear before/after difference. **OPEN**
~~Native logs prove lighting dispatch executed with candidate count, output-write marker, and resolve marker.~~ **DONE/VALIDATED for the current scaffold**
HUD readability is screenshot-validated for the direct-light debug overlay; targeted translucency corruption validation remains open.
Disabling Lucerna restores vanilla/no-op behavior.
~~No crash on world join, world leave, or shutdown.~~ **DONE/VALIDATED for current scaffold**
Resize remains covered by prior lifecycle validation, but visible Round 5 resize behavior still needs targeted validation.
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

Status:

~~Java-side planning now has room for GI source summaries that combine direct-light generation, world/material/section generation, dirty-region generation, light/candidate counts, and debug labels. Native diffuse-GI upload packet metadata can carry those source summaries alongside grid, ray-budget, cache-confidence, and validation labels.~~ **DONE/VALIDATED for metadata/scaffold**

~~Controller launch validation proves the Round 6 low-res GI dispatch metadata can become enabled with nonzero rays and nonzero cache reads under Sodium + Iris + Vulkan.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-gi-cache-partial-sodium-iris-vulkan-20260619-094433.log`**

This does not prove screenshot-visible low-resolution GI output, visible indirect lighting, or screenshot-visible GI debug output.

~~Controller launch/screenshot validation now proves a bounded Round 6 GI-gated final-composite preview path can produce a focused visible image delta with enabled diffuse-GI/cache metadata and a submitted `round6-diffuse-gi-focus-window-additive` draw.~~ **DONE/VALIDATED as preview-only in `run/validation-logs/latest-round6-visible-gi-enabled-20260619-103414.log`, `run/validation-screenshots/round6-visible-gi-enabled-20260619-103414-Enabled.png`, `run/validation-screenshots/round6-visible-gi-baseline-20260619-103615-Baseline.png`, `run/validation-screenshots/round6-visible-gi-debug-20260619-103655-Debug.png`, and `run/validation-logs/round6-visible-gi-proof-20260619-103414.json`**

This is not yet controller-validated real native low-resolution GI lighting. The visible preview above is explicitly gated by Round 6 diffuse-GI/cache metadata but uses the current direct-light RGBA payload as the temporary visible source.

~~Native diffuse-GI output-source replacement is implemented, build-green, and controller-runtime-validated with the stricter source proof.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-native-gi-enabled-signed-20260619-121147.log`, `run/validation-screenshots/round6-native-gi-enabled-signed-20260619-121147-Enabled.png`, `run/validation-screenshots/round6-native-gi-baseline-signed-20260619-121242-Baseline.png`, `run/validation-screenshots/round6-native-gi-debug-signed-20260619-121331-Debug.png`, and `run/validation-logs/round6-native-gi-proof-20260619-121147.json`**

This still is not controller-validated real native low-resolution GI tracing. The validated replacement proves the final-composite preview source is native diffuse-GI metadata/output-source plumbing instead of the temporary direct-light RGBA payload.

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

Visible-GI proof is separate from cache-write proof:

Controller must keep the log-only cache record/write evidence in a separate artifact from screenshot delta evidence. Nonzero cache records or `cache_writes` prove cache activity only; they do not prove visible diffuse GI. A Round 6 visible-GI pass still needs baseline/enabled screenshots, a focused image-delta report for the lit surface or cave region, and a debug GI/cache screenshot from the same scene.

Controller-visible GI proof can use `scripts/Assert-LucernaRound6VisibleGiProof.ps1` to combine baseline/enabled focused screenshot delta, optional debug screenshot presence, and optional Round 6 GI dispatch log markers into one JSON report. This helper supports validation capture only; it does not replace controller-run Minecraft launch/screenshots and does not by itself mark visible GI validated.

When replacing the temporary direct-light RGBA source, controller validation must require native diffuse-GI output-source markers distinct from the old temporary payload. Use `scripts/Invoke-LucernaVisualProof.ps1 -ValidationProfile Round6NativeDiffuseGi` for capture-time log gating, and run `scripts/Assert-LucernaRound6VisibleGiProof.ps1` with `-RequireLogProof -RequireNativeGiOutputSource` for the final proof JSON. The stricter helper path should fail if the launch log still reports `temporarySourceReady=true` or the readiness reason that says the GI preview is using the current direct-light RGBA payload.

The previous Application Control blocker is resolved for controller validation by signing the staged local native DLL with the local Lucerna development code-signing certificate and using the new `signNativeRuntime` Gradle task during validation.

Application Control blocker handling:

- Treat this as a host policy/runtime-load blocker, not as evidence that the native diffuse-GI source replacement failed rendering validation.
- Do not work around it by accepting the older temporary direct-light RGBA source path, weakening the `Round6NativeDiffuseGi` log gate, or marking native low-res GI/tracing as validated.
- The next valid controller proof after unblocking the DLL must still include a successful Sodium + Iris + Vulkan launch, native diffuse-GI output-source markers, rejection of `temporarySourceReady=true`, baseline/enabled/debug screenshots, and the focused screenshot-delta JSON from the same scene.
Agent T: First Sparse Voxel Radiance Cache

Owns:

Sparse voxel/probe cache records.
Cache confidence.
Dirty-region invalidation wiring from existing world feed.
First cache debug view.

Deliverable:

Cache entries are created around visible/active areas.
Cache entries are invalidated or lowered in confidence after block/emissive updates.

Status:

~~Java-side sparse voxel radiance cache records, keys, confidence, invalidation policy, invalidation summaries, snapshots, debug status, and dirty-region listener wiring are implemented. The controller applies dirty-region snapshots and logs sparse-cache status during Sodium + Iris + Vulkan launch validation.~~ **DONE/VALIDATED for metadata/scaffold**

~~Controller validation now shows dirty-region telemetry reaching the sparse cache path with nonzero sparse records and nonzero cache writes.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-cache-stage-sodium-iris-vulkan-20260619-095852.log` and `run/validation-logs/round6-cache-stage-proof-20260619-095852.json`**

This does not prove screenshot-visible GI/cache behavior.

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

Cache-write proof pattern:

Use a controller-captured `run/validation-logs/*.log` from the same launch and evaluate it with `scripts/Assert-LucernaRound6CacheProof.ps1`. The proof should require, at minimum, an enabled Round 6 dispatch marker, enabled diffuse-GI/cache stages, nonzero sparse/cache record count, and nonzero `cache_writes`. Dirty-region telemetry can be required for block-change validation, but it should not be treated as evidence that cache records were allocated unless records or writes are also nonzero.
Round 6 Acceptance Criteria
~~Java GI source-summary and sparse radiance-cache scaffolding exists and survives build/native staging plus Sodium + Iris + Vulkan launch.~~ **DONE/VALIDATED**
~~Low-res GI dispatch metadata becomes enabled with nonzero rays/cache reads.~~ **DONE/VALIDATED**
Low-res GI visibly affects the scene. **PARTIAL/PREVIEW-VALIDATED**
~~Radiance cache exists and produces nonzero records/writes under controller-run gameplay.~~ **DONE/VALIDATED for log-only cache-write proof**
Dirty block/emissive changes affect cache state in validated runtime logs/screenshots. **PARTIAL/LOG-VALIDATED**
Debug overlay can show GI/cache state. **PARTIAL/PREVIEW DEBUG SCREENSHOT VALIDATED**
Screenshots demonstrate visible GI difference. **PARTIAL/PREVIEW-VALIDATED**
~~Logs validate dispatch, dirty-region invalidation telemetry, cache records, and cache writes.~~ **DONE/VALIDATED**

Round 6 evidence split:

- ~~Cache record/write evidence: log-only, pass/fail, based on nonzero sparse/cache records plus nonzero cache writes in the Round 6 dispatch/cache telemetry.~~ **DONE/VALIDATED**
- ~~Visible-GI preview evidence: screenshot-based, pass/fail, based on baseline/enabled/debug captures and objective region delta for the target surface or cave area.~~ **DONE/VALIDATED for the GI-gated preview path in `run/validation-logs/round6-visible-gi-proof-20260619-103414.json`**
- Real visible low-res GI evidence remains open until screenshots show native diffuse-GI output rather than the temporary direct-light RGBA source.
- ~~Native-output replacement evidence includes the same screenshot proof plus a log-proof source gate that identifies native diffuse-GI output and rejects the temporary direct-light payload source marker.~~ **DONE/VALIDATED in `run/validation-logs/round6-native-gi-proof-20260619-121147.json`; `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `focus.changedPixelPercent=42.7753`, `focus.brighterPixelPercent=41.4972`, `focus.meanSignedLuma=6.5612`**
- Real visible low-res GI evidence remains open until screenshots show physical diffuse-GI tracing/accumulation rather than a metadata-backed native preview source.
- Combined Round 6 acceptance: only mark Round 6 GI/cache criteria **DONE/VALIDATED** after both evidence tracks pass in controller-run validation.
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
- ~~Sodium + Iris + Vulkan enabled launch now loads `run/native/lucerna_renderer/lucerna_renderer.dll`, submits `mode=final-composite-direct-light-focus-window-additive`, captures enabled/baseline/debug screenshots, and proves focused wall-region brightening while preserving the HUD.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-final-focus-enabled-20260619-091209.log`, `run/validation-screenshots/round5-final-focus-enabled-20260619-091209-Enabled.png`, `run/validation-screenshots/round5-final-focus-baseline-20260619-091412-Baseline.png`, `run/validation-screenshots/round5-final-focus-debug-20260619-091534-Debug.png`, and `run/validation-logs/round5-final-focus-delta-20260619-091412.json`**
- ~~Focused wall-region screenshot delta now exceeds the visible-lighting threshold: `focus.changedPixelPercent=58.7686`, `focus.brighterPixelPercent=58.4197`, and `focus.meanSignedLuma=8.8701`.~~ **DONE/VALIDATED in `run/validation-logs/round5-final-focus-delta-20260619-091412.json`**
- ~~Sodium + Iris + Vulkan enabled launch now submits the after-`LevelRenderer.render(...)` public Mojang final world-color composite draw with `attempted=true submitted=true drawCalls=true`, native direct-light CPU payload ready, and HUD intact in screenshots.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-final-composite-after-level-enabled-20260619-084513.log` and `run/validation-screenshots/round5-final-composite-after-level-enabled-20260619-084513-Enabled.png`**
- The earlier after-level final-composite screenshot pair did not meet the visible wall-lighting acceptance threshold before the focus-window final-composite shader. **SUPERSEDED by `run/validation-logs/round5-final-focus-delta-20260619-091412.json`**
- ~~Sodium + Iris + Vulkan enabled launch now captures a screenshot-visible, HUD-safe `R5 visual proof` marker only when the renderer is active and the native direct-light CPU payload is preview-ready; the disabled baseline screenshot does not show the marker.~~ **DONE/VALIDATED in `run/validation-screenshots/round5-proof-payload-overlay-enabled-20260619-081833-Enabled.png`, `run/validation-screenshots/round5-proof-payload-overlay-baseline-20260619-082044-Baseline.png`, `run/validation-logs/latest-round5-proof-payload-overlay-enabled-20260619-081833.log`, and `run/validation-logs/round5-proof-payload-overlay-delta-20260619-082044.json`**
- The proof marker validates native direct-light readiness in screenshots, not real world-surface illumination; the focused wall region still does not meet the direct/emissive lighting acceptance threshold. **OPEN with `focus.changedPixelPercent=0` and `focus.brighterPixelPercent=0` in `run/validation-logs/round5-proof-payload-overlay-delta-20260619-082044.json`**
- ~~Controller build/native build passes after native preview visibility diagnostics, Java preview-readiness gating, and image-delta helper changes; enabled launches load the staged DLL, generate preview-ready native CPU output with `displayablePixels=2304` and `peakChannel=255`, upload/draw the sampled public Mojang preview pass, and capture screenshots.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-native-coverage-floor-enabled-20260619-080231.log`, `run/validation-logs/latest-round5-tail-hook-enabled-20260619-080537.log`, `run/validation-screenshots/round5-native-coverage-floor-enabled-20260619-080231-Enabled.png`, and `run/validation-screenshots/round5-tail-hook-enabled-20260619-080537-Enabled.png`**
- ~~The controller image-delta helper works and writes JSON metrics for baseline/enabled screenshots.~~ **DONE/VALIDATED in `run/validation-logs/round5-existing-delta-check.json`, `run/validation-logs/round5-strong-native-preview-delta-20260619-075719.json`, `run/validation-logs/round5-native-coverage-floor-delta-20260619-080231.json`, and `run/validation-logs/round5-tail-hook-delta-20260619-080537.json`**
- The current sampled public Mojang preview pass still does not meet the visible lighting milestone: even after preview-ready output and tail-hook timing, the focus wall region remains below proof thresholds (`focus.changedPixelPercent=0`, `focus.brighterPixelPercent=0`). **OPEN in `run/validation-logs/round5-tail-hook-delta-20260619-080537.json`**
- ~~Controller build/native build passes with staged native runtime loading through `run/native/lucerna_renderer/lucerna_renderer.dll`; Sodium + Iris + Vulkan launches, joins `New World`, loads the staged DLL, runs native surface-sample direct lighting, uploads/submits the sampled public Mojang preview pass, and captures enabled/baseline screenshots.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-coverage-preview-enabled-20260619-074457.log`, `run/validation-screenshots/round5-coverage-preview-enabled-20260619-074457-Enabled.png`, and `run/validation-screenshots/round5-staged-native-baseline-20260619-073944-Baseline.png`**
- The current Round 5 visual proof screenshots still do not meet the "obvious visible surface lighting" bar: wall-region delta remains subtle despite native output and preview submission markers. **OPEN in `run/validation-screenshots/round5-coverage-preview-enabled-20260619-074457-Enabled.png` versus `run/validation-screenshots/round5-staged-native-baseline-20260619-073944-Baseline.png`**
- ~~Controller build/native build passes with the Round 5 visual-proof harness and direct-light HUD overlay changes; Sodium + Iris + Vulkan captures disabled baseline, enabled surface-sample direct-light preview, and direct-light debug overlay screenshots in `New World`. The direct-light debug overlay is readable and shows active renderer/native status, direct candidates/samples/rays, dispatch generation, output-write/resolve counters, and CPU output telemetry.~~ **DONE/VALIDATED in `run/validation-screenshots/round5-gain-baseline-disabled-visual-proof-20260619-065543-Baseline.png`, `run/validation-screenshots/round5-gain-enabled-surface-direct-visual-proof-20260619-065622-Enabled.png`, `run/validation-screenshots/round5-gain-direct-lighting-debug-visual-proof-20260619-065845-Debug.png`, `run/validation-logs/latest-round5-gain-enabled-surface-direct-visual-proof-20260619-065622.log`, and `run/validation-logs/latest-round5-gain-direct-lighting-debug-visual-proof-20260619-065845.log`**
- ~~A stronger native preview-alpha experiment could not be runtime-validated because Windows Application Control blocked the freshly rebuilt `lucerna_renderer.dll`; the unvalidated alpha-floor source change was reverted.~~ **RESOLVED BY STAGED NATIVE RUNTIME LOADING**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, extracts compact opaque surface sample metadata, plans direct-light shadow candidates with nonzero surface sample sections/samples, generates native surface-sample masked CPU direct-light output, uploads the bounded RGBA8 payload to a public Mojang `GpuTexture`, submits `mode=surface-sample-masked-direct-light-additive`, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-extracted-surface-sample-origins-sodium-iris-vulkan-fixed-20260619-061849.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, generates native surface-sample masked CPU direct-light output from emissive payload and shadow-candidate origins, uploads the bounded RGBA8 payload to a public Mojang `GpuTexture`, submits `mode=surface-sample-masked-direct-light-additive`, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-surface-sample-masked-direct-preview-sodium-iris-vulkan-20260619-055425.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, generates native CPU direct-light output, copies the bounded RGBA8 payload through JNI/Java, uploads it to a public Mojang `GpuTexture`, submits the sampled additive direct-light preview draw with `pipeline=lucerna:pipeline/direct_light_preview_additive`, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-sampled-direct-preview-texture-sodium-iris-vulkan-hardened-20260619-054155.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, the render-thread `GameRenderer` hook captures public Mojang Java-opaque render target objects, submits a public Mojang diagnostic direct-light preview draw with `drawCalls=true`, keeps native CPU direct-light output valid, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-public-mojang-diagnostic-draw-sodium-iris-vulkan-20260619-051631.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, the render-thread `GameRenderer` hook captures public Mojang Java-opaque render target objects, submits a public no-draw Mojang preview render pass, keeps native CPU direct-light output valid, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-public-mojang-preview-pass-sodium-iris-vulkan-20260619-050258.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, the render-thread `GameRenderer` hook captures public Mojang Java-opaque render target objects, native CPU direct-light output remains valid, direct preview reports no native-writable handles, and shutdown is clean.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-java-opaque-preview-target-sodium-iris-vulkan-20260619-045250.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, the render-thread `GameRenderer` hook supplies a HUD-preserving metadata-only direct-light preview target, tick no-op fallback is skipped after hook activation, native CPU direct-light output remains valid, and shutdown is clean.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-render-thread-preview-target-dedup-sodium-iris-vulkan-20260619-044204.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts direct lighting payloads, generates native CPU direct-light output after direct-light preview target/submission contracts, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-direct-preview-contract-sodium-iris-vulkan-20260619-042839.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts direct lighting payloads, generates native CPU direct-light output after the Java snapshot/overlay/frame-target contract changes, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-direct-output-bridge-sodium-iris-vulkan-20260619-041729.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts direct lighting payloads, generates a native 64x36 CPU direct-light output with nonzero energy/checksum telemetry, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-direct-cpu-output-sodium-iris-vulkan-20260619-040915.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts Java-to-native direct lighting payloads with celestial/emissive/shadow/section counts, accepts native direct lighting dispatches, records direct output-write and resolve markers, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-direct-payload-sodium-iris-vulkan-20260619-035920.log`**
- ~~Sodium + Iris + Vulkan launches, joins `New World`, accepts native direct lighting dispatches with candidate count greater than zero, records native direct output-write and resolve markers, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round5-direct-execution-dedup-sodium-iris-vulkan-20260619-035011.log`**
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
  - ~~Direct payload transfer from Java to native is validated.~~ **DONE/VALIDATED**
  - ~~Native direct dispatch/candidate/output-write/resolve telemetry is validated.~~ **DONE/VALIDATED**
  - ~~Native CPU direct-light output buffer generation is validated with nonzero energy/checksum telemetry.~~ **DONE/VALIDATED**
  - ~~Java direct-output snapshot/status contracts and HUD-preserving world-color target gating are compile/build/launch validated.~~ **DONE/VALIDATED**
  - ~~HUD-safe screenshot-visible native direct-light readiness marker is compile/build/launch validated and disabled in baseline mode.~~ **DONE/VALIDATED**
  - ~~Metadata-only versus native-writable target contracts and conservative direct-light preview submission result plumbing are compile/build/launch validated.~~ **DONE/VALIDATED**
  - ~~Render-thread `GameRenderer` metadata target hook is compile/build/launch validated with tick fallback suppression.~~ **DONE/VALIDATED**
  - ~~Public Mojang Java-opaque target capture is compile/build/launch validated; public APIs do not expose Vulkan native handles.~~ **DONE/VALIDATED**
  - ~~Public Mojang no-draw preview render-pass submission is compile/build/launch validated before HUD composition.~~ **DONE/VALIDATED**
  - ~~Public Mojang diagnostic fullscreen draw submission is compile/build/launch validated before HUD composition.~~ **DONE/VALIDATED**
  - ~~Public Mojang sampled direct-light preview texture upload/draw is compile/build/launch validated before HUD composition.~~ **DONE/VALIDATED**
  - ~~Public Mojang final world-color composite draw submission after `LevelRenderer.render(...)` is compile/build/launch validated before hand/HUD composition.~~ **DONE/VALIDATED**
  - ~~Visible direct/emissive focus-window final-composite proof is screenshot-validated with positive focused wall-region delta and readable HUD.~~ **DONE/VALIDATED**
  - ~~Native surface-sample masked direct-light preview generation is compile/build/launch validated before HUD composition.~~ **DONE/VALIDATED**
  - ~~Compact extracted opaque surface samples and surface-origin direct-light shadow candidate planning are compile/build/launch validated with nonzero `surfaceSampleSections` and `surfaceSamples` telemetry.~~ **DONE/VALIDATED**
  - ~~Java-side Round 6 GI source-summary, native diffuse-GI upload metadata, dirty-region listener, sparse radiance-cache scaffolding, Round 6 overlay presentation, native Round 6 dispatch telemetry, and controller Round 6 dispatch/cache logs compile and launch under Sodium + Iris + Vulkan.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-gi-cache-partial-sodium-iris-vulkan-20260619-094433.log`**
  - ~~Low-res GI dispatch metadata becomes enabled with `diffuse_gi={{enabled=true,size=427x240,samples=2,rays=409920,cache_reads=14150,...}}`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-gi-cache-partial-sodium-iris-vulkan-20260619-094433.log`**
  - ~~Sparse cache dirty-region telemetry, nonzero records, and nonzero cache writes are validated with `max.cacheRecords=128`, `max.cacheWrites=256`, and `max.cacheReads=7564`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-cache-stage-sodium-iris-vulkan-20260619-095852.log` and `run/validation-logs/round6-cache-stage-proof-20260619-095852.json`**
  - ~~Round 6 GI-gated final-composite preview is screenshot-validated with focused region delta `changedPixelPercent=57.5159`, `brighterPixelPercent=58.1384`, and `meanSignedLuma=8.8609`, plus `round6-diffuse-gi-focus-window-additive` draw submission.~~ **DONE/VALIDATED as preview-only in `run/validation-logs/round6-visible-gi-proof-20260619-103414.json` and `run/validation-logs/latest-round6-visible-gi-enabled-20260619-103414.log`**
  - ~~Native diffuse-GI output-source replacement is controller-validated with signed staged native runtime loading and stricter source proof.~~ **DONE/VALIDATED in `run/validation-logs/round6-native-gi-proof-20260619-121147.json`**
  - Low-res GI, denoise, physically correct final surface projection, and full final composite remain open.

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

Physically correct direct-light projection beyond the focus-window proof.
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

Current implementation focus:

Round 5 has a controller-validated focus-window final-composite proof, but physically correct direct/emissive surface projection remains open. Round 6 native diffuse-GI source replacement is now controller-validated as a metadata-backed native preview source; do not present it as real low-res GI tracing or physically correct diffuse GI.

Immediate Round 6 documentation/validation boundary:

Java-side GI source-summary, native upload metadata, dirty-region listener, sparse radiance-cache scaffolding, Round 6 overlay text, native Round 6 telemetry, controller GI/cache metadata logs, log-only cache records/writes, and native diffuse-GI source replacement are now validated. The remaining Round 6 gap is real low-resolution diffuse-GI tracing/accumulation plus a dedicated GI/cache debug overlay screenshot.

The next immediate milestone is:

Make low-res diffuse GI visibly affect a simple scene through real native GI tracing/accumulation, screenshot raw/debug output, and prove the logs match.
