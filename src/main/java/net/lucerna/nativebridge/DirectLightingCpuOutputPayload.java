package net.lucerna.nativebridge;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record DirectLightingCpuOutputPayload(
        DirectLightingCpuOutputSnapshot snapshot,
        byte[] rgba8,
        String reason
) {
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

    public ByteBuffer copyToByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(this.rgba8.length);
        buffer.put(this.rgba8);
        buffer.flip();
        return buffer;
    }

    public String debugSummary() {
        return "available=" + this.available()
                + " size=" + this.width() + "x" + this.height()
                + " bytes=" + this.byteCount()
                + " expectedBytes=" + this.expectedByteCount()
                + " generation=" + this.snapshot.dispatchGeneration()
                + " checksum=" + this.snapshot.outputChecksum()
                + " reason=" + this.reason;
    }
}
