package net.lucerna.render.tracing.rt;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record VulkanRtCapabilityStatus(
        boolean sodiumVulkanBackendActive,
        Map<VulkanRtExtension, Boolean> extensionAvailability,
        boolean accelerationStructureAvailable,
        boolean rayTracingPipelineAvailable,
        boolean bufferDeviceAddressAvailable,
        boolean descriptorIndexingAvailable,
        boolean deferredHostOperationsAvailable,
        String detectionSource,
        String fallbackReason,
        String evidenceBoundary
) {
    private static final String DEFAULT_SOURCE = "unwired";
    private static final String DEFAULT_BOUNDARY =
            "Round 10 RT status is capability metadata only; no hardware RT execution is claimed.";

    public VulkanRtCapabilityStatus {
        extensionAvailability = copyAvailability(extensionAvailability);
        accelerationStructureAvailable = accelerationStructureAvailable
                || available(extensionAvailability, VulkanRtExtension.ACCELERATION_STRUCTURE);
        rayTracingPipelineAvailable = rayTracingPipelineAvailable
                || available(extensionAvailability, VulkanRtExtension.RAY_TRACING_PIPELINE);
        bufferDeviceAddressAvailable = bufferDeviceAddressAvailable
                || available(extensionAvailability, VulkanRtExtension.BUFFER_DEVICE_ADDRESS);
        descriptorIndexingAvailable = descriptorIndexingAvailable
                || available(extensionAvailability, VulkanRtExtension.DESCRIPTOR_INDEXING);
        deferredHostOperationsAvailable = deferredHostOperationsAvailable
                || available(extensionAvailability, VulkanRtExtension.DEFERRED_HOST_OPERATIONS);
        detectionSource = clean(detectionSource, DEFAULT_SOURCE);
        fallbackReason = clean(fallbackReason, defaultFallbackReason(
                sodiumVulkanBackendActive,
                accelerationStructureAvailable,
                rayTracingPipelineAvailable,
                bufferDeviceAddressAvailable,
                descriptorIndexingAvailable,
                deferredHostOperationsAvailable
        ));
        evidenceBoundary = clean(evidenceBoundary, DEFAULT_BOUNDARY);
    }

    public static VulkanRtCapabilityStatus unavailable(String source, String fallbackReason) {
        return new VulkanRtCapabilityStatus(
                false,
                Map.of(),
                false,
                false,
                false,
                false,
                false,
                source,
                fallbackReason,
                DEFAULT_BOUNDARY
        );
    }

    public static VulkanRtCapabilityStatus detected(
            boolean sodiumVulkanBackendActive,
            Map<VulkanRtExtension, Boolean> extensionAvailability,
            String detectionSource
    ) {
        return new VulkanRtCapabilityStatus(
                sodiumVulkanBackendActive,
                extensionAvailability,
                false,
                false,
                false,
                false,
                false,
                detectionSource,
                null,
                DEFAULT_BOUNDARY
        );
    }

    public boolean hardwareRayTracingReady() {
        return this.sodiumVulkanBackendActive
                && this.accelerationStructureAvailable
                && this.rayTracingPipelineAvailable
                && this.bufferDeviceAddressAvailable
                && this.descriptorIndexingAvailable
                && this.deferredHostOperationsAvailable;
    }

    public boolean fallbackActive() {
        return !this.hardwareRayTracingReady();
    }

    public String summary() {
        return "rtReady=" + this.hardwareRayTracingReady()
                + ",backend=" + this.sodiumVulkanBackendActive
                + ",as=" + this.accelerationStructureAvailable
                + ",pipeline=" + this.rayTracingPipelineAvailable
                + ",bda=" + this.bufferDeviceAddressAvailable
                + ",descriptorIndexing=" + this.descriptorIndexingAvailable
                + ",deferredHostOps=" + this.deferredHostOperationsAvailable
                + ",source=" + this.detectionSource;
    }

    private static Map<VulkanRtExtension, Boolean> copyAvailability(
            Map<VulkanRtExtension, Boolean> extensionAvailability
    ) {
        EnumMap<VulkanRtExtension, Boolean> copy = new EnumMap<>(VulkanRtExtension.class);
        if (extensionAvailability != null) {
            for (Map.Entry<VulkanRtExtension, Boolean> entry : extensionAvailability.entrySet()) {
                VulkanRtExtension extension = Objects.requireNonNull(entry.getKey(), "extension key");
                copy.put(extension, Boolean.TRUE.equals(entry.getValue()));
            }
        }
        return Map.copyOf(copy);
    }

    private static boolean available(Map<VulkanRtExtension, Boolean> availability, VulkanRtExtension extension) {
        return Boolean.TRUE.equals(availability.get(extension));
    }

    private static String defaultFallbackReason(
            boolean sodiumVulkanBackendActive,
            boolean accelerationStructureAvailable,
            boolean rayTracingPipelineAvailable,
            boolean bufferDeviceAddressAvailable,
            boolean descriptorIndexingAvailable,
            boolean deferredHostOperationsAvailable
    ) {
        if (!sodiumVulkanBackendActive) {
            return "Sodium Vulkan backend is not active; RT path remains on non-RT fallback.";
        }
        if (!accelerationStructureAvailable) {
            return "VK_KHR_acceleration_structure is unavailable; RT path remains on non-RT fallback.";
        }
        if (!rayTracingPipelineAvailable) {
            return "VK_KHR_ray_tracing_pipeline is unavailable; RT path remains on non-RT fallback.";
        }
        if (!bufferDeviceAddressAvailable) {
            return "VK_KHR_buffer_device_address is unavailable; RT path remains on non-RT fallback.";
        }
        if (!descriptorIndexingAvailable) {
            return "VK_EXT_descriptor_indexing is unavailable; RT path remains on non-RT fallback.";
        }
        if (!deferredHostOperationsAvailable) {
            return "VK_KHR_deferred_host_operations is unavailable; RT path remains on non-RT fallback.";
        }
        return "Required Vulkan RT extensions are reported available; execution still requires native proof.";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
