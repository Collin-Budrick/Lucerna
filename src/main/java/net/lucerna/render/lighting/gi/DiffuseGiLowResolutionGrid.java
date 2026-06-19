package net.lucerna.render.lighting.gi;

import net.lucerna.render.frame.FrameViewport;

public record DiffuseGiLowResolutionGrid(
        int sourceWidth,
        int sourceHeight,
        int scaleDivisor,
        int width,
        int height
) {
    public DiffuseGiLowResolutionGrid {
        requireNonNegative(sourceWidth, "sourceWidth");
        requireNonNegative(sourceHeight, "sourceHeight");
        if (scaleDivisor <= 0) {
            throw new IllegalArgumentException("scaleDivisor must be positive");
        }
        requireNonNegative(width, "width");
        requireNonNegative(height, "height");
        if ((sourceWidth == 0 || sourceHeight == 0) && (width != 0 || height != 0)) {
            throw new IllegalArgumentException("unavailable source dimensions require an unavailable low-resolution grid");
        }
        if (sourceWidth > 0 && sourceHeight > 0 && (width == 0 || height == 0)) {
            throw new IllegalArgumentException("available source dimensions require a positive low-resolution grid");
        }
    }

    public static DiffuseGiLowResolutionGrid unavailable() {
        return unavailable(1);
    }

    public static DiffuseGiLowResolutionGrid unavailable(int scaleDivisor) {
        return new DiffuseGiLowResolutionGrid(0, 0, Math.max(1, scaleDivisor), 0, 0);
    }

    public static DiffuseGiLowResolutionGrid fromViewport(FrameViewport viewport, int scaleDivisor) {
        if (viewport == null || !viewport.available()) {
            return unavailable(scaleDivisor);
        }
        return fromDimensions(viewport.width(), viewport.height(), scaleDivisor);
    }

    public static DiffuseGiLowResolutionGrid fromDimensions(int sourceWidth, int sourceHeight, int scaleDivisor) {
        int resolvedScaleDivisor = Math.max(1, scaleDivisor);
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return unavailable(resolvedScaleDivisor);
        }
        return new DiffuseGiLowResolutionGrid(
                sourceWidth,
                sourceHeight,
                resolvedScaleDivisor,
                ceilDiv(sourceWidth, resolvedScaleDivisor),
                ceilDiv(sourceHeight, resolvedScaleDivisor)
        );
    }

    public boolean available() {
        return this.sourceWidth > 0 && this.sourceHeight > 0 && this.width > 0 && this.height > 0;
    }

    public int cellCount() {
        return clampToInt((long) this.width * (long) this.height);
    }

    public float sourcePixelsPerCell() {
        if (!this.available()) {
            return 0.0F;
        }
        return (float) ((long) this.sourceWidth * (long) this.sourceHeight) / (float) this.cellCount();
    }

    public String label() {
        if (!this.available()) {
            return "unavailable";
        }
        return this.width + "x" + this.height + " from " + this.sourceWidth + "x" + this.sourceHeight
                + " /" + this.scaleDivisor;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int clampToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.max(0L, value);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
