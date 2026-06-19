package net.lucerna.render;

public record GBufferDescriptor(
        int width,
        int height,
        boolean hasDepth,
        boolean hasNormals,
        boolean hasAlbedo,
        boolean hasMaterialIds,
        boolean hasMotionVectors
) {
    public static GBufferDescriptor empty() {
        return new GBufferDescriptor(0, 0, false, false, false, false, false);
    }
}
