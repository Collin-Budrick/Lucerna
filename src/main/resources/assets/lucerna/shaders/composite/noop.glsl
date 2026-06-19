#version 450

// Placeholder entry point for lucerna.composite.final.
// Future composite consumes lucerna.denoise.diffuse and optional lucerna.debug.overlay.
// It writes only lucerna.composite.worldColor, the borrowed Minecraft/Sodium world color target before HUD and late translucency.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
