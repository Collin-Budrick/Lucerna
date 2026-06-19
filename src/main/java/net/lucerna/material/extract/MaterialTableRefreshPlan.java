package net.lucerna.material.extract;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record MaterialTableRefreshPlan(
        long plannedAtGeneration,
        int blockCount,
        List<BlockState> states
) {
    public MaterialTableRefreshPlan {
        Objects.requireNonNull(states, "states");
        states = List.copyOf(states);
        if (plannedAtGeneration < 0) {
            throw new IllegalArgumentException("plannedAtGeneration must be non-negative");
        }
        if (blockCount < 0) {
            throw new IllegalArgumentException("blockCount must be non-negative");
        }
    }

    public static MaterialTableRefreshPlan allKnownBlockStates(long plannedAtGeneration) {
        if (plannedAtGeneration < 0) {
            throw new IllegalArgumentException("plannedAtGeneration must be non-negative");
        }

        List<BlockState> states = new ArrayList<>();
        int blockCount = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            blockCount++;
            states.addAll(block.getStateDefinition().getPossibleStates());
        }
        return new MaterialTableRefreshPlan(plannedAtGeneration, blockCount, states);
    }

    public static MaterialTableRefreshPlan fromStates(long plannedAtGeneration, Collection<BlockState> states) {
        Objects.requireNonNull(states, "states");
        return new MaterialTableRefreshPlan(plannedAtGeneration, 0, List.copyOf(states));
    }

    public int stateCount() {
        return this.states.size();
    }

    public boolean isEmpty() {
        return this.states.isEmpty();
    }
}
