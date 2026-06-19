package net.lucerna.render.context;

import java.util.List;

public record BorrowedVulkanContextValidation(
        boolean valid,
        List<String> missingRequiredHandles,
        String message
) {
    public BorrowedVulkanContextValidation {
        if (missingRequiredHandles == null) {
            missingRequiredHandles = List.of();
        } else {
            missingRequiredHandles = List.copyOf(missingRequiredHandles);
        }

        if (!missingRequiredHandles.isEmpty()) {
            valid = false;
        }

        if (message == null || message.isBlank()) {
            message = valid
                    ? "Borrowed Vulkan context handles passed Java-side validation."
                    : "Borrowed Vulkan context handles are incomplete.";
        } else {
            message = message.trim();
        }
    }

    public static BorrowedVulkanContextValidation passed() {
        return new BorrowedVulkanContextValidation(
                true,
                List.of(),
                "Borrowed Vulkan context handles passed Java-side validation."
        );
    }

    public static BorrowedVulkanContextValidation missing(List<String> missingRequiredHandles) {
        List<String> missing = missingRequiredHandles == null ? List.of() : List.copyOf(missingRequiredHandles);
        return new BorrowedVulkanContextValidation(
                false,
                missing,
                missing.isEmpty()
                        ? "Borrowed Vulkan context handles are incomplete."
                        : "Borrowed Vulkan context is missing required handles: " + String.join(", ", missing)
        );
    }

    public static BorrowedVulkanContextValidation unavailable(String message) {
        return new BorrowedVulkanContextValidation(false, List.of(), message);
    }
}
