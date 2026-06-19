package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelRayBudgetConfig;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.SectionSurfaceSampleMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Builds deterministic direct-shadow ray metadata from captured lighting and voxel-section references.
 *
 * <p>This planner only selects bounded candidate rays for later native traversal. It does not perform
 * visibility tests, occlusion queries, or GPU tracing.</p>
 */
public final class DirectShadowRayPlanner {
    private static final int DEFAULT_MAX_SHADOW_RAYS_PER_FRAME = 64;
    private static final int DEFAULT_MAX_VISITED_VOXELS_PER_RAY = 512;
    private static final int DEFAULT_MAX_VISITED_SECTIONS_PER_RAY = 64;
    private static final float SECTION_CENTER_OFFSET_BLOCKS = ChunkSectionOrigin.SECTION_EDGE_LENGTH * 0.5F;
    private static final double SECTION_HALF_DIAGONAL_BLOCKS = Math.sqrt(3.0D) * SECTION_CENTER_OFFSET_BLOCKS;
    private static final float MIN_EMISSIVE_SAMPLE_DISTANCE_SQUARED = 0.0001F;
    private static final float BLOCK_CENTER_OFFSET = 0.5F;

    private static final Comparator<VoxelSectionSnapshotReference> SECTION_REFERENCE_DETAIL_ORDER = Comparator
            .comparingLong(VoxelSectionSnapshotReference::combinedGeneration)
            .thenComparingLong(section -> section.generation().sectionGeneration())
            .thenComparingLong(section -> section.generation().materialGeneration())
            .thenComparingLong(section -> section.generation().occupancyGeneration())
            .thenComparingLong(section -> section.generation().emissiveGeneration())
            .thenComparingLong(VoxelSectionSnapshotReference::occupancyMaskGeneration)
            .thenComparingLong(VoxelSectionSnapshotReference::materialGeneration)
            .thenComparingInt(VoxelSectionSnapshotReference::occupiedVoxelCount)
            .thenComparingInt(VoxelSectionSnapshotReference::opaqueVoxelCount)
            .thenComparingInt(VoxelSectionSnapshotReference::translucentVoxelCount)
            .thenComparingInt(VoxelSectionSnapshotReference::fluidVoxelCount)
            .thenComparingInt(VoxelSectionSnapshotReference::emissiveVoxelCount)
            .thenComparingInt(VoxelSectionSnapshotReference::occupancyMaskWordOffset)
            .thenComparingInt(VoxelSectionSnapshotReference::occupancyMaskWordCount)
            .thenComparingInt(VoxelSectionSnapshotReference::occupancyMaskBitCount)
            .thenComparingInt(VoxelSectionSnapshotReference::materialPaletteOffset)
            .thenComparingInt(VoxelSectionSnapshotReference::materialPaletteSize)
            .thenComparing(section -> section.occupancyBitOrder().name())
            .thenComparing(VoxelSectionSnapshotReference::hasEmissivePayload);

    private DirectShadowRayPlanner() {
    }

    public static VoxelRayBudgetConfig conservativeDefaultBudget() {
        return new VoxelRayBudgetConfig(
                0,
                1,
                0,
                DEFAULT_MAX_SHADOW_RAYS_PER_FRAME,
                DEFAULT_MAX_VISITED_VOXELS_PER_RAY,
                DEFAULT_MAX_VISITED_SECTIONS_PER_RAY
        );
    }

    public static DirectShadowRayPlan plan(
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        return plan(celestialLighting, emissiveBlockList, conservativeDefaultBudget(), sectionSnapshots);
    }

    public static DirectShadowRayPlan plan(
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList,
            VoxelRayBudgetConfig rayBudget,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        Objects.requireNonNull(celestialLighting, "celestialLighting");
        Objects.requireNonNull(emissiveBlockList, "emissiveBlockList");
        Objects.requireNonNull(rayBudget, "rayBudget");

        List<VoxelSectionSnapshotReference> stableSections = stableTraversalSections(sectionSnapshots);
        if (stableSections.isEmpty() || rayBudget.shadowRaysPerHit() == 0) {
            return DirectShadowRayPlan.fromCandidates(
                    celestialLighting.frameIndex(),
                    List.of(),
                    rayBudget,
                    stableSections
            );
        }

        List<DirectCelestialLight> celestialLights = activeCelestialShadowCasters(celestialLighting);
        List<DirectEmissiveBlockLight> emissiveLights = selectedEmissiveLights(emissiveBlockList);
        if (celestialLights.isEmpty() && emissiveLights.isEmpty()) {
            return DirectShadowRayPlan.fromCandidates(
                    celestialLighting.frameIndex(),
                    List.of(),
                    rayBudget,
                    stableSections
            );
        }

        int candidateLimit = rayBudget.maxRaysPerFrame();
        float celestialMaxDistance = celestialMaxDistance(rayBudget);
        List<DirectShadowRayCandidate> candidates = new ArrayList<>(Math.min(candidateLimit, stableSections.size()));

        for (int sectionIndex = 0; sectionIndex < stableSections.size() && candidates.size() < candidateLimit; sectionIndex++) {
            VoxelSectionSnapshotReference section = stableSections.get(sectionIndex);
            float originX = sectionCenterX(section);
            float originY = sectionCenterY(section);
            float originZ = sectionCenterZ(section);

            for (DirectCelestialLight light : celestialLights) {
                if (candidates.size() >= candidateLimit) {
                    break;
                }
                if (!matchesCelestialDimension(celestialLighting, section)) {
                    continue;
                }
                candidates.add(DirectShadowRayCandidate.forCelestialLight(
                        light,
                        originX,
                        originY,
                        originZ,
                        celestialMaxDistance,
                        celestialLighting.frameIndex(),
                        sectionIndex
                ));
            }

            for (DirectEmissiveBlockLight light : emissiveLights) {
                if (candidates.size() >= candidateLimit) {
                    break;
                }
                SampleOrigin emissiveOrigin = emissiveSurfaceSampleOrigin(light, section);
                if (emissiveOrigin == null || !canSampleEmissiveLight(
                        light,
                        section,
                        emissiveOrigin.x(),
                        emissiveOrigin.y(),
                        emissiveOrigin.z()
                )) {
                    continue;
                }
                candidates.add(DirectShadowRayCandidate.forEmissiveBlock(
                        light,
                        emissiveOrigin.x(),
                        emissiveOrigin.y(),
                        emissiveOrigin.z(),
                        sectionIndex
                ));
            }
        }

        return DirectShadowRayPlan.fromCandidates(
                celestialLighting.frameIndex(),
                candidates,
                rayBudget,
                stableSections
        );
    }

    private static List<VoxelSectionSnapshotReference> stableTraversalSections(
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        NavigableMap<String, VoxelSectionSnapshotReference> sectionsByStableKey = new TreeMap<>();
        for (VoxelSectionSnapshotReference sectionSnapshot : sectionSnapshots) {
            Objects.requireNonNull(sectionSnapshot, "sectionSnapshots must not contain null entries");
            if (!usableForTraversal(sectionSnapshot)) {
                continue;
            }
            sectionsByStableKey.merge(
                    sectionSnapshot.stableKey(),
                    sectionSnapshot,
                    DirectShadowRayPlanner::newerSectionReference
            );
        }
        return List.copyOf(sectionsByStableKey.values());
    }

    private static VoxelSectionSnapshotReference newerSectionReference(
            VoxelSectionSnapshotReference existing,
            VoxelSectionSnapshotReference candidate
    ) {
        return SECTION_REFERENCE_DETAIL_ORDER.compare(candidate, existing) >= 0 ? candidate : existing;
    }

    private static boolean usableForTraversal(VoxelSectionSnapshotReference sectionSnapshot) {
        return sectionSnapshot.hasTraversalPayload()
                && (!sectionSnapshot.hasOccupiedVoxels() || sectionSnapshot.hasOccupancyMask());
    }

    private static List<DirectCelestialLight> activeCelestialShadowCasters(DirectCelestialLightingPlan celestialLighting) {
        List<DirectCelestialLight> lights = new ArrayList<>(2);
        if (celestialLighting.sun().castsEffectiveShadows()) {
            lights.add(celestialLighting.sun());
        }
        if (celestialLighting.moon().castsEffectiveShadows()) {
            lights.add(celestialLighting.moon());
        }
        lights.sort(Comparator.comparingInt(light -> light.source().ordinal()));
        return List.copyOf(lights);
    }

    private static List<DirectEmissiveBlockLight> selectedEmissiveLights(DirectEmissiveBlockListPlan emissiveBlockList) {
        return emissiveBlockList.selectedLights().stream()
                .filter(DirectEmissiveBlockLight::hasEnergy)
                .sorted(Comparator
                        .comparingDouble(DirectEmissiveBlockLight::priority)
                        .reversed()
                        .thenComparing(DirectEmissiveBlockLight::stableKey))
                .toList();
    }

    private static boolean matchesCelestialDimension(
            DirectCelestialLightingPlan celestialLighting,
            VoxelSectionSnapshotReference section
    ) {
        return !celestialLighting.hasDimension()
                || celestialLighting.dimensionId().equals(section.origin().dimension());
    }

    private static boolean canSampleEmissiveLight(
            DirectEmissiveBlockLight light,
            VoxelSectionSnapshotReference section,
            float originX,
            float originY,
            float originZ
    ) {
        if (!light.dimension().equals(section.origin().dimension())) {
            return false;
        }

        float targetX = light.blockX() + 0.5F;
        float targetY = light.blockY() + 0.5F;
        float targetZ = light.blockZ() + 0.5F;
        float deltaX = targetX - originX;
        float deltaY = targetY - originY;
        float deltaZ = targetZ - originZ;
        float distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        if (distanceSquared <= MIN_EMISSIVE_SAMPLE_DISTANCE_SQUARED) {
            return false;
        }

        double influenceDistance = light.influenceRadiusBlocks() + SECTION_HALF_DIAGONAL_BLOCKS;
        return distanceSquared <= influenceDistance * influenceDistance;
    }

    private static SampleOrigin emissiveSurfaceSampleOrigin(
            DirectEmissiveBlockLight light,
            VoxelSectionSnapshotReference section
    ) {
        if (!light.dimension().equals(section.origin().dimension()) || !hasPotentialReceiverSurface(section)) {
            return null;
        }

        float targetX = light.blockX() + BLOCK_CENTER_OFFSET;
        float targetY = light.blockY() + BLOCK_CENTER_OFFSET;
        float targetZ = light.blockZ() + BLOCK_CENTER_OFFSET;
        SampleOrigin extractedSurfaceOrigin = nearestExtractedSurfaceSampleOrigin(light, section, targetX, targetY, targetZ);
        if (extractedSurfaceOrigin != null) {
            return extractedSurfaceOrigin;
        }

        float minX = section.origin().minBlockX() + BLOCK_CENTER_OFFSET;
        float minY = section.origin().minBlockY() + BLOCK_CENTER_OFFSET;
        float minZ = section.origin().minBlockZ() + BLOCK_CENTER_OFFSET;
        float maxX = section.origin().minBlockX() + ChunkSectionOrigin.SECTION_EDGE_LENGTH - BLOCK_CENTER_OFFSET;
        float maxY = section.origin().minBlockY() + ChunkSectionOrigin.SECTION_EDGE_LENGTH - BLOCK_CENTER_OFFSET;
        float maxZ = section.origin().minBlockZ() + ChunkSectionOrigin.SECTION_EDGE_LENGTH - BLOCK_CENTER_OFFSET;

        SampleOrigin adjacentOrigin = nearestAdjacentOriginInSection(
                targetX,
                targetY,
                targetZ,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
        if (adjacentOrigin != null) {
            return adjacentOrigin;
        }

        float clampedX = clamp(targetX, minX, maxX);
        float clampedY = clamp(targetY, minY, maxY);
        float clampedZ = clamp(targetZ, minZ, maxZ);
        if (squaredDistance(targetX, targetY, targetZ, clampedX, clampedY, clampedZ) > MIN_EMISSIVE_SAMPLE_DISTANCE_SQUARED) {
            return new SampleOrigin(clampedX, clampedY, clampedZ);
        }
        return null;
    }

    private static boolean hasPotentialReceiverSurface(VoxelSectionSnapshotReference section) {
        return section.hasSurfaceSamples() || section.opaqueVoxelCount() > 0 || section.occupiedVoxelCount() > 0;
    }

    private static SampleOrigin nearestExtractedSurfaceSampleOrigin(
            DirectEmissiveBlockLight light,
            VoxelSectionSnapshotReference section,
            float targetX,
            float targetY,
            float targetZ
    ) {
        SampleOrigin nearestOrigin = null;
        float nearestDistanceSquared = Float.MAX_VALUE;

        for (SectionSurfaceSampleMetadata sample : section.surfaceSamples()) {
            float sampleX = section.origin().minBlockX() + sample.localX() + BLOCK_CENTER_OFFSET;
            float sampleY = section.origin().minBlockY() + sample.localY() + BLOCK_CENTER_OFFSET;
            float sampleZ = section.origin().minBlockZ() + sample.localZ() + BLOCK_CENTER_OFFSET;
            float distanceSquared = squaredDistance(targetX, targetY, targetZ, sampleX, sampleY, sampleZ);
            if (distanceSquared <= MIN_EMISSIVE_SAMPLE_DISTANCE_SQUARED || distanceSquared >= nearestDistanceSquared) {
                continue;
            }
            if (!light.dimension().equals(section.origin().dimension())) {
                continue;
            }
            nearestOrigin = new SampleOrigin(sampleX, sampleY, sampleZ);
            nearestDistanceSquared = distanceSquared;
        }

        return nearestOrigin;
    }

    private static SampleOrigin nearestAdjacentOriginInSection(
            float targetX,
            float targetY,
            float targetZ,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        SampleOrigin nearestOrigin = null;
        float nearestDistanceSquared = Float.MAX_VALUE;

        for (AdjacentBlockOffset offset : AdjacentBlockOffset.VALUES) {
            float sampleX = targetX + offset.x();
            float sampleY = targetY + offset.y();
            float sampleZ = targetZ + offset.z();
            if (sampleX < minX || sampleX > maxX || sampleY < minY || sampleY > maxY || sampleZ < minZ || sampleZ > maxZ) {
                continue;
            }
            float distanceSquared = squaredDistance(targetX, targetY, targetZ, sampleX, sampleY, sampleZ);
            if (distanceSquared <= MIN_EMISSIVE_SAMPLE_DISTANCE_SQUARED || distanceSquared >= nearestDistanceSquared) {
                continue;
            }
            nearestDistanceSquared = distanceSquared;
            nearestOrigin = new SampleOrigin(sampleX, sampleY, sampleZ);
        }

        return nearestOrigin;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float squaredDistance(
            float firstX,
            float firstY,
            float firstZ,
            float secondX,
            float secondY,
            float secondZ
    ) {
        float deltaX = firstX - secondX;
        float deltaY = firstY - secondY;
        float deltaZ = firstZ - secondZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    private static float celestialMaxDistance(VoxelRayBudgetConfig rayBudget) {
        long sectionDistanceBlocks = (long) rayBudget.maxVisitedSectionsPerRay() * ChunkSectionOrigin.SECTION_EDGE_LENGTH;
        float cappedSectionDistance = sectionDistanceBlocks > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : sectionDistanceBlocks;
        return Math.max(1.0F, Math.min(rayBudget.maxVisitedVoxelsPerRay(), cappedSectionDistance));
    }

    private static float sectionCenterX(VoxelSectionSnapshotReference section) {
        return section.origin().minBlockX() + SECTION_CENTER_OFFSET_BLOCKS;
    }

    private static float sectionCenterY(VoxelSectionSnapshotReference section) {
        return section.origin().minBlockY() + SECTION_CENTER_OFFSET_BLOCKS;
    }

    private static float sectionCenterZ(VoxelSectionSnapshotReference section) {
        return section.origin().minBlockZ() + SECTION_CENTER_OFFSET_BLOCKS;
    }

    private record SampleOrigin(float x, float y, float z) {
    }

    private record AdjacentBlockOffset(float x, float y, float z) {
        private static final AdjacentBlockOffset[] VALUES = {
                new AdjacentBlockOffset(1.0F, 0.0F, 0.0F),
                new AdjacentBlockOffset(-1.0F, 0.0F, 0.0F),
                new AdjacentBlockOffset(0.0F, 1.0F, 0.0F),
                new AdjacentBlockOffset(0.0F, -1.0F, 0.0F),
                new AdjacentBlockOffset(0.0F, 0.0F, 1.0F),
                new AdjacentBlockOffset(0.0F, 0.0F, -1.0F)
        };
    }
}
