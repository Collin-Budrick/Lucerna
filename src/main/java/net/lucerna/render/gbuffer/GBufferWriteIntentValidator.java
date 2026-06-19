package net.lucerna.render.gbuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GBufferWriteIntentValidator {
    private GBufferWriteIntentValidator() {
    }

    public static GBufferWriteIntentValidationReport validateLucernaMain(GBufferWriteIntent intent) {
        return validateAgainst(intent, GBufferTargetContract.lucernaMain());
    }

    public static GBufferWriteIntentValidationReport validateAgainst(
            GBufferWriteIntent intent,
            GBufferTargetContract targetContract
    ) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(targetContract, "targetContract");

        List<GBufferWriteIntentValidationFinding> findings = new ArrayList<>();
        if (!targetContract.passId().equals(intent.passId())) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "PASS_ID_MISMATCH",
                    "$.passId",
                    "G-buffer write intent passId must match the target contract"
            ));
        }
        if (targetContract.numericPassId() != intent.numericPassId()) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "NUMERIC_PASS_ID_MISMATCH",
                    "$.numericPassId",
                    "G-buffer write intent numericPassId must match the target contract"
            ));
        }
        if (!intent.hasWritableAttachments()) {
            findings.add(GBufferWriteIntentValidationFinding.warning(
                    "NO_ATTACHMENT_INTENTS",
                    "$.attachments",
                    "G-buffer write intent does not describe any attachment writes"
            ));
        }
        if (intent.hasWritableAttachments() && !intent.dimensionsAvailable()) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "MISSING_DIMENSIONS",
                    "$.width",
                    "G-buffer write intent with attachments requires positive width and height"
            ));
        }

        for (GBufferAttachmentContract contractAttachment : targetContract.attachments()) {
            intent.attachmentIntent(contractAttachment.name()).ifPresentOrElse(
                    attachment -> validateAttachment(findings, attachment, contractAttachment),
                    () -> findings.add(GBufferWriteIntentValidationFinding.warning(
                            "MISSING_ATTACHMENT_INTENT",
                            "$.attachments[" + contractAttachment.name() + "]",
                            "No write intent was declared for a target G-buffer attachment"
                    ))
            );
        }

        for (GBufferAttachmentWriteIntent attachment : intent.attachments()) {
            if (targetContract.attachment(attachment.attachmentName()).isEmpty()) {
                findings.add(GBufferWriteIntentValidationFinding.error(
                        "UNKNOWN_ATTACHMENT_INTENT",
                        "$.attachments[" + attachment.attachmentName() + "]",
                        "G-buffer write intent references an attachment outside the target contract"
                ));
            }
        }

        return new GBufferWriteIntentValidationReport(findings);
    }

    private static void validateAttachment(
            List<GBufferWriteIntentValidationFinding> findings,
            GBufferAttachmentWriteIntent attachment,
            GBufferAttachmentContract contractAttachment
    ) {
        if (!contractAttachment.format().equals(attachment.format())) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "ATTACHMENT_FORMAT_MISMATCH",
                    "$.attachments[" + attachment.attachmentName() + "].format",
                    "G-buffer write intent format must match the target attachment format"
            ));
        }
        if (!attachment.semantic().attachmentName().equals(contractAttachment.name())) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "SEMANTIC_ATTACHMENT_MISMATCH",
                    "$.attachments[" + attachment.attachmentName() + "].semantic",
                    "G-buffer write semantic must map to the target attachment"
            ));
        }
        if (attachment.required() && !contractAttachment.usage().contains("color_attachment")
                && !contractAttachment.usage().contains("depth_stencil_attachment")) {
            findings.add(GBufferWriteIntentValidationFinding.error(
                    "ATTACHMENT_NOT_WRITABLE",
                    "$.attachments[" + attachment.attachmentName() + "].usage",
                    "Required G-buffer attachment must be writable by the first pass"
            ));
        }
    }
}
