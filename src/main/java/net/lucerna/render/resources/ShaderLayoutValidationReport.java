package net.lucerna.render.resources;

import java.util.List;
import java.util.Objects;

public record ShaderLayoutValidationReport(List<ShaderLayoutValidationFinding> findings) {
    public ShaderLayoutValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (ShaderLayoutValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static ShaderLayoutValidationReport empty() {
        return new ShaderLayoutValidationReport(List.of());
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(ShaderLayoutValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<ShaderLayoutValidationFinding> errors() {
        return this.findings.stream()
                .filter(ShaderLayoutValidationFinding::error)
                .toList();
    }

    public List<ShaderLayoutValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == ShaderLayoutValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
