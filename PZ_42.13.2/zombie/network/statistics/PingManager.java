// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics;

import com.google.common.annotations.VisibleForTesting;
import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

public class PingManager {
    private static final long PING_INTERVAL = 1000L;
    private static final long PING_LIMIT_PERIOD = 60000L;
    private static final long PING_INTERVAL_COUNT = 60L;
    private static final long PING_LIMIT_COUNT = 20L;
    private static final long PING_LOG_COUNT = 120L;
    private static final long MAX_PING_TO_SUM = 1000L;
    public static long pingIntervalCount = 60L;
    public static long pingLimitCount = 20L;
    public static long maxPingToSum = 1000L;
    private static final UpdateLimit ulPing = new UpdateLimit(1000L);
    private static int connectionPing = -1;

    public static int getPing() {
        return connectionPing;
    }

    public static boolean doKickWhileLoading(UdpConnection connection, int ping) {
        int pingLimit = ServerOptions.instance.pingLimit.getValue();
        return pingLimit > 0 && ping > pingLimit && !connection.role.hasCapability(Capability.CantBeKickedIfTooLaggy);
    }

    @VisibleForTesting
    static boolean doKick(UdpConnection connection, int ping) {
        return doKickWhileLoading(connection, ping) && connection.isFullyConnected() && connection.isConnectionGraceIntervalTimeout();
    }

    public static int checkLatest(UdpConnection c, long limit) {
        if (c.pingHistory.size() >= pingIntervalCount) {
            long count = c.pingHistory.stream().limit(pingIntervalCount).filter(p -> p.intValue() > limit).count();
            if (count >= pingLimitCount) {
                return (int)Math.ceil(
                    (float)c.pingHistory.stream().limit(pingIntervalCount).mapToLong(v -> Math.min(maxPingToSum, (long)v.intValue())).sum()
                        / (float)pingIntervalCount
                );
            }
        }

        return 0;
    }

    private static void limitPing() {
        int pingLimit = ServerOptions.instance.pingLimit.getValue();

        for (UdpConnection connection : GameServer.udpEngine.connections) {
            connectionPing = connection.getLastPing();
            connection.pingHistory.addFirst(connectionPing);
            int ping = checkLatest(connection, pingLimit);
            if (doKick(connection, ping)) {
                GameServer.kick(connection, "UI_Policy_Kick", "UI_OnConnectFailed_Ping");
                connection.forceDisconnect("kick-ping-limit");
                DebugLog.Multiplayer.warn("Kick: player=\"%s\" type=\"%s\"", connection.username, "UI_OnConnectFailed_Ping");
                DebugLog.Multiplayer.debugln("Ping: limit=%d/%d average-%d=%d", pingLimit, pingLimitCount, pingIntervalCount, ping);
            }

            if (connection.pingHistory.size() > 120L) {
                connection.pingHistory.removeLast();
            }
        }
    }

    public static void update() {
        if (ulPing.Check()) {
            if (GameClient.client) {
                try {
                    if (GameClient.connection != null) {
                        connectionPing = GameClient.connection.getLastPing();
                    }
                } catch (Exception var1) {
                }
            } else if (GameServer.server) {
                limitPing();
            }
        }
    }
}
