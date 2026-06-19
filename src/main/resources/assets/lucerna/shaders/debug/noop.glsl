#version 450

// Placeholder entry point for lucerna.debug.overlay.
// Future overlays read DebugLabelTable plus direct lighting, diffuse GI, denoise, cache confidence, variance, ray budget, timing, and native queue metadata.
// Round 5 direct overlay inputs: lighting enabled state, emissive candidate count, shadow candidate count, dispatch frame, output write status, and resolve status.
// Controller screenshots must show baseline/disabled or no-op, enabled direct-light output, readable HUD, and this overlay state.
// This pass is scheduled before lucerna.composite.final so composite can consume lucerna.debug.overlay when enabled.
// Future labels include readiness.lucerna.* stage readiness and milestone.round4.first_lighting.* boundaries.
// Overlay data is Lucerna-owned telemetry and must not require Iris shader-pack outputs.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
