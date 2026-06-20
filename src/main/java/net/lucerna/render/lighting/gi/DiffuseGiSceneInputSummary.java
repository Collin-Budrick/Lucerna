package net.lucerna.render.lighting.gi;

import java.util.Objects;

public record DiffuseGiSceneInputSummary(
        int surfaceSampleCount,
        int coloredSurfaceSampleCount,
        float averageAlbedoR,
        float averageAlbedoG,
        float averageAlbedoB,
        float averageAlbedoSaturation,
        float coloredBounceInfluence,
        int skylitSurfaceCount,
        int sealedInteriorSurfaceCount,
        float skylightExposureRatio,
        float sealedInteriorRatio,
        float emissiveProximityScore,
        int emissiveProximitySignals,
        float cacheConfidenceInput,
        float cacheVarianceInput,
        int cacheSampleCountInput,
        boolean cacheDirtyInput,
        int radianceSampleCount,
        float averageRadianceR,
        float averageRadianceG,
        float averageRadianceB,
        float radianceEnergy,
        String debugLabel
) {
    private static final float SKYWARD_NORMAL_Y = 0.55F;
    private static final float SEALED_NORMAL_Y = 0.25F;
    private static final float COLORED_SURFACE_SATURATION = 0.12F;
    private static final float BRIGHT_SURFACE_LUMA = 0.45F;
    private static final float DARK_SURFACE_LUMA = 0.24F;

    public DiffuseGiSceneInputSummary {
        surfaceSampleCount = Math.max(0, surfaceSampleCount);
        coloredSurfaceSampleCount = Math.max(0, coloredSurfaceSampleCount);
        averageAlbedoR = clampUnit(averageAlbedoR);
        averageAlbedoG = clampUnit(averageAlbedoG);
        averageAlbedoB = clampUnit(averageAlbedoB);
        averageAlbedoSaturation = clampUnit(averageAlbedoSaturation);
        coloredBounceInfluence = clampUnit(coloredBounceInfluence);
        skylitSurfaceCount = Math.max(0, skylitSurfaceCount);
        sealedInteriorSurfaceCount = Math.max(0, sealedInteriorSurfaceCount);
        skylightExposureRatio = clampUnit(skylightExposureRatio);
        sealedInteriorRatio = clampUnit(sealedInteriorRatio);
        emissiveProximityScore = clampUnit(emissiveProximityScore);
        emissiveProximitySignals = Math.max(0, emissiveProximitySignals);
        cacheConfidenceInput = clampUnit(cacheConfidenceInput);
        cacheVarianceInput = finiteNonNegative(cacheVarianceInput);
        cacheSampleCountInput = Math.max(0, cacheSampleCountInput);
        radianceSampleCount = Math.max(0, radianceSampleCount);
        averageRadianceR = finiteNonNegative(averageRadianceR);
        averageRadianceG = finiteNonNegative(averageRadianceG);
        averageRadianceB = finiteNonNegative(averageRadianceB);
        radianceEnergy = finiteNonNegative(radianceEnergy);
        debugLabel = clean(debugLabel, defaultLabel(
                surfaceSampleCount,
                coloredSurfaceSampleCount,
                coloredBounceInfluence,
                skylightExposureRatio,
                sealedInteriorRatio,
                emissiveProximityScore,
                cacheConfidenceInput,
                cacheVarianceInput
        ));
    }

    public static DiffuseGiSceneInputSummary unavailable() {
        return new DiffuseGiSceneInputSummary(
                0,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0,
                0,
                0.0F,
                0.0F,
                0.0F,
                0,
                0.0F,
                1.0F,
                0,
                false,
                0,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
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

        for (SurfaceCacheRecord surface : resolvedCache.surfaceRecords()) {
            albedoR += surface.albedoR();
            albedoG += surface.albedoG();
            albedoB += surface.albedoB();
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

        int radianceSamples = 0;
        float radianceR = 0.0F;
        float radianceG = 0.0F;
        float radianceB = 0.0F;
        for (RadianceCacheRecord radiance : resolvedCache.radianceRecords()) {
            int weight = Math.max(1, radiance.sampleCount());
            radianceSamples += radiance.sampleCount();
            radianceR += radiance.radianceR() * weight;
            radianceG += radiance.radianceG() * weight;
            radianceB += radiance.radianceB() * weight;
        }
        int radianceWeight = Math.max(1, resolvedCache.radianceRecords().stream()
                .mapToInt(record -> Math.max(1, record.sampleCount()))
                .sum());
        float averageRadianceR = resolvedCache.hasRadianceRecords() ? radianceR / radianceWeight : 0.0F;
        float averageRadianceG = resolvedCache.hasRadianceRecords() ? radianceG / radianceWeight : 0.0F;
        float averageRadianceB = resolvedCache.hasRadianceRecords() ? radianceB / radianceWeight : 0.0F;
        float radianceEnergy = averageRadianceR + averageRadianceG + averageRadianceB;

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

        return new DiffuseGiSceneInputSummary(
                surfaceCount,
                coloredCount,
                averageAlbedoR,
                averageAlbedoG,
                averageAlbedoB,
                averageSaturation,
                coloredInfluence,
                skylitCount,
                sealedCount,
                skylightRatio,
                sealedRatio,
                emissiveProximity,
                proximitySignals,
                resolvedConfidence.confidence(),
                resolvedConfidence.variance(),
                resolvedConfidence.sampleCount(),
                resolvedConfidence.dirty(),
                radianceSamples,
                averageRadianceR,
                averageRadianceG,
                averageRadianceB,
                radianceEnergy,
                ""
        );
    }

    public boolean hasSceneTiedInputs() {
        return this.surfaceSampleCount > 0
                || this.emissiveProximitySignals > 0
                || this.radianceSampleCount > 0
                || this.cacheSampleCountInput > 0;
    }

    public String compactLabel() {
        return "surfaces=" + this.surfaceSampleCount
                + " colored=" + this.coloredSurfaceSampleCount + "/" + this.coloredBounceInfluence
                + " skylight=" + this.skylitSurfaceCount + "/" + this.skylightExposureRatio
                + " sealed=" + this.sealedInteriorSurfaceCount + "/" + this.sealedInteriorRatio
                + " emissiveProximity=" + this.emissiveProximitySignals + "/" + this.emissiveProximityScore
                + " cacheInput=" + this.cacheConfidenceInput + "/" + this.cacheVarianceInput
                + " radianceEnergy=" + this.radianceEnergy;
    }

    private static float saturation(float red, float green, float blue) {
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        return max <= 0.0F ? 0.0F : clampUnit((max - min) / max);
    }

    private static float luma(float red, float green, float blue) {
        return red * 0.2126F + green * 0.7152F + blue * 0.0722F;
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
            float coloredBounceInfluence,
            float skylightExposureRatio,
            float sealedInteriorRatio,
            float emissiveProximityScore,
            float cacheConfidenceInput,
            float cacheVarianceInput
    ) {
        return "surfaces=" + surfaceSampleCount
                + " colored=" + coloredSurfaceSampleCount + "/" + coloredBounceInfluence
                + " skylightRatio=" + skylightExposureRatio
                + " sealedRatio=" + sealedInteriorRatio
                + " emissiveProximity=" + emissiveProximityScore
                + " cache=" + cacheConfidenceInput + "/" + cacheVarianceInput;
    }

    private static String clean(String value, String fallback) {
        String resolvedFallback = Objects.requireNonNullElse(fallback, "GI scene inputs");
        if (value == null || value.isBlank()) {
            return resolvedFallback;
        }
        return value.trim();
    }
}
