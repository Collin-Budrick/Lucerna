package net.lucerna.render.frame;

public record FrameMatrix4f(
        float m00,
        float m01,
        float m02,
        float m03,
        float m10,
        float m11,
        float m12,
        float m13,
        float m20,
        float m21,
        float m22,
        float m23,
        float m30,
        float m31,
        float m32,
        float m33
) {
    public FrameMatrix4f {
        m00 = finiteOrZero(m00);
        m01 = finiteOrZero(m01);
        m02 = finiteOrZero(m02);
        m03 = finiteOrZero(m03);
        m10 = finiteOrZero(m10);
        m11 = finiteOrZero(m11);
        m12 = finiteOrZero(m12);
        m13 = finiteOrZero(m13);
        m20 = finiteOrZero(m20);
        m21 = finiteOrZero(m21);
        m22 = finiteOrZero(m22);
        m23 = finiteOrZero(m23);
        m30 = finiteOrZero(m30);
        m31 = finiteOrZero(m31);
        m32 = finiteOrZero(m32);
        m33 = finiteOrZero(m33);
    }

    public static FrameMatrix4f identity() {
        return new FrameMatrix4f(
                1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        );
    }

    public static FrameMatrix4f zero() {
        return new FrameMatrix4f(
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.0F
        );
    }

    public static FrameMatrix4f fromRowMajor(float[] values) {
        if (values == null || values.length != 16) {
            return zero();
        }
        return new FrameMatrix4f(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7],
                values[8], values[9], values[10], values[11],
                values[12], values[13], values[14], values[15]
        );
    }

    public float[] toRowMajorArray() {
        return new float[]{
                this.m00, this.m01, this.m02, this.m03,
                this.m10, this.m11, this.m12, this.m13,
                this.m20, this.m21, this.m22, this.m23,
                this.m30, this.m31, this.m32, this.m33
        };
    }

    public boolean finite() {
        return Float.isFinite(this.m00)
                && Float.isFinite(this.m01)
                && Float.isFinite(this.m02)
                && Float.isFinite(this.m03)
                && Float.isFinite(this.m10)
                && Float.isFinite(this.m11)
                && Float.isFinite(this.m12)
                && Float.isFinite(this.m13)
                && Float.isFinite(this.m20)
                && Float.isFinite(this.m21)
                && Float.isFinite(this.m22)
                && Float.isFinite(this.m23)
                && Float.isFinite(this.m30)
                && Float.isFinite(this.m31)
                && Float.isFinite(this.m32)
                && Float.isFinite(this.m33);
    }

    private static float finiteOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return value;
    }
}
