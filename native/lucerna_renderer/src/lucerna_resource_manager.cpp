#include "lucerna_resource_manager.hpp"

#include <cstddef>
#include <sstream>
#include <stdexcept>
#include <utility>

namespace lucerna {

namespace {

std::string label_or(std::string label, const char* fallback) {
    if (label.empty()) {
        return fallback;
    }
    return label;
}

void append_queue_family(std::ostringstream& out, std::uint32_t queue_family) {
    if (queue_family == kInvalidVulkanQueueFamily) {
        out << "invalid";
    } else {
        out << queue_family;
    }
}

} // namespace

const char* to_string(BorrowedContextState state) {
    switch (state) {
        case BorrowedContextState::Empty:
            return "empty";
        case BorrowedContextState::Adopted:
            return "adopted";
        case BorrowedContextState::Released:
            return "released";
    }

    return "unknown";
}

const char* to_string(NativeResourceKind kind) {
    switch (kind) {
        case NativeResourceKind::Buffer:
            return "buffer";
        case NativeResourceKind::Image:
            return "image";
    }

    return "unknown";
}

const char* to_string(NativeResourceOwnership ownership) {
    switch (ownership) {
        case NativeResourceOwnership::Borrowed:
            return "borrowed";
        case NativeResourceOwnership::LucernaPlaceholder:
            return "lucerna_placeholder";
    }

    return "unknown";
}

const char* to_string(NativeResourceIntentStage stage) {
    switch (stage) {
        case NativeResourceIntentStage::WorldDeltaUpload:
            return "world_delta_upload";
        case NativeResourceIntentStage::SectionUpload:
            return "section_upload";
        case NativeResourceIntentStage::VoxelUpload:
            return "voxel_upload";
        case NativeResourceIntentStage::FutureGBuffer:
            return "future_gbuffer";
    }

    return "unknown";
}

bool BorrowedVulkanContext::has_required_handles() const {
    return instance != 0
        && physical_device != 0
        && device != 0
        && graphics_queue != 0
        && graphics_queue_family != kInvalidVulkanQueueFamily;
}

bool NativeBufferHandle::live() const {
    return lifetime.live && lifetime.released_generation == 0;
}

bool NativeImageHandle::live() const {
    return lifetime.live && lifetime.released_generation == 0;
}

void FrameResources::clear_transient() {
    clear_transient(0);
}

void FrameResources::clear_transient(std::uint64_t release_generation) {
    for (auto& buffer : transient_buffers) {
        if (buffer.live()) {
            buffer.lifetime.live = false;
            buffer.lifetime.released_generation = release_generation;
            buffer_lifetime.released++;
        }
    }
    transient_buffers.clear();

    for (auto& image : transient_images) {
        if (image.live()) {
            image.lifetime.live = false;
            image.lifetime.released_generation = release_generation;
            image_lifetime.released++;
        }
    }
    transient_images.clear();
    allocation_intents.clear();
    buffer_lifetime.live = 0;
    image_lifetime.live = 0;
}

std::size_t FrameResources::live_buffer_count() const {
    std::size_t count = 0;
    for (const auto& buffer : transient_buffers) {
        if (buffer.live()) {
            count++;
        }
    }
    return count;
}

std::size_t FrameResources::live_image_count() const {
    std::size_t count = 0;
    for (const auto& image : transient_images) {
        if (image.live()) {
            count++;
        }
    }
    return count;
}

std::size_t FrameResources::allocation_intent_count() const {
    return allocation_intents.size();
}

FrameResourceRingStats FrameResources::stats() const {
    return FrameResourceRingStats{
        ring_index,
        generation,
        last_frame_index,
        reset_count,
        transient_buffers.size(),
        transient_images.size(),
        live_buffer_count(),
        live_image_count(),
        allocation_intent_count(),
        buffer_lifetime,
        image_lifetime,
        allocation_intent_counters
    };
}

std::string FrameResources::status() const {
    std::ostringstream out;
    out << "ring=" << ring_index
        << " generation=" << generation
        << " last_frame=" << last_frame_index
        << " resets=" << reset_count
        << " buffers={live=" << live_buffer_count()
        << ",created=" << buffer_lifetime.created
        << ",reused=" << buffer_lifetime.reused
        << ",released=" << buffer_lifetime.released
        << "}"
        << " images={live=" << live_image_count()
        << ",created=" << image_lifetime.created
        << ",reused=" << image_lifetime.reused
        << ",released=" << image_lifetime.released
        << "}"
        << " allocation_intents={active=" << allocation_intent_count()
        << ",recorded=" << allocation_intent_counters.recorded
        << ",buffers=" << allocation_intent_counters.buffers
        << ",images=" << allocation_intent_counters.images
        << ",estimated_bytes=" << allocation_intent_counters.estimated_bytes
        << ",last_generation=" << allocation_intent_counters.last_generation
        << "}";

    if (!transient_buffers.empty()) {
        const auto& buffer = transient_buffers.front();
        out << " first_buffer={label=\"" << buffer.debug_label
            << "\",handle=" << buffer.handle
            << ",bytes=" << buffer.byte_size
            << ",ownership=" << to_string(buffer.ownership)
            << ",uses=" << buffer.lifetime.use_count
            << ",created_generation=" << buffer.lifetime.created_generation
            << ",last_used_generation=" << buffer.lifetime.last_used_generation
            << "}";
    }

    if (!transient_images.empty()) {
        const auto& image = transient_images.front();
        out << " first_image={label=\"" << image.debug_label
            << "\",handle=" << image.handle
            << ",size=" << image.width << "x" << image.height
            << ",format_tag=" << image.format_tag
            << ",ownership=" << to_string(image.ownership)
            << ",uses=" << image.lifetime.use_count
            << ",created_generation=" << image.lifetime.created_generation
            << ",last_used_generation=" << image.lifetime.last_used_generation
            << "}";
    }

    if (!allocation_intents.empty()) {
        const auto& intent = allocation_intents.front();
        out << " first_intent={label=\"" << intent.debug_label
            << "\",kind=" << to_string(intent.kind)
            << ",stage=" << to_string(intent.stage)
            << ",frame=" << intent.frame_index
            << ",generation=" << intent.generation
            << ",estimated_bytes=" << intent.estimated_bytes;
        if (intent.kind == NativeResourceKind::Image) {
            out << ",size=" << intent.width << "x" << intent.height
                << ",format_tag=" << intent.format_tag;
        }
        out << "}";
    }

    return out.str();
}

ResourceManager::ResourceManager(std::uint32_t frames_in_flight) {
    if (frames_in_flight == 0) {
        throw std::invalid_argument("frames_in_flight must be greater than zero");
    }

    frames_.resize(frames_in_flight);
    for (std::size_t index = 0; index < frames_.size(); index++) {
        frames_[index].ring_index = static_cast<std::uint32_t>(index);
    }
}

ResourceManager::~ResourceManager() {
    release_context();
}

void ResourceManager::adopt_context(BorrowedVulkanContext context) {
    if (!context.has_required_handles()) {
        throw std::invalid_argument("borrowed Vulkan context is missing required handles");
    }

    clear_frame_resources();
    const auto generation = next_resource_generation();
    context_ = std::move(context);
    context_.state = BorrowedContextState::Adopted;
    context_.adopted_generation = generation;
    context_.released_generation = 0;
    context_.debug_label = label_or(std::move(context_.debug_label), "borrowed-sodium-vulkan-context");
    context_adoption_generation_ = generation;
    has_context_ = true;
}

void ResourceManager::release_context() {
    if (has_context_) {
        const auto generation = next_resource_generation();
        context_release_generation_ = generation;
        context_.state = BorrowedContextState::Released;
        context_.released_generation = generation;
    }

    clear_frame_resources();
    context_ = {};
    context_.state = BorrowedContextState::Released;
    context_.released_generation = context_release_generation_;
    has_context_ = false;
}

FrameResources& ResourceManager::frame(std::uint64_t frame_index) {
    return frames_.at(static_cast<std::size_t>(frame_index % frames_.size()));
}

const FrameResources& ResourceManager::frame(std::uint64_t frame_index) const {
    return frames_.at(static_cast<std::size_t>(frame_index % frames_.size()));
}

void ResourceManager::reset_frame(std::uint64_t frame_index) {
    auto& resources = frame(frame_index);
    resources.clear_transient(next_resource_generation());
    resources.generation++;
    resources.last_frame_index = frame_index;
    resources.reset_count++;
    has_active_ring_ = true;
    active_ring_index_ = resources.ring_index;
    last_frame_index_ = frame_index;
}

NativeBufferHandle& ResourceManager::track_transient_buffer(
        std::uint64_t frame_index,
        std::uint64_t handle,
        std::uint64_t byte_size,
        std::string debug_label,
        NativeResourceOwnership ownership) {
    auto& resources = frame(frame_index);
    if (handle == 0) {
        handle = next_placeholder_handle(NativeResourceKind::Buffer);
    }

    const auto generation = next_resource_generation();
    for (auto& buffer : resources.transient_buffers) {
        if (buffer.handle == handle) {
            buffer.byte_size = byte_size;
            buffer.ownership = ownership;
            buffer.debug_label = label_or(std::move(debug_label), "transient-buffer");
            buffer.lifetime.last_used_generation = generation;
            buffer.lifetime.use_count++;
            buffer.lifetime.live = true;
            buffer.lifetime.released_generation = 0;
            resources.buffer_lifetime.reused++;
            return buffer;
        }
    }

    NativeBufferHandle buffer;
    buffer.handle = handle;
    buffer.byte_size = byte_size;
    buffer.ownership = ownership;
    buffer.debug_label = label_or(std::move(debug_label), "transient-buffer");
    buffer.lifetime.created_generation = generation;
    buffer.lifetime.last_used_generation = generation;
    buffer.lifetime.use_count = 1;
    buffer.lifetime.live = true;

    resources.transient_buffers.push_back(std::move(buffer));
    resources.buffer_lifetime.created++;
    resources.buffer_lifetime.live++;
    return resources.transient_buffers.back();
}

NativeImageHandle& ResourceManager::track_transient_image(
        std::uint64_t frame_index,
        std::uint64_t handle,
        std::int32_t width,
        std::int32_t height,
        std::uint32_t format_tag,
        std::string debug_label,
        NativeResourceOwnership ownership) {
    auto& resources = frame(frame_index);
    if (handle == 0) {
        handle = next_placeholder_handle(NativeResourceKind::Image);
    }

    const auto generation = next_resource_generation();
    for (auto& image : resources.transient_images) {
        if (image.handle == handle) {
            image.width = width;
            image.height = height;
            image.format_tag = format_tag;
            image.ownership = ownership;
            image.debug_label = label_or(std::move(debug_label), "transient-image");
            image.lifetime.last_used_generation = generation;
            image.lifetime.use_count++;
            image.lifetime.live = true;
            image.lifetime.released_generation = 0;
            resources.image_lifetime.reused++;
            return image;
        }
    }

    NativeImageHandle image;
    image.handle = handle;
    image.width = width;
    image.height = height;
    image.format_tag = format_tag;
    image.ownership = ownership;
    image.debug_label = label_or(std::move(debug_label), "transient-image");
    image.lifetime.created_generation = generation;
    image.lifetime.last_used_generation = generation;
    image.lifetime.use_count = 1;
    image.lifetime.live = true;

    resources.transient_images.push_back(std::move(image));
    resources.image_lifetime.created++;
    resources.image_lifetime.live++;
    return resources.transient_images.back();
}

ResourceAllocationIntent& ResourceManager::track_buffer_allocation_intent(
        std::uint64_t frame_index,
        std::uint64_t byte_size,
        std::string debug_label,
        NativeResourceIntentStage stage) {
    auto& resources = frame(frame_index);
    const auto generation = next_resource_generation();

    ResourceAllocationIntent intent;
    intent.kind = NativeResourceKind::Buffer;
    intent.stage = stage;
    intent.generation = generation;
    intent.frame_index = frame_index;
    intent.estimated_bytes = byte_size;
    intent.debug_label = label_or(std::move(debug_label), "buffer-allocation-intent");

    resources.allocation_intents.push_back(std::move(intent));
    resources.allocation_intent_counters.recorded++;
    resources.allocation_intent_counters.buffers++;
    resources.allocation_intent_counters.estimated_bytes += byte_size;
    resources.allocation_intent_counters.last_generation = generation;
    return resources.allocation_intents.back();
}

ResourceAllocationIntent& ResourceManager::track_image_allocation_intent(
        std::uint64_t frame_index,
        std::int32_t width,
        std::int32_t height,
        std::uint32_t format_tag,
        std::uint64_t estimated_bytes,
        std::string debug_label,
        NativeResourceIntentStage stage) {
    auto& resources = frame(frame_index);
    const auto generation = next_resource_generation();

    ResourceAllocationIntent intent;
    intent.kind = NativeResourceKind::Image;
    intent.stage = stage;
    intent.generation = generation;
    intent.frame_index = frame_index;
    intent.estimated_bytes = estimated_bytes;
    intent.width = width;
    intent.height = height;
    intent.format_tag = format_tag;
    intent.debug_label = label_or(std::move(debug_label), "image-allocation-intent");

    resources.allocation_intents.push_back(std::move(intent));
    resources.allocation_intent_counters.recorded++;
    resources.allocation_intent_counters.images++;
    resources.allocation_intent_counters.estimated_bytes += estimated_bytes;
    resources.allocation_intent_counters.last_generation = generation;
    return resources.allocation_intents.back();
}

bool ResourceManager::has_context() const {
    return has_context_;
}

std::uint32_t ResourceManager::frames_in_flight() const {
    return static_cast<std::uint32_t>(frames_.size());
}

const BorrowedVulkanContext& ResourceManager::context() const {
    return context_;
}

std::uint64_t ResourceManager::context_adoption_generation() const {
    return context_adoption_generation_;
}

std::uint64_t ResourceManager::context_release_generation() const {
    return context_release_generation_;
}

std::uint64_t ResourceManager::resource_generation() const {
    return resource_generation_;
}

ResourceManagerStats ResourceManager::stats() const {
    ResourceManagerStats result;
    result.has_context = has_context_;
    result.has_active_ring = has_active_ring_;
    result.frames_in_flight = frames_in_flight();
    result.active_ring_index = active_ring_index_;
    result.last_frame_index = last_frame_index_;
    result.context_adoption_generation = context_adoption_generation_;
    result.context_release_generation = context_release_generation_;
    result.resource_generation = resource_generation_;
    result.rings.reserve(frames_.size());

    for (const auto& frame_resources : frames_) {
        auto ring = frame_resources.stats();
        result.transient_buffer_count += ring.transient_buffer_count;
        result.transient_image_count += ring.transient_image_count;
        result.live_buffer_count += ring.live_buffer_count;
        result.live_image_count += ring.live_image_count;
        result.allocation_intent_count += ring.allocation_intent_count;
        result.buffer_lifetime.created += ring.buffer_lifetime.created;
        result.buffer_lifetime.reused += ring.buffer_lifetime.reused;
        result.buffer_lifetime.released += ring.buffer_lifetime.released;
        result.buffer_lifetime.live += ring.buffer_lifetime.live;
        result.image_lifetime.created += ring.image_lifetime.created;
        result.image_lifetime.reused += ring.image_lifetime.reused;
        result.image_lifetime.released += ring.image_lifetime.released;
        result.image_lifetime.live += ring.image_lifetime.live;
        result.allocation_intent_counters.recorded += ring.allocation_intent_counters.recorded;
        result.allocation_intent_counters.buffers += ring.allocation_intent_counters.buffers;
        result.allocation_intent_counters.images += ring.allocation_intent_counters.images;
        result.allocation_intent_counters.estimated_bytes += ring.allocation_intent_counters.estimated_bytes;
        if (ring.allocation_intent_counters.last_generation > result.allocation_intent_counters.last_generation) {
            result.allocation_intent_counters.last_generation = ring.allocation_intent_counters.last_generation;
        }
        result.rings.push_back(ring);
    }

    return result;
}

std::string ResourceManager::status() const {
    const auto resource_stats = stats();
    std::ostringstream out;
    out << "context_state=" << to_string(context_.state)
        << " has_context=" << has_context_
        << " context_label=\"" << context_.debug_label << "\""
        << " context_adopt_generation=" << context_adoption_generation_
        << " context_release_generation=" << context_release_generation_
        << " resource_generation=" << resource_generation_
        << " handles={instance=" << (context_.instance != 0 ? "present" : "missing")
        << ",physical_device=" << (context_.physical_device != 0 ? "present" : "missing")
        << ",device=" << (context_.device != 0 ? "present" : "missing")
        << ",graphics_queue=" << (context_.graphics_queue != 0 ? "present" : "missing")
        << ",queue_family=";
    append_queue_family(out, context_.graphics_queue_family);
    out << "}"
        << " frames_in_flight=" << frames_.size()
        << " ring_stats={active=" << resource_stats.has_active_ring
        << ",active_ring=" << resource_stats.active_ring_index
        << ",last_frame=" << resource_stats.last_frame_index
        << ",transient_buffers=" << resource_stats.transient_buffer_count
        << ",transient_images=" << resource_stats.transient_image_count
        << ",live_buffers=" << resource_stats.live_buffer_count
        << ",live_images=" << resource_stats.live_image_count
        << ",allocation_intents=" << resource_stats.allocation_intent_count
        << ",buffers_created=" << resource_stats.buffer_lifetime.created
        << ",buffers_reused=" << resource_stats.buffer_lifetime.reused
        << ",buffers_released=" << resource_stats.buffer_lifetime.released
        << ",images_created=" << resource_stats.image_lifetime.created
        << ",images_reused=" << resource_stats.image_lifetime.reused
        << ",images_released=" << resource_stats.image_lifetime.released
        << ",intent_recorded=" << resource_stats.allocation_intent_counters.recorded
        << ",intent_buffers=" << resource_stats.allocation_intent_counters.buffers
        << ",intent_images=" << resource_stats.allocation_intent_counters.images
        << ",intent_estimated_bytes=" << resource_stats.allocation_intent_counters.estimated_bytes
        << ",intent_last_generation=" << resource_stats.allocation_intent_counters.last_generation
        << "}"
        << " frame_rings=[";
    for (std::size_t index = 0; index < frames_.size(); index++) {
        if (index != 0) {
            out << "; ";
        }
        out << "{" << frames_[index].status() << "}";
    }
    out << "]";
    return out.str();
}

void ResourceManager::clear_frame_resources() {
    const auto release_generation = next_resource_generation();
    for (auto& frame : frames_) {
        frame.clear_transient(release_generation);
        frame.generation = 0;
        frame.last_frame_index = 0;
        frame.reset_count = 0;
    }
    has_active_ring_ = false;
    active_ring_index_ = 0;
    last_frame_index_ = 0;
}

std::uint64_t ResourceManager::next_resource_generation() {
    resource_generation_++;
    return resource_generation_;
}

std::uint64_t ResourceManager::next_placeholder_handle(NativeResourceKind kind) {
    constexpr std::uint64_t placeholder_bit = 0x8000000000000000ULL;
    constexpr std::uint64_t buffer_kind_bits = 0x0100000000000000ULL;
    constexpr std::uint64_t image_kind_bits = 0x0200000000000000ULL;

    const auto kind_bits = kind == NativeResourceKind::Buffer ? buffer_kind_bits : image_kind_bits;
    const auto handle = placeholder_bit | kind_bits | next_placeholder_handle_;
    next_placeholder_handle_++;
    return handle;
}

} // namespace lucerna
