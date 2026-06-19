package net.lucerna.render.voxel;

import java.util.List;
import java.util.Objects;

public record VoxelTraversalValidationReport(List<VoxelTraversalValidationFinding> findings) {
    public VoxelTraversalValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (VoxelTraversalValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static VoxelTraversalValidationReport empty() {
        return new VoxelTraversalValidationReport(List.of());
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(VoxelTraversalValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<VoxelTraversalValidationFinding> errors() {
        return this.findings.stream()
                .filter(VoxelTraversalValidationFinding::error)
                .toList();
    }

    public List<VoxelTraversalValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == VoxelTraversalValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
