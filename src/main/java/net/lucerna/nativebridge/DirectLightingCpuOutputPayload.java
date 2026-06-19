package net.lucerna.nativebridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DirectLightingCpuOutputPayload(
        DirectLightingCpuOutputSnapshot snapshot,
        byte[] rgba8,
        String reason
) {
    private static final double PREVIEW_EXPOSURE = 24.0D;
    private static final double PREVIEW_INVERSE_GAMMA = 1.0D / 2.2D;
    private static final int PREVIEW_VISIBLE_ALPHA_FLOOR = 192;
    private static final int PREVIEW_VISIBLE_CHANNEL_FLOOR = 72;
    private static final int CHANNEL_MASK = 0xFF;

    public DirectLightingCpuOutputPayload {
        if (snapshot == null) {
            snapshot = DirectLightingCpuOutputSnapshot.unavailable("direct-light CPU output snapshot was not supplied");
        }
        rgba8 = rgba8 == null ? new byte[0] : Arrays.copyOf(rgba8, rgba8.length);
        if (reason == null || reason.isBlank()) {
            reason = available()
                    ? "direct-light CPU output payload is available"
                    : "direct-light CPU output payload is unavailable";
        } else {
            reason = reason.trim();
        }
    }

    public static DirectLightingCpuOutputPayload unavailable(String reason) {
        return new DirectLightingCpuOutputPayload(
                DirectLightingCpuOutputSnapshot.unavailable(reason),
                new byte[0],
                reason
        );
    }

    public boolean available() {
        return this.snapshot.hasCpuOutputTelemetry()
                && this.snapshot.hasNonzeroEnergy()
                && this.width() > 0
                && this.height() > 0
                && (long) this.width() * (long) this.height() == this.snapshot.outputPixels()
                && this.rgba8.length == this.expectedByteCount();
    }

    public boolean readyForPreviewDraw() {
        return available()
                && hasResolvedDirectLightingOutput()
                && this.displayablePixelCount() > 0
                && this.peakChannel() > 0;
    }

    public String previewReadinessReason() {
        if (!available()) {
            return "payload is unavailable or does not match native CPU output dimensions";
        }
        if (!hasResolvedDirectLightingOutput()) {
            return "native direct-light output write/resolve markers are incomplete";
        }
        if (this.displayablePixelCount() <= 0 || this.peakChannel() <= 0) {
            return "native direct-light output contains no displayable nonzero RGBA pixels";
        }
        return "native direct-light output is ready for sampled preview draw";
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
        putDisplayMappedRgba8(buffer);
        buffer.flip();
        return buffer;
    }

    public String debugSummary() {
        return "available=" + this.available()
                + " readyForPreviewDraw=" + this.readyForPreviewDraw()
                + " size=" + this.width() + "x" + this.height()
                + " bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " generation=" + this.snapshot.dispatchGeneration()
                + " checksum=" + this.snapshot.outputChecksum()
                + " readinessReason=" + this.previewReadinessReason()
                + " reason=" + this.reason;
    }

    private void putDisplayMappedRgba8(ByteBuffer buffer) {
        int completePixelBytes = this.rgba8.length - (this.rgba8.length % 4);
        boolean previewReady = readyForPreviewDraw();
        for (int index = 0; index < completePixelBytes; index += 4) {
            int red = this.rgba8[index] & CHANNEL_MASK;
            int green = this.rgba8[index + 1] & CHANNEL_MASK;
            int blue = this.rgba8[index + 2] & CHANNEL_MASK;
            int alpha = this.rgba8[index + 3] & CHANNEL_MASK;
            int maxColor = Math.max(red, Math.max(green, blue));

            if (!previewReady || maxColor == 0) {
                buffer.put((byte) 0);
                buffer.put((byte) 0);
                buffer.put((byte) 0);
                buffer.put((byte) 0);
                continue;
            }

            buffer.put((byte) displayMapChannel(red));
            buffer.put((byte) displayMapChannel(green));
            buffer.put((byte) displayMapChannel(blue));
            buffer.put((byte) Math.max(alpha, PREVIEW_VISIBLE_ALPHA_FLOOR));
        }
        for (int index = completePixelBytes; index < this.rgba8.length; index++) {
            buffer.put((byte) 0);
        }
    }

    private boolean hasResolvedDirectLightingOutput() {
        return this.snapshot.outputWriteRecorded()
                && this.snapshot.resolveRecorded()
                && this.snapshot.outputCount() > 0
                && !this.snapshot.outputChecksum().isBlank()
                && !"0".equals(this.snapshot.outputChecksum());
    }

    private static int displayMapChannel(int channel) {
        if (channel <= 0) {
            return 0;
        }
        double linear = channel / 255.0D;
        double exposed = 1.0D - Math.exp(-linear * PREVIEW_EXPOSURE);
        double gammaMapped = Math.pow(exposed, PREVIEW_INVERSE_GAMMA);
        return Math.max(PREVIEW_VISIBLE_CHANNEL_FLOOR, clampToByte((int) Math.round(gammaMapped * 255.0D)));
    }

    private static int clampToByte(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
