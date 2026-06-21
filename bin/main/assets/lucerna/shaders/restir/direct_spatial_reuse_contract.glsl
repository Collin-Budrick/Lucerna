#version 450

// Round 11 ReSTIR DI spatial reuse resource contract.
//
// Boundary:
// This file documents the future spatial-neighbor reuse pass. It is
// contract-only and does not prove GPU spatial reuse, cheaper many-light
// rendering, flicker reduction, or physically correct ReSTIR DI. Current status
// may expose CPU/native preview counters such as spatial reuse count and
// confidence; those counters must remain labeled as preview/status evidence
// until a shader dispatch writes the reservoirs below.
//
// Future readable inputs:
// - DirectReservoirAfterTemporal
// - lucerna.gbuffer.depth
// - lucerna.gbuffer.normalRoughness
// - lucerna.gbuffer.materialId
// - lucerna.lighting.cacheConfidence
// - lucerna.lighting.variance
// - BlueNoiseTexture
// - LucernaFrameConstants
//
// Future writable outputs:
// - DirectReservoirAfterSpatial: merged reservoir payload with neighbor count,
//   accepted-neighbor mask, confidence, and rejection reason.
// - RestirSpatialReuseDebug: spatial reuse count, rejected neighbor count,
//   radius/tile metadata, and per-pixel confidence.
//
// Validation boundary:
// A later implementation must show that spatial reuse is geometry aware. Depth,
// normal, material, variance, and confidence disagreement must reject neighbor
// reservoirs. A uniform full-screen neighbor blend or static debug number is
// insufficient for a real ReSTIR DI spatial-reuse claim.
//
// Placeholder policy: no storage writes, image writes, atomics, barriers,
// discard, color output, or depth output.
void main() {
}
