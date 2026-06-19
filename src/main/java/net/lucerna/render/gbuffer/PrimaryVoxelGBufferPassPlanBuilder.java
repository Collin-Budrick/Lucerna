package net.lucerna.render.gbuffer;

import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.render.voxel.VoxelTraversalPurpose;
import net.lucerna.render.voxel.VoxelTraversalRequest;
import net.lucerna.render.voxel.VoxelTraversalValidationFinding;
import net.lucerna.render.voxel.VoxelTraversalValidationReport;
import net.lucerna.render.voxel.VoxelTraversalValidator;
import net.lucerna.upload.NativeSectionSnapshotUpload;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PrimaryVoxelGBufferPassPlanBuilder {
    private GBufferWriteIntent writeIntent;
    private VoxelTraversalRequest traversalRequest;
    private List<VoxelSectionSnapshotReference> sectionSnapshots;

    private PrimaryVoxelGBufferPassPlanBuilder() {
    }

    public static PrimaryVoxelGBufferPassPlanBuilder create() {
        return new PrimaryVoxelGBufferPassPlanBuilder();
    }

    public PrimaryVoxelGBufferPassPlanBuilder writeIntent(GBufferWriteIntent writeIntent) {
        this.writeIntent = Objects.requireNonNull(writeIntent, "writeIntent");
        return this;
    }

    public PrimaryVoxelGBufferPassPlanBuilder traversalRequest(VoxelTraversalRequest traversalRequest) {
        this.traversalRequest = Objects.requireNonNull(traversalRequest, "traversalRequest");
        return this;
    }

    public PrimaryVoxelGBufferPassPlanBuilder sectionSnapshots(List<VoxelSectionSnapshotReference> sectionSnapshots) {
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        this.sectionSnapshots = List.copyOf(sectionSnapshots);
        return this;
    }

    public PrimaryVoxelGBufferPassPlanBuilder sectionSnapshotsFromWorldSnapshots(
            List<ChunkSectionVoxelSnapshot> snapshots
    ) {
        return this.sectionSnapshots(VoxelSectionSnapshotReference.fromSnapshots(snapshots));
    }

    public PrimaryVoxelGBufferPassPlanBuilder sectionSnapshotsFromNativeUploads(
            List<NativeSectionSnapshotUpload> uploads
    ) {
        return this.sectionSnapshots(VoxelSectionSnapshotReference.fromUploads(uploads));
    }

    public PrimaryVoxelGBufferPassPlan build() {
        GBufferWriteIntent resolvedWriteIntent = required(this.writeIntent, "writeIntent");
        VoxelTraversalRequest resolvedTraversalRequest = required(this.traversalRequest, "traversalRequest");
        List<VoxelSectionSnapshotReference> resolvedSectionSnapshots = this.sectionSnapshots == null
                ? resolvedTraversalRequest.sectionSnapshots()
                : this.sectionSnapshots;
        PrimaryVoxelGBufferPassOutputMetadata outputMetadata = PrimaryVoxelGBufferPassOutputMetadata.from(
                resolvedWriteIntent,
                resolvedTraversalRequest,
                resolvedSectionSnapshots
        );
        VoxelTraversalValidationReport validationReport = validate(
                resolvedWriteIntent,
                resolvedTraversalRequest,
                resolvedSectionSnapshots
        );
        return new PrimaryVoxelGBufferPassPlan(
                resolvedWriteIntent,
                resolvedTraversalRequest,
                resolvedSectionSnapshots,
                outputMetadata,
                validationReport
        );
    }

    private static VoxelTraversalValidationReport validate(
            GBufferWriteIntent writeIntent,
            VoxelTraversalRequest traversalRequest,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        List<VoxelTraversalValidationFinding> findings = new ArrayList<>(
                VoxelTraversalValidator.validateFirstPass(traversalRequest, writeIntent).findings()
        );

        if (traversalRequest.purpose() == VoxelTraversalPurpose.PRIMARY_GBUFFER
                && writeIntent.requiresHistory()
                && !writeIntent.writesAttachment(GBufferTargetContract.MOTION_HISTORY)) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "FIRST_PASS_HISTORY_DECLARED_WITHOUT_ATTACHMENT",
                    "$.gBuffer.attachments",
                    "Primary G-buffer history was requested but the motion/history attachment is not declared"
            ));
        }

        validateSectionReferenceList(findings, sectionSnapshots);
        validateSectionAlignment(findings, traversalRequest.sectionSnapshots(), sectionSnapshots);

        long maxSectionGeneration = sectionSnapshots.stream()
                .mapToLong(VoxelSectionSnapshotReference::combinedGeneration)
                .max()
                .orElse(0L);
        if (traversalRequest.requestGeneration() < maxSectionGeneration) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "PLAN_REQUEST_GENERATION_STALE",
                    "$.requestGeneration",
                    "Primary G-buffer plan request generation is older than the supplied section references"
            ));
        }
        if (writeIntent.generation() < traversalRequest.requestGeneration()) {
            findings.add(VoxelTraversalValidationFinding.warning(
                    "GBUFFER_WRITE_GENERATION_STALE",
                    "$.gBuffer.generation",
                    "G-buffer write intent generation is older than the voxel traversal request generation"
            ));
        }

        return new VoxelTraversalValidationReport(findings);
    }

    private static void validateSectionReferenceList(
            List<VoxelTraversalValidationFinding> findings,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        Set<String> stableKeys = new HashSet<>();
        for (int index = 0; index < sectionSnapshots.size(); index++) {
            VoxelSectionSnapshotReference sectionSnapshot = sectionSnapshots.get(index);
            if (sectionSnapshot == null) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "NULL_PLAN_SECTION_REFERENCE",
                        "$.sectionSnapshots[" + index + "]",
                        "Primary G-buffer plan section references must not contain null entries"
                ));
                continue;
            }
            if (!stableKeys.add(sectionSnapshot.stableKey())) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "DUPLICATE_PLAN_SECTION_REFERENCE",
                        "$.sectionSnapshots[" + index + "]",
                        "Primary G-buffer plan section references must be unique by section origin"
                ));
            }
        }
    }

    private static void validateSectionAlignment(
            List<VoxelTraversalValidationFinding> findings,
            List<VoxelSectionSnapshotReference> requestSectionSnapshots,
            List<VoxelSectionSnapshotReference> planSectionSnapshots
    ) {
        Map<String, VoxelSectionSnapshotReference> requestByKey = referencesByKey(requestSectionSnapshots);
        Map<String, VoxelSectionSnapshotReference> planByKey = referencesByKey(planSectionSnapshots);

        if (requestByKey.size() != planByKey.size()) {
            findings.add(VoxelTraversalValidationFinding.error(
                    "PLAN_SECTION_REFERENCE_COUNT_MISMATCH",
                    "$.sectionSnapshots",
                    "Primary G-buffer plan section references must describe the same sections as the traversal request"
            ));
        }

        for (Map.Entry<String, VoxelSectionSnapshotReference> planEntry : planByKey.entrySet()) {
            VoxelSectionSnapshotReference requestReference = requestByKey.get(planEntry.getKey());
            if (requestReference == null) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "PLAN_SECTION_REFERENCE_NOT_IN_REQUEST",
                        "$.sectionSnapshots[" + planEntry.getKey() + "]",
                        "Primary G-buffer plan includes a section reference that is not present in the traversal request"
                ));
            } else if (!requestReference.equals(planEntry.getValue())) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "PLAN_SECTION_REFERENCE_METADATA_MISMATCH",
                        "$.sectionSnapshots[" + planEntry.getKey() + "]",
                        "Primary G-buffer plan section metadata must match the traversal request metadata"
                ));
            }
        }

        for (String requestKey : requestByKey.keySet()) {
            if (!planByKey.containsKey(requestKey)) {
                findings.add(VoxelTraversalValidationFinding.error(
                        "REQUEST_SECTION_REFERENCE_NOT_IN_PLAN",
                        "$.traversalRequest.sectionSnapshots[" + requestKey + "]",
                        "Traversal request includes a section reference that is missing from the primary G-buffer plan"
                ));
            }
        }
    }

    private static Map<String, VoxelSectionSnapshotReference> referencesByKey(
            List<VoxelSectionSnapshotReference> references
    ) {
        Map<String, VoxelSectionSnapshotReference> referencesByKey = new LinkedHashMap<>();
        for (VoxelSectionSnapshotReference reference : references) {
            if (reference != null) {
                referencesByKey.putIfAbsent(reference.stableKey(), reference);
            }
        }
        return referencesByKey;
    }

    private static <T> T required(T value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + " must be provided before building a primary voxel G-buffer pass plan");
        }
        return value;
    }
}
