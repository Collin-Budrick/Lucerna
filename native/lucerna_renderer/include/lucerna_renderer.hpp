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
    void track_upload_staging_placeholder(const UploadPacket& packet);
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
    float last_tick_delta_ = 0.0F;
    std::uint64_t resize_count_ = 0;
    std::uint64_t begin_frame_count_ = 0;
    std::uint64_t end_frame_count_ = 0;
    std::uint64_t upload_packet_count_ = 0;
    std::uint64_t upload_dirty_payload_total_ = 0;
    std::uint64_t upload_material_payload_total_ = 0;
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
