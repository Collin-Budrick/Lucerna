package net.lucerna.upload;

import net.lucerna.world.DirtyRegion;

import java.util.Objects;

public record NativeDirtyRegionUpload(
        int typeId,
        String typeName,
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean sectionScoped,
        long generation
) {
    public NativeDirtyRegionUpload {
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(dimension, "dimension");
        if (typeId <= 0) {
            throw new IllegalArgumentException("typeId must be positive");
        }
        if (generation <= 0) {
            throw new IllegalArgumentException("generation must be positive");
        }
    }

    public static NativeDirtyRegionUpload from(DirtyRegion dirtyRegion) {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        return new NativeDirtyRegionUpload(
                dirtyRegion.type().nativeTypeId(),
                dirtyRegion.type().name(),
                dirtyRegion.dimension(),
                dirtyRegion.sectionX(),
                dirtyRegion.sectionY(),
                dirtyRegion.sectionZ(),
                dirtyRegion.sectionScoped(),
                dirtyRegion.generation()
        );
    }
}
