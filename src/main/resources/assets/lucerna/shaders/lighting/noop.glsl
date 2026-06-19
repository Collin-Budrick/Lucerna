#version 450

// Placeholder entry point shared by lucerna.lighting.direct and lucerna.lighting.gi.
// Phase 5 order: direct lighting writes lucerna.lighting.direct before diffuse GI consumes it.
// Future direct payload: sun/moon candidates, bounded emissive candidates, voxel shadow visibility, and linear direct radiance.
// Future GI/cache payload: low-res diffuseGi, cacheConfidence, variance, rayBudget, RadianceHistory, and VarianceConfidence.
// Readiness labels: readiness.lucerna.lighting.direct and readiness.lucerna.lighting.gi.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
