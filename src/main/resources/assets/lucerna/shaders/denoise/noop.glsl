#version 450

// Placeholder entry point for lucerna.denoise.diffuse.
// Future work consumes direct lighting, diffuse GI, cache confidence, variance, ray budget, and history.
// It writes lucerna.denoise.diffuse plus lucerna.denoise.rejectionMask after clearing them.
// Future payload boundary: denoise owns GI upsampling, variance clamps, temporal rejection, and history repair inputs.
// Readiness label: readiness.lucerna.denoise.diffuse.
// This placeholder declares no storage writes, image writes, atomics, barriers, color output, or depth output.
void main() {
}
