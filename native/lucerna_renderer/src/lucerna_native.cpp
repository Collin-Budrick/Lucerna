#include "lucerna_renderer.hpp"

#include "lucerna_resource_manager.hpp"

#include <jni.h>
#include <exception>
#include <memory>
#include <mutex>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>

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
Java_net_lucerna_nativebridge_LucernaNativeBridge_nativeUploadWorldDeltas(JNIEnv*, jclass, jlong generation, jint dirty_region_count, jint material_update_count) {
    std::lock_guard lock(g_renderer_mutex);
    return call_initialized_renderer("uploadWorldDeltas", [generation, dirty_region_count, material_update_count](lucerna::Renderer& renderer) {
        if (generation < 0) {
            throw std::invalid_argument("generation must be non-negative");
        }

        renderer.upload_world_deltas(lucerna::UploadSummary{
            static_cast<std::uint64_t>(generation),
            dirty_region_count,
            material_update_count
        });
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
