// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import gnu.trove.list.array.TShortArrayList;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.SystemDisabler;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.utils.UpdateTimer;
import zombie.core.znet.ZNetStatistics;
import zombie.iso.IsoUtils;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketsCache;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.network.server.EventManager;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.util.StringUtils;

public class UdpConnection extends PacketsCache {
    Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    Lock bufferLockPing = new ReentrantLock();
    private final ByteBuffer bbPing = ByteBuffer.allocate(50);
    private final ByteBufferWriter bbwPing = new ByteBufferWriter(this.bbPing);
    long connectedGuid;
    UdpEngine engine;
    public int index;
    public boolean allChatMuted;
    public String username;
    public String[] usernames = new String[4];
    public byte releventRange;
    public Role role;
    public static HashMap<String, Long> lastConnections = new HashMap<>();
    public long lastConnection;
    public long lastUnauthorizedPacket;
    public String ip;
    public boolean wasInLoadingQueue;
    public String password;
    public boolean ping;
    public Vector3[] releventPos = new Vector3[4];
    public short[] playerIds = new short[4];
    public IsoPlayer[] players = new IsoPlayer[4];
    public Vector3[] connectArea = new Vector3[4];
    public int chunkGridWidth;
    public ClientServerMap[] loadedCells = new ClientServerMap[4];
    public PlayerDownloadServer playerDownloadServer;
    public int zombieListHash;
    public UdpConnection.ChecksumState checksumState = UdpConnection.ChecksumState.Init;
    public long checksumTime;
    public boolean awaitingCoopApprove;
    public long steamId;
    public long ownerId;
    public String idStr;
    public boolean isCoopHost;
    public int maxPlayers;
    public final TShortArrayList chunkObjectState = new TShortArrayList();
    public ZNetStatistics netStatistics;
    public final Deque<Integer> pingHistory = new ArrayDeque<>();
    public final PacketValidator validator = new PacketValidator(this);
    private static final long CONNECTION_ATTEMPT_TIMEOUT = 5000L;
    private static final long CONNECTION_GOOGLE_AUTH_TIMEOUT = 60000L;
    public static final long CONNECTION_GRACE_INTERVAL = 60000L;
    public static final long CONNECTION_READY_INTERVAL = 5000L;
    public long connectionTimestamp;
    public boolean googleAuth;
    private boolean ready;
    public UpdateTimer timerSendZombie = new UpdateTimer();
    public HashMap<Short, UpdateTimer> timerUpdateAnimal = new HashMap<>();
    private boolean fullyConnected;
    public boolean isNeighborPlayer;

    public UdpConnection(UdpEngine engine, long connectedGuid, int index) {
        this.engine = engine;
        this.connectedGuid = connectedGuid;
        this.index = index;
        this.releventPos[0] = new Vector3();

        for (int i = 0; i < 4; i++) {
            this.playerIds[i] = -1;
        }

        this.setConnectionTimestamp(5000L);
        this.wasInLoadingQueue = false;
    }

    public RakNetPeerInterface getPeer() {
        return this.engine.peer;
    }

    public long getConnectedGUID() {
        return this.connectedGuid;
    }

    public String getServerIP() {
        return this.engine.getServerIP();
    }

    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bb.clear();
        return this.bbw;
    }

    public ByteBufferWriter startPingPacket() {
        this.bufferLockPing.lock();
        this.bbPing.clear();
        return this.bbwPing;
    }

    public boolean RelevantTo(float x, float y) {
        for (int n = 0; n < 4; n++) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= minX && x < maxX && y >= minY && y < maxY) {
                    return true;
                }
            }

            if (this.releventPos[n] != null
                && Math.abs(this.releventPos[n].x - x) <= this.releventRange * 8
                && Math.abs(this.releventPos[n].y - y) <= this.releventRange * 8) {
                return true;
            }
        }

        return false;
    }

    public float getRelevantAndDistance(float x, float y, float z) {
        for (int n = 0; n < 4; n++) {
            if (this.releventPos[n] != null
                && Math.abs(this.releventPos[n].x - x) <= this.releventRange * 8
                && Math.abs(this.releventPos[n].y - y) <= this.releventRange * 8) {
                return IsoUtils.DistanceTo(this.releventPos[n].x, this.releventPos[n].y, x, y);
            }
        }

        return Float.POSITIVE_INFINITY;
    }

    public boolean RelevantToPlayerIndex(int n, float x, float y) {
        if (this.connectArea[n] != null) {
            int chunkMapWidth = (int)this.connectArea[n].z;
            int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
            int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
            int maxX = minX + chunkMapWidth * 8;
            int maxY = minY + chunkMapWidth * 8;
            if (x >= minX && x < maxX && y >= minY && y < maxY) {
                return true;
            }
        }

        return this.releventPos[n] != null
            && Math.abs(this.releventPos[n].x - x) <= this.releventRange * 8
            && Math.abs(this.releventPos[n].y - y) <= this.releventRange * 8;
    }

    public boolean RelevantTo(float x, float y, float radius) {
        for (int n = 0; n < 4; n++) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= minX && x < maxX && y >= minY && y < maxY) {
                    return true;
                }
            }

            if (this.releventPos[n] != null && Math.abs(this.releventPos[n].x - x) <= radius && Math.abs(this.releventPos[n].y - y) <= radius) {
                return true;
            }
        }

        return false;
    }

    public void cancelPacket() {
        this.bufferLock.unlock();
    }

    public int getBufferPosition() {
        return this.bb.position();
    }

    public void endPacket(int priority, int reliability, byte ordering) {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, priority, reliability, ordering, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPacket() {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, 1, 3, (byte)0, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPacketImmediate() {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, 0, 3, (byte)0, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPacketUnordered() {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, 2, 2, (byte)0, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPacketUnreliable() {
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, 2, 1, (byte)0, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPacketSuperHighUnreliable() {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bb.flip();
        int i = this.engine.peer.Send(this.bb, 0, 1, (byte)0, this.connectedGuid, false);
        this.bufferLock.unlock();
    }

    public void endPingPacket() {
        int currentPosition = this.bb.position();
        this.bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(this.bb.getShort(), currentPosition, this);
        this.bb.position(currentPosition);
        this.bbPing.flip();
        this.engine.peer.Send(this.bbPing, 0, 1, (byte)0, this.connectedGuid, false);
        this.bufferLockPing.unlock();
    }

    public InetSocketAddress getInetSocketAddress() {
        String ip = this.engine.peer.getIPFromGUID(this.connectedGuid);
        if ("UNASSIGNED_SYSTEM_ADDRESS".equals(ip)) {
            return null;
        } else {
            ip = ip.replace("|", "\u00a3");
            String[] spl = ip.split("\u00a3");
            return new InetSocketAddress(spl[0], Integer.parseInt(spl[1]));
        }
    }

    public void forceDisconnect(String description) {
        if (!GameServer.server) {
            GameClient.instance.disconnect(!"receive-CustomizationData".equals(description) && !"lua-connect".equals(description));
        }

        this.engine.forceDisconnect(this.getConnectedGUID(), description);
        ConnectionManager.log("force-disconnect", description, this);
    }

    public void setFullyConnected() {
        this.validator.reset();
        this.fullyConnected = true;
        this.setConnectionTimestamp(5000L);
        ConnectionManager.log("fully-connected", "", this);
        EventManager.instance().report("[" + this.username + "] connected to server");
    }

    public void setConnectionTimestamp(long interval) {
        this.connectionTimestamp = System.currentTimeMillis() + interval;
        this.setReady(false);
    }

    public void checkReady() {
        if (!this.ready) {
            boolean ready = System.currentTimeMillis() > this.connectionTimestamp;
            if (this.ready != ready) {
                this.setReady(ready);
            }

            if (!ready) {
                IsoPlayer.getInstance().setAlpha(0, 0.6F);
            }
        }
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isGoogleAuthTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isConnectionAttemptTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 5000L;
    }

    public boolean isConnectionGraceIntervalTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isFullyConnected() {
        return this.fullyConnected;
    }

    public void calcCountPlayersInRelevantPosition() {
        if (this.isFullyConnected()) {
            boolean isNeighborPlayer = false;

            for (int j = 0; j < GameServer.udpEngine.connections.size(); j++) {
                UdpConnection c2 = GameServer.udpEngine.connections.get(j);
                if (c2.isFullyConnected() && c2 != this) {
                    for (int k = 0; k < c2.players.length; k++) {
                        IsoPlayer p = c2.players[k];
                        if (p != null && this.RelevantTo(p.getX(), p.getY(), 120.0F)) {
                            isNeighborPlayer = true;
                        }
                    }

                    if (isNeighborPlayer) {
                        break;
                    }
                }
            }

            this.isNeighborPlayer = isNeighborPlayer;
        }
    }

    public ZNetStatistics getStatistics() {
        try {
            this.netStatistics = this.engine.peer.GetNetStatistics(this.connectedGuid);
        } catch (Exception var2) {
            return null;
        }

        return this.netStatistics;
    }

    public int getAveragePing() {
        return this.engine.peer.GetAveragePing(this.connectedGuid);
    }

    public int getLastPing() {
        return this.engine.peer.GetLastPing(this.connectedGuid);
    }

    public int getLowestPing() {
        return this.engine.peer.GetLowestPing(this.connectedGuid);
    }

    public int getMTUSize() {
        return this.engine.peer.GetMTUSize(this.connectedGuid);
    }

    public UdpConnection.ConnectionType getConnectionType() {
        return UdpConnection.ConnectionType.values()[this.engine.peer.GetConnectionType(this.connectedGuid)];
    }

    @Override
    public String toString() {
        if (GameClient.client) {
            return SystemDisabler.printDetailedInfo()
                ? String.format(
                    "guid=%s ip=%s steam-id=%s role=\"%s\" username=\"%s\" connection-type=\"%s\"",
                    this.connectedGuid,
                    this.ip == null ? GameClient.ip : this.ip,
                    this.steamId == 0L ? GameClient.steamID : this.steamId,
                    this.role == null ? "" : this.role.getName(),
                    this.username == null ? GameClient.username : this.username,
                    this.getConnectionType().name()
                )
                : String.format("guid=%s", this.connectedGuid);
        } else {
            return SystemDisabler.printDetailedInfo()
                ? String.format(
                    "guid=%s ip=%s steam-id=%s role=%s username=\"%s\" connection-type=\"%s\"",
                    this.connectedGuid,
                    this.ip,
                    this.steamId,
                    this.role == null ? "" : this.role.getName(),
                    this.username,
                    this.getConnectionType().name()
                )
                : String.format("guid=%s", this.connectedGuid);
        }
    }

    public boolean havePlayer(String username) {
        if (StringUtils.isNullOrEmpty(username)) {
            return false;
        } else {
            for (int i = 0; i < this.players.length; i++) {
                if (this.players[i] != null && username.equals(this.players[i].getUsername())) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean havePlayer(IsoPlayer p) {
        if (p == null) {
            return false;
        } else {
            for (int i = 0; i < this.players.length; i++) {
                if (this.players[i] == p) {
                    return true;
                }
            }

            return false;
        }
    }

    public byte getPlayerIndex(IsoPlayer p) {
        for (byte i = 0; i < this.playerIds.length; i++) {
            if (this.playerIds[i] == p.onlineId) {
                return i;
            }
        }

        return -1;
    }

    public static enum ChecksumState {
        Init,
        Different,
        Done;
    }

    public static enum ConnectionType {
        Disconnected,
        UDPRakNet,
        Steam;
    }
}
