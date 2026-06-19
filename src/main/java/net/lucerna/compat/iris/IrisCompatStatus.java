package net.lucerna.compat.iris;

public record IrisCompatStatus(
        boolean installed,
        boolean disableAttempted,
        boolean shadersDisabledForLucerna,
        ShaderPackState shaderPackState,
        String userMessage
) {
    public IrisCompatStatus {
        if (shaderPackState == null) {
            shaderPackState = installed ? ShaderPackState.UNKNOWN : ShaderPackState.NOT_INSTALLED;
        }

        if (userMessage == null || userMessage.isBlank()) {
            userMessage = "Iris compatibility status is unavailable.";
        } else {
            userMessage = userMessage.trim();
        }
    }

    public static IrisCompatStatus notInstalled() {
        return new IrisCompatStatus(false, false, false, ShaderPackState.NOT_INSTALLED, "Iris is not installed.");
    }

    public static IrisCompatStatus installedPendingDisable(ShaderPackState shaderPackState) {
        String message = shaderPackState == ShaderPackState.ENABLED
                ? "Iris is installed; active shader pack will be disabled while Lucerna is active."
                : "Iris is installed; shader packs will be disabled while Lucerna is active.";
        return new IrisCompatStatus(
                true,
                false,
                false,
                shaderPackState,
                message
        );
    }

    public static IrisCompatStatus disabledForLucerna(ShaderPackState shaderPackState) {
        String message = shaderPackState == ShaderPackState.UNKNOWN
                ? "Iris is installed; Lucerna disabled shader packs, but current Iris state is unknown."
                : "Iris is installed; shader packs are disabled by Lucerna.";
        return new IrisCompatStatus(
                true,
                true,
                true,
                shaderPackState,
                message
        );
    }

    public static IrisCompatStatus needsDisableReapply(ShaderPackState shaderPackState) {
        return new IrisCompatStatus(
                true,
                true,
                false,
                shaderPackState,
                "Iris is installed; shader packs will be disabled again while Lucerna is active."
        );
    }

    public static IrisCompatStatus disableFailed(ShaderPackState shaderPackState, String message) {
        return new IrisCompatStatus(true, true, false, shaderPackState, message);
    }

    public enum ShaderPackState {
        NOT_INSTALLED,
        ENABLED,
        DISABLED,
        UNKNOWN
    }
}
