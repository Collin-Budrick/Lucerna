package net.lucerna.world.section;

import java.util.List;
import java.util.Objects;

public record SectionMaterialPaletteReference(
        long materialGeneration,
        int paletteOffset,
        List<Integer> materialIds
) {
    public SectionMaterialPaletteReference {
        Objects.requireNonNull(materialIds, "materialIds");
        materialIds = List.copyOf(materialIds);
        if (materialGeneration < 0) {
            throw new IllegalArgumentException("materialGeneration must be non-negative");
        }
        if (paletteOffset < 0) {
            throw new IllegalArgumentException("paletteOffset must be non-negative");
        }
        for (Integer materialId : materialIds) {
            Objects.requireNonNull(materialId, "materialIds must not contain null entries");
            if (materialId <= 0) {
                throw new IllegalArgumentException("materialIds must contain positive ids");
            }
        }
    }

    public static SectionMaterialPaletteReference empty() {
        return new SectionMaterialPaletteReference(0, 0, List.of());
    }

    public int paletteSize() {
        return this.materialIds.size();
    }

    public boolean hasPalette() {
        return !this.materialIds.isEmpty();
    }

    public int[] materialIdArray() {
        int[] ids = new int[this.materialIds.size()];
        for (int index = 0; index < this.materialIds.size(); index++) {
            ids[index] = this.materialIds.get(index);
        }
        return ids;
    }
}
