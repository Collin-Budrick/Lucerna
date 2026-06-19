package net.lucerna.render.context;

public enum BorrowedVulkanContextAcquisitionStatus {
    ABSENT("No borrowed Vulkan frame context provider is wired."),
    UNAVAILABLE("A borrowed Vulkan frame context provider was queried, but no valid context was available."),
    READY("A borrowed Vulkan frame context with the required handles is available.");

    private final String description;

    BorrowedVulkanContextAcquisitionStatus(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
