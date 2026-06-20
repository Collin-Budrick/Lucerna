# Lucerna Parallel Implementation Plan

## Progress Audit

Current controller estimate: **about 99.78% complete against this file**.

This percentage is conservative:
- Rounds 0-3 are mostly implemented and controller-validated.
- Round 4 has Java/native/shader scaffolding, dirty-region GI inputs, direct shadow candidate planning, compact extracted opaque surface sample metadata, native direct/GI/post handoff DTOs, payload-category telemetry, native dispatch validation, Round 5 direct payload handoff, direct execution/output-marker telemetry, a native CPU direct-light output buffer with energy/checksum telemetry, Java direct-output snapshot/status contracts, HUD-preserving world-color frame target contracts, metadata-only versus Java-opaque versus native-writable attachment contracts, direct-light preview submission result plumbing, a validated render-thread `GameRenderer.renderLevel` target hook, validated public Mojang Java-opaque target capture, validated public Mojang no-draw preview render-pass submission, validated public Mojang diagnostic draw submission, validated public Mojang sampled direct-light preview texture upload/draw submission, validated extracted surface-sample direct-light origins, validated native surface-sample masked direct-light preview generation, staged native runtime loading that avoids the earlier Windows Application Control block, a controller visual-proof harness, screenshot-validated in-world direct-light debug overlay text, and a no-marker native Round 6 diffuse-GI surface-composite screenshot proof.
- Latest controller work moves the final world-color composite hook into `GameRenderer.renderLevel` before hand/HUD composition and before Minecraft's F2 screenshot capture, uses full render-area public Mojang passes with scissor disabled, suppresses proof overlays for no-marker validation, and validates a no-marker native diffuse-GI surface-composite screenshot delta on the actual affected wall/ground surface region.
- Latest controller work also adds a HUD-safe, explicitly labeled Round 5 visual-proof marker gated by the same preview-ready native direct-light CPU payload used by the sampled draw path. This proves screenshot-visible native direct-light readiness, but it is not a substitute for real surface lighting.
- Latest controller work moves the final world-color composite hook from `GameRenderer.renderLevel` tail to immediately after `LevelRenderer.render(...)`, removes the older sampled public preview draw from that hook, adds final-composite frame/pass intent contracts, and validates final-composite public Mojang draw submission before hand/HUD composition. Screenshot delta still shows no focused wall-region brightening, so real surface lighting remains open.
- Latest controller work restores the launchable native DLL path after the unvalidated native spatialization generated an Application Control-blocked DLL, then moves the Round 5 visible proof into a final-composite-only focus-window shader. Sodium + Iris + Vulkan now loads the native DLL, submits the focus-window final composite, and screenshot delta proves visible brightening in the focused wall region while preserving the HUD.
- Latest controller work adds Round 9 virtualized chunk geometry metadata contracts, native metadata-only cluster/culling telemetry, a Round 9 debug overlay/status path, and a controller proof harness. Sodium + Iris + Vulkan captures flat/open, interior/wall-facing, and high-distance/open terrain overlay screenshots with clean shutdown and log proof for cluster counts, visible counts, upload bytes, generation counters, and indirect draw placeholders. Boundary: culled clusters remain `0` because real GPU/frustum/occlusion culling is still metadata-only.
- Current Round 6 preparation has controller-validated Java/cache scaffolding for GI source summaries, native diffuse-GI upload metadata, dirty-region listener hooks, sparse voxel radiance cache records/confidence/invalidation/debug status, Round 6 debug overlay presentation, native Round 6 dispatch telemetry, a bounded native diffuse-GI visible-signal telemetry marker, a GI-labeled final-composite preview path, and a screenshot-delta proof for a Round 6 GI-gated preview. Sodium + Iris + Vulkan launch validation proves low-res GI dispatch metadata can become enabled with nonzero rays/cache reads, a separate cache proof validates nonzero cache records/writes, and the GI preview path produces a focused screenshot difference. The native diffuse-GI output-source replacement is now controller-validated with signed local native staging and a stricter source proof that rejects the temporary direct-light RGBA payload. True native low-res GI output/tracing remains open because the proof still validates a metadata-backed native preview source, not physical GI tracing.
- Latest controller work adds Round 7 signal-separated denoise contracts, final composite mode/status controls, native denoise scaffold telemetry, first-practical CPU denoised diffuse-GI RGBA8 output, controller-selectable baseline/direct/raw-GI/denoised-GI/final/debug modes, a controller-only Round 7 proof helper, native surface-response strengthening, a distinct final direct-plus-raw-plus-denoised composite draw path, auto-selected affected-surface proof crops, clearer final-mode source telemetry, particle/translucency/temporal composite-stability capture modes, scene-tied GI metadata DTOs, native CPU denoise quality telemetry, and first-lighting quality overlay/status lines. Build/native/signing and Sodium + Iris + Vulkan world-join launches validate raw-GI metadata, CPU denoised-output generation/readback, composite placeholder metadata, explicit `realDenoiseShaderOutput=false`, HUD-safe selected-mode screenshots, a passing focused-region direct/raw/denoised/final visual proof, and a passing particle/translucency/temporal final-composite stability proof.
- Latest controller work fixes the root cause behind the "looks like default Minecraft" screenshots: Lucerna was calling Mojang `RenderPass.draw(...)` with the wrong argument order, so the submitted fullscreen draw had an effective vertex count of zero. The public Mojang draw path now uses Mojang's `draw(vertexCount, instanceCount, firstVertex, firstInstance)` order, and a capped Round 7 surface proof validates a visible source-gated wall-surface composite delta with baseline/enabled/debug screenshots, no proof marker, no focus-window-only fallback, no temporary direct-light substitution, and no native errors. Boundary: this proves the Java/public final-composite draw path and CPU/readback preview contribution are screenshot-visible; it is still not real shader denoise quality or physically correct GI.
- Latest controller work advances the next stage with parallel-agent patches: native CPU/readback diffuse-GI output now has scene-spatial/material/cache response telemetry, the public GI surface shader removes the earlier hard rectangle and shapes contribution with payload luminance/chroma/gradient cues, debug overlay wording explicitly labels the current path as CPU/readback preview rather than shader GI, and the Round 7 proof harness now rejects full-screen/rectangular washout. Controller build/native/signing passes, and Sodium + Iris + Vulkan screenshot validation passes the stricter scene-shaped Round 7 surface proof. Boundary: the visual result is broader and still preview-grade; it is validated as scene-shaped CPU/readback composite evidence, not physical GI tracing or real shader denoise.
- Latest controller work fixes misleading validation capture: Windows fallback screenshots were capturing the wrong desktop window or a blank Vulkan surface, so Lucerna now has a controller-only in-client screenshot hook gated by `LUCERNA_CONTROLLER_SCREENSHOT_REQUEST`. The Round 7 proof harness now uses real Minecraft screenshots from `net.minecraft.client.Screenshot`, excludes hand/HUD regions for raw-vs-denoised roughness selection, and validates the dedicated `lucerna:core/round7_denoised_gi_visual` shader-side visual shaping path with a passing same-build baseline/direct/raw-GI/denoised-GI/final/debug proof.
- Latest controller work adds stricter physical-GI source scoring, explicit shader-denoise boundary contracts, final-composite source-authenticity gates, native proof-boundary telemetry, compact stage timing/status overlay lines, and stronger in-client screenshot/provenance validation helpers. Controller validation passes parser checks, `git diff --check`, `gradlew build`, `gradlew buildNative stageNativeRuntime`, and a fresh Sodium + Iris + Vulkan Round 7 denoise/composite proof in `run/validation-logs/round7-controller-denoise-composite-proof-20260620-012426.json` using in-client baseline/direct/raw-GI/denoised-GI/final/debug screenshots. Boundary: the strict repeated temporal/flicker proof harness is now implemented and captures in-client stable/moved sequences, but the latest moved-camera focused-region assertion still fails (`run/validation-logs/round7-controller-composite-stability-proof-20260620-015756.json`) because the measured world region changed `0%`; stronger temporal/flicker validation remains open.
- Latest controller work integrates another six-agent pass for Round 7 proof hardening: native CPU/readback GI now carries physical-evidence boundary telemetry, Java GI source summaries expose scene/source coupling scores, shader-denoise contracts name the remaining real-shader quality gates, final-composite source identities distinguish CPU-denoised from shader-denoised output, debug overlay/status lines show compact timing/proof boundaries, and the controller screenshot helper rejects stale screenshots more strictly without blocking first in-client captures. Controller validation passes parser checks, `git diff --check`, `gradlew build`, `gradlew buildNative stageNativeRuntime`, and a fresh Sodium + Iris + Vulkan in-client Round 7 denoise/composite proof in `run/validation-logs/round7-denoise-composite-proof-20260620-031224.json`; screenshots are `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-025943-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030216-Direct.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030448-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030721-DenoisedGi.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030952-FinalComposite.png`, and `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-031224-Debug.png`. Key metrics: `direct.focus.changedPixelPercent=89.5399`, `raw.focus.changedPixelPercent=65.2127`, `denoise.focus.changedPixelPercent=5.303`, `denoise.roughness.meanAbsNeighborLumaReductionPercent=33.4676`, `denoise.roughness.rmsNeighborLumaReductionPercent=40.6378`, `final.focus.changedPixelPercent=88.5598`, `metadataOnlyPreviewPresent=False`, `temporaryDirectLightSourcePresent=False`, `focusWindowOnlyPresent=False`, and `nativeErrorPresent=False`. Boundary: this validates CPU/readback denoised output plus final composite proof, not real shader-side denoise or physically correct GI tracing.
- The remaining work is the hardest part of making the image production-quality: real shader denoise, stronger temporal/flicker proof, physically stronger GI/tracing, real GPU chunk culling, and later tracing/reuse systems.

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
  - ~~Status: Mojang `RenderPass.draw(...)` ordering is fixed for Lucerna public draw submissions, and capped Round 7 baseline/enabled/debug screenshot proof validates visible source-gated final-composite output on the affected wall surface without proof-marker, focus-window-only, temporary-source, or native-error evidence.~~ **DONE/VALIDATED**
  - ~~Status: Round 7 scene-shaped surface proof now validates a softer source-gated composite that passes anti-washout diagnostics: no full-screen/rectangular washout, localized scene-shaped delta present, proof markers hidden, focus-window-only and temporary-source evidence rejected, and debug overlay boundary text visible.~~ **DONE/VALIDATED**
  - ~~Status: Controller-only in-client screenshots now replace unreliable OS/F2 capture for visual proof, the dedicated Round 7 denoised GI visual shader resource is wired into the public Mojang denoised/final pipeline, and same-build baseline/direct/raw-GI/denoised-GI/final/debug screenshots pass the focused raw-vs-denoised/final assertion with hand/HUD-excluded denoise roughness selection.~~ **DONE/VALIDATED**
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
~~Direct/emissive lighting has a visible effect in-world.~~ **DONE/VALIDATED in `run/validation-logs/round5-direct-surface-proof-20260619-181014.json`; focused-surface metrics: `focus.changedPixelPercent=57.411`, `focus.brighterPixelPercent=55.8431`, `focus.meanSignedLuma=42.8193`**
~~Controller screenshots show clear before/after difference.~~ **DONE/VALIDATED with disabled baseline `run/validation-screenshots/round5-direct-surface-baseline-20260619-180836-Baseline.png`, enabled capture `run/validation-screenshots/round5-direct-surface-enabled-20260619-180925-Enabled.png`, debug capture `run/validation-screenshots/round5-direct-surface-debug-20260619-181014-Debug.png`, and focused comparison `run/validation-screenshots/round5-direct-surface-focused-comparison-20260619-181014.png`**
~~Native logs prove lighting dispatch executed with candidate count, output-write marker, and resolve marker.~~ **DONE/VALIDATED for the current scaffold**
HUD readability is screenshot-validated for the direct-light debug overlay; targeted translucency corruption validation remains open.
~~Disabling Lucerna restores vanilla/no-op behavior for the direct-surface proof scene.~~ **DONE/VALIDATED by the Round 5 direct-surface baseline screenshot and proof JSON**
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

Immediate Round 6 slice:

Move from metadata-backed native diffuse-GI preview output to deterministic native low-resolution GI output. The native path should produce a repeatable low-res diffuse-GI buffer from bounded scene inputs, not a visibility marker, metadata-derived brightness field, direct-light payload substitute, or focus-window-only proof source. The output can remain simple and low quality, but it must be attributable to native GI ray/sample/cache work and stable enough for controller screenshot and log comparison from the same scene.

Current controller result for this slice:

Native now has a build-green deterministic diffuse-GI-looking CPU output path that derives the bounded RGBA signal from GI ray/sample activity, cache read/write activity, and the latest native direct-light payload's emissive/celestial scene metadata. Java/log wording now identifies the source as native diffuse-GI output, and the HUD proof badge uses the neutral "CPU output proof" label instead of the older "R5 visual proof" label. Controller validation launched Sodium + Iris + Vulkan and captured enabled/baseline/debug screenshots, but the focused wall-region proof still failed: `run/validation-logs/round6-deterministic-native-gi-v5-proof-20260619-123851.json` reports `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `max.giRays=409920`, `max.giCacheReads=13812`, and `nativeErrorPresent=False`, but `focus.changedPixelPercent=0`, `focus.brighterPixelPercent=0`, and `focus.meanSignedLuma=-0.114`. Treat this as compile/runtime progress only, not visible-GI validation.

Latest controller result:

Native status now records deterministic diffuse-GI CPU output markers, nonzero energy/checksum booleans, and scene-tied input counters from the latest direct-light payload. The Round 6 final-composite shader now samples the native GI texture across the world target instead of a narrow focus-window mapping, and the debug overlay explicitly labels Direct Lighting as Round 5 while adding low-res GI/cache status lines. Controller build/native/signing passed and Sodium + Iris + Vulkan enabled/baseline/debug launches completed, but the formal focused wall proof still failed. `run/validation-logs/round6-world-gi-composite-v5-proof-20260619-130129.json` reports `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `max.giRays=409920`, `max.giCacheReads=14726`, `nativeErrorPresent=False`, and `focus.meanSignedLuma=0.5173`, but `focus.changedPixelPercent=0` and `focus.brighterPixelPercent=0`. Treat this as stronger runtime/log evidence and partial visual movement only, not visible-GI completion.

Latest focused-pixel investigation:

The proof script is not the blocker. The controller inspected the latest failed screenshots directly and confirmed the focus region receives only sub-threshold per-pixel deltas, while stronger changes land elsewhere in the frame. A follow-up native broad-projection signal plus per-pixel composite shader path still failed the formal proof: `run/validation-logs/round6-pixel-gi-composite-v2-proof-20260619-131345.json` reports `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `max.giRays=409920`, `max.giCacheReads=13885`, and `nativeErrorPresent=False`, but `focus.changedPixelPercent=0`, `focus.brighterPixelPercent=0`, and `focus.meanSignedLuma=0.2402`. Treat this as additional build/runtime progress only. The remaining issue is still real screen-space delivery/surface projection into the wall proof region.

Latest controller result:

~~No-marker native diffuse-GI surface-composite proof now passes when the controller measures the actual affected wall/ground surface region instead of the older central default crop.~~ **DONE/VALIDATED in `run/validation-logs/round6-no-marker-fullarea-lower-right-proof-20260619-150705.json` with `focus.changedPixelPercent=37.9981`, `focus.brighterPixelPercent=37.9981`, `focus.meanSignedLuma=18.5186`, `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `roundSixNoMarkerSurfaceDrawPresent=True`, `proofMarkerContaminationPresent=False`, `max.giRays=409920`, `max.giCacheReads=12090`, and `nativeErrorPresent=False`.**

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
For the next deterministic native-output slice, controller proof must also show:
native low-res GI output-source markers that identify real GI sample/output population rather than metadata-backed preview synthesis,
deterministic output dimensions, sample count, nonzero write count, nonzero energy/checksum, and stable readiness reason,
scene input counters tied to the proof scene, including emissive or skylight sources, surface/sample counts, cache reads or writes when enabled, and any rejected/fallback sample counts,
baseline/enabled/debug screenshots from one controller launch, with a focused delta region that corresponds to the GI-lit surface or cave area,
debug/raw GI view or overlay evidence from the same scene,
no fallback to temporary direct-light RGBA payloads, metadata-only preview source, focus-window-only brightness, or proof-marker-only rendering.

Visible-GI proof is separate from cache-write proof:

Controller must keep the log-only cache record/write evidence in a separate artifact from screenshot delta evidence. Nonzero cache records or `cache_writes` prove cache activity only; they do not prove visible diffuse GI. A Round 6 visible-GI pass still needs baseline/enabled screenshots, a focused image-delta report for the lit surface or cave region, and a debug GI/cache screenshot from the same scene.

Controller-visible GI proof can use `scripts/Assert-LucernaRound6VisibleGiProof.ps1` to combine baseline/enabled focused screenshot delta, optional debug screenshot presence, and optional Round 6 GI dispatch log markers into one JSON report. This helper supports validation capture only; it does not replace controller-run Minecraft launch/screenshots and does not by itself mark visible GI validated.

When replacing the temporary direct-light RGBA source, controller validation must require native diffuse-GI output-source markers distinct from the old temporary payload. Use `scripts/Invoke-LucernaVisualProof.ps1 -ValidationProfile Round6NativeDiffuseGi` for capture-time log gating, and run `scripts/Assert-LucernaRound6VisibleGiProof.ps1` with `-RequireLogProof -RequireNativeGiOutputSource` for the final proof JSON. The stricter helper path should fail if the launch log still reports `temporarySourceReady=true` or the readiness reason that says the GI preview is using the current direct-light RGBA payload. For no-marker Round 6 surface-composite validation, use `-ValidationProfile Round6NativeDiffuseGiNoMarker`; it requires the native diffuse-GI surface draw log and rejects temporary-source, focus-window, and proof-marker evidence during capture.

`scripts/Assert-LucernaRound6VisibleGiProof.ps1` now reports `proofClarity.classification` in its JSON/console output. In particular, `round6_draw_present_but_no_marker_screenshot_delta_failed` means the native Round 6 surface draw log was present and no proof-marker contamination was detected, but the focused screenshot delta still failed the visible-GI thresholds. This is useful controller evidence, but it must remain a failing proof until the screenshot thresholds pass.

For the deterministic native low-res GI slice, these proof helpers may need stricter markers or a new profile. The controller-owned proof should fail if the log only proves native output-source replacement, metadata-backed preview readiness, cache writes, or screenshot-visible focus-window brightness without deterministic GI sample/output counters.

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
~~Low-res GI visibly affects the scene through the native diffuse-GI surface-composite path.~~ **DONE/VALIDATED for no-marker surface-composite proof**
~~Radiance cache exists and produces nonzero records/writes under controller-run gameplay.~~ **DONE/VALIDATED for log-only cache-write proof**
Dirty block/emissive changes affect cache state in validated runtime logs/screenshots. **PARTIAL/LOG-VALIDATED**
Debug overlay can show GI/cache state. **PARTIAL/PREVIEW DEBUG SCREENSHOT VALIDATED**
~~Screenshots demonstrate visible GI difference.~~ **DONE/VALIDATED for no-marker native diffuse-GI surface-composite proof**
~~Logs validate dispatch, dirty-region invalidation telemetry, cache records, and cache writes.~~ **DONE/VALIDATED**

Round 6 evidence split:

- ~~Cache record/write evidence: log-only, pass/fail, based on nonzero sparse/cache records plus nonzero cache writes in the Round 6 dispatch/cache telemetry.~~ **DONE/VALIDATED**
- ~~Visible-GI preview evidence: screenshot-based, pass/fail, based on baseline/enabled/debug captures and objective region delta for the target surface or cave area.~~ **DONE/VALIDATED for the GI-gated preview path in `run/validation-logs/round6-visible-gi-proof-20260619-103414.json`**
- ~~Real visible low-res GI evidence from a native diffuse-GI output source, not the temporary direct-light RGBA source, has a passing no-marker screenshot proof.~~ **DONE/VALIDATED in `run/validation-logs/round6-no-marker-fullarea-lower-right-proof-20260619-150705.json`**
- ~~Native-output replacement evidence includes the same screenshot proof plus a log-proof source gate that identifies native diffuse-GI output and rejects the temporary direct-light payload source marker.~~ **DONE/VALIDATED in `run/validation-logs/round6-native-gi-proof-20260619-121147.json`; `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `focus.changedPixelPercent=42.7753`, `focus.brighterPixelPercent=41.4972`, `focus.meanSignedLuma=6.5612`**
- Round 6 no-marker surface-composite evidence is controller-validated for a native diffuse-GI output source reaching visible world surfaces. This is the current Round 6 visual proof boundary; do not reinterpret it as full physically correct GI tracing, denoise, temporal accumulation, or final composite quality.
- Deterministic native low-res GI sample/output evidence remains a stricter follow-up until controller proof shows repeatable native GI sample/output population, nonzero GI writes/energy/checksum, scene-tied input counters, baseline/enabled/debug screenshots, and no metadata-backed preview/focus-window/proof-marker fallback.
- Deterministic native output implementation is build/launch green but screenshot-proof failed for the focused wall region. **OPEN in `run/validation-logs/round6-deterministic-native-gi-v5-proof-20260619-123851.json`; logs prove native source markers and no temporary direct-light fallback, but the wall-region visual delta is still zero.**
- Composite alignment/native telemetry follow-up is build/launch green but screenshot-proof still failed for changed/brighter-pixel thresholds. **OPEN in `run/validation-logs/round6-world-gi-composite-v5-proof-20260619-130129.json`; focused mean luma is positive, but individual focused-region pixels still do not cross the proof helper thresholds.**
- Native broad-projection plus per-pixel composite follow-up is build/launch green but screenshot-proof still failed. **OPEN in `run/validation-logs/round6-pixel-gi-composite-v2-proof-20260619-131345.json`; the proof helper is behaving correctly and the wall-region deltas remain below changed/brighter-pixel thresholds.**
- ~~Screenshot-visible Round 6 GI payload proof marker is controller-validated with signed native runtime loading, enabled/baseline/debug screenshots, native diffuse-GI source gating, and focused-region pixel-delta proof.~~ **DONE/VALIDATED as a proof marker only in `run/validation-logs/round6-gi-proof-overlay-final-proof-20260619-133223.json`, `run/validation-screenshots/round6-gi-proof-overlay-final-enabled-20260619-133223-Enabled.png`, `run/validation-screenshots/round6-gi-proof-overlay-final-baseline-20260619-133309-Baseline.png`, and `run/validation-screenshots/round6-gi-proof-overlay-final-debug-20260619-133348-Debug.png`; metrics: `focus.changedPixelPercent=99.9646`, `focus.brighterPixelPercent=84.7898`, `focus.meanSignedLuma=44.0353`, `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`.**
- The GI-readiness/debug proof marker remains proof-marker evidence only; it is useful as a path-readiness check but must not be used as the Round 6 no-marker world-surface proof.
- Combined Round 6 acceptance for the current milestone requires both tracks: log-only cache record/write proof and no-marker screenshot/log surface-composite proof. Both are now controller-validated, while physically correct GI tracing/accumulation remains a later quality target.
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
Capture a baseline/disabled screenshot from the same scene when possible so denoise evidence is not confused with generic Lucerna-on brightness.
Validate visual expectations:
denoised GI has less noise/flicker,
hard block edges remain reasonably sharp,
light does not bleed heavily through walls.
Validate logs contain:
raw GI input/output marker,
~~denoise dispatch metadata scaffold,~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-foundation-sodium-iris-vulkan-20260619-152554.log`**
~~raw input dimensions and first-practical CPU denoised output dimensions,~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-cpu-denoise-output-sodium-iris-vulkan-20260619-161055.log`**
history accepted/rejected counts,
depth/normal rejection stats if implemented,
no GPU/native errors.

Status:

- ~~Java signal-separated denoise contracts exist for diffuse GI, direct shadows, optional specular/AO placeholders, edge-rejection inputs, history counters, and output contracts.~~ **DONE/VALIDATED by controller build**
- ~~Native denoise execution scaffold reports whether denoise metadata was accepted and explicitly marks `signal_separated_denoise_metadata_scaffold_no_render_output` when the validated placeholder metadata is consumed.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-foundation-sodium-iris-vulkan-20260619-152554.log`**
- ~~Native first-practical CPU denoised diffuse-GI RGBA8 output is generated from the raw GI payload and read back through Java with changed-pixel/mean-delta telemetry while explicitly keeping `realDenoiseShaderOutput=false`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-cpu-denoise-output-sodium-iris-vulkan-20260619-161055.log`; key markers include `marker=first_practical_cpu_denoised_diffuse_gi_rgba8_generated`, `denoised_cpu_output_generated=true`, `denoisedOutputChangedPixels=60964`, `denoisedOutputMeanAbsDelta=2`, `denoisedPayloadReady=true`, and `realDenoiseShaderOutput=false`**
- ~~Controller-selected raw-GI and denoised-GI modes launch with Sodium + Iris + Vulkan, capture valid same-scene screenshots without proof-overlay contamination, and log the requested raw/denoised source paths.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-rawgi-20260619-165335.log`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-165335-RawGi.png`, `run/validation-logs/latest-round7-denoise-composite-denoisedgi-20260619-165412.log`, and `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-165412-DenoisedGi.png`**
- ~~Focused raw-vs-denoised visible-delta proof now passes with controller-selected auto surface crops and required log proof.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-assert-20260619-172730.json`; key metrics include `raw.focus.changedPixelPercent=56.0655`, `denoise.focus.changedPixelPercent=50.51`, and `denoise.focus.meanAbsLuma=25.1396`**
- ~~A refreshed same-scene direct/raw-GI/denoised-GI/final/debug proof now shows denoised GI differs from raw GI and reduces mean neighboring-luma roughness by `4.4313%` in the selected affected-surface region.~~ **DONE/VALIDATED in `run/validation-logs/round7-direct-raw-denoised-final-proof-20260619-184256.json`; boundary: this is first-practical CPU output/readback, not real shader denoise**
- Real denoise shader/output, stronger edge-preservation proof, particle/translucency-specific proof, and temporal flicker/noise comparison remain open.
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

Status:

- ~~Composite mode config/status scaffolding exists for base/vanilla, direct-only, GI-only, and final Lucerna composite presentation paths.~~ **DONE/VALIDATED by controller build**
- ~~Settings/debug UI can surface the composite mode/status foundation without claiming visual final-composite quality.~~ **DONE/VALIDATED by controller build**
- ~~Same-scene baseline/raw-GI/denoised-GI/final-mode screenshots are controller-captured through selectable composite modes, with HUD and hand visible and without proof-overlay contamination.~~ **DONE/VALIDATED for selected-mode capture in `run/validation-screenshots/round7-denoise-composite-baseline-20260619-165305-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-165335-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-165412-DenoisedGi.png`, and `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-165450-FinalComposite.png`**
- ~~Final-mode telemetry now logs `round7.finalCompositeMode=final-composite`, HUD-safe status, ready direct/GI/denoised sources, and a denoised-GI draw source instead of falling back to the raw-GI visual path once the denoised payload is ready.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-165450.log`**
- ~~A distinct final-composite selected path now submits raw native diffuse-GI plus CPU-denoised diffuse-GI sources, avoids relying on direct-light focus-window substitution, and passes focused-region visual proof with HUD/hand intact.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-172730.log`, `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-172730-FinalComposite.png`, and `run/validation-logs/round7-denoise-composite-assert-20260619-172730.json`; final metrics include `final.focus.changedPixelPercent=55.9353` and `final.focus.meanAbsLuma=23.0501`**
- ~~Direct-only mode capture is now included in the same controller proof set as baseline/raw-GI/denoised-GI/final/debug captures.~~ **DONE/VALIDATED in `run/validation-screenshots/round7-denoise-composite-direct-20260619-183213-Direct.png` and `run/validation-logs/latest-round7-denoise-composite-direct-20260619-183213.log`; direct metrics include `direct.focus.changedPixelPercent=14.6647` and `direct.focus.meanAbsLuma=1.9768`**
- ~~A refreshed final-composite proof now submits direct + raw native diffuse-GI + CPU-denoised diffuse-GI sources together, rejects metadata-only/focus-window/proof-marker/native-error contamination at the accepted final state, and produces a focused-region final screenshot delta.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-184206.log`, `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-184206-FinalComposite.png`, and `run/validation-logs/round7-direct-raw-denoised-final-proof-20260619-184256.json`; final metrics include `final.focus.changedPixelPercent=57.0475` and `final.focus.meanAbsLuma=31.5265`**
- ~~Particle/translucency/temporal final-composite stability proof now has controller-captured before/after screenshots, required log proof, visual inspection, and rejection checks for temporary direct-light source, proof-marker, focus-window-only, and native-error evidence.~~ **DONE/VALIDATED in `run/validation-logs/round7-composite-stability-proof-20260619-201227.json`, `run/validation-screenshots/round7-composite-stability-particlebaseline-20260619-200530-ParticleBaseline.png`, `run/validation-screenshots/round7-composite-stability-particlefinalcomposite-20260619-200654-ParticleFinalComposite.png`, `run/validation-screenshots/round7-composite-stability-translucentbaseline-20260619-200820-TranslucentBaseline.png`, `run/validation-screenshots/round7-composite-stability-translucentfinalcomposite-20260619-200942-TranslucentFinalComposite.png`, `run/validation-screenshots/round7-composite-stability-temporalstable-20260619-201104-TemporalStable.png`, and `run/validation-screenshots/round7-composite-stability-temporalmoved-20260619-201227-TemporalMoved.png`; key metrics include `particle.focus.changedPixelPercent=48.0306`, `translucency.focus.changedPixelPercent=48.0306`, `temporal.focus.changedPixelPercent=44.9255`, `temporal.focus.meanAbsLuma=6.1699`, `finalCompositePresent=True`, `hudPreservationPresent=True`, `temporaryDirectLightSourcePresent=False`, `proofMarkerPresent=False`, `focusWindowOnlyPresent=False`, and `nativeErrorPresent=False`. Boundary: visual inspection confirms real screenshot differences and intact HUD/hotbar/crosshair, but the image is still subtle and this does not prove real shader denoise or physically correct GI.**
- Scene-tied GI metadata and native CPU denoise quality telemetry are implemented and build/launch validated, but they remain scaffolding until real physical GI/shader-side denoise output is visible.

Round 7 evidence split:

- Raw GI evidence: baseline/raw-GI screenshots from the same camera plus logs proving the raw native GI source, raw dimensions, raw sample/write counters, and absence of proof-marker/focus-window-only fallback. Raw GI may be noisy; the proof is source identity and visible/raw debug delivery, not quality.
- Denoised GI evidence: raw-GI and denoised-GI screenshots from the same camera plus logs proving a denoise dispatch consumed the raw GI source and wrote a denoised output. The denoised screenshot must preserve block edges better than a blur-only pass and must not hide light leaks by merely dimming the signal.
- Final composite evidence: baseline, direct-only if available, GI-only, and final-composite screenshots from the same scene plus logs proving the selected composite mode, final world-color draw/dispatch, HUD-safe timing, and no double-composite/full-screen corruption.
- Evidence must stay separated. A passing final composite screenshot does not prove the raw GI buffer exists, a denoised debug view does not prove final composite correctness, and cache-write logs do not prove denoise quality.
- Controller proofs must reject temporary direct-light RGBA source substitution, metadata-only preview source, focus-window-only brightness, proof-marker-only rendering, and path names/log markers that show the artifact was captured from a readiness marker instead of the requested Round 7 mode.
- A controller-only helper exists at `scripts/Assert-LucernaRound7DenoiseCompositeProof.ps1` for combining same-scene raw/denoised/final screenshots with log markers into one JSON report. It is an assertion/reporting helper only; it does not launch Minecraft, run builds, or replace controller capture.
- A controller-only helper exists at `scripts/Assert-LucernaRound7CompositeStabilityProof.ps1` for combining particle, translucent, and temporal final-composite screenshots with log markers into one JSON report. It is an assertion/reporting helper only; it does not launch Minecraft, run builds, or replace controller capture.
- The helper now treats Round 7 as four separate evidence tracks: baseline-to-direct image delta, baseline-to-raw-GI image delta, raw-GI-to-denoised-GI image delta, and baseline-to-final-composite image delta. Its JSON reports per-track image/log marker status and rejection-marker status for temporary direct-light substitution, metadata-only/scaffold output, proof markers, focus-window-only evidence, and native/Vulkan errors.
- The helper now requires named log-marker families for the upcoming visual run when `-RequireLogProof` is used:
  - raw GI source markers such as `round7.rawGiSource=lucerna.lighting.diffuseGi`, `rawGiSource=lucerna.lighting.diffuseGi`, `rawGiOutput=lucerna.lighting.diffuseGi`, `rawGi=true`, `raw_gi_input_available=true`, or `raw_input_marker=...`;
  - denoise dispatch markers such as `first_practical_cpu_denoised_diffuse_gi_rgba8_generated`, `denoisedCpuOutputGenerated=true`, `denoised_cpu_output_generated=true`, `Lucerna Round 7 denoise`, `denoise dispatch`, or `mode=round7-denoised-gi`;
  - denoised GI output markers such as `round7.denoisedGiOutput=lucerna.denoise.diffuse`, `denoisedGiOutput=lucerna.denoise.diffuse`, `denoisedOutputResource=lucerna.denoise.diffuse`, `denoisedPayloadReady=true`, `denoisedPayloadEvidence=denoised_diffuse_gi_rgba8_first_practical_cpu_output`, `denoised_diffuse_gi_cpu_rgba8_output_generated_from_raw_gi`, or `denoised_output_marker=...`;
  - final composite mode markers such as `round7.finalCompositeMode=round7.composite.final.base_direct_gi`, `compositeEvidenceKey=round7.composite.final.base_direct_gi`, `evidenceKey=round7.composite.final.base_direct_gi`, `mode=FINAL_LUCERNA_COMPOSITE`, or `mode=final-lucerna-composite`;
  - HUD-safe final composite markers such as `round7.finalCompositeHudSafe=true`, `hudSafeFinalComposite=true`, `hud_preserved=true`, `HUD-safe final composite`, `before hand/HUD composition`, or `HUD remains readable`.
- The helper rejects proof-marker contamination and focus-window-only evidence by failing if proof markers, temporary direct-light source substitution, native errors, or submitted focus-window-only proof are the evidence source. Earlier rejected focus-window attempts no longer poison a later valid Round 7 GI-source final submission.
- Passing the helper is still not enough by itself to close Round 7. The controller must provide same-scene screenshots, relevant launch/render logs, and visual inspection that HUD/hand/translucency are intact and that denoise does not simply blur edges or dim the raw signal.
- ~~Controller build/native build/signing passes after the Round 7 denoise/composite foundation integration. Sodium + Iris + Vulkan launches, joins `New World`, accepts native lighting dispatches with enabled stages, and logs `marker=signal_separated_denoise_metadata_scaffold_no_render_output` from the native denoise metadata scaffold.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-foundation-sodium-iris-vulkan-20260619-152554.log`**
- ~~Round 7 evidence telemetry now separates raw GI metadata, denoised-output intent, composite placeholder metadata, edge/history counters, and explicit no-real-denoise-output status in the native denoise scaffold log.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-evidence-telemetry-sodium-iris-vulkan-20260619-154001.log`; key markers include `rawGi=true`, `denoisedIntent=true`, `composite=placeholder`, `realDenoiseShaderOutput=false`, and `marker=signal_separated_denoise_metadata_scaffold_no_render_output`**
- ~~Sodium + Iris + Vulkan enabled launch validates first-practical CPU denoised diffuse-GI output after controller build/native/signing: the game joins `New World`, accepts native lighting dispatches, generates/readbacks the CPU denoised payload, logs changed-pixel/mean-delta telemetry, keeps `realDenoiseShaderOutput=false`, and shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-cpu-denoise-output-sodium-iris-vulkan-20260619-161055.log`**
- ~~Sodium + Iris + Vulkan selected-mode launches now capture baseline, raw-GI, denoised-GI, and final-mode screenshots from the same test scene with proof overlays hidden, HUD/hand intact, and logs proving the selected raw/denoised/final paths executed.~~ **DONE/VALIDATED for selected-mode capture in `run/validation-logs/latest-round7-denoise-composite-baseline-20260619-165305.log`, `run/validation-logs/latest-round7-denoise-composite-rawgi-20260619-165335.log`, `run/validation-logs/latest-round7-denoise-composite-denoisedgi-20260619-165412.log`, and `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-165450.log`**
- ~~Sodium + Iris + Vulkan selected-mode launches now pass the Round 7 raw/denoised/final focused-region assertion with same-scene screenshots, required log proof, no proof-marker contamination, no temporary direct-light source, no focus-window-only proof source, no native errors, and HUD-safe final composite markers.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-assert-20260619-172730.json`, `run/validation-logs/latest-round7-denoise-composite-baseline-20260619-172440.log`, `run/validation-logs/latest-round7-denoise-composite-rawgi-20260619-172544.log`, `run/validation-logs/latest-round7-denoise-composite-denoisedgi-20260619-172641.log`, and `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-172730.log`**
- ~~Fresh same-scene baseline/raw-GI/denoised-GI/final-composite screenshots were recaptured after the Round 8 scaffolding integration, and the focused-region assertion still passes with required log proof.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-assert-20260619-175013.json`; screenshots are `run/validation-screenshots/round7-denoise-composite-baseline-20260619-174736-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-174925-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-175013-DenoisedGi.png`, and `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-174826-FinalComposite.png`; focused comparison artifact is `run/validation-screenshots/round7-denoise-composite-focused-comparison-20260619-175013.png`. Boundary: the final composite delta is real but visually subtle (`final.focus.changedPixelPercent=5.4036`, `final.focus.meanAbsLuma=1.2025`), while raw/denoised debug tracks are much more obvious (`raw.focus.changedPixelPercent=58.1272`, `denoise.focus.changedPixelPercent=55.8431`).**
- ~~Fresh same-scene baseline/direct/raw-GI/denoised-GI/final/debug screenshots now pass the expanded Round 7 assertion with required log proof, accepted final direct+raw+denoised source identity, no accepted-state metadata-only source, no proof-marker contamination, no temporary direct-light source, no focus-window-only proof source, and no native errors.~~ **DONE/VALIDATED in `run/validation-logs/round7-direct-raw-denoised-final-proof-20260619-184256.json`; screenshots are `run/validation-screenshots/round7-denoise-composite-baseline-20260619-182351-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-direct-20260619-183213-Direct.png`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-183302-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-183351-DenoisedGi.png`, `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-184206-FinalComposite.png`, `run/validation-screenshots/round7-denoise-composite-debug-20260619-184256-Debug.png`, and comparison artifact `run/validation-screenshots/round7-direct-raw-denoised-final-comparison-20260619-184256.png`. Key metrics: `direct.focus.changedPixelPercent=14.6647`, `raw.focus.changedPixelPercent=56.3856`, `denoise.focus.changedPixelPercent=43.1424`, `denoise.roughness.meanAbsNeighborLumaReductionPercent=4.4313`, `final.focus.changedPixelPercent=57.0475`, `final.focus.meanAbsLuma=31.5265`. Boundary: `realDenoiseShaderOutput=false`; this validates CPU denoised output/readback and final blending, not a real shader denoiser.**
- ~~Same-build in-client screenshots now pass the Round 7 assertion after replacing misleading OS/F2 validation capture with a controller-only Minecraft screenshot hook and wiring the dedicated `lucerna:core/round7_denoised_gi_visual` shader into the denoised/final public Mojang draw path.~~ **DONE/VALIDATED in `run/validation-logs/round7-inclient-smoothed-denoise-composite-proof-20260619-231615.json`; screenshots are `run/validation-screenshots/round7-inclient-smoothed-baseline-20260619-231322-Baseline.png`, `run/validation-screenshots/round7-inclient-smoothed-direct-20260619-231354-Direct.png`, `run/validation-screenshots/round7-inclient-smoothed-rawgi-20260619-231427-RawGi.png`, `run/validation-screenshots/round7-inclient-smoothed-denoisedgi-20260619-231503-DenoisedGi.png`, `run/validation-screenshots/round7-inclient-smoothed-final-20260619-231540-FinalComposite.png`, and `run/validation-screenshots/round7-inclient-smoothed-debug-20260619-231615-Debug.png`. Key metrics: `direct.focus.changedPixelPercent=100`, `raw.focus.changedPixelPercent=95.166`, `denoise.focus.changedPixelPercent=100`, `denoise.roughness.meanAbsNeighborLumaReductionPercent=10.0669`, `denoise.roughness.rmsNeighborLumaReductionPercent=26.1867`, `final.focus.changedPixelPercent=99.0017`, `metadataOnlyPreviewPresent=False`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: `realDenoiseShaderOutput=false`; this proves the CPU denoised readback plus shader-side visual shaping path is screenshot-visible and smoother in the measured region, not a real shader-side denoise dispatch.**
- ~~Fresh six-agent Round 7 hardening pass validates source-separated direct/raw-GI/CPU-denoised-GI/final composite evidence with same-scene in-client screenshots after native denoise smoothing and source-identity fixes.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-proof-20260620-031224.json`; screenshots are `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-025943-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030216-Direct.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030448-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030721-DenoisedGi.png`, `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-030952-FinalComposite.png`, and `run/validation-screenshots/round7-denoise-composite-20260620-025943-20260620-031224-Debug.png`. Key metrics: `direct.focus.changedPixelPercent=89.5399`, `raw.focus.changedPixelPercent=65.2127`, `denoise.roughness.meanAbsNeighborLumaReductionPercent=33.4676`, `final.focus.changedPixelPercent=88.5598`, `metadataOnlyPreviewPresent=False`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: `realDenoiseShaderOutput=false`; this is still CPU/readback denoise and final composite validation, not real shader-denoise quality.**

Round 7 Acceptance Criteria
~~Denoised GI is visibly different from raw GI in focused affected-surface screenshots, with source/readback logs proving the CPU denoised path.~~ **DONE/VALIDATED for first-practical CPU output; real shader denoise quality/flicker proof remains open**
~~Composite path produces stable visible selected-source lighting for the current raw-plus-denoised final path.~~ **DONE/VALIDATED for selected-mode Round 7 path**
~~HUD is preserved in the selected-mode baseline/raw/denoised/final screenshots.~~ **DONE/VALIDATED for current test scene**
~~Debug composite modes work for the current Round 7 direct/raw-GI/denoised-GI/final proof set.~~ **DONE/VALIDATED in `run/validation-screenshots/round7-denoise-composite-debug-20260619-184256-Debug.png`**
~~Screenshots prove raw vs denoised vs final image for the selected Round 7 CPU-output path.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-assert-20260619-172730.json`; latest in-client proof is `run/validation-logs/round7-inclient-smoothed-denoise-composite-proof-20260619-231615.json`**
~~Logs prove denoise metadata dispatch scaffold and first-practical CPU denoised output readback run cleanly.~~ **DONE/VALIDATED for scaffold + CPU denoise output only**
~~Composite dispatch logs, raw-vs-denoised screenshots, and final-mode screenshots now exist for selected-mode capture.~~ **DONE/VALIDATED for selected-mode evidence only**
~~Direct-only capture exists in the same proof set as baseline/raw-GI/denoised-GI/final/debug.~~ **DONE/VALIDATED in `run/validation-screenshots/round7-denoise-composite-direct-20260619-183213-Direct.png`**
~~Particle/translucency-specific final-composite stability proof exists with controller screenshots, required log proof, and direct visual inspection.~~ **DONE/VALIDATED in `run/validation-logs/round7-composite-stability-proof-20260619-201227.json`**
Real shader denoise quality, stronger temporal/flicker proof, and physically correct GI remain open.
Raw GI delivery, denoised GI output quality, and final composite integration must be validated independently before any of these acceptance lines are struck through.
~~Required Round 7 selected-mode visual-proof gates now have controller evidence for raw GI source/output markers, denoise dispatch markers, denoised GI output markers, final composite mode markers, no proof-marker contamination, no focus-window-only proof source, and explicit HUD-safe final composite markers in the same validation set as the screenshots.~~ **DONE/VALIDATED for selected-mode CPU-output path**
Round 8: Adaptive Sampling Controller and Debug Heatmaps
Goal

Use existing cache/ray-budget scaffolding to make ray allocation visible and useful.

The renderer should begin spending more work where the image is unstable and less work where cache/history is strong.

~~Agent W: Adaptive Sampling Controller~~ **DONE/VALIDATED for CPU/native telemetry and visible debug heatmap evidence**

Owns:

~~Ray budget classification.~~
~~Per-tile or per-pixel budget map.~~
~~Inputs from variance, disocclusion/history state, cache confidence, emissive proximity, and motion/capture scene state.~~
Material-type weighting remains open because no material-type signal is available in the current budget inputs.
~~Output ray budget used by direct/GI pass telemetry and controller heatmap modes.~~

Deliverable:

~~Adaptive sampling affects dispatch-count telemetry and visible debug heatmap captures.~~ **DONE/VALIDATED in `run/validation-logs/round8-adaptive-heatmap-proof-20260619-193009.json`; boundary: this validates CPU/native work distribution telemetry and debug overlays, not a final GPU adaptive sampler**

Validation by controller:

Join test world.
View stable flat terrain.
Screenshot ray budget heatmap.
Move quickly into a dark/emissive cave or newly visible region.
Screenshot ray budget heatmap again.
Validate:
~~stable terrain receives lower/reuse ray budget markers,~~
~~new/noisy/emissive areas receive higher ray budget markers,~~
~~dispatch counts change in logs.~~
Validate logs contain:
~~budget bucket counts,~~
~~high/medium/low ray region counts,~~
~~cache confidence contribution,~~
~~no invalid budget values.~~
~~Agent X: Variance and History Confidence~~ **DONE/VALIDATED for telemetry/debug heatmap evidence**

Owns:

~~Variance map telemetry/status.~~
~~History confidence map telemetry/status.~~
~~Disocclusion mask telemetry/status.~~
~~Debug views for variance/confidence.~~

Deliverable:

~~Debug views show where history is trusted or rejected.~~ **DONE/VALIDATED in stable-vs-moved history-confidence screenshots and logs**

Validation by controller:

Join world with camera stationary.
Take screenshot of history confidence overlay.
Move camera rapidly.
Take screenshot after disocclusion.
Validate:
~~stationary surfaces gain confidence,~~
~~newly visible surfaces lose confidence,~~
~~variance/history views highlight unstable lighting regions.~~
~~Validate logs contain history accept/reject counts.~~
Round 8 Acceptance Criteria
~~Adaptive ray budgets are visible.~~ **DONE/VALIDATED**
~~Dispatch counts change based on scene conditions.~~ **DONE/VALIDATED through `dispatchCountsChanged=true` and distinct stable/moved/emissive log markers**
~~Stable areas get cheaper.~~ **DONE/VALIDATED through stable low/reuse markers**
~~New/noisy/emissive areas get more sampling.~~ **DONE/VALIDATED through moved/emissive high-budget markers**
~~Debug heatmaps are screenshot-validated.~~ **DONE/VALIDATED**
~~Logs include ray budget and history confidence telemetry.~~ **DONE/VALIDATED**
Round 8 evidence:
- ~~Sodium + Iris + Vulkan launches captured stable ray-budget, moved/noisy ray-budget, emissive ray-budget, stable history-confidence, and moved/disoccluded history-confidence screenshots; the controller assertion passed with required log proof.~~ **DONE/VALIDATED in `run/validation-logs/round8-adaptive-heatmap-proof-20260619-193009.json`; screenshots are `run/validation-screenshots/round8-adaptive-heatmap-stableheatmap-20260619-192639-StableHeatmap.png`, `run/validation-screenshots/round8-adaptive-heatmap-movedheatmap-20260619-192732-MovedHeatmap.png`, `run/validation-screenshots/round8-adaptive-heatmap-emissiveheatmap-20260619-192827-EmissiveHeatmap.png`, `run/validation-screenshots/round8-adaptive-heatmap-historystable-20260619-192915-HistoryStable.png`, `run/validation-screenshots/round8-adaptive-heatmap-historymoved-20260619-193009-HistoryMoved.png`, and comparison artifact `run/validation-screenshots/round8-adaptive-heatmap-comparison-20260619-193009.png`. Key metrics: `movedRayBudget.focus.changedPixelPercent=76.8338`, `emissiveRayBudget.focus.changedPixelPercent=59.4184`, `historyConfidence.focus.changedPixelPercent=73.1608`, `historyConfidence.focus.meanAbsLuma=19.3994`, `stableLowBudgetPresent=True`, `movedHighBudgetPresent=True`, `emissiveHighBudgetPresent=True`, `dispatchCountsChanged=True`, `historyAcceptedPresent=True`, `historyRejectedPresent=True`, `invalidBudgetValuesPresent=False`, `proofMarkerPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: this is Round 8 debug/telemetry heatmap evidence, not final GPU heatmap rendering quality.**
### Round 9: Virtualized Chunk Geometry **PARTIAL**
Goal

Begin the Nanite-like part of the stack, but Minecraft-native.

This is not literal Nanite. This is Virtualized Chunk Geometry:

chunk section clustering,
meshlet or cluster metadata,
GPU-friendly culling,
LOD-ready structure,
distant terrain simplification path.
~~Agent Y: Chunk Cluster/Meshlet Metadata~~ **DONE/VALIDATED**

Owns:

~~Chunk section cluster records.~~
~~Meshlet/cluster metadata layout.~~
~~LOD metadata contracts.~~
~~GPU upload format for cluster visibility data.~~

Deliverable:

~~Chunk geometry can be described as clusters/meshlets without changing visual output yet.~~ **DONE/VALIDATED**

Validation by controller:

~~Run compile/build checks.~~ **DONE/VALIDATED**
~~Launch and join world.~~ **DONE/VALIDATED**
~~Enable chunk cluster debug overlay.~~ **DONE/VALIDATED through `CHUNK_CULLING` Round 9 overlay**
~~Take screenshots in:~~ **DONE/VALIDATED for flat/open, interior/wall-facing, and high-distance/open terrain**
~~flat terrain,~~
~~cave/interior,~~
forest/complex area, **OPEN**
~~high render-distance view.~~
Validate:
clusters align with chunk/section boundaries or expected subdivisions, **PARTIAL through metadata contracts and overlay counts**
~~no missing terrain,~~ **DONE/VALIDATED by controller screenshot inspection**
~~no visual corruption.~~ **DONE/VALIDATED by controller screenshot inspection**
Validate logs contain:
~~cluster count,~~ **DONE/VALIDATED**
~~visible cluster count,~~ **DONE/VALIDATED**
~~upload size,~~ **DONE/VALIDATED**
~~generation counters.~~ **DONE/VALIDATED**
Round 9 Agent Y evidence:
- ~~Sodium + Iris + Vulkan Round 9 overlay launches captured flat/open, interior/wall-facing, and high-distance/open terrain screenshots; controller assertion passed with log proof for `cluster_count`, `visible_cluster_count`, `upload_byte_estimate`, `generation_counter`, and `indirect_draw_count_placeholder`.~~ **DONE/VALIDATED in `run/validation-logs/round9-virtualized-geometry-proof-20260619-195055.json`, `run/validation-logs/latest-round9-virtualized-geometry-flatclusteroverlay-20260619-194840.log`, `run/validation-logs/latest-round9-virtualized-geometry-interiorcullingoverlay-20260619-194950.log`, `run/validation-logs/latest-round9-virtualized-geometry-highdistancecullingoverlay-20260619-195055.log`, `run/validation-screenshots/round9-virtualized-geometry-flatclusteroverlay-20260619-194840-FlatClusterOverlay.png`, `run/validation-screenshots/round9-virtualized-geometry-interiorcullingoverlay-20260619-194950-InteriorCullingOverlay.png`, and `run/validation-screenshots/round9-virtualized-geometry-highdistancecullingoverlay-20260619-195055-HighDistanceCullingOverlay.png`. Key metrics include `maxClusterCount=36418`, `maxVisibleClusterCount=36418`, `maxUploadBytes=4078816`, `maxGenerationCounter=15376`, `maxIndirectDrawCount=36418`, `invalidClusterValuesPresent=False`, `terrainCorruptionPresent=False`, `proofMarkerPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`.**

Agent Z: GPU-Driven Chunk Culling **PARTIAL**

Owns:

Frustum culling. **OPEN for real runtime culling**
~~Occlusion culling placeholder or first implementation.~~ **DONE/VALIDATED as metadata-only placeholder**
~~Indirect draw list generation.~~ **DONE/VALIDATED as metadata-only placeholder count**
~~Debug culling statistics.~~ **DONE/VALIDATED**

Deliverable:

Hidden/offscreen cluster counts are reduced and visible in logs/overlay. **OPEN: current validated telemetry is metadata-only and reports `culled_cluster_count=0`**

Validation by controller:

~~Join world with high render distance.~~ **DONE/VALIDATED as high-distance/open terrain overlay capture**
~~Face open terrain and screenshot culling overlay.~~ **DONE/VALIDATED**
~~Face a wall/cave interior and screenshot culling overlay.~~ **DONE/VALIDATED**
Validate:
visible cluster count changes with camera orientation, **PARTIAL: logs show nonzero visible counts and controller proof marks distinct evidence, but real frustum/occlusion culling is not implemented**
~~terrain does not disappear incorrectly,~~ **DONE/VALIDATED by controller screenshot inspection**
CPU/GPU timing does not regress catastrophically. **OPEN**
~~Validate logs contain culling stats and indirect draw counts.~~ **DONE/VALIDATED for metadata-only placeholder counts**
Round 9 Acceptance Criteria
~~Virtualized chunk metadata exists.~~ **DONE/VALIDATED**
~~Debug overlay shows clusters/culling.~~ **DONE/VALIDATED**
~~No terrain corruption.~~ **DONE/VALIDATED by screenshots**
Visible/culled counts react to camera. **PARTIAL: visible counts are logged and screenshot scenes differ; real culled/offscreen reduction remains open**
~~Logs validate cluster upload and culling behavior.~~ **DONE/VALIDATED for metadata-only cluster upload and culling placeholder behavior**
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
- ~~Sodium + Iris + Vulkan Round 7 scene-shaped emissive/GI surface validation now captures hidden-HUD baseline, hidden-HUD enabled, and debug-overlay screenshots after native scene-spatial/material/cache telemetry, shader-side source shaping, clearer CPU/readback boundary labels, and stricter anti-washout proof diagnostics. The controller assertion passes with log proof for final composite dispatch, source-gated surface contribution, hidden GUI/command-feedback/chat-cleared capture, and rejection markers for proof-marker, focus-window-only, temporary direct-light substitution, native errors, and rectangular/full-screen washout.~~ **DONE/VALIDATED in `run/validation-logs/round7-emissive-gi-scene-shaped-proof-20260619-221725.json`, `run/validation-logs/latest-round7-emissive-gi-scene-shaped-20260619-221725-enabled-20260619-221817.log`, `run/validation-screenshots/round7-emissive-gi-scene-shaped-20260619-221725-baseline-20260619-221725-Baseline.png`, `run/validation-screenshots/round7-emissive-gi-scene-shaped-20260619-221725-enabled-20260619-221817-Enabled.png`, and `run/validation-screenshots/round7-emissive-gi-scene-shaped-20260619-221725-debug-20260619-221910-Debug.png`; key metrics include `classification=round7_emissive_gi_surface_evidence_passed`, `focus.changedPixelPercent=100`, `focus.brighterPixelPercent=100`, `focus.meanSignedLuma=40.7864`, `full.changedPixelPercent=71.0612`, `full.changedBoundingBoxAreaPercent=72.5664`, `full.activeTilePercent=83.3333`, `full.edgeActiveTilePercent=15.2778`, `fixed.changedPixelShareOfFull=8.2343`, `classification.fullScreenOrRectangularWashoutSuspect=False`, `classification.localizedSceneShapedDeltaPresent=True`, `proofMarkerPresent=False`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: this validates a less rectangular scene-shaped CPU/readback preview contribution, not physically correct GI tracing or real shader denoise.**
- ~~Sodium + Iris + Vulkan Round 7 composite-stability validation now captures particle baseline/final, translucent baseline/final, and temporal stable/moved screenshots. The controller assertion passes with required log proof for final composite dispatch, HUD preservation, particle/translucency scene markers, temporal/history markers, and rejection markers for temporary direct-light source, proof-marker, focus-window-only, and native-error evidence; screenshots were directly inspected and show real before/after frame differences with HUD/hotbar/crosshair intact.~~ **DONE/VALIDATED in `run/validation-logs/round7-composite-stability-proof-20260619-201227.json`, `run/validation-logs/latest-round7-composite-stability-particlebaseline-20260619-200530.log`, `run/validation-logs/latest-round7-composite-stability-particlefinalcomposite-20260619-200654.log`, `run/validation-logs/latest-round7-composite-stability-translucentbaseline-20260619-200820.log`, `run/validation-logs/latest-round7-composite-stability-translucentfinalcomposite-20260619-200942.log`, `run/validation-logs/latest-round7-composite-stability-temporalstable-20260619-201104.log`, `run/validation-logs/latest-round7-composite-stability-temporalmoved-20260619-201227.log`, `run/validation-screenshots/round7-composite-stability-particlebaseline-20260619-200530-ParticleBaseline.png`, `run/validation-screenshots/round7-composite-stability-particlefinalcomposite-20260619-200654-ParticleFinalComposite.png`, `run/validation-screenshots/round7-composite-stability-translucentbaseline-20260619-200820-TranslucentBaseline.png`, `run/validation-screenshots/round7-composite-stability-translucentfinalcomposite-20260619-200942-TranslucentFinalComposite.png`, `run/validation-screenshots/round7-composite-stability-temporalstable-20260619-201104-TemporalStable.png`, and `run/validation-screenshots/round7-composite-stability-temporalmoved-20260619-201227-TemporalMoved.png`; key metrics include `particle.focus.changedPixelPercent=48.0306`, `translucency.focus.changedPixelPercent=48.0306`, `temporal.focus.changedPixelPercent=44.9255`, `temporal.focus.meanAbsLuma=6.1699`, `finalCompositePresent=True`, `hudPreservationPresent=True`, `temporaryDirectLightSourcePresent=False`, `proofMarkerPresent=False`, `focusWindowOnlyPresent=False`, and `nativeErrorPresent=False`. Boundary: this validates composite stability proof for the current CPU-denoised/final path, not real shader denoise quality or physically correct GI.**
- ~~Sodium + Iris + Vulkan capped Round 7 emissive/GI surface validation proves the current public Mojang final-composite draw path is genuinely screenshot-visible after fixing the `RenderPass.draw(vertexCount, instanceCount, firstVertex, firstInstance)` argument order. The controller assertion passes with hidden-HUD baseline/enabled screenshots, a debug-overlay screenshot, source-gated surface contribution, and rejection markers for proof-marker, focus-window-only, temporary direct-light substitution, and native errors.~~ **DONE/VALIDATED in `run/validation-logs/round7-emissive-gi-surface-capped-proof-20260619-215916.json`, `run/validation-logs/latest-round7-emissive-gi-surface-capped-enabled-20260619-215916-20260619-220037.log`, `run/validation-screenshots/round7-emissive-gi-surface-capped-baseline-20260619-215916-20260619-215916-Baseline.png`, `run/validation-screenshots/round7-emissive-gi-surface-capped-enabled-20260619-215916-20260619-220037-Enabled.png`, and `run/validation-screenshots/round7-emissive-gi-surface-capped-debug-20260619-215916-20260619-220159-Debug.png`; key metrics include `classification=round7_emissive_gi_surface_evidence_passed`, `focus.changedPixelPercent=100`, `focus.brighterPixelPercent=100`, `focus.meanSignedLuma=46.2289`, `full.changedPixelPercent=67.8525`, `proofMarkerPresent=False`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: the visible effect is a capped CPU/readback preview contribution, not physically correct GI or real shader denoise.**
- ~~Sodium + Iris + Vulkan Round 9 virtualized chunk geometry validation now captures flat/open cluster overlay, interior/wall-facing culling overlay, and high-distance/open terrain culling overlay screenshots. The controller assertion passes with required log proof for Round 9 markers, cluster counts, visible cluster counts, upload bytes, generation counters, indirect draw placeholders, overlay markers, and rejection markers for invalid values/native errors/proof-marker contamination.~~ **DONE/VALIDATED in `run/validation-logs/round9-virtualized-geometry-proof-20260619-195055.json`, `run/validation-logs/latest-round9-virtualized-geometry-flatclusteroverlay-20260619-194840.log`, `run/validation-logs/latest-round9-virtualized-geometry-interiorcullingoverlay-20260619-194950.log`, `run/validation-logs/latest-round9-virtualized-geometry-highdistancecullingoverlay-20260619-195055.log`, `run/validation-screenshots/round9-virtualized-geometry-flatclusteroverlay-20260619-194840-FlatClusterOverlay.png`, `run/validation-screenshots/round9-virtualized-geometry-interiorcullingoverlay-20260619-194950-InteriorCullingOverlay.png`, and `run/validation-screenshots/round9-virtualized-geometry-highdistancecullingoverlay-20260619-195055-HighDistanceCullingOverlay.png`; key metrics include `interiorCulling.focus.changedPixelPercent=95.5838`, `highDistanceCulling.focus.changedPixelPercent=75.8952`, `maxClusterCount=36418`, `maxVisibleClusterCount=36418`, `maxUploadBytes=4078816`, `maxGenerationCounter=15376`, `maxIndirectDrawCount=36418`, `invalidClusterValuesPresent=False`, `terrainCorruptionPresent=False`, `proofMarkerPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: this validates Round 9 metadata/status overlays and native telemetry, not real GPU/frustum/occlusion culling because `culled_cluster_count=0` in the current implementation.**
- ~~Sodium + Iris + Vulkan Round 8 adaptive heatmap validation now captures stable ray-budget, moved/noisy ray-budget, emissive ray-budget, stable history-confidence, and moved/disoccluded history-confidence screenshots; the combined assertion passes with required log proof for bucket counts, scene states, cache-confidence contribution, dispatch-count changes, ray-budget heatmap artifacts, history accepted/rejected counts, and history-confidence heatmap artifacts.~~ **DONE/VALIDATED in `run/validation-logs/round8-adaptive-heatmap-proof-20260619-193009.json`, `run/validation-logs/latest-round8-adaptive-heatmap-stableheatmap-20260619-192639.log`, `run/validation-logs/latest-round8-adaptive-heatmap-movedheatmap-20260619-192732.log`, `run/validation-logs/latest-round8-adaptive-heatmap-emissiveheatmap-20260619-192827.log`, `run/validation-logs/latest-round8-adaptive-heatmap-historystable-20260619-192915.log`, `run/validation-logs/latest-round8-adaptive-heatmap-historymoved-20260619-193009.log`, and comparison artifact `run/validation-screenshots/round8-adaptive-heatmap-comparison-20260619-193009.png`; key metrics include `movedRayBudget.focus.changedPixelPercent=76.8338`, `emissiveRayBudget.focus.changedPixelPercent=59.4184`, `historyConfidence.focus.changedPixelPercent=73.1608`, `stableLowBudgetPresent=True`, `movedHighBudgetPresent=True`, `emissiveHighBudgetPresent=True`, `dispatchCountsChanged=True`, `historyAcceptedPresent=True`, `historyRejectedPresent=True`, `invalidBudgetValuesPresent=False`, `proofMarkerPresent=False`, and `nativeErrorPresent=False`. Boundary: this validates Round 8 debug/telemetry heatmaps and CPU/native work-distribution metadata, not finished GPU adaptive sampling quality.**
- ~~Sodium + Iris + Vulkan Round 7 recapture now includes baseline/off, direct-only, raw GI, CPU-denoised GI, final composite, and debug-overlay screenshots from the same test scene, plus a comparison artifact and expanded assertion proof. The accepted final state blends `native-direct-light-rgba8+native-diffuse-gi-rgba8+cpu-denoised-diffuse-gi-rgba8`, rejects accepted-state metadata-only/focus-window/proof-marker contamination, and preserves the HUD-safe final-composite path.~~ **DONE/VALIDATED in `run/validation-logs/round7-direct-raw-denoised-final-proof-20260619-184256.json`, `run/validation-logs/latest-round7-denoise-composite-finalcomposite-20260619-184206.log`, `run/validation-screenshots/round7-denoise-composite-baseline-20260619-182351-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-direct-20260619-183213-Direct.png`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-183302-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-183351-DenoisedGi.png`, `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-184206-FinalComposite.png`, `run/validation-screenshots/round7-denoise-composite-debug-20260619-184256-Debug.png`, and `run/validation-screenshots/round7-direct-raw-denoised-final-comparison-20260619-184256.png`; key proof metrics include `direct.focus.changedPixelPercent=14.6647`, `raw.focus.changedPixelPercent=56.3856`, `denoise.focus.changedPixelPercent=43.1424`, `denoise.roughness.meanAbsNeighborLumaReductionPercent=4.4313`, `final.focus.changedPixelPercent=57.0475`, `metadataOnlyPreviewPresent=False`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `nativeErrorPresent=False`. Boundary: `realDenoiseShaderOutput=false`, so real shader denoise quality remains open.**
- ~~Sodium + Iris + Vulkan direct-surface proof now captures disabled baseline, enabled, and debug screenshots from the glowstone wall scene, rejects focus-window/proof-marker/temporary-source contamination, and validates a real native direct/emissive final-composite surface delta.~~ **DONE/VALIDATED in `run/validation-logs/round5-direct-surface-proof-20260619-181014.json`, `run/validation-logs/latest-round5-direct-surface-enabled-20260619-180925.log`, `run/validation-screenshots/round5-direct-surface-baseline-20260619-180836-Baseline.png`, `run/validation-screenshots/round5-direct-surface-enabled-20260619-180925-Enabled.png`, `run/validation-screenshots/round5-direct-surface-debug-20260619-181014-Debug.png`, and `run/validation-screenshots/round5-direct-surface-focused-comparison-20260619-181014.png`; key proof metrics include `max.emissiveCandidates=128`, `max.shadowCandidates=4096`, `max.surfaceSamples=325742`, `max.directOutputEnergy=275232`, `focus.changedPixelPercent=57.411`, `focus.brighterPixelPercent=55.8431`, `focusWindowOnlyPresent=False`, `temporaryDirectLightSourcePresent=False`, and `proofMarkerPresent=False`.**
- ~~Sodium + Iris + Vulkan selected-mode launches were recaptured after Round 8 scaffolding edits and still pass the Round 7 focused raw/denoised/final assertion with required log proof and no proof-marker/focus-window/native-error contamination.~~ **DONE/VALIDATED in `run/validation-logs/round7-denoise-composite-assert-20260619-175013.json`; full screenshots are in `run/validation-screenshots/round7-denoise-composite-baseline-20260619-174736-Baseline.png`, `run/validation-screenshots/round7-denoise-composite-rawgi-20260619-174925-RawGi.png`, `run/validation-screenshots/round7-denoise-composite-denoisedgi-20260619-175013-DenoisedGi.png`, and `run/validation-screenshots/round7-denoise-composite-finalcomposite-20260619-174826-FinalComposite.png`; focused comparison is `run/validation-screenshots/round7-denoise-composite-focused-comparison-20260619-175013.png`. Note: the final composite remains subtle in full-screen view, so this evidence proves a real selected-path delta, not the finished obvious emissive-lighting milestone.**
- ~~Sodium + Iris + Vulkan enabled launch validates Round 7 first-practical CPU denoised diffuse-GI output after controller build/native/signing: the game joins `New World`, accepts native lighting dispatches, logs `marker=first_practical_cpu_denoised_diffuse_gi_rgba8_generated`, `denoised_cpu_output_generated=true`, `denoisedPayloadReady=true`, `denoisedPayloadEvidence=denoised_diffuse_gi_rgba8_first_practical_cpu_output`, and `realDenoiseShaderOutput=false`, then shuts down cleanly.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-cpu-denoise-output-sodium-iris-vulkan-20260619-161055.log`**
- ~~Sodium + Iris + Vulkan enabled launch validates Round 7 evidence telemetry after controller build/native/signing: the game joins `New World`, accepts native lighting dispatches, and logs raw GI metadata, denoised-output intent metadata, composite placeholder metadata, edge/history counters, and explicit `realDenoiseShaderOutput=false`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-evidence-telemetry-sodium-iris-vulkan-20260619-154001.log`**
- ~~Sodium + Iris + Vulkan enabled launch validates the Round 7 denoise/composite foundation after controller build/native/signing: the game joins `New World`, accepts native lighting dispatches with enabled stages, and logs the native denoise scaffold marker `signal_separated_denoise_metadata_scaffold_no_render_output`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round7-denoise-composite-foundation-sodium-iris-vulkan-20260619-152554.log`**
- ~~Sodium + Iris + Vulkan enabled launch now passes no-marker native Round 6 diffuse-GI surface-composite validation with baseline/enabled/debug screenshots, native diffuse-GI output-source markers, rejection of temporary direct-light source markers, no proof-marker contamination, and a visible affected-surface delta.~~ **DONE/VALIDATED in `run/validation-logs/round6-no-marker-fullarea-lower-right-proof-20260619-150705.json`, `run/validation-logs/latest-round6-no-marker-fullarea-enabled-20260619-150705.log`, `run/validation-screenshots/round6-no-marker-fullarea-enabled-20260619-150705-Enabled.png`, `run/validation-screenshots/round6-no-marker-fullarea-baseline-20260619-150620-Baseline.png`, and `run/validation-screenshots/round6-no-marker-fullarea-debug-20260619-150753-Debug.png`**
- ~~Sodium + Iris + Vulkan enabled launch now captures a screenshot-visible `R6 GI proof` marker only when Lucerna is active and the native Round 6 diffuse-GI CPU payload is preview-ready; the disabled baseline screenshot does not show the marker, and the debug screenshot shows the marker with Round 6/native telemetry present.~~ **DONE/VALIDATED as proof-marker evidence in `run/validation-logs/round6-gi-proof-overlay-final-proof-20260619-133223.json`, `run/validation-logs/latest-round6-gi-proof-overlay-final-enabled-20260619-133223.log`, `run/validation-screenshots/round6-gi-proof-overlay-final-enabled-20260619-133223-Enabled.png`, `run/validation-screenshots/round6-gi-proof-overlay-final-baseline-20260619-133309-Baseline.png`, and `run/validation-screenshots/round6-gi-proof-overlay-final-debug-20260619-133348-Debug.png`**
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
  - ~~Real native direct/emissive candidate payload is blended into the final composite with focus-window/proof-marker/temporary-source evidence rejected and screenshot-visible surface brightening.~~ **DONE/VALIDATED in `run/validation-logs/round5-direct-surface-proof-20260619-181014.json`**
  - ~~Visible direct/emissive focus-window final-composite proof is screenshot-validated with positive focused wall-region delta and readable HUD.~~ **DONE/VALIDATED**
  - ~~Native surface-sample masked direct-light preview generation is compile/build/launch validated before HUD composition.~~ **DONE/VALIDATED**
  - ~~Compact extracted opaque surface samples and surface-origin direct-light shadow candidate planning are compile/build/launch validated with nonzero `surfaceSampleSections` and `surfaceSamples` telemetry.~~ **DONE/VALIDATED**
  - ~~Java-side Round 6 GI source-summary, native diffuse-GI upload metadata, dirty-region listener, sparse radiance-cache scaffolding, Round 6 overlay presentation, native Round 6 dispatch telemetry, and controller Round 6 dispatch/cache logs compile and launch under Sodium + Iris + Vulkan.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-gi-cache-partial-sodium-iris-vulkan-20260619-094433.log`**
  - ~~Low-res GI dispatch metadata becomes enabled with `diffuse_gi={{enabled=true,size=427x240,samples=2,rays=409920,cache_reads=14150,...}}`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-gi-cache-partial-sodium-iris-vulkan-20260619-094433.log`**
  - ~~Sparse cache dirty-region telemetry, nonzero records, and nonzero cache writes are validated with `max.cacheRecords=128`, `max.cacheWrites=256`, and `max.cacheReads=7564`.~~ **DONE/VALIDATED in `run/validation-logs/latest-round6-cache-stage-sodium-iris-vulkan-20260619-095852.log` and `run/validation-logs/round6-cache-stage-proof-20260619-095852.json`**
  - ~~Round 6 GI-gated final-composite preview is screenshot-validated with focused region delta `changedPixelPercent=57.5159`, `brighterPixelPercent=58.1384`, and `meanSignedLuma=8.8609`, plus `round6-diffuse-gi-focus-window-additive` draw submission.~~ **DONE/VALIDATED as preview-only in `run/validation-logs/round6-visible-gi-proof-20260619-103414.json` and `run/validation-logs/latest-round6-visible-gi-enabled-20260619-103414.log`**
  - ~~Round 6 no-marker native diffuse-GI surface-composite proof is screenshot/log validated without the temporary direct-light RGBA source or proof-marker overlays.~~ **DONE/VALIDATED in `run/validation-logs/round6-no-marker-fullarea-lower-right-proof-20260619-150705.json`**
  - ~~Native diffuse-GI output-source replacement is controller-validated with signed staged native runtime loading and stricter source proof.~~ **DONE/VALIDATED in `run/validation-logs/round6-native-gi-proof-20260619-121147.json`**
  - ~~Round 6 native diffuse-GI payload readiness now reaches the screenshot path through a HUD-safe proof marker and passes focused pixel-delta proof with native GI source gating.~~ **DONE/VALIDATED as proof-marker evidence in `run/validation-logs/round6-gi-proof-overlay-final-proof-20260619-133223.json`**
  - ~~Public Mojang final-composite draw ordering is fixed and the capped Round 7 emissive/GI surface proof shows a visible source-gated wall-surface delta with proof-marker, focus-window-only, temporary-source, and native-error evidence rejected.~~ **DONE/VALIDATED in `run/validation-logs/round7-emissive-gi-surface-capped-proof-20260619-215916.json`**
  - ~~Native scene-spatial/material/cache telemetry, shader source shaping, boundary overlay text, and anti-washout diagnostics are implemented and controller-validated with the scene-shaped Round 7 surface proof.~~ **DONE/VALIDATED in `run/validation-logs/round7-emissive-gi-scene-shaped-proof-20260619-221725.json`**
  - ~~Controller-only in-client screenshot capture and dedicated Round 7 denoised visual shader shaping are implemented and controller-validated with same-build baseline/direct/raw-GI/denoised-GI/final/debug screenshots and a passing raw-vs-denoised/final assertion.~~ **DONE/VALIDATED in `run/validation-logs/round7-inclient-smoothed-denoise-composite-proof-20260619-231615.json`**
  - Physically correct low-res GI, real shader denoise, geometry/material-aware final surface projection, and production-quality final composite remain open.

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

Round 5 has a controller-validated direct/emissive surface proof, Round 6 has a controller-validated no-marker native diffuse-GI surface-composite proof over the actual affected wall/ground surface region, and Round 7 now has controller-validated same-scene baseline/direct/raw-GI/CPU-denoised-GI/final/debug evidence plus particle/translucency/temporal final-composite stability proof. The capped Round 7 proof confirmed that the public Mojang draw path affects the captured world-color image after the `RenderPass.draw(...)` argument-order fix, the scene-shaped proof confirms the current path is less rectangular and passes anti-washout diagnostics, and the latest in-client proof replaces misleading OS/F2 screenshots with real Minecraft screenshot artifacts while validating smoother denoised visual shaping. Do not present this as physically correct low-res GI tracing or real shader denoise quality yet: the next stage is improving physical GI behavior, real shader-side denoise dispatch/output, stronger temporal/flicker behavior, and the native GI/tracing model.

Immediate Round 6 documentation/validation boundary:

Java-side GI source-summary, native upload metadata, dirty-region listener, sparse radiance-cache scaffolding, Round 6 overlay text, native Round 6 telemetry, controller GI/cache metadata logs, log-only cache records/writes, native diffuse-GI source replacement, build-green deterministic native output generation, scene-tied native telemetry, clearer Round 5-versus-Round 6 debug overlay wording, native broad-projection/per-pixel composite attempts, and a native-GI-readiness screenshot proof marker are now implemented. The Direct Lighting debug mode now renders Round 6 GI/cache evidence in a separate compact HUD panel so controller screenshots are not dependent on the primary Direct Lighting overlay line cap. Native Round 6 output shaping now uses emissive/cache/section/shadow-candidate inputs instead of the old proof-wall weighting, and the Round 6 public composite has a dedicated `round6_native_diffuse_gi_surface` shader plus `round6-native-diffuse-gi-surface-additive` mode string. The Round 6 pipeline routing bug is fixed so direct-light final composite uses `lucerna:core/direct_light_final_composite_focus` and Round 6 uses `lucerna:core/round6_native_diffuse_gi_surface`; the latest hook path runs immediately after `LevelRenderer.render(...)` so the composite is captured before hand/HUD composition. Controller build/native/sign validation is green, and the `Round6NativeDiffuseGiNoMarker` profile rejects temporary-source, focus-window, and proof-marker evidence. Earlier no-marker attempts failed because their measured focus regions did not cover the affected surface, but the latest controller proof passes over the actual affected wall/ground surface region: `run/validation-logs/round6-no-marker-fullarea-lower-right-proof-20260619-150705.json` reports `focus.changedPixelPercent=37.9981`, `focus.brighterPixelPercent=37.9981`, `focus.meanSignedLuma=18.5186`, `nativeGiOutputSourcePresent=True`, `temporaryDirectLightSourcePresent=False`, `roundSixNoMarkerSurfaceDrawPresent=True`, `proofMarkerContaminationPresent=False`, `max.giRays=409920`, `max.giCacheReads=12090`, and `nativeErrorPresent=False`. Round 7 now also proves direct/raw-GI/CPU-denoised-GI/final/debug source separation in `run/validation-logs/round7-direct-raw-denoised-final-proof-20260619-184256.json`, particle/translucency/temporal final-composite stability in `run/validation-logs/round7-composite-stability-proof-20260619-201227.json`, a capped source-gated visible surface delta after the public draw-order fix in `run/validation-logs/round7-emissive-gi-surface-capped-proof-20260619-215916.json`, and a softer scene-shaped/anti-washout proof in `run/validation-logs/round7-emissive-gi-scene-shaped-proof-20260619-221725.json`. This closes the current separated evidence, composite-stability proof, "does the draw path visibly change the screenshot?" question, and the first anti-rectangle proof hardening pass, but it does not close physically correct GI tracing, real shader denoise quality, stronger temporal/flicker behavior, or finished final composite quality.

The next immediate milestone is:

Move beyond the now-validated direct/emissive surface proof, separated Round 7 direct/raw/denoised/final proof, particle/translucency/temporal composite-stability proof, capped screenshot-visible public draw proof, scene-shaped anti-washout proof, and in-client denoised visual-shaping proof by improving the remaining first-lighting stack: low-res diffuse GI should become more physically tied to scene geometry/materials, denoised GI should move from CPU-output/readback plus single-input visual shaping to real shader-side quality, and temporal stability should be validated under longer motion/flicker scenarios. Keep rejecting metadata-backed preview, focus-window-only brightness, proof markers, full-screen/rectangular washout, wrong-window/blank-surface screenshots, and temporary direct-light payload substitution.

Next-stage handoff checklist:

- Real shader denoise evidence: require logs that explicitly report shader-side denoise dispatch/output, not CPU denoised readback or metadata scaffold status; require same-scene raw-GI versus shader-denoised screenshots, focused affected-surface metrics, edge-preservation checks, and rejection of broad blur/dimming as the only improvement.
- Physical GI evidence: require scene/material/geometry-linked input logs and screenshots showing plausible bounce behavior from emissive and sun/moon sources on actual surfaces; reject proof-marker, focus-window-only, temporary direct-light substitution, metadata-only preview, and full-screen/rectangular washout evidence.
- Temporal/flicker evidence: require longer same-scene camera-motion captures or fixed-interval screenshot series with stable/moved comparisons, history acceptance/rejection logs, roughness/flicker metrics, and explicit checks for ghosting, shimmer, particle/translucency corruption, and HUD/hand preservation.
- Controller screenshot/log requirements: every claim needs baseline/off, enabled, debug-overlay, and focused comparison artifacts from Sodium + Iris + Vulkan, plus launch/render logs proving the requested path executed, the expected source identity was used, native errors were absent, and unsupported evidence sources were rejected.
- Acceptance boundary for the next worker: do not call the next stage complete until the proof shows real shader denoise and more physically grounded GI behavior together; intermediate scaffolding, telemetry, CPU/readback output, or visual deltas may be useful progress but should remain labeled as open.
