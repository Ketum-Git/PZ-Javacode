// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import zombie.pathfind.Chunk;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;

public abstract class HLLevelTransition {
    public abstract int getBottomFloorX();

    public abstract int getBottomFloorY();

    public abstract int getBottomFloorZ();

    public abstract int getTopFloorX();

    public abstract int getTopFloorY();

    public abstract int getTopFloorZ();

    public abstract float getSearchNodeX(boolean arg0);

    public abstract float getSearchNodeY(boolean arg0);

    public boolean isOnEdgeOfLoadedArea() {
        return false;
    }

    public boolean isBottomFloorAt(int x, int y) {
        return x == this.getBottomFloorX() && y == this.getBottomFloorY();
    }

    public Chunk getBottomFloorChunk() {
        return PolygonalMap2.instance.getChunkFromSquarePos(this.getBottomFloorX(), this.getBottomFloorY());
    }

    public Chunk getTopFloorChunk() {
        return PolygonalMap2.instance.getChunkFromSquarePos(this.getTopFloorX(), this.getTopFloorY());
    }

    public Square getBottomFloorSquare() {
        return PolygonalMap2.instance.getSquare(this.getBottomFloorX(), this.getBottomFloorY(), this.getBottomFloorZ());
    }

    public Square getTopFloorSquare() {
        return PolygonalMap2.instance.getSquare(this.getTopFloorX(), this.getTopFloorY(), this.getTopFloorZ());
    }

    public void renderDebug() {
    }

    boolean isStaircase() {
        return false;
    }

    boolean isSlopedSurface() {
        return false;
    }

    HLStaircase asStaircase() {
        return null;
    }

    HLSlopedSurface asSlopedSurface() {
        return null;
    }
}
