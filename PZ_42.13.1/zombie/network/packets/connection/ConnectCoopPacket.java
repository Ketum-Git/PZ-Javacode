// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunkMap;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.popman.ZombiePopulationManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class ConnectCoopPacket implements INetworkPacket {
    @JSONField
    protected byte stage;
    @JSONField
    protected byte playerIndex;
    @JSONField
    protected String username;
    @JSONField
    protected float x;
    @JSONField
    protected float y;
    @JSONField
    protected byte range;
    @JSONField
    protected byte extraInfoFlags;

    public void setInit(IsoPlayer player) {
        this.stage = 1;
        this.playerIndex = (byte)player.playerIndex;
        this.username = player.username != null ? player.username : "";
        this.x = player.getX();
        this.y = player.getY();
        this.extraInfoFlags = player.getExtraInfoFlags();
    }

    public void setPlayerConnect(IsoPlayer player) {
        this.stage = 2;
        this.playerIndex = (byte)player.playerIndex;
        this.extraInfoFlags = player.getExtraInfoFlags();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.stage = b.get();
        this.playerIndex = b.get();
        String playerIndexLog = this.playerIndex + 1 + "/4";
        if (!ServerOptions.instance.allowCoop.getValue() && this.playerIndex != 0) {
            sendCoopAccessDenied("Coop players not allowed", this.playerIndex, connection);
        } else if (this.playerIndex < 0 || this.playerIndex >= 4) {
            sendCoopAccessDenied("Invalid coop player index", this.playerIndex, connection);
        } else if (connection.players[this.playerIndex] != null && !connection.players[this.playerIndex].isDead()) {
            sendCoopAccessDenied("Coop player " + playerIndexLog + " already exists", this.playerIndex, connection);
        } else {
            if (this.stage == 1) {
                String username = GameWindow.ReadStringUTF(b);
                if (username.isEmpty()) {
                    sendCoopAccessDenied("No username given", this.playerIndex, connection);
                    return;
                }

                for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);

                    for (int playerIndex2 = 0; playerIndex2 < 4; playerIndex2++) {
                        if ((c != connection || this.playerIndex != playerIndex2) && username.equals(c.usernames[playerIndex2])) {
                            sendCoopAccessDenied("User \"" + username + "\" already connected", this.playerIndex, connection);
                            return;
                        }
                    }
                }

                DebugLog.Multiplayer.println("coop player=%s username=\"%s\" is joining", playerIndexLog, username);
                if (connection.players[this.playerIndex] != null) {
                    DebugLog.Multiplayer.println("coop player=%s username=\"%s\" is replacing dead player", playerIndexLog, username);
                    short playerID = connection.players[this.playerIndex].onlineId;
                    GameServer.disconnectPlayer(connection.players[this.playerIndex], connection);
                    float x = b.getFloat();
                    float y = b.getFloat();
                    connection.usernames[this.playerIndex] = username;
                    connection.releventPos[this.playerIndex] = new Vector3(x, y, 0.0F);
                    connection.connectArea[this.playerIndex] = new Vector3(x / 8.0F, y / 8.0F, connection.chunkGridWidth);
                    connection.playerIds[this.playerIndex] = playerID;
                    GameServer.IDToAddressMap.put(playerID, connection.getConnectedGUID());
                    sendCoopAccessGranted(this.playerIndex, connection);
                    ZombiePopulationManager.instance.updateLoadedAreas();
                    if (ChatServer.isInited()) {
                        ChatServer.getInstance().initPlayer(playerID);
                    }

                    return;
                }

                if (GameServer.getPlayerCount() >= ServerOptions.getInstance().getMaxPlayers()) {
                    sendCoopAccessDenied("Server is full", this.playerIndex, connection);
                    return;
                }

                short slot = -1;

                for (short i = 0; i < GameServer.udpEngine.getMaxConnections(); i++) {
                    if (GameServer.SlotToConnection[i] == connection) {
                        slot = i;
                        break;
                    }
                }

                short playerID = (short)(slot * 4 + this.playerIndex);
                DebugLog.Multiplayer.println("coop player=%s username=\"%s\" assigned id=%d", playerIndexLog, username, playerID);
                float x = b.getFloat();
                float y = b.getFloat();
                connection.usernames[this.playerIndex] = username;
                connection.releventPos[this.playerIndex] = new Vector3(x, y, 0.0F);
                connection.playerIds[this.playerIndex] = playerID;
                connection.connectArea[this.playerIndex] = new Vector3(x / 8.0F, y / 8.0F, connection.chunkGridWidth);
                GameServer.IDToAddressMap.put(playerID, connection.getConnectedGUID());
                sendCoopAccessGranted(this.playerIndex, connection);
                ZombiePopulationManager.instance.updateLoadedAreas();
            } else if (this.stage == 2) {
                String usernamex = connection.usernames[this.playerIndex];
                if (usernamex == null) {
                    sendCoopAccessDenied("Coop player login wasn't received", this.playerIndex, connection);
                    return;
                }

                DebugLog.Multiplayer.println("coop player=%s username=\"%s\" player info received", playerIndexLog, usernamex);
                GameServer.receivePlayerConnect(b, connection, usernamex);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.stage);
        b.putByte(this.playerIndex);
        if (this.stage == 1) {
            b.putUTF(this.username);
            b.putFloat(this.x);
            b.putFloat(this.y);
        } else if (this.stage == 2) {
            b.putByte(this.playerIndex);
            b.putByte((byte)IsoChunkMap.chunkGridWidth);
            b.putByte(this.extraInfoFlags);
        }
    }

    private static void sendCoopAccessGranted(int playerIndex, UdpConnection connection) {
        ConnectedCoopPacket packet = new ConnectedCoopPacket();
        packet.setAccessGranted((byte)playerIndex);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ConnectedCoop.doPacket(b);

        try {
            packet.write(b);
            PacketTypes.PacketType.ConnectedCoop.send(connection);
        } catch (Exception var5) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(var5, "SendCoopAccessGranted: failed", LogSeverity.Error);
        }
    }

    private static void sendCoopAccessDenied(String reason, int playerIndex, UdpConnection connection) {
        ConnectedCoopPacket packet = new ConnectedCoopPacket();
        packet.setAccessDenied(reason, (byte)playerIndex);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ConnectedCoop.doPacket(b);

        try {
            packet.write(b);
            PacketTypes.PacketType.ConnectedCoop.send(connection);
        } catch (Exception var6) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(var6, "SendCoopAccessDenied: failed", LogSeverity.Error);
        }
    }
}
