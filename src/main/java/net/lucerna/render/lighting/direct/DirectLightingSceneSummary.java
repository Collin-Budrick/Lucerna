package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelRay;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.world.section.ChunkSectionOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        float averageMatchedReceiverDistance,
        float sourcePositionMatchScore,
        float receiverDistanceEvidenceScore,
        float occluderShadowCandidateScore,
        float emissiveSpillPhysicalScore,
        float physicalEvidenceScore,
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
        averageMatchedReceiverDistance = finiteNonNegative(averageMatchedReceiverDistance);
        sourcePositionMatchScore = clampUnit(sourcePositionMatchScore);
        receiverDistanceEvidenceScore = clampUnit(receiverDistanceEvidenceScore);
        occluderShadowCandidateScore = clampUnit(occluderShadowCandidateScore);
        emissiveSpillPhysicalScore = clampUnit(emissiveSpillPhysicalScore);
        physicalEvidenceScore = clampUnit(physicalEvidenceScore);
        compactLabel = clean(compactLabel, defaultLabel(
                emissiveLightCount,
                shadowCandidateCount,
                surfaceProbeCount,
                emissiveProximityScore,
                surfaceOrientationConfidence,
                sectionOpacityHint,
                materialSourceCoupling,
                averageMatchedReceiverDistance,
                sourcePositionMatchScore,
                receiverDistanceEvidenceScore,
                occluderShadowCandidateScore,
                emissiveSpillPhysicalScore,
                physicalEvidenceScore
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
        float emissivePriorityEnergy = 0.0F;
        float maxEmissiveRadius = 0.0F;
        Map<String, DirectEmissiveBlockLight> emissiveLightByKey = new HashMap<>();
        for (DirectEmissiveBlockLight light : resolvedLights) {
            Objects.requireNonNull(light, "emissive lights must not contain null entries");
            float luma = light.color().luminance();
            emissiveIntensity += light.intensity();
            emissiveRadius += light.influenceRadiusBlocks();
            emissiveLuma += luma;
            emissiveEnergy += light.intensity() * Math.max(1.0F, light.influenceRadiusBlocks()) * luma;
            emissivePriorityEnergy += light.priority();
            maxEmissiveRadius = Math.max(maxEmissiveRadius, light.influenceRadiusBlocks());
            emissiveLightByKey.put(light.stableKey(), light);
        }

        int emissiveCandidateCount = 0;
        int matchedEmissiveCandidateCount = 0;
        int surfaceProbeCount = 0;
        float shadowDistance = 0.0F;
        float emissiveShadowDistance = 0.0F;
        float emissiveSurfaceContribution = 0.0F;
        float matchedReceiverDistance = 0.0F;
        float receiverDistanceEvidence = 0.0F;
        float sourceMaterialPositionEvidence = 0.0F;
        float emissiveSpillEvidence = 0.0F;
        float shadowCandidateEvidence = 0.0F;
        float normalX = 0.0F;
        float normalY = 0.0F;
        float normalZ = 0.0F;
        for (DirectShadowRayCandidate candidate : resolvedCandidates) {
            Objects.requireNonNull(candidate, "shadow candidates must not contain null entries");
            VoxelRay ray = candidate.ray();
            if (candidate.source() == DirectShadowRaySource.EMISSIVE_BLOCK) {
                emissiveCandidateCount++;
                emissiveShadowDistance += ray.maxDistance();
                emissiveSurfaceContribution += candidate.contributionWeight();
                DirectEmissiveBlockLight light = emissiveLightByKey.get(candidate.sourceKey());
                if (light != null) {
                    float receiverDistance = light.distanceToReceiver(ray.originX(), ray.originY(), ray.originZ());
                    if (Float.isFinite(receiverDistance)) {
                        matchedEmissiveCandidateCount++;
                        matchedReceiverDistance += receiverDistance;
                        receiverDistanceEvidence += light.receiverDistanceEvidenceScore(
                                ray.originX(),
                                ray.originY(),
                                ray.originZ()
                        );
                        sourceMaterialPositionEvidence += light.sourceMaterialEvidenceScore();
                        float contributionMatch = clampUnit(candidate.contributionWeight() / Math.max(1.0F, light.priority()));
                        emissiveSpillEvidence += light.physicalSpillEvidenceScore(
                                ray.originX(),
                                ray.originY(),
                                ray.originZ()
                        ) * (0.65F + contributionMatch * 0.35F);
                    }
                }
            }
            if (candidate.contributesLighting()) {
                surfaceProbeCount++;
            }
            shadowCandidateEvidence += clampUnit((candidate.contributesLighting() ? 0.32F : 0.0F)
                    + (ray.directionLooksNormalized() ? 0.22F : 0.0F)
                    + (clampUnit(ray.distanceRange() / 32.0F) * 0.20F)
                    + (clampUnit(candidate.contributionWeight() / 18.0F) * 0.16F)
                    + (candidate.source() == DirectShadowRaySource.EMISSIVE_BLOCK ? 0.10F : 0.0F));
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
        float averageEmissiveDistance = emissiveCandidateCount == 0
                ? averageDistance
                : emissiveShadowDistance / emissiveCandidateCount;
        float averageMatchedDistance = matchedEmissiveCandidateCount == 0
                ? averageEmissiveDistance
                : matchedReceiverDistance / matchedEmissiveCandidateCount;
        float averageRadius = emissiveRadius * inverseLightCount;
        float distanceCoverage = averageEmissiveDistance <= 0.0F
                ? 0.0F
                : clampUnit((averageRadius + maxEmissiveRadius * 0.25F) / Math.max(1.0F, averageEmissiveDistance));
        float matchedCandidateRatio = emissiveCandidateCount == 0
                ? 0.0F
                : clampUnit((float) matchedEmissiveCandidateCount / emissiveCandidateCount);
        float candidateCoverage = resolvedLights.isEmpty()
                ? 0.0F
                : clampUnit((float) emissiveCandidateCount / Math.max(1, resolvedLights.size()));
        float opacityHint = occupiedVoxels == 0 ? 0.0F : clampUnit((float) opaqueVoxels / occupiedVoxels);
        float emissiveDensity = occupiedVoxels == 0 ? 0.0F : clampUnit((float) emissiveVoxels / occupiedVoxels);
        float receiverContribution = resolvedLights.isEmpty()
                ? 0.0F
                : clampUnit(emissiveSurfaceContribution / Math.max(1.0F, resolvedLights.size() * 75.0F));
        float receiverDistanceScore = matchedEmissiveCandidateCount == 0
                ? distanceCoverage
                : clampUnit(receiverDistanceEvidence / matchedEmissiveCandidateCount);
        float sourcePositionScore = matchedEmissiveCandidateCount == 0
                ? 0.0F
                : clampUnit((matchedCandidateRatio * 0.60F)
                + ((sourceMaterialPositionEvidence / matchedEmissiveCandidateCount) * 0.40F));
        float spillEvidenceScore = matchedEmissiveCandidateCount == 0
                ? 0.0F
                : clampUnit(emissiveSpillEvidence / matchedEmissiveCandidateCount);
        float energyDensity = clampUnit(
                (emissiveEnergy + emissivePriorityEnergy * 0.35F) / Math.max(1.0F, resolvedLights.size() * 25.0F)
        );
        float proximity = clampUnit((distanceCoverage * 0.35F)
                + (candidateCoverage * 0.20F)
                + (energyDensity * 0.20F)
                + (receiverContribution * 0.25F));
        float orientationConfidence = clampUnit(normalLength);
        float shadowCandidateScore = resolvedCandidates.isEmpty()
                ? 0.0F
                : clampUnit(shadowCandidateEvidence / resolvedCandidates.size());
        float occluderShadowScore = clampUnit((shadowCandidateScore * 0.38F)
                + (opacityHint * 0.28F)
                + (candidateCoverage * 0.18F)
                + (surfaceProbeCount > 0 ? 0.10F : 0.0F)
                + (resolvedSections.isEmpty() ? 0.0F : 0.06F));
        float materialCoupling = clampUnit((proximity * 0.35F)
                + (orientationConfidence * 0.20F)
                + (opacityHint * 0.20F)
                + (emissiveDensity * 0.12F)
                + (sourcePositionScore * 0.07F)
                + (receiverDistanceScore * 0.04F)
                + (occluderShadowScore * 0.02F));
        float emissiveSpillPhysicalScore = clampUnit((spillEvidenceScore * 0.26F)
                + (receiverDistanceScore * 0.22F)
                + (sourcePositionScore * 0.20F)
                + (materialCoupling * 0.16F)
                + (occluderShadowScore * 0.16F));
        float physicalEvidenceScore = clampUnit((emissiveSpillPhysicalScore * 0.32F)
                + (sourcePositionScore * 0.20F)
                + (receiverDistanceScore * 0.18F)
                + (occluderShadowScore * 0.14F)
                + (materialCoupling * 0.10F)
                + (energyDensity * 0.06F));

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
                averageEmissiveDistance,
                averageNormalX,
                averageNormalY,
                averageNormalZ,
                orientationConfidence,
                opacityHint,
                emissiveDensity,
                materialCoupling,
                averageMatchedDistance,
                sourcePositionScore,
                receiverDistanceScore,
                occluderShadowScore,
                emissiveSpillPhysicalScore,
                physicalEvidenceScore,
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
            float materialSourceCoupling,
            float averageMatchedReceiverDistance,
            float sourcePositionMatchScore,
            float receiverDistanceEvidenceScore,
            float occluderShadowCandidateScore,
            float emissiveSpillPhysicalScore,
            float physicalEvidenceScore
    ) {
        return "emissive=" + emissiveLightCount
                + " shadows=" + shadowCandidateCount
                + " surfaceProbes=" + surfaceProbeCount
                + " proximity=" + emissiveProximityScore
                + " orientation=" + surfaceOrientationConfidence
                + " opacityHint=" + sectionOpacityHint
                + " materialSourceCoupling=" + materialSourceCoupling
                + " matchedReceiverDistance=" + averageMatchedReceiverDistance
                + " sourcePositionMatch=" + sourcePositionMatchScore
                + " receiverDistanceEvidence=" + receiverDistanceEvidenceScore
                + " occluderShadowEvidence=" + occluderShadowCandidateScore
                + " emissiveSpillPhysical=" + emissiveSpillPhysicalScore
                + " physicalEvidence=" + physicalEvidenceScore
                + " boundary=preview-scene-summary-not-physically-correct-gi";
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
