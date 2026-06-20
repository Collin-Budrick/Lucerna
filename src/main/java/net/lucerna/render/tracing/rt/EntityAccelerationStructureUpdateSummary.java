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
        List<EntityTransformUpdate> transformUpdates,
        String sourceLabel,
        String boundary
) {
    private static final String DEFAULT_BOUNDARY =
            "Entity AS update summary is Java metadata only; native BLAS/TLAS execution is not proven.";

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
        sourceLabel = clean(sourceLabel, "unwired-entity-extraction");
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
                List.of(),
                sourceLabel,
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
                + ",generation=" + this.transformGeneration
                + ",source=" + this.sourceLabel;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
