# Lucerna Parallel Implementation Plan

## Progress Audit

Current controller estimate: **about 76% complete against this file**.

This percentage is conservative:
- Rounds 0-3 are mostly implemented and controller-validated.
- Round 4 has Java/native/shader scaffolding, dirty-region GI inputs, direct shadow candidate planning, and native dispatch validation, but the first real lighting milestone is not complete yet.
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
  - Status: backend/native/upload/frame telemetry and Phase 5 lighting dispatch telemetry are implemented and compile/launch validated. Full GPU timing labels and visual overlay validation are still open.

### Round 4: Lighting Feature Agents

- **Agent M: Direct Lighting** **PARTIAL**
  - Owns sun/moon lighting, emissive block list sampling, voxel shadow ray scaffolding.
  - Status: Java planning contracts, emissive sampling, bounded direct shadow candidate generation, and native dispatch metadata exist and have been validated. Real native lighting execution is still open.

- **Agent N: First GI** **PARTIAL**
  - Owns low-res single-bounce diffuse GI, temporal accumulation inputs, and cache confidence output.
  - Status: Java planning contracts, ray budget model, dirty-region cache inputs, cache records, and dispatch metadata exist and have been validated. Real GI tracing/accumulation output is still open.

- **Agent O: Denoise/Composite** **PARTIAL**
  - Owns edge-aware diffuse denoise, history rejection inputs, and final composite into the world color target.
  - Status: Java planning contracts and native dispatch metadata exist and have been validated. Real denoise shader/composite output is still open.

- **Agent P: Cache/Adaptive Sampling** **PARTIAL**
  - Owns surface/radiance cache records, variance map, ray-budget classification, and dirty-region invalidation policy.
  - Status: cache/ray-budget scaffolding and dirty-region cache inputs exist and have been validated through launch. Visible adaptive debug output is still open.

## Controller Test Responsibilities

- After each sub-agent batch, the controller runs:
  - ~~Static compile/build checks.~~ **DONE/VALIDATED for current worktree**
  - ~~Fabric client launch checks.~~ **DONE/VALIDATED for current worktree**
  - ~~Minecraft world join/leave checks.~~ **DONE/VALIDATED for current worktree**
  - Sodium-only, Sodium+Iris, no-Sodium, OpenGL-forced, and Vulkan-unavailable launch matrix. **PARTIAL**
    - ~~Sodium-only Vulkan active launch.~~ **DONE/VALIDATED for current worktree**
    - ~~Sodium + Iris Vulkan active launch.~~ **DONE/VALIDATED for current worktree**
    - ~~No-Sodium launch disables Lucerna cleanly.~~ **DONE/VALIDATED for current worktree**
    - ~~OpenGL-forced launch disables Lucerna cleanly.~~ **DONE/VALIDATED for current worktree**
    - Vulkan-unavailable launch. **OPEN; not simulated on this Vulkan-capable machine**
  - Resize, resource reload, dimension switch, chunk streaming, and shutdown leak checks. **PARTIAL**
- ~~Controller feedback format to sub-agents:~~ **DONE/ONGOING**
  - ~~Failing command or scenario.~~
  - ~~Relevant logs/stack traces.~~
  - ~~Exact expected behavior.~~
  - ~~Files the sub-agent may edit for the fix.~~
- ~~No sub-agent is allowed to "verify" fixes by running tests or build-like checks. They patch, explain, and wait for controller feedback.~~ **DONE/ONGOING**

Latest strong validation evidence:
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
