#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace lucerna {

inline constexpr std::uint32_t kInvalidVulkanQueueFamily = ~std::uint32_t{0};

enum class BorrowedContextState : std::uint8_t {
    Empty,
    Adopted,
    Released
};

enum class NativeResourceOwnership : std::uint8_t {
    Borrowed,
    LucernaPlaceholder
};

enum class NativeResourceKind : std::uint8_t {
    Buffer,
    Image
};

enum class NativeResourceIntentStage : std::uint8_t {
    WorldDeltaUpload,
    SectionUpload,
    VoxelUpload,
    FutureGBuffer,
    DirectLighting,
    DiffuseGi,
    Denoise,
    Composite,
    LightingCache
};

struct BorrowedVulkanContext {
    std::uint64_t instance = 0;
    std::uint64_t physical_device = 0;
    std::uint64_t device = 0;
    std::uint64_t graphics_queue = 0;
    std::uint32_t graphics_queue_family = kInvalidVulkanQueueFamily;
    BorrowedContextState state = BorrowedContextState::Empty;
    std::uint64_t adopted_generation = 0;
    std::uint64_t released_generation = 0;
    std::string debug_label;

    [[nodiscard]] bool has_required_handles() const;
};

struct ResourceLifetimeTelemetry {
    std::uint64_t created_generation = 0;
    std::uint64_t last_used_generation = 0;
    std::uint64_t released_generation = 0;
    std::uint64_t use_count = 0;
    bool live = false;
};

struct ResourceLifetimeCounters {
    std::uint64_t created = 0;
    std::uint64_t reused = 0;
    std::uint64_t released = 0;
    std::uint64_t live = 0;
};

struct ResourceAllocationIntent {
    NativeResourceKind kind = NativeResourceKind::Buffer;
    NativeResourceIntentStage stage = NativeResourceIntentStage::WorldDeltaUpload;
    std::uint64_t generation = 0;
    std::uint64_t frame_index = 0;
    std::uint64_t estimated_bytes = 0;
    std::int32_t width = 0;
    std::int32_t height = 0;
    std::uint32_t format_tag = 0;
    std::string debug_label;
};

struct ResourceAllocationIntentCounters {
    std::uint64_t recorded = 0;
    std::uint64_t buffers = 0;
    std::uint64_t images = 0;
    std::uint64_t estimated_bytes = 0;
    std::uint64_t last_generation = 0;
};

struct NativeBufferHandle {
    std::uint64_t handle = 0;
    std::uint64_t byte_size = 0;
    NativeResourceOwnership ownership = NativeResourceOwnership::LucernaPlaceholder;
    std::string debug_label;
    ResourceLifetimeTelemetry lifetime;

    [[nodiscard]] bool live() const;
};

struct NativeImageHandle {
    std::uint64_t handle = 0;
    std::int32_t width = 0;
    std::int32_t height = 0;
    std::uint32_t mip_levels = 1;
    std::uint32_t format_tag = 0;
    NativeResourceOwnership ownership = NativeResourceOwnership::LucernaPlaceholder;
    std::string debug_label;
    ResourceLifetimeTelemetry lifetime;

    [[nodiscard]] bool live() const;
};

struct FrameResourceRingStats;

struct FrameResources {
    std::uint32_t ring_index = 0;
    std::uint64_t generation = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t reset_count = 0;
    std::vector<NativeBufferHandle> transient_buffers;
    std::vector<NativeImageHandle> transient_images;
    std::vector<ResourceAllocationIntent> allocation_intents;
    ResourceLifetimeCounters buffer_lifetime;
    ResourceLifetimeCounters image_lifetime;
    ResourceAllocationIntentCounters allocation_intent_counters;

    void clear_transient();
    void clear_transient(std::uint64_t release_generation);

    [[nodiscard]] std::size_t live_buffer_count() const;
    [[nodiscard]] std::size_t live_image_count() const;
    [[nodiscard]] std::size_t allocation_intent_count() const;
    [[nodiscard]] FrameResourceRingStats stats() const;
    [[nodiscard]] std::string status() const;
};

struct FrameResourceRingStats {
    std::uint32_t ring_index = 0;
    std::uint64_t generation = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t reset_count = 0;
    std::size_t transient_buffer_count = 0;
    std::size_t transient_image_count = 0;
    std::size_t live_buffer_count = 0;
    std::size_t live_image_count = 0;
    std::size_t allocation_intent_count = 0;
    ResourceLifetimeCounters buffer_lifetime;
    ResourceLifetimeCounters image_lifetime;
    ResourceAllocationIntentCounters allocation_intent_counters;
};

struct ResourceManagerStats {
    bool has_context = false;
    bool has_active_ring = false;
    std::uint32_t frames_in_flight = 0;
    std::uint32_t active_ring_index = 0;
    std::uint64_t last_frame_index = 0;
    std::uint64_t context_adoption_generation = 0;
    std::uint64_t context_release_generation = 0;
    std::uint64_t resource_generation = 0;
    std::size_t transient_buffer_count = 0;
    std::size_t transient_image_count = 0;
    std::size_t live_buffer_count = 0;
    std::size_t live_image_count = 0;
    std::size_t allocation_intent_count = 0;
    ResourceLifetimeCounters buffer_lifetime;
    ResourceLifetimeCounters image_lifetime;
    ResourceAllocationIntentCounters allocation_intent_counters;
    std::vector<FrameResourceRingStats> rings;
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
    [[nodiscard]] const FrameResources& frame(std::uint64_t frame_index) const;
    void reset_frame(std::uint64_t frame_index);
    NativeBufferHandle& track_transient_buffer(
            std::uint64_t frame_index,
            std::uint64_t handle,
            std::uint64_t byte_size,
            std::string debug_label,
            NativeResourceOwnership ownership = NativeResourceOwnership::LucernaPlaceholder);
    NativeImageHandle& track_transient_image(
            std::uint64_t frame_index,
            std::uint64_t handle,
            std::int32_t width,
            std::int32_t height,
            std::uint32_t format_tag,
            std::string debug_label,
            NativeResourceOwnership ownership = NativeResourceOwnership::LucernaPlaceholder);
    ResourceAllocationIntent& track_buffer_allocation_intent(
            std::uint64_t frame_index,
            std::uint64_t byte_size,
            std::string debug_label,
            NativeResourceIntentStage stage);
    ResourceAllocationIntent& track_image_allocation_intent(
            std::uint64_t frame_index,
            std::int32_t width,
            std::int32_t height,
            std::uint32_t format_tag,
            std::uint64_t estimated_bytes,
            std::string debug_label,
            NativeResourceIntentStage stage);

    [[nodiscard]] bool has_context() const;
    [[nodiscard]] std::uint32_t frames_in_flight() const;
    [[nodiscard]] const BorrowedVulkanContext& context() const;
    [[nodiscard]] std::uint64_t context_adoption_generation() const;
    [[nodiscard]] std::uint64_t context_release_generation() const;
    [[nodiscard]] std::uint64_t resource_generation() const;
    [[nodiscard]] ResourceManagerStats stats() const;
    [[nodiscard]] std::string status() const;

private:
    void clear_frame_resources();
    [[nodiscard]] std::uint64_t next_resource_generation();
    [[nodiscard]] std::uint64_t next_placeholder_handle(NativeResourceKind kind);

    BorrowedVulkanContext context_;
    std::vector<FrameResources> frames_;
    bool has_context_ = false;
    bool has_active_ring_ = false;
    std::uint32_t active_ring_index_ = 0;
    std::uint64_t last_frame_index_ = 0;
    std::uint64_t context_adoption_generation_ = 0;
    std::uint64_t context_release_generation_ = 0;
    std::uint64_t resource_generation_ = 0;
    std::uint64_t next_placeholder_handle_ = 1;
};

[[nodiscard]] const char* to_string(BorrowedContextState state);
[[nodiscard]] const char* to_string(NativeResourceKind kind);
[[nodiscard]] const char* to_string(NativeResourceOwnership ownership);
[[nodiscard]] const char* to_string(NativeResourceIntentStage stage);

} // namespace lucerna
