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
