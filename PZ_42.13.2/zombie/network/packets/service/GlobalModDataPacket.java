// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class GlobalModDataPacket implements INetworkPacket {
    String tag;
    KahluaTable table;

    public void set(String tag, KahluaTable table) {
        this.tag = tag;
        this.table = table;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.tag);
        b.putByte((byte)1);

        try {
            this.table.save(b.bb);
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        try {
            String tag = GameWindow.ReadString(b);
            if (b.get() != 1) {
                LuaEventManager.triggerEvent("OnReceiveGlobalModData", tag, false);
                return;
            }

            KahluaTable table = LuaManager.platform.newTable();
            table.load(b, 241);
            LuaEventManager.triggerEvent("OnReceiveGlobalModData", tag, table);
        } catch (Exception var5) {
            var5.printStackTrace();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        INetworkPacket.super.processServer(packetType, connection);
    }
}
