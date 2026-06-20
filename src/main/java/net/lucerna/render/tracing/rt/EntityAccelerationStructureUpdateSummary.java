package net.lucerna.render.tracing.rt;

import java.util.List;
import java.util.Objects;

public record EntityAccelerationStructureUpdateSummary(
        long frameIndex,
        long transformGeneration,
        int entityCount,
        int changedTransformCount,
        int visibleEntityCount,
        int blasGeometryCount,
        int blasPrimitiveCount,
        int entityMovementMarkerCount,
        long transformUpdateGeneration,
        int spawnReadyEntityCount,
        int despawnReadyEntityCount,
        List<EntityTransformUpdate> transformUpdates,
        String sourceLabel,
        String fallbackReason,
        String spawnDespawnReadiness,
        String boundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Entity AS update summary is Java metadata only; native BLAS/TLAS execution is not proven.";
    private static final String DEFAULT_FALLBACK_REASON =
            "Entity RT stress metadata is Java-side only; hardware RT entity execution is not proven.";
    private static final String DEFAULT_SPAWN_DESPAWN_READINESS =
            "Spawn/despawn entity AS readiness is metadata-only; native TLAS rebuild execution is not proven.";

    public EntityAccelerationStructureUpdateSummary(
            long frameIndex,
            long transformGeneration,
            int entityCount,
            int changedTransformCount,
            int visibleEntityCount,
            int blasGeometryCount,
            int blasPrimitiveCount,
            List<EntityTransformUpdate> transformUpdates,
            String sourceLabel,
            String boundary
    ) {
        this(
                frameIndex,
                transformGeneration,
                entityCount,
                changedTransformCount,
                visibleEntityCount,
                blasGeometryCount,
                blasPrimitiveCount,
                0,
                transformGeneration,
                0,
                0,
                transformUpdates,
                sourceLabel,
                DEFAULT_FALLBACK_REASON,
                DEFAULT_SPAWN_DESPAWN_READINESS,
                boundary
        );
    }

    public EntityAccelerationStructureUpdateSummary {
        frameIndex = Math.max(0L, frameIndex);
        transformGeneration = Math.max(0L, transformGeneration);
        transformUpdates = transformUpdates == null ? List.of() : List.copyOf(transformUpdates);
        for (EntityTransformUpdate update : transformUpdates) {
            Objects.requireNonNull(update, "transformUpdates must not contain null entries");
        }
        entityCount = Math.max(Math.max(0, entityCount), transformUpdates.size());
        changedTransformCount = clamp(changedTransformCount, entityCount);
        visibleEntityCount = clamp(visibleEntityCount, entityCount);
        blasGeometryCount = Math.max(0, blasGeometryCount);
        blasPrimitiveCount = Math.max(0, blasPrimitiveCount);
        entityMovementMarkerCount = clamp(
                Math.max(entityMovementMarkerCount, countMovementMarkers(transformUpdates)),
                entityCount
        );
        transformUpdateGeneration = Math.max(Math.max(0L, transformUpdateGeneration), transformGeneration);
        spawnReadyEntityCount = clamp(Math.max(spawnReadyEntityCount, countSpawnReady(transformUpdates)), entityCount);
        despawnReadyEntityCount = clamp(Math.max(despawnReadyEntityCount, countDespawnReady(transformUpdates)), entityCount);
        sourceLabel = clean(sourceLabel, "unwired-entity-extraction");
        fallbackReason = clean(fallbackReason, DEFAULT_FALLBACK_REASON);
        spawnDespawnReadiness = clean(spawnDespawnReadiness, DEFAULT_SPAWN_DESPAWN_READINESS);
        boundary = clean(boundary, DEFAULT_BOUNDARY);
    }

    public static EntityAccelerationStructureUpdateSummary empty(long frameIndex, String sourceLabel) {
        return new EntityAccelerationStructureUpdateSummary(
                frameIndex,
                0L,
                0,
                0,
                0,
                0,
                0,
                0,
                0L,
                0,
                0,
                List.of(),
                sourceLabel,
                DEFAULT_FALLBACK_REASON,
                DEFAULT_SPAWN_DESPAWN_READINESS,
                DEFAULT_BOUNDARY
        );
    }

    public boolean hasEntityInput() {
        return this.entityCount > 0 || !this.transformUpdates.isEmpty();
    }

    public String summary() {
        return "entities=" + this.entityCount
                + ",changedTransforms=" + this.changedTransformCount
                + ",visible=" + this.visibleEntityCount
                + ",blasGeometry=" + this.blasGeometryCount
                + ",blasPrimitives=" + this.blasPrimitiveCount
                + ",movementMarkers=" + this.entityMovementMarkerCount
                + ",generation=" + this.transformGeneration
                + ",transformUpdateGeneration=" + this.transformUpdateGeneration
                + ",spawnReady=" + this.spawnReadyEntityCount
                + ",despawnReady=" + this.despawnReadyEntityCount
                + ",fallbackReason=" + this.fallbackReason
                + ",spawnDespawnReadiness=" + this.spawnDespawnReadiness
                + ",source=" + this.sourceLabel;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private static int countMovementMarkers(List<EntityTransformUpdate> transformUpdates) {
        int count = 0;
        for (EntityTransformUpdate update : transformUpdates) {
            if (update.movementMarker()) {
                count++;
            }
        }
        return count;
    }

    private static int countSpawnReady(List<EntityTransformUpdate> transformUpdates) {
        int count = 0;
        for (EntityTransformUpdate update : transformUpdates) {
            if (update.spawnReady()) {
                count++;
            }
        }
        return count;
    }

    private static int countDespawnReady(List<EntityTransformUpdate> transformUpdates) {
        int count = 0;
        for (EntityTransformUpdate update : transformUpdates) {
            if (update.despawnReady()) {
                count++;
            }
        }
        return count;
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
