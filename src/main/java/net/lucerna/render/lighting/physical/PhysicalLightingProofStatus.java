package net.lucerna.render.lighting.physical;

import net.lucerna.render.preview.PublicMojangFinalCompositeSubmissionResult;
import net.lucerna.telemetry.LightingDispatchStageTelemetryStatus;
import net.lucerna.telemetry.LucernaStatusSnapshot;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PhysicalLightingProofStatus(
        boolean physicalSurfaceContribution,
        boolean focusWindowContribution,
        boolean previewFallbackContribution,
        String surfacePayloadConfidence,
        String physicalOutputEnergy,
        String physicalOutputChecksum,
        String directOutputEnergy,
        String directOutputChecksum,
        String giOutputEnergy,
        String giOutputChecksum,
        boolean metadataOnlyPhysicalPayload,
        boolean metadataOnlyPreview,
        String source
) {
    private static final String UNKNOWN = "not_recorded";

    public PhysicalLightingProofStatus {
        surfacePayloadConfidence = clean(surfacePayloadConfidence, UNKNOWN);
        physicalOutputEnergy = clean(physicalOutputEnergy, UNKNOWN);
        physicalOutputChecksum = clean(physicalOutputChecksum, UNKNOWN);
        directOutputEnergy = clean(directOutputEnergy, UNKNOWN);
        directOutputChecksum = clean(directOutputChecksum, UNKNOWN);
        giOutputEnergy = clean(giOutputEnergy, UNKNOWN);
        giOutputChecksum = clean(giOutputChecksum, UNKNOWN);
        source = clean(source, "controller-status-bridge");
    }

    public static PhysicalLightingProofStatus from(
            LucernaStatusSnapshot snapshot,
            PublicMojangFinalCompositeSubmissionResult finalComposite
    ) {
        String nativeStatus = snapshot == null || snapshot.nativeBridge() == null
                ? ""
                : snapshot.nativeBridge().nativeStatus();
        LightingDispatchStageTelemetryStatus directStage = stage(snapshot, "direct_lighting");
        LightingDispatchStageTelemetryStatus giStage = firstStage(snapshot, "diffuse_gi", "gi", "low_res_gi", "low_resolution_gi");

        boolean focusWindowContribution = firstBoolean(
                finalComposite != null && finalComposite.submittedFocusWindowOnly(),
                nativeStatus,
                "focus_window_contribution",
                "focusWindowContribution",
                "focus_window_only",
                "focusWindowOnly"
        );
        boolean previewFallbackContribution = firstBoolean(
                finalComposite != null && finalComposite.submittedPreviewOnlyEvidence(),
                nativeStatus,
                "preview_fallback_contribution",
                "previewFallbackContribution",
                "preview_fallback",
                "previewFallback",
                "temporary_direct_light_source",
                "temporaryDirectLightSource",
                "temporary_direct_source",
                "temporaryDirectSource"
        );
        boolean metadataOnlyPreview = firstBoolean(
                finalComposite != null && finalComposite.submittedMetadataOnlyPreview(),
                nativeStatus,
                "metadata_only_preview",
                "metadataOnlyPreview",
                "metadata_preview",
                "metadataPreview"
        );
        boolean metadataOnlyPhysicalPayload = firstBoolean(
                stageMetadataOnly(directStage) || stageMetadataOnly(giStage),
                nativeStatus,
                "metadata_only_physical_payload",
                "metadataOnlyPhysicalPayload",
                "physical_payload_metadata_only",
                "physicalPayloadMetadataOnly",
                "metadata_only"
        );
        boolean finalSurfaceContribution = finalComposite != null
                && finalComposite.submitted()
                && finalComposite.drawCallsIssued()
                && finalComposite.submittedSourceGatedSurfaceProjection()
                && !finalComposite.submittedPreviewOnlyEvidence();
        boolean physicalSurfaceContribution = firstBoolean(
                finalSurfaceContribution,
                nativeStatus,
                "physical_surface_contribution",
                "physicalSurfaceContribution",
                "surface_physical_contribution",
                "surfacePhysicalContribution"
        );

        String directEnergy = firstNonBlank(
                findValue(nativeStatus, "direct_output_energy", "directOutputEnergy"),
                stageOutputEnergy(directStage)
        );
        String directChecksum = firstNonBlank(
                findValue(nativeStatus, "direct_output_checksum", "directOutputChecksum"),
                stageOutputChecksum(directStage)
        );
        String giEnergy = firstNonBlank(
                findValue(nativeStatus, "gi_output_energy", "giOutputEnergy", "native_gi_output_energy", "nativeGiOutputEnergy"),
                stageOutputEnergy(giStage)
        );
        String giChecksum = firstNonBlank(
                findValue(nativeStatus, "gi_output_checksum", "giOutputChecksum", "native_gi_output_checksum", "nativeGiOutputChecksum"),
                stageOutputChecksum(giStage)
        );
        String physicalEnergy = firstNonBlank(
                findValue(nativeStatus, "physical_output_energy", "physicalOutputEnergy", "surface_output_energy", "surfaceOutputEnergy"),
                giEnergy,
                directEnergy
        );
        String physicalChecksum = firstNonBlank(
                findValue(nativeStatus, "physical_output_checksum", "physicalOutputChecksum", "surface_output_checksum", "surfaceOutputChecksum"),
                giChecksum,
                directChecksum
        );
        String confidence = firstNonBlank(
                findValue(
                        nativeStatus,
                        "surface_payload_confidence",
                        "surfacePayloadConfidence",
                        "physical_surface_confidence",
                        "physicalSurfaceConfidence",
                        "payload_confidence",
                        "payloadConfidence"
                ),
                stageDetail(giStage, "surface_payload_confidence", "physical_surface_confidence", "payload_confidence"),
                stageDetail(directStage, "surface_payload_confidence", "physical_surface_confidence", "payload_confidence")
        );

        return new PhysicalLightingProofStatus(
                physicalSurfaceContribution,
                focusWindowContribution,
                previewFallbackContribution,
                confidence,
                physicalEnergy,
                physicalChecksum,
                directEnergy,
                directChecksum,
                giEnergy,
                giChecksum,
                metadataOnlyPhysicalPayload,
                metadataOnlyPreview,
                sourceLabel(finalComposite, nativeStatus)
        );
    }

    public String logFields() {
        return "physicalSurfaceContribution=" + this.physicalSurfaceContribution
                + " focusWindowContribution=" + this.focusWindowContribution
                + " previewFallbackContribution=" + this.previewFallbackContribution
                + " surfacePayloadConfidence=" + this.surfacePayloadConfidence
                + " physicalOutputEnergy=" + this.physicalOutputEnergy
                + " physicalOutputChecksum=" + this.physicalOutputChecksum
                + " directOutputEnergy=" + this.directOutputEnergy
                + " directOutputChecksum=" + this.directOutputChecksum
                + " giOutputEnergy=" + this.giOutputEnergy
                + " giOutputChecksum=" + this.giOutputChecksum
                + " metadataOnlyPhysicalPayload=" + this.metadataOnlyPhysicalPayload
                + " metadataOnlyPreview=" + this.metadataOnlyPreview
                + " physicalProofSource=" + this.source;
    }

    private static LightingDispatchStageTelemetryStatus firstStage(LucernaStatusSnapshot snapshot, String... stageIds) {
        if (stageIds == null) {
            return null;
        }
        for (String stageId : stageIds) {
            LightingDispatchStageTelemetryStatus stage = stage(snapshot, stageId);
            if (stage != null) {
                return stage;
            }
        }
        return null;
    }

    private static LightingDispatchStageTelemetryStatus stage(LucernaStatusSnapshot snapshot, String stageId) {
        if (snapshot == null || snapshot.lightingDispatchStatus() == null || stageId == null || stageId.isBlank()) {
            return null;
        }
        return snapshot.lightingDispatchStatus().stages().get(normalizeKey(stageId));
    }

    private static boolean stageMetadataOnly(LightingDispatchStageTelemetryStatus stage) {
        return stage != null && Boolean.TRUE.equals(stage.metadataOnly());
    }

    private static String stageOutputEnergy(LightingDispatchStageTelemetryStatus stage) {
        return stage == null ? "" : stage.outputEnergy();
    }

    private static String stageOutputChecksum(LightingDispatchStageTelemetryStatus stage) {
        return stage == null || stage.outputChecksum() == null ? "" : Long.toString(stage.outputChecksum());
    }

    private static String stageDetail(LightingDispatchStageTelemetryStatus stage, String... keys) {
        if (stage == null || stage.details() == null || keys == null) {
            return "";
        }
        Map<String, String> details = stage.details();
        for (String key : keys) {
            String value = details.get(normalizeKey(key));
            if (value != null && !value.isBlank()) {
                return stripQuotes(value);
            }
        }
        return "";
    }

    private static boolean firstBoolean(boolean fallback, String nativeStatus, String... keys) {
        String value = findValue(nativeStatus, keys);
        if (value.isBlank()) {
            return fallback;
        }
        return switch (stripQuotes(value).toLowerCase(Locale.ROOT)) {
            case "1", "true", "yes", "y", "on", "enabled", "active", "present" -> true;
            case "0", "false", "no", "n", "off", "disabled", "inactive", "absent" -> false;
            default -> fallback;
        };
    }

    private static String findValue(String nativeStatus, String... keys) {
        if (nativeStatus == null || nativeStatus.isBlank() || keys == null) {
            return "";
        }
        for (String key : keys) {
            String value = findExactValue(nativeStatus, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String findExactValue(String nativeStatus, String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        Pattern pattern = Pattern.compile(
                "(?i)(?:^|[\\s,{])" + Pattern.quote(key) + "\\s*[:=]\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;}\\]]+)"
        );
        Matcher matcher = pattern.matcher(nativeStatus);
        if (!matcher.find()) {
            return "";
        }
        return stripQuotes(matcher.group(1));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String cleaned = stripQuotes(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private static String sourceLabel(PublicMojangFinalCompositeSubmissionResult finalComposite, String nativeStatus) {
        String explicit = findValue(nativeStatus, "physical_proof_source", "physicalProofSource", "physical_source", "physicalSource");
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (finalComposite == null) {
            return "native-status-only";
        }
        if (!finalComposite.submitted()) {
            return "final-composite-not-submitted";
        }
        return finalComposite.submittedSourceIdentity();
    }

    private static String normalizeKey(String value) {
        return stripQuotes(value).toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.length() >= 2
                && ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'")))) {
            return cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String clean(String value, String fallback) {
        String cleaned = stripQuotes(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
