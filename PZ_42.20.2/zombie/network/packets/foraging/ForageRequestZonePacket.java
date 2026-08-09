// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.foraging;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class ForageRequestZonePacket implements INetworkPacket {
    @JSONField
    private final PlayerID player = new PlayerID();
    @JSONField
    private String focus;

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.focus = values[1].toString();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.focus);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.player.parse(b, connection);
        this.focus = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LuaEventManager.triggerEvent("OnForageRequestZone", this.player.getPlayer(), this.focus);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.player.isConsistent(connection);
    }
}
