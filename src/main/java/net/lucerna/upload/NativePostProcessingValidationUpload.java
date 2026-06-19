package net.lucerna.upload;

import net.lucerna.render.lighting.post.PostProcessingPipelinePlan;
import net.lucerna.render.lighting.post.PostProcessingValidationFinding;
import net.lucerna.render.lighting.post.PostProcessingValidationReport;
import net.lucerna.render.lighting.post.PostProcessingValidationSeverity;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class NativePostProcessingValidationUpload {
    public static final int SEVERITY_INFO = 0;
    public static final int SEVERITY_WARNING = 1;
    public static final int SEVERITY_ERROR = 2;

    public static final int FLAG_VALID = 1;
    public static final int FLAG_READY_FOR_NATIVE_HANDOFF = 1 << 1;
    public static final int FLAG_DENOISE_SCHEDULED = 1 << 2;
    public static final int FLAG_HAS_INFOS = 1 << 3;
    public static final int FLAG_HAS_WARNINGS = 1 << 4;
    public static final int FLAG_HAS_ERRORS = 1 << 5;

    private final boolean valid;
    private final boolean readyForNativeHandoff;
    private final boolean denoiseScheduled;
    private final int findingCount;
    private final int errorCount;
    private final int warningCount;
    private final int infoCount;
    private final int[] severityIds;
    private final String[] severities;
    private final String[] codes;
    private final String[] locations;
    private final String[] messages;
    private final int flags;

    private NativePostProcessingValidationUpload(
            boolean valid,
            boolean readyForNativeHandoff,
            boolean denoiseScheduled,
            int findingCount,
            int errorCount,
            int warningCount,
            int infoCount,
            int[] severityIds,
            String[] severities,
            String[] codes,
            String[] locations,
            String[] messages,
            int flags
    ) {
        this.valid = valid;
        this.readyForNativeHandoff = readyForNativeHandoff;
        this.denoiseScheduled = denoiseScheduled;
        this.findingCount = findingCount;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
        this.infoCount = infoCount;
        this.severityIds = copy(severityIds, "severityIds");
        this.severities = copy(severities, "severities");
        this.codes = copy(codes, "codes");
        this.locations = copy(locations, "locations");
        this.messages = copy(messages, "messages");
        this.flags = flags;

        this.validate();
    }

    public static NativePostProcessingValidationUpload from(PostProcessingPipelinePlan plan) {
        Objects.requireNonNull(plan, "plan");
        PostProcessingValidationReport report = plan.validationReport();
        List<PostProcessingValidationFinding> findings = report.findings();
        int[] severityIds = new int[findings.size()];
        String[] severities = new String[findings.size()];
        String[] codes = new String[findings.size()];
        String[] locations = new String[findings.size()];
        String[] messages = new String[findings.size()];

        int errorCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        for (int index = 0; index < findings.size(); index++) {
            PostProcessingValidationFinding finding = findings.get(index);
            severityIds[index] = severityId(finding.severity());
            severities[index] = finding.severity().name();
            codes[index] = finding.code();
            locations[index] = finding.location();
            messages[index] = finding.message();
            switch (finding.severity()) {
                case ERROR -> errorCount++;
                case WARNING -> warningCount++;
                case INFO -> infoCount++;
            }
        }

        return new NativePostProcessingValidationUpload(
                report.valid(),
                plan.readyForNativeHandoff(),
                plan.denoiseScheduled(),
                findings.size(),
                errorCount,
                warningCount,
                infoCount,
                severityIds,
                severities,
                codes,
                locations,
                messages,
                flags(report.valid(), plan.readyForNativeHandoff(), plan.denoiseScheduled(), infoCount, warningCount, errorCount)
        );
    }

    public boolean valid() {
        return this.valid;
    }

    public boolean readyForNativeHandoff() {
        return this.readyForNativeHandoff;
    }

    public boolean denoiseScheduled() {
        return this.denoiseScheduled;
    }

    public int findingCount() {
        return this.findingCount;
    }

    public int errorCount() {
        return this.errorCount;
    }

    public int warningCount() {
        return this.warningCount;
    }

    public int infoCount() {
        return this.infoCount;
    }

    public int[] severityIds() {
        return copy(this.severityIds, "severityIds");
    }

    public String[] severities() {
        return copy(this.severities, "severities");
    }

    public String[] codes() {
        return copy(this.codes, "codes");
    }

    public String[] locations() {
        return copy(this.locations, "locations");
    }

    public String[] messages() {
        return copy(this.messages, "messages");
    }

    public int flags() {
        return this.flags;
    }

    private void validate() {
        requireNonNegative(this.findingCount, "findingCount");
        requireNonNegative(this.errorCount, "errorCount");
        requireNonNegative(this.warningCount, "warningCount");
        requireNonNegative(this.infoCount, "infoCount");
        requireMatchingLength(this.findingCount, "severityIds", this.severityIds.length);
        requireMatchingLength(this.findingCount, "severities", this.severities.length);
        requireMatchingLength(this.findingCount, "codes", this.codes.length);
        requireMatchingLength(this.findingCount, "locations", this.locations.length);
        requireMatchingLength(this.findingCount, "messages", this.messages.length);
        if (this.errorCount + this.warningCount + this.infoCount != this.findingCount) {
            throw new IllegalArgumentException("validation severity counts must equal findingCount");
        }
        if (this.valid && this.errorCount > 0) {
            throw new IllegalArgumentException("valid validation state cannot contain errors");
        }
        for (int index = 0; index < this.findingCount; index++) {
            requireSeverityId(this.severityIds[index]);
            requireText(this.severities[index], "severities entries");
            requireText(this.codes[index], "codes entries");
            requireText(this.locations[index], "locations entries");
            requireText(this.messages[index], "messages entries");
        }
        int expectedFlags = flags(
                this.valid,
                this.readyForNativeHandoff,
                this.denoiseScheduled,
                this.infoCount,
                this.warningCount,
                this.errorCount
        );
        if (this.flags != expectedFlags) {
            throw new IllegalArgumentException("flags must match validation upload state");
        }
    }

    private static int severityId(PostProcessingValidationSeverity severity) {
        return switch (severity) {
            case INFO -> SEVERITY_INFO;
            case WARNING -> SEVERITY_WARNING;
            case ERROR -> SEVERITY_ERROR;
        };
    }

    private static void requireSeverityId(int severityId) {
        if (severityId != SEVERITY_INFO && severityId != SEVERITY_WARNING && severityId != SEVERITY_ERROR) {
            throw new IllegalArgumentException("severityIds entries must be known severity ids");
        }
    }

    private static int flags(
            boolean valid,
            boolean readyForNativeHandoff,
            boolean denoiseScheduled,
            int infoCount,
            int warningCount,
            int errorCount
    ) {
        int flags = 0;
        if (valid) {
            flags |= FLAG_VALID;
        }
        if (readyForNativeHandoff) {
            flags |= FLAG_READY_FOR_NATIVE_HANDOFF;
        }
        if (denoiseScheduled) {
            flags |= FLAG_DENOISE_SCHEDULED;
        }
        if (infoCount > 0) {
            flags |= FLAG_HAS_INFOS;
        }
        if (warningCount > 0) {
            flags |= FLAG_HAS_WARNINGS;
        }
        if (errorCount > 0) {
            flags |= FLAG_HAS_ERRORS;
        }
        return flags;
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireMatchingLength(int expected, String name, int actual) {
        if (actual != expected) {
            throw new IllegalArgumentException(name + " length must be " + expected + " but was " + actual);
        }
    }

    private static int[] copy(int[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }

    private static String[] copy(String[] values, String name) {
        Objects.requireNonNull(values, name);
        return Arrays.copyOf(values, values.length);
    }
}
