package net.lucerna.material.extract;

import java.util.List;
import java.util.Objects;

public record MaterialTableRefreshResult(
        MaterialTableRefreshPlan plan,
        long generationBefore,
        long generationAfter,
        int materialCountBefore,
        int materialCountAfter,
        int attemptedStateCount,
        List<RegisteredMaterial> registeredMaterials,
        List<MaterialExtractionFailure> failures
) {
    public MaterialTableRefreshResult {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(registeredMaterials, "registeredMaterials");
        Objects.requireNonNull(failures, "failures");
        registeredMaterials = List.copyOf(registeredMaterials);
        failures = List.copyOf(failures);
        if (generationBefore < 0 || generationAfter < 0) {
            throw new IllegalArgumentException("generations must be non-negative");
        }
        if (generationAfter < generationBefore) {
            throw new IllegalArgumentException("generationAfter must not be less than generationBefore");
        }
        if (materialCountBefore < 0 || materialCountAfter < 0) {
            throw new IllegalArgumentException("material counts must be non-negative");
        }
        if (materialCountAfter < materialCountBefore) {
            throw new IllegalArgumentException("materialCountAfter must not be less than materialCountBefore");
        }
        if (attemptedStateCount < 0) {
            throw new IllegalArgumentException("attemptedStateCount must be non-negative");
        }
        if (attemptedStateCount != registeredMaterials.size() + failures.size()) {
            throw new IllegalArgumentException("attemptedStateCount must match registered and failed state counts");
        }
        if (attemptedStateCount > plan.stateCount()) {
            throw new IllegalArgumentException("attemptedStateCount cannot exceed planned state count");
        }
    }

    public int plannedStateCount() {
        return this.plan.stateCount();
    }

    public int registeredStateCount() {
        return this.registeredMaterials.size();
    }

    public int failedStateCount() {
        return this.failures.size();
    }

    public int createdMaterialCount() {
        return this.materialCountAfter - this.materialCountBefore;
    }

    public boolean generationAdvanced() {
        return this.generationAfter > this.generationBefore;
    }

    public boolean changed() {
        return this.generationAdvanced() || this.createdMaterialCount() > 0;
    }

    public boolean complete() {
        return this.failedStateCount() == 0 && this.attemptedStateCount == this.plannedStateCount();
    }
}
