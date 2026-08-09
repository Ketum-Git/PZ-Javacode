// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseMember;
import zombie.network.fields.SafeHousePlayer;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHouseMember
)
public class SafehouseChangeRespawnPacket extends SafeHousePlayer implements INetworkPacket, AntiCheatSafeHouseMember.IAntiCheat {
    @JSONField
    private boolean doRemove;

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0], (String)values[1]);
        this.doRemove = (Boolean)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putBoolean(this.doRemove);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.doRemove = b.getBoolean();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getSafehouse().setRespawnInSafehouse(this.doRemove, this.getUsername());
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, this.getSafehouse());
    }
}
