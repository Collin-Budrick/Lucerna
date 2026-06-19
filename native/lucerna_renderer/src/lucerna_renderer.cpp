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
constexpr std::uint64_t kEstimatedSectionMetadataBytes = 96;
constexpr std::uint64_t kEstimatedSectionSnapshotMetadataBytes = 160;
constexpr std::uint64_t kSectionVoxelCount = 16 * 16 * 16;
constexpr std::uint64_t kVoxelOccupancyWordCount = kSectionVoxelCount / 64;
constexpr std::uint64_t kVoxelOccupancyBytesPerSection = kVoxelOccupancyWordCount * sizeof(std::uint64_t);
constexpr std::uint64_t kVoxelMaterialIndexBytesPerSection = kSectionVoxelCount * sizeof(std::uint16_t);
constexpr std::uint64_t kSectionEmissiveEntryBytes =
        (sizeof(std::int32_t) * 3) + sizeof(std::uint64_t);
constexpr std::uint64_t kLightingConstantsBytes = 256;
constexpr std::uint32_t kNoopCompositeFormatTag = 1;
constexpr std::uint32_t kGBufferDepthFormatTag = 10;
constexpr std::uint32_t kGBufferNormalMaterialFormatTag = 11;
constexpr std::uint32_t kGBufferAlbedoEmissiveFormatTag = 12;
constexpr std::uint32_t kGBufferMotionHistoryFormatTag = 13;
constexpr std::uint32_t kGBufferReactiveMaskFormatTag = 14;
constexpr std::int32_t kMaxGBufferDimension = 32768;
constexpr std::int32_t kMaxGBufferAttachments = 32;
constexpr std::int32_t kMaxGBufferSamples = 64;
constexpr std::int32_t kLucernaGBufferMainPassId = 100;
constexpr const char* kLucernaGBufferMainPassName = "lucerna.gbuffer.main";
constexpr std::int32_t kMaxLightingDispatchDimension = 32768;
constexpr std::int32_t kMaxLightingDispatchGroups = 1048576;
constexpr std::int32_t kMaxLightingWorkgroupSize = 1024;
constexpr std::int32_t kMaxLightingIoCount = 64;
constexpr std::int32_t kMaxLightingSamples = 4096;
constexpr std::int32_t kMaxLightingRays = 16777216;
constexpr std::int32_t kMaxLightingCacheRecords = 16777216;
constexpr std::uint64_t kEstimatedLightingDispatchGroupBytes = 64;

std::size_t pass_index(NativeRenderPass pass) {
    return static_cast<std::size_t>(pass);
}

std::size_t lighting_stage_index(NativeLightingDispatchStage stage) {
    return static_cast<std::size_t>(stage);
}

bool is_valid_pass_id(std::int32_t pass_id) {
    return pass_id >= 0 && static_cast<std::size_t>(pass_id) < kNativeRenderPassCount;
}

NativeRenderPass pass_from_id(std::int32_t pass_id) {
    return static_cast<NativeRenderPass>(pass_id);
}

NativeResourceIntentStage resource_stage_for_lighting_stage(NativeLightingDispatchStage stage) {
    switch (stage) {
        case NativeLightingDispatchStage::DirectLighting:
            return NativeResourceIntentStage::DirectLighting;
        case NativeLightingDispatchStage::DiffuseGi:
            return NativeResourceIntentStage::DiffuseGi;
        case NativeLightingDispatchStage::Denoise:
            return NativeResourceIntentStage::Denoise;
        case NativeLightingDispatchStage::Composite:
            return NativeResourceIntentStage::Composite;
        case NativeLightingDispatchStage::Cache:
            return NativeResourceIntentStage::LightingCache;
    }

    return NativeResourceIntentStage::DirectLighting;
}

bool is_power_of_two(std::int32_t value) {
    return value > 0 && (value & (value - 1)) == 0;
}

bool is_blank(const std::string& value) {
    return value.find_first_not_of(" \t\n\r\f\v") == std::string::npos;
}

std::uint32_t estimate_bytes_per_pixel(std::int32_t format_tag) {
    switch (static_cast<std::uint32_t>(format_tag)) {
        case kGBufferDepthFormatTag:
            return 4;
        case kGBufferNormalMaterialFormatTag:
            return 4;
        case kGBufferAlbedoEmissiveFormatTag:
            return 8;
        case kGBufferMotionHistoryFormatTag:
            return 8;
        case kGBufferReactiveMaskFormatTag:
            return 1;
        default:
            return format_tag > 0 ? 4 : 0;
    }
}

std::uint64_t max_generation(
        std::uint64_t first,
        std::uint64_t second,
        std::uint64_t third,
        std::uint64_t fourth,
        std::uint64_t fifth) {
    std::uint64_t result = first;
    if (second > result) {
        result = second;
    }
    if (third > result) {
        result = third;
    }
    if (fourth > result) {
        result = fourth;
    }
    if (fifth > result) {
        result = fifth;
    }
    return result;
}

} // namespace

std::uint64_t SectionSnapshotUpload::combined_generation() const {
    return max_generation(
            section_generation,
            material_generation,
            occupancy_generation,
            emissive_generation,
            dirty_region.generation);
}

bool SectionSnapshotUpload::has_section_payload() const {
    return occupied_voxel_count > 0
        || occupancy_mask_word_count > 0
        || !material_palette_ids.empty()
        || !emissive_entries.empty();
}

const char* to_string(NativeRenderPass pass) {
    switch (pass) {
        case NativeRenderPass::FutureGBuffer:
            return kLucernaGBufferMainPassName;
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

const char* to_string(NativeLightingDispatchStage stage) {
    switch (stage) {
        case NativeLightingDispatchStage::DirectLighting:
            return "direct_lighting";
        case NativeLightingDispatchStage::DiffuseGi:
            return "diffuse_gi";
        case NativeLightingDispatchStage::Denoise:
            return "denoise";
        case NativeLightingDispatchStage::Composite:
            return "composite";
        case NativeLightingDispatchStage::Cache:
            return "cache";
    }

    return "unknown";
}

Renderer::Renderer() {
    reset_pass_counters();
    reset_staging_telemetry();
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
    last_section_upload_packet_ = {};
    last_gbuffer_staging_packet_ = {};
    last_lighting_dispatch_packet_ = {};
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    section_upload_packet_count_ = 0;
    gbuffer_staging_packet_count_ = 0;
    lighting_dispatch_packet_count_ = 0;
    upload_dirty_payload_total_ = 0;
    upload_material_payload_total_ = 0;
    section_snapshot_payload_total_ = 0;
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
    reset_staging_telemetry();
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
    last_section_upload_packet_ = {};
    last_gbuffer_staging_packet_ = {};
    last_lighting_dispatch_packet_ = {};
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    section_upload_packet_count_ = 0;
    gbuffer_staging_packet_count_ = 0;
    lighting_dispatch_packet_count_ = 0;
    upload_dirty_payload_total_ = 0;
    upload_material_payload_total_ = 0;
    section_snapshot_payload_total_ = 0;
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
    reset_staging_telemetry();
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

void Renderer::upload_section_snapshots(SectionUploadPacket packet) {
    if (!initialized_) {
        return;
    }

    auto fail = [this](std::string error) {
        set_error(std::move(error));
        throw std::invalid_argument(last_error_);
    };
    auto require_text = [&fail](const std::string& value, const char* name) {
        if (is_blank(value)) {
            fail(std::string(name) + " must not be blank");
        }
    };
    auto require_voxel_count = [&fail](std::int32_t value, const char* name) {
        if (value < 0 || static_cast<std::uint64_t>(value) > kSectionVoxelCount) {
            std::ostringstream error;
            error << name << " must be between 0 and " << kSectionVoxelCount;
            fail(error.str());
        }
    };

    if (packet.section_snapshot_count < 0) {
        fail("section snapshot count must be non-negative");
    }
    if (packet.first_section_snapshot_generation > packet.last_section_snapshot_generation) {
        fail("section snapshot generation bounds are invalid");
    }
    if (packet.snapshots.size() > static_cast<std::size_t>(packet.section_snapshot_count)) {
        fail("section snapshot payload count exceeds advertised count");
    }
    if (packet.snapshots.empty()
            && (packet.first_section_snapshot_generation != 0 || packet.last_section_snapshot_generation != 0)) {
        fail("empty section snapshot payload must use zero section generation bounds");
    }
    if (packet.section_snapshot_count == 0 && !packet.snapshots.empty()) {
        fail("section snapshot payload count requires a positive advertised count");
    }

    std::uint64_t first_combined_generation = 0;
    std::uint64_t last_combined_generation = 0;
    std::uint64_t max_section_generation = 0;
    std::uint64_t max_material_generation = 0;
    std::uint64_t max_occupancy_generation = 0;
    std::uint64_t max_emissive_generation = 0;
    std::uint64_t max_dirty_region_generation = 0;

    for (const auto& snapshot : packet.snapshots) {
        require_text(snapshot.dimension, "section dimension");
        require_text(snapshot.dirty_region.type_name, "dirty region type name");
        require_text(snapshot.dirty_region.dimension, "dirty region dimension");
        require_text(snapshot.occupancy_bit_order_name, "occupancy bit order name");

        if (snapshot.dirty_region.type_id <= 0) {
            fail("section dirty region type id must be positive");
        }
        if (snapshot.dirty_region.generation == 0) {
            fail("section dirty region generation must be positive");
        }
        if (snapshot.dirty_region.section_scoped
                && (snapshot.dirty_region.dimension != snapshot.dimension
                    || snapshot.dirty_region.section_x != snapshot.section_x
                    || snapshot.dirty_region.section_y != snapshot.section_y
                    || snapshot.dirty_region.section_z != snapshot.section_z)) {
            fail("section dirty region handoff must match the section origin");
        }

        require_voxel_count(snapshot.occupied_voxel_count, "occupied voxel count");
        require_voxel_count(snapshot.opaque_voxel_count, "opaque voxel count");
        require_voxel_count(snapshot.translucent_voxel_count, "translucent voxel count");
        require_voxel_count(snapshot.fluid_voxel_count, "fluid voxel count");
        require_voxel_count(snapshot.emissive_voxel_count, "emissive voxel count");
        if (snapshot.opaque_voxel_count + snapshot.translucent_voxel_count > snapshot.occupied_voxel_count) {
            fail("opaque and translucent voxel counts cannot exceed occupied voxel count");
        }
        if (snapshot.fluid_voxel_count > snapshot.occupied_voxel_count) {
            fail("fluid voxel count cannot exceed occupied voxel count");
        }
        if (snapshot.emissive_voxel_count > snapshot.occupied_voxel_count) {
            fail("emissive voxel count cannot exceed occupied voxel count");
        }

        if (snapshot.occupancy_bit_order_id <= 0) {
            fail("occupancy bit order id must be positive");
        }
        if (snapshot.occupancy_mask_word_offset < 0) {
            fail("occupancy mask word offset must be non-negative");
        }
        if (snapshot.occupancy_mask_word_count < 0
                || static_cast<std::uint64_t>(snapshot.occupancy_mask_word_count) > kVoxelOccupancyWordCount) {
            std::ostringstream error;
            error << "occupancy mask word count must be between 0 and " << kVoxelOccupancyWordCount;
            fail(error.str());
        }
        if (snapshot.occupancy_mask_bit_count < 0
                || static_cast<std::uint64_t>(snapshot.occupancy_mask_bit_count) > kSectionVoxelCount) {
            std::ostringstream error;
            error << "occupancy mask bit count must be between 0 and " << kSectionVoxelCount;
            fail(error.str());
        }
        if (snapshot.occupancy_mask_bit_count > snapshot.occupancy_mask_word_count * 64) {
            fail("occupancy mask bit count cannot exceed the occupancy mask word capacity");
        }
        if (snapshot.occupancy_generation < snapshot.occupancy_mask_generation) {
            fail("section occupancy generation must include the occupancy mask generation");
        }

        if (snapshot.material_palette_offset < 0) {
            fail("material palette offset must be non-negative");
        }
        if (snapshot.material_generation < snapshot.material_palette_generation) {
            fail("section material generation must include the material palette generation");
        }
        for (const auto material_id : snapshot.material_palette_ids) {
            if (material_id <= 0) {
                fail("material palette ids must be positive");
            }
        }

        if (snapshot.emissive_entries.size() > static_cast<std::size_t>(snapshot.emissive_voxel_count)) {
            fail("emissive entry payload count cannot exceed emissive voxel count");
        }
        std::uint64_t max_emissive_entry_generation = 0;
        for (const auto& emissive : snapshot.emissive_entries) {
            if (emissive.voxel_index < 0 || static_cast<std::uint64_t>(emissive.voxel_index) >= kSectionVoxelCount) {
                fail("emissive voxel indices must be section voxel indices");
            }
            if (emissive.material_id <= 0) {
                fail("emissive material ids must be positive");
            }
            if (emissive.block_light_level < 0 || emissive.block_light_level > 15) {
                fail("emissive block light levels must be between 0 and 15");
            }
            if (emissive.generation > max_emissive_entry_generation) {
                max_emissive_entry_generation = emissive.generation;
            }
        }
        if (snapshot.emissive_generation < max_emissive_entry_generation) {
            fail("section emissive generation must include all emissive entries");
        }

        const auto combined_generation = snapshot.combined_generation();
        if (first_combined_generation == 0 || combined_generation < first_combined_generation) {
            first_combined_generation = combined_generation;
        }
        if (combined_generation > last_combined_generation) {
            last_combined_generation = combined_generation;
        }
        if (snapshot.section_generation > max_section_generation) {
            max_section_generation = snapshot.section_generation;
        }
        if (snapshot.material_generation > max_material_generation) {
            max_material_generation = snapshot.material_generation;
        }
        if (snapshot.occupancy_generation > max_occupancy_generation) {
            max_occupancy_generation = snapshot.occupancy_generation;
        }
        if (snapshot.emissive_generation > max_emissive_generation) {
            max_emissive_generation = snapshot.emissive_generation;
        }
        if (snapshot.dirty_region.generation > max_dirty_region_generation) {
            max_dirty_region_generation = snapshot.dirty_region.generation;
        }
    }

    if (!packet.snapshots.empty()) {
        if (packet.first_section_snapshot_generation != first_combined_generation
                || packet.last_section_snapshot_generation != last_combined_generation) {
            fail("section snapshot generations do not match upload bounds");
        }
        if (packet.section_generation < max_section_generation) {
            fail("section generation does not include the section snapshot payload");
        }
        if (packet.section_material_generation < max_material_generation) {
            fail("section material generation does not include the section snapshot payload");
        }
        if (packet.section_occupancy_generation < max_occupancy_generation) {
            fail("section occupancy generation does not include the section snapshot payload");
        }
        if (packet.section_emissive_generation < max_emissive_generation) {
            fail("section emissive generation does not include the section snapshot payload");
        }
        if (packet.section_dirty_region_generation < max_dirty_region_generation) {
            fail("section dirty region generation does not include the section snapshot payload");
        }
        if (packet.generation < max_generation(
                max_section_generation,
                max_material_generation,
                max_occupancy_generation,
                max_emissive_generation,
                max_dirty_region_generation)) {
            fail("section upload generation does not include the section snapshot payload");
        }
    }

    section_upload_packet_count_++;
    section_snapshot_payload_total_ += static_cast<std::uint64_t>(packet.snapshots.size());
    track_section_snapshot_staging_placeholder(packet);
    last_section_upload_packet_ = std::move(packet);
    clear_error();
}

void Renderer::upload_gbuffer_staging(GBufferStagingPacket packet) {
    if (!initialized_) {
        return;
    }

    auto fail = [this](std::string error) {
        set_error(std::move(error));
        throw std::invalid_argument(last_error_);
    };
    auto require_text = [&fail](const std::string& value, const char* name) {
        if (is_blank(value)) {
            fail(std::string(name) + " must not be blank");
        }
    };
    auto require_dimension = [&fail](std::int32_t value, const char* name) {
        if (value <= 0 || value > kMaxGBufferDimension) {
            std::ostringstream error;
            error << name << " must be between 1 and " << kMaxGBufferDimension;
            fail(error.str());
        }
    };
    auto require_optional_dimension = [&fail](std::int32_t value, const char* name) {
        if (value < 0 || value > kMaxGBufferDimension) {
            std::ostringstream error;
            error << name << " must be between 0 and " << kMaxGBufferDimension;
            fail(error.str());
        }
    };

    if (packet.gbuffer_count < 0) {
        fail("gbuffer count must be non-negative");
    }
    if (packet.first_gbuffer_generation > packet.last_gbuffer_generation) {
        fail("gbuffer generation bounds are invalid");
    }
    if (packet.gbuffers.size() > static_cast<std::size_t>(packet.gbuffer_count)) {
        fail("gbuffer payload count exceeds advertised count");
    }
    if (packet.gbuffers.empty()
            && (packet.first_gbuffer_generation != 0 || packet.last_gbuffer_generation != 0)) {
        fail("empty gbuffer staging payload must use zero gbuffer generation bounds");
    }
    if (packet.gbuffer_count == 0 && !packet.gbuffers.empty()) {
        fail("gbuffer staging payload count requires a positive advertised count");
    }

    for (const auto& upload : packet.gbuffers) {
        require_text(upload.pass_id, "gbuffer pass id");
        if (upload.numeric_pass_id != kLucernaGBufferMainPassId) {
            fail("numeric gbuffer pass id must be lucerna.gbuffer.main");
        }
        if (upload.pass_id != kLucernaGBufferMainPassName) {
            fail("gbuffer staging uploads must target lucerna.gbuffer.main");
        }

        require_dimension(upload.width, "gbuffer width");
        require_dimension(upload.height, "gbuffer height");

        if (upload.attachment_count < 0) {
            fail("gbuffer attachment count must be non-negative");
        }
        if (upload.attachment_count > kMaxGBufferAttachments) {
            std::ostringstream error;
            error << "gbuffer attachment count must be at most " << kMaxGBufferAttachments;
            fail(error.str());
        }
        if (upload.attachments.size() != static_cast<std::size_t>(upload.attachment_count)) {
            fail("gbuffer attachment payload count must match the advertised attachment count");
        }

        for (const auto& attachment : upload.attachments) {
            require_text(attachment.name, "gbuffer attachment name");
            if (attachment.format_tag < 0) {
                fail("gbuffer attachment format must be non-negative");
            }
            if (!is_power_of_two(attachment.samples) || attachment.samples > kMaxGBufferSamples) {
                std::ostringstream error;
                error << "gbuffer attachment samples must be a power of two between 1 and " << kMaxGBufferSamples;
                fail(error.str());
            }

            if (attachment.enabled) {
                if (attachment.format_tag == 0) {
                    fail("enabled gbuffer attachment format must be positive");
                }
                require_dimension(attachment.width, "gbuffer attachment width");
                require_dimension(attachment.height, "gbuffer attachment height");
                if (attachment.width > upload.width || attachment.height > upload.height) {
                    fail("gbuffer attachment resolution cannot exceed the gbuffer resolution");
                }
            } else {
                require_optional_dimension(attachment.width, "disabled gbuffer attachment width");
                require_optional_dimension(attachment.height, "disabled gbuffer attachment height");
            }
        }
    }

    if (!packet.gbuffers.empty()) {
        if (packet.generation == 0) {
            fail("gbuffer staging generation must be positive when payload is present");
        }
        if (packet.first_gbuffer_generation == 0 || packet.last_gbuffer_generation == 0) {
            fail("gbuffer staging generation bounds must be positive when payload is present");
        }
        if (packet.generation < packet.last_gbuffer_generation) {
            fail("gbuffer staging generation must include the gbuffer generation bounds");
        }
    }

    gbuffer_staging_packet_count_++;
    track_gbuffer_staging_upload(packet);
    last_gbuffer_staging_packet_ = std::move(packet);
    clear_error();
}

void Renderer::upload_lighting_dispatch(LightingDispatchPacket packet) {
    if (!initialized_) {
        return;
    }

    auto fail = [this](std::string error) {
        set_error(std::move(error));
        throw std::invalid_argument(last_error_);
    };
    auto require_text = [&fail](const std::string& value, const char* name) {
        if (is_blank(value)) {
            fail(std::string(name) + " must not be blank");
        }
    };
    auto require_range = [&fail](std::int32_t value, std::int32_t minimum, std::int32_t maximum, const char* name) {
        if (value < minimum || value > maximum) {
            std::ostringstream error;
            error << name << " must be between " << minimum << " and " << maximum;
            fail(error.str());
        }
    };

    if (packet.dispatch_count < 0) {
        fail("lighting dispatch count must be non-negative");
    }
    if (packet.first_dispatch_generation > packet.last_dispatch_generation) {
        fail("lighting dispatch generation bounds are invalid");
    }
    if (packet.dispatches.empty()) {
        if (packet.dispatch_count != 0) {
            fail("empty lighting dispatch payload must advertise zero dispatches");
        }
        if (packet.first_dispatch_generation != 0 || packet.last_dispatch_generation != 0) {
            fail("empty lighting dispatch payload must use zero dispatch generation bounds");
        }
    } else {
        if (packet.dispatch_count != static_cast<std::int32_t>(kNativeLightingDispatchStageCount)) {
            fail("lighting dispatch packet must advertise direct, GI, denoise, composite, and cache stages");
        }
        if (packet.dispatches.size() != kNativeLightingDispatchStageCount) {
            fail("lighting dispatch payload must include direct, GI, denoise, composite, and cache metadata");
        }
    }

    std::array<bool, kNativeLightingDispatchStageCount> seen_stages{};
    std::uint64_t first_generation = 0;
    std::uint64_t last_generation = 0;

    for (const auto& dispatch : packet.dispatches) {
        const auto stage_index = lighting_stage_index(dispatch.stage);
        if (stage_index >= kNativeLightingDispatchStageCount) {
            fail("lighting dispatch stage id is invalid");
        }
        if (seen_stages[stage_index]) {
            fail("lighting dispatch stages must not contain duplicates");
        }
        seen_stages[stage_index] = true;

        require_text(dispatch.stage_name, "lighting dispatch stage name");
        if (dispatch.stage_name != to_string(dispatch.stage)) {
            fail("lighting dispatch stage name must match the stage id");
        }
        if (dispatch.generation == 0) {
            fail("lighting dispatch stage generation must be positive");
        }
        if (first_generation == 0 || dispatch.generation < first_generation) {
            first_generation = dispatch.generation;
        }
        if (dispatch.generation > last_generation) {
            last_generation = dispatch.generation;
        }

        const auto minimum_dimension = dispatch.enabled && dispatch.stage != NativeLightingDispatchStage::Cache ? 1 : 0;
        const auto minimum_dispatch = dispatch.enabled ? 1 : 0;
        const auto minimum_workgroup = dispatch.enabled ? 1 : 0;

        require_range(dispatch.width, minimum_dimension, kMaxLightingDispatchDimension, "lighting dispatch width");
        require_range(dispatch.height, minimum_dimension, kMaxLightingDispatchDimension, "lighting dispatch height");
        require_range(dispatch.dispatch_x, minimum_dispatch, kMaxLightingDispatchGroups, "lighting dispatch group x");
        require_range(dispatch.dispatch_y, minimum_dispatch, kMaxLightingDispatchGroups, "lighting dispatch group y");
        require_range(dispatch.dispatch_z, minimum_dispatch, kMaxLightingDispatchGroups, "lighting dispatch group z");
        require_range(dispatch.workgroup_size_x, minimum_workgroup, kMaxLightingWorkgroupSize, "lighting workgroup size x");
        require_range(dispatch.workgroup_size_y, minimum_workgroup, kMaxLightingWorkgroupSize, "lighting workgroup size y");
        require_range(dispatch.workgroup_size_z, minimum_workgroup, kMaxLightingWorkgroupSize, "lighting workgroup size z");
        require_range(dispatch.input_count, 0, kMaxLightingIoCount, "lighting dispatch input count");
        require_range(dispatch.output_count, 0, kMaxLightingIoCount, "lighting dispatch output count");
        require_range(dispatch.sample_count, 0, kMaxLightingSamples, "lighting dispatch sample count");
        require_range(dispatch.ray_count, 0, kMaxLightingRays, "lighting dispatch ray count");
        require_range(dispatch.cache_read_count, 0, kMaxLightingCacheRecords, "lighting dispatch cache read count");
        require_range(dispatch.cache_write_count, 0, kMaxLightingCacheRecords, "lighting dispatch cache write count");

        if (dispatch.enabled && dispatch.stage == NativeLightingDispatchStage::Composite && dispatch.output_count == 0) {
            fail("enabled composite lighting dispatch must advertise at least one output");
        }
        if (dispatch.enabled && dispatch.stage == NativeLightingDispatchStage::Cache
                && dispatch.cache_read_count == 0 && dispatch.cache_write_count == 0) {
            fail("enabled lighting cache dispatch must advertise cache reads or writes");
        }
    }

    if (!packet.dispatches.empty()) {
        for (std::size_t index = 0; index < seen_stages.size(); index++) {
            if (!seen_stages[index]) {
                fail("lighting dispatch payload must include every Phase 5 stage");
            }
        }

        if (packet.generation == 0) {
            fail("lighting dispatch packet generation must be positive when payload is present");
        }
        if (packet.first_dispatch_generation != first_generation
                || packet.last_dispatch_generation != last_generation) {
            fail("lighting dispatch generations do not match packet bounds");
        }
        if (packet.generation < packet.last_dispatch_generation) {
            fail("lighting dispatch packet generation must include dispatch generation bounds");
        }
    }

    lighting_dispatch_packet_count_++;
    track_lighting_dispatch_upload(packet);
    last_lighting_dispatch_packet_ = std::move(packet);
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
        << ",section_upload_packets=" << section_upload_packet_count_
        << ",gbuffer_staging_packets=" << gbuffer_staging_packet_count_
        << ",lighting_dispatch_packets=" << lighting_dispatch_packet_count_
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
        << ",section_snapshots=" << section_snapshot_payload_total_
        << "}"
        << " world_generation=" << last_upload_packet_.first_world_generation << "-" << last_upload_packet_.last_world_generation
        << " material_generation=" << last_upload_packet_.material_generation
        << " section_upload_generation=" << last_section_upload_packet_.generation
        << " section_snapshots=" << last_section_upload_packet_.section_snapshot_count
        << " section_snapshot_payloads=" << last_section_upload_packet_.snapshots.size()
        << " section_snapshot_generation=" << last_section_upload_packet_.first_section_snapshot_generation
        << "-" << last_section_upload_packet_.last_section_snapshot_generation
        << " section_generations={section=" << last_section_upload_packet_.section_generation
        << ",material=" << last_section_upload_packet_.section_material_generation
        << ",occupancy=" << last_section_upload_packet_.section_occupancy_generation
        << ",emissive=" << last_section_upload_packet_.section_emissive_generation
        << ",dirty=" << last_section_upload_packet_.section_dirty_region_generation
        << "}"
        << " gbuffer_staging_generation=" << last_gbuffer_staging_packet_.generation
        << " gbuffers=" << last_gbuffer_staging_packet_.gbuffer_count
        << " gbuffer_payloads=" << last_gbuffer_staging_packet_.gbuffers.size()
        << " gbuffer_generation=" << last_gbuffer_staging_packet_.first_gbuffer_generation
        << "-" << last_gbuffer_staging_packet_.last_gbuffer_generation
        << " last_gbuffer_pass=\"" << staging_.gbuffer.last_pass_id
        << "\""
        << " last_gbuffer_numeric_pass=" << staging_.gbuffer.last_numeric_pass_id
        << " lighting_dispatch_generation=" << last_lighting_dispatch_packet_.generation
        << " lighting_dispatches=" << last_lighting_dispatch_packet_.dispatch_count
        << " lighting_dispatch_payloads=" << last_lighting_dispatch_packet_.dispatches.size()
        << " lighting_dispatch_generation_range=" << last_lighting_dispatch_packet_.first_dispatch_generation
        << "-" << last_lighting_dispatch_packet_.last_dispatch_generation
        << " lighting_dependencies={world=" << last_lighting_dispatch_packet_.world_generation
        << ",material=" << last_lighting_dispatch_packet_.material_generation
        << ",section=" << last_lighting_dispatch_packet_.section_generation
        << ",gbuffer=" << last_lighting_dispatch_packet_.gbuffer_generation
        << "}"
        << " staging={section={packets=" << staging_.section.packets
        << ",advertised_dirty_regions=" << staging_.section.advertised_dirty_regions
        << ",payload_dirty_regions=" << staging_.section.payload_dirty_regions
        << ",section_scoped=" << staging_.section.section_scoped_regions
        << ",global=" << staging_.section.global_regions
        << ",last_packet_generation=" << staging_.section.last_packet_generation
        << ",last_generation_range=" << staging_.section.last_first_generation << "-" << staging_.section.last_generation
        << ",last_estimated_bytes=" << staging_.section.last_estimated_bytes
        << ",total_estimated_bytes=" << staging_.section.total_estimated_bytes
        << ",placeholder_buffers=" << staging_.section.placeholder_buffers
        << ",snapshot_packets=" << staging_.section.snapshot_packets
        << ",advertised_snapshots=" << staging_.section.advertised_snapshots
        << ",payload_snapshots=" << staging_.section.payload_snapshots
        << ",payload_sections=" << staging_.section.payload_sections
        << ",last_snapshot_packet_generation=" << staging_.section.last_snapshot_packet_generation
        << ",last_snapshot_generation_range=" << staging_.section.last_snapshot_first_generation
        << "-" << staging_.section.last_snapshot_generation
        << ",last_section_generation=" << staging_.section.last_section_generation
        << ",last_material_generation=" << staging_.section.last_material_generation
        << ",last_occupancy_generation=" << staging_.section.last_occupancy_generation
        << ",last_emissive_generation=" << staging_.section.last_emissive_generation
        << ",last_dirty_region_generation=" << staging_.section.last_dirty_region_generation
        << ",last_occupied_voxels=" << staging_.section.last_occupied_voxels
        << ",total_occupied_voxels=" << staging_.section.total_occupied_voxels
        << ",last_snapshot_payload_bytes=" << staging_.section.last_snapshot_payload_bytes
        << ",total_snapshot_payload_bytes=" << staging_.section.total_snapshot_payload_bytes
        << "},voxel={packets=" << staging_.voxel.packets
        << ",dirty_sections=" << staging_.voxel.dirty_sections
        << ",last_dirty_sections=" << staging_.voxel.last_dirty_sections
        << ",last_estimated_voxels=" << staging_.voxel.last_estimated_voxels
        << ",last_occupancy_words=" << staging_.voxel.last_occupancy_words
        << ",last_material_indices=" << staging_.voxel.last_material_indices
        << ",last_estimated_bytes=" << staging_.voxel.last_estimated_bytes
        << ",total_estimated_bytes=" << staging_.voxel.total_estimated_bytes
        << ",placeholder_buffers=" << staging_.voxel.placeholder_buffers
        << ",snapshot_packets=" << staging_.voxel.snapshot_packets
        << ",payload_sections=" << staging_.voxel.payload_sections
        << ",last_payload_sections=" << staging_.voxel.last_payload_sections
        << ",occupancy_words=" << staging_.voxel.occupancy_words
        << ",last_occupancy_payload_words=" << staging_.voxel.last_occupancy_payload_words
        << ",material_palette_entries=" << staging_.voxel.material_palette_entries
        << ",last_material_palette_entries=" << staging_.voxel.last_material_palette_entries
        << ",emissive_entries=" << staging_.voxel.emissive_entries
        << ",last_emissive_entries=" << staging_.voxel.last_emissive_entries
        << ",last_snapshot_estimated_bytes=" << staging_.voxel.last_snapshot_estimated_bytes
        << ",total_snapshot_estimated_bytes=" << staging_.voxel.total_snapshot_estimated_bytes
        << "},gbuffer={frames_planned=" << staging_.gbuffer.frames_planned
        << ",staging_packets=" << staging_.gbuffer.staging_packets
        << ",advertised_gbuffers=" << staging_.gbuffer.advertised_gbuffers
        << ",payload_gbuffers=" << staging_.gbuffer.payload_gbuffers
        << ",allocation_intents=" << staging_.gbuffer.allocation_intents
        << ",attachment_intents=" << staging_.gbuffer.attachment_intents
        << ",enabled_attachments=" << staging_.gbuffer.enabled_attachments
        << ",disabled_attachments=" << staging_.gbuffer.disabled_attachments
        << ",last_packet_generation=" << staging_.gbuffer.last_packet_generation
        << ",last_generation_range=" << staging_.gbuffer.last_first_generation
        << "-" << staging_.gbuffer.last_generation
        << ",last_payload_gbuffers=" << staging_.gbuffer.last_payload_gbuffers
        << ",last_enabled_attachments=" << staging_.gbuffer.last_enabled_attachments
        << ",last_disabled_attachments=" << staging_.gbuffer.last_disabled_attachments
        << ",last_attachment_count=" << staging_.gbuffer.last_attachment_count
        << ",last_attachment_samples=" << staging_.gbuffer.last_attachment_samples
        << ",last_size=" << staging_.gbuffer.last_width << "x" << staging_.gbuffer.last_height
        << ",last_estimated_bytes=" << staging_.gbuffer.last_estimated_bytes
        << ",total_estimated_bytes=" << staging_.gbuffer.total_estimated_bytes
        << ",planned_this_frame=" << staging_.gbuffer.planned_this_frame
        << ",last_payload_recorded_this_frame=" << staging_.gbuffer.last_payload_recorded_this_frame
        << "},lighting={packets=" << staging_.lighting.packets
        << ",advertised_dispatches=" << staging_.lighting.advertised_dispatches
        << ",payload_dispatches=" << staging_.lighting.payload_dispatches
        << ",enabled_dispatches=" << staging_.lighting.enabled_dispatches
        << ",disabled_dispatches=" << staging_.lighting.disabled_dispatches
        << ",allocation_intents=" << staging_.lighting.allocation_intents
        << ",placeholder_buffers=" << staging_.lighting.placeholder_buffers
        << ",last_packet_generation=" << staging_.lighting.last_packet_generation
        << ",last_generation_range=" << staging_.lighting.last_first_generation
        << "-" << staging_.lighting.last_generation
        << ",last_dependencies={world=" << staging_.lighting.last_world_generation
        << ",material=" << staging_.lighting.last_material_generation
        << ",section=" << staging_.lighting.last_section_generation
        << ",gbuffer=" << staging_.lighting.last_gbuffer_generation
        << "}"
        << ",last_estimated_bytes=" << staging_.lighting.last_estimated_bytes
        << ",total_estimated_bytes=" << staging_.lighting.total_estimated_bytes
        << ",last_payload_recorded_this_frame=" << staging_.lighting.last_payload_recorded_this_frame
        << ",stages=[";
    for (std::size_t index = 0; index < staging_.lighting.stages.size(); index++) {
        if (index != 0) {
            out << "; ";
        }
        const auto& stage = staging_.lighting.stages[index];
        out << "{id=" << to_string(stage.stage)
            << ",packets=" << stage.packets
            << ",enabled_count=" << stage.enabled_count
            << ",disabled_count=" << stage.disabled_count
            << ",allocation_intents=" << stage.allocation_intents
            << ",placeholder_buffers=" << stage.placeholder_buffers
            << ",last_generation=" << stage.last_generation
            << ",last_size=" << stage.last_width << "x" << stage.last_height
            << ",last_dispatch=" << stage.last_dispatch_x << "x" << stage.last_dispatch_y << "x" << stage.last_dispatch_z
            << ",last_workgroup=" << stage.last_workgroup_size_x << "x" << stage.last_workgroup_size_y << "x" << stage.last_workgroup_size_z
            << ",last_io=" << stage.last_input_count << "/" << stage.last_output_count
            << ",last_samples=" << stage.last_sample_count
            << ",last_rays=" << stage.last_ray_count
            << ",last_cache=" << stage.last_cache_read_count << "/" << stage.last_cache_write_count
            << ",last_flags=" << stage.last_flags
            << ",last_estimated_bytes=" << stage.last_estimated_bytes
            << ",total_estimated_bytes=" << stage.total_estimated_bytes
            << ",enabled_this_packet=" << stage.enabled_this_packet
            << ",recorded_this_frame=" << stage.recorded_this_frame
            << "}";
    }
    out << "]}}";
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
            << ",allocation_intents=" << resource_stats.allocation_intent_count
            << ",intent_recorded=" << resource_stats.allocation_intent_counters.recorded
            << ",intent_buffers=" << resource_stats.allocation_intent_counters.buffers
            << ",intent_images=" << resource_stats.allocation_intent_counters.images
            << ",intent_estimated_bytes=" << resource_stats.allocation_intent_counters.estimated_bytes
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

std::uint64_t Renderer::estimate_section_snapshot_staging_bytes(const SectionUploadPacket& packet) const {
    std::uint64_t bytes = static_cast<std::uint64_t>(packet.snapshots.size()) * kEstimatedSectionSnapshotMetadataBytes;
    for (const auto& snapshot : packet.snapshots) {
        bytes += static_cast<std::uint64_t>(snapshot.occupancy_mask_word_count) * sizeof(std::uint64_t);
        bytes += static_cast<std::uint64_t>(snapshot.material_palette_ids.size()) * sizeof(std::int32_t);
        bytes += static_cast<std::uint64_t>(snapshot.emissive_entries.size()) * kSectionEmissiveEntryBytes;
    }
    return bytes;
}

std::uint64_t Renderer::estimate_section_staging_bytes(std::uint64_t dirty_section_count) const {
    return dirty_section_count * kEstimatedSectionMetadataBytes;
}

std::uint64_t Renderer::estimate_voxel_staging_bytes(std::uint64_t dirty_section_count) const {
    return dirty_section_count * (kVoxelOccupancyBytesPerSection + kVoxelMaterialIndexBytesPerSection);
}

std::uint64_t Renderer::estimate_gbuffer_attachment_bytes(
        std::int32_t width,
        std::int32_t height,
        std::uint32_t bytes_per_pixel) const {
    if (width <= 0 || height <= 0 || bytes_per_pixel == 0) {
        return 0;
    }

    return static_cast<std::uint64_t>(width) * static_cast<std::uint64_t>(height) * bytes_per_pixel;
}

std::uint64_t Renderer::estimate_gbuffer_attachment_bytes(const GBufferAttachmentUpload& attachment) const {
    if (!attachment.enabled) {
        return 0;
    }

    return estimate_gbuffer_attachment_bytes(
            attachment.width,
            attachment.height,
            estimate_bytes_per_pixel(attachment.format_tag))
        * static_cast<std::uint64_t>(attachment.samples);
}

std::uint64_t Renderer::estimate_lighting_dispatch_bytes(const LightingDispatchStageUpload& dispatch) const {
    if (!dispatch.enabled) {
        return 0;
    }
    if (dispatch.estimated_bytes != 0) {
        return dispatch.estimated_bytes;
    }
    if (dispatch.dispatch_x <= 0 || dispatch.dispatch_y <= 0 || dispatch.dispatch_z <= 0) {
        return 0;
    }

    auto saturated_multiply = [](std::uint64_t left, std::uint64_t right) {
        constexpr std::uint64_t maximum = ~std::uint64_t{0};
        if (left != 0 && right > maximum / left) {
            return maximum;
        }
        return left * right;
    };
    auto saturated_add = [](std::uint64_t left, std::uint64_t right) {
        constexpr std::uint64_t maximum = ~std::uint64_t{0};
        if (right > maximum - left) {
            return maximum;
        }
        return left + right;
    };

    const auto group_count = saturated_multiply(
            saturated_multiply(
                    static_cast<std::uint64_t>(dispatch.dispatch_x),
                    static_cast<std::uint64_t>(dispatch.dispatch_y)),
            static_cast<std::uint64_t>(dispatch.dispatch_z));
    const auto metadata_bytes = saturated_multiply(group_count, kEstimatedLightingDispatchGroupBytes);
    const auto io_bytes = static_cast<std::uint64_t>(dispatch.input_count + dispatch.output_count) * 32;
    return saturated_add(metadata_bytes, io_bytes);
}

void Renderer::track_upload_staging_placeholder(const UploadPacket& packet) {
    std::uint64_t section_scoped_regions = 0;
    for (const auto& dirty_region : packet.dirty_regions) {
        if (dirty_region.section_scoped) {
            section_scoped_regions++;
        }
    }

    const auto payload_dirty_regions = static_cast<std::uint64_t>(packet.dirty_regions.size());
    const auto global_regions = payload_dirty_regions - section_scoped_regions;
    const auto section_staging_bytes = estimate_section_staging_bytes(payload_dirty_regions);
    const auto voxel_staging_bytes = estimate_voxel_staging_bytes(section_scoped_regions);

    staging_.section.packets++;
    staging_.section.advertised_dirty_regions += static_cast<std::uint64_t>(packet.dirty_region_count);
    staging_.section.payload_dirty_regions += payload_dirty_regions;
    staging_.section.section_scoped_regions += section_scoped_regions;
    staging_.section.global_regions += global_regions;
    staging_.section.last_packet_generation = packet.generation;
    staging_.section.last_first_generation = packet.first_world_generation;
    staging_.section.last_generation = packet.last_world_generation;
    staging_.section.last_estimated_bytes = section_staging_bytes;
    staging_.section.total_estimated_bytes += section_staging_bytes;

    staging_.voxel.packets++;
    staging_.voxel.dirty_sections += section_scoped_regions;
    staging_.voxel.last_dirty_sections = section_scoped_regions;
    staging_.voxel.last_estimated_voxels = section_scoped_regions * kSectionVoxelCount;
    staging_.voxel.last_occupancy_words = section_scoped_regions * kVoxelOccupancyWordCount;
    staging_.voxel.last_material_indices = section_scoped_regions * kSectionVoxelCount;
    staging_.voxel.last_estimated_bytes = voxel_staging_bytes;
    staging_.voxel.total_estimated_bytes += voxel_staging_bytes;

    const auto staging_bytes = estimate_upload_staging_bytes(packet);
    if (resources_ == nullptr || !frame_open_) {
        return;
    }

    if (staging_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                staging_bytes,
                "upload:world-delta-staging-intent",
                NativeResourceIntentStage::WorldDeltaUpload);
        resources_->track_transient_buffer(frame_index_, 0, staging_bytes, "upload:world-delta-staging");
    }

    if (section_staging_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                section_staging_bytes,
                "upload:section-metadata-staging-intent",
                NativeResourceIntentStage::SectionUpload);
        resources_->track_transient_buffer(frame_index_, 0, section_staging_bytes, "upload:section-metadata-staging");
        staging_.section.placeholder_buffers++;
    }

    if (voxel_staging_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                voxel_staging_bytes,
                "upload:voxel-occupancy-material-staging-intent",
                NativeResourceIntentStage::VoxelUpload);
        resources_->track_transient_buffer(frame_index_, 0, voxel_staging_bytes, "upload:voxel-occupancy-material-staging");
        staging_.voxel.placeholder_buffers++;
    }
}

void Renderer::track_section_snapshot_staging_placeholder(const SectionUploadPacket& packet) {
    std::uint64_t payload_sections = 0;
    std::uint64_t occupied_voxels = 0;
    std::uint64_t occupancy_words = 0;
    std::uint64_t material_palette_entries = 0;
    std::uint64_t emissive_entries = 0;
    for (const auto& snapshot : packet.snapshots) {
        if (snapshot.has_section_payload()) {
            payload_sections++;
        }
        occupied_voxels += static_cast<std::uint64_t>(snapshot.occupied_voxel_count);
        occupancy_words += static_cast<std::uint64_t>(snapshot.occupancy_mask_word_count);
        material_palette_entries += static_cast<std::uint64_t>(snapshot.material_palette_ids.size());
        emissive_entries += static_cast<std::uint64_t>(snapshot.emissive_entries.size());
    }

    const auto payload_snapshots = static_cast<std::uint64_t>(packet.snapshots.size());
    const auto section_metadata_bytes = payload_snapshots * kEstimatedSectionSnapshotMetadataBytes;
    const auto total_payload_bytes = estimate_section_snapshot_staging_bytes(packet);
    const auto voxel_payload_bytes = total_payload_bytes - section_metadata_bytes;

    staging_.section.snapshot_packets++;
    staging_.section.advertised_snapshots += static_cast<std::uint64_t>(packet.section_snapshot_count);
    staging_.section.payload_snapshots += payload_snapshots;
    staging_.section.payload_sections += payload_sections;
    staging_.section.last_snapshot_packet_generation = packet.generation;
    staging_.section.last_snapshot_first_generation = packet.first_section_snapshot_generation;
    staging_.section.last_snapshot_generation = packet.last_section_snapshot_generation;
    staging_.section.last_section_generation = packet.section_generation;
    staging_.section.last_material_generation = packet.section_material_generation;
    staging_.section.last_occupancy_generation = packet.section_occupancy_generation;
    staging_.section.last_emissive_generation = packet.section_emissive_generation;
    staging_.section.last_dirty_region_generation = packet.section_dirty_region_generation;
    staging_.section.last_occupied_voxels = occupied_voxels;
    staging_.section.total_occupied_voxels += occupied_voxels;
    staging_.section.last_snapshot_payload_bytes = total_payload_bytes;
    staging_.section.total_snapshot_payload_bytes += total_payload_bytes;

    staging_.voxel.snapshot_packets++;
    staging_.voxel.payload_sections += payload_sections;
    staging_.voxel.last_payload_sections = payload_sections;
    staging_.voxel.occupancy_words += occupancy_words;
    staging_.voxel.last_occupancy_payload_words = occupancy_words;
    staging_.voxel.material_palette_entries += material_palette_entries;
    staging_.voxel.last_material_palette_entries = material_palette_entries;
    staging_.voxel.emissive_entries += emissive_entries;
    staging_.voxel.last_emissive_entries = emissive_entries;
    staging_.voxel.last_snapshot_estimated_bytes = voxel_payload_bytes;
    staging_.voxel.total_snapshot_estimated_bytes += voxel_payload_bytes;

    if (resources_ == nullptr || !frame_open_) {
        return;
    }

    if (section_metadata_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                section_metadata_bytes,
                "upload:section-snapshot-metadata-intent",
                NativeResourceIntentStage::SectionUpload);
        resources_->track_transient_buffer(frame_index_, 0, section_metadata_bytes, "upload:section-snapshot-metadata");
        staging_.section.placeholder_buffers++;
    }

    if (voxel_payload_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                voxel_payload_bytes,
                "upload:section-voxel-payload-intent",
                NativeResourceIntentStage::VoxelUpload);
        resources_->track_transient_buffer(frame_index_, 0, voxel_payload_bytes, "upload:section-voxel-payload");
        staging_.voxel.placeholder_buffers++;
    }
}

void Renderer::track_gbuffer_staging_upload(const GBufferStagingPacket& packet) {
    const bool can_record_resource_intents = resources_ != nullptr && frame_open_;
    std::uint64_t payload_gbuffers = static_cast<std::uint64_t>(packet.gbuffers.size());
    std::uint64_t attachment_count = 0;
    std::uint64_t enabled_attachment_count = 0;
    std::uint64_t disabled_attachment_count = 0;
    std::uint64_t estimated_bytes = 0;
    std::uint64_t max_samples = 0;
    std::int32_t last_width = 0;
    std::int32_t last_height = 0;
    std::int32_t last_numeric_pass_id = 0;
    std::string last_pass_id;

    for (const auto& upload : packet.gbuffers) {
        last_width = upload.width;
        last_height = upload.height;
        last_numeric_pass_id = upload.numeric_pass_id;
        last_pass_id = upload.pass_id;

        for (const auto& attachment : upload.attachments) {
            attachment_count++;
            if (static_cast<std::uint64_t>(attachment.samples) > max_samples) {
                max_samples = static_cast<std::uint64_t>(attachment.samples);
            }

            if (!attachment.enabled) {
                disabled_attachment_count++;
                continue;
            }

            enabled_attachment_count++;
            const auto attachment_bytes = estimate_gbuffer_attachment_bytes(attachment);
            estimated_bytes += attachment_bytes;

            if (can_record_resource_intents) {
                const auto label = std::string("upload:gbuffer-staging:")
                    + upload.pass_id
                    + ":"
                    + attachment.name;
                resources_->track_image_allocation_intent(
                        frame_index_,
                        attachment.width,
                        attachment.height,
                        static_cast<std::uint32_t>(attachment.format_tag),
                        attachment_bytes,
                        label,
                        NativeResourceIntentStage::FutureGBuffer);
                staging_.gbuffer.allocation_intents++;
                staging_.gbuffer.attachment_intents++;
            }
        }
    }

    staging_.gbuffer.staging_packets++;
    staging_.gbuffer.advertised_gbuffers += static_cast<std::uint64_t>(packet.gbuffer_count);
    staging_.gbuffer.payload_gbuffers += payload_gbuffers;
    staging_.gbuffer.enabled_attachments += enabled_attachment_count;
    staging_.gbuffer.disabled_attachments += disabled_attachment_count;
    staging_.gbuffer.last_packet_generation = packet.generation;
    staging_.gbuffer.last_first_generation = packet.first_gbuffer_generation;
    staging_.gbuffer.last_generation = packet.last_gbuffer_generation;
    staging_.gbuffer.last_payload_gbuffers = payload_gbuffers;
    staging_.gbuffer.last_enabled_attachments = enabled_attachment_count;
    staging_.gbuffer.last_disabled_attachments = disabled_attachment_count;
    staging_.gbuffer.last_attachment_count = attachment_count;
    staging_.gbuffer.last_attachment_samples = max_samples;
    staging_.gbuffer.last_estimated_bytes = estimated_bytes;
    staging_.gbuffer.total_estimated_bytes += estimated_bytes;
    staging_.gbuffer.last_width = last_width;
    staging_.gbuffer.last_height = last_height;
    staging_.gbuffer.last_numeric_pass_id = last_numeric_pass_id;
    staging_.gbuffer.last_payload_recorded_this_frame = can_record_resource_intents && enabled_attachment_count != 0;
    staging_.gbuffer.last_pass_id = std::move(last_pass_id);
}

void Renderer::track_lighting_dispatch_upload(const LightingDispatchPacket& packet) {
    const bool can_record_resource_intents = resources_ != nullptr && frame_open_;
    auto saturated_add = [](std::uint64_t left, std::uint64_t right) {
        constexpr std::uint64_t maximum = ~std::uint64_t{0};
        if (right > maximum - left) {
            return maximum;
        }
        return left + right;
    };
    std::uint64_t enabled_dispatches = 0;
    std::uint64_t disabled_dispatches = 0;
    std::uint64_t estimated_bytes = 0;
    bool recorded_this_frame = false;

    if (packet.dispatches.empty()) {
        for (auto& stage : staging_.lighting.stages) {
            stage.enabled_this_packet = false;
            stage.recorded_this_frame = false;
        }
    }

    for (const auto& dispatch : packet.dispatches) {
        auto& stage = lighting_stage_telemetry(dispatch.stage);
        const auto dispatch_bytes = estimate_lighting_dispatch_bytes(dispatch);

        stage.packets++;
        stage.enabled_this_packet = dispatch.enabled;
        stage.recorded_this_frame = false;
        stage.last_generation = dispatch.generation;
        stage.last_estimated_bytes = dispatch_bytes;
        stage.total_estimated_bytes = saturated_add(stage.total_estimated_bytes, dispatch_bytes);
        stage.last_width = dispatch.width;
        stage.last_height = dispatch.height;
        stage.last_dispatch_x = dispatch.dispatch_x;
        stage.last_dispatch_y = dispatch.dispatch_y;
        stage.last_dispatch_z = dispatch.dispatch_z;
        stage.last_workgroup_size_x = dispatch.workgroup_size_x;
        stage.last_workgroup_size_y = dispatch.workgroup_size_y;
        stage.last_workgroup_size_z = dispatch.workgroup_size_z;
        stage.last_input_count = dispatch.input_count;
        stage.last_output_count = dispatch.output_count;
        stage.last_sample_count = dispatch.sample_count;
        stage.last_ray_count = dispatch.ray_count;
        stage.last_cache_read_count = dispatch.cache_read_count;
        stage.last_cache_write_count = dispatch.cache_write_count;
        stage.last_flags = dispatch.flags;

        if (dispatch.enabled) {
            enabled_dispatches++;
            stage.enabled_count++;
        } else {
            disabled_dispatches++;
            stage.disabled_count++;
            continue;
        }

        estimated_bytes = saturated_add(estimated_bytes, dispatch_bytes);
        if (can_record_resource_intents && dispatch_bytes != 0) {
            const auto label = std::string("lighting:")
                + dispatch.stage_name
                + ":dispatch-metadata";
            resources_->track_buffer_allocation_intent(
                    frame_index_,
                    dispatch_bytes,
                    label + "-intent",
                    resource_stage_for_lighting_stage(dispatch.stage));
            resources_->track_transient_buffer(frame_index_, 0, dispatch_bytes, label);
            stage.allocation_intents++;
            stage.placeholder_buffers++;
            stage.recorded_this_frame = true;
            staging_.lighting.allocation_intents++;
            staging_.lighting.placeholder_buffers++;
            recorded_this_frame = true;
        }
    }

    staging_.lighting.packets++;
    staging_.lighting.advertised_dispatches += static_cast<std::uint64_t>(packet.dispatch_count);
    staging_.lighting.payload_dispatches += static_cast<std::uint64_t>(packet.dispatches.size());
    staging_.lighting.enabled_dispatches += enabled_dispatches;
    staging_.lighting.disabled_dispatches += disabled_dispatches;
    staging_.lighting.last_packet_generation = packet.generation;
    staging_.lighting.last_first_generation = packet.first_dispatch_generation;
    staging_.lighting.last_generation = packet.last_dispatch_generation;
    staging_.lighting.last_world_generation = packet.world_generation;
    staging_.lighting.last_material_generation = packet.material_generation;
    staging_.lighting.last_section_generation = packet.section_generation;
    staging_.lighting.last_gbuffer_generation = packet.gbuffer_generation;
    staging_.lighting.last_estimated_bytes = estimated_bytes;
    staging_.lighting.total_estimated_bytes = saturated_add(staging_.lighting.total_estimated_bytes, estimated_bytes);
    staging_.lighting.last_payload_recorded_this_frame = recorded_this_frame;
}

void Renderer::track_gbuffer_placeholder_intent() {
    staging_.gbuffer.frames_planned++;
    staging_.gbuffer.last_width = width_;
    staging_.gbuffer.last_height = height_;
    staging_.gbuffer.last_attachment_count = 0;
    staging_.gbuffer.last_attachment_samples = 0;
    staging_.gbuffer.last_estimated_bytes = 0;
    staging_.gbuffer.planned_this_frame = false;
    staging_.gbuffer.last_payload_recorded_this_frame = false;

    if (resources_ == nullptr || !frame_open_ || width_ <= 0 || height_ <= 0) {
        return;
    }

    struct AttachmentIntent {
        const char* label;
        std::uint32_t format_tag;
        std::uint32_t bytes_per_pixel;
    };

    constexpr AttachmentIntent attachments[] = {
        {"gbuffer:depth-intent", kGBufferDepthFormatTag, 4},
        {"gbuffer:normal-material-intent", kGBufferNormalMaterialFormatTag, 4},
        {"gbuffer:albedo-emissive-intent", kGBufferAlbedoEmissiveFormatTag, 8},
        {"gbuffer:motion-history-intent", kGBufferMotionHistoryFormatTag, 8},
        {"gbuffer:reactive-mask-intent", kGBufferReactiveMaskFormatTag, 1}
    };

    std::uint64_t frame_estimated_bytes = 0;
    for (const auto& attachment : attachments) {
        const auto estimated_bytes = estimate_gbuffer_attachment_bytes(width_, height_, attachment.bytes_per_pixel);
        resources_->track_image_allocation_intent(
                frame_index_,
                width_,
                height_,
                attachment.format_tag,
                estimated_bytes,
                attachment.label,
                NativeResourceIntentStage::FutureGBuffer);
        frame_estimated_bytes += estimated_bytes;
        staging_.gbuffer.allocation_intents++;
        staging_.gbuffer.attachment_intents++;
        staging_.gbuffer.last_attachment_count++;
    }

    staging_.gbuffer.last_estimated_bytes = frame_estimated_bytes;
    staging_.gbuffer.total_estimated_bytes += frame_estimated_bytes;
    staging_.gbuffer.planned_this_frame = true;
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

void Renderer::reset_staging_telemetry() {
    staging_ = {};
    for (std::size_t index = 0; index < staging_.lighting.stages.size(); index++) {
        staging_.lighting.stages[index].stage = static_cast<NativeLightingDispatchStage>(index);
    }
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

    track_gbuffer_placeholder_intent();
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

NativeLightingDispatchStageTelemetry& Renderer::lighting_stage_telemetry(NativeLightingDispatchStage stage) {
    return staging_.lighting.stages.at(lighting_stage_index(stage));
}

const NativeLightingDispatchStageTelemetry& Renderer::lighting_stage_telemetry(NativeLightingDispatchStage stage) const {
    return staging_.lighting.stages.at(lighting_stage_index(stage));
}

} // namespace lucerna
