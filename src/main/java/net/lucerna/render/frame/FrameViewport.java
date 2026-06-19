package net.lucerna.render.frame;

public record FrameViewport(int width, int height) {
    public static final FrameViewport UNAVAILABLE = new FrameViewport(0, 0);

    public FrameViewport {
        width = Math.max(0, width);
        height = Math.max(0, height);
    }

    public boolean available() {
        return this.width > 0 && this.height > 0;
    }

    public float aspectRatio() {
        if (!this.available()) {
            return 0.0F;
        }
        return (float) this.width / (float) this.height;
    }

    public String label() {
        if (!this.available()) {
            return "unavailable";
        }
        return this.width + "x" + this.height;
    }
}
