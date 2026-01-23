// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class HelicopterPacket implements INetworkPacket {
    float x;
    float y;
    boolean active;

    public void set(float x, float y, boolean active) {
        this.x = x;
        this.y = y;
        this.active = active;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.active = b.get() == 1;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putBoolean(this.active);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance != null) {
            IsoWorld.instance.helicopter.clientSync(this.x, this.y, this.active);
        }
    }
}
