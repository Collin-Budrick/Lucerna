package net.lucerna.render;

import net.lucerna.render.gbuffer.GBufferTargetContract;

public record GBufferDescriptor(
        int width,
        int height,
        boolean hasDepth,
        boolean hasNormals,
        boolean hasAlbedo,
        boolean hasMaterialIds,
        boolean hasEmissive,
        boolean hasMotionVectors
) {
    public GBufferDescriptor(
            int width,
            int height,
            boolean hasDepth,
            boolean hasNormals,
            boolean hasAlbedo,
            boolean hasMaterialIds,
            boolean hasMotionVectors
    ) {
        this(width, height, hasDepth, hasNormals, hasAlbedo, hasMaterialIds, false, hasMotionVectors);
    }

    public GBufferDescriptor {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("G-buffer dimensions must be non-negative");
        }
    }

    public static GBufferDescriptor empty() {
        return new GBufferDescriptor(0, 0, false, false, false, false, false, false);
    }

    public static GBufferDescriptor lucernaMain(int width, int height) {
        return new GBufferDescriptor(width, height, true, true, true, true, true, true);
    }

    public GBufferTargetContract targetContract() {
        return GBufferTargetContract.lucernaMain();
    }

    public boolean hasAllLucernaMainAttachments() {
        return this.hasDepth
                && this.hasNormals
                && this.hasAlbedo
                && this.hasMaterialIds
                && this.hasEmissive
                && this.hasMotionVectors;
    }
}
