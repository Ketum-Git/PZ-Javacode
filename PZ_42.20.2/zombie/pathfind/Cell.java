// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;

final class Cell {
    PolygonalMap2 map;
    public short cx;
    public short cy;
    public Chunk[][] chunks;
    static final ArrayDeque<Cell> pool = new ArrayDeque<>();

    Cell init(PolygonalMap2 map, int x, int y) {
        this.map = map;
        this.cx = (short)x;
        this.cy = (short)y;
        return this;
    }

    Chunk getChunkFromChunkPos(int wx, int wy) {
        if (this.chunks == null) {
            return null;
        } else {
            wx -= this.cx * 32;
            wy -= this.cy * 32;
            return wx >= 0 && wx < 32 && wy >= 0 && wy < 32 ? this.chunks[wx][wy] : null;
        }
    }

    Chunk allocChunkIfNeeded(int wx, int wy) {
        wx -= this.cx * 32;
        wy -= this.cy * 32;
        if (wx >= 0 && wx < 32 && wy >= 0 && wy < 32) {
            if (this.chunks == null) {
                this.chunks = new Chunk[32][32];
            }

            if (this.chunks[wx][wy] == null) {
                this.chunks[wx][wy] = Chunk.alloc();
            }

            this.chunks[wx][wy].init(this.cx * 32 + wx, this.cy * 32 + wy);
            return this.chunks[wx][wy];
        } else {
            return null;
        }
    }

    void removeChunk(int wx, int wy) {
        if (this.chunks != null) {
            wx -= this.cx * 32;
            wy -= this.cy * 32;
            if (wx >= 0 && wx < 32 && wy >= 0 && wy < 32) {
                Chunk chunk = this.chunks[wx][wy];
                if (chunk != null) {
                    chunk.clear();
                    chunk.release();
                    this.chunks[wx][wy] = null;
                }
            }
        }
    }

    void clearChunks() {
        if (this.chunks != null) {
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    Chunk chunk = this.chunks[x][y];
                    if (chunk != null) {
                        chunk.clear();
                        chunk.release();
                        this.chunks[x][y] = null;
                    }
                }
            }
        }
    }

    static Cell alloc() {
        return pool.isEmpty() ? new Cell() : pool.pop();
    }

    void release() {
        this.clearChunks();

        assert !pool.contains(this);

        pool.push(this);
    }
}
