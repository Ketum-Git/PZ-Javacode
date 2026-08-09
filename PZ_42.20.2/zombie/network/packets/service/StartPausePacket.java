// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class StartPausePacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameClient.setIsClientPaused(true);
        LuaEventManager.triggerEvent("OnServerStartSaving");
    }
}
