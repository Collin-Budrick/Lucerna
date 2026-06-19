package net.lucerna.render.context;

import java.util.Objects;

public final class GuardedBorrowedVulkanContextAdapter {
    private static final String GUARD_SOURCE = "guarded-frame-context-adapter";

    private final BorrowedVulkanContextProbe probe;

    public GuardedBorrowedVulkanContextAdapter(BorrowedVulkanContextProbe probe) {
        this.probe = Objects.requireNonNullElseGet(probe, BorrowedVulkanContextProbe::unwired);
    }

    public static GuardedBorrowedVulkanContextAdapter unwired() {
        return new GuardedBorrowedVulkanContextAdapter(BorrowedVulkanContextProbe.unwired());
    }

    public BorrowedVulkanContextAcquisition acquire(VulkanFrameContextRequest request) {
        BorrowedVulkanContextAcquisition acquisition;
        try {
            acquisition = this.probe.acquire(request);
        } catch (RuntimeException exception) {
            return BorrowedVulkanContextAcquisition.unavailable(
                    GUARD_SOURCE,
                    "Borrowed Vulkan context probe threw "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }

        if (acquisition == null) {
            return BorrowedVulkanContextAcquisition.unavailable(
                    GUARD_SOURCE,
                    "Borrowed Vulkan context probe returned no acquisition result."
            );
        }

        BorrowedVulkanContextHandles handles = acquisition.handles();
        BorrowedVulkanContextValidation validation = handles == null
                ? acquisition.validation()
                : handles.validateRequiredHandles();

        if (acquisition.ready() && (handles == null || !validation.valid())) {
            return BorrowedVulkanContextAcquisition.unavailable(
                    acquisition.source(),
                    validation.message(),
                    validation
            );
        }

        if (acquisition.ready()) {
            return BorrowedVulkanContextAcquisition.ready(
                    acquisition.source(),
                    handles,
                    acquisition.message()
            );
        }

        return new BorrowedVulkanContextAcquisition(
                acquisition.status(),
                handles,
                validation,
                acquisition.source(),
                acquisition.message()
        );
    }
}
