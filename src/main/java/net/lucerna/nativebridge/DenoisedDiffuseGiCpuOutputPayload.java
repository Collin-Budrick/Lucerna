package net.lucerna.nativebridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DenoisedDiffuseGiCpuOutputPayload(
        DenoisedDiffuseGiCpuOutputSnapshot snapshot,
        byte[] rgba8,
        String reason
) {
    private static final int CHANNEL_MASK = 0xFF;

    public DenoisedDiffuseGiCpuOutputPayload {
        if (snapshot == null) {
            snapshot = DenoisedDiffuseGiCpuOutputSnapshot.unavailable(
                    "denoised diffuse GI CPU output snapshot was not supplied"
            );
        }
        rgba8 = rgba8 == null ? new byte[0] : Arrays.copyOf(rgba8, rgba8.length);
        if (reason == null || reason.isBlank()) {
            reason = available()
                    ? "denoised diffuse GI CPU output payload is available"
                    : "denoised diffuse GI CPU output payload is unavailable";
        } else {
            reason = reason.trim();
        }
    }

    public static DenoisedDiffuseGiCpuOutputPayload unavailable(String reason) {
        return new DenoisedDiffuseGiCpuOutputPayload(
                DenoisedDiffuseGiCpuOutputSnapshot.unavailable(reason),
                new byte[0],
                reason
        );
    }

    public boolean available() {
        return this.snapshot.readyForPreviewPayload()
                && this.width() > 0
                && this.height() > 0
                && this.pixelCount() == this.snapshot.outputPixels()
                && this.rgba8.length == this.expectedByteCount();
    }

    public boolean readyForPreviewDraw() {
        return this.available()
                && this.displayablePixelCount() > 0
                && this.peakChannel() > 0;
    }

    public boolean cpuOutputReadbackReady() {
        return this.snapshot.cpuOutputReadbackReady()
                && this.available();
    }

    public boolean denoiseQualityEvidenceReady() {
        return this.snapshot.denoiseQualityEvidenceReady()
                && this.readyForPreviewDraw();
    }

    public String previewReadinessReason() {
        if (!this.snapshot.readyForPreviewPayload()) {
            return this.snapshot.previewReadinessReason();
        }
        if (this.rgba8.length != this.expectedByteCount()) {
            return "denoised diffuse GI RGBA8 payload size does not match native output telemetry";
        }
        if (this.displayablePixelCount() <= 0 || this.peakChannel() <= 0) {
            return "denoised diffuse GI RGBA8 payload contains no displayable nonzero pixels";
        }
        return "denoised diffuse GI RGBA8 payload is ready for preview draw";
    }

    public String readinessBoundarySummary() {
        return "cpuOutputReadbackReady=" + this.cpuOutputReadbackReady()
                + ",previewDrawReady=" + this.readyForPreviewDraw()
                + ",denoiseQualityEvidenceReady=" + this.denoiseQualityEvidenceReady()
                + ",realDenoiseShaderOutput=" + this.snapshot.realDenoiseShaderOutput()
                + ",boundary=\"" + this.snapshot.outputReadinessBoundary() + "\"";
    }

    public int width() {
        return this.snapshot.outputWidth();
    }

    public int height() {
        return this.snapshot.outputHeight();
    }

    public int pixelCount() {
        return this.snapshot.outputPixels();
    }

    public int byteCount() {
        return this.rgba8.length;
    }

    public int expectedByteCount() {
        long pixels = Math.max(0L, this.snapshot.outputPixels());
        long bytes = pixels * 4L;
        return bytes > Integer.MAX_VALUE ? -1 : (int) bytes;
    }

    public int displayablePixelCount() {
        int count = 0;
        int completePixelBytes = this.rgba8.length - (this.rgba8.length % 4);
        for (int index = 0; index < completePixelBytes; index += 4) {
            int red = this.rgba8[index] & CHANNEL_MASK;
            int green = this.rgba8[index + 1] & CHANNEL_MASK;
            int blue = this.rgba8[index + 2] & CHANNEL_MASK;
            if (Math.max(red, Math.max(green, blue)) > 0) {
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
                + " readyForPreviewDraw=" + this.readyForPreviewDraw()
                + " " + this.readinessBoundarySummary()
                + " evidence=" + this.snapshot.outputEvidenceMarker()
                + " size=" + this.width() + "x" + this.height()
                + " bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " generation=" + this.snapshot.dispatchGeneration()
                + " outputs=" + this.snapshot.outputCount()
                + " realDenoiseShaderOutput=" + this.snapshot.realDenoiseShaderOutput()
                + " outputMarker=" + this.snapshot.outputMarker()
                + " denoisedOutputMarker=" + this.snapshot.denoisedOutputMarker()
                + " readinessReason=" + this.previewReadinessReason()
                + " reason=" + this.reason;
    }
}
