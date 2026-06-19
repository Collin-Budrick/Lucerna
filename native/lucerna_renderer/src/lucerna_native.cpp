#include "lucerna_renderer.hpp"

#include "lucerna_resource_manager.hpp"

#include <jni.h>
#include <cstddef>
#include <exception>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace {

std::mutex g_renderer_mutex;
std::unique_ptr<lucerna::Renderer> g_renderer;
std::string g_last_error;

lucerna::Renderer& renderer() {
    if (g_renderer == nullptr) {
        g_renderer = std::make_unique<lucerna::Renderer>();
    }

    return *g_renderer;
}

lucerna::Renderer& initialized_renderer() {
    if (g_renderer == nullptr || !g_renderer->initialized()) {
        throw std::logic_error("renderer is not initialized");
    }

    return *g_renderer;
}

void clear_last_error() {
    g_last_error.clear();
}

void set_last_error(const char* operation, const std::string& message) {
    g_last_error = std::string(operation) + " failed: " + message;
}

template <typename Function>
jboolean call_native(const char* operation, Function&& function) {
    try {
        std::forward<Function>(function)();
        clear_last_error();
        return JNI_TRUE;
    } catch (const std::exception& exception) {
        set_last_error(operation, exception.what());
        return JNI_FALSE;
    } catch (...) {
        set_last_error(operation, "unknown native exception");
        return JNI_FALSE;
    }
}

template <typename Function>
jboolean call_initialized_renderer(const char* operation, Function&& function) {
    return call_native(operation, [&function]() {
        std::forward<Function>(function)(initialized_renderer());
    });
}

std::uint64_t to_handle(jlong handle) {
    return static_cast<std::uint64_t>(handle);
}

class LocalRef {
public:
    LocalRef(JNIEnv* env, jobject object)
        : env_(env), object_(object) {
    }

    ~LocalRef() {
        if (object_ != nullptr) {
            env_->DeleteLocalRef(object_);
        }
    }

    LocalRef(const LocalRef&) = delete;
    LocalRef& operator=(const LocalRef&) = delete;

    [[nodiscard]] jobject get() const {
        return object_;
    }

private:
    JNIEnv* env_;
    jobject object_;
};

std::uint64_t to_non_negative_uint64(jlong value, const char* name) {
    if (value < 0) {
        throw std::invalid_argument(std::string(name) + " must be non-negative");
    }

    return static_cast<std::uint64_t>(value);
}

std::size_t array_length(JNIEnv* env, jarray array, const char* name) {
    if (array == nullptr) {
        throw std::invalid_argument(std::string(name) + " must not be null");
    }

    return static_cast<std::size_t>(env->GetArrayLength(array));
}

std::vector<std::int32_t> read_int_array(JNIEnv* env, jintArray array, const char* name) {
    const auto length = array_length(env, array, name);
    std::vector<jint> raw(length);
    env->GetIntArrayRegion(array, 0, static_cast<jsize>(length), raw.data());

    std::vector<std::int32_t> values;
    values.reserve(length);
    for (jint value : raw) {
        values.push_back(static_cast<std::int32_t>(value));
    }
    return values;
}

std::vector<std::uint64_t> read_long_array(JNIEnv* env, jlongArray array, const char* name) {
    const auto length = array_length(env, array, name);
    std::vector<jlong> raw(length);
    env->GetLongArrayRegion(array, 0, static_cast<jsize>(length), raw.data());

    std::vector<std::uint64_t> values;
    values.reserve(length);
    for (jlong value : raw) {
        values.push_back(to_non_negative_uint64(value, name));
    }
    return values;
}

std::vector<float> read_float_array(JNIEnv* env, jfloatArray array, const char* name) {
    const auto length = array_length(env, array, name);
    std::vector<jfloat> raw(length);
    env->GetFloatArrayRegion(array, 0, static_cast<jsize>(length), raw.data());

    std::vector<float> values;
    values.reserve(length);
    for (jfloat value : raw) {
        values.push_back(static_cast<float>(value));
    }
    return values;
}

std::string read_string(JNIEnv* env, jstring value, const char* name) {
    if (value == nullptr) {
        throw std::invalid_argument(std::string(name) + " must not contain null entries");
    }

    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error(std::string("could not read ") + name);
    }

    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

std::vector<std::string> read_string_array(JNIEnv* env, jobjectArray array, const char* name) {
    const auto length = array_length(env, array, name);
    std::vector<std::string> values;
    values.reserve(length);

    for (std::size_t index = 0; index < length; index++) {
        LocalRef element(env, env->GetObjectArrayElement(array, static_cast<jsize>(index)));
        values.push_back(read_string(env, static_cast<jstring>(element.get()), name));
    }

    return values;
}

void require_length(std::size_t expected, std::size_t actual, const char* name) {
    if (actual != expected) {
        std::ostringstream error;
        error << name << " length must be " << expected << " but was " << actual;
        throw std::invalid_argument(error.str());
    }
}

void require_payload_not_larger_than_count(std::size_t payload_count, jint advertised_count, const char* name) {
    if (advertised_count < 0) {
        throw std::invalid_argument(std::string(name) + " count must be non-negative");
    }
    if (payload_count > static_cast<std::size_t>(advertised_count)) {
        throw std::invalid_argument(std::string(name) + " payload count exceeds advertised count");
    }
}

std::size_t sum_counts(const std::vector<std::int32_t>& counts, const char* name) {
    std::size_t total = 0;
    for (const auto count : counts) {
        if (count < 0) {
            throw std::invalid_argument(std::string(name) + " entries must be non-negative");
        }
        total += static_cast<std::size_t>(count);
    }
    return total;
}

lucerna::UploadPacket make_upload_packet(
        JNIEnv* env,
        jlong generation,
        jint dirty_region_count,
        jint material_update_count,
        jlong first_world_generation,
        jlong last_world_generation,
        jlong material_generation,
        jintArray dirty_region_type_ids,
        jobjectArray dirty_region_dimensions,
        jintArray dirty_region_section_xs,
        jintArray dirty_region_section_ys,
        jintArray dirty_region_section_zs,
        jintArray dirty_region_section_scoped,
        jlongArray dirty_region_generations,
        jintArray material_ids,
        jlongArray material_generations,
        jobjectArray material_block_ids,
        jintArray material_face_ids,
        jintArray material_albedo_texture_indices,
        jfloatArray material_properties,
        jintArray material_flags) {
    constexpr std::size_t material_property_stride = 6;

    auto dirty_type_ids = read_int_array(env, dirty_region_type_ids, "dirtyRegionTypeIds");
    auto dirty_dimensions = read_string_array(env, dirty_region_dimensions, "dirtyRegionDimensions");
    auto dirty_section_xs = read_int_array(env, dirty_region_section_xs, "dirtyRegionSectionXs");
    auto dirty_section_ys = read_int_array(env, dirty_region_section_ys, "dirtyRegionSectionYs");
    auto dirty_section_zs = read_int_array(env, dirty_region_section_zs, "dirtyRegionSectionZs");
    auto dirty_section_scoped = read_int_array(env, dirty_region_section_scoped, "dirtyRegionSectionScoped");
    auto dirty_generations = read_long_array(env, dirty_region_generations, "dirtyRegionGenerations");

    const auto dirty_payload_count = dirty_type_ids.size();
    require_length(dirty_payload_count, dirty_dimensions.size(), "dirtyRegionDimensions");
    require_length(dirty_payload_count, dirty_section_xs.size(), "dirtyRegionSectionXs");
    require_length(dirty_payload_count, dirty_section_ys.size(), "dirtyRegionSectionYs");
    require_length(dirty_payload_count, dirty_section_zs.size(), "dirtyRegionSectionZs");
    require_length(dirty_payload_count, dirty_section_scoped.size(), "dirtyRegionSectionScoped");
    require_length(dirty_payload_count, dirty_generations.size(), "dirtyRegionGenerations");
    require_payload_not_larger_than_count(dirty_payload_count, dirty_region_count, "dirty region");

    auto material_id_values = read_int_array(env, material_ids, "materialIds");
    auto material_generation_values = read_long_array(env, material_generations, "materialGenerations");
    auto material_block_id_values = read_string_array(env, material_block_ids, "materialBlockIds");
    auto material_face_id_values = read_int_array(env, material_face_ids, "materialFaceIds");
    auto material_albedo_values = read_int_array(env, material_albedo_texture_indices, "materialAlbedoTextureIndices");
    auto material_property_values = read_float_array(env, material_properties, "materialProperties");
    auto material_flag_values = read_int_array(env, material_flags, "materialFlags");

    const auto material_payload_count = material_id_values.size();
    require_length(material_payload_count, material_generation_values.size(), "materialGenerations");
    require_length(material_payload_count, material_block_id_values.size(), "materialBlockIds");
    require_length(material_payload_count, material_face_id_values.size(), "materialFaceIds");
    require_length(material_payload_count, material_albedo_values.size(), "materialAlbedoTextureIndices");
    require_length(material_payload_count, material_flag_values.size(), "materialFlags");
    require_length(material_payload_count * material_property_stride, material_property_values.size(), "materialProperties");
    require_payload_not_larger_than_count(material_payload_count, material_update_count, "material");

    lucerna::UploadPacket packet{
        to_non_negative_uint64(generation, "generation"),
        dirty_region_count,
        material_update_count,
        to_non_negative_uint64(first_world_generation, "firstWorldGeneration"),
        to_non_negative_uint64(last_world_generation, "lastWorldGeneration"),
        to_non_negative_uint64(material_generation, "materialGeneration"),
        {},
        {}
    };

    packet.dirty_regions.reserve(dirty_payload_count);
    for (std::size_t index = 0; index < dirty_payload_count; index++) {
        packet.dirty_regions.push_back(lucerna::DirtyRegionUpload{
            dirty_type_ids[index],
            std::move(dirty_dimensions[index]),
            dirty_section_xs[index],
            dirty_section_ys[index],
            dirty_section_zs[index],
            dirty_section_scoped[index] != 0,
            dirty_generations[index]
        });
    }

    packet.material_updates.reserve(material_payload_count);
    for (std::size_t index = 0; index < material_payload_count; index++) {
        const auto property_offset = index * material_property_stride;
        packet.material_updates.push_back(lucerna::MaterialUpload{
            material_id_values[index],
            material_generation_values[index],
            std::move(material_block_id_values[index]),
            material_face_id_values[index],
            material_albedo_values[index],
            material_property_values[property_offset],
            material_property_values[property_offset + 1],
            material_property_values[property_offset + 2],
            material_property_values[property_offset + 3],
            material_property_values[property_offset + 4],
            material_property_values[property_offset + 5],
            material_flag_values[index]
        });
    }

    return packet;
}

lucerna::SectionUploadPacket make_section_upload_packet(
        JNIEnv* env,
        jlong generation,
        jint section_snapshot_count,
        jlong first_section_snapshot_generation,
        jlong last_section_snapshot_generation,
        jlong section_generation,
        jlong section_material_generation,
        jlong section_occupancy_generation,
        jlong section_emissive_generation,
        jlong section_dirty_region_generation,
        jintArray dirty_region_type_ids,
        jobjectArray dirty_region_type_names,
        jobjectArray dirty_region_dimensions,
        jintArray dirty_region_section_xs,
        jintArray dirty_region_section_ys,
        jintArray dirty_region_section_zs,
        jintArray dirty_region_section_scoped,
        jlongArray dirty_region_generations,
        jobjectArray section_dimensions,
        jintArray section_xs,
        jintArray section_ys,
        jintArray section_zs,
        jlongArray section_generations,
        jlongArray material_generations,
        jlongArray occupancy_generations,
        jlongArray section_emissive_generations,
        jintArray voxel_counts,
        jintArray occupancy_bit_order_ids,
        jobjectArray occupancy_bit_order_names,
        jintArray occupancy_mask_word_offsets,
        jintArray occupancy_mask_word_counts,
        jintArray occupancy_mask_bit_counts,
        jlongArray occupancy_mask_generations,
        jintArray material_palette_offsets,
        jlongArray material_palette_generations,
        jintArray material_palette_counts,
        jintArray material_palette_ids,
        jintArray emissive_entry_counts,
        jintArray emissive_voxel_indices,
        jintArray emissive_material_ids,
        jintArray emissive_block_light_levels,
        jlongArray emissive_entry_generations) {
    constexpr std::size_t voxel_count_stride = 5;

    auto section_dimension_values = read_string_array(env, section_dimensions, "sectionDimensions");
    const auto payload_count = section_dimension_values.size();
    require_payload_not_larger_than_count(payload_count, section_snapshot_count, "section snapshot");

    auto dirty_type_ids = read_int_array(env, dirty_region_type_ids, "sectionDirtyRegionTypeIds");
    auto dirty_type_names = read_string_array(env, dirty_region_type_names, "sectionDirtyRegionTypeNames");
    auto dirty_dimensions = read_string_array(env, dirty_region_dimensions, "sectionDirtyRegionDimensions");
    auto dirty_section_xs = read_int_array(env, dirty_region_section_xs, "sectionDirtyRegionSectionXs");
    auto dirty_section_ys = read_int_array(env, dirty_region_section_ys, "sectionDirtyRegionSectionYs");
    auto dirty_section_zs = read_int_array(env, dirty_region_section_zs, "sectionDirtyRegionSectionZs");
    auto dirty_section_scoped = read_int_array(env, dirty_region_section_scoped, "sectionDirtyRegionSectionScoped");
    auto dirty_generations = read_long_array(env, dirty_region_generations, "sectionDirtyRegionGenerations");

    require_length(payload_count, dirty_type_ids.size(), "sectionDirtyRegionTypeIds");
    require_length(payload_count, dirty_type_names.size(), "sectionDirtyRegionTypeNames");
    require_length(payload_count, dirty_dimensions.size(), "sectionDirtyRegionDimensions");
    require_length(payload_count, dirty_section_xs.size(), "sectionDirtyRegionSectionXs");
    require_length(payload_count, dirty_section_ys.size(), "sectionDirtyRegionSectionYs");
    require_length(payload_count, dirty_section_zs.size(), "sectionDirtyRegionSectionZs");
    require_length(payload_count, dirty_section_scoped.size(), "sectionDirtyRegionSectionScoped");
    require_length(payload_count, dirty_generations.size(), "sectionDirtyRegionGenerations");

    auto section_x_values = read_int_array(env, section_xs, "sectionXs");
    auto section_y_values = read_int_array(env, section_ys, "sectionYs");
    auto section_z_values = read_int_array(env, section_zs, "sectionZs");
    auto section_generation_values = read_long_array(env, section_generations, "sectionGenerations");
    auto material_generation_values = read_long_array(env, material_generations, "sectionMaterialGenerations");
    auto occupancy_generation_values = read_long_array(env, occupancy_generations, "sectionOccupancyGenerations");
    auto emissive_generation_values = read_long_array(env, section_emissive_generations, "sectionEmissiveGenerations");
    auto voxel_count_values = read_int_array(env, voxel_counts, "sectionVoxelCounts");
    auto occupancy_bit_order_id_values = read_int_array(env, occupancy_bit_order_ids, "occupancyBitOrderIds");
    auto occupancy_bit_order_name_values = read_string_array(env, occupancy_bit_order_names, "occupancyBitOrderNames");
    auto occupancy_mask_word_offset_values = read_int_array(env, occupancy_mask_word_offsets, "occupancyMaskWordOffsets");
    auto occupancy_mask_word_count_values = read_int_array(env, occupancy_mask_word_counts, "occupancyMaskWordCounts");
    auto occupancy_mask_bit_count_values = read_int_array(env, occupancy_mask_bit_counts, "occupancyMaskBitCounts");
    auto occupancy_mask_generation_values = read_long_array(env, occupancy_mask_generations, "occupancyMaskGenerations");
    auto material_palette_offset_values = read_int_array(env, material_palette_offsets, "materialPaletteOffsets");
    auto material_palette_generation_values = read_long_array(env, material_palette_generations, "materialPaletteGenerations");
    auto material_palette_count_values = read_int_array(env, material_palette_counts, "materialPaletteCounts");
    auto material_palette_id_values = read_int_array(env, material_palette_ids, "materialPaletteIds");
    auto emissive_entry_count_values = read_int_array(env, emissive_entry_counts, "emissiveEntryCounts");
    auto emissive_voxel_index_values = read_int_array(env, emissive_voxel_indices, "emissiveVoxelIndices");
    auto emissive_material_id_values = read_int_array(env, emissive_material_ids, "emissiveMaterialIds");
    auto emissive_block_light_level_values = read_int_array(env, emissive_block_light_levels, "emissiveBlockLightLevels");
    auto emissive_entry_generation_values = read_long_array(env, emissive_entry_generations, "emissiveEntryGenerations");

    require_length(payload_count, section_x_values.size(), "sectionXs");
    require_length(payload_count, section_y_values.size(), "sectionYs");
    require_length(payload_count, section_z_values.size(), "sectionZs");
    require_length(payload_count, section_generation_values.size(), "sectionGenerations");
    require_length(payload_count, material_generation_values.size(), "sectionMaterialGenerations");
    require_length(payload_count, occupancy_generation_values.size(), "sectionOccupancyGenerations");
    require_length(payload_count, emissive_generation_values.size(), "sectionEmissiveGenerations");
    require_length(payload_count * voxel_count_stride, voxel_count_values.size(), "sectionVoxelCounts");
    require_length(payload_count, occupancy_bit_order_id_values.size(), "occupancyBitOrderIds");
    require_length(payload_count, occupancy_bit_order_name_values.size(), "occupancyBitOrderNames");
    require_length(payload_count, occupancy_mask_word_offset_values.size(), "occupancyMaskWordOffsets");
    require_length(payload_count, occupancy_mask_word_count_values.size(), "occupancyMaskWordCounts");
    require_length(payload_count, occupancy_mask_bit_count_values.size(), "occupancyMaskBitCounts");
    require_length(payload_count, occupancy_mask_generation_values.size(), "occupancyMaskGenerations");
    require_length(payload_count, material_palette_offset_values.size(), "materialPaletteOffsets");
    require_length(payload_count, material_palette_generation_values.size(), "materialPaletteGenerations");
    require_length(payload_count, material_palette_count_values.size(), "materialPaletteCounts");
    require_length(payload_count, emissive_entry_count_values.size(), "emissiveEntryCounts");

    const auto material_palette_payload_count = sum_counts(material_palette_count_values, "materialPaletteCounts");
    require_length(material_palette_payload_count, material_palette_id_values.size(), "materialPaletteIds");

    const auto emissive_payload_count = sum_counts(emissive_entry_count_values, "emissiveEntryCounts");
    require_length(emissive_payload_count, emissive_voxel_index_values.size(), "emissiveVoxelIndices");
    require_length(emissive_payload_count, emissive_material_id_values.size(), "emissiveMaterialIds");
    require_length(emissive_payload_count, emissive_block_light_level_values.size(), "emissiveBlockLightLevels");
    require_length(emissive_payload_count, emissive_entry_generation_values.size(), "emissiveEntryGenerations");

    lucerna::SectionUploadPacket packet{
        to_non_negative_uint64(generation, "generation"),
        section_snapshot_count,
        to_non_negative_uint64(first_section_snapshot_generation, "firstSectionSnapshotGeneration"),
        to_non_negative_uint64(last_section_snapshot_generation, "lastSectionSnapshotGeneration"),
        to_non_negative_uint64(section_generation, "sectionGeneration"),
        to_non_negative_uint64(section_material_generation, "sectionMaterialGeneration"),
        to_non_negative_uint64(section_occupancy_generation, "sectionOccupancyGeneration"),
        to_non_negative_uint64(section_emissive_generation, "sectionEmissiveGeneration"),
        to_non_negative_uint64(section_dirty_region_generation, "sectionDirtyRegionGeneration"),
        {}
    };

    packet.snapshots.reserve(payload_count);
    std::size_t material_cursor = 0;
    std::size_t emissive_cursor = 0;
    for (std::size_t index = 0; index < payload_count; index++) {
        lucerna::SectionSnapshotUpload snapshot;
        snapshot.dirty_region = lucerna::SectionDirtyRegionHandoff{
            dirty_type_ids[index],
            std::move(dirty_type_names[index]),
            std::move(dirty_dimensions[index]),
            dirty_section_xs[index],
            dirty_section_ys[index],
            dirty_section_zs[index],
            dirty_section_scoped[index] != 0,
            dirty_generations[index]
        };
        snapshot.dimension = std::move(section_dimension_values[index]);
        snapshot.section_x = section_x_values[index];
        snapshot.section_y = section_y_values[index];
        snapshot.section_z = section_z_values[index];
        snapshot.section_generation = section_generation_values[index];
        snapshot.material_generation = material_generation_values[index];
        snapshot.occupancy_generation = occupancy_generation_values[index];
        snapshot.emissive_generation = emissive_generation_values[index];

        const auto voxel_count_offset = index * voxel_count_stride;
        snapshot.occupied_voxel_count = voxel_count_values[voxel_count_offset];
        snapshot.opaque_voxel_count = voxel_count_values[voxel_count_offset + 1];
        snapshot.translucent_voxel_count = voxel_count_values[voxel_count_offset + 2];
        snapshot.fluid_voxel_count = voxel_count_values[voxel_count_offset + 3];
        snapshot.emissive_voxel_count = voxel_count_values[voxel_count_offset + 4];

        snapshot.occupancy_bit_order_id = occupancy_bit_order_id_values[index];
        snapshot.occupancy_bit_order_name = std::move(occupancy_bit_order_name_values[index]);
        snapshot.occupancy_mask_word_offset = occupancy_mask_word_offset_values[index];
        snapshot.occupancy_mask_word_count = occupancy_mask_word_count_values[index];
        snapshot.occupancy_mask_bit_count = occupancy_mask_bit_count_values[index];
        snapshot.occupancy_mask_generation = occupancy_mask_generation_values[index];
        snapshot.material_palette_offset = material_palette_offset_values[index];
        snapshot.material_palette_generation = material_palette_generation_values[index];

        const auto material_count = static_cast<std::size_t>(material_palette_count_values[index]);
        snapshot.material_palette_ids.reserve(material_count);
        for (std::size_t material_index = 0; material_index < material_count; material_index++) {
            snapshot.material_palette_ids.push_back(material_palette_id_values[material_cursor + material_index]);
        }
        material_cursor += material_count;

        const auto emissive_count = static_cast<std::size_t>(emissive_entry_count_values[index]);
        snapshot.emissive_entries.reserve(emissive_count);
        for (std::size_t emissive_index = 0; emissive_index < emissive_count; emissive_index++) {
            const auto packed_index = emissive_cursor + emissive_index;
            snapshot.emissive_entries.push_back(lucerna::SectionEmissiveEntryUpload{
                emissive_voxel_index_values[packed_index],
                emissive_material_id_values[packed_index],
                emissive_block_light_level_values[packed_index],
                emissive_entry_generation_values[packed_index]
            });
        }
        emissive_cursor += emissive_count;

        packet.snapshots.push_back(std::move(snapshot));
    }

    return packet;
}

lucerna::GBufferStagingPacket make_gbuffer_staging_packet(
        JNIEnv* env,
        jlong generation,
        jint gbuffer_count,
        jlong first_gbuffer_generation,
        jlong last_gbuffer_generation,
        jobjectArray pass_ids,
        jintArray numeric_pass_ids,
        jintArray widths,
        jintArray heights,
        jintArray attachment_counts,
        jobjectArray attachment_names,
        jintArray attachment_formats,
        jintArray attachment_widths,
        jintArray attachment_heights,
        jintArray attachment_samples,
        jintArray attachment_enabled) {
    auto pass_id_values = read_string_array(env, pass_ids, "gbufferPassIds");
    const auto payload_count = pass_id_values.size();
    require_payload_not_larger_than_count(payload_count, gbuffer_count, "gbuffer");

    auto numeric_pass_id_values = read_int_array(env, numeric_pass_ids, "gbufferNumericPassIds");
    auto width_values = read_int_array(env, widths, "gbufferWidths");
    auto height_values = read_int_array(env, heights, "gbufferHeights");
    auto attachment_count_values = read_int_array(env, attachment_counts, "gbufferAttachmentCounts");

    require_length(payload_count, numeric_pass_id_values.size(), "gbufferNumericPassIds");
    require_length(payload_count, width_values.size(), "gbufferWidths");
    require_length(payload_count, height_values.size(), "gbufferHeights");
    require_length(payload_count, attachment_count_values.size(), "gbufferAttachmentCounts");

    const auto attachment_payload_count = sum_counts(attachment_count_values, "gbufferAttachmentCounts");
    auto attachment_name_values = read_string_array(env, attachment_names, "gbufferAttachmentNames");
    auto attachment_format_values = read_int_array(env, attachment_formats, "gbufferAttachmentFormats");
    auto attachment_width_values = read_int_array(env, attachment_widths, "gbufferAttachmentWidths");
    auto attachment_height_values = read_int_array(env, attachment_heights, "gbufferAttachmentHeights");
    auto attachment_sample_values = read_int_array(env, attachment_samples, "gbufferAttachmentSamples");
    auto attachment_enabled_values = read_int_array(env, attachment_enabled, "gbufferAttachmentEnabled");

    require_length(attachment_payload_count, attachment_name_values.size(), "gbufferAttachmentNames");
    require_length(attachment_payload_count, attachment_format_values.size(), "gbufferAttachmentFormats");
    require_length(attachment_payload_count, attachment_width_values.size(), "gbufferAttachmentWidths");
    require_length(attachment_payload_count, attachment_height_values.size(), "gbufferAttachmentHeights");
    require_length(attachment_payload_count, attachment_sample_values.size(), "gbufferAttachmentSamples");
    require_length(attachment_payload_count, attachment_enabled_values.size(), "gbufferAttachmentEnabled");

    lucerna::GBufferStagingPacket packet{
        to_non_negative_uint64(generation, "generation"),
        gbuffer_count,
        to_non_negative_uint64(first_gbuffer_generation, "firstGbufferGeneration"),
        to_non_negative_uint64(last_gbuffer_generation, "lastGbufferGeneration"),
        {}
    };

    packet.gbuffers.reserve(payload_count);
    std::size_t attachment_cursor = 0;
    for (std::size_t index = 0; index < payload_count; index++) {
        lucerna::GBufferStagingUpload upload;
        upload.pass_id = std::move(pass_id_values[index]);
        upload.numeric_pass_id = numeric_pass_id_values[index];
        upload.width = width_values[index];
        upload.height = height_values[index];
        upload.attachment_count = attachment_count_values[index];

        const auto attachment_count = static_cast<std::size_t>(attachment_count_values[index]);
        upload.attachments.reserve(attachment_count);
        for (std::size_t attachment_index = 0; attachment_index < attachment_count; attachment_index++) {
            const auto packed_index = attachment_cursor + attachment_index;
            const auto enabled = attachment_enabled_values[packed_index];
            if (enabled != 0 && enabled != 1) {
                throw std::invalid_argument("gbufferAttachmentEnabled entries must be 0 or 1");
            }

            upload.attachments.push_back(lucerna::GBufferAttachmentUpload{
                std::move(attachment_name_values[packed_index]),
                attachment_format_values[packed_index],
                attachment_width_values[packed_index],
                attachment_height_values[packed_index],
                attachment_sample_values[packed_index],
                enabled != 0
            });
        }
        attachment_cursor += attachment_count;
        packet.gbuffers.push_back(std::move(upload));
    }

    return packet;
}

std::string last_error_locked() {
    if (!g_last_error.empty()) {
        return g_last_error;
    }

    if (g_renderer != nullptr && !g_renderer->last_error().empty()) {
        return g_renderer->last_error();
    }

    return {};
}

jstring to_java_string(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeInit(JNIEnv*, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    return call_native("init", []() {
        renderer().init();
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeShutdown(JNIEnv*, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    return call_native("shutdown", []() {
        if (g_renderer != nullptr) {
            g_renderer->shutdown();
            g_renderer.reset();
        }
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeOnResize(JNIEnv*, jclass, jint width, jint height) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("resize", [width, height](lucerna::Renderer& renderer) {
        renderer.resize(width, height);
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeBeginFrame(JNIEnv*, jclass, jlong frame_index, jfloat tick_delta) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("beginFrame", [frame_index, tick_delta](lucerna::Renderer& renderer) {
        if (frame_index < 0) {
            throw std::invalid_argument("frame index must be non-negative");
        }

        renderer.begin_frame(lucerna::FrameInfo{
            static_cast<std::uint64_t>(frame_index),
            tick_delta
        });
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeUploadWorldDeltas(
        JNIEnv* env,
        jclass,
        jlong generation,
        jint dirty_region_count,
        jint material_update_count,
        jlong first_world_generation,
        jlong last_world_generation,
        jlong material_generation,
        jintArray dirty_region_type_ids,
        jobjectArray dirty_region_dimensions,
        jintArray dirty_region_section_xs,
        jintArray dirty_region_section_ys,
        jintArray dirty_region_section_zs,
        jintArray dirty_region_section_scoped,
        jlongArray dirty_region_generations,
        jintArray material_ids,
        jlongArray material_generations,
        jobjectArray material_block_ids,
        jintArray material_face_ids,
        jintArray material_albedo_texture_indices,
        jfloatArray material_properties,
        jintArray material_flags) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("uploadWorldDeltas", [=](lucerna::Renderer& renderer) {
        renderer.upload_world_deltas(make_upload_packet(
                env,
                generation,
                dirty_region_count,
                material_update_count,
                first_world_generation,
                last_world_generation,
                material_generation,
                dirty_region_type_ids,
                dirty_region_dimensions,
                dirty_region_section_xs,
                dirty_region_section_ys,
                dirty_region_section_zs,
                dirty_region_section_scoped,
                dirty_region_generations,
                material_ids,
                material_generations,
                material_block_ids,
                material_face_ids,
                material_albedo_texture_indices,
                material_properties,
                material_flags
        ));
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeUploadSectionSnapshots(
        JNIEnv* env,
        jclass,
        jlong generation,
        jint section_snapshot_count,
        jlong first_section_snapshot_generation,
        jlong last_section_snapshot_generation,
        jlong section_generation,
        jlong section_material_generation,
        jlong section_occupancy_generation,
        jlong section_emissive_generation,
        jlong section_dirty_region_generation,
        jintArray dirty_region_type_ids,
        jobjectArray dirty_region_type_names,
        jobjectArray dirty_region_dimensions,
        jintArray dirty_region_section_xs,
        jintArray dirty_region_section_ys,
        jintArray dirty_region_section_zs,
        jintArray dirty_region_section_scoped,
        jlongArray dirty_region_generations,
        jobjectArray section_dimensions,
        jintArray section_xs,
        jintArray section_ys,
        jintArray section_zs,
        jlongArray section_generations,
        jlongArray material_generations,
        jlongArray occupancy_generations,
        jlongArray section_emissive_generations,
        jintArray voxel_counts,
        jintArray occupancy_bit_order_ids,
        jobjectArray occupancy_bit_order_names,
        jintArray occupancy_mask_word_offsets,
        jintArray occupancy_mask_word_counts,
        jintArray occupancy_mask_bit_counts,
        jlongArray occupancy_mask_generations,
        jintArray material_palette_offsets,
        jlongArray material_palette_generations,
        jintArray material_palette_counts,
        jintArray material_palette_ids,
        jintArray emissive_entry_counts,
        jintArray emissive_voxel_indices,
        jintArray emissive_material_ids,
        jintArray emissive_block_light_levels,
        jlongArray emissive_entry_generations) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("uploadSectionSnapshots", [=](lucerna::Renderer& renderer) {
        renderer.upload_section_snapshots(make_section_upload_packet(
                env,
                generation,
                section_snapshot_count,
                first_section_snapshot_generation,
                last_section_snapshot_generation,
                section_generation,
                section_material_generation,
                section_occupancy_generation,
                section_emissive_generation,
                section_dirty_region_generation,
                dirty_region_type_ids,
                dirty_region_type_names,
                dirty_region_dimensions,
                dirty_region_section_xs,
                dirty_region_section_ys,
                dirty_region_section_zs,
                dirty_region_section_scoped,
                dirty_region_generations,
                section_dimensions,
                section_xs,
                section_ys,
                section_zs,
                section_generations,
                material_generations,
                occupancy_generations,
                section_emissive_generations,
                voxel_counts,
                occupancy_bit_order_ids,
                occupancy_bit_order_names,
                occupancy_mask_word_offsets,
                occupancy_mask_word_counts,
                occupancy_mask_bit_counts,
                occupancy_mask_generations,
                material_palette_offsets,
                material_palette_generations,
                material_palette_counts,
                material_palette_ids,
                emissive_entry_counts,
                emissive_voxel_indices,
                emissive_material_ids,
                emissive_block_light_levels,
                emissive_entry_generations
        ));
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeUploadGBufferStaging(
        JNIEnv* env,
        jclass,
        jlong generation,
        jint gbuffer_count,
        jlong first_gbuffer_generation,
        jlong last_gbuffer_generation,
        jobjectArray pass_ids,
        jintArray numeric_pass_ids,
        jintArray widths,
        jintArray heights,
        jintArray attachment_counts,
        jobjectArray attachment_names,
        jintArray attachment_formats,
        jintArray attachment_widths,
        jintArray attachment_heights,
        jintArray attachment_samples,
        jintArray attachment_enabled) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("uploadGBufferStaging", [=](lucerna::Renderer& renderer) {
        renderer.upload_gbuffer_staging(make_gbuffer_staging_packet(
                env,
                generation,
                gbuffer_count,
                first_gbuffer_generation,
                last_gbuffer_generation,
                pass_ids,
                numeric_pass_ids,
                widths,
                heights,
                attachment_counts,
                attachment_names,
                attachment_formats,
                attachment_widths,
                attachment_heights,
                attachment_samples,
                attachment_enabled
        ));
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeRenderLighting(JNIEnv*, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("renderLighting", [](lucerna::Renderer& renderer) {
        renderer.render_lighting();
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeEndFrame(JNIEnv*, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("endFrame", [](lucerna::Renderer& renderer) {
        renderer.end_frame();
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeAdoptBorrowedVulkanContext(JNIEnv*, jclass, jlong instance, jlong physical_device, jlong device, jlong graphics_queue, jint graphics_queue_family) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("adoptBorrowedVulkanContext", [instance, physical_device, device, graphics_queue, graphics_queue_family](lucerna::Renderer& renderer) {
        if (graphics_queue_family < 0) {
            throw std::invalid_argument("graphics queue family must be non-negative");
        }

        renderer.adopt_borrowed_context(lucerna::BorrowedVulkanContext{
            to_handle(instance),
            to_handle(physical_device),
            to_handle(device),
            to_handle(graphics_queue),
            static_cast<std::uint32_t>(graphics_queue_family)
        });
    });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeReleaseBorrowedVulkanContext(JNIEnv*, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("releaseBorrowedVulkanContext", [](lucerna::Renderer& renderer) {
        renderer.release_borrowed_context();
    });
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeStatus(JNIEnv* env, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    try {
        std::ostringstream status;
        if (g_renderer == nullptr) {
            status << "renderer=absent";
        } else {
            status << "renderer=present " << g_renderer->status();
        }

        const auto last_error = last_error_locked();
        if (!last_error.empty()) {
            status << " last_error=\"" << last_error << "\"";
        }

        return to_java_string(env, status.str());
    } catch (const std::exception& exception) {
        set_last_error("status", exception.what());
        return to_java_string(env, g_last_error);
    } catch (...) {
        set_last_error("status", "unknown native exception");
        return to_java_string(env, g_last_error);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeLastError(JNIEnv* env, jclass) {
    std::lock_guard lock(g_renderer_mutex);
    try {
        return to_java_string(env, last_error_locked());
    } catch (const std::exception& exception) {
        set_last_error("lastError", exception.what());
        return to_java_string(env, g_last_error);
    } catch (...) {
        set_last_error("lastError", "unknown native exception");
        return to_java_string(env, g_last_error);
    }
}
