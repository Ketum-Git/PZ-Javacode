// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SetMultiplierPacket implements INetworkPacket {
    @JSONField
    float multiplier = 1.0F;

    @Override
    public void setData(Object... values) {
        this.multiplier = GameTime.instance.getMultiplier();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putFloat(this.multiplier);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.multiplier = b.getFloat();
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameTime.instance.setMultiplier(this.multiplier);
    }
}
