package net.lucerna.world;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record DirtyRegionSnapshot(
        DirtyRegionBatch batch,
        DirtyRegionSnapshotMetadata metadata,
        int pendingRegionCountAfterDrain
) {
    public DirtyRegionSnapshot {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(metadata, "metadata");
        if (pendingRegionCountAfterDrain < 0) {
            throw new IllegalArgumentException("pendingRegionCountAfterDrain must be non-negative");
        }
        if (batch.dirtyRegionCount() != metadata.coalescedRegionCount()) {
            throw new IllegalArgumentException("batch dirty region count must match coalesced metadata count");
        }
    }

    public static DirtyRegionSnapshot empty() {
        return empty(0);
    }

    public static DirtyRegionSnapshot empty(int pendingRegionCountAfterDrain) {
        return new DirtyRegionSnapshot(
                DirtyRegionBatch.empty(),
                DirtyRegionSnapshotMetadata.empty(),
                pendingRegionCountAfterDrain
        );
    }

    public static DirtyRegionSnapshot from(Collection<DirtyRegion> regions) {
        return from(regions, 0);
    }

    public static DirtyRegionSnapshot from(Collection<DirtyRegion> regions, int pendingRegionCountAfterDrain) {
        return DirtyRegionCoalescer.coalesce(regions, pendingRegionCountAfterDrain);
    }

    public boolean isEmpty() {
        return this.batch.isEmpty();
    }

    public List<DirtyRegion> regions() {
        return this.batch.regions();
    }
}
