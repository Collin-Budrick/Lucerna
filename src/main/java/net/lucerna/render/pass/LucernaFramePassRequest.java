package net.lucerna.render.pass;

import java.util.Locale;

public record LucernaFramePassRequest(
        LucernaFramePassKind kind,
        long frameIndex,
        LucernaFramePassTarget target,
        float red,
        float green,
        float blue,
        float alpha,
        String label
) {
    public LucernaFramePassRequest {
        if (kind == null) {
            kind = LucernaFramePassKind.NO_OP;
        }
        frameIndex = Math.max(0L, frameIndex);
        if (target == null) {
            target = LucernaFramePassTarget.absent("No frame pass target was supplied.");
        }
        red = normalizeColor(red, 0.0F);
        green = normalizeColor(green, 0.0F);
        blue = normalizeColor(blue, 0.0F);
        alpha = normalizeColor(alpha, 1.0F);
        if (label == null || label.isBlank()) {
            label = kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
        } else {
            label = label.trim();
        }
    }

    public static LucernaFramePassRequest noOp(long frameIndex) {
        return noOp(frameIndex, LucernaFramePassTarget.absent("No frame pass target was supplied."));
    }

    public static LucernaFramePassRequest noOp(long frameIndex, LucernaFramePassTarget target) {
        return new LucernaFramePassRequest(
                LucernaFramePassKind.NO_OP,
                frameIndex,
                target,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                "lucerna-no-op"
        );
    }

    public static LucernaFramePassRequest flatComposite(
            long frameIndex,
            LucernaFramePassTarget target,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        return new LucernaFramePassRequest(
                LucernaFramePassKind.FLAT_COMPOSITE,
                frameIndex,
                target,
                red,
                green,
                blue,
                alpha,
                "lucerna-flat-composite"
        );
    }

    public boolean hasExplicitFrameIndex() {
        return this.frameIndex > 0L;
    }

    public boolean matchesFrame(long activeFrameIndex) {
        return !this.hasExplicitFrameIndex() || this.frameIndex == activeFrameIndex;
    }

    public boolean hasTarget() {
        return this.target.available();
    }

    public boolean targetSafeForAttachment() {
        return this.target.safeForAttachment();
    }

    private static float normalizeColor(float value, float fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
