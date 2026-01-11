// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;

public final class Square {
    static int nextID = 1;
    Integer id = nextID++;
    int x;
    int y;
    int z;
    int bits;
    short cost;
    IsoDirections slopedSurfaceDirection;
    float slopedSurfaceHeightMin;
    float slopedSurfaceHeightMax;
    static final ArrayDeque<Square> pool = new ArrayDeque<>();

    Square() {
    }

    Square init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.bits = 0;
        this.cost = 0;
        this.slopedSurfaceDirection = null;
        this.slopedSurfaceHeightMin = 0.0F;
        this.slopedSurfaceHeightMax = 0.0F;
        return this;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public boolean has(int bit) {
        return (this.bits & bit) != 0;
    }

    public boolean TreatAsSolidFloor() {
        return this.has(512) || this.has(504);
    }

    public boolean isReallySolid() {
        return this.has(1) || this.has(1024) && !this.isAdjacentToWindow() && !this.isAdjacentToHoppable();
    }

    boolean isNonThumpableSolid() {
        return this.isReallySolid() && !this.has(131072);
    }

    boolean isCanPathW() {
        if (this.has(8192)) {
            return true;
        } else {
            Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
            return w != null && (w.has(131072) || w.has(262144));
        }
    }

    boolean isCanPathN() {
        if (this.has(16384)) {
            return true;
        } else {
            Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
            return n != null && (n.has(131072) || n.has(524288));
        }
    }

    boolean isCollideW() {
        if (this.has(2)) {
            return true;
        } else {
            Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
            return w != null && (w.has(262144) || w.has(448) || w.isReallySolid());
        }
    }

    boolean isCollideN() {
        if (this.has(4)) {
            return true;
        } else {
            Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
            return n != null && (n.has(524288) || n.has(56) || n.isReallySolid());
        }
    }

    boolean isThumpW() {
        if (this.has(32768)) {
            return true;
        } else {
            Square w = PolygonalMap2.instance.getSquareRawZ(this.x - 1, this.y, this.z);
            return w != null && w.has(131072);
        }
    }

    boolean isThumpN() {
        if (this.has(65536)) {
            return true;
        } else {
            Square n = PolygonalMap2.instance.getSquareRawZ(this.x, this.y - 1, this.z);
            return n != null && n.has(131072);
        }
    }

    boolean isAdjacentToWindow() {
        if (!this.has(2048) && !this.has(4096)) {
            Square s = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
            if (s != null && s.has(4096)) {
                return true;
            } else {
                Square e = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
                return e != null && e.has(2048);
            }
        } else {
            return true;
        }
    }

    boolean isAdjacentToHoppable() {
        if (!this.has(16777216) && !this.has(33554432)) {
            Square s = PolygonalMap2.instance.getSquareRawZ(this.x, this.y + 1, this.z);
            if (s != null && s.has(16777216)) {
                return true;
            } else {
                Square e = PolygonalMap2.instance.getSquareRawZ(this.x + 1, this.y, this.z);
                return e != null && e.has(33554432);
            }
        } else {
            return true;
        }
    }

    public boolean isUnblockedWindowN() {
        if (!this.has(2097152)) {
            return false;
        } else if (this.isReallySolid()) {
            return false;
        } else {
            Square n = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
            return n != null && !n.isReallySolid();
        }
    }

    public boolean isUnblockedWindowW() {
        if (!this.has(1048576)) {
            return false;
        } else if (this.isReallySolid()) {
            return false;
        } else {
            Square w = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
            return w != null && !w.isReallySolid();
        }
    }

    boolean isUnblockedDoorN() {
        if (!this.has(8388608)) {
            return false;
        } else if (this.has(1025)) {
            return false;
        } else {
            Square n = PolygonalMap2.instance.getSquare(this.x, this.y - 1, this.z);
            return n != null && !n.has(1025);
        }
    }

    boolean isUnblockedDoorW() {
        if (!this.has(4194304)) {
            return false;
        } else if (this.has(1025)) {
            return false;
        } else {
            Square w = PolygonalMap2.instance.getSquare(this.x - 1, this.y, this.z);
            return w != null && !w.has(1025);
        }
    }

    public Square getAdjacentSquare(IsoDirections dir) {
        return PolygonalMap2.instance.getSquare(this.getX() + dir.dx(), this.getY() + dir.dy(), this.getZ());
    }

    public boolean isInside(int x1, int y1, int x2, int y2) {
        return this.getX() >= x1 && this.getX() < x2 && this.getY() >= y1 && this.getY() < y2;
    }

    public boolean testPathFindAdjacent(PMMover mover, int dx, int dy, int dz) {
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1 || dz < -1 || dz > 1) {
            return true;
        } else {
            return dx == 0 && dy == 0 && dz == 0
                ? false
                : PolygonalMap2.instance.canNotMoveBetween(mover, this.getX(), this.getY(), this.getZ(), this.getX() + dx, this.getY() + dy, this.getZ() + dz);
        }
    }

    public boolean hasTransitionToLevelAbove(IsoDirections edge) {
        if (edge == IsoDirections.N && this.has(64)) {
            return true;
        } else {
            return edge == IsoDirections.W && this.has(8) ? true : this.hasSlopedSurfaceToLevelAbove(edge);
        }
    }

    public boolean hasSlopedSurface() {
        return this.slopedSurfaceDirection != null;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        return this.slopedSurfaceDirection;
    }

    public float getSlopedSurfaceHeightMin() {
        return this.slopedSurfaceHeightMin;
    }

    public float getSlopedSurfaceHeightMax() {
        return this.slopedSurfaceHeightMax;
    }

    public boolean hasIdenticalSlopedSurface(Square other) {
        return this.getSlopedSurfaceDirection() == other.getSlopedSurfaceDirection()
            && this.getSlopedSurfaceHeightMin() == other.getSlopedSurfaceHeightMin()
            && this.getSlopedSurfaceHeightMax() == other.getSlopedSurfaceHeightMax();
    }

    public boolean isSlopedSurfaceDirectionVertical() {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        return dir == IsoDirections.N || dir == IsoDirections.S;
    }

    public boolean isSlopedSurfaceDirectionHorizontal() {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        return dir == IsoDirections.W || dir == IsoDirections.E;
    }

    public float getSlopedSurfaceHeight(float dx, float dy) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return 0.0F;
        } else {
            dx = PZMath.clamp(dx, 0.0F, 1.0F);
            dy = PZMath.clamp(dy, 0.0F, 1.0F);
            float slopeHeightMin = this.getSlopedSurfaceHeightMin();
            float slopeHeightMax = this.getSlopedSurfaceHeightMax();

            float z = switch (dir) {
                case N -> PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0F - dy);
                case S -> PZMath.lerp(slopeHeightMin, slopeHeightMax, dy);
                case W -> PZMath.lerp(slopeHeightMin, slopeHeightMax, 1.0F - dx);
                case E -> PZMath.lerp(slopeHeightMin, slopeHeightMax, dx);
                default -> -1.0F;
            };
            return z < 0.0F ? 0.0F : z;
        }
    }

    public float getSlopedSurfaceHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return 0.0F;
        } else if (slopeDir == edge) {
            return this.getSlopedSurfaceHeightMax();
        } else {
            return slopeDir.Rot180() == edge ? this.getSlopedSurfaceHeightMin() : -1.0F;
        }
    }

    public boolean isSlopedSurfaceEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return false;
        } else {
            Square square2 = this.getAdjacentSquare(edge);
            return square2 == null ? true : this.getSlopedSurfaceHeight(edge) != square2.getSlopedSurfaceHeight(edge.Rot180());
        }
    }

    public boolean hasSlopedSurfaceToLevelAbove(IsoDirections dir) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        return slopeDir == null ? false : this.getSlopedSurfaceHeight(dir) == 1.0F;
    }

    public boolean hasSlopedSurfaceBottom(IsoDirections slopeDir) {
        return !this.hasSlopedSurface() ? false : this.getSlopedSurfaceHeight(slopeDir.Rot180()) == 0.0F;
    }

    static Square alloc() {
        return pool.isEmpty() ? new Square() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
