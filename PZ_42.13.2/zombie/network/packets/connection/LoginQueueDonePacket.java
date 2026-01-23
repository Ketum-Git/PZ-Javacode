// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.JSONField;
import zombie.network.LoginQueue;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class LoginQueueDonePacket implements INetworkPacket {
    @JSONField
    long dt;

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof Long) {
            this.set((Long)values[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(long dt) {
        this.dt = dt;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putLong(this.dt);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.dt = b.getLong();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LoginQueue.receiveLoginQueueDone(this.dt, connection);
    }
}
