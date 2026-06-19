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

struct NativeGBufferStagingTelemetry {
    std::uint64_t frames_planned = 0;
    std::uint64_t allocation_intents = 0;
    std::uint64_t attachment_intents = 0;
    std::uint64_t last_attachment_count = 0;
    std::uint64_t last_estimated_bytes = 0;
    std::uint64_t total_estimated_bytes = 0;
    std::int32_t last_width = 0;
    std::int32_t last_height = 0;
    bool planned_this_frame = false;
};

struct NativeStagingTelemetry {
    NativeSectionStagingTelemetry section;
    NativeVoxelStagingTelemetry voxel;
    NativeGBufferStagingTelemetry gbuffer;
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
    void render_lighting();
    void end_frame();
    void adopt_borrowed_context(BorrowedVulkanContext context);
    void release_borrowed_context();

    [[nodiscard]] bool initialized() const;
    [[nodiscard]] std::string last_error() const;
    [[nodiscard]] std::string status() const;

private:
    void ensure_initialized(const char* operation) const;
    void clear_error();
    void set_error(std::string error);
    [[nodiscard]] std::uint64_t estimate_upload_staging_bytes(const UploadPacket& packet) const;
    [[nodiscard]] std::uint64_t estimate_section_snapshot_staging_bytes(const SectionUploadPacket& packet) const;
    [[nodiscard]] std::uint64_t estimate_section_staging_bytes(std::uint64_t dirty_section_count) const;
    [[nodiscard]] std::uint64_t estimate_voxel_staging_bytes(std::uint64_t dirty_section_count) const;
    [[nodiscard]] std::uint64_t estimate_gbuffer_attachment_bytes(std::int32_t width, std::int32_t height, std::uint32_t bytes_per_pixel) const;
    void track_upload_staging_placeholder(const UploadPacket& packet);
    void track_section_snapshot_staging_placeholder(const SectionUploadPacket& packet);
    void track_gbuffer_placeholder_intent();
    [[nodiscard]] std::uint64_t track_noop_lighting_placeholder();
    [[nodiscard]] std::uint64_t track_flat_composite_placeholder();
    void reset_pass_counters();
    void prepare_frame_passes();
    void mark_pass_not_wired(NativeRenderPass pass);
    void mark_pass_submitted(NativeRenderPass pass, std::uint64_t placeholder_resources);
    void mark_pass_skipped(NativeRenderPass pass, NativeRenderPassState state, bool missing_context);
    [[nodiscard]] NativeRenderPassCounters& pass_counters(NativeRenderPass pass);
    [[nodiscard]] const NativeRenderPassCounters& pass_counters(NativeRenderPass pass) const;

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
    float last_tick_delta_ = 0.0F;
    std::uint64_t resize_count_ = 0;
    std::uint64_t begin_frame_count_ = 0;
    std::uint64_t end_frame_count_ = 0;
    std::uint64_t upload_packet_count_ = 0;
    std::uint64_t section_upload_packet_count_ = 0;
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

} // namespace lucerna
