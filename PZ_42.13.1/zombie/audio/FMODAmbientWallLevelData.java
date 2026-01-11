// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import java.util.ArrayList;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class FMODAmbientWallLevelData extends PooledObject {
    private static final Pool<FMODAmbientWallLevelData> s_levelDataPool = new Pool<>(FMODAmbientWallLevelData::new);
    private static final Pool<FMODAmbientWallLevelData.FMODAmbientWall> s_wallPool = new Pool<>(FMODAmbientWallLevelData.FMODAmbientWall::new);
    public IsoChunkLevel chunkLevel;
    public final ArrayList<FMODAmbientWallLevelData.FMODAmbientWall> walls = new ArrayList<>();
    public boolean dirty = true;

    public FMODAmbientWallLevelData init(IsoChunkLevel chunkLevel) {
        this.chunkLevel = chunkLevel;
        return this;
    }

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.recreate();
        }
    }

    void recreate() {
        IPooledObject.release(this.walls);
        IsoChunk m_chunk = this.chunkLevel.getChunk();
        int level = this.chunkLevel.getLevel();
        IsoGridSquare[] squares = m_chunk.getSquaresForLevel(level);
        int CPW = 8;

        for (int y = 0; y < 8; y++) {
            FMODAmbientWallLevelData.FMODAmbientWall wall = null;

            for (int x = 0; x < 8; x++) {
                IsoGridSquare square = squares[x + y * 8];
                if (this.shouldAddNorth(square)) {
                    if (wall == null) {
                        wall = s_wallPool.alloc();
                        wall.owner = this;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                    }
                } else if (wall != null) {
                    wall.x2 = m_chunk.wx * 8 + x;
                    wall.y2 = m_chunk.wy * 8 + y;
                    this.walls.add(wall);
                    wall = null;
                }
            }

            if (wall != null) {
                wall.x2 = m_chunk.wx * 8 + 8;
                wall.y2 = m_chunk.wy * 8 + y;
                this.walls.add(wall);
            }
        }

        for (int xx = 0; xx < 8; xx++) {
            FMODAmbientWallLevelData.FMODAmbientWall wall = null;

            for (int y = 0; y < 8; y++) {
                IsoGridSquare square = squares[xx + y * 8];
                if (this.shouldAddWest(square)) {
                    if (wall == null) {
                        wall = s_wallPool.alloc();
                        wall.owner = this;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                    }
                } else if (wall != null) {
                    wall.x2 = m_chunk.wx * 8 + xx;
                    wall.y2 = m_chunk.wy * 8 + y;
                    this.walls.add(wall);
                    wall = null;
                }
            }

            if (wall != null) {
                wall.x2 = m_chunk.wx * 8 + xx;
                wall.y2 = m_chunk.wy * 8 + 8;
                this.walls.add(wall);
            }
        }
    }

    boolean shouldAddNorth(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            return squareN != null && isOutside(square) != isOutside(squareN) ? passesSoundNorth(square, true) : false;
        }
    }

    public static boolean passesSoundNorth(IsoGridSquare square, boolean bDoorAndWindowRattlesWhenClosed) {
        if (square == null) {
            return false;
        } else {
            if (square.getProperties().has(IsoFlagType.WallN)) {
                IsoObject wall = square.getWall(true);
                if (wall != null) {
                    return wall.getProperties().has(IsoFlagType.HoppableN) || wall.getProperties().has(IsoFlagType.SpearOnlyAttackThrough);
                }
            }

            if (square.getProperties().has(IsoFlagType.WallNW)) {
                return false;
            } else {
                if (!bDoorAndWindowRattlesWhenClosed) {
                    if (square.has(IsoFlagType.doorN)) {
                        IsoObject door = square.getDoor(true);
                        if (isDoorBlocked(door)) {
                            return false;
                        }
                    }

                    if (square.has(IsoFlagType.WindowN) && isWindowBlocked(square.getWindow(true))) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    boolean shouldAddWest(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            return squareW != null && isOutside(square) != isOutside(squareW) ? passesSoundWest(square, true) : false;
        }
    }

    public static boolean passesSoundWest(IsoGridSquare square, boolean bDoorAndWindowRattlesWhenClosed) {
        if (square == null) {
            return false;
        } else {
            if (square.getProperties().has(IsoFlagType.WallW)) {
                IsoObject wall = square.getWall(false);
                if (wall != null) {
                    return wall.getProperties().has(IsoFlagType.HoppableW) || wall.getProperties().has(IsoFlagType.SpearOnlyAttackThrough);
                }
            }

            if (square.getProperties().has(IsoFlagType.WallNW)) {
                return false;
            } else {
                if (!bDoorAndWindowRattlesWhenClosed) {
                    if (square.has(IsoFlagType.doorW)) {
                        IsoObject door = square.getDoor(false);
                        if (isDoorBlocked(door)) {
                            return false;
                        }
                    }

                    if (square.has(IsoFlagType.WindowW) && isWindowBlocked(square.getWindow(false))) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static boolean isOutside(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else if (square.getRoom() != null) {
            return false;
        } else if (square.haveRoof && square.associatedBuilding == null) {
            return false;
        } else {
            if (square.haveRoof) {
                for (int z = square.getZ() - 1; z >= 0; z--) {
                    IsoGridSquare square1 = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), z);
                    if (square1 != null && square1.getRoom() != null) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    static boolean isDoorBlocked(IsoObject object) {
        if (object instanceof IsoDoor door) {
            return !door.IsOpen();
        } else {
            return object instanceof IsoThumpable door ? !door.IsOpen() : false;
        }
    }

    static boolean isWindowBlocked(IsoWindow window) {
        if (window == null) {
            return false;
        } else if (!window.IsOpen() && !window.isDestroyed()) {
            return true;
        } else {
            IsoBarricade barricade1 = window.getBarricadeOnSameSquare();
            if (barricade1 != null && barricade1.isMetal()) {
                return true;
            } else {
                IsoBarricade barricade2 = window.getBarricadeOnOppositeSquare();
                if (barricade2 != null && barricade2.isMetal()) {
                    return true;
                } else {
                    int numPlanks1 = barricade1 == null ? 0 : barricade1.getNumPlanks();
                    int numPlanks2 = barricade2 == null ? 0 : barricade2.getNumPlanks();
                    return numPlanks1 == 4 || numPlanks2 == 4;
                }
            }
        }
    }

    public static FMODAmbientWallLevelData alloc() {
        return s_levelDataPool.alloc();
    }

    @Override
    public void onReleased() {
        IPooledObject.release(this.walls);
        this.dirty = true;
    }

    public static final class FMODAmbientWall extends PooledObject {
        FMODAmbientWallLevelData owner;
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        public boolean isHorizontal() {
            return this.y1 == this.y2;
        }
    }
}
