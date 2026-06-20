#version 450

// Round 11 first bounded ReSTIR DI preview resolve resource contract.
//
// Boundary:
// This is a shader/resource contract only. It does not implement GPU ReSTIR DI,
// reservoir sampling, many-light reduction, storage writes, image writes,
// atomics, barriers, color output, depth output, or native dispatch. Current
// Round 11 evidence may come from CPU/native preview telemetry and debug
// overlays; that preview must be reported as CPU/native preview, not final GPU
// ReSTIR execution.
//
// Future readable inputs, once the real shader path exists:
// - lucerna.lighting.direct
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.albedoOpacity
// - lucerna.gbuffer.materialId
// - DirectReservoirCandidateTable
// - DirectReservoirPreviousFrame
// - BlueNoiseTexture
// - LucernaFrameConstants
//
// Future writable outputs, not assigned concrete bindings here:
// - DirectReservoirCurrentFrame: selected light id, weight sum, confidence,
//   candidate count, selected count, age, and validation flags.
// - lucerna.restir.directPreview: optional bounded debug radiance used only for
//   controller proof that the resolve consumes reservoir state.
// - RestirDirectResolveDebug: candidate reduction ratio, output energy,
//   checksum, and boundary flags for overlay/status text.
//
// Evidence requirements before any real-GPU claim:
// - real_restir_di_execution=true from native/runtime telemetry.
// - direct reservoir count is nonzero in a scene with direct candidates.
// - selected count is bounded below candidate count when reuse/reduction is
//   active.
// - output energy/checksum changes when direct candidates change.
// - controller screenshots/logs distinguish CPU/native preview from shader
//   output.
//
// Placeholder policy: no storage writes, image writes, atomics, barriers,
// discard, color output, or depth output.
void main() {
}
