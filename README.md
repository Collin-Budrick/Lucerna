# Lucerna

Lucerna is a Fabric 26.x client mod scaffold for a Sodium Vulkan renderer with a C++ native core.

The initial implementation is intentionally compatibility-gated:

- Lucerna activates only when Sodium is installed and the active Minecraft backend reports Vulkan.
- Iris may be installed, but Lucerna disables Iris shader-pack rendering while Lucerna owns the renderer path.
- Native rendering is behind a JNI bridge and starts as a no-op lifecycle until Vulkan frame integration lands.

## Configuration

Lucerna persists client options in `config/lucerna.json` with a schema marker, renderer enable flag, quality preset, debug overlay selection, and Iris notice preference. Invalid or missing fields fall back to defaults and are rewritten by the client on the next load.

## Shader Resources

Shader placeholders live under `src/main/resources/assets/lucerna/shaders`. The layout draft is documented in `shaders/readme.md` and `shaders/layout.json`; these files reserve pass names and descriptor-set purposes for future Vulkan integration.

Sub-agent implementation rule: sub-agents must not run tests, Gradle checks, `runClient`, builds used as verification, render smoke tests, or Minecraft launches. The controller owns all verification.
