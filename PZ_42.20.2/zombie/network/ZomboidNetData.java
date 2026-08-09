// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferReader;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;

public class ZomboidNetData implements IZomboidPacket {
    public PacketTypes.PacketType type;
    public short length;
    public final ByteBufferReader buffer;
    public long connection;
    public long time;

    public ZomboidNetData() {
        this(2048);
    }

    public ZomboidNetData(int size) {
        this.buffer = new ByteBufferReader(ByteBuffer.allocate(size));
    }

    public void reset() {
        this.type = null;
        this.length = 0;
        this.connection = 0L;
        this.buffer.clear();
    }

    public void read(short id, ByteBufferReader bb, UdpConnection connection) {
        this.type = PacketTypes.packetTypes.get(id);
        if (this.type == null) {
            DebugType.Multiplayer.error("Received unknown packet id=%d", id);
        }

        this.connection = connection.getConnectedGUID();
        this.buffer.put(bb.bb);
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
