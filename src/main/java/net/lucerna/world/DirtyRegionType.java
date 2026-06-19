package net.lucerna.world;

public enum DirtyRegionType {
    CHUNK_LOAD(1, true),
    CHUNK_UNLOAD(2, true),
    SECTION_REBUILD(3, true),
    BLOCK_UPDATE(4, true),
    FLUID_UPDATE(5, true),
    EMISSIVE_UPDATE(6, true),
    RESOURCE_PACK_RELOAD(7, false),
    DIMENSION_CHANGE(8, false),
    WEATHER_CHANGE(9, false),
    TIME_OF_DAY_CHANGE(10, false);

    private final int nativeTypeId;
    private final boolean sectionScoped;

    DirtyRegionType(int nativeTypeId, boolean sectionScoped) {
        this.nativeTypeId = nativeTypeId;
        this.sectionScoped = sectionScoped;
    }

    public int nativeTypeId() {
        return this.nativeTypeId;
    }

    public boolean sectionScoped() {
        return this.sectionScoped;
    }
}
