#version 450

// Placeholder entry point for lucerna.debug.overlay.
// Future overlays read DebugLabelTable plus direct lighting, diffuse GI, denoise, cache confidence, variance, ray budget, timing, and native queue metadata.
// This pass is scheduled before lucerna.composite.final so composite can consume lucerna.debug.overlay when enabled.
// Future labels include readiness.lucerna.* stage readiness and milestone.round4.first_lighting.* boundaries.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
