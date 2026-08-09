// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPlayer extends AbstractAntiCheat {
    private static String getPlayerUsernameId(IsoPlayer player) {
        return player == null ? "" : "\"" + player.getUsername() + "\"-" + player.getOnlineID();
    }

    private static String getConnectionUsernamesIds(UdpConnection connection) {
        return connection == null
            ? ""
            : Arrays.stream(connection.players).filter(Objects::nonNull).map(AntiCheatPlayer::getPlayerUsernameId).collect(Collectors.joining(", "));
    }

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        connection.getValidator().playerUpdateTimeoutReset();
        if (packet instanceof AntiCheatPlayer.IAntiCheat field) {
            short playerId = field.getPlayerId();
            return GameServer.IDToPlayerMap.containsKey(playerId) && !connection.hasPlayer(playerId)
                ? String.format(
                    "connection=[%s] tries to update not belonging player id=%d belonging to connection=[%s]",
                    getConnectionUsernamesIds(connection),
                    playerId,
                    getConnectionUsernamesIds(GameServer.getConnectionByPlayerOnlineID(playerId))
                )
                : result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        short getPlayerId();
    }
}
