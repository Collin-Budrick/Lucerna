package net.lucerna.render.culling;

public enum ClusterVisibilityClassification {
    VISIBLE,
    FRUSTUM_CULLED,
    OFFSCREEN,
    OCCLUSION_PLACEHOLDER,
    MISSING_METADATA
}
