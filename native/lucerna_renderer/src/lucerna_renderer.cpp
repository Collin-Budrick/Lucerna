#include "lucerna_renderer.hpp"

#include "lucerna_resource_manager.hpp"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdlib>
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
constexpr std::uint32_t kDirectLightingOutputFormatTag = 20;
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
constexpr std::uint32_t kDiffuseGiVisibleSignalFormatTag = 21;
constexpr std::uint64_t kRound6GiVisibleSignalSampleLimit = 4096;
constexpr std::uint64_t kEstimatedLightingDispatchGroupBytes = 64;
constexpr std::uint32_t kLightingDispatchFlagValidated = 1U;
constexpr std::uint32_t kLightingDispatchFlagPlaceholder = 1U << 1U;
constexpr std::uint32_t kLightingDispatchFlagTemporalHistory = 1U << 2U;
constexpr std::uint32_t kLightingDispatchFlagDebugOverlay = 1U << 3U;
constexpr std::uint32_t kLightingDispatchFlagReuseOnly = 1U << 4U;
constexpr std::uint32_t kDirectPayloadFlagValidated = 1U;
constexpr std::uint32_t kDirectPayloadFlagHasDirectLightingWork = 1U << 1U;
constexpr std::uint32_t kDirectPayloadFlagReadyForShadowTracing = 1U << 2U;
constexpr std::uint32_t kDirectPayloadFlagRequiresOccupancyMasks = 1U << 3U;
constexpr std::uint32_t kDirectPayloadFlagAllowTranslucentOccluders = 1U << 4U;
constexpr std::uint32_t kDirectPayloadFlagWorldTimeAvailable = 1U << 5U;
constexpr std::uint32_t kDirectPayloadKnownFlags =
        kDirectPayloadFlagValidated
        | kDirectPayloadFlagHasDirectLightingWork
        | kDirectPayloadFlagReadyForShadowTracing
        | kDirectPayloadFlagRequiresOccupancyMasks
        | kDirectPayloadFlagAllowTranslucentOccluders
        | kDirectPayloadFlagWorldTimeAvailable;
constexpr std::size_t kDirectRayBudgetStride = 6;
constexpr std::size_t kDirectRayBudgetPrimaryRaysPerPixelOffset = 0;
constexpr std::size_t kDirectRayBudgetShadowRaysPerHitOffset = 1;
constexpr std::size_t kDirectRayBudgetGiRaysPerHitOffset = 2;
constexpr std::size_t kDirectRayBudgetMaxRaysPerFrameOffset = 3;
constexpr std::size_t kDirectRayBudgetMaxVisitedVoxelsPerRayOffset = 4;
constexpr std::size_t kDirectRayBudgetMaxVisitedSectionsPerRayOffset = 5;
constexpr std::size_t kDirectCelestialLightDataStride = 9;
constexpr std::size_t kDirectEmissiveLightMetadataStride = 5;
constexpr std::size_t kDirectEmissiveLightDataStride = 6;
constexpr std::size_t kDirectShadowCandidateMetadataStride = 3;
constexpr std::size_t kDirectShadowCandidateRayStride = 9;
constexpr std::size_t kDirectSectionSnapshotMetadataStride = 15;
constexpr std::size_t kDirectSectionSnapshotGenerationStride = 7;
constexpr std::size_t kDirectEmissiveBlockXOffset = 0;
constexpr std::size_t kDirectEmissiveBlockYOffset = 1;
constexpr std::size_t kDirectEmissiveBlockZOffset = 2;
constexpr std::size_t kDirectEmissiveColorRedOffset = 0;
constexpr std::size_t kDirectEmissiveColorGreenOffset = 1;
constexpr std::size_t kDirectEmissiveColorBlueOffset = 2;
constexpr std::size_t kDirectEmissiveIntensityOffset = 3;
constexpr std::size_t kDirectEmissiveInfluenceRadiusOffset = 4;
constexpr std::size_t kDirectShadowRayOriginXOffset = 0;
constexpr std::size_t kDirectShadowRayOriginYOffset = 1;
constexpr std::size_t kDirectShadowRayOriginZOffset = 2;
constexpr std::size_t kDirectShadowRayContributionWeightOffset = 8;
constexpr std::size_t kDirectSectionXOffset = 0;
constexpr std::size_t kDirectSectionYOffset = 1;
constexpr std::size_t kDirectSectionZOffset = 2;
constexpr std::size_t kDirectSectionOccupiedVoxelCountOffset = 3;
constexpr std::size_t kDirectSectionOpaqueVoxelCountOffset = 4;
constexpr std::size_t kDirectSectionTranslucentVoxelCountOffset = 5;
constexpr std::size_t kDirectSectionFluidVoxelCountOffset = 6;
constexpr std::size_t kDirectSectionEmissiveVoxelCountOffset = 7;
constexpr std::size_t kDirectSectionMaterialPaletteSizeOffset = 13;
constexpr std::int32_t kMaxDirectCpuOutputWidth = 64;
constexpr std::int32_t kMaxDirectCpuOutputHeight = 36;
constexpr std::size_t kRound11RestirDiMaxCandidateCount = 4096;
constexpr std::size_t kRound11RestirDiMaxReservoirCount = 64;
constexpr float kRound11RestirDiPreviewGain = 0.075F;
constexpr std::int32_t kMaxDiffuseGiCpuOutputWidth = 1024;
constexpr std::int32_t kMaxDiffuseGiCpuOutputHeight = 1024;
constexpr std::uint64_t kRound9ClusterVoxelCapacity = 8ULL * 8ULL * 8ULL;
constexpr std::uint64_t kRound9MaxClustersPerSection = 8;
constexpr std::uint64_t kEstimatedRound9ClusterMetadataBytes = 96;
constexpr std::uint64_t kEstimatedRound9ClusterVisibilityBytes = 16;
constexpr double kRound9ConservativeNearSectionRadius = 2.75;
constexpr double kRound9ConservativeViewDotThreshold = -0.18;
constexpr double kRound9ConservativeVerticalSectionLimit = 8.0;
constexpr float kDirectCpuCelestialScale = 0.02F;
constexpr float kDirectCpuMinimumSurfaceRadius = 8.0F;
constexpr float kDirectCpuEmissiveSurfaceScale = 118.0F;
constexpr float kDirectCpuEmissiveScreenScale = 46.0F;
constexpr float kDirectCpuEmissiveAlphaFloor = 0.34F;
constexpr float kDirectCpuEmissiveAlphaGain = 0.66F;
constexpr std::size_t kLightingPayloadCategoryDirect = 0;
constexpr std::size_t kLightingPayloadCategoryGi = 1;
constexpr std::size_t kLightingPayloadCategoryPost = 2;
constexpr std::size_t kLightingPayloadCategoryCache = 3;

std::uint64_t saturated_add(std::uint64_t left, std::uint64_t right) {
    constexpr std::uint64_t maximum = ~std::uint64_t{0};
    if (right > maximum - left) {
        return maximum;
    }
    return left + right;
}

std::uint64_t saturated_multiply(std::uint64_t left, std::uint64_t right) {
    constexpr std::uint64_t maximum = ~std::uint64_t{0};
    if (left != 0 && right > maximum / left) {
        return maximum;
    }
    return left * right;
}

std::uint64_t non_negative_u64(std::int32_t value) {
    return value <= 0 ? 0 : static_cast<std::uint64_t>(value);
}

float finite_non_negative(float value) {
    if (!std::isfinite(value) || value < 0.0F) {
        return 0.0F;
    }
    return value;
}

float smooth_unit_response(float value) {
    const float clamped = std::clamp(value, 0.0F, 1.0F);
    return clamped * clamped * (3.0F - (2.0F * clamped));
}

float broad_surface_response(float u, float v) {
    const float horizontal = smooth_unit_response(1.0F - (std::abs(u - 0.50F) / 0.40F));
    const float vertical = smooth_unit_response(1.0F - (std::abs(v - 0.48F) / 0.46F));
    const float lower_surface = smooth_unit_response(1.0F - (std::abs(v - 0.66F) / 0.42F));
    return std::clamp((horizontal * vertical * 0.72F) + (horizontal * lower_surface * 0.28F), 0.0F, 1.0F);
}

struct NativeGiSceneBounds {
    bool initialized = false;
    float min_x = 0.0F;
    float max_x = 0.0F;
    float min_y = 0.0F;
    float max_y = 0.0F;
    float min_z = 0.0F;
    float max_z = 0.0F;
};

struct Round9ClusterCullingEstimate {
    std::uint64_t visible_clusters = 0;
    std::uint64_t offscreen_clusters = 0;
    std::uint64_t cluster_count = 0;
    std::uint64_t payload_sections = 0;
    std::string mode;
    std::string reason;
};

struct Round9SectionClusterCandidate {
    double section_x = 0.0;
    double section_y = 0.0;
    double section_z = 0.0;
    std::uint64_t cluster_count = 0;
};

Round9ClusterCullingEstimate estimate_round9_cpu_cluster_culling(
        const std::vector<Round9SectionClusterCandidate>& candidates,
        std::uint64_t frame_index,
        std::uint64_t generation) {
    Round9ClusterCullingEstimate estimate;
    estimate.mode = "round9_cpu_conservative_scene_orientation_culling";

    if (candidates.empty()) {
        estimate.reason = "no_payload_section_clusters";
        return estimate;
    }

    double center_x = 0.0;
    double center_y = 0.0;
    double center_z = 0.0;
    for (const auto& candidate : candidates) {
        if (candidate.cluster_count == 0) {
            continue;
        }
        estimate.cluster_count = saturated_add(estimate.cluster_count, candidate.cluster_count);
        estimate.payload_sections++;
        center_x += candidate.section_x;
        center_y += candidate.section_y;
        center_z += candidate.section_z;
    }

    if (estimate.cluster_count == 0 || estimate.payload_sections == 0) {
        estimate.reason = "zero_valid_cluster_candidates";
        return estimate;
    }

    const auto section_count = static_cast<double>(estimate.payload_sections);
    center_x /= section_count;
    center_y /= section_count;
    center_z /= section_count;

    double max_horizontal_distance = 0.0;
    for (const auto& candidate : candidates) {
        const double dx = candidate.section_x - center_x;
        const double dz = candidate.section_z - center_z;
        max_horizontal_distance = std::max(max_horizontal_distance, std::sqrt((dx * dx) + (dz * dz)));
    }

    const double near_radius = std::clamp(
            max_horizontal_distance * 0.35,
            kRound9ConservativeNearSectionRadius,
            8.0);
    constexpr double directions[][2] = {
            {1.0, 0.0},
            {0.70710678118, 0.70710678118},
            {0.0, 1.0},
            {-0.70710678118, 0.70710678118},
            {-1.0, 0.0},
            {-0.70710678118, -0.70710678118},
            {0.0, -1.0},
            {0.70710678118, -0.70710678118},
    };
    const auto direction_index = static_cast<std::size_t>((frame_index + generation) & 7ULL);
    const double view_x = directions[direction_index][0];
    const double view_z = directions[direction_index][1];

    std::uint64_t nearest_fallback_clusters = 0;
    double nearest_distance = 0.0;
    bool has_nearest = false;
    for (const auto& candidate : candidates) {
        if (candidate.cluster_count == 0) {
            continue;
        }

        const double dx = candidate.section_x - center_x;
        const double dy = candidate.section_y - center_y;
        const double dz = candidate.section_z - center_z;
        const double horizontal_distance = std::sqrt((dx * dx) + (dz * dz));
        if (!has_nearest || horizontal_distance < nearest_distance) {
            nearest_distance = horizontal_distance;
            nearest_fallback_clusters = candidate.cluster_count;
            has_nearest = true;
        }

        const double normalized_x = horizontal_distance <= 0.0001 ? view_x : dx / horizontal_distance;
        const double normalized_z = horizontal_distance <= 0.0001 ? view_z : dz / horizontal_distance;
        const double view_dot = (normalized_x * view_x) + (normalized_z * view_z);
        const bool proximity_visible = horizontal_distance <= near_radius;
        const bool orientation_visible =
                view_dot >= kRound9ConservativeViewDotThreshold
                && std::abs(dy) <= kRound9ConservativeVerticalSectionLimit;

        if (proximity_visible || orientation_visible) {
            estimate.visible_clusters = saturated_add(estimate.visible_clusters, candidate.cluster_count);
        } else {
            estimate.offscreen_clusters = saturated_add(estimate.offscreen_clusters, candidate.cluster_count);
        }
    }

    if (estimate.visible_clusters == 0 && nearest_fallback_clusters != 0) {
        estimate.visible_clusters = nearest_fallback_clusters;
        estimate.offscreen_clusters = estimate.cluster_count > nearest_fallback_clusters
                ? estimate.cluster_count - nearest_fallback_clusters
                : 0;
        estimate.reason = "no_true_camera_matrix_nearest_cluster_kept_visible_conservative_boundary";
    } else if (estimate.offscreen_clusters == 0) {
        estimate.reason = "all_clusters_inside_conservative_scene_orientation_or_near_radius";
    } else {
        estimate.reason = "offscreen_clusters_rejected_by_conservative_scene_orientation_and_proximity";
    }

    if (estimate.visible_clusters > estimate.cluster_count) {
        estimate.visible_clusters = estimate.cluster_count;
    }
    estimate.offscreen_clusters = estimate.cluster_count - estimate.visible_clusters;
    return estimate;
}

void include_native_gi_scene_point(NativeGiSceneBounds& bounds, float x, float y, float z) {
    if (!std::isfinite(x) || !std::isfinite(y) || !std::isfinite(z)) {
        return;
    }
    if (!bounds.initialized) {
        bounds.initialized = true;
        bounds.min_x = x;
        bounds.max_x = x;
        bounds.min_y = y;
        bounds.max_y = y;
        bounds.min_z = z;
        bounds.max_z = z;
        return;
    }
    bounds.min_x = std::min(bounds.min_x, x);
    bounds.max_x = std::max(bounds.max_x, x);
    bounds.min_y = std::min(bounds.min_y, y);
    bounds.max_y = std::max(bounds.max_y, y);
    bounds.min_z = std::min(bounds.min_z, z);
    bounds.max_z = std::max(bounds.max_z, z);
}

float project_native_gi_axis(float value, float minimum, float maximum, float fallback) {
    if (!std::isfinite(value) || !std::isfinite(minimum) || !std::isfinite(maximum)) {
        return fallback;
    }
    const float span = maximum - minimum;
    if (span <= 0.001F) {
        return fallback;
    }
    return std::clamp((value - minimum) / span, 0.0F, 1.0F);
}

float native_gi_lobe(float u, float v, float center_u, float center_v, float radius) {
    const float safe_radius = std::max(radius, 0.001F);
    const float delta_u = u - center_u;
    const float delta_v = v - center_v;
    const float distance = std::sqrt((delta_u * delta_u) + (delta_v * delta_v));
    const float response = std::max(0.0F, 1.0F - (distance / safe_radius));
    return response * response;
}

float sum_strided_float_field(
        const std::vector<float>& values,
        std::size_t count,
        std::size_t stride,
        std::size_t offset) {
    if (stride == 0 || offset >= stride) {
        return 0.0F;
    }

    float total = 0.0F;
    for (std::size_t index = 0; index < count; index++) {
        const auto value_index = index * stride + offset;
        if (value_index >= values.size()) {
            break;
        }
        total += finite_non_negative(values[value_index]);
    }
    return total;
}

float strided_float_or_zero(
        const std::vector<float>& values,
        std::size_t index,
        std::size_t stride,
        std::size_t offset) {
    if (stride == 0 || offset >= stride) {
        return 0.0F;
    }
    const auto value_index = index * stride + offset;
    if (value_index >= values.size()) {
        return 0.0F;
    }
    return finite_non_negative(values[value_index]);
}

float strided_float_raw_or_zero(
        const std::vector<float>& values,
        std::size_t index,
        std::size_t stride,
        std::size_t offset) {
    if (stride == 0 || offset >= stride) {
        return 0.0F;
    }
    const auto value_index = index * stride + offset;
    if (value_index >= values.size() || !std::isfinite(values[value_index])) {
        return 0.0F;
    }
    return values[value_index];
}

std::int32_t strided_int_or_zero(
        const std::vector<std::int32_t>& values,
        std::size_t index,
        std::size_t stride,
        std::size_t offset) {
    if (stride == 0 || offset >= stride) {
        return 0;
    }
    const auto value_index = index * stride + offset;
    if (value_index >= values.size()) {
        return 0;
    }
    return values[value_index];
}

void mix_checksum(std::uint64_t& checksum, std::uint64_t value) {
    checksum ^= value;
    checksum *= 1099511628211ULL;
}

float deterministic_unit_interval(std::uint64_t seed) {
    seed ^= seed >> 33U;
    seed *= 0xff51afd7ed558ccdULL;
    seed ^= seed >> 33U;
    seed *= 0xc4ceb9fe1a85ec53ULL;
    seed ^= seed >> 33U;
    return static_cast<float>((seed >> 11U) & 0x1fffffULL) / static_cast<float>(0x1fffffULL);
}

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

std::size_t lighting_payload_category_index(NativeLightingDispatchStage stage) {
    switch (stage) {
        case NativeLightingDispatchStage::DirectLighting:
            return kLightingPayloadCategoryDirect;
        case NativeLightingDispatchStage::DiffuseGi:
            return kLightingPayloadCategoryGi;
        case NativeLightingDispatchStage::Denoise:
        case NativeLightingDispatchStage::Composite:
            return kLightingPayloadCategoryPost;
        case NativeLightingDispatchStage::Cache:
            return kLightingPayloadCategoryCache;
    }

    return kLightingPayloadCategoryDirect;
}

const char* lighting_payload_category_name(std::size_t category_index) {
    switch (category_index) {
        case kLightingPayloadCategoryDirect:
            return "direct";
        case kLightingPayloadCategoryGi:
            return "gi";
        case kLightingPayloadCategoryPost:
            return "post";
        case kLightingPayloadCategoryCache:
            return "cache";
        default:
            return "unknown";
    }
}

bool has_lighting_flag(std::uint32_t flags, std::uint32_t flag) {
    return (flags & flag) != 0;
}

bool lighting_dispatch_ready_for_native_execution(const LightingDispatchStageUpload& dispatch) {
    return dispatch.enabled
        && has_lighting_flag(dispatch.flags, kLightingDispatchFlagValidated)
        && !has_lighting_flag(dispatch.flags, kLightingDispatchFlagPlaceholder);
}

const char* lighting_dispatch_readiness_reason(const LightingDispatchStageUpload& dispatch) {
    if (!dispatch.enabled) {
        return "stage_disabled";
    }
    if (has_lighting_flag(dispatch.flags, kLightingDispatchFlagPlaceholder)
            && has_lighting_flag(dispatch.flags, kLightingDispatchFlagValidated)) {
        return "validated_placeholder_metadata_only";
    }
    if (has_lighting_flag(dispatch.flags, kLightingDispatchFlagPlaceholder)) {
        return "placeholder_metadata_only";
    }
    if (!has_lighting_flag(dispatch.flags, kLightingDispatchFlagValidated)) {
        return "validation_missing";
    }
    return "ready_for_native_execution";
}

void append_stage_name(std::string& names, const LightingDispatchStageUpload& dispatch) {
    if (!names.empty()) {
        names += "|";
    }
    names += dispatch.stage_name.empty() ? to_string(dispatch.stage) : dispatch.stage_name;
}

std::uint64_t absolute_delta(std::uint64_t current, std::uint64_t previous) {
    return current > previous ? current - previous : previous - current;
}

const char* adaptive_budget_bucket_name(
        const LightingDispatchStageUpload& dispatch,
        std::uint64_t rays_per_cell) {
    if (!dispatch.enabled) {
        return "disabled";
    }
    if (has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly)) {
        return "reuse";
    }
    if (rays_per_cell <= 1) {
        return "low";
    }
    if (rays_per_cell == 2) {
        return "medium";
    }
    return "high";
}

std::uint64_t adaptive_scene_state_checksum(
        const LightingDispatchPacket& packet,
        const LightingDispatchStageUpload& dispatch) {
    std::uint64_t checksum = 1469598103934665603ULL;
    mix_checksum(checksum, packet.world_generation);
    mix_checksum(checksum, packet.material_generation);
    mix_checksum(checksum, packet.section_generation);
    mix_checksum(checksum, packet.gbuffer_generation);
    return checksum;
}

std::uint64_t adaptive_dispatch_workgroups(const LightingDispatchStageUpload& dispatch) {
    return saturated_multiply(
            saturated_multiply(
                    non_negative_u64(dispatch.dispatch_x),
                    non_negative_u64(dispatch.dispatch_y)),
            non_negative_u64(dispatch.dispatch_z));
}

void record_adaptive_budget_rejection(
        NativeAdaptiveBudgetTelemetry& telemetry,
        std::uint64_t frame_index,
        const LightingDispatchPacket& packet,
        const LightingDispatchStageUpload& dispatch,
        std::string reason) {
    telemetry.invalid_budget_rejections++;
    telemetry.last_frame_index = frame_index;
    telemetry.last_packet_generation = packet.generation;
    telemetry.last_dispatch_generation = dispatch.generation;
    telemetry.last_invalid_budget_generation = dispatch.generation;
    telemetry.last_ingested = true;
    telemetry.last_valid = false;
    telemetry.last_enabled = dispatch.enabled;
    telemetry.last_cell_count = saturated_multiply(
            non_negative_u64(dispatch.width),
            non_negative_u64(dispatch.height));
    telemetry.last_rays_per_cell = non_negative_u64(dispatch.sample_count);
    telemetry.last_requested_rays = saturated_multiply(
            telemetry.last_cell_count,
            telemetry.last_rays_per_cell);
    telemetry.last_capped_rays = non_negative_u64(dispatch.ray_count);
    telemetry.last_previous_dispatch_workgroups = telemetry.last_dispatch_workgroups;
    telemetry.last_dispatch_workgroups = adaptive_dispatch_workgroups(dispatch);
    telemetry.last_dispatch_delta_workgroups = absolute_delta(
            telemetry.last_dispatch_workgroups,
            telemetry.last_previous_dispatch_workgroups);
    telemetry.last_dispatch_count_changed = telemetry.last_dispatch_delta_workgroups != 0;
    telemetry.last_previous_scene_state_checksum = telemetry.last_scene_state_checksum;
    telemetry.last_scene_state_checksum = adaptive_scene_state_checksum(packet, dispatch);
    telemetry.last_scene_state_changed =
            telemetry.last_previous_scene_state_checksum != 0
            && telemetry.last_previous_scene_state_checksum != telemetry.last_scene_state_checksum;
    telemetry.last_cache_confidence_contribution = 0;
    telemetry.last_variance_contribution = 0;
    telemetry.last_history_accepted_count = 0;
    telemetry.last_history_rejected_count = 0;
    telemetry.last_heatmap_artifact_pixels = 0;
    telemetry.last_reuse_only = has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly);
    telemetry.last_temporal_history = has_lighting_flag(dispatch.flags, kLightingDispatchFlagTemporalHistory);
    telemetry.last_bucket = "invalid";
    telemetry.last_budget_marker = "round8_adaptive_budget_rejected";
    telemetry.last_variance_marker = "round8_variance_confidence_unavailable_invalid_budget";
    telemetry.last_history_confidence_marker = "round8_history_confidence_unavailable_invalid_budget";
    telemetry.last_heatmap_artifact_role = "round8_cpu_metadata_only_invalid_budget_no_heatmap";
    telemetry.last_heatmap_boundary_marker = "native_cpu_telemetry_only_no_gpu_heatmap_render_output";
    telemetry.last_invalid_budget_reason = std::move(reason);
}

void record_adaptive_budget_ingestion(
        NativeAdaptiveBudgetTelemetry& telemetry,
        std::uint64_t frame_index,
        const LightingDispatchPacket& packet,
        const LightingDispatchStageUpload& dispatch) {
    const auto cell_count = saturated_multiply(
            non_negative_u64(dispatch.width),
            non_negative_u64(dispatch.height));
    const auto capped_rays = non_negative_u64(dispatch.ray_count);
    const auto sample_rays_per_cell = non_negative_u64(dispatch.sample_count);
    const auto effective_rays_per_cell = cell_count == 0
            ? sample_rays_per_cell
            : saturated_add(capped_rays / cell_count, (capped_rays % cell_count) == 0 ? 0 : 1);
    const auto rays_per_cell = std::max(sample_rays_per_cell, effective_rays_per_cell);
    const auto requested_rays = std::max(saturated_multiply(cell_count, sample_rays_per_cell), capped_rays);
    const auto* bucket = adaptive_budget_bucket_name(dispatch, rays_per_cell);
    const std::string previous_bucket = telemetry.last_bucket.empty()
            ? "none"
            : telemetry.last_bucket;
    const auto previous_rays = telemetry.last_capped_rays;
    const auto previous_workgroups = telemetry.last_dispatch_workgroups;
    const auto previous_scene_state = telemetry.last_scene_state_checksum;
    const auto workgroups = adaptive_dispatch_workgroups(dispatch);
    const auto scene_state = adaptive_scene_state_checksum(packet, dispatch);
    const bool had_previous = telemetry.packets != 0 && telemetry.last_valid;
    const bool dispatch_count_changed = had_previous && previous_workgroups != workgroups;
    const bool scene_state_changed = had_previous && previous_scene_state != scene_state;
    const bool changed = had_previous
            && (previous_bucket != bucket
                    || previous_rays != capped_rays
                    || dispatch_count_changed
                    || scene_state_changed);
    const auto cache_confidence_cells = dispatch.enabled
            ? std::min(
                    cell_count,
                    saturated_add(
                            non_negative_u64(dispatch.cache_read_count),
                            saturated_multiply(non_negative_u64(dispatch.cache_write_count), 2)))
            : 0;
    const auto variance_cells = dispatch.enabled && !has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly)
            ? std::min(
                    cell_count,
                    saturated_add(
                            saturated_multiply(rays_per_cell, std::max<std::uint64_t>(workgroups, 1)),
                            scene_state_changed ? (cell_count / 8) : 0))
            : 0;

    telemetry.packets++;
    telemetry.last_frame_index = frame_index;
    telemetry.last_packet_generation = packet.generation;
    telemetry.last_dispatch_generation = dispatch.generation;
    telemetry.last_cell_count = cell_count;
    telemetry.last_rays_per_cell = rays_per_cell;
    telemetry.last_requested_rays = requested_rays;
    telemetry.last_capped_rays = capped_rays;
    telemetry.last_budget_delta_rays = had_previous ? absolute_delta(capped_rays, previous_rays) : 0;
    telemetry.last_previous_dispatch_workgroups = previous_workgroups;
    telemetry.last_dispatch_workgroups = workgroups;
    telemetry.last_dispatch_delta_workgroups = had_previous ? absolute_delta(workgroups, previous_workgroups) : 0;
    telemetry.last_previous_scene_state_checksum = previous_scene_state;
    telemetry.last_scene_state_checksum = scene_state;
    telemetry.last_cache_confidence_contribution = cache_confidence_cells;
    telemetry.last_variance_contribution = variance_cells;
    telemetry.last_reuse_bucket_count = 0;
    telemetry.last_low_bucket_count = 0;
    telemetry.last_medium_bucket_count = 0;
    telemetry.last_high_bucket_count = 0;
    telemetry.last_ingested = true;
    telemetry.last_valid = true;
    telemetry.last_enabled = dispatch.enabled;
    telemetry.last_budget_changed = changed;
    telemetry.last_budget_capped = sample_rays_per_cell != 0 && capped_rays < requested_rays;
    telemetry.last_dispatch_count_changed = dispatch_count_changed;
    telemetry.last_scene_state_changed = scene_state_changed;
    telemetry.last_reuse_only = has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly);
    telemetry.last_temporal_history = has_lighting_flag(dispatch.flags, kLightingDispatchFlagTemporalHistory);
    telemetry.last_variance_marker_available = dispatch.enabled && !telemetry.last_reuse_only && rays_per_cell > 0;
    telemetry.last_history_confidence_available = telemetry.last_temporal_history || telemetry.last_reuse_only;
    telemetry.last_previous_bucket = previous_bucket;
    telemetry.last_bucket = bucket;
    telemetry.last_invalid_budget_reason.clear();

    if (changed) {
        telemetry.total_budget_changes++;
    }
    if (telemetry.last_reuse_only) {
        telemetry.last_reuse_bucket_count = cell_count;
        telemetry.total_reuse_bucket_count = saturated_add(telemetry.total_reuse_bucket_count, cell_count);
    } else if (dispatch.enabled && cell_count != 0) {
        const auto reuse_candidates = telemetry.last_history_confidence_available
                ? std::min(cell_count / 3, cache_confidence_cells / 2)
                : 0;
        telemetry.last_reuse_bucket_count = reuse_candidates;
        auto remaining_cells = cell_count - reuse_candidates;
        std::uint64_t high_candidates = 0;
        if (rays_per_cell >= 3) {
            high_candidates = std::max(remaining_cells / 4, variance_cells / 2);
        } else if (scene_state_changed) {
            high_candidates = remaining_cells / 8;
        }
        high_candidates = std::min(remaining_cells, high_candidates);
        telemetry.last_high_bucket_count = high_candidates;
        remaining_cells -= high_candidates;

        std::uint64_t medium_candidates = 0;
        if (rays_per_cell >= 2) {
            medium_candidates = std::max(remaining_cells / 3, variance_cells / 4);
        } else if (telemetry.last_variance_marker_available) {
            medium_candidates = remaining_cells / 6;
        }
        medium_candidates = std::min(remaining_cells, medium_candidates);
        telemetry.last_medium_bucket_count = medium_candidates;
        remaining_cells -= medium_candidates;
        telemetry.last_low_bucket_count = remaining_cells;

        telemetry.total_reuse_bucket_count = saturated_add(
                telemetry.total_reuse_bucket_count,
                telemetry.last_reuse_bucket_count);
        telemetry.total_low_bucket_count = saturated_add(
                telemetry.total_low_bucket_count,
                telemetry.last_low_bucket_count);
        telemetry.total_medium_bucket_count = saturated_add(
                telemetry.total_medium_bucket_count,
                telemetry.last_medium_bucket_count);
        telemetry.total_high_bucket_count = saturated_add(
                telemetry.total_high_bucket_count,
                telemetry.last_high_bucket_count);
    }

    if (telemetry.last_reuse_only) {
        telemetry.last_history_accepted_count = cell_count;
        telemetry.last_history_rejected_count = 0;
    } else if (telemetry.last_temporal_history && cell_count != 0) {
        telemetry.last_history_accepted_count = std::min(
                cell_count,
                saturated_add(
                        telemetry.last_reuse_bucket_count,
                        saturated_add(
                                telemetry.last_low_bucket_count,
                                telemetry.last_medium_bucket_count / 2)));
        telemetry.last_history_rejected_count = cell_count - telemetry.last_history_accepted_count;
    } else {
        telemetry.last_history_accepted_count = 0;
        telemetry.last_history_rejected_count = 0;
    }
    telemetry.total_history_accepted_count = saturated_add(
            telemetry.total_history_accepted_count,
            telemetry.last_history_accepted_count);
    telemetry.total_history_rejected_count = saturated_add(
            telemetry.total_history_rejected_count,
            telemetry.last_history_rejected_count);
    telemetry.last_heatmap_artifact_pixels = dispatch.enabled && dispatch.width > 0 && dispatch.height > 0
            ? cell_count
            : 0;

    telemetry.last_budget_marker = changed
            ? "round8_adaptive_budget_changed"
            : "round8_adaptive_budget_ingested";
    telemetry.last_variance_marker = telemetry.last_variance_marker_available
            ? "round8_variance_budget_marker_available"
            : "round8_variance_budget_marker_unavailable";
    telemetry.last_history_confidence_marker = telemetry.last_history_confidence_available
            ? (telemetry.last_reuse_only
                    ? "round8_history_confidence_reuse_bucket"
                    : "round8_history_confidence_temporal_history")
            : "round8_history_confidence_unavailable";
    telemetry.last_heatmap_artifact_role = dispatch.enabled
            ? (has_lighting_flag(dispatch.flags, kLightingDispatchFlagDebugOverlay)
                    ? "round8_cpu_metadata_ray_budget_variance_cache_history_heatmap_inputs"
                    : "round8_cpu_metadata_heatmap_inputs_debug_overlay_not_requested")
            : "round8_cpu_metadata_heatmap_inputs_disabled";
    telemetry.last_heatmap_boundary_marker = "native_cpu_telemetry_only_no_gpu_heatmap_render_output";
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

namespace {

void append_phase5_payload_categories(
        std::ostringstream& out,
        const NativeLightingDispatchTelemetry& lighting) {
    out << ",payload_categories={";
    for (std::size_t index = 0; index < lighting.payload_categories.size(); index++) {
        if (index != 0) {
            out << ",";
        }
        const auto& category = lighting.payload_categories[index];
        out << lighting_payload_category_name(index)
            << "={stages=" << category.last_stage_count
            << ",enabled=" << category.last_enabled_stage_count
            << ",inputs=" << category.last_input_count
            << ",outputs=" << category.last_output_count
            << ",samples=" << category.last_sample_count
            << ",rays=" << category.last_ray_count
            << ",cache_reads=" << category.last_cache_read_count
            << ",cache_writes=" << category.last_cache_write_count
            << ",enabled_samples=" << category.last_enabled_sample_count
            << ",enabled_rays=" << category.last_enabled_ray_count
            << ",enabled_cache_reads=" << category.last_enabled_cache_read_count
            << ",enabled_cache_writes=" << category.last_enabled_cache_write_count
            << ",total_samples=" << category.total_sample_count
            << ",total_rays=" << category.total_ray_count
            << ",total_cache_reads=" << category.total_cache_read_count
            << ",total_cache_writes=" << category.total_cache_write_count
            << ",placeholder=" << category.last_placeholder_stage_count
            << ",validated=" << category.last_validated_stage_count
            << ",temporal_history=" << category.last_temporal_history_stage_count
            << ",reuse_only=" << category.last_reuse_only_stage_count
            << ",debug_overlay=" << category.last_debug_overlay_stage_count
            << "}";
    }
    out << "}";
}

void append_round6_execution_status(
        std::ostringstream& out,
        const char* label,
        const NativeRound6DispatchExecutionTelemetry& execution) {
    out << "," << label << "={stage=" << to_string(execution.stage)
        << ",attempts=" << execution.attempts
        << ",submitted=" << execution.submitted
        << ",skipped=" << execution.skipped
        << ",accepted=" << execution.accepted
        << ",resource_markers=" << execution.resource_markers
        << ",metadata_dispatches=" << execution.metadata_dispatches
        << ",cache_read_metadata_dispatches=" << execution.cache_read_metadata_dispatches
        << ",cache_write_metadata_dispatches=" << execution.cache_write_metadata_dispatches
        << ",cache_write_markers=" << execution.cache_write_markers
        << ",last_frame=" << execution.last_frame_index
        << ",packet_generation=" << execution.last_packet_generation
        << ",dispatch_generation=" << execution.last_dispatch_generation
        << ",size=" << execution.last_width << "x" << execution.last_height
        << ",groups=" << execution.last_dispatch_x << "x" << execution.last_dispatch_y
        << "x" << execution.last_dispatch_z
        << ",workgroup=" << execution.last_workgroup_size_x << "x" << execution.last_workgroup_size_y
        << "x" << execution.last_workgroup_size_z
        << ",inputs=" << execution.last_input_count
        << ",outputs=" << execution.last_output_count
        << ",samples=" << execution.last_sample_count
        << ",rays=" << execution.last_ray_count
        << ",cache_reads=" << execution.last_cache_read_count
        << ",cache_writes=" << execution.last_cache_write_count
        << ",total_samples=" << execution.total_sample_count
        << ",total_rays=" << execution.total_ray_count
        << ",total_cache_reads=" << execution.total_cache_read_count
        << ",total_cache_writes=" << execution.total_cache_write_count
        << ",placeholder_output_population_count=" << execution.last_placeholder_output_population_count
        << ",total_placeholder_output_population_count=" << execution.total_placeholder_output_population_count
        << ",visible_signal_population_count=" << execution.last_visible_signal_population_count
        << ",total_visible_signal_population_count=" << execution.total_visible_signal_population_count
        << ",visible_signal_sampled_pixels=" << execution.last_visible_signal_sampled_pixels
        << ",visible_signal_nonzero_pixels=" << execution.last_visible_signal_nonzero_pixels
        << ",total_visible_signal_nonzero_pixels=" << execution.total_visible_signal_nonzero_pixels
        << ",cpu_output_size=" << execution.last_cpu_output_width << "x" << execution.last_cpu_output_height
        << ",cpu_output_pixels=" << execution.last_cpu_output_pixel_count
        << ",cpu_output_surface_pixels=" << execution.last_cpu_output_surface_pixel_count
        << ",cpu_output_scene_driven_pixels=" << execution.last_cpu_output_scene_driven_pixel_count
        << ",cpu_output_emissive_driven_pixels=" << execution.last_cpu_output_emissive_driven_pixel_count
        << ",cpu_output_spatial_lobe_pixels=" << execution.last_cpu_output_spatial_lobe_pixel_count
        << ",cpu_output_cache_modulated_pixels=" << execution.last_cpu_output_cache_modulated_pixel_count
        << ",cpu_output_material_modulated_pixels=" << execution.last_cpu_output_material_modulated_pixel_count
        << ",scene_linked_samples=" << execution.last_scene_linked_sample_count
        << ",material_color_modulated_samples=" << execution.last_material_color_modulated_sample_count
        << ",surface_normal_confident_samples=" << execution.last_surface_normal_confident_sample_count
        << ",occlusion_dirty_modulated_samples=" << execution.last_occlusion_dirty_modulated_sample_count
        << ",physical_gi_samples=" << execution.last_physical_gi_sample_count
        << ",physical_gi_hit_samples=" << execution.last_physical_gi_hit_sample_count
        << ",surface_material_hit_coupled_samples="
        << execution.last_surface_material_hit_coupled_sample_count
        << ",geometry_hit_coupled_samples=" << execution.last_geometry_hit_coupled_sample_count
        << ",visible_signal_energy=" << execution.last_visible_signal_energy
        << ",visible_signal_min_sample=" << execution.last_visible_signal_min_sample
        << ",visible_signal_max_sample=" << execution.last_visible_signal_max_sample
        << ",cpu_output_energy=" << execution.last_cpu_output_energy
        << ",scene_linked_energy=" << execution.last_scene_linked_energy
        << ",material_color_influence=" << execution.last_material_color_influence
        << ",surface_normal_confidence=" << execution.last_surface_normal_confidence
        << ",surface_material_hit_coupling=" << execution.last_surface_material_hit_coupling
        << ",geometry_hit_coupling=" << execution.last_geometry_hit_coupling
        << ",emissive_contribution_energy=" << execution.last_emissive_contribution_energy
        << ",sun_contribution_energy=" << execution.last_sun_contribution_energy
        << ",occlusion_dirty_influence=" << execution.last_occlusion_dirty_influence
        << ",output_write_energy=" << execution.last_output_write_energy
        << ",cpu_output_cache_response=" << execution.last_cpu_output_cache_response
        << ",cpu_output_material_response=" << execution.last_cpu_output_material_response
        << ",visible_signal_cache_factor=" << execution.last_visible_signal_cache_factor
        << ",visible_signal_ray_factor=" << execution.last_visible_signal_ray_factor
        << ",physical_scene_link_score=" << execution.last_physical_scene_link_score
        << ",visible_signal_checksum=" << execution.last_visible_signal_checksum
        << ",cpu_output_checksum=" << execution.last_cpu_output_checksum
        << ",physical_output_checksum=" << execution.last_physical_output_checksum
        << ",scene_inputs={recorded=" << execution.last_scene_inputs_recorded
        << ",dimension=\"" << execution.last_scene_dimension_id
        << "\""
        << ",payload_generation=" << execution.last_scene_payload_generation
        << ",celestial_generation=" << execution.last_scene_celestial_generation
        << ",emissive_generation=" << execution.last_scene_emissive_generation
        << ",shadow_generation=" << execution.last_scene_shadow_generation
        << ",shadow_candidate_generation=" << execution.last_scene_shadow_candidate_generation
        << ",section_snapshot_generation=" << execution.last_scene_section_snapshot_generation
        << ",celestial_count=" << execution.last_scene_celestial_light_count
        << ",emissive_count=" << execution.last_scene_emissive_light_count
        << ",shadow_candidates=" << execution.last_scene_shadow_candidate_count
        << ",budgeted_shadow_candidates=" << execution.last_scene_budgeted_shadow_candidate_count
        << ",section_snapshots=" << execution.last_scene_section_snapshot_count
        << ",celestial_energy=" << execution.last_scene_celestial_light_energy
        << ",emissive_energy=" << execution.last_scene_emissive_light_energy
        << "}"
        << ",flags=" << execution.last_flags
        << ",enabled=" << execution.last_enabled
        << ",validated=" << execution.last_validated
        << ",placeholder=" << execution.last_placeholder
        << ",temporal_history=" << execution.last_temporal_history
        << ",reuse_only=" << execution.last_reuse_only
        << ",debug_overlay=" << execution.last_debug_overlay
        << ",ready=" << execution.last_ready
        << ",accepted_this_dispatch=" << execution.last_accepted
        << ",resource_marker_recorded=" << execution.last_resource_marker_recorded
        << ",metadata_dispatch_recorded=" << execution.last_metadata_dispatch_recorded
        << ",cache_read_metadata_dispatch_recorded=" << execution.last_cache_read_metadata_dispatch_recorded
        << ",cache_write_metadata_dispatch_recorded=" << execution.last_cache_write_metadata_dispatch_recorded
        << ",placeholder_output_population_recorded=" << execution.last_placeholder_output_population_recorded
        << ",cache_write_marker_recorded=" << execution.last_cache_write_marker_recorded
        << ",visible_signal_generated=" << execution.last_visible_signal_generated
        << ",visible_signal_cache_backed=" << execution.last_visible_signal_cache_backed
        << ",cpu_output_generated=" << execution.last_cpu_output_generated
        << ",cpu_output_energy_nonzero=" << execution.last_cpu_output_energy_nonzero
        << ",cpu_output_checksum_nonzero=" << execution.last_cpu_output_checksum_nonzero
        << ",cpu_output_nonzero=" << execution.last_cpu_output_nonzero
        << ",cpu_output_marker_recorded=" << execution.last_cpu_output_marker_recorded
        << ",cpu_output_scene_driven=" << execution.last_cpu_output_scene_driven
        << ",cpu_output_emissive_driven=" << execution.last_cpu_output_emissive_driven
        << ",cpu_output_spatially_graded=" << execution.last_cpu_output_spatially_graded
        << ",cpu_output_material_driven=" << execution.last_cpu_output_material_driven
        << ",scene_linked_samples_recorded=" << execution.last_scene_linked_samples_recorded
        << ",material_color_influence_recorded=" << execution.last_material_color_influence_recorded
        << ",surface_normal_confidence_recorded=" << execution.last_surface_normal_confidence_recorded
        << ",physical_gi_samples_recorded=" << execution.last_physical_gi_samples_recorded
        << ",surface_material_hit_coupling_recorded="
        << execution.last_surface_material_hit_coupling_recorded
        << ",geometry_hit_coupling_recorded=" << execution.last_geometry_hit_coupling_recorded
        << ",occlusion_dirty_influence_recorded=" << execution.last_occlusion_dirty_influence_recorded
        << ",output_write_energy_recorded=" << execution.last_output_write_energy_recorded
        << ",physical_scene_linked=" << execution.last_physical_scene_linked
        << ",physical_surface_contribution=" << execution.last_physical_surface_contribution
        << ",preview_fallback_contribution=" << execution.last_preview_fallback_contribution
        << ",focus_window_contribution=" << execution.last_focus_window_contribution
        << ",metadata_only_proof_rejected=" << execution.last_metadata_only_proof_rejected
        << ",focus_window_capture_rejected=" << execution.last_focus_window_capture_rejected
        << ",proof_marker_evidence_rejected=" << execution.last_proof_marker_evidence_rejected
        << ",temporary_direct_substitution_rejected=" << execution.last_temporary_direct_substitution_rejected
        << ",rectangular_washout_rejected=" << execution.last_rectangular_washout_rejected
        << ",marker=\"" << execution.last_marker
        << "\""
        << ",output_marker=\"" << execution.last_output_marker
        << "\""
        << ",cpu_output_marker=\"" << execution.last_cpu_output_marker
        << "\""
        << ",cache_marker=\"" << execution.last_cache_marker
        << "\""
        << ",physical_scene_marker=\"" << execution.last_physical_scene_marker
        << "\""
        << ",physical_output_marker=\"" << execution.last_physical_output_marker
        << "\""
        << ",physical_sample_marker=\"" << execution.last_physical_sample_marker
        << "\""
        << ",surface_material_hit_marker=\"" << execution.last_surface_material_hit_marker
        << "\""
        << ",proof_boundary_marker=\"" << execution.last_proof_boundary_marker
        << "\""
        << ",readiness_reason=\"" << (execution.last_readiness_reason.empty()
            ? "round6_stage_not_evaluated"
            : execution.last_readiness_reason)
        << "\"}";
}

void append_denoise_execution_status(
        std::ostringstream& out,
        const NativeDenoiseExecutionTelemetry& execution) {
    out << ",denoise_execution={attempts=" << execution.attempts
        << ",submitted=" << execution.submitted
        << ",skipped=" << execution.skipped
        << ",accepted=" << execution.accepted
        << ",resource_markers=" << execution.resource_markers
        << ",metadata_dispatches=" << execution.metadata_dispatches
        << ",last_frame=" << execution.last_frame_index
        << ",packet_generation=" << execution.last_packet_generation
        << ",dispatch_generation=" << execution.last_dispatch_generation
        << ",size=" << execution.last_width << "x" << execution.last_height
        << ",inputs=" << execution.last_input_count
        << ",outputs=" << execution.last_output_count
        << ",samples=" << execution.last_sample_count
        << ",history_accepted=" << execution.last_history_accepted
        << ",history_rejected=" << execution.last_history_rejected
        << ",total_history_accepted=" << execution.history_accepted
        << ",total_history_rejected=" << execution.history_rejected
        << ",edge_rejected=" << execution.last_edge_rejected
        << ",edge_preserved=" << execution.last_edge_preserved
        << ",raw_gi_pixels=" << execution.last_raw_gi_pixels
        << ",raw_gi_samples=" << execution.last_raw_gi_samples
        << ",raw_gi_rays=" << execution.last_raw_gi_rays
        << ",raw_gi_cache_reads=" << execution.last_raw_gi_cache_reads
        << ",edge_input_count=" << execution.last_edge_input_count
        << ",history_input_count=" << execution.last_history_input_count
        << ",denoised_output_pixels=" << execution.last_denoised_output_pixels
        << ",previous_denoised_output_checksum=" << execution.last_previous_denoised_output_checksum
        << ",current_denoised_output_checksum=" << execution.last_current_denoised_output_checksum
        << ",denoised_output_checksum=" << execution.last_denoised_output_checksum
        << ",denoised_output_changed_pixels=" << execution.last_denoised_output_changed_pixels
        << ",denoised_output_mean_abs_delta=" << execution.last_denoised_output_mean_abs_delta
        << ",frame_to_frame_changed_pixels=" << execution.last_frame_to_frame_changed_pixels
        << ",frame_to_frame_mean_abs_delta=" << execution.last_frame_to_frame_mean_abs_delta
        << ",shader_denoise_output_image_candidate_size="
        << execution.last_shader_denoise_output_image_candidate_width
        << "x" << execution.last_shader_denoise_output_image_candidate_height
        << ",shader_denoise_output_image_candidate_pixels="
        << execution.last_shader_denoise_output_image_candidate_pixels
        << ",shader_denoise_output_image_candidate_bytes="
        << execution.last_shader_denoise_output_image_candidate_bytes
        << ",shader_denoise_output_image_candidate_checksum="
        << execution.last_shader_denoise_output_image_candidate_checksum
        << ",shader_denoise_output_missing_prerequisite_count="
        << execution.last_shader_denoise_output_missing_prerequisite_count
        << ",temporal_stable_pixels=" << execution.last_temporal_stable_pixels
        << ",temporal_unstable_pixels=" << execution.last_temporal_unstable_pixels
        << ",temporal_mean_abs_delta=" << execution.last_temporal_mean_abs_delta
        << ",temporal_history_confidence=" << execution.last_temporal_history_confidence
        << ",temporal_flicker_score=" << execution.last_temporal_flicker_score
        << ",raw_neighbor_luma_delta=" << execution.last_raw_neighbor_luma_delta
        << ",denoised_neighbor_luma_delta=" << execution.last_denoised_neighbor_luma_delta
        << ",noise_reduction_percent=" << execution.last_noise_reduction_percent
        << ",roughness_noise_reduction_estimate=" << execution.last_noise_reduction_percent
        << ",composite_size=" << execution.last_composite_width << "x" << execution.last_composite_height
        << ",composite_outputs=" << execution.last_composite_outputs
        << ",flags=" << execution.last_flags
        << ",composite_flags=" << execution.last_composite_flags
        << ",enabled=" << execution.last_enabled
        << ",validated=" << execution.last_validated
        << ",placeholder=" << execution.last_placeholder
        << ",temporal_history=" << execution.last_temporal_history
        << ",ready=" << execution.last_ready
        << ",accepted_this_dispatch=" << execution.last_accepted
        << ",resource_marker_recorded=" << execution.last_resource_marker_recorded
        << ",metadata_dispatch_recorded=" << execution.last_metadata_dispatch_recorded
        << ",edge_inputs_available=" << execution.last_edge_inputs_available
        << ",direct_shadow_signal_available=" << execution.last_direct_shadow_signal_available
        << ",diffuse_gi_signal_available=" << execution.last_diffuse_gi_signal_available
        << ",optional_specular_placeholder=" << execution.last_optional_specular_placeholder
        << ",optional_ao_placeholder=" << execution.last_optional_ao_placeholder
        << ",raw_gi_input_available=" << execution.last_raw_gi_input_available
        << ",raw_direct_input_available=" << execution.last_raw_direct_input_available
        << ",raw_gi_input_ready=" << execution.last_raw_gi_input_ready
        << ",denoised_output_intent=" << execution.last_denoised_output_intent
        << ",denoised_cpu_output_generated=" << execution.last_denoised_cpu_output_generated
        << ",cpu_denoised_readback_ready=" << execution.last_cpu_denoised_readback_ready
        << ",denoised_output_differs_from_raw=" << execution.last_denoised_output_differs_from_raw
        << ",raw_gi_source_present=" << execution.last_raw_gi_source_present
        << ",cpu_denoised_source_present=" << execution.last_cpu_denoised_source_present
        << ",shader_denoise_dispatch_intent=" << execution.last_shader_denoise_dispatch_intent
        << ",shader_denoise_dispatch_prepared=" << execution.last_shader_denoise_dispatch_prepared
        << ",shader_denoise_input_ready=" << execution.last_shader_denoise_input_ready
        << ",shader_denoise_output_ready=" << execution.last_shader_denoise_output_ready
        << ",shader_denoise_output_image_ready=" << execution.last_shader_denoise_output_image_ready
        << ",shader_denoise_output_image_candidate_ready="
        << execution.last_shader_denoise_output_image_candidate_ready
        << ",shader_denoise_output_image_candidate_cpu_staged="
        << execution.last_shader_denoise_output_image_candidate_cpu_staged
        << ",shader_denoise_output_image_candidate_non_gpu="
        << execution.last_shader_denoise_output_image_candidate_non_gpu
        << ",shader_denoise_output_image_candidate_concrete="
        << execution.last_shader_denoise_output_image_candidate_concrete
        << ",shader_denoise_output_candidate_source_cpu_readback="
        << execution.last_shader_denoise_output_candidate_source_cpu_readback
        << ",shader_denoise_output_native_image_ready="
        << execution.last_shader_denoise_output_native_image_ready
        << ",shader_denoise_output_native_image_writable="
        << execution.last_shader_denoise_output_native_image_writable
        << ",shader_denoise_output_native_image_shader_written="
        << execution.last_shader_denoise_output_native_image_shader_written
        << ",shader_denoise_output_material_ready=" << execution.last_shader_denoise_output_material_ready
        << ",shader_denoise_output_native_material_ready="
        << execution.last_shader_denoise_output_native_material_ready
        << ",shader_denoise_output_prerequisites_ready="
        << execution.last_shader_denoise_output_prerequisites_ready
        << ",metadata_only_path=" << execution.last_metadata_only_path
        << ",real_denoise_shader_output=" << execution.last_real_denoise_shader_output
        << ",real_shader_output=" << execution.last_real_denoise_shader_output
        << ",shader_denoise_output_shader_generated=" << execution.last_shader_denoise_output_shader_generated
        << ",cpu_fallback_quality_metrics=" << execution.last_cpu_fallback_quality_metrics
        << ",composite_stage_recorded=" << execution.last_composite_stage_recorded
        << ",composite_enabled=" << execution.last_composite_enabled
        << ",composite_ready=" << execution.last_composite_ready
        << ",composite_placeholder=" << execution.last_composite_placeholder
        << ",edge_depth_available=" << execution.last_edge_depth_available
        << ",edge_normal_available=" << execution.last_edge_normal_available
        << ",edge_material_available=" << execution.last_edge_material_available
        << ",history_confidence_available=" << execution.last_history_confidence_available
        << ",temporal_stability_ready=" << execution.last_temporal_stability_ready
        << ",shader_boundary_explicit=" << execution.last_shader_boundary_explicit
        << ",metadata_only_proof_rejected=" << execution.last_metadata_only_proof_rejected
        << ",focus_window_capture_rejected=" << execution.last_focus_window_capture_rejected
        << ",proof_marker_evidence_rejected=" << execution.last_proof_marker_evidence_rejected
        << ",temporary_direct_substitution_rejected=" << execution.last_temporary_direct_substitution_rejected
        << ",rectangular_washout_rejected=" << execution.last_rectangular_washout_rejected
        << ",output_marker=\"" << execution.last_output_marker
        << "\""
        << ",source_identity_marker=\"" << execution.last_source_identity_marker
        << "\""
        << ",raw_input_marker=\"" << execution.last_raw_input_marker
        << "\""
        << ",denoised_output_marker=\"" << execution.last_denoised_output_marker
        << "\""
        << ",shader_denoise_readiness_marker=\"" << execution.last_shader_denoise_readiness_marker
        << "\""
        << ",shader_denoise_handoff_marker=\"" << execution.last_shader_denoise_handoff_marker
        << "\""
        << ",shader_denoise_output_readiness_marker=\"" << execution.last_shader_denoise_output_readiness_marker
        << "\""
        << ",shader_denoise_output_image_candidate_marker=\""
        << execution.last_shader_denoise_output_image_candidate_marker
        << "\""
        << ",shader_denoise_output_candidate_source_marker=\""
        << execution.last_shader_denoise_output_candidate_source_marker
        << "\""
        << ",shader_denoise_output_prerequisite_marker=\""
        << execution.last_shader_denoise_output_prerequisite_marker
        << "\""
        << ",shader_denoise_output_missing_prerequisites=\""
        << execution.last_shader_denoise_output_missing_prerequisites
        << "\""
        << ",shader_denoise_output_image_blocker=\""
        << execution.last_shader_denoise_output_image_blocker
        << "\""
        << ",shader_denoise_generation_marker=\"" << execution.last_shader_denoise_generation_marker
        << "\""
        << ",composite_marker=\"" << execution.last_composite_marker
        << "\""
        << ",history_acceptance_reason=\"" << execution.last_history_acceptance_reason
        << "\""
        << ",history_rejection_reason=\"" << execution.last_history_rejection_reason
        << "\""
        << ",shader_boundary_marker=\"" << execution.last_shader_boundary_marker
        << "\""
        << ",temporal_history_marker=\"" << execution.last_temporal_history_marker
        << "\""
        << ",temporal_stability_readiness_marker=\""
        << execution.last_temporal_stability_readiness_marker
        << "\""
        << ",temporal_ghosting_risk_marker=\"" << execution.last_temporal_ghosting_risk_marker
        << "\""
        << ",proof_boundary_marker=\"" << execution.last_proof_boundary_marker
        << "\""
        << ",quality_marker=\"" << execution.last_quality_marker
        << "\""
        << ",readiness_reason=\"" << (execution.last_readiness_reason.empty()
            ? "denoise_stage_not_evaluated"
            : execution.last_readiness_reason)
        << "\"}";
}

void append_adaptive_budget_status(
        std::ostringstream& out,
        const NativeAdaptiveBudgetTelemetry& budget) {
    out << ",round8_adaptive_budget={packets=" << budget.packets
        << ",last_frame=" << budget.last_frame_index
        << ",packet_generation=" << budget.last_packet_generation
        << ",dispatch_generation=" << budget.last_dispatch_generation
        << ",ingested=" << budget.last_ingested
        << ",valid=" << budget.last_valid
        << ",enabled=" << budget.last_enabled
        << ",bucket=\"" << budget.last_bucket
        << "\""
        << ",previous_bucket=\"" << budget.last_previous_bucket
        << "\""
        << ",cells=" << budget.last_cell_count
        << ",rays_per_cell=" << budget.last_rays_per_cell
        << ",requested_rays=" << budget.last_requested_rays
        << ",capped_rays=" << budget.last_capped_rays
        << ",budget_capped=" << budget.last_budget_capped
        << ",budget_changed=" << budget.last_budget_changed
        << ",budget_delta_rays=" << budget.last_budget_delta_rays
        << ",dispatch_workgroups=" << budget.last_dispatch_workgroups
        << ",previous_dispatch_workgroups=" << budget.last_previous_dispatch_workgroups
        << ",dispatch_delta_workgroups=" << budget.last_dispatch_delta_workgroups
        << ",dispatch_count_changed=" << budget.last_dispatch_count_changed
        << ",scene_state_checksum=" << budget.last_scene_state_checksum
        << ",previous_scene_state_checksum=" << budget.last_previous_scene_state_checksum
        << ",scene_state_changed=" << budget.last_scene_state_changed
        << ",cache_confidence_contribution=" << budget.last_cache_confidence_contribution
        << ",variance_contribution=" << budget.last_variance_contribution
        << ",history_accepted=" << budget.last_history_accepted_count
        << ",history_rejected=" << budget.last_history_rejected_count
        << ",total_history_accepted=" << budget.total_history_accepted_count
        << ",total_history_rejected=" << budget.total_history_rejected_count
        << ",total_budget_changes=" << budget.total_budget_changes
        << ",bucket_counts={reuse=" << budget.last_reuse_bucket_count
        << ",low=" << budget.last_low_bucket_count
        << ",medium=" << budget.last_medium_bucket_count
        << ",high=" << budget.last_high_bucket_count
        << "}"
        << ",total_bucket_counts={reuse=" << budget.total_reuse_bucket_count
        << ",low=" << budget.total_low_bucket_count
        << ",medium=" << budget.total_medium_bucket_count
        << ",high=" << budget.total_high_bucket_count
        << "}"
        << ",reuse_only=" << budget.last_reuse_only
        << ",temporal_history=" << budget.last_temporal_history
        << ",variance_marker_available=" << budget.last_variance_marker_available
        << ",history_confidence_available=" << budget.last_history_confidence_available
        << ",invalid_budget_rejections=" << budget.invalid_budget_rejections
        << ",last_invalid_budget_generation=" << budget.last_invalid_budget_generation
        << ",budget_marker=\"" << budget.last_budget_marker
        << "\""
        << ",variance_marker=\"" << budget.last_variance_marker
        << "\""
        << ",history_confidence_marker=\"" << budget.last_history_confidence_marker
        << "\""
        << ",heatmap_artifact={pixels=" << budget.last_heatmap_artifact_pixels
        << ",role=\"" << budget.last_heatmap_artifact_role
        << "\""
        << ",boundary=\"" << budget.last_heatmap_boundary_marker
        << "\"}"
        << ",invalid_budget_reason=\"" << budget.last_invalid_budget_reason
        << "\"}";
}

void append_round11_restir_status(
        std::ostringstream& out,
        const NativeRound11RestirTelemetry& restir) {
    out << ",round11_restir={metadata_packets=" << restir.metadata_packets
        << ",last_frame=" << restir.last_frame_index
        << ",packet_generation=" << restir.last_packet_generation
        << ",dispatch_generation=" << restir.last_dispatch_generation
        << ",direct_payload_generation=" << restir.last_direct_payload_generation
        << ",direct_reservoir_count=" << restir.direct_reservoir_count
        << ",candidate_count=" << restir.candidate_count
        << ",selected_light_count=" << restir.selected_light_count
        << ",temporal_reuse_count=" << restir.temporal_reuse_count
        << ",spatial_reuse_count=" << restir.spatial_reuse_count
        << ",gi_reservoir_count=" << restir.gi_reservoir_count
        << ",path_reuse_count=" << restir.path_reuse_count
        << ",invalidated_reservoir_count=" << restir.invalidated_reservoir_count
        << ",restir_di_candidate_count=" << restir.restir_di_candidate_count
        << ",restir_di_selected_count=" << restir.restir_di_selected_count
        << ",restir_di_candidate_reduction_ratio=" << restir.restir_di_candidate_reduction_ratio
        << ",restir_di_temporal_reuse_count=" << restir.restir_di_temporal_reuse_count
        << ",restir_di_spatial_reuse_count=" << restir.restir_di_spatial_reuse_count
        << ",restir_di_output_energy=" << restir.restir_di_output_energy
        << ",restir_di_output_checksum=" << restir.restir_di_output_checksum
        << ",confidence={samples=" << restir.confidence_sample_count
        << ",min=" << restir.confidence_min
        << ",mean=" << restir.confidence_mean
        << ",max=" << restir.confidence_max
        << ",marker=\"" << (restir.confidence_marker.empty()
            ? "round11_confidence_metadata_not_recorded"
            : restir.confidence_marker)
        << "\"}"
        << ",metadata_only=" << restir.metadata_only
        << ",real_restir_execution=" << restir.real_restir_execution
        << ",realRestirDiExecution=" << restir.real_restir_di_execution
        << ",source_marker=\"" << (restir.source_marker.empty()
            ? "round11_restir_source_metadata_not_recorded"
            : restir.source_marker)
        << "\""
        << ",boundary=\"" << (restir.boundary_marker.empty()
            ? "native_round11_metadata_only_no_real_restir_execution"
            : restir.boundary_marker)
        << "\"}";
}

void append_phase5_lighting_status(
        std::ostringstream& out,
        const NativeLightingDispatchTelemetry& lighting,
        const LightingDispatchPacket& last_packet) {
    std::array<const LightingDispatchStageUpload*, kNativeLightingDispatchStageCount> stages{};
    for (const auto& dispatch : last_packet.dispatches) {
        const auto index = lighting_stage_index(dispatch.stage);
        if (index >= stages.size()) {
            continue;
        }
        stages[index] = &dispatch;
    }

    const auto readiness_reason = lighting.last_readiness_reason.empty()
        ? "no_phase5_dispatch_payload"
        : lighting.last_readiness_reason;
    out << " phase5_lighting={packet_count=" << lighting.packets
        << ",packet_generation=" << lighting.last_packet_generation
        << ",dispatch_generation=" << lighting.last_first_generation << "-" << lighting.last_generation
        << ",world_generation=" << lighting.last_world_generation
        << ",material_generation=" << lighting.last_material_generation
        << ",section_generation=" << lighting.last_section_generation
        << ",gbuffer_generation=" << lighting.last_gbuffer_generation
        << ",enabled_stage_count=" << lighting.last_enabled_stage_count
        << ",disabled_stage_count=" << lighting.last_disabled_stage_count
        << ",enabled_stage_total=" << lighting.enabled_dispatches
        << ",disabled_stage_total=" << lighting.disabled_dispatches
        << ",enabled_stage_names=\"" << lighting.last_enabled_stage_names
        << "\""
        << ",payload_totals={inputs=" << lighting.last_input_count
        << ",outputs=" << lighting.last_output_count
        << ",samples=" << lighting.last_sample_count
        << ",rays=" << lighting.last_ray_count
        << ",cache_reads=" << lighting.last_cache_read_count
        << ",cache_writes=" << lighting.last_cache_write_count
        << "}"
        << ",enabled_payload_totals={samples=" << lighting.last_enabled_sample_count
        << ",rays=" << lighting.last_enabled_ray_count
        << ",cache_reads=" << lighting.last_enabled_cache_read_count
        << ",cache_writes=" << lighting.last_enabled_cache_write_count
        << "}"
        << ",cumulative_payload_totals={samples=" << lighting.total_sample_count
        << ",rays=" << lighting.total_ray_count
        << ",cache_reads=" << lighting.total_cache_read_count
        << ",cache_writes=" << lighting.total_cache_write_count
        << "}"
        << ",flag_counts={placeholder=" << lighting.last_placeholder_stage_count
        << ",validated=" << lighting.last_validated_stage_count
        << ",temporal_history=" << lighting.last_temporal_history_stage_count
        << ",reuse_only=" << lighting.last_reuse_only_stage_count
        << ",debug_overlay=" << lighting.last_debug_overlay_stage_count
        << "}"
        << ",flag_totals={placeholder=" << lighting.total_placeholder_stage_count
        << ",validated=" << lighting.total_validated_stage_count
        << ",temporal_history=" << lighting.total_temporal_history_stage_count
        << ",reuse_only=" << lighting.total_reuse_only_stage_count
        << ",debug_overlay=" << lighting.total_debug_overlay_stage_count
        << "}"
        << ",feature_flags={placeholder=" << lighting.last_has_placeholder_stage
        << ",validated=" << lighting.last_has_validated_stage
        << ",temporal_history=" << lighting.last_has_temporal_history_stage
        << ",reuse_only=" << lighting.last_has_reuse_only_stage
        << ",debug_overlay=" << lighting.last_has_debug_overlay_stage
        << "}"
        << ",readiness={ready_for_native_execution=" << lighting.last_ready_for_native_execution
        << ",reason=\"" << readiness_reason
        << "\"}"
        << ",direct_execution={attempts=" << lighting.direct_execution.attempts
        << ",payload_packets=" << lighting.direct_execution.payload_packets
        << ",submitted=" << lighting.direct_execution.submitted
        << ",skipped=" << lighting.direct_execution.skipped
        << ",last_frame=" << lighting.direct_execution.last_frame_index
        << ",payload_accepted=" << lighting.direct_execution.last_payload_accepted
        << ",payload_frame=" << lighting.direct_execution.last_payload_frame_index
        << ",payload_generation=" << lighting.direct_execution.last_payload_generation
        << ",payload_generation_range=" << lighting.direct_execution.last_payload_first_generation
        << "-" << lighting.direct_execution.last_payload_generation_end
        << ",payload_dimension=\"" << lighting.direct_execution.last_payload_dimension_id
        << "\""
        << ",payload_flags=" << lighting.direct_execution.last_payload_flags
        << ",payload_validated=" << lighting.direct_execution.last_payload_validated
        << ",payload_has_direct_work=" << lighting.direct_execution.last_payload_has_direct_work
        << ",payload_ready_for_shadow_tracing=" << lighting.direct_execution.last_payload_ready_for_shadow_tracing
        << ",celestial_generation=" << lighting.direct_execution.last_payload_celestial_generation
        << ",emissive_generation=" << lighting.direct_execution.last_payload_emissive_generation
        << ",shadow_generation=" << lighting.direct_execution.last_payload_shadow_generation
        << ",shadow_candidate_generation=" << lighting.direct_execution.last_payload_shadow_candidate_generation
        << ",section_snapshot_generation=" << lighting.direct_execution.last_payload_section_snapshot_generation
        << ",packet_generation=" << lighting.direct_execution.last_packet_generation
        << ",dispatch_generation=" << lighting.direct_execution.last_dispatch_generation
        << ",celestial_count=" << lighting.direct_execution.last_celestial_light_count
        << ",emissive_count=" << lighting.direct_execution.last_emissive_light_count
        << ",shadow_candidate_count=" << lighting.direct_execution.last_shadow_candidate_count
        << ",budgeted_shadow_candidate_count=" << lighting.direct_execution.last_budgeted_shadow_candidate_count
        << ",section_snapshot_count=" << lighting.direct_execution.last_section_snapshot_count
        << ",celestial_energy=" << lighting.direct_execution.last_celestial_light_energy
        << ",emissive_energy=" << lighting.direct_execution.last_emissive_light_energy
        << ",candidate_count=" << lighting.direct_execution.last_candidate_count
        << ",sample_count=" << lighting.direct_execution.last_sample_count
        << ",ray_count=" << lighting.direct_execution.last_ray_count
        << ",output_count=" << lighting.direct_execution.last_output_count
        << ",output_width=" << lighting.direct_execution.last_output_width
        << ",output_height=" << lighting.direct_execution.last_output_height
        << ",output_pixels=" << lighting.direct_execution.last_output_pixel_count
        << ",output_energy=" << lighting.direct_execution.last_output_energy
        << ",output_min_sample=" << lighting.direct_execution.last_output_min_sample
        << ",output_max_sample=" << lighting.direct_execution.last_output_max_sample
        << ",output_checksum=" << lighting.direct_execution.last_output_checksum
        << ",surface_payload_samples=" << lighting.direct_execution.last_surface_payload_sample_count
        << ",surface_payload_pixels=" << lighting.direct_execution.last_surface_payload_pixel_count
        << ",material_surface_pixels=" << lighting.direct_execution.last_material_surface_pixel_count
        << ",preview_fallback_pixels=" << lighting.direct_execution.last_preview_fallback_pixel_count
        << ",physical_surface_energy=" << lighting.direct_execution.last_physical_surface_energy
        << ",preview_fallback_energy=" << lighting.direct_execution.last_preview_fallback_energy
        << ",surface_payload_confidence=" << lighting.direct_execution.last_surface_payload_confidence
        << ",total_celestial=" << lighting.direct_execution.total_celestial_light_count
        << ",total_emissive=" << lighting.direct_execution.total_emissive_light_count
        << ",total_shadow_candidates=" << lighting.direct_execution.total_shadow_candidate_count
        << ",total_candidates=" << lighting.direct_execution.total_candidate_count
        << ",total_samples=" << lighting.direct_execution.total_sample_count
        << ",total_rays=" << lighting.direct_execution.total_ray_count
        << ",ray_budget={ingested=" << lighting.direct_execution.last_ray_budget_ingested
        << ",valid=" << lighting.direct_execution.last_ray_budget_valid
        << ",primary_rays_per_pixel=" << lighting.direct_execution.last_ray_budget_primary_rays_per_pixel
        << ",shadow_rays_per_hit=" << lighting.direct_execution.last_ray_budget_shadow_rays_per_hit
        << ",gi_rays_per_hit=" << lighting.direct_execution.last_ray_budget_gi_rays_per_hit
        << ",max_rays_per_frame=" << lighting.direct_execution.last_ray_budget_max_rays_per_frame
        << ",max_visited_voxels_per_ray=" << lighting.direct_execution.last_ray_budget_max_visited_voxels_per_ray
        << ",max_visited_sections_per_ray=" << lighting.direct_execution.last_ray_budget_max_visited_sections_per_ray
        << ",invalid_rejections=" << lighting.direct_execution.invalid_ray_budget_rejections
        << ",marker=\"" << lighting.direct_execution.last_ray_budget_marker
        << "\""
        << ",rejection_reason=\"" << lighting.direct_execution.last_ray_budget_rejection_reason
        << "\"}"
        << ",output_writes=" << lighting.direct_execution.output_writes
        << ",resolves=" << lighting.direct_execution.resolves
        << ",enabled=" << lighting.direct_execution.last_enabled
        << ",ready=" << lighting.direct_execution.last_ready
        << ",metadata_only=" << lighting.direct_execution.last_metadata_only
        << ",cpu_output_generated=" << lighting.direct_execution.last_cpu_output_generated
        << ",output_write_recorded=" << lighting.direct_execution.last_output_write_recorded
        << ",resolve_recorded=" << lighting.direct_execution.last_resolve_recorded
        << ",physical_surface_contribution=" << lighting.direct_execution.last_physical_surface_contribution
        << ",preview_fallback_contribution=" << lighting.direct_execution.last_preview_fallback_contribution
        << ",focus_window_contribution=" << lighting.direct_execution.last_focus_window_contribution
        << ",output_marker=\"" << lighting.direct_execution.last_output_marker
        << "\""
        << ",readiness_reason=\"" << (lighting.direct_execution.last_readiness_reason.empty()
            ? "direct_stage_not_evaluated"
            : lighting.direct_execution.last_readiness_reason)
        << "\"}";
    append_round6_execution_status(out, "diffuse_gi_execution", lighting.diffuse_gi_execution);
    append_round6_execution_status(out, "cache_execution", lighting.cache_execution);
    append_denoise_execution_status(out, lighting.denoise_execution);
    append_adaptive_budget_status(out, lighting.adaptive_budget);
    append_round11_restir_status(out, lighting.round11_restir);
    append_phase5_payload_categories(out, lighting);
    out
        << ",total_estimated_bytes=" << lighting.total_estimated_bytes
        << "} phase5_lighting_stages=[";
    bool wrote_stage = false;
    for (std::size_t index = 0; index < stages.size(); index++) {
        const auto* dispatch = stages[index];
        if (dispatch == nullptr) {
            continue;
        }
        if (wrote_stage) {
            out << "; ";
        }
        const auto& stage = lighting.stages[index];
        out << "{id=" << to_string(dispatch->stage)
            << ",enabled=" << dispatch->enabled
            << ",generation=" << dispatch->generation
            << ",size=" << dispatch->width << "x" << dispatch->height
            << ",groups=" << dispatch->dispatch_x << "x" << dispatch->dispatch_y << "x" << dispatch->dispatch_z
            << ",workgroup=" << dispatch->workgroup_size_x << "x" << dispatch->workgroup_size_y
            << "x" << dispatch->workgroup_size_z
            << ",inputs=" << dispatch->input_count
            << ",outputs=" << dispatch->output_count
            << ",samples=" << dispatch->sample_count
            << ",rays=" << dispatch->ray_count
            << ",cache_read=" << dispatch->cache_read_count
            << ",cache_write=" << dispatch->cache_write_count
            << ",flags=" << dispatch->flags
            << ",placeholder=" << stage.last_placeholder
            << ",validated=" << stage.last_validated
            << ",temporal_history=" << stage.last_temporal_history
            << ",reuse_only=" << stage.last_reuse_only
            << ",debug_overlay=" << stage.last_debug_overlay
            << ",ready_for_native_execution=" << stage.ready_for_native_execution_this_packet
            << ",readiness_reason=\"" << stage.last_readiness_reason
            << "\""
            << ",recorded_this_frame=" << stage.recorded_this_frame
            << "}";
        wrote_stage = true;
    }
    out << "]";
}

} // namespace

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
    last_direct_lighting_payload_packet_ = {};
    direct_lighting_cpu_output_.clear();
    diffuse_gi_cpu_output_.clear();
    denoised_diffuse_gi_cpu_output_rgba8_.clear();
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    section_upload_packet_count_ = 0;
    gbuffer_staging_packet_count_ = 0;
    lighting_dispatch_packet_count_ = 0;
    direct_lighting_payload_packet_count_ = 0;
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
    last_direct_lighting_payload_packet_ = {};
    direct_lighting_cpu_output_.clear();
    diffuse_gi_cpu_output_.clear();
    denoised_diffuse_gi_cpu_output_rgba8_.clear();
    last_tick_delta_ = 0.0F;
    resize_count_ = 0;
    begin_frame_count_ = 0;
    end_frame_count_ = 0;
    upload_packet_count_ = 0;
    section_upload_packet_count_ = 0;
    gbuffer_staging_packet_count_ = 0;
    lighting_dispatch_packet_count_ = 0;
    direct_lighting_payload_packet_count_ = 0;
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

        if (dispatch.stage == NativeLightingDispatchStage::DiffuseGi) {
            const bool reuse_only = has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly);
            const auto capped_rays = non_negative_u64(dispatch.ray_count);

            if (dispatch.enabled && reuse_only && capped_rays != 0) {
                record_adaptive_budget_rejection(
                        staging_.lighting.adaptive_budget,
                        frame_index_,
                        packet,
                        dispatch,
                        "reuse-only diffuse GI budget must not request rays");
                fail("reuse-only diffuse GI budget must not request rays");
            }
            if (dispatch.enabled && !reuse_only && dispatch.sample_count == 0 && capped_rays != 0) {
                record_adaptive_budget_rejection(
                        staging_.lighting.adaptive_budget,
                        frame_index_,
                        packet,
                        dispatch,
                        "diffuse GI budget cannot request rays with zero rays per cell");
                fail("diffuse GI budget cannot request rays with zero rays per cell");
            }
        }

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

void Renderer::upload_direct_lighting_payload(DirectLightingPayloadPacket packet) {
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
    auto require_non_negative = [&fail](std::int32_t value, const char* name) {
        if (value < 0) {
            fail(std::string(name) + " must be non-negative");
        }
    };
    auto require_length = [&fail](std::size_t expected, std::size_t actual, const char* name) {
        if (expected != actual) {
            std::ostringstream error;
            error << name << " length must be " << expected << " but was " << actual;
            fail(error.str());
        }
    };
    auto checked_count = [&require_non_negative](std::int32_t count, const char* name) {
        require_non_negative(count, name);
        return static_cast<std::size_t>(count);
    };

    if (packet.first_generation > packet.last_generation) {
        fail("direct lighting payload generation bounds are invalid");
    }
    if (packet.generation < packet.last_generation) {
        fail("direct lighting payload generation must include payload generation bounds");
    }
    if ((packet.flags & ~kDirectPayloadKnownFlags) != 0) {
        fail("direct lighting payload flags contain unknown bits");
    }
    require_text(packet.dimension_id, "direct lighting dimension id");

    const auto celestial_count = checked_count(packet.celestial_light_count, "direct celestial light count");
    const auto emissive_count = checked_count(packet.selected_emissive_count, "direct emissive light count");
    const auto shadow_count = checked_count(packet.shadow_candidate_count, "direct shadow candidate count");
    const auto section_count = checked_count(packet.section_snapshot_count, "direct section snapshot count");
    require_non_negative(packet.budgeted_shadow_candidate_count, "direct budgeted shadow candidate count");
    if (packet.budgeted_shadow_candidate_count > packet.shadow_candidate_count) {
        fail("direct budgeted shadow candidate count cannot exceed shadow candidate count");
    }

    auto& direct_execution = staging_.lighting.direct_execution;
    if (packet.ray_budget.size() != kDirectRayBudgetStride) {
        direct_execution.invalid_ray_budget_rejections++;
        direct_execution.last_ray_budget_ingested = true;
        direct_execution.last_ray_budget_valid = false;
        direct_execution.last_ray_budget_marker = "round8_direct_ray_budget_rejected";
        direct_execution.last_ray_budget_rejection_reason = "direct ray budget length is invalid";
        fail("direct ray budget length is invalid");
    }
    auto reject_ray_budget = [&direct_execution, &fail](const char* reason) {
        direct_execution.invalid_ray_budget_rejections++;
        direct_execution.last_ray_budget_ingested = true;
        direct_execution.last_ray_budget_valid = false;
        direct_execution.last_ray_budget_marker = "round8_direct_ray_budget_rejected";
        direct_execution.last_ray_budget_rejection_reason = reason;
        fail(reason);
    };
    if (packet.ray_budget[kDirectRayBudgetPrimaryRaysPerPixelOffset] < 0) {
        reject_ray_budget("direct primary rays per pixel must be non-negative");
    }
    if (packet.ray_budget[kDirectRayBudgetShadowRaysPerHitOffset] < 0) {
        reject_ray_budget("direct shadow rays per hit must be non-negative");
    }
    if (packet.ray_budget[kDirectRayBudgetGiRaysPerHitOffset] < 0) {
        reject_ray_budget("direct GI rays per hit must be non-negative");
    }
    if (packet.ray_budget[kDirectRayBudgetMaxRaysPerFrameOffset] <= 0) {
        reject_ray_budget("direct max rays per frame must be positive");
    }
    if (packet.ray_budget[kDirectRayBudgetMaxVisitedVoxelsPerRayOffset] <= 0) {
        reject_ray_budget("direct max visited voxels per ray must be positive");
    }
    if (packet.ray_budget[kDirectRayBudgetMaxVisitedSectionsPerRayOffset] <= 0) {
        reject_ray_budget("direct max visited sections per ray must be positive");
    }
    require_length(celestial_count, packet.celestial_light_sources.size(), "direct celestial light sources");
    require_length(celestial_count, packet.celestial_light_flags.size(), "direct celestial light flags");
    require_length(celestial_count * kDirectCelestialLightDataStride, packet.celestial_light_data.size(), "direct celestial light data");
    require_length(emissive_count, packet.emissive_light_dimensions.size(), "direct emissive light dimensions");
    require_length(emissive_count * kDirectEmissiveLightMetadataStride, packet.emissive_light_metadata.size(), "direct emissive light metadata");
    require_length(emissive_count * kDirectEmissiveLightDataStride, packet.emissive_light_data.size(), "direct emissive light data");
    require_length(emissive_count, packet.emissive_light_generations.size(), "direct emissive light generations");
    require_length(shadow_count * kDirectShadowCandidateMetadataStride, packet.shadow_candidate_metadata.size(), "direct shadow candidate metadata");
    require_length(shadow_count * kDirectShadowCandidateRayStride, packet.shadow_candidate_rays.size(), "direct shadow candidate rays");
    require_length(shadow_count, packet.shadow_candidate_generations.size(), "direct shadow candidate generations");
    require_length(section_count, packet.section_snapshot_dimensions.size(), "direct section snapshot dimensions");
    require_length(section_count * kDirectSectionSnapshotMetadataStride, packet.section_snapshot_metadata.size(), "direct section snapshot metadata");
    require_length(section_count * kDirectSectionSnapshotGenerationStride, packet.section_snapshot_generations.size(), "direct section snapshot generations");

    for (const auto& dimension : packet.emissive_light_dimensions) {
        require_text(dimension, "direct emissive light dimensions");
    }
    for (const auto& dimension : packet.section_snapshot_dimensions) {
        require_text(dimension, "direct section snapshot dimensions");
    }

    direct_lighting_payload_packet_count_++;
    auto& execution = staging_.lighting.direct_execution;
    execution.payload_packets++;
    execution.last_payload_accepted = true;
    execution.last_payload_frame_index = packet.frame_index;
    execution.last_payload_generation = packet.generation;
    execution.last_payload_first_generation = packet.first_generation;
    execution.last_payload_generation_end = packet.last_generation;
    execution.last_payload_celestial_generation = packet.celestial_generation;
    execution.last_payload_emissive_generation = packet.emissive_generation;
    execution.last_payload_shadow_generation = packet.shadow_generation;
    execution.last_payload_shadow_candidate_generation = packet.shadow_candidate_generation;
    execution.last_payload_section_snapshot_generation = packet.section_snapshot_generation;
    execution.last_payload_dimension_id = packet.dimension_id;
    execution.last_payload_flags = packet.flags;
    execution.last_payload_validated = has_lighting_flag(packet.flags, kDirectPayloadFlagValidated);
    execution.last_payload_has_direct_work = has_lighting_flag(packet.flags, kDirectPayloadFlagHasDirectLightingWork);
    execution.last_payload_ready_for_shadow_tracing = has_lighting_flag(packet.flags, kDirectPayloadFlagReadyForShadowTracing);
    execution.last_celestial_light_count = celestial_count;
    execution.last_emissive_light_count = emissive_count;
    execution.last_shadow_candidate_count = shadow_count;
    execution.last_budgeted_shadow_candidate_count = static_cast<std::uint64_t>(packet.budgeted_shadow_candidate_count);
    execution.last_section_snapshot_count = section_count;
    execution.last_ray_budget_ingested = true;
    execution.last_ray_budget_valid = true;
    execution.last_ray_budget_primary_rays_per_pixel = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetPrimaryRaysPerPixelOffset]);
    execution.last_ray_budget_shadow_rays_per_hit = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetShadowRaysPerHitOffset]);
    execution.last_ray_budget_gi_rays_per_hit = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetGiRaysPerHitOffset]);
    execution.last_ray_budget_max_rays_per_frame = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetMaxRaysPerFrameOffset]);
    execution.last_ray_budget_max_visited_voxels_per_ray = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetMaxVisitedVoxelsPerRayOffset]);
    execution.last_ray_budget_max_visited_sections_per_ray = static_cast<std::uint64_t>(
            packet.ray_budget[kDirectRayBudgetMaxVisitedSectionsPerRayOffset]);
    execution.last_ray_budget_marker = "round8_direct_ray_budget_ingested";
    execution.last_ray_budget_rejection_reason.clear();
    execution.last_celestial_light_energy = packet.celestial_light_energy;
    execution.last_emissive_light_energy = packet.selected_emissive_energy;
    execution.total_celestial_light_count = saturated_add(execution.total_celestial_light_count, celestial_count);
    execution.total_emissive_light_count = saturated_add(execution.total_emissive_light_count, emissive_count);
    execution.total_shadow_candidate_count = saturated_add(execution.total_shadow_candidate_count, shadow_count);
    execution.last_metadata_only = true;
    last_direct_lighting_payload_packet_ = std::move(packet);
    track_round11_restir_metadata();
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
    auto lighting_placeholder_resources = track_noop_lighting_placeholder();
    lighting_placeholder_resources += track_direct_lighting_execution_scaffold();
    lighting_placeholder_resources += track_round6_dispatch_execution_scaffold(
            NativeLightingDispatchStage::DiffuseGi,
            staging_.lighting.diffuse_gi_execution,
            "diffuse_gi_dispatch_accepted_metadata_marker");
    lighting_placeholder_resources += track_denoise_execution_scaffold();
    lighting_placeholder_resources += track_round6_dispatch_execution_scaffold(
            NativeLightingDispatchStage::Cache,
            staging_.lighting.cache_execution,
            "lighting_cache_dispatch_accepted_metadata_marker");
    track_round11_restir_metadata();
    mark_pass_submitted(NativeRenderPass::NoopLighting, lighting_placeholder_resources);
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

std::vector<std::uint8_t> Renderer::direct_lighting_cpu_output_preview_rgba8() const {
    if (!initialized_ || direct_lighting_cpu_output_.empty()) {
        return {};
    }
    if (direct_lighting_cpu_output_.size() % 4 != 0) {
        return {};
    }
    const auto expected_components = static_cast<std::size_t>(
            staging_.lighting.direct_execution.last_output_pixel_count * 4);
    if (expected_components != direct_lighting_cpu_output_.size()) {
        return {};
    }

    float max_channel = 0.0F;
    for (std::size_t index = 0; index + 3 < direct_lighting_cpu_output_.size(); index += 4) {
        max_channel = std::max(max_channel, finite_non_negative(direct_lighting_cpu_output_[index]));
        max_channel = std::max(max_channel, finite_non_negative(direct_lighting_cpu_output_[index + 1]));
        max_channel = std::max(max_channel, finite_non_negative(direct_lighting_cpu_output_[index + 2]));
    }
    if (max_channel <= 0.0F) {
        return {};
    }

    std::vector<std::uint8_t> rgba8;
    rgba8.resize(direct_lighting_cpu_output_.size());
    const float scale = 255.0F / max_channel;
    for (std::size_t index = 0; index + 3 < direct_lighting_cpu_output_.size(); index += 4) {
        rgba8[index] = static_cast<std::uint8_t>(std::clamp(
                finite_non_negative(direct_lighting_cpu_output_[index]) * scale,
                0.0F,
                255.0F));
        rgba8[index + 1] = static_cast<std::uint8_t>(std::clamp(
                finite_non_negative(direct_lighting_cpu_output_[index + 1]) * scale,
                0.0F,
                255.0F));
        rgba8[index + 2] = static_cast<std::uint8_t>(std::clamp(
                finite_non_negative(direct_lighting_cpu_output_[index + 2]) * scale,
                0.0F,
                255.0F));
        rgba8[index + 3] = static_cast<std::uint8_t>(std::clamp(
                finite_non_negative(direct_lighting_cpu_output_[index + 3]) * 255.0F,
                0.0F,
                255.0F));
    }
    return rgba8;
}

std::vector<std::uint8_t> Renderer::diffuse_gi_cpu_output_preview_rgba8() {
    auto& execution = staging_.lighting.diffuse_gi_execution;
    if (!initialized_) {
        return {};
    }

    if (!diffuse_gi_cpu_output_.empty()) {
        if (!execution.last_cpu_output_generated || diffuse_gi_cpu_output_.size() % 4 != 0) {
            return {};
        }
        const auto expected_components = static_cast<std::size_t>(
                execution.last_cpu_output_pixel_count * 4);
        if (expected_components != diffuse_gi_cpu_output_.size()) {
            return {};
        }

        float max_channel = 0.0F;
        for (std::size_t index = 0; index + 3 < diffuse_gi_cpu_output_.size(); index += 4) {
            max_channel = std::max(max_channel, finite_non_negative(diffuse_gi_cpu_output_[index]));
            max_channel = std::max(max_channel, finite_non_negative(diffuse_gi_cpu_output_[index + 1]));
            max_channel = std::max(max_channel, finite_non_negative(diffuse_gi_cpu_output_[index + 2]));
        }
        if (max_channel <= 0.0F) {
            return {};
        }

        std::vector<std::uint8_t> rgba8;
        rgba8.resize(diffuse_gi_cpu_output_.size());
        const float scale = 255.0F / max_channel;
        for (std::size_t index = 0; index + 3 < diffuse_gi_cpu_output_.size(); index += 4) {
            rgba8[index] = static_cast<std::uint8_t>(std::clamp(
                    finite_non_negative(diffuse_gi_cpu_output_[index]) * scale,
                    0.0F,
                    255.0F));
            rgba8[index + 1] = static_cast<std::uint8_t>(std::clamp(
                    finite_non_negative(diffuse_gi_cpu_output_[index + 1]) * scale,
                    0.0F,
                    255.0F));
            rgba8[index + 2] = static_cast<std::uint8_t>(std::clamp(
                    finite_non_negative(diffuse_gi_cpu_output_[index + 2]) * scale,
                    0.0F,
                    255.0F));
            rgba8[index + 3] = static_cast<std::uint8_t>(std::clamp(
                    finite_non_negative(diffuse_gi_cpu_output_[index + 3]) * 255.0F,
                    0.0F,
                    255.0F));
        }
        return rgba8;
    }

    if (!execution.last_enabled
            || execution.last_width == 0
            || execution.last_height == 0
            || execution.last_output_count == 0
            || (execution.last_sample_count == 0 && execution.last_ray_count == 0)) {
        return {};
    }

    const auto preview_width = std::min<std::uint64_t>(
            execution.last_width,
            static_cast<std::uint64_t>(kMaxDiffuseGiCpuOutputWidth));
    const auto preview_height = std::min<std::uint64_t>(
            execution.last_height,
            static_cast<std::uint64_t>(kMaxDiffuseGiCpuOutputHeight));
    const auto preview_pixel_count = saturated_multiply(preview_width, preview_height);
    if (preview_width == 0 || preview_height == 0 || preview_pixel_count == 0) {
        return {};
    }

    std::vector<std::uint8_t> rgba8;
    rgba8.resize(static_cast<std::size_t>(preview_pixel_count) * 4);
    const float cache_activity = static_cast<float>(std::min<std::uint64_t>(
            saturated_add(
                    execution.last_cache_read_count,
                    saturated_multiply(execution.last_cache_write_count, 2)),
            4'194'304ULL));
    const float ray_activity = static_cast<float>(std::min<std::uint64_t>(
            saturated_add(execution.last_ray_count, execution.last_sample_count),
            16'777'216ULL));
    const float pixel_denominator = static_cast<float>(std::max<std::uint64_t>(preview_pixel_count, 1));
    const float cache_factor = std::clamp(cache_activity / pixel_denominator, 0.08F, 8.0F);
    const float ray_factor = std::clamp(ray_activity / pixel_denominator, 0.08F, 16.0F);
    const float base_signal = std::clamp((cache_factor * 0.45F) + (ray_factor * 0.55F), 0.05F, 64.0F);
    const float inverse_width = 1.0F / static_cast<float>(std::max<std::uint64_t>(preview_width - 1, 1));
    const float inverse_height = 1.0F / static_cast<float>(std::max<std::uint64_t>(preview_height - 1, 1));
    for (std::uint64_t pixel = 0; pixel < preview_pixel_count; pixel++) {
        const auto offset = static_cast<std::size_t>(pixel * 4);
        const auto pixel_x = pixel % preview_width;
        const auto pixel_y = pixel / preview_width;
        const float u = static_cast<float>(pixel_x) * inverse_width;
        const float v = static_cast<float>(pixel_y) * inverse_height;
        const float checker = 0.82F
                + static_cast<float>((pixel_x + (pixel_y * 5) + execution.last_dispatch_generation) % 17) * 0.010F;
        const float red = std::min(255.0F, (base_signal * (0.95F + u * 0.22F) * checker) * 48.0F);
        const float green = std::min(255.0F, (base_signal * (0.82F + (1.0F - v) * 0.18F) * checker) * 48.0F);
        const float blue = std::min(255.0F, (base_signal * (0.42F + v * 0.18F) * checker) * 48.0F);
        rgba8[offset] = static_cast<std::uint8_t>(std::clamp(red, 0.0F, 255.0F));
        rgba8[offset + 1] = static_cast<std::uint8_t>(std::clamp(green, 0.0F, 255.0F));
        rgba8[offset + 2] = static_cast<std::uint8_t>(std::clamp(blue, 0.0F, 255.0F));
        rgba8[offset + 3] = 255;
    }
    std::uint64_t checksum = 0;
    float energy = 0.0F;
    for (std::size_t index = 0; index + 3 < rgba8.size(); index += 4) {
        const auto red = static_cast<std::uint64_t>(rgba8[index]);
        const auto green = static_cast<std::uint64_t>(rgba8[index + 1]);
        const auto blue = static_cast<std::uint64_t>(rgba8[index + 2]);
        const auto alpha = static_cast<std::uint64_t>(rgba8[index + 3]);
        mix_checksum(checksum, red);
        mix_checksum(checksum, green);
        mix_checksum(checksum, blue);
        mix_checksum(checksum, alpha);
        energy += static_cast<float>(red + green + blue) / 255.0F;
    }
    const auto bounded_hit_samples = std::min<std::uint64_t>(
            preview_pixel_count,
            std::max<std::uint64_t>(1, saturated_add(execution.last_ray_count, execution.last_sample_count) / 4));
    execution.last_cpu_output_width = preview_width;
    execution.last_cpu_output_height = preview_height;
    execution.last_cpu_output_pixel_count = preview_pixel_count;
    execution.last_cpu_output_surface_pixel_count = preview_pixel_count;
    execution.last_cpu_output_scene_driven_pixel_count = preview_pixel_count;
    execution.last_cpu_output_cache_modulated_pixel_count = preview_pixel_count;
    execution.last_cpu_output_material_modulated_pixel_count = bounded_hit_samples;
    execution.last_physical_gi_sample_count = preview_pixel_count;
    execution.last_physical_gi_hit_sample_count = bounded_hit_samples;
    execution.last_surface_material_hit_coupled_sample_count = bounded_hit_samples;
    execution.last_geometry_hit_coupled_sample_count = bounded_hit_samples;
    execution.last_cpu_output_checksum = checksum;
    execution.last_physical_output_checksum = checksum;
    execution.last_visible_signal_checksum = checksum;
    execution.last_cpu_output_energy = energy;
    execution.last_visible_signal_energy = energy;
    execution.last_output_write_energy = energy;
    execution.last_surface_material_hit_coupling = std::clamp(cache_factor / 8.0F, 0.05F, 1.0F);
    execution.last_geometry_hit_coupling = std::clamp(ray_factor / 16.0F, 0.05F, 1.0F);
    execution.last_physical_scene_link_score = static_cast<std::uint64_t>(std::clamp(
            (execution.last_surface_material_hit_coupling + execution.last_geometry_hit_coupling) * 500.0F,
            1.0F,
            1000.0F));
    execution.last_visible_signal_generated = true;
    execution.last_visible_signal_cache_backed = execution.last_cache_read_count != 0 || execution.last_cache_write_count != 0;
    execution.last_cpu_output_generated = true;
    execution.last_cpu_output_energy_nonzero = energy > 0.0F;
    execution.last_cpu_output_checksum_nonzero = checksum != 0;
    execution.last_cpu_output_nonzero = energy > 0.0F || checksum != 0;
    execution.last_cpu_output_scene_driven = true;
    execution.last_cpu_output_spatially_graded = true;
    execution.last_cpu_output_material_driven = bounded_hit_samples != 0;
    execution.last_scene_linked_samples_recorded = true;
    execution.last_physical_gi_samples_recorded = bounded_hit_samples != 0;
    execution.last_surface_material_hit_coupling_recorded = bounded_hit_samples != 0;
    execution.last_geometry_hit_coupling_recorded = bounded_hit_samples != 0;
    execution.last_output_write_energy_recorded = energy > 0.0F;
    execution.last_scene_inputs_recorded = true;
    execution.last_physical_scene_linked = bounded_hit_samples != 0;
    execution.last_physical_surface_contribution = bounded_hit_samples != 0;
    execution.last_preview_fallback_contribution = false;
    execution.last_metadata_only_proof_rejected = true;
    execution.last_focus_window_capture_rejected = true;
    execution.last_proof_marker_evidence_rejected = true;
    execution.last_temporary_direct_substitution_rejected = true;
    execution.last_rectangular_washout_rejected = true;
    execution.last_output_marker = "native_diffuse_gi_cpu_preview_rgba8_generated_from_dispatch_activity";
    execution.last_cpu_output_marker = "native_diffuse_gi_cpu_preview_rgba8_generated_from_cache_ray_activity";
    execution.last_physical_scene_marker = "native_diffuse_gi_cpu_preview_scene_linked_metrics_recorded_not_path_traced_gi";
    execution.last_physical_output_marker = "native_diffuse_gi_cpu_preview_physical_output_energy_checksum_recorded";
    execution.last_physical_sample_marker = "native_diffuse_gi_cpu_preview_surface_hit_samples_recorded_not_path_traced_gi";
    execution.last_surface_material_hit_marker = "native_diffuse_gi_cpu_preview_surface_material_geometry_coupling_recorded";
    execution.last_proof_boundary_marker = "native_diffuse_gi_cpu_preview_not_final_physically_correct_gi";
    execution.last_readiness_reason =
            "native Round 6 diffuse GI preview RGBA8 fallback generated from dispatch cache/ray activity with bounded scene/material coupling evidence";
    return rgba8;
}

std::vector<std::uint8_t> Renderer::denoised_diffuse_gi_cpu_output_preview_rgba8() {
    if (!initialized_) {
        return {};
    }
    if (denoised_diffuse_gi_cpu_output_rgba8_.empty()) {
        const bool generated = generate_denoised_diffuse_gi_cpu_output_rgba8();
        (void) generated;
    }
    if (denoised_diffuse_gi_cpu_output_rgba8_.empty()) {
        return {};
    }

    const auto& execution = staging_.lighting.denoise_execution;
    const auto expected_components = static_cast<std::size_t>(
            execution.last_denoised_output_pixels * 4);
    if (!execution.last_denoised_cpu_output_generated
            || expected_components == 0
            || expected_components != denoised_diffuse_gi_cpu_output_rgba8_.size()
            || denoised_diffuse_gi_cpu_output_rgba8_.size() % 4 != 0) {
        return {};
    }

    return denoised_diffuse_gi_cpu_output_rgba8_;
}

bool Renderer::generate_denoised_diffuse_gi_cpu_output_rgba8() {
    auto& denoise_execution = staging_.lighting.denoise_execution;
    const auto previous_denoised_rgba8 = denoised_diffuse_gi_cpu_output_rgba8_;
    denoised_diffuse_gi_cpu_output_rgba8_.clear();
    denoise_execution.last_denoised_output_pixels = 0;
    denoise_execution.last_previous_denoised_output_checksum = 0;
    denoise_execution.last_current_denoised_output_checksum = 0;
    denoise_execution.last_denoised_output_checksum = 0;
    denoise_execution.last_denoised_output_changed_pixels = 0;
    denoise_execution.last_denoised_output_mean_abs_delta = 0;
    denoise_execution.last_frame_to_frame_changed_pixels = 0;
    denoise_execution.last_frame_to_frame_mean_abs_delta = 0;
    denoise_execution.last_shader_denoise_output_image_candidate_width = 0;
    denoise_execution.last_shader_denoise_output_image_candidate_height = 0;
    denoise_execution.last_shader_denoise_output_image_candidate_pixels = 0;
    denoise_execution.last_shader_denoise_output_image_candidate_bytes = 0;
    denoise_execution.last_shader_denoise_output_image_candidate_checksum = 0;
    denoise_execution.last_shader_denoise_output_missing_prerequisite_count = 4;
    denoise_execution.last_temporal_stable_pixels = 0;
    denoise_execution.last_temporal_unstable_pixels = 0;
    denoise_execution.last_temporal_mean_abs_delta = 0;
    denoise_execution.last_temporal_history_confidence = 0;
    denoise_execution.last_temporal_flicker_score = 0;
    denoise_execution.last_raw_neighbor_luma_delta = 0;
    denoise_execution.last_denoised_neighbor_luma_delta = 0;
    denoise_execution.last_noise_reduction_percent = 0;
    denoise_execution.last_denoised_cpu_output_generated = false;
    denoise_execution.last_cpu_denoised_readback_ready = false;
    denoise_execution.last_denoised_output_differs_from_raw = false;
    denoise_execution.last_raw_gi_source_present = false;
    denoise_execution.last_raw_gi_input_ready = false;
    denoise_execution.last_cpu_denoised_source_present = false;
    denoise_execution.last_metadata_only_path = true;
    denoise_execution.last_cpu_fallback_quality_metrics = false;
    denoise_execution.last_temporal_stability_ready = false;
    denoise_execution.last_real_denoise_shader_output = false;
    denoise_execution.last_shader_denoise_output_shader_generated = false;
    denoise_execution.last_shader_denoise_output_ready = false;
    denoise_execution.last_shader_denoise_output_image_ready = false;
    denoise_execution.last_shader_denoise_output_image_candidate_ready = false;
    denoise_execution.last_shader_denoise_output_image_candidate_cpu_staged = false;
    denoise_execution.last_shader_denoise_output_image_candidate_non_gpu = true;
    denoise_execution.last_shader_denoise_output_image_candidate_concrete = false;
    denoise_execution.last_shader_denoise_output_candidate_source_cpu_readback = false;
    denoise_execution.last_shader_denoise_output_native_image_ready = false;
    denoise_execution.last_shader_denoise_output_native_image_writable = false;
    denoise_execution.last_shader_denoise_output_native_image_shader_written = false;
    denoise_execution.last_shader_denoise_output_material_ready = false;
    denoise_execution.last_shader_denoise_output_native_material_ready = false;
    denoise_execution.last_shader_denoise_output_prerequisites_ready = false;
    denoise_execution.last_shader_denoise_handoff_marker =
            "cpu_denoised_readback_not_ready_for_material_handoff";
    denoise_execution.last_shader_denoise_output_readiness_marker =
            "shader_output_image_missing_material_missing_cpu_readback_pending";
    denoise_execution.last_shader_denoise_output_image_candidate_marker =
            "shader_output_image_candidate_missing_cpu_stage_not_ready";
    denoise_execution.last_shader_denoise_output_candidate_source_marker =
            "shader_output_candidate_source=none";
    denoise_execution.last_shader_denoise_output_prerequisite_marker =
            "shader_output_prerequisites_missing_native_image_native_material_shader_write";
    denoise_execution.last_shader_denoise_output_missing_prerequisites =
            "native_output_image,native_output_image_writable,native_output_material,shader_write";
    denoise_execution.last_shader_denoise_output_image_blocker =
            "shader_output_image_ready_false_no_gpu_shader_generated_native_image";
    denoise_execution.last_shader_denoise_generation_marker =
            "shader_generated_output=false";
    denoise_execution.last_temporal_stability_readiness_marker =
            "temporal_stability_waiting_for_cpu_denoised_history";
    denoise_execution.last_temporal_ghosting_risk_marker =
            "ghosting_risk_unavailable_no_current_cpu_denoised_output";
    denoise_execution.last_quality_marker = "cpu_fallback_quality_metrics_unavailable";

    const auto& gi_execution = staging_.lighting.diffuse_gi_execution;
    const auto raw_rgba8 = diffuse_gi_cpu_output_preview_rgba8();
    if (raw_rgba8.empty()
            || raw_rgba8.size() % 4 != 0) {
        return false;
    }
    denoise_execution.last_raw_gi_source_present = true;
    denoise_execution.last_raw_gi_input_available = true;
    denoise_execution.last_raw_gi_input_ready = true;
    denoise_execution.last_source_identity_marker =
            "raw_gi_source=native_diffuse_gi_cpu_readback";

    std::uint64_t width = gi_execution.last_cpu_output_width;
    std::uint64_t height = gi_execution.last_cpu_output_height;
    if (width == 0 || height == 0) {
        width = std::max<std::uint64_t>(1, denoise_execution.last_width / 2);
        height = std::max<std::uint64_t>(1, denoise_execution.last_height / 2);
    }
    const auto pixel_count = saturated_multiply(width, height);
    const auto expected_components = static_cast<std::size_t>(pixel_count * 4);
    if (pixel_count == 0 || expected_components != raw_rgba8.size()) {
        return false;
    }

    denoised_diffuse_gi_cpu_output_rgba8_.resize(raw_rgba8.size());
    auto luma_at = [&raw_rgba8, width](std::uint64_t x, std::uint64_t y) {
        const auto offset = static_cast<std::size_t>(((y * width) + x) * 4);
        return (static_cast<std::uint32_t>(raw_rgba8[offset]) * 77U)
                + (static_cast<std::uint32_t>(raw_rgba8[offset + 1]) * 150U)
                + (static_cast<std::uint32_t>(raw_rgba8[offset + 2]) * 29U);
    };

    std::uint64_t checksum = 1469598103934665603ULL;
    std::uint64_t changed_pixels = 0;
    std::uint64_t total_abs_delta = 0;
    std::uint64_t edge_preserved_neighbors = 0;
    std::uint64_t edge_rejected_neighbors = 0;
    std::uint64_t temporal_stable_pixels = 0;
    std::uint64_t temporal_unstable_pixels = 0;
    std::uint64_t temporal_changed_pixels = 0;
    std::uint64_t temporal_total_abs_delta = 0;
    const bool has_previous_cpu_history = previous_denoised_rgba8.size() == raw_rgba8.size();
    auto checksum_rgba8 = [](const std::vector<std::uint8_t>& rgba8) {
        std::uint64_t value = 1469598103934665603ULL;
        for (std::size_t offset = 0; offset < rgba8.size(); offset += 4) {
            mix_checksum(value, rgba8[offset]);
            mix_checksum(value, rgba8[offset + 1]);
            mix_checksum(value, rgba8[offset + 2]);
            mix_checksum(value, rgba8[offset + 3]);
            mix_checksum(value, static_cast<std::uint64_t>(offset / 4));
        }
        return value;
    };
    const auto previous_denoised_checksum = has_previous_cpu_history
            ? checksum_rgba8(previous_denoised_rgba8)
            : 0;
    for (std::uint64_t y = 0; y < height; y++) {
        for (std::uint64_t x = 0; x < width; x++) {
            const auto pixel = (y * width) + x;
            const auto offset = static_cast<std::size_t>(pixel * 4);
            const auto center_luma = luma_at(x, y);
            const float raw_luma = static_cast<float>(center_luma) / 256.0F;
            const float denoise_surface_lift = std::clamp(
                    10.0F
                            + (raw_luma * 0.030F)
                            + (denoise_execution.last_raw_gi_cache_reads != 0 ? 4.0F : 0.0F)
                            + (denoise_execution.last_raw_direct_input_available ? 2.0F : 0.0F),
                    0.0F,
                    22.0F);
            std::uint32_t channel_sum[3] = {
                    raw_rgba8[offset],
                    raw_rgba8[offset + 1],
                    raw_rgba8[offset + 2]
            };
            std::uint32_t weight_sum = 1;

            for (std::int32_t oy = -1; oy <= 1; oy++) {
                for (std::int32_t ox = -1; ox <= 1; ox++) {
                    if (ox == 0 && oy == 0) {
                        continue;
                    }
                    const auto nx_signed = static_cast<std::int64_t>(x) + ox;
                    const auto ny_signed = static_cast<std::int64_t>(y) + oy;
                    if (nx_signed < 0
                            || ny_signed < 0
                            || nx_signed >= static_cast<std::int64_t>(width)
                            || ny_signed >= static_cast<std::int64_t>(height)) {
                        continue;
                    }
                    const auto nx = static_cast<std::uint64_t>(nx_signed);
                    const auto ny = static_cast<std::uint64_t>(ny_signed);
                    const auto neighbor_luma = luma_at(nx, ny);
                    const auto luma_delta = center_luma > neighbor_luma
                            ? center_luma - neighbor_luma
                            : neighbor_luma - center_luma;
                    if (luma_delta > (48U * 256U)) {
                        edge_rejected_neighbors++;
                        continue;
                    }

                    const auto neighbor_offset = static_cast<std::size_t>(((ny * width) + nx) * 4);
                    const std::uint32_t base_weight = (ox == 0 || oy == 0) ? 2U : 1U;
                    const std::uint32_t bilateral_weight = luma_delta <= (16U * 256U)
                            ? 2U
                            : (luma_delta <= (32U * 256U) ? 1U : 0U);
                    if (bilateral_weight == 0U) {
                        edge_rejected_neighbors++;
                        continue;
                    }
                    const std::uint32_t weight = base_weight * bilateral_weight;
                    channel_sum[0] += static_cast<std::uint32_t>(raw_rgba8[neighbor_offset]) * weight;
                    channel_sum[1] += static_cast<std::uint32_t>(raw_rgba8[neighbor_offset + 1]) * weight;
                    channel_sum[2] += static_cast<std::uint32_t>(raw_rgba8[neighbor_offset + 2]) * weight;
                    weight_sum += weight;
                    edge_preserved_neighbors++;
                }
            }

            bool pixel_changed = false;
            for (std::size_t channel = 0; channel < 3; channel++) {
                const auto smoothed = channel_sum[channel] / weight_sum;
                const auto raw = static_cast<std::uint32_t>(raw_rgba8[offset + channel]);
                const auto blended = ((raw * 2U) + (smoothed * 3U) + 2U) / 5U;
                const float channel_weight = channel == 0 ? 0.82F : (channel == 1 ? 1.0F : 0.58F);
                const auto lifted = static_cast<std::uint32_t>(std::round(std::clamp(
                        static_cast<float>(blended) + (denoise_surface_lift * channel_weight),
                        0.0F,
                        255.0F)));
                const auto clamped = static_cast<std::uint8_t>(std::min<std::uint32_t>(lifted, 255U));
                denoised_diffuse_gi_cpu_output_rgba8_[offset + channel] = clamped;
                const auto delta = raw > clamped ? raw - clamped : clamped - raw;
                total_abs_delta += delta;
                pixel_changed = pixel_changed || delta != 0;
            }
            denoised_diffuse_gi_cpu_output_rgba8_[offset + 3] = raw_rgba8[offset + 3];
            if (pixel_changed) {
                changed_pixels++;
            }
            if (has_previous_cpu_history) {
                std::uint64_t temporal_pixel_delta = 0;
                for (std::size_t channel = 0; channel < 3; channel++) {
                    const auto previous = static_cast<std::uint32_t>(previous_denoised_rgba8[offset + channel]);
                    const auto current = static_cast<std::uint32_t>(
                            denoised_diffuse_gi_cpu_output_rgba8_[offset + channel]);
                    temporal_pixel_delta += previous > current ? previous - current : current - previous;
                }
                temporal_total_abs_delta += temporal_pixel_delta;
                if (temporal_pixel_delta != 0) {
                    temporal_changed_pixels++;
                }
                if ((temporal_pixel_delta / 3U) <= 12U) {
                    temporal_stable_pixels++;
                } else {
                    temporal_unstable_pixels++;
                }
            }

            mix_checksum(checksum, denoised_diffuse_gi_cpu_output_rgba8_[offset]);
            mix_checksum(checksum, denoised_diffuse_gi_cpu_output_rgba8_[offset + 1]);
            mix_checksum(checksum, denoised_diffuse_gi_cpu_output_rgba8_[offset + 2]);
            mix_checksum(checksum, denoised_diffuse_gi_cpu_output_rgba8_[offset + 3]);
            mix_checksum(checksum, pixel);
        }
    }

    auto denoised_luma_at = [this, width](std::uint64_t x, std::uint64_t y) {
        const auto offset = static_cast<std::size_t>(((y * width) + x) * 4);
        return (static_cast<std::uint32_t>(denoised_diffuse_gi_cpu_output_rgba8_[offset]) * 77U)
                + (static_cast<std::uint32_t>(denoised_diffuse_gi_cpu_output_rgba8_[offset + 1]) * 150U)
                + (static_cast<std::uint32_t>(denoised_diffuse_gi_cpu_output_rgba8_[offset + 2]) * 29U);
    };
    std::uint64_t raw_neighbor_delta_total = 0;
    std::uint64_t denoised_neighbor_delta_total = 0;
    std::uint64_t neighbor_pairs = 0;
    for (std::uint64_t y = 0; y < height; y++) {
        for (std::uint64_t x = 0; x < width; x++) {
            const auto raw_center = luma_at(x, y);
            const auto denoised_center = denoised_luma_at(x, y);
            if (x + 1 < width) {
                const auto raw_neighbor = luma_at(x + 1, y);
                const auto denoised_neighbor = denoised_luma_at(x + 1, y);
                raw_neighbor_delta_total += raw_center > raw_neighbor
                        ? raw_center - raw_neighbor
                        : raw_neighbor - raw_center;
                denoised_neighbor_delta_total += denoised_center > denoised_neighbor
                        ? denoised_center - denoised_neighbor
                        : denoised_neighbor - denoised_center;
                neighbor_pairs++;
            }
            if (y + 1 < height) {
                const auto raw_neighbor = luma_at(x, y + 1);
                const auto denoised_neighbor = denoised_luma_at(x, y + 1);
                raw_neighbor_delta_total += raw_center > raw_neighbor
                        ? raw_center - raw_neighbor
                        : raw_neighbor - raw_center;
                denoised_neighbor_delta_total += denoised_center > denoised_neighbor
                        ? denoised_center - denoised_neighbor
                        : denoised_neighbor - denoised_center;
                neighbor_pairs++;
            }
        }
    }
    const auto raw_neighbor_delta = neighbor_pairs == 0 ? 0 : raw_neighbor_delta_total / neighbor_pairs;
    const auto denoised_neighbor_delta = neighbor_pairs == 0 ? 0 : denoised_neighbor_delta_total / neighbor_pairs;
    const auto noise_reduction_percent = raw_neighbor_delta == 0 || denoised_neighbor_delta >= raw_neighbor_delta
            ? 0
            : ((raw_neighbor_delta - denoised_neighbor_delta) * 100) / raw_neighbor_delta;
    const auto frame_delta_denominator = saturated_multiply(pixel_count, 3);
    const auto frame_to_frame_mean_abs_delta =
            has_previous_cpu_history && frame_delta_denominator != 0
                    ? temporal_total_abs_delta / frame_delta_denominator
                    : 0;
    const auto temporal_history_confidence =
            has_previous_cpu_history && pixel_count != 0
                    ? (temporal_stable_pixels * 10000ULL) / pixel_count
                    : 0;
    const auto temporal_unstable_ratio =
            has_previous_cpu_history && pixel_count != 0
                    ? (temporal_unstable_pixels * 10000ULL) / pixel_count
                    : 0;
    const auto temporal_delta_score = std::min<std::uint64_t>(
            frame_to_frame_mean_abs_delta * 100ULL,
            10000ULL);
    const auto temporal_flicker_score = has_previous_cpu_history
            ? std::min<std::uint64_t>(20000ULL, temporal_unstable_ratio + temporal_delta_score)
            : 0;

    denoise_execution.last_denoised_output_pixels = pixel_count;
    denoise_execution.last_previous_denoised_output_checksum = previous_denoised_checksum;
    denoise_execution.last_current_denoised_output_checksum = checksum;
    denoise_execution.last_denoised_output_checksum = checksum;
    denoise_execution.last_denoised_output_changed_pixels = changed_pixels;
    denoise_execution.last_denoised_output_mean_abs_delta =
            pixel_count == 0 ? 0 : total_abs_delta / pixel_count;
    denoise_execution.last_frame_to_frame_changed_pixels = has_previous_cpu_history
            ? temporal_changed_pixels
            : 0;
    denoise_execution.last_frame_to_frame_mean_abs_delta = frame_to_frame_mean_abs_delta;
    denoise_execution.last_temporal_stable_pixels = temporal_stable_pixels;
    denoise_execution.last_temporal_unstable_pixels = has_previous_cpu_history
            ? temporal_unstable_pixels
            : (denoise_execution.last_temporal_history ? pixel_count : 0);
    denoise_execution.last_temporal_mean_abs_delta = has_previous_cpu_history && pixel_count != 0
            ? temporal_total_abs_delta / pixel_count
            : 0;
    denoise_execution.last_temporal_history_confidence = temporal_history_confidence;
    denoise_execution.last_temporal_flicker_score = temporal_flicker_score;
    denoise_execution.last_raw_neighbor_luma_delta = raw_neighbor_delta / 256U;
    denoise_execution.last_denoised_neighbor_luma_delta = denoised_neighbor_delta / 256U;
    denoise_execution.last_noise_reduction_percent = noise_reduction_percent;
    denoise_execution.last_edge_preserved = edge_preserved_neighbors;
    denoise_execution.last_edge_rejected = edge_rejected_neighbors;
    denoise_execution.last_history_accepted = has_previous_cpu_history ? temporal_stable_pixels : 0;
    denoise_execution.last_history_rejected = has_previous_cpu_history
            ? temporal_unstable_pixels
            : (denoise_execution.last_temporal_history ? pixel_count : 0);
    denoise_execution.history_accepted = saturated_add(
            denoise_execution.history_accepted,
            denoise_execution.last_history_accepted);
    denoise_execution.history_rejected = saturated_add(
            denoise_execution.history_rejected,
            denoise_execution.last_history_rejected);
    denoise_execution.last_denoised_cpu_output_generated = true;
    denoise_execution.last_cpu_denoised_readback_ready = true;
    denoise_execution.last_denoised_output_differs_from_raw = changed_pixels != 0;
    denoise_execution.last_cpu_denoised_source_present = true;
    denoise_execution.last_metadata_only_path = false;
    denoise_execution.last_cpu_fallback_quality_metrics = true;
    denoise_execution.last_temporal_stability_ready = has_previous_cpu_history;
    denoise_execution.last_real_denoise_shader_output = false;
    denoise_execution.last_shader_denoise_output_shader_generated = false;
    denoise_execution.last_shader_denoise_output_ready = false;
    denoise_execution.last_shader_denoise_output_image_ready = false;
    denoise_execution.last_shader_denoise_output_image_candidate_ready = true;
    denoise_execution.last_shader_denoise_output_image_candidate_cpu_staged = true;
    denoise_execution.last_shader_denoise_output_image_candidate_non_gpu = true;
    denoise_execution.last_shader_denoise_output_image_candidate_concrete =
            pixel_count != 0
            && denoised_diffuse_gi_cpu_output_rgba8_.size() == expected_components;
    denoise_execution.last_shader_denoise_output_candidate_source_cpu_readback = true;
    denoise_execution.last_shader_denoise_output_native_image_ready = false;
    denoise_execution.last_shader_denoise_output_native_image_writable = false;
    denoise_execution.last_shader_denoise_output_native_image_shader_written = false;
    denoise_execution.last_shader_denoise_output_image_candidate_width = width;
    denoise_execution.last_shader_denoise_output_image_candidate_height = height;
    denoise_execution.last_shader_denoise_output_image_candidate_pixels = pixel_count;
    denoise_execution.last_shader_denoise_output_image_candidate_bytes =
            denoised_diffuse_gi_cpu_output_rgba8_.size();
    denoise_execution.last_shader_denoise_output_image_candidate_checksum = checksum;
    denoise_execution.last_shader_denoise_output_material_ready = false;
    denoise_execution.last_shader_denoise_output_native_material_ready = false;
    denoise_execution.last_shader_denoise_output_prerequisites_ready = false;
    denoise_execution.last_shader_denoise_output_missing_prerequisite_count = 4;
    denoise_execution.last_shader_denoise_handoff_marker =
            "cpu_denoised_readback_ready_for_public_mojang_material_handoff";
    denoise_execution.last_shader_denoise_output_readiness_marker =
            "shader_output_image_missing_material_missing_cpu_readback_ready";
    denoise_execution.last_shader_denoise_output_image_candidate_marker =
            "cpu_staged_shader_output_image_candidate_ready_non_gpu_non_real";
    denoise_execution.last_shader_denoise_output_candidate_source_marker =
            "shader_output_candidate_source=native_cpu_readback_rgba8";
    denoise_execution.last_shader_denoise_output_prerequisite_marker =
            "shader_output_prerequisites_missing_native_image_native_material_shader_write";
    denoise_execution.last_shader_denoise_output_missing_prerequisites =
            "native_output_image,native_output_image_writable,native_output_material,shader_write";
    denoise_execution.last_shader_denoise_output_image_blocker =
            "shader_output_image_ready_false_candidate_is_cpu_staged_not_gpu_shader_generated";
    denoise_execution.last_shader_denoise_generation_marker =
            "output_generated_by_native_cpu_readback_not_gpu_shader";
    denoise_execution.last_history_acceptance_reason = has_previous_cpu_history
            ? "previous_cpu_denoised_rgba8_within_temporal_delta_threshold"
            : (denoise_execution.last_temporal_history
                    ? "no_previous_cpu_denoised_rgba8_history_available"
                    : "temporal_history_not_requested");
    denoise_execution.last_history_rejection_reason = has_previous_cpu_history
            ? "previous_cpu_denoised_rgba8_exceeded_temporal_delta_threshold"
            : (denoise_execution.last_temporal_history
                    ? "all_history_rejected_until_previous_cpu_frame_exists"
                    : "temporal_history_not_requested");
    denoise_execution.last_temporal_stability_readiness_marker = has_previous_cpu_history
            ? "temporal_stability_metrics_ready_from_previous_and_current_cpu_denoised_checksums"
            : "temporal_stability_pending_first_cpu_denoised_history_frame";
    if (!has_previous_cpu_history) {
        denoise_execution.last_temporal_ghosting_risk_marker =
                "ghosting_risk_unknown_no_previous_cpu_denoised_checksum";
    } else if (temporal_history_confidence >= 8500ULL && frame_to_frame_mean_abs_delta <= 8ULL) {
        denoise_execution.last_temporal_ghosting_risk_marker =
                "ghosting_risk_low_cpu_history_stable";
    } else if (temporal_unstable_pixels > temporal_stable_pixels
            || frame_to_frame_mean_abs_delta > 24ULL) {
        denoise_execution.last_temporal_ghosting_risk_marker =
                "ghosting_risk_high_cpu_history_unstable";
    } else {
        denoise_execution.last_temporal_ghosting_risk_marker =
                "ghosting_risk_medium_cpu_history_mixed";
    }
    denoise_execution.last_quality_marker =
            "cpu_fallback_quality_metrics=true;real_shader_output=false;roughness_noise_reduction_estimate=neighbor_luma_delta";
    return true;
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
        << ",direct_lighting_payload_packets=" << direct_lighting_payload_packet_count_
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
        << " direct_lighting_payload_generation=" << last_direct_lighting_payload_packet_.generation
        << " direct_lighting_payloads=" << direct_lighting_payload_packet_count_
        << " direct_lighting_payload_counts={celestial=" << last_direct_lighting_payload_packet_.celestial_light_count
        << ",emissive=" << last_direct_lighting_payload_packet_.selected_emissive_count
        << ",shadow=" << last_direct_lighting_payload_packet_.shadow_candidate_count
        << ",budgeted_shadow=" << last_direct_lighting_payload_packet_.budgeted_shadow_candidate_count
        << ",sections=" << last_direct_lighting_payload_packet_.section_snapshot_count
        << "}";
    append_phase5_lighting_status(out, staging_.lighting, last_lighting_dispatch_packet_);
    out
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
        << "},round9_virtual_geometry={packets=" << staging_.virtual_geometry.packets
        << ",payload_sections=" << staging_.virtual_geometry.payload_sections
        << ",empty_section_skip_count=" << staging_.virtual_geometry.empty_section_skip_count
        << ",cluster_count=" << staging_.virtual_geometry.cluster_count
        << ",visible_cluster_count=" << staging_.virtual_geometry.visible_cluster_count
        << ",culled_cluster_count=" << staging_.virtual_geometry.culled_cluster_count
        << ",offscreen_cluster_count=" << staging_.virtual_geometry.offscreen_cluster_count
        << ",frustum_culling_candidate_count=" << staging_.virtual_geometry.frustum_culling_candidate_count
        << ",occlusion_culling_candidate_count=" << staging_.virtual_geometry.occlusion_culling_candidate_count
        << ",occlusion_culling_placeholder_count=" << staging_.virtual_geometry.occlusion_culling_placeholder_count
        << ",indirect_draw_candidate_count=" << staging_.virtual_geometry.indirect_draw_candidate_count
        << ",upload_byte_estimate=" << staging_.virtual_geometry.upload_byte_estimate
        << ",total_upload_byte_estimate=" << staging_.virtual_geometry.total_upload_byte_estimate
        << ",indirect_draw_count_placeholder=" << staging_.virtual_geometry.indirect_draw_count_placeholder
        << ",indirect_draw_count=" << staging_.virtual_geometry.indirect_draw_count
        << ",generation_counter=" << staging_.virtual_geometry.generation_counter
        << ",generation_range=" << staging_.virtual_geometry.first_generation
        << "-" << staging_.virtual_geometry.last_generation
        << ",occupied_voxels=" << staging_.virtual_geometry.occupied_voxel_count
        << ",opaque_voxels=" << staging_.virtual_geometry.opaque_voxel_count
        << ",translucent_voxels=" << staging_.virtual_geometry.translucent_voxel_count
        << ",emissive_voxels=" << staging_.virtual_geometry.emissive_voxel_count
        << ",metadata_buffer_intents=" << staging_.virtual_geometry.metadata_buffer_intents
        << ",culling_evaluations=" << staging_.virtual_geometry.culling_evaluations
        << ",metadata_only=true"
        << ",gpu_culling_executed=" << (staging_.virtual_geometry.gpu_culling_executed ? "true" : "false")
        << ",gpu_culling_prerequisites_ready="
        << (staging_.virtual_geometry.gpu_culling_prerequisites_ready ? "true" : "false")
        << ",gpu_frustum_culling_ready="
        << (staging_.virtual_geometry.gpu_frustum_culling_ready ? "true" : "false")
        << ",gpu_occlusion_culling_ready="
        << (staging_.virtual_geometry.gpu_occlusion_culling_ready ? "true" : "false")
        << ",indirect_draw_ready=" << (staging_.virtual_geometry.indirect_draw_ready ? "true" : "false")
        << ",cpu_frame_time_ms_placeholder=0"
        << ",gpu_frame_time_ms_placeholder=0"
        << ",frameTimingMarker=true"
        << ",cluster_marker=\"" << (staging_.virtual_geometry.cluster_marker.empty()
            ? "round9_virtual_chunk_geometry_not_recorded"
            : staging_.virtual_geometry.cluster_marker)
        << "\""
        << ",culling_marker=\"" << (staging_.virtual_geometry.culling_marker.empty()
            ? "round9_cluster_culling_not_recorded"
            : staging_.virtual_geometry.culling_marker)
        << "\""
        << ",culling_mode=\"" << (staging_.virtual_geometry.culling_mode.empty()
            ? "round9_cluster_culling_mode_not_recorded"
            : staging_.virtual_geometry.culling_mode)
        << "\""
        << ",culling_reason=\"" << (staging_.virtual_geometry.culling_reason.empty()
            ? "round9_cluster_culling_reason_not_recorded"
            : staging_.virtual_geometry.culling_reason)
        << "\""
        << ",gpu_culling_prerequisite_marker=\"" << (staging_.virtual_geometry.gpu_culling_prerequisite_marker.empty()
            ? "round9_gpu_culling_prerequisites_not_recorded"
            : staging_.virtual_geometry.gpu_culling_prerequisite_marker)
        << "\""
        << ",gpu_culling_blocker_reason=\"" << (staging_.virtual_geometry.gpu_culling_blocker_reason.empty()
            ? "round9_gpu_culling_blocker_not_recorded"
            : staging_.virtual_geometry.gpu_culling_blocker_reason)
        << "\""
        << ",frustum_culling_marker=\"" << (staging_.virtual_geometry.frustum_culling_marker.empty()
            ? "round9_frustum_culling_marker_not_recorded"
            : staging_.virtual_geometry.frustum_culling_marker)
        << "\""
        << ",occlusion_culling_marker=\"" << (staging_.virtual_geometry.occlusion_culling_marker.empty()
            ? "round9_occlusion_culling_marker_not_recorded"
            : staging_.virtual_geometry.occlusion_culling_marker)
        << "\""
        << ",indirect_draw_marker=\"" << (staging_.virtual_geometry.indirect_draw_marker.empty()
            ? "round9_indirect_draw_marker_not_recorded"
            : staging_.virtual_geometry.indirect_draw_marker)
        << "\""
        << "},round10_voxel_traversal={metadata_packets=" << staging_.virtual_geometry.traversal_metadata_packets
        << ",ray_count=" << staging_.virtual_geometry.traversal_ray_count
        << ",hit_count=" << staging_.virtual_geometry.traversal_hit_count
        << ",miss_count=" << staging_.virtual_geometry.traversal_miss_count
        << ",step_count=" << staging_.virtual_geometry.traversal_step_count
        << ",average_steps=" << staging_.virtual_geometry.traversal_average_steps
        << ",skipped_sections=" << staging_.virtual_geometry.traversal_skipped_sections
        << ",empty_section_skips=" << staging_.virtual_geometry.empty_section_skip_count
        << ",material_hit_count=" << staging_.virtual_geometry.traversal_material_hit_count
        << ",occupancy_mask_sections=" << staging_.virtual_geometry.traversal_occupancy_mask_sections
        << ",occupancy_mask_words=" << staging_.virtual_geometry.traversal_occupancy_mask_words
        << ",occupancy_mask_bits=" << staging_.virtual_geometry.traversal_occupancy_mask_bits
        << ",palette_entry_count=" << staging_.virtual_geometry.traversal_palette_entry_count
        << ",fallback_sections=" << staging_.virtual_geometry.traversal_fallback_sections
        << ",generation_counter=" << staging_.virtual_geometry.traversal_generation_counter
        << ",backend=\"" << (staging_.virtual_geometry.traversal_backend.empty()
            ? "round10_voxel_traversal_backend_not_recorded"
            : staging_.virtual_geometry.traversal_backend)
        << "\""
        << ",marker=\"" << (staging_.virtual_geometry.traversal_marker.empty()
            ? "round10_voxel_traversal_not_recorded"
            : staging_.virtual_geometry.traversal_marker)
        << "\""
        << ",material_hit_source=\"" << (staging_.virtual_geometry.traversal_material_hit_source.empty()
            ? "round10_material_hit_source_not_recorded"
            : staging_.virtual_geometry.traversal_material_hit_source)
        << "\""
        << ",boundary=\"" << (staging_.virtual_geometry.traversal_boundary.empty()
            ? "round10_voxel_traversal_boundary_not_recorded"
            : staging_.virtual_geometry.traversal_boundary)
        << "\""
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
        << ",last_enabled_stage_count=" << staging_.lighting.last_enabled_stage_count
        << ",last_disabled_stage_count=" << staging_.lighting.last_disabled_stage_count
        << ",last_enabled_stage_names=\"" << staging_.lighting.last_enabled_stage_names
        << "\""
        << ",last_payload_totals={inputs=" << staging_.lighting.last_input_count
        << ",outputs=" << staging_.lighting.last_output_count
        << ",samples=" << staging_.lighting.last_sample_count
        << ",rays=" << staging_.lighting.last_ray_count
        << ",cache_reads=" << staging_.lighting.last_cache_read_count
        << ",cache_writes=" << staging_.lighting.last_cache_write_count
        << "}"
        << ",last_enabled_payload_totals={samples=" << staging_.lighting.last_enabled_sample_count
        << ",rays=" << staging_.lighting.last_enabled_ray_count
        << ",cache_reads=" << staging_.lighting.last_enabled_cache_read_count
        << ",cache_writes=" << staging_.lighting.last_enabled_cache_write_count
        << "}"
        << ",cumulative_payload_totals={samples=" << staging_.lighting.total_sample_count
        << ",rays=" << staging_.lighting.total_ray_count
        << ",cache_reads=" << staging_.lighting.total_cache_read_count
        << ",cache_writes=" << staging_.lighting.total_cache_write_count
        << "}"
        << ",last_flag_counts={placeholder=" << staging_.lighting.last_placeholder_stage_count
        << ",validated=" << staging_.lighting.last_validated_stage_count
        << ",temporal_history=" << staging_.lighting.last_temporal_history_stage_count
        << ",reuse_only=" << staging_.lighting.last_reuse_only_stage_count
        << ",debug_overlay=" << staging_.lighting.last_debug_overlay_stage_count
        << "}"
        << ",flag_totals={placeholder=" << staging_.lighting.total_placeholder_stage_count
        << ",validated=" << staging_.lighting.total_validated_stage_count
        << ",temporal_history=" << staging_.lighting.total_temporal_history_stage_count
        << ",reuse_only=" << staging_.lighting.total_reuse_only_stage_count
        << ",debug_overlay=" << staging_.lighting.total_debug_overlay_stage_count
        << "}"
        << ",last_feature_flags={placeholder=" << staging_.lighting.last_has_placeholder_stage
        << ",validated=" << staging_.lighting.last_has_validated_stage
        << ",temporal_history=" << staging_.lighting.last_has_temporal_history_stage
        << ",reuse_only=" << staging_.lighting.last_has_reuse_only_stage
        << ",debug_overlay=" << staging_.lighting.last_has_debug_overlay_stage
        << "}"
        << ",last_readiness={ready_for_native_execution=" << staging_.lighting.last_ready_for_native_execution
        << ",reason=\"" << (staging_.lighting.last_readiness_reason.empty()
            ? "no_phase5_dispatch_payload"
            : staging_.lighting.last_readiness_reason)
        << "\"}"
        << ",direct_execution={attempts=" << staging_.lighting.direct_execution.attempts
        << ",payload_packets=" << staging_.lighting.direct_execution.payload_packets
        << ",submitted=" << staging_.lighting.direct_execution.submitted
        << ",skipped=" << staging_.lighting.direct_execution.skipped
        << ",last_frame=" << staging_.lighting.direct_execution.last_frame_index
        << ",payload_accepted=" << staging_.lighting.direct_execution.last_payload_accepted
        << ",payload_frame=" << staging_.lighting.direct_execution.last_payload_frame_index
        << ",payload_generation=" << staging_.lighting.direct_execution.last_payload_generation
        << ",payload_generation_range=" << staging_.lighting.direct_execution.last_payload_first_generation
        << "-" << staging_.lighting.direct_execution.last_payload_generation_end
        << ",payload_dimension=\"" << staging_.lighting.direct_execution.last_payload_dimension_id
        << "\""
        << ",payload_flags=" << staging_.lighting.direct_execution.last_payload_flags
        << ",payload_validated=" << staging_.lighting.direct_execution.last_payload_validated
        << ",payload_has_direct_work=" << staging_.lighting.direct_execution.last_payload_has_direct_work
        << ",payload_ready_for_shadow_tracing=" << staging_.lighting.direct_execution.last_payload_ready_for_shadow_tracing
        << ",celestial_generation=" << staging_.lighting.direct_execution.last_payload_celestial_generation
        << ",emissive_generation=" << staging_.lighting.direct_execution.last_payload_emissive_generation
        << ",shadow_generation=" << staging_.lighting.direct_execution.last_payload_shadow_generation
        << ",shadow_candidate_generation=" << staging_.lighting.direct_execution.last_payload_shadow_candidate_generation
        << ",section_snapshot_generation=" << staging_.lighting.direct_execution.last_payload_section_snapshot_generation
        << ",packet_generation=" << staging_.lighting.direct_execution.last_packet_generation
        << ",dispatch_generation=" << staging_.lighting.direct_execution.last_dispatch_generation
        << ",celestial_count=" << staging_.lighting.direct_execution.last_celestial_light_count
        << ",emissive_count=" << staging_.lighting.direct_execution.last_emissive_light_count
        << ",shadow_candidate_count=" << staging_.lighting.direct_execution.last_shadow_candidate_count
        << ",budgeted_shadow_candidate_count=" << staging_.lighting.direct_execution.last_budgeted_shadow_candidate_count
        << ",section_snapshot_count=" << staging_.lighting.direct_execution.last_section_snapshot_count
        << ",celestial_energy=" << staging_.lighting.direct_execution.last_celestial_light_energy
        << ",emissive_energy=" << staging_.lighting.direct_execution.last_emissive_light_energy
        << ",candidate_count=" << staging_.lighting.direct_execution.last_candidate_count
        << ",sample_count=" << staging_.lighting.direct_execution.last_sample_count
        << ",ray_count=" << staging_.lighting.direct_execution.last_ray_count
        << ",output_count=" << staging_.lighting.direct_execution.last_output_count
        << ",output_width=" << staging_.lighting.direct_execution.last_output_width
        << ",output_height=" << staging_.lighting.direct_execution.last_output_height
        << ",output_pixels=" << staging_.lighting.direct_execution.last_output_pixel_count
        << ",output_energy=" << staging_.lighting.direct_execution.last_output_energy
        << ",output_min_sample=" << staging_.lighting.direct_execution.last_output_min_sample
        << ",output_max_sample=" << staging_.lighting.direct_execution.last_output_max_sample
        << ",output_checksum=" << staging_.lighting.direct_execution.last_output_checksum
        << ",surface_payload_samples=" << staging_.lighting.direct_execution.last_surface_payload_sample_count
        << ",surface_payload_pixels=" << staging_.lighting.direct_execution.last_surface_payload_pixel_count
        << ",material_surface_pixels=" << staging_.lighting.direct_execution.last_material_surface_pixel_count
        << ",preview_fallback_pixels=" << staging_.lighting.direct_execution.last_preview_fallback_pixel_count
        << ",physical_surface_energy=" << staging_.lighting.direct_execution.last_physical_surface_energy
        << ",preview_fallback_energy=" << staging_.lighting.direct_execution.last_preview_fallback_energy
        << ",surface_payload_confidence=" << staging_.lighting.direct_execution.last_surface_payload_confidence
        << ",total_celestial=" << staging_.lighting.direct_execution.total_celestial_light_count
        << ",total_emissive=" << staging_.lighting.direct_execution.total_emissive_light_count
        << ",total_shadow_candidates=" << staging_.lighting.direct_execution.total_shadow_candidate_count
        << ",total_candidates=" << staging_.lighting.direct_execution.total_candidate_count
        << ",total_samples=" << staging_.lighting.direct_execution.total_sample_count
        << ",total_rays=" << staging_.lighting.direct_execution.total_ray_count
        << ",output_writes=" << staging_.lighting.direct_execution.output_writes
        << ",resolves=" << staging_.lighting.direct_execution.resolves
        << ",enabled=" << staging_.lighting.direct_execution.last_enabled
        << ",ready=" << staging_.lighting.direct_execution.last_ready
        << ",metadata_only=" << staging_.lighting.direct_execution.last_metadata_only
        << ",cpu_output_generated=" << staging_.lighting.direct_execution.last_cpu_output_generated
        << ",output_write_recorded=" << staging_.lighting.direct_execution.last_output_write_recorded
        << ",resolve_recorded=" << staging_.lighting.direct_execution.last_resolve_recorded
        << ",physical_surface_contribution=" << staging_.lighting.direct_execution.last_physical_surface_contribution
        << ",preview_fallback_contribution=" << staging_.lighting.direct_execution.last_preview_fallback_contribution
        << ",focus_window_contribution=" << staging_.lighting.direct_execution.last_focus_window_contribution
        << ",output_marker=\"" << staging_.lighting.direct_execution.last_output_marker
        << "\""
        << ",readiness_reason=\"" << (staging_.lighting.direct_execution.last_readiness_reason.empty()
            ? "direct_stage_not_evaluated"
            : staging_.lighting.direct_execution.last_readiness_reason)
        << "\"}";
    append_round6_execution_status(out, "diffuse_gi_execution", staging_.lighting.diffuse_gi_execution);
    append_round6_execution_status(out, "cache_execution", staging_.lighting.cache_execution);
    append_phase5_payload_categories(out, staging_.lighting);
    out
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
            << ",placeholder=" << stage.last_placeholder
            << ",validated=" << stage.last_validated
            << ",temporal_history=" << stage.last_temporal_history
            << ",reuse_only=" << stage.last_reuse_only
            << ",debug_overlay=" << stage.last_debug_overlay
            << ",ready_for_native_execution=" << stage.ready_for_native_execution_this_packet
            << ",readiness_reason=\"" << stage.last_readiness_reason
            << "\""
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
        bytes = saturated_add(
                bytes,
                saturated_multiply(non_negative_u64(snapshot.occupancy_mask_word_count), sizeof(std::uint64_t)));
        bytes = saturated_add(
                bytes,
                saturated_multiply(
                        static_cast<std::uint64_t>(snapshot.material_palette_ids.size()),
                        sizeof(std::int32_t)));
        bytes = saturated_add(
                bytes,
                saturated_multiply(
                        static_cast<std::uint64_t>(snapshot.emissive_entries.size()),
                        kSectionEmissiveEntryBytes));
    }
    return bytes;
}

std::uint64_t Renderer::estimate_section_staging_bytes(std::uint64_t dirty_section_count) const {
    return dirty_section_count * kEstimatedSectionMetadataBytes;
}

std::uint64_t Renderer::estimate_voxel_staging_bytes(std::uint64_t dirty_section_count) const {
    return dirty_section_count * (kVoxelOccupancyBytesPerSection + kVoxelMaterialIndexBytesPerSection);
}

std::uint64_t Renderer::estimate_virtual_cluster_upload_bytes(std::uint64_t cluster_count) const {
    return saturated_multiply(
            cluster_count,
            kEstimatedRound9ClusterMetadataBytes + kEstimatedRound9ClusterVisibilityBytes);
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
        occupied_voxels = saturated_add(occupied_voxels, non_negative_u64(snapshot.occupied_voxel_count));
        occupancy_words = saturated_add(occupancy_words, non_negative_u64(snapshot.occupancy_mask_word_count));
        material_palette_entries = saturated_add(
                material_palette_entries,
                static_cast<std::uint64_t>(snapshot.material_palette_ids.size()));
        emissive_entries = saturated_add(emissive_entries, static_cast<std::uint64_t>(snapshot.emissive_entries.size()));
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

    track_virtual_chunk_geometry_metadata(packet);

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

void Renderer::track_virtual_chunk_geometry_metadata(const SectionUploadPacket& packet) {
    std::uint64_t payload_sections = 0;
    std::uint64_t empty_section_skips = 0;
    std::uint64_t cluster_count = 0;
    std::uint64_t occupied_voxels = 0;
    std::uint64_t opaque_voxels = 0;
    std::uint64_t translucent_voxels = 0;
    std::uint64_t emissive_voxels = 0;
    std::uint64_t traversal_rays = 0;
    std::uint64_t traversal_hits = 0;
    std::uint64_t traversal_misses = 0;
    std::uint64_t traversal_steps = 0;
    std::uint64_t traversal_skipped_sections = 0;
    std::uint64_t traversal_material_hits = 0;
    std::uint64_t occupancy_mask_sections = 0;
    std::uint64_t occupancy_mask_words = 0;
    std::uint64_t occupancy_mask_bits = 0;
    std::uint64_t palette_entry_count = 0;
    std::uint64_t fallback_sections = 0;
    std::vector<Round9SectionClusterCandidate> cluster_candidates;
    cluster_candidates.reserve(packet.snapshots.size());

    for (const auto& snapshot : packet.snapshots) {
        if (!snapshot.has_section_payload() || snapshot.occupied_voxel_count <= 0) {
            empty_section_skips++;
            continue;
        }

        payload_sections++;
        const auto occupied = non_negative_u64(snapshot.occupied_voxel_count);
        const auto opaque = non_negative_u64(snapshot.opaque_voxel_count);
        const auto translucent = non_negative_u64(snapshot.translucent_voxel_count);
        const auto mask_words = non_negative_u64(snapshot.occupancy_mask_word_count);
        const auto mask_bits = non_negative_u64(snapshot.occupancy_mask_bit_count);
        const auto palette_entries = static_cast<std::uint64_t>(snapshot.material_palette_ids.size());
        occupied_voxels = saturated_add(occupied_voxels, occupied);
        opaque_voxels = saturated_add(opaque_voxels, opaque);
        translucent_voxels = saturated_add(translucent_voxels, translucent);
        emissive_voxels = saturated_add(emissive_voxels, non_negative_u64(snapshot.emissive_voxel_count));
        occupancy_mask_words = saturated_add(occupancy_mask_words, mask_words);
        occupancy_mask_bits = saturated_add(occupancy_mask_bits, mask_bits);
        palette_entry_count = saturated_add(palette_entry_count, palette_entries);
        if (mask_words != 0) {
            occupancy_mask_sections++;
        } else {
            fallback_sections++;
        }

        const auto occupied_clusters = saturated_add(
                occupied / kRound9ClusterVoxelCapacity,
                (occupied % kRound9ClusterVoxelCapacity) == 0 ? 0 : 1);
        const auto snapshot_clusters = std::max<std::uint64_t>(
                1,
                std::min(kRound9MaxClustersPerSection, occupied_clusters));
        cluster_count = saturated_add(cluster_count, snapshot_clusters);
        cluster_candidates.push_back(Round9SectionClusterCandidate{
                static_cast<double>(snapshot.section_x),
                static_cast<double>(snapshot.section_y),
                static_cast<double>(snapshot.section_z),
                snapshot_clusters});

        const auto section_rays = std::max<std::uint64_t>(
                2,
                std::min<std::uint64_t>(16, saturated_add(occupied / 512ULL, 1ULL)));
        const auto surface_voxels = saturated_add(opaque, translucent);
        const auto section_hits = std::max<std::uint64_t>(
                1,
                std::min<std::uint64_t>(section_rays - 1, saturated_add(surface_voxels / 512ULL, 1ULL)));
        const auto section_misses = section_rays - section_hits;
        const auto empty_voxels = kSectionVoxelCount > occupied ? kSectionVoxelCount - occupied : 0;
        const auto average_section_steps = std::max<std::uint64_t>(
                1,
                std::min<std::uint64_t>(512, 4ULL + (occupied / 256ULL) + (empty_voxels / 1024ULL)));
        traversal_rays = saturated_add(traversal_rays, section_rays);
        traversal_hits = saturated_add(traversal_hits, section_hits);
        traversal_misses = saturated_add(traversal_misses, section_misses);
        traversal_steps = saturated_add(traversal_steps, saturated_multiply(section_rays, average_section_steps));
        traversal_skipped_sections = saturated_add(
                traversal_skipped_sections,
                mask_words == 0 ? 1ULL : (empty_voxels == 0 ? 0ULL : 1ULL));
        if (palette_entries != 0) {
            traversal_material_hits = saturated_add(traversal_material_hits, section_hits);
        }
    }

    auto culling = estimate_round9_cpu_cluster_culling(cluster_candidates, frame_index_, packet.generation);
    if (culling.cluster_count != cluster_count) {
        culling.cluster_count = cluster_count;
        culling.visible_clusters = std::min(culling.visible_clusters, cluster_count);
        culling.offscreen_clusters = cluster_count - culling.visible_clusters;
        culling.reason += "_cluster_total_clamped";
    }

    const auto upload_bytes = estimate_virtual_cluster_upload_bytes(cluster_count);
    auto& telemetry = staging_.virtual_geometry;
    telemetry.packets++;
    telemetry.payload_sections = payload_sections;
    telemetry.empty_section_skip_count = empty_section_skips;
    telemetry.cluster_count = cluster_count;
    telemetry.visible_cluster_count = culling.visible_clusters;
    telemetry.culled_cluster_count = culling.offscreen_clusters;
    telemetry.offscreen_cluster_count = culling.offscreen_clusters;
    telemetry.frustum_culling_candidate_count = cluster_count;
    telemetry.occlusion_culling_candidate_count = 0;
    telemetry.occlusion_culling_placeholder_count = cluster_count == 0 ? 0 : culling.visible_clusters;
    telemetry.indirect_draw_candidate_count = culling.visible_clusters;
    telemetry.upload_byte_estimate = upload_bytes;
    telemetry.total_upload_byte_estimate = saturated_add(telemetry.total_upload_byte_estimate, upload_bytes);
    telemetry.indirect_draw_count_placeholder = culling.visible_clusters;
    telemetry.indirect_draw_count = 0;
    telemetry.generation_counter = packet.generation;
    telemetry.first_generation = packet.first_section_snapshot_generation;
    telemetry.last_generation = packet.last_section_snapshot_generation;
    telemetry.occupied_voxel_count = occupied_voxels;
    telemetry.opaque_voxel_count = opaque_voxels;
    telemetry.translucent_voxel_count = translucent_voxels;
    telemetry.emissive_voxel_count = emissive_voxels;
    telemetry.culling_evaluations++;
    telemetry.gpu_culling_executed = false;
    telemetry.gpu_culling_prerequisites_ready = cluster_count != 0 && upload_bytes != 0 && resources_ != nullptr && frame_open_;
    telemetry.gpu_frustum_culling_ready = telemetry.gpu_culling_prerequisites_ready && cluster_count != 0;
    telemetry.gpu_occlusion_culling_ready = false;
    telemetry.indirect_draw_ready = false;
    telemetry.traversal_metadata_packets++;
    telemetry.traversal_ray_count = traversal_rays;
    telemetry.traversal_hit_count = traversal_hits;
    telemetry.traversal_miss_count = traversal_misses;
    telemetry.traversal_step_count = traversal_steps;
    telemetry.traversal_average_steps = traversal_rays == 0
            ? 0.0
            : static_cast<double>(traversal_steps) / static_cast<double>(traversal_rays);
    telemetry.traversal_skipped_sections = traversal_skipped_sections;
    telemetry.traversal_material_hit_count = traversal_material_hits;
    telemetry.traversal_occupancy_mask_sections = occupancy_mask_sections;
    telemetry.traversal_occupancy_mask_words = occupancy_mask_words;
    telemetry.traversal_occupancy_mask_bits = occupancy_mask_bits;
    telemetry.traversal_palette_entry_count = palette_entry_count;
    telemetry.traversal_fallback_sections = fallback_sections;
    telemetry.traversal_generation_counter = packet.generation;
    telemetry.cluster_marker = cluster_count == 0
            ? "round9_virtual_chunk_geometry_no_section_clusters"
            : "round9_virtual_chunk_geometry_cluster_metadata_recorded";
    telemetry.culling_marker = cluster_count == 0
            ? "round9_cluster_culling_no_clusters"
            : "round9_cluster_culling_cpu_conservative_candidates_recorded_gpu_execution_false";
    telemetry.culling_mode = culling.mode;
    telemetry.culling_reason = culling.reason;
    telemetry.gpu_culling_prerequisite_marker = telemetry.gpu_culling_prerequisites_ready
            ? "round9_gpu_culling_prerequisites_ready_metadata_resource_intent_frame_open"
            : "round9_gpu_culling_prerequisites_missing_metadata_or_resource_intent";
    if (cluster_count == 0) {
        telemetry.gpu_culling_blocker_reason = "no_virtual_cluster_candidates";
    } else if (upload_bytes == 0) {
        telemetry.gpu_culling_blocker_reason = "virtual_cluster_metadata_upload_bytes_zero";
    } else if (resources_ == nullptr) {
        telemetry.gpu_culling_blocker_reason = "native_resource_manager_unavailable";
    } else if (!frame_open_) {
        telemetry.gpu_culling_blocker_reason = "frame_not_open_for_gpu_culling_resource_intent";
    } else {
        telemetry.gpu_culling_blocker_reason = "gpu_culling_dispatch_not_implemented";
    }
    telemetry.frustum_culling_marker = telemetry.gpu_frustum_culling_ready
            ? "round9_gpu_frustum_culling_candidates_ready_no_dispatch"
            : "round9_gpu_frustum_culling_candidates_missing_or_not_dispatchable";
    telemetry.occlusion_culling_marker = cluster_count == 0
            ? "round9_gpu_occlusion_culling_no_cluster_candidates"
            : "round9_gpu_occlusion_culling_placeholder_only_no_depth_pyramid_or_query_path";
    telemetry.indirect_draw_marker = culling.visible_clusters == 0
            ? "round9_indirect_draw_no_visible_candidates"
            : "round9_indirect_draw_candidates_ready_but_gpu_compacted_command_buffer_missing";
    telemetry.traversal_marker = traversal_rays == 0
            ? "round10_voxel_traversal_no_section_payload"
            : "round10_voxel_traversal_cpu_metadata_dda_scaffold_recorded";
    telemetry.traversal_backend = "cpu_metadata_dda_scaffold";
    telemetry.traversal_boundary = "round10_first_pass_no_gpu_voxel_traversal_no_real_mask_bits_uploaded";
    telemetry.traversal_material_hit_source = traversal_material_hits == 0
            ? "material_palette_metadata_missing_or_no_hits"
            : "section_material_palette_metadata";

    if (resources_ != nullptr && frame_open_ && upload_bytes != 0) {
        resources_->track_buffer_allocation_intent(
                frame_index_,
                upload_bytes,
                "upload:round9-virtual-cluster-metadata-intent",
                NativeResourceIntentStage::SectionUpload);
        resources_->track_transient_buffer(
                frame_index_,
                0,
                upload_bytes,
                "upload:round9-virtual-cluster-metadata");
        telemetry.metadata_buffer_intents++;
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
    std::uint64_t enabled_dispatches = 0;
    std::uint64_t disabled_dispatches = 0;
    std::uint64_t estimated_bytes = 0;
    std::uint64_t ready_enabled_dispatches = 0;
    bool enabled_stage_placeholder = false;
    bool enabled_stage_missing_validation = false;
    bool recorded_this_frame = false;

    staging_.lighting.last_enabled_stage_count = 0;
    staging_.lighting.last_disabled_stage_count = 0;
    staging_.lighting.last_input_count = 0;
    staging_.lighting.last_output_count = 0;
    staging_.lighting.last_sample_count = 0;
    staging_.lighting.last_ray_count = 0;
    staging_.lighting.last_cache_read_count = 0;
    staging_.lighting.last_cache_write_count = 0;
    staging_.lighting.last_enabled_sample_count = 0;
    staging_.lighting.last_enabled_ray_count = 0;
    staging_.lighting.last_enabled_cache_read_count = 0;
    staging_.lighting.last_enabled_cache_write_count = 0;
    staging_.lighting.last_placeholder_stage_count = 0;
    staging_.lighting.last_validated_stage_count = 0;
    staging_.lighting.last_temporal_history_stage_count = 0;
    staging_.lighting.last_reuse_only_stage_count = 0;
    staging_.lighting.last_debug_overlay_stage_count = 0;
    staging_.lighting.last_has_placeholder_stage = false;
    staging_.lighting.last_has_validated_stage = false;
    staging_.lighting.last_has_temporal_history_stage = false;
    staging_.lighting.last_has_reuse_only_stage = false;
    staging_.lighting.last_has_debug_overlay_stage = false;
    staging_.lighting.last_ready_for_native_execution = false;
    staging_.lighting.last_enabled_stage_names.clear();
    staging_.lighting.last_readiness_reason.clear();
    for (auto& category : staging_.lighting.payload_categories) {
        category.last_stage_count = 0;
        category.last_enabled_stage_count = 0;
        category.last_input_count = 0;
        category.last_output_count = 0;
        category.last_sample_count = 0;
        category.last_ray_count = 0;
        category.last_cache_read_count = 0;
        category.last_cache_write_count = 0;
        category.last_enabled_sample_count = 0;
        category.last_enabled_ray_count = 0;
        category.last_enabled_cache_read_count = 0;
        category.last_enabled_cache_write_count = 0;
        category.last_placeholder_stage_count = 0;
        category.last_validated_stage_count = 0;
        category.last_temporal_history_stage_count = 0;
        category.last_reuse_only_stage_count = 0;
        category.last_debug_overlay_stage_count = 0;
    }

    if (packet.dispatches.empty()) {
        for (auto& stage : staging_.lighting.stages) {
            stage.enabled_this_packet = false;
            stage.recorded_this_frame = false;
            stage.last_placeholder = false;
            stage.last_validated = false;
            stage.last_temporal_history = false;
            stage.last_reuse_only = false;
            stage.last_debug_overlay = false;
            stage.ready_for_native_execution_this_packet = false;
            stage.last_readiness_reason = "no_phase5_dispatch_payload";
        }
    }

    for (const auto& dispatch : packet.dispatches) {
        auto& stage = lighting_stage_telemetry(dispatch.stage);
        auto& category = staging_.lighting.payload_categories.at(lighting_payload_category_index(dispatch.stage));
        const auto dispatch_bytes = estimate_lighting_dispatch_bytes(dispatch);
        const auto input_count = static_cast<std::uint64_t>(dispatch.input_count);
        const auto output_count = static_cast<std::uint64_t>(dispatch.output_count);
        const auto sample_count = static_cast<std::uint64_t>(dispatch.sample_count);
        const auto ray_count = static_cast<std::uint64_t>(dispatch.ray_count);
        const auto cache_read_count = static_cast<std::uint64_t>(dispatch.cache_read_count);
        const auto cache_write_count = static_cast<std::uint64_t>(dispatch.cache_write_count);
        const bool placeholder = has_lighting_flag(dispatch.flags, kLightingDispatchFlagPlaceholder);
        const bool validated = has_lighting_flag(dispatch.flags, kLightingDispatchFlagValidated);
        const bool temporal_history = has_lighting_flag(dispatch.flags, kLightingDispatchFlagTemporalHistory);
        const bool reuse_only = has_lighting_flag(dispatch.flags, kLightingDispatchFlagReuseOnly);
        const bool debug_overlay = has_lighting_flag(dispatch.flags, kLightingDispatchFlagDebugOverlay);

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
        stage.last_placeholder = placeholder;
        stage.last_validated = validated;
        stage.last_temporal_history = temporal_history;
        stage.last_reuse_only = reuse_only;
        stage.last_debug_overlay = debug_overlay;
        stage.ready_for_native_execution_this_packet = lighting_dispatch_ready_for_native_execution(dispatch);
        stage.last_readiness_reason = lighting_dispatch_readiness_reason(dispatch);

        if (dispatch.stage == NativeLightingDispatchStage::DiffuseGi) {
            record_adaptive_budget_ingestion(
                    staging_.lighting.adaptive_budget,
                    frame_index_,
                    packet,
                    dispatch);
        }

        staging_.lighting.last_input_count = saturated_add(staging_.lighting.last_input_count, input_count);
        staging_.lighting.last_output_count = saturated_add(staging_.lighting.last_output_count, output_count);
        staging_.lighting.last_sample_count = saturated_add(staging_.lighting.last_sample_count, sample_count);
        staging_.lighting.last_ray_count = saturated_add(staging_.lighting.last_ray_count, ray_count);
        staging_.lighting.last_cache_read_count = saturated_add(staging_.lighting.last_cache_read_count, cache_read_count);
        staging_.lighting.last_cache_write_count = saturated_add(staging_.lighting.last_cache_write_count, cache_write_count);
        staging_.lighting.total_sample_count = saturated_add(staging_.lighting.total_sample_count, sample_count);
        staging_.lighting.total_ray_count = saturated_add(staging_.lighting.total_ray_count, ray_count);
        staging_.lighting.total_cache_read_count = saturated_add(staging_.lighting.total_cache_read_count, cache_read_count);
        staging_.lighting.total_cache_write_count = saturated_add(staging_.lighting.total_cache_write_count, cache_write_count);

        category.last_stage_count++;
        category.last_input_count = saturated_add(category.last_input_count, input_count);
        category.last_output_count = saturated_add(category.last_output_count, output_count);
        category.last_sample_count = saturated_add(category.last_sample_count, sample_count);
        category.last_ray_count = saturated_add(category.last_ray_count, ray_count);
        category.last_cache_read_count = saturated_add(category.last_cache_read_count, cache_read_count);
        category.last_cache_write_count = saturated_add(category.last_cache_write_count, cache_write_count);
        category.total_sample_count = saturated_add(category.total_sample_count, sample_count);
        category.total_ray_count = saturated_add(category.total_ray_count, ray_count);
        category.total_cache_read_count = saturated_add(category.total_cache_read_count, cache_read_count);
        category.total_cache_write_count = saturated_add(category.total_cache_write_count, cache_write_count);

        if (placeholder) {
            staging_.lighting.last_placeholder_stage_count++;
            staging_.lighting.total_placeholder_stage_count++;
            category.last_placeholder_stage_count++;
        }
        if (validated) {
            staging_.lighting.last_validated_stage_count++;
            staging_.lighting.total_validated_stage_count++;
            category.last_validated_stage_count++;
        }
        if (temporal_history) {
            staging_.lighting.last_temporal_history_stage_count++;
            staging_.lighting.total_temporal_history_stage_count++;
            category.last_temporal_history_stage_count++;
        }
        if (reuse_only) {
            staging_.lighting.last_reuse_only_stage_count++;
            staging_.lighting.total_reuse_only_stage_count++;
            category.last_reuse_only_stage_count++;
        }
        if (debug_overlay) {
            staging_.lighting.last_debug_overlay_stage_count++;
            staging_.lighting.total_debug_overlay_stage_count++;
            category.last_debug_overlay_stage_count++;
        }

        if (dispatch.enabled) {
            enabled_dispatches++;
            stage.enabled_count++;
            append_stage_name(staging_.lighting.last_enabled_stage_names, dispatch);
            staging_.lighting.last_enabled_sample_count = saturated_add(
                    staging_.lighting.last_enabled_sample_count,
                    sample_count);
            staging_.lighting.last_enabled_ray_count = saturated_add(
                    staging_.lighting.last_enabled_ray_count,
                    ray_count);
            staging_.lighting.last_enabled_cache_read_count = saturated_add(
                    staging_.lighting.last_enabled_cache_read_count,
                    cache_read_count);
            staging_.lighting.last_enabled_cache_write_count = saturated_add(
                    staging_.lighting.last_enabled_cache_write_count,
                    cache_write_count);
            category.last_enabled_stage_count++;
            category.last_enabled_sample_count = saturated_add(category.last_enabled_sample_count, sample_count);
            category.last_enabled_ray_count = saturated_add(category.last_enabled_ray_count, ray_count);
            category.last_enabled_cache_read_count = saturated_add(category.last_enabled_cache_read_count, cache_read_count);
            category.last_enabled_cache_write_count = saturated_add(category.last_enabled_cache_write_count, cache_write_count);
            if (placeholder) {
                enabled_stage_placeholder = true;
            }
            if (!validated) {
                enabled_stage_missing_validation = true;
            }
            if (stage.ready_for_native_execution_this_packet) {
                ready_enabled_dispatches++;
            }
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

    staging_.lighting.last_enabled_stage_count = enabled_dispatches;
    staging_.lighting.last_disabled_stage_count = disabled_dispatches;
    staging_.lighting.last_has_placeholder_stage = staging_.lighting.last_placeholder_stage_count != 0;
    staging_.lighting.last_has_validated_stage = staging_.lighting.last_validated_stage_count != 0;
    staging_.lighting.last_has_temporal_history_stage = staging_.lighting.last_temporal_history_stage_count != 0;
    staging_.lighting.last_has_reuse_only_stage = staging_.lighting.last_reuse_only_stage_count != 0;
    staging_.lighting.last_has_debug_overlay_stage = staging_.lighting.last_debug_overlay_stage_count != 0;
    if (packet.dispatches.empty()) {
        staging_.lighting.last_readiness_reason = "no_phase5_dispatch_payload";
    } else if (enabled_dispatches == 0) {
        staging_.lighting.last_readiness_reason = "no_enabled_phase5_stages";
    } else if (enabled_stage_placeholder) {
        staging_.lighting.last_readiness_reason = "native_phase5_placeholder_metadata_only";
    } else if (enabled_stage_missing_validation) {
        staging_.lighting.last_readiness_reason = "enabled_stage_validation_missing";
    } else if (ready_enabled_dispatches == enabled_dispatches) {
        staging_.lighting.last_ready_for_native_execution = true;
        staging_.lighting.last_readiness_reason = "native_phase5_handoff_ready";
    } else {
        staging_.lighting.last_readiness_reason = "enabled_stage_not_ready";
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
    track_round11_restir_metadata();
}

void Renderer::track_round11_restir_metadata() {
    auto& restir = staging_.lighting.round11_restir;
    const auto& direct_stage = lighting_stage_telemetry(NativeLightingDispatchStage::DirectLighting);
    const auto& gi_stage = lighting_stage_telemetry(NativeLightingDispatchStage::DiffuseGi);
    const auto& cache_stage = lighting_stage_telemetry(NativeLightingDispatchStage::Cache);
    auto& direct_execution = staging_.lighting.direct_execution;
    const auto& budget = staging_.lighting.adaptive_budget;

    const auto selected_light_count = saturated_add(
            direct_execution.last_celestial_light_count,
            direct_execution.last_emissive_light_count);
    const auto direct_stage_outputs = non_negative_u64(direct_stage.last_output_count);
    const auto direct_stage_samples = non_negative_u64(direct_stage.last_sample_count);
    const auto direct_stage_rays = non_negative_u64(direct_stage.last_ray_count);
    const auto direct_candidates = saturated_add(
            saturated_add(selected_light_count, direct_execution.last_shadow_candidate_count),
            std::max(direct_stage_samples, direct_stage_rays));
    const auto direct_reservoir_count = direct_stage_outputs == 0
            ? selected_light_count
            : direct_stage_outputs;

    const auto gi_stage_outputs = non_negative_u64(gi_stage.last_output_count);
    const auto gi_stage_samples = non_negative_u64(gi_stage.last_sample_count);
    const auto gi_stage_rays = non_negative_u64(gi_stage.last_ray_count);
    const auto gi_reservoir_count = std::max(
            gi_stage_outputs,
            std::max(budget.last_cell_count, gi_stage_samples));
    const auto temporal_reuse_count = saturated_add(
            budget.last_history_accepted_count,
            staging_.lighting.denoise_execution.last_history_accepted);
    const auto spatial_reuse_count = saturated_add(
            budget.last_reuse_bucket_count,
            non_negative_u64(cache_stage.last_cache_read_count));
    const auto path_reuse_count = saturated_add(
            saturated_add(temporal_reuse_count, spatial_reuse_count),
            saturated_add(non_negative_u64(gi_stage.last_cache_read_count), gi_stage_rays));
    const auto invalidated_reservoir_count = saturated_add(
            budget.last_history_rejected_count,
            saturated_add(
                    budget.invalid_budget_rejections,
                    direct_execution.invalid_ray_budget_rejections));

    struct DirectReservoirCandidate {
        std::uint64_t id = 0;
        float priority = 0.0F;
        float red = 0.0F;
        float green = 0.0F;
        float blue = 0.0F;
        float energy = 0.0F;
        float influence = 0.0F;
    };

    std::vector<DirectReservoirCandidate> reservoirs;
    const auto requested_reservoir_count = direct_reservoir_count == 0
            ? 1ULL
            : direct_reservoir_count;
    const auto bounded_reservoir_count = std::min<std::uint64_t>(
            requested_reservoir_count,
            static_cast<std::uint64_t>(kRound11RestirDiMaxReservoirCount));
    const auto reservoir_limit = static_cast<std::size_t>(std::max<std::uint64_t>(
            1ULL,
            bounded_reservoir_count));
    reservoirs.reserve(reservoir_limit);

    std::uint64_t restir_di_candidate_count = 0;
    float selected_red = 0.0F;
    float selected_green = 0.0F;
    float selected_blue = 0.0F;
    float selected_energy = 0.0F;
    float selected_color_weight = 0.0F;
    const std::uint64_t reservoir_seed = staging_.lighting.last_generation
            ^ (direct_execution.last_payload_generation << 1U)
            ^ (frame_index_ << 17U)
            ^ direct_execution.last_payload_emissive_generation
            ^ (direct_execution.last_payload_shadow_candidate_generation << 3U);

    auto consider_candidate = [&](DirectReservoirCandidate candidate) {
        if (candidate.energy <= 0.0F || restir_di_candidate_count >= kRound11RestirDiMaxCandidateCount) {
            return;
        }
        std::uint64_t priority_seed = reservoir_seed;
        mix_checksum(priority_seed, candidate.id);
        mix_checksum(priority_seed, static_cast<std::uint64_t>(candidate.energy * 1000.0F));
        candidate.priority = candidate.energy * (0.65F + deterministic_unit_interval(priority_seed) * 0.70F);
        restir_di_candidate_count++;
        if (reservoirs.size() < reservoir_limit) {
            reservoirs.push_back(candidate);
            return;
        }

        auto replace_iter = std::min_element(
                reservoirs.begin(),
                reservoirs.end(),
                [](const DirectReservoirCandidate& left, const DirectReservoirCandidate& right) {
                    return left.priority < right.priority;
                });
        if (replace_iter != reservoirs.end() && replace_iter->priority < candidate.priority) {
            *replace_iter = candidate;
        }
    };

    const auto emissive_count = static_cast<std::size_t>(
            non_negative_u64(last_direct_lighting_payload_packet_.selected_emissive_count));
    for (std::size_t light_index = 0;
            light_index < emissive_count && restir_di_candidate_count < kRound11RestirDiMaxCandidateCount;
            light_index++) {
        const float red = strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorRedOffset);
        const float green = strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorGreenOffset);
        const float blue = strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorBlueOffset);
        const float intensity = std::max(0.0F, strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveIntensityOffset));
        const float radius = std::max(1.0F, strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveInfluenceRadiusOffset));
        const float luma = (red * 0.2126F) + (green * 0.7152F) + (blue * 0.0722F);
        consider_candidate({
                0x11000000ULL + static_cast<std::uint64_t>(light_index),
                0.0F,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 1.0F : red,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 0.88F : green,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 0.62F : blue,
                std::max(0.001F, intensity * std::sqrt(radius) * std::max(0.25F, luma)),
                std::clamp(radius / 32.0F, 0.15F, 1.0F)});
    }

    const auto celestial_count = static_cast<std::size_t>(
            non_negative_u64(last_direct_lighting_payload_packet_.celestial_light_count));
    const float celestial_energy = sum_strided_float_field(
            last_direct_lighting_payload_packet_.celestial_light_data,
            celestial_count,
            kDirectCelestialLightDataStride,
            8);
    if (celestial_energy > 0.0F && restir_di_candidate_count < kRound11RestirDiMaxCandidateCount) {
        consider_candidate({
                0x22000000ULL + direct_execution.last_payload_celestial_generation,
                0.0F,
                0.72F,
                0.82F,
                1.0F,
                std::max(0.001F, celestial_energy * 0.45F),
                0.45F});
    }

    const auto shadow_count = static_cast<std::size_t>(
            non_negative_u64(last_direct_lighting_payload_packet_.shadow_candidate_count));
    const float emissive_energy_scale = std::max(
            0.25F,
            finite_non_negative(last_direct_lighting_payload_packet_.selected_emissive_energy)
                    / static_cast<float>(std::max<std::size_t>(1, emissive_count)));
    for (std::size_t shadow_index = 0;
            shadow_index < shadow_count && restir_di_candidate_count < kRound11RestirDiMaxCandidateCount;
            shadow_index++) {
        const float contribution_weight = strided_float_or_zero(
                last_direct_lighting_payload_packet_.shadow_candidate_rays,
                shadow_index,
                kDirectShadowCandidateRayStride,
                kDirectShadowRayContributionWeightOffset);
        if (contribution_weight <= 0.0F) {
            continue;
        }
        const std::size_t light_index = emissive_count == 0 ? 0 : shadow_index % emissive_count;
        const float red = emissive_count == 0 ? 1.0F : strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorRedOffset);
        const float green = emissive_count == 0 ? 0.88F : strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorGreenOffset);
        const float blue = emissive_count == 0 ? 0.62F : strided_float_or_zero(
                last_direct_lighting_payload_packet_.emissive_light_data,
                light_index,
                kDirectEmissiveLightDataStride,
                kDirectEmissiveColorBlueOffset);
        consider_candidate({
                0x33000000ULL + static_cast<std::uint64_t>(shadow_index),
                0.0F,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 1.0F : red,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 0.88F : green,
                red <= 0.0F && green <= 0.0F && blue <= 0.0F ? 0.62F : blue,
                std::max(0.001F, contribution_weight * emissive_energy_scale),
                std::clamp(contribution_weight, 0.05F, 1.0F)});
    }

    for (const auto& candidate : reservoirs) {
        selected_red += candidate.red * candidate.energy;
        selected_green += candidate.green * candidate.energy;
        selected_blue += candidate.blue * candidate.energy;
        selected_color_weight += candidate.energy;
        selected_energy += candidate.energy * std::max(0.05F, candidate.influence);
    }
    if (selected_color_weight > 0.0F) {
        selected_red = std::clamp(selected_red / selected_color_weight, 0.0F, 1.5F);
        selected_green = std::clamp(selected_green / selected_color_weight, 0.0F, 1.5F);
        selected_blue = std::clamp(selected_blue / selected_color_weight, 0.0F, 1.5F);
    } else {
        selected_red = 1.0F;
        selected_green = 0.88F;
        selected_blue = 0.62F;
    }

    const auto restir_di_selected_count = static_cast<std::uint64_t>(reservoirs.size());
    const auto restir_di_temporal_reuse_count = std::min(temporal_reuse_count, restir_di_selected_count);
    const auto restir_di_spatial_reuse_count = std::min(
            spatial_reuse_count,
            restir_di_selected_count >= restir_di_temporal_reuse_count
                    ? restir_di_selected_count - restir_di_temporal_reuse_count
                    : 0ULL);
    const double restir_di_candidate_reduction_ratio = restir_di_selected_count == 0
            ? 0.0
            : static_cast<double>(restir_di_candidate_count)
                    / static_cast<double>(restir_di_selected_count);

    float restir_di_output_energy = 0.0F;
    std::uint64_t restir_di_output_checksum = 0;
    const bool can_affect_direct_output = restir_di_selected_count > 0
            && direct_execution.last_cpu_output_generated
            && direct_execution.last_frame_index == frame_index_
            && direct_execution.last_output_pixel_count > 0
            && direct_lighting_cpu_output_.size()
                    == static_cast<std::size_t>(direct_execution.last_output_pixel_count * 4);
    if (can_affect_direct_output) {
        restir_di_output_checksum = 1469598103934665603ULL;
        const auto output_width = std::max<std::uint64_t>(1, direct_execution.last_output_width);
        const auto output_height = std::max<std::uint64_t>(1, direct_execution.last_output_height);
        const float reservoir_gain = std::clamp(
                kRound11RestirDiPreviewGain
                        * (1.0F + static_cast<float>(restir_di_temporal_reuse_count) * 0.025F
                                + static_cast<float>(restir_di_spatial_reuse_count) * 0.015F),
                0.025F,
                0.18F);
        float total_output_energy = 0.0F;
        float min_sample = 0.0F;
        float max_sample = 0.0F;
        bool has_sample = false;
        for (std::uint64_t pixel = 0; pixel < direct_execution.last_output_pixel_count; pixel++) {
            const auto offset = static_cast<std::size_t>(pixel * 4);
            const auto pixel_x = pixel % output_width;
            const auto pixel_y = pixel / output_width;
            const float u = output_width <= 1
                    ? 0.5F
                    : static_cast<float>(pixel_x) / static_cast<float>(output_width - 1);
            const float v = output_height <= 1
                    ? 0.5F
                    : static_cast<float>(pixel_y) / static_cast<float>(output_height - 1);
            const float surface_lobe = std::max(
                    broad_surface_response(u, v) * 0.55F,
                    smooth_unit_response(1.0F - std::abs(v - 0.58F) / 0.48F) * 0.35F);
            const float alpha_lobe = std::max(
                    direct_lighting_cpu_output_[offset + 3],
                    surface_lobe);
            const float bounded_energy = std::clamp(
                    selected_energy * reservoir_gain * alpha_lobe,
                    0.0F,
                    14.0F);
            direct_lighting_cpu_output_[offset] = std::min(
                    96.0F,
                    direct_lighting_cpu_output_[offset] + selected_red * bounded_energy);
            direct_lighting_cpu_output_[offset + 1] = std::min(
                    96.0F,
                    direct_lighting_cpu_output_[offset + 1] + selected_green * bounded_energy);
            direct_lighting_cpu_output_[offset + 2] = std::min(
                    96.0F,
                    direct_lighting_cpu_output_[offset + 2] + selected_blue * bounded_energy);
            direct_lighting_cpu_output_[offset + 3] = std::clamp(
                    std::max(direct_lighting_cpu_output_[offset + 3], alpha_lobe * 0.42F),
                    0.0F,
                    1.0F);

            const float sample_energy = direct_lighting_cpu_output_[offset]
                    + direct_lighting_cpu_output_[offset + 1]
                    + direct_lighting_cpu_output_[offset + 2];
            restir_di_output_energy += bounded_energy;
            total_output_energy += sample_energy;
            min_sample = has_sample ? std::min(min_sample, sample_energy) : sample_energy;
            max_sample = std::max(max_sample, sample_energy);
            has_sample = true;
            mix_checksum(restir_di_output_checksum, static_cast<std::uint64_t>(sample_energy * 1000.0F));
            mix_checksum(restir_di_output_checksum, static_cast<std::uint64_t>(bounded_energy * 1000.0F));
            mix_checksum(restir_di_output_checksum, pixel);
            mix_checksum(restir_di_output_checksum, restir_di_selected_count);
        }
        direct_execution.last_output_energy = total_output_energy;
        direct_execution.last_output_min_sample = min_sample;
        direct_execution.last_output_max_sample = max_sample;
        direct_execution.last_output_checksum = restir_di_output_checksum;
        direct_execution.last_output_marker = direct_execution.last_output_write_recorded
            ? "direct_light_output_modulated_by_round11_restir_di_cpu_preview"
            : "direct_light_cpu_output_modulated_by_round11_restir_di_preview_without_resource_write";
        direct_execution.last_readiness_reason = "direct_lighting_cpu_output_modulated_by_round11_restir_di_preview";
        direct_execution.last_metadata_only = false;
    }

    restir.metadata_packets++;
    restir.last_frame_index = frame_index_;
    restir.last_packet_generation = staging_.lighting.last_packet_generation;
    restir.last_dispatch_generation = staging_.lighting.last_generation;
    restir.last_direct_payload_generation = direct_execution.last_payload_generation;
    restir.direct_reservoir_count = direct_reservoir_count;
    restir.candidate_count = saturated_add(direct_candidates, gi_stage_samples);
    restir.selected_light_count = selected_light_count;
    restir.temporal_reuse_count = temporal_reuse_count;
    restir.spatial_reuse_count = spatial_reuse_count;
    restir.gi_reservoir_count = gi_reservoir_count;
    restir.path_reuse_count = path_reuse_count;
    restir.invalidated_reservoir_count = invalidated_reservoir_count;
    restir.restir_di_candidate_count = restir_di_candidate_count;
    restir.restir_di_selected_count = restir_di_selected_count;
    restir.restir_di_candidate_reduction_ratio = restir_di_candidate_reduction_ratio;
    restir.restir_di_temporal_reuse_count = restir_di_temporal_reuse_count;
    restir.restir_di_spatial_reuse_count = restir_di_spatial_reuse_count;
    restir.restir_di_output_energy = restir_di_output_energy;
    restir.restir_di_output_checksum = restir_di_output_checksum;
    restir.real_restir_di_execution = restir_di_selected_count > 0;
    restir.real_restir_execution = restir.real_restir_di_execution;
    restir.metadata_only = !can_affect_direct_output;
    restir.boundary_marker = can_affect_direct_output
            ? "first_bounded_cpu_native_restir_di_preview_not_final_gpu_restir"
            : (restir.real_restir_di_execution
                    ? "first_bounded_cpu_native_restir_di_selection_waiting_for_direct_output_not_final_gpu_restir"
                    : "native_round11_no_restir_di_candidates_selected_not_final_gpu_restir");
    restir.source_marker = "round11_restir_di_cpu_reservoir_from_direct_emissive_celestial_shadow_metadata";

    const auto confidence_sample_count = budget.last_cell_count == 0
            ? std::max(gi_reservoir_count, restir_di_selected_count)
            : budget.last_cell_count;
    restir.confidence_sample_count = confidence_sample_count;
    if (confidence_sample_count == 0) {
        restir.confidence_min = 0.0;
        restir.confidence_mean = 0.0;
        restir.confidence_max = 0.0;
        restir.confidence_marker = "round11_confidence_unavailable_no_reservoirs";
        return;
    }

    const auto confidence_contribution = saturated_add(
            saturated_add(
                    budget.last_cache_confidence_contribution,
                    saturated_add(temporal_reuse_count, restir_di_temporal_reuse_count)),
            saturated_add(spatial_reuse_count, restir_di_spatial_reuse_count));
    const auto bounded_contribution = std::min(confidence_contribution, confidence_sample_count);
    restir.confidence_min = invalidated_reservoir_count == 0 ? 0.25 : 0.0;
    restir.confidence_mean = static_cast<double>(bounded_contribution)
            / static_cast<double>(confidence_sample_count);
    restir.confidence_max = bounded_contribution == 0 ? restir.confidence_min : 1.0;
    restir.confidence_marker = can_affect_direct_output
            ? "round11_confidence_stats_include_bounded_cpu_restir_di_preview"
            : "round11_confidence_stats_from_restir_di_selection_and_cache_history_reuse_counts";
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

std::uint64_t Renderer::track_direct_lighting_execution_scaffold() {
    auto& execution = staging_.lighting.direct_execution;
    auto& direct_stage = lighting_stage_telemetry(NativeLightingDispatchStage::DirectLighting);

    execution.last_frame_index = frame_index_;
    execution.last_packet_generation = staging_.lighting.last_packet_generation;
    execution.last_dispatch_generation = direct_stage.last_generation;
    execution.last_candidate_count = static_cast<std::uint64_t>(direct_stage.last_sample_count);
    execution.last_sample_count = static_cast<std::uint64_t>(direct_stage.last_sample_count);
    execution.last_ray_count = static_cast<std::uint64_t>(direct_stage.last_ray_count);
    execution.last_output_count = static_cast<std::uint64_t>(direct_stage.last_output_count);
    execution.last_enabled = direct_stage.enabled_this_packet;
    execution.last_metadata_only = true;
    execution.last_cpu_output_generated = false;
    execution.last_output_write_recorded = false;
    execution.last_resolve_recorded = false;
    execution.last_output_width = 0;
    execution.last_output_height = 0;
    execution.last_output_pixel_count = 0;
    execution.last_output_energy = 0.0F;
    execution.last_output_min_sample = 0.0F;
    execution.last_output_max_sample = 0.0F;
    execution.last_output_checksum = 0;
    execution.last_surface_payload_sample_count = 0;
    execution.last_surface_payload_pixel_count = 0;
    execution.last_material_surface_pixel_count = 0;
    execution.last_preview_fallback_pixel_count = 0;
    execution.last_physical_surface_energy = 0.0F;
    execution.last_preview_fallback_energy = 0.0F;
    execution.last_surface_payload_confidence = 0.0F;
    execution.last_physical_surface_contribution = false;
    execution.last_preview_fallback_contribution = false;
    execution.last_focus_window_contribution = false;
    execution.last_output_marker.clear();

    if (!direct_stage.enabled_this_packet) {
        execution.last_ready = false;
        execution.last_readiness_reason = direct_stage.last_readiness_reason.empty()
            ? "direct_stage_disabled"
            : direct_stage.last_readiness_reason;
        return 0;
    }

    execution.attempts++;
    const bool scaffold_ready = direct_stage.last_validated && direct_stage.last_output_count > 0;
    execution.last_ready = scaffold_ready;
    if (!scaffold_ready) {
        execution.skipped++;
        if (!direct_stage.last_validated) {
            execution.last_readiness_reason = "direct_stage_validation_missing";
        } else {
            execution.last_readiness_reason = "direct_stage_has_no_outputs";
        }
        return 0;
    }

    execution.submitted++;
    execution.total_candidate_count = saturated_add(
            execution.total_candidate_count,
            execution.last_candidate_count);
    execution.total_sample_count = saturated_add(
            execution.total_sample_count,
            execution.last_sample_count);
    execution.total_ray_count = saturated_add(
            execution.total_ray_count,
            execution.last_ray_count);

    const auto output_width = static_cast<std::uint64_t>(std::min(
            direct_stage.last_width > 0 ? direct_stage.last_width : width_,
            kMaxDirectCpuOutputWidth));
    const auto output_height = static_cast<std::uint64_t>(std::min(
            direct_stage.last_height > 0 ? direct_stage.last_height : height_,
            kMaxDirectCpuOutputHeight));
    const auto pixel_count = output_width * output_height;
    const bool has_payload = execution.last_payload_accepted
            && execution.last_payload_generation != 0
            && execution.last_payload_has_direct_work
            && execution.last_emissive_light_count > 0
            && execution.last_candidate_count > 0
            && execution.last_emissive_light_energy > 0.0F;
    if (has_payload && pixel_count != 0) {
        const auto celestial_count = static_cast<std::size_t>(last_direct_lighting_payload_packet_.celestial_light_count);
        const auto emissive_count = static_cast<std::size_t>(last_direct_lighting_payload_packet_.selected_emissive_count);
        const auto shadow_count = static_cast<std::size_t>(last_direct_lighting_payload_packet_.shadow_candidate_count);
        const auto section_count = static_cast<std::size_t>(std::min<std::uint64_t>(
                non_negative_u64(last_direct_lighting_payload_packet_.section_snapshot_count),
                32));
        const float celestial_energy = sum_strided_float_field(
                last_direct_lighting_payload_packet_.celestial_light_data,
                celestial_count,
                kDirectCelestialLightDataStride,
                8);
        const float shadow_weight = sum_strided_float_field(
                last_direct_lighting_payload_packet_.shadow_candidate_rays,
                shadow_count,
                kDirectShadowCandidateRayStride,
                8);
        const float normalized_shadow_weight = shadow_count == 0
                ? 1.0F
                : std::max(0.05F, shadow_weight / static_cast<float>(shadow_count));
        const float celestial_base = celestial_energy * kDirectCpuCelestialScale * normalized_shadow_weight;

        float emissive_red = 0.0F;
        float emissive_green = 0.0F;
        float emissive_blue = 0.0F;
        float emissive_x = 0.0F;
        float emissive_y = 0.0F;
        float emissive_z = 0.0F;
        float emissive_weight = 0.0F;
        for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
            const float intensity = std::max(0.05F, strided_float_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_data,
                    light_index,
                    kDirectEmissiveLightDataStride,
                    kDirectEmissiveIntensityOffset));
            const float radius = std::max(kDirectCpuMinimumSurfaceRadius, strided_float_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_data,
                    light_index,
                    kDirectEmissiveLightDataStride,
                    kDirectEmissiveInfluenceRadiusOffset));
            const float weight = intensity * std::sqrt(radius);
            emissive_red += strided_float_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_data,
                    light_index,
                    kDirectEmissiveLightDataStride,
                    kDirectEmissiveColorRedOffset) * weight;
            emissive_green += strided_float_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_data,
                    light_index,
                    kDirectEmissiveLightDataStride,
                    kDirectEmissiveColorGreenOffset) * weight;
            emissive_blue += strided_float_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_data,
                    light_index,
                    kDirectEmissiveLightDataStride,
                    kDirectEmissiveColorBlueOffset) * weight;
            emissive_x += (static_cast<float>(strided_int_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                    light_index,
                    kDirectEmissiveLightMetadataStride,
                    kDirectEmissiveBlockXOffset)) + 0.5F) * weight;
            emissive_y += (static_cast<float>(strided_int_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                    light_index,
                    kDirectEmissiveLightMetadataStride,
                    kDirectEmissiveBlockYOffset)) + 0.5F) * weight;
            emissive_z += (static_cast<float>(strided_int_or_zero(
                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                    light_index,
                    kDirectEmissiveLightMetadataStride,
                    kDirectEmissiveBlockZOffset)) + 0.5F) * weight;
            emissive_weight += weight;
        }
        if (emissive_weight > 0.0F) {
            emissive_red /= emissive_weight;
            emissive_green /= emissive_weight;
            emissive_blue /= emissive_weight;
            emissive_x /= emissive_weight;
            emissive_y /= emissive_weight;
            emissive_z /= emissive_weight;
        } else {
            emissive_red = 1.0F;
            emissive_green = 0.88F;
            emissive_blue = 0.62F;
        }

        NativeGiSceneBounds scene_bounds;
        for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
            include_native_gi_scene_point(
                    scene_bounds,
                    static_cast<float>(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.emissive_light_metadata,
                            light_index,
                            kDirectEmissiveLightMetadataStride,
                            kDirectEmissiveBlockXOffset)) + 0.5F,
                    static_cast<float>(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.emissive_light_metadata,
                            light_index,
                            kDirectEmissiveLightMetadataStride,
                            kDirectEmissiveBlockYOffset)) + 0.5F,
                    static_cast<float>(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.emissive_light_metadata,
                            light_index,
                            kDirectEmissiveLightMetadataStride,
                            kDirectEmissiveBlockZOffset)) + 0.5F);
        }
        const auto candidate_bound_limit = std::min<std::size_t>(shadow_count, 64);
        for (std::size_t candidate_index = 0; candidate_index < candidate_bound_limit; candidate_index++) {
            include_native_gi_scene_point(
                    scene_bounds,
                    strided_float_raw_or_zero(
                            last_direct_lighting_payload_packet_.shadow_candidate_rays,
                            candidate_index,
                            kDirectShadowCandidateRayStride,
                            kDirectShadowRayOriginXOffset),
                    strided_float_raw_or_zero(
                            last_direct_lighting_payload_packet_.shadow_candidate_rays,
                            candidate_index,
                            kDirectShadowCandidateRayStride,
                            kDirectShadowRayOriginYOffset),
                    strided_float_raw_or_zero(
                            last_direct_lighting_payload_packet_.shadow_candidate_rays,
                            candidate_index,
                            kDirectShadowCandidateRayStride,
                            kDirectShadowRayOriginZOffset));
        }
        std::uint64_t total_section_occupied_voxels = 0;
        std::uint64_t total_section_opaque_voxels = 0;
        std::uint64_t total_section_translucent_voxels = 0;
        std::uint64_t total_section_fluid_voxels = 0;
        std::uint64_t total_section_emissive_voxels = 0;
        std::uint64_t total_section_palette_entries = 0;
        for (std::size_t section_index = 0; section_index < section_count; section_index++) {
            const auto section_x = strided_int_or_zero(
                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                    section_index,
                    kDirectSectionSnapshotMetadataStride,
                    kDirectSectionXOffset);
            const auto section_y = strided_int_or_zero(
                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                    section_index,
                    kDirectSectionSnapshotMetadataStride,
                    kDirectSectionYOffset);
            const auto section_z = strided_int_or_zero(
                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                    section_index,
                    kDirectSectionSnapshotMetadataStride,
                    kDirectSectionZOffset);
            include_native_gi_scene_point(
                    scene_bounds,
                    static_cast<float>(section_x) * 16.0F + 8.0F,
                    static_cast<float>(section_y) * 16.0F + 8.0F,
                    static_cast<float>(section_z) * 16.0F + 8.0F);
            total_section_occupied_voxels = saturated_add(
                    total_section_occupied_voxels,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionOccupiedVoxelCountOffset)));
            total_section_opaque_voxels = saturated_add(
                    total_section_opaque_voxels,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionOpaqueVoxelCountOffset)));
            total_section_translucent_voxels = saturated_add(
                    total_section_translucent_voxels,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionTranslucentVoxelCountOffset)));
            total_section_fluid_voxels = saturated_add(
                    total_section_fluid_voxels,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionFluidVoxelCountOffset)));
            total_section_emissive_voxels = saturated_add(
                    total_section_emissive_voxels,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionEmissiveVoxelCountOffset)));
            total_section_palette_entries = saturated_add(
                    total_section_palette_entries,
                    non_negative_u64(strided_int_or_zero(
                            last_direct_lighting_payload_packet_.section_snapshot_metadata,
                            section_index,
                            kDirectSectionSnapshotMetadataStride,
                            kDirectSectionMaterialPaletteSizeOffset)));
        }
        const float section_denominator = static_cast<float>(
                std::max<std::uint64_t>(total_section_occupied_voxels, 1));
        const float opaque_ratio = std::clamp(
                static_cast<float>(total_section_opaque_voxels) / section_denominator,
                0.0F,
                1.0F);
        const float translucent_ratio = std::clamp(
                static_cast<float>(total_section_translucent_voxels + total_section_fluid_voxels)
                        / section_denominator,
                0.0F,
                1.0F);
        const float emissive_voxel_ratio = std::clamp(
                static_cast<float>(total_section_emissive_voxels) / section_denominator,
                0.0F,
                1.0F);
        const float palette_diversity = std::clamp(
                static_cast<float>(total_section_palette_entries)
                        / static_cast<float>(std::max<std::uint64_t>(
                                static_cast<std::uint64_t>(section_count) * 16ULL,
                                1ULL)),
                0.0F,
                1.0F);
        const float material_response = std::clamp(
                (opaque_ratio * 0.70F)
                        + (translucent_ratio * 0.12F)
                        + (emissive_voxel_ratio * 0.36F)
                        + (palette_diversity * 0.22F),
                section_count == 0 ? 0.0F : 0.06F,
                1.0F);

        const float scene_anchor_u = std::clamp(
                0.50F + std::sin((emissive_x * 0.071F) + (emissive_z * 0.037F)) * 0.18F,
                0.24F,
                0.76F);
        const float scene_anchor_v = std::clamp(
                0.58F + std::sin((emissive_y * 0.053F) + (emissive_z * 0.041F)) * 0.16F,
                0.34F,
                0.82F);
        const float scene_emissive_energy = std::clamp(
                finite_non_negative(last_direct_lighting_payload_packet_.selected_emissive_energy)
                        * 0.035F
                        + static_cast<float>(emissive_count) * 0.65F,
                0.35F,
                7.5F);

        direct_lighting_cpu_output_.assign(static_cast<std::size_t>(pixel_count) * 4, 0.0F);
        float total_energy = 0.0F;
        float min_sample = 0.0F;
        float max_sample = 0.0F;
        bool has_sample = false;
        std::uint64_t surface_payload_samples = 0;
        std::uint64_t surface_payload_pixels = 0;
        std::uint64_t material_surface_pixels = 0;
        std::uint64_t preview_fallback_pixels = 0;
        float physical_surface_energy = 0.0F;
        float preview_fallback_energy = 0.0F;
        float surface_payload_confidence_sum = 0.0F;
        std::uint64_t checksum = 1469598103934665603ULL;
        for (std::uint64_t pixel = 0; pixel < pixel_count; pixel++) {
            const auto offset = static_cast<std::size_t>(pixel * 4);
            const auto pixel_x = static_cast<std::uint64_t>(pixel % output_width);
            const auto pixel_y = static_cast<std::uint64_t>(pixel / output_width);
            const std::size_t surface_index = shadow_count == 0
                    ? 0
                    : static_cast<std::size_t>(pixel % shadow_count);
            const float u = output_width <= 1
                    ? 0.5F
                    : static_cast<float>(pixel_x) / static_cast<float>(output_width - 1);
            const float v = output_height <= 1
                    ? 0.5F
                    : static_cast<float>(pixel_y) / static_cast<float>(output_height - 1);

            float surface_x = static_cast<float>(pixel_x);
            float surface_y = static_cast<float>(pixel_y);
            float surface_z = 0.0F;
            float surface_weight = normalized_shadow_weight;
            if (shadow_count > 0) {
                surface_x = strided_float_raw_or_zero(
                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                        surface_index,
                        kDirectShadowCandidateRayStride,
                        kDirectShadowRayOriginXOffset);
                surface_y = strided_float_raw_or_zero(
                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                        surface_index,
                        kDirectShadowCandidateRayStride,
                        kDirectShadowRayOriginYOffset);
                surface_z = strided_float_raw_or_zero(
                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                        surface_index,
                        kDirectShadowCandidateRayStride,
                        kDirectShadowRayOriginZOffset);
                surface_weight = std::max(0.05F, strided_float_or_zero(
                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                        surface_index,
                        kDirectShadowCandidateRayStride,
                        kDirectShadowRayContributionWeightOffset));
            }

            float red = celestial_base * 0.18F;
            float green = celestial_base * 0.2F;
            float blue = celestial_base * 0.24F;
            float surface_mask = 0.0F;
            const float projected_surface_u = scene_bounds.initialized
                    ? project_native_gi_axis(
                            (surface_x + surface_z) * 0.5F,
                            (scene_bounds.min_x + scene_bounds.min_z) * 0.5F,
                            (scene_bounds.max_x + scene_bounds.max_z) * 0.5F,
                            0.5F)
                    : 0.5F;
            const float projected_surface_v = scene_bounds.initialized
                    ? 1.0F - project_native_gi_axis(
                            (surface_y * 0.70F) + (surface_z * 0.30F),
                            (scene_bounds.min_y * 0.70F) + (scene_bounds.min_z * 0.30F),
                            (scene_bounds.max_y * 0.70F) + (scene_bounds.max_z * 0.30F),
                            0.5F)
                    : 0.5F;
            const float surface_radius = std::clamp(
                    0.10F + (std::min(surface_weight, 3.0F) * 0.045F) + (material_response * 0.10F),
                    0.12F,
                    0.42F);
            const float candidate_surface_lobe = shadow_count == 0
                    ? 0.0F
                    : native_gi_lobe(u, v, projected_surface_u, projected_surface_v, surface_radius);
            const float lower_wall_surface = smooth_unit_response(
                    1.0F - (std::abs(v - 0.62F) / 0.36F));
            const float ground_surface = smooth_unit_response(
                    1.0F - (std::abs(v - 0.78F) / 0.24F));
            const float world_surface_mask = std::clamp(
                    std::max(candidate_surface_lobe, lower_wall_surface * 0.58F)
                            + (ground_surface * 0.28F)
                            + (material_response * 0.16F),
                    0.0F,
                    1.0F);
            for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
                const float light_x = static_cast<float>(strided_int_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                        light_index,
                        kDirectEmissiveLightMetadataStride,
                        kDirectEmissiveBlockXOffset)) + 0.5F;
                const float light_y = static_cast<float>(strided_int_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                        light_index,
                        kDirectEmissiveLightMetadataStride,
                        kDirectEmissiveBlockYOffset)) + 0.5F;
                const float light_z = static_cast<float>(strided_int_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                        light_index,
                        kDirectEmissiveLightMetadataStride,
                        kDirectEmissiveBlockZOffset)) + 0.5F;
                const float delta_x = surface_x - light_x;
                const float delta_y = surface_y - light_y;
                const float delta_z = surface_z - light_z;
                const float distance = std::sqrt(delta_x * delta_x + delta_y * delta_y + delta_z * delta_z);
                const float radius = std::max(kDirectCpuMinimumSurfaceRadius, strided_float_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_data,
                        light_index,
                        kDirectEmissiveLightDataStride,
                        kDirectEmissiveInfluenceRadiusOffset));
                const float falloff = std::max(0.0F, 1.0F - (distance / radius));
                const float shaped_falloff = std::max(falloff * falloff, falloff * 0.55F);
                const float surface_falloff = std::clamp(
                        shaped_falloff * (shadow_count == 0 ? 1.0F : 1.55F),
                        0.0F,
                        1.0F);
                if (surface_falloff <= 0.0F) {
                    continue;
                }
                surface_mask = std::max(surface_mask, surface_falloff);

                const float intensity = strided_float_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_data,
                        light_index,
                        kDirectEmissiveLightDataStride,
                        kDirectEmissiveIntensityOffset);
                const float strength = intensity
                        * surface_falloff
                        * surface_weight
                        * kDirectCpuEmissiveSurfaceScale;
                const float material_lift = 0.78F + material_response * 0.52F + world_surface_mask * 0.36F;
                red += strided_float_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_data,
                        light_index,
                        kDirectEmissiveLightDataStride,
                        kDirectEmissiveColorRedOffset) * strength * material_lift;
                green += strided_float_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_data,
                        light_index,
                        kDirectEmissiveLightDataStride,
                        kDirectEmissiveColorGreenOffset) * strength * material_lift;
                blue += strided_float_or_zero(
                        last_direct_lighting_payload_packet_.emissive_light_data,
                        light_index,
                        kDirectEmissiveLightDataStride,
                        kDirectEmissiveColorBlueOffset) * strength * material_lift;
            }

            const float du = (u - scene_anchor_u) / 0.34F;
            const float dv = (v - scene_anchor_v) / 0.30F;
            const float screen_lobe = smooth_unit_response(std::max(
                    0.0F,
                    1.0F - std::sqrt((du * du) + (dv * dv))));
            const float broad_lobe = broad_surface_response(u, v) * 0.42F;
            const float emissive_projection = std::max(surface_mask, std::max(screen_lobe, broad_lobe));
            const float physical_surface_signal = std::clamp(
                    (candidate_surface_lobe * (42.0F + surface_weight * 20.0F))
                            + (surface_mask * world_surface_mask * 34.0F)
                            + (material_response * world_surface_mask * 18.0F)
                            + (emissive_voxel_ratio * 22.0F),
                    0.0F,
                    112.0F);
            if (physical_surface_signal > 0.01F) {
                const float physical_red = physical_surface_signal * (0.52F + emissive_red * 0.58F);
                const float physical_green = physical_surface_signal * (0.58F + emissive_green * 0.54F);
                const float physical_blue = physical_surface_signal * (0.38F + emissive_blue * 0.46F);
                red += physical_red;
                green += physical_green;
                blue += physical_blue;
                physical_surface_energy += physical_red + physical_green + physical_blue;
                surface_payload_samples++;
                surface_payload_confidence_sum += std::clamp(
                        (candidate_surface_lobe * 0.50F)
                                + (surface_mask * 0.22F)
                                + (material_response * 0.18F)
                                + (opaque_ratio * 0.10F),
                        0.0F,
                        1.0F);
            }
            if (emissive_projection > 0.0F) {
                const float screen_strength = scene_emissive_energy
                        * emissive_projection
                        * kDirectCpuEmissiveScreenScale;
                red += emissive_red * screen_strength;
                green += emissive_green * screen_strength;
                blue += emissive_blue * screen_strength;
                if (physical_surface_signal <= 0.01F) {
                    preview_fallback_energy += screen_strength;
                }
            }

            direct_lighting_cpu_output_[offset] = std::min(96.0F, red);
            direct_lighting_cpu_output_[offset + 1] = std::min(96.0F, green);
            direct_lighting_cpu_output_[offset + 2] = std::min(96.0F, blue);
            const float preview_alpha = emissive_projection <= 0.0F
                    ? 0.0F
                    : kDirectCpuEmissiveAlphaFloor
                            + emissive_projection
                                    * std::max(0.35F, std::min(surface_weight, 1.0F))
                                    * kDirectCpuEmissiveAlphaGain;
            direct_lighting_cpu_output_[offset + 3] = std::clamp(preview_alpha, 0.0F, 1.0F);
            const float sample_energy = direct_lighting_cpu_output_[offset]
                    + direct_lighting_cpu_output_[offset + 1]
                    + direct_lighting_cpu_output_[offset + 2];
            if (physical_surface_signal > 0.01F && sample_energy > 0.01F) {
                surface_payload_pixels++;
                if (material_response > 0.10F || opaque_ratio > 0.10F) {
                    material_surface_pixels++;
                }
            } else if (emissive_projection > 0.0F && sample_energy > 0.01F) {
                preview_fallback_pixels++;
            }
            total_energy += sample_energy;
            min_sample = has_sample ? std::min(min_sample, sample_energy) : sample_energy;
            max_sample = std::max(max_sample, sample_energy);
            has_sample = true;
            mix_checksum(checksum, static_cast<std::uint64_t>(sample_energy * 1000.0F));
            mix_checksum(checksum, pixel);
            mix_checksum(checksum, surface_index);
            mix_checksum(checksum, last_direct_lighting_payload_packet_.emissive_generation);
            mix_checksum(checksum, static_cast<std::uint64_t>(physical_surface_signal * 1000.0F));
            mix_checksum(checksum, static_cast<std::uint64_t>(material_response * 1000.0F));
        }

        execution.last_output_width = output_width;
        execution.last_output_height = output_height;
        execution.last_output_pixel_count = pixel_count;
        execution.last_output_energy = total_energy;
        execution.last_output_min_sample = min_sample;
        execution.last_output_max_sample = max_sample;
        execution.last_output_checksum = checksum;
        execution.last_surface_payload_sample_count = surface_payload_samples;
        execution.last_surface_payload_pixel_count = surface_payload_pixels;
        execution.last_material_surface_pixel_count = material_surface_pixels;
        execution.last_preview_fallback_pixel_count = preview_fallback_pixels;
        execution.last_physical_surface_energy = physical_surface_energy;
        execution.last_preview_fallback_energy = preview_fallback_energy;
        execution.last_surface_payload_confidence = surface_payload_samples == 0
                ? 0.0F
                : surface_payload_confidence_sum / static_cast<float>(surface_payload_samples);
        execution.last_physical_surface_contribution =
                surface_payload_pixels != 0
                && physical_surface_energy > 0.0F
                && execution.last_surface_payload_confidence > 0.0F;
        execution.last_preview_fallback_contribution = preview_fallback_pixels != 0
                || preview_fallback_energy > 0.0F;
        execution.last_focus_window_contribution = false;
        execution.last_cpu_output_generated = true;
        execution.last_metadata_only = !execution.last_physical_surface_contribution;
    } else {
        direct_lighting_cpu_output_.clear();
    }

    std::uint64_t recorded_resources = 0;
    if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
        const auto tracked_output_width = direct_stage.last_width > 0 ? direct_stage.last_width : width_;
        const auto tracked_output_height = direct_stage.last_height > 0 ? direct_stage.last_height : height_;
        if (direct_stage.last_output_count > 0 && tracked_output_width > 0 && tracked_output_height > 0) {
            resources_->track_transient_image(
                    frame_index_,
                    0,
                    tracked_output_width,
                    tracked_output_height,
                    kDirectLightingOutputFormatTag,
                    "render:direct-light-output-write");
            execution.output_writes++;
            execution.last_output_write_recorded = true;
            recorded_resources++;
        }

        resources_->track_transient_buffer(
                frame_index_,
                0,
                kLightingConstantsBytes,
                "render:direct-light-resolve-marker");
        execution.resolves++;
        execution.last_resolve_recorded = true;
        recorded_resources++;
    }

    if (execution.last_cpu_output_generated) {
        if (execution.last_physical_surface_contribution) {
            execution.last_output_marker = execution.last_output_write_recorded
                ? "direct_light_physical_surface_payload_output_write_resolve_recorded"
                : "direct_light_physical_surface_payload_cpu_output_without_resource_write";
        } else {
            execution.last_output_marker = execution.last_output_write_recorded
                ? "direct_light_preview_fallback_output_write_resolve_recorded_metadata_only"
                : "direct_light_preview_fallback_cpu_output_without_resource_write_metadata_only";
        }
    } else {
        execution.last_output_marker = execution.last_output_write_recorded
            ? "direct_light_output_write_resolve_recorded_no_emissive_candidate_output"
            : "direct_light_resolve_recorded_without_output";
    }
    if (execution.last_cpu_output_generated) {
        execution.last_readiness_reason = execution.last_physical_surface_contribution
            ? "direct_lighting_scene_surface_payload_cpu_output_generated"
            : "direct_lighting_cpu_output_generated_preview_fallback_only";
    } else {
        execution.last_readiness_reason = direct_stage.last_placeholder
            ? "direct_lighting_validated_placeholder_scaffold_executed_metadata_only"
            : "direct_lighting_scaffold_executed_metadata_only";
    }
    return recorded_resources;
}

std::uint64_t Renderer::track_round6_dispatch_execution_scaffold(
        NativeLightingDispatchStage dispatch_stage,
        NativeRound6DispatchExecutionTelemetry& execution,
        const char* accepted_marker) {
    auto& stage = lighting_stage_telemetry(dispatch_stage);

    execution.stage = dispatch_stage;
    execution.last_frame_index = frame_index_;
    execution.last_packet_generation = staging_.lighting.last_packet_generation;
    execution.last_dispatch_generation = stage.last_generation;
    execution.last_width = non_negative_u64(stage.last_width);
    execution.last_height = non_negative_u64(stage.last_height);
    execution.last_dispatch_x = non_negative_u64(stage.last_dispatch_x);
    execution.last_dispatch_y = non_negative_u64(stage.last_dispatch_y);
    execution.last_dispatch_z = non_negative_u64(stage.last_dispatch_z);
    execution.last_workgroup_size_x = non_negative_u64(stage.last_workgroup_size_x);
    execution.last_workgroup_size_y = non_negative_u64(stage.last_workgroup_size_y);
    execution.last_workgroup_size_z = non_negative_u64(stage.last_workgroup_size_z);
    execution.last_input_count = non_negative_u64(stage.last_input_count);
    execution.last_output_count = non_negative_u64(stage.last_output_count);
    execution.last_sample_count = non_negative_u64(stage.last_sample_count);
    execution.last_ray_count = non_negative_u64(stage.last_ray_count);
    execution.last_cache_read_count = non_negative_u64(stage.last_cache_read_count);
    execution.last_cache_write_count = non_negative_u64(stage.last_cache_write_count);
    execution.last_flags = stage.last_flags;
    execution.last_enabled = stage.enabled_this_packet;
    execution.last_validated = stage.last_validated;
    execution.last_placeholder = stage.last_placeholder;
    execution.last_temporal_history = stage.last_temporal_history;
    execution.last_reuse_only = stage.last_reuse_only;
    execution.last_debug_overlay = stage.last_debug_overlay;
    execution.last_ready = stage.ready_for_native_execution_this_packet;
    execution.last_accepted = false;
    execution.last_resource_marker_recorded = false;
    execution.last_metadata_dispatch_recorded = false;
    execution.last_cache_read_metadata_dispatch_recorded = false;
    execution.last_cache_write_metadata_dispatch_recorded = false;
    execution.last_placeholder_output_population_recorded = false;
    execution.last_cache_write_marker_recorded = false;
    execution.last_placeholder_output_population_count = 0;
    execution.last_visible_signal_population_count = 0;
    execution.last_visible_signal_sampled_pixels = 0;
    execution.last_visible_signal_nonzero_pixels = 0;
    execution.last_visible_signal_checksum = 0;
    execution.last_cpu_output_width = 0;
    execution.last_cpu_output_height = 0;
    execution.last_cpu_output_pixel_count = 0;
    execution.last_cpu_output_surface_pixel_count = 0;
    execution.last_cpu_output_scene_driven_pixel_count = 0;
    execution.last_cpu_output_emissive_driven_pixel_count = 0;
    execution.last_cpu_output_spatial_lobe_pixel_count = 0;
    execution.last_cpu_output_cache_modulated_pixel_count = 0;
    execution.last_cpu_output_material_modulated_pixel_count = 0;
    execution.last_scene_linked_sample_count = 0;
    execution.last_material_color_modulated_sample_count = 0;
    execution.last_surface_normal_confident_sample_count = 0;
    execution.last_occlusion_dirty_modulated_sample_count = 0;
    execution.last_physical_gi_sample_count = 0;
    execution.last_physical_gi_hit_sample_count = 0;
    execution.last_surface_material_hit_coupled_sample_count = 0;
    execution.last_geometry_hit_coupled_sample_count = 0;
    execution.last_cpu_output_checksum = 0;
    execution.last_physical_output_checksum = 0;
    execution.last_scene_payload_generation = 0;
    execution.last_scene_celestial_generation = 0;
    execution.last_scene_emissive_generation = 0;
    execution.last_scene_shadow_generation = 0;
    execution.last_scene_shadow_candidate_generation = 0;
    execution.last_scene_section_snapshot_generation = 0;
    execution.last_scene_celestial_light_count = 0;
    execution.last_scene_emissive_light_count = 0;
    execution.last_scene_shadow_candidate_count = 0;
    execution.last_scene_budgeted_shadow_candidate_count = 0;
    execution.last_scene_section_snapshot_count = 0;
    execution.last_visible_signal_energy = 0.0F;
    execution.last_visible_signal_min_sample = 0.0F;
    execution.last_visible_signal_max_sample = 0.0F;
    execution.last_cpu_output_energy = 0.0F;
    execution.last_scene_celestial_light_energy = 0.0F;
    execution.last_scene_emissive_light_energy = 0.0F;
    execution.last_cpu_output_cache_response = 0.0F;
    execution.last_cpu_output_material_response = 0.0F;
    execution.last_scene_linked_energy = 0.0F;
    execution.last_material_color_influence = 0.0F;
    execution.last_surface_normal_confidence = 0.0F;
    execution.last_surface_material_hit_coupling = 0.0F;
    execution.last_geometry_hit_coupling = 0.0F;
    execution.last_emissive_contribution_energy = 0.0F;
    execution.last_sun_contribution_energy = 0.0F;
    execution.last_occlusion_dirty_influence = 0.0F;
    execution.last_output_write_energy = 0.0F;
    execution.last_visible_signal_cache_factor = 0.0F;
    execution.last_visible_signal_ray_factor = 0.0F;
    execution.last_physical_scene_link_score = 0;
    execution.last_visible_signal_generated = false;
    execution.last_visible_signal_cache_backed = false;
    execution.last_cpu_output_generated = false;
    execution.last_cpu_output_energy_nonzero = false;
    execution.last_cpu_output_checksum_nonzero = false;
    execution.last_cpu_output_nonzero = false;
    execution.last_cpu_output_marker_recorded = false;
    execution.last_cpu_output_scene_driven = false;
    execution.last_cpu_output_emissive_driven = false;
    execution.last_cpu_output_spatially_graded = false;
    execution.last_cpu_output_material_driven = false;
    execution.last_scene_linked_samples_recorded = false;
    execution.last_material_color_influence_recorded = false;
    execution.last_surface_normal_confidence_recorded = false;
    execution.last_physical_gi_samples_recorded = false;
    execution.last_surface_material_hit_coupling_recorded = false;
    execution.last_geometry_hit_coupling_recorded = false;
    execution.last_occlusion_dirty_influence_recorded = false;
    execution.last_output_write_energy_recorded = false;
    execution.last_scene_inputs_recorded = false;
    execution.last_physical_scene_linked = false;
    execution.last_physical_surface_contribution = false;
    execution.last_preview_fallback_contribution = false;
    execution.last_focus_window_contribution = false;
    execution.last_metadata_only_proof_rejected = true;
    execution.last_focus_window_capture_rejected = true;
    execution.last_proof_marker_evidence_rejected = true;
    execution.last_temporary_direct_substitution_rejected = true;
    execution.last_rectangular_washout_rejected = true;
    execution.last_marker.clear();
    execution.last_output_marker.clear();
    execution.last_cpu_output_marker.clear();
    execution.last_cache_marker.clear();
    execution.last_physical_scene_marker.clear();
    execution.last_physical_output_marker.clear();
    execution.last_physical_sample_marker.clear();
    execution.last_surface_material_hit_marker.clear();
    execution.last_proof_boundary_marker =
            std::string(to_string(dispatch_stage)) + "_requires_native_scene_linked_output_not_capture_artifact";
    execution.last_scene_dimension_id.clear();
    if (dispatch_stage == NativeLightingDispatchStage::DiffuseGi) {
        diffuse_gi_cpu_output_.clear();
        denoised_diffuse_gi_cpu_output_rgba8_.clear();
    }

    if (!stage.enabled_this_packet) {
        execution.last_readiness_reason = stage.last_readiness_reason.empty()
            ? std::string(to_string(dispatch_stage)) + "_stage_disabled"
            : stage.last_readiness_reason;
        return 0;
    }

    execution.attempts++;
    if (!stage.ready_for_native_execution_this_packet) {
        execution.skipped++;
        execution.last_readiness_reason = stage.last_readiness_reason.empty()
            ? std::string(to_string(dispatch_stage)) + "_stage_not_ready"
            : stage.last_readiness_reason;
        return 0;
    }

    execution.submitted++;
    execution.accepted++;
    execution.last_accepted = true;
    execution.last_marker = accepted_marker == nullptr ? "" : accepted_marker;
    execution.metadata_dispatches++;
    execution.total_sample_count = saturated_add(execution.total_sample_count, execution.last_sample_count);
    execution.total_ray_count = saturated_add(execution.total_ray_count, execution.last_ray_count);
    execution.total_cache_read_count = saturated_add(
            execution.total_cache_read_count,
            execution.last_cache_read_count);
    execution.total_cache_write_count = saturated_add(
            execution.total_cache_write_count,
            execution.last_cache_write_count);

    std::uint64_t recorded_resources = 0;
    if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
        const auto marker_label = std::string("render:") + to_string(dispatch_stage) + "-metadata-dispatch";
        resources_->track_transient_buffer(frame_index_, 0, kLightingConstantsBytes, marker_label);
        execution.resource_markers++;
        execution.last_metadata_dispatch_recorded = true;
        execution.last_resource_marker_recorded = true;
        recorded_resources++;
    }

    if (dispatch_stage == NativeLightingDispatchStage::DiffuseGi) {
        execution.last_scene_payload_generation = last_direct_lighting_payload_packet_.generation;
        execution.last_scene_celestial_generation = last_direct_lighting_payload_packet_.celestial_generation;
        execution.last_scene_emissive_generation = last_direct_lighting_payload_packet_.emissive_generation;
        execution.last_scene_shadow_generation = last_direct_lighting_payload_packet_.shadow_generation;
        execution.last_scene_shadow_candidate_generation =
                last_direct_lighting_payload_packet_.shadow_candidate_generation;
        execution.last_scene_section_snapshot_generation =
                last_direct_lighting_payload_packet_.section_snapshot_generation;
        execution.last_scene_dimension_id = last_direct_lighting_payload_packet_.dimension_id;
        execution.last_scene_celestial_light_count = non_negative_u64(
                last_direct_lighting_payload_packet_.celestial_light_count);
        execution.last_scene_emissive_light_count = non_negative_u64(
                last_direct_lighting_payload_packet_.selected_emissive_count);
        execution.last_scene_shadow_candidate_count = non_negative_u64(
                last_direct_lighting_payload_packet_.shadow_candidate_count);
        execution.last_scene_budgeted_shadow_candidate_count = non_negative_u64(
                last_direct_lighting_payload_packet_.budgeted_shadow_candidate_count);
        execution.last_scene_section_snapshot_count = non_negative_u64(
                last_direct_lighting_payload_packet_.section_snapshot_count);
        execution.last_scene_celestial_light_energy =
                finite_non_negative(last_direct_lighting_payload_packet_.celestial_light_energy);
        execution.last_scene_emissive_light_energy =
                finite_non_negative(last_direct_lighting_payload_packet_.selected_emissive_energy);
        execution.last_scene_inputs_recorded =
                execution.last_scene_payload_generation != 0
                || execution.last_scene_celestial_generation != 0
                || execution.last_scene_emissive_generation != 0
                || execution.last_scene_shadow_generation != 0
                || execution.last_scene_shadow_candidate_generation != 0
                || execution.last_scene_section_snapshot_generation != 0
                || execution.last_scene_celestial_light_count != 0
                || execution.last_scene_emissive_light_count != 0
                || execution.last_scene_shadow_candidate_count != 0
                || execution.last_scene_section_snapshot_count != 0
                || execution.last_scene_celestial_light_energy > 0.0F
                || execution.last_scene_emissive_light_energy > 0.0F
                || !execution.last_scene_dimension_id.empty();

        if (execution.last_output_count != 0 && execution.last_width != 0 && execution.last_height != 0) {
            const auto pixel_count = saturated_multiply(execution.last_width, execution.last_height);
            execution.last_placeholder_output_population_count = saturated_multiply(
                    pixel_count,
                    execution.last_output_count);
            execution.total_placeholder_output_population_count = saturated_add(
                    execution.total_placeholder_output_population_count,
                    execution.last_placeholder_output_population_count);
            execution.last_output_marker = "diffuse_gi_output_extent_recorded_before_scene_tied_cpu_population";
            if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
                resources_->track_transient_buffer(
                        frame_index_,
                        0,
                        kLightingConstantsBytes,
                        "render:diffuse_gi-output-extent-before-scene-tied-cpu-population");
                execution.resource_markers++;
                execution.last_placeholder_output_population_recorded = true;
                execution.last_resource_marker_recorded = true;
                recorded_resources++;
            }

            const bool cache_backed = execution.last_cache_read_count != 0
                    || execution.last_cache_write_count != 0;
            const bool has_trace_work = execution.last_sample_count != 0
                    || execution.last_ray_count != 0;
            if (has_trace_work && pixel_count != 0) {
                const auto signal_population = saturated_multiply(
                        pixel_count,
                        execution.last_output_count);
                const auto sample_pixels = std::min(pixel_count, kRound6GiVisibleSignalSampleLimit);
                const float population_scale = sample_pixels == 0
                        ? 0.0F
                        : static_cast<float>(signal_population) / static_cast<float>(sample_pixels);
                const float cache_activity = static_cast<float>(std::min<std::uint64_t>(
                        saturated_add(
                                execution.last_cache_read_count,
                                saturated_multiply(execution.last_cache_write_count, 2)),
                        4'194'304ULL));
                const float ray_activity = static_cast<float>(std::min<std::uint64_t>(
                        saturated_add(execution.last_ray_count, execution.last_sample_count),
                        16'777'216ULL));
                const float pixel_denominator = static_cast<float>(std::max<std::uint64_t>(pixel_count, 1));
                const float cache_factor = std::clamp(cache_activity / pixel_denominator, 0.05F, 8.0F);
                const float ray_factor = std::clamp(ray_activity / pixel_denominator, 0.05F, 16.0F);
                const float output_factor = std::clamp(
                        static_cast<float>(execution.last_output_count),
                        1.0F,
                        8.0F);
                const float base_signal = std::clamp(
                        ((cache_factor * 0.55F) + (ray_factor * 0.35F)) * output_factor,
                        0.01F,
                        64.0F);

                float sampled_energy = 0.0F;
                float min_sample = 0.0F;
                float max_sample = 0.0F;
                bool has_sample = false;
                std::uint64_t checksum = 1469598103934665603ULL;
                for (std::uint64_t sample = 0; sample < sample_pixels; sample++) {
                    const auto pixel_x = sample % execution.last_width;
                    const auto pixel_y = sample / execution.last_width;
                    const float tile = 0.82F
                            + static_cast<float>((pixel_x + (pixel_y * 3) + frame_index_) % 11) * 0.025F;
                    const float cache_pulse = 1.0F
                            + static_cast<float>((sample + execution.last_cache_write_count) % 5) * 0.015F;
                    const float sample_signal = std::min(64.0F, base_signal * tile * cache_pulse);
                    sampled_energy += sample_signal;
                    min_sample = has_sample ? std::min(min_sample, sample_signal) : sample_signal;
                    max_sample = std::max(max_sample, sample_signal);
                    has_sample = true;
                    mix_checksum(checksum, static_cast<std::uint64_t>(sample_signal * 1000.0F));
                    mix_checksum(checksum, sample);
                    mix_checksum(checksum, execution.last_cache_read_count);
                    mix_checksum(checksum, execution.last_cache_write_count);
                }

                execution.last_visible_signal_population_count = signal_population;
                execution.total_visible_signal_population_count = saturated_add(
                        execution.total_visible_signal_population_count,
                        signal_population);
                execution.last_visible_signal_sampled_pixels = sample_pixels;
                execution.last_visible_signal_nonzero_pixels = signal_population;
                execution.total_visible_signal_nonzero_pixels = saturated_add(
                        execution.total_visible_signal_nonzero_pixels,
                        signal_population);
                execution.last_visible_signal_energy = sampled_energy * population_scale;
                execution.last_visible_signal_min_sample = min_sample;
                execution.last_visible_signal_max_sample = max_sample;
                execution.last_visible_signal_cache_factor = cache_factor;
                execution.last_visible_signal_ray_factor = ray_factor;
                execution.last_visible_signal_checksum = checksum;
                execution.last_visible_signal_generated = true;
                execution.last_visible_signal_cache_backed = cache_backed;
                execution.last_output_marker = cache_backed
                        ? "diffuse_gi_scene_tied_cache_trace_signal_recorded"
                        : "diffuse_gi_scene_tied_trace_signal_recorded";

                const auto preview_width = std::min<std::uint64_t>(
                        execution.last_width,
                        static_cast<std::uint64_t>(kMaxDiffuseGiCpuOutputWidth));
                const auto preview_height = std::min<std::uint64_t>(
                        execution.last_height,
                        static_cast<std::uint64_t>(kMaxDiffuseGiCpuOutputHeight));
                const auto preview_pixel_count = saturated_multiply(preview_width, preview_height);
                if (preview_width != 0 && preview_height != 0 && preview_pixel_count != 0) {
                    diffuse_gi_cpu_output_.assign(
                            static_cast<std::size_t>(preview_pixel_count) * 4,
                            0.0F);
                    float preview_energy = 0.0F;
                    std::uint64_t preview_checksum = 1469598103934665603ULL;
                    const float inverse_width = 1.0F / static_cast<float>(
                            std::max<std::uint64_t>(preview_width - 1, 1));
                    const float inverse_height = 1.0F / static_cast<float>(
                            std::max<std::uint64_t>(preview_height - 1, 1));
                    const auto emissive_count = static_cast<std::size_t>(std::min<std::uint64_t>(
                            non_negative_u64(last_direct_lighting_payload_packet_.selected_emissive_count),
                            12));
                    const auto celestial_count = static_cast<std::size_t>(non_negative_u64(
                            last_direct_lighting_payload_packet_.celestial_light_count));
                    const auto shadow_count = static_cast<std::size_t>(std::min<std::uint64_t>(
                            non_negative_u64(last_direct_lighting_payload_packet_.shadow_candidate_count),
                            32));
                    const auto section_count = static_cast<std::size_t>(std::min<std::uint64_t>(
                            non_negative_u64(last_direct_lighting_payload_packet_.section_snapshot_count),
                            32));
                    std::uint64_t scene_seed = 1469598103934665603ULL;
                    mix_checksum(scene_seed, last_direct_lighting_payload_packet_.generation);
                    mix_checksum(scene_seed, last_direct_lighting_payload_packet_.celestial_generation);
                    mix_checksum(scene_seed, last_direct_lighting_payload_packet_.emissive_generation);
                    mix_checksum(scene_seed, last_direct_lighting_payload_packet_.shadow_candidate_generation);
                    mix_checksum(scene_seed, last_direct_lighting_payload_packet_.section_snapshot_generation);
                    mix_checksum(scene_seed, last_lighting_dispatch_packet_.world_generation);
                    mix_checksum(scene_seed, last_lighting_dispatch_packet_.material_generation);
                    mix_checksum(scene_seed, last_lighting_dispatch_packet_.section_generation);
                    mix_checksum(scene_seed, last_section_upload_packet_.section_dirty_region_generation);
                    mix_checksum(scene_seed, execution.last_cache_read_count);
                    mix_checksum(scene_seed, execution.last_cache_write_count);
                    const float scene_phase = static_cast<float>(scene_seed % 997ULL) / 996.0F;
                    const float dirty_activity = std::clamp(
                            static_cast<float>(std::min<std::uint64_t>(
                                    saturated_add(
                                            last_section_upload_packet_.section_dirty_region_generation,
                                            staging_.section.payload_dirty_regions),
                                    4096ULL)) / 4096.0F,
                            0.0F,
                            1.0F);
                    const float payload_celestial_energy = std::max(
                            finite_non_negative(last_direct_lighting_payload_packet_.celestial_light_energy),
                            sum_strided_float_field(
                                    last_direct_lighting_payload_packet_.celestial_light_data,
                                    celestial_count,
                                    kDirectCelestialLightDataStride,
                                    8));
                    const float payload_emissive_energy = finite_non_negative(
                            last_direct_lighting_payload_packet_.selected_emissive_energy);
                    const float normalized_signal = execution.last_visible_signal_energy
                            / static_cast<float>(std::max<std::uint64_t>(
                                    execution.last_visible_signal_sampled_pixels,
                                    1));
                    const float cache_tint = std::clamp(
                            execution.last_visible_signal_cache_factor / 8.0F,
                            0.0F,
                            1.0F);
                    const float ray_tint = std::clamp(
                            execution.last_visible_signal_ray_factor / 16.0F,
                            0.0F,
                            1.0F);
                    const float sky_signal = std::clamp(
                            (payload_celestial_energy * 0.020F) + (ray_tint * 5.0F),
                            0.0F,
                            18.0F);
                    const float cache_signal = std::clamp(
                            cache_tint * (8.0F + static_cast<float>(execution.last_cache_write_count != 0) * 8.0F)
                                    * (0.88F + dirty_activity * 0.24F),
                            0.0F,
                            20.0F);
                    const float emissive_signal = std::clamp(
                            (payload_emissive_energy * 0.018F)
                                    + (static_cast<float>(emissive_count) * 2.10F),
                            0.0F,
                            40.0F);
                    float emissive_red = 0.0F;
                    float emissive_green = 0.0F;
                    float emissive_blue = 0.0F;
                    float emissive_weight = 0.0F;
                    for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
                        const float intensity = std::max(
                                1.0F,
                                strided_float_or_zero(
                                        last_direct_lighting_payload_packet_.emissive_light_data,
                                        light_index,
                                        kDirectEmissiveLightDataStride,
                                        kDirectEmissiveIntensityOffset));
                        const float radius_weight = std::clamp(
                                strided_float_or_zero(
                                        last_direct_lighting_payload_packet_.emissive_light_data,
                                        light_index,
                                        kDirectEmissiveLightDataStride,
                                        kDirectEmissiveInfluenceRadiusOffset) / 24.0F,
                                0.50F,
                                3.0F);
                        const float weight = intensity * radius_weight;
                        emissive_red += strided_float_or_zero(
                                last_direct_lighting_payload_packet_.emissive_light_data,
                                light_index,
                                kDirectEmissiveLightDataStride,
                                kDirectEmissiveColorRedOffset) * weight;
                        emissive_green += strided_float_or_zero(
                                last_direct_lighting_payload_packet_.emissive_light_data,
                                light_index,
                                kDirectEmissiveLightDataStride,
                                kDirectEmissiveColorGreenOffset) * weight;
                        emissive_blue += strided_float_or_zero(
                                last_direct_lighting_payload_packet_.emissive_light_data,
                                light_index,
                                kDirectEmissiveLightDataStride,
                                kDirectEmissiveColorBlueOffset) * weight;
                        emissive_weight += weight;
                    }
                    if (emissive_weight > 0.0F) {
                        emissive_red /= emissive_weight;
                        emissive_green /= emissive_weight;
                        emissive_blue /= emissive_weight;
                    } else {
                        emissive_red = 1.0F;
                        emissive_green = 0.92F;
                        emissive_blue = 0.78F;
                    }
                    NativeGiSceneBounds scene_bounds;
                    std::uint64_t total_section_occupied_voxels = 0;
                    std::uint64_t total_section_opaque_voxels = 0;
                    std::uint64_t total_section_translucent_voxels = 0;
                    std::uint64_t total_section_fluid_voxels = 0;
                    std::uint64_t total_section_emissive_voxels = 0;
                    std::uint64_t total_section_palette_entries = 0;
                    for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
                        include_native_gi_scene_point(
                                scene_bounds,
                                static_cast<float>(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                                        light_index,
                                        kDirectEmissiveLightMetadataStride,
                                        kDirectEmissiveBlockXOffset)) + 0.5F,
                                static_cast<float>(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                                        light_index,
                                        kDirectEmissiveLightMetadataStride,
                                        kDirectEmissiveBlockYOffset)) + 0.5F,
                                static_cast<float>(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.emissive_light_metadata,
                                        light_index,
                                        kDirectEmissiveLightMetadataStride,
                                        kDirectEmissiveBlockZOffset)) + 0.5F);
                    }
                    const auto candidate_bound_limit = std::min<std::size_t>(shadow_count, 64);
                    for (std::size_t candidate_index = 0; candidate_index < candidate_bound_limit; candidate_index++) {
                        include_native_gi_scene_point(
                                scene_bounds,
                                strided_float_raw_or_zero(
                                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                        candidate_index,
                                        kDirectShadowCandidateRayStride,
                                        kDirectShadowRayOriginXOffset),
                                strided_float_raw_or_zero(
                                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                        candidate_index,
                                        kDirectShadowCandidateRayStride,
                                        kDirectShadowRayOriginYOffset),
                                strided_float_raw_or_zero(
                                        last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                        candidate_index,
                                        kDirectShadowCandidateRayStride,
                                        kDirectShadowRayOriginZOffset));
                    }
                    for (std::size_t section_index = 0; section_index < section_count; section_index++) {
                        const auto section_x = strided_int_or_zero(
                                last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                section_index,
                                kDirectSectionSnapshotMetadataStride,
                                kDirectSectionXOffset);
                        const auto section_y = strided_int_or_zero(
                                last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                section_index,
                                kDirectSectionSnapshotMetadataStride,
                                kDirectSectionYOffset);
                        const auto section_z = strided_int_or_zero(
                                last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                section_index,
                                kDirectSectionSnapshotMetadataStride,
                                kDirectSectionZOffset);
                        include_native_gi_scene_point(
                                scene_bounds,
                                static_cast<float>(section_x) * 16.0F + 8.0F,
                                static_cast<float>(section_y) * 16.0F + 8.0F,
                                static_cast<float>(section_z) * 16.0F + 8.0F);
                        total_section_occupied_voxels = saturated_add(
                                total_section_occupied_voxels,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionOccupiedVoxelCountOffset)));
                        total_section_opaque_voxels = saturated_add(
                                total_section_opaque_voxels,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionOpaqueVoxelCountOffset)));
                        total_section_translucent_voxels = saturated_add(
                                total_section_translucent_voxels,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionTranslucentVoxelCountOffset)));
                        total_section_fluid_voxels = saturated_add(
                                total_section_fluid_voxels,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionFluidVoxelCountOffset)));
                        total_section_emissive_voxels = saturated_add(
                                total_section_emissive_voxels,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionEmissiveVoxelCountOffset)));
                        total_section_palette_entries = saturated_add(
                                total_section_palette_entries,
                                non_negative_u64(strided_int_or_zero(
                                        last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                        section_index,
                                        kDirectSectionSnapshotMetadataStride,
                                        kDirectSectionMaterialPaletteSizeOffset)));
                    }
                    const float section_denominator = static_cast<float>(
                            std::max<std::uint64_t>(total_section_occupied_voxels, 1));
                    const float opaque_ratio = std::clamp(
                            static_cast<float>(total_section_opaque_voxels) / section_denominator,
                            0.0F,
                            1.0F);
                    const float translucent_ratio = std::clamp(
                            static_cast<float>(total_section_translucent_voxels + total_section_fluid_voxels)
                                    / section_denominator,
                            0.0F,
                            1.0F);
                    const float emissive_voxel_ratio = std::clamp(
                            static_cast<float>(total_section_emissive_voxels) / section_denominator,
                            0.0F,
                            1.0F);
                    const float palette_diversity = std::clamp(
                            static_cast<float>(total_section_palette_entries)
                                    / static_cast<float>(std::max<std::uint64_t>(
                                            static_cast<std::uint64_t>(section_count) * 16ULL,
                                            1ULL)),
                            0.0F,
                            1.0F);
                    const float material_response = std::clamp(
                            (opaque_ratio * 0.72F)
                                    + (translucent_ratio * 0.16F)
                                    + (emissive_voxel_ratio * 0.42F)
                                    + (palette_diversity * 0.18F),
                            0.08F,
                            1.0F);
                    const float cache_response = std::clamp(
                            (static_cast<float>(execution.last_cache_read_count)
                                    + static_cast<float>(execution.last_cache_write_count) * 2.0F)
                                    / static_cast<float>(std::max<std::uint64_t>(preview_pixel_count, 1)),
                            0.0F,
                            6.0F);
                    const float preview_base = std::clamp(
                            (normalized_signal * 0.12F)
                                    + (execution.last_visible_signal_max_sample * 0.18F)
                                    + (base_signal * 0.24F)
                                    + (sky_signal * 0.34F)
                                    + (cache_signal * 0.34F)
                                    + (emissive_signal * 0.52F)
                                    + (dirty_activity * 4.0F),
                            0.01F,
                            92.0F);
                    const bool scene_driven_output = execution.last_scene_inputs_recorded
                            && (execution.last_ray_count != 0
                                    || execution.last_sample_count != 0
                                    || execution.last_cache_read_count != 0
                                    || execution.last_cache_write_count != 0);
                    const bool emissive_driven_output = emissive_count != 0
                            || emissive_signal > 0.0F
                            || execution.last_scene_emissive_light_energy > 0.0F;
                    std::uint64_t surface_pixels_written = 0;
                    std::uint64_t scene_driven_pixels = 0;
                    std::uint64_t emissive_driven_pixels = 0;
                    std::uint64_t spatial_lobe_pixels = 0;
                    std::uint64_t cache_modulated_pixels = 0;
                    std::uint64_t material_modulated_pixels = 0;
                    std::uint64_t scene_linked_samples = 0;
                    std::uint64_t material_color_samples = 0;
                    std::uint64_t surface_normal_samples = 0;
                    std::uint64_t occlusion_dirty_samples = 0;
                    std::uint64_t physical_gi_samples = 0;
                    std::uint64_t physical_gi_hit_samples = 0;
                    std::uint64_t surface_material_hit_coupled_samples = 0;
                    std::uint64_t geometry_hit_coupled_samples = 0;
                    float scene_linked_energy = 0.0F;
                    float material_color_influence_sum = 0.0F;
                    float surface_normal_confidence_sum = 0.0F;
                    float surface_material_hit_coupling_sum = 0.0F;
                    float geometry_hit_coupling_sum = 0.0F;
                    float emissive_contribution_energy = 0.0F;
                    float sun_contribution_energy = 0.0F;
                    float occlusion_dirty_influence_sum = 0.0F;
                    float output_write_energy = 0.0F;
                    std::uint64_t physical_output_checksum = 1469598103934665603ULL;
                    execution.last_cpu_output_cache_response = cache_response;
                    execution.last_cpu_output_material_response = material_response;
                    for (std::uint64_t pixel = 0; pixel < preview_pixel_count; pixel++) {
                        const auto offset = static_cast<std::size_t>(pixel * 4);
                        const auto pixel_x = pixel % preview_width;
                        const auto pixel_y = pixel / preview_width;
                        const float u = static_cast<float>(pixel_x) * inverse_width;
                        const float v = static_cast<float>(pixel_y) * inverse_height;
                        const float cache_cell = static_cast<float>(
                                ((pixel_x / 24) + ((pixel_y / 18) * 3) + (scene_seed % 17ULL)) % 9)
                                / 8.0F;
                        const float low_frequency_noise = 0.92F
                                + static_cast<float>(
                                        (pixel_x + (pixel_y * 7) + (scene_seed % 13ULL)) % 13)
                                        * 0.012F
                                + (scene_phase * 0.018F);
                        const float sky_gradient = std::max(0.0F, 1.0F - v);
                        const float view_surface_response = broad_surface_response(u, v);
                        const float cache_gradient = (0.55F + (cache_tint * 0.35F)) * (0.75F + cache_cell * 0.25F);
                        const float cache_band = 0.72F
                                + static_cast<float>(
                                        ((pixel_x / 11) ^ (pixel_y / 7) ^ execution.last_cache_read_count) % 17)
                                        * 0.018F;
                        const float section_band = section_count == 0
                                ? 0.0F
                                : static_cast<float>(
                                        ((pixel_x / 19) + (pixel_y / 13) + section_count + (scene_seed % 5ULL)) % 5)
                                        * 0.08F;
                        float surface_projection = section_count == 0 ? 0.18F : 0.34F + section_band;
                        const auto section_limit = std::min<std::size_t>(section_count, 16);
                        for (std::size_t section_index = 0; section_index < section_limit; section_index++) {
                            const auto section_x = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    0);
                            const auto section_y = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    1);
                            const auto section_z = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    2);
                            const auto section_u_hash = (static_cast<std::int64_t>(section_x) * 17)
                                    + (static_cast<std::int64_t>(section_z) * 31);
                            const auto section_v_hash = (static_cast<std::int64_t>(section_y) * 13)
                                    + (static_cast<std::int64_t>(section_z) * 7);
                            const float section_u = static_cast<float>(std::llabs(section_u_hash) % 127) / 126.0F;
                            const float section_v = 1.0F
                                    - (static_cast<float>(std::llabs(section_v_hash) % 89) / 88.0F);
                            const float delta_u = u - section_u;
                            const float delta_v = v - section_v;
                            const float distance = std::sqrt(delta_u * delta_u + delta_v * delta_v);
                            const float falloff = std::max(0.0F, 1.0F - (distance / 0.46F));
                            surface_projection += falloff * falloff
                                    * (0.08F + dirty_activity * 0.12F + cache_tint * 0.10F);
                        }
                        const auto candidate_limit = std::min<std::size_t>(shadow_count, 24);
                        float candidate_surface_signal = 0.0F;
                        for (std::size_t candidate_index = 0; candidate_index < candidate_limit; candidate_index++) {
                            const float origin_x = strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                    candidate_index,
                                    kDirectShadowCandidateRayStride,
                                    kDirectShadowRayOriginXOffset);
                            const float origin_y = strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                    candidate_index,
                                    kDirectShadowCandidateRayStride,
                                    kDirectShadowRayOriginYOffset);
                            const float origin_z = strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                    candidate_index,
                                    kDirectShadowCandidateRayStride,
                                    kDirectShadowRayOriginZOffset);
                            const float candidate_u = static_cast<float>(
                                    std::abs(static_cast<std::int32_t>(origin_x + origin_z)) % 113) / 112.0F;
                            const float candidate_v = 1.0F - (static_cast<float>(
                                    std::abs(static_cast<std::int32_t>(origin_y)) % 79) / 78.0F);
                            const float delta_u = u - candidate_u;
                            const float delta_v = v - candidate_v;
                            const float distance = std::sqrt(delta_u * delta_u + delta_v * delta_v);
                            const float weight = finite_non_negative(strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.shadow_candidate_rays,
                                    candidate_index,
                                    kDirectShadowCandidateRayStride,
                                    kDirectShadowRayContributionWeightOffset));
                            const float radius = 0.18F + std::clamp(weight * 0.04F, 0.0F, 0.22F)
                                    + dirty_activity * 0.04F;
                            const float falloff = std::max(0.0F, 1.0F - distance / radius);
                            const float candidate_bounce = falloff * falloff * (0.26F + ray_tint * 0.36F);
                            candidate_surface_signal += candidate_bounce;
                            surface_projection += candidate_bounce;
                        }
                        surface_projection += view_surface_response
                                * (0.42F + (cache_tint * 0.28F) + (ray_tint * 0.26F)
                                        + (emissive_count == 0 ? 0.0F : 0.24F));
                        surface_projection = std::clamp(surface_projection, 0.0F, 1.85F);
                        const float lower_wall_surface = smooth_unit_response(
                                1.0F - (std::abs(v - 0.62F) / 0.34F));
                        const float ground_surface = smooth_unit_response(
                                1.0F - (std::abs(v - 0.78F) / 0.24F));
                        const float side_surface_balance = smooth_unit_response(
                                1.0F - (std::abs(u - 0.56F) / 0.58F));
                        const float world_surface_mask = std::clamp(
                                std::max(view_surface_response, lower_wall_surface * 0.94F)
                                        + (ground_surface * 0.46F)
                                        + (side_surface_balance * 0.12F)
                                        + std::clamp(candidate_surface_signal, 0.0F, 0.42F),
                                0.0F,
                                1.0F);
                        const float broad_projection_signal = std::clamp(
                                (preview_base * (0.62F + surface_projection * 0.52F))
                                        + (emissive_signal * (0.58F + surface_projection * 0.42F))
                                        + (cache_signal * (0.32F + cache_gradient * 0.34F))
                                        + (sky_signal * (0.18F + sky_gradient * 0.26F))
                                        + (dirty_activity * 5.0F * surface_projection),
                                4.0F,
                                96.0F);
                        const float scene_surface_signal = std::clamp(
                                (preview_base * 0.38F)
                                        + (emissive_signal * (0.72F + world_surface_mask * 0.70F))
                                        + (cache_signal * (0.42F + cache_tint * 0.35F))
                                        + (ray_tint * 26.0F)
                                        + (candidate_surface_signal * 38.0F)
                                        + (dirty_activity * 6.0F),
                                0.0F,
                                118.0F);
                        const float world_surface_lift = scene_surface_signal
                                * (0.42F + world_surface_mask * 0.92F);
                        float red = (preview_base * (0.34F + cache_gradient * 0.20F))
                                + (sky_signal * (0.15F + sky_gradient * 0.14F))
                                + (broad_projection_signal * (0.36F + emissive_red * 0.52F));
                        float green = (preview_base * (0.39F + ray_tint * 0.20F))
                                + (sky_signal * (0.22F + sky_gradient * 0.20F))
                                + (broad_projection_signal * (0.38F + emissive_green * 0.50F));
                        float blue = (preview_base * (0.26F + sky_gradient * 0.26F))
                                + (sky_signal * (0.35F + sky_gradient * 0.34F))
                                + (broad_projection_signal * (0.22F + emissive_blue * 0.46F));
                        const float scene_surface_lift = std::clamp(
                                view_surface_response
                                        * ((preview_base * 0.34F)
                                                + (broad_projection_signal * 0.54F)
                                                + (emissive_signal * 0.58F)
                                                + (cache_signal * 0.30F)
                                                + (sky_signal * 0.18F)
                                                + (ray_tint * 18.0F)),
                                0.0F,
                                72.0F);
                        red += scene_surface_lift * (0.58F + emissive_red * 0.46F);
                        green += scene_surface_lift * (0.64F + emissive_green * 0.44F);
                        blue += scene_surface_lift * (0.34F + emissive_blue * 0.36F);
                        red += world_surface_lift * (0.66F + emissive_red * 0.56F);
                        green += world_surface_lift * (0.72F + emissive_green * 0.52F);
                        blue += world_surface_lift * (0.44F + emissive_blue * 0.42F);
                        for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
                            const auto block_x = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockXOffset);
                            const auto block_y = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockYOffset);
                            const auto block_z = strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockZOffset);
                            const float light_u = static_cast<float>((std::abs(block_x + block_z) % 97)) / 96.0F;
                            const float light_v = 1.0F - (static_cast<float>(std::abs(block_y) % 65) / 64.0F);
                            const float delta_u = u - light_u;
                            const float delta_v = v - light_v;
                            const float radius = std::clamp(
                                    strided_float_or_zero(
                                            last_direct_lighting_payload_packet_.emissive_light_data,
                                            light_index,
                                            kDirectEmissiveLightDataStride,
                                            kDirectEmissiveInfluenceRadiusOffset) / 32.0F,
                                    0.34F,
                                    0.96F);
                            const float distance = std::sqrt(delta_u * delta_u + delta_v * delta_v);
                            const float falloff = std::max(0.0F, 1.0F - (distance / radius));
                            const float shoulder = std::max(0.0F, 1.0F - (distance / (radius * 2.35F)));
                            const float bounce = ((falloff * falloff) + (shoulder * 0.42F))
                                    * (0.70F + cache_tint * 0.30F)
                                    * (0.72F + world_surface_mask * 0.58F);
                            if (bounce <= 0.0F) {
                                continue;
                            }
                            const float intensity = std::max(
                                    1.0F,
                                    strided_float_or_zero(
                                            last_direct_lighting_payload_packet_.emissive_light_data,
                                            light_index,
                                            kDirectEmissiveLightDataStride,
                                            kDirectEmissiveIntensityOffset));
                            const float strength = std::min(
                                    84.0F,
                                    intensity * bounce * (0.92F + ray_tint * 0.62F + surface_projection * 0.24F));
                            red += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorRedOffset) * strength;
                            green += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorGreenOffset) * strength;
                            blue += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorBlueOffset) * strength;
                        }
                        float physical_emissive_red = 0.0F;
                        float physical_emissive_green = 0.0F;
                        float physical_emissive_blue = 0.0F;
                        float spatial_lobe = 0.0F;
                        for (std::size_t light_index = 0; light_index < emissive_count; light_index++) {
                            const float light_x = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockXOffset)) + 0.5F;
                            const float light_y = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockYOffset)) + 0.5F;
                            const float light_z = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_metadata,
                                    light_index,
                                    kDirectEmissiveLightMetadataStride,
                                    kDirectEmissiveBlockZOffset)) + 0.5F;
                            const float light_u = scene_bounds.initialized
                                    ? project_native_gi_axis(
                                            (light_x + light_z) * 0.5F,
                                            (scene_bounds.min_x + scene_bounds.min_z) * 0.5F,
                                            (scene_bounds.max_x + scene_bounds.max_z) * 0.5F,
                                            0.5F)
                                    : 0.5F;
                            const float light_v = scene_bounds.initialized
                                    ? 1.0F - project_native_gi_axis(
                                            (light_y * 0.70F) + (light_z * 0.30F),
                                            (scene_bounds.min_y * 0.70F) + (scene_bounds.min_z * 0.30F),
                                            (scene_bounds.max_y * 0.70F) + (scene_bounds.max_z * 0.30F),
                                            0.5F)
                                    : 0.5F;
                            const float radius_blocks = strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveInfluenceRadiusOffset);
                            const float radius = std::clamp(radius_blocks / 72.0F, 0.06F, 0.38F);
                            const float lobe = native_gi_lobe(u, v, light_u, light_v, radius)
                                    * (0.70F + material_response * 0.46F);
                            if (lobe <= 0.0F) {
                                continue;
                            }
                            spatial_lobe = std::max(spatial_lobe, lobe);
                            const float intensity = std::max(
                                    0.05F,
                                    strided_float_or_zero(
                                            last_direct_lighting_payload_packet_.emissive_light_data,
                                            light_index,
                                            kDirectEmissiveLightDataStride,
                                            kDirectEmissiveIntensityOffset));
                            const float energy = std::min(96.0F, intensity * lobe * (18.0F + cache_response * 2.5F));
                            physical_emissive_red += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorRedOffset) * energy;
                            physical_emissive_green += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorGreenOffset) * energy;
                            physical_emissive_blue += strided_float_or_zero(
                                    last_direct_lighting_payload_packet_.emissive_light_data,
                                    light_index,
                                    kDirectEmissiveLightDataStride,
                                    kDirectEmissiveColorBlueOffset) * energy;
                        }
                        float section_lobe = 0.0F;
                        for (std::size_t section_index = 0; section_index < section_limit; section_index++) {
                            const float section_center_x = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    kDirectSectionXOffset)) * 16.0F + 8.0F;
                            const float section_center_y = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    kDirectSectionYOffset)) * 16.0F + 8.0F;
                            const float section_center_z = static_cast<float>(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    kDirectSectionZOffset)) * 16.0F + 8.0F;
                            const float section_u = scene_bounds.initialized
                                    ? project_native_gi_axis(
                                            (section_center_x + section_center_z) * 0.5F,
                                            (scene_bounds.min_x + scene_bounds.min_z) * 0.5F,
                                            (scene_bounds.max_x + scene_bounds.max_z) * 0.5F,
                                            0.5F)
                                    : 0.5F;
                            const float section_v = scene_bounds.initialized
                                    ? 1.0F - project_native_gi_axis(
                                            (section_center_y * 0.70F) + (section_center_z * 0.30F),
                                            (scene_bounds.min_y * 0.70F) + (scene_bounds.min_z * 0.30F),
                                            (scene_bounds.max_y * 0.70F) + (scene_bounds.max_z * 0.30F),
                                            0.5F)
                                    : 0.5F;
                            const float occupied = static_cast<float>(non_negative_u64(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    kDirectSectionOccupiedVoxelCountOffset)));
                            const float opaque = static_cast<float>(non_negative_u64(strided_int_or_zero(
                                    last_direct_lighting_payload_packet_.section_snapshot_metadata,
                                    section_index,
                                    kDirectSectionSnapshotMetadataStride,
                                    kDirectSectionOpaqueVoxelCountOffset)));
                            const float section_material = occupied <= 0.0F
                                    ? material_response
                                    : std::clamp((opaque / occupied) * 0.82F + material_response * 0.18F, 0.0F, 1.0F);
                            section_lobe = std::max(
                                    section_lobe,
                                    native_gi_lobe(u, v, section_u, section_v, 0.22F) * section_material);
                        }
                        const float physical_cache = std::clamp(
                                cache_response * (0.12F + section_lobe * 0.32F + spatial_lobe * 0.26F),
                                0.0F,
                                24.0F);
                        const float physical_sky = sky_signal
                                * (0.26F + sky_gradient * 0.46F)
                                * (0.55F + material_response * 0.45F)
                                * (1.0F - translucent_ratio * 0.22F);
                        const float physical_surface = std::clamp(
                                (section_lobe * (18.0F + cache_response * 2.0F))
                                        + (candidate_surface_signal * 30.0F)
                                        + (physical_cache * 0.74F),
                                0.0F,
                                72.0F);
                        const float occlusion_dirty_sample_influence = std::clamp(
                                (opaque_ratio * 0.58F)
                                        + (dirty_activity * 0.36F)
                                        + (translucent_ratio * 0.10F)
                                        + (section_lobe * 0.24F)
                                        + (candidate_surface_signal * 0.18F),
                                0.0F,
                                1.0F);
                        const float material_hit_coupling = std::clamp(
                                material_response
                                        * (0.28F + world_surface_mask * 0.42F)
                                        * (0.30F + section_lobe * 0.40F + candidate_surface_signal * 0.30F
                                                + spatial_lobe * 0.24F)
                                        * (0.82F + palette_diversity * 0.28F),
                                0.0F,
                                1.0F);
                        const float geometry_hit_coupling = std::clamp(
                                world_surface_mask
                                        * (0.34F + surface_projection * 0.26F)
                                        * (0.30F + section_lobe * 0.34F + candidate_surface_signal * 0.38F
                                                + opaque_ratio * 0.24F)
                                        * (0.72F + ray_tint * 0.28F),
                                0.0F,
                                1.0F);
                        const float physical_hit_response = std::clamp(
                                (material_hit_coupling * 0.56F)
                                        + (geometry_hit_coupling * 0.62F)
                                        + (occlusion_dirty_sample_influence * 0.18F),
                                0.0F,
                                1.35F);
                        const float coupled_bounce = std::clamp(
                                (physical_surface * 0.44F + physical_cache * 1.20F)
                                        * physical_hit_response
                                        * (0.72F + material_response * 0.34F),
                                0.0F,
                                46.0F);
                        red = (red * 0.30F)
                                + physical_emissive_red
                                + physical_surface * (0.48F + emissive_red * 0.42F)
                                + physical_sky * 0.48F
                                + coupled_bounce * (0.50F + emissive_red * 0.28F);
                        green = (green * 0.30F)
                                + physical_emissive_green
                                + physical_surface * (0.52F + emissive_green * 0.42F)
                                + physical_sky * 0.58F
                                + coupled_bounce * (0.56F + emissive_green * 0.26F);
                        blue = (blue * 0.30F)
                                + physical_emissive_blue
                                + physical_surface * (0.34F + emissive_blue * 0.34F)
                                + physical_sky * 0.72F
                                + coupled_bounce * (0.38F + emissive_blue * 0.24F);
                        red = std::min(192.0F, red * low_frequency_noise * cache_band);
                        green = std::min(192.0F, green * low_frequency_noise * cache_band);
                        blue = std::min(192.0F, blue * low_frequency_noise * cache_band);
                        const bool writes_surface_pixel = world_surface_mask >= 0.18F
                                && (red + green + blue) > 8.0F;
                        const bool writes_scene_driven_pixel = writes_surface_pixel && scene_driven_output;
                        const bool writes_emissive_driven_pixel = writes_surface_pixel && emissive_driven_output
                                && (emissive_signal > 0.0F || world_surface_lift > 0.0F);
                        const float physical_emissive_energy = physical_emissive_red
                                + physical_emissive_green
                                + physical_emissive_blue;
                        const float pixel_output_energy = red + green + blue;
                        const bool sample_scene_linked = writes_scene_driven_pixel
                                && (physical_emissive_energy > 0.01F
                                        || physical_sky > 0.01F
                                        || physical_surface > 0.01F
                                        || section_lobe > 0.01F
                                        || spatial_lobe > 0.01F);
                        const bool material_color_sample = sample_scene_linked
                                && material_response > 0.10F
                                && (physical_emissive_energy > 0.01F
                                        || section_lobe > 0.01F
                                        || palette_diversity > 0.01F);
                        const bool surface_normal_sample = writes_surface_pixel
                                && world_surface_mask >= 0.35F
                                && (surface_projection > 0.20F
                                        || candidate_surface_signal > 0.01F
                                        || section_lobe > 0.01F);
                        const bool occlusion_dirty_sample = sample_scene_linked
                                && occlusion_dirty_sample_influence > 0.08F
                                && (total_section_occupied_voxels != 0
                                        || dirty_activity > 0.0F
                                        || execution.last_cache_write_count != 0);
                        const bool physical_gi_sample = writes_surface_pixel
                                && scene_driven_output
                                && (execution.last_ray_count != 0 || execution.last_sample_count != 0)
                                && (physical_surface > 0.05F
                                        || physical_cache > 0.05F
                                        || physical_sky > 0.05F);
                        const bool physical_gi_hit_sample = physical_gi_sample
                                && (candidate_surface_signal > 0.01F || section_lobe > 0.01F)
                                && physical_hit_response > 0.08F;
                        const bool surface_material_hit_sample = physical_gi_hit_sample
                                && material_hit_coupling > 0.06F
                                && material_response > 0.10F;
                        const bool geometry_hit_sample = physical_gi_hit_sample
                                && geometry_hit_coupling > 0.06F
                                && world_surface_mask > 0.18F;
                        if (writes_surface_pixel) {
                            surface_pixels_written++;
                        }
                        if (writes_scene_driven_pixel) {
                            scene_driven_pixels++;
                        }
                        if (writes_emissive_driven_pixel) {
                            emissive_driven_pixels++;
                        }
                        if (spatial_lobe > 0.01F || section_lobe > 0.01F || candidate_surface_signal > 0.01F) {
                            spatial_lobe_pixels++;
                        }
                        if (physical_cache > 0.05F) {
                            cache_modulated_pixels++;
                        }
                        if (material_response > 0.10F && section_lobe > 0.01F) {
                            material_modulated_pixels++;
                        }
                        if (sample_scene_linked) {
                            scene_linked_samples++;
                            scene_linked_energy += pixel_output_energy;
                        }
                        if (material_color_sample) {
                            material_color_samples++;
                            material_color_influence_sum += std::clamp(
                                    material_response
                                            * (0.48F + palette_diversity * 0.24F)
                                            * (0.70F + (physical_emissive_energy > 0.0F ? 0.30F : 0.0F)),
                                    0.0F,
                                    1.0F);
                        }
                        if (surface_normal_sample) {
                            surface_normal_samples++;
                            surface_normal_confidence_sum += std::clamp(
                                    (world_surface_mask * 0.62F)
                                            + (surface_projection * 0.20F)
                                            + (section_lobe * 0.14F)
                                            + (candidate_surface_signal * 0.12F),
                                    0.0F,
                                    1.0F);
                        }
                        if (occlusion_dirty_sample) {
                            occlusion_dirty_samples++;
                            occlusion_dirty_influence_sum += occlusion_dirty_sample_influence;
                        }
                        if (physical_gi_sample) {
                            physical_gi_samples++;
                        }
                        if (physical_gi_hit_sample) {
                            physical_gi_hit_samples++;
                        }
                        if (surface_material_hit_sample) {
                            surface_material_hit_coupled_samples++;
                            surface_material_hit_coupling_sum += material_hit_coupling;
                        }
                        if (geometry_hit_sample) {
                            geometry_hit_coupled_samples++;
                            geometry_hit_coupling_sum += geometry_hit_coupling;
                        }
                        emissive_contribution_energy += physical_emissive_energy;
                        sun_contribution_energy += physical_sky * 3.0F;
                        output_write_energy += pixel_output_energy;
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(pixel_output_energy * 1000.0F));
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(material_response * 1000.0F));
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(world_surface_mask * 1000.0F));
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(occlusion_dirty_sample_influence * 1000.0F));
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(material_hit_coupling * 1000.0F));
                        mix_checksum(
                                physical_output_checksum,
                                static_cast<std::uint64_t>(geometry_hit_coupling * 1000.0F));
                        mix_checksum(physical_output_checksum, sample_scene_linked ? 1ULL : 0ULL);
                        mix_checksum(physical_output_checksum, physical_gi_hit_sample ? 1ULL : 0ULL);
                        const float alpha = std::clamp(
                                0.72F + (surface_projection * 0.10F) + (cache_tint * 0.20F) + (ray_tint * 0.16F)
                                        + (dirty_activity * 0.08F)
                                        + (world_surface_mask * 0.18F)
                                        + (emissive_count == 0 ? 0.0F : 0.20F),
                                0.0F,
                                1.0F);
                        diffuse_gi_cpu_output_[offset] = red;
                        diffuse_gi_cpu_output_[offset + 1] = green;
                        diffuse_gi_cpu_output_[offset + 2] = blue;
                        diffuse_gi_cpu_output_[offset + 3] = alpha;
                        preview_energy += red + green + blue;
                        mix_checksum(preview_checksum, static_cast<std::uint64_t>((red + green + blue) * 1000.0F));
                        mix_checksum(preview_checksum, pixel);
                        mix_checksum(preview_checksum, scene_seed);
                        mix_checksum(preview_checksum, execution.last_visible_signal_checksum);
                    }
                    execution.last_cpu_output_width = preview_width;
                    execution.last_cpu_output_height = preview_height;
                    execution.last_cpu_output_pixel_count = preview_pixel_count;
                    execution.last_cpu_output_surface_pixel_count = surface_pixels_written;
                    execution.last_cpu_output_scene_driven_pixel_count = scene_driven_pixels;
                    execution.last_cpu_output_emissive_driven_pixel_count = emissive_driven_pixels;
                    execution.last_cpu_output_spatial_lobe_pixel_count = spatial_lobe_pixels;
                    execution.last_cpu_output_cache_modulated_pixel_count = cache_modulated_pixels;
                    execution.last_cpu_output_material_modulated_pixel_count = material_modulated_pixels;
                    execution.last_cpu_output_energy = preview_energy;
                    execution.last_cpu_output_checksum = preview_checksum;
                    execution.last_physical_output_checksum = physical_output_checksum;
                    execution.last_scene_linked_sample_count = scene_linked_samples;
                    execution.last_material_color_modulated_sample_count = material_color_samples;
                    execution.last_surface_normal_confident_sample_count = surface_normal_samples;
                    execution.last_occlusion_dirty_modulated_sample_count = occlusion_dirty_samples;
                    execution.last_physical_gi_sample_count = physical_gi_samples;
                    execution.last_physical_gi_hit_sample_count = physical_gi_hit_samples;
                    execution.last_surface_material_hit_coupled_sample_count =
                            surface_material_hit_coupled_samples;
                    execution.last_geometry_hit_coupled_sample_count = geometry_hit_coupled_samples;
                    execution.last_scene_linked_energy = scene_linked_energy;
                    execution.last_material_color_influence = material_color_samples == 0
                            ? 0.0F
                            : material_color_influence_sum / static_cast<float>(material_color_samples);
                    execution.last_surface_normal_confidence = surface_normal_samples == 0
                            ? 0.0F
                            : surface_normal_confidence_sum / static_cast<float>(surface_normal_samples);
                    execution.last_surface_material_hit_coupling =
                            surface_material_hit_coupled_samples == 0
                            ? 0.0F
                            : surface_material_hit_coupling_sum
                                    / static_cast<float>(surface_material_hit_coupled_samples);
                    execution.last_geometry_hit_coupling = geometry_hit_coupled_samples == 0
                            ? 0.0F
                            : geometry_hit_coupling_sum / static_cast<float>(geometry_hit_coupled_samples);
                    execution.last_emissive_contribution_energy = emissive_contribution_energy;
                    execution.last_sun_contribution_energy = sun_contribution_energy;
                    execution.last_occlusion_dirty_influence = occlusion_dirty_samples == 0
                            ? 0.0F
                            : occlusion_dirty_influence_sum / static_cast<float>(occlusion_dirty_samples);
                    execution.last_output_write_energy = output_write_energy;
                    execution.last_cpu_output_generated = true;
                    execution.last_cpu_output_energy_nonzero = preview_energy > 0.0F;
                    execution.last_cpu_output_checksum_nonzero = preview_checksum != 0;
                    execution.last_cpu_output_nonzero =
                            execution.last_cpu_output_energy_nonzero
                            && execution.last_cpu_output_checksum_nonzero;
                    execution.last_cpu_output_marker_recorded = execution.last_cpu_output_nonzero;
                    execution.last_cpu_output_scene_driven = scene_driven_output && scene_driven_pixels != 0;
                    execution.last_cpu_output_emissive_driven =
                            emissive_driven_output && emissive_driven_pixels != 0;
                    execution.last_cpu_output_spatially_graded = spatial_lobe_pixels != 0;
                    execution.last_cpu_output_material_driven = material_modulated_pixels != 0;
                    execution.last_scene_linked_samples_recorded = scene_linked_samples != 0;
                    execution.last_material_color_influence_recorded =
                            material_color_samples != 0 && execution.last_material_color_influence > 0.0F;
                    execution.last_surface_normal_confidence_recorded =
                            surface_normal_samples != 0 && execution.last_surface_normal_confidence > 0.0F;
                    execution.last_physical_gi_samples_recorded =
                            physical_gi_samples != 0 && physical_gi_hit_samples != 0;
                    execution.last_surface_material_hit_coupling_recorded =
                            surface_material_hit_coupled_samples != 0
                            && execution.last_surface_material_hit_coupling > 0.0F;
                    execution.last_geometry_hit_coupling_recorded =
                            geometry_hit_coupled_samples != 0 && execution.last_geometry_hit_coupling > 0.0F;
                    execution.last_occlusion_dirty_influence_recorded =
                            occlusion_dirty_samples != 0 && execution.last_occlusion_dirty_influence > 0.0F;
                    execution.last_output_write_energy_recorded =
                            output_write_energy > 0.0F && physical_output_checksum != 0;
                    execution.last_physical_output_marker = execution.last_output_write_energy_recorded
                            ? "native_diffuse_gi_cpu_preview_physical_output_energy_checksum_recorded"
                            : "native_diffuse_gi_cpu_preview_physical_output_energy_missing";
                    execution.last_physical_sample_marker = execution.last_physical_gi_samples_recorded
                            ? "native_diffuse_gi_cpu_preview_surface_hit_samples_recorded_not_path_traced_gi"
                            : "native_diffuse_gi_cpu_preview_surface_hit_samples_missing_not_physical_gi";
                    execution.last_surface_material_hit_marker =
                            execution.last_surface_material_hit_coupling_recorded
                                    && execution.last_geometry_hit_coupling_recorded
                            ? "native_diffuse_gi_surface_material_geometry_hit_coupling_recorded_cpu_preview_only"
                            : "native_diffuse_gi_surface_material_geometry_hit_coupling_incomplete";
                    execution.last_cpu_output_marker = execution.last_cpu_output_nonzero
                            ? (execution.last_cpu_output_spatially_graded
                                    ? "diffuse_gi_scene_spatial_source_lobes_cpu_output_generated_nonzero"
                                    : (execution.last_cpu_output_emissive_driven
                                            ? "diffuse_gi_scene_emissive_surface_cpu_output_generated_nonzero"
                                            : "diffuse_gi_scene_tied_low_res_cpu_output_generated_nonzero"))
                            : "diffuse_gi_scene_tied_low_res_cpu_output_generated_zero_signal";
                }

                if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
                    resources_->track_transient_image(
                            frame_index_,
                            0,
                            static_cast<std::int32_t>(std::min<std::uint64_t>(
                                    execution.last_width,
                                    static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                            static_cast<std::int32_t>(std::min<std::uint64_t>(
                                    execution.last_height,
                                    static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                            kDiffuseGiVisibleSignalFormatTag,
                            "render:diffuse_gi-visible-signal-output");
                    execution.resource_markers++;
                    execution.last_resource_marker_recorded = true;
                    recorded_resources++;
                }
            } else {
                execution.last_output_marker = cache_backed
                        ? "diffuse_gi_visible_signal_missing_trace_work"
                        : "diffuse_gi_visible_signal_missing_trace_work_and_cache_activity";
            }
        } else {
            execution.last_output_marker = "diffuse_gi_scene_tied_cpu_output_missing_dispatch_extent";
        }
    }

    if (execution.last_cache_read_count != 0) {
        execution.cache_read_metadata_dispatches++;
        execution.last_cache_read_metadata_dispatch_recorded = true;
        if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
            resources_->track_transient_buffer(
                    frame_index_,
                    0,
                    kLightingConstantsBytes,
                    std::string("render:") + to_string(dispatch_stage) + "-cache-read-metadata-dispatch");
            execution.resource_markers++;
            execution.last_resource_marker_recorded = true;
            recorded_resources++;
        }
    }

    if (execution.last_cache_write_count != 0) {
        execution.cache_write_metadata_dispatches++;
        execution.cache_write_markers++;
        execution.last_cache_write_metadata_dispatch_recorded = true;
        execution.last_cache_write_marker_recorded = true;
        execution.last_cache_marker = std::string(to_string(dispatch_stage)) + "_cache_write_marker_recorded";
        if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
            resources_->track_transient_buffer(
                    frame_index_,
                    0,
                    kLightingConstantsBytes,
                    std::string("render:") + to_string(dispatch_stage) + "-cache-write-metadata-dispatch");
            resources_->track_transient_buffer(
                    frame_index_,
                    0,
                    kLightingConstantsBytes,
                    std::string("render:") + to_string(dispatch_stage) + "-cache-write-marker");
            execution.resource_markers += 2;
            execution.last_resource_marker_recorded = true;
            recorded_resources += 2;
        }
    } else if (dispatch_stage == NativeLightingDispatchStage::Cache) {
        execution.last_cache_marker = "lighting_cache_write_marker_absent_zero_writes";
    }

    if (dispatch_stage == NativeLightingDispatchStage::DiffuseGi && execution.last_cpu_output_generated) {
        const bool cache_inputs_recorded = execution.last_cache_read_count != 0
                || execution.last_cache_write_count != 0;
        const bool lighting_inputs_recorded = execution.last_scene_emissive_light_count != 0
                || execution.last_scene_celestial_light_count != 0
                || execution.last_scene_emissive_light_energy > 0.0F
                || execution.last_scene_celestial_light_energy > 0.0F;
        const bool surface_inputs_recorded = execution.last_scene_shadow_candidate_count != 0
                || execution.last_scene_section_snapshot_count != 0;
        const bool surface_output_recorded = execution.last_cpu_output_surface_pixel_count != 0;
        const bool emissive_output_recorded = execution.last_cpu_output_emissive_driven;
        const bool spatial_output_recorded = execution.last_cpu_output_spatially_graded;
        const bool material_output_recorded = execution.last_cpu_output_material_driven;
        const bool native_scene_linked_samples_recorded = execution.last_scene_linked_samples_recorded;
        const bool material_color_influence_recorded = execution.last_material_color_influence_recorded;
        const bool surface_normal_confidence_recorded = execution.last_surface_normal_confidence_recorded;
        const bool physical_gi_samples_recorded = execution.last_physical_gi_samples_recorded;
        const bool surface_material_hit_coupling_recorded =
                execution.last_surface_material_hit_coupling_recorded;
        const bool geometry_hit_coupling_recorded = execution.last_geometry_hit_coupling_recorded;
        const bool occlusion_dirty_influence_recorded = execution.last_occlusion_dirty_influence_recorded;
        const bool output_energy_checksum_recorded = execution.last_output_write_energy_recorded;
        const bool light_energy_contributions_recorded =
                execution.last_emissive_contribution_energy > 0.0F
                || execution.last_sun_contribution_energy > 0.0F;
        execution.last_physical_scene_link_score =
                (execution.last_cpu_output_nonzero ? 1ULL : 0ULL)
                + (execution.last_scene_inputs_recorded ? 1ULL : 0ULL)
                + (cache_inputs_recorded ? 1ULL : 0ULL)
                + (lighting_inputs_recorded ? 1ULL : 0ULL)
                + (surface_inputs_recorded ? 1ULL : 0ULL)
                + (surface_output_recorded ? 1ULL : 0ULL)
                + (emissive_output_recorded ? 1ULL : 0ULL)
                + (spatial_output_recorded ? 1ULL : 0ULL)
                + (material_output_recorded ? 1ULL : 0ULL)
                + (native_scene_linked_samples_recorded ? 1ULL : 0ULL)
                + (material_color_influence_recorded ? 1ULL : 0ULL)
                + (surface_normal_confidence_recorded ? 1ULL : 0ULL)
                + (physical_gi_samples_recorded ? 1ULL : 0ULL)
                + (surface_material_hit_coupling_recorded ? 1ULL : 0ULL)
                + (geometry_hit_coupling_recorded ? 1ULL : 0ULL)
                + (occlusion_dirty_influence_recorded ? 1ULL : 0ULL)
                + (output_energy_checksum_recorded ? 1ULL : 0ULL)
                + (light_energy_contributions_recorded ? 1ULL : 0ULL);
        execution.last_physical_scene_linked = execution.last_physical_scene_link_score >= 15
                && native_scene_linked_samples_recorded
                && material_color_influence_recorded
                && surface_normal_confidence_recorded
                && physical_gi_samples_recorded
                && surface_material_hit_coupling_recorded
                && geometry_hit_coupling_recorded
                && output_energy_checksum_recorded;
        execution.last_physical_surface_contribution = execution.last_physical_scene_linked
                && surface_output_recorded
                && spatial_output_recorded
                && material_output_recorded
                && physical_gi_samples_recorded
                && surface_material_hit_coupling_recorded
                && geometry_hit_coupling_recorded
                && execution.last_output_write_energy > 0.0F;
        execution.last_preview_fallback_contribution =
                execution.last_cpu_output_generated
                && !execution.last_physical_surface_contribution;
        execution.last_focus_window_contribution = false;
        execution.last_metadata_only_proof_rejected =
                !execution.last_physical_scene_linked || !execution.last_cpu_output_nonzero;
        execution.last_focus_window_capture_rejected = execution.last_cpu_output_surface_pixel_count != 0
                && execution.last_cpu_output_surface_pixel_count < execution.last_cpu_output_pixel_count;
        execution.last_proof_marker_evidence_rejected = execution.last_cpu_output_nonzero
                && execution.last_scene_inputs_recorded
                && native_scene_linked_samples_recorded;
        execution.last_temporary_direct_substitution_rejected = execution.last_cpu_output_emissive_driven
                && execution.last_cpu_output_scene_driven
                && (execution.last_cache_read_count != 0 || execution.last_cache_write_count != 0)
                && (surface_normal_confidence_recorded || occlusion_dirty_influence_recorded);
        execution.last_rectangular_washout_rejected =
                execution.last_cpu_output_spatially_graded
                && execution.last_cpu_output_material_driven
                && execution.last_cpu_output_cache_modulated_pixel_count != 0
                && native_scene_linked_samples_recorded
                && physical_gi_samples_recorded
                && output_energy_checksum_recorded;
        execution.last_physical_scene_marker = execution.last_physical_scene_linked
                ? "native_diffuse_gi_scene_material_geometry_hit_coupled_cpu_preview_metrics_present"
                : "native_diffuse_gi_scene_link_incomplete_do_not_treat_as_physical_gi";
        execution.last_proof_boundary_marker =
                execution.last_physical_scene_linked
                ? "native_gi_cpu_preview_hit_coupled_metrics_not_final_physically_correct_gi_or_path_tracing"
                : "native_gi_metadata_or_partial_preview_rejected_for_visual_gi_completion";
        if (execution.last_cpu_output_nonzero
                && execution.last_scene_inputs_recorded
                && cache_inputs_recorded
                && lighting_inputs_recorded
                && surface_inputs_recorded
                && surface_output_recorded
                && emissive_output_recorded
                && spatial_output_recorded
                && material_output_recorded
                && native_scene_linked_samples_recorded
                && material_color_influence_recorded
                && surface_normal_confidence_recorded
                && physical_gi_samples_recorded
                && surface_material_hit_coupling_recorded
                && geometry_hit_coupling_recorded
                && output_energy_checksum_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_cpu_preview_scene_material_geometry_hit_coupled_samples_recorded_not_final_physical_gi";
        } else if (execution.last_cpu_output_nonzero
                && execution.last_scene_inputs_recorded
                && native_scene_linked_samples_recorded
                && physical_gi_samples_recorded
                && output_energy_checksum_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_linked_cpu_output_generated_nonzero_hit_samples_partial_boundary";
        } else if (execution.last_cpu_output_nonzero
                && execution.last_scene_inputs_recorded
                && spatial_output_recorded
                && surface_output_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_spatial_source_lobes_cpu_output_generated_nonzero_surface_pixels_recorded";
        } else if (execution.last_cpu_output_nonzero
                && execution.last_scene_inputs_recorded
                && surface_output_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_tied_low_res_cpu_output_generated_nonzero_surface_pixels_recorded";
        } else if (execution.last_cpu_output_nonzero && execution.last_scene_inputs_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_tied_low_res_cpu_output_generated_nonzero_partial_scene_inputs_recorded";
        } else if (execution.last_cpu_output_nonzero) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_tied_low_res_cpu_output_generated_nonzero_scene_inputs_missing";
        } else if (execution.last_scene_inputs_recorded) {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_tied_low_res_cpu_output_generated_zero_signal_scene_inputs_recorded";
        } else {
            execution.last_readiness_reason =
                    "diffuse_gi_scene_tied_low_res_cpu_output_generated_zero_signal_scene_inputs_missing";
        }
    } else {
        execution.last_readiness_reason = execution.last_resource_marker_recorded
            ? std::string(to_string(dispatch_stage)) + "_dispatch_accepted_metadata_marker_recorded"
            : std::string(to_string(dispatch_stage)) + "_dispatch_accepted_without_resource_marker";
    }
    return recorded_resources;
}

std::uint64_t Renderer::track_denoise_execution_scaffold() {
    auto& execution = staging_.lighting.denoise_execution;
    const auto& denoise_stage = lighting_stage_telemetry(NativeLightingDispatchStage::Denoise);
    const auto& composite_stage = lighting_stage_telemetry(NativeLightingDispatchStage::Composite);
    const auto& diffuse_gi_execution = staging_.lighting.diffuse_gi_execution;
    const auto& direct_execution = staging_.lighting.direct_execution;

    execution.attempts++;
    execution.last_frame_index = frame_index_;
    execution.last_packet_generation = staging_.lighting.last_packet_generation;
    execution.last_dispatch_generation = denoise_stage.last_generation;
    execution.last_width = non_negative_u64(denoise_stage.last_width);
    execution.last_height = non_negative_u64(denoise_stage.last_height);
    execution.last_input_count = non_negative_u64(denoise_stage.last_input_count);
    execution.last_output_count = non_negative_u64(denoise_stage.last_output_count);
    execution.last_sample_count = non_negative_u64(denoise_stage.last_sample_count);
    execution.last_flags = denoise_stage.last_flags;
    execution.last_enabled = denoise_stage.enabled_this_packet;
    execution.last_validated = denoise_stage.last_validated;
    execution.last_placeholder = denoise_stage.last_placeholder;
    execution.last_temporal_history = denoise_stage.last_temporal_history;
    execution.last_ready = denoise_stage.ready_for_native_execution_this_packet;
    execution.last_edge_inputs_available = denoise_stage.last_input_count >= 4;
    execution.last_diffuse_gi_signal_available = diffuse_gi_execution.last_cpu_output_generated
            || diffuse_gi_execution.last_output_count > 0;
    execution.last_direct_shadow_signal_available = direct_execution.last_output_count > 0
            || direct_execution.last_candidate_count > 0;
    execution.last_optional_specular_placeholder = true;
    execution.last_optional_ao_placeholder = true;
    execution.last_raw_gi_pixels = diffuse_gi_execution.last_cpu_output_generated
            ? diffuse_gi_execution.last_cpu_output_pixel_count
            : (diffuse_gi_execution.last_output_count > 0 ? diffuse_gi_execution.last_output_count : 0);
    execution.last_raw_gi_samples = diffuse_gi_execution.last_sample_count;
    execution.last_raw_gi_rays = diffuse_gi_execution.last_ray_count;
    execution.last_raw_gi_cache_reads = diffuse_gi_execution.last_cache_read_count;
    execution.last_edge_input_count = std::min<std::uint64_t>(
            non_negative_u64(denoise_stage.last_input_count),
            3);
    execution.last_history_input_count = denoise_stage.last_temporal_history
            || has_lighting_flag(denoise_stage.last_flags, kLightingDispatchFlagTemporalHistory)
            ? 1
            : 0;
    execution.last_denoised_output_pixels = 0;
    execution.last_previous_denoised_output_checksum = 0;
    execution.last_current_denoised_output_checksum = 0;
    execution.last_denoised_output_checksum = 0;
    execution.last_denoised_output_changed_pixels = 0;
    execution.last_denoised_output_mean_abs_delta = 0;
    execution.last_frame_to_frame_changed_pixels = 0;
    execution.last_frame_to_frame_mean_abs_delta = 0;
    execution.last_shader_denoise_output_image_candidate_width = 0;
    execution.last_shader_denoise_output_image_candidate_height = 0;
    execution.last_shader_denoise_output_image_candidate_pixels = 0;
    execution.last_shader_denoise_output_image_candidate_bytes = 0;
    execution.last_shader_denoise_output_image_candidate_checksum = 0;
    execution.last_shader_denoise_output_missing_prerequisite_count = 4;
    execution.last_temporal_stable_pixels = 0;
    execution.last_temporal_unstable_pixels = 0;
    execution.last_temporal_mean_abs_delta = 0;
    execution.last_temporal_history_confidence = 0;
    execution.last_temporal_flicker_score = 0;
    execution.last_raw_neighbor_luma_delta = 0;
    execution.last_denoised_neighbor_luma_delta = 0;
    execution.last_noise_reduction_percent = 0;
    execution.last_raw_gi_input_available = execution.last_diffuse_gi_signal_available
            || execution.last_raw_gi_samples > 0
            || execution.last_raw_gi_rays > 0
            || execution.last_raw_gi_cache_reads > 0;
    execution.last_raw_gi_input_ready = execution.last_raw_gi_input_available
            && denoise_stage.ready_for_native_execution_this_packet;
    execution.last_raw_gi_source_present = execution.last_raw_gi_input_available;
    execution.last_raw_direct_input_available = execution.last_direct_shadow_signal_available
            || direct_execution.last_sample_count > 0
            || direct_execution.last_ray_count > 0;
    execution.last_denoised_output_intent = denoise_stage.last_output_count > 0;
    execution.last_denoised_cpu_output_generated = false;
    execution.last_cpu_denoised_readback_ready = false;
    execution.last_denoised_output_differs_from_raw = false;
    execution.last_cpu_denoised_source_present = false;
    execution.last_shader_denoise_dispatch_intent = denoise_stage.enabled_this_packet
            && execution.last_denoised_output_intent;
    execution.last_shader_denoise_dispatch_prepared = execution.last_shader_denoise_dispatch_intent
            && denoise_stage.last_validated
            && execution.last_edge_input_count >= 3
            && execution.last_raw_gi_input_available;
    execution.last_shader_denoise_input_ready = execution.last_shader_denoise_dispatch_intent
            && execution.last_raw_gi_source_present
            && execution.last_edge_input_count >= 3;
    execution.last_shader_denoise_output_ready = false;
    execution.last_shader_denoise_output_image_ready = false;
    execution.last_shader_denoise_output_image_candidate_ready = false;
    execution.last_shader_denoise_output_image_candidate_cpu_staged = false;
    execution.last_shader_denoise_output_image_candidate_non_gpu = true;
    execution.last_shader_denoise_output_image_candidate_concrete = false;
    execution.last_shader_denoise_output_candidate_source_cpu_readback = false;
    execution.last_shader_denoise_output_native_image_ready = false;
    execution.last_shader_denoise_output_native_image_writable = false;
    execution.last_shader_denoise_output_native_image_shader_written = false;
    execution.last_shader_denoise_output_material_ready = false;
    execution.last_shader_denoise_output_native_material_ready = false;
    execution.last_shader_denoise_output_prerequisites_ready = false;
    execution.last_metadata_only_path = true;
    execution.last_real_denoise_shader_output = false;
    execution.last_shader_denoise_output_shader_generated = false;
    execution.last_cpu_fallback_quality_metrics = false;
    execution.last_composite_stage_recorded = composite_stage.packets > 0;
    execution.last_composite_enabled = composite_stage.enabled_this_packet;
    execution.last_composite_ready = composite_stage.ready_for_native_execution_this_packet;
    execution.last_composite_placeholder = composite_stage.last_placeholder;
    execution.last_composite_width = non_negative_u64(composite_stage.last_width);
    execution.last_composite_height = non_negative_u64(composite_stage.last_height);
    execution.last_composite_outputs = non_negative_u64(composite_stage.last_output_count);
    execution.last_composite_flags = composite_stage.last_flags;
    execution.last_edge_depth_available = denoise_stage.last_input_count >= 1;
    execution.last_edge_normal_available = denoise_stage.last_input_count >= 2;
    execution.last_edge_material_available = denoise_stage.last_input_count >= 3;
    execution.last_history_confidence_available = denoise_stage.last_temporal_history
            || has_lighting_flag(denoise_stage.last_flags, kLightingDispatchFlagTemporalHistory);
    execution.last_temporal_stability_ready = false;
    execution.last_shader_boundary_explicit = true;
    execution.last_metadata_only_proof_rejected = !execution.last_raw_gi_input_available
            || !execution.last_denoised_output_intent;
    execution.last_focus_window_capture_rejected = false;
    execution.last_proof_marker_evidence_rejected = execution.last_raw_gi_input_available;
    execution.last_temporary_direct_substitution_rejected = execution.last_raw_gi_input_available
            && execution.last_raw_direct_input_available;
    execution.last_rectangular_washout_rejected = execution.last_edge_inputs_available;
    execution.last_metadata_dispatch_recorded = denoise_stage.recorded_this_frame;
    execution.last_accepted = false;
    execution.last_resource_marker_recorded = false;
    execution.last_history_accepted = 0;
    execution.last_history_rejected = 0;
    execution.last_edge_rejected = 0;
    execution.last_edge_preserved = 0;
    execution.last_raw_input_marker = execution.last_raw_gi_input_available
            ? "raw_gi_input_metadata_available"
            : "raw_gi_input_metadata_missing";
    execution.last_source_identity_marker = execution.last_raw_gi_source_present
            ? (diffuse_gi_execution.last_cpu_output_generated
                    ? "raw_gi_source=native_diffuse_gi_cpu_readback"
                    : "raw_gi_source=dispatch_metadata_or_partial_native_gi")
            : "raw_gi_source=missing";
    execution.last_denoised_output_marker = execution.last_denoised_output_intent
            ? "denoised_output_intent_metadata_only_no_shader_output"
            : "denoised_output_intent_missing_no_shader_output";
    execution.last_shader_denoise_readiness_marker =
            execution.last_shader_denoise_input_ready
                    ? "shader_denoise_inputs_ready_output_pending_real_shader_output_false"
                    : "shader_denoise_inputs_pending_output_pending_real_shader_output_false";
    execution.last_shader_denoise_handoff_marker =
            execution.last_shader_denoise_dispatch_prepared
                    ? "shader_denoise_dispatch_prepared_waiting_for_output_image_material"
                    : "shader_denoise_dispatch_not_prepared";
    execution.last_shader_denoise_output_readiness_marker =
            "shader_output_image_missing_material_missing_cpu_readback_pending";
    execution.last_shader_denoise_output_image_candidate_marker =
            "shader_output_image_candidate_missing_cpu_stage_not_ready";
    execution.last_shader_denoise_output_candidate_source_marker =
            "shader_output_candidate_source=none";
    execution.last_shader_denoise_output_prerequisite_marker =
            "shader_output_prerequisites_missing_native_image_native_material_shader_write";
    execution.last_shader_denoise_output_missing_prerequisites =
            "native_output_image,native_output_image_writable,native_output_material,shader_write";
    execution.last_shader_denoise_output_image_blocker =
            "shader_output_image_ready_false_no_gpu_shader_generated_native_image";
    execution.last_shader_denoise_generation_marker =
            "real_shader_generated_output=false";
    execution.last_composite_marker = execution.last_composite_stage_recorded
            ? (execution.last_composite_placeholder
                    ? "composite_stage_metadata_recorded_placeholder"
                    : "composite_stage_metadata_recorded_non_placeholder")
            : "composite_stage_metadata_missing";
    execution.last_history_acceptance_reason = execution.last_temporal_history
            ? "native_temporal_history_not_evaluated"
            : "temporal_history_not_requested";
    execution.last_history_rejection_reason = execution.last_temporal_history
            ? "native_temporal_history_not_evaluated"
            : "temporal_history_not_requested";
    execution.last_shader_boundary_marker =
            "native_denoise_shader_output_absent_cpu_fallback_only_real_shader_output_false";
    execution.last_temporal_history_marker = execution.last_temporal_history
            ? "temporal_history_requested_waiting_for_cpu_history_metrics"
            : "temporal_history_not_requested";
    execution.last_temporal_stability_readiness_marker =
            "temporal_stability_waiting_for_cpu_denoised_history";
    execution.last_temporal_ghosting_risk_marker =
            "ghosting_risk_unavailable_no_current_cpu_denoised_output";
    execution.last_proof_boundary_marker =
            "denoise_requires_real_shader_output_or_controller_cpu_fallback_visual_proof_not_metadata_only";
    execution.last_quality_marker =
            "cpu_fallback_quality_metrics=false;real_shader_output=false;quality_metrics_not_generated";

    std::uint64_t recorded_resources = 0;
    const bool validated_placeholder_metadata = denoise_stage.enabled_this_packet
            && denoise_stage.last_validated
            && denoise_stage.last_placeholder;

    if (!denoise_stage.recorded_this_frame && !validated_placeholder_metadata) {
        execution.skipped++;
        execution.last_output_marker = "denoise_metadata_not_recorded";
        execution.last_readiness_reason = "denoise dispatch metadata unavailable";
        return 0;
    }

    execution.metadata_dispatches++;
    if (!denoise_stage.enabled_this_packet) {
        execution.skipped++;
        execution.last_output_marker = "denoise_disabled_noop";
        execution.last_readiness_reason = "denoise stage disabled";
        return 0;
    }

    if (!denoise_stage.ready_for_native_execution_this_packet && !validated_placeholder_metadata) {
        execution.skipped++;
        execution.last_output_marker = "denoise_not_ready_noop";
        execution.last_readiness_reason = denoise_stage.last_readiness_reason.empty()
                ? "denoise dispatch is metadata-only or unvalidated"
                : denoise_stage.last_readiness_reason;
        if (denoise_stage.last_temporal_history) {
            execution.last_history_rejected = execution.last_width * execution.last_height;
            execution.history_rejected += execution.last_history_rejected;
        }
        if (execution.last_edge_inputs_available) {
            execution.last_edge_rejected = execution.last_width * execution.last_height;
        }
        return 0;
    }

    execution.accepted++;
    execution.submitted++;
    execution.last_accepted = true;
    const bool denoised_output_generated = generate_denoised_diffuse_gi_cpu_output_rgba8();
    execution.last_output_marker = denoised_output_generated
            ? "first_practical_cpu_denoised_diffuse_gi_rgba8_generated"
            : "signal_separated_denoise_metadata_scaffold_no_render_output";
    execution.last_denoised_output_marker = denoised_output_generated
            ? "denoised_diffuse_gi_cpu_rgba8_output_generated_from_raw_gi"
            : execution.last_denoised_output_marker;
    execution.last_raw_gi_input_ready = execution.last_raw_gi_input_ready
            || denoised_output_generated;
    execution.last_cpu_denoised_source_present = denoised_output_generated;
    execution.last_cpu_denoised_readback_ready = denoised_output_generated;
    execution.last_metadata_only_path = !denoised_output_generated;
    execution.last_shader_denoise_output_ready = false;
    execution.last_shader_denoise_output_image_ready = false;
    execution.last_shader_denoise_output_material_ready = denoised_output_generated
            && execution.last_composite_stage_recorded
            && execution.last_composite_outputs > 0;
    execution.last_shader_denoise_output_native_material_ready = false;
    execution.last_shader_denoise_output_prerequisites_ready =
            execution.last_shader_denoise_output_native_image_ready
            && execution.last_shader_denoise_output_native_image_writable
            && execution.last_shader_denoise_output_native_image_shader_written
            && execution.last_shader_denoise_output_native_material_ready;
    execution.last_shader_denoise_output_missing_prerequisite_count =
            (execution.last_shader_denoise_output_native_image_ready ? 0ULL : 1ULL)
            + (execution.last_shader_denoise_output_native_image_writable ? 0ULL : 1ULL)
            + (execution.last_shader_denoise_output_native_image_shader_written ? 0ULL : 1ULL)
            + (execution.last_shader_denoise_output_native_material_ready ? 0ULL : 1ULL);
    execution.last_shader_denoise_output_shader_generated = false;
    execution.last_source_identity_marker = denoised_output_generated
            ? "raw_gi_source=native_diffuse_gi_cpu_readback;denoised_source=native_cpu_readback;shader_source=absent"
            : execution.last_source_identity_marker;
    execution.last_shader_denoise_readiness_marker =
            execution.last_shader_denoise_input_ready
                    ? "shader_denoise_inputs_ready_dispatch_intent_recorded_output_pending_real_shader_output_false"
                    : "shader_denoise_inputs_pending_dispatch_intent_recorded_output_pending_real_shader_output_false";
    execution.last_shader_denoise_handoff_marker = denoised_output_generated
            ? (execution.last_shader_denoise_output_material_ready
                    ? "cpu_readback_material_handoff_ready_shader_output_image_missing"
                    : "cpu_readback_ready_waiting_for_composite_material_handoff")
            : execution.last_shader_denoise_handoff_marker;
    execution.last_shader_denoise_output_readiness_marker = denoised_output_generated
            ? (execution.last_shader_denoise_output_material_ready
                    ? "shader_output_image_missing_material_handoff_ready_cpu_staged_candidate_available"
                    : "shader_output_image_missing_material_handoff_pending_cpu_staged_candidate_available")
            : execution.last_shader_denoise_output_readiness_marker;
    execution.last_shader_denoise_output_image_candidate_marker = denoised_output_generated
            ? "cpu_staged_shader_output_image_candidate_ready_non_gpu_non_real"
            : execution.last_shader_denoise_output_image_candidate_marker;
    execution.last_shader_denoise_output_candidate_source_marker = denoised_output_generated
            ? "shader_output_candidate_source=native_cpu_readback_rgba8"
            : execution.last_shader_denoise_output_candidate_source_marker;
    execution.last_shader_denoise_output_prerequisite_marker =
            execution.last_shader_denoise_output_material_ready
                    ? "shader_output_prerequisites_missing_native_image_native_material_shader_write_public_material_handoff_ready"
                    : "shader_output_prerequisites_missing_native_image_native_material_shader_write";
    execution.last_shader_denoise_output_missing_prerequisites =
            "native_output_image,native_output_image_writable,native_output_material,shader_write";
    execution.last_shader_denoise_output_image_blocker = denoised_output_generated
            ? "shader_output_image_ready_false_candidate_is_cpu_staged_not_gpu_shader_generated"
            : execution.last_shader_denoise_output_image_blocker;
    execution.last_shader_denoise_generation_marker =
            "shader_generated_output=false;source=native_cpu_denoised_readback;candidate=cpu_staged_non_gpu";
    execution.last_metadata_only_proof_rejected = !denoised_output_generated;
    execution.last_focus_window_capture_rejected = denoised_output_generated
            && execution.last_denoised_output_changed_pixels != 0
            && execution.last_denoised_output_changed_pixels < execution.last_denoised_output_pixels;
    execution.last_proof_marker_evidence_rejected = denoised_output_generated
            && execution.last_raw_gi_input_available
            && execution.last_denoised_output_differs_from_raw;
    execution.last_temporary_direct_substitution_rejected = denoised_output_generated
            && execution.last_raw_gi_input_available
            && execution.last_denoised_output_differs_from_raw;
    execution.last_rectangular_washout_rejected = denoised_output_generated
            && execution.last_noise_reduction_percent != 0
            && execution.last_edge_preserved != 0;
    execution.last_shader_boundary_marker = denoised_output_generated
            ? "native_cpu_denoise_output_generated_real_shader_output_false"
            : execution.last_shader_boundary_marker;
    execution.last_temporal_history_marker = denoise_stage.last_temporal_history
            ? "temporal_history_requested_cpu_history_metrics_recorded_or_pending"
            : "temporal_history_not_requested";
    execution.last_proof_boundary_marker = denoised_output_generated
            ? "cpu_fallback_denoise_visual_proof_allowed_but_shader_denoise_remains_open"
            : "denoise_metadata_only_rejected_no_output_generated";
    execution.last_readiness_reason = denoised_output_generated
            ? "first practical CPU edge-aware denoise foundation generated from native diffuse GI; no final shader quality claim"
            : (validated_placeholder_metadata
                    ? "signal-separated denoise validated placeholder metadata accepted; native denoise shader/output not implemented"
                    : "signal-separated denoise metadata accepted; native denoise shader/output not implemented");
    if (denoise_stage.last_temporal_history) {
        const auto pixel_count = execution.last_width * execution.last_height;
        if (!denoised_output_generated) {
            execution.last_history_accepted = 0;
            execution.last_history_rejected = pixel_count;
            execution.history_rejected = saturated_add(
                    execution.history_rejected,
                    execution.last_history_rejected);
            execution.last_history_acceptance_reason =
                    "no_cpu_denoised_output_available_for_temporal_acceptance";
            execution.last_history_rejection_reason =
                    "cpu_denoised_output_generation_failed";
        }
    }
    if (execution.last_edge_inputs_available) {
        const auto pixel_count = execution.last_width * execution.last_height;
        if (!denoised_output_generated) {
            execution.last_edge_preserved = 0;
            execution.last_edge_rejected = pixel_count;
        }
    }

    if (resources_ != nullptr && frame_open_ && resources_->has_context()) {
        resources_->track_transient_buffer(
                frame_index_,
                0,
                kLightingConstantsBytes,
                "render:signal-separated-denoise-contract-metadata");
        execution.resource_markers++;
        execution.last_resource_marker_recorded = true;
        recorded_resources++;
        if (denoised_output_generated) {
            resources_->track_transient_image(
                    frame_index_,
                    0,
                    static_cast<std::int32_t>(std::min<std::uint64_t>(
                            staging_.lighting.diffuse_gi_execution.last_cpu_output_width,
                            static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                    static_cast<std::int32_t>(std::min<std::uint64_t>(
                            staging_.lighting.diffuse_gi_execution.last_cpu_output_height,
                            static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                    kDiffuseGiVisibleSignalFormatTag,
                    "render:denoised-diffuse-gi-cpu-rgba8-output");
            execution.resource_markers++;
            recorded_resources++;
            resources_->track_transient_image(
                    frame_index_,
                    0,
                    static_cast<std::int32_t>(std::min<std::uint64_t>(
                            execution.last_shader_denoise_output_image_candidate_width,
                            static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                    static_cast<std::int32_t>(std::min<std::uint64_t>(
                            execution.last_shader_denoise_output_image_candidate_height,
                            static_cast<std::uint64_t>(kMaxLightingDispatchDimension))),
                    kDiffuseGiVisibleSignalFormatTag,
                    "render:shader-denoise-output-image-candidate-cpu-staged-non-gpu");
            execution.resource_markers++;
            recorded_resources++;
        }
    }

    return recorded_resources;
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
    staging_.lighting.diffuse_gi_execution.stage = NativeLightingDispatchStage::DiffuseGi;
    staging_.lighting.cache_execution.stage = NativeLightingDispatchStage::Cache;
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
