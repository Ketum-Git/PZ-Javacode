// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.blending;

public enum BlendDirection {
    NORTH(0, 1, 0, -1, 0, 1, (byte)7),
    SOUTH(1, 0, 0, 1, 0, 1, (byte)0),
    WEST(2, 3, -1, 0, 1, 0, (byte)7),
    EAST(3, 2, 1, 0, 1, 0, (byte)0);

    public final int x;
    public final int y;
    public final int planeX;
    public final int planeY;
    public final int index;
    private final int opposite;
    public final byte defaultDepth;

    private BlendDirection(final int index, final int opposite, final int x, final int y, final int planeX, final int planeY, final byte defaultDepth) {
        this.x = x;
        this.y = y;
        this.planeX = planeX;
        this.planeY = planeY;
        this.index = index;
        this.opposite = opposite;
        this.defaultDepth = defaultDepth;
    }

    public BlendDirection opposite() {
        return values()[this.opposite];
    }
}
