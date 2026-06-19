package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record DiffuseGiSourceSummary(
        long generation,
        long directLightingGeneration,
        long worldGeneration,
        long materialGeneration,
        long sectionGeneration,
        long dirtyRegionGeneration,
        boolean directLightingReady,
        boolean worldInputAvailable,
        boolean materialInputAvailable,
        boolean sectionInputAvailable,
        int celestialLightCount,
        int emissiveLightCount,
        int shadowCandidateCount,
        int budgetedShadowCandidateCount,
        int sectionSnapshotCount,
        int dirtyRegionCount,
        int materialUpdateCount,
        String debugLabel
) {
    public DiffuseGiSourceSummary {
        generation = maxNonNegative(
                generation,
                directLightingGeneration,
                worldGeneration,
                materialGeneration,
                sectionGeneration,
                dirtyRegionGeneration
        );
        directLightingGeneration = Math.max(0L, directLightingGeneration);
        worldGeneration = Math.max(0L, worldGeneration);
        materialGeneration = Math.max(0L, materialGeneration);
        sectionGeneration = Math.max(0L, sectionGeneration);
        dirtyRegionGeneration = Math.max(0L, dirtyRegionGeneration);
        celestialLightCount = Math.max(0, celestialLightCount);
        emissiveLightCount = Math.max(0, emissiveLightCount);
        shadowCandidateCount = Math.max(0, shadowCandidateCount);
        budgetedShadowCandidateCount = Math.max(0, budgetedShadowCandidateCount);
        sectionSnapshotCount = Math.max(0, sectionSnapshotCount);
        dirtyRegionCount = Math.max(0, dirtyRegionCount);
        materialUpdateCount = Math.max(0, materialUpdateCount);
        debugLabel = clean(debugLabel, defaultLabel(
                directLightingReady,
                worldInputAvailable,
                materialInputAvailable,
                sectionInputAvailable,
                emissiveLightCount,
                dirtyRegionCount
        ));
    }

    public static DiffuseGiSourceSummary unavailable() {
        return new DiffuseGiSourceSummary(
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "GI source inputs unavailable"
        );
    }

    public static DiffuseGiSourceSummary of(
            long directLightingGeneration,
            long worldGeneration,
            long materialGeneration,
            long sectionGeneration,
            long dirtyRegionGeneration,
            boolean directLightingReady,
            int celestialLightCount,
            int emissiveLightCount,
            int shadowCandidateCount,
            int budgetedShadowCandidateCount,
            int sectionSnapshotCount,
            int dirtyRegionCount,
            int materialUpdateCount,
            String debugLabel
    ) {
        return new DiffuseGiSourceSummary(
                0L,
                directLightingGeneration,
                worldGeneration,
                materialGeneration,
                sectionGeneration,
                dirtyRegionGeneration,
                directLightingReady,
                worldGeneration > 0L || dirtyRegionCount > 0,
                materialGeneration > 0L || materialUpdateCount > 0,
                sectionGeneration > 0L || sectionSnapshotCount > 0,
                celestialLightCount,
                emissiveLightCount,
                shadowCandidateCount,
                budgetedShadowCandidateCount,
                sectionSnapshotCount,
                dirtyRegionCount,
                materialUpdateCount,
                debugLabel
        );
    }

    public boolean hasDirectLightingWork() {
        return this.directLightingReady
                && (this.celestialLightCount > 0 || this.emissiveLightCount > 0 || this.shadowCandidateCount > 0);
    }

    public boolean hasWorldMaterialInputs() {
        return this.worldInputAvailable && this.materialInputAvailable && this.sectionInputAvailable;
    }

    public String compactLabel() {
        return "direct=" + (this.directLightingReady ? "ready" : "pending")
                + " emissive=" + this.emissiveLightCount
                + " shadows=" + this.shadowCandidateCount + "/" + this.budgetedShadowCandidateCount
                + " sections=" + this.sectionSnapshotCount
                + " dirty=" + this.dirtyRegionCount;
    }

    private static long maxNonNegative(long first, long second, long third, long fourth, long fifth, long sixth) {
        return Math.max(0L, Math.max(Math.max(first, second), Math.max(Math.max(third, fourth), Math.max(fifth, sixth))));
    }

    private static String defaultLabel(
            boolean directLightingReady,
            boolean worldInputAvailable,
            boolean materialInputAvailable,
            boolean sectionInputAvailable,
            int emissiveLightCount,
            int dirtyRegionCount
    ) {
        return "direct=" + (directLightingReady ? "ready" : "pending")
                + " world=" + (worldInputAvailable ? "available" : "missing")
                + " material=" + (materialInputAvailable ? "available" : "missing")
                + " sections=" + (sectionInputAvailable ? "available" : "missing")
                + " emissive=" + emissiveLightCount
                + " dirty=" + dirtyRegionCount;
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI source inputs");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
