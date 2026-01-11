// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.None, handlingType = 4)
public class GoogleAuthRequestPacket implements INetworkPacket {
    @Override
    public void setData(Object... values) {
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnGoogleAuthRequest");
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
    }

    @Override
    public void write(ByteBufferWriter b) {
    }
}
