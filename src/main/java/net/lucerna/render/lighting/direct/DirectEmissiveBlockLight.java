package net.lucerna.render.lighting.direct;

import net.lucerna.world.section.ChunkSectionOrigin;
import net.lucerna.world.section.SectionEmissiveEntryMetadata;

import java.util.Objects;

public record DirectEmissiveBlockLight(
        String dimension,
        int blockX,
        int blockY,
        int blockZ,
        int materialId,
        int blockLightLevel,
        DirectLightColor color,
        float intensity,
        float influenceRadiusBlocks,
        long generation
) {
    public DirectEmissiveBlockLight {
        dimension = cleanDimension(dimension);
        if (materialId <= 0) {
            throw new IllegalArgumentException("materialId must be positive");
        }
        if (blockLightLevel < 0 || blockLightLevel > 15) {
            throw new IllegalArgumentException("blockLightLevel must be between 0 and 15");
        }
        Objects.requireNonNull(color, "color");
        requireNonNegativeFinite(intensity, "intensity");
        requireNonNegativeFinite(influenceRadiusBlocks, "influenceRadiusBlocks");
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
    }

    public static DirectEmissiveBlockLight fromSectionEntry(
            ChunkSectionOrigin origin,
            SectionEmissiveEntryMetadata entry
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(entry, "entry");

        float normalizedLight = entry.blockLightLevel() / 15.0F;
        return new DirectEmissiveBlockLight(
                origin.dimension(),
                origin.minBlockX() + entry.localX(),
                origin.minBlockY() + entry.localY(),
                origin.minBlockZ() + entry.localZ(),
                entry.materialId(),
                entry.blockLightLevel(),
                DirectLightColor.warmEmissive(),
                normalizedLight * normalizedLight,
                Math.max(1.0F, entry.blockLightLevel()),
                entry.generation()
        );
    }

    public boolean hasEnergy() {
        return this.intensity > 0.0F && this.color.hasEnergy();
    }

    public float priority() {
        return this.intensity * Math.max(1.0F, this.influenceRadiusBlocks) * this.color.luminance();
    }

    public String stableKey() {
        return this.dimension + ":" + this.blockX + "," + this.blockY + "," + this.blockZ + ":" + this.materialId;
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        if (value < 0.0F) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String cleanDimension(String value) {
        Objects.requireNonNull(value, "dimension");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        return value;
    }
}
