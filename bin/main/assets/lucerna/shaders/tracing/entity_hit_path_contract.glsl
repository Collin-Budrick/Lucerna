#version 450

// Round 10 Vulkan RT/entity hit-path resource contract.
//
// Boundary:
// This file is a placeholder contract only. It does not declare shader stages,
// hit groups, payload structs, storage writes, image writes, atomics, barriers,
// color outputs, depth outputs, or a claim that hardware ray tracing executed.
// Controller validation must reject any RT execution claim that is not backed by
// native device/extension/build/dispatch telemetry.
//
// Capability gate:
// The RT path requires an active Sodium Vulkan backend plus all required device
// features/extensions represented by VulkanRtCapabilityStatus:
// - VK_KHR_acceleration_structure
// - VK_KHR_ray_tracing_pipeline
// - VK_KHR_deferred_host_operations
// - VK_KHR_buffer_device_address
// - VK_EXT_descriptor_indexing
//
// Fallback:
// If any required extension or backend precondition is unavailable, Lucerna must
// continue through the existing non-RT voxel/GI/composite path. Entity transform
// summaries may still be recorded as Java metadata, but BLAS/TLAS device builds
// and hit shaders must be reported as unavailable or unproven.
//
// Future descriptor intent, not assigned binding numbers:
// set=rtScene:
//   EntityInstanceTable        read-only instance id, material id, flags, BLAS index
//   EntityTransformHistory     read-only current/previous transform metadata
//   EntityMaterialTable        read-only opaque/translucent/emissive material metadata
//   EntityGeometryTable        read-only compact complex-geometry metadata
//   TopLevelAccelerationStruct read-only TLAS handle once native build is proven
// set=rtOutputs:
//   EntityHitMetadata          write-only hit id, barycentrics, primitive id, material id
//   RtVisibilityMask          write-only visibility/confidence mask for later lighting
//
// Future shader stages:
// - raygen: issue bounded entity/complex-geometry visibility or lighting rays.
// - closest-hit: resolve material id, normal, emissive, opacity, and primitive id.
// - any-hit: reject alpha-tested or non-opaque geometry when material metadata says so.
// - miss: preserve fallback/sky visibility semantics without mutating world color.
//
// Current Round 10 deliverable:
// Java records provide capability/status, BLAS/TLAS metadata, and entity transform
// update DTOs. Native files, shader compilation, pipeline creation, dispatch,
// and Minecraft launch validation are out of scope for this worker.
void main() {
}
