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
        int skylitSurfaceCount,
        int sealedInteriorSurfaceCount,
        int downwardFacingSurfaceCount,
        int verticalSurfaceCount,
        float skylightExposureRatio,
        float sealedInteriorRatio,
        float downwardFacingRatio,
        float verticalSurfaceRatio,
        float orientationBalance,
        float averageNormalLength,
        float emissiveProximityScore,
        int emissiveProximitySignals,
        float dirtyRegionInfluence,
        float cacheConfidenceInput,
        float cacheVarianceInput,
        int cacheSampleCountInput,
        boolean cacheDirtyInput,
        int radianceSampleCount,
        float averageRadianceR,
        float averageRadianceG,
        float averageRadianceB,
        float radianceEnergy,
        float radianceDirectionConfidence,
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
        skylitSurfaceCount = Math.max(0, skylitSurfaceCount);
        sealedInteriorSurfaceCount = Math.max(0, sealedInteriorSurfaceCount);
        downwardFacingSurfaceCount = Math.max(0, downwardFacingSurfaceCount);
        verticalSurfaceCount = Math.max(0, verticalSurfaceCount);
        skylightExposureRatio = clampUnit(skylightExposureRatio);
        sealedInteriorRatio = clampUnit(sealedInteriorRatio);
        downwardFacingRatio = clampUnit(downwardFacingRatio);
        verticalSurfaceRatio = clampUnit(verticalSurfaceRatio);
        orientationBalance = clampUnit(orientationBalance);
        averageNormalLength = clampUnit(averageNormalLength);
        emissiveProximityScore = clampUnit(emissiveProximityScore);
        emissiveProximitySignals = Math.max(0, emissiveProximitySignals);
        dirtyRegionInfluence = clampUnit(dirtyRegionInfluence);
        cacheConfidenceInput = clampUnit(cacheConfidenceInput);
        cacheVarianceInput = finiteNonNegative(cacheVarianceInput);
        cacheSampleCountInput = Math.max(0, cacheSampleCountInput);
        radianceSampleCount = Math.max(0, radianceSampleCount);
        averageRadianceR = finiteNonNegative(averageRadianceR);
        averageRadianceG = finiteNonNegative(averageRadianceG);
        averageRadianceB = finiteNonNegative(averageRadianceB);
        radianceEnergy = finiteNonNegative(radianceEnergy);
        radianceDirectionConfidence = clampUnit(radianceDirectionConfidence);
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
                skylightExposureRatio,
                sealedInteriorRatio,
                emissiveProximityScore,
                dirtyRegionInfluence,
                orientationBalance,
                radianceDirectionConfidence,
                cacheConfidenceInput,
                cacheVarianceInput
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
                0,
                0.0F,
                0.0F,
                1.0F,
                0,
                false,
                0,
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
            normalLengthTotal += clampUnit(vectorLength(surface.normalX(), surface.normalY(), surface.normalZ()));
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
        float averageAlbedoR = albedoR * inverseSurfaceCount;
        float averageAlbedoG = albedoG * inverseSurfaceCount;
        float averageAlbedoB = albedoB * inverseSurfaceCount;
        float averageSaturation = saturationTotal * inverseSurfaceCount;
        float coloredRatio = surfaceCount == 0 ? 0.0F : (float) coloredCount / surfaceCount;
        float coloredInfluence = clampUnit((averageSaturation * 0.65F) + (coloredRatio * 0.35F));
        float skylightRatio = surfaceCount == 0 ? 0.0F : (float) skylitCount / surfaceCount;
        float sealedRatio = surfaceCount == 0 ? 0.0F : (float) sealedCount / surfaceCount;
        int distinctMaterials = materialIds.size();
        float materialDiversity = surfaceCount == 0 ? 0.0F : (float) distinctMaterials / surfaceCount;
        float downwardRatio = surfaceCount == 0 ? 0.0F : (float) downwardCount / surfaceCount;
        float verticalRatio = surfaceCount == 0 ? 0.0F : (float) verticalCount / surfaceCount;
        float dominantOrientationRatio = Math.max(skylightRatio, Math.max(verticalRatio, downwardRatio));
        float orientationBalance = surfaceCount == 0 ? 0.0F : clampUnit(1.0F - dominantOrientationRatio);
        float averageNormalLength = normalLengthTotal * inverseSurfaceCount;

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
        float dirtyInfluence = clampUnit((resolvedSource.dirtyRegionCount() + (resolvedSource.materialUpdateCount() * 0.5F))
                / Math.max(1.0F, surfaceCount + resolvedSource.dirtyRegionCount() + resolvedSource.materialUpdateCount()));
        boolean hasSurfaceRegion = surfaceCount > 0;
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
                skylitCount,
                sealedCount,
                downwardCount,
                verticalCount,
                skylightRatio,
                sealedRatio,
                downwardRatio,
                verticalRatio,
                orientationBalance,
                averageNormalLength,
                emissiveProximity,
                proximitySignals,
                dirtyInfluence,
                resolvedConfidence.confidence(),
                resolvedConfidence.variance(),
                resolvedConfidence.sampleCount(),
                resolvedConfidence.dirty(),
                radianceSamples,
                averageRadianceR,
                averageRadianceG,
                averageRadianceB,
                radianceEnergy,
                radianceDirectionConfidence,
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

    public String compactLabel() {
        return "surfaces=" + this.surfaceSampleCount
                + " colored=" + this.coloredSurfaceSampleCount + "/" + this.coloredBounceInfluence
                + " materials=" + this.distinctMaterialCount + "/" + this.materialDiversityRatio
                + " skylight=" + this.skylitSurfaceCount + "/" + this.skylightExposureRatio
                + " sealed=" + this.sealedInteriorSurfaceCount + "/" + this.sealedInteriorRatio
                + " orientation=down:" + this.downwardFacingRatio
                + "/vertical:" + this.verticalSurfaceRatio
                + "/balance:" + this.orientationBalance
                + "/normalConfidence:" + this.averageNormalLength
                + " emissiveProximity=" + this.emissiveProximitySignals + "/" + this.emissiveProximityScore
                + " dirtyInfluence=" + this.dirtyRegionInfluence
                + "/available:" + this.emissiveProximityAvailable
                + " affectedSurfaceRegion=\"" + this.affectedSurfaceRegionLabel + "\""
                + " surfaceOnlyProofReady=" + this.readyForSurfaceOnlyProof()
                + " cacheInput=" + this.cacheConfidenceInput + "/" + this.cacheVarianceInput
                + " radianceEnergy=" + this.radianceEnergy
                + " radianceDirectionConfidence=" + this.radianceDirectionConfidence;
    }

    private static float saturation(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        return max <= 0.0F ? 0.0F : clampUnit((max - min) / max);
    }

    private static float luma(float red, float green, float blue) {
        return red * 0.2126F + green * 0.7152F + blue * 0.0722F;
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

    private static String defaultLabel(
            int surfaceSampleCount,
            int coloredSurfaceSampleCount,
            int distinctMaterialCount,
            float materialDiversityRatio,
            float coloredBounceInfluence,
            float skylightExposureRatio,
            float sealedInteriorRatio,
            float emissiveProximityScore,
            float dirtyRegionInfluence,
            float orientationBalance,
            float radianceDirectionConfidence,
            float cacheConfidenceInput,
            float cacheVarianceInput
    ) {
        return "surfaces=" + surfaceSampleCount
                + " colored=" + coloredSurfaceSampleCount + "/" + coloredBounceInfluence
                + " materials=" + distinctMaterialCount + "/" + materialDiversityRatio
                + " skylightRatio=" + skylightExposureRatio
                + " sealedRatio=" + sealedInteriorRatio
                + " emissiveProximity=" + emissiveProximityScore
                + " dirtyInfluence=" + dirtyRegionInfluence
                + " orientationBalance=" + orientationBalance
                + " radianceDirectionConfidence=" + radianceDirectionConfidence
                + " cache=" + cacheConfidenceInput + "/" + cacheVarianceInput;
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
