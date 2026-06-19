package net.lucerna.material;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class MaterialRegistry {
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicLong generation = new AtomicLong();
    private final Map<MaterialKey, LucernaMaterial> materials = new LinkedHashMap<>();

    public synchronized LucernaMaterial getOrCreate(MaterialKey key) {
        MaterialKey materialKey = Objects.requireNonNull(key, "key");
        return this.materials.computeIfAbsent(materialKey, ignored -> {
            int flags = materialKey.flags();
            float emissiveStrength = (flags & MaterialFlags.EMISSIVE) != 0 ? materialKey.emissiveStrength() : 0.0f;
            long materialGeneration = this.generation.incrementAndGet();
            return new LucernaMaterial(
                    this.nextId.getAndIncrement(),
                    materialGeneration,
                    materialKey.blockId(),
                    materialKey.faceId(),
                    materialKey.albedoTextureIndex(),
                    materialKey.roughness(),
                    materialKey.metalness(),
                    materialKey.emissiveRed(),
                    materialKey.emissiveGreen(),
                    materialKey.emissiveBlue(),
                    emissiveStrength,
                    flags
            );
        });
    }

    public synchronized Collection<LucernaMaterial> snapshot() {
        return List.copyOf(this.materials.values());
    }

    public synchronized MaterialSnapshot snapshotWithGeneration() {
        return new MaterialSnapshot(this.generation.get(), List.copyOf(this.materials.values()));
    }

    public synchronized MaterialSnapshot snapshotUpdatesAfter(long previousGeneration) {
        if (previousGeneration < 0) {
            throw new IllegalArgumentException("previousGeneration must be non-negative");
        }

        List<LucernaMaterial> updatedMaterials = this.materials.values().stream()
                .filter(material -> material.generation() > previousGeneration)
                .toList();
        return new MaterialSnapshot(this.generation.get(), updatedMaterials);
    }

    public long currentGeneration() {
        return this.generation.get();
    }

    public synchronized int materialCount() {
        return this.materials.size();
    }
}
