package net.lucerna.render.frame;

public record FrameCameraMatrices(
        FrameMatrix4f view,
        FrameMatrix4f projection,
        FrameMatrix4f viewProjection,
        FrameMatrix4f inverseViewProjection,
        boolean available
) {
    public FrameCameraMatrices {
        if (view == null) {
            view = FrameMatrix4f.identity();
        }
        if (projection == null) {
            projection = FrameMatrix4f.identity();
        }
        if (viewProjection == null) {
            viewProjection = FrameMatrix4f.identity();
        }
        if (inverseViewProjection == null) {
            inverseViewProjection = FrameMatrix4f.identity();
        }
    }

    public static FrameCameraMatrices unavailable() {
        return new FrameCameraMatrices(
                FrameMatrix4f.identity(),
                FrameMatrix4f.identity(),
                FrameMatrix4f.identity(),
                FrameMatrix4f.identity(),
                false
        );
    }

    public boolean hasRequiredMatrices() {
        return this.available
                && this.view.finite()
                && this.projection.finite()
                && this.viewProjection.finite()
                && this.inverseViewProjection.finite();
    }

    public String stateLabel() {
        return this.hasRequiredMatrices() ? "available" : "placeholder";
    }
}
