// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.fields.INetworkPacketField;

public interface INetworkPacket extends INetworkPacketField {
    default void setData(Object... values) {
    }

    default void parseClientLoading(ByteBuffer b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void parseClient(ByteBuffer b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void parseServer(ByteBuffer b, UdpConnection connection) {
        this.parse(b, connection);
    }

    default void postpone() {
    }

    default boolean isPostponed() {
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

    private void sendToConnection(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (connection != null) {
            ByteBufferWriter b = connection.startPacket();

            try {
                packetType.doPacket(b);
                this.write(b);
                packetType.send(connection);
            } catch (Exception var5) {
                connection.cancelPacket();
                DebugLog.Multiplayer.printException(var5, "Packet " + packetType.name() + " send error", LogSeverity.Error);
            }
        }
    }

    default void sendToClient(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (GameServer.server) {
            this.sendToConnection(packetType, connection);
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
                    && connection.RelevantTo(x, y)) {
                    this.sendToConnection(packetType, connection);
                }
            }
        }
    }

    static void send(UdpConnection connection, PacketTypes.PacketType packetType, Object... values) {
        if (connection != null) {
            INetworkPacket packet = connection.getPacket(packetType);
            packet.setData(values);
            packet.sendToConnection(packetType, connection);
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

    static void sendToAll(PacketTypes.PacketType packetType, UdpConnection excluded, Object... values) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID()) && connection.isFullyConnected()) {
                    INetworkPacket packet = connection.getPacket(packetType);
                    packet.setData(values);
                    packet.sendToConnection(packetType, connection);
                }
            }
        }
    }

    static void sendToRelative(PacketTypes.PacketType packetType, float x, float y, Object... values) {
        if (GameServer.server) {
            sendToRelative(packetType, null, x, y, values);
        }
    }

    static void sendToRelative(PacketTypes.PacketType packetType, UdpConnection excluded, float x, float y, Object... values) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if ((excluded == null || connection.getConnectedGUID() != excluded.getConnectedGUID())
                    && connection.isFullyConnected()
                    && connection.RelevantTo(x, y)) {
                    INetworkPacket packet = connection.getPacket(packetType);
                    packet.setData(values);
                    packet.sendToConnection(packetType, connection);
                }
            }
        }
    }

    static void sendByCapability(PacketTypes.PacketType packetType, Capability capability, Object... values) {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                if (connection.isFullyConnected() && connection.role != null && connection.role.hasCapability(capability)) {
                    INetworkPacket packet = connection.getPacket(packetType);
                    packet.setData(values);
                    packet.sendToConnection(packetType, connection);
                }
            }
        }
    }
}
