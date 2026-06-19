#pragma once

#include <cstdint>
#include <memory>
#include <string>

namespace lucerna {

struct FrameInfo {
    std::uint64_t frame_index = 0;
    float tick_delta = 0.0F;
};

struct UploadSummary {
    std::uint64_t generation = 0;
    std::int32_t dirty_region_count = 0;
    std::int32_t material_update_count = 0;
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
    void upload_world_deltas(UploadSummary summary);
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

    std::unique_ptr<ResourceManager> resources_;
    std::string last_error_;
    bool initialized_ = false;
    bool frame_open_ = false;
    std::int32_t width_ = 0;
    std::int32_t height_ = 0;
    std::uint64_t frame_index_ = 0;
    std::uint64_t last_upload_generation_ = 0;
};

} // namespace lucerna
