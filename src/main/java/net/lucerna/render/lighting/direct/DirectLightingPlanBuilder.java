package net.lucerna.render.lighting.direct;

import net.lucerna.render.frame.LucernaFrameConstants;
import net.lucerna.world.section.ChunkSectionVoxelSnapshot;

import java.util.List;
import java.util.Objects;

public final class DirectLightingPlanBuilder {
    private Long frameIndex;
    private DirectCelestialLightingPlan celestialLighting;
    private DirectEmissiveBlockListPlan emissiveBlockList;
    private DirectShadowRayPlan shadowRayPlan;

    private DirectLightingPlanBuilder() {
    }

    public static DirectLightingPlanBuilder create() {
        return new DirectLightingPlanBuilder();
    }

    public DirectLightingPlanBuilder frameIndex(long frameIndex) {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        this.frameIndex = frameIndex;
        return this;
    }

    public DirectLightingPlanBuilder frameConstants(LucernaFrameConstants constants) {
        Objects.requireNonNull(constants, "constants");
        this.frameIndex(constants.frameIndex());
        this.celestialLighting = DirectCelestialLightingPlan.fromFrameConstants(constants);
        return this;
    }

    public DirectLightingPlanBuilder celestialLighting(DirectCelestialLightingPlan celestialLighting) {
        this.celestialLighting = Objects.requireNonNull(celestialLighting, "celestialLighting");
        return this;
    }

    public DirectLightingPlanBuilder emissiveBlockList(DirectEmissiveBlockListPlan emissiveBlockList) {
        this.emissiveBlockList = Objects.requireNonNull(emissiveBlockList, "emissiveBlockList");
        return this;
    }

    public DirectLightingPlanBuilder emissiveBlockListFromSectionSnapshots(
            List<ChunkSectionVoxelSnapshot> snapshots,
            int maxSelectedLights
    ) {
        this.emissiveBlockList = DirectEmissiveBlockListPlan.fromSectionSnapshots(snapshots, maxSelectedLights);
        return this;
    }

    public DirectLightingPlanBuilder shadowRayPlan(DirectShadowRayPlan shadowRayPlan) {
        this.shadowRayPlan = Objects.requireNonNull(shadowRayPlan, "shadowRayPlan");
        return this;
    }

    public DirectLightingPlan build() {
        long resolvedFrameIndex = this.resolveFrameIndex();
        DirectCelestialLightingPlan resolvedCelestialLighting = this.celestialLighting == null
                ? DirectCelestialLightingPlan.unavailable(resolvedFrameIndex)
                : this.celestialLighting;
        DirectEmissiveBlockListPlan resolvedEmissiveBlockList = this.emissiveBlockList == null
                ? DirectEmissiveBlockListPlan.empty()
                : this.emissiveBlockList;
        DirectShadowRayPlan resolvedShadowRayPlan = this.shadowRayPlan == null
                ? DirectShadowRayPlan.empty(resolvedFrameIndex)
                : this.shadowRayPlan;

        DirectLightValidationReport validationReport = DirectLightingPlanValidator.validate(
                resolvedFrameIndex,
                resolvedCelestialLighting,
                resolvedEmissiveBlockList,
                resolvedShadowRayPlan
        );
        return new DirectLightingPlan(
                resolvedFrameIndex,
                resolvedCelestialLighting,
                resolvedEmissiveBlockList,
                resolvedShadowRayPlan,
                validationReport
        );
    }

    private long resolveFrameIndex() {
        if (this.frameIndex != null) {
            return this.frameIndex;
        }
        long resolvedFrameIndex = 0L;
        if (this.celestialLighting != null) {
            resolvedFrameIndex = Math.max(resolvedFrameIndex, this.celestialLighting.frameIndex());
        }
        if (this.shadowRayPlan != null) {
            resolvedFrameIndex = Math.max(resolvedFrameIndex, this.shadowRayPlan.frameIndex());
        }
        return resolvedFrameIndex;
    }
}
