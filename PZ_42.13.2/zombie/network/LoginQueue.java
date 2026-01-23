// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.network.packets.connection.QueuePacket;

public class LoginQueue {
    private static final ArrayList<UdpConnection> LoginQueue = new ArrayList<>();
    private static final ArrayList<UdpConnection> PreferredLoginQueue = new ArrayList<>();
    private static final UpdateLimit UpdateLimit = new UpdateLimit(3050L);
    private static final UpdateLimit UpdateServerInformationLimit = new UpdateLimit(20000L);
    private static final UpdateLimit LoginQueueTimeout = new UpdateLimit(15000L);
    private static UdpConnection currentLoginQueue;

    public static void receiveLoginQueueDone(long gameLoadingTime, UdpConnection connection) {
        LoggerManager.getLogger("user").write("player " + connection.username + " loading time was: " + gameLoadingTime + " ms");
        synchronized (LoginQueue) {
            if (currentLoginQueue == connection) {
                currentLoginQueue = null;
            }

            loadNextPlayer();
        }

        ConnectionManager.log("receive-packet", "login-queue-done", connection);
        connection.validator.checksumSend(true, false);
    }

    public static void receiveServerLoginQueueRequest(UdpConnection connection) {
        LoggerManager.getLogger("user")
            .write(
                connection.idStr
                    + " \""
                    + connection.username
                    + "\" attempting to join used "
                    + (connection.role.hasCapability(Capability.PriorityLogin) ? "preferred " : "")
                    + "queue"
            );
        synchronized (LoginQueue) {
            if (ServerOptions.getInstance().loginQueueEnabled.getValue()
                && (
                    connection.role.hasCapability(Capability.PriorityLogin)
                        || currentLoginQueue != null
                        || !PreferredLoginQueue.isEmpty()
                        || !LoginQueue.isEmpty()
                        || getCountPlayers() >= ServerOptions.getInstance().getMaxPlayers()
                )
                && (!connection.role.hasCapability(Capability.PriorityLogin) || currentLoginQueue != null || !PreferredLoginQueue.isEmpty())) {
                DebugLog.DetailedInfo.trace("PlaceInQueue ip=%s preferredInQueue=%b", connection.ip, connection.role.hasCapability(Capability.PriorityLogin));
                if (connection.role.hasCapability(Capability.PriorityLogin)) {
                    if (!PreferredLoginQueue.contains(connection)) {
                        PreferredLoginQueue.add(connection);
                    }
                } else if (!LoginQueue.contains(connection)) {
                    LoginQueue.add(connection);
                }

                sendPlaceInTheQueue();
            } else {
                DebugLog.DetailedInfo.trace("ConnectionImmediate ip=%s", connection.ip);
                currentLoginQueue = connection;
                currentLoginQueue.wasInLoadingQueue = true;
                LoginQueueTimeout.Reset(ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
                QueuePacket packet = new QueuePacket();
                packet.setConnectionImmediate();
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.LoginQueueRequest.send(connection);
            }
        }

        ConnectionManager.log("receive-packet", "login-queue-request", connection);
    }

    private static void sendPlaceInTheQueue() {
        QueuePacket packet = new QueuePacket();
        packet.setInformationFields();

        for (UdpConnection connection : PreferredLoginQueue) {
            packet.setPlaceInQueue((byte)(PreferredLoginQueue.indexOf(connection) + 1));
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.LoginQueueRequest.send(connection);
        }

        for (UdpConnection connection : LoginQueue) {
            packet.setPlaceInQueue((byte)(LoginQueue.indexOf(connection) + 1 + PreferredLoginQueue.size()));
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.LoginQueueRequest.send(connection);
        }
    }

    private static void sendConnectRequest(UdpConnection connection) {
        DebugLog.DetailedInfo.trace("SendApplyRequest ip=%s", connection.ip);
        QueuePacket packet = new QueuePacket();
        packet.setConnectionImmediate();
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.LoginQueueRequest.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.LoginQueueRequest.send(connection);
        ConnectionManager.log("send-packet", "login-queue-request", connection);
    }

    public static void disconnect(UdpConnection connection) {
        DebugLog.DetailedInfo.trace("ip=%s", connection.ip);
        synchronized (LoginQueue) {
            if (connection == currentLoginQueue) {
                currentLoginQueue = null;
            } else {
                LoginQueue.remove(connection);
                PreferredLoginQueue.remove(connection);
            }

            sendPlaceInTheQueue();
        }
    }

    public static boolean isInTheQueue(UdpConnection connection) {
        if (!ServerOptions.getInstance().loginQueueEnabled.getValue()) {
            return false;
        } else {
            synchronized (LoginQueue) {
                return connection == currentLoginQueue || LoginQueue.contains(connection) || PreferredLoginQueue.contains(connection);
            }
        }
    }

    public static void update() {
        if (ServerOptions.getInstance().loginQueueEnabled.getValue() && UpdateLimit.Check()) {
            synchronized (LoginQueue) {
                if (currentLoginQueue != null) {
                    if (currentLoginQueue.isFullyConnected()) {
                        DebugLog.DetailedInfo.trace("Connection isFullyConnected ip=%s", currentLoginQueue.ip);
                        currentLoginQueue = null;
                    } else if (LoginQueueTimeout.Check()) {
                        DebugLog.DetailedInfo.trace("Connection timeout ip=%s", currentLoginQueue.ip);
                        currentLoginQueue = null;
                    }
                }

                loadNextPlayer();
            }
        }

        if (UpdateServerInformationLimit.Check()) {
            sendPlaceInTheQueue();
        }
    }

    private static void loadNextPlayer() {
        if (!PreferredLoginQueue.isEmpty() && currentLoginQueue == null) {
            currentLoginQueue = PreferredLoginQueue.remove(0);
            currentLoginQueue.wasInLoadingQueue = true;
            DebugLog.DetailedInfo.trace("Next player from the preferred queue to connect ip=%s", currentLoginQueue.ip);
            LoginQueueTimeout.Reset(ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
            sendConnectRequest(currentLoginQueue);
            sendPlaceInTheQueue();
        }

        if (!LoginQueue.isEmpty() && currentLoginQueue == null && getCountPlayers() < ServerOptions.getInstance().getMaxPlayers()) {
            currentLoginQueue = LoginQueue.remove(0);
            currentLoginQueue.wasInLoadingQueue = true;
            DebugLog.DetailedInfo.trace("Next player from queue to connect ip=%s", currentLoginQueue.ip);
            LoginQueueTimeout.Reset(ServerOptions.getInstance().loginQueueConnectTimeout.getValue() * 1000L);
            sendConnectRequest(currentLoginQueue);
            sendPlaceInTheQueue();
        }
    }

    public static int getCountPlayers() {
        int countPlayers = 0;

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.role != null
                && !c.role.hasCapability(Capability.HideFromSteamUserList)
                && c.wasInLoadingQueue
                && !LoginQueue.contains(c)
                && !PreferredLoginQueue.contains(c)) {
                countPlayers++;
            }
        }

        return countPlayers;
    }

    public static String getDescription() {
        return "queue=["
            + LoginQueue.size()
            + "/"
            + PreferredLoginQueue.size()
            + "/\""
            + (currentLoginQueue == null ? "" : currentLoginQueue.getConnectedGUID())
            + "\"]";
    }
}
