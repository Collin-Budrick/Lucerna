package net.lucerna.render.lighting.gi;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionSnapshot;
import net.lucerna.world.DirtyRegionSnapshotMetadata;
import net.lucerna.world.DirtyRegionType;

import java.util.List;
import java.util.Objects;

public final class GiCacheSnapshotBuilder {
    private DirtyRegionBatch dirtyRegions = DirtyRegionBatch.empty();
    private int sourceDirtyRegionCount;
    private int pendingDirtyRegionCountAfterDrain;
    private long cacheGenerationHint;
    private long worldGenerationHint;
    private String reasonHint;

    private GiCacheSnapshotBuilder() {
    }

    public static GiCacheSnapshotBuilder create() {
        return new GiCacheSnapshotBuilder();
    }

    public static GiCacheSnapshotBuilder from(DirtyRegionSnapshot dirtyRegionSnapshot) {
        return create().dirtyRegions(dirtyRegionSnapshot);
    }

    public static GiCacheSnapshotBuilder from(DirtyRegionBatch dirtyRegionBatch) {
        return create().dirtyRegions(dirtyRegionBatch);
    }

    public GiCacheSnapshotBuilder dirtyRegions(DirtyRegionSnapshot dirtyRegionSnapshot) {
        DirtyRegionSnapshot resolvedSnapshot = dirtyRegionSnapshot == null
                ? DirtyRegionSnapshot.empty()
                : dirtyRegionSnapshot;
        DirtyRegionSnapshotMetadata metadata = resolvedSnapshot.metadata();
        this.dirtyRegions = resolvedSnapshot.batch();
        this.sourceDirtyRegionCount = metadata.sourceRegionCount();
        this.pendingDirtyRegionCountAfterDrain = resolvedSnapshot.pendingRegionCountAfterDrain();
        return this;
    }

    public GiCacheSnapshotBuilder dirtyRegions(DirtyRegionBatch dirtyRegionBatch) {
        DirtyRegionBatch resolvedBatch = dirtyRegionBatch == null
                ? DirtyRegionBatch.empty()
                : dirtyRegionBatch;
        this.sourceDirtyRegionCount = resolvedBatch.dirtyRegionCount();
        this.dirtyRegions = resolvedBatch.coalesced();
        this.pendingDirtyRegionCountAfterDrain = 0;
        return this;
    }

    public GiCacheSnapshotBuilder cacheGenerationHint(long cacheGenerationHint) {
        this.cacheGenerationHint = Math.max(0L, cacheGenerationHint);
        return this;
    }

    public GiCacheSnapshotBuilder worldGenerationHint(long worldGenerationHint) {
        this.worldGenerationHint = Math.max(0L, worldGenerationHint);
        return this;
    }

    public GiCacheSnapshotBuilder reason(String reasonHint) {
        this.reasonHint = reasonHint;
        return this;
    }

    public GiCacheSnapshot build() {
        return GiCacheSnapshot.from(
                this.resolvedCacheGeneration(),
                this.dirtyRegions,
                List.of(),
                List.of()
        );
    }

    public GiCachePlannerInputs buildPlannerInputs() {
        GiCacheSnapshot snapshot = this.build();
        CacheConfidence confidence = this.estimateConfidence(snapshot);
        return new GiCachePlannerInputs(
                snapshot,
                confidence,
                this.estimateRayBudgetPressure(snapshot, confidence),
                this.sourceDirtyRegionCount,
                snapshot.dirtyRegionCount(),
                this.pendingDirtyRegionCountAfterDrain,
                this.reason(snapshot, confidence)
        );
    }

    private long resolvedCacheGeneration() {
        return Math.max(
                Math.max(this.cacheGenerationHint, this.worldGenerationHint),
                this.dirtyRegions.lastGeneration()
        );
    }

    private CacheConfidence estimateConfidence(GiCacheSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.hasDirtyRegions()) {
            return new CacheConfidence(
                    0.0F,
                    this.dirtyVariancePressure(),
                    0,
                    Math.max(snapshot.cacheGeneration(), snapshot.latestDirtyGeneration()),
                    0L,
                    true,
                    this.reason(snapshot, null)
            );
        }
        if (snapshot.isEmpty()) {
            return new CacheConfidence(
                    0.0F,
                    1.0F,
                    0,
                    snapshot.cacheGeneration(),
                    0L,
                    false,
                    this.reason(snapshot, null)
            );
        }
        return snapshot.combinedConfidence();
    }

    private GiRayBudgetTier estimateRayBudgetPressure(GiCacheSnapshot snapshot, CacheConfidence confidence) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(confidence, "confidence");
        AdaptiveGiRayBudgetConfig config = AdaptiveGiRayBudgetConfig.firstMilestone();
        if (snapshot.hasDirtyRegions() || snapshot.isEmpty() || confidence.dirty()) {
            return GiRayBudgetTier.HIGH;
        }
        if (confidence.variance() >= config.highVarianceThreshold()) {
            return GiRayBudgetTier.HIGH;
        }
        if (confidence.confidence() < config.lowConfidenceThreshold()) {
            return GiRayBudgetTier.HIGH;
        }
        if (confidence.variance() >= config.mediumVarianceThreshold()) {
            return GiRayBudgetTier.MEDIUM;
        }
        if (confidence.confidence() >= config.reuseConfidenceThreshold()) {
            return GiRayBudgetTier.REUSE_ONLY;
        }
        return GiRayBudgetTier.LOW;
    }

    private float dirtyVariancePressure() {
        if (this.dirtyRegions.isEmpty()) {
            return 1.0F;
        }

        float typePressure = 0.0F;
        for (DirtyRegion dirtyRegion : this.dirtyRegions.regions()) {
            typePressure = Math.max(typePressure, typePressure(dirtyRegion.type()));
        }

        int totalPendingDirtyRegions = this.dirtyRegions.dirtyRegionCount() + this.pendingDirtyRegionCountAfterDrain;
        float countPressure = Math.min(1.0F, totalPendingDirtyRegions / 4.0F);
        return Math.max(0.5F, Math.max(typePressure, countPressure));
    }

    private String reason(GiCacheSnapshot snapshot, CacheConfidence confidence) {
        if (this.reasonHint != null && !this.reasonHint.isBlank()) {
            return this.reasonHint.trim();
        }
        if (snapshot.hasDirtyRegions()) {
            return "dirty regions pending for GI cache input";
        }
        if (snapshot.isEmpty()) {
            return "GI cache has no planned records yet";
        }
        if (confidence != null) {
            return confidence.reason();
        }
        return "GI cache planner inputs";
    }

    private static float typePressure(DirtyRegionType type) {
        return switch (type) {
            case RESOURCE_PACK_RELOAD, DIMENSION_CHANGE, WORLD_JOIN, WORLD_LEAVE -> 1.0F;
            case CHUNK_LOAD, CHUNK_UNLOAD, SECTION_REBUILD -> 0.85F;
            case WEATHER_CHANGE, TIME_OF_DAY_CHANGE -> 0.75F;
            case BLOCK_UPDATE, FLUID_UPDATE, EMISSIVE_UPDATE -> 0.60F;
        };
    }
}
