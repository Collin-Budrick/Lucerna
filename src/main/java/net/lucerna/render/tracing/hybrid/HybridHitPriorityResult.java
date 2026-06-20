package net.lucerna.render.tracing.hybrid;

public record HybridHitPriorityResult(
        HybridHitCandidate selectedHit,
        HybridMaterialConsistencyResult materialConsistency,
        HybridHitSourceCounts sourceCounts,
        boolean fallbackActive,
        String fallbackReason,
        String srcStable,
        String srcStableReason,
        String chunkChurnMaterialConsistent,
        String entityMoveMaterialConsistent,
        String fallbackSourceReason,
        boolean realTracedLightingConsumed,
        String rejectedSummary,
        String evidenceBoundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Round 10 hybrid hit resolver is Java contract/status only; native voxel/RT tracing must supply real hits.";
    private static final String DEFAULT_TRACED_LIGHTING_BOUNDARY =
            "realTracedLightingConsumed=false; open until native traced-light output is consumed";

    public HybridHitPriorityResult {
        if (materialConsistency == null) {
            materialConsistency = HybridMaterialConsistencyResult.evaluate(selectedHit);
        }
        if (sourceCounts == null) {
            sourceCounts = HybridHitSourceCounts.empty();
        }
        fallbackReason = clean(fallbackReason, fallbackActive ? "fallback active" : "no fallback");
        srcStable = clean(srcStable, "unknown");
        srcStableReason = clean(srcStableReason, "single-frame resolver result; stress stability not proven");
        chunkChurnMaterialConsistent = clean(chunkChurnMaterialConsistent, "unknown");
        entityMoveMaterialConsistent = clean(entityMoveMaterialConsistent, "unknown");
        fallbackSourceReason = clean(fallbackSourceReason, fallbackReason);
        rejectedSummary = clean(rejectedSummary, "no rejected hybrid hits reported");
        String tracedBoundary = realTracedLightingConsumed
                ? "realTracedLightingConsumed=true from caller-supplied status; controller proof still required"
                : DEFAULT_TRACED_LIGHTING_BOUNDARY;
        evidenceBoundary = clean(evidenceBoundary, DEFAULT_BOUNDARY) + "; " + tracedBoundary;
    }

    public HybridHitPriorityResult(
            HybridHitCandidate selectedHit,
            HybridMaterialConsistencyResult materialConsistency,
            HybridHitSourceCounts sourceCounts,
            boolean fallbackActive,
            String fallbackReason,
            String rejectedSummary,
            String evidenceBoundary
    ) {
        this(
                selectedHit,
                materialConsistency,
                sourceCounts,
                fallbackActive,
                fallbackReason,
                "unknown",
                "single-frame resolver result; stress stability not proven",
                "unknown",
                "unknown",
                fallbackReason,
                false,
                rejectedSummary,
                evidenceBoundary
        );
    }

    public static HybridHitPriorityResult empty(String reason) {
        HybridHitCandidate miss = HybridHitCandidate.miss(reason);
        return new HybridHitPriorityResult(
                miss,
                HybridMaterialConsistencyResult.evaluate(miss),
                HybridHitSourceCounts.empty(),
                true,
                reason,
                "unknown",
                "no candidates; selected source stability not measurable",
                "unknown",
                "unknown",
                reason,
                false,
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
                + " reason=" + this.fallbackReason
                + " fallbackSourceReason=" + this.fallbackSourceReason;
    }

    public String sourceCountsLine() {
        return "Hybrid source counts: " + this.sourceCounts.compactLabel();
    }

    public String materialLine() {
        return "Hybrid material consistency: " + this.materialConsistency.summary();
    }

    public String stressLine() {
        return "Hybrid stress: srcStable=" + this.srcStable
                + " srcReason=" + this.srcStableReason
                + " chunkChurnMaterial=" + this.chunkChurnMaterialConsistent
                + " entityMoveMaterial=" + this.entityMoveMaterialConsistent
                + " fallbackSourceReason=" + this.fallbackSourceReason;
    }

    public String tracedLightingBoundaryLine() {
        return "Hybrid traced lighting boundary: realTracedLightingConsumed="
                + yesNo(this.realTracedLightingConsumed)
                + "; " + (this.realTracedLightingConsumed
                ? "caller reports traced-light consumption, controller proof still required"
                : "open until native traced-light output is consumed");
    }

    public String debugSummary() {
        return "selected=" + this.selectedSourceLabel()
                + ",counts={" + this.sourceCounts.compactLabel() + "}"
                + ",fallback=" + this.fallbackActive
                + ",fallbackSourceReason={" + this.fallbackSourceReason + "}"
                + ",material={" + this.materialConsistency.summary() + "}"
                + ",stress={srcStable=" + this.srcStable
                + ",chunkChurnMaterial=" + this.chunkChurnMaterialConsistent
                + ",entityMoveMaterial=" + this.entityMoveMaterialConsistent
                + ",realTracedLightingConsumed=" + this.realTracedLightingConsumed + "}"
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
