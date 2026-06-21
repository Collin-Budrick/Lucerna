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
    private static final float MAX_BLOCK_LIGHT_LEVEL = 15.0F;
    private static final float MIN_NONZERO_INTENSITY = 0.18F;
    private static final float DIRECT_INTENSITY_SCALE = 2.15F;
    private static final float DIRECT_INTENSITY_SHOULDER = 0.85F;
    private static final float MIN_SURFACE_RADIUS_BLOCKS = 2.0F;
    private static final float LIGHT_LEVEL_RADIUS_SCALE = 1.25F;
    private static final float FULL_LIGHT_RADIUS_BOOST_BLOCKS = 3.0F;
    private static final float NEAR_SURFACE_BOOST_DISTANCE_BLOCKS = 5.0F;
    private static final float NEAR_SURFACE_EXTRA_WEIGHT = 1.35F;
    private static final float DISTANCE_ATTENUATION_SCALE = 0.16F;

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

        float normalizedLight = normalizedBlockLight(entry.blockLightLevel());
        return new DirectEmissiveBlockLight(
                origin.dimension(),
                origin.minBlockX() + entry.localX(),
                origin.minBlockY() + entry.localY(),
                origin.minBlockZ() + entry.localZ(),
                entry.materialId(),
                entry.blockLightLevel(),
                DirectLightColor.materialStableEmissive(entry.materialId(), entry.blockLightLevel()),
                directIntensity(normalizedLight),
                influenceRadius(entry.blockLightLevel(), normalizedLight),
                entry.generation()
        );
    }

    public boolean hasEnergy() {
        return this.intensity > 0.0F && this.color.hasEnergy();
    }

    public float priority() {
        return this.intensity * Math.max(1.0F, this.influenceRadiusBlocks) * this.color.luminance();
    }

    public float centerX() {
        return this.blockX + 0.5F;
    }

    public float centerY() {
        return this.blockY + 0.5F;
    }

    public float centerZ() {
        return this.blockZ + 0.5F;
    }

    public float distanceToReceiver(float receiverX, float receiverY, float receiverZ) {
        if (!Float.isFinite(receiverX) || !Float.isFinite(receiverY) || !Float.isFinite(receiverZ)) {
            return Float.POSITIVE_INFINITY;
        }
        float deltaX = this.centerX() - receiverX;
        float deltaY = this.centerY() - receiverY;
        float deltaZ = this.centerZ() - receiverZ;
        return (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
    }

    public float sourceMaterialEvidenceScore() {
        if (!this.hasEnergy()) {
            return 0.0F;
        }
        return clampUnit((normalizedBlockLight(this.blockLightLevel) * 0.34F)
                + (this.color.luminance() * 0.28F)
                + (clampUnit(this.intensity / 3.25F) * 0.20F)
                + (clampUnit(this.influenceRadiusBlocks / 24.0F) * 0.18F));
    }

    public float receiverDistanceEvidenceScore(float receiverX, float receiverY, float receiverZ) {
        float distanceBlocks = this.distanceToReceiver(receiverX, receiverY, receiverZ);
        if (!Float.isFinite(distanceBlocks)) {
            return 0.0F;
        }
        float radius = Math.max(1.0F, this.influenceRadiusBlocks);
        return clampUnit((this.nearbySurfaceFalloff(distanceBlocks) * 0.48F)
                + (clampUnit(radius / Math.max(1.0F, distanceBlocks)) * 0.30F)
                + (this.sourceMaterialEvidenceScore() * 0.22F));
    }

    public float physicalSpillEvidenceScore(float receiverX, float receiverY, float receiverZ) {
        float distanceBlocks = this.distanceToReceiver(receiverX, receiverY, receiverZ);
        if (!Float.isFinite(distanceBlocks)) {
            return 0.0F;
        }
        return clampUnit((this.receiverDistanceEvidenceScore(receiverX, receiverY, receiverZ) * 0.45F)
                + (this.sourceMaterialEvidenceScore() * 0.25F)
                + (clampUnit(this.nearbySurfaceContribution(distanceBlocks) / 24.0F) * 0.20F)
                + (clampUnit(this.color.luminance()) * 0.10F));
    }

    public float nearbySurfaceFalloff(float distanceBlocks) {
        if (!Float.isFinite(distanceBlocks) || distanceBlocks < 0.0F || !this.hasEnergy()) {
            return 0.0F;
        }
        float radius = Math.max(1.0F, this.influenceRadiusBlocks);
        float normalizedDistance = clampUnit(distanceBlocks / radius);
        float smoothReach = 1.0F - normalizedDistance;
        smoothReach *= smoothReach;
        float distanceAttenuation = 1.0F / (1.0F + (distanceBlocks * distanceBlocks * DISTANCE_ATTENUATION_SCALE));
        float nearSurfaceBoost = 1.0F + (NEAR_SURFACE_EXTRA_WEIGHT * clampUnit(
                (NEAR_SURFACE_BOOST_DISTANCE_BLOCKS - distanceBlocks) / NEAR_SURFACE_BOOST_DISTANCE_BLOCKS
        ));
        return ((0.18F + 0.82F * smoothReach) * distanceAttenuation) * nearSurfaceBoost;
    }

    public float nearbySurfaceContribution(float distanceBlocks) {
        return this.priority() * this.nearbySurfaceFalloff(distanceBlocks);
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

    private static float normalizedBlockLight(int blockLightLevel) {
        return clampUnit(blockLightLevel / MAX_BLOCK_LIGHT_LEVEL);
    }

    private static float directIntensity(float normalizedLight) {
        if (normalizedLight <= 0.0F) {
            return 0.0F;
        }
        return MIN_NONZERO_INTENSITY
                + (normalizedLight * DIRECT_INTENSITY_SCALE)
                + (normalizedLight * normalizedLight * DIRECT_INTENSITY_SHOULDER);
    }

    private static float influenceRadius(int blockLightLevel, float normalizedLight) {
        if (blockLightLevel <= 0) {
            return 0.0F;
        }
        return Math.max(
                MIN_SURFACE_RADIUS_BLOCKS,
                (blockLightLevel * LIGHT_LEVEL_RADIUS_SCALE) + (normalizedLight * FULL_LIGHT_RADIUS_BOOST_BLOCKS)
        );
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
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
