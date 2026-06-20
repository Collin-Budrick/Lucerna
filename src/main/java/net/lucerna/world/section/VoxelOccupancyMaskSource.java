package net.lucerna.world.section;

public enum VoxelOccupancyMaskSource {
    NONE(false),
    JAVA_SECTION_SCAN(false),
    NATIVE_UPLOAD(false),
    METADATA_ONLY(true);

    private final boolean metadataOnly;

    VoxelOccupancyMaskSource(boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
    }

    public boolean metadataOnly() {
        return this.metadataOnly;
    }
}
