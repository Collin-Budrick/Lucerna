package net.lucerna.material;

public final class MaterialFlags {
    public static final int OPAQUE = 1;
    public static final int EMISSIVE = 1 << 1;
    public static final int WATER = 1 << 2;
    public static final int GLASS = 1 << 3;
    public static final int LEAVES = 1 << 4;
    public static final int TRANSLUCENT = 1 << 5;
    public static final int FLUID = 1 << 6;
    public static final int CUTOUT = 1 << 7;

    private MaterialFlags() {
    }

    public static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
