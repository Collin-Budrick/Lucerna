package net.lucerna.world;

public record DirtyRegionSnapshotMetadata(
        long firstGeneration,
        long lastGeneration,
        int sourceRegionCount,
        int coalescedRegionCount
) {
    public DirtyRegionSnapshotMetadata {
        if (sourceRegionCount < 0) {
            throw new IllegalArgumentException("sourceRegionCount must be non-negative");
        }
        if (coalescedRegionCount < 0) {
            throw new IllegalArgumentException("coalescedRegionCount must be non-negative");
        }
        if (coalescedRegionCount > sourceRegionCount) {
            throw new IllegalArgumentException("coalescedRegionCount cannot exceed sourceRegionCount");
        }

        if (sourceRegionCount == 0) {
            if (firstGeneration != 0 || lastGeneration != 0 || coalescedRegionCount != 0) {
                throw new IllegalArgumentException("empty dirty region snapshots must use zero generation bounds and counts");
            }
        } else if (firstGeneration <= 0 || lastGeneration <= 0) {
            throw new IllegalArgumentException("non-empty dirty region snapshots must use positive generation bounds");
        } else if (firstGeneration > lastGeneration) {
            throw new IllegalArgumentException("firstGeneration must be less than or equal to lastGeneration");
        } else if (coalescedRegionCount == 0) {
            throw new IllegalArgumentException("non-empty dirty region snapshots must contain coalesced regions");
        }
    }

    public static DirtyRegionSnapshotMetadata empty() {
        return new DirtyRegionSnapshotMetadata(0, 0, 0, 0);
    }

    public int duplicateRegionCount() {
        return this.sourceRegionCount - this.coalescedRegionCount;
    }

    public boolean coalesced() {
        return this.duplicateRegionCount() > 0;
    }
}
