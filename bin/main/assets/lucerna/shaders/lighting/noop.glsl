#version 450

// Placeholder entry point shared by lucerna.lighting.direct and lucerna.lighting.gi.
// Phase 5 order: direct lighting writes lucerna.lighting.direct before diffuse GI consumes it.
// Round 5 direct proof: future direct output writes only lucerna.lighting.direct.
// Composite later resolves lucerna.lighting.direct into lucerna.composite.worldColor.
// Baseline/disabled mode skips this contribution and preserves the validated no-op or flat-composite result.
// Controller validation requires baseline, enabled direct-light, and debug-overlay screenshots plus dispatch/output/resolve logs.
// Future direct payload: sun/moon candidates, bounded emissive candidates, voxel shadow visibility, and linear direct radiance.
// Future GI/cache payload: low-res diffuseGi, cacheConfidence, variance, rayBudget, RadianceHistory, and VarianceConfidence.
// Readiness labels: readiness.lucerna.lighting.direct and readiness.lucerna.lighting.gi.
// This pass must not consume Iris shader-pack color, depth, shadow, or lighting outputs.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
