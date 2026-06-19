package net.lucerna.render.lighting.direct;

import net.lucerna.render.voxel.VoxelSectionSnapshotReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DirectLightingPlanValidator {
    private DirectLightingPlanValidator() {
    }

    public static DirectLightValidationReport validate(DirectLightingPlan plan) {
        Objects.requireNonNull(plan, "plan");
        return validate(plan.frameIndex(), plan.celestialLighting(), plan.emissiveBlockList(), plan.shadowRayPlan());
    }

    public static DirectLightValidationReport validate(
            long frameIndex,
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList,
            DirectShadowRayPlan shadowRayPlan
    ) {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        Objects.requireNonNull(celestialLighting, "celestialLighting");
        Objects.requireNonNull(emissiveBlockList, "emissiveBlockList");
        Objects.requireNonNull(shadowRayPlan, "shadowRayPlan");

        List<DirectLightValidationFinding> findings = new ArrayList<>();
        findings.addAll(validateCelestialLighting(celestialLighting).findings());
        findings.addAll(validateEmissiveBlockList(emissiveBlockList).findings());
        findings.addAll(validateShadowRayPlan(shadowRayPlan).findings());

        if (frameIndex == 0L) {
            findings.add(DirectLightValidationFinding.warning(
                    "DIRECT_LIGHTING_FRAME_INDEX_UNAVAILABLE",
                    "$.frameIndex",
                    "Direct lighting plan has no captured frame index"
            ));
        }
        if (celestialLighting.frameIndex() != frameIndex) {
            findings.add(DirectLightValidationFinding.error(
                    "CELESTIAL_FRAME_INDEX_MISMATCH",
                    "$.celestial.frameIndex",
                    "Celestial direct lighting plan must match the aggregate frame index"
            ));
        }
        if (shadowRayPlan.frameIndex() != frameIndex) {
            findings.add(DirectLightValidationFinding.error(
                    "SHADOW_FRAME_INDEX_MISMATCH",
                    "$.shadowRays.frameIndex",
                    "Shadow ray plan must match the aggregate frame index"
            ));
        }
        if (emissiveBlockList.hasSelectedLights() && emissiveBlockList.generation() < emissiveBlockList.maxLightGeneration()) {
            findings.add(DirectLightValidationFinding.warning(
                    "EMISSIVE_LIST_GENERATION_STALE",
                    "$.emissiveBlocks.generation",
                    "Emissive block list generation is older than one or more selected lights"
            ));
        }
        if (requiresShadowCandidates(celestialLighting, emissiveBlockList) && !shadowRayPlan.hasRayCandidates()) {
            findings.add(DirectLightValidationFinding.warning(
                    "MISSING_DIRECT_SHADOW_RAYS",
                    "$.shadowRays.rayCandidates",
                    "Active direct lights should provide shadow ray candidates before native lighting execution"
            ));
        }
        if (!celestialLighting.hasActiveLight() && !emissiveBlockList.hasSelectedLights()) {
            findings.add(DirectLightValidationFinding.info(
                    "NO_DIRECT_LIGHT_WORK",
                    "$",
                    "Direct lighting plan has no active celestial or emissive light work"
            ));
        }

        validateEmissiveDimensions(findings, celestialLighting, emissiveBlockList);
        return new DirectLightValidationReport(findings);
    }

    public static DirectLightValidationReport validateCelestialLighting(DirectCelestialLightingPlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<DirectLightValidationFinding> findings = new ArrayList<>();

        if (plan.frameIndex() == 0L) {
            findings.add(DirectLightValidationFinding.warning(
                    "CELESTIAL_FRAME_INDEX_UNAVAILABLE",
                    "$.celestial.frameIndex",
                    "Celestial lighting plan has no captured frame index"
            ));
        }
        if (!plan.hasDimension()) {
            findings.add(DirectLightValidationFinding.warning(
                    "CELESTIAL_DIMENSION_UNAVAILABLE",
                    "$.celestial.dimensionId",
                    "Celestial lighting plan has no dimension id"
            ));
        }
        if (!plan.worldTimeAvailable()) {
            findings.add(DirectLightValidationFinding.warning(
                    "WORLD_TIME_UNAVAILABLE",
                    "$.celestial.worldTimeAvailable",
                    "Sun and moon lighting should be planned from captured world time"
            ));
        }

        validateCelestialLight(findings, plan.sun(), DirectCelestialLightSource.SUN, "$.celestial.sun");
        validateCelestialLight(findings, plan.moon(), DirectCelestialLightSource.MOON, "$.celestial.moon");
        return new DirectLightValidationReport(findings);
    }

    public static DirectLightValidationReport validateEmissiveBlockList(DirectEmissiveBlockListPlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<DirectLightValidationFinding> findings = new ArrayList<>();

        if (plan.hasCandidates() && plan.maxSelectedLights() == 0) {
            findings.add(DirectLightValidationFinding.warning(
                    "EMISSIVE_SELECTION_DISABLED",
                    "$.emissiveBlocks.maxSelectedLights",
                    "Emissive block candidates exist but the selected light limit is zero"
            ));
        }
        if (plan.hasCandidates() && !plan.sortedByPriority() && plan.selectedLightCount() < plan.candidateCount()) {
            findings.add(DirectLightValidationFinding.warning(
                    "EMISSIVE_LIST_NOT_PRIORITY_SORTED",
                    "$.emissiveBlocks.sortedByPriority",
                    "Truncated emissive block lists should be sorted by direct lighting priority"
            ));
        }
        if (!plan.hasCandidates()) {
            findings.add(DirectLightValidationFinding.info(
                    "NO_EMISSIVE_BLOCK_CANDIDATES",
                    "$.emissiveBlocks.lights",
                    "No emissive block candidates were supplied for direct lighting"
            ));
        }

        Set<String> stableKeys = new HashSet<>();
        for (int index = 0; index < plan.lights().size(); index++) {
            DirectEmissiveBlockLight light = plan.lights().get(index);
            String location = "$.emissiveBlocks.lights[" + index + "]";
            if (!stableKeys.add(light.stableKey())) {
                findings.add(DirectLightValidationFinding.error(
                        "DUPLICATE_EMISSIVE_BLOCK_LIGHT",
                        location,
                        "Emissive block list entries must be unique by dimension, block position, and material id"
                ));
            }
            if (!light.hasEnergy()) {
                findings.add(DirectLightValidationFinding.warning(
                        "EMISSIVE_BLOCK_WITHOUT_ENERGY",
                        location + ".intensity",
                        "Emissive block candidate has no direct lighting energy"
                ));
            }
            if (light.blockLightLevel() == 0) {
                findings.add(DirectLightValidationFinding.warning(
                        "EMISSIVE_BLOCK_LIGHT_LEVEL_ZERO",
                        location + ".blockLightLevel",
                        "Emissive block candidate has a zero Minecraft block light level"
                ));
            }
        }

        return new DirectLightValidationReport(findings);
    }

    public static DirectLightValidationReport validateShadowRayPlan(DirectShadowRayPlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<DirectLightValidationFinding> findings = new ArrayList<>();

        if (plan.frameIndex() == 0L) {
            findings.add(DirectLightValidationFinding.warning(
                    "SHADOW_FRAME_INDEX_UNAVAILABLE",
                    "$.shadowRays.frameIndex",
                    "Shadow ray plan has no captured frame index"
            ));
        }
        if (!plan.hasRayCandidates()) {
            findings.add(DirectLightValidationFinding.info(
                    "NO_SHADOW_RAY_CANDIDATES",
                    "$.shadowRays.rayCandidates",
                    "No direct shadow ray candidates were supplied"
            ));
        }
        if (plan.hasRayCandidates() && !plan.hasSectionSnapshots()) {
            findings.add(DirectLightValidationFinding.warning(
                    "NO_SHADOW_SECTION_SNAPSHOTS",
                    "$.shadowRays.sectionSnapshots",
                    "Shadow ray candidates need section snapshot metadata for voxel occlusion"
            ));
        }
        if (plan.candidateCount() > plan.rayBudget().maxRaysPerFrame()) {
            findings.add(DirectLightValidationFinding.error(
                    "SHADOW_RAY_BUDGET_EXCEEDED",
                    "$.shadowRays.rayCandidates",
                    "Shadow ray plan contains more candidates than the configured frame ray budget"
            ));
        }
        if (plan.rayBudget().shadowRaysPerHit() == 0 && plan.hasRayCandidates()) {
            findings.add(DirectLightValidationFinding.warning(
                    "SHADOW_RAYS_PER_HIT_ZERO",
                    "$.shadowRays.rayBudget.shadowRaysPerHit",
                    "Shadow ray candidates exist but the ray budget has no shadow rays per hit"
            ));
        }
        if (plan.generation() < plan.maxCandidateGeneration()) {
            findings.add(DirectLightValidationFinding.warning(
                    "SHADOW_PLAN_CANDIDATE_GENERATION_STALE",
                    "$.shadowRays.generation",
                    "Shadow ray plan generation is older than one or more ray candidate sources"
            ));
        }
        if (plan.generation() < plan.maxSectionGeneration()) {
            findings.add(DirectLightValidationFinding.warning(
                    "SHADOW_PLAN_SECTION_GENERATION_STALE",
                    "$.shadowRays.generation",
                    "Shadow ray plan generation is older than one or more section snapshots"
            ));
        }

        validateShadowRayCandidates(findings, plan);
        validateShadowSectionSnapshots(findings, plan);
        return new DirectLightValidationReport(findings);
    }

    private static void validateCelestialLight(
            List<DirectLightValidationFinding> findings,
            DirectCelestialLight light,
            DirectCelestialLightSource expectedSource,
            String location
    ) {
        if (light.source() != expectedSource) {
            findings.add(DirectLightValidationFinding.error(
                    "CELESTIAL_SOURCE_MISMATCH",
                    location + ".source",
                    "Celestial light source does not match its plan slot"
            ));
        }
        if (!light.direction().looksNormalized()) {
            findings.add(DirectLightValidationFinding.warning(
                    "CELESTIAL_DIRECTION_NOT_NORMALIZED",
                    location + ".direction",
                    "Celestial light directions should be normalized before shadow ray planning"
            ));
        }
        if (light.castsEffectiveShadows() && light.angularRadiusRadians() == 0.0F) {
            findings.add(DirectLightValidationFinding.warning(
                    "CELESTIAL_SHADOW_WITHOUT_ANGULAR_RADIUS",
                    location + ".angularRadiusRadians",
                    "Shadow-casting celestial lights should carry a non-zero angular radius"
            ));
        }
        if (!light.enabled() && light.castsShadows()) {
            findings.add(DirectLightValidationFinding.info(
                    "DISABLED_CELESTIAL_SHADOW_CASTER",
                    location + ".castsShadows",
                    "Disabled celestial lights do not contribute shadow rays"
            ));
        }
    }

    private static void validateShadowRayCandidates(
            List<DirectLightValidationFinding> findings,
            DirectShadowRayPlan plan
    ) {
        Set<String> sampleKeys = new HashSet<>();
        for (int index = 0; index < plan.rayCandidates().size(); index++) {
            DirectShadowRayCandidate candidate = plan.rayCandidates().get(index);
            String location = "$.shadowRays.rayCandidates[" + index + "]";
            String sampleKey = candidate.source() + ":" + candidate.sourceKey() + ":" + candidate.sampleIndex();
            if (!sampleKeys.add(sampleKey)) {
                findings.add(DirectLightValidationFinding.warning(
                        "DUPLICATE_SHADOW_RAY_SAMPLE",
                        location,
                        "Shadow ray candidates repeat the same source and sample index"
                ));
            }
            if (!candidate.contributesLighting()) {
                findings.add(DirectLightValidationFinding.warning(
                        "SHADOW_RAY_WITHOUT_CONTRIBUTION",
                        location + ".contributionWeight",
                        "Shadow ray candidate has no direct lighting contribution weight"
                ));
            }
            if (!candidate.ray().directionLooksNormalized()) {
                findings.add(DirectLightValidationFinding.warning(
                        "SHADOW_RAY_DIRECTION_NOT_NORMALIZED",
                        location + ".ray",
                        "Shadow ray directions should be normalized before DDA traversal"
                ));
            }
            if (candidate.sourceGeneration() > plan.generation()) {
                findings.add(DirectLightValidationFinding.warning(
                        "SHADOW_RAY_SOURCE_NEWER_THAN_PLAN",
                        location + ".sourceGeneration",
                        "Shadow ray candidate source generation is newer than the shadow ray plan"
                ));
            }
        }
    }

    private static void validateShadowSectionSnapshots(
            List<DirectLightValidationFinding> findings,
            DirectShadowRayPlan plan
    ) {
        Set<String> stableKeys = new HashSet<>();
        for (int index = 0; index < plan.sectionSnapshots().size(); index++) {
            VoxelSectionSnapshotReference sectionSnapshot = plan.sectionSnapshots().get(index);
            String location = "$.shadowRays.sectionSnapshots[" + index + "]";
            if (!stableKeys.add(sectionSnapshot.stableKey())) {
                findings.add(DirectLightValidationFinding.error(
                        "DUPLICATE_SHADOW_SECTION_REFERENCE",
                        location,
                        "Shadow ray section snapshots must be unique by section origin"
                ));
            }
            if (plan.requireOccupancyMasks()
                    && sectionSnapshot.hasOccupiedVoxels()
                    && !sectionSnapshot.hasOccupancyMask()) {
                findings.add(DirectLightValidationFinding.error(
                        "SHADOW_SECTION_MISSING_OCCUPANCY_MASK",
                        location + ".occupancyMaskWordCount",
                        "Occupied section snapshots require occupancy masks for direct shadow traversal"
                ));
            }
        }
    }

    private static boolean requiresShadowCandidates(
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList
    ) {
        return celestialLighting.hasActiveShadowCaster() || emissiveBlockList.hasSelectedLights();
    }

    private static void validateEmissiveDimensions(
            List<DirectLightValidationFinding> findings,
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList
    ) {
        if (!celestialLighting.hasDimension()) {
            return;
        }
        List<DirectEmissiveBlockLight> selectedLights = emissiveBlockList.selectedLights();
        for (int index = 0; index < selectedLights.size(); index++) {
            DirectEmissiveBlockLight light = selectedLights.get(index);
            if (!celestialLighting.dimensionId().equals(light.dimension())) {
                findings.add(DirectLightValidationFinding.warning(
                        "EMISSIVE_LIGHT_DIMENSION_MISMATCH",
                        "$.emissiveBlocks.selectedLights[" + index + "].dimension",
                        "Selected emissive light dimension differs from the celestial lighting dimension"
                ));
            }
        }
    }
}
