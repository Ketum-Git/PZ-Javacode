// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.util.Type;

final class SquareUpdateTask {
    private PolygonalMap2 map;
    public int x;
    public int y;
    public int z;
    public int bits;
    public short cost;
    public SlopedSurface slopedSurface;
    private static final ArrayDeque<SquareUpdateTask> pool = new ArrayDeque<>();

    public SquareUpdateTask init(PolygonalMap2 map, IsoGridSquare square) {
        this.map = map;
        this.x = square.x;
        this.y = square.y;
        this.z = square.z + 32;
        this.bits = getBits(square);
        this.cost = getCost(square);
        if (this.slopedSurface != null) {
            this.slopedSurface.release();
            this.slopedSurface = null;
        }

        this.slopedSurface = initSlopedSurface(square, this.x - square.chunk.wx * 8, this.y - square.chunk.wy * 8);
        return this;
    }

    public void execute() {
        Chunk chunk = this.map.getChunkFromChunkPos(PZMath.coorddivision(this.x, 8), PZMath.coorddivision(this.y, 8));
        if (chunk != null && chunk.setData(this)) {
            ChunkDataZ.epochCount++;
            this.map.rebuild = true;
        }
    }

    public static int getBits(IsoGridSquare sq) {
        int bits = 0;
        if (sq.has(IsoFlagType.solidfloor)) {
            bits |= 512;
        }

        if (sq.isSolid()) {
            bits |= 1;
        }

        if (sq.isSolidTrans()) {
            bits |= 1024;
        }

        if (sq.has(IsoFlagType.collideW)) {
            bits |= 2;
        }

        if (sq.has(IsoFlagType.collideN)) {
            bits |= 4;
        }

        if (sq.has(IsoObjectType.stairsTW)) {
            bits |= 8;
        }

        if (sq.has(IsoObjectType.stairsMW)) {
            bits |= 16;
        }

        if (sq.has(IsoObjectType.stairsBW)) {
            bits |= 32;
        }

        if (sq.has(IsoObjectType.stairsTN)) {
            bits |= 64;
        }

        if (sq.has(IsoObjectType.stairsMN)) {
            bits |= 128;
        }

        if (sq.has(IsoObjectType.stairsBN)) {
            bits |= 256;
        }

        if (sq.has(IsoFlagType.windowW) || sq.has(IsoFlagType.WindowW)) {
            bits |= 2050;
            if (isWindowUnblocked(sq, false)) {
                bits |= 1048576;
            }
        }

        if (sq.has(IsoFlagType.windowN) || sq.has(IsoFlagType.WindowN)) {
            bits |= 4100;
            if (isWindowUnblocked(sq, true)) {
                bits |= 2097152;
            }
        }

        if (sq.has(IsoFlagType.canPathW)) {
            bits |= 8192;
        }

        if (sq.has(IsoFlagType.canPathN)) {
            bits |= 16384;
        }

        boolean bHasDoorW = false;
        boolean bHasDoorN = false;

        for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
            IsoObject obj = sq.getSpecialObjects().get(i);
            IsoDirections dir = IsoDirections.Max;
            if (obj instanceof IsoDoor isoDoor) {
                dir = isoDoor.getSpriteEdge(false);
                if (isoDoor.IsOpen()) {
                    dir = isoDoor.getSpriteEdge(true);
                    if (dir == IsoDirections.N) {
                        bits |= 8388608;
                    } else if (dir == IsoDirections.W) {
                        bits |= 4194304;
                    }

                    dir = IsoDirections.Max;
                }
            } else if (obj instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
                dir = isoThumpable.getSpriteEdge(false);
                if (isoThumpable.IsOpen()) {
                    dir = isoThumpable.getSpriteEdge(true);
                    if (dir == IsoDirections.N) {
                        bits |= 8388608;
                    } else if (dir == IsoDirections.W) {
                        bits |= 4194304;
                    }

                    dir = IsoDirections.Max;
                }
            }

            if (dir == IsoDirections.W) {
                bits |= 8192;
                bits |= 2;
                bHasDoorW = true;
            } else if (dir == IsoDirections.N) {
                bits |= 16384;
                bits |= 4;
                bHasDoorN = true;
            } else if (dir == IsoDirections.S) {
                bits |= 524288;
            } else if (dir == IsoDirections.E) {
                bits |= 262144;
            }
        }

        if (sq.has(IsoFlagType.DoorWallW)) {
            bits |= 8192;
            bits |= 2;
            if (!bHasDoorW) {
                bits |= 4194304;
            }
        }

        if (sq.has(IsoFlagType.DoorWallN)) {
            bits |= 16384;
            bits |= 4;
            if (!bHasDoorN) {
                bits |= 8388608;
            }
        }

        if (hasSquareThumpable(sq)) {
            bits |= 8192;
            bits |= 16384;
            bits |= 131072;
        }

        if (hasWallThumpableN(sq)) {
            bits |= 81920;
        }

        if (hasWallThumpableW(sq)) {
            bits |= 40960;
        }

        return bits;
    }

    private static boolean isWindowUnblocked(IsoGridSquare sq, boolean north) {
        for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
            IsoObject special = sq.getSpecialObjects().get(i);
            if (special instanceof IsoThumpable thump && thump.isWindow() && north == thump.north) {
                if (thump.isBarricaded()) {
                    return false;
                }

                return true;
            }

            if (special instanceof IsoWindow window && north == window.isNorth()) {
                if (window.isBarricaded()) {
                    return false;
                }

                if (window.isInvincible()) {
                    return false;
                }

                if (window.IsOpen()) {
                    return true;
                }

                if (window.isDestroyed() && window.isGlassRemoved()) {
                    return true;
                }

                return false;
            }
        }

        IsoWindowFrame frame = sq.getWindowFrame(north);
        return frame != null && frame.canClimbThrough(null);
    }

    private static boolean hasSquareThumpable(IsoGridSquare sq) {
        if (sq.HasStairs()) {
            return false;
        } else {
            for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
                IsoThumpable thump = Type.tryCastTo(sq.getSpecialObjects().get(i), IsoThumpable.class);
                if (thump != null && thump.isThumpable() && thump.isBlockAllTheSquare()) {
                    return true;
                }
            }

            for (int ix = 0; ix < sq.getObjects().size(); ix++) {
                IsoObject obj = sq.getObjects().get(ix);
                if (obj.isMovedThumpable()) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean hasWallThumpableN(IsoGridSquare sq) {
        IsoGridSquare n = sq.getAdjacentSquare(IsoDirections.N);
        if (n == null) {
            return false;
        } else {
            for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
                if (sq.getSpecialObjects().get(i) instanceof IsoThumpable thump
                    && !thump.canClimbThrough(null)
                    && !thump.canClimbOver(null)
                    && thump.isThumpable()
                    && !thump.isBlockAllTheSquare()
                    && !thump.isDoor()
                    && thump.TestCollide(null, sq, n)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean hasWallThumpableW(IsoGridSquare sq) {
        IsoGridSquare w = sq.getAdjacentSquare(IsoDirections.W);
        if (w == null) {
            return false;
        } else {
            for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
                if (sq.getSpecialObjects().get(i) instanceof IsoThumpable thump
                    && !thump.canClimbThrough(null)
                    && !thump.canClimbOver(null)
                    && thump.isThumpable()
                    && !thump.isBlockAllTheSquare()
                    && !thump.isDoor()
                    && thump.TestCollide(null, sq, w)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static short getCost(IsoGridSquare sq) {
        short cost = 0;
        if (sq.HasTree() || sq.hasBush()) {
            cost = (short)(cost + 5);
        }

        return cost;
    }

    private static SlopedSurface initSlopedSurface(IsoGridSquare square, int x, int y) {
        if (!square.hasSlopedSurface()) {
            return null;
        } else {
            SlopedSurface slopedSurface1 = SlopedSurface.alloc();
            slopedSurface1.x = (byte)x;
            slopedSurface1.y = (byte)y;
            slopedSurface1.direction = square.getSlopedSurfaceDirection();
            slopedSurface1.heightMin = square.getSlopedSurfaceHeightMin();
            slopedSurface1.heightMax = square.getSlopedSurfaceHeightMax();
            return slopedSurface1;
        }
    }

    public static SquareUpdateTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new SquareUpdateTask() : pool.pop();
        }
    }

    public void release() {
        synchronized (pool) {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
