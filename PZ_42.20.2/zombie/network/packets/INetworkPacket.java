// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;

public interface INetworkPacket extends INetworkPacketField {
    default void setData(Object... values) {
    }

    default void parseClientLoading(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void parseClient(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void parseServer(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void postpone() {
    }

    default boolean isPostponed() {
        return false;
    }

    default boolean shouldInstantiate() {
        return false;
    }

    default void processClientLoading(UdpConnection connection) {
    }

    default void processClient(UdpConnection connection) {
    }

    default void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
    }

    default void sync(PacketTypes.PacketType packetType, UdpConnection connection) {
    }

    default void logInconsistentPacket(IConnection connection, PacketTypes.PacketType packetType) {
        DebugType.Multiplayer.warn("The packet %s is not consistent: %s", packetType.name(), this.getDescription());
    }

    private void sendToConnection(PacketTypes.PacketType packetType, IConnection connection) {
        if (connection != null) {
            ByteBufferWriter b = connection.startPacket();

            try {
                packetType.doPacket(b);
                this.write(b);
                if (packetType.isCached && connection.isHashEquals(packetType, b.hashCode())) {
                    connection.cancelPacket();
                } else {
                    packetType.send(connection);
                }
            } catch (Exception var5) {
                connection.cancelPacket();
                DebugType.Multiplayer.printException(var5, "Packet " + packetType.name() + " send error", LogSeverity.Error);
            }
        }
    }

    default void sendToClient(PacketTypes.PacketType packetType, IConnection connection) {
        if (GameServer.server) {
            this.sendToConnection(packetType, connection);
        }
    }

    default void sendToClient(PacketTypes.PacketType packetType, String username) {
        if (GameServer.server) {
            IsoPlayer player = GameServer.getPlayerByUserName(username);
            if (player != null) {
                UdpConnection connection = GameServer.getConnectionFromPlayer(player);
                if (connection != null) {
                    this.sendToConnection(packetType, connection);
                }
            }
        }
    }

    default void sendToServer(PacketTypes.PacketType packetType) {
        if (GameClient.client) {
            this.sendToConnection(packetType, GameClient.connection);
        }
    }

    default void sendToClients(PacketTypes.PacketType packetType, UdpConnection excluded) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID()) && connection.isFullyConnected()) {
                    this.sendToConnection(packetType, connection);
                }
            }
        }
    }

    default void sendToRelativeClients(PacketTypes.PacketType packetType, UdpConnection excluded, float x, float y) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID())
                    && connection.isFullyConnected()
                    && connection.isRelevantTo(x, y)) {
                    this.sendToConnection(packetType, connection);
                }
            }
        }
    }

    static void send(IConnection connection, PacketTypes.PacketType packetType, Object... values) {
        if (connection != null && packetType != null) {
            try {
                INetworkPacket packet = connection.getPacket(packetType);
                packet.setData(values);
                packet.sendToConnection(packetType, connection);
            } catch (Exception var4) {
                ExceptionLogger.logException(var4, "Packet \"" + packetType + "\" send failed", DebugType.Multiplayer, LogSeverity.Error);
            }
        }
    }

    static void send(PacketTypes.PacketType packetType, Object... values) {
        if (GameClient.client) {
            send(GameClient.connection, packetType, values);
        }
    }

    static void send(IsoPlayer player, PacketTypes.PacketType packetType, Object... values) {
        if (GameServer.server) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(player);
            if (connection != null) {
                send(connection, packetType, values);
            }
        }
    }

    static void sendToAll(PacketTypes.PacketType packetType, Object... values) {
        if (GameServer.server) {
            sendToAll(packetType, null, values);
        }
    }

    static void sendToAll(PacketTypes.PacketType packetType, IConnection excluded, Object... values) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID()) && connection.isFullyConnected()) {
                    send(connection, packetType, values);
                }
            }
        }
    }

    static void sendToRelative(PacketTypes.PacketType packetType, float x, float y, Object... values) {
        if (GameServer.server) {
            sendToRelative(packetType, null, x, y, values);
        }
    }

    static void sendToRelative(PacketTypes.PacketType packetType, IConnection excluded, float x, float y, Object... values) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID())
                    && connection.isFullyConnected()
                    && connection.isRelevantTo(x, y)) {
                    send(connection, packetType, values);
                }
            }
        }
    }
}
