#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace lucerna {

struct FrameInfo {
    std::uint64_t frame_index = 0;
    float tick_delta = 0.0F;
};

struct DirtyRegionUpload {
    std::int32_t type_id = 0;
    std::string dimension;
    std::int32_t section_x = 0;
    std::int32_t section_y = 0;
    std::int32_t section_z = 0;
    bool section_scoped = false;
    std::uint64_t generation = 0;
};

struct MaterialUpload {
    std::int32_t material_id = 0;
    std::uint64_t generation = 0;
    std::string block_id;
    std::int32_t face_id = 0;
    std::int32_t albedo_texture_index = 0;
    float roughness = 0.0F;
    float metalness = 0.0F;
    float emissive_red = 0.0F;
    float emissive_green = 0.0F;
    float emissive_blue = 0.0F;
    float emissive_strength = 0.0F;
    std::int32_t flags = 0;
};

struct SectionDirtyRegionHandoff {
    std::int32_t type_id = 0;
    std::string type_name;
    std::string dimension;
    std::int32_t section_x = 0;
    std::int32_t section_y = 0;
    std::int32_t section_z = 0;
    bool section_scoped = false;
    std::uint64_t generation = 0;
};

struct SectionEmissiveEntryUpload {
    std::int32_t voxel_index = 0;
    std::int32_t material_id = 0;
    std::int32_t block_light_level = 0;
    std::uint64_t generation = 0;
};

struct SectionSnapshotUpload {
    SectionDirtyRegionHandoff dirty_region;
    std::string dimension;
    std::int32_t section_x = 0;
    std::int32_t section_y = 0;
    std::int32_t section_z = 0;
    std::uint64_t section_generation = 0;
    std::uint64_t material_generation = 0;
    std::uint64_t occupancy_generation = 0;
    std::uint64_t emissive_generation = 0;
    std::int32_t occupied_voxel_count = 0;
    std::int32_t opaque_voxel_count = 0;
    std::int32_t translucent_voxel_count = 0;
    std::int32_t fluid_voxel_count = 0;
    std::int32_t emissive_voxel_count = 0;
    std::int32_t occupancy_bit_order_id = 0;
    std::string occupancy_bit_order_name;
    std::int32_t occupancy_mask_word_offset = 0;
    std::int32_t occupancy_mask_word_count = 0;
    std::int32_t occupancy_mask_bit_count = 0;
    std::uint64_t occupancy_mask_generation = 0;
    std::int32_t material_palette_offset = 0;
    std::uint64_t material_palette_generation = 0;
    std::vector<std::int32_t> material_palette_ids;
    std::vector<SectionEmissiveEntryUpload> emissive_entries;

    [[nodiscard]] std::uint64_t combined_generation() const;
    [[nodiscard]] bool has_section_payload() const;
};

struct UploadPacket {
    std::uint64_t generation = 0;
    std::int32_t dirty_region_count = 0;
    std::int32_t material_update_count = 0;
    std::uint64_t first_world_generation = 0;
    std::uint64_t last_world_generation = 0;
    std::uint64_t material_generation = 0;
    std::vector<DirtyRegionUpload> dirty_regions;
    std::vector<MaterialUpload> material_updates;
};

struct SectionUploadPacket {
    std::uint64_t generation = 0;
    std::int32_t section_snapshot_count = 0;
    std::uint64_t first_section_snapshot_generation = 0;
    std::uint64_t last_section_snapshot_generation = 0;
    std::uint64_t section_generation = 0;
    std::uint64_t section_material_generation = 0;
    std::uint64_t section_occupancy_generation = 0;
    std::uint64_t section_emissive_generation = 0;
    std::uint64_t section_dirty_region_generation = 0;
    std::vector<SectionSnapshotUpload> snapshots;
};

struct GBufferAttachmentUpload {
    std::string name;
    std::int32_t format_tag = 0;
    std::int32_t width = 0;
    std::int32_t height = 0;
    std::int32_t samples = 1;
    bool enabled = false;
};

struct GBufferStagingUpload {
    std::string pass_id;
    std::int32_t numeric_pass_id = 0;
    std::int32_t width = 0;
    std::int32_t height = 0;
    std::int32_t attachment_count = 0;
    std::vector<GBufferAttachmentUpload> attachments;
};

struct GBufferStagingPacket {
    std::uint64_t generation = 0;
    std::int32_t gbuffer_count = 0;
    std::uint64_t first_gbuffer_generation = 0;
    std::uint64_t last_gbuffer_generation = 0;
    std::vector<GBufferStagingUpload> gbuffers;
};

enum class NativeLightingDispatchStage : std::uint8_t {
    DirectLighting = 0,
    DiffuseGi = 1,
    Denoise = 2,
    Composite = 3,
    Cache = 4
};

inline constexpr std::size_t kNativeLightingDispatchStageCount = 5;
inline constexpr std::size_t kNativeLightingDispatchPayloadCategoryCount = 4;

struct LightingDispatchStageUpload {
    NativeLightingDispatchStage stage = NativeLightingDispatchStage::DirectLighting;
    std::string stage_name;
    bool enabled = false;
    std::uint64_t generation = 0;
    std::int32_t width = 0;
    std::int32_t height = 0;
    std::int32_t dispatch_x = 0;
    std::int32_t dispatch_y = 0;
    std::int32_t dispatch_z = 0;
    std::int32_t workgroup_size_x = 0;
    std::int32_t workgroup_size_y = 0;
    std::int32_t workgroup_size_z = 0;
    std::int32_t input_count = 0;
    std::int32_t output_count = 0;
    std::int32_t sample_count = 0;
    std::int32_t ray_count = 0;
    std::int32_t cache_read_count = 0;
    std::int32_t cache_write_count = 0;
    std::uint64_t estimated_bytes = 0;
    std::uint32_t flags = 0;
};

struct LightingDispatchPacket {
    std::uint64_t generation = 0;
    std::int32_t dispatch_count = 0;
    std::uint64_t first_dispatch_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t world_generation = 0;
    std::uint64_t material_generation = 0;
    std::uint64_t section_generation = 0;
    std::uint64_t gbuffer_generation = 0;
    std::vector<LightingDispatchStageUpload> dispatches;
};

struct DirectLightingPayloadPacket {
    std::uint64_t frame_index = 0;
    std::uint64_t generation = 0;
    std::uint64_t first_generation = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t celestial_generation = 0;
    std::uint64_t emissive_generation = 0;
    std::uint64_t shadow_generation = 0;
    std::uint64_t shadow_candidate_generation = 0;
    std::uint64_t section_snapshot_generation = 0;
    std::string dimension_id;
    std::uint32_t flags = 0;
    std::int32_t celestial_light_count = 0;
    float celestial_light_energy = 0.0F;
    std::int32_t selected_emissive_count = 0;
    float selected_emissive_energy = 0.0F;
    std::int32_t shadow_candidate_count = 0;
    std::int32_t budgeted_shadow_candidate_count = 0;
    std::int32_t section_snapshot_count = 0;
    std::vector<std::int32_t> ray_budget;
    std::vector<std::int32_t> celestial_light_sources;
    std::vector<std::int32_t> celestial_light_flags;
    std::vector<float> celestial_light_data;
    std::vector<std::string> emissive_light_dimensions;
    std::vector<std::int32_t> emissive_light_metadata;
    std::vector<float> emissive_light_data;
    std::vector<std::uint64_t> emissive_light_generations;
    std::vector<std::int32_t> shadow_candidate_metadata;
    std::vector<float> shadow_candidate_rays;
    std::vector<std::uint64_t> shadow_candidate_generations;
    std::vector<std::string> section_snapshot_dimensions;
    std::vector<std::int32_t> section_snapshot_metadata;
    std::vector<std::uint64_t> section_snapshot_generations;
};

struct BorrowedVulkanContext;
class ResourceManager;

enum class NativeRenderPass : std::uint8_t {
    FutureGBuffer = 0,
    NoopLighting = 1,
    FlatComposite = 2
};

inline constexpr std::size_t kNativeRenderPassCount = 3;

enum class NativeRenderPassState : std::uint8_t {
    Inactive,
    WaitingForFrame,
    WaitingForContext,
    Ready,
    Submitted,
    SkippedInvalidOrder,
    SkippedNoContext,
    NotWired
};

struct NativeRenderPassCounters {
    NativeRenderPass pass = NativeRenderPass::FutureGBuffer;
    NativeRenderPassState state = NativeRenderPassState::Inactive;
    std::uint64_t attempts = 0;
    std::uint64_t submitted = 0;
    std::uint64_t skipped = 0;
    std::uint64_t invalid_order = 0;
    std::uint64_t missing_context = 0;
    std::uint64_t not_wired = 0;
    std::uint64_t placeholder_resources = 0;
    std::uint64_t last_frame_index = 0;
    bool expected_this_frame = false;
    bool submitted_this_frame = false;
};

struct NativeSectionStagingTelemetry {
    std::uint64_t packets = 0;
    std::uint64_t advertised_dirty_regions = 0;
    std::uint64_t payload_dirty_regions = 0;
    std::uint64_t section_scoped_regions = 0;
    std::uint64_t global_regions = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_first_generation = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::uint64_t placeholder_buffers = 0;
    std::uint64_t snapshot_packets = 0;
    std::uint64_t advertised_snapshots = 0;
    std::uint64_t payload_snapshots = 0;
    std::uint64_t payload_sections = 0;
    std::uint64_t last_snapshot_packet_generation = 0;
    std::uint64_t last_snapshot_first_generation = 0;
    std::uint64_t last_snapshot_generation = 0;
    std::uint64_t last_section_generation = 0;
    std::uint64_t last_material_generation = 0;
    std::uint64_t last_occupancy_generation = 0;
    std::uint64_t last_emissive_generation = 0;
    std::uint64_t last_dirty_region_generation = 0;
    std::uint64_t last_occupied_voxels = 0;
    std::uint64_t total_occupied_voxels = 0;
    std::uint64_t last_snapshot_payload_bytes = 0;
    std::uint64_t total_snapshot_payload_bytes = 0;
};

struct NativeVoxelStagingTelemetry {
    std::uint64_t packets = 0;
    std::uint64_t dirty_sections = 0;
    std::uint64_t last_dirty_sections = 0;
    std::uint64_t last_estimated_voxels = 0;
    std::uint64_t last_occupancy_words = 0;
    std::uint64_t last_material_indices = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::uint64_t placeholder_buffers = 0;
    std::uint64_t snapshot_packets = 0;
    std::uint64_t payload_sections = 0;
    std::uint64_t last_payload_sections = 0;
    std::uint64_t occupancy_words = 0;
    std::uint64_t last_occupancy_payload_words = 0;
    std::uint64_t material_palette_entries = 0;
    std::uint64_t last_material_palette_entries = 0;
    std::uint64_t emissive_entries = 0;
    std::uint64_t last_emissive_entries = 0;
    std::uint64_t last_snapshot_estimated_bytes = 0;
    std::uint64_t total_snapshot_estimated_bytes = 0;
};

struct NativeVirtualChunkGeometryTelemetry {
    std::uint64_t packets = 0;
    std::uint64_t payload_sections = 0;
    std::uint64_t empty_section_skip_count = 0;
    std::uint64_t cluster_count = 0;
    std::uint64_t visible_cluster_count = 0;
    std::uint64_t culled_cluster_count = 0;
    std::uint64_t offscreen_cluster_count = 0;
    std::uint64_t upload_byte_estimate = 0;
    std::uint64_t total_upload_byte_estimate = 0;
    std::uint64_t indirect_draw_count_placeholder = 0;
    std::uint64_t indirect_draw_count = 0;
    std::uint64_t generation_counter = 0;
    std::uint64_t first_generation = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t occupied_voxel_count = 0;
    std::uint64_t opaque_voxel_count = 0;
    std::uint64_t translucent_voxel_count = 0;
    std::uint64_t emissive_voxel_count = 0;
    std::uint64_t metadata_buffer_intents = 0;
    std::uint64_t culling_evaluations = 0;
    std::uint64_t traversal_metadata_packets = 0;
    std::uint64_t traversal_ray_count = 0;
    std::uint64_t traversal_hit_count = 0;
    std::uint64_t traversal_miss_count = 0;
    std::uint64_t traversal_step_count = 0;
    double traversal_average_steps = 0.0;
    std::uint64_t traversal_skipped_sections = 0;
    std::uint64_t traversal_material_hit_count = 0;
    std::uint64_t traversal_occupancy_mask_sections = 0;
    std::uint64_t traversal_occupancy_mask_words = 0;
    std::uint64_t traversal_occupancy_mask_bits = 0;
    std::uint64_t traversal_palette_entry_count = 0;
    std::uint64_t traversal_fallback_sections = 0;
    std::uint64_t traversal_generation_counter = 0;
    std::string cluster_marker;
    std::string culling_marker;
    std::string culling_mode;
    std::string culling_reason;
    std::string traversal_marker;
    std::string traversal_backend;
    std::string traversal_boundary;
    std::string traversal_material_hit_source;
};

struct NativeGBufferStagingTelemetry {
    std::uint64_t frames_planned = 0;
    std::uint64_t staging_packets = 0;
    std::uint64_t advertised_gbuffers = 0;
    std::uint64_t payload_gbuffers = 0;
    std::uint64_t allocation_intents = 0;
    std::uint64_t attachment_intents = 0;
    std::uint64_t enabled_attachments = 0;
    std::uint64_t disabled_attachments = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_first_generation = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t last_payload_gbuffers = 0;
    std::uint64_t last_enabled_attachments = 0;
    std::uint64_t last_disabled_attachments = 0;
    std::uint64_t last_attachment_count = 0;
    std::uint64_t last_attachment_samples = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::int32_t last_width = 0;
    std::int32_t last_height = 0;
    std::int32_t last_numeric_pass_id = 0;
    bool planned_this_frame = false;
    bool last_payload_recorded_this_frame = false;
    std::string last_pass_id;
};

struct NativeLightingDispatchStageTelemetry {
    NativeLightingDispatchStage stage = NativeLightingDispatchStage::DirectLighting;
    std::uint64_t packets = 0;
    std::uint64_t enabled_count = 0;
    std::uint64_t disabled_count = 0;
    std::uint64_t allocation_intents = 0;
    std::uint64_t placeholder_buffers = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::int32_t last_width = 0;
    std::int32_t last_height = 0;
    std::int32_t last_dispatch_x = 0;
    std::int32_t last_dispatch_y = 0;
    std::int32_t last_dispatch_z = 0;
    std::int32_t last_workgroup_size_x = 0;
    std::int32_t last_workgroup_size_y = 0;
    std::int32_t last_workgroup_size_z = 0;
    std::int32_t last_input_count = 0;
    std::int32_t last_output_count = 0;
    std::int32_t last_sample_count = 0;
    std::int32_t last_ray_count = 0;
    std::int32_t last_cache_read_count = 0;
    std::int32_t last_cache_write_count = 0;
    std::uint32_t last_flags = 0;
    bool last_placeholder = false;
    bool last_validated = false;
    bool last_temporal_history = false;
    bool last_reuse_only = false;
    bool last_debug_overlay = false;
    bool enabled_this_packet = false;
    bool recorded_this_frame = false;
    bool ready_for_native_execution_this_packet = false;
    std::string last_readiness_reason;
};

struct NativeLightingDispatchPayloadCategoryTelemetry {
    std::uint64_t last_stage_count = 0;
    std::uint64_t last_enabled_stage_count = 0;
    std::uint64_t last_input_count = 0;
    std::uint64_t last_output_count = 0;
    std::uint64_t last_sample_count = 0;
    std::uint64_t last_ray_count = 0;
    std::uint64_t last_cache_read_count = 0;
    std::uint64_t last_cache_write_count = 0;
    std::uint64_t last_enabled_sample_count = 0;
    std::uint64_t last_enabled_ray_count = 0;
    std::uint64_t last_enabled_cache_read_count = 0;
    std::uint64_t last_enabled_cache_write_count = 0;
    std::uint64_t total_sample_count = 0;
    std::uint64_t total_ray_count = 0;
    std::uint64_t total_cache_read_count = 0;
    std::uint64_t total_cache_write_count = 0;
    std::uint64_t last_placeholder_stage_count = 0;
    std::uint64_t last_validated_stage_count = 0;
    std::uint64_t last_temporal_history_stage_count = 0;
    std::uint64_t last_reuse_only_stage_count = 0;
    std::uint64_t last_debug_overlay_stage_count = 0;
};

struct NativeAdaptiveBudgetTelemetry {
    std::uint64_t packets = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t last_cell_count = 0;
    std::uint64_t last_rays_per_cell = 0;
    std::uint64_t last_requested_rays = 0;
    std::uint64_t last_capped_rays = 0;
    std::uint64_t last_budget_delta_rays = 0;
    std::uint64_t last_dispatch_workgroups = 0;
    std::uint64_t last_previous_dispatch_workgroups = 0;
    std::uint64_t last_dispatch_delta_workgroups = 0;
    std::uint64_t last_scene_state_checksum = 0;
    std::uint64_t last_previous_scene_state_checksum = 0;
    std::uint64_t last_cache_confidence_contribution = 0;
    std::uint64_t last_variance_contribution = 0;
    std::uint64_t last_history_accepted_count = 0;
    std::uint64_t last_history_rejected_count = 0;
    std::uint64_t total_history_accepted_count = 0;
    std::uint64_t total_history_rejected_count = 0;
    std::uint64_t last_heatmap_artifact_pixels = 0;
    std::uint64_t total_budget_changes = 0;
    std::uint64_t last_reuse_bucket_count = 0;
    std::uint64_t last_low_bucket_count = 0;
    std::uint64_t last_medium_bucket_count = 0;
    std::uint64_t last_high_bucket_count = 0;
    std::uint64_t total_reuse_bucket_count = 0;
    std::uint64_t total_low_bucket_count = 0;
    std::uint64_t total_medium_bucket_count = 0;
    std::uint64_t total_high_bucket_count = 0;
    std::uint64_t invalid_budget_rejections = 0;
    std::uint64_t last_invalid_budget_generation = 0;
    bool last_ingested = false;
    bool last_valid = false;
    bool last_enabled = false;
    bool last_budget_changed = false;
    bool last_budget_capped = false;
    bool last_dispatch_count_changed = false;
    bool last_scene_state_changed = false;
    bool last_reuse_only = false;
    bool last_temporal_history = false;
    bool last_variance_marker_available = false;
    bool last_history_confidence_available = false;
    std::string last_bucket;
    std::string last_previous_bucket;
    std::string last_budget_marker;
    std::string last_variance_marker;
    std::string last_history_confidence_marker;
    std::string last_heatmap_artifact_role;
    std::string last_heatmap_boundary_marker;
    std::string last_invalid_budget_reason;
};

struct NativeDirectLightingExecutionTelemetry {
    std::uint64_t payload_packets = 0;
    std::uint64_t attempts = 0;
    std::uint64_t submitted = 0;
    std::uint64_t skipped = 0;
    std::uint64_t output_writes = 0;
    std::uint64_t resolves = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t last_payload_frame_index = 0;
    std::uint64_t last_payload_generation = 0;
    std::uint64_t last_payload_first_generation = 0;
    std::uint64_t last_payload_generation_end = 0;
    std::uint64_t last_payload_celestial_generation = 0;
    std::uint64_t last_payload_emissive_generation = 0;
    std::uint64_t last_payload_shadow_generation = 0;
    std::uint64_t last_payload_shadow_candidate_generation = 0;
    std::uint64_t last_payload_section_snapshot_generation = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t last_celestial_light_count = 0;
    std::uint64_t last_emissive_light_count = 0;
    std::uint64_t last_shadow_candidate_count = 0;
    std::uint64_t last_budgeted_shadow_candidate_count = 0;
    std::uint64_t last_section_snapshot_count = 0;
    std::uint64_t last_candidate_count = 0;
    std::uint64_t last_sample_count = 0;
    std::uint64_t last_ray_count = 0;
    std::uint64_t last_output_count = 0;
    std::uint64_t last_output_width = 0;
    std::uint64_t last_output_height = 0;
    std::uint64_t last_output_pixel_count = 0;
    std::uint64_t last_output_checksum = 0;
    std::uint64_t last_surface_payload_sample_count = 0;
    std::uint64_t last_surface_payload_pixel_count = 0;
    std::uint64_t last_material_surface_pixel_count = 0;
    std::uint64_t last_preview_fallback_pixel_count = 0;
    std::uint64_t total_celestial_light_count = 0;
    std::uint64_t total_emissive_light_count = 0;
    std::uint64_t total_shadow_candidate_count = 0;
    std::uint64_t total_candidate_count = 0;
    std::uint64_t total_sample_count = 0;
    std::uint64_t total_ray_count = 0;
    std::uint64_t invalid_ray_budget_rejections = 0;
    std::uint64_t last_ray_budget_primary_rays_per_pixel = 0;
    std::uint64_t last_ray_budget_shadow_rays_per_hit = 0;
    std::uint64_t last_ray_budget_gi_rays_per_hit = 0;
    std::uint64_t last_ray_budget_max_rays_per_frame = 0;
    std::uint64_t last_ray_budget_max_visited_voxels_per_ray = 0;
    std::uint64_t last_ray_budget_max_visited_sections_per_ray = 0;
    float last_celestial_light_energy = 0.0F;
    float last_emissive_light_energy = 0.0F;
    float last_output_energy = 0.0F;
    float last_output_min_sample = 0.0F;
    float last_output_max_sample = 0.0F;
    float last_physical_surface_energy = 0.0F;
    float last_preview_fallback_energy = 0.0F;
    float last_surface_payload_confidence = 0.0F;
    std::uint32_t last_payload_flags = 0;
    bool last_payload_accepted = false;
    bool last_payload_validated = false;
    bool last_payload_has_direct_work = false;
    bool last_payload_ready_for_shadow_tracing = false;
    bool last_enabled = false;
    bool last_ready = false;
    bool last_metadata_only = true;
    bool last_cpu_output_generated = false;
    bool last_output_write_recorded = false;
    bool last_resolve_recorded = false;
    bool last_ray_budget_valid = false;
    bool last_ray_budget_ingested = false;
    bool last_physical_surface_contribution = false;
    bool last_preview_fallback_contribution = false;
    bool last_focus_window_contribution = false;
    std::string last_payload_dimension_id;
    std::string last_output_marker;
    std::string last_readiness_reason;
    std::string last_ray_budget_marker;
    std::string last_ray_budget_rejection_reason;
};

struct NativeRound6DispatchExecutionTelemetry {
    NativeLightingDispatchStage stage = NativeLightingDispatchStage::DiffuseGi;
    std::uint64_t attempts = 0;
    std::uint64_t submitted = 0;
    std::uint64_t skipped = 0;
    std::uint64_t accepted = 0;
    std::uint64_t resource_markers = 0;
    std::uint64_t metadata_dispatches = 0;
    std::uint64_t cache_read_metadata_dispatches = 0;
    std::uint64_t cache_write_metadata_dispatches = 0;
    std::uint64_t cache_write_markers = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t last_width = 0;
    std::uint64_t last_height = 0;
    std::uint64_t last_dispatch_x = 0;
    std::uint64_t last_dispatch_y = 0;
    std::uint64_t last_dispatch_z = 0;
    std::uint64_t last_workgroup_size_x = 0;
    std::uint64_t last_workgroup_size_y = 0;
    std::uint64_t last_workgroup_size_z = 0;
    std::uint64_t last_input_count = 0;
    std::uint64_t last_output_count = 0;
    std::uint64_t last_sample_count = 0;
    std::uint64_t last_ray_count = 0;
    std::uint64_t last_cache_read_count = 0;
    std::uint64_t last_cache_write_count = 0;
    std::uint64_t total_sample_count = 0;
    std::uint64_t total_ray_count = 0;
    std::uint64_t total_cache_read_count = 0;
    std::uint64_t total_cache_write_count = 0;
    std::uint64_t last_placeholder_output_population_count = 0;
    std::uint64_t total_placeholder_output_population_count = 0;
    std::uint64_t last_visible_signal_population_count = 0;
    std::uint64_t total_visible_signal_population_count = 0;
    std::uint64_t last_visible_signal_sampled_pixels = 0;
    std::uint64_t last_visible_signal_nonzero_pixels = 0;
    std::uint64_t total_visible_signal_nonzero_pixels = 0;
    std::uint64_t last_visible_signal_checksum = 0;
    std::uint64_t last_cpu_output_width = 0;
    std::uint64_t last_cpu_output_height = 0;
    std::uint64_t last_cpu_output_pixel_count = 0;
    std::uint64_t last_cpu_output_surface_pixel_count = 0;
    std::uint64_t last_cpu_output_scene_driven_pixel_count = 0;
    std::uint64_t last_cpu_output_emissive_driven_pixel_count = 0;
    std::uint64_t last_cpu_output_spatial_lobe_pixel_count = 0;
    std::uint64_t last_cpu_output_cache_modulated_pixel_count = 0;
    std::uint64_t last_cpu_output_material_modulated_pixel_count = 0;
    std::uint64_t last_scene_linked_sample_count = 0;
    std::uint64_t last_material_color_modulated_sample_count = 0;
    std::uint64_t last_surface_normal_confident_sample_count = 0;
    std::uint64_t last_occlusion_dirty_modulated_sample_count = 0;
    std::uint64_t last_cpu_output_checksum = 0;
    std::uint64_t last_physical_output_checksum = 0;
    std::uint64_t last_scene_payload_generation = 0;
    std::uint64_t last_scene_celestial_generation = 0;
    std::uint64_t last_scene_emissive_generation = 0;
    std::uint64_t last_scene_shadow_generation = 0;
    std::uint64_t last_scene_shadow_candidate_generation = 0;
    std::uint64_t last_scene_section_snapshot_generation = 0;
    std::uint64_t last_scene_celestial_light_count = 0;
    std::uint64_t last_scene_emissive_light_count = 0;
    std::uint64_t last_scene_shadow_candidate_count = 0;
    std::uint64_t last_scene_budgeted_shadow_candidate_count = 0;
    std::uint64_t last_scene_section_snapshot_count = 0;
    float last_visible_signal_energy = 0.0F;
    float last_visible_signal_min_sample = 0.0F;
    float last_visible_signal_max_sample = 0.0F;
    float last_cpu_output_energy = 0.0F;
    float last_scene_celestial_light_energy = 0.0F;
    float last_scene_emissive_light_energy = 0.0F;
    float last_cpu_output_cache_response = 0.0F;
    float last_cpu_output_material_response = 0.0F;
    float last_scene_linked_energy = 0.0F;
    float last_material_color_influence = 0.0F;
    float last_surface_normal_confidence = 0.0F;
    float last_emissive_contribution_energy = 0.0F;
    float last_sun_contribution_energy = 0.0F;
    float last_occlusion_dirty_influence = 0.0F;
    float last_output_write_energy = 0.0F;
    float last_visible_signal_cache_factor = 0.0F;
    float last_visible_signal_ray_factor = 0.0F;
    std::uint64_t last_physical_scene_link_score = 0;
    std::uint32_t last_flags = 0;
    bool last_enabled = false;
    bool last_validated = false;
    bool last_placeholder = false;
    bool last_temporal_history = false;
    bool last_reuse_only = false;
    bool last_debug_overlay = false;
    bool last_ready = false;
    bool last_accepted = false;
    bool last_resource_marker_recorded = false;
    bool last_metadata_dispatch_recorded = false;
    bool last_cache_read_metadata_dispatch_recorded = false;
    bool last_cache_write_metadata_dispatch_recorded = false;
    bool last_placeholder_output_population_recorded = false;
    bool last_cache_write_marker_recorded = false;
    bool last_visible_signal_generated = false;
    bool last_visible_signal_cache_backed = false;
    bool last_cpu_output_generated = false;
    bool last_cpu_output_energy_nonzero = false;
    bool last_cpu_output_checksum_nonzero = false;
    bool last_cpu_output_nonzero = false;
    bool last_cpu_output_marker_recorded = false;
    bool last_cpu_output_scene_driven = false;
    bool last_cpu_output_emissive_driven = false;
    bool last_cpu_output_spatially_graded = false;
    bool last_cpu_output_material_driven = false;
    bool last_scene_linked_samples_recorded = false;
    bool last_material_color_influence_recorded = false;
    bool last_surface_normal_confidence_recorded = false;
    bool last_occlusion_dirty_influence_recorded = false;
    bool last_output_write_energy_recorded = false;
    bool last_scene_inputs_recorded = false;
    bool last_physical_scene_linked = false;
    bool last_physical_surface_contribution = false;
    bool last_preview_fallback_contribution = false;
    bool last_focus_window_contribution = false;
    bool last_metadata_only_proof_rejected = true;
    bool last_focus_window_capture_rejected = true;
    bool last_proof_marker_evidence_rejected = true;
    bool last_temporary_direct_substitution_rejected = true;
    bool last_rectangular_washout_rejected = true;
    std::string last_marker;
    std::string last_output_marker;
    std::string last_cpu_output_marker;
    std::string last_cache_marker;
    std::string last_physical_scene_marker;
    std::string last_physical_output_marker;
    std::string last_proof_boundary_marker;
    std::string last_readiness_reason;
    std::string last_scene_dimension_id;
};

struct NativeDenoiseExecutionTelemetry {
    std::uint64_t attempts = 0;
    std::uint64_t submitted = 0;
    std::uint64_t skipped = 0;
    std::uint64_t accepted = 0;
    std::uint64_t resource_markers = 0;
    std::uint64_t metadata_dispatches = 0;
    std::uint64_t history_accepted = 0;
    std::uint64_t history_rejected = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t last_width = 0;
    std::uint64_t last_height = 0;
    std::uint64_t last_input_count = 0;
    std::uint64_t last_output_count = 0;
    std::uint64_t last_sample_count = 0;
    std::uint64_t last_history_accepted = 0;
    std::uint64_t last_history_rejected = 0;
    std::uint64_t last_edge_rejected = 0;
    std::uint64_t last_edge_preserved = 0;
    std::uint64_t last_raw_gi_pixels = 0;
    std::uint64_t last_raw_gi_samples = 0;
    std::uint64_t last_raw_gi_rays = 0;
    std::uint64_t last_raw_gi_cache_reads = 0;
    std::uint64_t last_edge_input_count = 0;
    std::uint64_t last_history_input_count = 0;
    std::uint64_t last_denoised_output_pixels = 0;
    std::uint64_t last_denoised_output_checksum = 0;
    std::uint64_t last_denoised_output_changed_pixels = 0;
    std::uint64_t last_denoised_output_mean_abs_delta = 0;
    std::uint64_t last_temporal_stable_pixels = 0;
    std::uint64_t last_temporal_unstable_pixels = 0;
    std::uint64_t last_temporal_mean_abs_delta = 0;
    std::uint64_t last_raw_neighbor_luma_delta = 0;
    std::uint64_t last_denoised_neighbor_luma_delta = 0;
    std::uint64_t last_noise_reduction_percent = 0;
    std::uint64_t last_composite_width = 0;
    std::uint64_t last_composite_height = 0;
    std::uint64_t last_composite_outputs = 0;
    std::uint32_t last_flags = 0;
    std::uint32_t last_composite_flags = 0;
    bool last_enabled = false;
    bool last_validated = false;
    bool last_placeholder = false;
    bool last_temporal_history = false;
    bool last_ready = false;
    bool last_accepted = false;
    bool last_resource_marker_recorded = false;
    bool last_metadata_dispatch_recorded = false;
    bool last_edge_inputs_available = false;
    bool last_direct_shadow_signal_available = false;
    bool last_diffuse_gi_signal_available = false;
    bool last_optional_specular_placeholder = true;
    bool last_optional_ao_placeholder = true;
    bool last_raw_gi_input_available = false;
    bool last_raw_direct_input_available = false;
    bool last_raw_gi_input_ready = false;
    bool last_denoised_output_intent = false;
    bool last_denoised_cpu_output_generated = false;
    bool last_cpu_denoised_readback_ready = false;
    bool last_denoised_output_differs_from_raw = false;
    bool last_raw_gi_source_present = false;
    bool last_cpu_denoised_source_present = false;
    bool last_shader_denoise_dispatch_intent = false;
    bool last_shader_denoise_dispatch_prepared = false;
    bool last_shader_denoise_input_ready = false;
    bool last_shader_denoise_output_ready = false;
    bool last_shader_denoise_output_image_ready = false;
    bool last_shader_denoise_output_material_ready = false;
    bool last_metadata_only_path = true;
    bool last_real_denoise_shader_output = false;
    bool last_shader_denoise_output_shader_generated = false;
    bool last_cpu_fallback_quality_metrics = false;
    bool last_composite_stage_recorded = false;
    bool last_composite_enabled = false;
    bool last_composite_ready = false;
    bool last_composite_placeholder = false;
    bool last_edge_depth_available = false;
    bool last_edge_normal_available = false;
    bool last_edge_material_available = false;
    bool last_history_confidence_available = false;
    bool last_shader_boundary_explicit = false;
    bool last_metadata_only_proof_rejected = true;
    bool last_focus_window_capture_rejected = true;
    bool last_proof_marker_evidence_rejected = true;
    bool last_temporary_direct_substitution_rejected = true;
    bool last_rectangular_washout_rejected = true;
    std::string last_output_marker;
    std::string last_source_identity_marker;
    std::string last_raw_input_marker;
    std::string last_denoised_output_marker;
    std::string last_shader_denoise_readiness_marker;
    std::string last_shader_denoise_handoff_marker;
    std::string last_shader_denoise_output_readiness_marker;
    std::string last_shader_denoise_generation_marker;
    std::string last_composite_marker;
    std::string last_history_acceptance_reason;
    std::string last_history_rejection_reason;
    std::string last_shader_boundary_marker;
    std::string last_temporal_history_marker;
    std::string last_proof_boundary_marker;
    std::string last_quality_marker;
    std::string last_readiness_reason;
};

struct NativeRound11RestirTelemetry {
    std::uint64_t metadata_packets = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_dispatch_generation = 0;
    std::uint64_t last_direct_payload_generation = 0;
    std::uint64_t direct_reservoir_count = 0;
    std::uint64_t candidate_count = 0;
    std::uint64_t selected_light_count = 0;
    std::uint64_t temporal_reuse_count = 0;
    std::uint64_t spatial_reuse_count = 0;
    std::uint64_t gi_reservoir_count = 0;
    std::uint64_t path_reuse_count = 0;
    std::uint64_t invalidated_reservoir_count = 0;
    std::uint64_t confidence_sample_count = 0;
    std::uint64_t restir_di_candidate_count = 0;
    std::uint64_t restir_di_selected_count = 0;
    std::uint64_t restir_di_temporal_reuse_count = 0;
    std::uint64_t restir_di_spatial_reuse_count = 0;
    std::uint64_t restir_di_output_checksum = 0;
    float restir_di_output_energy = 0.0F;
    double restir_di_candidate_reduction_ratio = 0.0;
    double confidence_min = 0.0;
    double confidence_mean = 0.0;
    double confidence_max = 0.0;
    bool metadata_only = true;
    bool real_restir_execution = false;
    bool real_restir_di_execution = false;
    std::string boundary_marker;
    std::string confidence_marker;
    std::string source_marker;
};

struct NativeLightingDispatchTelemetry {
    std::uint64_t packets = 0;
    std::uint64_t advertised_dispatches = 0;
    std::uint64_t payload_dispatches = 0;
    std::uint64_t enabled_dispatches = 0;
    std::uint64_t disabled_dispatches = 0;
    std::uint64_t allocation_intents = 0;
    std::uint64_t placeholder_buffers = 0;
    std::uint64_t last_packet_generation = 0;
    std::uint64_t last_first_generation = 0;
    std::uint64_t last_generation = 0;
    std::uint64_t last_world_generation = 0;
    std::uint64_t last_material_generation = 0;
    std::uint64_t last_section_generation = 0;
    std::uint64_t last_gbuffer_generation = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::uint64_t last_enabled_stage_count = 0;
    std::uint64_t last_disabled_stage_count = 0;
    std::uint64_t last_input_count = 0;
    std::uint64_t last_output_count = 0;
    std::uint64_t last_sample_count = 0;
    std::uint64_t last_ray_count = 0;
    std::uint64_t last_cache_read_count = 0;
    std::uint64_t last_cache_write_count = 0;
    std::uint64_t last_enabled_sample_count = 0;
    std::uint64_t last_enabled_ray_count = 0;
    std::uint64_t last_enabled_cache_read_count = 0;
    std::uint64_t last_enabled_cache_write_count = 0;
    std::uint64_t total_sample_count = 0;
    std::uint64_t total_ray_count = 0;
    std::uint64_t total_cache_read_count = 0;
    std::uint64_t total_cache_write_count = 0;
    std::uint64_t last_placeholder_stage_count = 0;
    std::uint64_t last_validated_stage_count = 0;
    std::uint64_t last_temporal_history_stage_count = 0;
    std::uint64_t last_reuse_only_stage_count = 0;
    std::uint64_t last_debug_overlay_stage_count = 0;
    std::uint64_t total_placeholder_stage_count = 0;
    std::uint64_t total_validated_stage_count = 0;
    std::uint64_t total_temporal_history_stage_count = 0;
    std::uint64_t total_reuse_only_stage_count = 0;
    std::uint64_t total_debug_overlay_stage_count = 0;
    bool last_has_placeholder_stage = false;
    bool last_has_validated_stage = false;
    bool last_has_temporal_history_stage = false;
    bool last_has_reuse_only_stage = false;
    bool last_has_debug_overlay_stage = false;
    bool last_ready_for_native_execution = false;
    bool last_payload_recorded_this_frame = false;
    std::string last_enabled_stage_names;
    std::string last_readiness_reason;
    NativeDirectLightingExecutionTelemetry direct_execution;
    NativeRound6DispatchExecutionTelemetry diffuse_gi_execution;
    NativeRound6DispatchExecutionTelemetry cache_execution;
    NativeDenoiseExecutionTelemetry denoise_execution;
    NativeAdaptiveBudgetTelemetry adaptive_budget;
    NativeRound11RestirTelemetry round11_restir;
    std::array<NativeLightingDispatchStageTelemetry, kNativeLightingDispatchStageCount> stages;
    std::array<NativeLightingDispatchPayloadCategoryTelemetry, kNativeLightingDispatchPayloadCategoryCount> payload_categories;
};

struct NativeStagingTelemetry {
    NativeSectionStagingTelemetry section;
    NativeVoxelStagingTelemetry voxel;
    NativeVirtualChunkGeometryTelemetry virtual_geometry;
    NativeGBufferStagingTelemetry gbuffer;
    NativeLightingDispatchTelemetry lighting;
};

class Renderer {
public:
    Renderer();
    ~Renderer();

    Renderer(const Renderer&) = delete;
    Renderer& operator=(const Renderer&) = delete;

    void init();
    void shutdown();
    void resize(std::int32_t width, std::int32_t height);
    void begin_frame(FrameInfo info);
    void upload_world_deltas(UploadPacket packet);
    void upload_section_snapshots(SectionUploadPacket packet);
    void upload_gbuffer_staging(GBufferStagingPacket packet);
    void upload_lighting_dispatch(LightingDispatchPacket packet);
    void upload_direct_lighting_payload(DirectLightingPayloadPacket packet);
    void render_lighting();
    void end_frame();
    void adopt_borrowed_context(BorrowedVulkanContext context);
    void release_borrowed_context();

    [[nodiscard]] bool initialized() const;
    [[nodiscard]] std::string last_error() const;
    [[nodiscard]] std::string status() const;
    [[nodiscard]] std::vector<std::uint8_t> direct_lighting_cpu_output_preview_rgba8() const;
    [[nodiscard]] std::vector<std::uint8_t> diffuse_gi_cpu_output_preview_rgba8() const;
    [[nodiscard]] std::vector<std::uint8_t> denoised_diffuse_gi_cpu_output_preview_rgba8();

private:
    void ensure_initialized(const char* operation) const;
    void clear_error();
    void set_error(std::string error);
    [[nodiscard]] std::uint64_t estimate_upload_staging_bytes(const UploadPacket& packet) const;
    [[nodiscard]] std::uint64_t estimate_section_snapshot_staging_bytes(const SectionUploadPacket& packet) const;
    [[nodiscard]] std::uint64_t estimate_section_staging_bytes(std::uint64_t dirty_section_count) const;
    [[nodiscard]] std::uint64_t estimate_voxel_staging_bytes(std::uint64_t dirty_section_count) const;
    [[nodiscard]] std::uint64_t estimate_virtual_cluster_upload_bytes(std::uint64_t cluster_count) const;
    [[nodiscard]] std::uint64_t estimate_gbuffer_attachment_bytes(std::int32_t width, std::int32_t height, std::uint32_t bytes_per_pixel) const;
    [[nodiscard]] std::uint64_t estimate_gbuffer_attachment_bytes(const GBufferAttachmentUpload& attachment) const;
    [[nodiscard]] std::uint64_t estimate_lighting_dispatch_bytes(const LightingDispatchStageUpload& dispatch) const;
    void track_upload_staging_placeholder(const UploadPacket& packet);
    void track_section_snapshot_staging_placeholder(const SectionUploadPacket& packet);
    void track_virtual_chunk_geometry_metadata(const SectionUploadPacket& packet);
    void track_gbuffer_staging_upload(const GBufferStagingPacket& packet);
    void track_lighting_dispatch_upload(const LightingDispatchPacket& packet);
    void track_round11_restir_metadata();
    void track_gbuffer_placeholder_intent();
    [[nodiscard]] std::uint64_t track_noop_lighting_placeholder();
    [[nodiscard]] std::uint64_t track_flat_composite_placeholder();
    [[nodiscard]] std::uint64_t track_direct_lighting_execution_scaffold();
    [[nodiscard]] std::uint64_t track_round6_dispatch_execution_scaffold(
            NativeLightingDispatchStage dispatch_stage,
            NativeRound6DispatchExecutionTelemetry& execution,
            const char* accepted_marker);
    [[nodiscard]] std::uint64_t track_denoise_execution_scaffold();
    [[nodiscard]] bool generate_denoised_diffuse_gi_cpu_output_rgba8();
    void reset_staging_telemetry();
    void reset_pass_counters();
    void prepare_frame_passes();
    void mark_pass_not_wired(NativeRenderPass pass);
    void mark_pass_submitted(NativeRenderPass pass, std::uint64_t placeholder_resources);
    void mark_pass_skipped(NativeRenderPass pass, NativeRenderPassState state, bool missing_context);
    [[nodiscard]] NativeRenderPassCounters& pass_counters(NativeRenderPass pass);
    [[nodiscard]] const NativeRenderPassCounters& pass_counters(NativeRenderPass pass) const;
    [[nodiscard]] NativeLightingDispatchStageTelemetry& lighting_stage_telemetry(NativeLightingDispatchStage stage);
    [[nodiscard]] const NativeLightingDispatchStageTelemetry& lighting_stage_telemetry(NativeLightingDispatchStage stage) const;

    std::unique_ptr<ResourceManager> resources_;
    std::array<NativeRenderPassCounters, kNativeRenderPassCount> pass_counters_;
    NativeStagingTelemetry staging_;
    std::string last_error_;
    bool initialized_ = false;
    bool frame_open_ = false;
    bool current_frame_borrowed_context_adopted_ = false;
    bool current_frame_context_released_ = false;
    bool current_frame_render_lighting_submitted_ = false;
    bool current_frame_order_valid_ = true;
    bool last_frame_borrowed_context_adopted_ = false;
    bool last_render_lighting_order_valid_ = true;
    bool last_end_frame_order_valid_ = true;
    std::int32_t width_ = 0;
    std::int32_t height_ = 0;
    std::uint64_t frame_index_ = 0;
    UploadPacket last_upload_packet_;
    SectionUploadPacket last_section_upload_packet_;
    GBufferStagingPacket last_gbuffer_staging_packet_;
    LightingDispatchPacket last_lighting_dispatch_packet_;
    DirectLightingPayloadPacket last_direct_lighting_payload_packet_;
    std::vector<float> direct_lighting_cpu_output_;
    std::vector<float> diffuse_gi_cpu_output_;
    std::vector<std::uint8_t> denoised_diffuse_gi_cpu_output_rgba8_;
    float last_tick_delta_ = 0.0F;
    std::uint64_t resize_count_ = 0;
    std::uint64_t begin_frame_count_ = 0;
    std::uint64_t end_frame_count_ = 0;
    std::uint64_t upload_packet_count_ = 0;
    std::uint64_t section_upload_packet_count_ = 0;
    std::uint64_t gbuffer_staging_packet_count_ = 0;
    std::uint64_t lighting_dispatch_packet_count_ = 0;
    std::uint64_t direct_lighting_payload_packet_count_ = 0;
    std::uint64_t upload_dirty_payload_total_ = 0;
    std::uint64_t upload_material_payload_total_ = 0;
    std::uint64_t section_snapshot_payload_total_ = 0;
    std::uint64_t lighting_pass_count_ = 0;
    std::uint64_t context_adopt_count_ = 0;
    std::uint64_t context_release_count_ = 0;
    std::uint64_t context_adopted_for_frame_count_ = 0;
    std::uint64_t context_released_during_frame_count_ = 0;
    std::uint64_t frame_without_context_count_ = 0;
    std::uint64_t invalid_begin_frame_order_count_ = 0;
    std::uint64_t invalid_render_lighting_order_count_ = 0;
    std::uint64_t invalid_end_frame_order_count_ = 0;
    std::uint64_t render_lighting_without_frame_count_ = 0;
    std::uint64_t render_lighting_without_context_count_ = 0;
    std::uint64_t render_lighting_duplicate_count_ = 0;
    std::uint64_t end_frame_without_begin_count_ = 0;
    std::uint64_t end_frame_without_context_count_ = 0;
    std::uint64_t end_frame_without_lighting_count_ = 0;
};

[[nodiscard]] const char* to_string(NativeRenderPass pass);
[[nodiscard]] const char* to_string(NativeRenderPassState state);
[[nodiscard]] const char* to_string(NativeLightingDispatchStage stage);

} // namespace lucerna
