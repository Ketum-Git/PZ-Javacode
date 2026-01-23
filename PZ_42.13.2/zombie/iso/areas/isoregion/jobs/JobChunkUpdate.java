// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.jobs;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.areas.isoregion.ChunkUpdate;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.network.GameClient;

/**
 * TurboTuTone.
 */
public class JobChunkUpdate extends RegionJob {
    private final ByteBuffer buffer = ByteBuffer.allocate(65536);
    private int chunkCount;
    private int bufferMaxBytes;
    private long netTimeStamp = -1L;
    private UdpConnection targetConn;

    protected JobChunkUpdate() {
        super(RegionJobType.ChunkUpdate);
    }

    @Override
    protected void reset() {
        this.chunkCount = 0;
        this.bufferMaxBytes = 0;
        this.netTimeStamp = -1L;
        this.targetConn = null;
        this.buffer.clear();
    }

    public UdpConnection getTargetConn() {
        return this.targetConn;
    }

    public void setTargetConn(UdpConnection conn) {
        this.targetConn = conn;
    }

    public int getChunkCount() {
        return this.chunkCount;
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public long getNetTimeStamp() {
        return this.netTimeStamp;
    }

    public void setNetTimeStamp(long netTimeStamp) {
        this.netTimeStamp = netTimeStamp;
    }

    public boolean readChunksPacket(DataRoot root, List<Integer> knownChunks) {
        this.buffer.position(0);
        int totalLen = this.buffer.getInt();
        int count = this.buffer.getInt();

        for (int i = 0; i < count; i++) {
            int packlen = this.buffer.getInt();
            int worldVers = this.buffer.getInt();
            int chunkx = this.buffer.getInt();
            int chunky = this.buffer.getInt();
            root.select.reset(chunkx * 8, chunky * 8, 0, true, false);
            if (GameClient.client) {
                if (this.netTimeStamp != -1L && this.netTimeStamp < root.select.chunk.getLastUpdateStamp()) {
                    int startPos = this.buffer.position();
                    int chunklen = this.buffer.getInt();
                    this.buffer.position(startPos + chunklen);
                    continue;
                }

                root.select.chunk.setLastUpdateStamp(this.netTimeStamp);
            } else {
                int hashid = IsoRegions.hash(chunkx, chunky);
                if (!knownChunks.contains(hashid)) {
                    knownChunks.add(hashid);
                }
            }

            root.select.chunk.load(this.buffer, worldVers, true);
            root.select.chunk.setDirtyAllActive();
        }

        return true;
    }

    public boolean saveChunksToDisk() {
        if (Core.getInstance().isNoSave()) {
            return true;
        } else if (this.chunkCount <= 0) {
            return false;
        } else {
            this.buffer.position(0);
            int totalLen = this.buffer.getInt();
            int count = this.buffer.getInt();

            for (int i = 0; i < count; i++) {
                this.buffer.mark();
                int packlen = this.buffer.getInt();
                int worldVers = this.buffer.getInt();
                int chunkx = this.buffer.getInt();
                int chunky = this.buffer.getInt();
                this.buffer.reset();
                File saveFile = IsoRegions.getChunkFile(chunkx, chunky);

                try {
                    FileOutputStream output = new FileOutputStream(saveFile);
                    output.getChannel().truncate(0L);
                    output.write(this.buffer.array(), this.buffer.position(), packlen);
                    output.flush();
                    output.close();
                } catch (Exception var10) {
                    DebugLog.log(var10.getMessage());
                    var10.printStackTrace();
                }

                this.buffer.position(this.buffer.position() + packlen);
            }

            return true;
        }
    }

    public boolean saveChunksToNetBuffer(ByteBuffer bb) {
        IsoRegions.log("Server max bytes buffer = " + this.bufferMaxBytes + ", chunks = " + this.chunkCount);
        bb.put(this.buffer.array(), 0, this.bufferMaxBytes);
        return true;
    }

    public boolean readChunksFromNetBuffer(ByteBuffer bb, long serverTimeStamp) {
        this.netTimeStamp = serverTimeStamp;
        bb.mark();
        this.bufferMaxBytes = bb.getInt();
        this.chunkCount = bb.getInt();
        bb.reset();
        IsoRegions.log("Client max bytes buffer = " + this.bufferMaxBytes + ", chunks = " + this.chunkCount);
        this.buffer.position(0);
        this.buffer.put(bb.array(), bb.position(), this.bufferMaxBytes);
        return true;
    }

    public boolean canAddChunk() {
        return this.buffer.position() + 2076 < this.buffer.capacity();
    }

    private int startBufferBlock() {
        if (this.chunkCount == 0) {
            this.buffer.position(0);
            this.buffer.putInt(0);
            this.buffer.putInt(0);
        }

        int bufferStartPos = this.buffer.position();
        this.buffer.putInt(0);
        return bufferStartPos;
    }

    private void endBufferBlock(int bufferStartPos) {
        this.bufferMaxBytes = this.buffer.position();
        this.buffer.position(bufferStartPos);
        this.buffer.putInt(this.bufferMaxBytes - bufferStartPos);
        this.chunkCount++;
        this.buffer.position(0);
        this.buffer.putInt(this.bufferMaxBytes);
        this.buffer.putInt(this.chunkCount);
        this.buffer.position(this.bufferMaxBytes);
    }

    public boolean addChunkFromDataChunk(DataChunk chunk) {
        if (this.buffer.position() + 2076 >= this.buffer.capacity()) {
            return false;
        } else {
            int bufferStartPos = this.startBufferBlock();
            this.buffer.putInt(241);
            this.buffer.putInt(chunk.getChunkX());
            this.buffer.putInt(chunk.getChunkY());
            chunk.save(this.buffer);
            this.endBufferBlock(bufferStartPos);
            return true;
        }
    }

    public boolean addChunkFromIsoChunk(IsoChunk isoChunk) {
        if (this.buffer.position() + 2076 >= this.buffer.capacity()) {
            return false;
        } else {
            int bufferStartPos = this.startBufferBlock();
            this.buffer.putInt(241);
            this.buffer.putInt(isoChunk.wx);
            this.buffer.putInt(isoChunk.wy);
            ChunkUpdate.writeIsoChunkIntoBuffer(isoChunk, this.buffer);
            this.endBufferBlock(bufferStartPos);
            return true;
        }
    }

    public boolean addChunkFromFile(ByteBuffer bb) {
        if (this.buffer.position() + bb.limit() >= this.buffer.capacity()) {
            return false;
        } else {
            int numBytes = bb.getInt();
            int version = bb.getInt();
            int wx = bb.getInt();
            int wy = bb.getInt();
            int bufferStartPos = this.startBufferBlock();
            this.buffer.putInt(version);
            this.buffer.putInt(wx);
            this.buffer.putInt(wy);
            bb.mark();
            int length = bb.getInt();
            bb.reset();
            this.buffer.put(bb.array(), bb.position(), length);
            this.endBufferBlock(bufferStartPos);
            return true;
        }
    }
}
