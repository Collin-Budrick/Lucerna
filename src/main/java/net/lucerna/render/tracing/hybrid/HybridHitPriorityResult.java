package net.lucerna.render.tracing.hybrid;

public record HybridHitPriorityResult(
        HybridHitCandidate selectedHit,
        HybridMaterialConsistencyResult materialConsistency,
        HybridHitSourceCounts sourceCounts,
        boolean fallbackActive,
        String fallbackReason,
        String rejectedSummary,
        String evidenceBoundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Round 10 hybrid hit resolver is Java contract/status only; native voxel/RT tracing must supply real hits.";

    public HybridHitPriorityResult {
        if (materialConsistency == null) {
            materialConsistency = HybridMaterialConsistencyResult.evaluate(selectedHit);
        }
        if (sourceCounts == null) {
            sourceCounts = HybridHitSourceCounts.empty();
        }
        fallbackReason = clean(fallbackReason, fallbackActive ? "fallback active" : "no fallback");
        rejectedSummary = clean(rejectedSummary, "no rejected hybrid hits reported");
        evidenceBoundary = clean(evidenceBoundary, DEFAULT_BOUNDARY);
    }

    public static HybridHitPriorityResult empty(String reason) {
        HybridHitCandidate miss = HybridHitCandidate.miss(reason);
        return new HybridHitPriorityResult(
                miss,
                HybridMaterialConsistencyResult.evaluate(miss),
                HybridHitSourceCounts.empty(),
                true,
                reason,
                "no candidates",
                DEFAULT_BOUNDARY
        );
    }

    public HybridHitSource selectedSource() {
        return this.selectedHit == null ? HybridHitSource.MISS : this.selectedHit.source();
    }

    public String selectedSourceLabel() {
        return this.selectedSource().telemetryKey();
    }

    public String priorityLine() {
        return "Hybrid priority: selected=" + this.selectedSourceLabel()
                + " fallback=" + yesNo(this.fallbackActive)
                + " material=" + yesNo(this.materialConsistency.consistent())
                + " reason=" + this.fallbackReason;
    }

    public String sourceCountsLine() {
        return "Hybrid source counts: " + this.sourceCounts.compactLabel();
    }

    public String materialLine() {
        return "Hybrid material consistency: " + this.materialConsistency.summary();
    }

    public String debugSummary() {
        return "selected=" + this.selectedSourceLabel()
                + ",counts={" + this.sourceCounts.compactLabel() + "}"
                + ",fallback=" + this.fallbackActive
                + ",material={" + this.materialConsistency.summary() + "}"
                + ",rejected={" + this.rejectedSummary + "}";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
