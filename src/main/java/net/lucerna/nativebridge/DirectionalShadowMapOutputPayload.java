package net.lucerna.nativebridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DirectionalShadowMapOutputPayload(
        DirectionalShadowMapOutputSnapshot snapshot,
        byte[] rgba8,
        String reason
) {
    private static final int CHANNEL_MASK = 0xFF;

    public DirectionalShadowMapOutputPayload {
        if (snapshot == null) {
            snapshot = DirectionalShadowMapOutputSnapshot.unavailable("directional shadow-map output snapshot was not supplied");
        }
        rgba8 = rgba8 == null ? new byte[0] : Arrays.copyOf(rgba8, rgba8.length);
        if (reason == null || reason.isBlank()) {
            reason = available()
                    ? "directional shadow-map RGBA8 payload is available"
                    : "directional shadow-map RGBA8 payload is unavailable";
        } else {
            reason = reason.trim();
        }
    }

    public static DirectionalShadowMapOutputPayload unavailable(String reason) {
        return new DirectionalShadowMapOutputPayload(
                DirectionalShadowMapOutputSnapshot.unavailable(reason),
                new byte[0],
                reason
        );
    }

    public boolean available() {
        return this.snapshot.readyForPreviewPayload()
                && this.width() > 0
                && this.height() > 0
                && this.pixelCount() > 0
                && (long) this.width() * (long) this.height() == this.pixelCount()
                && this.rgba8.length == this.expectedByteCount();
    }

    public boolean readyForPreviewUpload() {
        return this.available()
                && this.displayablePixelCount() > 0
                && this.peakChannel() > 0;
    }

    public boolean readyForPreviewDraw() {
        return this.readyForPreviewUpload();
    }

    public boolean readyForFinalCompositeConsumption() {
        return this.readyForPreviewDraw()
                && this.snapshot.readyForFinalCompositeConsumption()
                && this.byteCount() == this.expectedByteCount();
    }

    public boolean finalCompositeConsumptionBoundaryHonest() {
        return this.readyForFinalCompositeConsumption()
                && this.snapshot.finalCompositeConsumptionBoundaryHonest();
    }

    public boolean softShadowMaskReady() {
        return this.readyForFinalCompositeConsumption()
                && this.snapshot.softShadowMaskReady()
                && this.displayablePixelCount() > 0
                && this.peakChannel() > 0;
    }

    public boolean receiverTiedSoftShadowPayloadReady() {
        return this.softShadowMaskReady()
                && this.snapshot.receiverTiedSoftShadowPayloadReady();
    }

    public boolean visualQualityStillBlocked() {
        return !this.receiverTiedSoftShadowPayloadReady()
                || this.snapshot.visualQualityStillBlocked();
    }

    public String previewReadinessReason() {
        if (!this.available()) {
            return "directional shadow-map RGBA8 payload is unavailable or does not match native texel dimensions";
        }
        if (this.displayablePixelCount() <= 0 || this.peakChannel() <= 0) {
            return "directional shadow-map RGBA8 payload contains no displayable nonzero pixels";
        }
        return "directional shadow-map RGBA8 payload is ready for upload";
    }

    public String finalCompositeReadinessReason() {
        if (!this.readyForPreviewDraw()) {
            return this.previewReadinessReason();
        }
        if (!this.snapshot.readyForFinalCompositeConsumption()) {
            return this.snapshot.finalCompositeReadinessReason();
        }
        if (!this.snapshot.finalCompositeConsumptionBoundaryHonest()) {
            return "directional shadow-map final composite would overclaim GPU or hardware-RT shadow output";
        }
        return "directional shadow-map RGBA8 payload is ready for final-composite consumption";
    }

    public String blockerSummary() {
        StringBuilder builder = new StringBuilder();
        appendBlocker(builder, !this.available(), "payload unavailable or byte dimensions mismatch");
        appendBlocker(builder, this.displayablePixelCount() <= 0, "payload has no displayable pixels");
        appendBlocker(builder, this.peakChannel() <= 0, "payload peak channel is zero");
        appendBlocker(builder, !this.snapshot.receiverTiedSoftShadowPayloadReady(), this.snapshot.blockerSummary());
        appendBlocker(builder, !this.snapshot.finalCompositeConsumptionBoundaryHonest(),
                "shadow payload final-composite boundary is not honest");
        return builder.length() == 0 ? "none" : builder.toString();
    }

    public int width() {
        return this.snapshot.texelWidth();
    }

    public int height() {
        return this.snapshot.texelHeight();
    }

    public int pixelCount() {
        return this.snapshot.texelCount();
    }

    public int byteCount() {
        return this.rgba8.length;
    }

    public int expectedByteCount() {
        return this.snapshot.expectedByteCount();
    }

    public int displayablePixelCount() {
        int count = 0;
        int completePixelBytes = this.rgba8.length - (this.rgba8.length % 4);
        for (int index = 0; index < completePixelBytes; index += 4) {
            int red = this.rgba8[index] & CHANNEL_MASK;
            int green = this.rgba8[index + 1] & CHANNEL_MASK;
            int blue = this.rgba8[index + 2] & CHANNEL_MASK;
            int alpha = this.rgba8[index + 3] & CHANNEL_MASK;
            if (Math.max(Math.max(red, green), Math.max(blue, alpha)) > 0) {
                count++;
            }
        }
        return count;
    }

    public int peakChannel() {
        int peak = 0;
        int completePixelBytes = this.rgba8.length - (this.rgba8.length % 4);
        for (int index = 0; index < completePixelBytes; index += 4) {
            peak = Math.max(peak, this.rgba8[index] & CHANNEL_MASK);
            peak = Math.max(peak, this.rgba8[index + 1] & CHANNEL_MASK);
            peak = Math.max(peak, this.rgba8[index + 2] & CHANNEL_MASK);
            peak = Math.max(peak, this.rgba8[index + 3] & CHANNEL_MASK);
        }
        return peak;
    }

    public String budgetMetadataSummary() {
        return "payloadBudget bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " pixels=" + this.pixelCount()
                + " size=" + this.width() + "x" + this.height()
                + " outputSamples=" + this.snapshot.outputSampleCount()
                + " depthCoveredTexels=" + this.snapshot.depthCoveredTexelCount()
                + " checksum=" + this.snapshot.checksum();
    }

    public String finalCompositeConsumptionSummary() {
        return "readyForFinalCompositeConsumption=" + this.readyForFinalCompositeConsumption()
                + " finalCompositeBoundaryHonest=" + this.finalCompositeConsumptionBoundaryHonest()
                + " softShadowMaskReady=" + this.softShadowMaskReady()
                + " receiverTiedSoftShadowPayloadReady=" + this.receiverTiedSoftShadowPayloadReady()
                + " visualQualityStillBlocked=" + this.visualQualityStillBlocked()
                + " sourceKind=native-conservative-shadow-map-rgba8"
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " " + this.budgetMetadataSummary()
                + " blockers=" + this.blockerSummary()
                + " reason=" + this.finalCompositeReadinessReason();
    }

    public String softShadowMaskSummary() {
        return "softShadowMaskReady=" + this.softShadowMaskReady()
                + " softReceiverTiedShadowMask=" + this.receiverTiedSoftShadowPayloadReady()
                + " shadowMaskReceiverTied=" + this.snapshot.receiverTiedShadowMaskReady()
                + " shadowMaskSoftened=" + this.receiverTiedSoftShadowPayloadReady()
                + " readyForPreviewUpload=" + this.readyForPreviewUpload()
                + " readyForFinalCompositeConsumption=" + this.readyForFinalCompositeConsumption()
                + " finalCompositeBoundaryHonest=" + this.finalCompositeConsumptionBoundaryHonest()
                + " visualQualityStillBlocked=" + this.visualQualityStillBlocked()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " " + this.snapshot.softShadowMaskSummary()
                + " blockers=" + this.blockerSummary();
    }

    public String receiverTiedSoftShadowPayloadSummary() {
        return "receiverTiedSoftShadowPayloadReady=" + this.receiverTiedSoftShadowPayloadReady()
                + " softReceiverTiedShadowMask=" + this.receiverTiedSoftShadowPayloadReady()
                + " shadowMaskReceiverTied=" + this.snapshot.receiverTiedShadowMaskReady()
                + " shadowMaskSoftened=" + this.receiverTiedSoftShadowPayloadReady()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " byteCount=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " " + this.snapshot.receiverTiedSoftShadowPayloadSummary()
                + " blockers=" + this.blockerSummary();
    }

    public ByteBuffer copyToByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(this.rgba8.length);
        buffer.put(this.rgba8);
        buffer.flip();
        return buffer;
    }

    public String debugSummary() {
        return "available=" + this.available()
                + " readyForPreviewUpload=" + this.readyForPreviewUpload()
                + " softShadowMaskReady=" + this.softShadowMaskReady()
                + " receiverTiedSoftShadowPayloadReady=" + this.receiverTiedSoftShadowPayloadReady()
                + " size=" + this.width() + "x" + this.height()
                + " bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " outputSamples=" + this.snapshot.outputSampleCount()
                + " outputCasters=" + this.snapshot.outputCasterCount()
                + " outputReceivers=" + this.snapshot.outputReceiverCount()
                + " depthCoveredTexels=" + this.snapshot.depthCoveredTexelCount()
                + " checksum=" + this.snapshot.checksum()
                + " marker=" + this.snapshot.marker()
                + " blocker=" + this.snapshot.blocker()
                + " receiverBlocker=" + this.snapshot.receiverBlocker()
                + " casterBlocker=" + this.snapshot.casterBlocker()
                + " depthBlocker=" + this.snapshot.depthBlocker()
                + " " + this.snapshot.boundarySummary()
                + " " + this.finalCompositeConsumptionSummary()
                + " readinessReason=" + this.previewReadinessReason()
                + " reason=" + this.reason;
    }

    private static void appendBlocker(StringBuilder builder, boolean active, String message) {
        if (!active) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(message);
    }
}
