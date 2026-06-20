package net.lucerna.render.tracing.rt;

public enum VulkanRtExtension {
    ACCELERATION_STRUCTURE("VK_KHR_acceleration_structure"),
    RAY_TRACING_PIPELINE("VK_KHR_ray_tracing_pipeline"),
    DEFERRED_HOST_OPERATIONS("VK_KHR_deferred_host_operations"),
    BUFFER_DEVICE_ADDRESS("VK_KHR_buffer_device_address"),
    DESCRIPTOR_INDEXING("VK_EXT_descriptor_indexing");

    private final String vulkanName;

    VulkanRtExtension(String vulkanName) {
        this.vulkanName = vulkanName;
    }

    public String vulkanName() {
        return this.vulkanName;
    }
}
