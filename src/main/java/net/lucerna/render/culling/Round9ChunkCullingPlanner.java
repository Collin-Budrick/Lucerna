package net.lucerna.render.culling;

import java.util.ArrayList;
import java.util.List;

public final class Round9ChunkCullingPlanner {
    private static final double FRUSTUM_PADDING_DEGREES = 12.0D;

    private Round9ChunkCullingPlanner() {
    }

    public static ClusterCullingSummary summarize(
            ChunkClusterMetadataSnapshot snapshot,
            CullingCameraMetadata camera
    ) {
        if (snapshot == null || snapshot.clusterCount() == 0) {
            return ClusterCullingSummary.unavailable("cluster metadata unavailable");
        }

        List<ClusterCullingDecision> decisions = new ArrayList<>(snapshot.clusterCount());
        int visible = 0;
        int offscreen = 0;
        int frustumCulled = 0;
        int occlusionPlaceholder = 0;
        int missingMetadata = 0;
        int primitiveCount = 0;

        for (ChunkClusterMetadata cluster : snapshot.clusters()) {
            ClusterCullingDecision decision = classify(cluster, camera);
            decisions.add(decision);
            switch (decision.classification()) {
                case VISIBLE -> {
                    visible++;
                    primitiveCount += cluster.primitiveCount();
                }
                case OFFSCREEN -> offscreen++;
                case FRUSTUM_CULLED -> frustumCulled++;
                case OCCLUSION_PLACEHOLDER -> occlusionPlaceholder++;
                case MISSING_METADATA -> missingMetadata++;
            }
        }

        IndirectDrawListStats indirect = IndirectDrawListStats.metadataOnly(
                visible,
                primitiveCount,
                snapshot.generation()
        );
        int culled = snapshot.clusterCount() - visible;
        int frustumCandidates = snapshot.clusterCount() - missingMetadata;
        return new ClusterCullingSummary(
                snapshot.clusterCount(),
                visible,
                culled,
                offscreen,
                frustumCulled,
                occlusionPlaceholder,
                missingMetadata,
                snapshot.uploadBytes(),
                false,
                false,
                "gpu-dispatch-visibility-buffer-occlusion-query",
                "actual GPU culling not executed; using conservative Java metadata classification",
                frustumCandidates,
                false,
                false,
                "conservative-cpu-status",
                snapshot.generationSummary(),
                indirect,
                decisions
        );
    }

    private static ClusterCullingDecision classify(ChunkClusterMetadata cluster, CullingCameraMetadata camera) {
        if (cluster == null) {
            return new ClusterCullingDecision(
                    0L,
                    "unknown",
                    ClusterVisibilityClassification.MISSING_METADATA,
                    false,
                    false,
                    "cluster record missing"
            );
        }
        if (!cluster.hasBounds() || camera == null) {
            return new ClusterCullingDecision(
                    cluster.clusterId(),
                    cluster.sectionKey(),
                    ClusterVisibilityClassification.MISSING_METADATA,
                    false,
                    false,
                    camera == null ? "camera metadata missing" : "cluster bounds missing"
            );
        }

        ClusterBounds bounds = cluster.bounds();
        double dx = bounds.centerX() - camera.positionX();
        double dy = bounds.centerY() - camera.positionY();
        double dz = bounds.centerZ() - camera.positionZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double maxDistance = camera.maxDistanceBlocks();
        if (maxDistance > 0.0D && distanceSquared > maxDistance * maxDistance) {
            return decision(cluster, ClusterVisibilityClassification.OFFSCREEN, "outside max culling distance");
        }

        double distance = Math.sqrt(distanceSquared);
        if (distance > 0.000001D) {
            double facing = ((dx / distance) * camera.forwardX())
                    + ((dy / distance) * camera.forwardY())
                    + ((dz / distance) * camera.forwardZ());
            if (facing < camera.halfFovCosine(FRUSTUM_PADDING_DEGREES)) {
                return decision(cluster, ClusterVisibilityClassification.FRUSTUM_CULLED, "outside padded frustum cone");
            }
        }

        if (cluster.occlusionCandidate()) {
            return decision(
                    cluster,
                    ClusterVisibilityClassification.OCCLUSION_PLACEHOLDER,
                    "occlusion classification placeholder; no terrain rendering change"
            );
        }
        return decision(cluster, ClusterVisibilityClassification.VISIBLE, "visible metadata-only cluster");
    }

    private static ClusterCullingDecision decision(
            ChunkClusterMetadata cluster,
            ClusterVisibilityClassification classification,
            String reason
    ) {
        boolean visible = classification == ClusterVisibilityClassification.VISIBLE;
        return new ClusterCullingDecision(
                cluster.clusterId(),
                cluster.sectionKey(),
                classification,
                visible,
                visible,
                reason
        );
    }
}
