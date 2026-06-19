# Lucerna Parallel Implementation Plan

## Summary

- Lucerna is a Fabric 26.x + Sodium Vulkan renderer mod with a C++ native core.
- Iris is compatibility-visible only: Lucerna appears in Iris-facing settings/status paths, but Iris shader-pack rendering is disabled while Lucerna owns rendering.
- Implementation should be coordinated by a **main controller** with multiple sub-agents working on disjoint modules.
- **Sub-agents must not run tests, Gradle checks, `runClient`, builds used as verification, render smoke tests, or Minecraft launches.** They only edit their assigned scope and report changed files. The main controller runs all tests and sends feedback.

## Controller/Sub-Agent Rules

- The main controller owns repo scaffolding, task sequencing, final integration, conflict resolution, all test execution, all Minecraft launches, and all feedback loops.
- Sub-agents get explicit write ownership. They must not edit outside their assigned paths unless they ask first.
- Sub-agents must assume others are editing the repo in parallel. They must not revert unrelated changes.
- Sub-agents may read/search files and make code changes in their scope. They must not run verification commands.
- Verification includes Gradle checks, test tasks, compiler-only checks, native builds, shader compilation, `runClient`, Minecraft launches, and render smoke tests. Reading files and searching source are allowed.
- Each sub-agent final report must include: changed files, public interfaces touched, assumptions made, and any untested risk.
- Controller reviews each patch, runs the relevant checks, then either integrates it or sends focused feedback back to that sub-agent.

## Parallel Work Breakdown

### Round 0: Controller-Owned Bootstrap

- Create the initial Fabric/Loom project, package layout, CMake/native layout, shared naming, config directory, and build metadata.
- Establish stable module boundaries:
  - Java mod core: `net.lucerna`
  - Compatibility: `net.lucerna.compat`
  - World extraction: `net.lucerna.world`
  - Native bridge: `net.lucerna.nativebridge`
  - Native C++: `native/lucerna_renderer`
  - Shaders/resources: `src/main/resources/assets/lucerna`
- Add placeholder contracts/interfaces before spawning workers so write scopes do not collide.

### Round 1: Parallel Foundation Agents

- **Agent A: Sodium/Vulkan Compatibility**
  - Owns backend detection, Sodium presence checks, Vulkan activation gates, and Lucerna enable/disable state.
  - Does not implement rendering.
  - Deliverable: `LucernaBackendDetector`, backend enum/status model, logs and user-facing disable reasons.

- **Agent B: Iris Settings Compatibility**
  - Owns Iris detection and settings/status integration.
  - Implements “Iris installed, Lucerna active, Iris shader packs disabled” behavior.
  - Deliverable: Iris-facing placeholder/status hooks and Lucerna settings entry point.
  - Must not attempt Iris shader-pack rendering.

- **Agent C: Native Bridge Skeleton**
  - Owns JNI declarations, Java loader, native library lifecycle, and C++ exported ABI stubs.
  - Deliverable: no-op native lifecycle with `init`, `shutdown`, `beginFrame`, `endFrame`, `onResize`, `uploadWorldDeltas`, `renderLighting`.
  - Must not touch Sodium/Iris compatibility logic.

- **Agent D: Config And Debug UI**
  - Owns Lucerna config model, config persistence, keybind/debug overlay toggles, and status presentation.
  - Deliverable: config schema with renderer enable flag, quality preset, debug overlay selection, and backend status display.

### Round 2: Parallel Renderer Data Agents

- **Agent E: World Event Feed**
  - Owns chunk load/unload, section rebuild, block update, fluid update, emissive update, resource-pack reload, dimension/weather/time tracking.
  - Deliverable: `LucernaWorldFeed` and dirty-region queue.
  - Must not implement GPU upload.

- **Agent F: Material Extraction**
  - Owns block/material table generation from Minecraft block states, models, textures, emissive flags, opacity, water/glass/leaves flags.
  - Deliverable: stable Java-side material records and native upload DTOs.

- **Agent G: Native Resource Manager**
  - Owns C++ Vulkan resource lifetime abstractions, frame-resource rings, buffer/image wrappers, and safe shutdown.
  - Deliverable: native resource manager that can accept borrowed Vulkan context handles later.
  - Must not wire into Minecraft frame rendering.

- **Agent H: Shader/Resource Layout**
  - Owns Lucerna shader asset organization, shader compile conventions, descriptor layout docs, and placeholder fullscreen/no-op shaders.
  - Deliverable: resource structure for G-buffer, lighting, denoise, composite, and debug passes.

### Round 3: Integration-Critical Agents

- **Agent I: Vulkan Frame Hook**
  - Owns attaching Lucerna passes to Mojang/Sodium Vulkan frame/render-pass flow.
  - Deliverable: no-op Vulkan pass that runs only when backend state is `SODIUM_VULKAN`.
  - High-risk scope; controller tests immediately after integration.

- **Agent J: Chunk Data Upload**
  - Owns converting world/material deltas into native upload packets and staging them into GPU buffers.
  - Deliverable: upload queue, generation counters, dirty-region invalidation handoff.

- **Agent K: G-buffer/Voxel First Pass**
  - Owns first Lucerna-controlled world buffers: depth-compatible data, normals, albedo/material id, emissive, motion/history metadata.
  - Deliverable: minimal G-buffer and voxel occupancy traversal scaffolding.

- **Agent L: Debug Telemetry**
  - Owns GPU/CPU timing labels, backend status overlay, dirty chunk overlay, material id overlay, and native error reporting.
  - Deliverable: debug views and structured logs for controller-led testing.

### Round 4: Lighting Feature Agents

- **Agent M: Direct Lighting**
  - Owns sun/moon lighting, emissive block list sampling, voxel shadow ray scaffolding.
- **Agent N: First GI**
  - Owns low-res single-bounce diffuse GI, temporal accumulation inputs, and cache confidence output.
- **Agent O: Denoise/Composite**
  - Owns edge-aware diffuse denoise, history rejection inputs, and final composite into the world color target.
- **Agent P: Cache/Adaptive Sampling**
  - Owns surface/radiance cache records, variance map, ray-budget classification, and dirty-region invalidation policy.

## Controller Test Responsibilities

- After each sub-agent batch, the controller runs:
  - Static compile/build checks.
  - Fabric client launch checks.
  - Minecraft world join/leave checks.
  - Sodium-only, Sodium+Iris, no-Sodium, OpenGL-forced, and Vulkan-unavailable launch matrix.
  - Resize, resource reload, dimension switch, chunk streaming, and shutdown leak checks.
- Controller feedback format to sub-agents:
  - Failing command or scenario.
  - Relevant logs/stack traces.
  - Exact expected behavior.
  - Files the sub-agent may edit for the fix.
- No sub-agent is allowed to "verify" fixes by running tests or build-like checks. They patch, explain, and wait for controller feedback.

## Acceptance Criteria

- Lucerna activates only on Sodium Vulkan and disables cleanly elsewhere.
- Iris can be installed without crashing; Lucerna is visible/status-aware, but Iris shader packs do not render while Lucerna is active.
- Native lifecycle survives startup, world join, resize, resource reload, world leave, and shutdown.
- World/material updates reach native upload queues with generation tracking.
- First visual milestone: no-op or flat composite pass renders without corrupting vanilla HUD/translucency.
- First lighting milestone: basic emissive/direct light + low-res diffuse GI + denoise/composite path works under controller-run testing.

## Assumptions

- The workspace starts empty, so the controller creates the initial scaffold before parallel work.
- Fabric 26.x, Sodium 26.2 Vulkan path, and C++ native core remain the target.
- Sub-agent work is parallelized only after stable interfaces exist to avoid overlapping edits.
