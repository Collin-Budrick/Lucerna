package net.lucerna.render.context;

import java.util.ArrayList;
import java.util.List;

public record BorrowedVulkanContextHandles(
        long instance,
        long physicalDevice,
        long device,
        long graphicsQueue,
        int graphicsQueueFamily
) {
    public static BorrowedVulkanContextHandles required(
            long instance,
            long physicalDevice,
            long device,
            long graphicsQueue,
            int graphicsQueueFamily
    ) {
        return new BorrowedVulkanContextHandles(
                instance,
                physicalDevice,
                device,
                graphicsQueue,
                graphicsQueueFamily
        );
    }

    public BorrowedVulkanContextValidation validateRequiredHandles() {
        List<String> missing = new ArrayList<>();
        if (this.instance == 0L) {
            missing.add("instance");
        }
        if (this.physicalDevice == 0L) {
            missing.add("physicalDevice");
        }
        if (this.device == 0L) {
            missing.add("device");
        }
        if (this.graphicsQueue == 0L) {
            missing.add("graphicsQueue");
        }
        if (this.graphicsQueueFamily < 0) {
            missing.add("graphicsQueueFamily");
        }

        if (missing.isEmpty()) {
            return BorrowedVulkanContextValidation.passed();
        }
        return BorrowedVulkanContextValidation.missing(missing);
    }

    public boolean hasRequiredHandles() {
        return this.validateRequiredHandles().valid();
    }
}
