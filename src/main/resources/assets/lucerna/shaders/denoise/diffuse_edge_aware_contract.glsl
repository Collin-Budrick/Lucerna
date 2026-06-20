#version 450

// Contract resource for lucerna.denoise.diffuse real shader-output evidence.
//
// This file is intentionally side-effect free until the render scheduler binds a
// real denoise dispatch path. It documents the shader-side inputs and outputs
// controller validation should assert before flipping realDenoiseShaderOutput to
// true in Java/runtime telemetry.
//
// Expected readable inputs:
// - lucerna.lighting.direct
// - lucerna.lighting.diffuseGi
// - lucerna.lighting.cacheConfidence
// - lucerna.lighting.variance
// - lucerna.lighting.rayBudget
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.materialId
// - lucerna.gbuffer.motionHistory
// - PreviousDepth
// - PreviousNormalRoughness
// - PreviousLighting
// - MotionHistory
// - VarianceConfidence
//
// Expected writable outputs once implemented:
// - lucerna.denoise.diffuse, full-resolution linear diffuse radiance
// - lucerna.denoise.rejectionMask, full-resolution temporal/edge rejection mask
// - lucerna.denoise.historyRejectionMask, optional debug mask explaining why
//   history was rejected at depth/normal/material/motion boundaries
// - lucerna.denoise.rawVsDenoisedQuality, optional debug target used only for
//   controller proof that raw GI and shader-denoised GI differ for the right
//   reasons
//
// Evidence boundary:
// - realDenoiseShaderOutput=false while this resource remains contract-only.
// - CPU/readback denoised GI payloads must be reported separately.
// - A future implementation must write the outputs from shader execution, then
//   controller validation can compare raw-GI, shader-denoised, final, and debug
//   screenshots/logs without relying on preview draw substitution.
// - Quality proof must include edge-preservation, history rejection,
//   variance/confidence, and raw-vs-denoised comparisons. A full-screen blur or
//   rectangular preview wash is explicitly insufficient.
//
// Placeholder policy: no storage writes, image writes, atomics, barriers,
// discard, color output, or depth output.
void main() {
}
