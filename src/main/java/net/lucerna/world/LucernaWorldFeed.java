package net.lucerna.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class LucernaWorldFeed {
    private final AtomicLong generation = new AtomicLong();
    private final Queue<DirtyRegion> dirtyRegions = new ConcurrentLinkedQueue<>();
    private volatile String currentDimension = "minecraft:overworld";

    public void markChunkLoaded(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.CHUNK_LOAD, dimension, sectionX, sectionY, sectionZ);
    }

    public void markChunkUnloaded(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.CHUNK_UNLOAD, dimension, sectionX, sectionY, sectionZ);
    }

    public void markSectionRebuilt(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.SECTION_REBUILD, dimension, sectionX, sectionY, sectionZ);
    }

    public void markBlockUpdated(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.BLOCK_UPDATE, dimension, sectionX, sectionY, sectionZ);
    }

    public void markFluidUpdated(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.FLUID_UPDATE, dimension, sectionX, sectionY, sectionZ);
    }

    public void markEmissiveUpdated(String dimension, int sectionX, int sectionY, int sectionZ) {
        this.mark(DirtyRegionType.EMISSIVE_UPDATE, dimension, sectionX, sectionY, sectionZ);
    }

    public void markResourcePackReloaded() {
        this.mark(DirtyRegionType.RESOURCE_PACK_RELOAD, this.currentDimension, 0, 0, 0);
    }

    public void markResourcePackReloaded(String dimension) {
        this.mark(DirtyRegionType.RESOURCE_PACK_RELOAD, dimension, 0, 0, 0);
    }

    public void markWorldJoined(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        this.currentDimension = dimension;
        this.mark(DirtyRegionType.WORLD_JOIN, dimension, 0, 0, 0);
    }

    public void markWorldLeft() {
        this.markWorldLeft(this.currentDimension);
    }

    public void markWorldLeft(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        this.currentDimension = dimension;
        this.mark(DirtyRegionType.WORLD_LEAVE, dimension, 0, 0, 0);
    }

    public void markDimensionChanged(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        this.currentDimension = dimension;
        this.mark(DirtyRegionType.DIMENSION_CHANGE, dimension, 0, 0, 0);
    }

    public void markWeatherChanged() {
        this.mark(DirtyRegionType.WEATHER_CHANGE, this.currentDimension, 0, 0, 0);
    }

    public void markWeatherChanged(String dimension) {
        this.mark(DirtyRegionType.WEATHER_CHANGE, dimension, 0, 0, 0);
    }

    public void markTimeOfDayChanged() {
        this.mark(DirtyRegionType.TIME_OF_DAY_CHANGE, this.currentDimension, 0, 0, 0);
    }

    public void markTimeOfDayChanged(String dimension) {
        this.mark(DirtyRegionType.TIME_OF_DAY_CHANGE, dimension, 0, 0, 0);
    }

    public List<DirtyRegion> drainDirtyRegions() {
        return this.drainDirtyRegionBatch().regions();
    }

    public DirtyRegionBatch drainDirtyRegionBatch() {
        List<DirtyRegion> drained = new ArrayList<>();
        DirtyRegion region;
        while ((region = this.dirtyRegions.poll()) != null) {
            drained.add(region);
        }
        return DirtyRegionBatch.from(drained);
    }

    public DirtyRegionBatch drainDirtyRegionBatch(int maxRegions) {
        if (maxRegions <= 0) {
            throw new IllegalArgumentException("maxRegions must be positive");
        }

        List<DirtyRegion> drained = new ArrayList<>(Math.min(maxRegions, this.dirtyRegions.size()));
        DirtyRegion region;
        while (drained.size() < maxRegions && (region = this.dirtyRegions.poll()) != null) {
            drained.add(region);
        }
        return DirtyRegionBatch.from(drained);
    }

    public long currentGeneration() {
        return this.generation.get();
    }

    public String currentDimension() {
        return this.currentDimension;
    }

    public int pendingDirtyRegionCount() {
        return this.dirtyRegions.size();
    }

    private void mark(DirtyRegionType type, String dimension, int sectionX, int sectionY, int sectionZ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(dimension, "dimension");
        long nextGeneration = this.generation.incrementAndGet();
        this.dirtyRegions.add(new DirtyRegion(type, dimension, sectionX, sectionY, sectionZ, nextGeneration));
    }
}
