#version 450

// Placeholder entry point for lucerna.composite.final.
// Round 5 direct-only resolve consumes lucerna.gbuffer.albedoOpacity and lucerna.lighting.direct.
// Future full composite consumes lucerna.denoise.diffuse, diffuse GI, and optional lucerna.debug.overlay.
// It writes only lucerna.composite.worldColor, the borrowed Minecraft/Sodium world color target before HUD and late translucency.
// Baseline/disabled mode skips the direct-light contribution and preserves validated no-op or flat-composite behavior.
// Enabled direct mode may resolve direct radiance before vanilla HUD and late translucency.
// Future payload boundary: combine albedo, direct light, diffuse GI, denoised diffuse, and debug overlay without taking swapchain ownership.
// This pass must not consume Iris shader-pack color, depth, shadow, or lighting outputs.
// Readiness label: readiness.lucerna.composite.final.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
