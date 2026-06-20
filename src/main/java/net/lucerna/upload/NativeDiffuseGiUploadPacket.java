package net.lucerna.upload;

import net.lucerna.render.lighting.gi.DiffuseGiSourceSummary;
import net.lucerna.render.lighting.gi.LowResDiffuseGiPlan;

import java.util.Objects;

public final class NativeDiffuseGiUploadPacket {
    private final long generation;
    private final NativeDiffuseGiPlanUpload planUpload;
    private final NativeDiffuseGiCacheUpload cacheUpload;

    private NativeDiffuseGiUploadPacket(
            long generation,
            NativeDiffuseGiPlanUpload planUpload,
            NativeDiffuseGiCacheUpload cacheUpload
    ) {
        this.generation = Math.max(generation, Math.max(
                Objects.requireNonNull(planUpload, "planUpload").generation(),
                Objects.requireNonNull(cacheUpload, "cacheUpload").cacheGeneration()
        ));
        this.planUpload = planUpload;
        this.cacheUpload = cacheUpload;
        this.validate();
    }

    public static NativeDiffuseGiUploadPacket from(LowResDiffuseGiPlan plan) {
        Objects.requireNonNull(plan, "plan");
        NativeDiffuseGiPlanUpload planUpload = NativeDiffuseGiPlanUpload.from(plan);
        NativeDiffuseGiCacheUpload cacheUpload = NativeDiffuseGiCacheUpload.from(plan.cacheSnapshot());
        return new NativeDiffuseGiUploadPacket(planUpload.generation(), planUpload, cacheUpload);
    }

    public static NativeDiffuseGiUploadPacket from(
            LowResDiffuseGiPlan plan,
            NativeDirectLightingUploadPacket directLightingUpload,
            NativeUploadStagingMetadata stagingMetadata
    ) {
        Objects.requireNonNull(plan, "plan");
        NativeUploadStagingMetadata resolvedMetadata = stagingMetadata == null
                ? NativeUploadStagingMetadata.empty()
                : stagingMetadata;
        LowResDiffuseGiPlan planWithSources = plan.withSourceSummary(DiffuseGiSourceSummary.of(
                directLightingUpload == null ? 0L : directLightingUpload.generation(),
                resolvedMetadata.lastWorldGeneration(),
                resolvedMetadata.materialGeneration(),
                resolvedMetadata.sectionGeneration(),
                resolvedMetadata.sectionDirtyRegionGeneration(),
                directLightingUpload != null && (directLightingUpload.flags() & NativeDirectLightingUploadPacket.FLAG_VALIDATED) != 0,
                directLightingUpload == null ? 0 : directLightingUpload.celestialLightCount(),
                directLightingUpload == null ? 0 : directLightingUpload.selectedEmissiveCount(),
                directLightingUpload == null ? 0 : directLightingUpload.shadowCandidateCount(),
                directLightingUpload == null ? 0 : directLightingUpload.budgetedShadowCandidateCount(),
                directLightingUpload == null ? resolvedMetadata.sectionSnapshotCount() : directLightingUpload.sectionSnapshotCount(),
                resolvedMetadata.dirtyRegionCount(),
                resolvedMetadata.materialUpdateCount(),
                "direct/upload + world/material/dirty staging"
        ));
        return from(planWithSources);
    }

    public static NativeDiffuseGiUploadPacket of(
            NativeDiffuseGiPlanUpload planUpload,
            NativeDiffuseGiCacheUpload cacheUpload
    ) {
        return new NativeDiffuseGiUploadPacket(0L, planUpload, cacheUpload);
    }

    public long generation() {
        return this.generation;
    }

    public NativeDiffuseGiPlanUpload planUpload() {
        return this.planUpload;
    }

    public NativeDiffuseGiCacheUpload cacheUpload() {
        return this.cacheUpload;
    }

    public boolean readyForScheduling() {
        return this.planUpload.readyForScheduling();
    }

    public boolean requiresTracing() {
        return this.planUpload.requiresTracing();
    }

    public boolean reusesTemporalHistory() {
        return this.planUpload.reusesTemporalHistory();
    }

    public boolean cacheUsable() {
        return this.planUpload.cacheUsable();
    }

    public boolean isEmpty() {
        return !this.readyForScheduling() && this.cacheUpload.isEmpty();
    }

    public int dirtyRegionCount() {
        return this.cacheUpload.dirtyRegionCount();
    }

    public int surfaceRecordCount() {
        return this.cacheUpload.surfaceRecordCount();
    }

    public int radianceRecordCount() {
        return this.cacheUpload.radianceRecordCount();
    }

    public int[] gridDimensions() {
        return this.planUpload.gridDimensions();
    }

    public int[] settingsIntegers() {
        return this.planUpload.settingsIntegers();
    }

    public float[] settingsFloats() {
        return this.planUpload.settingsFloats();
    }

    public int[] rayBudgetData() {
        return this.planUpload.rayBudgetData();
    }

    public long[] temporalFrames() {
        return this.planUpload.temporalFrames();
    }

    public int[] temporalState() {
        return this.planUpload.temporalState();
    }

    public float[] temporalFloats() {
        return this.planUpload.temporalFloats();
    }

    public float[] cacheConfidenceFloats() {
        return this.planUpload.cacheConfidenceFloats();
    }

    public int[] cacheConfidenceIntegers() {
        return this.planUpload.cacheConfidenceIntegers();
    }

    public long[] cacheConfidenceGenerations() {
        return this.planUpload.cacheConfidenceGenerations();
    }

    public int[] cacheCounts() {
        return this.planUpload.cacheCounts();
    }

    public long[] cacheGenerations() {
        return this.planUpload.cacheGenerations();
    }

    public long[] sourceGenerations() {
        return this.planUpload.sourceGenerations();
    }

    public int[] sourceFlags() {
        return this.planUpload.sourceFlags();
    }

    public int[] sourceCounts() {
        return this.planUpload.sourceCounts();
    }

    public int[] sceneInputIntegers() {
        return this.planUpload.sceneInputIntegers();
    }

    public float[] sceneInputFloats() {
        return this.planUpload.sceneInputFloats();
    }

    public String[] debugLabels() {
        return this.planUpload.debugLabels();
    }

    public int[] dirtyRegionTypeIds() {
        return this.cacheUpload.dirtyRegionTypeIds();
    }

    public String[] dirtyRegionTypeNames() {
        return this.cacheUpload.dirtyRegionTypeNames();
    }

    public String[] dirtyRegionDimensions() {
        return this.cacheUpload.dirtyRegionDimensions();
    }

    public int[] dirtyRegionSections() {
        return this.cacheUpload.dirtyRegionSections();
    }

    public int[] dirtyRegionSectionScoped() {
        return this.cacheUpload.dirtyRegionSectionScoped();
    }

    public long[] dirtyRegionGenerations() {
        return this.cacheUpload.dirtyRegionGenerations();
    }

    public String[] surfaceDimensions() {
        return this.cacheUpload.surfaceDimensions();
    }

    public int[] surfaceKeys() {
        return this.cacheUpload.surfaceKeys();
    }

    public long[] surfaceGenerations() {
        return this.cacheUpload.surfaceGenerations();
    }

    public int[] surfaceMaterialIds() {
        return this.cacheUpload.surfaceMaterialIds();
    }

    public float[] surfaceProperties() {
        return this.cacheUpload.surfaceProperties();
    }

    public float[] surfaceConfidenceFloats() {
        return this.cacheUpload.surfaceConfidenceFloats();
    }

    public int[] surfaceConfidenceIntegers() {
        return this.cacheUpload.surfaceConfidenceIntegers();
    }

    public long[] surfaceConfidenceGenerations() {
        return this.cacheUpload.surfaceConfidenceGenerations();
    }

    public String[] radianceDimensions() {
        return this.cacheUpload.radianceDimensions();
    }

    public int[] radianceKeys() {
        return this.cacheUpload.radianceKeys();
    }

    public long[] radianceGenerations() {
        return this.cacheUpload.radianceGenerations();
    }

    public float[] radianceProperties() {
        return this.cacheUpload.radianceProperties();
    }

    public int[] radianceSampleCounts() {
        return this.cacheUpload.radianceSampleCounts();
    }

    public long[] radianceLastFrameIndices() {
        return this.cacheUpload.radianceLastFrameIndices();
    }

    public float[] radianceConfidenceFloats() {
        return this.cacheUpload.radianceConfidenceFloats();
    }

    public int[] radianceConfidenceIntegers() {
        return this.cacheUpload.radianceConfidenceIntegers();
    }

    public long[] radianceConfidenceGenerations() {
        return this.cacheUpload.radianceConfidenceGenerations();
    }

    private void validate() {
        requireNonNegative(this.generation, "generation");
        if (this.planUpload.cacheGeneration() != this.cacheUpload.cacheGeneration()) {
            throw new IllegalArgumentException("plan and cache upload cache generations must match");
        }
        if (this.planUpload.dirtyRegionCount() != this.cacheUpload.dirtyRegionCount()) {
            throw new IllegalArgumentException("plan and cache upload dirty region counts must match");
        }
        if (this.planUpload.firstDirtyRegionGeneration() != this.cacheUpload.firstDirtyRegionGeneration()
                || this.planUpload.lastDirtyRegionGeneration() != this.cacheUpload.lastDirtyRegionGeneration()) {
            throw new IllegalArgumentException("plan and cache upload dirty generation bounds must match");
        }
        if (this.planUpload.surfaceRecordCount() != this.cacheUpload.surfaceRecordCount()) {
            throw new IllegalArgumentException("plan and cache upload surface record counts must match");
        }
        if (this.planUpload.radianceRecordCount() != this.cacheUpload.radianceRecordCount()) {
            throw new IllegalArgumentException("plan and cache upload radiance record counts must match");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
