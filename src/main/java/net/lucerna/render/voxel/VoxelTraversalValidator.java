package net.lucerna.render.voxel;

import net.lucerna.render.gbuffer.GBufferWriteIntent;
import net.lucerna.render.gbuffer.GBufferWriteIntentValidationFinding;
import net.lucerna.render.gbuffer.GBufferWriteIntentValidationReport;
import net.lucerna.render.gbuffer.GBufferWriteIntentValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VoxelTraversalValidator {
    private VoxelTraversalValidator() {
    }

    public static VoxelTraversalValidationReport validateRequest(VoxelTraversalRequest request) {
        Objects.requireNonNull(request, "request");
        List<VoxelTraversalValidationFinding> findings = new ArrayList<>();

        if (!request.ray().directionLooksNormalized()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "RAY_DIRECTION_NOT_NORMALIZED",
                    "$.ray",
                    "Voxel traversal rays should be normalized before DDA setup"
            ));
        }
        if (!request.hasSectionSnapshots()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "NO_SECTION_SNAPSHOTS",
                    "$.sectionSnapshots",
                    "Voxel traversal request has no section snapshot metadata"
            ));
        }
        if (request.requestGeneration() < request.maxSnapshotGeneration()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "STALE_REQUEST_GENERATION",
                    "$.requestGeneration",
                    "Traversal request generation is older than one or more section snapshots"
            ));
        }
        if (request.ddaConfig().maxStepsPerRay() > request.rayBudget().maxVisitedVoxelsPerRay()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "DDA_STEPS_EXCEED_RAY_BUDGET",
                    "$.ddaConfig.maxStepsPerRay",
                    "DDA step limit exceeds the configured per-ray voxel visit budget"
            ));
        }
        if (request.ddaConfig().maxSectionsPerRay() > request.rayBudget().maxVisitedSectionsPerRay()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "DDA_SECTIONS_EXCEED_RAY_BUDGET",
                    "$.ddaConfig.maxSectionsPerRay",
                    "DDA section limit exceeds the configured per-ray section visit budget"
            ));
        }

        for (int index = 0; index < request.sectionSnapshots().size(); index++) {
            VoxelSectionSnapshotReference sectionSnapshot = request.sectionSnapshots().get(index);
            String location = "$.sectionSnapshots[" + index + "]";
            if (request.requireOccupancyMasks() && sectionSnapshot.hasOccupiedVoxels() && !sectionSnapshot.hasOccupancyMask()) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "MISSING_OCCUPANCY_MASK",
                        location + ".occupancyMaskWordCount",
                        "Occupied section snapshot requires an occupancy mask for CPU-side DDA traversal"
                ));
            }
            if (request.requireMaterialPalette() && sectionSnapshot.hasOccupiedVoxels() && !sectionSnapshot.hasMaterialPalette()) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "MISSING_MATERIAL_PALETTE",
                        location + ".materialPaletteSize",
                        "Occupied section snapshot requires material palette metadata for G-buffer material id writes"
                ));
            }
        }

        return new VoxelTraversalValidationReport(findings);
    }

    public static VoxelTraversalValidationReport validateResult(
            VoxelTraversalRequest request,
            VoxelTraversalResult result
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");

        List<VoxelTraversalValidationFinding> findings = new ArrayList<>();
        if (result.requestGeneration() != request.requestGeneration()) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "REQUEST_GENERATION_MISMATCH",
                    "$.requestGeneration",
                    "Traversal result must refer to the same request generation"
            ));
        }
        if (result.sourceGeneration() < request.maxSnapshotGeneration() && result.completed()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "RESULT_SOURCE_GENERATION_STALE",
                    "$.sourceGeneration",
                    "Completed traversal result was produced from older section metadata than the request carried"
            ));
        }
        if (result.consumedRayCount() > request.rayBudget().maxRaysPerFrame()) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "RAY_BUDGET_EXCEEDED",
                    "$.consumedRayCount",
                    "Traversal result consumed more rays than allowed by the frame budget"
            ));
        }

        long maxVisitedVoxels = (long) result.consumedRayCount() * request.rayBudget().maxVisitedVoxelsPerRay();
        if (result.visitedVoxelCount() > maxVisitedVoxels) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "VOXEL_VISIT_BUDGET_EXCEEDED",
                    "$.visitedVoxelCount",
                    "Traversal result visited more voxels than allowed by the consumed ray count"
            ));
        }

        long maxVisitedSections = (long) result.consumedRayCount() * request.rayBudget().maxVisitedSectionsPerRay();
        if (result.visitedSectionCount() > maxVisitedSections) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "SECTION_VISIT_BUDGET_EXCEEDED",
                    "$.visitedSectionCount",
                    "Traversal result visited more sections than allowed by the consumed ray count"
            ));
        }

        return new VoxelTraversalValidationReport(findings);
    }

    public static VoxelTraversalValidationReport validateFirstPass(
            VoxelTraversalRequest request,
            GBufferWriteIntent writeIntent
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(writeIntent, "writeIntent");

        List<VoxelTraversalValidationFinding> findings = new ArrayList<>(validateRequest(request).findings());
        if (request.purpose() != VoxelTraversalPurpose.PRIMARY_GBUFFER) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "UNSUPPORTED_FIRST_PASS_PURPOSE",
                    "$.purpose",
                    "G-buffer first pass requires PRIMARY_GBUFFER voxel traversal"
            ));
        }

        GBufferWriteIntentValidationReport gBufferReport = GBufferWriteIntentValidator.validateLucernaMain(writeIntent);
        for (GBufferWriteIntentValidationFinding gBufferFinding : gBufferReport.findings()) {
            findings.add(convertGBufferFinding(gBufferFinding));
        }

        return new VoxelTraversalValidationReport(findings);
    }

    private static VoxelTraversalValidationFinding convertGBufferFinding(
            GBufferWriteIntentValidationFinding finding
    ) {
        return switch (finding.severity()) {
            case INFO -> VoxelTraversalValidationFinding.info(
                    "GBUFFER_" + finding.code(),
                    gBufferLocation(finding.location()),
                    finding.message()
            );
            case WARNING -> VoxelTraversalValidationFinding.warning(
                    "GBUFFER_" + finding.code(),
                    gBufferLocation(finding.location()),
                    finding.message()
            );
            case ERROR -> VoxelTraversalValidationFinding.error(
                    "GBUFFER_" + finding.code(),
                    gBufferLocation(finding.location()),
                    finding.message()
            );
        };
    }

    private static String gBufferLocation(String location) {
        if (location == null || location.isBlank() || "$".equals(location)) {
            return "$.gBuffer";
        }
        if (location.startsWith("$.")) {
            return "$.gBuffer." + location.substring(2);
        }
        if (location.startsWith("$")) {
            return "$.gBuffer" + location.substring(1);
        }
        return "$.gBuffer." + location;
    }
}
