package net.lucerna.render.lighting.direct;

import java.util.List;
import java.util.Objects;

public record DirectLightValidationReport(List<DirectLightValidationFinding> findings) {
    public DirectLightValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (DirectLightValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static DirectLightValidationReport empty() {
        return new DirectLightValidationReport(List.of());
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(DirectLightValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<DirectLightValidationFinding> errors() {
        return this.findings.stream()
                .filter(DirectLightValidationFinding::error)
                .toList();
    }

    public List<DirectLightValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == DirectLightValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
