package net.lucerna.material.extract;

import net.minecraft.core.Direction;

public enum MaterialFace {
    WHOLE_BLOCK(-1),
    DOWN(Direction.DOWN.get3DDataValue()),
    UP(Direction.UP.get3DDataValue()),
    NORTH(Direction.NORTH.get3DDataValue()),
    SOUTH(Direction.SOUTH.get3DDataValue()),
    WEST(Direction.WEST.get3DDataValue()),
    EAST(Direction.EAST.get3DDataValue());

    private final int id;

    MaterialFace(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    public static int idFor(Direction direction) {
        return direction == null ? WHOLE_BLOCK.id : direction.get3DDataValue();
    }
}
