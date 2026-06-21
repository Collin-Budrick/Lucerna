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

    public String previewReadinessReason() {
        if (!this.available()) {
            return "directional shadow-map RGBA8 payload is unavailable or does not match native texel dimensions";
        }
        if (this.displayablePixelCount() <= 0 || this.peakChannel() <= 0) {
            return "directional shadow-map RGBA8 payload contains no displayable nonzero pixels";
        }
        return "directional shadow-map RGBA8 payload is ready for upload";
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

    public ByteBuffer copyToByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(this.rgba8.length);
        buffer.put(this.rgba8);
        buffer.flip();
        return buffer;
    }

    public String debugSummary() {
        return "available=" + this.available()
                + " readyForPreviewUpload=" + this.readyForPreviewUpload()
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
                + " readinessReason=" + this.previewReadinessReason()
                + " reason=" + this.reason;
    }
}
