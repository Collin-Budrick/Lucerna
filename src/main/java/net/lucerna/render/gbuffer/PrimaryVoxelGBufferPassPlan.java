package net.lucerna.render.gbuffer;

import net.lucerna.render.voxel.VoxelSectionSnapshotReference;
import net.lucerna.render.voxel.VoxelTraversalRequest;
import net.lucerna.render.voxel.VoxelTraversalValidationFinding;
import net.lucerna.render.voxel.VoxelTraversalValidationReport;

import java.util.List;
import java.util.Objects;

public record PrimaryVoxelGBufferPassPlan(
        GBufferWriteIntent writeIntent,
        VoxelTraversalRequest traversalRequest,
        List<VoxelSectionSnapshotReference> sectionSnapshots,
        PrimaryVoxelGBufferPassOutputMetadata outputMetadata,
        VoxelTraversalValidationReport validationReport
) {
    public PrimaryVoxelGBufferPassPlan {
        Objects.requireNonNull(writeIntent, "writeIntent");
        Objects.requireNonNull(traversalRequest, "traversalRequest");
        Objects.requireNonNull(sectionSnapshots, "sectionSnapshots");
        sectionSnapshots = List.copyOf(sectionSnapshots);
        for (VoxelSectionSnapshotReference sectionSnapshot : sectionSnapshots) {
            Objects.requireNonNull(sectionSnapshot, "sectionSnapshots must not contain null entries");
        }
        Objects.requireNonNull(outputMetadata, "outputMetadata");
        Objects.requireNonNull(validationReport, "validationReport");
        if (outputMetadata.requestGeneration() != traversalRequest.requestGeneration()) {
            throw new IllegalArgumentException("outputMetadata requestGeneration must match traversalRequest");
        }
        if (outputMetadata.sectionCount() != sectionSnapshots.size()) {
            throw new IllegalArgumentException("outputMetadata sectionCount must match sectionSnapshots");
        }
    }

    public static PrimaryVoxelGBufferPassPlanBuilder builder() {
        return PrimaryVoxelGBufferPassPlanBuilder.create();
    }

    public static PrimaryVoxelGBufferPassPlan from(
            GBufferWriteIntent writeIntent,
            VoxelTraversalRequest traversalRequest
    ) {
        return builder()
                .writeIntent(writeIntent)
                .traversalRequest(traversalRequest)
                .build();
    }

    public static PrimaryVoxelGBufferPassPlan from(
            GBufferWriteIntent writeIntent,
            VoxelTraversalRequest traversalRequest,
            List<VoxelSectionSnapshotReference> sectionSnapshots
    ) {
        return builder()
                .writeIntent(writeIntent)
                .traversalRequest(traversalRequest)
                .sectionSnapshots(sectionSnapshots)
                .build();
    }

    public boolean valid() {
        return this.validationReport.valid();
    }

    public boolean hasFindings() {
        return this.validationReport.hasFindings();
    }

    public List<VoxelTraversalValidationFinding> findings() {
        return this.validationReport.findings();
    }

    public boolean readyForCpuPlanning() {
        return this.valid()
                && this.outputMetadata.hasExpectedAttachmentWrites()
                && this.traversalRequest.hasSectionSnapshots();
    }
}
