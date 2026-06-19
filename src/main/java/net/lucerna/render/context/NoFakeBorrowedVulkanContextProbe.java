package net.lucerna.render.context;

public final class NoFakeBorrowedVulkanContextProbe implements BorrowedVulkanContextProbe {
    private static final NoFakeBorrowedVulkanContextProbe INSTANCE = new NoFakeBorrowedVulkanContextProbe();

    private NoFakeBorrowedVulkanContextProbe() {
    }

    public static NoFakeBorrowedVulkanContextProbe instance() {
        return INSTANCE;
    }

    @Override
    public BorrowedVulkanContextAcquisition acquire(VulkanFrameContextRequest request) {
        return BorrowedVulkanContextAcquisition.absent(
                "no-fake-frame-context-probe",
                "No Sodium or Mojang Vulkan frame context adapter is wired; Lucerna will not synthesize fake Vulkan handles."
        );
    }
}
