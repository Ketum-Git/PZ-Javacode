// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.IDShort;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class PlayerDataRequestPacket extends IDShort implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.setID((Short)values[0]);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer player = GameServer.IDToPlayerMap.get(this.getID());
        if (player != null
            && (
                connection.role.hasCapability(Capability.SeesInvisiblePlayers)
                    || !player.isInvisible() && (connection.RelevantTo(player.getX(), player.getY()) || connection.RelevantTo(player.realx, player.realy))
            )) {
            GameServer.sendPlayerConnected(player, connection);
            player.getNetworkCharacterAI().getState().sync(connection);
            INetworkPacket.send(connection, PacketTypes.PacketType.PlayerInjuries, player);
        }
    }
}
