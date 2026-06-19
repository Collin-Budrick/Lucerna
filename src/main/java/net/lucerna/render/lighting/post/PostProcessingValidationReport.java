package net.lucerna.render.lighting.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PostProcessingValidationReport(List<PostProcessingValidationFinding> findings) {
    public PostProcessingValidationReport {
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (PostProcessingValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
    }

    public static PostProcessingValidationReport empty() {
        return new PostProcessingValidationReport(List.of());
    }

    public static PostProcessingValidationReport merge(PostProcessingValidationReport... reports) {
        Objects.requireNonNull(reports, "reports");
        List<PostProcessingValidationFinding> findings = new ArrayList<>();
        for (PostProcessingValidationReport report : reports) {
            if (report != null) {
                findings.addAll(report.findings());
            }
        }
        return new PostProcessingValidationReport(findings);
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(PostProcessingValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<PostProcessingValidationFinding> errors() {
        return this.findings.stream()
                .filter(PostProcessingValidationFinding::error)
                .toList();
    }

    public List<PostProcessingValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == PostProcessingValidationSeverity.WARNING)
                .toList();
    }

    public List<PostProcessingValidationFinding> infos() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == PostProcessingValidationSeverity.INFO)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }
}
