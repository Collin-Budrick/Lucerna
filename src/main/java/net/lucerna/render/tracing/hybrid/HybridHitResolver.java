package net.lucerna.render.tracing.hybrid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HybridHitResolver {
    private HybridHitResolver() {
    }

    public static HybridHitPriorityResult resolve(
            List<HybridHitCandidate> candidates,
            boolean voxelPathAvailable,
            boolean hardwareRtPathAvailable
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return HybridHitPriorityResult.empty("no hybrid hit candidates supplied");
        }

        HybridHitSourceCounts counts = HybridHitSourceCounts.fromCandidates(candidates);
        List<String> rejected = new ArrayList<>();
        List<HybridHitCandidate> eligibleSurfaceHits = new ArrayList<>();
        HybridHitCandidate fallback = null;

        for (HybridHitCandidate candidate : candidates) {
            if (candidate == null) {
                rejected.add("null-candidate");
                continue;
            }
            if (candidate.source() == HybridHitSource.VOXEL && !voxelPathAvailable) {
                rejected.add("voxel-unavailable:" + candidate.fallbackReason());
                continue;
            }
            if (candidate.source() == HybridHitSource.HARDWARE_RT && !hardwareRtPathAvailable) {
                rejected.add("hardware-rt-unavailable:" + candidate.fallbackReason());
                continue;
            }
            if (candidate.source() == HybridHitSource.SKY || candidate.source() == HybridHitSource.MISS) {
                fallback = preferFallback(fallback, candidate);
                continue;
            }
            HybridMaterialConsistencyResult material = HybridMaterialConsistencyResult.evaluate(candidate);
            if (!candidate.usableSurfaceHit()) {
                rejected.add(candidate.source().telemetryKey() + "-not-usable:" + candidate.fallbackReason());
            } else if (material.checked() && !material.consistent()) {
                rejected.add(candidate.source().telemetryKey() + "-material-mismatch");
            } else {
                eligibleSurfaceHits.add(candidate);
            }
        }

        if (!eligibleSurfaceHits.isEmpty()) {
            HybridHitCandidate selected = eligibleSurfaceHits.stream()
                    .max(Comparator
                            .comparingInt(HybridHitResolver::priorityScore)
                            .thenComparingDouble(candidate -> -candidate.distance()))
                    .orElseThrow();
            boolean fallbackActive = !voxelPathAvailable || !hardwareRtPathAvailable;
            String fallbackReason = fallbackReason(voxelPathAvailable, hardwareRtPathAvailable, selected);
            return new HybridHitPriorityResult(
                    selected,
                    HybridMaterialConsistencyResult.evaluate(selected),
                    counts,
                    fallbackActive,
                    fallbackReason,
                    rejectedSummary(rejected),
                    "Round 10 priority rule: hardware RT > voxel > screen-space for consistent surface hits; unavailable RT/voxel paths are explicit fallbacks."
            );
        }

        HybridHitCandidate selectedFallback = fallback == null
                ? HybridHitCandidate.miss("no eligible surface hit after hybrid priority filtering")
                : fallback;
        return new HybridHitPriorityResult(
                selectedFallback,
                HybridMaterialConsistencyResult.evaluate(selectedFallback),
                counts,
                true,
                fallbackReason(voxelPathAvailable, hardwareRtPathAvailable, selectedFallback),
                rejectedSummary(rejected),
                "Round 10 priority rule: sky/miss is selected only after all surface sources are unavailable, missing, or material-inconsistent."
        );
    }

    private static int priorityScore(HybridHitCandidate candidate) {
        int sourcePriority = candidate.source().priority() * 1000;
        int opacityPriority = candidate.opaque() ? 100 : 0;
        int emissivePriority = candidate.emissive() ? 10 : 0;
        return sourcePriority + opacityPriority + emissivePriority;
    }

    private static HybridHitCandidate preferFallback(HybridHitCandidate current, HybridHitCandidate candidate) {
        if (current == null) {
            return candidate;
        }
        if (current.source() == HybridHitSource.MISS && candidate.source() == HybridHitSource.SKY) {
            return candidate;
        }
        return current;
    }

    private static String fallbackReason(
            boolean voxelPathAvailable,
            boolean hardwareRtPathAvailable,
            HybridHitCandidate selected
    ) {
        List<String> reasons = new ArrayList<>();
        if (!voxelPathAvailable) {
            reasons.add("voxel path unavailable");
        }
        if (!hardwareRtPathAvailable) {
            reasons.add("hardware RT path unavailable");
        }
        if (selected == null || selected.source() == HybridHitSource.MISS) {
            reasons.add("selected miss");
        } else if (selected.source() == HybridHitSource.SKY) {
            reasons.add("selected sky");
        } else if (reasons.isEmpty()) {
            reasons.add("selected " + selected.source().telemetryKey() + " by priority");
        }
        return String.join("; ", reasons);
    }

    private static String rejectedSummary(List<String> rejected) {
        if (rejected == null || rejected.isEmpty()) {
            return "none";
        }
        return String.join(",", rejected);
    }
}
