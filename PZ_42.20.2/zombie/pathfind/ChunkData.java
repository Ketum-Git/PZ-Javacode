// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.Arrays;
import zombie.util.list.PZArrayUtil;

final class ChunkData {
    final ChunkDataZ[] data = new ChunkDataZ[64];

    public ChunkDataZ init(Chunk chunk, int z) {
        if (this.data[z] == null) {
            this.data[z] = ChunkDataZ.pool.alloc();
            this.data[z].init(chunk, z);
        } else if (this.data[z].epoch != ChunkDataZ.epochCount) {
            this.data[z].clear();
            this.data[z].init(chunk, z);
        }

        return this.data[z];
    }

    public void clear() {
        PZArrayUtil.forEach(this.data, e -> {
            if (e != null) {
                e.clear();
                ChunkDataZ.pool.release(e);
            }
        });
        Arrays.fill(this.data, null);
    }
}
