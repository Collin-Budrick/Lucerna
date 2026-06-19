package net.lucerna.world.extract;

import net.lucerna.material.MaterialFlags;
import net.lucerna.material.extract.LucernaMaterialExtractionService;
import net.lucerna.material.extract.RegisteredMaterial;
import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionEmissiveEntryMetadata;
import net.lucerna.world.section.SectionMaterialPaletteReference;
import net.lucerna.world.section.VoxelOccupancyBitOrder;
import net.lucerna.world.section.VoxelOccupancyMaskMetadata;
import net.lucerna.world.section.VoxelOccupancySummary;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MinecraftChunkSectionSnapshotExtractor {
    private final LucernaMaterialExtractionService materialExtractionService;

    public MinecraftChunkSectionSnapshotExtractor(LucernaMaterialExtractionService materialExtractionService) {
        this.materialExtractionService = Objects.requireNonNull(materialExtractionService, "materialExtractionService");
    }

    public List<ChunkSectionVoxelSnapshot> extractChunkSections(
            ClientLevel level,
            LevelChunk chunk,
            long sectionGeneration
    ) {
        return this.extractChunkSections(level, chunk, sectionGeneration, null);
    }

    public List<ChunkSectionVoxelSnapshot> extractChunkSections(
            ClientLevel level,
            LevelChunk chunk,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Objects.requireNonNull(level, "level");
        return this.extractChunkSections(dimensionId(level), chunk, sectionGeneration, modelManager);
    }

    public List<ChunkSectionVoxelSnapshot> extractChunkSections(
            String dimension,
            LevelChunk chunk,
            long sectionGeneration
    ) {
        return this.extractChunkSections(dimension, chunk, sectionGeneration, null);
    }

    public List<ChunkSectionVoxelSnapshot> extractChunkSections(
            String dimension,
            LevelChunk chunk,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(chunk, "chunk");
        requireNonNegative(sectionGeneration, "sectionGeneration");

        LevelChunkSection[] sections = chunk.getSections();
        List<ChunkSectionVoxelSnapshot> snapshots = new ArrayList<>(sections.length);
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            snapshots.add(this.extract(dimension, chunk, sectionIndex, sectionGeneration, modelManager));
        }
        return List.copyOf(snapshots);
    }

    public ChunkSectionVoxelSnapshot extract(
            ClientLevel level,
            LevelChunk chunk,
            int sectionIndex,
            long sectionGeneration
    ) {
        return this.extract(level, chunk, sectionIndex, sectionGeneration, null);
    }

    public ChunkSectionVoxelSnapshot extract(
            ClientLevel level,
            LevelChunk chunk,
            int sectionIndex,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Objects.requireNonNull(level, "level");
        return this.extract(dimensionId(level), chunk, sectionIndex, sectionGeneration, modelManager);
    }

    public ChunkSectionVoxelSnapshot extract(
            String dimension,
            LevelChunk chunk,
            int sectionIndex,
            long sectionGeneration
    ) {
        return this.extract(dimension, chunk, sectionIndex, sectionGeneration, null);
    }

    public ChunkSectionVoxelSnapshot extract(
            String dimension,
            LevelChunk chunk,
            int sectionIndex,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(chunk, "chunk");
        requireNonNegative(sectionGeneration, "sectionGeneration");

        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            throw new IllegalArgumentException("sectionIndex must be between 0 and " + (sections.length - 1));
        }

        ChunkPos chunkPos = chunk.getPos();
        ChunkSectionOrigin origin = new ChunkSectionOrigin(
                dimension,
                chunkPos.x(),
                chunk.getSectionYFromSectionIndex(sectionIndex),
                chunkPos.z()
        );
        LevelChunkSection section = sections[sectionIndex];
        if (section == null) {
            return this.emptySnapshot(origin, sectionGeneration);
        }
        return this.extract(origin, section, sectionGeneration, modelManager);
    }

    public ChunkSectionVoxelSnapshot extract(
            ChunkSectionOrigin origin,
            LevelChunkSection section,
            long sectionGeneration
    ) {
        return this.extract(origin, section, sectionGeneration, null);
    }

    public ChunkSectionVoxelSnapshot extract(
            ChunkSectionOrigin origin,
            LevelChunkSection section,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(section, "section");
        requireNonNegative(sectionGeneration, "sectionGeneration");

        if (section.hasOnlyAir() && !section.hasFluid()) {
            return this.emptySnapshot(origin, sectionGeneration);
        }

        SectionReadback readback = readOccupiedVoxels(section);
        if (readback.voxels().isEmpty()) {
            return this.emptySnapshot(origin, sectionGeneration);
        }

        return this.buildSnapshot(origin, readback, sectionGeneration, modelManager);
    }

    public LucernaMaterialExtractionService materialExtractionService() {
        return this.materialExtractionService;
    }

    private ChunkSectionVoxelSnapshot buildSnapshot(
            ChunkSectionOrigin origin,
            SectionReadback readback,
            long sectionGeneration,
            ModelManager modelManager
    ) {
        Set<Integer> materialIds = new LinkedHashSet<>();
        List<SectionEmissiveEntryMetadata> emissiveEntries = new ArrayList<>();
        int opaqueVoxelCount = 0;
        int translucentVoxelCount = 0;
        int fluidVoxelCount = 0;
        int emissiveVoxelCount = 0;

        for (VoxelReadback voxel : readback.voxels()) {
            RegisteredMaterial registeredMaterial = this.materialExtractionService.resolve(voxel.state(), modelManager);
            int materialId = registeredMaterial.material().materialId();
            int materialFlags = registeredMaterial.material().flags();
            materialIds.add(materialId);

            boolean opaque = MaterialFlags.has(materialFlags, MaterialFlags.OPAQUE);
            boolean translucent = MaterialFlags.has(materialFlags, MaterialFlags.TRANSLUCENT);
            if (opaque) {
                opaqueVoxelCount++;
            }
            if (translucent && !opaque) {
                translucentVoxelCount++;
            }
            if (MaterialFlags.has(materialFlags, MaterialFlags.FLUID) || !voxel.fluidState().isEmpty()) {
                fluidVoxelCount++;
            }

            int blockLightLevel = blockLightLevel(voxel.state());
            if (isEmissive(voxel.state(), materialFlags, blockLightLevel)) {
                emissiveVoxelCount++;
                emissiveEntries.add(new SectionEmissiveEntryMetadata(
                        voxel.localX(),
                        voxel.localY(),
                        voxel.localZ(),
                        materialId,
                        blockLightLevel,
                        sectionGeneration
                ));
            }
        }

        long materialGeneration = this.materialExtractionService.materialRegistry().currentGeneration();
        ChunkSectionGeneration generation = new ChunkSectionGeneration(
                sectionGeneration,
                materialGeneration,
                sectionGeneration,
                sectionGeneration
        );
        VoxelOccupancySummary summary = new VoxelOccupancySummary(
                readback.voxels().size(),
                opaqueVoxelCount,
                translucentVoxelCount,
                fluidVoxelCount,
                emissiveVoxelCount
        );
        VoxelOccupancyMaskMetadata maskMetadata = VoxelOccupancyMaskMetadata.sectionMask(0, sectionGeneration);
        SectionMaterialPaletteReference paletteReference = new SectionMaterialPaletteReference(
                materialGeneration,
                0,
                List.copyOf(materialIds)
        );

        return new ChunkSectionVoxelSnapshot(
                origin,
                generation,
                summary,
                maskMetadata,
                paletteReference,
                emissiveEntries
        );
    }

    private ChunkSectionVoxelSnapshot emptySnapshot(ChunkSectionOrigin origin, long sectionGeneration) {
        long materialGeneration = this.materialExtractionService.materialRegistry().currentGeneration();
        return new ChunkSectionVoxelSnapshot(
                origin,
                new ChunkSectionGeneration(sectionGeneration, materialGeneration, sectionGeneration, sectionGeneration),
                VoxelOccupancySummary.empty(),
                VoxelOccupancyMaskMetadata.empty(),
                new SectionMaterialPaletteReference(materialGeneration, 0, List.of()),
                List.of()
        );
    }

    private static SectionReadback readOccupiedVoxels(LevelChunkSection section) {
        List<VoxelReadback> voxels = new ArrayList<>();
        section.acquire();
        try {
            for (int localY = 0; localY < ChunkSectionOrigin.SECTION_EDGE_LENGTH; localY++) {
                for (int localZ = 0; localZ < ChunkSectionOrigin.SECTION_EDGE_LENGTH; localZ++) {
                    for (int localX = 0; localX < ChunkSectionOrigin.SECTION_EDGE_LENGTH; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        FluidState fluidState = section.getFluidState(localX, localY, localZ);
                        if (!isOccupied(state, fluidState)) {
                            continue;
                        }
                        voxels.add(new VoxelReadback(localX, localY, localZ, state, fluidState));
                    }
                }
            }
        } finally {
            section.release();
        }
        return new SectionReadback(voxels);
    }

    private static boolean isOccupied(BlockState state, FluidState fluidState) {
        return !state.isAir() || !fluidState.isEmpty();
    }

    private static boolean isEmissive(BlockState state, int materialFlags, int blockLightLevel) {
        return blockLightLevel > 0
                || state.emissiveRendering()
                || MaterialFlags.has(materialFlags, MaterialFlags.EMISSIVE);
    }

    private static int blockLightLevel(BlockState state) {
        return Math.max(0, Math.min(15, state.getLightEmission()));
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String dimensionId(ClientLevel level) {
        return level.dimension().identifier().toString();
    }

    private record SectionReadback(List<VoxelReadback> voxels) {
        private SectionReadback {
            voxels = List.copyOf(voxels);
        }
    }

    private record VoxelReadback(
            int localX,
            int localY,
            int localZ,
            BlockState state,
            FluidState fluidState
    ) {
        private VoxelReadback {
            VoxelOccupancyBitOrder.requireLocalCoordinate(localX, "localX");
            VoxelOccupancyBitOrder.requireLocalCoordinate(localY, "localY");
            VoxelOccupancyBitOrder.requireLocalCoordinate(localZ, "localZ");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(fluidState, "fluidState");
        }
    }
}
