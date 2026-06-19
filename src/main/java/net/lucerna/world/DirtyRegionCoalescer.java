package net.lucerna.world;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DirtyRegionCoalescer {
    private static final Comparator<DirtyRegion> DETERMINISTIC_REGION_ORDER = Comparator
            .comparingLong(DirtyRegion::generation)
            .thenComparing(DirtyRegion::dimension)
            .thenComparing(region -> region.type().nativeTypeId())
            .thenComparingInt(DirtyRegion::sectionX)
            .thenComparingInt(DirtyRegion::sectionY)
            .thenComparingInt(DirtyRegion::sectionZ);

    private DirtyRegionCoalescer() {
    }

    static DirtyRegionSnapshot coalesce(Collection<DirtyRegion> regions, int pendingRegionCountAfterDrain) {
        Objects.requireNonNull(regions, "regions");
        List<DirtyRegion> sourceRegions = List.copyOf(regions);
        if (sourceRegions.isEmpty()) {
            return DirtyRegionSnapshot.empty(pendingRegionCountAfterDrain);
        }

        Map<DirtyRegionCoalesceKey, DirtyRegion> latestByKey = new LinkedHashMap<>();
        long firstGeneration = Long.MAX_VALUE;
        long lastGeneration = 0L;

        for (DirtyRegion region : sourceRegions) {
            Objects.requireNonNull(region, "regions must not contain null entries");

            firstGeneration = Math.min(firstGeneration, region.generation());
            lastGeneration = Math.max(lastGeneration, region.generation());

            DirtyRegionCoalesceKey key = region.coalesceKey();
            DirtyRegion latestRegion = latestByKey.get(key);
            if (latestRegion == null || region.generation() > latestRegion.generation()) {
                latestByKey.put(key, region);
            }
        }

        List<DirtyRegion> coalescedRegions = latestByKey.values().stream()
                .sorted(DETERMINISTIC_REGION_ORDER)
                .toList();
        DirtyRegionSnapshotMetadata metadata = new DirtyRegionSnapshotMetadata(
                firstGeneration,
                lastGeneration,
                sourceRegions.size(),
                coalescedRegions.size()
        );
        return new DirtyRegionSnapshot(
                DirtyRegionBatch.fromCoalesced(coalescedRegions),
                metadata,
                pendingRegionCountAfterDrain
        );
    }
}
