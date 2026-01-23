// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayList;
import zombie.popman.ObjectPool;

final class ChunkUpdateTaskLevel {
    final int[][] data = new int[8][8];
    final short[][] cost = new short[8][8];
    final ArrayList<SlopedSurface> slopedSurfaces = new ArrayList<>();
    static final ObjectPool<ChunkUpdateTaskLevel> pool = new ObjectPool<>(ChunkUpdateTaskLevel::new);

    static ChunkUpdateTaskLevel alloc() {
        return pool.alloc();
    }

    void release() {
        SlopedSurface.pool.releaseAll(this.slopedSurfaces);
        this.slopedSurfaces.clear();
        pool.release(this);
    }
}
