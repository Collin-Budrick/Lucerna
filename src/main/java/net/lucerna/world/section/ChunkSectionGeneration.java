package net.lucerna.world.section;

public record ChunkSectionGeneration(
        long sectionGeneration,
        long materialGeneration,
        long occupancyGeneration,
        long emissiveGeneration
) {
    public ChunkSectionGeneration {
        if (sectionGeneration < 0 || materialGeneration < 0 || occupancyGeneration < 0 || emissiveGeneration < 0) {
            throw new IllegalArgumentException("generations must be non-negative");
        }
    }

    public static ChunkSectionGeneration empty() {
        return new ChunkSectionGeneration(0, 0, 0, 0);
    }

    public long combinedGeneration() {
        return Math.max(
                Math.max(this.sectionGeneration, this.materialGeneration),
                Math.max(this.occupancyGeneration, this.emissiveGeneration)
        );
    }

    public boolean isEmpty() {
        return this.combinedGeneration() == 0;
    }
}
