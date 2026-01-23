// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.iso.IsoChunk;

final class ChunkRemoveTask implements IChunkTask {
    PolygonalMap2 map;
    int wx;
    int wy;
    static final ArrayDeque<ChunkRemoveTask> pool = new ArrayDeque<>();

    ChunkRemoveTask init(PolygonalMap2 map, IsoChunk chunk) {
        this.map = map;
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        return this;
    }

    @Override
    public void execute() {
        Cell cell = this.map.getCellFromChunkPos(this.wx, this.wy);
        cell.removeChunk(this.wx, this.wy);
    }

    static ChunkRemoveTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new ChunkRemoveTask() : pool.pop();
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
