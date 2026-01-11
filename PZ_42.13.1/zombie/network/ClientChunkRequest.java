// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.raknet.UdpConnection;

public class ClientChunkRequest {
    public ArrayList<ClientChunkRequest.Chunk> chunks = new ArrayList<>(20);
    private static final ConcurrentLinkedQueue<ClientChunkRequest.Chunk> freeChunks = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<ByteBuffer> freeBuffers = new ConcurrentLinkedQueue<>();
    public boolean largeArea;
    int minX;
    int maxX;
    int minY;
    int maxY;

    public ClientChunkRequest.Chunk getChunk() {
        ClientChunkRequest.Chunk chunk = freeChunks.poll();
        if (chunk == null) {
            chunk = new ClientChunkRequest.Chunk();
        }

        return chunk;
    }

    public void releaseChunk(ClientChunkRequest.Chunk chunk) {
        this.releaseBuffer(chunk);
        freeChunks.add(chunk);
    }

    public void getByteBuffer(ClientChunkRequest.Chunk chunk) {
        chunk.bb = freeBuffers.poll();
        if (chunk.bb == null) {
            chunk.bb = ByteBuffer.allocate(16384);
        } else {
            chunk.bb.clear();
        }
    }

    public void releaseBuffer(ClientChunkRequest.Chunk chunk) {
        if (chunk.bb != null) {
            freeBuffers.add(chunk.bb);
            chunk.bb = null;
        }
    }

    public void releaseBuffers() {
        for (int i = 0; i < this.chunks.size(); i++) {
            this.chunks.get(i).bb = null;
        }
    }

    public void unpack(ByteBuffer bb, UdpConnection connection) {
        for (int i = 0; i < this.chunks.size(); i++) {
            this.releaseBuffer(this.chunks.get(i));
        }

        freeChunks.addAll(this.chunks);
        this.chunks.clear();
        int count = bb.getInt();

        for (int n = 0; n < count; n++) {
            ClientChunkRequest.Chunk chunk = this.getChunk();
            chunk.requestNumber = bb.getInt();
            chunk.wx = bb.getInt();
            chunk.wy = bb.getInt();
            chunk.crc = bb.getLong();
            this.chunks.add(chunk);
        }

        this.largeArea = false;
    }

    public void unpackLargeArea(ByteBuffer bb, UdpConnection connection) {
        for (int i = 0; i < this.chunks.size(); i++) {
            this.releaseBuffer(this.chunks.get(i));
        }

        freeChunks.addAll(this.chunks);
        this.chunks.clear();
        this.minX = bb.getInt();
        this.minY = bb.getInt();
        this.maxX = bb.getInt();
        this.maxY = bb.getInt();

        for (int x = this.minX; x < this.maxX; x++) {
            for (int y = this.minY; y < this.maxY; y++) {
                ClientChunkRequest.Chunk chunk = this.getChunk();
                chunk.requestNumber = bb.getInt();
                chunk.wx = x;
                chunk.wy = y;
                chunk.crc = 0L;
                this.releaseBuffer(chunk);
                this.chunks.add(chunk);
            }
        }

        this.largeArea = true;
    }

    public static final class Chunk {
        public int requestNumber;
        public int wx;
        public int wy;
        public long crc;
        public ByteBuffer bb;
    }
}
