package net.lucerna.render.lighting.restir.direct;

import java.util.List;
import java.util.Objects;

public record DirectRestirValidationStatus(
        boolean executionAvailable,
        boolean metadataOnly,
        String boundaryLabel,
        List<DirectRestirValidationFinding> findings
) {
    public DirectRestirValidationStatus {
        boundaryLabel = requireText(boundaryLabel, "boundaryLabel");
        Objects.requireNonNull(findings, "findings");
        findings = List.copyOf(findings);
        for (DirectRestirValidationFinding finding : findings) {
            Objects.requireNonNull(finding, "findings must not contain null entries");
        }
        if (executionAvailable && metadataOnly) {
            throw new IllegalArgumentException("executionAvailable and metadataOnly cannot both be true");
        }
    }

    public static DirectRestirValidationStatus metadataScaffold() {
        return new DirectRestirValidationStatus(
                false,
                true,
                "direct ReSTIR metadata scaffold only; no real reservoir execution",
                List.of(DirectRestirValidationFinding.info(
                        "DIRECT_RESTIR_METADATA_ONLY",
                        "$",
                        "Direct-light ReSTIR contracts are present, execution is not wired"
                ))
        );
    }

    public boolean valid() {
        return this.findings.stream().noneMatch(DirectRestirValidationFinding::error);
    }

    public boolean hasFindings() {
        return !this.findings.isEmpty();
    }

    public List<DirectRestirValidationFinding> errors() {
        return this.findings.stream()
                .filter(DirectRestirValidationFinding::error)
                .toList();
    }

    public List<DirectRestirValidationFinding> warnings() {
        return this.findings.stream()
                .filter(finding -> finding.severity() == DirectRestirValidationSeverity.WARNING)
                .toList();
    }

    public int errorCount() {
        return this.errors().size();
    }

    public int warningCount() {
        return this.warnings().size();
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
