// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;

final class ChunkUpdateTask implements IChunkTask {
    PolygonalMap2 map;
    int wx;
    int wy;
    ChunkUpdateTaskLevel[] levels = new ChunkUpdateTaskLevel[1];
    int minLevel = 32;
    int maxLevel = 32;
    static final ArrayDeque<ChunkUpdateTask> pool = new ArrayDeque<>();

    ChunkUpdateTask() {
        this.levels[0] = new ChunkUpdateTaskLevel();
    }

    void setMinMaxLevel(int minLevel, int maxLevel) {
        if (minLevel != this.minLevel || maxLevel != this.maxLevel) {
            for (int z = this.minLevel; z <= this.maxLevel; z++) {
                if (z < minLevel || z > maxLevel) {
                    ChunkUpdateTaskLevel chunkLevel = this.levels[z - this.minLevel];
                    if (chunkLevel != null) {
                        chunkLevel.release();
                        this.levels[z - this.minLevel] = null;
                    }
                }
            }

            ChunkUpdateTaskLevel[] newLevels = new ChunkUpdateTaskLevel[maxLevel - minLevel + 1];

            for (int zx = minLevel; zx <= maxLevel; zx++) {
                if (zx >= this.minLevel && zx <= this.maxLevel) {
                    newLevels[zx - minLevel] = this.levels[zx - this.minLevel];
                } else {
                    newLevels[zx - minLevel] = ChunkUpdateTaskLevel.alloc();
                }
            }

            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.levels = newLevels;
        }
    }

    ChunkUpdateTask init(PolygonalMap2 map, IsoChunk chunk) {
        this.map = map;
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        this.setMinMaxLevel(chunk.minLevel + 32, chunk.maxLevel + 32);

        for (int z = this.minLevel; z <= this.maxLevel; z++) {
            ChunkUpdateTaskLevel chunkLevel = this.levels[z - this.minLevel];
            SlopedSurface.pool.releaseAll(chunkLevel.slopedSurfaces);
            chunkLevel.slopedSurfaces.clear();

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    IsoGridSquare sq = chunk.getGridSquare(x, y, z - 32);
                    if (sq == null) {
                        chunkLevel.data[x][y] = 0;
                        chunkLevel.cost[x][y] = 0;
                    } else {
                        chunkLevel.data[x][y] = SquareUpdateTask.getBits(sq);
                        chunkLevel.cost[x][y] = SquareUpdateTask.getCost(sq);
                        if (sq.hasSlopedSurface()) {
                            SlopedSurface slopedSurface = SlopedSurface.alloc();
                            slopedSurface.x = (byte)x;
                            slopedSurface.y = (byte)y;
                            slopedSurface.direction = sq.getSlopedSurfaceDirection();
                            slopedSurface.heightMin = sq.getSlopedSurfaceHeightMin();
                            slopedSurface.heightMax = sq.getSlopedSurfaceHeightMax();
                            chunkLevel.slopedSurfaces.add(slopedSurface);
                        }
                    }
                }
            }
        }

        return this;
    }

    @Override
    public void execute() {
        Chunk chunk = this.map.allocChunkIfNeeded(this.wx, this.wy);
        chunk.setData(this);
        ChunkDataZ.epochCount++;
    }

    static ChunkUpdateTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new ChunkUpdateTask() : pool.pop();
        }
    }

    @Override
    public void release() {
        synchronized (pool) {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
