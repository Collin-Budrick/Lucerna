package net.lucerna.render.voxel;

public enum VoxelTraversalStatus {
    NOT_RUN,
    HIT,
    MISS,
    BUDGET_EXHAUSTED,
    MISSING_SECTION_DATA,
    INVALID_REQUEST
}
