// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class StartPausePacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameClient.setIsClientPaused(true);
        LuaEventManager.triggerEvent("OnServerStartSaving");
    }

    @Override
    public void setData(Object... values) {
    }
}
