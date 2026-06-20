package net.lucerna.render.lighting.restir.reuse;

public enum DirectLightReuseInvalidationReason {
    NONE("none"),
    CAMERA_CUT("camera_cut"),
    CAMERA_MOVED_TOO_FAR("camera_moved_too_far"),
    PROJECTION_CHANGED("projection_changed"),
    RESOLUTION_CHANGED("resolution_changed"),
    WORLD_GENERATION_CHANGED("world_generation_changed"),
    MATERIAL_GENERATION_CHANGED("material_generation_changed"),
    LIGHT_GENERATION_CHANGED("light_generation_changed"),
    DIRTY_REGION_OVERLAP("dirty_region_overlap"),
    RESERVOIR_LAYOUT_CHANGED("reservoir_layout_changed"),
    MISSING_PREVIOUS_FRAME("missing_previous_frame"),
    MISSING_NEIGHBOR_RESERVOIRS("missing_neighbor_reservoirs"),
    REUSE_DISABLED("reuse_disabled"),
    STATUS_ONLY_NO_EXECUTION("status_only_no_execution");

    private final String stableId;

    DirectLightReuseInvalidationReason(String stableId) {
        this.stableId = stableId;
    }

    public String stableId() {
        return this.stableId;
    }
}
