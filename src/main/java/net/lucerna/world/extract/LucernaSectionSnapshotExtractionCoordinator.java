package net.lucerna.world.extract;

import net.lucerna.world.DirtyRegion;
import net.lucerna.world.DirtyRegionBatch;
import net.lucerna.world.DirtyRegionKey;
import net.lucerna.world.DirtyRegionSnapshot;
import net.lucerna.world.DirtyRegionType;
import net.lucerna.world.LucernaWorldFeed;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LucernaSectionSnapshotExtractionCoordinator {
    private final MinecraftChunkSectionSnapshotExtractor snapshotExtractor;
    private final Map<DirtyRegionKey, ChunkSectionVoxelSnapshot> snapshotCache = new LinkedHashMap<>();

    public LucernaSectionSnapshotExtractionCoordinator(MinecraftChunkSectionSnapshotExtractor snapshotExtractor) {
        this.snapshotExtractor = Objects.requireNonNull(snapshotExtractor, "snapshotExtractor");
    }

    public synchronized ChunkSectionSnapshotExtractionResult drainAndExtract(
            Minecraft client,
            LucernaWorldFeed worldFeed
    ) {
        Objects.requireNonNull(worldFeed, "worldFeed");
        return this.extractDirtySections(client, worldFeed.drainDirtyRegionSnapshot());
    }

    public synchronized ChunkSectionSnapshotExtractionResult drainAndExtract(
            Minecraft client,
            LucernaWorldFeed worldFeed,
            int maxRegions
    ) {
        Objects.requireNonNull(worldFeed, "worldFeed");
        return this.extractDirtySections(client, worldFeed.drainDirtyRegionSnapshot(maxRegions));
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            Minecraft client,
            Collection<DirtyRegion> dirtyRegions
    ) {
        return this.extractDirtySections(client, DirtyRegionSnapshot.from(dirtyRegions));
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            Minecraft client,
            DirtyRegionBatch dirtyRegionBatch
    ) {
        Objects.requireNonNull(dirtyRegionBatch, "dirtyRegionBatch");
        return this.extractDirtySections(client, DirtyRegionSnapshot.from(dirtyRegionBatch.regions()));
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            Minecraft client,
            DirtyRegionSnapshot dirtyRegionSnapshot
    ) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.extractDirtySections(client.level, client.getModelManager(), dirtyRegionSnapshot, true);
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            ClientLevel level,
            ModelManager modelManager,
            Collection<DirtyRegion> dirtyRegions
    ) {
        return this.extractDirtySections(level, modelManager, DirtyRegionSnapshot.from(dirtyRegions));
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            ClientLevel level,
            ModelManager modelManager,
            DirtyRegionBatch dirtyRegionBatch
    ) {
        Objects.requireNonNull(dirtyRegionBatch, "dirtyRegionBatch");
        return this.extractDirtySections(level, modelManager, DirtyRegionSnapshot.from(dirtyRegionBatch.regions()));
    }

    public synchronized ChunkSectionSnapshotExtractionResult extractDirtySections(
            ClientLevel level,
            ModelManager modelManager,
            DirtyRegionSnapshot dirtyRegionSnapshot
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(dirtyRegionSnapshot, "dirtyRegionSnapshot");
        return this.extractDirtySections(level, modelManager, dirtyRegionSnapshot, false);
    }

    public MinecraftChunkSectionSnapshotExtractor snapshotExtractor() {
        return this.snapshotExtractor;
    }

    public synchronized Optional<ChunkSectionVoxelSnapshot> cachedSnapshot(DirtyRegionKey key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(this.snapshotCache.get(key));
    }

    public synchronized List<ChunkSectionVoxelSnapshot> cachedSnapshots() {
        return List.copyOf(this.snapshotCache.values());
    }

    public synchronized Map<DirtyRegionKey, ChunkSectionVoxelSnapshot> cachedSnapshotsByRegion() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.snapshotCache));
    }

    public synchronized int cachedSectionCount() {
        return this.snapshotCache.size();
    }

    public synchronized void clearCache() {
        this.snapshotCache.clear();
    }

    private ChunkSectionSnapshotExtractionResult extractDirtySections(
            ClientLevel level,
            ModelManager modelManager,
            DirtyRegionSnapshot dirtyRegionSnapshot,
            boolean allowMissingLevel
    ) {
        if (!allowMissingLevel) {
            Objects.requireNonNull(level, "level");
        }

        if (dirtyRegionSnapshot.isEmpty()) {
            return ChunkSectionSnapshotExtractionResult.empty(dirtyRegionSnapshot, this.snapshotCache.size());
        }

        String currentDimension = level == null ? null : dimensionId(level);
        List<ChunkSectionSnapshotHandoff> sectionSnapshots = new ArrayList<>();
        List<ChunkSectionSnapshotSkip> skippedSections = new ArrayList<>();

        for (DirtyRegion dirtyRegion : dirtyRegionSnapshot.regions()) {
            if (!dirtyRegion.sectionScoped()) {
                this.applyGlobalInvalidation(dirtyRegion);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.NON_SECTION_SCOPED_REGION,
                        "Dirty region type " + dirtyRegion.type().name() + " does not target a chunk section"
                ));
                continue;
            }

            DirtyRegionKey cacheKey = dirtyRegion.key();
            if (dirtyRegion.type() == DirtyRegionType.CHUNK_UNLOAD) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.CHUNK_UNLOADED,
                        "Chunk unload invalidated the cached section snapshot"
                ));
                continue;
            }

            if (level == null) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.NO_CLIENT_LEVEL,
                        "No client level is available for section extraction"
                ));
                continue;
            }

            if (!dirtyRegion.dimension().equals(currentDimension)) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.DIMENSION_MISMATCH,
                        "Dirty region dimension " + dirtyRegion.dimension()
                                + " does not match current level " + currentDimension
                ));
                continue;
            }

            if (!isSectionInsideLevel(level, dirtyRegion.sectionY())) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.SECTION_OUTSIDE_LEVEL_HEIGHT,
                        "Section Y " + dirtyRegion.sectionY() + " is outside the current level height"
                ));
                continue;
            }

            ChunkSectionVoxelSnapshot cachedSnapshot = this.snapshotCache.get(cacheKey);
            if (satisfies(cachedSnapshot, dirtyRegion)) {
                sectionSnapshots.add(ChunkSectionSnapshotHandoff.cached(dirtyRegion, cachedSnapshot));
                continue;
            }

            LevelChunk chunk = level.getChunkSource().getChunk(
                    dirtyRegion.sectionX(),
                    dirtyRegion.sectionZ(),
                    ChunkStatus.FULL,
                    false
            );
            if (chunk == null) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.MISSING_CHUNK,
                        "Chunk is not currently loaded in the client chunk cache"
                ));
                continue;
            }

            int sectionIndex = level.getSectionIndexFromSectionY(dirtyRegion.sectionY());
            LevelChunkSection[] sections = chunk.getSections();
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.SECTION_INDEX_OUT_OF_RANGE,
                        "Resolved section index " + sectionIndex + " is outside the chunk section array"
                ));
                continue;
            }

            if (sections[sectionIndex] == null) {
                this.snapshotCache.remove(cacheKey);
                skippedSections.add(skip(
                        dirtyRegion,
                        ChunkSectionSnapshotSkipReason.SECTION_UNAVAILABLE,
                        "Chunk section is not available for extraction"
                ));
                continue;
            }

            ChunkSectionVoxelSnapshot snapshot = this.snapshotExtractor.extract(
                    currentDimension,
                    chunk,
                    sectionIndex,
                    dirtyRegion.generation(),
                    modelManager
            );
            this.snapshotCache.put(cacheKey, snapshot);
            sectionSnapshots.add(ChunkSectionSnapshotHandoff.extracted(dirtyRegion, snapshot));
        }

        return new ChunkSectionSnapshotExtractionResult(
                dirtyRegionSnapshot,
                sectionSnapshots,
                skippedSections,
                this.snapshotCache.size()
        );
    }

    private void applyGlobalInvalidation(DirtyRegion dirtyRegion) {
        switch (dirtyRegion.type()) {
            case WORLD_JOIN, WORLD_LEAVE, DIMENSION_CHANGE, RESOURCE_PACK_RELOAD -> this.snapshotCache.clear();
            case WEATHER_CHANGE, TIME_OF_DAY_CHANGE -> {
            }
            default -> {
            }
        }
    }

    private static boolean satisfies(ChunkSectionVoxelSnapshot cachedSnapshot, DirtyRegion dirtyRegion) {
        if (cachedSnapshot == null) {
            return false;
        }
        return cachedSnapshot.generation().sectionGeneration() >= dirtyRegion.generation();
    }

    private static boolean isSectionInsideLevel(ClientLevel level, int sectionY) {
        return sectionY >= level.getMinSectionY() && sectionY <= level.getMaxSectionY();
    }

    private static String dimensionId(ClientLevel level) {
        return level.dimension().identifier().toString();
    }

    private static ChunkSectionSnapshotSkip skip(
            DirtyRegion dirtyRegion,
            ChunkSectionSnapshotSkipReason reason,
            String detail
    ) {
        return ChunkSectionSnapshotSkip.of(dirtyRegion, reason, detail);
    }
}
