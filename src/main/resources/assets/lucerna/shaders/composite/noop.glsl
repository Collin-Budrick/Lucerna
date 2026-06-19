#version 450

// Placeholder entry point for lucerna.composite.final.
// Future composite consumes lucerna.denoise.diffuse and optional lucerna.debug.overlay.
// It writes only lucerna.composite.worldColor, the borrowed Minecraft/Sodium world color target before HUD and late translucency.
// Future payload boundary: combine albedo, direct light, diffuse GI, denoised diffuse, and debug overlay without taking swapchain ownership.
// Readiness label: readiness.lucerna.composite.final.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
