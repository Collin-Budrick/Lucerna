package net.lucerna.render.context;

public record BorrowedVulkanContextAcquisition(
        BorrowedVulkanContextAcquisitionStatus status,
        BorrowedVulkanContextHandles handles,
        BorrowedVulkanContextValidation validation,
        String source,
        String message
) {
    private static final String DEFAULT_SOURCE = "unwired";

    public BorrowedVulkanContextAcquisition {
        if (source == null || source.isBlank()) {
            source = DEFAULT_SOURCE;
        } else {
            source = source.trim();
        }

        if (validation == null) {
            validation = handles == null
                    ? BorrowedVulkanContextValidation.unavailable("No borrowed Vulkan context handles were supplied.")
                    : handles.validateRequiredHandles();
        }

        if (status == null) {
            status = handles == null
                    ? BorrowedVulkanContextAcquisitionStatus.ABSENT
                    : BorrowedVulkanContextAcquisitionStatus.READY;
        }

        if (status == BorrowedVulkanContextAcquisitionStatus.READY
                && (handles == null || !validation.valid())) {
            status = BorrowedVulkanContextAcquisitionStatus.UNAVAILABLE;
        }

        if (message == null || message.isBlank()) {
            message = defaultMessage(status, validation);
        } else {
            message = message.trim();
        }
    }

    public static BorrowedVulkanContextAcquisition absent(String message) {
        return absent(DEFAULT_SOURCE, message);
    }

    public static BorrowedVulkanContextAcquisition absent(String source, String message) {
        return new BorrowedVulkanContextAcquisition(
                BorrowedVulkanContextAcquisitionStatus.ABSENT,
                null,
                BorrowedVulkanContextValidation.unavailable("No borrowed Vulkan context provider is wired."),
                source,
                message
        );
    }

    public static BorrowedVulkanContextAcquisition unavailable(String source, String message) {
        return unavailable(
                source,
                message,
                BorrowedVulkanContextValidation.unavailable(message)
        );
    }

    public static BorrowedVulkanContextAcquisition unavailable(
            String source,
            String message,
            BorrowedVulkanContextValidation validation
    ) {
        return new BorrowedVulkanContextAcquisition(
                BorrowedVulkanContextAcquisitionStatus.UNAVAILABLE,
                null,
                validation,
                source,
                message
        );
    }

    public static BorrowedVulkanContextAcquisition ready(
            String source,
            BorrowedVulkanContextHandles handles,
            String message
    ) {
        return new BorrowedVulkanContextAcquisition(
                BorrowedVulkanContextAcquisitionStatus.READY,
                handles,
                handles == null ? null : handles.validateRequiredHandles(),
                source,
                message
        );
    }

    public boolean ready() {
        return this.status == BorrowedVulkanContextAcquisitionStatus.READY;
    }

    public boolean absent() {
        return this.status == BorrowedVulkanContextAcquisitionStatus.ABSENT;
    }

    public boolean unavailable() {
        return this.status == BorrowedVulkanContextAcquisitionStatus.UNAVAILABLE;
    }

    private static String defaultMessage(
            BorrowedVulkanContextAcquisitionStatus status,
            BorrowedVulkanContextValidation validation
    ) {
        return switch (status) {
            case ABSENT -> "No borrowed Vulkan frame context provider is wired.";
            case UNAVAILABLE -> validation.message();
            case READY -> "Borrowed Vulkan frame context is ready.";
        };
    }
}
