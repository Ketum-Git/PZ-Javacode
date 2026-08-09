// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class FactionCreatePacket implements INetworkPacket {
    private static final int MIN_FACTION_TITLE_LENGTH = 3;
    private static final int MAX_FACTION_TITLE_LENGTH = 15;
    @JSONField
    private String title;
    @JSONField
    private String username;

    @Override
    public void setData(Object... values) {
        this.title = (String)values[0];
        this.username = (String)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.title);
        b.putUTF(this.username);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.title = b.getUTF();
        this.username = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (StringUtils.isNullOrEmpty(this.title)) {
            DebugType.Multiplayer.error("title is not set");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.username)) {
            DebugType.Multiplayer.error("player is not set");
            return false;
        } else if (Faction.factionExist(this.title)) {
            DebugType.Multiplayer.error("faction is already exists");
            return false;
        } else {
            int titleLength = this.title.length();
            if (titleLength >= 3 && titleLength <= 15) {
                return true;
            } else {
                DebugType.Multiplayer.error("title length is incorrect");
                return false;
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        Faction oldFaction = Faction.getPlayerFaction(this.username);
        if (oldFaction != null && !connection.getRole().hasCapability(Capability.FactionCheat)) {
            DebugType.Multiplayer.error("player is already member or owner of faction");
        } else {
            IsoPlayer isoPlayer = GameServer.getPlayerByUserName(this.username);
            if (isoPlayer == null) {
                DebugType.Multiplayer.error("player not found");
            } else if (!Faction.canCreateFaction(isoPlayer)) {
                DebugType.Multiplayer.error("player can't create faction");
            } else {
                Faction faction = new Faction(this.title, this.username);
                Faction.getFactions().add(faction);
                INetworkPacket.sendToAll(PacketTypes.PacketType.FactionSync, faction);
                ChatServer.getInstance().createFactionChat(this.title);
                ChatServer.getInstance().syncFactionChatMembers(faction.getName(), faction.getOwner(), faction.getPlayers());
            }
        }
    }
}
