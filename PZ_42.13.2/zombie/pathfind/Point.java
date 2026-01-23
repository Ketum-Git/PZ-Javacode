// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

public final class Point {
    public int x;
    public int y;

    Point init(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Point point && point.x == this.x && point.y == this.y;
    }
}
