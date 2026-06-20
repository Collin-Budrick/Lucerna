#version 450

// Round 11 ReSTIR DI temporal reuse resource contract.
//
// Boundary:
// This placeholder describes the temporal reservoir handoff for a future GPU
// shader path. It is not a temporal reuse implementation and must not be used
// as proof of stable lighting, reduced flicker, or final ReSTIR DI quality.
// CPU/native preview telemetry can advertise temporal reuse counters only when
// the overlay/status text also keeps the preview or contract-only label.
//
// Future readable inputs:
// - DirectReservoirCurrentCandidates
// - DirectReservoirPreviousFrame
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.materialId
// - lucerna.gbuffer.motionHistory
// - PreviousDepth
// - PreviousNormalRoughness
// - PreviousMaterialId
// - LucernaFrameConstants
//
// Future writable outputs:
// - DirectReservoirAfterTemporal: selected direct-light sample, M/weight sum,
//   previous-frame age, history validity, confidence, and invalidation reason.
// - RestirTemporalReuseDebug: temporal reuse count, invalidated reservoir count,
//   motion/depth/normal/material rejection counts, and confidence range.
//
// Validation boundary:
// A real implementation must reject stale history on camera movement,
// disocclusion, material changes, dirty-region invalidation, and low-confidence
// cache regions. Controller proof must compare stable and moved camera
// sequences before claiming temporal stability or flicker improvement.
//
// Placeholder policy: no storage writes, image writes, atomics, barriers,
// discard, color output, or depth output.
void main() {
}
