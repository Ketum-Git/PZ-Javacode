// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.nio.ByteBuffer;
import zombie.network.IZomboidPacket;

public final class ZomboidNetData implements IZomboidPacket {
    public short type;
    public short length;
    public ByteBuffer buffer;
    public UdpConnection connection;

    public ZomboidNetData() {
        this.buffer = ByteBuffer.allocate(2048);
    }

    public ZomboidNetData(int size) {
        this.buffer = ByteBuffer.allocate(size);
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
