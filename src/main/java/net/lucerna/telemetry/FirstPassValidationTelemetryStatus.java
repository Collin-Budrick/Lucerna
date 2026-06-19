package net.lucerna.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record FirstPassValidationTelemetryStatus(
        boolean validationAvailable,
        boolean placeholder,
        boolean gBufferStagingPresent,
        boolean frameConstantsReady,
        boolean framePassAttachable,
        boolean nativePassStatesAvailable,
        String nativeFutureGBufferState,
        String summary
) {
    public FirstPassValidationTelemetryStatus {
        nativeFutureGBufferState = clean(nativeFutureGBufferState, "unreported");
        summary = clean(summary, "First-pass validation has not been reported.");
        if (!validationAvailable) {
            placeholder = true;
        }
    }

    public static FirstPassValidationTelemetryStatus placeholder(LucernaStatusSnapshot snapshot) {
        if (snapshot == null) {
            return new FirstPassValidationTelemetryStatus(
                    false,
                    true,
                    false,
                    false,
                    false,
                    false,
                    "unreported",
                    "placeholder: first-pass validation is waiting for a status snapshot."
            );
        }

        NativePassTelemetryStatus nativePassStates = snapshot.nativePassStates();
        String futureGBufferState = nativePassStates.stateFor("future_gbuffer");
        if (futureGBufferState.isBlank()) {
            futureGBufferState = nativePassStates.stateFor("lucerna.gbuffer.main");
        }
        if (futureGBufferState.isBlank()) {
            futureGBufferState = "unreported";
        }

        boolean gBufferStagingPresent = snapshot.stagedGBufferStagingCount() > 0
                || snapshot.uploadGBufferStagingGeneration() > 0L;
        boolean frameConstantsReady = snapshot.frameConstantsRequiredAvailable();
        boolean framePassAttachable = snapshot.frameLifecycle().framePassAttachable();
        boolean nativePassStatesAvailable = nativePassStates.hasPassStates();

        return new FirstPassValidationTelemetryStatus(
                false,
                true,
                gBufferStagingPresent,
                frameConstantsReady,
                framePassAttachable,
                nativePassStatesAvailable,
                futureGBufferState,
                "placeholder: gbufferStaged=%s count=%d gen=%d nativeFutureGBuffer=%s framePass=%s constants=%s"
                        .formatted(
                                Boolean.toString(gBufferStagingPresent),
                                snapshot.stagedGBufferStagingCount(),
                                snapshot.uploadGBufferStagingGeneration(),
                                futureGBufferState,
                                snapshot.frameLifecycle().framePassStatusCode().name(),
                                snapshot.frameConstants().stateLabel()
                        )
        );
    }

    public Map<String, String> validationFields(String prefix) {
        String normalizedPrefix = clean(prefix, "firstPass");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(normalizedPrefix + ".validationAvailable", Boolean.toString(this.validationAvailable));
        fields.put(normalizedPrefix + ".placeholder", Boolean.toString(this.placeholder));
        fields.put(normalizedPrefix + ".gBufferStagingPresent", Boolean.toString(this.gBufferStagingPresent));
        fields.put(normalizedPrefix + ".frameConstantsReady", Boolean.toString(this.frameConstantsReady));
        fields.put(normalizedPrefix + ".framePassAttachable", Boolean.toString(this.framePassAttachable));
        fields.put(normalizedPrefix + ".nativePassStatesAvailable", Boolean.toString(this.nativePassStatesAvailable));
        fields.put(normalizedPrefix + ".nativeFutureGBufferState", this.nativeFutureGBufferState);
        fields.put(normalizedPrefix + ".summary", this.summary);
        return Collections.unmodifiableMap(fields);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
