package net.lucerna.render.lighting.direct;

import net.lucerna.world.section.ChunkSectionVoxelSnapshot;
import net.lucerna.world.section.SectionEmissiveEntryMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record DirectEmissiveBlockListPlan(
        long generation,
        List<DirectEmissiveBlockLight> lights,
        int maxSelectedLights,
        boolean sortedByPriority
) {
    public DirectEmissiveBlockListPlan {
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must be non-negative");
        }
        Objects.requireNonNull(lights, "lights");
        lights = List.copyOf(lights);
        for (DirectEmissiveBlockLight light : lights) {
            Objects.requireNonNull(light, "lights must not contain null entries");
        }
        if (maxSelectedLights < 0) {
            throw new IllegalArgumentException("maxSelectedLights must be non-negative");
        }
    }

    public static DirectEmissiveBlockListPlan empty() {
        return new DirectEmissiveBlockListPlan(0L, List.of(), 0, true);
    }

    public static DirectEmissiveBlockListPlan fromSectionSnapshots(
            List<ChunkSectionVoxelSnapshot> snapshots,
            int maxSelectedLights
    ) {
        Objects.requireNonNull(snapshots, "snapshots");
        List<DirectEmissiveBlockLight> lights = new ArrayList<>();
        long generation = 0L;

        for (ChunkSectionVoxelSnapshot snapshot : snapshots) {
            Objects.requireNonNull(snapshot, "snapshots must not contain null entries");
            generation = Math.max(generation, snapshot.generation().emissiveGeneration());
            for (SectionEmissiveEntryMetadata entry : snapshot.emissiveEntries()) {
                DirectEmissiveBlockLight light = DirectEmissiveBlockLight.fromSectionEntry(snapshot.origin(), entry);
                lights.add(light);
                generation = Math.max(generation, light.generation());
            }
        }

        Comparator<DirectEmissiveBlockLight> priorityOrder = Comparator
                .comparingDouble(DirectEmissiveBlockLight::priority)
                .reversed()
                .thenComparing(DirectEmissiveBlockLight::stableKey);
        lights.sort(priorityOrder);
        return new DirectEmissiveBlockListPlan(generation, lights, maxSelectedLights, true);
    }

    public boolean hasCandidates() {
        return !this.lights.isEmpty();
    }

    public int candidateCount() {
        return this.lights.size();
    }

    public List<DirectEmissiveBlockLight> selectedLights() {
        if (this.maxSelectedLights == 0 || this.lights.isEmpty()) {
            return List.of();
        }
        if (this.lights.size() <= this.maxSelectedLights) {
            return this.lights;
        }
        return List.copyOf(this.lights.subList(0, this.maxSelectedLights));
    }

    public int selectedLightCount() {
        return this.selectedLights().size();
    }

    public boolean hasSelectedLights() {
        return this.selectedLightCount() > 0;
    }

    public long maxLightGeneration() {
        return this.lights.stream()
                .mapToLong(DirectEmissiveBlockLight::generation)
                .max()
                .orElse(0L);
    }
}
