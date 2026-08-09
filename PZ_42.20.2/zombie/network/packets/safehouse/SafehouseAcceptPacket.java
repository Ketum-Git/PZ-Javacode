// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseNotMember;
import zombie.network.chat.ChatServer;
import zombie.network.fields.SafehouseInvite;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafehouseAcceptPacket extends SafehouseInvite implements INetworkPacket, AntiCheatSafeHouseNotMember.IAntiCheat {
    @JSONField
    private boolean isAccepted;

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0], (String)values[1], (String)values[2]);
        this.isAccepted = (Boolean)values[3];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putBoolean(this.isAccepted);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.isAccepted = b.getBoolean();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isAccepted) {
            this.getSafehouse().addPlayer(this.getUsername());
            LuaEventManager.triggerEvent("AcceptedSafehouseInvite", this.getSafehouse().getTitle(), this.getOwner());
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (SafeHouse.hasSafehouse(this.getUsername()) != null) {
            DebugType.Multiplayer.warn("player is already member or owner");
        } else if (!this.getSafehouse().hasInvite(this.getUsername())) {
            DebugType.Multiplayer.warn("invite is not found");
        } else {
            if (this.isAccepted) {
                this.getSafehouse().addPlayer(this.getUsername());
            }

            this.getSafehouse().removeInvite(this.getUsername());
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, this.getSafehouse());
            this.sendToClients(packetType, null);
            if (this.isAccepted) {
                ChatServer.getInstance()
                    .syncSafehouseChatMembers(this.getSafehouse().getId(), this.getSafehouse().getOwner(), this.getSafehouse().getPlayers());
            }
        }
    }
}
