package net.lucerna.world;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record DirtyRegionBatch(
        long firstGeneration,
        long lastGeneration,
        List<DirtyRegion> regions
) {
    public DirtyRegionBatch {
        Objects.requireNonNull(regions, "regions");
        regions = List.copyOf(regions);

        if (regions.isEmpty()) {
            if (firstGeneration != 0 || lastGeneration != 0) {
                throw new IllegalArgumentException("empty dirty region batches must use zero generation bounds");
            }
        } else if (firstGeneration <= 0 || lastGeneration <= 0) {
            throw new IllegalArgumentException("non-empty dirty region batches must use positive generation bounds");
        } else if (firstGeneration > lastGeneration) {
            throw new IllegalArgumentException("firstGeneration must be less than or equal to lastGeneration");
        } else {
            long actualFirstGeneration = regions.stream()
                    .min(Comparator.comparingLong(DirtyRegion::generation))
                    .orElseThrow()
                    .generation();
            long actualLastGeneration = regions.stream()
                    .max(Comparator.comparingLong(DirtyRegion::generation))
                    .orElseThrow()
                    .generation();
            if (firstGeneration != actualFirstGeneration || lastGeneration != actualLastGeneration) {
                throw new IllegalArgumentException("generation bounds must match dirty region payloads");
            }
        }
    }

    public static DirtyRegionBatch empty() {
        return new DirtyRegionBatch(0, 0, List.of());
    }

    public static DirtyRegionBatch from(Collection<DirtyRegion> regions) {
        Objects.requireNonNull(regions, "regions");
        if (regions.isEmpty()) {
            return empty();
        }

        List<DirtyRegion> immutableRegions = List.copyOf(regions);
        long firstGeneration = immutableRegions.stream()
                .min(Comparator.comparingLong(DirtyRegion::generation))
                .orElseThrow()
                .generation();
        long lastGeneration = immutableRegions.stream()
                .max(Comparator.comparingLong(DirtyRegion::generation))
                .orElseThrow()
                .generation();
        return new DirtyRegionBatch(firstGeneration, lastGeneration, immutableRegions);
    }

    public boolean isEmpty() {
        return this.regions.isEmpty();
    }

    public int dirtyRegionCount() {
        return this.regions.size();
    }
}
