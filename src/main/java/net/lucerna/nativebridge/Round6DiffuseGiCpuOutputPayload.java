package net.lucerna.nativebridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record Round6DiffuseGiCpuOutputPayload(
        Round6DiffuseGiCpuOutputSnapshot snapshot,
        byte[] rgba8,
        String reason
) {
    private static final double PREVIEW_EXPOSURE = 24.0D;
    private static final double PREVIEW_INVERSE_GAMMA = 1.0D / 2.2D;
    private static final int PREVIEW_VISIBLE_ALPHA_FLOOR = 192;
    private static final int PREVIEW_VISIBLE_CHANNEL_FLOOR = 72;
    private static final int CHANNEL_MASK = 0xFF;

    public Round6DiffuseGiCpuOutputPayload {
        if (snapshot == null) {
            snapshot = Round6DiffuseGiCpuOutputSnapshot.unavailable("Round 6 diffuse GI CPU output snapshot was not supplied");
        }
        rgba8 = rgba8 == null ? new byte[0] : Arrays.copyOf(rgba8, rgba8.length);
        if (reason == null || reason.isBlank()) {
            reason = available()
                    ? "Round 6 diffuse GI CPU output payload is available"
                    : "Round 6 diffuse GI CPU output payload is unavailable";
        } else {
            reason = reason.trim();
        }
    }

    public static Round6DiffuseGiCpuOutputPayload unavailable(String reason) {
        return new Round6DiffuseGiCpuOutputPayload(
                Round6DiffuseGiCpuOutputSnapshot.unavailable(reason),
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
                && this.snapshot.readyForPreviewPayload()
                && this.displayablePixelCount() > 0
                && this.peakChannel() > 0;
    }

    public boolean rawGiInputReady() {
        return readyForPreviewDraw();
    }

    public String previewReadinessReason() {
        if (!available()) {
            return "Round 6 diffuse GI payload is unavailable or does not match native CPU output dimensions";
        }
        if (!this.snapshot.readyForPreviewPayload()) {
            return "native Round 6 diffuse GI output write/resolve/readiness markers are incomplete";
        }
        if (this.displayablePixelCount() <= 0 || this.peakChannel() <= 0) {
            return "native Round 6 diffuse GI output contains no displayable nonzero RGBA pixels";
        }
        return "native Round 6 diffuse GI output is ready for sampled final-composite preview draw";
    }

    public String rawGiInputSourceLabel() {
        return rawGiInputReady()
                ? "native scene-tied raw diffuse-GI RGBA8 CPU/readback payload"
                : "blocked native scene-tied raw diffuse-GI RGBA8 CPU/readback payload: "
                + previewReadinessReason();
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
                + " rawGiInputReady=" + this.rawGiInputReady()
                + " rawGiInputSource=\"" + this.rawGiInputSourceLabel() + "\""
                + " size=" + this.width() + "x" + this.height()
                + " bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " displayablePixels=" + this.displayablePixelCount()
                + " peakChannel=" + this.peakChannel()
                + " generation=" + this.snapshot.dispatchGeneration()
                + " outputs=" + this.snapshot.outputCount()
                + " checksum=" + this.snapshot.outputChecksum()
                + " visibleSignalGenerated=" + this.snapshot.visibleSignalGenerated()
                + " visibleSignalNonzeroPixels=" + this.snapshot.visibleSignalNonzeroPixels()
                + " outputMarker=" + this.snapshot.outputMarker()
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
