// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.util.Type;

public final class HLSlopedSurface extends HLLevelTransition {
    IsoDirections dir;
    int x;
    int y;
    int z;

    @Override
    public boolean equals(Object other) {
        HLSlopedSurface rhs = Type.tryCastTo(other, (Class<HLSlopedSurface>)this.getClass());
        return rhs != null && this.dir == rhs.dir && this.x == rhs.x && this.y == rhs.y && this.z == rhs.z;
    }

    public HLSlopedSurface set(IsoDirections dir, int x, int y, int z) {
        assert dir == IsoDirections.N || dir == IsoDirections.S || dir == IsoDirections.W || dir == IsoDirections.E;

        this.dir = dir;
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public HLSlopedSurface set(HLSlopedSurface other) {
        return this.set(other.dir, other.x, other.y, other.z);
    }

    public IsoDirections getDir() {
        return this.dir;
    }

    public IsoDirections getReverseDir() {
        return this.dir.Rot180();
    }

    public boolean isDir(IsoDirections dir) {
        return this.dir == dir;
    }

    public boolean isNorth() {
        return this.isDir(IsoDirections.N);
    }

    public boolean isSouth() {
        return this.isDir(IsoDirections.S);
    }

    public boolean isWest() {
        return this.isDir(IsoDirections.W);
    }

    public boolean isEast() {
        return this.isDir(IsoDirections.E);
    }

    @Override
    public int getBottomFloorX() {
        return this.x;
    }

    @Override
    public int getBottomFloorY() {
        return this.y;
    }

    @Override
    public int getBottomFloorZ() {
        return this.z;
    }

    @Override
    public int getTopFloorX() {
        return this.getBottomFloorX() + this.dir.dx();
    }

    @Override
    public int getTopFloorY() {
        return this.getBottomFloorY() + this.dir.dy();
    }

    @Override
    public int getTopFloorZ() {
        return this.getBottomFloorZ() + 1;
    }

    @Override
    public float getSearchNodeX(boolean bBottom) {
        return (bBottom ? this.getBottomFloorX() : this.getTopFloorX()) + 0.5F;
    }

    @Override
    public float getSearchNodeY(boolean bBottom) {
        return (bBottom ? this.getBottomFloorY() : this.getTopFloorY()) + 0.5F;
    }

    @Override
    public boolean isOnEdgeOfLoadedArea() {
        return false;
    }

    @Override
    HLSlopedSurface asSlopedSurface() {
        return this;
    }

    @Override
    public void renderDebug() {
        LineDrawer.addLine(
            this.getBottomFloorX() + 0.5F,
            this.getBottomFloorY() + 0.5F,
            this.getBottomFloorZ() - 32,
            this.getTopFloorX() + 0.5F,
            this.getTopFloorY() + 0.5F,
            this.getTopFloorZ() - 32,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );
    }
}
