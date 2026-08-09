// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
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
        b.putBoolean(true);

        try {
            this.table.save(b.bb);
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        try {
            String tag = b.getUTF();
            if (!b.getBoolean()) {
                LuaEventManager.triggerEvent("OnReceiveGlobalModData", tag, false);
                return;
            }

            KahluaTable table = LuaManager.platform.newTable();
            table.load(b.bb, 249);
            LuaEventManager.triggerEvent("OnReceiveGlobalModData", tag, table);
        } catch (Exception var5) {
            DebugType.General.printException(var5, LogSeverity.Error);
        }
    }
}
