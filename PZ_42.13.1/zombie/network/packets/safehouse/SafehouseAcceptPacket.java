// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
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
    handlingType = 3,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafehouseAcceptPacket extends SafehouseID implements INetworkPacket, AntiCheatSafeHousePlayer.IAntiCheat {
    @JSONField
    protected String owner;
    @JSONField
    private String invited;
    @JSONField
    private boolean isAccepted;

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
        this.owner = (String)values[1];
        this.invited = ((IsoPlayer)values[2]).getUsername();
        this.isAccepted = (Boolean)values[3];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.owner);
        b.putUTF(this.invited);
        b.putByte((byte)(this.isAccepted ? 1 : 0));
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.owner = GameWindow.ReadString(b);
        this.invited = GameWindow.ReadString(b);
        this.isAccepted = b.get() != 0;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.invited)) {
            DebugLog.Multiplayer.error("player is not set");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.owner)) {
            DebugLog.Multiplayer.error("owner is not set");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isAccepted) {
            this.getSafehouse().addPlayer(this.getPlayer());
            LuaEventManager.triggerEvent("AcceptedSafehouseInvite", this.getSafehouse().getTitle(), this.owner);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (SafeHouse.hasSafehouse(this.getPlayer()) != null) {
            DebugLog.Multiplayer.error("player is already member or owner");
        } else if (!this.getSafehouse().haveInvite(this.getPlayer())) {
            DebugLog.Multiplayer.error("invite is not found");
        } else {
            if (this.isAccepted) {
                this.getSafehouse().addPlayer(this.getPlayer());
            }

            this.getSafehouse().removeInvite(this.getPlayer());
            IsoPlayer isoPlayer = GameServer.getPlayerByUserName(this.getPlayer());
            if (isoPlayer != null && GameServer.getConnectionFromPlayer(isoPlayer) != null && !isoPlayer.role.hasCapability(Capability.CanSetupSafehouses)) {
                INetworkPacket.send(GameServer.getConnectionFromPlayer(isoPlayer), PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), true);
            }

            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (c.isFullyConnected()) {
                    INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
                }
            }

            this.sendToClients(packetType, null);
        }
    }

    @Override
    public String getPlayer() {
        return this.invited;
    }
}
