#version 450

// Contract resource for shader-side denoise history rejection and
// variance/confidence quality proof.
//
// This is intentionally side-effect free. It exists so Java/runtime telemetry
// and controller logs can refer to a concrete shader resource while the real
// dispatch path is still pending.
//
// Required future inputs:
// - lucerna.denoise.diffuse shader-written output from the current frame
// - lucerna.lighting.diffuseGi raw low-resolution GI
// - lucerna.lighting.variance
// - VarianceConfidence
// - PreviousDepth
// - PreviousNormalRoughness
// - PreviousLighting
// - MotionHistory
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.materialId
//
// Required future proof outputs:
// - lucerna.denoise.historyRejectionMask
// - lucerna.denoise.rawVsDenoisedQuality
//
// Input/output texture expectations:
// - Raw GI, cache confidence, current-frame variance, and ray-budget textures are
//   half-resolution unless layout.json says otherwise; denoise must upsample to
//   full-resolution lucerna.denoise.diffuse.
// - Depth, normal/roughness, material id, motion/history, and previous-frame
//   resources are full-resolution or explicitly reprojected before rejection.
// - Rejection/debug outputs must not be synthesized from the public Mojang
//   core/round7_denoised_gi_visual.fsh preview path.
// - A candidate output image can be logged for readiness only; it is not the
//   shader-written lucerna.denoise.diffuse contract until shader dispatch writes
//   the declared attachment and the controller validates the result.
//
// Quality boundary:
// - realDenoiseShaderOutput must stay false until shader execution writes a
//   denoised target and controller validation proves that denoise quality is
//   not a CPU/readback preview, proof marker, focus-window artifact, or
//   rectangular full-screen wash.
// - Controller proof should compare raw GI, shader-denoised GI, final
//   composite, rejection mask, variance/confidence, and debug overlay evidence.
// - Boundary/status markers should preserve the distinction between
//   shaderDenoiseOutputImageCandidateReady,
//   shaderDenoiseOutputImageCandidateNonGpu,
//   shaderDenoiseOutputImageReady, and realShaderDenoiseOutputReady.
void main() {
}
