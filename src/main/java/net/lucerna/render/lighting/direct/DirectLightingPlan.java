package net.lucerna.render.lighting.direct;

import java.util.List;
import java.util.Objects;

public record DirectLightingPlan(
        long frameIndex,
        DirectCelestialLightingPlan celestialLighting,
        DirectEmissiveBlockListPlan emissiveBlockList,
        DirectShadowRayPlan shadowRayPlan,
        DirectLightValidationReport validationReport
) {
    public DirectLightingPlan {
        if (frameIndex < 0L) {
            throw new IllegalArgumentException("frameIndex must be non-negative");
        }
        Objects.requireNonNull(celestialLighting, "celestialLighting");
        Objects.requireNonNull(emissiveBlockList, "emissiveBlockList");
        Objects.requireNonNull(shadowRayPlan, "shadowRayPlan");
        Objects.requireNonNull(validationReport, "validationReport");
    }

    public static DirectLightingPlanBuilder builder() {
        return DirectLightingPlanBuilder.create();
    }

    public static DirectLightingPlan from(
            DirectCelestialLightingPlan celestialLighting,
            DirectEmissiveBlockListPlan emissiveBlockList,
            DirectShadowRayPlan shadowRayPlan
    ) {
        Objects.requireNonNull(celestialLighting, "celestialLighting");
        Objects.requireNonNull(emissiveBlockList, "emissiveBlockList");
        Objects.requireNonNull(shadowRayPlan, "shadowRayPlan");
        long frameIndex = Math.max(celestialLighting.frameIndex(), shadowRayPlan.frameIndex());
        DirectLightValidationReport validationReport = DirectLightingPlanValidator.validate(
                frameIndex,
                celestialLighting,
                emissiveBlockList,
                shadowRayPlan
        );
        return new DirectLightingPlan(
                frameIndex,
                celestialLighting,
                emissiveBlockList,
                shadowRayPlan,
                validationReport
        );
    }

    public boolean valid() {
        return this.validationReport.valid();
    }

    public boolean hasFindings() {
        return this.validationReport.hasFindings();
    }

    public List<DirectLightValidationFinding> findings() {
        return this.validationReport.findings();
    }

    public boolean hasDirectLightingWork() {
        return this.celestialLighting.hasActiveLight() || this.emissiveBlockList.hasSelectedLights();
    }

    public boolean readyForShadowPlanning() {
        return this.valid()
                && this.hasDirectLightingWork()
                && this.shadowRayPlan.hasRayCandidates()
                && this.shadowRayPlan.hasSectionSnapshots();
    }
}
