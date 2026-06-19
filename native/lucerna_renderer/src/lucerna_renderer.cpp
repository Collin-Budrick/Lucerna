#include "lucerna_renderer.hpp"

#include "lucerna_resource_manager.hpp"

#include <cstddef>
#include <sstream>
#include <stdexcept>
#include <utility>

namespace lucerna {

namespace {

constexpr std::uint64_t kEstimatedDirtyRegionUploadBytes = 64;
constexpr std::uint64_t kEstimatedMaterialUploadBytes = 128;
constexpr std::uint64_t kLightingConstantsBytes = 256;
constexpr std::uint32_t kNoopCompositeFormatTag = 1;

} // namespace

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
    frame_index_ = 0;
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    upload_dirty_payload_total_ = 0;
    upload_material_payload_total_ = 0;
    lighting_pass_count_ = 0;
    context_adopt_count_ = 0;
    context_release_count_ = 0;
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
    last_upload_packet_ = {};
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    upload_dirty_payload_total_ = 0;
    upload_material_payload_total_ = 0;
    lighting_pass_count_ = 0;
    context_adopt_count_ = 0;
    context_release_count_ = 0;
    clear_error();
}

void Renderer::resize(std::int32_t width, std::int32_t height) {
    if (!initialized_) {
        return;
    }

    width_ = width < 0 ? 0 : width;
    height_ = height < 0 ? 0 : height;
    resize_count_++;
}

void Renderer::begin_frame(FrameInfo info) {
    if (!initialized_) {
        return;
    }

    frame_index_ = info.frame_index;
    last_tick_delta_ = info.tick_delta;
    if (resources_ != nullptr) {
        resources_->reset_frame(info.frame_index);
    }
    frame_open_ = true;
    begin_frame_count_++;
}

void Renderer::upload_world_deltas(UploadPacket packet) {
    if (!initialized_) {
        return;
    }

    if (packet.dirty_region_count < 0 || packet.material_update_count < 0) {
        set_error("upload delta counts must be non-negative");
        throw std::invalid_argument(last_error_);
    }

    if (packet.first_world_generation > packet.last_world_generation) {
        set_error("upload world generation bounds are invalid");
        throw std::invalid_argument(last_error_);
    }

    if (packet.dirty_regions.size() > static_cast<std::size_t>(packet.dirty_region_count)) {
        set_error("dirty region payload count exceeds advertised count");
        throw std::invalid_argument(last_error_);
    }
    if (packet.material_updates.size() > static_cast<std::size_t>(packet.material_update_count)) {
        set_error("material payload count exceeds advertised count");
        throw std::invalid_argument(last_error_);
    }

    if (!packet.dirty_regions.empty()) {
        std::uint64_t first_generation = packet.dirty_regions.front().generation;
        std::uint64_t last_generation = packet.dirty_regions.front().generation;
        for (const auto& dirty_region : packet.dirty_regions) {
            if (dirty_region.type_id <= 0) {
                set_error("dirty region type id must be positive");
                throw std::invalid_argument(last_error_);
            }
            if (dirty_region.generation == 0) {
                set_error("dirty region generation must be positive");
                throw std::invalid_argument(last_error_);
            }
            if (dirty_region.generation < first_generation) {
                first_generation = dirty_region.generation;
            }
            if (dirty_region.generation > last_generation) {
                last_generation = dirty_region.generation;
            }
        }

        if (packet.first_world_generation != first_generation || packet.last_world_generation != last_generation) {
            set_error("dirty region generations do not match upload bounds");
            throw std::invalid_argument(last_error_);
        }
    }

    for (const auto& material : packet.material_updates) {
        if (material.material_id <= 0) {
            set_error("material id must be positive");
            throw std::invalid_argument(last_error_);
        }
    }

    upload_packet_count_++;
    upload_dirty_payload_total_ += static_cast<std::uint64_t>(packet.dirty_regions.size());
    upload_material_payload_total_ += static_cast<std::uint64_t>(packet.material_updates.size());
    track_upload_staging_placeholder(packet);
    last_upload_packet_ = std::move(packet);
    clear_error();
}

void Renderer::render_lighting() {
    if (!initialized_) {
        return;
    }

    lighting_pass_count_++;
    track_lighting_placeholders();

    // Milestone placeholder: no-op until Lucerna owns real Vulkan render passes.
}

void Renderer::end_frame() {
    if (!initialized_) {
        return;
    }

    if (frame_open_) {
        end_frame_count_++;
    }
    frame_open_ = false;
}

void Renderer::adopt_borrowed_context(BorrowedVulkanContext context) {
    ensure_initialized("adopt borrowed Vulkan context");
    resources_->adopt_context(context);
    context_adopt_count_++;
    clear_error();
}

void Renderer::release_borrowed_context() {
    if (resources_ != nullptr) {
        resources_->release_context();
        context_release_count_++;
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
        << " tick_delta=" << last_tick_delta_
        << " counters={resizes=" << resize_count_
        << ",begin_frames=" << begin_frame_count_
        << ",end_frames=" << end_frame_count_
        << ",upload_packets=" << upload_packet_count_
        << ",lighting_passes=" << lighting_pass_count_
        << ",context_adopts=" << context_adopt_count_
        << ",context_releases=" << context_release_count_
        << "}"
        << " upload_generation=" << last_upload_packet_.generation
        << " dirty_regions=" << last_upload_packet_.dirty_region_count
        << " dirty_region_payloads=" << last_upload_packet_.dirty_regions.size()
        << " material_updates=" << last_upload_packet_.material_update_count
        << " material_payloads=" << last_upload_packet_.material_updates.size()
        << " upload_payload_totals={dirty=" << upload_dirty_payload_total_
        << ",materials=" << upload_material_payload_total_
        << "}"
        << " world_generation=" << last_upload_packet_.first_world_generation << "-" << last_upload_packet_.last_world_generation
        << " material_generation=" << last_upload_packet_.material_generation;
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

std::uint64_t Renderer::estimate_upload_staging_bytes(const UploadPacket& packet) const {
    const auto dirty_bytes = static_cast<std::uint64_t>(packet.dirty_regions.size()) * kEstimatedDirtyRegionUploadBytes;
    const auto material_bytes = static_cast<std::uint64_t>(packet.material_updates.size()) * kEstimatedMaterialUploadBytes;
    return dirty_bytes + material_bytes;
}

void Renderer::track_upload_staging_placeholder(const UploadPacket& packet) {
    if (resources_ == nullptr || !frame_open_) {
        return;
    }

    const auto staging_bytes = estimate_upload_staging_bytes(packet);
    if (staging_bytes == 0) {
        return;
    }

    resources_->track_transient_buffer(frame_index_, 0, staging_bytes, "upload:world-delta-staging");
}

void Renderer::track_lighting_placeholders() {
    if (resources_ == nullptr || !frame_open_ || !resources_->has_context()) {
        return;
    }

    resources_->track_transient_buffer(frame_index_, 0, kLightingConstantsBytes, "render:lighting-constants");
    resources_->track_transient_image(frame_index_, 0, width_, height_, kNoopCompositeFormatTag, "render:noop-composite-target");
}

} // namespace lucerna
