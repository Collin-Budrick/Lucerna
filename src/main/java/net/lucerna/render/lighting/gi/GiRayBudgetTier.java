package net.lucerna.render.lighting.gi;

public enum GiRayBudgetTier {
    DISABLED,
    REUSE_ONLY,
    LOW,
    MEDIUM,
    HIGH;

    public boolean active() {
        return this != DISABLED;
    }

    public boolean requiresTracing() {
        return this == LOW || this == MEDIUM || this == HIGH;
    }
}
