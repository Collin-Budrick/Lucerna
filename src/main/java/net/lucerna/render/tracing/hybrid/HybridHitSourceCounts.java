package net.lucerna.render.tracing.hybrid;

import java.util.Collection;

public record HybridHitSourceCounts(
        int screenSpaceHits,
        int voxelHits,
        int hardwareRtHits,
        int skyFallbacks,
        int misses,
        int unavailableVoxelHits,
        int unavailableHardwareRtHits
) {
    public HybridHitSourceCounts {
        screenSpaceHits = Math.max(0, screenSpaceHits);
        voxelHits = Math.max(0, voxelHits);
        hardwareRtHits = Math.max(0, hardwareRtHits);
        skyFallbacks = Math.max(0, skyFallbacks);
        misses = Math.max(0, misses);
        unavailableVoxelHits = Math.max(0, unavailableVoxelHits);
        unavailableHardwareRtHits = Math.max(0, unavailableHardwareRtHits);
    }

    public static HybridHitSourceCounts empty() {
        return new HybridHitSourceCounts(0, 0, 0, 0, 0, 0, 0);
    }

    public static HybridHitSourceCounts fromCandidates(Collection<HybridHitCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return empty();
        }
        int screenSpace = 0;
        int voxel = 0;
        int hardwareRt = 0;
        int sky = 0;
        int miss = 0;
        int unavailableVoxel = 0;
        int unavailableHardwareRt = 0;
        for (HybridHitCandidate candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            switch (candidate.source()) {
                case SCREEN_SPACE -> screenSpace += candidate.hit() ? 1 : 0;
                case VOXEL -> {
                    voxel += candidate.hit() ? 1 : 0;
                    unavailableVoxel += candidate.hit() && !candidate.sourceAvailable() ? 1 : 0;
                }
                case HARDWARE_RT -> {
                    hardwareRt += candidate.hit() ? 1 : 0;
                    unavailableHardwareRt += candidate.hit() && !candidate.sourceAvailable() ? 1 : 0;
                }
                case SKY -> sky++;
                case MISS -> miss++;
            }
        }
        return new HybridHitSourceCounts(
                screenSpace,
                voxel,
                hardwareRt,
                sky,
                miss,
                unavailableVoxel,
                unavailableHardwareRt
        );
    }

    public int surfaceHits() {
        return this.screenSpaceHits + this.voxelHits + this.hardwareRtHits;
    }

    public int availableSurfaceHits() {
        return Math.max(0, this.surfaceHits() - this.unavailableVoxelHits - this.unavailableHardwareRtHits);
    }

    public int fallbackHits() {
        return this.skyFallbacks + this.misses;
    }

    public String compactLabel() {
        return "screen=" + this.screenSpaceHits
                + ",voxel=" + this.voxelHits
                + ",rt=" + this.hardwareRtHits
                + ",surface=" + this.surfaceHits()
                + ",availableSurface=" + this.availableSurfaceHits()
                + ",sky=" + this.skyFallbacks
                + ",miss=" + this.misses
                + ",fallback=" + this.fallbackHits()
                + ",voxelUnavailable=" + this.unavailableVoxelHits
                + ",rtUnavailable=" + this.unavailableHardwareRtHits;
    }
}
