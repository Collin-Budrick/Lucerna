package net.lucerna.render.lighting.gi;

import net.lucerna.render.cache.SparseVoxelRadianceCacheConfidence;
import net.lucerna.render.cache.SparseVoxelRadianceCacheKey;
import net.lucerna.render.cache.SparseVoxelRadianceCacheRecord;
import net.lucerna.render.cache.SparseVoxelRadianceCacheSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SparseVoxelRadianceCacheSnapshotAdapter {
    private static final int DEFAULT_SECTIONS_PER_RADIANCE_CELL = 1;
    private static final int LOCAL_SAMPLE_MASK = 15;
    private static final int FACE_COUNT = 6;

    private SparseVoxelRadianceCacheSnapshotAdapter() {
    }

    public static GiCacheSnapshot toGiCacheSnapshot(SparseVoxelRadianceCacheSnapshot snapshot) {
        return toGiCacheSnapshot(snapshot, DEFAULT_SECTIONS_PER_RADIANCE_CELL);
    }

    public static GiCacheSnapshot toGiCacheSnapshot(
            SparseVoxelRadianceCacheSnapshot snapshot,
            int sectionsPerRadianceCell
    ) {
        if (snapshot == null || !snapshot.hasRecords()) {
            return GiCacheSnapshot.empty();
        }

        int resolvedSectionsPerRadianceCell = Math.max(1, sectionsPerRadianceCell);
        List<SurfaceCacheRecord> surfaceRecords = new ArrayList<>(snapshot.records().size());
        List<RadianceCacheRecord> radianceRecords = new ArrayList<>(snapshot.records().size());
        for (SparseVoxelRadianceCacheRecord record : snapshot.records()) {
            surfaceRecords.add(toSurfaceRecord(record, resolvedSectionsPerRadianceCell));
            radianceRecords.add(toRadianceRecord(record));
        }

        return GiCacheSnapshot.from(
                snapshot.cacheGeneration(),
                snapshot.dirtyRegionSnapshot().batch(),
                surfaceRecords,
                radianceRecords
        );
    }

    private static SurfaceCacheRecord toSurfaceRecord(
            SparseVoxelRadianceCacheRecord record,
            int sectionsPerRadianceCell
    ) {
        Objects.requireNonNull(record, "record");
        SparseVoxelRadianceCacheKey key = record.key();
        int hash = key.stableKey().hashCode();
        int sectionX = key.cellX() * sectionsPerRadianceCell;
        int sectionY = key.cellY() * sectionsPerRadianceCell;
        int sectionZ = key.cellZ() * sectionsPerRadianceCell;
        int localX = local(hash, 0);
        int localY = local(hash, 4);
        int localZ = local(hash, 8);
        int faceOrdinal = Math.floorMod(hash >>> 12, FACE_COUNT);
        return new SurfaceCacheRecord(
                new SurfaceCacheKey(
                        key.dimension(),
                        sectionX,
                        sectionY,
                        sectionZ,
                        localX,
                        localY,
                        localZ,
                        faceOrdinal
                ),
                record.generation(),
                0,
                record.directionX(),
                record.directionY(),
                record.directionZ(),
                clampUnit(record.radianceR() * 4.0F),
                clampUnit(record.radianceG() * 4.0F),
                clampUnit(record.radianceB() * 4.0F),
                0.75F,
                toCacheConfidence(record.confidence())
        );
    }

    private static RadianceCacheRecord toRadianceRecord(SparseVoxelRadianceCacheRecord record) {
        Objects.requireNonNull(record, "record");
        SparseVoxelRadianceCacheKey key = record.key();
        return new RadianceCacheRecord(
                new RadianceCacheKey(
                        key.dimension(),
                        key.cellX(),
                        key.cellY(),
                        key.cellZ(),
                        key.cascade()
                ),
                record.generation(),
                record.radianceR(),
                record.radianceG(),
                record.radianceB(),
                record.directionX(),
                record.directionY(),
                record.directionZ(),
                record.variance(),
                record.sampleCount(),
                record.lastFrameIndex(),
                toCacheConfidence(record.confidence())
        );
    }

    private static CacheConfidence toCacheConfidence(SparseVoxelRadianceCacheConfidence confidence) {
        if (confidence == null) {
            return CacheConfidence.empty("sparse radiance cache confidence unavailable");
        }
        return new CacheConfidence(
                confidence.confidence(),
                confidence.variance(),
                confidence.sampleCount(),
                confidence.sourceGeneration(),
                confidence.lastTouchedFrame(),
                confidence.dirty(),
                confidence.reason()
        );
    }

    private static int local(int hash, int shift) {
        return (hash >>> shift) & LOCAL_SAMPLE_MASK;
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
