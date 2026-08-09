// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.extra;

import java.util.EnumMap;

public enum BorderStatus {
    OPEN,
    WALL,
    OUT_OF_RANGE;

    public static final EnumMap<Direction, BorderStatus> NO_WALLS = new EnumMap<>(Direction.class);

    static {
        NO_WALLS.put(Direction.NORTH, OPEN);
        NO_WALLS.put(Direction.SOUTH, OPEN);
        NO_WALLS.put(Direction.EAST, OPEN);
        NO_WALLS.put(Direction.WEST, OPEN);
        NO_WALLS.put(Direction.NORTH_EAST, OPEN);
        NO_WALLS.put(Direction.NORTH_WEST, OPEN);
        NO_WALLS.put(Direction.SOUTH_EAST, OPEN);
        NO_WALLS.put(Direction.SOUTH_WEST, OPEN);
    }
}
