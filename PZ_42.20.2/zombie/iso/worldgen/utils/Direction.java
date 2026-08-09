// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils;

import java.util.List;

public enum Direction {
    NORTH(0, 1, 4, 5, 0, -1, "n"),
    SOUTH(1, 0, 7, 6, 0, 1, "s"),
    WEST(2, 3, 6, 4, -1, 0, "w"),
    EAST(3, 2, 5, 7, 1, 0, "e"),
    NORTH_WEST(4, 7, 2, 0, -1, -1, "nw"),
    NORTH_EAST(5, 6, 0, 3, 1, -1, "ne"),
    SOUTH_WEST(6, 5, 1, 2, -1, 1, "sw"),
    SOUTH_EAST(7, 4, 3, 1, 1, 1, "se");

    private static final List<Direction> cardinals = List.of(NORTH, SOUTH, WEST, EAST);
    private static final List<Direction> diagonals = List.of(NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST);
    private static final List<Direction> rose = List.of(NORTH, SOUTH, WEST, EAST, NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST);
    public final int index;
    public final int x;
    public final int y;
    public final String name;
    private final int opposite;
    private final int prev;
    private final int next;

    private Direction(final int index, final int opposite, final int prev, final int next, final int x, final int y, final String name) {
        this.index = index;
        this.opposite = opposite;
        this.prev = prev;
        this.next = next;
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public static List<Direction> cardinals() {
        return cardinals;
    }

    public static List<Direction> diagonals() {
        return diagonals;
    }

    public static List<Direction> rose() {
        return rose;
    }

    public Direction opposite() {
        return values()[this.opposite];
    }

    public Direction prev() {
        return values()[this.prev];
    }

    public Direction next() {
        return values()[this.next];
    }
}
