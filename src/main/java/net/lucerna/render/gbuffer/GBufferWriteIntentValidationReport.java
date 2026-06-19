package net.lucerna.render.gbuffer;

import java.util.List;
import java.util.Objects;

public record GBufferWriteIntentValidationReport(List<GBufferWriteIntentValidationFinding> findings) {
    public GBufferWriteIntentValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (GBufferWriteIntentValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static GBufferWriteIntentValidationReport empty() {
        return new GBufferWriteIntentValidationReport(List.of());
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(GBufferWriteIntentValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<GBufferWriteIntentValidationFinding> errors() {
        return this.findings.stream()
                .filter(GBufferWriteIntentValidationFinding::error)
                .toList();
    }

    public List<GBufferWriteIntentValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == GBufferWriteIntentValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
