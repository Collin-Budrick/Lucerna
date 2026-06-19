package net.lucerna.render.context;

@FunctionalInterface
public interface BorrowedVulkanContextProbe {
    BorrowedVulkanContextAcquisition acquire(VulkanFrameContextRequest request);

    static BorrowedVulkanContextProbe unwired() {
        return NoFakeBorrowedVulkanContextProbe.instance();
    }
}
