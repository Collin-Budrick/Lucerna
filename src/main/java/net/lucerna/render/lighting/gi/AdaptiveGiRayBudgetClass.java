package net.lucerna.render.lighting.gi;

public enum AdaptiveGiRayBudgetClass {
    STABLE_REUSE(GiRayBudgetTier.REUSE_ONLY, "stable-reuse"),
    STABLE_REFRESH(GiRayBudgetTier.LOW, "stable-refresh"),
    FIXED_MEDIUM(GiRayBudgetTier.MEDIUM, "fixed-medium"),
    CACHE_CONFIDENCE(GiRayBudgetTier.MEDIUM, "cache-confidence"),
    LOW_CONFIDENCE(GiRayBudgetTier.HIGH, "low-confidence"),
    VARIANCE_REFRESH(GiRayBudgetTier.MEDIUM, "variance-refresh"),
    NOISY(GiRayBudgetTier.HIGH, "noisy"),
    EMISSIVE(GiRayBudgetTier.HIGH, "emissive"),
    DIRTY(GiRayBudgetTier.HIGH, "dirty");

    private final GiRayBudgetTier tier;
    private final String label;

    AdaptiveGiRayBudgetClass(GiRayBudgetTier tier, String label) {
        this.tier = tier;
        this.label = label;
    }

    public GiRayBudgetTier tier() {
        return this.tier;
    }

    public String label() {
        return this.label;
    }
}
