package net.lucerna.render.lighting.gi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record DiffuseGiSceneInputSummary(
        int surfaceSampleCount,
        int coloredSurfaceSampleCount,
        int distinctMaterialCount,
        float materialDiversityRatio,
        float averageAlbedoR,
        float averageAlbedoG,
        float averageAlbedoB,
        float averageAlbedoSaturation,
        float coloredBounceInfluence,
        float materialColorInfluence,
        int skylitSurfaceCount,
        int sealedInteriorSurfaceCount,
        int downwardFacingSurfaceCount,
        int verticalSurfaceCount,
        float skylightExposureRatio,
        float sealedInteriorRatio,
        float skylightInteriorContrast,
        float downwardFacingRatio,
        float verticalSurfaceRatio,
        float orientationBalance,
        float averageSurfaceNormalX,
        float averageSurfaceNormalY,
        float averageSurfaceNormalZ,
        float averageNormalLength,
        float surfaceOrientationConfidence,
        float averageSurfaceRoughness,
        float averageSurfaceConfidence,
        float usableSurfaceConfidenceRatio,
        int dirtySurfaceSampleCount,
        float dirtySurfaceSampleRatio,
        float materialOpacityHint,
        float emissiveProximityScore,
        float emissiveSourceCoupling,
        float celestialSourceCoupling,
        float lightSourceSceneCoupling,
        int emissiveProximitySignals,
        float dirtyRegionInfluence,
        float occlusionDirtyRegionInfluence,
        float cacheConfidenceInput,
        float cacheVarianceInput,
        int cacheSampleCountInput,
        boolean cacheDirtyInput,
        float cachePhysicalConfidence,
        int radianceSampleCount,
        float averageRadianceR,
        float averageRadianceG,
        float averageRadianceB,
        float radianceEnergy,
        float radianceDirectionConfidence,
        float materialGeometryCoupling,
        float physicalGiInputScore,
        float receiverSurfacePositionScore,
        float sourceReceiverEvidenceScore,
        float materialPositionEvidenceScore,
        float occluderShadowCandidateScore,
        float coloredBouncePhysicalScore,
        float emissiveSpillPhysicalScore,
        float previewPhysicalEvidenceScore,
        boolean emissiveProximityAvailable,
        boolean affectedSurfaceRegionAvailable,
        int affectedSurfaceMinBlockX,
        int affectedSurfaceMinBlockY,
        int affectedSurfaceMinBlockZ,
        int affectedSurfaceMaxBlockX,
        int affectedSurfaceMaxBlockY,
        int affectedSurfaceMaxBlockZ,
        String affectedSurfaceRegionLabel,
        String debugLabel
) {
    private static final float SKYWARD_NORMAL_Y = 0.55F;
    private static final float SEALED_NORMAL_Y = 0.25F;
    private static final float COLORED_SURFACE_SATURATION = 0.12F;
    private static final float BRIGHT_SURFACE_LUMA = 0.45F;
    private static final float DARK_SURFACE_LUMA = 0.24F;
    private static final float DOWNWARD_NORMAL_Y = -0.55F;

    public DiffuseGiSceneInputSummary {
        surfaceSampleCount = Math.max(0, surfaceSampleCount);
        coloredSurfaceSampleCount = Math.max(0, coloredSurfaceSampleCount);
        distinctMaterialCount = Math.max(0, distinctMaterialCount);
        materialDiversityRatio = clampUnit(materialDiversityRatio);
        averageAlbedoR = clampUnit(averageAlbedoR);
        averageAlbedoG = clampUnit(averageAlbedoG);
        averageAlbedoB = clampUnit(averageAlbedoB);
        averageAlbedoSaturation = clampUnit(averageAlbedoSaturation);
        coloredBounceInfluence = clampUnit(coloredBounceInfluence);
        materialColorInfluence = clampUnit(materialColorInfluence);
        skylitSurfaceCount = Math.max(0, skylitSurfaceCount);
        sealedInteriorSurfaceCount = Math.max(0, sealedInteriorSurfaceCount);
        downwardFacingSurfaceCount = Math.max(0, downwardFacingSurfaceCount);
        verticalSurfaceCount = Math.max(0, verticalSurfaceCount);
        skylightExposureRatio = clampUnit(skylightExposureRatio);
        sealedInteriorRatio = clampUnit(sealedInteriorRatio);
        skylightInteriorContrast = clampUnit(skylightInteriorContrast);
        downwardFacingRatio = clampUnit(downwardFacingRatio);
        verticalSurfaceRatio = clampUnit(verticalSurfaceRatio);
        orientationBalance = clampUnit(orientationBalance);
        averageSurfaceNormalX = finiteOrZero(averageSurfaceNormalX);
        averageSurfaceNormalY = finiteOrZero(averageSurfaceNormalY);
        averageSurfaceNormalZ = finiteOrZero(averageSurfaceNormalZ);
        averageNormalLength = clampUnit(averageNormalLength);
        surfaceOrientationConfidence = clampUnit(surfaceOrientationConfidence);
        averageSurfaceRoughness = clampUnit(averageSurfaceRoughness);
        averageSurfaceConfidence = clampUnit(averageSurfaceConfidence);
        usableSurfaceConfidenceRatio = clampUnit(usableSurfaceConfidenceRatio);
        dirtySurfaceSampleCount = Math.max(0, dirtySurfaceSampleCount);
        dirtySurfaceSampleRatio = clampUnit(dirtySurfaceSampleRatio);
        materialOpacityHint = clampUnit(materialOpacityHint);
        emissiveProximityScore = clampUnit(emissiveProximityScore);
        emissiveSourceCoupling = clampUnit(emissiveSourceCoupling);
        celestialSourceCoupling = clampUnit(celestialSourceCoupling);
        lightSourceSceneCoupling = clampUnit(lightSourceSceneCoupling);
        emissiveProximitySignals = Math.max(0, emissiveProximitySignals);
        dirtyRegionInfluence = clampUnit(dirtyRegionInfluence);
        occlusionDirtyRegionInfluence = clampUnit(occlusionDirtyRegionInfluence);
        cacheConfidenceInput = clampUnit(cacheConfidenceInput);
        cacheVarianceInput = finiteNonNegative(cacheVarianceInput);
        cacheSampleCountInput = Math.max(0, cacheSampleCountInput);
        cachePhysicalConfidence = clampUnit(cachePhysicalConfidence);
        radianceSampleCount = Math.max(0, radianceSampleCount);
        averageRadianceR = finiteNonNegative(averageRadianceR);
        averageRadianceG = finiteNonNegative(averageRadianceG);
        averageRadianceB = finiteNonNegative(averageRadianceB);
        radianceEnergy = finiteNonNegative(radianceEnergy);
        radianceDirectionConfidence = clampUnit(radianceDirectionConfidence);
        materialGeometryCoupling = clampUnit(materialGeometryCoupling);
        physicalGiInputScore = clampUnit(physicalGiInputScore);
        receiverSurfacePositionScore = clampUnit(receiverSurfacePositionScore);
        sourceReceiverEvidenceScore = clampUnit(sourceReceiverEvidenceScore);
        materialPositionEvidenceScore = clampUnit(materialPositionEvidenceScore);
        occluderShadowCandidateScore = clampUnit(occluderShadowCandidateScore);
        coloredBouncePhysicalScore = clampUnit(coloredBouncePhysicalScore);
        emissiveSpillPhysicalScore = clampUnit(emissiveSpillPhysicalScore);
        previewPhysicalEvidenceScore = clampUnit(previewPhysicalEvidenceScore);
        if (!affectedSurfaceRegionAvailable) {
            affectedSurfaceMinBlockX = 0;
            affectedSurfaceMinBlockY = 0;
            affectedSurfaceMinBlockZ = 0;
            affectedSurfaceMaxBlockX = 0;
            affectedSurfaceMaxBlockY = 0;
            affectedSurfaceMaxBlockZ = 0;
        }
        affectedSurfaceRegionLabel = clean(affectedSurfaceRegionLabel, defaultSurfaceRegionLabel(
                affectedSurfaceRegionAvailable,
                affectedSurfaceMinBlockX,
                affectedSurfaceMinBlockY,
                affectedSurfaceMinBlockZ,
                affectedSurfaceMaxBlockX,
                affectedSurfaceMaxBlockY,
                affectedSurfaceMaxBlockZ
        ));
        debugLabel = clean(debugLabel, defaultLabel(
                surfaceSampleCount,
                coloredSurfaceSampleCount,
                distinctMaterialCount,
                materialDiversityRatio,
                coloredBounceInfluence,
                materialColorInfluence,
                skylightExposureRatio,
                sealedInteriorRatio,
                skylightInteriorContrast,
                emissiveProximityScore,
                emissiveSourceCoupling,
                celestialSourceCoupling,
                lightSourceSceneCoupling,
                dirtyRegionInfluence,
                occlusionDirtyRegionInfluence,
                orientationBalance,
                averageSurfaceNormalX,
                averageSurfaceNormalY,
                averageSurfaceNormalZ,
                surfaceOrientationConfidence,
                averageSurfaceRoughness,
                averageSurfaceConfidence,
                usableSurfaceConfidenceRatio,
                dirtySurfaceSampleRatio,
                materialOpacityHint,
                radianceDirectionConfidence,
                cacheConfidenceInput,
                cacheVarianceInput,
                cachePhysicalConfidence,
                materialGeometryCoupling,
                physicalGiInputScore,
                receiverSurfacePositionScore,
                sourceReceiverEvidenceScore,
                materialPositionEvidenceScore,
                occluderShadowCandidateScore,
                coloredBouncePhysicalScore,
                emissiveSpillPhysicalScore,
                previewPhysicalEvidenceScore
        ));
    }

    public static DiffuseGiSceneInputSummary unavailable() {
        return new DiffuseGiSceneInputSummary(
                0,
                0,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0,
                0,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0.0F,
                0.0F,
                0.0F,
                1.0F,
                0,
                false,
                0.0F,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                false,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                "affected surface region unavailable",
                "GI scene inputs unavailable"
        );
    }

    public static DiffuseGiSceneInputSummary from(
            DiffuseGiSourceSummary sourceSummary,
            GiCacheSnapshot cacheSnapshot,
            CacheConfidence cacheConfidence
    ) {
        DiffuseGiSourceSummary resolvedSource = sourceSummary == null
                ? DiffuseGiSourceSummary.unavailable()
                : sourceSummary;
        GiCacheSnapshot resolvedCache = cacheSnapshot == null ? GiCacheSnapshot.empty() : cacheSnapshot;
        CacheConfidence resolvedConfidence = cacheConfidence == null
                ? CacheConfidence.empty("GI cache confidence unavailable")
                : cacheConfidence;

        int surfaceCount = resolvedCache.surfaceRecordCount();
        float albedoR = 0.0F;
        float albedoG = 0.0F;
        float albedoB = 0.0F;
        float saturationTotal = 0.0F;
        int coloredCount = 0;
        int skylitCount = 0;
        int sealedCount = 0;
        int downwardCount = 0;
        int verticalCount = 0;
        float normalLengthTotal = 0.0F;
        float normalXTotal = 0.0F;
        float normalYTotal = 0.0F;
        float normalZTotal = 0.0F;
        float roughnessTotal = 0.0F;
        float surfaceConfidenceTotal = 0.0F;
        int usableConfidenceCount = 0;
        int dirtySurfaceCount = 0;
        Set<Integer> materialIds = new HashSet<>();
        int minBlockX = Integer.MAX_VALUE;
        int minBlockY = Integer.MAX_VALUE;
        int minBlockZ = Integer.MAX_VALUE;
        int maxBlockX = Integer.MIN_VALUE;
        int maxBlockY = Integer.MIN_VALUE;
        int maxBlockZ = Integer.MIN_VALUE;

        for (SurfaceCacheRecord surface : resolvedCache.surfaceRecords()) {
            SurfaceCacheKey key = surface.key();
            int blockX = blockCoordinate(key.sectionX(), key.localX());
            int blockY = blockCoordinate(key.sectionY(), key.localY());
            int blockZ = blockCoordinate(key.sectionZ(), key.localZ());
            minBlockX = Math.min(minBlockX, blockX);
            minBlockY = Math.min(minBlockY, blockY);
            minBlockZ = Math.min(minBlockZ, blockZ);
            maxBlockX = Math.max(maxBlockX, blockX);
            maxBlockY = Math.max(maxBlockY, blockY);
            maxBlockZ = Math.max(maxBlockZ, blockZ);
            albedoR += surface.albedoR();
            albedoG += surface.albedoG();
            albedoB += surface.albedoB();
            materialIds.add(surface.materialId());
            float normalLength = clampUnit(vectorLength(surface.normalX(), surface.normalY(), surface.normalZ()));
            normalLengthTotal += normalLength;
            normalXTotal += surface.normalX();
            normalYTotal += surface.normalY();
            normalZTotal += surface.normalZ();
            roughnessTotal += surface.roughness();
            surfaceConfidenceTotal += surface.confidence().confidence();
            if (surface.usable(resolvedConfidence.confidence())) {
                usableConfidenceCount++;
            }
            if (surface.dirty()) {
                dirtySurfaceCount++;
            }
            float saturation = saturation(surface.albedoR(), surface.albedoG(), surface.albedoB());
            saturationTotal += saturation;
            float luma = luma(surface.albedoR(), surface.albedoG(), surface.albedoB());
            if (saturation >= COLORED_SURFACE_SATURATION) {
                coloredCount++;
            }
            if (surface.normalY() >= SKYWARD_NORMAL_Y && (luma >= BRIGHT_SURFACE_LUMA || resolvedSource.celestialLightCount() > 0)) {
                skylitCount++;
            }
            if (Math.abs(surface.normalY()) <= SEALED_NORMAL_Y && luma <= DARK_SURFACE_LUMA) {
                sealedCount++;
            }
            if (surface.normalY() <= DOWNWARD_NORMAL_Y) {
                downwardCount++;
            }
            if (Math.abs(surface.normalY()) <= SEALED_NORMAL_Y) {
                verticalCount++;
            }
        }

        float inverseSurfaceCount = surfaceCount == 0 ? 0.0F : 1.0F / surfaceCount;
        float rawAverageAlbedoR = albedoR * inverseSurfaceCount;
        float rawAverageAlbedoG = albedoG * inverseSurfaceCount;
        float rawAverageAlbedoB = albedoB * inverseSurfaceCount;
        float sourcePresence = clampUnit((resolvedSource.emissiveWorkScore() * 0.42F)
                + (resolvedSource.emissiveSourceScore() * 0.18F)
                + (resolvedSource.celestialSourceScore() * 0.12F)
                + (resolvedSource.sourceCouplingScore() * 0.18F)
                + (resolvedSource.sceneMutationScore() * 0.10F));
        float weightedAlbedoR = 0.0F;
        float weightedAlbedoG = 0.0F;
        float weightedAlbedoB = 0.0F;
        float weightedAlbedoTotal = 0.0F;
        float coloredSurfaceFalloffTotal = 0.0F;
        float sourceLinkedColorTotal = 0.0F;
        if (surfaceCount > 0) {
            float centerBlockX = (minBlockX + maxBlockX) * 0.5F;
            float centerBlockY = (minBlockY + maxBlockY) * 0.5F;
            float centerBlockZ = (minBlockZ + maxBlockZ) * 0.5F;
            float radius = Math.max(1.0F, vectorLength(
                    maxBlockX - minBlockX + 1.0F,
                    maxBlockY - minBlockY + 1.0F,
                    maxBlockZ - minBlockZ + 1.0F
            ) * 0.5F);
            for (SurfaceCacheRecord surface : resolvedCache.surfaceRecords()) {
                SurfaceCacheKey key = surface.key();
                float blockX = blockCoordinate(key.sectionX(), key.localX());
                float blockY = blockCoordinate(key.sectionY(), key.localY());
                float blockZ = blockCoordinate(key.sectionZ(), key.localZ());
                float normalizedDistance = vectorLength(
                        blockX - centerBlockX,
                        blockY - centerBlockY,
                        blockZ - centerBlockZ
                ) / radius;
                float distanceFalloff = 1.0F / (1.0F + (normalizedDistance * normalizedDistance * 1.35F));
                float saturation = saturation(surface.albedoR(), surface.albedoG(), surface.albedoB());
                float luma = luma(surface.albedoR(), surface.albedoG(), surface.albedoB());
                float confidence = surface.confidence().confidence();
                float normalLength = clampUnit(vectorLength(surface.normalX(), surface.normalY(), surface.normalZ()));
                float orientationWeight = clampUnit(0.42F
                        + (Math.abs(surface.normalY()) <= SEALED_NORMAL_Y ? 0.20F : 0.0F)
                        + (surface.normalY() <= DOWNWARD_NORMAL_Y ? 0.16F : 0.0F)
                        + (surface.normalY() >= SKYWARD_NORMAL_Y ? 0.08F : 0.0F)
                        + (normalLength * 0.14F));
                float chromaWeight = clampUnit((saturation * 0.55F)
                        + (Math.max(0.0F, luma - 0.10F) * 0.25F)
                        + (saturation >= COLORED_SURFACE_SATURATION ? 0.20F : 0.0F));
                float usableBoost = surface.usable(resolvedConfidence.confidence()) ? 0.18F : 0.0F;
                float surfaceWeight = distanceFalloff
                        * orientationWeight
                        * (0.35F + (confidence * 0.35F) + usableBoost)
                        * (0.35F + (chromaWeight * 0.65F))
                        * (0.72F + (sourcePresence * 0.28F));
                weightedAlbedoR += surface.albedoR() * surfaceWeight;
                weightedAlbedoG += surface.albedoG() * surfaceWeight;
                weightedAlbedoB += surface.albedoB() * surfaceWeight;
                weightedAlbedoTotal += surfaceWeight;
                coloredSurfaceFalloffTotal += distanceFalloff * chromaWeight;
                sourceLinkedColorTotal += surfaceWeight * sourcePresence;
            }
        }
        if (weightedAlbedoTotal > 0.0F) {
            weightedAlbedoR /= weightedAlbedoTotal;
            weightedAlbedoG /= weightedAlbedoTotal;
            weightedAlbedoB /= weightedAlbedoTotal;
        } else {
            weightedAlbedoR = rawAverageAlbedoR;
            weightedAlbedoG = rawAverageAlbedoG;
            weightedAlbedoB = rawAverageAlbedoB;
        }
        float weightedAlbedoSaturation = saturation(weightedAlbedoR, weightedAlbedoG, weightedAlbedoB);
        float coloredSurfaceFalloff = surfaceCount == 0 ? 0.0F : clampUnit(coloredSurfaceFalloffTotal * inverseSurfaceCount);
        float sourceLinkedColor = surfaceCount == 0 ? 0.0F : clampUnit(sourceLinkedColorTotal * inverseSurfaceCount);
        float bounceTintBlend = clampUnit((weightedAlbedoSaturation * 0.28F)
                + (coloredSurfaceFalloff * 0.24F)
                + (sourcePresence * 0.18F)
                + (sourceLinkedColor * 0.12F)
                + (resolvedConfidence.confidence() * 0.10F));
        float averageAlbedoR = mix(rawAverageAlbedoR, weightedAlbedoR, bounceTintBlend);
        float averageAlbedoG = mix(rawAverageAlbedoG, weightedAlbedoG, bounceTintBlend);
        float averageAlbedoB = mix(rawAverageAlbedoB, weightedAlbedoB, bounceTintBlend);
        float averageSaturation = clampUnit((saturationTotal * inverseSurfaceCount * 0.58F)
                + (weightedAlbedoSaturation * 0.26F)
                + (coloredSurfaceFalloff * 0.16F));
        float coloredRatio = surfaceCount == 0 ? 0.0F : (float) coloredCount / surfaceCount;
        float coloredInfluence = clampUnit((averageSaturation * 0.45F)
                + (coloredRatio * 0.23F)
                + (weightedAlbedoSaturation * 0.18F)
                + (coloredSurfaceFalloff * 0.14F));
        float averageAlbedoLuma = luma(averageAlbedoR, averageAlbedoG, averageAlbedoB);
        float skylightRatio = surfaceCount == 0 ? 0.0F : (float) skylitCount / surfaceCount;
        float sealedRatio = surfaceCount == 0 ? 0.0F : (float) sealedCount / surfaceCount;
        float skylightInteriorContrast = clampUnit(Math.min(skylightRatio, sealedRatio) * 2.0F);
        int distinctMaterials = materialIds.size();
        float materialDiversity = surfaceCount == 0 ? 0.0F : (float) distinctMaterials / surfaceCount;
        float materialColorInfluence = clampUnit((materialDiversity * 0.35F)
                + (coloredInfluence * 0.35F)
                + (averageSaturation * 0.20F)
                + (averageAlbedoLuma * 0.10F));
        float downwardRatio = surfaceCount == 0 ? 0.0F : (float) downwardCount / surfaceCount;
        float verticalRatio = surfaceCount == 0 ? 0.0F : (float) verticalCount / surfaceCount;
        float dominantOrientationRatio = Math.max(skylightRatio, Math.max(verticalRatio, downwardRatio));
        float orientationBalance = surfaceCount == 0 ? 0.0F : clampUnit(1.0F - dominantOrientationRatio);
        float averageNormalLength = normalLengthTotal * inverseSurfaceCount;
        float averageSurfaceNormalX = normalXTotal * inverseSurfaceCount;
        float averageSurfaceNormalY = normalYTotal * inverseSurfaceCount;
        float averageSurfaceNormalZ = normalZTotal * inverseSurfaceCount;
        float averageSurfaceRoughness = roughnessTotal * inverseSurfaceCount;
        float averageSurfaceConfidence = surfaceConfidenceTotal * inverseSurfaceCount;
        float usableSurfaceRatio = surfaceCount == 0 ? 0.0F : (float) usableConfidenceCount / surfaceCount;
        float dirtySurfaceRatio = surfaceCount == 0 ? 0.0F : (float) dirtySurfaceCount / surfaceCount;
        float materialOpacityHint = surfaceCount == 0 ? 0.0F : clampUnit((verticalRatio * 0.40F)
                + (sealedRatio * 0.30F)
                + ((1.0F - downwardRatio) * 0.15F)
                + (averageNormalLength * 0.15F));
        float surfaceOrientationConfidence = clampUnit(averageNormalLength
                * (0.50F + (orientationBalance * 0.25F) + (usableSurfaceRatio * 0.25F)));
        float materialGeometryCoupling = clampUnit((materialColorInfluence * 0.45F)
                + (surfaceOrientationConfidence * 0.22F)
                + (skylightInteriorContrast * 0.15F)
                + (materialDiversity * 0.08F)
                + (materialOpacityHint * 0.10F)
                + (coloredSurfaceFalloff * 0.05F)
                + (sourceLinkedColor * 0.05F));

        int radianceSamples = 0;
        float radianceR = 0.0F;
        float radianceG = 0.0F;
        float radianceB = 0.0F;
        float radianceDirectionConfidenceTotal = 0.0F;
        int radianceDirectionWeight = 0;
        for (RadianceCacheRecord radiance : resolvedCache.radianceRecords()) {
            int weight = Math.max(1, radiance.sampleCount());
            radianceSamples += radiance.sampleCount();
            radianceR += radiance.radianceR() * weight;
            radianceG += radiance.radianceG() * weight;
            radianceB += radiance.radianceB() * weight;
            float directionLength = clampUnit(vectorLength(
                    radiance.directionX(),
                    radiance.directionY(),
                    radiance.directionZ()
            ));
            radianceDirectionConfidenceTotal += clampUnit(directionLength * (1.0F - radiance.directionalVariance())) * weight;
            radianceDirectionWeight += weight;
        }
        int radianceWeight = Math.max(1, resolvedCache.radianceRecords().stream()
                .mapToInt(record -> Math.max(1, record.sampleCount()))
                .sum());
        float averageRadianceR = resolvedCache.hasRadianceRecords() ? radianceR / radianceWeight : 0.0F;
        float averageRadianceG = resolvedCache.hasRadianceRecords() ? radianceG / radianceWeight : 0.0F;
        float averageRadianceB = resolvedCache.hasRadianceRecords() ? radianceB / radianceWeight : 0.0F;
        float radianceEnergy = averageRadianceR + averageRadianceG + averageRadianceB;
        float radianceDirectionConfidence = radianceDirectionWeight == 0
                ? 0.0F
                : radianceDirectionConfidenceTotal / radianceDirectionWeight;

        int proximitySignals = resolvedSource.emissiveLightCount()
                + resolvedSource.shadowCandidateCount()
                + resolvedSource.budgetedShadowCandidateCount()
                + resolvedSource.dirtyRegionCount();
        float emissiveProximity = proximitySignals == 0
                ? 0.0F
                : clampUnit((resolvedSource.emissiveLightCount() * 0.35F
                + resolvedSource.budgetedShadowCandidateCount() * 0.35F
                + resolvedSource.shadowCandidateCount() * 0.20F
                + resolvedSource.dirtyRegionCount() * 0.10F) / Math.max(1.0F, surfaceCount + proximitySignals));
        float emissiveSourceCoupling = clampUnit(resolvedSource.emissiveWorkScore()
                * emissiveProximity
                * (0.50F + (surfaceOrientationConfidence * 0.30F) + (materialColorInfluence * 0.20F)));
        float celestialSourceCoupling = clampUnit(resolvedSource.celestialSourceScore()
                * (0.45F + (skylightRatio * 0.25F) + (skylightInteriorContrast * 0.20F) + (surfaceOrientationConfidence * 0.10F)));
        float lightSourceSceneCoupling = clampUnit((emissiveSourceCoupling * 0.55F)
                + (celestialSourceCoupling * 0.25F)
                + (resolvedSource.sourceCouplingScore() * 0.20F));
        float dirtyInfluence = clampUnit((resolvedSource.dirtyRegionCount() + (resolvedSource.materialUpdateCount() * 0.5F))
                / Math.max(1.0F, surfaceCount + resolvedSource.dirtyRegionCount() + resolvedSource.materialUpdateCount()));
        float occlusionDirtyInfluence = clampUnit((sealedRatio * 0.35F)
                + (downwardRatio * 0.20F)
                + (verticalRatio * 0.15F)
                + (dirtyInfluence * 0.12F)
                + (dirtySurfaceRatio * 0.08F)
                + (resolvedConfidence.dirty() ? 0.10F : 0.0F));
        float cacheSampleWeight = clampUnit(resolvedConfidence.sampleCount() / 16.0F);
        float cachePhysicalConfidence = clampUnit(resolvedConfidence.confidence()
                * (1.0F - clampUnit(resolvedConfidence.variance()))
                * (0.35F + (cacheSampleWeight * 0.65F))
                * (0.55F + (averageSurfaceConfidence * 0.30F) + (usableSurfaceRatio * 0.15F))
                * (resolvedConfidence.dirty() ? 0.50F : 1.0F));
        boolean hasSurfaceRegion = surfaceCount > 0;
        float surfaceSpanScore = hasSurfaceRegion
                ? clampUnit(vectorLength(
                maxBlockX - minBlockX + 1.0F,
                maxBlockY - minBlockY + 1.0F,
                maxBlockZ - minBlockZ + 1.0F
        ) / 32.0F)
                : 0.0F;
        float receiverSurfacePositionScore = clampUnit((clampUnit(surfaceCount / 32.0F) * 0.24F)
                + (surfaceSpanScore * 0.22F)
                + (usableSurfaceRatio * 0.18F)
                + (surfaceOrientationConfidence * 0.16F)
                + (materialDiversity * 0.12F)
                + (hasSurfaceRegion ? 0.08F : 0.0F));
        float materialPositionEvidenceScore = clampUnit((materialColorInfluence * 0.25F)
                + (materialDiversity * 0.22F)
                + (coloredRatio * 0.18F)
                + (receiverSurfacePositionScore * 0.18F)
                + (averageSurfaceConfidence * 0.10F)
                + (dirtySurfaceRatio * 0.07F));
        int shadowSourceSignals = resolvedSource.shadowCandidateCount()
                + resolvedSource.budgetedShadowCandidateCount()
                + resolvedSource.emissiveLightCount();
        float shadowSourceCandidateScore = shadowSourceSignals == 0
                ? 0.0F
                : clampUnit((resolvedSource.shadowCandidateCount() * 0.38F
                + resolvedSource.budgetedShadowCandidateCount() * 0.36F
                + resolvedSource.emissiveLightCount() * 0.26F) / Math.max(1.0F, shadowSourceSignals));
        float occluderShadowCandidateScore = clampUnit((shadowSourceCandidateScore * 0.30F)
                + (materialOpacityHint * 0.24F)
                + (occlusionDirtyInfluence * 0.18F)
                + (receiverSurfacePositionScore * 0.12F)
                + (surfaceOrientationConfidence * 0.10F)
                + (resolvedSource.sectionSnapshotCount() > 0 ? 0.06F : 0.0F));
        float sourceReceiverEvidenceScore = clampUnit((emissiveProximity * 0.26F)
                + (lightSourceSceneCoupling * 0.22F)
                + (receiverSurfacePositionScore * 0.20F)
                + (occluderShadowCandidateScore * 0.14F)
                + (cachePhysicalConfidence * 0.10F)
                + (radianceDirectionConfidence * 0.08F));
        float coloredBouncePhysicalScore = clampUnit((coloredInfluence * 0.22F)
                + (materialPositionEvidenceScore * 0.22F)
                + (materialGeometryCoupling * 0.18F)
                + (sourceReceiverEvidenceScore * 0.16F)
                + (cachePhysicalConfidence * 0.12F)
                + (radianceDirectionConfidence * 0.10F));
        float emissiveSpillPhysicalScore = clampUnit((emissiveSourceCoupling * 0.28F)
                + (emissiveProximity * 0.20F)
                + (sourceReceiverEvidenceScore * 0.18F)
                + (occluderShadowCandidateScore * 0.16F)
                + (materialPositionEvidenceScore * 0.10F)
                + (cachePhysicalConfidence * 0.08F));
        float physicalGiInputScore = clampUnit((lightSourceSceneCoupling * 0.20F)
                + (materialGeometryCoupling * 0.18F)
                + (emissiveProximity * 0.14F)
                + (skylightInteriorContrast * 0.12F)
                + (surfaceOrientationConfidence * 0.12F)
                + (occlusionDirtyInfluence * 0.10F)
                + (cachePhysicalConfidence * 0.08F)
                + (radianceDirectionConfidence * 0.06F));
        float previewPhysicalEvidenceScore = clampUnit((physicalGiInputScore * 0.30F)
                + (coloredBouncePhysicalScore * 0.18F)
                + (emissiveSpillPhysicalScore * 0.18F)
                + (sourceReceiverEvidenceScore * 0.15F)
                + (occluderShadowCandidateScore * 0.09F)
                + (materialPositionEvidenceScore * 0.06F)
                + (receiverSurfacePositionScore * 0.04F));
        boolean hasEmissiveProximity = resolvedSource.emissiveLightCount() > 0
                || resolvedSource.budgetedShadowCandidateCount() > 0
                || emissiveProximity > 0.0F;

        return new DiffuseGiSceneInputSummary(
                surfaceCount,
                coloredCount,
                distinctMaterials,
                materialDiversity,
                averageAlbedoR,
                averageAlbedoG,
                averageAlbedoB,
                averageSaturation,
                coloredInfluence,
                materialColorInfluence,
                skylitCount,
                sealedCount,
                downwardCount,
                verticalCount,
                skylightRatio,
                sealedRatio,
                skylightInteriorContrast,
                downwardRatio,
                verticalRatio,
                orientationBalance,
                averageSurfaceNormalX,
                averageSurfaceNormalY,
                averageSurfaceNormalZ,
                averageNormalLength,
                surfaceOrientationConfidence,
                averageSurfaceRoughness,
                averageSurfaceConfidence,
                usableSurfaceRatio,
                dirtySurfaceCount,
                dirtySurfaceRatio,
                materialOpacityHint,
                emissiveProximity,
                emissiveSourceCoupling,
                celestialSourceCoupling,
                lightSourceSceneCoupling,
                proximitySignals,
                dirtyInfluence,
                occlusionDirtyInfluence,
                resolvedConfidence.confidence(),
                resolvedConfidence.variance(),
                resolvedConfidence.sampleCount(),
                resolvedConfidence.dirty(),
                cachePhysicalConfidence,
                radianceSamples,
                averageRadianceR,
                averageRadianceG,
                averageRadianceB,
                radianceEnergy,
                radianceDirectionConfidence,
                materialGeometryCoupling,
                physicalGiInputScore,
                receiverSurfacePositionScore,
                sourceReceiverEvidenceScore,
                materialPositionEvidenceScore,
                occluderShadowCandidateScore,
                coloredBouncePhysicalScore,
                emissiveSpillPhysicalScore,
                previewPhysicalEvidenceScore,
                hasEmissiveProximity,
                hasSurfaceRegion,
                hasSurfaceRegion ? minBlockX : 0,
                hasSurfaceRegion ? minBlockY : 0,
                hasSurfaceRegion ? minBlockZ : 0,
                hasSurfaceRegion ? maxBlockX : 0,
                hasSurfaceRegion ? maxBlockY : 0,
                hasSurfaceRegion ? maxBlockZ : 0,
                defaultSurfaceRegionLabel(
                        hasSurfaceRegion,
                        hasSurfaceRegion ? minBlockX : 0,
                        hasSurfaceRegion ? minBlockY : 0,
                        hasSurfaceRegion ? minBlockZ : 0,
                        hasSurfaceRegion ? maxBlockX : 0,
                        hasSurfaceRegion ? maxBlockY : 0,
                        hasSurfaceRegion ? maxBlockZ : 0
                ),
                ""
        );
    }

    public boolean hasSceneTiedInputs() {
        return this.surfaceSampleCount > 0
                || this.emissiveProximitySignals > 0
                || this.radianceSampleCount > 0
                || this.cacheSampleCountInput > 0
                || this.distinctMaterialCount > 0;
    }

    public boolean readyForSurfaceOnlyProof() {
        return this.surfaceSampleCount > 0
                && this.affectedSurfaceRegionAvailable
                && (this.emissiveProximityAvailable || this.radianceSampleCount > 0 || this.cacheSampleCountInput > 0);
    }

    public boolean hasPhysicalGiEvidence() {
        return this.surfaceSampleCount > 0
                && this.affectedSurfaceRegionAvailable
                && this.surfaceOrientationConfidence >= 0.35F
                && this.materialColorInfluence > 0.03F
                && this.materialGeometryCoupling >= 0.10F
                && this.receiverSurfacePositionScore >= 0.08F
                && this.sourceReceiverEvidenceScore >= 0.05F
                && this.previewPhysicalEvidenceScore >= 0.18F
                && this.physicalGiInputScore >= 0.20F
                && (this.lightSourceSceneCoupling >= 0.05F
                        || this.radianceEnergy > 0.0F
                        || this.cachePhysicalConfidence > 0.05F);
    }

    public boolean hasColoredBounceEvidence() {
        return this.hasPhysicalGiEvidence()
                && this.coloredSurfaceSampleCount > 0
                && this.averageAlbedoSaturation >= COLORED_SURFACE_SATURATION
                && this.coloredBounceInfluence >= 0.08F
                && this.materialColorInfluence >= 0.08F
                && this.materialPositionEvidenceScore >= 0.08F
                && this.coloredBouncePhysicalScore >= 0.12F
                && this.materialGeometryCoupling >= 0.12F
                && this.surfaceOrientationConfidence >= 0.35F
                && this.cachePhysicalConfidence >= 0.05F
                && (this.emissiveSourceCoupling >= 0.04F
                        || this.celestialSourceCoupling >= 0.04F
                        || this.radianceEnergy > 0.0F);
    }

    public boolean hasEmissiveSpillEvidence() {
        return this.hasPhysicalGiEvidence()
                && this.emissiveProximityAvailable
                && this.emissiveSpillPhysicalScore >= 0.10F
                && this.sourceReceiverEvidenceScore >= 0.06F
                && this.occluderShadowCandidateScore >= 0.04F
                && this.emissiveSourceCoupling >= 0.03F;
    }

    public String physicalReadinessLabel() {
        return "physicalEvidence=" + this.hasPhysicalGiEvidence()
                + " coloredBounceEvidence=" + this.hasColoredBounceEvidence()
                + " emissiveSpillEvidence=" + this.hasEmissiveSpillEvidence()
                + " score=" + this.physicalGiInputScore
                + " previewScore=" + this.previewPhysicalEvidenceScore
                + " receiverSurfacePosition=" + this.receiverSurfacePositionScore
                + " sourceReceiverEvidence=" + this.sourceReceiverEvidenceScore
                + " materialPositionEvidence=" + this.materialPositionEvidenceScore
                + " occluderShadowEvidence=" + this.occluderShadowCandidateScore
                + " coloredBouncePhysical=" + this.coloredBouncePhysicalScore
                + " emissiveSpillPhysical=" + this.emissiveSpillPhysicalScore
                + " emissiveProximity=" + this.emissiveProximityScore
                + " emissiveCoupling=" + this.emissiveSourceCoupling
                + " celestialCoupling=" + this.celestialSourceCoupling
                + " lightSceneCoupling=" + this.lightSourceSceneCoupling
                + " albedoRgb=" + this.averageAlbedoR + "," + this.averageAlbedoG + "," + this.averageAlbedoB
                + " albedoSaturation=" + this.averageAlbedoSaturation
                + " coloredSurfaces=" + this.coloredSurfaceSampleCount + "/" + this.surfaceSampleCount
                + " coloredBounceInfluence=" + this.coloredBounceInfluence
                + " materialColor=" + this.materialColorInfluence
                + " materialGeometry=" + this.materialGeometryCoupling
                + " orientationConfidence=" + this.surfaceOrientationConfidence
                + " skylightInteriorContrast=" + this.skylightInteriorContrast
                + " occlusionDirty=" + this.occlusionDirtyRegionInfluence
                + " cachePhysical=" + this.cachePhysicalConfidence
                + " boundary=preview-scene-coupling-not-physically-correct-gi";
    }

    public String physicalEvidenceRejectionLabel() {
        if (this.hasPhysicalGiEvidence()) {
            return "physical GI evidence is scene-linked; artifact-only evidence is still insufficient without controller screenshots";
        }
        return "reject metadata-only=true"
                + " proof-marker=true"
                + " focus-window-only=true"
                + " temporary-direct-light-substitution=true"
                + " full-screen-washout=true"
                + " because " + this.physicalReadinessLabel();
    }

    public String compactLabel() {
        return "surfaces=" + this.surfaceSampleCount
                + " colored=" + this.coloredSurfaceSampleCount + "/" + this.coloredBounceInfluence
                + "/coloredBounceEvidence:" + this.hasColoredBounceEvidence()
                + " albedoRgb=" + this.averageAlbedoR + "," + this.averageAlbedoG + "," + this.averageAlbedoB
                + "/sat:" + this.averageAlbedoSaturation
                + " materials=" + this.distinctMaterialCount + "/" + this.materialDiversityRatio
                + "/colorInfluence:" + this.materialColorInfluence
                + " skylight=" + this.skylitSurfaceCount + "/" + this.skylightExposureRatio
                + " sealed=" + this.sealedInteriorSurfaceCount + "/" + this.sealedInteriorRatio
                + "/contrast:" + this.skylightInteriorContrast
                + " orientation=down:" + this.downwardFacingRatio
                + "/vertical:" + this.verticalSurfaceRatio
                + "/balance:" + this.orientationBalance
                + "/normal:" + this.averageSurfaceNormalX + "," + this.averageSurfaceNormalY + "," + this.averageSurfaceNormalZ
                + "/normalConfidence:" + this.averageNormalLength
                + "/surfaceConfidence:" + this.surfaceOrientationConfidence
                + " surfaceMaterial=roughness:" + this.averageSurfaceRoughness
                + "/cacheConfidence:" + this.averageSurfaceConfidence
                + "/usable:" + this.usableSurfaceConfidenceRatio
                + "/dirty:" + this.dirtySurfaceSampleCount + "/" + this.dirtySurfaceSampleRatio
                + "/opacityHint:" + this.materialOpacityHint
                + " emissiveProximity=" + this.emissiveProximitySignals + "/" + this.emissiveProximityScore
                + "/emissiveCoupling:" + this.emissiveSourceCoupling
                + "/celestialCoupling:" + this.celestialSourceCoupling
                + "/lightSceneCoupling:" + this.lightSourceSceneCoupling
                + "/sourceReceiverEvidence:" + this.sourceReceiverEvidenceScore
                + " dirtyInfluence=" + this.dirtyRegionInfluence
                + "/occlusionDirty:" + this.occlusionDirtyRegionInfluence
                + "/occluderShadowEvidence:" + this.occluderShadowCandidateScore
                + "/available:" + this.emissiveProximityAvailable
                + " affectedSurfaceRegion=\"" + this.affectedSurfaceRegionLabel + "\""
                + " receiverSurfacePosition=" + this.receiverSurfacePositionScore
                + " materialPositionEvidence=" + this.materialPositionEvidenceScore
                + " surfaceOnlyProofReady=" + this.readyForSurfaceOnlyProof()
                + " cacheInput=" + this.cacheConfidenceInput + "/" + this.cacheVarianceInput
                + "/physical:" + this.cachePhysicalConfidence
                + " radianceEnergy=" + this.radianceEnergy
                + " radianceDirectionConfidence=" + this.radianceDirectionConfidence
                + " materialGeometryCoupling=" + this.materialGeometryCoupling
                + " coloredBouncePhysical=" + this.coloredBouncePhysicalScore
                + " emissiveSpillPhysical=" + this.emissiveSpillPhysicalScore
                + " previewPhysicalEvidence=" + this.previewPhysicalEvidenceScore
                + " " + this.physicalReadinessLabel();
    }

    private static float saturation(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        return max <= 0.0F ? 0.0F : clampUnit((max - min) / max);
    }

    private static float luma(float red, float green, float blue) {
        return red * 0.2126F + green * 0.7152F + blue * 0.0722F;
    }

    private static float mix(float from, float to, float weight) {
        float clampedWeight = clampUnit(weight);
        return from + ((to - from) * clampedWeight);
    }

    private static int blockCoordinate(int sectionCoordinate, int localCoordinate) {
        return (sectionCoordinate << 4) + localCoordinate;
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

    private static String defaultLabel(
            int surfaceSampleCount,
            int coloredSurfaceSampleCount,
            int distinctMaterialCount,
            float materialDiversityRatio,
            float coloredBounceInfluence,
            float materialColorInfluence,
            float skylightExposureRatio,
            float sealedInteriorRatio,
            float skylightInteriorContrast,
            float emissiveProximityScore,
            float emissiveSourceCoupling,
            float celestialSourceCoupling,
            float lightSourceCoupling,
            float dirtyRegionInfluence,
            float occlusionDirtyRegionInfluence,
            float orientationBalance,
            float averageSurfaceNormalX,
            float averageSurfaceNormalY,
            float averageSurfaceNormalZ,
            float surfaceOrientationConfidence,
            float averageSurfaceRoughness,
            float averageSurfaceConfidence,
            float usableSurfaceConfidenceRatio,
            float dirtySurfaceSampleRatio,
            float materialOpacityHint,
            float radianceDirectionConfidence,
            float cacheConfidenceInput,
            float cacheVarianceInput,
            float cachePhysicalConfidence,
            float materialGeometryCoupling,
            float physicalGiInputScore,
            float receiverSurfacePositionScore,
            float sourceReceiverEvidenceScore,
            float materialPositionEvidenceScore,
            float occluderShadowCandidateScore,
            float coloredBouncePhysicalScore,
            float emissiveSpillPhysicalScore,
            float previewPhysicalEvidenceScore
    ) {
        return "surfaces=" + surfaceSampleCount
                + " colored=" + coloredSurfaceSampleCount + "/" + coloredBounceInfluence
                + " materials=" + distinctMaterialCount + "/" + materialDiversityRatio
                + "/colorInfluence:" + materialColorInfluence
                + " skylightRatio=" + skylightExposureRatio
                + " sealedRatio=" + sealedInteriorRatio
                + " skylightInteriorContrast=" + skylightInteriorContrast
                + " emissiveProximity=" + emissiveProximityScore
                + " emissiveCoupling=" + emissiveSourceCoupling
                + " celestialCoupling=" + celestialSourceCoupling
                + " lightSceneCoupling=" + lightSourceCoupling
                + " sourceReceiverEvidence=" + sourceReceiverEvidenceScore
                + " dirtyInfluence=" + dirtyRegionInfluence
                + "/occlusionDirty:" + occlusionDirtyRegionInfluence
                + "/occluderShadowEvidence:" + occluderShadowCandidateScore
                + " orientationBalance=" + orientationBalance
                + "/normal:" + averageSurfaceNormalX + "," + averageSurfaceNormalY + "," + averageSurfaceNormalZ
                + "/surfaceConfidence:" + surfaceOrientationConfidence
                + " materialSurface=roughness:" + averageSurfaceRoughness
                + "/cacheConfidence:" + averageSurfaceConfidence
                + "/usable:" + usableSurfaceConfidenceRatio
                + "/dirty:" + dirtySurfaceSampleRatio
                + "/opacityHint:" + materialOpacityHint
                + "/receiverSurfacePosition:" + receiverSurfacePositionScore
                + "/materialPositionEvidence:" + materialPositionEvidenceScore
                + " radianceDirectionConfidence=" + radianceDirectionConfidence
                + " cache=" + cacheConfidenceInput + "/" + cacheVarianceInput
                + "/physical:" + cachePhysicalConfidence
                + " materialGeometryCoupling=" + materialGeometryCoupling
                + " physicalGiInputScore=" + physicalGiInputScore
                + " coloredBouncePhysical=" + coloredBouncePhysicalScore
                + " emissiveSpillPhysical=" + emissiveSpillPhysicalScore
                + " previewPhysicalEvidence=" + previewPhysicalEvidenceScore
                + " boundary=preview-scene-coupling-not-physically-correct-gi";
    }

    private static String defaultSurfaceRegionLabel(
            boolean available,
            int minBlockX,
            int minBlockY,
            int minBlockZ,
            int maxBlockX,
            int maxBlockY,
            int maxBlockZ
    ) {
        if (!available) {
            return "affected surface region unavailable";
        }
        return "blocks=[" + minBlockX + "," + minBlockY + "," + minBlockZ + " -> "
                + maxBlockX + "," + maxBlockY + "," + maxBlockZ + "]";
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI scene inputs");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
