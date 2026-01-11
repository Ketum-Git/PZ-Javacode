// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;

public class ZomboidNetData implements IZomboidPacket {
    public PacketTypes.PacketType type;
    public short length;
    public ByteBuffer buffer;
    public long connection;
    public long time;

    public ZomboidNetData() {
        this.buffer = ByteBuffer.allocate(2048);
    }

    public ZomboidNetData(int size) {
        this.buffer = ByteBuffer.allocate(size);
    }

    public void reset() {
        this.type = null;
        this.length = 0;
        this.connection = 0L;
        this.buffer.clear();
    }

    public void read(short id, ByteBuffer bb, UdpConnection connection) {
        this.type = PacketTypes.packetTypes.get(id);
        if (this.type == null) {
            DebugLog.Multiplayer.error("Received unknown packet id=%d", id);
        }

        this.connection = connection.getConnectedGUID();
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
