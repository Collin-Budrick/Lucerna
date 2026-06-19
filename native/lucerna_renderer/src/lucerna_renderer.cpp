#include "lucerna_renderer.hpp"

#include "lucerna_resource_manager.hpp"

#include <sstream>
#include <stdexcept>
#include <utility>

namespace lucerna {

Renderer::Renderer() = default;

Renderer::~Renderer() {
    shutdown();
}

void Renderer::init() {
    if (initialized_) {
        return;
    }

    resources_ = std::make_unique<ResourceManager>(3);
    initialized_ = true;
    frame_open_ = false;
    clear_error();
}

void Renderer::shutdown() {
    if (resources_ != nullptr) {
        resources_->release_context();
    }
    resources_.reset();
    initialized_ = false;
    frame_open_ = false;
    width_ = 0;
    height_ = 0;
    frame_index_ = 0;
    last_upload_generation_ = 0;
    clear_error();
}

void Renderer::resize(std::int32_t width, std::int32_t height) {
    if (!initialized_) {
        return;
    }

    width_ = width < 0 ? 0 : width;
    height_ = height < 0 ? 0 : height;
}

void Renderer::begin_frame(FrameInfo info) {
    if (!initialized_) {
        return;
    }

    frame_index_ = info.frame_index;
    if (resources_ != nullptr) {
        resources_->reset_frame(info.frame_index);
    }
    frame_open_ = true;
}

void Renderer::upload_world_deltas(UploadSummary summary) {
    if (!initialized_) {
        return;
    }

    if (summary.dirty_region_count < 0 || summary.material_update_count < 0) {
        set_error("upload delta counts must be non-negative");
        throw std::invalid_argument(last_error_);
    }

    last_upload_generation_ = summary.generation;
}

void Renderer::render_lighting() {
    if (!initialized_) {
        return;
    }

    // Milestone placeholder: no-op until Lucerna owns real Vulkan render passes.
}

void Renderer::end_frame() {
    frame_open_ = false;
}

void Renderer::adopt_borrowed_context(BorrowedVulkanContext context) {
    ensure_initialized("adopt borrowed Vulkan context");
    resources_->adopt_context(context);
    clear_error();
}

void Renderer::release_borrowed_context() {
    if (resources_ != nullptr) {
        resources_->release_context();
    }
    clear_error();
}

bool Renderer::initialized() const {
    return initialized_;
}

std::string Renderer::last_error() const {
    return last_error_;
}

std::string Renderer::status() const {
    std::ostringstream out;
    out << "initialized=" << initialized_
        << " size=" << width_ << "x" << height_
        << " frame=" << frame_index_
        << " frame_open=" << frame_open_
        << " upload_generation=" << last_upload_generation_;
    if (resources_ != nullptr) {
        out << " resources={" << resources_->status() << "}";
    } else {
        out << " resources=absent";
    }
    if (!last_error_.empty()) {
        out << " last_error=\"" << last_error_ << "\"";
    }
    return out.str();
}

void Renderer::ensure_initialized(const char* operation) const {
    if (!initialized_ || resources_ == nullptr) {
        throw std::logic_error(std::string("renderer is not initialized for ") + operation);
    }
}

void Renderer::clear_error() {
    last_error_.clear();
}

void Renderer::set_error(std::string error) {
    last_error_ = std::move(error);
}

} // namespace lucerna
