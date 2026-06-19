# Lucerna

Lucerna is a Fabric 26.x client mod scaffold for a Sodium Vulkan renderer with a C++ native core.

The initial implementation is intentionally compatibility-gated:

- Lucerna activates only when Sodium is installed and the active Minecraft backend reports Vulkan.
- Iris may be installed, but Lucerna disables Iris shader-pack rendering while Lucerna owns the renderer path.
- Native rendering is behind a JNI bridge and currently runs a guarded no-op lifecycle on the borrowed Mojang/Sodium Vulkan context.

## Current Status

Current validated baseline:

- Fabric Loom `1.17.11`, Minecraft `26.2`, Fabric API `0.152.1+26.2`.
- Sodium + Iris + Vulkan launch reaches native init, material refresh, borrowed Mojang Vulkan context `READY`, world join, world leave, and clean shutdown.
- Forced OpenGL launch disables Lucerna cleanly without native load or material refresh.
- No-Sodium Vulkan launch disables Lucerna cleanly without native load or material refresh.
- Existing implementation includes backend/Iris gating, JNI lifecycle, native resource/pass telemetry, dirty-region/material upload batches, guarded frame hooks, G-buffer contracts, section DTOs, and debug/status scaffolding.

The next implementation stage is Phase 3 into early Phase 4: turn the validated guardrail layer into data-producing renderer foundations. That means wiring real per-frame constants from the Fabric world-render context, extracting chunk-section voxel/material snapshots, extending upload contracts for section data, tracking native staging telemetry for voxel/G-buffer resources, and validating shader layout metadata before attempting visual G-buffer or GI output.

## Configuration

Lucerna persists client options in `config/lucerna.json` with a schema marker, renderer enable flag, quality preset, debug overlay selection, and Iris notice preference. Invalid or missing fields fall back to defaults and are rewritten by the client on the next load.

## Shader Resources

Shader placeholders live under `src/main/resources/assets/lucerna/shaders`. The layout draft is documented in `shaders/readme.md` and `shaders/layout.json`; these files reserve pass names and descriptor-set purposes for future Vulkan integration.

Sub-agent implementation rule: sub-agents must not run tests, Gradle checks, `runClient`, builds used as verification, render smoke tests, or Minecraft launches. The controller owns all verification.
