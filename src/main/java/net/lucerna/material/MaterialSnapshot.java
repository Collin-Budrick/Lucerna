package net.lucerna.material;

import java.util.List;
import java.util.Objects;

public record MaterialSnapshot(
        long generation,
        List<LucernaMaterial> materials
) {
    public MaterialSnapshot {
        Objects.requireNonNull(materials, "materials");
        materials = List.copyOf(materials);
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        long maxMaterialGeneration = materials.stream()
                .mapToLong(LucernaMaterial::generation)
                .max()
                .orElse(0L);
        if (generation < maxMaterialGeneration) {
            throw new IllegalArgumentException("snapshot generation must include all material payload generations");
        }
    }

    public static MaterialSnapshot empty() {
        return new MaterialSnapshot(0, List.of());
    }

    public boolean isEmpty() {
        return this.materials.isEmpty();
    }

    public int materialCount() {
        return this.materials.size();
    }
}
