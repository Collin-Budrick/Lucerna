package net.lucerna.render.cache;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionListener;
import net.lucerna.world.DirtyRegionSnapshot;
import net.lucerna.world.DirtyRegionSnapshotMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SparseVoxelRadianceCache implements DirtyRegionListener {
    private static final float DEFAULT_USABLE_CONFIDENCE_FLOOR = 0.25F;
    private static final int MAX_RECORDS = 512;
    private static final int MAX_NEW_RECORDS_PER_SNAPSHOT = 128;

    private final SparseVoxelRadianceCacheInvalidationPolicy invalidationPolicy;
    private final Map<SparseVoxelRadianceCacheKey, SparseVoxelRadianceCacheRecord> records = new ConcurrentHashMap<>();
    private final AtomicLong cacheGeneration = new AtomicLong();
    private volatile SparseVoxelRadianceCacheInvalidationSummary lastInvalidation = SparseVoxelRadianceCacheInvalidationSummary.empty(0L);

    public SparseVoxelRadianceCache() {
        this(SparseVoxelRadianceCacheInvalidationPolicy.conservative());
    }

    public SparseVoxelRadianceCache(SparseVoxelRadianceCacheInvalidationPolicy invalidationPolicy) {
        this.invalidationPolicy = invalidationPolicy == null
                ? SparseVoxelRadianceCacheInvalidationPolicy.conservative()
                : invalidationPolicy;
    }

    public void put(SparseVoxelRadianceCacheRecord record) {
        Objects.requireNonNull(record, "record");
        this.records.put(record.key(), record);
        this.cacheGeneration.accumulateAndGet(record.generation(), Math::max);
        this.trimToRecordLimit();
    }

    public void clear() {
        this.records.clear();
        long generation = this.cacheGeneration.incrementAndGet();
        this.lastInvalidation = new SparseVoxelRadianceCacheInvalidationSummary(
                generation,
                generation,
                0,
                0,
                0,
                0,
                0,
                0,
                true,
                "sparse voxel radiance cache cleared"
        );
    }

    @Override
    public void onDirtyRegionMarked(DirtyRegion dirtyRegion) {
        this.applyDirtyRegionSnapshot(DirtyRegionSnapshot.from(List.of(Objects.requireNonNull(dirtyRegion, "dirtyRegion"))));
    }

    public SparseVoxelRadianceCacheSnapshot applyDirtyRegionSnapshot(DirtyRegionSnapshot dirtyRegionSnapshot) {
        DirtyRegionSnapshot resolvedSnapshot = dirtyRegionSnapshot == null
                ? DirtyRegionSnapshot.empty()
                : dirtyRegionSnapshot;
        if (resolvedSnapshot.isEmpty()) {
            return this.snapshot(resolvedSnapshot, SparseVoxelRadianceCacheInvalidationSummary.empty(this.cacheGeneration.get()));
        }

        DirtyRegionSnapshotMetadata metadata = resolvedSnapshot.metadata();
        long nextGeneration = this.cacheGeneration.accumulateAndGet(
                Math.max(resolvedSnapshot.batch().lastGeneration(), metadata.lastGeneration()),
                (current, dirtyGeneration) -> Math.max(current + 1L, dirtyGeneration)
        );

        SparseVoxelRadianceCacheInvalidationSummary summary;
        if (this.invalidationPolicy.invalidatesAll(resolvedSnapshot)) {
            int affected = this.records.size();
            this.records.clear();
            summary = new SparseVoxelRadianceCacheInvalidationSummary(
                    nextGeneration,
                    resolvedSnapshot.batch().lastGeneration(),
                    metadata.sourceRegionCount(),
                    metadata.coalescedRegionCount(),
                    resolvedSnapshot.pendingRegionCountAfterDrain(),
                    affected,
                    0,
                    0,
                    true,
                    "global dirty region invalidated sparse voxel radiance cache"
            );
        } else {
            summary = this.applySectionInvalidations(resolvedSnapshot, metadata, nextGeneration);
        }

        this.lastInvalidation = summary;
        return this.snapshot(resolvedSnapshot, summary);
    }

    public SparseVoxelRadianceCacheSnapshot snapshot() {
        return this.snapshot(DirtyRegionSnapshot.empty(), this.lastInvalidation);
    }

    public SparseVoxelRadianceCacheDebugStatus debugStatus() {
        return this.buildDebugStatus(this.lastInvalidation);
    }

    public int recordCount() {
        return this.records.size();
    }

    private SparseVoxelRadianceCacheInvalidationSummary applySectionInvalidations(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            DirtyRegionSnapshotMetadata metadata,
            long nextGeneration
    ) {
        Set<SparseVoxelRadianceCacheKey> affectedKeys = new HashSet<>();
        int createdDirtyRecords = 0;
        int originalRecordCount = this.records.size();

        for (DirtyRegion dirtyRegion : dirtyRegionSnapshot.regions()) {
            boolean matchedExistingRecord = false;
            for (Map.Entry<SparseVoxelRadianceCacheKey, SparseVoxelRadianceCacheRecord> entry : this.records.entrySet()) {
                SparseVoxelRadianceCacheRecord record = entry.getValue();
                if (this.invalidationPolicy.affects(record, dirtyRegion)) {
                    matchedExistingRecord = true;
                    affectedKeys.add(entry.getKey());
                    this.records.put(entry.getKey(), record.withConfidence(this.invalidationPolicy.dirtyConfidence(
                            dirtyRegion,
                            "dirty region " + dirtyRegion.type().name() + " invalidated sparse voxel radiance cache record"
                    )));
                }
            }

            if (dirtyRegion.sectionScoped()
                    && !matchedExistingRecord
                    && createdDirtyRecords < MAX_NEW_RECORDS_PER_SNAPSHOT
                    && this.records.size() < MAX_RECORDS) {
                SparseVoxelRadianceCacheKey key = SparseVoxelRadianceCacheKey.fromDirtyRegion(
                        dirtyRegion,
                        0,
                        this.invalidationPolicy.sectionsPerCell()
                );
                SparseVoxelRadianceCacheRecord previous = this.records.putIfAbsent(
                        key,
                        SparseVoxelRadianceCacheRecord.allocatedFromDirtyRegion(
                                key,
                                dirtyRegion,
                                nextGeneration
                        )
                );
                if (previous == null) {
                    createdDirtyRecords++;
                }
            }
        }
        this.trimToRecordLimit();

        return new SparseVoxelRadianceCacheInvalidationSummary(
                nextGeneration,
                dirtyRegionSnapshot.batch().lastGeneration(),
                metadata.sourceRegionCount(),
                metadata.coalescedRegionCount(),
                dirtyRegionSnapshot.pendingRegionCountAfterDrain(),
                affectedKeys.size(),
                Math.max(0, originalRecordCount - affectedKeys.size()),
                createdDirtyRecords,
                false,
                !affectedKeys.isEmpty() || createdDirtyRecords > 0
                        ? "section dirty regions invalidated or allocated sparse voxel radiance cache records"
                        : "dirty regions did not overlap sparse voxel radiance cache records"
        );
    }

    private SparseVoxelRadianceCacheSnapshot snapshot(
            DirtyRegionSnapshot dirtyRegionSnapshot,
            SparseVoxelRadianceCacheInvalidationSummary invalidationSummary
    ) {
        List<SparseVoxelRadianceCacheRecord> orderedRecords = new ArrayList<>(this.records.values());
        orderedRecords.sort(Comparator.comparing(record -> record.key().stableKey()));
        SparseVoxelRadianceCacheDebugStatus debugStatus = this.buildDebugStatus(invalidationSummary);
        return new SparseVoxelRadianceCacheSnapshot(
                this.cacheGeneration.get(),
                dirtyRegionSnapshot,
                orderedRecords,
                invalidationSummary,
                debugStatus
        );
    }

    private SparseVoxelRadianceCacheDebugStatus buildDebugStatus(SparseVoxelRadianceCacheInvalidationSummary invalidationSummary) {
        int dirtyRecordCount = 0;
        int usableRecordCount = 0;
        float weightedConfidence = 0.0F;
        int weightTotal = 0;
        float maxVariance = 0.0F;
        long latestSourceGeneration = 0L;

        for (SparseVoxelRadianceCacheRecord record : this.records.values()) {
            SparseVoxelRadianceCacheConfidence confidence = record.confidence();
            if (confidence.dirty()) {
                dirtyRecordCount++;
            }
            if (record.usable(DEFAULT_USABLE_CONFIDENCE_FLOOR)) {
                usableRecordCount++;
            }
            int weight = Math.max(1, confidence.sampleCount());
            weightedConfidence += confidence.confidence() * weight;
            weightTotal += weight;
            maxVariance = Math.max(maxVariance, confidence.variance());
            latestSourceGeneration = Math.max(latestSourceGeneration, confidence.sourceGeneration());
        }

        int recordCount = this.records.size();
        return new SparseVoxelRadianceCacheDebugStatus(
                this.cacheGeneration.get(),
                recordCount,
                dirtyRecordCount,
                usableRecordCount,
                weightTotal == 0 ? 0.0F : weightedConfidence / weightTotal,
                recordCount == 0 ? 1.0F : maxVariance,
                latestSourceGeneration,
                invalidationSummary,
                recordCount == 0
                        ? "sparse voxel radiance cache has no records"
                        : "sparse voxel radiance cache status"
        );
    }

    private void trimToRecordLimit() {
        int overflow = this.records.size() - MAX_RECORDS;
        if (overflow <= 0) {
            return;
        }

        List<SparseVoxelRadianceCacheRecord> orderedRecords = new ArrayList<>(this.records.values());
        orderedRecords.sort(Comparator
                .comparingLong(SparseVoxelRadianceCacheRecord::generation)
                .thenComparing(record -> record.key().stableKey()));
        for (int index = 0; index < overflow && index < orderedRecords.size(); index++) {
            SparseVoxelRadianceCacheRecord record = orderedRecords.get(index);
            this.records.remove(record.key(), record);
        }
    }
}
