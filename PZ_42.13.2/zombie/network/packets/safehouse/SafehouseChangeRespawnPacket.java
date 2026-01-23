// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHousePlayer;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafehouseChangeRespawnPacket extends SafehouseID implements INetworkPacket, AntiCheatSafeHousePlayer.IAntiCheat {
    @JSONField
    public String player;
    @JSONField
    public boolean doRemove;

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
        this.player = (String)values[1];
        this.doRemove = (Boolean)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.player);
        b.putBoolean(this.doRemove);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.player = GameWindow.ReadString(b);
        this.doRemove = b.get() != 0;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.player)) {
            DebugLog.Multiplayer.error("player is not set");
            return false;
        } else if (!this.getSafehouse().getPlayers().contains(this.player) && !this.getSafehouse().isOwner(this.player)) {
            DebugLog.Multiplayer.error("player is not member or owner");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getSafehouse().setRespawnInSafehouse(this.doRemove, this.player);

        for (UdpConnection c : GameServer.udpEngine.connections) {
            if (c.isFullyConnected()) {
                INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
            }
        }
    }

    @Override
    public String getPlayer() {
        return this.player;
    }
}
