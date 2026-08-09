// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferReader;
import zombie.network.IZomboidPacket;

public final class ZomboidNetData implements IZomboidPacket {
    public short type;
    public short length;
    public ByteBuffer buffer;
    public ByteBufferReader bufferReader;
    public UdpConnection connection;

    public ZomboidNetData() {
        this(2048);
    }

    public ZomboidNetData(int size) {
        this.buffer = ByteBuffer.allocate(size);
        this.bufferReader = new ByteBufferReader(this.buffer);
    }

    public void reset() {
        this.type = 0;
        this.length = 0;
        this.buffer.clear();
        this.connection = null;
    }

    public void read(short id, ByteBuffer bb, UdpConnection connection) {
        this.type = id;
        this.connection = connection;
        this.buffer.put(bb);
        this.buffer.flip();
    }

    @Override
    public boolean isConnect() {
        return false;
    }

    @Override
    public boolean isDisconnect() {
        return false;
    }
}
