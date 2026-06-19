#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace lucerna {

inline constexpr std::uint32_t kInvalidVulkanQueueFamily = ~std::uint32_t{0};

struct BorrowedVulkanContext {
    std::uint64_t instance = 0;
    std::uint64_t physical_device = 0;
    std::uint64_t device = 0;
    std::uint64_t graphics_queue = 0;
    std::uint32_t graphics_queue_family = kInvalidVulkanQueueFamily;

    [[nodiscard]] bool has_required_handles() const;
};

struct FrameResources {
    std::uint64_t generation = 0;
    std::vector<std::uint64_t> transient_buffers;
    std::vector<std::uint64_t> transient_images;

    void clear_transient();
};

class ResourceManager {
public:
    explicit ResourceManager(std::uint32_t frames_in_flight);
    ~ResourceManager();

    ResourceManager(const ResourceManager&) = delete;
    ResourceManager& operator=(const ResourceManager&) = delete;

    void adopt_context(BorrowedVulkanContext context);
    void release_context();
    FrameResources& frame(std::uint64_t frame_index);
    void reset_frame(std::uint64_t frame_index);

    [[nodiscard]] bool has_context() const;
    [[nodiscard]] std::uint32_t frames_in_flight() const;
    [[nodiscard]] const BorrowedVulkanContext& context() const;
    [[nodiscard]] std::string status() const;

private:
    void clear_frame_resources();

    BorrowedVulkanContext context_;
    std::vector<FrameResources> frames_;
    bool has_context_ = false;
};

} // namespace lucerna
