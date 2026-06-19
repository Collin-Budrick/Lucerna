#version 450

// Placeholder entry point for Lucerna lighting passes.
// Future direct lighting writes lucerna.lighting.direct after a clear.
// Future GI writes diffuseGi, cacheConfidence, variance, and adaptive rayBudget metadata.
// This placeholder intentionally performs no storage writes, image writes, atomics, barriers, or color/depth output.
void main() {
}
