package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelRay;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.List;
import java.util.Objects;

public record DirectLightingSceneSummary(
        int emissiveLightCount,
        int shadowCandidateCount,
        int sectionSnapshotCount,
        int emissiveShadowCandidateCount,
        int surfaceProbeCount,
        int sectionOccupiedVoxelCount,
        int sectionOpaqueVoxelCount,
        int sectionEmissiveVoxelCount,
        float averageEmissiveIntensity,
        float averageEmissiveRadius,
        float averageEmissiveLuma,
        float emissiveEnergyDensity,
        float emissiveProximityScore,
        float averageShadowDistance,
        float averageSurfaceNormalX,
        float averageSurfaceNormalY,
        float averageSurfaceNormalZ,
        float surfaceOrientationConfidence,
        float sectionOpacityHint,
        float sectionEmissiveDensity,
        float materialSourceCoupling,
        String compactLabel
) {
    public DirectLightingSceneSummary {
        emissiveLightCount = Math.max(0, emissiveLightCount);
        shadowCandidateCount = Math.max(0, shadowCandidateCount);
        sectionSnapshotCount = Math.max(0, sectionSnapshotCount);
        emissiveShadowCandidateCount = Math.max(0, emissiveShadowCandidateCount);
        surfaceProbeCount = Math.max(0, surfaceProbeCount);
        sectionOccupiedVoxelCount = Math.max(0, sectionOccupiedVoxelCount);
        sectionOpaqueVoxelCount = Math.max(0, sectionOpaqueVoxelCount);
        sectionEmissiveVoxelCount = Math.max(0, sectionEmissiveVoxelCount);
        averageEmissiveIntensity = finiteNonNegative(averageEmissiveIntensity);
        averageEmissiveRadius = finiteNonNegative(averageEmissiveRadius);
        averageEmissiveLuma = finiteNonNegative(averageEmissiveLuma);
        emissiveEnergyDensity = clampUnit(emissiveEnergyDensity);
        emissiveProximityScore = clampUnit(emissiveProximityScore);
        averageShadowDistance = finiteNonNegative(averageShadowDistance);
        averageSurfaceNormalX = finiteOrZero(averageSurfaceNormalX);
        averageSurfaceNormalY = finiteOrZero(averageSurfaceNormalY);
        averageSurfaceNormalZ = finiteOrZero(averageSurfaceNormalZ);
        surfaceOrientationConfidence = clampUnit(surfaceOrientationConfidence);
        sectionOpacityHint = clampUnit(sectionOpacityHint);
        sectionEmissiveDensity = clampUnit(sectionEmissiveDensity);
        materialSourceCoupling = clampUnit(materialSourceCoupling);
        compactLabel = clean(compactLabel, defaultLabel(
                emissiveLightCount,
                shadowCandidateCount,
                surfaceProbeCount,
                emissiveProximityScore,
                surfaceOrientationConfidence,
                sectionOpacityHint,
                materialSourceCoupling
        ));
    }

    public static DirectLightingSceneSummary from(
            List<DirectEmissiveBlockLight> emissiveLights,
            List<DirectShadowRayCandidate> shadowCandidates,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        List<DirectEmissiveBlockLight> resolvedLights = emissiveLights == null ? List.of() : emissiveLights;
        List<DirectShadowRayCandidate> resolvedCandidates = shadowCandidates == null ? List.of() : shadowCandidates;
        List<VoxelSectionSnapshotReference> resolvedSections = sectionSnapshots == null ? List.of() : sectionSnapshots;

        float emissiveIntensity = 0.0F;
        float emissiveRadius = 0.0F;
        float emissiveLuma = 0.0F;
        float emissiveEnergy = 0.0F;
        for (DirectEmissiveBlockLight light : resolvedLights) {
            Objects.requireNonNull(light, "emissive lights must not contain null entries");
            float luma = light.color().luminance();
            emissiveIntensity += light.intensity();
            emissiveRadius += light.influenceRadiusBlocks();
            emissiveLuma += luma;
            emissiveEnergy += light.intensity() * Math.max(1.0F, light.influenceRadiusBlocks()) * luma;
        }

        int emissiveCandidateCount = 0;
        int surfaceProbeCount = 0;
        float shadowDistance = 0.0F;
        float normalX = 0.0F;
        float normalY = 0.0F;
        float normalZ = 0.0F;
        for (DirectShadowRayCandidate candidate : resolvedCandidates) {
            Objects.requireNonNull(candidate, "shadow candidates must not contain null entries");
            VoxelRay ray = candidate.ray();
            if (candidate.source() == DirectShadowRaySource.EMISSIVE_BLOCK) {
                emissiveCandidateCount++;
            }
            if (candidate.contributesLighting()) {
                surfaceProbeCount++;
            }
            shadowDistance += ray.maxDistance();
            normalX -= ray.directionX();
            normalY -= ray.directionY();
            normalZ -= ray.directionZ();
        }

        int occupiedVoxels = 0;
        int opaqueVoxels = 0;
        int emissiveVoxels = 0;
        for (VoxelSectionSnapshotReference section : resolvedSections) {
            Objects.requireNonNull(section, "section snapshots must not contain null entries");
            occupiedVoxels = saturatingAdd(occupiedVoxels, section.occupiedVoxelCount(), ChunkSectionOrigin.SECTION_VOLUME);
            opaqueVoxels = saturatingAdd(opaqueVoxels, section.opaqueVoxelCount(), ChunkSectionOrigin.SECTION_VOLUME);
            emissiveVoxels = saturatingAdd(emissiveVoxels, section.emissiveVoxelCount(), ChunkSectionOrigin.SECTION_VOLUME);
        }

        float inverseLightCount = resolvedLights.isEmpty() ? 0.0F : 1.0F / resolvedLights.size();
        float inverseCandidateCount = resolvedCandidates.isEmpty() ? 0.0F : 1.0F / resolvedCandidates.size();
        float averageNormalX = normalX * inverseCandidateCount;
        float averageNormalY = normalY * inverseCandidateCount;
        float averageNormalZ = normalZ * inverseCandidateCount;
        float normalLength = vectorLength(averageNormalX, averageNormalY, averageNormalZ);
        float averageDistance = shadowDistance * inverseCandidateCount;
        float averageRadius = emissiveRadius * inverseLightCount;
        float distanceCoverage = averageDistance <= 0.0F
                ? 0.0F
                : clampUnit(averageRadius / Math.max(1.0F, averageDistance));
        float candidateCoverage = resolvedCandidates.isEmpty()
                ? 0.0F
                : clampUnit((float) emissiveCandidateCount / resolvedCandidates.size());
        float opacityHint = occupiedVoxels == 0 ? 0.0F : clampUnit((float) opaqueVoxels / occupiedVoxels);
        float emissiveDensity = occupiedVoxels == 0 ? 0.0F : clampUnit((float) emissiveVoxels / occupiedVoxels);
        float energyDensity = clampUnit(emissiveEnergy / Math.max(1.0F, resolvedLights.size() * 15.0F));
        float proximity = clampUnit((distanceCoverage * 0.45F)
                + (candidateCoverage * 0.35F)
                + (energyDensity * 0.20F));
        float orientationConfidence = clampUnit(normalLength);
        float materialCoupling = clampUnit((proximity * 0.35F)
                + (orientationConfidence * 0.25F)
                + (opacityHint * 0.25F)
                + (emissiveDensity * 0.15F));

        return new DirectLightingSceneSummary(
                resolvedLights.size(),
                resolvedCandidates.size(),
                resolvedSections.size(),
                emissiveCandidateCount,
                surfaceProbeCount,
                occupiedVoxels,
                opaqueVoxels,
                emissiveVoxels,
                emissiveIntensity * inverseLightCount,
                averageRadius,
                emissiveLuma * inverseLightCount,
                energyDensity,
                proximity,
                averageDistance,
                averageNormalX,
                averageNormalY,
                averageNormalZ,
                orientationConfidence,
                opacityHint,
                emissiveDensity,
                materialCoupling,
                ""
        );
    }

    private static String defaultLabel(
            int emissiveLightCount,
            int shadowCandidateCount,
            int surfaceProbeCount,
            float emissiveProximityScore,
            float surfaceOrientationConfidence,
            float sectionOpacityHint,
            float materialSourceCoupling
    ) {
        return "emissive=" + emissiveLightCount
                + " shadows=" + shadowCandidateCount
                + " surfaceProbes=" + surfaceProbeCount
                + " proximity=" + emissiveProximityScore
                + " orientation=" + surfaceOrientationConfidence
                + " opacityHint=" + sectionOpacityHint
                + " materialSourceCoupling=" + materialSourceCoupling;
    }

    private static int saturatingAdd(int first, int second, int maxSingleValue) {
        long next = (long) first + Math.max(0, Math.min(second, maxSingleValue));
        return (int) Math.min(Integer.MAX_VALUE, next);
    }

    private static float vectorLength(float x, float y, float z) {
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float finiteNonNegative(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }

    private static float finiteOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return value;
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "direct scene summary");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
