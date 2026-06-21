package net.lucerna.render.tracing.voxel;

import net.lucerna.render.tracing.TracedLightingConsumptionEvidence;
import net.lucerna.render.voxel.VoxelSectionSnapshotReference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Round10VoxelTraversalOutputSummary(
        long generation,
        long rayCount,
        long hitCount,
        long missCount,
        long wallHitCount,
        long openSkyMissCount,
        long stepCount,
        double averageStepsPerRay,
        long skippedSectionCount,
        long materialHitCount,
        long sourceCoupledBounceCount,
        String sourceCoupledBounceSource,
        long glassWaterMaterialHitCount,
        long opaqueMaterialHitCount,
        String materialHitSource,
        String materialIdConsistency,
        boolean emptySectionSkipSafe,
        boolean maskBitsReady,
        String maskBitsSource,
        String traversalBackend,
        boolean realGpuTraversalExecuted,
        String boundary
) {
    public Round10VoxelTraversalOutputSummary {
        requireNonNegative(generation, "generation");
        requireNonNegative(rayCount, "rayCount");
        requireNonNegative(hitCount, "hitCount");
        requireNonNegative(missCount, "missCount");
        requireNonNegative(wallHitCount, "wallHitCount");
        requireNonNegative(openSkyMissCount, "openSkyMissCount");
        requireNonNegative(stepCount, "stepCount");
        if (!Double.isFinite(averageStepsPerRay) || averageStepsPerRay < 0.0D) {
            throw new IllegalArgumentException("averageStepsPerRay must be finite and non-negative");
        }
        requireNonNegative(skippedSectionCount, "skippedSectionCount");
        requireNonNegative(materialHitCount, "materialHitCount");
        requireNonNegative(sourceCoupledBounceCount, "sourceCoupledBounceCount");
        sourceCoupledBounceSource = requireText(sourceCoupledBounceSource, "sourceCoupledBounceSource");
        requireNonNegative(glassWaterMaterialHitCount, "glassWaterMaterialHitCount");
        requireNonNegative(opaqueMaterialHitCount, "opaqueMaterialHitCount");
        materialHitSource = requireText(materialHitSource, "materialHitSource");
        materialIdConsistency = requireText(materialIdConsistency, "materialIdConsistency");
        maskBitsSource = requireText(maskBitsSource, "maskBitsSource");
        traversalBackend = requireText(traversalBackend, "traversalBackend");
        boundary = requireText(boundary, "boundary");
        if (hitCount > rayCount || missCount > rayCount - hitCount) {
            throw new IllegalArgumentException("hitCount plus missCount cannot exceed rayCount");
        }
        if (wallHitCount > hitCount) {
            throw new IllegalArgumentException("wallHitCount cannot exceed hitCount");
        }
        if (openSkyMissCount > missCount) {
            throw new IllegalArgumentException("openSkyMissCount cannot exceed missCount");
        }
        if (rayCount == 0 && stepCount != 0) {
            throw new IllegalArgumentException("stepCount requires at least one ray");
        }
        if (materialHitCount > hitCount) {
            throw new IllegalArgumentException("materialHitCount cannot exceed hitCount");
        }
        if (sourceCoupledBounceCount > hitCount) {
            throw new IllegalArgumentException("sourceCoupledBounceCount cannot exceed hitCount");
        }
        if (glassWaterMaterialHitCount + opaqueMaterialHitCount > materialHitCount) {
            throw new IllegalArgumentException(
                    "glassWaterMaterialHitCount plus opaqueMaterialHitCount cannot exceed materialHitCount"
            );
        }
        if (realGpuTraversalExecuted) {
            String backend = traversalBackend.toLowerCase(java.util.Locale.ROOT);
            if (backend.contains("cpu") || backend.contains("metadata") || backend.contains("section_snapshot")) {
                throw new IllegalArgumentException(
                        "realGpuTraversalExecuted requires a non-CPU, non-metadata traversal backend label"
                );
            }
        }
    }

    public Round10VoxelTraversalOutputSummary(
            long generation,
            long rayCount,
            long hitCount,
            long missCount,
            long stepCount,
            double averageStepsPerRay,
            long skippedSectionCount,
            long materialHitCount,
            String materialHitSource,
            String backend,
            String boundary
    ) {
        this(
                generation,
                rayCount,
                hitCount,
                missCount,
                0L,
                0L,
                stepCount,
                averageStepsPerRay,
                skippedSectionCount,
                materialHitCount,
                0L,
                "not_recorded",
                0L,
                0L,
                materialHitSource,
                "not_checked",
                true,
                false,
                "not_recorded",
                backend,
                false,
                boundary
        );
    }

    public static Round10VoxelTraversalOutputSummary notRun(long generation) {
        return new Round10VoxelTraversalOutputSummary(
                generation,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0.0D,
                0L,
                0L,
                0L,
                "not_run",
                0L,
                0L,
                "not_run",
                "not_checked",
                true,
                false,
                "not_uploaded",
                "not_run",
                false,
                "round10_voxel_traversal_not_evaluated"
        );
    }

    public static Round10VoxelTraversalOutputSummary boundedCpuFromSectionSnapshots(
            long generation,
            List<VoxelSectionSnapshotReference> sections
    ) {
        return boundedFromSectionSnapshots(
                generation,
                sections,
                "cpu_bounded_section_snapshot_traversal",
                "bounded_cpu_section_snapshot_material_depth_source_evidence_not_gpu_traversal"
        );
    }

    public static Round10VoxelTraversalOutputSummary boundedNativeFromSectionSnapshots(
            long generation,
            List<VoxelSectionSnapshotReference> sections
    ) {
        return boundedFromSectionSnapshots(
                generation,
                sections,
                "native_bounded_section_snapshot_traversal",
                "bounded_native_section_snapshot_material_depth_source_evidence_not_gpu_traversal"
        );
    }

    public static Round10VoxelTraversalOutputSummary metadataOnlyFromSectionSnapshots(
            long generation,
            List<VoxelSectionSnapshotReference> sections
    ) {
        return boundedFromSectionSnapshots(
                generation,
                sections,
                "metadata_only_section_snapshot_status",
                "section_snapshot_metadata_only_traversal_evidence_incomplete_for_gi_consumption"
        );
    }

    public boolean hasTraversalEvidence() {
        return this.rayCount > 0 && this.stepCount > 0 && (this.hitCount > 0 || this.missCount > 0);
    }

    public boolean hasMaterialHitEvidence() {
        return this.materialHitCount > 0 && !"not_run".equals(this.materialHitSource);
    }

    public boolean hasSourceCoupledBounceEvidence() {
        return this.sourceCoupledBounceCount > 0L && !"not_run".equals(this.sourceCoupledBounceSource);
    }

    public boolean hasDepthCoupledHitEvidence() {
        return this.depthCoupledHitCount() > 0L;
    }

    public long depthCoupledHitCount() {
        return this.wallHitCount;
    }

    public boolean hasMaterialDepthSourceCoupling() {
        return this.hasMaterialHitEvidence()
                && this.hasDepthCoupledHitEvidence()
                && this.hasSourceCoupledBounceEvidence();
    }

    public boolean hasTerrainMaterialCorrectnessEvidence() {
        return this.wallHitCount > 0
                || this.openSkyMissCount > 0
                || this.glassWaterMaterialHitCount > 0
                || this.opaqueMaterialHitCount > 0
                || !"not_checked".equals(this.materialIdConsistency);
    }

    public String backend() {
        return this.traversalBackend;
    }

    public String correctnessLabel() {
        return "wallHitCount=" + this.wallHitCount
                + ",openSkyMissCount=" + this.openSkyMissCount
                + ",glassWaterMaterialHitCount=" + this.glassWaterMaterialHitCount
                + ",opaqueMaterialHitCount=" + this.opaqueMaterialHitCount
                + ",sourceCoupledBounceCount=" + this.sourceCoupledBounceCount
                + ",sourceCoupledBounceSource=" + this.sourceCoupledBounceSource
                + ",materialIdConsistency=" + this.materialIdConsistency
                + ",emptySectionSkipSafe=" + this.emptySectionSkipSafe
                + ",maskBitsReady=" + this.maskBitsReady
                + ",maskBitsSource=" + this.maskBitsSource
                + ",traversalBackend=" + this.traversalBackend
                + ",realGpuTraversalExecuted=" + this.realGpuTraversalExecuted
                + ",boundary=" + this.boundary;
    }

    public TracedLightingConsumptionEvidence toTraceConsumptionEvidence(
            long cacheReadCount,
            long cacheWriteCount,
            boolean finalGiSourceConsumed,
            String finalGiSource
    ) {
        return TracedLightingConsumptionEvidence.fromVoxelTraversal(
                this,
                cacheReadCount,
                cacheWriteCount,
                finalGiSourceConsumed,
                finalGiSource
        );
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static Round10VoxelTraversalOutputSummary boundedFromSectionSnapshots(
            long generation,
            List<VoxelSectionSnapshotReference> sections,
            String traversalBackend,
            String completeBoundary
    ) {
        requireNonNegative(generation, "generation");
        Objects.requireNonNull(sections, "sections");
        traversalBackend = requireText(traversalBackend, "traversalBackend");
        completeBoundary = requireText(completeBoundary, "completeBoundary");
        boolean metadataOnlyBackend = traversalBackend.contains("metadata_only");

        long resolvedGeneration = generation;
        long wallHitCount = 0L;
        long openSkyMissCount = 0L;
        long skippedSectionCount = 0L;
        long glassWaterMaterialHitCount = 0L;
        long opaqueMaterialHitCount = 0L;
        long surfaceSampleMaterialHitCount = 0L;
        long emissiveSourceCount = 0L;
        long occupiedVoxelCount = 0L;
        long occupiedSectionCount = 0L;
        long concreteMaskReadySectionCount = 0L;
        long metadataOnlyMaskSectionCount = 0L;
        long missingConcreteMaskSectionCount = 0L;
        long materialLookupSectionCount = 0L;
        long materialMaskReadySectionCount = 0L;
        long materialPaletteOnlySectionCount = 0L;
        long missingMaterialMaskSectionCount = 0L;
        long sourceCoupledBounceCandidateCount = 0L;
        boolean emptySectionSkipSafe = true;
        Set<String> materialSources = new LinkedHashSet<>();
        Set<String> maskSources = new LinkedHashSet<>();

        for (VoxelSectionSnapshotReference section : sections) {
            Objects.requireNonNull(section, "sections must not contain null entries");
            resolvedGeneration = Math.max(resolvedGeneration, section.combinedGeneration());
            wallHitCount += section.depthWallHitEvidenceCount();
            openSkyMissCount += section.openSkyMissEvidenceCount();
            skippedSectionCount += section.skippedEmptySectionEvidenceCount();
            occupiedVoxelCount += section.occupiedVoxelCount();
            if (section.hasOccupiedVoxels()) {
                occupiedSectionCount++;
            }
            glassWaterMaterialHitCount += section.glassWaterMaterialHitEvidenceCount();
            opaqueMaterialHitCount += section.opaqueMaterialHitEvidenceCount();
            if (section.materialHitEvidenceCount() > 0) {
                materialSources.add(section.materialEvidenceSource());
            } else if (section.hasMaterialLookupReady()) {
                materialPaletteOnlySectionCount++;
            }
            if (section.hasMaterialLookupReady()) {
                materialLookupSectionCount++;
            }
            if (section.hasMaterialMaskPayload()) {
                materialMaskReadySectionCount++;
            } else if (section.hasOccupiedVoxels()) {
                missingMaterialMaskSectionCount++;
            }
            if (section.hasMaterialLookupReady() && section.hasSurfaceSamples()) {
                surfaceSampleMaterialHitCount += section.surfaceSamples().size();
            }
            emissiveSourceCount += Math.max(section.emissiveVoxelCount(), section.hasEmissivePayload() ? 1 : 0);
            sourceCoupledBounceCandidateCount += section.sourceCoupledBounceCandidateCount();
            if (section.hasConcreteOccupancyMaskPayload()) {
                concreteMaskReadySectionCount++;
            } else if (section.hasOccupiedVoxels()) {
                missingConcreteMaskSectionCount++;
            }
            if (section.occupancyMaskMetadataOnly()) {
                metadataOnlyMaskSectionCount++;
            }
            if (section.hasOccupancyMask()) {
                maskSources.add(section.occupancyMaskSourceLabel());
            }
            if (!section.hasOccupiedVoxels() && !section.emptySectionSkipSafe()) {
                emptySectionSkipSafe = false;
            }
        }

        long materialHitCount = glassWaterMaterialHitCount + opaqueMaterialHitCount;
        if (materialHitCount == 0L) {
            materialHitCount = surfaceSampleMaterialHitCount;
        }
        boolean maskBitsReady = occupiedSectionCount > 0L && concreteMaskReadySectionCount == occupiedSectionCount;
        boolean materialMasksReady = occupiedSectionCount > 0L && materialMaskReadySectionCount == occupiedSectionCount;
        long sourceCoupledBounceCount = !metadataOnlyBackend && maskBitsReady && materialMasksReady
                ? sourceCoupledBounceCandidateCount
                : 0L;
        long hitCount = Math.max(Math.max(wallHitCount, materialHitCount), sourceCoupledBounceCount);
        long missCount = Math.max(openSkyMissCount, skippedSectionCount);
        long rayCount = hitCount + missCount;
        long stepCount = rayCount == 0L ? 0L : Math.max(rayCount, occupiedVoxelCount + skippedSectionCount);
        double averageStepsPerRay = rayCount == 0L ? 0.0D : stepCount / (double) rayCount;
        String materialHitSource = materialHitSource(materialSources, materialPaletteOnlySectionCount);
        String sourceCoupledBounceSource = sourceCoupledBounceSource(
                sourceCoupledBounceCount,
                emissiveSourceCount,
                materialHitCount,
                wallHitCount,
                maskBitsReady,
                materialMasksReady,
                metadataOnlyBackend
        );
        String maskBitsSource = maskBitsSource(
                maskBitsReady,
                concreteMaskReadySectionCount,
                metadataOnlyMaskSectionCount,
                missingConcreteMaskSectionCount,
                maskSources
        );
        String materialIdConsistency = materialIdConsistency(
                materialHitCount,
                materialLookupSectionCount,
                materialMaskReadySectionCount,
                occupiedSectionCount
        );
        String boundary = boundaryFor(
                sourceCoupledBounceCount,
                metadataOnlyBackend,
                occupiedSectionCount,
                concreteMaskReadySectionCount,
                materialMaskReadySectionCount,
                missingMaterialMaskSectionCount,
                materialHitCount,
                wallHitCount,
                completeBoundary
        );

        return new Round10VoxelTraversalOutputSummary(
                resolvedGeneration,
                rayCount,
                hitCount,
                missCount,
                wallHitCount,
                openSkyMissCount,
                stepCount,
                averageStepsPerRay,
                skippedSectionCount,
                materialHitCount,
                sourceCoupledBounceCount,
                sourceCoupledBounceSource,
                glassWaterMaterialHitCount,
                opaqueMaterialHitCount,
                materialHitSource,
                materialIdConsistency,
                emptySectionSkipSafe,
                maskBitsReady,
                maskBitsSource,
                traversalBackend,
                false,
                boundary
        );
    }

    private static String materialHitSource(Set<String> materialSources, long materialPaletteOnlySectionCount) {
        if (!materialSources.isEmpty()) {
            return String.join("+", materialSources);
        }
        if (materialPaletteOnlySectionCount > 0L) {
            return "section_snapshot_material_palette_no_hit_flags";
        }
        return "not_run";
    }

    private static String sourceCoupledBounceSource(
            long sourceCoupledBounceCount,
            long emissiveSourceCount,
            long materialHitCount,
            long wallHitCount,
            boolean maskBitsReady,
            boolean materialMasksReady,
            boolean metadataOnlyBackend
    ) {
        if (sourceCoupledBounceCount > 0L) {
            return "section_snapshot_emissive_payload_to_material_depth_receivers_with_concrete_masks";
        }
        if (emissiveSourceCount == 0L) {
            return "no_emissive_source_payload";
        }
        if (metadataOnlyBackend) {
            return "metadata_only_backend_no_source_bounce_consumption";
        }
        if (!maskBitsReady) {
            return "source_bounce_blocked_by_incomplete_occupancy_masks";
        }
        if (!materialMasksReady) {
            return "source_bounce_blocked_by_incomplete_material_masks";
        }
        if (emissiveSourceCount > 0L && (materialHitCount == 0L || wallHitCount == 0L)) {
            return "emissive_source_payload_without_material_depth_receiver_coupling";
        }
        if (emissiveSourceCount > 0L) {
            return "emissive_source_payload_not_bounced";
        }
        return "no_source_bounce_evidence";
    }

    private static String maskBitsSource(
            boolean maskBitsReady,
            long concreteMaskReadySectionCount,
            long metadataOnlyMaskSectionCount,
            long missingConcreteMaskSectionCount,
            Set<String> maskSources
    ) {
        if (maskBitsReady && !maskSources.isEmpty()) {
            return String.join("+", maskSources);
        }
        if (concreteMaskReadySectionCount > 0L) {
            return "partial_section_snapshot_concrete_occupancy_masks";
        }
        if (metadataOnlyMaskSectionCount > 0L) {
            return "metadata_only_section_snapshot_occupancy_masks";
        }
        if (missingConcreteMaskSectionCount > 0L) {
            return "missing_section_snapshot_occupancy_masks";
        }
        if (!maskSources.isEmpty()) {
            return "section_snapshot_occupancy_masks_not_ready";
        }
        return "not_uploaded";
    }

    private static String materialIdConsistency(
            long materialHitCount,
            long materialLookupSectionCount,
            long materialMaskReadySectionCount,
            long occupiedSectionCount
    ) {
        if (materialHitCount > 0L && materialMaskReadySectionCount > 0L) {
            return materialMaskReadySectionCount == occupiedSectionCount
                    ? "consistent_scene_material_masks"
                    : "partial_scene_material_masks";
        }
        if (materialLookupSectionCount > 0L) {
            return "material_palette_present_no_ready_hit_masks";
        }
        return "not_checked";
    }

    private static String boundaryFor(
            long sourceCoupledBounceCount,
            boolean metadataOnlyBackend,
            long occupiedSectionCount,
            long concreteMaskReadySectionCount,
            long materialMaskReadySectionCount,
            long missingMaterialMaskSectionCount,
            long materialHitCount,
            long wallHitCount,
            String completeBoundary
    ) {
        if (sourceCoupledBounceCount > 0L && materialHitCount > 0L && wallHitCount > 0L) {
            return completeBoundary;
        }
        if (metadataOnlyBackend) {
            return "section_snapshot_metadata_only_traversal_evidence_incomplete_for_gi_consumption";
        }
        if (occupiedSectionCount == 0L) {
            return "section_snapshot_no_occupied_scene_sections_for_traced_lighting_consumption";
        }
        if (concreteMaskReadySectionCount < occupiedSectionCount) {
            return "section_snapshot_concrete_occupancy_mask_incomplete_for_scene_traversal";
        }
        if (materialMaskReadySectionCount < occupiedSectionCount || missingMaterialMaskSectionCount > 0L) {
            return "section_snapshot_material_mask_incomplete_for_scene_traversal";
        }
        if (wallHitCount == 0L) {
            return "section_snapshot_depth_receiver_evidence_missing_for_traced_lighting_consumption";
        }
        if (materialHitCount == 0L) {
            return "section_snapshot_material_hit_evidence_missing_for_traced_lighting_consumption";
        }
        return "section_snapshot_source_bounce_evidence_missing_for_traced_lighting_consumption";
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
