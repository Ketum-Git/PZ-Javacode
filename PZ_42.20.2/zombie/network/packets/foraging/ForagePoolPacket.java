// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.foraging;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ForagePoolPacket implements INetworkPacket {
    @JSONField
    private final PlayerID player = new PlayerID();
    @JSONField
    private String zoneId;
    private KahluaTable icons;

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.zoneId = values[1].toString();
        this.icons = (KahluaTable)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.zoneId);

        try {
            this.icons.save(b.bb);
        } catch (IOException var3) {
            ExceptionLogger.logException(var3, "ForagePool save error", DebugType.Multiplayer, LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.zoneId = b.getUTF();
        this.icons = LuaManager.platform.newTable();

        try {
            this.icons.load(b.bb, 249);
        } catch (Exception var4) {
            ExceptionLogger.logException(var4, "ForagePool load error", DebugType.Multiplayer, LogSeverity.Error);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("OnForagePool", this.player.getPlayer(), this.zoneId, this.icons);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.player.isConsistent(connection);
    }
}
