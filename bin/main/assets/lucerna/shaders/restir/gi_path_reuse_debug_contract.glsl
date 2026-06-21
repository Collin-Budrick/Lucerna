#version 450

// Round 11 future GI/PT-style path reuse debug output resource contract.
//
// Boundary:
// This is contract-only. It does not implement ReSTIR GI, ReSTIR PT,
// path-space reuse, neural reuse, shader denoise, or physically correct
// indirect lighting. Any current Round 11 GI reuse evidence must be labeled as
// CPU/native preview metadata unless a future shader/native path writes the
// outputs described here and the controller validates them in-world.
//
// Future readable inputs:
// - lucerna.lighting.diffuseGi
// - lucerna.lighting.cacheConfidence
// - lucerna.lighting.variance
// - lucerna.lighting.rayBudget
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.albedoOpacity
// - lucerna.gbuffer.materialId
// - GiPathReservoirPreviousFrame
// - SparseVoxelRadianceCache
// - LucernaFrameConstants
//
// Future writable outputs:
// - GiPathReservoirCurrentFrame: path id, bounce summary, weight sum,
//   confidence, cache generation, reuse age, and invalidation reason.
// - GiPathReuseDebug: path reuse count, invalidated reservoir count,
//   confidence min/mean/max, and boundary flags for overlay/status text.
// - Optional GiPathPreviewRadiance: debug-only low-resolution radiance preview
//   that must remain separate from production final composite proof.
//
// Validation boundary:
// A later real path must prove scene-coupled indirect response, history
// invalidation, and stable moved-camera behavior. Reservoir metadata alone does
// not prove cheaper GI/PT, path reuse quality, or physically correct lighting.
//
// Placeholder policy: no storage writes, image writes, atomics, barriers,
// discard, color output, or depth output.
void main() {
}
