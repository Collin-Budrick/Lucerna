package net.lucerna.render.frame;

public record FrameJitter(
        float x,
        float y,
        int sequenceIndex,
        int sequenceLength,
        boolean enabled
) {
    public FrameJitter {
        x = finiteOrZero(x);
        y = finiteOrZero(y);
        sequenceLength = Math.max(0, sequenceLength);
        sequenceIndex = sequenceLength == 0 ? 0 : Math.floorMod(sequenceIndex, sequenceLength);
        if (!enabled) {
            x = 0.0F;
            y = 0.0F;
        }
    }

    public static FrameJitter disabled() {
        return new FrameJitter(0.0F, 0.0F, 0, 0, false);
    }

    public boolean available() {
        return !this.enabled || this.sequenceLength > 0;
    }

    public String label() {
        if (!this.enabled) {
            return "disabled";
        }
        return this.sequenceIndex + "/" + this.sequenceLength + " (" + this.x + "," + this.y + ")";
    }

    private static float finiteOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return value;
    }
}
