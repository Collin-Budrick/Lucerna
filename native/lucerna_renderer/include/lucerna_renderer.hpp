#pragma once

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
    void track_lighting_placeholders();

    std::unique_ptr<ResourceManager> resources_;
    std::string last_error_;
    bool initialized_ = false;
    bool frame_open_ = false;
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
};

} // namespace lucerna
