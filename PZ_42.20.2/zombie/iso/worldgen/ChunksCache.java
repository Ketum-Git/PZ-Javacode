// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.HashMap;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.worldgen.utils.ChunkCoord;

public class ChunksCache extends HashMap<ChunkCoord, IsoChunk> {
    private int minChunkX = Integer.MAX_VALUE;
    private int maxChunkX = Integer.MIN_VALUE;
    private int minChunkY = Integer.MAX_VALUE;
    private int maxChunkY = Integer.MIN_VALUE;
    private int minTileX = Integer.MAX_VALUE;
    private int maxTileX = Integer.MIN_VALUE;
    private int minTileY = Integer.MAX_VALUE;
    private int maxTileY = Integer.MIN_VALUE;

    public IsoChunk put(ChunkCoord key, IsoChunk value) {
        this.minChunkX = Math.min(key.x(), this.minChunkX);
        this.maxChunkX = Math.max(key.x(), this.maxChunkX);
        this.minChunkY = Math.min(key.y(), this.minChunkY);
        this.maxChunkY = Math.max(key.y(), this.maxChunkY);
        this.minTileX = this.minChunkX * 8;
        this.maxTileX = (this.maxChunkX + 1) * 8;
        this.minTileY = this.minChunkY * 8;
        this.maxTileY = (this.maxChunkY + 1) * 8;
        return super.put(key, value);
    }

    public int getMinTileX() {
        return this.minTileX;
    }

    public int getMaxTileX() {
        return this.maxTileX;
    }

    public int getMinTileY() {
        return this.minTileY;
    }

    public int getMaxTileY() {
        return this.maxTileY;
    }

    public int getMinChunkX() {
        return this.minChunkX;
    }

    public int getMaxChunkX() {
        return this.maxChunkX;
    }

    public int getMinChunkY() {
        return this.minChunkY;
    }

    public int getMaxChunkY() {
        return this.maxChunkY;
    }

    public IsoGridSquare getGridSquare(int localX, int localY, int localZ) {
        IsoChunk ch = this.get(new ChunkCoord(this.getMinChunkX() + localX / 8, this.getMinChunkY() + localY / 8));
        return ch == null ? null : ch.getGridSquare(localX - 8 * (localX / 8), localY - 8 * (localY / 8), localZ);
    }
}
