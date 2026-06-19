package net.lucerna.world.extract;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;

import java.util.Objects;

public record ChunkSectionSnapshotHandoff(
        DirtyRegion dirtyRegion,
        ChunkSectionVoxelSnapshot snapshot,
        boolean fromCache
) {
    public ChunkSectionSnapshotHandoff {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!dirtyRegion.sectionScoped()) {
            throw new IllegalArgumentException("dirtyRegion must be section scoped");
        }

        ChunkSectionOrigin origin = snapshot.origin();
        if (!dirtyRegion.dimension().equals(origin.dimension())
                || dirtyRegion.sectionX() != origin.sectionX()
                || dirtyRegion.sectionY() != origin.sectionY()
                || dirtyRegion.sectionZ() != origin.sectionZ()) {
            throw new IllegalArgumentException("dirtyRegion must match the snapshot origin");
        }
    }

    public static ChunkSectionSnapshotHandoff extracted(
            DirtyRegion dirtyRegion,
            ChunkSectionVoxelSnapshot snapshot
    ) {
        return new ChunkSectionSnapshotHandoff(dirtyRegion, snapshot, false);
    }

    public static ChunkSectionSnapshotHandoff cached(
            DirtyRegion dirtyRegion,
            ChunkSectionVoxelSnapshot snapshot
    ) {
        return new ChunkSectionSnapshotHandoff(dirtyRegion, snapshot, true);
    }

    public ChunkSectionOrigin origin() {
        return this.snapshot.origin();
    }

    public long dirtyRegionGeneration() {
        return this.dirtyRegion.generation();
    }
}
