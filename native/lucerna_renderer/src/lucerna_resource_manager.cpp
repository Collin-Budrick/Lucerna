#include "lucerna_resource_manager.hpp"

#include <cstddef>
#include <sstream>
#include <stdexcept>

namespace lucerna {

bool BorrowedVulkanContext::has_required_handles() const {
    return instance != 0
        && physical_device != 0
        && device != 0
        && graphics_queue != 0
        && graphics_queue_family != kInvalidVulkanQueueFamily;
}

void FrameResources::clear_transient() {
    transient_buffers.clear();
    transient_images.clear();
}

ResourceManager::ResourceManager(std::uint32_t frames_in_flight) {
    if (frames_in_flight == 0) {
        throw std::invalid_argument("frames_in_flight must be greater than zero");
    }

    frames_.resize(frames_in_flight);
}

ResourceManager::~ResourceManager() {
    release_context();
}

void ResourceManager::adopt_context(BorrowedVulkanContext context) {
    if (!context.has_required_handles()) {
        throw std::invalid_argument("borrowed Vulkan context is missing required handles");
    }

    clear_frame_resources();
    context_ = context;
    has_context_ = true;
}

void ResourceManager::release_context() {
    clear_frame_resources();
    context_ = {};
    has_context_ = false;
}

FrameResources& ResourceManager::frame(std::uint64_t frame_index) {
    return frames_.at(static_cast<std::size_t>(frame_index % frames_.size()));
}

void ResourceManager::reset_frame(std::uint64_t frame_index) {
    auto& resources = frame(frame_index);
    resources.generation++;
    resources.clear_transient();
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

std::string ResourceManager::status() const {
    std::ostringstream out;
    out << "has_context=" << has_context_
        << " frames=" << frames_.size()
        << " queue_family=" << context_.graphics_queue_family;
    return out.str();
}

void ResourceManager::clear_frame_resources() {
    for (auto& frame : frames_) {
        frame.clear_transient();
        frame.generation = 0;
    }
}

} // namespace lucerna
