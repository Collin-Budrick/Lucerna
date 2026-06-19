package net.lucerna.world;

@FunctionalInterface
public interface DirtyRegionListener {
    void onDirtyRegionMarked(DirtyRegion dirtyRegion);
}
