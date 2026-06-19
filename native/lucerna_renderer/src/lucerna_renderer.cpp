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

std::size_t pass_index(NativeRenderPass pass) {
    return static_cast<std::size_t>(pass);
}

} // namespace

const char* to_string(NativeRenderPass pass) {
    switch (pass) {
        case NativeRenderPass::FutureGBuffer:
            return "future_gbuffer";
        case NativeRenderPass::NoopLighting:
            return "noop_lighting";
        case NativeRenderPass::FlatComposite:
            return "flat_composite";
    }

    return "unknown";
}

const char* to_string(NativeRenderPassState state) {
    switch (state) {
        case NativeRenderPassState::Inactive:
            return "inactive";
        case NativeRenderPassState::WaitingForFrame:
            return "waiting_for_frame";
        case NativeRenderPassState::WaitingForContext:
            return "waiting_for_context";
        case NativeRenderPassState::Ready:
            return "ready";
        case NativeRenderPassState::Submitted:
            return "submitted";
        case NativeRenderPassState::SkippedInvalidOrder:
            return "skipped_invalid_order";
        case NativeRenderPassState::SkippedNoContext:
            return "skipped_no_context";
        case NativeRenderPassState::NotWired:
            return "not_wired";
    }

    return "unknown";
}

Renderer::Renderer() {
    reset_pass_counters();
}

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
    current_frame_borrowed_context_adopted_ = false;
    current_frame_context_released_ = false;
    current_frame_render_lighting_submitted_ = false;
    current_frame_order_valid_ = true;
    last_frame_borrowed_context_adopted_ = false;
    last_render_lighting_order_valid_ = true;
    last_end_frame_order_valid_ = true;
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
    context_adopted_for_frame_count_ = 0;
    context_released_during_frame_count_ = 0;
    frame_without_context_count_ = 0;
    invalid_begin_frame_order_count_ = 0;
    invalid_render_lighting_order_count_ = 0;
    invalid_end_frame_order_count_ = 0;
    render_lighting_without_frame_count_ = 0;
    render_lighting_without_context_count_ = 0;
    render_lighting_duplicate_count_ = 0;
    end_frame_without_begin_count_ = 0;
    end_frame_without_context_count_ = 0;
    end_frame_without_lighting_count_ = 0;
    reset_pass_counters();
    clear_error();
}

void Renderer::shutdown() {
    if (resources_ != nullptr) {
        resources_->release_context();
    }
    resources_.reset();
    initialized_ = false;
    frame_open_ = false;
    current_frame_borrowed_context_adopted_ = false;
    current_frame_context_released_ = false;
    current_frame_render_lighting_submitted_ = false;
    current_frame_order_valid_ = true;
    last_frame_borrowed_context_adopted_ = false;
    last_render_lighting_order_valid_ = true;
    last_end_frame_order_valid_ = true;
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
    context_adopted_for_frame_count_ = 0;
    context_released_during_frame_count_ = 0;
    frame_without_context_count_ = 0;
    invalid_begin_frame_order_count_ = 0;
    invalid_render_lighting_order_count_ = 0;
    invalid_end_frame_order_count_ = 0;
    render_lighting_without_frame_count_ = 0;
    render_lighting_without_context_count_ = 0;
    render_lighting_duplicate_count_ = 0;
    end_frame_without_begin_count_ = 0;
    end_frame_without_context_count_ = 0;
    end_frame_without_lighting_count_ = 0;
    reset_pass_counters();
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

    const bool began_while_frame_open = frame_open_;
    if (began_while_frame_open) {
        invalid_begin_frame_order_count_++;
    }

    frame_index_ = info.frame_index;
    last_tick_delta_ = info.tick_delta;
    if (resources_ != nullptr) {
        resources_->reset_frame(info.frame_index);
    }
    frame_open_ = true;
    current_frame_borrowed_context_adopted_ = resources_ != nullptr && resources_->has_context();
    current_frame_context_released_ = false;
    current_frame_render_lighting_submitted_ = false;
    current_frame_order_valid_ = !began_while_frame_open && current_frame_borrowed_context_adopted_;
    last_frame_borrowed_context_adopted_ = current_frame_borrowed_context_adopted_;
    if (current_frame_borrowed_context_adopted_) {
        context_adopted_for_frame_count_++;
    } else {
        frame_without_context_count_++;
    }
    prepare_frame_passes();
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

    auto& lighting = pass_counters(NativeRenderPass::NoopLighting);
    auto& composite = pass_counters(NativeRenderPass::FlatComposite);
    lighting.attempts++;
    lighting.last_frame_index = frame_index_;
    composite.attempts++;
    composite.last_frame_index = frame_index_;

    const bool has_context = resources_ != nullptr && resources_->has_context();
    if (!frame_open_) {
        last_render_lighting_order_valid_ = false;
        current_frame_order_valid_ = false;
        invalid_render_lighting_order_count_++;
        render_lighting_without_frame_count_++;
        mark_pass_skipped(NativeRenderPass::NoopLighting, NativeRenderPassState::SkippedInvalidOrder, false);
        mark_pass_skipped(NativeRenderPass::FlatComposite, NativeRenderPassState::SkippedInvalidOrder, false);
        return;
    }

    if (!current_frame_borrowed_context_adopted_ || !has_context) {
        last_render_lighting_order_valid_ = false;
        current_frame_order_valid_ = false;
        invalid_render_lighting_order_count_++;
        render_lighting_without_context_count_++;
        mark_pass_skipped(NativeRenderPass::NoopLighting, NativeRenderPassState::SkippedNoContext, true);
        mark_pass_skipped(NativeRenderPass::FlatComposite, NativeRenderPassState::SkippedNoContext, true);
        return;
    }

    if (current_frame_render_lighting_submitted_) {
        last_render_lighting_order_valid_ = false;
        current_frame_order_valid_ = false;
        invalid_render_lighting_order_count_++;
        render_lighting_duplicate_count_++;
        mark_pass_skipped(NativeRenderPass::NoopLighting, NativeRenderPassState::SkippedInvalidOrder, false);
        mark_pass_skipped(NativeRenderPass::FlatComposite, NativeRenderPassState::SkippedInvalidOrder, false);
        return;
    }

    last_render_lighting_order_valid_ = true;
    current_frame_render_lighting_submitted_ = true;
    lighting_pass_count_++;
    mark_pass_submitted(NativeRenderPass::NoopLighting, track_noop_lighting_placeholder());
    mark_pass_submitted(NativeRenderPass::FlatComposite, track_flat_composite_placeholder());

    // Milestone placeholder: no-op until Lucerna owns real Vulkan render passes.
}

void Renderer::end_frame() {
    if (!initialized_) {
        return;
    }

    if (!frame_open_) {
        last_end_frame_order_valid_ = false;
        current_frame_order_valid_ = false;
        invalid_end_frame_order_count_++;
        end_frame_without_begin_count_++;
        return;
    }

    const bool has_context = resources_ != nullptr && resources_->has_context();
    bool valid_order = true;
    if (!current_frame_borrowed_context_adopted_ || !has_context) {
        valid_order = false;
        end_frame_without_context_count_++;
    }
    if (!current_frame_render_lighting_submitted_) {
        valid_order = false;
        end_frame_without_lighting_count_++;
    }

    last_end_frame_order_valid_ = valid_order;
    if (!valid_order) {
        current_frame_order_valid_ = false;
        invalid_end_frame_order_count_++;
    }
    end_frame_count_++;
    frame_open_ = false;
}

void Renderer::adopt_borrowed_context(BorrowedVulkanContext context) {
    ensure_initialized("adopt borrowed Vulkan context");
    resources_->adopt_context(context);
    context_adopt_count_++;
    if (frame_open_ && !current_frame_borrowed_context_adopted_) {
        current_frame_borrowed_context_adopted_ = true;
        last_frame_borrowed_context_adopted_ = true;
        context_adopted_for_frame_count_++;
    }
    clear_error();
}

void Renderer::release_borrowed_context() {
    if (frame_open_ && current_frame_borrowed_context_adopted_) {
        current_frame_context_released_ = true;
        current_frame_order_valid_ = false;
        context_released_during_frame_count_++;
    }
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
    const bool has_context = resources_ != nullptr && resources_->has_context();
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
        << ",context_adopted_for_frames=" << context_adopted_for_frame_count_
        << "}"
        << " frame_validity={has_context=" << has_context
        << ",current_frame_context_adopted=" << (frame_open_ && current_frame_borrowed_context_adopted_)
        << ",last_frame_context_adopted=" << last_frame_borrowed_context_adopted_
        << ",context_released_during_frame=" << current_frame_context_released_
        << ",render_lighting_submitted=" << current_frame_render_lighting_submitted_
        << ",frame_order_valid=" << current_frame_order_valid_
        << ",last_render_lighting_order_valid=" << last_render_lighting_order_valid_
        << ",last_end_frame_order_valid=" << last_end_frame_order_valid_
        << "}"
        << " order_counters={invalid_begin_frames=" << invalid_begin_frame_order_count_
        << ",invalid_render_lighting=" << invalid_render_lighting_order_count_
        << ",invalid_end_frames=" << invalid_end_frame_order_count_
        << ",frames_without_context=" << frame_without_context_count_
        << ",render_lighting_without_frame=" << render_lighting_without_frame_count_
        << ",render_lighting_without_context=" << render_lighting_without_context_count_
        << ",render_lighting_duplicates=" << render_lighting_duplicate_count_
        << ",end_frame_without_begin=" << end_frame_without_begin_count_
        << ",end_frame_without_context=" << end_frame_without_context_count_
        << ",end_frame_without_lighting=" << end_frame_without_lighting_count_
        << ",context_released_during_frame=" << context_released_during_frame_count_
        << "}"
        << " passes=[";
    for (std::size_t index = 0; index < pass_counters_.size(); index++) {
        if (index != 0) {
            out << "; ";
        }
        const auto& counters = pass_counters_[index];
        out << "{id=" << to_string(counters.pass)
            << ",state=" << to_string(counters.state)
            << ",attempts=" << counters.attempts
            << ",submitted=" << counters.submitted
            << ",skipped=" << counters.skipped
            << ",invalid_order=" << counters.invalid_order
            << ",missing_context=" << counters.missing_context
            << ",not_wired=" << counters.not_wired
            << ",placeholder_resources=" << counters.placeholder_resources
            << ",last_frame=" << counters.last_frame_index
            << ",expected_this_frame=" << counters.expected_this_frame
            << ",submitted_this_frame=" << counters.submitted_this_frame
            << "}";
    }
    out << "]"
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
        const auto resource_stats = resources_->stats();
        out << " resource_ring_stats={frames_in_flight=" << resource_stats.frames_in_flight
            << ",active=" << resource_stats.has_active_ring
            << ",active_ring=" << resource_stats.active_ring_index
            << ",last_frame=" << resource_stats.last_frame_index
            << ",transient_buffers=" << resource_stats.transient_buffer_count
            << ",transient_images=" << resource_stats.transient_image_count
            << ",live_buffers=" << resource_stats.live_buffer_count
            << ",live_images=" << resource_stats.live_image_count
            << ",buffers_created=" << resource_stats.buffer_lifetime.created
            << ",buffers_reused=" << resource_stats.buffer_lifetime.reused
            << ",buffers_released=" << resource_stats.buffer_lifetime.released
            << ",images_created=" << resource_stats.image_lifetime.created
            << ",images_reused=" << resource_stats.image_lifetime.reused
            << ",images_released=" << resource_stats.image_lifetime.released
            << "}";
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

std::uint64_t Renderer::track_noop_lighting_placeholder() {
    if (resources_ == nullptr || !frame_open_ || !resources_->has_context()) {
        return 0;
    }

    resources_->track_transient_buffer(frame_index_, 0, kLightingConstantsBytes, "render:lighting-constants");
    return 1;
}

std::uint64_t Renderer::track_flat_composite_placeholder() {
    if (resources_ == nullptr || !frame_open_ || !resources_->has_context()) {
        return 0;
    }

    resources_->track_transient_image(frame_index_, 0, width_, height_, kNoopCompositeFormatTag, "render:noop-composite-target");
    return 1;
}

void Renderer::reset_pass_counters() {
    pass_counters_[pass_index(NativeRenderPass::FutureGBuffer)] = NativeRenderPassCounters{
        NativeRenderPass::FutureGBuffer,
        NativeRenderPassState::WaitingForFrame
    };
    pass_counters_[pass_index(NativeRenderPass::NoopLighting)] = NativeRenderPassCounters{
        NativeRenderPass::NoopLighting,
        NativeRenderPassState::WaitingForFrame
    };
    pass_counters_[pass_index(NativeRenderPass::FlatComposite)] = NativeRenderPassCounters{
        NativeRenderPass::FlatComposite,
        NativeRenderPassState::WaitingForFrame
    };
}

void Renderer::prepare_frame_passes() {
    for (auto& counters : pass_counters_) {
        counters.expected_this_frame = true;
        counters.submitted_this_frame = false;
        counters.last_frame_index = frame_index_;
        counters.state = current_frame_borrowed_context_adopted_
            ? NativeRenderPassState::Ready
            : NativeRenderPassState::WaitingForContext;
    }

    mark_pass_not_wired(NativeRenderPass::FutureGBuffer);
}

void Renderer::mark_pass_not_wired(NativeRenderPass pass) {
    auto& counters = pass_counters(pass);
    counters.state = NativeRenderPassState::NotWired;
    counters.skipped++;
    counters.not_wired++;
    counters.last_frame_index = frame_index_;
}

void Renderer::mark_pass_submitted(NativeRenderPass pass, std::uint64_t placeholder_resources) {
    auto& counters = pass_counters(pass);
    counters.state = NativeRenderPassState::Submitted;
    counters.submitted++;
    counters.placeholder_resources += placeholder_resources;
    counters.last_frame_index = frame_index_;
    counters.expected_this_frame = true;
    counters.submitted_this_frame = true;
}

void Renderer::mark_pass_skipped(NativeRenderPass pass, NativeRenderPassState state, bool missing_context) {
    auto& counters = pass_counters(pass);
    counters.state = state;
    counters.skipped++;
    if (state == NativeRenderPassState::SkippedInvalidOrder) {
        counters.invalid_order++;
    }
    if (missing_context) {
        counters.missing_context++;
    }
    counters.last_frame_index = frame_index_;
    counters.expected_this_frame = frame_open_;
    counters.submitted_this_frame = false;
}

NativeRenderPassCounters& Renderer::pass_counters(NativeRenderPass pass) {
    return pass_counters_.at(pass_index(pass));
}

const NativeRenderPassCounters& Renderer::pass_counters(NativeRenderPass pass) const {
    return pass_counters_.at(pass_index(pass));
}

} // namespace lucerna
