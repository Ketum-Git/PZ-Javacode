// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.popman.ObjectPool;

class ChunkUpdateTask implements IPathfindTask {
    protected static final int SQUARES_PER_CHUNK = 8;
    protected static final int LEVELS_PER_CHUNK = 64;
    int wx;
    int wy;
    short loadId;
    ByteBuffer bb;
    static ByteBuffer bbTemp;
    private static final int BLOCK_SIZE = 256;
    static final ObjectPool<ChunkUpdateTask> pool = new ObjectPool<>(ChunkUpdateTask::new);

    private static int bufferSize(int size) {
        return (size + 256 - 1) / 256 * 256;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb, int capacity) {
        if (bb == null || bb.capacity() < capacity) {
            bb = ByteBuffer.allocateDirect(bufferSize(capacity));
        }

        return bb;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb) {
        if (bb == null) {
            return ByteBuffer.allocateDirect(256);
        } else if (bb.capacity() - bb.position() < 256) {
            ByteBuffer newBB = ensureCapacity(null, bb.position() + 256);
            newBB.put(0, bb, 0, bb.position());
            return newBB.position(bb.position());
        } else {
            return bb;
        }
    }

    ChunkUpdateTask init(IsoChunk chunk) {
        this.wx = chunk.wx;
        this.wy = chunk.wy;
        this.loadId = chunk.getLoadID();
        this.bb = ensureCapacity(this.bb);
        this.bb.clear();
        this.bb.putInt(chunk.minLevel + 32);
        this.bb.putInt(chunk.maxLevel + 32);
        bbTemp = ensureCapacity(bbTemp);
        bbTemp.clear();
        int numSlopedSurfaces = 0;

        for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    this.bb = ensureCapacity(this.bb);
                    IsoGridSquare sq = chunk.getGridSquare(x, y, z);
                    if (sq == null) {
                        this.bb.putInt(0);
                        this.bb.putShort((short)0);
                    } else {
                        int bits = SquareUpdateTask.getBits(sq);
                        short cost = SquareUpdateTask.getCost(sq);
                        this.bb.putInt(bits);
                        this.bb.putShort(cost);
                        IsoDirections slopeDir = sq.getSlopedSurfaceDirection();
                        if (slopeDir != null) {
                            bbTemp = ensureCapacity(bbTemp);
                            bbTemp.put((byte)x);
                            bbTemp.put((byte)y);
                            bbTemp.put((byte)(z + 32));
                            bbTemp.put((byte)slopeDir.indexUnmodified());
                            bbTemp.putFloat(sq.getSlopedSurfaceHeightMin());
                            bbTemp.putFloat(sq.getSlopedSurfaceHeightMax());
                            numSlopedSurfaces++;
                        }
                    }
                }
            }
        }

        this.bb.putShort((short)numSlopedSurfaces);
        if (numSlopedSurfaces > 0) {
            int numBytes = this.bb.position() + bbTemp.position();
            if (numBytes > this.bb.capacity()) {
                ByteBuffer newBB = ByteBuffer.allocateDirect(bufferSize(numBytes));
                newBB.put(0, this.bb, 0, this.bb.position());
                newBB.position(this.bb.position());
                this.bb = newBB;
            }

            this.bb.put(this.bb.position(), bbTemp, 0, bbTemp.position());
            this.bb.position(this.bb.position() + bbTemp.position());
        }

        this.bb.flip();
        return this;
    }

    @Override
    public void execute() {
        PathfindNative.updateChunk(this.loadId, this.wx, this.wy, this.bb);
    }

    static ChunkUpdateTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}
