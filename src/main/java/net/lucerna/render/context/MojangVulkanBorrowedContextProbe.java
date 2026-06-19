package net.lucerna.render.context;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanInstance;
import com.mojang.blaze3d.vulkan.VulkanQueue;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.lang.reflect.Field;

public final class MojangVulkanBorrowedContextProbe implements BorrowedVulkanContextProbe {
    private static final String SOURCE = "mojang-vulkan-borrowed-context-probe";
    private static final MojangVulkanBorrowedContextProbe INSTANCE = new MojangVulkanBorrowedContextProbe();

    private volatile Field gpuDeviceBackendField;
    private volatile String gpuDeviceBackendFieldFailure;

    private MojangVulkanBorrowedContextProbe() {
    }

    public static MojangVulkanBorrowedContextProbe instance() {
        return INSTANCE;
    }

    @Override
    public BorrowedVulkanContextAcquisition acquire(VulkanFrameContextRequest request) {
        ProbeValue<GpuDevice> gpuDevice = currentGpuDevice();
        if (gpuDevice.failed()) {
            return unavailable(gpuDevice.failureMessage());
        }

        if (gpuDevice.value() == null) {
            return BorrowedVulkanContextAcquisition.absent(
                    SOURCE,
                    "RenderSystem has no initialized GPU device; Mojang Vulkan handles are not available yet."
            );
        }

        ProbeValue<GpuDeviceBackend> backend = readBackend(gpuDevice.value());
        if (backend.failed()) {
            return unavailable(backend.failureMessage());
        }

        if (backend.value() == null) {
            return unavailable("RenderSystem GPU device has a null backend; Mojang Vulkan handles cannot be borrowed.");
        }

        if (!(backend.value() instanceof VulkanDevice vulkanDevice)) {
            return BorrowedVulkanContextAcquisition.absent(
                    SOURCE,
                    "RenderSystem GPU backend is "
                            + className(backend.value())
                            + ", not com.mojang.blaze3d.vulkan.VulkanDevice."
            );
        }

        return extractHandles(vulkanDevice);
    }

    private static ProbeValue<GpuDevice> currentGpuDevice() {
        try {
            return ProbeValue.present(RenderSystem.tryGetDevice());
        } catch (RuntimeException | LinkageError exception) {
            return ProbeValue.failed(
                    "RenderSystem.tryGetDevice() failed while probing Mojang Vulkan context: "
                            + exceptionSummary(exception)
            );
        }
    }

    private ProbeValue<GpuDeviceBackend> readBackend(GpuDevice gpuDevice) {
        ProbeValue<Field> backendField = backendField();
        if (backendField.failed()) {
            return ProbeValue.failed(backendField.failureMessage());
        }

        Object backend;
        try {
            backend = backendField.value().get(gpuDevice);
        } catch (IllegalAccessException | RuntimeException | LinkageError exception) {
            return ProbeValue.failed(
                    "GpuDevice.backend could not be read reflectively: " + exceptionSummary(exception)
            );
        }

        if (backend == null) {
            return ProbeValue.present(null);
        }

        if (!(backend instanceof GpuDeviceBackend gpuDeviceBackend)) {
            return ProbeValue.failed(
                    "GpuDevice.backend resolved to "
                            + className(backend)
                            + ", not com.mojang.blaze3d.systems.GpuDeviceBackend."
            );
        }

        return ProbeValue.present(gpuDeviceBackend);
    }

    private ProbeValue<Field> backendField() {
        Field cachedField = this.gpuDeviceBackendField;
        if (cachedField != null) {
            return ProbeValue.present(cachedField);
        }

        String cachedFailure = this.gpuDeviceBackendFieldFailure;
        if (cachedFailure != null) {
            return ProbeValue.failed(cachedFailure);
        }

        synchronized (this) {
            cachedField = this.gpuDeviceBackendField;
            if (cachedField != null) {
                return ProbeValue.present(cachedField);
            }

            cachedFailure = this.gpuDeviceBackendFieldFailure;
            if (cachedFailure != null) {
                return ProbeValue.failed(cachedFailure);
            }

            try {
                Field backendField = GpuDevice.class.getDeclaredField("backend");
                backendField.setAccessible(true);
                this.gpuDeviceBackendField = backendField;
                return ProbeValue.present(backendField);
            } catch (NoSuchFieldException | RuntimeException | LinkageError exception) {
                String failure = "GpuDevice.backend is not reflectively accessible: "
                        + exceptionSummary(exception);
                this.gpuDeviceBackendFieldFailure = failure;
                return ProbeValue.failed(failure);
            }
        }
    }

    private static BorrowedVulkanContextAcquisition extractHandles(VulkanDevice vulkanDevice) {
        VulkanInstance mojangInstance;
        VkInstance vkInstance;
        VkDevice vkDevice;
        VkPhysicalDevice vkPhysicalDevice;
        VulkanQueue graphicsQueue;
        VkQueue vkGraphicsQueue;
        int graphicsQueueFamily;
        BorrowedVulkanContextHandles handles;

        try {
            mojangInstance = vulkanDevice.instance();
            vkInstance = mojangInstance == null ? null : mojangInstance.vkInstance();
            vkDevice = vulkanDevice.vkDevice();
            vkPhysicalDevice = vkDevice == null ? null : vkDevice.getPhysicalDevice();
            graphicsQueue = vulkanDevice.graphicsQueue();
            vkGraphicsQueue = graphicsQueue == null ? null : graphicsQueue.vkQueue();
            graphicsQueueFamily = graphicsQueue == null ? -1 : graphicsQueue.queueFamilyIndex();
            handles = BorrowedVulkanContextHandles.required(
                    address(vkInstance),
                    address(vkPhysicalDevice),
                    address(vkDevice),
                    address(vkGraphicsQueue),
                    graphicsQueueFamily
            );
        } catch (RuntimeException | LinkageError exception) {
            return unavailable(
                    "Mojang Vulkan backend is active, but handle extraction failed: " + exceptionSummary(exception)
            );
        }

        BorrowedVulkanContextValidation validation = handles.validateRequiredHandles();
        if (!validation.valid()) {
            return BorrowedVulkanContextAcquisition.unavailable(
                    SOURCE,
                    "Mojang Vulkan backend is active, but " + validation.message(),
                    validation
            );
        }

        return BorrowedVulkanContextAcquisition.ready(
                SOURCE,
                handles,
                "Borrowed Mojang Vulkan context is ready with graphics queue family " + graphicsQueueFamily + "."
        );
    }

    private static long address(Pointer pointer) {
        return pointer == null ? 0L : pointer.address();
    }

    private static BorrowedVulkanContextAcquisition unavailable(String message) {
        return BorrowedVulkanContextAcquisition.unavailable(SOURCE, message);
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static String exceptionSummary(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }

    private record ProbeValue<T>(T value, String failureMessage) {
        static <T> ProbeValue<T> present(T value) {
            return new ProbeValue<>(value, null);
        }

        static <T> ProbeValue<T> failed(String failureMessage) {
            return new ProbeValue<>(null, failureMessage);
        }

        boolean failed() {
            return this.failureMessage != null && !this.failureMessage.isBlank();
        }
    }
}
