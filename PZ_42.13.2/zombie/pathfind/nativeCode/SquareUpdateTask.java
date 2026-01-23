// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import zombie.iso.BentFences;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.util.Type;

class SquareUpdateTask implements IPathfindTask {
    private static final int BIT_SOLID = 1;
    private static final int BIT_COLLIDE_W = 2;
    private static final int BIT_COLLIDE_N = 4;
    private static final int BIT_STAIR_TW = 8;
    private static final int BIT_STAIR_MW = 16;
    private static final int BIT_STAIR_BW = 32;
    private static final int BIT_STAIR_TN = 64;
    private static final int BIT_STAIR_MN = 128;
    private static final int BIT_STAIR_BN = 256;
    private static final int BIT_SOLID_FLOOR = 512;
    private static final int BIT_SOLID_TRANS = 1024;
    private static final int BIT_WINDOW_W = 2048;
    private static final int BIT_WINDOW_N = 4096;
    private static final int BIT_CAN_PATH_W = 8192;
    private static final int BIT_CAN_PATH_N = 16384;
    private static final int BIT_THUMP_W = 32768;
    private static final int BIT_THUMP_N = 65536;
    private static final int BIT_THUMPABLE = 131072;
    private static final int BIT_DOOR_E = 262144;
    private static final int BIT_DOOR_S = 524288;
    private static final int BIT_WINDOW_W_UNBLOCKED = 1048576;
    private static final int BIT_WINDOW_N_UNBLOCKED = 2097152;
    private static final int BIT_DOOR_W_UNBLOCKED = 4194304;
    private static final int BIT_DOOR_N_UNBLOCKED = 8388608;
    private static final int BIT_HOPPABLE_N = 16777216;
    private static final int BIT_HOPPABLE_W = 33554432;
    private static final int BIT_TARGET_PATH_W = 67108864;
    private static final int BIT_TARGET_PATH_N = 134217728;
    private static final int BIT_BENDABLE_W = 268435456;
    private static final int BIT_BENDABLE_N = 536870912;
    private static final int BIT_TALL_HOPPABLE_N = 1073741824;
    private static final int BIT_TALL_HOPPABLE_W = Integer.MIN_VALUE;
    private static final int ALL_SOLID_BITS = 1025;
    private static final int ALL_STAIR_BITS = 504;
    private int x;
    private int y;
    private int z;
    private int bits;
    private short cost;
    private IsoDirections slopedSurfaceDirection;
    private float slopedSurfaceHeightMin;
    private float slopedSurfaceHeightMax;
    private short loadId;
    private static final ObjectPool<SquareUpdateTask> pool = new ObjectPool<>(SquareUpdateTask::new);

    public SquareUpdateTask init(IsoGridSquare square) {
        this.x = square.x;
        this.y = square.y;
        this.z = square.z + 32;
        this.bits = getBits(square);
        this.cost = getCost(square);
        this.slopedSurfaceDirection = square.getSlopedSurfaceDirection();
        if (this.slopedSurfaceDirection == null) {
            this.slopedSurfaceDirection = IsoDirections.Max;
        }

        this.slopedSurfaceHeightMin = square.getSlopedSurfaceHeightMin();
        this.slopedSurfaceHeightMax = square.getSlopedSurfaceHeightMax();
        this.loadId = square.chunk.getLoadID();
        return this;
    }

    @Override
    public void execute() {
        PathfindNative.updateSquare(
            this.loadId,
            this.x,
            this.y,
            this.z,
            this.bits,
            this.cost,
            this.slopedSurfaceDirection.indexUnmodified(),
            this.slopedSurfaceHeightMin,
            this.slopedSurfaceHeightMax
        );
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

        if (sq.has(IsoFlagType.HoppableN)) {
            bits |= 16777216;
        } else if ((sq.has(IsoFlagType.TallHoppableN) || sq.has(IsoFlagType.WallN) || sq.has(IsoFlagType.WallNTrans))
            && canClimbOverWall(sq, IsoDirections.N)
            && canClimbOverWall(sq, IsoDirections.S)) {
            bits |= 1073741824;
        }

        if (sq.has(IsoFlagType.HoppableW)) {
            bits |= 33554432;
        } else if ((sq.has(IsoFlagType.TallHoppableW) || sq.has(IsoFlagType.WallW) || sq.has(IsoFlagType.WallWTrans))
            && canClimbOverWall(sq, IsoDirections.W)
            && canClimbOverWall(sq, IsoDirections.E)) {
            bits |= Integer.MIN_VALUE;
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
            if (!sq.isSolid() && !sq.isSolidTrans()) {
                bits |= 1024;
            }
        }

        if (sq.hasLitCampfire()) {
            bits |= 8192;
            bits |= 16384;
            bits |= 131072;
            bits |= 1024;
        }

        if (hasWallThumpableN(sq)) {
            bits |= 81920;
        }

        if (hasWallThumpableW(sq)) {
            bits |= 40960;
        }

        if (BentFences.getInstance().isEnabled()) {
            if (hasWallBendableN(sq)) {
                bits |= 16384;
                bits |= 65536;
                bits |= 536870912;
                bits |= 134217728;
            }

            if (hasWallBendableW(sq)) {
                bits |= 8192;
                bits |= 32768;
                bits |= 268435456;
                bits |= 67108864;
            }
        }

        return bits;
    }

    public static short getCost(IsoGridSquare sq) {
        short cost = 0;
        if (sq.HasTree() || sq.hasBush()) {
            cost = (short)(cost + 5);
        }

        return cost;
    }

    static boolean isWindowUnblocked(IsoGridSquare sq, boolean north) {
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
        for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
            if (sq.getSpecialObjects().get(i) instanceof IsoThumpable thump
                && !thump.canClimbThrough(null)
                && (!thump.canClimbOver(null) || thump.isTallHoppable())
                && thump.isThumpable()
                && !thump.isBlockAllTheSquare()
                && !thump.isDoor()) {
                return (thump.isWallN() || thump.isCorner()) && !thump.isCanPassThrough();
            }
        }

        return false;
    }

    private static boolean hasWallThumpableW(IsoGridSquare sq) {
        for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
            if (sq.getSpecialObjects().get(i) instanceof IsoThumpable thump
                && !thump.canClimbThrough(null)
                && (!thump.canClimbOver(null) || thump.isTallHoppable())
                && thump.isThumpable()
                && !thump.isBlockAllTheSquare()
                && !thump.isDoor()) {
                return (thump.isWallW() || thump.isCorner()) && !thump.isCanPassThrough();
            }
        }

        return false;
    }

    private static boolean hasWallBendableW(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (BentFences.getInstance().isUnbentObject(obj) && obj.isWallW()) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasWallBendableN(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (BentFences.getInstance().isUnbentObject(obj) && obj.isWallN()) {
                return true;
            }
        }

        return false;
    }

    private static boolean canClimbOverWall(IsoGridSquare square, IsoDirections dir) {
        if (square == null) {
            return false;
        } else {
            IsoGridSquare squareAdjacent = square.getAdjacentSquare(dir);
            if (squareAdjacent == null) {
                return false;
            } else if (square.haveRoof || squareAdjacent.haveRoof) {
                return false;
            } else if (!square.TreatAsSolidFloor() || !squareAdjacent.TreatAsSolidFloor()) {
                return false;
            } else if (IsoWindow.isSheetRopeHere(square) || IsoWindow.isSheetRopeHere(squareAdjacent)) {
                return false;
            } else if (square.getBuilding() != null || squareAdjacent.getBuilding() != null) {
                return false;
            } else if (square.has(IsoFlagType.water) || squareAdjacent.has(IsoFlagType.water)) {
                return false;
            } else if (square.has(IsoFlagType.CantClimb) || squareAdjacent.has(IsoFlagType.CantClimb)) {
                return false;
            } else if (!square.isSolid() && !square.isSolidTrans() && !squareAdjacent.isSolid() && !squareAdjacent.isSolidTrans()) {
                IsoGridSquare above = IsoWorld.instance.currentCell.getGridSquare(square.x, square.y, square.z + 1);
                if (above != null && above.HasSlopedRoof() && !above.HasEave()) {
                    return false;
                } else {
                    IsoGridSquare above2 = IsoWorld.instance.currentCell.getGridSquare(squareAdjacent.x, squareAdjacent.y, squareAdjacent.z + 1);
                    if (above2 != null && above2.HasSlopedRoof() && !above2.HasEave()) {
                        return false;
                    } else {
                        return (above == null || !above.has(IsoFlagType.collideN)) && (above2 == null || !above2.has(IsoFlagType.collideN))
                            ? !GameClient.client || SafeHouse.getSafeHouse(square) == null && SafeHouse.getSafeHouse(squareAdjacent) == null
                            : false;
                    }
                }
            } else {
                return false;
            }
        }
    }

    private static boolean canClimbDownSheetRope(IsoGridSquare sq) {
        if (sq == null) {
            return false;
        } else {
            for (int startZ = sq.getZ();
                sq != null;
                sq = IsoWorld.instance.currentCell.getGridSquare((double)sq.getX(), (double)sq.getY(), (double)(sq.getZ() - 1.0F))
            ) {
                if (!IsoWindow.isSheetRopeHere(sq)) {
                    return false;
                }

                if (!IsoWindow.canClimbHere(sq)) {
                    return false;
                }

                if (sq.TreatAsSolidFloor()) {
                    return sq.getZ() < startZ;
                }
            }

            return false;
        }
    }

    public static SquareUpdateTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}
