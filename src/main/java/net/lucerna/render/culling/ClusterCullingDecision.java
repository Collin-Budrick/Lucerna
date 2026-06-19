package net.lucerna.render.culling;

public record ClusterCullingDecision(
        long clusterId,
        String sectionKey,
        ClusterVisibilityClassification classification,
        boolean visible,
        boolean contributesIndirectDraw,
        String reason
) {
    public ClusterCullingDecision {
        if (clusterId < 0L) {
            throw new IllegalArgumentException("clusterId must be non-negative");
        }
        if (sectionKey == null || sectionKey.isBlank()) {
            sectionKey = "unknown";
        }
        if (classification == null) {
            classification = ClusterVisibilityClassification.MISSING_METADATA;
        }
        if (reason == null || reason.isBlank()) {
            reason = "unreported";
        }
        visible = visible && classification == ClusterVisibilityClassification.VISIBLE;
        contributesIndirectDraw = contributesIndirectDraw && visible;
    }
}
