package net.lucerna.render.lighting.gi;

import java.util.List;
import java.util.Objects;

public record DiffuseGiValidationReport(List<DiffuseGiValidationFinding> findings) {
    public DiffuseGiValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (DiffuseGiValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static DiffuseGiValidationReport empty() {
        return new DiffuseGiValidationReport(List.of());
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(DiffuseGiValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<DiffuseGiValidationFinding> errors() {
        return this.findings.stream()
                .filter(DiffuseGiValidationFinding::error)
                .toList();
    }

    public List<DiffuseGiValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == DiffuseGiValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
