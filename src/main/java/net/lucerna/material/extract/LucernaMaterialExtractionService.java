package net.lucerna.material.extract;

import net.lucerna.material.LucernaMaterial;
import net.lucerna.material.MaterialRegistry;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LucernaMaterialExtractionService {
    private final MaterialRegistry materialRegistry;
    private final MinecraftMaterialExtractor extractor;
    private volatile MaterialTableRefreshResult lastRefreshResult;

    public LucernaMaterialExtractionService(MaterialRegistry materialRegistry) {
        this(materialRegistry, new MinecraftMaterialExtractor());
    }

    public LucernaMaterialExtractionService(MaterialRegistry materialRegistry, MinecraftMaterialExtractor extractor) {
        this.materialRegistry = Objects.requireNonNull(materialRegistry, "materialRegistry");
        this.extractor = Objects.requireNonNull(extractor, "extractor");
    }

    public RegisteredMaterial resolve(BlockState state) {
        return this.resolve(state, null);
    }

    public RegisteredMaterial resolve(BlockState state, ModelManager modelManager) {
        ExtractedMaterial extracted = this.extractor.extract(state, modelManager);
        LucernaMaterial material = extracted.register(this.materialRegistry);
        return RegisteredMaterial.from(extracted, material);
    }

    public List<RegisteredMaterial> resolveAll(Collection<BlockState> states) {
        return this.resolveAll(states, null);
    }

    public List<RegisteredMaterial> resolveAll(Collection<BlockState> states, ModelManager modelManager) {
        Objects.requireNonNull(states, "states");
        return states.stream()
                .map(state -> this.resolve(state, modelManager))
                .toList();
    }

    public MaterialTableRefreshPlan planKnownBlockStateRefresh() {
        return MaterialTableRefreshPlan.allKnownBlockStates(this.materialRegistry.currentGeneration());
    }

    public MaterialTableRefreshPlan planRefresh(Collection<BlockState> states) {
        return MaterialTableRefreshPlan.fromStates(this.materialRegistry.currentGeneration(), states);
    }

    public MaterialTableRefreshResult refreshKnownBlockStateMaterials() {
        return this.refreshKnownBlockStateMaterials(null);
    }

    public MaterialTableRefreshResult refreshKnownBlockStateMaterials(ModelManager modelManager) {
        return this.refresh(this.planKnownBlockStateRefresh(), modelManager);
    }

    public MaterialTableRefreshResult refresh(MaterialTableRefreshPlan plan) {
        return this.refresh(plan, null);
    }

    public synchronized MaterialTableRefreshResult refresh(MaterialTableRefreshPlan plan, ModelManager modelManager) {
        Objects.requireNonNull(plan, "plan");

        long generationBefore = this.materialRegistry.currentGeneration();
        int materialCountBefore = this.materialRegistry.materialCount();
        List<RegisteredMaterial> registeredMaterials = new ArrayList<>(plan.stateCount());
        List<MaterialExtractionFailure> failures = new ArrayList<>();
        int attemptedStateCount = 0;

        for (BlockState state : plan.states()) {
            attemptedStateCount++;
            try {
                registeredMaterials.add(this.resolve(state, modelManager));
            } catch (RuntimeException exception) {
                failures.add(MaterialExtractionFailure.from(state, exception));
            }
        }

        MaterialTableRefreshResult result = new MaterialTableRefreshResult(
                plan,
                generationBefore,
                this.materialRegistry.currentGeneration(),
                materialCountBefore,
                this.materialRegistry.materialCount(),
                attemptedStateCount,
                registeredMaterials,
                failures
        );
        this.lastRefreshResult = result;
        return result;
    }

    public Optional<MaterialTableRefreshResult> lastRefreshResult() {
        return Optional.ofNullable(this.lastRefreshResult);
    }

    public MaterialRegistry materialRegistry() {
        return this.materialRegistry;
    }

    public MinecraftMaterialExtractor extractor() {
        return this.extractor;
    }
}
