#version 450

// Placeholder entry point shared by lucerna.lighting.direct and lucerna.lighting.gi.
// Phase 5 order: direct lighting writes lucerna.lighting.direct before diffuse GI consumes it.
// Future GI writes lucerna.lighting.diffuseGi, cacheConfidence, variance, rayBudget, RadianceHistory, and VarianceConfidence.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
