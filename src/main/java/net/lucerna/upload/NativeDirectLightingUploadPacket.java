package net.lucerna.upload;

import net.lucerna.render.lighting.direct.DirectCelestialLight;
import net.lucerna.render.lighting.direct.DirectCelestialLightSource;
import net.lucerna.render.lighting.direct.DirectEmissiveBlockLight;
import net.lucerna.render.lighting.direct.DirectLightingPlan;
import net.lucerna.render.lighting.direct.DirectShadowRayCandidate;
import net.lucerna.render.lighting.direct.DirectShadowRayPlan;
import net.lucerna.render.lighting.direct.DirectShadowRaySource;
import net.lucerna.render.voxel.VoxelRay;
import net.lucerna.render.voxel.VoxelRayBudgetConfig;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.world.section.ChunkSectionGeneration;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.VoxelOccupancyMaskMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record NativeDirectLightingUploadPacket(
        long frameIndex,
        long generation,
        long firstGeneration,
        long lastGeneration,
        long celestialGeneration,
        long emissiveGeneration,
        long shadowGeneration,
        long shadowCandidateGeneration,
        long sectionSnapshotGeneration,
        String dimensionId,
        int flags,
        int celestialLightCount,
        float celestialLightEnergy,
        int selectedEmissiveCount,
        float selectedEmissiveEnergy,
        int shadowCandidateCount,
        int budgetedShadowCandidateCount,
        int sectionSnapshotCount,
        int[] rayBudget,
        int[] celestialLightSources,
        int[] celestialLightFlags,
        float[] celestialLightData,
        String[] emissiveLightDimensions,
        int[] emissiveLightMetadata,
        float[] emissiveLightData,
        long[] emissiveLightGenerations,
        int[] shadowCandidateMetadata,
        float[] shadowCandidateRays,
        long[] shadowCandidateGenerations,
        String[] sectionSnapshotDimensions,
        int[] sectionSnapshotMetadata,
        long[] sectionSnapshotGenerations
) {
    public static final int SOURCE_SUN = 1;
    public static final int SOURCE_MOON = 2;
    public static final int SOURCE_EMISSIVE_BLOCK = 3;

    public static final int FLAG_VALIDATED = 1;
    public static final int FLAG_HAS_DIRECT_LIGHTING_WORK = 1 << 1;
    public static final int FLAG_READY_FOR_SHADOW_TRACING = 1 << 2;
    public static final int FLAG_REQUIRES_OCCUPANCY_MASKS = 1 << 3;
    public static final int FLAG_ALLOW_TRANSLUCENT_OCCLUDERS = 1 << 4;
    public static final int FLAG_WORLD_TIME_AVAILABLE = 1 << 5;

    public static final int LIGHT_FLAG_ENABLED = 1;
    public static final int LIGHT_FLAG_CASTS_SHADOWS = 1 << 1;

    public static final int RAY_BUDGET_STRIDE = 6;
    public static final int RAY_BUDGET_PRIMARY_RAYS_PER_PIXEL_OFFSET = 0;
    public static final int RAY_BUDGET_SHADOW_RAYS_PER_HIT_OFFSET = 1;
    public static final int RAY_BUDGET_GI_RAYS_PER_HIT_OFFSET = 2;
    public static final int RAY_BUDGET_MAX_RAYS_PER_FRAME_OFFSET = 3;
    public static final int RAY_BUDGET_MAX_VISITED_VOXELS_PER_RAY_OFFSET = 4;
    public static final int RAY_BUDGET_MAX_VISITED_SECTIONS_PER_RAY_OFFSET = 5;

    public static final int CELESTIAL_LIGHT_DATA_STRIDE = 9;
    public static final int CELESTIAL_DIRECTION_X_OFFSET = 0;
    public static final int CELESTIAL_DIRECTION_Y_OFFSET = 1;
    public static final int CELESTIAL_DIRECTION_Z_OFFSET = 2;
    public static final int CELESTIAL_COLOR_RED_OFFSET = 3;
    public static final int CELESTIAL_COLOR_GREEN_OFFSET = 4;
    public static final int CELESTIAL_COLOR_BLUE_OFFSET = 5;
    public static final int CELESTIAL_ILLUMINANCE_OFFSET = 6;
    public static final int CELESTIAL_ANGULAR_RADIUS_OFFSET = 7;
    public static final int CELESTIAL_WEIGHTED_ENERGY_OFFSET = 8;

    public static final int EMISSIVE_LIGHT_METADATA_STRIDE = 5;
    public static final int EMISSIVE_BLOCK_X_OFFSET = 0;
    public static final int EMISSIVE_BLOCK_Y_OFFSET = 1;
    public static final int EMISSIVE_BLOCK_Z_OFFSET = 2;
    public static final int EMISSIVE_MATERIAL_ID_OFFSET = 3;
    public static final int EMISSIVE_BLOCK_LIGHT_LEVEL_OFFSET = 4;

    public static final int EMISSIVE_LIGHT_DATA_STRIDE = 6;
    public static final int EMISSIVE_COLOR_RED_OFFSET = 0;
    public static final int EMISSIVE_COLOR_GREEN_OFFSET = 1;
    public static final int EMISSIVE_COLOR_BLUE_OFFSET = 2;
    public static final int EMISSIVE_INTENSITY_OFFSET = 3;
    public static final int EMISSIVE_INFLUENCE_RADIUS_OFFSET = 4;
    public static final int EMISSIVE_LIGHT_ENERGY_OFFSET = 5;

    public static final int SHADOW_CANDIDATE_METADATA_STRIDE = 3;
    public static final int SHADOW_CANDIDATE_SOURCE_ID_OFFSET = 0;
    public static final int SHADOW_CANDIDATE_SAMPLE_INDEX_OFFSET = 1;
    public static final int SHADOW_CANDIDATE_CONTRIBUTES_OFFSET = 2;

    public static final int SHADOW_CANDIDATE_RAY_STRIDE = 9;
    public static final int SHADOW_RAY_ORIGIN_X_OFFSET = 0;
    public static final int SHADOW_RAY_ORIGIN_Y_OFFSET = 1;
    public static final int SHADOW_RAY_ORIGIN_Z_OFFSET = 2;
    public static final int SHADOW_RAY_DIRECTION_X_OFFSET = 3;
    public static final int SHADOW_RAY_DIRECTION_Y_OFFSET = 4;
    public static final int SHADOW_RAY_DIRECTION_Z_OFFSET = 5;
    public static final int SHADOW_RAY_MIN_DISTANCE_OFFSET = 6;
    public static final int SHADOW_RAY_MAX_DISTANCE_OFFSET = 7;
    public static final int SHADOW_RAY_CONTRIBUTION_WEIGHT_OFFSET = 8;

    public static final int SECTION_SNAPSHOT_METADATA_STRIDE = 15;
    public static final int SECTION_X_OFFSET = 0;
    public static final int SECTION_Y_OFFSET = 1;
    public static final int SECTION_Z_OFFSET = 2;
    public static final int SECTION_OCCUPIED_VOXEL_COUNT_OFFSET = 3;
    public static final int SECTION_OPAQUE_VOXEL_COUNT_OFFSET = 4;
    public static final int SECTION_TRANSLUCENT_VOXEL_COUNT_OFFSET = 5;
    public static final int SECTION_FLUID_VOXEL_COUNT_OFFSET = 6;
    public static final int SECTION_EMISSIVE_VOXEL_COUNT_OFFSET = 7;
    public static final int SECTION_OCCUPANCY_BIT_ORDER_ID_OFFSET = 8;
    public static final int SECTION_OCCUPANCY_MASK_WORD_OFFSET_OFFSET = 9;
    public static final int SECTION_OCCUPANCY_MASK_WORD_COUNT_OFFSET = 10;
    public static final int SECTION_OCCUPANCY_MASK_BIT_COUNT_OFFSET = 11;
    public static final int SECTION_MATERIAL_PALETTE_OFFSET_OFFSET = 12;
    public static final int SECTION_MATERIAL_PALETTE_SIZE_OFFSET = 13;
    public static final int SECTION_HAS_EMISSIVE_PAYLOAD_OFFSET = 14;

    public static final int SECTION_SNAPSHOT_GENERATION_STRIDE = 7;
    public static final int SECTION_GENERATION_OFFSET = 0;
    public static final int SECTION_MATERIAL_GENERATION_OFFSET = 1;
    public static final int SECTION_OCCUPANCY_GENERATION_OFFSET = 2;
    public static final int SECTION_EMISSIVE_GENERATION_OFFSET = 3;
    public static final int SECTION_OCCUPANCY_MASK_GENERATION_OFFSET = 4;
    public static final int SECTION_MATERIAL_PALETTE_GENERATION_OFFSET = 5;
    public static final int SECTION_COMBINED_GENERATION_OFFSET = 6;

    private static final int ALL_FLAGS = FLAG_VALIDATED
            | FLAG_HAS_DIRECT_LIGHTING_WORK
            | FLAG_READY_FOR_SHADOW_TRACING
            | FLAG_REQUIRES_OCCUPANCY_MASKS
            | FLAG_ALLOW_TRANSLUCENT_OCCLUDERS
            | FLAG_WORLD_TIME_AVAILABLE;

    private static final int ALL_LIGHT_FLAGS = LIGHT_FLAG_ENABLED | LIGHT_FLAG_CASTS_SHADOWS;

    public NativeDirectLightingUploadPacket {
        requireNonNegative(frameIndex, "frameIndex");
        requireNonNegative(generation, "generation");
        requireNonNegative(firstGeneration, "firstGeneration");
        requireNonNegative(lastGeneration, "lastGeneration");
        requireNonNegative(celestialGeneration, "celestialGeneration");
        requireNonNegative(emissiveGeneration, "emissiveGeneration");
        requireNonNegative(shadowGeneration, "shadowGeneration");
        requireNonNegative(shadowCandidateGeneration, "shadowCandidateGeneration");
        requireNonNegative(sectionSnapshotGeneration, "sectionSnapshotGeneration");
        if (firstGeneration > lastGeneration) {
            throw new IllegalArgumentException("firstGeneration must be less than or equal to lastGeneration");
        }
        if (generation < lastGeneration) {
            throw new IllegalArgumentException("generation must include lastGeneration");
        }
        dimensionId = requireText(dimensionId, "dimensionId");
        if ((flags & ~ALL_FLAGS) != 0) {
            throw new IllegalArgumentException("flags contains unknown direct lighting bits");
        }
        requireNonNegative(celestialLightCount, "celestialLightCount");
        requireNonNegativeFinite(celestialLightEnergy, "celestialLightEnergy");
        requireNonNegative(selectedEmissiveCount, "selectedEmissiveCount");
        requireNonNegativeFinite(selectedEmissiveEnergy, "selectedEmissiveEnergy");
        requireNonNegative(shadowCandidateCount, "shadowCandidateCount");
        requireNonNegative(budgetedShadowCandidateCount, "budgetedShadowCandidateCount");
        requireNonNegative(sectionSnapshotCount, "sectionSnapshotCount");
        if (celestialLightCount > 2) {
            throw new IllegalArgumentException("celestialLightCount cannot exceed sun and moon");
        }
        if (budgetedShadowCandidateCount > shadowCandidateCount) {
            throw new IllegalArgumentException("budgetedShadowCandidateCount cannot exceed shadowCandidateCount");
        }
        if (celestialLightCount == 0 && celestialLightEnergy != 0.0F) {
            throw new IllegalArgumentException("empty celestial payload must have zero energy");
        }
        if (selectedEmissiveCount == 0 && selectedEmissiveEnergy != 0.0F) {
            throw new IllegalArgumentException("empty emissive payload must have zero energy");
        }

        rayBudget = copy(rayBudget, "rayBudget");
        celestialLightSources = copy(celestialLightSources, "celestialLightSources");
        celestialLightFlags = copy(celestialLightFlags, "celestialLightFlags");
        celestialLightData = copy(celestialLightData, "celestialLightData");
        emissiveLightDimensions = copy(emissiveLightDimensions, "emissiveLightDimensions");
        emissiveLightMetadata = copy(emissiveLightMetadata, "emissiveLightMetadata");
        emissiveLightData = copy(emissiveLightData, "emissiveLightData");
        emissiveLightGenerations = copy(emissiveLightGenerations, "emissiveLightGenerations");
        shadowCandidateMetadata = copy(shadowCandidateMetadata, "shadowCandidateMetadata");
        shadowCandidateRays = copy(shadowCandidateRays, "shadowCandidateRays");
        shadowCandidateGenerations = copy(shadowCandidateGenerations, "shadowCandidateGenerations");
        sectionSnapshotDimensions = copy(sectionSnapshotDimensions, "sectionSnapshotDimensions");
        sectionSnapshotMetadata = copy(sectionSnapshotMetadata, "sectionSnapshotMetadata");
        sectionSnapshotGenerations = copy(sectionSnapshotGenerations, "sectionSnapshotGenerations");

        validateRayBudget(rayBudget);
        validateCelestialPayload(celestialLightCount, celestialLightSources, celestialLightFlags, celestialLightData);
        validateEmissivePayload(
                selectedEmissiveCount,
                emissiveLightDimensions,
                emissiveLightMetadata,
                emissiveLightData,
                emissiveLightGenerations
        );
        validateShadowCandidatePayload(
                shadowCandidateCount,
                shadowCandidateMetadata,
                shadowCandidateRays,
                shadowCandidateGenerations
        );
        validateSectionSnapshotPayload(
                sectionSnapshotCount,
                sectionSnapshotDimensions,
                sectionSnapshotMetadata,
                sectionSnapshotGenerations
        );
    }

    public static NativeDirectLightingUploadPacket from(DirectLightingPlan plan) {
        Objects.requireNonNull(plan, "plan");

        var celestialLighting = plan.celestialLighting();
        var emissiveBlockList = plan.emissiveBlockList();
        DirectShadowRayPlan shadowRayPlan = plan.shadowRayPlan();
        List<DirectCelestialLight> celestialLights = celestialLighting.activeLights();
        List<DirectEmissiveBlockLight> emissiveLights = emissiveBlockList.selectedLights();
        List<DirectShadowRayCandidate> shadowCandidates = shadowRayPlan.rayCandidates();
        List<VoxelSectionSnapshotReference> sectionSnapshots = shadowRayPlan.sectionSnapshots();

        int[] rayBudget = rayBudget(shadowRayPlan.rayBudget());
        int[] celestialLightSources = new int[celestialLights.size()];
        int[] celestialLightFlags = new int[celestialLights.size()];
        float[] celestialLightData = new float[checkedArrayLength(
                celestialLights.size(),
                CELESTIAL_LIGHT_DATA_STRIDE,
                "celestial light data"
        )];
        float celestialLightEnergy = fillCelestialPayload(
                celestialLights,
                celestialLightSources,
                celestialLightFlags,
                celestialLightData
        );

        String[] emissiveLightDimensions = new String[emissiveLights.size()];
        int[] emissiveLightMetadata = new int[checkedArrayLength(
                emissiveLights.size(),
                EMISSIVE_LIGHT_METADATA_STRIDE,
                "emissive light metadata"
        )];
        float[] emissiveLightData = new float[checkedArrayLength(
                emissiveLights.size(),
                EMISSIVE_LIGHT_DATA_STRIDE,
                "emissive light data"
        )];
        long[] emissiveLightGenerations = new long[emissiveLights.size()];
        float selectedEmissiveEnergy = fillEmissivePayload(
                emissiveLights,
                emissiveLightDimensions,
                emissiveLightMetadata,
                emissiveLightData,
                emissiveLightGenerations
        );

        int[] shadowCandidateMetadata = new int[checkedArrayLength(
                shadowCandidates.size(),
                SHADOW_CANDIDATE_METADATA_STRIDE,
                "shadow candidate metadata"
        )];
        float[] shadowCandidateRays = new float[checkedArrayLength(
                shadowCandidates.size(),
                SHADOW_CANDIDATE_RAY_STRIDE,
                "shadow candidate rays"
        )];
        long[] shadowCandidateGenerations = new long[shadowCandidates.size()];
        fillShadowCandidatePayload(
                shadowCandidates,
                shadowCandidateMetadata,
                shadowCandidateRays,
                shadowCandidateGenerations
        );

        String[] sectionSnapshotDimensions = new String[sectionSnapshots.size()];
        int[] sectionSnapshotMetadata = new int[checkedArrayLength(
                sectionSnapshots.size(),
                SECTION_SNAPSHOT_METADATA_STRIDE,
                "section snapshot metadata"
        )];
        long[] sectionSnapshotGenerations = new long[checkedArrayLength(
                sectionSnapshots.size(),
                SECTION_SNAPSHOT_GENERATION_STRIDE,
                "section snapshot generations"
        )];
        fillSectionSnapshotPayload(
                sectionSnapshots,
                sectionSnapshotDimensions,
                sectionSnapshotMetadata,
                sectionSnapshotGenerations
        );

        long celestialGeneration = celestialLighting.frameIndex();
        long emissiveGeneration = emissiveBlockList.generation();
        long shadowGeneration = shadowRayPlan.generation();
        long shadowCandidateGeneration = shadowRayPlan.maxCandidateGeneration();
        long sectionSnapshotGeneration = shadowRayPlan.maxSectionGeneration();
        GenerationBounds generationBounds = GenerationBounds.empty()
                .acceptPositive(plan.frameIndex())
                .acceptPositive(celestialGeneration)
                .acceptPositive(emissiveGeneration)
                .acceptPositive(shadowGeneration)
                .acceptPositive(shadowCandidateGeneration)
                .acceptPositive(sectionSnapshotGeneration)
                .acceptPositive(max(emissiveLightGenerations))
                .acceptPositive(max(shadowCandidateGenerations))
                .acceptPositive(maxSectionCombinedGeneration(sectionSnapshotGenerations));
        long generation = Math.max(plan.frameIndex(), generationBounds.lastGeneration());

        return new NativeDirectLightingUploadPacket(
                plan.frameIndex(),
                generation,
                generationBounds.firstGeneration(),
                generationBounds.lastGeneration(),
                celestialGeneration,
                emissiveGeneration,
                shadowGeneration,
                shadowCandidateGeneration,
                sectionSnapshotGeneration,
                celestialLighting.dimensionId(),
                flags(plan),
                celestialLights.size(),
                celestialLightEnergy,
                emissiveLights.size(),
                selectedEmissiveEnergy,
                shadowCandidates.size(),
                shadowRayPlan.budgetedCandidateCount(),
                sectionSnapshots.size(),
                rayBudget,
                celestialLightSources,
                celestialLightFlags,
                celestialLightData,
                emissiveLightDimensions,
                emissiveLightMetadata,
                emissiveLightData,
                emissiveLightGenerations,
                shadowCandidateMetadata,
                shadowCandidateRays,
                shadowCandidateGenerations,
                sectionSnapshotDimensions,
                sectionSnapshotMetadata,
                sectionSnapshotGenerations
        );
    }

    public boolean valid() {
        return hasFlag(FLAG_VALIDATED);
    }

    public boolean hasDirectLightingWork() {
        return hasFlag(FLAG_HAS_DIRECT_LIGHTING_WORK);
    }

    public boolean readyForShadowTracing() {
        return hasFlag(FLAG_READY_FOR_SHADOW_TRACING);
    }

    public boolean requireOccupancyMasks() {
        return hasFlag(FLAG_REQUIRES_OCCUPANCY_MASKS);
    }

    public boolean allowTranslucentOccluders() {
        return hasFlag(FLAG_ALLOW_TRANSLUCENT_OCCLUDERS);
    }

    public boolean worldTimeAvailable() {
        return hasFlag(FLAG_WORLD_TIME_AVAILABLE);
    }

    public boolean hasPayloads() {
        return this.celestialLightCount > 0
                || this.selectedEmissiveCount > 0
                || this.shadowCandidateCount > 0
                || this.sectionSnapshotCount > 0;
    }

    @Override
    public int[] rayBudget() {
        return copy(this.rayBudget, "rayBudget");
    }

    @Override
    public int[] celestialLightSources() {
        return copy(this.celestialLightSources, "celestialLightSources");
    }

    @Override
    public int[] celestialLightFlags() {
        return copy(this.celestialLightFlags, "celestialLightFlags");
    }

    @Override
    public float[] celestialLightData() {
        return copy(this.celestialLightData, "celestialLightData");
    }

    @Override
    public String[] emissiveLightDimensions() {
        return copy(this.emissiveLightDimensions, "emissiveLightDimensions");
    }

    @Override
    public int[] emissiveLightMetadata() {
        return copy(this.emissiveLightMetadata, "emissiveLightMetadata");
    }

    @Override
    public float[] emissiveLightData() {
        return copy(this.emissiveLightData, "emissiveLightData");
    }

    @Override
    public long[] emissiveLightGenerations() {
        return copy(this.emissiveLightGenerations, "emissiveLightGenerations");
    }

    @Override
    public int[] shadowCandidateMetadata() {
        return copy(this.shadowCandidateMetadata, "shadowCandidateMetadata");
    }

    @Override
    public float[] shadowCandidateRays() {
        return copy(this.shadowCandidateRays, "shadowCandidateRays");
    }

    @Override
    public long[] shadowCandidateGenerations() {
        return copy(this.shadowCandidateGenerations, "shadowCandidateGenerations");
    }

    @Override
    public String[] sectionSnapshotDimensions() {
        return copy(this.sectionSnapshotDimensions, "sectionSnapshotDimensions");
    }

    @Override
    public int[] sectionSnapshotMetadata() {
        return copy(this.sectionSnapshotMetadata, "sectionSnapshotMetadata");
    }

    @Override
    public long[] sectionSnapshotGenerations() {
        return copy(this.sectionSnapshotGenerations, "sectionSnapshotGenerations");
    }

    private boolean hasFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    private static int flags(DirectLightingPlan plan) {
        DirectShadowRayPlan shadowRayPlan = plan.shadowRayPlan();
        int flags = 0;
        if (plan.valid()) {
            flags |= FLAG_VALIDATED;
        }
        if (plan.hasDirectLightingWork()) {
            flags |= FLAG_HAS_DIRECT_LIGHTING_WORK;
        }
        if (plan.readyForShadowPlanning()) {
            flags |= FLAG_READY_FOR_SHADOW_TRACING;
        }
        if (shadowRayPlan.requireOccupancyMasks()) {
            flags |= FLAG_REQUIRES_OCCUPANCY_MASKS;
        }
        if (shadowRayPlan.allowTranslucentOccluders()) {
            flags |= FLAG_ALLOW_TRANSLUCENT_OCCLUDERS;
        }
        if (plan.celestialLighting().worldTimeAvailable()) {
            flags |= FLAG_WORLD_TIME_AVAILABLE;
        }
        return flags;
    }

    private static int[] rayBudget(VoxelRayBudgetConfig rayBudget) {
        Objects.requireNonNull(rayBudget, "rayBudget");
        int[] values = new int[RAY_BUDGET_STRIDE];
        values[RAY_BUDGET_PRIMARY_RAYS_PER_PIXEL_OFFSET] = rayBudget.primaryRaysPerPixel();
        values[RAY_BUDGET_SHADOW_RAYS_PER_HIT_OFFSET] = rayBudget.shadowRaysPerHit();
        values[RAY_BUDGET_GI_RAYS_PER_HIT_OFFSET] = rayBudget.giRaysPerHit();
        values[RAY_BUDGET_MAX_RAYS_PER_FRAME_OFFSET] = rayBudget.maxRaysPerFrame();
        values[RAY_BUDGET_MAX_VISITED_VOXELS_PER_RAY_OFFSET] = rayBudget.maxVisitedVoxelsPerRay();
        values[RAY_BUDGET_MAX_VISITED_SECTIONS_PER_RAY_OFFSET] = rayBudget.maxVisitedSectionsPerRay();
        return values;
    }

    private static float fillCelestialPayload(
            List<DirectCelestialLight> lights,
            int[] sources,
            int[] lightFlags,
            float[] lightData
    ) {
        float energy = 0.0F;
        for (int index = 0; index < lights.size(); index++) {
            DirectCelestialLight light = Objects.requireNonNull(lights.get(index), "celestial lights must not contain null entries");
            int dataOffset = index * CELESTIAL_LIGHT_DATA_STRIDE;
            var direction = light.direction();
            var color = light.color();
            sources[index] = sourceId(light.source());
            lightFlags[index] = (light.enabled() ? LIGHT_FLAG_ENABLED : 0)
                    | (light.castsShadows() ? LIGHT_FLAG_CASTS_SHADOWS : 0);
            lightData[dataOffset + CELESTIAL_DIRECTION_X_OFFSET] = direction.x();
            lightData[dataOffset + CELESTIAL_DIRECTION_Y_OFFSET] = direction.y();
            lightData[dataOffset + CELESTIAL_DIRECTION_Z_OFFSET] = direction.z();
            lightData[dataOffset + CELESTIAL_COLOR_RED_OFFSET] = color.red();
            lightData[dataOffset + CELESTIAL_COLOR_GREEN_OFFSET] = color.green();
            lightData[dataOffset + CELESTIAL_COLOR_BLUE_OFFSET] = color.blue();
            lightData[dataOffset + CELESTIAL_ILLUMINANCE_OFFSET] = light.illuminance();
            lightData[dataOffset + CELESTIAL_ANGULAR_RADIUS_OFFSET] = light.angularRadiusRadians();
            lightData[dataOffset + CELESTIAL_WEIGHTED_ENERGY_OFFSET] = light.weightedEnergy();
            energy += light.weightedEnergy();
        }
        return energy;
    }

    private static float fillEmissivePayload(
            List<DirectEmissiveBlockLight> lights,
            String[] dimensions,
            int[] metadata,
            float[] lightData,
            long[] generations
    ) {
        float energy = 0.0F;
        for (int index = 0; index < lights.size(); index++) {
            DirectEmissiveBlockLight light = Objects.requireNonNull(lights.get(index), "emissive lights must not contain null entries");
            int metadataOffset = index * EMISSIVE_LIGHT_METADATA_STRIDE;
            int dataOffset = index * EMISSIVE_LIGHT_DATA_STRIDE;
            var color = light.color();
            float lightEnergy = light.intensity() * color.luminance();

            dimensions[index] = light.dimension();
            metadata[metadataOffset + EMISSIVE_BLOCK_X_OFFSET] = light.blockX();
            metadata[metadataOffset + EMISSIVE_BLOCK_Y_OFFSET] = light.blockY();
            metadata[metadataOffset + EMISSIVE_BLOCK_Z_OFFSET] = light.blockZ();
            metadata[metadataOffset + EMISSIVE_MATERIAL_ID_OFFSET] = light.materialId();
            metadata[metadataOffset + EMISSIVE_BLOCK_LIGHT_LEVEL_OFFSET] = light.blockLightLevel();
            lightData[dataOffset + EMISSIVE_COLOR_RED_OFFSET] = color.red();
            lightData[dataOffset + EMISSIVE_COLOR_GREEN_OFFSET] = color.green();
            lightData[dataOffset + EMISSIVE_COLOR_BLUE_OFFSET] = color.blue();
            lightData[dataOffset + EMISSIVE_INTENSITY_OFFSET] = light.intensity();
            lightData[dataOffset + EMISSIVE_INFLUENCE_RADIUS_OFFSET] = light.influenceRadiusBlocks();
            lightData[dataOffset + EMISSIVE_LIGHT_ENERGY_OFFSET] = lightEnergy;
            generations[index] = light.generation();
            energy += lightEnergy;
        }
        return energy;
    }

    private static void fillShadowCandidatePayload(
            List<DirectShadowRayCandidate> candidates,
            int[] metadata,
            float[] rays,
            long[] generations
    ) {
        for (int index = 0; index < candidates.size(); index++) {
            DirectShadowRayCandidate candidate = Objects.requireNonNull(
                    candidates.get(index),
                    "shadow candidates must not contain null entries"
            );
            VoxelRay ray = candidate.ray();
            int metadataOffset = index * SHADOW_CANDIDATE_METADATA_STRIDE;
            int rayOffset = index * SHADOW_CANDIDATE_RAY_STRIDE;
            metadata[metadataOffset + SHADOW_CANDIDATE_SOURCE_ID_OFFSET] = sourceId(candidate.source());
            metadata[metadataOffset + SHADOW_CANDIDATE_SAMPLE_INDEX_OFFSET] = candidate.sampleIndex();
            metadata[metadataOffset + SHADOW_CANDIDATE_CONTRIBUTES_OFFSET] = candidate.contributesLighting() ? 1 : 0;
            rays[rayOffset + SHADOW_RAY_ORIGIN_X_OFFSET] = ray.originX();
            rays[rayOffset + SHADOW_RAY_ORIGIN_Y_OFFSET] = ray.originY();
            rays[rayOffset + SHADOW_RAY_ORIGIN_Z_OFFSET] = ray.originZ();
            rays[rayOffset + SHADOW_RAY_DIRECTION_X_OFFSET] = ray.directionX();
            rays[rayOffset + SHADOW_RAY_DIRECTION_Y_OFFSET] = ray.directionY();
            rays[rayOffset + SHADOW_RAY_DIRECTION_Z_OFFSET] = ray.directionZ();
            rays[rayOffset + SHADOW_RAY_MIN_DISTANCE_OFFSET] = ray.minDistance();
            rays[rayOffset + SHADOW_RAY_MAX_DISTANCE_OFFSET] = ray.maxDistance();
            rays[rayOffset + SHADOW_RAY_CONTRIBUTION_WEIGHT_OFFSET] = candidate.contributionWeight();
            generations[index] = candidate.sourceGeneration();
        }
    }

    private static void fillSectionSnapshotPayload(
            List<VoxelSectionSnapshotReference> sections,
            String[] dimensions,
            int[] metadata,
            long[] generations
    ) {
        for (int index = 0; index < sections.size(); index++) {
            VoxelSectionSnapshotReference section = Objects.requireNonNull(
                    sections.get(index),
                    "section snapshots must not contain null entries"
            );
            ChunkSectionOrigin origin = section.origin();
            ChunkSectionGeneration generation = section.generation();
            int metadataOffset = index * SECTION_SNAPSHOT_METADATA_STRIDE;
            int generationOffset = index * SECTION_SNAPSHOT_GENERATION_STRIDE;

            dimensions[index] = origin.dimension();
            metadata[metadataOffset + SECTION_X_OFFSET] = origin.sectionX();
            metadata[metadataOffset + SECTION_Y_OFFSET] = origin.sectionY();
            metadata[metadataOffset + SECTION_Z_OFFSET] = origin.sectionZ();
            metadata[metadataOffset + SECTION_OCCUPIED_VOXEL_COUNT_OFFSET] = section.occupiedVoxelCount();
            metadata[metadataOffset + SECTION_OPAQUE_VOXEL_COUNT_OFFSET] = section.opaqueVoxelCount();
            metadata[metadataOffset + SECTION_TRANSLUCENT_VOXEL_COUNT_OFFSET] = section.translucentVoxelCount();
            metadata[metadataOffset + SECTION_FLUID_VOXEL_COUNT_OFFSET] = section.fluidVoxelCount();
            metadata[metadataOffset + SECTION_EMISSIVE_VOXEL_COUNT_OFFSET] = section.emissiveVoxelCount();
            metadata[metadataOffset + SECTION_OCCUPANCY_BIT_ORDER_ID_OFFSET] = section.occupancyBitOrder().ordinal() + 1;
            metadata[metadataOffset + SECTION_OCCUPANCY_MASK_WORD_OFFSET_OFFSET] = section.occupancyMaskWordOffset();
            metadata[metadataOffset + SECTION_OCCUPANCY_MASK_WORD_COUNT_OFFSET] = section.occupancyMaskWordCount();
            metadata[metadataOffset + SECTION_OCCUPANCY_MASK_BIT_COUNT_OFFSET] = section.occupancyMaskBitCount();
            metadata[metadataOffset + SECTION_MATERIAL_PALETTE_OFFSET_OFFSET] = section.materialPaletteOffset();
            metadata[metadataOffset + SECTION_MATERIAL_PALETTE_SIZE_OFFSET] = section.materialPaletteSize();
            metadata[metadataOffset + SECTION_HAS_EMISSIVE_PAYLOAD_OFFSET] = section.hasEmissivePayload() ? 1 : 0;
            generations[generationOffset + SECTION_GENERATION_OFFSET] = generation.sectionGeneration();
            generations[generationOffset + SECTION_MATERIAL_GENERATION_OFFSET] = generation.materialGeneration();
            generations[generationOffset + SECTION_OCCUPANCY_GENERATION_OFFSET] = generation.occupancyGeneration();
            generations[generationOffset + SECTION_EMISSIVE_GENERATION_OFFSET] = generation.emissiveGeneration();
            generations[generationOffset + SECTION_OCCUPANCY_MASK_GENERATION_OFFSET] = section.occupancyMaskGeneration();
            generations[generationOffset + SECTION_MATERIAL_PALETTE_GENERATION_OFFSET] = section.materialGeneration();
            generations[generationOffset + SECTION_COMBINED_GENERATION_OFFSET] = section.combinedGeneration();
        }
    }

    private static void validateRayBudget(int[] rayBudget) {
        requireMatchingLength(RAY_BUDGET_STRIDE, "rayBudget", rayBudget.length);
        requireNonNegative(rayBudget[RAY_BUDGET_PRIMARY_RAYS_PER_PIXEL_OFFSET], "primaryRaysPerPixel");
        requireNonNegative(rayBudget[RAY_BUDGET_SHADOW_RAYS_PER_HIT_OFFSET], "shadowRaysPerHit");
        requireNonNegative(rayBudget[RAY_BUDGET_GI_RAYS_PER_HIT_OFFSET], "giRaysPerHit");
        requirePositive(rayBudget[RAY_BUDGET_MAX_RAYS_PER_FRAME_OFFSET], "maxRaysPerFrame");
        requirePositive(rayBudget[RAY_BUDGET_MAX_VISITED_VOXELS_PER_RAY_OFFSET], "maxVisitedVoxelsPerRay");
        requirePositive(rayBudget[RAY_BUDGET_MAX_VISITED_SECTIONS_PER_RAY_OFFSET], "maxVisitedSectionsPerRay");
    }

    private static void validateCelestialPayload(
            int count,
            int[] sources,
            int[] flags,
            float[] lightData
    ) {
        requireMatchingLength(count, "celestialLightSources", sources.length);
        requireMatchingLength(count, "celestialLightFlags", flags.length);
        requireMatchingLength(
                checkedArrayLength(count, CELESTIAL_LIGHT_DATA_STRIDE, "celestialLightData"),
                "celestialLightData",
                lightData.length
        );
        for (int index = 0; index < count; index++) {
            requireCelestialSourceId(sources[index]);
            if ((flags[index] & ~ALL_LIGHT_FLAGS) != 0) {
                throw new IllegalArgumentException("celestialLightFlags contains unknown bits");
            }
            int dataOffset = index * CELESTIAL_LIGHT_DATA_STRIDE;
            requireFinitePayload(lightData, dataOffset, CELESTIAL_LIGHT_DATA_STRIDE, "celestialLightData");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_COLOR_RED_OFFSET], "celestial color red");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_COLOR_GREEN_OFFSET], "celestial color green");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_COLOR_BLUE_OFFSET], "celestial color blue");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_ILLUMINANCE_OFFSET], "celestial illuminance");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_ANGULAR_RADIUS_OFFSET], "celestial angular radius");
            requireNonNegativeFinite(lightData[dataOffset + CELESTIAL_WEIGHTED_ENERGY_OFFSET], "celestial weighted energy");
        }
    }

    private static void validateEmissivePayload(
            int count,
            String[] dimensions,
            int[] metadata,
            float[] lightData,
            long[] generations
    ) {
        requireMatchingLength(count, "emissiveLightDimensions", dimensions.length);
        requireMatchingLength(
                checkedArrayLength(count, EMISSIVE_LIGHT_METADATA_STRIDE, "emissiveLightMetadata"),
                "emissiveLightMetadata",
                metadata.length
        );
        requireMatchingLength(
                checkedArrayLength(count, EMISSIVE_LIGHT_DATA_STRIDE, "emissiveLightData"),
                "emissiveLightData",
                lightData.length
        );
        requireMatchingLength(count, "emissiveLightGenerations", generations.length);
        for (int index = 0; index < count; index++) {
            dimensions[index] = requireText(dimensions[index], "emissiveLightDimensions entries");
            int metadataOffset = index * EMISSIVE_LIGHT_METADATA_STRIDE;
            if (metadata[metadataOffset + EMISSIVE_MATERIAL_ID_OFFSET] <= 0) {
                throw new IllegalArgumentException("emissive material ids must be positive");
            }
            int blockLightLevel = metadata[metadataOffset + EMISSIVE_BLOCK_LIGHT_LEVEL_OFFSET];
            if (blockLightLevel < 0 || blockLightLevel > 15) {
                throw new IllegalArgumentException("emissive block light levels must be between 0 and 15");
            }
            int dataOffset = index * EMISSIVE_LIGHT_DATA_STRIDE;
            requireFinitePayload(lightData, dataOffset, EMISSIVE_LIGHT_DATA_STRIDE, "emissiveLightData");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_COLOR_RED_OFFSET], "emissive color red");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_COLOR_GREEN_OFFSET], "emissive color green");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_COLOR_BLUE_OFFSET], "emissive color blue");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_INTENSITY_OFFSET], "emissive intensity");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_INFLUENCE_RADIUS_OFFSET], "emissive influence radius");
            requireNonNegativeFinite(lightData[dataOffset + EMISSIVE_LIGHT_ENERGY_OFFSET], "emissive light energy");
            requireNonNegative(generations[index], "emissiveLightGenerations entries");
        }
    }

    private static void validateShadowCandidatePayload(
            int count,
            int[] metadata,
            float[] rays,
            long[] generations
    ) {
        requireMatchingLength(
                checkedArrayLength(count, SHADOW_CANDIDATE_METADATA_STRIDE, "shadowCandidateMetadata"),
                "shadowCandidateMetadata",
                metadata.length
        );
        requireMatchingLength(
                checkedArrayLength(count, SHADOW_CANDIDATE_RAY_STRIDE, "shadowCandidateRays"),
                "shadowCandidateRays",
                rays.length
        );
        requireMatchingLength(count, "shadowCandidateGenerations", generations.length);
        for (int index = 0; index < count; index++) {
            int metadataOffset = index * SHADOW_CANDIDATE_METADATA_STRIDE;
            requireShadowSourceId(metadata[metadataOffset + SHADOW_CANDIDATE_SOURCE_ID_OFFSET]);
            requireNonNegative(metadata[metadataOffset + SHADOW_CANDIDATE_SAMPLE_INDEX_OFFSET], "shadow candidate sample index");
            requireBooleanInt(metadata[metadataOffset + SHADOW_CANDIDATE_CONTRIBUTES_OFFSET], "shadow candidate contributes flag");
            int rayOffset = index * SHADOW_CANDIDATE_RAY_STRIDE;
            requireFinitePayload(rays, rayOffset, SHADOW_CANDIDATE_RAY_STRIDE, "shadowCandidateRays");
            if (rays[rayOffset + SHADOW_RAY_MIN_DISTANCE_OFFSET] < 0.0F) {
                throw new IllegalArgumentException("shadow ray min distance must be non-negative");
            }
            if (rays[rayOffset + SHADOW_RAY_MAX_DISTANCE_OFFSET] <= rays[rayOffset + SHADOW_RAY_MIN_DISTANCE_OFFSET]) {
                throw new IllegalArgumentException("shadow ray max distance must be greater than min distance");
            }
            requireNonNegativeFinite(rays[rayOffset + SHADOW_RAY_CONTRIBUTION_WEIGHT_OFFSET], "shadow ray contribution weight");
            requireNonNegative(generations[index], "shadowCandidateGenerations entries");
        }
    }

    private static void validateSectionSnapshotPayload(
            int count,
            String[] dimensions,
            int[] metadata,
            long[] generations
    ) {
        requireMatchingLength(count, "sectionSnapshotDimensions", dimensions.length);
        requireMatchingLength(
                checkedArrayLength(count, SECTION_SNAPSHOT_METADATA_STRIDE, "sectionSnapshotMetadata"),
                "sectionSnapshotMetadata",
                metadata.length
        );
        requireMatchingLength(
                checkedArrayLength(count, SECTION_SNAPSHOT_GENERATION_STRIDE, "sectionSnapshotGenerations"),
                "sectionSnapshotGenerations",
                generations.length
        );
        for (int index = 0; index < count; index++) {
            dimensions[index] = requireText(dimensions[index], "sectionSnapshotDimensions entries");
            int metadataOffset = index * SECTION_SNAPSHOT_METADATA_STRIDE;
            requireVoxelCount(metadata[metadataOffset + SECTION_OCCUPIED_VOXEL_COUNT_OFFSET], "section occupied voxels");
            requireVoxelCount(metadata[metadataOffset + SECTION_OPAQUE_VOXEL_COUNT_OFFSET], "section opaque voxels");
            requireVoxelCount(metadata[metadataOffset + SECTION_TRANSLUCENT_VOXEL_COUNT_OFFSET], "section translucent voxels");
            requireVoxelCount(metadata[metadataOffset + SECTION_FLUID_VOXEL_COUNT_OFFSET], "section fluid voxels");
            requireVoxelCount(metadata[metadataOffset + SECTION_EMISSIVE_VOXEL_COUNT_OFFSET], "section emissive voxels");
            if (metadata[metadataOffset + SECTION_OCCUPANCY_BIT_ORDER_ID_OFFSET] <= 0) {
                throw new IllegalArgumentException("section occupancy bit order ids must be positive");
            }
            requireNonNegative(metadata[metadataOffset + SECTION_OCCUPANCY_MASK_WORD_OFFSET_OFFSET], "section occupancy word offset");
            int occupancyWordCount = metadata[metadataOffset + SECTION_OCCUPANCY_MASK_WORD_COUNT_OFFSET];
            if (occupancyWordCount < 0 || occupancyWordCount > VoxelOccupancyMaskMetadata.SECTION_MASK_WORD_COUNT) {
                throw new IllegalArgumentException("section occupancy word count is outside the supported range");
            }
            requireVoxelCount(metadata[metadataOffset + SECTION_OCCUPANCY_MASK_BIT_COUNT_OFFSET], "section occupancy bits");
            requireNonNegative(metadata[metadataOffset + SECTION_MATERIAL_PALETTE_OFFSET_OFFSET], "section material palette offset");
            requireNonNegative(metadata[metadataOffset + SECTION_MATERIAL_PALETTE_SIZE_OFFSET], "section material palette size");
            requireBooleanInt(metadata[metadataOffset + SECTION_HAS_EMISSIVE_PAYLOAD_OFFSET], "section emissive payload flag");
            int generationOffset = index * SECTION_SNAPSHOT_GENERATION_STRIDE;
            for (int generationIndex = 0; generationIndex < SECTION_SNAPSHOT_GENERATION_STRIDE; generationIndex++) {
                requireNonNegative(generations[generationOffset + generationIndex], "sectionSnapshotGenerations entries");
            }
        }
    }

    private static int sourceId(DirectCelestialLightSource source) {
        Objects.requireNonNull(source, "source");
        return switch (source) {
            case SUN -> SOURCE_SUN;
            case MOON -> SOURCE_MOON;
        };
    }

    private static int sourceId(DirectShadowRaySource source) {
        Objects.requireNonNull(source, "source");
        return switch (source) {
            case SUN -> SOURCE_SUN;
            case MOON -> SOURCE_MOON;
            case EMISSIVE_BLOCK -> SOURCE_EMISSIVE_BLOCK;
        };
    }

    private static long max(long[] values) {
        long max = 0L;
        for (long value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static long maxSectionCombinedGeneration(long[] values) {
        long max = 0L;
        for (int index = SECTION_COMBINED_GENERATION_OFFSET; index < values.length; index += SECTION_SNAPSHOT_GENERATION_STRIDE) {
            max = Math.max(max, values[index]);
        }
        return max;
    }

    private static int checkedArrayLength(int count, int stride, String name) {
        requireNonNegative(count, name + " count");
        requirePositive(stride, name + " stride");
        try {
            return Math.multiplyExact(count, stride);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " exceeds supported packet array length", exception);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireNonNegativeFinite(float value, String name) {
        requireFinite(value, name);
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireFinitePayload(float[] values, int offset, int stride, String name) {
        for (int index = 0; index < stride; index++) {
            requireFinite(values[offset + index], name + " entries");
        }
    }

    private static void requireVoxelCount(int value, String name) {
        if (value < 0 || value > ChunkSectionOrigin.SECTION_VOLUME) {
            throw new IllegalArgumentException(name + " must be between 0 and " + ChunkSectionOrigin.SECTION_VOLUME);
        }
    }

    private static void requireBooleanInt(int value, String name) {
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException(name + " must be 0 or 1");
        }
    }

    private static void requireCelestialSourceId(int sourceId) {
        if (sourceId != SOURCE_SUN && sourceId != SOURCE_MOON) {
            throw new IllegalArgumentException("celestial source ids must be sun or moon");
        }
    }

    private static void requireShadowSourceId(int sourceId) {
        if (sourceId != SOURCE_SUN && sourceId != SOURCE_MOON && sourceId != SOURCE_EMISSIVE_BLOCK) {
            throw new IllegalArgumentException("shadow source ids must be sun, moon, or emissive block");
        }
    }

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static long[] copy(long[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static float[] copy(float[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private record GenerationBounds(long firstGeneration, long lastGeneration, boolean hasValue) {
        private static GenerationBounds empty() {
            return new GenerationBounds(0L, 0L, false);
        }

        private GenerationBounds acceptPositive(long generation) {
            if (generation <= 0L) {
                return this;
            }
            if (!this.hasValue) {
                return new GenerationBounds(generation, generation, true);
            }
            return new GenerationBounds(
                    Math.min(this.firstGeneration, generation),
                    Math.max(this.lastGeneration, generation),
                    true
            );
        }
    }
}
