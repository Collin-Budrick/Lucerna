package net.lucerna.compat;

public record BackendStatus(BackendKind kind, boolean active, String backendName, String userMessage) {
    private static final String UNKNOWN_BACKEND = "unknown";

    public BackendStatus {
        backendName = normalizeBackendName(backendName);
        userMessage = normalizeMessage(userMessage);
    }

    public static BackendStatus active(BackendKind kind, String backendName, String userMessage) {
        return new BackendStatus(kind, true, backendName, userMessage);
    }

    public static BackendStatus disabled(BackendKind kind, String userMessage) {
        return new BackendStatus(kind, false, UNKNOWN_BACKEND, userMessage);
    }

    public static BackendStatus disabled(BackendKind kind, String backendName, String userMessage) {
        return new BackendStatus(kind, false, backendName, userMessage);
    }

    public boolean disabled() {
        return !this.active;
    }

    public boolean sodiumVulkan() {
        return this.active && this.kind == BackendKind.SODIUM_VULKAN;
    }

    public String disabledReason() {
        return this.active ? "" : this.userMessage;
    }

    public String diagnosticSummary() {
        return "%s, backend=%s, active=%s, message=%s".formatted(this.kind, this.backendName, this.active, this.userMessage);
    }

    private static String normalizeBackendName(String backendName) {
        if (backendName == null || backendName.isBlank()) {
            return UNKNOWN_BACKEND;
        }

        return backendName.trim();
    }

    private static String normalizeMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "Lucerna renderer status is unavailable.";
        }

        return userMessage.trim();
    }
}
