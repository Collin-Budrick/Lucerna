package net.lucerna.upload;

import net.lucerna.world.DirtyRegion;

import java.util.Objects;

public record NativeDirtyRegionHandoff(
        int typeId,
        String typeName,
        String dimension,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean sectionScoped,
        long dirtyRegionGeneration
) {
    public NativeDirtyRegionHandoff {
        if (typeId <= 0) {
            throw new IllegalArgumentException("typeId must be positive");
        }
        requireText(typeName, "typeName");
        requireText(dimension, "dimension");
        if (dirtyRegionGeneration <= 0) {
            throw new IllegalArgumentException("dirtyRegionGeneration must be positive");
        }
    }

    public static NativeDirtyRegionHandoff from(DirtyRegion dirtyRegion) {
        Objects.requireNonNull(dirtyRegion, "dirtyRegion");
        return new NativeDirtyRegionHandoff(
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

    public static NativeDirtyRegionHandoff from(NativeDirtyRegionUpload dirtyRegionUpload) {
        Objects.requireNonNull(dirtyRegionUpload, "dirtyRegionUpload");
        return new NativeDirtyRegionHandoff(
                dirtyRegionUpload.typeId(),
                dirtyRegionUpload.typeName(),
                dirtyRegionUpload.dimension(),
                dirtyRegionUpload.sectionX(),
                dirtyRegionUpload.sectionY(),
                dirtyRegionUpload.sectionZ(),
                dirtyRegionUpload.sectionScoped(),
                dirtyRegionUpload.generation()
        );
    }

    public boolean matchesSection(String dimension, int sectionX, int sectionY, int sectionZ) {
        requireText(dimension, "dimension");
        if (!this.sectionScoped) {
            return true;
        }
        if (!this.dimension.equals(dimension)) {
            return false;
        }
        return this.sectionX == sectionX && this.sectionY == sectionY && this.sectionZ == sectionZ;
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
