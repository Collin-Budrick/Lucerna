package net.lucerna.world.hooks;

import net.lucerna.world.LucernaWorldFeed;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import java.util.Objects;

public final class LucernaWorldFeedAdapter {
    private final LucernaWorldFeed feed;

    public LucernaWorldFeedAdapter(LucernaWorldFeed feed) {
        this.feed = Objects.requireNonNull(feed, "feed");
    }

    public String dimensionId(ClientLevel level) {
        Objects.requireNonNull(level, "level");
        return level.dimension().identifier().toString();
    }

    public void markWorldJoined(ClientLevel level) {
        this.feed.markWorldJoined(this.dimensionId(level));
    }

    public void markWorldLeft(ClientLevel level) {
        this.feed.markWorldLeft(this.dimensionId(level));
    }

    public void markWorldLeft(String dimension) {
        this.feed.markWorldLeft(dimension);
    }

    public void markDimensionChanged(ClientLevel level) {
        this.feed.markDimensionChanged(this.dimensionId(level));
    }

    public void markWeatherChanged(ClientLevel level) {
        this.feed.markWeatherChanged(this.dimensionId(level));
    }

    public void markTimeOfDayChanged(ClientLevel level) {
        this.feed.markTimeOfDayChanged(this.dimensionId(level));
    }

    public void markResourcePackReloaded() {
        this.feed.markResourcePackReloaded();
    }

    public void markChunkLoaded(ClientLevel level, LevelChunk chunk) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(chunk, "chunk");

        String dimension = this.dimensionId(level);
        ChunkPos pos = chunk.getPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            this.feed.markChunkLoaded(dimension, pos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), pos.z());
        }
    }

    public void markChunkUnloaded(ClientLevel level, LevelChunk chunk) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(chunk, "chunk");

        String dimension = this.dimensionId(level);
        ChunkPos pos = chunk.getPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            this.feed.markChunkUnloaded(dimension, pos.x(), chunk.getSectionYFromSectionIndex(sectionIndex), pos.z());
        }
    }

    public void markSectionRebuilt(ClientLevel level, SectionPos sectionPos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(sectionPos, "sectionPos");
        this.feed.markSectionRebuilt(this.dimensionId(level), sectionPos.x(), sectionPos.y(), sectionPos.z());
    }

    public void markSectionRebuilt(ClientLevel level, int sectionX, int sectionY, int sectionZ) {
        this.feed.markSectionRebuilt(this.dimensionId(level), sectionX, sectionY, sectionZ);
    }

    public void markBlockUpdated(ClientLevel level, BlockPos pos, BlockState previousState, BlockState currentState) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");

        String dimension = this.dimensionId(level);
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());

        this.feed.markBlockUpdated(dimension, sectionX, sectionY, sectionZ);
        if (this.fluidChanged(previousState, currentState)) {
            this.feed.markFluidUpdated(dimension, sectionX, sectionY, sectionZ);
        }
        if (this.emissiveChanged(previousState, currentState)) {
            this.feed.markEmissiveUpdated(dimension, sectionX, sectionY, sectionZ);
        }
    }

    public void markBlockEntityChanged(ClientLevel level, BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity");
        this.markBlockUpdated(level, blockEntity.getBlockPos(), null, blockEntity.getBlockState());
    }

    private boolean fluidChanged(BlockState previousState, BlockState currentState) {
        FluidState previousFluid = previousState == null ? null : previousState.getFluidState();
        FluidState currentFluid = currentState == null ? null : currentState.getFluidState();
        if (previousFluid == null) {
            return currentFluid != null && !currentFluid.isEmpty();
        }
        if (currentFluid == null) {
            return !previousFluid.isEmpty();
        }

        return previousFluid.isEmpty() != currentFluid.isEmpty()
                || previousFluid.getType() != currentFluid.getType()
                || previousFluid.getAmount() != currentFluid.getAmount();
    }

    private boolean emissiveChanged(BlockState previousState, BlockState currentState) {
        return this.lightEmission(previousState) != this.lightEmission(currentState)
                || this.emissiveRendering(previousState) != this.emissiveRendering(currentState)
                || this.isEmissive(currentState);
    }

    private boolean isEmissive(BlockState state) {
        return this.lightEmission(state) > 0 || this.emissiveRendering(state);
    }

    private int lightEmission(BlockState state) {
        return state == null ? 0 : state.getLightEmission();
    }

    private boolean emissiveRendering(BlockState state) {
        return state != null && state.emissiveRendering();
    }
}
