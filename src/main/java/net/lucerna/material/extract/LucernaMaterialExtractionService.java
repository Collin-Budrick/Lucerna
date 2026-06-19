package net.lucerna.material.extract;

import net.lucerna.material.LucernaMaterial;
import net.lucerna.material.MaterialRegistry;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class LucernaMaterialExtractionService {
    private final MaterialRegistry materialRegistry;
    private final MinecraftMaterialExtractor extractor;

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

    public MaterialRegistry materialRegistry() {
        return this.materialRegistry;
    }

    public MinecraftMaterialExtractor extractor() {
        return this.extractor;
    }
}
