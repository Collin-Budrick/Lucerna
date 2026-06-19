package net.lucerna.world.extract;

import net.lucerna.world.DirtyRegion;

import java.util.Objects;

public record ChunkSectionSnapshotSkip(
        DirtyRegion dirtyRegion,
        ChunkSectionSnapshotSkipReason reason,
        String detail
) {
    public ChunkSectionSnapshotSkip {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
    }

    public static ChunkSectionSnapshotSkip of(
            DirtyRegion dirtyRegion,
            ChunkSectionSnapshotSkipReason reason,
            String detail
    ) {
        return new ChunkSectionSnapshotSkip(dirtyRegion, reason, detail);
    }

    public long dirtyRegionGeneration() {
        return this.dirtyRegion.generation();
    }
}
