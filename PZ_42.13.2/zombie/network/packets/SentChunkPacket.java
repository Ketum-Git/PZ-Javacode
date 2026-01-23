// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ClientChunkRequest;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 4, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 0)
public class SentChunkPacket implements INetworkPacket {
    static final int chunkSize = 1000;
    int requestNumber;
    int fileSize;
    int numChunks;
    int bytesSent;
    int chunkIndex;
    int bytesToSend;
    private byte[] inMemoryZip;

    public void setChunk(ClientChunkRequest.Chunk chunk, int _fileSize, byte[] _inMemoryZip) {
        this.requestNumber = chunk.requestNumber;
        this.fileSize = _fileSize;
        this.inMemoryZip = _inMemoryZip;
        this.numChunks = this.fileSize / 1000;
        if (this.fileSize % 1000 != 0) {
            this.numChunks++;
        }

        this.bytesSent = 0;
        this.chunkIndex = 0;
    }

    public boolean hasData() {
        return this.chunkIndex < this.numChunks;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.bytesToSend = Math.min(this.fileSize - this.bytesSent, 1000);
        b.putInt(this.requestNumber);
        b.putInt(this.numChunks);
        b.putInt(this.chunkIndex);
        b.putInt(this.fileSize);
        b.putInt(this.bytesSent);
        b.putInt(this.bytesToSend);
        b.bb.put(this.inMemoryZip, this.bytesSent, this.bytesToSend);
        this.chunkIndex++;
        this.bytesSent = this.bytesSent + this.bytesToSend;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
    }
}
