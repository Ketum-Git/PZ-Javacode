// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

public final class Chunk {
    public short wx;
    public short wy;
    ChunkLevel[] levels = new ChunkLevel[1];
    final ChunkData collision = new ChunkData();
    int minLevel = 32;
    int maxLevel = 32;
    final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList<>();
    static final ArrayDeque<Chunk> pool = new ArrayDeque<>();

    Chunk() {
        this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
    }

    Chunk init(int wx, int wy) {
        this.wx = (short)wx;
        this.wy = (short)wy;
        return this;
    }

    void clear() {
        for (int z = this.minLevel; z <= this.maxLevel; z++) {
            ChunkLevel chunkLevel = this.levels[z - this.minLevel];
            if (chunkLevel != null) {
                chunkLevel.clear();
                chunkLevel.release();
                this.levels[z - this.minLevel] = null;
            }
        }

        this.wx = this.wy = -1;
        this.levels = new ChunkLevel[1];
        this.minLevel = this.maxLevel = 32;
        this.levels[0] = ChunkLevel.alloc().init(this, this.minLevel);
    }

    public int getMinX() {
        return this.wx * 8;
    }

    public int getMinY() {
        return this.wy * 8;
    }

    public int getMaxX() {
        return (this.wx + 1) * 8 - 1;
    }

    public int getMaxY() {
        return (this.wy + 1) * 8 - 1;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public boolean isValidLevel(int level) {
        return level >= this.getMinLevel() && level <= this.getMaxLevel();
    }

    public boolean contains(int worldX, int worldY) {
        return worldX >= this.getMinX() && worldY >= this.getMinY() && worldX <= this.getMaxX() && worldY <= this.getMaxY();
    }

    void setMinMaxLevel(int minLevel, int maxLevel) {
        if (minLevel != this.minLevel || maxLevel != this.maxLevel) {
            for (int z = this.minLevel; z <= this.maxLevel; z++) {
                if (z < minLevel || z > maxLevel) {
                    ChunkLevel chunkLevel = this.levels[z - this.minLevel];
                    if (chunkLevel != null) {
                        chunkLevel.clear();
                        chunkLevel.release();
                        this.levels[z - this.minLevel] = null;
                    }
                }
            }

            ChunkLevel[] newLevels = new ChunkLevel[maxLevel - minLevel + 1];

            for (int zx = minLevel; zx <= maxLevel; zx++) {
                if (this.isValidLevel(zx)) {
                    newLevels[zx - minLevel] = this.levels[zx - this.minLevel];
                } else {
                    newLevels[zx - minLevel] = ChunkLevel.alloc().init(this, zx);
                }
            }

            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.levels = newLevels;
        }
    }

    public ChunkLevel getLevelData(int level) {
        return this.isValidLevel(level) ? this.levels[level - this.minLevel] : null;
    }

    Square getSquare(int x, int y, int z) {
        ChunkLevel chunkLevel = this.getLevelData(z);
        return chunkLevel == null ? null : chunkLevel.getSquare(x, y);
    }

    public Square[][] getSquaresForLevel(int z) {
        ChunkLevel chunkLevel = this.getLevelData(z);
        return chunkLevel == null ? null : chunkLevel.squares;
    }

    void setData(ChunkUpdateTask task) {
        this.setMinMaxLevel(task.minLevel, task.maxLevel);

        for (int z = this.minLevel; z <= this.maxLevel; z++) {
            ChunkUpdateTaskLevel taskLevel = task.levels[z - this.minLevel];
            ChunkLevel chunkLevel = this.getLevelData(z);

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int bits = taskLevel.data[x][y];
                    short cost = taskLevel.cost[x][y];
                    chunkLevel.setBits(x, y, bits, cost);
                    Square square = chunkLevel.squares[x][y];
                    if (square != null) {
                        square.slopedSurfaceDirection = null;
                        square.slopedSurfaceHeightMin = 0.0F;
                        square.slopedSurfaceHeightMax = 0.0F;
                    }
                }
            }

            for (int i = 0; i < taskLevel.slopedSurfaces.size(); i++) {
                SlopedSurface slopedSurface = taskLevel.slopedSurfaces.get(i);
                Square square = chunkLevel.squares[slopedSurface.x][slopedSurface.y];
                square.slopedSurfaceDirection = slopedSurface.direction;
                square.slopedSurfaceHeightMin = slopedSurface.heightMin;
                square.slopedSurfaceHeightMax = slopedSurface.heightMax;
            }
        }
    }

    boolean setData(SquareUpdateTask task) {
        int x = task.x - this.wx * 8;
        int y = task.y - this.wy * 8;
        if (x < 0 || x >= 8) {
            return false;
        } else if (y >= 0 && y < 8) {
            Square[][] squares = this.getSquaresForLevel(task.z);
            Square square = squares[x][y];
            if (task.bits == 0) {
                if (square != null) {
                    square.release();
                    squares[x][y] = null;
                    return true;
                }
            } else {
                if (square == null) {
                    square = Square.alloc().init(task.x, task.y, task.z);
                    squares[x][y] = square;
                }

                boolean bChanged = square.bits != task.bits || square.cost != task.cost;
                if (task.slopedSurface == null) {
                    bChanged |= square.slopedSurfaceDirection != null || square.slopedSurfaceHeightMin != 0.0F || square.slopedSurfaceHeightMax != 0.0F;
                    square.slopedSurfaceDirection = null;
                    square.slopedSurfaceHeightMin = 0.0F;
                    square.slopedSurfaceHeightMax = 0.0F;
                } else {
                    bChanged |= square.slopedSurfaceDirection != task.slopedSurface.direction
                        || square.slopedSurfaceHeightMin != task.slopedSurface.heightMin
                        || square.slopedSurfaceHeightMax != task.slopedSurface.heightMax;
                    square.slopedSurfaceDirection = task.slopedSurface.direction;
                    square.slopedSurfaceHeightMin = task.slopedSurface.heightMin;
                    square.slopedSurfaceHeightMax = task.slopedSurface.heightMax;
                }

                if (bChanged) {
                    square.bits = task.bits;
                    square.cost = task.cost;
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    void addVisibilityGraph(VisibilityGraph graph) {
        if (!this.visibilityGraphs.contains(graph)) {
            this.visibilityGraphs.add(graph);
        }
    }

    void removeVisibilityGraph(VisibilityGraph graph) {
        this.visibilityGraphs.remove(graph);
    }

    static Chunk alloc() {
        return pool.isEmpty() ? new Chunk() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
